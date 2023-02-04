import copy
import json
from typing import Any, Dict

import mock
import pytest
from bson import ObjectId

from staff.budget_position.models.workflow import Workflow
from staff.departments.controllers.tickets import ProposalContext, ProposalTicketDispatcher
from staff.departments.edit.proposal_mongo import MONGO_COLLECTION_NAME
from staff.lib.db import atomic

from staff.proposal import views, models
from staff.proposal.tasks import SyncProposalTickets


@pytest.mark.django_db
def test_ticket_context_on_proposal_1(company, proposal1, proposal1_front_json):
    models.Grade.objects.all().delete()
    context = ProposalContext.from_proposal_id(proposal_id=proposal1)
    creating_dep_fake_id = proposal1_front_json['departments']['actions'][0]['fake_id']

    assert set(context.renaming_department_actions) == set()
    assert set(context.get_creating_department_names()) == {'Новое подразделение'}
    assert set(context.get_deleting_department_names()) == set()
    assert set(context.get_moving_department_names()) == set()
    assert context.get_dep_names() == {creating_dep_fake_id: 'Новое подразделение'}
    assert context.vacancies_data == {}
    assert context.headcounts_data == {}

    persons_data = context.persons_data
    assert set(persons_data) == {'dep11-chief', 'dep11-person', 'dep12-chief'}
    for login in persons_data:
        assert persons_data[login] == {
            'instance': company.persons[login],
            'id': company.persons[login].id,
            'login': login,
            'first_name': company.persons[login].first_name,
            'last_name': company.persons[login].last_name,
            'department_id': company.persons[login].department_id,
            'department__url': company.persons[login].department.url,
            'office_id': company.persons[login].office_id,
            'position': company.persons[login].position,
        }

    departments_data = context.dep_data
    assert set(departments_data) == {
        company.dep2.url,
        company.dep2.id,
        company.dep11.url,
        company.dep11.id,
        creating_dep_fake_id,
    }
    assert departments_data[company.dep2.url] == departments_data[company.dep2.id] == {
        'chain': [
            {'id': company.yandex.id, 'name_en': 'Yandex', 'url': 'yandex', 'name': 'Яндекс'},
            {'id': company.dep2.id, 'name_en': 'dep2', 'url': 'yandex_dep2', 'name': 'Бизнес юниты'}
        ],
        'id': company.dep2.id,
        'instance': company.dep2
    }
    assert departments_data[company.dep11.url] == departments_data[company.dep11.id] == {
        'chain': [
            {'id': company.yandex.id, 'name_en': 'Yandex', 'url': 'yandex', 'name': 'Яндекс'},
            {'id': company.dep1.id, 'name_en': 'dep1', 'url': 'yandex_dep1', 'name': 'Главный бизнес-юнит'},
            {'id': company.dep11.id, 'name_en': 'dep11', 'url': 'yandex_dep1_dep11', 'name': 'dep11'}
        ],
        'id': company.dep11.id,
        'instance': company.dep11,
    }
    assert departments_data[creating_dep_fake_id] == {'ru': 'Новое подразделение', 'en': 'new dep en'}

    department_chains = context.dep_chains
    assert set(department_chains) == {company.dep2.url, company.dep2.id, company.dep11.url, company.dep11.id}
    assert department_chains[company.dep2.url] == department_chains[company.dep2.id] == {
        'chain_ru': 'Яндекс → Бизнес юниты',
        'chain_en': 'Yandex → dep2',
        'ids': [company.yandex.id, company.dep2.id],
        'urls': ['yandex', 'yandex_dep2'],
        'direction': {'chain_ru': 'Яндекс → Бизнес юниты', 'bottom_ids': [company.yandex.id, company.dep2.id]},
        'chiefs': ['yandex-chief', 'dep2-chief'],
        'hr_partners': [['dep2-hr-partner']],   # ??? надо глянуть как это работает
    }
    assert department_chains[company.dep11.url] == department_chains[company.dep11.id] == {
        'chain_ru': 'Яндекс → Главный бизнес-юнит → dep11',
        'chain_en': 'Yandex → dep1 → dep11',
        'ids': [company.yandex.id, company.dep1.id, company.dep11.id],
        'urls': ['yandex', 'yandex_dep1', 'yandex_dep1_dep11'],
        'direction': {
            'chain_ru': 'Яндекс → Главный бизнес-юнит → dep11',
            'bottom_ids': [company.yandex.id, company.dep1.id, company.dep11.id]
        },
        'chiefs': ['yandex-chief', 'dep1-chief', 'dep11-chief'],
        'hr_partners': [['dep1-hr-partner']],
    }

    assert context.positions == {}
    assert context.official_positions == {
        company.persons['dep11-chief'].id: 'Работник',
        company.persons['dep11-person'].id: 'Трудяга',
        company.persons['dep12-chief'].id: 'Работник',
    }
    assert context.offices == {company.offices['KR'].id: company.offices['KR']}
    for org in company.organizations.values():
        assert org.id in context.organizations
        assert context.organizations[org.id] == {
            'id': org.id,
            'name': org.name,
            'name_en': org.name_en,
            'st_translation_id': org.st_translation_id,
        }
    assert context.grades == {}


