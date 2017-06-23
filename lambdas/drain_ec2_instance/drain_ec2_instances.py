import json
import pprint
import boto3
from sns_utils import publish_sns_message


def set_container_instance_to_draining(cluster_arn, ecs_client, ecs_container_instance_arn):
    ecs_client.update_container_instances_state(
        cluster=cluster_arn,
        containerInstances=[
            ecs_container_instance_arn,
        ],
        status='DRAINING'
    )


def continue_lifecycle_action(asg_group_name, ec2_instance_id, lifecycle_hook_name):
    asg_client = boto3.client("asg")
    response = asg_client.complete_lifecycle_action(
        LifecycleHookName=lifecycle_hook_name,
        AutoScalingGroupName=asg_group_name,
        LifecycleActionResult='CONTINUE',
        InstanceId=ec2_instance_id)
    pprint.pprint(response)



def main(event, _):
    print(f'Received event:\n{pprint.pformat(event)}')
    topic_arn = event['Records'][0]['Sns']['TopicArn']
    message = event['Records'][0]['Sns']['Message']
    message_data = json.loads(message)
    ec2_instance_id = message_data['EC2InstanceId']
    asg_group_name = message_data['AutoScalingGroupName']
    lifecycle_hook_name = message_data['LifecycleHookName']
    lifecycle_transition = message_data['LifecycleTransition']
    if lifecycle_transition == 'autoscaling:EC2_INSTANCE_TERMINATING':
        ec2_client = boto3.client("ec2")
        ec2_instance_info = ec2_client.describe_instances(InstanceIds=[
            ec2_instance_id,
        ],)
        tags = ec2_instance_info['Reservations'][0]['Instances'][0]['Tags']
        ecs_container_instance_arn = [x['Value'] for x in tags if x['Key'] == 'containerInstanceArn'][0]
        cluster_arn = [x['Value'] for x in tags if x['Key'] == 'clusterArn'][0]
        ecs_client = boto3.client('ecs')
        running_tasks = ecs_client.list_tasks(cluster=cluster_arn,containerInstance=ecs_container_instance_arn)
        if not running_tasks:
            continue_lifecycle_action(asg_group_name, ec2_instance_id, lifecycle_hook_name)
        else:
            container_instance_info = ecs_client.describe_container_instances(cluster=cluster_arn,containerInstances=[ecs_container_instance_arn])
            if container_instance_info['containerInstances'][0]['status'] != 'DRAINING':
                set_container_instance_to_draining(cluster_arn, ecs_client, ecs_container_instance_arn)
            publish_sns_message(topic_arn, message)
