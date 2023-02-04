import pytest

import json

from maps.garden.sdk.module_autostart import module_autostart
from maps.garden.sdk.test_utils import autostart

from maps.garden.libs.matrix_router_data_builder.autostart \
    import start_with_last_n_sources, start_with_last_sources_picked_from_config


def test_do_not_run_with_incomplete_data():
    builds = [
        autostart.create_build(
            module_name="source",
            status=module_autostart.BuildStatus.COMPLETED),
        autostart.create_build(
            module_name="source",
            status=module_autostart.BuildStatus.COMPLETED),
    ]

    build_manager = module_autostart.BuildManager(builds)
    with pytest.raises(RuntimeError):
        start_with_last_n_sources(3, wait_for_source_completion=False)(
            builds[-1], build_manager)


def test_trigger_by_failed_build():
    builds = [
        autostart.create_build(
            module_name="source",
            status=module_autostart.BuildStatus.COMPLETED),
        # We may have been waiting for the completion of this build. If it
        # fails, use the previous one.
        autostart.create_build(
            module_name="source",
            status=module_autostart.BuildStatus.FAILED),
    ]

    build_manager = module_autostart.BuildManager(builds)
    start_with_last_n_sources(1, wait_for_source_completion=False)(
        builds[-1], build_manager)
    assert build_manager.build_to_create.source_ids == [builds[0].full_id]


def test_use_last_n_completed_builds():
    builds = [
        autostart.create_build(
            module_name="source",
            status=module_autostart.BuildStatus.COMPLETED),
        autostart.create_build(
            module_name="source",
            status=module_autostart.BuildStatus.FAILED),
        autostart.create_build(
            module_name="source",
            status=module_autostart.BuildStatus.IN_PROGRESS),
        autostart.create_build(
            module_name="source",
            status=module_autostart.BuildStatus.COMPLETED),
    ]

    build_manager = module_autostart.BuildManager(builds)
    start_with_last_n_sources(2, wait_for_source_completion=False)(
        builds[-1], build_manager)
    assert build_manager.build_to_create.source_ids == [
        builds[0].full_id, builds[3].full_id]


def test_wait_for_completion():
    builds = [
        autostart.create_build(
            module_name="source",
            status=module_autostart.BuildStatus.COMPLETED),
        autostart.create_build(
            module_name="source",
            status=module_autostart.BuildStatus.IN_PROGRESS),
    ]

    build_manager = module_autostart.BuildManager(builds)
    start_with_last_n_sources(1, wait_for_source_completion=True)(
        builds[0], build_manager)
    assert build_manager.build_to_create is None


def test_take_latest_shipping_dates():
    builds = [
        autostart.create_build(
            module_name="source",
            properties={"shipping_date": "2000-01-02"},
            status=module_autostart.BuildStatus.COMPLETED),
        autostart.create_build(
            module_name="source",
            properties={"shipping_date": "2000-01-04"},
            status=module_autostart.BuildStatus.COMPLETED),
        autostart.create_build(
            module_name="source",
            properties={"shipping_date": "2000-01-03"},
            status=module_autostart.BuildStatus.COMPLETED),
        autostart.create_build(
            module_name="source",
            properties={"shipping_date": "2000-01-01"},
            status=module_autostart.BuildStatus.COMPLETED),
    ]

    build_manager = module_autostart.BuildManager(builds)
    start_with_last_n_sources(2, wait_for_source_completion=True)(
        builds[-1], build_manager)
    assert build_manager.build_to_create.source_ids == [
        builds[2].full_id, builds[1].full_id]


def test_do_not_run_both_with_and_without_shipping_date():
    builds = [
        autostart.create_build(
            module_name="source",
            status=module_autostart.BuildStatus.COMPLETED),
        autostart.create_build(
            module_name="source",
            properties={"shipping_date": "2000-01-01"},
            status=module_autostart.BuildStatus.COMPLETED),
    ]

    build_manager = module_autostart.BuildManager(builds)
    with pytest.raises(RuntimeError):
        start_with_last_n_sources(1, wait_for_source_completion=True)(
            builds[-1], build_manager)


def test_use_more_dates():
    builds = [
        autostart.create_build(
            module_name="source",
            status=module_autostart.BuildStatus.COMPLETED,
            properties={'shipping_date': '2021-11-10'}),
        autostart.create_build(
            module_name="source",
            status=module_autostart.BuildStatus.COMPLETED,
            properties={'shipping_date': '2021-11-11'}),
        autostart.create_build(
            module_name="source",
            status=module_autostart.BuildStatus.COMPLETED,
            properties={'shipping_date': '2021-11-12'}),
        autostart.create_build(
            module_name="source",
            status=module_autostart.BuildStatus.COMPLETED,
            properties={'shipping_date': '2021-11-13'}),
        autostart.create_build(
            module_name="source",
            status=module_autostart.BuildStatus.COMPLETED,
            properties={'shipping_date': '2021-11-14'}),
        autostart.create_build(
            module_name="source",
            status=module_autostart.BuildStatus.COMPLETED,
            properties={'shipping_date': '2021-11-15'}),
        autostart.create_build(
            module_name="source",
            status=module_autostart.BuildStatus.COMPLETED,
            properties={'shipping_date': '2021-11-16'}),
        autostart.create_build(
            module_name="source",
            status=module_autostart.BuildStatus.COMPLETED,
            properties={'shipping_date': '2021-11-17'}),
        autostart.create_build(
            module_name="source",
            status=module_autostart.BuildStatus.COMPLETED,
            properties={'shipping_date': '2021-11-18'}),
        autostart.create_build(
            module_name="source",
            status=module_autostart.BuildStatus.COMPLETED,
            properties={'shipping_date': '2021-11-19'}),
    ]

    config = {
        "1": {
            "dates_per_weekday": 1,
            "calculate_day_of_week": True,
            "country_for_weekdays": 225,
            "skip_dates": ["11-19"],
        },
    }
    build_manager = module_autostart.BuildManager(builds)
    start_with_last_sources_picked_from_config(10, wait_for_source_completion=False, config=json.dumps(config))(builds[-1], build_manager)
    assert build_manager.build_to_create.source_ids == [builds[i].full_id for i in (2, 3, 4, 5, 6, 7, 8)]
