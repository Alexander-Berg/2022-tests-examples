# -*- coding: utf-8 -*-
import datetime
import json
import collections
import pdb
from copy import (
    deepcopy,
)

from frozendict import frozendict
from hamcrest import (
    assert_that,
    has_entries,
    has_entry,
    contains_inanyorder,
    equal_to,
    contains,
    empty,
    not_none,
    none,
    has_length,
    has_items,
    has_item,
    is_,
    is_not,
    has_key,
    not_,
    all_of,
    starts_with,
    anything,
    contains_string,
)
from unittest.mock import (
    patch,
    Mock,
    PropertyMock,
    ANY,
)
from werkzeug.datastructures import FileMultiDict

from testutils import (
    frozen_time,
    is_same,
    has_only_entries,
    get_auth_headers,
    PaginationTestsMixin,
    TestYandexTeamOrgMixin,
    create_organization,
    create_department,
    create_user,
    create_group,
    set_sso_in_organization,
    TestCase,
    set_auth_uid,
    create_robot_for_anothertest,
    OAUTH_CLIENT_ID,
    oauth_success,
    get_oauth_headers,
    scopes,
    mocked_blackbox,
    assert_called_once,
    assert_not_called,
    calls_count,
    mocked_passport,
    create_outer_admin,
    get_all_admin_permssions,
    get_deputy_admin_permissions,
    TestOrganizationWithoutDomainMixin,
    create_organization_without_domain,
    create_yandex_user,
    source_path,
    MockToDict,
    fake_userinfo,
)
from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.auth.scopes import scope
from intranet.yandex_directory.src.yandex_directory.auth.user import User
from intranet.yandex_directory.src.yandex_directory.common.db import (
    catched_sql_queries,
)
from intranet.yandex_directory.src.yandex_directory.common.models.types import (
    TYPE_USER,
    ROOT_DEPARTMENT_ID,
)
from intranet.yandex_directory.src.yandex_directory.common.utils import (
    utcnow,
    format_date,
    parse_birth_date,
    lstring,
    validate_data_by_schema,
    url_join,
)
from intranet.yandex_directory.src.yandex_directory.core.actions import action
from intranet.yandex_directory.src.yandex_directory.core.commands.update_services_in_shards import Command as UpdateServicesInShards
from intranet.yandex_directory.src.yandex_directory.core.models import (
    UserModel,
    UserDismissedModel,
    UserMetaModel,
    ResourceModel,
    DepartmentModel,
    GroupModel,
    UserGroupMembership,
    ActionModel,
    EventModel,
    OrganizationModel,
    ServiceModel,
    OrganizationServiceModel,
    OrganizationSsoSettingsModel,
    DomainModel,
    OrganizationMetaModel,
)
from intranet.yandex_directory.src.yandex_directory.core.models.service import (
    enable_service,
    MAILLIST_SERVICE_SLUG,
)
from intranet.yandex_directory.src.yandex_directory.core.permission.permissions import (
    get_permissions,
    user_permissions,
    global_permissions,
)
from intranet.yandex_directory.src.yandex_directory.core.utils import (
    prepare_user,
    build_email,
    get_localhost_ip_address,
    only_ids,
    only_fields,
    PRIVATE_USER_FIELDS,
)
from intranet.yandex_directory.src.yandex_directory.core.utils.robots import create_robot_for_service_and_org_id
from intranet.yandex_directory.src.yandex_directory.core.views.users import (
    USERS_OUT_SCHEMA,
)
from intranet.yandex_directory.src.yandex_directory.core.features import (
    MULTIORG,
    set_feature_value_for_organization,
)
from intranet.yandex_directory.src.yandex_directory.passport.exceptions import (
    AccountDisabled,
    AliasNotFound,
    ChangeAvatarInvalidFileSize,
    ChangeAvatarInvalidImageSize,
    ChangeAvatarInvalidUrl,
    DomainInvalidType,
    FileInvalid,
    FormInvalid,
    LoginEmpty,
    LoginNotavailable,
    LoginProhibitedsymbols,
    PassportException,
    PassportUnavailable,
    PasswordLong,
    InvalidTimezone,
    InvalidLanguage,
    AliasExists,
    FirstnameInvalid,
)


def prepare_entity(main_connection, entity):
    return prepare_user(
        main_connection,
        entity,
        expand_contacts=True,
        api_version=1,
    )


expected_keys = [
    'id',
    'login',
    'nickname',
    'email',
    'name',
    'gender',
    'department',
    'groups',
    'position',
    'about',
    'birthday',
    'contacts',
    'aliases',
    'is_dismissed',
    'is_admin',
    'external_id',
    'user_type',
    'role',
]

position = {
    'ru': 'Разработчик',
    'en': 'Developer'
}
another_position = {
    'ru': 'Дизайнер',
    'en': 'Designer'
}

about = {
    'ru': 'RU about',
    'en': 'EN about',
}
another_about = {
    'ru': 'RU another about',
    'en': 'EN another about',
}

birthday = datetime.date(day=2, month=3, year=1990)
another_birthday = birthday + datetime.timedelta(days=1)

contacts = [
    frozendict({
        'type': 'skype',
        'label': {
            'ru': 'Домашний',
            'en': 'Homie'
        },
        'value': 'polina-sosisa'
    }),
    frozendict({
        'type': 'email',
        'label': {
            'ru': 'Украденный',
            'en': 'Stolen'
        },
        'value': 'obama@usa.com'
    }),
]
another_contacts = [
    frozendict({
        'type': 'skype',
        'label': {
            'ru': 'Рабочий',
            'en': 'Work'
        },
        'value': 'web-chib'
    }),
    frozendict({
        'type': 'email',
        'label': {
            'ru': 'Мой',
            'en': 'My'
        },
        'value': 'web-chib@usa.com'
    }),
]


class BaseMixin(object):
    def setUp(self):
        super(BaseMixin, self).setUp()
        self.department = DepartmentModel(self.main_connection).create(
            name={'en': 'Marketing'},
            org_id=self.organization['id']
        )
        self.groups = {
            'admins': GroupModel(self.main_connection).create(
                name={'en': 'Admins'},
                org_id=self.organization['id']
            ),
            'managers': GroupModel(self.main_connection).create(
                name={'en': 'Managers'},
                org_id=self.organization['id']
            ),
        }
        self.groups_ids = [i['id'] for i in list(self.groups.values())]
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
        self.dismissed_user = self.create_user()
        UserModel(self.main_connection).dismiss(
            self.organization['id'],
            self.dismissed_user['id'],
            self.admin_uid
        )
        self.update_department_members_count()

    def has_permissions(self, uid, permissions, admin_id=None):
        """Вспомогательный метод для проверки, что
        у admin_uid есть указанные права для
        работы с пользователем с заданным uid.

        Если admin_uid не задан, то используется self.admin_user['id'].
        """
        if admin_id is None:
            admin_id = self.admin_user['id']

        self.auth_user = User(
            admin_id,
            '127.0.0.1'
        )
        return self.auth_user.has_permissions(
            self.meta_connection,
            self.main_connection,
            permissions=permissions,
            object_type=TYPE_USER,
            object_id=uid,
            org_id=self.organization['id'],
        )

    def update_department_members_count(self):
        department_ids = only_ids(
            DepartmentModel(self.main_connection).find({'org_id': self.organization['id']})
        )
        DepartmentModel(self.main_connection).update_members_count(
            department_ids=department_ids,
            org_id=self.organization['id']
        )


class TestUserList__get(PaginationTestsMixin, BaseMixin, TestCase):
    entity_list_url = '/users/'
    entity_model = UserModel
    entity_model_select_related = ['department']
    entity_model_prefetch_related = ['groups']

    def create_entity(self):
        self.entity_counter += 1

        return self.create_user()

    # def prepare_entity_for_api_response(self, entity):
    #     return prepare_entity(self.main_connection, entity)

    def test_redirect_to_slash(self):
        # Роуты должны всегда оканчиваться на обратный слэш.
        # Если это не так, то должен отдаваться редирект.
        response, headers = self.get_json(
            '/users',
            raw=True,
            expected_code=308,
            return_headers=True,
        )
        assert_that(
            headers,
            has_entries(
                location='http://localhost/users/',
            )
        )

    def test_simple(self):
        user_1 = self.create_user(
            department_id=self.department['id'],
            groups=self.groups_ids
        )
        user_2 = self.create_user(
            department_id=self.department['id'],
            groups=self.groups_ids
        )
        response = self.get_json('/users/')['result']
        self.assertEqual(sorted(response[0].keys()), sorted(expected_keys))

        root_department = has_entries(
            id=1,
            name={'en': 'All employees',
                  'ru': '\u0412\u0441\u0435 \u0441\u043e\u0442\u0440\u0443\u0434\u043d\u0438\u043a\u0438'},
            description={'ru': ''},
            email=None,
            external_id=None,
            label=None,
            members_count=1,
            parent_id=None,
            removed=False,
        )
        marketing = has_entries(
            id=2,
            name={'en': 'Marketing'},
            description={'ru': ''},
            email=None,
            external_id=None,
            label=None,
            members_count=2,
            parent_id=None,  # Это баг, но оно всегда так было
            removed=False,
        )

        def group_matcher(**kwargs):
            default_entries = {
                'id': ANY,
                'created': ANY,
                'description': {'en': ANY, 'ru': ANY},
                'email': None,
                'external_id': None,
                'label': None,
                'members_count': 0,
                'name': ANY,
                'type': 'generic',
                'author_id': ANY,
            }
            entries = deepcopy(default_entries)
            entries.update(kwargs)
            return has_entries(**entries)

        group2 = group_matcher(
            created=ANY,
            id=2,
            members_count=1,
            name={'en': 'Organization administrator',
                  'ru': '\u0410\u0434\u043c\u0438\u043d\u0438\u0441\u0442\u0440\u0430\u0442\u043e\u0440 \u043e\u0440\u0433\u0430\u043d\u0438\u0437\u0430\u0446\u0438\u0438'},
            type='organization_admin',
        )
        group4 = group_matcher(
            id=self.groups['admins']['id'],
            members_count=2,
            name={'en': 'Admins'},
        )
        group5 = has_entries(
            id=self.groups['managers']['id'],
            members_count=2,
            name={'en': 'Managers'},
        )
        staff_link = url_join(app.config['STAFF_URL'], '{nickname}?org_id={org_id}&uid={uid}').format(
            tld=self.tld,
            nickname='admin',
            org_id=self.user['org_id'],
            uid=self.user['id'],
        )
        admin = has_entries(
            nickname='admin',
            name={'first': {'ru': 'Admin'}, 'last': {'ru': 'Adminovich'}},
            about=None,
            aliases=[],
            birthday=ANY,
            contacts=contains(
                has_entries(
                    type='staff',
                    value=staff_link,
                    synthetic=True,
                    alias=False,
                    main=False,
                ),
                has_entries(
                    alias=False,
                    main=True,
                    synthetic=True,
                    type='email',
                    value='admin@not_yandex_test.ws.autotest.yandex.ru',
                )
            ),
            department=root_department,
            email='admin@not_yandex_test.ws.autotest.yandex.ru',
            external_id=None,
            gender='male',
            groups=contains_inanyorder(group2),
            id=ANY,
            is_admin=True,
            is_dismissed=False,
            login='admin',
            position=None
        )
        staff_link = url_join(app.config['STAFF_URL'], '{nickname}?org_id={org_id}&uid={uid}').format(
            tld=self.tld,
            nickname='test-2',
            org_id=user_1['org_id'],
            uid=user_1['id'],
        )
        user1 = has_entries(
            about=None,
            aliases=[],
            birthday=None,
            contacts=contains(
                has_entries(
                    type='staff',
                    value=staff_link,
                    synthetic=True,
                    alias=False,
                    main=False,
                ),
                has_entries(
                    alias=False,
                    main=True,
                    synthetic=True,
                    type='email',
                    value='test-2@not_yandex_test.ws.autotest.yandex.ru',
                )
            ),
            department=marketing,
            email='test-2@not_yandex_test.ws.autotest.yandex.ru',
            external_id=None,
            gender='male',
            groups=contains_inanyorder(group4, group5),
            id=ANY,
            is_admin=False,
            is_dismissed=False,
            login='test-2',
            name={'first': {'en': 'Gennady', 'ru': '\u0413\u0435\u043d\u043d\u0430\u0434\u0438\u0439'},
                  'last': {'en': 'Chibisov', 'ru': '\u0427\u0438\u0431\u0438\u0441\u043e\u0432'},
                  'middle': {'en': 'Chibisovich', 'ru': '\u0427\u0438\u0431\u0438\u0441\u043e\u0432\u0438\u0447'}},
            nickname='test-2',
            position=None,
        )
        staff_link = url_join(app.config['STAFF_URL'], '{nickname}?org_id={org_id}&uid={uid}').format(
            tld=self.tld,
            nickname='test-3',
            org_id=user_2['org_id'],
            uid=user_2['id'],
        )

        user2 = has_entries(
            name={'first': {'en': 'Gennady', 'ru': '\u0413\u0435\u043d\u043d\u0430\u0434\u0438\u0439'},
                  'last': {'en': 'Chibisov', 'ru': '\u0427\u0438\u0431\u0438\u0441\u043e\u0432'},
                  'middle': {'en': 'Chibisovich', 'ru': '\u0427\u0438\u0431\u0438\u0441\u043e\u0432\u0438\u0447'}},
            nickname='test-3',
            about=None,
            aliases=[],
            birthday=None,
            contacts=contains(
                has_entries(
                    type='staff',
                    value=staff_link,
                    synthetic=True,
                    alias=False,
                    main=False,
                ),
                has_entries(
                    alias=False,
                    main=True,
                    synthetic=True,
                    type='email',
                    value='test-3@not_yandex_test.ws.autotest.yandex.ru',
                )
            ),
            department=marketing,
            email='test-3@not_yandex_test.ws.autotest.yandex.ru',
            external_id=None,
            gender='male',
            groups=contains_inanyorder(group4, group5),
            id=ANY,
            is_admin=False,
            is_dismissed=False,
            login='test-3',
            position=None,
        )
        by_nickname = {user['nickname']: user
                       for user in response}
        assert_that(response, has_length(3))
        assert_that(by_nickname['admin'], admin)
        assert_that(by_nickname['test-2'], user1)
        assert_that(by_nickname['test-3'], user2)

    def test_get_works_when_only_org_id_header_was_given(self):
        headers = get_auth_headers(as_org=self.organization)
        response = self.get_json('/users/', headers=headers)
        assert len(response['result']) > 0

    def test_department_id_filter(self):
        user = self.create_user(department_id=self.department['id'])
        another_department = self.create_department(
            parent_id=self.department['id']
        )
        self.create_user(department_id=another_department['id'])
        response = self.client.get(
            '/users/?department_id=%s' % self.department['id'],
            headers=get_auth_headers())
        self.assertEqual(response.status_code, 200)
        response_data = json.loads(response.data).get('result')
        self.assertEqual(sorted(response_data[0].keys()), sorted(expected_keys))
        assert_that(response_data, contains_inanyorder(
            has_entries(id=user['id']),
        ))

    def test_org_id_zero(self):
        # проверяем, что если org_id = 0 то возвращаем 404, а не 500
        response = self.get_json('/users/', headers=get_auth_headers(as_org=0), expected_code=404)

        assert_that(
            response,
            equal_to({
                'message': 'Organization was deleted',
                'code': 'organization_deleted',
            })
        )

    def test_recursive_department_id_filter(self):
        """
        Проверяем развернутую выдачу с фильтром по департаменту:
        department_id=id
        Есть два департамента, один вложен в в другой и по одному юзеру в
        каждом.
        """
        user1 = self.create_user(department_id=self.department['id'])
        child_department = self.create_department(
            parent_id=self.department['id']
        )
        user2 = self.create_user(department_id=child_department['id'])
        response_data = self.get_json(
            '/users/?recursive_department_id=%s' % self.department['id']
        )['result']
        assert_that(response_data, contains_inanyorder(
            has_entries(id=user1['id']),
            has_entries(id=user2['id']),
        ))

    def test_nickname_filter(self):
        """
        Проверяем выдачу с фильтром по nickname
        """
        user1 = self.create_user(
            department_id=self.department['id'],
            nickname='user1'
        )
        user2 = self.create_user(
            department_id=self.department['id'],
            nickname='user2'
        )
        response_data = self.get_json('/users/?nickname=user1')['result']
        assert_that(response_data, contains(
            has_entries(id=user1['id']),
        ))
        response_data = self.get_json('/users/?nickname=user2')['result']
        assert_that(response_data, contains(
            has_entries(id=user2['id']),
        ))
        response_data = self.get_json('/users/?nickname=user1,user2')['result']
        assert_that(response_data, contains_inanyorder(
            has_entries(id=user1['id']),
            has_entries(id=user2['id']),
        ))
        response_data = self.get_json('/users/?nickname=userdkslfj')['result']
        assert_that(response_data, empty())
        # nickname число
        response_data = self.get_json('/users/?nickname=123456')['result']
        assert_that(response_data, empty())
        # nickname массив число
        response_data = self.get_json('/users/?nickname=123,456')['result']
        assert_that(response_data, empty())
        # nickname  массив число+строка
        response_data = self.get_json('/users/?nickname=123,abc')['result']
        assert_that(response_data, empty())

    def test_multiple_group_filter(self):
        org_id = self.organization['id']
        first_group = create_group(
            self.main_connection,
            org_id=org_id
        )['id']
        second_group = create_group(
            self.main_connection,
            org_id=org_id
        )['id']

        vasya = create_user(
            self.meta_connection,
            self.main_connection,
            user_id=100,
            nickname='vasya',
            name={'first': lstring('Vasya')},
            email='vasya@ya.ru',
            groups=[first_group, second_group],
            org_id=org_id,
        )['id']
        petya = create_user(
            self.meta_connection,
            self.main_connection,
            user_id=101,
            nickname='petya',
            name={'first': lstring('Petya')},
            email='petya@ya.ru',
            groups=[first_group],
            org_id=org_id,
        )['id']
        kolya = create_user(
            self.meta_connection,
            self.main_connection,
            user_id=102,
            nickname='kolya',
            name={'first': lstring('Kolya')},
            email='kolya@ya.ru',
            groups=[second_group],
            org_id=org_id,
        )['id']

        # first_group включает и vasya and petya
        response = self.get_json('/users/?group_id=%s' % first_group)['result']
        assert_that(
            response,
            contains_inanyorder(
                has_entries(nickname='vasya'),
                has_entries(nickname='petya'),
            )
        )

        # first_group и second_group включают всех троих
        response = self.get_json('/users/?group_id={0},{1}'.format(
            first_group, second_group)
        )['result']

        assert_that(
            response,
            contains_inanyorder(
                has_entries(nickname='vasya'),
                has_entries(nickname='petya'),
                has_entries(nickname='kolya'),
            )
        )

    def test_group_id_filter(self):
        """
                group2
                /    \
            group1   user2
            /
         user1
        """
        user1 = self.create_user()
        group1 = self.create_group(
            members=[{'type': 'user', 'id': user1['id']}]
        )
        user2 = self.create_user()
        members = [{'type': 'group', 'id': group1['id']},
                   {'type': 'user', 'id': user2['id']}]
        group2 = self.create_group(members=members)
        response = self.get_json('/users/?group_id={0}'.format(
            group2['id'])
        )['result']

        assert_that(
            response,
            contains_inanyorder(
                has_entries(id=user2['id']),
            )
        )

    def test_role_filter(self):
        user1 = self.create_user()
        user2 = self.create_user()

        self.patch_json(
            '/users/%s/' % user1['id'],
            data={'role': 'deputy_admin'},
            headers=get_auth_headers(as_uid=self.admin_uid),
        )
        response = self.get_json('/users/?role=admin,deputy_admin')['result']
        assert_that(
            response,
            contains_inanyorder(
                has_entries(id=self.admin_uid),
                has_entries(id=user1['id']),
            )
        )

        response = self.get_json('/users/?role=admin')['result']
        assert_that(
            response,
            contains_inanyorder(
                has_entries(id=self.admin_uid),
            )
        )

        response = self.get_json('/users/?role=deputy_admin')['result']
        assert_that(
            response,
            contains_inanyorder(
                has_entries(id=user1['id']),
            )
        )

    def test_non_existed_group_id_filter(self):
        response = self.get_json('/users/?group_id=123456')['result']

        assert_that(
            response,
            equal_to([])
        )

    def test_recursive_group_id_filter(self):
        """
                group2
                /    \
            group1   user2
            /
         user1
        """
        user1 = self.create_user()
        group1 = self.create_group(
            members=[{'type': 'user', 'id': user1['id']}]
        )
        user2 = self.create_user()
        members = [{'type': 'group', 'id': group1['id']},
                   {'type': 'user', 'id': user2['id']}]
        group2 = self.create_group(members=members)
        response = self.get_json('/users/?recursive_group_id={0}'.format(
            group2['id'])
        )['result']

        assert_that(
            response,
            contains_inanyorder(
                has_entries(id=user1['id']),
                has_entries(id=user2['id']),
            )
        )

    def test_should_show_items_of_current_user_organization(self):
        another_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']

        self.create_user(
            uid=1,
            nickname='web-chib',
            name=self.name,
            email='web-chib@ya.ru',
            org_id=another_organization['id']
        )

        response = self.client.get('/users/', headers=get_auth_headers())
        response_data = json.loads(response.data).get('result')
        self.assertNotIn(1, [i['id'] for i in response_data])

    def test_pagination__no_entities(self):
        """Скипаем, т.к. в users всегда будет как минимум текущий пользователь"""
        pass

    def test_id_filters_is_not_int(self):
        not_int_id = '232374))'
        self.get_json('/users/?department_id=%s' % not_int_id,
                      expected_code=422)
        self.get_json('/users/?id=%s' % not_int_id,
                      expected_code=422)
        self.get_json('/users/?group_id=%s' % not_int_id,
                      expected_code=422)
        combined_uri = '/users/?department_id=%s&group_id=%s' % (not_int_id,
                                                                 not_int_id)
        self.get_json(combined_uri, expected_code=422)

    def test_group_has_no_user_members(self):
        # список членов группы для групп состоящих только из отделов и других групп
        # не должно быть админов группы в списке членов группы
        group = create_group(
            self.main_connection,
            org_id=self.organization['id']
        )
        user = self.create_user()
        department = create_department(
            self.main_connection,
            org_id=group['org_id']
        )
        another_group = create_group(
            self.main_connection,
            org_id=group['org_id']
        )
        members = [
            {
                'type': 'department',
                'id': department['id'],
            }, {
                'type': 'group',
                'id': another_group['id'],
            },
        ]
        data = {
            'members': members,
            'admins': [{'type': TYPE_USER, 'id': user['id']}],
        }
        GroupModel(self.main_connection).update_one(
            org_id=group['org_id'],
            group_id=group['id'],
            data=data,
        )
        response = self.get_json(
            '/users/?group_id=%s&resource_relation_name=include' % group['id'],
        )

        assert_that(
            response['result'],
            equal_to([])
        )

        response = self.get_json(
            '/users/?recursive_group_id=%s' % group['id'],
        )

        assert_that(
            response['result'],
            equal_to([])
        )

    def test_is_dismissed_true_in_filter(self):
        # получаем только уволенных сотрудников

        response_data = self.get_json('/users/?is_dismissed=true')['result']
        assert_that(
            response_data,
            all_of(
                # в ответе есть уволенный сотрудник
                has_item(
                    has_entries(
                        id=self.dismissed_user['id'],
                        is_dismissed=True,
                    )
                ),
                # в ответе нет работающего сотрудника, хотя в базе он есть
                not_(
                    has_item(
                        has_entries(
                            is_dismissed=False
                        )
                    )
                )
            )
        )

    def test_is_dismissed_ignore_in_filter(self):
        # получаем всех сотрудников, включая уволенных

        response_data = self.get_json('/users/?is_dismissed=ignore')['result']
        assert_that(
            response_data,
            all_of(
                # в ответе есть уволенный сотрудник
                has_item(
                    has_entries(
                        id=self.dismissed_user['id'],
                        is_dismissed=True,

                    )
                ),
                # в ответе есть работающий сотрудник
                has_item(
                    has_entries(
                        is_dismissed=False,
                    )
                )
            )
        )

    def test_is_service_in_filter(self):
        # получаем роботных пользователей сервиса

        # создадим робота в организации
        robot_uid = create_robot_for_service_and_org_id(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            service_slug=self.service['slug'],
            org_id=self.organization['id'],
        )

        response_data = self.get_json('/users/?service_slug=%s' % self.service['slug'])
        # пользователь-робот есть в ответе ручки
        assert_that(
            response_data,
            has_entries(
                result=contains(
                    has_entries(
                        id=robot_uid
                    )
                )
            )
        )

    def test_id_gt_filter(self):
        # проверяем, что возвращаются пользователи с id больше указанного
        users = [self.create_user() for _ in range(5)]
        response_data = self.get_json('/users/?id__gt=%s' % users[1]['id'])
        assert_that(
            response_data['result'],
            contains(
                has_entries(
                    id=users[2]['id']
                ),
                has_entries(
                    id=users[3]['id']
                ),
                has_entries(
                    id=users[4]['id']
                ),
                has_entries(
                    id=self.user['id']
                ),
            )
        )

    def test_created_filter(self):
        users = [self.create_user() for _ in range(5)]
        old_date = '2018-06-01 17:02:55'
        for u in users[3:]:
            UserModel(self.main_connection).update_one(
                update_data={'created': old_date},
                filter_data={'id': u['id']}
            )

        response = self.get_json('/users/?created__lt=%s' % '2018-06-02')['result']
        assert_that(
            response,
            contains(
                has_entries(
                    id=users[3]['id']
                ),
                has_entries(
                    id=users[4]['id']
                ),
            )
        )

        response_data = self.get_json('/users/?created__gt=%s' % '2018-06-01')
        assert_that(
            response_data['result'],
            contains(
                has_entries(
                    id=users[0]['id']
                ),
                has_entries(
                    id=users[1]['id']
                ),
                has_entries(
                    id=users[2]['id']
                ),
                has_entries(
                    id=users[3]['id']
                ),
                has_entries(
                    id=users[4]['id']
                ),
                has_entries(
                    id=self.user['id']
                ),
            )
        )

    def test_ordering_by_name(self):
        org_id = self.organization['id']
        user_model = UserModel(self.main_connection)
        user_model.create(
            id=100,
            nickname='vasya',
            name={'last': {'ru': 'ddd'}, 'first': {'ru': 'Aab'}, 'middle': {'ru': 'H'}},
            email='vasya@ya.ru',
            gender='male',
            org_id=org_id,
        )
        user_model.create(
            id=500,
            nickname='petya',
            name={'last': {'en': 'ddd'}, 'first': {'en': 'Abc'}, 'middle': {'en': 'X'}},
            email='petya@ya.ru',
            gender='male',
            org_id=org_id,
        )
        user_model.create(
            id=300,
            nickname='kolya',
            name={'last': {'en': 'D e'}, 'first': {'en': 'Abch'}, 'middle': {'en': 'X'}},
            email='kolya@ya.ru',
            gender='male',
            org_id=org_id,
        )
        user_model.create(
            id=301,
            nickname='kolya1',
            name={'last': {'en': 'zer'}, 'first': {'en': 'Abc'}, 'middle': {'en': 'X'}},
            email='kolya1@ya.ru',
            gender='male',
            org_id=org_id,
        )
        user_model.create(
            id=200,
            nickname='sveta',
            name={'last': {'ru': 'Дабв'}, 'first': {'ru': 'Абв'}, 'middle': {'ru': 'А'}},
            email='sveta@ya.ru',
            gender='male',
            org_id=org_id,
        )
        user_model.create(
            id=400,
            nickname='tanya',
            name={'last': {'ru': 'деви'}, 'first': {'ru': 'Абв'}, 'middle': {'ru': 'Клмн'}},
            email='tanya@ya.ru',
            gender='male',
            org_id=org_id,
        )
        user_model.create(
            id=600,
            nickname='katya',
            name={'last': {'ru': 'Де ж'}, 'first': {'ru': 'Абв'}, 'middle': {'ru': 'Клмн'}},
            email='katya@ya.ru',
            gender='male',
            org_id=org_id,
        )

        data = self.get_json('/users/?ordering=name')
        result = data.get('result')
        self.assertEqual(len(result), 8)
        self.assertEqual([r['id'] for r in result], [self.admin_uid, 100, 500, 300, 301, 200, 400, 600])

        data = self.get_json('/users/?ordering=-name')
        result = data.get('result')
        self.assertEqual(len(result), 8)
        self.assertEqual([r['id'] for r in result], [self.admin_uid, 100, 500, 300, 301, 200, 400, 600][::-1])

    def test_ordering_by_login(self):
        # В случае одинаковых имени и фамилии сортировка должна выполняться по nicname/login
        # В прямом и обратном порядке, в случае если было передано -name

        org_id = self.organization['id']
        user_model = UserModel(self.main_connection)
        user_model.create(
            id=1002,
            nickname='no_name_2',
            name={'last': {'ru': ''}, 'first': {'ru': ''}, 'middle': {'ru': ''}},
            email='vasya1@ya.ru',
            gender='male',
            org_id=org_id,
        )
        user_model.create(
            id=1003,
            nickname='unnamed',
            name={'last': {'ru': ''}, 'first': {'ru': ''}, 'middle': {'ru': ''}},
            email='vasya2@ya.ru',
            gender='male',
            org_id=org_id,
        )
        user_model.create(
            id=1004,
            nickname='no_name',
            name={'last': {'ru': ''}, 'first': {'ru': ''}, 'middle': {'ru': ''}},
            email='vasya3@ya.ru',
            gender='male',
            org_id=org_id,
        )
        user_model.create(
            id=1005,
            nickname='o.bender',
            name={'last': {'ru': 'Бендер'}, 'first': {'ru': 'Остап'}, 'middle': {'ru': ''}},
            email='vasya3@ya.ru',
            gender='male',
            org_id=org_id,
        )
        user_model.create(
            id=1006,
            nickname='ivanov',
            name={'last': {'ru': 'Иванов'}, 'first': {'ru': 'Петр'}, 'middle': {'ru': ''}},
            email='vasya3@ya.ru',
            gender='male',
            org_id=org_id,
        )
        user_model.create(
            id=1007,
            nickname='ivanov_2',
            name={'last': {'ru': 'Иванов'}, 'first': {'ru': 'Петр'}, 'middle': {'ru': ''}},
            email='vasya3@ya.ru',
            gender='male',
            org_id=org_id,
        )
        user_model.create(
            id=1008,
            nickname='p.ivanov',
            name={'last': {'ru': 'Иванов'}, 'first': {'ru': 'Петр'}, 'middle': {'ru': ''}},
            email='vasya3@ya.ru',
            gender='male',
            org_id=org_id,
        )
        user_model.create(
            id=1009,
            nickname='i.petrov',
            name={'last': {'ru': 'Петров'}, 'first': {'ru': 'Иван'}, 'middle': {'ru': ''}},
            email='vasya3@ya.ru',
            gender='male',
            org_id=org_id,
        )
        user_model.create(
            id=10,
            nickname='no_name_3',
            name={'last': {'ru': ''}, 'first': {'ru': ''}, 'middle': {'ru': ''}},
            email='vasya4@ya.ru',
            gender='male',
            org_id=org_id,
        )
        user_model.create(
            id=11,
            nickname='abbc',
            name={'last': {'ru': 'ddd'}, 'first': {'ru': 'Abc'}, 'middle': {'ru': 'X'}},
            email='vasya@ya.ru',
            gender='male',
            org_id=org_id,
        )
        user_model.create(
            id=5001,
            nickname='aabc',
            name={'last': {'en': 'ddd'}, 'first': {'en': 'Abc'}, 'middle': {'en': 'X'}},
            email='petya@ya.ru',
            gender='male',
            org_id=org_id,
        )
        user_model.create(
            id=3001,
            nickname='aaac',
            name={'last': {'en': 'ddd'}, 'first': {'en': 'Abc'}, 'middle': {'en': 'X'}},
            email='kolya@ya.ru',
            gender='male',
            org_id=org_id,
        )
        user_model.create(
            id=3011,
            nickname='bac',
            name={'last': {'en': 'b'}, 'first': {'en': 'а'}, 'middle': {'en': 'а'}},
            email='kolya1@ya.ru',
            gender='male',
            org_id=org_id,
        )
        user_model.create(
            id=2001,
            nickname='bbb',
            name={'last': {'ru': 'b'}, 'first': {'ru': 'а'}, 'middle': {'ru': 'а'}},
            email='sveta@ya.ru',
            gender='male',
            org_id=org_id,
        )
        user_model.create(
            id=4001,
            nickname='bbc',
            name={'last': {'ru': 'b'}, 'first': {'ru': 'а'}, 'middle': {'ru': 'а'}},
            email='tanya@ya.ru',
            gender='male',
            org_id=org_id,
        )
        user_model.create(
            id=6001,
            nickname='bba',
            name={'last': {'en': 'b'}, 'first': {'en': 'а'}, 'middle': {'en': 'а'}},
            email='katya@ya.ru',
            gender='male',
            org_id=org_id,
        )

        right_order =  [1004, 1002,  10, 1003, self.admin_uid, 3011, 6001, 2001, 4001, 3001, 5001, 11, 1005, 1006, 1007, 1008, 1009]
        data = self.get_json('/users/?ordering=name')
        result = data.get('result')
        self.assertEqual(len(result), 17)
        self.assertEqual([r['id'] for r in result], right_order)

        data = self.get_json('/users/?ordering=-name')
        result = data.get('result')
        self.assertEqual(len(result), 17)
        self.assertEqual([r['id'] for r in result], right_order[::-1])


