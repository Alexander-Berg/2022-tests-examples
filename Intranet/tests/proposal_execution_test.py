import pytest
from mock import Mock
from bson.objectid import ObjectId

from django.db.models import Q

from staff.budget_position.const import WORKFLOW_STATUS
from staff.budget_position.models import ChangeRegistry
from staff.headcounts.tests.factories import HeadcountPositionFactory

from staff.departments.controllers.proposal_execution import ProposalExecution
from staff.groups.models import GROUP_TYPE_CHOICES
from staff.lib.db import atomic
from staff.lib.testing import (
    DepartmentKindFactory,
    DepartmentFactory,
    GroupFactory,
    OfficeFactory,
    OrganizationFactory,
    StaffFactory, BudgetPositionFactory,
)
from staff.oebs.constants import PERSON_POSITION_STATUS
from staff.person.models import Staff

from staff.departments.controllers.proposal import ProposalCtl
from staff.departments.models import Department, ProposalMetadata

# Что это вообще за тест должен был быть?
from staff.proposal.proposal_builder import ProposalBuilder


def test_execute(proposal_dict, deadlines):
    GroupFactory(url='__departments__')
    proposal_dict['_id'] = ObjectId('5b0289c618ddb2051409f49d')
    ProposalMetadata.objects.create(proposal_id=proposal_dict['_id'])

    p_ctl = ProposalCtl()
    p_ctl.fill_from_dict(proposal_dict)
    ProposalExecution(p_ctl).execute()


def test_create_dep(company, deadlines, mocked_mongo):
    name = 'some_name'
    proposal_id = (
        ProposalBuilder()
        .for_new_department(name, lambda department: department.set_parent(company.yandex.url))
        .build(company.persons['yandex-chief'].login)
    )

    ProposalExecution(ProposalCtl(proposal_id)).execute()
    assert Department.objects.filter(name=name).exists()


def test_delete_dep(proposal_skeleton, deadlines):
    dep = DepartmentFactory()
    GroupFactory(
        name='dep_name',
        url=dep.url,
        department=dep,
        type=GROUP_TYPE_CHOICES.DEPARTMENT,
    )

    proposal_skeleton['actions'].append({
        'id': dep.id,
        'fake_id': '',
        'delete': True,
    })
    ProposalMetadata.objects.create(proposal_id=proposal_skeleton['_id'])

    ProposalExecution(ProposalCtl.create_from_dict(proposal_skeleton)).execute()

    assert Department.objects.filter(id=dep.id, intranet_status=0).exists()


def test_update_dep_name(company, proposal_skeleton, deadlines):
    name_dict = {
        'name': 'Не яндекс',
        'name_en': 'Not Yandex',
        'short_name': 'не-я',
        'short_name_en': 'no-ya',
    }
    dep_id = company.yandex.id
    proposal_skeleton['actions'].append({
        'id': dep_id,
        'name': name_dict,
        'fake_id': '',
        'delete': False,
    })
    ProposalMetadata.objects.create(proposal_id=proposal_skeleton['_id'])

    ProposalExecution(ProposalCtl.create_from_dict(proposal_skeleton)).execute()


def test_create_dep_no_parent(company, proposal_skeleton, deadlines):
    parent_name = 'parent_dep'
    name = 'creating_dep'
    fake_parent_id = '_3231111674'
    proposal_skeleton['actions'] += [
        {
            'administration': {
                'chief': StaffFactory().id,
            },
            'delete': False,
            'fake_id': fake_parent_id,
            'hierarchy': {
                'fake_parent': '',
                'parent': company.yandex.id,
            },
            'id': None,
            'name': {
                'name': parent_name,
                'name_en': parent_name,
                'short_name': parent_name,
                'short_name_en': parent_name,
            },
            'technical': {
                'code': 'fk',
                'position': 0,
                'kind': DepartmentKindFactory().id,
            },
        },
        {
            'administration': {
                'chief': StaffFactory().id,
            },
            'delete': False,
            'fake_id': '_3231111645',
            'hierarchy': {
                'fake_parent': fake_parent_id,
                'parent': '',
            },
            'id': None,
            'name': {
                'name': name,
                'name_en': name,
                'short_name': name,
                'short_name_en': name,
            },
            'technical': {
                'code': 'cdep',
                'position': 0,
                'kind': DepartmentKindFactory().id,
            },
        },
    ]
    ProposalMetadata.objects.create(proposal_id=proposal_skeleton['_id'])

    ProposalExecution(ProposalCtl.create_from_dict(proposal_skeleton)).execute()

    assert Department.objects.filter(name=name, parent__name=parent_name).exists()


