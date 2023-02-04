import pytest
from django.conf import settings
from django.test import TestCase
from django.core.urlresolvers import reverse

from plan.roles.models import Role
from plan.services.models import ServiceMember
from plan.services.tasks import update_service_department_members

from common import factories


class DepartmentMembersTestCase(TestCase):
    def setUp(self):
        self.plan_sender = factories.StaffFactory(login=settings.ABC_ZOMBIK_LOGIN)
        self.plan_sender.user.is_superuser = True
        self.plan_sender.user.is_staff = True
        self.plan_sender.user.save()

        self.consultant = factories.RoleFactory(name='Consultant', name_en='Consultant')

        self.owner = factories.StaffFactory()
        self.owner_role = factories.RoleFactory(code=Role.EXCLUSIVE_OWNER)
        self.developer = factories.RoleFactory(code='developer')
        self.person1 = factories.StaffFactory()
        self.person2 = factories.StaffFactory()
        self.person3 = factories.StaffFactory()
        self.person4 = factories.StaffFactory()

        self.service = factories.ServiceFactory(owner=self.owner)
        factories.ServiceMemberFactory(
            service=self.service,
            role=self.owner_role,
            staff=self.owner,
        )
        self.department = factories.DepartmentFactory()
        self.member_department = factories.ServiceMemberDepartmentFactory(
            service=self.service,
            department=self.department,
            role=self.developer,
        )

    def test_auto_delete(self):
        # второй сотрудник в департаменте
        self.person2.department = self.department
        self.person2.save()

        # обычный
        factories.ServiceMemberFactory(
            service=self.service,
            staff=self.person1,
        )
        # от связанного департамента
        factories.ServiceMemberFactory(
            service=self.service,
            staff=self.person2,
            from_department=self.member_department,
        )
        # от связанного департамента, но сам уже переместился в другой
        will_be_deleted = factories.ServiceMemberFactory(
            service=self.service,
            staff=self.person3,
            from_department=self.member_department,
        )

        update_service_department_members(self.service)

        self.assertEqual(ServiceMember.all_states.get(pk=will_be_deleted.pk).state, 'deprived')
        self.assertEqual(self.service.members.count(), 3)

    def test_auto_add_approved(self):
        for person in (self.person2, self.person3, self.person4):
            person.department = self.department
            person.save()

        # обычный
        member1 = factories.ServiceMemberFactory(
            service=self.service,
            staff=self.person1,
        )
        # от связанного департамента
        member3 = factories.ServiceMemberFactory(
            service=self.service,
            staff=self.person3,
            from_department=self.member_department,
        )
        # участник, которого надо добавить из связанного подразделения,
        # уже есть в команде сервиса, но в другой роли
        alt_role = factories.RoleFactory()
        member4 = factories.ServiceMemberFactory(
            service=self.service,
            staff=self.person4,
            role=alt_role,
        )

        update_service_department_members(self.service)

        member1.refresh_from_db()
        member3.refresh_from_db()
        member4.refresh_from_db()
        self.assertEqual(self.service.members.count(), 6)

        new_member_queryset = self.service.members.exclude(id=member4.id)
        new_member = new_member_queryset.get(staff=self.person4)

        self.assertEqual(new_member.from_department, self.member_department)

    def test_auto_update_existing_approved(self):
        for person in (self.person2, self.person3):
            person.department = self.department
            person.save()

        # обычный
        member1 = factories.ServiceMemberFactory(
            service=self.service,
            staff=self.person1,
        )
        # от связанного департамента
        member2 = factories.ServiceMemberFactory(
            service=self.service,
            staff=self.person2,
            from_department=self.member_department,
        )

        update_service_department_members(self.service)

        member1.refresh_from_db()
        member2.refresh_from_db()
        self.assertEqual(self.service.members.count(), 4)

    def test_auto_update_existing_keep_approved(self):
        for person in (self.person2, self.person3):
            person.department = self.department
            person.save()

        # обычный
        member1 = factories.ServiceMemberFactory(
            service=self.service,
            staff=self.person1,
        )
        # от связанного департамента
        member2 = factories.ServiceMemberFactory(
            service=self.service,
            staff=self.person2,
            from_department=self.member_department,
        )
        # тот же сотрудник в другой роли
        alt_role = factories.RoleFactory()
        member4 = factories.ServiceMemberFactory(
            service=self.service,
            staff=self.person3,
            role=alt_role,
        )

        update_service_department_members(self.service)

        member1.refresh_from_db()
        member2.refresh_from_db()
        member4.refresh_from_db()
        self.assertEqual(self.service.members.count(), 5)


@pytest.mark.django_db
@pytest.mark.parametrize(('role', 'result'), [
    ('own_only_viewer', [settings.HAS_OWN_ONLY_VIEWER]),
    ('services_viewer', [settings.HAS_SERVICES_VIEWER]),
    ('full_access', []),
])
def test_abc_ext(client, service, staff_factory, role, result):
    department = factories.DepartmentFactory()
    staff = factories.StaffFactory(department=department)
    factories.InternalRoleFactory(staff=staff, role=role)

    response = client.get(reverse('departments:members', args=[department.id]))
    assert response.status_code == 200
    content = response.json()['content']

    assert content == {
        'members': [{
            'id': staff.id,
            'login': staff.login,
            'firstName': staff.i_first_name,
            'lastName': staff.i_last_name,
            'isDismissed': staff.is_dismissed,
            'fullName': staff.get_full_name(),
            'is_robot': staff.is_robot,
            'affiliation': staff.affiliation,
            'abc_ext': result,
            'is_frozen': staff.is_frozen,
        }],
        'departments': {
            'id': department.id,
            'name': department.name,
        }
    }


def test_roles(client, service, staff_factory):
    default_role = factories.RoleFactory(code=settings.ABC_DEFAULT_SERVICE_ROLE)
    department = factories.DepartmentFactory()
    staff = factories.StaffFactory(department=department)
    factories.InternalRoleFactory(staff=staff, role='full_access')

    form = {
        'service': service.id,
        'default_role_for': 'service',
    }
    response = client.get(reverse('departments:members', args=[department.id]), form)
    assert response.status_code == 200
    content = response.json()['content']
    assert content['members'][0]['default_service_role']['id'] == default_role.id
