import pytest

from infra.walle.server.tests.lib.util import monkeypatch_config
from walle.clients.bot import _HardwareLocation
from walle.physical_location_tree import (
    get_shortname,
    generate_shortnames_for_location,
    LocationNamesMap,
    LocationSegment,
    NoShortNameError,
)


@pytest.mark.parametrize(
    ["location", "dc_path", "dc_name", "queue_path", "queue_name"],
    [
        (
            _HardwareLocation("RU", "SPB", "AVRORA", "5-floor-c-1-1", "1", "5"),
            "RU|SPB|AVRORA",
            "avrora",
            "RU|SPB|AVRORA|5-floor-c-1-1",
            "avrora-5-flo",
        ),
        (
            _HardwareLocation("RU", "PASTA_SASTA", "A", "1-11.c-3-2.2", "1", "5"),
            "RU|PASTA_SASTA|A",
            "a",
            "RU|PASTA_SASTA|A|1-11.c-3-2.2",
            "a-1-11",
        ),
        (
            _HardwareLocation("FI", "MANKA_FINKA", "PREOBRAZENSKAY_PLOSHAD", "1", "1", "5"),
            "FI|MANKA_FINKA|PREOBRAZENSKAY_PLOSHAD",
            "preobr",
            "FI|MANKA_FINKA|PREOBRAZENSKAY_PLOSHAD|1",
            "preobr-1",
        ),
    ],
)
def test_generate_shortnames_for_location(walle_test, location, dc_path, dc_name, queue_path, queue_name):
    walle_test.location_names_map.mock({"name": dc_name, "path": dc_path}, save=False)
    walle_test.location_names_map.mock({"name": queue_name, "path": queue_path}, save=False)

    location_lookup = LocationNamesMap.get_map()

    generate_shortnames_for_location(location_lookup, location)

    walle_test.location_names_map.assert_equal()


@pytest.mark.parametrize(
    ["location", "level", "result"],
    [
        (_HardwareLocation("RU", "SPB", "AVRORA", "5-floor-c-1-1", "1", "5"), LocationSegment.DATACENTER, "avrora"),
        (_HardwareLocation("RU", "SPB", "AVRORA", "5-floor-c-1-1", "1", "5"), LocationSegment.QUEUE, "avrora-5-flo"),
        (_HardwareLocation("RU", "PASTA_SASTA", "A", "1-11.c-3-2.2", "1", "5"), LocationSegment.DATACENTER, "a"),
        (_HardwareLocation("RU", "PASTA_SASTA", "A", "1-11.c-3-2.2", "1", "5"), LocationSegment.QUEUE, "a-1-11"),
        (
            _HardwareLocation("FI", "MANKA_FINKA", "PREOBRAZENSKAY_PLOSHAD", "1", "1", "5"),
            LocationSegment.DATACENTER,
            "preobr",
        ),
        (
            _HardwareLocation("FI", "MANKA_FINKA", "PREOBRAZENSKAY_PLOSHAD", "1", "1", "5"),
            LocationSegment.QUEUE,
            "preobr-1",
        ),
    ],
)
def test_get_shortname_successfully(walle_test, mp, location, level, result):
    monkeypatch_config(mp, "shortnames.cities_with_disabled_name_autogeneration", [])

    location_lookup = LocationNamesMap.get_map()
    assert get_shortname(location_lookup, location, level) == result


@pytest.mark.parametrize(
    ["location", "level", "result"],
    [
        (_HardwareLocation("RU", "PASTA_SASTA", "A", "1-11.c-3-2.2", "1", "5"), LocationSegment.DATACENTER, "a"),
        (_HardwareLocation("RU", "PASTA_SASTA", "A", "1-11.c-3-2.2", "1", "5"), LocationSegment.QUEUE, "a-1-11"),
        (
            _HardwareLocation("FI", "MANKA_FINKA", "PREOBRAZENSKAY_PLOSHAD", "1", "1", "5"),
            LocationSegment.DATACENTER,
            "preobr",
        ),
        (
            _HardwareLocation("FI", "MANKA_FINKA", "PREOBRAZENSKAY_PLOSHAD", "1", "1", "5"),
            LocationSegment.QUEUE,
            "preobr-1",
        ),
    ],
)
def test_try_to_get_shortname_for_disabled_city(walle_test, mp, location, level, result):
    monkeypatch_config(mp, "shortnames.cities_with_disabled_name_autogeneration", ["PASTA_SASTA", "MANKA_FINKA"])
    location_lookup = LocationNamesMap.get_map()

    with pytest.raises(NoShortNameError):
        get_shortname(location_lookup, location, level)


@pytest.mark.parametrize(
    ["location", "logical_datacenter", "level", "result"],
    [
        (
            _HardwareLocation("RU", "SPB", "AVRORA", "5-floor-c-1-1", "1", "5"),
            "SPBX",
            LocationSegment.DATACENTER,
            "spbx",
        ),
        (
            _HardwareLocation("RU", "SPB", "AVRORA", "5-floor-c-1-1", "1", "5"),
            "SPBX",
            LocationSegment.QUEUE,
            "spbx-5-flo",
        ),
    ],
)
def test_get_shortname_with_logical_dc(walle_test, mp, location, logical_datacenter, level, result):
    monkeypatch_config(mp, "shortnames.cities_with_disabled_name_autogeneration", [])

    location_lookup = LocationNamesMap.get_map()
    assert get_shortname(location_lookup, location, level, logical_datacenter=logical_datacenter) == result
