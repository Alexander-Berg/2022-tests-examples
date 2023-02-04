"""Test netmon integration."""

import json

import pytest

from infra.walle.server.tests.lib.util import (
    mock_location,
    monkeypatch_config,
    TestCase,
    mock_response,
    monkeypatch_request,
)
from walle.expert import netmon
from walle.expert.constants import NETMON_CONNECTIVITY_STALE_TIMEOUT
from walle.expert.types import CheckType, CheckStatus
from walle.host_health import HealthCheck
from walle.hosts import HostState
from walle.models import monkeypatch_timestamp, timestamp
from walle.util.misc import drop_none

_ALL_LEVELS = frozenset({netmon._LEVEL_DATACENTER, netmon._LEVEL_QUEUE, netmon._LEVEL_SWITCH})
_MOCK_SWITCH_THRESHOLD = 0.7
_MOCK_QUEUE_THRESHOLD = 0.8
_MOCK_DATACENTER_THRESHOLD = 0.9

_MOCK_QUEUE_SEEN_THRESHOLD = 60
_MOCK_DC_SEEN_THRESHOLD = 70
_MOCK_CUSTOM_SEEN_THRESHOLD = 55

_MOCK_SWITCH_NAME = "{}-mock".format(netmon._LEVEL_SWITCH)
_MOCK_QUEUE_NAME = "{}-mock".format(netmon._LEVEL_QUEUE)
_MOCK_DC_NAME = "{}-mock".format(netmon._LEVEL_DATACENTER)


def get_mocked_netmon_config():
    return netmon.NetmonConfig(
        service="netmon",
        host="mock-netmon-host",
        expression="mock-expression",
        network="mock-network",
        protocol="mock-protocol",
        total_hosts_cut_off=100,
        alive_connectivity_index=3,
        seen_hosts_threshold=netmon.SeenHostsThreshold(
            queue=dict(default=_MOCK_QUEUE_SEEN_THRESHOLD), datacenter=dict(default=_MOCK_DC_SEEN_THRESHOLD)
        ),
        alive_threshold=netmon.ConnectivityThreshold(
            switch=_MOCK_SWITCH_THRESHOLD, queue=_MOCK_QUEUE_THRESHOLD, datacenter=_MOCK_DATACENTER_THRESHOLD
        ),
    )


@pytest.fixture
def test(monkeypatch, request, mp_juggler_source):
    walle_test = TestCase.create(request, healthdb=True)

    monkeypatch_timestamp(monkeypatch)
    return walle_test


@pytest.fixture
def mock_netmon_data(monkeypatch, mock_netmon_alive_data, mock_seen_hosts_data):
    monkeypatch_request(
        monkeypatch,
        side_effect=[
            mock_response(mock_netmon_alive_data),
            mock_response(mock_seen_hosts_data),
        ],
    )

    return mock_netmon_alive_data, mock_seen_hosts_data


@pytest.fixture
def mock_netmon_alive_data(load_test_json):
    # we've got data for these switches in the file:
    #   "name": "ugr6-s25" - normal process
    #   "name": "fol1-s13" - normal process
    #   "name": "fol7-s55" - absent connectivity data
    #   "name": "iva3-s25" - absent connectivity data for both switch and queue
    #   "name": "red1-53s1" - absent connectivity data for switch, queue and datacenter
    # but we do not use them in tests because it is not stable. We generate custom data in tests.
    data = load_test_json("mocks/netmon-alive.json")
    data["generated"] = int(timestamp())

    return data


@pytest.fixture
def mock_seen_hosts_data(load_test_json):
    data = load_test_json("mocks/netmon-seen-hosts.json")
    data["hosts"]["timestamp"] = int(timestamp())

    return data


def test_read_netmon_config(monkeypatch):
    monkeypatch_config(
        monkeypatch,
        "switch_connectivity.netmon",
        dict(
            host="mock-netmon-host",
            expression="mock-expression",
            network="mock-network",
            protocol="mock-protocol",
            seen_hosts=dict(
                cutoff=100,
                threshold=dict(
                    queue=dict(default=_MOCK_QUEUE_SEEN_THRESHOLD), datacenter=dict(default=_MOCK_DC_SEEN_THRESHOLD)
                ),
            ),
            alive=dict(
                connectivity_index=3,
                threshold=dict(
                    switch=_MOCK_SWITCH_THRESHOLD, queue=_MOCK_QUEUE_THRESHOLD, datacenter=_MOCK_DATACENTER_THRESHOLD
                ),
            ),
        ),
    )

    assert netmon.make_netmon_config() == get_mocked_netmon_config()


