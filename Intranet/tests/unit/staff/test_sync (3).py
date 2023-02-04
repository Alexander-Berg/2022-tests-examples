import mock
import pytest
from django.conf import settings
from django.test import override_settings

from plan.services.models import Service
from plan.staff import tasks
from plan.staff.models import Staff, Department, ServiceScope
from common import factories


class StaffRepositoryMock(object):
    lookups = []

    def __init__(self):
        self.iter_number = 0
        self.pages = self.get_pages()
        StaffRepositoryMock.lookups = []

    def get_pages(self):
        return []

    def get_nopage(self, *args, lookup=None, **kwargs):
        page = self.pages[self.iter_number]
        self.iter_number += 1
        StaffRepositoryMock.lookups.append(lookup)
        return page


class PersonRepositoryMock(StaffRepositoryMock):
    def get_pages(self):
        people = [
            {
                'id': 1,
                'login': 'frodo',
                'uid': '101',
                'official': {
                    'is_dismissed': False,
                    'join_at': None,
                    'quit_at': None,
                    'is_robot': False,
                    'affiliation': 'yandex',
                },
                'department_group': {'id': 101},
                'name': {
                    'first': {'en': 'Frodo', 'ru': 'Фродо'},
                    'last': {'en': 'Baggins', 'ru': 'Бэггинс'},
                },
                'work_email': 'frodo@yandex-team.ru',
                'language': {'ui': 'ru'},
                'personal': {'gender': 'male'},
                'telegram_accounts': [
                    {'value': 'frodo-xxx', 'private': True},
                    {'value': 'frodo-yyy', 'private': True},
                ],
            },
            {
                'id': 2,
                'login': 'sam',
                'uid': '102',
                'official': {
                    'is_dismissed': False,
                    'join_at': None,
                    'quit_at': None,
                    'is_robot': False,
                    'affiliation': 'yandex',
                },
                'department_group': {'id': 101},
                'name': {
                    'first': {'en': 'Samwise', 'ru': 'Сэмуайз'},
                    'last': {'en': 'Gamgee', 'ru': 'Гэмджи'},
                },
                'work_email': 'sam@yande-team.ru',
                'language': {'ui': 'en'},
                'personal': {'gender': 'male'},
                'chief': {'login': 'frodo'},
                'telegram_accounts': [
                    {'value': 'sam-xxx', 'private': True},
                    {'value': 'sam-yyy', 'private': False},
                ],
            },
            {
                'id': 3,
                'login': 'peregrin',
                'uid': '103',
                'official': {
                    'is_dismissed': True,
                    'join_at': None,
                    'quit_at': None,
                    'is_robot': False,
                    'affiliation': 'yandex',
                },
                'department_group': {'id': 102},
                'name': {
                    'first': {'en': 'Peregrin', 'ru': 'Перегрин'},
                    'last': {'en': 'Took', 'ru': 'Тук'},
                },
                'work_email': 'peregrin@yande-team.ru',
                'language': {'ui': 'ru'},
                'personal': {'gender': 'male'},
                'chief': {'login': None},
                'telegram_accounts': [],
            },
            {
                'id': 4,
                'login': 'gollum',
                'uid': '104',
                'official': {
                    'is_dismissed': False,
                    'join_at': None,
                    'quit_at': None,
                    'is_robot': True,
                    'affiliation': 'external'
                },
                'department_group': {'id': 103},
                'name': {
                    'first': {'en': 'Smeagol', 'ru': 'Смеагол'},
                    'last': {'en': '', 'ru': ''},
                },
                'work_email': 'gollum@yandex-team.ru',
                'language': {'ui': 'ru'},
                'personal': {'gender': ''},
                'chief': {'login': None},
                'telegram_accounts': [
                    {'value': 'gollum-xxx', 'private': False},
                    {'value': 'gollum-yyy', 'private': False},
                ],
            },
        ]
        return [people[:2], people[2:], []]


