from decimal import Decimal
from typing import Any

import arrow
import pytest
from hamcrest import assert_that, close_to, instance_of, is_not

from billing.hot.calculators.trust.calculator.core.adapters import EventAdapter, EventRowAdapter
from billing.hot.calculators.trust.calculator.core.configurable_blocks.middlewares import (
    MiddlewareType,
    build_middleware,
)
from billing.hot.calculators.trust.calculator.core.configurable_blocks.middlewares.event_middlewares import (
    FillFromEventAdapterMiddleware,
)
from billing.hot.calculators.trust.calculator.core.configurable_blocks.middlewares.row_middlewares import (
    FillAmountWoVatMiddleware,
    FillCommonTsMiddleware,
    FillFromRowAdapterMiddleware,
    FillTaxPolicyMiddleware,
)
from billing.hot.calculators.trust.calculator.core.models.method import to_payment_method
from billing.hot.calculators.trust.calculator.core.parse_settings import _parse_settings

from ..test_data.payment.const import SERVICE_ID
from ..test_data.payment.generated_data import gen_event_with_one_row_method, gen_event_with_refund_with_one_row_method


SETTINGS = _parse_settings({"namespace": "trust", "endpoint": "payment", "accounts_mapping": []})


def get_event_adapter_row(method_data: dict) -> EventRowAdapter:
    payment_method = to_payment_method(**method_data)
    header_adapter = EventAdapter(payment_method.event, SETTINGS, payment_method.references)

    rows = header_adapter.refunds + header_adapter.payments
    assert len(rows) == 1

    return rows[0]


class TestFillCommonTsMiddleware:
    def test_middleware(self) -> None:
        method_data = gen_event_with_one_row_method()
        row_adapter = get_event_adapter_row(method_data)

        FillCommonTsMiddleware().run(row_adapter)

        assert row_adapter.tariffer_payload.get("common_ts") == arrow.get(row_adapter.on_dt).int_timestamp

    def test_build_from_config(self) -> None:
        test_config = {"type": MiddlewareType.FillCommonTsMiddleware, "arguments": {}}
        middleware = build_middleware(test_config)

        assert_that(middleware, instance_of(FillCommonTsMiddleware))


class TestFillFromEventAdapterMiddleware:
    @pytest.mark.parametrize(
        "src, dst, value",
        [
            pytest.param("service_id", None, SERVICE_ID),
            pytest.param("service_id", "service_id_field", SERVICE_ID),
        ],
    )
    def test_middleware(self, src: str, dst: str, value: Any) -> None:
        method_data = gen_event_with_one_row_method()
        row_adapter = get_event_adapter_row(method_data)

        FillFromEventAdapterMiddleware(src, dst).run(row_adapter)

        assert row_adapter.tariffer_payload.get(dst or src) == value

    def test_build_from_config(self) -> None:
        test_config = {"type": MiddlewareType.FillFromEventAdapterMiddleware, "arguments": {"src": "service_id"}}
        middleware = build_middleware(test_config)

        assert_that(middleware, instance_of(FillFromEventAdapterMiddleware))


class TestFillFromRowAdapterMiddleware:
    @pytest.mark.parametrize(
        "src, dst, value",
        [
            pytest.param("amount", None, Decimal("100.6")),
            pytest.param("amount", "info_amount", Decimal("100.6")),
        ],
    )
    def test_middleware(self, src: str, dst: str, value: Any) -> None:
        method_data = gen_event_with_one_row_method()
        row_adapter = get_event_adapter_row(method_data)

        FillFromRowAdapterMiddleware(src, dst).run(row_adapter)

        assert row_adapter.tariffer_payload.get(dst or src) == value

    def test_build_from_config(self) -> None:
        test_config = {"type": MiddlewareType.FillFromRowAdapterMiddleware, "arguments": {"src": "amount"}}
        middleware = build_middleware(test_config)

        assert_that(middleware, instance_of(FillFromRowAdapterMiddleware))


class TestFillTaxPolicyMiddleware:
    def test_middleware(self) -> None:
        method_data = gen_event_with_one_row_method()
        row_adapter = get_event_adapter_row(method_data)

        FillTaxPolicyMiddleware().run(row_adapter)

        assert_that(row_adapter.tariffer_payload.get("tax_policy_id"), is_not(None))
        assert_that(row_adapter.tariffer_payload.get("tax_policy_pct"), is_not(None))
        assert_that(row_adapter.tariffer_payload.get("firm_id"), is_not(None))

    def test_build_from_config(self) -> None:
        test_config = {"type": MiddlewareType.FillTaxPolicyMiddleware, "arguments": {}}
        middleware = build_middleware(test_config)

        assert_that(middleware, instance_of(FillTaxPolicyMiddleware))


class TestFillAmountWoVatMiddleware:
    @pytest.mark.parametrize(
        "method_data, expected_amount_wo_vat",
        [
            pytest.param(gen_event_with_one_row_method(), Decimal("83.833333")),
            pytest.param(gen_event_with_refund_with_one_row_method(), Decimal("-25.083333")),
        ],
    )
    def test_fill_amount_wo_vat_middleware(self, method_data: dict, expected_amount_wo_vat: Decimal) -> None:
        row_adapter = get_event_adapter_row(method_data)

        FillAmountWoVatMiddleware().run(row_adapter)

        assert_that(row_adapter.tariffer_payload["amount_wo_vat"], close_to(expected_amount_wo_vat, Decimal(0.000001)))

    def test_build_from_config(self) -> None:
        test_config = {"type": MiddlewareType.FillAmountWoVatMiddleware, "arguments": {}}
        middleware = build_middleware(test_config)

        assert_that(middleware, instance_of(FillAmountWoVatMiddleware))