class TestUserList__get3(PaginationTestsMixin, BaseMixin, TestCase):
    api_version = 'v3'
    entity_list_url = '/v3/users/'
    entity_model = UserModel

    def create_entity(self):
        self.entity_counter += 1
        return self.create_user()

    def prepare_entity_for_api_response(self, entity):
        return {'id': entity['id']}

    def test_filter_by_id(self):
        org_id = self.organization['id']
        OrganizationModel(self.main_connection).update(
            filter_data={'id': self.organization['id']},
            update_data={'last_passport_sync': utcnow()}
        )
        admin_group = GroupModel(self.main_connection).get_or_create_admin_group(org_id)

        user1 = self.create_user(
            department_id=self.department['id'],
            nickname='user1',
            groups=[self.groups['managers']['id']],
        )
        user2 = self.create_user(
            department_id=self.department['id'],
            nickname='user2',
            groups=[self.groups['admins']['id']],
        )

        response_data = self.get_json('/users/?id__gt={}&id__lt={}'.format(user1['id']-1, user1['id']+1))['result']
        assert_that(
            response_data,
            contains_inanyorder(
                {'id': user1['id']},
            )
        )

    def test_fields_support(self):
        # Проверяем, что по дефолту отдаются только id,
        # но можно указать дополнительные поля в fields
        org_id = self.organization['id']
        OrganizationModel(self.main_connection).update(
            filter_data={'id': self.organization['id']},
            update_data={'last_passport_sync': utcnow()}
        )
        admin_group = GroupModel(self.main_connection).get_or_create_admin_group(org_id)

        user1 = self.create_user(
            department_id=self.department['id'],
            nickname='user1',
            groups=[self.groups['managers']['id']],
        )
        user2 = self.create_user(
            department_id=self.department['id'],
            nickname='user2',
            groups=[self.groups['admins']['id']],
        )

        # Проверим, что по-умолчанию отдаются только id
        with catched_sql_queries() as sql:
            response_data = self.get_json('/users/')['result']
            assert_that(
                response_data,
                contains_inanyorder(
                    {'id': self.user['id']},
                    {'id': user1['id']},
                    {'id': user2['id']},
                )
            )
            # Запомним, сколько было SQL запросов в этом базовом варианте
            # base_sql_count включает в себя все запросы, которые делаются
            # в рамках обработки HTTP запроса, включая селекты из миддльварей.

            # Тут же, имеется в виду, что сама вьюшка для вынимания данных,
            # использует только один запрос. На самом деле может и нет,
            # но важно то, что взяв ручку с одним полем id, мы используем
            # base_sql_count, как базовую точку отсчёта, чтобы проверить,
            # что указание дополнительных полей не повлечёт за собой
            # непредусмотренного числа дополнительных запросов в базу.
            base_sql_count = len(sql)

        # Проверим, что отдаются дополнительные поля
        with catched_sql_queries() as sql:
            response_data = self.get_json('/users/?fields=org_id,nickname')['result']
            # Список полей для проверки. id тут добавляется автоматически,
            # так как он должен присутствовать везде.
            fields = ['id', 'nickname', 'org_id']
            assert_that(
                response_data,
                contains_inanyorder(
                    only_fields(self.user, *fields),
                    only_fields(user1, *fields),
                    only_fields(user2, *fields),
                )
            )
            # И всё это вынимается за один запрос.
            assert_that(sql, has_length(base_sql_count))

        # Проверим, что если попросили, то мы раскроем поле department
        with catched_sql_queries() as sql:
            response_data = self.get_json('/users/?fields=department')['result']
            fields = ['id', 'department']

            # Ручка должна отдать для отдела только поле id
            def user_matcher(user, department_id=self.department['id']):
                return has_entries(
                    id=user['id'],
                    department=has_only_entries(id=department_id)
                )

            assert_that(
                response_data,
                contains_inanyorder(
                    user_matcher(self.user, department_id=1),
                    user_matcher(user1),
                    user_matcher(user2),
                )
            )
            # и это не потребует дополнительного запроса,
            # так как данные про отделы подтянутся обычным джойном
            assert_that(sql, has_length(base_sql_count))

        # Проверим, что у поля department можно запросить вложенные поля
        with catched_sql_queries() as sql:
            response_data = self.get_json('/users/?fields=department.label,department.name')['result']

            # Ручка должна отдать для отдела только поля id, label и name
            def user_matcher(user,
                             department_id=self.department['id'],
                             department_name={'en': 'Marketing'}):
                return has_entries(
                    id=user['id'],
                    department=has_only_entries(
                        id=department_id,
                        label=None,
                        name=department_name,
                    )
                )

            assert_that(
                response_data,
                contains_inanyorder(
                    user_matcher(
                        self.user,
                        department_id=1,
                        department_name={
                            'en': 'All employees',
                            'ru': 'Все сотрудники'
                        }
                    ),
                    user_matcher(user1),
                    user_matcher(user2),
                )
            )
            # и это не потребует дополнительного запроса,
            # так как данные про отделы подтянутся обычным джойном
            assert_that(sql, has_length(base_sql_count))

        # А ещё, у поля department должно быть можно поставить * чтобы
        # получить все простые поля
        with catched_sql_queries() as sql:
            response_data = self.get_json(
                '/users/?fields=department.*,department.email&id={0}'.format(user1['id'])
            )['result']

            assert_that(
                response_data[0],
                has_entries(
                    id=user1['id'],
                    department=has_only_entries(
                        id=self.department['id'],
                        label=None,
                        name={
                            'en': 'Marketing',
                        },
                        description={'ru': ''},
                        parent_id=None,
                        email=None,
                        removed=False,
                        members_count=2,
                        external_id=None,
                        created=not_none(),
                        maillist_type='inbox',
                        uid=None,
                    )
                )
            )
            # и это не потребует дополнительного запроса,
            # так как данные про отделы подтянутся обычным джойном
            assert_that(sql, has_length(base_sql_count))

        # Проверим, что если попросили, то мы раскроем поле groups
        with catched_sql_queries() as sql:
            response_data = self.get_json('/users/?fields=groups')['result']
            fields = ['id', 'groups']

            # Ручка должна отдать список команд пользователя
            # и про каждую только её id
            def user_matcher2(user, group=None):
                return has_entries(
                    id=user['id'],
                    groups=[{'id': group['id']}] if group else [],
                )

            assert_that(
                response_data,
                contains_inanyorder(
                    user_matcher2(self.user, group=admin_group),
                    user_matcher2(user1, group=self.groups['managers']),
                    user_matcher2(user2, group=self.groups['admins']),
                )
            )
            # а вот для запроса команд потребуется дополнительный запрос в базу
            assert_that(sql, has_length(base_sql_count + 1))

    def test_fields_are_validated_against_model_fields(self):
        # Проверим, что в фильтры нельзя передать что попало.
        # а то таким образом можно SQL иньекцию провернуть:
        # https://st.yandex-team.ru/DIR-3062

        # Если запрошенное поле не соответствует ни одному из поддерживаемых,
        # то в результате должна быть ошибка со списком поддерживаемых полей.

        response = self.get_json('/users/?fields=some-field', expected_code=422)

        # Ручка должна отдать для отдела только поле id,
        def user_matcher(user, department_id=self.department['id']):
            return has_entries(
                id=user['id'],
                department=has_only_entries(id=department_id)
            )

        assert_that(
            response,
            has_entries(
                message='Unknown field {field}. Supported fields are: {supported_fields}.',
                code='unknown_field',
                params=has_entries(
                    field='some-field',
                    supported_fields=starts_with('about, aliases, avatar_id, birthday'),
                )
            )
        )

    def test_all_fields_separately(self):
        # В этом тесте мы запрашиваем каждое поле по отдельности.
        fields = list(UserModel.all_fields)

        # Эти поля не возвращают ничего, даже если запросить напрямую через модель UserModel,
        # поэтому мы не будем проверять их.
        for field in PRIVATE_USER_FIELDS:
            fields.remove(field)
        fields.remove('is_enabled')
        fields.remove('service_slug')

        for field in fields:
            response = self.get_json('/users/?fields=%s' % field, expected_code=200)
            assert_that(
                response['result'][0],
                has_entries(
                    **{
                        'id': anything(),
                        field: anything(),
                    }
                )
            )

    def test_if_etag_given_return_304(self):
        # Проверим, что ручка возвращает etag,
        # а если его передать в заголовке If-None-Modified,
        # то ручка вернет пустой ответ и 304 код
        response, headers = self.get_json('/users/', return_headers=True)

        # получим свежие данные про ревизию организации
        org = OrganizationModel(self.main_connection).find(
            {'id': self.organization['id']},
            fields=['revision'],
            one=True,
        )
        assert_that(
            headers,
            has_entries(
                # По стандарту, etag должен быть обрамлён кавычками.
                etag='"{0}"'.format(org['revision']),
            )
        )
        etag = headers['etag']

        # Теперь сделаем такой же запрос, но указав в заголовке etag
        response = self.get_json(
            '/users/',
            add_headers={'if-none-match': etag},
            raw=True,
            expected_code=304,
        )
        assert_that(
            response,
            empty(),
        )

    def test_get_with_deleted_groups(self):
        # DIR-7071
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            MAILLIST_SERVICE_SLUG,
        )

        user = self.create_entity()
        group_model = GroupModel(self.main_connection)
        group1 = group_model.create(
            name='test',
            org_id=self.organization['id'],
            members=[{
                'type': 'user',
                'id': user['id']
            }]
        )
        group2 = group_model.create(
            name='test',
            org_id=self.organization['id'],
            members=[{
                'type': 'user',
                'id': user['id']
            }]
        )
        group_model.delete(filter_data={
            'id': group1['id'],
            'org_id': self.organization['id'],
        })

        result = self.get_json(
            '/users/?fields=groups&id={}'.format(user['id']),
        )['result']
        assert_that(
            result,
            contains(
                has_entries(
                    groups=contains(
                        has_entries(
                            id=group2['id']
                        )
                    )
                )
            )
        )


class TestUserList__get_with_resource(BaseMixin, TestCase):
    api_version = 'v9'
    entity_list_url = '/v9/users/'

    def setUp(self):
        super(TestUserList__get_with_resource, self).setUp()
        self.user1 = self.create_user(
            department_id=self.department['id'],
            nickname='user1',
            groups=[self.groups['admins']['id']],
        )
        # два ресурса с id '2a', потому что это краевой случай -
        # мы не контролируем ID, пришедшие к нам из сервисов
        ext_id = '2a'
        self.autotest_resource = self._create_resource(service='autotest', external_id=ext_id, uid=self.user['id'])
        self.resource2 = self._create_resource(service='my-fake-service-2', external_id=ext_id, uid=self.user1['id'])

    def _create_resource(self, service, external_id, uid):
        relations = [
            {
                'user_id': uid,
                'name': 'la-la-la',
            }
        ]
        resource = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service=service,
            external_id=external_id,
            relations=relations,
        )
        return ResourceModel(self.main_connection).get(
            id=resource['id'],
            org_id=self.organization['id'],
        )

    def test_resource_adds_default_service(self):
        # фильтрация по resource фильтрует с учетом сервиса, даже если сервис не передан
        # и под условие попадет только тот из 2х ресурсов с id "2a", который
        # принадлежит сервису от которого сделан запрос
        response_data = self.get_json('/users/?resource=2a', expected_code=200)
        assert_that(
            response_data['result'],
            contains(
                {'id': self.user['id']}
            )
        )


class TestUserList__get9(BaseMixin, TestCase):
    api_version = 'v9'
    entity_list_url = '/v9/users/'

    def setUp(self):
        super(TestUserList__get9, self).setUp()
        self.user1 = self.create_user(
            department_id=self.department['id'],
            nickname='user1',
            groups=[self.groups['admins']['id']],
        )
        self.user2 = self.create_user(
            department_id=self.department['id'],
            nickname='user2',
            groups=[self.groups['managers']['id']],
        )
        self.user3 = self.create_user(
            department_id=self.department['id'],
            nickname='user3',
            groups=[self.groups['managers']['id']],
        )
        self.user4 = self.create_user(
            department_id=self.department['id'],
            nickname='user4',
            groups=[self.groups['managers']['id']],
        )

    def _assert_link(self, link, term, value, total, page):
        assert_that(
            link,
            contains_string('{}={}'.format(term, value))
        )
        assert_that(
            link,
            contains_string('total={}'.format(total))
        )
        assert_that(
            link,
            contains_string('page={}'.format(page))
        )

    def _assert_response(self, response_data, total, page):
        assert_that(
            response_data['total'],
            equal_to(total),
        )
        assert_that(
            response_data['page'],
            equal_to(page),
        )

    def test_keyset_pagination_next(self):
        response_data = self.get_json('/users/?per_page=2', expected_code=200)
        assert_that(
            response_data['result'],
            contains(
                {'id': self.user1['id']},
                {'id': self.user2['id']},
            )
        )
        self._assert_link(
            link=response_data['links']['next'],
            term='id__gt',
            value=self.user2['id'],
            total=5,
            page=2,
        )
        self._assert_response(
            response_data,
            total=5,
            page=1,
        )
        assert_that(
            response_data['links'],
            not_(has_key('prev')),
        )
        next_link = response_data['links']['next']
        next_link = next_link[next_link.find('/users/'):]
        response_data = self.get_json(next_link, expected_code=200)
        assert_that(
            response_data['result'],
            contains(
                {'id': self.user3['id']},
                {'id': self.user4['id']},
            )
        )
        self._assert_link(
            link=response_data['links']['next'],
            term='id__gt',
            value=self.user4['id'],
            total=5,
            page=3,
        )
        self._assert_link(
            link=response_data['links']['prev'],
            term='id__lt',
            value=self.user3['id'],
            total=5,
            page=1,
        )
        self._assert_response(
            response_data,
            total=5,
            page=2,
        )
        next_link = response_data['links']['next']
        next_link = next_link[next_link.find('/users/'):]
        response_data = self.get_json(next_link, expected_code=200)
        assert_that(
            response_data['result'],
            contains(
                {'id': self.user['id']},
            )
        )
        assert_that(
            response_data['links'],
            not_(has_key('next')),
        )
        self._assert_link(
            link=response_data['links']['prev'],
            term='id__lt',
            value=self.user['id'],
            total=5,
            page=2,
        )
        self._assert_response(
            response_data,
            total=5,
            page=3,
        )

    def test_keyset_pagination_prev(self):
        response_data = self.get_json('/users/?per_page=2&reverse=1', expected_code=200)
        assert_that(
            response_data['result'],
            contains(
                {'id': self.user4['id']},
                {'id': self.user['id']},
            )
        )
        self._assert_link(
            link=response_data['links']['prev'],
            term='id__lt',
            value=self.user4['id'],
            total=5,
            page=2,
        )
        self._assert_response(
            response_data,
            total=5,
            page=3,
        )
        assert_that(
            response_data['links'],
            not_(has_key('next')),
        )
        prev_link = response_data['links']['prev']
        prev_link = prev_link[prev_link.find('/users/'):]
        response_data = self.get_json(prev_link, expected_code=200)
        assert_that(
            response_data['result'],
            contains(
                {'id': self.user2['id']},
                {'id': self.user3['id']},
            )
        )
        self._assert_link(
            link=response_data['links']['next'],
            term='id__gt',
            value=self.user3['id'],
            total=5,
            page=3,
        )
        self._assert_link(
            link=response_data['links']['prev'],
            term='id__lt',
            value=self.user2['id'],
            total=5,
            page=1,
        )
        self._assert_response(
            response_data,
            total=5,
            page=2,
        )
        prev_link = response_data['links']['prev']
        prev_link = prev_link[prev_link.find('/users/'):]
        response_data = self.get_json(prev_link, expected_code=200)
        assert_that(
            response_data['result'],
            contains(
                {'id': self.user1['id']},
            )
        )
        self._assert_link(
            link=response_data['links']['next'],
            term='id__gt',
            value=self.user1['id'],
            total=5,
            page=2,
        )
        self._assert_response(
            response_data,
            total=5,
            page=1,
        )
        assert_that(
            response_data['links'],
            not_(has_key('prev')),
        )

    def test_get_with_avatar_id(self):
        with mocked_blackbox() as blackbox:
            userinfo_return_value = [
                {
                    'attributes': {},
                    'fields': collections.defaultdict(dict),
                    'uid': str(self.user['id']),
                    'default_email': None,
                    'display_name': {
                        'avatar': {
                            'default': '12345',
                            'empty': False,
                        }
                    },
                    'karma': 123,
                },
            ]
            blackbox.batch_userinfo.return_value = userinfo_return_value

            response_data = self.get_json('/users/?fields=avatar_id,org_id', expected_code=200)
        assert_that(
            response_data['result'],
            contains_inanyorder(
                {'avatar_id': '12345', 'id': self.user['id'], 'org_id': self.organization['id']},
                {'avatar_id': None, 'id': self.user1['id'], 'org_id': self.organization['id']},
                {'avatar_id': None, 'id': self.user2['id'], 'org_id': self.organization['id']},
                {'avatar_id': None, 'id': self.user3['id'], 'org_id': self.organization['id']},
                {'avatar_id': None, 'id': self.user4['id'], 'org_id': self.organization['id']}
            )
        )

    def test_keyset_pagination_default_limit(self):
        response_data = self.get_json('/users/', expected_code=200)
        assert_that(
            response_data['result'],
            contains(
                {'id': self.user1['id']},
                {'id': self.user2['id']},
                {'id': self.user3['id']},
                {'id': self.user4['id']},
                {'id': self.user['id']},
            )
        )
        assert_that(
            response_data['links'],
            not_(has_key('next')),
        )
        assert_that(
            response_data['links'],
            not_(has_key('prev')),
        )
        assert_that(
            response_data['total'],
            equal_to(5),
        )
        assert_that(
            response_data['page'],
            equal_to(1),
        )


class TestUserList__get11(BaseMixin, TestCase):
    api_version = 'v11'
    entity_list_url = '/{}/users/'.format(api_version)

    def setUp(self):
        super(TestUserList__get11, self).setUp()
        self.user1 = self.create_user(
            department_id=self.department['id'],
            nickname='user1',
            groups=[self.groups['admins']['id']],
        )
        self.user2 = self.create_user(
            department_id=self.department['id'],
            nickname='user2',
            groups=[self.groups['managers']['id']],
        )
        self.robot1 = UserModel(self.main_connection).create(
            id=self.user2['id'] + 1,
            nickname='znick',
            name={
                'first': {
                    'ru': 'Робот',
                    'en': ''
                },
                'last': {
                    'ru': 'Первый',
                    'en': ''
                },
            },
            email='email',
            gender='male',
            org_id=self.organization['id'],
            user_type='robot',
        )
        self.robot2 = UserModel(self.main_connection).create(
            id=self.user2['id'] + 2,
            nickname='znick',
            name={
                'first': {
                    'ru': 'Робот',
                    'en': ''
                },
                'last': {
                    'ru': 'Второй',
                    'en': ''
                },
            },
            email='email',
            gender='male',
            org_id=self.organization['id'],
            user_type='robot',
        )

    def test_get_filter_by_is_not_robot(self):
        response = self.get_json(
            '/users/',
            query_string={
                'is_robot': '0',
                'fields': 'is_robot',

            },
        )
        assert_that(
            response,
            has_entries(
                result=[
                    {
                        'id': self.user1['id'],
                        'is_robot': False,
                    },
                    {
                        'id': self.user2['id'],
                        'is_robot': False,
                    },
                    {
                        'id': self.user['id'],
                        'is_robot': False,
                    },
                ]
            )
        )

    def test_get_filter_by_is_robot_not_valide_value(self):
        response = self.get_json(
            '/users/',
            query_string={
                'is_robot': 'dnbcdjbc sd',
                'fields': 'is_robot',
            },
            expected_code=422,
        )
        assert_that(
            response,
            has_entries(
                code='validation_query_parameters_error',
                message='Query parameters "{query_params}" are not valid',
                params={'query_params': 'is_robot'},
            )
        )

    def test_get_filter_by_is_robot(self):
        response = self.get_json(
            '/users/',
            query_string={
                'is_robot': 1,
                'fields': 'is_robot',
            },
        )
        assert_that(
            response,
            has_entries(
                result=[
                    {
                        'id': self.robot1['id'],
                        'is_robot': True,
                    },
                    {
                        'id': self.robot2['id'],
                        'is_robot': True,
                    },
                ]
            )
        )

    def test_get_filter_by_user_type(self):
        response = self.get_json(
            '/users/',
            query_string={
                'user_type': 'robot',
                'fields': 'user_type',
            },
        )
        assert_that(
            response,
            has_entries(
                result=[
                    {
                        'id': self.robot1['id'],
                        'user_type': 'robot',
                    },
                    {
                        'id': self.robot2['id'],
                        'user_type': 'robot',
                    },
                ]
            )
        )


class BaseUserDetailMixin__get(object):
    api_version = None

    def setUp(self):
        super(BaseUserDetailMixin__get, self).setUp()

        another_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']

        self.another_user = self.create_user(
            uid=2,
            nickname='another-web-chib',
            name=self.name,
            email='another-web-chib@ya.ru',
            org_id=another_organization['id']
        )
        self.user = self.create_user()

    def test_internal_service_can_get_info_about_user(self):
        headers = get_auth_headers(as_org=self.organization)
        response = self.get_json('/users/%s/' % self.user['id'], headers=headers)
        assert response['id'] == self.user['id']

    def test_existing_user__but_it_from_another_organization(self):
        # на существующего в другой организации пользователя ручка должна отдавать 404
        response = self.get_json('/users/%s/' % self.another_user['id'], expected_code=404)
        assert_that(
            response,
            has_entries(
                code='not_found',
                message='Not found',
            )
        )

    def test_not_existing_user(self):
        # на несуществующего пользователя ручка должна отдавать 404
        response = self.get_json('/users/100/', expected_code=404)
        assert_that(
            response,
            has_entries(
                code='not_found',
                message='Not found',
            )
        )


class TestUserDetail__get(BaseUserDetailMixin__get, BaseMixin, TestCase):
    api_version = 'v1'

    def test_existing_user(self):
        response = self.get_json('/users/%s/' % self.user['id'])
        expected = prepare_entity(
            self.main_connection,
            UserModel(self.main_connection).get(
                user_id=self.user['id'],
                org_id=self.organization['id'],
                fields=[
                    '*',
                    'department.*',
                    'groups.*',
                ],
            ),
        )
        assert_that(response, expected)
        # Проверяем, что у обычного пользователя нет полей про роботность и slug-сервиса
        assert_that(
            response,
            is_not(has_key('is_robot')),
            is_not(has_key('service_slug')),
        )
        errors = validate_data_by_schema(response, schema=USERS_OUT_SCHEMA)
        assert_that(errors, equal_to([]))

    def test_existing_user_admin(self):
        user_model = UserModel(self.main_connection)
        user_model.make_admin_of_organization(self.organization['id'], self.user['id'])
        response = self.get_json('/users/%s/' % self.user['id'])
        expected = prepare_entity(
            self.main_connection,
            user_model.get(
                user_id=self.user['id'],
                org_id=self.organization['id'],
                fields=[
                    '*',
                    'department.*',
                    'groups.*',
                ],
            )
        )
        # т.к. это поле не из модели
        expected['is_admin'] = True
        assert_that(response, expected)
        errors = validate_data_by_schema(response, schema=USERS_OUT_SCHEMA)
        assert_that(errors, equal_to([]))

    def test_is_enabled_status(self):
        for status in (0, 1):
            with mocked_blackbox() as blackbox:
                userinfo_return_value = {
                    'attributes': {'1009': str(status)},
                    'fields': collections.defaultdict(dict),
                    'uid': str(self.user['id']),
                    'default_email': None,
                    'karma': 123,
                }
                blackbox.userinfo.return_value = userinfo_return_value
                blackbox.batch_userinfo.return_value = [userinfo_return_value]

                is_enabled = UserModel(self.main_connection).is_enabled(
                    uid=self.user['id'],
                    user_ip=get_localhost_ip_address()
                )
                self.assertEqual(is_enabled, bool(status))
                response = self.get_json('/users/%s/' % self.user['id'])
                self.assertEqual(response['is_enabled'], bool(status))

    def test_contacts_not_null(self):
        self.patch_json('/users/%s/' % self.user['id'], data={
            'contacts': None
        }, expected_code=200)
        response = self.get_json('/users/%s/' % self.user['id'])
        staff_link = url_join(app.config['STAFF_URL'], '{nickname}?org_id={org_id}&uid={uid}').format(
            tld=self.tld,
            nickname=self.user['nickname'],
            org_id=self.user['org_id'],
            uid=self.user['id'],
        )
        expect_contacts = [{
            'alias': False,
            'main': False,
            'synthetic': True,
            'type': 'staff',
            'value': staff_link,
        }, {
            "main": True,
            "alias": False,
            "synthetic": True,
            "type": "email",
            "value": build_email(
                self.main_connection,
                self.user['nickname'],
                self.user['org_id'],
            )
        }]
        self.assertEqual(response['contacts'], expect_contacts)

    def test_user_changed_data(self):
        user = self.create_user(uid=12345, outer_admin=False)
        self.api_version = 'v11'
        with mocked_blackbox() as mock_blackbox:
            fields = collections.defaultdict(dict)
            fields['first_name'] = 'some new name'
            fields['sex'] = '1'
            fields['birth_date'] = '2020-01-01'
            fields['last_name'] = 'last name'
            fields['login'] = user['nickname']
            userinfo_return_value = {
                'fields': fields,
                'uid': str(user['id']),
                'default_email': user['nickname'],
                'karma': 123,
            }
            mock_blackbox.userinfo.return_value = userinfo_return_value
            mock_blackbox.batch_userinfo.return_value = [userinfo_return_value]
            response_data = self.get_json('/users/?fields=name,id,email')
            user_data = [obj for obj in response_data['result'] if obj['id'] == user['id']][0]
            assert user_data['name']['first'] == 'some new name'
            assert OrganizationModel(self.main_connection).get(self.organization['id'])['last_passport_sync'] is not None
            self.process_tasks()
            new_user = UserModel(self.main_connection).get(user['id'])
            assert new_user['name']['first'] == 'some new name'

    def test_javascript_is_not_allowed_in_site_contact(self):
        # Проверяем, что нельзя вставить javascript:alert('blah') в контакт типа site
        # https://st.yandex-team.ru/DIR-3845
        response = self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'contacts': [
                    {
                        'type': 'site',
                        'value': 'javascript:alert("blah")',
                    }
                ]
            },
            expected_code=422
        )
        assert_that(
            response,
            has_entries(
                code='constraint_validation.invalid_value',
            )
        )

    def test_exist_robot_and_slug_fields_for_robots(self):
        # Проверяем, что отдаются поля is_robot & slug_service у роботных аккаунтов
        slug = 'slug_autotest'
        service_name = 'Service Autotest Name'
        robot = create_robot_for_anothertest(self.meta_connection, self.main_connection,
                                             self.organization['id'], slug, self.post_json, name=service_name)
        robot_uid = robot['id']
        response_data = self.get_json('/users/%s/' % robot_uid)
        staff_link = url_join(app.config['STAFF_URL'], '{nickname}?org_id={org_id}&uid={uid}').format(
            tld=self.tld,
            nickname='robot-%s' % slug,
            org_id=self.user['org_id'],
            uid=robot['id'],
        )
        expect_contact = [{
            'alias': False,
            'main': False,
            'synthetic': True,
            'type': 'staff',
            'value': staff_link,
        }, {
            'alias': False,
            'main': True,
            'synthetic': True,
            'type': 'email',
            'value': 'robot-%s@%s' % (slug, self.organization_domain),
        }]

        assert_that(
            response_data,
            has_entries(
                id=robot_uid,
                is_robot=True,
                service_slug=slug,
                contacts=expect_contact,
                department_id=None,
                email='robot-%s@%s' % (slug, self.organization_domain),
                groups=contains_inanyorder(
                    has_entries(
                        type='robots'
                    )
                ),
                is_admin=False,
                is_dismissed=False,
                nickname='robot-%s' % slug,
                name={
                    'first': {
                        'en': '%s Robot' % slug.capitalize(),
                        'ru': 'Робот сервиса %s' % slug.capitalize(),
                    },
                    'last': {
                        'en': '',
                        'ru': '',
                    }
                }
            )
        )

    def test_user_with_empty_services(self):
        # Проверяем, что метод /user/{user_id}/?fields=services возвращает пустой список сервисов, если ни один сервис
        # у пользователя не подключен

        user = self.create_user()
        response_data = self.get_json('/users/%s/?fields=services' % user['id'])
        # проверяем, что список сервисов - пустой
        assert_that(
            response_data,
            has_entries(
                id=user['id'],
                nickname=user['nickname'],
                org_id=user['org_id'],
                services=empty()
            )
        )

    def test_user_by_cloud_uid(self):
        user = self.create_user(uid=12345, cloud_uid='some_uid')
        self.api_version = 'v6'
        response_data = self.get_json('/users/cloud/{}/?fields=id,cloud_uid'.format('some_uid'))
        assert response_data['id'] == user['id']

    def test_get_user_from_cloud_uid(self):
        info = self.create_organization(
            label='hello',
            cloud_org_id='org-cloud-uid',
        )
        user = self.create_user(uid=12345, cloud_uid='some_uid', org_id=info['id'])
        self.api_version = 'v6'
        with patch('intranet.yandex_directory.src.yandex_directory.connect_services.cloud.grpc'
                   '.client.GrpcCloudClient.list_organizations') as list_organizations, \
            patch('intranet.yandex_directory.src.yandex_directory.core.cloud.utils.MessageToDict', MockToDict), \
            patch('intranet.yandex_directory.src.yandex_directory.app.cloud_blackbox_instance') \
                as mock_cloud_blackbox_instance:

            list_organizations_response = self.get_dict_object({
                'next_page_token': None,
                'organizations': [
                    {
                        'id': 'org-cloud-uid',
                        'name': 'test',
                        'description': 'smth desc',
                    }
                ]
            })
            list_organizations.return_value = list_organizations_response
            mock_cloud_blackbox_instance.userinfo.return_value = [{
                'uid': 12345,
                'attributes': {'193': 'abad1dea'},
                'claims': {
                    'given_name': 'user given name',
                    'family_name': 'user family name',
                    'preferred_username': 'username',
                    'email': 'username@example.com'
                }
            }]
            auth_headers = get_auth_headers(as_cloud_uid='some_uid', with_uid=False)
            response_data = self.get_json(
                '/users/{}/?fields=id,cloud_uid'.format(user['id']),
                headers=auth_headers,
            )
        assert response_data['id'] == user['id']

    def test_user_with_service_field(self):
        # Проверяем, что метод /user/{user_id}/?fields=services возвращает все включенные
        # сервисы для заданного пользователя

        # создаем 2 включенных сервиса
        service_data1 = {
            'name': 'Service Autotest #1',
            'slug': 'slug_autotest_1',
            'client_id': 'JfhHfkfjfKHfkwofkK',
        }
        ServiceModel(self.meta_connection).create(**service_data1)
        self.post_json('/services/%s/enable/' % service_data1['slug'], data=None)

        service_data2 = {
            'name': 'Service Autotest #2',
            'slug': 'slug_autotest_2',
            'client_id': 'YurutitoYriorotuUIIjJ',
        }
        ServiceModel(self.meta_connection).create(**service_data2)
        self.post_json('/services/%s/enable/' % service_data2['slug'], data=None)

        # и один сервис включим и выключим
        service_data3 = {
            'name': 'Service Autotest #3',
            'slug': 'slug_autotest_3',
            'client_id': 'sdfjlfjJJldjsfkjHHu',
        }
        ServiceModel(self.meta_connection).create(**service_data3)
        self.post_json('/services/%s/enable/' % service_data3['slug'], data=None)
        self.post_json('/services/%s/disable/' % service_data3['slug'], data=None)

        response_data = self.get_json('/users/%s/?fields=services' % self.user['id'])
        # проверяем, что выключенного сервиса нет в выдаче
        assert_that(
            response_data,
            has_entries(
                id=self.user['id'],
                nickname=self.user['nickname'],
                org_id=self.user['org_id'],
                services=contains_inanyorder(
                    has_entries(
                        name=service_data1['name'],
                        slug=service_data1['slug'],
                        ready=True,
                    ),
                    has_entries(
                        name=service_data2['name'],
                        slug=service_data2['slug'],
                        ready=True,
                    )
                )
            )
        )

    def test_ignore_fields(self):
        # ручка игнорирует параметр fields
        # кроме случая fields=services

        response_data = self.get_json('/users/%s/?fields=id' % self.user['id'])

        assert_that(
            list(response_data.keys()),
            has_items(
                *expected_keys
            )
        )

    def test_user_with_licensed_services(self):
        tracker = ServiceModel(self.meta_connection).create(
            client_id='some-client-id1',
            slug='tracker',
            name='tracker',
            robot_required=False,
            trial_period_months=1,
            paid_by_license=True,
            ready_default=True,
        )
        wiki = ServiceModel(self.meta_connection).create(
            client_id='some-client-id2',
            slug='wiki',
            name='wiki',
            robot_required=False,
            paid_by_license=True,
            ready_default=True,
        )
        self.post_json('/services/%s/enable/' % tracker['slug'], data=None)
        self.post_json('/services/%s/enable/' % wiki['slug'], data=None)
        self.put_json('/subscription/services/%s/licenses/' % tracker['slug'],
                      data=[
                          {
                              'type': 'user',
                              'id': self.user['id'],
                          }
                      ],
                      headers=get_auth_headers(as_uid=self.admin_uid),
                      expected_code=200,
                      )
        # запрашиваем сервисы, когда триальный период трекера и вики закончился
        with frozen_time(datetime.timedelta(days=50)):
            response_data = self.get_json('/users/%s/?fields=services,id' % self.user['id'])
            # у user есть только трекер
            assert_that(
                response_data,
                has_entries(
                    id=self.user['id'],
                    nickname=self.user['nickname'],
                    org_id=self.user['org_id'],
                    services=contains(
                        has_entries(
                            name=tracker['name'],
                            slug=tracker['slug'],
                            ready=True,
                        ),
                    )
                )
            )


