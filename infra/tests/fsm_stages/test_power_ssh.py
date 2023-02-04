"""Tests host powering on/off."""

from unittest.mock import call

import pytest

import walle.util.tasks
from infra.walle.server.tests.lib.util import (
    TestCase,
    handle_host,
    mock_task,
    monkeypatch_clients_for_host,
    mock_commit_stage_changes,
    check_stage_initialization,
    mock_fail_current_stage,
    mock_skip_current_stage,
    mock_complete_parent_stage,
    mock_complete_parent_of_composite_stage,
)
from walle.fsm_stages import power_ssh, power_ipmi
from walle.models import timestamp
from walle.stages import Stage, Stages, StageTerminals


@pytest.fixture
def test(request, monkeypatch_timestamp):
    return TestCase.create(request)


# Stage initialization
def test_initiate_ssh_reboot(test):
    check_stage_initialization(test, Stage(name=Stages.SSH_REBOOT), status=power_ssh._STATUS_REBOOT)


# Test actual workflow


def test_ssh_reboot_begin(test, monkeypatch):
    stage = Stage(name=Stages.SSH_REBOOT, status=power_ssh._STATUS_REBOOT, status_time=timestamp())
    host = test.mock_host({"task": mock_task(stage=stage)})
    clients = monkeypatch_clients_for_host(monkeypatch, host, boot_id="10000001-001")

    handle_host(host)
    assert clients.mock_calls == [call.ssh.get_boot_id(), call.ssh.issue_reboot_command()]

    stage.set_temp_data("boot_id", "10000001-001")
    mock_commit_stage_changes(host, status=power_ssh._STATUS_REBOOTING, check_after=power_ssh._REBOOT_DELAY)
    test.hosts.assert_equal()


def test_ssh_reboot_pending(test, monkeypatch):
    stage = Stage(name=Stages.SSH_REBOOT, status=power_ssh._STATUS_REBOOTING, status_time=timestamp())
    stage.set_temp_data("boot_id", "10000001-001")

    host = test.mock_host({"task": mock_task(stage=stage)})
    clients = monkeypatch_clients_for_host(monkeypatch, host, boot_id="10000001-001")

    handle_host(host)
    assert clients.mock_calls == [call.ssh.get_boot_id()]

    stage.set_temp_data("boot_id", "10000001-001")
    mock_commit_stage_changes(host, check_after=power_ssh._CHECK_PERIOD)
    test.hosts.assert_equal()


def test_ssh_reboot_pending_host_down(test, monkeypatch):
    stage = Stage(name=Stages.SSH_REBOOT, status=power_ssh._STATUS_REBOOTING, status_time=timestamp())
    stage.set_temp_data("boot_id", "10000001-001")

    host = test.mock_host({"task": mock_task(stage=stage)})
    clients = monkeypatch_clients_for_host(monkeypatch, host, mock_ssh_error=True)

    handle_host(host)
    assert clients.mock_calls == [call.ssh.get_boot_id()]

    mock_commit_stage_changes(host, check_after=power_ssh._CHECK_PERIOD)
    test.hosts.assert_equal()


def test_ssh_reboot_pending_authentication_failure(test, monkeypatch):
    stage = Stage(name=Stages.SSH_REBOOT, status=power_ssh._STATUS_REBOOTING, status_time=timestamp())
    stage.set_temp_data("boot_id", "10000001-001")

    host = test.mock_host({"task": mock_task(stage=stage)})
    clients = monkeypatch_clients_for_host(monkeypatch, host, mock_ssh_auth_error=True)

    handle_host(host)
    assert clients.mock_calls == []

    reason = "Failed to reboot host via ssh: SSH connection failed: ssh authentication failed mock"
    mock_fail_current_stage(host, reason=reason)
    test.hosts.assert_equal()


