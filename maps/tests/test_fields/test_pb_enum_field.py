import enum

import pytest
from marshmallow import ValidationError

from smb.common.multiruntime.lib.basics import is_arcadia_python
from maps_adv.common.protomallow import PbEnumField
from maps_adv.common.protomallow.tests.test_fields.helpers import (
    reverse_parameters,
    serialize,
)

try:
    from maps_adv.common.protomallow.tests.proto import for_tests_pb2
except ImportError:
    if not is_arcadia_python:
        pytestmark = pytest.mark.skip(
            ".proto files in {} are not compiled. "
            "Compile them manually to run these tests.".format(
                "maps_adv.common.protomallow.tests.proto"
            )
        )
    else:
        raise

    for_tests_pb2 = None


class EnumTesting(enum.Enum):
    ONE = "ONE"
    TWO = "TWO"
    THREE = "THREE"


test_data_serialize = [
    (EnumTesting.ONE, 11),
    (EnumTesting.TWO, 12),
    (EnumTesting.THREE, 13),
]
test_data_deserialize = reverse_parameters(test_data_serialize)


@pytest.fixture
def field_instance():
    return PbEnumField(
        enum=EnumTesting,
        pb_enum=for_tests_pb2.Enum,
        values_map=[
            (for_tests_pb2.Enum.Value("ELEVEN"), EnumTesting.ONE),
            (for_tests_pb2.Enum.Value("TWELVE"), EnumTesting.TWO),
            (for_tests_pb2.Enum.Value("THIRTEEN"), EnumTesting.THREE),
        ],
    )


@pytest.mark.parametrize(("data", "expected"), test_data_deserialize)
def test_deserialize(field_instance, data, expected):
    result = field_instance.deserialize(data)

    assert result is expected


@pytest.mark.parametrize(("data", "expected"), test_data_serialize)
def test_serialize(field_instance, data, expected):
    result = serialize(field_instance, data)

    assert result == expected


def test_serialize_none(field_instance):
    assert serialize(field_instance, None) is None


@pytest.mark.parametrize("field_name", ["enum", "pb_enum", "values_map"])
def test_field_params_required(field_name):
    all_valid_params = {
        "enum": EnumTesting,
        "pb_enum": for_tests_pb2.Enum,
        "values_map": [
            (for_tests_pb2.Enum.Value("ELEVEN"), EnumTesting.ONE),
            (for_tests_pb2.Enum.Value("TWELVE"), EnumTesting.TWO),
            (for_tests_pb2.Enum.Value("THIRTEEN"), EnumTesting.THREE),
        ],
    }
    del all_valid_params[field_name]

    with pytest.raises(TypeError):
        PbEnumField(**all_valid_params)


@pytest.mark.parametrize("value", [0, -1, (1, 2), "213", "", None])
def test_deserialize_raises_for_wrong_value(field_instance, value):
    with pytest.raises(ValidationError):
        field_instance.deserialize(value)


@pytest.mark.parametrize("value", [0, -1, (1, 2), "213", ""])
def test_serialize_raises_for_wrong_value(field_instance, value):
    with pytest.raises(ValidationError):
        serialize(field_instance, value)