def test_read_nocsla_configs(monkeypatch):
    monkeypatch_config(
        monkeypatch,
        "switch_connectivity.nocsla",
        dict(
            expression="mock-expression",
            network="mock-network",
            protocol="mock-protocol",
            seen_hosts=dict(
                cutoff=100,
                threshold=dict(
                    queue=dict(default=_MOCK_QUEUE_SEEN_THRESHOLD), datacenter=dict(default=_MOCK_DC_SEEN_THRESHOLD)
                ),
            ),
            alive=dict(
                connectivity_index=3,
                threshold=dict(
                    switch=_MOCK_SWITCH_THRESHOLD, queue=_MOCK_QUEUE_THRESHOLD, datacenter=_MOCK_DATACENTER_THRESHOLD
                ),
            ),
            endpoints=dict(
                dc1=dict(host="mock-dc1-host"),
                dc2=dict(host="mock-dc2-host"),
            ),
        ),
    )

    dc1_conf = get_mocked_netmon_config()
    dc1_conf.service = "nocsla-dc1"
    dc1_conf.host = "mock-dc1-host"
    dc2_conf = get_mocked_netmon_config()
    dc2_conf.service = "nocsla-dc2"
    dc2_conf.host = "mock-dc2-host"

    assert netmon.make_nocsla_configs() == [dc1_conf, dc2_conf]


def test_saves_netmon_data(test, mock_netmon_data):
    switch = "ugr6-s25"
    host = test.mock_host({"state": HostState.ASSIGNED, "location": mock_location(switch=switch)})
    poller = netmon.NetmonPoller(get_mocked_netmon_config(), current_shard=0, shards_num=1)
    poller.poll()

    netmon_check = _mock_netmon_check(switch, host.name, *mock_netmon_data)
    test.health_checks.mock(netmon_check, save=False)

    test.hosts.assert_equal()
    test.health_checks.assert_equal()


def test_calculates_status_mtime_no_prev_state(test, mock_netmon_data):
    switch = "ugr6-s25"
    hostname = "default-1"
    test.mock_host({"inv": 1, "name": hostname, "state": HostState.ASSIGNED, "location": mock_location(switch=switch)})

    # data received
    poller = netmon.NetmonPoller(get_mocked_netmon_config(), current_shard=0, shards_num=1)
    poller.poll()

    # reconstruct expected results
    # host have new check received
    host_check = _mock_netmon_check(switch, hostname, *mock_netmon_data)

    test.health_checks.mock(host_check, save=False)

    test.hosts.assert_equal()
    test.health_checks.assert_equal()


def test_calculates_status_mtime_no_change(test, mock_netmon_data):
    switch = "ugr6-s25"
    old_health_time = timestamp() - 120

    hostname = "default-1"
    test.mock_host(
        {
            "inv": 1,
            "name": hostname,
            "state": HostState.ASSIGNED,
            "location": mock_location(switch=switch),
        }
    )

    # old checks
    # host check's status does not change, timestamp bump, status_mtime stays the same
    host_check = _mock_netmon_check(
        switch, hostname, *mock_netmon_data, check_timestamp=old_health_time, status_mtime=old_health_time - 300
    )

    test.health_checks.mock(host_check, add=False)

    # data received
    poller = netmon.NetmonPoller(get_mocked_netmon_config(), current_shard=0, shards_num=1)
    poller.poll()

    # reconstruct expected results
    # host have no changes, but receive date should bump
    host_check = _mock_netmon_check(switch, hostname, *mock_netmon_data, status_mtime=old_health_time - 300)

    test.health_checks.mock(host_check, save=False)

    test.hosts.assert_equal()
    test.health_checks.assert_equal()


def test_calculates_status_mtime_prev_state_differs(test, mock_netmon_data):
    switch = "ugr6-s25"
    hostname = "default-1"
    old_health_time = timestamp() - 120

    test.mock_host(
        {
            "inv": 1,
            "name": hostname,
            "state": HostState.ASSIGNED,
            "location": mock_location(switch=switch),
        }
    )

    # host check's status changes, effective timestamp and status_mtime should be bumped
    host_check = _mock_netmon_check(
        switch,
        hostname,
        *mock_netmon_data,
        status=CheckStatus.FAILED,
        check_timestamp=old_health_time,
        status_mtime=old_health_time - 300
    )

    test.health_checks.mock(host_check, add=False)

    # data received
    poller = netmon.NetmonPoller(get_mocked_netmon_config(), current_shard=0, shards_num=1)
    poller.poll()

    # reconstruct expected results
    # host have new state received, dates bumped, state hash changed
    host_check = _mock_netmon_check(switch, hostname, *mock_netmon_data)

    test.health_checks.mock(host_check, save=False)

    test.hosts.assert_equal()
    test.health_checks.assert_equal()


