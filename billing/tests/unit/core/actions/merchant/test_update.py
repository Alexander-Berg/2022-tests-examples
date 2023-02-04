import logging
from uuid import uuid4

import pytest

from hamcrest import assert_that, equal_to, has_entries, has_item, has_properties, is_

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.merchant.update import UpdateMerchantAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.pay_backend.put_merchant import PayBackendPutMerchantAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.role import AuthorizeRoleAction
from billing.yandex_pay_admin.yandex_pay_admin.core.exceptions import (
    InsecureCallbackUrlSchemaError,
    MerchantNotFoundError,
)
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.merchant import (
    DeliveryIntegrationParams,
    Merchant,
    YandexDeliveryParams,
)


@pytest.fixture
async def merchant(storage, partner, role):
    return await storage.merchant.create(
        Merchant(
            name='merchant name',
            partner_id=partner.partner_id,
        )
    )


@pytest.fixture()
def delivery_integration_params():
    return DeliveryIntegrationParams(YandexDeliveryParams(oauth_token="token", autoaccept=False))


@pytest.fixture(autouse=True)
async def mock_pay_backend(mock_action):
    return mock_action(PayBackendPutMerchantAction)


def test_should_run_in_transaction():
    assert_that(UpdateMerchantAction.transact, is_(True))


@pytest.mark.asyncio
@pytest.mark.parametrize('callback_url', [None, 'https://merchant.test'])
async def test_update_merchant(user, merchant, callback_url, delivery_integration_params):
    name = 'new name'

    updated_merchant = await UpdateMerchantAction(
        user=user,
        partner_id=merchant.partner_id,
        merchant_id=merchant.merchant_id,
        name=name,
        callback_url=callback_url,
        delivery_integration_params=delivery_integration_params,
    ).run()

    merchant.name = name
    merchant.revision += 1
    merchant.updated = updated_merchant.updated
    merchant.callback_url = callback_url
    merchant.delivery_integration_params = delivery_integration_params

    assert_that(updated_merchant, equal_to(merchant))


@pytest.mark.asyncio
async def test_update_merchant_logged(user, merchant, dummy_logs):
    name = 'new name'

    await UpdateMerchantAction(
        user=user,
        partner_id=merchant.partner_id,
        merchant_id=merchant.merchant_id,
        name=name,
    ).run()

    logs = dummy_logs()
    assert_that(
        logs,
        has_item(
            has_properties(
                message='MERCHANT_UPDATED',
                levelno=logging.INFO,
                _context=has_entries(
                    uid=user.uid,
                    partner_id=merchant.partner_id,
                    merchant_id=merchant.merchant_id,
                ),
            )
        ),
    )


@pytest.mark.asyncio
async def test_update_skipped_if_name_and_callback_url_unchanged(user, merchant, dummy_logs, mock_pay_backend):
    updated_merchant = await UpdateMerchantAction(
        user=user,
        partner_id=merchant.partner_id,
        merchant_id=merchant.merchant_id,
        name=merchant.name,
    ).run()

    assert_that(updated_merchant, equal_to(merchant))

    logs = dummy_logs()
    assert_that(
        logs,
        has_item(
            has_properties(
                message='MERCHANT_UPDATE_SKIPPED',
                levelno=logging.INFO,
                _context=has_entries(
                    uid=user.uid,
                    partner_id=merchant.partner_id,
                    merchant_id=merchant.merchant_id,
                ),
            )
        ),
    )
    mock_pay_backend.assert_not_run()


@pytest.mark.asyncio
async def test_pay_backend_called(user, mock_pay_backend, merchant):
    name = 'new name'
    merchant = await UpdateMerchantAction(
        user=user,
        partner_id=merchant.partner_id,
        merchant_id=merchant.merchant_id,
        name=name,
    ).run()

    mock_pay_backend.assert_run_once_with(merchant=merchant)


@pytest.mark.asyncio
async def test_pay_backend_not_called_if_propagation_disabled(user, mock_pay_backend, merchant):
    await UpdateMerchantAction(
        user=user,
        partner_id=merchant.partner_id,
        merchant_id=merchant.merchant_id,
        name='new name',
        propagate=False,
    ).run()

    mock_pay_backend.assert_not_run()


@pytest.mark.asyncio
async def test_calls_authorize_action(user, merchant, mock_action):
    mock = mock_action(AuthorizeRoleAction)

    await UpdateMerchantAction(
        user=user,
        partner_id=merchant.partner_id,
        merchant_id=merchant.merchant_id,
        name=merchant.name,
    ).run()

    mock.assert_run_once_with(
        partner_id=merchant.partner_id,
        user=user,
    )


@pytest.mark.asyncio
async def test_cannot_update_missing_merchant(user, merchant):
    with pytest.raises(MerchantNotFoundError):
        await UpdateMerchantAction(
            user=user,
            partner_id=merchant.partner_id,
            merchant_id=uuid4(),
            name='new name',
        ).run()


@pytest.mark.asyncio
async def test_cannot_update_merchant_belonging_to_different_partner(user, merchant, mock_action):
    mock_action(AuthorizeRoleAction)

    with pytest.raises(MerchantNotFoundError):
        await UpdateMerchantAction(
            user=user,
            partner_id=uuid4(),
            merchant_id=merchant.merchant_id,
            name='new name',
        ).run()


@pytest.mark.asyncio
@pytest.mark.parametrize('url', ['http://insecure.test', '127.0.0.1', '[::1]:8000'])
async def test_insecure_callback_url(user, merchant, url):
    with pytest.raises(InsecureCallbackUrlSchemaError):
        await UpdateMerchantAction(
            user=user,
            partner_id=merchant.partner_id,
            merchant_id=merchant.merchant_id,
            name=merchant.name,
            callback_url=url,
        ).run()
