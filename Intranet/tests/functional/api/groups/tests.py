# -*- coding: utf-8 -*-
import json
import random
from typing import List

from hamcrest import (
    assert_that,
    contains_inanyorder,
    contains,
    has_entries,
    is_,
    not_none,
    only_contains,
    empty,
    equal_to,
    has_length,
    greater_than_or_equal_to,
    has_key,
    has_item,
    not_,
)
from unittest.mock import (
    Mock,
    patch,
)
import pytest

from testutils import (
    assert_that_not_found,
    get_auth_headers,
    PaginationTestsMixin,
    TestYandexTeamOrgMixin,
    check_response,
    create_organization,
    create_group,
    create_department,
    TestCase,
    set_auth_uid,
    scopes,
    assert_called_once,
    override_settings,
    oauth_client,
    get_oauth_headers,
    TestOrganizationWithoutDomainMixin,
    assert_not_called,
)
from intranet.yandex_directory.src.yandex_directory.auth.scopes import scope
from intranet.yandex_directory.src.yandex_directory.common.models.types import (
    TYPE_GROUP,
    TYPE_USER,
    TYPE_DEPARTMENT,
)
from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.core.actions import action
from intranet.yandex_directory.src.yandex_directory.core.events import event
from intranet.yandex_directory.src.yandex_directory.core.models.action import ActionModel
from intranet.yandex_directory.src.yandex_directory.core.models.event import EventModel
from intranet.yandex_directory.src.yandex_directory.core.models.group import GroupModel, UserGroupMembership
from intranet.yandex_directory.src.yandex_directory.core.models.resource import ResourceModel
from intranet.yandex_directory.src.yandex_directory.core.models.service import ServiceModel, enable_service
from intranet.yandex_directory.src.yandex_directory.core.models.resource import (
    ResourceRelationModel,
)
from intranet.yandex_directory.src.yandex_directory.core.models import OrganizationModel
from intranet.yandex_directory.src.yandex_directory.core.models.user import (
    UserModel
)
from intranet.yandex_directory.src.yandex_directory.core.utils import (
    build_organization_domain,
    build_email,
    prepare_user,
    prepare_group,
    get_organization_admin_uid,
    prepare_group_member_for_members_view,
    only_ids,
)
from intranet.yandex_directory.src.yandex_directory.core.features import (
    MULTIORG,
    is_feature_enabled,
    set_feature_value_for_organization,
)
from intranet.yandex_directory.src.yandex_directory.passport.exceptions import (
    AliasNotFound,
    LoginLong,
)

GROUP_NAME = {
    'ru': 'Группа',
    'en': 'Group',
}
GROUP_DESCRIPTION = {
    'ru': 'Описание группы',
    'en': 'Group description',
}
GROUP_PRIVATE_FIELDS = ['name_plain', 'description_plain']


class TestGroupList__get(PaginationTestsMixin, TestCase):
    entity_list_url = '/groups/'
    entity_model = GroupModel

    def create_entity(self):
        self.entity_counter += 1
        return self.create_group()

    def prepare_entity_for_api_response(self, entity):
        return prepare_group(self.main_connection, entity, api_version=1)

    def test_simple(self):
        # Проверяем, что на выдаче ручки /groups/ получим те же данные, что сохранили.

        group_model = GroupModel(self.main_connection)

        for i in range(2):
            name = dict(GROUP_NAME)
            name['ru'] += ' ' + str(i + 1)
            name['en'] += ' ' + str(i + 1)
            group_model.create(
                name=name,
                org_id=self.organization['id'],
                description=GROUP_DESCRIPTION,
                label='group{0}'.format(i),
            )
        data = self.get_json('/groups/')['result']
        self.assertEqual(len(data), group_model.count())
        assert_that(
            list(data[0].keys()),
            contains_inanyorder(
                'type',
                'id',
                'name',
                'description',
                'members',
                'author',
                'email',
                'created',
                'label',
                'members_count',
                'external_id',
                'maillist_type',
                'uid',
            )
        )
        expected = contains_inanyorder(
            has_entries(name=has_entries(en='Head of department "All employees"')),
            has_entries(name=has_entries(en='Organization administrator')),
            has_entries(name=has_entries(en='Group 1')),
            has_entries(name=has_entries(en='Group 2')),
            has_entries(name=has_entries(en='Organization robots')),
            has_entries(name=has_entries(en='Organization deputy administrators')),
        )
        assert_that(data, expected)

    def test_get_tracker_license(self):
        group_users = [{'type': 'user', 'id': self.create_user()['id']} for _ in range(10)]
        group = GroupModel(self.main_connection).create(
            org_id=self.organization['id'],
            name={
                'ru': 'Group'
            },
            members=group_users
        )
        group_2 = GroupModel(self.main_connection).create(
            org_id=self.organization['id'],
            name={
                'ru': 'Group2'
            },
            members=group_users
        )
        service = ServiceModel(self.meta_connection).create(
            slug='tracker',
            name='tracker',
            paid_by_license=True,
        )

        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            service['slug'],
        )
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
            '/subscription/services/%s/licenses/' % service['slug'],
            data=[
                {
                    'type': 'group',
                    'id': group['id']
                },
            ],
        )
        self.api_version = 'v6'
        data = self.get_json('/groups/?fields=tracker_license,org_id&ordering=tracker_license')
        group_data = [i for i in data['result'] if i['id'] == group['id']][0]
        assert group_data['tracker_license'] is True
        group2_data = [i for i in data['result'] if i['id'] == group_2['id']][0]
        assert group2_data['tracker_license'] is False

    def test_get_after_delete(self):
        """Проверяем, что на выдаче ручки /groups/ получим те же данные, что сохранили.
        """
        for i in range(2):
            self.create_group()

        group_for_delete = self.create_group()
        group_model = GroupModel(self.main_connection)

        group_model.delete({
            'id': group_for_delete['id'],
            'org_id': group_for_delete['org_id']
        })
        response = self.get_json('/groups/')
        data = response['result']
        total = response['total']
        self.assertEqual(len(data), group_model.count())
        self.assertEqual(len(data), total)

    def test_show_items_of_current_user_organization(self):
        another_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']

        group = GroupModel(self.main_connection).create(
            name=GROUP_NAME,
            org_id=another_organization['id']
        )

        data = self.get_json('/groups/')['result']
        assert len(data) > 0
        assert group['id'] not in only_ids(data)

    def test_show_items_to_internal_service_by_org_id(self):
        another_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']

        group = GroupModel(self.main_connection).create(
            name=GROUP_NAME,
            org_id=another_organization['id']
        )

        auth_headers = get_auth_headers(as_org=self.organization['id'])
        data = self.get_json('/groups/', headers=auth_headers)['result']
        assert len(data) > 0
        assert group['id'] not in only_ids(data)


    def test_filter_by_id(self):
        group_one = create_group(
            self.main_connection,
            org_id=self.organization['id'],
        )
        group_two = create_group(
            self.main_connection,
            org_id=self.organization['id']
        )
        create_group(
            self.main_connection,
            org_id=self.organization['id']
        )

        ids = [
            group_one['id'],
            group_two['id'],
        ]

        response = self.client.get(
            '/groups/',
            headers=get_auth_headers(),
            query_string={
                'id': ','.join(map(str, ids))
            }
        )
        response_data = json.loads(response.data).get('result')
        self.assertEqual(len(response_data), len(ids))

        response_ids = [i['id'] for i in response_data]
        self.assertEqual(
            sorted(response_ids),
            sorted(ids)
        )

    def test_filter_by_uid(self):
        group_one = create_group(
            self.main_connection,
            org_id=self.organization['id'],
            uid=3
        )
        group_two = create_group(
            self.main_connection,
            org_id=self.organization['id']
        )
        create_group(
            self.main_connection,
            org_id=self.organization['id']
        )

        uids = [
            group_one['uid'],
        ]

        response = self.client.get(
            '/groups/',
            headers=get_auth_headers(),
            query_string={
                'uid': ','.join(map(str, uids))
            }
        )
        response_data = json.loads(response.data).get('result')
        self.assertEqual(len(response_data), len(uids))

        response_ids = [i['uid'] for i in response_data]
        self.assertEqual(
            sorted(response_ids),
            sorted(uids)
        )

    @override_settings(INTERNAL=False)
    def test_error_filter_by_uid_not_internal(self):

        group_one = create_group(
            self.main_connection,
            org_id=self.organization['id'],
            uid=3
        )
        group_two = create_group(
            self.main_connection,
            org_id=self.organization['id']
        )
        create_group(
            self.main_connection,
            org_id=self.organization['id']
        )

        uids = [
            group_one['uid'],
        ]
        with oauth_client(client_id="kek", uid=self.user['id'], scopes=[scope.read_groups]):
            response = self.get_json(
                '/groups/',
                headers=get_oauth_headers(),
                query_string={
                    'uid': ','.join(map(str, uids))
                }
            )
        assert(len(response) > 1)

    def test_filter_by_type(self):
        generic_group = create_group(
            self.main_connection,
            org_id=self.organization['id'],
            type='generic'
        )
        create_group(
            self.main_connection,
            org_id=self.organization['id'],
            type='organization_admin'
        )

        response = self.client.get(
            '/groups/',
            headers=get_auth_headers(),
            query_string={
                'type': 'generic'
            }
        )
        response_data = json.loads(response.data).get('result')
        self.assertEqual(len(response_data), 1)

        response_ids = [i['id'] for i in response_data]
        self.assertEqual(response_ids, [generic_group['id']])

    def test_filter_my_own_groups(self):
        # проверяем, что можно получить все группы в которых я админ
        org_id = self.organization['id']
        uid = self.user['id']

        g1 = create_group(
            self.main_connection,
            org_id=org_id,
            type='generic', admins=[uid]
        )
        g2 = create_group(
            self.main_connection,
            org_id=org_id,
            type='generic',
            admins=[uid]
        )
        create_group(
            self.main_connection,
            org_id=org_id,
            type='generic'
        )

        data = self.get_json(
            '/groups/',
            query_string={
                'type': 'generic',
                'admin_uid': uid,
            }
        )['result']

        assert_that(
            data,
            contains_inanyorder(
                has_entries(id=g1['id']),
                has_entries(id=g2['id']),
            )
        )

    def test_get_department_groups(self):
        # Создаём группы, в которые будет входить департамент и проверяем, что
        # в в ответе на запрос пришли группы, только содержащие этот департамент, учитывая другой фильтр по типу.
        org_id = self.organization['id']
        group1 = create_group(
            self.main_connection,
            org_id=org_id
        )
        group2 = create_group(
            self.main_connection,
            org_id=org_id
        )
        # Третья группа будет иметь тип 'organization_admin'.
        group3 = GroupModel(self.main_connection).get_or_create_admin_group(org_id)
        # Четвёртая не будет иметь департамента в участниках.
        group4 = create_group(
            self.main_connection,
            label='do-not-have-department',
            org_id=org_id
        )

        department = create_department(
            self.main_connection,
            org_id=org_id
        )

        def add_to_group(group):
            GroupModel(self.main_connection).update_one(
                org_id=org_id,
                group_id=group['id'],
                data={
                    'members': [
                        {
                            'type': 'department',
                            'id': department['id']
                        }
                    ]
                }
            )

        # добавляем департамент в группы
        add_to_group(group1)
        add_to_group(group2)
        add_to_group(group3)

        # Проверяем наличие только первых двух групп, так как третья отсутствует из-за фильтра по типу,
        # а четвёртая не содержит нужного департамента.
        headers = get_auth_headers(as_org=self.organization['id'])
        response_data = self.get_json(
            '/groups/',
            query_string={
                'type': 'generic',
                'department_id': department['id']
            }
        )['result']

        assert_that(
            response_data,
            only_contains(
                has_entries(id=group1['id']),
                has_entries(id=group2['id']),
            )
        )

    def test_get_department_without_groups(self):
        # Проверим, что если департамент не состоит ни в одной группе, то вернётся пустой список.
        org_id = self.organization['id']
        group1 = create_group(
            self.main_connection,
            org_id=org_id
        )
        group2 = create_group(
            self.main_connection,
            org_id=org_id
        )

        department = create_department(
            self.main_connection,
            org_id=org_id
        )

        headers = get_auth_headers(as_org=self.organization['id'])
        response_data = self.get_json(
            '/groups/',
            query_string={
                'department_id': department['id']
            }
        )['result']

        assert_that(
            response_data,
            empty()
        )

    def test_ordering_by_name(self):
        group_model = GroupModel(self.main_connection)
        group_model.create(
            id=20,
            name={'en': 'Ac group'},
            org_id=self.organization['id']
        )
        group_model.create(
            id=90,
            name={'en': 'Ab group'},
            org_id=self.organization['id']
        )
        group_model.create(
            id=30,
            name={'en': 'a group'},
            org_id=self.organization['id'],
        )
        group_model.create(
            id=40,
            name={'en': 'b group'},
            org_id=self.organization['id'],
        )

        data = self.get_json('/groups/?ordering=name')
        result = data.get('result')
        self.assertEqual(len(result), 8)
        self.assertEqual([r['id'] for r in result], [90, 20, 30, 40, 2, 3, 4, 1])

        data = self.get_json('/groups/?ordering=-name')
        result = data.get('result')
        self.assertEqual(len(result), 8)
        self.assertEqual([r['id'] for r in result], [90, 20, 30, 40, 2, 3, 4, 1][::-1])


