import datetime as dt
import logging
import mongomock
import pytz
from unittest import mock

from maps.garden.sdk.module_traits.module_traits import (
    BuildsLimit,
    ModuleTraits,
    ModuleType,
)
from maps.garden.sdk.utils.contour import default_contour_name
from maps.garden.sdk.core import AutotestsFailedError

from maps.garden.libs_server.module.module_manager import ModuleManager
from maps.garden.libs_server.module.storage_interface import ModuleReleaseInfo
from maps.garden.libs_server.graph.request_storage import FailedTask
from maps.garden.libs_server.build.build_defs import (
    Build,
    BuildStatus,
    BuildStatusString,
)
from maps.garden.libs_server.build.builds_storage import BuildsStorage
from maps.garden.libs_server.log_storage.module_log_storage import ModuleLogStorage
from maps.garden.libs_server.module.module_settings_storage import ModuleSettingsStorage

from maps.garden.tools.module_monitoring.lib import all_events

MODULE_NAME = 'test'

TRAITS = ModuleTraits(
    name=MODULE_NAME,
    type=ModuleType.MAP,
    build_limits=[BuildsLimit(max=2)],
)


class _FakeContourManager:
    def find_system_contour_names(self):
        return [default_contour_name()]


class _FakeDanglingResourcesManager:
    def get_all(self):
        yield from []


def test_generate_build_events(freezer, supervisor_pidfile_ctime, mocker, module_manager_config):
    module_manager_mock = mock.create_autospec(ModuleManager)
    module_manager_mock.get_all_modules_traits.return_value = [TRAITS]
    module_manager_mock.get_all_modules_release_info.return_value = {
        MODULE_NAME: {
            "stable": ModuleReleaseInfo.parse_obj({
                "module_name": "some_name",
                "sandbox_task_id": "123",
                "module_version": "1111",
                "released_at": dt.datetime.now(pytz.utc).isoformat(),
            }),
            "testing": ModuleReleaseInfo.parse_obj({
                "module_name": "some_name",
                "sandbox_task_id": "124",
                "module_version": "1111",
                "released_at": dt.datetime.now(pytz.utc).isoformat(),
            }),
        }
    }

    mocker.patch.object(all_events, "ModuleManager", module_manager_mock)

    now = supervisor_pidfile_ctime + dt.timedelta(hours=1)
    freezer.move_to(now)

    db = mongomock.MongoClient(tz_aware=True).db
    builds_storage = BuildsStorage(db)
    module_settings_storage = ModuleSettingsStorage(db)
    build_id = 1

    def add(
        status_string,
        hours_ago=0,
        failed_tasks=None,
    ):
        created_at = dt.datetime.now(pytz.utc) - dt.timedelta(hours=hours_ago)

        nonlocal build_id
        builds_storage.save(Build(
            id=build_id,
            name=MODULE_NAME,
            contour_name=default_contour_name(),
            status=BuildStatus(
                string=status_string,
                start_time=created_at,
                finish_time=created_at,
                failed_tasks=failed_tasks or [],
            ),
            created_at=created_at,
            extras={},
        ))

        build_id += 1

    add(BuildStatusString.COMPLETED, hours_ago=5)
    add(BuildStatusString.COMPLETED, hours_ago=3)

    def assert_events_correct(status_by_check):

        events = all_events.generate_all_events(
            ui_hostname="localhost",
            logger=logging.getLogger(),
            contour_manager=_FakeContourManager(),
            builds_storage=builds_storage,
            module_settings_storage=module_settings_storage,
            module_log_storage=ModuleLogStorage(db),
            dangling_resources_storage=_FakeDanglingResourcesManager(),
            server_settings=module_manager_config,
            db=db
        )
        for event in events:
            assert event["status"] == status_by_check[event["service"]], event

    expected_statuses = {
        'latest_build_status': 'OK',
        'build_limits': 'OK',
        'autotests_failed': 'OK',
        'hanging_builds': 'OK',
        'module_traits_consistency_unittest': 'OK',
        'stable_version_too_old': 'OK',
        'dangling_resources': 'OK',
    }
    assert_events_correct(expected_statuses)

    add(BuildStatusString.COMPLETED, hours_ago=2)
    add(BuildStatusString.FAILED, hours_ago=1)
    expected_statuses = {
        'latest_build_status': 'CRIT',
        'build_limits': 'WARN',
        'autotests_failed': 'OK',
        'hanging_builds': 'OK',
        'module_traits_consistency_unittest': 'OK',
        'stable_version_too_old': 'OK',
        'dangling_resources': 'OK',
    }
    assert_events_correct(expected_statuses)

    failed_task = FailedTask(error=b'', exception_name=AutotestsFailedError.__name__)
    add(BuildStatusString.FAILED, hours_ago=0, failed_tasks=[failed_task])
    add(BuildStatusString.WAITING, hours_ago=1, failed_tasks=[failed_task])
    expected_statuses = {
        'latest_build_status': 'OK',
        'build_limits': 'WARN',
        'autotests_failed': 'CRIT',
        'hanging_builds': 'CRIT',
        'module_traits_consistency_unittest': 'OK',
        'stable_version_too_old': 'OK',
        'dangling_resources': 'OK',
    }
    freezer.move_to(now + dt.timedelta(seconds=1))
    assert_events_correct(expected_statuses)
