import datetime as dt
import pytest
import mongomock
from unittest import mock
from maps.garden.tools.module_monitoring.lib.autostart import generate_autostart_events
from maps.garden.libs_server.module.module_settings_storage import AutostartSettings, ModuleSettingsStorage

from maps.garden.sdk.module_traits.module_traits import ModuleTraits, ModuleType


CURRENT_TIME = dt.datetime(2021, 5, 25, 16, 30, 0, tzinfo=dt.timezone.utc)
CURRENT_TIME_MINUS_ONE_HOUR = CURRENT_TIME - dt.timedelta(hours=1)
CURRENT_TIME_MINUS_THREE_HOUR = CURRENT_TIME - dt.timedelta(hours=3)
CURRENT_TIME_MINUS_FIVE_HOUR = CURRENT_TIME - dt.timedelta(hours=5)


def _get_all_autostart_settings(*args, **kwargs):
    time_points = [
        CURRENT_TIME_MINUS_ONE_HOUR,
        CURRENT_TIME_MINUS_THREE_HOUR,
        CURRENT_TIME_MINUS_FIVE_HOUR
    ]

    settings = {
        "another_module": AutostartSettings.parse_obj({
            "enabled": True
        }),
    }

    module_traits = [
        ModuleTraits(
            name="module_without_settings",
            type=ModuleType.MAP,
        ),

        ModuleTraits(
            name="another_module",
            type=ModuleType.MAP,
        ),
    ]

    for i, some_time_point in enumerate(time_points):
        module_name = f"test_module_name{i}"
        settings[module_name] = AutostartSettings.parse_obj({
            "enabled": False,
            "by": "very_bad_user",
            "at": some_time_point,
        })
        module_traits.append(
            ModuleTraits(
                name=module_name,
                type=ModuleType.MAP,
            ),
        )

    return settings, module_traits


@pytest.mark.freeze_time(CURRENT_TIME)
@mock.patch("maps.garden.libs_server.module.module_settings_storage.ModuleSettingsStorage.get_all_autostart_settings",
            autospec=True)
def test_generate_autostart_events(get_all_autostart_settings_mock):
    settings, traits = _get_all_autostart_settings()
    get_all_autostart_settings_mock.return_value = settings
    db = mongomock.MongoClient(tz_aware=True).db
    settings_storage = ModuleSettingsStorage(db)
    return list(generate_autostart_events("magical_mystery_tour", settings_storage, traits))
