from datetime import datetime
from mock import Mock, patch

from staff.person.models import Staff

from staff.departments.controllers.proposal import ProposalCtl, proposal_oebs_format
from staff.departments.controllers.proposal_execution import actualize_affiliation_for_department
from staff.departments.models import Department, ProposalMetadata


def test_get_action(db, proposal_dict):
    dep1, dep2 = Department.objects.filter(url__in=['dep_1', 'dep_2']).order_by('url')
    ctl = ProposalCtl.create_from_dict(proposal_dict)
    assert ctl.get_action(dep1.id) == proposal_dict['actions'][0]
    assert ctl.get_action(dep2.id) == proposal_dict['actions'][1]
    assert ctl.get_action(123456789876) is None


def test_actualize_affiliation(company):
    mocked_actualize = Mock()
    with patch('staff.departments.controllers.proposal_execution.actualize_affiliation', new=mocked_actualize):
        actualize_affiliation_for_department(company.dep1.id)
        # в yandex_dep1 12 человек (по 2 в подразделениях + 2 HR_партнёра + 2 HR_аналитика)
        assert mocked_actualize.call_count == 12


def test_proposal_oebs_format(applied_proposal_dict):
    applied_proposal_dict.update({
        'created_at': datetime.now(),
        'updated_at': datetime.now(),
        'apply_at_hr': datetime.now(),
        'apply_at': datetime.now(),
    })

    dep1, dep2, dep3, dep4 = (
        Department.objects
        .filter(url__in=['dep_1', 'dep_2', 'dep_3', 'dep_4'])
        .order_by('url')
    )
    dep2.category = 'technical'
    dep3.category = 'technical'
    dep2.save()
    dep3.save()

    oebs_data = proposal_oebs_format(applied_proposal_dict)

    actions = [
        {
            'delete': False,
            'execution_order': 0,
            'extra': {'category': '200'},
            'global_org_id': dep1.id,
            'name': {
                'is_correction': False,
                'hr_type': True,
                'kind': 'None',
                'name': dep1.name,
                'name_en': '',
                'type': 'update'
            }
        },
        {
            'delete': False,
            'execution_order': 1,
            'extra': {'category': '100'},
            'global_org_id': dep2.id,
            'name': {
                'is_correction': False,
                'hr_type': False,
                'kind': 'None',
                'name': dep2.name,
                'name_en': '',
                'type': 'update'
            }
        },
        {
            'delete': False,
            'execution_order': 2,
            'extra': {'category': '100'},
            'global_org_id': dep3.id,
            'name': {
                'is_correction': True,
                'hr_type': False,
                'kind': 'None',
                'name': dep3.name,
                'name_en': '',
                'type': 'update'
            }
        },
        {
            'delete': False,
            'execution_order': 3,
            'extra': {'category': '200'},
            'global_org_id': dep4.id,
            'name': {
                'is_correction': True,
                'hr_type': False,
                'kind': 'None',
                'name': dep4.name,
                'name_en': '',
                'type': 'update'}
            }
    ]
    meta = ProposalMetadata.objects.get(proposal_id=str(applied_proposal_dict['_id']))

    assert set(oebs_data.keys()) == {
        'ticket', 'description', 'applied_by', 'apply_at',
        'created_by', 'created_at', 'execution_date', 'actions',
        'effective_date',
    }
    assert oebs_data['ticket'] == 'TSALARY-712'
    assert oebs_data['description'] == 'Заявка применена!'
    assert oebs_data['applied_by'] == meta.applied_by.login
    assert oebs_data['apply_at'] == applied_proposal_dict['apply_at'].isoformat()[:-3]

    # Удалим когда OEBS с этого поля слезет
    assert oebs_data['created_by'] == Staff.objects.get(id=applied_proposal_dict['author']).login

    assert oebs_data['created_at'][:-4] == applied_proposal_dict['created_at'].isoformat()[:-7]
    assert oebs_data['execution_date'] == '2017-12-31T04:23:45.000'
    assert oebs_data['actions'] == actions


def test_delete(proposal_skeleton, person_from_ya, deadlines):
    p_ctl = ProposalCtl(author=person_from_ya).create_from_cleaned_data(proposal_skeleton)
    p_ctl.save()

    p_ctl.delete()

    assert ProposalMetadata.objects.filter(proposal_id=p_ctl.proposal_id, deleted_at__isnull=False).exists()


def test_update_person_sections(company, proposal_skeleton, person_from_ya):
    proposal_skeleton['persons']['actions'] = [
        {
            'action_id': 'act_90365',
            'sections': ['salary', 'position'],
            'salary':
                {
                    'old_currency': 'RUB',
                    'new_currency': 'RUB',
                    'new_salary': '123321',
                    'new_wage_system': 'fixed',
                    'old_wage_system': 'fixed',
                    'old_salary': '123123',
                    'new_rate': '1',
                    'old_rate': '1'
                },
            'position': {
                'position_legal': 'fsdfsdfsdf',
                'new_position': 'sdfsdfsdf'
            },
            'comment': 'sdfsdfsdf',
            'login': person_from_ya.login,
        }
    ]
    new_persons_actions_state = [{
        'comment': 'sdfsdfsdf',
        'position': {
            'position_legal': 'fsdfsdfsdf',
            'new_position': 'sdfsdfsdf',
        },
        'login': 'wlame',
        'sections': ['position'],
        'action_id': 'act_90365',
    }]
    ctl = ProposalCtl.create_from_dict(proposal_skeleton)
    ctl._update_person_actions(new_persons_actions_state)

    assert 'salary' not in next(ctl.person_actions)
