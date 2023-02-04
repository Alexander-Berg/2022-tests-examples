from decimal import Decimal
from uuid import uuid4

import pytest
from pay.lib.entities.enums import CardNetwork
from pay.lib.entities.order import Contact, Order, PaymentMethod, PaymentMethodType, PaymentStatus
from pay.lib.entities.shipping import Address, Location, ShippingMethod, ShippingMethodType
from pay.lib.interactions.merchant.entities import MerchantCreateOrderResponse
from pay.lib.interactions.merchant.exceptions import MerchantAPIMalformedResponseError, MerchantAPIResponseError

from sendr_interactions.exceptions import InteractionResponseError
from sendr_pytest.helpers import ensure_all_fields
from sendr_pytest.matchers import equal_to
from sendr_pytest.mocks import explain_call_asserts, mock_action  # noqa

from hamcrest import assert_that, has_properties, match_equality, not_none

import billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant_order.create
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant_order.create import CreateMerchantOrderAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant_order.prices import ValidatePricesAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant_order.resolve_address import ResolveAddressAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant_order.resolve_contact import ResolveContactAction
from billing.yandex_pay_plus.yandex_pay_plus.core.entities.form_order import CreateMerchantOrderRequest
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import (
    CoreMerchantNotFoundError,
    MerchantMalformedResponseError,
    MerchantRejectedOrderError,
    MerchantUnexpectedError,
    OrderAlreadyExistsError,
)
from billing.yandex_pay_plus.yandex_pay_plus.interactions.merchant import AbstractMerchantClient
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.checkout_order import (
    CheckoutOrder,
    StorageAddress,
    StorageCart,
    StorageContact,
    StorageShippingMethod,
)


@pytest.mark.asyncio
async def test_merchant_not_found(params):
    params['createmerchantorderrequest'].merchant_id = uuid4()
    with pytest.raises(CoreMerchantNotFoundError):
        await CreateMerchantOrderAction(**params).run()


@pytest.mark.asyncio
async def test_returned(params, expected_order):
    returned = await CreateMerchantOrderAction(**params).run()
    assert_that(returned, equal_to(expected_order))


