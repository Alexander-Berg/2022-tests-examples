from dataclasses import replace
from datetime import datetime, timezone
from decimal import Decimal

import pytest
from pay.lib.entities.cart import Cart, CartItem, CartItemType, CartTotal, ItemQuantity, ItemReceipt, Measurements
from pay.lib.entities.order import Order, PaymentMethodType
from pay.lib.entities.receipt import TaxType
from pay.lib.entities.shipping import (
    DeliveryCategory,
    Location,
    ShippingMethodType,
    ShippingOptions,
    ShippingWarehouse,
    YandexDeliveryOption,
    YandexDeliveryShippingParams,
)
from pay.lib.interactions.yandex_delivery.entities import (
    CheckPriceResponse,
    DeliveryInterval,
    DeliveryMethodInfo,
    GetDeliveryMethodsResponse,
    Item,
    ItemSize,
    SameDayDeliveryMethodInfo,
)
from pay.lib.interactions.yandex_delivery.exceptions import UnknownInteractionError

from sendr_pytest.matchers import equal_to

from hamcrest import assert_that, is_, none

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.delivery.render import RenderDeliveryAction
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import (
    CoreDataError,
    MerchantShippingItemWithNoMeasurementsError,
    MerchantShippingMethodNotConfiguredError,
    MerchantShippingWarehouseIsMissingError,
)
from billing.yandex_pay_plus.yandex_pay_plus.interactions.yandex_delivery import YandexDeliveryClient
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant import (
    DeliveryIntegrationParams,
    YandexDeliveryParams,
)


@pytest.mark.asyncio
async def test_success(params, shipping_options):
    returned = await RenderDeliveryAction(**params).run()

    assert_that(returned, equal_to(shipping_options))


@pytest.mark.asyncio
async def test_calls_get_delivery_methods(params, mock_ydelivery_get_delivery_methods, warehouse):
    await RenderDeliveryAction(**params).run()

    mock_ydelivery_get_delivery_methods.assert_awaited_once_with(
        auth_token='OaUtHtOkEn',
        start_point=warehouse.address.location,
    )


@pytest.mark.asyncio
async def test_calls_check_price(
    mocker, params, mock_ydelivery_check_price, warehouse, shipping_address, delivery_interval
):
    await RenderDeliveryAction(**params).run()

    mock_ydelivery_check_price.assert_has_awaits(
        [
            mocker.call(
                auth_token='OaUtHtOkEn',
                items=[
                    Item(
                        cost_currency='RUB',
                        cost_value='42.00',
                        droppof_point=1,
                        pickup_point=0,
                        title='Awesome Product',
                        weight=4.,
                        size=ItemSize(length=1., height=2., width=3.),
                        quantity=10,
                    ),
                ],
                route_points=[
                    warehouse.address.location,
                    shipping_address.location,
                ],
            ),
            mocker.call(
                auth_token='OaUtHtOkEn',
                items=[
                    Item(
                        cost_currency='RUB',
                        cost_value='42.00',
                        droppof_point=1,
                        pickup_point=0,
                        title='Awesome Product',
                        weight=4.,
                        size=ItemSize(length=1., height=2., width=3.),
                        quantity=10,
                    ),
                ],
                route_points=[
                    warehouse.address.location,
                    shipping_address.location,
                ],
                same_day_delivery_interval=delivery_interval,
            ),
        ]
    )


@pytest.mark.asyncio
async def test_when_no_shipping__returns_order_without_shipping(params):
    params['order'].shipping = None

    returned = await RenderDeliveryAction(**params).run()

    assert_that(returned, is_(none()))


@pytest.mark.asyncio
async def test_when_no_yandex_delivery__returns_shipping_without_yandex_delivery(params):
    params['order'].shipping.available_methods = [ShippingMethodType.COURIER]

    returned = await RenderDeliveryAction(**params).run()

    assert_that(returned.yandex_delivery, is_(none()))
    assert ShippingMethodType.YANDEX_DELIVERY not in returned.available_methods


