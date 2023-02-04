from datetime import datetime

import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.merchant.get import GetMerchantAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.merchant_order.list import ListMerchantOrdersAction
from billing.yandex_pay_admin.yandex_pay_admin.interactions import YandexPayPlusBackendClient


@pytest.fixture
async def orders(order_entity):
    return [order_entity]


@pytest.mark.asyncio
async def test_list_orders(merchant, user, mocker, mock_action, orders):
    role_mock = mock_action(GetMerchantAction)
    client_mock = mocker.patch.object(YandexPayPlusBackendClient, 'get_orders', mocker.AsyncMock(return_value=orders))

    returned = await ListMerchantOrdersAction(
        user=user,
        merchant_id=merchant.merchant_id,
        limit=10,
        created_lt=datetime(2022, 2, 3),
        created_gte=datetime(2022, 2, 2),
    ).run()

    role_mock.assert_run_once_with(user=user, merchant_id=merchant.merchant_id)
    client_mock.assert_called_once_with(
        merchant_id=merchant.merchant_id,
        limit=10,
        created_lt=datetime(2022, 2, 3),
        created_gte=datetime(2022, 2, 2),
    )
    assert_that(returned, equal_to(orders))
