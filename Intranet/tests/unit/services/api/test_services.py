from collections import Counter
from datetime import timedelta
from functools import partial
from typing import Dict, Any
from unittest import mock

import itertools
import pytest
from django.conf import settings
from django.contrib.auth.models import Permission
from django.core.urlresolvers import reverse
from django.test.utils import override_settings
from django.utils import timezone
from mock import patch
from pretend import stub
from waffle.testutils import override_switch

from common import factories
from plan.oebs.tasks.finish_approve import finish_oebs_approve_process
from plan.common.utils.dates import datetime_isoformat_with_microseconds
from plan.duty.models import Role
from plan.services.api.services import ServicesView
from plan.services.models import (
    Service,
    ServiceCreateRequest,
    ServiceSuspiciousReason,
    ServiceType,
    ServiceCloseRequest,
    ServiceTag,
)
from plan.services.tasks import update_service_fields
from plan.staff.models import ServiceScope
from plan.suspicion.constants import LEVELS
from plan.suspicion.models import ServiceIssue, IssueGroup, ServiceTrafficStatus
from plan.suspicion.tasks import check_issue_groups
from utils import iterables_are_equal
from plan.denormalization.check import check_obj_with_denormalized_fields
from plan.services.tasks import close_service
from plan.oebs.models import OEBSAgreement, ACTIONS, STATES
from plan.oebs.utils import finalize_flag_changing

pytestmark = pytest.mark.django_db


def check_services(client, params, services):
    response = client.json.get(
        reverse('services-api:service-list'),
        params
    )
    assert response.status_code == 200

    expected_ids = Counter([s.id for s in services])
    real_ids = Counter([s['id'] for s in response.json()['results']])
    assert real_ids == expected_ids


def change_flags(
    client, api, service, use_for_hardware,
    use_for_hr, use_for_procurement, use_for_revenue,
):
    patch_data = {
        'use_for_hardware': use_for_hardware,
        'use_for_hr': use_for_hr,
        'use_for_procurement': use_for_procurement,
        'use_for_revenue': use_for_revenue,
    }
    patch_data = {k: v for k, v in patch_data.items() if v is not None}
    return client.json.patch(
        reverse(f'{api}:service-detail', args=[service.id]),
        data=patch_data,
    )


def test_get_services(client, data):
    issue_group_1 = IssueGroup.objects.create(name='Имя', name_en='Name', code='old')
    issue_group_2 = IssueGroup.objects.create(name='Имя', name_en='Name', code='new')
    traffic_status_1 = ServiceTrafficStatus.objects.create(
        issue_group=issue_group_1, service=data.metaservice, issue_count=5, level='ok'
    )
    traffic_status_2 = ServiceTrafficStatus.objects.create(
        issue_group=issue_group_2, service=data.metaservice, issue_count=5, level='ok'
    )

    response = client.json.get(reverse('services-api:service-list'))

    assert response.status_code == 200

    json = response.json()['results']
    assert len(json) == 4
    assert json[0]['slug'] == data.metaservice.slug
    assert json[0]['name'] == {'ru': data.metaservice.name, 'en': data.metaservice.name_en}
    assert json[0]['owner']['first_name'] == {
        'ru': data.metaservice.owner.first_name,
        'en': data.metaservice.owner.first_name_en,
    }
    assert json[0]['owner']['name'] == {
        'ru': '%s %s' % (data.metaservice.owner.first_name, data.metaservice.owner.last_name),
        'en': '%s %s' % (data.metaservice.owner.first_name_en, data.metaservice.owner.last_name_en),
    }
    assert json[0]['is_suspicious'] is False
    service_type = data.metaservice.service_type
    assert json[0]['type'] == {
        'name': {'ru': service_type.name, 'en': service_type.name_en}, 'code': service_type.code, 'id': service_type.id
    }

    assert json[0]['membership_inheritance'] is True
    assert json[0]['sandbox_move_date'] is None

    assert json[0]['traffic_light'] == [
        {
            'id': traffic_status_2.id,
            'group': {
                'id': issue_group_2.id,
                'code': issue_group_2.code,
                'name': {
                    'ru': issue_group_2.name,
                    'en': issue_group_2.name_en,
                }
            },
            'issue_count': traffic_status_2.issue_count,
            'level': traffic_status_2.level,
        },
        {
            'id': traffic_status_1.id,
            'group': {
                'id': issue_group_1.id,
                'code': issue_group_1.code,
                'name': {
                    'ru': issue_group_1.name,
                    'en': issue_group_1.name_en,
                }
            },
            'issue_count': traffic_status_1.issue_count,
            'level': traffic_status_1.level,
        }
    ]


def test_get_services_num_queries(client, django_assert_num_queries):
    departments = [factories.DepartmentFactory() for _ in range(10)]
    services = [factories.ServiceFactory() for _ in range(100)]
    for service in services:
        for department in departments:
            service.departments.add(department)

    waffle_query_count = 1
    total_query_limit = 9 + waffle_query_count

    with django_assert_num_queries(total_query_limit):
        response = client.json.get(reverse('services-api:service-list'))

    assert response.status_code == 200
    data = response.json()
    assert len(data['results']) == 20  # Максимальный размер страницы по умолчанию - 20


def test_services_robot_count_and_external_members(client, data):
    user_john = factories.UserFactory()
    user_ext = factories.UserFactory()
    user_robot = factories.UserFactory()
    staff_john = factories.StaffFactory(user=user_john, first_name='John', last_name='Doe',
                                        affiliation='yandex', is_robot=False)
    staff_ext = factories.StaffFactory(user=user_ext, first_name='User', last_name='Ext',
                                       affiliation='external', is_robot=False)
    staff_robot = factories.StaffFactory(user=user_robot, first_name='User', last_name='Rob', is_robot=True)
    cat_role = factories.RoleFactory()
    mouse_role = factories.RoleFactory()

    def up(service):
        check_obj_with_denormalized_fields(service, Service.DENORMALIZED_FIELDS, fix=True)
        response_up = client.json.get(reverse('services-api:service-detail', args=[service.id]))
        assert response_up.status_code == 200
        return response_up

    # запомним количество людей и роботов в сервисе до добавления новых
    response = up(data.metaservice)
    robots_count_before = response.json()['unique_immediate_robots_count']
    members_count_before = response.json()['unique_immediate_members_count']
    external_members_count_before = response.json()['unique_immediate_external_members_count']

    # добавляем одного робота
    # ожидаем, что увеличится только исходное количество роботов на единицу
    factories.ServiceMemberFactory(
        service=data.metaservice,
        staff=staff_robot,
        role=mouse_role,
    )

    response = up(data.metaservice)
    assert response.json()['unique_immediate_robots_count'] == robots_count_before + 1
    assert response.json()['unique_immediate_members_count'] == members_count_before
    assert response.json()['unique_immediate_external_members_count'] == external_members_count_before

    # добавляем робота с другой ролью
    # не ожидаем изменений
    factories.ServiceMemberFactory(
        service=data.metaservice,
        staff=staff_robot,
        role=cat_role,
    )

    response = up(data.metaservice)
    assert response.json()['unique_immediate_robots_count'] == robots_count_before + 1
    assert response.json()['unique_immediate_members_count'] == members_count_before
    assert response.json()['unique_immediate_external_members_count'] == external_members_count_before

    # добавляем внешнего неробота
    # ожидаем, что увеличится только исходное количество людей на единицу
    factories.ServiceMemberFactory(
        service=data.metaservice,
        staff=staff_ext,
        role=mouse_role,
    )

    response = up(data.metaservice)
    assert response.json()['unique_immediate_robots_count'] == robots_count_before + 1
    assert response.json()['unique_immediate_members_count'] == members_count_before + 1
    assert response.json()['unique_immediate_external_members_count'] == external_members_count_before + 1

    # добавляем еще раз внешнего неробота с новой ролью
    # не ожидаем изменений
    factories.ServiceMemberFactory(
        service=data.metaservice,
        staff=staff_ext,
        role=cat_role,
    )

    response = up(data.metaservice)
    assert response.json()['unique_immediate_robots_count'] == robots_count_before + 1
    assert response.json()['unique_immediate_members_count'] == members_count_before + 1
    assert response.json()['unique_immediate_external_members_count'] == external_members_count_before + 1

    # добавляем неробота
    # ожидаем, что увеличится только исходное количество людей на единицу
    factories.ServiceMemberFactory(
        service=data.metaservice,
        staff=staff_john,
        role=mouse_role,
    )

    response = up(data.metaservice)
    assert response.json()['unique_immediate_robots_count'] == robots_count_before + 1
    assert response.json()['unique_immediate_members_count'] == members_count_before + 2
    assert response.json()['unique_immediate_external_members_count'] == external_members_count_before + 1

    # добавляем еще раз неробота c другой ролью
    # не ожидаем изменений
    factories.ServiceMemberFactory(
        service=data.metaservice,
        staff=staff_john,
        role=mouse_role,
    )

    response = up(data.metaservice)
    assert response.json()['unique_immediate_robots_count'] == robots_count_before + 1
    assert response.json()['unique_immediate_members_count'] == members_count_before + 2
    assert response.json()['unique_immediate_external_members_count'] == external_members_count_before + 1

    assert response.json()['unique_immediate_robots_count'] == data.metaservice.unique_immediate_robots_count
    assert response.json()['unique_immediate_members_count'] == data.metaservice.unique_immediate_members_count
    assert response.json()['unique_immediate_external_members_count'] == external_members_count_before + 1

    assert response.json()['has_external_members'] == data.metaservice.has_external_members


def test_get_and_edit_departments(client, data):
    department1 = factories.DepartmentFactory()
    department2 = factories.DepartmentFactory()

    request = partial(
        client.json.patch,
        reverse('services-api:service-detail', args=[data.service.pk])
    )

    # один департамент
    response = request({'departments': [department1.id]})
    assert response.status_code == 200

    response = client.json.get(reverse('services-api:service-detail', args=[data.service.id]))
    json = response.json()
    assert len(json['departments']) == data.service.departments.count()

    # если проставляем второй
    response = request({'departments': [department1.id, department2.id]})
    assert response.status_code == 200

    response = client.json.get(reverse('services-api:service-detail', args=[data.service.id]))
    json = response.json()
    assert len(json['departments']) == data.service.departments.count()


@pytest.mark.parametrize('is_superuser', [True, False])
def test_available_states_for_base(client, data, is_superuser):
    factories.ServiceTagFactory(slug=settings.BASE_TAG_SLUG)
    staff = data.service.owner
    staff.user.is_superuser = is_superuser
    staff.user.save()
    service = data.service
    service.is_base = True
    service.save()

    client.login(staff.login)
    with patch('plan.services.views.catalog.serializers.idm_actions') as idm_actions:
        idm_actions.get_chown_requests.return_value = []
        idm_actions.get_roles_with_permissions.return_value = []
        response = client.json.get(reverse('services:service', args=[service.pk]))
    states = [
        state['id']
        for state in response.json()['content']['service']['availableStates']
    ]

    full_list = ['supported', 'needinfo', 'closed', 'deleted']
    partial_list = ['supported', 'needinfo']

    if is_superuser:
        assert states == full_list
    else:
        assert states == partial_list


def test_available_states_with_execution(client, data):
    staff = data.service.owner
    service = data.service
    service.state = Service.states.NEEDINFO
    service.save()

    service_issue = factories.ServiceIssueFactory(service=service, state='active')
    execution = factories.ExecutionFactory(code='change_state_to_need_info')
    factories.ServiceExecutionActionFactory(service_issue=service_issue, applied_at=timezone.now(), execution=execution)

    client.login(staff.login)
    with patch('plan.services.views.catalog.serializers.idm_actions') as idm_actions:
        idm_actions.get_chown_requests.return_value = []
        idm_actions.get_roles_with_permissions.return_value = []
        response = client.json.get(reverse('services:service', args=[service.pk]))
    states = [
        state['id']
        for state in response.json()['content']['service']['availableStates']
    ]

    assert states == ['closed', 'deleted']


# права на смену есть только у владельцев сервиса или родительских сервисов или суперюзеров
@pytest.mark.parametrize('endpoint_path', ('services-api:service-detail', 'api-v3:service-detail'))
def test_edit_membership_inheritance_with_owner_service(client, data, endpoint_path):
    client.login(data.child.owner.login)
    with patch('plan.services.tasks.notify_staff') as notify:
        response = client.json.patch(
            reverse(endpoint_path, args=[data.child.id]),
            data={'membership_inheritance': False}
        )
    notify.delay_on_commit.assert_has_calls([
        mock.call(data.child.parent.id),
        mock.call(data.child.parent.parent.id)
    ])
    assert response.status_code == 200


@pytest.mark.parametrize('endpoint_path', ('services-api:service-detail', 'api-v3:service-detail'))
def test_edit_membership_inheritance_enable_with_owner_of_parent(client, data, endpoint_path):
    client.login(data.staff.login)
    data.child.membership_inheritance = False
    data.child.save()
    check_obj_with_denormalized_fields(data.child, Service.DENORMALIZED_FIELDS, fix=True)
    with patch('plan.services.tasks.notify_staff') as notify:
        response = client.json.patch(reverse(endpoint_path, args=[data.child.id]), data={'membership_inheritance': True})
    assert response.status_code == 403
    message = response.json()['error']['message']['ru']
    assert message == 'Включение наследования прав отключено, если оно вам необходимо - обратитесь в поддержку'
    notify.delay_on_commit.assert_not_called()
    response = client.json.get(reverse('services-api:service-detail', args=[data.child.id]))
    json = response.json()
    assert json['membership_inheritance'] is False


# staff.login - владелец родителя
@pytest.mark.parametrize('endpoint_path', ('services-api:service-detail', 'api-v3:service-detail'))
def test_edit_membership_inheritance_with_owner_of_parent(client, data, endpoint_path):
    client.login(data.staff.login)

    check_obj_with_denormalized_fields(data.child, Service.DENORMALIZED_FIELDS, fix=True)

    response = client.json.patch(reverse(endpoint_path, args=[data.child.id]), data={'membership_inheritance': False})
    assert response.status_code == 200

    response = client.json.get(reverse('services-api:service-detail', args=[data.child.id]))
    json = response.json()
    assert json['membership_inheritance'] is False


# big_boss.login - владелец родителя родителя
@pytest.mark.parametrize('endpoint_path', ('services-api:service-detail', 'api-v3:service-detail'))
def test_edit_membership_inheritance_with_owner_of_parent_parent(client, data, endpoint_path):
    client.login(data.big_boss.login)

    check_obj_with_denormalized_fields(data.child, Service.DENORMALIZED_FIELDS, fix=True)

    response = client.json.patch(reverse(endpoint_path, args=[data.child.id]), data={'membership_inheritance': False})
    assert response.status_code == 200

    response = client.json.get(reverse('services-api:service-detail', args=[data.child.id]))
    json = response.json()
    assert json['membership_inheritance'] is False