class TestGroupList__get_6(PaginationTestsMixin, TestCase):
    entity_list_url = '/groups/'
    entity_model = GroupModel
    api_version = 'v6'

    def create_entity(self):
        self.entity_counter += 1
        return self.create_group()

    def prepare_entity_for_api_response(self, entity):
        return prepare_group(self.main_connection, entity, api_version=6)

    def test_fields_returned_by_list_view(self):
        # Проверяем, что ручка возвращает все перечисленные поля.
        fields = list(GroupModel.all_fields)
        for field in GROUP_PRIVATE_FIELDS:
            fields.remove(field)
        # В этом тесте запрашиваются одновременно author и author_id.
        # Так как будет два поля с одинаковым значением, то в prepare_group
        # поле author_id не будет добавлено в response.
        fields.remove('author_id')
        headers = get_auth_headers()
        response = self.get_json(
            '/groups/',
            headers=headers,
            query_string={
                'fields': ','.join(map(str, fields))
            }
        )
        assert_that(
           list(response['result'][0].keys()),
           contains_inanyorder(*fields),
        )

        # Проверяем каждое поле по отдельности.
        fields.append('author_id')
        for field in fields:
            response = self.get_json(
                '/groups/',
                headers=headers,
                query_string={
                    'fields': field,
                }
            )

            assert_that(response['result'][0], has_key(field))


class TestGroupInYandexTeamOrg(TestCase):

    def setUp(self):
        super(TestGroupInYandexTeamOrg, self).setUp()
        org_yt_label = 'yandex-team'
        self.org_info = create_organization(
            self.meta_connection,
            self.main_connection,
            label=org_yt_label,
        )
        self.domain = build_organization_domain(org_yt_label, None)
        self.org = self.org_info['organization']
        self.department = self.org_info['root_department']
        self.user = UserModel(self.main_connection).get(
            user_id=self.org_info['admin_user']['id'],
            org_id=self.org['id'],
            fields=[
                '*',
                'groups.*',
            ]
        )
        set_auth_uid(self.user['id'])

    def test_delete_group_in_yandex_team_org(self):
        # тут проверяем, что рассылка для огранизации в домене
        # yandex-team.ws.yandex.ru удалилась только из Директории

        group_model = GroupModel(self.main_connection)
        group = group_model.create(
            name=GROUP_NAME,
            org_id=self.org['id'],
        )
        group_model.update_one(
            org_id=self.org['id'],
            group_id=group['id'],
            data={
                'members': [],
                'email': 'g0@yandex-team.ru',
            }
        )

        response = self.delete_json(
            '/groups/%s/' % group['id'],
            expected_code=200
        )
        assert_that(response['message'], 'No Content')
        # проверим, что группа действительно удалилась
        self.delete_json('/groups/%s/' % group['id'], expected_code=404)