def _mock_two_reference_hosts(test, switch_name, check_timestamp=None, remember_health=False):
    # generate two hosts with different previous checks, which allows us to validate check storing.
    host1 = test.mock_host(
        {"inv": 1, "name": "mock-host-1", "state": HostState.ASSIGNED, "location": mock_location(switch=switch_name)}
    )
    host2 = test.mock_host(
        {"inv": 2, "name": "mock-host-2", "state": HostState.ASSIGNED, "location": mock_location(switch=switch_name)}
    )

    if check_timestamp is None:
        check_timestamp = timestamp() - 1

    host2_check = _mock_netmon_check(
        switch_name,
        host2.name,
        check_timestamp=check_timestamp,
        alive_data=None,
        seen_hosts_data=None,
        need_metadata=False,
    )
    test.health_checks.mock(host2_check, add=remember_health)

    return host1, host2


@pytest.mark.parametrize(
    "empty_levels",
    [
        {netmon._LEVEL_SWITCH},
        {netmon._LEVEL_QUEUE, netmon._LEVEL_SWITCH},
        _ALL_LEVELS,
    ],
)
def test_saves_absent_connectivity_as_missing(
    test, monkeypatch, mock_netmon_alive_data, mock_seen_hosts_data, empty_levels
):

    _inject_connectivity(mock_netmon_alive_data, empty_levels=empty_levels)
    _inject_seen_hosts(mock_seen_hosts_data)

    monkeypatch_request(
        monkeypatch,
        side_effect=[
            mock_response(mock_netmon_alive_data),
            mock_response(mock_seen_hosts_data),
        ],
    )

    host1, host2 = _mock_two_reference_hosts(test, _MOCK_SWITCH_NAME)

    poller = netmon.NetmonPoller(get_mocked_netmon_config(), current_shard=0, shards_num=1)
    poller.poll()

    check_kwargs = {"status": CheckStatus.MISSING, "missing_levels": empty_levels}

    for host in host1, host2:
        check = _mock_netmon_check(
            _MOCK_SWITCH_NAME, host.name, mock_netmon_alive_data, mock_seen_hosts_data, **check_kwargs
        )
        test.health_checks.mock(check, save=False)

    test.hosts.assert_equal()
    test.health_checks.assert_equal()


def test_missing_switch_is_a_void_check(test, monkeypatch, mock_netmon_alive_data, mock_seen_hosts_data):
    _inject_connectivity(mock_netmon_alive_data, levels=frozenset({netmon._LEVEL_QUEUE, netmon._LEVEL_DATACENTER}))
    _inject_seen_hosts(mock_seen_hosts_data)

    monkeypatch_request(
        monkeypatch,
        side_effect=[
            mock_response(mock_netmon_alive_data),
            mock_response(mock_seen_hosts_data),
        ],
    )

    # create hosts, with old check data. Remember this data exists, it won't be changed.
    _mock_two_reference_hosts(test, _MOCK_SWITCH_NAME, remember_health=True)

    poller = netmon.NetmonPoller(get_mocked_netmon_config(), current_shard=0, shards_num=1)
    poller.poll()

    test.hosts.assert_equal()
    test.health_checks.assert_equal()


def test_saves_staled_connectivity_as_void(test, monkeypatch, mock_netmon_alive_data, mock_seen_hosts_data):
    data_timestamp = int(timestamp() - NETMON_CONNECTIVITY_STALE_TIMEOUT - 1)
    old_check_timestamp = int(timestamp() - NETMON_CONNECTIVITY_STALE_TIMEOUT - 2)

    mock_netmon_alive_data["generated"] = data_timestamp
    _inject_connectivity(mock_netmon_alive_data)
    _inject_seen_hosts(mock_seen_hosts_data)

    monkeypatch_request(
        monkeypatch,
        side_effect=[
            mock_response(mock_netmon_alive_data),
            mock_response(mock_seen_hosts_data),
        ],
    )

    # create hosts, with old check data. Remember this data exists, it won't be changed.
    _mock_two_reference_hosts(test, _MOCK_SWITCH_NAME, check_timestamp=old_check_timestamp, remember_health=True)

    with pytest.raises(netmon.ConnectivityDataStaledException):
        poller = netmon.NetmonPoller(get_mocked_netmon_config(), current_shard=0, shards_num=1)
        poller.poll()

    test.hosts.assert_equal()
    test.health_checks.assert_equal()


