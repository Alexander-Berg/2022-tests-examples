import pretend
import pytest

from datetime import timedelta
from django.conf import settings
from django.core.urlresolvers import reverse
from django.contrib.auth.models import Permission
from django.utils import timezone
from common import factories

pytestmark = pytest.mark.django_db


@pytest.fixture
def data(db, owner_role, staff_factory):
    metaservice = factories.ServiceFactory(slug=settings.ABC_DEFAULT_SERVICE_PARENT_SLUG)
    service1 = factories.ServiceFactory(parent=metaservice, owner=staff_factory())
    service2 = factories.ServiceFactory(parent=metaservice, owner=staff_factory())

    service3 = factories.ServiceFactory(parent=metaservice, owner=staff_factory())

    view_own_services = Permission.objects.get(codename='view_own_services')
    view_details = Permission.objects.get(codename='view_details')
    view_perm = Permission.objects.get(codename='can_view')
    view_kpi_perm = Permission.objects.get(codename='view_kpi')

    ya_powered = factories.StaffFactory()

    for i_perm in [view_kpi_perm, view_perm, view_details, view_own_services]:
        ya_powered.user.user_permissions.add(i_perm)

    service3.owner = ya_powered

    factories.ServiceMemberFactory(
        staff=service1.owner,
        service=service1,
        role=owner_role,
    )

    factories.ServiceMemberFactory(
        staff=service3.owner,
        service=service3,
        role=owner_role,
    )

    return pretend.stub(
        service1=service1,
        service2=service2,
        metaservice=metaservice,
        role_head=owner_role,
        service3=service3,
        ya_powered=ya_powered,
    )


def test_for_new_roles_get(client, data):
    client.login(data.service3.owner.login)
    response = client.json.get(
        reverse('api-v4:service-list'),
        {'fields': 'is_exportable,is_suspicious,id,slug'}
    )
    assert response.status_code == 200

    results = response.json()

    assert len(results['results']) == 1
    assert results['results'][0]['is_exportable'] == data.service3.is_exportable
    assert results['results'][0]['id'] == data.service3.pk
    assert results['results'][0]['slug'] == data.service3.slug
    assert results['results'][0]['is_suspicious'] == data.service3.is_suspicious


@pytest.mark.parametrize('is_superuser', [True, False])
def test_for_new_roles_get_not_enough_perms(client, data, is_superuser):
    user = factories.UserFactory(is_superuser=is_superuser)
    stranger = factories.StaffFactory(user=user)
    client.login(stranger.login)
    response = client.json.get(
        reverse('api-v4:service-list'),
        {'fields': 'is_exportable,is_suspicious,id,slug'}
    )
    if is_superuser:
        assert response.status_code == 200
    else:
        assert response.status_code == 403  # after new middleware with strict dening without permisison


def test_for_new_roles_post(client, data):
    client.login(data.service3.owner.login)
    response = client.json.post(
        reverse('api-v4:service-list'),
        {'fields': 'is_exportable,is_suspicious'}
    )

    assert response.status_code == 403  # cause ya_powered has no 'can-edit' permission


def test_get_services(client, data):
    client.login(data.service1.owner.login)

    response = client.json.get(
        reverse('services:service_list'),
        {'fields': 'is_exportable,is_suspicious,id'}
    )
    assert response.status_code == 200

    results = response.json()
    assert results['content']['services'][0]['value']['id'] == data.metaservice.pk
    assert results['content']['services'][0]['value']['is_exportable'] == data.metaservice.is_exportable
    assert results['content']['services'][0]['entities'][0]['value']['id'] == data.service1.pk
    assert results['content']['services'][0]['value']['is_suspicious'] == data.metaservice.is_suspicious


def test_filter_by_tag(client, data):
    tag = factories.ServiceTagFactory()
    tag2 = factories.ServiceTagFactory()

    data.service2.tags.add(tag)
    factories.ServiceMemberFactory(
        staff=data.service1.owner,
        service=data.service2,
        role=data.role_head,
    )
    factories.ServiceFactory()

    client.login(data.service1.owner.login)

    response = client.json.get(
        reverse('services:service_list'),
        {'fields': 'id'}
    )
    assert response.status_code == 200

    results = response.json()
    assert results['content']['services'][0]['value']['id'] == data.metaservice.pk
    children = results['content']['services'][0]['entities']
    assert {children[0]['value']['id'], children[1]['value']['id']} == {data.service1.pk, data.service2.pk}

    response = client.json.get(
        reverse('services:service_list'),
        {
            'tags': [tag.pk, tag2.pk],
            'fields': 'id',
        }
    )

    results = response.json()
    assert results['content']['services'][0]['value']['id'] == data.metaservice.pk
    assert results['content']['services'][0]['entities'][0]['value']['id'] == data.service2.pk