class TestUserDetail__get_3(BaseUserDetailMixin__get, BaseMixin, TestCase):
    api_version = 'v3'

    def test_existing_user(self):
        # проверяем, что если запрошен пользователь без указания полей, мы вернем только id
        response = self.get_json('/users/%s/' % self.user['id'])
        expected = {
            'id': self.user['id'],
        }
        assert_that(response, equal_to(expected))

    def test_existing_user_admin(self):
        # запрос ручки с ?fields=is_admin должен вернуть id и статус is_admin для пользователя
        user_model = UserModel(self.main_connection)
        user_model.make_admin_of_organization(self.organization['id'], self.user['id'])
        response = self.get_json('/users/%s/?fields=is_admin' % self.user['id'])
        expected = {
            'id': self.user['id'],
            'is_admin': True,
        }
        assert_that(response, equal_to(expected))

    def test_existing_user_get_with_fields(self):
        # проверяем запрос существующего пользователя с полями id,email
        response = self.get_json('/users/%s/?fields=org_id,id,email' % self.user['id'])

        assert_that(
            response,
            has_entries(
                org_id=self.organization['id'],
                id=self.user['id'],
                email=build_email(
                    self.main_connection,
                    self.user['nickname'],
                    self.user['org_id'],
                ),
            )
        )

    def test_expand_user_department(self):
        # Проверим, что можно указать какие поля отдела нужно вернуть
        uid = self.user['id']
        response = self.get_json(
            '/users/%s/?fields=department.label,department.members_count' % uid
        )

        assert_that(
            response,
            has_entries(
                id=uid,
                department=has_only_entries(
                    id=self.user['department_id'],
                    label=None,
                    members_count=2,
                )
            )
        )

    def test_on_trash_in_fields_we_return_error(self):
        # Проверим, что если в полях передаётся какой-то мусор,
        # то мы вернём 422 ошибку
        uid = self.user['id']
        url = '/users/%s/?fields=department.label,department.members_count;select 1;' % uid
        response = self.get_json(url, expected_code=422)

        assert_that(
            response,
            has_entries(
                message='Unknown field {field}. Supported fields are: {supported_fields}.',
                params=has_entries(
                    field='members_count;select 1;',
                    supported_fields=ANY,
                )
            )
        )

    def test_contacts_not_null(self):
        # проверяем выдачу ручки при наличии контактов у пользователя
        self.patch_json('/users/%s/' % self.user['id'], data={
            'contacts': None
        }, expected_code=200)
        response = self.get_json('/users/%s/?fields=contacts' % self.user['id'])
        staff_link = url_join(app.config['STAFF_URL'], '{nickname}?org_id={org_id}&uid={uid}').format(
            tld=self.tld,
            nickname=self.user['nickname'],
            org_id=self.user['org_id'],
            uid=self.user['id'],
        )
        exp_contacts = [{
            'alias': False,
            'main': False,
            'synthetic': True,
            'type': 'staff',
            'value': staff_link
        }, {
            "main": True,
            "alias": False,
            "synthetic": True,
            "type": "email",
            "value": build_email(
                self.main_connection,
                self.user['nickname'],
                self.user['org_id'],
            )
        }]
        assert_that(
            response,
            equal_to({
                'id': self.user['id'],
                'contacts': exp_contacts,
            }),
        )

    def test_exist_robot_and_slug_fields_for_robots(self):
        # Проверяем, что отдаются поля is_robot & slug_service у роботных аккаунтов
        slug = 'slug_autotest'
        service_name = 'Service Autotest Name'
        robot = create_robot_for_anothertest(self.meta_connection, self.main_connection,
                                             self.organization['id'], slug, self.post_json, name=service_name)
        robot_uid = robot['id']
        response_data = self.get_json(
            '/users/%s/?fields=is_robot,service_slug,email,nickname,name,contacts' % robot_uid,
        )
        staff_link = url_join(app.config['STAFF_URL'], '{nickname}?org_id={org_id}&uid={uid}').format(
            tld=self.tld,
            nickname='robot-%s' % slug,
            org_id=self.user['org_id'],
            uid=robot['id'],
        )
        expect_contact = [{
            'alias': False,
            'main': False,
            'synthetic': True,
            'type': 'staff',
            'value': staff_link,
        }, {
            'alias': False,
            'main': True,
            'synthetic': True,
            'type': 'email',
            'value': 'robot-%s@%s' % (slug, self.organization_domain),
        }]
        assert_that(
            response_data,
            has_entries(
                id=robot_uid,
                is_robot=True,
                service_slug=slug,
                email='robot-%s@%s' % (slug, self.organization_domain),
                contacts=expect_contact,
                nickname='robot-%s' % slug,
                name={
                    'first': {
                        'en': '%s Robot' % slug.capitalize(),
                        'ru': 'Робот сервиса %s' % slug.capitalize(),
                    },
                    'last': {
                        'en': '',
                        'ru': '',
                    }
                }
            )
        )

    def test_exist_robot_and_slug_fields_for_non_robots(self):
        # если аккаунт не роботный, а запрошены поля is_robot & slug_service, они должны принимать значения
        # False и None соответственно
        response = self.get_json('/users/%s/?fields=is_robot,service_slug' % self.user['id'])
        assert_that(
            response,
            equal_to({
                'id': self.user['id'],
                'is_robot': False,
                'service_slug': None,
            }),
        )

    def test_existing_user_get_with_services_and_id_only(self):
        # проверяем, что при запросе ?fields=id,services
        # ручка вернет только указанные поля
        # создаем 2 включенных сервиса
        service_data1 = {
            'name': 'Service Autotest #1',
            'slug': 'slug_autotest_1',
            'client_id': 'slug_autotest_service_1',
        }
        ServiceModel(self.meta_connection).create(**service_data1)
        self.post_json('/services/%s/enable/' % service_data1['slug'], data=None)

        service_data2 = {
            'name': 'Service Autotest #2',
            'slug': 'slug_autotest_2',
            'client_id': 'slug_autotest_service_2',
        }
        ServiceModel(self.meta_connection).create(**service_data2)
        self.post_json('/services/%s/enable/' % service_data2['slug'], data=None)

        response = self.get_json('/users/%s/?fields=id,services' % self.user['id'])

        assert_that(
            response,
            has_entries(
                id=self.user['id'],
                services=contains_inanyorder(
                    has_entries(
                        slug=service_data1['slug'],
                        name=service_data1['name'],
                        ready=True,
                    ),
                    has_entries(
                        slug=service_data2['slug'],
                        name=service_data2['name'],
                        ready=True,
                    )
                )
            )
        )

    def test_existing_user_get_with_services_and_paid_disk_info(self):
        # проверяем, что при получении сервисов с services.disk.has_paid_space
        # ручка вернет информацию о платном диске пользователя
        service_data1 = {
            'name': 'Service Autotest #1',
            'slug': 'slug_autotest_1',
            'client_id': 'slug_autotest_service_1',
        }
        ServiceModel(self.meta_connection).create(**service_data1)
        self.post_json('/services/%s/enable/' % service_data1['slug'], data=None)

        service_disk = {
            'name': 'Yandex.Disk',
            'slug': 'disk',
            'client_id': 'disk_service',
        }
        ServiceModel(self.meta_connection).create(**service_disk)
        self.post_json('/services/%s/enable/' % service_disk['slug'], data=None)

        for has_paid_space in (False, True):
            mocked_disk = Mock()
            mocked_disk.is_paid.return_value = has_paid_space

            with patch('intranet.yandex_directory.src.yandex_directory.disk', mocked_disk):
                response = self.get_json(
                    '/users/%s/?fields=id,services,services.disk.has_paid_space' % self.user['id'],
                )

            mocked_disk.is_paid.assert_called_once_with(self.user['id'])

            assert_that(
                response,
                has_entries(
                    id=self.user['id'],
                    services=contains_inanyorder(
                        has_entries(
                            slug=service_data1['slug'],
                            name=service_data1['name'],
                            ready=True,
                        ),
                        has_entries(
                            slug=service_disk['slug'],
                            name=service_disk['name'],
                            has_paid_space=has_paid_space,
                            ready=True,
                        ),
                    )
                )
            )

    def test_existing_user_get_with_services_and_paid_disk_info_with_disk_service_field_only(self):
        # если мы запросили services.disk.has_paid_space без поля services,
        # нужно всё равно вернуть инфомрацию и о сервисах и о платном диске
        service_data1 = {
            'name': 'Service Autotest #1',
            'slug': 'slug_autotest_1',
            'client_id': 'slug_autotest_service_1',
        }
        ServiceModel(self.meta_connection).create(**service_data1)
        self.post_json('/services/%s/enable/' % service_data1['slug'], data=None)

        service_disk = {
            'name': 'Yandex.Disk',
            'slug': 'disk',
            'client_id': 'disk_service',
        }
        ServiceModel(self.meta_connection).create(**service_disk)
        self.post_json('/services/%s/enable/' % service_disk['slug'], data=None)

        for has_paid_space in (False, True):
            mocked_disk = Mock()
            mocked_disk.is_paid.return_value = has_paid_space

            with patch('intranet.yandex_directory.src.yandex_directory.disk', mocked_disk):
                response = self.get_json(
                    '/users/%s/?fields=id,services.disk.has_paid_space' % self.user['id'],
                )

            mocked_disk.is_paid.assert_called_once_with(self.user['id'])

            assert_that(
                response,
                has_entries(
                    id=self.user['id'],
                    services=contains_inanyorder(
                        has_entries(
                            slug=service_data1['slug'],
                            name=service_data1['name'],
                            ready=True,
                        ),
                        has_entries(
                            slug=service_disk['slug'],
                            name=service_disk['name'],
                            has_paid_space=has_paid_space,
                            ready=True,
                        )
                    )
                )
            )

    def test_user_with_service_field(self):
        # Проверяем, что метод /user/{user_id}/?fields=services возвращает все включенные
        # сервисы для заданного пользователя

        # создаем 2 включенных сервиса
        service_data1 = {
            'name': 'Service Autotest #1',
            'slug': 'slug_autotest_1',
            'client_id': 'JfhHfkfjfKHfkwofkK',
        }
        ServiceModel(self.meta_connection).create(**service_data1)
        self.post_json('/services/%s/enable/' % service_data1['slug'], data=None)

        service_data2 = {
            'name': 'Service Autotest #2',
            'slug': 'slug_autotest_2',
            'client_id': 'YurutitoYriorotuUIIjJ',
        }
        ServiceModel(self.meta_connection).create(**service_data2)
        self.post_json('/services/%s/enable/' % service_data2['slug'], data=None)

        # и один сервис включим и выключим
        service_data3 = {
            'name': 'Service Autotest #3',
            'slug': 'slug_autotest_3',
            'client_id': 'sdfjlfjJJldjsfkjHHu',
        }
        ServiceModel(self.meta_connection).create(**service_data3)
        self.post_json('/services/%s/enable/' % service_data3['slug'], data=None)
        self.post_json('/services/%s/disable/' % service_data3['slug'], data=None)

        response_data = self.get_json('/users/%s/?fields=services,id' % self.user['id'])
        # проверяем, что выключенного сервиса нет в выдаче
        assert_that(
            response_data,
            has_entries(
                id=self.user['id'],
                services=contains_inanyorder(
                    has_entries(
                        name=service_data1['name'],
                        slug=service_data1['slug'],
                        ready=True,
                    ),
                    has_entries(
                        name=service_data2['name'],
                        slug=service_data2['slug'],
                        ready=True,
                    )
                )
            )
        )

    def test_user_with_empty_services(self):
        # Проверяем, что метод /user/{user_id}/?fields=services возвращает пустой список сервисов, если ни один сервис
        # у пользователя не подключен
        user = self.create_user()
        response_data = self.get_json('/users/%s/?fields=services' % user['id'])
        # проверяем, что список сервисов - пустой
        assert_that(
            response_data,
            has_entries(
                id=user['id'],
                services=empty(),
            )
        )

    def test_user_with_licensed_services(self):
        tracker = ServiceModel(self.meta_connection).create(
            client_id='some-client-id1',
            slug='tracker',
            name='tracker',
            robot_required=False,
            trial_period_months=1,
            paid_by_license=True,
            ready_default=True,
        )
        wiki = ServiceModel(self.meta_connection).create(
            client_id='some-client-id2',
            slug='wiki',
            name='wiki',
            robot_required=False,
            paid_by_license=True,
            ready_default=True,
        )
        group = self.create_group(members=[{'type': 'user', 'object': self.user}])
        self.post_json('/services/%s/enable/' % tracker['slug'], data=None)
        self.post_json('/services/%s/enable/' % wiki['slug'], data=None)
        person_id = 1
        client_id = 2
        first_name = 'Alexander'
        last_name = 'Akhmetov'
        middle_name = 'R'
        phone = '+7'
        email = 'akhmetov@yandex-team.ru'

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {}
        mocked_xmlrpc.Balance.CreateClient.return_value = (None, None, client_id)
        mocked_xmlrpc.Balance.CreatePerson.return_value = person_id

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            OrganizationModel(self.main_connection).create_contract_info_for_natural_person(
                org_id=self.organization['id'],
                author_id=self.admin_uid,
                first_name=first_name,
                last_name=last_name,
                middle_name=middle_name,
                phone=phone,
                email=email,
            )
        # включим лицензии для группы, в которой находится пользователь
        self.put_json('/subscription/services/%s/licenses/' % tracker['slug'],
                      data=[
                          {
                              'type': 'group',
                              'id': group['id'],
                          }
                      ],
                      headers=get_auth_headers(as_uid=self.admin_uid),
                      expected_code=200,
                      )
        # запрашиваем сервисы, когда триальный период трекера и вики закончился
        with frozen_time(datetime.timedelta(days=50)):
            response_data = self.get_json('/users/%s/?fields=services,id' % self.user['id'])
            # у user есть только трекер
            assert_that(
                response_data,
                has_entries(
                    id=self.user['id'],
                    services=contains(
                        has_entries(
                            slug=tracker['slug'],
                            ready=True,
                        )
                    )
                )
            )

    def test_user_with_licensed_services_no_trial(self):
        # проверяем, что если у сервиса нулевой триальный период,
        # то в день подключения он сразу доступен только по лицензиям

        tracker = ServiceModel(self.meta_connection).create(
            client_id='some-client-id1',
            slug='tracker',
            name='tracker',
            robot_required=False,
            trial_period_months=0,
            paid_by_license=True,
            ready_default=True,
        )

        self.post_json('/services/%s/enable/' % tracker['slug'], data=None)
        # включим лицензии для пользователя
        self.put_json('/subscription/services/%s/licenses/' % tracker['slug'],
                      data=[
                          {
                              'type': 'user',
                              'id': self.user['id'],
                          }
                      ],
                      headers=get_auth_headers(as_uid=self.admin_uid),
                      expected_code=200,
                      )

        response_data = self.get_json('/users/%s/?fields=services,id' % self.user['id'])
        # у user есть трекер
        assert_that(
            response_data,
            has_entries(
                id=self.user['id'],
                services=contains(
                    has_entries(
                        slug=tracker['slug'],
                        ready=True,
                    )
                )
            )
        )

        new_user = self.create_user()
        response_data = self.get_json('/users/%s/?fields=services,id' % new_user['id'])
        # у new_user нет лицензий
        assert_that(
            response_data,
            has_entries(
                id=new_user['id'],
                services=empty()
            )
        )

    def test_is_enabled_status(self):
        # проверяем, что при запросе поля is_enabled вернется правильное значение из похода в blackbox
        for status in (1, 0):
            with mocked_blackbox() as blackbox:
                userinfo_return_value = {
                    'attributes': {'1009': str(status)},
                    'fields': collections.defaultdict(dict),
                    'uid': str(self.user['id']),
                    'default_email': None,
                    'karma': 123,
                }
                blackbox.userinfo.return_value = userinfo_return_value
                blackbox.batch_userinfo.return_value = [userinfo_return_value]

                is_enabled = UserModel(self.main_connection).is_enabled(
                    uid=self.user['id'],
                    user_ip=get_localhost_ip_address()
                )
                self.assertEqual(is_enabled, bool(status))
                response = self.get_json('/users/%s/?fields=is_enabled' % self.user['id'])
                self.assertEqual(response['is_enabled'], bool(status))

    def test_departments_field(self):
        # Можно указать поле departments и поля отдела, которые нужны

        # По-умолчанию, отдаются только id
        response = self.get_json('/users/%s/?fields=departments' % self.user['id'])
        assert_that(
            response,
            has_entries(
                departments=contains(
                    has_only_entries(
                        id=1,
                    )
                )
            )
        )

        # Но можно запросить и имя
        response = self.get_json('/users/%s/?fields=departments.name' % self.user['id'])
        assert_that(
            response,
            has_entries(
                departments=contains(
                    has_only_entries(
                        id=1,
                        name=has_only_entries(
                            ru='Все сотрудники',
                            en='All employees',
                        ),
                    )
                )
            )
        )

    def test_get_language(self):
        data = self.get_json(
            '/users/%s/?fields=language' % self.user['id'],
        )
        assert_that(
            data,
            has_key('language'),
        )

        data = self.get_json(
            '/users/?fields=language',
        )
        assert_that(
            data['result'][0],
            has_key('language'),
        )


