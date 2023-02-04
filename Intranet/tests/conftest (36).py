import mock
import pytest

import json
from datetime import date, timedelta
from random import randint
from typing import Any, Dict, List, Optional

from django.contrib.auth.models import Permission
from django.core.urlresolvers import reverse

from staff.lib.auth.utils import get_or_create_test_user
from staff.lib.testing import GroupFactory
from staff.lib.utils.date import parse_datetime

from staff.headcounts.tests.factories import HeadcountPositionFactory
from staff.departments.models.hr_deadline import DEADLINE_TYPE, HrDeadline
from staff.groups.models import GROUP_TYPE_CHOICES

from staff.proposal import views
from staff.proposal.models import CityAttrs


@pytest.yield_fixture(autouse=True)
def mocked_login_existence_validator():
    with mock.patch('staff.proposal.forms.person.StarTrekLoginExistenceValidator'):
        yield


@pytest.fixture(autouse=True)
def create_hrdeadlines(db):
    current_month = date(year=3000, month=7, day=1)
    for deadline_type, _ in DEADLINE_TYPE.choices():
        HrDeadline.objects.create(type=deadline_type, month=current_month, date=current_month)


@pytest.fixture
def without_workflows():
    with mock.patch('staff.departments.controllers.proposal.ProposalCtl.update_workflow_changes'):
        yield


@pytest.fixture
def tester():
    test_user = get_or_create_test_user()
    test_user.user_permissions.add(
        Permission.objects.get(codename='can_manage_department_proposals')
    )
    return test_user


@pytest.fixture
def tester_that_can_execute(tester):
    tester.user_permissions.add(
        Permission.objects.get(codename='can_execute_department_proposals')
    )


@pytest.fixture
def get_request(rf, tester):
    def inner(to_reverse, **kwargs):
        url = reverse(to_reverse, kwargs=kwargs)
        request = rf.get(url)
        request.user = tester
        setattr(request, 'service_is_readonly', False)
        setattr(request, 'csrf_processing_done', True)
        return request
    return inner


@pytest.fixture
def post_request(rf, tester):
    def inner(to_reverse, post_data, **kwargs):
        url = reverse(to_reverse, kwargs=kwargs)
        request = rf.post(url, json.dumps(post_data), 'application/json')
        request.user = tester
        setattr(request, 'service_is_readonly', False)
        setattr(request, 'csrf_processing_done', True)
        return request
    return inner


@pytest.fixture
def edit_proposal_get_view(get_request):
    def inner(proposal_id):
        res = views.edit_proposal(
            get_request('proposal-api:edit-proposal', proposal_id=proposal_id),
            proposal_id,
        )
        return json.loads(res.content)
    return inner


@pytest.fixture
def company(company_with_module_scope):
    return company_with_module_scope


@pytest.fixture
def servicegroup(db):
    return GroupFactory(
        url='svc_devoops',
        type=GROUP_TYPE_CHOICES.SERVICE,
        service_id=1,
        department=None,
    )


@pytest.fixture
def department_creating_action(company):
    def _create_department_action(parent_dep_url: str = '', fake_parent_id: str = '', **params) -> Dict[str, Any]:
        assert (
            parent_dep_url or fake_parent_id
            and not (parent_dep_url and fake_parent_id)
        )
        rand = str(randint(11111, 99999))
        action_dict = {
            'sections': ['name', 'hierarchy', 'administration', 'technical'],
            'action_id': f'act_{rand}',
            'url': '',
            'fake_id': f'dep_{rand}',
            'name': {
                'name': 'new dep rus',
                'name_en': 'new dep en',
                'hr_type': 'true',
                'is_correction': 'false',
            },
            'hierarchy': {
                'parent': parent_dep_url,
                'fake_parent': fake_parent_id,
            },
            'administration': {
                'chief': company.persons['dep11-chief'].login,
            },
            'technical': {
                'code': f'dep{rand}',
                'department_type': company.dep11.kind_id,
                'category': 'nontechnical',
                'order': '',
                'allowed_overdraft_percents': '',
            },
            'delete': False,
        }
        return _update_action_dict(action_dict, params, exclude=action_dict['sections'])

    return _create_department_action


@pytest.fixture
def department_moving_action(company):
    def _move_department_action(
            dep_url: str,
            parent_dep_url: str = '',
            fake_parent_id: str = '', **params) -> Dict[str, Any]:
        assert (
            parent_dep_url or fake_parent_id
            and not (parent_dep_url and fake_parent_id)
        )
        rand = str(randint(11111, 99999))
        action_dict = {
            'action_id': f'act_{rand}',
            'sections': ['hierarchy', 'technical'],
            'url': dep_url,
            'fake_id': '',
            'hierarchy': {
                'parent': parent_dep_url,
                'fake_parent': fake_parent_id,
                'changing_duties': 'null',
            },
            'technical': {
                'department_type': company.dep11.kind_id,
                'category': 'technical',
                'order': '0',
                'allowed_overdraft_percents': '',
            },
            'delete': False,
        }
        return _update_action_dict(action_dict, params)

    return _move_department_action


