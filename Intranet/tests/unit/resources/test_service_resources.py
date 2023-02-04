from functools import partial
from urllib.parse import urlencode

import pretend
import pytest
from django.core.urlresolvers import reverse
from freezegun import freeze_time
from mock import patch

from plan import settings
from plan.api.base import ABCCursorPagination
from plan.resources.models import ServiceResource
from plan.common.utils.oauth import get_abc_zombik
from plan.history.models import HistoryRawEntry
from common import factories
from utils import iterables_are_equal
from plan.resources.tasks import revoke_dismissed_robots_resources

BOT_SOURCE_SLUG = 'test_bot'
TEST_SOURCE_SLUG = 'test_test'

pytestmark = pytest.mark.django_db


@pytest.fixture
def infrastructure_data():
    service = factories.ServiceFactory()
    arcadia_type = factories.ResourceTypeFactory(code='arcadia')
    warden_type = factories.ResourceTypeFactory(code='warden')

    arcadia_resource = factories.ResourceFactory(
        type=arcadia_type,
        attributes={'dirs': ['frontend/services/abc', 'intranet/plan']}
    )

    warden_resource = factories.ResourceFactory(
        type=warden_type,
        link='https://warden.z.yandex-team.ru/components/abcd/s/abc/',
        attributes={
            'goalUrl': 'https://goals.yandex-team.ru/filter?goal=106971',
            'humanReadableName': 'ABC',
            'infraPreset': 'Zft6tfk3eXT',
            'spiChat': {
                'chatLink': 'https://t.me/joinchat/BdtakRQm0yD8dS3x0BGCcQ',
                'name': 'SpiCoordinationChat'},
            'state': 'INVALID',
            'tier': 'B',
        }
    )

    warden_resource_1 = factories.ResourceFactory(
        type=warden_type,
        attributes={
            'infraPreset': 'Zft6tfk3eXT23',
        }
    )

    for resource in [warden_resource, warden_resource_1, arcadia_resource]:
        factories.ServiceResourceFactory(
            service=service,
            resource=resource,
        )
    return pretend.stub(
        service=service
    )


@pytest.mark.usefixtures('robot')
def test_get_resource(client, django_assert_num_queries):
    consumer_approver = factories.StaffFactory()
    granter = factories.StaffFactory()
    depriver = factories.StaffFactory()
    category = factories.ResourceTagCategoryFactory()
    tag = factories.ResourceTagFactory(category=category)
    sr = factories.ServiceResourceFactory(
        granter=granter, depriver=depriver, supplier_approver=get_abc_zombik(), consumer_approver=consumer_approver
    )
    sr.tags.add(tag)

    with django_assert_num_queries(12):
        response = client.json.get(reverse('resources-api:serviceresources-detail', args=(sr.id,)))
    assert response.status_code == 200

    response = response.json()
    assert response['consumer_approver']['id'] == consumer_approver.staff_id
    assert response['supplier_approver'] is None
    assert response['depriver']['id'] == depriver.staff_id
    assert response['granter']['id'] == granter.staff_id

    tags = response['tags']
    assert len(tags) == 1
    assert tags[0]['slug'] == tag.slug
    assert tags[0]['category'] == {
        'id': tag.category.id,
        'name': {'ru': tag.category.name, 'en': tag.category.name_en},
        'slug': tag.category.slug,
    }


@pytest.mark.parametrize('endpoint_path', ('resources-api:serviceresources-list', 'api-v3:resource-consumer-list', 'api-v4:resource-consumer-list'))
def test_get_resource_with_nonexistent_fields(client, endpoint_path):
    resource = factories.ResourceFactory()
    service_resource = factories.ServiceResourceFactory(resource=resource)
    response = client.json.get(reverse(endpoint_path), data={'fields': 'id,resource.example'})
    assert response.status_code == 200
    data = response.json()
    assert data['results'] == [{'id': service_resource.id}]


@pytest.mark.usefixtures('robot')
def test_get_resource_num_queries(client, django_assert_num_queries):
    """
    GET /api/v3/resources/consumers/
    """
    consumer_approver = factories.StaffFactory()
    granter = factories.StaffFactory()
    depriver = factories.StaffFactory()
    category = factories.ResourceTagCategoryFactory()
    tag = factories.ResourceTagFactory(category=category)

    for _ in range(10):
        sr = factories.ServiceResourceFactory(
            granter=granter, depriver=depriver, supplier_approver=get_abc_zombik(), consumer_approver=consumer_approver,
        )
        sr.tags.add(tag)

    with django_assert_num_queries(10):
        response = client.json.get(reverse('api-v3:resource-consumer-list'))
    assert response.status_code == 200

    with django_assert_num_queries(10):
        response = client.json.get(
            reverse('api-v3:resource-consumer-list'),
            params={'fields': 'id,resource.id,state_display,obsolete_id'},
        )
    assert response.status_code == 200


@pytest.mark.usefixtures('robot')
@pytest.mark.parametrize('endpoint_path', (
    'resources-api:serviceresources-detail',
    'api-v3:resource-consumer-detail',
    'api-v4:resource-consumer-detail',
    'api-frontend:resource-consumer-detail',
))
def test_get_resource_detail_num_queries(client, django_assert_num_queries, django_assert_num_queries_lte, endpoint_path):
    """
    GET /api/.../resources/consumers/<id>/?fields=...
    """
    consumer_approver = factories.StaffFactory()
    granter = factories.StaffFactory()
    depriver = factories.StaffFactory()
    category = factories.ResourceTagCategoryFactory()
    tag = factories.ResourceTagFactory(category=category)
    service_resource = factories.ServiceResourceFactory(
        granter=granter, depriver=depriver, supplier_approver=get_abc_zombik(), consumer_approver=consumer_approver
    )
    service_resource.tags.add(tag)

    with django_assert_num_queries(12):
        response = client.json.get(reverse('resources-api:serviceresources-detail', args=(service_resource.id,)))
    assert response.status_code == 200

    waffle_query_num = 1
    data = response.json()
    all_fields = []
    stack = [(data, '')]
    while stack:
        element, prefix = stack.pop()
        for item, value in element.items():
            new_prefix = f'{prefix}.{item}' if prefix else item
            if isinstance(value, dict):
                stack.append((value, new_prefix))
            all_fields.append(new_prefix)
    url = reverse(endpoint_path, args=(service_resource.id,))
    for field in all_fields:
        num_queries = 12 if field == 'actions' else 6
        with django_assert_num_queries_lte(num_queries + waffle_query_num):
            response = client.json.get(url, data={'fields': field})
        assert response.status_code == 200


