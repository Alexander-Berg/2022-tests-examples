import logging
from datetime import timedelta
from decimal import Decimal
from uuid import uuid4

import pytest

from sendr_taskqueue.worker.storage.db.entities import TaskState
from sendr_utils import alist, utcnow

from hamcrest import assert_that, equal_to, has_entries, has_items, has_properties

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.cashback.create_reservation import (
    CreateCashbackReservationAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.cashback.revert_reservation import (
    RevertCashbackReservationAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.customer import CreateCustomerAction
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_account import CashbackAccount
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_budget import CashbackBudget
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_card_sheet import CashbackCardSheet
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_reservation import CashbackReservation
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_transaction import CashbackTransaction
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_user_sheet import CashbackUserSheet
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import (
    CashbackTransactionStatus,
    ClassicOrderStatus,
    PaymentMethodType,
    TaskType,
)
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.order import Order

ORDER_AMOUNT = Decimal('1000')
CASHBACK_AMOUNT = Decimal('30')
CASHBACK_CATEGORY = Decimal('0.03')


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
        cashback_amount=CASHBACK_AMOUNT,
    ).run()


@pytest.fixture
async def order(storage, customer, reservation, rands, trust_card_id):
    return await storage.order.create(
        Order(
            uid=customer.uid,
            message_id=rands(),
            currency='XTS',
            amount=ORDER_AMOUNT,
            cashback=reservation.amount,
            cashback_category=CASHBACK_CATEGORY,
            psp_id=uuid4(),
            merchant_id=uuid4(),
            status=ClassicOrderStatus.SUCCESS,
            reservation_id=reservation.reservation_id,
            trust_card_id=trust_card_id,
            payment_method_type=PaymentMethodType.CARD,
        )
    )


class TestRevertCashbackReservationActionAsync:
    def test_serialize(self):
        action = RevertCashbackReservationAction(uid=123, order_id=456)
        assert_that(
            RevertCashbackReservationAction.serialize_kwargs(action._init_kwargs),
            equal_to({'uid': 123, 'order_id': 456})
        )

    def test_deserialize(self):
        action = RevertCashbackReservationAction(uid=123, order_id=456)
        serialized = RevertCashbackReservationAction.serialize_kwargs(action._init_kwargs)
        deserialized = RevertCashbackReservationAction.deserialize_kwargs(serialized)

        action_from_serialized = RevertCashbackReservationAction(**deserialized)

        assert_that(action._init_kwargs, equal_to(action_from_serialized._init_kwargs))

    @pytest.mark.asyncio
    async def test_run_async_with_run_at(self, storage):
        run_at = utcnow() + timedelta(days=7)

        action = RevertCashbackReservationAction(uid=123, order_id=456)
        async with action.storage_setter(transact=action.transact):
            await action.run_async(run_at=run_at)

        filters = {'action_name': 'revert_cashback_reservation'}
        [task] = await alist(storage.task.find(filters=filters))
        assert_that(
            task,
            has_properties(
                params=has_entries(
                    action_kwargs={"uid": 123, "order_id": 456}
                ),
                task_type=TaskType.RUN_ACTION,
                state=TaskState.PENDING,
                run_at=run_at,
            )
        )


@pytest.mark.asyncio
async def test_revert_reservation_succeeds(
    order: Order, reservation: CashbackReservation, storage
):
    await storage.cashback_reservation.get(reservation.uid, reservation.reservation_id)

    await RevertCashbackReservationAction(order.uid, order.order_id).run()

    with pytest.raises(CashbackReservation.DoesNotExist):
        await storage.cashback_reservation.get(reservation.uid, reservation.reservation_id)


@pytest.mark.asyncio
async def test_revert_reservation_succeeds_without_card_sheet(
    order: Order, reservation: CashbackReservation, storage
):
    card_sheet = await storage.cashback_card_sheet.get(reservation.cashback_card_sheet_id)
    reservation.cashback_card_sheet_id = None
    await storage.cashback_reservation.save(reservation)

    await RevertCashbackReservationAction(order.uid, order.order_id).run()

    with pytest.raises(CashbackReservation.DoesNotExist):
        await storage.cashback_reservation.get(reservation.uid, reservation.reservation_id)

    loaded_card_sheet = await storage.cashback_card_sheet.get(card_sheet.sheet_id)
    assert_that(loaded_card_sheet, equal_to(card_sheet))


@pytest.mark.asyncio
async def test_revert_reservation_updates_budget_and_sheets(
    order: Order, reservation: CashbackReservation, storage
):
    budget = await storage.cashback_budget.get(reservation.budget_id)
    budget_unspent_before = budget.get_unspent()
    user_sheet = await storage.cashback_user_sheet.get(reservation.uid, reservation.cashback_user_sheet_id)
    user_sheet_unspent_before = user_sheet.get_unspent()
    card_sheet = await storage.cashback_card_sheet.get(reservation.cashback_card_sheet_id)
    card_sheet_unspent_before = card_sheet.get_unspent()

    await RevertCashbackReservationAction(order.uid, order.order_id).run()

    budget = await storage.cashback_budget.get(reservation.budget_id)
    assert_that(budget.get_unspent(), equal_to(budget_unspent_before + CASHBACK_AMOUNT))

    user_sheet = await storage.cashback_user_sheet.get(reservation.uid, reservation.cashback_user_sheet_id)
    assert_that(user_sheet.get_unspent(), equal_to(user_sheet_unspent_before + CASHBACK_AMOUNT))

    card_sheet = await storage.cashback_card_sheet.get(reservation.cashback_card_sheet_id)
    assert_that(card_sheet.get_unspent(), equal_to(card_sheet_unspent_before + CASHBACK_AMOUNT))


@pytest.mark.asyncio
async def test_revert_reservation_has_no_effect_if_transaction_exists(
    order: Order, reservation: CashbackReservation, storage
):
    account = await storage.cashback_account.create(
        CashbackAccount(
            uid=order.uid,
            currency='XTS',
            trust_account_id='fake_account',
        )
    )
    await storage.cashback_transaction.create(
        CashbackTransaction(
            uid=order.uid,
            order_id=order.order_id,
            account_id=account.account_id,
            trust_purchase_token='fake_token',
            status=CashbackTransactionStatus.CREATED,
            has_plus=True,
        )
    )
    budget = await storage.cashback_budget.get(reservation.budget_id)
    user_sheet = await storage.cashback_user_sheet.get(reservation.uid, reservation.cashback_user_sheet_id)
    card_sheet = await storage.cashback_card_sheet.get(reservation.cashback_card_sheet_id)

    await RevertCashbackReservationAction(order.uid, order.order_id).run()
    loaded_reservation = await storage.cashback_reservation.get(reservation.uid, reservation.reservation_id)
    assert_that(loaded_reservation, equal_to(reservation))

    loaded_budget = await storage.cashback_budget.get(reservation.budget_id)
    assert_that(loaded_budget, equal_to(budget))

    loaded_user_sheet = await storage.cashback_user_sheet.get(
        reservation.uid, reservation.cashback_user_sheet_id
    )
    assert_that(loaded_user_sheet, equal_to(user_sheet))

    loaded_card_sheet = await storage.cashback_card_sheet.get(
        reservation.cashback_card_sheet_id
    )
    assert_that(loaded_card_sheet, equal_to(card_sheet))


@pytest.mark.asyncio
async def test_revert_reservation_has_no_effect_if_order_not_attached_to_reservation(
    order: Order, reservation: CashbackReservation, storage
):
    order.reservation_id = None
    await storage.order.save(order)

    await RevertCashbackReservationAction(order.uid, order.order_id).run()

    loaded_reservation = await storage.cashback_reservation.get(
        reservation.uid, reservation.reservation_id
    )
    assert_that(loaded_reservation, equal_to(reservation))


@pytest.mark.asyncio
async def test_revert_reservation_logged(
    order: Order, reservation: CashbackReservation, caplog, dummy_logger
):
    caplog.set_level(logging.INFO, logger=dummy_logger.logger.name)

    await RevertCashbackReservationAction(order.uid, order.order_id).run()

    logs = [r for r in caplog.records if r.name == dummy_logger.logger.name]
    assert_that(
        logs,
        has_items(
            has_properties(
                message='Cashback reservation reverted',
                levelno=logging.INFO,
                _context=has_entries(
                    uid=order.uid,
                    order_id=order.order_id,
                    reservation_id=reservation.reservation_id,
                )
            )
        )
    )
