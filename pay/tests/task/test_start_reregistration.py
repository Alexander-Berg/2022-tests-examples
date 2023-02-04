from hamcrest import assert_that, equal_to, empty, has_length
from mock import patch, Mock
from sqlalchemy.orm import Session
from typing import List

from yb_darkspirit import scheme
from yb_darkspirit.process.reregistration import ReRegistrationProcess
from yb_darkspirit.task.start_reregistration_process import StartReregistrationProcessTask
from yb_darkspirit.task.task_manager import TaskManager


def test_start_reregistration_starts_processes(session, cr_wrapper_postfiscal):
    processes = run_start_rereg_task(session)

    assert_that(processes, has_length(1))
    assert_that(processes[0].cash_register.id, equal_to(cr_wrapper_postfiscal.cash_register_id))


def test_start_reregistration_doesnt_start_processes(session, cr_wrapper):
    processes = run_start_rereg_task(session)

    assert_that(processes, empty())


def test_start_reregistration_doesnt_start_processes_that_are_already_in_it(session, cr_wrapper_with_test_process):
    with session.begin():
        cr_wrapper_with_test_process.cash_register.state = scheme.CASH_REGISTER_STATE_POSTFISCAL

    assert cr_wrapper_with_test_process.cash_register.state == scheme.CASH_REGISTER_STATE_POSTFISCAL

    processes = run_start_rereg_task(session)

    assert_that(processes, empty())


def run_start_rereg_task(session):
    # type: (Session) -> List[scheme.CashRegisterProcess]
    task = StartReregistrationProcessTask()

    with session.begin():
        task.run(session, {})

    return session.query(scheme.CashRegisterProcess).filter_by(process=ReRegistrationProcess.name()).all()


def test_start_reregistration_initializes_config(session, cr_wrapper_postfiscal):
    task = StartReregistrationProcessTask()
    config_data = {'config': {'some_data': 'blabla'}}

    with session.begin():
        task.run(session, config_data)

    process = (session.query(scheme.CashRegisterProcess)
                      .filter_by(process=ReRegistrationProcess.name())
                      .one())  # type: scheme.CashRegisterProcess

    assert_that(process.data, equal_to(config_data))


@patch.object(StartReregistrationProcessTask, 'run', Mock(return_value={}))
def test_task_request_calls_run_with_data(session, test_client):
    resp = test_client.post('/v1/task/{task_name}/run'.format(task_name=StartReregistrationProcessTask.name()))

    assert_that(resp.status_code, equal_to(200))
    StartReregistrationProcessTask.run.assert_called()
