from uuid import uuid4

import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.cms.merchant.create import CreateMerchantForCMSAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.merchant.create import CreateMerchantAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.merchant.load import GetMerchantWithLazyFieldsAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.merchant.update import UpdateMerchantAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.origin.create import CreateOriginAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.partner.create import CreatePartnerAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.partner.update_data import UpdatePartnerDataAction
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.pay_backend.put_merchant import PayBackendPutMerchantAction
from billing.yandex_pay_admin.yandex_pay_admin.core.exceptions import CallbackUrlAlreadyExistsError
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.merchant import (
    DeliveryIntegrationParams,
    YandexDeliveryParams,
)
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.origin import Origin, OriginBackbone
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.partner import Contact, RegistrationData


@pytest.mark.asyncio
async def test_returns_merchant(params, merchant_entity):
    returned = await CreateMerchantForCMSAction(**params).run()

    assert_that(returned, equal_to(merchant_entity))


@pytest.mark.asyncio
async def test_creates_partner(
    params, mock_create_partner, mock_update_partner_data, user, registration_data, partner_entity
):
    await CreateMerchantForCMSAction(**params).run()

    mock_create_partner.assert_run_once_with(
        user=user,
        name='partner-name',
        registration_data=registration_data,
    )
    mock_update_partner_data.assert_run_once_with(
        user=user,
        partner=partner_entity,
        registration_data=registration_data,
    )


@pytest.mark.asyncio
async def test_reuses_partner_if_already_exists(storage, partner, role, params, mock_create_partner):
    await CreateMerchantForCMSAction(**params).run()

    mock_create_partner.assert_not_run()


@pytest.mark.asyncio
@pytest.mark.parametrize('callback_url', [None, 'https://url.test'])
async def test_creates_merchant(
    params,
    mock_create_merchant,
    mock_update_merchant,
    user,
    partner_entity,
    merchant_entity,
    callback_url,
    delivery_integration_params,
):
    if callback_url:
        params['callback_url'] = callback_url
    params['delivery_integration_params'] = delivery_integration_params

    await CreateMerchantForCMSAction(**params).run()

    mock_create_merchant.assert_run_once_with(
        user=user,
        name='partner-name',
        partner_id=partner_entity.partner_id,
        propagate=False,
    )
    mock_update_merchant.assert_run_once_with(
        user=user,
        partner_id=partner_entity.partner_id,
        merchant_id=merchant_entity.merchant_id,
        name=merchant_entity.name,
        propagate=False,
        callback_url=callback_url,
        delivery_integration_params=delivery_integration_params,
    )


@pytest.mark.asyncio
async def test_reuses_merchant_if_already_exists(partner, role, merchant, params, mock_create_merchant):
    params['partner_name'] = merchant.name
    await CreateMerchantForCMSAction(**params).run()

    mock_create_merchant.assert_not_run()


@pytest.mark.asyncio
async def test_still_creates_merchant_if_name_does_not_match(partner, role, merchant, params, mock_create_merchant):
    params['partner_name'] = merchant.name + '-does-not-match'
    await CreateMerchantForCMSAction(**params).run()

    mock_create_merchant.assert_run_once()


@pytest.mark.asyncio
async def test_creates_origin(params, mock_create_origin, user, partner_entity, merchant_entity):
    await CreateMerchantForCMSAction(**params).run()

    mock_create_origin.assert_run_once_with(
        user=user,
        partner_id=partner_entity.partner_id,
        merchant_id=merchant_entity.merchant_id,
        origin='https://a.test',
        post_moderation=True,
        propagate=False,
    )


@pytest.mark.asyncio
async def test_reuses_origin_if_already_exists(
    storage, params, mock_create_origin, user, partner, role, merchant, mock_action
):
    mock_action(UpdateMerchantAction, merchant)
    params['partner_name'] = merchant.name
    await storage.origin.create(Origin(origin_id=uuid4(), origin='https://a.test', merchant_id=merchant.merchant_id))

    await CreateMerchantForCMSAction(**params).run()

    mock_create_origin.assert_not_run()


@pytest.mark.asyncio
async def test_calls_put_merchant(params, mock_put_merchant, merchant_entity):
    await CreateMerchantForCMSAction(**params).run()

    mock_put_merchant.assert_run_once_with(merchant=merchant_entity)


@pytest.mark.asyncio
async def test_cannot_overwrite_callback_url(
    params, mock_create_merchant, mock_update_merchant, user, partner_entity, merchant_entity
):
    merchant_entity.callback_url = 'https://first.test'
    params['callback_url'] = 'https://second.test'

    with pytest.raises(CallbackUrlAlreadyExistsError):
        await CreateMerchantForCMSAction(**params).run()

    mock_create_merchant.assert_run_once_with(
        user=user,
        name='partner-name',
        partner_id=partner_entity.partner_id,
        propagate=False,
    )
    mock_update_merchant.assert_not_run()


@pytest.fixture
def registration_data():
    return RegistrationData(
        contact=Contact(),
        tax_ref_number='11001100',
    )


@pytest.fixture
def params(user, registration_data):
    return {
        'user': user,
        'partner_registration_data': registration_data,
        'partner_name': 'partner-name',
        'origins': [OriginBackbone(origin='https://a.test')],
    }


@pytest.fixture()
def delivery_integration_params():
    return DeliveryIntegrationParams(YandexDeliveryParams(oauth_token="token", autoaccept=False))


@pytest.fixture(autouse=True)
def mock_create_partner(mock_action, partner_entity):
    return mock_action(CreatePartnerAction, partner_entity)


@pytest.fixture(autouse=True)
def mock_update_partner_data(mock_action, partner_entity):
    return mock_action(UpdatePartnerDataAction, partner_entity)


@pytest.fixture(autouse=True)
def mock_create_merchant(mock_action, merchant_entity):
    return mock_action(CreateMerchantAction, merchant_entity)


@pytest.fixture(autouse=True)
def mock_update_merchant(mock_action, merchant_entity):
    return mock_action(UpdateMerchantAction, merchant_entity)


@pytest.fixture(autouse=True)
def mock_create_origin(mock_action):
    return mock_action(CreateOriginAction)


@pytest.fixture(autouse=True)
def mock_load_merchant(mock_action, merchant_entity):
    return mock_action(GetMerchantWithLazyFieldsAction, merchant_entity)


@pytest.fixture(autouse=True)
def mock_put_merchant(mock_action):
    return mock_action(PayBackendPutMerchantAction)
