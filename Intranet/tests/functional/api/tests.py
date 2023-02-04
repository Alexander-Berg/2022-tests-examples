# -*- coding: utf-8 -*-
import json

from testutils import TestCase

from flask import g

from unittest.mock import (
    Mock,
    patch,
    ANY,
)

from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.core.utils import delete_user
from intranet.yandex_directory.src.yandex_directory.core.actions.base import save_action
from intranet.yandex_directory.src.yandex_directory.auth.scopes import scope
from intranet.yandex_directory.src.yandex_directory.core.models.organization import OrganizationModel

from hamcrest import (
    assert_that,
    equal_to,
    has_properties,
)

from testutils import (
    oauth_client,
    get_auth_headers,
    get_oauth_headers,
    get_auth_uid,
    create_organization,
)


class TestAuthentication(TestCase):
    def test_should_not_allow_requests_without_authorization_header(self):
        response = self.client.get('/')
        self.assertEqual(response.status_code, 401)
        response_data = json.loads(response.data)
        assert_that(
            response_data,
            equal_to({'message': 'Our API requires authentication', 'code': 'authentication-error'})
 )

    def test_should_not_allow_requests_with_bad_authorization_header(self):
        bad_headers = [
            None,
            '',
            'No',
            'No No No',
            'OAuth',
            'Token',
        ]
        for header in bad_headers:
            response = self.client.get('/', headers={'Authorization': header})
            self.assertEqual(response.status_code, 401)
            response_data = json.loads(response.data)
            assert_that(
                response_data,
                equal_to({'message': 'Our API requires authentication', 'code': 'authentication-error'}),
            )


class AuthenticatedUserByTokenBaseMixin(object):
    def test_should_forbid_access_for_user_without_organization(self):
        delete_user(
            self.meta_connection,
            self.main_connection,
            self.user['id']
        )
        headers = self.valid_headers.copy()
        response = self.client.get('/organization/', headers=headers)
        self.check_response(
            response=response,
            error_message='Organization is required for this operation',
            error_code='authorization-error',
            status_code=403,
        )


class AuthenticatedOrganizationBaseMixin(object):
    def test_forbid_access_for_not_exist_organization(self):
        headers = self.valid_headers.copy()

        # проставим несуществующий org-id
        headers['X-ORG-ID'] = 8347827

        response = self.client.get('/organization/', headers=headers)
        self.check_response(
            response=response,
            error_message='Wrong organization (org_id: {}, user.uid: {})'.format(headers['X-ORG-ID'], headers['X-UID']),
            error_code='authorization-error',
            status_code=403,
        )


class RevisionChangeTestMixin(object):
    def test_org_revision(self):
        # проверим, что в g-объект проставляется правильный номер ревизии организации
        self.get_json('/')
        old_revision = g.get('revision')
        self.assertIsNotNone(old_revision)
        self.assertEqual(old_revision, self.organization['revision'])

        # Запишем действие чтобы сменилась ревизия.
        # После этого действия, ревизия станет нулевой,
        # так как в базе изначально нет действий, а ревизия
        # считается по ним.
        revision = save_action(
            self.main_connection,
            org_id=self.organization['id'],
            name='some_action',
            author_id=1,
            object_value=self.create_user(),
        )
        old_revision = g.get('revision')

        # Теперь добавим ещё одно действие, чтобы поинкрементить ревизию
        revision = save_action(
            self.main_connection,
            org_id=self.organization['id'],
            name='some_action',
            author_id=1,
            object_value=self.create_user(),
        )

        # Так как мы тестируем вьюху, то
        # проставим ревизию в None
        g.revision = None
        # После вызова вьюшки, ревизия должна установиться в число
        self.get_json('/')
        revision = g.get('revision')
        self.assertIsNotNone(revision)
        self.assertEqual(revision, old_revision + 1)


class TestAuthenticationByOauthToken(RevisionChangeTestMixin,
                                     TestCase):

    def test_should_forbid_access_for_user_without_organization(self):
        delete_user(
            self.meta_connection,
            self.main_connection,
            self.user['id']
        )
        with oauth_client(uid=self.user['id'], scopes=[
                scope.work_with_any_organization,
                scope.work_on_behalf_of_any_user]):
            headers = get_oauth_headers()

            response = self.get_json(
                '/organization/',
                expected_code=403,
                headers=headers,
            )

        self.assertEqual(
            response['message'],
            'Organization is required for this operation',
        )

    def test_user(self):
        with oauth_client(uid=self.user['id'], scopes=[
                scope.work_with_any_organization,
                scope.work_on_behalf_of_any_user]):
            headers = get_oauth_headers()
            self.get_json('/', headers=headers)

        auth_type = g.get('auth_type')
        assert_that(
            auth_type,
            equal_to('oauth')
        )

        user = g.get('user')
        assert_that(
            user,
            has_properties(
                # Миддльварь должна по OAuth токену определить, что за пользователь
                # пришёл, и какой у него UID.
                passport_uid=get_auth_uid(),
                # А IP пользователя взять из заголовка
                ip=headers['X-Real-IP'],
            )
        )

        service = g.get('service')
        assert_that(
            service,
            has_properties(
                name='Autotest',
                identity='service-slug',
                is_internal=True,
            )
        )