@pytest.mark.parametrize(
    "failed_levels",
    [
        _ALL_LEVELS,
        {netmon._LEVEL_SWITCH},
        {netmon._LEVEL_SWITCH, netmon._LEVEL_QUEUE},
        {netmon._LEVEL_SWITCH, netmon._LEVEL_QUEUE, netmon._LEVEL_DATACENTER},
        {netmon._LEVEL_QUEUE, netmon._LEVEL_DATACENTER},
        {netmon._LEVEL_DATACENTER},
    ],
)
def test_bad_connectivity_is_failure(test, monkeypatch, mock_netmon_alive_data, mock_seen_hosts_data, failed_levels):
    _inject_connectivity(mock_netmon_alive_data, failed_levels=failed_levels)
    _inject_seen_hosts(mock_seen_hosts_data)

    monkeypatch_request(
        monkeypatch,
        side_effect=[
            mock_response(mock_netmon_alive_data),
            mock_response(mock_seen_hosts_data),
        ],
    )

    host1, host2 = _mock_two_reference_hosts(test, _MOCK_SWITCH_NAME)

    poller = netmon.NetmonPoller(get_mocked_netmon_config(), current_shard=0, shards_num=1)
    poller.poll()

    for host in host1, host2:
        check = _mock_netmon_check(
            _MOCK_SWITCH_NAME,
            host.name,
            mock_netmon_alive_data,
            mock_seen_hosts_data,
            status=CheckStatus.FAILED,
            failed_levels=failed_levels,
        )
        test.health_checks.mock(check, save=False)

    test.hosts.assert_equal()
    test.health_checks.assert_equal()


@pytest.mark.parametrize(
    "sparse_levels",
    [
        {netmon._LEVEL_QUEUE, netmon._LEVEL_DATACENTER},
        {netmon._LEVEL_DATACENTER},
        {netmon._LEVEL_QUEUE},
    ],
)
def test_few_seen_hosts_is_suspected_check(
    test, monkeypatch, mock_netmon_alive_data, mock_seen_hosts_data, sparse_levels
):
    _inject_connectivity(mock_netmon_alive_data)
    _inject_seen_hosts(mock_seen_hosts_data, sparse_levels=sparse_levels)

    monkeypatch_request(
        monkeypatch,
        side_effect=[
            mock_response(mock_netmon_alive_data),
            mock_response(mock_seen_hosts_data),
        ],
    )

    host1, host2 = _mock_two_reference_hosts(test, _MOCK_SWITCH_NAME)

    poller = netmon.NetmonPoller(get_mocked_netmon_config(), current_shard=0, shards_num=1)
    poller.poll()

    for host in host1, host2:
        check = _mock_netmon_check(
            _MOCK_SWITCH_NAME,
            host.name,
            mock_netmon_alive_data,
            mock_seen_hosts_data,
            suspected_levels=sparse_levels,
            status=CheckStatus.SUSPECTED,
        )
        test.health_checks.mock(check, save=False)

    test.hosts.assert_equal()
    test.health_checks.assert_equal()


@pytest.mark.parametrize(
    "missing_levels",
    [
        {netmon._LEVEL_DATACENTER},
        {netmon._LEVEL_QUEUE},
    ],
)
def test_missing_seen_hosts_is_missing_check(
    test, monkeypatch, mock_netmon_alive_data, mock_seen_hosts_data, missing_levels
):
    _inject_connectivity(mock_netmon_alive_data)
    _inject_seen_hosts(mock_seen_hosts_data, levels=_ALL_LEVELS - missing_levels)

    monkeypatch_request(
        monkeypatch,
        side_effect=[
            mock_response(mock_netmon_alive_data),
            mock_response(mock_seen_hosts_data),
        ],
    )

    host1, host2 = _mock_two_reference_hosts(test, _MOCK_SWITCH_NAME)

    poller = netmon.NetmonPoller(get_mocked_netmon_config(), current_shard=0, shards_num=1)
    poller.poll()

    for host in host1, host2:
        check = _mock_netmon_check(
            _MOCK_SWITCH_NAME,
            host.name,
            mock_netmon_alive_data,
            mock_seen_hosts_data,
            seen_hosts_missing_levels=missing_levels,
            status=CheckStatus.MISSING,
        )
        test.health_checks.mock(check, save=False)

    test.hosts.assert_equal()
    test.health_checks.assert_equal()


