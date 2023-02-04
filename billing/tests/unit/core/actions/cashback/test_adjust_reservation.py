import logging
from datetime import timedelta
from decimal import Decimal
from uuid import uuid4

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, contains, equal_to, has_entries, has_properties

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.cashback.adjust_reservation import (
    AdjustCashbackReservationAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.cashback.create_reservation import (
    CreateCashbackReservationAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.customer import CreateCustomerAction
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import ReservationNotFoundError
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_budget import CashbackBudget
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_card_sheet import CashbackCardSheet
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_reservation import CashbackReservation
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_user_sheet import CashbackUserSheet

AMOUNT = Decimal('567')


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
async def card_sheet(storage, trust_card_id):
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


@pytest.fixture
async def reservation(customer, budget, user_sheet, card_sheet):
    return await CreateCashbackReservationAction(
        uid=customer.uid,
        budget=budget,
        cashback_user_sheet=user_sheet,
        cashback_card_sheet=card_sheet,
        cashback_amount=AMOUNT,
    ).run()


@pytest.mark.asyncio
async def test_adjust_reservation_succeeds(
    budget: CashbackBudget, user_sheet, card_sheet, reservation: CashbackReservation
):
    delta = Decimal('123')

    adjusted = await AdjustCashbackReservationAction(
        reservation.uid, reservation.reservation_id, delta
    ).run()

    assert_that(
        adjusted,
        has_properties(
            uid=reservation.uid,
            reservation_id=reservation.reservation_id,
            budget_id=budget.budget_id,
            cashback_user_sheet_id=user_sheet.sheet_id,
            cashback_card_sheet_id=card_sheet.sheet_id,
            amount=AMOUNT - delta,
        )
    )


@pytest.mark.asyncio
async def test_adjust_reservation_succeeds_without_card_sheet(
    customer, budget: CashbackBudget, user_sheet
):
    reservation = await CreateCashbackReservationAction(
        uid=customer.uid,
        budget=budget,
        cashback_user_sheet=user_sheet,
        cashback_amount=AMOUNT,
    ).run()
    delta = Decimal('123')

    adjusted = await AdjustCashbackReservationAction(
        reservation.uid, reservation.reservation_id, delta
    ).run()

    assert_that(
        adjusted,
        has_properties(
            uid=reservation.uid,
            reservation_id=reservation.reservation_id,
            budget_id=budget.budget_id,
            cashback_user_sheet_id=user_sheet.sheet_id,
            cashback_card_sheet_id=None,
            amount=AMOUNT - delta,
        )
    )


@pytest.mark.asyncio
async def test_adjust_reservation_updates_budget_and_sheets(
    reservation: CashbackReservation, storage
):
    delta = Decimal('123')
    budget = await storage.cashback_budget.get(reservation.budget_id)
    user_sheet = await storage.cashback_user_sheet.get(
        reservation.uid, reservation.cashback_user_sheet_id
    )
    card_sheet = await storage.cashback_card_sheet.get(
        reservation.cashback_card_sheet_id
    )
    budget_unspent_before = budget.get_unspent()
    user_sheet_unspent_before = user_sheet.get_unspent()
    card_sheet_unspent_before = card_sheet.get_unspent()

    await AdjustCashbackReservationAction(
        reservation.uid, reservation.reservation_id, delta
    ).run()

    budget = await storage.cashback_budget.get(budget.budget_id)
    assert_that(budget.get_unspent(), equal_to(budget_unspent_before + delta))

    user_sheet = await storage.cashback_user_sheet.get(
        user_sheet.uid, user_sheet.sheet_id
    )
    assert_that(user_sheet.get_unspent(), equal_to(user_sheet_unspent_before + delta))

    card_sheet = await storage.cashback_card_sheet.get(card_sheet.sheet_id)
    assert_that(card_sheet.get_unspent(), equal_to(card_sheet_unspent_before + delta))


@pytest.mark.asyncio
async def test_adjustment_logged(
    reservation: CashbackReservation, customer, dummy_logger, caplog
):
    caplog.set_level(logging.INFO, logger=dummy_logger.logger.name)
    delta = Decimal('123')
    amount_adjusted = AMOUNT - delta

    await AdjustCashbackReservationAction(
        reservation.uid, reservation.reservation_id, delta
    ).run()

    first, second = [r for r in caplog.records if r.name == dummy_logger.logger.name]
    assert_that(
        first,
        has_properties(
            message='Adjusting reservation',
            levelno=logging.INFO,
            _context=has_entries(
                uid=customer.uid,
                reservation_id=reservation.reservation_id,
            )
        )
    )
    assert_that(
        second,
        has_properties(
            message='Reservation adjusted',
            levelno=logging.INFO,
            _context=has_entries(
                uid=customer.uid,
                reservation_id=reservation.reservation_id,
                budget_before=AMOUNT,
                user_sheet_before=AMOUNT,
                card_sheet_before=AMOUNT,
                reservation_before=AMOUNT,
                budget=amount_adjusted,
                user_sheet=amount_adjusted,
                card_sheet=amount_adjusted,
                reservation=amount_adjusted,
            )
        )
    )


@pytest.mark.asyncio
async def test_adjust_reservation_attempt_to_set_negative_amounts(
    reservation: CashbackReservation, storage, dummy_logger, caplog
):
    caplog.set_level(logging.WARNING, logger=dummy_logger.logger.name)

    await AdjustCashbackReservationAction(
        reservation.uid, reservation.reservation_id, AMOUNT + 1
    ).run()

    budget = await storage.cashback_budget.get(reservation.budget_id)
    assert_that(budget.spent, equal_to(0))

    user_sheet = await storage.cashback_user_sheet.get(
        reservation.uid, reservation.cashback_user_sheet_id
    )
    assert_that(user_sheet.spent, equal_to(0))

    card_sheet = await storage.cashback_card_sheet.get(
        reservation.cashback_card_sheet_id
    )
    assert_that(card_sheet.spent, equal_to(0))

    reservation = await storage.cashback_reservation.get(
        reservation.uid, reservation.reservation_id
    )
    assert_that(reservation.amount, equal_to(0))

    logs = [r for r in caplog.records if r.name == dummy_logger.logger.name]
    assert_that(
        logs,
        contains(
            has_properties(
                message='Attempt to reduce budget spent amount below zero',
                levelno=logging.WARNING,
                _context=has_entries(budget_id=budget.budget_id),
            ),
            has_properties(
                message='Attempt to reduce user sheet spent amount below zero',
                levelno=logging.WARNING,
                _context=has_entries(user_sheet_id=user_sheet.sheet_id),
            ),
            has_properties(
                message='Attempt to reduce card sheet spent amount below zero',
                levelno=logging.WARNING,
                _context=has_entries(card_sheet_id=card_sheet.sheet_id),
            ),
            has_properties(
                message='Attempt to reduce reservation amount below zero',
                levelno=logging.WARNING,
                _context=has_entries(reservation_id=reservation.reservation_id),
            ),
        )
    )


@pytest.mark.asyncio
async def test_reservation_not_found(customer):
    with pytest.raises(ReservationNotFoundError):
        await AdjustCashbackReservationAction(customer.uid, 10, Decimal('123')).run()