@pytest.mark.usefixtures('robot')
@pytest.mark.parametrize('endpoint_path', (
    'resources-api:serviceresources-list',
    'api-v3:resource-consumer-list',
    'api-v4:resource-consumer-list',
    'api-frontend:resource-consumer-list',
))
def test_get_resource_with_fields_num_queries(client, django_assert_num_queries_lte, endpoint_path):
    """
    GET /api/v3(v4)/resources/consumers/?fields=...

    Проверим, что при запросе любого из доступных полей количество запросов к базе не увеличивается
    """
    consumer_approver = factories.StaffFactory()
    granter = factories.StaffFactory()
    depriver = factories.StaffFactory()
    category = factories.ResourceTagCategoryFactory()
    tag = factories.ResourceTagFactory(category=category)

    for _ in range(10):
        sr = factories.ServiceResourceFactory(
            granter=granter, depriver=depriver, supplier_approver=get_abc_zombik(), consumer_approver=consumer_approver
        )
        sr.tags.add(tag)

    # v4 возвращает по умолчанию не все поля, за всеми полями сходим в старую ручку
    url = reverse('resources-api:serviceresources-list')
    response = client.json.get(url)
    assert response.status_code == 200
    data = response.json()
    all_fields = []
    stack = [(data['results'][0], '')]
    while stack:
        element, prefix = stack.pop()
        for item, value in element.items():
            new_prefix = f'{prefix}.{item}' if prefix else item
            if isinstance(value, dict):
                stack.append((value, new_prefix))
            all_fields.append(new_prefix)

    waffle_query_count = 1
    url = reverse(endpoint_path)
    for field in all_fields:
        num_queries = 55 if field == 'actions' else 7
        with django_assert_num_queries_lte(num_queries + waffle_query_count):
            response = client.json.get(url, data={'fields': field})
        assert response.status_code == 200


@pytest.mark.usefixtures('robot')
def test_get_resource_v4_page_size(client):
    """
    GET /api/v4/resources/consumers/?page_size=5
    """
    consumer_approver = factories.StaffFactory()
    granter = factories.StaffFactory()
    depriver = factories.StaffFactory()
    category = factories.ResourceTagCategoryFactory()
    tag = factories.ResourceTagFactory(category=category)

    for _ in range(10):
        sr = factories.ServiceResourceFactory(
            granter=granter, depriver=depriver, supplier_approver=get_abc_zombik(), consumer_approver=consumer_approver
        )
        sr.tags.add(tag)

    response = client.json.get(reverse('api-v4:resource-consumer-list'))
    assert response.status_code == 200
    data = response.json()
    assert len(data['results']) <= ABCCursorPagination.page_size

    page_size = 5
    response = client.json.get(reverse('api-v4:resource-consumer-list'), data={'page_size': page_size})
    assert response.status_code == 200
    data = response.json()
    assert len(data['results']) == page_size


@pytest.mark.usefixtures('robot')
def test_get_resource_v4_unwrap_pagination(client, django_assert_num_queries_lte):
    """
    GET /api/v4/resources/consumers/?page_size=5
    """
    # Проверим, что при использовании пагинатора получаем все объекты и в нужном порядке при обходе по next_url
    consumer_approver = factories.StaffFactory()
    granter = factories.StaffFactory()
    depriver = factories.StaffFactory()
    category = factories.ResourceTagCategoryFactory()
    tag = factories.ResourceTagFactory(category=category)
    resource_type = factories.ResourceTypeFactory()

    for _ in range(10):
        resource = factories.ResourceFactory(type=resource_type)
        sr = factories.ServiceResourceFactory(
            granter=granter, depriver=depriver, supplier_approver=get_abc_zombik(), consumer_approver=consumer_approver,
            resource=resource
        )
        sr.tags.add(tag)
    url = '%s?%s' % (reverse('api-v4:resource-consumer-list'), urlencode({'page_size': 5, 'type': resource_type.id}))
    received_ids = []
    # При первом запросе - используем unwrap, по этому на 1 sql запрос больше
    waffle_switch_request = 1 + 1  # middleware + middleware-'deny-without-permission'
    expected_queries_count = 5 + waffle_switch_request

    while url:
        with django_assert_num_queries_lte(expected_queries_count):
            response = client.json.get(url)
        assert response.status_code == 200
        data = response.json()
        assert len(data['results']) == 5
        received_ids += [service['id'] for service in data['results']]
        url = data['next']
        expected_queries_count = 5 + waffle_switch_request
    assert len(received_ids) == ServiceResource.objects.count()
    assert received_ids == list(ServiceResource.objects.values_list('id', flat=True).order_by('pk'))


@pytest.mark.parametrize('endpoint_path', ('resources-api:serviceresources-detail', 'api-v3:resource-consumer-detail', 'api-v4:resource-consumer-detail'))
def test_get_obsolete_resource(client, endpoint_path):
    obsolete_resource = factories.ResourceFactory(attributes={'a': 'b'})
    obsolete_service_resource = factories.ServiceResourceFactory(resource=obsolete_resource, state='obsolete')
    staff = factories.StaffFactory()
    resource = factories.ResourceFactory(obsolete=obsolete_resource)
    service_resource = factories.ServiceResourceFactory(
        resource=resource,
        obsolete=obsolete_service_resource,
        requester=staff,
    )

    response = client.json.get(
        reverse(endpoint_path, args=[service_resource.id]),
        data={'fields': 'id,obsolete_id,requester'}
    )

    data = response.json()
    assert data['obsolete_id'] == obsolete_service_resource.id
    assert data['requester']['login'] == staff.login


def test_get_yp_quota_resource(client, person, yp_quota_service_resource_data):
    service_resource = yp_quota_service_resource_data.service_resource

    client.login(person.login)

    response = client.json.get(
        reverse('resources-api:serviceresources-detail', args=[service_resource.id]),
    )
    assert response.status_code == 200

    resource_attributes = response.json()['resource']['attributes']
    assert resource_attributes == yp_quota_service_resource_data.expected_api_attributes


@pytest.mark.parametrize('endpoint_path', ('resources-api:serviceresources-list', 'api-v3:resource-consumer-list', 'api-v4:resource-consumer-list'))
@pytest.mark.usefixtures('robot')
def test_consume_resource(client, service_with_owner, owner_role, endpoint_path):
    # Потребить ресурс
    sr = factories.ServiceResourceFactory()
    sr.type.has_multiple_consumers = True
    sr.type.save()
    sr.type.consumer_roles.add(owner_role)

    client.login(service_with_owner.owner.login)
    result = client.json.post(
        reverse(endpoint_path),
        {
            'service': service_with_owner.id,
            'resource': sr.resource.id,
        }
    )

    assert result.status_code == 201

    second_sr = ServiceResource.objects.last()
    assert second_sr.service == service_with_owner
    assert second_sr.resource == sr.resource
    assert second_sr.requester == service_with_owner.owner
    assert second_sr.state == ServiceResource.APPROVED

    result = client.json.post(
        reverse(endpoint_path),
        {
            'service': service_with_owner.id,
            'resource': sr.resource.id,
        }
    )

    # Нельзя иметь две активные привязки ресурса к одному сервису
    assert result.status_code == 400


