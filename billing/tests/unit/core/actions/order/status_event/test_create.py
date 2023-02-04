from datetime import timedelta
from decimal import Decimal
from uuid import uuid4

import pytest
from pay.lib.entities.enums import PaymentMethodType
from pay.lib.entities.payment_sheet import PaymentMerchant

from sendr_pytest.matchers import convert_then_match
from sendr_utils import alist, utcnow

from hamcrest import assert_that, contains_inanyorder, equal_to, greater_than, has_entries, has_properties

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.customer import CreateCustomerAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.order.create import CreateOrderAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.order.status_event.create import CreateOrderStatusEventAction
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import OrderEventAlreadyExistsError, OrderNotFoundError
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import ClassicOrderStatus, OrderEventStatus
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.order import Order, OrderData


@pytest.fixture
def message_id(rands):
    return rands()


@pytest.fixture
async def customer():
    return await CreateCustomerAction(uid=123).run()


@pytest.fixture
async def order(storage, message_id, customer, rands):
    return await storage.order.create(
        Order(
            uid=customer.uid,
            message_id=message_id,
            currency='XTS',
            amount=Decimal('44'),
            cashback=Decimal('4'),
            cashback_category=Decimal('4'),
            psp_id=uuid4(),
            merchant_id=uuid4(),
            status=ClassicOrderStatus.NEW,
            trust_card_id=rands(),
            payment_method_type=PaymentMethodType.CARD,
            data=OrderData(
                last4='5678',
                merchant_name='merchant_name',
                merchant_url='merchant_url',
                card_network='MIR',
                country_code='RU',
                antifraud_external_id='antifraud_external_id',
                order_basket=dict(id='order-id'),
            ),
        )
    )


@pytest.mark.asyncio
async def test_action_creates_event(storage, message_id):
    event_time = utcnow()
    order_status = ClassicOrderStatus.HOLD

    saved = await CreateOrderStatusEventAction(
        message_id=message_id,
        status=order_status,
        event_time=event_time,
    ).run()

    loaded = await storage.order_status_event.find_ensure_one(
        filters={'message_id': message_id}
    )
    assert_that(saved, equal_to(loaded))
    assert_that(
        loaded,
        has_properties(
            message_id=message_id,
            order_status=order_status,
            event_status=OrderEventStatus.PENDING,
            params=equal_to({}),
            details=equal_to({}),
            event_time=event_time,
            run_at=greater_than(utcnow()),
        )
    )


@pytest.mark.asyncio
async def test_action_creates_event_with_params(storage, message_id):
    amount = Decimal(100)
    reason_code = 'fake_reason_code'
    reason = 'fake_reason'

    await CreateOrderStatusEventAction(
        message_id=message_id,
        status=ClassicOrderStatus.HOLD,
        event_time=utcnow(),
        amount=amount,
        reason_code=reason_code,
        reason=reason,
    ).run()

    loaded = await storage.order_status_event.find_ensure_one(
        filters={'message_id': message_id}
    )
    assert_that(
        loaded,
        has_properties(
            message_id=message_id,
            event_status=OrderEventStatus.PENDING,
            params=has_entries(
                amount=convert_then_match(Decimal, amount),
                reason_code=reason_code,
                reason=reason,
            ),
            details=equal_to({}),
        )
    )


@pytest.mark.asyncio
async def test_action_creates_multiple_events(storage, message_id):
    event_time = utcnow()

    event1 = await CreateOrderStatusEventAction(
        message_id=message_id,
        status=ClassicOrderStatus.HOLD,
        event_time=event_time,
    ).run()

    event2 = await CreateOrderStatusEventAction(
        message_id=message_id,
        status=ClassicOrderStatus.FAIL,
        event_time=event_time + timedelta(milliseconds=1),
    ).run()

    event3 = await CreateOrderStatusEventAction(
        message_id=message_id,
        status=ClassicOrderStatus.FAIL,
        event_time=event_time + timedelta(milliseconds=2),
    ).run()

    events = await alist(
        storage.order_status_event.find(filters={'message_id': message_id})
    )

    assert_that(events, contains_inanyorder(event1, event2, event3))


