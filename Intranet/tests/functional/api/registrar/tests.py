# coding: utf-8
#
import responses
from hamcrest import (
    assert_that,
    has_entries,
    has_properties,
    contains_inanyorder,
    not_none,
    none,
    has_key,
)
from sqlalchemy.orm import Session

from intranet.yandex_directory.src.yandex_directory.common.components import component_registry

from intranet.yandex_directory.src.yandex_directory.auth.scopes import scope
from testutils import (
    scopes,
    TestCase,
    create_organization,
    get_auth_headers,
    override_settings,
)
from unittest.mock import patch

from intranet.yandex_directory.src.yandex_directory.core.models import OrganizationModel, DomainModel
from intranet.yandex_directory.src.yandex_directory.auth import tvm
from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.connect_services.domenator import setup_domenator_client


class BaseTestCase(TestCase):
    pdd_id = 1
    pdd_version = 'new'
    registrar_id = 123
    admin_uid = 123123

    def get_short_registrar_data(self, pdd_id=None, admin_id=None, registrar_id=None):
        return {
            'id': registrar_id or self.registrar_id,
            'pdd_id': pdd_id or self.pdd_id,
            'admin_id': admin_id or self.admin_uid,
        }

    def get_registrar_data(self, pdd_id=None, pdd_version=None, admin_id=None, registrar_id=None):
        return {
            'password': 'p$wd',
            'id': registrar_id or self.registrar_id,
            'pdd_id': pdd_id or self.pdd_id,
            'name': 'Name',
            'admin_id': admin_id or self.admin_uid,
            'pdd_version': pdd_version or self.pdd_version,
            'oauth_client_id': '321',
            'validate_domain_url': 'http://validate_domain_url.com/',
            'domain_added_callback_url': 'http://domain_added_callback_url.com/',
            'domain_verified_callback_url': 'http://domain_verified_callback_url.com/',
            'domain_deleted_callback_url': 'http://domain_deleted_callback_url.com/',
            'payed_url': 'http://validate_domain_url.com/',
            'added_init': 'http://domain_added_callback_url.com/',
            'added': 'http://domain_verified_callback_url.com/',
            'delete_url': 'http://domain_deleted_callback_url.com/',
        }

    def add_domenator_registrar_response(self, pdd_id=None, pdd_version=None, admin_id=None, registrar_id=None, url_by_pdd=False):
        registrar_id = registrar_id or self.registrar_id
        pdd_version = pdd_version or self.pdd_version
        pdd_id = pdd_id or self.pdd_id

        registrar_id_url_param = registrar_id
        if url_by_pdd:
            registrar_id_url_param = f'{pdd_version}:{pdd_id}'

        registrar_data = self.get_registrar_data(pdd_id, pdd_version, admin_id, registrar_id)
        responses.add(
            responses.GET,
            f'https://domenator-test.yandex.net/api/registrar/{registrar_id_url_param}/',
            json=registrar_data,
        )

    def add_domenator_registrar_patch_response(self, registrar_id=None):
        registrar_id = registrar_id or self.registrar_id
        responses.add(
            responses.PATCH,
            f'https://domenator-test.yandex.net/api/registrar/{registrar_id}/',
            status=200,
        )

    def add_domenator_registrar_not_found_response(self, registrar_id):
        responses.add(
            responses.GET,
            f'https://domenator-test.yandex.net/api/registrar/{registrar_id}/',
            status=404,
        )

    def add_domenator_registrar_invalid_id_response(self, registrar_id):
        responses.add(
            responses.GET,
            f'https://domenator-test.yandex.net/api/registrar/{registrar_id}/',
            status=422,
        )

    def setUp(self):
        super(BaseTestCase, self).setUp()
        tvm.tickets['domenator'] = 'tvm-ticket-domenator'
        self.registrar = self.get_registrar_data()
        setup_domenator_client(app)
        app.domenator.private_get_domains = lambda *args, **kwargs: []


