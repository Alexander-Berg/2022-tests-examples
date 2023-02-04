from datetime import timedelta
from decimal import Decimal
from uuid import uuid4

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to, has_properties

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.cashback.create_reservation import (
    CreateCashbackReservationAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.customer import CreateCustomerAction
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import InsufficientFundsError
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_budget import CashbackBudget
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_card_sheet import CashbackCardSheet
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_user_sheet import CashbackUserSheet


@pytest.fixture
async def customer():
    return await CreateCustomerAction(uid=456).run()


@pytest.fixture
async def trust_card_id(rands):
    return f'card-x{rands()}'


@pytest.fixture
async def budget(storage):
    return await storage.cashback_budget.create(
        CashbackBudget(
            budget_id=uuid4(),
            currency='XTS',
            spent=Decimal('0'),
            spending_limit=Decimal('100000'),
            period_start=utcnow() - timedelta(days=1),
            period_end=utcnow() + timedelta(days=30),
        )
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
async def card_sheet(storage, customer, trust_card_id):
    return await storage.cashback_card_sheet.create(
        CashbackCardSheet(
            trust_card_id=trust_card_id,
            currency='XTS',
            spent=Decimal('0'),
            spending_limit=Decimal('100000'),
            period_start=utcnow() - timedelta(days=1),
            period_end=utcnow() + timedelta(days=10)
        ),
    )


@pytest.mark.asyncio
async def test_reservation_creation_succeeds(
    customer,
    budget: CashbackBudget,
    user_sheet: CashbackUserSheet,
    card_sheet: CashbackCardSheet,
    storage,
):
    amount = Decimal('777')
    reservation = await CreateCashbackReservationAction(
        uid=customer.uid,
        budget=budget,
        cashback_user_sheet=user_sheet,
        cashback_card_sheet=card_sheet,
        cashback_amount=amount,
    ).run()

    loaded = await storage.cashback_reservation.get(customer.uid, reservation.reservation_id)
    assert_that(
        loaded,
        has_properties(
            budget_id=budget.budget_id,
            cashback_user_sheet_id=user_sheet.sheet_id,
            cashback_card_sheet_id=card_sheet.sheet_id,
            amount=amount,
        )
    )


@pytest.mark.asyncio
async def test_reservation_creation_succeeds_without_card_sheet(
    customer,
    budget: CashbackBudget,
    user_sheet: CashbackUserSheet,
    storage,
):
    amount = Decimal('777')
    reservation = await CreateCashbackReservationAction(
        uid=customer.uid,
        budget=budget,
        cashback_user_sheet=user_sheet,
        cashback_amount=amount,
    ).run()

    loaded = await storage.cashback_reservation.get(customer.uid, reservation.reservation_id)
    assert_that(
        loaded,
        has_properties(
            budget_id=budget.budget_id,
            cashback_user_sheet_id=user_sheet.sheet_id,
            cashback_card_sheet_id=None,
            amount=amount,
        )
    )


@pytest.mark.asyncio
async def test_reservation_updates_budget_and_sheets(
    customer, budget: CashbackBudget, user_sheet, card_sheet, storage
):
    budget_unspent_before = budget.get_unspent()
    user_sheet_unspent_before = user_sheet.get_unspent()
    card_sheet_unspent_before = card_sheet.get_unspent()

    amount = Decimal('777')
    await CreateCashbackReservationAction(
        uid=customer.uid,
        budget=budget,
        cashback_user_sheet=user_sheet,
        cashback_card_sheet=card_sheet,
        cashback_amount=amount,
    ).run()

    budget = await storage.cashback_budget.get(budget.budget_id)
    user_sheet = await storage.cashback_user_sheet.get(customer.uid, user_sheet.sheet_id)
    card_sheet = await storage.cashback_card_sheet.get(card_sheet.sheet_id)

    assert_that(budget.get_unspent(), equal_to(budget_unspent_before - amount))
    assert_that(user_sheet.get_unspent(), equal_to(user_sheet_unspent_before - amount))
    assert_that(card_sheet.get_unspent(), equal_to(card_sheet_unspent_before - amount))


@pytest.mark.asyncio
async def test_reservation_creation_fails_if_insufficient_budget_unspent_amount_(
    customer, budget: CashbackBudget, user_sheet, card_sheet, storage
):
    amount = Decimal('777')
    budget.spending_limit = amount - 1
    budget = await storage.cashback_budget.save(budget)

    with pytest.raises(InsufficientFundsError):
        await CreateCashbackReservationAction(
            uid=customer.uid,
            budget=budget,
            cashback_user_sheet=user_sheet,
            cashback_card_sheet=card_sheet,
            cashback_amount=amount,
        ).run()


@pytest.mark.asyncio
async def test_reservation_creation_fails_if_insufficient_user_sheet_unspent_amount(
    customer, budget: CashbackBudget, user_sheet, card_sheet, storage
):
    amount = Decimal('777')
    user_sheet.spending_limit = amount - 1
    user_sheet = await storage.cashback_user_sheet.save(user_sheet)

    with pytest.raises(InsufficientFundsError):
        await CreateCashbackReservationAction(
            uid=customer.uid,
            budget=budget,
            cashback_user_sheet=user_sheet,
            cashback_card_sheet=card_sheet,
            cashback_amount=amount,
        ).run()


@pytest.mark.asyncio
async def test_reservation_creation_fails_if_insufficient_card_sheet_unspent_amount(
    customer, budget: CashbackBudget, user_sheet, card_sheet, storage
):
    amount = Decimal('777')
    card_sheet.spending_limit = amount - 1
    card_sheet = await storage.cashback_card_sheet.save(card_sheet)

    with pytest.raises(InsufficientFundsError):
        await CreateCashbackReservationAction(
            uid=customer.uid,
            budget=budget,
            cashback_user_sheet=user_sheet,
            cashback_card_sheet=card_sheet,
            cashback_amount=amount,
        ).run()
