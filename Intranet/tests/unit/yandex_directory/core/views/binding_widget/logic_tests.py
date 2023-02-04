# coding: utf-8
import responses

from hamcrest import (
    assert_that,
    has_entries,
    contains_inanyorder,
)
import unittest.mock
from unittest.mock import (
    patch,
)

from flask import g

from testutils import (
    create_organization,
    TestCase,
    override_settings,
    mocked_requests,
    assert_called_once,
)
from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.common.utils import (
    Ignore,
)
from intranet.yandex_directory.src.yandex_directory.core.models import (
    TaskModel,
    ResourceModel,
    UserModel,
    ServiceModel,
    OrganizationServiceModel,
)
from intranet.yandex_directory.src.yandex_directory.core.models.resource import ResourceRelationModel
from intranet.yandex_directory.src.yandex_directory.core.resource.tasks import CreateExistingAccountTask
from intranet.yandex_directory.src.yandex_directory.core.task_queue.base import SyncResult
from intranet.yandex_directory.src.yandex_directory.core.tasks.resource import BindTask
from intranet.yandex_directory.src.yandex_directory.core.views.binding_widget.logic import (
    gather_uids_from_request,
    check_in_some_org,
    check_is_resource_exists,
    check_access_for_bind,
    check_access_for_bind_forms,
    check_access_for_bind_metrika,
    UserIsAlreadyInAnotherOrg,
    ResourceIsAlreadyExists,
    AuthorizationError,
    ExternalServiceError,
    UserNotFoundInOrganizationError,
    AuthenticationError,
    check_access_for_bind_yandexsprav,
    check_in_current_org,
    check_access_for_bind_direct,
)

from intranet.yandex_directory.src.yandex_directory.auth import tvm
from intranet.yandex_directory.src.yandex_directory import app


class TestGatherUIDS(TestCase):
    def _cases(self):
        cases = list()  # (input, expected_output)
        cases.append(([],
                     []))

        cases.append(([dict(object_id=1, object_type='department', name='')],
                     []))

        cases.append(([dict(object_id='1', object_type='user', name='')],
                     [1]))

        cases.append(([dict(object_id='1', object_type='group', name='')],
                     []))

        cases.append(([dict(object_id='1', object_type='user', name=''),
                      dict(object_id='2', object_type='user', name='')],
                     [1, 2]))

        cases.append(([dict(object_id='1', object_type='user', name=''),
                      dict(object_id='2', object_type='group', name=''),
                      dict(object_id='3', object_type='department', name='')],
                     [1]))

        return cases

    def test_cases(self):
        for input, expected_output in self._cases():
            self.assertEqual(
                set(expected_output),
                gather_uids_from_request(relations=input)
            )


class TestCheckUserInSomeOrg(TestCase):
    def test_responsible_in_same_org(self):
        self.assertEqual(None,
                         check_in_some_org(
                             self.meta_connection,
                             responsible_id=self.user['id'],
                             others_ids=[],
                             existing_org_id=self.organization['id']
                         ))

    def test_others_in_same_org(self):
        self.assertEqual(None,
                         check_in_some_org(
                             self.meta_connection,
                             responsible_id=0,
                             others_ids=[self.user['id']],
                             existing_org_id=self.organization['id']
                         ))

    def test_responsible_in_other_org(self):
        with self.assertRaises(UserIsAlreadyInAnotherOrg):
            check_in_some_org(
                self.meta_connection,
                responsible_id=self.user['id'],
                others_ids=[],
                existing_org_id=0
            )

    def test_others_in_other_org(self):
        with self.assertRaises(UserIsAlreadyInAnotherOrg):
            check_in_some_org(
                self.meta_connection,
                responsible_id=0,
                others_ids=[self.user['id']],
                existing_org_id=0
            )

    def test_user_is_outer_admin_in_other_org(self):
        outer_admin_uid = 100500
        create_organization(
            self.meta_connection,
            self.main_connection,
            label='google',
            admin_uid=outer_admin_uid
        )
        self.assertEqual(None,
                         check_in_some_org(
                             self.meta_connection,
                             responsible_id=0,
                             others_ids=[outer_admin_uid],
                             existing_org_id=self.organization['id']
                         ))

    def test_responsible_is_outer_admin_in_other_org(self):
        outer_admin_uid = 100500
        create_organization(
            self.meta_connection,
            self.main_connection,
            label='google',
            admin_uid=outer_admin_uid
        )
        self.assertEqual(None,
                         check_in_some_org(
                             self.meta_connection,
                             responsible_id=outer_admin_uid,
                             others_ids=[],
                             existing_org_id=self.organization['id']
                         ))