class TestRegistrarView(BaseTestCase):

    @responses.activate
    def test_get_404(self):
        registrar_id = 100500
        self.add_domenator_registrar_not_found_response(registrar_id)
        self.get_json(f'/registrar/{registrar_id}/', expected_code=404)

    @responses.activate
    def test_get_422(self):
        registrar_id = 'hello'
        self.add_domenator_registrar_invalid_id_response(registrar_id)
        self.get_json(f'/registrar/{registrar_id}/', expected_code=422)

    @responses.activate
    def test_get_200(self):
        self.add_domenator_registrar_response()
        response = self.get_json('/registrar/%s/' % self.registrar_id)
        assert_that(
            response,
            has_entries(
                id=self.registrar['id'],
                pdd_id=self.pdd_id,
                pdd_version='new',
                name=self.registrar['name'],
                admin_id=self.registrar['admin_id'],
                password=self.registrar['password'],
                oauth_client_id=self.registrar['oauth_client_id'],
                validate_domain_url=self.registrar['validate_domain_url'],
                domain_added_callback_url=self.registrar['domain_added_callback_url'],
                domain_verified_callback_url=self.registrar['domain_verified_callback_url'],
                domain_deleted_callback_url=self.registrar['domain_deleted_callback_url'],
            )
        )

    @responses.activate
    def test_get_200_by_pdd_id(self):
        registrar_id = f'{self.pdd_version}:{self.pdd_id}'

        self.add_domenator_registrar_response(url_by_pdd=True)
        response = self.get_json(f'/registrar/{registrar_id}/')

        assert_that(
            response,
            has_entries(
                id=self.registrar_id,
                pdd_id=self.pdd_id,
                pdd_version='new',
                name=self.registrar['name'],
                admin_id=self.registrar['admin_id'],
                password=self.registrar['password'],
                oauth_client_id=self.registrar['oauth_client_id'],
                validate_domain_url=self.registrar['validate_domain_url'],
                domain_added_callback_url=self.registrar['domain_added_callback_url'],
                domain_verified_callback_url=self.registrar['domain_verified_callback_url'],
                domain_deleted_callback_url=self.registrar['domain_deleted_callback_url'],
            )
        )


class TestOldRegistrarView(BaseTestCase):
    pdd_version = 'old'

    @responses.activate
    def test_get(self):
        self.add_domenator_registrar_response()
        response = self.get_json('/registrar/%s/' % self.registrar_id)
        assert_that(
            response,
            has_entries(
                id=self.registrar['id'],
                pdd_id=self.pdd_id,
                pdd_version='old',
                name=self.registrar['name'],
                admin_id=self.registrar['admin_id'],
                password=self.registrar['password'],
                oauth_client_id=self.registrar['oauth_client_id'],
                payed_url=self.registrar['validate_domain_url'],
                added_init=self.registrar['domain_added_callback_url'],
                added=self.registrar['domain_verified_callback_url'],
                delete_url=self.registrar['domain_deleted_callback_url'],
            )
        )

    @responses.activate
    def test_patch(self):
        # Проверяем, что некоторые поля можно попатчить
        new_url = 'http://new.validate_domain_url.com/'
        data = [
            {'patch_name': 'payed_url', 'value': new_url, 'check_name': 'validate_domain_url'},
            {'patch_name': 'added_init', 'value': new_url, 'check_name': 'domain_added_callback_url'},
            {'patch_name': 'added', 'value': new_url, 'check_name': 'domain_verified_callback_url'},
            {'patch_name': 'delete_url', 'value': new_url, 'check_name': 'domain_deleted_callback_url'},
            {'patch_name': 'oauth_client_id', 'value': 'new-client-id'},
            {'patch_name': 'name', 'value': 'new-name'},
            {'patch_name': 'password', 'value': 'new-password'},
        ]

        for item in data:
            patch_data = {
                item['patch_name']: item['value']
            }
            self.add_domenator_registrar_patch_response()
            self.patch_json('/registrar/%s/' % self.registrar_id, data=patch_data)


