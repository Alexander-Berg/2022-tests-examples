from unittest.mock import patch, Mock

import pretend
import pytest

from django.core.urlresolvers import reverse
from django.conf import settings
from django.test.utils import override_settings
from waffle.testutils import override_switch

from plan.common.person import Person
from plan.oebs.models import ACTIONS
from plan.services.models import Service, ServiceMoveRequest, ServiceMember
from plan.resources.models import Resource
from plan.services import tasks

from common import factories
from utils import iterables_are_equal


@pytest.fixture
def moves(data):
    requested = factories.ServiceMoveRequestFactory(
        service=data.service,
        destination=data.other_service,
        source=data.service.parent,
        state=ServiceMoveRequest.REQUESTED,
        requester=data.staff
    )
    completed = factories.ServiceMoveRequestFactory(
        service=data.service,
        destination=data.metaservice,
        source=data.meta_other,
        state=ServiceMoveRequest.COMPLETED,
        requester=data.staff,
        approver_incoming=data.big_boss,
        approver_outgoing=data.big_boss,
    )
    rejected = factories.ServiceMoveRequestFactory(
        service=data.service,
        destination=data.metaservice,
        source=data.meta_other,
        state=ServiceMoveRequest.REJECTED,
        requester=data.stranger
    )
    return pretend.stub(
        requested=requested,
        completed=completed,
        rejected=rejected,
    )


@pytest.mark.parametrize('is_superuser', [True, False])
def test_move_base_service(client, data, is_superuser):
    factories.ServiceTagFactory(slug=settings.BASE_TAG_SLUG)
    data.service.is_base = True
    data.service.save()
    data.stranger.user.is_superuser = is_superuser
    data.stranger.user.save()

    client.login(data.stranger.login)
    response = client.json.post(
        reverse('services-api:moves-list'),
        {
            'service': data.service.id,
            'destination': data.other_service.id
        }
    )

    if is_superuser:
        assert response.status_code == 200
    else:
        assert response.status_code == 403
        assert response.json()['error']['code'] == 'Cannot move base service'


@pytest.mark.parametrize('directly', [True, False])
def test_move_service_out_of_sandbox(client, directly, owner_role, robot, staff_factory):
    sandbox = factories.ServiceFactory(slug=settings.ABC_DEFAULT_SERVICE_PARENT_SLUG)
    if directly:
        service = factories.ServiceFactory(parent=sandbox)
    else:
        parent = factories.ServiceFactory(parent=sandbox)
        service = factories.ServiceFactory(parent=parent)
    new_parent = factories.ServiceFactory(owner=staff_factory())
    factories.ServiceMemberFactory(service=new_parent, staff=new_parent.owner, role=owner_role)
    staff = staff_factory()
    client.login(staff.login)
    response = client.json.post(
        reverse('services-api:moves-list'),
        {
            'service': service.id,
            'destination': new_parent.id,
        }
    )
    assert response.status_code == 200
    move_request = ServiceMoveRequest.objects.get()
    assert move_request.state == ServiceMoveRequest.REQUESTED
    client.login(new_parent.owner.login)
    with patch('plan.services.tasks.move_service'):
        response = client.json.post(
            reverse('services-api:moves-approve', args=[move_request.id])
        )
    assert response.status_code == 204
    move_request.refresh_from_db()
    assert move_request.state == ServiceMoveRequest.APPROVED
    assert move_request.approver_outgoing == robot


@pytest.mark.parametrize('is_superuser', [True, False])
def test_move_to_base_non_leaf_service(client, data, is_superuser):
    factories.ServiceTagFactory(slug=settings.BASE_TAG_SLUG)
    data.other_service.is_base = True
    data.other_service.save()
    data.service.is_base = False
    data.service.save()
    data.stranger.user.is_superuser = is_superuser
    data.stranger.user.save()
    data.service.parent.is_base = True
    data.service.parent.save()
    factories.ServiceFactory(is_base=True, parent=data.other_service)

    client.login(data.stranger.login)
    with patch('plan.services.tasks.move_service'):
        response = client.json.post(
            reverse('services-api:moves-list'),
            {
                'service': data.service.id,
                'destination': data.other_service.id
            }
        )

    if is_superuser:
        assert response.status_code == 200
    else:
        assert response.status_code == 403
        assert response.json()['error']['code'] == 'Cannot move non base service to base non leaf destination'


