import pytest

from django.core.urlresolvers import reverse

from common import factories


@pytest.fixture
def service_resource(db, owner_role):
    return factories.ServiceResourceFactory()


def test_get_selective_fields(client, service_resource):
    fields = ['id', 'service.slug', 'service.name']
    response = client.json.get(
        reverse('resources-api:serviceresources-detail', args=(service_resource.id,)),
        {'fields': ','.join(fields)}
    )

    assert response.status_code == 200

    result = response.json()
    assert set(result.keys()) == {'id', 'service'}
    assert set(result['service'].keys()) == {'slug', 'name'}
    assert result['service']['name'] == {
        'ru': service_resource.service.name,
        'en': service_resource.service.name_en
    }


def test_get_selective_fields_embedded_serializations(client, service_resource):
    fields = ['id', 'service', 'resource.type']
    response = client.json.get(
        reverse('resources-api:serviceresources-detail', args=(service_resource.id,)),
        {'fields': ','.join(fields)}
    )

    assert response.status_code == 200

    result = response.json()
    assert set(result.keys()) == {'id', 'service', 'resource'}
    assert result['service']
    assert set(result['resource']['type'].keys()) == {
        'category',
        'description',
        'form_link',
        'has_editable_tags',
        'has_multiple_consumers',
        'has_supplier_tags',
        'has_tags',
        'id',
        'code',
        'is_enabled',
        'is_important',
        'name',
        'supplier',
        'tags',
        'usage_tag',
        'dependencies',
        'need_monitoring',
    }


def test_get_selective_fields_embedded_filters(client, service_resource):
    fields = ['id', 'resource.type.name']
    response = client.json.get(
        reverse('resources-api:serviceresources-detail', args=(service_resource.id,)),
        {'fields': ','.join(fields)}
    )

    assert response.status_code == 200

    result = response.json()
    assert set(result.keys()) == {'id', 'resource'}
    assert set(result['resource']['type'].keys()) == {'name'}
    assert result['resource']['type']['name'] == {
        'ru': service_resource.resource.type.name,
        'en': service_resource.resource.type.name
    }


def test_get_selective_fields_redundant_embedded_filters(client, service_resource):
    # если у нас запросили и общий сериализатор, и конкретное поле из него,
    # то отдаем предпочтение более детальному запросу
    fields = ['id', 'resource.type', 'resource.type.name']
    response = client.json.get(
        reverse('resources-api:serviceresources-detail', args=(service_resource.id,)),
        {'fields': ','.join(fields)}
    )

    assert response.status_code == 200

    result = response.json()
    assert set(result.keys()) == {'id', 'resource'}
    assert result['resource']['type']
    assert set(result['resource']['type'].keys()) == {'name'}
    assert result['resource']['type']['name'] == {
        'ru': service_resource.resource.type.name,
        'en': service_resource.resource.type.name
    }


def test_ordering_filter(client, db):
    [factories.ServiceResourceFactory() for _ in range(10)]

    response = client.json.get(
        reverse('resources-api:serviceresources-list'),
        {'ordering': '-id'}
    )
    assert response.status_code == 200

    ids = [_['id'] for _ in response.json()['results']]
    assert ids == sorted(ids, reverse=True)


def test_wrong_ordering_field(client, db):
    response = client.json.get(
        reverse('resources-api:serviceresources-list'),
        {'ordering': '-derp'}
    )

    assert response.status_code == 400
    assert response.json()['error']['code'] == 'bad_request'
    assert response.json()['error']['detail'] == 'api handle does not support sorting by fields: derp'
