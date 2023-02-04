from datetime import datetime
import mock

from bson.objectid import ObjectId
import pytest
import sform

from staff.lib.testing import DepartmentFactory
from staff.person.models import Staff
from staff.departments.edit.proposal_mongo import save_mongo_object, MONGO_COLLECTION_NAME
from staff.departments.models import ProposalMetadata, Department

from staff.proposal.forms.department import DepartmentEditForm, find_conflict_proposals


arguments_test_values = [
    (None, None, True),
    ({'name': 'value'}, None, True),
    ({'url': 'yandex_test'}, None, False),
    (None, {'extra': 'data'}, True),
    (None, {'url': 'yandex_test'}, False),
    ({'url': 'yandex_test'}, {'url': 'yandex_test'}, False),
]


@pytest.mark.parametrize('data, initial, is_creating', arguments_test_values)
def test_setting_create_department_flag(data, initial, is_creating):
    form = DepartmentEditForm(data=data, initial=initial)
    assert form.base_initial['creating_department'] is is_creating


def test_fieldset_states():
    not_creating_states = {
        'name': sform.NORMAL,
        'technical': sform.NORMAL,
        'administration': sform.NORMAL,
        'hierarchy': sform.NORMAL,
    }
    creating_states = {
        'name': sform.REQUIRED,
        'technical': sform.REQUIRED,
        'administration': sform.NORMAL,
        'hierarchy': sform.REQUIRED,
    }

    not_creating_form = DepartmentEditForm(data={'url': 'some_yandex_url'})
    creating_form = DepartmentEditForm()

    for field_name, required_state in not_creating_states.items():
        assert not_creating_form.get_field_state(field_name) == required_state

    for field_name, required_state in creating_states.items():
        assert creating_form.get_field_state(field_name) == required_state


@pytest.mark.django_db
def test_department_conflict_detection(company, mocked_mongo):
    # Создаём заявку
    dep11 = Department.objects.get(url='yandex_dep1_dep11')
    dep2 = Department.objects.get(url='yandex_dep2')
    person1 = Staff.objects.get(login='dep11-person')

    mongo_object = {
        'tickets': {
            'department_ticket': 'TSALARY-777',
            'department_linked_ticket': '',
        },
        'description': '',
        'author': person1.id,
        'created_at': '2018-05-03T13:57:32.272',
        'pushed_to_oebs': None,
        'updated_at': '2018-05-03T13:57:32.272',
        'actions': [
            {
                'hierarchy': {
                    'changing_duties': False,
                    'fake_parent': '',
                    'parent': dep2.id,
                },
                'fake_id': '',
                'id': dep11.id,
                'delete': False,
            },
        ],
        'root_departments': ['yandex'],
        'apply_at_hr': '2018-05-02',
        'apply_at': datetime(year=2019, month=6, day=18),
    }
    proposal_id = save_mongo_object(mongo_object)
    meta = ProposalMetadata(proposal_id=proposal_id, applied_at=None)
    meta.save()

    action_data = {
        'url': dep11.url,
        'fake_id': '',
        'delete': False,
        'sections': ['name', 'hierarchy', 'administration', 'technical'],
        'action_id': 'act_23759',
        'name': {
             'name_en': 'HR Department',
             'name': 'HR-департамент 1',
             'hr_type': True,
         },
        'administration': {
            'deputies': [],
            'chief': person1.login,
            'budget_holder': None,
            'hr_partners': [],
        },
        'hierarchy': {
            'changing_duties': False,
            'fake_parent': '',
            'parent': 'yandex',
        },
        'technical': {
            'category': 'nontechnical',
            'order': 40,
            'department_type': dep11.kind_id,
        },
    }

    # считаем невалидным если создаём новую заявку с этим подразделением
    form = DepartmentEditForm(
        action_data,
        base_initial={
            'changes_controlled_by_hr': True,
            'author_user': company.persons['dep1-chief'].user,
        },
    )
    assert form.is_valid() is False
    errors = form.errors
    assert errors == {
        'errors': {
            'url': [
                {
                    'code': 'department_conflict',
                    'params': {'proposal_uid': proposal_id},
                },
            ],
        },
    }

    # валидируем нормально если редактируем эту же заявку
    form = DepartmentEditForm(
        data=action_data,
        base_initial={
            '_id': proposal_id,
            'changes_controlled_by_hr': True,
            'author_user': company.persons['dep1-chief'].user,
        }
    )
    assert form.is_valid()
    assert form.cleaned_data['url'] == dep11.url

    # но считаем ошибкой если редактируем другую заявку
    form = DepartmentEditForm(
        data=action_data,
        base_initial={
            '_id': ''.join(reversed(proposal_id)),
            'changes_controlled_by_hr': True,
            'author_user': company.persons['dep1-chief'].user,
        }
    )
    assert form.is_valid() is False
    errors = form.errors
    assert errors == {
        'errors': {
            'url': [
                {
                    'code': 'department_conflict',
                    'params': {'proposal_uid': proposal_id},
                },
            ],
        },
    }

    # Помечаем заявку выполненной
    meta.applied_at = datetime.now()
    meta.save()

    # Теперь форма будет валидна
    form = DepartmentEditForm(
        data=action_data,
        base_initial={
            'changes_controlled_by_hr': True,
            'author_user': company.persons['dep1-chief'].user,
        },
    )
    assert form.is_valid()


