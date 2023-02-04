import pytest

from hamcrest import equal_to, assert_that, raises, calling
from mock import patch, Mock
from typing import List

from tests.processes_testing_instances import prepare_process_instance_for_unreg
from yb_darkspirit.process.base import ProcessResult
from yb_darkspirit.process.base import ProcessRunStatus
from yb_darkspirit.process.prepare_unregistered_cash_to_registration import (
    PrepareUnregisteredCashToRegistrationProcess, Stage, WaitingStage
)
from yb_darkspirit.scheme import CashRegisterProcess, DsCashState
from yb_darkspirit import interface
from yb_darkspirit.application.plugins.dbhelper import Session
from yb_darkspirit.core.cash_register import NotReadyException
from yb_darkspirit.core.cash_register.state_manager import DsStateManager
from yb_darkspirit.core.cash_register.state_checks import Checks
from yb_darkspirit.core.cash_register.maintenance_actions import MaintenanceAction, MakeReadyToRegistrationAction, \
    SyncDocumentsAction, ChangeFSAction, TurnOffLEDAction, ClearDeviceAction, SetDatetimeAction

from tests import ws_responses
from tests.whitespirit_mocks import WhiteSpiritMocks
from tests.utils import insert_into_oebs_data


TICKET = 'ISSUE-1'
DEFAULT_PROCESS_PARAMETERS = {}


@patch.object(DsStateManager, 'failed_checks_for_ds_state', Mock(return_value=[]))
@patch.object(interface.CashRegister, 'pull_missing_documents', Mock())
@patch('yb_darkspirit.core.fs_replacement._get_issue', Mock(return_value=TICKET))
def test_pipeline(session, cr_wrapper_unregistered, response_maker, ws_mocks, check_if_ready_for_actions):
    cr_wrapper = cr_wrapper_unregistered
    response_maker.create_fiscalization_xml_response(cr_wrapper)
    insert_into_oebs_data(cr_wrapper.session, 12345, cr_wrapper.serial_number)

    process = PrepareUnregisteredCashToRegistrationProcess()

    instance = prepare_process_instance_for_unreg(cr_wrapper, process, process.initial_stage())

    assert_that(instance.stage, equal_to(SyncDocumentsAction.get_lower_name()))
    check_if_ready_for_actions.return_value = {SyncDocumentsAction.get_name(): []}
    res = _run_process_and_flush_session(session, instance, process)
    assert_that(res.status, equal_to(ProcessRunStatus.IN_PROGRESS))

    assert_that(instance.stage, equal_to(ChangeFSAction.get_lower_name()))
    check_if_ready_for_actions.return_value = {ChangeFSAction.get_name(): []}
    res = _run_process_and_flush_session(session, instance, process)
    assert_that(res.status, equal_to(ProcessRunStatus.IN_PROGRESS))
    assert_that(instance.get_data_field(
        stage_name=ChangeFSAction.get_lower_name(), field_name=ChangeFSAction.ticket_name), equal_to(TICKET)
    )

    assert_that(instance.stage, equal_to('wait_for_fn_change'))
    res = _run_process_and_flush_session(session, instance, process)
    assert_that(res.status, equal_to(ProcessRunStatus.WAITING))

    assert_that(instance.stage, equal_to('wait_for_fn_change'))
    check_if_ready_for_actions.return_value = {}
    res = _run_process_and_flush_session(session, instance, process)
    assert_that(res.status, equal_to(ProcessRunStatus.IN_PROGRESS))

    ws_mocks.cashmachines_ident(
        cr_long_sn=cr_wrapper.whitespirit_key,
    )
    assert_that(instance.stage, equal_to(TurnOffLEDAction.get_lower_name()))
    check_if_ready_for_actions.return_value = {TurnOffLEDAction.get_name(): []}
    res = _run_process_and_flush_session(session, instance, process)
    assert_that(res.status, equal_to(ProcessRunStatus.IN_PROGRESS))

    ws_mocks.cashmachines_clear_device_data(
        cr_long_sn=cr_wrapper.whitespirit_key,
        json={"status": "ok"},
        content_type="application/json"
    )
    assert_that(instance.stage, equal_to(ClearDeviceAction.get_lower_name()))
    check_if_ready_for_actions.return_value = {ClearDeviceAction.get_name(): []}
    res = _run_process_and_flush_session(session, instance, process)
    assert_that(res.status, equal_to(ProcessRunStatus.IN_PROGRESS))

    assert_that(instance.stage, equal_to('wait_clear_device'))
    res = _run_process_and_flush_session(session, instance, process)
    assert_that(res.status, equal_to(ProcessRunStatus.WAITING))

    assert_that(instance.stage, equal_to('wait_clear_device'))
    check_if_ready_for_actions.return_value = {}
    res = _run_process_and_flush_session(session, instance, process)
    assert_that(res.status, equal_to(ProcessRunStatus.IN_PROGRESS))

    assert_that(instance.stage, equal_to(SetDatetimeAction.get_lower_name()))
    res = _run_process_and_flush_session(session, instance, process)
    assert_that(res.status, equal_to(ProcessRunStatus.IN_PROGRESS))

    assert_that(instance.stage, equal_to(MakeReadyToRegistrationAction.get_lower_name()))
    check_if_ready_for_actions.return_value = {MakeReadyToRegistrationAction.get_name(): []}
    res = _run_process_and_flush_session(session, instance, process)
    assert_that(instance.cash_register.ds_state, equal_to(DsCashState.READY_TO_REGISTRATION.value))

    assert_that(res.status, equal_to(ProcessRunStatus.COMPLETE))