def test_ssh_reboot_completed(test, monkeypatch):
    stage = Stage(name=Stages.SSH_REBOOT, status=power_ssh._STATUS_REBOOTING, status_time=timestamp())
    stage.set_temp_data("boot_id", "10000001-001")

    host = test.mock_host({"task": mock_task(stage=stage)})
    clients = monkeypatch_clients_for_host(monkeypatch, host, boot_id="10000001-002")

    handle_host(host)
    assert clients.mock_calls == [call.ssh.get_boot_id()]

    mock_complete_parent_stage(host)
    test.hosts.assert_equal()


def test_ssh_reboot_failure(test, monkeypatch):
    stage = Stage(name=Stages.SSH_REBOOT, status=power_ssh._STATUS_REBOOT)

    host = test.mock_host({"task": mock_task(stage=stage)})
    clients = monkeypatch_clients_for_host(monkeypatch, host, mock_ssh_error=True)

    handle_host(host)
    assert clients.mock_calls == [call.ssh.get_boot_id()]

    reason = "Failed to reboot host via ssh: SSH connection failed: ssh connection failed mock."
    mock_fail_current_stage(host, reason=reason)
    test.hosts.assert_equal()


def test_ssh_reboot_timeout(test, monkeypatch):
    stage = Stage(
        name=Stages.SSH_REBOOT, status=power_ssh._STATUS_REBOOTING, status_time=timestamp() - power_ssh._REBOOT_TIMEOUT
    )
    stage.set_temp_data("boot_id", "10000001-001")

    host = test.mock_host({"task": mock_task(stage=stage)})
    clients = monkeypatch_clients_for_host(monkeypatch, host, boot_id="10000001-001")

    handle_host(host)
    assert clients.mock_calls == [call.ssh.get_boot_id()]

    reason = "Failed to reboot host via ssh: Timeout while waiting for the host to actually reboot."
    mock_fail_current_stage(host, reason=reason)
    test.hosts.assert_equal()


def test_ssh_reboot_failure_fallback(test, monkeypatch):
    stage = Stage(
        name=Stages.SSH_REBOOT, status=power_ssh._STATUS_REBOOT, terminators={StageTerminals.FAIL: StageTerminals.SKIP}
    )

    host = test.mock_host({"task": _mock_reboot_task(stage)})
    clients = monkeypatch_clients_for_host(monkeypatch, host, mock_ssh_error=True)

    handle_host(host)
    assert clients.mock_calls == [call.ssh.get_boot_id()]

    mock_skip_current_stage(host, Stages.POWER_OFF, power_ipmi._STATUS_POWER_OFF)
    test.hosts.assert_equal()


def test_ssh_reboot_timeout_fallback(test, monkeypatch):
    stage = Stage(
        name=Stages.SSH_REBOOT,
        status=power_ssh._STATUS_REBOOTING,
        status_time=timestamp() - power_ssh._REBOOT_TIMEOUT,
        terminators={StageTerminals.FAIL: StageTerminals.SKIP},
    )

    stage.set_temp_data("boot_id", "10000001-001")

    host = test.mock_host({"task": _mock_reboot_task(stage)})
    clients = monkeypatch_clients_for_host(monkeypatch, host, boot_id="10000001-001")

    handle_host(host)
    assert clients.mock_calls == [call.ssh.get_boot_id()]

    mock_skip_current_stage(host, Stages.POWER_OFF, power_ipmi._STATUS_POWER_OFF)
    test.hosts.assert_equal()


def _mock_reboot_task(reboot_stage):
    sb = walle.util.tasks.StageBuilder()
    with sb.nested(Stages.REBOOT) as reboot_stages:
        reboot_stages.add_stages([reboot_stage])
        reboot_stages.stage(name=Stages.POWER_OFF)
        reboot_stages.add_stages(walle.util.tasks.get_power_on_stages(check_post_code=True))

    return mock_task(stage=reboot_stage, stages=sb.get_stages())