def test_any_changes_provided():
    action_data = {'fake_id': 'fake_12345'}
    form = DepartmentEditForm(data=action_data)
    assert form.is_valid() is False
    form_level_error = form.errors['errors'][''][0]
    assert form_level_error['code'] == 'cannot_be_empty'


@pytest.mark.django_db
def test_find_conflict_proposals(company, mocked_mongo):
    # Создаём заявку
    dep1 = Department.objects.get(url='yandex_dep1_dep11')
    dep2 = Department.objects.get(url='yandex_dep2')
    person1 = Staff.objects.get(login='dep11-person')

    mongo_object = {
        'tickets': {
            'department_ticket': 'TSALARY-777',
            'department_linked_ticket': '',
        },
        'description': '',
        'author': person1.id,
        'created_at': '2018-05-03T13:57:32.272',
        'pushed_to_oebs': None,
        'updated_at': '2018-05-03T13:57:32.272',
        'actions': [
            {
                'hierarchy': {
                    'changing_duties': False,
                    'fake_parent': '',
                    'parent': dep2.id,
                },
                'fake_id': '',
                'id': dep1.id,
                'delete': False,
            },
        ],
        'root_departments': ['yandex'],
        'apply_at_hr': '2018-05-02',
        'apply_at': datetime(year=2019, month=6, day=18),
    }
    proposal_id = save_mongo_object(mongo_object)
    meta = ProposalMetadata(proposal_id=proposal_id, applied_at=None)
    meta.save()

    assert find_conflict_proposals('yandex_dep1_dep11') == [proposal_id]
    assert find_conflict_proposals('yandex_dep1_dep11', exclude=proposal_id) == []
    assert find_conflict_proposals('yandex_dep1') == []


def _action_renaming(url, new_name):
    return {
        'url': url,
        'fake_id': '',
        'delete': False,
        'sections': ['name'],
        'action_id': 'act_23759',
        'name': {
             'name': new_name,
             'name_en': 'namanama',
             'hr_type': True,
         },
    }


def _action_renaming_en(url, new_name_en):
    return {
        'url': url,
        'fake_id': '',
        'delete': False,
        'sections': ['name'],
        'action_id': 'act_23759',
        'name': {
             'name': 'namanama',
             'name_en': new_name_en,
             'hr_type': True,
         },
    }


def _action_creating(parent_url, dep_type_id, new_name=None, new_name_en=None):
    assert new_name or new_name_en
    if new_name:
        action_data = _action_renaming(None, new_name=new_name)
    else:
        action_data = _action_renaming_en(None, new_name_en=new_name_en)
    action_data['fake_id'] = 'fake_884603'
    action_data['sections'] = ['name', 'hierarchy', 'technical']
    action_data['hierarchy'] = {
        'changing_duties': False,
        'fake_parent': '',
        'parent': parent_url,
    }
    action_data['technical'] = {
        'category': 'nontechnical',
        'order': 40,
        'department_type': dep_type_id,
        'code': 'newdepcode',
    }
    return action_data