@pytest.mark.parametrize('api', ('services-api', 'api-frontend'))
def test_move_oebs_service_to_oebs_unrelated_subtree_fails(client, api, data, oebs_related_service):
    client.login(data.owner_of_service.staff.login)
    destination = factories.ServiceFactory()
    response = client.json.post(
        reverse(f'{api}:moves-list'),
        {
            'service': oebs_related_service.id,
            'destination': destination.id
        }
    )
    assert response.status_code == 400
    assert (
        response.json()['error']['title']['en'] ==
        'Can\'t move OEBS service to OEBS unrelated subtree'
    )
    oebs_related_service.refresh_from_db()
    assert oebs_related_service.parent == data.metaservice
    assert oebs_related_service.oebs_agreements.count() == 0


@pytest.mark.parametrize('api', ('services-api', 'api-frontend'))
@pytest.mark.parametrize('tag_current_side', (None, 'service', 'parent'))
@pytest.mark.parametrize('tag_target_side', (None, 'service'))
def test_billing_point_service(client, api, data, tag_current_side, tag_target_side):
    client.login(data.owner_of_service.staff.login)

    tag = factories.ServiceTagFactory(slug=settings.OEBS_BILLING_AGGREGATION_TAG)
    destination = factories.ServiceFactory()

    if tag_current_side == 'service':
        data.service.tags.add(tag)
    elif tag_current_side == 'parent':
        data.service.parent.tags.add(tag)

    if tag_target_side == 'service':
        destination.tags.add(tag)

    response = client.json.post(
        reverse(f'{api}:moves-list'),
        {
            'service': data.service.id,
            'destination': destination.id
        }
    )
    expected = 200
    if tag_current_side == 'parent' and not tag_target_side:
        expected = 403
    assert response.status_code == expected


@pytest.mark.parametrize('api', ('services-api', 'api-frontend'))
@pytest.mark.parametrize('has_available_type', (True, False))
@pytest.mark.parametrize('switch_active', (True, False))
def test_check_allowed_parent(client, api, data, has_available_type, switch_active):
    client.login(data.owner_of_service.staff.login)

    service_type = factories.ServiceTypeFactory()
    destination = factories.ServiceFactory(service_type=service_type)

    if has_available_type:
        data.service.service_type.available_parents.add(service_type)

    with override_switch(settings.SWITCH_CHECK_ALLOWED_PARENT_TYPE, active=switch_active):
        response = client.json.post(
            reverse(f'{api}:moves-list'),
            {
                'service': data.service.id,
                'destination': destination.id
            }
        )
    expected = 200
    if not has_available_type and switch_active:
        expected = 400
    assert response.status_code == expected


@pytest.mark.parametrize('oebs_agreement', ['child', 'service', 'metaservice'], indirect=True)
@pytest.mark.parametrize('api', ('services-api', 'api-frontend'))
def test_move_oebs_service_fails(client, data, api, oebs_agreement):
    """Запрещать перемещать OEBS сервис, если у его предка, потомка или его самого есть активное OEBSAgreement"""
    client.login(data.owner_of_service.staff.login)
    response = client.json.post(
        reverse(f'{api}:moves-list'),
        {
            'service': data.service.id,
            'destination': data.other_service.id
        }
    )

    assert response.status_code == 400
    assert (
        response.json()['error']['title']['en'] ==
        'Action is impossible because active agreement already exists.'
    )
    data.service.refresh_from_db()
    oebs_agreement.service.refresh_from_db()
    assert data.service.parent == data.metaservice
    assert not data.service.oebs_agreements.exists() or data.service == oebs_agreement.service
    assert oebs_agreement.service.oebs_agreements.count() == 1


@override_settings(RESTRICT_OEBS_SUBTREE=True)
@pytest.mark.parametrize('api', ('services-api', 'api-frontend'))
def test_move_oebs_service_fails_outside_allowed_subtree(client, data, api, oebs_related_service):
    client.login(data.owner_of_service.staff.login)

    factories.ServiceResourceFactory(
        service=data.other_service,
        resource=Resource.objects.filter(
            type__code=settings.OEBS_PRODUCT_RESOURCE_TYPE_CODE
        ).first()
    )

    response = client.json.post(
        reverse(f'{api}:moves-list'),
        {
            'service': oebs_related_service.id,
            'destination': data.other_service.id
        }
    )

    assert response.status_code == 400
    assert (
        response.json()['error']['title']['en'] ==
        'Moving OEBS-related services from allowed OEBS subtree is restricted'
    )
    data.service.refresh_from_db()
    assert data.service.parent == data.metaservice