# смена флага суперюзером
@pytest.mark.parametrize('endpoint_path', ('services-api:service-detail', 'api-v3:service-detail'))
def test_edit_membership_inheritance_with_superuser(client, data, endpoint_path):
    superuser = factories.StaffFactory(user=factories.UserFactory(is_superuser=True))
    client.login(superuser.login)

    check_obj_with_denormalized_fields(data.child, Service.DENORMALIZED_FIELDS, fix=True)

    response = client.json.patch(reverse(endpoint_path, args=[data.child.id]), data={'membership_inheritance': False})
    assert response.status_code == 200

    response = client.json.get(reverse('services-api:service-detail', args=[data.child.id]))
    json = response.json()
    assert json['membership_inheritance'] is False


@pytest.mark.parametrize('endpoint_path', ('services-api:service-detail', 'api-v3:service-detail'))
def test_edit_membership_inheritance_enable_with_superuser(client, data, endpoint_path):
    superuser = factories.StaffFactory(user=factories.UserFactory(is_superuser=True))
    client.login(superuser.login)
    data.child.membership_inheritance = False
    data.child.save()

    check_obj_with_denormalized_fields(data.child, Service.DENORMALIZED_FIELDS, fix=True)

    response = client.json.patch(reverse(endpoint_path, args=[data.child.id]), data={'membership_inheritance': True})
    assert response.status_code == 200

    response = client.json.get(reverse('services-api:service-detail', args=[data.child.id]))
    json = response.json()
    assert json['membership_inheritance'] is True


def test_filter_services_by_id(client, data):
    check_services(
        client,
        {'id': data.service.pk},
        [data.service]
    )

    check_services(
        client,
        {'id__in': '%s,%s' % (data.service.pk, data.other_service.pk)},
        [data.service, data.other_service]
    )


def test_filter_services_by_slug(client, data):
    check_services(
        client,
        {'slug': data.service.slug},
        [data.service]
    )

    check_services(
        client,
        {'slug__in': '%s,%s' % (data.service.slug, data.other_service.slug)},
        [data.service, data.other_service]
    )

    check_services(
        client,
        {'slug__contains': 'slu'},
        Service.objects.filter(slug__contains='slu', is_exportable=True)
    )


# эти тесты нужно разскипать, когда в тестах подключится postgresql
@pytest.mark.skip
def test_filter_by_slug_is_case_insensetive(client, data):
    check_services(
        client,
        {'slug': data.service.slug.title()},
        [data.service]
    )


@pytest.mark.skip
def test_filter_by_parent_slug_is_case_insensetive(client, data):
    check_services(
        client,
        {'parent__slug': data.metaservice.slug.title()},
        [data.service, data.other_service],
    )


@pytest.mark.skip
def test_filter_by_parent_slug_with_descendants_is_case_insensetive(client, data):
    service = factories.ServiceFactory(parent=data.other_service)
    check_services(
        client,
        {'parent__with_descendants__slug': data.metaservice.slug.title()},
        [data.service, data.other_service, service]
    )


def test_filter_by_type(client):
    type1 = factories.ServiceTypeFactory()
    type2 = factories.ServiceTypeFactory()
    service1 = factories.ServiceFactory(service_type=type1)
    factories.ServiceFactory(service_type=type2)
    check_services(client, {'type': type1.code}, [service1])


def test_filter_services_by_name(client, data):
    check_services(
        client,
        {'name': data.service.name},
        [data.service]
    )

    check_services(
        client,
        {'name_en': data.other_service.name_en},
        [data.other_service]
    )


def test_filter_services_by_state(client, data):
    data.service.state = 'closed'
    data.service.save()
    data.other_service.state = 'supported'
    data.other_service.save()

    check_services(
        client,
        {'state': 'closed'},
        [data.service]
    )

    check_services(
        client,
        {'state__in': 'needinfo,supported'},
        [data.other_service]
    )


@pytest.mark.parametrize('endpoint_path', ('services-api', 'api-v3', 'api-v4', 'api-frontend'))
def test_filter_deleted_services(client, data, endpoint_path):
    data.service.state = 'deleted'
    data.service.save()

    # Удаленные сервисы не возвращаются в list для frontend, для остальных - возвращаются
    response = client.json.get(
        reverse(endpoint_path + ':service-list'),
        {'slug': data.service.slug}
    )
    assert response.status_code == 200
    results = response.json()['results']

    if endpoint_path == 'api-frontend':
        assert len(results) == 0
    else:
        assert len(results) == 1

    # Удаленные сервисы возвращаются в detail
    response = client.json.get(
        reverse(endpoint_path + ':service-detail', args=[data.service.id])
    )
    assert response.status_code == 200
    assert response.json()['id'] == data.service.id


def test_filter_services_by_readonly(client, data):
    data.service.readonly_state = 'moving'
    data.service.save()
    data.other_service.readonly_state = 'renaming'
    data.other_service.save()

    check_services(
        client,
        {'readonly_state': 'moving'},
        [data.service]
    )

    check_services(
        client,
        {'readonly_state__in': 'renaming,creating'},
        [data.other_service]
    )


def test_filter_services_by_keywords(client, data):
    data.other_service.keywords = 'xxx yyy'
    data.other_service.save()

    check_services(
        client,
        {'keywords': 'xxx yyy'},
        [data.other_service]
    )

    check_services(
        client,
        {'keywords__contains': 'xxx'},
        [data.other_service]
    )

    check_services(
        client,
        {'keywords': 'zzz'},
        []
    )


def test_filter_services_by_parent(client, data):
    check_services(
        client,
        {'parent': data.metaservice.pk},
        [data.service, data.other_service]
    )

    service = factories.ServiceFactory()
    check_services(
        client,
        {'parent__in': '%s,%s' % (data.metaservice.pk, service.pk)},
        [data.service, data.other_service]
    )


def test_filter_services_by_parent_slug(client, data):
    check_services(
        client,
        {'parent__slug': data.metaservice.slug},
        [data.service, data.other_service]
    )

    service = factories.ServiceFactory()
    check_services(
        client,
        {'parent__slug__in': '%s,%s' % (data.metaservice.slug, service.slug)},
        [data.service, data.other_service]
    )


def test_filter_services_by_parent_with_descendants(client, data):
    service = factories.ServiceFactory(parent=data.other_service)

    data.child.state = 'closed'
    data.child.save()
    check_services(
        client,
        {'parent__with_descendants': data.metaservice.id},
        [data.service, data.child, data.other_service, service]
    )

    check_services(
        client,
        {'parent__with_descendants__slug': data.metaservice.slug},
        [data.service, data.child, data.other_service, service]
    )


def test_filter_services_by_parent_isnull(client, data):
    check_services(
        client,
        {'parent__isnull': True},
        [data.metaservice]
    )

    check_services(
        client,
        {'parent__isnull': False},
        [data.service, data.child, data.other_service]
    )


def test_filter_services_by_tag(client, data):
    tag1 = factories.ServiceTagFactory()
    tag2 = factories.ServiceTagFactory()
    data.service.tags.add(tag1)

    check_services(
        client,
        {'tags': tag1.pk},
        [data.service]
    )

    check_services(
        client,
        {'tags': (tag1.pk, tag2.pk)},
        [data.service]
    )


def test_filter_services_by_tag_slug(client, data):
    tag1 = factories.ServiceTagFactory()
    tag2 = factories.ServiceTagFactory()
    data.service.tags.add(tag1)

    check_services(
        client,
        {'tags__slug': tag1.slug},
        [data.service]
    )

    check_services(
        client,
        {'tags__slug__in': '%s,%s' % (tag1.slug, tag2.slug)},
        [data.service]
    )


def test_filter_services_by_member(client, data):

    check_services(
        client,
        {'member': data.staff.login},
        [data.service]
    )

    check_services(
        client,
        {'member': [data.big_boss.login, data.other_staff.login]},
        [data.metaservice, data.other_service]
    )


def test_filter_services_by_responsible(client, data):
    check_services(
        client,
        {'responsible': data.responsible.staff.login},
        [data.service]
    )


def test_filter_services_by_responsible_with_multiple_responsible(client, data):
    role_owner = factories.RoleFactory(code=Role.DEPUTY_OWNER)
    role_responsible = factories.RoleFactory(code=Role.RESPONSIBLE)
    factories.ServiceMemberFactory(service=data.service, staff=data.staff, role=role_owner)
    factories.ServiceMemberFactory(service=data.service, staff=data.staff, role=role_responsible)

    check_services(
        client,
        {'responsible': data.staff.login},
        [data.service]
    )


def test_filter_services_by_department(client, data):

    check_services(
        client,
        {'department': data.staff.department.pk},
        [data.service]
    )

    check_services(
        client,
        {'department': [data.big_boss.department.pk, data.other_staff.department.pk]},
        [data.metaservice, data.other_service]
    )


def test_filter_by_exportable(client, data):
    check_services(
        client,
        params={},
        services=Service.objects.filter(is_exportable=True)
    )

    factories.ServiceFactory(is_exportable=False)

    check_services(
        client,
        params={'is_exportable': False},
        services=Service.objects.filter(is_exportable=False)
    )

    check_services(
        client,
        {'is_exportable__in': 'true,false'},
        services=Service.objects.all()
    )


def test_filter_by_suspicious(client):
    # положим, существует один подозрительный и один неподозрительный сервис
    suspicious_service = factories.ServiceFactory(
        suspicious_date=timezone.now().date() - timedelta(days=20)
    )

    not_suspicious_service = factories.ServiceFactory(suspicious_date=None)

    assert suspicious_service.is_suspicious is True
    assert not_suspicious_service.is_suspicious is False

    check_services(client, params={'is_suspicious': 'true'}, services=Service.objects.exclude(suspicious_date=None))
    check_services(client, params={'is_suspicious': 'false'}, services=Service.objects.filter(suspicious_date=None))
    check_services(client, params={'is_suspicious': 'true, false'}, services=Service.objects.all())
    check_services(client, params={}, services=Service.objects.all())


def test_filter_by_has_external_members(client):
    normal_service = factories.ServiceFactory()
    external_service = factories.ServiceFactory()
    external_staff = factories.StaffFactory(affiliation='external')
    factories.ServiceMemberFactory(service=external_service, staff=external_staff)
    check_obj_with_denormalized_fields(external_service, Service.DENORMALIZED_FIELDS, fix=True)
    external_service.refresh_from_db()
    assert external_service.has_external_members is True
    check_services(client, {'has_external_members__in': 'true,false'}, [normal_service, external_service])
    check_services(client, {'has_external_members': True}, [external_service])
    check_services(client, {'has_external_members': False}, [normal_service])


def test_filter_by_membership_inheritance(client):
    factories.ServiceFactory()
    inheritance_service = factories.ServiceFactory(membership_inheritance=True)
    response = client.json.get(
        reverse('api-frontend:service-list'),
        {'membership_inheritance': True}
    )
    assert response.status_code == 200
    data = response.json()['results']
    assert len(data) == 1
    assert data[0]['id'] == inheritance_service.id


def test_search(client):
    service = factories.ServiceFactory(slug='somali_cat')
    check_services(
        client,
        {'search': service.slug[-3:]},
        [service]
    )


def test_get_service(client, data):
    response = client.json.get(reverse('services-api:service-detail', args=[data.metaservice.id]))

    assert response.status_code == 200

    json = response.json()
    assert json['slug'] == data.metaservice.slug
    assert json['name'] == {'ru': data.metaservice.name, 'en': data.metaservice.name_en}
    assert json['readonly_state'] is None
    assert json['state'] == 'develop'
    assert json['state_display_i18n'] == 'Развивается'
    assert json['state_display'] == {
        'ru': 'Развивается',
        'en': 'Развивается',
    }
    assert json['is_suspicious'] is False


def test_get_service_select_fields(client, data):
    fields = ['id', 'name']
    response = client.json.get(
        reverse('services-api:service-detail', args=[data.metaservice.id]),
        {'fields': ','.join(fields)}
    )

    assert response.status_code == 200
    assert set(response.json().keys()) == set(fields)


def test_kpi(client, data):
    data.service.kpi_bugs_count = 1
    data.service.kpi_release_count = 2
    data.service.kpi_lsr_count = 3
    data.service.save()

    client.login(data.staff.login)

    response = client.json.get(
        reverse('services-api:service-detail', args=[data.service.id]),
        {'fields': 'kpi'}
    )

    assert response.status_code == 200

    json = response.json()
    assert json['kpi']['bugs_count'] == 1
    assert json['kpi']['releases_count'] == 2
    assert json['kpi']['lsr_count'] == 3


def test_get_service_moving(client, data):
    data.metaservice.readonly_state = 'moving'
    data.metaservice.save()

    response = client.json.get(reverse('services-api:service-detail', args=[data.metaservice.id]))

    assert response.status_code == 200

    json = response.json()
    assert json['slug'] == data.metaservice.slug
    assert json['readonly_state'] == Service.MOVING


@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4'))
def test_create_service(api, client, data):
    """
    Для ручки frontend есть отдельный тест
    """
    client.login(data.staff.login)

    with patch('plan.services.tasks.register_service') as register_service:
        response = client.json.post(
            reverse(f'{api}:service-list'),
            {
                'name': 'Сервис 3',
                'slug': 'slug3',
                'owner': data.staff.login,
            }
        )
        assert response.status_code == 400

        response = client.json.post(
            reverse(f'{api}:service-list'),
            {
                'name': {'ru': 'Сервис 3'},
                'slug': 'slug3',
                'owner': data.staff.login,
            }
        )
        assert response.status_code == 400

        response = client.json.post(
            reverse(f'{api}:service-list'),
            {
                'name': {'ru': 'Сервис 3', 'en': 'Service 3'},
                'slug': 'slug3',
                'owner': data.staff.login,
            }
        )
        assert response.status_code == 201

        assert register_service.apply_async.called

    service = Service.objects.get(slug='slug3')
    service.fetch_owner()

    assert service.parent == data.meta_other
    assert service.name == 'Сервис 3'
    assert service.name_en == 'Service 3'
    assert service.state == Service.states.IN_DEVELOP
    assert service.owner == data.staff

    assert ServiceCreateRequest.objects.filter(service=service).exists()