class TestRegistrarDomains(BaseTestCase):
    api_version = 'v10'

    def test_get_by_registrar_id(self):
        reg_org = create_organization(
            self.meta_connection,
            self.main_connection,
            label='foo',
        )
        org_id = reg_org['organization']['id']
        reg_id = self.registrar_id

        # Добавим второй домен
        second = 'second.ru'
        DomainModel(self.main_connection).create(second, org_id, owned=True)

        OrganizationModel(self.main_connection) \
            .filter(id=org_id) \
            .update(registrar_id=reg_id)

        # Если указан только регистратор, то должны отдаваться все его домены
        response = self.get_json(
            '/domains/?registrar_id={}'.format(reg_id),
            # Тут мы указываем anonymous, чтобы смоделировать ситуацию,
            # будто к нам пришёл прокси и у него в запросе нет ни пользователя ни организации.
            # Ведь он будет искать все организации регистратора.
            headers=get_auth_headers(as_anonymous=True)
        )
        assert_that(
            response,
            contains_inanyorder(
                has_entries(
                    name='foo.ws.autotest.yandex.ru',
                ),
                has_entries(
                    name=second,
                ),
            )
        )
        # Если указан ещё и name, то данные должны отдаваться только по нему
        response = self.get_json('/domains/?registrar_id={}&name={}'.format(reg_id, second))
        assert_that(
            response,
            contains_inanyorder(
                has_entries(
                    name=second,
                ),
            )
        )

    def test_filtering_by_registrar_is_under_a_scope(self):
        # Если скоупа manage_registrar нет, то мы должны отдавать 403
        url = '/domains/?registrar_id=1'

        # Эти скоупы API проксе надо будет иметь в любом случае, чтобы
        # воспользоваться ручкой
        standard_scopes = [scope.write_domains, scope.work_with_any_organization, scope.work_on_behalf_of_any_user]

        # Без скоупа - не ОК
        with scopes(*standard_scopes):
            self.get_json(url, expected_code=403)

        # Со скоупом всё работает!
        with scopes(scope.manage_registrar, *standard_scopes):
            self.get_json(url, expected_code=200)


class TestRegistrarV2ByTokenView(BaseTestCase):

    @responses.activate
    def test_get_404(self):
        token = 'unknown_token'
        responses.add(
            responses.GET,
            f'https://domenator-test.yandex.net/api/registrar/token/v2/{token}/',
            status=404,
        )
        self.get_json(f'/registrar/token/v2/{token}/', expected_code=404)

    @responses.activate
    def test_get_200(self):
        token = 'correct-token'
        responses.add(
            responses.GET,
            f'https://domenator-test.yandex.net/api/registrar/token/v2/{token}/',
            json=self.get_short_registrar_data(),
        )

        response = self.get_json('/registrar/token/v2/%s/' % token)
        assert_that(
            response,
            has_entries(
                id=self.registrar_id,
                pdd_id=self.pdd_id,
                admin_id=self.admin_uid,
            )
        )


class TestRegistrarV2TokenView(BaseTestCase):

    @responses.activate
    def test_get_404(self):
        registrar_id = 1111
        responses.add(
            responses.POST,
            f'https://domenator-test.yandex.net/api/registrar/{registrar_id}/token/',
            status=404,
        )
        self.post_json('/registrar/%s/token/' % registrar_id, data={}, expected_code=404)

    @responses.activate
    def test_get_token(self):
        token = 'some-token'
        responses.add(
            responses.POST,
            f'https://domenator-test.yandex.net/api/registrar/{self.registrar_id}/token/',
            json={
                'token': token
            }
        )

        response = self.post_json('/registrar/%s/token/' % self.registrar_id, data={}, expected_code=200)
        assert_that(
            response,
            has_entries(
                token=token
            )
        )

    @responses.activate
    def test_delete_token(self):
        responses.add(
            responses.DELETE,
            f'https://domenator-test.yandex.net/api/registrar/{self.registrar_id}/token/',
            status=200
        )
        self.delete_json('/registrar/%s/token/' % self.registrar_id, expected_code=200)


class TestRegistrarV2ByUidView(BaseTestCase):

    @responses.activate
    def test_get_404(self):
        uid = 123123
        responses.add(
            responses.GET,
            f'https://domenator-test.yandex.net/api/registrar/uid/v2/{uid}/',
            status=404
        )
        self.get_json('/registrar/uid/v2/123123/', expected_code=404)

    @responses.activate
    def test_get_200(self):
        uid = 123123
        responses.add(
            responses.GET,
            f'https://domenator-test.yandex.net/api/registrar/uid/v2/{uid}/',
            json=self.get_registrar_data(admin_id=uid)
        )

        response = self.get_json('/registrar/uid/v2/%s/' % uid)

        assert_that(
            response,
            has_entries(
                id=self.registrar['id'],
                pdd_id=self.pdd_id,
                admin_id=uid,
            )
        )


