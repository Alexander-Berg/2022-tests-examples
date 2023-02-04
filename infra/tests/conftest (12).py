import pytest
import yatest.common
import local_yp

import logging

from yp.local import reset_yp
from yp.logger import logger

from yt.wrapper.common import generate_uuid


OBJECT_TYPES = [
    "stage",
    "deploy_ticket",
    'release_rule',
    'release',
    'project',
]

logger.setLevel(logging.DEBUG)


def test_method_teardown(yp_client):
    reset_yp(yp_client)


@pytest.fixture(scope="session")
def session_yp_client(request):
    yp_instance = local_yp.get_yp_instance(
        yatest.common.output_path(),
        f'yp_{generate_uuid()}',
        start_proxy=True,
        enable_ssl=True,
        yp_master_config={
            'access_control_manager': {
                'cluster_state_allowed_object_types': [
                    "stage",
                    "deploy_ticket",
                ],
            },
        },
    )
    yp_instance.start()
    local_yp.sync_access_control(yp_instance)

    try:
        client = yp_instance.create_client()
        yield client
    finally:
        yp_instance.stop()


@pytest.fixture(scope="function")
def yp_env(request, session_yp_client):
    session_yp_client.create_object(object_type='user', attributes={
        'meta': {
            'id': 'test',
        },
    })

    try:
        yield session_yp_client
    finally:
        test_method_teardown(session_yp_client)
