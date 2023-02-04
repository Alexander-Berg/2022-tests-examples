from uuid import UUID

import pytest
from freezegun import freeze_time
from pay.lib.interactions.merchant.entities import TransactionStatus
from pay.lib.interactions.split.entities import YandexSplitOrderStatus

from sendr_utils import utcnow

from billing.yandex_pay.yandex_pay.core.actions.merchant.update_order import UpdateMerchantOrderAction
from billing.yandex_pay.yandex_pay.core.actions.plus_backend.update_order_status import (
    OrderStatus, YandexPayPlusUpdateOrderStatusAction
)
from billing.yandex_pay.yandex_pay.core.actions.split.process_callback import ProcessSplitCallbackAction
from billing.yandex_pay.yandex_pay.core.entities.enums import SplitCallbackEventType


@pytest.fixture
def mock_merchant_order_update(mock_action):
    return mock_action(UpdateMerchantOrderAction)


@pytest.fixture
def mock_plus_order_update(mock_action):
    return mock_action(YandexPayPlusUpdateOrderStatusAction)


@freeze_time('2021-12-31')
@pytest.mark.parametrize(
    'split_status, merchant_status, plus_order_status',
    [
        (YandexSplitOrderStatus.NEW, None, None),
        (YandexSplitOrderStatus.PROCESSING, None, None),
        (YandexSplitOrderStatus.APPROVED, TransactionStatus.HOLD, OrderStatus.HOLD),
        (YandexSplitOrderStatus.COMMITED, TransactionStatus.SUCCESS, None),
        (YandexSplitOrderStatus.REFUNDED, TransactionStatus.REVERSE, OrderStatus.REVERSE),
        (
            YandexSplitOrderStatus.PARTIALLY_REFUNDED,
            TransactionStatus.PARTIAL_REFUND,
            OrderStatus.HOLD,
        ),
        (YandexSplitOrderStatus.FAILED, TransactionStatus.FAIL, None),
    ],
)
@pytest.mark.asyncio
async def test_update_merchant_order(
    mock_merchant_order_update,
    mock_plus_order_update,
    yandex_pay_settings,
    split_status,
    merchant_status,
    plus_order_status,
):
    created = utcnow()

    await ProcessSplitCallbackAction(
        order_id='123',
        external_id='fake_external_id',
        status=split_status,
        split_merchant_id='fake_split_merchant_id',
        event_type=SplitCallbackEventType.ORDER_UPDATE,
    ).run()

    if merchant_status is None:
        mock_merchant_order_update.assert_not_called()
    else:
        mock_merchant_order_update.assert_called_once_with(
            merchant_id=UUID(yandex_pay_settings.SPLIT_MERCHANT_ID),
            external_id='fake_external_id',
            status=merchant_status,
            event_time=created,
        )

    if plus_order_status is None:
        mock_plus_order_update.assert_not_called()
    else:
        mock_plus_order_update.assert_called_once_with(
            message_id=f'2:{yandex_pay_settings.SPLIT_MERCHANT_ID}_fake_external_id',
            status=plus_order_status,
            event_time=created,
        )


@pytest.mark.asyncio
@freeze_time('2021-12-31')
@pytest.mark.parametrize(
    'split_status, plus_order_status',
    [
        (YandexSplitOrderStatus.NEW, None),
        (YandexSplitOrderStatus.PROCESSING, None),
        (YandexSplitOrderStatus.APPROVED, OrderStatus.SUCCESS),
        (YandexSplitOrderStatus.COMMITED, OrderStatus.SUCCESS),
        (YandexSplitOrderStatus.REFUNDED, None),
        (YandexSplitOrderStatus.PARTIALLY_REFUNDED, OrderStatus.SUCCESS),
        (YandexSplitOrderStatus.FAILED, None),
    ],
)
async def test_update_merchant_order_bnpl_finished(
    mock_merchant_order_update,
    mock_plus_order_update,
    yandex_pay_settings,
    split_status,
    plus_order_status,
):
    await ProcessSplitCallbackAction(
        order_id='123',
        external_id='fake_external_id',
        status=split_status,
        split_merchant_id='fake_split_merchant_id',
        event_type=SplitCallbackEventType.BNPL_FINISHED,
    ).run()

    mock_merchant_order_update.assert_not_called()

    if plus_order_status is None:
        mock_plus_order_update.assert_not_called()
    else:
        mock_plus_order_update.assert_called_once_with(
            message_id=f'2:{yandex_pay_settings.SPLIT_MERCHANT_ID}_fake_external_id',
            status=plus_order_status,
            event_time=utcnow(),
        )
