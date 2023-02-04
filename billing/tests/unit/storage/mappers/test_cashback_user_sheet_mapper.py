from datetime import datetime, timedelta, timezone
from decimal import Decimal

import psycopg2.errors
import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_user_sheet import CashbackUserSheet
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.customer import Customer
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.customer_serial import CustomerSerial


@pytest.mark.asyncio
async def test_create(storage, customer, customer_serial, make_cashback_user_sheet):
    cashback_user_sheet = make_cashback_user_sheet()

    created = await storage.cashback_user_sheet.create(cashback_user_sheet)

    cashback_user_sheet.sheet_id = customer_serial.sheet_id
    cashback_user_sheet.created = created.created
    cashback_user_sheet.updated = created.updated
    assert_that(
        created,
        equal_to(cashback_user_sheet),
    )


@pytest.mark.asyncio
async def test_get(storage, customer, make_cashback_user_sheet):
    cashback_user_sheet = make_cashback_user_sheet()

    created = await storage.cashback_user_sheet.create(cashback_user_sheet)

    got = await storage.cashback_user_sheet.get(created.uid, created.sheet_id)

    assert_that(
        got,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_not_found(storage):
    with pytest.raises(CashbackUserSheet.DoesNotExist):
        await storage.cashback_user_sheet.get(1, 2)


@pytest.mark.asyncio
async def test_save(storage, customer):
    cashback_user_sheet = CashbackUserSheet(
        uid=customer.uid,
        currency='XTS',
        period_start=datetime(2021, 3, 1, 3, 0, 0, tzinfo=timezone.utc),
        period_end=datetime(2021, 4, 1, 3, 0, 0, tzinfo=timezone.utc),
        spent=Decimal('10'),
        spending_limit=Decimal('100'),
    )
    created = await storage.cashback_user_sheet.create(cashback_user_sheet)
    created.spent += 10
    created.spending_limit += 5
    created.period_start += timedelta(minutes=1)
    created.period_end += timedelta(minutes=1)

    saved = await storage.cashback_user_sheet.save(created)

    created.updated = saved.updated
    assert_that(
        saved,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_create_with_same_uid_currency_period_start__raises_unique_violation(
    storage,
    customer,
    make_cashback_user_sheet,
):
    cashback_user_sheet = make_cashback_user_sheet()
    await storage.cashback_user_sheet.create(cashback_user_sheet)

    with pytest.raises(psycopg2.errors.UniqueViolation):
        await storage.cashback_user_sheet.create(cashback_user_sheet)


class TestGetForDateTime:
    @pytest.mark.asyncio
    async def test_returns_sheet(self, storage, customer, make_cashback_user_sheet):
        cashback_user_sheet = make_cashback_user_sheet(
            period_start=utcnow() - timedelta(days=1), period_end=utcnow() + timedelta(days=30)
        )
        cashback_user_sheet = await storage.cashback_user_sheet.create(cashback_user_sheet)

        found = await storage.cashback_user_sheet.get_for_datetime(
            uid=cashback_user_sheet.uid,
            currency=cashback_user_sheet.currency,
            for_datetime=utcnow(),
        )

        assert_that(
            (found.uid, found.sheet_id),
            equal_to((cashback_user_sheet.uid, cashback_user_sheet.sheet_id))
        )

    @pytest.mark.asyncio
    async def test_does_not_return_too_old_sheet(self, storage, customer, make_cashback_user_sheet):
        cashback_user_sheet = make_cashback_user_sheet(
            period_start=utcnow() - timedelta(days=30), period_end=utcnow() - timedelta(days=1)
        )
        cashback_user_sheet = await storage.cashback_user_sheet.create(cashback_user_sheet)

        with pytest.raises(CashbackUserSheet.DoesNotExist):
            await storage.cashback_user_sheet.get_for_datetime(
                uid=cashback_user_sheet.uid,
                currency=cashback_user_sheet.currency,
                for_datetime=utcnow(),
            )

    @pytest.mark.asyncio
    async def test_does_not_return_too_new_sheet(self, storage, customer, make_cashback_user_sheet):
        cashback_user_sheet = make_cashback_user_sheet(
            period_start=utcnow() + timedelta(days=1), period_end=utcnow() + timedelta(days=30)
        )
        cashback_user_sheet = await storage.cashback_user_sheet.create(cashback_user_sheet)

        with pytest.raises(CashbackUserSheet.DoesNotExist):
            await storage.cashback_user_sheet.get_for_datetime(
                uid=cashback_user_sheet.uid,
                currency=cashback_user_sheet.currency,
                for_datetime=utcnow(),
            )


@pytest.fixture
async def customer(storage):
    return await storage.customer.create(Customer(uid=1400))


@pytest.fixture(autouse=True)
async def customer_serial(storage, customer):
    return await storage.customer_serial.create(CustomerSerial(uid=customer.uid, sheet_id=10))


@pytest.fixture
def make_cashback_user_sheet(storage, customer):
    def _make_cashback_sheet(**kwargs):
        cashback_sheet = CashbackUserSheet(
            uid=customer.uid,
            currency='XTS',
            period_start=datetime(2021, 3, 1, 3, 0, 0, tzinfo=timezone.utc),
            period_end=datetime(2021, 4, 1, 3, 0, 0, tzinfo=timezone.utc),
            spent=Decimal('10'),
            spending_limit=Decimal('100'),
        )
        for key in kwargs:
            setattr(cashback_sheet, key, kwargs[key])
        return cashback_sheet
    return _make_cashback_sheet
