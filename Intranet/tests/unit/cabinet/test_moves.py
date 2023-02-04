import pytest
from django.core.urlresolvers import reverse

from plan.roles.models import Role
from common import factories

pytestmark = pytest.mark.django_db


def test_moves_200(client, move_request):
    url = reverse('cabinet-api:cabinet_requested_moves')
    client.login(move_request.service.owner.login)
    response = client.get(url)
    assert response.status_code == 200


def test_moves_meta(client, move_request):
    url = reverse('cabinet-api:cabinet_requested_moves')
    client.login(move_request.service.owner.login)
    response = client.json.get(url)
    response_json = response.json()

    assert response_json['meta']['count'] == len(response_json['objects']) == 1


def test_moves_serialization(client, move_request):
    url = reverse('cabinet-api:cabinet_requested_moves')
    client.login(move_request.service.owner.login)
    response = client.json.get(url)
    response_json = response.json()

    assert response_json['objects'][0]['service']['id'] == move_request.service.id
    assert response_json['objects'][0]['destination']['id'] == move_request.destination.id


def make_service(staff_factory):
    owner = staff_factory('full_access')
    service = factories.ServiceFactory(owner=owner)
    factories.ServiceMemberFactory(service=service, staff=service.owner, role=Role.get_responsible())
    return service


def test_approve_as_owner(client, staff_factory):
    service1 = make_service(staff_factory)
    service2 = make_service(staff_factory)

    mr = factories.ServiceMoveRequestFactory(
        service=service1,
        destination=service2,
        approver_incoming=service2.owner,
    )

    client.login(service1.owner.login)
    response = client.json.get(reverse('cabinet-api:cabinet_requested_moves'))
    response_json = response.json()

    assert len(response_json['objects']) == 1
    assert response_json['objects'][0]['id'] == mr.id
    assert response_json['objects'][0]['actions'] == ['approve_as_outgoing', 'decline_as_outgoing']


def test_approve_as_receiver(client, staff_factory):
    service1 = make_service(staff_factory)
    service2 = make_service(staff_factory)

    mr = factories.ServiceMoveRequestFactory(
        service=service1,
        destination=service2,
        approver_outgoing=service1.owner,
    )

    client.login(service2.owner.login)
    response = client.json.get(reverse('cabinet-api:cabinet_requested_moves'))
    response_json = response.json()

    assert len(response_json['objects']) == 1
    assert response_json['objects'][0]['id'] == mr.id
    assert response_json['objects'][0]['actions'] == ['approve_as_incoming', 'decline_as_incoming']


def test_already_approved_by_sender(client, staff_factory):
    service1 = make_service(staff_factory)
    service2 = make_service(staff_factory)

    factories.ServiceMoveRequestFactory(
        service=service1,
        destination=service2,
        approver_outgoing=service1.owner,
    )

    client.login(service1.owner.login)
    response = client.json.get(reverse('cabinet-api:cabinet_requested_moves'))
    response_json = response.json()

    assert len(response_json['objects']) == 0


def test_already_approved_by_receiver(client, staff_factory):
    service1 = make_service(staff_factory)
    service2 = make_service(staff_factory)

    factories.ServiceMoveRequestFactory(
        service=service1,
        destination=service2,
        approver_incoming=service2.owner,
    )

    client.login(service2.owner.login)
    response = client.json.get(reverse('cabinet-api:cabinet_requested_moves'))
    response_json = response.json()

    assert len(response_json['objects']) == 0


def test_approve_both(client, staff_factory):
    staff = staff_factory('full_access')

    service1 = factories.ServiceFactory(owner=staff)
    factories.ServiceMemberFactory(service=service1, staff=staff, role=Role.get_responsible())
    service2 = factories.ServiceFactory(owner=staff)
    factories.ServiceMemberFactory(service=service2, staff=staff, role=Role.get_responsible())

    mr = factories.ServiceMoveRequestFactory(
        service=service1,
        destination=service2,
    )

    client.login(staff.login)
    response = client.json.get(reverse('cabinet-api:cabinet_requested_moves'))
    response_json = response.json()

    assert len(response_json['objects']) == 1
    assert response_json['objects'][0]['id'] == mr.id
    assert response_json['objects'][0]['actions'] == [
        'approve_as_outgoing', 'decline_as_outgoing', 'approve_as_incoming', 'decline_as_incoming'
    ]
