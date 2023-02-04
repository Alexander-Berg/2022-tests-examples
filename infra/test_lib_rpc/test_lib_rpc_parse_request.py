# Separate directory because of test data in .py file
from __future__ import unicode_literals
import json
import pytest
from werkzeug.datastructures import MultiDict

from awacs.lib import rpc


try:
    from . import _test_case_for_parse_request_pb2
except ImportError:
    from infra.awacs.vendor.awacs.tests.test_lib_rpc.proto import _test_case_for_parse_request_pb2


def test_init_from_args_ok():
    test_case = MultiDict({
        'intField': '100',
        'floatField': '2.0',
        'stringField': 'string',
        'boolField': 'true',
        'messageField.intField': '200',
        'messageField.floatField': '3.0',
        'messageField.stringField': 'string2',
        'messageField.boolField': 'false'
    })

    test_case.add('messageField.repeatedField', '1')
    test_case.add('messageField.repeatedField', '2')
    protobuf_object = _test_case_for_parse_request_pb2.Test1()
    rpc.parse_request.init_from_args(protobuf_object, test_case)
    assert protobuf_object.int_field == 100
    assert protobuf_object.float_field == 2.0
    assert protobuf_object.string_field == 'string'
    assert protobuf_object.bool_field is True
    # Check embedded message
    assert protobuf_object.message_field.int_field == 200
    assert protobuf_object.message_field.float_field == 3.0
    assert protobuf_object.message_field.string_field == 'string2'
    assert protobuf_object.message_field.bool_field is False
    assert protobuf_object.message_field.repeated_field == [1, 2]


def test_init_from_args_failure():
    test_cases = [
        {'intField': '200abc'},
        {'floatField': '-000xx3'},
        {'boolField': 'not bad'},
        {'messageField.intField': '200abc'},
        {'messageField.floatField': '-000xx3'},
        {'messageField': '{a: 1}'},
    ]
    protobuf_object = _test_case_for_parse_request_pb2.Test1()
    for test_case in test_cases:
        with pytest.raises(ValueError):
            rpc.parse_request.init_from_args(protobuf_object, MultiDict(test_case))


def test_init_from_args_ignore():
    test_cases = [
        # Test cases with wrong embedded objects, which we decided to ignore
        {'intField.foo': '100'},
        {'floatField.bar': '1.2'},
        {'boolField.baz': 'true'},
    ]
    protobuf_object = _test_case_for_parse_request_pb2.Test1()
    for test_case in test_cases:
        with pytest.raises(ValueError):
            rpc.parse_request.init_from_args(protobuf_object, MultiDict(test_case))
    # Completely unknown field
    rpc.parse_request.init_from_args(protobuf_object, MultiDict({'fooBar': '1'}))
    assert not protobuf_object.HasField('int_field')
    assert not protobuf_object.HasField('float_field')
    assert not protobuf_object.HasField('bool_field')


def test_init_from_content_ok():
    test_case = {
        "intField": 100,
        "floatField": 2.0,
        "stringField": "string",
        "boolField": True,
        "messageField": {
            "intField": 200,
            "floatField": 3.0,
            "stringField": "string2",
            "boolField": False,
            "repeatedField": [1, 2, 3]
        }
    }
    protobuf_object = _test_case_for_parse_request_pb2.Test1()
    rpc.parse_request.init_from_content(protobuf_object, json.dumps(test_case).encode('utf-8'))
    assert protobuf_object.int_field == 100
    assert protobuf_object.float_field == 2.0
    assert protobuf_object.string_field == "string"
    assert protobuf_object.bool_field is True
    # Check embedded message
    assert protobuf_object.message_field.int_field == 200
    assert protobuf_object.message_field.float_field == 3.0
    assert protobuf_object.message_field.string_field == 'string2'
    assert protobuf_object.message_field.bool_field is False
    # Check repeated field
    assert protobuf_object.message_field.repeated_field == [1, 2, 3]


def test_init_from_content_failure():
    pb_object = _test_case_for_parse_request_pb2.Test1()
    # Test json content
    test_cases_json = [
        b'{{}',  # Invalid json
        b'{"intField": "string"}',  # Invalid field type
        b'{"messageField": 42}',  # Another case for invalid type
    ]
    for test_case in test_cases_json:
        with pytest.raises(ValueError):
            rpc.parse_request.init_from_content(pb_object, test_case)
    # Test protobuf content
    test_cases_protobuf = [
        (b'Something wrong', ValueError),  # Non protobuf object
        (None, TypeError),
        (1, TypeError),
        (3.14159, TypeError),
    ]
    for test_case, exc_cls in test_cases_protobuf:
        with pytest.raises(exc_cls):
            rpc.parse_request.init_from_content(pb_object, test_case, content_type='application/protobuf')
    # Test unknown content type
    with pytest.raises(ValueError):
        rpc.parse_request.init_from_content(pb_object, b'Some content', content_type='text/html')