class TestBindTask(TestCase):
    def setUp(self):
        super(TestBindTask, self).setUp()
        self.user_id = 100500
        self.second_user_id = 100600
        self.resource_id = 'blah-minor'
        self.language='ru'
        self.relations = [
            {'object_type': 'user', 'object_id': self.user_id, 'name': 'own'},
            {'object_type': 'user', 'object_id': self.second_user_id, 'name': 'read-only'},
        ]

        def fake_side_effect(uid, *args, **kwargs):
            if uid == 100500:
                return {
                    'uid': uid,
                    'fields': {
                        'login': 'the-admin',
                        'firstname': 'Admin',
                        'lastname': 'Adminov',
                        'sex': 1,
                        'birth_date': '1980-01-01',
                    },
                    'default_email': 'the-admin@domain.ri',
                }
            elif uid == 100600:
                return {
                    'uid': uid,
                    'fields': {
                        'login': 'just-user',
                        'firstname': 'Just',
                        'lastname': 'Userof',
                        'sex': 1,
                        'birth_date': '1980-01-01',
                    },
                    'default_email': 'just-user@domain.ri',
                }
            else:
                # Такого вообще быть не должно
                raise NotImplementedError()

        self.mocked_blackbox.userinfo.side_effect = fake_side_effect
        self.org_id = self.organization['id']

    def test_successful_binding(self):
        # Проверяем, что пользователь добавится в организацию
        # что добавятся все пользователи перечисленные в связях
        # и что добавится ресурс и будет связан с указанными сотрудниками.

        bind_task_id = BindTask(self.main_connection).delay(
            org_id=self.org_id,
            user_id=self.user_id,
            service_slug=self.service['slug'],
            resource_id=self.resource_id,
            language=self.language,
            relations=self.relations,
        ).task_id
        #  выполним задачу в сихронном режиме
        SyncResult(self.main_connection, bind_task_id)

        # Посмотрим, завелись ли сотрудники в организации
        users = UserModel(self.main_connection).\
            filter(org_id=self.org_id, is_robot=False).\
            all()

        assert_that(
            users,
            contains_inanyorder(
                # Админ в организации был изначально
                has_entries(id=self.admin_uid),
                # а эти два сотруника добавил таск
                has_entries(id=self.user_id),
                has_entries(id=self.second_user_id),
            )
        )
        # Теперь проверим, что пользователей можно вытащить
        # по resource_id
        users = UserModel(self.main_connection) \
                .filter(org_id=self.org_id, resource=self.resource_id, resource_service=Ignore) \
                .all()

        # К ресурсу должны быть привязаны только два сотрудника
        assert_that(
            users,
            contains_inanyorder(
                has_entries(id=self.user_id),
                has_entries(id=self.second_user_id),
            )
        )

        # проверим что есть история
        self.meta_session.flush()
        data = self.meta_connection.execute(
            'select * from resource_history '
            'where resource_id = %(resource_id)s '
            'and action = \'add\'',
            resource_id=self.resource_id
        ).fetchall()
        self.assertEqual(len(data), 1)

    def test_successful_binding_with_existed_responsible(self):
        responsible = self.create_user()

        service = ServiceModel(self.meta_connection).create(
            slug='metrika',
            name='Metrika',
            client_id='someclient',
        )

        OrganizationServiceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service_id=service['id'],
            ready=True,
            responsible_id=responsible['id']
        )

        responsible_relations = ResourceRelationModel(self.main_connection).filter(
            org_id=self.org_id,
            user_id=responsible['id'],
            name='own',
        ).count()
        self.assertEqual(responsible_relations, 0)

        BindTask(self.main_connection).delay(
            org_id=self.org_id,
            user_id=self.user_id,
            service_slug=service['slug'],
            resource_id=self.resource_id,
            language=self.language,
            relations=self.relations,
        )
        self.process_tasks()

        # ответственному должна выдаться роль хотя его и не было в списке ролей
        relations = ResourceRelationModel(self.main_connection).filter(
            org_id=self.org_id,
            user_id=responsible['id'],
            name='own',
        ).count()
        self.assertEqual(relations, 1)

        # если кому-то еще в списке запросили роль владельца - ее быть не должно
        relations = ResourceRelationModel(self.main_connection).filter(
            org_id=self.org_id,
            user_id=self.user_id,
            name='own',
        ).count()
        self.assertEqual(relations, 0)

        # вместо нее должна выдаться другая роль зависящая от сервиса
        relations = ResourceRelationModel(self.main_connection).filter(
            org_id=self.org_id,
            user_id=self.user_id,
            name='edit',
        ).count()
        self.assertEqual(relations, 1)

    def test_successful_binding_without_existed_responsible_with_edit(self):
        # Проверяем что права не задваиваются для ответственного
        # если он передан с правом edit

        service = ServiceModel(self.meta_connection).create(
            slug='metrika',
            name='Metrika',
            client_id='someclient',
        )

        OrganizationServiceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service_id=service['id'],
            ready=True,
        )

        responsible_relations = ResourceRelationModel(self.main_connection).filter(
            org_id=self.org_id,
            user_id=self.user_id,
            name='own',
        ).count()
        self.assertEqual(responsible_relations, 0)

        self.relations = [
            {'object_type': 'user', 'object_id': self.user_id, 'name': 'own'},
            {'object_type': 'user', 'object_id': self.second_user_id, 'name': 'read-only'},
        ]

        BindTask(self.main_connection).delay(
            org_id=self.org_id,
            user_id=self.user_id,
            service_slug=service['slug'],
            resource_id=self.resource_id,
            language=self.language,
            relations=self.relations,
        )
        self.process_tasks()

        # у ответственного должна быть только одна роль
        relations = ResourceRelationModel(self.main_connection).filter(
            org_id=self.org_id,
            user_id=self.user_id,
        ).count()
        self.assertEqual(relations, 1)

    def test_successful_binding_without_existed_responsible_with_own(self):
        # Проверяем что права не задваиваются для ответственного
        # если он передан с правом own
        service = ServiceModel(self.meta_connection).create(
            slug='metrika',
            name='Metrika',
            client_id='someclient',
        )

        OrganizationServiceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service_id=service['id'],
            ready=True,
        )

        responsible_relations = ResourceRelationModel(self.main_connection).filter(
            org_id=self.org_id,
            user_id=self.user_id,
            name='own',
        ).count()
        self.assertEqual(responsible_relations, 0)

        self.relations = [
            {'object_type': 'user', 'object_id': self.user_id, 'name': 'edit'},
            {'object_type': 'user', 'object_id': self.second_user_id, 'name': 'read-only'},
        ]

        BindTask(self.main_connection).delay(
            org_id=self.org_id,
            user_id=self.user_id,
            service_slug=service['slug'],
            resource_id=self.resource_id,
            language=self.language,
            relations=self.relations,
        )
        self.process_tasks()

        # у ответственного должна быть только одна роль
        relations = ResourceRelationModel(self.main_connection).filter(
            org_id=self.org_id,
            user_id=self.user_id,
        ).count()
        self.assertEqual(relations, 1)

    def test_failed_binding(self):
        # Проверим, что если в таске CreateExistingAccountTask произойдет ошибка
        # главный таск BindTask завершится с ошибкой, ресурс не заведется.
        with patch.object(CreateExistingAccountTask, 'synchronous_call', side_effect=Exception):
            bind_task_id = BindTask(self.main_connection).delay(
                org_id=self.org_id,
                user_id=self.user_id,
                service_slug=self.service['slug'],
                resource_id=self.resource_id,
                language=self.language,
                relations=self.relations,
            ).task_id
            SyncResult(self.main_connection, bind_task_id)

        # Проверим, что BindTask завершилась с ошибкой
        assert_that(
            TaskModel(self.main_connection).get(bind_task_id),
            has_entries(
                state='failed',
            )
        )
        # Проверим, что рессурса нет
        resource = ResourceModel(self.main_connection).\
            filter(external_id=self.resource_id, org_id=self.org_id, service=self.service['slug']).\
            one()
        assert not resource


