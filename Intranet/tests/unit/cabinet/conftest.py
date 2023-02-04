import pytest
from mock import MagicMock, patch

from plan.resources.models import ServiceResource
from plan.roles.models import Role
from common import factories


@pytest.fixture
def service(db, staff_factory):
    owner = staff_factory('full_access')
    parent = factories.ServiceFactory(owner=owner)
    child = factories.ServiceFactory(parent=parent, owner=owner)
    factories.ServiceMemberFactory(
        service=parent,
        staff=owner,
        role=Role.get_responsible(),
    )
    factories.ServiceMemberFactory(
        service=child,
        staff=owner,
        role=Role.get_responsible(),
    )
    return child


@pytest.fixture
def role(db, service):
    return factories.RoleFactory(service=service)


@pytest.fixture
def move_request(db, service_with_owner, person):
    new_parent = factories.ServiceFactory(owner=person)
    move_request = factories.ServiceMoveRequestFactory(
        service=service_with_owner,
        destination=new_parent,
        requester=person,
    )

    return move_request


@pytest.fixture
def resources(db, service):
    resource_type = factories.ResourceTypeFactory(supplier=service)
    resource = factories.ResourceFactory(type=resource_type)
    service_resource = factories.ServiceResourceFactory(
        resource=resource, state=ServiceResource.APPROVED)

    factories.ServiceResourceFactory(
        resource=resource, state=ServiceResource.GRANTED)

    return service_resource


@pytest.fixture
def mock_idm(db, service, role, person, department):
    child_service = factories.ServiceFactory(parent=service)
    role_requests = [
        {
            'idm_request_id': 0,
            'requester': person.login,
            'user': person.login,
            'group': None,
            'service': service.slug,
            'role': f'{role.id}',
        },
        {
            'idm_request_id': 1,
            'requester': person.login,
            'user': None,
            'group': department.staff_id,
            'service': service.slug,
            'role': f'{role.id}',
        },
        {
            'idm_request_id': 2,
            'requester': person.login,
            'user': None,
            'group': department.staff_id,
            'service': child_service.slug,
            'role': f'{role.id}',
        }
    ]
    with patch('plan.idm.adapters.ApproveRequestManager.get_list',
               new=MagicMock(return_value=role_requests)) as mocked_idm:
        yield mocked_idm