@pytest.mark.parametrize('crowdtest', (False, True))
def test_import_staff(crowdtest):
    department1 = factories.DepartmentFactory(staff_id=101)
    department2 = factories.DepartmentFactory(staff_id=102)
    frodo = factories.StaffFactory(
        login='frodo', uid=101,
        department=department2, is_dismissed=True,
        telegram_account='frodo_smth',
    )
    frodo.user.is_active = False
    frodo.user.save()

    deleted_staff = factories.StaffFactory(staff_id=2, uid=322, department=department1)

    with override_settings(CROWDTEST=crowdtest):
        with mock.patch.object(tasks.PersonStaffImporter, 'staff_repo', new_callable=PersonRepositoryMock):
            tasks.sync_staff_users(delete_duplicates=True)

    lookup = PersonRepositoryMock.lookups[0]
    assert (
        f'and groups.group.id == {settings.STAFF_CROWDTEST_SYNC_GROUP}'
        in lookup['_query']
    ) == crowdtest

    assert Staff.objects.count() == 5  # 4 импортированных стаффа + существующий по умолчанию зомбик
    # refresh_from_db не сбросит кеш связанных объектов
    frodo = Staff.objects.get(login='frodo')
    frodo.fetch_department()
    assert frodo.department == department1
    assert not frodo.is_dismissed
    assert frodo.user.is_active
    assert frodo.first_name == 'Фродо'
    assert frodo.last_name == 'Бэггинс'
    assert frodo.staff_id == 1
    assert frodo.chief is None
    assert frodo.telegram_account is None

    sam = Staff.objects.get(login='sam')
    assert sam.staff_id == 2
    assert sam.chief is None
    assert sam.telegram_account == 'sam-yyy'

    gollum = Staff.objects.get(login='gollum')
    gollum.fetch_department()
    assert not gollum.is_dismissed
    assert gollum.is_robot
    assert gollum.affiliation == 'external'
    assert gollum.department is None
    assert gollum.staff_id == 4
    assert gollum.telegram_account == 'gollum-xxx'

    assert not Staff.objects.filter(login=deleted_staff.login).exists()

    # при следующем полном синке засинкаем шефа Сэма
    with mock.patch.object(tasks.PersonStaffImporter, 'staff_repo', new_callable=PersonRepositoryMock):
        tasks.sync_staff_users(delete_duplicates=True)

    sam = Staff.objects.select_related('chief').get(login='sam')
    assert sam.chief == frodo


class DepartmentRepositoryMock(StaffRepositoryMock):
    @staticmethod
    def get_pages():
        departments = [
            {
                'id': 11,
                'department': {
                    'name': {
                        'full': {'ru': 'full_ru1', 'en': 'full_en1'},
                        'short': {'ru': 'short_ru1', 'en': 'short_en1'},
                    },
                    'heads': [
                        {
                            'person': {'login': 'frodo'},
                            'role': 'chief',
                        },
                        {
                            'person': {'login': 'sam'},
                            'role': 'deputy',
                        }
                    ]
                },
                'url': 'ololo',
            },
            {
                'id': 13,
                'parent': None,
                'department': {
                    'name': {
                        'full': {'ru': 'full_ru3', 'en': 'full_en3'},
                        'short': {'ru': 'short_ru3', 'en': 'short_en3'},
                    },
                    'heads': [],
                },
                'url': 'xxx',
            },
            {
                'id': 13,
                'parent': {'id': 12, 'url': 'bebebe'},
                'department': {
                    'name': {
                        'full': {'ru': 'full_ru3', 'en': 'full_en3'},
                        'short': {'ru': 'short_ru3', 'en': 'short_en3'},
                    },
                    'heads': [],
                },
                'url': 'xxx',
            },
            {
                'id': 12,
                'parent': {'id': 11, 'url': 'ololo'},
                'department': {
                    'name': {
                        'full': {'ru': 'full_ru2', 'en': 'full_en2'},
                        'short': {'ru': 'short_ru2', 'en': 'short_en2'},
                    },
                    'heads': [
                        {
                            'person': {'login': 'gollum'},
                            'role': 'deputy',
                        },
                        {
                            'person': {'login': 'sam'},
                            'role': 'hr_analyst',
                        }
                    ],
                },
                'url': 'bebebe',
            },
            {
                'id': 15,
                'parent': {'id': 14, 'url': 'parenturl'},
                'department': {
                    'name': {
                        'full': {'ru': 'full_ru5', 'en': 'full_en5'},
                        'short': {'ru': 'short_ru5', 'en': 'short_en5'},
                    },
                    'heads': [],
                },
                'url': 'childurl',
            },
            {
                'id': 17,
                'parent': {'id': 16, 'url': 'parenturl2'},
                'department': {
                    'name': {
                        'full': {'ru': 'full_ru7', 'en': 'full_en7'},
                        'short': {'ru': 'short_ru7', 'en': 'short_en7'},
                    },
                    'heads': [],
                },
                'url': 'childurl2',
            },
        ]
        return [departments[:2], departments[2:4], departments[4:], []]


