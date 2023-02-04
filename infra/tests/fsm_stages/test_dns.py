"""Test DNS FSM stage."""

from unittest import mock

import pytest
from requests import Request, Response

from infra.walle.server.tests.lib.util import (
    TestCase,
    mock_task,
    handle_host,
    mock_commit_stage_changes,
    mock_complete_current_stage,
    mock_fail_current_stage,
    mock_retry_current_stage,
    monkeypatch_max_errors,
    monkeypatch_config,
    check_stage_initialization,
    mock_response,
)
from sepelib.core import config
from sepelib.yandex.dns_api import DnsApiError
from walle import network
from walle.clients.dns import DnsApiOperation, slayer_dns_api
from walle.clients.dns.interface import DnsError
from walle.constants import NETWORK_SOURCE_EINE, VLAN_SCHEME_SEARCH, VLAN_SCHEME_STATIC, MAC_SOURCE_AGENT
from walle.fsm_stages import dns
from walle.fsm_stages.common import get_current_stage
from walle.hosts import HostState, DnsConfiguration
from walle.models import timestamp
from walle.network import HostNetworkLocationInfo
from walle.stages import Stages, Stage
from walle.util.misc import drop_none

DNS_API_ERROR_MOCK = "Error mock"


@pytest.fixture
def test(request, monkeypatch_timestamp):
    return TestCase.create(request)


def test_stage_initialization(test):
    check_stage_initialization(test, Stage(name=Stages.SETUP_DNS, status=dns._STATUS_PREPARE))


@pytest.fixture
def mocked_parse_dns_api_error_response(mp):
    mp.function(dns._parse_dns_api_error_response, return_value=DNS_API_ERROR_MOCK)


@pytest.fixture
def email_config(mp):
    mp.setitem(
        config.get(),
        "notifications",
        {
            "sender": "test-sender",
            "recipients_by_severity": {
                "info": ["info1", "info2"],
                "warning": ["warning1", "warning2"],
                "error": ["error1", "error2"],
                "critical": ["critical1", "critical2"],
            },
            "dns_api_session_to": ["dns-api-session-target"],
        },
    )


@pytest.fixture
def http_request():
    request = mock.MagicMock(Request)()
    request.method = "GET"
    request.path_url = "/mock-http-request"
    request.body = "Request line 1\nRequest line2\n"
    return request


@pytest.fixture
def http_response():
    response = mock.MagicMock(Response)()
    response.reason = "OK"
    response.status_code = 200
    response.content = "Response line 1\nResponse line2\n"
    return response


@pytest.fixture
def dns_client(mp, http_request, http_response):
    dns_client_mock = mock.MagicMock(slayer_dns_api.DnsClient)
    mp.setattr("walle.clients.dns.slayer_dns_api.DnsClient", dns_client_mock)

    network_client = mock.MagicMock(slayer_dns_api.dns_api._DnsApiNetworkClient)()
    dns_client_mock()._dns_api_client.network_client = network_client

    network_client.last_request = http_request
    network_client.last_response = http_response

    return dns_client_mock


@pytest.fixture
def add_dns_api_http_session(monkeypatch_timestamp, http_request, http_response):
    request_message = dns._format_http_request(timestamp(), http_request)
    response_message = dns._format_http_response(timestamp(), http_response)

    def add_to_stage(stage):
        dns_api_http_session = stage.get_temp_data("dns_api_http_session", [])
        dns_api_http_session += [request_message, response_message]
        stage.set_temp_data("dns_api_http_session", dns_api_http_session)

    return add_to_stage


@pytest.fixture
def dns_api_session_email(monkeypatch_timestamp, email_config):
    def return_expected_email_call(host, session):
        # NOTE send_email is already mocked in walle.test.util TestCase.setUp
        from walle.util.notifications import send_email

        assert send_email.mock_calls[1] == mock.call(
            ["dns-api-session-target"],
            "DNS-API session for host {}".format(host.human_id()),
            "\n\n".join(session),
        )

    return return_expected_email_call


