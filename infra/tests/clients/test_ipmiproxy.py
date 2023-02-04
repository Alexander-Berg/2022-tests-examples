"""Tests for IPMI Proxy client."""

import socket
from unittest.mock import Mock

import pytest
from requests import RequestException

import object_validator
import walle.clients.dns.local_dns_resolver
import walle.clients.dns.slayer_dns_api
import walle.clients.utils
import walle.util.misc
from infra.walle.server.tests.lib.util import patch_attr, monkeypatch_config
from walle import boxes
from walle.clients import ipmiproxy

INTERNAL_PROVIDER = "internal"
BOX_PROVIDER = "box"
ALL_PROVIDERS = [INTERNAL_PROVIDER, BOX_PROVIDER]


@pytest.fixture(autouse=True)
def patch_config(mp):
    monkeypatch_config(mp, "ipmiproxy.access_token", "mock-ipmiproxy-access-token")
    monkeypatch_config(mp, f"{boxes.IPMIPROXY_BOXES_SECTION}.mock_box", {})


@pytest.fixture(autouse=True)
def patch_dns_client(mp):
    patch_attr(mp, walle.clients.dns.slayer_dns_api.DnsClient, "__init__", return_value=None)
    patch_attr(mp, walle.clients.dns.local_dns_resolver.LocalDNSResolver, "get_a", return_value=["ipmi-ip-mock"])
    patch_attr(mp, walle.clients.dns.local_dns_resolver.LocalDNSResolver, "get_aaaa", return_value=["ipmi-ip-mock"])


def get_client(provider):
    if provider == INTERNAL_PROVIDER:
        return ipmiproxy.get_client(ipmiproxy.get_yandex_internal_provider("ipmi-mac-mock"), "human-id-mock")
    if provider == BOX_PROVIDER:
        return ipmiproxy.get_client(ipmiproxy.get_yandex_box_provider("mock_box", 666), "human-id-mock")
    raise RuntimeError("Unknown IPMI provider")


def get_json_request_side_effect(result_mock):
    def json_request_side_effect(service, method, url, scheme=None, *args, **kwargs):
        if scheme is not None:
            return object_validator.validate("result", result_mock, scheme)
        else:
            return result_mock

    return json_request_side_effect


@pytest.mark.parametrize(
    ["status", "result"],
    [
        ("on", True),
        ("off", False),
    ],
)
@pytest.mark.parametrize("provider", ALL_PROVIDERS)
def test_get_power_status_success(mp, status, result, provider):
    mock_reply = {
        "host": "hostname-mock",
        "data": {
            "Cooling/Fan Fault": False,
            "Power Control Fault": False,
            "Main Power Fault": False,
            "Power Overload": False,
            "Chassis Intrusion": "inactive",
            "Power Interlock": "inactive",
            "Front-Panel Lockout": "inactive",
            "Power Restore Policy": "previous",
            "Drive Fault": False,
            "System Power": status,
            "Last Power Event": "",
        },
    }

    mock_json_request = mp.function(
        walle.clients.utils.json_request, side_effect=get_json_request_side_effect(mock_reply)
    )
    client = get_client(provider)
    assert client.is_power_on() is result
    assert client.power_status() == status
    assert mock_json_request.called


@pytest.mark.parametrize("operation", ["power_on", "reset", "power_off", "soft_power_off", "bmc_reset"])
@pytest.mark.parametrize("provider", ALL_PROVIDERS)
def test_operation_success(mp, operation, provider):
    mock_reply = {"host": "hostname-mock", "message": "Set Boot Device to disk", "success": True}

    mock_json_request = mp.function(
        walle.clients.utils.json_request, side_effect=get_json_request_side_effect(mock_reply)
    )

    client = get_client(provider)
    assert getattr(client, operation)() is None
    assert mock_json_request.called