class TestCheckIsResourceExists(TestCase):
    def test_empty_resource_id(self):
        self.assertEqual(
            None,
            check_is_resource_exists(
                service_slug=self.service['slug'],
                resource_id=None,
            )
        )

    def test_non_existing_resource(self):
        self.assertEqual(
            None,
            check_is_resource_exists(
                service_slug=self.service['slug'],
                resource_id='qwerty123',
            )
        )

    def test_existing_resource(self):
        external_id = 'qwerty123'
        ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service=self.service['slug'],
            external_id=external_id,
        )
        with self.assertRaises(ResourceIsAlreadyExists):
            check_is_resource_exists(
                service_slug=self.service['slug'],
                resource_id=external_id,
            )

    def test_existing_resource_in_another_org(self):
        organization_2 = create_organization(
            self.meta_connection,
            self.main_connection,
            label='smth',
            source='smth',
        )['organization']

        external_id = 'qwerty123'
        ResourceModel(self.main_connection).create(
            org_id=organization_2['id'],
            service=self.service['slug'],
            external_id=external_id,
        )

        with self.assertRaises(ResourceIsAlreadyExists):
            check_is_resource_exists(
                service_slug=self.service['slug'],
                resource_id=external_id,
            )


class TestCheckAccessForBindForms(TestCase):
    def init(self, *args, **kwargs):
        super(TestCheckAccessForBindForms, self).init(*args, **kwargs)
        tvm.tickets['forms'] = 'ticket-2001259'

    def test_no_auth_data(self):
        with app.app_context():
            g.user_ticket = None
            with app.test_request_context('/'):
                with self.assertRaises(AuthenticationError):
                    check_access_for_bind_forms(
                        existing_org_id=111, resource_id=1234,
                    )

    def test_has_permission(self):
        with app.app_context():
            g.user_ticket = 'smth'
            with app.test_request_context('/'):
                with mocked_requests() as requests:
                    requests.get.return_value.status_code = 200
                    self.assertIsNone(
                        check_access_for_bind_forms(
                            existing_org_id=111, resource_id=1234,
                        )
                    )
                    expected_call = unittest.mock.call(
                        url='https://forms-biz-api.test.yandex.ru/admin/api/v2/surveys/1234/?detailed=0',
                        headers={'X-Ya-Service-Ticket': 'ticket-2001259', 'X-Ya-User-Ticket': 'smth', 'X-ORGS': '111'}
                    )
                    self.assertEqual(requests.get.call_args, expected_call)

    def test_server_error(self):
        with app.app_context():
            g.user_ticket = 'smth'
            with app.test_request_context('/'):
                with mocked_requests() as requests:
                    requests.get.return_value.status_code = 500
                    requests.get.return_value.text = "{'error': 'smth'}"
                    with self.assertRaises(ExternalServiceError):
                        check_access_for_bind_forms(
                            existing_org_id=111, resource_id=1234,
                        )
                    expected_call = unittest.mock.call(
                        url='https://forms-biz-api.test.yandex.ru/admin/api/v2/surveys/1234/?detailed=0',
                        headers={'X-Ya-Service-Ticket': 'ticket-2001259', 'X-Ya-User-Ticket': 'smth', 'X-ORGS': '111'}
                    )
                    self.assertEqual(requests.get.call_args, expected_call)

    def test_has_not_permission(self):
        with app.app_context():
            g.user_ticket = 'smth'
            with app.test_request_context('/'):
                with mocked_requests() as requests:
                    requests.get.return_value.status_code = 404
                    requests.get.return_value.text = '{}'
                    with self.assertRaises(AuthorizationError):
                        check_access_for_bind_forms(
                            existing_org_id=111, resource_id=1234,
                        )
                    expected_call = unittest.mock.call(
                        url='https://forms-biz-api.test.yandex.ru/admin/api/v2/surveys/1234/?detailed=0',
                        headers={'X-Ya-Service-Ticket': 'ticket-2001259', 'X-Ya-User-Ticket': 'smth', 'X-ORGS': '111'}
                    )
                    self.assertEqual(requests.get.call_args, expected_call)