def test_update_person_position(proposal_skeleton, person_from_ya, deadlines):
    new_posigion = 'pseudo random generator'
    proposal_skeleton['persons']['actions'].append({
        'sections': ['position'],
        'login': person_from_ya.login,
        'position': {'new_position': new_posigion},
        'action_id': 'act_3231111',
    })
    ProposalMetadata.objects.create(proposal_id=proposal_skeleton['_id'])

    ProposalExecution(ProposalCtl.create_from_dict(proposal_skeleton)).execute()

    assert Staff.objects.filter(id=person_from_ya.id, position=new_posigion).exists()


def test_update_person_office(proposal_skeleton, person_from_ya, deadlines):
    new_office = OfficeFactory()
    proposal_skeleton['persons']['actions'].append({
        'sections': ['office'],
        'login': person_from_ya.login,
        'office': {'office': new_office.id},
        'action_id': 'act_674323',
    })
    ProposalMetadata.objects.create(proposal_id=proposal_skeleton['_id'])

    ProposalExecution(ProposalCtl.create_from_dict(proposal_skeleton)).execute()

    assert Staff.objects.filter(id=person_from_ya.id, office_id=new_office.id).exists()


def test_update_person_organization(proposal_skeleton, person_from_ya, deadlines):
    new_org = OrganizationFactory()
    proposal_skeleton['persons']['actions'].append({
        'sections': ['organization'],
        'login': person_from_ya.login,
        'organization': {'organization': new_org.id},
        'action_id': 'act_320672',
    })
    ProposalMetadata.objects.create(proposal_id=proposal_skeleton['_id'])

    ProposalExecution(ProposalCtl.create_from_dict(proposal_skeleton)).execute()

    assert Staff.objects.filter(id=person_from_ya.id, organization_id=new_org.id).exists()


def test_update_person_dep(proposal_skeleton, person_from_ya, deadlines):
    new_dep = Department.objects.filter(~Q(id=person_from_ya.department_id)).first()
    GroupFactory(
        name='dep_name',
        url=new_dep.url,
        department=new_dep,
        type=GROUP_TYPE_CHOICES.DEPARTMENT,
    )
    proposal_skeleton['persons']['actions'].append({
        'sections': ['department'],
        'login': person_from_ya.login,
        'department': {'department': new_dep.url},
        'action_id': 'act_8997654',
    })
    ProposalMetadata.objects.create(proposal_id=proposal_skeleton['_id'])

    ProposalExecution(ProposalCtl.create_from_dict(proposal_skeleton)).execute()

    assert Staff.objects.filter(id=person_from_ya.id, department_id=new_dep.id).exists()


