import pytest

from intranet.search.tests.helpers.abovemeta_helpers import request_search, parse_response, get_request_params


@pytest.mark.gen_test
def test_url_feature_used(http_client, base_url, requester):
    response = yield request_search(http_client, base_url, params={'feature.some_feature': '1'})
    assert parse_response(response)['meta']['features']['some_feature'] == '1'


@pytest.mark.gen_test
def test_api_feature_used(http_client, base_url, requester):
    requester.patch_api_features({'some_feature': '1'})
    response = yield request_search(http_client, base_url, params={'text': 'test'})
    assert parse_response(response)['meta']['features']['some_feature'] == '1'


@pytest.mark.gen_test
def test_join_features(http_client, base_url, requester):
    requester.patch_api_features({'api_feature': '1'})
    requester.patch_uaas(features={'abt_feature': '2'})

    response = yield request_search(http_client, base_url,
                                    params={'text': 'test', 'feature.url_feature': '3'})

    expected_features = {
        'api_feature': '1',
        'abt_feature': '2',
        'url_feature': '3',
    }
    for name, value in expected_features.items():
        assert parse_response(response)['meta']['features'][name] == value


@pytest.mark.gen_test
def test_url_feature_priority_over_api(http_client, base_url, requester):
    requester.patch_api_features({'some_feature': '1'})
    response = yield request_search(http_client, base_url,
                                    params={'text': 'test', 'feature.some_feature': '2'})
    assert parse_response(response)['meta']['features']['some_feature'] == '2'


@pytest.mark.gen_test
def test_url_feature_priority_over_abt(http_client, base_url, requester):
    requester.patch_uaas(features={'some_feature': '1'})
    response = yield request_search(http_client, base_url,
                                    params={'text': 'test', 'feature.some_feature': '2'})
    assert parse_response(response)['meta']['features']['some_feature'] == '2'


@pytest.mark.gen_test
def test_url_features_send_to_revision_api(http_client, base_url, requester):
    """ Фичи из параметров передаются в апи получения ревизий """
    features = {'feature.some_feature': '1', 'feature.revisions': '123,456'}
    response = yield request_search(http_client, base_url, params=dict(text='test', **features))  # noqa

    assert len(requester.called.get('api.revisions', [])) == 1
    revision_params = get_request_params(requester.called['api.revisions'][0])
    for feature_name, feature_value in features.items():
        assert revision_params.get(feature_name) == feature_value


@pytest.mark.gen_test
def test_abt_features_send_to_revision_api(http_client, base_url, requester):
    """ Фичи из abt передаются в апи получения ревизий """
    features = {'some_feature': '1', 'revisions': '123,456'}
    requester.patch_uaas(features=features)
    response = yield request_search(http_client, base_url, params={'text': 'test'})  # noqa

    assert len(requester.called.get('api.revisions', [])) == 1
    revision_params = get_request_params(requester.called['api.revisions'][0])
    for feature_name, feature_value in features.items():
        assert revision_params.get(f'feature.{feature_name}') == feature_value
