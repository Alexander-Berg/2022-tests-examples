from functools import partial

import pytest
from django.core.urlresolvers import reverse
from mock import patch
from common import factories

from plan.services.models import ServiceMember

pytestmark = pytest.mark.django_db


@pytest.fixture
def get_responsibles(client):
    return partial(
        client.json.get,
        reverse('services-api:responsible-list')
    )


def test_get_responsibles(get_responsibles, data):
    response = get_responsibles()

    assert response.status_code == 200

    result = response.json()
    assert result['count'] == ServiceMember.objects.responsibles().count()


def test_add_responsible(client, data, responsible_role):
    client.login(data.owner_of_service.staff.login)

    with patch('plan.api.idm.actions.request_membership') as request_membership:
        response = client.json.post(
            reverse('services-api:responsible-list'),
            {
                'service': data.service.id,
                'staff': data.stranger.login,
            }
        )

    request_membership.assert_called_once_with(
        data.service,
        data.stranger,
        responsible_role,
        requester=data.staff,
    )

    assert response.status_code == 201


def test_remove_responsible(client, data, responsible_role):
    client.login(data.owner_of_service.staff.login)

    with patch('plan.api.idm.actions.deprive_role') as deprive_role:
        response = client.json.delete(
            reverse('services-api:responsible-detail', args=[data.responsible.id]),
        )

    deprive_role.assert_called_once_with(data.responsible, requester=data.staff,)

    assert response.status_code == 204


def test_duplicate_responsibles(client, data):
    """Responsibles could not be duplicated, but you can be both responsible and owner"""

    client.login(data.owner_of_service.staff.login)

    response = client.json.post(
        reverse('services-api:responsible-list'),
        {
            'service': data.service.id,
            'staff': data.responsible.staff.login,
        }
    )

    assert response.status_code == 400
    assert response.json()['error']['code'] == 'validation_error'

    with patch('plan.api.idm.actions.request_membership'):
        response = client.json.post(
            reverse('services-api:responsible-list'),
            {
                'service': data.service.id,
                'staff': data.team_member.staff.login,
            }
        )

    assert response.status_code == 201


def test_stranger_cannot_add_responsible(client, data):
    client.login(data.stranger.login)

    response = client.json.post(
        reverse('services-api:responsible-list'),
        {
            'service': data.service.id,
            'staff': data.stranger.login,
        }
    )

    assert response.status_code == 403
    assert response.json()['error']['message']['en'] == (
        'You are not allowed to add responsible to this service')


def test_stranger_cannot_remove_responsible(client, data, responsible_role):
    client.login(data.stranger.login)

    response = client.json.delete(
        reverse('services-api:responsible-detail', args=[data.responsible.id]),
    )

    assert response.status_code == 403
    assert response.json()['error']['message']['en'] == (
        'You are not allowed to delete responsibles from this service'
    )


def test_support_can_add_responsible(client, data, responsible_role, robot):
    client.login(data.support.login)

    with patch('plan.api.idm.actions.request_membership') as request_membership:
        response = client.json.post(
            reverse('services-api:responsible-list'),
            {
                'service': data.service.id,
                'staff': data.stranger.login,
            }
        )

    request_membership.assert_called_once_with(
        data.service,
        data.stranger,
        responsible_role,
        comment='Роль запрошена сотрудником службы поддержки {} и '
                'автоподтверждена Конрадом.'.format(data.support.login)
        )

    assert response.status_code == 201


def test_support_can_remove_responsible(client, data, responsible_role):
    client.login(data.support.login)

    with patch('plan.api.idm.actions.deprive_role') as deprive_role:
        response = client.json.delete(
            reverse('services-api:responsible-detail', args=[data.responsible.id]),
        )

    deprive_role.assert_called_once_with(
        data.responsible,
        comment='Роль отозвана сотрудником службы поддержки {} и автоподтверждена Конрадом.'.format(data.support.login)
    )

    assert response.status_code == 204


