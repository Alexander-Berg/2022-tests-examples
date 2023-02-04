from datetime import datetime
from enum import Enum
from typing import Final, Optional

import hamcrest as hm
import pytest

from billing.library.python.calculator.values import PaymentMethodID

from billing.hot.calculators.trust.calculator.core.adapters import EventAdapter, EventRowAdapter
from billing.hot.calculators.trust.calculator.core.configurable_blocks.exceptions import Skip
from billing.hot.calculators.trust.calculator.core.configurable_blocks.filters import (
    FilterType,
    CurrencyFilter,
    RefundDTCheckFilter,
    SkipByPaymentMethodFilter,
    build_filter,
)
from billing.hot.calculators.trust.calculator.core.configurable_blocks.filters.base import BaseRowFilter
from billing.hot.calculators.trust.calculator.core.models.method import PaymentMethodModel, to_payment_method
from billing.hot.calculators.trust.calculator.core.parse_settings import _parse_settings

from ..test_data.payment.generated_data import (
    gen_event_with_multiple_refunds_method,
    gen_event_with_multiple_rows_method,
)


CONFIG = _parse_settings({"namespace": "trust", "endpoint": "payment", "accounts_mapping": []})


class CheckType(Enum):
    PAYMENT = "payment"
    REFUND = "refund"


def to_datetime(dt_str: str) -> datetime:
    return datetime.fromisoformat(dt_str)


class BaseTestFilter:
    def check_filter(
        self,
        payment_method: PaymentMethodModel,
        row_filter: BaseRowFilter,
        check_type: CheckType,
        error_expected: bool,
    ) -> None:

        rows_adapters = self.payment_rows_adapters(payment_method)
        if check_type == CheckType.REFUND:
            rows_adapters = self.refund_rows_adapters(payment_method)

        if error_expected:
            return self.check_with_error_expected(row_filter, rows_adapters)

        self.check_with_error_not_expected(row_filter, rows_adapters)

    @staticmethod
    def payment_rows_adapters(payment_method: PaymentMethodModel) -> list[EventRowAdapter]:
        adapter = EventAdapter(payment_method.event, CONFIG, payment_method.references)
        return adapter.payments

    @staticmethod
    def refund_rows_adapters(payment_method: PaymentMethodModel) -> list[EventRowAdapter]:
        adapter = EventAdapter(payment_method.event, CONFIG, payment_method.references)
        return adapter.refunds

    @staticmethod
    def check_with_error_expected(row_filter: BaseRowFilter, rows_adapters: list[EventRowAdapter]) -> None:
        for row_adapter_ in rows_adapters:
            with pytest.raises(Skip):
                row_filter.run(row_adapter_)

    @staticmethod
    def check_with_error_not_expected(row_filter: BaseRowFilter, rows_adapters: list[EventRowAdapter]) -> None:
        try:
            for row_adapter_ in rows_adapters:
                row_filter.run(row_adapter_)
        except Skip as e:
            assert False, f"raises exception when isn't supposed to: {e}"


class TestRefundDTCheckFilter(BaseTestFilter):
    @pytest.mark.parametrize(
        "cancel_dt,error_expected",
        (
            pytest.param(to_datetime("2021-11-05T17:18:44+00:00"), True, id='Skip if cancel_dt is not none'),
            pytest.param(None, False, id='Process if cancel_dt is none'),
        ),
    )
    def test_payment_with_refunds_filter(self, cancel_dt: Optional[str], error_expected: bool) -> None:
        method_data = gen_event_with_multiple_refunds_method()

        for refund in method_data["event"]["refunds"]:
            refund["cancel_dt"] = cancel_dt

        payment_method = to_payment_method(**method_data)

        self.check_filter(payment_method, RefundDTCheckFilter(), CheckType.REFUND, error_expected)


class TestSkipByPaymentFilter(BaseTestFilter):
    FILTER: Final[SkipByPaymentMethodFilter] = SkipByPaymentMethodFilter(
        skipped_payment_methods=["payment_type_1", "payment_type_2"],
        skipped_payment_method_ids=[PaymentMethodID.APPLE_PAY, PaymentMethodID.GOOGLE_PAY],
    )

    @pytest.mark.parametrize(
        "event_payment_method,payment_method_id,error_expected",
        (
            ("card-hash", PaymentMethodID.CARD, False),
            ("fiscal::payment_info-hash", PaymentMethodID.CARD, True),
            ("virtual::payment_info-hash", PaymentMethodID.CARD, False),
            ("virtual::payment_type_1-hash", PaymentMethodID.CARD, True),
            ("wrapper::payment_type_2-hash", PaymentMethodID.CARD, True),
            ("virtual::payment_info-hash", PaymentMethodID.APPLE_PAY, True),
            ("virtual::payment_info-hash", PaymentMethodID.GOOGLE_PAY, True),
        ),
    )
    def test_both_filters(
        self, event_payment_method: str, payment_method_id: PaymentMethodID, error_expected: bool
    ) -> None:
        payment_methods_data = [gen_event_with_multiple_rows_method(), gen_event_with_multiple_refunds_method()]

        for i, method_data in enumerate(payment_methods_data):
            check_type = CheckType.PAYMENT if i == 0 else CheckType.REFUND

            method_data["event"]["payment_method"] = event_payment_method
            method_data["event"]["payment_method_id"] = payment_method_id
            payment_method = to_payment_method(**method_data)

            self.check_filter(payment_method, self.FILTER, check_type, error_expected)

    def test_build_from_config(self) -> None:
        test_config = {
            "type": FilterType.SkipByPaymentMethodFilter,
            "arguments": {"skip_payment_methods": ["first", "second"], "skip_payment_method_ids": [100, 101]},
        }
        filter_ = build_filter(test_config)

        hm.assert_that(filter_, hm.instance_of(SkipByPaymentMethodFilter))


class TestCurrencyFilter(BaseTestFilter):
    FILTER: Final[CurrencyFilter] = CurrencyFilter(allowed_currencies=["RUB", "USD"])

    @pytest.mark.parametrize(
        "contract_currency,error_expected",
        (
            ("RUB", False),
            ("USD", False),
            ("EUR", True),
        ),
    )
    def tets_both_filters(self, contract_currency: str, error_expected: bool) -> None:
        payment_methods_data = [gen_event_with_multiple_rows_method(), gen_event_with_multiple_refunds_method()]

        for i, method_data in enumerate(payment_methods_data):
            check_type = CheckType.PAYMENT if i == 0 else CheckType.REFUND

            method_data["event"]["references"]["contracts"][0]["collaterals"][0]["currency"] = contract_currency
            payment_method = to_payment_method(**method_data)

            self.check_filter(payment_method, self.FILTER, check_type, error_expected)

    def test_build_from_config(self) -> None:
        test_config = {"type": FilterType.CurrencyFilter, "arguments": {"allowed_currencies": ["RUB"]}}
        filter_ = build_filter(test_config)

        hm.assert_that(filter_, hm.instance_of(CurrencyFilter))
