from datetime import date, datetime

import pytest

from staff.groups.models import GROUP_TYPE_CHOICES
from staff.lib.testing import (
    StaffFactory,
    DepartmentFactory,
    GroupFactory,
    OfficeFactory,
)
from staff.lib.utils.date import parse_datetime
from staff.oebs.models import Job
from staff.person.models import Staff

from staff.departments.tests.utils import apply_proposal_dict
from staff.departments.models import DEPARTMENT_CATEGORY, Vacancy, DepartmentKind


@pytest.fixture
def proposal_skeleton(db, mocked_mongo):
    return {
        'tickets': {
            'department_ticket': 'TSALARY-712',
            'department_linked_ticket': '',
            'persons': {},
            'restructurisation': '',
        },
        'locked': False,
        'description': '',
        'author': StaffFactory(login='author-user').id,
        'last_error': None,
        'created_at': parse_datetime('2017-11-30T18:22:13.967'),
        'updated_at': parse_datetime('2017-11-30T18:22:13.967'),
        'actions': [],
        'root_departments': [
            'yandex'
        ],
        'persons': {'actions': []},
        'vacancies': {'actions': []},
        'headcounts': {'actions': []},
        'apply_at_hr': '2017-12-01',
        '_id': '5a204c5538742b00368014f3',
        'apply_at': parse_datetime('2017-11-30T00:00:00'),
    }


@pytest.fixture()
def proposal_dict(proposal_skeleton):
    proposal_skeleton['actions'] = [
        {
            'name': {
                'name_en': 'Field certification group',
                'name': 'Группа выездной сертификации 2',
                'hr_type': True,
                'is_correction': False,
            },
            'fake_id': '',
            'finished': parse_datetime('2017-11-30T18:22:23.476'),
            'id': DepartmentFactory(url='dep_1').id,
            'delete': False
        },
        {
            'name': {
                'name_en': 'SP Ring Group 53',
                'name': 'Группа СЦ МКАД 53 2',
                'hr_type': False,
                'is_correction': False,
            },
            'fake_id': '',
            'finished': parse_datetime('2017-11-30T18:22:23.667'),
            'id': DepartmentFactory(url='dep_2').id,
            'delete': False
        },
        {
            'name': {
                'name_en': 'Brick Group SO',
                'name': 'Группа СЦ Кирпичный 2',
                'hr_type': False,
                'is_correction': True,
            },
            'fake_id': '',
            'finished': ('2017-11-30T18:22:23.863'),
            'id': DepartmentFactory(url='dep_3').id,
            'delete': False
        },
        {
            'name': {
                'name_en': 'Group Novohohlovskaya',
                'name': 'Группа СЦ Новохохловская 2',
                'hr_type': False,
                'is_correction': True,
            },
            'fake_id': '',
            'finished': parse_datetime('2017-11-30T18:22:24.057'),
            'id': DepartmentFactory(url='dep_4').id,
            'delete': False
        }

    ]
    return proposal_skeleton


@pytest.fixture()
def applied_proposal_dict(proposal_dict):
    return apply_proposal_dict(proposal_dict)


@pytest.fixture
def person_from_ya(company):
    return company.persons['dep1-chief']


