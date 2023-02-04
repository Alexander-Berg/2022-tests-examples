import datetime as dt
import pytest
import pytz

from maps.garden.sdk.module_autostart import module_autostart as autostart
from maps.garden.sdk.module_autostart import reduce_starters
from maps.garden.sdk.module_traits import module_traits as mt
from maps.garden.sdk.test_utils.autostart import create_build

NOW = dt.datetime(2022, 3, 15, 12, 00, 0, tzinfo=pytz.utc)

MODULE_TRAITS = mt.ModuleTraits(
    name="merge_masstransit",
    type=mt.ModuleType.REDUCE,
    sources=["convert_gtfs"],
    configs=["rasp_export"],
)


REGION_BERLIN_1_BUILD = create_build(
    "convert_gtfs",
    properties={"shipping_date": "20220115", "region": "berlin"},
    status=autostart.BuildStatus.COMPLETED,
)

REGION_ROME_1_BUILD = create_build(
    "convert_gtfs",
    properties={"shipping_date": "20220115", "region": "rome"},
    status=autostart.BuildStatus.COMPLETED,
)

REGION_VILNIUS_1_BUILD = create_build(
    "convert_gtfs",
    properties={"shipping_date": "20220115", "region": "vilnius"},
    status=autostart.BuildStatus.COMPLETED,
)

REGION_PARIS_1_BUILD = create_build(
    "convert_gtfs",
    properties={"shipping_date": "20220115", "region": "paris"},
    status=autostart.BuildStatus.COMPLETED,
)


REGION_BERLIN_2_REMOVED = create_build(
    "convert_gtfs",
    properties={"shipping_date": "20220215", "region": "berlin"},
    status=autostart.BuildStatus.REMOVED,
)

REGION_VILNIUS_2_REMOVED = create_build(
    "convert_gtfs",
    properties={"shipping_date": "20220215", "region": "vilnius"},
    status=autostart.BuildStatus.REMOVED,
)

REGION_ROME_2_REMOVED = create_build(
    "convert_gtfs",
    properties={"shipping_date": "20220215", "region": "rome"},
    status=autostart.BuildStatus.REMOVED,
)

REGION_PARIS_2_REMOVED = create_build(
    "convert_gtfs",
    properties={"shipping_date": "20220215", "region": "paris"},
    status=autostart.BuildStatus.REMOVED,
)

REGION_ROME_2_RUNNING = create_build(
    "convert_gtfs",
    properties={"shipping_date": "20220215", "region": "rome"},
    status=autostart.BuildStatus.IN_PROGRESS,
)

REGION_PARIS_2_RUNNING = create_build(
    "convert_gtfs",
    properties={"shipping_date": "20220215", "region": "paris"},
    status=autostart.BuildStatus.IN_PROGRESS,
)

REGION_BERLIN_2_BUILD = create_build(
    "convert_gtfs",
    properties={"shipping_date": "20220215", "region": "berlin"},
    status=autostart.BuildStatus.COMPLETED,
)

REGION_ROME_2_BUILD = create_build(
    "convert_gtfs",
    properties={"shipping_date": "20220215", "region": "rome"},
    status=autostart.BuildStatus.COMPLETED,
)

REGION_VILNIUS_2_BUILD = create_build(
    "convert_gtfs",
    properties={"shipping_date": "20220215", "region": "vilnius"},
    status=autostart.BuildStatus.COMPLETED,
)

REGION_PARIS_2_BUILD = create_build(
    "convert_gtfs",
    properties={"shipping_date": "20220215", "region": "paris"},
    status=autostart.BuildStatus.COMPLETED,
)

REGION_BERLIN_3_BUILD = create_build(
    "convert_gtfs",
    properties={"shipping_date": "20220315", "region": "berlin"},
    status=autostart.BuildStatus.COMPLETED,
)

REGION_PARIS_3_BUILD = create_build(
    "convert_gtfs",
    properties={"shipping_date": "20220315", "region": "paris"},
    status=autostart.BuildStatus.COMPLETED,
)


REGION_ROME_3_BUILD = create_build(
    "convert_gtfs",
    properties={"shipping_date": "20220315", "region": "rome"},
    status=autostart.BuildStatus.COMPLETED,
)

REGION_VILNIUS_3_BUILD = create_build(
    "convert_gtfs",
    properties={"shipping_date": "20220315", "region": "vilnius"},
    status=autostart.BuildStatus.COMPLETED,
)

CONFIG_BUILD = create_build(
    "rasp_export",
    properties={"release_name": "20220315-0"},
    status=autostart.BuildStatus.COMPLETED,
)


NORMAL_PREVIOUS_BUILD = create_build(
    "merge_masstransit",
    sources=[
        REGION_ROME_1_BUILD,
        REGION_PARIS_1_BUILD,
        REGION_VILNIUS_2_BUILD,
        REGION_BERLIN_2_BUILD,
    ],
    properties={"release_name": "20220314"},
    status=autostart.BuildStatus.COMPLETED,
)


