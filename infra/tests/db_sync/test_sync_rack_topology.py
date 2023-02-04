import pytest

from infra.walle.server.tests.lib.expert_util import (
    mock_rack_hosts,
    MOCKED_UNIT_RANGES,
    MOCK_SYSTEM,
    MOCKED_RACK_MODEL,
    mock_rack_topology,
)
from walle.db_sync.rack_topology_sync import _sync
from walle.expert.rack_topology import get_rack_topology, RackTopology
from walle.juggler import get_aggregate_name

MOCK_QUEUE = "queue"
MOCK_RACK = "rack"


@pytest.mark.usefixtures("mp_rack_map")
def test_rack_topology_sync(walle_test, mp_juggler_source):
    hosts = mock_rack_hosts(
        walle_test, unit_ranges=MOCKED_UNIT_RANGES, system=MOCK_SYSTEM, queue=MOCK_QUEUE, rack=MOCK_RACK, save=True
    )
    _sync()
    aggregate = get_aggregate_name(MOCK_QUEUE, MOCK_RACK)
    rack_topology = mock_rack_topology(
        unit_ranges=MOCKED_UNIT_RANGES,
        aggregate=aggregate,
        rack_model=MOCKED_RACK_MODEL,
        queue=MOCK_QUEUE,
        rack=MOCK_RACK,
    )
    assert get_rack_topology(aggregate) == RackTopology.from_hosts(aggregate, hosts) == rack_topology


def test_rack_topology_sync_unknown_systems(walle_test, mp_juggler_source):
    hosts = mock_rack_hosts(walle_test, unit_ranges=[(1, 48)], system=None, queue=MOCK_QUEUE, rack=MOCK_RACK, save=True)
    _sync()
    aggregate = get_aggregate_name(MOCK_QUEUE, MOCK_RACK)
    rack_topology = mock_rack_topology(
        unit_ranges=[(1, 48)], aggregate=aggregate, rack_model=None, queue=MOCK_QUEUE, rack=MOCK_RACK
    )
    assert get_rack_topology(aggregate) == RackTopology.from_hosts(aggregate, hosts) == rack_topology


@pytest.mark.usefixtures("mp_rack_map")
def test_different_queues(walle_test, mp_juggler_source):
    hosts1 = mock_rack_hosts(walle_test, unit_ranges=[(1, 48)], system=None, queue="queue1", rack=MOCK_RACK, save=True)
    hosts2 = mock_rack_hosts(
        walle_test,
        unit_ranges=MOCKED_UNIT_RANGES,
        system=MOCK_SYSTEM,
        queue="queue2",
        rack=MOCK_RACK,
        start_inv_from=len(hosts1),
        save=True,
    )
    aggregate1 = get_aggregate_name("queue1", MOCK_RACK)
    aggregate2 = get_aggregate_name("queue2", MOCK_RACK)
    _sync()
    rack_topology1 = mock_rack_topology(
        unit_ranges=[(1, 48)], aggregate=aggregate1, rack_model=None, queue="queue1", rack=MOCK_RACK
    )
    rack_topology2 = mock_rack_topology(
        unit_ranges=MOCKED_UNIT_RANGES,
        aggregate=aggregate2,
        rack_model=MOCKED_RACK_MODEL,
        queue="queue2",
        rack=MOCK_RACK,
    )
    assert get_rack_topology(aggregate1) == RackTopology.from_hosts(aggregate1, hosts1) == rack_topology1
    assert get_rack_topology(aggregate2) == RackTopology.from_hosts(aggregate2, hosts2) == rack_topology2