TEST_STAGES = {
    'test_stage': Stage(MaintenanceAction, name='test_stage'),
    'next_test_stage': Stage(MaintenanceAction, name='next_test_stage')
}
TEST_STAGE = TEST_STAGES.get('test_stage')


def _run_process_and_flush_session(session, instance, process):
    # type: (Session, CashRegisterProcess, PrepareUnregisteredCashToRegistrationProcess) -> ProcessResult
    res = process.run(session, instance, DEFAULT_PROCESS_PARAMETERS)
    session.add(instance)
    session.flush()
    return res


@patch.object(PrepareUnregisteredCashToRegistrationProcess, '_stages_list', TEST_STAGES.values())
def test_not_ready_exception(session, cr_wrapper_unregistered, check_if_ready_for_actions, ws_mocks):
    # type: (Session, Mock, interface.CashRegister, WhiteSpiritMocks) -> None
    ws_mocks.cashmachines_status(
        cr_long_sn=cr_wrapper_unregistered.whitespirit_key,
        json=ws_responses.CASHMACHINES_STATUS_CR_CLOSE_SHIFT_FS_FISCAL_GOOD
    )
    check_if_ready_for_actions.return_value = {TEST_STAGE.maintenance_action.get_name(): [Checks.NONCONFIGURED]}
    process = PrepareUnregisteredCashToRegistrationProcess()
    cr_wrapper = cr_wrapper_unregistered  # type: interface.CashRegister
    instance = prepare_process_instance_for_unreg(cr_wrapper, process, TEST_STAGE)

    assert_that(
        calling(process.run).with_args(session, instance, DEFAULT_PROCESS_PARAMETERS),
        raises(NotReadyException)
    )
    assert_that(instance.stage, equal_to(TEST_STAGE.name))


@patch.object(PrepareUnregisteredCashToRegistrationProcess, '_stages_list', TEST_STAGES.values())
@patch.object(DsStateManager, 'failed_checks_for_ds_state', Mock())
@patch.object(interface.CashRegister, 'from_mapper')
@patch.object(MaintenanceAction, 'apply')
@pytest.mark.parametrize('is_ready_for_registration,times_run', [
    ({TEST_STAGE.maintenance_action.get_name(): []}, 1),  # No blocking checks -> run stage
    ({}, 0)  # Skip stage
])
def test_one_stage_run(session, cr_wrapper_unregistered, check_if_ready_for_actions, is_ready_for_registration, times_run):
    # type: (Session, interface.CashRegister, Mock, List[Checks], bool) -> None
    check_if_ready_for_actions.return_value = is_ready_for_registration
    process = PrepareUnregisteredCashToRegistrationProcess()
    cr_wrapper = cr_wrapper_unregistered  # type: interface.CashRegister
    instance = prepare_process_instance_for_unreg(cr_wrapper, process, TEST_STAGE)

    assert_that(process.run(session, instance, DEFAULT_PROCESS_PARAMETERS).status, equal_to(ProcessRunStatus.IN_PROGRESS))
    assert_that(instance.stage, equal_to('next_test_stage'))
    assert_that(MaintenanceAction.apply.call_count, equal_to(times_run))