class TestCheckAccessForBind(TestCase):

    def test_pass_kwargs_correct(self):
        with app.app_context():
            g.user_ticket = None
            tvm.tickets['forms'] = 'forms-ticket'
            with app.test_request_context('/'):
                with self.assertRaises(AuthenticationError):
                    check_access_for_bind(
                        existing_org_id=111, resource_id=1234, service_slug='forms'
                    )

    def test_fail_if_not_exists(self):
        with app.app_context():
            g.user_ticket = None
            with app.test_request_context('/'):
                with self.assertRaises(KeyError):
                    check_access_for_bind(
                        existing_org_id=111, resource_id=1234, service_slug='some service'
                    )


class TestCheckAccessForBindMetrika(TestCase):
    def init(self, *args, **kwargs):
        super(TestCheckAccessForBindMetrika, self).init(*args, **kwargs)
        tvm.tickets['metrika'] = 'ticket-2000269'

    def test_has_permission(self):
        # Проверка работы функции check_access_for_bind_metrika
        # при условии, что у пользователя есть доступы до счетчика
        with app.test_request_context('/'):
            with mocked_requests() as requests:
                requests.get.return_value.status_code = 200
                requests.get.return_value.json.return_value = {'has_permission': True}
                self.assertIsNone(
                    check_access_for_bind_metrika(
                        existing_org_id=111, resource_id=1234,
                        responsible_id=123, service_slug='metrika',
                    )
                )
                assert_called_once(
                    requests.get,
                    url='https://internalapi.test.metrika.yandex.net/connect/check_transfer_possible',
                    headers={'X-Ya-Service-Ticket': 'ticket-2000269'},
                    params={'uid': 123, 'counter_id': 1234},
                    timeout=0.3,
                )

    def test_has_not_permission(self):
        # Проверка работы функции check_access_for_bind_metrika
        # при условии, что у пользователя нет доступа до счетчика
        with app.test_request_context('/'):
            with mocked_requests() as requests:
                requests.get.return_value.status_code = 200
                requests.get.return_value.json.return_value = {'has_permission': False}
                with self.assertRaises(AuthorizationError):
                    check_access_for_bind_metrika(
                        existing_org_id=111, resource_id=1234,
                        responsible_id=123, service_slug='metrika'
                    )
                assert_called_once(
                    requests.get,
                    url='https://internalapi.test.metrika.yandex.net/connect/check_transfer_possible',
                    headers={'X-Ya-Service-Ticket': 'ticket-2000269'},
                    params={'uid': 123, 'counter_id': 1234},
                    timeout=0.3,
                )