@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4', 'api-frontend'))
@pytest.mark.parametrize('switch_active', (True, False))
@pytest.mark.parametrize('going_to_meta_other', (True, False))
@pytest.mark.parametrize('available_type', (True, False))
@pytest.mark.parametrize('send_type', (True, False))
def test_create_service_check_type(
    api, client, data, switch_active,
    going_to_meta_other, available_type, send_type
):
    client.login(data.staff.login)
    service_type = factories.ServiceTypeFactory()
    parent = data.service
    if available_type:
        service_type.available_parents.add(parent.service_type)
    create_data = {
        'name': {'ru': 'Сервис smth', 'en': 'Service smth'},
        'slug': 'slug_smth',
        'description': {'ru': 'Описание сервиса', 'en': 'Service description'},
        'owner': data.staff.login,
    }
    if send_type:
        create_data['type'] = service_type.code
    if not going_to_meta_other:
        create_data['parent'] = parent.id

    with patch('plan.services.tasks.register_service') as register_service:
        with override_switch(settings.SWITCH_CHECK_ALLOWED_PARENT_TYPE, active=switch_active):
            response = client.json.post(
                reverse(f'{api}:service-list'),
                create_data,
            )

    if switch_active and (
        (not available_type and not going_to_meta_other)
        or not send_type
    ):
        assert response.status_code == 400, response.json()
        register_service.apply_async.assert_not_called()
    else:
        assert response.status_code == 201, response.json()
        register_service.apply_async.assert_called_once()

        service = Service.objects.get(slug='slug_smth')
        service.fetch_owner()
        assert service.parent == data.meta_other if going_to_meta_other else parent
        assert service.name == 'Сервис smth'
        assert service.name_en == 'Service smth'
        assert service.state == Service.states.IN_DEVELOP
        assert service.owner == data.staff
        assert service.service_type.code == service_type.code if send_type else 'undefined'

        assert ServiceCreateRequest.objects.filter(service=service).exists()


@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4'))
def test_create_service_description(api, client, data):
    """
    Для ручки frontend есть отдельный тест
    """
    client.login(data.staff.login)

    with patch('plan.services.tasks.register_service'):
        response = client.json.post(
            reverse(f'{api}:service-list'),
            {
                'name': {'ru': 'Котики, вперёд!', 'en': 'Cats'},
                'slug': 'cats',
                'owner': data.staff.login,
                'description': {'ru': 'Теперь понятно, летим обратно!'},
            }
        )

    assert response.status_code == 201

    description = ''
    description_en = ''
    service = Service.objects.get(slug='cats')
    if api == 'api-v4':
        description = 'Теперь понятно, летим обратно!'

    assert service.description == description
    assert service.description_en == description_en

    with patch('plan.services.tasks.register_service'):
        response = client.json.post(
            reverse(f'{api}:service-list'),
            {
                'name': {'ru': 'Сервис 3', 'en': 'Service 3'},
                'slug': 'slug3',
                'owner': data.staff.login,
                'description': {'ru': 'Описание сервиса', 'en': 'Service description'},
            }
        )

    assert response.status_code == 201

    description = ''
    description_en = ''
    service = Service.objects.get(slug='slug3')
    if api == 'api-v4':
        description = 'Описание сервиса'
        description_en = 'Service description'

    assert service.description == description
    assert service.description_en == description_en


@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4', 'api-frontend'))
def test_create_service_no_owner(api, client, data):
    client.login(data.staff.login)

    with patch('plan.services.tasks.register_service'):
        response = client.json.post(
            reverse(f'{api}:service-list'),
            {
                'name': {'ru': 'Сервис 3', 'en': 'Service 3'},
                'slug': 'slug3',
                'description': {'ru': 'Описание сервиса', 'en': 'Service description'},
            }
        )

    assert response.status_code == 400


@pytest.mark.parametrize('with_permissions', [False, True])
@pytest.mark.parametrize('api', ('services-api', 'api-v4', 'api-frontend'))
def test_create_service_tags(api, client, data, with_permissions):
    tag_1 = factories.ServiceTagFactory(slug='mr')
    tag_2 = factories.ServiceTagFactory(slug='mau')
    if with_permissions:
        for tag in (tag_1, tag_2):
            data.staff.user.user_permissions.add(Permission.objects.get(codename=f'change_service_tag_{tag.slug}'))

    client.login(data.staff.login)

    with patch('plan.services.tasks.register_service'):
        response = client.json.post(
            reverse(f'{api}:service-list'),
            {
                'name': {'ru': 'Котики вперёд!', 'en': 'Cats'},
                'slug': 'cats',
                'owner': data.staff.login,
                'tags': [tag_1.id, tag_2.id],
                'description': {'ru': 'Описание сервиса', 'en': 'Service description'},
            }
        )

    assert response.status_code == 201

    tags = set()
    service = Service.objects.get(slug='cats')
    tags = set([tag_1.id, tag_2.id])

    assert set(service.tags.values_list('id', flat=True)) == tags


def test_create_service_tags_forbidden(client, person, data, django_assert_num_queries):
    """
    Если на тег сервиса кому-либо выданы разрешения,
    то для его создания нужно иметь разрешение на редактирование этого тега.
    """
    tag = factories.ServiceTagFactory(slug='mr')
    person.user.user_permissions.add(Permission.objects.get(codename=f'change_service_tag_{tag.slug}'))

    client.login(data.staff.login)
    with patch('plan.services.tasks.register_service'):
        with django_assert_num_queries(14):
            # 2 selects intranet_staff + django_content_type
            # 1 select services_service
            # 3 exists() selects from services_service
            # 2 waffle flags
            # 1 select intranet_staff
            # 2 selects services_servicetype + services_servicetag
            # 1 select auth_user_permissions
            # 2 selects pg_in_in_recovery + waffle
            response = client.json.post(
                reverse('api-frontend:service-list'),
                {
                    'name': {'ru': 'Котики вперёд!', 'en': 'Cats'},
                    'slug': 'cats',
                    'owner': data.staff.login,
                    'tags': [tag.id],
                    'description': {'ru': 'Описание сервиса', 'en': 'Service description'},
                }
            )
    assert response.status_code == 403


@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4', 'api-frontend'))
@pytest.mark.parametrize('is_superuser', [True, False])
def test_create_service_under_base(api, client, data, is_superuser):
    data.staff.user.is_superuser = is_superuser
    data.staff.user.save()
    base_service = factories.ServiceFactory(is_base=True)
    factories.ServiceFactory(is_base=True, parent=base_service)
    client.login(data.staff.login)

    with patch('plan.services.tasks.register_service') as register_service:
        response = client.json.post(
            reverse(f'{api}:service-list'),
            {
                'name': {'ru': 'Сервис 3', 'en': 'Service 3'},
                'slug': 'slug3',
                'parent': base_service.pk,
                'owner': data.staff.login,
                'description': {'ru': 'Описание сервиса', 'en': 'Service description'},
            }
        )
    if is_superuser:
        assert response.status_code == 201

        assert register_service.apply_async.called

        service = Service.objects.get(slug='slug3')
        service.fetch_owner()
        assert service.parent == base_service  # data.meta_other
        assert service.name == 'Сервис 3'
        assert service.name_en == 'Service 3'
        assert service.state == Service.states.IN_DEVELOP
        assert service.owner == data.staff

        assert ServiceCreateRequest.objects.filter(service=service).exists()
    else:
        assert response.status_code == 403
        assert response.json()['error']['code'] == 'incorrect_parent'


@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4', 'api-frontend'))
def test_create_service_wrong_type(api, client, data):
    client.login(data.staff.login)
    response = client.json.post(
        reverse(f'{api}:service-list'),
        {
            'name': {'ru': 'any_name', 'en': 'any_name'},
            'slug': 'any_slug',
            'owner': data.staff.login,
            'type': 'xxx',
            'description': {'ru': 'Описание сервиса', 'en': 'Service description'},
        }
    )
    assert response.status_code == 400


@mock.patch('plan.api.idm.actions.add_service', mock.Mock())
@mock.patch('plan.api.idm.actions.add_service_head', mock.Mock())
@mock.patch('plan.api.idm.actions.move_service', mock.Mock())
@mock.patch('plan.api.idm.actions.assert_service_node_exists', mock.Mock())
@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4', 'api-frontend'))
def test_create_service_in_same_hierarchy(api, client, data):
    client.login(data.big_boss.login)
    with patch('plan.services.tasks.rerequest_roles'):
        with patch('plan.idm.manager.Manager._run_request'):
            response = client.json.post(
                reverse(f'{api}:service-list'),
                {
                    'name': {'ru': 'Сервис 3', 'en': 'Service 3'},
                    'slug': 'slug3',
                    'owner': data.staff.login,
                    'parent': data.metaservice.pk,
                    'description': {'ru': 'Описание сервиса', 'en': 'Service description'},
                }
            )

    assert response.status_code == 201
    service = Service.objects.get(slug='slug3')
    assert service.parent == data.metaservice


@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4', 'api-frontend'))
def test_create_service_in_different_hierarchy(api, client, data, owner_role):
    def fake_add_service_head(service, owner, requester):
        factories.ServiceMemberFactory(service=service, staff=owner, role=owner_role)

    client.login(data.stranger.login)

    with patch('plan.api.idm.actions.add_service') as add_service, \
            patch('plan.api.idm.actions.add_service_head') as add_service_head, \
            patch('plan.api.idm.actions.move_service') as move_service:
        add_service.return_value = None
        add_service_head.side_effect = fake_add_service_head
        move_service.return_value = None

        response = client.json.post(
            reverse(f'{api}:service-list'),
            {
                'name': {'ru': 'ru4', 'en': 'en4'},
                'slug': 'slug_4',
                'owner': data.stranger.login,
                'parent': data.metaservice.pk,
                'description': {'ru': 'Описание сервиса', 'en': 'Service description'},
            }
        )

    assert response.status_code == 201
    service = Service.objects.get(slug='slug_4')
    assert service.parent == data.meta_other
    move_request = service.move_requests.get()
    assert move_request.approver_outgoing == data.stranger
    assert move_request.approver_incoming is None


@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4', 'api-frontend'))
def test_cannot_create_service_same_name(api, client, data):
    client.login(data.staff.login)

    response = client.json.post(
        reverse(f'{api}:service-list'),
        {
            'name': {
                'ru': data.metaservice.name,
                'en': 'xxx',
            },
            'slug': 'xxx',
            'owner': data.staff.login,
            'description': {'ru': 'Описание сервиса', 'en': 'Service description'},
        }
    )
    assert response.status_code == 400
    assert response.json()['error']['extra']['name'] == ['Service name should be unique']


@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4', 'api-frontend'))
def test_cannot_create_service_same_name_en(api, client, data):
    client.login(data.staff.login)

    response = client.json.post(
        reverse(f'{api}:service-list'),
        {
            'name': {
                'ru': 'xxx',
                'en': data.metaservice.name_en,
            },
            'slug': 'xxx',
            'owner': data.staff.login,
            'description': {'ru': 'Описание сервиса', 'en': 'Service description'},
        }
    )
    assert response.status_code == 400
    assert response.json()['error']['extra']['name'] == ['English Service name should be unique']


@pytest.mark.parametrize('name_data, error_message', (
    ({'ru': '..'}, 'Service name must contain at least one letter'),
    ({'ru': 'а'}, 'Service name must have at least two characters long'),
    ({'en': '##'}, 'Service name must contain at least one letter'),
    ({'en': 'a'}, 'Service name must have at least two characters long'),
))
@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4', 'api-frontend'))
def test_cannot_create_service_invalid_name(api, client, data, name_data: Dict[str, Any], error_message: str):
    client.login(data.staff.login)

    response = client.json.post(
        reverse(f'{api}:service-list'),
        {
            'name': {
                'ru': 'xxx',
                'en': 'xxx',
                **name_data,
            },
            'slug': 'xxx',
            'owner': data.staff.login,
            'description': {'ru': 'Описание сервиса', 'en': 'Service description'},
        }
    )
    assert response.status_code == 400
    assert response.json()['error']['message']['en'] == error_message


@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4', 'api-frontend'))
def test_create_service_valid_slug(api, client, data):
    client.login(data.staff.login)

    for slug in ('h1234', 'hello', 'h3h3', 'h3-h3', 'h3_h3', 'h3-h3_he', '1hello', 'hello123'):
        with patch('plan.services.tasks.register_service'):
            response = client.json.post(
                reverse(f'{api}:service-list'),
                {
                    'name': {
                        'ru': 'Имя сервиса {}'.format(slug),
                        'en': 'Service name {}'.format(slug),
                    },
                    'slug': slug,
                    'owner': data.staff.login,
                    'description': {'ru': 'Описание сервиса', 'en': 'Service description'},
                }
            )
        assert 'id' in response.json()
        assert response.status_code == 201


@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4', 'api-frontend'))
@pytest.mark.parametrize('slug', ('1234', 'a'*51, 'smth_support', 'test_unique_slug'))
def test_cannot_create_service_invalid_slug(api, client, data, slug):
    """
    Проверяем, что нельзя создать сервис с невалидным слагом

    1234 - нельзя так как только цифры
    'a'*51 - слишком длинный
    smth_support - заканчивается на _support, где support это существующий scope
    test_unique_slug - сервис с таким слагом уже есть
    """
    client.login(data.staff.login)
    factories.ServiceFactory(
        owner=data.staff,
        slug='test_unique_slug'
    )
    factories.RoleScopeFactory(slug='support')

    response = client.json.post(
        reverse(f'{api}:service-list'),
        {
            'name': {
                'ru': 'Имя сервиса {}'.format(slug),
                'en': 'Service name {}'.format(slug),
            },
            'slug': slug,
            'owner': data.staff.login,
            'description': {'ru': 'Описание сервиса', 'en': 'Service description'},
        }
    )
    assert response.status_code == 400


@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4', 'api-frontend'))
def test_create_and_move_service(api, client, data):
    client.login(data.staff.login)

    with patch('plan.services.tasks.register_service') as register_service:
        response = client.json.post(
            reverse(f'{api}:service-list'),
            {
                'name': {'ru': 'Сервис 3', 'en': 'Service 3'},
                'slug': 'slug3',
                'owner': data.staff.login,
                'parent': data.other_service.pk,
                'description': {'ru': 'Описание сервиса', 'en': 'Service description'},
            }
        )

    assert response.status_code == 201
    assert register_service.apply_async.called

    service = Service.objects.get(slug='slug3')
    service.fetch_owner()
    assert service.name == 'Сервис 3'
    assert service.name_en == 'Service 3'
    assert service.state == Service.states.IN_DEVELOP
    assert service.owner == data.staff

    create_request = ServiceCreateRequest.objects.get(service=service)
    assert create_request.move_to == data.other_service


def test_edit_service(client, service_with_owner):
    service = service_with_owner
    client.login(service.owner.login)

    with patch('plan.services.tasks.notify_staff') as notify_staff:
        response = client.json.patch(
            reverse('services-api:service-detail', args=[service.pk]),
            {
                'url': 'xxx',
                'state': Service.states.SUPPORTED,
                'description': {
                    'ru': 'Описание',
                    'en': 'Description',
                },
                'activity': {
                    'ru': 'Деятельность',
                    'en': 'Activity',
                },
            }
        )

    assert response.status_code == 200

    notify_staff.delay.assert_called_once_with(service.pk)

    service.refresh_from_db()
    assert service.url == 'xxx'
    assert service.description == 'Описание'
    assert service.description_en == 'Description'
    assert service.activity == 'Деятельность'
    assert service.activity_en == 'Activity'
    assert service.state == Service.states.SUPPORTED


