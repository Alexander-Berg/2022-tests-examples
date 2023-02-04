from datetime import datetime, timezone
from decimal import Decimal

import pytest

from sendr_pytest.matchers import convert_then_match
from sendr_utils import utcnow

from hamcrest import assert_that, equal_to, has_properties

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.card_sheet.create import CreateCashbackCardSheetAction
from billing.yandex_pay_plus.yandex_pay_plus.core.cashback_sheet import resolve_sheet_period_for_datetime
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import CashbackSheetAlreadyExistsError
from billing.yandex_pay_plus.yandex_pay_plus.tests.utils import to_utc


class TestCreateSheet:
    @pytest.fixture
    async def trust_card_id(self, rands):
        return f'card-x{rands()}'

    def expected_sheet_periods(self, now):
        return dict(
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

    @pytest.mark.asyncio
    async def test_create_with_limit_provided(self, trust_card_id):
        limit = Decimal('1000')
        now = utcnow()
        sheet = await CreateCashbackCardSheetAction(
            trust_card_id=trust_card_id,
            currency='XTS',
            spending_limit=limit,
            **resolve_sheet_period_for_datetime(now)
        ).run()

        assert_that(
            sheet,
            has_properties(
                trust_card_id=trust_card_id,
                currency='XTS',
                spending_limit=limit,
                spent=Decimal('0'),
                **self.expected_sheet_periods(now),
            )
        )

    @pytest.mark.asyncio
    async def test_create_with_default_limit(
        self, trust_card_id, yandex_pay_plus_settings
    ):
        yandex_pay_plus_settings.CASHBACK_CARD_SHEET_SPENDING_LIMIT['XTS'] = 567

        now = utcnow()
        sheet = await CreateCashbackCardSheetAction(
            trust_card_id=trust_card_id,
            currency='XTS',
            **resolve_sheet_period_for_datetime(now),
        ).run()

        assert_that(
            sheet,
            has_properties(
                trust_card_id=trust_card_id,
                currency='XTS',
                spending_limit=Decimal('567'),
                spent=Decimal('0'),
                **self.expected_sheet_periods(now),
            )
        )

    @pytest.mark.asyncio
    async def test_create_with_zero_limit_if_default_limit_not_set(
        self, trust_card_id, yandex_pay_plus_settings
    ):
        now = utcnow()
        sheet = await CreateCashbackCardSheetAction(
            trust_card_id=trust_card_id,
            currency='GBP',
            **resolve_sheet_period_for_datetime(now),
        ).run()

        assert 'GBP' not in yandex_pay_plus_settings.CASHBACK_CARD_SHEET_SPENDING_LIMIT

        assert_that(
            sheet,
            has_properties(
                trust_card_id=trust_card_id,
                currency='GBP',
                spending_limit=Decimal('0'),
                spent=Decimal('0'),
                **self.expected_sheet_periods(now),
            )
        )

    @pytest.mark.asyncio
    async def test_create_duplicate_sheet_fails(self, trust_card_id):
        kwargs = dict(
            trust_card_id=trust_card_id,
            currency='XTS',
            spending_limit=Decimal('1000'),
            **resolve_sheet_period_for_datetime(utcnow())
        )
        await CreateCashbackCardSheetAction(**kwargs).run()

        with pytest.raises(CashbackSheetAlreadyExistsError):
            await CreateCashbackCardSheetAction(**kwargs).run()
