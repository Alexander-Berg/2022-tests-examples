import json

from hamcrest import assert_that, is_, has_entry

from yb_darkspirit import scheme


def check_cash_ds_state(response, session, wrapper, state, state_info):
    assert_that(response.status_code, is_(200), response.data)
    assert_that(
        json.loads(response.data),
        has_entry('ds_state', state.value)
    )
    cash = (
        session.query(scheme.CashRegister)
            .filter_by(id=wrapper.cash_register_id)
            .one()
    )  # type: scheme.CashRegister
    assert_that(cash.ds_state, is_(state.value))
    assert_that(cash.ds_state_info, is_(state_info))