def test_clean_description(client, service_with_owner):
    service = service_with_owner
    service.description = 'xxx'
    service.description_en = 'xxx'
    service.activity = 'xxx'
    service.save()
    client.login(service.owner.login)

    response = client.json.patch(
        reverse('services-api:service-detail', args=[service.pk]),
        {
            'description': {
                'ru': '',
                'en': '',
            },
        }
    )

    assert response.status_code == 200

    service.refresh_from_db()
    assert service.description == ''
    assert service.description_en == ''
    assert service.activity == 'xxx'


def test_cannot_edit_readonly_service(client, data):
    client.login(data.staff.login)

    data.service.readonly_state = Service.MOVING
    data.service.save()

    response = client.json.patch(
        reverse('services-api:service-detail', args=[data.service.pk]),
        {'state': Service.states.NEEDINFO}
    )

    assert response.status_code == 409

    data.service.refresh_from_db()
    assert data.service.state == Service.states.IN_DEVELOP


@pytest.mark.parametrize('target_state', [Service.states.NEEDINFO, Service.states.CLOSED, Service.states.DELETED])
@patch('plan.api.idm.actions.delete_service')
def test_change_state(delete_service, client, data, target_state):
    now = timezone.now().date()
    service = data.service
    service.suspicious_date = now
    service.suspicious_notification_date = now
    suspicious_reason = factories.ServiceSuspiciousReasonFactory(service=service)
    service.save()
    factories.ServiceScopeFactory(service=service)
    client.login(data.staff.login)
    assert service.state != target_state
    with patch('plan.services.tasks.drop_requests'):
        with patch('plan.services.models.get_unclosable_services') as changing_patch:
            changing_patch.return_value = []
            response = client.json.patch(
                reverse('services-api:service-detail', args=[service.pk]),
                {
                    'state': target_state,
                }
            )
    assert response.status_code == 200

    service.refresh_from_db()
    assert service.state == target_state

    if target_state not in Service.states.ACTIVE_STATES:
        assert service.suspicious_date is None
        assert service.suspicious_notification_date is None
        assert ServiceSuspiciousReason.objects.count() == 0
        if target_state == Service.states.DELETED:
            assert service.staff_id is None
            for scope in ServiceScope.objects.filter(service=service):
                assert scope.staff_id is None

    else:
        assert service.suspicious_date == now
        assert service.suspicious_notification_date == now
        suspicious_reason.refresh_from_db()


def test_service_close_request(client, data):
    now = timezone.now().date()
    service = data.service
    service.suspicious_date = now
    service.suspicious_notification_date = now
    service.membership_inheritance = True
    factories.ServiceSuspiciousReasonFactory(service=service)
    service.save()
    factories.ServiceScopeFactory(service=service)
    client.login(data.staff.login)
    assert service.state != Service.states.CLOSED
    assert service.readonly_state is None
    assert ServiceSuspiciousReason.objects.count() == 1

    with patch('plan.services.tasks.close_service') as patched_close:
        with patch('plan.services.models.get_unclosable_services') as close_patcher:
            close_patcher.return_value = []
            response = client.json.patch(
                reverse('services-api:service-detail', args=[service.pk]),
                {
                    'state': Service.states.CLOSED,
                }
            )
        patched_close.apply_async.assert_called_once()

    assert response.status_code == 200

    service.refresh_from_db()
    assert service.readonly_state == Service.CLOSING
    assert service.state == Service.states.IN_DEVELOP
    assert service.suspicious_date is not None
    assert service.suspicious_notification_date is not None
    assert ServiceSuspiciousReason.objects.count() == 0

    request = ServiceCloseRequest.objects.get(service=service)

    with patch('plan.services.models.get_unclosable_services') as close_patcher:
        close_patcher.return_value = []
        close_service(request.id)

    service.refresh_from_db()
    assert service.readonly_state is None
    assert service.state == Service.states.CLOSED
    assert service.suspicious_date is None
    assert service.suspicious_notification_date is None
    assert ServiceSuspiciousReason.objects.count() == 0
    assert service.membership_inheritance is False


def test_change_state_does_not_affect_children(client, data):
    client.login(data.big_boss.login)

    response = client.json.patch(
        reverse('services-api:service-detail', args=[data.metaservice.pk]),
        {'state': Service.states.NEEDINFO}
    )

    assert response.status_code == 200

    data.metaservice.refresh_from_db()
    assert data.metaservice.state == Service.states.NEEDINFO
    for service in data.metaservice.get_descendants():
        assert service.state == Service.states.IN_DEVELOP


@patch('plan.services.tasks.delete_service')
def test_delete_service(delete_service, client, data):
    client.login(data.big_boss.login)
    with patch('plan.services.tasks.drop_requests'):
        with patch('plan.services.models.get_unclosable_services') as close_patcher:
            close_patcher.return_value = []
            response = client.json.patch(
                reverse('services-api:service-detail', args=[data.metaservice.pk]),
                {'state': Service.states.DELETED}
            )

    assert response.status_code == 200
    assert delete_service.apply_async.called

    for service in data.metaservice.get_descendants(include_self=True):
        assert service.readonly_state == Service.DELETING
        assert service.state == Service.states.IN_DEVELOP


@patch('plan.services.tasks.delete_service')
@pytest.mark.parametrize('is_superuser', [True, False])
def test_delete_base_service(delete_service, client, data, is_superuser):
    factories.ServiceTagFactory(slug=settings.BASE_TAG_SLUG)
    data.big_boss.user.is_superuser = is_superuser
    data.big_boss.user.save()
    data.metaservice.is_base = True
    data.metaservice.save()
    client.login(data.big_boss.login)
    with patch('plan.services.tasks.drop_requests'):
        with patch('plan.services.models.get_unclosable_services') as close_patcher:
            close_patcher.return_value = []
            response = client.json.patch(
                reverse('services-api:service-detail', args=[data.metaservice.pk]),
                {'state': Service.states.DELETED}
            )
    if is_superuser:
        assert response.status_code == 200
        assert delete_service.apply_async.called

        for service in data.metaservice.get_descendants(include_self=True):
            assert service.readonly_state == Service.DELETING
            assert service.state == Service.states.IN_DEVELOP
    else:
        assert response.status_code == 403
        assert response.json()['error']['code'] == 'Cannot delete base service'

        for service in data.metaservice.get_descendants(include_self=True):
            assert service.readonly_state is None
            assert service.state == Service.states.IN_DEVELOP


@pytest.mark.parametrize('oebs_agreement', ['child', 'service', 'metaservice'], indirect=True)
@pytest.mark.parametrize('state', [Service.states.DELETED, Service.states.CLOSED])
@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4', 'api-frontend'))
def test_delete_or_close_oebs_service_fails(client, data, api, state, oebs_agreement):
    """Запрещать удалять OEBS сервис, если у его предка, потомка или его самого есть активное OEBSAgreement"""
    assert data.service.state == Service.states.IN_DEVELOP

    client.login(data.owner_of_service.staff.login)
    response = client.json.patch(
        reverse(f'{api}:service-detail', args=[data.service.pk]),
        {'state': Service.states.DELETED}
    )

    assert response.status_code == 400
    assert (
        response.json()['error']['title']['en'] ==
        'Action is impossible because active agreement already exists.'
    )
    data.service.refresh_from_db()
    oebs_agreement.service.refresh_from_db()
    assert data.service.state == Service.states.IN_DEVELOP
    assert not data.service.oebs_agreements.exists() or data.service == oebs_agreement.service
    assert oebs_agreement.service.oebs_agreements.active().count() == 1


@pytest.mark.parametrize('state', [Service.states.DELETED, Service.states.CLOSED])
@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4', 'api-frontend'))
def test_delete_or_close_service_fails_if_child_has_active_oebs_agreement(client, data, api, state):
    """Запрещать удалять сервис, если у его потомка или его самого есть активное OEBSAgreement."""
    factories.OEBSAgreementFactory(service=data.child, action=ACTIONS.CHANGE_FLAGS)
    assert data.service.state == Service.states.IN_DEVELOP

    client.login(data.owner_of_service.staff.login)
    response = client.json.patch(
        reverse(f'{api}:service-detail', args=[data.service.pk]),
        {'state': Service.states.DELETED}
    )

    assert response.status_code == 400
    assert (
        response.json()['error']['title']['en'] ==
        'Action is impossible because active agreement already exists.'
    )
    data.service.refresh_from_db()
    data.child.refresh_from_db()
    assert data.service.state == Service.states.IN_DEVELOP
    assert not data.service.oebs_agreements.exists()
    assert data.child.oebs_agreements.count() == 1


@pytest.mark.skip
@pytest.mark.parametrize('state', [Service.states.DELETED, Service.states.CLOSED])
@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4', 'api-frontend'))
def test_delete_or_close_service_fails_if_d_wont_close(client, data, api, state):
    assert data.service.state == Service.states.IN_DEVELOP

    client.login(data.owner_of_service.staff.login)
    with patch('plan.services.models.get_unclosable_services') as close_patcher:
        close_patcher.return_value = [data.service.id]
        response = client.json.patch(
            reverse(f'{api}:service-detail', args=[data.service.pk]),
            {'state': state}
        )

    assert response.status_code == 409
    assert response.json()['error']['code'] == 'cannot_close_service_with_issued_quotas'
    data.service.refresh_from_db()
    assert data.service.state == Service.states.IN_DEVELOP


@patch('plan.services.tasks.drop_requests')
def test_closing_service_closes_children(drop_requests, client, data):
    client.login(data.big_boss.login)

    with patch('plan.services.models.get_unclosable_services') as changing_patch:
        changing_patch.return_value = []

        response = client.json.patch(
            reverse('services-api:service-detail', args=[data.metaservice.pk]),
            {'state': Service.states.CLOSED}
        )

    assert response.status_code == 200
    assert drop_requests.delay.called

    for service in data.metaservice.get_descendants(include_self=True):
        assert service.state == Service.states.CLOSED


@patch('plan.services.tasks.drop_requests')
@pytest.mark.parametrize('is_superuser', [False, True])
def test_closing_base_service(drop_requests, client, data, is_superuser):
    factories.ServiceTagFactory(slug=settings.BASE_TAG_SLUG)
    data.metaservice.is_base = True
    data.big_boss.user.is_superuser = is_superuser
    data.metaservice.save()
    data.big_boss.user.save()

    client.login(data.big_boss.login)

    with patch('plan.services.models.get_unclosable_services') as changing_patch:
        changing_patch.return_value = []
        response = client.json.patch(
            reverse('services-api:service-detail', args=[data.metaservice.pk]),
            {'state': Service.states.CLOSED}
        )

    if is_superuser:
        assert response.status_code == 200
        assert drop_requests.delay.called

        for service in data.metaservice.get_descendants(include_self=True):
            assert service.state == Service.states.CLOSED
    else:
        assert response.status_code == 403
        assert response.json()['error']['code'] == 'Cannot close base service'


def test_closing_unclosable_on_dispenser_service(client, data):
    data.big_boss.user.is_superuser = True
    data.big_boss.user.save()

    client.login(data.big_boss.login)

    with patch('plan.services.models.get_unclosable_services') as changing_patch:
        changing_patch.return_value = []
        response = client.json.patch(
            reverse('services-api:service-detail', args=[data.metaservice.pk]),
            {'state': Service.states.CLOSED}
        )

    assert response.status_code == 200
    data = response.json()
    assert data['state'] != Service.states.CLOSED


def test_change_state_permissions(client, data):
    request = partial(
        client.json.patch,
        reverse('services-api:service-detail', args=[data.metaservice.pk]),
        {'state': 'needinfo'}
    )

    client.login(data.staff.login)
    response = request()
    assert response.status_code == 200

    data.metaservice.refresh_from_db()
    assert data.metaservice.state == 'needinfo'


def test_change_state_cannot_restore(client, data):
    data.service.state = Service.states.DELETED
    data.service.save()

    client.login(data.staff.login)
    response = client.json.patch(
        reverse('services-api:service-detail', args=[data.service.pk]),
        {'state': 'needinfo'}
    )

    assert response.status_code == 400
    assert response.json()['error']['message']['ru'] == (
        'Восстановление сервиса из удаленных невозможно.'
    )


def test_change_state_ancestors_must_be_active(client, data):
    data.metaservice.state = data.service.state = Service.states.CLOSED
    data.metaservice.save()
    data.service.save()

    client.login(data.staff.login)
    response = client.json.patch(
        reverse('services-api:service-detail', args=[data.service.pk]),
        {'state': 'needinfo'}
    )
    assert response.status_code == 400
    assert response.json()['error']['message']['ru'] == (
        'Нельзя изменить статус сервиса на активный, пока один из его родителей в неактивном статусе.'
    )


def test_edit_name(client, data):
    client.login(data.other_staff.login)

    with patch('plan.api.idm.actions.rename_service') as rename_service:
        response = client.json.patch(
            reverse('services-api:service-detail', args=[data.service.pk]),
            {'name': {'ru': 'Новое название', 'en': 'New name'}}
        )

    assert response.status_code == 200
    assert rename_service.call_count == 1

    data.service.refresh_from_db()
    assert data.service.name == 'Новое название'
    assert data.service.name_en == 'New name'


def test_edit_name_oebs_sync(client, data):
    client.login(data.other_staff.login)
    new_name = {'ru': 'Новое название', 'en': 'New name'}

    resource_type = factories.ResourceTypeFactory(code=settings.OEBS_PRODUCT_RESOURCE_TYPE_CODE)
    resource = factories.ResourceFactory(type=resource_type)
    factories.ServiceResourceFactory(
        service=data.service,
        resource=resource,
        state='granted',
    )

    response = client.json.patch(
        reverse('services-api:service-detail', args=[data.service.pk]),
        {'name': new_name}
    )

    assert response.status_code == 200
    data.service.refresh_from_db()
    assert data.service.name != new_name['ru']
    assert data.service.name_en != new_name['en']
    agreement = data.service.oebs_agreements.get()
    assert agreement.action == ACTIONS.RENAME
    assert agreement.state == STATES.VALIDATED_IN_OEBS
    assert agreement.attributes == {
        'new_ru': new_name['ru'],
        'new_en': new_name['en'],
        'ru': data.service.name,
        'en': data.service.name_en,
    }
    agreement.state = STATES.APPLIED_IN_OEBS
    agreement.save()
    with patch('plan.services.tasks.rename_service') as mock_rename_task:
        with patch('plan.oebs.utils.update_issue') as mock_update_issue:
            finish_oebs_approve_process(
                agreement_id=agreement.id,
                leaf_oebs_id='lead_id',
                parent_oebs_id='group_id',
            )
    mock_rename_task.apply_async.assert_called_once()
    mock_update_issue.assert_called_once_with(assignee='dubilt', key=None)

    data.service.refresh_from_db()
    assert data.service.name == new_name['ru']
    assert data.service.name_en == new_name['en']
    agreement.refresh_from_db()
    assert agreement.state == STATES.APPLIED


