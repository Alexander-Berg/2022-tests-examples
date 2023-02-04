import uuid
from datetime import timedelta
from decimal import Decimal

import pytest
from pay.lib.entities.payment_sheet import PaymentMerchant as Merchant

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to, has_properties, is_

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.card_sheet.get_unspent import (
    GetCashbackCardSheetUnspentAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.cashback.category import GetCashbackCategoryAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.cashback.get import GetCashbackAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.user_sheet.get_unspent import (
    GetCashbackUserSheetUnspentAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.entities.cashback import Cashback
from billing.yandex_pay_plus.yandex_pay_plus.core.entities.partner import _ANY, CashbackPartnerEntry
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_budget import CashbackBudget

MERCHANT_ID = uuid.uuid4()
PSP_ID = uuid.uuid4()

DEFAULT_PARAMS = {
    'amount': Decimal('100'),
    'per_order_limit': {'XTS': 200},
    'uid': 1,
    'merchant': Merchant(
        id=MERCHANT_ID,
        name='the-name',
        url='https://url.test'
    ),
    'psp_id': PSP_ID,
    'budget_limit': Decimal('100000'),
    'user_sheet_limit': Decimal('1000'),
    'card_sheet_limit': Decimal('3000'),
    'force_category': None,
    'enable_partner_restrictions': True,
    'cashback_category_id': None,
}


partner_entry_matches_default_params = pytest.mark.parametrize('partner_entry', (
    pytest.param(CashbackPartnerEntry.create(psp_id=PSP_ID), id='by-psp-id'),
    pytest.param(CashbackPartnerEntry.create(merchant_id=MERCHANT_ID), id='by-merchant-id'),
    pytest.param(CashbackPartnerEntry.create(merchant_url='https://url.test'), id='by-merchant-url'),
    pytest.param(CashbackPartnerEntry.create(merchant_name='the-name'), id='by-merchant-name'),
))


partner_entry_does_not_match_default_params = pytest.mark.parametrize('partner_entry', (
    pytest.param(CashbackPartnerEntry.create(psp_id=uuid.uuid4()), id='by-psp-id'),
    pytest.param(CashbackPartnerEntry.create(merchant_id=uuid.uuid4()), id='by-merchant-id'),
    pytest.param(CashbackPartnerEntry.create(merchant_url='https://blacklist.test'), id='by-merchant-url'),
    pytest.param(CashbackPartnerEntry.create(merchant_name='blacklisted-name'), id='by-merchant-name'),
))


def set_params(params):
    return pytest.mark.parametrize('params', (params,))


def cashback(amount=Decimal('0'), category=Decimal('0'), order_limit=None):
    return Cashback(
        amount=amount,
        category=category,
        order_limit=order_limit if order_limit is not None else amount,
    )


@set_params(DEFAULT_PARAMS)
@pytest.mark.asyncio
async def test_get_cashback(run_action):
    assert_that(
        await run_action(),
        equal_to(cashback(amount=Decimal('10'), category=Decimal('0.1'), order_limit=Decimal('10'))),
    )


@set_params(DEFAULT_PARAMS | {'force_category': Decimal('0.33'), 'cashback_category_id': 'category-id'})
@pytest.mark.asyncio
async def test_calls_get_cashback_category(run_action, mock_get_cashback_category):
    await run_action()

    mock_get_cashback_category.assert_called_once_with(
        force_category=Decimal('0.33'),
        cashback_category_id='category-id',
        merchant=DEFAULT_PARAMS['merchant'],
        psp_id=DEFAULT_PARAMS['psp_id'],
        enable_partner_restrictions=True
    )


@set_params(DEFAULT_PARAMS | {'amount': Decimal('75')})
@pytest.mark.asyncio
async def test_cashback_round_floor(run_action):
    assert_that(
        await run_action(),
        equal_to(cashback(amount=Decimal('7'), category=Decimal('0.1'), order_limit=Decimal('7'))),
    )


@set_params(DEFAULT_PARAMS | {'amount': Decimal('0')})
@pytest.mark.asyncio
async def test_zero_amount(run_action):
    assert_that(
        await run_action(),
        equal_to(cashback(amount=Decimal('0'), category=Decimal('0'), order_limit=Decimal('0'))),
    )


@set_params(
    DEFAULT_PARAMS | {'per_order_limit': {'XTS': '2'}}
)
@pytest.mark.asyncio
async def test_per_order_limit_prevails(run_action):
    assert_that(
        await run_action(),
        equal_to(cashback(amount=Decimal('2'), category=Decimal('0.1'), order_limit=Decimal('2'))),
    )


@set_params(DEFAULT_PARAMS | {'per_order_limit': {}})
@pytest.mark.asyncio
async def test_no_per_order_limit(run_action):
    assert_that(
        await run_action(),
        equal_to(cashback(amount=Decimal('10'), category=Decimal('0.1'), order_limit=Decimal('10'))),
    )


@set_params(DEFAULT_PARAMS | {'user_sheet_limit': Decimal('2')})
@pytest.mark.asyncio
async def test_user_sheet_limit_prevails(run_action):
    assert_that(
        await run_action(),
        equal_to(cashback(amount=Decimal('2'), category=Decimal('0.1'), order_limit=Decimal('10'))),
    )


@set_params(DEFAULT_PARAMS | {'card_sheet_limit': Decimal('3')})
@pytest.mark.asyncio
async def test_card_sheet_limit_prevails(run_action):
    assert_that(
        await run_action(),
        equal_to(cashback(amount=Decimal('3'), category=Decimal('0.1'), order_limit=Decimal('10'))),
    )


@set_params(DEFAULT_PARAMS | {'budget_limit': Decimal('2')})
@pytest.mark.asyncio
async def test_budget_limit_prevails(run_action):
    assert_that(
        await run_action(),
        equal_to(cashback(amount=Decimal('2'), category=Decimal('0.1'), order_limit=Decimal('2'))),
    )


@set_params(DEFAULT_PARAMS)
@pytest.mark.asyncio
async def test_no_budget(run_action, storage, budget):
    await storage.cashback_budget.delete(budget)

    assert_that(
        await run_action(),
        equal_to(cashback(amount=Decimal('0'), category=Decimal('0'), order_limit=Decimal('0'))),
    )


@set_params(DEFAULT_PARAMS | {'user_sheet_limit': Decimal('0')})
@pytest.mark.asyncio
async def test_limit_is_zero__category_should_be_zero(run_action):
    assert_that(
        await run_action(),
        equal_to(cashback(amount=Decimal('0'), category=Decimal('0'), order_limit=Decimal('10'))),
    )


@set_params(DEFAULT_PARAMS)
@pytest.mark.asyncio
async def test_cashback_uid_whitelist_enabled__uid_ok(run_action, yandex_pay_plus_settings):
    yandex_pay_plus_settings.CASHBACK_UID_WHITELIST_ENABLED = True
    yandex_pay_plus_settings.CASHBACK_UID_WHITELIST = (1, 2)
    assert_that(
        await run_action(),
        equal_to(cashback(amount=Decimal('10'), category=Decimal('0.1'), order_limit=Decimal('10'))),
    )


@set_params(DEFAULT_PARAMS)
@pytest.mark.asyncio
async def test_cashback_uid_whitelist_enabled__uid_bad(run_action, yandex_pay_plus_settings):
    yandex_pay_plus_settings.CASHBACK_UID_WHITELIST_ENABLED = True
    yandex_pay_plus_settings.CASHBACK_UID_WHITELIST = (3, 4)
    assert_that(
        await run_action(),
        equal_to(cashback(amount=Decimal('0'), category=Decimal('0'), order_limit=Decimal('0'))),
    )


@set_params(DEFAULT_PARAMS)
def test_create_partner_entry():
    config_entry = {'psp_id': 'a9dfa979-d270-49aa-a68b-2a5443a83799', 'merchant_url': None}
    assert_that(
        CashbackPartnerEntry.create(**config_entry),
        has_properties(
            psp_id=uuid.UUID(config_entry['psp_id']),
            merchant_id=is_(_ANY),
            merchant_url=None,
        ),
    )

    config_entry = {'merchant_id': 'b74b61d8-0285-4c76-aa3d-7ae296a63fa7'}
    assert_that(
        CashbackPartnerEntry.create(**config_entry),
        has_properties(
            psp_id=is_(_ANY),
            merchant_id=uuid.UUID(config_entry['merchant_id']),
            merchant_url=is_(_ANY),
        ),
    )


@pytest.fixture
def run_action(params):
    async def _run_action():
        return await GetCashbackAction(
            uid=params['uid'],
            currency='XTS',
            amount=params['amount'],
            merchant=params['merchant'],
            psp_id=params['psp_id'],
            force_category=params['force_category'],
            enable_partner_restrictions=params['enable_partner_restrictions'],
            cashback_category_id=params['cashback_category_id'],
        ).run()
    return _run_action


@pytest.fixture(autouse=True)
async def budget(storage, params):
    return await storage.cashback_budget.create(
        CashbackBudget(
            budget_id=uuid.uuid4(),
            currency='XTS',
            spent=Decimal('0'),
            spending_limit=params['budget_limit'],
            period_start=utcnow() - timedelta(days=1),
            period_end=utcnow() + timedelta(days=10),
        )
    )


@pytest.fixture(autouse=True)
def mock_user_sheet_limit(mock_action, params):
    return mock_action(GetCashbackUserSheetUnspentAction, params['user_sheet_limit'])


@pytest.fixture(autouse=True)
def mock_card_sheet_limit(mock_action, params):
    return mock_action(GetCashbackCardSheetUnspentAction, params['card_sheet_limit'])


@pytest.fixture(autouse=True)
def mock_cashback_action_settings(mocker, params, yandex_pay_plus_settings):
    yandex_pay_plus_settings.CASHBACK_UID_WHITELIST_ENABLED = False
    mocker.patch.object(GetCashbackAction, 'per_order_limit', params['per_order_limit'])


@pytest.fixture(autouse=True)
def mock_get_cashback_category(mock_action):
    return mock_action(GetCashbackCategoryAction, Decimal('0.1'))
