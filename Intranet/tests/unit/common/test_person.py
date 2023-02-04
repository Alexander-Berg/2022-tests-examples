import pytest

from plan.common.person import Person
from plan.roles.models import Role
from plan.services.models import Service
from common import factories

pytestmark = pytest.mark.django_db


def test_repr():
    staff = factories.StaffFactory()
    person = Person(staff)

    assert isinstance(repr(person), str)


def test_person_permissions(owner_role, django_assert_num_queries):
    """ Проверяем методы проверки прав в Person
    """
    service_a = factories.ServiceFactory()
    service_ab = factories.ServiceFactory(parent=service_a)
    factories.ServiceFactory(parent=service_a)
    service_abd = factories.ServiceFactory(parent=service_ab)
    service_abde = factories.ServiceFactory(parent=service_abd)

    Service.rebuildtable()

    staff_a = factories.StaffFactory()

    role = factories.RoleFactory()

    factories.ServiceMemberFactory(service=service_abd, role=role, staff=staff_a)
    factories.ServiceMemberFactory(
        service=service_ab, staff=staff_a, role=Role.get_responsible())
    factories.ServiceMemberFactory(service=service_ab, role=owner_role, staff=staff_a)

    person = Person(staff_a)

    with django_assert_num_queries(9):
        assert person.is_member_of_team(service_abd)
        assert person.is_member_of_team(service_ab)
        assert person.is_member_of_team(service_a)
        assert not person.is_member_of_team(service_a, with_ancestors=False)

        assert person.has_role(service_abd, role)
        assert person.has_role(service_abde, role)
        assert not person.has_role(service_abde, role, with_descendants=False)

        assert person.is_responsible(service_ab)
        assert person.is_responsible(service_abd)

        assert person.is_head(service_ab)
        assert person.is_head(service_abd)