@pytest.mark.parametrize('crowdtest', (False, True))
def test_sync_departments(crowdtest):
    department = factories.DepartmentFactory(staff_id=12, url='bebebe')
    frodo = factories.StaffFactory(login='frodo', department=department)
    factories.StaffFactory(login='sam', department=department)
    # Должно будет удалится после синка
    factories.DepartmentStaffFactory(staff=frodo, department=department, role='C')

    with override_settings(CROWDTEST=crowdtest):
        with mock.patch.object(tasks.DepartmentStaffImporter, 'staff_repo', new_callable=DepartmentRepositoryMock):
            tasks.sync_departments()

    lookup = DepartmentRepositoryMock.lookups[0]
    assert (
        f'and (ancestors.id == {settings.STAFF_CROWDTEST_SYNC_ROOT} or id == {settings.STAFF_CROWDTEST_SYNC_ROOT})'
        in lookup['_query']
    ) == crowdtest

    a, b, c, d, e, f, g = Department.objects.filter(staff_id__in=range(11, 18)).order_by('staff_id')
    assert a.staff_id == 11
    assert a.url == 'ololo'
    assert a.name == 'full_ru1'
    assert a.name_en == 'full_en1'
    assert a.short_name == 'short_ru1'
    assert a.short_name_en == 'short_en1'
    assert a.parent is None
    assert list(a.get_ancestors()) == []
    assert a.departmentstaff_set.count() == 2
    # frodo - руководитель, sam - заместитель
    assert sorted(a.departmentstaff_set.values_list('staff__login', 'role')) == [('frodo', 'C'), ('sam', 'D')]

    assert b.staff_id == 12
    assert b.url == 'bebebe'
    assert b.name == 'full_ru2'
    assert b.name_en == 'full_en2'
    assert b.short_name == 'short_ru2'
    assert b.short_name_en == 'short_en2'
    assert b.parent == a
    assert list(b.get_ancestors()) == [a]
    # по данным от staff-api руковолителей нет, выше создали вручную, проверим, что данные дулаятся после синка
    assert b.departmentstaff_set.count() == 0

    assert c.staff_id == 13
    assert c.url == 'xxx'
    assert c.name == 'full_ru3'
    assert c.name_en == 'full_en3'
    assert c.short_name == 'short_ru3'
    assert c.short_name_en == 'short_en3'
    assert c.parent == b
    assert list(c.get_ancestors().order_by('id')) == sorted([a, b], key=lambda x: x.id)
    # по данным от staff-api руковолителей нет
    assert c.departmentstaff_set.count() == 0

    assert d.staff_id == 14
    assert d.url == 'parenturl'

    assert e.staff_id == 15
    assert e.url == 'childurl'

    assert f.staff_id == 16
    assert f.url == 'parenturl2'

    assert g.staff_id == 17
    assert g.url == 'childurl2'
    assert g.native_lang == 'ru'


class ServiceRepositoryMock(StaffRepositoryMock):

    def __init__(self):
        self.services = Service.objects.order_by('id')
        super(ServiceRepositoryMock, self).__init__()

    def get_pages(self):
        services = [
            {
                'id': 1,
                'service': {
                    'id': self.services[0].id,
                },
            },
            {
                'id': 2,
                'service': {
                    'id': 1000000000,
                },
            },
            {
                'id': 7,
                'service': {
                    'id': self.services[1].id,
                },
            },
            {
                'id': 25,
                'service': {
                    'id': self.services[2].id,
                },
            },
        ]
        return [services[:2], services[2:], []]


class ServiceScopeRepositoryMock(ServiceRepositoryMock):
    def get_pages(self):
        scope = [
            {
                'parent': {
                    'service': {
                        'id': self.services[2].id
                    }
                },
                'role_scope': 'robots_management',
                'id': 165423,
            },
            {
                'parent': {
                    'service': {
                        'id': self.services[0].id
                    }
                },
                'role_scope': 'services_management',
                'id': 265425,
            },
            {
                'parent': {
                    'service': {
                        'id': self.services[1].id
                    }
                },
                'role_scope': 'services_management',
                'id': 365561,
            },
            {
                'parent': {
                    'service': {
                        'id': self.services[2].id
                    }
                },
                'role_scope': 'services_man',
                'id': 424561,
            },
            {
                'parent': {
                    'service': {
                        'id': 1000000000
                    }
                },
                'role_scope': 'services_management',
                'id': 524561,
            },
            {
                'role_scope': 'services_management',
                'id': 624561,
            },
        ]
        return [scope[:2], scope[2:], []]


def test_import_services_scope():
    for _ in range(4):
        factories.ServiceFactory(staff_id=None)

    services = Service.objects.order_by('id')

    scope_1 = factories.RoleScopeFactory(slug=settings.ABC_ROBOTS_MANAGEMENT_SCOPE)
    scope_2 = factories.RoleScopeFactory(slug='services_management')
    factories.ServiceScopeFactory(service=services[2], role_scope=scope_1, staff_id=None)

    with mock.patch.object(tasks.ServiceStaffImporter, 'staff_repo', new_callable=ServiceRepositoryMock):
        with mock.patch.object(tasks.ServiceScopeStaffImporter, 'staff_repo', new_callable=ServiceScopeRepositoryMock):
            tasks.sync_services_scope()

    services = Service.objects.order_by('id')
    assert services[0].staff_id == 1
    assert services[1].staff_id == 7
    assert services[2].staff_id == 25
    assert services[3].staff_id is None

    a, b, c = ServiceScope.objects.order_by('staff_id')

    assert a.staff_id == 165423
    assert a.service == services[2]
    assert a.role_scope == scope_1

    assert b.staff_id == 265425
    assert b.service == services[0]
    assert b.role_scope == scope_2

    assert c.staff_id == 365561
    assert c.service == services[1]
    assert c.role_scope == scope_2
