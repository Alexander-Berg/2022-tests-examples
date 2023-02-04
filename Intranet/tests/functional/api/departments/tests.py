# -*- coding: utf-8 -*-
import datetime
import json
from functools import partial

from hamcrest import (
    assert_that,
    has_entries,
    has_length,
    has_entry,
    is_,
    has_items,
    instance_of,
    all_of,
    not_none,
    is_not,
    anything,
    equal_to,
    contains_inanyorder,
    contains,
    has_key,
    not_,
    has_item,
)
from unittest.mock import ANY
from unittest.mock import (
    patch,
    Mock,
)

from testutils import (
    get_auth_headers,
    PaginationTestsMixin,
    create_organization,
    TestCase,
    TestDepartments,
    TestYandexTeamOrgMixin,
    create_department,
    assert_called_once,
    override_settings,
    oauth_client,
    get_oauth_headers,
    TestOrganizationWithoutDomainMixin,
    assert_not_called,
)
from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.auth.scopes import scope
from intranet.yandex_directory.src.yandex_directory.common.models.types import ROOT_DEPARTMENT_ID
from intranet.yandex_directory.src.yandex_directory.common.utils import (
    lstring,
    format_date,
    Ignore,
)
from intranet.yandex_directory.src.yandex_directory.core.actions import action
from intranet.yandex_directory.src.yandex_directory.core.events import event
from intranet.yandex_directory.src.yandex_directory.core.models import OrganizationModel
from intranet.yandex_directory.src.yandex_directory.core.models.action import ActionModel
from intranet.yandex_directory.src.yandex_directory.core.models.department import (
    DepartmentModel,
)
from intranet.yandex_directory.src.yandex_directory.core.models.event import EventModel
from intranet.yandex_directory.src.yandex_directory.core.models.group import UserGroupMembership
from intranet.yandex_directory.src.yandex_directory.core.models.user import UserModel
from intranet.yandex_directory.src.yandex_directory.core.utils import (
    only_fields,
    except_fields,
)
from intranet.yandex_directory.src.yandex_directory.core.utils import (
    prepare_department,
    build_email,
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
from intranet.yandex_directory.src.yandex_directory.core.models.service import enable_service, ServiceModel
from intranet.yandex_directory.src import settings

DEPARTMENT_NAME = {
    'ru': 'Департамент',
    'en': 'Department',
}

ANOTHER_DEPARTMENT_NAME = {
    'ru': 'Департамент другой',
    'en': 'Department another',
}
DEPARTMENT_PRIVATE_FIELDS = ['name_plain', 'description_plain']


class TestDepartmentList__get(PaginationTestsMixin, TestCase):
    entity_list_url = '/departments/'
    entity_model = DepartmentModel
    prepare_entity_for_api_response = lambda self, e: prepare_department(
        self.main_connection,
        e,
        api_version=1,
    )

    def create_entity(self):
        self.entity_counter += 1

        return DepartmentModel(self.main_connection).create(
            name={'en': 'Department %s' % self.entity_counter},
            org_id=self.organization['id']
        )

    def test_simple(self):
        department_model = DepartmentModel(self.main_connection)
        dep_one = department_model.create(
            id=2,
            name=DEPARTMENT_NAME,
            org_id=self.organization['id']
        )
        department_model.create(
            id=3,
            name=DEPARTMENT_NAME,
            org_id=self.organization['id'],
            parent_id=dep_one['id']
        )

        data = self.get_json('/departments/')
        result = data.get('result')
        self.assertEqual(len(result), 3)

    def test_ordering_by_name(self):
        dep_model = DepartmentModel(self.main_connection)

        dep_one = dep_model.create(
            id=2,
            name={'en': 'A department'},
            org_id=self.organization['id']
        )
        dep_one = dep_model.create(
            id=9,
            name={'en': 'a department'},
            org_id=self.organization['id']
        )
        dep_model.create(
            id=3,
            name={'en': 'b department'},
            org_id=self.organization['id'],
            parent_id=dep_one['id']
        )
        dep_model.create(
            id=4,
            name={'en': 'B department'},
            org_id=self.organization['id'],
            parent_id=dep_one['id']
        )

        data = self.get_json('/departments/?ordering=name')
        result = data.get('result')
        self.assertEqual(len(result), 5)
        self.assertEqual([r['id'] for r in result], [9, 2, 3, 4, 1])

        data = self.get_json('/departments/?ordering=-name')
        result = data.get('result')
        self.assertEqual(len(result), 5)
        self.assertEqual([r['id'] for r in result], [9, 2, 3, 4, 1][::-1])

    def test_get_tracker_license(self):
        dep = DepartmentModel(self.main_connection).create(
            org_id=self.organization['id'],
            name={
                'ru': 'Group'
            },
        )
        dep_2 = DepartmentModel(self.main_connection).create(
            org_id=self.organization['id'],
            name={
                'ru': 'Group2'
            },
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
                    'type': 'department',
                    'id': dep['id']
                },
            ],
        )
        self.api_version = 'v6'
        data = self.get_json('/departments/?fields=tracker_license,org_id&ordering=tracker_license')
        department_data = [i for i in data['result'] if i['id'] == dep['id']][0]
        assert department_data['tracker_license'] is True
        department_2_data = [i for i in data['result'] if i['id'] == dep_2['id']][0]
        assert department_2_data['tracker_license'] is False

    def test_get_works_when_only_org_id_header_was_given(self):
        # Проверяем, что внутренние сервисы могут получать департаменты, указывая только X-Org-ID.
        org_id = self.organization['id']
        DepartmentModel(self.main_connection).create(
            id=2,
            name=DEPARTMENT_NAME,
            org_id=org_id,
        )

        headers = get_auth_headers(as_org=org_id)
        data = self.get_json('/departments/', headers=headers)
        assert len(data['result']) == 2

    def test_should_show_items_of_current_user_organization(self):
        another_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']

        DepartmentModel(self.main_connection).create(
            id=2,
            name=DEPARTMENT_NAME,
            org_id=another_organization['id']
        )

        data = self.get_json('/departments/')
        self.assertNotIn(2, [i['id'] for i in data['result']])

    def test_filter_by_id(self):
        department_model = DepartmentModel(self.main_connection)
        dep_one = department_model.create(
            id=2,
            name=DEPARTMENT_NAME,
            org_id=self.organization['id']
        )
        department_model.create(
            id=3,
            name=DEPARTMENT_NAME,
            org_id=self.organization['id'],
            parent_id=dep_one['id']
        )

        ids = [self.department['id'], dep_one['id']]
        response = self.client.get(
            '/departments/',
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
        department_model = DepartmentModel(self.main_connection)
        dep_one = department_model.create(
            id=2,
            name=DEPARTMENT_NAME,
            org_id=self.organization['id'],
            uid=10
        )
        department_model.create(
            id=3,
            name=DEPARTMENT_NAME,
            org_id=self.organization['id'],
            parent_id=dep_one['id']
        )

        uids = [10]
        response = self.client.get(
            '/departments/',
            headers=get_auth_headers(),
            query_string={
                'uid': ','.join(map(str, uids))
            }
        )
        response_data = json.loads(response.data).get('result')
        self.assertEqual(len(response_data), len(uids))

        response_uids = [i['uid'] for i in response_data]
        self.assertEqual(
            sorted(response_uids),
            sorted(uids)
        )

    @override_settings(INTERNAL=False)
    def test_error_filter_by_uid_not_internal(self):
        department_model = DepartmentModel(self.main_connection)
        dep_one = department_model.create(
            id=2,
            name=DEPARTMENT_NAME,
            org_id=self.organization['id'],
            uid=10
        )
        department_model.create(
            id=3,
            name=DEPARTMENT_NAME,
            org_id=self.organization['id'],
            parent_id=dep_one['id']
        )

        uids = [10]
        with oauth_client(client_id="kek", uid=self.user['id'], scopes=[scope.read_departments]):
            response = self.client.get(
                '/departments/',
                headers=get_oauth_headers(),
                query_string={
                    'uid': ','.join(map(str, uids))
                }
            )
        response_data = json.loads(response.data).get('result')
        assert(len(response_data) > 1)

    def test_filter_by_parent_id(self):
        department_model = DepartmentModel(self.main_connection)
        dep_one = department_model.create(
            id=2,
            name=DEPARTMENT_NAME,
            org_id=self.organization['id']
        )
        dep_two = department_model.create(
            id=3,
            name=DEPARTMENT_NAME,
            org_id=self.organization['id'],
            parent_id=dep_one['id']
        )
        department_model.create(
            id=4,
            name=DEPARTMENT_NAME,
            org_id=self.organization['id'],
            parent_id=dep_two['id']
        )
        department_model.create(
            id=5,
            name=DEPARTMENT_NAME,
            org_id=self.organization['id'],
            parent_id=dep_two['id']
        )

        parent_ids = [dep_one['id'], dep_two['id']]
        response = self.client.get(
            '/departments/',
            headers=get_auth_headers(),
            query_string={
                'parent_id': ','.join(map(str, parent_ids))
            }
        )
        response_data = json.loads(response.data).get('result')
        self.assertEqual(len(response_data), 3)

        ids = [i['id'] for i in response_data]
        expected = [i['id'] for i in department_model.find({'parent_id': parent_ids})]
        self.assertEqual(sorted(ids), sorted(expected))

    def test_head_of_department_returned_by_list_view(self):
        org_id = self.organization['id']
        department = self.department

        UserGroupMembership(self.main_connection).create(
            org_id=org_id,
            group_id=department['heads_group_id'],
            user_id=self.user['id'])

        data = self.get_json('/departments/')
        result = data['result']
        self.assertEqual(1, len(result))
        self.assertTrue('head' in result[0])
        self.assertEqual(
            self.user['name'],
            result[0]['head']['name']
        )
        # проверяем, что поле department не раскрыто у руководителя департамента
        # https://st.yandex-team.ru/DIR-181
        self.assertTrue('department_id' in result[0]['head'])
        self.assertTrue('department' not in result[0]['head'])


class TestDepartmentList__get_6(PaginationTestsMixin, TestCase):
    entity_list_url = '/departments/'
    entity_model = DepartmentModel
    prepare_entity_for_api_response = lambda self, e: prepare_department(
        self.main_connection,
        e,
        api_version=6,
    )
    api_version = 'v6'

    def create_entity(self):
        self.entity_counter += 1

        return DepartmentModel(self.main_connection).create(
            name={'en': 'Department %s' % self.entity_counter},
            org_id=self.organization['id']
        )

    def test_fields_returned_by_list_view(self):
        """Проверяем, что ручка возвращает все перечисленные поля.
        """
        fields = list(DepartmentModel.all_fields)
        for field in DEPARTMENT_PRIVATE_FIELDS:
            fields.remove(field)
        # Убираем лишнее поле 'parent_id', так как уже есть поле 'parent'.
        # Подготавливая вывод, ручка выбирает только одно из этих двух полей.
        fields.remove('parent_id')

        headers = get_auth_headers()
        response = self.get_json('/departments/',
            headers=headers,
            query_string={
                'fields': ','.join(map(str, fields))
            })

        assert_that(len(response['result']), 1)
        assert_that(
           list(response['result'][0].keys()),
           contains_inanyorder(*fields),
        )

        # Проверяем каждое поле по отдельности.
        fields.append('parent_id')
        for field in fields:
            response = self.get_json('/departments/',
            headers=headers,
            query_string={
                'fields': field,
            })

            assert_that(len(response['result']), 1)
            assert_that(response['result'][0], has_key(field))

    def test_order_by_name(self):
        # проверим что работает выдача при поиске с сортировкой
        # для случая https://st.yandex-team.ru/DIR-5215

        dep1 = self.create_department(parent_id=self.department['id'])
        self.create_department(parent_id=dep1['id'])
        self.create_department(parent_id=dep1['id'])
        self.create_department(parent_id=dep1['id'])
        self.create_department(parent_id=dep1['id'])

        self.get_json(
            '/departments/?fields=id,name,removed,parent,parents&ordering=name&parent_id={}'.format(dep1['id'])
        )


class TestDepartmentList__post(TestOrganizationWithoutDomainMixin, TestCase):
    def setUp(self):
        super(TestDepartmentList__post, self).setUp()

        self.department = DepartmentModel(self.main_connection).create(
            name=DEPARTMENT_NAME,
            org_id=self.organization['id']
        )
        self.label = 'department_label'

    def test_with_parent_id(self):
        data = {
            'label': self.label,
            'name': DEPARTMENT_NAME,
            'parent_id': self.department['id'],
        }
        response_data = self.post_json('/departments/', data)
        self.assertEqual({'name': DEPARTMENT_NAME,
                          'parent': {
                              'name': self.department['name'],
                              'id': self.department['id'],
                              'parent_id': None,
                              'removed': False,
                              'external_id': None,
                          },
                          'description': {'ru': ''}},
                         only_fields(response_data,
                                     'name', 'parent', 'description'))

    def test_with_external_id(self):
        # указываем external_id при создании отдела
        external_id = 'external_id'
        data = {
            'label': self.label,
            'name': DEPARTMENT_NAME,
            'parent_id': self.department['id'],
            'external_id': external_id
        }
        response_data = self.post_json('/departments/', data)
        self.assertEqual({'name': DEPARTMENT_NAME,
                          'parent': {
                              'name': self.department['name'],
                              'id': self.department['id'],
                              'parent_id': None,
                              'removed': False,
                              'external_id': None,
                          },
                          'external_id': external_id,
                          'description': {'ru': ''}},
                         only_fields(response_data,
                                     'name', 'parent', 'external_id', 'description'))

    def test_with_parent_object(self):
        data = {
            'label': self.label,
            'name': DEPARTMENT_NAME,
            'parent': {'id': self.department['id']},
        }
        response_data = self.post_json('/departments/', data)
        self.assertEqual({'name': DEPARTMENT_NAME,
                          'parent': {
                              'name': self.department['name'],
                              'id': self.department['id'],
                              'parent_id': None,
                              'removed': False,
                              'external_id': None,
                          },
                          'description': {'ru': ''}},
                         only_fields(response_data,
                                     'name', 'parent', 'description'))

    def test_with_maillist_type(self):
        data = {
            'label': self.label,
            'name': DEPARTMENT_NAME,
            'parent_id': self.department['id'],
            'maillist_type': 'both',
        }
        response_data = self.post_json('/departments/', data)
        self.assertEqual({'name': DEPARTMENT_NAME,
                          'parent': {
                              'name': self.department['name'],
                              'id': self.department['id'],
                              'parent_id': None,
                              'removed': False,
                              'external_id': None,
                          },
                          'description': {'ru': ''},
                          'maillist_type': 'both'},
                         only_fields(response_data,
                                     'name', 'parent', 'description', 'maillist_type'))

    def test_with_wrong_parent_id(self):
        data = {
            'label': self.label,
            'name': DEPARTMENT_NAME,
            'parent_id': 123
        }
        data = self.post_json('/departments/', data, expected_code=422)
        assert_that(
            data,
            has_entries(
                code='parent_department_not_found',
                message='Unable to find parent department with id={id}',
                params={'id': 123},
            )
        )

    def test_without_parent_id(self):
        data = {
            'label': self.label,
            'name': DEPARTMENT_NAME,
        }
        response = self.client.post(
            '/departments/',
            data=json.dumps(data),
            content_type='application/json',
            headers=get_auth_headers()
        )
        self.assertEqual(response.status_code, 422)
        response_data = json.loads(response.data)
        assert_that(
            response_data,
            has_entries(
                code='schema_validation_error',
            )
        )

    def test_without_label(self):
        data = {
            'name': DEPARTMENT_NAME,
            'parent_id': 1,
        }
        response = self.post_json('/departments/', data)

        self.assertEqual(response.get('label'), None)
        self.assertEqual(response.get('email'), None)
        self.assertEqual(response.get('uid'), None)

    def test_post_department_with_empty_label(self):
        data = {
            'parent_id': 1,
            'name': DEPARTMENT_NAME,
            'label': ' \t\v'
        }
        response = self.post_json('/departments/', data)

        self.assertEqual(response.get('label'), None)
        self.assertEqual(response.get('email'), None)
        self.assertEqual(response.get('uid'), None)

    def test_incorrect_label_in_passport(self):
        max_length_login_for_pdd = 41
        label = '1'*max_length_login_for_pdd
        data = {
            'name': DEPARTMENT_NAME,
            'label': label,
            'parent_id': self.department['id'],
        }
        self.mocked_passport.validate_login.side_effect = LoginLong
        self.post_json('/departments/', data, expected_code=422)

    def test_head_id_could_be_none_when_creating_department(self):
        data = dict(name=DEPARTMENT_NAME, parent_id=1, head_id=None, label=self.label)
        response = self.post_json('/departments/', data)
        assert_that(response, has_entry('head', None))

    def test_error_on_wrong_head_id_on_create(self):
        data = dict(name=DEPARTMENT_NAME, parent_id=1, head_id=887766, label=self.label)
        response = self.post_json('/departments/', data, expected_code=422)
        assert_that(
            response,
            has_entries(
                code='head_of_department_not_found',
                message='Unable to find person with head_id={uid}',
            )
        )

    def test_conflict_error(self):
        # проверим, что больше одного отдела с одним и тем же
        # label создать нельзя
        data = {
            'parent_id': 1,
            'name': DEPARTMENT_NAME,
            'label': self.label,
        }
        # первой создание проходит отлично
        self.post_json('/departments/', data)

        # а при втором мы должны получить ошибку
        response = self.post_json('/departments/', data, expected_code=409)
        assert_that(
            response,
            has_entries(
                code='some_department_has_this_label',
                message='Some department already uses "{login}" as label',
                params={'login': self.label},
            )
        )

        # и если это такой же label (с другим регистром),
        # должна вернуться ошибка
        data['label'] = data['label'].upper()
        response = self.post_json('/departments/', data, expected_code=409)
        assert_that(
            response,
            has_entries(
                code='some_department_has_this_label',
                message='Some department already uses "{login}" as label',
                params={'login': self.label},
            )
        )

    def not_unique_label_with_nickname(self):
        not_uniq_label = 'nickname'
        self.create_user(
            nickname=not_uniq_label,
            email=build_email(self.main_connection, not_uniq_label, org_id=self.organization['id']),
        )
        data = {
            'parent_id': 1,
            'name': self.department_name,
            'label': not_uniq_label,
        }
        response_data = self.post_json('/departments/', data, expected_code=409)
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
        not_uniq_label = 'login'

        another_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']

        self.another_group = DepartmentModel(self.main_connection).create(
            label=not_uniq_label,
            name=DEPARTMENT_NAME,
            org_id=another_organization['id']
        )
        data = {
            'parent_id': 1,
            'name': self.department_name,
            'label': not_uniq_label,
        }
        self.post_json('/departments/', data)

    def test_not_unique_label_with_group(self):
        not_uniq_label = 'group_label'
        self.create_group(label=not_uniq_label)
        data = {
            'name': self.department_name,
            'label': not_uniq_label,
            'parent_id': 1,
        }
        response_data = self.post_json(
            '/departments/',
            data,
            expected_code=409
        )
        assert_that(
            response_data,
            has_entries(
                code='some_group_has_this_label',
                message='Some group already uses "{login}" as label',
                params={'login': not_uniq_label},
            )
        )

    def test_post_without_name(self):
        response = self.client.post(
            '/departments/',
            data=json.dumps({
                'parent_id': self.department['id'],
                'label': self.label,
            }),
            content_type='application/json',
            headers=get_auth_headers()
        )
        self.assertEqual(response.status_code, 422)
        response_data = json.loads(response.data)
        assert_that(
            response_data,
            has_entries(
                code='schema_validation_error',
            )
        )

    def test_post_with_invalid_name(self):
        response = self.client.post(
            '/departments/',
            data=json.dumps({
                'label': self.label,
                'parent_id': self.department['id'],
                'name': 'sosisa'
            }),
            content_type='application/json',
            headers=get_auth_headers()
        )
        self.assertEqual(response.status_code, 422)
        response_data = json.loads(response.data)
        assert_that(
            response_data,
            has_entries(
                code='schema_validation_error',
            )
        )

    def test_returned_object_has_name_description_and_parent(self):
        data = {
            'label': self.label,
            'name': DEPARTMENT_NAME,
            'description': lstring('Some department'),
            'parent_id': self.department['id']
        }
        response_data = self.post_json('/departments/', data)

        assert_that(response_data,
                    has_entries(name=lstring('Департамент', 'Department'),
                                description=lstring('Some department'),

                                parent=is_(dict),
                                parents=has_items(instance_of(dict))))

    def test_post_events(self):
        self.clean_actions_and_events()

        event_model = EventModel(self.main_connection)
        action_model = ActionModel(self.main_connection)

        data = {
            'label': self.label,
            'name': DEPARTMENT_NAME,
            'parent_id': self.department['id'],
            'head_id': self.user['id'],
        }
        self.post_json('/departments/', data)

        events = [x['name'] for x in event_model.find()]
        events.sort()
        expected = ['department_added', 'department_department_added', 'group_added', 'user_group_added']
        assert_that(events, equal_to(expected))

        action = action_model.find()[0]['name']
        assert_that(action, equal_to('department_add'))

    def test_create_department_with_head(self):
        data = dict(name=DEPARTMENT_NAME, parent_id=1, head_id=self.user['id'], label=self.label)
        response = self.post_json('/departments/', data)
        assert_that(response.get('head'), has_entry('id', self.user['id']))

        created_department = DepartmentModel(self.main_connection).get(
            department_id=response['id'],
            org_id=self.organization['id']
        )
        user_is_head = UserGroupMembership(self.main_connection).get(
            self.user['id'],
            self.organization['id'],
            created_department['heads_group_id'],
        )
        assert_that(user_is_head, is_not(None))

    def test_post_department_with_uppercase_label(self):
        # проверим, что label с uppercase-ом переведется в нижний регистр
        uppercase_label = 'UPPERCASElabel'
        data = {
            'label': uppercase_label,
            'name': DEPARTMENT_NAME,
            'parent_id': self.department['id'],
        }
        response_data = self.post_json('/departments/', data)
        assert_that(response_data,
                    has_entries(name=DEPARTMENT_NAME,
                                label=uppercase_label.lower()))

        # проверим, что label с содержимым uppercase-а, но приведенным в нижний
        # регистр - не добавится повторно
        data = {
            'label': uppercase_label.lower(),
            'name': DEPARTMENT_NAME,
            'parent_id': self.department['id'],
        }
        response_data = self.post_json('/departments/', data, expected_code=409)

    def test_post_department_org_without_domains_with_label_should_return_error(self):
        # Пытаемся создать отдел для организации с has_owned_domains=False и передаем label
        data = {
            'name': DEPARTMENT_NAME,
            'label': 'some_label',
            'parent_id': 1,
        }
        headers = get_auth_headers(as_outer_admin={
            'id': self.yandex_admin['id'],
            'org_id': self.yandex_organization['id'],
        })
        self.post_json(
            '/departments/',
            data,
            headers=headers,
            expected_code=422,
        )

    def test_post_department_org_without_domain_without_label(self):
        # Создаем отдел для организации с has_owned_domains=False и не передаем label
        data = {
            'name': DEPARTMENT_NAME,
            'parent_id': 1,
        }
        headers = get_auth_headers(as_outer_admin={
            'id': self.yandex_admin['id'],
            'org_id': self.yandex_organization['id'],
        })
        response_data = self.post_json('/departments/', data, headers=headers)
        assert_that(
            response_data,
            has_entries(
                uid=None,
                email=None,
                label=None,
                name=DEPARTMENT_NAME,
            )
        )


class TestDepartmentList__maillist_service(TestCase):
    maillist_management = True

    def test_create_department(self):
        # создаем отдел в организации под управлением нового сервиса рассылок
        self.clean_actions_and_events()

        data = {
            'label': self.label,
            'name': DEPARTMENT_NAME,
            'parent_id': self.department['id'],
            'head_id': self.user['id'],
        }
        department = self.post_json('/departments/', data)

        #  рассылка создана напрямую в паспорте и у нее есть uid
        assert_called_once(
            self.mocked_passport.maillist_add,
            login=self.label,
            domain=self.organization_domain,
        )

        assert_that(
            department,
            has_entries(uid=not_none())
        )

        assert_that(
            ActionModel(self.main_connection).filter(name=action.department_add).all(),
            contains(
                has_entries(
                    object=has_entries(
                        uid=department['uid']
                    )
                )
            )
        )

        assert_that(
            EventModel(self.main_connection).filter(name=event.department_added).all(),
            contains(
                has_entries(
                    object=has_entries(
                        uid=department['uid']
                    )
                )
            )
        )

    def test_deleting_department_with_maillist_service(self):
        # при удалении департамента в организации с включенным сервисов рассылок,
        # удалим рассылку из паспорта и у себя
        data = {
            'label': self.label,
            'name': DEPARTMENT_NAME,
            'parent_id': self.department['id'],
            'head_id': self.user['id'],
        }
        department = self.post_json('/departments/', data)

        #  рассылка создана напрямую в паспорте и у нее есть uid
        assert_that(
            department,
            has_entries(uid=not_none())
        )
        self.clean_actions_and_events()

        self.delete_json('/departments/%s/' % department['id'])

        assert_called_once(
            self.mocked_passport.maillist_delete,
            department['uid'],
        )

        assert_that(
            EventModel(self.main_connection).filter(name=event.department_deleted).all(),
            contains(
                has_entries(
                    object=has_entries(
                        uid=department['uid']
                    )
                )
            )
        )

        fresh_department = DepartmentModel(self.main_connection).get(
            department_id=department['id'],
            org_id=self.organization['id'],
            removed=True,
        )
        assert not fresh_department

    def test_patch_label(self):
        data = {
            'label': self.label,
            'name': DEPARTMENT_NAME,
            'parent_id': self.department['id'],
            'head_id': self.user['id'],
        }
        department = self.post_json('/departments/', data)

        assert_that(
            department,
            has_entries(
                uid=not_none(),
                label=self.label,
            )
        )
        new_label = 'new_label'
        response_data = self.patch_json(
            '/departments/%s/' % department['id'],
            data={
                'label': new_label
            }
        )

        assert response_data['label'] == new_label
        assert response_data['uid'] != department['uid']

        assert_called_once(
            self.mocked_passport.maillist_delete,
            department['uid'],
        )

    def test_patch_label_from_none(self):
        data = {
            'name': DEPARTMENT_NAME,
            'parent_id': self.department['id'],
            'head_id': self.user['id'],
        }
        department = self.post_json('/departments/', data)

        assert_that(
            department,
            has_entries(
                uid=None,
                label=None,
                email=None,
            )
        )
        new_label = 'new_label'
        response_data = self.patch_json(
            '/departments/%s/' % department['id'],
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
            'name': DEPARTMENT_NAME,
            'parent_id': self.department['id'],
            'head_id': self.user['id'],
        }
        department = self.post_json('/departments/', data)

        assert_that(
            department,
            has_entries(
                uid=not_none(),
                label=self.label,
            )
        )
        new_label = None
        response_data = self.patch_json(
            '/departments/%s/' % department['id'],
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
            department['uid'],
        )

        assert_that(
            response_data,
            has_entries(
                label=None,
                uid=None,
            )
        )


class TestDepartmentListYandexTeamOrg__post(TestYandexTeamOrgMixin, TestCase):

    def setUp(self):
        super(TestDepartmentListYandexTeamOrg__post, self).setUp()
        self.name = {
            'ru': 'Название департамента в yt-организации',
            'en': 'YT-org department name'
        }
        settings.YA_TEAM_ORG_IDS = {self.organization['id']}

    def test_create_department_in_yt_org_without_label(self):
        data = {
            'parent_id': 1,
            'name': self.name,
        }
        self.post_json('/departments/', data, expected_code=403)


class TestDepartmentDetail__get(TestCase):
    def test_existing_department(self):
        another_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']

        department_model = DepartmentModel(self.main_connection)
        parent_department = department_model.create(
            id=2,
            name=ANOTHER_DEPARTMENT_NAME,
            org_id=self.organization['id']
        )
        department = department_model.create(
            id=3,
            name=ANOTHER_DEPARTMENT_NAME,
            org_id=self.organization['id'],
            parent_id=parent_department['id'],
            external_id='external_id'
        )
        department_model.create(
            id=3,
            name=DEPARTMENT_NAME,
            org_id=another_organization['id']
        )
        data = self.get_json('/departments/%s/' % department['id'])
        expected = prepare_department(
            self.main_connection,
            department_model.get(
                department_id=department['id'],
                org_id=self.organization['id'],
                fields=[
                    '*',
                    'parent.*',
                    'head.*',
                    'parents.*',
                ]
            ),
            api_version=1,
        )
        self.assertEqual(expected, data)

    def test_internal_service_can_get_info_about_department(self):
        department = DepartmentModel(self.main_connection).create(
            id=2,
            name=ANOTHER_DEPARTMENT_NAME,
            org_id=self.organization['id']
        )
        headers = get_auth_headers(as_org=self.organization)
        data = self.get_json('/departments/%s/' % department['id'], headers=headers)
        assert data['id'] == department['id']

    def test_not_existing_department(self):
        DepartmentModel(self.main_connection).create(
            id=321,
            name=ANOTHER_DEPARTMENT_NAME,
            org_id=self.organization['id']
        )
        data = self.get_json('/departments/123/', expected_code=404)
        expected = {
            'message': 'Not found',
            'code': 'not_found',
        }
        self.assertEqual(expected, data)

    def test_head_of_department_returned_by_detail_view(self):
        org_id = self.organization['id']
        department = self.department

        UserGroupMembership(self.main_connection).create(
            org_id=org_id,
            group_id=department['heads_group_id'],
            user_id=self.user['id']
        )

        data = self.get_json('/departments/%s/' % department['id'])
        self.assertTrue('head' in data)
        self.assertEqual(
            self.user['name'],
            data['head']['name']
        )

    def test_department_id_is_correct(self):
        self.get_json('/departments/v8/', expected_code=404)


class TestDepartmentDetail__get_6(TestCase):
    api_version = 'v6'

    def test_fields_returned_by_detail_view(self):
        """Проверяем, что ручка возвращает все перечисленные поля.
        """
        fields = list(DepartmentModel.all_fields)
        for field in DEPARTMENT_PRIVATE_FIELDS:
            fields.remove(field)
        # Убираем лишнее поле 'parent_id', так как уже есть поле 'parent'.
        # Подготавливая вывод, ручка выбирает только одно из этих двух полей.
        fields.remove('parent_id')
        dep = self.create_department(parent_id=self.department['id'], label='test-dep')
        headers = get_auth_headers()
        response = self.get_json('/departments/{department_id}/'.format(
            department_id=dep['id']),
            headers=headers,
            query_string={
                'fields': ','.join(map(str, fields))
            })

        assert_that(
           list(response.keys()),
           contains_inanyorder(*fields),
        )

        # Проверяем каждое поле по отдельности.
        fields.append('parent_id')
        for field in fields:
            response = self.get_json('/departments/{department_id}/'.format(
            department_id=self.department['id']),
            headers=headers,
            query_string={
                'fields': field,
            })

            assert_that(response, has_key(field))


class TestDepartmentDetail__get_10(TestCase):
    api_version = 'v10'

    def test_department_head_returns(self):
        headers = get_auth_headers()
        dep = self.create_department(
            parent_id=self.department['id'],
            label='test-dep',
            head_id=self.user['id'],
        )

        response = self.get_json(
            uri='/users/%s/?fields=contacts' % self.user['id'],
            headers=headers,
        )
        assert_that(response, has_entries(contacts=not_none()))

        response = self.get_json(
            uri='/departments/%s/?fields=head.contacts' % dep['id'],
            headers=headers,
        )
        assert_that(
            response,
            has_entry('head', has_entry('contacts', not_none())),
        )

        # Проверяем что в head не попало ничего лишнего
        assert_that(
            response,
            has_entry(
                'head',
                not_(has_entry(all_of(not_('contacts'), not_('id')), ANY)),
            ),
        )


class TestDepartmentDetail__patch(TestCase):
    def setUp(self):
        super(TestDepartmentDetail__patch, self).setUp()
        department_model = DepartmentModel(self.main_connection)
        department = department_model.create(
            id=2,
            name={'ru': 'IT'},
            org_id=self.organization['id'],
            parent_id=1
        )
        self.department = department_model.get(
            department_id=department['id'],
            org_id=self.organization['id'],
            fields=[
                '*',
                'parent.*',
                'head.*',
                'parents.*',
            ]
        )
        self.prepared_department = prepare_department(
            self.main_connection,
            self.department,
            api_version=1,
        )

        self.other_department = department_model.create(
            id=3,
            name={'ru': 'Another department'},
            org_id=self.organization['id'],
            parent_id=1
        )

    def test_should_return_not_found_if_no_department_exists(self):
        response_data = self.patch_json(
            '/departments/1000/',
            data={
                'name': {'name': 'some name'}
            },
            expected_code=404
        )
        self.assertEqual(response_data.get('message'), 'Not found')

    def test_patch_name__with_value(self):
        new_name = {
            'ru': 'New IT'
        }
        response_data = self.patch_json(
            '/departments/%s/' % self.department['id'],
            data={
                'name': new_name
            }
        )
        self.prepared_department['name'] = new_name
        self.assertEqual(response_data.get('name'), new_name)
        self.assertEqual(response_data, self.prepared_department)

    def test_patch_name__with_null_should_not_be_allowed(self):
        self.patch_json(
            '/departments/%s/' % self.department['id'],
            data={
                'name': None
            },
            expected_code=422
        )

    def test_patch_name__with_empty_string_should_not_be_allowed(self):
        self.patch_json(
            '/departments/%s/' % self.department['id'],
            data={
                'name': ''
            },
            expected_code=422,
        )

    def test_patch_description(self):
        new_value = {
            'ru': 'New description'
        }
        response_data = self.patch_json(
            '/departments/%s/' % self.department['id'],
            data={
                'description': new_value
            }
        )
        self.prepared_department['description'] = new_value
        self.assertEqual(response_data.get('description'), new_value)
        self.assertEqual(response_data, self.prepared_department)

    def test_patch_maillist_type(self):
        new_value = 'both'
        response_data = self.patch_json(
            '/departments/%s/' % self.department['id'],
            data={
                'maillist_type': new_value
            }
        )
        self.prepared_department['maillist_type'] = new_value
        self.assertEqual(response_data.get('maillist_type'), new_value)
        self.assertEqual(response_data, self.prepared_department)

    def test_patch_label(self):
        new_label = 'new_label'
        response_data = self.patch_json(
            '/departments/%s/' % self.department['id'],
            data={
                'label': new_label
            }
        )
        self.prepared_department['label'] = new_label
        self.assertEqual(response_data.get('label'), new_label)
        self.assertEqual(response_data.get('email'), new_label+'@'+self.label+self.domain_part)

    def test_returned_object(self):
        new_value = {
            'ru': 'New description'
        }
        response_data = self.patch_json(
            '/departments/%s/' % self.department['id'],
            data={
                'description': new_value
            }
        )

        expected = prepare_department(
            self.main_connection,
            DepartmentModel(self.main_connection).get(
                department_id=self.department['id'],
                org_id=self.organization['id'],
                fields=[
                    '*',
                    'parent.*',
                    'head.*',
                    'parents.*',
                ]
            ),
            api_version=1,
        )
        self.assertEqual(expected, response_data)

    def test_set_a_head_of_department(self):
        response_data = self.patch_json(
            '/departments/%s/' % self.department['id'],
            {
                'head_id': self.user['id']
            }
        )

        assert_that(
            response_data,
            has_entry(
                'head',
                all_of(
                    has_entries(
                        id=self.user['id'],
                        name=self.user['name'],
                        department_id=self.user['department_id'],
                    ),
                    is_not(
                        has_entries(
                            department=anything
                        )
                    )
                )
            )
        )
        self.assertEqual(
            except_fields(response_data, 'head'),
            except_fields(self.prepared_department, 'head')
        )

    def test_error_on_wrong_head_id_on_update(self):
        response_data = self.patch_json(
            '/departments/%s/' % self.department['id'],
            {
                'head_id': 776688
            },
            expected_code=422
        )
        assert_that(
            response_data,
            has_entries(
                code='head_of_department_not_found',
                message='Unable to find person with head_id={uid}',
            )
        )

    def test_change_a_head_of_department(self):
        DepartmentModel(self.main_connection).update_one(
            id=self.department['id'],
            org_id=self.department['org_id'],
            data={
                'head_id': self.user['id']
            },
        )
        other_user = UserModel(self.main_connection).create(
            id=123,
            nickname='art',
            name={'first': {'ru': 'Саша', 'en': 'Sasha'}},
            email='art@ya.ru',
            gender='male',
            org_id=self.organization['id'],
            department_id=self.department['id']
        )

        response_data = self.patch_json(
            '/departments/%s/' % self.department['id'],
            {
                'head_id': other_user['id']
            }
        )

        assert_that(
            response_data,
            has_entry(
                'head',
                has_entry(
                    'name',
                    {'first': lstring('Саша', 'Sasha')}
                )
            )
        )

    def test_set_new_parent_id(self):
        response_data = self.patch_json(
            '/departments/%s/' % self.department['id'],
            {
                'parent_id': self.other_department['id']
            }
        )
        self.assertEqual(response_data['parent'].get('id'), self.other_department['id'])

    def test_set_new_external_id(self):
        # меняем поле external_id

        external_id = 'new_external_id'
        response_data = self.patch_json(
            '/departments/%s/' % self.department['id'],
            {
                'external_id': external_id
            }
        )
        self.assertEqual(response_data['external_id'], external_id)

    def test_set_new_parent_as_object(self):
        response_data = self.patch_json(
            '/departments/%s/' % self.department['id'],
            {
                'parent': {'id': self.other_department['id']}
            }
        )
        self.assertEqual(
            response_data['parent'].get('id'),
            self.other_department['id']
        )

    def test_set_not_existing_parent_id(self):
        # пытаемся переместить отдел в несуществующий отдел
        nonexisting_parent_id = 10000
        response_data = self.patch_json(
            '/departments/%s/' % self.department['id'],
            {
                'parent_id': nonexisting_parent_id,
            },
            expected_code=422,
        )
        assert_that(
            response_data,
            has_entries(
                code='parent_department_not_found',
                message='Unable to find parent department with id={id}',
                params={'id': nonexisting_parent_id},
            )
        )

    def test_set_parent_id_as_null(self):
        self.patch_json(
            '/departments/%s/' % self.department['id'],
            {
                'parent_id': None
            },
            expected_code=422
        )

    def test_set_parent_id_as_self(self):
        # проверяем, что попытка переместить отдел в самого
        # себя закончится ошибкой валидации
        response = self.patch_json(
            '/departments/%s/' % self.department['id'],
            {
                'parent_id': self.department['id']
            },
            expected_code=422
        )
        assert_that(
            response,
            has_entries(
                code='constraint_validation.cant_move_department_to_itself',
                message='Department could not be used as parent of itself',
            )
        )

    def test_set_as_parent_id_one_of_departments_descendants(self):
        # проверяем, что попытка переместить отдел в один из его дочерних
        # отделов, закончится ошибкой
        child_department = DepartmentModel(self.main_connection).create(
            id=4,
            name={
                'ru': 'Разработка диска',
                'en': 'Disk development'
            },
            org_id=self.department['org_id'],
            parent_id=self.department['id']
        )

        response = self.patch_json(
            '/departments/%s/' % self.department['id'],
            {
                'parent_id': child_department['id']
            },
            expected_code=422
        )

        assert_that(
            response,
            has_entries(
                code='constraint_validation.cant_move_department_to_descendant',
                message="Department with id {child_id} is a descendant of department with id {parent_id} and cannot be used as it's parent",
                params={
                    'child_id': 4,
                    'parent_id': 2,
                },
            )
        )

    def test_patch_events(self):
        # Проверяем какие события сгененируются пи переносе отдела в другой отдел
        # Изначальная конфигурация:

        # Root
        #   -> IT (id=2)
        #   -> Another (id=3)
        #
        # После переноса:
        #
        # Root
        #   -> Another
        #      ->IT
        #
        # Плюс, у отдела IT меняется Description

        event_model = EventModel(self.main_connection)
        action_model = ActionModel(self.main_connection)
        event_model.delete(force_remove_all=True)
        action_model.delete(force_remove_all=True)

        new_value = {
            'ru': 'New Patch description'
        }

        self.patch_json(
            '/departments/%s/' % self.department['id'],
            data={
                'description': new_value,
                'parent_id': self.other_department['id'],
            }
        )
        events = event_model.find()
        event_names = [x['name'] for x in events]

        expected = [
            'department_department_added',
            'department_department_deleted',
            'department_moved',
            # событие про изменение родителя и описания у IT
            'department_property_changed',
            # раньше тут были ещё пара событий, типа про изменение
            # числа сотрудников. Но поскольку и IT и Another
            # пустые, то таких событий генериться не должно
            # а прежде просто был баг.
            # u'department_property_changed',
            # u'department_property_changed',
        ]
        assert_that(event_names, contains_inanyorder(*expected))

        # У отдела IT сменился родитель и описание
        assert_that(
            list(events[-1]['content']['diff'].keys()),
            # никаких parent или parent_id тут оказаться
            # не должно, так как по изменению родителя
            # генерится отдельное событие department_moved
            contains_inanyorder(*['description', 'description_plain'])
        )

        action = action_model.find()[0]['name']
        assert_that(action, equal_to('department_modify'))


class TestDepartmentDetail__delete(TestCase):
    def setUp(self):
        super(TestDepartmentDetail__delete, self).setUp()
        department_model = DepartmentModel(self.main_connection)
        department = department_model.create(
            id=2,
            name={'ru': 'IT'},
            org_id=self.organization['id'],
            parent_id=1
        )
        self.department = department_model.get(
            department_id=department['id'],
            org_id=self.organization['id'],
            fields=[
                '*',
                'parent.*',
                'head.*',
                'parents.*',
            ]
        )
        self.prepared_department = prepare_department(
            self.main_connection,
            self.department,
            api_version=1,
        )

        self.other_department = department_model.create(
            id=3,
            name={'ru': 'Another department'},
            org_id=self.organization['id'],
            parent_id=1
        )

    def test_deleting_department(self):
        removed_dep_id = 3121
        department_model = DepartmentModel(self.main_connection)
        department_model.create(
            id=removed_dep_id,
            name=DEPARTMENT_NAME,
            org_id=self.organization['id'],
            parent_id=self.other_department['id']
        )

        department = department_model.get(department_id=removed_dep_id, org_id=self.organization['id'])
        self.assertFalse(department['removed'])

        self.delete_json('/departments/%s/' % removed_dep_id)

        fresh_department = department_model.get(
            department_id=removed_dep_id,
            org_id=self.organization['id'],
            removed=True,
        )
        self.assertTrue(fresh_department['removed'])

    def test_not_patch_removed_department(self):
        # Проверим, что нельзя изменить удаленный отдел
        removed_dep = self.create_department(parent_id=self.department['id'])
        self.delete_json('/departments/%s/' % removed_dep['id'])

        self.patch_json(
            '/departments/%s/' % removed_dep['id'],
            data={
                'parent_id': self.other_department['id'],
            },
            expected_code=404
        )

    def test_response_does_not_return_removed_department(self):
        # Проверим, что get департаментов не возвращает удаленные департаменты
        removed_dep_id = 3121
        DepartmentModel(self.main_connection).create(
            id=removed_dep_id,
            name=DEPARTMENT_NAME,
            org_id=self.organization['id'],
            parent_id=self.other_department['id']
        )

        self.get_json('/departments/%d/' % removed_dep_id)  # отдел всё еще есть
        self.delete_json('/departments/%s/' % removed_dep_id)  # удаляем его
        self.get_json('/departments/%d/' % removed_dep_id, expected_code=404)

    def test_suggest_does_not_return_removed_departments(self):
        # Саджест не должен возвращать удаленные департаменты
        uniq_remove_name = {
            'ru': 'Уникальноедепимя',
            'en': 'Uniqdepname',
        }
        removed_dep_id = 3121
        DepartmentModel(self.main_connection).create(
            id=removed_dep_id,
            name=uniq_remove_name,
            org_id=self.organization['id'],
            parent_id=self.other_department['id']
        )
        self.delete_json('/departments/%s/' % removed_dep_id)  # удаляем его

        response_data = self.get_json('/suggest/?text=Uniqdepname&limit=1')
        assert_that(
            len(response_data['departments']),
            equal_to(0)
        )

    def test_can_not_remove_department_with_active_users(self):
        # Проверяем, что не можем удалить департамент с активными пользователями
        dep = self.create_department(parent_id=self.department['id'])
        self.create_user(department_id=dep['id'])
        self.create_user(department_id=dep['id'])

        self.delete_json('/departments/%s/' % dep['id'], expected_code=422)

    def test_cant_remove_department_with_dismissed_users(self):
        # Проверяем, что не можем удалить департамент с уволенными сотрудниками
        new_parent = DepartmentModel(self.main_connection).create(
            id=312,
            name={'ru': 'Another department'},
            org_id=self.organization['id'],
            parent_id=1
        )
        dep = self.create_department(parent_id=new_parent['id'])
        user1 = self.create_user(department_id=dep['id'])
        # Зачем это?
        UserModel(self.main_connection).dismiss(
            self.organization['id'],
            user1['id'],
            self.user['id'],
        )
        user2 = self.create_user(department_id=dep['id'])

        UserModel(self.main_connection).dismiss(
            self.organization['id'],
            user2['id'],
            self.user['id'],
        )
        # при увольнении пользователь перносится в корневой отдел
        # @art: Какого чорта тут 422 тогда? И почему тут нет проверки что
        #       случилось с уволенными?
        #       Короче, описание теста не соответствует тому, что проверяется
        #       надеюсь, что @khunafin и @akhmetov прояснят эту ситуацию?
        self.delete_json('/departments/%s/' % ROOT_DEPARTMENT_ID, expected_code=422)

    def test_can_remove_department_without_users(self):
        # Проверяем, что можем удалить департамент без сотрудников
        new_parent = DepartmentModel(self.main_connection).create(
            id=312,
            name={'ru': 'Another department'},
            org_id=self.organization['id'],
            parent_id=1
        )
        dep = self.create_department(parent_id=new_parent['id'])
        self.assertEqual(
            UserModel(self.main_connection).find(
                filter_data={
                    'org_id': dep['org_id'],
                    'department_id': dep['id'],
                    'is_dismissed': Ignore
                }
            ),
            [],
        )
        self.delete_json('/departments/%s/' % dep['id'])

    def test_can_not_remove_department_with_child_active_departments(self):
        # Проверяем, что не можем удалить департамент с активными дочерними
        # департаментами в обычном случае
        dep = self.create_department(parent_id=self.department['id'])
        dep1 = self.create_department(parent_id=dep['id'])
        dep2 = self.create_department(parent_id=dep['id'])

        self.delete_json('/departments/%s/' % dep['id'], expected_code=422)

        department_model = DepartmentModel(self.main_connection)

        # Проверим, что дочерние подотделы не удалились
        check_dep1 = department_model.get(
            org_id=self.organization['id'],
            department_id=dep1['id']
        )
        assert_that(
            check_dep1,
            has_entries(
                id=dep1['id'],
                removed=False,
            )
        )
        check_dep2 = department_model.get(
            org_id=self.organization['id'],
            department_id=dep2['id']
        )
        assert_that(
            check_dep2,
            has_entries(
                id=dep2['id'],
                removed=False,
            )
        )

    def test_can_remove_department_with_child_removed_departments(self):
        # Проверяем, что можем удалить департамент с удаленными дочерними департаментами
        new_parent = DepartmentModel(self.main_connection).create(
            id=412,
            name={'ru': 'Another department'},
            org_id=self.organization['id'],
            parent_id=1
        )
        parent_department = self.create_department(parent_id=new_parent['id'])
        child_department_1 = self.create_department(parent_id=parent_department['id'])
        child_department_2 = self.create_department(parent_id=parent_department['id'])

        self.delete_json('/departments/%s/' % parent_department['id'], expected_code=422)

        self.delete_json('/departments/%s/' % child_department_1['id'])
        self.delete_json('/departments/%s/' % child_department_2['id'])
        self.delete_json('/departments/%s/' % parent_department['id'])

    def test_can_remove_department_with_child_removed_departments_and_without_users(self):
        # Проверяем, что можем удалить департамент с удаленными дочерними
        # департаментами и уволенными сотрудниками внутри
        new_parent = DepartmentModel(self.main_connection).create(
            id=512,
            name={'ru': 'Another department'},
            org_id=self.organization['id'],
            parent_id=1
        )
        parent_department = self.create_department(parent_id=new_parent['id'])
        child_department_1 = self.create_department(parent_id=parent_department['id'])
        self.delete_json('/departments/%s/' % child_department_1['id'])

        child_department_2 = self.create_department(parent_id=parent_department['id'])
        self.delete_json('/departments/%s/' % child_department_2['id'])

        self.delete_json('/departments/%s/' % parent_department['id'])

    def test_removed_twice(self):
        dep = self.create_department(parent_id=self.department['id'])
        self.delete_json('/departments/%s/' % dep['id'])
        self.delete_json('/departments/%s/' % dep['id'], expected_code=404)

    def test_removing_root_department(self):
        self.delete_json('/departments/%s/' % ROOT_DEPARTMENT_ID, expected_code=422)


class TestDepartmentSubMembersCountMoveUser(TestCase, TestDepartments):
    """
    Тест на пересчет кэша суммарного количества сотрудников в подотделах.
    При добавлении/переремещении/увольнении сотрудника.

    Изначально создаётся такая структура компании:

    dep1 (6)
      -> user11
      -> dep2 (5)
        -> user21
        -> user22
        -> dep3 (3)
          -> user31
          -> user32
          -> user33

    dep4 (0)
    """

    def setUp(self):
        super(TestDepartmentSubMembersCountMoveUser, self).setUp()

        org_id = self.organization['id']

        cr = partial(
            create_department,
            self.main_connection,
        )
        self.department1 = cr(org_id)
        self.department2 = cr(
            org_id,
            parent_id=self.department1['id'],
        )
        self.department3 = cr(
            org_id,
            parent_id=self.department2['id'],
        )
        self.department4 = cr(org_id)

        self.user31 = self.create_user(department_id=self.department3['id'])
        self.user32 = self.create_user(department_id=self.department3['id'])
        self.user33 = self.create_user(department_id=self.department3['id'])

        self.user21 = self.create_user(department_id=self.department2['id'])
        self.user22 = self.create_user(department_id=self.department2['id'])

        self.user11 = self.create_user(department_id=self.department1['id'])

        EventModel(self.main_connection).delete(force_remove_all=True)
        ActionModel(self.main_connection).delete(force_remove_all=True)

    def assertDepartmentChangedEvents(self, count):
        self.assertEqual(
            EventModel(self.main_connection).count({
                'org_id': self.organization['id'],
                'name': 'department_property_changed'
            }),
            count
        )

    def test_user_dismiss(self):
        # Если пользователь уволен, то счетчик уменьшается
        self.patch_json('/users/%s/' % self.user31['id'], data={'is_dismissed': True})

        self.refresh_deparments()

        self.assertDepartmentChangedEvents(3)
        self.assertSubMembersCount(self.department1, 5)
        self.assertSubMembersCount(self.department2, 4)
        self.assertSubMembersCount(self.department3, 2)

    def test_user_move_to_parent_department(self):
        # Перемещаем сотрудника в отдел по иерархии выше
        # из отдела 3 в отдел 1

        self.refresh_deparments()
        self.assertEqual(self.department1['members_count'], 6)

        self.patch_json('/users/%s/' % self.user31['id'], data={'department_id': self.department1['id']})

        self.refresh_deparments()

        # так как в цепочке отделов три департамента, то в двух из них
        # поменяется счётчик с количеством сотрудников в подотделах,
        # а в самом верхнеуровневом счетчик останется прежним, так как
        # суммарное количество сотрудников в нём не изменится
        self.assertDepartmentChangedEvents(2)
        # после перемещения в первом отделе должно быть то же самое
        # количество сотрудников
        self.assertSubMembersCount(self.department1, 6)
        # а во втором и третьем должно уменьшиться на 1
        self.assertSubMembersCount(self.department2, 4)
        self.assertSubMembersCount(self.department3, 2)

        # а теперь проверим, что счетчики в событиях тоже правильные
        events = EventModel(self.main_connection).find(
            {
                'org_id': self.organization['id'],
                'name': 'department_property_changed'
            }
        )
        events = {
            e['object']['id']: e
            for e in events
        }

        def get_diff(dep_id):
            return events[dep_id]['content']['diff']['members_count']

        # первый отдел не проверяем, так как в нём суммарное
        # количество сотрудников не изменилось
        self.assertEqual(
            get_diff(self.department2['id']),
            [5, 4]
        )
        self.assertEqual(
            get_diff(self.department3['id']),
            [3, 2]
        )

    def test_user_move_to_another_department(self):
        # Перемещаем сотрудника в соседний отдел

        self.patch_json('/users/%s/' % self.user31['id'], data={'department_id': self.department4['id']})

        self.refresh_deparments()

        self.assertDepartmentChangedEvents(4)
        self.assertSubMembersCount(self.department1, 5)
        self.assertSubMembersCount(self.department2, 4)
        self.assertSubMembersCount(self.department3, 2)
        self.assertSubMembersCount(self.department4, 1)

    def test_add_user_to_department(self):
        # Новый сотрудник в отделе
        birthday = datetime.date(day=1, month=1, year=1970)
        data = {
            'name': self.name,
            'nickname': 'nickname',
            'password': '123456787',
            'department_id': self.department2['id'],
            'birthday': format_date(birthday),
            'gender': 'male',
        }

        self.post_json('/users/', data, expected_code=201)

        self.refresh_deparments()

        self.assertDepartmentChangedEvents(2)
        self.assertSubMembersCount(self.department1, 7)
        self.assertSubMembersCount(self.department2, 6)
        self.assertSubMembersCount(self.department3, 3)


class TestDepartmentSubMembersCountMoveDepartment(TestCase, TestDepartments):
    """
    Тест на пересчет кэша суммарного количества сотрудников в подотделах.
    При перемещении отдела.

    Изначально структура выглядит так:
       1[20]
     /       \
    2[10]     3[5]
    |
    4[2]

    """

    def setUp(self):
        super(TestDepartmentSubMembersCountMoveDepartment, self).setUp()

        def create_users(dep_id, count):
            for i in range(count):
                self.create_user(department_id=dep_id)

        cr = partial(
            create_department,
            self.main_connection,
            self.organization['id']
        )
        self.department1 = cr()
        create_users(self.department1['id'], 5)

        self.department2 = cr(
            parent_id=self.department1['id'],
        )
        create_users(self.department2['id'], 8)

        self.department3 = cr(
            parent_id=self.department1['id'],
        )
        create_users(self.department3['id'], 5)

        self.department4 = cr(
            parent_id=self.department2['id'],
        )
        create_users(self.department4['id'], 2)

    def test_move_department(self):
        #    1[20]    --->     1[20]
        #  /       \         /       \
        # 2[10]     3[5]    2[8]     3[7]
        # |                          |
        # 4[2]                       4[2]
        #
        # Перемещаем отдел 4 в отдел 3
        # Должен пересчитаться кэш количества сотрудников

        url = '/departments/{}/'.format(self.department4['id'])
        data = {'parent_id': self.department3['id']}

        self.refresh_deparments()
        self.assertSubMembersCount(self.department1, 20)
        self.assertSubMembersCount(self.department2, 10)
        self.assertSubMembersCount(self.department3, 5)
        self.assertSubMembersCount(self.department4, 2)

        self.patch_json(url, data=data)
        self.refresh_deparments()

        self.assertSubMembersCount(self.department1, 20)
        self.assertSubMembersCount(self.department2, 8)
        self.assertSubMembersCount(self.department3, 7)
        self.assertSubMembersCount(self.department4, 2)

        # а теперь проверим, что счетчики в событиях тоже правильные
        events = EventModel(self.main_connection).find(
            {
                'org_id': self.organization['id'],
                'name': 'department_property_changed'
            }
        )
        self.assertEqual(len(events), 2)

        # все эти события, они про изменение счетчиков с числом
        # сотрудников
        events = {
            e['object']['id']: e
            for e in events
        }

        def get_diff(dep_id):
            return events[dep_id]['content']['diff']['members_count']

        self.assertEqual(
            get_diff(self.department2['id']),
            [10, 8]
        )
        self.assertEqual(
            get_diff(self.department3['id']),
            [5, 7]
        )


class TestDepartmentBulkMove__post(TestCase):
    def test_simple_move_to(self):
        # Простой сценарий, где всё передано как надо.
        # При этом в базе должно завестись 4 таска - два
        # для переноса пользователей и два для переноса отделов.
        user1 = self.create_user()
        user2 = self.create_user()
        dep1 = self.create_department()
        dep2 = self.create_department()
        dep_to = self.create_department()
        data = {
            'user_ids': [user1['id'], user2['id']],
            'dep_ids': [dep1['id'], dep2['id']],
            'to_dep_id': dep_to['id'],
        }
        r = self.post_json('/move/', data=data, expected_code=202)
        assert_that(
            r,
            has_entries(tasks=has_length(4))
        )

    def test_move_to_not_exist_dep_id(self):
        # Если отдел, куда мы пытаемся перенсети людей, не существует,
        # то это должно приводить к ошибке
        user = self.create_user()
        dep = self.create_department()
        not_exist_dep_id = 435434
        data = {
            'user_ids': [user['id']],
            'dep_ids': [dep['id']],
            'to_dep_id': not_exist_dep_id,
        }
        r = self.post_json('/move/', data=data, expected_code=422)
        assert_that(
            r,
            has_entries(
                code='department_not_found',
                message='Unable to find department with id={id}',
                params=has_entries(
                    id=not_exist_dep_id,
                )
            )
        )

    def test_move_empty_data_to_dep_id(self):
        # В ручку обязательно должны быть переданы либо
        # id сотрудников, либо id отделов
        dep = self.create_department()
        data = {
            'user_ids': [],
            'dep_ids': [],
            'to_dep_id': dep['id'],
        }
        r = self.post_json('/move/', data=data, expected_code=422)
        assert_that(
            r,
            has_entries(
                code='schema_validation_error',
            )
        )

    def test_move_not_exist_users_or_deps_to_dep_id(self):
        # Попытка перенести несуществующие отделы или пользователей
        # должна завершаться 202 статусом и создавать таски.
        # Но таски эти конечно потом сфейлятся и соответствующая ошибка
        # будет показана пользователю.
        dep = self.create_department()
        dep1 = self.create_department()
        data = {
            'user_ids': [3434, 34545, 546345, 34534534],
            'dep_ids': [dep1['id']],
            'to_dep_id': dep['id'],
        }
        r = self.post_json('/move/', data=data, expected_code=202)
        # Ожидаем, что хотя все пользователи не существуют, тасков всё равно будет 5
        assert_that(
            r,
            has_entries(tasks=has_length(5))
        )

        user1 = self.create_user()
        data = {
            'user_ids': [user1['id']],
            'dep_ids': [3434, 546456, 3424, 45232],
            'to_dep_id': dep['id'],
        }
        r = self.post_json('/move/', data=data, expected_code=202)
        # Ожидаем, что хотя все отделы не существуют, тасков всё равно будет 5
        assert_that(
            r,
            has_entries(tasks=has_length(5))
        )

    def test_move_dep_to_itself(self):
        # Попытка перенести отдел в самого себя, должна заканчиваться ошибкой
        # сразу же.
        dep = self.create_department()
        data = {
            'user_ids': [3434],
            'dep_ids': [dep['id']],
            'to_dep_id': dep['id'],
        }
        r = self.post_json('/move/', data=data, expected_code=409)
        assert_that(
            r,
            has_entries(
                code='cant_move_department_to_itself',
                message='Department could not be used as parent of itself'
            )
        )

    def test_move_dep_to_ascedent(self):
        # Попытка перенести отдел в один из его под-отделов, должна
        # приводить к ошибке
        dep = self.create_department()
        dep1 = self.create_department(parent_id=dep['id'])
        data = {
            'user_ids': [],
            'dep_ids': [dep['id']],
            'to_dep_id': dep1['id'],
        }
        r = self.post_json('/move/', data=data, expected_code=409)
        assert_that(
            r,
            has_entries(
                code='cant_move_department_to_descendant',
                message='Department with id {child_id} is a descendant of department '
                        'with id {parent_id} and cannot be used as it\'s '
                        'parent',
                params=has_entries(
                    child_id=dep1['id'],
                    parent_id=dep['id']
                )
            )
        )


class TestDepartmentAliasesListView__post(TestCase):
    api_version = 'v7'

    def test_add_alias(self):
        # добавление алиаса отдела

        old_aliases = ['alias1']
        added_alias = 'alias2'

        department = DepartmentModel(self.main_connection).create(
            id=2,
            name=DEPARTMENT_NAME,
            org_id=self.organization['id'],
            aliases=old_aliases,
        )

        another_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']

        another_department = DepartmentModel(self.main_connection).create(
            id=2,
            name=DEPARTMENT_NAME,
            org_id=another_organization['id'],
        )

        with patch('intranet.yandex_directory.src.yandex_directory.core.views.departments.action_department_modify'):

            response_data = self.post_json(
                '/departments/%s/aliases/' % department['id'],
                data={
                    'name': added_alias
                },
            )
            # добавили алиас в Паспорт
            assert_called_once(
                self.mocked_passport.alias_add,
                department['uid'],
                added_alias,
            )

        # алиасы обновились в директории в одной организации
        assert_that(
            DepartmentModel(self.main_connection).count(
                filter_data={
                    'id': department['id'],
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
                id=department['id']
            )
        )
        events = EventModel(self.main_connection).find({
            'org_id': department['org_id'],
            'revision': self.get_org_revision(self.organization['id']),
        })
        assert_that(events, has_length(1))
        assert_that(
            events[0],
            has_entries(
                name='department_alias_added',
                object=has_entries(
                    id=department['id'],
                    org_id=self.organization['id'],
                    aliases=has_item(added_alias),
                ),
                content=has_entries(alias_added=added_alias)
            )
        )


class TestDepartmentAliasesDetailView__delete(TestCase):
    def _delete_alias(self, delete_func=None):
        """
        Общий тест для удаления алиасов
        :param delete_func: Функция для мока запрос на удаления в паспорте
        :type delete_func: function
        """

        old_aliases = ['alias1', 'alias2']
        deleted_alias = 'alias2'

        if delete_func is not None:
            self.mocked_passport.alias_delete = delete_func

        department = self.create_department(
            org_id=self.organization['id'],
            aliases=old_aliases,
            uid=123,
        )

        with patch('intranet.yandex_directory.src.yandex_directory.core.views.departments.action_department_modify'):
            self.delete_json(
                '/departments/%s/aliases/%s/' % (department['id'], deleted_alias),
            )
            # удалили алиас в Паспорте
            assert_called_once(
                self.mocked_passport.alias_delete,
                department['uid'],
                deleted_alias
            )

        # алиасы обновились в директории
        assert_that(
            DepartmentModel(self.main_connection).count(
                filter_data={
                    'id': department['id'],
                    'alias': deleted_alias
                }
            ),
            equal_to(0)
        )
        events = EventModel(self.main_connection).find({
            'org_id': department['org_id'],
            'revision': self.get_org_revision(self.organization['id']),
        })
        assert_that(events, has_length(1))
        assert_that(
            events[0],
            has_entries(
                name='department_alias_deleted',
                object=has_entries(
                    id=department['id'],
                    aliases=not_(has_item(deleted_alias)),
                ),
                content=has_entries(alias_deleted=deleted_alias)
            )
        )

    def test_delete_alias(self):
        # удаление алиаса отдела

        self._delete_alias()

    def test_delete_alias_no_in_passport(self):
        # удаление алиаса который есть в директории, но нет в Паспорте

        self._delete_alias(
            Mock(side_effect=AliasNotFound)
        )