def test_edit_name_same_name(client, data):
    client.login(data.staff.login)

    with patch('plan.services.tasks.rename_service') as rename_service:
        response = client.json.patch(
            reverse('services-api:service-detail', args=[data.service.pk]),
            {'name': {'ru': 'Новое название', 'en': data.service.name_en}}
        )

    assert response.status_code == 200
    assert rename_service.apply_async.call_count == 1

    data.service.refresh_from_db()
    assert data.service.name == 'Новое название'


@pytest.mark.parametrize('name_data, error_message', (
    ({'ru': '..'}, 'Service name must contain at least one letter'),
    ({'ru': 'а'}, 'Service name must have at least two characters long'),
    ({'en': '##'}, 'Service name must contain at least one letter'),
    ({'en': 'a'}, 'Service name must have at least two characters long'),
))
def test_edit_invalid_name(client, data, name_data: Dict[str, str], error_message: str):
    client.login(data.staff.login)
    old_name = data.service.name

    with patch('plan.services.tasks.rename_service') as rename_service:
        response = client.json.patch(
            reverse('services-api:service-detail', args=[data.service.pk]),
            {'name': {'ru': 'новоеназвание', 'en': 'new-name', **name_data}}
        )

    assert response.status_code == 400
    assert response.json()['error']['message']['en'] == error_message
    rename_service.apply_async.assert_not_called()

    data.service.refresh_from_db()
    assert data.service.name == old_name


def test_edit_duplicate_name(client, data):
    client.login(data.staff.login)
    old_name = data.service.name

    response = client.json.patch(
        reverse('services-api:service-detail', args=[data.service.pk]),
        {'name': {'ru': data.other_service.name, 'en': 'New name'}}
    )

    assert response.status_code == 400
    assert response.json()['error']['message']['ru'] == (
        'Значения ключей "ru" поля name должны быть уникальными.'
    )

    data.service.refresh_from_db()
    assert data.service.name_en == old_name


def test_edit_duplicate_name_en(client, data):
    client.login(data.staff.login)
    old_name = data.service.name_en

    response = client.json.patch(
        reverse('services-api:service-detail', args=[data.service.pk]),
        {'name': {'ru': 'Новое имя', 'en': data.other_service.name_en}}
    )

    assert response.status_code == 400
    assert response.json()['error']['message']['ru'] == (
        'Значения ключей "en" поля name должны быть уникальными.'
    )

    data.service.refresh_from_db()
    assert data.service.name_en == old_name


@pytest.mark.parametrize('with_permissions', [False, True])
def test_edit_tags(client, data, with_permissions):
    client.login(data.staff.login)

    tag1 = factories.ServiceTagFactory()
    tag2 = factories.ServiceTagFactory()
    if with_permissions:
        for tag in (tag1, tag2):
            data.staff.user.user_permissions.add(Permission.objects.get(codename=f'change_service_tag_{tag.slug}'))

    request = partial(
        client.json.patch,
        reverse('services-api:service-detail', args=[data.service.pk])
    )

    # ставим один
    response = request({'tags': [tag1.id]})
    assert response.status_code == 200

    data.service.refresh_from_db()
    assert data.service.tags.count() == 1
    assert data.service.tags.get().id == tag1.id

    # ставим два
    response = request({'tags': [tag1.id, tag2.id]})
    assert response.status_code == 200

    data.service.refresh_from_db()
    assert data.service.tags.count() == 2
    assert set(data.service.tags.values_list('id', flat=True)) == set((tag1.id, tag2.id))

    # ставим обратно один
    response = request({'tags': [tag2.id]})
    assert response.status_code == 200

    data.service.refresh_from_db()
    assert data.service.tags.count() == 1
    assert data.service.tags.get().id == tag2.id


def test_edit_tags_forbidden(client, data, person):
    """
    Если на тег сервиса кому-либо выданы разрешения,
    то для его изменения нужно иметь разрешение на редактирование этого тега.
    """
    tag = factories.ServiceTagFactory()
    person.user.user_permissions.add(Permission.objects.get(codename=f'change_service_tag_{tag.slug}'))

    client.login(data.staff.login)
    response = client.json.patch(
        reverse('services-api:service-detail', args=[data.service.pk]),
        {'tags': [tag.id]},
    )
    assert response.status_code == 403


def test_move_permissions(client, data):
    service = factories.ServiceFactory()
    request = partial(
        client.json.options,
        reverse('services-api:service-detail', args=[service.id]),
        {'url': 'derp'}
    )

    client.login(data.stranger.login)
    response = request()
    assert response.status_code == 200
    assert 'can_request_move' in response.json()['permissions']

    service.readonly_state = Service.MOVING
    service.save()

    response = request()
    assert response.status_code == 200
    assert 'can_request_move' not in response.json()['permissions']


def test_edit_permissions(client, data):
    request = partial(
        client.json.patch,
        reverse('services-api:service-detail', args=[data.service.pk]),
        {'url': 'derp'}
    )

    client.login(data.stranger.login)
    response = request()
    assert response.status_code == 200


def test_tag_permissions(client, data):
    request = partial(
        client.json.options,
        reverse('services-api:service-detail', args=(data.service.id,))
    )

    response = request()
    assert response.status_code == 200
    assert 'can_edit_tags' not in response.json()['permissions']

    client.login(data.staff.login)
    response = request()
    assert response.status_code == 200
    assert 'can_edit_tags' in response.json()['permissions']


def test_list_stranger_permissions(client, data):
    client.login(data.stranger.login)

    response = client.json.options(reverse('services-api:service-list'))
    assert response.status_code == 200

    assert set(response.json()['permissions']) == {
        'can_edit_description',
        'can_edit_activity',
    }


def test_list_support_permissions(client, data):
    client.login(data.support.login)

    response = client.json.options(
        reverse('services-api:service-detail', args=[data.service.id])
    )
    assert response.status_code == 200

    assert set(response.json()['permissions']) == {
        'can_edit_description',
        'can_edit_activity',
        'can_manage_responsible',
        'can_edit_contacts',
        'can_request_move',
    }


def test_list_responsible_permissions(client, data):
    client.login(data.responsible.staff.login)

    response = client.json.options(
        reverse('services-api:service-detail', args=[data.service.id])
    )
    assert response.status_code == 200

    assert set(response.json()['permissions']) == {
        'can_approve_shifts',
        'can_edit_description',
        'can_edit_activity',
        'can_edit_duty_settings',
        'can_edit_tags',
        'can_edit_contacts',
        'can_manage_responsible',
        'can_request_move',
        'can_fix_issues',
        'can_edit_oebs_flags',
        'can_edit_membership_inheritance',
        'is_service_member',
    }


def test_list_responsible_for_duty_permissions(client, data, staff_factory):
    role = factories.RoleFactory(code=Role.RESPONSIBLE_FOR_DUTY)
    staff = factories.ServiceMemberFactory(service=data.service, role=role, staff=staff_factory()).staff
    client.login(staff.login)

    response = client.json.options(
        reverse('services-api:service-detail', args=[data.service.id])
    )
    assert response.status_code == 200

    assert set(response.json()['permissions']) == {
        'can_approve_shifts',
        'can_edit_activity',
        'can_edit_contacts',
        'can_edit_description',
        'can_edit_duty_settings',
        'can_edit_tags',
        'can_request_move',
        'is_service_member',
    }


def test_list_member_permissions(client, data):
    client.login(data.team_member.staff.login)

    response = client.json.options(
        reverse('services-api:service-detail', args=[data.service.id])
    )
    assert response.status_code == 200

    assert set(response.json()['permissions']) == {
        'can_edit_description',
        'can_edit_activity',
        'can_edit_tags',
        'can_edit_contacts',
        'can_request_move',
        'is_service_member'
    }


def test_ordering_by_id(client):
    factories.ServiceFactory(id=100, name='name')
    factories.ServiceFactory(id=99, name='name')
    factories.ServiceFactory(id=98, name='name')

    response = client.json.get(
        reverse('services-api:service-list'),
        {
            'name': 'name',
        }
    )
    results = response.json()['results']
    assert [s['id'] for s in results] == [98, 99, 100]

    response = client.json.get(
        reverse('services-api:service-list'),
        {
            'ordering': '-id',
        }
    )
    results = response.json()['results']
    assert [s['id'] for s in results] == [100, 99, 98]


def test_ordering_by_kpi(client):
    s1 = factories.ServiceFactory(kpi_bugs_count=100)
    s2 = factories.ServiceFactory(kpi_bugs_count=98)
    s3 = factories.ServiceFactory(kpi_bugs_count=99)

    response = client.json.get(
        reverse('services-api:service-list'),
        {
            'ordering': 'kpi_bugs_count',
        }
    )
    results = response.json()['results']
    assert [s['id'] for s in results] == [s2.pk, s3.pk, s1.pk]


def test_close_with_important_resources(client, service_with_owner, important_resource):
    factories.ServiceResourceFactory(service=service_with_owner, resource=important_resource)

    client.login(service_with_owner.owner.login)
    for state in Service.states.ACTIVE_STATES:
        response = client.json.patch(
            reverse('services-api:service-detail', args=(service_with_owner.id,)),
            {'state': state}
        )
        assert response.status_code == 200

    for state in (Service.states.DELETED, Service.states.CLOSED):
        response = client.json.patch(
            reverse('services-api:service-detail', args=(service_with_owner.id,)),
            {'state': state}
        )

        assert response.status_code == 400
        error = response.json()['error']
        assert error['detail'] == 'Sent data is wrong.'
        assert error['message']['ru'] == (
            'Этому сервису или его детям выданы важные ресурсы, для удаления/закрытия их нужно отозвать.'
        )

        service_with_owner.refresh_from_db()
        assert service_with_owner.state in Service.states.ACTIVE_STATES


def test_close_with_important_resources_on_children(client, service_with_owner, important_resource):
    child = factories.ServiceFactory(parent=service_with_owner)
    factories.ServiceResourceFactory(service=child, resource=important_resource)

    client.login(service_with_owner.owner.login)
    for state in Service.states.ACTIVE_STATES:
        response = client.json.patch(
            reverse('services-api:service-detail', args=(service_with_owner.id,)),
            {'state': state}
        )
        assert response.status_code == 200

    for state in (Service.states.DELETED, Service.states.CLOSED):
        response = client.json.patch(
            reverse('services-api:service-detail', args=(service_with_owner.id,)),
            {'state': state}
        )

        assert response.status_code == 400
        error = response.json()['error']
        assert error['detail'] == 'Sent data is wrong.'
        assert error['message']['ru'] == (
            'Этому сервису или его детям выданы важные ресурсы, для удаления/закрытия их нужно отозвать.'
        )

        for service in (service_with_owner, child):
            service.refresh_from_db()
            assert service.state in Service.states.ACTIVE_STATES


def test_get_service_with_filters(client, data):
    response = client.json.get(
        reverse('services-api:service-detail', args=[data.metaservice.id]),
        {'department': data.staff.department.id}
    )
    assert response.status_code == 404

    response = client.json.get(
        reverse('services-api:service-detail', args=[data.service.id]),
        {'department': data.staff.department.id}
    )
    assert response.status_code == 200


def test_ancestors_serialization(client, data):
    update_service_fields(data.service.id)
    response = client.json.get(
        reverse('services-api:service-detail', args=[data.service.id])
    )
    assert response.status_code == 200
    ancestors = response.json()['ancestors']
    assert isinstance(ancestors, list)
    assert len(ancestors) == 1
    assert isinstance(ancestors[0], dict)


def test_ancestors_is_readonly(client, data):
    # на момент написания теста ancestors нет в update сериализаторе,
    # тест на случай, если по какой-то причине поле вдруг начнет писаться
    update_service_fields(data.service.id)
    client.login(data.moderator.login)
    response = client.json.patch(
        reverse('services-api:service-detail', args=[data.service.id]),
        {'ancestors': []}
    )
    assert response.status_code == 200

    data.service.refresh_from_db()
    assert data.service.ancestors


def test_new_parent_suggest(client, data, owner_role):
    department = factories.DepartmentFactory()
    potential_parent = factories.ServiceFactory(parent=None)
    potential_parent.departments.add(department)

    service = factories.ServiceFactory(parent=data.meta_other)
    staff = factories.StaffFactory(department=department)
    factories.ServiceMemberFactory(staff=staff, service=service, role=owner_role)

    response = client.json.get(
        reverse('services-api:service-suggest-parent', args=[service.id])
    )
    assert response.status_code == 200

    result = response.json()
    assert len(result) == 1
    assert result[0]['id'] == potential_parent.id


def test_limit_suggest(client, data, owner_role):
    department = factories.DepartmentFactory()

    for _ in range(10):
        parent = factories.ServiceFactory(parent=None)
        parent.departments.add(department)

    service = factories.ServiceFactory(parent=data.meta_other)
    staff = factories.StaffFactory(department=department)
    factories.ServiceMemberFactory(staff=staff, service=service, role=owner_role)

    response = client.json.get(
        reverse('services-api:service-suggest-parent', args=[service.id])
    )
    assert response.status_code == 200

    result = response.json()
    assert len(result) == 5


def test_new_parent_suggest_multiple_potential_parents(client, data, owner_role):
    department = factories.DepartmentFactory()
    potential_parent = factories.ServiceFactory(parent=None)
    potential_parent.departments.add(department)

    child_department = factories.DepartmentFactory(parent=department)
    other_potential_parent = factories.ServiceFactory(parent=data.metaservice)
    other_potential_parent.departments.add(child_department)

    service = factories.ServiceFactory(parent=data.meta_other)
    staff = factories.StaffFactory(department=child_department)
    factories.ServiceMemberFactory(staff=staff, service=service, role=owner_role)

    response = client.json.get(
        reverse('services-api:service-suggest-parent', args=[service.id])
    )
    assert response.status_code == 200

    result = response.json()
    assert len(result) == 2
    assert {parent['id'] for parent in result} == {potential_parent.id, other_potential_parent.id}


def test_new_parent_suggest_service_not_in_other(client, data):
    response = client.json.get(
        reverse('services-api:service-suggest-parent', args=[data.service.id])
    )
    assert response.status_code == 200
    assert response.json() == []


def test_new_parent_suggest_head_has_no_department(client, data, owner_role):
    service = factories.ServiceFactory(parent=data.meta_other)
    staff = factories.StaffFactory(department=None)
    factories.ServiceMemberFactory(staff=staff, service=service, role=owner_role)

    response = client.json.get(
        reverse('services-api:service-suggest-parent', args=[service.id])
    )
    assert response.status_code == 200
    assert response.json() == []


