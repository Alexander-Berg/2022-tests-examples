import datetime
import pytest

from enum import auto
from fastapi_utils.enums import StrEnum
from fastapi import Request
from contextlib import nullcontext as does_not_raise
from unittest.mock import patch

import responses

from watcher.db import UserPermission, Role
from watcher.config import settings
from watcher.logic.timezone import now
from watcher.logic.exceptions import PermissionDenied

PERMISSION_USER_UID = 444
PERMISSION_USER_ID = 1005001


class RelationToService(StrEnum):
    in_service = auto()
    in_service_responsible = auto()
    in_ancestor = auto()
    in_ancestor_responsible = auto()
    outsider = auto()


@pytest.fixture(autouse=True)
def user_without_permissions(test_request_user, staff_factory, scope_session):
    scope_session.query(UserPermission).delete()
    settings.test_user_data['uid'] = PERMISSION_USER_UID
    return staff_factory(uid=PERMISSION_USER_UID, user_id=PERMISSION_USER_ID)


@pytest.fixture(scope='function')
def shift_with_service_member(request, user_with_permissions, service_factory, shift_factory, schedule_factory,
                              role_factory, member_factory):
    relation = request.param
    staff = user_with_permissions
    service = service_factory()
    shift = shift_factory(schedule=schedule_factory(service=service), empty=False)

    if relation in (RelationToService.in_service, RelationToService.in_service_responsible):
        member = member_factory(staff=staff, service=service)
        if relation == RelationToService.in_service_responsible:
            member.role = role_factory(code=Role.RESPONSIBLE)
    return shift, staff, relation


@pytest.mark.parametrize(
    'perm', (
        settings.OWN_ONLY_VIEWER_ID,
        settings.SERVICES_VIEWER_ID,
        settings.FULL_ACCESS_ID,
        None,
    )
)
def test_granular_permissions_on_get(client, permission_factory, schedules_group_factory, db_session, perm):
    schedule_group = schedules_group_factory()
    if perm:
        permission_factory(
            user_id=PERMISSION_USER_ID,
            permission_id=perm,
        )
    response = client.get(
        f'/api/watcher/v1/schedule_group/{schedule_group.id}'
    )

    if perm:
        assert response.status_code == 200, response.text
    else:
        assert response.status_code == 403, response.text
        assert response.json()['context']['message']['en'] == 'User does not have strictly limited role'


@pytest.mark.parametrize(
    'perm', (
        settings.OWN_ONLY_VIEWER_ID,
        settings.SERVICES_VIEWER_ID,
        settings.FULL_ACCESS_ID,
    )
)
def test_granular_permissions_on_delete(
    client, group_responsible_factory,
    permission_factory, schedules_group_factory, user_without_permissions,
    scope_session, perm
):
    schedule_group = schedules_group_factory()

    group_responsible_factory(
        schedule_group=schedule_group,
        responsible=user_without_permissions,
    )
    permission_factory(
        user_id=PERMISSION_USER_ID,
        permission_id=perm,
    )

    response = client.delete(
        f'/api/watcher/v1/schedule_group/{schedule_group.id}'
    )
    if perm == settings.FULL_ACCESS_ID:
        assert response.status_code == 204, response.text
    else:
        assert response.status_code == 403, response.text
        assert response.json()['context']['message']['en'] == 'User does not have advanced role'


@pytest.mark.parametrize(
    'perm', (
        settings.OWN_ONLY_VIEWER_ID,
        settings.SERVICES_VIEWER_ID,
        settings.FULL_ACCESS_ID,
    )
)
@pytest.mark.parametrize('is_responsible', (True, False))
@pytest.mark.parametrize('action', ('delete', 'patch'))
def test_group_responsible_permissions_on_change(
    client, group_responsible_factory,
    permission_factory, schedules_group_factory, user_without_permissions,
    scope_session, is_responsible, action, perm,
):
    schedule_group = schedules_group_factory()
    if is_responsible:
        group_responsible_factory(
            schedule_group=schedule_group,
            responsible=user_without_permissions,
        )
    permission_factory(
        user_id=PERMISSION_USER_ID,
        permission_id=perm,
    )
    if action == 'delete':
        response = client.delete(
            f'/api/watcher/v1/schedule_group/{schedule_group.id}'
        )
    else:
        patch_data = {
            'name': 'test schedule group name',
        }
        response = client.patch(
            f'/api/watcher/v1/schedule_group/{schedule_group.id}',
            json=patch_data,
        )
    if is_responsible and perm == settings.FULL_ACCESS_ID:
        assert response.status_code == 204 if action == 'delete' else 200, response.text
    else:
        assert response.status_code == 403, response.text
        if perm == settings.FULL_ACCESS_ID:
            assert response.json()['context']['message']['en'] == 'User is not one of group responsibles'
        else:
            assert response.json()['context']['message']['en'] == 'User does not have advanced role'


