# -*- coding: utf-8 -*-
import datetime
import collections
import unittest.mock
from hamcrest import (
    assert_that,
    contains,
    contains_inanyorder,
    has_entries,
    has_items,
    has_length,
    equal_to,
    empty,
    is_not,
)
from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.common.models.types import TYPE_USER
from intranet.yandex_directory.src.yandex_directory.common.utils import (
    utcnow,
    lstring,
    Ignore,
)
from intranet.yandex_directory.src.yandex_directory.common.db import catched_sql_queries
from intranet.yandex_directory.src.yandex_directory.core.utils.robots import create_robot_for_service_and_org_id
from intranet.yandex_directory.src.yandex_directory.core.models import (
    DepartmentModel,
    OrganizationMetaModel,
    ActionModel,
    UserModel,
    UserMetaModel,
    UserDismissedModel,
    GroupModel,
    ResourceRelationModel,
    UserGroupMembership,
    ServiceModel,
    OrganizationServiceModel,
    ResourceModel,
    UserServiceLicenses,
    UsersAnalyticsInfoModel,
    AdminContactsAnalyticsInfoModel,
    TaskModel,
)
from intranet.yandex_directory.src.yandex_directory.core.exceptions import (
    UnableToBlockServiceResponsible,
    UnableToDismissServiceResponsible,
)
from intranet.yandex_directory.src.yandex_directory.core.models.service import enable_service
from intranet.yandex_directory.src.yandex_directory.core.utils import (
    only_fields,
    only_attrs,
    only_ids, RANGE_PASSPORT,
)
from unit.yandex_directory.core.models.resource.tests import (
    ResourceRelationsBaseTestMixin
)
from intranet.yandex_directory.src.yandex_directory.core.commands.update_services_in_shards import Command as UpdateServicesInShards
from intranet.yandex_directory.src.yandex_directory.core.utils.users.restore import mark_user_restored
from unittest.mock import (
    patch,
    Mock,
)

from testutils import (
    create_department,
    TestCase,
    create_inner_uid,
    has_only_entries,
    create_user,
    create_group,
    assert_called_once,
    assert_called,
    calls_count,
    mocked_blackbox,
    create_organization,
    fake_userinfo,
)
from checks import (
    organization_admin,
    generic_group,
)
from intranet.yandex_directory.src.yandex_directory.connect_services.partner_disk.tasks import DeleteSpacePartnerDiskTask


class TestUserModel_create(TestCase):
    def setUp(self):
        super(TestUserModel_create, self).setUp()

        self.department = create_department(
            self.main_connection,
            org_id=self.organization['id']
        )
        self.name = {
            'first': {
                'ru': 'Геннадий',
                'en': 'Gennady'
            },
            'last': {
                'ru': 'Чибисов',
                'en': 'Chibisov'
            },
            'middle': {
                'ru': 'Чибисович',
                'en': 'Chibisovich'
            },
        }
        self.position = {
            'ru': 'Разработчик',
            'en': 'Developer'
        }
        self.groups = {
            'admins': GroupModel(self.main_connection).create(name={'ru': 'Admins'}, org_id=self.organization['id']),
            'managers': GroupModel(self.main_connection).create(name={'ru': 'Managers'}, org_id=self.organization['id']),
        }
        self.about = 'Пишу код'
        self.birthday = datetime.date(day=2, month=3, year=1990)
        self.contacts = [
            {
                'type': 'skype',
                'label': {
                    'ru': 'Домашний',
                    'en': 'Homie'
                },
                'value': 'polina-sosisa'
            },
            {
                'type': 'email',
                'label': {
                    'ru': 'Украденный',
                    'en': 'Stolen'
                },
                'value': 'obama@usa.com'
            },
        ]
        self.groups_ids = [i['id'] for i in list(self.groups.values())]
        self.external_id = 'external_id'

    def test_simple(self):
        # test return value
        user = UserModel(self.main_connection).create(
            id=123,
            nickname='web-chib',
            name=self.name,
            email='web-chib@ya.ru',
            gender='male',
            org_id=self.organization['id'],
            department_id=self.department['id'],
            position=self.position,
            about=self.about,
            birthday=self.birthday,
            contacts=self.contacts,
            external_id=self.external_id,
        )

        self.assertEqual(user['id'], 123)
        self.assertEqual(user['name'], self.name)
        self.assertEqual(user['email'], 'web-chib@ya.ru')
        self.assertEqual(user['nickname'], 'web-chib')
        self.assertEqual(user['gender'], 'male')
        self.assertEqual(user['org_id'], self.organization['id'])
        self.assertEqual(user['department_id'], self.department['id'])
        self.assertEqual(user['position'], self.position)
        self.assertEqual(user['about'], self.about)
        self.assertEqual(user['birthday'], self.birthday)
        self.assertEqual(user['external_id'], self.external_id)

        # test data in database
        user_from_db = UserModel(self.main_connection).get(user_id=user['id'])
        self.assertIsNotNone(user_from_db)
        self.assertEqual(user_from_db['id'], 123)
        self.assertEqual(user_from_db['email'], 'web-chib@ya.ru')
        self.assertEqual(user_from_db['nickname'], 'web-chib')
        self.assertEqual(user_from_db['org_id'], self.organization['id'])
        self.assertEqual(user_from_db['department_id'], self.department['id'])
        self.assertEqual(user_from_db['position'], self.position)
        self.assertEqual(user_from_db['about'], self.about)
        self.assertEqual(user_from_db['external_id'], self.external_id)

    def test_with_groups(self):
        user = UserModel(self.main_connection).create(
            id=123,
            nickname='web-chib',
            name=self.name,
            email='web-chib@ya.ru',
            gender='male',
            org_id=self.organization['id'],
            department_id=self.department['id'],
            groups=self.groups_ids
        )
        self.assertEqual(user['groups'], self.groups_ids)

    def test_create_with_uppercase(self):
        nickname_upper = 'NICKNAME_UPPERCASE'
        email_upper = '%s@ya.ru' % nickname_upper
        user = UserModel(self.main_connection).create(
            id=123,
            nickname=nickname_upper,
            name=self.name,
            email=email_upper,
            gender='male',
            org_id=self.organization['id'],
            department_id=self.department['id'],
            position=self.position,
            about=self.about,
            birthday=self.birthday,
            contacts=self.contacts,
            external_id=self.external_id,
        )

        self.assertEqual(user['id'], 123)
        self.assertEqual(user['nickname'], nickname_upper.lower())
        self.assertEqual(user['email'], email_upper.lower())


class TestUserModel_make_member_of_group(TestCase):
    def setUp(self):
        super(TestUserModel_make_member_of_group, self).setUp()

        self.group = GroupModel(self.main_connection).create(name={'ru': 'Admins'}, org_id=self.organization['id'])
        self.user = UserModel(self.main_connection).create(
            id=123,
            nickname='web-chib',
            name={
                'first': {
                    'ru': 'Геннадий',
                    'en': 'Gennady'
                },
                'last': {
                    'ru': 'Чибисов',
                    'en': 'Chibisov'
                },
            },
            email='web-chib@ya.ru',
            gender='male',
            org_id=self.organization['id'],
        )

    def test_simple(self):
        """Проверяем, что пользователя можно добавить группу.
        """
        org_id = self.organization['id']
        user_id =  self.user['id']
        group_id = self.group['id']
        m_groups = GroupModel(self.main_connection)

        m_groups.add_member(
            org_id=org_id,
            group_id=group_id,
            member={'type': 'user', 'id': user_id},
        )

        user_member = dict(id=user_id, type=TYPE_USER)
        groups = m_groups.get_member_groups(
            org_id=org_id,
            member=user_member,
        )
        assert_that(
            groups,
            contains(has_entries(id=group_id))
        )

        users = m_groups.get_all_users(
            org_id=org_id,
            group_id=group_id,
        )
        assert_that(
            users,
            contains(has_entries(id=user_id))
        )

    def test_should_not_create_membership_duplicates(self):
        """Если добавить в группу дважды, то не будет дубля или ошибки.
        """
        org_id = self.organization['id']
        user_id =  self.user['id']
        group_id = self.group['id']
        m_groups = GroupModel(self.main_connection)

        m_groups.add_member(
            org_id=org_id,
            group_id=group_id,
            member={'type': 'user', 'id': user_id},
        )
        m_groups.add_member(
            org_id=org_id,
            group_id=group_id,
            member={'type': 'user', 'id': user_id},
        )

        user_member = dict(id=user_id, type=TYPE_USER)
        groups = m_groups.get_member_groups(
            org_id=org_id,
            member=user_member,
        )
        assert_that(groups, has_length(1))

        users = m_groups.get_all_users(
            org_id=org_id,
            group_id=group_id,
        )
        assert_that(users, has_length(1))


class TestUserModel_set_generic_groups(TestCase):
    def setUp(self):
        super(TestUserModel_set_generic_groups, self).setUp()

        org_id = self.organization['id']
        self.admins = GroupModel(self.main_connection).create(name=lstring('Admins'), org_id=org_id)
        self.managers = GroupModel(self.main_connection).create(name=lstring('Managers'), org_id=org_id)

        # The kind of guy most girls ACTUALLY want when they say they want a Nice Guy.
        # http://www.urbandictionary.com/define.php?term=Jerk
        self.jerks = GroupModel(self.main_connection).create(name=lstring('Jerks'), org_id=org_id)

        self.user = UserModel(self.main_connection).create(
            id=123,
            nickname='web-chib',
            name={
                'first': lstring('Gennady'),
                'last': lstring('Chibisov'),
            },
            email='web-chib@ya.ru',
            gender='male',
            org_id=self.organization['id'],
        )
        self.uid = self.user['id']
        self.org_id = self.organization['id']
        UserModel(self.main_connection).make_admin_of_organization(
            org_id=self.org_id,
            user_id=self.uid,
        )

    def get_groups(self):
        user_member = dict(id=self.uid, type=TYPE_USER)
        return GroupModel(self.main_connection).get_member_groups(
            org_id=self.org_id,
            member=user_member,
        )

    def test_change_generic_groups(self):
        response = self.get_groups()
        assert_that(response, contains(organization_admin))

        GroupModel(self.main_connection).set_user_groups(
            org_id=self.org_id,
            user_id=self.uid,
            groups=[self.admins['id'], self.managers['id']],
        )

        groups = self.get_groups()
        assert_that(groups,
                    contains_inanyorder(organization_admin,
                                        generic_group('Managers'),
                                        generic_group('Admins')))

        GroupModel(self.main_connection).set_user_groups(
            org_id=self.org_id,
            user_id=self.uid,
            groups=[self.admins['id'], self.jerks['id']],
        )

        result = self.get_groups()
        assert_that(result,
                    contains_inanyorder(organization_admin,
                                        generic_group('Admins'),
                                        generic_group('Jerks')))


