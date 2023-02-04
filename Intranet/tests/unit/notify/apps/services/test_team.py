import pytest

from plan.common.utils.collection.mapping import DotAccessedDict
from plan.notify.apps.services import generators, aggregators
from common import factories
from utils import iterables_are_equal

pytestmark = [pytest.mark.django_db]


def strip_qs_digest(result):
    key = 'querystring_ids_digest'
    for item in result:
        if key in item[1]:
            del item[1][key]


def member2event(sender, type, member):
    if type == 'service_member_edited':
        def get_params():
            return {
                'service_id': member.service_id,
                'person_id': member.staff_id,
                'old': {
                    'role_id': member.role_id,
                },
                'new': {
                    'role_id': member.role_id,
                }
            }
    else:
        if member.from_department_id:
            from_department_id = member.from_department.department_id
        else:
            from_department_id = None

        def get_params():
            return {
                'service_id': member.service_id,
                'person_id': member.staff_id,
                'role_id': member.role_id,
                'from_department_id': from_department_id,
            }

    return {
        'type': type,
        'sender_id': sender.id,
        'params': get_params()
    }


def departmentmember2event(sender, type, departmentmember):
    return {
        'type': type,
        'sender_id': sender.id,
        'params': {
            'service_id': departmentmember.service_id,
            'department_id': departmentmember.department_id
        }
    }


@pytest.fixture
def services_and_members(owner_role):
    fixture = DotAccessedDict()

    fixture.owner_role = owner_role
    fixture.role1 = factories.RoleFactory(name='role1')
    fixture.role2 = factories.RoleFactory(name='role2')

    fixture.department1 = factories.DepartmentFactory(name='department1')
    fixture.department2 = factories.DepartmentFactory(name='department2')

    fixture.person1 = factories.StaffFactory(login='person1')
    fixture.person2 = factories.StaffFactory(login='person2')
    fixture.person3 = factories.StaffFactory(
        login='person3',
        department=fixture.department2,
    )
    fixture.person4 = factories.StaffFactory(
        login='person4',
        department=fixture.department2,
    )
    fixture.person5 = factories.StaffFactory(
        login='person5',
        department=fixture.department1,
    )
    fixture.person6 = factories.StaffFactory(
        login='person6',
        department=fixture.department1,
    )

    # service #1

    fixture.service1 = DotAccessedDict()
    fixture.service1.object = factories.ServiceFactory(
        owner=fixture.person1,
        name='service1',
    )
    fixture.service1.department1 = factories.ServiceMemberDepartmentFactory(
        department=fixture.department1,
        service=fixture.service1.object,
    )
    fixture.service1.member1 = factories.ServiceMemberFactory(
        service=fixture.service1.object,
        staff=fixture.person1,
        role=fixture.owner_role,
    )
    fixture.service1.member2 = factories.ServiceMemberFactory(
        service=fixture.service1.object,
        staff=fixture.person2,
        role=fixture.role1,
    )
    factories.ServiceMemberFactory(
        service=fixture.service1.object,
        staff=fixture.person2,
        role=owner_role,
    )
    fixture.service1.member3 = factories.ServiceMemberFactory(
        service=fixture.service1.object,
        staff=fixture.person3,
        role=fixture.role1,
    )
    fixture.service1.member4 = factories.ServiceMemberFactory(
        service=fixture.service1.object,
        staff=fixture.person4,
        role=fixture.role2,
    )
    fixture.service1.member5 = factories.ServiceMemberFactory(
        service=fixture.service1.object,
        staff=fixture.person5,
        role=fixture.role1,
        from_department=fixture.service1.department1,
    )
    fixture.service1.member6 = factories.ServiceMemberFactory(
        service=fixture.service1.object,
        staff=fixture.person6,
        role=fixture.role2,
        from_department=fixture.service1.department1,
    )

    # service #2, same owner

    fixture.service2 = DotAccessedDict()
    fixture.service2.object = factories.ServiceFactory(
        owner=fixture.person1,
        name='service2',
    )
    fixture.service2.department2 = factories.ServiceMemberDepartmentFactory(
        department=fixture.department2,
        service=fixture.service2.object,
    )
    fixture.service2.member1 = factories.ServiceMemberFactory(
        service=fixture.service2.object,
        staff=fixture.person1,
        role=fixture.owner_role,
    )
    fixture.service2.member2 = factories.ServiceMemberFactory(
        service=fixture.service2.object,
        staff=fixture.person5,
        role=fixture.role1,
    )
    fixture.service2.member3 = factories.ServiceMemberFactory(
        service=fixture.service2.object,
        staff=fixture.person6,
        role=fixture.role1,
    )
    fixture.service2.member4 = factories.ServiceMemberFactory(
        service=fixture.service2.object,
        staff=fixture.person3,
        role=fixture.role2,
    )
    fixture.service2.member5 = factories.ServiceMemberFactory(
        service=fixture.service2.object,
        staff=fixture.person3,
        role=fixture.role2,
        from_department=fixture.service2.department2,
    )

    fixture.person1.__class__.__repr__ = lambda self: "fixture.{}".format(self.login)
    fixture.service1.object.__class__.__repr__ = lambda self: "fixture.{}.object".format(self.name)
    fixture.role1.__class__.__repr__ = lambda self: "fixture.{}".format(self.name)

    return fixture


