import json
from uuid import uuid4

import pytest
from intranet.search.tests.helpers.abovemeta_helpers import request_search, parse_response, get_request_params


def assert_auth_failed(response):
    assert response.code == 403
    assert parse_response(response)['errors'][0]['code'] == 'ERROR_AUTH'


def assert_blackbox_headers_ok(requester):
    tickets = json.loads(requester.request_mocking[requester.REQ_GET_SERVICE_TICKET]['body'])
    expected_ticket = tickets['blackbox']['ticket']
    request = get_auth_request(requester)
    # в bb передаем тикет, полученный из ручки tvmtool
    assert request.headers['X-Ya-Service-Ticket'] == expected_ticket


def assert_tvm_user_ticket_saved_in_state(requester):
    """ tvm-тикеты, полученные из blackbox должны сохраниться в state
    """
    tickets = json.loads(requester.request_mocking[requester.REQ_AUTH]['body'])
    expected_ticket = tickets['user_ticket']
    assert requester.state.tvm_auth_user_ticket == expected_ticket


def get_auth_request(requester):
    if requester.REQ_AUTH not in requester.called:
        return None
    return requester.called[requester.REQ_AUTH][0]


@pytest.mark.gen_test
def test_no_auth(http_client, base_url, requester):
    requester.patch_blackbox(status='INVALID')
    response = yield request_search(http_client, base_url)
    assert_auth_failed(response)
    assert_blackbox_headers_ok(requester)


@pytest.mark.gen_test
def test_cookie_ok(http_client, base_url, requester):
    sid = uuid4().hex
    response = yield request_search(http_client, base_url,
                                    headers={'Cookie': f'Session_id={sid}'})
    assert 200 == response.code
    assert_blackbox_headers_ok(requester)
    params = get_request_params(get_auth_request(requester))
    assert params['method'] == 'sessionid'
    assert params['sessionid'] == sid
    assert params['get_user_ticket'] == 'yes'
    assert_tvm_user_ticket_saved_in_state(requester)


@pytest.mark.gen_test
def test_oauth_ok(http_client, base_url, requester):
    token = uuid4().hex
    response = yield request_search(http_client, base_url,
                                    headers={'Authorization': f'OAuth {token}'})
    assert 200 == response.code
    assert_blackbox_headers_ok(requester)
    auth_request = get_auth_request(requester)
    params = get_request_params(auth_request)
    assert params['method'] == 'oauth'
    assert auth_request.headers['Authorization'] == f'OAuth {token}'
    assert_tvm_user_ticket_saved_in_state(requester)


@pytest.mark.gen_test
def test_tvm2_user_ticket_ok(http_client, base_url, requester):
    user_ticket = uuid4().hex
    server_ticket = uuid4().hex
    # ответ blackbox для tvm отличается от oauth и sessionid
    requester.patch_user_ticket_blackbox(login='some_login', uid='some_uid')
    response = yield request_search(http_client, base_url,
                                    headers={'X-Ya-User-Ticket': user_ticket,
                                             'X-Ya-Service-Ticket': server_ticket})
    assert 200 == response.code
    assert_blackbox_headers_ok(requester)
    auth_request = get_auth_request(requester)
    params = get_request_params(auth_request)
    assert params['method'] == 'user_ticket'
    assert params['user_ticket'] == user_ticket


@pytest.mark.gen_test
def test_tvm2_invalid_service_ticket(http_client, base_url, requester):
    """ Если передан некорректный сервисный тикет - авторизацию отклоняем
    """
    user_ticket = uuid4().hex
    server_ticket = uuid4().hex
    requester.patch_check_service_ticket(code=403, body={'error': 'invalid service ticket'})
    response = yield request_search(http_client, base_url,
                                    headers={'X-Ya-User-Ticket': user_ticket,
                                             'X-Ya-Service-Ticket': server_ticket})
    assert_auth_failed(response)
    assert get_auth_request(requester) is None


@pytest.mark.gen_test
def test_tvm2_invalid_user_ticket(http_client, base_url, requester):
    """ Если передан некорректный сервисный тикет - авторизацию отклоняем
    """
    user_ticket = uuid4().hex
    server_ticket = uuid4().hex
    requester.patch_blackbox(status='INVALID')
    response = yield request_search(http_client, base_url,
                                    headers={'X-Ya-User-Ticket': user_ticket,
                                             'X-Ya-Service-Ticket': server_ticket})
    assert_auth_failed(response)
    assert_blackbox_headers_ok(requester)


@pytest.mark.gen_test
def test_blackbox_error(http_client, base_url, requester):
    """ При непонятных ошибках blackbox - отклоняем авторизацию
    """
    requester.patch(requester.REQ_AUTH, code=500)
    response = yield request_search(http_client, base_url)
    assert_auth_failed(response)
    assert_blackbox_headers_ok(requester)


@pytest.mark.gen_test
def test_get_service_ticket_failed(http_client, base_url, requester):
    """ Если не можем выписать сами себе сервисные тикеты - отклоняем авторизацию
    """
    requester.patch(requester.REQ_GET_SERVICE_TICKET, code=500)
    response = yield request_search(http_client, base_url)
    assert_auth_failed(response)
    assert get_auth_request(requester) is None