class TestUserModel_user_admin(TestCase):
    def setUp(self):
        super(TestUserModel_user_admin, self).setUp()

        self.department = self.create_department(org_id=self.organization['id'])
        self.user = self.create_user(
            nickname='test_not_admin',
            department_id=self.department['id'],
        )

    def test_is_not_admin(self):
        is_admin = UserModel(self.main_connection).is_admin(self.organization['id'], self.user['id'])
        self.assertFalse(is_admin)

    def test_user_should_not_be_admin_if_he_in_generic_group(self):
        members = [{'type': 'user', 'id': self.user['id']}]
        self.create_group(members=members)
        is_admin = UserModel(self.main_connection).is_admin(self.organization['id'], self.user['id'])
        self.assertFalse(is_admin)

    def test_is_admin(self):
        UserModel(self.main_connection).make_admin_of_organization(
            org_id=self.organization['id'],
            user_id=self.user['id'],
        )
        is_admin = UserModel(self.main_connection).is_admin(self.organization['id'], self.user['id'])
        self.assertTrue(is_admin)


class TestUserModel_revoke_admin_permissions(TestCase):
    def setUp(self):
        super(TestUserModel_revoke_admin_permissions, self).setUp()

        self.department = self.create_department(org_id=self.organization['id'])
        self.user = self.create_user(
            nickname='test_not_admin',
            department_id=self.department['id'],
            uid=123456,
        )

    def test_should_remove_admin_permissions(self):
        # проверяем, что метод remove_admin_permissions удалит права администратора у пользователя
        with calls_count(self.mocked_passport.set_admin_option, 1):
            UserModel(self.main_connection).make_admin_of_organization(
                org_id=self.organization['id'],
                user_id=self.user['id'],
            )

        is_admin = UserModel(self.main_connection).is_admin(self.organization['id'], self.user['id'])
        self.assertTrue(is_admin)

        with calls_count(self.mocked_passport.reset_admin_option, 1):
            UserModel(self.main_connection).revoke_admin_permissions(
                org_id=self.organization['id'],
                user_id=self.user['id'],
            )

        is_admin = UserModel(self.main_connection).is_admin(self.organization['id'], self.user['id'])
        self.assertFalse(is_admin)

    def test_should_remove_admin_permissions_without_changing_is_connect_admin(self):
        # проверяем, что метод remove_admin_permissions удалит права администратора у пользователя
        # но не удаляет is_connect_admin если пользователь является админом в другой организации
        with calls_count(self.mocked_passport.set_admin_option, 1):
            UserModel(self.main_connection).make_admin_of_organization(
                org_id=self.organization['id'],
                user_id=self.user['id'],
            )
        organization_1 = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']
        self.add_user_by_invite(organization_1, self.user['id'])

        UserModel(self.main_connection).make_admin_of_organization(
            org_id=organization_1['id'],
            user_id=self.user['id'],
        )

        is_admin = UserModel(self.main_connection).is_admin(self.organization['id'], self.user['id'])
        self.assertTrue(is_admin)
        is_admin = UserModel(self.main_connection).is_admin(organization_1['id'], self.user['id'])
        self.assertTrue(is_admin)

        with calls_count(self.mocked_passport.reset_admin_option, 0):
            UserModel(self.main_connection).revoke_admin_permissions(
                org_id=self.organization['id'],
                user_id=self.user['id'],
            )

        is_admin = UserModel(self.main_connection).is_admin(self.organization['id'], self.user['id'])
        self.assertFalse(is_admin)

        is_admin = UserModel(self.main_connection).is_admin(organization_1['id'], self.user['id'])
        self.assertTrue(is_admin)


class TestUserModel_find_by_id(TestCase):
    def setUp(self):
        super(TestUserModel_find_by_id, self).setUp()

        self.another_user = UserModel(self.main_connection).create(
            id=124,
            nickname='test_not_admin_1',
            name={
                'first': {
                    'ru': 'Геннадий',
                    'en': 'Gennady'
                },
                'last': {
                    'ru': 'Чибисов',
                    'en': 'Chibisov'
                },
            },
            email='test_not_admin@ya.ru',
            gender='male',
            org_id=self.organization['id'],
        )
        self.yet_another_user = UserModel(self.main_connection).create(
            id=456,
            nickname='test_not_admin_2',
            name={
                'first': {
                    'ru': 'Геннадий',
                    'en': 'Gennady'
                },
                'last': {
                    'ru': 'Чибисов',
                    'en': 'Chibisov'
                },
            },
            email='test_not_admin@ya.ru',
            gender='male',
            org_id=self.organization['id'],
        )

    def test_find_by_one_id(self):
        users = UserModel(self.main_connection).filter(id=self.user['id'])
        self.assertEqual(len(users), 1)
        self.assertEqual(users[0]['id'], self.user['id'])

    def test_find_by_one_id_test_no_sort(self):
        # DIR-5657 при параметре one=True функция find  не должна
        # осуществлять сортировку
        with catched_sql_queries() as queries:
            UserModel(self.main_connection).filter(id=self.user['id']).one()
            for request in queries:
                self.assertFalse('order by' in request[0].lower())

    def test_find_by_list_of_ids(self):
        expected_ids = [
            self.user['id'],
            self.another_user['id']
        ]
        response = UserModel(self.main_connection).filter(id=expected_ids)
        self.assertIsNotNone(response)
        self.assertEqual(len(response), len(expected_ids))
        self.assertEqual(
            sorted([i['id'] for i in response]),
            sorted(expected_ids)
        )


class TestUserModel_get_userinfo_from_blackbox(TestCase):
    passport_uid = 124
    passport_robot_uid = 125
    domain_uid = RANGE_PASSPORT[1] + 1000

    all_uids = [passport_uid, passport_robot_uid, domain_uid]

    def create_user(self, **kwargs):
        user = {
            'nickname': 'test_not_admin_1',
            'name': {
                'first': {
                    'ru': 'Геннадий',
                    'en': 'Gennady'
                },
                'last': {
                    'ru': 'Чибисов',
                    'en': 'Chibisov'
                },
            },
            'email': 'test_not_admin@ya.ru',
            'gender': 'male',
            'org_id': self.organization['id'],
            **kwargs
        }
        UserModel(self.main_connection).create(**user)

    def setUp(self):
        super(TestUserModel_get_userinfo_from_blackbox, self).setUp()
        self.create_user(id=self.passport_uid)
        self.create_user(id=self.passport_robot_uid, email='super-robot-test@yandex.ru')
        self.create_user(id=self.domain_uid, email='superuser@domain.com')

    def test_without_blackbox(self):
        with mocked_blackbox() as blackbox:
            # спрашиваем только ID
            UserModel(self.main_connection).find(filter_data={'id': self.all_uids}, fields=['id'])
            assert blackbox.batch_userinfo.call_count == 0

            # спрашиваем userinfo у доменного
            UserModel(self.main_connection).find(filter_data={'id': self.domain_uid}, fields=['id', 'name', 'gender', 'birthday'])
            assert blackbox.batch_userinfo.call_count == 0

            # спрашиваем userinfo у робота
            UserModel(self.main_connection).find(filter_data={'id': self.passport_robot_uid}, fields=['id', 'name', 'gender', 'birthday'])
            assert blackbox.batch_userinfo.call_count == 0

    def test_with_blackbox(self):
        passport_userinfo = {
            self.passport_uid: fake_userinfo(uid=self.passport_uid, first_name='Name for passport uid from blackbox', sex='2'),
            self.passport_robot_uid: fake_userinfo(uid=self.passport_robot_uid, first_name='Name for passport robot uid from blackbox', sex='2'),
            self.domain_uid: fake_userinfo(uid=self.domain_uid, first_name='Name for domain uid from blackbox', sex='2'),
        }
        # Спрашиваем is_enabled и avatar_id для всех юзеров
        with mocked_blackbox() as blackbox:
            blackbox.batch_userinfo.return_value = passport_userinfo.values()

            users = UserModel(self.main_connection).find(filter_data={'id': self.all_uids}, fields=['is_enabled', 'avatar_id'])
            assert blackbox.batch_userinfo.call_count == 1
            assert blackbox.batch_userinfo.call_args_list[0][1]['uids'] == self.all_uids

        # Спрашиваем userinfo у всех юзеров
        with mocked_blackbox() as blackbox:
            blackbox.batch_userinfo.return_value = passport_userinfo.values()

            users = UserModel(self.main_connection).find(filter_data={'id': self.all_uids}, fields=['id', 'name', 'gender', 'birthday'])
            assert blackbox.batch_userinfo.call_count == 1
            assert blackbox.batch_userinfo.call_args_list[0][1]['uids'] == [self.passport_uid]

            assert users[0]['id'] == self.passport_uid
            assert users[0]['name']['first'] == 'Name for passport uid from blackbox'
            assert users[0]['gender'] == 'female'
            assert users[1]['id'] == self.passport_robot_uid
            assert users[1]['name']['first'] != 'Name for passport robot uid from blackbox'
            assert users[1]['gender'] == 'male'
            assert users[2]['id'] == self.domain_uid
            assert users[2]['name']['first'] != 'Name for domain uid from blackbox'
            assert users[2]['gender'] == 'male'

        # Спрашиваем userinfo у паспортного юзера
        with mocked_blackbox() as blackbox:
            blackbox.batch_userinfo.return_value = passport_userinfo.values()

            users = UserModel(self.main_connection).find(filter_data={'id': [self.passport_uid]}, fields=['id', 'name', 'gender', 'birthday'])
            assert blackbox.batch_userinfo.call_count == 1
            assert blackbox.batch_userinfo.call_args_list[0][1]['uids'] == [self.passport_uid]

            assert users[0]['id'] == self.passport_uid
            assert users[0]['name']['first'] == 'Name for passport uid from blackbox'
            assert users[0]['gender'] == 'female'

        # Спрашиваем userinfo у все поля у всех юзеров
        with mocked_blackbox() as blackbox:
            blackbox.batch_userinfo.return_value = passport_userinfo.values()

            users = UserModel(self.main_connection).find(filter_data={'id': self.all_uids}, fields=['id', 'is_enabled', 'avatar_id', 'name', 'gender', 'birthday'])
            assert blackbox.batch_userinfo.call_count == 1
            assert blackbox.batch_userinfo.call_args_list[0][1]['uids'] == self.all_uids

            assert users[0]['id'] == self.passport_uid
            assert users[0]['name']['first'] == 'Name for passport uid from blackbox'
            assert users[0]['gender'] == 'female'
            assert users[1]['id'] == self.passport_robot_uid
            assert users[1]['name']['first'] != 'Name for passport robot uid from blackbox'
            assert users[1]['gender'] == 'male'
            assert users[2]['id'] == self.domain_uid
            assert users[2]['name']['first'] != 'Name for domain uid from blackbox'
            assert users[2]['gender'] == 'male'


