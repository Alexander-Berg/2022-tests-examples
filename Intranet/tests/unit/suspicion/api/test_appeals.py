import pytest
from django.core.urlresolvers import reverse
from django.utils import timezone

from plan.cabinet.serializers import StaffSerializer, ServiceSerializer
from plan.common.utils.dates import datetime_isoformat_with_microseconds
from plan.roles.models import Role
from plan.services.models import Service
from plan.suspicion.api.serializers import ServiceIssueSerializer
from plan.suspicion.models import ServiceAppealIssue
from common import factories

pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('api', ['api-v3', 'api-v4'])
@pytest.mark.parametrize('is_superuser', [True, False])
def test_appeals_get(client, api, is_superuser, staff_factory):
    requester = staff_factory()
    client.login(requester)
    appeal = factories.ServiceAppealIssueFactory(requester=requester)

    service = appeal.service_issue.service
    parent_service = factories.ServiceFactory()
    factories.ServiceAppealIssueFactory(service_issue=factories.ServiceIssueFactory(service=parent_service))
    service.parent = parent_service
    service.save()

    approved_issue = factories.ServiceIssueFactory(service=service)
    approved_appeal = factories.ServiceAppealIssueFactory(
        state=ServiceAppealIssue.STATES.APPROVED,
        service_issue=approved_issue
    )

    child_appeal = factories.ServiceAppealIssueFactory()
    child_service = child_appeal.service_issue.service
    child_service.parent = service
    child_service.save()

    responsible_role = factories.RoleFactory(code=Role.RESPONSIBLE)
    factories.ServiceMemberFactory(role=responsible_role, service=service, staff=requester)

    child_role = factories.RoleFactory(code=Role.DEPUTY_OWNER)
    factories.ServiceMemberFactory(role=child_role, service=child_service)

    client.login(requester.login)
    fields = ['id', 'message', 'approvers', 'created_at', 'requester', 'issue', 'service', 'user_can_approve', 'state']
    response = client.json.get(
        reverse(api + ':appeal-list'),
        {'only_mine': True, 'kind': 'active', 'fields': ','.join(fields)},
    )
    assert response.status_code == 200
    results = response.json()['results']
    assert len(results) == 1
    assert results[0] == {
        'id': child_appeal.id,
        'message': child_appeal.message,
        'approvers': StaffSerializer([requester, ], many=True).data,
        'created_at': datetime_isoformat_with_microseconds(child_appeal.created_at),
        'requester': StaffSerializer(child_appeal.requester).data,
        'issue': ServiceIssueSerializer(child_appeal.service_issue).data,
        'service': ServiceSerializer(child_service).data,
        'user_can_approve': True,
        'state': child_appeal.state,
    }

    response = client.json.get(
        reverse(api + ':appeal-list'),
        {'service': service.id, 'kind': 'active', 'fields': ','.join(fields)},
    )
    results = response.json()['results']
    assert len(results) == 1
    assert results[0]['message'] == appeal.message

    response = client.json.get(
        reverse(api + ':appeal-list'),
        {'service': service.id, 'kind': 'inactive', 'fields': ','.join(fields)},
    )
    results = response.json()['results']
    assert len(results) == 1
    assert results[0]['message'] == approved_appeal.message

    staff = staff_factory()
    staff.user.is_superuser = is_superuser
    staff.user.save()
    client.login(staff.login)
    response = client.json.get(
        reverse(api + ':appeal-list'),
        {'service': service.id, 'kind': 'active', 'fields': ','.join(fields)},
    )
    if is_superuser:
        assert response.json()['results'][0]['user_can_approve']
    else:
        assert not response.json()['results'][0]['user_can_approve']


