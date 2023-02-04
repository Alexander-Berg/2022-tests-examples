from infra.walle.server.tests.lib.util import monkeypatch_audit_log
from walle import audit_log
from walle._tasks.sb_helpers import set_downtime
from walle._tasks.task_args import BaseTaskArgs
from walle._tasks.task_provider import schedule_task_from_scenario, create_new_task
from walle.audit_log import LogEntry
from walle.audit_log import TYPE_PREPARE_HOST
from walle.clients.juggler import JugglerDowntimeName
from walle.hosts import HostState
from walle.operations_log.constants import Operation
from walle.stages import Stages
from walle.util.tasks import StageBuilder


def mock_get_stages_func(task_args):
    sb = StageBuilder()
    set_downtime(sb)
    return sb


def get_mock_task_args():
    task_args = BaseTaskArgs(
        issuer="test_issuer",
        task_type="manual",
        project="test-project",
        host_inv=0,
        host_name="test_fqdn",
        host_uuid="test_uuid",
        scenario_id=2,
        operation_type="test",
        operation_host_status="preparing",
        cms_action="test",
        type=TYPE_PREPARE_HOST,
        disable_admin_requests=True,
        monitor_on_completion=False,
    )
    return task_args


def mock_host_task(host, task_args):
    sb = StageBuilder()
    sb.stage(name=Stages.SET_DOWNTIME, juggler_downtime_name=JugglerDowntimeName.DEFAULT)
    host.task = create_new_task(host, task_args, LogEntry(id=audit_log._uuid()), sb)
    host.task.task_id -= 1
    host.set_status(Operation.PREPARE.host_status, "test_issuer", host.task.audit_log_id)


def test_schedule_task_from_scenario_successfully(walle_test, mp, monkeypatch_timestamp):
    monkeypatch_audit_log(mp)
    host = walle_test.mock_host({"inv": 0, "state": HostState.ASSIGNED}, save=True)
    task_args = get_mock_task_args()
    schedule_task_from_scenario(host, task_args, mock_get_stages_func)
    mock_host_task(host, task_args)
    walle_test.hosts.assert_equal()
