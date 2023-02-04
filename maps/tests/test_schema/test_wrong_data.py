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
                "maps_adv/common/tests/protomallow/proto"
            )
        )
    else:
        raise


class MessageOneSchema(ProtobufSchema):
    class Meta:
        pb_message_class = for_tests_pb2.MessageOne

    tint = fields.Integer()


def test_deserialize_raises_in_wrong_message_type():
    input_pb = for_tests_pb2.MessageTwo(tstring="2")
    with pytest.raises(ValidationError):
        MessageOneSchema().load(input_pb)


class Schema(ProtobufSchema):
    class Meta:
        pb_message_class = for_tests_pb2.Message

    tint1 = fields.Integer()
    tint2 = fields.Integer()


def test_serialize_submessage_raises_on_insufficient_fields():
    with pytest.raises(ValidationError):
        Schema().dump({"tint1": 2})


def test_deserialize_raises_on_insufficient_data():
    insufficient_data = for_tests_pb2.Message(tint1=1)

    with pytest.raises(ValidationError):
        Schema().load(insufficient_data)
