from unittest.mock import patch

from testutils import TestCase, scopes, create_organization_without_domain, mocked_passport, frozen_time
from intranet.yandex_directory.src.yandex_directory.auth.scopes import scope
from intranet.yandex_directory.src.yandex_directory.common.utils import utcnow
from intranet.yandex_directory.src.yandex_directory.core.features import SSO_AVAILABLE
from intranet.yandex_directory.src.yandex_directory.core.features.utils import set_feature_value_for_organization
from intranet.yandex_directory.src.yandex_directory.core.models import DomainModel, OrganizationSsoSettingsModel
from intranet.yandex_directory.src.yandex_directory.sso.config_service.client import SsoConfigServiceNotFoundError

class TestSsoAllowed(TestCase):
    def setUp(self):
        super(TestSsoAllowed, self).setUp()

        self.organization = create_organization_without_domain(self.meta_connection, self.main_connection)['organization']
        self.org_id = self.organization['id']
        set_feature_value_for_organization(self.meta_connection, self.org_id, SSO_AVAILABLE, True)
        self.domain = DomainModel(self.main_connection).create('master-domain.ru', self.organization['id'], owned=True, master=True)

    def test_allowed(self):
        self._check_response('')

    def test_no_domain(self):
        DomainModel(self.main_connection).filter(name='master-domain.ru').delete(force_remove_all=True)
        self._check_response('no domain')

    def test_domain_not_owned(self):
        DomainModel(self.main_connection).filter(name='master-domain.ru').update(master=False, owned=False)
        self._check_response('no domain')

    def test_has_aliases(self):
        DomainModel(self.main_connection).create('alias-domain.ru', self.organization['id'], owned=True, master=False)
        self._check_response('has alias domains')

    def test_has_not_owned_alias(self):
        DomainModel(self.main_connection).create('alias-domain.ru', self.organization['id'], owned=False, master=False)
        self._check_response('has alias domains')

    def test_has_pdd_user(self):
        self.create_user(org_id=self.org_id, uid=1130000000000003)
        self._check_response('has pdd accounts')

    def test_has_pdd_user_dismissed(self):
        self.create_user(org_id=self.org_id, uid=1130000000000003, is_dismissed=True)
        self._check_response('')

    def _check_response(self, expected_reason):
        with scopes(scope.work_with_any_organization, scope.work_on_behalf_of_any_user, scope.read_sso_settings):
            response = self.get_json(
                '/organizations/sso/allowed/',
                as_org=self.org_id,
                as_uid=self.organization['admin_uid']
            )

        if expected_reason:
            assert response == {
                'allowed': False,
                'reason': expected_reason,
            }
        else:
            assert response == {'allowed': True}


class BaseTestSsoView(TestCase):
    def setUp(self):
        super(BaseTestSsoView, self).setUp()

        self.config_by_domain_id = self._get_config()
        self.config_by_entity_id = self._get_config()

        def mocked_sso_config_service_get(*args, **kwargs):
            return_config = self.config_by_entity_id if 'by_entity_id' in args[0] else self.config_by_domain_id
            if return_config is None:
                raise SsoConfigServiceNotFoundError()
            return return_config

        self.mocked_sso_config_service_get.side_effect = mocked_sso_config_service_get
        self.mocked_sso_config_service_delete.return_value = {}

        self.organization = create_organization_without_domain(self.meta_connection, self.main_connection)['organization']
        self.org_id = self.organization['id']
        set_feature_value_for_organization(self.meta_connection, self.org_id, SSO_AVAILABLE, True)
        self.domain = DomainModel(self.main_connection).create('master-domain.ru', self.organization['id'], owned=True, master=True)

        self.mocked_blackbox.hosted_domains.return_value = {
            'hosted_domains': [{
                'admin': '1130000000293941',
                'born_date': '2016-08-24 13:22:25',
                'default_uid': '0',
                'domain': 'master-domain.ru',
                'domid': '5678',
                'ena': '1',
                'master_domain': '',
                'mx': '0',
                'options': '{"organization_name": "Название организации"}'
            }]
        }

    def _get_config(self, config_id=11, domain_ids=None):
        return {
            'config_id': config_id,
            'entity_id': '<entity_id>',
            'domain_ids': domain_ids or [5678],
            'namespace': '360',
            'saml_config': {
                'single_sign_on_service': {
                    'url': 'SSO SIGN URL',
                    'binding': 'urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect'
                },
                'single_logout_service': {
                    'url': '',
                    'binding': 'urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect'
                },
                'x509_cert': {
                    'new': 'CERT NEW',
                    'old': 'CERT OLD'
                }
            },
            'oauth_config': {
                'client_id': '<client_id>',
            },
            'enabled': True
        }