@pytest.fixture
def department_deleting_action(company):
    def _delete_department_action(dep_url: str) -> Dict[str, Any]:
        rand = str(randint(11111, 99999))
        action_dict = {
            'action_id': f'act_{rand}',
            'sections': [],
            'url': dep_url,
            'fake_id': '',
            'delete': True,
        }
        return action_dict

    return _delete_department_action


@pytest.fixture
def department_renaming_action(company, department_moving_action):
    def _move_department_action(
            dep_url: str,
            new_name: str = 'Другое название',
            new_name_en: str = 'Other name', **params) -> Dict[str, Any]:
        action_dict = department_moving_action(dep_url, fake_parent_id='no matter what')
        action_dict['sections'] = ['name']
        action_dict.pop('hierarchy')
        action_dict.pop('technical')
        action_dict['name'] = {
            'name': new_name,
            'name_en': new_name_en,
            'hr_type': 'true',
            'is_correction': 'false',
        }
        return action_dict

    return _move_department_action


@pytest.fixture
def person_editing_action(company, servicegroup):
    def _edit_person_action(login: str, sections: List[str], **params) -> Dict[str, Any]:
        rand = str(randint(11111, 99999))
        if params.get('department'):
            params['fake_department'] = ''
        elif params.get('fake_department'):
            params['department'] = ''

        changes = {
            'salary': {
                'old_currency': 'RUB',
                'old_salary': '100500',
                'old_rate': '1',
                'old_wage_system': 'fixed',
                'new_currency': 'RUB',
                'new_salary': '500100',
                'new_rate': None,
                'new_wage_system': 'hourly',
            },
            'position': {
                'new_position': 'new dolzhnost',
                'position_legal': company.positions[123].code,
            },
            'grade': {'new_grade': '+1'},
            'department': {
                'department': company.dep2.url,
                'fake_department': '',
                'with_budget': 'true',
                'from_maternity_leave': 'false',
                'vacancy_url': '',
                'service_groups': [servicegroup.url],
                'changing_duties': 'false',
            },
            'office': {
                'office': company.offices['MRP'].id,
            },
            'organization': {
                'organization': company.organizations['yandex_tech'].id,
            },
        }
        action_dict = {
            'sections': sections,  # ['salary', 'position', 'grade', 'department', 'office', 'organization'],
            'action_id': f'act_{rand}',
            'login': login,
            'comment': 'Obosnovanie',
        }
        for section in sections:
            action_dict[section] = changes[section]
        return _update_action_dict(action_dict, params, exclude=list(changes))

    return _edit_person_action


@pytest.fixture
def vacancy_editing_action(company, servicegroup):
    def _edit_vacancy_action(vacancy_id: int, **params) -> Dict[str, Any]:
        rand = str(randint(11111, 99999))
        if params.get('department'):
            params['fake_department'] = ''
        elif params.get('fake_department'):
            params['department'] = ''
        action_dict = {
            'vacancy_id': vacancy_id,
            'action_id': f'act_{rand}',
            'department': company.dep2.url,
            'fake_department': '',
            'value_stream': None,
        }
        return _update_action_dict(action_dict, params)

    return _edit_vacancy_action


@pytest.fixture
def headcount_editing_action(company, servicegroup):
    def _edit_headcount_action(headcount_code: int, **params) -> Dict[str, Any]:
        rand = str(randint(11111, 99999))
        if params.get('department'):
            params['fake_department'] = ''
        elif params.get('fake_department'):
            params['department'] = ''
        action_dict = {
            'headcount_code': headcount_code,
            'action_id': f'act_{rand}',
            'department': company.dep2.url,
            'fake_department': '',
            'value_stream': None,
        }
        return _update_action_dict(action_dict, params)

    return _edit_headcount_action


def _update_action_dict(action_dict: Dict[str, Any],
                        params: Dict[str, Any],
                        exclude: Optional[List[str]] = None) -> Dict[str, Any]:
    exclude = exclude or []
    for key, value in action_dict.items():
        if key in params and key not in exclude:
            action_dict[key] = params[key]
        elif isinstance(value, dict):
            for subkey in set(value) & set(params):
                value[subkey] = params[subkey]
    return action_dict


@pytest.fixture(params=[
    ('proposal1_front_json', 'proposal1_mongo_object'),
    ('proposal2_front_json', 'proposal2_mongo_object'),
    ('proposal3_front_json', 'proposal3_mongo_object'),
    ('proposal4_front_json', 'proposal4_mongo_object'),
    ('proposal6_front_json', 'proposal6_mongo_object'),
])
def proposal_params(request):
    proposal_json, proposal_mongo_object = request.param
    return request.getfixturevalue(proposal_json), request.getfixturevalue(proposal_mongo_object)