@pytest.mark.parametrize('endpoint_path', ('resources-api:serviceresources-list', 'api-v3:resource-consumer-list', 'api-v4:resource-consumer-list'))
@pytest.mark.usefixtures('robot')
def test_consume_resource_with_tags(client, service_with_owner, owner_role, endpoint_path):
    # Потребить ресурс с добавочными тегами
    sr = factories.ServiceResourceFactory()
    sr.type.has_multiple_consumers = True
    sr.type.save()
    sr.type.consumer_roles.add(owner_role)

    tag1 = factories.ResourceTagFactory()
    tag2 = factories.ResourceTagFactory()

    client.login(service_with_owner.owner.login)
    result = client.json.post(
        reverse(endpoint_path),
        {
            'service': service_with_owner.id,
            'resource': sr.resource.id,
            'tags': [tag1.id],
            'supplier_tags': [tag2.id]
        }
    )

    assert result.status_code == 201

    second_sr = ServiceResource.objects.last()
    assert second_sr.service == service_with_owner
    assert second_sr.resource == sr.resource
    assert second_sr.requester == service_with_owner.owner
    assert second_sr.state == ServiceResource.APPROVED

    assert list(second_sr.tags.all()) == [tag1]
    assert list(second_sr.supplier_tags.all()) == [tag2]


@pytest.mark.parametrize('endpoint_path', ('resources-api:serviceresources-list', 'api-v3:resource-consumer-list', 'api-v4:resource-consumer-list'))
def test_consume_resource_by_stranger(client, service_with_owner, endpoint_path, staff_factory):
    # Кто попало не может потребить ресурс
    sr = factories.ServiceResourceFactory()
    staff = staff_factory()

    client.login(staff.login)
    result = client.json.post(
        reverse(endpoint_path),
        {
            'service': service_with_owner.id,
            'resource': sr.resource.id,
        }
    )

    assert result.status_code == 400
    assert result.json()['error']['detail'] == 'Вы не можете запросить этот ресурс'


@pytest.mark.parametrize('endpoint_path', ('resources-api:serviceresources-list', 'api-v3:resource-consumer-list', 'api-v4:resource-consumer-list'))
def test_consume_resource_without_multiple_consuming(client, service_with_owner, owner_role, endpoint_path):
    # Нельзя потребить ресурс, если это запрещено в типе ресурса
    sr = factories.ServiceResourceFactory()
    sr.type.consumer_roles.add(owner_role)

    client.login(service_with_owner.owner.login)
    result = client.json.post(
        reverse(endpoint_path),
        {
            'service': service_with_owner.id,
            'resource': sr.resource.id,
        }
    )

    assert result.status_code == 400
    assert result.json()['error']['detail'] == 'Многократное использование ресурсов запрещено'


@pytest.mark.parametrize('endpoint_path', ('resources-api:serviceresources-detail', 'api-v3:resource-consumer-detail', 'api-v4:resource-consumer-detail'))
def test_grant_resource(client, service_with_owner, owner_role, endpoint_path):
    # Выдать подтвержденный ресурс
    sr = factories.ServiceResourceFactory(state=ServiceResource.APPROVED)
    sr.type.supplier = service_with_owner
    sr.type.supplier_roles.add(owner_role)
    sr.type.save()

    client.login(service_with_owner.owner.login)
    result = client.json.patch(
        reverse(endpoint_path, args=[sr.id]),
        {
            'state': 'granted',
        }
    )

    assert result.status_code == 200

    sr.refresh_from_db()
    assert sr.state == 'granted'
    assert sr.granter == service_with_owner.owner


@pytest.mark.parametrize('endpoint_path', ('resources-api:serviceresources-detail', 'api-v3:resource-consumer-detail', 'api-v4:resource-consumer-detail'))
def test_move_resource_to_wrong_state(client, service_with_owner, owner_role, endpoint_path):
    # Сейчас мы разрешаем только выдавать подтвержденные ресурсы
    sr = factories.ServiceResourceFactory()
    sr.type.supplier = service_with_owner
    sr.type.consumer_roles.add(owner_role)
    sr.type.save()

    client.login(service_with_owner.owner.login)
    result = client.json.patch(
        reverse(endpoint_path, args=[sr.id]),
        {
            'state': 'deprived',
        }
    )

    assert result.status_code == 400
    assert result.json()['error']['detail'] == 'Эта смена состояния объекта не разрешена'


@pytest.mark.parametrize('endpoint_path', ('resources-api:serviceresources-detail', 'api-v3:resource-consumer-detail', 'api-v4:resource-consumer-detail'))
def test_grant_resource_by_stranger(client, service_with_owner, endpoint_path):
    # Кто попало не может выдать подтвержденный ресурс
    sr = factories.ServiceResourceFactory(state=ServiceResource.APPROVED)

    client.login(service_with_owner.owner.login)
    result = client.json.patch(
        reverse(endpoint_path, args=[sr.id]),
        {
            'state': 'granted',
        }
    )

    assert result.status_code == 403
    assert result.json()['error']['detail'] == 'Вы не можете выдать ресурс чужого поставщика'


@pytest.mark.parametrize('endpoint_path', ('resources-api:serviceresources-detail', 'api-v3:resource-consumer-detail', 'api-v4:resource-consumer-detail'))
def test_edit_resource_tags(client, endpoint_path):
    service = factories.ServiceFactory()
    tag1 = factories.ResourceTagFactory(service=service)
    tag2 = factories.ResourceTagFactory(service=service)
    sr = factories.ServiceResourceFactory(service=service)

    patch = partial(
        client.json.patch,
        reverse(endpoint_path, args=(sr.id,))
    )

    response = patch({'tags': [tag1.id]})
    assert response.status_code == 200

    tags = response.json()['tags']
    assert len(tags) == 1
    sr.refresh_from_db()
    assert sr.tags.count() == 1
    assert sr.tags.get() == tag1

    response = patch({'tags': [tag2.id]})
    assert response.status_code == 200

    tags = response.json()['tags']
    assert len(tags) == 1
    sr.refresh_from_db()
    assert sr.tags.count() == 1
    assert sr.tags.get() == tag2

    response = patch({'tags': [tag1.id, tag2.id]})
    assert response.status_code == 200

    tags = response.json()['tags']
    assert len(tags) == 2
    sr.refresh_from_db()
    assert sr.tags.count() == 2


@pytest.mark.parametrize('endpoint_path', ('resources-api:serviceresources-detail', 'api-v3:resource-consumer-detail', 'api-v4:resource-consumer-detail'))
def test_add_free_tag(client, endpoint_path):
    tag1 = factories.ResourceTagFactory()
    sr = factories.ServiceResourceFactory()

    response = client.json.patch(
        reverse(endpoint_path, args=(sr.id,)),
        {
            'tags': [tag1.id],
        }
    )
    assert response.status_code == 200

    sr.refresh_from_db()
    assert sr.tags.count() == 1
    assert sr.tags.get() == tag1


