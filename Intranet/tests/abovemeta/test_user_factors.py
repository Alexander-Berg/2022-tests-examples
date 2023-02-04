import pytest

from intranet.search.tests.helpers.abovemeta_helpers import (
    request_search,
    request_suggest,
    get_request_params,
)
from intranet.search.tests.helpers.models_helpers import get_revision_data


@pytest.mark.parametrize('period', ('frequently', 'recently'))
@pytest.mark.gen_test
def test_frequently_or_recently_searched_people(http_client, base_url, requester, period):
    requester.patch_api_user_meta(**{f'{period}_searched_people': {'1': 100, '2': 50}})
    requester.patch_api_revisions([get_revision_data(search='people')])
    params = {
        'scope': 'peoplesearch',
        'text': 'test',
    }
    response = yield request_search(http_client, base_url, params)  # noqa
    saas_params = get_request_params(requester.called['search.search_results'][0])

    expected_relev = f'calc=USER_people_{period}_searched:insetany(#group_staff_id,1,2)'
    assert saas_params['relev'] == expected_relev


@pytest.mark.parametrize('period', ('frequently', 'recently'))
@pytest.mark.gen_test
def test_frequently_or_recently_searched_people_suggest(http_client, base_url, requester, period):
    requester.patch_api_user_meta(**{f'{period}_searched_people': {'1': 100, '2': 50}})
    requester.patch_api_revisions([get_revision_data(search='people')])
    params = {
        'layers': 'people',
        'version': 2,
        'text': 'test',
    }
    response = yield request_suggest(http_client, base_url, params)  # noqa
    saas_params = get_request_params(requester.called['search.people'][0])

    expected_relev = f'calc=USER_people_{period}_searched:insetany(#group_staff_id,1,2)'
    assert saas_params['relev'] == expected_relev


@pytest.mark.gen_test
def test_services_is_member(http_client, base_url, requester):
    requester.patch_api_revisions([get_revision_data(search='plan', index='services')])
    params = {
        'scope': 'abcsearch',
        'text': 'test',
    }
    response = yield request_search(http_client, base_url, params)  # noqa
    saas_params = get_request_params(requester.called['search.search_results'][0])

    expected_relev = 'calc=USER_plan_is_member:insetany(#group_member,1)'
    assert saas_params['relev'] == expected_relev