class TestCheckAccessForBindYandexSprav(TestCase):
    def init(self, *args, **kwargs):
        super(TestCheckAccessForBindYandexSprav, self).init(*args, **kwargs)
        tvm.tickets['yandexsprav'] = 'ticket-2011748'

    @responses.activate
    def test_has_permission(self):
        resource_id = 1234
        responses.add(
            responses.GET,
            '{}/v1/companies/{}?expand=grants'.format(app.config['YANDEXSPRAV_HOST'], resource_id),
            json={
                'uids_with_modify_permission': [123, 456],
            },
        )
        with app.app_context():
            g.user_ticket = 'smth'
            self.assertIsNone(
                check_access_for_bind_yandexsprav(
                    resource_id=resource_id, responsible_id=123,
                    service_slug='yandexsprav',
                )
            )

    @responses.activate
    def test_has_not_permission(self):
        resource_id = 1234
        responses.add(
            responses.GET,
            '{}/v1/companies/{}?expand=grants'.format(app.config['YANDEXSPRAV_HOST'], resource_id),
            json={
                'uids_with_modify_permission': [],
            },
        )
        with app.app_context():
            g.user_ticket = 'smth'
            with self.assertRaises(AuthorizationError):
                check_access_for_bind_yandexsprav(
                    resource_id=resource_id, responsible_id=123,
                    service_slug='yandexsprav',
                )

    @responses.activate
    def test_on_exception_correct(self):
        resource_id = 1234
        responses.add(
            responses.GET,
            '{}/v1/companies/{}?expand=grants'.format(app.config['YANDEXSPRAV_HOST'], resource_id),
            status=500,
        )
        with app.app_context():
            g.user_ticket = 'smth'
            with self.assertRaises(ExternalServiceError):
                check_access_for_bind_yandexsprav(
                    resource_id=resource_id, responsible_id=123,
                    service_slug='yandexsprav',
                )