@pytest.mark.parametrize(
    ["cut_off", "small_levels"],
    [
        (100, set()),
        (5000, {netmon._LEVEL_QUEUE}),
        (50000, {netmon._LEVEL_QUEUE, netmon._LEVEL_DATACENTER}),
    ],
)
def test_small_entity_is_always_ok(
    test, monkeypatch, mock_netmon_alive_data, mock_seen_hosts_data, cut_off, small_levels
):
    _inject_connectivity(mock_netmon_alive_data)
    # all levels have few hosts, but cut off force some of levels to Ok status
    _inject_seen_hosts(mock_seen_hosts_data, sparse_levels=_ALL_LEVELS)

    monkeypatch_request(
        monkeypatch,
        side_effect=[
            mock_response(mock_netmon_alive_data),
            mock_response(mock_seen_hosts_data),
        ],
    )

    host1, host2 = _mock_two_reference_hosts(test, _MOCK_SWITCH_NAME)

    conf = get_mocked_netmon_config()
    conf.total_hosts_cut_off = cut_off
    poller = netmon.NetmonPoller(conf, current_shard=0, shards_num=1)
    poller.poll()

    check_suspected_levels = {netmon._LEVEL_QUEUE, netmon._LEVEL_DATACENTER} - small_levels
    check_status = CheckStatus.SUSPECTED if check_suspected_levels else CheckStatus.PASSED
    for host in host1, host2:
        check = _mock_netmon_check(
            _MOCK_SWITCH_NAME,
            host.name,
            mock_netmon_alive_data,
            mock_seen_hosts_data,
            suspected_levels=check_suspected_levels,
            small_levels=small_levels,
            status=check_status,
            custom_conf=conf,
        )
        test.health_checks.mock(check, save=False)

    test.hosts.assert_equal()
    test.health_checks.assert_equal()


@pytest.mark.parametrize(
    ["level", "name"],
    [
        (netmon._LEVEL_QUEUE, _MOCK_QUEUE_NAME),
        (netmon._LEVEL_DATACENTER, _MOCK_DC_NAME),
    ],
)
@pytest.mark.parametrize("fail_check", (True, False))
def test_custom_thresholds(test, monkeypatch, mock_netmon_alive_data, mock_seen_hosts_data, level, name, fail_check):

    failed_levels = {level} if fail_check else ()

    _inject_connectivity(mock_netmon_alive_data)
    _inject_seen_hosts(mock_seen_hosts_data, custom_threshold_levels={level}, sparse_levels=failed_levels)

    monkeypatch_request(
        monkeypatch,
        side_effect=[
            mock_response(mock_netmon_alive_data),
            mock_response(mock_seen_hosts_data),
        ],
    )

    host1, host2 = _mock_two_reference_hosts(test, _MOCK_SWITCH_NAME)

    conf = get_mocked_netmon_config()
    getattr(conf.seen_hosts_threshold, level)[name] = _MOCK_CUSTOM_SEEN_THRESHOLD
    poller = netmon.NetmonPoller(conf, current_shard=0, shards_num=1)
    poller.poll()

    check_status = CheckStatus.SUSPECTED if fail_check else CheckStatus.PASSED
    for host in host1, host2:
        check = _mock_netmon_check(
            _MOCK_SWITCH_NAME,
            host.name,
            mock_netmon_alive_data,
            mock_seen_hosts_data,
            suspected_levels=failed_levels,
            status=check_status,
            custom_conf=conf,
        )
        test.health_checks.mock(check, save=False)

    test.hosts.assert_equal()
    test.health_checks.assert_equal()


