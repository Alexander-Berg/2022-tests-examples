import pytest

from infra.walle.server.tests.lib.expert_util import (
    mock_rack_hosts,
    mock_rack_topology,
    get_hostname_by_location,
    MOCKED_UNIT_RANGES,
    MOCK_SYSTEM,
    MOCKED_RACK_MODEL,
)
from infra.walle.server.tests.lib.util import monkeypatch_config
from walle.expert.rack_topology import (
    get_rack_topology,
    RACK_TOPOLOGY_COLD_CACHE_TTL,
    RackTopology,
    save_rack_topology,
    normalize_hostname,
)
from walle.models import monkeypatch_timestamp
from walle.util.db_cache import DbCacheTTLExpired, DbCacheNotFound


@pytest.fixture
def mp_rack_map(mp):
    monkeypatch_config(
        mp,
        "rack_map.{}".format(MOCKED_RACK_MODEL),
        {"systems": [MOCK_SYSTEM], "slot_ranges": [{"min": 1, "max": 21}, {"min": 28, "max": 48}]},
    )


def test_ttl_expired(walle_test, mp):
    rack_topology = mock_rack_topology(MOCKED_UNIT_RANGES)
    monkeypatch_timestamp(mp, 1)
    save_rack_topology(rack_topology)
    monkeypatch_timestamp(mp, 1 + RACK_TOPOLOGY_COLD_CACHE_TTL)
    with pytest.raises(DbCacheTTLExpired):
        get_rack_topology(rack_topology.aggregate)


def test_non_existing(walle_test):
    with pytest.raises(DbCacheNotFound):
        get_rack_topology("non-existing")


def test_save_load_rack_topology(walle_test):
    aggregate = "mock-aggregate"
    save_rack_topology(mock_rack_topology(aggregate=aggregate))
    assert get_rack_topology(aggregate) == mock_rack_topology(aggregate=aggregate)


@pytest.mark.parametrize("system", ["N/A", None])
def test_unknown_rack_topology_identification(walle_test, mp_rack_map, system):
    queue = "queue"
    rack = "rack"
    aggregate = "mock-aggregate"
    rack_topology = RackTopology.from_hosts(
        aggregate, mock_rack_hosts(walle_test, unit_ranges=MOCKED_UNIT_RANGES, system=system)
    )
    assert rack_topology.rack_model is None
    assert rack_topology.total_ranges == 1
    test_hosts_ranges = {}
    for unit_range in MOCKED_UNIT_RANGES:
        for unit in range(unit_range[0], unit_range[1] + 1):
            test_hosts_ranges[normalize_hostname(get_hostname_by_location(queue, rack, unit))] = 0
    assert rack_topology.hosts_ranges == test_hosts_ranges


def test_rack_topology_identification(walle_test, mp_rack_map):
    queue = "queue"
    rack = "rack"
    aggregate = "mock-aggregate"
    rack_topology = RackTopology.from_hosts(
        aggregate, mock_rack_hosts(walle_test, unit_ranges=MOCKED_UNIT_RANGES, system=MOCK_SYSTEM)
    )
    assert rack_topology.rack_model == MOCKED_RACK_MODEL
    assert rack_topology.total_ranges == 2
    test_hosts_ranges = {}
    index = 0
    for unit_range in MOCKED_UNIT_RANGES:
        for unit in range(unit_range[0], unit_range[1] + 1):
            test_hosts_ranges[normalize_hostname(get_hostname_by_location(queue, rack, unit))] = index
        index += 1
    assert rack_topology.hosts_ranges == test_hosts_ranges
