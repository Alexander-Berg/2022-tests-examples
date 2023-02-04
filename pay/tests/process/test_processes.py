# coding=utf-8
import datetime

from hamcrest import assert_that, equal_to, has_entries, calling, raises
from mock import patch, ANY, PropertyMock
from time import sleep
from sqlalchemy.orm import Session
import pytest

from tests.processes_testing_instances import GoodProcess, InfiniteProcess, BadProcess, \
    run_process_with_process_manager, TestProcess, prepare_process_instance, run_process, TestMaintenanceAction, \
    TestingChecks, TestSwitchingAction
from yb_darkspirit import scheme
from yb_darkspirit.core.cash_register import NotReadyException
from yb_darkspirit.process.base import ProcessRunStatus, ProcessResult
from yb_darkspirit.process.process_manager import PROCESSES, ProcessManager
from yb_darkspirit.api.errors import NotFound
from yb_darkspirit.process.stages import Stage, WaitingStage, SwitchingStage


def test_unknown_process_run(session, test_client):
    assert_that(
        calling(run_process_with_process_manager).with_args('unknown', session),
        raises(NotFound)
    )


def spawn_process(session, cr_serial_number, name, stage):
    # type: (Session, str, str, str) -> int
    cash_register = scheme.CashRegister(serial_number=cr_serial_number)
    session.add(cash_register)
    session.flush()

    process = scheme.CashRegisterProcess.insert(
        session,
        cash_register_id=cash_register.id,
        process=name,
        stage=stage,
    )
    session.flush()
    return process.id


@patch.dict(PROCESSES, {GoodProcess.name(): GoodProcess()})
def test_process_run(session):
    spawn_process(session, '12345', name='good', stage='42')

    assert_that(
        run_process_with_process_manager('good', session),
        has_entries({
            'locked': False,
            'failed': 0,
            'success': 1,
            'total': 1
        })
    )

    processes_count = session.query(scheme.CashRegisterProcess).count()
    assert_that(processes_count, equal_to(0))


@pytest.mark.parametrize('instance_limit,instance_left', [(1, 1), (2, 0)])
@patch.dict(PROCESSES, {GoodProcess.name(): GoodProcess()})
def test_process_run_with_limit(session, instance_limit, instance_left):
    spawn_process(session, '12345', name='good', stage='42')
    spawn_process(session, '12346', name='good', stage='42')

    assert_that(
        run_process_with_process_manager('good', session, instance_limit=instance_limit),
        has_entries({
            'locked': False,
            'failed': 0,
            'success': instance_limit,
            'total': instance_limit
        })
    )

    processes_count = session.query(scheme.CashRegisterProcess).count()
    assert_that(processes_count, equal_to(instance_left))


@patch.dict(PROCESSES, {InfiniteProcess.name(): InfiniteProcess()})
@patch.object(ProcessManager, '_run_process_for_an_instance')
def test_process_run_with_limit_use_most_old_instances(run_for_an_instance_mock, committed_objects, commitable_session):
    process_ids = committed_objects
    run_for_an_instance_mock.return_value = ProcessResult(ProcessRunStatus.IN_PROGRESS)

    def sleep_run_and_check(process_id):
        sleep(1)
        run_for_an_instance_mock.reset()
        run_process_with_process_manager('test_process', commitable_session)
        run_for_an_instance_mock.assert_called_with(process_id, ANY, ANY)

    sleep_run_and_check(process_ids[0])
    sleep_run_and_check(process_ids[1])
    sleep_run_and_check(process_ids[0])
    sleep_run_and_check(process_ids[1])


@patch.dict(PROCESSES, {BadProcess.name(): BadProcess()})
def test_process_run_with_failure(another_session, committed_objects, session):
    assert_that(
        run_process_with_process_manager('test_process', another_session),
        has_entries({
            'locked': False,
            'failed': 1,
            'success': 0,
            'total': 1
        })
    )

    processes_count = another_session.query(scheme.CashRegisterProcess).count()
    assert_that(processes_count, equal_to(2))


@pytest.mark.parametrize('stage', [WaitingStage, Stage])
@pytest.mark.parametrize('readiness_checks', [{TestingChecks.TEST_CHECK_TRUE}, {TestingChecks.TEST_CHECK_FALSE}])
@patch.object(TestProcess, '_stages_list', PropertyMock())
@patch.object(TestMaintenanceAction, 'readiness_checks_', PropertyMock())
def test_process_workflow_if_need_wait_with_skip_check_failed_results_waiting_and_does_nothing(
        session, cr_wrapper, maintenance_test_action_apply_if_skipped, maintenance_test_action_need_wait,
        maintenance_test_action_apply_on_cash_register, stage, readiness_checks
):
    TestMaintenanceAction.readiness_checks_ = readiness_checks
    maintenance_test_action_need_wait.return_value = datetime.timedelta(minutes=5)
    TestProcess._stages_list = [stage(TestMaintenanceAction)]

    result = _init_and_run_test_process(session, cr_wrapper)

    assert_that(maintenance_test_action_apply_if_skipped.called, equal_to(False))
    assert_that(maintenance_test_action_apply_on_cash_register.called, equal_to(False))
    assert_that(result.status, equal_to(ProcessRunStatus.WAITING))


