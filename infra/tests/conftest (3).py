import pytest
import yatest.common
import local_yp

import logging

from yp.common import YtResponseError
from yp.logger import logger

from yt.wrapper.common import generate_uuid
from yt.wrapper.retries import run_with_retries

from infra.yp_drp.podutil import window


OBJECT_TYPES = [
    "pod",
    "pod_set",
    "replica_set",
    "dynamic_resource",
]

logger.setLevel(logging.DEBUG)


def test_method_teardown(yp_client):
    for object_type in OBJECT_TYPES:
        def do():
            for win in window(yp_client.select_objects(object_type, selectors=["/meta/id"]), 20):
                yp_client.remove_objects([(object_type, oids[0]) for oids in win])

        run_with_retries(do, exceptions=(YtResponseError,))


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
                    "pod",
                    "pod_set",
                    "dynamic_resource",
                ],
            },
        },
    )
    yp_instance.start()
    local_yp.sync_access_control(yp_instance)

    try:
        client = yp_instance.create_client()
        client.create_object(object_type='user', attributes={
            'meta': {
                'id': 'test',
            },
        })
        yield client
    finally:
        yp_instance.stop()


@pytest.fixture(scope="function")
def yp_env(request, session_yp_client):
    try:
        yield session_yp_client
    finally:
        test_method_teardown(session_yp_client)