@pytest.mark.django_db
def test_validate_duplicate_names_on_renaming(company, mocked_mongo):
    """Внутри yandex и outstaff (HR_CONTROLLED_DEPARTMENT_ROOTS) русские названия подразделений
    должны быть уникальны.
    Внутри yamoney, ext, virtual, ... русские названия тоже должны быть уникальны.
    """
    hr_departments = {
        'yandex': company.yandex.tree_id,
        'outstaff': company.outstaff.tree_id,
    }
    # переименовываем dep11 в dep12 (русское название)
    action_data = _action_renaming(company.dep11.url, new_name=company.dep12.name)
    form = DepartmentEditForm(action_data, base_initial={'changes_controlled_by_hr': True})
    with mock.patch('staff.proposal.forms.department.HR_CONTROLLED_DEPARTMENT_ROOTS', hr_departments):
        assert not form.is_valid()
    assert form.errors['errors']['name[name]'] == [
        {'code': 'name_conflict_with_department', 'params': {'conflict_with_department': company.dep12.url}},
    ]

    # переименовываем dep11 в dep12 (англ название)
    action_data = _action_renaming_en(company.dep11.url, new_name_en=company.dep12.name)
    form = DepartmentEditForm(action_data, base_initial={'changes_controlled_by_hr': True})
    with mock.patch('staff.proposal.forms.department.HR_CONTROLLED_DEPARTMENT_ROOTS', hr_departments):
        assert not form.is_valid()
    assert form.errors['errors']['name[name_en]'] == [
        {'code': 'name_conflict_with_department', 'params': {'conflict_with_department': company.dep12.url}}
    ]

    # переименовываем dep11 в 'New unique name'
    action_data = _action_renaming(company.dep11.url, 'New unique name')
    form = DepartmentEditForm(action_data, base_initial={'changes_controlled_by_hr': True})
    with mock.patch('staff.proposal.forms.department.HR_CONTROLLED_DEPARTMENT_ROOTS', hr_departments):
        assert form.is_valid()

    # переименовываем dep11 в ext11 - не валидно т.к. ext уникальность теперь глобальная
    action_data = _action_renaming(company.dep11.url, company.ext11.name)
    form = DepartmentEditForm(action_data, base_initial={'changes_controlled_by_hr': True})

    with mock.patch('staff.proposal.forms.department.HR_CONTROLLED_DEPARTMENT_ROOTS', hr_departments):
        assert not form.is_valid()

    # переименовываем ext11 в dep11 - не валидно т.к. уникальность теперь глобальная
    action_data = _action_renaming(company.ext11.url, company.dep11.name)
    form = DepartmentEditForm(action_data, base_initial={'changes_controlled_by_hr': False})
    with mock.patch('staff.proposal.forms.department.HR_CONTROLLED_DEPARTMENT_ROOTS', hr_departments):
        assert not form.is_valid()

    # переименовываем ext11 в removed1 - валидно т.к. removed1 удалено
    action_data = _action_renaming(company.ext11.url, company.removed1.name)
    form = DepartmentEditForm(action_data, base_initial={'changes_controlled_by_hr': False})
    with mock.patch('staff.proposal.forms.department.HR_CONTROLLED_DEPARTMENT_ROOTS', hr_departments):
        assert form.is_valid()


@pytest.mark.django_db
def test_validate_duplicate_names_on_creating(company, mocked_mongo):
    hr_departments = {
        'yandex': company.yandex.tree_id,
        'outstaff': company.outstaff.tree_id,
    }

    # Создание подразделения с уникальным названием
    action_data = _action_creating(company.dep1.url, company.dep11.kind_id, new_name='new unique name')
    form = DepartmentEditForm(
        action_data,
        base_initial={
            'changes_controlled_by_hr': True,
            'author_user': company.persons['dep1-chief'].user,
        })
    with mock.patch('staff.proposal.forms.department.HR_CONTROLLED_DEPARTMENT_ROOTS', hr_departments):
        assert form.is_valid()


@pytest.mark.django_db
def test_validation_duplicate_russian_names_on_creating(company, mocked_mongo):
    hr_departments = {
        'yandex': company.yandex.tree_id,
        'outstaff': company.outstaff.tree_id,
    }

    # Создание подразделения с русским названием другого внутреннего
    action_data = _action_creating(company.dep1.url, company.dep11.kind_id, new_name=company.dep2.name)
    form = DepartmentEditForm(
        action_data,
        base_initial={
            'changes_controlled_by_hr': True,
            'author_user': company.persons['dep1-chief'].user,
        },
    )
    with mock.patch('staff.proposal.forms.department.HR_CONTROLLED_DEPARTMENT_ROOTS', hr_departments):
        assert not form.is_valid()
    assert form.errors['errors']['name[name]'] == [
        {'code': 'name_conflict_with_department', 'params': {'conflict_with_department': 'yandex_dep2'}}
    ]

    # Создание подразделения снаружи с русским названием другого внутреннего - так нельзя, т.к. уникальность глобальная
    action_data = _action_creating(company.ext1.url, company.dep11.kind_id, new_name=company.dep2.name)
    form = DepartmentEditForm(
        action_data,
        base_initial={
            'changes_controlled_by_hr': False,
            'author_user': company.persons['dep1-chief'].user,
        },
    )
    with mock.patch('staff.proposal.forms.department.HR_CONTROLLED_DEPARTMENT_ROOTS', hr_departments):
        assert not form.is_valid()