def test_fsm_stage__status_prepare(test, mp, dns_client):
    host = test.mock_host(
        {
            "state": HostState.FREE,
            "active_mac": "00:00:00:00:00:10",
            "active_mac_source": MAC_SOURCE_AGENT,
            "task": mock_task(stage=Stages.SETUP_DNS, stage_status=dns._STATUS_PREPARE),
        }
    )

    test.mock_host_network(
        {
            "active_mac": "00:00:00:00:00:10",
            "active_mac_source": MAC_SOURCE_AGENT,
        },
        host=host,
    )

    dns_operations = [
        DnsApiOperation.delete("PTR", "10.0.0.1", "hostname."),
        DnsApiOperation.delete("PTR", "2a02:06b8:b010:0070:0225:90ff:fe88:b555", "hostname."),
        DnsApiOperation.delete("A", "hostname", "10.0.0.1"),
        DnsApiOperation.delete("AAAA", "hostname", "2a02:06b8:b010:0070:0225:90ff:fe88:b555"),
        DnsApiOperation.add("A", "hostname", "127.0.0.1"),
        DnsApiOperation.add("PTR", "127.0.0.1", "hostname"),
        DnsApiOperation.add("AAAA", "hostname", "2a02:06b8:b010:0070:0225:90ff:fe88:b334"),
        DnsApiOperation.add("PTR", "2a02:06b8:b010:0070:0225:90ff:fe88:b334", "hostname"),
    ]

    get_operations_mock = mp.function(dns.get_operations_for_dns_records, module=dns, return_value=dns_operations)

    switch = "switch-mock"
    mp.function(
        network.get_current_host_switch_port,
        return_value=HostNetworkLocationInfo(switch=switch, port="port-mock", source=NETWORK_SOURCE_EINE, timestamp=0),
    )

    dns_records = [
        network.DnsRecord("AAAA", "hostname", ["2a02:06b8:b010:0070:0225:90ff:fe88:b334"]),
        network.DnsRecord("A", "hostname", ["127.0.0.1"]),
    ]
    mp.function(network.get_host_dns_records, return_value=dns_records)

    handle_host(host)

    stage = get_current_stage(host)
    stage.set_temp_data("switch", switch)
    stage.set_temp_data("switch_source", NETWORK_SOURCE_EINE)
    stage.set_temp_data("mac", host.active_mac)
    stage.set_temp_data("mac_source", MAC_SOURCE_AGENT)
    stage.set_temp_data("vlan_scheme", VLAN_SCHEME_STATIC)  # mocker defaults
    stage.set_temp_data("vlans", [666])  # mocker defaults

    stage.set_temp_data("dns_records", list(map(network.DnsRecord._asdict, dns_records)))
    stage.set_temp_data("dns_operations", list(map(DnsApiOperation.to_dict, dns_operations)))
    mock_commit_stage_changes(host, status=dns._STATUS_SEND_REQUEST, check_now=True)

    test.hosts.assert_equal()
    get_operations_mock.assert_called_once_with(dns_records, mock.ANY)


@pytest.mark.parametrize("create", [True, False, None])
def test_fsm_stage_status_send_request__success(dns_client, test, monkeypatch_timestamp, create):
    host = test.mock_host(
        {
            "name": "hostname-mock",
            "state": HostState.FREE,
            "task": mock_task(
                stage=Stages.SETUP_DNS,
                stage_params=drop_none({"create": create}),
                stage_status=dns._STATUS_SEND_REQUEST,
                stage_temp_data={
                    "dns_operations": [
                        {
                            "data": "127.0.0.1",
                            "type": "A",
                            "operation": DnsApiOperation.DNS_API_ADD,
                            "name": "hostname-mock",
                        },
                        {"data": None, "type": "PTR", "operation": DnsApiOperation.DNS_API_DELETE, "name": "127.0.0.1"},
                    ],
                    "dns_records": [],
                    "switch": "switch-mock",
                    "switch_source": NETWORK_SOURCE_EINE,
                    "mac": "mac-mock",
                    "mac_source": MAC_SOURCE_AGENT,
                },
            ),
        }
    )

    handle_host(host)
    dns_client().apply_operations.assert_called_once_with(
        [
            DnsApiOperation.add("A", "hostname-mock", "127.0.0.1"),
            DnsApiOperation.delete("PTR", "127.0.0.1", None),
        ]
    )

    get_current_stage(host).set_temp_data("dns_update_time", timestamp())
    mock_commit_stage_changes(host, status=dns._STATUS_COMPLETE, check_now=True)
    test.hosts.assert_equal()


