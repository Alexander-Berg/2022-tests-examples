from typing import Any, Optional

import hamcrest as hm
import pytest

from billing.library.python.calculator.exceptions import InvalidConfigError
from billing.library.python.calculator.services.contract import Eq

from billing.hot.calculators.trust.calculator.core.adapters import EventAdapter, EventRowAdapter
from billing.hot.calculators.trust.calculator.core.configurable_blocks.contract_filters import (
    ContractFilterType,
    build_contract_filter_builder,
)
from billing.hot.calculators.trust.calculator.core.configurable_blocks.contract_filters.contract_filters import (
    CommissionContractFilterBuilder,
)
from billing.hot.calculators.trust.calculator.core.configurable_blocks.exceptions import NoFiscalNdsError
from billing.hot.calculators.trust.calculator.core.models.method import to_payment_method
from billing.hot.calculators.trust.calculator.core.parse_settings import _parse_settings

from ..test_data.payment.generated_data import gen_event_with_one_row_method


CONFIG = _parse_settings({"namespace": "trust", "endpoint": "payment", "accounts_mapping": []})


def build_row_adapter(method_data: Optional[dict[str, Any]] = None) -> EventRowAdapter:
    method_data = method_data or gen_event_with_one_row_method()
    payment_method = to_payment_method(**method_data)

    return EventAdapter(payment_method.event, CONFIG, payment_method.references).payments[0]


class TestCommissionContractFilterBuilder:
    MAPPING = {"nds_18_118": 0, "nds_20": 0, "nds_20_120": 0, "nds_0": 72, "nds_none": 72}

    @pytest.mark.parametrize(
        "fiscal_nds,expected",
        (
            ("nds_20", 0),
            ("nds_none", 72),
            ("nds_18_118", 0),
        ),
    )
    def test_build_filter(self, fiscal_nds: str, expected: int) -> None:
        payment_method_data = gen_event_with_one_row_method()
        payment_method_data["event"]["rows"][0]["fiscal_nds"] = fiscal_nds

        row_adapter = build_row_adapter(payment_method_data)
        contract_filter = CommissionContractFilterBuilder(self.MAPPING).build_filter(row_adapter)

        hm.assert_that(contract_filter, hm.instance_of(Eq))
        hm.assert_that(contract_filter.field, hm.equal_to("commission"))
        hm.assert_that(contract_filter.value, hm.equal_to(expected))

    def test_no_row_fiscal_nds(self) -> None:
        payment_method_data = gen_event_with_one_row_method()
        payment_method_data["event"]["rows"][0]["fiscal_nds"] = None

        with pytest.raises(NoFiscalNdsError):
            CommissionContractFilterBuilder(self.MAPPING).build_filter(build_row_adapter(payment_method_data))

    def test_no_mapping_fiscal_nds(self) -> None:
        payment_method_data = gen_event_with_one_row_method()
        payment_method_data["event"]["rows"][0]["fiscal_nds"] = "non_existing"

        with pytest.raises(InvalidConfigError):
            CommissionContractFilterBuilder(self.MAPPING).build_filter(build_row_adapter(payment_method_data))

    def test_build_from_config(self) -> None:
        test_config = {
            "type": ContractFilterType.CommissionContractFilter,
            "arguments": {"nds_to_contract_type": self.MAPPING},
        }

        builder = build_contract_filter_builder(test_config)
        builder.build_filter(build_row_adapter())

        hm.assert_that(builder, hm.instance_of(CommissionContractFilterBuilder))