@pytest.mark.parametrize('api', ('services-api', 'api-frontend'))
def test_move_service_fails_if_child_has_active_oebs_agreement(client, data, api):
    """Запрещать перемещать сервис, если у потомка или его самого есть активное OEBSAgreement"""
    factories.OEBSAgreementFactory(service=data.child, action=ACTIONS.CHANGE_FLAGS)

    client.login(data.owner_of_service.staff.login)
    response = client.json.post(
        reverse(f'{api}:moves-list'),
        {
            'service': data.service.id,
            'destination': data.other_service.id
        }
    )

    assert response.status_code == 400
    assert (
        response.json()['error']['title']['en'] ==
        'Action is impossible because active agreement already exists.'
    )
    data.service.refresh_from_db()
    data.child.refresh_from_db()
    assert data.service.parent == data.metaservice
    assert not data.service.oebs_agreements.exists()
    assert data.child.oebs_agreements.count() == 1


def test_proper_person_serialization(client, data, moves):
    response = client.json.get(
        reverse('services-api:moves-detail', args=[moves.completed.id]),
    )
    assert response.status_code == 200

    move_data = response.json()
    assert move_data['requester']['login'] == moves.completed.requester.login
    assert move_data['approver_incoming']['login'] == data.big_boss.login
    assert move_data['approver_outgoing']['login'] == data.big_boss.login


def test_proper_state_serialization(client, data, moves):
    response = client.json.get(
        reverse('services-api:moves-detail', args=[moves.completed.id]),
    )
    assert response.status_code == 200

    move_data = response.json()
    assert move_data['state'] == moves.completed.state
    assert move_data['state_display']['en'] == moves.completed.state.title()
    assert move_data['state_display']['ru'] == moves.completed.get_state_display()


def test_filter_active(client, moves):
    response = client.json.get(
        reverse('services-api:moves-list'),
        {'kind': 'active'}
    )
    assert response.status_code == 200
    assert response.json()['results'][0]['id'] == moves.requested.id


def test_filter_inactive(client, moves):
    response = client.json.get(
        reverse('services-api:moves-list'),
        {'kind': 'inactive'}
    )
    assert response.status_code == 200
    assert response.json()['count'] == 2
    assert {move['id'] for move in response.json()['results']} == {moves.completed.id, moves.rejected.id}


def test_filter_by_requester(client, moves):
    response = client.json.get(
        reverse('services-api:moves-list'),
        {'requester': moves.completed.requester.login}
    )
    assert response.status_code == 200
    assert response.json()['count'] == 2

    response = client.json.get(
        reverse('services-api:moves-list'),
        {'requester': moves.rejected.requester.login}
    )
    assert response.status_code == 200
    assert response.json()['count'] == 1
    assert response.json()['results'][0]['id'] == moves.rejected.id


def test_request_move(client, data):

    client.login(data.stranger.login)
    response = client.json.post(
        reverse('services-api:moves-list'),
        {
            'service': data.service.id,
            'destination': data.other_service.id
        }
    )
    assert response.status_code == 200

    returned_move_request = response.json()
    assert ServiceMoveRequest.objects.filter(id=returned_move_request['id']).exists()


def test_fry_paradox(client, data):
    response = client.json.post(
        reverse('services-api:moves-list'),
        {
            'service': data.service.id,
            'destination': data.child.id
        }
    )
    assert response.status_code == 400

    assert response.json()['error']['code'] == 'fry_paradox'


def test_request_move_nonexistent_service(client, data):
    response = client.json.post(
        reverse('services-api:moves-list'),
        {
            'service': 100500,
            'destination': data.other_service.id
        }
    )
    assert response.status_code == 400

    error = response.json()['error']
    assert error['code'] == 'validation_error'
    assert 'object does not exist' in error['extra']['service'][0]


def test_request_move_inactive_service(client, data):
    data.service.state = Service.states.CLOSED
    data.service.save()
    response = client.json.post(
        reverse('services-api:moves-list'),
        {
            'service': data.service.id,
            'destination': data.other_service.id
        }
    )
    assert response.status_code == 400

    error = response.json()['error']
    assert error['code'] == 'validation_error'
    assert error['extra']['service'] == [
        'Мы не можем выполнить это действие над сервисом в неактивном статусе'
    ]


