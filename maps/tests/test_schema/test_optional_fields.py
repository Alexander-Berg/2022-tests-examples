import pytest
from marshmallow import ValidationError, fields

from smb.common.multiruntime.lib.basics import is_arcadia_python
from maps_adv.common.protomallow import ProtobufSchema

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


class MessageWithOptionalFieldSchema(ProtobufSchema):
    class Meta:
        pb_message_class = for_tests_pb2.MessageWithOptionalField

    required_field = fields.Integer()
    optional_field_one = fields.Integer()
    optional_field_two = fields.Integer()


@pytest.mark.parametrize(
    ("input_pb", "expected"),
    [
        (
            for_tests_pb2.MessageWithOptionalField(
                required_field=1, optional_field_one=2, optional_field_two=3
            ),
            {"required_field": 1, "optional_field_one": 2, "optional_field_two": 3},
        ),
        (
            for_tests_pb2.MessageWithOptionalField(
                required_field=1, optional_field_one=2
            ),
            {"required_field": 1, "optional_field_one": 2},
        ),
        (
            for_tests_pb2.MessageWithOptionalField(
                required_field=1, optional_field_two=3
            ),
            {"required_field": 1, "optional_field_two": 3},
        ),
        (
            for_tests_pb2.MessageWithOptionalField(required_field=1),
            {"required_field": 1},
        ),
    ],
)
def test_deserialize_optional_fields(input_pb, expected):
    result = MessageWithOptionalFieldSchema().load(input_pb).data

    assert result == expected


class MessageWithOnlyOptionalFields(ProtobufSchema):
    class Meta:
        pb_message_class = for_tests_pb2.MessageWithOnlyOptionalFields

    optional_field_one = fields.Integer()
    optional_field_two = fields.Integer()


@pytest.mark.parametrize(
    ("input_pb", "expected"),
    [
        (
            for_tests_pb2.MessageWithOnlyOptionalFields(
                optional_field_one=2, optional_field_two=3
            ),
            {"optional_field_one": 2, "optional_field_two": 3},
        ),
        (
            for_tests_pb2.MessageWithOnlyOptionalFields(optional_field_one=2),
            {"optional_field_one": 2},
        ),
        (
            for_tests_pb2.MessageWithOnlyOptionalFields(optional_field_two=2),
            {"optional_field_two": 2},
        ),
        (for_tests_pb2.MessageWithOnlyOptionalFields(), {}),
    ],
)
def test_deserialize_all_optional_field(input_pb, expected):
    result = MessageWithOnlyOptionalFields().load(input_pb).data

    assert result == expected


def test_deserialize_raises_if_required_field_not_set():
    input_pb = for_tests_pb2.MessageWithOptionalField(
        optional_field_one=2, optional_field_two=3
    )

    with pytest.raises(ValidationError):
        MessageWithOnlyOptionalFields().load(input_pb)


@pytest.mark.parametrize(
    ("input_data", "expected"),
    [
        (
            {"required_field": 1, "optional_field_one": 2, "optional_field_two": 3},
            for_tests_pb2.MessageWithOptionalField(
                required_field=1, optional_field_one=2, optional_field_two=3
            ),
        ),
        (
            {"required_field": 1, "optional_field_one": 2},
            for_tests_pb2.MessageWithOptionalField(
                required_field=1, optional_field_one=2
            ),
        ),
        (
            {"required_field": 1, "optional_field_one": None, "optional_field_two": 3},
            for_tests_pb2.MessageWithOptionalField(
                required_field=1, optional_field_two=3
            ),
        ),
    ],
)
def test_serialize(input_data, expected):
    result = MessageWithOptionalFieldSchema().dump(input_data).data

    assert result == expected


class MessageWithSubmessageFieldOfOptionalsSchema(ProtobufSchema):
    class Meta:
        pb_message_class = for_tests_pb2.MessageWithSubmessageFieldOfOptionals

    optional_submessage = fields.Nested(MessageWithOnlyOptionalFields)


def test_serialize_empty_optional_submessage_field():
    input_data = {"optional_submessage": {}}

    result = MessageWithSubmessageFieldOfOptionalsSchema().dump(input_data).data

    assert result == for_tests_pb2.MessageWithSubmessageFieldOfOptionals(
        optional_submessage=for_tests_pb2.MessageWithOnlyOptionalFields()
    )