class TestGroupList__post(TestOrganizationWithoutDomainMixin, TestCase):
    entity_list_url = '/groups/'
    entity_model = GroupModel

    def create_entity(self):
        return self.create_group()

    def _create_group(self, name, **kwargs):
        data = dict(kwargs, name=dict(ru=name))
        return self.post_json('/groups/', data)

    def prepare_entity_for_api_response(self, entity):
        return prepare_group(self.main_connection, entity, api_version=1)

    def test_create_single_group(self):
        # тут проверяем, что пользователь, создающий группу,
        # включается в неё автоматически и становится её админом
        # https://st.yandex-team.ru/DIR-286
        label = 'group_%d' % random.randint(1024, 65535)
        external_id = 'external_id'
        data = {
            'name': GROUP_NAME,
            'label': label,
            'description': GROUP_DESCRIPTION,
            'external_id': external_id,
        }
        response_data = self.post_json('/groups/', data)

        user = UserModel(self.main_connection).get(user_id=self.user['id'])
        user = has_entries(id=user['id'])

        assert_that(
            response_data,
            has_entries(
                member_of=[],
                name=GROUP_NAME,
                label=label,
                description=GROUP_DESCRIPTION,
                author=user,
                external_id=external_id,
                admins=contains(user),
                maillist_type='inbox',
                members=contains(
                    has_entries(
                        type='user',
                        object=user,
                    )
                ),
            )
        )
        self.assertEqual(len(response_data['admins']), 1)

    def test_create_group_with_maillist_type(self):
        label = 'group_%d' % random.randint(1024, 65535)
        external_id = 'external_id'
        maillist_type = 'shared'
        data = {
            'name': GROUP_NAME,
            'label': label,
            'description': GROUP_DESCRIPTION,
            'external_id': external_id,
            'maillist_type': maillist_type,
        }
        response_data = self.post_json('/groups/', data)

        assert_that(
            response_data,
            has_entries(
                member_of=[],
                name=GROUP_NAME,
                label=label,
                description=GROUP_DESCRIPTION,
                external_id=external_id,
                maillist_type=maillist_type,
            ),
        )

    def test_create_group_from_outer_admin_without_admins_should_not_return_error(self):
        outer_admin = self.create_user(nickname='outer_admin', is_outer=True)
        outer_admin_auth_headers = get_auth_headers(as_outer_admin=outer_admin)

        data = {
            'name': GROUP_NAME,
            'label': 'group_%d' % random.randint(1024, 65535),
            'description': GROUP_DESCRIPTION,
        }
        self.post_json(
            '/groups/',
            data=data,
            headers=outer_admin_auth_headers,
            expected_code=201,
        )

    def test_create_group_from_outer_admin_with_admins(self):
        outer_admin = self.create_user(nickname='outer_admin', is_outer=True)
        outer_admin_auth_headers = get_auth_headers(as_outer_admin=outer_admin)
        user = UserModel(self.main_connection).get(user_id=self.user['id'])

        data = {
            'name': GROUP_NAME,
            'label': 'group_%d' % random.randint(1024, 65535),
            'description': GROUP_DESCRIPTION,
            'admins': [{'id': user['id']}],
        }
        response_data = self.post_json(
            '/groups/',
            data,
            headers=outer_admin_auth_headers,
            expected_code=201,
        )
        self.assertEqual(len(response_data['admins']), 1)
        assert_that(
            response_data,
            has_entries(
                member_of=[],
                name=data['name'],
                label=data['label'],
                description=data['description'],
                author=None,
                members=[],
                admins=contains(has_entries(id=user['id'])),
            )
        )

    def test_add_admin_to_empty_admin_group(self):
        outer_admin = self.create_user(nickname='outer_admin', is_outer=True)
        outer_admin_auth_headers = get_auth_headers(as_outer_admin=outer_admin)
        user = self.create_user()

        group_model = GroupModel(self.main_connection)
        admin_group = group_model.get_or_create_admin_group(self.organization['id'])
        group_model.delete({'id': admin_group['id'], 'org_id': admin_group['org_id']}, force=True, generate_action=False)

        group = self.create_group(type='organization_admin')
        response_data = self.get_json('/groups/%s/' % group['id'], headers=outer_admin_auth_headers)
        self.assertEqual(response_data['admins'], [])

        data = {
            'admins': [{'id': user['id'], 'type': TYPE_USER}],
        }
        response_data = self.patch_json(
            '/groups/%s/' % group['id'],
            data,
            headers=outer_admin_auth_headers,
            expected_code=200,
        )
        assert_that(
            response_data,
            has_entries(
                admins=contains(has_entries(id=user['id'])),
            )
        )

    def test_create_group_from_outer_admin_without_admins_but_with_members_should_not_return_error(self):
        outer_admin = self.create_user(nickname='outer_admin', is_outer=True)
        outer_admin_auth_headers = get_auth_headers(as_outer_admin=outer_admin)
        user = UserModel(self.main_connection).get(user_id=self.user['id'])
        department = self.create_department()
        group = self.create_group()

        members = [
            {'type': TYPE_USER, 'id': user['id']},
            {'type': TYPE_GROUP, 'id': group['id']},
            {'type': TYPE_DEPARTMENT, 'id': department['id']},
        ]
        data = {
            'name': GROUP_NAME,
            'label': 'group_%d' % random.randint(1024, 65535),
            'description': GROUP_DESCRIPTION,
            'members': members,
        }
        self.post_json(
            '/groups/',
            data,
            headers=outer_admin_auth_headers,
            expected_code=201,
        )

    def test_create_group_from_admin_with_another_user_as_group_admin(self):
        another_user = self.create_user()
        label = 'group_%d' % random.randint(1024, 65535)
        data = {
            'name': GROUP_NAME,
            'label': label,
            'description': GROUP_DESCRIPTION,
            'admins': [{'id': another_user['id'], 'type': TYPE_USER}],
        }
        response_data = self.post_json('/groups/', data)

        msg = 'У созданной группы должен был быть только один админ переданный в admins в POST запросе'
        self.assertEqual(len(response_data['admins']), 1, msg=msg)

        msg = 'Группа была создана не с тем человеком в качестве админа, что был передан в POST запросе'
        self.assertEqual(response_data['admins'][0]['id'], another_user['id'], msg=msg)

    def test_create_group_from_admin_with_himself_and_another_user_as_group_admin(self):
        another_user = self.create_user()
        label = 'group_%d' % random.randint(1024, 65535)
        data = {
            'name': GROUP_NAME,
            'label': label,
            'description': GROUP_DESCRIPTION,
            'admins': [
                {'id': another_user['id'], 'type': TYPE_USER},
                {'id': self.user['id'], 'type': TYPE_USER}
            ],
        }
        response_data = self.post_json('/groups/', data)

        msg = 'У созданной группы должно было быть два админа, которые были переданы в admins в POST запросе'
        self.assertEqual(len(response_data['admins']), 2, msg=msg)

        expected_admin_ids = {self.user['id'], another_user['id']}
        response_admin_ids = {admin['id'] for admin in response_data['admins']}
        msg = 'Группа была создана не с теми админами, что были переданы в POST запросе'
        self.assertEqual(response_admin_ids, expected_admin_ids, msg=msg)

    def test_create_group_with_members(self):
        label = 'group_%d' % random.randint(1024, 65535)
        user0 = self.create_user()
        department = self.create_department()
        group = self.create_group()
        members = [
            {'type': TYPE_USER, 'id': user0['id']},
            {'type': TYPE_GROUP, 'id': group['id']},
            {'type': TYPE_DEPARTMENT, 'id': department['id']},
        ]
        data = {
            'name': GROUP_NAME,
            'label': label,
            'description': GROUP_DESCRIPTION,
            'members': members,
        }
        response_data = self.post_json('/groups/', data)
        group_model = GroupModel(self.main_connection)

        expected = prepare_group(
            self.main_connection,
            group_model.get(
                org_id=self.organization['id'],
                group_id=response_data['id'],
                fields=[
                    '*',
                    'author.*',
                    'members.*',
                    'admins.*',
                    'email',
                ],
            ),
            api_version=1,
        )
        expected['member_of'] = group_model.get_parents(
            org_id=self.organization['id'],
            group_id=response_data['id']
        )
        self.assertEqual(response_data, expected)
        self.assertEqual(response_data['name'], GROUP_NAME)
        self.assertEqual(response_data['label'], label)
        self.assertEqual(response_data['description'], GROUP_DESCRIPTION)
        user = UserModel(self.main_connection).get(user_id=self.user['id'])
        del user['user_type']
        self.assertEqual(
            response_data['author'],
            prepare_user(
                self.main_connection,
                user,
                expand_contacts=True,
                api_version=1,
            )
        )
        assert_that(
            response_data['members'],
            contains_inanyorder(
                has_entries(object=has_entries(id=user0['id'])),
                has_entries(object=has_entries(id=group['id'])),
                has_entries(object=has_entries(id=department['id'])),
                has_entries(object=has_entries(id=self.user['id'])),
            )
        )

    def test_create_group_with_double_member(self):
        label = 'group_%d' % random.randint(1024, 65535)
        members = [
            {'type': TYPE_USER, 'id': self.user['id']},
            {'type': TYPE_USER, 'id': self.user['id']},
        ]
        data = {
            'name': GROUP_NAME,
            'label': label,
            'description': GROUP_DESCRIPTION,
            'members': members,
        }
        self.post_json('/groups/', data)

    def test_create_group_with_not_exist_members(self):
        label = 'group_%d' % random.randint(1024, 65535)
        user0 = self.create_user()
        department = self.create_department()
        group = self.create_group()
        members = [
            {'type': TYPE_USER, 'id': user0['id']},
            {'type': TYPE_GROUP, 'id': group['id']},
            {'type': TYPE_DEPARTMENT, 'id': department['id']},
            {'type': TYPE_USER, 'id': 1234567890},
        ]
        data = {
            'name': GROUP_NAME,
            'label': label,
            'description': GROUP_DESCRIPTION,
            'members': members,
        }
        response_data = self.post_json('/groups/', data, expected_code=422)
        assert_that(
            response_data,
            has_entries(
                code='constraint_validation.objects_not_found',
                message='Some objects of type "{type}" were not found in database',
                params={'type': 'user'},
            )
        )

    def test_incorrect_label_in_passport(self):
        max_length_login_for_pdd = 41
        label = '1'*max_length_login_for_pdd
        data = {
            'name': GROUP_NAME,
            'label': label,
            'description': GROUP_DESCRIPTION,
        }
        self.mocked_passport.validate_login.side_effect = LoginLong
        self.post_json('/groups/', data, expected_code=422)

    def test_no_label(self):
        data = {
            'name': GROUP_NAME,
            'description': GROUP_DESCRIPTION,
        }
        self.post_json('/groups/', data)

        response = self.post_json('/groups/', data)
        assert_that(
            response,
            has_entries(
                label=None,
                uid=None,
                email=None,
            )
        )

    def test_empty_label(self):
        data = {
            'name': GROUP_NAME,
            'label': ' \t\v'
        }
        response = self.post_json('/groups/', data)
        assert_that(
            response,
            has_entries(
                label=None,
                uid=None,
                email=None,
            )
        )

    def test_conflict_error(self):
        # проверим, что если логин занят
        # то вторую группу завести не удастся
        label = 'group_%d' % random.randint(1024, 65535)
        data = {
            'name': GROUP_NAME,
            'label': label,
            'description': GROUP_DESCRIPTION,
        }

        # заводим первую
        self.post_json('/groups/', data)

        # заводим вторую
        response_data = self.post_json('/groups/', data, expected_code=409)

        # и на второй раз должна быть ошибка
        assert_that(
            response_data,
            has_entries(
                code='some_group_has_this_label',
                message='Some group already uses "{login}" as label',
                params={'login': label},
            )
        )

    def not_unique_label_with_nickname(self):
        not_uniq_label = 'user_login'
        self.create_user(
            nickname=not_uniq_label,
            email=build_email(self.main_connection, not_uniq_label, org_id=self.organization['id']),
        )
        data = {
            'name': GROUP_NAME,
            'label': not_uniq_label,
            'description': GROUP_DESCRIPTION,
        }
        response_data = self.post_json('/groups/', data, expected_code=409)
        assert_that(
            response_data,
            has_entries(
                code='some_user_has_this_login',
                message='Some user already exists with login "{login}"',
                params={'login': not_uniq_label},
            )
        )

    def test_not_unique_label_with_nickname_with_feature_off(self):
        set_feature_value_for_organization(
            self.meta_connection,
            self.organization['id'],
            MULTIORG,
            False,
        )
        self.not_unique_label_with_nickname()

    def test_not_unique_label_with_nickname_with_feature_enabled(self):
        set_feature_value_for_organization(
            self.meta_connection,
            self.organization['id'],
            MULTIORG,
            True,
        )

        self.not_unique_label_with_nickname()

    def test_can_create_not_unique_label_within_instance(self):
        not_uniq_label = 'group_label'
        another_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']

        self.another_group = GroupModel(self.main_connection).create(
            label=not_uniq_label,
            name=GROUP_NAME,
            email='group@google.com',
            org_id=another_organization['id']
        )
        data = {
            'name': GROUP_NAME,
            'label': not_uniq_label,
            'description': GROUP_DESCRIPTION,
        }
        self.post_json('/groups/', data)

    def test_not_unique_label_with_department(self):
        not_uniq_label = 'department_label'
        self.create_department(label=not_uniq_label)
        data = {
            'name': GROUP_NAME,
            'label': not_uniq_label,
            'description': GROUP_DESCRIPTION,
        }
        response_data = self.post_json('/groups/', data, expected_code=409)
        assert_that(
            response_data,
            has_entries(
                code='some_department_has_this_label',
                message='Some department already uses "{login}" as label',
                params={'login': not_uniq_label},
            )
        )

    def test_create_group_with_empty_name(self):
        # поле name не должно быть пустым
        # https://st.yandex-team.ru/DIR-399
        data = {
            'name': {'ru': ''},
        }
        response = self.post_json('/groups/', data, expected_code=422)
        error_message = (
            '{\'ru\': \'\'} is not valid under any of the given schemas'
        )
        self.assertTrue(
            response['params']['errors'][0]['message'].startswith(error_message)
        )

    def test_create_group_with_name_from_spaces(self):
        # поле name не должно состоять из одних пробельных символов
        # https://st.yandex-team.ru/DIR-399
        data = {
            'name': {'ru': '  \t '},
            'label': 'label',
        }
        response = self.post_json('/groups/', data, expected_code=422)
        error_message = (
            '{\'ru\': \'  \\t \'} is not valid under any of the given schemas'
        )
        self.assertTrue(
            response['params']['errors'][0]['message'].startswith(error_message)
        )

    def test_create_group_with_name_from_unicode_spaces(self):
        # поле name не должно состоять из одних пробельных символов
        # https://st.yandex-team.ru/DIR-399
        data = {
            'name': {'ru': '  \u00A0 '},
        }
        response = self.post_json('/groups/', data, expected_code=422)
        error_message = (
            '{\'ru\': \'  \\xa0 \'} is not valid under any of the given '
            'schemas'
        )
        self.assertTrue(
            response['params']['errors'][0]['message'].startswith(error_message)
        )

    def test_link_two_groups_using_inline_references(self):
        a_label = 'group_%d' % random.randint(1024, 65535)
        b_label = 'group_%d' % random.randint(1024, 65535)
        group1 = self._create_group('A', label=a_label)
        members = [{'type': 'group', 'id': group1['id']}]
        group2 = self._create_group('B', members=members, label=b_label)

        group2_from_db = GroupModel(self.main_connection).get(
            org_id=self.organization['id'],
            group_id=group2['id'],
            fields=['members.*']
        )
        assert_that(
            group2_from_db,
            has_entries(
                members=contains_inanyorder(
                    has_entries(
                        type='group',
                        object=has_entries(id=group1['id'])
                    ),
                    has_entries(
                        type='user',
                        object=has_entries(
                            id=self.user['id']
                        ),
                    ),
                )
            )
        )

    def test_post_event(self):
        event_model = EventModel(self.main_connection)
        action_model = ActionModel(self.main_connection)
        event_model.delete(force_remove_all=True)
        action_model.delete(force_remove_all=True)

        label = 'event'
        data = {
            'name': GROUP_NAME,
            'label': label,
            'description': GROUP_DESCRIPTION,
        }
        self.post_json('/groups/', data)

        events = [x['name'] for x in event_model.find()]
        events.sort()
        expected = ['group_added',
                    'user_group_added',
                    ]
        assert_that(events, equal_to(expected))

        action = action_model.find()[0]['name']
        assert_that(action, equal_to('group_add'))

    def test_post_group_with_uppercase_label(self):
        # проверим, что label с uppercase-ом переведется в нижний регистр
        label = 'GROUP_%d' % random.randint(1024, 65535)
        data = {
            'name': GROUP_NAME,
            'label': label,
            'description': GROUP_DESCRIPTION,
        }
        response_data = self.post_json('/groups/', data)

        assert_that(
            response_data,
            has_entries(
                label=label.lower()
            )
        )

        # проверим, что label с содержимым uppercase-а, но приведенным в нижний
        # регистр - не добавится повторно
        data = {
            'name': GROUP_NAME,
            'label': label.lower(),
            'description': GROUP_DESCRIPTION,
        }
        self.post_json('/groups/', data, expected_code=409)

    def test_post_group_with_type_required_no_dynamic_scope(self):
        # пытаемся создать группу с типом для которого необходим динамический scope,
        # но на этот скоуп нет прав
        data = {
            'name': GROUP_NAME,
            'label': 'label',
            'type': 'arbitrary-group-type',
        }
        response = self.post_json('/groups/', data, expected_code=403)

        assert_that(
            response,
            has_entries(
                code='authorization-error',
                message='This operation requires one of scopes: create_groups_of_type_arbitrary-group-type',
            )
        )

    def test_post_group_with_type_required_has_dynamic_scope(self):
        # пытаемся создать группу с типом для которого необходим динамический scope,
        # и есть права на этот скоуп
        group_type = 'my-group-type'
        data = {
            'name': GROUP_NAME,
            'label': 'label',
            'type': group_type,
        }
        dynamic_scope = 'create_groups_of_type_{}'.format(group_type)
        allowed_scopes = [
            dynamic_scope,
            scope.write_groups,
            scope.work_on_behalf_of_any_user,
            scope.work_with_any_organization,
        ]
        with scopes(*allowed_scopes):
            self.post_json('/groups/', data)

    def test_post_group_org_without_domains_with_label_should_return_error(self):
        #  Пытаемся создать группу для организации с has_owned_domains=False и передаем label
        data = {
            'name': GROUP_NAME,
            'label': 'some_label',
        }
        headers = get_auth_headers(as_outer_admin={
            'id': self.yandex_admin['id'],
            'org_id': self.yandex_organization['id'],
        })
        self.post_json(
            '/groups/',
            data,
            headers=headers,
            expected_code=422,
        )

    def test_post_group_org_without_domain_without_label(self):
        # Создаем группу для организации с has_owned_domains=False и не передаем label
        data = {
            'name': GROUP_NAME,
        }
        headers = get_auth_headers(as_outer_admin={
            'id': self.yandex_admin['id'],
            'org_id': self.yandex_organization['id'],
        })
        response_data = self.post_json('/groups/', data, headers=headers)

        assert_that(
            response_data,
            has_entries(
                name=GROUP_NAME,
                label=None,
                uid=None,
                email=None,
            )
        )
        self.assertEqual(len(response_data['admins']), 1)


class TestGroupList__maillist_service(TestCase):
    maillist_management = True
    # api_version = 7

    def test_create_groups(self):
        # создаем отдел в организации под управлением нового сервиса рассылок
        self.clean_actions_and_events()

        label = 'group_%d' % random.randint(1024, 65535)
        data = {
            'name':  GROUP_NAME,
            'description':  GROUP_DESCRIPTION,
            'label': label,
        }
        response_data = self.post_json('/groups/', data)

        #  рассылка создана напрямую в паспорте и у нее есть uid
        assert_called_once(
            self.mocked_passport.maillist_add,
            login=label,
            domain=self.organization_domain,
        )

        assert_that(
            response_data,
            has_entries(uid=not_none())
        )

        assert_that(
            ActionModel(self.main_connection).filter(name=action.group_add).all(),
            contains(
                has_entries(
                    object=has_entries(
                        uid=response_data['uid']
                    )
                )
            )
        )

        assert_that(
            EventModel(self.main_connection).filter(name=event.group_added).all(),
            contains(
                has_entries(
                    object=has_entries(
                        uid=response_data['uid']
                    )
                )
            )
        )

    def test_delete_groups(self):
        # удаляем команду в организации под управлением нового сервиса рассылок

        label = 'group_%d' % random.randint(1024, 65535)
        data = {
            'name':  GROUP_NAME,
            'description':  GROUP_DESCRIPTION,
            'label': label,
        }
        group = self.post_json('/groups/', data)

        #  рассылка создана напрямую в паспорте и у нее есть uid
        assert_that(
            group,
            has_entries(uid=not_none())
        )
        self.clean_actions_and_events()
        self.delete_json('/groups/%s/' % group['id'], expected_code=200)

        assert_called_once(
            self.mocked_passport.maillist_delete,
            group['uid'],
        )

        assert_that(
            EventModel(self.main_connection).filter(name=event.group_deleted).all(),
            contains(
                has_entries(
                    object=has_entries(
                        uid=group['uid']
                    )
                )
            )
        )

    def test_patch_label(self):
        data = {
            'label': self.label,
            'name': GROUP_NAME,
        }
        group = self.post_json('/groups/', data)

        assert_that(
            group,
            has_entries(
                uid=not_none(),
                label=self.label,
            )
        )
        new_label = 'new_label'
        response_data = self.patch_json(
            '/groups/%s/' % group['id'],
            data={
                'label': new_label
            }
        )

        assert response_data['label'] == new_label
        assert response_data['uid'] != group['uid']

        assert_called_once(
            self.mocked_passport.maillist_delete,
            group['uid'],
        )

    def test_patch_label_from_none(self):
        data = {
            'name': GROUP_NAME,
        }
        group = self.post_json('/groups/', data)

        assert_that(
            group,
            has_entries(
                uid=None,
                label=None,
                email=None,
            )
        )
        new_label = 'new_label'
        response_data = self.patch_json(
            '/groups/%s/' % group['id'],
            data={
                'label': new_label
            }
        )

        assert_called_once(
            self.mocked_passport.maillist_add,
            login=new_label,
            domain=self.organization_domain,
        )

        assert_not_called(self.mocked_passport.maillist_delete)

        assert_that(
            response_data,
            has_entries(
                label=new_label,
                uid=not_none()
            )
        )

    def test_patch_label_to_none(self):
        data = {
            'label': self.label,
            'name': GROUP_NAME,
        }
        group = self.post_json('/groups/', data)

        assert_that(
            group,
            has_entries(
                uid=not_none(),
                label=self.label,
            )
        )
        new_label = None
        response_data = self.patch_json(
            '/groups/%s/' % group['id'],
            data={
                'label': new_label
            }
        )

        assert_called_once(
            self.mocked_passport.maillist_add,
            login=self.label,
            domain=self.organization_domain,
        )

        assert_called_once(
            self.mocked_passport.maillist_delete,
            group['uid'],
        )

        assert_that(
            response_data,
            has_entries(
                label=None,
                uid=None,
            )
        )