class TestCheckUserInCurrentOrg(TestCase):

    def test_in_same_org_success(self):
        self.assertIsNone(
            check_in_current_org(
                self.meta_connection,
                self.organization['id'],
                self.user['id'],
            )
        )

    def test_in_same_org_fail(self):
        with self.assertRaises(UserNotFoundInOrganizationError):
            check_in_current_org(
                self.meta_connection,
                self.organization['id'],
                123333333,
            )

    def test_in_same_org_dismissed_fail(self):
        UserModel(self.main_connection).update(
            update_data={'is_dismissed': True},
            filter_data={'id': self.user['id']},
        )
        with self.assertRaises(UserNotFoundInOrganizationError):
            check_in_current_org(
                self.meta_connection,
                self.organization['id'],
                self.user['id'],
            )


class TestCheckAccessForBindDirect(TestCase):

    def test_has_permission(self):
        self.assertIsNone(
            check_access_for_bind_direct(
                responsible_id=113000, resource_id=1234,
                relations=[
                    {
                        'object_type': 'user',
                        'object_id': 113000,
                        'name': '/user/employee/',
                    },
                    {
                        'object_type': 'user',
                        'object_id': 113000,
                        'name': '/user/chief/',
                    },
                    {
                        'object_type': 'user',
                        'object_id': 113003,
                        'name': '/user/employee/',
                    },
                ]
            )
        )

    def test_has_not_permission(self):
        with self.assertRaises(AuthorizationError):
            check_access_for_bind_direct(
                responsible_id=113000, resource_id=1234,
                relations=[
                    {
                        'object_type': 'user',
                        'object_id': 113000,
                        'name': '/user/employee/',
                    },
                    {
                        'object_type': 'user',
                        'object_id': 113000,
                        'name': '/user/hello/',
                    },
                    {
                        'object_type': 'user',
                        'object_id': 113003,
                        'name': '/user/chief/',
                    },
                ]
            )
