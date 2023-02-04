# -*- coding: utf-8 -*-
from hamcrest import (
    assert_that,
    has_entries,
    has_length,
    empty,
    greater_than,
    all_of,
    has_key,
    is_not,
    contains,
    contains_inanyorder,
)
from intranet.yandex_directory.src.yandex_directory.auth.scopes import scope

from intranet.yandex_directory.src.yandex_directory.core.models.user import UserModel
from intranet.yandex_directory.src.yandex_directory.core.models.department import DepartmentModel
from intranet.yandex_directory.src.yandex_directory.core.models.group import GroupModel
from intranet.yandex_directory.src.yandex_directory.core.models.service import (
    ServiceModel,
    enable_service,
)
from intranet.yandex_directory.src.yandex_directory.core.views.users import prepare_user
from intranet.yandex_directory.src.yandex_directory.core.utils import prepare_department
from intranet.yandex_directory.src.yandex_directory.core.views.groups import prepare_group
from intranet.yandex_directory.src.yandex_directory.core.utils import only_fields


from testutils import (
    TestCase,
    get_auth_headers,
    get_oauth_headers,
    create_organization,
    oauth_client,
    OAUTH_CLIENT_ID,
)

class BaseMixin(object):
    def setUp(self):
        super(BaseMixin, self).setUp()

        self.department_name = {
            'ru': 'Маркетинг',
            'en': 'Marketing department',
        }
        self.department = DepartmentModel(self.main_connection).create(
            name=self.department_name,
            org_id=self.organization['id']
        )

        self.development_department_name = {
            'ru': 'Разработка',
            'en': 'Development department',
        }
        self.development_department = DepartmentModel(self.main_connection).create(
            name=self.development_department_name,
            org_id=self.organization['id']
        )
        self.bisness_department_name = {
            'ru': 'Разработка сервисов для бизнеса',
            'en': 'Bisness Development department',
        }
        self.bisness_department = DepartmentModel(self.main_connection).create(
            name=self.bisness_department_name,
            org_id=self.organization['id']
        )
        admin_group = GroupModel(self.main_connection).create(
            name={'en': 'Admins group'},
            org_id=self.organization['id']
        )
        managers_group = GroupModel(self.main_connection).create(
            name={'en': 'Managers'},
            org_id=self.organization['id']
        )
        dba_group = GroupModel(self.main_connection).create(
                name={'en': 'DB Admins group'},
                org_id=self.organization['id']
        )

        self.groups = {
            'admins': admin_group,
            'managers': managers_group,
            'dba': dba_group,
        }
        self.groups_ids = [i['id'] for i in list(self.groups.values())]
        self.nickname = 'suggest_test'
        self.name = {
            'first': {
                'ru': 'Тестер',
                'en': 'Tester'
            },
            'last': {
                'ru': 'Автомат',
                'en': 'Auto'
            },
        }
        self.user = UserModel(self.main_connection).create(
            id='1',
            nickname=self.nickname,
            name=self.name,
            email='test@ya.ru',
            gender='male',
            org_id=self.organization['id'],
            department_id=self.department['id'],
            groups=self.groups_ids,
        )
        self.another_user = UserModel(self.main_connection).create(
            id='2',
            nickname=self.nickname + '_test',
            name=self.name,
            email='test@ya.ru',
            gender='male',
            org_id=self.organization['id'],
            department_id=self.department['id'],
            groups=self.groups_ids,
        )

        # чтобы проверить, что suggest не выдает пользователей из
        # других организаций, заведем вторую и в ней пользователя с иным uid
        self.another_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            name={'ru': 'Шашлычная'},
            label='sha',
        )['organization']
        self.department_in_another_org = DepartmentModel(self.main_connection).create(
            name=self.department_name,
            org_id=self.another_organization['id']
        )
        admin_group = GroupModel(self.main_connection).create(
            name={'en': 'Admins group'},
            org_id=self.another_organization['id']
        )
        managers_group = GroupModel(self.main_connection).create(
            name={'en': 'Managers'},
            org_id=self.another_organization['id']
        )

        self.groups_in_another_org = {
            'admins': admin_group,
            'managers': managers_group,
        }
        groups_ids = [i['id'] for i in list(self.groups_in_another_org.values())]
        self.test_in_another_org = UserModel(self.main_connection).create(
            id=1001,
            nickname=self.nickname,
            name=self.name,
            email='test@ya.ru',
            gender='male',
            org_id=self.another_organization['id'],
            department_id=self.department_in_another_org['id'],
            groups=groups_ids,
        )


