import pytest

from infra.walle.server.tests.lib.util import mock_schedule_bot_project_sync
from walle.clients import bot
from walle.db_sync import bot_project as sync_bot_project_cron
from walle.hosts import HostState, HostStatus

MATCHING_BOT_PROJECT_ID = 1000604
UNMATCHING_BOT_PROJECT_ID = 1000605


def _mock_project(walle_test, validate_bot_project_id=True, bot_project_id=MATCHING_BOT_PROJECT_ID):
    return walle_test.mock_project(
        {
            "id": "bot-project-matched",
            "validate_bot_project_id": validate_bot_project_id,
            "bot_project_id": bot_project_id,
        }
    )


def _mock_assigned_host(walle_test, inv, project_id):
    return walle_test.mock_host(
        {"inv": inv, "project": project_id, "state": HostState.ASSIGNED, "status": HostStatus.READY}
    )


def monkeypatch_bot_iter_hosts_info(mp, host_projects):
    def mkplanner_id(bot_project):
        return bot_project * 10

    mp.function(
        bot.get_oebs_projects,
        return_value={bot_project: {"planner_id": mkplanner_id(bot_project)} for bot_project in host_projects.values()},
    )

    mock_host_info = [
        {
            "inv": host_inv,
            "planner_id": str(mkplanner_id(bot_project)),
        }
        for host_inv, bot_project in host_projects.items()
    ]
    mock_host_info.append({"inv": 10000000999900001, "planner_id": "10,20"})

    mp.function(bot.iter_hosts_info, side_effect=lambda: iter(mock_host_info))


def host_has_task(host):
    return host.task is not None


def test_sync_for_unmatched_hosts(walle_test, mp, monkeypatch_timestamp, monkeypatch_audit_log):
    sync_bot_project_cron.MAX_HOSTS_TO_SYNC = 2
    project = _mock_project(walle_test)

    matching_host = _mock_assigned_host(walle_test, 1, project.id)
    unmatching_host = _mock_assigned_host(walle_test, 2, project.id)

    monkeypatch_bot_iter_hosts_info(
        mp, {matching_host.inv: MATCHING_BOT_PROJECT_ID, unmatching_host.inv: UNMATCHING_BOT_PROJECT_ID}
    )

    sync_bot_project_cron.sync()

    mock_schedule_bot_project_sync(
        host=unmatching_host,
        bot_project_id=MATCHING_BOT_PROJECT_ID,
        reason=sync_bot_project_cron.mk_reason(project.id, MATCHING_BOT_PROJECT_ID, UNMATCHING_BOT_PROJECT_ID),
    )
    walle_test.hosts.assert_equal()


@pytest.mark.xfail(reason="This logic is disabled currently, we force sync on project list")
def test_no_sync_for_projects_without_flag(walle_test, mp, monkeypatch_timestamp, monkeypatch_audit_log):
    project = _mock_project(walle_test, validate_bot_project_id=False)

    matching_host = _mock_assigned_host(walle_test, 1, project.id)
    unmatching_host = _mock_assigned_host(walle_test, 2, project.id)

    monkeypatch_bot_iter_hosts_info(
        mp, {matching_host.inv: MATCHING_BOT_PROJECT_ID, unmatching_host.inv: UNMATCHING_BOT_PROJECT_ID}
    )

    sync_bot_project_cron.sync()

    walle_test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_timestamp", "monkeypatch_audit_log")
def test_sync_enabled_for_project_by_default(walle_test, mp):
    mp.config("bot_project_sync.excluded_projects", [])

    project = walle_test.mock_project(
        {
            "id": "bot-project-matched",
            "tags": ["bot-project-matched-tag"],
            "validate_bot_project_id": True,
            "bot_project_id": MATCHING_BOT_PROJECT_ID,
        }
    )

    unmatching_host = _mock_assigned_host(walle_test, 2, project.id)
    monkeypatch_bot_iter_hosts_info(mp, {unmatching_host.inv: UNMATCHING_BOT_PROJECT_ID})

    sync_bot_project_cron.sync()
    mock_schedule_bot_project_sync(
        host=unmatching_host,
        bot_project_id=MATCHING_BOT_PROJECT_ID,
        reason=sync_bot_project_cron.mk_reason(project.id, MATCHING_BOT_PROJECT_ID, UNMATCHING_BOT_PROJECT_ID),
    )

    walle_test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_timestamp", "monkeypatch_audit_log")
def test_sync_explicitly_disabled_for_project_by_config(walle_test, mp):
    mp.config("bot_project_sync.excluded_projects", ["not-matched"])

    project = walle_test.mock_project(
        {
            "id": "not-matched",
            "tags": ["not-matched-tag"],
            "validate_bot_project_id": True,
            "bot_project_id": MATCHING_BOT_PROJECT_ID,
        }
    )

    unmatching_host = _mock_assigned_host(walle_test, 2, project.id)
    monkeypatch_bot_iter_hosts_info(mp, {unmatching_host.inv: UNMATCHING_BOT_PROJECT_ID})

    sync_bot_project_cron.sync()
    walle_test.hosts.assert_equal()


def test_first_n_hosts_scheduled(walle_test, mp, monkeypatch_timestamp, monkeypatch_audit_log):
    sync_bot_project_cron.MAX_HOSTS_TO_SYNC = 2
    project = _mock_project(walle_test)

    unmatching_hosts = [_mock_assigned_host(walle_test, i, project.id) for i in range(3)]

    monkeypatch_bot_iter_hosts_info(mp, {host.inv: UNMATCHING_BOT_PROJECT_ID for host in unmatching_hosts})

    sync_bot_project_cron.sync()

    forced_task_id = 0
    for host in unmatching_hosts[: sync_bot_project_cron.MAX_HOSTS_TO_SYNC]:
        mock_schedule_bot_project_sync(
            host=host,
            bot_project_id=MATCHING_BOT_PROJECT_ID,
            reason=sync_bot_project_cron.mk_reason(
                project.id,
                MATCHING_BOT_PROJECT_ID,
                UNMATCHING_BOT_PROJECT_ID,
            ),
            forced_task_id=forced_task_id,
        )
        forced_task_id += 1

    walle_test.hosts.assert_equal()
