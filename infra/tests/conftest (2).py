import local_yp
import logging
import pytest
import yatest

import yp.data_model

from yp.local import reset_yp
from yp.logger import logger


logger.setLevel(logging.DEBUG)


MASTER_NUMS = 3

OBJECT_TYPES = [
    yp.data_model.OT_DNS_RECORD_SET,
]


def create_user(yp_instance, id=None, grant_permissions=None):
    yp_client = yp_instance.create_client()

    attributes = dict()
    if id is not None:
        attributes["meta"] = dict(id=id)
    id = yp_client.create_object("user", attributes=attributes)

    if grant_permissions:
        yp_client.update_objects(
            [
                dict(
                    object_type="schema",
                    object_id=object_type,
                    set_updates=[
                        dict(
                            path="/meta/acl/end",
                            value=dict(
                                action="allow",
                                permissions=[permission],
                                subjects=[id],
                            ),
                        ),
                    ],
                )
                for object_type, permission in grant_permissions
            ]
        )

    local_yp.sync_access_control(yp_instance)


def get_yp_instances(yp_master_config):
    master_names = ['master-{}'.format(i) for i in range(1, MASTER_NUMS + 1)]
    yp_instances = {
        name: local_yp.get_yp_instance(
            yatest.common.output_path(path=name),
            'yp_{}'.format(name),
            start_proxy=True,
            yp_master_config=yp_master_config,
        ) for name in master_names
    }

    for yp_instance in yp_instances.values():
        yp_instance.start()
        local_yp.sync_access_control(yp_instance)

    return yp_instances


def test_teardown(yp_instances):
    for cluster, yp_instance in yp_instances.items():
        try:
            reset_yp(yp_instance.create_client())
        except:
            logging.exception(f"Failed to reset YP cluster {cluster}")


def stop_instances(yp_instances):
    for yp_instance in yp_instances.values():
        yp_instance.stop()


@pytest.fixture(scope="class")
def session_yp_env(request):
    yp_master_config = getattr(request.cls, "YP_MASTER_CONFIG", None)
    yp_instances = get_yp_instances(yp_master_config)
    request.addfinalizer(lambda: stop_instances(yp_instances))
    return yp_instances


@pytest.fixture(scope="function")
def yp_env(request, session_yp_env):
    request.addfinalizer(lambda: test_teardown(session_yp_env))
    return session_yp_env
