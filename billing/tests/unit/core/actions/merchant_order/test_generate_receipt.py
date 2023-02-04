from datetime import date
from decimal import Decimal
from typing import List, Optional

import pytest
from pay.lib.entities.cart import Cart, CartItem, CartTotal, Discount, ItemQuantity, ItemReceipt
from pay.lib.entities.enums import DeliveryCategory
from pay.lib.entities.order import Order
from pay.lib.entities.receipt import (
    Agent,
    AgentType,
    MarkQuantity,
    MeasureType,
    PaymentSubjectType,
    PaymentType,
    Receipt,
    ReceiptItem,
    ReceiptItemQuantity,
    Supplier,
    TaxType,
)
from pay.lib.entities.shipping import CourierOption, ShippingMethod, ShippingMethodType

from sendr_pytest.helpers import ensure_all_fields
from sendr_pytest.matchers import equal_to
from sendr_pytest.mocks import explain_call_asserts, mock_action  # noqa

from hamcrest import assert_that

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant_order.generate_receipt import GenerateReceiptAction

FULL_ITEM_RECEIPT = ItemReceipt(
    tax=TaxType.VAT_10,
    title='Custom title',
    excise=Decimal('1.12'),
    payment_method_type=PaymentType.FULL_PAYMENT,
    measure=MeasureType.OTHER,
    payment_subject_type=PaymentSubjectType.SERVICE,
    product_code=bytes.fromhex('fefefe'),
    mark_quantity=MarkQuantity(numerator=2, denominator=3),
    agent=Agent(agent_type=AgentType.PAYMENT_AGENT, operation='operation'),
    supplier=Supplier(inn='12345', name='Supplier'),
)

FULL_RECEIPT_ITEM_FIELDS = dict(
    tax=TaxType.VAT_10,
    title='Custom title',
    excise=Decimal('1.12'),
    payment_method_type=PaymentType.FULL_PAYMENT,
    payment_subject_type=PaymentSubjectType.SERVICE,
    product_code=bytes.fromhex('fefefe'),
    mark_quantity=MarkQuantity(numerator=2, denominator=3),
    agent=Agent(agent_type=AgentType.PAYMENT_AGENT, operation='operation'),
    supplier=Supplier(inn='12345', name='Supplier'),
)


def c_item(
    pid: int, price: str, quantity: str, receipt: Optional[ItemReceipt] = None, mark_denominator: Optional[int] = None
) -> CartItem:
    if receipt is None:
        receipt = ItemReceipt(tax=TaxType.VAT_20)
    if mark_denominator is not None:
        receipt.mark_quantity = MarkQuantity(numerator=int(quantity), denominator=mark_denominator)
    return CartItem(
        title=f'Product {pid}',
        product_id=f'product-{pid}',
        quantity=ItemQuantity(count=Decimal(quantity)),
        discounted_unit_price=Decimal(price),
        unit_price=Decimal(price),
        subtotal=Decimal(quantity) * Decimal(price),
        total=Decimal(quantity) * Decimal(price),
        receipt=receipt,
    )


def r_item(pid: int, price: str, quantity: str, mark_denominator: Optional[int] = None) -> ReceiptItem:
    return ReceiptItem(
        title=f'Product {pid}',
        product_id=f'product-{pid}',
        discounted_unit_price=Decimal(price),
        quantity=ReceiptItemQuantity(count=Decimal(quantity)),
        tax=TaxType.VAT_20,
        mark_quantity=MarkQuantity(numerator=int(quantity), denominator=mark_denominator) if mark_denominator else None,
    )


def _shipping(receipt: Optional[ItemReceipt] = None) -> ShippingMethod:
    return ShippingMethod(
        method_type=ShippingMethodType.COURIER,
        courier_option=CourierOption(
            courier_option_id='courier-1',
            provider='CDEK',
            category=DeliveryCategory.STANDARD,
            title='Courier 1',
            amount=Decimal('39.00'),
            from_date=date(2022, 3, 1),
            receipt=receipt,
        ),
    )


