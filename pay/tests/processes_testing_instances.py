import json

from enum import Enum
from mock import Mock
from typing import Optional

from yb_darkspirit.api.schemas import ProcessParametersSchema
from yb_darkspirit.core.cash_register.state_checks import (
    CashRegisterBaseInfo, Check, FailedCheck, CheckResult, FailedCheckResult
)
from yb_darkspirit.interface import CashRegister
from yb_darkspirit.process.base import Process, ProcessResult, ProcessRunStatus
from yb_darkspirit.core.cash_register.maintenance_actions import MaintenanceAction, SwitchToActionMixin
from yb_darkspirit.process.process_manager import ProcessManager
from yb_darkspirit.process.stages import Stage
from yb_darkspirit.scheme import CashRegisterProcess


def check_success(info):
    return CheckResult()


def check_fail(info):
    return FailedCheckResult('False')


class TestingChecks(Enum):
    def __call__(self, info):
        return self.value(info)

    TEST_CHECK_TRUE = Check(check_success)
    TEST_CHECK_FALSE = Check(check_fail)

    def to_failed(self, failed_check_result):
        return FailedCheck(self, failed_check_result)


class TestMaintenanceAction(MaintenanceAction):
    name_ = "TEST"
    skipping_checks_ = {TestingChecks.TEST_CHECK_FALSE}
    readiness_checks_ = {TestingChecks.TEST_CHECK_TRUE}

    @classmethod
    def apply_on_cash_register(cls, session, cash_register, reason):
        pass


class TestProcess(Process):
    """
        Process for tests.
        You can patch stages_list with patch.object to get different stages
    """
    _stages_list = [
        Stage(TestMaintenanceAction),
    ]

    @classmethod
    def name(cls):
        return 'test'

    @classmethod
    def expected_ds_state(cls):
        pass


class GoodProcess(Process):
    @classmethod
    def name(cls):
        return 'good'

    def run(self, session, instance, params):
        return ProcessResult(ProcessRunStatus.COMPLETE)


class InfiniteProcess(Process):
    @classmethod
    def name(cls):
        return 'test_process'

    def run(self, session, instance, params):
        return ProcessResult(ProcessRunStatus.IN_PROGRESS)


class BadProcess(Process):
    @classmethod
    def name(cls):
        return 'test_process'

    def run(self, session, instance, params):
        raise Exception('Failure')


def run_process_with_process_manager(name, session, instance_limit=1):
    parameters = ProcessParametersSchema().load({'instance_limit': instance_limit}).data
    result = ProcessManager()._run_process(name, session, parameters)
    return result.to_ordered_dict()


DEFAULT_PROCESS_PARAMETERS = ProcessParametersSchema()


def run_process(session, instance, process):
    # type: (Session, CashRegisterProcess, Process) -> ProcessResult
    res = process.run(session, instance, DEFAULT_PROCESS_PARAMETERS)
    return res


def prepare_process_instance(cr_wrapper, process, stage=None):
    # type: (CashRegister, Process, Optional[Stage]) -> CashRegisterProcess
    return process.create_cash_register_process(cr_wrapper.cash_register, stage.name if stage else None)


def prepare_process_instance_for_unreg(cr_wrapper, process, stage):
    # type: (CashRegister, Process, Stage) -> CashRegisterProcess
    return CashRegisterProcess(
        cash_register_id=cr_wrapper.cash_register_id,
        process=process.name(),
        stage=stage.name,
        attempt=0,
        cash_register=cr_wrapper.cash_register
    )


class FnsregTestResponse:
    def __init__(self, status_code, content=None, headers=None):
        self.status_code = status_code
        self.content = content
        self.headers = headers

    def json(self):
        return json.loads(self.content)


class TestSwitchingAction(SwitchToActionMixin, TestMaintenanceAction):
    name_ = 'TEST_SWITCHING'

    @classmethod
    def name_of_stage_to_switch_to(cls):
        return 'final'


def prepare_info_for_rejection_code(session, cr_wrapper, status, code):
    response = FnsregTestResponse(200, content=json.dumps({'status': status, 'code': code}))
    info = CashRegisterBaseInfo(cr_wrapper.serial_number, session, Mock(), Mock(), Mock(), Mock())
    info.fnsreg_reregistration_status = response
    return info
