import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant import YandexDeliveryParams
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant_key import MerchantKey
from billing.yandex_pay_plus.yandex_pay_plus.tests.lib.functional.mocks.yandex_delivery import YandexDeliveryMocker
from billing.yandex_pay_plus.yandex_pay_plus.tests.lib.functional.steps.checkout_delivery import (
    CheckoutOrderDeliveryStepper,
)
from billing.yandex_pay_plus.yandex_pay_plus.tests.lib.functional.steps.checkout_order import CheckoutOrderStepper


@pytest.fixture
async def authenticate_merchant(stored_merchant, storage):
    await storage.merchant_key.create(MerchantKey.create(merchant_id=stored_merchant.merchant_id, key='UNITTEST_KEY'))

    def _authenticate_merchant(test_client):
        test_client.session.headers['Authorization'] = f'Api-Key {stored_merchant.merchant_id.hex}.UNITTEST_KEY'

    return _authenticate_merchant


@pytest.mark.asyncio
async def test_response(
    public_app,
    storage,
    aioresponses_mocker,
    stored_merchant,
    authenticate_client,
    authenticate_merchant,
    mock_sessionid_auth,
    yandex_pay_plus_settings,
):
    stored_merchant.delivery_integration_params.yandex_delivery = YandexDeliveryParams(
        oauth_token=YandexDeliveryParams.encrypt_oauth_token('123')
    )
    stored_merchant = await storage.merchant.save(stored_merchant)

    yandex_delivery = YandexDeliveryMocker(
        aioresponses_mocker, yandex_pay_plus_settings.YANDEX_DELIVERY_API_URL
    )
    order_stepper = CheckoutOrderStepper(
        merchant_id=stored_merchant.merchant_id,
        merchant_callback_url=stored_merchant.callback_url,
        public_client=public_app,
        aioresponses_mocker=aioresponses_mocker,
        yandex_pay_plus_settings=yandex_pay_plus_settings,
    )
    delivery_stepper = CheckoutOrderDeliveryStepper(
        merchant_id=stored_merchant.merchant_id,
        merchant_callback_url=stored_merchant.callback_url,
        public_client=public_app,
        aioresponses_mocker=aioresponses_mocker,
        yandex_pay_plus_settings=yandex_pay_plus_settings,
    )
    authenticate_client(public_app)
    order_id = await order_stepper.make_order_with_delivery()
    authenticate_merchant(public_app)
    await delivery_stepper.create_delivery(order_id)

    yandex_delivery.mock_cancel_info(claim_id='the-claim-id', cancel_state='paid')
    cancel_info = (
        await (
            await public_app.get(
                f'/api/merchant/v1/orders/{order_id}/delivery/cancel-info',
                raise_for_status=True,
            )
        ).json()
    )['data']['cancelState']

    assert_that(cancel_info, equal_to('PAID'))
