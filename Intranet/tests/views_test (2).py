from datetime import datetime, date
import json

from bson import ObjectId
import pytest
from mock import patch
import random

from django.core.urlresolvers import reverse

from staff.dismissal.models import DISMISSAL_STATUS
from staff.dismissal.tests.factories import DismissalFactory
from staff.femida.constants import VACANCY_STATUS
from staff.femida.tests.utils import FemidaVacancyFactory
from staff.headcounts.tests.factories import HeadcountPositionFactory
from staff.headcounts.views.headcounts_export_views import hc_export
from staff.lib.mongodb import mongo
from staff.lib.testing import (
    StaffFactory,
    DepartmentFactory,
    RoomFactory,
    TableFactory,
    FloorFactory,
    ContactFactory,
    ContactTypeFactory,
)
from staff.oebs.constants import PERSON_POSITION_STATUS
from staff.oebs.tests.factories import RewardFactory
from staff.person.models import Staff

from staff.departments.controllers.proposal_execution import ProposalExecution
from staff.departments.models import ProposalMetadata, Department
from staff.departments.tests.utils import dep, person
from staff.person.models.contacts import ContactTypeId

pytestmark = pytest.mark.usefixtures(
    'company_with_module_scope',
    'mocked_mongo',
    'check_locking_proposal',
)


@pytest.fixture
@pytest.mark.django_db
def company(company_with_module_scope):
    return company_with_module_scope


@pytest.fixture
def check_locking_proposal(monkeypatch):
    """
    В рантайме декорируем методы DepartmentCtl, проверяя что
     - на момент выполнения работ заявка в монге с локом.
    (Не проверяет при создании подразделения)
    """
    def check_department_lock_decorator(func):
        def edit_department_func(self, dep_id, action_params):
            assert mongo.db.department_edit_proposals.find_one(
                {'_id': ObjectId(self.proposal_id)})['locked']
            return func(self, dep_id, action_params)
        return edit_department_func
    monkeypatch.setattr(
        ProposalExecution,
        'edit_department',
        check_department_lock_decorator(ProposalExecution.edit_department)
    )


@pytest.fixture
def prepared_proposal(company, mocked_mongo, db):
    author = Staff.objects.get(login='dep12-person')
    proposal_dict = {
        'tickets': {
            'department_ticket': 'TSALARY-724',
            'department_linked_ticket': '',
        },
        'locked': False,
        'description': '',
        'pushed_to_oebs': None,
        'last_error': None,
        'created_at': '2017-12-04T18:36:56.727',
        'author': author.id,
        'updated_at': '2017-12-04T18:37:09.341',
        'actions': [{
                'name': {
                    'name_en': 'test',
                    'name': 'test',
                    'hr_type': True,
                    'is_correction': False,
                },
                'hierarchy': {
                    'fake_parent': '',
                    'parent': dep('yandex_dep1_dep11'),
                    'changing_duties': True,
                },
                'fake_id': '_04517221144',
                'technical': {
                    'position': 123,
                    'code': '0451',
                },
                'finished': '2017-12-04T18:37:10.026',
                '_execution_order': 1,
                'id': dep('yandex_dep2').id,
                'delete': False,
            },
            {
                'name': {
                    'name_en': 'Magazine group',
                    'name': 'Группа журнала 12345',
                    'hr_type': False,
                    'is_correction': False,
                },
                'fake_id': '',
                'finished': '2017-12-04T18:37:09.462',
                '_execution_order': 0,
                'id': dep('yandex_dep1_dep11_dep111').id,
                'delete': False,
            }
        ],
        'root_departments': ['yandex'],
        'old_state': {
            dep('yandex_dep2').id: {
                'name': {
                    'name_en': 'Magazine group',
                    'name': 'Группа журнала 12345',
                },
                'hierarchy': {
                    'fake_parent': '',
                    'parent': dep('yandex').id,
                },
                'administration': {
                    'deputies': [],
                    'chief': 3191,
                },
                'technical': {
                    'position': 0,
                    'kind': 11,
                    'code': 'mag',
                },
                'fake_id': '',
                'id': 3939,
                'delete': False,
            }
        },
        'apply_at_hr': '2017-12-01',
        'apply_at': datetime(year=2018, month=1, day=13)
    }
    ins = mocked_mongo.db.get_collection('department_edit_proposals').insert_one(proposal_dict)
    proposal_id = ins.inserted_id
    ProposalMetadata.objects.create(
        proposal_id=proposal_id,
        applied_at='2017-12-01T18:37:09.462',
        applied_by=person('yandex-chief')
    )
    return proposal_id