@pytest.fixture
def proposal1_front_json(company, department_creating_action, department_moving_action,
                         person_editing_action, deadlines):
    """
    В заявке у сотрудника меняется организация
    и создаётся новое подразделение, куда его переводят.
    """
    once_upon_a_time = date.today() + timedelta(days=145)
    once_upon_a_time.replace(day=1)
    proposal_json = {
        'apply_at': once_upon_a_time.isoformat(),
        'description':
            'В заявке у сотрудника меняется часть про кадры (организация) '
            'и создаётся новое подразделение, куда его переводят.',
        'link_to_ticket': '',
        'departments': {'actions': []},
        'persons': {'actions': []},
        'vacancies': {'actions': []},
        'headcounts': {'actions': []},
    }
    proposal_json['departments']['actions'].append(
        department_creating_action(
            parent_dep_url='yandex_dep2',
            name='Новое подразделение',
        )
    )
    proposal_json['persons']['actions'].append(
        person_editing_action(
            login='dep11-person',
            sections=['organization', 'department'],
            fake_department=proposal_json['departments']['actions'][0]['fake_id'],
        )
    )
    return proposal_json


@pytest.fixture
def proposal2_front_json(company, department_creating_action, department_moving_action,
                         person_editing_action, deadlines):
    """
    В заявке 4 сотрудника, у которых меняются данные в разном сочетании.
    """
    once_upon_a_time = date.today() + timedelta(days=145)
    once_upon_a_time = once_upon_a_time.replace(day=1)
    proposal_json = {
        'apply_at': once_upon_a_time.isoformat(),
        'description':
            'В заявке 4 сотрудника, у которых меняются данные в разном сочетании. '
            'Должно создаться 4 кадровых тикета.',
        'link_to_ticket': '',
        'departments': {'actions': []},
        'persons': {'actions': []},
        'vacancies': {'actions': []},
        'headcounts': {'actions': []},
    }

    proposal_json['persons']['actions'].extend([
        person_editing_action(
            login='dep11-person',
            sections=['position', 'salary'],
            new_position='Должность dep11-person',
        ),
        person_editing_action(
            login='dep12-person',
            sections=['position', 'office'],
            new_position='Должность dep12-person',
        ),
        person_editing_action(
            login='dep1-person',
            sections=['position', 'organization'],
            new_position='Должность dep1-person',
        ),
        person_editing_action(
            login='dep2-person',
            sections=['position'],
            new_position='Должность dep2-person',
        ),
    ])
    return proposal_json


@pytest.fixture
def proposal3_front_json(company, department_creating_action, department_moving_action,
                         person_editing_action, deadlines):
    """
    В заявке 5 сотрудников, у которых меняются должность/подразделение/офис.
    """
    once_upon_a_time = date.today() + timedelta(days=145)
    once_upon_a_time.replace(day=1)
    proposal_json = {
        'apply_at': once_upon_a_time.isoformat(),
        'description':
            'В заявке 5 сотрудников, у которых меняются должность/подразделение/офис. '
            'Должен создаться один тикет на реструктуризацию.',
        'link_to_ticket': '',
        'departments': {'actions': []},
        'persons': {'actions': []},
        'vacancies': {'actions': []},
        'headcounts': {'actions': []},
    }

    proposal_json['persons']['actions'].extend(
        [
            person_editing_action(login='dep11-person', sections=['position', 'department', 'office']),
            person_editing_action(login='dep12-person', sections=['position', 'department', 'office']),
            person_editing_action(login='dep1-person', sections=['position', 'department', 'office']),
            person_editing_action(login='dep111-person', sections=['position', 'department', 'office']),
            person_editing_action(login='dep111-chief', sections=['position', 'department', 'office']),
        ]
    )
    return proposal_json


@pytest.fixture
def proposal4_front_json(company, department_creating_action, department_moving_action,
                         person_editing_action, deadlines):
    """
    В заявке создаётся подразделение и в него переводятся 3 сотрудника.
    """
    once_upon_a_time = date.today() + timedelta(days=145)
    once_upon_a_time.replace(day=1)
    proposal_json = {
        'apply_at': once_upon_a_time.isoformat(),
        'description':
            'Создаётся подразделение и в него переводятся 3 сотрудника. '
            'Должен создаться один тикет на реструктуризацию.',
        'link_to_ticket': '',
        'departments': {'actions': []},
        'persons': {'actions': []},
        'vacancies': {'actions': []},
        'headcounts': {'actions': []},
    }
    fake_id = 'dep_12345'
    proposal_json['departments']['actions'].append(
        department_creating_action(
            fake_id=fake_id,
            parent_dep_url='yandex_dep2',
            name='Новое подразделение',
            code='dep21'
        )
    )
    proposal_json['persons']['actions'].append(
        person_editing_action(
            login='dep11-person',
            sections=['department'],
            fake_department=fake_id,
        )
    )
    proposal_json['persons']['actions'].append(
        person_editing_action(
            login='dep12-person',
            sections=['department'],
            fake_department=fake_id,
        )
    )
    proposal_json['persons']['actions'].append(
        person_editing_action(
            login='dep1-chief',
            sections=['department'],
            fake_department=fake_id,
        )
    )
    return proposal_json


