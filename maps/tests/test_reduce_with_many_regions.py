import datetime as dt
import pytest
import pytz

from maps.garden.sdk.module_autostart import module_autostart as autostart
from maps.garden.sdk.module_autostart import reduce_starters
from maps.garden.sdk.module_traits import module_traits as mt
from maps.garden.sdk.test_utils.autostart import create_build

NOW = dt.datetime(2020, 10, 9, 13, 40, 0, tzinfo=pytz.utc)

MODULE_TRAITS = mt.ModuleTraits(
    name="carparks",
    type=mt.ModuleType.REDUCE,
    sources=["ymapsdf"],
    configs=["altay"],
)

REGION_SAA_0_BUILD_REMOVED = create_build(
    "ymapsdf",
    properties={"shipping_date": "20201102", "region": "saa"},
    status=autostart.BuildStatus.REMOVED,
)

REGION_SAA_1_BUILD = create_build(
    "ymapsdf",
    properties={"shipping_date": "20201103", "region": "saa"},
    status=autostart.BuildStatus.COMPLETED,
)

REGION_AAO_1_BUILD = create_build(
    "ymapsdf",
    properties={"shipping_date": "20201103", "region": "aao"},
    status=autostart.BuildStatus.COMPLETED,
)

REGION_TR_1_BUILD = create_build(
    "ymapsdf",
    properties={"shipping_date": "20201103", "region": "tr"},
    status=autostart.BuildStatus.COMPLETED,
)

REGION_SAA_2_BUILD = create_build(
    "ymapsdf",
    properties={"shipping_date": "20201104", "region": "saa"},
    status=autostart.BuildStatus.COMPLETED,
)

REGION_AAO_2_BUILD = create_build(
    "ymapsdf",
    properties={"shipping_date": "20201104", "region": "aao"},
    status=autostart.BuildStatus.COMPLETED,
)

REGION_TR_2_BUILD_IN_PROGRESS = create_build(
    "ymapsdf",
    properties={"shipping_date": "20201104", "region": "tr"},
    status=autostart.BuildStatus.IN_PROGRESS,
)

REGION_SAA_3_BUILD_CANCELLED = create_build(
    "ymapsdf",
    properties={"shipping_date": "20201104", "region": "saa"},
    status=autostart.BuildStatus.CANCELLED,
)

REGION_AAO_3_BUILD_FAILED = create_build(
    "ymapsdf",
    properties={"shipping_date": "20201104", "region": "aao"},
    status=autostart.BuildStatus.FAILED,
    finished_at=NOW - dt.timedelta(hours=2),
)

REGION_SAA_4_BUILD_REMOVED = create_build(
    "ymapsdf",
    properties={"shipping_date": "20201104", "region": "saa"},
    status=autostart.BuildStatus.REMOVED,
)

REGION_AAO_4_BUILD_IN_PROGRESS = create_build(
    "ymapsdf",
    properties={"shipping_date": "20201104", "region": "saa"},
    status=autostart.BuildStatus.IN_PROGRESS,
)

REGION_SAA_5_BUILD_FAILED_FRESH = create_build(
    "ymapsdf",
    properties={"shipping_date": "20201104", "region": "aao"},
    status=autostart.BuildStatus.FAILED,
    finished_at=NOW,
)

CONFIG_BUILD = create_build(
    "altay",
    properties={"release_name": "20200603"},
    status=autostart.BuildStatus.COMPLETED,
)

PREVIOUS_BUILD = create_build(
    "carparks",
    sources=[
        REGION_SAA_1_BUILD,
        REGION_AAO_1_BUILD,
    ],
    properties={"release_name": "20201103"},
    status=autostart.BuildStatus.COMPLETED,
)