def test_request_same_parent(client, moves, data):
    response = client.json.post(
        reverse('services-api:moves-list'),
        {
            'service': data.service.id,
            'destination': data.service.parent.id,
        }
    )
    assert response.status_code == 400
    assert response.json()['error']['message'] == {
        'ru': 'Сервис уже находится в месте назначения',
        'en': 'Service is already at this destination',
    }


def test_approve_move(client, moves, data):
    client.login(data.big_boss.login)
    with patch('plan.services.tasks.move_service') as move_service:
        response = client.json.post(
            reverse('services-api:moves-approve', args=[moves.requested.id])
        )

    assert response.status_code == 204
    assert move_service.apply_async.called

    moves.requested.refresh_from_db()
    assert moves.requested.state == ServiceMoveRequest.APPROVED
    assert moves.requested.approver_incoming == moves.requested.approver_outgoing == data.big_boss


def test_approve_move_400(client, moves, data):
    client.login(data.big_boss.login)
    response = client.json.post(
        reverse('services-api:moves-reject', args=[moves.completed.id])
    )

    assert response.status_code == 400
    assert response.json()['error']['message'] == {
        'ru': 'Решение по этому запросу уже было принято',
        'en': 'This request was already processed',
    }


def test_reject_move(client, moves, data):
    client.login(data.big_boss.login)
    response = client.json.post(
        reverse('services-api:moves-reject', args=[moves.requested.id])
    )

    assert response.status_code == 204

    moves.requested.refresh_from_db()
    assert moves.requested.state == ServiceMoveRequest.REJECTED


def test_reject_move_400(client, moves, data):
    client.login(data.big_boss.login)
    response = client.json.post(
        reverse('services-api:moves-reject', args=[moves.completed.id])
    )

    assert response.status_code == 400
    assert response.json()['error']['message'] == {
        'ru': 'Решение по этому запросу уже было принято',
        'en': 'This request was already processed',
    }


def test_ask_permissions_for_big_boss(client, moves, data):
    client.login(data.big_boss.login)
    response = client.json.options(
        reverse('services-api:moves-reject', args=[moves.requested.id])
    )

    assert response.status_code == 200
    assert response.json()['permissions'] == ['can_approve', 'can_cancel']


def test_ask_permissions_for_stranger(client, moves, data):
    client.login(data.stranger.login)
    response = client.json.options(
        reverse('services-api:moves-reject', args=[moves.requested.id])
    )

    assert response.status_code == 200
    assert response.json()['permissions'] == ['can_cancel']


def test_ask_permissions_for_stranger_move_in_progress(client, moves, data):
    client.login(data.big_boss.login)
    with patch('plan.services.tasks.move_service'):
        response = client.json.post(
            reverse('services-api:moves-approve', args=[moves.requested.id])
        )

    moves.requested.refresh_from_db()
    assert moves.requested.state == ServiceMoveRequest.APPROVED

    client.login(data.stranger.login)
    response = client.json.options(
        reverse('services-api:moves-reject', args=[moves.requested.id])
    )

    assert response.status_code == 200
    assert response.json()['permissions'] == []


def test_actions_stranger_can_reject_requested(client, moves, data):
    client.login(data.stranger.login)

    response = client.json.get(
        reverse('services-api:moves-detail', args=[moves.requested.id])
    )
    assert response.status_code == 200

    assert response.json()['actions'] == ['reject']


def test_actions_stranger_cant_reject_approved(client, moves, data):
    client.login(data.stranger.login)

    moves.requested._approve_transition(Person(data.big_boss))
    moves.requested.save()

    response = client.json.get(
        reverse('services-api:moves-detail', args=[moves.requested.id])
    )
    assert response.status_code == 200

    assert response.json()['actions'] == []


def test_actions_big_boss_can_do_as_he_pleases(client, moves, data):
    client.login(data.big_boss.login)

    response = client.json.get(
        reverse('services-api:moves-detail', args=[moves.requested.id])
    )
    assert response.status_code == 200

    assert set(response.json()['actions']) == {'approve', 'reject'}


def test_approvers_list(client, moves, data):
    response = client.json.get(
        reverse('services-api:moves-approvers', args=[moves.requested.id])
    )
    assert response.status_code == 200

    json = response.json()

    expected_result = [approver['login'] for approver in json['service']['recommended']]
    result = (
        moves.requested.service.members
        .responsibles()
        .values_list('staff__login', flat=True)
        .distinct()
    )
    assert iterables_are_equal(expected_result, result)
    assert [approver['login'] for approver in json['service']['other']] == [data.big_boss.login]

    assert [approver['login'] for approver in json['destination']['recommended']] == [data.other_staff.login]
    assert [approver['login'] for approver in json['destination']['other']] == [data.big_boss.login]


