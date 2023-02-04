import pytest
import mock

from datetime import date

from django.conf import settings

from staff.departments.tests.factories import VacancyMemberFactory
from staff.lib.auth.utils import get_or_create_test_user
from staff.proposal.forms.proposal import ProposalForm

from staff.departments.models import Vacancy
from staff.departments.controllers import tickets
from staff.departments.controllers.proposal import ProposalCtl
from staff.departments.tests.utils import person


# force using fixtures in all tests inside this module
pytestmark = pytest.mark.usefixtures(
    'company',
    'mocked_mongo',
)


def test_generate_r15n_ticket_context(company, proposal_complex_data, robot_staff_user, deadlines):
    """
        По переданному валидному json формата sform'ы генерируем сложную заявку
        и затем генерируем dict с контекстом для создания тикета реструктуризации.

        === Отладка реструктуризации
        Кадровые тикеты по: [@dep11-chief, @dep12-person]
        В реструктуризацию попадут [@dep2-person, @dep2-chief]
        Подробности:
         **Условия:**
        1. меняется (зарплата ИЛИ ставка ИЛИ система оплаты труда ИЛИ грейд ИЛИ организация) ИЛИ
           (выход из декрета == да ИЛИ перевод без бюджета == да)
        2. Mеняется (подразделение ИЛИ кадровая должность ИЛИ офис) И
           не меняется (зарплата ИЛИ ставка ИЛИ система оплаты труда ИЛИ грейд ИЛИ организация)
        3. Меняется структура синхронизируемых с OEBS веток ИЛИ меняется название синхронизируемого с OEBS подразделения
        Под условие 2 попало 4 сотрудников: [@dep11-chief, @dep12-person, @dep2-person, @dep2-chief]

        **@dep11-chief**: попал в кадровый тикет по 1-му условию
        **@dep12-person**: попал в кадровый тикет по 1-му условию
        **@dep2-person**: попал в тикет реструктуризации по 3-му условию (где < 5 и меняются подразделения)
        **@dep2-chief**: попал в тикет реструктуризации по 3-му условию (где < 5 и меняются подразделения)

    """
    get_or_create_test_user()
    author = company.persons['dep1-chief']
    vacancy1 = Vacancy.objects.get(id=proposal_complex_data['vacancies']['actions'][0]['vacancy_id'])
    vacancy2 = Vacancy.objects.get(id=proposal_complex_data['vacancies']['actions'][1]['vacancy_id'])
    VacancyMemberFactory(vacancy=vacancy1, person=author)
    VacancyMemberFactory(vacancy=vacancy2, person=author)

    proposal_data = proposal_complex_data
    form = ProposalForm(data=proposal_data, base_initial={'author_user': author.user})
    department_mock = mock.Mock()
    department_mock.has_population.return_value = False
    with mock.patch('staff.proposal.forms.department.DepartmentCtl', return_value=department_mock):
        with mock.patch('staff.proposal.forms.person.StarTrekLoginExistenceValidator'):
            assert form.is_valid()

    proposal_ctl = (
        ProposalCtl(author=person('dep12-person'))
        .create_from_cleaned_data(proposal_params=form.cleaned_data_old_style)
    )
    proposal_ctl.save()
    ticket_ctl = tickets.RestructurisationTicket.from_proposal_ctl(proposal_ctl)

    r15n_ticket_context = ticket_ctl.generate_ticket_context()

    dep = company.departments
    pers = company.persons
    kind = company.kinds

    assert set(r15n_ticket_context.keys()) == {
        'description', 'settings',
        'departments', 'persons', 'vacancies',
        'proposal_id', 'apply_at_hr', 'apply_at',
    }

    assert r15n_ticket_context['description'] == 'Описание тестовой заявки'
    assert r15n_ticket_context['settings'] == settings
    assert r15n_ticket_context['proposal_id'] == proposal_ctl.proposal_id
    assert r15n_ticket_context['apply_at'] == proposal_ctl.proposal_object['apply_at'].date()

    departments_context = r15n_ticket_context['departments']
    persons_context = r15n_ticket_context['persons']
    vacancies_context = r15n_ticket_context['vacancies']

    context_deputies = departments_context[3]['update'].pop('deputies')
    assert context_deputies['old'] == []
    assert set(context_deputies['new']) == {pers['dep2-chief'], pers['dep11-person']}

    assert departments_context[0] == {
        'number': 1,
        'fake_id': None,
        'department': dep['yandex_dep1_dep11'],
        'id': dep['yandex_dep1_dep11'].id,
        'department_chain': ['yandex', 'yandex_dep1', 'yandex_dep1_dep11'],
        'create': None,
        'update': {'parent': {'new': dep['yandex_dep2'], 'old': dep['yandex_dep1']}},
        'delete': None
    }

    assert departments_context[1] == {
        'number': 2,
        'fake_id': 'dep_12345',
        'department': None,
        'id': None,
        'department_chain': ['yandex', 'yandex_dep2'],
        'create': {
            'allowed_overdraft_percents': {'value': None},
            'category': {'value': 'technical'},
            'kind': {'value':  kind['51']},
            'code': {'value': 'newdep'},
            'name': {'value': 'new department'},
            'name_en': {'value': 'new-department-en'},
            'is_correction': {'value': False},
            'parent': {'value': dep['yandex_dep2']},
            'fake_parent': {'value': None},
            'changing_duties': {'value': None},
            'chief': {'value': pers['dep2-person']},
            'position': {'value': 0},
            'deputies': {'value': []},
            'delete': False,
        },
        'update': None,
        'delete': None
    }

    assert departments_context[2] == {
        'number': 3,
        'fake_id': None,
        'department': dep['yandex_dep1_dep12'],
        'id': dep['yandex_dep1_dep12'].id,
        'department_chain': ['yandex', 'yandex_dep1', 'yandex_dep1_dep12'],
        'create': None,
        'update': {
            'parent': {'new': None, 'old': dep['yandex_dep1']},
            'fake_parent': {'new': 'new department', 'old': None}
        },
        'delete': None,
    }

    assert departments_context[3] == {
        'number': 4,
        'fake_id': None,
        'department': dep['yandex'],
        'id': dep['yandex'].id,
        'department_chain': ['yandex'],
        'create': None,
        'update': {
            'category': {'new': 'technical', 'old': 'nontechnical'},
            'position': {'new': 1, 'old': 0},
            'kind': {'new': kind['51'], 'old': kind['5']},
            'chief': {'new': pers['dep111-person'], 'old': pers['yandex-chief']},
        },
        'delete': None,
    }

    assert departments_context[4] == {
        'number': 5,
        'fake_id': None,
        'department': dep['yandex_dep1'],
        'id': dep['yandex_dep1'].id,
        'department_chain': ['yandex', 'yandex_dep1'],
        'create': None,
        'update': None,
        'delete': {'delete': True}
    }

    # @dep2-person: попал в тикет реструктуризации по 3-му условию (где < 5 и меняются подразделения)
    # @dep2-chief: попал в тикет реструктуризации по 3-му условию (где < 5 и меняются подразделения)
    pc = {pc['login']: pc for pc in persons_context}

    assert set(pc) == {'dep2-person', 'dep2-chief'}

    assert pc['dep2-chief'] == {
        'login': 'dep2-chief',
        'first_name': pers['dep2-chief'].first_name,
        'last_name': pers['dep2-chief'].last_name,
        'office': {'old': 'Красная роза', 'new': 'office 2'},
        'department': {'new': dep['yandex_dep1_dep11_dep111'], 'old': dep['yandex_dep2']},
        'geography': None,
        'position': {'new': 'Легальная позиция', 'old': ''},
    }

    assert pc['dep2-person'] == {
        'login': 'dep2-person',
        'first_name':  pers['dep2-person'].first_name,
        'last_name': pers['dep2-person'].last_name,
        'office': {'old': 'Красная роза', 'new': ''},
        'department': {'old': dep['yandex_dep2'], 'new': ''},
        'geography': None,
        'position': {'old': '', 'new': 'Humanist'},
    }

    assert vacancies_context[0] == {
        'vacancy_id': vacancy1.id,
        'name': vacancy1.name,
        'ticket': vacancy1.ticket,
        'status': vacancy1.status,
        'headcount_position_code': vacancy1.headcount_position_code,
        'department': {
            'new': {'url': 'yandex_dep1_dep11'},
            'old': {'url': 'yandex_dep1_dep12'}
        },
        'new_hr_product': None,
        'old_hr_product': None,
        'old_geography': None,
        'new_geography': None,
    }

    assert vacancies_context[1] == {
        'vacancy_id': vacancy2.id,
        'name': vacancy2.name,
        'ticket': vacancy2.ticket,
        'status': vacancy2.status,
        'headcount_position_code': vacancy2.headcount_position_code,
        'department': {
            'new': 'new department',
            'old': {'url': 'yandex_dep1_dep11_dep111'},
        },
        'new_hr_product': None,
        'old_hr_product': None,
        'old_geography': None,
        'new_geography': None,
    }