@pytest.mark.parametrize('endpoint_path', ('resources-api:serviceresources-detail', 'api-v3:resource-consumer-detail', 'api-v4:resource-consumer-detail'))
def test_edit_supplier_tags_tagless_type(client, endpoint_path):
    service = factories.ServiceFactory()
    tag = factories.ResourceTagFactory(service=service)

    supplier = factories.ServiceFactory()
    supplier_tag = factories.ResourceTagFactory(service=supplier)

    resource_type = factories.ResourceTypeFactory(supplier=supplier)
    resource = factories.ResourceFactory(type=resource_type)
    sr = factories.ServiceResourceFactory(service=service, resource=resource)

    patch = partial(
        client.json.patch,
        reverse(endpoint_path, args=(sr.id,))
    )

    response = patch({'supplier_tags': [tag.id]})
    assert response.status_code == 400
    assert response.json()['error']['message']['ru'] == (
        'Теги от поставщика ресурса должны принадлежать поставщику ресурса')

    response = patch({'supplier_tags': [supplier_tag.id]})
    assert response.status_code == 200


@pytest.mark.parametrize('endpoint_path', ('resources-api:serviceresources-detail', 'api-v3:resource-consumer-detail', 'api-v4:resource-consumer-detail'))
def test_has_monitoring(client, endpoint_path):
    service = factories.ServiceFactory()
    resource_type = factories.ResourceTypeFactory()
    resource = factories.ResourceFactory(type=resource_type)
    sr = factories.ServiceResourceFactory(service=service, resource=resource)
    assert sr.has_monitoring is False

    response = client.json.patch(
        reverse(endpoint_path, args=(sr.id,)),
        {'has_monitoring': True}
    )
    assert response.status_code == 200

    sr.refresh_from_db()
    assert sr.has_monitoring is True


@pytest.mark.parametrize('endpoint_path', ('resources-api:serviceresources-detail', 'api-v3:resource-consumer-detail', 'api-v4:resource-consumer-detail'))
def test_edit_supplier_tags_tagged_type(client, endpoint_path):
    supplier = factories.ServiceFactory()
    valid_supplier_tag = factories.ResourceTagFactory(service=supplier)
    invalid_supplier_tag = factories.ResourceTagFactory(service=supplier)

    resource_type = factories.ResourceTypeFactory(supplier=supplier)
    resource_type.tags.add(valid_supplier_tag)
    resource = factories.ResourceFactory(type=resource_type)
    sr = factories.ServiceResourceFactory(resource=resource)

    patch = partial(
        client.json.patch,
        reverse(endpoint_path, args=(sr.id,))
    )

    response = patch({'supplier_tags': [invalid_supplier_tag.id]})
    assert response.status_code == 400
    assert response.json()['error']['message']['ru'] == (
        'Теги от поставщика ресурса должны соответствовать типу ресурса')
    assert not HistoryRawEntry.objects.count()
    response = patch({'supplier_tags': [valid_supplier_tag.id]})
    assert response.status_code == 200
    history_entry = HistoryRawEntry.objects.get(object_id=sr.id)
    assert history_entry.request_data == {'supplier_tags': [valid_supplier_tag.id]}


@pytest.mark.parametrize('endpoint_path', ('resources-api:serviceresources-detail', 'api-v3:resource-consumer-detail', 'api-v4:resource-consumer-detail'))
def test_add_stranger_tag(client, endpoint_path):
    service = factories.ServiceFactory()
    tag1 = factories.ResourceTagFactory(service=service)
    sr = factories.ServiceResourceFactory()

    response = client.json.patch(
        reverse(endpoint_path, args=(sr.id,)),
        {
            'tags': [tag1.id],
        }
    )
    assert response.status_code == 400
    assert response.json()['error']['message']['ru'] == 'Теги должны принадлежать тому же сервису, что и ресурс'

    sr.refresh_from_db()
    assert sr.tags.count() == 0


def check_resources(client, params, resources):
    response = client.json.get(
        reverse('resources-api:serviceresources-list'),
        params,
    )

    assert response.status_code == 200

    expected_ids = [s.id for s in resources]
    real_ids = [s['id'] for s in response.json()['results']]
    assert real_ids == expected_ids


@pytest.mark.parametrize('attribute', ['id', 'external_id', 'name'])
def test_search_resources(client, attribute, django_assert_num_queries):
    sr = factories.ServiceResourceFactory(
        resource=factories.ResourceFactory(
            name='some name',
            external_id='external:id')
    )
    factories.ServiceResourceFactory(resource=factories.ResourceFactory(name='notfound', external_id='notfound'))

    with django_assert_num_queries(16):
        # 2 * middleware selects
        # 1 select resourcetype
        # 2 select service
        # 1 select count(serviceresource) + 1 select serviceresource
        # prefetch related: 3 select resourcetag + 1 select resourcetype
        # 2 select role + 1 select auth_permission
        # 1 pg_is_in_recovery + 1 waffle
        check_resources(
            client,
            {
                'fields': 'actions,id,resource,service,state,state_display,supplier_tags,tags,modified_at',
                'type': sr.type.id,
                'state': 'requested,approved,granting,granted'.split(','),
                'supplier': sr.type.supplier_id,
                'search': getattr(sr.resource, attribute),
                'service': sr.service.id,
                'ordering': '-modified_at'
            },
            [sr],
        )


def test_filter_by_state(client):
    service1 = factories.ServiceFactory()
    service2 = factories.ServiceFactory()

    resource = factories.ResourceFactory()
    sr1 = factories.ServiceResourceFactory(
        resource=resource,
        service=service1,
        state='requested',
    )
    factories.ServiceResourceFactory(
        resource=resource,
        service=service2,
        state='granted',
    )

    check_resources(
        client,
        {'state': ['requested', 'deprived', 'non_valid_state']},
        [sr1],
    )


def test_filter_by_has_monitoring(client):
    service1 = factories.ServiceFactory()
    service2 = factories.ServiceFactory()

    resource = factories.ResourceFactory()
    resource_1 = factories.ResourceFactory()
    resource_1.type.need_monitoring = True
    resource_1.type.save()
    sr1 = factories.ServiceResourceFactory(
        resource=resource,
        service=service1,
        has_monitoring=True
    )
    factories.ServiceResourceFactory(
        resource=resource,
        service=service2,
        has_monitoring=False
    )

    sr2 = factories.ServiceResourceFactory(
        resource=resource_1,
        service=service1,
        has_monitoring=False
    )

    check_resources(
        client,
        {'has_monitoring': True},
        [sr1],
    )

    check_resources(
        client,
        {'need_monitoring': 'true'},
        [sr2],
    )


def test_filter_by_requester(client):
    staff = factories.StaffFactory()
    another_staff = factories.StaffFactory()
    sr1 = factories.ServiceResourceFactory(requester=staff)
    factories.ServiceResourceFactory(requester=another_staff)

    check_resources(client, {'requester': staff.login}, [sr1])


def test_filter_by_category(client, data):
    cat = factories.ResourceTypeCategoryFactory()
    cat2 = factories.ResourceTypeCategoryFactory()
    data.resource.type.category = cat
    data.resource.type.save()

    check_resources(
        client,
        {'category': cat.id},
        [data.sr],
    )

    check_resources(
        client,
        {'category': cat2.id},
        [],
    )


def test_filter_by_service(client):
    sr = factories.ServiceResourceFactory()
    factories.ServiceResourceFactory()

    check_resources(
        client,
        {'service': sr.service.pk},
        [sr],
    )


