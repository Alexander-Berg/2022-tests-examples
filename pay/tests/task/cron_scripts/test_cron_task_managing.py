from datetime import timedelta

from hamcrest.core import assert_that
from hamcrest.core.core import is_
from mock import patch
from mock import Mock

from yb_darkspirit import scheme
from yb_darkspirit.task.cron_scripts.cron_task_managing import CronTaskManager, SubProcess
from yb_darkspirit.task.cron_scripts.exceptions import TimeoutException


def prepare_params_and_run_task_with_timeout(session, cr_wrapper, task_name):
    def ws_action(session, ws, timeout):
        raise TimeoutException

    task = scheme.Task(
        task_name=task_name, state=scheme.TaskState.NOT_ASSIGNED.value, init_uuid='123',
        params={'whitespirit_url': cr_wrapper.cash_register.whitespirit_url}
    )
    with session.begin():
        session.add(task)

    task_manager = CronTaskManager(timeout=timedelta(0))

    task_manager.run_ws_task(session, task_name, ws_action)


@patch.object(CronTaskManager, 'finish_task', Mock())
def test_run_ws_task_writes_log_on_timeout(session, cr_wrapper, disabled_metrics_subprocess_collect):
    prepare_params_and_run_task_with_timeout(session, cr_wrapper, 'test')

    disabled_metrics_subprocess_collect.assert_called_with(SubProcess.LOG_TIMEOUT.value)


def test_run_ws_task_finishes_on_timeout(session, cr_wrapper, disabled_metrics_subprocess_collect):
    task_name = 'test'
    prepare_params_and_run_task_with_timeout(session, cr_wrapper, task_name)

    disabled_metrics_subprocess_collect.assert_called_with(SubProcess.FINISH_TASK.value)

    task = session.query(scheme.Task).filter_by(task_name=task_name).one_or_none()

    assert_that(task, is_(None))