class TestGroupDetail__get(TestCase):
    def setUp(self):
        super(TestGroupDetail__get, self).setUp()

        GROUP_NAME = {
            'ru': 'Группа',
            'en': 'Group',
        }

    def test_existing_group(self):
        # получаем данные по группе
        another_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']

        uid = self.user['id']

        group_model = GroupModel(self.main_connection)
        group = group_model.create(
            name={
                'ru': 'Группа',
                'en': 'Group'
            },
            org_id=self.organization['id'],
            admins=[uid],
            author_id=uid,
        )
        # добавим в группу участника
        GroupModel(self.main_connection).add_member(
            org_id=self.organization['id'],
            group_id=group['id'],
            member={'type': 'user', 'id': self.user['id']}
        )
        group_model.create(
            name={
                'ru': 'Группа другая',
                'en': 'Group another'
            },
            org_id=another_organization['id']
        )

        data = self.get_json('/groups/%s/' % group['id'])

        assert_that(
            data,
            has_entries(
                id=group['id'],
                admins=contains(
                    has_entries(
                        id=uid,
                        contacts=has_length(
                            greater_than_or_equal_to(1)
                        )
                    )
                ),
                author=has_entries(
                    id=uid,
                    contacts=contains(
                        has_entries(
                            type='staff',
                        ),
                        has_entries(
                            type='email',
                            main=True,
                            value=self.user['email'],
                        )
                    )
                ),
                member_of=[],
                members=contains(
                    has_entries(
                        type='user',
                        object=has_entries(
                            contacts=contains(
                                has_entries(
                                    type='staff',
                                ),
                                has_entries(
                                    type='email',
                                    main=True,
                                    value=self.user['email'],
                                )
                            )
                        )
                    )
                ),
            )
        )

    def test_internal_service_can_get_info_about_group(self):
        group = GroupModel(self.main_connection).create(
            name={'ru': 'Группа'},
            org_id=self.organization['id'],
        )

        headers = get_auth_headers(as_org=self.organization)
        data = self.get_json('/groups/%s/' % group['id'], headers=headers)

        assert_that(data, has_entries(id=group['id']))

    def test_not_existing_group(self):
        response = self.client.get('/groups/100/', headers=get_auth_headers())
        check_response(response, 404)
        response_data = json.loads(response.data)
        assert_that_not_found(response_data)


class TestGroupDetail__get_6(TestCase):
    api_version = 'v6'

    def setUp(self):
        super(TestGroupDetail__get_6, self).setUp()

        group_model = GroupModel(self.main_connection)
        self.group = group_model.create(
            name=GROUP_NAME,
            org_id=self.organization['id'],
            admins=[self.user['id']],
            author_id=self.user['id'],
        )

    def test_fields_returned_by_detail_view(self):
        # Проверяем, что ручка возвращает все перечисленные поля.
        fields = list(GroupModel.all_fields)
        for field in GROUP_PRIVATE_FIELDS:
            fields.remove(field)
        # В этом тесте запрашиваются одновременно author и author_id.
        # Так как будет два поля с одинаковым значением, то в prepare_group
        # поле author_id не будет добавлено в response.
        fields.remove('author_id')
        headers = get_auth_headers()
        response = self.get_json(
            '/groups/{group_id}/'.format(group_id=self.group['id']),
            headers=headers,
            query_string={
                'fields': ','.join(map(str, fields))
            }
        )
        assert_that(
           list(response.keys()),
           contains_inanyorder(*fields),
        )

        # Проверяем каждое поле по отдельности.
        fields.append('author_id')
        for field in fields:
            response = self.get_json(
                '/groups/{group_id}/'.format(group_id=self.group['id']),
                headers=headers,
                query_string={
                    'fields': field,
                }
            )
            assert_that(response, has_key(field))


    def test_member_count(self):
        group = create_group(
            self.main_connection,
            org_id=self.group['org_id'],
            type='generic',
        )
        self.members = [
            {
                'type': 'user',
                'id': self.user['id']
            },
        ]
        GroupModel(self.main_connection).update_one(
            org_id=self.group['org_id'],
            group_id=group['id'],
            data={
                'members': self.members
            }
        )
        headers = get_auth_headers()
        response = self.get_json(
            '/groups/{group_id}/'.format(group_id=group['id']),
            headers=headers,
            query_string={
                'fields': 'members_count'
            }
        )
        assert(response == {'members_count': 1, 'id': 6})

class TestSubgroups(TestCase):
    group_counter = 0

    def create_group(self, **kwargs):
        self.group_counter += 1
        m = GroupModel(self.main_connection)
        return m.create(name={'ru': 'g{0}'.format(self.group_counter)},
                        org_id=self.organization['id'],
                        **kwargs)

    def group_list_matcher(self, *group_ids):
        """Вовращает матчер, который проверяет, что
        список содержит объекты с такими же id как у *group_ids
        """
        matchers = [
            has_entries(id=group_id)
            for group_id in group_ids
        ]
        return contains_inanyorder(*matchers)

    def group_members_matcher(self, *ids):
        """Вовращает матчер, который проверяет, что
        список содержит объекты {type: 'group', object: {id: 124, ...}}
        """
        matchers = [
            has_entries(type='group',
                        object=has_entries(id=group_id))
            for group_id in ids
        ]
        return contains_inanyorder(*matchers)

    def test_get(self):
        g1 = self.create_group()['id']
        g2 = self.create_group()['id']
        members = [{'type': 'group', 'id': g1},
                   {'type': 'group', 'id': g2}]
        g3 = self.create_group(members=members)['id']

        response = self.get_json('/groups/{0}/members/'.format(g3))
        assert_that(response, self.group_members_matcher(g1, g2))


