# coding: utf-8
import json

import httpretty
import mock
import pytest
import requests
from hamcrest import assert_that, equal_to, has_entries, empty

from butils.http_message import HttpResponse
from medium.medium_nirvana import NirvanaLogic


RETURN_VALUE = HttpResponse(
    200,
    body=json.dumps({"something": "111111"}),
    content_type='application/json',
)

def validate_response(res, method_mock):
    # type: (requests.Response, mock.MagicMock)
    assert_that(res.content, equal_to(method_mock.return_value.content))
    assert_that(res.status_code, equal_to(method_mock.return_value.status_code))


def test_put_block(medium_http, tvm_valid_ticket_mock):
    with mock.patch.object(NirvanaLogic, NirvanaLogic.put_operation.__name__, autospec=True) as method_mock:
        method_mock.return_value = RETURN_VALUE
        with medium_http.mocked_uri(path="/nirvana/v2/call/some_operation/some_instance_id",
                                    http_method=httpretty.PUT) as uri:
            ticket, service_ticket, headers = tvm_valid_ticket_mock
            res = requests.put(uri, data="some_content", headers=headers)

        method_mock.assert_called_once_with(
                mock.ANY, 'some_operation', 'some_instance_id', 'some_content',
            )
        validate_response(res, method_mock)


# igogor: werkzeug автоматически добавляет роутинг на head если добавлен get. Не вижу смысла с этим бороться
def test_get_block(medium_http, tvm_valid_ticket_mock):
    with mock.patch.object(NirvanaLogic, NirvanaLogic.get_operation.__name__, autospec=True) as method_mock:
        method_mock.return_value = RETURN_VALUE
        with medium_http.mocked_uri(path="/nirvana/v2/call/some_operation/some_instance_id",
                                    http_method=httpretty.GET) as uri:
            ticket, service_ticket, headers = tvm_valid_ticket_mock
            res = requests.get(uri + "?ticket=some_ticket", headers=headers)

        method_mock.assert_called_once_with(
                mock.ANY, 'some_operation', 'some_instance_id', 'some_ticket',
            )
        validate_response(res, method_mock)

def test_get_block_head(medium_http, tvm_valid_ticket_mock):
    with mock.patch.object(NirvanaLogic, NirvanaLogic.get_operation.__name__, autospec=True) as method_mock:
        method_mock.return_value = RETURN_VALUE
        with medium_http.mocked_uri(path="/nirvana/v2/call/some_operation/some_instance_id",
                                    http_method=httpretty.HEAD) as uri:
            ticket, service_ticket, headers = tvm_valid_ticket_mock
            res = requests.head(uri + "?ticket=some_ticket", headers=headers)

        method_mock.assert_called_once_with(
                mock.ANY, 'some_operation', 'some_instance_id', 'some_ticket',
            )
        assert_that(res.content, empty())
        assert_that(res.status_code, equal_to(method_mock.return_value.status_code))


def test_get_block_missed_ticket_param(medium_http, tvm_valid_ticket_mock):
    with mock.patch.object(NirvanaLogic, NirvanaLogic.get_operation.__name__, autospec=True) as method_mock:
        method_mock.return_value = RETURN_VALUE
        with medium_http.mocked_uri(path="/nirvana/v2/call/some_operation/some_instance_id",
                                    http_method=httpretty.GET) as uri:
            ticket, service_ticket, headers = tvm_valid_ticket_mock
            res = requests.get(uri + "?some_arg=some_value", headers=headers)

    assert_that(res.status_code, equal_to(400))
    assert_that(res.text, equal_to(u'Missing parameter: ticket'))
    method_mock.assert_not_called()


def test_delete_block(medium_http, tvm_valid_ticket_mock):
    with mock.patch.object(NirvanaLogic, NirvanaLogic.delete_operation.__name__, autospec=True) as method_mock:
        method_mock.return_value = HttpResponse(204)
        with medium_http.mocked_uri(path="/nirvana/v2/call/some_operation/some_instance_id",
                                    http_method=httpretty.DELETE) as uri:
            ticket, service_ticket, headers = tvm_valid_ticket_mock
            res = requests.delete(uri + "?ticket=some_ticket", headers=headers)

        method_mock.assert_called_once_with(
                mock.ANY, 'some_operation', 'some_instance_id', 'some_ticket',
            )
        validate_response(res, method_mock)


def test_delete_block_missed_ticket_param(medium_http, tvm_valid_ticket_mock):
    with mock.patch.object(NirvanaLogic, NirvanaLogic.get_operation.__name__, autospec=True) as method_mock:
        method_mock.return_value = RETURN_VALUE
        with medium_http.mocked_uri(path="/nirvana/v2/call/some_operation/some_instance_id",
                                    http_method=httpretty.DELETE) as uri:
            ticket, service_ticket, headers = tvm_valid_ticket_mock
            res = requests.delete(uri + "?some_arg=some_value", headers=headers)

        assert_that(res.status_code, equal_to(400))
        assert_that(res.text, equal_to(u'Missing parameter: ticket'))
        method_mock.assert_not_called()


def test_get_operations(medium_http, tvm_valid_ticket_mock):
    name = NirvanaLogic.operations.__name__
    with mock.patch.object(NirvanaLogic, name, autospec=True) as method_mock:
        method_mock.return_value = RETURN_VALUE
        # igogor: из-за того что метод статический походу в мок не добавляется имя, а нам оно нужно
        method_mock.__name__ = name
        with medium_http.mocked_uri(path="/nirvana/v2/call/",
                                    http_method=httpretty.GET) as uri:
            ticket, service_ticket, headers = tvm_valid_ticket_mock
            res = requests.get(uri, headers=headers)

        method_mock.assert_called_once_with()
        validate_response(res, method_mock)


@pytest.mark.parametrize('method', ['put', 'get', 'head', 'delete'])
@pytest.mark.parametrize('path', [
    '/',
    '/v2',
    '/v2/call',
    '/v2/call/some/bad/path',
])
def test_bad_path(medium_http, method, path, tvm_valid_ticket_mock):
    with medium_http.mocked_uri(path=path, http_method=method) as uri:
        ticket, service_ticket, headers = tvm_valid_ticket_mock
        res = requests.request(method, uri, headers=headers)

    assert_that(res.status_code, equal_to(404))


# httpretty не умеет мокать http запросы отличающиеся от списка. Поэтому часть не проверить.
# https://a.yandex-team.ru/arc/trunk/arcadia/contrib/python/HTTPretty/httpretty/http.py?rev=6393653#L110
@pytest.mark.parametrize('method', ['options','post', 'patch', 'connect'])
def test_bad_method(medium_http, method, tvm_valid_ticket_mock):
    with medium_http.mocked_uri(path="/nirvana/v2/call/some_operation/some_instance_id",
                                http_method=method) as uri:
        ticket, service_ticket, headers = tvm_valid_ticket_mock
        res = requests.request(method, uri + "?ticket=some_ticket", headers=headers)

    assert_that(res.status_code, equal_to(405))
    assert_that(res.text, equal_to(u'The method is not allowed for the requested URL.'))