def test_filter_by_usage_tag(client):
    tag = factories.ResourceTagFactory()
    sr1 = factories.ServiceResourceFactory()
    factories.ServiceResourceFactory()

    sr1.tags.add(tag)

    check_resources(
        client,
        {'usage_tag': tag.pk},
        [sr1],
    )


@pytest.mark.parametrize('endpoint_path', ('resources-api:serviceresources-list', 'api-v3:resource-consumer-list', 'api-v4:resource-consumer-list'))
def test_filter_by_tag_slug(client, endpoint_path):
    tag1 = factories.ResourceTagFactory()
    tag2 = factories.ResourceTagFactory()
    sr1 = factories.ServiceResourceFactory()
    sr2 = factories.ServiceResourceFactory()
    factories.ServiceResourceFactory()

    sr1.tags.add(tag1)
    sr2.tags.add(tag2)

    response = client.json.get(reverse(endpoint_path), {'tags_slug': tag1.slug})
    assert response.status_code == 200
    expected_ids = [sr1.id]
    real_ids = [s['id'] for s in response.json()['results']]
    assert real_ids == expected_ids

    response = client.json.get(reverse(endpoint_path), {'tags_slug': (tag1.slug, tag2.slug)})
    assert response.status_code == 200
    expected_ids = [sr1.id, sr2.id]
    real_ids = [s['id'] for s in response.json()['results']]
    assert real_ids == expected_ids


def test_filter_by_resource_type(client):
    sr1 = factories.ServiceResourceFactory()
    sr2 = factories.ServiceResourceFactory()
    sr3 = factories.ServiceResourceFactory()

    check_resources(
        client,
        {'type': sr2.type.pk},
        [sr2],
    )

    check_resources(
        client,
        {'type': (sr1.type.pk, sr3.type.pk)},
        [sr1, sr3],
    )


def test_filter_by_resource_external_id(client):
    # левый sr, который не должен появится в выдаче
    factories.ServiceResourceFactory()

    resource = factories.ResourceFactory(external_id='hello_world')
    service_resource = factories.ServiceResourceFactory(resource=resource)

    check_resources(
        client,
        {'resource__external_id': resource.external_id},
        [service_resource]
    )

    check_resources(
        client,
        {'resource__external_id__in': resource.external_id},
        [service_resource]
    )

    check_resources(
        client,
        {'resource__external_id__in': ','.join([resource.external_id, resource.external_id])},
        [service_resource]
    )

    check_resources(
        client,
        {'resource__external_id': 'derp'},
        []
    )

    other_resource = factories.ResourceFactory(external_id='phantom_pain')
    other_service_resource = factories.ServiceResourceFactory(resource=other_resource)

    check_resources(
        client,
        {'resource__external_id__in': ','.join([resource.external_id, other_resource.external_id])},
        [service_resource, other_service_resource]
    )


def test_filter_by_resource_name(client):
    # левый sr, который не должен появится в выдаче
    factories.ServiceResourceFactory()

    resource = factories.ResourceFactory(name='hello_world')
    service_resource = factories.ServiceResourceFactory(resource=resource)

    check_resources(
        client,
        {'resource__name': resource.name},
        [service_resource]
    )

    check_resources(
        client,
        {'resource__name': 'derp'},
        []
    )

    other_resource = factories.ResourceFactory(name='phantom_pain')
    other_service_resource = factories.ServiceResourceFactory(resource=other_resource)

    check_resources(
        client,
        {'resource__name__in': ','.join([resource.name, other_resource.name])},
        [service_resource, other_service_resource]
    )


def test_filter_by_approver(service_with_owner, client):

    resource_type = factories.ResourceTypeFactory(supplier=service_with_owner)
    resource = factories.ResourceFactory(type=resource_type)
    requested = factories.ServiceResourceFactory(
        resource=resource,
        state=ServiceResource.REQUESTED,
    )
    granted = factories.ServiceResourceFactory(
        resource=resource,
        state=ServiceResource.GRANTED,
    )

    other_service = factories.ServiceFactory()
    other_resource_type = factories.ResourceTypeFactory(supplier=other_service)
    other_resource = factories.ResourceFactory(type=other_resource_type)
    factories.ServiceResourceFactory(resource=other_resource)

    check_resources(
        client,
        {'approvable_by': service_with_owner.owner.login},
        [requested, granted],
    )

    check_resources(
        client,
        {
            'approvable_by': service_with_owner.owner.login,
            'state': ServiceResource.GRANTED,
        },
        [granted],
    )


def test_superuser_can_approve_all(service_with_owner, client):
    resource_type = factories.ResourceTypeFactory(supplier=service_with_owner)
    resource = factories.ResourceFactory(type=resource_type)
    requested = factories.ServiceResourceFactory(
        resource=resource,
        state=ServiceResource.REQUESTED,
    )

    superuser = factories.StaffFactory(user=factories.UserFactory(is_superuser=True))

    client.login(superuser.login)

    check_resources(
        client,
        {'approvable_by': superuser.login},
        [requested],
    )


def test_filter_by_approver_accounts_for_children(service_with_owner, client):
    child = factories.ServiceFactory(parent=service_with_owner)

    resource_type = factories.ResourceTypeFactory(supplier=child)
    resource = factories.ResourceFactory(type=resource_type)
    requested = factories.ServiceResourceFactory(
        resource=resource,
        state=ServiceResource.REQUESTED,
    )

    check_resources(
        client,
        {'approvable_by': service_with_owner.owner.login},
        [requested],
    )


def test_filter_by_approver_ignores_deactivated_resources(service_with_owner, client):
    child = factories.ServiceFactory(parent=service_with_owner)

    resource_type = factories.ResourceTypeFactory(supplier=child)
    resource = factories.ResourceFactory(type=resource_type)
    requested = factories.ServiceResourceFactory(
        resource=resource,
        state=ServiceResource.REQUESTED,
    )

    deactivated_resource_type = factories.ResourceTypeFactory(supplier=child, is_enabled=False)
    deactivated_resource = factories.ResourceFactory(type=deactivated_resource_type)
    factories.ServiceResourceFactory(
        resource=deactivated_resource,
        state=ServiceResource.REQUESTED,
    )

    check_resources(
        client,
        {'approvable_by': service_with_owner.owner.login},
        [requested],
    )


def test_order_by_type(service_with_owner, client):
    sr1 = factories.ServiceResourceFactory()
    sr2 = factories.ServiceResourceFactory()
    sr3 = factories.ServiceResourceFactory()

    check_resources(
        client,
        {'ordering': 'resource__type_id'},
        [sr1, sr2, sr3],
    )

    check_resources(
        client,
        {'ordering': '-resource__type_id'},
        [sr3, sr2, sr1],
    )