def _inject_connectivity(connectivity_data, levels=_ALL_LEVELS, empty_levels=frozenset(), failed_levels=frozenset()):
    if netmon._LEVEL_SWITCH in levels:
        connectivity_data["switches"].append(
            {
                "name": _MOCK_SWITCH_NAME,
                "queue": _MOCK_QUEUE_NAME,
                "dc": _MOCK_DC_NAME,
                "alive": 0.9961028839,
                "connectivity": _mock_connectivity(netmon._LEVEL_SWITCH, empty_levels, failed_levels),
            }
        )

    if netmon._LEVEL_QUEUE in levels:
        connectivity_data["queues"].append(
            {
                "name": _MOCK_QUEUE_NAME,
                "dc": _MOCK_DC_NAME,
                "alive": 0.9961028839,
                "connectivity": _mock_connectivity(netmon._LEVEL_QUEUE, empty_levels, failed_levels),
            }
        )

    if netmon._LEVEL_DATACENTER in levels:
        connectivity_data["datacenters"].append(
            {
                "name": _MOCK_DC_NAME,
                "alive": 0.9961028839,
                "connectivity": _mock_connectivity(netmon._LEVEL_DATACENTER, empty_levels, failed_levels),
            }
        )


def _mock_connectivity(level, empty_levels, failed_levels):
    if level in empty_levels:
        return []
    if level in failed_levels:
        return [1, 1, 1, _get_mock_connectivity_threshold(level) - 0.0001]
    else:
        return [1, 1, 1, 1]


def _get_mock_connectivity_threshold(level):
    if level == netmon._LEVEL_DATACENTER:
        return _MOCK_DATACENTER_THRESHOLD

    if level == netmon._LEVEL_QUEUE:
        return _MOCK_QUEUE_THRESHOLD

    if level == netmon._LEVEL_SWITCH:
        return _MOCK_SWITCH_THRESHOLD


def _inject_seen_hosts(seen_hosts_data, levels=_ALL_LEVELS, sparse_levels=(), custom_threshold_levels=()):
    if netmon._LEVEL_QUEUE in levels:
        level = netmon._LEVEL_QUEUE
        threshold = _MOCK_CUSTOM_SEEN_THRESHOLD if level in custom_threshold_levels else _MOCK_QUEUE_SEEN_THRESHOLD
        data = _mock_seen_hosts(level, _MOCK_QUEUE_NAME, 2000, threshold, sparse_levels)
        seen_hosts_data["hosts"]["queue"].append(data)

    if netmon._LEVEL_DATACENTER in levels:
        level = netmon._LEVEL_DATACENTER
        threshold = _MOCK_CUSTOM_SEEN_THRESHOLD if level in custom_threshold_levels else _MOCK_DC_SEEN_THRESHOLD
        data = _mock_seen_hosts(level, _MOCK_DC_NAME, 20000, threshold, sparse_levels)
        seen_hosts_data["hosts"]["dc"].append(data)


