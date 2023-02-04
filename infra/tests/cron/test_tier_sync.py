import pytest

from infra.walle.server.tests.lib.util import monkeypatch_locks
from walle.cron.tier_sync import _sync_tiers
from walle.hosts import HostState, HostStatus

PROJECT_TIER = 1


@pytest.mark.parametrize("state", HostState.ALL_ASSIGNED)
@pytest.mark.parametrize("status", HostStatus.ALL_ASSIGNED)
@pytest.mark.parametrize("crit_hosts_count_reached", [True, False])
def test_sync_tiers_successfully(walle_test, mp, state, status, crit_hosts_count_reached):
    monkeypatch_locks(mp)

    if crit_hosts_count_reached:
        mp.config("tier_configuration.hosts_with_mismathed_tiers_crit_count", 1)

    project = walle_test.mock_project({"id": "some-id", "tier": PROJECT_TIER})
    host = walle_test.mock_host({"tier": PROJECT_TIER + 1, "project": project.id, "state": state, "status": status})

    _sync_tiers()

    host.tier = PROJECT_TIER
    walle_test.hosts.assert_equal()


@pytest.mark.parametrize("status", HostStatus.ALL_TASK)
def test_sync_tiers_for_host_in_task_status(walle_test, mp, status):
    monkeypatch_locks(mp)

    project = walle_test.mock_project({"id": "some-id", "tier": PROJECT_TIER})
    walle_test.mock_host(
        {"tier": PROJECT_TIER + 1, "project": project.id, "state": HostState.ASSIGNED, "status": status}
    )

    _sync_tiers()

    walle_test.hosts.assert_equal()