class TestGroupDetailView__patch(TestCase):
    def setUp(self):
        super(TestGroupDetailView__patch, self).setUp()

        self.group = create_group(
            self.main_connection,
            org_id=self.organization['id']
        )

        group_model = GroupModel(self.main_connection)

        prepared_group = group_model.get(
            org_id=self.group['org_id'],
            group_id=self.group['id'],
            fields=[
                '*',
                'members.*',
                'author.*',
            ],
        )

        prepared_group['member_of'] = group_model.get_parents(
            org_id=self.group['org_id'],
            group_id=self.group['id']
        )
        prepared_group['admins'] = []
        self.prepared_group = prepare_group(
            self.main_connection,
            prepared_group,
            api_version=1,
        )

    def test_should_return_not_found_if_no_group_exists(self):
        response_data = self.patch_json(
            '/groups/1000/',
            data={
                'name': {'ru': 'some name'}
            },
            expected_code=404
        )
        assert_that_not_found(response_data)

    def test_patch_name__with_value(self):
        new_name = {
            'ru': 'New group name'
        }
        response_data = self.patch_json(
            '/groups/%s/' % self.group['id'],
            data={
                'name': new_name
            }
        )
        self.prepared_group['name'] = new_name
        self.assertEqual(response_data.get('name'), new_name)
        self.assertEqual(response_data, self.prepared_group)
        new_group = GroupModel(self.main_connection).get(
            self.group['id'],
            self.organization['id'],
            fields=['name_plain']
        )
        self.assertEqual(new_group['name_plain'], new_name['ru'])

    def test_patch_name__with_null(self):
        self.patch_json(
            '/groups/%s/' % self.group['id'],
            data={
                'name': None
            },
            expected_code=422
        )

    def test_patch_name__with_empty_value(self):
        self.patch_json(
            '/groups/%s/' % self.group['id'],
            data={
                'name': None
            },
            expected_code=422
        )

    def test_patch_description(self):
        new_description = {
            'ru': 'New group description'
        }
        response_data = self.patch_json(
            '/groups/%s/' % self.group['id'],
            data={
                'description': new_description
            }
        )
        self.prepared_group['description'] = new_description
        self.assertEqual(response_data.get('description'), new_description)
        self.assertEqual(response_data, self.prepared_group)

    def test_patch_maillist_type(self):
        new_maillist_type = 'shared'
        response_data = self.patch_json(
            '/groups/%s/' % self.group['id'],
            data={
                'maillist_type': new_maillist_type
            }
        )
        self.prepared_group['maillist_type'] = new_maillist_type
        self.assertEqual(response_data.get('maillist_type'), new_maillist_type)
        self.assertEqual(response_data, self.prepared_group)

    def test_patch_external_id(self):
        # меняем external_id
        external_id = 'new_external_id'
        response_data = self.patch_json(
            '/groups/%s/' % self.group['id'],
            data={
                'external_id': external_id
            }
        )
        self.prepared_group['external_id'] = external_id
        self.assertEqual(response_data.get('external_id'), external_id)
        self.assertEqual(response_data, self.prepared_group)

    def test_patch_label(self):
        # запрещено менять label у команд
        response = self.patch_json(
            '/groups/%s/' % self.group['id'],
            data={
                'label': 'new_label'
            },
        )
        assert response['label'] == 'new_label'

    def test_patch_of_type_should_not_be_allowed(self):
        self.patch_json(
            '/groups/%s/' % self.group['id'],
            data={
                'type': 'department_head'
            },
            expected_code=422
        )

    def test_patch_members(self):
        department = create_department(self.main_connection, org_id=self.group['org_id'])
        another_group = create_group(
            self.main_connection,
            org_id=self.group['org_id']
        )

        members = [
            {
                'type': 'department',
                'id': department['id']
            },
            {
                'type': 'user',
                'id': self.user['id']
            },
            {
                'type': 'group',
                'id': another_group['id']
            },
        ]

        response_data = self.patch_json(
            '/groups/%s/' % self.group['id'],
            data={
                'members': members
            }
        )

        assert_that(
            response_data['members'],
            contains_inanyorder(
                has_entries(
                    type='department',
                    object=has_entries(
                        id=department['id']
                    )
                ),
                has_entries(
                    type='user',
                    object=has_entries(
                        id=self.user['id']
                    )
                ),
                has_entries(
                    type='group',
                    object=has_entries(
                        id=another_group['id']
                    )
                ),
            )
        )

    def test_should_return_error_if_delete_admin_uid_from_admin_group(self):
        members = [{'id': self.user['id'], 'type': TYPE_USER}]
        admin_group = self.create_group(type='organization_admin', members=members)
        another_user = self.create_user()
        members = [
            {
                'type': 'user',
                'id': another_user['id']
            },
        ]

        response = self.patch_json(
            '/groups/%s/' % admin_group['id'],
            data={
                'members': members
            },
            expected_code=403,
        )
        assert_that(
            response,
            has_entries(
                code='forbidden',
            )
        )

    def test_change_admin_group(self):
        admin_group = self.create_group(type='organization_admin')
        admin_uid = get_organization_admin_uid(
            self.main_connection,
            self.organization['id']
        )
        another_user = self.create_user()
        members = [
            {
                'type': 'user',
                'id': admin_uid
            },
            {
                'type': 'user',
                'id': another_user['id']
            },
        ]

        response_data = self.patch_json(
            '/groups/%s/' % admin_group['id'],
            data={
                'members': members
            }
        )
        assert_that(
            response_data['members'],
            contains_inanyorder(
                has_entries(
                    type=TYPE_USER,
                    object=has_entries(
                        id=admin_uid
                    )
                ),
                has_entries(
                    type=TYPE_USER,
                    object=has_entries(
                        id=another_user['id']
                    )
                ),
            )
        )

    def test_patch_with_double_member(self):
        members = [
            {'type': TYPE_USER, 'id': self.user['id']},
            {'type': TYPE_USER, 'id': self.user['id']},
        ]

        response_data = self.patch_json(
            '/groups/%s/' % self.group['id'],
            data={
                'members': members
            }
        )
        assert_that(
            response_data['members'],
            contains_inanyorder(
                has_entries(
                    type=TYPE_USER,
                    object=has_entries(
                        id=self.user['id']
                    )
                ),

            )
        )

    def test_patch_incorrect_members(self):
        # Пытаемся добавить несуществующие департамент, юзера, группу

        group = self.create_group()
        GroupModel(self.main_connection).delete(
            {'org_id': self.organization['id'],
             'id': group['id']}
        )

        members = [
            {
                'type': 'department',
                'id': -1,
            },
            {
                'type': 'user',
                'id': -1,
            },
            {
                'type': 'group',
                'id': -1,
            },
        ]

        self.patch_json(
            '/groups/%s/' % self.group['id'],
            data={
                'members': members
            },
            expected_code=422,
        )

    def test_cant_create_circular_dependency_by_patching_members(self):
        group_one = self.create_group()['id']
        self.create_group()['id']
        group_three = self.create_group(
            members=[{'type': 'group', 'id': group_one}]
        )['id']

        # у нас группа 3 содержит группу 1
        # а теперь попробуем создать циклическую зависимость
        # сделав так, чтобы группа 1 содержала группу 3
        response = self.patch_json(
            '/groups/%s/' % group_one,
            data={
                'members': [
                    {
                        'type': 'group',
                        'id': group_three
                    }
                ]
            },
            expected_code=422
        )
        assert_that(
            response,
            has_entries(
                code='constraint_validation.cycle_detected',
                message='Group {subgroup_id} already includes {group_id}',
                params={
                    'group_id': group_one,
                    'subgroup_id': group_three,
                },
            )
        )

        # на всякий случай проверим что подгруппы никак не изменились
        g = GroupModel(self.main_connection).find(
            filter_data={
                'org_id': self.organization['id'],
                'id': group_one,
            },
            fields=[
                'members',
            ]
        )
        assert_that(g[0], has_entries(members=is_([])))

    def test_patch_event(self):
        event_model = EventModel(self.main_connection)
        action_model = ActionModel(self.main_connection)
        event_model.delete(force_remove_all=True)
        action_model.delete(force_remove_all=True)

        new_name = {
            'ru': 'New'
        }
        department = create_department(self.main_connection, org_id=self.group['org_id'])
        another_group = create_group(
            self.main_connection,
            org_id=self.group['org_id']
        )
        members = [
            {
                'type': 'department',
                'id': department['id']
            },
            {
                'type': 'user',
                'id': self.user['id']
            },
            {
                'type': 'group',
                'id': another_group['id']
            },
        ]


        self.patch_json(
            '/groups/%s/' % self.group['id'],
            data={
                'name': new_name,
                'members': members
            }
        )

        events = [x['name'] for x in event_model.find()]
        events.sort()
        expected = sorted([
            'department_group_added',
            'group_group_added',
            'group_membership_changed',
            'group_property_changed',
            'resource_grant_changed',
            'user_group_added'
        ])
        assert_that(events, equal_to(expected))

        action = action_model.find()[0]['name']
        assert_that(action, equal_to('group_modify'))

    def test_patch_admins_groups(self):
        # проверяем, что при удалении всех members, админы - не удаляются
        department = create_department(self.main_connection, org_id=self.group['org_id'])
        another_group = create_group(
            self.main_connection,
            org_id=self.group['org_id']
        )

        group = create_group(
            self.main_connection,
            org_id=self.group['org_id'],
            type='generic',
            admins=[self.user['id']],
        )

        group['members'] = [
            {
                'type': 'department',
                'id': department['id']
            },
            {
                'type': 'user',
                'id': self.user['id']
            },
            {
                'type': 'group',
                'id': another_group['id']
            },
        ]

        patch_response = self.patch_json(
            '/groups/%s/' % group['id'],
            data={
                'members': []
            }
        )
        assert_that(patch_response['members'], equal_to([]))

        get_response = self.get_json('/groups/%s/' % group['id'])
        assert_that(
            get_response['admins'],
            contains_inanyorder(
                has_entries(id=self.user['id']),
            )
        )

    def test_patch_group_admins_add_and_delete(self):
        # проверяем, что изменение админов проходит успешно
        group = create_group(
            self.main_connection,
            org_id=self.group['org_id'],
            type='generic',
            admins=[self.user['id']],
        )

        another_user = self.create_user()

        patch_response = self.patch_json(
            '/groups/%s/' % group['id'],
            data={
                'admins': [dict(type=TYPE_USER, id=another_user['id'])]
            }
        )
        assert_that(
            patch_response['admins'],
            contains_inanyorder(
                has_entries(id=another_user['id']),
            )
        )

        get_response = self.get_json('/groups/%s/' % group['id'])
        assert_that(
            get_response['admins'],
            contains_inanyorder(
                has_entries(id=another_user['id']),
            )
        )

    def test_patch_group_admins_with_no_one(self):
        # проверяем, что можно оставить группу без админов
        another_user = self.create_user()
        group = create_group(
            self.main_connection,
            org_id=self.group['org_id'],
            type='generic',
            admins=[self.user['id'], another_user['id']],
        )

        self.patch_json('/groups/%s/' % group['id'], data={
                'admins': []
            }, expected_code=200,
        )

        # проверим, что в группе теперь нет админов
        response_group = self.get_json('/groups/%s/' % group['id'])
        assert len(response_group['admins']) == 0

    def test_patch_group_without_admins_should_not_return_error(self):
        # если группа была без админов и раньше - разрешаем её редактировать
        group = create_group(
            self.main_connection,
            org_id=self.group['org_id'],
            type='generic',
            admins=[],
        )

        new_group_name = {
            'ru': 'New group name',
            'en': '',
        }
        data = {
            'admins': [],
            'name': new_group_name,
        }
        self.patch_json('/groups/%s/' % group['id'], data=data, expected_code=200)

        # проверим, что в группе по прежнему нет админов
        response_group = self.get_json('/groups/%s/' % group['id'])
        self.assertEqual(response_group['admins'], [])
        self.assertEqual(response_group['name'], new_group_name)

    def test_patch_group_admins_with_incorrect_uid(self):
        # проверяем, что нельзя добавить объект с несуществующим uid-ом
        # в качестве админа и при этом оставить группу без админа
        another_user = self.create_user()
        group = create_group(
            self.main_connection,
            org_id=self.group['org_id'],
            type='generic',
            admins=[self.user['id'], another_user['id']],
        )

        response = self.patch_json('/groups/%s/' % group['id'], data={
                'admins': [dict(type='user', id=3475879345)]
            }, expected_code=422,
        )
        assert_that(
            response,
            has_entries(
                code='constraint_validation.objects_not_found',
                message='Some objects of type "{type}" were not found in database',
                params={'type': 'user'},
            )
        )

        # проверим, что в группе по прежнему есть админы, несмотря на
        # неуспешный патч
        response_group = self.get_json('/groups/%s/' % group['id'])
        assert len(response_group['admins']) != 0

    def test_patch_group_admins_with_group_and_department(self):
        # проверяем, что нельзя добавить группу или департамент
        # в качестве админа
        another_user = self.create_user()
        group = create_group(
            self.main_connection,
            org_id=self.group['org_id'],
            type='generic',
            admins=[self.user['id'], another_user['id']],
        )

        response = self.patch_json('/groups/%s/' % group['id'], data={
                'admins': [
                    dict(type='group', id=self.group['id']),
                    dict(type='deparment', id=self.department['id']),
                ]
            }, expected_code=422,
        )
        assert_that(
            response,
            has_entries(
                code='constraint_validation.only_users_can_be_group_admins',
                message='Only users can be group admins',
            )
        )

        # проверим, что в группе по прежнему есть админы, несмотря на
        # неуспешный патч
        response_group = self.get_json('/groups/%s/' % group['id'])
        assert len(response_group['admins']) != 0

    def test_patch_group_admins_delete_relations(self):
        # проверяем, что если есть только ресурсы на удаление, то
        # удаление успешно проходит
        another_user = self.create_user()

        group = create_group(
            self.main_connection,
            org_id=self.group['org_id'],
            type='generic',
            admins=[self.user['id'], another_user['id']],
        )

        self.patch_json('/groups/%s/' % group['id'], data={
                'admins': [dict(type=TYPE_USER, id=another_user['id'])]
            }
        )

    def test_patch_not_linked_admin_with_members(self):
        # Проверим, что если пользователи уже являются админами группы,
        # то мы можем их добавить руками в подписчики этой группы
        department = create_department(self.main_connection, org_id=self.group['org_id'])
        another_group = create_group(
            self.main_connection,
            org_id=self.group['org_id']
        )
        another_user = self.create_user()
        group = create_group(
            self.main_connection,
            org_id=self.group['org_id'],
            type='generic',
            admins=[self.user['id'], another_user['id']],
        )
        members = [
            {
                'type': 'department',
                'id': department['id']
            },
            {
                'type': 'user',
                'id': self.user['id']
            },
            {
                'type': 'user',
                'id': another_user['id']
            },
            {
                'type': 'group',
                'id': another_group['id']
            },
        ]

        response_data = self.patch_json(
            '/groups/%s/' % group['id'],
            data={
                'members': members
            }
        )
        assert_that(
            response_data['members'],
            contains_inanyorder(
                has_entries(
                    type='department',
                    object=has_entries(
                        id=department['id']
                    )
                ),
                has_entries(
                    type='user',
                    object=has_entries(
                        id=self.user['id']
                    )
                ),
                has_entries(
                    type='user',
                    object=has_entries(
                        id=another_user['id']
                    )
                ),
                has_entries(
                    type='group',
                    object=has_entries(
                        id=another_group['id']
                    )
                ),
            )
        )

        get_response = self.get_json('/groups/%s/' % group['id'])
        assert_that(
            get_response['admins'],
            contains_inanyorder(
                has_entries(id=self.user['id']),
                has_entries(id=another_user['id']),
            )
        )

    def test_patch_from_outer_admin(self):
        new_name = {
            'ru': 'New group name',
        }
        outer_admin = self.create_user(nickname='outer_admin', is_outer=True)
        outer_admin_auth_headers = get_auth_headers(as_outer_admin=outer_admin)

        response_data = self.patch_json(
            '/groups/%s/' % self.group['id'],
            data={
                'name': new_name
            },
            headers=outer_admin_auth_headers,
        )
        self.prepared_group['name'] = new_name
        self.assertEqual(response_data.get('name'), new_name)
        self.assertEqual(response_data, self.prepared_group)

    def test_patch_tech_group(self):
        # Создаём отдел и пытаемся добавить пользователя в его техническую группу.
        department = self.create_department()
        heads_group = GroupModel(self.main_connection).get(
            org_id=self.organization['id'],
            group_id=department['heads_group_id'],
            fields=['id', 'members'],
        )
        members = [
            {
                'type': 'user',
                'id': self.user['id'],
            },
        ]

        response = self.patch_json(
            '/groups/%s/' % heads_group['id'],
            data={
                'members': members,
            },
            headers=get_auth_headers(),
            expected_code=403,
        )

        assert_that(
            response,
            has_entries(
                code='forbidden',
            )
        )