class TestSuggestUser(BaseMixin, TestCase):
    def test_empty_query(self):
        response_data = self.get_json('/suggest/?text=&limit=1',
                                      headers=get_auth_headers())
        self.assertEqual(response_data['users'], [])
        self.assertEqual(response_data['departments'], [])
        self.assertEqual(response_data['groups'], [])

    def test_special_characters(self):
        response_data = self.get_json('/suggest/?text= %22%26%27',
                                      headers=get_auth_headers())
        self.assertEqual(response_data['users'], [])
        self.assertEqual(response_data['departments'], [])
        self.assertEqual(response_data['groups'], [])

    def test_user_suggest_by_nickname(self):

        response_data = self.get_json(
            '/suggest/?text=suggest_t&limit=1',
            headers=get_auth_headers())

        user = UserModel(self.main_connection).get(
            user_id=self.user['id'],
            fields=[
                '*',
                'department.*',
                'groups.*',
            ],
        )
        expected = prepare_user(
            self.main_connection,
            user,
            expand_contacts=True,
            api_version=1,
        )

        self.assertEqual(len(response_data['users']), 1)
        self.assertEqual(response_data['users'][0], expected)
        self.assertEqual(response_data['departments'], [])
        self.assertEqual(response_data['groups'], [])

    def test_user_suggest_by_name_and_nickname(self):
        response_data = self.get_json(
            '/suggest/?text=Тестер suggest_test&limit=1',
            headers=get_auth_headers())

        user = UserModel(self.main_connection).get(
            user_id=self.user['id'],
            fields=[
                '*',
                'department.*',
                'groups.*',
            ],
        )
        expected = prepare_user(
            self.main_connection,
            user,
            expand_contacts=True,
            api_version=1,
        )

        self.assertEqual(len(response_data['users']), 1)
        self.assertEqual(response_data['users'][0], expected)
        self.assertEqual(response_data['departments'], [])
        self.assertEqual(response_data['groups'], [])

    def test_user_suggest_negative_limit(self):
        # проверяем, что отрицательный limit не ломает запрос
        response_data = self.get_json(
            '/suggest/?text=Тестер suggest_test&limit=-1',
            headers=get_auth_headers())

        self.assertEqual(len(response_data['users']), 2)
        self.assertEqual(response_data['departments'], [])
        self.assertEqual(response_data['groups'], [])

    def test_user_suggest_by_first_symbols(self):
        """
            Проверяет, что работает, когда запрос - не слово из
            русского/английского языка
        """
        response_data = self.get_json(
            '/suggest/?text=Т&limit=1',
            headers=get_auth_headers())

        user = UserModel(self.main_connection).get(
            user_id=self.user['id'],
            fields=[
                '*',
                'department.*',
                'groups.*',
            ],
        )
        expected = prepare_user(
            self.main_connection,
            user,
            expand_contacts=True,
            api_version=1,
        )

        self.assertEqual(len(response_data['users']), 1)
        self.assertEqual(response_data['users'][0], expected)
        self.assertEqual(response_data['departments'], [])
        self.assertEqual(response_data['groups'], [])

        response_data = self.get_json(
            '/suggest/?text=Т&limit=1&domain_only=true',
            headers=get_auth_headers()
        )

        self.assertEqual(response_data['users'], [])
        self.assertEqual(response_data['departments'], [])
        self.assertEqual(response_data['groups'], [])

    def test_user_suggest_query_word_conjunction(self):
        """
            Проверяет, что слова из запроса соединиются функцией AND
            Все слова запроса должны присутвовать в выдаче
        """
        response_data = self.get_json(
            '/suggest/?text=Тес Неавто&limit=1',
            headers=get_auth_headers())
        self.assertEqual(response_data['users'], [])
        self.assertEqual(response_data['departments'], [])
        self.assertEqual(response_data['groups'], [])

        response_data = self.get_json(
            '/suggest/?text=Тес Ав&limit=1',
            headers=get_auth_headers())

        user = UserModel(self.main_connection).get(
            user_id=self.user['id'],
            fields=[
                '*',
                'department.*',
                'groups.*',
            ],
        )

        expected = prepare_user(
            self.main_connection,
            user,
            expand_contacts=True,
            api_version=1,
        )

        self.assertEqual(len(response_data['users']), 1)
        self.assertEqual(response_data['users'][0], expected)
        self.assertEqual(response_data['departments'], [])
        self.assertEqual(response_data['groups'], [])

    def test_check_we_cant_suggest_user_from_another_organization(self):
        data = self.get_json('/suggest/?text=Автомат&limit=10')

        assert_that(data, has_entries(
            users=has_length(2),
            departments=empty(),
            groups=empty(),
        ))


    def test_user_suggest_not_exists(self):
        response_data = self.get_json(
            '/suggest/?text=NotExistTester&limit=5',
            headers=get_auth_headers())
        self.assertEqual(response_data['users'], [])
        self.assertEqual(response_data['departments'], [])
        self.assertEqual(response_data['groups'], [])

    def test_suggest_if_there_is_dot_in_the_name(self):
        # Проверим, что если в имени или фамилии есть точка, то
        # саджест будет работать всё равно
        UserModel(self.main_connection).create(
            id=100500,
            nickname='a.orlov',
            name={'first': {'ru': 'a.orlov'}, 'last': {'ru': 'a.orlov'}},
            email='',
            gender='male',
            org_id=self.organization['id'],
            department_id=1,
            groups=[],
        )
        response_data = self.get_json(
            '/suggest/?text=a.',
            headers=get_auth_headers(),
        )
        assert_that(
            response_data['users'],
            contains(
                has_entries(
                    nickname='a.orlov',
                )
            )
        )

    def test_suggest_index_doesnot_break_user_creation(self):
        # Проверим, что можно использовать некоторые знаки препинания
        # в имени пользователя, и добавление не сломается на
        # вставке такого имени в full-text индекс.

        for idx, char in enumerate(' ,:/\\?-!^_"`\''):
            UserModel(self.main_connection).create(
                id=100500 + idx,
                nickname='suggest-user-{0}'.format(idx),
                name={
                    'first': {'ru': 'First{0}Name'.format(char)},
                    'last': {'ru': 'Last{0}Name'.format(char)},
                },
                email='',
                gender='male',
                org_id=self.organization['id'],
                department_id=1,
                groups=[],
            )


