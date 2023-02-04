from mock import create_autospec

import pytest
import yaml

import yp.client
from sepelib.core import config as sconfig
from infra.rsc.src.model.yp_client import YpClient


# FIXME: find out how to do it in arcadia with real file
TEST_CONFIG = """
log:
    filepath: '/tmp/rsc.log'
    handler_class: 'TimedRotatingFileHandler'
    params:
        when: 'midnight'
        backupCount: 10
    loglevel: 'DEBUG'
web:
    http:
        port: 8088
coord:
    hosts: nanny.test
    root: 'rsc'
    log_debug: false
replica_sets:
    storage:
        zk:
            path: 'replica_sets'
yp:
    address: 'xxx:8081'
    cluster: 'xxx'
    user: 'sauron'
    token: 'fake-token'
    root_users:
      - 'some-root'
reflector:
    batch_size: 100
    sleep_secs: 10
    pod_match_labels:
        environ: 'test'
runner:
    threads_count: 1
gc:
    sleep_secs: 10
"""


def load_config():
    sconfig.load()
    sconfig._CONFIG = yaml.load(TEST_CONFIG)


@pytest.fixture(scope='session')
def config():
    load_config()
    return sconfig


@pytest.fixture(scope='session')
def yp_client(config):
    base_yp_client = yp.client.YpClient(address=config.get_value('yp.address'))
    stub = create_autospec(base_yp_client.create_grpc_object_stub())
    return YpClient(stub=stub)
