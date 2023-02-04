from datetime import datetime, timezone
from decimal import Decimal

import pytest

from sendr_pytest.matchers import convert_then_match
from sendr_utils import alist, utcnow

from hamcrest import assert_that, equal_to, has_properties

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.card_sheet.create import CreateCashbackCardSheetAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.card_sheet.get_or_create import (
    GetOrCreateCashbackCardSheetAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.cashback_sheet import resolve_sheet_period_for_datetime
from billing.yandex_pay_plus.yandex_pay_plus.tests.utils import to_utc


@pytest.fixture
async def trust_card_id(rands):
    return f'card-x{rands()}'


@pytest.mark.asyncio
async def test_get_sheet_if_exists(trust_card_id):
    for_datetime = utcnow()

    created = await CreateCashbackCardSheetAction(
        trust_card_id=trust_card_id,
        currency='XTS',
        spending_limit=Decimal('890'),
        **resolve_sheet_period_for_datetime(for_datetime)
    ).run()
    loaded = await GetOrCreateCashbackCardSheetAction(
        trust_card_id=trust_card_id,
        currency='XTS',
        for_datetime=for_datetime
    ).run()

    assert_that(loaded, equal_to(created))


@pytest.mark.asyncio
async def test_get_sheet_if_does_not_exist(trust_card_id, storage, yandex_pay_plus_settings):
    now = utcnow()
    sheets = await alist(storage.cashback_user_sheet.find())
    assert_that(sheets, equal_to([]))
    yandex_pay_plus_settings.CASHBACK_CARD_SHEET_SPENDING_LIMIT['XTS'] = 567

    created = await GetOrCreateCashbackCardSheetAction(
        trust_card_id=trust_card_id,
        currency='XTS',
    ).run()

    assert_that(
        created,
        has_properties(
            trust_card_id=trust_card_id,
            currency='XTS',
            spending_limit=Decimal('567'),
            period_start=convert_then_match(
                to_utc,
                equal_to(
                    datetime(year=now.year, month=now.month, day=1, tzinfo=timezone.utc)
                ),
            ),
            period_end=convert_then_match(
                to_utc,
                equal_to(
                    datetime(
                        year=now.year + int(now.month == 12),
                        month=1 if now.month == 12 else now.month + 1,
                        day=1,
                        tzinfo=timezone.utc,
                    )
                ),
            ),
        )
    )