@pytest.mark.parametrize(
    "trigger_build",
    [
        REGION_BERLIN_3_BUILD,
        REGION_PARIS_3_BUILD,
    ],
)
@pytest.mark.parametrize(
    "regions_config",
    [
        pytest.param(
            {"rome", "paris", "berlin", "vilnius"},
            id="Case: Test full normal",
        ),
        pytest.param(
            {"berlin", "vilnius"},
            id="Case: Test part regions normal",
        ),
    ],
)
def test_normal_case(trigger_build, regions_config):
    """
    Testing normal situation, with part-regions
    """

    _helper = {
        "rome": REGION_ROME_1_BUILD,
        "paris": REGION_PARIS_3_BUILD,
        "berlin": REGION_BERLIN_3_BUILD,
        "vilnius": REGION_VILNIUS_2_BUILD,
    }
    build_manager = autostart.BuildManager(
        [
            REGION_BERLIN_1_BUILD,
            REGION_BERLIN_2_BUILD,
            REGION_BERLIN_3_BUILD,
            REGION_VILNIUS_1_BUILD,
            REGION_VILNIUS_2_BUILD,
            REGION_PARIS_1_BUILD,
            REGION_PARIS_3_BUILD,
            REGION_ROME_1_BUILD,
            CONFIG_BUILD,
            NORMAL_PREVIOUS_BUILD,
        ],
        MODULE_TRAITS,
    )

    reduce_starters.start_with_many_defined_regions_and_last_configs(trigger_build, build_manager, regions_config)

    assert set(build_manager.build_to_create.source_ids) == (
        {_helper[region].full_id for region in regions_config} | {CONFIG_BUILD.full_id}
    )
    assert build_manager.build_to_create.properties is None


def test_non_existing_regions():
    """
    Look at situation where there's sligtly more regions defined
    """

    build_manager = autostart.BuildManager(
        [
            REGION_PARIS_3_BUILD,
            REGION_BERLIN_3_BUILD,
            REGION_VILNIUS_3_BUILD,
            REGION_PARIS_3_BUILD,
            CONFIG_BUILD,
            NORMAL_PREVIOUS_BUILD,
        ],
        MODULE_TRAITS,
    )

    reduce_starters.start_with_many_defined_regions_and_last_configs(
        REGION_PARIS_3_BUILD,
        build_manager,
        {"rome", "paris", "berlin", "vilnius", "budapest", "rotterdam"},
    )

    assert set(build_manager.build_to_create.source_ids) == (
        {
            REGION_PARIS_3_BUILD.full_id,
            REGION_BERLIN_3_BUILD.full_id,
            REGION_VILNIUS_3_BUILD.full_id,
            REGION_PARIS_3_BUILD.full_id,
            CONFIG_BUILD.full_id,
        }
    )
    assert build_manager.build_to_create.properties is None


FORCE_RESTART_SOURCES_BUILD = create_build(
    "merge_masstransit",
    sources=[
        REGION_ROME_2_REMOVED,
        REGION_PARIS_2_REMOVED,
        REGION_VILNIUS_2_REMOVED,
        REGION_BERLIN_2_REMOVED,
    ],
    properties={"release_name": "20220314"},
    status=autostart.BuildStatus.COMPLETED,
)


@pytest.mark.freeze_time(NOW)
@pytest.mark.parametrize(
    ["additional_source", "expected_result"],
    [
        pytest.param(
            [
                REGION_ROME_1_BUILD,
                REGION_BERLIN_2_BUILD,
                REGION_VILNIUS_2_BUILD,
            ],
            [
                REGION_PARIS_1_BUILD,
                REGION_ROME_1_BUILD,
                REGION_BERLIN_2_BUILD,
                REGION_VILNIUS_2_BUILD,
            ],
            id="Only half recounted",
        ),
        pytest.param(
            [
                REGION_BERLIN_2_BUILD,
                REGION_VILNIUS_2_BUILD,
                REGION_PARIS_2_BUILD,
                REGION_ROME_2_BUILD,
            ],
            [
                REGION_PARIS_2_BUILD,
                REGION_ROME_2_BUILD,
                REGION_BERLIN_2_BUILD,
                REGION_VILNIUS_2_BUILD,
            ],
            id="All recounted",
        ),
        pytest.param(
            [
                REGION_BERLIN_2_BUILD,
                REGION_VILNIUS_2_BUILD,
                REGION_PARIS_2_BUILD,
            ],
            [
                REGION_PARIS_2_BUILD,
                REGION_BERLIN_2_BUILD,
                REGION_VILNIUS_2_BUILD,
            ],
            id="Rome `exited the thread`",
        ),
    ],
)
def test_after_force_restart(additional_source, expected_result):
    """
    Test `force-restart` situation when last builds were
    removed to recount data with new module version
    """
    build_manager = autostart.BuildManager(
        [
            REGION_BERLIN_1_BUILD,
            REGION_PARIS_1_BUILD,
            # REGION_ROME_1_BUILD,  # deleted for test 3
            REGION_VILNIUS_1_BUILD,
            REGION_BERLIN_2_REMOVED,
            REGION_ROME_2_REMOVED,
            REGION_PARIS_2_REMOVED,
            REGION_VILNIUS_2_REMOVED,
            CONFIG_BUILD,
            NORMAL_PREVIOUS_BUILD,
        ]
        + additional_source,
        MODULE_TRAITS,
    )

    reduce_starters.start_with_many_defined_regions_and_last_configs(
        REGION_BERLIN_2_BUILD,
        build_manager,
        {"berlin", "paris", "rome", "vilnius"},
    )

    assert set(build_manager.build_to_create.source_ids) == (
        set(map(lambda b: b.full_id, expected_result)) | {CONFIG_BUILD.full_id}
    )
    assert build_manager.build_to_create.properties is None


