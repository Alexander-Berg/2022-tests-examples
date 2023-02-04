import pytest

from itsman.client import ApiClient
from itsman.exceptions import ApiException


def test_exceptions(response_mock):

    client = ApiClient(token='ttt')
    with pytest.raises(ApiException):
        client.session.get = None
        client.request('xxx')

    client = ApiClient(token='ttt')
    with pytest.raises(ApiException) as e:
        with response_mock('GET https://its.yandex-team.ru/v2/l7/heavy/some -> 400:{"a": "b"}'):
            client.request('some')
    assert f'{e.value}' == "{'a': 'b'}"


def test_etag(response_mock):

    client = ApiClient(token='ttt')
    with response_mock('''
        GET https://its.yandex-team.ru/v2/l7/heavy/some

        ETag: klmn

        -> 200:{"a": "b"}
    '''):
        response = client.request('some', etag_match='xxyyzz')

    assert response == ({'a': 'b'}, 'klmn')
