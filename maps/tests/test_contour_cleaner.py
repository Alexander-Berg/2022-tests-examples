import collections
from unittest import mock

from maps.garden.libs_server.common.contour_manager import ContourManager, ContourStatus
from maps.garden.libs_server.common.module_event_types import CommonEventType
from maps.garden.libs_server.log_storage.module_event_storage import ModuleEventStorage
from maps.garden.libs_server.common.log_types import LogRecordType
from maps.garden.libs_server.log_storage.module_log_storage import ModuleLogStorage
from maps.pylibs.utils.lib.common import wait_until

from maps.garden.scheduler.lib.contour_cleaner import ContourCleaner


class MockBuilds:
    def __init__(self):
        self.removed = False

    def list_all(self, contour_names):
        if self.removed:
            return []
        return ["test_build"]

    def remove(self, build):
        assert build == "test_build"
        self.removed = True


MockModule = collections.namedtuple("MockModule", ["name", "version"])


class MockModuleManager:
    def __init__(self):
        self.removed = False
        self._module = MockModule(name="test", version="module")

    def get_user_module_versions(self, contour_name):
        return {self._module.name: [self._module]}

    def remove_module_version(self, module_name, module_version):
        assert module_name == self._module.name
        assert module_version == self._module.version
        self.removed = True


class MockSettingsProvider:
    def __init__(self, s3stub_environment_settings):
        self._settings = {
            "yt_servers": {
                "hahn": {
                    "yt_config": {},
                    "prefix": "some_path"
                }
            }
        }
        self._settings.update(s3stub_environment_settings)

    def get_settings(self, contour_name):
        return self._settings


class MockYtClient:
    def __init__(self, builds):
        self.removed = False
        self.builds = builds

    def remove(self, path, recursive=False, force=False):
        assert recursive and force
        assert path == "some_path"
        assert self.builds.removed
        self.removed = True


def test_contour_cleaner(db, s3stub_environment_settings):
    contour_cleaner = ContourCleaner(db, polling_interval_sec=1)

    module_event_storage = ModuleEventStorage(db)
    module_log_storage = ModuleLogStorage(db)

    contour_manager = ContourManager(db)
    contour = contour_manager.create("vasya_test", "vasya")

    module_event_storage.add_common_event(
        contour_name="vasya_test",
        module_name="some_module",
        username="vasya",
        event_type=CommonEventType.AUTOSTART_ENABLED
    )

    module_log_storage.add_log(
        log_type=LogRecordType.SCAN_RESOURCES,
        contour_name="vasya_test",
        module_name="some_module",
        module_version="module_version",
        username="vasya",
        message="message",
        exception=None,
    )

    assert db.contours.count() == 1
    assert db.module_events.count() == 1
    assert db.module_logs.count() == 1

    contour_manager.delete(contour)
    contour = contour_manager.find("vasya_test")
    assert contour.status == ContourStatus.DELETING

    builds = MockBuilds()
    module_manager = MockModuleManager()
    yt_client = MockYtClient(builds)
    contour_settings = MockSettingsProvider(s3stub_environment_settings)

    with mock.patch("maps.garden.libs_server.application.state.build_manager", return_value=builds), \
         mock.patch("maps.garden.libs_server.application.state.module_manager", return_value=module_manager), \
         mock.patch("maps.garden.libs_server.application.state.settings_provider", return_value=contour_settings), \
         mock.patch("maps.garden.libs_server.application.state.module_event_storage", return_value=module_event_storage), \
         mock.patch("maps.garden.libs_server.application.state.module_log_storage", return_value=module_log_storage), \
         mock.patch("maps.garden.scheduler.lib.contour_cleaner.get_yt_client", return_value=yt_client), \
         contour_cleaner:
        assert wait_until(lambda: db.contours.count() == 0)

    assert builds.removed
    assert module_manager.removed
    assert yt_client.removed
    assert db.module_events.count() == 0
    assert db.module_logs.count() == 0