@pytest.fixture
def department_data(company):
    logins = ['dep2-person', 'dep2-chief']

    persons = Staff.objects.filter(login__in=logins)

    telegram_type_contact = ContactTypeFactory(name='Telegram', id=ContactTypeId.telegram.value)

    def create_data_for_person(person):
        office = person.office

        floor = FloorFactory(office=office, num=random.randint(0, 10))
        room = RoomFactory(floor=floor, num=random.randint(0, 99999))
        table = TableFactory(floor=floor, room=room)

        person.table = table
        person.room = room

        person.save()

        contact = ContactFactory(person=person, contact_type=telegram_type_contact, account_id=person.login + 'tg')

        return {
            'id': person.id,
            'floor': floor,
            'room': room,
            'table': table,
            'telegram': [contact.account_id],
            'work_mode': person.work_mode,
            'home_phone': person.home_phone,
            'position': person.position,
            'join_at': person.join_at,
        }

    res = {}

    for _person in persons:
        res[_person.login] = create_data_for_person(_person)

    return res


def test_moves_history_ok(prepared_proposal, fetcher):
    url = reverse('departments-api:edit-departments:moves-history', kwargs={'department_url': 'yandex_dep2'})
    response = fetcher.get(url + '?from=2017-01-01&to=2018-01-01')
    assert response.status_code == 200
    content = json.loads(response.content)
    assert 'result' in content
    assert content['result'] == [{
        'proposal_id': str(prepared_proposal),
        'applied_at': '2017-12-01T18:37:09.462',
        'changing_duties': True,
    }]


def test_moves_history_404(prepared_proposal, fetcher):
    nonexistent_url = 'yandex_nonexistent_department'
    url = reverse('departments-api:edit-departments:moves-history', kwargs={'department_url': nonexistent_url})
    response = fetcher.get(url + '?from=2017-01-01&to=2018-01-01')
    assert response.status_code == 404
    content = json.loads(response.content)
    assert 'error' in content
    assert content['error'] == 'Department with url {} does not exist'.format(nonexistent_url)


def test_moves_history_400(prepared_proposal, fetcher):
    url = reverse('departments-api:edit-departments:moves-history', kwargs={'department_url': 'yandex_dep2'})
    response = fetcher.get(url)
    assert response.status_code == 400
    content = json.loads(response.content)
    assert 'error' in content
    assert content['error'] == 'Both "from" and "to" parameters are required'


@pytest.mark.django_db()
def test_persons(department_data, fetcher):
    url = reverse('departments-api:persons-url', kwargs={'url': 'yandex_dep2'})
    response = fetcher.get(url)
    assert response.status_code == 200

    content = json.loads(response.content)

    assert 'persons_count' in content
    assert content['persons_count'] == 2

    assert 'departments' in content
    assert len(content['departments']) == 1

    department = content['departments'][0]

    assert 'url' in department
    assert department['url'] == 'yandex_dep2'

    assert 'chain' in department

    assert 'info' in department

    assert department['info']['hr_partners'][0]['login'] == 'dep2-hr-partner'
    assert len(department['info']['persons']) == 2

    response_persons = {person['login']: person for person in department['info']['persons']}

    for _person in department_data:
        assert _person in response_persons
        response_person = response_persons[_person]

        assert response_person['telegram'] == department_data[_person]['telegram']
        assert response_person['room__floor__num'] == department_data[_person]['floor'].num
        assert response_person['room__num'] == department_data[_person]['room'].num
        assert response_person['table__num'] == department_data[_person]['table'].num
        assert response_person['work_mode'] == department_data[_person]['work_mode']
        assert response_person['join_at'] == str(department_data[_person]['join_at'])


@pytest.mark.django_db
def test_tree_section(company, fetcher):
    url = reverse('departments-api:tree-sections')
    response = fetcher.get(url)
    assert response.status_code == 200, response.content

    content = json.loads(response.content)
    assert content == {
        'sections': {
            str(company.section_group.id): {'name': '', 'order': 0, 'description': ''},
            str(company.section_caption.id): {'name': company.section_caption.name, 'order': 0, 'description': ''},
        }
    }, response.content


@pytest.mark.django_db
def test_tree_ancestors_checks_url(company, fetcher):
    url = reverse('departments-api:ancestors', kwargs={'url': company.yandex.url})
    response = fetcher.get(url)
    assert response.status_code == 200

    content = json.loads(response.content)
    assert content == [
        {
            "url": company.yandex.url,
            "name": company.yandex.name,
            "id": company.yandex.id
        }
    ]

    url = reverse('departments-api:ancestors', kwargs={'url': 'no_such_department_url'})
    response = fetcher.get(url)
    assert response.status_code == 404

    content = json.loads(response.content)
    assert content == {
        'errors': [
            'not_found'
        ]
    }