@pytest.mark.freeze_time(NOW)
def test_after_force_restart_with_running():
    """
    Test that build is not starting while running
    """

    build_manager = autostart.BuildManager(
        [
            REGION_BERLIN_1_BUILD,
            REGION_ROME_1_BUILD,
            REGION_PARIS_1_BUILD,
            REGION_VILNIUS_1_BUILD,
            REGION_BERLIN_2_REMOVED,
            REGION_ROME_2_REMOVED,
            REGION_PARIS_2_REMOVED,
            REGION_VILNIUS_2_REMOVED,
            REGION_BERLIN_2_BUILD,
            REGION_VILNIUS_2_BUILD,
            REGION_PARIS_2_RUNNING,
            REGION_ROME_2_RUNNING,
            CONFIG_BUILD,
            NORMAL_PREVIOUS_BUILD,
        ],
        MODULE_TRAITS,
    )

    reduce_starters.start_with_many_defined_regions_and_last_configs(
        REGION_BERLIN_2_BUILD,
        build_manager,
        {"berlin", "paris", "rome", "vilnius"},
    )

    assert build_manager.build_to_create is None
    assert build_manager.retry_at is None


@pytest.mark.parametrize(
    "add_to_builds",
    [
        [REGION_PARIS_2_RUNNING],
        [REGION_PARIS_2_BUILD],
    ],
)
@pytest.mark.freeze_time(NOW)
def test_defined_only_regions_case_1(add_to_builds):
    """
    Test that we check on running only needed regions
    Simulating masstransit_merge_for_realtime
    """

    build_manager = autostart.BuildManager(
        [
            REGION_BERLIN_2_BUILD,
            REGION_VILNIUS_2_BUILD,
            # REGION_PARIS_2_???,
            REGION_ROME_2_RUNNING,
            CONFIG_BUILD,
            NORMAL_PREVIOUS_BUILD,
        ]
        + add_to_builds,
        MODULE_TRAITS,
    )

    reduce_starters.start_with_many_defined_regions_and_last_configs(
        REGION_VILNIUS_2_BUILD,
        build_manager,
        {"vilnius"},
    )

    assert set(build_manager.build_to_create.source_ids) == {REGION_VILNIUS_2_BUILD.full_id, CONFIG_BUILD.full_id}
    assert build_manager.build_to_create.properties is None


@pytest.mark.parametrize(
    [
        "new_build_expected",
        "add_to_builds",
    ],
    [
        pytest.param(True, [REGION_PARIS_2_BUILD]),
        pytest.param(False, [REGION_PARIS_2_RUNNING]),
    ],
)
@pytest.mark.freeze_time(NOW)
def test_defined_only_regions_case_2(new_build_expected, add_to_builds):
    """
    Test that we check on running only needed regions
    """

    build_manager = autostart.BuildManager(
        [
            REGION_BERLIN_2_BUILD,
            REGION_VILNIUS_2_BUILD,
            # REGION_PARIS_2_???,
            REGION_ROME_2_RUNNING,
            CONFIG_BUILD,
            NORMAL_PREVIOUS_BUILD,
        ]
        + add_to_builds,
        MODULE_TRAITS,
    )

    reduce_starters.start_with_many_defined_regions_and_last_configs(
        REGION_VILNIUS_2_BUILD,
        build_manager,
        {"vilnius", "paris"},
    )

    if not new_build_expected:
        assert build_manager.build_to_create is None
        assert build_manager.retry_at is None
    else:
        assert set(build_manager.build_to_create.source_ids) == {
            REGION_VILNIUS_2_BUILD.full_id,
            add_to_builds[0].full_id,
            CONFIG_BUILD.full_id,
        }
        assert build_manager.build_to_create.properties is None