def test_update_person_dep_vacancy_url(proposal_skeleton, person_from_ya, deadlines):
    new_dep = Department.objects.filter(~Q(id=person_from_ya.department_id)).first()
    ticket = 'JOB-000'
    from staff.departments.tests.factories import VacancyFactory
    VacancyFactory(is_active=True, ticket=ticket)
    vacancy_url = "https://st.test.yandex-team.ru/" + ticket
    GroupFactory(
        name='dep_name',
        url=new_dep.url,
        department=new_dep,
        type=GROUP_TYPE_CHOICES.DEPARTMENT,
    )

    proposal_skeleton['persons']['actions'].append({
        'sections': ['department'],
        'login': person_from_ya.login,
        'department': {'department': new_dep.url, 'vacancy_url': vacancy_url},
        'action_id': 'act_8997654',
    })
    ProposalMetadata.objects.create(proposal_id=proposal_skeleton['_id'])

    ProposalExecution(ProposalCtl.create_from_dict(proposal_skeleton)).execute()

    assert Staff.objects.filter(id=person_from_ya.id, department_id=new_dep.id).exists()
    # TODO: Check vacancy_url effects


def test_move_person_to_creating_dep(company, proposal_skeleton, person_from_ya, deadlines):
    name = 'some_name'
    fake_id = '_3231111674'
    proposal_skeleton['actions'].append({
        'id': None,
        'fake_id': fake_id,
        'action_id': 'act_32254',
        'delete': False,
        'administration': {
            'chief': StaffFactory().id,
        },
        'hierarchy': {
            'fake_parent': '',
            'parent': company.yandex.id,
        },
        'name': {
            'name': name,
            'name_en': 'ABC',
            'short_name': 's_name',
            'short_name_en': 'ggl',
        },
        'technical': {
            'code': 'rndnm',
            'position': 0,
            'kind': DepartmentKindFactory().id,
        },
    })
    proposal_skeleton['persons']['actions'].append({
        'sections': ['department'],
        'login': person_from_ya.login,
        'department': {
            'fake_department': fake_id,
            'department': None,
        },
        'action_id': 'act_1234567',
    })
    ProposalMetadata.objects.create(proposal_id=proposal_skeleton['_id'])
    ProposalExecution(ProposalCtl.create_from_dict(proposal_skeleton)).execute()

    assert Staff.objects.filter(id=person_from_ya.id, department__name=name).exists()


def test_move_person_from_deleting_dep(company, proposal_skeleton, person_from_ya, deadlines):
    deleting_dep = DepartmentFactory()
    GroupFactory(
        name='dep_name',
        url=deleting_dep.url,
        department=deleting_dep,
        type=GROUP_TYPE_CHOICES.DEPARTMENT,
    )
    person_from_ya.department_id = deleting_dep.id
    person_from_ya.save()
    target_dep = company.yandex

    proposal_skeleton['actions'].append({
        'id': deleting_dep.id,
        'action_id': 'act_987123',
        'fake_id': '',
        'delete': True,
    })
    proposal_skeleton['persons']['actions'].append({
        'action_id': 'act_123987',
        'sections': ['department'],
        'login': person_from_ya.login,
        'department': {'department': target_dep.url},
    })
    ProposalMetadata.objects.create(proposal_id=proposal_skeleton['_id'])

    ProposalExecution(ProposalCtl.create_from_dict(proposal_skeleton)).execute()

    assert Staff.objects.filter(id=person_from_ya.id, department_id=target_dep.id).exists()
    assert Department.objects.filter(id=deleting_dep.id, intranet_status=0)