@pytest.mark.parametrize("create", [True, False, None])
def test_fsm_stage_status_send_request__success_new_records_for_host(dns_client, test, monkeypatch_timestamp, create):
    host = test.mock_host(
        {
            "name": "hostname-mock",
            "state": HostState.FREE,
            "task": mock_task(
                stage=Stages.SETUP_DNS,
                stage_params=drop_none({"create": create}),
                stage_status=dns._STATUS_SEND_REQUEST,
                stage_temp_data={
                    "dns_operations": [
                        {
                            "data": "127.0.0.1",
                            "type": "A",
                            "operation": DnsApiOperation.DNS_API_ADD,
                            "name": "hostname-mock",
                        }
                    ],
                    "dns_records": [],
                    "switch": "switch-mock",
                    "switch_source": NETWORK_SOURCE_EINE,
                    "mac": "mac-mock",
                    "mac_source": MAC_SOURCE_AGENT,
                },
            ),
        }
    )

    handle_host(host)
    dns_client().apply_operations.assert_called_once_with(
        [
            DnsApiOperation.add("A", "hostname-mock", "127.0.0.1"),
        ]
    )

    if not create:
        # we are creating new records for a new host, not updating anything
        get_current_stage(host).set_temp_data("dns_update_time", timestamp())

    mock_commit_stage_changes(host, status=dns._STATUS_COMPLETE, check_now=True)
    test.hosts.assert_equal()


def test_fsm_stage_status_send_request__no_operations(dns_client, test):
    host = test.mock_host(
        {
            "state": HostState.FREE,
            "task": mock_task(
                stage=Stages.SETUP_DNS,
                stage_status=dns._STATUS_SEND_REQUEST,
                stage_temp_data={
                    "dns_operations": [],
                    "dns_records": [],
                    "switch": "switch-mock",
                    "switch_source": NETWORK_SOURCE_EINE,
                    "mac": "mac-mock",
                    "mac_source": MAC_SOURCE_AGENT,
                },
            ),
        }
    )

    handle_host(host)

    assert not dns_client().apply_operations.called
    mock_commit_stage_changes(host, status=dns._STATUS_COMPLETE, check_now=True)
    test.hosts.assert_equal()


def test_fsm_stage__send_request_fatal_error__retry(dns_client, test, mp, mocked_parse_dns_api_error_response):
    monkeypatch_max_errors(mp, "dns_api.max_errors", dont_allow=False)
    monkeypatch_config(mp, "dns_api.dns_sync_wait_timeout", 5)
    dns_client().apply_operations.side_effect = dns.DnsApiError(DNS_API_ERROR_MOCK)

    dns_client.network_client = dns_client()._dns_api_client.network_client

    host = test.mock_host(
        {
            "state": HostState.FREE,
            "task": mock_task(
                stage=Stages.SETUP_DNS,
                stage_status=dns._STATUS_SEND_REQUEST,
                stage_temp_data={
                    "dns_operations": [
                        {"data": None, "type": "PTR", "operation": DnsApiOperation.DNS_API_DELETE, "name": "127.0.0.1"}
                    ],
                    "dns_records": [],
                    "switch": "switch-mock",
                    "switch_source": NETWORK_SOURCE_EINE,
                    "mac": "mac-mock",
                    "mac_source": MAC_SOURCE_AGENT,
                },
            ),
        }
    )

    stage = get_current_stage(host)

    handle_host(host)

    stage.set_data("dns_api_errors", 1)
    mock_retry_current_stage(
        host,
        Stages.SETUP_DNS,
        dns._STATUS_PREPARE,
        check_after=5,
        error="Failed to setup records: DNS API returned an error: {}".format(DNS_API_ERROR_MOCK),
    )

    test.hosts.assert_equal()


