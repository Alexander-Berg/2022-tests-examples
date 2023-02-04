import itertools
import pytest

from dataclasses import dataclass, asdict
from unittest.mock import patch

from django.contrib.auth import get_user_model

from ok.core.choices import LANGUAGES
from ok.staff.choices import GROUP_TYPES
from ok.staff.models import Group
from ok.staff.sync import sync_group_memberships, sync_users, sync_groups

from tests import factories as f
from tests.utils.ids import FakeIdsCollection


pytestmark = pytest.mark.django_db


User = get_user_model()


@dataclass
class StaffPerson:

    username: str
    staff_id: int
    uid: str = None
    first_name: str = ''
    last_name: str = ''
    first_name_en: str = ''
    last_name_en: str = ''
    is_dismissed: bool = False
    affiliation: str = 'yandex'
    language: str = LANGUAGES.ru

    @property
    def _uid(self):
        return self.uid or str(self.staff_id)

    @property
    def _email(self):
        return f'{self.username}@yandex-team.ru'

    def as_ok(self):
        data = asdict(self)
        data['uid'] = self._uid
        data['email'] = self._email
        return data

    def as_staff(self):
        return {
            'login': self.username,
            'id': self.staff_id,
            'uid': self._uid,
            'work_email': self._email,
            'name': {
                'first': {'ru': self.first_name, 'en': self.first_name_en},
                'last': {'ru': self.last_name, 'en': self.last_name_en},
            },
            'official': {
                'is_dismissed': self.is_dismissed,
                'affiliation': self.affiliation,
            },
            'language': {
                'ui': self.language,
            },
        }


@dataclass
class StaffGroup:

    url: str
    staff_id: int
    is_deleted: bool = False
    name_ru: str = ''
    name_en: str = ''
    type: str = GROUP_TYPES.service

    def as_ok(self):
        data = asdict(self)
        data['name_en'] = self.name_ru
        return data

    def as_staff(self):
        return {
            'url': self.url,
            'id': self.staff_id,
            'is_deleted': self.is_deleted,
            'name': self.name_ru,
            'type': self.type,
        }


@dataclass
class StaffDepartment(StaffGroup):

    type = GROUP_TYPES.department

    def as_ok(self):
        return asdict(self)

    def as_staff(self):
        data = super().as_staff()
        data['department'] = {
            'name': {
                'full': {
                    'en': self.name_en,
                },
            },
        }
        return data


@pytest.mark.without_yauth_test_user
@patch('ok.staff.sync.get_staff_repo')
def test_users_sync(mocked_get_repo):
    new_person = StaffPerson(
        staff_id=100,
        username='ivan',
        first_name='Иван',
        last_name='Иванов',
        first_name_en='Ivan',
        last_name_en='Ivanov',
    )
    unchanged_person = StaffPerson(
        staff_id=200,
        username='maxim',
        first_name='Максим',
        last_name='Уволенный',
        is_dismissed=True,
    )
    changed_person = StaffPerson(
        staff_id=300,
        username='john',
        first_name='John',
        last_name='External',
        first_name_en='John',
        last_name_en='External',
        language=LANGUAGES.en,
        affiliation='external',
        is_dismissed=True,
    )

    f.UserFactory(**unchanged_person.as_ok())
    f.UserFactory(**changed_person.as_ok())

    changed_person.first_name = 'Джон'
    changed_person.last_name = 'Яндекс'
    changed_person.last_name_en = 'Yandex'
    changed_person.is_dismissed = False
    changed_person.affiliation = 'yandex'
    changed_person.language = LANGUAGES.en

    mocked_get_repo().getiter.return_value = FakeIdsCollection([
        new_person.as_staff(),
        changed_person.as_staff(),
        unchanged_person.as_staff(),
    ])

    sync_users()

    users = list(User.objects.order_by('staff_id'))
    persons = [new_person, unchanged_person, changed_person]
    for user, person in itertools.zip_longest(users, persons):
        for attr, person_value in person.as_ok().items():
            assert getattr(user, attr) == person_value, f'{user.username}: {attr}'



@patch('ok.staff.sync.get_staff_repo')
def test_groups_sync(mocked_get_repo):
    new_group = StaffGroup(
        url='new_group',
        staff_id=100,
        name_ru='Новая группа',
    )
    new_department = StaffDepartment(
        url='new_department',
        staff_id=200,
        name_ru='Новый департамент',
        name_en='New department',
    )
    unchanged_group = StaffGroup(
        url='unchanged_group',
        staff_id=300,
        name_ru='Group',
    )
    changed_group = StaffGroup(
        url='changed_group',
        staff_id=400,
        name_ru='Группа',
    )

    f.GroupFactory(**unchanged_group.as_ok())
    f.GroupFactory(**changed_group.as_ok())

    changed_group.name_ru = 'Спецгруппа'
    changed_group.is_deleted = True

    mocked_get_repo().getiter.return_value = FakeIdsCollection([
        new_group.as_staff(),
        new_department.as_staff(),
        unchanged_group.as_staff(),
        changed_group.as_staff(),
    ])

    sync_groups()

    groups = list(Group.objects.order_by('staff_id'))
    staff_groups = [new_group, new_department, unchanged_group, changed_group]
    for group, staff_group in itertools.zip_longest(groups, staff_groups):
        for attr, value in staff_group.as_ok().items():
            assert getattr(group, attr) == value, f'{group.url}: {attr}'


def test_sync_group_memberships():
    synced, changed, new, not_active = f.GroupFactory.create_batch(4)

    f.ApprovementGroupFactory(group=synced)
    f.ApprovementGroupFactory(group=changed)
    f.ScenarioResponsibleGroupFactory(group=new)

    synced.memberships.create(login='existing-1')
    synced.memberships.create(login='existing-2')
    changed.memberships.create(login='existing-1')
    changed.memberships.create(login='existing-2')
    changed.memberships.create(login='existing-3')
    not_active.memberships.create(login='existing-1')

    mocked_staff_data = [
        {'group_url': synced.url, 'login': 'existing-1'},
        {'group_url': synced.url, 'login': 'existing-2'},
        {'group_url': changed.url, 'login': 'existing-1'},
        {'group_url': changed.url, 'login': 'new-1'},
        {'group_url': new.url, 'login': 'new-1'},
        {'group_url': new.url, 'login': 'new-2'},
    ]

    with patch('ok.staff.sync.get_staff_group_memberships', return_value=mocked_staff_data):
        sync_group_memberships()

    assert set(synced.memberships.values_list('login', flat=True)) == {'existing-1', 'existing-2'}
    assert set(changed.memberships.values_list('login', flat=True)) == {'existing-1', 'new-1'}
    assert set(new.memberships.values_list('login', flat=True)) == {'new-1', 'new-2'}
    assert set(not_active.memberships.values_list('login', flat=True)) == set()
