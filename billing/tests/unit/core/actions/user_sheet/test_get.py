from datetime import timedelta
from decimal import Decimal

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.customer import CreateCustomerAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.user_sheet.get_unspent import (
    GetCashbackUserSheetUnspentAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.cashback_user_sheet import CashbackUserSheet


@pytest.mark.asyncio
async def test_returns_existing_limit_when_sheet_exists(storage):
    await CreateCustomerAction(uid=100).run()
    await storage.cashback_user_sheet.create(
        CashbackUserSheet(
            uid=100,
            currency='XTS',
            spent=Decimal('100'),
            spending_limit=Decimal('600'),
            period_start=utcnow() - timedelta(days=1),
            period_end=utcnow() + timedelta(days=30),
        )
    )

    assert_that(
        await GetCashbackUserSheetUnspentAction(uid=100, currency='XTS', for_datetime=utcnow()).run(),
        equal_to(Decimal('500'))
    )


@pytest.mark.asyncio
async def test_returns_default_when_no_sheet(yandex_pay_plus_settings):
    yandex_pay_plus_settings.CASHBACK_USER_SHEET_SPENDING_LIMIT = {'XTS': '10.0'}
    assert_that(
        await GetCashbackUserSheetUnspentAction(uid=100, currency='XTS', for_datetime=utcnow()).run(),
        equal_to(Decimal('10.0'))
    )


@pytest.mark.asyncio
async def test_returns_default_when_no_uid(yandex_pay_plus_settings):
    yandex_pay_plus_settings.CASHBACK_USER_SHEET_SPENDING_LIMIT = {'XTS': '10.0'}
    assert_that(
        await GetCashbackUserSheetUnspentAction(uid=None, currency='XTS', for_datetime=utcnow()).run(),
        equal_to(Decimal('10.0'))
    )


@pytest.mark.asyncio
async def test_returns_zero_when_no_default_sheet_limit(yandex_pay_plus_settings):
    yandex_pay_plus_settings.CASHBACK_USER_SHEET_SPENDING_LIMIT = {}
    assert_that(
        await GetCashbackUserSheetUnspentAction(uid=None, currency='XTS', for_datetime=utcnow()).run(),
        equal_to(Decimal('0'))
    )
