"""Test tskv loggers"""

import logging
from unittest import mock

import pytest

from walle.statbox import handlers


class TestTskvSerializer:
    def test_formats_key_value_pairs(self):
        serializer = handlers.TskvSerializer()
        assert "tskv\ta=1\tb=2" == serializer.serialize({"a": 1, "b": 2})

    def test_int_to_str_is_number(self):
        serializer = handlers.TskvSerializer()
        assert "1" == serializer._value_to_str(1)

    def test_string_to_str_is_same_string(self):
        serializer = handlers.TskvSerializer()
        assert "letter" == serializer._value_to_str("letter")

    def test_dict_to_str_is_json(self):
        serializer = handlers.TskvSerializer()
        assert '{"dict": 1}' == serializer._value_to_str({"dict": 1})

    def test_list_to_str_is_json(self):
        serializer = handlers.TskvSerializer()
        assert '["list"]' == serializer._value_to_str(["list"])

    def test_tuple_to_str_is_list_in_json(self):
        serializer = handlers.TskvSerializer()
        assert '["tuple"]' == serializer._value_to_str(("tuple",))

    @pytest.mark.parametrize("value", [True, False])
    def test_boolean_to_str_is_str(self, value):
        serializer = handlers.TskvSerializer()
        assert str(value) == serializer._value_to_str(value)

    def test_none_to_str_is_str(self):
        serializer = handlers.TskvSerializer()
        assert "None" == serializer._value_to_str(None)


class TestJsonSerializer:
    def test_serializes_key_value_pairs_as_json(self):
        serializer = handlers.JsonSerializer()
        assert '{"a": 1, "b": 2}' == serializer.serialize({"a": 1, "b": 2})


def test_log_uses_base_context():
    logger = mock.Mock()
    context = {"a": 1, "b": 2}

    class MockSerializer:
        context = []

        def serialize(self, context):
            self.context.append(context)
            return handlers.TskvSerializer.serialize(context)

    serializer = MockSerializer()
    cl = handlers.ContextLogger(logger, serializer, **context)
    cl.log(c=3)
    cl.log(b=3)

    assert serializer.context == [{"a": 1, "b": 2, "c": 3}, {"a": 1, "b": 3}]
    assert logger.log.call_args_list == [
        mock.call(logging.INFO, "tskv\ta=1\tb=2\tc=3"),
        mock.call(logging.INFO, "tskv\ta=1\tb=3"),
    ]


def test_get_child_is_copying():
    context = {"foo": 0, "bar": 1}

    class MockSerializer:
        context = []

        def serialize(self, context):
            self.context.append(context)
            return handlers.TskvSerializer.serialize(context)

    serializer = MockSerializer()
    cl = handlers.ContextLogger(mock.Mock(), serializer, **context)

    cl0 = cl.get_child(baz=2)
    cl1 = cl0.get_child(foo=1)
    cl2 = cl0.get_child(foo=2)
    cl3 = cl1.get_child(bar=3)
    cl4 = cl2.get_child(bar=4)

    cl3.log(baz=5)
    cl4.log(baz=6)

    assert cl.get_context() == {"foo": 0, "bar": 1}
    assert cl0.get_context() == {"foo": 0, "bar": 1, "baz": 2}
    assert cl1.get_context() == {"foo": 1, "bar": 1, "baz": 2}
    assert cl2.get_context() == {"foo": 2, "bar": 1, "baz": 2}
    assert cl3.get_context() == {"foo": 1, "bar": 3, "baz": 2}
    assert cl4.get_context() == {"foo": 2, "bar": 4, "baz": 2}

    assert serializer.context == [
        {"foo": 1, "bar": 3, "baz": 5},
        {"foo": 2, "bar": 4, "baz": 6},
    ]


def test_logging_data_with_same_keys():
    context = {"timestamp": 1, "timezone": 2}
    scenario_logger = handlers.StatboxLogger(
        log_type="test", logger=logging.getLogger("tskv.mock"), serializer=handlers.JsonSerializer()
    )
    # https://st.yandex-team.ru/WALLE-2384 if this bug appears the test will fail
    try:
        scenario_logger.log(**context)
    except TypeError:  # logger raises TypeError if it has similar keys in context
        assert False