@pytest.fixture
def proposal_complex_data(company, vacancies):
    """
    Пример сложной заявки с разными видами экшенов основанной на фикстурах company и vacancies
    Структурные изменения:
    0. Перемещение dep11 под dep2,
    1. Создание new_dep под dep2
    2. Удаление dep1
    3. Перемещение dep12 под свежесозданный new_dep
    4. Изменение руководства и info в yandex

    Кадровые измнения:
    1. dep11-person переводим в dep12 с бюджетом
    2. dep11-chief переводим в dep12 без бюджета
    3. dep12-person переводим в создаваемое подразделение new_dep и меняем офис
    4. dep2-person меняем должность
    5. dep2-chief меняем и должность и офис и подразделение (на dep-111)

    Изменения вакансий:
    1. Перенос вакансии из dep12 в dep11
    2. Перенос вакансии из dep111 в новосозданный new_dep (п.2 структурных изменений)
    """

    logins = ['dep11-person', 'dep11-chief', 'dep12-person', 'dep2-person', 'dep1-person']
    persons = {
        p.login: p for p in Staff.objects.filter(login__in=logins)
    }

    office1 = OfficeFactory(name='office 1', city=None, intranet_status=1)
    office2 = OfficeFactory(name='office 2', city=None, intranet_status=1)

    vacancy1 = Vacancy.objects.filter(department__url='yandex_dep1_dep12').first()
    vacancy2 = Vacancy.objects.filter(department__url='yandex_dep1_dep11_dep111').first()

    persons['dep12-person'].office = persons['dep1-person'].office = office1
    persons['dep12-person'].save()
    persons['dep1-person'].save()

    svc_group = GroupFactory(
        name='svc_browser',
        url='svc_browser',
        type=GROUP_TYPE_CHOICES.SERVICE,
        parent=GroupFactory(
            name='__services__',
            department=None,
            url='__services__',
            type=GROUP_TYPE_CHOICES.SERVICE,
            intranet_status=1,
        ),
        intranet_status=1,
    )
    dep_type = DepartmentKind.objects.last()
    # Экшены структурных изменений
    fake_dep_id = 'dep_12345'

    dep11_move_to_dep2 = {
        'action_id': 'act_43997',
        'fake_id': '',
        'url': 'yandex_dep1_dep11',
        'sections': ['hierarchy'],
        'hierarchy': {'parent': 'yandex_dep2', 'fake_parent': '', 'changing_duties': False},
    }
    new_dep_under_dep2 = {
        'action_id': 'act_43901',
        'fake_id': fake_dep_id,
        'url': '',
        'sections': ['name', 'hierarchy', 'administration', 'technical'],
        'name': {'name': 'new department', 'name_en': 'new-department-en'},
        'hierarchy': {'parent': 'yandex_dep2', 'fake_parent': ''},
        'administration': {'chief': 'dep2-person', 'deputies': []},
        'technical': {'category': 'technical', 'code': 'newdep', 'order': '', 'department_type': str(dep_type.id)},
    }
    delete_dep1 = {
        'action_id': 'act_432177',
        'fake_id': '',
        'url': 'yandex_dep1',
        'sections': [],
        'delete': True,
    }
    move_dep12_to_new_dep = {
        'action_id': 'act_43591',
        'fake_id': '',
        'url': 'yandex_dep1_dep12',
        'sections': ['hierarchy'],
        'hierarchy': {'parent': '', 'fake_parent': 'dep_12345', 'changing_duties': False},
    }
    change_administration_adt_tech_in_yandex = {
        'action_id': 'act_23997',
        'fake_id': '',
        'url': 'yandex',
        'sections': ['administration', 'technical'],
        'administration': {'chief': 'dep111-person', 'deputies': ['dep2-chief', 'dep11-person']},
        'technical': {
            'code': '',
            'category': DEPARTMENT_CATEGORY.TECHNICAL,
            'order': 1,
            'department_type': str(dep_type.id)},
    }

    # Экшены кадровых изменений
    move_person_to_dep12_with_budget = {
        'login': 'dep11-person',
        'department': {
            'from_maternity_leave': False,
            'changing_duties': False,
            'vacancy_url': 'https://st.test.yandex-team.ru/TJOB-555',
            'with_budget': True,
            'fake_department': '',
            'department': 'yandex_dep1_dep12',
            'service_groups': [svc_group.url],
        },
        'sections': ['department'],
        'comment': 'without budget',
        'action_id': 'act_70434',
    }
    move_person_to_dep12_without_budget = {
        'login': 'dep11-chief',
        'department': {
            'from_maternity_leave': False,
            'changing_duties': False,
            'vacancy_url': 'https://st.test.yandex-team.ru/TJOB-69',
            'with_budget': False,
            'fake_department': '',
            'department': 'yandex_dep1_dep12',
            'service_groups': [svc_group.url],
        },
        'sections': ['department'],
        'comment': 'without budget',
        'action_id': 'act_70434',
    }
    move_person_to_new_dep_and_change_office = {
        'login': 'dep12-person',
        'department': {
            'department': '',
            'fake_department': 'dep_12345',
            'with_budget': False,
            'changing_duties': False,
            'vacancy_url': 'https://st.test.yandex-team.ru/TJOB-96',
            'from_maternity_leave': False,
            'service_groups': [svc_group.url],
        },
        'office': {
            'office': office2.id,
        },
        'sections': ['department', 'office'],
        'action_id': 'act_49572',
    }
    Job(code=1, name='Humanist', start_date=datetime.now().date()).save()
    Job(code=2, name='Легальная позиция', start_date=datetime.now().date()).save()

    change_position_for_dep2_person = {
        'login': 'dep2-person',
        'position': {
            'position_legal': 1,
            'new_position': 'Humanist'
        },
        'sections': ['position'],
        'action_id': 'act_83046',
        'comment': 'Comment',
    }
    change_all_on_dep2_chief = {
        'login': 'dep2-chief',
        'department': {
            'department': 'yandex_dep1_dep11_dep111',
            'fake_department': '',
            'with_budget': True,
            'vacancy_url': '',
            'changing_duties': False,
            'from_maternity_leave': False,
            'service_groups': [svc_group.url],
        },
        'position': {'new_position': 'Новая позиция', 'position_legal': 2},
        'office': {'office': office2.id},
        'sections': ['position', 'department', 'office'],
        'action_id': 'act_43762',
        'comment': 'Yo!',
    }

    move_vacancy_to_dep11 = {
        'action_id': 'act_66666',
        'vacancy_id': vacancy1.id,
        'department': 'yandex_dep1_dep11',
        'fake_department': '',
    }
    move_vacancy_to_new_dep = {
        'action_id': 'act_88888',
        'vacancy_id': vacancy2.id,
        'department': None,
        'fake_department': fake_dep_id,
    }

    proposal_data = {
        'apply_at': date.today().isoformat(),
        'description': 'Описание тестовой заявки',
        'departments': {
            'link_to_ticket': '',
            'actions': [
                dep11_move_to_dep2,
                new_dep_under_dep2,
                delete_dep1,
                move_dep12_to_new_dep,
                change_administration_adt_tech_in_yandex,
            ],
        },
        'persons': {
            'actions': [
                move_person_to_dep12_with_budget,
                move_person_to_dep12_without_budget,
                move_person_to_new_dep_and_change_office,
                change_position_for_dep2_person,
                change_all_on_dep2_chief,
            ]
        },
        'vacancies': {
            'actions': [
                move_vacancy_to_dep11,
                move_vacancy_to_new_dep,
            ]
        }

    }

    return proposal_data
