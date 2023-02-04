import pytest
import mock
import datetime

from django.contrib.auth.models import Permission
from django.conf import settings
from django.core.exceptions import ValidationError

from staff.departments.models import DepartmentRoles, DEPARTMENT_CATEGORY, DepartmentKind
from staff.groups.models import GROUP_TYPE_CHOICES
from staff.lib.testing import GroupFactory, DepartmentStaffFactory, StaffFactory

from staff.proposal.forms.proposal import (
    is_first_date_required,
    is_first_date_selected,
    ProposalForm,
    _convert_from_form_data,
)

proposal_cases = [
    ({'departments': {'actions': []}}, False),
    ({'persons': {'actions': []}, 'departments': {}}, False),
    (
        {'persons': {
            'actions': [{
                  'comment': 'comment 12345',
                  'login': 'evening',
                  'position': {'new_position': 'sdfasdfa',
                               'position_legal': 'fasdfasdf'},
                  'sections': ['position'],
            }]
        }, 'departments': {}}, False
    ),
    (
        {'persons': {
            'actions': [{
                  'comment': 'comment 12345',
                  'login': 'wlame',
                  'salary': {''},
                  'sections': ['position'],
            }]
        }, 'departments': {}}, True
    ),
    (
        {'persons': {
            'actions': [{
                'comment': 'comment 12345',
                'login': 'denis-p',
                'salary': {''},
                'sections': ['position'],
                'department': {'from_maternity_leave': True},
            }]
        }, 'departments': {}}, False
    ),
    (
        {'persons': {
            'actions': [{
                'comment': 'comment 12345',
                'login': 'denis-p',
                'salary': {''},
                'sections': ['position'],
                'department': {'from_maternity_leave': False},
            }]
        }, 'departments': {}}, True
    ),

]


class MockedDate(datetime.date):
    @classmethod
    def today(cls):
        return cls(year=2018, month=2, day=4)


@pytest.mark.parametrize('proposal_data, is_required', proposal_cases)
def test_is_first_date_required(proposal_data, is_required):
    assert is_first_date_required(proposal_data) == is_required


def test_is_first_date_selected():
    test_dates_correct = [
        datetime.date(year=2018, month=3, day=1),
        datetime.date(year=2018, month=4, day=1),
        datetime.date(year=2018, month=5, day=1),
        datetime.date(year=2018, month=6, day=1),
        datetime.date(year=2018, month=7, day=1),
        datetime.date(year=2018, month=8, day=1),
        datetime.date(year=2018, month=9, day=1),
        datetime.date(year=2018, month=10, day=1),
        datetime.date(year=2018, month=11, day=1),
        datetime.date(year=2018, month=12, day=1),
        datetime.date(year=2019, month=1, day=1),
        datetime.date(year=2019, month=2, day=1),
    ]
    test_dates_incorrect = [
        datetime.date(year=2018, month=3, day=3),
        datetime.date(year=2018, month=8, day=15),
        datetime.date(year=2019, month=5, day=1),
        datetime.date(year=2024, month=6, day=1),
    ]
    with mock.patch('staff.proposal.forms.proposal.date', MockedDate):
        for test_date in test_dates_correct:
            assert is_first_date_selected(test_date) is True
        for test_date in test_dates_incorrect:
            assert is_first_date_selected(test_date) is False


def make_move_department_action(action_id, url, new_parent_url=None, fake_parent_id=None):
    assert bool(new_parent_url) ^ bool(fake_parent_id)

    return {
        'action_id': action_id,
        'sections': ['hierarchy'],
        'hierarchy': {
            'fake_parent': fake_parent_id or '',
            'parent': new_parent_url or '',
            'changing_duties': False,
        },
        'name': {},
        'technical': {},
        'url': url,
        'fake_id': '',
    }


def make_change_department_role_action(action_id, url, login, role='chief'):
    return {
        'action_id': action_id,
        'sections': ['administration'],
        'administration': {
            role: login,
        },
        'name': {},
        'technical': {},
        'url': url,
        'fake_id': '',
    }