@pytest.fixture
def proposal5_step1_json(person_editing_action):
    """
    Шаблон заявки для 3-х сотрудников на смену должности.
    """
    once_upon_a_time = date.today() + timedelta(days=145)
    once_upon_a_time.replace(day=1)
    proposal_json = {
        'apply_at': once_upon_a_time.isoformat(),
        'description':
            'В заявке 3 сотрудника на смену должности',
        'link_to_ticket': '',
        'departments': {'actions': []},
        'persons': {'actions': []},
        'vacancies': {'actions': []},
        'headcounts': {'actions': []},
    }

    proposal_json['persons']['actions'].extend(
        [
            person_editing_action(login='dep12-person', sections=['position']),
            person_editing_action(login='dep1-person', sections=['position']),
            person_editing_action(login='dep111-chief', sections=['position']),
        ]
    )
    return proposal_json


@pytest.fixture
def proposal6_front_json(company, department_creating_action, vacancy_editing_action):
    """
    Создаётся подразделение и в него переносится вакансия.
    """
    once_upon_a_time = date.today() + timedelta(days=145)
    once_upon_a_time.replace(day=1)
    proposal_json = {
        'apply_at': once_upon_a_time.isoformat(),
        'description':
            'Создаётся подразделение и в него переносится вакансия.',
        'link_to_ticket': '',
        'departments': {'actions': []},
        'persons': {'actions': []},
        'vacancies': {'actions': []},
        'headcounts': {'actions': []},
    }
    proposal_json['departments']['actions'].append(
        department_creating_action(
            parent_dep_url='yandex_dep2',
            name='Новое подразделение',
        )
    )
    proposal_json['vacancies']['actions'].append(
        vacancy_editing_action(
            vacancy_id=company.vacancies['dep111-vac'].id,
            fake_department=proposal_json['departments']['actions'][0]['fake_id'],
        )
    )
    return proposal_json


@pytest.fixture
def proposal7_front_json(company, department_creating_action, headcount_editing_action):
    """
    Создаётся подразделение и в него переносится бюджетная позиция.
    """
    once_upon_a_time = date.today() + timedelta(days=145)
    once_upon_a_time.replace(day=1)
    proposal_json = {
        'apply_at': once_upon_a_time.isoformat(),
        'description':
            'Создаётся подразделение и в него переносится вакансия.',
        'link_to_ticket': '',
        'departments': {'actions': []},
        'persons': {'actions': []},
        'vacancies': {'actions': []},
        'headcounts': {'actions': []},
    }
    proposal_json['departments']['actions'].append(
        department_creating_action(
            parent_dep_url='yandex_dep2',
            name='Новое подразделение',
        )
    )
    proposal_json['headcounts']['actions'].append(
        headcount_editing_action(
            headcount_code=HeadcountPositionFactory().code,
            fake_department=proposal_json['departments']['actions'][0]['fake_id'],
        )
    )
    return proposal_json


# Фикстуры объектов в Монго, которые должны получиться при создании заявки из соответствующего proposal_front_json


@pytest.fixture
def mongo_dict():
    return {
        'apply_at_hr': '2018-05-02',  # Захардкожено, не используется
        'pushed_to_oebs': None,
        'tickets': {
            'department_ticket': '',
            'department_linked_ticket': '',
            'persons': {},
            'splitted_persons': {},
            'restructurisation': '',
            'deleted_persons': {},
            'deleted_restructurisation': '',
            'headcount': '',
            'deleted_headcount': '',
            'value_stream': '',
            'deleted_value_stream': '',
        },
        'actions': [],
        'splitted_actions': [],
        'persons': {
            'actions': [],
            'splitted_actions': [],
        },
        'vacancies': {
            'actions': [],
            'splitted_actions': [],
        },
        'headcounts': {
            'actions': [],
            'splitted_actions': [],
        },
        '_prev_actions_state': {},
    }