@pytest.mark.django_db
def test_validation_duplicate_english_names_on_creating(company, mocked_mongo):
    hr_departments = {
        'yandex': company.yandex.tree_id,
        'outstaff': company.outstaff.tree_id,
    }

    # Создание подразделения с английским названием другого внутреннего
    action_data = _action_creating(company.dep1.url, company.dep11.kind_id, new_name_en=company.dep2.name_en)
    form = DepartmentEditForm(
        action_data,
        base_initial={
            'changes_controlled_by_hr': True,
            'author_user': company.persons['dep1-chief'].user,
        }
    )
    with mock.patch('staff.proposal.forms.department.HR_CONTROLLED_DEPARTMENT_ROOTS', hr_departments):
        assert not form.is_valid()
    assert form.errors['errors']['name[name_en]'] == [
        {'code': 'name_conflict_with_department', 'params': {'conflict_with_department': 'yandex_dep2'}}
    ]

    # Создание подразделения снаружи с английским названием другого внутреннего запрещено, т.к. уникальность глобальная
    action_data = _action_creating(company.ext1.url, company.dep11.kind_id, new_name_en=company.dep2.name_en)
    form = DepartmentEditForm(
        action_data,
        base_initial={
            'changes_controlled_by_hr': False,
            'author_user': company.persons['dep1-chief'].user,
        },
    )
    with mock.patch('staff.proposal.forms.department.HR_CONTROLLED_DEPARTMENT_ROOTS', hr_departments):
        assert not form.is_valid()


@pytest.fixture
def create_dep_in_yandex_proposal(company, mocked_mongo):
    name = 'name'
    name_en = 'name_en'
    author_login = 'yandex-person'

    hr_departments = {
        'yandex': company.yandex.tree_id,
        'outstaff': company.outstaff.tree_id,
    }

    proposal_id = mocked_mongo.db[MONGO_COLLECTION_NAME].insert_one(
        {
            'tickets': {
                'department_ticket': '',
                'department_linked_ticket': '',
                'deleted_persons': {},
                'restructurisation': '',
                'persons': {},
            },
            'description': '',
            'author': company.persons[author_login].id,
            'created_at': '2019-10-06T11:54:49.437',
            'pushed_to_oebs': None,
            'updated_at': '2019-10-06T11:54:49.437',
            'actions': [
                {
                    'id': None,
                    'name': {
                        'name': name,
                        'name_en': name_en,
                        'hr_type': True,
                        'is_correction': False,
                    },
                    '__department_chain__': [company.yandex.url],
                    'administration': {'chief': None, 'deputies': []},
                    'delete': False,
                    'fake_id': 'dep_28137',
                    'hierarchy': {
                        'changing_duties': False,
                        'parent': company.yandex.url,
                    },
                    'technical': {
                        'category': 'nontechnical',
                        'code': 'dep28137',
                        'kind': 3,
                        'position': 0
                    }
                }
            ],
            'persons': {'actions': []},
            'root_departments': [company.yandex.url],
            'apply_at_hr': '2018-05-02',
            'apply_at': datetime(2019, 10, 7, 0, 0),
            'vacancies': {
                'actions': [],
            },
            '_prev_actions_state': {},
        }
    ).inserted_id
    ProposalMetadata(proposal_id=str(proposal_id)).save()
    return (
        str(proposal_id),
        name,
        name_en,
        {
            'login': author_login,
            'first_name': company.persons[author_login].first_name,
            'last_name': company.persons[author_login].last_name,
        },
        hr_departments,
    )


@pytest.fixture
def create_dep_in_ext_proposal(company, mocked_mongo, create_dep_in_yandex_proposal):
    proposal_id, name, name_en, author, hr_departments = create_dep_in_yandex_proposal
    mongo_object = mocked_mongo.db[MONGO_COLLECTION_NAME].find_one(
        {'_id': ObjectId(proposal_id)}
    )
    mongo_object['actions'][0].update(
        {
            'hierarchy': {
                'changing_duties': False,
                'parent': company.ext.url,
            },
            '__department_chain__': [company.ext.url],
        }
    )
    mongo_object['root_departments'] = [company.ext.url]

    mocked_mongo.db[MONGO_COLLECTION_NAME].replace_one(
        {'_id': mongo_object['_id']},
        mongo_object,
    )
    return proposal_id, name, name_en, author, hr_departments


