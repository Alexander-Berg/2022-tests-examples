from datetime import timedelta, date, datetime
from typing import List, Dict, Any

import pytest

from django.conf import settings
from django.core.exceptions import ValidationError

from staff.departments.models import ProposalMetadata
from staff.departments.edit.proposal_mongo import MONGO_COLLECTION_NAME
from staff.lib.testing import StaffFactory, DepartmentFactory

from staff.proposal.forms.department import DepartmentChangesProposalForm
from staff.proposal.forms.proposal import normalize_link, _check_ticket_can_be_linked_to_proposal


def test_clean_apply_at():
    today = date.today()
    yesterday = today - timedelta(days=1)
    tomorrow = today + timedelta(days=1)

    assert DepartmentChangesProposalForm.clean_apply_at(today) == today
    assert DepartmentChangesProposalForm.clean_apply_at(tomorrow) == tomorrow
    with pytest.raises(ValidationError):
        DepartmentChangesProposalForm.clean_apply_at(yesterday)


@pytest.mark.django_db
@pytest.mark.parametrize(
    'ticket_type',
    ['department_ticket', 'department_linked_ticket', 'restructurisation', 'deleted_restructurisation'],
)
def test_check_ticket_can_be_linked(ticket_type, mocked_mongo):
    proposal_dict = {
        'tickets': {
            'department_ticket': '',
            'department_linked_ticket': '',
            'persons': {},
            'splitted_persons': {},
            'restructurisation': '',
            'deleted_persons': {},
            'deleted_restructurisation': '',
        },
        'locked': False,
        'description': '',
        'pushed_to_oebs': None,
        'last_error': None,
        'created_at': '2017-12-04T18:36:56.727',
        'author': StaffFactory(login='testlogin').id,
        'updated_at': '2017-12-04T18:37:09.341',
        'actions': [
            {
                'name': {
                    'name_en': 'test',
                    'name': 'test',
                    'hr_type': True,
                },
                'id': DepartmentFactory(url='test_department'),
                'fake_id': None,
                'delete': False,
            },
        ],
        'root_departments': [
            'yandex'
        ],
        'apply_at_hr': '2017-12-01',
        'apply_at': datetime(year=2018, month=1, day=13)
    }
    ticket_key = f'{settings.PROPOSAL_QUEUE_ID}-1234'
    proposal_dict['tickets'][ticket_type] = ticket_key

    ins = mocked_mongo.db.get_collection(MONGO_COLLECTION_NAME).insert_one(proposal_dict)
    proposal_id = ins.inserted_id
    meta = ProposalMetadata(proposal_id=proposal_id, applied_at=None)
    meta.save()

    with pytest.raises(ValidationError):
        _check_ticket_can_be_linked_to_proposal(ticket_key)

    meta.applied_at = '2011-11-11T11:11:11.111'
    meta.save()

    _check_ticket_can_be_linked_to_proposal(ticket_key)

    meta.applied_at = None
    meta.deleted_at = '2022-12-22T22:22:22.222'
    meta.save()

    _check_ticket_can_be_linked_to_proposal(ticket_key)


links_examples = [
    ('TSALARY-123', True, 'TSALARY-123'),
    ('TSALARY-3726457', True, 'TSALARY-3726457'),
    ('https://st.test.yandex-team.ru/TSALARY-123', True, 'TSALARY-123'),
    ('https://st.test.yandex-team.ru/TSALARY-1', True, 'TSALARY-1'),

    ('STAFF-9388', False, None),
    ('staff.yandex-team.ru/STAFF-9388', False, None),
    ('lorem ipsum', False, None),
]


@pytest.mark.parametrize('link, is_valid, result', links_examples)
def test_link_is_valid(link, is_valid, result):
    if is_valid:
        assert normalize_link(link) == result
    else:
        with pytest.raises(ValueError):
            normalize_link(link)


changing_deps_presets = [
    (['yandex'], True),
    (['dep11', 'out1'], True),
    (['dep111', 'dep2'], True),
    (['dep1', 'outstaff'], True),
    (['ext1'], True),
    (['ext1', 'yam2'], True),
    (['ext', 'yamoney', 'virtual'], True),
    (['ext', 'yamoney', 'virtual', 'yandex'], False),
    (['out11', 'yam11'], False),
    (['dep1', 'virtual_robot'], False),
]


