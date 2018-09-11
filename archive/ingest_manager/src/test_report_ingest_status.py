# -*- encoding: utf-8

import random

import pytest

from report_ingest_status import report_ingest_status


def test_finds_present_status(dynamodb_resource, table_name):
    result = report_ingest_status(
        dynamodb_resource=dynamodb_resource,
        table_name=table_name,
        guid='123'
    )
    assert result == '123'


@pytest.fixture()
def table_name(dynamodb_client):
    table_name = 'report_ingest_status--table-%d' % random.randint(0, 10000)
    create_table(dynamodb_client, table_name)
    yield table_name
    dynamodb_client.delete_table(TableName=table_name)


def create_table(dynamodb_client, table_name):
    try:
        dynamodb_client.create_table(
            TableName=table_name,
            KeySchema=[
                {
                    'AttributeName': 'id',
                    'KeyType': 'HASH'
                }
            ],
            AttributeDefinitions=[
                {
                    'AttributeName': 'id',
                    'AttributeType': 'S'
                }
            ],
            ProvisionedThroughput={
                'ReadCapacityUnits': 1,
                'WriteCapacityUnits': 1
            }
        )
        dynamodb_client.get_waiter('table_exists').wait(TableName=table_name)
    except dynamodb_client.exceptions.ResourceInUseException:
        pass
