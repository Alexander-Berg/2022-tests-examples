"""Tests for OK client."""

from unittest.mock import call

import pytest
from requests.utils import CaseInsensitiveDict

from infra.walle.server.tests.lib.util import monkeypatch_config, monkeypatch_request, mock_response
from sepelib.core import constants
from walle import constants as walle_constants
from walle.clients import qloud
from walle.util.misc import drop_none

MOCK_QLOUD_TOKEN = "mock"
USER_AGENT = "Wall-E/" + walle_constants.version
AUTHORIZATION = "OAuth {}".format(MOCK_QLOUD_TOKEN)


@pytest.fixture(autouse=True)
def qloud_config(monkeypatch):
    monkeypatch_config(monkeypatch, "qloud.access_token", MOCK_QLOUD_TOKEN)
    monkeypatch_config(
        monkeypatch,
        "qloud.api_map",
        {"ext": "qloud-ext.yandex-team.ru", "pre": "qloud.yandex-team.ru", "test": "qloud-test.yandex-team.ru"},
    )
    monkeypatch_config(
        monkeypatch,
        "qloud.segment_project_map",
        {
            "ext": "qloud",
            "pre": "qloud-prestable",
            "test": "qloud-testing",
        },
    )
    monkeypatch_config(monkeypatch, "qloud.host_resolver_api", "qloud-host-resolver.n.yandex-team.ru")


def mock_qloud_host_resolver_response_data(installation=None, segment=None):
    if installation is None or segment is None:
        return {}
    response_dict = {"installation": installation, "loc": "{}.{}".format(installation, segment), "segment": segment}
    return response_dict


def mock_qloud_host_response_data(hostname, mock_dict, drop=False):
    response_dict = {
        "fqdn": hostname,
        "state": "UP",
        "stateMtimeMs": 1578478626826,
        "automationEnabled": True,
        "comment": "{\"transitionFrom\":\"PROBATION\","
        "\"transitionTo\":\"UP\","
        "\"message\":\"updated by HFSM at 2020-01-08T16:17:06.865517\","
        "\"transitionTime\":1578478626826}",
        "cpu": 31.0,
        "memoryBytes": 265089515520,
        "bandwidthMegabitsPerSec": 10000,
        "segment": "",
        "dataCenter": "MAN",
        "rack": "3C7",
        "line": "MAN-3#B.1.08",
        "cpuName": "Xeon E5-2650 v2",
        "ipv6": "2a02:06b8:0c01:0898:0000:0604:ba56:e8e4",
        "nocDC": "[Mantsala-1.3]",
        "metaUpdatedMtimeMs": 1580824660531,
        "botStatusId": 100171944,
        "slots": [],
        "instances": [],
        "disks": [
            {
                "type": "HDD",
                "device": "/dev/disk/by-uuid/4afdb2bc-9bd4-44c4-914c-2b1ac0a55a83",
                "deviceNumber": "8:34",
                "size": 20000538624,
                "allocationMountPoint": [],
            },
            {
                "type": "HDD",
                "device": "/dev/disk/by-uuid/05f2fe54-769f-4944-8929-5818c5538809",
                "deviceNumber": "8:36",
                "size": 3974770982912,
                "allocationMountPoint": ["/place"],
            },
            {
                "type": "HDD",
                "device": "/dev/disk/by-uuid/de652f0b-2d4f-4000-ac05-e0d3a6236d58",
                "deviceNumber": "8:35",
                "size": 4999610368,
                "allocationMountPoint": [],
            },
            {
                "type": "SSD",
                "device": "/dev/disk/by-uuid/b8504ab2-08d1-44c8-a542-c1eba8274d18",
                "deviceNumber": "9:5",
                "size": 1798070861824,
                "allocationMountPoint": ["/ssd"],
            },
            {
                "type": "SSD",
                "device": "/dev/sda2",
                "deviceNumber": "8:2",
                "size": 899169648640,
                "allocationMountPoint": [],
            },
            {
                "type": "SSD",
                "device": "/dev/sdb2",
                "deviceNumber": "8:18",
                "size": 899169648640,
                "allocationMountPoint": [],
            },
        ],
        "defaultAllocationMountPoint": "/place",
    }
    response_dict.update(mock_dict)

    if drop:
        response_dict = drop_none(response_dict)
    return [response_dict]


def test_find_host(monkeypatch, qloud_config):
    client = qloud.get_client()
    name = "mock-host"
    monkeypatch_request(
        monkeypatch,
        mock_response(mock_qloud_host_response_data(name, {"segment": "common", "state": qloud.QloudHostStates.UP})),
    )
    host = client.find_host(name)
    assert host.name == name
    assert host.state == qloud.QloudHostStates.UP
    assert host.segment == "ext.common"
    assert host.installation == "ext"