@pytest.mark.parametrize(
    'perm', (
        settings.OWN_ONLY_VIEWER_ID,
        settings.SERVICES_VIEWER_ID,
        settings.FULL_ACCESS_ID,
    )
)
def test_granular_permissions_on_create(client, permission_factory, scope_session, assert_json_keys_value_equal, perm):
    initial_data = {
        'name': 'имя группы',
        'slug': 'test_schedule_slug',
    }
    permission_factory(
        user_id=PERMISSION_USER_ID,
        permission_id=perm,
    )
    response = client.post(
        '/api/watcher/v1/schedule_group/',
        json=initial_data
    )
    if perm == settings.FULL_ACCESS_ID:
        assert response.status_code == 201, response.text
    else:
        assert response.status_code == 403, response.text
        assert response.json()['context']['message']['en'] == 'User does not have advanced role'


@pytest.mark.parametrize(
    'relation', (
        RelationToService.in_service,
        RelationToService.in_ancestor,
        RelationToService.in_ancestor_responsible,
        RelationToService.outsider,
    )
)
def test_composition_create_permissions(client, user_with_permissions, service_factory,
                                        member_factory, role_factory, scope_session, relation):
    service = service_factory()
    staff = user_with_permissions
    if relation == RelationToService.in_service:
        member_factory(staff=staff, service=service)
    elif relation in (RelationToService.in_ancestor, RelationToService.in_ancestor_responsible):
        ancestor = service_factory()
        service.ancestors = [{'id': ancestor.id}]
        member = member_factory(staff=user_with_permissions, service=ancestor)

        if relation == RelationToService.in_ancestor_responsible:
            member.role = role_factory(code=Role.RESPONSIBLE)
        scope_session.commit()
    with patch('watcher.api.routes.composition.update_composition'):
        response = client.post(
            '/api/watcher/v1/composition/',
            json={
                'name': 'test composition',
                'slug': 'slug',
                'service_id': service.id,
                'full_service': True,
            }
        )

    if relation in (RelationToService.in_service, RelationToService.in_ancestor_responsible):
        assert response.status_code == 201
    elif relation in (RelationToService.in_ancestor, RelationToService.outsider):
        assert response.status_code == 403
        assert response.json()['context']['message']['en'] == 'No permission to create composition'


@pytest.mark.parametrize(
    'relation,used', (
        (RelationToService.in_service, False),
        (RelationToService.in_service, True),
        (RelationToService.in_service_responsible, False),
        (RelationToService.outsider, False),
    )
)
def test_composition_update_permissions(client, user_with_permissions, service_factory, member_factory, role_factory,
                                        composition_factory, schedule_factory, interval_factory, revision_factory,
                                        slot_factory, scope_session, relation, used):
    service = service_factory()
    responsible = role_factory(code=Role.RESPONSIBLE)
    if relation in (RelationToService.in_service, RelationToService.in_service_responsible):
        member = member_factory(staff=user_with_permissions, service=service)
        if relation == RelationToService.in_service_responsible:
            member.role = responsible
        scope_session.commit()

    composition = composition_factory(service=service)
    if used:
        schedule = schedule_factory(service=service)
        revision = revision_factory(schedule=schedule)
        interval = interval_factory(schedule=schedule, revision=revision)
        slot_factory(composition=composition, interval=interval)

    patch_data = {'name': 'somenew', 'full_service': True}
    response = client.patch(
        f'/api/watcher/v1/composition/{composition.id}',
        json=patch_data
    )

    if relation == RelationToService.in_service:
        if used:
            assert response.status_code == 403
            assert response.json()['context']['message']['en'] == 'No permission to change composition being used'
        else:
            assert response.status_code == 200
    elif relation == RelationToService.in_service_responsible:
        assert response.status_code == 200
    elif relation == RelationToService.outsider:
        assert response.status_code == 403


