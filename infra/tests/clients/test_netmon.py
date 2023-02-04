"""Test netmon client methods."""
import http.client
from contextlib import contextmanager

import pytest
import six

from infra.walle.server.tests.lib.util import load_mock_data
from walle.clients import netmon, utils
from walle.expert.constants import NETMON_CONNECTIVITY_LEVEL


@contextmanager
def mock_netmon_response_data(mp, filepath):
    f = six.BytesIO(six.ensure_binary(load_mock_data(filepath)))
    result = utils.requests.Response()
    result.headers = {"Content-Type": "application/json"}
    result.status_code = http.client.OK
    result.raw = f

    request_mock = mp.function(utils.requests.request, module=utils.requests, return_value=result)
    yield request_mock


@pytest.yield_fixture
def mock_netmon_alive_data(mp):
    filepath = "mocks/netmon-alive.json"
    with mock_netmon_response_data(mp, filepath) as request_mock:
        yield request_mock


@pytest.yield_fixture
def mock_netmon_seen_hosts_data(mp):
    filepath = "mocks/netmon-seen-hosts.json"
    with mock_netmon_response_data(mp, filepath) as request_mock:
        yield request_mock


def test_netmon_alive(mock_netmon_alive_data):
    """Faster version of the test, uses stored data sample to check validation."""
    client = netmon.NetmonClient(service="netmon-test", host="localhost")
    assert client.get_alive_metrics(NETMON_CONNECTIVITY_LEVEL)


def test_netmon_seen_hosts(mock_netmon_seen_hosts_data):
    """Faster version of the test, uses stored data sample to check validation."""
    client = netmon.NetmonClient(service="netmon-test", host="localhost")
    seen_hosts_data = client.get_seen_hosts()

    assert "dc" in seen_hosts_data
    assert seen_hosts_data["dc"]

    assert "queue" in seen_hosts_data
    assert seen_hosts_data["queue"]
