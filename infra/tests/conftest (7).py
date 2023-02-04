import local_yp
import logging
import pytest
import yatest

from yp.local import reset_yp
from yp.logger import logger
from yt.wrapper.common import generate_uuid

logger.setLevel(logging.DEBUG)


def get_yp_instance():
    uuid = generate_uuid()
    yp_instance = local_yp.get_yp_instance(
        yatest.common.output_path(),
        'yp_{}'.format(uuid),
        start_proxy=True
    )
    yp_instance.start()
    local_yp.sync_access_control(yp_instance)

    return yp_instance


@pytest.fixture(scope="session")
def session_ctl_env(request):
    yp_instance = get_yp_instance()
    request.addfinalizer(lambda: yp_instance.stop())
    return yp_instance


def test_teardown(yp_instance):
    reset_yp(yp_instance.create_client())


@pytest.fixture(scope="function")
def ctl_env(request, session_ctl_env):
    yp_instance = session_ctl_env
    request.addfinalizer(lambda: test_teardown(yp_instance))
    return yp_instance