def test_composition_delete_permissions(client, user_with_permissions, service_factory, member_factory, role_factory,
                                        composition_factory, schedule_factory, revision_factory, interval_factory,
                                        slot_factory, scope_session):
    service = service_factory()
    member_factory(staff=user_with_permissions, service=service, role=role_factory(code=Role.RESPONSIBLE))
    composition = composition_factory(service=service)

    response = client.delete(
        f'/api/watcher/v1/composition/{composition.id}'
    )

    assert response.status_code == 204


@responses.activate
@pytest.mark.parametrize('relation', (RelationToService.in_service, RelationToService.outsider))
def test_schedule_create_permissionns(client, user_with_permissions, service_factory, member_factory, relation, scope_session):
    service = service_factory()
    if relation == RelationToService.in_service:
        member_factory(staff=user_with_permissions, service=service)

    responses.add(
        responses.GET,
        'https://abc-back.test.yandex-team.ru/api/v4/duty/schedules/',
        status=200,
        json={'results': []}
    )
    response = client.post(
        '/api/watcher/v1/schedule/',
        json={
            'slug': 'test_schedule_slug',
            'name': 'test schedule name',
            'service_id': service.id,
        }
    )

    if relation == RelationToService.in_service:
        assert response.status_code == 201
    elif relation == RelationToService.outsider:
        assert response.status_code == 403


@pytest.mark.parametrize(
    'responsible_type', (False, True)
)
def test_check_responsible_or_schedule_owner(client, user_with_permissions, service_factory, schedule_factory,
                                             member_factory, schedule_responsible_factory,
                                             role_factory, scope_session, responsible_type):
    service = service_factory()
    schedule = schedule_factory(service=service)
    role = role_factory()
    member_factory(staff=user_with_permissions, service=service, role=role)

    if responsible_type:
        schedule_responsible_factory(schedule=schedule, responsible=user_with_permissions)

    response = client.patch(
        f'/api/watcher/v1/schedule/{schedule.id}',
        json={
            'name': 'test name',
        }
    )

    if responsible_type:
        assert response.status_code == 200
    else:
        assert response.status_code == 403


@responses.activate
def test_interval_creation_pipeline_permission(client, user_with_permissions, staff_factory, member_factory,
                                               permission_factory, service_factory):
    service = service_factory()
    member_factory(staff=user_with_permissions, service=service)
    responses.add(
        responses.GET,
        'https://abc-back.test.yandex-team.ru/api/v4/duty/schedules/',
        status=200,
        json={'results': []}
    )

    response = client.post(
        '/api/watcher/v1/schedule/',
        json={
            "slug": "ololo-test-2",
            "name": "тестовый график 2",
            "service_id": service.id,
            "recalculate": False,
        }
    )
    assert response.status_code == 201
    schedule_id = response.json()['id']

    interval_data = {
        "schedule_id": schedule_id,
        "intervals": [
            {
                "schedule_id": schedule_id,
                "duration": 64000,
                "type_employment": "empty",
                "unexpected_holidays": "remove",
                "order": 1,
                "slots": [],
            }
        ]
    }

    response = client.put(
        '/api/watcher/v1/interval/',
        json=interval_data
    )
    assert response.status_code == 200

    settings.test_user_data['uid'] = 457
    staff = staff_factory(uid=457, login='test_another_user')
    permission_factory(
        user_id=staff.user_id,
        permission_id=settings.FULL_ACCESS_ID,
    )

    response = client.put(
        '/api/watcher/v1/interval/',
        json=interval_data
    )
    assert response.status_code == 403


@pytest.mark.parametrize('is_responsible', (True, False))
def test_resolve_problem_permissions(client, permission_factory, user_without_permissions, member_factory, role_factory,
                                     service_factory, schedule_factory, shift_factory, problem_factory, is_responsible):
    service = service_factory()
    if is_responsible:
        member_factory(staff=user_without_permissions, service=service, role=role_factory(code=Role.RESPONSIBLE))
    schedule = schedule_factory(service=service)
    shift = shift_factory(schedule=schedule)
    problem = problem_factory(shift=shift)
    permission_factory(
        user_id=PERMISSION_USER_ID,
        permission_id=settings.FULL_ACCESS_ID,
    )

    response = client.post(
        f'/api/watcher/v1/problem/{problem.id}/resolve',
    )
    if is_responsible:
        assert response.status_code == 200, response.text
        data = response.json()
        assert data['status'] == 'resolved'
    else:
        assert response.status_code == 403, response.text
        assert response.json()['context']['message']['en'] == 'No permission to change problem status'


