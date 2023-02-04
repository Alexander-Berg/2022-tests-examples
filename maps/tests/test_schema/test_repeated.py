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
    ({"rint": [1, 2, 3]}, for_tests_pb2.RepeatedMessage(rint=[1, 2, 3])),
    ({"rint": []}, for_tests_pb2.RepeatedMessage(rint=[])),
]
test_data_deserialize = reverse_parameters(test_data_serialize)


class RepeatedSchema(ProtobufSchema):
    class Meta:
        pb_message_class = for_tests_pb2.RepeatedMessage

    rint = fields.List(fields.Integer())


@pytest.mark.parametrize(("input_pb", "expected"), test_data_deserialize)
def test_deserialize_repeated(input_pb, expected):
    result = RepeatedSchema().load(input_pb).data

    assert result == expected


@pytest.mark.parametrize(("input_data", "expected"), test_data_serialize)
def test_serialize_repeated(input_data, expected):
    result = RepeatedSchema().dump(input_data).data

    assert result == expected