@patch.object(UserModel, 'is_enabled', Mock(return_value=True))
class TestUserDetail__patch(BaseMixin, TestCase):
    def setUp(self):
        super(TestUserDetail__patch, self).setUp()
        user_model = UserModel(self.main_connection)
        another_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']

        self.another_sso_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='microsoft',
            is_sso_enabled=True,
            is_provisioning_enabled=True,
        )['organization']

        self.another_sso_user = self.create_user(
            org_id=self.another_sso_organization['id'],
            nickname='another-web-sso-chib',
            name=self.name,
            is_sso=True,
        )

        self.another_sso_portal_admin = self.create_portal_user(
            org_id=self.another_sso_organization['id'],
            login='test-1',
            email='test-1@yandex.ru',
            is_admin=True,
        )

        self.another_sso_admin = self.create_user(
            org_id=self.another_sso_organization['id'],
            nickname='another-web-sso-admin',
            name=self.name,
            is_outer=True,
        )

        self.another_department = create_department(
            self.main_connection,
            org_id=self.organization['id'],
        )

        self.another_user = self.create_user(
            # Этот пользователь создаётся в отдельной организации
            org_id=another_organization['id'],
            nickname='another-web-chib',
            name=self.name,
        )

        self.another_user_from_this_organization = self.create_user(
            nickname='web-chib-another',
            name=self.name,
        )
        self.another_user_from_this_organization['is_admin'] = False

        self.name = {
            'first': {
                'ru': 'Владимир'
            },
            'last': {
                'ru': 'Путин'
            }
        }
        self.position = {'ru': 'New position'}
        self.about = {'ru': 'New about'}
        self.birthday = utcnow().date()
        self.organization_admin_group = create_group(
            self.main_connection,
            org_id=self.organization['id'],
            type='organization_admin'
        )
        self.generic_group = create_group(
            self.main_connection,
            org_id=self.organization['id']
        )

        set_auth_uid(self.user['id'])

        # для тестирования увольнений
        self.another_user_group1 = create_group(
            self.main_connection,
            org_id=self.organization['id']
        )
        self.another_user_group2 = create_group(
            self.main_connection,
            org_id=self.organization['id']
        )

        self.dep1 = create_department(
            self.main_connection,
            parent_id=ROOT_DEPARTMENT_ID,
            org_id=self.organization['id'],
            name={'ru': 'Д1'}
        )
        self.dep11 = create_department(
            self.main_connection,
            org_id=self.organization['id'],
            parent_id=self.dep1['id'],
            name={'ru': 'Д11'}
        )
        self.dep111 = create_department(
            self.main_connection,
            org_id=self.organization['id'],
            parent_id=self.dep11['id'],
            name={'ru': 'Д111'}
        )

        self.dismiss_user = self.create_user(
            nickname='another-web-chib',
            name=self.name,
            department_id=self.dep111['id'],
            groups=[
                self.another_user_group1['id'],
                self.another_user_group2['id'],
            ]
        )
        GroupModel(self.main_connection).update_one(
            org_id=self.organization['id'],
            group_id=self.another_user_group1['id'],
            data={
                'admins': [
                    {'type': TYPE_USER, 'id': self.dismiss_user['id']},
                ],
            }
        )

        self.common_user = self.create_user()
        # Подготовка пользователя должна быть в конце setUp,
        # иначе у вложенного отдела будет неправильное число сотрудников
        user = user_model.get(
            self.user['id'],
            org_id=self.user['org_id'],
            fields=[
                '*',
                'department.*',
                'groups.*',
                'services.*',
            ],
        )
        self.prepared_user = prepare_user(
            self.main_connection,
            user,
            expand_contacts=True,
            api_version=1,
        )
        # Эти поля берутся не из базы, так что замокаем их отдельно
        self.prepared_user['is_admin'] = True
        self.prepared_user['is_enabled'] = True
        self.prepared_user['timezone'] = 'Europe/Moscow'
        self.prepared_user['language'] = 'ru'

    def test_should_return_not_found_if_no_user_exists(self):
        response_data = self.patch_json(
            '/users/1000/',
            data={
                'name': self.name
            },
            expected_code=404
        )
        self.assertEqual(response_data.get('code'), 'not_found')

    def test_existing_user__but_it_from_another_organization(self):
        response = self.patch_json(
            '/users/%s/' % self.another_user['id'],
            data={
                'name': self.name
            },
            expected_code=404
        )
        self.assertEqual(response.get('code'), 'not_found')

    def test_organization_admin_should_be_allowed_to_edit_other_user(self):
        self.patch_json(
            '/users/%s/' % self.another_user_from_this_organization['id'],
            data={
                'name': self.name,
                'contacts': another_contacts,
            },
            expected_code=200
        )

    def test_patch_name_validate(self):
        self.mocked_passport.validate_firstname.side_effect = FirstnameInvalid
        self.patch_json(
            '/users/%s/' % self.another_user_from_this_organization['id'],
            data={
                'name': self.name,
            },
            expected_code=422
        )

    def test_simple_user_should_not_be_allowed_to_edit_his_data_except_contacts(self):
        self.patch_json(
            '/users/%s/' % self.common_user['id'],
            data={
                'contacts': another_contacts,
            },
            headers=get_auth_headers(as_uid=self.common_user['id']),
            expected_code=200
        )

        experiments = [
            {
                'name': self.name,
                'contacts': another_contacts,
            },
            {
                'name': self.name,
            },
        ]
        for experiment_data in experiments:
            self.patch_json(
                '/users/%s/' % self.common_user['id'],
                data=experiment_data,
                headers=get_auth_headers(as_uid=self.common_user['id']),
                expected_code=403,
            )

    def test_simple_user_should_not_be_allowed_to_edit_his_data_except_birthday(self):
        formatted_birthday = format_date(self.birthday)
        self.patch_json(
            '/users/%s/' % self.common_user['id'],
            data={
                'birthday': formatted_birthday,
            },
            headers=get_auth_headers(as_uid=self.common_user['id']),
            expected_code=200,
        )

        experiments = [
            {
                'name': self.name,
                'birthday': formatted_birthday,
            },
            {
                'name': self.name,
            },
        ]
        for experiment_data in experiments:
            self.patch_json(
                '/users/%s/' % self.common_user['id'],
                data=experiment_data,
                headers=get_auth_headers(as_uid=self.common_user['id']),
                expected_code=403,
            )

    def test_simple_user_should_not_be_allowed_to_edit_other_user(self):
        other_user = self.create_user()
        response = self.patch_json(
            '/users/%s/' % other_user['id'],
            data={
                'name': self.name
            },
            headers=get_auth_headers(as_uid=self.common_user['id']),
            expected_code=403
        )
        assert_that(
            response,
            has_entries(
                code='forbidden',
                message='Access denied',
            )
        )

    def test_deputy_admin_can_edit_simple_users(self):
        deputy_admin = self.create_deputy_admin()

        response = self.patch_json(
            '/users/%s/' % self.another_user_from_this_organization['id'],
            data={
                'name': self.name,
                'birthday': '2018-01-01',
                'is_enabled': False,
            },
            headers=get_auth_headers(as_uid=deputy_admin['id']),
            expected_code=200
        )

    def test_deputy_admin_should_not_be_allowed_to_edit_admin(self):
        deputy_admin = self.create_deputy_admin()

        response = self.patch_json(
            '/users/%s/' % self.admin_uid,
            data={
                'name': self.name
            },
            headers=get_auth_headers(as_uid=deputy_admin['id']),
            expected_code=403
        )
        assert_that(
            response,
            has_entries(
                code='forbidden',
                message='Access denied',
            )
        )

    def test_patch_name__with_value(self):
        self.mocked_blackbox.batch_userinfo.return_value = [
            fake_userinfo(
                self.user['id'],
                timezone='Asia/Jakarta',
                language='ru',
            )
        ]
        response_data = self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'name': self.name
            }
        )
        self.prepared_user['name'] = self.name
        self.assertEqual(response_data.get('name'), self.name)
        self.assertEqual(response_data, self.prepared_user)
        new_user = UserModel(self.main_connection).get(self.user['id'], self.organization['id'], fields=['first_name'])
        self.assertEqual(new_user['first_name'], self.name['first']['ru'])

    def test_patch_name__with_null(self):
        self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'name': None
            },
            expected_code=422
        )

    def test_patch_name__with_empty_string(self):
        self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'name': ''
            },
            expected_code=422
        )

    def test_patch_nickname(self):
        self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'nickname': 'new_nick',
            },
            expected_code=422,
        )

    def test_patch_position(self):
        self.mocked_blackbox.batch_userinfo.return_value = [
            fake_userinfo(
                self.user['id'],
                timezone='Asia/Jakarta',
                language='ru',
            )
        ]
        response_data = self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'position': self.position
            }
        )
        self.prepared_user['position'] = self.position
        self.assertEqual(response_data.get('position'), self.position)
        self.assertEqual(response_data, self.prepared_user)

    def test_patch_external_id(self):
        # меняем поле external_id
        external_id = 'external_id'
        self.mocked_blackbox.batch_userinfo.return_value = [
            fake_userinfo(
                self.user['id'],
                timezone='Asia/Jakarta',
                language='ru',
            )
        ]
        response_data = self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'external_id': external_id
            }
        )
        self.prepared_user['external_id'] = external_id
        self.assertEqual(response_data.get('external_id'), external_id)
        self.assertEqual(response_data, self.prepared_user)

    def test_patch_about(self):
        self.mocked_blackbox.batch_userinfo.return_value = [
            fake_userinfo(
                self.user['id'],
                timezone='Asia/Jakarta',
                language='ru',
            )
        ]
        response_data = self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'about': self.about
            }
        )
        self.prepared_user['about'] = self.about
        self.assertEqual(response_data.get('about'), self.about)
        assert_that(
            response_data,
            is_same(self.prepared_user)
        )

    def test_patch_birthday(self):
        formatted_birthday = format_date(self.birthday)

        self.mocked_blackbox.batch_userinfo.return_value = [
            fake_userinfo(
                self.user['id'],
                timezone='Asia/Jakarta',
                language='ru',
            )
        ]
        response_data = self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'birthday': formatted_birthday
            }
        )
        self.prepared_user['birthday'] = formatted_birthday
        self.assertEqual(response_data.get('birthday'), formatted_birthday)
        self.assertEqual(response_data, self.prepared_user)

    def test_path_incorrect_birthday(self):
        self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'birthday': '4000-10-10'
            },
            expected_code=422
        )

    def test_nullable_birthday(self):
        # Так как с некоторых пор (DIR-3071) день рождения стал опциональным,
        # то надо чтобы была возможность сбрасывать его путём патча.

        response_data = self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'birthday': None,
            }
        )
        assert_that(
            response_data,
            has_entries(
                birthday=None,
            )
        )

    def test_patch_recovery_email(self):
        recovery_email = 'sos@email.com'

        self.mocked_blackbox.batch_userinfo.return_value = [
            fake_userinfo(
                self.user['id'],
                timezone='Asia/Jakarta',
                language='ru',
            )
        ]
        response_data = self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'recovery_email': recovery_email
            }
        )
        new_user = UserModel(self.main_connection).get(self.user['id'])
        self.assertEqual(new_user['recovery_email'], recovery_email)
        self.assertEqual(response_data, self.prepared_user)

    def test_nullable_gender(self):
        # Так как с некоторых пор (DIR-3071) пол стал опциональным,
        # то надо чтобы была возможность сбрасывать его путём патча.

        response_data = self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'gender': None,
            }
        )
        assert_that(
            response_data,
            has_entries(
                gender=None,
            )
        )

    def test_patch_contacts(self):
        self.mocked_blackbox.batch_userinfo.return_value = [
            fake_userinfo(
                self.user['id'],
                timezone='Asia/Jakarta',
                language='ru',
            )
        ]
        response_data = self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'contacts': another_contacts
            }
        )

        expected_contacts = []

        for contact in another_contacts:
            expected_contacts.append(
                has_entries(
                    **contact.copy(
                        main=False,
                        alias=False,
                        synthetic=False,
                    )
                )
            )
        staff_link = url_join(app.config['STAFF_URL'], '{nickname}?org_id={org_id}&uid={uid}').format(
            tld=self.tld,
            nickname=self.user['nickname'],
            org_id=self.user['org_id'],
            uid=self.user['id'],
        )
        expected_contacts.append(
            has_entries(
                type='staff',
                value=staff_link,
                synthetic=True,
                alias=False,
                main=False,
            )
        )
        expected_contacts.append(
            has_entries(
                main=True,
                alias=False,
                synthetic=True,
                type='email',
                value=build_email(
                    self.main_connection,
                    self.user['nickname'],
                    self.user['org_id'],
                )
            )
        )

        user = self.prepared_user.copy()
        user['contacts'] = expected_contacts
        matcher = is_same(user)
        assert_that(
            response_data,
            matcher,
        )

    def test_patch_phones(self):
        phone_contacts = [
            {
                "type": "phone",
                "value": "+79250747348",
                "main": True,
            },
            {
                "type": "phone",
                "value": "+79250740011",
                "main": True,
            },
            {
                "type": "phone_extension",
                "value": "7369",
                "main": True,
            },
        ]
        self.mocked_blackbox.batch_userinfo.return_value = [
            fake_userinfo(
                self.user['id'],
                timezone='Asia/Jakarta',
                language='ru',
            )
        ]
        response_data = self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'contacts': phone_contacts
            }
        )

        for contact in phone_contacts:
            contact['main'] = bool(contact.get('main'))
            contact['alias'] = False
            contact['synthetic'] = False
        staff_link = url_join(app.config['STAFF_URL'], '{nickname}?org_id={org_id}&uid={uid}').format(
            tld=self.tld,
            nickname=self.user['nickname'],
            org_id=self.user['org_id'],
            uid=self.user['id'],
        )
        expand_contacts = phone_contacts + [{
            "main": False,
            "alias": False,
            "synthetic": True,
            "type": "staff",
            "value": staff_link
        }, {
            "main": True,
            "alias": False,
            "synthetic": True,
            "type": "email",
            "value": build_email(
                self.main_connection,
                self.user['nickname'],
                self.user['org_id'],
            )
        }]

        self.prepared_user['contacts'] = expand_contacts
        self.prepared_user['is_enabled'] = True
        self.assertEqual(response_data.get('contacts'), expand_contacts)
        self.assertEqual(response_data, self.prepared_user)

    def test_patch_department_id(self):
        # Проверка ручки patch.
        # Проверяем, что
        #   - вернулся код 200
        #   - отдается правильный id департамента
        #   - при перемещении пользователя обновляется кеш групп,
        #   связанных с департаментов
        department1 = self.create_department()
        department2 = self.create_department(parent_id=department1['id'])
        user = self.create_user(department_id=department2['id'])
        group1 = self.create_group(
            members=[{
                'type': 'department',
                'id': user['department_id'],
            }]
        )
        group2 = self.create_group(
            members=[{
                'type': 'department',
                'id': self.another_department['id'],
            }]
        )
        response_data = self.patch_json(
            '/users/%s/' % user['id'],
            data={
                'department_id': self.another_department['id']
            },
            expected_code=200
        )
        self.assertEqual(response_data['department'].get('id'),
                         self.another_department['id'])
        users_group1 = UserGroupMembership(self.main_connection).find({'group_id': group1['id']})
        assert_that(users_group1, empty())
        users_group2 = UserGroupMembership(self.main_connection).find({'group_id': group2['id']})
        user_ids = [x['user_id'] for x in users_group2]
        assert_that(user_ids, contains(user['id']))

        # Проверим, что событие попала информация про изменения отдела
        event = EventModel(self.main_connection) \
                .filter(
                    org_id=self.organization['id'],
                    name='user_property_changed',
                ) \
                .order_by('-id') \
                .one()

        assert_that(
            event['content']['diff']['departments'],
            contains(
                contains(
                    has_entries(id=1),
                    has_entries(id=department1['id']),
                    has_entries(id=department2['id']),
                ),
                # Цепочка отделов изменилась
                contains(
                    has_entries(id=self.another_department['id'])
                ),
            )
        )

        # NOTE: это поле пока в diff не попадает.
        #       похоже, что это ошибка, но пока просто зафиксируем
        #       это в тесте.
        assert 'department' not in event['content']['diff']

    def test_patch_department_as_object(self):
        # Проверка ручки patch.
        # Проверяем, что
        #   - вернулся код 200
        #   - отдается правильный id департамента
        #   - при перемещении пользователя обновляется кеш групп,
        #   связанных с департаментов

        department1 = self.create_department()
        department = self.create_department(parent_id=department1['id'])
        user = self.create_user(department_id=department['id'])
        group1 = self.create_group(
            members=[{
                'type': 'department',
                'id': user['department_id'],
            }]
        )
        group2 = self.create_group(
            members=[{
                'type': 'department',
                'id': self.another_department['id'],
            }]
        )
        response_data = self.patch_json(
            '/users/%s/' % user['id'],
            data={
                'department': {'id': self.another_department['id']}
            },
            expected_code=200
        )
        self.assertEqual(response_data['department'].get('id'),
                         self.another_department['id'])
        users_group1 = UserGroupMembership(self.main_connection).find({'group_id': group1['id']})
        assert_that(users_group1, empty())
        users_group2 = UserGroupMembership(self.main_connection).find({'group_id': group2['id']})
        user_ids = [x['user_id'] for x in users_group2]
        assert_that(user_ids, contains(user['id']))

    def test_with_percent_symbol(self):
        new_about = {'ru': '+%26+', 'en': ''}
        self.mocked_blackbox.batch_userinfo.return_value = [
            fake_userinfo(
                self.user['id'],
                timezone='Asia/Jakarta',
                language='ru',
            )
        ]
        response_data = self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'about': new_about,
            }
        )
        self.prepared_user['about'] = new_about
        self.assertEqual(response_data.get('about'), new_about)
        assert_that(
            response_data,
            is_same(self.prepared_user)
        )

    def test_patch_not_existing_department_id(self):
        department_id = 1000

        response_data = self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'department_id': department_id
            },
            expected_code=422
        )
        assert_that(
            response_data,
            has_entries(
                code='department_not_found',
                message='Unable to find department with id={id}',
                params={'id': department_id},
            )
        )

    def test_should_not_be_allowed_to_add_user_to_not_generic_group(self):
        response = self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'groups': [self.organization_admin_group['id']]
            },
            expected_code=422
        )
        assert_that(
            response,
            has_entries(
                code='invalid_group_type',
                message='Invalid group type "{type}"',
                params={'type': 'organization_admin'},
            )
        )

    def test_organization_admin_should_be_able_to_add_user_to_any_generic_group(self):
        """
        Добавляем группу, проверяем, что кеши групп обновились
        """
        parent_group = self.create_group(
            members=[{'type': 'group', 'id': self.generic_group['id']}])

        generic_group_membership = UserGroupMembership(self.main_connection).find(
            {'group_id': self.generic_group['id']}
        )
        assert_that(generic_group_membership, equal_to([]))
        parent_group__membership = UserGroupMembership(self.main_connection).find(
            {'group_id': parent_group['id']}
        )
        assert_that(parent_group__membership, equal_to([]))

        response_data = self.patch_json(
            '/users/%s/' % self.another_user_from_this_organization['id'],
            data={
                'groups': [self.generic_group['id']]
            }
        )
        self.assertEqual(len(response_data['groups']), 2)
        self.assertEqual(response_data['groups'][0]['id'], self.generic_group['id'])
        generic_group_membership = UserGroupMembership(self.main_connection).find(
            {'group_id': self.generic_group['id']}
        )
        assert_that(
            generic_group_membership,
            contains_inanyorder(has_entries(
                'user_id',
                self.another_user_from_this_organization['id']
            ))
        )
        parent_group__membership = UserGroupMembership(self.main_connection).find(
            {'group_id': parent_group['id']}
        )
        assert_that(
            parent_group__membership,
            contains_inanyorder(has_entries(
                'user_id',
                self.another_user_from_this_organization['id']
            ))
        )

    def test_simple_user_should_not_be_allowed_to_add_himself_to_generic_group(self):
        response = self.patch_json(
            '/users/%s/' % self.common_user['id'],
            data={
                'groups': [self.generic_group['id']]
            },
            headers=get_auth_headers(as_uid=self.common_user['id']),
            expected_code=403
        )
        self.assertEqual(response['code'], 'authorization-error')

    def test_simple_user_should_be_allowed_to_remove_himself_from_groups(self):
        group_one = create_group(
            self.main_connection,
            org_id=self.organization['id']
        )
        group_two = create_group(
            self.main_connection,
            org_id=self.organization['id']
        )
        group_three = create_group(
            self.main_connection,
            org_id=self.organization['id']
        )
        group_ids = [
            group_one['id'],
            group_two['id'],
            group_three['id'],
        ]
        GroupModel(self.main_connection).set_user_groups(
            org_id=self.organization['id'],
            user_id=self.common_user['id'],
            groups=group_ids,
        )
        new_group_ids = [
            group_one['id'],
            group_two['id'],
        ]
        self.patch_json(
            '/users/%s/' % self.common_user['id'],
            data={
                'groups': new_group_ids
            },
            headers=get_auth_headers(as_uid=self.common_user['id']),
        )
        user = UserModel(self.main_connection).get(
            user_id=self.common_user['id'],
            fields=[
                '*',
                'groups.*',
            ],
        )
        self.assertEqual(
            sorted([i['id'] for i in user['groups']]),
            sorted(new_group_ids)
        )

    def test_patch_departments_events(self):
        # проверим, что при переводе пользователя
        # из отдела в отдел сгенерятся соответствующие
        # события
        EventModel(self.main_connection).delete(force_remove_all=True)
        ActionModel(self.main_connection).delete(force_remove_all=True)
        group = self.create_group(
            members=[{
                'type': 'department',
                'id': self.user['department_id'],
            }]
        )
        self.create_resource_with_group(group_id=group['id'])
        self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'position': {
                    'en': 'New',
                    'ru': 'Новая',
                },
                'department_id': self.another_department['id'],
            },
            expected_code=200
        )

        events = EventModel(self.main_connection).find()
        events = [x['name'] for x in events]

        expected = [
            # события про изменение департаментов генерятся потому,
            # что при переводе сотрудника меняется счетстчик members_count
            'department_property_changed',
            'department_property_changed',
            'department_user_added',
            'department_user_deleted',
            'user_moved',
            'user_property_changed',
            'resource_grant_changed',  # для технического ресурса группы
            'resource_grant_changed',  # для пользовательского ресурса
        ]
        assert_that(events, contains_inanyorder(*expected))

        action = ActionModel(self.main_connection).find()[0]['name']
        assert_that(action, equal_to('user_modify'))

    def test_patch_departments_events_move_user_to_root(self):
        d1 = self.create_department(
            parent_id=ROOT_DEPARTMENT_ID,
        )
        d2 = self.create_department(
            parent_id=d1['id'],
        )
        user = self.create_user(department_id=d2['id'])
        resource = self.create_resource_with_department(d1['id'], relation='write')

        EventModel(self.main_connection).delete(force_remove_all=True)
        ActionModel(self.main_connection).delete(force_remove_all=True)

        self.patch_json(
            '/users/%s/' % user['id'],
            data={
                'position': {
                    'en': 'New',
                    'ru': 'Новая',
                },
                'department_id': ROOT_DEPARTMENT_ID,
            },
            expected_code=200
        )

        events = EventModel(self.main_connection).find()
        events = [event['name'] for event in events]

        expected = [
            # события про изменение департаментов генерятся потому,
            # что при переводе сотрудника меняется счетчик members_count
            # пересчёт идёт в департаментах d1 и d2, а в корневом количество не изменилось.
            'department_property_changed',
            'department_property_changed',
            # сотрудник добавляется в корневой департамент
            'department_user_added',
            # сотрудник удаляется из департаментов d1 и d2
            'department_user_deleted',
            'department_user_deleted',
            # сотрудник перемещён в другой департамент
            'user_moved',
            # информация о сотруднике изменилась
            'user_property_changed',
            # для пользовательского ресурса
            'resource_grant_changed',
        ]
        assert_that(events, contains_inanyorder(*expected))

        action = ActionModel(self.main_connection).find()[0]['name']
        assert_that(action, equal_to('user_modify'))

    def test_user_patch_with_groups_events(self):
        # Проверяем, что в PATCH /users/{id} при изменении поля group
        # user c id-шником удаляется или добавляется в соотв. группы.
        # Проверяем, что callback account_edit для Паспорта тоже вызывается
        # при PATCH /users/{id}. Проверяем, что генерируются необходимые event-ы.

        group_model = GroupModel(self.main_connection)
        event_model = EventModel(self.main_connection)
        action_model = ActionModel(self.main_connection)
        event_model.delete(force_remove_all=True)
        action_model.delete(force_remove_all=True)

        # Создаем группы:
        # group_user_no_change - группа с пользователем,
        # которая не должна поменяться
        org_id = self.organization['id']
        uid = self.user['id']

        group_user_no_change = self.create_group(members=[{
            'type': 'user',
            'object': self.user
        }])
        # group_user_removed - группа с пользователем,
        # из которой после patch-а пользователь должен удалиться
        group_user_removed = self.create_group(members=[{
            'type': 'user',
            'object': self.user
        }])
        # group_user_added - группа без members,
        # после patch-a пользователь должен быть добавлен в эту группу
        group_user_added = self.create_group()

        data, headers = self.patch_json(
            '/users/%s/' % uid,
            data={
                'about': {'ru': 'Тестовое описание'},
                'groups': [group_user_no_change['id'], group_user_added['id']],
            },
            expected_code=200,
            return_headers=True,
        )
        revision = headers['x-revision']
        events = event_model.find({'revision': revision})

        # Проверим, что должно было добавиться одно событие
        # про изменение свойств пользователя и ещё
        # по три события разного типа про удаление его из одной
        # команды и добавление в другую команду.
        assert_that(
            events,
            contains_inanyorder(
                has_entries(
                    name='user_property_changed'),
                has_entries(
                    name='group_membership_changed',
                    object=has_entries(
                        id=group_user_added['id']),
                    content=has_entries(
                        diff=has_entries(
                            members=has_entries(
                                add=has_entries(
                                    users=contains(uid)))))),
                has_entries(
                    name='group_membership_changed',
                    object=has_entries(
                        id=group_user_removed['id']),
                    content=has_entries(
                        diff=has_entries(
                            members=has_entries(
                                remove=has_entries(
                                    users=contains(uid)))))),
                has_entries(
                    name='user_group_added',
                    content=has_entries(
                        subject=has_entries(
                            id=group_user_added['id']))),
                has_entries(
                    name='user_group_deleted',
                    content=has_entries(
                        subject=has_entries(
                            id=group_user_removed['id']))),
                has_entries(
                    name='resource_grant_changed',
                    object=has_entries(
                        id=group_user_removed['resource_id']),
                    content=has_entries(
                        relations=has_entries(
                            remove=has_entries(
                                users=contains(uid))))),
                has_entries(
                    name='resource_grant_changed',
                    object=has_entries(
                        id=group_user_added['resource_id']),
                    content=has_entries(
                        relations=has_entries(
                            add=has_entries(
                                users=contains(uid))))),
            ))

        action = action_model.find()[0]['name']
        assert_that(action, equal_to('user_modify'))

        new_group_user_removed = group_model.get(
            org_id=org_id,
            group_id=group_user_removed['id'],
            fields=[
                '*',
                'members.*',
            ],
        )
        assert_that(
            new_group_user_removed['members'],
            equal_to([]),
        )

        new_group_user_no_change = group_model.get(
            org_id=org_id,
            group_id=group_user_no_change['id'],
            fields=[
                '*',
                'members.*',
            ],
        )
        assert_that(
            new_group_user_no_change['members'],
            contains_inanyorder(
                has_entries(
                    object=has_entries(id=uid)
                )
            )
        )

        new_group_user_added = group_model.get(
            org_id=org_id,
            group_id=group_user_added['id'],
            fields=[
                '*',
                'members.*',
            ],
        )
        assert_that(
            new_group_user_added['members'],
            contains_inanyorder(
                has_entries(
                    object=has_entries(id=uid)
                )
            )
        )

    def test_patch_simple_user_change_admin_status(self):
        for is_admin in [True, False]:
            response_data = self.patch_json(
                '/users/%s/' % self.another_user_from_this_organization['id'],
                data={
                    'is_admin': is_admin
                },
                headers=get_auth_headers(as_uid=self.another_user_from_this_organization['id']),
                expected_code=403
            )
            self.assertIn('message', response_data)

    def test_patch_restriction_no_personal_info_change_in_sso_org(self):
        response_data = self.patch_json(
            '/users/%s/' % self.another_sso_user['id'],
            data={
                'name': {
                    'first': {
                        'ru': 'Александр'
                    },
                    'last': {
                        'ru': 'Пушкин'
                    }
                },
            },
            headers=get_auth_headers(as_uid=self.another_sso_admin['id']),
            expected_code=403
        )
        self.assertIn('message', response_data)

        response_data = self.patch_json(
            '/users/%s/' % self.another_sso_user['id'],
            data={
                'birthday': '2011-01-01',
            },
            headers=get_auth_headers(as_uid=self.another_sso_admin['id']),
            expected_code=403
        )
        self.assertIn('message', response_data)

        response_data = self.patch_json(
            '/users/%s/' % self.another_sso_user['id'],
            data={
                'gender': 'female',
            },
            headers=get_auth_headers(as_uid=self.another_sso_admin['id']),
            expected_code=403
        )
        self.assertIn('message', response_data)

    def test_patch_restriction_no_password_change_for_sso_user(self):
        response_data = self.patch_json(
            '/users/%s/' % self.another_sso_user['id'],
            data={
                'password': '098754153636',
            },
            headers=get_auth_headers(as_uid=self.another_sso_admin['id']),
            expected_code=403
        )
        self.assertIn('message', response_data)

    def test_patch_restriction_no_is_enabled_change_for_sso_user(self):
        set_sso_in_organization(self.main_connection, self.another_sso_organization['id'], False, False)
        response_data = self.patch_json(
            '/users/%s/' % self.another_sso_user['id'],
            data={
                'is_enabled': True,
            },
            headers=get_auth_headers(as_uid=self.another_sso_admin['id']),
            expected_code=403
        )
        self.assertIn('message', response_data)

    def test_patch_restriction_no_block_for_sso_user(self):
        set_sso_in_organization(self.main_connection, self.another_sso_organization['id'], True, True)
        response_data = self.patch_json(
            '/users/%s/' % self.another_sso_user['id'],
            data={
                'is_enabled': False,
            },
            headers=get_auth_headers(as_uid=self.another_sso_admin['id']),
            expected_code=403
        )
        self.assertIn('message', response_data)

    def test_patch_restriction_no_unblock_for_sso_user(self):
        set_sso_in_organization(self.main_connection, self.another_sso_organization['id'], True, True)
        response_data = self.patch_json(
            '/users/%s/' % self.another_sso_user['id'],
            data={
                'is_enabled': True,
            },
            headers=get_auth_headers(as_uid=self.another_sso_admin['id']),
            expected_code=403
        )
        self.assertIn('message', response_data)

    def test_patch_grant_admin_revoke_admin_rights(self):
        # пробуем выдать/отобрать права админа для пользователя
        user = self.create_user()

        experiments = [(True, 'admin'), (False, 'user')]

        for is_admin, role in experiments:
            ActionModel(self.main_connection).delete(force_remove_all=True)

            response_data = self.patch_json(
                '/users/%s/' % user['id'],
                data={'is_admin': is_admin},
                headers=get_auth_headers(as_uid=self.prepared_user['id']),
            )
            self.assertEqual(response_data['is_admin'], is_admin)
            self.assertEqual(response_data['role'], role)

            user = self.get_json('/users/%s/' % user['id'])
            self.assertEqual(user['is_admin'], is_admin)
            self.assertEqual(user['role'], role)

            action_name = action.security_user_grant_organization_admin
            if not is_admin:
                action_name = action.security_user_revoke_organization_admin
            action_models_count = ActionModel(self.main_connection).count(
                filter_data={
                    'org_id': self.organization['id'],
                    'name': action_name
                }
            )
            msg = 'Должна была создаться запись об изменении прав администратора в истории действий'
            self.assertEqual(action_models_count, 1, msg=msg)

    def test_patch_last_portal_admin_in_sso_org_retract_admin_rights_prohibited(self):
        self.patch_json(
            '/users/%s/' % self.another_sso_portal_admin['id'],
            data={'is_admin': False},
            headers=get_auth_headers(as_uid=self.another_sso_admin['id']),
            expected_code=403
        )

    def test_patch_not_last_portal_admin_in_sso_org_retract_admin_rights(self):
        self.another_sso_portal_admin_2 = self.create_portal_user(
            org_id=self.another_sso_organization['id'],
            login='test-2',
            email='test-2@yandex.ru',
            is_admin=True,
        )

        self.patch_json(
            '/users/%s/' % self.another_sso_portal_admin_2['id'],
            data={'is_admin': False},
            headers=get_auth_headers(as_uid=self.another_sso_admin['id']),
            expected_code=200
        )

    def test_patch_deputy_admin_rights(self):
        # пробуем выдать/отобрать права заместителя админа для пользователя
        user = self.create_user()

        experiments = ['deputy_admin', 'user']

        for role in experiments:
            ActionModel(self.main_connection).delete(force_remove_all=True)

            response_data = self.patch_json(
                '/users/%s/' % user['id'],
                data={'role': role},
                headers=get_auth_headers(as_uid=self.prepared_user['id']),
            )

            self.assertEqual(response_data['role'], role)

            user = self.get_json('/users/%s/' % user['id'])
            self.assertEqual(user['role'], role)

            action_name = action.security_user_grant_deputy_admin
            if role == 'user':
                action_name = action.security_user_revoke_deputy_admin
            action_models_count = ActionModel(self.main_connection).count(
                filter_data={
                    'org_id': self.organization['id'],
                    'name': action_name
                }
            )
            msg = 'Должна была создаться запись об изменении прав заместителя администратора в истории действий'
            self.assertEqual(action_models_count, 1, msg=msg)

    def check_permissions(self, user, permissions):
        result = get_permissions(
            self.meta_connection,
            self.main_connection,
            user['id'],
            org_id=self.organization['id'],
        )
        assert_that(result, contains_inanyorder(*permissions))

    def test_patch_role_permissions(self):
        # меняем роль с заместителя на администратора и обратно и проверяем, что права обновляются корректно
        user = self.create_user()
        response_data = self.patch_json(
            '/users/%s/' % user['id'],
            data={'role': 'deputy_admin'},
            headers=get_auth_headers(as_uid=self.prepared_user['id']),
        )
        deputy_perms = get_deputy_admin_permissions()
        deputy_perms.remove(global_permissions.leave_organization)
        self.check_permissions(user, deputy_perms)

        admin_permission = get_all_admin_permssions()
        response_data = self.patch_json(
            '/users/%s/' % user['id'],
            data={'role': 'admin'},
            headers=get_auth_headers(as_uid=self.prepared_user['id']),
        )
        admin_permission.remove(global_permissions.leave_organization)
        self.check_permissions(user, admin_permission)

        response_data = self.patch_json(
            '/users/%s/' % user['id'],
            data={'role': 'deputy_admin'},
            headers=get_auth_headers(as_uid=self.prepared_user['id']),
        )
        self.check_permissions(user, deputy_perms)

    def test_deputy_admin_cant_revoke_rights(self):
        # заместитель админа не может отобрать это право у себя или дать другим
        deputy_admin = self.create_user()
        response_data = self.patch_json(
            '/users/%s/' % deputy_admin['id'],
            data={'role': 'deputy_admin'},
            headers=get_auth_headers(as_uid=self.prepared_user['id']),
        )

        simple_user = self.create_user()
        response_data = self.patch_json(
            '/users/%s/' % simple_user['id'],
            data={'role': 'deputy_admin'},
            headers=get_auth_headers(as_uid=deputy_admin['id']),
            expected_code=403
        )
        response_data = self.patch_json(
            '/users/%s/' % deputy_admin['id'],
            data={'role': 'user'},
            headers=get_auth_headers(as_uid=deputy_admin['id']),
            expected_code=403
        )

    def test_patch_take_away_admin_rights_from_himself(self):
        response_data = self.patch_json(
            '/users/%s/' % self.prepared_user['id'],
            data={
                'is_admin': False
            },
            headers=get_auth_headers(as_uid=self.prepared_user['id']),
            expected_code=403
        )
        self.assertIn(response_data['code'], 'cannot_change_organization_owner')

    def test_patch_dismiss_self(self):
        # пользователь увольняет сам себя
        dismiss_user = self.create_user(
            nickname='another-user',
            name=self.name,
            department_id=self.dep111['id'],
            uid=100500,
            outer_admin=False,
            groups=[
                self.another_user_group1['id'],
                self.another_user_group2['id'],
            ]
        )
        self.patch_json(
            '/users/%s/' % dismiss_user['id'],
            data={'is_dismissed': True},
            headers=get_auth_headers(as_uid=dismiss_user['id']),
        )
        du = UserDismissedModel(self.main_connection).get(dismiss_user['id'])
        assert_that(du, not_none())

    def test_patch_dismiss(self):
        # увольнение пользователя
        result, headers = self.patch_json(
            '/users/%s/' % self.dismiss_user['id'],
            data={'is_dismissed': True},
            return_headers=True,
        )
        revision = headers['x-revision']

        uid = self.dismiss_user['id']
        du = UserDismissedModel(self.main_connection).get(uid)
        assert_that(du, not_none())
        assert_that(
            du.get('groups', []),
            contains_inanyorder(
                has_entries({'id': self.another_user_group1['id']}),
                has_entries({'id': self.another_user_group2['id']}),
            )
        )
        assert_that(
            du.get('groups_admin', []),
            contains_inanyorder(
                has_entries({'id': self.another_user_group1['id']}),
            )
        )
        assert_that(
            du.get('department', []),
            has_entries({'id': self.dep111['id']}),
        )
        assert_that(
            du['department']['parents'],
            has_length(3)
        )
        # изгнали из всех групп
        assert_that(
            GroupModel(self.main_connection).count({
                'user_id': uid,
                'type': 'generic',
            }),
            equal_to(0)
        )
        # Уволенный сотрудник перемещается в корневой отдел
        assert_that(
            UserModel(self.main_connection).find(
                filter_data={
                    'id': uid,
                    'is_dismissed': True,
                },
                one=True
            )['department_id'],
            equal_to(ROOT_DEPARTMENT_ID)
        )
        # Действие про увольнение должно было сохраниться в базу
        actions = ActionModel(self.main_connection).find(
            {'revision': revision}
        )

        assert_that(
            actions,
            has_item(
                has_entries({'name': 'user_dismiss'}),
            )
        )
        # А за действием должно последовать ряд событий
        events = EventModel(self.main_connection).find(
            {'revision': revision}
        )

        # При увольнении должно сгенериться событие про то, что человек
        # удалён из отдела куда он входил и изо всех родительских.
        # А так же по три события разного типа, про удаление его
        # из двух команд куда он так же входил.
        assert_that(
            events,
            contains_inanyorder(
                has_entries(name='user_dismissed'),
                has_entries(
                    name='department_user_deleted',
                    object=has_entries(id=6),
                    content=has_entries(
                        directly=True)),
                has_entries(
                    name='department_user_deleted',
                    object=has_entries(id=5),
                    content=has_entries(
                        directly=False)),
                has_entries(
                    name='department_user_deleted',
                    object=has_entries(id=4),
                    content=has_entries(
                        directly=False)),
                # Почему-то цепочка отделов такая что тут пробел
                # в айдишниках.
                has_entries(
                    name='department_user_deleted',
                    object=has_entries(id=1),
                    content=has_entries(
                        directly=False)),
                # Удаление из первой команды
                has_entries(
                    name='group_membership_changed',
                    object=has_entries(
                        id=self.another_user_group1['id']),
                    content=has_entries(
                        diff=has_entries(
                            members=has_entries(
                                remove=has_entries(
                                    users=contains(uid)))))),
                has_entries(
                    name='user_group_deleted',
                    object=has_entries(
                        id=uid),
                    content=has_entries(
                        subject=has_entries(
                            id=self.another_user_group1['id']))),
                has_entries(
                    name='resource_grant_changed',
                    content=has_entries(
                        relations=has_entries(
                            remove=has_entries(
                                users=contains(uid))))),
                # Удаление из второй команды
                has_entries(
                    name='group_membership_changed',
                    object=has_entries(
                        id=self.another_user_group2['id']),
                    content=has_entries(
                        diff=has_entries(
                            members=has_entries(
                                remove=has_entries(
                                    users=contains(uid)))))),
                has_entries(
                    name='user_group_deleted',
                    object=has_entries(
                        id=uid),
                    content=has_entries(
                        subject=has_entries(
                            id=self.another_user_group2['id']))),
                has_entries(
                    name='resource_grant_changed',
                    content=has_entries(
                        relations=has_entries(
                            remove=has_entries(
                                users=contains(uid))))),

                # И ещё, по событию про изменение числа сотрудников в отделе
                has_entries(
                    name='department_property_changed',
                    object=has_entries(
                        id=6),
                    content=has_entries(
                        diff=has_only_entries(
                            members_count=[1, 0]),
                        directly=True)),
                has_entries(
                    name='department_property_changed',
                    object=has_entries(
                        id=5),
                    content=has_entries(
                        diff=has_only_entries(
                            members_count=[1, 0]),
                        directly=False)),
                has_entries(
                    name='department_property_changed',
                    object=has_entries(
                        id=4),
                    content=has_entries(
                        diff=has_only_entries(
                            members_count=[1, 0]),
                        directly=False)),
                has_entries(
                    name='department_property_changed',
                    object=has_entries(
                        id=1),
                    content=has_entries(
                        diff=has_only_entries(
                            members_count=[4, 3]),
                        directly=False)),
            )
        )
        # удалили все связанные ресурсы
        assert_that(
            ResourceModel(self.main_connection).count({'user_id': uid}),
            equal_to(0)
        )

    def test_patch_dismiss_robot(self):
        # увольнение робота
        robot = create_robot_for_anothertest(self.meta_connection, self.main_connection,
                                             self.organization['id'], 'slug', self.post_json)
        self.patch_json('/users/%s/' % robot['id'], data={'is_dismissed': True}, expected_code=403)

    def test_patch_robot(self):
        # запрещено редактировать роботов через API
        self.mocked_blackbox.userinfo.reset_mock()
        robot = create_robot_for_anothertest(self.meta_connection, self.main_connection,
                                             self.organization['id'], 'slug', self.post_json)
        self.patch_json('/users/%s/' % robot['id'], data={
            'contacts': another_contacts
        }, expected_code=403)

    def test_patch_dismissed_user(self):
        # Уволенный пользователь не может патчить себя.
        user = self.create_user()
        self.patch_json(
            '/users/%s/' % user['id'],
            data={'is_dismissed': True},
        )
        self.patch_json(
            '/users/%s/' % user['id'],
            data={'about': {'ru': 'Тестовое описание'}},
            headers=get_auth_headers(as_uid=user['id']),
            expected_code=403,
        )

    def test_dismiss_owner(self):
        new_admin = self.create_user()
        UserModel(self.main_connection).make_admin_of_organization(self.organization['id'], new_admin['id'])
        response = self.patch_json(
            '/users/%s/' % self.admin_uid,
            data={'is_dismissed': True},
            headers=get_auth_headers(as_uid=new_admin['id']),
            expected_code=422)

        assert_that(
            response,
            has_entries(
                code='cannot_dismiss_owner',
            )
        )

    def test_admin_cannot_block_owner_or_change_role(self):
        new_admin = self.create_user()
        UserModel(self.main_connection).make_admin_of_organization(self.organization['id'], new_admin['id'])

        self.patch_json(
            '/users/%s/' % self.admin_uid,
            data={'role': 'user'},
            headers=get_auth_headers(as_uid=new_admin['id']),
            expected_code=403)

        self.patch_json(
            '/users/%s/' % self.admin_uid,
            data={'is_enabled': False},
            headers=get_auth_headers(as_uid=new_admin['id']),
            expected_code=403)

    def test_owner_cannot_change_role_himself(self):
        self.patch_json(
            '/users/%s/' % self.admin_uid,
            data={'role': 'user'},
            headers=get_auth_headers(as_uid=self.admin_uid),
            expected_code=403)


class TestUserDetail_portal_accounts__patch(TestOrganizationWithoutDomainMixin, TestCase):
    def setUp(self):
        super(TestUserDetail_portal_accounts__patch, self).setUp()

        DomainModel(self.main_connection).create(
            'not-yandex-team',
            self.yandex_organization['id'],
            True,
            True,
        )

        set_auth_uid(self.yandex_admin['id'])

    def test_admin_can_change_role(self):

        self.patch_json(
            '/users/%s/' % self.yandex_user['id'],
            data={
                'role': 'admin',
            },
        )
        response = self.get_json('/users/?role=admin')['result']
        assert_that(
            response,
            contains_inanyorder(
                has_entries(id=self.yandex_admin['id']),
                has_entries(id=self.yandex_user['id'])
            )
        )

    def test_admin_can_make_admin(self):

        self.patch_json(
            '/users/%s/' % self.yandex_user['id'],
            data={
                'is_admin': True,
            },
        )
        response = self.get_json('/users/?role=admin')['result']
        assert_that(
            response,
            contains_inanyorder(
                has_entries(id=self.yandex_admin['id']),
                has_entries(id=self.yandex_user['id'])
            )
        )

    def test_admin_can_not_edit_timezone(self):
        # Нельзя редактировать timezone в портальный аккаунтах
        self.patch_json(
            '/users/%s/' % self.yandex_user['id'],
            data={
                'timezone': 'Europe/Moscow',
            },
            expected_code=403
        )

    def test_admin_can_not_edit_contacts(self):
        # Нельзя редактировать contacts в портальный аккаунтах
        self.patch_json(
            '/users/%s/' % self.yandex_user['id'],
            data={
                'contacts': another_contacts,
            },
            expected_code=403
        )

    def test_admin_can_not_edit_language(self):
        # Нельзя редактировать language в портальный аккаунтах
        self.patch_json(
            '/users/%s/' % self.yandex_user['id'],
            data={
                'language': 'ru',
            },
            expected_code=403
        )

    def test_admin_can_not_edit_gender(self):
        # Нельзя редактировать gender в портальный аккаунтах
        self.patch_json(
            '/users/%s/' % self.yandex_user['id'],
            data={
                'gender': 'male',
            },
            expected_code=403
        )

    def test_admin_can_not_edit_birthday(self):
        # Нельзя редактировать birthday в портальный аккаунтах
        self.patch_json(
            '/users/%s/' % self.yandex_user['id'],
            data={
                'birthday': birthday,
            },
            expected_code=403
        )

    def test_admin_can_not_edit_name(self):
        # Нельзя редактировать name в портальный аккаунтах
        name = {
            'first': {
                'ru': 'Владимир'
            },
            'last': {
                'ru': 'Путин'
            }
        }
        self.patch_json(
            '/users/%s/' % self.yandex_user['id'],
            data={
                'name': name,
            },
            expected_code=403
        )

    def test_admin_can_not_block(self):
        # Нельзя редактировать блокировать портальные аккаунты
        self.patch_json(
            '/users/%s/' % self.yandex_user['id'],
            data={
                'is_enabled': False,
            },
            expected_code=403,
        )


class TestUserDetail__patch_6(BaseMixin, TestCase):
    api_version = 'v6'

    def setUp(self):
        super(TestUserDetail__patch_6, self).setUp()
        self.contact = {
            'type': 'skype',
            'label': {
                'ru': 'Домашний',
                'en': 'Homie'
            },
            'value': 'polina-sosisa'
        }

    def test_patch_nickname(self):
        # запрещено менять nickname
        new_nickname = 'new_nickname'
        self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'nickname': new_nickname
            },
            expected_code=422
        )

    def test_patch_email(self):
        # запрещено менять email
        new_email = 'new_mail@%s' % self.organization_domain
        self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'email': new_email
            },
            expected_code=422
        )

    def test_patch_synthetic_contact(self):
        # запрещено передавать признак synthetic для контакта
        contact = deepcopy(self.contact)
        contact['synthetic'] = False
        self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'contacts': [contact]
            },
            expected_code=422
        )

    def test_patch_alias_contact(self):
        # запрещено передавать признак alias для контакта
        contact = deepcopy(self.contact)
        contact['alias'] = True
        self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'contacts': [contact]
            },
            expected_code=422,
        )

    def test_patch_timezone_passport_error(self):
        # Если паспорт возвращает timezone.invalid, ручка возвращает 422
        timezone = 'Europe/Moscow'
        with mocked_passport() as passport:
            passport.account_edit.side_effect = InvalidTimezone
            self.patch_json(
                '/users/%s/' % self.user['id'],
                data={
                    'timezone': timezone,
                },
                expected_code=422,
            )

    def test_patch_valid_timezone(self):
        timezone = 'Europe/Moscow'
        response = self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'timezone': timezone,
            },
            expected_code=200,
        )
        assert_that(
            response,
            has_key('timezone'),
        )

    def test_patch_language_passport_error(self):
        # Если паспорт возвращает language.invalid, ручка возвращает 422
        language = 'ru'
        with mocked_passport() as passport:
            passport.account_edit.side_effect = InvalidLanguage
            self.patch_json(
                '/users/%s/' % self.user['id'],
                data={
                    'language': language,
                },
                expected_code=422,
            )

    def test_patch_valid_language(self):
        response = self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'language': 'ru',
            },
            expected_code=200,
        )
        assert_that(
            response,
            has_key('language'),
        )


class TestUserListYandexTeamOrg__post(TestYandexTeamOrgMixin, TestCase):
    def setUp(self):
        super(TestUserListYandexTeamOrg__post, self).setUp()

        self.nickname = 'tester'
        self.test_name = {
            'first': {
                'ru': 'Александр',
                'en': 'Alex'
            },
            'last': {
                'ru': 'Артеменко',
                'en': 'Art'
            },
            'middle': {
                'ru': 'Артеменкович',
                'en': 'Artemenkovich'
            },
        }

    def test_create_user_with_robot_prefix(self):
        # запрещено создавать не роботные аккаунты с префиксом "robot-"
        nickname = 'robot-bender'

        data = {
            'name': self.test_name,
            'nickname': nickname,
            'password': '098754153636',
            'department_id': self.department['id'],
        }
        response = self.post_json('/users/', data=data, expected_code=422)

        assert_that(
            response,
            has_entries(
                code='invalid_value',
                params=has_entries(
                    field='nickname',
                    prefix=app.config['ROBOT_ACCOUNT_NICKNAME_PREFIX'],
                )
            )
        )

    def test_create_robot_without_robot_perfix(self):
        # запрещено создавать роботные аккаунты без префикса "robot-"
        nickname = 'bender'

        data = {
            'name': self.test_name,
            'nickname': nickname,
            'password': '098754153636',
            'department_id': self.department['id'],
            'is_yamb_bot': True,
        }
        response = self.post_json('/users/', data=data, expected_code=422)

        assert_that(
            response,
            has_entries(
                code='invalid_value',
                params=has_entries(
                    field='nickname',
                    prefix=app.config['ROBOT_ACCOUNT_NICKNAME_PREFIX'],
                )
            )
        )

    def test_create_robot_without_scope(self):
        # Запрещено создавать роботные аккаунты без скоупа manage_robots
        nickname = 'robot-bender'

        data = {
            'name': self.test_name,
            'nickname': nickname,
            'password': '098754153636',
            'department_id': self.department['id'],
            'is_yamb_bot': True,
        }
        with scopes(scope.write_users):
            response = self.post_json('/users/', data=data, expected_code=403)

        assert_that(
            has_entries(
                response,
                message=contains(scope.manage_yamb_bots)
            )
        )

    def test_raises_create_user(self):
        data = {
            'name': self.test_name,
            'nickname': self.nickname,
            'password': '098754153636',
            'department_id': self.department['id'],
        }
        response = self.post_json('/users/', data=data, expected_code=422)
        assert_that(
            has_entries(
                response,
                code='domain.yandex_team',
            )
        )

    def test_create_robot(self):
        nickname = 'robot-bender'

        data = {
            'name': self.test_name,
            'nickname': nickname,
            'password': '098754153636',
            'department_id': self.department['id'],
            'is_yamb_bot': True,
        }
        test_uid = 42
        self.mocked_team_passport.yambot_add.return_value = test_uid

        response = self.post_json(
            '/users/',
            data=data,
        )

        self.mocked_team_passport.yambot_add.assert_called_once_with()

        assert_that(
            response,
            has_entries(
                id=test_uid,
                is_robot=is_(True),
                nickname=nickname,
                user_type='yamb_bot',
            )
        )

        response = self.get_json('/v7/users/%s/?fields=nickname,is_robot,user_type' % test_uid)

        assert_that(
            response,
            has_entries(
                id=test_uid,
                is_robot=is_(True),
                nickname=nickname,
                user_type='yamb_bot',
            )
        )


