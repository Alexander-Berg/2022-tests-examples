import pytest

from maps.garden.sdk.module_autostart import module_autostart as autostart
from maps.garden.sdk.module_autostart import map_starters
from maps.garden.sdk.module_traits import module_traits as mt

MODULE_TRAITS = mt.ModuleTraits(
    name="ymapsdf",
    type=mt.ModuleType.MAP,
    sources=["ymapsdf_src"],
    configs=["extra_poi_bundle"],
)


def test_success():
    builds = [
        autostart.Build(
            full_id=autostart.BuildId(module_name="ymapsdf_src", id=1),
            source_ids=[],
            properties={"shipping_date": "202101013", "region": "cis1"},
            status=autostart.BuildStatus.COMPLETED,
        ),
        autostart.Build(
            full_id=autostart.BuildId(module_name="ymapsdf_src", id=2),
            source_ids=[],
            properties={"shipping_date": "202101013", "region": "cis2"},
            status=autostart.BuildStatus.COMPLETED,
        ),
        autostart.Build(
            full_id=autostart.BuildId(module_name="extra_poi_bundle", id=1),
            source_ids=[],
            properties={"release_name": "111"},
            status=autostart.BuildStatus.COMPLETED,
        ),
        autostart.Build(
            full_id=autostart.BuildId(module_name="extra_poi_bundle", id=2),
            source_ids=[],
            properties={"release_name": "111"},
            status=autostart.BuildStatus.COMPLETED,
        ),
    ]

    build_manager = autostart.BuildManager(builds, MODULE_TRAITS)

    map_starters.start_with_last_configs(builds[1], build_manager)

    assert build_manager.build_to_create.source_ids == [
        builds[1].full_id,  # Trigger build
        builds[3].full_id,  # Last config build
    ]
    assert build_manager.build_to_create.properties is None


def test_absent_config():
    trigger_build = autostart.Build(
        full_id=autostart.BuildId(module_name="ymapsdf_src", id=1),
        source_ids=[],
        properties={"shipping_date": "202101013", "region": "cis1"},
        status=autostart.BuildStatus.COMPLETED,
    )

    build_manager = autostart.BuildManager([trigger_build], MODULE_TRAITS)

    with pytest.raises(RuntimeError):
        map_starters.start_with_last_configs(trigger_build, build_manager)


def test_failed_build():
    trigger_build = autostart.Build(
        full_id=autostart.BuildId(module_name="ymapsdf_src", id=1),
        source_ids=[],
        properties={"shipping_date": "202101013", "region": "cis1"},
        status=autostart.BuildStatus.FAILED,
    )

    build_manager = autostart.BuildManager([trigger_build], MODULE_TRAITS)

    map_starters.start_with_last_configs(trigger_build, build_manager)

    assert build_manager.build_to_create is None
