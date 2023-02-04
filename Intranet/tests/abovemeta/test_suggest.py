import pytest

from intranet.search.tests.helpers.abovemeta_helpers import request_suggest, parse_response
from intranet.search.tests.helpers.models_helpers import get_revision_data


def _fetch_features(data):
    return data.get('meta', {}).get('features', {})


def _fetch_features_v1(data):
    meta = {}
    for item in data:
        if item.get('layer') == 'meta':
            meta = item.get('result')
            break
    return meta.get('features', {})


@pytest.mark.gen_test
@pytest.mark.parametrize('referer, has_features', (
    ('wiki.yandex-team.ru', True),
    ('femida.yandex-team.ru', False),
))
@pytest.mark.parametrize('version, fetcher', (
    (0, _fetch_features),
    (1, _fetch_features_v1),
    (2, _fetch_features),
))
@pytest.mark.parametrize('layers', ('people', 'wiki', 'atushka'))
def test_suggest_has_features(http_client, base_url, requester,
                              referer, has_features, version, fetcher, layers):
    """ Проверяет, что в саджесте прилетают фичи (обычные и uaas),
    если в запросе Referer – это wiki
    """
    requester.patch_api_features({'feature': '1'})
    requester.patch_uaas(features={'uaas_feature': True})
    revisions = [get_revision_data(search='people')]
    if layers == 'wiki':
        revisions.append(get_revision_data(search='wiki'))
    if layers == 'atushka':
        revisions.append(get_revision_data(search='at', index='posts'))
    requester.patch_api_revisions(revisions)

    response = yield request_suggest(
        http_client=http_client,
        base_url=base_url,
        params={
            'version': version,
            'layers': layers,
            'text': 'some text',
            'debug': 1,
        },
        headers={
            'Referer': f'https://{referer}/some-url/',
        },
    )

    assert response.code == 200

    data = parse_response(response)
    features = fetcher(data)
    assert bool(features.get('feature')) is has_features, features
    assert bool(features.get('uaas_feature')) is has_features, features
