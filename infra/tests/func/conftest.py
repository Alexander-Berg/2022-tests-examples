from __future__ import unicode_literals
import logging

import local_yp
import yatest.common
import yp.data_model
from yp.common import YtResponseError
from yp.logger import logger
from yt.wrapper.common import generate_uuid
from yt.wrapper.retries import run_with_retries

import py
import pytest
import yatest


DCTL_BIN_PATH = 'infra/dctl/bin/dctl'
FUNC_TESTS_DIR = 'infra/dctl/tests'


@pytest.fixture
def dctl_binary():
    """
    Copies built binary to current directory.
    """
    str_path = yatest.common.ram_drive_path()
    path = py.path.local(str_path)
    ctl_path = py.path.local(yatest.common.binary_path(DCTL_BIN_PATH))

    # Copy built dctl: in CI tests execution can be permitted to copied binary
    copied_ctl = path.join('dctl')
    ctl_path.copy(copied_ctl)
    copied_ctl.chmod(0o775)

    return copied_ctl


OBJECT_TYPES = [
    yp.data_model.OT_STAGE,
    yp.data_model.OT_PROJECT,
]

logger.setLevel(logging.DEBUG)


def test_method_teardown(yp_client):
    for object_type in OBJECT_TYPES:
        def do():
            for object_ids in yp_client.select_objects(object_type, selectors=["/meta/id"]):
                yp_client.remove_object(object_type, object_ids[0])

        run_with_retries(do, exceptions=(YtResponseError,))


@pytest.fixture(scope="session")
def session_yp_env():
    yp_instance = local_yp.get_yp_instance(
        yatest.common.output_path(),
        'yp_{}'.format(generate_uuid()),
        start_proxy=True,
        enable_ssl=True,
        yp_master_config={
            'access_control_manager': {
                'cluster_state_allowed_object_types': [
                    'stage',
                ],
            },
        },
    )
    yp_instance.start()
    local_yp.sync_access_control(yp_instance)

    try:
        client = yp_instance.create_client()
        client.create_object(object_type=yp.data_model.OT_USER, attributes={
            'meta': {
                'id': 'robot-drug-deploy',
            },
        })
        yield {'client': client, 'addr': '{}'.format(yp_instance.yp_client_grpc_address)}
    finally:
        yp_instance.stop()


@pytest.fixture(scope="function")
def yp_env(session_yp_env):
    try:
        yield session_yp_env
    finally:
        test_method_teardown(session_yp_env['client'])