class TestSuggestDepartment(BaseMixin, TestCase):
    def test_suggest_by_name(self):
        response_data = self.get_json(
            '/suggest/?text=Маркетинг&limit=1',
            headers=get_auth_headers())
        expected_department = prepare_department(
            self.main_connection,
            DepartmentModel(self.main_connection).get(
                department_id=self.department['id'],
                org_id=self.organization['id'],
                fields=[
                    '*',
                    'parent.*',
                ],
            ),
            api_version=1,
        )
        self.assertEqual(len(response_data['departments']), 1)
        self.assertEqual(response_data['departments'][0], expected_department)
        self.assertEqual(response_data['users'], [])

    def test_suggest_sort(self):
        # Проверяет, что департаменты, полностью совпавшие по названию с запросом будут на первом месте

        response_data = self.get_json(
            '/suggest/?text=Разработка&limit=10',
            headers=get_auth_headers())
        expected_department = prepare_department(
            self.main_connection,
            DepartmentModel(self.main_connection).get(
                department_id=self.development_department['id'],
                org_id=self.organization['id'],
                fields=[
                    '*',
                    'parent.*',
                ],
            ),
            api_version=1,
        )
        self.assertEqual(len(response_data['departments']), 2)
        self.assertEqual(response_data['departments'][0], expected_department)