@pytest.mark.slow
@pytest.mark.parametrize(
    "operation",
    [
        "is_power_on",
        "power_on",
        "reset",
        "power_off",
        "soft_power_off",
        "bmc_reset",
    ],
)
@pytest.mark.parametrize(
    ["code", "message", "exception", "error"],
    [
        (400, "message-mock", ipmiproxy.InternalError, "message-mock"),
        (401, "message-mock", ipmiproxy.InternalError, "message-mock"),
        (403, "message-mock", ipmiproxy.InternalError, "message-mock"),
        (
            404,
            "message-mock",
            {INTERNAL_PROVIDER: ipmiproxy.IpmiHostMissingError, BOX_PROVIDER: ipmiproxy.InternalError},
            {INTERNAL_PROVIDER: "IPMI host ipmi-mac-mock.ipmi.yandex.net is missing.", BOX_PROVIDER: "message-mock"},
        ),
        (422, "message-mock", ipmiproxy.HostHwError, "message-mock"),
        (
            422,
            "IPMI error: power status: Invalid data field in request",
            ipmiproxy.BrokenIpmiCommandError,
            "IPMI error: power status: Invalid data field in request",
        ),
        (419, "message-mock", ipmiproxy.InternalError, "message-mock"),
    ],
)
@pytest.mark.parametrize("provider", ALL_PROVIDERS)
def test_operation_failure(mp, operation, code, message, exception, error, provider):
    mock_result = {"message": message}

    mock_response = Mock(
        status_code=code, reason="reason-mock", text="don't do that", **{"json.return_value": mock_result}
    )

    side_effect = RequestException("error-mock", response=mock_response)
    mock_json_request = mp.function(walle.clients.utils.json_request, side_effect=side_effect)
    client = get_client(provider)
    client._max_tries = 0

    if isinstance(exception, dict):
        exception = exception[provider]

    with pytest.raises(exception) as exc:
        getattr(client, operation)()

    assert mock_json_request.called

    if isinstance(error, dict):
        error = error[provider]

    assert str(exc.value) == error


@pytest.mark.parametrize("provider", ALL_PROVIDERS)
def test_power_status_network_error(mp, provider):
    side_effect = socket.error("socket-error-mock")
    mp.function(walle.clients.utils.json_request, side_effect=side_effect)

    client = get_client(provider)
    with pytest.raises(ipmiproxy.IPMIProxyTemporaryError) as exc:
        client.is_power_on()

    assert str(exc.value) == "socket-error-mock"


@pytest.mark.parametrize("provider", ALL_PROVIDERS)
def test_raw_command_success(mp, provider):
    mock_reply = {"host": "hostname-mock", "message": " 0x00 0x00 0x00 0x00", "success": True}

    mock_json_request = mp.function(
        walle.clients.utils.json_request, side_effect=get_json_request_side_effect(mock_reply)
    )

    client = get_client(provider)
    assert client.raw_command('raw-command-mock') == mock_reply
    assert mock_json_request.called


@pytest.mark.slow
@pytest.mark.parametrize(
    ["code", "message", "exception", "error"],
    [
        (400, "message-mock", ipmiproxy.InternalError, "message-mock"),
        (401, "message-mock", ipmiproxy.InternalError, "message-mock"),
        (403, "message-mock", ipmiproxy.InternalError, "message-mock"),
        (
            404,
            "message-mock",
            {INTERNAL_PROVIDER: ipmiproxy.IpmiHostMissingError, BOX_PROVIDER: ipmiproxy.InternalError},
            {INTERNAL_PROVIDER: "IPMI host ipmi-mac-mock.ipmi.yandex.net is missing.", BOX_PROVIDER: "message-mock"},
        ),
        (422, "message-mock", ipmiproxy.HostHwError, "message-mock"),
        (
            422,
            "IPMI error: power status: Invalid data field in request",
            ipmiproxy.BrokenIpmiCommandError,
            "IPMI error: power status: Invalid data field in request",
        ),
        (419, "message-mock", ipmiproxy.InternalError, "message-mock"),
    ],
)
@pytest.mark.parametrize("provider", ALL_PROVIDERS)
def test_raw_command_failure(mp, code, message, exception, error, provider):
    mock_result = {"message": message}

    mock_response = Mock(
        status_code=code, reason="reason-mock", text="don't do that", json=Mock(return_value=mock_result)
    )

    side_effect = RequestException("error-mock", response=mock_response)
    mock_json_request = mp.function(walle.clients.utils.json_request, side_effect=side_effect)
    client = get_client(provider)
    client._max_tries = 0

    if isinstance(exception, dict):
        exception = exception[provider]

    with pytest.raises(exception) as exc:
        client.raw_command('raw-command-mock')

    assert mock_json_request.called

    if isinstance(error, dict):
        error = error[provider]

    assert str(exc.value) == error
