import pretend
import pytest
from rest_framework.reverse import reverse

from plan.resources.models import ServiceResource
from plan.resources.policies import APPROVE_POLICY
from common import factories

pytestmark = pytest.mark.django_db


@pytest.fixture
def oebs():
    resource_type = factories.ResourceTypeFactory(approve_policy=APPROVE_POLICY.SUPERUSER)
    resource = factories.ResourceFactory(type=resource_type)
    service = factories.ServiceFactory()
    service_resource = factories.ServiceResourceFactory(
        service=service,
        resource=resource,
        state=ServiceResource.REQUESTED
    )

    return pretend.stub(
        resource_type=resource_type,
        resource=resource,
        service=service,
        service_resource=service_resource,
    )


def test_get_oebs_product_resource(client, oebs):
    response = client.get(reverse('resources-api:resources-detail', [oebs.resource.id]))
    assert response.status_code == 200

    response = client.get(reverse('resources-api:resources-list'), {'type': oebs.resource_type.id})
    assert response.status_code == 200
    assert [r['id'] for r in response.json()['results']] == [oebs.resource.id]


def test_get_oebs_product_service_resource(client, oebs):
    response = client.get(reverse('resources-api:serviceresources-detail', [oebs.service_resource.id]))
    assert response.status_code == 200

    response = client.get(reverse('resources-api:serviceresources-list'))
    assert response.status_code == 200
    assert [r['id'] for r in response.json()['results']] == [oebs.service_resource.id]


@pytest.mark.parametrize('is_superuser', (False, True))
@pytest.mark.parametrize('owner', (False, True))
def test_edit_oebs_product_service_resource(client, oebs, is_superuser, owner, owner_role):
    staff = factories.StaffFactory(user=factories.UserFactory(is_superuser=is_superuser))
    client.login(staff.login)

    if owner:
        factories.ServiceMemberFactory(service=oebs.service, staff=staff, role=owner_role)

    response = client.json.patch(
        reverse('resources-api:serviceresources-detail', [oebs.service_resource.id]),
        {'state': ServiceResource.APPROVED}
    )
    assert response.status_code == 200 if is_superuser else 403


@pytest.mark.parametrize('is_superuser', (False, True))
@pytest.mark.parametrize('owner', (False, True))
def test_request_oebs_product_service_resource(client, oebs, is_superuser, owner, owner_role):
    staff = factories.StaffFactory(user=factories.UserFactory(is_superuser=is_superuser))
    client.login(staff.login)

    if owner:
        factories.ServiceMemberFactory(service=oebs.service, staff=staff, role=owner_role)

    resource = factories.ResourceFactory(type=oebs.resource_type)
    service = factories.ServiceFactory()
    response = client.json.post(
        reverse('resources-api:serviceresources-list'),
        {'service': service.id, 'resource': resource.id}
    )
    assert response.status_code == 201 if is_superuser else 400


@pytest.mark.parametrize('is_superuser', (False, True))
@pytest.mark.parametrize('owner', (False, True))
def test_request_oebs_product_resource(client, oebs, is_superuser, owner, owner_role):
    staff = factories.StaffFactory(user=factories.UserFactory(is_superuser=is_superuser))
    client.login(staff.login)

    if owner:
        factories.ServiceMemberFactory(service=oebs.service, staff=staff, role=owner_role)


@pytest.mark.parametrize('is_superuser', (False, True))
@pytest.mark.parametrize('owner', (False, True))
def test_request_oebs_product_resource_financial(client, oebs, is_superuser, owner, owner_role):
    staff = factories.StaffFactory(user=factories.UserFactory(is_superuser=is_superuser))
    client.login(staff.login)

    if owner:
        factories.ServiceMemberFactory(service=oebs.service, staff=staff, role=owner_role)

    service = factories.ServiceFactory()
    response = client.json.post(
        reverse('resources-api:financial-resources-list'),
        {'service': service.id, 'resource_type': oebs.resource_type.id, 'obj_id': 111}
    )
    assert response.status_code == 201 if is_superuser else 403
