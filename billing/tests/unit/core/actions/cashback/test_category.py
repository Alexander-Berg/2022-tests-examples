import uuid
from dataclasses import replace
from datetime import timedelta
from decimal import Decimal

import pytest
from pay.lib.entities.payment_sheet import PaymentMerchant as Merchant

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.cashback.category import GetCashbackCategoryAction
from billing.yandex_pay_plus.yandex_pay_plus.core.entities.partner import CashbackPartnerEntry

PSP_EXP_WHITELISTED = uuid.uuid4()
PSP_EXP_NOT_WHITELISTED = uuid.uuid4()
ANY_PSP_ID = uuid.uuid4()
PSP_WITH_DEFAULT_CASHBACK = uuid.uuid4()
MERCHANT_ID_WITH_DEFAULT_CASHBACK = uuid.uuid4()
MERCHANT_URL_WITH_DEFAULT_CASHBACK = 'https://cashback.please.test'
MERCHANT_NAME_WITH_DEFAULT_CASHBACK = 'the-cashback'
CUSTOM_DEFAULT_CATEGORY = Decimal('0.07')
ANY_MERCHANT = Merchant(id=uuid.uuid4(), name='name', url='http://url.test')
DEFAULT_CATEGORY = Decimal('0.2')
EXPERIMENT_CATEGORY_ID = '0.15'

MERCHANT_ID = uuid.uuid4()
PSP_ID = uuid.uuid4()

