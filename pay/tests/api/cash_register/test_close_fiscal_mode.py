import json

import pytest
from hamcrest import assert_that, is_, not_none

from tests import ws_responses
from yb_darkspirit import scheme


@pytest.mark.parametrize(
    ('status_response', 'should_call_close_shift', 'should_call_close_fiscal_mode'), (
        (ws_responses.CASHMACHINES_STATUS_CR_OPEN_SHIFT_FS_FISCAL_GOOD, True, True),
        (ws_responses.CASHMACHINES_STATUS_CR_CLOSE_SHIFT_FS_FISCAL_GOOD, False, True),
        # Not yet implemented
        # (ws_responses.CASHMACHINES_STATUS_CR_POSTFISCAL_FS_ARCHIVE_READING, False, False),
    )
)
def test_cash_register_close_fiscal_mode_success(
    cr_wrapper_ok, test_client, session, ws_mocks,
    status_response, should_call_close_shift, should_call_close_fiscal_mode,
):
    cr_wrapper = cr_wrapper_ok
    ws_mocks.cashmachines_status(
        cr_long_sn=cr_wrapper.whitespirit_key,
        json=status_response,
        reset=True,
    )

    close_shift_mock = ws_mocks.cashmachines_close_shift(cr_long_sn=cr_wrapper.whitespirit_key, json=ws_responses.SHIFT_CLOSE_REPORT)
    close_fmode_mock = ws_mocks.cashmachines_close_fiscal_mode(cr_long_sn=cr_wrapper.whitespirit_key, json=ws_responses.CLOSE_FN_REPORT)

    response = test_client.post(
        '/v1/cash-registers/{0}/close_fiscal_mode'.format(cr_wrapper.cash_register_id),
        data=json.dumps({'reason': 'test'}),
        content_type="application/json",
    )
    assert_that(response.status_code, is_(200), response.data)

    assert_that(close_shift_mock.call_count, is_(1 if should_call_close_shift else 0))
    assert_that(close_fmode_mock.call_count, is_(1 if should_call_close_fiscal_mode else 0))

    cash = session.query(scheme.CashRegister).get(cr_wrapper.cash_register_id)  # type: scheme.CashRegister
    assert_that(cash.current_process, is_(not_none()))


def test_cash_register_close_fiscal_mode_fail_on_nonconfigured(cr_wrapper_nonconfigured_fs_ready_fiscal, test_client, session, ws_mocks):
    cr_wrapper = cr_wrapper_nonconfigured_fs_ready_fiscal

    response = test_client.post(
        '/v1/cash-registers/{0}/close_fiscal_mode'.format(cr_wrapper.cash_register_id),
        data=json.dumps({'reason': 'test'}),
        content_type="application/json",
    )
    assert_that(response.status_code, is_(400), response.data)
