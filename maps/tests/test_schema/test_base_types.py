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


test_data_serialize = [
    (
        {
            "tint32": -32,
            "tint64": -64,
            "tuint32": 32,
            "tuint64": 64,
            "tsint32": -320,
            "tsint64": -640,
            "tfixed32": 3200,
            "tfixed64": 6400,
            "tsfixed32": -3200,
            "tsfixed64": -6400,
            "tbool": True,
            "tstring": "qwerty",
        },
        for_tests_pb2.SimpleTypesMessage(
            tint32=-32,
            tint64=-64,
            tuint32=32,
            tuint64=64,
            tsint32=-320,
            tsint64=-640,
            tfixed32=3200,
            tfixed64=6400,
            tsfixed32=-3200,
            tsfixed64=-6400,
            tbool=True,
            tstring="qwerty",
        ),
    )
]
test_data_deserialize = reverse_parameters(test_data_serialize)


class BaseTypeSchema(ProtobufSchema):
    class Meta:
        pb_message_class = for_tests_pb2.SimpleTypesMessage

    tint32 = fields.Integer()
    tint64 = fields.Integer()
    tuint32 = fields.Integer()
    tuint64 = fields.Integer()
    tsint32 = fields.Integer()
    tsint64 = fields.Integer()
    tfixed32 = fields.Integer()
    tfixed64 = fields.Integer()
    tsfixed32 = fields.Integer()
    tsfixed64 = fields.Integer()
    tbool = fields.Boolean()
    tstring = fields.String()


@pytest.mark.parametrize(("input_pb", "expected"), test_data_deserialize)
def test_deserialize_base_types(input_pb, expected):
    result = BaseTypeSchema().load(input_pb).data

    assert result == expected


@pytest.mark.parametrize(("input_data", "expected"), test_data_serialize)
def test_serialize_base_types(input_data, expected):
    result = BaseTypeSchema().dump(input_data).data

    assert result == expected