@pytest.mark.django_db
@pytest.mark.parametrize('dep_urls, is_correct', changing_deps_presets)
def test_departments_are_in_same_branches_group(company, mocked_mongo, dep_urls, is_correct):
    def construct_form_data_for_departments(dep_codes: List) -> Dict[str, Any]:
        deps = (company[dep_code] for dep_code in dep_codes)
        return {
            'actions': [
                {
                    'fake_id': '',
                    'url': dep.url,
                    'sections': ['name'],
                    'name': {
                        'name': 'new {}'.format(dep.name),
                        'name_en': 'new {}'.format(dep.name_en),
                        'short_name': '',
                        'short_name_en': '',
                        'hr_type': 'true'
                    },
                    'action_id': 'act_{}{}'.format(dep.id, dep.id)[:10]
                }
                for dep in deps
            ]
        }

    form_data = construct_form_data_for_departments(dep_urls)
    form = DepartmentChangesProposalForm(data=form_data)

    if is_correct:
        assert form.is_valid()
    else:
        assert not form.is_valid()
        assert form.errors['errors']['actions'] == [{'code': 'mixed_department_branches'}]


names_presets = [
    ([('Тындекс', 'Tyndex')], [], []),
    (
        [
             ('подразделение один', 'department one'),
             ('Подразделение один   ', 'department two'),
        ],
        [0, 1],
        [],
    ),

    (
        [
             ('подразделение один', 'department one'),
             ('Подразделение два', 'Department    one'),
        ],
        [], [0, 1]
    ),

    (
        [
            ('подразделение один', 'department one'),
            ('Подразделение два', 'Department two'),
            ('ПодразДеление один', '  department  TWO'),
        ],
        [0, 2], [1, 2]
    ),
    (
        [
            ('подразделение один', 'department one'),
            ('Подразделение два', 'department two'),
            ('Подразделение три', 'department three'),
        ],
        [], []),
]


@pytest.mark.django_db
@pytest.mark.parametrize('name_pairs, errors_for_name, errors_for_name_en', names_presets)
def test_duplicate_names_in_same_proposal(company, name_pairs, errors_for_name, errors_for_name_en, mocked_mongo):
    def construct_form_data_for_departments(dep_name_pairs: List) -> Dict[str, Any]:
        deps = [company.dep1, company.dep2, company.dep11, company.dep111]

        return {
            'actions': [
                {
                    'fake_id': '',
                    'url': deps[i].url,
                    'sections': ['name'],
                    'name': {
                        'name': ru_name,
                        'name_en': en_name,
                        'short_name': '',
                        'short_name_en': '',
                        'hr_type': 'true'
                    },
                    'action_id': 'act_{}{}'.format(deps[i].id, deps[i].id)[:10]
                }
                for i, (ru_name, en_name) in enumerate(dep_name_pairs)
            ]
        }

    form_data = construct_form_data_for_departments(name_pairs)
    form = DepartmentChangesProposalForm(data=form_data)

    if errors_for_name or errors_for_name_en:
        assert not form.is_valid()
        if errors_for_name:
            for index in errors_for_name:
                assert form.errors['errors']['actions[{}][name][name]'.format(index)] == [
                    {'code': 'name_conflict_with_same_proposal'}
                ]
        if errors_for_name_en:
            for index in errors_for_name_en:
                assert form.errors['errors']['actions[{}][name][name_en]'.format(index)] == [
                    {'code': 'name_conflict_with_same_proposal'}
                ]
        # form.errors['errors']['actions'] == [{u'code': u'mixed_department_branches'}]
    else:
        assert form.is_valid()


def test_form_is_not_crashing_on_empty_department():
    # given
    action = {'url': None, 'hierarchy': {'parent': None}, 'action_id': 'some_action_id'}
    form = DepartmentChangesProposalForm(data={'actions': [action]})

    # when
    assert not form.is_valid()


@pytest.mark.django_db
def test_delete_departments_multiple_times(mocked_mongo):
    department = DepartmentFactory()
    form_data = {
        'actions': [
            {
                'fake_id': None,
                'url': dep.url,
                'sections': [],
                'action_id': f'act_1{index}',
                'delete': True,
            }
            for index, dep in enumerate([department, department])
        ]
    }

    form = DepartmentChangesProposalForm(data=form_data)

    assert not form.is_valid()
    assert form.errors['errors']['actions[0][url][url]'][0]['code'] == 'department_conflict'
    assert form.errors['errors']['actions[1][url][url]'][0]['code'] == 'department_conflict'