class TestUserList__post(BaseMixin, TestCase):
    def setUp(self):
        super(TestUserList__post, self).setUp()

        UserModel(self.main_connection).make_admin_of_organization(
            org_id=self.organization['id'],
            user_id=self.user['id'],
        )
        self.test_name = {
            'first': {
                'ru': 'Александр',
                'en': 'Alex'
            },
            'last': {
                'ru': 'Артеменко',
                'en': 'Art'
            },
            'middle': {
                'ru': 'Артеменкович',
                'en': 'Artemenkovich'
            },
        }
        self.nickname = 'tester'
        self.department = DepartmentModel(self.main_connection).create(
            name={'en': 'DepartmentTest'},
            org_id=self.organization['id'],
            parent_id=1,
        )
        self.domain = self.organization_domain
        self.admin_uid = self.user['id']
        self.auth_headers = get_auth_headers()

    def test_minimal_properties_create_user_use_pass(self):
        nickname = 'alex-art'
        del self.test_name['middle']
        data = {
            'name': self.test_name,
            'nickname': nickname,
            'password': '1234456787',
            'department_id': self.department['id'],
            'birthday': format_date(birthday),
            'gender': 'male',
            'aliases': ['smth1']
        }
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.users.change_object_alias') as mock_alias:
            response = self.post_json('/users/', data)
            mock_alias.assert_called_once()

        assert_that(
            response,
            has_entries(
                name=data['name'],
                nickname=data['nickname'],
                gender='male'
            )
        )

    def test_minimal_properties_create_user_use_hash(self):
        nickname = 'alex-art'
        del self.test_name['middle']
        data = {
            'name': self.test_name,
            'nickname': nickname,
            'password': '1234456787',
            'password_mode': 'hash',
            'department_id': self.department['id'],
            'birthday': format_date(birthday),
            'gender': 'male',
        }

        response = self.post_json('/users/', data)
        assert_that(
            response,
            has_entries(
                name=data['name'],
                nickname=data['nickname'],
                gender='male'
            )
        )

    def test_users_limit(self):
        nickname = 'alex-art'
        del self.test_name['middle']
        data = {
            'name': self.test_name,
            'nickname': nickname,
            'password': '1234456787',
            'department_id': self.department['id'],
            'birthday': format_date(birthday),
            'gender': 'male',
        }

        OrganizationMetaModel(self.meta_connection).update(
            filter_data={'id': self.organization['id']},
            update_data={'limits': {'users_limit': 1}}
        )
        self.post_json('/users/', data, expected_code=422)

        OrganizationMetaModel(self.meta_connection).update(
            filter_data={'id': self.organization['id']},
            update_data={'limits': {'users_limit': 2}}
        )
        self.post_json('/users/', data)

    def test_simple_create_user(self):
        nickname = 'alex-art'
        external_id = 'alex-art-777'
        data = {
            'name': self.test_name,
            'nickname': nickname,
            'password': '1234456787',
            'department_id': self.department['id'],
            'position': position,
            'about': about,
            'birthday': format_date(birthday),
            'contacts': contacts,
            'gender': 'male',
            'external_id': external_id,
        }

        response = self.post_json('/users/', data)

        staff_link = url_join(app.config['STAFF_URL'], '{nickname}?org_id={org_id}&uid={uid}').format(
            tld=self.tld,
            nickname=nickname,
            org_id=self.user['org_id'],
            uid=response['id'],
        )
        expected_contacts = contains(
            has_entries(
                **contacts[0].copy(
                    main=False,
                    alias=False,
                    synthetic=False,
                )
            ),
            has_entries(
                **contacts[1].copy(
                    main=False,
                    alias=False,
                    synthetic=False,
                )
            ),
            has_entries(
                type='staff',
                value=staff_link,
                synthetic=True,
                alias=False,
                main=False,
            ),
            has_entries(
                type='email',
                value='alex-art@not_yandex_test.ws.autotest.yandex.ru',
                main=True,
                alias=False,
                synthetic=True,
            ),
        )
        assert_that(
            response,
            has_entries(
                name=data['name'],
                nickname=data['nickname'],
                gender='male',
                position=position,
                about=about,
                contacts=expected_contacts,
                external_id=external_id,
            )
        )
        assert_that(parse_birth_date(response['birthday']), birthday)

    def test_user_not_exist_in_meta(self):
        # когда пользователь не записался в мета базу, а только в мэин базе существует
        nickname = 'alex-art'
        user = UserModel(self.main_connection).create(
            id=1110000000000102,
            org_id=self.organization['id'],
            nickname=nickname,
            name=self.test_name,
            email='dsdnsk@jfrh.ru',
            gender='male',
        )
        data = {
            'name': self.test_name,
            'nickname': nickname+'two',
            'password': '123567890',
            'department_id': self.department['id'],
        }
        response_data = self.post_json(
            '/users/',
            data,
            expected_code=201,
        )
        assert_that(
            response_data,
            has_entries(
                id=user['id'],
                nickname=nickname,
            )
        )

    def test_user_not_exist_in_main(self):
        # когда пользователь не записался в мain базу, а только в meta базе существует
        nickname = 'alex-art'
        user = UserMetaModel(self.meta_connection).create(
            id=1110000000000102,
            org_id=self.organization['id'],
        )
        data = {
            'name': self.test_name,
            'nickname': nickname + 'two',
            'password': '123567890',
            'department_id': self.department['id'],
        }
        response_data = self.post_json(
            '/users/',
            data,
            expected_code=201,
        )
        assert_that(
            response_data,
            has_entries(
                id=user['id'],
                nickname=nickname + 'two',
            )
        )

    def test_user_exist_in_main_and_meta(self):
        # когда пользователь c таким id уже существует в main и meta базе
        nickname = 'alex-art'
        UserMetaModel(self.meta_connection).create(
            id=1110000000000102,
            org_id=self.organization['id'],
        )
        UserModel(self.main_connection).create(
            id=1110000000000102,
            org_id=self.organization['id'],
            nickname=nickname,
            name=self.test_name,
            email='dsdnsk@jfrh.ru',
            gender='male',
        )
        data = {
            'name': self.test_name,
            'nickname': nickname + 'two',
            'password': '123567890',
            'department_id': self.department['id'],
        }
        response_data = self.post_json('/users/', data, expected_code=422, )
        assert_that(
            response_data,
            has_entries(
              code='user_already_exists',
              message='User already exists',
            )
        )

    def test_creating_user_with_duplicated_external_id_should_return_409(self):
        first_nickname = 'alex-art'
        second_nickname = 'vova'
        external_id = 'user-777'

        def get_data(nickname):
            return {
                'name': self.test_name,
                'nickname': nickname,
                'password': '1234456787',
                'department_id': 1,
                'position': position,
                'about': about,
                'birthday': format_date(birthday),
                'contacts': contacts,
                'gender': 'male',
                'external_id': external_id,
            }

        response = self.post_json('/users/', get_data(first_nickname))
        self.post_json('/users/', get_data(second_nickname), expected_code=409)

        # Однако, если первого сотрудника уволить, то
        # должно быть можно завести нового и переиспользовать external_id
        UserModel(self.main_connection).dismiss(
            self.organization['id'],
            response['id'],
            self.admin_uid,
        )

        # Теперь пользователя завести удалось!
        self.post_json('/users/', get_data(second_nickname), expected_code=201)

    def test_language_user_from_organization(self):
        # язык юзера берется из организации

        nickname = 'langtr'
        data = {
            'name': self.test_name,
            'nickname': nickname,
            'password': '123456787',
            'department_id': self.department['id'],
            'position': position,
            'about': about,
            'birthday': format_date(birthday),
            'contacts': contacts,
            'gender': 'male',
        }

        uid = 12345
        self.mocked_passport.account_add.return_value = uid
        self.mocked_passport.account_add.side_effect = None

        response = self.post_json('/users/', data)

        self.assertEqual(response['id'], uid)

        expected_passport_data = {
            'birthday': birthday,
            'firstname': self.test_name['first']['ru'],
            'lastname': self.test_name['last']['ru'],
            'language': self.organization['language'],
            'login': nickname,
            'password': data['password'],
            'gender': data['gender'],
        }

        self.mocked_passport.account_add.assert_called_once_with(
            domain=ANY,
            user_data=expected_passport_data,
        )

    # def test_welcome_email(self):
    #     # при добавлении пользователя мы отправляем ему приветственное письмо
    #     nickname = 'alex-art'
    #     data = {
    #         'name': self.test_name,
    #         'nickname': nickname,
    #         'password': '1234456787',
    #         'department_id': self.department['id'],
    #         'position': position,
    #         'about': about,
    #         'birthday': format_date(birthday),
    #         'contacts': contacts,
    #         'gender': 'male',
    #     }
    #
    #     with patch('intranet.yandex_directory.src.yandex_directory.core.views.users.send_welcome_email') as welcome_email:
    #         response = self.post_json('/users/', data)
    #
    #     welcome_email.assert_called_once_with(
    #         meta_connection=ANY,
    #         main_connection=ANY,
    #         org_id=self.organization['id'],
    #         uid=response['id'],  # только новому сотруднику
    #     )

    # def test_no_welcome_email_for_portal_user(self):
    #     # при добавлении пользователя мы отправляем ему приветственное письмо
    #     nickname = 'alex-art'
    #     data = {
    #         'name': self.test_name,
    #         'nickname': nickname,
    #         'password': '1234456787',
    #         'department_id': self.department['id'],
    #         'position': position,
    #         'about': about,
    #         'birthday': format_date(birthday),
    #         'contacts': contacts,
    #         'gender': 'male',
    #     }
    #     org_id = self.organization['id']
    #     OrganizationModel(self.main_connection) \
    #         .filter(id=org_id) \
    #         .update(organization_type='portal')
    #
    #     with patch('intranet.yandex_directory.src.yandex_directory.core.views.users.send_welcome_email') as welcome_email:
    #         response = self.post_json('/users/', data)
    #
    #         assert_not_called(welcome_email)

    def test_with_wrong_department_id(self):
        self.test_name['first']['ru'] = ' %s ' % self.test_name['first']['ru']
        nickname = ' akhmetov '
        data = {
            'name': self.test_name,
            'nickname': nickname,
            'password': ' 123567890 ',
            'department_id': 9999,
            'position': position,
            'about': about,
            'birthday': format_date(birthday),
            'contacts': contacts,
            'gender': 'male',
        }
        self.post_json('/users/', data, expected_code=422)

    def test_simple_create_user_with_spaces(self):
        self.test_name['first']['ru'] = ' %s ' % self.test_name['first']['ru']
        nickname = ' akhmetov '
        data = {
            'name': self.test_name,
            'nickname': nickname,
            'password': ' 123567890 ',
            'department_id': self.department['id'],
            'position': position,
            'about': about,
            'birthday': format_date(birthday),
            'contacts': contacts,
            'gender': 'male',
        }
        response = self.post_json('/users/', data)
        self.assertEqual(response['name']['first']['ru'], self.test_name['first']['ru'].strip())
        self.assertEqual(response['nickname'], nickname.strip())

    def test_user_can_be_created_without_name(self):

        nickname = 'misterRoman'
        external_id = 'Romani4'
        data = {
            'nickname': nickname,
            'password': '1234456787',
            'department_id': self.department['id'],
            'position': position,
            'about': about,
            'birthday': format_date(birthday),
            'contacts': contacts,
            'gender': 'male',
            'external_id': external_id,
        }

        response = self.post_json('/users/', data)
        assert_that(
            response,
            has_entries(
                name={
                    'first': {
                'ru': '',
                'en': ''
            },
                    'last': {
                'ru': '',
                'en': ''
            },
                },
                nickname="misterroman",
                gender='male',
                position=position,
                about=about,
                external_id=external_id,
            )
        )

    def test_user_can_be_created_without_last_name(self):

        nickname = 'misterRoman'
        external_id = 'Romani4'
        name = {
            'first': {
                'ru': 'Роман',
                'en': 'TopGuy'
            },
        }
        data = {
            'name': name,
            'nickname': nickname,
            'password': '1234456787',
            'department_id': self.department['id'],
            'position': position,
            'about': about,
            'birthday': format_date(birthday),
            'contacts': contacts,
            'gender': 'male',
            'external_id': external_id,
        }

        response = self.post_json('/users/', data)

        assert_that(
            response,
            has_entries(
                name={
                    'first': {
                        'ru': 'Роман',
                        'en': 'TopGuy'
                    },
                    'last': {
                        'ru': '',
                        'en': ''
                    },
                },
                nickname="misterroman",
                gender='male',
                position=position,
                about=about,
                external_id=external_id,
            )
        )

    def test_user_can_be_created_without_gender(self):
        # С некоторых пор мы стали считать "пол" необязательным
        # параметром: DIR-3003
        # Проверим, что его можно не указывать при создании пользователя.

        nickname = 'akhmetov'
        data = {
            'name': self.test_name,
            'nickname': nickname,
            'password': '123567890',
            'department_id': self.department['id'],
            'position': position,
            'about': about,
            'birthday': format_date(birthday),
            'contacts': contacts
        }
        response = self.post_json('/users/', data)
        assert_that(
            response,
            has_entries(
                gender=none(),
            )
        )

    def test_user_can_be_created_without_birthday(self):
        # С некоторых пор мы стали считать "день рождения" необязательным
        # параметром: DIR-3003
        # Проверим, что его можно не указывать при создании пользователя.

        nickname = 'akhmetov'
        data = {
            'name': self.test_name,
            'nickname': nickname,
            'password': ' 123567890 ',
            'department_id': self.department['id'],
            'position': position,
            'about': about,
            'gender': 'male',
            'contacts': contacts,
        }
        response = self.post_json('/users/', data)
        assert_that(
            response,
            has_entries(
                birthday=none(),
            )
        )

    def test_create_user_from_outer_admin(self):
        nickname = 'alex-art'
        data = {
            'name': self.test_name,
            'nickname': nickname,
            'password': '1234456787',
            'department_id': self.department['id'],
            'position': position,
            'about': about,
            'birthday': format_date(birthday),
            'contacts': contacts,
            'gender': 'male',
        }
        outer_admin = self.create_user(nickname='outer_admin', is_outer=True)
        outer_admin_auth_headers = get_auth_headers(as_outer_admin=outer_admin)
        self.post_json('/users/', data, headers=outer_admin_auth_headers)

    def test_create_female(self):
        nickname = 'natasha'
        data = {
            'name': self.test_name,
            'nickname': nickname,
            'password': '1234456787',
            'department_id': self.department['id'],
            'birthday': format_date(birthday),
            'gender': 'female',
        }
        data = self.post_json('/users/', data)
        assert_that(data, has_entry('gender', 'female'))

    def test_forbidden_domain_user_create_for_sso_org(self):
        OrganizationSsoSettingsModel(self.main_connection).insert_or_update(self.organization['id'], True, True)

        nickname = 'akhmetov'
        data = {
            'name': self.test_name,
            'nickname': nickname,
            'password': ' 123567890 ',
            'department_id': self.department['id'],
            'position': position,
            'about': about,
            'gender': 'male',
            'contacts': contacts,
        }

        self.post_json('/users/', data, expected_code=403)

    def test_create_with_login_and_without_nickname(self):
        #  тест, чтобы не сломать POST - создание юзеров
        login = 'natasha@ya.ru'
        data = {
            'name': self.test_name,
            'login': login,
            'password': '1234456787',
            'department_id': self.department['id'],
            'birthday': format_date(birthday),
            'gender': 'female',
        }
        data = self.post_json('/users/', data)
        assert_that(data, has_entry('nickname', 'natasha'))
        assert_that(data, has_entry('login', 'natasha'))
        assert_that(data, has_entry('gender', 'female'))

    def test_invalid_post_data(self):
        data = {
            'name': 'Alexandr',
            'nickname': 'test',
            'password': 'test',
            'department_id': self.department['id']
        }
        self.mocked_passport.account_add.side_effect = LoginEmpty
        response = self.post_json('/users/', data, expected_code=422)
        assert_that(
            response,
            has_entries(
                code='schema_validation_error',
            )
        )

    def test_post_without_department(self):
        data = {
            'name': self.test_name,
            'nickname': 'alex-art',
            'password': '1234456787',
        }
        self.mocked_passport.account_add.side_effect = DomainInvalidType
        response = self.post_json('/users/', data, expected_code=422)
        assert_that(
            response,
            has_entries(
                code='schema_validation_error',
            )
        )

    def test_create_user_with_department(self):
        nickname = 'art'

        data = {
            'name': self.test_name,
            'nickname': nickname,
            'password': '098754153636',
            'department_id': self.department['id'],
            'gender': 'male',
            'birthday': format_date(birthday),
        }
        response = self.post_json('/users/', data)

        assert_that(
            response,
            has_entries(
                name=data['name'],
                nickname=data['nickname'],
                gender='male',
                department=has_entries(
                    name=self.department['name'],
                    id=self.department['id']
                )
            )
        )

    def test_conflicted_error(self):
        nickname = 'testtest'
        data = {
            'name': self.test_name,
            'nickname': nickname,
            'password': '0987654321',
            'department_id': self.department['id'],
            'birthday': format_date(birthday),
            'gender': 'male',
        }
        self.post_json('/users/', data)
        self.mocked_passport.account_add.side_effect = LoginNotavailable
        self.post_json('/users/', data, expected_code=409)

    def test_not_unique_nickname_with_group(self):
        not_uniq_nickname = 'nickname'
        self.create_group(label=not_uniq_nickname)

        data = {
            'name': self.test_name,
            'nickname': not_uniq_nickname,
            'password': '0987654321',
            'department_id': self.department['id'],
            'birthday': format_date(birthday),
            'gender': 'male',
        }
        self.mocked_passport.account_add.side_effect = LoginNotavailable
        response_data = self.post_json('/users/', data, expected_code=409)
        assert_that(
            response_data,
            has_entries(
                code='some_group_has_this_label',
                message='Some group already uses "{login}" as label',
            )
        )

    def test_can_create_not_unique_nickname_within_instance(self):
        not_uniq_nickname = 'nickname'

        another_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']
        self.another_user = self.create_user(
            uid=2,
            nickname=not_uniq_nickname,
            name=self.name,
            email='another-web-chib@ya.ru',
            org_id=another_organization['id']
        )

        data = {
            'name': self.test_name,
            'nickname': not_uniq_nickname,
            'password': '0987654321',
            'department_id': self.department['id'],
            'birthday': format_date(birthday),
            'gender': 'male',
        }
        self.post_json('/users/', data)

    def test_not_unique_nickname_with_department(self):
        not_uniq_nickname = 'nickname'
        self.create_department(label=not_uniq_nickname)

        data = {
            'name': self.test_name,
            'nickname': not_uniq_nickname,
            'password': '0987654321',
            'department_id': self.department['id'],
            'birthday': format_date(birthday),
            'gender': 'male',
        }
        response_data = self.post_json('/users/', data, expected_code=409)
        assert_that(
            response_data,
            has_entries(
                code='some_department_has_this_label',
                message='Some department already uses "{login}" as label',
                params={'login': not_uniq_nickname},
            )
        )

    def test_post_with_about_info(self):
        nickname = 'alex-art'
        data = {
            'name': self.test_name,
            'nickname': nickname,
            'password': '1234456787',
            'department_id': self.department['id'],
            'about': about,
            'gender': 'male',
            'birthday': format_date(birthday),
        }
        response = self.post_json('/users/', data)
        assert_that(
            response,
            has_entries(
                about=data['about'],
            )
        )

    def test_post_with_empty_nickname(self):
        del self.test_name['middle']
        data = {
            'name': self.test_name,
            'nickname': '',
            'password': '1234456787',
            'department_id': self.department['id'],
            'gender': 'male',
        }
        self.mocked_passport.account_add.side_effect = LoginEmpty
        self.post_json('/users/', data, expected_code=422)

    def test_post_events(self):
        # Пользователя добавляем в департамент, который входит в группу.
        # Есть ресурс, которому выданы права на эту группу.
        del self.test_name['middle']
        data = {
            'name': self.test_name,
            'nickname': self.nickname,
            'password': '1234456787',
            'department_id': self.department['id'],
            'gender': 'male',
            'birthday': format_date(birthday),
        }
        group = self.create_group(
            members=[{'type': 'department', 'object': self.department}]
        )
        self.create_resource_with_group(group_id=group['id'])
        EventModel(self.main_connection).delete(force_remove_all=True)
        ActionModel(self.main_connection).delete(force_remove_all=True)

        self.post_json('/users/', data)

        events = EventModel(self.main_connection).find()
        event_names = [x['name'] for x in events]
        expected = [
            'user_added',
            'department_user_added',
            'department_user_added',
            'resource_grant_changed',
            'resource_grant_changed',
            'department_property_changed',
            'department_property_changed',
        ]
        assert_that(event_names, equal_to(expected))

        # проверим, что department_property_changed
        # содержат правильные счетчики members_count

        # первое сообщение - про отдел куда
        # был добавлен пользователь
        event = events[5]
        assert_that(event['object']['parent_id'], equal_to(1))
        # и в нём должен быть один пользователь
        assert_that(
            event['content']['diff']['members_count'],
            contains(0, 1)
        )

        # и не должно там быть никаких других ключей
        assert_that(
            list(event['content']['diff'].keys()),
            equal_to(['members_count'])
        )

        # второе событие - про корневой отдел
        event = events[6]
        assert_that(event['object']['parent_id'], none())
        # и в нём теперь должно быть два пользователя
        assert_that(
            event['content']['diff']['members_count'],
            contains(1, 2)
        )

        # и тоже не должно там быть никаких других ключей
        assert_that(
            list(event['content']['diff'].keys()),
            equal_to(['members_count'])
        )

        action = ActionModel(self.main_connection).find()[0]['name']
        assert_that(action, equal_to('user_add'))

    def test_create_user_in_department_with_group(self):
        """
        Департамент входит в состав группы, добавляем сотрудника в этот
        департамент и проверяем, что кеш группы UserGroupMembership
        обновился.
        """
        nickname = 'test'
        data = {
            'name': self.test_name,
            'nickname': nickname,
            'password': '1234456787',
            'department_id': self.department['id'],
            'position': position,
            'about': about,
            'birthday': format_date(birthday),
            'contacts': contacts,
            'gender': 'male',
        }

        expected_contacts = []

        for contact in contacts:
            expected_contacts.append(
                has_entries(
                    **contact.copy(
                        main=False,
                        alias=False,
                        synthetic=False,
                    )
                )
            )

        user = self.create_user(department_id=self.department['id'])  # ToDo: этот и наш мок в одном тесте. Плохо.
        group = self.create_group(
            members=[{'type': 'department', 'object': self.department}]
        )

        user_count = len(
            UserGroupMembership(self.main_connection).find({'group_id': group['id']})
        )
        response = self.post_json('/users/', data)

        staff_link = url_join(app.config['STAFF_URL'], '{nickname}?org_id={org_id}&uid={uid}').format(
            tld=self.tld,
            nickname='test',
            org_id=self.user['org_id'],
            uid=response['id'],
        )

        expected_contacts.append(
            has_entries(
                type='staff',
                value=staff_link,
                synthetic=True,
                alias=False,
                main=False,
            )
        )

        expected_contacts.append(
            has_entries(
                type='email',
                value='test@not_yandex_test.ws.autotest.yandex.ru',
                synthetic=True,
                alias=False,
                main=True,
            )
        )

        assert_that(
            response,
            has_entries(
                name=data['name'],
                nickname=data['nickname'],
                gender='male',
                position=position,
                about=about,
                contacts=contains(*expected_contacts)
            )
        )
        assert_that(parse_birth_date(response['birthday']), birthday)

        user_count_after_post = len(
            UserGroupMembership(self.main_connection).find({'group_id': group['id']})
        )
        assert_that(user_count_after_post, user_count + 1)

    def test_create_user_with_robot_prefix(self):
        # запрещено создавать не роботные аккаунты с префиксом "robot-"
        nickname = 'robot-bender'

        data1 = {
            'name': self.test_name,
            'nickname': nickname,
            'password': '098754153636',
            'department_id': self.department['id'],
        }
        # login - deprecated поле
        data2 = data1.copy()
        del data2['nickname']
        data2['login'] = nickname

        for data in [data1, data2]:
            response = self.post_json('/users/', data=data, expected_code=422)

            assert_that(
                response,
                has_entries(
                    code='invalid_value',
                    params=has_entries(
                        field='nickname',
                        prefix=app.config['ROBOT_ACCOUNT_NICKNAME_PREFIX'],
                    )
                )
            )

    def test_create_robot_without_robot_perfix(self):
        # запрещено создавать роботные аккаунты без префикса "robot-"
        nickname = 'bender'

        data = {
            'name': self.test_name,
            'nickname': nickname,
            'password': '098754153636',
            'department_id': self.department['id'],
            'is_yamb_bot': True,
        }
        response = self.post_json('/users/', data=data, expected_code=422)

        assert_that(
            response,
            has_entries(
                code='invalid_value',
                params=has_entries(
                    field='nickname',
                    prefix=app.config['ROBOT_ACCOUNT_NICKNAME_PREFIX'],
                )
            )
        )

    def test_create_robot_without_scope(self):
        # Запрещено создавать роботные аккаунты без скоупа manage_robots
        nickname = 'robot-bender'

        data = {
            'name': self.test_name,
            'nickname': nickname,
            'password': '098754153636',
            'department_id': self.department['id'],
            'is_yamb_bot': True,
        }
        with scopes(scope.write_users):
            response = self.post_json('/users/', data=data, expected_code=403)

        assert_that(
            has_entries(
                response,
                message=contains(scope.manage_yamb_bots)
            )
        )

    def test_create_robot(self):
        nickname = 'robot-bender'

        data = {
            'name': self.test_name,
            'nickname': nickname,
            'password': '098754153636',
            'department_id': self.department['id'],
            'is_yamb_bot': True,
        }

        response = self.post_json(
            '/users/',
            data=data,
        )

        assert_that(
            response,
            has_entries(
                is_robot=is_(True),
                nickname=nickname,
                user_type='yamb_bot',
            )
        )

        response = self.get_json('/v7/users/%s/?fields=nickname,is_robot,user_type' % response['id'])

        assert_that(
            response,
            has_entries(
                is_robot=is_(True),
                nickname=nickname,
                user_type='yamb_bot',
            )
        )

    def test_requires_x_uid(self):
        # Требуем указания X-UID в headers.
        del self.test_name['middle']
        data = {
            'name': self.test_name,
            'nickname': self.nickname,
            'password': '1234456787',
            'department_id': self.department['id'],
            'birthday': format_date(birthday),
            'gender': 'male',
        }
        headers = get_auth_headers(as_org=self.organization['id'])
        response = self.post_json('/users/', data, headers=headers, expected_code=403)

    def test_create_with_timezone(self):
        timezone = 'Asia/Jakarta'
        data = {
            'name': self.test_name,
            'nickname': 'alex-art',
            'password': '1234456787',
            'department_id': self.department['id'],
            'birthday': format_date(birthday),
            'gender': 'male',
            'timezone': timezone,
        }

        response = self.post_json('/users/', data)
        assert_called_once(
            self.mocked_passport.account_edit,
            {
                'uid': response['id'],
                'timezone': timezone,
            },
        )
        assert_that(
            response,
            has_key('timezone'),
        )

    def test_create_with_language(self):
        language = 'ru'
        data = {
            'name': self.test_name,
            'nickname': 'alex-art',
            'password': '1234456787',
            'department_id': self.department['id'],
            'birthday': format_date(birthday),
            'gender': 'male',
            'language': language,
        }

        response = self.post_json('/users/', data)

        expected_passport_data = {
            'birthday': birthday,
            'firstname': data['name']['first']['ru'],
            'lastname': data['name']['last']['ru'],
            'login': data['nickname'],
            'password': data['password'],
            'gender': data['gender'],
            'language': language,
        }
        self.mocked_passport.account_add.assert_called_once_with(
            domain=ANY,
            user_data=expected_passport_data,
        )
        assert not self.mocked_passport.account_edit.called
        assert_that(
            response,
            has_key('language'),
        )

    def test_post_with_empty_fields(self):
        # DIR-2450 в ряде запросов может посылаться мусор. И в каждом случае мы должны
        # уметь обрабатывать этот мусор.
        headers = get_auth_headers(as_uid=self.user['id'])
        headers['X-Org-Id'] = '829285'
        self.post_json('/users/', data=None, expected_code=422, content_type=None)


class TestUserDetail__dismiss(BaseMixin, TestCase):
    def setUp(self):
        super(TestUserDetail__dismiss, self).setUp()

        self.another_user = self.create_user()
        self.admin_user = self.create_user(is_outer=True)

    def test_dismiss_without_permissions_should_return_error(self):
        # если у пользователя нет прав на увольнение людей - нужно выдать ему ошибку
        self._check_dismiss_permissions(expected_permission=False, as_uid=self.another_user['id'])
        self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'is_dismissed': True,
            },
            expected_code=403,
            headers=get_auth_headers(as_uid=self.another_user['id']),
        )

    def test_dismiss_with_permissions_must_create_action_objects(self):
        # при увольнении человека, нужно создать соответствующий Action объект
        ActionModel(self.main_connection).delete(force_remove_all=True)
        self._check_dismiss_permissions(expected_permission=True)

        old_user = self.another_user.copy()
        old_user['groups'] = []

        self.patch_json(
            '/users/%s/' % self.another_user['id'],
            data={
                'is_dismissed': True,
            },
            expected_code=200,
            headers=get_auth_headers(as_uid=self.admin_user['id']),
        )
        actions = ActionModel(self.main_connection).find()
        self.assertEqual(len(actions), 1)
        action_names = {action['name'] for action in actions}
        expected_action_names = {action.user_dismiss, 'user_dismiss'}
        msg = 'Были созданы записи ActionModel с неправильными именами'
        self.assertEqual(action_names, expected_action_names, msg=msg)

    def test_dismiss_with_permissions_should_call_user_model_dismiss_method(self):
        ActionModel(self.main_connection).delete(force_remove_all=True)
        self._check_dismiss_permissions(expected_permission=True)

        mocked_user_model = Mock()
        old_user = self.another_user.copy()
        old_user['groups'] = []
        mocked_user_model().get = Mock(return_value=old_user)
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.users.UserModel', mocked_user_model):
            self.patch_json(
                '/users/%s/' % self.another_user['id'],
                data={
                    'is_dismissed': True,
                },
                expected_code=200,
                headers=get_auth_headers(as_uid=self.admin_user['id']),
            )
            # Если у пользователя достаточно прав, нужно уволить сотрудника
            mocked_user_model().dismiss.assert_called_once_with(
                self.organization['id'],
                self.another_user['id'],
                old_user=old_user,
                author_id=self.admin_user['id'],
            )

    def test_dismiss_last_outer_admin_in_sso_org_prohibited(self):
        ActionModel(self.main_connection).delete(force_remove_all=True)
        self._check_dismiss_permissions(expected_permission=True)

        another_sso_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='microsoft',
            is_sso_enabled=True,
            is_provisioning_enabled=True,
        )['organization']

        sso_portal_admin = self.create_portal_user(
            org_id=another_sso_organization['id'],
            login='test-1',
            email='test-1@yandex.ru',
            is_admin=True,
        )

        another_sso_admin = self.create_user(
            org_id=another_sso_organization['id'],
            nickname='another-web-sso-admin',
            name=self.name,
            is_outer=True,
        )

        self.patch_json(
            '/users/%s/' % sso_portal_admin['id'],
            data={
                'is_dismissed': True,
            },
            expected_code=403,
            headers=get_auth_headers(as_uid=another_sso_admin['id']),
        )

    def test_dismiss_not_last_outer_admin_in_sso_org(self):
        ActionModel(self.main_connection).delete(force_remove_all=True)
        self._check_dismiss_permissions(expected_permission=True)

        another_sso_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='microsoft',
            is_sso_enabled=True,
            is_provisioning_enabled=True,
        )['organization']

        sso_portal_admin_1 = self.create_portal_user(
            org_id=another_sso_organization['id'],
            login='test-1',
            email='test-1@yandex.ru',
            is_admin=True,
        )

        sso_portal_admin_2 = self.create_portal_user(
            org_id=another_sso_organization['id'],
            login='test-2',
            email='test-2@yandex.ru',
            is_admin=True,
        )

        another_sso_admin = self.create_user(
            org_id=another_sso_organization['id'],
            nickname='another-web-sso-admin',
            name=self.name,
            is_outer=True,
        )

        self.patch_json(
            '/users/%s/' % sso_portal_admin_1['id'],
            data={
                'is_dismissed': True,
            },
            expected_code=200,
            headers=get_auth_headers(as_uid=another_sso_admin['id']),
        )

    def _check_dismiss_permissions(self, expected_permission, as_uid=None):
        real_permission = self.has_permissions(
            self.user['id'],
            [user_permissions.dismiss],
            admin_id=as_uid,
        )
        if expected_permission:
            msg = 'У пользователя должны быть права на увольнение перед началом теста'
        else:
            msg = 'У пользователя не должно быть прав на увольнение перед началом теста'
        self.assertEqual(real_permission, expected_permission, msg=msg)

    def test_delete_dismissed_user_resources(self):
        # при увольнении человека удаляем его связи с ресурсами

        self._check_dismiss_permissions(expected_permission=True)

        # создадим ресурс с пользователем
        ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='autotest',
            relations=[
                {
                    'name': 'read',
                    'user_id': self.another_user['id']
                }
            ]
        )

        self.patch_json(
            '/users/%s/' % self.another_user['id'],
            data={
                'is_dismissed': True,
            },
            expected_code=200,
            headers=get_auth_headers(as_uid=self.admin_user['id']),
        )

        # у уволенного пользователя нет связанных  ресурсов
        assert_that(
            ResourceModel(self.main_connection).find(
                filter_data={
                    'user_id': self.another_user['id']
                }
            ),
            empty()
        )