@pytest.mark.freeze_time(NOW)
@pytest.mark.parametrize(
    "trigger_build",
    [
        REGION_SAA_1_BUILD,
        REGION_SAA_2_BUILD,
        REGION_SAA_3_BUILD_CANCELLED,
        REGION_AAO_3_BUILD_FAILED,
        REGION_SAA_4_BUILD_REMOVED,
        CONFIG_BUILD,
        PREVIOUS_BUILD,
    ],
)
def test_success(trigger_build):
    """
    Trigger doesn't matter: autostarter always tooks the last sources
    """
    build_manager = autostart.BuildManager(
        [
            REGION_SAA_0_BUILD_REMOVED,
            REGION_SAA_1_BUILD,
            REGION_AAO_1_BUILD,
            REGION_TR_1_BUILD,
            REGION_SAA_2_BUILD,
            REGION_AAO_2_BUILD,
            REGION_TR_2_BUILD_IN_PROGRESS,
            REGION_SAA_3_BUILD_CANCELLED,
            REGION_AAO_3_BUILD_FAILED,
            REGION_SAA_4_BUILD_REMOVED,
            CONFIG_BUILD,
            PREVIOUS_BUILD,
        ],
        MODULE_TRAITS,
    )

    reduce_starters.start_with_many_regions_and_last_configs(trigger_build, build_manager)

    assert set(build_manager.build_to_create.source_ids) == {
        REGION_SAA_2_BUILD.full_id,
        REGION_AAO_2_BUILD.full_id,
        CONFIG_BUILD.full_id,
    }
    assert build_manager.build_to_create.properties is None


@pytest.mark.freeze_time(NOW)
@pytest.mark.parametrize(
    "trigger_build",
    [
        REGION_SAA_0_BUILD_REMOVED,  # too old
        REGION_TR_1_BUILD,  # wrong region
    ],
)
def test_no_build(trigger_build):
    build_manager = autostart.BuildManager(
        [
            REGION_SAA_0_BUILD_REMOVED,
            REGION_SAA_1_BUILD,
            REGION_AAO_1_BUILD,
            REGION_TR_1_BUILD,
            REGION_SAA_2_BUILD,
            REGION_AAO_2_BUILD,
            CONFIG_BUILD,
            PREVIOUS_BUILD,
        ],
        MODULE_TRAITS,
    )

    reduce_starters.start_with_many_regions_and_last_configs(trigger_build, build_manager)

    assert build_manager.build_to_create is None
    assert build_manager.retry_at is None


@pytest.mark.parametrize(
    "trigger_build",
    [
        REGION_SAA_2_BUILD,
        CONFIG_BUILD,
        PREVIOUS_BUILD,
    ],
)
def test_running_source(trigger_build):
    """
    No matter what build is a trigger - do not start a new build
    if there is at least one running ymapsdf.
    """
    build_manager = autostart.BuildManager(
        [
            REGION_SAA_1_BUILD,
            REGION_AAO_1_BUILD,
            REGION_TR_1_BUILD,
            REGION_SAA_2_BUILD,
            REGION_AAO_2_BUILD,
            REGION_AAO_4_BUILD_IN_PROGRESS,
            CONFIG_BUILD,
            PREVIOUS_BUILD,
        ],
        MODULE_TRAITS,
    )

    reduce_starters.start_with_many_regions_and_last_configs(trigger_build, build_manager)

    assert build_manager.build_to_create is None
    assert build_manager.retry_at is None


@pytest.mark.freeze_time(NOW)
def test_failed_source():
    build_manager = autostart.BuildManager(
        [
            REGION_SAA_1_BUILD,
            REGION_AAO_1_BUILD,
            REGION_TR_1_BUILD,
            REGION_SAA_2_BUILD,
            REGION_AAO_2_BUILD,
            REGION_SAA_5_BUILD_FAILED_FRESH,
            CONFIG_BUILD,
            PREVIOUS_BUILD,
        ],
        MODULE_TRAITS,
    )

    reduce_starters.start_with_many_regions_and_last_configs(REGION_SAA_5_BUILD_FAILED_FRESH, build_manager)

    assert build_manager.build_to_create is None
    assert build_manager.retry_at == NOW + dt.timedelta(hours=1)
