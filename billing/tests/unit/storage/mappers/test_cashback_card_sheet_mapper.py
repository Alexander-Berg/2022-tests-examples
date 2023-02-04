from datetime import datetime, timedelta, timezone
from decimal import Decimal
from uuid import uuid4

import psycopg2.errors
import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_card_sheet import CashbackCardSheet


@pytest.mark.asyncio
async def test_create(storage, make_cashback_card_sheet):
    cashback_card_sheet = make_cashback_card_sheet()

    created = await storage.cashback_card_sheet.create(cashback_card_sheet)

    cashback_card_sheet.sheet_id = created.sheet_id
    cashback_card_sheet.created = created.created
    cashback_card_sheet.updated = created.updated
    assert_that(
        created,
        equal_to(cashback_card_sheet),
    )


@pytest.mark.asyncio
async def test_get(storage, make_cashback_card_sheet):
    cashback_card_sheet = make_cashback_card_sheet()

    created = await storage.cashback_card_sheet.create(cashback_card_sheet)

    got = await storage.cashback_card_sheet.get(created.sheet_id)

    assert_that(
        got,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_not_found(storage):
    with pytest.raises(CashbackCardSheet.DoesNotExist):
        await storage.cashback_card_sheet.get(uuid4())


@pytest.mark.asyncio
async def test_save(storage, trust_card_id):
    cashback_sheet = CashbackCardSheet(
        trust_card_id=trust_card_id,
        currency='XTS',
        period_start=datetime(2021, 3, 1, 3, 0, 0, tzinfo=timezone.utc),
        period_end=datetime(2021, 4, 1, 3, 0, 0, tzinfo=timezone.utc),
        spent=Decimal('10'),
        spending_limit=Decimal('100'),
    )
    created = await storage.cashback_card_sheet.create(cashback_sheet)
    created.spent += 10
    created.spending_limit += 5
    created.period_start += timedelta(minutes=1)
    created.period_end += timedelta(minutes=1)

    saved = await storage.cashback_card_sheet.save(created)

    created.updated = saved.updated
    assert_that(
        saved,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_create_with_same_card_id_currency_period_start__raises_unique_violation(
    storage,
    make_cashback_card_sheet,
):
    cashback_card_sheet = make_cashback_card_sheet()
    await storage.cashback_card_sheet.create(cashback_card_sheet)

    with pytest.raises(psycopg2.errors.UniqueViolation):
        await storage.cashback_card_sheet.create(cashback_card_sheet)


class TestGetForDateTime:
    @pytest.mark.asyncio
    async def test_returns_sheet(self, storage, trust_card_id, make_cashback_card_sheet):
        cashback_card_sheet = make_cashback_card_sheet(
            period_start=utcnow() - timedelta(days=1),
            period_end=utcnow() + timedelta(days=30),
        )
        cashback_card_sheet = await storage.cashback_card_sheet.create(cashback_card_sheet)

        found = await storage.cashback_card_sheet.get_for_datetime(
            trust_card_id=trust_card_id,
            currency=cashback_card_sheet.currency,
            for_datetime=utcnow(),
        )

        assert_that(found.sheet_id, equal_to(cashback_card_sheet.sheet_id))

    @pytest.mark.asyncio
    async def test_does_not_return_too_old_sheet(self, storage, trust_card_id, make_cashback_card_sheet):
        cashback_card_sheet = make_cashback_card_sheet(
            period_start=utcnow() - timedelta(days=30),
            period_end=utcnow() - timedelta(days=1),
        )
        cashback_card_sheet = await storage.cashback_card_sheet.create(cashback_card_sheet)

        with pytest.raises(CashbackCardSheet.DoesNotExist):
            await storage.cashback_card_sheet.get_for_datetime(
                trust_card_id=cashback_card_sheet.trust_card_id,
                currency=cashback_card_sheet.currency,
                for_datetime=utcnow(),
            )

    @pytest.mark.asyncio
    async def test_does_not_return_too_new_sheet(
        self, storage, trust_card_id, make_cashback_card_sheet
    ):
        cashback_card_sheet = make_cashback_card_sheet(
            period_start=utcnow() + timedelta(days=1),
            period_end=utcnow() + timedelta(days=30),
        )
        cashback_card_sheet = await storage.cashback_card_sheet.create(cashback_card_sheet)

        with pytest.raises(CashbackCardSheet.DoesNotExist):
            await storage.cashback_card_sheet.get_for_datetime(
                trust_card_id=cashback_card_sheet.trust_card_id,
                currency=cashback_card_sheet.currency,
                for_datetime=utcnow(),
            )


@pytest.fixture
async def trust_card_id(rands):
    return f'card-x{rands()}'


@pytest.fixture
def make_cashback_card_sheet(storage, trust_card_id):
    def _make_cashback_sheet(**kwargs):
        cashback_sheet = CashbackCardSheet(
            trust_card_id=trust_card_id,
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
