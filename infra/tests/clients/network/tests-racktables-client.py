from unittest.mock import Mock

import pytest

from infra.walle.server.tests.lib.util import monkeypatch_function
from walle.clients import racktables
from walle.clients.network import racktables_client


def test_get_switch_ports(mp):
    response_data = ["switch1\tport1", "switch2\tport2", "switch2\tport3"]
    response = Mock()
    response.attach_mock(Mock(return_value=response_data), "iter_lines")
    monkeypatch_function(mp, func=racktables._raw_request_to_racktables, module=racktables, return_value=response)

    result = racktables_client.RacktablesClient.get_switch_ports()

    assert {"switch2": ["port2", "port3"], "switch1": ["port1"]} == result


@pytest.mark.usefixtures("disable_caches")
def test_interconnect_switch_set(mp):
    switch_set = {1, 2, 3}
    monkeypatch_function(
        mp, func=racktables_client._get_interconnect_switch_list, module=racktables_client, return_value=switch_set
    )

    result = racktables_client.RacktablesClient._interconnect_switch_set()

    assert switch_set == result


def test_get_interconnect_switch_list(walle_test, mp):
    response_data = ["switch1", "switch2", "switch3.yndx.net"]
    response = Mock()
    response.attach_mock(Mock(return_value=response_data), "split")
    monkeypatch_function(mp, func=racktables.request, module=racktables, return_value=response)

    check_val = ["switch1", "switch2", "switch3"]
    result = racktables_client._get_interconnect_switch_list()

    assert check_val == result


@pytest.mark.parametrize(["response_data", "result"], [("", []), ("1", [1]), ("1\n2", [1, 2]), ("1\n2 ", [1, 2])])
def test_get_owned_vlans(walle_test, mp, response_data, result):
    monkeypatch_function(mp, func=racktables.request, module=racktables, return_value=response_data)

    assert racktables_client.get_owned_vlans("test") == result
