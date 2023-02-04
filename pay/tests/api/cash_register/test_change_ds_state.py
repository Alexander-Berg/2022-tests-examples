import pytest
import json

from hamcrest import assert_that, equal_to, has_entries
from mock import patch, Mock

from tests import ws_responses
from yb_darkspirit.core.cash_register.state_manager import DsStateManager, Checks


def _test_change_state_case(cr_wrapper, test_client, ws_mocks, skip_checks, state, info, code, response_body):
    ws_mocks.cashmachines_status(
        cr_long_sn=cr_wrapper.whitespirit_key,
        json=ws_responses.CASHMACHINES_STATUS_CR_CLOSE_SHIFT_FS_FISCAL_GOOD
    )
    resp = test_client.post(
        "/v1/cash-registers/{cr_id}/change_state".format(cr_id=cr_wrapper.cash_register_id),
        json={"target_state": "UNREGISTERED", "reason": "info", "skip_checks": skip_checks}
    )
    assert_that(resp.status_code, equal_to(code))
    assert_that(json.loads(resp.data), has_entries(response_body))
    assert_that(cr_wrapper.cash_register.ds_state, equal_to(state))
    assert_that(cr_wrapper.cash_register.ds_state_info, equal_to(info))


@pytest.mark.parametrize('failed_checks,skip_checks,response_body', [
    ([], False, {'ds_state': 'UNREGISTERED'}),
    ([Checks.NONCONFIGURED], True, {
        'checks_exception':
            'NotReadyException(\'The CashRegister is not ready for UNREGISTERED. Blocking checks: [Checks.NONCONFIGURED]\',)',
        'ds_state': 'UNREGISTERED'
    }),
])
@patch.object(DsStateManager, 'failed_checks_for_ds_state')
def test_change_state_succeed(failed_checks_mock, test_client, failed_checks, response_body,
                              skip_checks, cr_wrapper_with_completed_registration, ws_mocks):
    failed_checks_mock.return_value = failed_checks
    _test_change_state_case(
        cr_wrapper_with_completed_registration, test_client, ws_mocks, skip_checks,
        state='UNREGISTERED', info='info',
        code=200, response_body=response_body
    )


@patch.object(DsStateManager, 'failed_checks_for_ds_state', Mock(return_value=[Checks.NONCONFIGURED]))
def test_change_state_blocked_by_checks_is_error(test_client, cr_wrapper_with_completed_registration, ws_mocks):
    _test_change_state_case(
        cr_wrapper_with_completed_registration, test_client, ws_mocks,
        skip_checks=False,
        state='OK', info=None,
        code=400, response_body={
            'status': 'error',
            'message': 'The CashRegister is not ready for UNREGISTERED. Blocking checks: [Checks.NONCONFIGURED]'
        }
    )


@patch.object(DsStateManager, 'failed_checks_for_ds_state', Mock(side_effect=Exception('Exception for test')))
def test_change_state_and_exception_happens_is_error(test_client, cr_wrapper_with_completed_registration, ws_mocks):
    _test_change_state_case(
        cr_wrapper_with_completed_registration, test_client, ws_mocks, skip_checks=False,
        state='OK', info=None,
        code=500, response_body={
            'message': 'Error: Exception for test'
        }
    )


@patch.object(DsStateManager, 'failed_checks_for_ds_state', Mock(side_effect=Exception('Exception for test')))
def test_change_state_force_and_exception_happens_is_ok(test_client, cr_wrapper_with_completed_registration, ws_mocks):
    _test_change_state_case(
        cr_wrapper_with_completed_registration, test_client, ws_mocks, skip_checks=True,
        state='UNREGISTERED', info='info',
        code=200, response_body={
            'ds_state': 'UNREGISTERED',
            'checks_exception': "Exception('Exception for test',)"
        }
    )
