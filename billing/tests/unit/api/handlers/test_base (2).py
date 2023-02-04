import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.ping_db import PingDBAction
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import (
    CoreDataError,
    CurrencyNotSupportedError,
    OrderEventAlreadyExistsError,
    OrderEventTooOldError,
    OrderNotFoundError,
    UnableToProcessRefundError,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions.entities import CoreExceptionStatus


@pytest.mark.parametrize('exc, expected_code, expected_message', (
    (CoreDataError, 400, 'DATA_ERROR'),
    (OrderEventAlreadyExistsError, 409, 'ORDER_EVENT_ALREADY_EXISTS'),
    (OrderEventTooOldError, 400, 'ORDER_EVENT_TOO_OLD_ERROR'),
    (OrderNotFoundError, 404, 'ORDER_NOT_FOUND_ERROR'),
))
@pytest.mark.asyncio
async def test_error(app, mock_action, exc, expected_code, expected_message):
    mock_action(PingDBAction, exc)

    r = await app.get('/pingdb')

    assert_that(
        await r.json(),
        equal_to({
            'code': expected_code,
            'status': CoreExceptionStatus.FAIL.value,
            'data': {
                'message': expected_message,
            }
        })
    )


@pytest.mark.parametrize('exc, expected_code, expected_message, expected_params', (
    (
        UnableToProcessRefundError(
            reason_code=UnableToProcessRefundError.ReasonCode.SUCCESS_NOT_FOUND
        ),
        400,
        'REFUND_NOT_PROCESSABLE',
        {'reason_code': 'SUCCESS_NOT_FOUND'},
    ),
    (
        CurrencyNotSupportedError(
            reason_code=CurrencyNotSupportedError.ReasonCode.DEFAULT_CARD_SHEET_LIMIT_NOT_SET
        ),
        400,
        'CURRENCY_NOT_SUPPORTED',
        {'reason_code': 'DEFAULT_CARD_SHEET_LIMIT_NOT_SET'},
    ),
    (
        CurrencyNotSupportedError(
            reason_code=CurrencyNotSupportedError.ReasonCode.DEFAULT_USER_SHEET_LIMIT_NOT_SET
        ),
        400,
        'CURRENCY_NOT_SUPPORTED',
        {'reason_code': 'DEFAULT_USER_SHEET_LIMIT_NOT_SET'},
    ),
))
@pytest.mark.asyncio
async def test_error_with_params(app, mock_action, exc, expected_code, expected_message, expected_params):
    mock_action(PingDBAction, exc)

    r = await app.get('/pingdb')

    assert_that(
        await r.json(),
        equal_to({
            'code': expected_code,
            'status': CoreExceptionStatus.FAIL.value,
            'data': {
                'message': expected_message,
                'params': expected_params,
            }
        })
    )
