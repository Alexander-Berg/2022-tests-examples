import pytest
from django.conf import settings

from plan.internal_roles.utils import add_perm
from plan.roles.models import Role
from plan.staff.models import Staff
from common import factories


@pytest.fixture()
def metaservice(db, responsible_role, person,):
    metaservice = factories.ServiceFactory(parent=None, owner=person)
    factories.ServiceMemberFactory(service=metaservice, role=responsible_role, staff=person)
    return metaservice


@pytest.fixture
def defaultmeta(responsible_role, person):
    defaultmeta = factories.ServiceFactory(parent=None, owner=person, slug=settings.ABC_DEFAULT_SERVICE_PARENT_SLUG)
    factories.ServiceMemberFactory(service=defaultmeta, role=responsible_role, staff=person)
    return defaultmeta


@pytest.fixture
def metaservices(defaultmeta, metaservice):
    """Нужна для проверки отношений наследования в дереве сервисов."""
    return metaservice, defaultmeta


@pytest.fixture
def service(metaservice, owner_role, person):
    service = factories.ServiceFactory(parent=metaservice, owner=person)
    factories.ServiceMemberFactory(service=service, role=owner_role, staff=person)
    return service


@pytest.fixture
def owner_role(db):
    return factories.RoleFactory(code=Role.EXCLUSIVE_OWNER)


@pytest.fixture
def duty_role(db):
    return factories.RoleFactory(code=Role.DUTY)


@pytest.fixture
def responsible_for_duty_role(db):
    return factories.RoleFactory(code=Role.RESPONSIBLE_FOR_DUTY)


@pytest.fixture(autouse=True)
def deputy_role(db):
    return factories.RoleFactory(code=Role.DEPUTY_OWNER)


@pytest.fixture(autouse=True)
def responsible_role(db):
    return factories.RoleFactory(code=Role.RESPONSIBLE)


@pytest.fixture
def service_with_owner(db, person, owner_role):
    service = factories.ServiceFactory(owner=person)
    factories.ServiceMemberFactory(
        service=service,
        role=owner_role,
        staff=person,
    )
    return service


@pytest.fixture
def service_member(db):
    return factories.ServiceMemberFactory()


@pytest.fixture
def service_member_department(db):
    return factories.ServiceMemberDepartmentFactory()


@pytest.fixture
def service_member_from_department(db, service_member_department):
    return factories.ServiceMemberFactory(
        from_department=service_member_department)


@pytest.fixture
def role(db):
    return factories.RoleFactory()


@pytest.fixture
def role_two(db):
    return factories.RoleFactory()


@pytest.fixture
def person(db, staff_factory):
    return staff_factory('full_access', login='login')


@pytest.fixture
def person_two(db, staff_factory):
    return staff_factory('full_access')


@pytest.fixture
def department(db):
    return factories.DepartmentFactory()


@pytest.fixture
def superuser(db, staff_factory):
    return staff_factory('full_access', user=factories.UserFactory(is_superuser=True))


@pytest.fixture
def make_staff_superuser(db, staff_factory):
    def update(staff=None):
        return staff_factory('full_access', staff, user=factories.UserFactory(is_superuser=True))

    return update


@pytest.fixture(autouse=True)
def robot(db):
    return factories.StaffFactory(login=settings.ABC_ZOMBIK_LOGIN, user=factories.UserFactory(is_superuser=True))


@pytest.fixture
def important_resource(db):
    resource_type = factories.ResourceTypeFactory(is_important=True)
    return factories.ResourceFactory(type=resource_type)


def staff_factory():
    def creator(role_name='full_access', staff=None, **kwargs):
        if not staff:
            staff = factories.StaffFactory(**kwargs)
        else:
            Staff.objects.filter(pk=staff.id).update(**kwargs)
        perms_to_add = settings.ABC_INTERNAL_ROLES_PERMISSIONS.get(role_name, [])
        for perm in perms_to_add:
            add_perm(perm, staff)
        return staff

    return creator


@pytest.fixture(name='staff_factory')
def staff_factory_fixture():
    return staff_factory()
