from datetime import timedelta
from decimal import Decimal
from uuid import uuid4

import psycopg2.errors
import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.customer import Customer
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.customer_serial import CustomerSerial
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import (
    ClassicOrderStatus,
    OrderEventSource,
    OrderEventStatus,
    PaymentMethodType,
)
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.order import Order, OrderData
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.order_status_event import OrderStatusEvent


@pytest.mark.asyncio
async def test_create(storage, customer, customer_serial, make_order):
    order = make_order()

    created = await storage.order.create(order)

    order.order_id = customer_serial.order_id
    order.created = created.created
    order.updated = created.updated
    assert_that(
        created,
        equal_to(order),
    )


@pytest.mark.asyncio
async def test_duplicate_pretty_id_not_allowed(make_order, storage):
    pretty_id = 'duplicate'
    order1 = make_order(message_id='1:msgid1', pretty_id=pretty_id)
    await storage.order.create(order1)

    order2 = make_order(message_id='1:msgid2', pretty_id=pretty_id)
    pattern = 'uniq_orders_pretty_id'
    with pytest.raises(psycopg2.errors.UniqueViolation, match=pattern):
        await storage.order.create(order2)


@pytest.mark.asyncio
async def test_get(storage, customer, make_order):
    order = make_order()

    created = await storage.order.create(order)

    got = await storage.order.get(created.uid, created.order_id)

    assert_that(
        got,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_dumps_and_maps_data_with_null_fields(storage, customer, make_order):
    order = make_order(data=OrderData())

    created = await storage.order.create(order)

    got = await storage.order.get(created.uid, created.order_id)

    assert_that(
        got.data,
        equal_to(order.data),
    )


@pytest.mark.asyncio
async def test_get_not_found(storage):
    with pytest.raises(Order.DoesNotExist):
        await storage.order.get(1, 2)


@pytest.mark.asyncio
@pytest.mark.parametrize('payment_method_type', list(PaymentMethodType))
async def test_save(storage, customer, payment_method_type):
    order = Order(
        uid=customer.uid,
        message_id='1:msgid',
        currency='XTS',
        amount=Decimal('15.0'),
        cashback=Decimal('10.0'),
        cashback_category=Decimal('0.66'),
        refunded_amount=Decimal('2'),
        refunded_cashback=Decimal('1'),
        status=ClassicOrderStatus.NEW,
        psp_id=uuid4(),
        merchant_id=uuid4(),
        payment_method_type=payment_method_type,
    )
    created = await storage.order.create(order)
    created.currency = 'USD'
    created.message_id = '2:msgid'
    created.amount += 1
    created.cashback_category = Decimal('0.55')
    created.refunded_amount += 10
    created.refunded_cashback += 5

    saved = await storage.order.save(created)

    created.updated = saved.updated
    assert_that(
        saved,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_order_message_id_is_unique(storage, make_order):
    await storage.order.create(make_order(message_id='msg-id'))

    with pytest.raises(Order.DuplicateMessageID):
        await storage.order.create(make_order(message_id='msg-id'))


@pytest.mark.asyncio
async def test_get_by_message_id(storage, make_order):
    created = await storage.order.create(make_order(message_id='msg-id'))

    found = await storage.order.get_by_message_id('msg-id')

    assert_that(found, equal_to(created))


@pytest.mark.asyncio
async def test_find_by_message_id(storage, make_order):
    await storage.order.create(make_order(message_id='m'))
    created = [
        await storage.order.create(make_order(message_id='msg-id', payment_id='1')),
        await storage.order.create(make_order(message_id='msg-id', payment_id='2')),
    ]

    found = await storage.order.find_by_message_id('msg-id')

    assert_that(sorted(found, key=lambda x: x.payment_id), equal_to(created))


@pytest.mark.asyncio
async def test_get_first_with_pending_status_events(storage, make_order, create_event):
    for i in range(1, 7):
        await storage.order.create(make_order(message_id='msg-id', payment_id=str(i)))

    # msg-id1 no events at all
    await create_event(message_id='msg-id', payment_id='2', event_status=OrderEventStatus.APPLIED)
    await create_event(message_id='msg-id', payment_id='3', event_time=utcnow() + timedelta(days=1))
    await create_event(message_id='msg-id', payment_id='4', run_at=utcnow() + timedelta(days=1))
    await create_event(message_id='msg-id', payment_id='5', run_at=utcnow() - timedelta(days=0.5))
    await create_event(message_id='msg-id', payment_id='6', run_at=utcnow() - timedelta(days=1))
    await create_event(message_id='msg-id', payment_id='7', run_at=utcnow() - timedelta(days=2))

    got = await storage.order.get_first_with_pending_status_events()

    assert_that(got.payment_id, equal_to('6'))


@pytest.mark.asyncio
async def test_get_first_with_pending_status_events__not_found(storage, make_order):
    await storage.order.create(make_order(message_id='msg-id'))
    await storage.order_status_event.create(
        OrderStatusEvent(
            event_id=uuid4(),
            message_id='msg-id',
            order_status=ClassicOrderStatus.SUCCESS,
            event_time=utcnow() - timedelta(days=1),
            event_status=OrderEventStatus.APPLIED,
            event_source=OrderEventSource.PSP,
        )
    )

    with pytest.raises(Order.DoesNotExist):
        await storage.order.get_first_with_pending_status_events()


@pytest.fixture
async def customer(storage):
    return await storage.customer.create(Customer(uid=1400))


@pytest.fixture(autouse=True)
async def customer_serial(storage, customer):
    return await storage.customer_serial.create(CustomerSerial(uid=customer.uid, order_id=10))


@pytest.fixture
def make_order(storage, customer):
    def _make_order(**kwargs):
        order = Order(
            uid=customer.uid,
            message_id='1:msgid',
            currency='XTS',
            amount=Decimal('15.0'),
            cashback=Decimal('10.0'),
            cashback_category=Decimal('0.66'),
            status=ClassicOrderStatus.NEW,
            merchant_id=uuid4(),
            psp_id=uuid4(),
            data=OrderData(
                last4='1234',
                order_cashback_limit=Decimal('100.0'),
                merchant_name='merchant-name',
                merchant_url='merchant-url',
            ),
            payment_method_type=PaymentMethodType.CARD,
        )
        for key in kwargs:
            setattr(order, key, kwargs[key])
        return order
    return _make_order


@pytest.fixture
def create_event(storage):
    async def _create_event(**kwargs):
        event = OrderStatusEvent(
            message_id=None,
            event_id=uuid4(),
            order_status=ClassicOrderStatus.SUCCESS,
            event_time=utcnow() - timedelta(days=1),
            event_status=OrderEventStatus.PENDING,
            run_at=utcnow() - timedelta(days=1),
            event_source=OrderEventSource.PSP,
        )
        for key in kwargs:
            setattr(event, key, kwargs[key])
        return await storage.order_status_event.create(event)
    return _create_event