class TestUserModel_find_with_fields(TestCase):
    def test_with_nickname(self):
        # Проверяем, что если в fields указан только nickname,
        # то в результатах от find будет только это поле и id.
        response = UserModel(self.main_connection).fields('nickname')
        assert_that(
            response,
            contains_inanyorder(
                has_only_entries(
                    **only_fields(self.user, 'id', 'nickname')
                ),
            )
        )

    def test_with_department(self):
        # Проверяем, что если в fields указан department,
        # то он раскроется в объект с полем id
        response = UserModel(self.main_connection).fields('department')
        assert_that(
            response,
            contains_inanyorder(
                has_only_entries(
                    id=self.user['id'],
                    department=has_only_entries(
                        id=1,
                    ),
                ),
            )
        )

    def test_with_is_outstaff(self):
        # Проверяем, что если в fields указан только is_outstaff,
        # то в результатах от find будет только это поле и id.
        # И при этом is_outstaff будет зависеть от того, в каком
        # отделе пользователь.

        org_id = self.organization['id']
        outstaff = DepartmentModel(self.main_connection) \
                   .get_or_create_outstaff(org_id)
        outstaffer = self.create_user(department_id=outstaff['id'])

        response = UserModel(self.main_connection).fields('is_outstaff')

        assert_that(
            response,
            contains_inanyorder(
                has_only_entries(
                    id=self.user['id'],
                    is_outstaff=False,
                ),
                has_only_entries(
                    id=outstaffer['id'],
                    is_outstaff=True,
                ),
            )
        )


class TestUserModel_get_by_external_id(TestCase):
    def test_simple(self):
        # поиск пользователя по внешнему идентификатору
        # создаем 2 пользователей 1 из которых с внешним идентификатором

        external_id = 'external_id'
        UserModel(self.main_connection).create(
            id=124,
            nickname='test_not_admin',
            name={
                'first': {
                    'ru': 'Геннадий',
                    'en': 'Gennady'
                },
                'last': {
                    'ru': 'Чибисов',
                    'en': 'Chibisov'
                },
            },
            email='test_not_admin@ya.ru',
            gender='male',
            org_id=self.organization['id'],
        )
        another_user = UserModel(self.main_connection).create(
            id=456,
            nickname='test_not_admin2',
            name={
                'first': {
                    'ru': 'Геннадий',
                    'en': 'Gennady'
                },
                'last': {
                    'ru': 'Чибисов',
                    'en': 'Chibisov'
                },
            },
            email='test_not_admin2@ya.ru',
            gender='male',
            org_id=self.organization['id'],
            external_id=external_id
        )

        self.assertEqual(
            UserModel(self.main_connection).get_by_external_id(org_id=self.organization['id'], id=external_id)['nickname'],
            another_user['nickname']
        )


class TestUserModel_find_by_nickname(TestCase):
    def setUp(self):
        super(TestUserModel_find_by_nickname, self).setUp()

        self.another_user = UserModel(self.main_connection).create(
            id=124,
            nickname='nickname_one',
            name={
                'first': {
                    'ru': 'Геннадий',
                    'en': 'Gennady'
                },
                'last': {
                    'ru': 'Чибисов',
                    'en': 'Chibisov'
                },
            },
            email='test_not_admin@ya.ru',
            gender='male',
            org_id=self.organization['id'],
        )
        self.yet_another_user = UserModel(self.main_connection).create(
            id=456,
            nickname='logon_two',
            name={
                'first': {
                    'ru': 'Геннадий',
                    'en': 'Gennady'
                },
                'last': {
                    'ru': 'Чибисов',
                    'en': 'Chibisov'
                },
            },
            email='test_not_admin@ya.ru',
            gender='male',
            org_id=self.organization['id'],
        )

    def test_me(self):
        response = UserModel(self.main_connection).filter(nickname=self.user['nickname'])
        self.assertIsNotNone(response)
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0]['nickname'], self.user['nickname'])

    def test_find_with_percent(self):
        # проверяем, что запрос не ломается, если в nickname есть знак %
        response = UserModel(self.main_connection).find({'nickname': 'nick%'})
        self.assertEqual(response, [])


class TestUserModel_find_by_alias(TestCase):
    def setUp(self):
        super(TestUserModel_find_by_alias, self).setUp()

        self.another_user = UserModel(self.main_connection).create(
            id=124,
            nickname='nickname_one',
            name={
                'first': {
                    'ru': 'Геннадий',
                    'en': 'Gennady'
                },
                'last': {
                    'ru': 'Чибисов',
                    'en': 'Chibisov'
                },
            },
            email='test_not_admin@ya.ru',
            gender='male',
            org_id=self.organization['id'],
            aliases=['alias1', 'alias2']
        )

    def test_me(self):
        response = UserModel(self.main_connection).filter(alias=self.another_user['aliases'][0])
        self.assertIsNotNone(response)
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0]['aliases'], self.another_user['aliases'])

    def test_alias_with_quota(self):
        response = UserModel(self.main_connection).filter(alias='"alias1"').all()
        self.assertEqual(response, [])


class TestUserModel_find_by_department_id(TestCase):
    def setUp(self):
        super(TestUserModel_find_by_department_id, self).setUp()

        self.another_department = create_department(
            self.main_connection,
            org_id=self.organization['id']
        )
        self.another_user = UserModel(self.main_connection).create(
            id=124,
            nickname='test_not_admin_1',
            name={
                'first': {
                    'ru': 'Геннадий',
                    'en': 'Gennady'
                },
                'last': {
                    'ru': 'Чибисов',
                    'en': 'Chibisov'
                },
            },
            email='test_not_admin@ya.ru',
            gender='male',
            org_id=self.organization['id'],
            department_id=self.another_department['id']
        )
        self.yet_another_user = UserModel(self.main_connection).create(
            id=456,
            nickname='test_not_admin_2',
            name={
                'first': {
                    'ru': 'Геннадий',
                    'en': 'Gennady'
                },
                'last': {
                    'ru': 'Чибисов',
                    'en': 'Chibisov'
                },
            },
            email='test_not_admin@ya.ru',
            gender='male',
            org_id=self.organization['id'],
        )

    def test_find_by_one_department_id(self):
        response = UserModel(self.main_connection).filter(department_id=self.user['department_id'])
        self.assertIsNotNone(response)
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0]['id'], self.user['id'])

    def test_find_by_list_of_department_ids(self):
        expected_ids = [
            self.user['id'],
            self.another_user['id']
        ]
        response = UserModel(self.main_connection).filter(
            department_id=[
                self.user['department_id'],
                self.another_user['department_id']
            ]
        )
        self.assertIsNotNone(response)
        self.assertEqual(len(response), len(expected_ids))
        self.assertEqual(
            sorted([i['id'] for i in response]),
            sorted(expected_ids)
        )


class TestUserModel_count(TestCase):
    def setUp(self):
        super(TestUserModel_count, self).setUp()

    def test_count(self):
        # User в департаменте, департамент в группе, группа в ресурсе.
        # И еще user  связан с ресурсом напрямую.
        # Проверяем, что distinct в count модели работает правильно.

        department = self.create_department()
        user = self.create_user(department_id=department['id'])
        group = self.create_group(
            members=[{'type': 'department', 'object': department}]
        )
        resource = self.create_resource_with_group(
            group_id=group['id']
        )
        ResourceRelationModel(self.main_connection).create(
            org_id=self.organization['id'],
            resource_id=resource['id'],
            name='read',
            user_id=user['id'],
        )
        query = UserModel(self.main_connection).filter(
            resource=resource['id'], resource_service=resource['service']
        )
        count = query.count()
        found_data = query.distinct()
        find_length = len(found_data)
        assert_that(count, 1)
        assert_that(count, equal_to(find_length))


class TestUserModel_find_by_resource(ResourceRelationsBaseTestMixin, TestCase):
    def check_expectations(self, users, resource, other_user, other_resource):
        response = UserModel(self.main_connection).filter(
            resource=resource['id'],
            resource_service=resource['service'],
        )
        self.check_response(response, users)

        # list of resources
        response = UserModel(self.main_connection).filter(
            resource=[
                resource['id'],
                other_resource['id'],
            ],
            resource_service=Ignore,
        )
        expected = users + [other_user]
        self.check_response(response, expected)