class TestUserDetail__change_password(BaseMixin, TestCase):
    def setUp(self):
        super(TestUserDetail__change_password, self).setUp()

        self.another_user = self.create_user()
        self.admin_user = self.create_user(is_outer=True)

    def test_change_password_without_permissions_should_raise_error(self):
        has_permissions = self.has_permissions(
            self.user['id'],
            [user_permissions.change_password],
            # На самом деле, another_user не админ
            admin_id=self.another_user['id'],
        )
        msg = 'У пользователя не должно быть прав менять пароли другим пользователям перед началом теста'
        self.assertFalse(has_permissions, msg=msg)

        self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'password': 'password'
            },
            expected_code=403,
            headers=get_auth_headers(as_uid=self.another_user['id']),
        )

    def test_change_password_with_permissions_should_call_passport(self):
        # проверяем, что идем в паспорт менять пароль
        ActionModel(self.main_connection).delete(force_remove_all=True)
        has_permissions = self.has_permissions(
            self.user['id'],
            [user_permissions.change_password],
        )
        self.assertTrue(has_permissions)
        track_id = 321
        new_password = 'password'

        self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'password': new_password
            },
            expected_code=200,
            headers=get_auth_headers(as_uid=self.admin_user['id']),
        )
        # Если у пользователя есть права на смену пароля - нужно сходить в паспорт и поменять его
        assert_called_once(
            self.mocked_passport.change_password,
            uid=self.user['id'],
            new_password=new_password,
            force_next_login_password_change=False,
        )

        actions = ActionModel(self.main_connection).find()
        self.assertEqual(len(actions), 2)
        action_names = {action['name'] for action in actions}
        expected_action_names = {action.security_user_password_changed, 'user_modify'}
        msg = 'Были созданы записи ActionModel с неправильными именами'
        self.assertEqual(action_names, expected_action_names, msg=msg)

        assert_called_once(
            self.mocked_passport.account_edit,
            {
                'uid': self.user['id'],
                'firstname': 'Admin',
                'lastname': 'Adminovich',
                'gender': '1',
                'birthday': datetime.datetime.now().strftime('%Y-%m-%d'),
            },
        )

    def test_change_password_with_password_change_required(self):
        # проверяем, что в апи паспорта передается параметр password_change_required,
        # который заставит пользователя менять пароль при первом входе
        track_id = 321
        new_password = 'password'
        self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'password': new_password,
                'password_change_required': True,
            },
            expected_code=200,
            headers=get_auth_headers(as_uid=self.admin_user['id']),
        )

        # Нужно сходить в паспорт и поменять пароль
        assert_called_once(
            self.mocked_passport.change_password,
            uid=self.user['id'],
            new_password=new_password,
            # И тут мы указываем, что нужно запросить смену пароля
            force_next_login_password_change=True,
        )

    def test_patch_user_should_return_422_if_password_wasnt_changed(self):
        # Если не получилось поменять пароль в паспорте - возвращаем 422

        has_permissions = self.has_permissions(
            self.user['id'],
            [user_permissions.change_password],
        )
        self.assertTrue(has_permissions)
        self.mocked_passport.change_password.side_effect = PasswordLong

        self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'password': 'password'
            },
            expected_code=422,
            headers=get_auth_headers(as_uid=self.admin_user['id']),
        )

    def test_patch_user_should_return_account_disabled_passport_error_is_account_disabled(self):
        # Если не получилось поменять пароль в паспорте из-за заблокированности пользователя - возвращаем 422
        # с соответствующей ошибкой
        has_permissions = self.has_permissions(
            self.user['id'],
            [user_permissions.change_password],
        )
        self.assertTrue(has_permissions)

        self.mocked_passport.change_password.side_effect = AccountDisabled

        response = self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'password': 'password'
            },
            expected_code=422,
            headers=get_auth_headers(as_uid=self.admin_user['id']),
        )
        assert_that(
            response,
            has_entries(
                code='account.disabled',
                message='Account is disabled',
            )
        )

    # NB: на неизвестную ошибку новая схема про ошибки возвращает 503
    # поэтому и здесь теперь измененно название теста и ожидаемый код
    # если это ок и ничего не ломает, то NB потом можно удалить
    def test_patch_user_with_changing_password_should_not_return_unknown_errors(self):
        has_permissions = self.has_permissions(
            self.user['id'],
            [user_permissions.change_password],
        )
        self.assertTrue(has_permissions)

        self.mocked_passport.change_password.side_effect = PassportException

        response = self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'password': 'password'
            },
            expected_code=503,
            headers=get_auth_headers(as_uid=self.admin_user['id']),
        )

    def test_patch_user_without_password_should_return_422(self):
        # Должны вернуть 422 если есть password_change_required, а нового пароля нет
        self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'password_change_required': True,
            },
            expected_code=422,
        )

    @patch('intranet.yandex_directory.src.yandex_directory.core.mailer.send')
    def test_change_password_send_to_external_email(self, mocked_send):
        password = 'password'
        to_email = 'external_email'
        campaign_slug = app.config['SENDER_CAMPAIGN_SLUG']['CHANGE_PASSWORD_EMAIL']

        self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'password_change_required': True,
                'password': password,
                'external_email': to_email,
            },
        )
        assert_called_once(
            mocked_send,
            main_connection=ANY,
            campaign_slug=campaign_slug,
            to_email=to_email,
            org_id=self.user['org_id'],
            mail_args={
                'lang': self.language,
                'tld': self.tld,
                'password': password,
                'login': '{}@{}'.format(self.user['nickname'], self.organization_domain)
            },
        )

    def test_change_password_send_to_external_email_no_password_change_required(self):
        self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'password': 'password',
                'external_email': 'external_email',
            },
            expected_code=422,
        )
        self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'password_change_required': False,
                'password': 'password',
                'external_email': 'external_email',
            },
            expected_code=422,
        )


class TestUserDetail__change_is_enabled_status(BaseMixin, TestCase):
    def setUp(self):
        super(TestUserDetail__change_is_enabled_status, self).setUp()

        self.another_user = self.create_user()
        self.admin_user = self.create_user(is_outer=True)

    def test_change_is_enabled_status_without_permissions_should_raise_error(self):
        self._check_user_permissions_to_block(self.another_user['id'], self.user['id'], has_permission=False)

        self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'is_enabled': False,
            },
            expected_code=403,
            headers=get_auth_headers(as_uid=self.another_user['id']),
        )

    def test_change_is_enabled_status_with_permissions_should_call_passport(self):
        common_user = self.create_user()
        self._check_user_permissions_to_block(self.admin_user['id'], common_user['id'])

        experiments = [
            {'is_enabled': False, 'action_name': action.security_user_blocked},
            {'is_enabled': True, 'action_name': action.security_user_unblocked},
        ]

        for experiment in experiments:
            # Чтобы правильно сгенерились события, надо специальным образом
            # замокать метод is_enable так, чтобы он сначала выдавал одно
            # значение, а потом другое. Это нужно потому, что поле is_enabled
            # подтягивается из blackbox, как prefetch_related поле, а в тестах
            # вызовы к blackbox замоканы так, что is_enabled всегда возвращает
            # False. Из-за того, что patch меняет только is_enabled, когда
            # метод возвращает всегда одно и то же, diff для событий получается
            # пустой, и события не генерятся.

            # Изначально, is_enabled должен быть противоположен тому состоянию,
            # в который мы переключаемся во время теста. Если проверяем, как
            # пользователь блокируется, то первый вызов is_enabled должен
            # вернуть True, а второй False. И наоборот.
            state = [not experiment['is_enabled']]
            with mocked_blackbox() as blackbox:

                def is_enabled(*args, **kwargs):
                    # blackbox дергается с тесте несколько раз, нам нужно переключать
                    # состояние только тогда когда запрашивается нужный атрибут
                    userinfo_return_value = []
                    if kwargs.get('uids') and '1009' in kwargs.get('attributes', ''):
                        result = state[0]
                        # следующее значение будет инвертированным
                        state[0] = not state[0]
                        userinfo_return_value = [{
                            'attributes': {'1009': str(int(result))},
                            'fields': collections.defaultdict(dict),
                            'uid': str(uid),
                            'default_email': None,
                            'karma': 123,
                        } for uid in kwargs['uids']]
                    return userinfo_return_value

                blackbox.batch_userinfo = is_enabled

                ActionModel(self.main_connection).delete(force_remove_all=True)
                EventModel(self.main_connection).delete(force_remove_all=True)

                if experiment['is_enabled']:
                    checked_function = self.mocked_passport.unblock_user
                else:
                    checked_function = self.mocked_passport.block_user

                with calls_count(checked_function, 1):
                    self.patch_json(
                        '/users/%s/' % common_user['id'],
                        data={
                            'is_enabled': experiment['is_enabled'],
                        },
                        expected_code=200,
                        headers=get_auth_headers(as_uid=self.admin_user['id']),
                    )

                if experiment['is_enabled']:
                    assert_called_once(
                        self.mocked_passport.account_edit,
                        {
                            'uid': common_user['id'],
                            'firstname': 'Gennady',
                            'lastname': 'Chibisov',
                            'gender': '1',
                            'birthday': ''
                        },
                    )
                else:
                    assert_not_called(self.mocked_passport.account_edit)

                actions = ActionModel(self.main_connection).find()
                self.assertEqual(len(actions), 2)
                action_names = {action['name'] for action in actions}
                expected_action_names = {experiment['action_name'], 'user_modify'}
                msg = 'Были созданы записи ActionModel с неправильными именами'
                self.assertEqual(action_names, expected_action_names, msg=msg)

                # Проверим, что в результате сгенерится одно событие, про изменение
                # свойства
                events = EventModel(self.main_connection).find()
                assert_that(
                    events,
                    contains(
                        has_entries(
                            name='user_property_changed',
                            content=has_entries(
                                diff=has_entries(
                                    is_enabled=[
                                        not experiment['is_enabled'],
                                        experiment['is_enabled']
                                    ]
                                )
                            )
                        )
                    )
                )

    def _check_user_permissions_to_block(self, user_id, user_id_to_block, has_permission=True):
        has_permissions = self.has_permissions(
            user_id_to_block,
            [user_permissions.block],
            admin_id=user_id,
        )
        if has_permission:
            msg = 'У пользователя должны быть права блокировать других пользователей перед началом теста'
            self.assertTrue(has_permissions, msg=msg)
        else:
            msg = 'У пользователя не должно быть прав блокировать других пользователей перед началом теста'
            self.assertFalse(has_permissions, msg=msg)

    def test_block_with_requests_error_should_return_503(self):
        """Если во время запроса в паспорт случилась какая-то сетевая ошибка - клиент вернёт PassportUnavailable
           и это должно приводить к 503 ошибке."""
        common_user = self.create_user()
        self._check_user_permissions_to_block(self.admin_user['id'], common_user['id'])

        self.mocked_passport.block_user.side_effect = PassportUnavailable
        self.patch_json(
            '/users/%s/' % common_user['id'],
            data={
                'is_enabled': False,
            },
            expected_code=503,
            headers=get_auth_headers(as_uid=self.admin_user['id']),
        )

    def test_change_is_enabled_status_for_yourself_should_return_access_denied_error(self):
        # прав быть не должно
        self._check_user_permissions_to_block(self.admin_uid, self.admin_uid, has_permission=False)

        self.patch_json(
            '/users/%s/' % self.admin_uid,
            data={
                'is_enabled': False,
            },
            expected_code=403,
            headers=get_auth_headers(as_uid=self.admin_uid),
        )
        msg = 'В паспорт ходить не нужно, т.к. прав на блокировку нет'
        assert_not_called(self.mocked_passport.block_user)


class TestUserChangeAvatarView__post(BaseMixin, TestCase):
    def setUp(self):
        super(TestUserChangeAvatarView__post, self).setUp()
        self.another_user = self.create_user()
        self.admin_user = self.create_user(is_outer=True)

    def test_change_avatar_without_permissions_should_raise_error(self):
        # Проверим, что один пользователь не может менять аватарку другому
        # пользователю
        self.post_json(
            '/users/%s/change-avatar/' % self.user['id'],
            data={
                'avatar_url': 'ya.ru/pic.img'
            },
            expected_code=403,
            headers=get_auth_headers(as_uid=self.another_user['id']),
        )

    def test_change_avatar_with_incorrect_data(self):
        # Проверяем, что вызвав метод с неверными параметрами не свалимся с 500
        self.post_json(
            '/users/%s/change-avatar/' % self.user['id'],
            data={'url': 'http://ya.ru/pic.img'},
            expected_code=422,
            headers=get_auth_headers(as_uid=self.admin_user['id']),
        )

    def test_change_avatar_with_permissions_should_call_passport(self):
        # Проверим, что вызываются методы паспорта, если есть соотв. права
        ActionModel(self.main_connection).delete(force_remove_all=True)
        has_permissions = self.has_permissions(
            self.user['id'],
            [user_permissions.edit],
        )
        self.assertTrue(has_permissions)

        img_url = 'ya.ru/pic.img'
        self.post_json(
            '/users/%s/change-avatar/' % self.user['id'],
            data={
                'avatar_url': img_url
            },
            expected_code=200,
            headers=get_auth_headers(as_uid=self.admin_user['id']),
        )

        assert_called_once(
            self.mocked_passport.change_avatar,
            self.user['id'],
            None,
            img_url,
        )

        actions = ActionModel(self.main_connection).find()
        assert_that(
            actions,
            contains(
                has_entries(name=action.security_user_avatar_changed),
                has_entries(name=action.user_modify),
            )
        )

    def test_change_avatar_by_file(self):
        # Меняем аватар, передавая файл с картинкой
        has_permissions = self.has_permissions(
            self.user['id'],
            [user_permissions.change_avatar],
        )

        self.assertTrue(has_permissions)

        filesdict = FileMultiDict()
        file_path = source_path(
            'intranet/yandex_directory/tests/unit/yandex_directory/passport/data/scream.jpg'
        )
        img = open(file_path, 'rb')
        filesdict.add_file('avatar_file', img, filename=file_path)
        file_img = filesdict.get('avatar_file')

        self.post_form_data(
            '/users/%s/change-avatar/' % self.user['id'],
            data={
                'avatar_file': file_img
            },
            expected_code=200,
            headers=get_auth_headers(as_uid=self.admin_user['id']),
        )
        assert_called_once(
            self.mocked_passport.change_avatar,
            self.user['id'],
            ANY,  # почему-то либа mock не умеет сравнивать файловые обекты и если передать file_img, то тест падает
            None,
        )

    def test_change_avatar_with_invalid_file(self):
        # Меняем аватар, передавая невалидный файл, паспорт вернет ошибку file.invalid, которую мы должны обработат
        # и вернуть 422 код наружу
        has_permissions = self.has_permissions(
            self.user['id'],
            [user_permissions.change_avatar],
        )
        self.assertTrue(has_permissions)

        filesdict = FileMultiDict()
        file_path = source_path(
            'intranet/yandex_directory/tests/unit/yandex_directory/passport/data/scream.jpg'
        )
        img = open(file_path, 'rb')
        filesdict.add_file('avatar_file', img, filename=file_path)
        file_img = filesdict.get('avatar_file')

        self.mocked_passport.change_avatar.side_effect = FileInvalid

        response_data = self.post_form_data(
            '/users/%s/change-avatar/' % self.user['id'],
            data={
                'avatar_file': file_img,
            },
            expected_code=422,
            headers=get_auth_headers(as_uid=self.admin_user['id']),
        )
        exp_response = {
            'code': 'change_avatar.file_invalid',
            'message': 'Invalid file for avatar',
        }
        self.assertEqual(response_data, exp_response)

    def test_change_avatar_by_url(self):
        # Меняем аватар, передавая url с картинкой
        has_permissions = self.has_permissions(
            self.user['id'],
            [user_permissions.change_avatar],
        )
        self.assertTrue(has_permissions)

        img_url = 'ya.ru/pic.img'
        self.post_json(
            '/users/%s/change-avatar/' % self.user['id'],
            data={
                'avatar_url': img_url
            },
            expected_code=200,
            headers=get_auth_headers(as_uid=self.admin_user['id']),
        )
        assert_called_once(
            self.mocked_passport.change_avatar,
            self.user['id'],
            None,
            img_url,
        )

    def test_change_avatar_should_return_422_if_avatar_cant_be_changed(self):
        # Если не получилось поменять аватарку в паспорте - возвращаем 422
        has_permissions = self.has_permissions(
            self.user['id'],
            [user_permissions.change_avatar],
        )
        self.assertTrue(has_permissions)

        self.mocked_passport.change_avatar.side_effect = ChangeAvatarInvalidImageSize

        self.post_json(
            '/users/%s/change-avatar/' % self.user['id'],
            data={
                'avatar_url': 'ya.ru/pic.img'
            },
            expected_code=422,
            headers=get_auth_headers(as_uid=self.user['id']),
        )

    def test_change_avatar_by_invalid_image_file_should_return_422(self):
        # Проверим, что ошибки от паспорта превращаются в коды ошибок
        # с префиксом change_avatar.
        has_permissions = self.has_permissions(
            self.user['id'],
            [user_permissions.change_avatar],
        )

        self.assertTrue(has_permissions)

        passport_to_directory_errors = [
            (ChangeAvatarInvalidImageSize, 'change_avatar.invalid_image_size'),
            (ChangeAvatarInvalidFileSize, 'change_avatar.invalid_file_size'),
            (ChangeAvatarInvalidUrl, 'change_avatar.invalid_url'),
            (FormInvalid, 'change_avatar.error'),
            (FileInvalid, 'change_avatar.file_invalid'),
        ]
        for passport_error, directory_error in passport_to_directory_errors:
            self.mocked_passport.change_avatar.side_effect = passport_error()

            response = self.post_json(
                '/users/%s/change-avatar/' % self.user['id'],
                data={
                    'avatar_url': 'ya.ru/pic.img'
                },
                expected_code=422,
                headers=get_auth_headers(as_uid=self.user['id']),
            )
            assert_that(
                response,
                has_entries(
                    code=directory_error,
                )
            )


class TestUserUserAliasesListView__post(BaseMixin, TestCase):

    def test_add_alias(self):
        # добавление алиаса для пользователя

        user_id = 1010
        old_aliases = ['alias1']
        added_alias = 'alias2'
        user_id = 1010
        nickname = 'petya'
        user = create_user(
            self.meta_connection,
            self.main_connection,
            user_id=user_id,
            org_id=self.organization['id'],
            nickname=nickname,
            name={'first': lstring('Petya'), 'last': lstring('Ivanov')},
            email='petya@ya.ru',
            groups=[],
            department_id=1,
            aliases=old_aliases,
        )

        with patch('intranet.yandex_directory.src.yandex_directory.core.views.users.action_user_modify'):
            response_data = self.post_json(
                '/users/%s/aliases/' % user['id'],
                data={
                    'name': added_alias
                },
            )
            # добавили алиас в Паспорт
            assert_called_once(
                self.mocked_passport.alias_add,
                user['id'],
                added_alias,
            )

        # алиасы обновились в директории
        assert_that(
            UserModel(self.main_connection).count(
                filter_data={
                    'id': user['id'],
                    'alias': added_alias
                }
            ),
            equal_to(1)
        )

        new_alaises = old_aliases + [added_alias]
        assert_that(
            response_data,
            has_entries(
                aliases=new_alaises,
                id=user['id']
            )
        )
        events = EventModel(self.main_connection).find({
            'org_id': user['org_id'],
            'revision': self.get_org_revision(self.organization['id']),
        })
        assert_that(events, has_length(1))
        assert_that(
            events[0],
            has_entries(
                object=has_entries(
                    id=user['id'],
                    aliases=has_item(added_alias),
                ),
                content=has_entries(alias_added=added_alias)
            )
        )

    def test_add_invalid_alias(self):
        # пытаемся добавить некорректный алиас
        user = self.create_user()

        self.mocked_passport.alias_add.side_effect = LoginProhibitedsymbols

        response_data = self.post_json(
            '/users/%s/aliases/' % user['id'],
            data={
                'name': 'страшый алиас'
            },
            expected_code=422,
        )
        assert_that(
            response_data,
            has_entries(
                code='login.prohibitedsymbols',
            )
        )

    def test_add_alias_to_sso_user_is_prohibited(self):
        # пытаемся добавить некорректный алиас
        another_sso_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='microsoft',
            is_sso_enabled=True,
            is_provisioning_enabled=True,
        )['organization']

        another_sso_user = self.create_user(
            org_id=another_sso_organization['id'],
            nickname='another-web-sso-chib',
            name=self.name,
            is_sso=True,
        )

        another_sso_admin = self.create_user(
            org_id=another_sso_organization['id'],
            nickname='another-web-sso-admin',
            name=self.name,
            is_outer=True,
        )

        self.mocked_passport.alias_add.side_effect = LoginProhibitedsymbols

        response_data = self.post_json(
            '/users/%s/aliases/' % another_sso_user['id'],
            data={
                'name': 'страшый алиас'
            },
            expected_code=403,
            headers=get_auth_headers(as_uid=another_sso_admin['id']),
        )
        assert_that(
            response_data,
            has_entries(
                code='cannot_create_sso_user_alias',
            )
        )

    def test_add_same_alias_as_in_passport(self):
        self.mocked_passport.alias_add.side_effect = AliasExists

        # пытаемся добавить алиас, который уже есть в паспорте

        user_id = 1010
        old_aliases = ['alias1']
        added_alias = 'alias2'
        nickname = 'petya'
        user = create_user(
            self.meta_connection,
            self.main_connection,
            user_id=user_id,
            org_id=self.organization['id'],
            nickname=nickname,
            name={'first': lstring('Petya'), 'last': lstring('Ivanov')},
            email='petya@ya.ru',
            groups=[],
            department_id=1,
            aliases=old_aliases,
        )

        with patch('intranet.yandex_directory.src.yandex_directory.core.views.users.action_user_modify'):
            with patch('intranet.yandex_directory.src.yandex_directory.core.utils.get_user_id_from_passport_by_login') as mock_data:
                mock_data.return_value = user_id

                response_data = self.post_json(
                    '/users/%s/aliases/' % user['id'],
                    data={
                        'name': added_alias
                    },
                )
            # добавили алиас в Паспорт
            assert_called_once(
                self.mocked_passport.alias_add,
                user['id'],
                added_alias,
            )

        # алиасы обновились в директории
        assert_that(
            UserModel(self.main_connection).count(
                filter_data={
                    'id': user['id'],
                    'alias': added_alias
                }
            ),
            equal_to(1)
        )

        new_alaises = old_aliases + [added_alias]
        assert_that(
            response_data,
            has_entries(
                aliases=new_alaises,
                id=user['id']
            )
        )
        events = EventModel(self.main_connection).find({
            'org_id': user['org_id'],
            'revision': self.get_org_revision(self.organization['id']),
        })
        assert_that(events, has_length(1))
        assert_that(
            events[0],
            has_entries(
                object=has_entries(
                    id=user['id'],
                    aliases=has_item(added_alias),
                ),
                content=has_entries(alias_added=added_alias)
            )
        )

    def test_add_same_alias_as_in_db(self):
        # пытаемся добавить алиас, который уже есть в нашей базе

        user_id = 1010
        old_aliases = ['alias1']
        added_alias = 'alias1'
        nickname = 'petya'
        user = create_user(
            self.meta_connection,
            self.main_connection,
            user_id=user_id,
            org_id=self.organization['id'],
            nickname=nickname,
            name={'first': lstring('Petya'), 'last': lstring('Ivanov')},
            email='petya@ya.ru',
            groups=[],
            department_id=1,
            aliases=old_aliases,
        )
        self.post_json(
            '/users/%s/aliases/' % user['id'],
            data={
                'name': added_alias
            },
            expected_code=422,
        )

    def test_add_same_alias_as_another_user_has(self):
        # пытаемся добавить алиас, который уже есть в в паспорте
        # но у другого пользователя

        self.mocked_passport.alias_add.side_effect = AliasExists
        user_id = 1010
        old_aliases = ['alias1']
        added_alias = 'alias2'
        nickname = 'petya'
        user = create_user(
            self.meta_connection,
            self.main_connection,
            user_id=user_id,
            org_id=self.organization['id'],
            nickname=nickname,
            name={'first': lstring('Petya'), 'last': lstring('Ivanov')},
            email='petya@ya.ru',
            groups=[],
            department_id=1,
            aliases=old_aliases,
        )
        with patch('intranet.yandex_directory.src.yandex_directory.core.utils.get_user_id_from_passport_by_login') as mock_data:
            mock_data.return_value = user_id + 1
            self.post_json(
                '/users/%s/aliases/' % user['id'],
                data={
                    'name': added_alias
                },
                expected_code=422,
            )

    def test_aliases_count_limit(self):
        old_aliases = []
        for i in range(10):
            old_aliases.append('alias%s' % i)
        added_alias = 'alias10'
        user_id = 1010
        nickname = 'petya'
        user = create_user(
            self.meta_connection,
            self.main_connection,
            user_id=user_id,
            org_id=self.organization['id'],
            nickname=nickname,
            name={'first': lstring('Petya'), 'last': lstring('Ivanov')},
            email='petya@ya.ru',
            groups=[],
            department_id=1,
            aliases=old_aliases,
        )

        with patch('intranet.yandex_directory.src.yandex_directory.core.views.users.action_user_modify'):
            response = self.post_json(
                '/users/%s/aliases/' % user['id'],
                data={
                    'name': added_alias
                },
                expected_code=422
            )
        assert_that(
            response,
            has_entries(
                code='too_many_aliases',
                message='User has to many aliases',
            )
        )


class TestUserUserAliasesDetailView__delete(BaseMixin, TestCase):

    def _delete_alias(self, delete_func=None, deleted_alias=None):
        """
        Общий тест для удаления алиасов
        :param delete_func: Функция для мока запрос на удаления в паспорте
        :type delete_func: function
        """

        old_aliases = ['alias1', 'alias2']
        deleted_alias = deleted_alias or 'alias2'

        nickname = 'petya'
        user = create_user(
            self.meta_connection,
            self.main_connection,
            user_id=1010,
            org_id=self.organization['id'],
            nickname=nickname,
            name={'first': lstring('Petya'), 'last': lstring('Ivanov')},
            email='petya@ya.ru',
            groups=[],
            department_id=1,
            aliases=old_aliases,
        )

        if delete_func:
            self.mocked_passport.alias_delete = delete_func

        with patch('intranet.yandex_directory.src.yandex_directory.core.views.users.action_user_modify'):
            self.delete_json(
                '/users/%s/aliases/%s/' % (user['id'], deleted_alias),
            )
            # удалили алиас в Паспорте
            assert_called_once(
                self.mocked_passport.alias_delete,
                user['id'],
                deleted_alias,
            )

        # алиасы обновились в директории
        assert_that(
            UserModel(self.main_connection).count(
                filter_data={
                    'id': user['id'],
                    'alias': deleted_alias
                }
            ),
            equal_to(0)
        )
        events = EventModel(self.main_connection).find({
            'org_id': user['org_id'],
            'revision': self.get_org_revision(self.organization['id']),
        })
        assert_that(events, has_length(1))
        assert_that(
            events[0],
            has_entries(
                object=has_entries(
                    id=user['id'],
                    aliases=not_(has_item(deleted_alias)),
                ),
                content=has_entries(alias_deleted=deleted_alias)
            )
        )

    def test_delete_alias(self):
        # удаление алиаса для пользователя

        self._delete_alias()

    def test_delete_alias_no_in_passport(self):
        # удаление алиаса который есть в директории, но нет в Паспорте

        self._delete_alias(
            Mock(side_effect=AliasNotFound)
        )

    def test_delete_alias_no_in_db(self):
        # удаление алиаса которого нет в директории

        self._delete_alias(deleted_alias='fake_one')


class TestUserSettingsListView__get(BaseMixin, TestCase):

    def test_default_header(self):
        # возвращаем значение настройки "header" по умолчанию
        # организация создается в setUp c шапкой по умолчанию

        response = self.get_json('/settings/')
        assert_that(
            response,
            equal_to({
                'header': 'connect',  # имя шапки по умолчанию
                'shared_contacts': False,
            })
        )

    def test_custom_header(self):
        # возвращаем измененное значение настройки "header"

        # выставим новую шапку для организации
        custom_header_name = 'custom'
        OrganizationModel(self.main_connection).update(
            update_data={'header': custom_header_name},
            filter_data={'id': self.user['org_id']}
        )

        response = self.get_json('/settings/')
        assert_that(
            response,
            equal_to({
                'header': custom_header_name,
                'shared_contacts': False,
            })
        )

    def test_shared_contacts_true(self):
        # возвращаем измененное значение настройки "shared_contacts"

        OrganizationModel(self.main_connection).update(
            update_data={'shared_contacts': True},
            filter_data={'id': self.user['org_id']}
        )

        response = self.get_json('/settings/')
        assert_that(
            response,
            equal_to({
                'header': 'connect',
                'shared_contacts': True,
            })
        )

    def test_view_require_org_id(self):
        # Проверим, что если org_id не известен, то ручка вернёт 403.
        # Раньше случалась пятисотка:
        # https://st.yandex-team.ru/TOOLSB-279

        # Используем любой uid, неизвестный Директории
        headers = get_auth_headers(as_uid=100500)

        # И ожидаем в ответ 403
        self.get_json('/settings/', headers=headers, expected_code=403)


class TestUserTokenView__get(BaseMixin, TestCase):

    def setUp(self):
        super(TestUserTokenView__get, self).setUp()
        self.headers = get_oauth_headers(as_anonymous=True)

        self.robot_uid = create_robot_for_service_and_org_id(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            service_slug=self.service['slug'],
            org_id=self.organization['id'],
        )

        UserMetaModel(self.meta_connection).update({'is_outer': False}, {'id': self.robot_uid})

    @oauth_success(OAUTH_CLIENT_ID)
    def test_user_not_found(self):
        # нет пользоватлея с таким id
        self.get_json('/users/100500/token/', headers=self.headers, expected_code=404)

    @oauth_success(OAUTH_CLIENT_ID)
    def test_user_not_robot(self):
        # пользователь не робот
        self.get_json('/users/%s/token/' % self.user['id'], headers=self.headers, expected_code=403)

    @oauth_success(OAUTH_CLIENT_ID)
    def test_no_secret(self):
        # для сlient_id сервиса незадано (internal_client_id, secret)
        self.get_json('/users/%s/token/' % self.robot_uid, headers=self.headers, expected_code=403)

    @oauth_success(OAUTH_CLIENT_ID)
    def test_not_get_token_error(self):
        # при получении токена возникла ошибка http запросы, то вернем 500
        get_oauth_service_data_result = 'internal_client_id', 'internal_client_secret'

        with patch('intranet.yandex_directory.src.yandex_directory.core.views.users.get_oauth_service_data',
                   return_value=get_oauth_service_data_result), \
             patch('requests.post') as mock_post:
            mock_post.return_value = PropertyMock(status_code=400, json=Mock(return_value={}))
            self.get_json('/users/%s/token/' % self.robot_uid, headers=self.headers, expected_code=500)

    @oauth_success(OAUTH_CLIENT_ID)
    def test_not_get_token_error_data(self):
        # при получении токена вернулся ответ с ошибкой, то вернем 500

        get_oauth_service_data_result = 'internal_client_id', 'internal_client_secret'
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.users.get_oauth_service_data',
                   return_value=get_oauth_service_data_result), \
             patch('requests.post') as mock_post:
            mock_post.return_value = PropertyMock(status_code=200, json=Mock(return_value={'error': 'code'}))
            self.get_json('/users/%s/token/' % self.robot_uid, headers=self.headers, expected_code=500)

    @oauth_success(OAUTH_CLIENT_ID)
    def test_get_token(self):
        # удачно получаем токен
        token = 'token'
        get_oauth_service_data_result = 'internal_client_id', 'internal_client_secret'

        with patch('intranet.yandex_directory.src.yandex_directory.core.views.users.get_oauth_service_data',
                   return_value=get_oauth_service_data_result), \
             patch('intranet.yandex_directory.src.yandex_directory.app.requests.post') as mock_post:
            # Само значение не важно, важно чтобы ручка отдала его на выходе.
            expires_in = 100500

            mock_post.return_value = PropertyMock(
                status_code=200,
                json=Mock(return_value={
                    'access_token': token,
                    'expires_in': expires_in,
                }))

            result = self.get_json('/users/%s/token/' % self.robot_uid, headers=self.headers)

        assert_that(
            result,
            equal_to({
                'token': token,
                'expires_in': expires_in,
            })
        )