class TestAuthenticationByToken(AuthenticatedOrganizationBaseMixin,
                                AuthenticatedUserByTokenBaseMixin,
                                RevisionChangeTestMixin,
                                TestCase):
    def setUp(self):
        super(TestAuthenticationByToken, self).setUp()

        self.service_for_test = app.config['INTERNAL_SERVICES_BY_IDENTITY']['autotest']
        self.valid_headers = get_auth_headers()

    def check_response(self, response, error_message, error_code, status_code, params=None):
        self.assertEqual(response.status_code, status_code)
        response_data = json.loads(response.data)
        expected = {
            'message': error_message,
            'code': error_code,
        }
        if params is not None:
            expected['params'] = params

        assert_that(
            response_data,
            equal_to(expected)
        )

    def check_token_auth_error(self,
                               response,
                               error_message='Bad credentials',
                               error_code='authentication-error',
                               params=None,
                               status_code=401):
        self.check_response(response, error_message, error_code, status_code, params)
        self.assertEqual(response.headers.get('WWW-Authenticate'), "Token")

    def test_should_not_allow_requests_with_not_valid_token(self):
        self.check_token_auth_error(
            self.client.get('/', headers={'Authorization': 'Token 123456'})
        )

    def test_should_not_allow_requests_with_valid_token_and_x_uid_header_but_without_x_user_ip_header(self):
        headers = self.valid_headers.copy()
        # уберём заголовок и проверим, будет ли ошибка аутентификации
        del headers['X-USER-IP']

        response = self.client.get('/', headers=headers)
        self.check_response(
            response=response,
            error_message='Header "{header}" is required.',
            # Так как поля должны быть переданы вместе, то отсутствие
            # заголовка X-User-IP расценивается, как BadRequest.
            status_code=400,
            error_code='header_is_required',
            params={'header': 'X-User-IP'},
        )

    def test_uid_less_zero(self):
        # если uid < 0, то отдаем 400

        headers = self.valid_headers.copy()
        # портим uid
        headers['x-uid'] = '-100'

        self.check_response(
            self.client.get('/', headers=headers),
            error_message='Header "{header}" should be a positive integer.',
            error_code='header_should_be_integer',
            params={'header': 'X-UID'},
            status_code=400,
        )

    def test_uid_bignumber(self):
        # если uid > 2^63-1, то отдаем 400
        headers = self.valid_headers.copy()
        # портим uid
        headers['x-uid'] = str(2**63)

        self.check_response(
            self.client.get('/', headers=headers),
            error_message='Header "{header}" should be a positive integer.',
            error_code='header_should_be_integer',
            params={'header': 'X-UID'},
            status_code=400,
        )

    def test_uid_not_number(self):
        # если uid не целое число, то отдаем 400
        headers = self.valid_headers.copy()
        # теперь испортим UID
        headers['X-UID'] = 'not_number'

        self.check_response(
            self.client.get('/', headers=headers),
            error_message='Header "{header}" should be a positive integer.',
            error_code='header_should_be_integer',
            params={'header': 'X-UID'},
            status_code=400,
        )

    def test_should_allow_requests_with_valid_token__x_uid_header__and__x_user_ip_header(self):
        response = self.client.get('/', headers=self.valid_headers)
        self.assertEqual(response.status_code, 200)

    def test_service(self):
        with self.client as client:
            response = client.get('/', headers=self.valid_headers)
            self.assertEqual(response.status_code, 200)
            service = g.get('service')
            scopes = g.get('scopes')
            self.assertIsNotNone(service)
            self.assertEqual(service.name, self.service_for_test['name'])
            self.assertEqual(service.identity, self.service_for_test['identity'])
            self.assertTrue(service.is_internal)
            # Я решил, что лучше явно тут написать число требуемых скоупов.
            # их должно быть столько, сколько описано в модуле scopes.
            assert_that(
                scopes,
                equal_to(['*']),
            )

    def test_user(self):
        with self.client as client:
            response = client.get('/', headers=self.valid_headers)
            self.assertEqual(response.status_code, 200)
            user = g.get('user')
            self.assertIsNotNone(user)
            self.assertEqual(user.passport_uid, int(self.valid_headers['X-UID']))
            self.assertEqual(user.ip, self.valid_headers['X-USER-IP'])

    def test_org_id(self):
        with self.client as client:
            valid_headers = get_auth_headers(
                as_org={'id': self.organization['id']}
            )
            response = client.get('/', headers=valid_headers)
            self.assertEqual(response.status_code, 200)
            org_id = g.get('org_id')
            self.assertIsNotNone(org_id)
            self.assertEqual(org_id, int(valid_headers['X-ORG-ID']))

            service = g.get('service')
            self.assertIsNotNone(service)
            self.assertTrue(service.is_internal)

    def test_admin_uid(self):
        with self.client as client:
            valid_headers = get_auth_headers(
                as_outer_admin={
                    'id': self.outer_admin['id'],
                    'org_id': self.organization['id'],
                }
            )
            response = client.get('/', headers=valid_headers)
            self.assertEqual(response.status_code, 200)

            org_id = g.get('org_id')
            self.assertIsNotNone(org_id)
            self.assertEqual(org_id, int(valid_headers['X-ORG-ID']))

            user = g.get('user')
            self.assertIsNotNone(user)
            self.assertEqual(user.passport_uid, int(valid_headers['X-UID']))


