import logging
from datetime import timedelta
from decimal import Decimal
from uuid import UUID

import pytest
from pay.lib.entities.payment_sheet import PaymentMerchant as Merchant

from sendr_utils import alist, utcnow

from hamcrest import (
    all_of,
    assert_that,
    equal_to,
    has_entries,
    has_item,
    has_properties,
    instance_of,
    match_equality,
    matches_regexp,
    not_,
    not_none,
)

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.cashback.get import GetCashbackAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.customer import EnsureCustomerAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.order.create import CreateOrderAction
from billing.yandex_pay_plus.yandex_pay_plus.core.cashback_sheet import resolve_sheet_period_for_datetime
from billing.yandex_pay_plus.yandex_pay_plus.core.entities.cashback import Cashback
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import OrderAlreadyExistsError
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_budget import CashbackBudget
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_card_sheet import CashbackCardSheet
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_user_sheet import CashbackUserSheet
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import ClassicOrderStatus, PaymentMethodType
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.order import Order, OrderData


@pytest.mark.asyncio
async def test_creates_order_when_budget_exists(storage, params):
    await CreateOrderAction(**params).run()

    [order] = await alist(storage.order.find(filters={'uid': 20}))
    assert_that(
        order,
        equal_to(
            Order(
                uid=20,
                order_id=match_equality(not_none()),
                status=ClassicOrderStatus.NEW,
                cashback_category=Decimal('0.01'),
                pretty_id=match_equality(matches_regexp(r'^\d{10}\-\d{4}$')),
                message_id='msgid',
                currency='XTS',
                amount=Decimal('100'),
                psp_id=UUID('f4f9b696-6f6f-4673-beb0-d0b70c4052f9'),
                merchant_id=UUID('6031216d-520c-47c4-832e-bfe663991d79'),
                cashback=Decimal('322'),
                trust_card_id='card-x123abc',
                card_id=UUID('8841aef1-1d7d-4f09-af3f-b65239391511'),
                updated=match_equality(not_none()),
                created=match_equality(not_none()),
                data=OrderData(
                    last4='1234',
                    merchant_name='the-name',
                    merchant_url='https://url.test',
                    order_cashback_limit=Decimal('123'),
                    antifraud_external_id='antifraud_id',
                    card_network='visa',
                    country_code='RUS',
                    order_basket={
                        'id': 'item_id',
                    },
                ),
                payment_method_type=PaymentMethodType.CARD,
            )
        )
    )


@pytest.mark.asyncio
async def test_can_create_multiple_orders(params):
    """Тестируем, что запись нескольких заказов в рамках одной транзакции
    не приводит к ошибкам. Т.е. будет одинаковый order.created, и он не ломает
    уникальность pretty_id, например.
    """
    await CreateOrderAction(**params).run()

    params['message_id'] = 'other-message-id'
    await CreateOrderAction(**params).run()


@pytest.mark.asyncio
async def can_generate_pretty_id_on_unsaved_order(params):
    order = await CreateOrderAction(**params).run()
    order.created = None

    pretty_id = CreateOrderAction._generate_pretty_id(order)

    assert_that(pretty_id, matches_regexp(r'^\d{10}\-\d{4}$'))
    assert_that(pretty_id, not_(equal_to(order.pretty_id)))


@pytest.mark.asyncio
@pytest.mark.parametrize('cashback_category_id', [None, '0.1'])
async def test_calls_get_cashback(params, mock_get_cashback, budget, cashback_category_id):
    params['cashback_category_id'] = cashback_category_id
    params['force_category'] = Decimal('0.1')
    await CreateOrderAction(**params).run()

    mock_get_cashback.assert_called_once_with(
        uid=20,
        currency='XTS',
        amount=Decimal('100'),
        psp_id=UUID('f4f9b696-6f6f-4673-beb0-d0b70c4052f9'),
        merchant=params['merchant'],
        budget=budget,
        force_category=Decimal('0.1'),
        cashback_user_sheet=match_equality(
            all_of(
                instance_of(CashbackUserSheet),
                has_properties(
                    uid=20,
                    currency='XTS',
                    spent=Decimal('0'),
                )
            )
        ),
        cashback_card_sheet=match_equality(
            all_of(
                instance_of(CashbackCardSheet),
                has_properties(
                    trust_card_id='card-x123abc',
                    currency='XTS',
                    spent=Decimal('0'),
                )
            )
        ),
        cashback_category_id=cashback_category_id,
    )


@pytest.mark.asyncio
async def test_calls_ensure_customer(storage, params, spy_ensure_customer):
    await CreateOrderAction(
        **params,
    ).run()

    spy_ensure_customer.assert_called_once_with(match_equality(instance_of(EnsureCustomerAction)), uid=20)