@pytest.mark.asyncio
async def test_when_no_shipping_warehouse__uses_default_shipping(params, merchant):
    params['order'].shipping.yandex_delivery = None

    returned = await RenderDeliveryAction(**params).run()

    default_warehouse = merchant.delivery_integration_params.yandex_delivery.warehouses[0]
    assert_that(returned.yandex_delivery.warehouse, equal_to(default_warehouse))


@pytest.mark.asyncio
async def test_when_no_eligible_items__returns_shipping_without_yandex_delivery(params):
    params['order'].cart.items[0].type = CartItemType.DIGITAL

    returned = await RenderDeliveryAction(**params).run()

    assert_that(returned.yandex_delivery, is_(none()))
    assert ShippingMethodType.YANDEX_DELIVERY not in returned.available_methods


@pytest.mark.asyncio
async def test_when_item_without_measurements__fails(params, merchant, order):
    params['order'].cart.items[0].measurements = None
    params['merchant'].delivery_integration_params.measurements = None

    with pytest.raises(MerchantShippingItemWithNoMeasurementsError):
        await RenderDeliveryAction(**params).run()


@pytest.mark.asyncio
async def test_when_item_without_measurements__success(params, shipping_options):
    params['order'].cart.items[0].measurements = None
    returned = await RenderDeliveryAction(**params).run()

    assert_that(returned, equal_to(shipping_options))


@pytest.mark.asyncio
async def test_when_no_warehouses__fails(params, merchant):
    params['order'].shipping.yandex_delivery.warehouse = None
    merchant.delivery_integration_params.yandex_delivery.warehouses = []

    with pytest.raises(MerchantShippingWarehouseIsMissingError):
        await RenderDeliveryAction(**params).run()


@pytest.mark.asyncio
async def test_when_check_price_fails__returns_shipping_without_yandex_delivery(mocker, params, merchant):
    mocker.patch.object(
        YandexDeliveryClient,
        'check_price',
        mocker.AsyncMock(
            side_effect=UnknownInteractionError(
                service='service',
                method='method',
                status_code=0,
                params={'code': 'error_code', 'message': 'Human readable (maybe)'},
            ),
        ),
    )
    returned = await RenderDeliveryAction(**params).run()

    assert_that(returned.yandex_delivery, is_(none()))
    assert ShippingMethodType.YANDEX_DELIVERY not in returned.available_methods


@pytest.mark.asyncio
async def test_when_shipping_render_has_bad_config__fails(params, merchant):
    merchant.delivery_integration_params.yandex_delivery = None

    with pytest.raises(MerchantShippingMethodNotConfiguredError):
        await RenderDeliveryAction(**params).run()


@pytest.mark.asyncio
async def test_when_address_has_no_location__fails(params, warehouse):
    warehouse.address.location = None

    with pytest.raises(CoreDataError):
        await RenderDeliveryAction(**params).run()


@pytest.fixture(autouse=True)
def mock_ydelivery_get_delivery_methods(mocker, delivery_interval):
    return mocker.patch.object(
        YandexDeliveryClient,
        'get_delivery_methods',
        mocker.AsyncMock(
            return_value=GetDeliveryMethodsResponse(
                express_delivery=DeliveryMethodInfo(allowed=True),
                same_day_delivery=SameDayDeliveryMethodInfo(
                    allowed=True,
                    available_intervals=[delivery_interval],
                ),
            ),
        ),
    )


@pytest.fixture(autouse=True)
def mock_ydelivery_check_price(mocker):
    return mocker.patch.object(
        YandexDeliveryClient,
        'check_price',
        mocker.AsyncMock(
            side_effect=(
                CheckPriceResponse(price=Decimal('11.11')),
                CheckPriceResponse(price=Decimal('22.22')),
            ),
        ),
    )


