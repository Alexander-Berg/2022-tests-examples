from uuid import uuid4

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.merchant.get import GetMerchantAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.merchant_key.create import CreateMerchantKeyAction
from billing.yandex_pay_admin.yandex_pay_admin.core.exceptions import PutMerchantDataError
from billing.yandex_pay_admin.yandex_pay_admin.interactions import YandexPayPlusBackendClient
from billing.yandex_pay_admin.yandex_pay_admin.interactions.pay_backend import (
    MerchantKey,
    PayBackendClientResponseError,
)
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.enums import RoleType
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
async def key():
    return MerchantKey(key_id=uuid4(), created=utcnow(), value='1')


@pytest.fixture
def client_mock(mocker, key):
    return mocker.patch.object(YandexPayPlusBackendClient, 'create_merchant_key', mocker.AsyncMock(return_value=key))


@pytest.mark.asyncio
async def test_create_key(merchant, user, client_mock, key, mock_action):
    role_mock = mock_action(GetMerchantAction)

    returned = await CreateMerchantKeyAction(user=user, merchant_id=merchant.merchant_id).run()

    role_mock.assert_run_once_with(
        user=user,
        merchant_id=merchant.merchant_id,
        minimum_role_required=RoleType.OWNER,
    )
    client_mock.assert_called_once_with(merchant_id=merchant.merchant_id)
    assert_that(returned, equal_to(key))


@pytest.mark.asyncio
async def test_bad_request(merchant, user, mocker, key, mock_action):
    mocker.patch.object(
        YandexPayPlusBackendClient,
        'create_merchant_key',
        mocker.AsyncMock(
            side_effect=PayBackendClientResponseError(status_code=400, method='x', service='s', message='TOO_MANY_KEYS')
        ),
    )

    with pytest.raises(PutMerchantDataError) as exc_info:
        await CreateMerchantKeyAction(user=user, merchant_id=merchant.merchant_id).run()

    assert_that(exc_info.value.message, equal_to('TOO_MANY_KEYS'))
