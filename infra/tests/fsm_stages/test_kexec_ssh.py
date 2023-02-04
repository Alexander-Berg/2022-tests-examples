"""Tests host powering on/off."""

from unittest.mock import call

import pytest

from infra.walle.server.tests.lib.util import (
    TestCase,
    handle_host,
    mock_task,
    monkeypatch_clients_for_host,
    mock_commit_stage_changes,
    check_stage_initialization,
    mock_fail_current_stage,
    mock_complete_current_stage,
)
from walle.fsm_stages import kexec_ssh
from walle.models import timestamp
from walle.stages import Stage, Stages


@pytest.fixture
def test(request, monkeypatch_timestamp):
    return TestCase.create(request)


# Stage initialization
def test_initiate_kexec_reboot(test):
    check_stage_initialization(test, Stage(name=Stages.KEXEC_REBOOT), status=kexec_ssh._STATUS_KEXEC_REBOOT)


# Test actual workflow


def test_kexec_reboot_begin(test, monkeypatch):
    stage = Stage(name=Stages.KEXEC_REBOOT, status=kexec_ssh._STATUS_KEXEC_REBOOT, status_time=timestamp())
    host = test.mock_host({"task": mock_task(stage=stage)})
    clients = monkeypatch_clients_for_host(monkeypatch, host, boot_id="10000001-001")

    handle_host(host)
    assert clients.mock_calls == [call.ssh.get_boot_id(), call.ssh.issue_kexec_reboot_command()]

    stage.set_temp_data("boot_id", "10000001-001")
    mock_commit_stage_changes(host, status=kexec_ssh._STATUS_KEXEC_REBOOTING, check_after=kexec_ssh._REBOOT_DELAY)
    test.hosts.assert_equal()


def test_kexec_reboot_pending(test, monkeypatch):
    stage = Stage(name=Stages.KEXEC_REBOOT, status=kexec_ssh._STATUS_KEXEC_REBOOTING, status_time=timestamp())
    stage.set_temp_data("boot_id", "10000001-001")

    host = test.mock_host({"task": mock_task(stage=stage)})
    clients = monkeypatch_clients_for_host(monkeypatch, host, boot_id="10000001-001")

    handle_host(host)
    assert clients.mock_calls == [call.ssh.get_boot_id()]

    stage.set_temp_data("boot_id", "10000001-001")
    mock_commit_stage_changes(host, check_after=kexec_ssh._CHECK_PERIOD)
    test.hosts.assert_equal()


def test_kexec_reboot_pending_host_down(test, monkeypatch):
    stage = Stage(name=Stages.KEXEC_REBOOT, status=kexec_ssh._STATUS_KEXEC_REBOOTING, status_time=timestamp())
    stage.set_temp_data("boot_id", "10000001-001")

    host = test.mock_host({"task": mock_task(stage=stage)})
    clients = monkeypatch_clients_for_host(monkeypatch, host, mock_ssh_error=True)

    handle_host(host)
    assert clients.mock_calls == [call.ssh.get_boot_id()]

    mock_commit_stage_changes(host, check_after=kexec_ssh._CHECK_PERIOD)
    test.hosts.assert_equal()


def test_kexec_reboot_pending_authentication_failure(test, monkeypatch):
    stage = Stage(name=Stages.KEXEC_REBOOT, status=kexec_ssh._STATUS_KEXEC_REBOOTING, status_time=timestamp())
    stage.set_temp_data("boot_id", "10000001-001")

    host = test.mock_host({"task": mock_task(stage=stage)})
    clients = monkeypatch_clients_for_host(monkeypatch, host, mock_ssh_auth_error=True)

    handle_host(host)
    assert clients.mock_calls == []

    reason = "Failed to reboot host via kexec: SSH connection failed: ssh authentication failed mock"
    mock_fail_current_stage(host, reason=reason)
    test.hosts.assert_equal()


def test_kexec_reboot_completed(test, monkeypatch):
    stage = Stage(name=Stages.KEXEC_REBOOT, status=kexec_ssh._STATUS_KEXEC_REBOOTING, status_time=timestamp())
    stage.set_temp_data("boot_id", "10000001-001")

    host = test.mock_host({"task": mock_task(stage=stage)})
    clients = monkeypatch_clients_for_host(monkeypatch, host, boot_id="10000001-002")

    handle_host(host)
    assert clients.mock_calls == [call.ssh.get_boot_id()]

    mock_complete_current_stage(host)
    test.hosts.assert_equal()


def test_kexec_reboot_failure(test, monkeypatch):
    stage = Stage(name=Stages.KEXEC_REBOOT, status=kexec_ssh._STATUS_KEXEC_REBOOT)

    host = test.mock_host({"task": mock_task(stage=stage)})
    clients = monkeypatch_clients_for_host(monkeypatch, host, mock_ssh_error=True)

    handle_host(host)
    assert clients.mock_calls == [call.ssh.get_boot_id()]

    reason = "Failed to reboot host via kexec: SSH connection failed: ssh connection failed mock."
    mock_fail_current_stage(host, reason=reason)
    test.hosts.assert_equal()


def test_kexec_reboot_timeout(test, monkeypatch):
    stage = Stage(
        name=Stages.KEXEC_REBOOT,
        status=kexec_ssh._STATUS_KEXEC_REBOOTING,
        status_time=timestamp() - kexec_ssh._KEXEC_REBOOT_TIMEOUT,
    )
    stage.set_temp_data("boot_id", "10000001-001")

    host = test.mock_host({"task": mock_task(stage=stage)})
    clients = monkeypatch_clients_for_host(monkeypatch, host, boot_id="10000001-001")

    handle_host(host)
    assert clients.mock_calls == [call.ssh.get_boot_id()]

    reason = "Failed to reboot host via kexec: Timeout while waiting for the host to actually reboot via kexec."
    mock_fail_current_stage(host, reason=reason)
    test.hosts.assert_equal()