DEFAULT_PARAMS = {
    'merchant': Merchant(
        id=MERCHANT_ID,
        name='the-name',
        url='https://url.test'
    ),
    'psp_id': PSP_ID,
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


@pytest.mark.asyncio
async def test_when_category_is_forced():
    category = await GetCashbackCategoryAction(
        force_category=Decimal('0.01'),
        cashback_category_id=EXPERIMENT_CATEGORY_ID,
        psp_id=PSP_EXP_WHITELISTED,
        merchant=ANY_MERCHANT,
    ).run()

    assert_that(category, equal_to(Decimal('0.01')))


@pytest.mark.asyncio
async def test_when_category_id_is_set_and_partner_is_ok():
    category = await GetCashbackCategoryAction(
        cashback_category_id=EXPERIMENT_CATEGORY_ID,
        psp_id=PSP_EXP_WHITELISTED,
        merchant=ANY_MERCHANT,
    ).run()

    assert_that(category, equal_to(Decimal(EXPERIMENT_CATEGORY_ID)))


@pytest.mark.asyncio
async def test_when_category_id_is_set_and_partner_is_bad():
    category = await GetCashbackCategoryAction(
        cashback_category_id=EXPERIMENT_CATEGORY_ID,
        psp_id=PSP_EXP_NOT_WHITELISTED,
        merchant=ANY_MERCHANT,
    ).run()

    assert_that(category, equal_to(DEFAULT_CATEGORY))


@pytest.mark.asyncio
async def test_when_category_id_is_set_and_exp_disabled(mocker):
    mocker.patch.object(GetCashbackCategoryAction, 'experiment_enabled', False)

    category = await GetCashbackCategoryAction(
        cashback_category_id=EXPERIMENT_CATEGORY_ID,
        psp_id=PSP_EXP_NOT_WHITELISTED,
        merchant=ANY_MERCHANT,
    ).run()

    assert_that(category, equal_to(DEFAULT_CATEGORY))


@pytest.mark.asyncio
async def test_when_category_id_is_set_and_exp_whitelist_disabled(mocker):
    mocker.patch.object(
        GetCashbackCategoryAction,
        'experiment_whitelist_enabled',
        False,
    )

    category = await GetCashbackCategoryAction(
        cashback_category_id=EXPERIMENT_CATEGORY_ID,
        psp_id=PSP_EXP_NOT_WHITELISTED,
        merchant=ANY_MERCHANT,
    ).run()

    assert_that(category, equal_to(Decimal(EXPERIMENT_CATEGORY_ID)))


@pytest.mark.asyncio
async def test_when_category_id_is_set_but_id_is_unknown(mocker):
    category = await GetCashbackCategoryAction(
        cashback_category_id='0.77',
        psp_id=PSP_EXP_NOT_WHITELISTED,
        merchant=ANY_MERCHANT,
    ).run()

    assert_that(category, equal_to(DEFAULT_CATEGORY))


@pytest.mark.parametrize('psp_id, merchant', (
    pytest.param(PSP_WITH_DEFAULT_CASHBACK, ANY_MERCHANT, id='by-psp-id'),
    pytest.param(ANY_PSP_ID, replace(ANY_MERCHANT, id=MERCHANT_ID_WITH_DEFAULT_CASHBACK), id='by-merchant-id'),
    pytest.param(ANY_PSP_ID, replace(ANY_MERCHANT, name=MERCHANT_NAME_WITH_DEFAULT_CASHBACK), id='by-merchant-name'),
    pytest.param(ANY_PSP_ID, replace(ANY_MERCHANT, url=MERCHANT_URL_WITH_DEFAULT_CASHBACK), id='by-merchant-url'),
    pytest.param(None, replace(ANY_MERCHANT, id=MERCHANT_ID_WITH_DEFAULT_CASHBACK), id='by-merchant-id'),
))
@pytest.mark.asyncio
async def test_default_partner_category(mocker, psp_id, merchant):
    category = await GetCashbackCategoryAction(
        psp_id=psp_id,
        merchant=merchant,
    ).run()

    assert_that(category, equal_to(Decimal(CUSTOM_DEFAULT_CATEGORY)))


@partner_entry_matches_default_params
@pytest.mark.asyncio
async def test_partner_ok_by_whitelist(mocker, partner_entry):
    mocker.patch.object(
        GetCashbackCategoryAction,
        'whitelist',
        (partner_entry,),
    )
    assert_that(
        await GetCashbackCategoryAction(**DEFAULT_PARAMS).run(),
        equal_to(DEFAULT_CATEGORY),
    )


@partner_entry_does_not_match_default_params
@pytest.mark.asyncio
async def test_partner_forbidden_by_whitelist(mocker, partner_entry):
    mocker.patch.object(
        GetCashbackCategoryAction,
        'whitelist',
        (partner_entry,),
    )
    assert_that(
        await GetCashbackCategoryAction(**DEFAULT_PARAMS).run(),
        equal_to(Decimal('0')),
    )


@partner_entry_matches_default_params
@pytest.mark.asyncio
async def test_partner_forbidden_by_blacklist(mocker, partner_entry):
    mocker.patch.object(
        GetCashbackCategoryAction,
        'blacklist',
        (partner_entry,),
    )
    assert_that(
        await GetCashbackCategoryAction(**DEFAULT_PARAMS).run(),
        equal_to(Decimal('0')),
    )


@partner_entry_does_not_match_default_params
@pytest.mark.asyncio
async def test_partner_ok_by_blacklist(mocker, partner_entry):
    mocker.patch.object(
        GetCashbackCategoryAction,
        'blacklist',
        (partner_entry,),
    )
    assert_that(
        await GetCashbackCategoryAction(**DEFAULT_PARAMS).run(),
        equal_to(DEFAULT_CATEGORY),
    )


@pytest.mark.asyncio
async def test_psp_merchant_pair_ok_by_blacklist(mocker):
    mocker.patch.object(
        GetCashbackCategoryAction,
        'blacklist',
        (
            CashbackPartnerEntry.create(psp_id=uuid.uuid4(), merchant_url='https://url.test'),
            CashbackPartnerEntry.create(psp_id=PSP_ID, merchant_url='https://black.test'),
        ),
    )
    assert_that(
        await GetCashbackCategoryAction(**DEFAULT_PARAMS).run(),
        equal_to(DEFAULT_CATEGORY),
    )


@pytest.mark.asyncio
async def test_psp_merchant_pair_forbidden_by_blacklist(mocker):
    mocker.patch.object(
        GetCashbackCategoryAction,
        'blacklist',
        (
            CashbackPartnerEntry.create(psp_id=uuid.uuid4(), merchant_url='https://url.test'),
            CashbackPartnerEntry.create(psp_id=PSP_ID, merchant_url='https://url.test'),
        ),
    )

    assert_that(
        await GetCashbackCategoryAction(**DEFAULT_PARAMS).run(),
        equal_to(Decimal('0')),
    )


@pytest.mark.asyncio
async def test_until_ok(mocker):
    mocker.patch.object(
        GetCashbackCategoryAction,
        'whitelist',
        (CashbackPartnerEntry.create(psp_id=PSP_ID, until=utcnow() + timedelta(hours=1)),),
    )
    assert_that(
        await GetCashbackCategoryAction(**DEFAULT_PARAMS).run(),
        equal_to(DEFAULT_CATEGORY),
    )


@pytest.mark.asyncio
async def test_until_past(mocker):
    mocker.patch.object(
        GetCashbackCategoryAction,
        'whitelist',
        (CashbackPartnerEntry.create(psp_id=PSP_ID, until=utcnow() - timedelta(hours=1)),),
    )
    assert_that(
        await GetCashbackCategoryAction(**DEFAULT_PARAMS).run(),
        equal_to(Decimal('0')),
    )


@pytest.mark.asyncio
async def test_since_ok(mocker):
    mocker.patch.object(
        GetCashbackCategoryAction,
        'whitelist',
        (CashbackPartnerEntry.create(psp_id=PSP_ID, since=utcnow() - timedelta(hours=1)),),
    )
    assert_that(
        await GetCashbackCategoryAction(**DEFAULT_PARAMS).run(),
        equal_to(DEFAULT_CATEGORY),
    )


@pytest.mark.asyncio
async def test_since_note_yet_come(mocker):
    mocker.patch.object(
        GetCashbackCategoryAction,
        'whitelist',
        (CashbackPartnerEntry.create(psp_id=PSP_ID, since=utcnow() + timedelta(hours=1)),),
    )
    assert_that(
        await GetCashbackCategoryAction(**DEFAULT_PARAMS).run(),
        equal_to(Decimal('0')),
    )


@pytest.mark.asyncio
async def test_cashback_disabled(mocker):
    mocker.patch.object(GetCashbackCategoryAction, 'cashback_enabled', False)
    assert_that(
        await GetCashbackCategoryAction(**DEFAULT_PARAMS).run(),
        equal_to(Decimal('0')),
    )


@pytest.mark.asyncio
async def test_disable_partner_restrictions__ignores_white_and_black_lists(mocker):
    mocker.patch.object(
        GetCashbackCategoryAction,
        'blacklist',
        (CashbackPartnerEntry.create(),)
    )

    assert_that(
        await GetCashbackCategoryAction(
            **(DEFAULT_PARAMS | {'enable_partner_restrictions': False})
        ).run(),
        equal_to(DEFAULT_CATEGORY),
    )


@pytest.fixture(autouse=True)
def set_experiment_enabled(mocker):
    mocker.patch.object(GetCashbackCategoryAction, 'experiment_enabled', True)


@pytest.fixture(autouse=True)
def set_experiment_whitelist_enabled(mocker):
    mocker.patch.object(GetCashbackCategoryAction, 'experiment_whitelist_enabled', True)


@pytest.fixture(autouse=True)
def set_default_category_per_partner(mocker):
    mocker.patch.object(
        GetCashbackCategoryAction,
        'default_cashback_category_per_partner',
        (
            (CashbackPartnerEntry.create(psp_id=PSP_WITH_DEFAULT_CASHBACK), CUSTOM_DEFAULT_CATEGORY),
            (CashbackPartnerEntry.create(merchant_id=MERCHANT_ID_WITH_DEFAULT_CASHBACK), CUSTOM_DEFAULT_CATEGORY),
            (CashbackPartnerEntry.create(merchant_name=MERCHANT_NAME_WITH_DEFAULT_CASHBACK), CUSTOM_DEFAULT_CATEGORY),
            (CashbackPartnerEntry.create(merchant_url=MERCHANT_URL_WITH_DEFAULT_CASHBACK), CUSTOM_DEFAULT_CATEGORY),
        )
    )


@pytest.fixture(autouse=True)
def set_experiment_whitelist(mocker):
    mocker.patch.object(
        GetCashbackCategoryAction,
        'experiment_whitelist',
        (CashbackPartnerEntry.create(psp_id=PSP_EXP_WHITELISTED),),
    )


@pytest.fixture(autouse=True)
def set_default_cashback_category(mocker):
    mocker.patch.object(
        GetCashbackCategoryAction,
        'cashback_default_category',
        DEFAULT_CATEGORY,
    )


@pytest.fixture(autouse=True)
def set_cashback_categories(mocker):
    mocker.patch.object(
        GetCashbackCategoryAction,
        'cashback_categories',
        (Decimal(EXPERIMENT_CATEGORY_ID),),
    )
