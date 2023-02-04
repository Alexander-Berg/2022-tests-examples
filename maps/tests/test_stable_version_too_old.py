from datetime import datetime, timedelta

import pytest
import pytz

from maps.garden.libs_server.module.storage_interface import ModuleReleaseInfo
from maps.garden.tools.module_monitoring.lib.stable_version_too_old import generate_stable_version_too_old_events

NOW = datetime(2020, 9, 21, 14, 17, 51, tzinfo=pytz.utc)


class _FakeModuleManager:
    def __init__(self, release_infos):
        self.release_infos = release_infos

    def get_all_modules_release_info(self):
        return self.release_infos


def test_empty_release_infos():
    events = generate_stable_version_too_old_events(_FakeModuleManager({}))
    assert not list(events)


@pytest.mark.freeze_time(NOW)
def test_ok():
    release_infos = {
        "same_version_module": {
            "stable": {
                "module_version": "1111",
                "released_at": (NOW - timedelta(days=30)).isoformat(),
            },
            "testing": {
                "module_version": "1111",
                "released_at": (NOW - timedelta(days=30)).isoformat(),
            },
        },
        "just_released_module": {
            "stable": {
                "module_version": "2222",
                "released_at": (NOW - timedelta(days=30)).isoformat(),
            },
            "testing": {
                "module_version": "2223",
                "released_at": (NOW - timedelta(hours=4)).isoformat(),
            },
        },
        "warn_module": {
            "stable": {
                "module_version": "3333",
                "released_at": (NOW - timedelta(days=30)).isoformat(),
            },
            "testing": {
                "module_version": "3334",
                "released_at": (NOW - timedelta(days=10)).isoformat(),
            },
        },
        "crit_module": {
            "stable": {
                "module_version": "4444",
                "released_at": (NOW - timedelta(days=30)).isoformat(),
            },
            "testing": {
                "module_version": "4445",
                "released_at": (NOW - timedelta(days=17)).isoformat(),
            },
        },
    }

    for moudule_name, module_info in release_infos.items():
        for stage in module_info:
            module_info[stage]["module_name"] = moudule_name
            module_info[stage]["sandbox_task_id"] = "123"  # it is not imprortant here

            module_info[stage] = ModuleReleaseInfo.parse_obj(module_info[stage])

    events = generate_stable_version_too_old_events(
        module_manager=_FakeModuleManager(release_infos),
    )
    return list(events)