@pytest.mark.parametrize(
    "shift_with_service_member",
    (
        RelationToService.in_service,
        RelationToService.in_service_responsible,
        RelationToService.outsider,
    ),
    indirect=["shift_with_service_member"]
)
@pytest.mark.parametrize('approved', (True, False))
@pytest.mark.parametrize('shift_owner', (True, False))
def test_patch_shift_approve_permissions(client, shift_with_service_member, scope_session, approved, shift_owner):

    shift, staff, relation = shift_with_service_member
    shift.approved = not approved
    if shift_owner:
        shift.staff = staff

    scope_session.commit()

    response = client.patch(
        f'/api/watcher/v1/shift/{shift.id}',
        json={
            'empty': shift.empty,
            'approved': approved
        }
    )

    if relation == RelationToService.in_service_responsible or (shift_owner and approved):
        assert response.status_code == 200, response.text
        assert response.json()['approved'] == approved
    else:
        assert response.status_code == 403, response.text
        assert response.json()['context']['message']['en'] == 'No permission to change shift'


@pytest.mark.parametrize(
    "shift_with_service_member",
    (RelationToService.in_service, ),
    indirect=["shift_with_service_member"]
)
@pytest.mark.parametrize('before_pin', (True, False))
def test_disapprove_self_shift_before_pin(client, shift_with_service_member, scope_session, before_pin):
    shift, staff, relation = shift_with_service_member
    shift.approved = True
    shift.staff = staff

    shift.schedule.pin_shifts = datetime.timedelta(days=7)
    shift.start = now()
    shift.start += datetime.timedelta(days=14) if before_pin else datetime.timedelta(days=3)
    shift.end = shift.start + datetime.timedelta(days=7)

    scope_session.commit()

    response = client.patch(
        f'/api/watcher/v1/shift/{shift.id}',
        json={'approved': False}
    )

    if before_pin:
        assert response.status_code == 200, response.text
        assert response.json()['approved'] is False
    else:
        assert response.status_code == 403, response.text
        assert response.json()['context']['message']['en'] == 'No permission to change shift'


@pytest.mark.parametrize(
    "shift_with_service_member",
    (
        RelationToService.in_service,
        RelationToService.in_service_responsible,
        RelationToService.outsider,
    ),
    indirect=["shift_with_service_member"]
)
def test_patch_shift_take_to_self_permissions(client, shift_with_service_member, scope_session):
    shift, staff, relation = shift_with_service_member
    scope_session.commit()

    response = client.patch(
        f'/api/watcher/v1/shift/{shift.id}',
        json={
            'staff_id': staff.id,
            'empty': shift.empty,
            'approved': shift.approved
        }
    )

    if relation in (RelationToService.in_service_responsible, RelationToService.in_service):
        assert response.status_code == 200, response.text
        assert response.json()['staff_id'] == staff.id
    else:
        assert response.status_code == 403, response.text
        assert response.json()['context']['message']['en'] == 'No permission to change shift'


@pytest.mark.parametrize(
    "shift_with_service_member",
    (
        RelationToService.in_service,
        RelationToService.in_service_responsible,
        RelationToService.outsider,
    ),
    indirect=["shift_with_service_member"]
)
@pytest.mark.parametrize('take_subshift_to_self', (True, False))
@pytest.mark.parametrize('put_subshift_id', (True, False))
def test_update_sub_shifts_permissions(client, shift_with_service_member, shift_factory, scope_session,
                                       take_subshift_to_self, put_subshift_id):
    shift, staff, relation = shift_with_service_member

    start = now()
    middle = start + datetime.timedelta(days=5)
    end = now() + datetime.timedelta(days=10)
    shift.start = start
    shift.end = end
    shift.next = shift_factory()
    subshift = shift_factory(start=start, end=middle, replacement_for=shift)

    scope_session.commit()

    subshift_json = {
        'start': subshift.start.isoformat(),
        'end': subshift.end.isoformat(),
        'staff_id': staff.id if take_subshift_to_self else None,
        'approved': False if take_subshift_to_self else True,
    }
    if put_subshift_id:
        subshift_json['id'] = subshift.id
    response = client.put(
        f'/api/watcher/v1/subshift/{shift.id}',
        json={
            'sub_shifts': [
                subshift_json,
            ]
        }
    )
    if (relation == RelationToService.in_service_responsible
            or (relation == RelationToService.in_service and take_subshift_to_self and put_subshift_id)):
        assert response.status_code == 200, response.text
    elif relation == RelationToService.outsider and take_subshift_to_self:
        assert response.status_code == 400, response.text
        assert response.json()['context']['message']['en'] == 'Not all employees are members of service'
    else:
        assert response.status_code == 403, response.text
        assert response.json()['context']['message']['en'] == 'No permission to change subshifts'


