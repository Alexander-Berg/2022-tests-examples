from uuid import uuid4

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.merchant.get import GetMerchantAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.merchant_key.list import ListMerchantKeysAction
from billing.yandex_pay_admin.yandex_pay_admin.interactions import YandexPayPlusBackendClient
from billing.yandex_pay_admin.yandex_pay_admin.interactions.pay_backend import MerchantKey
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.merchant import Merchant


@pytest.fixture
async def merchant(storage, partner, role):
    return await storage.merchant.create(
        Merchant(
            name='merchant name',
            partner_id=partner.partner_id,
        )
    )


@pytest.fixture
async def keys():
    return [MerchantKey(key_id=uuid4(), created=utcnow())]


@pytest.fixture
def client_mock(mocker, keys):
    return mocker.patch.object(YandexPayPlusBackendClient, 'get_merchant_keys', mocker.AsyncMock(return_value=keys))


@pytest.mark.asyncio
async def test_get_merchant_keys(merchant, user, client_mock, mock_action, keys):
    role_mock = mock_action(GetMerchantAction)

    returned = await ListMerchantKeysAction(user=user, merchant_id=merchant.merchant_id).run()

    role_mock.assert_run_once_with(user=user, merchant_id=merchant.merchant_id)
    client_mock.assert_called_once_with(merchant_id=merchant.merchant_id)
    assert_that(returned, equal_to(keys))
