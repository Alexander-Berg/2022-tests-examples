from decimal import Decimal
from typing import Any

import hamcrest as hm

from billing.library.python.calculator.vatcalc import add_percent

from billing.hot.calculators.trust.calculator.core.configurable_blocks.adapters import DefaultAdapter
from billing.hot.calculators.trust.calculator.core.configurable_blocks.adapters.base import AbstractAdapter
from billing.hot.calculators.trust.calculator.core.configurable_blocks.decorators import (
    DecoratorType,
    MinimalValueDecorator,
    NoRewardDecorator,
    VatDecorator,
    build_decorator,
)
from billing.hot.calculators.trust.calculator.core.configurable_blocks.getters import ConstGetter


def build_adapter(mapping: dict[str, Any]) -> AbstractAdapter:
    getters = {key: ConstGetter(value) for key, value in mapping.items()}
    return DefaultAdapter(getters)


DEFAULT_ADAPTER = build_adapter({})


class TestMinimalValueDecorator:
    def test_less_than_minimal(self) -> None:
        minimal_value_decorator = MinimalValueDecorator(DEFAULT_ADAPTER, "10")

        agent_reward = Decimal(5)

        assert minimal_value_decorator(agent_reward) == Decimal(10)

    def test_equal_to_minimal(self) -> None:
        minimal_value_decorator = MinimalValueDecorator(DEFAULT_ADAPTER, "10")

        agent_reward = Decimal(10)

        assert minimal_value_decorator(agent_reward) == agent_reward

    def test_more_than_minimal(self) -> None:
        minimal_value_decorator = MinimalValueDecorator(DEFAULT_ADAPTER, "10")

        agent_reward = Decimal(12)

        assert minimal_value_decorator(agent_reward) == agent_reward

    def test_negative_minimal(self) -> None:
        minimal_value_decorator = MinimalValueDecorator(DEFAULT_ADAPTER, "-10")

        agent_reward = Decimal(-5)

        assert minimal_value_decorator(agent_reward) == agent_reward

    def test_build_from_config(self) -> None:
        test_config = {"type": DecoratorType.MinimalValueDecorator, "arguments": {"value": 10}}
        decorator = build_decorator(test_config, DEFAULT_ADAPTER)

        hm.assert_that(decorator, hm.instance_of(MinimalValueDecorator))


class TestNoRewardDecorator:
    def test_payment_method_in_skipped_methods(self) -> None:
        adapter = build_adapter({"payment_method": "some_payment_method"})

        decorator = NoRewardDecorator(adapter, "payment_method", ["ignored_payment_method"])
        agent_reward = Decimal(2)

        assert decorator(agent_reward, {}) == agent_reward

    def test_payment_method_not_in_no_reward(self) -> None:
        adapter = build_adapter({"payment_method": "ignored_payment_method"})

        decorator = NoRewardDecorator(adapter, "payment_method", ["ignored_payment_method"])
        agent_reward = Decimal(2)

        assert decorator(agent_reward, {}) == Decimal(0)

    def test_build_from_config(self) -> None:
        test_config = {
            "type": DecoratorType.NoRewardDecorator,
            "arguments": {"field": "key", "skipped_values": ["first", "second"]},
        }
        decorator = build_decorator(test_config, DEFAULT_ADAPTER)

        hm.assert_that(decorator, hm.instance_of(NoRewardDecorator))


class TestVatDecorator:
    def test_with_vat(self) -> None:
        agent_reward = Decimal(110)
        vat_value = Decimal(13)

        adapter = build_adapter({"vat_value": vat_value})
        decorator = VatDecorator(adapter)

        assert decorator(agent_reward, {}) == add_percent(agent_reward, vat_value)

    def test_build_from_config(self) -> None:
        test_config = {"type": DecoratorType.VatDecorator, "arguments": {}}
        decorator = build_decorator(test_config, DEFAULT_ADAPTER)

        hm.assert_that(decorator, hm.instance_of(VatDecorator))
