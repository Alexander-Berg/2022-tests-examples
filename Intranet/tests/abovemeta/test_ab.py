from urllib.parse import parse_qs, urlsplit, urlparse
from uuid import uuid4

import pytest

from intranet.search.tests.helpers.models_helpers import get_revision_data
from intranet.search.tests.helpers.abovemeta_helpers import (
    request_search,
    request_suggest,
    parse_response,
)


def get_saas_requests(requester, req_type=None):
    req_type = req_type or requester.REQ_SEARCH_RESULTS
    if req_type not in requester.called:
        return []
    return requester.called[req_type]


def get_uaas_request(requester):
    if requester.REQ_UAAS not in requester.called:
        return None
    return requester.called[requester.REQ_UAAS][0]


def get_request_query(request):
    return parse_qs(urlsplit(request.url).query)


def get_buckets(request):
    buckets = get_request_query(request).get('test_buckets', [])
    return buckets[0] if buckets else None


@pytest.mark.gen_test
def test_send_right_headers_to_uaas(http_client, base_url, requester):
    text = uuid4().hex
    uid = uuid4().hex
    requester.patch_blackbox(uid=uid, login='some_login')
    user_ip = '123.45.67.89'
    user_agent = uuid4().hex
    headers = {'X-Forwarded-For': user_ip, 'User-Agent': user_agent}

    response = yield request_search(http_client, base_url, {'text': text}, headers=headers)  # noqa

    request = get_uaas_request(requester)
    assert request is not None
    assert request.headers['X-Forwarded-For-Y'] == user_ip
    assert request.headers['User-Agent'] == user_agent
    assert urlparse(request.url).path == '/intrasearch'
    expected_query = {'uuid': [f'is{uid}'], 'text': [text]}
    assert expected_query == get_request_query(request)


@pytest.mark.gen_test
def test_send_buckets_to_saas(http_client, base_url, requester):
    buckets = 'sometestid,0,10'
    requester.patch_uaas(buckets=buckets)
    requester.patch_api_revisions([get_revision_data(search='wiki')])

    response = yield request_search(http_client, base_url, {'text': 'some text'})

    requests = get_saas_requests(requester)
    assert response.code == 200
    assert len(requests) > 0
    for req in requests:
        assert get_buckets(req) == buckets


@pytest.mark.gen_test
def test_send_buckets_to_saas_suggest(http_client, base_url, requester):
    buckets = 'sometestid,0,10'
    requester.patch_uaas(buckets=buckets)
    search_name = 'people'
    requester.patch_api_revisions([get_revision_data(search=search_name)])

    response = yield request_suggest(http_client, base_url, {'version': 2, 'layers': 'people',
                                                             'text': 'some text'})
    requests = get_saas_requests(requester, '.'.join([requester.REQ_SEARCH_ALL, search_name]))
    assert response.code == 200
    assert len(requests) > 0
    for req in requests:
        assert get_buckets(req) == buckets


@pytest.mark.gen_test
def test_uaas_error(http_client, base_url, requester):
    requester.patch_uaas(code=500)
    requester.patch_api_revisions([get_revision_data(search='wiki')])

    response = yield request_search(http_client, base_url, {'text': 'some text'})

    requests = get_saas_requests(requester)
    assert response.code == 200
    assert len(requests) > 0
    for req in requests:
        assert get_buckets(req) is None


@pytest.mark.gen_test
def test_cannot_parse_flags(http_client, base_url, requester):
    requester.patch_uaas(flags='some_corrupted base64')
    requester.patch_api_revisions([get_revision_data(search='wiki')])

    response = yield request_search(http_client, base_url, {'text': 'some text'})
    # нигде ничего не сломалось
    assert response.code == 200


@pytest.mark.gen_test
def test_get_features_from_flags(http_client, base_url, requester):
    features = {'feature1': '1', 'FEATURE2': '2'}
    flags = {'HANDLER': 'INTRASEARCH',
             'CONTEXT': {'INTRASEARCH': {'FEATURES': features}}}

    requester.patch_uaas(flags=flags)
    requester.patch_api_revisions([get_revision_data(search='wiki')])

    response = yield request_search(http_client, base_url, {'text': 'some text'})
    expected_features = {'feature1': '1', 'feature2': '2'}
    for name, value in expected_features.items():
        assert parse_response(response)['meta']['features'][name] == value


@pytest.mark.gen_test
def test_ignore_no_intrasearch_flags(http_client, base_url, requester):
    features = {'feature1': '1'}
    flags = {'HANDLER': 'NOT_INTRASEARCH',
             'CONTEXT': {'INTRASEARCH': {'FEATURES': features}}}

    requester.patch_uaas(flags=flags)
    requester.patch_api_revisions([get_revision_data(search='wiki')])

    response = yield request_search(http_client, base_url, {'text': 'some text'})
    assert 'feature1' not in parse_response(response)['meta']['features']