@pytest.fixture
def params(merchant, shipping_address, order):
    return {
        'merchant': merchant,
        'shipping_address': shipping_address,
        'order': order,
    }


@pytest.fixture
def merchant(entity_merchant, entity_warehouse):
    return replace(
        entity_merchant,
        delivery_integration_params=DeliveryIntegrationParams(
            yandex_delivery=YandexDeliveryParams(
                oauth_token=YandexDeliveryParams.encrypt_oauth_token('OaUtHtOkEn'),
                warehouses=[entity_warehouse],
            ),
            measurements=Measurements(length=1, height=2, width=3, weight=4),
        ),
    )


@pytest.fixture
def shipping_address(entity_address):
    return replace(entity_address, location=Location(latitude=6, longitude=1))


@pytest.fixture
def order(entity_cart, warehouse):
    return Order(
        currency_code='XTS',
        cart=Cart(
            items=[
                CartItem(
                    title='Awesome Product',
                    type=CartItemType.PHYSICAL,
                    discounted_unit_price=Decimal('42.00'),
                    product_id='product-1',
                    quantity=ItemQuantity(count=Decimal('10')),
                    total=Decimal('42.00'),
                    receipt=ItemReceipt(
                        tax=TaxType.VAT_20,
                    ),
                    measurements=Measurements(
                        length=1.,
                        height=2.,
                        width=3.,
                        weight=4.,
                    ),
                ),
            ],
            total=CartTotal(amount=Decimal('42.00')),
        ),
        order_amount=Decimal('10.00'),
        shipping=ShippingOptions(
            available_methods=[ShippingMethodType.YANDEX_DELIVERY],
            yandex_delivery=YandexDeliveryShippingParams(
                warehouse=warehouse,
            ),
        ),
        available_payment_methods=[
            PaymentMethodType.CARD,
            PaymentMethodType.CARD_ON_DELIVERY,
            PaymentMethodType.CASH_ON_DELIVERY,
        ],
    )


@pytest.fixture
def warehouse(entity_address, entity_contact):
    return ShippingWarehouse(
        address=replace(
            entity_address,
            location=Location(latitude=5, longitude=31),
        ),
        contact=entity_contact,
        emergency_contact=entity_contact,
    )


@pytest.fixture
def delivery_interval():
    return DeliveryInterval(
        from_=datetime(2022, 1, 1, 0, 0, 0, tzinfo=timezone.utc),
        to=datetime(2022, 1, 1, 3, 0, 0, tzinfo=timezone.utc),
    )


@pytest.fixture
def shipping_options(warehouse):
    return ShippingOptions(
        available_methods=[ShippingMethodType.YANDEX_DELIVERY],
        yandex_delivery=YandexDeliveryShippingParams(
            warehouse=warehouse,
            options=[
                YandexDeliveryOption(
                    receipt=ItemReceipt(tax=TaxType.VAT_20),
                    title='Экспресс-доставка',
                    allowed_payment_methods=[PaymentMethodType.CARD, PaymentMethodType.CARD_ON_DELIVERY],
                    yandex_delivery_option_id='yandex-delivery:express',
                    amount=Decimal('11.11'),
                    category=DeliveryCategory.EXPRESS,
                ),
                YandexDeliveryOption(
                    receipt=ItemReceipt(tax=TaxType.VAT_20),
                    title='Доставка',
                    allowed_payment_methods=[PaymentMethodType.CARD, PaymentMethodType.CARD_ON_DELIVERY],
                    yandex_delivery_option_id=(
                        'yandex-delivery:sdd:2022-01-01T00:00:00+00:00_2022-01-01T03:00:00+00:00'
                    ),
                    amount=Decimal('22.22'),
                    category=DeliveryCategory.TODAY,
                    from_datetime=datetime(2022, 1, 1, 0, 0, 0, tzinfo=timezone.utc),
                    to_datetime=datetime(2022, 1, 1, 3, 0, 0, tzinfo=timezone.utc),
                ),
            ],
        ),
    )