class TestRequestId(TestCase):
    def setUp(self):
        super(TestRequestId, self).setUp()

        self.headers = get_auth_headers()

    def test_should_generate_random_request_id(self):
        with self.client as client:
            client.get('/', headers=self.headers)
            self.assertTrue('request_id' in g)
            self.assertEqual(len(g.request_id), 32)

    def test_should_generate_random_request_id_for_every_request(self):
        with self.client as client:
            client.get('/', headers=self.headers)
            request_one_id = g.request_id
            client.get('/', headers=self.headers)
            request_two_id = g.request_id
            self.assertNotEqual(request_one_id, request_two_id)

    def test_should_user_request_id_from_header_if_it_sent(self):
        self.headers['x-request-id'] = 'polina-sosisa'
        with self.client as client:
            client.get('/', headers=self.headers)
            self.assertEqual(g.request_id, self.headers['x-request-id'])

    def test_should_return_request_id_as_response_header(self):
        self.headers['x-request-id'] = 'polina-sosisa'
        with self.client as client:
            response = client.get('/', headers=self.headers)
            self.assertEqual(response.headers.get('x-request-id'), self.headers['x-request-id'])

    def test_should_validate_sent_request_id_header(self):
        experiments = [
            {
                'value': 'test',
                'valid': True
            },
            {
                'value': 't' * 100,
                'valid': True
            },
            {
                'value': 't-t',
                'valid': True
            },
            {
                'value': 't1-t2',
                'valid': True
            },
            {
                'value': 'T1-T2',
                'valid': True
            },
            {
                'value': 't' * 101,
                'valid': False
            },
            {
                'value': 't t',
                'valid': False
            },
            {
                'value': 'сосиса',
                'valid': True
            },
        ]

        for exp in experiments:
            self.headers['x-request-id'] = exp['value']
            response = self.client.get('/', headers=self.headers)
            if exp['valid']:
                self.assertEqual(response.status_code, 200, msg='Value "%s" should be valid' % exp['value'])
            else:
                self.assertEqual(response.status_code, 422, msg='Value "%s" should be invalid' % exp['value'])


class TestXRevisionMiddleware(TestCase):
    def test_should_return_x_revision_header_for_organization_even_if_revision_eq_zero(self):
        # проверяем, что заголовок x-revision вернется и будет совпадать с ревизией организации,
        new_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']

        new_organization = OrganizationModel(self.main_connection).find(
            filter_data={'id': new_organization['id']},
            fields=['id', 'revision'],
        )[0]

        auth_headers = get_auth_headers(as_org=new_organization['id'])
        response = self.client.get('/', headers=auth_headers)

        assert_that(response.status_code, equal_to(200))
        assert_that(response.headers.get('X-Revision'), equal_to(str(new_organization['revision'])))

    def test_should_return_x_revision_header_for_organization(self):
        response = self.client.get('/', headers=get_auth_headers())

        assert_that(response.status_code, equal_to(200))
        assert_that(response.headers.get('X-Revision'), equal_to(str(self.organization['revision'])))


class TestCloseServiceBySmokeTestResultsMiddleware(TestCase):
    def test_middleware_should_close_access_to_all_views_except_ping_if_service_unavailable(self):
        # Если is_need_to_close_service возвращает True, все ручки кроме /ping/ должны быть недоступны

        # Тут run_smoke_checks надо запатчить, чтобы в тестах не запускались
        # все проверки в потоках
        with patch('intranet.yandex_directory.src.yandex_directory.common.views.main.get_smoke_tests_results') as get_smoke_tests_results, \
             patch('intranet.yandex_directory.src.yandex_directory.auth.middlewares.backpressure.is_need_to_close_service') as is_need_to_close_service:

            is_need_to_close_service.return_value = True
            get_smoke_tests_results.return_value = {'has_errors_in_vital_services': True}

            self.get_json('/', expected_code=503)
            # Тут ручка ping возвращает 500, а не 503, потому что
            # были обнаружены ошибки в жизненно-важных сервисах
            self.get_json('/ping/', expected_code=500)

            # если is_need_to_close_service возвращает False, все ручки должны быть доступны
            is_need_to_close_service.return_value = False
            get_smoke_tests_results.return_value = {'has_errors_in_vital_services': False}

            self.get_json('/')
            self.get_json('/ping/')
