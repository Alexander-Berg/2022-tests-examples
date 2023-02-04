from dataclasses import replace
from decimal import Decimal
from uuid import uuid4

import pytest
from pay.lib.entities.order import Order, PaymentMethodType
from pay.lib.entities.shipping import Address, Location, ShippingMethodType, ShippingOptions
from pay.lib.interactions.passport_addresses.entities import Location as PassportLocation

from sendr_pytest.helpers import ensure_all_fields
from sendr_pytest.mocks import explain_call_asserts, mock_action  # noqa

from hamcrest import assert_that, equal_to, instance_of, match_equality

import billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant_order.render
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.delivery.render import RenderDeliveryAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant_order.render import RenderMerchantOrderAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant_order.resolve_address import ResolveAddressAction
from billing.yandex_pay_plus.yandex_pay_plus.core.cart import get_derived_cart_id
from billing.yandex_pay_plus.yandex_pay_plus.core.entities.form_order import RenderMerchantOrderRequest
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import CoreMerchantNotFoundError
from billing.yandex_pay_plus.yandex_pay_plus.interactions.merchant import AbstractMerchantClient


@pytest.mark.asyncio
async def test_returned(params, stub_render_delivery, mock_merchant_response):
    returned = await RenderMerchantOrderAction(**params).run()
    assert_that(
        returned,
        equal_to(
            replace(
                mock_merchant_response,
                cart=replace(
                    mock_merchant_response.cart,
                    cart_id=get_derived_cart_id('pay-session-id'),
                ),
            )
        ),
    )


@pytest.mark.asyncio
async def test_merchant_not_found(params):
    params['rendermerchantorderrequest'].merchant_id = uuid4()
    with pytest.raises(CoreMerchantNotFoundError):
        await RenderMerchantOrderAction(**params).run()


@pytest.mark.asyncio
async def test_calls_get_address(params, mock_address, entity_auth_user):
    await RenderMerchantOrderAction(**params).run()

    mock_address.assert_run_once_with(
        user=entity_auth_user,
        address_id='ship-a-id',
    )


@pytest.mark.asyncio
async def test_calls_render_delivery(params, mock_render_delivery, shipping_address, stored_merchant):
    order = await RenderMerchantOrderAction(**params).run()

    mock_render_delivery.assert_run_once_with(
        shipping_address=shipping_address,
        merchant=stored_merchant,
        order=match_equality(instance_of(Order)),
    )

    assert order.t is not None


@pytest.mark.asyncio
async def test_when_no_shipping_address__does_not_call_render_delivery(
    params, mock_action, mock_render_delivery  # noqa
):
    mock_action(
        ResolveAddressAction,
        return_value=None,
    )

    await RenderMerchantOrderAction(**params).run()

    mock_render_delivery.assert_not_run()


@pytest.mark.asyncio
async def test_when_no_shipping_from_merchant__does_not_call_render_delivery(
    params, mock_render_delivery, mock_merchant_response
):
    mock_merchant_response.shipping = None

    await RenderMerchantOrderAction(**params).run()

    mock_render_delivery.assert_not_run()


@pytest.mark.asyncio
async def test_calls_merchant_render_order(params, mock_merchant_client, stored_merchant, entity_auth_user):
    await RenderMerchantOrderAction(**params).run()

    mock_merchant_client.assert_awaited_once_with(
        base_url=stored_merchant.callback_url,
        order=Order(
            merchant_id=params['rendermerchantorderrequest'].merchant_id,
            currency_code=params['rendermerchantorderrequest'].currency_code,
            cart=params['rendermerchantorderrequest'].cart,
            shipping_address=ensure_all_fields(
                Address,
                id='a-id',
                country='country',
                locality='locality',
                street='street',
                building='building',
                region='region',
                room='room',
                entrance='entrance',
                floor='floor',
                intercom='intercom',
                comment='comment',
                zip='zip',
                location=ensure_all_fields(
                    Location,
                    latitude=30.15,
                    longitude=15.30,
                ),
                locale='locale',
                address_line='address_line',
                district='district',
            ),
            metadata=params['rendermerchantorderrequest'].metadata,
        ),
    )


@pytest.fixture
def mock_merchant_response(params):
    return Order(
        currency_code=params['rendermerchantorderrequest'].currency_code,
        cart=replace(params['rendermerchantorderrequest'].cart, cart_id=None),
        order_amount=Decimal('10.00'),
        shipping=ShippingOptions(available_methods=[ShippingMethodType.COURIER, ShippingMethodType.PICKUP]),
        available_payment_methods=[PaymentMethodType.CARD, PaymentMethodType.CARD_ON_DELIVERY],
        enable_coupons=True,
    )


@pytest.fixture(autouse=True)
def mock_merchant_client(mocker, mock_merchant_response):
    return mocker.patch.object(
        AbstractMerchantClient, 'render_order', mocker.AsyncMock(return_value=mock_merchant_response)
    )


@pytest.fixture
def stub_render_delivery(mocker):
    class RenderDeliveryActionStub:
        def __init__(self, /, order, **kwargs):
            self.order = order

        async def run(self):
            return self.order.shipping

    return mocker.patch.object(
        billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant_order.render,
        'RenderDeliveryAction',
        RenderDeliveryActionStub,
    )


@pytest.fixture(autouse=True)
def mock_render_delivery(mock_action):  # noqa
    return mock_action(RenderDeliveryAction)


@pytest.fixture
def params(entity_cart, stored_merchant, entity_auth_user):
    return {
        'user': entity_auth_user,
        'rendermerchantorderrequest': RenderMerchantOrderRequest(
            merchant_id=stored_merchant.merchant_id,
            currency_code='XTS',
            cart=entity_cart,
            shipping_address_id='ship-a-id',
            metadata='mdata',
        ),
        'pay_session_id': 'pay-session-id',
    }


@pytest.fixture
def shipping_address():
    return Address(
        country='country',
        locality='locality',
        street='street',
        building='building',
        id='a-id',
        region='region',
        room='room',
        entrance='entrance',
        floor='floor',
        intercom='intercom',
        comment='comment',
        zip='zip',
        location=PassportLocation(latitude=30.15, longitude=15.30),
        locale='locale',
        address_line='address_line',
        district='district',
    )


@pytest.fixture(autouse=True)
def mock_address(mock_action, shipping_address):  # noqa
    return mock_action(
        ResolveAddressAction,
        return_value=shipping_address,
    )