@pytest.mark.parametrize('api', ['api-v3', 'api-v4'])
def test_appeals_filter_hierarchy(client, api, owner_role, staff_factory):
    parent_service = factories.ServiceFactory(owner=staff_factory())
    factories.ServiceAppealIssue(service_issue=factories.ServiceIssueFactory(service=parent_service))
    factories.ServiceMemberFactory(
        role=owner_role,
        staff=parent_service.owner,
        service=parent_service,
    )
    child_service = factories.ServiceFactory(parent=parent_service)
    factories.ServiceFactory(parent=parent_service, state=Service.states.CLOSED)
    descendant_service = factories.ServiceFactory(parent=child_service)
    closed_descendant_service = factories.ServiceFactory(parent=child_service, state=Service.states.CLOSED)

    factories.ServiceAppealIssueFactory(
        service_issue=factories.ServiceIssueFactory(service=parent_service),
    )
    appeal_for_child = factories.ServiceAppealIssueFactory(
        service_issue=factories.ServiceIssueFactory(service=child_service),
    )
    appeal_for_descendant = factories.ServiceAppealIssueFactory(
        service_issue=factories.ServiceIssueFactory(service=descendant_service)
    )
    factories.ServiceAppealIssueFactory(
        service_issue=factories.ServiceIssueFactory(service=closed_descendant_service)
    )

    client.login(parent_service.owner.login)
    response = client.json.get(
        reverse(api + ':appeal-list'),
        {'only_mine': True, 'kind': 'active'},
    )
    assert {appeal['id'] for appeal in response.json()['results']} == {appeal_for_child.id, appeal_for_descendant.id}
    response = client.json.get(
        reverse(api + ':appeal-list'),
        {'only_mine': 'hierarchy', 'kind': 'active'},
    )
    assert {appeal['id'] for appeal in response.json()['results']} == {appeal_for_child.id, appeal_for_descendant.id}
    response = client.json.get(
        reverse(api + ':appeal-list'),
        {'only_mine': 'direct', 'kind': 'active'},
    )
    assert {appeal['id'] for appeal in response.json()['results']} == {appeal_for_child.id}
    response = client.json.get(
        reverse(api + ':appeal-list'),
        {'kind': 'active'},
    )
    all_active = set(ServiceAppealIssue.objects.active().values_list('id', flat=True))
    assert {appeal['id'] for appeal in response.json()['results']} == all_active


@pytest.mark.parametrize('api', ['api-v3', 'api-v4'])
def test_appeals_filter_state(client, data, api):
    client.login(data.staff.login)
    appeal_1 = factories.ServiceAppealIssueFactory(state=ServiceAppealIssue.STATES.REQUESTED)
    factories.ServiceAppealIssueFactory(state=ServiceAppealIssue.STATES.APPROVED)
    factories.ServiceAppealIssueFactory(state=ServiceAppealIssue.STATES.REJECTED)
    factories.ServiceAppealIssueFactory(state=ServiceAppealIssue.STATES.REQUESTED)

    response = client.json.get(
        reverse(api + ':appeal-list'),
        {'state': ServiceAppealIssue.STATES.REQUESTED},
    )
    assert response.status_code == 200
    results = response.json()['results']
    assert len(results) == 2
    assert set(appeal['state'] for appeal in results) == {appeal_1.state}


@pytest.mark.parametrize('api', ['api-v3', 'api-v4'])
@pytest.mark.parametrize('responsible', [True, False])
def test_appeals_post(client, data, api, responsible):
    """
    Если запрашивающий является управляющим вышестоящего сервиса,
    то должен происходит автоапрув апелляции.
    """

    if responsible:
        client.login(data.big_boss.login)
        issue = factories.ServiceIssueFactory(service=data.service)
    else:
        client.login(data.staff.login)
        issue = factories.ServiceIssueFactory()
    response = client.json.post(
        reverse(api + ':appeal-list'),
        {'issue': issue.id, 'message': 'appeal'}
    )
    assert response.status_code == 201
    appeal = ServiceAppealIssue.objects.get()
    assert appeal.service_issue == issue
    assert appeal.state == (ServiceAppealIssue.STATES.APPROVED if responsible else ServiceAppealIssue.STATES.REQUESTED)
    assert appeal.requester == (data.big_boss if responsible else data.staff)
    assert appeal.message == 'appeal'


@pytest.mark.parametrize('api', ['api-v3', 'api-v4'])
@pytest.mark.parametrize('after_reject', [False, True])
@pytest.mark.parametrize('is_superuser', [False, True])
def test_appeals_post_twice(client, data, api, after_reject, is_superuser):
    data.staff.user.is_superuser = is_superuser
    data.staff.user.save()
    client.login(data.staff.login)
    issue = factories.ServiceIssueFactory()

    response = client.json.post(
        reverse(api + ':appeal-list'),
        {'issue': issue.id, 'message': 'appeal'}
    )
    assert response.status_code == 201
    appeal = ServiceAppealIssue.objects.get()
    assert appeal.service_issue == issue

    response = client.json.post(
        reverse(api + ':appeal-list'),
        {'issue': issue.id, 'message': 'appeal'}
    )
    assert response.status_code == 400

    if after_reject:
        appeal.state = 'rejected'
        appeal.rejected_at = timezone.now()
        appeal.rejecter = data.staff
        appeal.save()

    response = client.json.post(
        reverse(api + ':appeal-list'),
        {'issue': issue.id, 'message': 'appeal'}
    )
    assert response.status_code == (201 if after_reject else 400)
    assert ServiceAppealIssue.objects.count() == (2 if after_reject else 1)