@pytest.mark.django_db
def test_creating_ext_dep_name_validation_with_other_proposal(company, create_dep_in_ext_proposal, mocked_mongo):
    proposal_id, name, name_en, author, hr_departments = create_dep_in_ext_proposal

    # Создание подразделения снаружи с английским названием создаваемого в заявке.
    action_data = _action_creating(company.ext.url, company.dep11.kind_id, new_name_en=name_en)
    form = DepartmentEditForm(
        action_data,
        base_initial={
            'changes_controlled_by_hr': False,
            'author_user': company.persons['dep1-chief'].user,
        },
    )
    with mock.patch('staff.proposal.forms.department.HR_CONTROLLED_DEPARTMENT_ROOTS', hr_departments):
        assert not form.is_valid()
    assert form.errors['errors']['name[name_en]'] == [
        {
            'code': 'name_conflict_with_proposal',
            'params': {
                'conflict_with_proposal': proposal_id,
                'conflict_proposal_author_login': author['login'],
                'conflict_proposal_author_first_name': author['first_name'],
                'conflict_proposal_author_last_name': author['last_name'],
            }
        }
    ]

    # Создание подразделения снаружи с русским названием создаваемого в заявке.
    action_data = _action_creating(company.ext.url, company.dep11.kind_id, new_name=name)
    form = DepartmentEditForm(
        action_data,
        base_initial={
            'changes_controlled_by_hr': False,
            'author_user': company.persons['dep1-chief'].user,
        })
    with mock.patch('staff.proposal.forms.department.HR_CONTROLLED_DEPARTMENT_ROOTS', hr_departments):
        assert not form.is_valid()
    assert form.errors['errors']['name[name]'] == [
        {
            'code': 'name_conflict_with_proposal',
            'params': {
                'conflict_with_proposal': proposal_id,
                'conflict_proposal_author_login': author['login'],
                'conflict_proposal_author_first_name': author['first_name'],
                'conflict_proposal_author_last_name': author['last_name'],
            }
        }
    ]

    # При этом создание подразделения снаружи с валидными названиями
    action_data = _action_creating(company.ext.url, company.dep11.kind_id, new_name='uniquename')
    form = DepartmentEditForm(
        action_data,
        base_initial={
            'changes_controlled_by_hr': False,
            'author_user': company.persons['dep1-chief'].user,
        },
    )
    with mock.patch('staff.proposal.forms.department.HR_CONTROLLED_DEPARTMENT_ROOTS', hr_departments):
        assert form.is_valid()


@pytest.mark.django_db
def test_renaming_yandex_dep_name_validation_with_other_proposal(company, create_dep_in_yandex_proposal, mocked_mongo):
    proposal_id, name, name_en, author, hr_departments = create_dep_in_yandex_proposal

    # Переименование подразделения внутри Яндекса с английским названием создаваемого в заявке.
    action_data = _action_renaming_en(company.dep1.url, new_name_en=name_en)
    form = DepartmentEditForm(action_data, base_initial={'changes_controlled_by_hr': True})
    with mock.patch('staff.proposal.forms.department.HR_CONTROLLED_DEPARTMENT_ROOTS', hr_departments):
        assert not form.is_valid()
    assert form.errors['errors']['name[name_en]'] == [
        {
            'code': 'name_conflict_with_proposal',
            'params': {
                'conflict_with_proposal': proposal_id,
                'conflict_proposal_author_login': author['login'],
                'conflict_proposal_author_first_name': author['first_name'],
                'conflict_proposal_author_last_name': author['last_name'],
            }
        }
    ]

    # Переименование подразделения снаружи с русским названием создаваемого в заявке.
    action_data = _action_renaming(company.ext1.url, new_name=name)
    form = DepartmentEditForm(action_data, base_initial={'changes_controlled_by_hr': True})
    with mock.patch('staff.proposal.forms.department.HR_CONTROLLED_DEPARTMENT_ROOTS', hr_departments):
        assert not form.is_valid()
    assert form.errors['errors']['name[name]'] == [
        {
            'code': 'name_conflict_with_proposal',
            'params': {
                'conflict_with_proposal': proposal_id,
                'conflict_proposal_author_login': author['login'],
                'conflict_proposal_author_first_name': author['first_name'],
                'conflict_proposal_author_last_name': author['last_name'],
            }
        }
    ]

    # При этом переименование подразделения снаружи с валидными названиями
    action_data = _action_renaming(company.ext1.url, new_name='uniquename')
    form = DepartmentEditForm(action_data, base_initial={'changes_controlled_by_hr': True})
    with mock.patch('staff.proposal.forms.department.HR_CONTROLLED_DEPARTMENT_ROOTS', hr_departments):
        assert form.is_valid()