def make_create_department_action(action_id, url, fake_id, parent_url=None, fake_parent_id=None):
    assert bool(parent_url) ^ bool(fake_parent_id)

    return {
        'action_id': action_id,
        'fake_id': fake_id,
        'url': '',
        'sections': ['hierarchy'],
        'hierarchy': {
            'fake_parent': fake_parent_id or '',
            'parent': parent_url or '',
            'changing_duties': False,
        },
        'name': {
            'name': url,
            'name_en': url,
        },
        'technical': {
            'category': DEPARTMENT_CATEGORY.TECHNICAL,
            'code': url,
            'department_type': DepartmentKind.objects.first().id,
        },
    }


def make_department_form_data(actions):
    return {
        'persons': [],
        'departments': {
            'actions': actions
        },
        'apply_at': '2032-12-01',
        'description': '',
    }


def make_person_form_data(actions):
    return {
        'persons': {
            'actions': actions,
        },
        'departments': {},
        'apply_at': '2022-12-01',
        'description': '',
    }


def make_move_person_action(action_id, login, department_url):
    return {
        'action_id': action_id,
        'login': login,
        'sections': ['department'],
        'department': {
            'department': department_url,
            'service_groups': [GroupFactory(type=GROUP_TYPE_CHOICES.SERVICE).url],
            'vacancy_url': 'TJOB-1',
            'changing_duties': False,
        }
    }


@pytest.mark.django_db
@mock.patch('staff.proposal.forms.department.find_conflict_proposals', return_value=None)
@mock.patch('staff.proposal.forms.department.DepartmentConsistancyValidator')
def test_proposal_form_transactional_validator_works(mock1, mock2, company, robot_staff_user):
    department_to_move = company.dep2
    parent_department = company.dep111
    form_data = make_department_form_data([
        make_move_department_action(0, department_to_move.url, parent_department.url),
    ])

    form = ProposalForm(data=form_data)
    assert form.is_valid()


@pytest.mark.django_db
@mock.patch('staff.proposal.forms.department.find_conflict_proposals', return_value=None)
@mock.patch('staff.proposal.forms.department.DepartmentConsistancyValidator')
def test_proposal_form_is_valid_for_cyclic_deputy_on_department_moving(mock1, mock2, company, robot_staff_user):
    DepartmentStaffFactory(
        department=company.dep1,
        role_id=DepartmentRoles.DEPUTY.value,
        staff=company.persons['dep2-chief'],
    )

    department_to_move = company.dep2
    parent_department = company.dep111
    form_data = make_department_form_data([
        make_move_department_action(0, department_to_move.url, parent_department.url),
    ])

    form = ProposalForm(data=form_data)
    assert form.is_valid()


@pytest.mark.django_db
@mock.patch('staff.proposal.forms.department.find_conflict_proposals', return_value=None)
@mock.patch('staff.proposal.forms.department.DepartmentConsistancyValidator')
def test_proposal_form_is_valid_for_cyclic_chief_on_changing_deputy(mock1, mock2, company, robot_staff_user):
    department = company.yandex
    form_data = make_department_form_data([
        make_change_department_role_action(0, department.url, ['dep2-chief'], 'deputies'),
    ])

    form = ProposalForm(data=form_data)
    assert form.is_valid()


@pytest.mark.django_db
@mock.patch('staff.proposal.forms.department.find_conflict_proposals', return_value=None)
@mock.patch('staff.proposal.forms.department.DepartmentConsistancyValidator')
def test_proposal_form_transactional_validator_works_for_person(mock1, mock2, company, robot_staff_user):
    person_to_move = company.persons['dep2-chief']
    parent_department = company.dep111
    form_data = make_person_form_data([make_move_person_action(0, person_to_move.login, parent_department.url)])

    form = ProposalForm(data=form_data)
    assert form.is_valid()


