import uuid
from datetime import timedelta
from decimal import Decimal

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_budget import CashbackBudget
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_card_sheet import CashbackCardSheet
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_reservation import CashbackReservation
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_user_sheet import CashbackUserSheet
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.customer import Customer
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.customer_serial import CustomerSerial


@pytest.fixture
async def budget(storage):
    return await storage.cashback_budget.create(
        CashbackBudget(
            budget_id=uuid.uuid4(),
            currency='XTS',
            spent=Decimal('0'),
            spending_limit=Decimal('1000000'),
            period_start=utcnow() - timedelta(days=1),
            period_end=utcnow() + timedelta(days=10),
        )
    )


@pytest.fixture
async def customer(storage):
    return await storage.customer.create(Customer(uid=1500))


@pytest.fixture(autouse=True)
async def customer_serial(storage, customer):
    return await storage.customer_serial.create(
        CustomerSerial(uid=customer.uid, sheet_id=10)
    )


@pytest.fixture
async def user_sheet(storage, customer):
    return await storage.cashback_user_sheet.create(
        CashbackUserSheet(
            uid=customer.uid,
            currency='XTS',
            spent=Decimal('0'),
            spending_limit=Decimal('100000'),
            period_start=utcnow() - timedelta(days=1),
            period_end=utcnow() + timedelta(days=10)
        ),
    )


@pytest.fixture
async def card_sheet(storage):
    return await storage.cashback_card_sheet.create(
        CashbackCardSheet(
            trust_card_id='card-x123abc',
            currency='XTS',
            spent=Decimal('0'),
            spending_limit=Decimal('100000'),
            period_start=utcnow() - timedelta(days=1),
            period_end=utcnow() + timedelta(days=10)
        ),
    )


@pytest.fixture
def make_cashback_reservation(storage, customer, budget, user_sheet, card_sheet):
    def _make_cashback_reservation(**kwargs):
        cashback_reservation = CashbackReservation(
            uid=customer.uid,
            budget_id=budget.budget_id,
            cashback_user_sheet_id=user_sheet.sheet_id,
            cashback_card_sheet_id=card_sheet.sheet_id,
            amount=Decimal('100'),
        )
        for key in kwargs:
            setattr(cashback_reservation, key, kwargs[key])
        return cashback_reservation
    return _make_cashback_reservation


@pytest.mark.asyncio
async def test_create(storage, make_cashback_reservation):
    cashback_reservation = make_cashback_reservation()

    created = await storage.cashback_reservation.create(cashback_reservation)

    cashback_reservation.created = created.created
    cashback_reservation.updated = created.updated
    assert_that(created, equal_to(cashback_reservation))


@pytest.mark.asyncio
async def test_get(storage, customer, make_cashback_reservation):
    cashback_reservation = make_cashback_reservation()

    created = await storage.cashback_reservation.create(cashback_reservation)
    got = await storage.cashback_reservation.get(created.uid, created.reservation_id)

    assert_that(
        got,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_not_found(storage):
    with pytest.raises(CashbackReservation.DoesNotExist):
        await storage.cashback_reservation.get(1, 2)


@pytest.mark.asyncio
async def test_save(storage, customer, make_cashback_reservation):
    cashback_reservation = make_cashback_reservation()

    created = await storage.cashback_reservation.create(cashback_reservation)
    created.amount += 10

    saved = await storage.cashback_reservation.save(created)

    created.updated = saved.updated
    assert_that(
        saved,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_create_using_same_budget_and_sheet_succeeds(
    storage, customer, make_cashback_reservation
):
    cashback_reservation = make_cashback_reservation()
    created1 = await storage.cashback_reservation.create(cashback_reservation)
    created2 = await storage.cashback_reservation.create(cashback_reservation)

    assert_that(created2.reservation_id, equal_to(created1.reservation_id + 1))

    created2.reservation_id = created1.reservation_id
    created2.created = created1.created
    created2.updated = created1.updated
    assert_that(created1, equal_to(created2))