class TestSsoSettingsView__get(BaseTestSsoView):
    def test_get(self):
        with patch('intranet.yandex_directory.src.yandex_directory.sso.utils.get_domain_info_from_blackbox', return_value={'domain_id': 5678}),\
             scopes(scope.work_with_any_organization, scope.work_on_behalf_of_any_user, scope.read_sso_settings):

            response = self.get_json('/organizations/sso/settings/', as_uid=self.organization['admin_uid'], as_org=self.organization['id'])

            assert self.mocked_sso_config_service_get.call_count == 1
            assert self.mocked_sso_config_service_get.call_args[0][0] == '/1/config/by_domain_id/5678/'

            assert response['entity_id'] == '<entity_id>'
            assert response['single_sign_on_service_url'] == 'SSO SIGN URL'
            assert response['certs'] == ['CERT NEW', 'CERT OLD']
            assert response['client_id'] == '<client_id>'
            assert response['enabled'] == True

    def test_not_found(self):
        with patch('intranet.yandex_directory.src.yandex_directory.sso.utils.get_domain_info_from_blackbox', return_value={'domain_id': 5678}),\
             scopes(scope.work_with_any_organization, scope.work_on_behalf_of_any_user, scope.read_sso_settings):

            self.config_by_domain_id = None

            self.get_json('/organizations/sso/settings/', as_uid=self.organization['admin_uid'], as_org=self.organization['id'], expected_code=404)

            assert self.mocked_sso_config_service_get.call_count == 1
            assert self.mocked_sso_config_service_get.call_args[0][0] == '/1/config/by_domain_id/5678/'