@pytest.mark.asyncio
async def test_creates_order_in_db(params, storage):
    request: CreateMerchantOrderRequest = params['createmerchantorderrequest']
    returned = await CreateMerchantOrderAction(**params).run()

    order = await storage.checkout_order.get(returned.checkout_order_id)
    assert_that(
        order,
        equal_to(
            ensure_all_fields(
                CheckoutOrder,
                merchant_id=request.merchant_id,
                uid=params['user'].uid,
                currency_code=request.currency_code,
                cart=StorageCart.from_cart(request.cart),
                chargeable=True,
                checkout_order_id=match_equality(not_none()),
                order_amount=request.order_amount,
                authorize_amount=request.order_amount,
                capture_amount=None,
                payment_status=PaymentStatus.PENDING,
                order_id='real-order-id',
                payment_method_type=request.payment_method.method_type,
                shipping_method=StorageShippingMethod.from_shipping_method(request.shipping_method),
                shipping_address=ensure_all_fields(
                    StorageAddress,
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
                shipping_contact=ensure_all_fields(
                    StorageContact,
                    id='c-id',
                    first_name='first_name',
                    second_name='second_name',
                    last_name='last_name',
                    email='email',
                    phone='phone_number',
                ),
                billing_contact=ensure_all_fields(
                    StorageContact,
                    id='c-id',
                    first_name='first_name',
                    second_name='second_name',
                    last_name='last_name',
                    email='email',
                    phone='phone_number',
                ),
                metadata=request.metadata,
                created=match_equality(not_none()),
                updated=match_equality(not_none()),
                # А с этими полями, кстати, что делать?
                available_payment_methods=None,
                enable_coupons=None,
                enable_comment_field=None,
                required_fields=None,
                delivery=None,
            ),
        ),
    )


@pytest.mark.parametrize(
    'payment_method_type, shipping_address_id, is_chargeable',
    [
        (PaymentMethodType.CARD, None, False),
        (PaymentMethodType.SPLIT, 'ship-a-id', False),
        (PaymentMethodType.CARD_ON_DELIVERY, 'ship-a-id', False),
        (PaymentMethodType.CASH_ON_DELIVERY, 'ship-a-id', False),
        (PaymentMethodType.CARD, 'ship-a-id', True),
    ],
)
@pytest.mark.asyncio
async def test_fills_chargeable_fields(params, storage, payment_method_type, shipping_address_id, is_chargeable):
    request: CreateMerchantOrderRequest = params['createmerchantorderrequest']
    request.payment_method.method_type = payment_method_type
    request.shipping_address_id = shipping_address_id

    returned = await CreateMerchantOrderAction(**params).run()

    order = await storage.checkout_order.get(returned.checkout_order_id)
    assert_that(order.chargeable, equal_to(is_chargeable))


@pytest.mark.asyncio
async def test_calls_derive_cart_id(mocker, params, spy_derive_cart_id, entity_cart):
    await CreateMerchantOrderAction(**params).run()

    spy_derive_cart_id.assert_called_once_with(
        context=CreateMerchantOrderAction.context,
        cart=params['createmerchantorderrequest'].cart,
        seed=params['pay_session_id'],
    )


@pytest.mark.asyncio
async def test_calls_validate_prices(mocker, params, mock_validate, entity_cart, entity_courier_option):
    await CreateMerchantOrderAction(**params).run()

    mock_validate.assert_run_once_with(
        checkout_order=match_equality(
            has_properties(
                cart=StorageCart.from_cart(entity_cart),
                shipping_method=StorageShippingMethod(
                    method_type=ShippingMethodType.COURIER,
                    courier_option=entity_courier_option,
                ),
            ),
        ),
    )


@pytest.mark.asyncio
async def test_calls_merchant_create_order(params, mock_merchant_client, stored_merchant, expected_order):
    await CreateMerchantOrderAction(**params).run()

    mock_merchant_client.assert_awaited_once_with(
        base_url=stored_merchant.callback_url,
        order=expected_order,
    )


@pytest.fixture
def expected_order(params):
    request: CreateMerchantOrderRequest = params['createmerchantorderrequest']
    return Order(
        merchant_id=request.merchant_id,
        currency_code=request.currency_code,
        cart=request.cart,
        checkout_order_id=match_equality(not_none()),
        order_amount=request.order_amount,
        order_id='real-order-id',
        payment_method=request.payment_method,
        shipping_method=request.shipping_method,
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
        shipping_contact=Contact(
            id='c-id',
            first_name='first_name',
            second_name='second_name',
            last_name='last_name',
            email='email',
            phone='phone_number',
        ),
        billing_contact=Contact(
            id='c-id',
            first_name='first_name',
            second_name='second_name',
            last_name='last_name',
            email='email',
            phone='phone_number',
        ),
        metadata=request.metadata,
        t=params['createmerchantorderrequest'].t,
    )


@pytest.mark.parametrize(
    'merchant_exc, expected_exc, expected_exc_params',
    (
        pytest.param(
            MerchantAPIResponseError(status_code=200, method='POST', service='service', reason_code='1', reason='2'),
            MerchantRejectedOrderError,
            None,
            id='parsed-error-assumed-as-reject',
        ),
        pytest.param(
            InteractionResponseError(status_code=200, method='POST', service='service'),
            MerchantUnexpectedError,
            None,
            id='any-other-error-assumed-to-be-unexpected',
        ),
        pytest.param(
            MerchantAPIMalformedResponseError(service='service', validation_errors={'foo': ['bar']}),
            MerchantMalformedResponseError,
            {'validation_errors_description': {'foo': ['bar']}},
            id='raises-validation-error',
        ),
    ),
)
@pytest.mark.asyncio
async def test_handles_merchant_api_errors(mocker, merchant_exc, expected_exc, params, expected_exc_params):
    mocker.patch.object(AbstractMerchantClient, 'create_order_v1', mocker.AsyncMock(side_effect=merchant_exc))
    with pytest.raises(expected_exc) as exc_info:
        await CreateMerchantOrderAction(**params).run()

    if expected_exc_params is not None:
        assert_that(exc_info.value.params, equal_to(expected_exc_params))


class TestWhenOrderAlreadyExists:
    @pytest.mark.asyncio
    async def test_if_order_has_same_content__should_return_existing(self, mocker, params, mock_validate):
        order = await CreateMerchantOrderAction(**params).run()

        same_order = await CreateMerchantOrderAction(**params).run()

        assert_that(same_order.checkout_order_id, equal_to(order.checkout_order_id))

    @pytest.mark.asyncio
    async def test_if_order_has_different_content__should_raise(self, mocker, params, mock_validate):
        await CreateMerchantOrderAction(**params).run()
        params['createmerchantorderrequest'].shipping_method.courier_option.amount += Decimal('10')
        params['createmerchantorderrequest'].order_amount += Decimal('10')
        with pytest.raises(OrderAlreadyExistsError):
            await CreateMerchantOrderAction(**params).run()


@pytest.fixture
def spy_derive_cart_id(mocker):
    return mocker.spy(billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant_order.create, 'derive_cart_id')


@pytest.fixture(autouse=True)
def mock_merchant_client(mocker):
    return mocker.patch.object(
        AbstractMerchantClient,
        'create_order_v1',
        mocker.AsyncMock(return_value=MerchantCreateOrderResponse(order_id='real-order-id')),
    )


@pytest.fixture
async def params(entity_cart, stored_merchant, entity_auth_user, entity_courier_option):
    currency_code = 'XTS'
    order_amount = entity_cart.total.amount + entity_courier_option.amount
    return {
        'user': entity_auth_user,
        'createmerchantorderrequest': CreateMerchantOrderRequest(
            merchant_id=stored_merchant.merchant_id,
            currency_code=currency_code,
            cart=entity_cart,
            order_amount=order_amount,
            payment_method=PaymentMethod(
                method_type=PaymentMethodType.CARD,
                card_last4='1234',
                card_network=CardNetwork.VISA,
            ),
            shipping_method=ShippingMethod(
                method_type=ShippingMethodType.COURIER,
                courier_option=entity_courier_option,
            ),
            shipping_address_id='ship-a-id',
            shipping_contact_id='ship-c-id',
            billing_contact=Contact(
                id='c-id',
                first_name='first_name',
                second_name='second_name',
                last_name='last_name',
                email='email',
                phone='phone_number',
            ),
            metadata='mdata',
        ),
        'pay_session_id': 'pay-session-id',
    }


@pytest.fixture(autouse=True)
def mock_validate(mock_action):  # noqa
    return mock_action(ValidatePricesAction)


@pytest.fixture(autouse=True)
def mock_address(mock_action):  # noqa
    return mock_action(
        ResolveAddressAction,
        return_value=Address(
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
            location=Location(latitude=30.15, longitude=15.30),
            locale='locale',
            address_line='address_line',
            district='district',
        ),
    )


@pytest.fixture(autouse=True)
def mock_contact(mock_action):  # noqa
    return mock_action(
        ResolveContactAction,
        return_value=Contact(
            id='c-id',
            first_name='first_name',
            second_name='second_name',
            last_name='last_name',
            email='email',
            phone='phone_number',
        ),
    )