def test_find_host_resolver(monkeypatch, qloud_config):
    name = "mock-host"
    client = qloud.get_client()
    installation, segment = "ext", "routers"
    monkeypatch_request(monkeypatch, mock_response(mock_qloud_host_resolver_response_data(installation, segment)))
    assert client.find_host_installation_segment(name) == (installation, segment)


def test_not_find_host_resolver(monkeypatch, qloud_config):
    name = "mock-host"
    client = qloud.get_client()
    monkeypatch_request(monkeypatch, mock_response(mock_qloud_host_resolver_response_data(), status_code=404))
    installation, segment = client.find_host_installation_segment(name)
    assert installation is None and segment is None


@pytest.mark.parametrize("drop", [True, False])
@pytest.mark.parametrize(
    "cpu,mem,noc_dc,rack,line,dataCenter,expected",
    [
        (1, 1, "ndc", "rack", "line", "dc", True),
        (1, 1, "ndc", "rack", "line", None, False),
        (1, 1, "ndc", "rack", None, "dc", False),
        (1, 1, "ndc", None, "line", "dc", False),
        (1, 1, None, "rack", "line", "dc", False),
        (1, None, "ndc", "rack", "line", "dc", False),
        (None, 1, "ndc", "rack", "line", "dc", False),
        (1, 0, "ndc", "rack", "line", "dc", False),
        (0, 1, "ndc", "rack", "line", "dc", False),
    ],
)
def test_host_fullness(monkeypatch, qloud_config, cpu, mem, noc_dc, rack, line, dataCenter, expected, drop):
    name = "mock-host"
    client = qloud.get_client()
    monkeypatch_request(
        monkeypatch,
        mock_response(
            mock_qloud_host_response_data(
                name,
                {
                    "cpu": cpu,
                    "memoryBytes": mem,
                    "nocDC": noc_dc,
                    "rack": rack,
                    "line": line,
                    "dataCenter": dataCenter,
                    "segment": "common",
                    "state": qloud.QloudHostStates.UP,
                },
                drop,
            ),
        ),
    )
    host = client.find_host(name)
    assert expected == host.is_data_filled


def test_remove(monkeypatch, qloud_config):
    client = qloud.get_client()
    name = "mock-host"
    mock = monkeypatch_request(monkeypatch, mock_response(mock_qloud_host_response_data(name, {})))
    host = client.find_host(name)
    client.remove_qloud_host(host)
    assert mock.mock_calls == [
        call(
            "GET",
            str("http://qloud-host-resolver.n.yandex-team.ru/v1/host/{}".format(name)),
            data=None,
            headers=CaseInsensitiveDict({"User-Agent": USER_AGENT}),
            params=None,
            timeout=constants.NETWORK_TIMEOUT,
        ),
        call(
            "GET",
            "https://qloud-ext.yandex-team.ru/api/v1/hosts/search",
            data=None,
            headers=CaseInsensitiveDict({"User-Agent": USER_AGENT, "Authorization": AUTHORIZATION}),
            params={"fqdn": name},
            timeout=constants.NETWORK_TIMEOUT,
        ),
        call(
            "DELETE",
            str("https://qloud-ext.yandex-team.ru/api/v1/hosts/{}".format(name)),
            data=None,
            headers=CaseInsensitiveDict({"User-Agent": USER_AGENT, "Authorization": AUTHORIZATION}),
            params=None,
            timeout=constants.NETWORK_TIMEOUT,
        ),
    ]


def test_not_update_metadata(monkeypatch, qloud_config):
    client = qloud.get_client()
    name = "mock-host"
    mock = monkeypatch_request(monkeypatch, mock_response({}, status_code=404))
    client.update_host_metadata(name)
    assert mock.mock_calls == [
        call(
            "GET",
            str("http://qloud-host-resolver.n.yandex-team.ru/v1/host/{}".format(name)),
            data=None,
            headers=CaseInsensitiveDict({"User-Agent": USER_AGENT}),
            params=None,
            timeout=constants.NETWORK_TIMEOUT,
        ),
        call(
            "GET",
            "https://qloud-ext.yandex-team.ru/api/v1/hosts/search",
            data=None,
            headers=CaseInsensitiveDict({"User-Agent": USER_AGENT, "Authorization": AUTHORIZATION}),
            params={"fqdn": name},
            timeout=constants.NETWORK_TIMEOUT,
        ),
        call(
            "GET",
            "https://qloud.yandex-team.ru/api/v1/hosts/search",
            data=None,
            headers=CaseInsensitiveDict({"User-Agent": USER_AGENT, "Authorization": AUTHORIZATION}),
            params={"fqdn": name},
            timeout=constants.NETWORK_TIMEOUT,
        ),
        call(
            "GET",
            "https://qloud-test.yandex-team.ru/api/v1/hosts/search",
            data=None,
            headers=CaseInsensitiveDict({"User-Agent": USER_AGENT, "Authorization": AUTHORIZATION}),
            params={"fqdn": name},
            timeout=constants.NETWORK_TIMEOUT,
        ),
    ]


