import pytest
from django.core.urlresolvers import reverse

from plan.resources import models
from plan.roles.models import RoleScope

from common import factories
from utils import iterables_are_equal

pytestmark = [
    pytest.mark.django_db,
    pytest.mark.usefixtures('robot')
]


def get_types(client, **params):
    url = reverse('resources-api:resourcetypes-list')
    params['page_size'] = 1000
    response = client.json.get(url, params)
    data = response.json()

    return {type['name']['ru']: type for type in data['results']}


def test_get_type(base_data, client):
    url = reverse('resources-api:resourcetypes-detail', args=[base_data['test_type'].id])
    response = client.json.get(url)
    assert response.status_code == 200

    result = response.json()
    assert result['id'] == base_data['test_type'].id
    assert result['name'] == {'ru': base_data['test_type'].name, 'en': base_data['test_type'].name}
    assert result['description'] == {'ru': base_data['test_type'].description, 'en': ''}


def test_get_type_all_consumer_scopes(base_data, client):
    resource_type = base_data['test_type']
    resource_type.consumer_scopes = [RoleScope.objects.first()]
    resource_type.save()

    url = reverse('resources-api:resourcetypes-roles', args=[resource_type.id])
    response = client.json.get(url)
    assert response.status_code == 200
    assert response.json()['has_all_consumer_scopes'] is False

    all_scopes = list(RoleScope.objects.all())
    resource_type.consumer_scopes = all_scopes
    resource_type.save()

    response = client.json.get(url)
    assert response.status_code == 200
    assert response.json()['has_all_consumer_scopes'] is True


def test_all_types(base_data, client, django_assert_num_queries):
    """Запрос типов без аргументов возвращает все типы"""

    with django_assert_num_queries(10):
        data = get_types(client)

    all_types = {type.name: type for type in models.ResourceType.objects.all()}

    assert len(data) == len(all_types)
    assert set(data.keys()) == {name for name in all_types}


def test_list_service_types_without_child_param(client, base_data):
    """Запрос ресурсов листового сервиса должен возвращать тот же ответ, что и
    этот же запрос с параметром with_childs=False"""
    services = base_data['services']
    data = get_types(client, service=services['broauto'].id)
    data_without_childs = get_types(client,
                                    service=services['broauto'].id,
                                    with_childs=False)
    assert data == data_without_childs


def test_parent_service_types_without_child_param(client, base_data):
    """Запрос ресурсов родительского сервиса без учета детей в определенной
    ситуации должен возвращать меньше типов, чем с детьми"""
    services = base_data['services']
    data = get_types(client, service=services['mobilebrowser'].id, with_childs=True)
    data_without_childs = get_types(client, service=services['mobilebrowser'].id)
    data_keys = set(data.keys())
    data_without_childs_keys = set(data_without_childs.keys())

    assert data_without_childs_keys != data_keys
    assert data_without_childs_keys | data_keys == data_keys


def test_types_parent_service_without_child(client, base_data):
    """Запрос ресурсов сервиса с параметром with_childs=False должен выдавать
    только типы ресурсов данного сервиса"""
    services = base_data['services']
    data = get_types(client, service=services['mobilebrowser'].id, with_childs=False)

    assert list(data.keys()) == ['SRV.SERVERS']


def test_types_parent_service(client, base_data):
    """Запрос ресурсов сервиса с параметром with_childs должен выдавать
    типы ресурсов данного сервиса и потомков"""
    services = base_data['services']
    data = get_types(client, service=services['mobilebrowser'].id, with_childs=True)

    all_types = {'NET.SWITCHES', 'SRV.NODES', 'SRV.SERVERS', 'NET.DWDM'}
    assert set(data.keys()) == all_types


def test_types_without_form(client):
    resource_type = factories.ResourceTypeFactory()

    data = get_types(client, form_id__isnull=True)
    assert list(data.keys()) == [resource_type.name]

    resource_type.form_id = 100500
    resource_type.save()

    data = get_types(client, form_id__isnull=True)
    assert list(data.keys()) == []


