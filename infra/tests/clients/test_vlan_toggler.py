import json

import pytest

from infra.walle.server.tests.lib.util import mock_response
from walle.clients.vlan_toggler import (
    VlanTogglerError,
    VlanTogglerPersistentError,
    Response,
    get_port_state,
    switch_port_state,
)

API_URL = "http://vlantoggler.cloud.yandex.net/api/v1.0/"
TOKEN = "token-mock"
SWITCH = "mock-switch"
PORT = "mock-port"
STATE = "prod"
TEST_PARAMS = (
    (
        {"result": {"tor": "some-tor", "interface": "some-intf", "state": "setup"}, "status": "ok", "code": 200},
        200,
        Response(tor="some-tor", interface="some-intf", state="setup"),
        None,
    ),
    (
        {"result": {"tor": "some-tor", "interface": "some-intf", "state": "setup"}, "status": "unknown", "code": 200},
        200,
        None,
        VlanTogglerError,
    ),
    ({}, 400, None, VlanTogglerPersistentError),
    ({}, 500, None, VlanTogglerError),
)


@pytest.fixture(autouse=True)
def mock_config(mp):
    mp.config("vlan_toggler.api_url", API_URL)
    mp.config("vlan_toggler.access_token", TOKEN)


def check_request_args(req_mock, method, data=None, call_idx=0):
    call_args = req_mock.call_args_list[call_idx]
    assert call_args[0] == (method, API_URL)
    assert call_args[1]["headers"]["Authorization"] == "Bearer {}".format(TOKEN)
    if data is not None:
        assert json.loads(call_args[1]["data"]) == data


@pytest.mark.parametrize("response_body,response_code,response,expected_exception", TEST_PARAMS)
def test_get_port_state(mp, mock_config, response_body, response_code, response, expected_exception):
    req_mock = mp.request(mock_response(response_body, status_code=response_code))
    if expected_exception is None:
        assert get_port_state(SWITCH, PORT) == response
    else:
        with pytest.raises(expected_exception):
            get_port_state(SWITCH, PORT)

    check_request_args(req_mock, "GET", {"tor": SWITCH, "interface": PORT})


@pytest.mark.parametrize("response_body,response_code,response,expected_exception", TEST_PARAMS)
def test_switch_port_state(mp, mock_config, response_body, response_code, response, expected_exception):
    req_mock = mp.request(mock_response(response_body, status_code=response_code))
    if expected_exception is None:
        assert switch_port_state(SWITCH, PORT, STATE) == response
    else:
        with pytest.raises(expected_exception):
            switch_port_state(SWITCH, PORT, STATE)

    check_request_args(req_mock, "POST", {"tor": SWITCH, "interface": PORT, "state": STATE})
