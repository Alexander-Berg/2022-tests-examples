import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.merchant.get import GetMerchantAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.merchant_order.get import GetMerchantOrderAction
from billing.yandex_pay_admin.yandex_pay_admin.interactions import YandexPayPlusBackendClient


@pytest.mark.asyncio
async def test_get_order(merchant, user, mocker, mock_action, order_entity):
    role_mock = mock_action(GetMerchantAction)
    client_mock = mocker.patch.object(
        YandexPayPlusBackendClient, 'get_order', mocker.AsyncMock(return_value=order_entity)
    )

    returned = await GetMerchantOrderAction(
        user=user,
        merchant_id=merchant.merchant_id,
        order_id=order_entity.order_id,
    ).run()

    role_mock.assert_run_once_with(user=user, merchant_id=merchant.merchant_id)
    client_mock.assert_called_once_with(
        merchant_id=merchant.merchant_id,
        order_id=order_entity.order_id,
    )
    assert_that(returned, equal_to(order_entity))