def test_cannot_remove_heads_from_responsibles(client, data):
    client.login(data.owner_of_service.staff.login)

    response = client.json.delete(
        reverse('services-api:responsible-detail', args=[data.deputy.id]),
    )

    assert response.status_code == 403
    assert response.json()['error']['message']['en'] == (
        'You cannot remove a head or deputy from services responsibles'
    )

    assert ServiceMember.objects.filter(id=data.owner_of_service.id).exists()


def test_delete_non_existent_responsible(client, data):
    client.login(data.owner_of_service.staff.login)

    response = client.json.delete(
        reverse('services-api:responsible-detail', args=[100500]),
    )

    assert response.status_code == 404
    assert response.json()['error']['detail'] == 'Not found.'


def test_editing_responsibles_is_not_allowed(client, data):
    client.login(data.owner_of_service.staff.login)

    for method in (client.json.post, client.json.patch):
        response = method(
            reverse('services-api:responsible-detail', args=[data.responsible.id]),
            {'staff': data.stranger.login}
        )

        assert response.status_code == 405
        assert response.json()['error']['detail'] == 'Method "{}" not allowed.'.format(method.__name__.upper())

        data.responsible.refresh_from_db()
        assert data.responsible.staff != data.stranger


def test_service_filter_num_queries(client, data, django_assert_num_queries):
    services = [factories.ServiceFactory().id for _ in range(10)]
    services.append(data.service.id)
    services.append(data.child.id)
    with django_assert_num_queries(6):
        # 1 staff
        # 1 permissions
        # 2 сама ручка
        # 1 pg_is_in_recovery
        # 1 waffle
        response = client.json.get(
            reverse('api-v4:service-responsible-list'),
            {
                'service__in': ','.join(map(str, services)),
                'fields': 'person.login,service.id',
            }
        )

    result = response.json()
    assert result['count'] == 4


def test_service_filter_by_slug(client, data, django_assert_num_queries):
    with django_assert_num_queries(6):
        # 1 staff
        # 1 permissions
        # 2 сама ручка
        # 1 pg_is_in_recovery
        # 1 waffle
        response = client.json.get(
            reverse('api-v4:service-responsible-list'),
            {
                'service__slug': data.service.slug,
                'fields': 'person.login,service.id',
            }
        )

    result = response.json()
    assert result['count'] == 3
    assert (
        set(item['service']['id'] for item in result['results']) ==
        {data.service.id}
    )


@pytest.mark.parametrize('api', ['api-v4', 'api-frontend'])
def test_service_filter_by_slug_not_exportable(client, data, api):
    data.service.is_exportable = False
    data.service.save()

    response = client.json.get(
        reverse(f'{api}:service-responsible-list'),
        {
            'service__slug': data.service.slug,
            'fields': 'person.login,service.id',
        }
    )

    result = response.json()
    assert result['count'] == (3 if api == 'api-frontend' else 0)


def test_service_filter(client, data):
    response = client.json.get(
        reverse('services-api:responsible-list'),
        {'service': data.service.id}
    )

    result = response.json()
    assert result['count'] == (
        ServiceMember
        .objects
        .responsibles()
        .filter(service=data.service)
        .count()
    )
    assert {responsible_data['id'] for responsible_data in result['results']} == set(
        ServiceMember
        .objects
        .filter(service=data.service)
        .responsibles()
        .values_list('id', flat=True)
    )


def test_descendants_filter(client, data):

    for service in (data.metaservice, data.service, data.child):
        response = client.json.get(
            reverse('services-api:responsible-list'),
            {
                'service': service.id,
                'descendants': True
            }
        )

        result = response.json()
        assert result['count'] == (
            ServiceMember
            .objects
            .responsibles()
            .filter(service__in=service.get_descendants(include_self=True))
            .count()
        )
        assert {responsible_data['id'] for responsible_data in result['results']} == set(
            ServiceMember
            .objects
            .responsibles()
            .filter(service__in=service.get_descendants(include_self=True))
            .values_list('id', flat=True)
        )


