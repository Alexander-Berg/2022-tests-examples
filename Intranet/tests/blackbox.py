from mock import Mock, patch
import pytest

from staff.lib import blackbox


REQUEST_PARAMS_TEST_DATA = [
    (
        {
            'uid': 1234,
            'attributes': [1, 2],
            'passport_fields': ['a.b', 'a.c'],
        },
        {
            'format': 'json',
            'method': 'userinfo',
            'userip': '127.0.0.1',
            'uid': 1234,
            'attributes': '1,2',
            'dbfields': 'a.b,a.c',
        },
    ),
    (
        {
            'uid': 1234,
            'attributes': [5, 6],
        },
        {
            'format': 'json',
            'method': 'userinfo',
            'userip': '127.0.0.1',
            'uid': 1234,
            'attributes': '5,6',
        },
    ),
    (
        {
            'uid': 1234,
            'passport_fields': ['x.y', 'x.z'],
        },
        {
            'format': 'json',
            'method': 'userinfo',
            'userip': '127.0.0.1',
            'uid': 1234,
            'dbfields': 'x.y,x.z',
        },
    ),
    (
        {
            'login': 'uhura',
            'attributes': [1, 2],
            'passport_fields': ['a.b', 'a.c'],
        },
        {
            'format': 'json',
            'method': 'userinfo',
            'userip': '127.0.0.1',
            'login': 'uhura',
            'attributes': '1,2',
            'dbfields': 'a.b,a.c',
        },
    ),
    (
        {
            'login': 'spock',
            'uid': 5678,
            'attributes': [3, 4],
            'passport_fields': ['a.b', 'a.c'],
        },
        {
            'format': 'json',
            'method': 'userinfo',
            'userip': '127.0.0.1',
            'uid': 5678,
            'attributes': '3,4',
            'dbfields': 'a.b,a.c',
        },
    ),
]

FAKE_BLACKBOX_RESPONSE_BODY = {
    'users': [
        {
            'uid': {
                'value': '1234',
            },
            'attributes': {
                '1': 'abc',
                '2': 'def',
            },
            'dbfields': {
                'test.foo': 'vuw',
                'test.bar': 'xyz',
            },
        },
    ],
}


class FakeResponse:
    def __init__(self, parsed_body):
        self.parsed_body = parsed_body

    def raise_for_status(self):
        pass

    def json(self):
        return self.parsed_body


@pytest.mark.parametrize('params,expected_answer', REQUEST_PARAMS_TEST_DATA)
def test_create_request_params(params, expected_answer):
    test_bb = blackbox.Blackbox('test_url', 'test_service')
    assert test_bb._create_request_params(**params) == expected_answer


def test_parse_bb_answer():
    test_bb = blackbox.Blackbox('test_url', 'test_service')

    answer = {
        'users': [
            {
                'uid': {
                    'value': '1234',
                },
                'attributes': {
                    '1': 'abc',
                    '2': 'def',
                },
                'dbfields': {
                    'test.foo': 'vuw',
                    'test.bar': 'xyz',
                },
            },
        ],
    }

    expected_result = {
        'uid': {
            'value': '1234',
        },
        'attributes': {
            '1': 'abc',
            '2': 'def',
        },
        'dbfields': {
            'test.foo': 'vuw',
            'test.bar': 'xyz',
        },
    }

    assert test_bb._parse_bb_answer(answer) == expected_result


def test_parse_bb_answer_error():
    test_bb = blackbox.Blackbox('test_url', 'test_service')

    bad_answer = {
        'error': 'Something went wrong',
    }

    with pytest.raises(blackbox.BlackboxError):
        test_bb._parse_bb_answer(bad_answer)


@patch('staff.lib.blackbox.tvm2.get_tvm_ticket_by_deploy', Mock(return_value=''))
@patch('staff.lib.blackbox.requests.get', Mock(return_value=FakeResponse(FAKE_BLACKBOX_RESPONSE_BODY)))
def test_get_user_info():
    test_bb = blackbox.Blackbox('test_url', 'test_service')

    test_params = {
        'uid': 1234,
        'attributes': [1, 2],
        'passport_fields': ['test.foo', 'test.bar'],
    }

    expected_result = {
        'uid': {
            'value': '1234',
        },
        'attributes': {
            '1': 'abc',
            '2': 'def',
        },
        'dbfields': {
            'test.foo': 'vuw',
            'test.bar': 'xyz',
        },
    }

    assert test_bb.get_user_info(**test_params) == expected_result


@patch('staff.lib.blackbox.tvm2.get_tvm_ticket_by_deploy', Mock(return_value=''))
@patch('staff.lib.blackbox.requests.get', Mock(return_value=FakeResponse(FAKE_BLACKBOX_RESPONSE_BODY)))
def test_get_uid_by_login():
    test_bb = blackbox.Blackbox('test_url', 'test_service')
    assert test_bb.get_uid_by_login(login='any') == 1234