class TestUserAttributesInAPI5(TestCase):
    api_version = 'v5'

    def test_get_returns_usual_strings(self):
        # GET запросы должны возвращать нормальные строки
        # вместо интернационализированных.

        response = self.get_json('/users/?fields=name,position')
        assert_that(
            response['result'],
            contains(
                has_entries(
                    name=has_entries(
                        first='Admin',
                        last='Adminovich',
                    ),
                    # Позиция не задана, так что тут будет
                    # как обычно – None.
                    position=None,
                )
            )
        )

    def test_patch_accepts_usual_strings(self):
        # PATCH запросы должны принимать нормальные строки
        # но в базу сохранять их как интернационализированные.

        uid = self.user['id']
        response = self.patch_json(
            '/users/{0}/'.format(uid),
            {
                'name': {'first': 'Василий', 'last': 'Пупкин'},
                'position': 'разработчик',
            }
        )
        user = UserModel(self.main_connection).find(
            {'id': uid},
            fields=['name', 'position'],
            one=True,
        )
        assert_that(
            user,
            has_entries(
                name=has_entries(
                    # Данные должны записываться в ru ключ
                    # несмотря на то, какой язык у организации,
                    # потому что все потребители старых версий API просто
                    # берут значение из ключа 'ru'.
                    first={'ru': 'Василий'},
                    last={'ru': 'Пупкин'},
                ),
                position={'ru': 'разработчик'},
            )
        )

    def test_users_without_deparment_dont_cause_error(self):
        # В какой-то момент была обнаружена пятисотка
        # при запросе поля department:
        # https://st.yandex-team.ru/DIR-3349
        # этот тест проверяет, что поле отдаётся нормально.
        # Проблема возникала если у роботного пользователя не задан
        # отдел.

        # Сначала создадим роботного пользователя без отдела

        self.post_json('/services/%s/enable/' % self.service['slug'], data=None)

        robots = OrganizationModel(self.main_connection).get_robots(self.organization['id'])
        assert_that(
            robots,
            contains(ANY)
        )

        # Теперь сделаем запрос и убедимся, что в качестве отдела у
        # роботного пользователя – None
        response = self.get_json('/users/?fields=department')

        assert_that(
            response,
            has_entries(
                result=contains(
                    has_entries(
                        id=robots[0]['id'],
                        department=none(),
                    ),
                    has_entries(
                        id=self.user['id'],
                        department=has_entries(
                            id=1,
                        )
                    )
                )
            )
        )

    def test_services_field_should_not_cause_error(self):
        # В какой-то момент была обнаружена пятисотка
        # при запросе поля services, если дополнительно не указано поле org_id
        # https://st.yandex-team.ru/DIR-3350
        # этот тест проверяет, что поле отдаётся нормально.

        # Сначала включим у организации сервис.
        # (это не обязательно, ошибка проявлялась и без этого,
        # но всё же хотелось бы проверить, что ручка отдаст корректные данные).
        self.post_json('/services/%s/enable/' % self.service['slug'], data=None)

        robots = OrganizationModel(self.main_connection).get_robots(self.organization['id'])

        # Теперь сделаем запрос и убедимся, что в качестве отдела у
        # роботного пользователя – None
        response = self.get_json('/users/?fields=services')

        assert_that(
            response,
            has_entries(
                result=contains_inanyorder(
                    has_entries(
                        id=robots[0]['id'],
                        services=contains(
                            has_entries(
                                slug='service-slug'
                            )
                        )
                    ),
                    has_entries(
                        id=self.user['id'],
                        services=contains(
                            has_entries(
                                slug='service-slug'
                            )
                        )
                    ),
                )
            )
        )

    def test_user_with_licensed_services(self):
        tracker = ServiceModel(self.meta_connection).create(
            client_id='some-client-id1',
            slug='tracker',
            name='tracker',
            robot_required=False,
            trial_period_months=1,
            paid_by_license=True,
            ready_default=True,
        )
        another_user = self.create_user()

        response = self.get_json('/users/?fields=services')
        assert_that(
            response,
            has_entries(
                result=contains_inanyorder(
                    has_entries(
                        id=self.user['id'],
                        services=[],
                    ),
                    has_entries(
                        id=another_user['id'],
                        services=[]
                    )
                )
            )
        )

        self.post_json('/services/%s/enable/' % tracker['slug'], data=None)
        self.put_json('/subscription/services/%s/licenses/' % tracker['slug'],
                      data=[
                          {
                              'type': 'user',
                              'id': self.user['id'],
                          }
                      ],
                      headers=get_auth_headers(as_uid=self.admin_uid),
                      expected_code=200,
                      )

        response = self.get_json('/users/?fields=services')
        # терекер есть у всех, т.к. в триальном периоде
        assert_that(
            response,
            has_entries(
                result=contains_inanyorder(
                    has_entries(
                        id=self.user['id'],
                        services=contains(
                            has_entries(
                                slug=tracker['slug'],
                                ready=True,
                            )
                        )
                    ),
                    has_entries(
                        id=another_user['id'],
                        services=contains(
                            has_entries(
                                slug=tracker['slug'],
                                ready=True,
                            )
                        )
                    )
                )
            )
        )
        # обновляем дату окончания триального периода трекера
        self.update_service_trial_expires_date(
            self.organization['id'],
            tracker['id'],
            utcnow() - datetime.timedelta(days=50)
        )
        response = self.get_json('/users/?fields=services')
        # трекер есть только у user
        assert_that(
            response,
            has_entries(
                result=contains_inanyorder(
                    has_entries(
                        id=self.user['id'],
                        services=contains(
                            has_entries(
                                slug=tracker['slug'],
                                ready=True,
                            )
                        )
                    ),
                    has_entries(
                        id=another_user['id'],
                        services=[]
                    )
                )
            )
        )


class TestUserDetailServicesFieldInAPI7(BaseUserDetailMixin__get, BaseMixin, TestCase):
    api_version = 'v7'

    def test_user_with_service_field(self):
        # Проверяем, что метод /user/{user_id}/?fields=services возвращает все включенные
        # сервисы для заданного пользователя

        # создаем 2 включенных сервиса
        service_data1 = {
            'name': 'Service Autotest #1',
            'slug': 'slug_autotest_1',
            'client_id': 'JfhHfkfjfKHfkwofkK',
        }
        ServiceModel(self.meta_connection).create(**service_data1)
        self.post_json('/services/%s/enable/' % service_data1['slug'], data=None)

        service_data2 = {
            'name': 'Service Autotest #2',
            'slug': 'slug_autotest_2',
            'client_id': 'YurutitoYriorotuUIIjJ',
        }
        ServiceModel(self.meta_connection).create(**service_data2)
        self.post_json('/services/%s/enable/' % service_data2['slug'], data=None)

        # и один сервис включим и выключим
        service_data3 = {
            'name': 'Service Autotest #3',
            'slug': 'slug_autotest_3',
            'client_id': 'sdfjlfjJJldjsfkjHHu',
        }
        ServiceModel(self.meta_connection).create(**service_data3)
        self.post_json('/services/%s/enable/' % service_data3['slug'], data=None)
        self.post_json('/services/%s/disable/' % service_data3['slug'], data=None)

        response_data = self.get_json('/users/%s/?fields=services,id' % self.user['id'])
        # проверяем, что выключенного сервиса нет в выдаче
        assert_that(
            response_data,
            has_entries(
                id=self.user['id'],
                services=contains_inanyorder(
                    has_entries(
                        name=service_data1['name'],
                        slug=service_data1['slug'],
                    ),
                    has_entries(
                        name=service_data2['name'],
                        slug=service_data2['slug'],
                    )
                )
            )
        )

    def test_user_with_org_type(self):
        response_data = self.get_json('/users/%s/?fields=id,nickname,organization.organization_type' % self.user['id'])
        assert_that(
            response_data,
            has_entries(
                id=self.user['id'],
                nickname=self.user['nickname'],
                organization={
                    'id': self.organization['id'],
                    'organization_type': 'common',
                },
            )
        )

    def test_user_with_licensed_services(self):
        tracker = ServiceModel(self.meta_connection).create(
            client_id='some-client-id1',
            slug='tracker',
            name='tracker',
            robot_required=False,
            trial_period_months=1,
            paid_by_license=True,
            ready_default=True,
        )
        wiki = ServiceModel(self.meta_connection).create(
            client_id='some-client-id2',
            slug='wiki',
            name='wiki',
            robot_required=False,
            paid_by_license=True,
            ready_default=True,
        )
        self.post_json('/services/%s/enable/' % tracker['slug'], data=None)
        self.post_json('/services/%s/enable/' % wiki['slug'], data=None)
        self.put_json('/subscription/services/%s/licenses/' % tracker['slug'],
                      data=[
                          {
                              'type': 'user',
                              'id': self.user['id'],
                          }
                      ],
                      headers=get_auth_headers(as_uid=self.admin_uid),
                      expected_code=200,
                      )
        # обновляем дату окончания триального периода трекера
        self.update_service_trial_expires_date(
            self.organization['id'],
            tracker['id'],
            utcnow() - datetime.timedelta(days=50)
        )
        response_data = self.get_json('/users/%s/?fields=services,id,nickname' % self.user['id'])
        # у user есть только трекер
        assert_that(
            response_data,
            has_entries(
                id=self.user['id'],
                nickname=self.user['nickname'],
                services=contains(
                    has_entries(
                        name=tracker['name'],
                        slug=tracker['slug'],
                    ),
                )
            )
        )


class TestUserListServicesFieldInAPI7(TestCase):
    api_version = 'v7'

    def test_user_with_licensed_services(self):
        tracker = ServiceModel(self.meta_connection).create(
            client_id='some-client-id1',
            slug='tracker',
            name='tracker',
            robot_required=False,
            trial_period_months=1,
            paid_by_license=True,
            ready_default=True,
        )
        another_user = self.create_user()

        response = self.get_json('/users/?fields=services')
        assert_that(
            response,
            has_entries(
                result=contains_inanyorder(
                    has_entries(
                        id=self.user['id'],
                        services=[],
                    ),
                    has_entries(
                        id=another_user['id'],
                        services=[]
                    )
                )
            )
        )

        self.post_json('/services/%s/enable/' % tracker['slug'], data=None)
        self.put_json('/subscription/services/%s/licenses/' % tracker['slug'],
                      data=[
                          {
                              'type': 'user',
                              'id': self.user['id'],
                          }
                      ],
                      headers=get_auth_headers(as_uid=self.admin_uid),
                      expected_code=200,
                      )

        response = self.get_json('/users/?fields=services')
        # терекер есть у всех, т.к. в триальном периоде
        assert_that(
            response,
            has_entries(
                result=contains_inanyorder(
                    has_entries(
                        id=self.user['id'],
                        services=contains(
                            has_entries(
                                slug=tracker['slug'],
                            )
                        )
                    ),
                    has_entries(
                        id=another_user['id'],
                        services=contains(
                            has_entries(
                                slug=tracker['slug'],
                            )
                        )
                    )
                )
            )
        )
        # обновляем дату окончания триального периода трекера
        self.update_service_trial_expires_date(
            self.organization['id'],
            tracker['id'],
            utcnow() - datetime.timedelta(days=50)
        )
        response = self.get_json('/users/?fields=services')
        # трекер есть только у user
        assert_that(
            response,
            has_entries(
                result=contains_inanyorder(
                    has_entries(
                        id=self.user['id'],
                        services=contains(
                            has_entries(
                                slug=tracker['slug'],
                            )
                        )
                    ),
                    has_entries(
                        id=another_user['id'],
                        services=[]
                    )
                )
            )
        )

    def test_filter_services(self):
        tracker = ServiceModel(self.meta_connection).create(
            client_id='some-client-id1',
            slug='tracker',
            name='tracker',
            robot_required=False,
            trial_period_months=1,
            paid_by_license=True,
            ready_default=True,
        )

        wiki = ServiceModel(self.meta_connection).create(
            client_id='some-client-id2',
            slug='wiki',
            name='wiki',
            robot_required=False,
            ready_default=True,
        )
        # синхронизируем таблицу services
        UpdateServicesInShards().try_run()
        another_user = self.create_user()

        # сервис не подключен
        response = self.get_json('/users/?service=wiki')
        assert_that(
            response,
            has_entries(
                result=[]
            )
        )

        self.post_json('/services/%s/enable/' % tracker['slug'], data=None)
        self.post_json('/services/%s/enable/' % wiki['slug'], data=None)
        self.put_json(
            '/subscription/services/%s/licenses/' % tracker['slug'],
            data=[
                {
                    'type': 'user',
                    'id': self.user['id'],
                }
            ],
            headers=get_auth_headers(as_uid=self.admin_uid),
            expected_code=200,
        )

        # вики доступен всем
        response = self.get_json('/users/?service=wiki')
        assert_that(
            response,
            has_entries(
                result=contains_inanyorder(
                    has_entries(
                        id=self.user['id']
                    ),
                    has_entries(
                        id=another_user['id']
                    )
                )
            )
        )
        # трекер доступен всем, потому что в триальном периоде
        response = self.get_json('/users/?service=tracker')
        assert_that(
            response,
            has_entries(
                result=contains_inanyorder(
                    has_entries(
                        id=self.user['id']
                    ),
                    has_entries(
                        id=another_user['id']
                    )
                )
            )
        )

        # обновляем дату окончания триального периода трекера
        self.update_service_trial_expires_date(
            self.organization['id'],
            tracker['id'],
            utcnow() - datetime.timedelta(days=50)
        )
        response = self.get_json('/users/?service=tracker')
        # трекер есть только у user
        assert_that(
            response,
            has_entries(
                result=contains(
                    has_entries(
                        id=self.user['id'],
                    )
                )
            )
        )

    def test_filter_services_with_license_issued(self):
        # проверяем, что если в слаге сервиса указан license_issued, то отдаются только пользователи
        # с выданными лицензиями на сервис

        tracker = ServiceModel(self.meta_connection).create(
            client_id='some-client-id1',
            slug='tracker',
            name='tracker',
            robot_required=False,
            trial_period_months=1,
            paid_by_license=True,
            ready_default=True,
        )

        # синхронизируем таблицу services
        UpdateServicesInShards().try_run()
        another_user = self.create_user()

        self.post_json('/services/%s/enable/' % tracker['slug'], data=None)
        self.put_json(
            '/subscription/services/%s/licenses/' % tracker['slug'],
            data=[
                {
                    'type': 'user',
                    'id': self.user['id'],
                }
            ],
            headers=get_auth_headers(as_uid=self.admin_uid),
            expected_code=200,
        )

        # трекер доступен всем, потому что в триальном периоде
        response = self.get_json('/users/?service=tracker')
        assert_that(
            response,
            has_entries(
                result=contains_inanyorder(
                    has_entries(
                        id=self.user['id']
                    ),
                    has_entries(
                        id=another_user['id']
                    )
                )
            )
        )

        # получаем пользователей только с выданными лицензиями
        response = self.get_json('/users/?service=tracker.license_issued')
        assert_that(
            response,
            has_entries(
                result=[{'id': self.user['id']}]
            )
        )

    def test_filter_services_with_multiple_license_issued(self):
        # проверяем, что пользователи с лиценщиями выдаются 1 раз, если лицензия выдана разными способами

        tracker = ServiceModel(self.meta_connection).create(
            client_id='some-client-id1',
            slug='tracker',
            name='tracker',
            robot_required=False,
            trial_period_months=1,
            paid_by_license=True,
            ready_default=True,
        )

        # синхронизируем таблицу services
        UpdateServicesInShards().try_run()
        dep = self.create_department()
        another_user = self.create_user(department_id=dep['id'])

        self.post_json('/services/%s/enable/' % tracker['slug'], data=None)
        person_id = 1
        client_id = 2
        first_name = 'Alexander'
        last_name = 'Akhmetov'
        middle_name = 'R'
        phone = '+7'
        email = 'akhmetov@yandex-team.ru'

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {}
        mocked_xmlrpc.Balance.CreateClient.return_value = (None, None, client_id)
        mocked_xmlrpc.Balance.CreatePerson.return_value = person_id

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            OrganizationModel(self.main_connection).create_contract_info_for_natural_person(
                org_id=self.organization['id'],
                author_id=self.admin_uid,
                first_name=first_name,
                last_name=last_name,
                middle_name=middle_name,
                phone=phone,
                email=email,
            )
        self.put_json(
            '/subscription/services/%s/licenses/' % tracker['slug'],
            data=[
                {
                    'type': 'user',
                    'id': another_user['id'],
                },
                {
                    'type': 'department',
                    'id': dep['id'],
                },
            ],
            headers=get_auth_headers(as_uid=self.admin_uid),
            expected_code=200,
        )

        # получаем пользователей только с выданными лицензиями
        response = self.get_json('/users/?service=tracker.license_issued')
        assert_that(
            response,
            has_entries(
                result=[{'id': another_user['id']}]
            )
        )

    def test_filter_not_ready_service(self):
        # проверим, что платный сервис доступен всем, если ready=False
        not_ready_service = ServiceModel(self.meta_connection).create(
            client_id='some-client-id3',
            slug='abc',
            name='abc',
            ready_default=False,
            paid_by_license=True,
            trial_period_months=1,
        )

        # синхронизируем таблицу services
        UpdateServicesInShards().try_run()
        another_user = self.create_user()

        self.post_json('/services/%s/enable/' % not_ready_service['slug'], data=None)

        org_service = OrganizationServiceModel(self.main_connection).find(
            filter_data={
                'org_id': self.organization['id'],
                'service_id': not_ready_service['id'],
            },
            fields=['ready'],
            one=True,
        )
        assert_that(
            org_service['ready'],
            equal_to(False)
        )

        # сервис доступен всем, потому что в триланом периоде
        response = self.get_json('/users/?fields=services')
        assert_that(
            response,
            has_entries(
                result=contains_inanyorder(
                    has_entries(
                        id=self.user['id'],
                        services=contains(
                            has_entries(
                                slug=not_ready_service['slug'],
                            )
                        )
                    ),
                    has_entries(
                        id=another_user['id'],
                        services=contains(
                            has_entries(
                                slug=not_ready_service['slug'],
                            )
                        )
                    )
                )
            )
        )

        response = self.get_json('/users/?service=abc')
        assert_that(
            response,
            has_entries(
                result=contains_inanyorder(
                    has_entries(
                        id=self.user['id']
                    ),
                    has_entries(
                        id=another_user['id']
                    )
                )
            )
        )
        # обновляем дату окончания триального периода трекера
        self.update_service_trial_expires_date(
            self.organization['id'],
            not_ready_service['id'],
            (utcnow() - datetime.timedelta(days=1)),
        )
        # выключаем сервис
        self.post_json('/services/%s/disable/' % not_ready_service['slug'], data=None, expected_code=201)

        # проверяем, что триал кончился вчера
        org_service = OrganizationServiceModel(self.main_connection).find(
            filter_data={
                'org_id': self.organization['id'],
                'service_id': not_ready_service['id'],
                'enabled': False,
            },
            fields=['trial_expires'],
            one=True,
        )
        assert_that(
            org_service['trial_expires'],
            equal_to((utcnow() - datetime.timedelta(days=1)).date())
        )
        # включаем сервис второй раз
        self.post_json('/services/%s/enable/' % not_ready_service['slug'], data=None)

        # сервис не доступен никому - нет лицензий
        response = self.get_json('/users/?fields=services')
        assert_that(
            response,
            has_entries(
                result=contains_inanyorder(
                    has_entries(
                        id=self.user['id'],
                        services=[]
                    ),
                    has_entries(
                        id=another_user['id'],
                        services=[]
                    )
                )
            )
        )

        response = self.get_json('/users/?service=abc')
        assert_that(
            response,
            has_entries(
                result=[]
            )
        )

    def test_filter_service_after_reenable(self):
        # проверим, что платный сервис доступен всем,
        # если его повторно включили до окончания триального периода
        not_ready_service = ServiceModel(self.meta_connection).create(
            client_id='some-client-id3',
            slug='abc',
            name='abc',
            ready_default=False,
            paid_by_license=True,
            trial_period_months=1,
        )

        # синхронизируем таблицу services
        UpdateServicesInShards().try_run()
        another_user = self.create_user()

        self.post_json('/services/%s/enable/' % not_ready_service['slug'], data=None)

        org_service = OrganizationServiceModel(self.main_connection).find(
            filter_data={
                'org_id': self.organization['id'],
                'service_id': not_ready_service['id'],
            },
            fields=['ready'],
            one=True,
        )
        assert_that(
            org_service['ready'],
            equal_to(False)
        )

        # сервис доступен всем
        response = self.get_json('/users/?fields=services')
        assert_that(
            response,
            has_entries(
                result=contains_inanyorder(
                    has_entries(
                        id=self.user['id'],
                        services=contains(
                            has_entries(
                                slug=not_ready_service['slug'],
                            )
                        )
                    ),
                    has_entries(
                        id=another_user['id'],
                        services=contains(
                            has_entries(
                                slug=not_ready_service['slug'],
                            )
                        )
                    )
                )
            )
        )

        response = self.get_json('/users/?service=abc')
        assert_that(
            response,
            has_entries(
                result=contains_inanyorder(
                    has_entries(
                        id=self.user['id']
                    ),
                    has_entries(
                        id=another_user['id']
                    )
                )
            )
        )

        # выключаем сервис
        self.post_json('/services/%s/disable/' % not_ready_service['slug'], data=None, expected_code=201)

        # включаем сервис второй раз
        self.post_json('/services/%s/enable/' % not_ready_service['slug'], data=None)

        # сервис доступен всем - все еще идет триальный период
        response = self.get_json('/users/?fields=services')
        assert_that(
            response,
            has_entries(
                result=contains_inanyorder(
                    has_entries(
                        id=self.user['id'],
                        services=contains(
                            has_entries(
                                slug=not_ready_service['slug'],
                            )
                        )
                    ),
                    has_entries(
                        id=another_user['id'],
                        services=contains(
                            has_entries(
                                slug=not_ready_service['slug'],
                            )
                        )
                    )
                )
            )
        )

        response = self.get_json('/users/?service=abc')
        assert_that(
            response,
            has_entries(
                result=contains_inanyorder(
                    has_entries(
                        id=self.user['id']
                    ),
                    has_entries(
                        id=another_user['id']
                    )
                )
            )
        )

    def test_ordering_by_name(self):
        org_id = self.organization['id']
        user_model = UserModel(self.main_connection)
        user_model.create(
            id=100,
            nickname='vasya',
            name={'last': {'ru': 'ddd'}, 'first': {'ru': 'Aab'}, 'middle': {'ru': 'H'}},
            email='vasya@ya.ru',
            gender='male',
            org_id=org_id,
        )
        user_model.create(
            id=500,
            nickname='petya',
            name={'last': {'en': 'ddd'}, 'first': {'en': 'Abc'}, 'middle': {'en': 'X'}},
            email='petya@ya.ru',
            gender='male',
            org_id=org_id,
        )
        user_model.create(
            id=300,
            nickname='kolya',
            name={'last': {'en': 'D e'}, 'first': {'en': 'Abch'}, 'middle': {'en': 'X'}},
            email='kolya@ya.ru',
            gender='male',
            org_id=org_id,
        )
        user_model.create(
            id=301,
            nickname='kolya1',
            name={'last': {'en': 'zer'}, 'first': {'en': 'Abc'}, 'middle': {'en': 'X'}},
            email='kolya1@ya.ru',
            gender='male',
            org_id=org_id,
        )
        user_model.create(
            id=200,
            nickname='sveta',
            name={'last': {'ru': 'Дабв'}, 'first': {'ru': 'Абв'}, 'middle': {'ru': 'А'}},
            email='sveta@ya.ru',
            gender='male',
            org_id=org_id,
        )
        user_model.create(
            id=400,
            nickname='tanya',
            name={'last': {'ru': 'деви'}, 'first': {'ru': 'Абв'}, 'middle': {'ru': 'Клмн'}},
            email='tanya@ya.ru',
            gender='male',
            org_id=org_id,
        )
        user_model.create(
            id=600,
            nickname='katya',
            name={'last': {'ru': 'Де ж'}, 'first': {'ru': 'Абв'}, 'middle': {'ru': 'Клмн'}},
            email='katya@ya.ru',
            gender='male',
            org_id=org_id,
        )

        data = self.get_json('/users/?fields=name,services&ordering=name')
        result = data.get('result')
        self.assertEqual(len(result), 8)
        self.assertEqual([r['id'] for r in result], [self.admin_uid, 100, 500, 300, 301, 200, 400, 600])

        data = self.get_json('/users/?fields=name,services&ordering=-name')
        result = data.get('result')
        self.assertEqual(len(result), 8)
        self.assertEqual([r['id'] for r in result], [self.admin_uid, 100, 500, 300, 301, 200, 400, 600][::-1])

    def test_ordering_by_name_with_filter(self):
        org_id = self.organization['id']
        user_model = UserModel(self.main_connection)
        user1 = user_model.create(
            id=100,
            nickname='vasya',
            name={'last': {'ru': 'ddd'}, 'first': {'ru': 'Aab'}, 'middle': {'ru': 'H'}},
            email='vasya@ya.ru',
            gender='male',
            org_id=org_id,
        )
        user2 = user_model.create(
            id=500,
            nickname='petya',
            name={'last': {'en': 'ddd'}, 'first': {'en': 'Abc'}, 'middle': {'en': 'X'}},
            email='petya@ya.ru',
            gender='male',
            org_id=org_id,
        )
        user_model.create(
            id=300,
            nickname='kolya',
            name={'last': {'en': 'D e'}, 'first': {'en': 'Abch'}, 'middle': {'en': 'X'}},
            email='kolya@ya.ru',
            gender='male',
            org_id=org_id,
        )
        group = self.create_group(
            members=[
                {'type': 'user', 'id': user1['id']},
                {'type': 'user', 'id': user2['id']}
            ]
        )
        data = self.get_json('/users/?fields=name,services&recursive_group_id={}&ordering=name'.format(group['id']))
        result = data.get('result')
        self.assertEqual(len(result), 2)
        self.assertEqual([r['id'] for r in result], [100, 500])

        data = self.get_json('/users/?fields=name,services&recursive_group_id={}&ordering=-name'.format(group['id']))
        result = data.get('result')
        self.assertEqual(len(result), 2)
        self.assertEqual([r['id'] for r in result], [500, 100])


