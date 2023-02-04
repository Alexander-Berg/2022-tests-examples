# -*- coding: utf-8 -*-

from hamcrest import (
    assert_that,
    has_key,
    is_not,
    all_of,
    contains,
    has_entries,
)
from unittest.mock import patch

from intranet.yandex_directory.src import settings
from testutils import (
    TestCase,
    get_auth_headers,
    create_organization,
    create_user,

)
from testutils import (
    override_settings,
)
from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.auth import tvm
from intranet.yandex_directory.src.yandex_directory.common.db import catched_sql_queries
from intranet.yandex_directory.src.yandex_directory.connect_services.direct.client.client import DirectRoleDto
from intranet.yandex_directory.src.yandex_directory.core.features import CAN_WORK_WITHOUT_OWNED_DOMAIN
from intranet.yandex_directory.src.yandex_directory.core.features import is_feature_enabled
from intranet.yandex_directory.src.yandex_directory.core.models import (
    UserModel,
    OrganizationServiceModel,
    PresetModel,
    ResourceModel,
    ServiceModel,
    TaskModel,
    UserMetaModel,
)
from intranet.yandex_directory.src.yandex_directory.core.resource.tasks import CreateExistingAccountTask
from intranet.yandex_directory.src.yandex_directory.core.utils import only_attrs


