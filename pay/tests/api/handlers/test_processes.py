import json

from tests.processes_testing_instances import TestProcess
from yb_darkspirit.api.handlers.processes import UpdateConfigResource
from yb_darkspirit.interface import CashRegister
from yb_darkspirit import locks
from yb_darkspirit import scheme
from mock import patch, Mock
from typing import Dict
from hamcrest import assert_that, equal_to, none, not_none
import pytest

from yb_darkspirit.process.process_manager import PROCESSES

CLOSE_SHIFT = 'close_shift'
CLOSE_FISCAL_STORAGE = 'close_fiscal_mode'
CLEAR_DEVICE = 'clear_device_data'
SYNC_DOCUMENTS = 'pull_missing_documents'
NOT_EXISTING_METHOD = 'not_existing_method'


@pytest.mark.parametrize('action_name,called_mock_name', [
    ('CLOSE_SHIFT', CLOSE_SHIFT),
    ('CLOSE_FISCAL_STORAGE', CLOSE_FISCAL_STORAGE),
    ('CLEAR_DEVICE', CLEAR_DEVICE),
    ('SYNC_DOCUMENTS', SYNC_DOCUMENTS),
])
def test_apply_maintenance_action_succeed(wrapper_mocks, test_client, session, action_name, called_mock_name):
    cash_register = _prepare_cash_register_process(session)

    resp = test_client.post(
        "/v1/process/{sn}/apply_maintenance_action/{action_name}".format(sn=cash_register.id, action_name=action_name),
        json={'apply_checks': False}
    )

    assert_that(resp.status_code, equal_to(200))
    _check_mocks(wrapper_mocks, called_mock_name)


def test_add_cash_register_process_again(session, test_client):
    cash_register = _prepare_cash_register_process(session)

    resp = test_client.post(
        "/v1/process/launch/{cr_id}/prepare_cash_to_registration".format(cr_id=cash_register.id)
    )
    assert_that(resp.status_code, equal_to(409))


def test_reset_attempts_for_cash(session, test_client):
    cash_register = _prepare_cash_register_process(session)
    cash_register.current_process.attempt = 3
    session.flush()

    resp = test_client.post(
        "/v1/process/reset_attempts/{cr_id}".format(cr_id=cash_register.id)
    )
    assert_that(resp.status_code, equal_to(200))
    assert_that(cash_register.current_process.attempt, equal_to(0))


def test_reset_attempts_process_for_four_cashes_with_target_process(session, test_client):
    process = "process"
    cash_register_list = [_prepare_cash_register_process(session, "1234{}".format(i), process) for i in range(4)]
    for cash_register in cash_register_list:
        cash_register.current_process.attempt = 3
    session.flush()
    resp = test_client.post(
        "/v1/process/reset_attempts/{pr_name}".format(pr_name=process)
    )
    assert_that(resp.status_code, equal_to(200))
    for cash_register in cash_register_list:
        assert_that(cash_register.current_process.attempt, equal_to(0))


def test_reset_attempts_process_for_one_cash_with_other_process(session, test_client):
    process = "process"
    other_cash_register = _prepare_cash_register_process(session, "12349", "other")
    cash_register = _prepare_cash_register_process(session, "12345", process)
    cash_register.current_process.attempt = 3
    other_cash_register.current_process.attempt = 2
    session.flush()
    resp = test_client.post(
        "/v1/process/reset_attempts/{pr_name}".format(pr_name=process)
    )
    assert_that(resp.status_code, equal_to(200))
    assert_that(other_cash_register.current_process.attempt, equal_to(2))


def test_reset_attempts_process_with_no_processes(session, test_client):
    process = "process"
    session.flush()
    resp = test_client.post(
        "/v1/process/reset_attempts/{pr_name}".format(pr_name=process)
    )
    assert_that(resp.status_code, equal_to(200))


def test_remove_cash_register_from_process(session, test_client):
    cash_register = _prepare_cash_register_process(session)
    assert_that(cash_register.current_process, not_none())
    resp = test_client.delete(
        "/v1/process/launch/{cr_id}/process".format(cr_id=cash_register.id)
    )
    assert_that(resp.status_code, equal_to(200))
    assert_that(cash_register.current_process, none())

    resp = test_client.delete(
        "/v1/process/launch/{cr_id}/process".format(cr_id=cash_register.id)
    )
    assert_that(resp.status_code, equal_to(404))