def test_fsm_stage__send_request_fatal_error__send_report(dns_client, test, mp, mocked_parse_dns_api_error_response):
    monkeypatch_max_errors(mp, "dns_api.max_errors", dont_allow=True)
    monkeypatch_config(mp, "dns_api.dns_sync_wait_timeout", 4)

    dns_client().apply_operations.side_effect = dns.DnsApiError(DNS_API_ERROR_MOCK)
    dns_client.network_client = dns_client()._dns_api_client.network_client

    host = test.mock_host(
        {
            "state": HostState.FREE,
            "task": mock_task(
                stage=Stages.SETUP_DNS,
                stage_status=dns._STATUS_SEND_REQUEST,
                stage_data={"dns_api_http_session_sent": True},
                stage_temp_data={
                    "dns_operations": [
                        {"data": None, "type": "PTR", "operation": DnsApiOperation.DNS_API_DELETE, "name": "127.0.0.1"}
                    ],
                    "dns_records": [],
                    "switch": "switch-mock",
                    "switch_source": NETWORK_SOURCE_EINE,
                    "mac": "mac-mock",
                    "mac_source": MAC_SOURCE_AGENT,
                },
            ),
        }
    )

    stage = get_current_stage(host)

    handle_host(host)

    stage.set_data("dns_api_errors", 1)
    mock_retry_current_stage(
        host,
        Stages.SETUP_DNS,
        dns._STATUS_PREPARE,
        check_after=dns.STAGE_RETRY_INTERVAL,
        error="Failed to setup records: DNS API returned an error: {}".format(DNS_API_ERROR_MOCK),
    )

    test.hosts.assert_equal()


def test_fsm_stage__send_request_fatal_error__global_timeout(dns_client, test, mp, mocked_parse_dns_api_error_response):
    monkeypatch_max_errors(mp, "dns_api.max_errors", dont_allow=True)
    monkeypatch_config(mp, "dns_api.dns_sync_wait_timeout", 4)

    dns_client().apply_operations.side_effect = dns.DnsApiError(DNS_API_ERROR_MOCK)
    dns_client.network_client = dns_client()._dns_api_client.network_client

    host = test.mock_host(
        {
            "state": HostState.FREE,
            "task": mock_task(
                stage=Stages.SETUP_DNS,
                stage_status=dns._STATUS_SEND_REQUEST,
                stage_status_time=timestamp() - dns.STAGE_RETRY_TIMEOUT - 1,
                stage_temp_data={
                    "dns_operations": [
                        {"data": None, "type": "PTR", "operation": DnsApiOperation.DNS_API_DELETE, "name": "127.0.0.1"}
                    ],
                    "dns_records": [],
                    "switch": "switch-mock",
                    "switch_source": NETWORK_SOURCE_EINE,
                    "mac": "mac-mock",
                    "mac_source": MAC_SOURCE_AGENT,
                },
            ),
        }
    )

    handle_host(host)

    mock_fail_current_stage(
        host, reason="Failed to setup records: DNS API returned an error: {}".format(DNS_API_ERROR_MOCK)
    )
    test.hosts.assert_equal()


