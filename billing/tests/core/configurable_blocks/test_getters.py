import operator
from typing import Any

import hamcrest as hm
import pytest

from billing.library.python.calculator.exceptions import InvalidConfigError

from billing.hot.calculators.trust.calculator.core.configurable_blocks.exceptions import (
    InvalidJsonPathError,
    NoDataInEventError,
)
from billing.hot.calculators.trust.calculator.core.configurable_blocks.getters import (
    AbstractGetter,
    CaseGetter,
    ConstDispatcherGetter,
    ConstGetter,
    DispatcherGetter,
    FieldGetter,
    GetterType,
    build_getter,
)
from billing.hot.calculators.trust.calculator.core.configurable_blocks.getters.case_getter import Case


TEST_DATA = {
    "field1": {
        "inner_field1": "value1",
    },
    "field2": {
        "inner_field1": "value2",
    },
}


class TestFieldGetter:
    def test_get_value(self) -> None:
        arguments: dict[str, Any] = {"jsonpath": "$.field1.inner_field1", "strict": True, "default": None}
        field_getter = FieldGetter(**arguments)

        hm.assert_that(field_getter.get(TEST_DATA), hm.equal_to("value1"))

    def test_with_strict_and_default(self) -> None:
        arguments: dict[str, Any] = {"jsonpath": "$.no_data", "strict": True, "default": "result"}
        field_getter = FieldGetter(**arguments)

        with pytest.raises(NoDataInEventError):
            field_getter.get(TEST_DATA)

    def test_no_strict_with_default(self) -> None:
        arguments: dict[str, Any] = {"jsonpath": "$.no_data", "strict": False, "default": "result"}
        field_getter = FieldGetter(**arguments)

        hm.assert_that(field_getter.get(TEST_DATA), hm.equal_to("result"))

    def test_no_strict_no_default(self) -> None:
        arguments: dict[str, Any] = {"jsonpath": "$.no_data", "strict": False}
        field_getter = FieldGetter(**arguments)

        with pytest.raises(NoDataInEventError):
            field_getter.get(TEST_DATA)

    def test_ambiguous_search_result(self) -> None:
        arguments: dict[str, Any] = {"jsonpath": "$.*.inner_field1", "strict": False, "default": "result"}
        field_getter = FieldGetter(**arguments)

        with pytest.raises(NoDataInEventError):
            field_getter.get(TEST_DATA)

    def test_invalid_jsonpath(self) -> None:
        arguments: dict[str, Any] = {"jsonpath": "$$", "strict": False, "default": "result"}

        with pytest.raises(InvalidJsonPathError):
            FieldGetter(**arguments)

    def test_build_from_config(self) -> None:
        config = {
            "type": GetterType.FieldGetter,
            "arguments": {"jsonpath": "$.field1.inner_field1", "strict": True},
        }
        getter = build_getter(config, units={})

        hm.assert_that(getter, hm.instance_of(FieldGetter))
        assert getter.get(TEST_DATA) == TEST_DATA["field1"]["inner_field1"]


class TestConstGetter:
    @pytest.mark.parametrize("value", [10, None, False, {}])
    def test_get_value(self, value: Any) -> None:
        const_getter = ConstGetter(value)

        hm.assert_that(const_getter.get({}), hm.equal_to(value))

    def test_build_from_config(self) -> None:
        config = {"type": GetterType.ConstGetter, "arguments": {"const": "value1"}}
        getter = build_getter(config, units={})

        hm.assert_that(getter, hm.instance_of(ConstGetter))
        assert getter.get({}) == "value1"

    def test_get_none_value(self) -> None:
        const_getter = ConstGetter(None)

        hm.assert_that(const_getter.get(TEST_DATA), hm.equal_to(None))