@pytest.fixture
def proposal1_mongo_object(company, mongo_dict, proposal1_front_json):
    proposal = proposal1_front_json
    new_dep_chief_login = proposal['departments']['actions'][0]['administration']['chief']
    new_dep_new_parent_url = proposal['departments']['actions'][0]['hierarchy']['parent']

    mongo_dict.update({
        'apply_at': parse_datetime(proposal['apply_at']),
        'description': proposal['description'],
        'root_departments': ['yandex'],
        'actions': [{
            'delete': False,
            'fake_id': proposal['departments']['actions'][0]['fake_id'],
            'id': None,
            'name': {
                'name': 'Новое подразделение',
                'name_en': 'new dep en',
                'hr_type': True,
                'is_correction': False,
            },
            'administration': {
                'chief': company.persons[new_dep_chief_login].id,
                'deputies': [],
            },
            'hierarchy': {
                'parent': company.departments[new_dep_new_parent_url].id,
                'fake_parent': '',
                'changing_duties': None,
            },
            'technical': {
                'position': 0,
                'kind': int(proposal['departments']['actions'][0]['technical']['department_type']),
                'category': 'nontechnical',
                'code': proposal['departments']['actions'][0]['technical']['code'],
                'allowed_overdraft_percents': None,
            },
            '__department_chain__': ['yandex', 'yandex_dep2'],
        }],
    })
    mongo_dict['persons']['actions'] = [
        {
            'action_id': proposal['persons']['actions'][0]['action_id'],
            'sections': ['organization', 'department'],
            'department': {
                'with_budget': True,
                'from_maternity_leave': False,
                'vacancy_url': '',
                'department': None,
                'fake_department': proposal['departments']['actions'][0]['fake_id'],
                'service_groups': ['svc_devoops'],
                'changing_duties': False,
            },
            'organization': {'organization': company.organizations['yandex_tech'].id},
            'comment': 'Obosnovanie',
            'login': 'dep11-person',
            '__department_chain__': ['yandex', 'yandex_dep1', 'yandex_dep1_dep11'],
        },
    ]
    return mongo_dict


@pytest.fixture
def proposal2_mongo_object(company, mongo_dict, proposal2_front_json):
    proposal = proposal2_front_json
    mongo_dict.update({
        'apply_at': parse_datetime(proposal['apply_at']),
        'description': proposal['description'],
        'root_departments': [],
    })
    mongo_dict['persons']['actions'] = [
        {
            'action_id': proposal['persons']['actions'][0]['action_id'],
            'sections': ['position', 'salary'],
            'position': {
                'new_position': 'Должность dep11-person',
                'position_legal': 123,
            },
            'salary': {
                'old_currency': 'RUB',
                'old_salary': '100500',
                'old_rate': '1',
                'old_wage_system': 'fixed',
                'new_currency': 'RUB',
                'new_salary': '500100',
                'new_rate': None,
                'new_wage_system': 'hourly',
            },
            'comment': 'Obosnovanie',
            'login': 'dep11-person',
            '__department_chain__': ['yandex', 'yandex_dep1', 'yandex_dep1_dep11'],
        },
        {
            'action_id': proposal['persons']['actions'][1]['action_id'],
            'sections': ['position', 'office'],
            'position': {
                'new_position': 'Должность dep12-person',
                'position_legal': 123,
            },
            'office': {
                'office': company.offices['MRP'].id,
            },
            'comment': 'Obosnovanie',
            'login': 'dep12-person',
            '__department_chain__': ['yandex', 'yandex_dep1', 'yandex_dep1_dep12'],
        },
        {
            'action_id': proposal['persons']['actions'][2]['action_id'],
            'sections': ['position', 'organization'],
            'position': {
                'new_position': 'Должность dep1-person',
                'position_legal': 123,
            },
            'organization': {
                'organization': company.organizations['yandex_tech'].id,
            },
            'comment': 'Obosnovanie',
            'login': 'dep1-person',
            '__department_chain__': ['yandex', 'yandex_dep1'],
        },
        {
            'action_id': proposal['persons']['actions'][3]['action_id'],
            'sections': ['position'],
            'position': {
                'new_position': 'Должность dep2-person',
                'position_legal': 123,
            },
            'comment': 'Obosnovanie',
            'login': 'dep2-person',
            '__department_chain__': ['yandex', 'yandex_dep2'],
        },
    ]
    return mongo_dict


