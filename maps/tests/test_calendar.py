import copy
import pytest
from datetime import datetime

from maps.garden.libs.auth.auth_client import TVMServiceAuth
from maps.garden.libs_server.autostart.calendar import CalendarManager


SERVER_SETTINGS = {
    "calendars": [
        22354,
    ],
    "tvm_client": {
        "client_id": 2025200,
        "token": "token",
    }
}


NOW = datetime(2020, 12, 25, 17, 45, 00)
URL = "https://calendar-api.tools.yandex.net/internal/get_events?layerId=22354"
URL_2 = "https://calendar-api.tools.yandex.net/internal/get_events?layerId=248122"


@pytest.mark.freeze_time(NOW)
def test_calendar_manager_without_events(mocker, requests_mock):
    mocker.patch.object(TVMServiceAuth, "get_garden_server_auth_headers", return_value={})
    requests_mock.get(URL, json={"events": [], "lastUpdateTs": 1608724477199, "failedLayers": []})

    manager = CalendarManager(SERVER_SETTINGS)

    assert manager._calendars
    assert not manager.has_events()


@pytest.mark.freeze_time(NOW)
def test_calendar_manager_with_events(mocker, requests_mock):
    server_settings = copy.deepcopy(SERVER_SETTINGS)
    server_settings["calendars"].append(248122)

    requests_mock.get(URL, json={"events": [], "lastUpdateTs": 1608724477199, "failedLayers": []})

    requests_mock.get(URL_2, json={"events": [{"id": 53716961, "name": "some_name"}, {"id": 53716962, "name": "some_name_with_IVA"}], "lastUpdateTs": 1608907549196, "failedLayers": []})

    mocker.patch.object(TVMServiceAuth, "get_garden_server_auth_headers", return_value={})
    manager = CalendarManager(server_settings)

    assert manager._calendars
    assert manager.has_events()


@pytest.mark.freeze_time(NOW)
def test_calendar_manager_with_events_in_myt_and_iva(mocker, requests_mock):
    server_settings = copy.deepcopy(SERVER_SETTINGS)
    server_settings["calendars"].append(248122)

    requests_mock.get(URL, json={"events": [], "lastUpdateTs": 1608724477199, "failedLayers": []})

    requests_mock.get(URL_2, json={"events": [{"id": 53716961, "name": "some_name_with_MYT"}, {"id": 53716962, "name": "some_name_with_IVA"}], "lastUpdateTs": 1608907549196, "failedLayers": []})

    mocker.patch.object(TVMServiceAuth, "get_garden_server_auth_headers", return_value={})
    manager = CalendarManager(server_settings)

    assert manager._calendars
    assert not manager.has_events()