class TestCaseGetter:
    def test_case(self) -> None:
        cases = [
            Case(
                left=ConstGetter(10),
                operator=operator.eq,
                right=ConstGetter(11),
                value=ConstGetter(0),
            ),
            Case(
                left=ConstGetter(10),
                operator=operator.ne,
                right=ConstGetter(10),
                value=ConstGetter(1),
            ),
            Case(
                left=ConstGetter(10),
                operator=operator.le,
                right=ConstGetter(11),
                value=ConstGetter(2),
            ),
        ]
        default = ConstGetter(3)
        getter = CaseGetter(cases, default)
        hm.assert_that(getter.get({}), hm.equal_to(2))

    def test_default(self) -> None:
        cases = [
            Case(
                left=ConstGetter(10),
                operator=operator.eq,
                right=ConstGetter(11),
                value=ConstGetter(0),
            ),
        ]
        default = ConstGetter(3)
        getter = CaseGetter(cases, default)
        hm.assert_that(getter.get({}), hm.equal_to(3))

    def test_without_default_with_match(self) -> None:
        cases = [
            Case(
                left=ConstGetter(11),
                operator=operator.eq,
                right=ConstGetter(11),
                value=ConstGetter(0),
            ),
        ]
        getter = CaseGetter(cases)
        hm.assert_that(getter.get({}), hm.equal_to(0))

    def test_without_default_no_match(self) -> None:
        cases = [
            Case(
                left=ConstGetter(10),
                operator=operator.eq,
                right=ConstGetter(11),
                value=ConstGetter(0),
            ),
        ]
        getter = CaseGetter(cases)
        with pytest.raises(InvalidConfigError):
            getter.get({})

    def test_build_from_config(self) -> None:
        units = {"field1": ConstGetter(10), "field2": ConstGetter(10), "value": ConstGetter(100)}
        config = {
            "type": GetterType.CaseGetter,
            "arguments": {"cases": [{"left": "field1", "operator": "eq", "right": "field2", "value": "value"}]},
        }

        getter = build_getter(config, units)

        hm.assert_that(getter, hm.instance_of(CaseGetter))
        assert getter.get({}) == 100


class TestDispatcherGetterSchema:
    @pytest.mark.parametrize(
        "key_getter,result",
        [
            (ConstGetter("field1"), 10),
            (ConstGetter("field2"), 20),
            (ConstGetter("field3"), -1),
        ],
    )
    def test_get_value(self, key_getter: AbstractGetter, result: Any) -> None:
        mapping: dict[Any, AbstractGetter] = {
            "field1": ConstGetter(10),
            "field2": ConstGetter(20),
            "field3": ConstGetter(-1),
        }

        assert DispatcherGetter(key_getter, mapping).get({}) == result

    def test_default(self) -> None:
        key_getter, default_getter = ConstGetter(-1), ConstGetter("default")
        mapping: dict[Any, AbstractGetter] = {"field1": ConstGetter(10)}

        assert DispatcherGetter(key_getter, mapping, default_getter).get({}) == "default"

    def test_no_data_no_default(self) -> None:
        mapping: dict[Any, AbstractGetter] = {"field1": ConstGetter(10)}

        with pytest.raises(InvalidConfigError):
            assert DispatcherGetter(ConstGetter(-1), mapping).get({})

    def test_build_from_config(self) -> None:
        units = {
            "__actual_service_fee": ConstGetter(1),
            "__getter_1": ConstGetter(10),
            "__getter_2": ConstGetter(20),
            "__default_getter": ConstGetter(-1),
        }
        config = {
            "type": GetterType.DispatcherGetter,
            "arguments": {
                "key": "__actual_service_fee",
                "default": "__default_getter",
                "mapping": {1: "__getter_1", 2: "__getter_2"},
            },
        }

        getter = build_getter(config, units)
        assert getter.get({}) == 10


class TestConstDispatcherGetterSchema:
    def test_get_value(self) -> None:
        mapping = {"field1": 10, "field2": 20, "field3": 30}

        assert ConstDispatcherGetter(ConstGetter("field2"), mapping).get({}) == 20

    def test_default(self) -> None:
        mapping = {"field1": 10}

        assert ConstDispatcherGetter(ConstGetter("field2"), mapping, default="default").get({}) == "default"
        assert ConstDispatcherGetter(ConstGetter("field2"), mapping, default=0).get({}) == 0
        assert ConstDispatcherGetter(ConstGetter("field2"), mapping, default=None).get({}) is None

    def test_no_data_no_default(self) -> None:
        mapping = {"field1": 10}

        with pytest.raises(InvalidConfigError):
            assert ConstDispatcherGetter(ConstGetter("field2"), mapping).get({})

    def test_build_from_config(self) -> None:
        units: dict[Any, AbstractGetter] = {"__actual_service_fee": ConstGetter(1), 1: ConstGetter(10)}
        config = {
            "type": GetterType.ConstDispatcherGetter,
            "arguments": {
                "key": "__actual_service_fee",
                "default": "default",
                "mapping": {
                    1: "612e0f9c-a3d7-4d02-be0b-a31a57242da4",
                    2: "85a6597d-2bca-4da4-9b46-f6c23c4638cd",
                },
            },
        }

        getter = build_getter(config, units)
        assert getter.get({}) == "612e0f9c-a3d7-4d02-be0b-a31a57242da4"