class TestSsoSettingsView__put(BaseTestSsoView):
    def test_check_access(self):
        with patch('intranet.yandex_directory.src.yandex_directory.sso.utils.get_domain_info_from_blackbox', return_value={'domain_id': 5678}),\
             scopes(scope.work_with_any_organization, scope.work_on_behalf_of_any_user, scope.write_sso_settings):

            new_organization = create_organization_without_domain(self.meta_connection, self.main_connection, admin_uid=self.organization['admin_uid'])['organization']
            set_feature_value_for_organization(self.meta_connection, self.org_id, SSO_AVAILABLE, False)
            set_feature_value_for_organization(self.meta_connection, new_organization['id'], SSO_AVAILABLE, True)
            DomainModel(self.main_connection).create('master-domain-2.ru', new_organization['id'], owned=True, master=True)

            self.put_json('/organizations/sso/settings/', data={}, as_uid=self.organization['admin_uid'], as_org=self.organization['id'], expected_code=403)
            self.put_json('/organizations/sso/settings/', data={}, as_uid=self.organization['admin_uid'], as_org=new_organization['id'], expected_code=422)

    def test_create_new_config(self):
        with patch('intranet.yandex_directory.src.yandex_directory.sso.views.get_domain_info_from_blackbox', return_value={'domain_id': 5678}),\
             scopes(scope.work_with_any_organization, scope.work_on_behalf_of_any_user, scope.write_sso_settings):

            self.config_by_entity_id = None
            self.config_by_domain_id = None

            self._send_request('<entity_id>')

            self._check_get()

            assert self.mocked_sso_config_service_put.call_count == 0

            assert self.mocked_sso_config_service_post.call_count == 1
            self._check_post(self.mocked_sso_config_service_post.call_args)

            self._check_settings()

    def test_change_entity_id_with_delete_old_config(self):
        with patch('intranet.yandex_directory.src.yandex_directory.sso.views.get_domain_info_from_blackbox', return_value={'domain_id': 5678}),\
             scopes(scope.work_with_any_organization, scope.work_on_behalf_of_any_user, scope.write_sso_settings):

            self.config_by_entity_id = None

            self._send_request('<new entity_id>')

            self._check_get(entity_id='%3Cnew+entity_id%3E')

            assert self.mocked_sso_config_service_put.call_count == 0
            assert self.mocked_sso_config_service_delete.call_count == 1
            self._check_delete(self.mocked_sso_config_service_delete.call_args)

            assert self.mocked_sso_config_service_post.call_count == 1
            self._check_post(self.mocked_sso_config_service_post.call_args, entity_id='<new entity_id>')

            self._check_settings()

    def test_change_entity_id_with_update_old_config(self):
        with patch('intranet.yandex_directory.src.yandex_directory.sso.views.get_domain_info_from_blackbox', return_value={'domain_id': 5678}),\
             scopes(scope.work_with_any_organization, scope.work_on_behalf_of_any_user, scope.write_sso_settings):

            self.config_by_domain_id = self._get_config(config_id=11, domain_ids=[1111, 5678])
            self.config_by_entity_id = None

            self._send_request('<new entity_id>')

            self._check_get(entity_id='%3Cnew+entity_id%3E')

            assert self.mocked_sso_config_service_put.call_count == 1
            self._check_put(self.mocked_sso_config_service_put.call_args, domain_ids=[1111])

            assert self.mocked_sso_config_service_delete.call_count == 0

            assert self.mocked_sso_config_service_post.call_count == 1
            self._check_post(self.mocked_sso_config_service_post.call_args, entity_id='<new entity_id>')

            self._check_settings()

    def test_change_entity_id_validate_fail(self):
        with patch('intranet.yandex_directory.src.yandex_directory.sso.views.get_domain_info_from_blackbox', return_value={'domain_id': 5678}),\
             scopes(scope.work_with_any_organization, scope.work_on_behalf_of_any_user, scope.write_sso_settings):

            self.config_by_domain_id = None
            self.config_by_entity_id = self._get_config(domain_ids=[1111])

            self._send_request('<entity_id>', sign_url='OTHER SIGN URL', expected_code=409)
            self._send_request('<entity_id>', certs=['OTHER FIRST CERT'], expected_code=409)
            self._send_request('<entity_id>', certs=['CERT NEW', 'OTHER SECOND CERT'], expected_code=409)
            self._send_request('<entity_id>', certs=['OTHER FIRST CERT', 'CERT OLD'], expected_code=409)
            self._send_request('<entity_id>', client_id='<other client_id>', expected_code=409)
            self._send_request('<entity_id>', client_id='', expected_code=409)

            assert self.mocked_sso_config_service_put.call_count == 0
            assert self.mocked_sso_config_service_delete.call_count == 0
            assert self.mocked_sso_config_service_post.call_count == 0

    def test_check_certs(self):
        with patch('intranet.yandex_directory.src.yandex_directory.sso.views.get_domain_info_from_blackbox', return_value={'domain_id': 5678}),\
             scopes(scope.work_with_any_organization, scope.work_on_behalf_of_any_user, scope.write_sso_settings):

            self.config_by_domain_id = None
            self.config_by_entity_id = self._get_config(domain_ids=[1111])

            self._send_request('<entity_id>', certs=['CERT NEW', 'CERT OLD'], expected_code=201)
            self._send_request('<entity_id>', certs=[' CERT NEW', 'CERT OLD '], expected_code=201)
            self._send_request('<entity_id>', certs=['\nCERT NEW', 'CERT OLD\n'], expected_code=201)
            self._send_request('<entity_id>', certs=['\nCERT \nNEW', 'CERT\n OLD\n'], expected_code=201)

    def _send_request(self, entity_id, sign_url="SSO SIGN URL", certs=None, client_id='<client_id>', expected_code=201):
        self.put_json(
            '/organizations/sso/settings/',
            as_org=self.organization['id'],
            as_uid=self.organization['admin_uid'],
            data={
                "entity_id": entity_id,
                "single_sign_on_service_url": sign_url,
                "certs": certs or ["CERT NEW", "CERT OLD"],
                "client_id": client_id,
            },
            expected_code=expected_code
        )

    def _check_settings(self):
        assert OrganizationSsoSettingsModel(self.main_connection).filter(org_id=self.org_id).one()['enabled'] == True
        assert OrganizationSsoSettingsModel(self.main_connection).filter(org_id=self.org_id).one()['provisioning_enabled'] == True

    def _check_get(self, entity_id='%3Centity_id%3E'):
        assert self.mocked_sso_config_service_get.call_count == 2
        assert self.mocked_sso_config_service_get.call_args_list[0][0][0] == '/1/config/by_domain_id/5678/'
        assert self.mocked_sso_config_service_get.call_args_list[1][0][0] == f'/1/config/by_entity_id/{entity_id}/'

    def _check_post(self, call_args, domain_ids=None, entity_id=None):
        assert call_args[0][0] == '/1/config/'
        self._check_data(call_args, domain_ids, entity_id)

    def _check_put(self, call_args, domain_ids=None, entity_id=None):
        self._check_data(call_args, domain_ids, entity_id)

    def _check_delete(self, call_args, config_id=11):
        assert call_args[0][0] == f'/1/config/by_config_id/{config_id}/'

    def _check_data(self, call_args, domain_ids, entity_id=None):
        assert call_args[1]['get_params'] == {'domain_id': domain_ids or [5678], 'entity_id': entity_id or '<entity_id>'}
        assert call_args[1]['post_params'] == {
            'saml_config': {
                'single_sign_on_service': {
                    'url': 'SSO SIGN URL',
                    'binding': 'urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect'
                },
                'single_logout_service': {
                    'url': '',
                    'binding': 'urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect'
                },
                'x509_cert': {
                    'new': 'CERT NEW',
                    'old': 'CERT OLD',
                }
            },
            'oauth_config': {
                'client_id': '<client_id>'
            },
            'enabled': True
        }


