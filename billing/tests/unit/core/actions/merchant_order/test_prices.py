from dataclasses import replace
from datetime import date
from decimal import Decimal

import pytest
from pay.lib.entities.cart import Cart, CartTotal
from pay.lib.entities.shipping import CourierOption, DeliveryCategory, ShippingMethod, ShippingMethodType, ShippingPrice

from sendr_pytest.matchers import equal_to
from sendr_pytest.mocks import explain_call_asserts, mock_action  # noqa

from hamcrest import assert_that

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant_order.prices import (
    UpdatePricesAction,
    ValidatePricesAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import (
    MerchantOrderAmountMismatchError,
    MerchantOrderCartIsEmptyError,
    MerchantOrderShippingIsEmptyError,
    MerchantOrderShippingMethodMismatchError,
    MerchantOrderShippingPriceNotApplicableError,
)
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.checkout_order import StorageCart, StorageShippingMethod


class TestValidatePricesAction:
    @pytest.mark.asyncio
    async def test_amount_valid(self, entity_checkout_order, cart):
        entity_checkout_order.cart = cart
        entity_checkout_order.order_amount = Decimal('123.45')

        await ValidatePricesAction(entity_checkout_order).run()

    @pytest.mark.asyncio
    async def test_amount_invalid(self, entity_checkout_order, cart):
        entity_checkout_order.cart = cart
        entity_checkout_order.order_amount = Decimal('543.21')

        with pytest.raises(MerchantOrderAmountMismatchError) as exc_info:
            await ValidatePricesAction(entity_checkout_order).run()

        params = exc_info.value.params
        assert_that(
            params,
            equal_to(
                dict(
                    total_amount='543.21',
                    authorize_amount=None,
                    capture_amount=None,
                    cart_amount='123.45',
                    shipping_amount='0',
                    description=params['description'],
                )
            ),
        )

    @pytest.mark.asyncio
    async def test_amount_valid_with_shipping(self, entity_checkout_order, cart, shipping_method):
        entity_checkout_order.cart = cart
        entity_checkout_order.shipping_method = shipping_method
        entity_checkout_order.order_amount = Decimal('200.00')

        await ValidatePricesAction(entity_checkout_order).run()

    @pytest.mark.asyncio
    async def test_amount_invalid_with_shipping(self, entity_checkout_order, cart, shipping_method):
        entity_checkout_order.cart = cart
        entity_checkout_order.shipping_method = shipping_method
        entity_checkout_order.order_amount = Decimal('777')

        with pytest.raises(MerchantOrderAmountMismatchError) as exc_info:
            await ValidatePricesAction(entity_checkout_order).run()

        params = exc_info.value.params
        assert_that(
            params,
            equal_to(
                dict(
                    total_amount='777',
                    authorize_amount=None,
                    capture_amount=None,
                    cart_amount='123.45',
                    shipping_amount='76.55',
                    description=params['description'],
                )
            ),
        )


class TestUpdatePricesAction:
    @pytest.mark.asyncio
    async def test_returned(self, entity_checkout_order, cart, shipping_method):
        result = await UpdatePricesAction(entity_checkout_order, cart=cart, shipping=shipping_method).run()

        updated_order = replace(
            entity_checkout_order,
            cart=StorageCart.from_cart(cart),
            shipping_method=StorageShippingMethod.from_shipping_method(shipping_method),
        )
        assert_that(result, equal_to(updated_order))

    @pytest.mark.asyncio
    async def test_returned__with_shipping_price(self, entity_checkout_order, cart, shipping_method, shipping_price):
        entity_checkout_order.shipping_method = StorageShippingMethod.from_shipping_method(shipping_method)
        result = await UpdatePricesAction(entity_checkout_order, cart=cart, shipping=shipping_price).run()

        updated_order = replace(
            entity_checkout_order,
            cart=StorageCart.from_cart(cart),
        )
        updated_order.shipping_method = replace(
            updated_order.shipping_method,
            courier_option=replace(
                updated_order.shipping_method.courier_option,
                amount=Decimal('11.22'),
            ),
        )
        assert_that(result, equal_to(updated_order))

    @pytest.mark.asyncio
    async def test_calls_validate(self, mock_validate, entity_checkout_order, cart, shipping_method):
        await UpdatePricesAction(entity_checkout_order, cart=cart, shipping=shipping_method).run()

        updated_order = replace(
            entity_checkout_order,
            cart=StorageCart.from_cart(cart),
            shipping_method=StorageShippingMethod.from_shipping_method(shipping_method),
        )
        mock_validate.assert_run_once_with(updated_order)

    @pytest.mark.asyncio
    async def test_when_cart_is_empty__should_raise(self, entity_checkout_order, shipping_method):
        with pytest.raises(MerchantOrderCartIsEmptyError):
            await UpdatePricesAction(entity_checkout_order, cart=None, shipping=shipping_method).run()

    @pytest.mark.asyncio
    async def test_when_shipping_is_empty_but_was_not_empty__should_raise(
        self, entity_checkout_order, cart, shipping_method
    ):
        entity_checkout_order.shipping_method = shipping_method
        with pytest.raises(MerchantOrderShippingIsEmptyError):
            await UpdatePricesAction(entity_checkout_order, cart=cart, shipping=None).run()

    @pytest.mark.asyncio
    async def test_when_shipping_is_empty_and_was_empty__should_be_fine(self, entity_checkout_order, cart):
        entity_checkout_order.shipping_method = None
        await UpdatePricesAction(entity_checkout_order, cart=cart, shipping=None).run()

    @pytest.mark.asyncio
    async def test_when_shipping_price_is_not_empty_but_order_method_was_empty__should_raise(
        self, entity_checkout_order, cart, shipping_price
    ):
        entity_checkout_order.shipping_method = None
        with pytest.raises(MerchantOrderShippingPriceNotApplicableError):
            await UpdatePricesAction(entity_checkout_order, cart=cart, shipping=shipping_price).run()

    @pytest.mark.asyncio
    async def test_when_shipping_price_not_matches_order_method_type__should_raise(
        self, entity_checkout_order, cart, shipping_method, shipping_price
    ):
        entity_checkout_order.shipping_method = shipping_method
        shipping_price.method_type = ShippingMethodType.PICKUP

        with pytest.raises(MerchantOrderShippingMethodMismatchError):
            await UpdatePricesAction(entity_checkout_order, cart=cart, shipping=shipping_price).run()

    @pytest.fixture(autouse=True)
    def mock_validate(self, mock_action):  # noqa
        return mock_action(ValidatePricesAction)


@pytest.fixture
def cart():
    return Cart(items=[], total=CartTotal(amount=Decimal('123.45')))


@pytest.fixture
def shipping_method():
    return ShippingMethod(
        method_type=ShippingMethodType.COURIER,
        courier_option=CourierOption(
            courier_option_id='courier_option_id',
            provider='provider',
            category=DeliveryCategory.STANDARD,
            title='label',
            amount=Decimal('76.55'),
            from_date=date(2000, 12, 30),
        ),
    )


@pytest.fixture
def shipping_price():
    return ShippingPrice(
        method_type=ShippingMethodType.COURIER,
        amount=Decimal('11.22'),
    )
