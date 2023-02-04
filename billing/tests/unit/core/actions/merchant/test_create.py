import logging

import pytest

from hamcrest import assert_that, equal_to, has_entries, has_item, has_properties, is_

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.merchant.create import CreateMerchantAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.pay_backend.put_merchant import PayBackendPutMerchantAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.role import AuthorizeRoleAction
from billing.yandex_pay_admin.yandex_pay_admin.core.exceptions import (
    InsecureCallbackUrlSchemaError,
    TooManyMerchantsError,
)
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.merchant import (
    DeliveryIntegrationParams,
    YandexDeliveryParams,
)
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.origin import OriginBackbone


@pytest.fixture(autouse=True)
async def mock_pay_backend(mock_action):
    return mock_action(PayBackendPutMerchantAction)


def test_should_run_in_transaction():
    assert_that(CreateMerchantAction.transact, is_(True))


@pytest.fixture()
def delivery_integration_params():
    return DeliveryIntegrationParams(YandexDeliveryParams(oauth_token="token", autoaccept=False))


@pytest.mark.asyncio
@pytest.mark.usefixtures('role')
@pytest.mark.parametrize('callback_url', [None, 'https://merchant.test'])
async def test_creates_merchant_entity(storage, partner, user, callback_url, delivery_integration_params):
    merchant = await CreateMerchantAction(
        partner_id=partner.partner_id,
        user=user,
        name='test name',
        callback_url=callback_url,
        delivery_integration_params=delivery_integration_params,
    ).run()

    loaded_merchant = await storage.merchant.get(merchant.merchant_id)

    assert_that(loaded_merchant, equal_to(merchant))
    assert_that(
        loaded_merchant,
        has_properties(
            name='test name',
            revision=1,
            paypartsup_issue_id=None,
            callback_url=callback_url,
            delivery_integration_params=delivery_integration_params,
        ),
    )


@pytest.mark.asyncio
@pytest.mark.usefixtures('role')
@pytest.mark.parametrize('callback_url', ['http://insecure.test', '127.0.0.1', '[::1]:8000'])
async def test_insecure_callback_url(user, partner, callback_url):
    with pytest.raises(InsecureCallbackUrlSchemaError):
        await CreateMerchantAction(
            partner_id=partner.partner_id,
            user=user,
            name='test name',
            callback_url=callback_url,
        ).run()


@pytest.mark.asyncio
@pytest.mark.usefixtures('role')
async def test_call_logged(partner, user, dummy_logs):
    merchant = await CreateMerchantAction(
        partner_id=partner.partner_id,
        user=user,
        name='test name',
        callback_url='https://merchant.test',
    ).run()

    logs = dummy_logs()
    assert_that(
        logs,
        has_item(
            has_properties(
                message='MERCHANT_CREATED',
                levelno=logging.INFO,
                _context=has_entries(
                    uid=user.uid,
                    partner_id=partner.partner_id,
                    merchant_id=merchant.merchant_id,
                    callback_url='https://merchant.test',
                ),
            )
        ),
    )


@pytest.mark.asyncio
@pytest.mark.usefixtures('role')
async def test_pay_backend_called(partner, user, mock_pay_backend):
    merchant = await CreateMerchantAction(
        partner_id=partner.partner_id,
        user=user,
        name='test name',
    ).run()

    mock_pay_backend.assert_run_once_with(merchant=merchant, force_add_origin=False)


@pytest.mark.asyncio
async def test_calls_authorize_action(partner, user, mock_action):
    mock = mock_action(AuthorizeRoleAction)

    await CreateMerchantAction(
        partner_id=partner.partner_id,
        user=user,
        name='test name',
    ).run()

    mock.assert_run_once_with(partner_id=partner.partner_id, user=user)


@pytest.mark.asyncio
@pytest.mark.usefixtures('role')
async def test_max_number_of_merchants_exceeded(partner, user, mocker):
    mocker.patch.object(CreateMerchantAction, 'max_merchants_per_partner', 0)

    with pytest.raises(TooManyMerchantsError):
        await CreateMerchantAction(
            partner_id=partner.partner_id,
            user=user,
            name='test name',
        ).run()


@pytest.mark.asyncio
async def test_register_merchant_without_user(storage, partner, mock_pay_backend, mock_action):
    mock_authorize = mock_action(AuthorizeRoleAction)
    name = 'merchant name'
    origins = origins = [OriginBackbone('https://127.0.0.1')]
    merchant = await CreateMerchantAction(
        partner_id=partner.partner_id,
        name=name,
        origins=origins,
    ).run()
    origins = await storage.origin.find_by_merchant_id(merchant.merchant_id)

    assert origins == origins
    mock_authorize.assert_not_called()
    mock_pay_backend.assert_run_once_with(merchant=merchant, force_add_origin=False)