def test_new_parent_suggest_two_heads(client, data, owner_role):
    department = factories.DepartmentFactory()
    potential_parent = factories.ServiceFactory(parent=None)
    potential_parent.departments.add(department)
    service = factories.ServiceFactory(parent=data.meta_other)
    staff = factories.StaffFactory(department=None)
    another_staff = factories.StaffFactory(department=department)
    factories.ServiceMemberFactory(staff=staff, service=service, role=owner_role)
    factories.ServiceMemberFactory(staff=another_staff, service=service, role=owner_role)

    response = client.json.get(
        reverse('services-api:service-suggest-parent', args=[service.id])
    )
    assert response.status_code == 200
    response_json = response.json()
    assert len(response_json) == 1
    assert response_json[0]['id'] == potential_parent.id


def test_auth(base_client):
    response = base_client.get(reverse('api-v4:service-list'))
    assert response.status_code == 403


def test_suspicious_reasons(client):
    owner = factories.StaffFactory()
    service = factories.ServiceFactory(owner=owner)
    reason1 = factories.ServiceSuspiciousReasonFactory(
        service=service, reason=ServiceSuspiciousReason.NO_ONWER, context={}
    )
    reason2 = factories.ServiceSuspiciousReasonFactory(
        service=service, reason=ServiceSuspiciousReason.WRONG_OWNER, context={'owner': 'login', 'parent_owner': 'abcd'}
    )
    reason3 = factories.ServiceSuspiciousReasonFactory(
        service=service, reason=ServiceSuspiciousReason.PARENT_IS_SUSPICIOUS, context={'parent': 123}
    )

    expected_result = [
        {
            'id': x.id,
            'created_at': datetime_isoformat_with_microseconds(x.created_at),
            'updated_at': datetime_isoformat_with_microseconds(x.updated_at),
            'reason': x.reason,
            'human_text': x.human_text
        }
        for x in (reason1, reason2, reason3)
    ]
    response = client.json.get(
        reverse('services-api:service-suspicious-reasons', args=[service.id])
    )
    assert iterables_are_equal(expected_result, response.json())

    expected_human_text = [
        {
            'ru': 'У сервиса нет руководителя',
            'en': 'Service has no owner'
        },
        {
            'ru': 'Руководитель сервиса (кто:login) не находится в одном департаменте с руководителем вышестоящего сервиса (кто:abcd)',
            'en': 'Service owner (кто:login) is not in the same department with the higher service owner (кто:abcd)'
        },
        {
            'ru': '(({base_url}/services/123/ Родительский сервис)) является подозрительным'.format(
                base_url=settings.ABC_URL),
            'en': '(({base_url}/services/123/ Parent service)) is suspicious'.format(base_url=settings.ABC_URL)
        },
    ]
    assert iterables_are_equal(expected_human_text, [x['human_text'] for x in response.json()])


def test_mark_as_not_suspicious_not_permitted(client):
    service = factories.ServiceFactory(suspicious_date=timezone.now().date())
    stranger = factories.StaffFactory()
    client.login(stranger.login)
    response = client.json.post(
        reverse('services-api:service-mark-as-not-suspicious', args=[service.id]), {'text': 'text'}
    )
    assert response.status_code == 403


def test_get_and_edit_related_services(client, data):
    client.login(data.service.owner.login)

    type_service = data.service_type
    type_team = factories.ServiceTypeFactory(
        name='Команда',
        name_en='Team',
        code='team'
    )

    data.service.service_type = type_service
    data.service.save()
    service_1 = data.service
    service_2 = factories.ServiceFactory(service_type=type_service)
    service_team_1 = factories.ServiceFactory(service_type=type_team)
    service_team_2 = factories.ServiceFactory(service_type=type_team)

    request_service_1 = partial(client.json.patch, reverse('services-api:service-detail', args=[service_1.id]))
    request_service_2 = partial(client.json.patch, reverse('services-api:service-detail', args=[service_2.id]))
    request_team_2 = partial(client.json.patch, reverse('services-api:service-detail', args=[service_team_2.id]))

    # проверяем, что изначально команды и сервисы не имеют связей
    assert service_1.related_services.count() == 0
    assert service_2.related_services.count() == 0
    assert service_team_1.related_services.count() == 0
    assert service_team_2.related_services.count() == 0

    # связываем первый сервис и первую команду
    response = request_service_1({'related_services': [service_team_1.id]})
    assert response.status_code == 200

    assert service_1.related_services.count() == 1
    assert service_team_1.related_services.count() == 1

    # добавляем сервису вторую команду
    response = request_service_1({'related_services': [service_team_1.id, service_team_2.id]})
    assert response.status_code == 200

    assert service_1.related_services.count() == 2
    assert service_team_1.related_services.count() == 1
    assert service_team_2.related_services.count() == 1

    # добавляем во второй сервис вторую команду
    response = request_service_2({'related_services': [service_team_2.id]})
    assert response.status_code == 200

    assert service_1.related_services.count() == 2
    assert service_2.related_services.count() == 1
    assert service_team_1.related_services.count() == 1
    assert service_team_2.related_services.count() == 2

    # проверим id связанных сервисов
    # у первого сервиса должен быть id обеих команд
    related_services_list = service_1.related_services.values_list('id', flat=True)
    assert set(related_services_list) == set([service_team_1.id, service_team_2.id])

    # у второго сервиса должен быть id второй команды
    assert service_2.related_services.get().id == service_team_2.id

    # у первой команды должен быть id первого сервиса
    assert service_team_1.related_services.get().id == service_1.id

    # у второй команды должен быть id обоих сервисов
    related_services_list = service_team_2.related_services.values_list('id', flat=True)
    assert set(related_services_list) == set((service_1.id, service_2.id))

    # отвязываем вторую команду от всех сервисов
    response = request_team_2({'related_services': []})
    assert response.status_code == 200
    assert service_1.related_services.count() == 1
    assert service_2.related_services.count() == 0
    assert service_team_1.related_services.count() == 1
    assert service_team_2.related_services.count() == 0

    # проверим id связанных сервисов
    assert service_1.related_services.get().id == service_team_1.id
    assert service_team_1.related_services.get().id == service_1.id


def test_edit_related_services_bad_request(client, data):
    service = data.service
    another_service = factories.ServiceFactory(service_type=service.service_type)
    response = client.json.patch(
        reverse('services-api:service-detail', args=[service.id]), {'related_services': [another_service.id]}
    )
    assert response.status_code == 400


def test_noissue_by_actions(client, data):
    issue = factories.IssueFactory()
    factories.ServiceIssueFactory(service=data.service, issue=issue)
    response = client.json.get(
        reverse('services-api:service-detail', args=[data.service.id])
    )
    assert response.status_code == 200
    issue = response.json()['issue']
    assert issue is None


def test_issue(client, data):
    issue = factories.IssueFactory()
    service_issue1 = factories.ServiceIssueFactory(service=data.service, issue=issue)
    factories.ServiceIssueFactory(service=data.service)

    execution1 = factories.ExecutionFactory(is_critical=True)

    action1 = factories.ServiceExecutionActionFactory(execution=execution1, service_issue=service_issue1)
    factories.ServiceExecutionActionFactory(execution=execution1, service_issue=service_issue1)
    factories.ServiceExecutionActionFactory(service_issue=service_issue1)

    response = client.json.get(
        reverse('services-api:service-detail', args=[data.service.id])
    )
    assert response.status_code == 200
    issue = response.json()['issue']
    assert issue == {
        'execution': {
            'is_critical': True,
            'name': {'ru': execution1.name, 'en': execution1.name_en},
            'should_be_applied_at': datetime_isoformat_with_microseconds(action1.should_be_applied_at),
            'applied_at': None,
        },
        'code': service_issue1.issue.code,
    }


def test_main_issue_is_group(client, data):
    issue_group = factories.IssueGroupFactory()
    service_issue = factories.ServiceIssueFactory(service=data.service, issue_group=issue_group)
    execution = factories.ExecutionFactory(is_critical=True)
    action = factories.ServiceExecutionActionFactory(execution=execution, service_issue=service_issue)

    response = client.json.get(
        reverse('services-api:service-detail', args=[data.service.id])
    )
    assert response.status_code == 200
    issue = response.json()['issue']
    assert issue == {
        'execution': {
            'is_critical': True,
            'name': {'ru': execution.name, 'en': execution.name_en},
            'should_be_applied_at': datetime_isoformat_with_microseconds(action.should_be_applied_at),
            'applied_at': None,
        },
        'code': issue_group.code,
    }


@pytest.fixture
def data_issuegroups(data):
    wrong_service = factories.ServiceFactory()
    group1 = factories.IssueGroupFactory()
    group2 = factories.IssueGroupFactory()
    issue_inactive = factories.IssueFactory(issue_group=group1)
    issue1 = factories.IssueFactory(issue_group=group1)
    issue2 = factories.IssueFactory(issue_group=group2)
    issue3 = factories.IssueFactory(issue_group=group2)
    issue4 = factories.IssueFactory(issue_group=group1)
    issue5 = factories.IssueFactory(issue_group=group1)
    issue6 = factories.IssueFactory(issue_group=group2)
    factories.ServiceIssueFactory(
        service=data.service,
        issue=issue_inactive,
        state=ServiceIssue.STATES.FIXED
    )
    service_issue1 = factories.ServiceIssueFactory(service=data.service, issue=issue1, state=ServiceIssue.STATES.REVIEW)
    service_issue2 = factories.ServiceIssueFactory(service=data.service, issue=issue2)
    service_issue3 = factories.ServiceIssueFactory(service=data.service, issue=issue3)
    service_issue4 = factories.ServiceIssueFactory(service=wrong_service, issue=issue4)
    service_issue5 = factories.ServiceIssueFactory(service=data.service, issue=issue5)
    service_issue6 = factories.ServiceIssueFactory(service=data.service, issue=issue6)
    service_issue_with_group = factories.ServiceIssueFactory(service=data.service, issue_group=group1)
    factories.ServiceExecutionActionFactory(service_issue=service_issue1)
    factories.ServiceExecutionActionFactory(service_issue=service_issue1)
    factories.ServiceExecutionActionFactory(
        service_issue=service_issue2,
        should_be_applied_at=timezone.now() + timezone.timedelta(days=5)
    )
    factories.ServiceExecutionActionFactory(service_issue=service_issue3)
    factories.ServiceExecutionActionFactory(
        service_issue=service_issue_with_group,
        should_be_applied_at=timezone.now() - timezone.timedelta(days=1),
        applied_at=timezone.now() - timezone.timedelta(days=1)
    )
    action_6 = factories.ServiceExecutionActionFactory(
        service_issue=service_issue_with_group,
        should_be_applied_at=timezone.now() + timezone.timedelta(days=5),
        applied_at=None
    )

    factories.IssueGroupThresholdFactory(issue_group=group1, threshold=0.7, level=LEVELS.CRIT)
    factories.IssueGroupThresholdFactory(issue_group=group1, threshold=0.3, level=LEVELS.WARN)

    factories.IssueGroupThresholdFactory(issue_group=group2, threshold=0.7, level=LEVELS.CRIT)
    factories.IssueGroupThresholdFactory(issue_group=group2, threshold=0.3, level=LEVELS.WARN)

    return stub(
        group1=group1,
        group2=group2,
        issue4=issue4,
        issue_inactive=issue_inactive,
        service_issue1=service_issue1,
        service_issue2=service_issue2,
        service_issue3=service_issue3,
        service_issue4=service_issue4,
        service_issue5=service_issue5,
        service_issue6=service_issue6,
        action_6=action_6,
    )


@pytest.mark.parametrize('invert_code_order', [True, False])
def test_issuegroups(client, data, data_issuegroups, invert_code_order):
    if invert_code_order:
        data_issuegroups.group1.code = 'code_b'
        data_issuegroups.group1.save()
        data_issuegroups.group2.code = 'code_a'
        data_issuegroups.group2.save()
    check_issue_groups()

    response = client.json.get(
        reverse('services-api:service-issuegroups', args=[data.service.id])
    )
    assert response.status_code == 200
    result = response.json()

    if invert_code_order:
        result.reverse()

    assert result[0]['issues_count'] == 2
    assert result[0]['name'] == {'en': data_issuegroups.group1.name_en, 'ru': data_issuegroups.group1.name}
    assert result[0]['code'] == data_issuegroups.group1.code
    assert result[0]['issues'][0]['context'] is None

    assert [
        issue['id'] if 'id' in issue else None for issue in result[0]['issues']
    ] == [data_issuegroups.service_issue1.id, data_issuegroups.service_issue5.id, None, None]

    assert [
        issue['code'] for issue in result[0]['issues']
    ] == [
        data_issuegroups.service_issue1.issue.code,
        data_issuegroups.service_issue5.issue.code,
        data_issuegroups.issue4.code,
        data_issuegroups.issue_inactive.code,
    ]

    assert result[1]['issues_count'] == 3
    assert result[1]['name'] == {'en': data_issuegroups.group2.name_en, 'ru': data_issuegroups.group2.name}
    assert result[1]['code'] == data_issuegroups.group2.code

    assert [
        issue['id'] if 'id' in issue else None for issue in result[1]['issues']
    ] == [
        data_issuegroups.service_issue2.id,
        data_issuegroups.service_issue3.id,
        data_issuegroups.service_issue6.id,
    ]

    assert [
        issue['code'] for issue in result[1]['issues']
    ] == [
        data_issuegroups.service_issue2.issue.code,
        data_issuegroups.service_issue3.issue.code,
        data_issuegroups.service_issue6.issue.code
    ]


def test_issuegroups_restricted(client, staff_factory, data, data_issuegroups):
    client.login(staff_factory('own_only_viewer').login)

    response = client.json.get(
        reverse('services-api:service-issuegroups', args=[data.service.id])
    )
    assert response.status_code == 403


def test_issuegroups_summary(client, data, data_issuegroups):
    check_issue_groups()
    response = client.json.get(
        reverse('services-api:service-issuegroups', args=[data.service.id])
    )
    assert response.status_code == 200
    result = response.json()

    assert result[0]['summary']['current_weight'] == 50
    assert len(result[0]['summary']['thresholds']) == 2

    thresholds = result[0]['summary']['thresholds']
    assert thresholds[0]['level'] == LEVELS.CRIT
    assert not thresholds[0]['is_current']
    assert thresholds[0]['weight'] == 70
    assert thresholds[0]['is_next']

    assert thresholds[1]['level'] == LEVELS.WARN
    assert thresholds[1]['is_current']
    assert thresholds[1]['weight'] == 30
    assert not thresholds[1]['is_next']

    assert result[1]['summary']['current_weight'] == 100
    assert len(result[1]['summary']['thresholds']) == 2

    thresholds = result[1]['summary']['thresholds']
    assert thresholds[0]['level'] == LEVELS.CRIT
    assert thresholds[0]['is_current']
    assert thresholds[0]['weight'] == 70
    assert not thresholds[0]['is_next']
    assert thresholds[0]['execution'] is None

    assert thresholds[1]['level'] == LEVELS.WARN
    assert not thresholds[1]['is_current']
    assert thresholds[1]['weight'] == 30
    assert not thresholds[1]['is_next']

    to_fix = [data_issuegroups.service_issue1, data_issuegroups.service_issue4, data_issuegroups.service_issue5]
    for service_issue in to_fix:
        service_issue.change_state(ServiceIssue.STATES.FIXED)
    check_issue_groups()
    response = client.json.get(
        reverse('services-api:service-issuegroups', args=[data.service.id])
    )
    assert response.status_code == 200
    result = response.json()

    assert result[0]['summary']['thresholds'][0]['level'] == LEVELS.CRIT
    assert not result[0]['summary']['thresholds'][0]['is_current']
    assert result[0]['summary']['thresholds'][0]['weight'] == 70
    assert not result[0]['summary']['thresholds'][0]['is_next']

    assert result[0]['summary']['thresholds'][1]['level'] == LEVELS.WARN
    assert not result[0]['summary']['thresholds'][1]['is_current']
    assert result[0]['summary']['thresholds'][1]['weight'] == 30
    assert result[0]['summary']['thresholds'][1]['is_next']


