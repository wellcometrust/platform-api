# -*- encoding: utf-8 -*-
"""
Sends slack notifications for alarms events
"""

import datetime as dt
import json
import os
import re

import attr
import boto3
import requests

from cloudwatch_alarms import build_cloudwatch_url, ThresholdMessage
from platform_alarms import (
    guess_cloudwatch_log_group, guess_cloudwatch_search_terms
)


@attr.s
class Interval:
    start = attr.ib()
    end = attr.ib()


class Alarm:
    def __init__(self, json_message):
        self.message = json.loads(json_message)

    @property
    def name(self):
        return self.message['AlarmName']

    @property
    def state_reason(self):
        return self.message['NewStateReason']

    def human_reason(self):
        """
        Try to return a more human-readable explanation for the alarm.
        """
        try:
            threshold = ThresholdMessage.from_message(self.state_reason)
        except ValueError:
            return

        if (
            self.name.endswith('-alb-not-enough-healthy-hosts') and
            threshold.is_breaching
        ):
            return 'There are no healthy hosts in the ALB target group.'

        display_time = threshold.date.strftime(
            'at %H:%M:%S on %d %b %Y').replace('on 0', 'on ')

        if self.name.startswith('loris'):
            service = 'Loris'
        elif self.name.startswith('api_'):
            service = 'the API'
        else:
            return

        if self.name.endswith('-alb-target-500-errors'):
            if threshold.actual_value == 1:
                return f'The ALB spotted a 500 error in {service} {display_time}.'
            else:
                return f'The ALB spotted multiple 500 errors ({threshold.actual_value}) in {service} {display_time}.'

        elif self.name.endswith('-alb-unhealthy-hosts'):
            if threshold.actual_value == 1:
                return f'There is an unhealthy host in {service} {display_time}.'
            else:
                return f'There are multiple unhealthy hosts ({threshold.actual_value}) in {service} {display_time}.'

        elif self.name.endswith('-alb-not-enough-healthy-hosts'):
            return (
                f"There aren't enough healthy hosts in {service} "
                f'(saw {threshold.actual_value}; expected more than {threshold.desired_value}) {display_time}.'
            )

    # Sometimes there's enough data in the alarm to make an educated guess
    # about useful CloudWatch logs to check, so we include that in the alarm.
    # The methods and properties below pull out the relevant info.

    @property
    def cloudwatch_timeframe(self):
        """
        Try to work out a likely timeframe for CloudWatch errors.
        """
        threshold = ThresholdMessage.from_message(self.state_reason)

        return Interval(
            start=threshold.date - dt.timedelta(seconds=300),
            end=threshold.date - dt.timedelta(seconds=300)
        )

    def cloudwatch_urls(self):
        """
        Return some CloudWatch URLs that might be useful to check.
        """
        try:
            log_group_name = guess_cloudwatch_log_group(alarm=self)
            timeframe = self.cloudwatch_timeframe
            return [
                build_cloudwatch_url(
                    search_term=search_term,
                    log_group_name=log_group_name,
                    start_date=timeframe.start,
                    end_date=timeframe.end
                )
                for search_term in guess_cloudwatch_search_terms(alarm=self)
            ]
        except ValueError as err:
            print(f'Error in cloudwatch_urls: {err}')
            return []

    def cloudwatch_messages(self):
        """
        Try to find some CloudWatch messages that might be relevant.
        """
        client = boto3.client('logs')

        messages = []

        try:
            log_group_name = guess_cloudwatch_log_group(alarm=self)

            # CloudWatch wants these parameters specified as seconds since
            # 1 Jan 1970 00:00:00, so convert to that first.
            timeframe = self.cloudwatch_timeframe
            epoch = dt.datetime(1970, 1, 1, 0, 0, 0)
            startTime = int((timeframe.start - epoch).total_seconds() * 1000)
            endTime = int((timeframe.end - epoch).total_seconds() * 1000)

            # We only get the first page of results.  If there's more than
            # one page, we have so many errors that not getting them all
            # in the Slack alarm is the least of our worries!
            for term in guess_cloudwatch_search_terms(alarm=self):
                resp = client.filter_log_events(
                    logGroupName=log_group_name,
                    startTime=startTime,
                    endTime=endTime,
                    filterPattern=term
                )
                messages.extend([e['message'] for e in resp['events']])

        except Exception as exc:
            print(f'Error in cloudwatch_messages: {exc}')

        return messages

    @property
    def is_critical(self):
        """Returns True if this is a critical alarm."""
        # Alarms for the API or Loris are *always* critical.
        if any(p in self.name for p in ['api_remus', 'api_romulus', 'loris']):
            return True

        # Lambdas and DLQ alarms are *never* critical.
        if (
            self.name.endswith('dlq_not_empty') or
            self.name.startswith('lambda-')
        ):
            return False

        # Alarms in the service stack are *never* critical.
        if self.name.startswith(
            ('id_minter', 'ingestor', 'transformer'),
        ):
            return False

        # Otherwise default to True, because we don't know what this alarm is.
        return True