def test_filter_by_person(get_responsibles, data):
    response = get_responsibles({'person': data.staff.login})

    result = response.json()
    assert result['count'] == ServiceMember.objects.responsibles().filter(staff=data.staff).count()

    response = get_responsibles({
        'person__in': ','.join((data.staff.login, data.other_staff.login))
    })

    result = response.json()
    assert result['count'] == (
        ServiceMember
        .objects
        .responsibles()
        .filter(staff__in=[data.staff, data.other_staff])
        .count()
    )


def test_filter_by_person_uid(get_responsibles, data):
    response = get_responsibles({'person__uid': data.staff.uid})

    result = response.json()
    assert result['count'] == ServiceMember.objects.responsibles().filter(staff=data.staff).count()

    response = get_responsibles({
        'person__uid__in': ','.join((data.staff.uid, data.other_staff.uid))
    })

    result = response.json()
    assert result['count'] == (
        ServiceMember
        .objects
        .responsibles()
        .filter(staff__in=[data.staff, data.other_staff])
        .count()
    )


def test_filter_by_exportable(get_responsibles, data):
    data.service.is_exportable = False
    data.service.save()

    # по умолчанию неэкспортируемые сервисы не отдаются
    for request in (get_responsibles, partial(get_responsibles, {'service__is_exportable': 'true'})):
        result = request().json()
        assert result['count'] == (
            ServiceMember
            .objects
            .filter(service__is_exportable=True)
            .responsibles()
            .count()
        )
        assert {responsible_data['id'] for responsible_data in result['results']} == set(
            ServiceMember
            .objects
            .filter(service__is_exportable=True)
            .responsibles()
            .values_list('id', flat=True)
        )

    result = get_responsibles({'service__is_exportable': 'false'}).json()
    assert result['count'] == (
        ServiceMember
        .objects
        .filter(service__is_exportable=False)
        .responsibles()
        .count()
    )
    assert {responsible_data['id'] for responsible_data in result['results']} == set(
        ServiceMember
        .objects
        .filter(service__is_exportable=False)
        .responsibles()
        .values_list('id', flat=True)
    )

    result = get_responsibles({'service__is_exportable__in': 'true,false'}).json()
    assert result['count'] == ServiceMember.objects.responsibles().count()


def test_v3_api_is_read_only(client, data):
    client.login(data.owner_of_service.staff.login)

    response = client.json.post(
        reverse('api-v3:service-responsible-list'),
        {
            'service': data.service.id,
            'person': data.stranger.login,
        }
    )
    assert response.status_code == 405


@pytest.mark.parametrize('endpoint', (
    'services-api:responsible-list',
    'api-v3:service-responsible-list',
    'api-v4:service-responsible-list',
    'api-frontend:service-responsible-list',
))
def test_pesponsible_restricted(client, data, endpoint, staff_factory):
    staff = staff_factory('own_only_viewer')
    client.login(staff.login)
    factories.ServiceMemberFactory(service=data.service, staff=staff)

    response = client.get(reverse(endpoint))
    assert response.status_code == 200
    responsibles = ServiceMember.objects.filter(service=data.service).responsibles()
    assert response.json()['count'] == responsibles.count()

    staff = staff_factory('services_viewer')
    client.login(staff.login)

    response = client.get(reverse(endpoint))
    assert response.status_code == 200
    responsibles = ServiceMember.objects.responsibles()
    assert response.json()['count'] == responsibles.count()


def test_get_service_responsible(client, data, staff_factory):
    staff = staff_factory('own_only_viewer')
    client.login(staff.login)
    factories.ServiceMemberFactory(service=data.service, staff=staff)
    response = client.get(reverse('api-v4:service-responsible-list'), {'service': 100500})
    assert response.data['count'] == 0
    response = client.get(reverse('api-v4:service-responsible-list'), {'service': data.service.id})
    responsibles = ServiceMember.objects.filter(service=data.service).responsibles()
    assert response.json()['count'] == responsibles.count()
