import logging
import uuid
from datetime import timedelta
from decimal import Decimal

import pytest
from pay.lib.entities.payment_sheet import PaymentMerchant as Merchant

from sendr_utils import alist, utcnow

from hamcrest import assert_that, equal_to, greater_than, has_entries, has_properties, not_none
from hamcrest.library.integration import match_equality

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.cashback.account.ensure import EnsureCashbackAccountAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.cashback.create_transaction import (
    CreateCashbackTransactionAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.cashback.get import GetCashbackAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.customer import CreateCustomerAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.order.create import CreateOrderAction
from billing.yandex_pay_plus.yandex_pay_plus.core.entities.cashback import Cashback
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import TransactionAlreadyExistsError
from billing.yandex_pay_plus.yandex_pay_plus.interactions.blackbox import BlackBoxClient
from billing.yandex_pay_plus.yandex_pay_plus.interactions.trust_payments import TrustPaymentsClient
from billing.yandex_pay_plus.yandex_pay_plus.interactions.trust_payments.entities import YandexPlusAccount
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_account import CashbackAccount
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_budget import CashbackBudget
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_card_sheet import CashbackCardSheet
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_transaction import CashbackTransaction
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_user_sheet import CashbackUserSheet
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import (
    CashbackTransactionStatus,
    ClassicOrderStatus,
    PaymentMethodType,
    TaskType,
)


@pytest.fixture(autouse=True)
async def set_default_per_user_limit(yandex_pay_plus_settings):
    yandex_pay_plus_settings.CASHBACK_USER_SHEET_SPENDING_LIMIT['XTS'] = 1000


@pytest.fixture
def trust_account(order):
    return YandexPlusAccount(
        account_id='acc_id',
        uid=order.uid,
        currency=order.currency,
    )


@pytest.fixture
async def budget(storage):
    return await storage.cashback_budget.create(
        CashbackBudget(
            currency='XTS',
            budget_id=uuid.uuid4(),
            spent=Decimal('0'),
            spending_limit=Decimal('100000'),
            period_start=utcnow() - timedelta(days=1),
            period_end=utcnow() + timedelta(days=1),
        ),
    )


@pytest.fixture
async def customer():
    return await CreateCustomerAction(uid=123).run()


@pytest.fixture
async def trust_card_id(rands):
    return f'card-x{rands()}'


@pytest.fixture
async def user_sheet(storage, customer):
    now = utcnow()
    return await storage.cashback_user_sheet.create(
        CashbackUserSheet(
            uid=customer.uid,
            currency='XTS',
            spent=Decimal('0'),
            spending_limit=Decimal('100000'),
            period_start=now - timedelta(days=1),
            period_end=now + timedelta(days=30)
        ),
    )


@pytest.fixture
async def card_sheet(storage, trust_card_id):
    now = utcnow()
    return await storage.cashback_card_sheet.create(
        CashbackCardSheet(
            trust_card_id=trust_card_id,
            currency='XTS',
            spent=Decimal('0'),
            spending_limit=Decimal('100000'),
            period_start=now - timedelta(days=1),
            period_end=now + timedelta(days=30),
        ),
    )


@pytest.fixture
async def order(storage, customer, budget, user_sheet, card_sheet, trust_card_id):
    order = await CreateOrderAction(
        uid=customer.uid,
        message_id='msgid',
        currency='XTS',
        amount=Decimal('1000.0'),
        psp_id=uuid.uuid4(),
        merchant=Merchant(id=uuid.uuid4(), name='test_merchant', url=None),
        trust_card_id=trust_card_id,
        payment_method_type=PaymentMethodType.CARD,
    ).run()
    order.status = ClassicOrderStatus.SUCCESS
    return await storage.order.save(order)


@pytest.fixture
async def account(storage, order, trust_account):
    return await storage.cashback_account.create(
        CashbackAccount(
            uid=order.uid,
            currency=order.currency,
            trust_account_id=trust_account.account_id,
        ),
    )


@pytest.fixture(autouse=True)
def mock_have_plus(mocker):
    return mocker.patch.object(BlackBoxClient, 'have_plus', mocker.AsyncMock(return_value=True))


@pytest.fixture(autouse=True)
def mock_create_plus_account(mocker, trust_account):
    return mocker.patch.object(
        TrustPaymentsClient, 'create_plus_account', mocker.AsyncMock(return_value=trust_account)
    )


@pytest.fixture(autouse=True)
def mock_create_plus_transaction(mocker):
    return mocker.patch.object(
        TrustPaymentsClient,
        'create_plus_transaction',
        mocker.AsyncMock(return_value='purchase_token')
    )


@pytest.fixture
def run_action(order):
    async def _run_action():
        return await CreateCashbackTransactionAction(order.uid, order.order_id).run()

    return _run_action


@pytest.fixture
def billing_payload(yandex_pay_plus_settings):
    return {
        'cashback_service': 'yapay',
        'cashback_type': 'nontransaction',
        'has_plus': 'true',
        'service_id': '1024',
        'issuer': 'marketing',
        'campaign_name': yandex_pay_plus_settings.CASHBACK_TRANSACTION_CAMPAIGN_NAME,
        'ticket': 'NEWSERVICE-1591',
        'product_id': yandex_pay_plus_settings.CASHBACK_TRANSACTION_PRODUCT_ID,
    }


class TestCreateCashbackTransactionActionKwargs:
    def test_serialize(self):
        action = CreateCashbackTransactionAction(uid=123, order_id=456)
        assert_that(
            CreateCashbackTransactionAction.serialize_kwargs(action._init_kwargs),
            equal_to({'uid': 123, 'order_id': 456})
        )

    def test_deserialize(self):
        action = CreateCashbackTransactionAction(uid=123, order_id=456)
        serialized = CreateCashbackTransactionAction.serialize_kwargs(action._init_kwargs)
        deserialized = CreateCashbackTransactionAction.deserialize_kwargs(serialized)

        action_from_serialized = CreateCashbackTransactionAction(**deserialized)

        assert_that(action._init_kwargs, equal_to(action_from_serialized._init_kwargs))


@pytest.mark.asyncio
async def test_ensures_there_is_no_transaction_in_storage_yet(run_action, order, storage, budget, user_sheet, account):
    await storage.cashback_transaction.create(
        CashbackTransaction(
            uid=order.uid,
            order_id=order.order_id,
            account_id=account.account_id,
            trust_purchase_token='pur-cha-se-to-ken',
            status=CashbackTransactionStatus.STARTED,
            has_plus=True,
        )
    )

    with pytest.raises(TransactionAlreadyExistsError):
        await run_action()


@pytest.mark.asyncio
async def test_richest_budget_updated(run_action, order, storage, budget):
    await storage.cashback_budget.create(
        CashbackBudget(
            currency='XTS',
            budget_id=uuid.uuid4(),
            spent=Decimal('0'),
            spending_limit=Decimal('1000'),
            period_start=utcnow() - timedelta(days=1),
            period_end=utcnow() + timedelta(days=1),
        ),
    )

    await run_action()

    assert_that(
        (await storage.cashback_budget.get(budget.budget_id)).spent,
        equal_to(budget.spent + order.cashback)
    )


@pytest.mark.asyncio
async def test_sheet_updated(run_action, order, storage, budget, user_sheet):
    await run_action()

    assert_that(
        (await storage.cashback_user_sheet.get(user_sheet.uid, user_sheet.sheet_id)).spent,
        equal_to(user_sheet.spent + order.cashback)
    )


@pytest.mark.asyncio
async def test_calls_create_plus_transaction(
    run_action,
    order,
    storage,
    mock_create_plus_transaction,
    yandex_pay_plus_settings,
    trust_account,
    budget,
    billing_payload,
):
    await run_action()

    mock_create_plus_transaction.assert_awaited_once_with(
        account=trust_account,
        product_id=yandex_pay_plus_settings.CASHBACK_TRANSACTION_PRODUCT_ID,
        amount=int(order.cashback),
        billing_payload=billing_payload,
    )


@pytest.mark.asyncio
async def test_does_not_create_plus_transaction_if_no_cashback_given(
    run_action, order, storage, mock_create_plus_transaction,
):
    order.cashback = order.cashback_category = Decimal('0')
    order.reservation_id = None
    await storage.order.save(order)

    await run_action()

    mock_create_plus_transaction.assert_not_awaited()


@pytest.mark.asyncio
@pytest.mark.usefixtures('mock_create_plus_transaction')
async def test_creates_transaction(run_action, order, storage, account, billing_payload):
    await run_action()

    transaction = await storage.cashback_transaction.get_by_uid_and_order_id(
        uid=order.uid, order_id=order.order_id
    )
    assert_that(
        transaction,
        has_properties({
            'account_id': account.account_id,
            'trust_purchase_token': 'purchase_token',
            'status': CashbackTransactionStatus.CREATED,
            'has_plus': True,
            'billing_payload': billing_payload,
        })
    )


@pytest.mark.asyncio
@pytest.mark.usefixtures('mock_create_plus_transaction')
@pytest.mark.skip
async def test_creates_reservation_if_not_exists(
    run_action, order, storage, account, card_sheet, user_sheet, budget
):
    cashback_amount = order.cashback
    cashback_category = order.cashback_category
    original_reservation_id = order.reservation_id
    reservation = await storage.cashback_reservation.get(order.uid, order.reservation_id)
    order.reservation_id = None
    await storage.order.save(order)
    await storage.cashback_reservation.delete(reservation)

    await run_action()

    # new reservation should have been created
    order = await storage.order.get(order.uid, order.order_id)
    reservation = await storage.cashback_reservation.get(order.uid, order.reservation_id)
    assert_that(
        order,
        has_properties(
            cashback=cashback_amount,
            cashback_category=cashback_category,
        )
    )
    assert_that(
        reservation,
        has_properties(
            uid=order.uid,
            reservation_id=greater_than(original_reservation_id),
            budget_id=budget.budget_id,
            cashback_card_sheet_id=card_sheet.sheet_id,
            cashback_user_sheet_id=user_sheet.sheet_id,
            amount=cashback_amount,
        )
    )

    card_sheet = await storage.cashback_card_sheet.get(card_sheet.sheet_id)
    # as the first reservation hasn't been property reverted
    # (we just removed it from the db instead),
    # the card sheet should have been 'charged' twice
    assert_that(card_sheet.spent, equal_to(cashback_amount * 2))

    # the same applies to the user sheet
    user_sheet = await storage.cashback_user_sheet.get(user_sheet.uid, user_sheet.sheet_id)
    assert_that(user_sheet.spent, equal_to(cashback_amount * 2))

    # and the budget
    budget = await storage.cashback_budget.get(budget.budget_id)
    assert_that(budget.spent, equal_to(cashback_amount * 2))


@pytest.mark.asyncio
@pytest.mark.skip
async def test_reservation_not_created_if_cashback_is_zero(
    run_action, order, storage, account, card_sheet, user_sheet, budget, mock_create_plus_transaction, mocker
):
    mock_run = mocker.AsyncMock(
        return_value=Cashback(amount=Decimal('0'), category=Decimal('0'), order_limit=Decimal('0'))
    )
    mocker.patch.object(GetCashbackAction, 'run', mock_run)

    reservation = await storage.cashback_reservation.get(order.uid, order.reservation_id)
    order.reservation_id = None
    await storage.order.save(order)
    await storage.cashback_reservation.delete(reservation)

    await run_action()

    mock_create_plus_transaction.assert_not_awaited()
    order = await storage.order.get(order.uid, order.order_id)
    assert_that(
        order,
        has_properties(
            cashback=Decimal('0'),
            cashback_category=Decimal('0'),
            reservation_id=None,
        )
    )
    assert_that(await alist(storage.cashback_reservation.find()), equal_to([]))


@pytest.mark.asyncio
@pytest.mark.usefixtures('mock_create_plus_transaction')
async def test_create_transaction_logged(
    run_action, order, storage, account, billing_payload, caplog, dummy_logger
):
    caplog.set_level(logging.INFO, logger=dummy_logger.logger.name)

    await run_action()

    [*_, log] = [r for r in caplog.records if r.name == dummy_logger.logger.name]
    assert_that(
        log,
        has_properties(
            message='Transaction created',
            levelno=logging.INFO,
            _context=has_entries(
                uid=order.uid,
                order_id=order.order_id,
                account_id=account.account_id,
                has_plus=True,
                trust_purchase_token='purchase_token',
                billing_payload=billing_payload,
            ),
        )
    )


@pytest.mark.asyncio
async def test_creates_start_transaction_task(run_action, order, storage, budget):
    await run_action()

    transaction = await storage.cashback_transaction.get_by_uid_and_order_id(
        uid=order.uid, order_id=order.order_id
    )
    filters = {'action_name': 'start_cashback_transaction'}
    [task] = await alist(storage.task.find(filters=filters))
    assert_that(
        task,
        has_properties({
            'task_type': TaskType.RUN_ACTION,
            'params': has_entries({
                'action_kwargs': {
                    'uid': transaction.uid,
                    'transaction_id': transaction.transaction_id,
                }
            }),
        })
    )


@pytest.mark.asyncio
async def test_should_call_ensure_cashback_account_action_with_expected_args(run_action, order, mocker):
    spy = mocker.spy(EnsureCashbackAccountAction, '__init__')

    await run_action()

    spy.assert_called_once_with(match_equality(not_none()), uid=order.uid, currency=order.currency)
