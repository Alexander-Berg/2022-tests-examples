from decimal import Decimal

import hamcrest as hm
import pytest

from billing.hot.calculators.trust.calculator.core.adapters import EventAdapter
from billing.hot.calculators.trust.calculator.core.configurable_blocks.adapters import DefaultAdapter
from billing.hot.calculators.trust.calculator.core.configurable_blocks.adapters.const import AdapterType
from billing.hot.calculators.trust.calculator.core.configurable_blocks.aw_calculators import (
    DefaultCalculator,
    parse_aw_manifest,
)
from billing.hot.calculators.trust.calculator.core.configurable_blocks.aw_calculators.const import CalculatorType
from billing.hot.calculators.trust.calculator.core.configurable_blocks.decorators import (
    AbstractDecorator,
    MinimalValueDecorator,
    NoRewardDecorator,
    VatDecorator,
)
from billing.hot.calculators.trust.calculator.core.configurable_blocks.decorators.const import DecoratorType
from billing.hot.calculators.trust.calculator.core.configurable_blocks.getters import ConstGetter, FieldGetter
from billing.hot.calculators.trust.calculator.core.configurable_blocks.getters.const import GetterType
from billing.hot.calculators.trust.calculator.core.models.method import to_payment_method
from billing.hot.calculators.trust.calculator.core.parse_settings import _parse_settings
from billing.hot.calculators.trust.calculator.core.utils import DictAccessWrapper

from ..test_data.manifests import gen_manifest_settings
from ..test_data.payment.generated_data import gen_event_with_contract_with_reward_percent


DEFAULT_ADAPTER = DefaultAdapter(
    {
        "amount_getter": ConstGetter(110),
        "percent_getter": ConstGetter(13),
        "vat_value": ConstGetter(10),
        "skip_field": ConstGetter("trip_bonus"),
    }
)


class TestDefaultCalculator:
    TEST_CONFIG = {
        "adapter": {
            "type": AdapterType.DefaultAdapter,
            "getters": {
                "amount_getter": {"type": GetterType.FieldGetter, "arguments": {"jsonpath": "$.row.amount"}},
                "percent_getter": {
                    "type": GetterType.FieldGetter,
                    "arguments": {"jsonpath": "$.cs.partner_commission_pct2", "strict": False, "default": 0},
                },
                "payment_method": {
                    "type": GetterType.FieldGetter,
                    "arguments": {"jsonpath": "$.row.payment_type"},
                },
                "const_field": {"type": GetterType.ConstGetter, "arguments": {"const": "value"}},
            },
            "arguments": {},
        },
        "calculator": {
            "type": CalculatorType.DefaultCalculator,
            "decorators": [
                {"type": DecoratorType.MinimalValueDecorator, "arguments": {"value": 1}},
                {
                    "type": DecoratorType.NoRewardDecorator,
                    "arguments": {"field": "payment_method", "skipped_values": ["some_method"]},
                },
            ],
            "arguments": {},
        },
    }

    def test_calculate_reward(self) -> None:
        calculator = DefaultCalculator(DEFAULT_ADAPTER)

        assert calculator.calculate({}) == (Decimal(110), Decimal("14.300000"))
        assert calculator.calculate({}, rounding=Decimal("0.01")) == (Decimal(110), Decimal("14.30"))

    @pytest.mark.parametrize(
        "decorators,result",
        [
            ([MinimalValueDecorator(DEFAULT_ADAPTER, "20")], Decimal(20)),
            ([NoRewardDecorator(DEFAULT_ADAPTER, "skip_field", ["trip_bonus"])], Decimal(0)),
            ([VatDecorator(DEFAULT_ADAPTER)], Decimal("15.730000")),
            ([VatDecorator(DEFAULT_ADAPTER), MinimalValueDecorator(DEFAULT_ADAPTER, "20")], Decimal(20)),
            ([MinimalValueDecorator(DEFAULT_ADAPTER, "20"), VatDecorator(DEFAULT_ADAPTER)], Decimal(22)),
        ],
    )
    def test_with_decorators(self, decorators: list[AbstractDecorator], result: Decimal) -> None:
        calculator = DefaultCalculator(DEFAULT_ADAPTER, decorators)

        assert calculator.calculate(data={}) == (Decimal(110), result)

    @pytest.mark.parametrize(
        "input_method,expected_rewards",
        [(gen_event_with_contract_with_reward_percent(), [Decimal("1"), Decimal("5")])],
    )
    def test_with_params_from_contract(self, input_method: dict, expected_rewards: list[Decimal]) -> None:
        """Check that jsonpath_rw can successfully work with contract and adapter objects"""

        settings = _parse_settings(gen_manifest_settings())
        payment_method = to_payment_method(**input_method)
        header_adapter = EventAdapter(payment_method.event, settings, payment_method.references)

        rewards_values = []
        for row in header_adapter.payments:
            _, reward = parse_aw_manifest(self.TEST_CONFIG).calculate(
                {
                    "row": DictAccessWrapper(row),
                    "header": DictAccessWrapper(header_adapter),
                    "cs": DictAccessWrapper(row.current_signed),
                }
            )

            rewards_values.append(reward)

        hm.assert_that(rewards_values, hm.contains_inanyorder(*expected_rewards))

    def test_build_from_config(self) -> None:
        calculator = parse_aw_manifest(self.TEST_CONFIG)
        adapter = calculator.adapter

        hm.assert_that(calculator, hm.instance_of(DefaultCalculator))
        hm.assert_that(calculator.decorators, hm.has_length(2))
        hm.assert_that(calculator.decorators[0], hm.instance_of(MinimalValueDecorator))
        hm.assert_that(calculator.decorators[1], hm.instance_of(NoRewardDecorator))

        hm.assert_that(adapter, hm.instance_of(DefaultAdapter))
        hm.assert_that(adapter.getters, hm.has_length(4))
        hm.assert_that(adapter.getters["amount_getter"], hm.instance_of(FieldGetter))
        hm.assert_that(adapter.getters["const_field"], hm.instance_of(ConstGetter))
