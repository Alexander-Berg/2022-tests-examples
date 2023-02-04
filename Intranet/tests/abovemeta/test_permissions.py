import pytest

from django.conf import settings

from intranet.search.tests.helpers.abovemeta_helpers import (
    request_search,
    request_suggest,
    parse_response,
    get_request_params,
)
from intranet.search.tests.helpers.models_helpers import get_revision_data


@pytest.mark.parametrize('scope',  settings.ISEARCH['scopes'].keys())
@pytest.mark.gen_test
def test_search_available_with_permissions(http_client, base_url, requester, scope):
    requester.patch_api_permissions(search=[scope])
    response = yield request_search(http_client, base_url, params={'scope': scope})
    response_scopes = [s['value'] for s in parse_response(response)['scopes']]
    assert response_scopes == [scope]


@pytest.mark.parametrize('scope',  settings.ISEARCH['scopes'].keys())
@pytest.mark.gen_test
def test_search_forbidden_without_permissions(http_client, base_url, requester, scope):
    requester.patch_api_permissions(search=[])
    response = yield request_search(http_client, base_url, params={'scope': scope})
    assert parse_response(response)['scopes'] == []
    assert parse_response(response)['errors'][0]['code'] == 'ERROR_ACL'


@pytest.mark.parametrize('layer',  settings.ISEARCH['suggest']['layers'].keys())
@pytest.mark.parametrize('version',  ['1', '2'])
@pytest.mark.gen_test
def test_suggest_forbidden_without_permissions(http_client, base_url, requester, layer, version):
    requester.patch_api_permissions(suggest=[])
    params = {'layer': layer, 'version': version, 'allow_empty': 1}
    response = yield request_suggest(http_client, base_url, params=params)
    assert layer not in parse_response(response)


@pytest.mark.parametrize('permissions,public_restrict', (
    ([], 'public:"1" & (s_type:"doc" | s_type:"post")'),
    (['search_public_wiki_pages'], 'public:"1"')
))
@pytest.mark.gen_test
def test_search_public_wiki_pages_permission(http_client, base_url, requester,
                                             permissions, public_restrict):
    requester.patch_api_revisions([get_revision_data(search='wiki')])
    requester.patch_api_permissions(common=permissions, search=['search'])
    response = yield request_search(http_client, base_url, {'text': 'test'})  # noqa

    saas_params = get_request_params(requester.called['search.search_results'][0])
    restrict = f'({public_restrict} | acl_users_whitelist:"some_ya_user")'
    assert saas_params['restrict'] == restrict