class TestGroupDetail__delete(TestCase):
    def test_delete_exist_group(self):
        d0 = self.create_department()
        u0 = self.create_user()
        g0 = self.create_group(members=[
            {'type': 'user', 'id': u0['id']},
            {'type': 'department', 'id': d0['id']}
        ])
        g0['email'] = 'g0@yandex-team.ru'
        domain = self.organization_domain

        response = self.delete_json('/groups/%s/' % g0['id'], expected_code=200)
        assert_that(response['message'], 'No Content')
        # проверим, что группа действительно удалилась
        self.delete_json('/groups/%s/' % g0['id'], expected_code=404)

    def test_delete_not_exist_group(self):
        response = self.delete_json('/groups/349383840/', expected_code=404)
        assert_that(response['message'], 'Not Found')

    def test_delete_group_with_parent(self):
        # Проверим, что после удаления группы, родительская группа никуда не удалилась
        parent_group = self.create_group()
        group = self.create_group()
        GroupModel(self.main_connection).add_member(
            org_id=self.organization['id'],
            group_id=parent_group['id'],
            member={'type': 'group', 'id': group['id']}
        )
        response = self.delete_json('/groups/%s/' % group['id'], expected_code=200)
        self.assertEqual(response['message'], 'No Content')
        response = self.get_json('/groups/%s/' % parent_group['id'], expected_code=200)

    def test_delete_generic_group(self):
        group = self.create_group(type='generic')
        self.delete_json('/groups/%s/' % group['id'], expected_code=200)
        group_from_db = GroupModel(self.main_connection).find({
            'org_id': self.organization['id'],
            'id': group['id'],
            'removed': True,
        })
        assert_that(group_from_db[0]['removed'], True)
        # проверим, что группа действительно удалилась
        self.delete_json('/groups/%s/' % group['id'], expected_code=404)

    def test_delete_organization_admin_group(self):
        group_type_name = 'organization_admin'
        group = self.create_group(type=group_type_name)
        response = self.delete_json(
            '/groups/%s/' % group['id'],
            expected_code=403
        )
        assert_that(
            response['message'],
            equal_to('This operation requires one of scopes: create_groups_of_type_%s' % group_type_name)
        )

    def test_delete_robots_group(self):
        # провереям, что нельзя удалить роботную группу
        group_type_name = 'robots'
        group = self.create_group(type=group_type_name)
        response = self.delete_json(
            '/groups/%s/' % group['id'],
            expected_code=403
        )
        assert_that(
            response['message'],
            equal_to('This operation requires one of scopes: create_groups_of_type_%s' % group_type_name)
        )

    def test_delete_event(self):
        event_model = EventModel(self.main_connection)
        action_model = ActionModel(self.main_connection)
        event_model.delete(force_remove_all=True)
        action_model.delete(force_remove_all=True)

        d0 = self.create_department()
        u0 = self.create_user()
        g0 = self.create_group(members=[
            {'type': 'user', 'id': u0['id']},
            {'type': 'department', 'id': d0['id']}
        ])
        ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='autotest',
            relations=[
                {
                    'name': 'read',
                    'group_id': g0['id'],
                }
            ]
        )
        response = self.delete_json(
            '/groups/%s/' % g0['id'],
            expected_code=200
        )
        assert_that(response['message'], 'No Content')

        action = action_model.find()[0]['name']
        assert_that(action, equal_to('group_delete'))

        events = [x['name'] for x in event_model.find()]

        expected = ([
            'group_membership_changed',
            'user_group_deleted',
            'department_group_deleted',
            'resource_grant_changed',
            'resource_grant_changed',
            'group_deleted',
        ])

        assert_that(events, contains(*expected))

    def test_delete_group_with_members(self):
        label = 'group_%d' % random.randint(1024, 65535)
        user0 = self.create_user()
        department = self.create_department()
        group = self.create_group()
        members = [
            {'type': TYPE_USER, 'id': user0['id']},
            {'type': TYPE_GROUP, 'id': group['id']},
            {'type': TYPE_DEPARTMENT, 'id': department['id']},
        ]
        data = {
            'name': self.group_name,
            'label': label,
            'members': members,
            'description': {
                'ru': 'Описание группы',
                'en': 'Group description',
            }
        }
        response_data = self.post_json('/groups/', data)
        self.assertEqual(response_data['name'], self.group_name)
        self.assertEqual(response_data['label'], label)
        user = UserModel(self.main_connection).get(user_id=self.user['id'])
        del user['user_type']
        self.assertEqual(
            response_data['author'],
            prepare_user(
                self.main_connection,
                user,
                expand_contacts=True,
                api_version=1,
            )
        )
        assert_that(
            response_data['members'],
            contains_inanyorder(
                has_entries(object=has_entries(id=user0['id'])),
                has_entries(object=has_entries(id=group['id'])),
                has_entries(object=has_entries(id=department['id'])),
                has_entries(object=has_entries(id=self.user['id'])),
            )
        )


    def test_delete_group_with_custom_type(self):
        # Проверим, что если есть скоуп для создания группы с кастомным типом,
        # то и удалять такие группы можно
        group_type = 'my-group-type'
        dynamic_scope = 'create_groups_of_type_{}'.format(group_type)
        org_id = self.organization['id']
        group = GroupModel(self.main_connection).create(
            GROUP_NAME,
            org_id,
            label='label',
            type=group_type,
        )
        usual_scopes = [
            scope.write_groups,
            scope.work_on_behalf_of_any_user,
            scope.work_with_any_organization,
        ]

        # Если скоупа нет, то попытка удалить команду должна вернуть 403 Access Denied
        with scopes(*usual_scopes):
            self.delete_json(
                '/groups/{0}/'.format(group['id']),
                expected_code=403,
            )

        # А если скоуп есть, то удаление пройдёт без ошибки
        with scopes(dynamic_scope, *usual_scopes):
            self.delete_json(
                '/groups/{0}/'.format(group['id']),
                expected_code=200,
            )

        # Проверим, что команда удалилась
        group = GroupModel(self.main_connection).filter(id=group['id']).one()
        assert group is None, 'Group should be absent in database'


class TestGroupListYandexTeamOrg__post(TestYandexTeamOrgMixin, TestCase):

    def setUp(self):
        super(TestGroupListYandexTeamOrg__post, self).setUp()
        self.test_group_name = {
            'ru': 'Название группы в yt-организации',
            'en': 'YT-org name'
        }

    def test_create_group_in_yt_org_without_label(self):
        data = {
            'name': self.test_group_name,
        }
        response = self.post_json('/groups/', data)
        self.assertEqual(response['name'], self.test_group_name)
        self.assertEqual(response['label'], None)


class TestGroupMembersView__get(TestCase):
    def setUp(self):
        super(TestGroupMembersView__get, self).setUp()

        self.group = create_group(
            self.main_connection,
            org_id=self.organization['id']
        )

        department = create_department(self.main_connection, org_id=self.group['org_id'])
        another_group = create_group(
            self.main_connection,
            org_id=self.group['org_id']
        )

        self.members = [
            {
                'type': 'department',
                'id': department['id']
            },
            {
                'type': 'user',
                'id': self.user['id']
            },
            {
                'type': 'group',
                'id': another_group['id']
            },
        ]
        GroupModel(self.main_connection).update_one(
            org_id=self.group['org_id'],
            group_id=self.group['id'],
            data={
                'members': self.members
            }
        )

    def test_should_return_not_found_if_no_group_exists(self):
        response_data = self.get_json(
            '/groups/1000/members/',
            expected_code=404
        )
        assert_that_not_found(response_data)

    def test_should_return_prepared_group_members(self):
        response_data = self.get_json(
            '/groups/%s/members/' % self.group['id'],
        )
        expected = list(map(
            prepare_group_member_for_members_view,
            ResourceRelationModel(self.main_connection).find(
                filter_data=dict(
                    resource_id=self.group['resource_id'],
                    org_id=self.group['org_id'],
                ),
                fields=['*', 'user.*', 'group.*', 'department.*']
            )
        ))

        for x in expected:
            if x['type'] == 'user':
                x['object']['avatar_id'] = None

        self.assertEqual(response_data, expected)

    def test_should_work_when_only_org_id_known(self):
        # ручка может быть вызвана от имени сервиса,
        # и в тикете https://st.yandex-team.ru/DIR-1564
        # есть трейсбэк с которым она падала
        headers = get_auth_headers(as_org=self.organization['id'])
        with patch('intranet.yandex_directory.src.yandex_directory.core.utils.get_user_data_from_blackbox_by_uids') as mock_user_data:
            mock_user_data.return_value = {
                self.user['id']: {
                    'aliases': [],
                    'birth_date': '2000-01-01',
                    'default_email': 'only_passport_user@khunafin.xyz',
                    'first_name': 'user',
                    'is_maillist': False,
                    'language': 'ru',
                    'last_name': 'user',
                    'login': 'only_passport_user@khunafin.xyz',
                    'sex': '1',
                    'avatar_id': '123445',
                    'uid': self.user['id'],
                },
            }
            response_data = self.get_json(
                '/groups/%s/members/' % self.group['id'],
                headers=headers,
            )

        assert_that(
            response_data,
            contains_inanyorder(
                has_entries(
                    type='user',
                    object=has_entries(
                        nickname='admin',
                        avatar_id='123445',
                    ),
                ),
                has_entries(
                    type='group',
                ),
                has_entries(
                    type='department',
                ),
            )
        )

    def test_group_has_no_user_members(self):
        # список членов группы для групп состоящих только из отделов и других групп
        # не должно быть админов группы в списке членов группы
        group = create_group(
            self.main_connection,
            org_id=self.organization['id']
        )
        user = self.create_user()
        department = create_department(self.main_connection, org_id=group['org_id'])
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

        response_data = self.get_json(
            '/groups/%s/members/' % group['id'],
        )

        for item in response_data:
            self.assertIn('type', item)
            self.assertNotEqual(item['type'], 'user')


class TestGroupMembersView__post(TestCase):
    def setUp(self):
        super(TestGroupMembersView__post, self).setUp()

        self.group = create_group(
            self.main_connection,
            org_id=self.organization['id']
        )

        department = create_department(self.main_connection, org_id=self.group['org_id'])
        another_group = create_group(
            self.main_connection,
            org_id=self.group['org_id']
        )

        self.members = [
            {
                'type': 'department',
                'id': department['id']
            },
            {
                'type': 'user',
                'id': self.user['id']
            },
            {
                'type': 'group',
                'id': another_group['id']
            },
        ]
        GroupModel(self.main_connection).update_one(
            org_id=self.group['org_id'],
            group_id=self.group['id'],
            data={
                'members': self.members
            }
        )

    def create_group(self, **kwargs):
        self.group_counter += 1
        m = GroupModel(self.main_connection)
        return m.create(name={'ru': 'g{0}'.format(self.group_counter)},
                        org_id=self.organization['id'],
                        **kwargs)

    def test_should_return_not_found_if_no_group_exists(self):
        response_data = self.post_json(
            '/groups/1000/members/',
            data={
                'type': 'user',
                'id': 1
            },
            expected_code=404
        )
        assert_that_not_found(response_data)

    def test_should_return_validation_error_if_type_is_null_or_empty(self):
        for value in [None, '']:
            self.post_json(
                '/groups/%s/members/' % self.group['id'],
                data={
                    'type': value,
                    'id': 1
                },
                expected_code=422
            )

    def test_should_return_validation_error_if_id_is_null_or_empty(self):
        for value in [None, '']:
            self.post_json(
                '/groups/%s/members/' % self.group['id'],
                data={
                    'type': 'user',
                    'id': value
                },
                expected_code=422
            )

    def test_should_return_validation_error_if_type_is_not_sent(self):
        self.post_json(
            '/groups/%s/members/' % self.group['id'],
            data={
                'id': 1
            },
            expected_code=422
        )

    def test_should_return_validation_error_if_id_is_not_sent(self):
        self.post_json(
            '/groups/%s/members/' % self.group['id'],
            data={
                'type': 'user',
            },
            expected_code=422
        )

    def test_should_return_validation_error_if_user_does_not_exist(self):
        unknown_uid = 10000
        response_data = self.post_json(
            '/groups/%s/members/' % self.group['id'],
            data={
                'type': 'user',
                'id': unknown_uid,
            },
            expected_code=422
        )
        assert_that(
            response_data,
            has_entries(
                code='member_not_found',
                message='Unable to find "{type}" with id "{id}"',
                params={'type': 'user', 'id': unknown_uid},
            )
        )

    def test_should_return_validation_error_if_department_does_not_exist(self):
        unknown_department_id = 10000
        response_data = self.post_json(
            '/groups/%s/members/' % self.group['id'],
            data={
                'type': 'department',
                'id': unknown_department_id,
            },
            expected_code=422
        )
        assert_that(
            response_data,
            has_entries(
                code='member_not_found',
                message='Unable to find "{type}" with id "{id}"',
                params={'type': 'department', 'id': unknown_department_id},
            )
        )

    def test_should_return_validation_error_if_group_does_not_exist(self):
        unknown_group_id = 10000
        response_data = self.post_json(
            '/groups/%s/members/' % self.group['id'],
            data={
                'type': 'group',
                'id': unknown_group_id
            },
            expected_code=422
        )
        assert_that(
            response_data,
            has_entries(
                code='member_not_found',
                message='Unable to find "{type}" with id "{id}"',
                params={'type': 'group', 'id': unknown_group_id},
            )
        )

    def test_should_return_created_member_if_created(self):
        new_group = create_group(
            self.main_connection,
            org_id=self.group['org_id']
        )

        response_data = self.post_json(
            '/groups/%s/members/' % self.group['id'],
            data={
                'type': 'group',
                'id': new_group['id']
            },
            expected_code=201
        )
        expected = prepare_group_member_for_members_view(
            ResourceRelationModel(self.main_connection).find(
                filter_data=dict(
                    resource_id=self.group['resource_id'],
                    org_id=self.group['org_id'],
                    group_id=new_group['id']
                ),
                fields=['*', 'group.*']
            )[0]
        )
        self.assertEqual(response_data, expected)

    def test_cant_create_circular_dependency_by_creating_member(self):
        group_one = self.create_group()['id']
        self.create_group()['id']
        group_three = self.create_group(
            members=[{'type': 'group', 'id': group_one}]
        )['id']

        # у нас группа 3 содержит группу 1
        # а теперь попробуем создать циклическую зависимость
        # сделав так, чтобы группа 1 содержала группу 3
        response = self.post_json(
            '/groups/%s/members/' % group_one,
            data={
                'type': 'group',
                'id': group_three
            },
            expected_code=422
        )
        assert_that(
            response,
            has_entries(
                code='constraint_validation.cycle_detected',
                message='Group {subgroup_id} already includes {group_id}',
                params={'group_id': group_one, 'subgroup_id': group_three}
            )
        )

        # на всякий случай проверим что подгруппы никак не изменились
        g = GroupModel(self.main_connection).find(
            filter_data={
                'org_id': self.organization['id'],
                'id': group_one,
            },
            fields=['members'],
        )
        assert_that(g[0], has_entries(members=is_([])))

    def test_created_group_with_custom_user(self):
        user = self.create_user()

        self.post_json(
            '/groups/%s/members/' % self.group['id'],
            data={
                'type': 'user',
                'id': user['id']
            },
            expected_code=201
        )

    def test_new_member_in_tech_group(self):
        # создаём отдел и пытаемся добавить пользователя в его техническую группу
        department = self.create_department()
        heads_group = GroupModel(self.main_connection).get(
            org_id=self.organization['id'],
            group_id=department['heads_group_id'],
        )

        member = {
            'type': 'user',
            'id': self.user['id'],
        }

        response = self.post_json(
            '/groups/%s/members/' % heads_group['id'],
            data=member,
            headers=get_auth_headers(),
            expected_code=403,
        )

        assert_that(
            response,
            has_entries(
                code='forbidden',
            )
        )