@pytest.mark.parametrize('endpoint', ['services-api:moves-approvers', 'api-v4:moves-approvers'])
def test_approvers_list_no_immediate_responsibles(client, data, endpoint):
    service = factories.ServiceFactory()
    move_request = factories.ServiceMoveRequestFactory(
        service=service,
        destination=data.child
    )
    response = client.json.get(
        reverse(endpoint, args=[move_request.id])
    )
    assert response.status_code == 200

    json = response.json()

    assert json['service']['recommended'] == []
    assert json['service']['other'] == []

    expected_result = [approver['login'] for approver in json['destination']['recommended']]
    result = (
        data.child.members
        .responsibles()
        .values_list('staff__login', flat=True)
        .distinct()
    )
    assert iterables_are_equal(expected_result, result)

    expected_result = [approver['login'] for approver in json['destination']['other']]
    result = (
        ServiceMember.objects
        .filter(service__in=data.child.get_ancestors())
        .responsibles()
        .order_by('service__level')
        .values_list('staff__login', flat=True)
        .distinct()
    )
    assert iterables_are_equal(expected_result, result)


def test_recommended_approvers_do_not_overlap_with_other_approvers(client, data, responsible_role):
    service = factories.ServiceFactory()
    move_request = factories.ServiceMoveRequestFactory(
        service=service,
        destination=data.child
    )
    factories.ServiceMemberFactory(
        service=data.metaservice,
        staff=data.owner_of_child.staff,
        role=responsible_role
    )

    response = client.json.get(
        reverse('services-api:moves-approvers', args=[move_request.id])
    )
    assert response.status_code == 200

    json = response.json()

    recommended_approver_logins = [approver['login'] for approver in json['destination']['recommended']]
    assert data.owner_of_child.staff.login in recommended_approver_logins

    other_approver_logins = [approver['login'] for approver in json['destination']['other']]
    assert data.owner_of_child.staff.login not in other_approver_logins


def test_no_duplicate_approvers(client, data, responsible_role):
    service = factories.ServiceFactory()
    move_request = factories.ServiceMoveRequestFactory(
        service=service,
        destination=data.child
    )
    factories.ServiceMemberFactory(
        service=data.metaservice,
        staff=data.responsible.staff,
        role=responsible_role
    )

    response = client.json.get(
        reverse('services-api:moves-approvers', args=[move_request.id])
    )
    assert response.status_code == 200

    json = response.json()

    approver_logins = [approver['login'] for approver in json['destination']['other']]
    assert approver_logins.count(data.responsible.staff.login) == 1


@pytest.mark.parametrize('api', ['services-api', 'api-frontend', 'api-v4'])
def test_restrict_move_to_junk(client, api, defaultmeta, service,):
    response = client.json.post(
        reverse(f'{api}:moves-list'),
        {'service': service.id, 'destination': defaultmeta.id}
    )
    assert response.status_code == 400
    assert response.json()['error']['message']['en'] == 'Move to Junk restricted'


@patch('plan.api.idm.actions.assert_service_node_exists', Mock())
def test_move_disables_inheritance(client, data):
    new_parent = factories.ServiceFactory(membership_inheritance=True,)
    new_child = factories.ServiceFactory(parent=new_parent, membership_inheritance=True,)
    parent_staff = factories.StaffFactory()
    child_staff = factories.StaffFactory()
    factories.ServiceMemberFactory(service=new_parent, staff=parent_staff)
    factories.ServiceMemberFactory(service=new_child, staff=child_staff)

    data.service.membership_inheritance = True
    data.service.save()

    move_request = factories.ServiceMoveRequestFactory(
        service=data.service, destination=new_parent, state='approved')
    with patch('plan.api.idm.actions.move_service'):
        tasks.move_service(move_request.id)

    new_parent.refresh_from_db()
    data.service.refresh_from_db()

    all_members = (
        ServiceMember.objects.filter(
            service__in=new_parent.get_descendants(include_self=True)
        ).team().count()
    )

    service_members = (
        ServiceMember.objects.filter(
            service__in=data.service.get_descendants(include_self=True)
        ).team().count()
    )

    response = client.json.get(
        reverse('services-api:member-list'),
        {
            'service__with_descendants': new_parent.id,
            'use_inheritance_settings': 'True'
        }
    )
    assert response.status_code == 200
    assert response.json()['count'] == all_members - service_members