def test_person_removed_manually(services_and_members):
    fixture = services_and_members
    fixture.service1.member3.delete()

    entry = member2event(
        sender=fixture.person4,
        type='service_member_removed',
        member=fixture.service1.member3,
    )

    result = list(generators.PersonRemovedGenerator(entry))

    # владелец сервиса и главный в роли что-то получат
    expected_result = [
        ((fixture.person1, fixture.service1.object), [
            ('members_removed', {
                'person': fixture.person3,
                'role': fixture.role1,
                'sender': fixture.person4})
        ]),
        ((fixture.person2, fixture.service1.object), [
            ('members_removed', {
                'person': fixture.person3,
                'role': fixture.role1,
                'sender': fixture.person4
            })
        ])
    ]

    assert iterables_are_equal(result, expected_result)


def test_person_removed_automatically(services_and_members):
    fixture = services_and_members
    fixture.service1.member3.delete()

    entry = member2event(
        sender=fixture.person4,
        type='service_member_removed',
        member=fixture.service1.member3,
    )
    entry['params']['from_department_id'] = fixture.department1.id

    result = list(generators.PersonRemovedGenerator(entry))

    # владелец сервиса и главный в роли что-то получат
    expected_result = [
        ((fixture.person1, fixture.service1.object), [
            ('members_auto_removed', {
                'from_department': fixture.department1,
                'person': fixture.person3,
                'role': fixture.role1,
                'sender': fixture.person4})
        ]),
        ((fixture.person2, fixture.service1.object), [
            ('members_auto_removed', {
                'from_department': fixture.department1,
                'person': fixture.person3,
                'role': fixture.role1,
                'sender': fixture.person4})
        ])
    ]

    assert iterables_are_equal(result, expected_result)


def test_person_added_automatically_approved(services_and_members):
    fixture = services_and_members

    entry = member2event(
        sender=fixture.person4,
        type='service_member_added',
        member=fixture.service1.member5,
    )

    result = list(generators.PersonAddedGenerator(entry))

    # владелец сервиса и главный в роли что-то получат
    expected_result = [
        ((fixture.person1, fixture.service1.object), [
            ('members_auto_added', {
                'from_department': fixture.department1,
                'person': fixture.person5,
                'role': fixture.role1,
                'sender': fixture.person4})
        ]),
        ((fixture.person2, fixture.service1.object), [
            ('members_auto_added', {
                'from_department': fixture.department1,
                'person': fixture.person5,
                'role': fixture.role1,
                'sender': fixture.person4
            })
        ])
    ]

    assert iterables_are_equal(result, expected_result)


def test_you_were_removed_manually(services_and_members):
    fixture = services_and_members
    fixture.service1.member4.delete()

    entry = member2event(
        sender=fixture.person6,
        type='service_member_removed',
        member=fixture.service1.member4,
    )

    result = list(generators.YouWereRemovedGenerator(entry))

    expected_result = [
        ((fixture.person4, fixture.service1.object), [
            ('removed_from', {
                'sender': fixture.person6
            })
        ])
    ]

    assert result == expected_result


def test_you_were_removed_automatically(services_and_members):
    fixture = services_and_members

    entry = member2event(
        sender=fixture.person6,
        type='service_member_removed',
        member=fixture.service1.member5,
    )

    fixture.service1.member5.delete()

    result = list(generators.YouWereRemovedGenerator(entry))

    expected_result = [
        ((fixture.person5, fixture.service1.object), [
            ('auto_removed_from', {
                'from_department': fixture.department1,
                'sender': fixture.person6
            })
        ])
    ]

    assert result == expected_result


def test_weekly_reminder_nothing_happened(services_and_members):
    result = list(aggregators.weekly_reminder())
    assert result == []
