import functools
from datetime import datetime, timezone
from decimal import Decimal
from uuid import uuid4

import pytest
from pay.lib.entities.cart import CartItem, CartTotal, ItemQuantity, ItemReceipt
from pay.lib.entities.enums import PaymentMethodType, ShippingMethodType
from pay.lib.entities.order import ContactFields
from pay.lib.entities.receipt import TaxType

from sendr_pytest.matchers import equal_to

from hamcrest import assert_that, has_properties

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.checkout_order import (
    CheckoutOrder,
    StorageAddress,
    StorageCart,
    StorageContact,
    StorageRequiredFields,
    StorageShippingMethod,
)
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.delivery import Delivery, StorageWarehouse


@pytest.mark.asyncio
async def test_create(storage, make_checkout_order):
    checkout_order = make_checkout_order()

    created = await storage.checkout_order.create(checkout_order)

    checkout_order.created = created.created
    checkout_order.updated = created.updated
    assert_that(
        created,
        equal_to(checkout_order),
    )


@pytest.mark.asyncio
async def test_order_id_and_merchant_id_pair_is_unique(make_checkout_order, storage, rands):
    checkout_order1 = make_checkout_order(merchant_id=uuid4(), order_id=rands())
    await storage.checkout_order.create(checkout_order1)

    checkout_order2 = make_checkout_order(merchant_id=checkout_order1.merchant_id, order_id=checkout_order1.order_id)
    with pytest.raises(CheckoutOrder.DuplicateOrderID):
        await storage.checkout_order.create(checkout_order2)


@pytest.mark.asyncio
async def test_get(storage, make_checkout_order):
    checkout_order = make_checkout_order()

    created = await storage.checkout_order.create(checkout_order)

    got = await storage.checkout_order.get(created.checkout_order_id)

    assert_that(
        got,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_with_related(storage, make_checkout_order, entity_warehouse):
    checkout_order = make_checkout_order()

    created = await storage.checkout_order.create(checkout_order)
    created.delivery = await storage.delivery.create(Delivery(
        checkout_order_id=created.checkout_order_id,
        merchant_id=created.merchant_id,
        price=Decimal(1),
        warehouse=StorageWarehouse.from_warehouse(entity_warehouse),
    ))

    got = await storage.checkout_order.get(created.checkout_order_id, skip_related=False)

    assert_that(
        got,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_dumps_and_maps_data_null_json_fields(storage, make_checkout_order):
    fields = (
        'available_payment_methods',
        'billing_contact',
        'required_fields',
        'shipping_address',
        'shipping_contact',
        'shipping_method',
    )
    checkout_order = make_checkout_order(**{f: None for f in fields})

    created = await storage.checkout_order.create(checkout_order)

    got = await storage.checkout_order.get(created.checkout_order_id)

    assert_that(
        got,
        has_properties({f: equal_to(None) for f in fields}),
    )


@pytest.mark.asyncio
async def test_get_not_found(storage):
    with pytest.raises(CheckoutOrder.DoesNotExist):
        await storage.checkout_order.get(uuid4())


@pytest.mark.asyncio
async def test_save(storage, make_checkout_order):
    created = await storage.checkout_order.create(make_checkout_order())
    created.currency_code = 'USD'
    created.cart.total.amount += 1
    created.order_amount += 1
    created.payment_method_type = PaymentMethodType.CARD_ON_DELIVERY
    created.required_fields.billing_contact.name = False

    saved = await storage.checkout_order.save(created)

    created.updated = saved.updated
    assert_that(
        saved,
        equal_to(created),
    )


@pytest.mark.asyncio
@pytest.mark.parametrize('payment_method_type', list(PaymentMethodType))
async def test_all_payment_method_types_supported(storage, payment_method_type, make_checkout_order):
    created = await storage.checkout_order.create(make_checkout_order(payment_method_type=PaymentMethodType.CARD))
    created.payment_method_type = payment_method_type

    saved = await storage.checkout_order.save(created)

    created.updated = saved.updated
    assert_that(
        saved,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_by_merchant_id_and_order_id(storage, make_checkout_order):
    created = await storage.checkout_order.create(make_checkout_order())

    found = await storage.checkout_order.get_by_merchant_id_and_order_id(
        merchant_id=created.merchant_id, order_id=created.order_id
    )

    assert_that(found, equal_to(created))


@pytest.mark.asyncio
async def test_find_by_merchant_id(storage, make_checkout_order):
    merchant_id = uuid4()
    dt = functools.partial(datetime, 2021, 12, 1, tzinfo=timezone.utc)
    find = functools.partial(storage.checkout_order.find_by_merchant_id, merchant_id=merchant_id)

    await storage.checkout_order.create(make_checkout_order())  # random merchant_id

    orders = []
    for i in range(5):
        order = await storage.checkout_order.create(make_checkout_order(merchant_id=merchant_id))
        orders.append(await storage.checkout_order.forge_created_datetime(order.checkout_order_id, created=dt(i)))

    assert_that(
        await find(limit=2),
        equal_to(orders[:2:-1]),
    )
    assert_that(
        await find(created_lt=dt(4), limit=2),
        equal_to(orders[3:1:-1]),
    )
    assert_that(
        await find(created_gte=dt(4), limit=5),
        equal_to(orders[:3:-1]),
    )
    assert_that(
        await find(created_lt=dt(4), created_gte=dt(2), limit=5),
        equal_to(orders[3:1:-1]),
    )


@pytest.fixture
def make_checkout_order(storage, rands, entity_address, entity_courier_option, stored_merchant):
    def _make_checkout_order(**kwargs):
        checkout_order = CheckoutOrder(
            merchant_id=stored_merchant.merchant_id,
            uid=555,
            currency_code='XTS',
            cart=StorageCart(
                total=CartTotal(amount=Decimal('10.00')),
                items=[
                    CartItem(
                        product_id='pid',
                        title='title',
                        discounted_unit_price=Decimal('10.00'),
                        receipt=ItemReceipt(
                            tax=TaxType.VAT_20,
                        ),
                        total=Decimal('10.00'),
                        quantity=ItemQuantity(count=Decimal('10.00')),
                    )
                ],
            ),
            chargeable=True,
            order_amount=Decimal('10.00'),
            order_id=rands(),
            payment_method_type=PaymentMethodType.CARD,
            shipping_method=StorageShippingMethod(
                method_type=ShippingMethodType.COURIER,
                courier_option=entity_courier_option,
            ),
            shipping_address=StorageAddress.from_address(entity_address),
            shipping_contact=StorageContact(id='scid'),
            billing_contact=StorageContact(
                id='bcid',
                first_name='fname',
                second_name='sname',
                last_name='lname',
                email='email',
                phone='phone',
            ),
            metadata='mdata',
            available_payment_methods=[PaymentMethodType.CARD],
            enable_coupons=False,
            enable_comment_field=False,
            required_fields=StorageRequiredFields(
                billing_contact=ContactFields(name=True, email=True, phone=True),
                shipping_contact=ContactFields(name=False, email=False, phone=False),
            ),
        )
        for key in kwargs:
            setattr(checkout_order, key, kwargs[key])
        return checkout_order

    return _make_checkout_order
