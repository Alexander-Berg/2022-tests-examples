import pytest
from marshmallow import fields

from smb.common.multiruntime.lib.basics import is_arcadia_python
from maps_adv.common.protomallow import ProtobufSchema
from maps_adv.common.protomallow.fields import PbMapField
from maps_adv.common.protomallow.tests.test_fields.helpers import reverse_parameters

try:
    from maps_adv.common.protomallow.tests.proto import for_tests_pb2
except ImportError:
    if not is_arcadia_python:
        raise Exception(
            ".proto files in {} are not compiled. "
            "Compile them manually to run these tests.".format(
                "maps_adv.common.protomallow.tests.proto"
            )
        )
    else:
        raise


class ChildSchema(ProtobufSchema):
    class Meta:
        pb_message_class = for_tests_pb2.Child

    i1 = fields.Integer()
    i2 = fields.Integer()


class MessageWithMapFieldsSchema(ProtobufSchema):
    class Meta:
        pb_message_class = for_tests_pb2.MessageWithMapFields

    map_field_one = PbMapField()
    map_field_two = PbMapField()
    map_field_three = PbMapField(value_field=fields.Nested(ChildSchema))


test_deserialize_map_fields_data = [
    (
        for_tests_pb2.MessageWithMapFields(
            map_field_one={"a": "b", "x": "y"},
            map_field_two={"a": 0, "x": 1},
            map_field_three={
                1: for_tests_pb2.Child(i1=1, i2=2),
                2: for_tests_pb2.Child(i1=3, i2=4),
            },
        ),
        {
            "map_field_one": {"a": "b", "x": "y"},
            "map_field_two": {"a": 0, "x": 1},
            "map_field_three": {1: {"i1": 1, "i2": 2}, 2: {"i1": 3, "i2": 4}},
        },
    ),
    (
        for_tests_pb2.MessageWithMapFields(
            map_field_two={"a": 0, "x": 1},
        ),
        {
            "map_field_one": {},
            "map_field_two": {"a": 0, "x": 1},
            "map_field_three": {},
        },
    ),
    (
        for_tests_pb2.MessageWithMapFields(),
        {
            "map_field_one": {},
            "map_field_two": {},
            "map_field_three": {},
        },
    ),
]


@pytest.mark.parametrize(("input_pb", "expected"), test_deserialize_map_fields_data)
def test_deserialize_map_fields(input_pb, expected):
    result = MessageWithMapFieldsSchema().load(input_pb).data

    assert result == expected


@pytest.mark.parametrize(
    ("input_data", "expected"), reverse_parameters(test_deserialize_map_fields_data)
)
def test_serialize_map_fields(input_data, expected):
    result = MessageWithMapFieldsSchema().dump(input_data).data

    assert result == expected
