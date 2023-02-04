"""Tests host name allocation."""

import pytest

from infra.walle.server.tests.lib.util import (
    TestCase,
    patch,
    mock_task,
    handle_host,
    mock_complete_current_stage,
    check_stage_initialization,
)
from walle import network
from walle.fsm_stages import allocate_hostname
from walle.fsm_stages.common import get_current_stage
from walle.hosts import HostState
from walle.models import timestamp
from walle.network import BlockedHostName
from walle.stages import Stages, Stage
from walle.util.misc import drop_none


@pytest.fixture()
def test(request, monkeypatch_timestamp):
    return TestCase.create(request)


@pytest.fixture
def mock_template(mp):
    mp.function(
        network.get_host_name_template, side_effect=lambda host: network.get_default_host_name_template("test", "mock")
    )


def monkeypatch_get_bot_hosts(mp, *hostnames):
    mp.function(allocate_hostname._get_bot_hosts, return_value=set(hostnames))


def mock_blocked_hostname(hostname):
    BlockedHostName.store(hostname)
    allocate_hostname._get_blocked_hostnames.cache_clear()


def test_stage_initialization(test):
    check_stage_initialization(test, Stage(name=Stages.ALLOCATE_HOSTNAME))


@pytest.mark.parametrize("has_name", (True, False))
@pytest.mark.parametrize("next_id", (None, 10005))
def test_allocate(mock_template, mp, test, has_name, next_id):
    def get_host_info(inv):
        if inv == host.inv:
            return drop_none({"inv": host.inv, "name": host.name})

    # mock taken hostnames
    test.mock_host({"inv": 1, "name": "test0-0001.mock"})
    test.mock_host({"inv": 2, "name": "test1-0001.mock"})
    monkeypatch_get_bot_hosts(mp, "test0-0000.mock", "test0-0002.mock", "test1-0000.mock", "test1-0002.mock")
    mock_blocked_hostname("test0-0003.mock")
    mock_blocked_hostname("test1-0003.mock")
    old_hostname = "some.name"

    host = test.mock_host(
        {
            "inv": 3,
            "state": HostState.FREE,
            "task": mock_task(stage=Stages.ALLOCATE_HOSTNAME),
            "rename_time": 0,
        }
    )
    if has_name:
        host.name = old_hostname
    else:
        del host.name
    if next_id is not None:
        get_current_stage(host).set_data("next_id", next_id)
    host.save()

    with patch("walle.clients.bot.get_host_info", side_effect=get_host_info):
        with patch("walle.fsm_stages.allocate_hostname.BlockedHostName.store", autospec=False) as store_blocked_name:
            handle_host(host)

    template = network.get_default_host_name_template("test", "mock")
    cur_id = 4 if next_id is None else next_id
    host.name = template.fill(cur_id)
    host.rename_time = timestamp()
    get_current_stage(host).set_data("next_id", cur_id + 1)
    mock_complete_current_stage(host)

    test.hosts.assert_equal()
    if has_name:
        store_blocked_name.assert_called_once_with(old_hostname)
    else:
        assert store_blocked_name.mock_calls == []


@pytest.mark.parametrize("has_name", (True, False))
def test_already_allocated(mock_template, test, has_name):
    name = "test1-6666.mock"

    def get_host_info(inv):
        if inv == host.inv:
            return {"inv": host.inv, "name": name}

    host = test.mock_host(
        {
            "state": HostState.FREE,
            "task": mock_task(stage=Stages.ALLOCATE_HOSTNAME),
            "rename_time": 0,
        }
    )
    if has_name:
        host.name = name
    else:
        del host.name
    host.save()

    with patch("walle.clients.bot.get_host_info", side_effect=get_host_info):
        with patch("walle.fsm_stages.allocate_hostname.BlockedHostName.store", autospec=False) as store_blocked_name:
            handle_host(host)

    if not has_name:
        host.name = name
        host.rename_time = timestamp()

    mock_complete_current_stage(host)

    test.hosts.assert_equal()
    assert store_blocked_name.mock_calls == []


@pytest.mark.parametrize("has_name", (True, False))
@pytest.mark.parametrize("next_id", (None, 10005))
def test_name_became_blocked_during_stage(mock_template, mp, test, has_name, next_id):
    def get_host_info(inv):
        if inv == host.inv:
            return drop_none({"inv": host.inv, "name": host.name})

    # mock taken hostnames
    test.mock_host({"inv": 1, "name": "test0-0001.mock"})
    test.mock_host({"inv": 2, "name": "test1-0001.mock"})
    monkeypatch_get_bot_hosts(mp, "test0-0000.mock", "test0-0002.mock", "test1-0000.mock", "test1-0002.mock")
    mock_blocked_hostname("test0-0003.mock")
    mock_blocked_hostname("test1-0003.mock")

    mp.method(BlockedHostName.exists, obj=BlockedHostName, side_effect=[True, True, True, False])

    old_hostname = "some.name"

    host = test.mock_host(
        {
            "inv": 3,
            "state": HostState.FREE,
            "task": mock_task(stage=Stages.ALLOCATE_HOSTNAME),
            "rename_time": 0,
        }
    )
    if has_name:
        host.name = old_hostname
    else:
        del host.name
    if next_id is not None:
        get_current_stage(host).set_data("next_id", next_id)
    host.save()

    with patch("walle.clients.bot.get_host_info", side_effect=get_host_info):
        handle_host(host)

    template = network.get_default_host_name_template("test", "mock")
    cur_id = 4 if next_id is None else next_id
    cur_id += 3  # it became blocked three times
    host.name = template.fill(cur_id)
    host.rename_time = timestamp()
    get_current_stage(host).set_data("next_id", cur_id + 1)
    mock_complete_current_stage(host)

    test.hosts.assert_equal()


@pytest.mark.parametrize("host_name", ("free-666666.wall-e.yandex.net", "sas1-3866.search.yandex.net"))
def test_allocate_free(mp, test, host_name):
    def get_host_info(inv):
        if inv == host.inv:
            return drop_none({"inv": host.inv, "name": host.name})

    monkeypatch_get_bot_hosts(mp, "some-name")
    test.mock_host({"inv": 1, "name": "some_name"})  # canary

    host = test.mock_host(
        {
            "inv": 666666,
            "name": host_name,
            "state": HostState.FREE,
            "task": mock_task(stage=Stages.ALLOCATE_HOSTNAME),
            "rename_time": 0,
        }
    )
    get_current_stage(host).set_param("free", True)
    host.save()

    with patch("walle.clients.bot.get_host_info", side_effect=get_host_info):
        handle_host(host)

    host.name = "free-666666.wall-e.yandex.net"
    if host_name != "free-666666.wall-e.yandex.net":
        host.rename_time = timestamp()
    mock_complete_current_stage(host)

    test.hosts.assert_equal()