class TestUserModel_restore(TestCase):
    def setUp(self):
        super(TestUserModel_restore, self).setUp()
        UserModel(self.main_connection).dismiss(
            org_id=self.user['org_id'],
            user_id=self.user['id'],
            author_id=self.user['id']
        )

    def test_restored_gets_marked_in_db(self):
        # mark_user_restored устанавливает флаг уволенности в main, meta базах.
        mark_user_restored(
            self.meta_connection, self.main_connection,
            org_id=self.user['org_id'],
            user_id=self.user['id'],
            department_id=self.department['id']
        )
        restored_user = UserModel(self.main_connection).filter(
            is_dismissed=False,
            id=self.user['id'],
            org_id=self.user['org_id']
        ).fields('id').all()
        assert_that(
            restored_user,
            contains(
                {'id': self.user['id']}
            )
        )
        restored_meta_user = UserMetaModel(self.meta_connection).filter(
            is_dismissed=False,
            id=self.user['id'],
            org_id=self.user['org_id']
        ).fields('id').all()
        assert_that(
            restored_meta_user,
            contains(
                {'id': self.user['id']}
            )
        )
        self.assert_task_created('SyncExternalIDS')


class TestUserModel__filter_by_is_robot(TestCase):
    def test_filter_by_is_robot(self):
        # проверяем, что фильтр по роботам работает правильно

        user_id = self.create_user(
            nickname='akhmetov',
        )['id']

        robot_user_id = create_robot_for_service_and_org_id(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            service_slug=self.service['slug'],
            org_id=self.organization['id']
        )

        robots = UserModel(self.main_connection).find(filter_data={
            'is_robot': True,
        })

        # выбранные роботы содержат robot_user_id и не содержат user_id
        assert_that(
            only_attrs(robots, 'id'),
            has_items(robot_user_id),
        )
        assert_that(
            only_attrs(robots, 'id'),
            is_not(
                has_items(user_id),
            ),
        )

        users = UserModel(self.main_connection).find(filter_data={
            'is_robot': False,
        })
        # выбранные пользователи содержат user_id и не содержат robot_user_id
        assert_that(
            only_attrs(users, 'id'),
            has_items(user_id),
        )
        assert_that(
            only_attrs(users, 'id'),
            is_not(
                has_items(robot_user_id),
            ),
        )


class TestUserMetaModel_create(TestCase):
    create_organization = False

    def setUp(self):
        self.organization_meta = OrganizationMetaModel(self.meta_connection).create(
            label='not_yandex',
            shard=1,
        )

    def test_me(self):
        uid = create_inner_uid(123)

        user = UserMetaModel(self.meta_connection).create(
            id=uid,
            org_id=self.organization_meta['id'],
        )

        self.assertEqual(user['id'], uid)
        self.assertEqual(user['org_id'], self.organization_meta['id'])

        # test data in database
        user_from_db = UserMetaModel(self.meta_connection).get(
            user_id=user['id'],
            org_id=self.organization_meta['id']
        )
        self.assertIsNotNone(user_from_db)
        self.assertEqual(user_from_db['id'], uid)
        self.assertEqual(user_from_db['org_id'], self.organization_meta['id'])

    def test_outer_admin(self):
        outer_uid = 123
        user = UserMetaModel(self.meta_connection).create(
            id=outer_uid,
            org_id=self.organization_meta['id'],
        )

        self.assertEqual(user['id'], outer_uid)
        self.assertEqual(user['org_id'], self.organization_meta['id'])
        self.assertEqual(user['is_outer'], True)

        # test data in database
        user_from_db = UserMetaModel(self.meta_connection).get(
            user_id=user['id'],
            org_id=self.organization_meta['id'],
            is_outer=True
        )
        self.assertIsNotNone(user_from_db)
        self.assertEqual(user_from_db['id'], outer_uid)
        self.assertEqual(user_from_db['org_id'], self.organization_meta['id'])
        self.assertEqual(user_from_db['is_outer'], True)


class TestUserMetaModel_find(TestCase):

    def setUp(self):
        orgmetamodel = OrganizationMetaModel(self.meta_connection)
        self.organization_meta = orgmetamodel.create(
            label='the.other.domain',
            shard=1,
        )
        self.other_org_id = self.organization_meta['id']

        self.organization_meta = orgmetamodel.create(
            label='another.domain',
            shard=1,
        )
        self.another_org_id = self.organization_meta['id']

        self.organization_meta = orgmetamodel.create(
            label='shard2.domain',
            shard=2,
        )
        self.shard2_org_id = self.organization_meta['id']

        self.start_pdd_uid = 111*10**13
        self.uid1 = 111
        self.uid2 = self.start_pdd_uid + 2
        self.uid3 = 113
        self.uid4 = self.start_pdd_uid + 4
        self.uid5 = 115

        usermetamodel = UserMetaModel(self.meta_connection)

        self.user1 = usermetamodel.create(
            id=self.uid1,
            org_id=self.other_org_id,
        )

        self.user2 = usermetamodel.create(
            id=self.uid2,
            org_id=self.other_org_id,
        )

        self.user3 = usermetamodel.create(
            id=self.uid3,
            org_id=self.another_org_id,
        )

        self.user4 = usermetamodel.create(
            id=self.uid4,
            org_id=self.other_org_id,
        )

        self.user5 = usermetamodel.create(
            id=self.uid5,
            org_id=self.another_org_id,
        )

    def test_find_by_id(self):
        # test simple find
        user_from_db = UserMetaModel(self.meta_connection).filter(
            id=self.uid1,
        )

        assert_that(user_from_db, has_length(1))
        assert_that(
            user_from_db,
            contains(
                has_entries(
                    id=self.uid1,
                    org_id=self.other_org_id,
                    is_outer=True,
                )
            )
        )

    def test_find_by_outers(self):
        outer_users = UserMetaModel(self.meta_connection).filter(
            is_outer=True,
        )

        assert_that(outer_users, has_length(3))
        assert_that(
            outer_users,
            contains_inanyorder(
                has_entries(
                    id=self.user1['id'],
                    org_id=self.user1['org_id'],
                    is_outer=True,
                ),
                has_entries(
                    id=self.user3['id'],
                    org_id=self.user3['org_id'],
                    is_outer=True,
                ),
                has_entries(
                    id=self.user5['id'],
                    org_id=self.user5['org_id'],
                    is_outer=True,
                ),
            )
        )

    def test_find_by_org_id(self):
        users_in_one_org = UserMetaModel(self.meta_connection).filter(
            org_id=self.other_org_id,
        )

        assert_that(users_in_one_org, has_length(3))
        assert_that(
            users_in_one_org,
            contains_inanyorder(
                has_entries(
                    id=self.user1['id'],
                    org_id=self.other_org_id,
                    is_outer=self.user1['is_outer'],
                ),
                has_entries(
                    id=self.user2['id'],
                    org_id=self.other_org_id,
                    is_outer=self.user2['is_outer'],
                ),
                has_entries(
                    id=self.user4['id'],
                    org_id=self.other_org_id,
                    is_outer=self.user4['is_outer'],
                ),
            )
        )

        all_users = UserMetaModel(self.meta_connection).filter(
            org_id=[self.another_org_id, self.other_org_id],
        )
        assert_that(all_users, has_length(5))

    def test_find_by_mixed_fields(self):
        outer_users = UserMetaModel(self.meta_connection).filter(
            is_outer= True,
            org_id=[self.another_org_id, self.other_org_id],
        )

        assert_that(outer_users, has_length(3))
        assert_that(
            outer_users,
            contains_inanyorder(
                has_entries(
                    id=self.user1['id'],
                    org_id=self.user1['org_id'],
                    is_outer=True,
                ),
                has_entries(
                    id=self.user3['id'],
                    org_id=self.user3['org_id'],
                    is_outer=True,
                ),
                has_entries(
                    id=self.user5['id'],
                    org_id=self.user5['org_id'],
                    is_outer=True,
                ),
            )
        )


class TestUserModel_change_password(TestCase):
    def setUp(self):
        super(TestUserModel_change_password, self).setUp()

        self.user = self.create_user(
            nickname='akhmetov',
            department_id=self.department['id'],
        )

    def test_change_password_should_make_call_to_passport_api(self):
        # проверяем, что идем в паспорт менять пароль
        track_id = 321
        new_password = 'password'
        kwargs = {
            'org_id': self.organization['id'],
            'user_id': self.user['id'],
            'author_id': self.user['id'],
            'new_password': new_password,
            'force_next_login_password_change': False,
        }
        UserModel(self.main_connection).change_password(**kwargs)
        assert_called_once(
            self.mocked_passport.change_password,
            uid=self.user['id'],
            new_password=new_password,
            force_next_login_password_change=False,
        )

    def test_change_password_should_make_call_to_passport_api_with_force_password_change_required(self):
        # проверяем, что в апи паспорта передается параметр password_change_required,
        # который заставит пользователя менять пароль при первом входе
        track_id = 321
        new_password = 'password'
        kwargs = {
            'org_id': self.organization['id'],
            'user_id': self.user['id'],
            'author_id': self.user['id'],
            'new_password': new_password,
            'force_next_login_password_change': True,
        }
        UserModel(self.main_connection).change_password(**kwargs)
        assert_called_once(
            self.mocked_passport.change_password,
            uid=self.user['id'],
            new_password=new_password,
            force_next_login_password_change=True,
        )

    def test_change_password_should_create_action(self):
        another_user = self.create_user(
            nickname='another_user',
            department_id=self.department['id'],
        )
        ActionModel(self.main_connection).delete(force_remove_all=True)
        UserModel(self.main_connection).change_password(
            org_id=self.organization['id'],
            user_id=another_user['id'],
            author_id=self.user['id'],
            new_password='new_password',
            force_next_login_password_change=False,
        )
        actions = ActionModel(self.main_connection).all()
        self.assertEqual(len(actions), 1)
        assert_that(
            actions,
            contains(
                has_entries(
                    name='security_user_password_changed',
                    org_id=self.organization['id'],
                    object_type='security',
                    object=another_user['id'],
                )
            )
        )