def test_issuegroups_num_queries(client, data, data_issuegroups, django_assert_num_queries):
    check_issue_groups()
    # 2 middleware
    # 1 for serviceexecutionaction + serviceissue + execution
    # 1 for serviceissue + issue
    # 1 for issuegroup

    # 1 for servicetrafficstatus
    # 1 for issuegroupthreshold + executionchain
    # 1 for serviceexecutionaction + execution
    # 1 for executionstep + execution
    # 1 for issue

    # 1 for servicetrafficstatus
    # 1 for issuegroupthreshold + executionchain
    # 1 for serviceexecutionaction + execution
    # 1 for issue

    # 1 pg_is_in_recovery()
    # 1 for waffle_switch

    with django_assert_num_queries(16):
        response = client.json.get(
            reverse('services-api:service-issuegroups', args=[data.service.id])
        )
    assert response.status_code == 200


def test_services_num_queries(client, django_assert_num_queries):
    def add_services_with_groups():
        factories.ServiceTrafficStatusFactory(
            service=factories.ServiceFactory(),
            issue_group=factories.IssueGroupFactory(),
        )

    for _ in range(100):
        add_services_with_groups()

    waffle_query = 1
    with django_assert_num_queries(9 + waffle_query):
        response = client.json.get(reverse('api-v3:service-list'))
        assert response.status_code == 200
        assert response.json()['count'] == 100

    for _ in range(100):
        add_services_with_groups()

    with django_assert_num_queries(9 + waffle_query):
        response = client.json.get(reverse('api-v3:service-list'))
        assert response.status_code == 200
        assert response.json()['count'] == 200


def test_issue_groups_order(client, data, data_issuegroups):
    check_issue_groups()
    response = client.json.get(
        reverse('services-api:service-issuegroups', args=[data.service.id])
    )
    codes_in_issuegroups = [item['code'] for item in response.json()]
    response = client.json.get(reverse('services-api:service-list'), {'id': data.service.id})
    codes_in_traffics = [traffic['group']['code'] for traffic in response.json()['results'][0]['traffic_light']]
    assert codes_in_issuegroups == codes_in_traffics


def test_issues_order(client, data, data_issuegroups):
    data_issuegroups.service_issue5.issue.weight = 2
    data_issuegroups.service_issue5.issue.save()
    check_issue_groups()
    response = client.json.get(
        reverse('services-api:service-issuegroups', args=[data.service.id])
    )
    issues = [
        ('id' in issue, issue['weight'])
        for issue in response.json()[0]['issues']
    ]
    assert issues == [(True, 40), (True, 20), (False, 20), (False, 20)]


def test_percents_round(client, data, data_issuegroups):
    ServiceIssue.objects.filter(
        id__in=[data_issuegroups.service_issue2.id, data_issuegroups.service_issue3.id]
    ).mark_fixed()
    check_issue_groups()
    response = client.json.get(
        reverse('services-api:service-issuegroups', args=[data.service.id])
    )
    assert response.json()[1]['summary']['current_weight'] == 33


def test_issue_groups_with_appealed_issues(client, data, data_issuegroups):
    issue1 = factories.IssueFactory(issue_group=data_issuegroups.group1)
    issue2 = factories.IssueFactory(issue_group=data_issuegroups.group2)
    issue3 = factories.IssueFactory(issue_group=data_issuegroups.group2)

    factories.ServiceIssueFactory(
        service=data.service,
        issue=issue1,
        state=ServiceIssue.STATES.APPEALED
    )
    factories.ServiceIssueFactory(
        service=data.service,
        issue=issue2,
        state=ServiceIssue.STATES.APPEALED
    )
    factories.ServiceIssueFactory(
        service=data.service,
        issue=issue3,
        state=ServiceIssue.STATES.APPEALED
    )

    check_issue_groups()

    response = client.json.get(
        reverse('services-api:service-issuegroups', args=[data.service.id])
    )
    assert response.status_code == 200
    result = response.json()

    assert result[0]['issues_count'] == 2
    assert result[1]['issues_count'] == 3
    for group in range(2):
        for issue in result[group]['issues']:
            assert issue['is_appealed'] == ServiceIssue.objects.appealed().filter(
                service=data.service,
                issue__code=issue['code']
            ).exists()


@pytest.mark.parametrize('api', ('api-v3', 'api-v4',))
def test_get_services_with_fields_num_queries(client, django_assert_num_queries_lte, api):
    """
    GET /api/v3(v4)/services/?fields=...

    Проверим, что при запросе любого из доступных полей количество запросов к базе не увеличивается
    """
    services = factories.ServiceFactory.create_batch(100)
    issue_groups = factories.IssueGroupFactory.create_batch(3)
    for service in services:
        for issue_group in issue_groups:
            factories.ServiceTrafficStatusFactory(issue_group=issue_group, service=service, issue_count=5, level='ok')
    # В v4 по дефолту отдаём мало полей, а при первом походе в ручку хотим получить максимум возможных полей
    url = reverse(f'{api}:service-list')
    response = client.json.get(url)
    assert response.status_code == 200
    data = response.json()
    all_fields = []
    fields_to_exclude = {'bugs_count', 'releases_count', 'lsr_count'}
    stack = [(data['results'][0], '')]
    while stack:
        element, prefix = stack.pop()
        for item, value in element.items():
            if item in fields_to_exclude:
                continue
            new_prefix = f'{prefix}.{item}' if prefix else item
            if isinstance(value, dict):
                stack.append((value, new_prefix))
            all_fields.append(new_prefix)
    url = reverse('%s:service-list' % api)
    for field in all_fields:
        waffle_query = 1
        perms_query = 2
        num_queries = 4  # different on single/module rune
        if field == 'traffic_light':
            field = 'id,traffic_light'
        with django_assert_num_queries_lte(num_queries + waffle_query + perms_query):
            response = client.json.get(url, data={'fields': field})
        assert response.status_code == 200


@pytest.mark.parametrize('api', ('api-v3', 'api-v4',))
def test_get_service_with_fields(client, api):
    """
    GET /api/v3(v4)/services/<id>/?fields=...
    """
    service = factories.ServiceFactory()
    url = reverse('%s:service-detail' % api, args=(service.id,))
    response = client.json.get(url)
    assert response.status_code == 200
    data = response.json()
    all_fields = []
    fields_to_exclude = {'bugs_count', 'releases_count', 'lsr_count'}
    stack = [(data, '')]
    while stack:
        element, prefix = stack.pop()
        for item, value in element.items():
            if item in fields_to_exclude:
                continue
            new_prefix = f'{prefix}.{item}' if prefix else item
            if isinstance(value, dict):
                stack.append((value, new_prefix))
            all_fields.append(new_prefix)
    for field in all_fields:
        response = client.json.get(url, data={'fields': field})
        assert response.status_code == 200


@pytest.mark.parametrize('name_field', ('id,name.en', 'id,name.ru',))
@pytest.mark.parametrize('api', ('api-v3', 'api-v4',))
def test_get_service_name(client, django_assert_num_queries_lte, api, name_field):
    """
    GET /api/v3(v4)/services/<id>/on_duty/?fields=...
    """
    service = factories.ServiceFactory()
    url = reverse('%s:service-list' % api)

    waffle_check_query = 1
    total_query_limit = 5 + waffle_check_query  # differs on single/module run
    with django_assert_num_queries_lte(total_query_limit):
        response = client.json.get(url, data={'fields': name_field})
    assert response.status_code == 200
    data = response.json()
    assert data['results'] == [{'name': {'ru': service.name, 'en': service.name_en}, 'id': service.id}]


@pytest.mark.parametrize(('state', 'status_code'), (
    (None, 200),
    ('requested', 409),
    ('partially_approved', 409),
    ('approved', 409),
    ('rejected', 200),
    ('processing_idm', 409),
    ('processing_abc', 409),
    ('completed', 200),
))
def test_delete_service_with_descendants(client, service_with_owner, state, status_code):
    parent_service = service_with_owner
    child_service = factories.ServiceFactory(parent=parent_service)
    if state is not None:
        factories.ServiceMoveRequestFactory(service=child_service, state=state)
    client.login(parent_service.owner.login)
    url = reverse('services-api:service-detail', args=[parent_service.pk])
    with patch('plan.services.tasks.delete_service'):
        with patch('plan.services.tasks.drop_requests'):
            with patch('plan.services.models.get_unclosable_services') as close_patcher:
                close_patcher.return_value = []
                response = client.json.patch(url, {'state': Service.states.DELETED})
    assert response.status_code == status_code


@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4', 'api-frontend'))
def test_create_validate_uniq_slug(api, client, data, staff_factory):
    staff = staff_factory()
    factories.ServiceFactory(slug='test_service')

    client.login(staff.login)
    url = reverse(f'{api}:service-list')
    post_data = {
        'name': {'ru': 'Новый сервис', 'en': 'New service'},
        'slug': 'test_service',
        'owner': staff.login,
        'description': {'ru': 'Описание сервиса', 'en': 'Service description'},
    }
    expected_data = {
        'error': {
            'detail': 'Invalid input.',
            'code': 'invalid',
            'extra': {
                'slug': {
                    'ru': 'Сервис с таким слагом уже существует',
                    'en': 'Service with this slug already exists',
                }
            }
        }
    }
    response = client.json.post(url, post_data)
    assert response.status_code == 400
    assert response.json() == expected_data


@pytest.mark.parametrize('api', ['frontend', 'v3', 'v4'])
@pytest.mark.parametrize('staff_role', ['full_access', 'own_only_viewer', 'services_viewer'])
def test_view_own_services_permission_detail(client, services_tree, staff_factory, staff_role, api):
    service_a = Service.objects.get(slug='A')
    service_e = Service.objects.get(slug='E')
    staff = staff_factory(staff_role)
    factories.ServiceMemberFactory(staff=staff, service=service_e)
    url_a = reverse(f'api-{api}:service-detail', args=[service_a.id])
    url_e = reverse(f'api-{api}:service-detail', args=[service_e.id])
    client.login(staff.login)
    response = client.get(url_e)
    assert response.status_code == 200

    response = client.get(url_a)

    if staff_role == 'own_only_viewer':
        assert response.status_code == 404
    else:
        assert response.status_code == 200


@pytest.mark.parametrize('api', ['frontend', 'v3', 'v4'])
@pytest.mark.parametrize('staff_role', ['full_access', 'own_only_viewer', 'services_viewer'])
def test_view_own_services_permission_list(client, services_tree, staff_factory, staff_role, api):
    service = Service.objects.get(slug='C')
    staff = staff_factory(staff_role)
    factories.ServiceMemberFactory(staff=staff, service=service)
    url = reverse(f'api-{api}:service-list')
    client.login(staff.login)
    response = client.get(url)
    assert response.status_code == 200
    data = response.json()['results']
    if staff_role == 'own_only_viewer':
        # Во фронтовой ручке вернем еще и предка
        obj_count = 2 if api == 'frontend' else 1
        services_ids = {'A', 'C'} if api == 'frontend' else {'C'}
    else:
        obj_count = Service.objects.count()
        services_ids = set(Service.objects.values_list('slug', flat=True))
    assert len(data) == obj_count
    assert {service['slug'] for service in data} == services_ids

    staff_perms = [
        item.split('.')[-1] for item in staff.user.get_all_permissions() if item.startswith('internal_roles')
    ]

    allowed_fields = set()
    for perm in staff_perms:
        allowed_fields.update(ServicesView.PERMISSION_CODENAME_MAPPER.get(perm, 'empty'))

    for field in data[0].keys():
        assert field in allowed_fields


@pytest.mark.parametrize('api', ['frontend', 'v3', 'v4'])
@pytest.mark.parametrize('staff_role', ['full_access', ])
def test_services_fields_detail(client, services_tree, staff_factory, staff_role, api):
    service = Service.objects.get(slug='E')
    tag = factories.ServiceTagFactory()
    service.tags.add(tag)

    staff = staff_factory(staff_role)
    factories.ServiceMemberFactory(staff=staff, service=service)

    url = reverse(f'api-{api}:service-detail', args=[service.id])
    client.login(staff.login)
    response = client.get(url, {'fields': 'tags.id'})
    assert response.status_code == 200

    result = response.json()
    assert result['tags'][0]['id'] == tag.id


@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4', 'api-frontend'))
@pytest.mark.parametrize(('role', 'tags'), (('services_viewer', 0), ('full_access', 1)))
def test_ancestors_tags_restricted(client, services_tree, staff_factory, api, role, tags):
    tag = factories.ServiceTagFactory()

    service = Service.objects.get(slug='D')
    parent_service = Service.objects.get(slug='C')
    parent_service.tags.add(tag)
    root_service = Service.objects.get(slug='A')
    root_service.tags.add(tag)

    check_obj_with_denormalized_fields(service, Service.DENORMALIZED_FIELDS, fix=True)

    staff = staff_factory(role)
    client.login(staff.login)

    factories.ServiceMemberFactory(staff=staff, service=service)

    url = reverse(f'{api}:service-detail', args=[service.id])
    response = client.get(url, {'fields': 'ancestors'})

    assert response.status_code == 200
    ancestors = response.json()['ancestors']
    assert len(ancestors) == 2
    assert len(ancestors[0].get('tags', [])) == tags
    assert len(ancestors[1].get('tags', [])) == tags


@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4', 'api-frontend'))
@override_switch('new_internal_roles_on', active=True)
@pytest.mark.parametrize('staff_role', ['own_only_viewer', 'full_access', 'services_viewer'])
def test_create_service_internal_roles(api, staff_role, client, staff_factory, data):
    staff = staff_factory(staff_role)
    client.login(staff.login)
    with patch('plan.services.tasks.register_service'):
        response = client.json.post(
            reverse(f'{api}:service-list'),
            {
                'name': {'ru': 'Котики вперёд!', 'en': 'Cats'},
                'slug': 'cats',
                'owner': staff.login,
                'description': {'ru': 'Описание сервиса', 'en': 'Service description'},
            }
        )
    if staff_role == 'full_access':
        assert response.status_code == 201

    else:
        assert response.status_code == 403


