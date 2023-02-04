from collections import namedtuple

import pytest
import requests
import six
from requests import Response, codes

from infra.walle.server.tests.lib.util import monkeypatch_function, monkeypatch_method
from walle.clients import bot
from walle.clients.network.netmap import (
    _get_actualization_time,
    _get_netmap,
    _parse_racktables_data,
    _parse_mac_to_switch_mappings,
    Netmap,
)
from walle.clients.network.racktables_client import RacktablesClient
from walle.clients.racktables import InternalRacktablesError, RacktablesError, RacktablesSwitchInfo


@pytest.mark.parametrize(
    ["last_modified", "check_val"], [("Tue, 15 Oct 2019 12:45:26 GMT", 1571143526), (None, None), ("", None)]
)
def test_get_actualization_time(last_modified, check_val):
    response = Response()
    if last_modified is not None:
        response.headers["Last-Modified"] = last_modified
    result = _get_actualization_time(response)
    assert result == check_val


class TestGetNetmap:
    def test_get_netmap_successfuly(self, mp):
        response = Response()
        response.headers["Last-Modified"] = "Tue, 15 Oct 2019 12:45:26 GMT"
        response.status_code = codes.ok
        monkeypatch_function(mp, func=requests.request, module=requests, return_value=response)

        result = _get_netmap("L12.fvt")

        assert result == (response, 1571143526)

    def test_get_not_modified_netmap(self, mp):
        response = Response()
        response.headers["Last-Modified"] = "Tue, 15 Oct 2019 12:45:26 GMT"
        response.status_code = codes.not_modified
        if_modified_since = 1
        monkeypatch_function(mp, func=requests.request, module=requests, return_value=response)

        result = _get_netmap("L12.fvt", if_modified_since=if_modified_since)

        assert result == (None, if_modified_since)

    def test_get_netmap_with_not_ok_status_code(self, mp):
        response = Response()
        response.headers["Last-Modified"] = "Tue, 15 Oct 2019 12:45:26 GMT"
        response.status_code = codes.bad_request
        monkeypatch_function(mp, func=requests.request, module=requests, return_value=response)

        with pytest.raises(InternalRacktablesError):
            _get_netmap("L12.fvt")


class TestParseRacktablesData:
    def test_parse_with_empty_fields(self):
        with pytest.raises(ValueError):
            list(_parse_racktables_data(b""))

    def test_parse_data_successfully(self):
        data = [b"1 2 3", b"a b c"]
        fields = ["z", "x", "y"]
        RowClass = namedtuple("RowClass_ZXY", ["line"] + fields)
        check_val = [
            RowClass(six.ensure_str(data[0]), "1", "2", "3"),
            RowClass(six.ensure_str(data[1]), "a", "b", "c"),
        ]
        assert check_val == list(_parse_racktables_data(data, fields=fields))

    def test_len_fields_not_equal_len_columns(self):
        data = [b"1 2"]
        fields = ["z", "x", "y"]
        with pytest.raises(RacktablesError):
            list(_parse_racktables_data(data, fields=fields))


class TestParseMacToSwitchMappings:
    def test_parse_data_successfully(self, mp):
        content = ["1 don't_need 2/3 4", "5 don't_need 6/7 8" ""]
        monkeypatch_method(mp, method=Response.iter_lines, obj=Response, return_value=content)
        monkeypatch_method(mp, method=RacktablesClient.is_interconnect_switch, obj=RacktablesClient, return_value=False)

        result = _parse_mac_to_switch_mappings(Response())
        check_val = (
            {
                1: RacktablesSwitchInfo(switch='2', port='3', int_mac=1, timestamp=-3596),
                5: RacktablesSwitchInfo(switch='6', port='7', int_mac=5, timestamp=-3592),
            },
            {('2', '3'): (1,), ('6', '7'): (5,)},
        )
        assert check_val == result

    def test_parse_data_with_wrong_format(self, mp):
        content = ["1 don't_need 23 4"]
        monkeypatch_method(mp, method=Response.iter_lines, obj=Response, return_value=content)

        with pytest.raises(RacktablesError):
            _parse_mac_to_switch_mappings(Response())

    def test_skip_interconnected_switch(self, mp):
        content = ["1 don't_need 2/3 4"]
        monkeypatch_method(mp, method=Response.iter_lines, obj=Response, return_value=content)
        monkeypatch_method(mp, method=RacktablesClient.is_interconnect_switch, obj=RacktablesClient, return_value=True)

        result = _parse_mac_to_switch_mappings(Response())
        assert ({}, {}) == result

    def test_replace_switch_with_older_timestamp(self, mp):
        content = ["1 don't_need 2/3 4", "1 don't_need 3/3 8" ""]
        monkeypatch_method(mp, method=Response.iter_lines, obj=Response, return_value=content)
        monkeypatch_method(mp, method=RacktablesClient.is_interconnect_switch, obj=RacktablesClient, return_value=False)

        result = _parse_mac_to_switch_mappings(Response())
        check_val = ({1: RacktablesSwitchInfo(switch='3', port='3', int_mac=1, timestamp=-3592)}, {('3', '3'): (1,)})
        assert check_val == result


def test_get_mac_to_inv_mapping(mp):
    mocked_bot_info = [
        {"inv": 1, "ipmi_mac": 2, "macs": [3, 4]},
        {"inv": 5, "ipmi_mac": 6, "macs": [7, 8]},
    ]
    monkeypatch_function(mp, func=bot.iter_hosts_info, module=bot, return_value=mocked_bot_info)

    check_val = {2: (1, True), 3: (1, False), 4: (1, False), 6: (5, True), 7: (5, False), 8: (5, False)}
    assert check_val == Netmap._get_mac_to_inv_mapping()