class TestUsersBulkUpdateView__post(TestCase):
    def test_update_users(self):
        # проверим, что работает обновление пользователей

        another_department = self.create_department()
        another_user = self.create_user(department_id=self.user['department_id'])
        deputy_admin = self.create_user()

        self.post_json(
            '/users/bulk-update/',
            data=[
                {
                    'id': another_user['id'],
                    'role': 'admin',
                    'department_id': another_department['id'],
                },
                {
                    'id': deputy_admin['id'],
                    'role': 'deputy_admin',
                },
            ],
            expected_code=200,
        )

        deputy_admin = UserModel(self.main_connection).find(
            filter_data={
                'id': deputy_admin['id']
            },
            fields=[
                'role'
            ]
        )[0]
        another_user = UserModel(self.main_connection).find(
            filter_data={
                'id': another_user['id']
            },
            fields=[
                'department_id',
                'role'
            ]
        )[0]

        assert_that(
            deputy_admin,
            has_entries(
                role='deputy_admin'
            )
        )
        assert_that(
            another_user,
            has_entries(
                role='admin',
                department_id=another_department['id'],
            )
        )

    def test_one_invalid_user(self):
        # проверим, что при ошибки валидации полей хотя бы одного из пользователей, никто не обновляется

        another_user = self.create_user(department_id=self.user['department_id'])
        deputy_admin = self.create_user()
        self.post_json(
            '/users/bulk-update/',
            data=[
                {
                    'id': deputy_admin['id'],
                    'role': 'deputy_admin',
                },
                {
                    'id': another_user['id'],
                    'department_id': 0,
                },
            ],
            expected_code=422,
        )

        deputy_admin = UserModel(self.main_connection).find(
            filter_data={
                'id': deputy_admin['id']
            },
            fields=[
                'role'
            ]
        )[0]
        another_user = UserModel(self.main_connection).find(
            filter_data={
                'id': another_user['id']
            },
            fields=[
                'department_id',
                'role'
            ]
        )[0]

        assert_that(
            deputy_admin,
            has_entries(
                role='user'
            )
        )
        assert_that(
            another_user,
            has_entries(
                department_id=self.user['department_id'],
            )
        )

    def test_no_user_id(self):
        # проверим, что если не передать id пользователя, то падает с schema_validation_error
        response = self.post_json(
            '/users/bulk-update/',
            data=[
                {
                    'role': 'deputy_admin',
                },
            ],
            expected_code=422,
        )
        assert_that(
            response,
            has_entries(
                code='schema_validation_error'
            )
        )


class TestGroupMembersBulkUpdateView__post(TestCase):
    def setUp(self):
        super(TestGroupMembersBulkUpdateView__post, self).setUp()

        self.group = create_group(
            self.main_connection,
            org_id=self.organization['id']
        )

        department = create_department(self.main_connection, org_id=self.group['org_id'])
        another_group = create_group(
            self.main_connection,
            org_id=self.group['org_id']
        )

        self.members = [
            {
                'type': 'department',
                'id': department['id']
            },
            {
                'type': 'user',
                'id': self.user['id']
            },
            {
                'type': 'group',
                'id': another_group['id']
            },
        ]
        GroupModel(self.main_connection).update_one(
            org_id=self.group['org_id'],
            group_id=self.group['id'],
            data={
                'members': self.members
            }
        )
        self.valid_operations = [
            {
                'operation_type': 'add',
                'value': {
                    'type': 'user',
                    'id': 1
                }
            }
        ]
        self.new_group = create_group(
            self.main_connection,
            org_id=self.group['org_id']
        )
        self.new_group_member = {
            'type': 'group',
            'id': self.new_group['id']
        }
        self.new_department = create_department(self.main_connection, org_id=self.group['org_id'])
        self.new_department_member = {
            'type': 'department',
            'id': self.new_department['id']
        }

    def create_group(self, **kwargs):
        self.group_counter += 1
        m = GroupModel(self.main_connection)
        return m.create(name='g{0}'.format(self.group_counter),
                        org_id=self.organization['id'],
                        **kwargs)

    def test_should_return_not_found_if_no_group_exists(self):
        response_data = self.post_json(
            '/groups/1000/members/bulk-update/',
            data=self.valid_operations,
            expected_code=404
        )
        assert_that_not_found(response_data)

    def test_should_return_validation_error_if_object_does_not_exist(self):
        for object_type in ['user', 'department', 'group']:
            response_data = self.post_json(
                '/groups/%s/members/bulk-update/' % self.group['id'],
                data=[
                    {
                        'operation_type': 'add',
                        'value': {
                            'type': object_type,
                            'id': 10000
                        }
                    },
                ],
                expected_code=422
            )

            assert_that(
                response_data,
                has_entries(
                    code='member_not_found',
                    message='Unable to find "{type}" with id "{id}"',
                    params={'type': object_type, 'id': 10000},
                )
            )

    def test_should_return_validation_error_if_one_of_the_objects_does_not_exist(self):
        self.post_json(
            '/groups/%s/members/bulk-update/' % self.group['id'],
            data=[
                {
                    'operation_type': 'add',
                    'value': self.new_group_member
                },
                {
                    'operation_type': 'add',
                    'value': {
                        'type': 'group',
                        'id': 10000
                    }
                },
            ],
            expected_code=422
        )
        members = ResourceRelationModel(self.main_connection).find(
            filter_data=dict(
                resource_id=self.group['resource_id'],
                org_id=self.group['org_id'],
            ),
            fields=['user.*', 'group.*', 'department.*']
        )
        self.assertEqual(
            len(members),
            len(self.members),
            msg='Should not add existing member if one of added members does not exist'
        )

    def test_should_add_existing_members(self):
        self.post_json(
            '/groups/%s/members/bulk-update/' % self.group['id'],
            data=[
                {
                    'operation_type': 'add',
                    'value': self.new_group_member
                },
                {
                    'operation_type': 'add',
                    'value': self.new_department_member
                },
            ],
            expected_code=200
        )
        members = ResourceRelationModel(self.main_connection).find(
            filter_data=dict(
                resource_id=self.group['resource_id'],
                org_id=self.group['org_id'],
            ),
            fields=[
                '*',
                'user.*',
                'group.*',
                'department.*',
            ],
        )
        members = [
            {
                'id': i['object']['id'],
                'type': i['type']
            }
            for i in map(prepare_group_member_for_members_view, members)
        ]
        expected = self.members + [self.new_group_member, self.new_department_member]
        assert_that(members, expected)

    def test_should_add_double_members(self):
            new_user_member = {'type': 'user', 'id': self.user['id']}
            self.post_json(
                '/groups/%s/members/bulk-update/' % self.group['id'],
                data=[
                    {
                        'operation_type': 'add',
                        'value': new_user_member
                    },
                    {
                        'operation_type': 'add',
                        'value': new_user_member
                    },
                ],
                expected_code=200
            )

    def test_should_return_validation_error_if_non_generic_group(self):
        group_type_name = 'organization_admin'
        group = GroupModel(self.main_connection).get_or_create_admin_group(self.organization['id'])
        new_user_member = {'type': 'user', 'id': self.user['id']}
        response = self.post_json(
            '/groups/%s/members/bulk-update/' % group['id'],
            data=[
                {
                    'operation_type': 'add',
                    'value': new_user_member
                },
            ],
            expected_code=422
        )
        assert_that(
            response['message'],
            'Can not change group members with <%s>-type' % group_type_name
        )

    def test_should_not_fail_when_deleting_not_existing_user(self):
        self.post_json(
            '/groups/%s/members/bulk-update/' % self.group['id'],
            data=[
                {
                    'operation_type': 'remove',
                    'value': {
                        'type': 'user',
                        'id': 10000
                    }
                },
            ],
            expected_code=200
        )

    @pytest.fixture()
    def users(self):
        range_ = (100, 115)
        users = [self.create_user(department_id=self.department['id'], uid=i) for i in range(*range_)]
        return users

    @pytest.mark.parametrize("one_call", [True, False])
    def test_add_many_delete_many_gives_correct_final_state(self, users: List[dict], one_call: bool):
        new_group = create_group(self.main_connection, org_id=self.organization['id'])
        # берем 2/3 пользователей, кладем сразу
        idx_now = 2 * len(users) // 3
        put_now = users[:idx_now]
        put_later = users[idx_now:]
        if one_call:
            data = [{'operation_type': 'add', 'value': {'type': 'user', 'id': x['id']}} for x in put_now]
            self.post_json('/groups/%s/members/bulk-update/' % new_group['id'], data=data, expected_code=200)
        else:
            for x in put_now:
                data = [{'operation_type': 'add', 'value': {'type': 'user', 'id': x['id']}}]
                self.post_json('/groups/%s/members/bulk-update/' % new_group['id'], data=data, expected_code=200)

        # вторым запросом каждого второго из добавленных удалить
        to_delete = users[:idx_now:2]
        if one_call:
            data = (
                [{'operation_type': 'remove', 'value': {'type': 'user', 'id': x['id']}} for x in to_delete]
                +
                [{'operation_type': 'add', 'value': {'type': 'user', 'id': x['id']}} for x in put_later]
            )
            self.post_json('/groups/%s/members/bulk-update/' % new_group['id'], data=data, expected_code=200)
        else:
            for x in to_delete:
                self.post_json(
                    '/groups/%s/members/bulk-update/' % new_group['id'],
                    data=[{'operation_type': 'remove', 'value': {'type': 'user', 'id': x['id']}}],
                    expected_code=200
                )
            for x in put_later:
                self.post_json(
                    '/groups/%s/members/bulk-update/' % new_group['id'],
                    data=[{'operation_type': 'add', 'value': {'type': 'user', 'id': x['id']}}],
                    expected_code=200
                )
        results = UserGroupMembership(self.main_connection).filter(
            org_id=self.organization['id'],
            group_id=new_group['id']
        ).all()
        actual_ids = {x['user_id'] for x in results}
        expected_ids = {x['id'] for x in users[1:idx_now:2] + users[idx_now:]}
        assert expected_ids == actual_ids

    def test_should_not_fail_when_deleting_not_existing_department(self):
        self.post_json(
            '/groups/%s/members/bulk-update/' % self.group['id'],
            data=[
                {
                    'operation_type': 'remove',
                    'value': {
                        'type': 'department',
                        'id': 10000
                    }
                },
            ],
            expected_code=200
        )

    def test_should_not_fail_when_deleting_not_existing_group(self):
        self.post_json(
            '/groups/%s/members/bulk-update/' % self.group['id'],
            data=[
                {
                    'operation_type': 'remove',
                    'value': {
                        'type': 'group',
                        'id': 10000
                    }
                },
            ],
            expected_code=200
        )

    def test_should_return_prepared_group_members(self):
        response_data = self.post_json(
            '/groups/%s/members/bulk-update/' % self.group['id'],
            data=[
                {
                    'operation_type': 'add',
                    'value': self.new_group_member
                },
                {
                    'operation_type': 'remove',
                    'value': self.members[0]
                },
            ],
            expected_code=200
        )
        expected = list(map(
            prepare_group_member_for_members_view,
            ResourceRelationModel(self.main_connection).find(
                filter_data=dict(
                    resource_id=self.group['resource_id'],
                    org_id=self.group['org_id'],
                ),
                fields=['*', 'user.*', 'group.*', 'department.*']
            )
        ))
        self.assertEqual(response_data, expected)

    def test_mixed_bulk_update(self):
        self.post_json(
            '/groups/%s/members/bulk-update/' % self.group['id'],
            data=[
                {
                    'operation_type': 'add',
                    'value': self.new_group_member
                },
                {
                    'operation_type': 'add',
                    'value': self.new_department_member
                },
                {
                    'operation_type': 'remove',
                    'value': self.members[0]
                },
                {
                    'operation_type': 'remove',
                    'value': self.members[2]
                },
            ],
            expected_code=200
        )
        members = ResourceRelationModel(self.main_connection).find(
            filter_data=dict(
                resource_id=self.group['resource_id'],
                org_id=self.group['org_id'],
            ),
            fields=[
                '*',
                'user.*',
                'group.*',
                'department.*',
            ]
        )
        members = [
            {
                'id': i['object']['id'],
                'type': i['type']
            }
            for i in map(prepare_group_member_for_members_view, members)
        ]
        expected = [self.members[1]] + [self.new_group_member, self.new_department_member]
        assert_that(members, expected)

    def test_should_send_action(self):
        # проверим, что добавляются нужные actions и events
        operations = [
            {
                'operation_type': 'add',
                'value': self.new_group_member
            },
            {
                'operation_type': 'add',
                'value': self.new_department_member
            },
            {
                'operation_type': 'remove',
                'value': self.members[0]
            },
            {
                'operation_type': 'remove',
                'value': self.members[2]
            },
        ]
        self.post_json(
            '/groups/%s/members/bulk-update/' % self.group['id'],
            data=operations,
            expected_code=200,
        )

        events = [e['name'] for e in EventModel(self.main_connection).find()]
        expected = [
            'group_membership_changed',
            'group_group_added',
            'resource_grant_changed',

            'group_membership_changed',
            'department_group_added',
            'resource_grant_changed',

            'group_membership_changed',
            'department_group_deleted',
            'resource_grant_changed',

            'group_membership_changed',
            'group_group_deleted',
            'resource_grant_changed',
        ]
        assert_that(events, equal_to(expected))

        actions = [a['name'] for a in ActionModel(self.main_connection).find()]
        assert_that(actions, equal_to(['group_modify'] * 4))