class TestSuggestGroup(BaseMixin, TestCase):
    def test_group_suggest_by_name(self):
        response_data = self.get_json(
            '/suggest/?text=Admins%20group&limit=10',
            headers=get_auth_headers()
        )
        self.assertEqual(len(response_data['groups']), 2)

        expected_admins_group = GroupModel(self.main_connection).get(
            org_id=self.organization['id'],
            group_id=self.groups['admins']['id'],
            fields=[
                '*',
                'members.*',
            ],
        )
        expected_admins_group = prepare_group(
            self.main_connection,
            expected_admins_group,
            api_version=1,
        )

        expected_dba = GroupModel(self.main_connection).get(
            org_id=self.organization['id'],
            group_id=self.groups['dba']['id'],
            fields=[
                '*',
                'members.*',
            ],
        )
        expected_dba = prepare_group(
            self.main_connection,
            expected_dba,
            api_version=1,
        )

        self.assertEqual(response_data['groups'][0], expected_admins_group)
        self.assertEqual(response_data['groups'][1], expected_dba)
        self.assertEqual(response_data['users'], [])
        self.assertEqual(response_data['departments'], [])

    def test_suggest_returns_all_group_types_by_default(self):
        response_data = self.get_json('/suggest/?text=admin&limit=5',
                                      headers=get_auth_headers())
        # всего в базе 3 группы, имя которых содержит admin
        self.assertEqual(4, len(response_data['groups']))
        self.assertEqual({'generic', 'organization_admin', 'organization_deputy_admin'},
                         set([g['type'] for g in response_data['groups']]))

    def test_groups_returned_by_suggest_could_be_filtered_by_type(self):
        response_data = self.get_json(
            '/suggest/?text=admin&group_type=generic',
            headers=get_auth_headers())

        # всего в базе только 2 generic группа, имя которой содержит admin
        self.assertEqual(2, len(response_data['groups']))
        self.assertEqual({'generic'},
                         set([g['type'] for g in response_data['groups']]))


def add_scopes(*scopes):
    """Добавляет к перечисленным скоупам пару дефолтных."""
    return scopes + (
        # Это нужно, чтобы не эмулировать подключение сервиса
        # для каждой из организаций.
        scope.work_with_any_organization,
        # А этот нужен, чтобы директория игнорировала
        # тот факт, что OAuth токен привязан и к сервису и к пользователю.
        scope.work_on_behalf_of_any_user,
    )


class TestSuggestOAuth(TestCase):
    def setUp(self):
        super(TestSuggestOAuth, self).setUp()

        self.create_group(label='oauth_test_group')
        self.create_user(nickname='oauth_test_user')
        self.create_department(label='oauth_test_department')

        self.headers = get_oauth_headers()

    def test_only_users_scope(self):
        #  есть права только на чтение пользователей
        with oauth_client(OAUTH_CLIENT_ID,
                          uid=self.user['id'],
                          scopes=add_scopes(scope.read_users,
                                            scope.write_users)):
            response_data = self.get_json(
                '/suggest/?text=oauth_test',
                headers=self.headers
            )

        assert_that(
            response_data,
            all_of(
                has_entries(
                    users=has_length(
                        greater_than(0)
                    ),
                ),
                is_not(
                    has_key('departments')
                ),
                is_not(
                    has_key('groups')
                )
            )
        )

    def test_only_groups_scope(self):
        #  есть права только на чтение команд
        with oauth_client(OAUTH_CLIENT_ID,
                          uid=self.user['id'],
                          scopes=add_scopes(scope.read_groups,
                                            scope.write_groups)):
            response_data = self.get_json(
                '/suggest/?text=oauth_test',
                headers=self.headers
            )

        assert_that(
            response_data,
            all_of(
                has_entries(
                    groups=has_length(
                        greater_than(0)
                    ),
                ),
                is_not(
                    has_key('departments')
                ),
                is_not(
                    has_key('users')
                )
            )
        )

    def test_only_department_scope(self):
        #  есть права только на чтение отделов
        with oauth_client(OAUTH_CLIENT_ID,
                          uid=self.user['id'],
                          scopes=add_scopes(scope.read_departments,
                                            scope.write_departments)):
            response_data = self.get_json(
                '/suggest/?text=oauth_test',
                headers=self.headers
            )

        assert_that(
            response_data,
            all_of(
                has_entries(
                    departments=has_length(
                        greater_than(0)
                    ),
                ),
                is_not(
                    has_key('groups')
                ),
                is_not(
                    has_key('users')
                )
            )
        )

    def test_all_read_scope(self):
        #  есть права на чтение отделов, команд и пользователей
        with oauth_client(OAUTH_CLIENT_ID,
                          uid=self.user['id'],
                          scopes=add_scopes(scope.read_users,
                                            scope.read_departments,
                                            scope.read_groups)):
            response_data = self.get_json(
                '/suggest/?text=oauth_test',
                headers=self.headers,
            )

        assert_that(
            response_data,
            has_entries(
                users=contains(
                    has_entries(nickname='oauth_test_user'),
                ),
                groups=contains(
                    has_entries(label='oauth_test_group'),
                ),
                departments=contains(
                    has_entries(label='oauth_test_department'),
                ),
            )
        )

    def test_all_write_scope(self):
        #  есть права на запись отделов, команд и пользователей
        with oauth_client(OAUTH_CLIENT_ID,
                          uid=self.user['id'],
                          scopes=add_scopes(scope.write_users,
                                            scope.write_departments,
                                            scope.write_groups)):
            response_data = self.get_json(
                '/suggest/?text=oauth_test',
                headers=self.headers
            )

        assert_that(
            response_data,
            has_entries(
                users=has_length(
                    greater_than(0)
                ),
                groups=has_length(
                    greater_than(0)
                ),
                departments=has_length(
                    greater_than(0)
                ),
            )
        )