def _make_order(
    items: List[CartItem],
    discounts: List[str] = None,
    shipping: Optional[ShippingMethod] = None,
) -> Order:
    if discounts is None:
        discounts = []
    discounts = [Discount(discount_id='d-1', amount=Decimal(d), description='') for d in discounts]
    cart_total = sum(i.total for i in items) - sum(d.amount for d in discounts)
    return Order(
        currency_code='RUB',
        cart=Cart(items=items, discounts=discounts, total=CartTotal(amount=cart_total)),
        shipping_method=shipping,
        order_amount=cart_total + (shipping.get_option().amount if shipping else 0),
    )


@pytest.mark.asyncio
async def test_no_receipt():
    order = _make_order(
        [
            c_item(1, '4.99', '2'),
            c_item(2, '4.99', '1.5'),
        ]
    )
    order.cart.items[-1].receipt = None

    receipt = await GenerateReceiptAction(cart=order.cart, shipping_method=order.shipping_method).run()

    assert_that(receipt, equal_to(None))


@pytest.mark.asyncio
async def test_generate_receipt():
    order = _make_order(
        [
            c_item(1, '4.99', '2'),
            c_item(2, '9.99', '1', FULL_ITEM_RECEIPT),
            CartItem(
                product_id='3',
                quantity=ItemQuantity(count=Decimal(3)),
                total=Decimal('3.69'),
                unit_price=Decimal('1.23'),
                receipt=ItemReceipt(tax=TaxType.VAT_10),
            ),
            CartItem(
                product_id='4',
                quantity=ItemQuantity(count=Decimal(4)),
                total=Decimal('4.00'),
                receipt=ItemReceipt(tax=TaxType.VAT_10),
            ),
        ]
    )

    receipt = await GenerateReceiptAction(cart=order.cart, shipping_method=order.shipping_method).run()

    expected_receipt_items = [
        r_item(1, '4.99', '2'),
        ensure_all_fields(
            ReceiptItem,
            discounted_unit_price=Decimal('9.99'),
            quantity=ReceiptItemQuantity(count=Decimal('1'), measure=MeasureType.OTHER),
            product_id='product-2',
            **FULL_RECEIPT_ITEM_FIELDS,
        ),
        ReceiptItem(
            title='3',
            product_id='3',
            discounted_unit_price=Decimal('1.23'),
            quantity=ReceiptItemQuantity(count=Decimal(3)),
            tax=TaxType.VAT_10,
        ),
        ReceiptItem(
            title='4',
            product_id='4',
            discounted_unit_price=Decimal('1'),
            quantity=ReceiptItemQuantity(count=Decimal(4)),
            tax=TaxType.VAT_10,
        ),
    ]
    assert_that(
        receipt,
        equal_to(Receipt(items=expected_receipt_items)),
    )


@pytest.mark.parametrize(
    'shipping,expected_receipt_item',
    [
        pytest.param(
            _shipping(),
            ReceiptItem(
                title='Courier 1',
                discounted_unit_price=Decimal('39.00'),
                quantity=ReceiptItemQuantity(count=Decimal('1'), measure=MeasureType.PIECE),
                tax=TaxType.VAT_20,
                product_id='courier-1',
            ),
            id='default',
        ),
        pytest.param(
            _shipping(receipt=FULL_ITEM_RECEIPT),
            ensure_all_fields(
                ReceiptItem,
                discounted_unit_price=Decimal('39.00'),
                quantity=ReceiptItemQuantity(count=Decimal('1'), measure=MeasureType.OTHER),
                product_id='courier-1',
                **FULL_RECEIPT_ITEM_FIELDS,
            ),
            id='custom',
        ),
    ],
)
@pytest.mark.asyncio
async def test_generate_receipt_with_shipping(shipping, expected_receipt_item):
    order = _make_order(
        items=[c_item(1, '4.99', '1')],
        discounts=['2'],
        shipping=shipping,
    )

    receipt = await GenerateReceiptAction(cart=order.cart, shipping_method=order.shipping_method).run()

    expected_receipt_items = [
        r_item(1, '2.99', '1'),
        expected_receipt_item,
    ]
    assert_that(receipt, equal_to(Receipt(items=expected_receipt_items)))