@pytest.mark.django_db
def test_dispatcher_routing_persons_on_proposal_1(proposal1):
    dispatcher = ProposalTicketDispatcher.from_proposal_id(proposal_id=proposal1)
    assert len(dispatcher.personal) == 1
    assert dispatcher.personal[0]['login'] == 'dep11-person'


@pytest.mark.django_db
def test_dispatcher_routing_persons_on_proposal_2(proposal2):
    dispatcher = ProposalTicketDispatcher.from_proposal_id(proposal_id=proposal2)
    assert len(dispatcher.personal) == 4
    personal_tickets = {action['login'] for action in dispatcher.personal}
    assert personal_tickets == {'dep2-person', 'dep11-person', 'dep12-person', 'dep1-person'}


@pytest.mark.django_db
def test_dispatcher_routing_persons_on_proposal_3(proposal3):
    dispatcher = ProposalTicketDispatcher.from_proposal_id(proposal_id=proposal3)
    assert len(dispatcher.personal) == 0
    assert len(dispatcher.restructurisation) == 5
    r15n_tickets = {action['login'] for action in dispatcher.restructurisation}
    assert r15n_tickets == {'dep1-person', 'dep11-person', 'dep111-chief', 'dep12-person', 'dep111-person'}


@pytest.mark.django_db
def test_dispatcher_routing_persons_on_proposal_4(proposal4):
    dispatcher = ProposalTicketDispatcher.from_proposal_id(proposal_id=proposal4)
    assert len(dispatcher.personal) == 0
    assert len(dispatcher.restructurisation) == 3
    r15n_tickets = {action['login'] for action in dispatcher.restructurisation}
    assert r15n_tickets == {'dep1-chief', 'dep11-person', 'dep12-person'}


@pytest.mark.django_db
def test_dispatcher_routing_persons_on_proposal_5(proposal5):
    dispatcher = ProposalTicketDispatcher.from_proposal_id(proposal_id=proposal5)
    assert len(dispatcher.personal) == 3
    assert len(dispatcher.restructurisation) == 0
    personal_tickets = {action['login'] for action in dispatcher.personal}
    assert personal_tickets == {'dep12-person', 'dep111-chief', 'dep1-person'}