def test_update_metadata(monkeypatch, qloud_config):
    client = qloud.get_client()
    name = "mock-host"
    mock = monkeypatch_request(monkeypatch, mock_response(mock_qloud_host_response_data(name, {})))
    client.update_host_metadata(name)
    assert mock.mock_calls == [
        call(
            "GET",
            str("http://qloud-host-resolver.n.yandex-team.ru/v1/host/{}".format(name)),
            data=None,
            headers=CaseInsensitiveDict({"User-Agent": USER_AGENT}),
            params=None,
            timeout=constants.NETWORK_TIMEOUT,
        ),
        call(
            "GET",
            "https://qloud-ext.yandex-team.ru/api/v1/hosts/search",
            data=None,
            headers=CaseInsensitiveDict({"User-Agent": USER_AGENT, "Authorization": AUTHORIZATION}),
            params={"fqdn": name},
            timeout=constants.NETWORK_TIMEOUT,
        ),
        call(
            "POST",
            "https://qloud-ext.yandex-team.ru/api/v1/admin/hostupdate",
            data=str("[\"{}\"]".format(name)),
            headers=CaseInsensitiveDict(
                {"User-Agent": USER_AGENT, "Authorization": AUTHORIZATION, "Content-Type": "application/json"}
            ),
            params=None,
            timeout=constants.NETWORK_TIMEOUT,
        ),
    ]


def test_set_state(monkeypatch, qloud_config):
    client = qloud.get_client()
    name = "mock-host"
    state = qloud.QloudHostStates.DOWN
    comment = "mock-comment"
    mock = monkeypatch_request(monkeypatch, mock_response(mock_qloud_host_response_data(name, {})))
    client.set_host_state(name, state, comment)
    assert mock.mock_calls == [
        call(
            "GET",
            str("http://qloud-host-resolver.n.yandex-team.ru/v1/host/{}".format(name)),
            data=None,
            headers=CaseInsensitiveDict({"User-Agent": USER_AGENT}),
            params=None,
            timeout=constants.NETWORK_TIMEOUT,
        ),
        call(
            "GET",
            "https://qloud-ext.yandex-team.ru/api/v1/hosts/search",
            data=None,
            headers=CaseInsensitiveDict({"User-Agent": USER_AGENT, "Authorization": AUTHORIZATION}),
            params={"fqdn": name},
            timeout=constants.NETWORK_TIMEOUT,
        ),
        call(
            "PUT",
            str("https://qloud-ext.yandex-team.ru/api/v1/hosts/{}".format(name)),
            data=None,
            headers=CaseInsensitiveDict({"User-Agent": USER_AGENT, "Authorization": AUTHORIZATION}),
            params={"comment": comment, "state": state},
            timeout=constants.NETWORK_TIMEOUT,
        ),
    ]


def test_set_same_state(monkeypatch, qloud_config):
    client = qloud.get_client()
    name = "mock-host"
    state = qloud.QloudHostStates.UP
    comment = "mock-comment"
    mock = monkeypatch_request(monkeypatch, mock_response(mock_qloud_host_response_data(name, {"state": state})))
    client.set_host_state(name, state, comment)
    assert mock.mock_calls == [
        call(
            "GET",
            str("http://qloud-host-resolver.n.yandex-team.ru/v1/host/{}".format(name)),
            data=None,
            headers=CaseInsensitiveDict({"User-Agent": USER_AGENT}),
            params=None,
            timeout=constants.NETWORK_TIMEOUT,
        ),
        call(
            "GET",
            "https://qloud-ext.yandex-team.ru/api/v1/hosts/search",
            data=None,
            headers=CaseInsensitiveDict({"User-Agent": USER_AGENT, "Authorization": AUTHORIZATION}),
            params={"fqdn": name},
            timeout=constants.NETWORK_TIMEOUT,
        ),
    ]


def test_add(monkeypatch, qloud_config):
    client = qloud.get_client()
    name = "mock-host"
    mock = monkeypatch_request(monkeypatch, mock_response(mock_qloud_host_response_data(name, {})))
    client.add_host(name, 'ext.common')
    assert mock.mock_calls[0][1][0] == "POST"
    assert mock.mock_calls[0][1][1] == "https://qloud-ext.yandex-team.ru/api/v1/hosts"
    assert mock.mock_calls[0][2]["params"] == {"boxFqdn": "mock-host", "hardwareSegment": "common"}
    assert mock.mock_calls[0][2]["headers"]["Authorization"] == "OAuth mock"

    assert mock.mock_calls == [
        call(
            "POST",
            "https://qloud-ext.yandex-team.ru/api/v1/hosts",
            data=None,
            headers=CaseInsensitiveDict({"User-Agent": USER_AGENT, "Authorization": AUTHORIZATION}),
            params={"boxFqdn": "mock-host", "hardwareSegment": "common"},
            timeout=constants.NETWORK_TIMEOUT,
        )
    ]