class TestDryRun(TestCase):
    def setUp(self):
        super(TestDryRun, self).setUp()

        self.author_uid = 100700
        self.second_user_id = 100600
        self.resource_id = '382i4wnck-2348=1'
        inner_user_uid = self.user['id']

        def fake_side_effect(uid, *args, **kwargs):
            if uid == 100700:
                return {
                    'uid': 100700,
                    'fields': {
                        'login': 'the-admin',
                        'firstname': 'Admin',
                        'lastname': 'Adminov',
                        'sex': 1,
                        'birth_date': '1980-01-01',
                    },
                    'default_email': 'theadmin@domain.ru',
                }
            elif uid == 100600:
                return {
                    'uid': 100600,
                    'fields': {
                        'login': 'just-user',
                        'firstname': 'Just',
                        'lastname': 'Userof',
                        'sex': 1,
                        'birth_date': '1980-01-01',
                    },
                    'default_email': 'justuser@domain.ru',
                }
            elif uid == inner_user_uid:
                return {
                    'uid': inner_user_uid,
                    'fields': {
                        'login': 'just-user',
                        'firstname': 'Just',
                        'lastname': 'Userof',
                        'sex': 1,
                        'birth_date': '1980-01-01',
                    },
                    'default_email': 'default@domain.ru',
                }
            else:
                # Такого вообще быть не должно
                raise NotImplementedError()

        # Представим, будто Паспорт что-то знает про пользователей,
        # которых ещё нет в организации
        self.mocked_blackbox.userinfo.side_effect = fake_side_effect
        # Создадим пресет no-owned-domain с сервисов dashboard
        PresetModel(self.meta_connection).create(
            'without-domain',
            service_slugs=[],
            settings={}
        )
        # Сделаем так, чтобы сервису не нужен был робот
        ServiceModel(self.meta_connection).\
            filter(slug=self.service['slug']).\
            update(robot_required=False)

    def make_call(self, url, expected_code=200, as_org=False):
        as_uid = self.user['id'] if as_org else self.author_uid
        return self.post_json(
            url,
            data=dict(
                org_id=self.organization['id'] if as_org else None,
                organization={},
                service=dict(
                    slug=self.service['slug'],
                    resource=dict(
                        id=self.resource_id,
                        relations=[
                            dict(object_id=as_uid, object_type='user', name='rw'),
                            dict(object_id=self.second_user_id, object_type='user', name='rw'),
                        ]
                    )
                )),
            headers=get_auth_headers(as_uid=as_uid),
            expected_code=expected_code,
        )

    def test_dry_run_mode(self):
        # По умолчанию, должен запускаться dry-run
        org_id = self.organization['id']

        response = self.make_call('/bind/')
        self.assertEqual(response['binding_available'], True)

        # Проверим, что никакие пользователи не завелись
        users_count = UserModel(self.main_connection) \
                      .filter(org_id=org_id, is_robot=False) \
                      .count()
        self.assertEqual(users_count, 1)

    def test_with_users_in_some_org(self):
        another_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']

        another_user = self.create_user(nickname='some_nickname', org_id=another_organization['id'])
        response = self.post_json(
            '/bind/',
            data=dict(
                org_id=self.organization['id'],
                organization={},
                service=dict(
                    slug=self.service['slug'],
                    resource=dict(
                        id=self.resource_id,
                        relations=[
                            dict(object_id=another_user['id'], object_type='user', name='rw'),
                            dict(object_id=self.second_user_id, object_type='user', name='rw'),
                        ]
                    )
                )),
            expected_code=409,
        )
        self.assertEqual(
            response['params']['error_details'],
            [
                {'id': another_user['id'],
                 'nickname': another_user['nickname'],
                 'org_id': another_organization['id'],
                 }
            ],
        )

    def test_no_dry_run_mode(self):
        # Если указано dry-run=0, то ручка должна создать новую организацию,
        # создать таск, вернуть task_id и org_id
        with catched_sql_queries() as queries:
            response = self.make_call('/bind/?dry-run=0')
            assert 200 < len(queries)
        assert_that(
            response,
            all_of(
                has_key('org_id'),
                is_not(
                    has_key('binding_available'),
                ),
            )
        )

        create_account_tasks = only_attrs(
            TaskModel(self.main_connection).
                filter(task_name=CreateExistingAccountTask.get_task_name()).
                fields('id'),
            'id'
        )
        # Проверим, что завёлся дополнительный пользователь
        users_count = UserModel(self.main_connection).\
            filter(org_id=response['org_id'], is_robot=False)\
            .count()
        # Пользователя уже должно быть два, так как второй, указанный
        # в запросе, должен был добавиться в список сотрудников
        # в результате выполнения таски.
        self.assertEqual(users_count, 2)
        # пользователь должен стать ответственным за сервис
        db_query = OrganizationServiceModel(self.main_connection).filter(
            org_id=response['org_id'],
            service_id=self.service['id']
        ).all()
        self.assertEqual(len(db_query), 1)
        assert_that(
            db_query,
            contains(
                has_entries(
                    responsible_id=100700,
                )
            )
        )

    def test_no_dry_run_mode_existing_org(self):
        # Если указано dry-run=0, то ручка должна вернуть task_id
        # включить фичу и создать таск
        with catched_sql_queries() as queries:
            response = self.make_call('/bind/?dry-run=0', as_org=True)
            assert 50 < len(queries)
        assert_that(
            response,
            all_of(
                has_key('org_id'),
                is_not(
                    has_key('binding_available'),
                ),
            )
        )

        # Проверим, что завёлся дополнительный пользователь
        users_count = UserModel(self.main_connection).\
            filter(org_id=self.organization['id'], is_robot=False)\
            .count()
        # Пользователя уже должно быть два, так как второй, указанный
        # в запросе, должен был добавиться в список сотрудников
        # в результате выполнения таски.
        self.assertEqual(users_count, 2)
        # Проверим, что включилась фича
        assert is_feature_enabled(
            self.meta_connection,
            self.organization['id'],
            CAN_WORK_WITHOUT_OWNED_DOMAIN,
        )

    def test_cant_add_two_direct_resources(self):
        with patch.object(app.direct, 'get_roles') as get_roles, \
            patch('intranet.yandex_directory.src.yandex_directory.connect_services.idm.direct.service.check_if_already_has_association') \
                    as check_if_already_has_association:
            get_roles.return_value = [
                DirectRoleDto(self.organization['id'], self.resource_id, '/user/employee/', self.user['id']),
                DirectRoleDto(self.organization['id'], self.resource_id, '/user/chief/', self.user['id']),
            ]
            check_if_already_has_association.return_value = False
            tvm.tickets[settings.DIRECT_SERVICE_SLUG] = 'direct_tvm_ticket'

            self.service = ServiceModel(self.meta_connection).create(
                slug='direct',
                name='Direct',
                client_id='client_id',
            )

            # добавим client_id директа
            self.make_call('/bind/?dry-run=0', as_org=True)

            # попробуем добавить другой client_id
            self.resource_id = 'some_another'
            self.make_call('/bind/?dry-run=0', expected_code=422, as_org=True)

            # при это в dry_run режиме проверять наличие другого тоже нужно
            self.make_call('/bind/?dry-run=1', expected_code=422, as_org=True)

    def test_get_orgs_with_users_in_another_org(self):
        another_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']

        another_user = self.create_user(nickname='some_nickname', org_id=another_organization['id'])
        response = self.post_json(
            '/bind/organizations/',
            data=dict(
                service=dict(
                    slug=self.service['slug'],
                    resource=dict(
                        id=self.resource_id,
                        relations=[
                            dict(object_id=another_user['id'], object_type='user', name='rw'),
                        ],
                    ),
                )),
            expected_code=200,
        )
        self.assertEqual(
            response,
            [
                {
                    'org_id': self.organization['id'],
                    'binding_available': False,
                    'reason': 'person_is_already_in_some_org',
                },
            ]
        )

    def test_get_orgs_with_users_in_this_org(self):
        another_user = self.create_user(nickname='some_nickname', org_id=self.organization['id'])
        response = self.post_json(
            '/bind/organizations/',
            data=dict(
                service=dict(
                    slug=self.service['slug'],
                    resource=dict(
                        id=self.resource_id,
                        relations=[
                            dict(object_id=another_user['id'], object_type='user', name='rw'),
                        ],
                    ),
                )),
            expected_code=200,
        )
        self.assertEqual(
            response,
            [
                {
                    'org_id': self.organization['id'],
                    'binding_available': True,
                },
            ],
        )

    def test_get_orgs_with_self_user_in_two_org(self):
        another_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']
        UserMetaModel(self.meta_connection).create(
            id=self.user['id'],
            org_id=another_organization['id'],
        )
        UserModel(self.main_connection).create(
            id=self.user['id'],
            nickname=self.user['nickname'],
            name=self.user['name'],
            email=self.user['email'],
            gender=self.user['gender'],
            org_id=another_organization['id'],
        )
        response = self.post_json(
            '/bind/organizations/',
            data=dict(
                service=dict(
                    slug=self.service['slug'],
                    resource=dict(
                        id=self.resource_id,
                        relations=[],
                    ),
                ),
            ),
            expected_code=200,
        )
        self.assertEqual(
            sorted(response, key=lambda x: x['org_id']),
            sorted([
                {
                    'org_id': self.organization['id'],
                    'binding_available': False,
                    'reason': 'person_is_already_in_some_org',
                },
                {
                    'org_id': another_organization['id'],
                    'binding_available': False,
                    'reason': 'person_is_already_in_some_org',
                },

            ], key=lambda x: x['org_id'])
        )

    def test_get_orgs_where_resource_is_already_exists(self):
        another_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='smth',
            source='smth',
        )['organization']

        external_id = 'qwerty123'
        ResourceModel(self.main_connection).create(
            org_id=another_organization['id'],
            service=self.service['slug'],
            external_id=external_id,
        )
        response = self.post_json(
            '/bind/organizations/',
            data=dict(
                service=dict(
                    slug=self.service['slug'],
                    resource=dict(
                        id=external_id,
                        relations=[],
                    ),
                ),
            ),
            expected_code=200,
        )
        self.assertEqual(
            response,
            [
                {
                    'org_id': self.organization['id'],
                    'binding_available': False,
                    'reason': 'resource_is_already_exists',
                }
            ],
        )

    def test_get_orgs_where_organization_already_has_direct_resource(self):
        with patch.object(app.direct, 'get_roles') as get_roles, \
                patch('intranet.yandex_directory.src.yandex_directory.connect_services.idm.direct.service.check_if_already_has_association') \
                        as check_if_already_has_association:
            get_roles.return_value = [
                DirectRoleDto(self.organization['id'], self.resource_id, '/user/employee/', self.user['id']),
                DirectRoleDto(self.organization['id'], self.resource_id, '/user/chief/', self.user['id']),
            ]
            check_if_already_has_association.return_value = False
            tvm.tickets[settings.DIRECT_SERVICE_SLUG] = 'direct_tvm_ticket'

            self.service = ServiceModel(self.meta_connection).create(
                slug='direct',
                name='Direct',
                client_id='client_id',
            )
            self.make_call('/bind/?dry-run=0', as_org=True)
            self.resource_id = 'some_another'

            response = self.post_json(
                '/bind/organizations/',
                data=dict(
                    service=dict(
                        slug=self.service['slug'],
                        resource=dict(
                            id=self.resource_id,
                            relations=[],
                        ),
                    )),
                expected_code=200,
                headers=get_auth_headers(as_uid=self.user['id']),

            )
            self.assertEqual(
                response,
                [
                    {
                        'org_id': self.organization['id'],
                        'binding_available': False,
                        'reason': 'organization_already_has_direct_resource',
                    }
                ],
            )
