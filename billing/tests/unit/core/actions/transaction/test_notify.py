import re
from dataclasses import replace
from datetime import datetime
from uuid import UUID

import pytest
from pay.lib.entities.enums import OperationStatus
from pay.lib.entities.order import PaymentStatus
from pay.lib.interactions.merchant.entities import (
    EventType,
    MerchantWebhookV1Request,
    OperationWebhookData,
    OrderWebhookData,
)

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.transaction.notify import (
    NotifyFirstTransactionAction,
    NotifyMerchantTransactionAction,
    NotifyOperationFailedAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.interactions.merchant import MerchantZoraClient
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import TransactionStatus


class TestNotifyAction:
    @pytest.mark.asyncio
    async def test_calls_client(self, params, mock_notify_merchant, merchant):
        await NotifyMerchantTransactionAction(**params).run()

        mock_notify_merchant.assert_awaited_once_with(
            base_url='https://callback.test',
            request=MerchantWebhookV1Request(
                merchant_id=merchant.merchant_id,
                event=EventType.ORDER_STATUS_UPDATED,
                order=OrderWebhookData(
                    order_id='order-id',
                    payment_status=PaymentStatus.AUTHORIZED,
                ),
                event_time=datetime.fromisoformat('2020-12-30T00:00:00+00:00'),
            ),
        )

    @pytest.fixture
    def params(self, merchant):
        return {
            'merchant_id': merchant.merchant_id,
            'event_time': datetime.fromisoformat('2020-12-30T00:00:00+00:00'),
            'transaction_id': UUID('0b865b6e-b546-495b-abe5-f831689a0f47'),
            'status': TransactionStatus.AUTHORIZED,
            'order_id': 'order-id',
        }


class TestNotifyOperationFailedAction:
    @pytest.mark.asyncio
    async def test_calls_client(self, params, mock_notify_merchant, merchant, stored_operation):
        await NotifyOperationFailedAction(**params).run()

        mock_notify_merchant.assert_awaited_once_with(
            base_url='https://callback.test',
            request=MerchantWebhookV1Request(
                merchant_id=merchant.merchant_id,
                event=EventType.OPERATION_STATUS_UPDATED,
                operation=OperationWebhookData(
                    operation_id=params['operation_id'],
                    order_id=stored_operation.order_id,
                    status=OperationStatus.FAIL,
                    operation_type=stored_operation.operation_type,
                    external_operation_id=stored_operation.external_operation_id,
                ),
                event_time=datetime.fromisoformat('2020-12-30T00:00:00+00:00'),
            ),
        )

    @pytest.fixture
    def params(self, merchant, stored_operation):
        return {
            'event_time': datetime.fromisoformat('2020-12-30T00:00:00+00:00'),
            'operation_id': stored_operation.operation_id,
        }


@pytest.mark.asyncio
async def test_notify_first_transaction_success(
    storage,
    stored_merchant,
    stored_transaction,
    aioresponses_mocker,
    yandex_pay_plus_settings,
):
    query_mock = aioresponses_mocker.post(
        re.compile(f'^{yandex_pay_plus_settings.YANDEX_PAY_ADMIN_API_URL}.*$')
    )
    transaction_id = stored_transaction.transaction_id
    merchant_id = stored_merchant.merchant_id
    event_time = datetime.now()

    await NotifyFirstTransactionAction(
        transaction_id=transaction_id,
        merchant_id=merchant_id,
        partner_id=stored_merchant.partner_id,
        event_time=event_time,
    ).run()

    updated_merchant = await storage.merchant.get(stored_merchant.merchant_id)

    assert updated_merchant.first_transaction == stored_transaction.transaction_id
    assert query_mock.call_args.kwargs['json'] == {
        'event_time': event_time.isoformat(),
        'data': {
            'event_type': 'FIRST_TRANSACTION',
            'merchant_id': str(merchant_id),
            'partner_id': str(stored_merchant.partner_id),
        }
    }


@pytest.mark.asyncio
async def test_notify_merchant_with_first_transaction(
    stored_merchant,
    stored_transaction,
    mock_action,
    mock_notify_merchant,
):
    admin_mock = mock_action(NotifyFirstTransactionAction)
    transaction_id = stored_transaction.transaction_id
    merchant_id = stored_merchant.merchant_id
    order_id = stored_transaction.checkout_order_id
    event_time = datetime.now()

    await NotifyMerchantTransactionAction(
        transaction_id=transaction_id,
        merchant_id=merchant_id,
        order_id=order_id,
        status=TransactionStatus.CHARGED,
        event_time=event_time,
    ).run()

    admin_mock.assert_called_once_with(
        transaction_id=transaction_id,
        merchant_id=merchant_id,
        partner_id=stored_merchant.partner_id,
        event_time=event_time,
    )
    mock_notify_merchant.assert_called_once()


@pytest.fixture
def mock_notify_merchant(mocker):
    return mocker.patch.object(MerchantZoraClient, 'notify', mocker.AsyncMock())


@pytest.fixture
async def merchant(storage, stored_merchant):
    return await storage.merchant.save(
        replace(
            stored_merchant,
            callback_url='https://callback.test',
        )
    )
