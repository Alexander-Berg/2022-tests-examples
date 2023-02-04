# coding=utf-8
import pytest
from mock import patch
from hamcrest import assert_that, equal_to, calling, raises

from butils.application import getApplication

from yb_darkspirit.core.errors import DataError
from yb_darkspirit.core.cash_register import NotReadyException, DsStateMismatchException
from yb_darkspirit.core.cash_register.state_checks import ChecksManager, Checks
from yb_darkspirit.core.cash_register.state_manager import DS_STATE_CHECKS, DsStateManager
from yb_darkspirit.scheme import DsCashState


@pytest.mark.parametrize('target_state', [DsCashState.NEW, DsCashState.BROKEN])
@patch.object(ChecksManager, 'filter_failed_checks')
@patch.dict(DS_STATE_CHECKS, {
    DsCashState.NEW: {Checks.NONCONFIGURED, Checks.HAS_ADMIN_PASSWORD},
    DsCashState.BROKEN: {Checks.POSTFISCAL},
}, clear=True)
def test_failed_checks_for_ds_state_uses_correct_list_of_checks(filter_failed_checks_mock, cr_wrapper, target_state):
    """
    По ds_state-у должен прогоняться правильный список проверок.

    :param target_state: ds_state, для которого проверяем готовность кассы.
    """
    cash_register = cr_wrapper.cash_register
    checker = DsStateManager(ChecksManager.from_app(getApplication()))

    checker.failed_checks_for_ds_state(cash_register, target_state)

    filter_failed_checks_mock.assert_called_once_with(cash_register.serial_number, DS_STATE_CHECKS[target_state])


@patch.object(ChecksManager, 'filter_failed_checks')
@patch.dict(DS_STATE_CHECKS, {DsCashState.OK: {}})
def test_failed_checks_for_ds_state_correct_response(filter_failed_checks_mock, cr_wrapper):
    """
    В качестве результата failed_checks_for_ds_state должен отдавать тот же список проверок,
    что получил от ChecksManager.
    """
    cash_register = cr_wrapper.cash_register
    checker = DsStateManager(ChecksManager.from_app(getApplication()))

    assert_that(
        checker.failed_checks_for_ds_state(cash_register, DsCashState(cash_register.ds_state)),
        equal_to(filter_failed_checks_mock.return_value)
    )


@patch.object(ChecksManager, 'filter_failed_checks')
@patch.dict(DS_STATE_CHECKS, {}, clear=True)
def test_failed_checks_for_ds_state_failed(filter_failed_checks_mock, cr_wrapper):
    """
    Тест на исключение при прогоне проверок для неожиданного ds_state.
    """
    not_expected_state = DsCashState.READY_TO_REGISTRATION

    cash_register = cr_wrapper.cash_register

    checker = DsStateManager(ChecksManager.from_app(getApplication()))
    assert_that(
        calling(checker.failed_checks_for_ds_state).with_args(cash_register, not_expected_state),
        raises(NotImplementedError),
    )
    filter_failed_checks_mock.assert_not_called()


@pytest.mark.parametrize('expected_ds_state', [None, DsCashState.OK])
@patch.object(DsStateManager, 'failed_checks_for_ds_state')
def test_assert_current_state_succeed(failed_checks_for_ds_state_mock, cr_wrapper, expected_ds_state):
    """
    Тест на метод проверки что касса действительно находится в нужном ds_state (логически и физически).
    Здесь положительные тест-кейсы.

    :param expected_ds_state: ds_state, для которого проверяем готовность кассы.
    """
    cr_wrapper.cash_register.ds_state = DsCashState.OK
    failed_checks_for_ds_state_mock.return_value = []

    checker = DsStateManager(ChecksManager.from_app(getApplication()))
    checker.assert_current_state(cr_wrapper.cash_register, expected_ds_state=expected_ds_state)

    failed_checks_for_ds_state_mock.assert_called_once_with(cr_wrapper.cash_register, DsCashState.OK)


@pytest.mark.parametrize('cr_wrapper_ds_state,target_ds_state,failed_checks,expected_exception', [
    (DsCashState.OK, DsCashState.OK, [Checks.NONCONFIGURED], NotReadyException),
    (DsCashState.OK, DsCashState.BROKEN, [], DsStateMismatchException),
    (DsCashState.OK, DsCashState.BROKEN, [Checks.NONCONFIGURED], DataError),
])
@patch.object(DsStateManager, 'failed_checks_for_ds_state')
def test_assert_current_state_failed(failed_checks_for_ds_state_mock, cr_wrapper,
                                     cr_wrapper_ds_state, target_ds_state, failed_checks, expected_exception):
    """
    Тест на метод проверки что касса действительно находится в нужном ds_state (логически и физически).
    Здесь отрицательные тест-кейсы.

    :param cr_wrapper_ds_state: ds_state, кассы,
    :param target_ds_state: ds_state, для которого хотим проверить,
    :param failed_checks: проверки, которые по тест-кейсу хотим зафелйить.
    """
    cr_wrapper.cash_register.ds_state = cr_wrapper_ds_state
    failed_checks_for_ds_state_mock.return_value = failed_checks

    checker = DsStateManager(ChecksManager.from_app(getApplication()))
    assert_that(
        calling(checker.assert_current_state).with_args(cr_wrapper.cash_register, expected_ds_state=target_ds_state),
        raises(expected_exception)
    )
