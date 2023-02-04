from typing import Any, Optional

from billing.hot.calculators.trust.calculator.core.configurable_blocks.adapters.const import AdapterType
from billing.hot.calculators.trust.calculator.core.configurable_blocks.aw_calculators.const import CalculatorType
from billing.hot.calculators.trust.calculator.core.configurable_blocks.getters.const import GetterType


def gen_manifest_settings(
    namespace: Optional[str] = None,
    endpoint: Optional[str] = None,
    accounts_mapping: Optional[list[list[list[int]]]] = None,
    aw_calculator: Optional[dict] = None,
    adapter_getters: Optional[dict[str, Any]] = None,
    row_filters: Optional[list[dict]] = None,
    contract_filters_builders: Optional[list[dict]] = None,
    event_middlewares: Optional[list[dict]] = None,
    row_middlewares: Optional[list[dict]] = None,
    currency_conversion: Optional[dict] = None,
) -> dict[str, Any]:

    return {
        "namespace": namespace or "trust",
        "endpoint": endpoint or "payment",
        "accounts_mapping": accounts_mapping or [],
        "aw_calculator": aw_calculator or {},
        "adapter_getters": adapter_getters or {},
        "row_filters": row_filters or [],
        "contract_filters_builders": contract_filters_builders or [],
        "event_middlewares": event_middlewares or [],
        "row_middlewares": row_middlewares or [],
        "currency_conversion": currency_conversion or {},
    }


def gen_const_aw_calculator(amount: str = "10", percent: str = "100") -> dict[str, Any]:
    return {
        "adapter": {
            "type": AdapterType.DefaultAdapter,
            "getters": {
                "amount_getter": {"type": GetterType.ConstGetter, "arguments": {"const": amount}},
                "percent_getter": {"type": GetterType.ConstGetter, "arguments": {"const": percent}},
            },
        },
        "calculator": {"type": CalculatorType.DefaultCalculator, "arguments": {}, "decorators": []},
    }
