from datetime import datetime, timedelta

import pytest
from hamcrest import equal_to, assert_that, not_

from yb_darkspirit import scheme
from yb_darkspirit.application.plugins.dbhelper import Session
from yb_darkspirit.interface import CashRegister
from yb_darkspirit.process.auto_offline import AutoOfflineProcess
from yb_darkspirit.process.schemas import AutoOfflineConfig
from yb_darkspirit.task.handle_offline_task import HandleOfflineCashesTask


def test_handle_auto_offline_initializes_process(session, cr_wrapper):
    process = _run_handle_offline_task_with_parameters(
        session, cr_wrapper, scheme.CASH_REGISTER_STATE_OFFLINE, timedelta(days=1, seconds=1)
    )

    assert_that(process, not_(equal_to(None)))
    assert_that(process.data.get('config'), equal_to({u'cc': [u"gogam"]}))


@pytest.mark.parametrize('state', [scheme.CASH_REGISTER_STATE_WS_OFFLINE, scheme.CASH_REGISTER_STATE_OPEN_SHIFT])
def test_handle_auto_offline_doesnt_take_not_offline_cash(session, cr_wrapper, state):
    process = _run_handle_offline_task_with_parameters(
        session, cr_wrapper, state, timedelta(days=1, seconds=1)
    )

    assert_that(process, equal_to(None))


@pytest.mark.parametrize('ds_state', [scheme.DsCashState.IN_PROCESS.value, scheme.DsCashState.IN_REREGISTRATION.value])
def test_handle_auto_offline_doesnt_take_not_ok_cash(session, cr_wrapper, ds_state):
    cr_wrapper.cash_register.ds_state = ds_state
    process = _run_handle_offline_task_with_parameters(
        session, cr_wrapper, scheme.CASH_REGISTER_STATE_OFFLINE, timedelta(days=1, seconds=1)
    )

    assert_that(process, equal_to(None))


def test_handle_auto_offline_doesnt_include_cashes_less_than_day_offline(session, cr_wrapper):
    process = _run_handle_offline_task_with_parameters(
        session, cr_wrapper, scheme.CASH_REGISTER_STATE_OFFLINE, timedelta(hours=23, minutes=59, seconds=59)
    )

    assert_that(process, equal_to(None))


def _run_handle_offline_task_with_parameters(session, cr_wrapper, state, time_shift, limit=1):
    # type: (Session, CashRegister, str, timedelta, int) -> scheme.CashRegisterProcess
    task = HandleOfflineCashesTask()
    data = {'limit': limit, 'config': AutoOfflineConfig(["gogam"])}
    cr_wrapper.cash_register.state = state
    cr_wrapper.cash_register.state_dt = datetime.now() - time_shift

    with session.begin():
        task.run(session, data)

    return (
        session.query(scheme.CashRegisterProcess)
        .filter_by(process=AutoOfflineProcess.name())
        .one_or_none()
    )
