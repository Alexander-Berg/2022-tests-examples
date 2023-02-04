"""Tests host profiling log API."""

import pytest
import six
import http.client

import walle.clients.eine
from infra.walle.server.tests.lib.util import monkeypatch_config
from walle.clients import eine
from walle.hosts import HostState

MOCK_HOST_INV = 999

TEST_RESPONSE_HOST_JSON = {
    "inventory": str(MOCK_HOST_INV),
    "in_use": True,
    "macs": [],
    "einstellung": {"_id": "589094330fb5b90d2f255ddf"},
}

TEST_RESPONSE_LOG_JSON = [
    {
        "type": "log",
        "einstellung_id": "589094330fb5b90d2f255ddf",
        "date": 1485870140.0,
        "message": "Executing 'r:pxe-switch -c'",
        "created_at": 1485870140.0,
        "_id": "5890943c0fb5b90d35252ee4",
    },
    {
        "type": "log",
        "einstellung_id": "589094330fb5b90d2f255ddf",
        "date": 1485870150.0,
        "message": "Executing 'r:ipmi-fwup'",
        "created_at": 1485870150.0,
        "_id": "589094460fb5b90d35252ee6",
    },
    {
        "type": "setup",
        "date": 1485870152.0,
        "message": "r:ipmi-fwup: VERSION: 0.4p",
        "created_at": 1485870152.0,
        "_id": "5890944859e9f55e7d35855d",
        "einstellung_id": "589094330fb5b90d2f255ddf",
    },
]


TEST_PROFILE_LOG = "\n".join(
    [
        "2017.01.31 16:42:20 | log | Executing 'r:pxe-switch -c'",
        "2017.01.31 16:42:30 | log | Executing 'r:ipmi-fwup'",
        "2017.01.31 16:42:32 | setup | r:ipmi-fwup: VERSION: 0.4p",
    ]
)


TEST_TIME_MAP = {
    1485870140.0: "2017.01.31 16:42:20",
    1485870150.0: "2017.01.31 16:42:30",
    1485870152.0: "2017.01.31 16:42:32",
}


@pytest.fixture
def profile_log_mock(mp):
    monkeypatch_config(mp, "eine.access_token", "access-token-mock")

    side_effect = [TEST_RESPONSE_HOST_JSON]
    mp.method(eine.EineClient._api_request, side_effect=side_effect, obj=eine.EineClient)
    return side_effect


@pytest.mark.parametrize("host_state", HostState.ALL)
def test_get_profile_log(mp, host_state, profile_log_mock, walle_test):
    def profile_format_time(time, format=None):
        return TEST_TIME_MAP[time]

    format_time_mock = mp.function(eine.format_time, module=walle.clients.eine, side_effect=profile_format_time)
    profile_log_mock.append(TEST_RESPONSE_LOG_JSON)

    walle_test.mock_host({"inv": MOCK_HOST_INV, "name": "host-1", "state": host_state})

    result = walle_test.api_client.get("/v1/hosts/{}/profile-log".format(MOCK_HOST_INV))
    assert result.status_code == http.client.OK
    assert format_time_mock.call_count == 3
    assert result.data == six.ensure_binary(TEST_PROFILE_LOG)


def test_get_profile_log__empty(profile_log_mock, walle_test):
    profile_log_mock.append([])
    walle_test.mock_host({"inv": MOCK_HOST_INV, "name": "host-1", "state": HostState.ASSIGNED})

    result = walle_test.api_client.get("/v1/hosts/{}/profile-log".format(MOCK_HOST_INV))
    assert result.status_code == http.client.OK
    assert result.data == b""


def test_get_profile_log__error(profile_log_mock, walle_test):
    error_message = (
        "Einstellung returned an error: Server returned an error: 400 Bad Request. Computer with ID 'host-1' not found."
    )
    profile_log_mock.append(walle.clients.eine.EinePersistentError(error_message))

    walle_test.mock_host({"inv": MOCK_HOST_INV, "name": "host-1", "state": HostState.ASSIGNED})

    result = walle_test.api_client.get("/v1/hosts/{}/profile-log".format(MOCK_HOST_INV))
    assert result.status_code == http.client.INTERNAL_SERVER_ERROR
    assert "Computer with ID 'host-1' not found." in result.json["message"]