def to_bitly(url, access_token):
    """
    Try to shorten a URL with bit.ly.  If it fails, just return the
    original URL.
    """
    def _to_bity_single_url(url):
        resp = requests.get(
            'https://api-ssl.bitly.com/v3/user/link_save',
            params={'access_token': access_token, 'longUrl': url}
        )
        try:
            return resp.json()['data']['link_save']['link']
        except KeyError:
            return url

    return ' / '.join([_to_bity_single_url(u) for u in url.split(' / ')])


def simplify_message(message):
    """
    Sometimes a CloudWatch message includes information that we don't want
    to appear in Slack -- e.g. date/time.

    This function tries to strip out extra bits from the message, so we get
    a tight and focused error appearing in Slack.
    """
    # Scala messages have a prefix that gives us a timestamp and thread info:
    #
    #     14:02:47.249 [ForkJoinPool-2-worker-17]
    #
    # Bin it!
    message = re.sub(
        r'\d{2}:\d{2}:\d{2}\.\d{3} \[ForkJoinPool-\d+-worker-\d+\] ', '',
        message
    )

    # Loris messages have a bunch of origin and UWSGI information as a prefix:
    #
    #     [pid: 86|app: 0|req: 195/3682] 172.17.0.5 () {40 vars in 879
    #       bytes} [Tue Oct 10 19:37:06 2017]
    #
    # Discard it!
    message = re.sub(
        r'\[pid: \d+\|app: \d+\|req: \d+/\d+\] '
        r'\d+\.\d+\.\d+\.\d+ \(\) '
        r'{\d+ vars in \d+ bytes} '
        r'\[[A-Za-z0-9: ]+\]', '', message
    )

    # Loris messages also have an uninteresting suffix:
    #
    #     3 headers in 147 bytes (1 switches on core 0)
    #
    # Throw it away!
    message = re.sub(
        r'\d+ headers in \d+ bytes \(\d+ switches on core \d+\)', '', message
    )

    # Loris logs tell us information that isn't helpful for debugging:
    #
    #      => generated 271 bytes in 988 msecs
    #
    # Expunge-inate!
    message = re.sub(r'=> generated \d+ bytes in \d+ msecs ', '', message)

    # Lambda timeouts have an opaque prefix:
    #
    #     2017-10-12T13:18:31.917Z d1fdfca5-af4f-11e7-a100-030f2a39c6f6 Task
    #     timed out after 10.01 seconds
    #
    # Drop it!
    message = re.sub(
        r'\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z '
        r'[0-9a-f-]+ (?=Task timed out)', '', message
    )

    return message.strip()


def prepare_slack_payload(alarm, bitly_access_token):
    if alarm.is_critical:
        slack_data = {
            'username': 'cloudwatch-alarm',
            'icon_emoji': ':rotating_light:',
        }
        alarm_color = 'danger'
    else:
        slack_data = {
            'username': 'cloudwatch-warning',
            'icon_emoji': ':warning:',
        }
        alarm_color = 'warning'

    slack_data['attachments'] = [
        {
            'color': alarm_color,
            'fallback': alarm.name,
            'title': alarm.name,
            'fields': [{
                'value': alarm.human_reason() or alarm.state_reason
            }]
        }
    ]

    messages = alarm.cloudwatch_messages()
    if messages:
        cloudwatch_message_str = '\n'.join(set([
            simplify_message(m) for m in messages
        ]))
        slack_data['attachments'][0]['fields'].append({
            'title': 'CloudWatch messages',
            'value': cloudwatch_message_str
        })

    cloudwatch_urls = alarm.cloudwatch_urls()
    if cloudwatch_urls:
        cloudwatch_url_str = ' / '.join([
            to_bitly(url=url, access_token=bitly_access_token)
            for url in cloudwatch_urls
        ])
        slack_data['attachments'][0]['fields'].append({
            'value': cloudwatch_url_str
        })

    return slack_data


def main(event, context):
    print(f'event = {event!r}')

    bitly_access_token = os.environ['BITLY_ACCESS_TOKEN']
    slack_critical_hook = os.environ['CRITICAL_SLACK_WEBHOOK']
    slack_noncritical_hook = os.environ['NONCRITICAL_SLACK_WEBHOOK']

    alarm = Alarm(event['Records'][0]['Sns']['Message'])
    slack_data = prepare_slack_payload(alarm, bitly_access_token)

    print('Sending message %s' % json.dumps(slack_data))

    if alarm.is_critical:
        webhook_url = slack_critical_hook
    else:
        webhook_url = slack_noncritical_hook

    response = requests.post(
        webhook_url,
        data=json.dumps(slack_data),
        headers={'Content-Type': 'application/json'}
    )
    response.raise_for_status()
