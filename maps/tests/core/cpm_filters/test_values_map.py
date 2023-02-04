from decimal import Decimal

import pytest

from maps_adv.billing_proxy.lib.core.cpm_filters import CpmUndefined, CpmData
from maps_adv.billing_proxy.lib.core.cpm_filters.values_map import ValuesMapCpmFilter


@pytest.fixture
def cpm_filter():
    return ValuesMapCpmFilter(values_map_key="values", value_key="parameter")


@pytest.mark.parametrize(
    ("values", "parameter", "expected_cpm"),
    [
        (
            {"value_one": "10.0", "value_two": "20.0", "value_three": "30.0"},
            "value_one",
            Decimal("10.0"),
        ),
        (
            {"value_one": "10.0", "value_two": "20.0", "value_three": "30.0"},
            "value_two",
            Decimal("20.0"),
        ),
        (
            {"value_one": "10.0", "value_two": "20.0", "value_three": "30.0"},
            "value_three",
            Decimal("30.0"),
        ),
        ({"value_one": "10.0"}, "value_one", Decimal("10.0")),
    ],
)
def test_returns_right_cpm(cpm_filter, values, parameter, expected_cpm):
    result = cpm_filter(values=values, parameter=parameter)

    assert result == CpmData(base_cpm=expected_cpm)


def test_returns_decimal(cpm_filter):
    result = cpm_filter(values={"value_one": "10.0"}, parameter="value_one")

    assert isinstance(result.final_cpm, Decimal)


def test_respects_values_map_key():
    cpm_filter = ValuesMapCpmFilter(
        values_map_key="values_custom", value_key="parameter"
    )

    result = cpm_filter(
        values={"value_one": "10.0"},
        values_custom={"value_one": "20.0"},
        parameter="value_one",
    )

    assert result.final_cpm == Decimal("20.0")


def test_respects_value_key():
    cpm_filter = ValuesMapCpmFilter(
        values_map_key="values", value_key="parameter_custom"
    )

    result = cpm_filter(
        values={"value_one": "10.0", "value_two": "20.0"},
        parameter="value_one",
        parameter_custom="value_two",
    )

    assert result.final_cpm == Decimal("20.0")


def test_raises_if_values_map_not_passed(cpm_filter):
    with pytest.raises(CpmUndefined):
        cpm_filter(
            parameter="value_one",
            values_custom={"value_one": "10.0", "value_two": "20.0"},
        )


def test_raises_if_value_not_passed(cpm_filter):
    with pytest.raises(CpmUndefined):
        cpm_filter(
            values={"value_one": "10.0", "value_two": "20.0"},
            parameter_custom="value_one",
        )


def test_raises_if_parameter_not_in_value_map(cpm_filter):
    with pytest.raises(CpmUndefined):
        cpm_filter(
            values={"value_one": "10.0", "value_two": "20.0"}, parameter="value_three"
        )
