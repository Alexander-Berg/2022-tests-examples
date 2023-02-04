from uuid import uuid4

import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.merchant.get import GetMerchantAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.merchant_key.delete import DeleteMerchantKeyAction
from billing.yandex_pay_admin.yandex_pay_admin.core.exceptions import PutMerchantDataError
from billing.yandex_pay_admin.yandex_pay_admin.interactions import YandexPayPlusBackendClient
from billing.yandex_pay_admin.yandex_pay_admin.interactions.pay_backend import PayBackendClientResponseError
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
async def key_id():
    return uuid4()


@pytest.fixture
def client_mock(mocker):
    return mocker.patch.object(YandexPayPlusBackendClient, 'delete_merchant_key', mocker.AsyncMock())


@pytest.mark.asyncio
async def test_delete_key(merchant, user, client_mock, key_id, mock_action):
    role_mock = mock_action(GetMerchantAction)

    await DeleteMerchantKeyAction(user=user, merchant_id=merchant.merchant_id, key_id=key_id).run()

    role_mock.assert_run_once_with(
        user=user,
        merchant_id=merchant.merchant_id,
        minimum_role_required=RoleType.OWNER,
    )
    client_mock.assert_called_once_with(merchant_id=merchant.merchant_id, key_id=key_id)


@pytest.mark.asyncio
async def test_bad_request(merchant, user, mocker, key_id, mock_action):
    mocker.patch.object(
        YandexPayPlusBackendClient,
        'delete_merchant_key',
        mocker.AsyncMock(
            side_effect=PayBackendClientResponseError(status_code=400, method='x', service='s', message='ERROR')
        ),
    )

    with pytest.raises(PutMerchantDataError) as exc_info:
        await DeleteMerchantKeyAction(user=user, merchant_id=merchant.merchant_id, key_id=key_id).run()

    assert_that(exc_info.value.message, equal_to('ERROR'))