def test_filter_by_category(client):
    category = factories.ResourceTypeCategoryFactory()
    type1 = factories.ResourceTypeFactory(category=category)
    type2 = factories.ResourceTypeFactory()
    category2 = factories.ResourceTypeCategoryFactory()

    data = get_types(client)
    assert set(data.keys()) == set((type1.name, type2.name))

    data = get_types(client, category=category.id)
    assert list(data.keys()) == [type1.name]

    data = get_types(client, category=[category.id, category2.id])
    assert list(data.keys()) == [type1.name]


def test_filter_by_supplier(client):
    type1 = factories.ResourceTypeFactory()
    type2 = factories.ResourceTypeFactory()
    type3 = factories.ResourceTypeFactory()

    data = get_types(client)
    assert set(data.keys()) == set((type1.name, type2.name, type3.name))

    data = get_types(client, supplier=type1.supplier.id)
    assert list(data.keys()) == [type1.name]

    data = get_types(client, supplier=[type1.supplier.id, type2.supplier.id])
    assert set(data.keys()) == set((type1.name, type2.name))


def test_get_allowed_roles_by_default(client, owner_role, responsible_role):
    resource_type = factories.ResourceTypeFactory()
    supplier_role = factories.RoleFactory()
    consumer_role = factories.RoleFactory()
    resource_type.supplier_roles.add(supplier_role)
    resource_type.consumer_roles.add(consumer_role)

    response = client.json.get(
        reverse('resources-api:resourcetypes-roles', args=[resource_type.id])
    )
    assert response.status_code == 200

    result = response.json()
    supplier_roles = [supplier_role.code]
    consumer_roles = [consumer_role.code]
    roles = supplier_roles + consumer_roles
    assert iterables_are_equal([role_data['code'] for role_data in result['supplier_roles']], supplier_roles)
    assert iterables_are_equal([role_data['code'] for role_data in result['consumer_roles']], consumer_roles)
    assert iterables_are_equal([role_data['code'] for role_data in result['results']], roles)
    assert result['count'] == len(roles)


def test_get_allowed_roles_with_scopes(client, owner_role, responsible_role):
    resource_type = factories.ResourceTypeFactory()
    dev_scope = factories.RoleScopeFactory(slug='development')
    dev = factories.RoleFactory(scope=dev_scope)
    role = factories.RoleFactory()
    resource_type.supplier_scopes.add(dev_scope)
    resource_type.supplier_roles.add(role)

    response = client.json.get(reverse('resources-api:resourcetypes-roles', args=[resource_type.id]))
    assert response.status_code == 200

    result = response.json()
    supplier_roles = [role.code, dev.code]
    consumer_roles = []
    roles = supplier_roles + consumer_roles
    assert iterables_are_equal([role_data['code'] for role_data in result['supplier_roles']], supplier_roles)
    assert iterables_are_equal([role_data['code'] for role_data in result['consumer_roles']], consumer_roles)
    assert iterables_are_equal([role_data['code'] for role_data in result['results']], roles)
    assert result['count'] == len(roles)


def test_v4_pagination(client, person):
    client.login(person.username)
    expected = {factories.ResourceTypeFactory() for i in range(100)}
    reversed_url = reverse('api-v4:resource-types-list')
    response = client.json.get(reversed_url)
    actual = [service for service in response.json()['results']]
    while response.json()['next'] is not None:
        assert len(response.json()['results']) == 20
        assert reversed_url + '?cursor=' in response.json()['next']
        response = client.json.get(response.json()['next'])
        actual.extend([service for service in response.json()['results']])
    actual = {
        resource_type for resource_type in models.ResourceType.objects.filter(
            id__in=[resource_type['id'] for resource_type in actual]
        )
    }
    assert actual == expected


def test_has_multiple_consumers(client, person):
    client.login(person.username)
    resource_type = factories.ResourceTypeFactory(idempotent_request=True, has_multiple_consumers=True)
    reversed_url = reverse('api-v4:resource-types-list')
    result = client.json.get(reversed_url, data={'fields': 'id,has_multiple_consumers,code'}).json()['results'][0]
    assert result['code'] == resource_type.code
    assert not result['has_multiple_consumers']