@patch.object(locks, 'exec_if_not_locked', Mock(side_effect=locks.LockedException('')))
def test_apply_maintenance_action_failed_when_locked(wrapper_mocks, test_client, session):
    cash_register = _prepare_cash_register_process(session)

    resp = test_client.post(
        "/v1/process/{sn}/apply_maintenance_action/{action_name}".format(sn=cash_register.id, action_name='CLOSE_SHIFT'),
        json={'apply_checks': False}
    )

    assert_that(resp.status_code, equal_to(503))
    _check_mocks(wrapper_mocks, NOT_EXISTING_METHOD)


def test_update_config_updates_config(session, cr_wrapper_with_test_process):
    cr_wrapper_with_test_process.current_process.data['config'] = {'one': 'three', 'three': 'four'}

    with session.begin():
        processes_count = UpdateConfigResource._update_config(session, TestProcess.name(), {'one': 'two'})

    assert_process_config(cr_wrapper_with_test_process, {'one': 'two', 'three': 'four'})
    assert_that(processes_count, equal_to(1))


def test_update_config_updates_empty_config(session, cr_wrapper_with_test_process):
    with session.begin():
        processes_count = UpdateConfigResource._update_config(session, TestProcess.name(), {'one': 'two'})

    assert_process_config(cr_wrapper_with_test_process, {'one': 'two'})
    assert_that(processes_count, equal_to(1))


def test_update_doesnt_update_config_for_process_with_different_name(session, cr_wrapper_with_test_process):
    with session.begin():
        processes_count = UpdateConfigResource._update_config(session, 'blablabla', {'one': 'two'})

    assert_process_config(cr_wrapper_with_test_process, {})
    assert_that(processes_count, equal_to(0))


def assert_process_config(cr_wrapper_with_test_process, config):
    assert_that(cr_wrapper_with_test_process.current_process.data['config'], equal_to(config))


@patch.object(UpdateConfigResource, '_update_config', Mock(return_value=1))
@patch.dict(PROCESSES, {TestProcess.name(): TestProcess()})
def test_update_config_call_from_api_call(session, cr_wrapper_with_test_process, test_client):
    resp = test_client.post(
        "/v1/process/update_config/{process_name}".format(process_name=TestProcess.name()),
        json={'config': {'three': 'four'}}
    )

    assert_that(resp.status_code, equal_to(200))
    assert_that(json.loads(resp.data)['processes_count'], equal_to(1))
    UpdateConfigResource._update_config.assert_called_with(session, TestProcess.name(), {'three': 'four'})


@patch.object(UpdateConfigResource, '_update_config', Mock(return_value=1))
@patch.dict(PROCESSES, {})
def test_update_config_call_fails_from_api_call_when_process_doesnt_exist(session, cr_wrapper_with_test_process, test_client):
    resp = test_client.post(
        "/v1/process/update_config/{process_name}".format(process_name=TestProcess.name()),
        json={'config': {}}
    )

    assert_that(resp.status_code, equal_to(404))


def _check_mocks(wrapper_mocks, should_be_called):
    # type: (Dict[str, Mock], str) -> None
    for name, mock in wrapper_mocks.items():
        if name == should_be_called:
            mock.assert_called_once()
        else:
            mock.assert_not_called()


def _prepare_cash_register_process(session, serial_number='12345', process_name='process'):
    cash_register = scheme.CashRegister(serial_number=serial_number)
    session.add(cash_register)
    session.flush()
    cr_process = scheme.CashRegisterProcess(process=process_name, stage='stage', cash_register_id=cash_register.id)
    session.add(cr_process)
    session.flush()
    return cash_register


@pytest.fixture
def wrapper_mocks():
    with patch.object(CashRegister, CLOSE_SHIFT) as close_shift_mock,\
            patch.object(CashRegister, CLOSE_FISCAL_STORAGE) as close_fs_mock,\
            patch.object(CashRegister, SYNC_DOCUMENTS) as sync_mock,\
            patch.object(CashRegister, CLEAR_DEVICE) as clear_data_mock:
        yield {
            CLOSE_SHIFT: close_shift_mock,
            CLOSE_FISCAL_STORAGE: close_fs_mock,
            CLEAR_DEVICE: clear_data_mock,
            SYNC_DOCUMENTS: sync_mock
        }
