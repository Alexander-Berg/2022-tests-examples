import pytest

from intranet.search.tests.helpers.models_helpers import get_revision_data
from intranet.search.tests.helpers.abovemeta_helpers import request_search, get_request_params


def request_candidate_search(http_client, base_url, requester, text='test', **kwargs):
    params = dict(scope='candidatesearch', text=text, **kwargs)
    requester.patch_api_revisions([get_revision_data(search='femida', index='candidates')])
    return request_search(http_client, base_url, params=params)


@pytest.mark.gen_test
def test_template_used_in_saas_request(http_client, base_url, requester):
    response = yield request_candidate_search(http_client, base_url, requester)  # noqa

    saas_params = get_request_params(requester.called['search.search_results'][0])
    assert saas_params['text'] == 'test'
    assert saas_params['template'] == 'z_base:(%request%)'


@pytest.mark.gen_test
def test_filter_by_zone_added_to_template(http_client, base_url, requester):
    params = {'text': 'test', 'zone.city': 'moscow'}
    response = yield request_candidate_search(http_client, base_url, requester, **params)  # noqa

    saas_params = get_request_params(requester.called['search.search_results'][0])
    assert saas_params['text'] == params['text']
    expected = 'z_base:(%request%) << z_city:"{}"'.format(params['zone.city'])
    assert saas_params['template'] == expected


@pytest.mark.gen_test
def test_filter_by_facet_added_to_template(http_client, base_url, requester):
    params = {'text': 'test', 'facet.status': 'active'}
    response = yield request_candidate_search(http_client, base_url, requester, **params)  # noqa

    saas_params = get_request_params(requester.called['search.search_results'][0])
    assert saas_params['text'] == params['text']
    expected = 'z_base:(%request%) << s_status:"{}"'.format(params['facet.status'])
    assert saas_params['template'] == expected


@pytest.mark.gen_test
def test_filter_by_attr_added_to_template(http_client, base_url, requester):
    params = {'text': 'test', 'attr.created__gte': '12345678'}
    response = yield request_candidate_search(http_client, base_url, requester, **params)  # noqa

    saas_params = get_request_params(requester.called['search.search_results'][0])
    assert saas_params['text'] == params['text']
    expected = 'z_base:(%request%) << i_created:>="{}"'.format(params['attr.created__gte'])
    assert saas_params['template'] == expected


@pytest.mark.gen_test
def test_filter_by_name_changed_zone(http_client, base_url, requester):
    name = dict(first_name='ivan', last_name='ivanov')
    requester.patch_api_begemot(**name)
    params = {'feature.femida_use_name_zone': 1}
    response = yield request_candidate_search(http_client, base_url, requester, **params)  # noqa

    saas_params = get_request_params(requester.called['search.search_results'][0])
    assert saas_params['text'] == '{first_name} {last_name}'.format(**name)
    assert saas_params['template'] == 'z_femida_name:(%request%)'


@pytest.mark.gen_test
def test_filter_by_name_disabled_by_feature(http_client, base_url, requester):
    requester.patch_api_begemot(first_name='ivan', last_name='ivanov')
    params = {'feature.femida_use_name_zone': 0, 'text': 'test'}
    response = yield request_candidate_search(http_client, base_url, requester, **params)  # noqa

    saas_params = get_request_params(requester.called['search.search_results'][0])
    # с выключенной фичей никак не используем имя, распаршенное бегемотом
    assert saas_params['text'] == params['text']
    assert saas_params['template'] == 'z_base:(%request%)'


@pytest.mark.gen_test
def test_filter_by_name_works_only_if_begemot_parsed_name(http_client, base_url, requester):
    requester.patch_api_begemot(first_name='', last_name='')

    params = {'feature.femida_use_name_zone': 1, 'text': 'test'}
    response = yield request_candidate_search(http_client, base_url, requester, **params)  # noqa

    saas_params = get_request_params(requester.called['search.search_results'][0])
    # если бегемот ничего не распарсил, даже если фича включена, ищем везде
    assert saas_params['text'] == params['text']
    assert saas_params['template'] == 'z_base:(%request%)'