class TestGroupAdminBulkUpdateView__post(TestCase):
    def setUp(self):
        super(TestGroupAdminBulkUpdateView__post, self).setUp()

        self.group = create_group(
            self.main_connection,
            org_id=self.organization['id']
        )

        group_model = GroupModel(self.main_connection)

        first_user = self.create_user()
        second_user = self.create_user()
        third_user = self.create_user()

        self.members = [
            {
                'type': 'user',
                'id': first_user['id']
            },
            {
                'type': 'user',
                'id': second_user['id']
            },
            {
                'type': 'user',
                'id': third_user['id']
            },
        ]
        self.admins = [
            {
                'type': 'user',
                'id': third_user['id']
            }
        ]
        GroupModel(self.main_connection).update_one(
            org_id=self.group['org_id'],
            group_id=self.group['id'],
            data={
                'members': self.members,
                'admins': self.admins
            }
        )

    def test_should_return_not_found_if_no_group_exists(self):
        response_data = self.post_json(
            '/groups/%s/admins/bulk-update/' % 6,
            data=[
                {
                    'operation_type': 'add',
                    'value': {
                        'type': 'user',
                        'id': 1,
                    },
                },
            ],
            expected_code=404
        )
        assert_that_not_found(response_data)

    def test_group_bulk_update_admins_add_and_delete(self):
        # проверяем, что изменение админов проходит успешно
        group = create_group(
            self.main_connection,
            org_id=self.group['org_id'],
            type='generic',
            admins=[self.user['id']],
        )

        another_user = self.create_user()
        response_data = self.post_json(
            '/groups/%s/admins/bulk-update/' % group['id'],
            data=[
                {
                    'operation_type': 'add',
                    'value': {
                        'type': TYPE_USER,
                        'id': another_user['id'],
                    },
                },
                {
                    'operation_type': 'remove',
                    'value': {
                        'type': TYPE_USER,
                        'id': self.user['id'],
                    },
                },
            ],
            expected_code = 200
        )

        assert_that(
            response_data['admins'],
            contains_inanyorder(
                has_entries(id=another_user['id']),
            )
        )

        get_response = self.get_json('/groups/%s/' % group['id'])
        assert_that(
            get_response['admins'],
            contains_inanyorder(
                has_entries(id=another_user['id']),
            )
        )

    def test_group_bulk_update_admins_void_admins(self):
        # проверяем, что можно оставить группу без админов
        another_user = self.create_user()
        group = create_group(
            self.main_connection,
            org_id=self.group['org_id'],
            type='generic',
            admins=[self.user['id'], another_user['id']],
        )
        response_data = self.post_json(
            '/groups/%s/admins/bulk-update/' % group['id'],
            data=[
                {
                    'operation_type': 'remove',
                    'value': {
                        'type': TYPE_USER,
                        'id': another_user['id'],
                    },
                },
                {
                    'operation_type': 'remove',
                      'value': {
                        'type': TYPE_USER,
                        'id': self.user['id'],
                    },
                },
            ],
            expected_code=200
        )


        # проверим, что в группе по прежнему есть админы, несмотря на
        # неуспешный патч
        response_group = self.get_json('/groups/%s/' % group['id'])
        assert len(response_group['admins']) == 0

    def test_bulk_update_grops_without_admins_should_not_return_error(self):
        # если группа была без админов и раньше - разрешаем её редактировать
        group = create_group(
            self.main_connection,
            org_id=self.group['org_id'],
            type='generic',
            admins=[],
        )

        response_data = self.post_json(
            '/groups/%s/admins/bulk-update/' % group['id'],
            data=[],
            expected_code=200
        )

        # проверим, что в группе по прежнему нет админов
        response_group = self.get_json('/groups/%s/' % group['id'])
        self.assertEqual(response_group['admins'], [])

    def test_grup_bulk_update_admins_with_incorrect_uid(self):
        # проверяем, что нельзя добавить объект с несуществующим uid-ом
        # в качестве админа и при этом оставить группу без админа
        another_user = self.create_user()
        group = create_group(
            self.main_connection,
            org_id=self.group['org_id'],
            type='generic',
            admins=[self.user['id'], another_user['id']],
        )
        response_data = self.post_json(
            '/groups/%s/admins/bulk-update/' % group['id'],
            data=[
                {
                    'operation_type': 'remove',
                    'value': {
                        'type': TYPE_USER,
                        'id': another_user['id'],
                    },
                },
                {
                    'operation_type': 'remove',
                      'value': {
                        'type': TYPE_USER,
                        'id': self.user['id'],
                    },
                },
                {
                    'operation_type': 'add',
                      'value': {
                        'type': TYPE_USER,
                        'id': 12341231,
                    },
                },
            ],
            expected_code=422
        )
        assert_that(
            response_data,
            has_entries(
                code='constraint_validation.objects_not_found',
                message='Some objects of type "{type}" were not found in database',
                params={'type': 'user'},
            )
        )

        # проверим, что в группе по прежнему есть админы, несмотря на
        # неуспешный патч
        response_group = self.get_json('/groups/%s/' % group['id'])
        assert len(response_group['admins']) != 0

    def test_group_bulk_update_admins_with_group_and_department(self):
        # проверяем, что нельзя добавить группу или департамент
        # в качестве админа
        another_user = self.create_user()
        group = create_group(
            self.main_connection,
            org_id=self.group['org_id'],
            type='generic',
            admins=[self.user['id']],
        )

        response_data = self.post_json(
            '/groups/%s/admins/bulk-update/' % group['id'],
            data=[
                {
                    'operation_type': 'add',
                    'value': {
                        'type': TYPE_GROUP,
                        'id': self.group['id'],
                    },
                },
                {
                    'operation_type': 'add',
                    'value': {
                        'type': TYPE_DEPARTMENT,
                        'id': self.department['id'],
                    },
                },
            ],
            expected_code=422
        )

        assert_that(
            response_data,
            has_entries(
                code='constraint_validation.only_users_can_be_group_admins',
                message='Only users can be group admins',
            )
        )

        # проверим, что в группе по прежнему есть админы, несмотря на
        # неуспешный патч
        response_group = self.get_json('/groups/%s/' % group['id'])
        assert len(response_group['admins']) == 1


    def test_group_bulk_update_delte_admin_not_exist(self):
        # проверяем, что не админ не удалится
        another_user = self.create_user()
        group = create_group(
            self.main_connection,
            org_id=self.group['org_id'],
            type='generic',
            admins=[self.user['id']],
        )

        response_data = self.post_json(
            '/groups/%s/admins/bulk-update/' % group['id'],
            data=[
                {
                    'operation_type': 'remove',
                    'value': {
                        'type': TYPE_USER,
                        'id': another_user['id'],
                    },
                },
            ],
            expected_code=422
        )
        assert_that(
            response_data,
            has_entries(
                code='can_not_remove_admin',
                message='user "{id}" is not admin'
            )
        )

        # проверим, что в группе по прежнему есть админы, несмотря на
        # неуспешный патч
        response_group = self.get_json('/groups/%s/' % group['id'])
        assert len(response_group['admins']) == 1

class TestGroupAliasesListView__post(TestCase):
    api_version = 'v7'

    def test_add_alias(self):
        # добавление алиаса группы

        old_aliases = ['alias1']
        added_alias = 'alias2'

        group = self.create_group(
            org_id=self.organization['id'],
            aliases=old_aliases,
            uid=123,
        )

        with patch('intranet.yandex_directory.src.yandex_directory.core.views.groups.action_group_modify'):

            response_data = self.post_json(
                '/groups/%s/aliases/' % group['id'],
                data={
                    'name': added_alias
                },
            )
            # добавили алиас в Паспорт
            assert_called_once(
                self.mocked_passport.alias_add,
                group['uid'],
                added_alias,
            )

        # алиасы обновились в директории
        assert_that(
            GroupModel(self.main_connection).count(
                filter_data={
                    'id': group['id'],
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
                id=group['id']
            )
        )
        events = EventModel(self.main_connection).find({
            'org_id': group['org_id'],
            'revision': self.get_org_revision(self.organization['id']),
        })
        assert_that(events, has_length(1))
        assert_that(
            events[0],
            has_entries(
                name='group_alias_added',
                object=has_entries(
                    id=group['id'],
                    aliases=has_item(added_alias),
                ),
                content=has_entries(alias_added=added_alias)
            )
        )


class TestGroupAliasesDetailView__delete(TestCase):
    def _delete_alias(self, delete_func=None):
        """
        Общий тест для удаления алиасов
        :param delete_func: Функция для мока запрос на удаления в паспорте
        :type delete_func: function
        """

        old_aliases = ['alias1', 'alias2']
        deleted_alias = 'alias2'

        group = self.create_group(
            org_id=self.organization['id'],
            aliases=old_aliases,
            label='group_group',
            uid=123,
        )

        if delete_func:
            self.mocked_passport.alias_delete = delete_func

        with patch('intranet.yandex_directory.src.yandex_directory.core.views.groups.action_group_modify'):
            self.delete_json(
                '/groups/%s/aliases/%s/' % (group['id'], deleted_alias),
            )
            # удалили алиас в Паспорте
            assert_called_once(
                self.mocked_passport.alias_delete,
                group['uid'],
                deleted_alias,
            )

        # алиасы обновились в директории
        assert_that(
            GroupModel(self.main_connection).count(
                filter_data={
                    'id': group['id'],
                    'alias': deleted_alias
                }
            ),
            equal_to(0)
        )
        events = EventModel(self.main_connection).find({
            'org_id': group['org_id'],
            'revision': self.get_org_revision(self.organization['id']),
        })
        assert_that(events, has_length(1))
        assert_that(
            events[0],
            has_entries(
                name='group_alias_deleted',
                object=has_entries(
                    id=group['id'],
                    aliases=not_(has_item(deleted_alias)),
                ),
                content=has_entries(alias_deleted=deleted_alias)
            )
        )

    def test_delete_alias(self):
        # удаление алиаса для группы

        self._delete_alias()

    def test_delete_alias_no_in_passport(self):
        # удаление алиаса который есть в директории, но нет в Паспорте

        self._delete_alias(
            Mock(side_effect=AliasNotFound)
        )