@pytest.fixture
@freeze_time('2018-01-01')
def data(db, owner_role, staff_factory):
    user = factories.UserFactory(username='enot')
    staff = staff_factory(login='enot', user=user)

    service = factories.ServiceFactory(owner=staff)
    member = factories.ServiceMemberFactory(service=service, staff=staff, role=owner_role)

    supplier_owner = staff_factory()
    supplier = factories.ServiceFactory(owner=supplier_owner)
    supplier_dev = factories.ServiceMemberFactory(service=supplier, staff=staff_factory())

    resource_type = factories.ResourceTypeFactory(supplier=supplier)
    resource_type.consumer_roles.add(owner_role)
    resource_type.supplier_roles.add(owner_role)

    resource = factories.ResourceFactory(type=resource_type)
    sr = factories.ServiceResourceFactory(
        resource=resource,
        service=service,
        state=ServiceResource.REQUESTED,
        has_monitoring=True,
    )

    tvm_type = factories.ResourceTypeFactory(supplier_plugin='tvm')
    tvm_type.consumer_roles.add(owner_role)
    tvm_resource = factories.ResourceFactory(type=tvm_type)
    tvm_sr = factories.ServiceResourceFactory(
        resource=tvm_resource,
        service=service,
        state=ServiceResource.GRANTED,
    )

    return pretend.stub(
        user=user,
        staff=staff,
        service=service,
        supplier_dev=supplier_dev,
        resource=resource,
        resource_type=resource_type,
        member=member,
        sr=sr,
        tvm_sr=tvm_sr,
    )


def test_decline_resource(client, data):
    client.login(data.supplier_dev.staff.login)

    data.sr.state = ServiceResource.APPROVED
    data.sr.save()
    response = client.json.delete(reverse('resources-api:serviceresources-detail', args=(data.sr.id,)))
    assert response.status_code == 200

    data.sr.refresh_from_db()
    assert data.sr.state == ServiceResource.DEPRIVED
    assert data.sr.depriver == data.supplier_dev.staff


@pytest.mark.parametrize('state', (
    None,
    ServiceResource.GRANTED,
    ServiceResource.DEPRIVED,
    ServiceResource.REQUESTED,
))
@pytest.mark.parametrize('path', (
    'resources-api:serviceresources-detail',
    'api-v3:resource-consumer-detail',
    'api-v4:resource-consumer-detail',
))
def test_delete_robot_resource(client, data, state, path):
    """
    Проверяем, что невозможно разорвать последнюю связь робота с сервисом
    """
    data.sr.state = ServiceResource.GRANTED
    data.sr.type.code = 'staff-robot'
    data.sr.type.save()
    data.sr.save()

    if state:
        factories.ServiceResourceFactory(
            resource=data.sr.resource,
            state=state,
        )

    client.login(data.staff.login)

    response = client.json.delete(reverse(path, args=(data.sr.id,)))
    data.sr.refresh_from_db()

    if state != ServiceResource.GRANTED:
        assert response.status_code == 400
        assert response.json()['error']['message']['ru'] == (
            'Нельзя разорвать последнюю связь робота с сервисом,'
            ' если он больше не нужен, пожалуйста, увольте его - '
            'https://wiki.yandex-team.ru/tools/support/zombik/#faq '
        )
        assert data.sr.state == ServiceResource.GRANTED
    else:
        assert response.status_code == 200
        assert data.sr.state == ServiceResource.DEPRIVED


def test_decline_obsolete_resource(client, data):
    client.login(data.supplier_dev.staff.login)

    data.sr.state = ServiceResource.OBSOLETE
    data.sr.save()
    response = client.json.delete(reverse('resources-api:serviceresources-detail', args=(data.sr.id,)))
    assert response.status_code == 400

    data.sr.refresh_from_db()
    assert data.sr.state == ServiceResource.OBSOLETE
    assert data.sr.depriver is None


def test_decline_resource_by_stranger(client, data):
    stranger = factories.StaffFactory()
    client.login(stranger.login)

    response = client.json.delete(
        reverse('resources-api:serviceresources-detail', args=[data.sr.id])
    )
    assert response.status_code == 403


def test_delete_resource(client, data):
    data.sr.state = ServiceResource.GRANTED
    data.sr.save()
    client.login(data.staff.login)

    response = client.json.delete(reverse('resources-api:serviceresources-detail', args=(data.sr.id,)))
    assert response.status_code == 200

    data.sr.refresh_from_db()
    assert data.sr.state == ServiceResource.DEPRIVED
    assert data.sr.depriver == data.staff


@pytest.mark.parametrize('is_dismissed', (True, False))
def test_deprive_resources_with_dismissed_robot(data, is_dismissed):
    robot = factories.StaffFactory(
        login='cool-robot',
        user=factories.UserFactory(username='cool-robot'),
        is_dismissed=is_dismissed,
        is_robot=True,
    )
    data.sr.state = ServiceResource.GRANTED
    data.sr.resource.attributes = {}
    data.sr.resource.name = robot.login
    data.sr.type.code = 'staff-robot'
    data.sr.type.supplier_plugin = 'robots'
    data.sr.type.save()
    data.sr.resource.save()
    data.sr.save()

    revoke_dismissed_robots_resources()

    data.sr.refresh_from_db()
    target_state = ServiceResource.DEPRIVED if is_dismissed else ServiceResource.GRANTED
    assert data.sr.state == target_state


def test_delete_resource_which_not_finished(client, data):
    data.sr.state = ServiceResource.GRANTED
    data.sr.resource.attributes = {}
    data.sr.type.code = 'staff-robot'
    data.sr.type.supplier_plugin = 'robots'

    data.sr.type.save()
    data.sr.resource.save()
    data.sr.save()

    factories.ServiceResourceFactory(resource=data.sr.resource, state=ServiceResource.GRANTED)
    role = factories.RoleFactory(code=settings.ABC_ROBOTS_MANAGER_ROLE)
    factories.ServiceMemberFactory(staff=data.staff, service=data.sr.service, role=role)
    client.login(data.staff.login)

    response = client.json.delete(reverse('resources-api:serviceresources-detail', args=(data.sr.id,)))
    assert response.status_code == 200

    data.sr.refresh_from_db()
    assert data.sr.state == ServiceResource.DEPRIVED
    assert data.sr.depriver == data.staff


def test_delete_resource_by_stranger(client, data, person):
    data.sr.state = ServiceResource.GRANTED
    data.sr.save()

    client.login(person.login)

    response = client.json.delete(reverse('resources-api:serviceresources-detail', args=(data.sr.id,)))
    assert response.status_code == 403


@pytest.mark.parametrize('endpoint_path', ('resources-api:serviceresources-detail', 'api-v3:resource-consumer-detail', 'api-v4:resource-consumer-detail'))
def test_delete_tvm_resource(client, data, owner_role, endpoint_path, staff_factory):
    staff = staff_factory()
    factories.ServiceMemberFactory(service=data.tvm_sr.service, staff=staff, role=owner_role)
    client.login(staff.login)
    with patch('plan.resources.suppliers.tvm.TVMPlugin.delete', return_value=True) as delete:
        response = client.json.delete(
            reverse(endpoint_path, args=(data.tvm_sr.id,)))
        assert response.status_code == 200
        assert delete.called

        data.tvm_sr.refresh_from_db()
        assert data.tvm_sr.state == ServiceResource.DEPRIVED


