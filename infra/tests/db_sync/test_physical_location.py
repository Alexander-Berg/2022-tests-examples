"""Tests physical location tree synchronization"""

import pytest

from infra.walle.server.tests.lib.util import monkeypatch_config
from walle.clients.bot import HostLocationInfo, HardwareLocation
from walle.db_sync.physical_location import bot, _sync, _sync_hardware, MissingShortNames
from walle.hosts import HostLocation, HostState, HostStatus
from walle.models import timestamp
from walle.physical_location_tree import LocationTree, get_shortname, LocationSegment, LocationNamesMap


@pytest.mark.parametrize("city", ["new-city", None])
@pytest.mark.parametrize("status", HostStatus.ALL)
def test_synchronization(walle_test, mp, monkeypatch_timestamp, status, city):
    host = walle_test.mock_host(
        dict(state=HostState.ASSIGNED, status=status, inv=1, name="one", location=HostLocation())
    )
    country, datacenter, queue, rack, unit = "new-country", "new-datacenter", "new-queue", "new-rack", "new-unit"

    bot_location = HardwareLocation(
        country=country, city=city, datacenter=datacenter, queue=queue, rack=rack, unit=unit
    )
    location_info = HostLocationInfo(inv=1, name=None, location=bot_location)

    mp.function(bot.get_host_location_info, return_value=[location_info])
    mp.function(bot.get_locations, return_value={1: location_info.location})

    expected_timestamp = timestamp()
    _sync()

    location_lookup = LocationNamesMap.get_map()
    host.location = HostLocation(
        country=country,
        city=city,
        datacenter=datacenter,
        queue=queue,
        rack=rack,
        unit=unit,
        physical_timestamp=expected_timestamp,
        short_datacenter_name=get_shortname(location_lookup, bot_location, LocationSegment.DATACENTER),
        short_queue_name=get_shortname(location_lookup, bot_location, LocationSegment.QUEUE),
    )

    walle_test.hosts.assert_equal()

    location_trees = list(LocationTree.objects)
    assert len(location_trees) == 1
    location_tree = location_trees[0]

    assert location_tree.timestamp == expected_timestamp
    assert len(location_tree.countries) == 1
    assert location_tree.countries[0].name == country

    assert len(location_tree.countries[0].cities) == 1
    assert location_tree.countries[0].cities[0].name == (city or "-")

    assert len(location_tree.countries[0].cities[0].datacenters) == 1
    assert location_tree.countries[0].cities[0].datacenters[0].name == datacenter

    assert len(location_tree.countries[0].cities[0].datacenters[0].queues) == 1
    assert location_tree.countries[0].cities[0].datacenters[0].queues[0].name == queue

    assert len(location_tree.countries[0].cities[0].datacenters[0].queues[0].racks) == 1
    assert location_tree.countries[0].cities[0].datacenters[0].queues[0].racks[0].name == rack


def test_synchronization_names_fail(walle_test, mp):
    """Test fail on sync without defining datacenter and queue names in LocationNamesMap"""
    monkeypatch_config(mp, "shortnames.cities_with_disabled_name_autogeneration", ["new-city"])
    country, city, datacenter, queue, rack, unit = (
        "new-country",
        "new-city",
        "new-datacenter",
        "new-queue",
        "new-rack",
        "new-unit",
    )

    walle_test.mock_host(
        dict(
            inv=1,
            name="one",
            location=HostLocation(
                country=country,
                city=city,
                datacenter=datacenter,
                queue=queue,
                rack=rack,
                unit=unit,
                physical_timestamp=timestamp(),
            ),
        )
    )

    location_info = HostLocationInfo(
        inv=1,
        name=None,
        location=HardwareLocation(country=country, city=city, datacenter=datacenter, queue=queue, rack=rack, unit=unit),
    )

    mp.function(bot.get_host_location_info, return_value=[location_info])
    mp.function(bot.get_locations, return_value={1: location_info.location})

    with pytest.raises(MissingShortNames):
        _sync()

    walle_test.hosts.assert_equal()


def test_synchronization_logical_dataceneter(walle_test, mp):
    project_id, logical_datacenter = "project-id", "DCX"
    expected_dc_short_name = "dcx"
    walle_test.mock_project(dict(id=project_id, logical_datacenter=logical_datacenter))

    country, city, datacenter, queue, rack, unit = "IS", "SOMECITY", "GAMMA", "VLA-99", "rack-1", "unit-1"
    bot_location = HardwareLocation(
        country=country, city=city, datacenter=datacenter, queue=queue, rack=rack, unit=unit
    )
    location_lookup = LocationNamesMap.get_map()
    host = walle_test.mock_host(
        dict(
            inv=1,
            name="one",
            project=project_id,
            location=HostLocation(
                country=country,
                city=city,
                datacenter=datacenter,
                queue=queue,
                rack=rack,
                unit=unit,
                short_datacenter_name=get_shortname(location_lookup, bot_location, LocationSegment.DATACENTER),
                short_queue_name=get_shortname(location_lookup, bot_location, LocationSegment.QUEUE),
            ),
        )
    )

    now = timestamp()
    _sync_hardware({host.inv: bot_location}, now)

    host.location.logical_datacenter = logical_datacenter
    host.location.short_datacenter_name = get_shortname(
        location_lookup, bot_location, LocationSegment.DATACENTER, logical_datacenter=logical_datacenter
    )
    assert host.location.short_datacenter_name == expected_dc_short_name

    host.location.short_queue_name = get_shortname(
        location_lookup, bot_location, LocationSegment.QUEUE, logical_datacenter=logical_datacenter
    )
    host.location.physical_timestamp = now

    # verify that logical_datacenter option is applied on sync
    walle_test.hosts.assert_equal()

    # verify that logical_datacenter is not cleaned on sync
    _sync_hardware({host.inv: bot_location}, now)
    walle_test.hosts.assert_equal()