@pytest.mark.parametrize(
    'discounts,items,expected_receipt_items',
    [
        pytest.param(
            ['1.00', '0.50', '0.01'],
            [c_item(1, '4.99', '1')],
            [r_item(1, '3.48', '1')],
            id='single-item-multiple-discounts',
        ),
        pytest.param(
            ['1.00'],
            [c_item(1, '4.99', '3', mark_denominator=7)],
            [r_item(1, '4.66', '2', mark_denominator=7), r_item(1, '4.65', '1', mark_denominator=7)],
            id='position-split',
        ),
        pytest.param(
            ['2.00'],
            [c_item(1, '0.02', '5'), c_item(2, '2.00', '3'), c_item(3, '1.00', '2')],
            [r_item(1, '0.02', '5'), r_item(2, '1.50', '3'), r_item(3, '0.75', '2')],
            id='distribution',
        ),
        pytest.param(
            ['2.00'],
            [c_item(2, '2.00', '4'), c_item(3, '0.01', '100'), c_item(1, '0.02', '100')],
            [r_item(2, '1.60', '4'), r_item(3, '0.01', '100'), r_item(1, '0.02', '60'), r_item(1, '0.01', '40')],
            id='distribution-split',
        ),
        pytest.param(
            ['10.00'],
            [c_item(2, '2000.0', '0.001'), c_item(3, '5.00', '2.222')],
            [r_item(2, '474.44', '0.001'), r_item(3, '1.19', '1.360'), r_item(3, '1.18', '0.862')],
            id='non-integer-quantity-split',
        ),
    ],
)
@pytest.mark.asyncio
async def test_apply_discounts(discounts, items, expected_receipt_items):
    order = _make_order(items=items, discounts=discounts)

    receipt = await GenerateReceiptAction(cart=order.cart, shipping_method=order.shipping_method).run()

    assert_that(
        receipt,
        equal_to(Receipt(items=expected_receipt_items)),
    )


def coupon_c_item(
    pid: int, quantity: str, total: str, unit_price: Optional[str] = None, receipt: Optional[ItemReceipt] = None
):
    if receipt is None:
        receipt = ItemReceipt(tax=TaxType.VAT_20)
    return CartItem(
        title=f'Product {pid}',
        product_id=f'product-{pid}',
        quantity=ItemQuantity(count=Decimal(quantity)),
        unit_price=Decimal(unit_price) if unit_price is not None else None,
        total=Decimal(total),
        receipt=receipt,
    )


@pytest.mark.parametrize(
    'items,expected_receipt_items',
    [
        pytest.param(
            [coupon_c_item(1, '1', '390.95', None)],
            [r_item(1, '390.95', '1')],
            id='simple-item',
        ),
        pytest.param(
            [
                coupon_c_item(1, '1', '390.95', None),
                coupon_c_item(2, '2', '1327.37', '737.43'),
            ],
            [r_item(1, '390.95', '1'), r_item(2, '663.68', '1'), r_item(2, '663.69', '1')],
            id='item-with-split',
        ),
        pytest.param(
            [
                coupon_c_item(1, '1', '390.95', None),
                coupon_c_item(2, '7', '699.99', '111.11'),
            ],
            [r_item(1, '390.95', '1'), r_item(2, '99.99', '1'), r_item(2, '100', '6')],
            id='item-with-large-split',
        ),
        pytest.param(
            [
                coupon_c_item(1, '0.733', '130.99'),
            ],
            [r_item(1, '178.70', '0.443'), r_item(1, '178.71', '0.29')],
            id='split-with-float-count',
        ),
    ],
)
@pytest.mark.asyncio
async def test_with_apply_coupon(items, expected_receipt_items):
    order = _make_order(items=items)

    receipt = await GenerateReceiptAction(cart=order.cart, shipping_method=order.shipping_method).run()
    assert_that(
        receipt,
        equal_to(Receipt(items=expected_receipt_items)),
    )