@pytest.mark.asyncio
async def test_duplicate_events_with_different_params(storage, message_id):
    event_time = utcnow()

    saved = await CreateOrderStatusEventAction(
        message_id=message_id,
        status=ClassicOrderStatus.HOLD,
        event_time=event_time,
    ).run()

    with pytest.raises(OrderEventAlreadyExistsError):
        await CreateOrderStatusEventAction(
            message_id=message_id,
            status=ClassicOrderStatus.SUCCESS,
            event_time=event_time,
        ).run()

    with pytest.raises(OrderEventAlreadyExistsError):
        await CreateOrderStatusEventAction(
            message_id=message_id,
            status=ClassicOrderStatus.HOLD,
            event_time=event_time,
            amount=Decimal(100),
        ).run()

    loaded = await storage.order_status_event.find_ensure_one(
        filters={'message_id': message_id}
    )
    assert_that(saved, equal_to(loaded))


@pytest.mark.asyncio
async def test_duplicate_event_not_created(storage, message_id):
    event_time = utcnow()

    saved = await CreateOrderStatusEventAction(
        message_id=message_id,
        status=ClassicOrderStatus.HOLD,
        event_time=event_time,
    ).run()

    duplicate = await CreateOrderStatusEventAction(
        message_id=message_id,
        status=ClassicOrderStatus.HOLD,
        event_time=event_time,
    ).run()

    assert_that(duplicate, equal_to(saved))
    loaded = await storage.order_status_event.find_ensure_one(
        filters={'message_id': message_id}
    )
    assert_that(saved, equal_to(loaded))


@pytest.mark.asyncio
async def test_calls_create_order(storage, order, mock_action):
    amount = Decimal('55')
    create_mock = mock_action(CreateOrderAction, order)

    await CreateOrderStatusEventAction(
        message_id=order.message_id,
        status=ClassicOrderStatus.HOLD,
        event_time=utcnow(),
        payment_id='recurring_payment',
        amount=amount,
        recurring=True,
    ).run()

    create_mock.assert_called_once_with(
        uid=order.uid,
        message_id=order.message_id,
        payment_id='recurring_payment',
        currency=order.currency,
        amount=amount,
        force_category=Decimal(0),
        psp_id=order.psp_id,
        merchant=PaymentMerchant(
            id=order.merchant_id,
            name=order.data.merchant_name,
            url=order.data.merchant_url,
        ),
        payment_method_type=order.payment_method_type,
        trust_card_id=order.trust_card_id,
        card_id=order.card_id,
        last4=order.data.last4,
        card_network=order.data.card_network,
        country_code=order.data.country_code,
    )


@pytest.mark.asyncio
async def test_updates_payment_id(storage, order, mock_action):
    payment_id = 'recurring_payment'
    create_mock = mock_action(CreateOrderAction, order)

    await CreateOrderStatusEventAction(
        message_id=order.message_id,
        status=ClassicOrderStatus.HOLD,
        event_time=utcnow(),
        payment_id=payment_id,
        amount=Decimal('55'),
    ).run()

    stored = await storage.order.get_by_message_id(order.message_id, payment_id=payment_id)
    order.updated = stored.updated
    order.payment_id = payment_id
    assert_that(stored, equal_to(order))
    create_mock.assert_not_called()


@pytest.mark.parametrize('status', (ClassicOrderStatus.REFUND, ClassicOrderStatus.FAIL, ClassicOrderStatus.REVERSE))
@pytest.mark.asyncio
async def test_skips_create_order(storage, order, mock_action, status):
    create_mock = mock_action(CreateOrderAction, order)

    await CreateOrderStatusEventAction(
        message_id=order.message_id,
        status=status,
        event_time=utcnow(),
        payment_id='recurring_payment',
        amount=Decimal('55'),
    ).run()

    create_mock.assert_not_called()


@pytest.mark.asyncio
async def test_order_not_found(storage, message_id):
    with pytest.raises(OrderNotFoundError):
        await CreateOrderStatusEventAction(
            message_id=message_id,
            status=ClassicOrderStatus.HOLD,
            event_time=utcnow(),
            payment_id='recurring_payment',
        ).run()
