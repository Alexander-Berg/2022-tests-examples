import pretend
import pytest

from plan.common.person import Person
from plan.resources import permissions
from plan.resources.models import ServiceResource
from plan.resources.policies import BaseApprovePolicy, SupplierApproveOrOwnerRolePolicy

from common import factories


pytestmark = pytest.mark.django_db


@pytest.fixture
def permissions_data(owner_role):
    supplier = factories.ServiceFactory()
    rt = factories.ResourceTypeFactory(supplier=supplier, form_id='1111')

    rt.supplier_roles.add(owner_role)
    staff = factories.StaffFactory()
    service = factories.ServiceFactory()

    return pretend.stub(
        supplier=supplier,
        service=service,
        staff=staff,
        resource_type=rt
    )


def test_edit_by_stranger(permissions_data):
    resource = factories.ResourceFactory(type=permissions_data.resource_type)
    service_resource = factories.ServiceResourceFactory(resource=resource, service=permissions_data.service)
    resource_policy = BaseApprovePolicy.get_approve_policy_class(service_resource)(service_resource)
    assert permissions.can_edit_resource(Person(permissions_data.staff), resource_policy) is False


@pytest.mark.usefixtures('robot')
def test_approve_after_approve(permissions_data, owner_role):
    factories.ServiceMemberFactory(service=permissions_data.supplier, staff=permissions_data.staff, role=owner_role)
    permissions_data.resource_type.approve_policy = 'supplier'
    permissions_data.resource_type.save()
    resource = factories.ResourceFactory(type=permissions_data.resource_type)
    service_resource = factories.ServiceResourceFactory(resource=resource, service=permissions_data.service)
    person = Person(permissions_data.staff)
    assert permissions.can_approve_resource(person, service_resource) is True
    service_resource.approve(person)
    assert permissions.can_approve_resource(person, service_resource) is False


def test_edit_by_owner(permissions_data, owner_role):
    resource = factories.ResourceFactory(type=permissions_data.resource_type)
    service_resource = factories.ServiceResourceFactory(resource=resource, service=permissions_data.service)
    factories.ServiceMemberFactory(
        staff=permissions_data.staff,
        service=permissions_data.service,
        role=owner_role,
    )
    resource_policy = BaseApprovePolicy.get_approve_policy_class(service_resource)(service_resource)
    assert permissions.can_edit_resource(Person(permissions_data.staff), resource_policy) is False
    permissions_data.resource_type.consumer_roles.add(owner_role)
    assert permissions.can_edit_resource(Person(permissions_data.staff), resource_policy) is True


@pytest.mark.parametrize('policy_class, can_edit, can_delete', [
    (BaseApprovePolicy, False, False),
    (SupplierApproveOrOwnerRolePolicy, True, True),
])
def test_edit_by_parent_service_member(service, staff_factory, role,
                                       yp_quota_resource_type, yp_quota_service_resource_data,
                                       policy_class, can_edit, can_delete):
    """
        Сотрудник родительского сервиса может изменять ресурс дочернего сервиса,
        если для типа ресурса выбрана политика SupplierApproveOrOwnerRolePolicy
        и роль сотрудника входит в owner_roles этого типа ресурса.
    """
    service_resource = yp_quota_service_resource_data.service_resource
    yp_quota_resource_type.owner_roles.add(role)
    parent_service = service.parent
    parent_member = staff_factory()
    factories.ServiceMemberFactory(staff=parent_member, service=parent_service, role=role)

    resource_policy = policy_class(service_resource)
    assert permissions.can_edit_resource(Person(parent_member), resource_policy) is can_edit
    assert permissions.can_delete_resource(Person(parent_member), resource_policy) is can_delete


def test_request_by_supplier(permissions_data, owner_role):
    """Ресурс может запросить поставщик с определенной ролью
    """

    service = factories.ServiceFactory()
    scope = factories.RoleScopeFactory(slug='development')
    dev = factories.RoleFactory(scope=scope, service=service)
    factories.ServiceMemberFactory(
        staff=permissions_data.staff,
        service=permissions_data.resource_type.supplier,
        role=dev,
    )
    resource_type = permissions_data.resource_type

    assert permissions.can_request_resource(
        Person(permissions_data.staff),
        permissions_data.service,
        resource_type
    ) is False

    factories.ServiceMemberFactory(
        staff=permissions_data.staff,
        service=permissions_data.resource_type.supplier,
        role=owner_role
    )

    assert permissions.can_request_resource(
        Person(permissions_data.staff),
        permissions_data.service,
        permissions_data.resource_type
    ) is True


def test_edit_by_supplier(permissions_data, owner_role):
    """Ресурс может быть изменен поставшиком с определенной ролью
    """
    scope = factories.RoleScopeFactory(slug='development')
    dev = factories.RoleFactory(scope=scope)
    factories.ServiceMemberFactory(
        staff=permissions_data.staff,
        service=permissions_data.resource_type.supplier,
        role=dev,
    )
    service_resource = factories.ServiceResourceFactory(
        state=ServiceResource.GRANTED,
        resource=factories.ResourceFactory(type=permissions_data.resource_type),
        service=permissions_data.service,
    )
    resource_policy = BaseApprovePolicy.get_approve_policy_class(service_resource)(service_resource)

    assert permissions.can_edit_resource(
        Person(permissions_data.staff),
        resource_policy,
    ) is False

    factories.ServiceMemberFactory(
        staff=permissions_data.staff,
        service=permissions_data.resource_type.supplier,
        role=owner_role
    )

    assert permissions.can_edit_resource(
        Person(permissions_data.staff),
        resource_policy,
    ) is True


def test_delete_by_supplier(permissions_data, owner_role):
    """Ресурс может быть удален поставшиком с определенной ролью
    """
    scope = factories.RoleScopeFactory(slug='development')
    dev = factories.RoleFactory(scope=scope)
    factories.ServiceMemberFactory(
        staff=permissions_data.staff,
        service=permissions_data.resource_type.supplier,
        role=dev,
    )
    service_resource = factories.ServiceResourceFactory(
        state=ServiceResource.GRANTED,
        resource=factories.ResourceFactory(type=permissions_data.resource_type),
        service=permissions_data.service,
    )
    resource_policy = BaseApprovePolicy.get_approve_policy_class(service_resource)(service_resource)

    assert permissions.can_edit_resource(
        Person(permissions_data.staff),
        resource_policy,
    ) is False

    factories.ServiceMemberFactory(
        staff=permissions_data.staff,
        service=permissions_data.resource_type.supplier,
        role=owner_role
    )

    assert permissions.can_edit_resource(
        Person(permissions_data.staff),
        resource_policy,
    ) is True