def _mock_seen_hosts(level, name, total, threshold, sparse_levels):
    return {
        "name": name,
        "total": total,
        "seen": (threshold * total // 100) - int(level in sparse_levels),
    }


def _mock_netmon_check(
    switch,
    host_name,
    alive_data,
    seen_hosts_data,
    status=CheckStatus.PASSED,
    missing_levels=(),
    failed_levels=(),
    suspected_levels=(),
    seen_hosts_missing_levels=(),
    small_levels=(),
    check_timestamp=None,
    status_mtime=None,
    need_metadata=True,
    custom_conf=None,
):
    custom_conf = custom_conf or get_mocked_netmon_config()
    if need_metadata:
        metadata = _mock_check_metadata(
            switch,
            alive_data,
            seen_hosts_data,
            custom_conf,
            missing_levels,
            failed_levels,
            suspected_levels,
            seen_hosts_missing_levels,
            small_levels,
        )
    else:
        metadata = None

    ts = timestamp()
    return drop_none(
        {
            "id": HealthCheck.mk_check_key(host_name, CheckType.NETMON),
            "type": CheckType.NETMON,
            "fqdn": host_name,
            "status": status,
            "status_mtime": status_mtime or ts,
            "timestamp": check_timestamp or ts,
            "metadata": metadata,
        }
    )


def _mock_check_metadata(
    switch,
    alive_data,
    seen_hosts_data,
    config,
    missing_levels=(),
    failed_levels=(),
    suspected_levels=(),
    seen_hosts_missing_levels=(),
    small_levels=(),
):

    switches_ = alive_data["switches"]

    [switch_data] = list(filter(lambda s: s["name"] == switch, switches_))
    [queue_data] = list(filter(lambda q: q["name"] == switch_data["queue"], alive_data["queues"]))
    [dc_data] = list(filter(lambda dc: dc["name"] == switch_data["dc"], alive_data["datacenters"]))

    connectivity_bar = 3

    def get_or_none(data):
        return data["connectivity"][connectivity_bar] if data["connectivity"] else None

    def get_status(level):
        return (
            CheckStatus.MISSING
            if level in missing_levels
            else CheckStatus.MISSING
            if level in seen_hosts_missing_levels
            else CheckStatus.FAILED
            if level in failed_levels
            else CheckStatus.SUSPECTED
            if level in suspected_levels
            else CheckStatus.PASSED
        )

    def get_connectivity_status(level):
        return (
            CheckStatus.MISSING
            if level in missing_levels
            else CheckStatus.FAILED
            if level in failed_levels
            else CheckStatus.PASSED
        )

    def get_connectivity_reason(level, name, connectivity):
        metadata_converter = netmon.ConnectivityMetadata(
            level, config.alive_connectivity_index, getattr(config.alive_threshold, level)
        )
        if level in missing_levels:
            return metadata_converter.missing_data_reason(name)

        status = get_connectivity_status(level)
        result = "above" if status == CheckStatus.PASSED else "below"
        return metadata_converter.status_reason(name, connectivity, result)

    def get_volume_status(level):
        return (
            CheckStatus.PASSED
            if level in small_levels
            else CheckStatus.MISSING
            if level in seen_hosts_missing_levels
            else CheckStatus.SUSPECTED
            if level in suspected_levels
            else CheckStatus.PASSED
        )

    def get_volume_reason(level, name, seen_hosts_data):
        metadata_converter = netmon.VolumeMetadata(
            level, getattr(config.seen_hosts_threshold, level), config.total_hosts_cut_off
        )
        if level in seen_hosts_missing_levels:
            return metadata_converter.missing_data_reason(name)

        [seen_hosts] = list(filter(lambda s: s["name"] == name, seen_hosts_data))
        seen_hosts["percent"] = seen_hosts["seen"] * 100.0 / seen_hosts["total"]

        if level in small_levels:
            return metadata_converter.few_total_hosts_reason(name, seen_hosts["total"])

        status = get_volume_status(level)
        result = "above" if status == CheckStatus.PASSED else "below"
        return metadata_converter.status_reason(
            name, seen_hosts["seen"], seen_hosts["total"], seen_hosts["percent"], result
        )

    metadata = json.dumps(
        {
            "switch": {
                "status": get_status(netmon._LEVEL_SWITCH),
                "connectivity": drop_none(
                    {
                        "status": get_connectivity_status(netmon._LEVEL_SWITCH),
                        "reason": (
                            get_connectivity_reason(netmon._LEVEL_SWITCH, switch_data["name"], get_or_none(switch_data))
                        ),
                        "alive": switch_data["alive"],
                        "connectivity": get_or_none(switch_data),
                    }
                ),
            },
            "queue": {
                "status": get_status(netmon._LEVEL_QUEUE),
                "connectivity": drop_none(
                    {
                        "status": get_connectivity_status(netmon._LEVEL_QUEUE),
                        "reason": (
                            get_connectivity_reason(netmon._LEVEL_QUEUE, switch_data["queue"], get_or_none(queue_data))
                        ),
                        "alive": queue_data["alive"],
                        "connectivity": get_or_none(queue_data),
                    }
                ),
                "volume": {
                    "status": get_volume_status(netmon._LEVEL_QUEUE),
                    "reason": get_volume_reason(
                        netmon._LEVEL_QUEUE, switch_data["queue"], seen_hosts_data["hosts"]["queue"]
                    ),
                },
            },
            "datacenter": {
                "status": get_status(netmon._LEVEL_DATACENTER),
                "connectivity": drop_none(
                    {
                        "status": get_connectivity_status(netmon._LEVEL_DATACENTER),
                        "reason": (
                            get_connectivity_reason(netmon._LEVEL_DATACENTER, switch_data["dc"], get_or_none(dc_data))
                        ),
                        "alive": dc_data["alive"],
                        "connectivity": get_or_none(dc_data),
                    }
                ),
                "volume": {
                    "status": get_volume_status(netmon._LEVEL_DATACENTER),
                    "reason": get_volume_reason(
                        netmon._LEVEL_DATACENTER, switch_data["dc"], seen_hosts_data["hosts"]["dc"]
                    ),
                },
            },
            "timestamp": alive_data["generated"],
        }
    )

    return metadata
