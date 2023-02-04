"""Tests host update API."""

import pytest

from infra.walle.server.tests.lib.util import TestCase, hosts_api_url, TEST_HOST, TEST_HOST_INV
from walle import restrictions
from walle.clients.bot import HostPlatform
from walle.hosts import HostState, HostStatus


@pytest.fixture
def test(monkeypatch_timestamp, request):
    return TestCase.create(request)


@pytest.fixture
def mock_get_bot_platform(monkeypatch):
    monkeypatch.setattr(
        'walle.clients.bot._get_host_platform', lambda *args, **kwargs: HostPlatform('system_model', 'board_model')
    )


@pytest.mark.usefixtures("authorized_admin", "mock_get_bot_platform")
def test_debug_update_bot_platform(test):
    host = test.mock_host(
        {
            "inv": TEST_HOST_INV,
            "name": TEST_HOST,
            "state": HostState.ASSIGNED,
            "status": HostStatus.MANUAL,
            "status_author": "other-user@",
            "restrictions": restrictions.ALL,
        }
    )

    result = test.api_client.open(hosts_api_url(host, "inv", action="/update-bot-platform"), method='POST', data={})

    assert result.status_code == 200