@pytest.mark.parametrize(
    ('method', 'service_ticket_only', 'expectation'),
    (
        ('POST', False, pytest.raises(AssertionError)),
        ('GET', True, does_not_raise()),
        ('POST', True, pytest.raises(PermissionDenied)),
        ('GET', False, pytest.raises(AssertionError))
    )
)
def test_service_ticket_only(method, service_ticket_only, expectation):
    from watcher.api.routes.base import BaseRoute
    request = Request(
        scope={
            'type': 'http',
            'headers': [],
            'service_ticket_only': service_ticket_only,
            'method': method,
        }
    )
    with expectation:
        BaseRoute(request=request)


@pytest.mark.parametrize('responsible_in', ['direct', 'ancestor', 'descendant', None])
@pytest.mark.parametrize('code', [Role.RESPONSIBLE, Role.RESPONSIBLE_FOR_DUTY, Role.DUTY])
def test_change_show_in_services(
    client, schedule_factory, user_with_permissions,
    service_factory, responsible_in, code,
    member_factory, role_factory, schedule_responsible_factory,
    scope_session,
):
    ancestor = service_factory()
    service = service_factory()
    service.ancestors = [{'id': ancestor.id}]

    descendant = service_factory()
    descendant.ancestors = [
        {'id': service.id},
        {'id': ancestor.id},
    ]

    services_map = {
        'direct': service,
        'ancestor': ancestor,
        'descendant': descendant,
    }
    if responsible_in:
        member_factory(
            service=services_map[responsible_in],
            staff=user_with_permissions,
            role=role_factory(code=code),
        )

    schedule = schedule_factory()
    schedule_responsible_factory(
        schedule=schedule,
        responsible=user_with_permissions,
    )
    member_factory(
        staff=user_with_permissions,
        service=schedule.service,
    )
    already_has_link = service_factory()
    schedule.show_in_services.append(already_has_link)
    scope_session.commit()

    response = client.patch(
        f'/api/watcher/v1/schedule/{schedule.id}',
        json={
            'show_in_services': [service.id, already_has_link.id]
        },
    )
    if (
        responsible_in in ('ancestor', 'direct')
        and code != Role.DUTY
    ):
        assert response.status_code == 200, response.text
        data = response.json()['show_in_services']
        assert len(schedule.show_in_services) == 2
        assert len(data) == 2
        assert {link['slug'] for link in data} == {service.slug, already_has_link.slug}
    else:
        assert response.status_code == 400, response.text
        assert len(schedule.show_in_services) == 1
        assert response.json()['error'] == 'not_responsible_in_services'


@pytest.mark.parametrize('action', ['remove', 'add'])
@pytest.mark.parametrize('code', [Role.RESPONSIBLE_FOR_DUTY, Role.DUTY])
def test_put_schedule_group_schedules_permissions(scope_session, user_with_permissions, client, schedules_group_factory,
                                                  schedule_factory, member_factory, role_factory, action, code):
    schedule_group = schedules_group_factory(responsibles=[user_with_permissions])
    schedule_1 = schedule_factory(schedules_group=schedule_group)
    schedule_2 = schedule_factory()

    member_factory(
        service=schedule_2.service if action == 'add' else schedule_1.service,
        staff=user_with_permissions,
        role=role_factory(code=code),
    )

    scope_session.refresh(schedule_group)
    assert {schedule.id for schedule in schedule_group.schedules} == {schedule_1.id, }

    patch_data = {'schedule_ids': [schedule_2.id] if action == 'add' else []}
    response = client.put(
        f'/api/watcher/v1/schedule_group/{schedule_group.id}/schedules',
        json=patch_data,
    )

    if code == Role.DUTY and action == 'add':
        assert response.status_code == 400, response.text
        assert response.json()['error'] == 'not_responsible_in_services'
    else:
        assert response.status_code == 200
        data = response.json()
        expected_data = {schedule_2.id, } if action == 'add' else set()
        assert {schedule['id'] for schedule in data['schedules']} == expected_data


