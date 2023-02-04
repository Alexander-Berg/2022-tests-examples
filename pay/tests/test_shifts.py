# -*- coding: utf-8 -*-
"""
При выполнении на реальном WS тесты должны выполняться в
определённом порядке из-за того, что вызовы меняют состояние кассы.
"""
from yb_darkspirit import scheme

from tests import ws_responses


def test_close_overdue_open_shift(cr_wrapper, ws_mocks):
    ws_mocks.cashmachines_status(
        cr_long_sn=cr_wrapper.whitespirit_key,
        json=ws_responses.CASHMACHINES_STATUS_CR_OVERDUE_OPEN_SHIFT_FS_FISCAL_GOOD
    )
    ws_mocks.cashmachines_close_shift(
        cr_long_sn=cr_wrapper.whitespirit_key,
        json=ws_responses.SHIFT_CLOSE_REPORT,
    )
    ws_mocks.cashmachines_status(
        cr_long_sn=cr_wrapper.whitespirit_key,
        json=ws_responses.CASHMACHINES_STATUS_CR_CLOSE_SHIFT_FS_FISCAL_GOOD
    )

    cr_wrapper.close_shift(cr_wrapper.close_shift_reason())

    assert cr_wrapper.cash_register_state == scheme.CASH_REGISTER_STATE_CLOSE_SHIFT
    # проверить, что сохранили документ в базу
    # cr_wrapper.sync_state()