@pytest.mark.parametrize('api', ['api-v3', 'api-v4'])
def test_appeals_owner_service(client, data, api):
    """
    Если запрашивающий является управляющим своего сервиса,
    и не является управляющим вышестоящего, то автоапрува быть не должно.
    """

    client.login(data.owner_of_service.staff.login)
    issue = factories.ServiceIssueFactory(service=data.service)

    response = client.json.post(
        reverse(api + ':appeal-list'),
        {'issue': issue.id, 'message': 'appeal'}
    )
    assert response.status_code == 201
    appeal = ServiceAppealIssue.objects.get()
    assert appeal.service_issue == issue
    assert appeal.state == ServiceAppealIssue.STATES.REQUESTED


@pytest.mark.parametrize('api', ['api-v3', 'api-v4'])
@pytest.mark.parametrize('permission', [False, 'superuser', 'owner'])
def test_approve(client, api, permission, owner_role, staff_factory):
    approver = staff_factory()
    appeal = factories.ServiceAppealIssueFactory()
    if permission == 'superuser':
        approver.user.is_superuser = True
        approver.user.save()
    elif permission == 'owner':
        grand_parent = factories.ServiceFactory(owner=approver)
        parent = factories.ServiceFactory(parent=grand_parent)
        factories.ServiceMemberFactory(service=grand_parent, role=owner_role, staff=approver)
        appeal.service_issue.service.parent = parent
        appeal.service_issue.service.save()

    client.login(approver.login)
    response = client.json.post(
        reverse(api + ':appeal-approve', args=[appeal.id])
    )
    if permission:
        assert response.status_code == 204
        appeal.refresh_from_db()
        assert appeal.approver == approver
        assert appeal.state == ServiceAppealIssue.STATES.APPROVED
    else:
        assert response.status_code == 403


@pytest.mark.parametrize('api', ['api-v3', 'api-v4'])
def test_approve_400(client, api, staff_factory):
    appeal = factories.ServiceAppealIssueFactory(state=ServiceAppealIssue.STATES.APPROVED)
    staff = staff_factory()
    staff.user.is_superuser = True
    staff.user.save()
    client.login(staff.login)
    response = client.json.post(
        reverse(api + ':appeal-approve', args=[appeal.id])
    )
    assert response.status_code == 400
    assert response.json()['error']['message'] == {
        'ru': 'Решение по этой апелляции уже было принято',
        'en': 'This appeal was already processed',
    }


@pytest.mark.parametrize('api', ['api-v3', 'api-v4'])
@pytest.mark.parametrize('permission', [False, 'superuser', 'owner'])
def test_reject(client, api, permission, owner_role, staff_factory):
    rejecter = staff_factory()
    appeal = factories.ServiceAppealIssueFactory()
    if permission == 'superuser':
        rejecter.user.is_superuser = True
        rejecter.user.save()
    elif permission == 'owner':
        parent = factories.ServiceFactory(owner=rejecter)
        factories.ServiceMemberFactory(service=parent, role=owner_role, staff=rejecter)
        appeal.service_issue.service.parent = parent
        appeal.service_issue.service.save()
    client.login(rejecter.login)
    response = client.json.post(
        reverse(api + ':appeal-reject', args=[appeal.id])
    )

    if permission:
        assert response.status_code == 204
        appeal.refresh_from_db()
        assert appeal.rejecter == rejecter
        assert appeal.state == ServiceAppealIssue.STATES.REJECTED
    else:
        assert response.status_code == 403


@pytest.mark.parametrize('api', ['api-v3', 'api-v4'])
def test_reject_400(client, api, staff_factory):
    appeal = factories.ServiceAppealIssueFactory(state=ServiceAppealIssue.STATES.REJECTED)
    staff = staff_factory()
    staff.user.is_superuser = True
    staff.user.save()
    client.login(staff.login)
    response = client.json.post(
        reverse(api + ':appeal-reject', args=[appeal.id])
    )
    assert response.status_code == 400
    assert response.json()['error']['message'] == {
        'ru': 'Решение по этой апелляции уже было принято',
        'en': 'This appeal was already processed',
    }
