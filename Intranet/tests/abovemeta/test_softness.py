import pytest

from django.conf import settings

from intranet.search.tests.helpers.abovemeta_helpers import get_request_params, request_search
from intranet.search.tests.helpers.models_helpers import get_revision_data, Organization


ALL_SAAS_SCOPES = [s for s in settings.ISEARCH['scopes'] if s not in {'mlsearch', 'yasensearch', 'stackoverflowsearch'}]


@pytest.fixture(scope='function')
def all_revisions(requester):
    all_revisions = []
    org = Organization.build()
    for search, data in settings.ISEARCH['searches']['base'].items():
        for index in data['indexes']:
            all_revisions.append(get_revision_data(search=search, index=index, organization=org))
    requester.patch_api_revisions(all_revisions)


def get_expected_request(scope):
    return 'z_base:(%request%)' if scope == 'candidatesearch' else '%request%'


@pytest.mark.parametrize('scope', ALL_SAAS_SCOPES)
@pytest.mark.gen_test
def test_softness_disabled_by_default(http_client, base_url, requester, all_revisions, scope):
    response = yield request_search(http_client, base_url, params={'scope': scope, 'text': 'test'})  # noqa

    saas_params = get_request_params(requester.called['search.search_results'][0])
    assert saas_params['template'] == get_expected_request(scope)


@pytest.mark.parametrize('scope', ALL_SAAS_SCOPES)
@pytest.mark.gen_test
def test_enable_by_feature(http_client, base_url, requester, all_revisions, scope):
    params = {f'feature.{scope}_search_results_softness': 6, 'scope': scope, 'text': 'test'}
    response = yield request_search(http_client, base_url, params)  # noqa

    saas_params = get_request_params(requester.called['search.search_results'][0])
    expected = f'{get_expected_request(scope)} softness:6'
    assert saas_params['template'] == expected


@pytest.mark.parametrize('scope', ALL_SAAS_SCOPES)
@pytest.mark.gen_test
def test_softness_and_filters(http_client, base_url, requester, all_revisions, scope):
    """ Softness не затирает фильтры, фильтры не затирают softness """
    params = {
        f'feature.{scope}_search_results_softness': 6,
        'zone.name': 'some_filter',
        'scope': scope,
        'text': 'test',
    }
    response = yield request_search(http_client, base_url, params)  # noqa

    saas_params = get_request_params(requester.called['search.search_results'][0])
    expected = f'{get_expected_request(scope)} softness:6 << z_name:"some_filter"'
    assert saas_params['template'] == expected
