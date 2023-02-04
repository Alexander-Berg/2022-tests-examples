from __future__ import unicode_literals
import logging

import pytest

import local_yp
import yatest.common
import yp.data_model
from yp.common import YtResponseError
from yp.logger import logger
from yt.wrapper.common import generate_uuid
from yt.wrapper.retries import run_with_retries


OBJECT_TYPES = [
    yp.data_model.OT_DEPLOY_TICKET,
    yp.data_model.OT_RELEASE,
    yp.data_model.OT_RELEASE_RULE,
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
def session_yp_client(request):
    yp_instance = local_yp.get_yp_instance(
        yatest.common.output_path(),
        'yp_{}'.format(generate_uuid()),
        start_proxy=True,
        enable_ssl=True,
        yp_master_config={
            'access_control_manager': {
                'cluster_state_allowed_object_types': [
                    'release_rule',
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
