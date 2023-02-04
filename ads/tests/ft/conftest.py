import pytest
import time
from datetime import datetime
from random import randint

from ads.bsyeti.caesar.tests.ft.common import (
    make_stand,
    create_tables,
    update_tables,
    create_queues,
    get_yt_cluster,
)
from ads.bsyeti.caesar.tests.ft.common.caesar import run_caesar
from ads.bsyeti.caesar.tests.ft.common.model_service import model_service, model_service_port  # noqa: F401


def pytest_configure(config):
    config.addinivalue_line("markers", "table(arg): table to work with.")
    config.addinivalue_line("markers", "extra_profiles(arg): add extra profiles.")
    config.addinivalue_line("markers", "extra_config_args(arg): add extra config args.")


@pytest.fixture(autouse=True)
def enable_bigrt_test_configs(config_test_default_enabled):
    pass


def _get_timestamp(offset=0, random=False):
    ts = int(time.time())
    if random:
        return randint(ts - offset, ts)
    return ts - offset


@pytest.fixture()
def get_timestamp():
    return _get_timestamp


@pytest.fixture()
def yt_cluster():
    return get_yt_cluster()


@pytest.fixture()
def stand(request):
    initialize = {
        "table": None,
        "extra_profiles": {},
        "shard_count": 3,
        "extra_config_args": {},
        "enable_watcher": False,
        "now": datetime.now(),
    }
    for marker in initialize:
        value = request.node.get_closest_marker(marker)
        if value is not None:
            initialize[marker] = value.args[0]
    if initialize["table"] is None:
        raise RuntimeError("Please, provide @pytest.mark.table(**) first")

    return make_stand(request, **initialize)


@pytest.fixture()
def tables(yt_cluster, stand):
    create_tables(yt_cluster, stand.tables)
    update_tables(yt_cluster, stand.tables, stand.extra_profiles)
    return stand.tables


@pytest.fixture()
def queues(yt_cluster, stand):
    return create_queues(yt_cluster, stand.queues)


@pytest.fixture()
def queue(queues):
    return queues["input"]


@pytest.fixture()
def caesar(yt_cluster, stand, tables, queues, port_manager, model_service_port):  # noqa: F811
    with run_caesar(stand, yt_cluster, port_manager.get_port(), model_service_port) as caesar:
        yield caesar