def get_services_ids(client, params):
    response = client.json.get(
        reverse('services:service_list'),
        params,
    )
    assert response.status_code == 200

    def get_ids(nodes):
        # разворачиваем дерево в список
        for node in nodes:
            yield node['value']['id']

            for service_id in get_ids(node['entities']):
                yield service_id

    return set(get_ids(response.json()['content']['services']))


def test_filter_by_my(client, data):
    client.login(data.service1.owner.login)

    assert get_services_ids(client, {'show': 'all', 'fields': 'id'}) == {
        data.metaservice.pk, data.service1.pk, data.service2.pk, data.service3.pk,
    }
    assert get_services_ids(client, {'show': 'my', 'fields': 'id'}) == {data.metaservice.pk, data.service1.pk}


def test_filter_by_person(client, data, staff_factory):
    staff = staff_factory()
    factories.ServiceMemberFactory(
        staff=staff,
        service=data.service2,
        role=data.role_head,
    )
    client.login(staff.login)

    assert get_services_ids(client, {'person': staff.login, 'fields': 'id'}) == {data.metaservice.pk, data.service2.pk}


def test_filter_by_department(client, data, staff_factory):
    staff = staff_factory()
    factories.ServiceMemberFactory(
        staff=staff,
        service=data.service2,
        role=data.role_head,
    )
    client.login(staff.login)

    assert get_services_ids(client, {'department': staff.department.pk, 'fields': 'id'}) == {data.metaservice.pk, data.service2.pk}


def test_order_by_kpi(client, data):
    data.service1.kpi_bugs_count = 1
    data.service1.save()
    data.service2.kpi_bugs_count = 2
    data.service2.save()
    data.service3.kpi_bugs_count = 0
    data.service3.save()

    client.login(data.service1.owner.login)

    assert (
        get_services_ids(client, {'show': 'all', 'sorting': 'kpi_bugs_count', 'fields': 'id'}) ==
        {data.metaservice.pk, data.service1.pk, data.service2.pk, data.service3.pk}
    )
    # no order checking
    assert (
        get_services_ids(client, {'show': 'all', 'sorting': '-kpi_bugs_count', 'fields': 'id'}) ==
        {data.metaservice.pk, data.service3.pk, data.service2.pk, data.service1.pk}
    )


def test_filter_by_suspicious(client, data, staff_factory):
    staff = staff_factory()
    client.login(staff.login)

    # положим, существует один подозрительный и один неподозрительный сервис
    suspicious_service = factories.ServiceFactory(
        suspicious_date=timezone.now().date() - timedelta(days=20)
    )

    not_suspicious_service = factories.ServiceFactory(suspicious_date=None)

    assert suspicious_service.is_suspicious is True
    assert not_suspicious_service.is_suspicious is False

    # проверим, что при True suspicious_service попадает, а not_suspicious_service - нет
    response = client.json.get(reverse('services-api:service-list'), {'is_suspicious': 'true', 'fields': 'id'})
    assert response.status_code == 200
    assert suspicious_service.id in [service['id'] for service in response.json()['results']]
    assert not_suspicious_service.id not in [service['id'] for service in response.json()['results']]

    # проверим, что при False not_suspicious_service попадает, а suspicious_service - нет
    response = client.json.get(reverse('services-api:service-list'), {'is_suspicious': 'false', 'fields': 'id'})
    assert response.status_code == 200
    assert suspicious_service.id not in [service['id'] for service in response.json()['results']]
    assert not_suspicious_service.id in [service['id'] for service in response.json()['results']]

    # в иных случаях должны получить оба сервиса
    response = client.json.get(reverse('services-api:service-list'), {'is_suspicious': 'true, false', 'fields': 'id'})
    assert response.status_code == 200
    assert suspicious_service.id in [service['id'] for service in response.json()['results']]
    assert not_suspicious_service.id in [service['id'] for service in response.json()['results']]

    response = client.json.get(reverse('services-api:service-list'), {'fields': 'id'})
    assert response.status_code == 200
    assert suspicious_service.id in [service['id'] for service in response.json()['results']]
    assert not_suspicious_service.id in [service['id'] for service in response.json()['results']]