@pytest.mark.parametrize(
    'relation', (
        'in_service_responsible',
        'in_ancestor_responsible',
        'schedule_responsible',
        None
    )
)
def test_upload_shifts_permissions(
    client, user_with_permissions, service_factory,
    member_factory, role_factory, scope_session, relation,
    schedule_factory, schedule_responsible_factory, staff_factory, test_request_user
):
    service = service_factory()
    schedule = schedule_factory(service=service, recalculate=False)
    if relation == 'in_service_responsible':
        member = member_factory(staff=user_with_permissions, service=service)
        member.role = role_factory(code=Role.RESPONSIBLE)
    elif relation == 'in_ancestor_responsible':
        ancestor = service_factory()
        service.ancestors = [{'id': ancestor.id}]
        member = member_factory(staff=user_with_permissions, service=ancestor)
        member.role = role_factory(code=Role.RESPONSIBLE)
    elif relation == 'schedule_responsible':
        member_factory(staff=user_with_permissions, service=service)
        schedule_responsible_factory(schedule=schedule, responsible=user_with_permissions)
    else:
        member_factory(service=service)
    scope_session.commit()

    initial_data = {
        'schedule_id': schedule.id,
        'shifts': [
            {
                'start': now().isoformat(),
                'end': (now() + datetime.timedelta(hours=1)).isoformat(),
                'empty': True,
            },
        ]
    }
    response = client.post(
        '/api/watcher/v1/shift/upload',
        json=initial_data,
    )
    if relation:
        assert response.status_code == 204, response.text
    else:
        assert response.status_code == 403
        assert response.json()['context']['message'] == {
            'ru': 'Нет разрешения на загрузку смен',
            'en': 'No permission to upload shifts'
        }


@pytest.mark.parametrize(
    'relation', (
        RelationToService.in_service,
        RelationToService.outsider,
        RelationToService.in_service_responsible
    ))
def test_schedule_recalculate_permissions(
    client, user_with_permissions, service_factory,
    member_factory, relation, scope_session,
    role_factory, schedule_factory
):
    service = service_factory()
    staff = user_with_permissions
    schedule = schedule_factory(service=service)
    if relation in (RelationToService.in_service, RelationToService.in_service_responsible):
        member = member_factory(staff=staff, service=service)
        if relation == RelationToService.in_service_responsible:
            member.role = role_factory(code=Role.RESPONSIBLE)
    scope_session.commit()
    response = client.post(
        f'/api/watcher/v1/schedule/{schedule.id}/recalculate',
    )
    if relation == RelationToService.in_service_responsible:
        assert response.status_code == 200
    else:
        assert response.status_code == 403


@pytest.mark.parametrize(
    'relation', (
        RelationToService.in_service,
        RelationToService.outsider,
        RelationToService.in_service_responsible
    ))
def test_slot_permissions(
    client, user_with_permissions, service_factory,
    member_factory, relation, scope_session,
    role_factory, schedule_factory, interval_factory, revision_factory,
    slot_factory,
):
    service = service_factory()
    schedule = schedule_factory(service=service)
    interval = interval_factory(schedule=schedule)
    role = role_factory()
    staff = user_with_permissions
    slot = slot_factory(interval=interval)
    if relation in (RelationToService.in_service, RelationToService.in_service_responsible):
        member = member_factory(staff=staff, service=service)
        if relation == RelationToService.in_service_responsible:
            member.role = role_factory(code=Role.RESPONSIBLE)
    scope_session.commit()
    response = client.patch(
        f'/api/watcher/v1/slot/{slot.id}',
        json={
            'role_on_duty_id': role.id,
            'show_in_staff': True,
        }
    )
    if relation == RelationToService.in_service_responsible:
        assert response.status_code == 200
    else:
        assert response.status_code == 403