class TestSsoActionsView__put(BaseTestSsoView):
    def test_enable(self):
        with mocked_passport() as passport_mock,\
             patch('intranet.yandex_directory.src.yandex_directory.sso.utils.get_domain_info_from_blackbox', return_value={'domain_id': 5678}),\
             scopes(scope.work_with_any_organization, scope.work_on_behalf_of_any_user, scope.write_sso_settings),\
             frozen_time():

            self.put_json(
                '/organizations/sso/enable/',
                as_org=self.organization['id'],
                as_uid=self.organization['admin_uid'],
                data={},
            )
            self._check_mock(True)

            assert passport_mock.global_logout_domain.call_count == 0
            assert OrganizationSsoSettingsModel(self.main_connection).filter(
                    org_id=self.organization['id']).one()['last_sync'] == utcnow()

    def test_disable(self):
        with mocked_passport() as passport_mock,\
             patch('intranet.yandex_directory.src.yandex_directory.sso.utils.get_domain_info_from_blackbox', return_value={'domain_id': 5678}),\
             scopes(scope.work_with_any_organization, scope.work_on_behalf_of_any_user, scope.write_sso_settings):

            self.put_json(
                '/organizations/sso/disable/',
                as_org=self.organization['id'],
                as_uid=self.organization['admin_uid'],
                data={},
            )
            self._check_mock(False)

            assert passport_mock.global_logout_domain.call_count == 1
            assert passport_mock.global_logout_domain.call_args == ((), {'domain_id': '5678'})

    def test_disable_feature(self):
        with mocked_passport() as passport_mock,\
            patch('intranet.yandex_directory.src.yandex_directory.sso.utils.get_domain_info_from_blackbox', return_value={'domain_id': 5678}):
            self.post_json(
                '/organizations/%s/features/sso_available/disable/' % self.organization['id'],
                data={},
            )
            self.process_tasks()
            self._check_mock(False)

            assert passport_mock.global_logout_domain.call_count == 1
            assert passport_mock.global_logout_domain.call_args == ((), {'domain_id': '5678'})

    def _check_mock(self, expected_enabled):
        assert self.mocked_sso_config_service_get.call_count == 1
        assert self.mocked_sso_config_service_get.call_args[0][0] == '/1/config/by_domain_id/5678/'

        assert self.mocked_sso_config_service_post.call_count == 0

        assert self.mocked_sso_config_service_put.call_count == 1
        assert self.mocked_sso_config_service_put.call_args[0][0] == '/1/config/by_config_id/11/'
        assert self.mocked_sso_config_service_put.call_args[1]['get_params'] == {'domain_id': [5678],
                                                                                 'entity_id': '<entity_id>'}
        assert self.mocked_sso_config_service_put.call_args[1]['post_params'] == {
            "saml_config": {
                'single_sign_on_service': {
                    'url': 'SSO SIGN URL',
                    'binding': 'urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect'
                },
                'single_logout_service': {
                    'url': '',
                    'binding': 'urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect'
                },
                'x509_cert': {
                    'new': 'CERT NEW',
                    'old': 'CERT OLD'
                }
            },
            "oauth_config": {
                "client_id": "<client_id>"
            },
            "enabled": expected_enabled
        }

        assert OrganizationSsoSettingsModel(self.main_connection).filter(org_id=self.org_id).one()['enabled'] == expected_enabled


class TestGetOrganizationSsoFields(TestCase):
    api_version = 'v11'

    def test_without_sso_settings(self):
        self._check_response(False, False)

    def test_with_sso_settings_all_disabled(self):
        OrganizationSsoSettingsModel(self.main_connection).insert_or_update(self.organization['id'], False, False)
        OrganizationSsoSettingsModel(self.main_connection).update({'provisioning_enabled': False}, {'org_id': self.organization['id']})
        self._check_response(False, False)

    def test_with_sso_settings_all_enabled(self):
        OrganizationSsoSettingsModel(self.main_connection).insert_or_update(self.organization['id'], True, True)
        OrganizationSsoSettingsModel(self.main_connection).update({'provisioning_enabled': True}, {'org_id': self.organization['id']})
        self._check_response(True, True)

    def _check_response(self, is_sso_enabled, is_provisioning_enabled):
        response = self.get_json(
            '/organizations/%s/' % self.organization['id'],
            query_string={'fields': 'id,is_sso_enabled,is_provisioning_enabled'}
        )
        assert response['id'] == self.organization['id']
        assert response['is_sso_enabled'] == is_sso_enabled
        assert response['is_provisioning_enabled'] == is_provisioning_enabled