@pytest.mark.parametrize("with_dns_update_time", (True, False))
def test_fsm_stage__setup_completion(test, with_dns_update_time):
    stage_temp_data = {
        "switch": "switch-mock",
        "mac": test.macs[0],
        "vlan_scheme": VLAN_SCHEME_SEARCH,
        "vlans": [666],
        "dns_operations": [],
    }
    if with_dns_update_time:
        stage_temp_data["dns_update_time"] = 666

    host = test.mock_host(
        {
            "state": HostState.ASSIGNED,
            "task": mock_task(
                stage=Stages.SETUP_DNS, stage_status=dns._STATUS_COMPLETE, stage_temp_data=stage_temp_data
            ),
        }
    )

    handle_host(host)

    host.dns = DnsConfiguration(
        switch=stage_temp_data["switch"],
        mac=stage_temp_data["mac"],
        vlan_scheme=VLAN_SCHEME_SEARCH,
        vlans=[666],
        project=host.project,
        check_time=timestamp(),
    )
    if with_dns_update_time:
        host.dns.update_time = stage_temp_data["dns_update_time"]
    mock_complete_current_stage(host, inc_revision=1)

    test.hosts.assert_equal()


@pytest.mark.parametrize("with_dns_update_time", (True, False))
def test_fsm_stage__clear_completion(test, with_dns_update_time):
    stage_temp_data = {"dns_operations": []}
    if with_dns_update_time:
        stage_temp_data["dns_update_time"] = 666

    host = test.mock_host(
        {
            "state": HostState.ASSIGNED,
            "task": mock_task(
                stage=Stages.SETUP_DNS,
                stage_params={"clear": True},
                stage_status=dns._STATUS_COMPLETE,
                stage_temp_data=stage_temp_data,
            ),
        }
    )

    handle_host(host)

    if with_dns_update_time:
        host.dns = DnsConfiguration(update_time=stage_temp_data["dns_update_time"])
    else:
        assert host.dns is None
    mock_complete_current_stage(host, inc_revision=1)

    test.hosts.assert_equal()


def test_fsm_stage__multiple_endpoints(dns_client, test, mp):
    host = test.mock_host(
        {
            "state": HostState.FREE,
            "active_mac": "00:00:00:00:00:10",
            "task": mock_task(stage=Stages.SETUP_DNS, stage_status=dns._STATUS_PREPARE),
        }
    )

    test.mock_host_network(
        {
            "active_mac": "00:00:00:00:00:10",
            "active_mac_source": MAC_SOURCE_AGENT,
        },
        host=host,
    )

    mp.function(
        network.get_current_host_switch_port,
        return_value=HostNetworkLocationInfo(switch="switch", port="port", source=NETWORK_SOURCE_EINE, timestamp=0),
    )

    mp.function(
        network.get_host_dns_records,
        return_value=[
            network.DnsRecord("AAAA", "default", ["2a02:06b8:b010:0070:0225:90ff:fe88:b334"]),
            network.DnsRecord("A", "default", ["127.0.0.1"]),
        ],
    )

    dns_client().get_a.side_effect = DnsError("multiple-endpoints-error-mock")
    dns_client().get_aaaa.side_effect = DnsError("multiple-endpoints-error-mock")
    dns_client().get_ptr.side_effect = DnsError("multiple-endpoints-error-mock")

    handle_host(host)

    mock_commit_stage_changes(
        host,
        error="Failed to prepare DNS operation: multiple-endpoints-error-mock",
        check_after=dns.STAGE_RETRY_INTERVAL,
    )
    test.hosts.assert_equal()