@pytest.mark.django_db
@mock.patch('staff.proposal.forms.department.find_conflict_proposals', return_value=None)
@mock.patch('staff.proposal.forms.department.DepartmentConsistancyValidator')
def test_proposal_form_no_job_ticket_without_budget(mock1, mock2, company, robot_staff_user):
    person_to_move = company.persons['dep2-chief']
    parent_department = company.dep111
    action = make_move_person_action(0, person_to_move.login, parent_department.url)
    action["department"]["with_budget"] = False
    del action["department"]["vacancy_url"]
    form_data = make_person_form_data([action])

    form = ProposalForm(data=form_data)
    assert not form.is_valid()


@pytest.mark.django_db
@mock.patch('staff.proposal.forms.department.find_conflict_proposals', return_value=None)
@mock.patch('staff.proposal.forms.department.DepartmentConsistancyValidator')
def test_proposal_form_is_valid_for_cyclic_deputy_on_person_moving(mock1, mock2, company, robot_staff_user):
    DepartmentStaffFactory(
        department=company.dep1,
        role_id=DepartmentRoles.DEPUTY.value,
        staff=company.persons['dep2-chief'],
    )

    person_to_move = company.persons['dep2-chief']
    parent_department = company.dep111
    form_data = make_person_form_data([make_move_person_action(0, person_to_move.login, parent_department.url)])

    form = ProposalForm(data=form_data)
    assert form.is_valid()


@pytest.mark.django_db
def test_converting_to_old_cleaned_data_does_not_fall(company, vacancies):

    vacancy_id = next(iter(vacancies.keys()))

    cd = {
        'persons': {
            'actions': [
                {
                    'grade': {'new_grade': '+1'},
                    'comment': 'wef wef wef',
                    'login': company.persons['dep1-chief'].login,
                    'sections': ['grade'],
                    'action_id': 'act_71946',
                },
            ]
        },
        'vacancies': {
            'actions': [
                {
                    'vacancy_id': vacancy_id,
                    'department': 'yandex_dep1_dep11',
                    'fake_department': '',
                    'action_id': 'act_69869',
                },
            ]
        },
        'headcounts': {},
        'apply_at': datetime.date(2018, 12, 26),
        'description': 'wef',
        'departments': {
            'link_to_ticket': '',
            'actions': [
                {
                    'name': {
                         'name_en': 'External consultants of the HR Department 2323',
                         'name': 'русское название',
                         'hr_type': True
                     },
                    'url': company.dep1.url,
                    'fake_id': '',
                    'action_id': 'act_98031',
                    'sections': ['name'],
                    'delete': False
                }
            ]
        }
    }

    del cd['apply_at']  # in case of invalid
    converted = _convert_from_form_data(cd)
    assert converted == {
        'tickets': {'department_linked_ticket': ''},
        'description': 'wef',
        'actions': [
            {
                'fake_id': None,
                'id': company.dep1.id,
                'name': {
                    'name_en': 'External consultants of the HR Department 2323',
                    'name': 'русское название',
                    'hr_type': True,
                    'is_correction': False,
                },
                'delete': False,
            },
        ],
        'persons': {
            'actions': [
                {
                    'grade': {'new_grade': '+1'},
                    'comment': 'wef wef wef', 'login': company.persons['dep1-chief'].login,
                    'sections': ['grade'],
                    'action_id': 'act_71946',
                },
            ],
        },
        'vacancies': {
            'actions': [
                {
                    'vacancy_id': vacancy_id,
                    'department': 'yandex_dep1_dep11',
                    'fake_department': '',
                    'action_id': 'act_69869',
                },
            ],
        },
        'headcounts': {},
        'apply_at_hr': '2018-05-02',
        'apply_at':  datetime.date.today(),
    }


@pytest.mark.django_db
def test_link_to_ticket_hides_while_editing():

    form = ProposalForm()
    structure = form.structure_as_dict()
    assert 'link_to_ticket' in structure

    form = ProposalForm(base_initial={'_id': '19283746192874361287634'})
    structure = form.structure_as_dict()
    assert 'link_to_ticket' not in structure


