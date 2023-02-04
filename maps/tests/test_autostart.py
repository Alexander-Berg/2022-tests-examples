import datetime as dt
import pytest

from maps.garden.sdk.module_autostart import module_autostart as common
from maps.garden.sdk.test_utils.autostart import create_build
from maps.garden.modules.coverage_geoid_deployment.lib import autostart

PROPERTIES = {"release_name": "202101013"}

COVERAGE_GEOID_BUILD = create_build("coverage_geoid")

FAILED_BUILD = create_build(
    "coverage_geoid_deployment",
    sources=[COVERAGE_GEOID_BUILD],
    properties=PROPERTIES | {"deploy_step": "testing"},
    status=common.BuildStatus.FAILED
)

DEPLOY_TO_TESTING_BUILD = create_build(
    "coverage_geoid_deployment",
    sources=[COVERAGE_GEOID_BUILD],
    properties=PROPERTIES | {"deploy_step": "testing"}
)

DEPLOY_TO_PRESTABLE_BUILD = create_build(
    "coverage_geoid_deployment",
    sources=[COVERAGE_GEOID_BUILD],
    properties=PROPERTIES | {"deploy_step": "prestable"},
    finished_at=dt.datetime(2020, 7, 8, 11, 30, 0, tzinfo=dt.timezone.utc),
)

DEPLOY_TO_STABLE_BUILD = create_build(
    "coverage_geoid_deployment",
    sources=[COVERAGE_GEOID_BUILD],
    properties=PROPERTIES | {"deploy_step": "stable"},
)


def test_new_coverage_geoid_build():
    build_manager = common.BuildManager([COVERAGE_GEOID_BUILD])

    autostart.start_build(None, build_manager)  # Triggers does not matter

    assert build_manager.build_to_create.source_ids == [COVERAGE_GEOID_BUILD.full_id]
    assert build_manager.build_to_create.properties == {"deploy_step": "testing"}


@pytest.mark.parametrize(
    ('triggered_at', 'expected_retry_at'),
    [
        (dt.datetime(2020, 7, 8, 3, 40, 0), dt.datetime(2020, 7, 8, 10, 0, 0)),  # Moscow timezone
        (dt.datetime(2020, 7, 8, 22, 40, 0), dt.datetime(2020, 7, 9, 10, 0, 0)),  # Moscow timezone
    ],
)
def test_working_hours(freezer, triggered_at, expected_retry_at):
    freezer.move_to(autostart.MOSCOW_TZ.localize(triggered_at))

    build_manager = common.BuildManager([
        COVERAGE_GEOID_BUILD,
        DEPLOY_TO_TESTING_BUILD,
    ])
    autostart.start_build(None, build_manager)  # Triggers does not matter

    assert build_manager.retry_at == autostart.MOSCOW_TZ.localize(expected_retry_at)


@pytest.mark.freeze_time(dt.datetime(2020, 7, 8, 12, 40, 0, tzinfo=dt.timezone.utc))
def test_testing_starts_prestable():
    build_manager = common.BuildManager([
        COVERAGE_GEOID_BUILD,
        DEPLOY_TO_TESTING_BUILD,
    ])
    autostart.start_build(None, build_manager)  # Triggers does not matter

    assert build_manager.build_to_create.source_ids == [COVERAGE_GEOID_BUILD.full_id]
    assert build_manager.build_to_create.properties == {"deploy_step": "prestable"}


@pytest.mark.freeze_time(dt.datetime(2020, 7, 8, 12, 40, 0, tzinfo=dt.timezone.utc))
def test_delay_between_prestable_and_stable():
    build_manager = common.BuildManager([
        COVERAGE_GEOID_BUILD,
        DEPLOY_TO_TESTING_BUILD,
        DEPLOY_TO_PRESTABLE_BUILD,
    ])
    autostart.start_build(None, build_manager)  # Triggers does not matter

    assert build_manager.retry_at == dt.datetime(2020, 7, 8, 13, 30, 0, tzinfo=dt.timezone.utc)


@pytest.mark.freeze_time(dt.datetime(2020, 7, 8, 15, 40, 0, tzinfo=dt.timezone.utc))
def test_prestable_starts_stable():
    build_manager = common.BuildManager([
        COVERAGE_GEOID_BUILD,
        DEPLOY_TO_TESTING_BUILD,
        DEPLOY_TO_PRESTABLE_BUILD,
    ])
    autostart.start_build(None, build_manager)  # Triggers does not matter

    assert build_manager.build_to_create.source_ids == [COVERAGE_GEOID_BUILD.full_id]
    assert build_manager.build_to_create.properties == {"deploy_step": "stable"}


@pytest.mark.freeze_time(dt.datetime(2020, 7, 8, 15, 40, 0, tzinfo=dt.timezone.utc))
def test_skip_failed_build():
    build_manager = common.BuildManager([
        FAILED_BUILD
    ])
    autostart.start_build(None, build_manager)  # Triggers does not matter

    assert not build_manager.build_to_create
