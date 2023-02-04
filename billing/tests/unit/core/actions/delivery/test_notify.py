from dataclasses import replace
from datetime import datetime

import pytest
from pay.lib.entities.shipping import DeliveryStatus
from pay.lib.interactions.merchant.entities import EventType, MerchantWebhookV1Request, OrderWebhookData

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.delivery.notify import NotifyMerchantDeliveryAction
from billing.yandex_pay_plus.yandex_pay_plus.interactions.merchant import MerchantZoraClient


class TestNotifyAction:
    @pytest.mark.asyncio
    async def test_calls_client(self, params, mock_notify_merchant, merchant):
        await NotifyMerchantDeliveryAction(**params).run()

        mock_notify_merchant.assert_awaited_once_with(
            base_url='https://callback.test',
            request=MerchantWebhookV1Request(
                merchant_id=merchant.merchant_id,
                event=EventType.ORDER_STATUS_UPDATED,
                order=OrderWebhookData(
                    order_id='order-id',
                    delivery_status=DeliveryStatus.NEW,
                ),
                event_time=datetime.fromisoformat('2020-12-30T00:00:00+00:00'),
            ),
        )

    @pytest.fixture
    def params(self, merchant):
        return {
            'merchant_id': merchant.merchant_id,
            'event_time': datetime.fromisoformat('2020-12-30T00:00:00+00:00'),
            'status': DeliveryStatus.NEW,
            'order_id': 'order-id',
        }


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
