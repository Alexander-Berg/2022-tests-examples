import pytest
from marshmallow import fields

from smb.common.multiruntime.lib.basics import is_arcadia_python
from maps_adv.common.protomallow import ProtobufSchema
from maps_adv.common.protomallow.tests.test_fields.helpers import reverse_parameters

try:
    from maps_adv.common.protomallow.tests.proto import for_tests_pb2
except ImportError:
    if not is_arcadia_python:
        raise Exception(
            ".proto files in {} are not compiled. "
            "Compile them manually to run these tests.".format(
                "maps_adv/common/tests/protomallow/proto"
            )
        )
    else:
        raise


class ChildSchema(ProtobufSchema):
    class Meta:
        pb_message_class = for_tests_pb2.Child

    i1 = fields.Integer()
    i2 = fields.Integer()


class ParentSchema(ProtobufSchema):
    class Meta:
        pb_message_class = for_tests_pb2.Parent

    child = fields.Nested(ChildSchema)


test_data_nested_serialize = [
    (
        {"child": {"i1": 1, "i2": 2}},
        for_tests_pb2.Parent(child=for_tests_pb2.Child(i1=1, i2=2)),
    )
]
test_data_nested_deserialize = reverse_parameters(test_data_nested_serialize)


@pytest.mark.parametrize(("input_pb", "expected"), test_data_nested_deserialize)
def test_deserialize_nested(input_pb, expected):
    result = ParentSchema().load(input_pb).data

    assert result == expected


@pytest.mark.parametrize(("input_data", "expected"), test_data_nested_serialize)
def test_serialize_nested(input_data, expected):
    result = ParentSchema().dump(input_data).data

    assert result == expected


class Level3Schema(ProtobufSchema):
    class Meta:
        pb_message_class = for_tests_pb2.Level3Message

    tint = fields.Integer()


class Level2Schema(ProtobufSchema):
    class Meta:
        pb_message_class = for_tests_pb2.Level2Message

    child = fields.Nested(Level3Schema)


class Level1Schema(ProtobufSchema):
    class Meta:
        pb_message_class = for_tests_pb2.Level1Message

    child = fields.Nested(Level2Schema)


class Level0Schema(ProtobufSchema):
    class Meta:
        pb_message_class = for_tests_pb2.Level0Message

    child = fields.Nested(Level1Schema)


test_data_nested_deeper_serialize = [
    (
        {"child": {"child": {"child": {"tint": 1}}}},
        for_tests_pb2.Level0Message(
            child=for_tests_pb2.Level1Message(
                child=for_tests_pb2.Level2Message(
                    child=for_tests_pb2.Level3Message(tint=1)
                )
            )
        ),
    )
]
test_data_nested_deeper_deserialize = reverse_parameters(
    test_data_nested_deeper_serialize
)


@pytest.mark.parametrize(("input_pb", "expected"), test_data_nested_deeper_deserialize)
def test_deserialize_nested_deeper(input_pb, expected):
    result = Level0Schema().load(input_pb).data

    assert result == expected


@pytest.mark.parametrize(("input_data", "expected"), test_data_nested_deeper_serialize)
def test_serialize_nested_deeper(input_data, expected):
    result = Level0Schema().dump(input_data).data

    assert result == expected


class ParentRepeatedSchema(ProtobufSchema):
    class Meta:
        pb_message_class = for_tests_pb2.ParentRepeated

    children = fields.Nested(ChildSchema, many=True)


test_data_nested_repeated_serialize = [
    (
        {"children": [{"i1": 1, "i2": 2}, {"i1": 2, "i2": 3}, {"i1": 4, "i2": 5}]},
        for_tests_pb2.ParentRepeated(
            children=[
                for_tests_pb2.Child(i1=1, i2=2),
                for_tests_pb2.Child(i1=2, i2=3),
                for_tests_pb2.Child(i1=4, i2=5),
            ]
        ),
    )
]
test_data_nested_repeated_deserialize = reverse_parameters(
    test_data_nested_repeated_serialize
)


@pytest.mark.parametrize(
    ("input_pb", "expected"), test_data_nested_repeated_deserialize
)
def test_deserialize_nested_repeated(input_pb, expected):
    result = ParentRepeatedSchema().load(input_pb).data

    assert result == expected


@pytest.mark.parametrize(
    ("input_data", "expected"), test_data_nested_repeated_serialize
)
def test_serialize_nested_repeated(input_data, expected):
    result = ParentRepeatedSchema().dump(input_data).data

    assert result == expected


class ChildWithRepeatedSchema(ProtobufSchema):
    class Meta:
        pb_message_class = for_tests_pb2.ChildWithRepeated

    tints = fields.List(fields.Integer)


class ParentWithChildWithRepeatedSchema(ProtobufSchema):
    class Meta:
        pb_message_class = for_tests_pb2.ParentWithChildWithRepeated

    child = fields.Nested(ChildWithRepeatedSchema)


test_data_nested_with_repeated_serialize = [
    (
        {"child": {"tints": [1, 2, 4]}},
        for_tests_pb2.ParentWithChildWithRepeated(
            child=for_tests_pb2.ChildWithRepeated(tints=[1, 2, 4])
        ),
    )
]
test_data_nested_with_repeated_deserialize = reverse_parameters(
    test_data_nested_with_repeated_serialize
)


@pytest.mark.parametrize(
    ("input_pb", "expected"), test_data_nested_with_repeated_deserialize
)
def test_deserialize_nested_with_repeated(input_pb, expected):
    result = ParentWithChildWithRepeatedSchema().load(input_pb).data

    assert result == expected


@pytest.mark.parametrize(
    ("input_data", "expected"), test_data_nested_with_repeated_serialize
)
def test_serialize_nested_with_repeated(input_data, expected):
    result = ParentWithChildWithRepeatedSchema().dump(input_data).data

    assert result == expected
