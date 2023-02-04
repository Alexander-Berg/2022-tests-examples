import itertools
import logging
import os

import boto3

from library.python.testing.recipe import declare_recipe

logger = logging.getLogger()


def start(argv):
    sqs_user = os.getenv('AWS_ACCESS_KEY_ID')
    sqs_port = int(os.getenv('SQS_PORT'))
    logger.info(f'sqs port: {sqs_port}, sqs_user: {sqs_user}')
    sqs_api = boto3.client(
        'sqs',
        region_name='yandex',
        endpoint_url='http://localhost:{}'.format(sqs_port),
        aws_access_key_id=sqs_user,
        aws_secret_access_key='unused',
        aws_session_token='')
    for queue_name in itertools.chain(('configshop-dev', 'configshop-dev-deadletter',
                                       'infra', 'infra-deadletter',
                                       'infra-internal'), argv):
        sqs_api.create_queue(
            QueueName=queue_name + '.fifo',
            Attributes={
                'FifoQueue': 'true',
                'ContentBasedDeduplication': 'true'
            },
        )


def stop(argv):
    return

if __name__ == "__main__":
    declare_recipe(start, stop)