@pytest.fixture
def proposal3_mongo_object(company, mongo_dict, proposal3_front_json):
    proposal = proposal3_front_json
    mongo_dict.update({
        'apply_at': parse_datetime(proposal['apply_at']),
        'description': proposal['description'],
        'root_departments': [],
    })
    mongo_dict['persons']['actions'] = [
        {
            'action_id': proposal['persons']['actions'][0]['action_id'],
            'sections': ['position', 'department', 'office'],
            'position': {'new_position': 'new dolzhnost', 'position_legal': 123},
            'department': {
                'with_budget': True,
                'from_maternity_leave': False,
                'vacancy_url': '',
                'department': 'yandex_dep2',
                'fake_department': '',
                'service_groups': ['svc_devoops'],
                'changing_duties': False,
            },
            'office': {
                'office': company.offices['MRP'].id,
            },
            'comment': 'Obosnovanie',
            'login': 'dep11-person',
            '__department_chain__': ['yandex', 'yandex_dep1', 'yandex_dep1_dep11'],
        },
        {
            'action_id': proposal['persons']['actions'][1]['action_id'],
            'sections': ['position', 'department', 'office'],
            'position': {'new_position': 'new dolzhnost', 'position_legal': 123},
            'department': {
                'with_budget': True,
                'from_maternity_leave': False,
                'vacancy_url': '',
                'department': 'yandex_dep2',
                'fake_department': '',
                'service_groups': ['svc_devoops'],
                'changing_duties': False,
            },
            'office': {
                'office': company.offices['MRP'].id,
            },
            'comment': 'Obosnovanie',
            'login': 'dep12-person',
            '__department_chain__': ['yandex', 'yandex_dep1', 'yandex_dep1_dep12'],
        },
        {
            'action_id': proposal['persons']['actions'][2]['action_id'],
            'sections': ['position', 'department', 'office'],
            'position': {'new_position': 'new dolzhnost', 'position_legal': 123},
            'department': {
                'with_budget': True,
                'from_maternity_leave': False,
                'vacancy_url': '',
                'department': 'yandex_dep2',
                'fake_department': '',
                'service_groups': ['svc_devoops'],
                'changing_duties': False,
            },
            'office': {
                'office': company.offices['MRP'].id,
            },
            'comment': 'Obosnovanie',
            'login': 'dep1-person',
            '__department_chain__': ['yandex', 'yandex_dep1'],
        },
        {
            'action_id': proposal['persons']['actions'][3]['action_id'],
            'sections': ['position', 'department', 'office'],
            'position': {'new_position': 'new dolzhnost', 'position_legal': 123},
            'department': {
                'with_budget': True,
                'from_maternity_leave': False,
                'vacancy_url': '',
                'department': 'yandex_dep2',
                'fake_department': '',
                'service_groups': ['svc_devoops'],
                'changing_duties': False,
            },
            'office': {
                'office': company.offices['MRP'].id,
            },
            'comment': 'Obosnovanie',
            'login': 'dep111-person',
            '__department_chain__': ['yandex', 'yandex_dep1', 'yandex_dep1_dep11', 'yandex_dep1_dep11_dep111'],
        },
        {
            'action_id': proposal['persons']['actions'][4]['action_id'],
            'sections': ['position', 'department', 'office'],
            'position': {'new_position': 'new dolzhnost', 'position_legal': 123},
            'department': {
                'with_budget': True,
                'from_maternity_leave': False,
                'vacancy_url': '',
                'department': 'yandex_dep2',
                'fake_department': '',
                'service_groups': ['svc_devoops'],
                'changing_duties': False,
            },
            'office': {
                'office': company.offices['MRP'].id,
            },
            'comment': 'Obosnovanie',
            'login': 'dep111-chief',
            '__department_chain__': ['yandex', 'yandex_dep1', 'yandex_dep1_dep11', 'yandex_dep1_dep11_dep111'],
        }
    ]
    return mongo_dict


@pytest.fixture
def proposal4_mongo_object(company, mongo_dict, proposal4_front_json):
    proposal = proposal4_front_json
    new_dep_chief_login = proposal['departments']['actions'][0]['administration']['chief']
    new_dep_new_parent_url = proposal['departments']['actions'][0]['hierarchy']['parent']

    mongo_dict.update({
        'apply_at': parse_datetime(proposal['apply_at']),
        'description': proposal['description'],
        'root_departments': ['yandex'],
        'actions': [
            {
                'delete': False,
                'fake_id': 'dep_12345',
                'id': None,
                'name': {
                    'name': 'Новое подразделение',
                    'name_en': 'new dep en',
                    'hr_type': True,
                    'is_correction': False,
                },
                'administration': {
                    'chief': company.persons[new_dep_chief_login].id,
                    'deputies': [],
                },
                'hierarchy': {
                    'parent': company.departments[new_dep_new_parent_url].id,
                    'fake_parent': '',
                    'changing_duties': None,
                },
                'technical': {
                    'position': 0,
                    'kind': int(proposal['departments']['actions'][0]['technical']['department_type']),
                    'category': 'nontechnical',
                    'code': proposal['departments']['actions'][0]['technical']['code'],
                    'allowed_overdraft_percents': None,
                },
                '__department_chain__': ['yandex', 'yandex_dep2'],
            },
        ],
    })
    mongo_dict['persons']['actions'] = [
        {
            'action_id': proposal['persons']['actions'][0]['action_id'],
            'sections': ['department'],
            'department': {
                'with_budget': True,
                'from_maternity_leave': False,
                'vacancy_url': '',
                'department': None,
                'fake_department': 'dep_12345',
                'service_groups': ['svc_devoops'],
                'changing_duties': False,
            },
            'comment': 'Obosnovanie',
            'login': 'dep11-person',
            '__department_chain__': ['yandex', 'yandex_dep1', 'yandex_dep1_dep11'],
        },
        {
            'action_id': proposal['persons']['actions'][1]['action_id'],
            'sections': ['department'],
            'department': {
                'with_budget': True,
                'from_maternity_leave': False,
                'vacancy_url': '',
                'department': None,
                'fake_department': 'dep_12345',
                'service_groups': ['svc_devoops'],
                'changing_duties': False,
            },
            'comment': 'Obosnovanie',
            'login': 'dep12-person',
            '__department_chain__': ['yandex', 'yandex_dep1', 'yandex_dep1_dep12'],
        },
        {
            'action_id': proposal['persons']['actions'][2]['action_id'],
            'sections': ['department'],
            'department': {
                'with_budget': True,
                'from_maternity_leave': False,
                'vacancy_url': '',
                'department': None,
                'fake_department': 'dep_12345',
                'service_groups': ['svc_devoops'],
                'changing_duties': False,
            },
            'comment': 'Obosnovanie',
            'login': 'dep1-chief',
            '__department_chain__': ['yandex', 'yandex_dep1'],
        }
    ]
    return mongo_dict