class TestUserDetail__portal(BaseMixin, TestCase):
    api_version = 'v8'

    def setUp(self):
        super(TestUserDetail__portal, self).setUp()

        self.portal = create_organization(
            self.meta_connection,
            self.main_connection,
            label='portal_org'
        )['organization']
        self.org_id = self.portal['id']
        OrganizationModel(self.main_connection).update_one(
            self.org_id,
            {'organization_type': 'portal'}
        )
        self.existing_user = self.create_user(org_id=self.org_id)
        self.admin_uid = self.portal['admin_uid']

    def test_get(self):
        # проверяем, что возвращаем пользователя, если он есть, и досоздаем, если его нет
        response = self.get_json(
            '/users/%s/?fields=email' % self.existing_user['id'],
            headers=get_auth_headers(as_uid=self.admin_uid)
        )
        expected = {
            'id': self.existing_user['id'],
            'email': build_email(
                self.main_connection,
                self.existing_user['nickname'],
                self.org_id,
            )
        }
        assert_that(response, equal_to(expected))

        # если пользователь есть в паспорте, то создадим его и вернем
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.users.get_user_data_from_blackbox_by_uid') as mock_user_data:
            uid = 1130000011111111
            mock_user_data.return_value = {
                'aliases': [],
                'birth_date': '2000-01-01',
                'default_email': 'only_passport_user@khunafin.xyz',
                'first_name': 'user',
                'is_maillist': False,
                'language': 'ru',
                'last_name': 'user',
                'login': 'only_passport_user@khunafin.xyz',
                'sex': '1',
                'uid': uid,
                'org_ids': [self.org_id],
            }

            response = self.get_json(
                '/users/%s/?fields=email' % uid,
                headers=get_auth_headers(as_uid=self.admin_uid)
            )
            expected = {
                'id': uid,
                'email':  build_email(
                    self.main_connection,
                    'only_passport_user',
                    self.org_id,
                )
            }
            assert_that(response, equal_to(expected))

            # пользователь создался
            user = UserModel(self.main_connection)\
                .filter(id=uid)\
                .fields('org_id', 'nickname', 'gender', 'birthday')\
                .one()
            assert_that(
                user,
                equal_to({
                    'id': uid,
                    'org_id': self.org_id,
                    'nickname': 'only_passport_user',
                    'gender': 'male',
                    'birthday': datetime.datetime.strptime('2000-01-01', '%Y-%m-%d').date()
                })
            )

        # если пользователя нет в паспорте, то вернем 404
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.users.get_user_data_from_blackbox_by_uid') as mock_user_data:
            mock_user_data.return_value = {}
            response = self.get_json(
                '/users/%s/?fields=email' % 123,
                headers=get_auth_headers(as_uid=self.admin_uid),
                expected_code=404,
            )

    def test_dismiss(self):
        # увольнение пользователя
        result = self.patch_json(
            '/users/%s/' % self.existing_user['id'],
            data={'is_dismissed': True},
            headers=get_auth_headers(as_uid=self.admin_uid),
        )

        assert_that(
            UserDismissedModel(self.main_connection).get(self.existing_user['id']),
            has_entries(
                user_id=self.existing_user['id'],
                org_id=self.org_id,
            )
        )

        # увольняем несуществующего пользователя
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.users.get_user_data_from_blackbox_by_uid') as mock_user_data:
            uid = 1130000011111111
            mock_user_data.return_value = {
                'aliases': [],
                'birth_date': '2000-01-01',
                'default_email': 'only_passport_user@khunafin.xyz',
                'first_name': 'user',
                'is_maillist': False,
                'language': 'ru',
                'last_name': 'user',
                'login': 'only_passport_user@khunafin.xyz',
                'sex': '1',
                'uid': uid,
                'org_ids': [self.org_id],
            }

            result = self.patch_json(
                '/users/%s/' % uid,
                data={'is_dismissed': True},
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

            # пользователь создался
            action = ActionModel(self.main_connection).filter(org_id=self.org_id, name='user_add').fields('object').one()

            assert_that(
                action['object'],
                has_entries(
                    id=uid,
                    org_id=self.org_id,
                    nickname='only_passport_user',
                    gender='male',
                )
            )

            assert_that(
                UserDismissedModel(self.main_connection).get(uid),
                has_entries(
                    user_id=uid,
                    org_id=self.org_id,
                )
            )

        # если пользователя нет в паспорте, то вернем 404
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.users.get_user_data_from_blackbox_by_uid') as mock_user_data:
            mock_user_data.return_value = {}
            response = self.patch_json(
                '/users/%s/' % 123,
                data={'is_dismissed': True},
                headers=get_auth_headers(as_uid=self.admin_uid),
                expected_code=404,
            )

    def test_patch(self):
        # редактирование пользователя
        result = self.patch_json(
            '/users/%s/?fields=birthday' % self.existing_user['id'],
            data={'birthday': '2018-01-01'},
            headers=get_auth_headers(as_uid=self.admin_uid),
        )

        assert_that(
            result,
            has_entries(
                id=self.existing_user['id'],
                birthday='2018-01-01'
            ),
        )

        # редкатируем несуществующего пользователя
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.users.get_user_data_from_blackbox_by_uid') as mock_user_data:
            uid = 1130000011111111
            mock_user_data.return_value = {
                'aliases': [],
                'birth_date': '2000-01-01',
                'default_email': 'only_passport_user@khunafin.xyz',
                'first_name': 'user',
                'is_maillist': False,
                'language': 'ru',
                'last_name': 'user',
                'login': 'only_passport_user@khunafin.xyz',
                'sex': '1',
                'uid': uid,
                'org_ids': [self.org_id],
            }

            result = self.patch_json(
                '/users/%s/?fields=birthday' % uid,
                data={'birthday': '2018-01-01'},
                headers=get_auth_headers(as_uid=self.admin_uid),
            )

            assert_that(
                result,
                has_entries(
                    id=uid,
                    birthday='2018-01-01'
                ),
            )

            # пользователь создался
            action = ActionModel(self.main_connection).filter(org_id=self.org_id, name='user_add').fields('object').one()

            assert_that(
                action['object'],
                has_entries(
                    id=uid,
                    org_id=self.org_id,
                    nickname='only_passport_user',
                    gender='male',
                    birthday='2000-01-01',
                )
            )

            # информация о нем обновилась
            user = UserModel(self.main_connection).filter(id=uid).fields('org_id', 'nickname', 'birthday').one()
            assert_that(
                user,
                equal_to({
                    'id': uid,
                    'org_id': self.org_id,
                    'nickname': 'only_passport_user',
                    'birthday': datetime.datetime.strptime('2018-01-01', '%Y-%m-%d').date()
                })
            )

        # если пользователя нет в паспорте, то вернем 404
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.users.get_user_data_from_blackbox_by_uid') as mock_user_data:
            mock_user_data.return_value = {}
            response = self.patch_json(
                '/users/%s/' % 123,
                data={'birthday': '2018-01-01'},
                headers=get_auth_headers(as_uid=self.admin_uid),
                expected_code=404,
            )

    def test_get_other_org(self):
        # проверяем, что не пытаемся создать пользователя в организации, к котрой он не принадлежит

        with patch('intranet.yandex_directory.src.yandex_directory.core.views.users.get_user_data_from_blackbox_by_uid') as mock_user_data:
            uid = 1130000011111111
            mock_user_data.return_value = {
                'aliases': [],
                'birth_date': '2000-01-01',
                'default_email': 'only_passport_user@khunafin.xyz',
                'first_name': 'user',
                'is_maillist': False,
                'language': 'ru',
                'last_name': 'user',
                'login': 'only_passport_user@khunafin.xyz',
                'sex': '1',
                'uid': uid,
                'org_ids': [123456789],
            }

            response = self.get_json(
                '/users/%s/?fields=email' % uid,
                headers=get_auth_headers(as_uid=self.admin_uid),
                expected_code=404
            )

            # пользователя нет в базе
            assert_that(
                UserModel(self.main_connection).filter(id=uid).one(),
                equal_to(None)
            )


class TestUserByNicknameView(TestCase):
    api_version = 'v8'

    def setUp(self):
        super(TestUserByNicknameView, self).setUp()

        self.portal = create_organization(
            self.meta_connection,
            self.main_connection,
            label='portal_org'
        )['organization']
        self.org_id = self.portal['id']
        OrganizationModel(self.main_connection).update_one(
            self.org_id,
            {'organization_type': 'portal'}
        )
        self.another_user = self.create_user(
            org_id=self.org_id,
            nickname='test-1',
            email=build_email(self.main_connection, 'test-1', self.org_id),
        )
        self.existing_user = self.create_user(org_id=self.org_id)
        self.admin_uid = self.portal['admin_uid']


    def test_get(self):
        # проверяем, что возвращаем пользователя по nickname, если он есть, и досоздаем, если его нет
        response = self.get_json(
            '/users/nickname/%s/?fields=id,email' % self.existing_user['nickname'],
            headers=get_auth_headers(as_uid=self.admin_uid)
        )
        expected = {
            'id': self.existing_user['id'],
            'email': build_email(
                self.main_connection,
                self.existing_user['nickname'],
                self.org_id,
            )
        }
        assert_that(response, equal_to(expected))

        # если пользователь есть в паспорте, то создадим его и вернем
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.users.get_user_data_from_blackbox_by_login') as mock_user_data:
            uid = 1130000011111111
            nickname = 'only_passport_user'
            mock_user_data.return_value = {
                'aliases': [],
                'birth_date': '2000-01-01',
                'default_email': 'only_passport_user@khunafin.xyz',
                'first_name': 'user',
                'is_maillist': False,
                'language': 'ru',
                'last_name': 'user',
                'login': 'only_passport_user@khunafin.xyz',
                'sex': '1',
                'uid': uid,
                'org_ids': [self.org_id],
            }

            response = self.get_json(
                '/users/nickname/%s/?fields=id,email' % nickname,
                headers=get_auth_headers(as_uid=self.admin_uid)
            )
            expected = {
                'id': uid,
                'email':  build_email(
                    self.main_connection,
                    nickname,
                    self.org_id,
                )
            }
            assert_that(response, equal_to(expected))

            # пользователь создался
            user = UserModel(self.main_connection)\
                .filter(id=uid)\
                .fields('org_id', 'nickname', 'gender', 'birthday')\
                .one()
            assert_that(
                user,
                equal_to({
                    'id': uid,
                    'org_id': self.org_id,
                    'nickname': 'only_passport_user',
                    'gender': 'male',
                    'birthday': datetime.datetime.strptime('2000-01-01', '%Y-%m-%d').date()
                })
            )

        # если пользователя нет в паспорте, то вернем 404
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.users.get_user_data_from_blackbox_by_login') as mock_user_data:
            mock_user_data.return_value = {}
            response = self.get_json(
                '/users/nickname/%s/?fields=email' % '123',
                headers=get_auth_headers(as_uid=self.admin_uid),
                expected_code=404,
            )

    def test_get_by_alias(self):
        uid = 1130000011111222
        existing_nickname = 'hello_world'
        self.create_user(uid=uid, nickname=existing_nickname, org_id=self.org_id)
        # если пользователь есть в паспорте, то создадим его и вернем
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.users.get_user_data_from_blackbox_by_login') as mock_user_data:
            nickname = 'only_passport_user'
            mock_user_data.return_value = {
                'aliases': [],
                'birth_date': '2000-01-01',
                'default_email': 'only_passport_user@khunafin.xyz',
                'first_name': 'user',
                'is_maillist': False,
                'language': 'ru',
                'last_name': 'user',
                'login': 'only_passport_user@khunafin.xyz',
                'sex': '1',
                'uid': uid,
                'org_ids': [self.org_id],
            }

            response = self.get_json(
                '/users/nickname/%s/?fields=id,email' % nickname,
                headers=get_auth_headers(as_uid=self.admin_uid)
            )
            expected = {
                'id': uid,
                'email': build_email(
                    self.main_connection,
                    existing_nickname,
                    self.org_id,
                )
            }
            assert_that(response, equal_to(expected))

    def test_return_domain_account_with_feature_enabled(self):
        # фича MULTIORG включена
        set_feature_value_for_organization(
            self.meta_connection,
            self.org_id,
            MULTIORG,
            True,
        )
        # создаем портального пользователя в организации
        portal_user = self.create_portal_user(
            org_id=self.org_id,
            login='test-1',
            email='test-1@yandex.ru',
        )

        # проверяем, что у нас существуют два пользователя с одинаковыми никнеймами
        assert_that(
            UserModel(self.main_connection).filter(nickname='test-1').all(),
            has_length(2),
        )

        response = self.get_json(
            '/users/nickname/%s/?fields=id,email' % portal_user['nickname'],
            headers=get_auth_headers(as_uid=portal_user['id']),
        )

        # проверим, что выдаем пользователя с доменной учеткой
        assert_that(
            response,
            has_entries(
                id=self.another_user['id'],
                email=self.another_user['email'],
            )
        )


@patch.object(UserModel, 'is_enabled', Mock(return_value=True))
class TestUserDetail__patch_v8(BaseMixin, TestCase):
    api_version = 'v8'

    def setUp(self):
        super(TestUserDetail__patch_v8, self).setUp()

        self.name = {
            'first': {
                'ru': 'Владимир'
            },
            'last': {
                'ru': 'Путин'
            }
        }
        self.another_department = create_department(
            self.main_connection,
            org_id=self.organization['id'],
        )

        self.generic_group = create_group(
            self.main_connection,
            org_id=self.organization['id']
        )

        set_auth_uid(self.user['id'])

        self.common_user = self.create_user()

    def test_patch_simple_fields(self):
        # проверяем, что в ответе только те поля, которые изменились
        simple_fields = {
            'birthday': '2010-01-01',
            'name': self.name,
            'role': 'deputy_admin',
            'timezone': 'Asia/Jakarta',
            'language': 'ru',
            'is_admin': True,
        }

        for field, value in simple_fields.items():
            result = self.patch_json(
                '/users/{uid}/?fields={field}'.format(
                    uid=self.common_user['id'],
                    field=field,
                ),
                data={
                    field: value,
                },
                expected_code=200
            )
            assert_that(
                result,
                equal_to({
                    'id': self.common_user['id'],
                    field: ANY,
                })
            )

        multiple_fields = {
            'birthday': '2010-01-01',
            'name': self.name,
            'gender': 'female'
        }

        result = self.patch_json(
            '/users/{uid}/?fields={fields}'.format(
                uid=self.common_user['id'],
                fields=','.join(multiple_fields)
            ),
            data=multiple_fields,
            expected_code=200
        )
        assert_that(
            result,
            equal_to({
                'id': self.common_user['id'],
                'birthday': '2010-01-01',
                'name': {
                    'first': self.name['first']['ru'],
                    'last': self.name['last']['ru'],
                },
                'gender': 'female',
            })
        )
        # Но при этом в событии должен быть объект "пользователь"
        # со всеми простыми полями, не требующими запросов в базу.
        # Это важно, чтобы не сломать ноги себе и тем потребителям,
        # которые уже завязались на данные из событий.
        # https://st.yandex-team.ru/DIR-5828
        event = EventModel(self.main_connection) \
                .filter(
                    org_id=self.organization['id'],
                    name='user_property_changed',
                ) \
                .order_by('-id') \
                .one()

        object_fields = list(event['object'].keys())
        assert_that(
            object_fields,
            contains_inanyorder(*UserModel.simple_fields),
        )

    def test_patch_department(self):
        # Проверяем, что отдаются только поля про департамент и id пользователя,
        # при перемещении пользователя обновляется кеш групп, связанных с департаментов
        department1 = self.create_department()
        department2 = self.create_department(parent_id=department1['id'])
        user = self.create_user(department_id=department2['id'])
        group1 = self.create_group(
            members=[{
                'type': 'department',
                'id': user['department_id'],
            }]
        )
        group2 = self.create_group(
            members=[{
                'type': 'department',
                'id': self.another_department['id'],
            }]
        )
        response_data = self.patch_json(
            '/users/%s/' % user['id'],
            data={
                'department_id': self.another_department['id']
            },
            expected_code=200
        )

        # В ответе должен быть лишь только id
        assert_that(
            response_data,
            equal_to({
                'id': user['id'],
            })
        )

        users_group1 = UserGroupMembership(self.main_connection).find({'group_id': group1['id']})
        assert_that(users_group1, empty())
        users_group2 = UserGroupMembership(self.main_connection).find({'group_id': group2['id']})
        user_ids = [x['user_id'] for x in users_group2]
        assert_that(user_ids, contains(user['id']))


        # Проверим, что в событие попала информация про изменения отдела
        event = EventModel(self.main_connection) \
                .filter(
                    org_id=self.organization['id'],
                    name='user_property_changed',
                ) \
                .order_by('-id') \
                .one()

        assert_that(
            event['content']['diff']['departments'],
            contains(
                contains(
                    has_entries(id=1),
                    has_entries(id=department1['id']),
                    has_entries(id=department2['id']),
                ),
                # Цепочка отделов изменилась
                contains(
                    has_entries(id=self.another_department['id'])
                ),
            )
        )

        # NOTE: это поле пока в diff не попадает.
        #       похоже, что это ошибка, но пока просто зафиксируем
        #       это в тесте.
        assert 'department' not in event['content']['diff']


        # А теперь убедимся, что и для патча отдела в виде словаря
        # ручка тоже работает. А заодно проверим, что можно
        # указать дополнительные поля в урле.
        response_data = self.patch_json(
            '/users/%s/?fields=department.name' % user['id'],
            data={
                'department': {'id': department2['id']}
            },
            expected_code=200
        )

        assert_that(
            response_data,
            has_entries(
                id=user['id'],
                department=has_entries(
                    id=department2['id'],
                    # Так как это API v8 то в нём мы отдаём обычные строки
                    # а не интернационализированные.
                    name=department2['name']['ru'],
                ),
            ),
        )

    def test_patch_group(self):
        # Обновляем группу, проверяем, что кеши групп обновились
        parent_group = self.create_group(
            members=[{'type': 'group', 'id': self.generic_group['id']}])

        generic_group_membership = UserGroupMembership(self.main_connection).find(
            {'group_id': self.generic_group['id']}
        )
        assert_that(generic_group_membership, equal_to([]))
        parent_group__membership = UserGroupMembership(self.main_connection).find(
            {'group_id': parent_group['id']}
        )
        assert_that(parent_group__membership, equal_to([]))

        response_data = self.patch_json(
            '/users/%s/?fields=groups' % self.common_user['id'],
            data={
                'groups': [self.generic_group['id']]
            }
        )
        assert_that(
            response_data,
            equal_to({
                'id': self.common_user['id'],
                'groups': ANY,
            })
        )
        self.assertEqual(len(response_data['groups']), 2)
        self.assertEqual(response_data['groups'][0]['id'], self.generic_group['id'])
        generic_group_membership = UserGroupMembership(self.main_connection).find(
            {'group_id': self.generic_group['id']}
        )
        assert_that(
            generic_group_membership,
            contains_inanyorder(has_entries(
                'user_id',
                self.common_user['id']
            ))
        )
        parent_group__membership = UserGroupMembership(self.main_connection).find(
            {'group_id': parent_group['id']}
        )
        assert_that(
            parent_group__membership,
            contains_inanyorder(has_entries(
                'user_id',
                self.common_user['id']
            ))
        )

    def test_change_password_return_only_id(self):
        # при смене пароля возвращается только uid
        new_password = 'password'

        result = self.patch_json(
            '/users/%s/' % self.user['id'],
            data={
                'password': new_password
            },
            expected_code=200,
        )

        # Если у пользователя есть права на смену пароля - нужно сходить в паспорт и поменять его
        assert_called_once(
            self.mocked_passport.change_password,
            uid=self.user['id'],
            new_password=new_password,
            force_next_login_password_change=False,
        )

        assert_that(
            result,
            equal_to({
                'id': self.user['id'],
            })
        )

    def test_patch_with_fields(self):
        # проверяем, что в ответе только те поля, которые были явно указаны в запросе
        result = self.patch_json(
            '/users/%s/?fields=birthday' % self.common_user['id'],
            data={
                'gender': 'female',
            },
            expected_code=200
        )

        assert_that(
            result,
            equal_to({
                'id': self.common_user['id'],
                # gender мы не просили, поэтому его быть не должно
                # 'gender': 'female',
                'birthday': self.common_user['birthday']
            })
        )

        result = self.patch_json(
            '/users/%s/?fields=birthday,is_enabled,email' % self.common_user['id'],
            data={
                'name': self.name,
            },
            expected_code=200
        )

        assert_that(
            result,
            equal_to({
                'id': self.common_user['id'],
                'birthday': self.common_user['birthday'],
                'is_enabled': True,
                'email': self.common_user['email'],
            })
        )


class TestUserOuterDeputyAdminListView(TestCase):
    api_version = 'v8'

    def setUp(self):
        super(TestUserOuterDeputyAdminListView, self).setUp()
        self.outer_admin_uid, self.orgs, revision = create_outer_admin(
            self.meta_connection,
            self.main_connection,
            num_organizations=1
        )
        self.org_id = self.orgs[0]
        # хак, чтобы get_organization_admin_uid возвращал внешнего админа
        OrganizationModel(self.main_connection).update(
            {'admin_uid': self.outer_admin_uid},
            {'id': self.org_id}
        )
        self.outer_uid = 123
        self.nickname = 'hello.world'
        self.outer_admin_auth_headers = get_auth_headers(
            as_outer_admin={
                'id': self.outer_admin_uid, 'org_id': self.org_id
            }
        )

    def test_get_deputies_list(self):
        response = self.get_json(
            '/users/deputy/',
            headers=self.outer_admin_auth_headers
        )

        assert_that(
            response,
            equal_to({
                'deputies': []
            }),
        )

        deputy_admin = self.create_deputy_admin(uid=self.outer_uid, org_id=self.org_id, is_outer=True)
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.users.get_user_data_from_blackbox_by_uid') as mock_user_data:
            mock_user_data.return_value = {
                'aliases': [],
                'birth_date': '2000-01-01',
                'default_email': 'hello.world@ayndex.ru',
                'first_name': 'user',
                'is_maillist': False,
                'language': 'ru',
                'last_name': 'user',
                'login': 'hello-world',
                'sex': '1',
                'uid': self.outer_uid
            }
            response = self.get_json(
                '/users/deputy/',
                headers=self.outer_admin_auth_headers
            )

            assert_that(
                response,
                equal_to({
                    'deputies': [self.nickname]
                })
            )

    def test_add_deputy_admin(self):
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.users.get_user_id_from_passport_by_login') as mock_user_data:
            mock_user_data.return_value = self.outer_uid
            response = self.post_json(
                '/users/deputy/',
                data={'nickname': self.nickname},
                headers=self.outer_admin_auth_headers,
                expected_code=201,
            )

            assert_that(
                bool(UserMetaModel(self.meta_connection).get_outer_deputy_admins(
                    org_id=self.org_id,
                    uid=self.outer_uid
                )),
                equal_to(True)
            )

            action = ActionModel(self.main_connection).filter(
                name='organization_outer_deputy_add'
            ).one()
            assert_that(
                action['object'],
                has_entries(id=self.org_id)
            )

            # если попытаемся добавить второй раз, то ошибок не будет
            response = self.post_json(
                '/users/deputy/',
                data={'nickname': self.nickname},
                headers=self.outer_admin_auth_headers,
                expected_code=201,
            )

    def test_add_deputy_admin_with_errors(self):
        # внутренних пользоватлеей нельзя передавать в эту ручку
        common_user = self.create_user()
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.users.get_user_id_from_passport_by_login') as mock_user_data:
            mock_user_data.return_value = common_user['id']
            response = self.post_json(
                '/users/deputy/',
                data={'nickname': common_user['nickname']},
                headers=self.outer_admin_auth_headers,
                expected_code=403,
            )

        with patch('intranet.yandex_directory.src.yandex_directory.core.views.users.get_user_id_from_passport_by_login') as mock_user_data:
            mock_user_data.return_value = None
            response = self.post_json(
                '/users/deputy/',
                data={'nickname': 'dmitry'},
                headers=self.outer_admin_auth_headers,
                expected_code=404,
            )

        # админу нельзя становится замом
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.users.get_user_id_from_passport_by_login') as mock_user_data:
            mock_user_data.return_value = self.outer_admin_uid
            response = self.post_json(
                '/users/deputy/',
                data={'nickname': 'outer.org.admin'},
                headers=self.outer_admin_auth_headers,
                expected_code=403,
            )

    def test_delete_deputy_admin(self):
        deputy_admin = self.create_deputy_admin(uid=self.outer_uid, org_id=self.org_id, is_outer=True)
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.users.get_user_id_from_passport_by_login') as mock_user_data:
            mock_user_data.return_value = self.outer_uid

            response = self.delete_json(
                '/users/deputy/%s/' % self.nickname,
                headers=self.outer_admin_auth_headers,
                expected_code=204,
            )

            assert_that(
                bool(UserMetaModel(self.meta_connection).get_outer_deputy_admins(
                    org_id=self.org_id,
                    uid=self.outer_uid
                )),
                equal_to(False)
            )

            action = ActionModel(self.main_connection).filter(
                name='organization_outer_deputy_delete'
            ).one()
            assert_that(
                action['object'],
                has_entries(id=self.org_id)
            )

        # нельзя удалять тех, кто не является замом
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.users.get_user_id_from_passport_by_login') as mock_user_data:
            mock_user_data.return_value = 12345678

            response = self.delete_json(
                '/users/deputy/%s/' % 'admin',
                headers=self.outer_admin_auth_headers,
                expected_code=403,
            )


class TestUserTypeView(TestOrganizationWithoutDomainMixin, TestCase):
    def setUp(self):
        super(TestUserTypeView, self).setUp()
        self.outer_admin_uid, self.orgs, _ = create_outer_admin(
            self.meta_connection,
            self.main_connection,
            num_organizations=1
        )
        self.org_id = self.orgs[0]

    def test_get_outer(self):
        # Внешний админ
        headers = get_auth_headers(
            as_outer_admin={
                'id': self.outer_admin_uid, 'org_id': self.org_id,
            }
        )
        response = self.get_json(
            '/users/type/',
            headers=headers,
        )
        assert_that(
            response,
            equal_to({'internal': False})
        )

    def test_get_deputy(self):
        # Заместитель админа
        outer_deputy_admin_id = self.create_deputy_admin(org_id=self.org_id)['id']
        headers = get_auth_headers(
            as_outer_admin={
                'id': outer_deputy_admin_id, 'org_id': self.org_id,
            }
        )
        response = self.get_json(
            '/users/type/',
            headers=headers,
        )
        assert_that(
            response,
            equal_to({'internal': False})
        )

        inner_deputy_admin_id = self.create_deputy_admin(org_id=self.org_id, is_outer=False)['id']
        headers = get_auth_headers(
            as_outer_admin={
                'id': inner_deputy_admin_id, 'org_id': self.org_id,
            }
        )
        response = self.get_json(
            '/users/type/',
            headers=headers,
        )
        assert_that(
            response,
            equal_to({'internal': True})
        )

    def test_get_inner_admin(self):
        # Админ Яндекс.организации - uid внешний, но у нас в базе помечен как inner_user
        headers = get_auth_headers(as_uid=self.yandex_admin['id'])
        response = self.get_json(
            '/users/type/',
            headers=headers,
        )
        assert_that(
            response,
            equal_to({'internal': True})
        )

    def test_get_inner_user(self):
        # Пользователь Яндекс.организации - uid внешний, но у нас в базе помечен как inner_user
        headers = get_auth_headers(as_uid=self.yandex_user['id'])
        response = self.get_json(
            '/users/type/',
            headers=headers,
        )
        assert_that(
            response,
            equal_to({'internal': True})
        )


class TestUserList__get6(BaseMixin, TestCase):
    api_version = 'v6'
    entity_list_url = '/v6/users/'

    def setUp(self):
        super(TestUserList__get6, self).setUp()

        # без лицензии
        self.user1 = self.create_user(
            department_id=self.department['id'],
            nickname='user1',
            groups=[self.groups['admins']['id']],
        )

        # с персональной лицензией
        self.user2 = self.create_user(
            department_id=self.department['id'],
            nickname='user2',
        )

        self.department_one = self.create_department()
        # с лицензией на департамент
        self.user3 = self.create_user(
            department_id=self.department_one['id'],
            nickname='user3',
        )
        # с лицензией на департамент и персональной лицензией
        self.user4 = self.create_user(
            department_id=self.department_one['id'],
            nickname='user4',
        )

        self.group_one = self.create_group(label='group', org_id=self.organization['id'])
        # с лицензией на группу
        self.user5 = self.create_user(
            department_id=self.department['id'],
            nickname='user5',
            groups=[self.group_one['id']],
        )
        # с лицензией на группу и персональной лицензией
        self.user6 = self.create_user(
            department_id=self.department['id'],
            nickname='user6',
            groups=[self.group_one['id']],
        )

        # с лицензией на группу и департамент
        self.user7 = self.create_user(
            department_id=self.department_one['id'],
            nickname='user7',
            groups=[self.group_one['id']],
        )

        # с лицензией на группу, департамент и персональной лицензией
        self.user8 = self.create_user(
            department_id=self.department_one['id'],
            nickname='user8',
            groups=[self.group_one['id']],
        )

        for conn in [self.meta_connection, self.main_connection]:
            ServiceModel(conn).create(
                slug='tracker',
                name='Yandex.Tracker',
                client_id='test_client_id_tracker',
                paid_by_license=True,
                trial_period_months=2,
                id=99999,
            )
        service = enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            'tracker',
        )
        self.create_licenses_for_service(
            service_id=service['id'],
            user_ids=[self.user2['id'], self.user4['id'], self.user6['id'], self.user8['id']],
            department_ids=[self.department_one['id']],
            group_ids=[self.group_one['id']],
            org_id=self.organization['id'],
        )

    def test_get_with_tracker_licenses(self):
        response_data = self.get_json(
            '/users/?&fields=tracker_licenses,tracker_licenses.via_department_id,tracker_licenses.via_group_id',
            expected_code=200
        )
        assert_that(
            response_data['result'],
            contains_inanyorder(
                {'id': self.user['id'], 'tracker_licenses': []},
                {'id': self.user1['id'], 'tracker_licenses': []},
                {'id': self.user2['id'], 'tracker_licenses': [
                    {'via_department_id': None, 'via_group_id': None, 'user_id': self.user2['id']}
                ]},
                {'id': self.user3['id'], 'tracker_licenses': [
                    {'via_department_id': self.department_one['id'], 'via_group_id': None, 'user_id': self.user3['id']}
                ]},
                {'id': self.user4['id'], 'tracker_licenses': [
                    {'via_department_id': None, 'via_group_id': None, 'user_id': self.user4['id']},
                    {'via_department_id': self.department_one['id'], 'via_group_id': None, 'user_id': self.user4['id']}
                ]},
                {'id': self.user5['id'], 'tracker_licenses': [
                    {'via_department_id': None, 'via_group_id': self.group_one['id'], 'user_id': self.user5['id']}
                ]},
                {'id': self.user6['id'], 'tracker_licenses': [
                    {'via_department_id': None, 'via_group_id': None, 'user_id': self.user6['id']},
                    {'via_department_id': None, 'via_group_id': self.group_one['id'], 'user_id': self.user6['id']}
                ]},
                {'id': self.user7['id'], 'tracker_licenses': [
                    {'via_department_id': self.department_one['id'], 'via_group_id': None, 'user_id': self.user7['id']},
                    {'via_department_id': None, 'via_group_id': self.group_one['id'], 'user_id': self.user7['id']},
                ]},
                {'id': self.user8['id'], 'tracker_licenses': [
                    {'via_department_id': None, 'via_group_id': None, 'user_id': self.user8['id']},
                    {'via_department_id': self.department_one['id'], 'via_group_id': None, 'user_id': self.user8['id']},
                    {'via_department_id': None, 'via_group_id': self.group_one['id'], 'user_id': self.user8['id']},
                ]}
            )
        )

    def test_get_with_tracker_licenses_filter(self):
        response_data = self.get_json(
            '/users/?tracker_licenses=1&fields=tracker_licenses,tracker_licenses.via_department_id,'
            'tracker_licenses.via_group_id',
            expected_code=200
        )
        assert_that(
            response_data['result'],
            contains_inanyorder(
                {'id': self.user2['id'], 'tracker_licenses': [
                    {'via_department_id': None, 'via_group_id': None, 'user_id': self.user2['id']}
                ]},
                {'id': self.user3['id'], 'tracker_licenses': [
                    {'via_department_id': self.department_one['id'], 'via_group_id': None, 'user_id': self.user3['id']}
                ]},
                {'id': self.user4['id'], 'tracker_licenses': [
                    {'via_department_id': None, 'via_group_id': None, 'user_id': self.user4['id']},
                    {'via_department_id': self.department_one['id'], 'via_group_id': None, 'user_id': self.user4['id']}
                ]},
                {'id': self.user5['id'], 'tracker_licenses': [
                    {'via_department_id': None, 'via_group_id': self.group_one['id'], 'user_id': self.user5['id']}
                ]},
                {'id': self.user6['id'], 'tracker_licenses': [
                    {'via_department_id': None, 'via_group_id': None, 'user_id': self.user6['id']},
                    {'via_department_id': None, 'via_group_id': self.group_one['id'], 'user_id': self.user6['id']}
                ]},
                {'id': self.user7['id'], 'tracker_licenses': [
                    {'via_department_id': self.department_one['id'], 'via_group_id': None, 'user_id': self.user7['id']},
                    {'via_department_id': None, 'via_group_id': self.group_one['id'], 'user_id': self.user7['id']},
                ]},
                {'id': self.user8['id'], 'tracker_licenses': [
                    {'via_department_id': None, 'via_group_id': None, 'user_id': self.user8['id']},
                    {'via_department_id': self.department_one['id'], 'via_group_id': None, 'user_id': self.user8['id']},
                    {'via_department_id': None, 'via_group_id': self.group_one['id'], 'user_id': self.user8['id']},
                ]}
            )
        )

        assert_that(
            response_data['result'],
            is_not(
                contains_inanyorder(
                    {'id': self.user['id'], 'tracker_licenses': []},
                    {'id': self.user1['id'], 'tracker_licenses': []},
                )
            )
        )

    def test_get_with_tracker_licenses_ordered(self):
        response_data = self.get_json(
            '/users/?ordering=tracker_licenses&fields=tracker_licenses,tracker_licenses.via_department_id,'
            'tracker_licenses.via_group_id',
            expected_code=200
        )
        assert_that(
            response_data['result'][0],
            has_entries(
                id=self.user2['id']
            )
        )
        assert_that(
            response_data['result'][1],
            has_entries(
                id=self.user3['id']
            )
        )
        assert_that(
            response_data['result'][2],
            has_entries(
                id=self.user4['id']
            )
        )
        assert_that(
            response_data['result'][3],
            has_entries(
                id=self.user5['id']
            )
        )
        assert_that(
            response_data['result'][4],
            has_entries(
                id=self.user6['id']
            )
        )
        assert_that(
            response_data['result'][5],
            has_entries(
                id=self.user7['id']
            )
        )
        assert_that(
            response_data['result'][6],
            has_entries(
                id=self.user8['id']
            )
        )
        assert_that(
            response_data['result'][7],
            has_entries(
                id=self.user1['id']
            )
        )
        assert_that(
            response_data['result'][8],
            has_entries(
                id=self.user['id']
            )
        )


class TestCloudBillingAccountParamForTracker(TestCase):
    def test_put_without_parameter(self):
        tracker = ServiceModel(self.meta_connection).create(
            client_id='some-client-id1',
            slug='tracker',
            name='tracker',
            robot_required=False,
            trial_period_months=0,
            paid_by_license=True,
            ready_default=True,
        )
        users = []
        for i in range(6):
            users.append(self.create_user())
        self.post_json('/services/%s/enable/' % tracker['slug'], data=None)

        self.put_json(
            '/subscription/services/%s/licenses/' % tracker['slug'],
            data=[
                {'type': 'user', 'id': users[0]['id']},
                {'type': 'user', 'id': users[1]['id']},
                {'type': 'user', 'id': users[2]['id']},
                {'type': 'user', 'id': users[3]['id']},
                {'type': 'user', 'id': users[4]['id']},
                {'type': 'user', 'id': users[5]['id']},
            ],
            headers=get_auth_headers(as_uid=self.admin_uid),
            expected_code=422,
        )

    def test_put_with_parameter(self):
        tracker = ServiceModel(self.meta_connection).create(
            client_id='some-client-id1',
            slug='tracker',
            name='tracker',
            robot_required=False,
            trial_period_months=0,
            paid_by_license=True,
            ready_default=True,
        )
        users = []
        for i in range(6):
            users.append(self.create_user())
        self.post_json('/services/%s/enable/' % tracker['slug'], data=None)

        self.put_json(
            '/subscription/services/%s/licenses/?cloud_billing_account=1' % tracker['slug'],
            data=[
                {'type': 'user', 'id': users[0]['id']},
                {'type': 'user', 'id': users[1]['id']},
                {'type': 'user', 'id': users[2]['id']},
                {'type': 'user', 'id': users[3]['id']},
                {'type': 'user', 'id': users[4]['id']},
                {'type': 'user', 'id': users[5]['id']},
            ],
            headers=get_auth_headers(as_uid=self.admin_uid),
            expected_code=200,
        )

    def test_post_without_parameter(self):
        tracker = ServiceModel(self.meta_connection).create(
            client_id='some-client-id1',
            slug='tracker',
            name='tracker',
            robot_required=False,
            trial_period_months=0,
            paid_by_license=True,
            ready_default=True,
        )

        users = []
        for i in range(6):
            users.append(self.create_user())
        self.post_json('/services/%s/enable/' % tracker['slug'], data=None)

        for i in range(5):
            self.post_json(
                '/subscription/services/%s/licenses/' % tracker['slug'],
                data={'type': 'user', 'id': users[i]['id']},
                headers=get_auth_headers(as_uid=self.admin_uid),
                expected_code=201,
            )

        self.post_json(
            '/subscription/services/%s/licenses/' % tracker['slug'],
            data={'type': 'user', 'id': users[5]['id']},
            headers=get_auth_headers(as_uid=self.admin_uid),
            expected_code=422,
        )

    def test_post_wit_parameter(self):
        tracker = ServiceModel(self.meta_connection).create(
            client_id='some-client-id1',
            slug='tracker',
            name='tracker',
            robot_required=False,
            trial_period_months=0,
            paid_by_license=True,
            ready_default=True,
        )

        users = []
        for i in range(6):
            users.append(self.create_user())
        self.post_json('/services/%s/enable/' % tracker['slug'], data=None)

        for i in range(5):
            self.post_json(
                '/subscription/services/%s/licenses/' % tracker['slug'],
                data={'type': 'user', 'id': users[i]['id']},
                headers=get_auth_headers(as_uid=self.admin_uid),
                expected_code=201,
            )

        self.post_json(
            '/subscription/services/%s/licenses/?cloud_billing_account=1' % tracker['slug'],
            data={'type': 'user', 'id': users[5]['id']},
            headers=get_auth_headers(as_uid=self.admin_uid),
            expected_code=201,
        )


class TestCloudEvents(BaseMixin, TestCase):
    api_version = 'v6'

    def setUp(self):
        super().setUp()
        self.cloud_org = self.create_organization(
            label='hello',
            cloud_org_id='org-cloud-id',
        )

    def test_create_user(self):
        data = {
            'event_type': 'user_added',
            'cloud_org_id': 'org-cloud-id',
            'object_id': 'org-user-id',
        }

        with patch('intranet.yandex_directory.src.yandex_directory.connect_services.cloud.grpc'
                   '.client.GrpcCloudClient.list_organizations') as list_organizations, \
            patch('intranet.yandex_directory.src.yandex_directory.app.cloud_blackbox_instance') \
                as mock_cloud_blackbox_instance, \
            patch('intranet.yandex_directory.src.yandex_directory.core.cloud.utils.MessageToDict', MockToDict):

            list_organizations_response = self.get_dict_object({
                'next_page_token': None,
                'organizations': [
                    {
                        'id': 'org-cloud-id',
                        'name': 'test',
                        'description': 'smth desc',
                    },
                    {
                        'id': 'smth-2',
                        'name': 'test-1',
                        'description': 'smth desc-1',
                    }
                ]
            })
            list_organizations.return_value = list_organizations_response
            mock_cloud_blackbox_instance.userinfo.return_value = {
                'uid': 9000000000000100,
                'claims': {
                    'given_name': 'user given name',
                    'family_name': 'user family name',
                    'preferred_username': 'username',
                    'email': 'username@example.com'
                }
            }
            self.post_json('/cloud/event/', data=data, expected_code=201)
            user = UserModel(self.main_connection).get(user_id=9000000000000100)
            assert user['email'] == 'username@example.com'
            assert user['cloud_uid'] == 'org-user-id'
            assert user['is_dismissed'] is False

    def test_dismiss_user(self):
        data = {
            'event_type': 'user_added',
            'cloud_org_id': 'org-cloud-id',
            'object_id': 'org-user-id',
        }
        UserMetaModel(self.meta_connection).create(id=9000000000000100, cloud_uid='world', org_id=self.cloud_org['id'])
        user_model = UserModel(self.main_connection)
        user_model.create(
            id=9000000000000100,
            nickname='username',
            name={'last': {'ru': 'ddd'}, 'first': {'ru': 'Aab'}, 'middle': {'ru': 'H'}},
            email='username@example.com',
            gender='male',
            org_id=self.cloud_org['id'],
            cloud_uid='org-user-id'
        )

        with patch('intranet.yandex_directory.src.yandex_directory.connect_services.cloud.grpc'
                   '.client.GrpcCloudClient.list_organizations') as list_organizations, \
            patch('intranet.yandex_directory.src.yandex_directory.app.cloud_blackbox_instance') \
                as mock_cloud_blackbox_instance, \
            patch('intranet.yandex_directory.src.yandex_directory.core.cloud.utils.MessageToDict', MockToDict):

            list_organizations_response = self.get_dict_object({
                'next_page_token': None,
                'organizations': [
                    {
                        'id': 'smth-2',
                        'name': 'test-1',
                        'description': 'smth desc-1',
                    }
                ]
            })
            list_organizations.return_value = list_organizations_response
            mock_cloud_blackbox_instance.userinfo.return_value = {
                'uid': 9000000000000100,
                'claims': {
                    'given_name': 'user given name',
                    'family_name': 'user family name',
                    'preferred_username': 'username',
                    'email': 'username@example.com'
                }
            }
            self.post_json('/cloud/event/', data=data, expected_code=201)
            dismissed = UserDismissedModel(self.main_connection).get(
                user_id=9000000000000100,
                org_id=self.cloud_org['id']
            )
            assert dismissed is not None