@pytest.mark.django_db
def test_ticket_context_on_proposal_5_step1(company, proposal5):
    models.Grade.objects.all().delete()
    context = ProposalContext.from_proposal_id(proposal_id=proposal5)

    assert set(context.renaming_department_actions) == set()
    assert set(context.get_creating_department_names()) == set()
    assert set(context.get_deleting_department_names()) == set()
    assert set(context.get_moving_department_names()) == set()
    assert context.get_dep_names() == {}
    assert context.vacancies_data == {}
    assert context.headcounts_data == {}

    persons_data = context.persons_data
    assert set(persons_data) == {'dep12-person', 'dep12-chief', 'dep111-chief', 'dep1-person'}
    for login in persons_data:
        assert persons_data[login] == {
            'instance': company.persons[login],
            'id': company.persons[login].id,
            'login': login,
            'first_name': company.persons[login].first_name,
            'last_name': company.persons[login].last_name,
            'department_id': company.persons[login].department_id,
            'department__url': company.persons[login].department.url,
            'office_id': company.persons[login].office_id,
            'position': company.persons[login].position,
        }

    departments_data = context.dep_data
    assert set(departments_data) == {
        company.dep1.url,
        company.dep1.id,
        company.dep12.url,
        company.dep12.id,
        company.dep111.url,
        company.dep111.id,
    }
    assert departments_data[company.dep1.url] == departments_data[company.dep1.id] == {
        'chain': [
            {'id': company.yandex.id, 'name_en': 'Yandex', 'url': 'yandex', 'name': company.yandex.name},
            {'id': company.dep1.id, 'name_en': 'dep1', 'url': 'yandex_dep1', 'name': company.dep1.name}
        ],
        'id': company.dep1.id,
        'instance': company.dep1
    }
    assert departments_data[company.dep12.url] == departments_data[company.dep12.id] == {
        'chain': [
            {'id': company.yandex.id, 'name_en': 'Yandex', 'url': 'yandex', 'name': 'Яндекс'},
            {'id': company.dep1.id, 'name_en': 'dep1', 'url': 'yandex_dep1', 'name': 'Главный бизнес-юнит'},
            {'id': company.dep12.id, 'name_en': 'dep12', 'url': 'yandex_dep1_dep12', 'name': 'dep12'},
        ],
        'id': company.dep12.id,
        'instance': company.dep12,
    }
    assert departments_data[company.dep111.url] == departments_data[company.dep111.id] == {
        'chain': [
            {'id': company.yandex.id, 'name_en': 'Yandex', 'url': 'yandex', 'name': 'Яндекс'},
            {'id': company.dep1.id, 'name_en': 'dep1', 'url': 'yandex_dep1', 'name': 'Главный бизнес-юнит'},
            {'id': company.dep11.id, 'name_en': 'dep11', 'url': 'yandex_dep1_dep11', 'name': 'dep11'},
            {'id': company.dep111.id, 'name_en': 'dep111', 'url': 'yandex_dep1_dep11_dep111', 'name': 'dep111'},
        ],
        'id': company.dep111.id,
        'instance': company.dep111,
    }

    department_chains = context.dep_chains
    assert set(department_chains) == {
        company.dep1.url,
        company.dep1.id,
        company.dep12.url,
        company.dep12.id,
        company.dep111.url,
        company.dep111.id,
    }

    assert context.positions == {123: company.positions[123]}
    assert context.official_positions == {
        company.persons['dep12-person'].id: 'Трудяга',
        company.persons['dep12-chief'].id: 'Работник',
        company.persons['dep111-chief'].id: 'Работник',
        company.persons['dep1-person'].id: 'Трудяга',
    }
    assert context.offices == {company.offices['KR'].id: company.offices['KR']}
    assert context.grades == {}