class TestSuggestLicenseView(TestCase):
    def setUp(self):
        super(TestSuggestLicenseView, self).setUp()
        self.service = ServiceModel(self.meta_connection).create(
            client_id='some-client-id',
            slug='new_service',
            name='Service',
            paid_by_license=True,
            ready_default=True,
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            self.service['slug'])

    def test_suggest_user_with_licenses(self):
        # проверяем, что саджест возвращат пользователя и родительский контейнер с лицензией
        department = self.create_department()
        user1 = self.create_user(nickname='user1')
        user2 = self.create_user(name={'first': {'ru': 'User2'}})
        user3 = self.create_user(department_id=department['id'], nickname='user_three')
        user4 = self.create_user(name={'first': {'ru': 'cool'}, 'last': {'ru': 'guy'}})

        members = [
            {'type': 'user', 'object': user2},
            {'type': 'user', 'object': user3},
        ]
        group = self.create_group(members=members)
        self.create_licenses_for_service(
            self.service['id'],
            department_ids=[department['id']],
            group_ids=[group['id']],
            user_ids=[user3['id'], user4['id']]
        )

        response_data = self.get_json('/suggest-license/new_service/?text=GUY&limit=5', headers=get_auth_headers())
        assert_that(
            response_data['users'],
            contains(
                has_entries(
                    id=user4['id'],
                    name={'first': {'ru': 'cool'}, 'last': {'ru': 'guy'}},
                )
            )
        )
        assert_that(
            response_data['groups'],
            has_length(0)
        )
        assert_that(
            response_data['departments'],
            has_length(0)
        )

        # user3 должен вернутся в составе users, departments и groups
        # user2 только в составе группы
        response_data = self.get_json('/suggest-license/new_service/?text=use&limit=5', headers=get_auth_headers())
        assert_that(
            response_data['users'],
            contains(
                has_entries(
                    id=user3['id'],
                    nickname='user_three',
                )
            )
        )

        assert_that(
            response_data['groups'],
            contains(
                has_entries(
                    id=group['id'],
                    users=contains_inanyorder(
                        has_entries(
                            id=user3['id'],
                            nickname='user_three',
                        ),
                        has_entries(
                            id=user2['id'],
                            name={'first': {'ru': 'User2'}},
                        )
                    ),
                )
            )
        )

        assert_that(
            response_data['departments'],
            contains(
                has_entries(
                    id=department['id'],
                    users=contains_inanyorder(
                        has_entries(
                            id=user3['id'],
                            nickname='user_three',
                        ),
                    )
                )
            )
        )
