#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
This task tries to ensure graceful termination of ECS container instances.

The SNS topic "ec2_terminating" receives messages telling us about terminating EC2 instances.
If the terminating instance is part of an ECS cluster, it drains the ECS tasks on the instance.
"""
import json
import pprint
import time

import boto3

from sns_utils import publish_sns_message


def set_container_instance_to_draining(ecs_client, cluster_arn, ecs_container_instance_arn):
    ecs_client.update_container_instances_state(
        cluster=cluster_arn,
        containerInstances=[
            ecs_container_instance_arn,
        ],
        status='DRAINING'
    )


def continue_lifecycle_action(asg_client, asg_group_name, ec2_instance_id, lifecycle_hook_name):
    response = asg_client.complete_lifecycle_action(
        LifecycleHookName=lifecycle_hook_name,
        AutoScalingGroupName=asg_group_name,
        LifecycleActionResult='CONTINUE',
        InstanceId=ec2_instance_id)
    pprint.pprint(response)


def get_ecs_info_from_tags(ec2_client, ec2_instance_id):
    ec2_instance_info = ec2_client.describe_instances(InstanceIds=[
        ec2_instance_id,
    ])
    tags = ec2_instance_info['Reservations'][0]['Instances'][0]['Tags']
    ecs_container_instance_arns = [x['Value'] for x in tags if x['Key'] == 'containerInstanceArn']
    cluster_arns = [x['Value'] for x in tags if x['Key'] == 'clusterArn']
    print(f"containerInstanceArns: {ecs_container_instance_arns}, clusterArns: {cluster_arns}")
    return {
        'cluster_arns': cluster_arns,
        'ecs_container_instance_arns': ecs_container_instance_arns
    }


def main(event, _):
    print(f'Received event:\n{pprint.pformat(event)}')
    asg_client = boto3.client("autoscaling")
    ec2_client = boto3.client("ec2")
    ecs_client = boto3.client('ecs')

    topic_arn = event['Records'][0]['Sns']['TopicArn']
    message = event['Records'][0]['Sns']['Message']
    message_data = json.loads(message)

    ec2_instance_id = message_data['EC2InstanceId']
    asg_group_name = message_data['AutoScalingGroupName']
    lifecycle_hook_name = message_data['LifecycleHookName']
    lifecycle_transition = message_data['LifecycleTransition']
    lifecycle_action_token = message_data['LifecycleActionToken']

    if lifecycle_transition == 'autoscaling:EC2_INSTANCE_TERMINATING':
        ecs_info = get_ecs_info_from_tags(ec2_client, ec2_instance_id)

        if not ecs_info['cluster_arns'] and not ecs_info['ecs_container_instance_arns']:
            continue_lifecycle_action(asg_client, asg_group_name, ec2_instance_id, lifecycle_hook_name)
            return

        cluster_arn = ecs_info['cluster_arns'][0]
        ecs_container_instance_arn = ecs_info['ecs_container_instance_arns'][0]
        running_tasks = ecs_client.list_tasks(
            cluster=ecs_info['cluster_arns'][0],
            containerInstance=ecs_info['ecs_container_instance_arns'][0]
        )
        print(f"running tasks: {running_tasks['taskArns']}")

        if not running_tasks['taskArns']:
            continue_lifecycle_action(asg_client, asg_group_name, ec2_instance_id, lifecycle_hook_name)
        else:
            asg_client.record_lifecycle_action_heartbeat(
                LifecycleHookName=lifecycle_hook_name,
                AutoScalingGroupName=asg_group_name,
                LifecycleActionToken=lifecycle_action_token,
                InstanceId=ec2_instance_id,
            )

            container_instance_info = ecs_client.describe_container_instances(
                cluster=cluster_arn,
                containerInstances=[ecs_container_instance_arn],
            )
            if container_instance_info['containerInstances'][0]['status'] != 'DRAINING':
                set_container_instance_to_draining(ecs_client, cluster_arn, ecs_container_instance_arn)

            time.sleep(30)
            publish_sns_message(topic_arn, message_data)
