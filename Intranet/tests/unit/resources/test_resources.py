from urllib.parse import urlparse, parse_qs

import pytest
from django.core.urlresolvers import reverse

from plan.resources import models
from common import factories

BOT_SOURCE_SLUG = 'test_bot'
TEST_SOURCE_SLUG = 'test_test'

pytestmark = [
    pytest.mark.django_db,
    pytest.mark.usefixtures('robot')
]


def querystring(url):
    return parse_qs(urlparse(url).query)


def get_pagination_resources(client, url='resources-api:resources-list', **params):
    resources = {}
    next_page = 1
    while next_page is not None:
        params['page'] = next_page
        data = client.json.get(reverse(url), params).json()
        for resource in data['results']:
            resources[resource['external_id']] = resource
        next_page = querystring(data['next'])['page'][0] if data['next'] else None

    return resources


def test_all_resources(client, base_data, django_assert_num_queries):
    """Запрос всех ресурсов должен возвращать все объекты из базы"""

    with django_assert_num_queries(16):
        # повторяется дважды, так как get_pagination_resources пролистывает две страницы ресурсов
        # 2 select staff, content_type
        # 1 select count(resource)
        # 1 select resource
        # 1 prefetch recourcetag, resourcetagcategory for ResourceType
        # 1 prefetch resourcetype, resourcetype.supplier
        # 2 pg_in_recovery, waffle
        data = get_pagination_resources(client)

    db_resources = models.Resource.objects.all()
    assert len(data) == db_resources.count()

    for db_resource in db_resources:
        assert db_resource.external_id in data


def test_resources_with_type_field(client, base_data, django_assert_num_queries):
    """resources-list не порождает большое количество запросов с полем type."""
    with django_assert_num_queries(7):
        # 2 select staff, content_type
        # 1 select resource
        # 1 prefetch recourcetag, resourcetagcategory for resource__type__tags
        # 1 prefetch resourcetype, resourcetype.supplier for resource__type__dependencies
        # 2 pg_in_recovery, waffle
        data = get_pagination_resources(client, url='api-v4:resource-list', fields='id,external_id,type')

    assert len([r['type'] for r in data.values()]) == models.Resource.objects.count()


def test_resources_type_filter(client, base_data):
    """Тест фильра по типу"""
    dwdm_type = models.ResourceType.objects.filter(name='NET.DWDM').get()
    switches_type = models.ResourceType.objects.filter(name='NET.SWITCHES').get()
    data = get_pagination_resources(client, type=[dwdm_type.id, switches_type.id])

    assert len(data) == 2
    assert '398625' in data
    assert '403984' in data


def test_resource_supplier_filter(client, base_data):
    """Тест фильтра по источнику"""
    test_supplier_id = base_data['test_supplier'].id
    data = get_pagination_resources(client, supplier=test_supplier_id)

    db_data = models.Resource.objects.filter(type__supplier__id=test_supplier_id)

    assert 1 == len(data) == len(db_data)
    assert list(data.values())[0]['type']['supplier']['id'] == db_data[0].type.supplier.id


def test_get_resource_attributes(client):
    res = factories.ResourceFactory(
        attributes={'a': 'b'},
    )
    sr = factories.ServiceResourceFactory(resource=res)

    response = client.json.get(reverse('resources-api:resources-detail', args=(res.id,)))
    data = response.json()
    assert data['attributes'] == [{'name': 'a', 'value': 'b'}]

    response = client.json.get(reverse('resources-api:serviceresources-detail', args=(sr.id,)))
    data = response.json()
    assert data['resource']['attributes'] == [{'name': 'a', 'value': 'b'}]