@pytest.mark.asyncio
async def test_creates_sheet_if_not_exists(params, storage, budget, dbconn):
    now = await dbconn.scalar('select now()')
    with pytest.raises(CashbackUserSheet.DoesNotExist):
        await storage.cashback_user_sheet.get_for_datetime(
            uid=20, currency='XTS', for_datetime=now,
        )

    order = await CreateOrderAction(**params).run()

    sheet = await storage.cashback_user_sheet.get_for_datetime(
        uid=order.uid, currency=order.currency, for_datetime=order.created
    )
    sheet_period = resolve_sheet_period_for_datetime(order.created)
    spent = Decimal(0) if order.reservation_id is None else order.cashback
    assert_that(
        sheet,
        has_properties(
            spending_limit=Decimal('1000'),
            spent=spent,
            **sheet_period,
        )
    )


@pytest.mark.asyncio
async def test_when_order_data_has_nulls(params, storage):
    params['last4'] = None
    params['merchant'].url = None
    params['antifraud_external_id'] = None
    params['card_network'] = None
    params['country_code'] = None
    params['order_basket'] = None
    await CreateOrderAction(
        **params,
    ).run()

    [order] = await alist(storage.order.find(filters={'uid': 20}))
    assert_that(
        order,
        has_properties({
            'data': OrderData(
                merchant_name='the-name',
                merchant_url=None,
                last4=None,
                order_cashback_limit=Decimal('123'),
                antifraud_external_id=None,
                card_network=None,
                country_code=None,
                order_basket=None,
            ),
        }),
    )


@pytest.mark.asyncio
async def test_create_duplicate_order(storage, params, dummy_logs):
    created = await CreateOrderAction(**params).run()
    duplicate = await CreateOrderAction(**params).run()

    assert_that(duplicate, equal_to(created))
    logs = dummy_logs()
    assert_that(
        logs,
        has_item(
            has_properties(
                message='Duplicate order',
                levelno=logging.WARNING,
                _context=has_entries(
                    uid=params['uid'],
                    message_id=params['message_id'],
                )
            )
        )
    )


@pytest.mark.asyncio
async def test_create_duplicate_order__malformed_user(storage, params, dummy_logs):
    order = await CreateOrderAction(**params).run()

    params['uid'] += 1
    with pytest.raises(OrderAlreadyExistsError):
        await CreateOrderAction(**params).run()

    logs = dummy_logs()
    assert_that(
        logs,
        has_item(
            has_properties(
                message='Order does not belong to user',
                levelno=logging.WARNING,
                _context=has_entries(
                    uid=params['uid'],
                    message_id=params['message_id'],
                    order=order,
                )
            )
        )
    )


@pytest.fixture(autouse=True)
async def set_default_limits(yandex_pay_plus_settings):
    yandex_pay_plus_settings.CASHBACK_USER_SHEET_SPENDING_LIMIT['XTS'] = 1000
    yandex_pay_plus_settings.CASHBACK_CARD_SHEET_SPENDING_LIMIT['XTS'] = 3000


@pytest.fixture(autouse=True)
async def budget(storage):
    return await storage.cashback_budget.create(
        CashbackBudget(
            currency='XTS',
            budget_id=UUID('637b6199-10f5-4ebc-bdab-369340cc6ccb'),
            spent=Decimal('0'),
            spending_limit=Decimal('100000'),
            period_start=utcnow() - timedelta(days=1),
            period_end=utcnow() + timedelta(days=1),
        ),
    )


@pytest.fixture
def params():
    return dict(
        uid=20,
        message_id='msgid',
        currency='XTS',
        amount=Decimal('100'),
        psp_id=UUID('f4f9b696-6f6f-4673-beb0-d0b70c4052f9'),
        merchant=Merchant(
            id=UUID('6031216d-520c-47c4-832e-bfe663991d79'),
            name='the-name',
            url='https://url.test',
        ),
        trust_card_id='card-x123abc',
        cashback_category_id=None,
        card_id=UUID('8841aef1-1d7d-4f09-af3f-b65239391511'),
        last4='1234',
        antifraud_external_id='antifraud_id',
        card_network='visa',
        country_code='RUS',
        order_basket={
            'id': 'item_id',
        },
        payment_method_type=PaymentMethodType.CARD,
    )


@pytest.fixture(autouse=True)
def mock_get_cashback(mock_action):
    return mock_action(
        GetCashbackAction, Cashback(amount=Decimal('322'), category=Decimal('0.01'), order_limit=Decimal('123'))
    )


@pytest.fixture(autouse=True)
def spy_ensure_customer(mocker):
    return mocker.spy(EnsureCustomerAction, '__init__')
