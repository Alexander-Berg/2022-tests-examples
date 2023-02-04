from collections import namedtuple
import datetime

import pytest
from mock import patch, Mock
from hamcrest import equal_to, assert_that, contains_exactly

from tests import ws_responses
from tests.processes_testing_instances import prepare_process_instance, run_process
from yb_darkspirit import scheme
from yb_darkspirit.core.cash_register import NotReadyException
from yb_darkspirit.core.cash_register.maintenance_actions import (
    ClearDeviceReregistrationAction, ConfigureAction, NewReregistrationAction, ReregisterAction,
    WaitForIssueAfterFSTicketClosedAction, WaitForFSTicketClosedAction, SetDatetimeAction, ChangeFSAction,
    CreateApplicationIssueTicketForReregistrationAction, WaitForApplicationIssueTicketForReregistrationClosedAction,
    FnsResendAction, UploadToS3ReregistrationCardAction, CreateReregistrationApplicationAction,
    UploadToFnsregReregistrationApplicationAction,
    AttachReregCardToTicketAction, CloseReregTicketAction, TurnOffLEDAction,
    SwitchToWaitForApplicationIssueTicketForReregistrationClosedAction, WaitFnsReregistrationUpload, OpenShiftAction
)
from yb_darkspirit.core.cash_register.state_checks import Checks
from yb_darkspirit.process.base import ProcessRunStatus
from yb_darkspirit.process.reregistration import ReRegistrationProcess, STAGES_LIST
from yb_darkspirit.process.stages import Stage, WaitingStage, SwitchingStage
from yb_darkspirit.scheme import DsCashState


@patch.object(TurnOffLEDAction, 'apply_on_cash_register', Mock())
@patch.object(ClearDeviceReregistrationAction, 'apply_on_cash_register', Mock())
@patch.object(NewReregistrationAction, 'apply_on_cash_register', Mock())
@patch.object(ReregisterAction, 'apply_on_cash_register', Mock())
@patch.object(WaitForIssueAfterFSTicketClosedAction, 'apply_if_skipped', Mock())
@patch.object(WaitForFSTicketClosedAction, 'apply_if_skipped', Mock())
@patch.object(SetDatetimeAction, 'apply_on_cash_register', Mock())
@patch.object(ChangeFSAction, 'get_or_create_ticket', Mock(return_value=''))
@patch.object(ConfigureAction, 'apply_on_cash_register', Mock())
@patch.object(CreateApplicationIssueTicketForReregistrationAction, 'get_or_create_ticket', Mock(return_value=''))
@patch.object(WaitForApplicationIssueTicketForReregistrationClosedAction, 'apply_if_skipped', Mock())
@patch.object(FnsResendAction, 'apply_on_cash_register', Mock())
@patch.object(UploadToS3ReregistrationCardAction, 'apply_on_cash_register', Mock(return_value={'upload_info': {}}))
@patch.object(CreateReregistrationApplicationAction, 'apply_on_cash_register', Mock(return_value={'upload_info': {}}))
@patch.object(UploadToFnsregReregistrationApplicationAction, 'apply_on_cash_register', Mock(return_value={'upload_info': {}}))
@patch.object(AttachReregCardToTicketAction, 'apply_on_cash_register', Mock())
@patch.object(CloseReregTicketAction, 'apply_on_cash_register', Mock())
@patch.object(WaitFnsReregistrationUpload, 'wait_for_', Mock(return_value=datetime.timedelta(minutes=10)))
@patch.object(OpenShiftAction, 'apply_on_cash_register', Mock())
def test_process_runs_all_stages(
        session, cr_wrapper_postfiscal, check_if_ready_for_actions,
        get_or_create_ticket_issue_after_change_fs, need_wait_fs, need_wait_issue,
):
    cr_wrapper = cr_wrapper_postfiscal

    process = ReRegistrationProcess()
    get_or_create_ticket_issue_after_change_fs.return_value = ''

    need_wait_fs.return_value = None
    need_wait_issue.return_value = None

    instance = prepare_process_instance(cr_wrapper, process, process.initial_stage())
    with session.begin():
        session.add(instance)
        stage = namedtuple('stage', ['action', 'skip'])
        stages = [
            stage(stage_.maintenance_action.get_name(), type(stage_) in [WaitingStage, SwitchingStage])
            for stage_ in STAGES_LIST
        ]

        res = None
        for stage in stages:
            check_if_ready_for_actions.return_value = {stage.action: None if stage.skip else []}
            res = run_process(session, instance, process)

    assert_that(instance.cash_register.ds_state, equal_to(DsCashState.OK.value))
    assert_that(res.status, equal_to(ProcessRunStatus.COMPLETE))


def test_fiscal_storage_model_check_supported_model(session, cr_wrapper_maker):
    cr = cr_wrapper_maker.ready_to_reregister_nonconfigured_fs_ready()
    process = ReRegistrationProcess()
    proc_instance = prepare_process_instance(cr, process, Stage(NewReregistrationAction))
    with session.begin():
        session.add(proc_instance)
        run_process(session, proc_instance, process)
    assert True  # No exception occurred


def test_fiscal_storage_model_check_unsupported_model(session, cr_wrapper_maker):
    cr = cr_wrapper_maker.ready_to_reregister_nonconfigured_fs_ready()
    process = ReRegistrationProcess()
    proc_instance = prepare_process_instance(cr, process, Stage(NewReregistrationAction))
    with session.begin():
        cr.fiscal_storage.type = session.query(scheme.FiscalStorageType).filter(
            scheme.FiscalStorageType.code == ws_responses.UNSUPPORTED_FS_TYPE_CODE
        ).one()
        session.add(proc_instance)
        with pytest.raises(NotReadyException) as exinfo:
            run_process(session, proc_instance, process)
        exc = exinfo.value  # type: NotReadyException
        assert len(exc.failed_checks) == 1
        assert_that(exc.failed_checks[0].check, equal_to(Checks.SUPPORTED_FISCAL_STORAGE_MODEL))


def test_process_from_move_to_ready_to_hide_cash(session, cr_wrapper_postfiscal, check_if_ready_for_actions):
    cr_wrapper = cr_wrapper_postfiscal

    process = ReRegistrationProcess()

    instance = prepare_process_instance(
        cr_wrapper, process, process.stages()['make_ready_to_reregistration'].stage
    )

    check_if_ready_for_actions.return_value = {'MAKE_READY_TO_REREGISTRATION': []}
    with session.begin():
        session.add(instance)
        res = run_process(session, instance, process)
    assert_that(instance.cash_register.ds_state, equal_to(DsCashState.READY_TO_REREGISTRATION.value))
    assert_that(instance.stage, equal_to('change_fs'))

    assert_that(res.status, equal_to(ProcessRunStatus.IN_PROGRESS))


def test_process_switches_to_wait_for_ticket_closed_stage(
        session, cr_wrapper_with_completed_registration, check_if_ready_for_actions
):
    process = ReRegistrationProcess()

    instance = process.create_cash_register_process(
        cr_wrapper_with_completed_registration.cash_register,
        SwitchToWaitForApplicationIssueTicketForReregistrationClosedAction.get_lower_name(),
        {'inn_fns_api_not_available_list': [cr_wrapper_with_completed_registration.current_registration.firm_inn]}
    )

    check_if_ready_for_actions.return_value = {
        SwitchToWaitForApplicationIssueTicketForReregistrationClosedAction.get_name(): []
    }
    with session.begin():
        session.add(instance)
        run_process(session, instance, process)

    assert_that(instance.stage, equal_to(WaitForApplicationIssueTicketForReregistrationClosedAction.get_lower_name()))