# tests for POST code checks
@pytest.mark.parametrize(
    "platform_name",
    (
        {"system": "T174-N40", "board": None},
        {"system": None, "board": "MY70-EX0-Y3N"},
    ),
)
def test_reboot_with_post_handling(test, monkeypatch, platform_name):
    host = test.mock_host(
        {
            "platform": platform_name,
            "task": mock_task(
                stage=Stage(name=Stages.SSH_REBOOT, params={"check_post_code": True}, status=power_ssh._STATUS_REBOOT),
            ),
        }
    )
    clients = monkeypatch_clients_for_host(monkeypatch, host, boot_id="10000001-001")

    handle_host(host)

    mock_commit_stage_changes(
        host,
        status=power_ssh._STATUS_WAITING_HOST_DOWN,
        check_after=power_ssh._REBOOT_DELAY,
        temp_data={"boot_id": "10000001-001"},
    )
    assert clients.mock_calls == [call.ssh.get_boot_id(), call.ssh.issue_reboot_command()]
    test.hosts.assert_equal()


@pytest.mark.parametrize(
    "platform_name",
    (
        {"system": "T174-N40", "board": None},
        {"system": None, "board": "MY70-EX0-Y3N"},
    ),
)
def test_wait_host_down_host_still_up(test, monkeypatch, platform_name):
    stage = Stage(name=Stages.SSH_REBOOT, params={"check_post_code": True}, status=power_ssh._STATUS_WAITING_HOST_DOWN)
    stage.set_temp_data("boot_id", "10000001-001")
    host = test.mock_host({"platform": platform_name, "task": mock_task(stage=stage)})
    clients = monkeypatch_clients_for_host(monkeypatch, host, boot_id="10000001-001")

    handle_host(host)

    mock_commit_stage_changes(host, check_after=power_ssh._CHECK_PERIOD)

    assert clients.mock_calls == [call.ssh.get_boot_id()]
    test.hosts.assert_equal()


@pytest.mark.parametrize(
    "platform_name",
    (
        {"system": "T174-N40", "board": None},
        {"system": None, "board": "MY70-EX0-Y3N"},
    ),
)
def test_wait_host_down_host_already_up(test, monkeypatch, platform_name):
    stage = Stage(name=Stages.SSH_REBOOT, params={"check_post_code": True}, status=power_ssh._STATUS_WAITING_HOST_DOWN)
    stage.set_temp_data("boot_id", "10000001-001")
    host = test.mock_host({"platform": platform_name, "task": mock_task(stage=stage)})
    clients = monkeypatch_clients_for_host(monkeypatch, host, boot_id="10000001-002")

    handle_host(host)

    mock_complete_parent_stage(host)

    assert clients.mock_calls == [call.ssh.get_boot_id()]
    test.hosts.assert_equal()


@pytest.mark.parametrize(
    "platform_name",
    (
        {"system": "T174-N40", "board": None},
        {"system": None, "board": "MY70-EX0-Y3N"},
    ),
)
def test_wait_host_down_host_gone_down(test, monkeypatch, platform_name):
    stage = Stage(name=Stages.SSH_REBOOT, params={"check_post_code": True}, status=power_ssh._STATUS_WAITING_HOST_DOWN)
    stage.set_temp_data("boot_id", "10000001-001")
    host = test.mock_host({"platform": platform_name, "task": mock_task(stage=stage)})
    clients = monkeypatch_clients_for_host(monkeypatch, host, mock_ssh_error=power_ssh.ssh.SshConnectionFailedError)

    handle_host(host)

    mock_commit_stage_changes(host, status=power_ssh.STATUS_WAIT_POST_COMPLETE, check_after=power_ssh._POST_CHECK_DELAY)

    assert clients.mock_calls == [call.ssh.get_boot_id()]
    test.hosts.assert_equal()


