import pytest

from infra.walle.server.tests.lib.util import (
    handle_host,
    mock_task,
    mock_schedule_host_profiling,
    mock_schedule_host_redeployment,
    any_task_status,
    mock_complete_current_stage,
)
from walle.clients.eine import ProfileMode
from walle.hosts import HostState, TaskType
from walle.stages import Stages


@pytest.mark.usefixtures("monkeypatch_audit_log")
def test_no_op(walle_test):
    host = walle_test.mock_host(
        {
            "state": HostState.ASSIGNED,
            "status": any_task_status(),
            "task": mock_task(stage=Stages.CLOUD_POST_PROCESSOR, stage_params={}),
        }
    )

    handle_host(host)
    mock_complete_current_stage(host)

    walle_test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_audit_log")
def test_schedule_profiling(walle_test):
    host = walle_test.mock_host(
        {
            "state": HostState.ASSIGNED,
            "status": any_task_status(),
            "task": mock_task(stage=Stages.CLOUD_POST_PROCESSOR, stage_params={"profile_after_task": True}),
        }
    )

    handle_host(host)
    mock_schedule_host_profiling(host, manual=False)

    walle_test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_audit_log")
@pytest.mark.parametrize("with_profile", (True, False))
def test_schedule_redeploy(walle_test, with_profile):
    host = walle_test.mock_host(
        {
            "state": HostState.ASSIGNED,
            "status": any_task_status(),
            "task": mock_task(
                stage=Stages.CLOUD_POST_PROCESSOR,
                stage_params={"profile_after_task": with_profile, "redeploy_after_task": True},
            ),
        }
    )

    handle_host(host)

    params = {}
    if with_profile:
        params["custom_profile_mode"] = ProfileMode.DEFAULT

    mock_schedule_host_redeployment(host, manual=False, task_type=TaskType.AUTOMATED_HEALING, **params)

    walle_test.hosts.assert_equal()