class TestUserModel_change_is_enabled_status(TestCase):
    def setUp(self):
        self.start_patchers()
        super(TestUserModel_change_is_enabled_status, self).setUp()

        self.user = self.create_user(
            nickname='akhmetov',
            department_id=self.department['id'],
        )

    def test_to_disable_impossible_for_responsible(self):
        # нельзя заблокировать ответственного
        enable_service(
            self.meta_connection, self.main_connection,
            org_id=self.user['org_id'],
            service_slug=self.service['slug']
        )
        OrganizationServiceModel(self.main_connection).change_responsible(
            org_id=self.user['org_id'],
            service_id=self.service['id'],
            responsible_id=self.user['id']
        )

        with self.assertRaises(UnableToBlockServiceResponsible):
            UserModel(self.main_connection).change_is_enabled_status(
                org_id=self.user['org_id'],
                user_id=self.user['id'],
                author_id=self.user['id'],
                is_enabled=False
            )

    def test_change_is_enabled_status_should_make_call_to_passport_api(self):
        kwargs = {
            'org_id': self.organization['id'],
            'user_id': self.user['id'],
            'author_id': self.user['id'],
            'is_enabled': True,
        }
        UserModel(self.main_connection).change_is_enabled_status(**kwargs)

        assert_called_once(
            self.mocked_passport.unblock_user,
            self.user['id'],
        )

    def test_change_password_should_create_action(self):
        experiments = {
            'security_user_blocked': False,
            'security_user_unblocked': True,
        }

        for action_name, is_enabled in list(experiments.items()):
            ActionModel(self.main_connection).delete(force_remove_all=True)
            UserModel(self.main_connection).change_is_enabled_status(
                org_id=self.organization['id'],
                user_id=self.user['id'],
                author_id=self.user['id'],
                is_enabled=is_enabled,
            )
            actions = ActionModel(self.main_connection).all()
            assert_that(
                actions,
                contains(
                    has_entries(
                        name=action_name,
                        org_id=self.organization['id'],
                        object_type='security',
                        object=self.user['id'],
                    )
                )
            )


class TestUserModel_is_enabled(TestCase):
    def test_is_enabled_status(self):
        experiments = [
            {'result': {}, 'expected_status': False},
            {'result': {'attributes': {'1009': '1'}}, 'expected_status': True},
            {'result': {'attributes': {'1009': '0'}}, 'expected_status': False},
            {'result': {'attributes': {}}, 'expected_status': False},
        ]
        uid = 123
        user_ip = '123'
        for experiment in experiments:
            with mocked_blackbox() as blackbox:
                userinfo_return_value = {
                    'fields': collections.defaultdict(dict),
                    'uid': str(uid),
                    'default_email': None,
                    'karma': 123,
                }
                userinfo_return_value.update(experiment['result'])
                blackbox.userinfo.return_value = userinfo_return_value

                response = UserModel(self.main_connection).is_enabled(uid, user_ip)
                self.assertEqual(response, experiment['expected_status'])

                expected_call_args = unittest.mock.call(
                    uid=uid,
                    userip=user_ip,
                    dbfields=[], aliases='all',
                    attributes='1009,13,98', emails='getdefault'
                )
                self.assertEqual(blackbox.userinfo.call_args, expected_call_args)

    def test_is_enabled_status_for_yandex_team_uid(self):
        uid = 112 * 10**13 + 10
        user_ip = ''
        mocked_userinfo = Mock()
        with patch.object(app.blackbox_instance, 'userinfo', mocked_userinfo):
            response = UserModel(self.main_connection).is_enabled(uid, user_ip)
            msg = 'Если uid принадлежит диапазону yandex-team пользователей, is_enabled должен вернуть True'
            self.assertEqual(response, True)
            msg = 'Если uid принадлежит диапазону yandex-team пользователей, не нужно ходить в паспорт'
            self.assertEqual(mocked_userinfo.call_count, 0, msg=msg)


class TestUserModel_dismiss(TestCase):
    def test_impossible_for_responsible(self):
        # нельзя уволить ответственного
        enable_service(
            self.meta_connection, self.main_connection,
            org_id=self.user['org_id'],
            service_slug=self.service['slug']
        )
        OrganizationServiceModel(self.main_connection).change_responsible(
            org_id=self.user['org_id'],
            service_id=self.service['id'],
            responsible_id=self.user['id']
        )
        with self.assertRaises(UnableToDismissServiceResponsible):
            UserModel(self.main_connection).dismiss(
                org_id=self.user['org_id'],
                user_id=self.user['id'],
                author_id=self.user['id']
            )

    def test_possible_for_forms_responsible(self):
        # можно уволить ответственного за формы
        service = ServiceModel(self.meta_connection).create(
            slug='forms',
            name='Name',
            robot_required=True,
            client_id='test',
        )
        enable_service(
            self.meta_connection, self.main_connection,
            org_id=self.user['org_id'],
            service_slug=service['slug']
        )
        OrganizationServiceModel(self.main_connection).change_responsible(
            org_id=self.user['org_id'],
            service_id=service['id'],
            responsible_id=self.user['id']
        )
        UserModel(self.main_connection).dismiss(
            org_id=self.user['org_id'],
            user_id=self.user['id'],
            author_id=self.user['id']
        )
        assert OrganizationServiceModel(self.main_connection).filter(
            service_id=service['id'],
            org_id=self.user['org_id'],
        ).one()['responsible_id'] is None

    def test_dismiss_marks_models_in_db(self):
        # UserModel.dismiss ставит флаг уволенности в meta, main базах
        UserModel(self.main_connection).dismiss(
            org_id=self.user['org_id'],
            user_id=self.user['id'],
            author_id=self.user['id']
        )
        dismissed_user = UserModel(self.main_connection).filter(
            is_dismissed=True,
            id=self.user['id'],
            org_id=self.user['org_id']
        ).fields('id').all()
        assert_that(
            dismissed_user,
            contains(
                {'id': self.user['id']}
            )
        )
        dismissed_meta_user = UserMetaModel(self.meta_connection).filter(
            is_dismissed=True,
            id=self.user['id'],
            org_id=self.user['org_id']
        ).fields('id').all()
        assert_that(
            dismissed_meta_user,
            contains(
                {'id': self.user['id']}
            )
        )

    def test_dismiss_creates_events(self):
        with patch('intranet.yandex_directory.src.yandex_directory.core.actions.action_user_dismiss') as mock_action:
            org_id = self.organization['id']
            new_user = self.create_user(
                nickname='user_for_delete',
                department_id=self.department['id'],
                org_id=org_id
            )
            UserModel(self.main_connection).dismiss(
                org_id=org_id,
                user_id=new_user['id'],
                author_id=new_user['id'],
            )
            assert_called(mock_action, count=1)

    def test_dismiss_creates_events_and_updates_organization_ids_in_passport(self):
        # Увольнение пользователя должно обновлять список organization_ids в паспорте
        # если учётка не принадлежит домену организации.
        org_id = self.organization['id']
        admin_id = self.user['id']
        user_id = 100500
        self.add_user_by_invite(self.organization, user_id)
        self.process_tasks()

        # Нужно сбросить мок, так как он уже вызывался, когда мы добавляли пользователя
        self.mocked_passport.set_organization_ids.reset_mock()

        UserModel(self.main_connection).dismiss(
            org_id=org_id,
            user_id=user_id,
            author_id=admin_id,
        )
        self.process_tasks()
        self.assert_no_failed_tasks()

        # Так как пользователя исключили из организации,
        # то в паспорте должен быть установлен пустой список.
        assert_called_once(
            self.mocked_passport.set_organization_ids,
            user_id,
            [],
        )

    def test_dismiss_should_revoke_admin_permissions_and_remove_him_from_all_group(self):
        # Проверяем, что при увольнении пользователя у него отберутся админские права
        # и он будет удалён из всех групп.

        org_id = self.organization['id']

        # создаём нового пользователя
        new_user = self.create_user(
            nickname='user_for_delete',
            department_id=self.department['id'],
            org_id=org_id,
        )
        uid = new_user['id']

        group_m = GroupModel(self.main_connection)

        def get_groups_where_he_member():
            """Возвращает id групп, в которые пользователь входит непосредственно.
            """
            return group_m.get_user_groups(org_id, uid)

        def get_groups_relations():
            """Возвращает id групп, в которые пользователь входит непосредственно.
            """
            return UserGroupMembership(self.main_connection).filter(
                org_id=org_id,
                user_id=uid,
            )

        # делаем этого пользователя админом
        UserModel(self.main_connection).make_admin_of_organization(
            org_id=org_id,
            user_id=uid,
        )
        # проверим, что он стал админом
        is_admin = UserModel(self.main_connection).is_admin(org_id, uid)
        self.assertTrue(is_admin)

        # Добавим его в generic группу, как участника и админа
        members = [{'type': 'user', 'id': uid}]
        group1 = self.create_group(
            members=members,
            admins=[uid],
        )
        # Добавим во вторую группу только как админа У нас был баг, из
        # за ктого мы таких админов не убирали из списка админов при
        # увольнении https://st.yandex-team.ru/DIR-3485
        group2 = self.create_group(
            members=[],
            admins=[uid],
        )

        # И сделаем руководителем отдела
        DepartmentModel(self.main_connection).update_one(
            self.department['id'],
            org_id,
            {'head_id': uid},
        )

        # Проверим, что пользователь состоит в трех группах:
        # - администраторы организации
        # - generic группа
        # - группа "руководитель отдела"
        assert_that(
            get_groups_where_he_member(),
            has_length(3),
        )
        assert_that(
            get_groups_relations(),
            has_length(3),
        )

        # Убедимся, что пользователь является админом в обоих командах
        group_ids = [group1['id'], group2['id']]
        groups = group_m.fields('admins').filter(
            id=group_ids,
        )
        assert_that(
            groups,
            has_items(
                has_entries(
                    admins=contains(
                        has_entries(id=uid)
                    ),
                )
            )
        )

        # увольняем
        UserModel(self.main_connection).dismiss(
            org_id=org_id,
            user_id=uid,
            author_id=uid,
        )

        # обычный get не должен возвращать уволенного человека
        user = UserModel(self.main_connection).get(org_id=org_id, user_id=uid)
        self.assertIsNone(user)

        # заберем запись из базы и проверим, что этот пользователь больше не админ
        dismissed_user = UserModel(self.main_connection).find(
            filter_data={
                'id': uid,
                'org_id': org_id,
                'is_dismissed': True,
            },
        )
        self.assertTrue(len(dismissed_user), 1)
        dismissed_user = dismissed_user[0]

        is_admin = UserModel(self.main_connection).is_admin(org_id, dismissed_user['id'])
        self.assertFalse(is_admin)

        # Теперь проверим, что он не имеет никаких связей с группами.
        # Ни в кэше.
        assert_that(
            get_groups_where_he_member(),
            has_length(0),
        )
        # Ни напрямую.
        assert_that(
            get_groups_relations(),
            has_length(0),
        )

        # Так же удостоверимся, что пользователь пропал из списка админов в
        # обоих командах
        groups = GroupModel(self.main_connection).find(
            {'id': [group1['id'], group2['id']]},
            fields=['admins'],
        )
        assert_that(
            groups,
            has_items(
                has_entries(
                    admins=empty(),
                )
            )
        )

    def test_dismiss_user_with_uppercase_nickname(self):
        # проверяем увольнение пользователя, у которого есть большая буква в nickname
        # создаём нового пользователя
        new_user = self.create_user(
            nickname='User_For_DeletE',
            department_id=self.department['id'],
        )

        # увольняем
        UserModel(self.main_connection).dismiss(
            org_id=self.organization['id'],
            user_id=new_user['id'],
            author_id=new_user['id'],
        )

        # обычный get не должен возвращать уволенного человека
        user = UserModel(self.main_connection).get(org_id=self.organization['id'], user_id=new_user['id'])
        self.assertIsNone(user)

        # PassportApiClient должен вызваться с lowercase ником
        self.mocked_passport.account_delete.assert_called_once_with(new_user['id'])

    def test_dismiss_blocked_user(self):
        # проверяем увольнение блокированного пользователя, он должен разблокироваться перед удалением из паспорта
        new_user = self.create_user(
            nickname='blocked_user',
            department_id=self.department['id'],
        )
        # увольняем и проверяем, что вызывается функция разблокировки
        mocked__batch_userinfo = Mock(return_value=[{
            'attributes': {},
            'fields': collections.defaultdict(dict),
            'uid': str(new_user['id']),
            'default_email': None,
            'karma': 123,
        }])
        with patch.object(app.blackbox_instance, 'batch_userinfo', mocked__batch_userinfo):
                UserModel(self.main_connection).dismiss(
                    org_id=self.organization['id'],
                    user_id=new_user['id'],
                    author_id=new_user['id'],
                )
        self.mocked_passport.unblock_user.assert_called_once_with(new_user['id'])

        # обычный get не должен возвращать уволенного человека
        user = UserModel(self.main_connection).get(org_id=self.organization['id'], user_id=new_user['id'])
        self.assertIsNone(user)

        dismissed_user = UserModel(self.main_connection).find(
            filter_data={
                'org_id': self.organization['id'],
                'is_dismissed': True,
            },
        )
        self.assertTrue(len(dismissed_user), 1)
        self.assertEqual(dismissed_user[0]['id'], new_user['id'])

    def test_dismiss_delete_external_id(self):
        # проверим, что при увольнениии затирается external_id
        external_id = 7777777
        new_user = self.create_user(
            nickname='external_user',
            department_id=self.department['id'],
            external_id=external_id
        )
        UserModel(self.main_connection).dismiss(
            org_id=self.organization['id'],
            user_id=new_user['id'],
            author_id=new_user['id'],
        )
        dismissed_user = UserModel(self.main_connection).filter(
            id=new_user['id'],
            is_dismissed=True
        ).one()
        assert_that(
            dismissed_user['external_id'],
            equal_to(None)
        )
        # можем создать пользователя с таким же external_id
        another_user = self.create_user(
            nickname='external_user',
            department_id=self.department['id'],
            external_id=external_id
        )

    def test_dismiss_with_disk_license(self):
        # при увольнении пользователя нужно отбирать место на диске, если была лицензия
        disk = ServiceModel(self.meta_connection).create(
            client_id='some-client-id2',
            slug='disk',
            name='disk',
            robot_required=False,
            paid_by_license=True,
        )
        UpdateServicesInShards().try_run()
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            disk['slug'],
        )
        resource_id = OrganizationServiceModel(self.main_connection).filter(
            org_id=self.organization['id'],
            service_id=disk['id'],
        ).scalar('resource_id')[0]

        # выдаем лицензии на диск
        new_user = self.create_user()
        self.create_licenses_for_service(disk['id'], user_ids=[new_user['id']])
        self.assertEqual(UserServiceLicenses(self.main_connection).count(), 1)

        with patch.object(app, 'partner_disk'):
            UserModel(self.main_connection).dismiss(
                org_id=self.organization['id'],
                user_id=new_user['id'],
                author_id=self.admin_uid,
            )
            self.process_tasks()

        disk_task = TaskModel(self.main_connection).filter(
            task_name=DeleteSpacePartnerDiskTask.get_task_name(),
        ).one()

        assert_that(
            disk_task,
            has_entries(
                params=has_entries(
                    org_id=self.organization['id'],
                    uid=new_user['id'],
                    resource_id=resource_id,
                    author_id=self.admin_uid,
                )
            ),
        )

        self.assertEqual(UserServiceLicenses(self.main_connection).count(), 0)


