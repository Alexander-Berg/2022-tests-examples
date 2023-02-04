from plan.services.models import ServiceMember
from plan.staff.constants import DEPARTMENT_ROLES
from plan.staff.models import Staff
from common import factories
from common.factories.staff import DepartmentStaffFactory
from common.fixtures import staff_factory


def make_superior(person, departments):
    for department in departments:
        DepartmentStaffFactory(
            staff=person,
            department=department,
            role=DEPARTMENT_ROLES.CHIEF,
        )


def put_in_department(department, persons):
    persons_ids = (person.id for person in persons)
    Staff.objects.filter(id__in=persons_ids).update(department=department)


def make_boss_for(staff, boss=None):
    boss = staff_factory()()
    department = factories.DepartmentFactory()
    make_superior(boss, [department])
    put_in_department(department, [staff])

    return boss


def request_member_side_effect(service, staff, role, *args, **kwargs):
    members = ServiceMember.all_states.filter(service=service, staff=staff, role=role, from_department=None, resource=None)
    if members.exists():
        members.update(state=ServiceMember.states.ACTIVE)
    else:
        factories.ServiceMemberFactory(service=service, staff=staff, role=role, state=ServiceMember.states.ACTIVE)
    return {'id': 1}


def deprive_member_side_effect(member, *args, **kwargs):
    ServiceMember.all_states.filter(id=member.id).update(state=ServiceMember.states.DEPRIVED)