@pytest.mark.skip
def test_proposal_can_be_executed_again_after_department_creation_failure(proposal_skeleton, company, deadlines):
    name = 'some_name'
    proposal_skeleton['actions'].append({
        'administration': {
            'chief': StaffFactory().id,
        },
        'delete': False,
        'fake_id': '_3231111674',
        'hierarchy': {
            'fake_parent': '',
            'parent': company.yandex.id,
        },
        'id': None,
        'name': {
            'name': name,
            'name_en': 'ABC',
            'short_name': 's_name',
            'short_name_en': 'ggl',
        },
        'technical': {
            'code': 'rndnm',
            'position': 0,
            'kind': DepartmentKindFactory().id,
        },
    })

    name_dict = {
        'name': 'Не яндекс',
        'name_en': 'Not Yandex',
        'short_name': 'не-я',
        'short_name_en': 'no-ya',
    }
    dep_id = company.dep1.id
    proposal_skeleton['actions'].append({
        'id': dep_id,
        'name': name_dict,
        'fake_id': '',
        'delete': False,
    })
    ProposalMetadata.objects.create(proposal_id=proposal_skeleton['_id'])
    proposal = ProposalCtl.create_from_dict(proposal_skeleton)
    proposal.save()
    proposal_id = proposal.proposal_id

    class ServerIsBroken(Exception):
        pass

    proposel_execution = ProposalExecution(proposal)
    proposel_execution.create_department = Mock(side_effect=ServerIsBroken('KILL ALL HUMANS'))
    try:
        with atomic():
            proposel_execution.execute()
    except ServerIsBroken:
        pass

    proposal = ProposalCtl(proposal_id=proposal_id)
    proposel_execution = ProposalExecution(proposal)
    proposel_execution.execute()

    assert Department.objects.filter(name=name).exists()
    assert Department.objects.get(id=company.dep1.id).name == name_dict['name']


@pytest.mark.skip
def test_proposal_can_be_executed_again_after_fail_at_the_end(proposal_skeleton, company, deadlines):
    name = 'some_name'
    proposal_skeleton['actions'].append({
        'administration': {
            'chief': StaffFactory().id,
        },
        'delete': False,
        'fake_id': '_3231111674',
        'hierarchy': {
            'fake_parent': '',
            'parent': company.yandex.id,
        },
        'id': None,
        'name': {
            'name': name,
            'name_en': 'ABC',
            'short_name': 's_name',
            'short_name_en': 'ggl',
        },
        'technical': {
            'code': 'rndnm',
            'position': 0,
            'kind': DepartmentKindFactory().id,
        },
    })

    name_dict = {
        'name': 'Не яндекс',
        'name_en': 'Not Yandex',
        'short_name': 'не-я',
        'short_name_en': 'no-ya',
    }
    dep_id = company.dep1.id
    proposal_skeleton['actions'].append({
        'id': dep_id,
        'name': name_dict,
        'fake_id': '',
        'delete': False,
    })
    ProposalMetadata.objects.create(proposal_id=proposal_skeleton['_id'])
    proposal = ProposalCtl.create_from_dict(proposal_skeleton)
    proposal.save()
    proposal_id = proposal.proposal_id

    class ServerIsBroken(Exception):
        pass

    proposal_execution = ProposalExecution(proposal)
    proposal_execution.actualize_affiliations = Mock(side_effect=ServerIsBroken('KILL ALL HUMANS'))
    try:
        with atomic():
            proposal_execution.execute()
    except ServerIsBroken:
        pass

    proposal = ProposalCtl(proposal_id=proposal_id)
    proposal_execution = ProposalExecution(proposal)
    proposal_execution.execute()

    assert Department.objects.filter(name=name).exists()
    assert Department.objects.get(id=company.dep1.id).name == name_dict['name']


def test_with_headcount_movement(company, deadlines, mocked_mongo):
    # given
    budget_position = BudgetPositionFactory()
    HeadcountPositionFactory(
        status=PERSON_POSITION_STATUS.VACANCY_PLAN,
        department=company.yandex,
        code=budget_position.code,
    )
    proposal_id = (
        ProposalBuilder()
        .for_new_department(
            'test_with_headcount_movement',
            lambda department: (
                department
                .set_parent(company.yandex.url)
                .use_for_headcount(budget_position.code)
            )
        )
        .build(company.persons['yandex-chief'].login)
    )

    # when
    ProposalExecution(ProposalCtl(proposal_id)).execute()

    # then
    department = Department.objects.get(name='test_with_headcount_movement')
    change = (
        ChangeRegistry.objects
        .get(workflow__proposal__proposal_id=proposal_id, workflow__status=WORKFLOW_STATUS.CONFIRMED)
    )

    assert change.department_id == department.id