@pytest.mark.parametrize('endpoint_path', ('resources-api:serviceresources-passp-export', 'api-v3:resource-consumer-passp-export', 'api-v4:resource-consumer-passp-export'))
def test_passp_export(client, django_assert_num_queries, endpoint_path):
    """Тест выгрузки денормализванных данных для Паспорта

    # источник[окржения]: приложения -> api(скоупы) -> приемник[окружения]: приложения
    service1(11)[env1]: app1_1, app1_2 -> API1(scope1, scope2) -> service3(33)[env1, env2]: app3_1, app3_2
    service2(22)[env2]: app2_2 -> API2(scope3, scope4) -> service4(44)[env1]: app4_1, app4_2

    service5 без приложений
    """
    service1 = factories.ServiceFactory(id=11)
    service2 = factories.ServiceFactory(id=22)
    service3 = factories.ServiceFactory(id=33)
    service4 = factories.ServiceFactory(id=44)
    service5 = factories.ServiceFactory(id=55)

    usage_tag1 = factories.ResourceTagFactory(name='Uses API1')
    usage_tag2 = factories.ResourceTagFactory(name='Uses API2')
    usage_tag5 = factories.ResourceTagFactory(name='Uses API5')

    env1_tag = factories.ResourceTagFactory(name='Env 1')
    env2_tag = factories.ResourceTagFactory(name='Env 2')

    app_type = factories.ResourceTypeFactory(supplier=service3)
    api_type1 = factories.ResourceTypeFactory(supplier=service1, usage_tag=usage_tag1)
    api_type1.dependencies.add(app_type)
    api_type2 = factories.ResourceTypeFactory(supplier=service2, usage_tag=usage_tag2)
    api_type2.dependencies.add(app_type)
    api_type5 = factories.ResourceTypeFactory(supplier=service5, usage_tag=usage_tag5)
    api_type5.dependencies.add(app_type)

    def create_api(type_, id_, scopes, service, supplier_tags, tags):
        api = factories.ServiceResourceFactory(resource=factories.ResourceFactory(type=type_, external_id=id_,
                                                                                  attributes={'scopes': scopes}),
                                               service=service, state=ServiceResource.GRANTED)
        api.supplier_tags.add(*supplier_tags)
        api.tags.add(*tags)

    create_api(api_type1, 'api1-3', ['scope1', 'scope2'], service3, [env1_tag], [env1_tag, env2_tag])
    create_api(api_type2, 'api2-4', ['scope3', 'scope4'], service4, [env2_tag], [env1_tag])
    create_api(api_type2, 'api2-5', ['scope3', 'scope4'], service5, [env2_tag], [env1_tag])
    create_api(api_type5, 'api5-4', ['scope3', 'scope4'], service4, [env2_tag], [env1_tag])

    def create_app(id_, service, tags):
        app = factories.ServiceResourceFactory(resource=factories.ResourceFactory(type=app_type, external_id=id_),
                                               service=service, state=ServiceResource.GRANTED)
        app.tags.add(*tags)

    create_app('1_1', service1, [api_type1.usage_tag, env1_tag])
    create_app('1_2', service1, [api_type1.usage_tag, env1_tag, env2_tag])

    create_app('2_1', service2, [api_type2.usage_tag, env1_tag])
    create_app('2_2', service2, [api_type2.usage_tag, env1_tag, env2_tag])

    create_app('3_1', service3, [api_type1.usage_tag, env1_tag])
    create_app('3_2', service3, [api_type1.usage_tag, env1_tag, env2_tag])

    create_app('4_1', service4, [api_type2.usage_tag, env1_tag])
    create_app('4_2', service4, [api_type2.usage_tag, env1_tag, env2_tag])

    with django_assert_num_queries(9):
        response = client.get(
            reverse(endpoint_path),
            {'type': app_type.id}
        )
    assert response.status_code == 200

    content = response.content.decode()
    content = [line.split('\t') for line in content.split('\n') if line]

    expected = [
        ['11', '1_1', '33', '3_1', 'scope1,scope2'],
        ['11', '1_1', '33', '3_2', 'scope1,scope2'],
        ['11', '1_2', '33', '3_1', 'scope1,scope2'],
        ['11', '1_2', '33', '3_2', 'scope1,scope2'],
        ['22', '2_2', '44', '4_1', 'scope3,scope4'],
        ['22', '2_2', '44', '4_2', 'scope3,scope4'],
    ]
    assert iterables_are_equal(content, expected)


@pytest.mark.parametrize('endpoint_path', ('resources-api:serviceresources-detail', 'api-v3:resource-consumer-detail', 'api-v4:resource-consumer-detail'))
def test_get_uneditable_type_actions(client, data, endpoint_path):
    client.login(data.user.username)

    assert data.sr.type.form_back_handler is None

    response = client.json.get(
        reverse(endpoint_path, args=(data.sr.id,)),
        data={'fields': 'id,actions'},
    )

    assert response.status_code == 200
    assert 'edit' not in response.json()['actions']

    data.resource_type.form_back_handler = "result = {'answer_short_text_18809': service_resource.resource.name}"
    data.resource_type.save()
    assert data.sr.type.form_back_handler is not None

    with patch('plan.resources.permissions.can_edit_resource') as patched:
        patched.return_value = True
        response = client.json.get(
            reverse(endpoint_path, args=(data.sr.id,)),
            data={'fields': 'id,actions'},
        )

    assert response.status_code == 200
    assert 'edit' in response.json()['actions']


@pytest.mark.parametrize('endpoint_path', ('resources-api:serviceresources-detail', 'api-v3:resource-consumer-detail', 'api-v4:resource-consumer-detail'))
def test_superuser_can_do_everything(client, data, endpoint_path):
    superuser = factories.StaffFactory(user=factories.UserFactory(is_superuser=True))
    client.login(superuser.login)

    data.sr.state = ServiceResource.APPROVED
    data.sr.save()
    response = client.json.get(
        reverse(endpoint_path, args=(data.sr.id,)),
        data={'fields': 'id,actions'},
    )
    assert response.status_code == 200
    # здесь в будущем должны добавится другие пермишны,
    # когда мы поддержим их в беке
    # сейчас tvm не разрешит суперюзерам ничего редактировать, например
    assert set(response.json()['actions']) == {'delete', 'resource_provide'}


@pytest.mark.parametrize('endpoint_path', ('resources-api:serviceresources-list', 'api-v3:resource-consumer-list', 'api-v4:resource-consumer-list'))
def test_filter_by_request_id(client, endpoint_path):
    service_resource = factories.ServiceResourceFactory(request_id=100500)
    response = client.json.get(
        reverse(endpoint_path),
        {'request_id': service_resource.request_id}
    )

    assert response.status_code == 200

    result = response.json()['results']
    assert len(result) == 1
    assert result[0]['id'] == service_resource.id


