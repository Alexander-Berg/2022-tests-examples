"""Tests tasks"""

from unittest.mock import Mock

import pytest

from infra.walle.server.tests.lib.util import monkeypatch_audit_log, mock_task, monkeypatch_function, monkeypatch_method
from walle import audit_log
from walle import hosts
from walle import restrictions
from walle._tasks.task_args import RebootTaskArgs
from walle._tasks.task_creator import get_reboot_task_stages, create_new_task
from walle.constants import SshOperation
from walle.errors import BadRequestError
from walle.stages import StageTerminals
from walle.tasks import validate_hostname, TaskType
from walle.util.tasks import _delete_task_from_cms


class MockedTask:
    def __init__(self):
        self.is_hostname_setted = False

    def set_hostname(self):
        self.is_hostname_setted = True

    def __getattr__(self, attr_name):
        pass


class MockedHost:
    def __init__(self, name):
        self.name = name


def _get_task_and_host(host_name=""):
    task = MockedTask()
    host = MockedHost(host_name)
    return task, host


def test_empty_host_name():
    task, host = _get_task_and_host()
    with pytest.raises(BadRequestError):
        validate_hostname(host)
    assert task.is_hostname_setted is False


def test_host_name_matches_free_host_name_template():
    task, host = _get_task_and_host("free-1.wall-e.yandex.net")
    with pytest.raises(BadRequestError):
        validate_hostname(host)
    assert task.is_hostname_setted is False


def test_new_reboot_task_ssh_fallback_terminators_without_post_check(walle_test, monkeypatch):
    monkeypatch_audit_log(monkeypatch)
    host = walle_test.mock_host({"inv": 1, "project": "mocked-project", "name": "host-name"})

    task_args = RebootTaskArgs(
        issuer="mocked-issuer",
        task_type=TaskType.MANUAL,
        project=host.project,
        host_inv=host.inv,
        host_name=host.name,
        host_uuid=host.uuid,
        scenario_id=host.scenario_id,
        ssh=SshOperation.FALLBACK,
        disable_admin_requests=False,
        monitor_on_completion=False,
        with_auto_healing=True,
        check_post_code=False,
        ignore_cms=False,
        operation_restrictions=restrictions.REBOOT,
    )

    with audit_log.create(**task_args.get_task_params()) as audit_entry:
        sb = get_reboot_task_stages(task_args)
        task = create_new_task(host, task_args, audit_entry, sb)

    expected_terminators = {StageTerminals.FAIL: StageTerminals.SKIP}

    assert task.stages[2].stages[0].terminators == expected_terminators


def test_new_reboot_task_ssh_fallback_terminators_with_post_check(walle_test, monkeypatch):
    monkeypatch_audit_log(monkeypatch)
    host = walle_test.mock_host({"inv": 1, "project": "mocked-project", "name": "host-name"})

    task_args = RebootTaskArgs(
        issuer="mocked-issuer",
        task_type=TaskType.MANUAL,
        project=host.project,
        host_inv=host.inv,
        host_name=host.name,
        host_uuid=host.uuid,
        scenario_id=host.scenario_id,
        ssh=SshOperation.FALLBACK,
        disable_admin_requests=False,
        monitor_on_completion=False,
        with_auto_healing=True,
        check_post_code=True,
        ignore_cms=False,
        operation_restrictions=restrictions.REBOOT,
    )

    with audit_log.create(**task_args.get_task_params()) as audit_entry:
        sb = get_reboot_task_stages(task_args)
        task = create_new_task(host, task_args, audit_entry, sb)

    expected_terminators = {StageTerminals.FAIL: StageTerminals.COMPLETE_PARENT}

    assert task.stages[2].stages[0].stages[0].terminators == expected_terminators


def test_delete_task_from_cms(walle_test, mp):
    def _get_mocked_cms_client():
        mock_cms_client = Mock()
        mock_cms_client.attach_mock(Mock(), "delete_task")
        return mock_cms_client

    PROJECT_ID = "aaa"

    project = walle_test.mock_project(dict(id=PROJECT_ID, tags=[]))
    host = walle_test.mock_host(dict(task=mock_task(cms_task_id="1"), project=project.id, cms_task_id="1"))

    primary_cms_client = _get_mocked_cms_client()

    monkeypatch_method(mp, method=hosts.Host.get_cms_clients, obj=hosts.Host, return_value=[primary_cms_client])

    _delete_task_from_cms(host)

    primary_cms_client.delete_task.assert_called_with(host.cms_task_id)
