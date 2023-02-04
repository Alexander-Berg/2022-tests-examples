"""Test DNS API"""

import pytest
import http.client

from infra.walle.server.tests.lib.util import TestCase
from walle.network import BlockedHostName


@pytest.fixture
def test(request):
    return TestCase.create(request)


def test_get_blocked_host_names(test):
    BlockedHostName.store("test-dns2.mock")
    BlockedHostName.store("test-dns1.mock")
    BlockedHostName.store("test-dns3.mock")
    BlockedHostName.store("a-test-dns4.mock")

    expected_list = ["test-dns2.mock", "test-dns1.mock", "test-dns3.mock", "a-test-dns4.mock"]

    response = test.api_client.get("/v1/dns/blocked-host-names")
    assert response.status_code == http.client.OK

    hosts = response.json["result"]
    assert expected_list == hosts


def test_get_blocked_host_names_with_paging(test):
    BlockedHostName.store("test-dns2.mock")
    BlockedHostName.store("test-dns1.mock")
    BlockedHostName.store("test-dns3.mock")
    BlockedHostName.store("a-test-dns4.mock")

    # same order as the names were stored
    expected_list = ["test-dns2.mock", "test-dns1.mock"]

    response = test.api_client.get("/v1/dns/blocked-host-names?cursor=0&limit=2")
    assert response.status_code == http.client.OK

    assert expected_list == response.json["result"]
    assert "next_cursor" in response.json