class TestUserModel_delete(TestCase):
    def test_delete_simple(self):
        # Проверяем, удалённый пользователь не будет возвращаться
        # методом get

        org_id = self.organization['id']
        new_user = self.create_user(
            nickname='user_for_delete',
            department_id=self.department['id'],
            org_id=org_id
        )
        UserModel(self.main_connection).delete(
            filter_data={
                'org_id': org_id,
                'id': new_user['id']
            }
        )

        delete_none_user = UserModel(self.main_connection).get(
            org_id=org_id,
            user_id=new_user['id']
        )
        self.assertEqual(delete_none_user, None)

    def test_delete_dismissed_user(self):
        # Убеждаемся, что обычное удаление не может удалить данные,
        # с флажком is_dismissed=True и проверяем работу delete с
        # force=True параметром

        org_id = self.organization['id']
        new_user = self.create_user(
            nickname='user_for_delete',
            department_id=self.department['id'],
            org_id=org_id
        )
        UserModel(self.main_connection).update_one(
            filter_data={
                'org_id': org_id,
                'id': new_user['id'],
            },
            update_data={
                'is_dismissed': True
            }
        )
        UserModel(self.main_connection).delete(filter_data={
            'org_id': org_id,
            'id': new_user['id']
        })

        deleted_users = UserModel(self.main_connection).find(
            filter_data={
                'org_id': org_id,
                'id': new_user['id'],
                'is_dismissed': True
            }
        )
        # все еще в таблице
        self.assertNotEqual(deleted_users, [])

        UserModel(self.main_connection).delete(
            filter_data={
                'org_id': org_id,
                'id': new_user['id'],
            },
            force=True
        )

        delete_none_user = UserModel(self.main_connection).get(
            org_id=org_id,
            user_id=new_user['id'],
        )
        self.assertEqual(delete_none_user, None)


class TestUserDismissModel(TestCase):

    def test_create(self):
        # создаем запись по увольнению
        user = self.create_user()
        group = {'group': 1}
        group_admin = {'group_admin': 1}
        department = [{'department': 1}, {'department': 2}]
        actual = UserDismissedModel(self.main_connection).create(
            org_id=self.organization['id'],
            uid=user['id'],
            department=department,
            groups=group,
            groups_admin=group_admin
        )
        assert_that(
            actual,
            has_entries({
                'org_id': self.organization['id'],
                'user_id': user['id'],
                'department': department,
                'groups': group,
                'groups_admin': group_admin
            })
        )


