import uuid
from datetime import datetime, timedelta, timezone
from decimal import Decimal

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_budget import CashbackBudget


@pytest.mark.asyncio
async def test_create(storage, make_cashback_budget):
    cashback_budget = make_cashback_budget()

    created = await storage.cashback_budget.create(cashback_budget)

    cashback_budget.created = created.created
    cashback_budget.updated = created.updated
    assert_that(
        created,
        equal_to(cashback_budget),
    )


@pytest.mark.asyncio
async def test_get(storage, make_cashback_budget):
    cashback_budget = make_cashback_budget()

    created = await storage.cashback_budget.create(cashback_budget)

    got = await storage.cashback_budget.get(created.budget_id)

    assert_that(
        got,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_not_found(storage):
    with pytest.raises(CashbackBudget.DoesNotExist):
        await storage.cashback_budget.get(uuid.uuid4())


@pytest.mark.asyncio
async def test_save(storage):
    cashback_budget = CashbackBudget(
        budget_id=uuid.uuid4(),
        currency='XTS',
        spent=Decimal('0'),
        spending_limit=Decimal('1000'),
        period_start=datetime(2021, 3, 1, 3, 0, 0, tzinfo=timezone.utc),
        period_end=datetime(2021, 4, 1, 3, 0, 0, tzinfo=timezone.utc),
    )
    created = await storage.cashback_budget.create(cashback_budget)
    created.spent += 100
    created.spending_limit += 10
    created.period_start += timedelta(minutes=1)
    created.period_end += timedelta(minutes=1)

    saved = await storage.cashback_budget.save(created)

    created.updated = saved.updated
    assert_that(
        saved,
        equal_to(created),
    )


class TestFindRichest:
    @pytest.mark.asyncio
    async def test_finds_richest(self, storage, make_cashback_budget):
        await storage.cashback_budget.create(
            make_cashback_budget(
                period_start=utcnow() - timedelta(days=1),
                period_end=utcnow() + timedelta(days=30),
                spent=Decimal('0'),
                spending_limit=Decimal('1000'),
            )
        )
        rich_budget = await storage.cashback_budget.create(
            make_cashback_budget(
                period_start=utcnow() - timedelta(days=1),
                period_end=utcnow() + timedelta(days=30),
                spent=Decimal('900'),
                spending_limit=Decimal('2000'),
            )
        )

        found = await storage.cashback_budget.find_richest('XTS')

        assert_that(
            found.budget_id,
            equal_to(rich_budget.budget_id),
        )

    @pytest.mark.asyncio
    async def test_does_not_find_too_old_budget(self, storage, make_cashback_budget):
        await storage.cashback_budget.create(
            make_cashback_budget(
                period_start=utcnow() - timedelta(days=30),
                period_end=utcnow() - timedelta(days=1),
                spent=Decimal('0'),
                spending_limit=Decimal('1000000'),
            )
        )

        with pytest.raises(CashbackBudget.DoesNotExist):
            await storage.cashback_budget.find_richest('XTS')

    @pytest.mark.asyncio
    async def test_does_not_find_too_new_budget(self, storage, make_cashback_budget):
        await storage.cashback_budget.create(
            make_cashback_budget(
                period_start=utcnow() + timedelta(days=1),
                period_end=utcnow() + timedelta(days=30),
                spent=Decimal('0'),
                spending_limit=Decimal('1000000'),
            )
        )

        with pytest.raises(CashbackBudget.DoesNotExist):
            await storage.cashback_budget.find_richest('XTS')

    @pytest.mark.asyncio
    async def test_not_found(self, storage):
        with pytest.raises(CashbackBudget.DoesNotExist):
            await storage.cashback_budget.find_richest('XTS')


@pytest.fixture
def make_cashback_budget(storage):
    def _make_cashback_budget(**kwargs):
        cashback_budget = CashbackBudget(
            budget_id=uuid.uuid4(),
            currency='XTS',
            spent=Decimal('100'),
            spending_limit=Decimal('200'),
            period_start=datetime(2021, 3, 1, 3, 0, 0, tzinfo=timezone.utc),
            period_end=datetime(2021, 4, 1, 3, 0, 0, tzinfo=timezone.utc),
        )
        for key in kwargs:
            setattr(cashback_budget, key, kwargs[key])
        return cashback_budget
    return _make_cashback_budget