@pytest.mark.django_db
def test_creating_dep_name_validation_with_other_proposal(company, create_dep_in_yandex_proposal, mocked_mongo):
    proposal_id, name, name_en, author, hr_departments = create_dep_in_yandex_proposal

    # Создание подразделения в Яндексе с английским названием создаваемого в заявке.
    action_data = _action_creating(company.yandex.url, company.dep11.kind_id, new_name_en=name_en)
    form = DepartmentEditForm(
        action_data,
        base_initial={
            'changes_controlled_by_hr': True,
            'author_user': company.persons['dep1-chief'].user,
        },
    )
    with mock.patch('staff.proposal.forms.department.HR_CONTROLLED_DEPARTMENT_ROOTS', hr_departments):
        assert not form.is_valid()
    assert form.errors['errors']['name[name_en]'] == [
        {
            'code': 'name_conflict_with_proposal',
            'params': {
                'conflict_with_proposal': proposal_id,
                'conflict_proposal_author_login': author['login'],
                'conflict_proposal_author_first_name': author['first_name'],
                'conflict_proposal_author_last_name': author['last_name'],
            }
        }
    ]

    # Создание подразделения в Яндексе с русским названием создаваемого в заявке.
    action_data = _action_creating(company.dep1.url, company.dep11.kind_id, new_name=name)
    form = DepartmentEditForm(
        action_data,
        base_initial={
            'changes_controlled_by_hr': True,
            'author_user': company.persons['dep1-chief'].user,
        },
    )
    with mock.patch('staff.proposal.forms.department.HR_CONTROLLED_DEPARTMENT_ROOTS', hr_departments):
        assert not form.is_valid()
    assert form.errors['errors']['name[name]'] == [
        {
            'code': 'name_conflict_with_proposal',
            'params': {
                'conflict_with_proposal': proposal_id,
                'conflict_proposal_author_login': author['login'],
                'conflict_proposal_author_first_name': author['first_name'],
                'conflict_proposal_author_last_name': author['last_name'],
            }
        }
    ]

    # При этом создание подразделения в Яндексе с валидными названиями работает.
    action_data = _action_creating(company.yandex.url, company.dep11.kind_id, new_name='uniquename')
    form = DepartmentEditForm(
        action_data,
        base_initial={
            'changes_controlled_by_hr': True,
            'author_user': company.persons['dep1-chief'].user,
        },
    )
    with mock.patch('staff.proposal.forms.department.HR_CONTROLLED_DEPARTMENT_ROOTS', hr_departments):
        assert form.is_valid()

    # А также работает создание подразделения снаружи с этими же названиями.
    action_data = _action_creating(company.ext.url, company.dep11.kind_id, new_name=name)
    form = DepartmentEditForm(
        action_data,
        base_initial={
            'changes_controlled_by_hr': False,
            'author_user': company.persons['dep1-chief'].user,
        },
    )
    with mock.patch('staff.proposal.forms.department.HR_CONTROLLED_DEPARTMENT_ROOTS', hr_departments):
        assert form.is_valid()


@pytest.mark.parametrize(
    'has_population, is_valid',
    [
        (True, False),
        (False, True),
    ],
)
@pytest.mark.django_db
def test_validate_delete_with_population(company, mocked_mongo, has_population, is_valid):
    department = DepartmentFactory()

    action_data = {
        'url': department.url,
        'fake_id': '',
        'delete': True,
        'action_id': 'act_23759',
    }
    form = DepartmentEditForm(
        action_data,
        base_initial={
            'changes_controlled_by_hr': True,
            'author_user': company.persons['dep1-chief'].user,
        })
    department_mock = mock.Mock()
    department_mock.has_population.return_value = has_population
    with mock.patch('staff.proposal.forms.department.DepartmentCtl', return_value=department_mock):
        assert form.is_valid() == is_valid