@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4', 'api-frontend'))
def test_create_service_membership_inheritance(api, client, staff_factory, data):
    staff = staff_factory('full_access')
    client.login(staff.login)

    with patch('plan.services.tasks.register_service'):
        response = client.json.post(
            reverse(f'{api}:service-list'),
            {
                'name': {'ru': 'Котики вперёд!', 'en': 'Cats'},
                'slug': 'cats',
                'owner': staff.login,
                'description': {'ru': 'Описание сервиса', 'en': 'Service description'},
            }
        )

    assert response.status_code == 201

    service = Service.objects.get(slug='cats')
    assert not service.membership_inheritance


@pytest.mark.parametrize('is_superuser', [True, False])
@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4', 'api-frontend'))
def test_create_service_not_in_sandbox(api, is_superuser, client, staff_factory):
    staff = staff_factory('full_access')

    factories.ServiceTypeFactory(
        name='Сервис',
        name_en='Service',
        code=ServiceType.UNDEFINED,
    )
    factories.ServiceFactory(
        slug=settings.ABC_DEFAULT_SERVICE_PARENT_SLUG,
        parent=None,
        owner=staff_factory('full_access'),
    )
    parent_service = factories.ServiceFactory(
        slug='cats',
        parent=None,
        owner=staff_factory('full_access'),
    )

    staff.user.is_superuser = is_superuser
    staff.user.save()

    client.login(staff.login)

    with patch('plan.services.tasks.register_service'):
        response = client.json.post(
            reverse(f'{api}:service-list'),
            {
                'name': {'ru': 'Котики вперёд!', 'en': 'Cats'},
                'slug': 'kittens',
                'owner': staff.login,
                'description': {'ru': 'Описание сервиса', 'en': 'Service description'},
                'parent': parent_service.pk,
            }
        )
        assert response.status_code == 201

    service = Service.objects.get(slug='kittens')

    if is_superuser:
        assert service.parent.slug == parent_service.slug
        assert service.is_exportable is True
    else:
        assert service.parent.slug == settings.ABC_DEFAULT_SERVICE_PARENT_SLUG
        assert service.is_exportable is False


@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4', 'api-frontend'))
def test_edit_oebs_flags_success(client, data, api):
    client.login(data.service.owner.login)
    assert data.service.use_for_hardware is False
    assert data.service.use_for_hr is False
    assert data.service.use_for_procurement is False
    OEBSAgreement.objects.create(service=data.service, state=STATES.DECLINED)
    response = client.json.patch(
        reverse(f'{api}:service-detail', args=[data.service.id]),
        data={'use_for_hr': True, 'use_for_revenue': True},
    )

    assert response.status_code == 200

    data.service.refresh_from_db()
    assert data.service.use_for_hr is False
    assert data.service.use_for_procurement is False

    agreement = data.service.active_agreement
    assert agreement.action == 'change_flags'
    assert agreement.attributes == {'use_for_hr': True, 'use_for_revenue': True}


@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4', 'api-frontend'))
@pytest.mark.parametrize('current_type', (ServiceType.UNDEFINED, 'some_type'))
@pytest.mark.parametrize('is_allowed_type', (True, False))
def test_edit_service_type(client, data, api, current_type, is_allowed_type):
    client.login(data.service.owner.login)
    parent_type = factories.ServiceTypeFactory()
    new_type = factories.ServiceTypeFactory()
    data.service.parent.service_type = parent_type
    data.service.parent.save()
    if is_allowed_type:
        new_type.available_parents.add(parent_type)
    if current_type == ServiceType.UNDEFINED:
        data.service.service_type = ServiceType.objects.get(code=ServiceType.UNDEFINED)
    elif current_type == 'some_type':
        data.service.service_type = factories.ServiceTypeFactory(code=current_type)

    data.service.save()

    response = client.json.patch(
        reverse(f'{api}:service-detail', args=[data.service.id]),
        data={'type': new_type.code},
    )

    data.service.refresh_from_db()
    if is_allowed_type and current_type != 'some_type':
        assert response.status_code == 200, response.content
        assert data.service.service_type_id == new_type.id
    else:
        assert response.status_code == 400, response.content
        assert data.service.service_type_id != new_type.id


@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4', 'api-frontend'))
def test_edit_oebs_flags_group_only(client, data, api):
    '''
    Проверяем корректность выставления флага use_for_group_only
    '''
    client.login(data.service.owner.login)
    assert data.service.use_for_group_only is False
    assert data.service.use_for_hr is False
    assert data.service.use_for_procurement is False
    OEBSAgreement.objects.create(service=data.service, state=STATES.DECLINED)
    response = client.json.patch(
        reverse(f'{api}:service-detail', args=[data.service.id]),
        data={'use_for_group_only': True},
    )

    # один флаг выставить нельзя
    assert response.status_code == 400

    data.service.use_for_hr = True
    data.service.save()
    response = client.json.patch(
        reverse(f'{api}:service-detail', args=[data.service.id]),
        data={'use_for_group_only': True},
    )

    # выставить флаг если другие стоят нельзя
    assert response.status_code == 400

    data.service.use_for_hr = False
    data.service.save()

    response = client.json.patch(
        reverse(f'{api}:service-detail', args=[data.service.id]),
        data={
            'use_for_group_only': True,
            'use_for_hr': True,
            'use_for_revenue': True,
        },
    )
    # можно выставить вместе с другими
    assert response.status_code == 200

    data.service.refresh_from_db()
    assert data.service.use_for_hr is False
    assert data.service.use_for_procurement is False
    assert data.service.use_for_group_only is False

    agreement = data.service.active_agreement
    assert agreement.action == 'change_flags'
    assert agreement.attributes == {
        'use_for_hr': True,
        'use_for_revenue': True,
        'use_for_group_only': True,
    }
    finalize_flag_changing(agreement)
    agreement.apply(leaf_oebs_id='leaf_id', parent_oebs_id='group_id')

    response = client.json.patch(
        reverse(f'{api}:service-detail', args=[data.service.id]),
        data={
            'use_for_group_only': False,
        },
    )

    # снять флаг можно
    assert response.status_code == 200


@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4', 'api-frontend'))
def test_set_oebs_flags_group_only(client, data, api):
    '''
    нельзя выставить флаг, если сервис уже oebs синхронизированный
    '''
    client.login(data.service.owner.login)
    assert data.service.use_for_group_only is False
    data.service.use_for_hr = True
    data.service.save()
    response = client.json.patch(
        reverse(f'{api}:service-detail', args=[data.service.id]),
        data={
            'use_for_group_only': True,
            'use_for_procurement': True,
        },
    )

    assert response.status_code == 400


@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4', 'api-frontend'))
def test_edit_oebs_flags_group_only_remove(client, data, api):
    '''
    Нельзя снять флаг, если уже поставили
    '''
    client.login(data.service.owner.login)
    data.service.use_for_group_only = True
    data.service.save()
    response = client.json.patch(
        reverse(f'{api}:service-detail', args=[data.service.id]),
        data={'use_for_group_only': False},
    )
    assert response.status_code == 400


@override_settings(OEBS_MDM_RESTRICTED=True)
@pytest.mark.parametrize('from_mdm', (True, False))
def test_edit_oebs_flags_mdm(client, data, from_mdm):
    if from_mdm:
        data.service.owner.department.url = settings.OEBS_MDM_DEPARTMENT
        data.service.owner.department.save()
    client.login(data.service.owner.login)
    assert data.service.use_for_hardware is False
    assert data.service.use_for_hr is False
    assert data.service.use_for_procurement is False
    response = client.json.patch(
        reverse('api-frontend:service-detail', args=[data.service.id]),
        data={'use_for_hr': True, 'use_for_revenue': True},
    )
    if from_mdm:
        assert response.status_code == 200
    else:
        assert response.status_code == 403


@override_settings(RESTRICT_OEBS_SUBTREE=True)
@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4', 'api-frontend'))
def test_edit_oebs_flags_failed_allowed_subtree(client, data, api):
    client.login(data.service.owner.login)
    assert data.service.use_for_hr is False
    response = client.json.patch(
        reverse(f'{api}:service-detail', args=[data.service.id]),
        data={'use_for_hr': True},
    )

    assert response.status_code == 400
    assert (
        response.json()['error']['title']['en'] ==
        'Setting OEBS flags for services outside OEBS allowed subtree is restricted'
    )


@pytest.mark.parametrize('oebs_agreement', ['child', 'service', 'metaservice'], indirect=True)
@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4', 'api-frontend'))
@pytest.mark.parametrize('is_responsible', (True, False))
def test_edit_oebs_flags_fail(client, data, api, oebs_agreement, is_responsible):
    """Запрещать изменять OEBS флаги сервиса, если у его предка, потомка или его самого есть активное OEBSAgreement"""
    if is_responsible:
        client.login(data.service.owner.login)
    assert data.service.use_for_hardware is False
    assert data.service.use_for_hr is False
    assert data.service.use_for_procurement is False
    response = client.json.patch(
        reverse(f'{api}:service-detail', args=[data.service.id]),
        data={'use_for_hr': True, 'use_for_revenue': True},
    )
    if is_responsible:
        assert response.status_code == 400
        assert (
            response.json()['error']['title']['en'] ==
            'Action is impossible because active agreement already exists.'
        )
    else:
        assert response.status_code == 403

    data.service.refresh_from_db()
    oebs_agreement.service.refresh_from_db()
    assert data.service.use_for_hr is False
    assert data.service.use_for_procurement is False
    assert not data.service.oebs_agreements.exists() or data.service == oebs_agreement.service
    assert oebs_agreement.service.oebs_agreements.count() == 1


@pytest.mark.parametrize(
    ('use_for_hardware', 'use_for_hr', 'use_for_procurement', 'use_for_revenue', 'conflict'),
    [
        (True, True, True, True, False),
        (True, False, False, False, True),
        (False, True, False, False, True),
        (False, False, True, False, True),
        (False, False, True, True, False),
        (None, None, None, True, False),
        (None, False, None, True, False),
        (False, False, False, True, False),
        (False, True, False, None, True),
        (False, False, True, None, True),
    ]
)
@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4', 'api-frontend'))
def test_edit_oebs_flags_check_conflict(client, data, api,
                                        use_for_hardware, use_for_hr, use_for_procurement, use_for_revenue, conflict):
    client.login(data.service.owner.login)

    response = change_flags(
        client=client, api=api,
        service=data.service,
        use_for_hardware=use_for_hardware,
        use_for_hr=use_for_hr,
        use_for_procurement=use_for_procurement,
        use_for_revenue=use_for_revenue,
    )

    if conflict:
        assert response.status_code == 400
        assert data.service.oebs_agreements.count() == 0
    else:
        assert response.status_code == 200
        assert data.service.oebs_agreements.count() == 1
        agreement = data.service.oebs_agreements.first()
        expected_attributes = {}
        if use_for_hardware:
            expected_attributes['use_for_hardware'] = use_for_hardware
        if use_for_hr:
            expected_attributes['use_for_hr'] = use_for_hr
        if use_for_procurement:
            expected_attributes['use_for_procurement'] = use_for_procurement
        if use_for_revenue:
            expected_attributes['use_for_revenue'] = use_for_revenue
        if use_for_hardware or use_for_hr or use_for_procurement:
            expected_attributes['use_for_revenue'] = True

        assert agreement.attributes == expected_attributes

    data.service.refresh_from_db()
    assert data.service.use_for_hardware is False
    assert data.service.use_for_hr is False
    assert data.service.use_for_procurement is False
    assert data.service.use_for_revenue is False


@pytest.mark.parametrize(
    ('use_for_hardware', 'use_for_hr', 'use_for_procurement', 'use_for_revenue', 'conflict'),
    [
        (True, True, True, True, False),
        (False, True, False, False, True),
        (None, None, None, True, True),
        (None, False, None, True, True),
    ]
)
@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4', 'api-frontend'))
def test_edit_oebs_flags_for_gradient_service(client, data, api, conflict,
                                              use_for_hardware, use_for_hr, use_for_procurement, use_for_revenue):
    client.login(data.service.owner.login)
    tag, _ = ServiceTag.objects.get_or_create(slug=settings.GRADIENT_VS)
    data.service.tags.add(tag)

    response = change_flags(
        client=client, api=api,
        service=data.service,
        use_for_hardware=use_for_hardware,
        use_for_hr=use_for_hr,
        use_for_procurement=use_for_procurement,
        use_for_revenue=use_for_revenue,
    )
    if conflict:
        assert response.status_code == 400
        assert data.service.oebs_agreements.count() == 0
    else:
        assert response.status_code == 200
        assert data.service.oebs_agreements.count() == 1


@pytest.mark.parametrize('tag_slug', settings.REVIEW_REQUIRED_TAG_SLUGS)
@pytest.mark.parametrize(('old_value', 'new_value'), itertools.product([True, False], [True, False]))
@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4', 'api-frontend'))
def test_update__sox_tag_change_review_policy(
        client,
        data,
        api: str,
        old_value: bool,
        new_value: bool,
        tag_slug: str,
        review_required_tags: [ServiceTag],

):
    client.login(data.service.owner.login)
    service: Service = data.service
    tag = ServiceTag.objects.get(slug=tag_slug)
    if old_value:
        service.tags.add(tag)

    data = {'tags': []}
    if new_value:
        data['tags'].append(tag.id)

    with mock.patch('plan.services.tasks.update_service_review_policy.delay') as update_task_mock:
        response = client.json.patch(reverse(f'{api}:service-detail', args=[service.id]), data)

    assert response.status_code == 200, (response.status_code, response.json())
    if old_value ^ new_value:
        update_task_mock.assert_called_once_with(service_slug=service.slug, review_required=new_value)

    else:
        update_task_mock.assert_not_called()

    assert new_value ^ (tag.id not in {tag["id"] for tag in response.json()["tags"]})


@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4', 'api-frontend'))
@pytest.mark.parametrize('mock_tvm_service_ticket', [settings.WATCHER_TVM_ID, 123], indirect=True)
def test_edit_service_flags(client, data, api, mock_tvm_service_ticket):

    assert data.service.flags == {'duty1': False, 'duty2': True}
    new_flags = {'cats_rule': True}

    response = client.json.patch(
        reverse(f'{api}:service-detail', args=[data.service.id]),
        data={'flags': new_flags},
    )

    if mock_tvm_service_ticket == settings.WATCHER_TVM_ID:
        assert response.status_code == 200
        assert response.json()['flags'] == new_flags
        data.service.refresh_from_db()
        assert data.service.flags == new_flags
    else:
        assert response.status_code == 403
