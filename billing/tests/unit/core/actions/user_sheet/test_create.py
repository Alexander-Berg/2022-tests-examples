from datetime import datetime, timezone
from decimal import Decimal

import pytest

from sendr_pytest.matchers import convert_then_match
from sendr_utils import utcnow

from hamcrest import assert_that, equal_to, has_properties

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.customer import CreateCustomerAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.user_sheet.create import CreateCashbackUserSheetAction
from billing.yandex_pay_plus.yandex_pay_plus.core.cashback_sheet import resolve_sheet_period_for_datetime
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import CashbackSheetAlreadyExistsError
from billing.yandex_pay_plus.yandex_pay_plus.tests.utils import to_utc


class TestCreateSheet:
    @pytest.fixture
    async def customer(self):
        return await CreateCustomerAction(uid=456).run()

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
    async def test_create_with_limit_provided(self, customer):
        limit = Decimal('1000')
        now = utcnow()
        sheet = await CreateCashbackUserSheetAction(
            uid=customer.uid,
            currency='XTS',
            spending_limit=limit,
            **resolve_sheet_period_for_datetime(now),
        ).run()

        assert_that(
            sheet,
            has_properties(
                uid=customer.uid,
                currency='XTS',
                spending_limit=limit,
                spent=Decimal('0'),
                **self.expected_sheet_periods(now),
            )
        )

    @pytest.mark.asyncio
    async def test_create_with_default_limit(
        self, customer, yandex_pay_plus_settings
    ):
        yandex_pay_plus_settings.CASHBACK_USER_SHEET_SPENDING_LIMIT['XTS'] = 567

        now = utcnow()
        sheet = await CreateCashbackUserSheetAction(
            uid=customer.uid,
            currency='XTS',
            **resolve_sheet_period_for_datetime(now),
        ).run()

        assert_that(
            sheet,
            has_properties(
                uid=customer.uid,
                currency='XTS',
                spending_limit=Decimal('567'),
                spent=Decimal('0'),
                **self.expected_sheet_periods(now),
            )
        )

    @pytest.mark.asyncio
    async def test_create_with_zero_limit_if_default_limit_not_set(
        self, customer, yandex_pay_plus_settings
    ):
        now = utcnow()
        sheet = await CreateCashbackUserSheetAction(
            uid=customer.uid,
            currency='GBP',
            **resolve_sheet_period_for_datetime(now),
        ).run()

        assert 'GBP' not in yandex_pay_plus_settings.CASHBACK_USER_SHEET_SPENDING_LIMIT
        assert_that(
            sheet,
            has_properties(
                uid=customer.uid,
                currency='GBP',
                spending_limit=Decimal('0'),
                spent=Decimal('0'),
                **self.expected_sheet_periods(now),
            )
        )

    @pytest.mark.asyncio
    async def test_create_duplicate_sheet_fails(self, customer):
        kwargs = dict(
            uid=customer.uid,
            currency='XTS',
            spending_limit=Decimal('1000'),
            **resolve_sheet_period_for_datetime(utcnow())
        )
        await CreateCashbackUserSheetAction(**kwargs).run()

        with pytest.raises(CashbackSheetAlreadyExistsError):
            await CreateCashbackUserSheetAction(**kwargs).run()