class TestUserModel_find_with_services(TestCase):
    def setUp(self):
        super(TestUserModel_find_with_services, self).setUp()
        self.departments = {
            'tech': create_department(
                self.main_connection,
                name={'en': 'Technology department'},
                org_id=self.organization['id']
            ),
            'business': create_department(
                self.main_connection,
                name={'en': 'Business department'},
                org_id=self.organization['id']
            ),
            'business_development': create_department(
                self.main_connection,
                name={'en': 'Business development department'},
                org_id=self.organization['id']
            ),
        }
        self.groups = {
            'python': create_group(
                self.main_connection,
                name={'en': 'Python group'},
                org_id=self.organization['id']
            ),
            'ruby': create_group(
                self.main_connection,
                name={'en': 'Ruby group'},
                org_id=self.organization['id']
            ),
            'normal_programming_languages': create_group(
                self.main_connection,
                name={'en': 'Golang group'},
                org_id=self.organization['id']
            ),
        }
        self.users = {
            'user1': create_user(
                self.meta_connection,
                self.main_connection,
                user_id='2345',
                nickname='user1',
                name={'first': {'ru': 'user1'}},
                email='user1@ya.ru',
                org_id=self.organization['id']
            ),
            'user2': create_user(
                self.meta_connection,
                self.main_connection,
                user_id='3456',
                nickname='user2',
                name={'first': {'ru': 'user2'}},
                email='user2@ya.ru',
                org_id=self.organization['id']
            ),
        }
        self.services = {
            'tracker': ServiceModel(self.meta_connection).create(
                            client_id='some-client-id1',
                            slug='tracker',
                            name='tracker',
                            robot_required=False,
                            trial_period_months=1,
                            paid_by_license=True,
                            ready_default=True,
                        ),
            'wiki': ServiceModel(self.meta_connection).create(
                        client_id='some-client-id2',
                        slug='wiki',
                        name='wiki',
                        robot_required=False,
                        ready_default=True,
                    ),
            'abc': ServiceModel(self.meta_connection).create(
                        client_id='some-client-id3',
                        slug='abc',
                        name='abc',
                        robot_required=False,
                        ready_default=True,
                        trial_period_months=0,
                        paid_by_license=True,
                    ),
        }
        for service in list(self.services.values()):
            enable_service(
                self.meta_connection,
                self.main_connection,
                self.organization['id'],
                service['slug']
            )
        self.resource_id_tracker = OrganizationServiceModel(self.main_connection).find(
            filter_data={'service_id': self.services['tracker']['id']}
        )[0]['resource_id']
        self.resource_id_abc = OrganizationServiceModel(self.main_connection).find(
            filter_data={'service_id': self.services['abc']['id']}
        )[0]['resource_id']

    def _check_services(self, user, user_service=None):
        # wiki всегда доступен всем
        if user_service:
            assert_that(
                user,
                has_entries(
                    id=user['id'],
                    services=contains_inanyorder(
                        has_entries(
                            id=user_service['id'],
                            slug=user_service['slug'],
                            paid_by_license=True,
                        ),
                        has_entries(
                            id=self.services['wiki']['id'],
                            slug=self.services['wiki']['slug'],
                            paid_by_license=False,
                        ),
                    ),
                ),
            )
        else:
            assert_that(
                user,
                has_entries(
                    id=user['id'],
                    services=contains(
                        has_entries(
                            id=self.services['wiki']['id'],
                            slug=self.services['wiki']['slug'],
                            paid_by_license=False,
                        ),
                    ),
                ),
            )

    def test_get_user_service_direct_relations(self):
        # проверяем, что у пользователей есть доступ к сервису, если лицензии выданы напрямую

        # дадим доступ user2 к abc
        relations = [
            {
                'name': 'member',
                'user_id': self.users['user2']['id'],
            },
        ]
        ResourceModel(self.main_connection).update_relations(
            resource_id=self.resource_id_abc,
            org_id=self.organization['id'],
            relations=relations,
        )
        # обновим таблицу лицензий
        for service_id in [self.services['tracker']['id'], self.services['abc']['id']]:
            UserServiceLicenses(self.main_connection).update_licenses_cache(self.organization['id'], service_id)

        users = UserModel(self.main_connection).find(
            filter_data={'org_id': self.organization['id']},
            fields=['id', 'services.slug', 'services.id', 'services.paid_by_license']
        )
        # проверим, что у всех есть доступ к трекеру, потому что у него еще не кончился триальный период, и wiki
        assert_that(
            users,
            contains(
                has_entries(
                    id=self.users['user1']['id'],
                    services=contains_inanyorder(
                        has_entries(
                            id=self.services['tracker']['id'],
                            slug='tracker',
                            paid_by_license=True,
                        ),
                        has_entries(
                            id=self.services['wiki']['id'],
                            slug='wiki',
                            paid_by_license=False,
                        ),
                    ),
                ),
                has_entries(
                    id=self.users['user2']['id'],
                    services=contains_inanyorder(
                        has_entries(
                            id=self.services['tracker']['id'],
                            slug='tracker',
                            paid_by_license=True,
                        ),
                        has_entries(
                            id=self.services['wiki']['id'],
                            slug='wiki',
                            paid_by_license=False,
                        ),
                        has_entries(
                            id=self.services['abc']['id'],  # есть лицензия
                            slug='abc',
                            paid_by_license=True,
                        )
                    ),
                ),
                has_entries(
                    id=self.user['id'],
                    services=contains_inanyorder(
                        has_entries(
                            id=self.services['tracker']['id'],
                            slug='tracker',
                            paid_by_license=True,
                        ),
                        has_entries(
                            id=self.services['wiki']['id'],
                            slug='wiki',
                            paid_by_license=False,
                        ),
                    ),
                )
            )
        )

    def test_get_user_service_direct_group_relations(self):
        # проверяем, что у пользователей есть доступ к сервису, если лицензии выданы на группу, в которой они состоят

        # включим user1 в группу python
        GroupModel(self.main_connection).update_one(
            org_id=self.organization['id'],
            group_id=self.groups['python']['id'],
            data={
                'members': [
                    {
                        'type': 'user',
                        'id': self.users['user1']['id']
                    }
                    ]
            }
        )

        # включим user2 в группу ruby
        GroupModel(self.main_connection).update_one(
            org_id=self.organization['id'],
            group_id=self.groups['ruby']['id'],
            data={
                'members': [
                    {
                        'type': 'user',
                        'id': self.users['user2']['id']
                    }
                ]
            }
        )
        # дадим группе python доступ к abc
        relations_python = [
            {
                'name': 'member',
                'group_id': self.groups['python']['id'],
            },
        ]
        ResourceModel(self.main_connection).update_relations(
            resource_id=self.resource_id_abc,
            org_id=self.organization['id'],
            relations=relations_python,
        )

        # дадим группе ruby доступ к tracker
        relations_ruby = [
            {
                'name': 'member',
                'group_id': self.groups['ruby']['id'],
            },
        ]
        ResourceModel(self.main_connection).update_relations(
            resource_id=self.resource_id_tracker,
            org_id=self.organization['id'],
            relations=relations_ruby,
        )
        # обновим таблицу лицензий
        for service_id in [self.services['tracker']['id'], self.services['abc']['id']]:
            UserServiceLicenses(self.main_connection).update_licenses_cache(self.organization['id'], service_id)

        # обновляем дату окончания триального периода трекера
        self.update_service_trial_expires_date(
            self.organization['id'],
            self.services['tracker']['id'],
            utcnow() - datetime.timedelta(days=50)
        )
        users = UserModel(self.main_connection).find(
            filter_data={
                'org_id': self.organization['id'],
                'id': [self.users['user1']['id'], self.users['user2']['id']]
            },
            fields=['id', 'services.slug', 'services.id', 'services.paid_by_license']
        )

        # abc доступен только группе python, tracker только в группе ruby
        self._check_services(users[0], user_service=self.services['abc'])
        self._check_services(users[1], user_service=self.services['tracker'])

    def test_get_user_service_through_2_depth_group_relations(self):
        # проверяем, что у пользователей есть доступ к сервису,
        # если лицензии выданы на группу, а пользователи находятся в подгруппе

        # включим группу python в normal_programming_languages
        GroupModel(self.main_connection).update_one(
            org_id=self.organization['id'],
            group_id=self.groups['normal_programming_languages']['id'],
            data={
                'members': [
                    {
                        'type': 'group',
                        'id': self.groups['python']['id']
                    }
                ]
            }
        )
        # включим user1 в группу python
        GroupModel(self.main_connection).update_one(
            org_id=self.organization['id'],
            group_id=self.groups['python']['id'],
            data={
                'members': [
                    {
                        'type': 'user',
                        'id': self.users['user1']['id'],
                    }
                ]
            }
        )
        # дадим группе normal_programming_languages доступ к tracker
        relations = [
            {
                'name': 'member',
                'group_id': self.groups['normal_programming_languages']['id'],
            },
        ]
        ResourceModel(self.main_connection).update_relations(
            resource_id=self.resource_id_tracker,
            org_id=self.organization['id'],
            relations=relations,
        )
        # обновим таблицу лицензий
        for service_id in [self.services['tracker']['id'], self.services['abc']['id']]:
            UserServiceLicenses(self.main_connection).update_licenses_cache(self.organization['id'], service_id)

        # обновляем дату окончания триального периода трекера
        self.update_service_trial_expires_date(
            self.organization['id'],
            self.services['tracker']['id'],
            utcnow() - datetime.timedelta(days=50)
        )
        users = UserModel(self.main_connection).find(
            filter_data={
                'org_id': self.organization['id'],
                'id': [self.users['user1']['id'], self.users['user2']['id']]
            },
            fields=['id', 'services.slug', 'services.id', 'services.paid_by_license']
        )

        # проверим, что tracker доступен user1, у user2 доступ только к wiki
        self._check_services(users[0], user_service=self.services['tracker'])
        self._check_services(users[1])

    def test_get_user_service_direct_department_relations(self):
        # проверяем, что у пользователей есть доступ к сервису,
        # если лицензии выданы на отдел, в котором они состоят

        # включим user1 в business_development департамент
        UserModel(self.main_connection).update_one(
            update_data={
                'department_id': self.departments['business_development']['id']
            },
            filter_data={
                'id': self.users['user1']['id'],
                'org_id': self.organization['id']
            }
        )

        # включим user2 в tech департамент
        UserModel(self.main_connection).update_one(
            update_data={
                'department_id': self.departments['tech']['id']
            },
            filter_data={
                'id': self.users['user2']['id'],
                'org_id': self.organization['id']
            }
        )
        # дадим business_development департаменту доступ к трекеру
        relations = [
            {
                'name': 'member',
                'department_id': self.departments['business_development']['id'],
            },
        ]
        ResourceModel(self.main_connection).update_relations(
            resource_id=self.resource_id_tracker,
            org_id=self.organization['id'],
            relations=relations,
        )
        # дадим tech департаменту доступ к abc
        relations = [
            {
                'name': 'member',
                'department_id': self.departments['tech']['id'],
            },
        ]
        ResourceModel(self.main_connection).update_relations(
            resource_id=self.resource_id_abc,
            org_id=self.organization['id'],
            relations=relations,
        )
        # обновим таблицу лицензий
        for service_id in [self.services['tracker']['id'], self.services['abc']['id']]:
            UserServiceLicenses(self.main_connection).update_licenses_cache(self.organization['id'], service_id)

        # обновляем дату окончания триального периода трекера
        self.update_service_trial_expires_date(
            self.organization['id'],
            self.services['tracker']['id'],
            utcnow() - datetime.timedelta(days=50)
        )
        users = UserModel(self.main_connection).find(
            filter_data={
                'org_id': self.organization['id'],
                'id': [self.users['user1']['id'], self.users['user2']['id']]
            },
            fields=['id', 'services.slug', 'services.id', 'services.paid_by_license']
        )

        # проверим, что у user1 есть доступ к трекеру, у user2 доступ к abc
        self._check_services(users[0], user_service=self.services['tracker'])
        self._check_services(users[1], user_service=self.services['abc'])

    def test_get_user_service_through_2_depth_department_relations(self):
        # проверяем, что у пользователей есть доступ к сервису,
        # если лицензии выданы на отдел, а пользователи находятся в под отделе

        # включим business департамент в business_development
        DepartmentModel(self.main_connection).update_one(
            id=self.departments['business_development']['id'],
            org_id=self.organization['id'],
            data={
                'parent_id': self.departments['business']['id']
            }
        )
        # включим user1 в business_development департамент
        UserModel(self.main_connection).update_one(
            update_data={
                'department_id': self.departments['business_development']['id']
            },
            filter_data={
                'id': self.users['user1']['id'],
                'org_id': self.organization['id']
            }
        )
        # дадим business_development доступ к tracker
        relations = [
            {
                'name': 'member',
                'department_id': self.departments['business_development']['id'],
            },
        ]
        ResourceModel(self.main_connection).update_relations(
            resource_id=self.resource_id_tracker,
            org_id=self.organization['id'],
            relations=relations,
        )
        # обновим таблицу лицензий
        for service_id in [self.services['tracker']['id'], self.services['abc']['id']]:
            UserServiceLicenses(self.main_connection).update_licenses_cache(self.organization['id'], service_id)

        # обновляем дату окончания триального периода трекера
        self.update_service_trial_expires_date(
            self.organization['id'],
            self.services['tracker']['id'],
            utcnow() - datetime.timedelta(days=50)
        )
        users = UserModel(self.main_connection).find(
            filter_data={
                'org_id': self.organization['id'],
                'id': [self.users['user1']['id'], self.users['user2']['id']]
            },
            fields=['id', 'services.slug', 'services.id', 'services.paid_by_license']
        )

        # проверим, что у user1 есть доступ к трекеру, у user2 доступ только к wiki
        self._check_services(users[0], user_service=self.services['tracker'])
        self._check_services(users[1])


