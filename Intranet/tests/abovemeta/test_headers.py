import pytest
from intranet.search.tests.helpers.abovemeta_helpers import request_search, parse_response


@pytest.mark.gen_test
def test_get_request_id_from_params(http_client, base_url, requester):
    request_id = 'my_req_id'
    response = yield request_search(http_client, base_url, params={'request_id': request_id})
    assert parse_response(response)['meta']['request_id'] == request_id


@pytest.mark.gen_test
def test_get_request_id_from_headers(http_client, base_url, requester):
    request_id = 'my_req_id'
    response = yield request_search(http_client, base_url, headers={'x-request-id': request_id})
    assert parse_response(response)['meta']['request_id'] == request_id


@pytest.mark.gen_test
def test_generate_request_id_if_not_exists(http_client, base_url, requester):
    response = yield request_search(http_client, base_url)
    # смотрим, что request_id не пустой
    assert parse_response(response)['meta']['request_id']