@pytest.mark.parametrize(
    "platform_name",
    (
        {"system": "T174-N40", "board": None},
        {"system": None, "board": "MY70-EX0-Y3N"},
    ),
)
def test_wait_post_complete_ipmi_ok_post_code(test, monkeypatch, platform_name):
    stage = Stage(name=Stages.SSH_REBOOT, params={"check_post_code": True}, status=power_ssh.STATUS_WAIT_POST_COMPLETE)
    stage.set_temp_data("last_post_code_seen", 0xB2)
    stage.set_temp_data("last_post_code_seen_times", 3)
    host = test.mock_host({"platform": platform_name, "task": mock_task(stage=stage)})

    monkeypatch_clients_for_host(monkeypatch, host, raw_cmd_result={"success": True, "message": " 00 40 b2 80"})

    handle_host(host)

    mock_commit_stage_changes(host, status=power_ssh._STATUS_REBOOTING, check_after=power_ssh._CHECK_PERIOD)

    test.hosts.assert_equal()


@pytest.mark.parametrize(
    "platform_name",
    (
        {"system": "T174-N40", "board": None},
        {"system": None, "board": "MY70-EX0-Y3N"},
    ),
)
def test_wait_reboot_to_complete_ipmi_ok_post_code(test, monkeypatch, platform_name):
    stage = Stage(name=Stages.SSH_REBOOT, params={"check_post_code": True}, status=power_ssh._STATUS_REBOOTING)
    stage.set_temp_data("boot_id", "boot-1")

    composite_stage_mock = Stage(name=Stages.SSH_REBOOT_COMPOSITE, stages=[stage])

    reboot_stage_mock = Stage(name=Stages.REBOOT, stages=[composite_stage_mock])

    host = test.mock_host({"platform": platform_name, "task": mock_task(stage=reboot_stage_mock)})
    # goto ssh-reboot stage
    host.task.stage_uid = "1.2.1.1"
    host.task.stage_name = "ssh-reboot"
    host.save()

    monkeypatch_clients_for_host(monkeypatch, host, boot_id="boot-2")

    handle_host(host)

    mock_complete_parent_of_composite_stage(host)

    test.hosts.assert_equal()


def test_ssh_reboot_fallback_reboot_failure_without_post_check(test, monkeypatch):
    stage = Stage(
        name=Stages.SSH_REBOOT,
        params={"check_post_code": False},
        status=power_ssh._STATUS_REBOOT,
        terminators={StageTerminals.FAIL: StageTerminals.SKIP},
    )

    reboot_stage_mock = Stage(
        name=Stages.REBOOT, stages=[stage, Stage(name=Stages.POWER_OFF), Stage(name=Stages.POWER_ON)]
    )

    host = test.mock_host({"task": mock_task(stage=reboot_stage_mock)})

    # goto ssh-reboot stage
    host.task.stage_uid = "1.2.1"
    host.task.stage_name = "ssh-reboot"
    host.save()

    monkeypatch_clients_for_host(monkeypatch, host, mock_ssh_error=power_ssh.ssh.SshError("SSH Error mock"))

    handle_host(host)

    mock_skip_current_stage(host, expected_name="power-off", expected_status="power-off")

    test.hosts.assert_equal()


def test_ssh_reboot_fallback_reboot_failure_with_post_check(test, monkeypatch):
    stage = Stage(
        name=Stages.SSH_REBOOT,
        params={"check_post_code": False},
        status=power_ssh._STATUS_REBOOT,
        terminators={StageTerminals.FAIL: StageTerminals.SKIP},
    )

    composite_stage_mock = Stage(name=Stages.SSH_REBOOT_COMPOSITE, stages=[stage])

    reboot_stage_mock = Stage(
        name=Stages.REBOOT, stages=[composite_stage_mock, Stage(name=Stages.POWER_OFF), Stage(name=Stages.POWER_ON)]
    )

    host = test.mock_host({"task": mock_task(stage=reboot_stage_mock)})

    # goto ssh-reboot stage
    host.task.stage_uid = "1.2.1.1"
    host.task.stage_name = "ssh-reboot"
    host.save()

    monkeypatch_clients_for_host(monkeypatch, host, mock_ssh_error=power_ssh.ssh.SshError("SSH Error mock"))

    handle_host(host)

    mock_complete_parent_stage(host, expected_name="power-off", expected_status="power-off")

    test.hosts.assert_equal()