class TestUserModel_filter_services(TestCase):
    def setUp(self):
        super(TestUserModel_filter_services, self).setUp()
        self.tracker = ServiceModel(self.meta_connection).create(
            client_id='some-client-id1',
            slug='tracker',
            name='tracker',
            robot_required=False,
            trial_period_months=1,
            paid_by_license=True,
            ready_default=True,
        )
        self.wiki = ServiceModel(self.meta_connection).create(
            client_id='some-client-id2',
            slug='wiki',
            name='wiki',
            robot_required=False,
            ready_default=True,
        )
        self.abc = ServiceModel(self.meta_connection).create(
            client_id='some-client-id3',
            slug='abc',
            name='abc',
            robot_required=False,
            ready_default=True,
            trial_period_months=0,
            paid_by_license=True,
        )
        self.services = [self.tracker, self.wiki, self.abc]
        # синхронизируем таблицу services
        UpdateServicesInShards().try_run()
        for service in self.services:
            enable_service(
                self.meta_connection,
                self.main_connection,
                self.organization['id'],
                service['slug']
            )
        self.resource_id_tracker = OrganizationServiceModel(self.main_connection).find(
            filter_data={'service_id': self.tracker['id']},
            one=True,
        )['resource_id']
        self.resource_id_abc = OrganizationServiceModel(self.main_connection).find(
            filter_data={'service_id': self.abc['id']},
            one=True,
        )['resource_id']
        self.users = [self.create_user() for _ in range(4)]

    def test_common_service(self):
        # обычный сервис достпен всем
        assert_that(
            only_ids(UserModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                    'service': self.wiki['slug'],
                },
                fields=['id']
            )),
            contains_inanyorder(
                *only_ids([self.user] + self.users)
            )
        )

    def test_licensed_services(self):
        # abc недоступен никому, потому что триального периода у него нет и лицензий тоже нет
        assert_that(
            UserModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                    'service': self.abc['slug'],
                },
                fields=['id']
            ),
            equal_to([])
        )

        # tracker доступен всем, потому что в триальном периоде
        assert_that(
            only_ids(UserModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                    'service': self.tracker['slug'],
                },
                fields=['id']
            )),
            contains_inanyorder(
                *only_ids([self.user] + self.users)
            )
        )

        # выдаем лицензии
        self.create_licenses_for_service(self.abc['id'], user_ids=only_ids(self.users[:2]))
        self.create_licenses_for_service(self.tracker['id'], user_ids=only_ids([self.users[0], self.users[2]]))

        # обновляем дату окончания триального периода трекера
        self.update_service_trial_expires_date(
            self.organization['id'],
            self.tracker['id'],
            utcnow() - datetime.timedelta(days=50)
        )
        self.process_tasks()

        # проверяем, что сервисы доступны только тем, у кого есть лицензии
        assert_that(
            only_ids(UserModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                    'service': self.abc['slug'],
                },
                fields=['id']
            )),
            contains_inanyorder(
                *only_ids(self.users[:2])
            )
        )

        assert_that(
            only_ids(UserModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                    'service': self.tracker['slug'],
                },
                fields=['id']
            )),
            contains_inanyorder(
                *only_ids([self.users[0], self.users[2]])
            )
        )


class TestUsersAnalyticsInfoModel(TestCase):
    def test_save_analytics(self):
        # проверяем, что удаляются старые данные и считаются данные на сегодня
        date1 = utcnow() - datetime.timedelta(days=10)
        date2 = utcnow() - datetime.timedelta(days=6)
        uids = []

        with patch('intranet.yandex_directory.src.yandex_directory.core.models.user.utcnow', return_value=date1):
            for _ in range(2):
                uids.append(self.create_user()['id'])

            # считаем аналитику
            UsersAnalyticsInfoModel(self.main_connection).save(self.organization['id'])

        with patch('intranet.yandex_directory.src.yandex_directory.core.models.user.utcnow', return_value=date2):
            for _ in range(3):
                uids.append(self.create_user()['id'])

            UserModel(self.main_connection).dismiss(
                org_id=self.organization['id'],
                user_id=uids[-1],
                author_id=uids[-1],
            )

            # считаем аналитику
            UsersAnalyticsInfoModel(self.main_connection).save(self.organization['id'])

        # в таблице 3 записи на первую дату (+ админ)
        assert_that(
            UsersAnalyticsInfoModel(self.main_connection).find(filter_data={'for_date': date1}),
            has_length(3)
        )

        # в таблице 5 записей на вторую дату (- уволенный) (+ админ)
        assert_that(
            UsersAnalyticsInfoModel(self.main_connection).find(filter_data={'for_date': date2}),
            has_length(5)
        )

        # сохраняем аналитику на сегодня, старые записи должны удалиться
        today = utcnow()
        UsersAnalyticsInfoModel(self.main_connection).save_analytics()
        # в таблице всего 5 записей (- уволенный) (+ админ)
        assert_that(
            UsersAnalyticsInfoModel(self.main_connection).find(),
            has_length(5)
        )
        # и все они за сегодня
        uids = uids[:-1] + [self.admin_uid]
        assert_that(
            only_attrs(UsersAnalyticsInfoModel(self.main_connection).find(
                filter_data={'for_date': today},
                fields=['uid']
            ), 'uid'),
            contains_inanyorder(*uids)
        )


class TestAdminsContactsAnalyticsInfoModel(TestCase):
    def test_save_analytics(self):
        # добавим еще одного админа
        admin2 = self.create_user()
        UserModel(self.main_connection).make_admin_of_organization(
            org_id=self.organization['id'],
            user_id=admin2['id'],
        )

        # включим трекер
        tracker = ServiceModel(self.meta_connection).create(
            client_id='some-client-id3',
            slug='tracker',
            name='tracker',
            robot_required=False,
            trial_period_months=1,
            paid_by_license=True,
        )

        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            tracker['slug'],
        )

        # считаем аналитику
        with patch('intranet.yandex_directory.src.yandex_directory.core.models.user.app.blackbox_instance.batch_userinfo') as get_batch_userinfo:
            get_batch_userinfo.return_value = [
                {'attributes': {'33': 'Europe/Moscow'},
                 'fields': {'aliases': [('7', 'valentina.mangolini@larus.it')],
                            'first_name': 'Valentina',
                            'language': 'en',
                            'last_name': 'Mangolini',
                            'login': 'valentina.mangolini@larus.it',
                            'phones': ['79122121222', '79222121111'],
                            },
                 'uid': str(admin2['id'])},
                {'attributes': {'33': 'Europe/Samara'},
                 'fields': {'aliases': [('7', 'test@larus.it')],
                            'first_name': 'test',
                            'last_name': 'test',
                            'login': 'test@larus.it',
                            'phones': ['79122121333'],
                            },
                 'uid': str(self.admin_uid)}
            ]

            AdminContactsAnalyticsInfoModel(self.main_connection).save_analytics()

        data_from_db = AdminContactsAnalyticsInfoModel(self.main_connection).filter().all()
        assert_that(
            data_from_db,
            has_length(2)
        )
        assert_that(
            data_from_db,
            contains_inanyorder(
                has_entries(
                    uid=admin2['id'],
                    utc='+03:00',
                    phones=['79122121222', '79222121111'],
                ),
                has_entries(
                    uid=self.admin_uid,
                    utc='+04:00',
                    phones=['79122121333'],
                ),
            )
        )


class TestUserModel_user_license_suggest(TestCase):
    def test_simple(self):
        # проверяем, что саджест возвращает пользователей с лицензиями
        service = ServiceModel(self.meta_connection).create(
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
            service['slug'])
        user1 = self.create_user(nickname='user1')
        user2 = self.create_user(name={'first': {'ru': 'User2'}})
        user3 = self.create_user(nickname='user_three')
        user4 = self.create_user(name={'first': {'ru': 'cool'}, 'last': {'ru': 'guy'}})

        members = [
            {'type': 'user', 'object': user2},
            {'type': 'user', 'object': user3},
            {'type': 'user', 'object': user4},
        ]
        group = self.create_group(members=members)
        self.create_licenses_for_service(service['id'], group_ids=[group['id']])

        self.process_tasks()

        assert_that(
            UserServiceLicenses(self.main_connection).find(),
            has_length(3)
        )

        suggest_user = UserModel(self.main_connection).user_license_suggest(
            self.organization['id'],
            service['id'],
            'use'
        )
        assert_that(
            suggest_user,
            has_length(2)
        )
        assert_that(
            suggest_user,
            contains_inanyorder(
                has_entries(
                    id=user2['id'],
                    via_group_id=group['id'],
                    via_department_id=None,
                ),
                has_entries(
                    id=user3['id'],
                    via_group_id=group['id'],
                    via_department_id=None,
                )
            )
        )
