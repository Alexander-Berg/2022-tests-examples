import pytest

from intranet.audit.src.users.logic.user import prepare_blackbox_response


def test_prepare_blackbox_response_success():
    response = {
        'users': [
            {'id': '123', 'login': 'test',
             'dbfields': {'userinfo.firstname.uid': 'some name',
                          'userinfo.lastname.uid': 'some last name',
                          },
             },
        ]
    }
    prepared_response = prepare_blackbox_response(response)
    assert len(prepared_response) == 1
    assert prepared_response[0]['last_name'] == 'some last name'


def test_prepare_blackbox_response_fail_no_login(caplog):
    response = {
        'users': [
            {'id': '123',
             },
            {'id': '1234', 'login': 'test',
             'dbfields': {'userinfo.firstname.uid': 'some name',
                          'userinfo.lastname.uid': 'some last name',
                          },
             },
        ]
    }
    prepared_response = prepare_blackbox_response(response)
    assert len(prepared_response) == 1
    assert 'User with uid "123" doesnt exists' in caplog.text
    assert prepared_response[0]['uid'] == '1234'


def test_prepare_blackbox_response_fail_error(caplog):
    response = {
        'users': [
            {'id': 123, 'error': 'something bad happens'},
        ]
    }
    prepared_response = prepare_blackbox_response(response)
    assert len(prepared_response) == 0
    assert ' Got error "something bad happens" while getting data with "123"' in caplog.text


def test_prepare_blackbox_response_fail_no_firstname():
    response = {
        'users': [
            {'login': 'test', 'id': 123,
             'dbfields': {'userinfo.lastname.uid': 'some last name',
                          },
             },
        ]
    }
    with pytest.raises(KeyError):
        prepare_blackbox_response(response)
