import json
from typing import Any

from hamcrest import assert_that, is_, all_of, has_entry

from tests import ws_responses
from yb_darkspirit import scheme


def test_cash_register_patch_hide_cash(cr_wrapper_with_completed_registration, test_client, session, ws_mocks):
    cr_wrapper = cr_wrapper_with_completed_registration
    password = cr_wrapper.admin_password
    ws_mocks.cashmachines_status(
        cr_long_sn=cr_wrapper.whitespirit_key,
        json=ws_responses.CASHMACHINES_STATUS_CR_CLOSE_SHIFT_FS_FISCAL_GOOD
    )

    def trigger(hidden):
        # type: (bool) -> Any
        return test_client.patch(
            '/v1/cash-registers/{0}'.format(cr_wrapper.cash_register_id),
            data=json.dumps({
                'hidden': hidden,
                'admin_password': password
            }),
            content_type="application/json"
        )

    def assert_response(response, expected_hidden):
        assert_that(response.status_code, is_(200), response.data)
        assert_that(
            json.loads(response.data),
            all_of(
                has_entry('hidden', expected_hidden),
                has_entry('admin_password', int(password))
            )
        )

    def assert_db_changes(expected_hidden, expected_password):
        # type: (bool, int) -> None
        cash = session.query(scheme.CashRegister).get(cr_wrapper.cash_register_id)  # type: scheme.CashRegister

        assert_that(cash.hidden, is_(expected_hidden))
        assert_that(cash.admin_password, is_(expected_password))

    resp = trigger(hidden=True)
    assert_response(response=resp, expected_hidden=True)
    assert_db_changes(expected_hidden=True, expected_password=password)

    resp = trigger(hidden=False)
    assert_response(response=resp, expected_hidden=False)
    assert_db_changes(expected_hidden=False, expected_password=password)