@pytest.mark.parametrize('endpoint_path', ('resources-api:serviceresources-list', 'api-v3:resource-consumer-list', 'api-v4:resource-consumer-list'))
def test_csv_export(client, person, mailoutbox, data, endpoint_path):
    client.login(person.username)
    response = client.json.get(reverse(endpoint_path), data={'export': True})
    assert response.status_code == 201
    assert len(mailoutbox) == 1
    attachments = mailoutbox[0].attachments
    assert len(attachments) == 1
    file_content = attachments[0][1]
    service_resource1, service_resource2 = ServiceResource.objects.all()
    resource1 = service_resource1.resource
    resource2 = service_resource2.resource
    assert file_content == '''"supplier","type","consumer","consumer_id","consumer_slug","tags","attributes","modified_at","state","has_monitoring","need_monitoring"
"%s","%s","%s","%s","%s","","ID:None;name:%s","2018-01-01T00:00:00Z","requested","%s","%s"
"%s","%s","%s","%s","%s","","ID:None;name:%s","2018-01-01T00:00:00Z","granted","%s","%s"
''' % (
        resource1.type.supplier.name, resource1.type.name, service_resource1.service.name,
        service_resource1.service.id, service_resource1.service.slug, resource1.name, service_resource1.has_monitoring, resource1.type.need_monitoring,
        resource2.type.supplier.name, resource2.type.name, service_resource2.service.name,
        service_resource2.service.id, service_resource2.service.slug, resource2.name, service_resource2.has_monitoring, resource2.type.need_monitoring
    )


@pytest.mark.parametrize('endpoint_path', ('resources-api:serviceresources-list', 'api-v3:resource-consumer-list', 'api-v4:resource-consumer-list'))
def test_csv_export_too_many(client, person, monkeypatch, endpoint_path):
    monkeypatch.setattr('django.db.models.query.QuerySet.count', lambda *args, **kwargs: 100001)
    client.login(person.username)
    response = client.json.get(reverse(endpoint_path), data={'export': True})
    assert response.status_code == 400


def test_v4_pagination(client, person):
    client.login(person.username)
    expected = set(factories.ServiceResourceFactory.create_batch(100))
    reversed_url = reverse('api-v4:resource-consumer-list')
    response = client.json.get(reversed_url)
    actual = [service for service in response.json()['results']]
    while response.json()['next'] is not None:
        assert len(response.json()['results']) == 20
        assert reversed_url + '?cursor=' in response.json()['next']
        response = client.json.get(response.json()['next'])
        actual.extend([service for service in response.json()['results']])
    actual = {
        resource for resource in ServiceResource.objects.filter(
            id__in=[resource['id'] for resource in actual]
        )
    }
    assert actual == expected


@pytest.mark.parametrize('ordering', ['id', 'modified_at', 'pk'])
@pytest.mark.parametrize('api', ['api-v4', 'api-v3'])
def test_ordering(client, person, ordering, api):
    client.login(person.username)
    factories.ServiceResourceFactory()
    response = client.json.get(reverse(api + ':resource-consumer-list'), {'ordering': ordering})
    if ordering != 'pk':
        assert response.status_code == 200
    else:
        assert response.status_code == 400


def test_infrastructure_api(client, infrastructure_data):
    response = client.json.get(
        reverse('api-frontend:infrastructure-list'),
        {'service_id': infrastructure_data.service.id}
    )

    assert response.status_code == 200
    assert response.json()['results'] == [
        {'type': {'code': 'arcadia-url', 'content_type': 'url'},
         'content': 'https://arcanum-test.yandex.net/arc/trunk/arcadia/frontend/services/abc/'},
        {'type': {'code': 'arcadia-url', 'content_type': 'url'},
         'content': 'https://arcanum-test.yandex.net/arc/trunk/arcadia/intranet/plan/'},
        {'type': {'code': 'warden-url', 'content_type': 'url'},
         'content': 'https://warden.z.yandex-team.ru/components/abcd/s/abc/'},
        {'type': {'code': 'spi-chat', 'content_type': 'url'},
         'content': 'https://t.me/joinchat/BdtakRQm0yD8dS3x0BGCcQ'},
        {'type': {'code': 'infra-preset', 'content_type': 'url'},
         'content': 'https://infra.yandex-team.ru/timeline?preset=Zft6tfk3eXT'},
        {'type': {'code': 'infra-preset', 'content_type': 'url'},
         'content': 'https://infra.yandex-team.ru/timeline?preset=Zft6tfk3eXT23'}
    ]


def test_export_alerts(client):
    """
    GET /api/frontend/resources/consumers/create_alerts
    """
    resource_type = factories.ResourceTypeFactory(
        code='deploy_deploy_unit',
        category=factories.ResourceTypeCategoryFactory(slug='provider_alerts'),
    )
    service = factories.ServiceFactory()
    factories.ServiceResourceFactory(
        resource=factories.ResourceFactory(
            type=resource_type,
            name='geo:vla,stage:zooface-testing,deployUnit:Frontend',
        ),
        service=service,
    )
    factories.ServiceResourceFactory(
        resource=factories.ResourceFactory(
            type=resource_type,
            name='geo:iva,stage:smth,deployUnit:Back,itype:deploy',
        ),
        service=service,
    )
    factories.ServiceResourceFactory(
        resource=factories.ResourceFactory(
            type=resource_type,
            name='geo:msk,stage:smth,deployUnit:Back,itype:deploy',
        ),
    )

    response = client.json.get(
        reverse('api-frontend:resource-consumer-create-alerts'),
        {'service': service.id, 'type': resource_type.id}
    )
    assert response.status_code == 302
    assert response.url == (
        'https://solomon.yandex-team.ru/admin/projects/'
        f'{service.slug}/alerts/templates/multipleResources?'
        'serviceProviderId=deploy&r=geo:vla,stage:zooface-testing,deployUnit:Frontend,resourceType:deploy_unit'
        '&r=geo:iva,stage:smth,deployUnit:Back,itype:deploy,resourceType:deploy_unit'
    )


def test_export_alerts_validation(client):
    service = factories.ServiceFactory()
    resource_type = factories.ResourceTypeFactory(
        code='deploy_deploy_unit',
        category=factories.ResourceTypeCategoryFactory(slug='smth'),
    )

    response = client.json.get(
        reverse('api-frontend:resource-consumer-create-alerts'),
        {'service': service.id}
    )
    assert response.status_code == 400

    response = client.json.get(
        reverse('api-frontend:resource-consumer-create-alerts'),
        {'service': service.id, 'type': resource_type.id}
    )
    assert response.status_code == 400

    resource_type.category.slug = 'provider_alerts'
    resource_type.category.save()

    response = client.json.get(
        reverse('api-frontend:resource-consumer-create-alerts'),
        {'service': service.id, 'type': resource_type.id}
    )
    assert response.status_code == 302

    response = client.json.get(
        reverse('api-frontend:resource-consumer-create-alerts'),
        {'type': resource_type.id}
    )
    assert response.status_code == 400

    response = client.json.get(
        reverse('api-frontend:resource-consumer-create-alerts'),
    )
    assert response.status_code == 400
