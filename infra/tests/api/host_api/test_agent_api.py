"""Tests Wall-E agent reporting API."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import TestCase, monkeypatch_method
from sepelib.core import constants
from walle import constants as walle_constants
from walle.clients.network.racktables_client import RacktablesClient
from walle.hosts import HostState, HostLocation, HostMessage
from walle.models import timestamp
from walle.util.misc import drop_none

REPORT_MACS = {
    "01:02:03:04:05:06": False,
    "11:12:13:14:15:16": True,
}

REPORT_SWITCHES = [
    {
        "switch": "report-switch",
        "port": "report-port",
        "time": timestamp() + constants.DAY_SECONDS,
    }
]

HOST_MACS = list(REPORT_MACS)
ACTIVE_MAC = "11:12:13:14:15:16"
OTHER_MACS = ["ff:ff:ff:ff:ff:ff"]

HOST_IPS = ["fe80::8ac0:5d16:c7d1:ff92", "193.211.100.5"]


@pytest.fixture
def test(monkeypatch_timestamp, request):
    return TestCase.create(request)


def test_unknown(test):
    _mock_host_with_network(test)

    result = test.api_client.put(
        "/v1/hosts/unknown/agent-report",
        data={
            "macs": REPORT_MACS,
            "switches": REPORT_SWITCHES,
            "version": "0.0",
        },
    )

    assert result.status_code == http.client.OK
    assert result.json == {"result": "Ignore report: host unknown is not registered in Wall-E."}

    test.hosts.assert_equal()
    test.host_network.assert_equal()


def test_no_macs(test):
    host, _ = _mock_host_with_network(test)

    result = test.api_client.put(
        "/v1/hosts/{}/agent-report".format(host.name),
        data={
            "switches": REPORT_SWITCHES,
            "version": "0.0",
        },
    )

    assert result.status_code == http.client.OK
    assert result.json == {"result": "Reject report: host mocked-0.mock authentication failed."}

    test.hosts.assert_equal()
    test.host_network.assert_equal()


def test_unauthorized(test):
    _mock_host_with_network(test, {"inv": 0})
    host, _ = _mock_host_with_network(test, {"inv": 1, "macs": OTHER_MACS})

    result = test.api_client.put(
        "/v1/hosts/{}/agent-report".format(host.name),
        data={
            "macs": REPORT_MACS,
            "switches": REPORT_SWITCHES,
            "version": "0.0",
        },
    )

    assert result.status_code == http.client.OK
    assert result.json == {"result": "Reject report: host mocked-1.mock authentication failed."}

    test.hosts.assert_equal()
    test.host_network.assert_equal()


def test_no_active_macs(test):
    host, _ = _mock_host_with_network(test)

    result = test.api_client.put(
        "/v1/hosts/{}/agent-report".format(host.name),
        data={
            "macs": {mac: False for mac in REPORT_MACS},
            "switches": REPORT_SWITCHES,
            "version": "0.0",
        },
    )

    assert result.status_code == http.client.OK
    assert result.json == {"result": "Reject report: host mocked-0.mock authentication failed."}

    test.hosts.assert_equal()
    test.host_network.assert_equal()
    test.projects.assert_equal()


def test_few_active_macs(test):
    host, host_network = _mock_host_with_network(
        test,
        {"active_mac": OTHER_MACS[0], "active_mac_source": walle_constants.MAC_SOURCE_EINE},
        {
            "active_mac": OTHER_MACS[0],
            "active_mac_time": timestamp() - 1,
            "active_mac_source": walle_constants.MAC_SOURCE_EINE,
        },
    )

    result = test.api_client.put(
        "/v1/hosts/{}/agent-report".format(host.name),
        data={
            "macs": {mac: True for mac in REPORT_MACS},
            "version": "0.0",
        },
    )

    assert result.status_code == http.client.OK
    assert result.json == {"result": "The report has been processed."}

    # ignore this update, keep old info.

    test.hosts.assert_equal()
    test.host_network.assert_equal()
    test.projects.assert_equal()


def test_active_mac_actualization(test):
    host, host_network = _mock_host_with_network(test)

    result = test.api_client.put(
        "/v1/hosts/{}/agent-report".format(host.name),
        data={
            "macs": REPORT_MACS,
            "version": "0.0",
        },
    )

    assert result.status_code == http.client.OK
    assert result.json == {"result": "The report has been processed."}

    host.active_mac = ACTIVE_MAC
    host.active_mac_source = walle_constants.MAC_SOURCE_AGENT

    host_network.active_mac = ACTIVE_MAC
    host_network.active_mac_time = timestamp()
    host_network.active_mac_source = walle_constants.MAC_SOURCE_AGENT

    project = test.default_project
    project.dns_automation.credit = {"max_dns_fixes": 1}
    project.dns_automation.credit_end_time = timestamp() + 1200

    test.hosts.assert_equal()
    test.host_network.assert_equal()
    test.projects.assert_equal()


@pytest.mark.parametrize("network_location", ("no", "outdated", "new"))
def test_switch_actualization(test, network_location, mp):
    monkeypatch_method(
        mp,
        method=RacktablesClient.get_switch_ports,
        obj=RacktablesClient,
        return_value={"report-switch": ["report-port"]},
    )
    host, host_network = _mock_host_with_network(
        test, {"active_mac": ACTIVE_MAC}, {"active_mac": ACTIVE_MAC, "active_mac_time": timestamp()}
    )
    actualization_time = REPORT_SWITCHES[0]["time"]

    if network_location != "no":
        host.location = HostLocation(
            switch="current-switch", port="current-port", network_source=walle_constants.NETWORK_SOURCE_EINE
        )
        host.save()
        host_network.network_switch = "current-switch"
        host_network.network_port = "current-port"
        host_network.network_source = walle_constants.NETWORK_SOURCE_EINE
        host_network.network_timestamp = (
            actualization_time - 1 if network_location == "outdated" else actualization_time + 1
        )
        host_network.save()

    result = test.api_client.put(
        "/v1/hosts/{}/agent-report".format(host.name),
        data={
            "macs": REPORT_MACS,
            "switches": REPORT_SWITCHES,
            "version": "0.0",
        },
    )

    assert result.status_code == http.client.OK
    assert result.json == {"result": "The report has been processed."}

    if network_location != "new":
        host.location.switch = "report-switch"
        host.location.port = "report-port"
        host.location.network_source = walle_constants.NETWORK_SOURCE_LLDP
        host_network.network_switch = "report-switch"
        host_network.network_port = "report-port"
        host_network.network_source = walle_constants.NETWORK_SOURCE_LLDP
        host_network.network_timestamp = actualization_time
        host_network.save()

    test.hosts.assert_equal()
    test.host_network.assert_equal()


class TestErrorActualization:
    def test_add_first_ever_error(self, test):
        host, _ = _mock_host_with_network(
            test,
            {
                "active_mac": ACTIVE_MAC,
            },
            {"active_mac": ACTIVE_MAC, "active_mac_time": timestamp()},
        )

        result = test.api_client.put(
            "/v1/hosts/{}/agent-report".format(host.name),
            data={
                "macs": REPORT_MACS,
                "version": "0.0",
                "errors": ["these are the errors: %s"],  # unsafe string from agent
            },
        )

        assert result.status_code == http.client.OK
        assert result.json == {"result": "The report has been processed."}

        host.messages["agent"] = [HostMessage.error("these are the errors: %s")]
        host.walle_agent_errors_flag = True

        test.hosts.assert_equal()
        test.host_network.assert_equal()

    def test_replace_old_error(self, test):
        host, _ = _mock_host_with_network(
            test,
            {
                "active_mac": ACTIVE_MAC,
                "messages": {
                    "dmc": [{"severity": "info", "message": "host is healthy"}],
                    "agent": [{"severity": "error", "message": "some older error"}],
                },
            },
            {"active_mac": ACTIVE_MAC, "active_mac_time": timestamp()},
        )

        result = test.api_client.put(
            "/v1/hosts/{}/agent-report".format(host.name),
            data={
                "macs": REPORT_MACS,
                "version": "0.0",
                "errors": ["these are the errors: %s"],  # unsafe string from agent
            },
        )

        assert result.status_code == http.client.OK
        assert result.json == {"result": "The report has been processed."}

        host.messages["agent"] = [HostMessage.error("these are the errors: %s")]
        host.walle_agent_errors_flag = True

        test.hosts.assert_equal()
        test.host_network.assert_equal()

    def test_remove_existing_error(self, test):
        host, _ = _mock_host_with_network(
            test,
            {
                "active_mac": ACTIVE_MAC,
                "messages": {
                    "dmc": [{"severity": "info", "message": "host is healthy"}],
                    "agent": [{"severity": "error", "message": "some older error"}],
                },
            },
            {
                "active_mac": ACTIVE_MAC,
                "active_mac_time": timestamp(),
            },
        )

        result = test.api_client.put(
            "/v1/hosts/{}/agent-report".format(host.name),
            data={
                "macs": REPORT_MACS,
                "version": "0.0",
            },
        )

        assert result.status_code == http.client.OK
        assert result.json == {"result": "The report has been processed."}

        del host.messages["agent"]

        test.hosts.assert_equal()
        test.host_network.assert_equal()

    def test_no_errors_before_and_after(self, test):
        host, _ = _mock_host_with_network(
            test,
            {"active_mac": ACTIVE_MAC, "messages": {"dmc": [{"severity": "info", "message": "host is healthy"}]}},
            {"active_mac": ACTIVE_MAC, "active_mac_time": timestamp()},
        )

        result = test.api_client.put(
            "/v1/hosts/{}/agent-report".format(host.name),
            data={
                "macs": REPORT_MACS,
                "version": "0.0",
            },
        )

        assert result.status_code == http.client.OK
        assert result.json == {"result": "The report has been processed."}

        test.hosts.assert_equal()


class TestAgentVersion:
    @pytest.mark.parametrize("version", [None, "0.0"])
    def test_agent_version(self, test, version):
        host, host_network = _mock_host_with_network(
            test,
            {"active_mac": ACTIVE_MAC, "agent_version": version},
            {"active_mac": ACTIVE_MAC, "active_mac_time": timestamp()},
        )
        assert host.agent_version == version
        test.hosts.assert_equal()
        result = test.api_client.put(
            "/v1/hosts/{}/agent-report".format(host.name),
            data={
                "macs": REPORT_MACS,
                "version": "1.0",
            },
        )

        host.agent_version = "1.0"
        assert result.status_code == http.client.OK
        assert result.json == {"result": "The report has been processed."}
        test.hosts.assert_equal()

    def test_none_agent_version(self, test):
        host, _ = _mock_host_with_network(
            test,
            drop_none(
                {"active_mac": ACTIVE_MAC, "agent_version": "1.0"},
                {"active_mac": ACTIVE_MAC, "active_mac_time": timestamp()},
            ),
        )

        result = test.api_client.put(
            "/v1/hosts/{}/agent-report".format(host.name), data={"macs": REPORT_MACS, "version": None}
        )
        assert result.status_code == http.client.BAD_REQUEST
        test.hosts.assert_equal()

    def test_no_agent_version(self, test):
        host, _ = _mock_host_with_network(
            test,
            drop_none(
                {"active_mac": ACTIVE_MAC, "agent_version": "1.0"},
                {"active_mac": ACTIVE_MAC, "active_mac_time": timestamp()},
            ),
        )

        result = test.api_client.put("/v1/hosts/{}/agent-report".format(host.name), data={"macs": REPORT_MACS})
        assert result.status_code == http.client.BAD_REQUEST
        test.hosts.assert_equal()


class TestHostIPs:
    @pytest.mark.parametrize("cur_ips", [None, ["192.168.1.3"], HOST_IPS])
    def test_normal_ips(self, test, cur_ips):
        host, host_network = _mock_host_with_network(
            test,
            drop_none(
                {
                    "ips": cur_ips,
                    "active_mac": ACTIVE_MAC,
                }
            ),
            {"ips": cur_ips, "ips_time": timestamp(), "active_mac": ACTIVE_MAC, "active_mac_time": timestamp()},
        )
        result = test.api_client.put(
            "/v1/hosts/{}/agent-report".format(host.name),
            data={
                "macs": REPORT_MACS,
                "ips": HOST_IPS,
                "version": "0.0",
            },
        )

        host.ips = sorted(HOST_IPS)
        host_network.ips = sorted(HOST_IPS)
        assert result.status_code == http.client.OK
        assert result.json == {"result": "The report has been processed."}
        test.hosts.assert_equal()
        test.host_network.assert_equal()

    def test_invalid_ips(self, test):
        host, _ = _mock_host_with_network(
            test,
            {
                "ips": HOST_IPS,
            },
            {"ips": HOST_IPS, "ips_time": timestamp()},
        )
        invalid_ip = "fufl::obvi:ous:c7d1:ff92"
        result = test.api_client.put(
            "/v1/hosts/{}/agent-report".format(host.name),
            data={
                "ips": [invalid_ip],
                "version": "0.0",
            },
        )

        assert result.status_code == http.client.BAD_REQUEST
        expected_msg = 'Request validation error: {} does not appear to be an IPv4 or IPv6 address'.format(
            str([invalid_ip])[1:-1]
        )
        assert result.json["message"] == expected_msg
        test.hosts.assert_equal()
        test.host_network.assert_equal()


def _mock_host_with_network(test, host_overrides={}, network_overrides={}):
    host = test.mock_host(
        dict(
            {
                "inv": 0,
                "state": HostState.ASSIGNED,
                "macs": HOST_MACS,
                "active_mac": None,
                "location": HostLocation(),
                "agent_version": "0.0",
            },
            **host_overrides
        )
    )
    host_network = test.mock_host_network(network_overrides, host=host)
    return host, host_network


def test_saving_walle_agent_errors_flag(test, mp):
    host, host_network = _mock_host_with_network(
        test, {"active_mac": ACTIVE_MAC, "inv": 0}, {"active_mac": ACTIVE_MAC, "active_mac_time": timestamp()}
    )

    result = test.api_client.put(
        "/v1/hosts/{}/agent-report".format(host.name),
        data={"macs": REPORT_MACS, "version": "0.0", "errors": ["some_error"]},
    )

    host.messages["agent"] = [HostMessage.error("some_error")]
    host.walle_agent_errors_flag = True

    assert result.status_code == http.client.OK

    test.hosts.assert_equal()
    test.host_network.assert_equal()
