from datetime import datetime, timezone
from decimal import Decimal

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to, has_entries, has_item, has_length

from billing.yandex_pay.yandex_pay.core.actions.payment_notification import HandlePaymentNotificationAction
from billing.yandex_pay.yandex_pay.core.actions.plus_backend.update_order_status import (
    YandexPayPlusUpdateOrderStatusAction
)
from billing.yandex_pay.yandex_pay.core.entities.enums import CardNetwork, PaymentNotificationStatus
from billing.yandex_pay.yandex_pay.core.exceptions import CoreEventAlreadyExistsError
from billing.yandex_pay.yandex_pay.interactions import UnifiedAgentMetricPushClient
from billing.yandex_pay.yandex_pay.interactions.plus_backend.entities import OrderStatus
from billing.yandex_pay.yandex_pay.utils.logging import get_product_logger
from billing.yandex_pay.yandex_pay.utils.stats import payment_status


@pytest.fixture
def key():
    return (
        'MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEwmTG26LY5/0bc7VwMfJ9kfqfLTlYr/Ue9c0G7jdFp08dWX2osZ22JR/S9PUWKblmyl1CSGiHV0'
        'bAtlbeC3QgjQ=='
    )


@pytest.fixture(autouse=True)
def mock_update_order_in_plus_action(mock_action):
    return mock_action(YandexPayPlusUpdateOrderStatusAction)


@pytest.fixture(autouse=True)
def mock_ua_push_client(mocker):
    mock = mocker.AsyncMock(return_value=None)
    return mocker.patch.object(UnifiedAgentMetricPushClient, 'send', mock)


@pytest.fixture(autouse=True)
def disable_async_order_update(yandex_pay_settings):
    yandex_pay_settings.ASYNC_CASHBACK_ORDER_UPDATE_ENABLED = False


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'amount,expected_amount', [(100, Decimal('1.00')), (None, None)]
)
async def test_calls_update_order_action(
    storage,
    key,
    mocker,
    yandex_pay_settings,
    mock_update_order_in_plus_action,
    amount,
    expected_amount,
):
    now = utcnow()
    mocker.patch.object(HandlePaymentNotificationAction, 'should_update_order_in_pay_plus', True)

    await HandlePaymentNotificationAction(
        message_id='msgid',
        event_time=now,
        status=PaymentNotificationStatus.SUCCESS,
        amount=amount,
        currency='XTS',
        payment_id='7',
        recurring=True,
    ).run()

    mock_update_order_in_plus_action.assert_called_once_with(
        message_id='msgid',
        status=OrderStatus.SUCCESS,
        event_time=now,
        amount=expected_amount,
        payment_id='7',
        recurring=True,
    )


@pytest.mark.asyncio
async def test_when_update_disabled__does_not_call_yandex_pay_plus(
    storage, key, mocker, mock_update_order_in_plus_action
):
    now = utcnow()
    mocker.patch.object(HandlePaymentNotificationAction, 'should_update_order_in_pay_plus', False)

    await HandlePaymentNotificationAction(
        message_id='msgid',
        event_time=now,
        status=PaymentNotificationStatus.SUCCESS,
        amount=100,
        currency='XTS',
    ).run()

    mock_update_order_in_plus_action.assert_not_called()


@pytest.mark.asyncio
async def test_status_mapping(storage, key):
    statuses = set(HandlePaymentNotificationAction.status_mapping[status] for status in PaymentNotificationStatus)

    assert_that(statuses, has_length(len(PaymentNotificationStatus)))
    assert all(status in OrderStatus for status in statuses)


@pytest.mark.asyncio
@pytest.mark.parametrize('status', [PaymentNotificationStatus.SUCCESS, PaymentNotificationStatus.FAIL])
async def test_increment_payment_status_metric(app, status):
    before = payment_status.labels(status.value).get()

    await HandlePaymentNotificationAction(
        message_id='msgid',
        event_time=utcnow(),
        status=status,
        amount=100,
        currency='XTS',
    ).run()

    after = payment_status.labels(status.value).get()
    assert_that(after[0][1] - before[0][1], equal_to(1))


@pytest.mark.asyncio
async def test_logging(
    app,
    mocker,
    mocked_logger,
):
    event_time = datetime(2020, 1, 1, 0, 0, 0, tzinfo=timezone.utc)
    product_logger = get_product_logger()
    product_logger.setLevel('INFO')
    product_log_spy = mocker.spy(obj=product_logger, name='handle')

    await HandlePaymentNotificationAction(
        message_id='123',
        event_time=event_time,
        status=PaymentNotificationStatus.SUCCESS,
        amount=100,
        currency='XTS',
        rrn='rrn',
        approval_code='approval_code',
        eci='eci',
        card_network=CardNetwork.MASTERCARD,
        reason='there is no reason',
        reason_code='OH_OH',
        payment_id='7',
        recurring=True,
    ).run()

    log_records = []
    for call in product_log_spy.call_args_list:
        log_record = call.args[0]
        log_records.append({
            'message': log_record.message,
            'context': log_record._context,
        })

    expected_payment_notification_in_ctx = dict(
        message_id='123',
        status=PaymentNotificationStatus.SUCCESS,
        event_time=event_time,
        rrn='rrn',
        approval_code='approval_code',
        eci='eci',
        card_network=CardNetwork.MASTERCARD,
        amount=100,
        currency='XTS',
        reason='there is no reason',
        reason_code='OH_OH',
        payment_id='7',
        recurring=True,
    )

    assert_that(
        log_records,
        has_item(
            has_entries({
                'message': 'Payment notifications request body',
                'context': has_entries({
                    'payment_notification': expected_payment_notification_in_ctx,
                }),
            })
        )
    )


@pytest.mark.asyncio
async def test_suppressed_exception__does_not_raise_error(mocker, mock_action):
    mocker.patch.object(HandlePaymentNotificationAction, 'should_update_order_in_pay_plus', True)
    mock = mock_action(YandexPayPlusUpdateOrderStatusAction, CoreEventAlreadyExistsError)

    await HandlePaymentNotificationAction(
        message_id='msgid',
        event_time=utcnow(),
        status=PaymentNotificationStatus.SUCCESS,
        amount=100,
        currency='XTS',
    ).run()

    mock.assert_called_once()