class TestDomenatorProxyForRegistrar(BaseTestCase):
    def setUp(self):
        super().setUp()
        tvm.tickets['domenator'] = 'tvm-ticket-domenator'

    @responses.activate
    @override_settings(DOMENATOR_REGISTRAR_PROXY=True)
    def test_get_registrar(self):
        registrar_id = 123
        domenator_response = {
            'password': 'password',
            'id': registrar_id,
            'name': 'name',
            'admin_id': 123,
            'pdd_version': 'new',
            'oauth_client_id': '321',
            'validate_domain_url': 'validate_domain_url',
            'domain_added_callback_url': 'domain_added_callback_url',
            'domain_verified_callback_url': 'domain_verified_callback_url',
            'domain_deleted_callback_url': 'domain_deleted_callback_url',
            'payed_url': 'validate_domain_url',
            'added_init': 'domain_added_callback_url',
            'added': 'domain_verified_callback_url',
            'delete_url': 'domain_deleted_callback_url',
        }

        responses.add(
            responses.GET,
            f'https://domenator-test.yandex.net/api/registrar/{registrar_id}/',
            json=domenator_response,
            status=200,
        )

        response_data = self.get_json(
            f'/registrar/{registrar_id}/',
            expected_code=200,
        )

        assert domenator_response == response_data

    @responses.activate
    @override_settings(DOMENATOR_REGISTRAR_PROXY=True)
    def test_get_registrar_404(self):
        registrar_id = 123
        responses.add(
            responses.GET,
            f'https://domenator-test.yandex.net/api/registrar/{registrar_id}/',
            status=404,
        )
        self.get_json(
            f'/registrar/{registrar_id}/',
            expected_code=404,
        )

    @responses.activate
    @override_settings(DOMENATOR_REGISTRAR_PROXY=True)
    def test_patch_registrar(self):
        registrar_id = 123
        data_for_patch = {
            'name': 'name',
            'password': 'password',
            'oauth_client_id': 'oauth_client_id',
            'validate_domain_url': 'validate_domain_url',
            'domain_added_callback_url': 'domain_added_callback_url',
            'domain_verified_callback_url': 'domain_verified_callback_url',
            'domain_deleted_callback_url': 'domain_deleted_callback_url',
            'payed_url': 'payed_url',
            'added_init': 'added_init',
            'added': 'added',
            'delete_url': 'delete_url',
        }
        responses.add(
            responses.PATCH,
            f'https://domenator-test.yandex.net/api/registrar/{registrar_id}/',
            status=200,
        )
        self.patch_json(
            f'/registrar/{registrar_id}/',
            data=data_for_patch,
            expected_code=200,
        )

    @responses.activate
    @override_settings(DOMENATOR_REGISTRAR_PROXY=True)
    def test_patch_registrar_404(self):
        registrar_id = 123
        data_for_patch = {
            'name': 'name',
            'password': 'password',
            'oauth_client_id': 'oauth_client_id',
            'validate_domain_url': 'validate_domain_url',
            'domain_added_callback_url': 'domain_added_callback_url',
            'domain_verified_callback_url': 'domain_verified_callback_url',
            'domain_deleted_callback_url': 'domain_deleted_callback_url',
            'payed_url': 'payed_url',
            'added_init': 'added_init',
            'added': 'added',
            'delete_url': 'delete_url',
        }
        responses.add(
            responses.PATCH,
            f'https://domenator-test.yandex.net/api/registrar/{registrar_id}/',
            status=404,
        )
        self.patch_json(
            f'/registrar/{registrar_id}/',
            data=data_for_patch,
            expected_code=404,
        )

    @responses.activate
    @override_settings(DOMENATOR_REGISTRAR_PROXY=True)
    def test_get_token_for_registrar(self):
        registrar_id = 123
        token = 'some-token'
        responses.add(
            responses.POST,
            f'https://domenator-test.yandex.net/api/registrar/{registrar_id}/token/',
            json={'token': token},
            status=200,
        )

        response = self.post_json(
            f'/registrar/{registrar_id}/token/',
            data={},
            expected_code=200,
        )

        assert response['token'] == token

    @responses.activate
    @override_settings(DOMENATOR_REGISTRAR_PROXY=True)
    def test_get_token_for_registrar_404(self):
        registrar_id = 123
        token = 'some-token'
        responses.add(
            responses.POST,
            f'https://domenator-test.yandex.net/api/registrar/{registrar_id}/token/',
            json={'token': token},
            status=404,
        )

        self.post_json(
            f'/registrar/{registrar_id}/token/',
            data={},
            expected_code=404,
        )

    @responses.activate
    @override_settings(DOMENATOR_REGISTRAR_PROXY=True)
    def test_delete_registrar_token(self):
        registrar_id = 123
        responses.add(
            responses.DELETE,
            f'https://domenator-test.yandex.net/api/registrar/{registrar_id}/token/',
            status=200,
        )
        self.delete_json(
            f'/registrar/{registrar_id}/token/',
            expected_code=200,
        )

    @responses.activate
    @override_settings(DOMENATOR_REGISTRAR_PROXY=True)
    def test_delete_registrar_token_404(self):
        registrar_id = 123
        responses.add(
            responses.DELETE,
            f'https://domenator-test.yandex.net/api/registrar/{registrar_id}/token/',
            status=404,
        )
        self.delete_json(
            f'/registrar/{registrar_id}/token/',
            expected_code=404,
        )

    @responses.activate
    @override_settings(DOMENATOR_REGISTRAR_PROXY=True)
    def test_get_registrar_by_token(self):
        token = 'some-token'
        registrar_data = {
            'id': 123,
            'admin_id': 321,
            'pdd_id': 213,
        }
        responses.add(
            responses.GET,
            f'https://domenator-test.yandex.net/api/registrar/token/v2/{token}/',
            json=registrar_data,
            status=200,
        )

        response = self.get_json(
            f'/registrar/token/v2/{token}/',
        )
        assert response == registrar_data

    @responses.activate
    @override_settings(DOMENATOR_REGISTRAR_PROXY=True)
    def test_get_registrar_by_token_404(self):
        token = 'some-token'
        responses.add(
            responses.GET,
            f'https://domenator-test.yandex.net/api/registrar/token/v2/{token}/',
            status=404,
        )

        self.get_json(
            f'/registrar/token/v2/{token}/',
            expected_code=404,
        )

    @responses.activate
    @override_settings(DOMENATOR_REGISTRAR_PROXY=True)
    def test_get_registrar_by_uid(self):
        admin_uid = 123
        registrar_data = {
            'name': 'name',
            'admin_id': 'admin_id',
            'pdd_version': 'pdd_version',
            'oauth_client_id': 'oauth_client_id',
            'validate_domain_url': 'validate_domain_url',
            'domain_added_callback_url': 'domain_added_callback_url',
            'domain_verified_callback_url': 'domain_verified_callback_url',
            'domain_deleted_callback_url': 'domain_deleted_callback_url',
            'payed_url': 'payed_url',
            'added_init': 'added_init',
            'added': 'added',
            'delete_url': 'delete_url',
        }
        responses.add(
            responses.GET,
            f'https://domenator-test.yandex.net/api/registrar/uid/v2/{admin_uid}/',
            json=registrar_data,
            status=200,
        )

        response = self.get_json(
            f'/registrar/uid/v2/{admin_uid}/',
        )
        assert response == registrar_data

    @responses.activate
    @override_settings(DOMENATOR_REGISTRAR_PROXY=True)
    def test_get_registrar_by_uid_404(self):
        admin_uid = 123
        responses.add(
            responses.GET,
            f'https://domenator-test.yandex.net/api/registrar/uid/v2/{admin_uid}/',
            status=404,
        )

        self.get_json(
            f'/registrar/uid/v2/{admin_uid}/',
            expected_code=404,
        )
