import pytest
import http.client

from infra.walle.server.tests.lib.util import TestCase, hosts_api_url, mock_schedule_release_host
from walle.errors import InvalidHostStateError
from walle.hosts import HostState, HostStatus
from walle.util.misc import drop_none


@pytest.fixture
def test(monkeypatch_timestamp, monkeypatch_audit_log, request):
    return TestCase.create(request)


@pytest.mark.parametrize("state", HostState.ALL)
@pytest.mark.parametrize("status", set(HostStatus.ALL) - {HostStatus.READY, HostStatus.MANUAL, HostStatus.DEAD})
def test_reject_by_status(test, state, status):
    host = test.mock_host({"state": state, "status": status})

    result = test.api_client.post(hosts_api_url(host, action="/release-host"), data={})

    allowed_states = HostState.ALL
    allowed_statuses = [HostStatus.READY, HostStatus.MANUAL, HostStatus.DEAD]
    error_message = InvalidHostStateError(host, allowed_states=allowed_states, allowed_statuses=allowed_statuses)

    assert result.status_code == http.client.CONFLICT
    assert result.json["message"] == str(error_message)
    test.hosts.assert_equal()


@pytest.mark.parametrize("state", HostState.ALL)
@pytest.mark.parametrize("status", [HostStatus.READY, HostStatus.MANUAL, HostStatus.DEAD])
@pytest.mark.parametrize("erase_disks", [True, False])
@pytest.mark.parametrize("disable_admin_requests", [True, False])
@pytest.mark.parametrize("ignore_cms", [True, False])
@pytest.mark.parametrize("reason", [None, "WALLE-3328"])
@pytest.mark.parametrize("cms_task_id", [None, "1"])
def test_fqdn_deinvalidation(test, state, status, erase_disks, disable_admin_requests, ignore_cms, reason, cms_task_id):
    host = test.mock_host({"state": state, "status": status, "cms_task_id": cms_task_id})
    request = drop_none({"disable_admin_requests": disable_admin_requests, "ignore_cms": ignore_cms, "reason": reason})

    result = test.api_client.post(hosts_api_url(host, action="/release-host"), data=request)

    assert result.status_code == http.client.OK

    drop_cms_task = True if cms_task_id else False
    mock_schedule_release_host(
        host,
        disable_admin_requests=disable_admin_requests,
        ignore_cms=ignore_cms,
        reason=reason,
        drop_cms_task=drop_cms_task,
    )

    assert result.json == host.to_api_obj()

    test.hosts.assert_equal()