@pytest.fixture
def proposal5_mongo_object(company, mongo_dict, proposal5_step1_json):
    proposal = proposal5_step1_json
    mongo_dict.update({
        'apply_at': parse_datetime(proposal['apply_at']),
        'description': proposal['description'],
        'root_departments': [],
    })
    mongo_dict['persons']['actions'] = [
        {
            'action_id': proposal['persons']['actions'][0]['action_id'],
            'sections': ['position'],
            'position': {'new_position': 'new dolzhnost', 'position_legal': 123},
            'comment': 'Obosnovanie',
            'login': 'dep12-person',
            '__department_chain__': ['yandex', 'yandex_dep1', 'yandex_dep1_dep12'],
        },
        {
            'action_id': proposal['persons']['actions'][1]['action_id'],
            'sections': ['position'],
            'position': {'new_position': 'new dolzhnost', 'position_legal': 123},
            'comment': 'Obosnovanie',
            'login': 'dep1-person',
            '__department_chain__': ['yandex', 'yandex_dep1'],
        },
        {
            'action_id': proposal['persons']['actions'][2]['action_id'],
            'sections': ['position'],
            'position': {'new_position': 'new dolzhnost', 'position_legal': 123},
            'comment': 'Obosnovanie',
            'login': 'dep111-chief',
            '__department_chain__': ['yandex', 'yandex_dep1', 'yandex_dep1_dep11', 'yandex_dep1_dep11_dep111'],
        }
    ]
    return mongo_dict


@pytest.fixture
def proposal6_mongo_object(company, mongo_dict, proposal6_front_json):
    proposal = proposal6_front_json
    new_dep_chief_login = proposal['departments']['actions'][0]['administration']['chief']
    new_dep_new_parent_url = proposal['departments']['actions'][0]['hierarchy']['parent']

    mongo_dict.update({
        'apply_at': parse_datetime(proposal['apply_at']),
        'description': proposal['description'],
        'root_departments': ['yandex'],
        'actions': [{
            'delete': False,
            'fake_id': proposal['departments']['actions'][0]['fake_id'],
            'id': None,
            'name': {
                'name': 'Новое подразделение',
                'name_en': 'new dep en',
                'hr_type': True,
                'is_correction': False,
            },
            'administration': {
                'chief': company.persons[new_dep_chief_login].id,
                'deputies': [],
            },
            'hierarchy': {
                'parent': company.departments[new_dep_new_parent_url].id,
                'fake_parent': '',
                'changing_duties': None,
            },
            'technical': {
                'position': 0,
                'kind': int(proposal['departments']['actions'][0]['technical']['department_type']),
                'category': 'nontechnical',
                'code': proposal['departments']['actions'][0]['technical']['code'],
                'allowed_overdraft_percents': None,
            },
            '__department_chain__': ['yandex', 'yandex_dep2'],
        }],
    })
    mongo_dict['vacancies']['actions'] = [
        {
            'action_id': proposal['vacancies']['actions'][0]['action_id'],
            'vacancy_id': company.vacancies['dep111-vac'].id,
            'department': None,
            'fake_department': proposal['departments']['actions'][0]['fake_id'],
            '__department_chain__': ['yandex', 'yandex_dep1', 'yandex_dep1_dep11', 'yandex_dep1_dep11_dep111'],
            'value_stream': None,
            'force_recalculate_schemes': None,
        },
    ]
    return mongo_dict