def test_fsm_stage__clear__prepare(test, mp):
    host = test.mock_host(
        {
            "state": HostState.FREE,
            "task": mock_task(stage=Stages.SETUP_DNS, stage_status=dns._STATUS_PREPARE, stage_params={"clear": True}),
        }
    )

    dns_operations = [
        DnsApiOperation.delete("PTR", "10.0.0.1", "hostname."),
        DnsApiOperation.delete("PTR", "2a02:06b8:b010:0070:0225:90ff:fe88:b555", "hostname."),
        DnsApiOperation.delete("A", "hostname", "10.0.0.1"),
        DnsApiOperation.delete("AAAA", "hostname", "2a02:06b8:b010:0070:0225:90ff:fe88:b555"),
        DnsApiOperation.delete("PTR", "127.0.0.1", "fb-hostname."),
        DnsApiOperation.delete("A", "fb-hostname", "127.0.0.1"),
        DnsApiOperation.delete("AAAA", "fb-hostname", "2a02:06b8:b010:0070:0225:90ff:fe88:b334"),
    ]

    get_operations_mock = mp.function(dns.get_delete_operations_for_fqdns, module=dns, return_value=dns_operations)

    mp.function(network.get_host_fqdns, return_value=["hostname", "fb-hostname"])

    handle_host(host)

    stage = get_current_stage(host)
    mock_commit_stage_changes(host, status=dns._STATUS_SEND_REQUEST, check_now=True)
    stage.set_temp_data("dns_records", [])
    stage.set_temp_data("dns_operations", list(map(DnsApiOperation.to_dict, dns_operations)))

    test.hosts.assert_equal()
    get_operations_mock.assert_called_once_with(["hostname", "fb-hostname"], mock.ANY)


def test_fsm_stage__clear__send_request(dns_client, test, mp):
    dns_operations = [
        DnsApiOperation.delete("PTR", "10.0.0.1", "hostname."),
        DnsApiOperation.delete("PTR", "2a02:06b8:b010:0070:0225:90ff:fe88:b555", "hostname."),
        DnsApiOperation.delete("A", "hostname", "10.0.0.1"),
        DnsApiOperation.delete("AAAA", "hostname", "2a02:06b8:b010:0070:0225:90ff:fe88:b555"),
        DnsApiOperation.delete("PTR", "127.0.0.1", "fb-hostname."),
        DnsApiOperation.delete("A", "fb-hostname", "127.0.0.1"),
        DnsApiOperation.delete("AAAA", "fb-hostname", "2a02:06b8:b010:0070:0225:90ff:fe88:b334"),
    ]

    host = test.mock_host(
        {
            "state": HostState.FREE,
            "task": mock_task(
                stage=Stages.SETUP_DNS,
                stage_status=dns._STATUS_SEND_REQUEST,
                stage_params={"clear": True},
                stage_temp_data={
                    "dns_operations": list(map(DnsApiOperation.to_dict, dns_operations)),
                    "dns_records": [],
                },
            ),
        }
    )

    mp.function(network.get_host_fqdns, return_value=["hostname", "fb-hostname"])

    handle_host(host)

    stage = get_current_stage(host)
    mock_commit_stage_changes(host, status=dns._STATUS_COMPLETE, check_now=True)
    stage.set_temp_data("dns_records", [])
    stage.set_temp_data("dns_operations", list(map(DnsApiOperation.to_dict, dns_operations)))

    test.hosts.assert_equal()


def _create_mocked_dns_api_error(data, error_name="test_error"):
    mocked_response = mock_response(data)
    return DnsApiError(error_name, response=mocked_response)


def test_parse_dns_api_error_order_for_error():
    value = 'error1'
    test_value = {'errors': [{'error': value}, {'error': 'test'}]}
    error = _create_mocked_dns_api_error(test_value)
    message = dns._parse_dns_api_error_response(error)
    assert message == value


def test_parse_dns_api_error_order_for_http_message():
    value = 'http_message1'
    test_value = {'errors': [{'http_message': value}, {'http_message': 'http_message2'}]}
    error = _create_mocked_dns_api_error(test_value)
    message = dns._parse_dns_api_error_response(error)
    assert message == value


def test_parse_dns_api_error_order_for_error_and_http_message():
    value = 'error2'
    test_value = {'errors': [{'http_message': 'http_message1'}, {'http_message': 'http_message2', 'error': value}]}
    error = _create_mocked_dns_api_error(test_value)
    message = dns._parse_dns_api_error_response(error)
    assert message == value


def test_parse_dns_api_error_without_error_and_http_message():
    test_value = {'errors': [{'test': None}, {'test': None}]}
    error_name = 'error_name'
    error = _create_mocked_dns_api_error(test_value, error_name)
    message = dns._parse_dns_api_error_response(error)
    assert message == error_name