@pytest.mark.parametrize('stage', [WaitingStage, Stage])
@patch.object(TestProcess, '_stages_list', PropertyMock())
@patch.object(TestMaintenanceAction, 'readiness_checks_', {TestingChecks.TEST_CHECK_FALSE})
def test_process_workflow_fails_on_readiness_checks(
        session, cr_wrapper, maintenance_test_action_apply_if_skipped,
        maintenance_test_action_apply_on_cash_register, stage
):
    TestProcess._stages_list = [stage(TestMaintenanceAction)]

    result = None
    with pytest.raises(NotReadyException):
        result = _init_and_run_test_process(session, cr_wrapper)

    assert_that(maintenance_test_action_apply_if_skipped.called, equal_to(False))
    assert_that(maintenance_test_action_apply_on_cash_register.called, equal_to(False))
    assert_that(result, equal_to(None))  # не оч хорошо, что None, но так пока работает


@pytest.mark.parametrize('stage', [WaitingStage, Stage])
@pytest.mark.parametrize('readiness_checks', [{TestingChecks.TEST_CHECK_TRUE}, {TestingChecks.TEST_CHECK_FALSE}])
@pytest.mark.parametrize('wait_for', [datetime.timedelta(minutes=5), None])
@patch.object(TestProcess, '_stages_list', PropertyMock())
@patch.object(TestMaintenanceAction, 'skipping_checks_', {TestingChecks.TEST_CHECK_TRUE})
@patch.object(TestMaintenanceAction, 'readiness_checks_', PropertyMock())
def test_process_workflow_with_passing_skipping_checks_always_skips_stage_and_applies_apply_if_skipped(
        session, cr_wrapper, maintenance_test_action_apply_if_skipped, maintenance_test_action_need_wait,
        maintenance_test_action_apply_on_cash_register, stage, readiness_checks, wait_for
):

    TestProcess._stages_list = [stage(TestMaintenanceAction)]
    TestMaintenanceAction.readiness_checks_ = readiness_checks
    maintenance_test_action_need_wait.return_value = wait_for

    result = _init_and_run_test_process(session, cr_wrapper)

    assert_that(maintenance_test_action_apply_if_skipped.called, equal_to(True))
    assert_that(maintenance_test_action_apply_on_cash_register.called, equal_to(False))
    assert_that(result.status, equal_to(ProcessRunStatus.COMPLETE))


def test_process_workflow_applies_and_not_skipped(
        session, cr_wrapper, maintenance_test_action_apply_if_skipped,
        maintenance_test_action_apply_on_cash_register
):
    result = _init_and_run_test_process(session, cr_wrapper)

    assert_that(maintenance_test_action_apply_if_skipped.called, equal_to(False))
    assert_that(maintenance_test_action_apply_on_cash_register.called, equal_to(True))
    assert_that(result.status, equal_to(ProcessRunStatus.COMPLETE))


@patch.object(TestProcess, '_stages_list', PropertyMock())
def test_process_workflow_waits_on_waiting_stage(
        session, cr_wrapper, maintenance_test_action_apply_if_skipped,
        maintenance_test_action_apply_on_cash_register
):
    TestProcess._stages_list = [WaitingStage(TestMaintenanceAction)]

    result = _init_and_run_test_process(session, cr_wrapper)

    assert_that(maintenance_test_action_apply_if_skipped.called, equal_to(False))
    assert_that(maintenance_test_action_apply_on_cash_register.called, equal_to(False))
    assert_that(result.status, equal_to(ProcessRunStatus.WAITING))


@patch.object(TestProcess, '_stages_list', PropertyMock())
def test_process_workflow_switches_on_switching_stage(session, cr_wrapper):
    TestProcess._stages_list = [
        SwitchingStage(TestSwitchingAction, name='switching'),
        WaitingStage(TestMaintenanceAction, name='waiting'),
        Stage(TestMaintenanceAction, name='final')
    ]

    _init_and_run_test_process(session, cr_wrapper)

    assert_that(cr_wrapper.current_process.stage, equal_to('final'))


@patch.object(TestSwitchingAction, 'skipping_checks_', {TestingChecks.TEST_CHECK_TRUE})
@patch.object(TestProcess, '_stages_list', PropertyMock())
def test_process_workflow_doesnt_switch_on_switching_stage_if_skipping_check_succeeds(session, cr_wrapper):
    TestProcess._stages_list = [
        SwitchingStage(TestSwitchingAction, name='switching'),
        WaitingStage(TestMaintenanceAction, name='waiting'),
        Stage(TestMaintenanceAction, name='final')
    ]

    _init_and_run_test_process(session, cr_wrapper)

    assert_that(cr_wrapper.current_process.stage, equal_to('waiting'))


def _init_and_run_test_process(session, cr_wrapper):
    process = TestProcess()
    instance = prepare_process_instance(cr_wrapper, process)
    with session.begin():
        session.add(instance)
        return run_process(session, instance, process)


def test_process_instance_get_data_field(session, cr_wrapper):
    process = TestProcess()
    instance = prepare_process_instance(cr_wrapper, process)

    stage_name, field_name, field_value = 'some_stage', 'field', 'value'

    with session.begin():
        instance.data = {stage_name: {field_name: field_value}}

    assert_that(instance.get_data_field(stage_name=stage_name, field_name=field_name), equal_to(field_value))


def test_process_instance_set_data_field_for_stage(session, cr_wrapper):
    process = TestProcess()
    instance = prepare_process_instance(cr_wrapper, process)

    field_name, field_value = 'field', 'value'

    expected_data = {process.initial_stage().name: {field_name: field_value}, 'config': {}}

    with session.begin():
        instance.set_data_field_for_stage(field_name=field_name, field_value=field_value)

    assert_that(instance.data, equal_to(expected_data))
