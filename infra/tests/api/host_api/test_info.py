"""Tests host 'get iss ban status' API."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import mock_task, mock_location
from walle.hosts import HostState
from walle.operations_log.constants import Operation


class TestIssBanStatus:
    @staticmethod
    def mock_host(walle_test, status_reason='test', **kwargs):
        return walle_test.mock_host(dict(inv=1, status_reason=status_reason, **kwargs))

    @staticmethod
    def get_ban_status_from_api(walle_test, host):
        return walle_test.api_client.get("/v1/hosts/{}/ban-status".format(host.inv))

    def test_not_banned_because_havent_task_and_cms_task_id(self, walle_test):
        host = self.mock_host(walle_test)
        result = self.get_ban_status_from_api(walle_test, host)

        assert result.status_code == http.client.OK
        assert result.json == {'result': False, 'reason': 'test'}

    def test_banned_by_cms_task_id(self, walle_test):
        host = self.mock_host(walle_test, cms_task_id="1", ticket="test_ticket")
        result = self.get_ban_status_from_api(walle_test, host)

        assert result.status_code == http.client.OK
        assert result.json == {'result': True, 'reason': 'test', 'cms_task_id': "1", 'ticket': "test_ticket"}

    def test_banned_by_cms(self, walle_test):
        host = self.mock_host(walle_test, ticket="test_ticket", task=mock_task(iss_banned=True, cms_task_id="1"))
        result = self.get_ban_status_from_api(walle_test, host)

        assert result.status_code == http.client.OK
        assert result.json == {'result': True, 'reason': 'test', 'cms_task_id': "1", 'ticket': "test_ticket"}

    def test_not_banned_because_havent_cms_task_and_iss_banned_is_false(self, walle_test):
        host = self.mock_host(walle_test, ticket="test_ticket", task=mock_task(iss_banned=False))
        result = self.get_ban_status_from_api(walle_test, host)

        assert result.status_code == http.client.OK
        assert result.json == {'result': False, 'reason': 'test', 'ticket': "test_ticket"}

    def test_maintenance_task_does_not_affect_reason(self, walle_test):
        host = self.mock_host(
            walle_test,
            ticket="test_ticket",
            task=mock_task(iss_banned=True),
            state=HostState.MAINTENANCE,
            state_reason="me working on host",
            status=Operation.PROFILE.host_status,
            status_reason="reboot hasn't helped, profiling",
        )
        result = self.get_ban_status_from_api(walle_test, host)

        assert result.status_code == http.client.OK
        assert result.json == {'result': True, 'reason': 'me working on host', 'ticket': "test_ticket"}


@pytest.mark.parametrize("stand_name", ("wall-e", "walle.testing"))
@pytest.mark.parametrize("rack", ("r!ack", "r@ack", "r.ack"))
@pytest.mark.parametrize("short_queue_name", ("q!ueue", "q*ueue"))
def test_juggler_aggregate_name(walle_test, mp, stand_name, short_queue_name, rack):
    walle_test.mock_host(
        dict(
            inv=0,
            status_reason='test',
            location=mock_location(short_queue_name=short_queue_name, rack=rack),
        )
    )
    mp.config("juggler.source", stand_name)
    result = walle_test.api_client.get("/v1/hosts/0?fields=juggler_aggregate_name")
    assert result.status_code == http.client.OK
    assert result.json == {'juggler_aggregate_name': "{}-q.ueue-r.ack".format(stand_name)}


@pytest.mark.parametrize("rack", ("", None))
def test_juggler_aggregate_name_empty_rack(walle_test, mp, rack, mp_juggler_source):
    walle_test.mock_host(
        dict(
            inv=0,
            status_reason='test',
            location=mock_location(short_queue_name="queue-mock", rack=rack),
        )
    )
    result = walle_test.api_client.get("/v1/hosts/0?fields=juggler_aggregate_name")
    assert result.status_code == http.client.OK
    assert result.json == {'juggler_aggregate_name': "wall-e.unittest-queue-mock"}