@pytest.mark.django_db
def test_proposal_5_scenario(
    company,
    post_request,
    proposal5: str,
    proposal5_step1_json: Dict[str, Any],
    department_renaming_action,
    person_editing_action,
    mocked_mongo,
):
    """# Сценарий 5:
    В заявке 3 сотрудника на смену должности,
    добавляю переименование подразделение, затем добавляю еще один перевод.
    Сначала должны создаться 3 тикета на кадровые изменения,
    потом один на реструктуризацию с учетом этих 3х изменений должностей
    с отрывом кадровых, затем перевод добавится в таблицу тикета на реструктуризацию.
    """
    assert Workflow.objects.count() == 3
    collection = mocked_mongo.db[MONGO_COLLECTION_NAME]
    proposal_body = collection.find_one({'_id': ObjectId(proposal5)})

    faked_personal_tickets = {
        proposal_body['persons']['actions'][0]['login']: 'SALARY-123',
        proposal_body['persons']['actions'][1]['login']: 'SALARY-124',
        proposal_body['persons']['actions'][2]['login']: 'SALARY-125',
    }
    # якобы тикеты были созданы
    proposal_context = ProposalContext.from_proposal_id(proposal5)
    proposal_context.proposal.update_tickets(person_tickets=faked_personal_tickets)
    proposal_context.proposal.save()

    # добавляю переименование подразделения
    proposal5_step2_json = copy.deepcopy(proposal5_step1_json)
    proposal5_step2_json['departments']['actions'].append(
        department_renaming_action(dep_url='yandex_dep1_dep12', new_name='Департамент 12')
    )
    request = post_request('proposal-api:edit-proposal', proposal5_step2_json, proposal_id=proposal5)
    request.user = company.persons['dep12-chief'].user  # автор заявки

    with mock.patch(
            'staff.proposal.views.views.tasks.SyncProposalTickets',
            mock.Mock(**{'is_running.return_value': False})) as mocked_sync_task:
        response = views.edit_proposal(request, proposal5)

    # Таска была вызвана с правильными аргументами
    task_correct_args = {
        'author_login': 'dep12-chief',
        'proposal_id': proposal5,
        'deleted_logins': [],
        'updated_logins': [],
        'proposal_diff': {
            'actions': {
                'added': [company.dep12.id],  # в заявку добавлено подразделение dep12
                'deleted': [],
                'updated': [],
            },
            'vacancy_actions_diff': {
                'created': [],
                'deleted': [],
                'updated': [],
            },
            'headcount_actions_diff': {
                'created': [],
                'deleted': [],
                'updated': [],
            },
        },
    }
    mocked_sync_task.assert_called_once_with(**task_correct_args)

    assert response.status_code == 200
    assert json.loads(response.content)['proposal_uid'] == proposal5

    with atomic(savepoint=True):
        # Проверяем что диспатчер теперь говорит что все должны быть в реструктуризации
        dispatcher = ProposalTicketDispatcher.from_proposal_id(proposal_id=proposal5)
        assert len(dispatcher.personal) == 0
        assert len(dispatcher.restructurisation) == 3
        personal_tickets = {action['login'] for action in dispatcher.restructurisation}
        assert personal_tickets == {'dep1-person', 'dep12-person', 'dep111-chief'}

    # теперь вызываем таску по-настоящему с этими же аргументами и мокаем работу с тикетами.
    # Должен создаться один тикет на реструктуризацию с учетом этих 3х изменений должностей
    # и удалиться три персональных тикета
    def mocked_create_r15n_ticket(self):
        self.ticket_key = 'SALARY-987654'

    with mock.patch('staff.proposal.tasks.RestructurisationTicket._create_ticket', mocked_create_r15n_ticket):
        with mock.patch('staff.proposal.tasks.PersonTicket.move_to_r15n') as personal_ticket_mock:
            with atomic(savepoint=True):
                SyncProposalTickets(**task_correct_args)

    assert personal_ticket_mock.call_count == 3  # всех троих перенесли в реструктуризацию
    personal_ticket_mock.assert_called_with(author_login='dep12-chief', r15n_ticket_key='SALARY-987654')

    # помечаем тикеты в заявке удалёнными, как если бы move_to_r15n не был замокан.
    with atomic(savepoint=True):
        proposal_context = ProposalContext.from_proposal_id(proposal5)
        proposal_context.proposal.update_tickets(deleted_person_tickets=faked_personal_tickets)
        proposal_context.proposal.save()

    # затем добавляем еще один перевод сотрудника. Он должен добавиться в тикет реструктуризации.
    proposal5_step3_json = copy.deepcopy(proposal5_step2_json)
    proposal5_step3_json['persons']['actions'].append(
        person_editing_action(
            login='dep2-person',
            sections=['department'],
            department='yandex_dep1',
        )
    )

    request = post_request('proposal-api:edit-proposal', proposal5_step3_json, proposal_id=proposal5)
    request.user = company.persons['dep12-chief'].user  # автор заявки

    with mock.patch(
            'staff.proposal.views.views.tasks.SyncProposalTickets',
            mock.Mock(**{'is_running.return_value': False})) as mocked_sync_task:
        with atomic(savepoint=True):
            response = views.edit_proposal(request, proposal5)

    # Таска была вызвана с правильными аргументами
    task_correct_args = {
        'author_login': 'dep12-chief',
        'proposal_id': proposal5,
        'deleted_logins': [],
        'updated_logins': [],
        'proposal_diff': {
            'vacancy_actions_diff': {'created': [], 'deleted': [], 'updated': []},
            'headcount_actions_diff': {'created': [], 'deleted': [], 'updated': []},
        },
    }
    mocked_sync_task.assert_called_once_with(**task_correct_args)

    assert response.status_code == 200
    assert json.loads(response.content)['proposal_uid'] == proposal5

    #
    with mock.patch('staff.proposal.tasks.RestructurisationTicket.update_ticket') as update_r15n_mock:
        SyncProposalTickets(**task_correct_args)

    # Вызывали обновление тикета реструктуризации
    update_r15n_mock.assert_called_once_with('dep12-chief')
