from dataclasses import replace
from uuid import UUID, uuid4

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, contains_inanyorder, equal_to, has_property, instance_of, match_equality, not_none

from pay.bill_payments.bill_payments.core.actions.order.create import CreateOrderAction
from pay.bill_payments.bill_payments.core.exceptions import BillNotFoundError
from pay.bill_payments.bill_payments.storage.entities.bill import Bill
from pay.bill_payments.bill_payments.storage.entities.enums import BillStatus, OrderStatus
from pay.bill_payments.bill_payments.storage.entities.order import Order
from pay.bill_payments.bill_payments.storage.entities.user import User


@pytest.fixture
def example_bill(user, document):
    return Bill(
        uid=user.uid,
        supplier_bill_id='bill-1',
        document_id=document.document_id,
        status=BillStatus.NEW,
        amount=10000,
        amount_to_pay=15000,
        bill_date=utcnow(),
    )


@pytest.fixture
async def bills(storage, example_bill):
    bills = []
    bills.append(await storage.bill.create(replace(example_bill)))

    example_bill = replace(example_bill, supplier_bill_id='bill-2')
    bills.append(await storage.bill.create(example_bill))

    return bills


@pytest.fixture
def params(bills, user):
    return dict(uid=user.uid, bill_ids=[bill.bill_id for bill in bills])


@pytest.mark.asyncio
async def test_returns_created(params, user):
    order = await CreateOrderAction(**params).run()

    assert_that(
        order,
        equal_to(
            Order(
                uid=user.uid,
                order_id=match_equality(instance_of(UUID)),
                status=OrderStatus.NEW,
                created=match_equality(not_none()),
                updated=match_equality(not_none()),
            )
        ),
    )


@pytest.mark.asyncio
async def test_creates_in_db(params, storage):
    order = await CreateOrderAction(**params).run()

    stored_order = await storage.order.get(order.order_id)
    assert_that(
        order,
        equal_to(stored_order),
    )


@pytest.mark.asyncio
async def test_links_bills(params, storage, bills):
    order = await CreateOrderAction(**params).run()

    bill_orders = await storage.bill_order.find_by_order_id(order.order_id)
    assert_that(
        bill_orders,
        contains_inanyorder(
            has_property('bill_id', bills[0].bill_id),
            has_property('bill_id', bills[1].bill_id),
        ),
    )


@pytest.mark.asyncio
async def test_bill_not_found(params, storage, user):
    non_existent_bill = uuid4()
    params['bill_ids'] = [non_existent_bill]

    with pytest.raises(BillNotFoundError) as exc_info:
        await CreateOrderAction(**params).run()

    assert_that(exc_info.value.bill_id, equal_to(non_existent_bill))


@pytest.mark.asyncio
async def test_authorizes_user_against_bills(params, storage, example_bill, user):
    other_user = await storage.user.create(User(uid=user.uid + 1))
    foreign_bill = replace(example_bill, uid=other_user.uid)
    foreign_bill = await storage.bill.create(foreign_bill)
    params['bill_ids'] = [foreign_bill.bill_id]

    with pytest.raises(BillNotFoundError) as exc_info:
        await CreateOrderAction(**params).run()

    assert_that(exc_info.value.bill_id, equal_to(foreign_bill.bill_id))