WAITING_STAGES = {
    process_stage.stage for process_stage in PrepareUnregisteredCashToRegistrationProcess.stages().values()
    if isinstance(process_stage.stage, WaitingStage)
}
PREFINAL_STAGE = PrepareUnregisteredCashToRegistrationProcess.stages().get(MakeReadyToRegistrationAction.get_lower_name()).stage
ACTION_STAGES = {
    process_stage.stage for process_stage in PrepareUnregisteredCashToRegistrationProcess.stages().values()
    if process_stage.stage not in WAITING_STAGES and process_stage.stage != PREFINAL_STAGE
}


@pytest.mark.parametrize('stage', ACTION_STAGES)
@patch.object(interface.CashRegister, 'from_mapper', Mock())
@patch.object(DsStateManager, 'failed_checks_for_ds_state', Mock())
@patch('yb_darkspirit.core.fs_replacement._get_issue', Mock(return_value=TICKET))
def test_switch_to_the_next_stage_if_not_a_waiting_stage(session, cr_wrapper_unregistered, ws_mocks,
                                                         check_if_ready_for_actions, stage):
    # type: (Session, interface.CashRegister, WhiteSpiritMocks, Mock, Stage) -> None
    check_if_ready_for_actions.return_value = {stage.maintenance_action.get_name(): []}
    process = PrepareUnregisteredCashToRegistrationProcess()
    cr_wrapper = cr_wrapper_unregistered  # type: interface.CashRegister
    instance = prepare_process_instance_for_unreg(cr_wrapper, process, stage)
    session.add(instance)
    session.flush()

    assert_that(process.run(session, instance, DEFAULT_PROCESS_PARAMETERS).status, equal_to(ProcessRunStatus.IN_PROGRESS))
    assert_that(
        instance.stage,
        equal_to(
            PrepareUnregisteredCashToRegistrationProcess._find_next_stage_in_stages_list(stage).name
        )
    )


@patch.object(interface.CashRegister, 'from_mapper')
@patch.object(DsStateManager, 'failed_checks_for_ds_state', Mock(return_value=[]))
def test_switch_to_the_next_stage_if_it_is_a_prefinal_stage(session, cr_wrapper_unregistered, ws_mocks,
                                                            check_if_ready_for_actions):
    # type: (Session, interface.CashRegister, WhiteSpiritMocks, Mock) -> None
    ws_mocks.cashmachines_status(
        cr_long_sn=cr_wrapper_unregistered.whitespirit_key,
        json=ws_responses.CASHMACHINES_STATUS_CR_CLOSE_SHIFT_FS_FISCAL_GOOD
    )
    stage = PREFINAL_STAGE
    check_if_ready_for_actions.return_value = {stage.maintenance_action.get_name(): []}
    process = PrepareUnregisteredCashToRegistrationProcess()
    cr_wrapper = cr_wrapper_unregistered  # type: interface.CashRegister
    instance = prepare_process_instance_for_unreg(cr_wrapper, process, stage)
    session.add(instance)
    session.flush()

    assert_that(process.run(session, instance, DEFAULT_PROCESS_PARAMETERS).status, equal_to(ProcessRunStatus.COMPLETE))


@pytest.mark.parametrize('stage', WAITING_STAGES)
def test_stay_at_the_stage_if_a_waiting_stage(session, cr_wrapper_unregistered, ws_mocks, check_if_ready_for_actions, stage):
    # type: (Session, interface.CashRegister, WhiteSpiritMocks, Mock, Stage) -> None
    ws_mocks.cashmachines_status(
        cr_long_sn=cr_wrapper_unregistered.whitespirit_key,
        json=ws_responses.CASHMACHINES_STATUS_CR_CLOSE_SHIFT_FS_FISCAL_GOOD
    )
    check_if_ready_for_actions.return_value = {stage.maintenance_action.get_name(): []}
    process = PrepareUnregisteredCashToRegistrationProcess()
    cr_wrapper = cr_wrapper_unregistered  # type: interface.CashRegister
    instance = prepare_process_instance_for_unreg(cr_wrapper, process, stage)
    session.add(instance)
    session.flush()

    assert_that(process.run(session, instance, DEFAULT_PROCESS_PARAMETERS).status, equal_to(ProcessRunStatus.WAITING))
    assert_that(instance.stage, equal_to(stage.name))