@pytest.fixture
def proposal7_mongo_object(company, mongo_dict, proposal7_front_json):
    proposal = proposal7_front_json
    new_dep_chief_login = proposal['departments']['actions'][0]['administration']['chief']
    new_dep_new_parent_url = proposal['departments']['actions'][0]['hierarchy']['parent']

    mongo_dict.update({
        'apply_at': parse_datetime(proposal['apply_at']),
        'description': proposal['description'],
        'root_departments': ['yandex'],
        'actions': [{
            'delete': False,
            'fake_id': proposal['departments']['actions'][0]['fake_id'],
            'id': None,
            'name': {
                'name': 'Новое подразделение',
                'name_en': 'new dep en',
                'hr_type': True,
                'is_correction': False,
            },
            'administration': {
                'chief': company.persons[new_dep_chief_login].id,
                'deputies': [],
            },
            'hierarchy': {
                'parent': company.departments[new_dep_new_parent_url].id,
                'fake_parent': '',
                'changing_duties': None,
            },
            'technical': {
                'position': 0,
                'kind': int(proposal['departments']['actions'][0]['technical']['department_type']),
                'category': 'nontechnical',
                'code': proposal['departments']['actions'][0]['technical']['code'],
                'allowed_overdraft_percents': None,
            },
            '__department_chain__': ['yandex', 'yandex_dep2'],
        }],
    })
    mongo_dict['headcounts']['actions'] = [
        {
            'action_id': proposal['headcounts']['actions'][0]['action_id'],
            'headcount_code': company.headcounts['dep111-hc'].code,
            'department': None,
            'fake_department': proposal['departments']['actions'][0]['fake_id'],
            '__department_chain__': ['yandex', 'yandex_dep1', 'yandex_dep1_dep11', 'yandex_dep1_dep11_dep111'],
        },
    ]
    return mongo_dict


# Фикстуры для создания состояния в базе, когда заявка сохранена, но тикетов пока нет


@pytest.fixture
def proposal1(company, post_request, proposal1_front_json, robot_staff_user, mocked_mongo) -> str:
    request = post_request('proposal-api:add-proposal', proposal1_front_json)
    request.user = company.persons['dep12-chief'].user

    with mock.patch('staff.proposal.views.views.tasks.create_proposal_tickets'):
        response = views.add_proposal(request)

    assert response.status_code == 200
    return json.loads(response.content)['proposal_uid']


@pytest.fixture
def proposal2(company, post_request, proposal2_front_json, robot_staff_user, mocked_mongo) -> str:
    request = post_request('proposal-api:add-proposal', proposal2_front_json)
    request.user = company.persons['dep12-chief'].user

    with mock.patch('staff.proposal.views.views.tasks.create_proposal_tickets'):
        response = views.add_proposal(request)

    assert response.status_code == 200
    return json.loads(response.content)['proposal_uid']


@pytest.fixture
def proposal3(company, post_request, proposal3_front_json, robot_staff_user, mocked_mongo) -> str:
    request = post_request('proposal-api:add-proposal', proposal3_front_json)
    request.user = company.persons['dep12-chief'].user

    with mock.patch('staff.proposal.views.views.tasks.create_proposal_tickets'):
        response = views.add_proposal(request)

    assert response.status_code == 200
    return json.loads(response.content)['proposal_uid']


@pytest.fixture
def proposal4(company, post_request, proposal4_front_json, robot_staff_user, mocked_mongo) -> str:
    request = post_request('proposal-api:add-proposal', proposal4_front_json)
    request.user = company.persons['dep12-chief'].user

    with mock.patch('staff.proposal.views.views.tasks.create_proposal_tickets'):
        response = views.add_proposal(request)

    assert response.status_code == 200
    return json.loads(response.content)['proposal_uid']


@pytest.fixture
def proposal5(company, post_request, proposal5_step1_json, robot_staff_user, mocked_mongo) -> str:
    request = post_request('proposal-api:add-proposal', proposal5_step1_json)
    request.user = company.persons['dep12-chief'].user

    with mock.patch('staff.proposal.views.views.tasks.create_proposal_tickets'):
        response = views.add_proposal(request)

    assert response.status_code == 200
    return json.loads(response.content)['proposal_uid']


@pytest.fixture
def proposal7(company, post_request, proposal7_front_json, robot_staff_user, mocked_mongo) -> str:
    request = post_request('proposal-api:add-proposal', proposal7_front_json)
    request.user = company.persons['dep1-hr-analyst'].user

    with mock.patch('staff.proposal.views.views.tasks.create_proposal_tickets'):
        response = views.add_proposal(request)

    assert response.status_code == 200
    return json.loads(response.content)['proposal_uid']


@pytest.fixture(autouse=True)
def default_city_attrs(db):
    CityAttrs.objects.create(city=None, component=None, hr=None)