def test_r15n_ticket_params_for_complex_proposal(company, settings, proposal_complex_data, deadlines):
    settings.TURN_ON_PARTNERS_IN_SALARY_TICKETS = True
    get_or_create_test_user()
    author = company.persons['dep1-chief']
    vacancy1 = Vacancy.objects.get(id=proposal_complex_data['vacancies']['actions'][0]['vacancy_id'])
    vacancy2 = Vacancy.objects.get(id=proposal_complex_data['vacancies']['actions'][1]['vacancy_id'])
    VacancyMemberFactory(vacancy=vacancy1, person=author)
    VacancyMemberFactory(vacancy=vacancy2, person=author)

    proposal_data = proposal_complex_data
    form = ProposalForm(data=proposal_data, base_initial={'author_user': author.user})

    department_mock = mock.Mock()
    department_mock.has_population.return_value = False
    with mock.patch('staff.proposal.forms.department.DepartmentCtl', return_value=department_mock):
        with mock.patch('staff.proposal.forms.person.StarTrekLoginExistenceValidator'):
            assert form.is_valid()

    proposal_ctl = (
        ProposalCtl(author=person('dep12-person'))
        .create_from_cleaned_data(proposal_params=form.cleaned_data_old_style)
    )
    proposal_ctl.save()
    ticket_ctl = tickets.RestructurisationTicket.from_proposal_ctl(proposal_ctl)
    r15n_params, _ = ticket_ctl.generate_ticket_params()

    assert r15n_params['assignee'] == 'yandex-person'
    assert r15n_params['staffDate'] == date.today().isoformat()
    assert r15n_params['analytics'] == ['dep2-person', 'dep111-person', 'yandex-chief']
    assert set(r15n_params['followers']) == set()
    assert r15n_params['access'] == []
    assert r15n_params['components'] == []
    assert r15n_params['toCreate'] == 'new department'
    assert r15n_params['toMove'] == 'dep11; dep12'
    assert r15n_params['toDelete'] == 'Главный бизнес-юнит'