@pytest.mark.django_db
def test_clean_link_to_ticket():
    mock.patch(
        'staff.proposal.forms.department._check_ticket_can_be_linked_to_proposal',
        lambda ticket_key: None
    )
    can_execute_permission = Permission.objects.get(codename='can_execute_department_proposals')

    regular = StaffFactory(login='regular').user
    executer = StaffFactory(login='executer').user
    executer.user_permissions.add(can_execute_permission)

    ticket_key = '{}-12345'.format(settings.PROPOSAL_QUEUE_ID)
    with mock.patch(
        'staff.proposal.forms.proposal._check_ticket_can_be_linked_to_proposal',
        lambda ticket_key: None
    ):
        with pytest.raises(ValidationError) as error:
            ProposalForm(
                base_initial={'author_user': regular}
            ).clean_link_to_ticket(ticket_key)
            assert error.value.code == 'no_permission'

        assert ProposalForm(
            base_initial={'author_user': executer}
        ).clean_link_to_ticket(ticket_key) == ticket_key

    ticket_key = 'NOT{}-12345'.format(settings.PROPOSAL_QUEUE_ID)
    with pytest.raises(ValidationError):
        ProposalForm().clean_link_to_ticket(ticket_key)


@pytest.mark.django_db
def test_clean_apply_at_by_regular_person():
    regular = StaffFactory(login='regular').user
    yesterday = (MockedDate.today() - datetime.timedelta(days=1))

    with mock.patch('staff.proposal.forms.proposal.date', MockedDate):
        with mock.patch('staff.proposal.forms.proposal.is_first_date_required', lambda proposal_data: False):
            # regular author
            proposal_form = ProposalForm(base_initial={'author_user': regular})
            with pytest.raises(ValidationError) as error:
                proposal_form.clean_apply_at(yesterday)
            assert error.value.message == 'Apply date should be at least today'


@pytest.mark.django_db
def test_clean_apply_at_by_executer_person():
    can_execute_permission = Permission.objects.get(codename='can_execute_department_proposals')

    executer = StaffFactory(login='executer').user
    executer.user_permissions.add(can_execute_permission)

    yesterday = (MockedDate.today() - datetime.timedelta(days=1))

    with mock.patch('staff.proposal.forms.proposal.date', MockedDate):
        with mock.patch('staff.proposal.forms.proposal.is_first_date_required', lambda proposal_data: False):
            # executor_author
            proposal_form = ProposalForm(base_initial={'author_user': executer})
            assert proposal_form.clean_apply_at(yesterday) == yesterday


@pytest.mark.django_db
def test_form_no_actions_error(mocked_mongo):
    form = ProposalForm(
        data={
            'apply_at': '2032-12-01',
            'description': '',
            'actions': [],
            'persons': {'actions': []},
            'vacancies': {'actions': []},
        }
    )
    assert not form.is_valid()
    assert form.errors['errors'][''][0]['code'] == 'no_actions_error'


@pytest.mark.django_db
def test_form_no_headcounts(mocked_mongo, company):
    DepartmentStaffFactory(
        department=company.dep1,
        role_id=DepartmentRoles.CHIEF.value,
        staff=company.persons['dep2-chief'],
    )

    headcount_action = {
        'action_id': 'act_41535',
        'headcount_code': '127981',
        'department': company.dep1.url,
        'fake_department': '',
        'value_stream': '',
    }

    data = {
        'apply_at': '2032-12-01',
        'description': '',
        'actions': [],
        'persons': {'actions': []},
        'vacancies': {'actions': []},
        'headcounts': {'actions': [headcount_action]}
    }

    form = ProposalForm(
        base_initial={'author_user': company.persons['dep1-chief'].user},
        initial_old_style=data,
        data=data,
    )
    assert not form.is_valid()
    errors = form.errors.get('errors')
    assert errors
    headcount_errors = errors.get('headcounts[actions][0][headcount_code]')
    assert headcount_errors
    assert len(headcount_errors) == 1
    assert headcount_errors[0]['code'] == 'invalid'
    assert errors[''][0]['code'] == 'configuration_error'