@pytest.mark.django_db
def test_tree_ancestors_for_department(company, fetcher):
    url = reverse('departments-api:ancestors', kwargs={'url': company.dep11.url})
    response = fetcher.get(url)
    assert response.status_code == 200

    content = json.loads(response.content)
    assert content == [
        {
            "url": company.yandex.url,
            "name": company.yandex.name,
            "id": company.yandex.id
        },
        {
            "url": company.dep1.url,
            "name": company.dep1.name,
            "id": company.dep1.id
        },
        {
            "url": company.dep11.url,
            "name": company.dep11.name,
            "id": company.dep11.id
        },
    ]


@pytest.mark.django_db
def test_tree_ancestors_for_valuestream(company, fetcher):
    url = reverse('departments-api:ancestors', kwargs={'url': company.vs_11.url})
    response = fetcher.get(url)
    assert response.status_code == 200

    content = json.loads(response.content)
    assert content == [
        {
            "url": company.vs_root.url,
            "name": company.vs_root.name,
            "id": company.vs_root.id
        },
        {
            "url": company.vs_1.url,
            "name": company.vs_1.name,
            "id": company.vs_1.id
        },
        {
            "url": company.vs_11.url,
            "name": company.vs_11.name,
            "id": company.vs_11.id
        },
    ]


@pytest.mark.django_db
@patch('staff.lib.decorators._is_tvm_request', new=lambda *args, **kwargs: True)
@patch('staff.lib.decorators._check_service_id', new=lambda *args, **kwargs: True)
def test_hc_export_target_is_market(rf):
    market_root_dep = DepartmentFactory(name='Yandex Market Root', url='yandex_monetize_market')
    target_dep = DepartmentFactory(name='Target Department', parent=market_root_dep)
    sub_dep = DepartmentFactory(name='Sub Department', parent=target_dep)
    staff = StaffFactory(is_homeworker=True, date_completion_internship=date(2020, 1, 20), join_at=date(2020, 1, 21))
    reward = RewardFactory(category='Mass Positions')
    headcount = HeadcountPositionFactory(
        status=PERSON_POSITION_STATUS.MATERNITY,
        current_person=staff,
        current_login=staff.login,
        reward_id=reward.scheme_id,
        department=sub_dep,
    )
    femida = FemidaVacancyFactory(
        headcount_position_id=headcount.code,
        startrek_key='job.ticket.url',
        status=VACANCY_STATUS.CLOSED,
    )
    dismissal = DismissalFactory(staff=staff, quit_date=date(2020, 10, 20), status=DISMISSAL_STATUS.DONE)

    url = reverse('departments-api:hc_export', kwargs={'department_id': target_dep.pk})
    request = rf.get(url)
    response = hc_export(request, department_id=target_dep.pk)
    payload = json.loads(response.content)['headcounts']
    assert len(payload) == 1
    payload_headcount = payload[0]
    assert payload_headcount['headcount_id'] == headcount.code
    assert payload_headcount['department_id'] == headcount.department.pk
    assert payload_headcount['parent_department_id'] == headcount.department.parent.pk
    assert payload_headcount['group_id'] == headcount.department.group.pk
    assert payload_headcount['parent_group_id'] == headcount.department.parent.group.pk
    assert payload_headcount['status'] == headcount.status
    assert payload_headcount['login'] == staff.login
    assert payload_headcount['job_tickets'] == [{'startrek_key': femida.startrek_key, 'status': femida.status}]
    assert payload_headcount['is_intern']
    assert payload_headcount['is_maternity']
    assert payload_headcount['is_homeworker']
    assert payload_headcount['category'] == reward.category
    assert payload_headcount['join_at'] == str(staff.join_at)
    assert payload_headcount['quit_date'] == str(dismissal.quit_date)


@pytest.mark.django_db
@patch('staff.lib.decorators._is_tvm_request', new=lambda *args, **kwargs: True)
@patch('staff.lib.decorators._check_service_id', new=lambda *args, **kwargs: True)
def test_hc_export_target_is_not_market(rf):
    target_dep = DepartmentFactory(name='Target Department')
    url = reverse('departments-api:hc_export', kwargs={'department_id': target_dep.pk})
    request = rf.get(url)
    request.auth_mechanism = None
    with pytest.raises(Department.DoesNotExist):
        hc_export(request, department_id=target_dep.pk)
