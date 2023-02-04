from unittest.mock import patch, ANY

from testutils import (
    TestCase,
    assert_not_called,
    assert_called_once,
)

import responses

from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.auth.scopes import scope
from intranet.yandex_directory.src.yandex_directory.common.models.types import ROOT_DEPARTMENT_ID
from intranet.yandex_directory.src.yandex_directory.core.maillist.tasks import CreateMaillistTask
from intranet.yandex_directory.src.yandex_directory.core.models.service import MAILLIST_SERVICE_SLUG, enable_service
from intranet.yandex_directory.src.yandex_directory.core.registrar.tasks import DomainVerifiedCallbackTask
from intranet.yandex_directory.src.yandex_directory.core.tasks import UpdateEmailFieldsTask, DomainModel
from intranet.yandex_directory.src.yandex_directory.auth import tvm
from testutils import tvm2_auth_success, get_auth_headers, override_settings
from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.connect_services.domenator import setup_domenator_client


class TestDomainOccupiedEvent(TestCase):
    def setUp(self):
        super(TestDomainOccupiedEvent, self).setUp()
        self.org1_admin_uid = 111
        self.org1 = self.create_organization(admin_uid=self.org1_admin_uid)
        self.org2 = self.create_organization()
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.org1['id'],
            MAILLIST_SERVICE_SLUG,
        )

    @tvm2_auth_success(100700, scopes=[scope.domenator])
    def test_with_old_owner(self):
        data = {
            'domain': 'domain.com',
            'new_owner': {
                'org_id': self.org1['id'],
                'new_domain_is_master': True,
            },
            'old_owner': {
                'org_id': self.org2['id'],
                'new_master': 'some-yaconnect-domain.com',
                'tech': True,
            },
            'registrar_id': 123,
        }
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.domenator.views.send_email_to_all_async') as send_email_to_all_async, \
            patch('intranet.yandex_directory.src.yandex_directory.core.views.domenator.views.apply_preset') as apply_preset, \
            patch('intranet.yandex_directory.src.yandex_directory.core.views.domenator.views.sms_domain_confirmed') as sms_domain_confirmed, \
            patch.object(UpdateEmailFieldsTask, 'delay') as update_email_fields_task, \
            patch.object(DomainVerifiedCallbackTask, 'delay') as domain_verified_callback_task, \
            patch.object(CreateMaillistTask, 'delay') as create_maillist_task:

            self.post_json('/domenator/event/domain-occupied/', data, expected_code=200)

            assert_called_once(
                send_email_to_all_async,
                ANY,
                org_id=self.org2['id'],
                domain=data['domain'],
                campaign_slug=app.config['SENDER_CAMPAIGN_SLUG']['DISABLE_DOMAIN_EMAIL'],
                new_master=data['old_owner']['new_master'],
                tech=data['old_owner']['tech'],
            )

            assert_called_once(
                update_email_fields_task,
                master_domain=data['old_owner']['new_master'],
                org_id=self.org2['id'],
            )

            assert_called_once(
                domain_verified_callback_task,
                registrar_id=data['registrar_id'],
                domain_name=data['domain'],
            )

            assert_called_once(
                apply_preset,
                ANY,
                ANY,
                data['new_owner']['org_id'],
                ANY,
            )

            assert_called_once(
                create_maillist_task,
                start_in=15,
                org_id=data['new_owner']['org_id'],
                department_id=ROOT_DEPARTMENT_ID,
                label='all',
                ignore_login_not_available=True,
            )

            assert_called_once(
                sms_domain_confirmed,
                ANY,
                ANY,
                data['new_owner']['org_id'],
                self.org1_admin_uid,
                data['domain'],
            )

    @tvm2_auth_success(100700, scopes=[scope.domenator])
    def test_old_owner_not_master_auto_handover(self):
        data = {
            'domain': 'domain.com',
            'new_owner': {
                'org_id': self.org1['id'],
                'new_domain_is_master': True,
            },
            'old_owner': {
                'org_id': self.org2['id'],
                'new_master': 'another-domain.com',
                'tech': False,
            },
        }
        with patch(
            'intranet.yandex_directory.src.yandex_directory.core.views.domenator.views.send_email_to_all_async') as send_email_to_all_async, \
            patch(
                'intranet.yandex_directory.src.yandex_directory.core.views.domenator.views.apply_preset') as apply_preset, \
            patch(
                'intranet.yandex_directory.src.yandex_directory.core.views.domenator.views.sms_domain_confirmed') as sms_domain_confirmed, \
            patch.object(UpdateEmailFieldsTask, 'delay') as update_email_fields_task, \
            patch.object(DomainVerifiedCallbackTask, 'delay') as domain_verified_callback_task, \
            patch.object(CreateMaillistTask, 'delay') as create_maillist_task:
            self.post_json('/domenator/event/domain-occupied/', data, expected_code=200)

            assert_called_once(
                send_email_to_all_async,
                ANY,
                org_id=self.org2['id'],
                domain=data['domain'],
                campaign_slug=app.config['SENDER_CAMPAIGN_SLUG']['DISABLE_DOMAIN_EMAIL'],
                new_master=data['old_owner']['new_master'],
                tech=data['old_owner']['tech'],
            )

            assert_called_once(
                update_email_fields_task,
                master_domain=data['old_owner']['new_master'],
                org_id=self.org2['id'],
            )

            assert_not_called(domain_verified_callback_task)

            assert_called_once(
                apply_preset,
                ANY,
                ANY,
                data['new_owner']['org_id'],
                ANY,
            )

            assert_called_once(
                create_maillist_task,
                start_in=15,
                org_id=data['new_owner']['org_id'],
                department_id=ROOT_DEPARTMENT_ID,
                label='all',
                ignore_login_not_available=True,
            )

            assert_called_once(
                sms_domain_confirmed,
                ANY,
                ANY,
                data['new_owner']['org_id'],
                self.org1_admin_uid,
                data['domain'],
            )

    @tvm2_auth_success(100700, scopes=[scope.domenator])
    def test_new_owner_already_has_master_and_old_owner_not_exist(self):
        data = {
            'domain': 'domain.com',
            'new_owner': {
                'org_id': self.org1['id'],
                'new_domain_is_master': False,
            },
        }
        with patch(
            'intranet.yandex_directory.src.yandex_directory.core.views.domenator.views.send_email_to_all_async') as send_email_to_all_async, \
            patch(
                'intranet.yandex_directory.src.yandex_directory.core.views.domenator.views.apply_preset') as apply_preset, \
            patch(
                'intranet.yandex_directory.src.yandex_directory.core.views.domenator.views.sms_domain_confirmed') as sms_domain_confirmed, \
            patch.object(UpdateEmailFieldsTask, 'delay') as update_email_fields_task, \
            patch.object(DomainVerifiedCallbackTask, 'delay') as domain_verified_callback_task, \
            patch.object(CreateMaillistTask, 'delay') as create_maillist_task:
            self.post_json('/domenator/event/domain-occupied/', data, expected_code=200)

            assert_not_called(send_email_to_all_async)
            assert_not_called(update_email_fields_task)
            assert_not_called(domain_verified_callback_task)
            assert_not_called(apply_preset)
            assert_not_called(create_maillist_task)

            assert_called_once(
                sms_domain_confirmed,
                ANY,
                ANY,
                data['new_owner']['org_id'],
                self.org1_admin_uid,
                data['domain'],
            )


class TestDomenatorProxyForDomains(TestCase):

    def setUp(self):
        super().setUp()
        tvm.tickets['domenator'] = 'tvm-ticket-domenator'
        tvm.tickets['gendarme'] = 'tvm-ticket-gendarme'
        setup_domenator_client(app)
        app.domenator.private_get_domains = lambda *args, **kwargs: []

        from intranet.yandex_directory.src.yandex_directory.core.features import (
            set_feature_value_for_organization,
            USE_DOMENATOR,
        )
        with responses.RequestsMock() as rsps:
            rsps.add(
                responses.POST,
                'https://domenator-test.yandex.net/api/domains/sync-connect',
                body='{}',
                status=200,
                content_type='application/json'
            )
            set_feature_value_for_organization(
                self.meta_connection,
                self.organization['id'],
                USE_DOMENATOR,
                True,
            )

    @override_settings(USE_DOMENATOR_ENABLED_FOR_DOMAINS=True)
    @responses.activate
    def test_add_domains(self):
        org_id = self.organization['id']
        responses.add(
            responses.GET,
            f'https://domenator-test.yandex.net/api/private/domains/?org_id={org_id}&owned=True',
            json=[],
            status=200,
        )
        responses.add(
            responses.GET,
            f'https://domenator-test.yandex.net/api/private/domains/?org_id={org_id}&master=True',
            json=[],
            status=200,
        )
        responses.add(
            responses.POST,
            'https://domenator-test.yandex.net/api/domains/',
            json={},
            status=200,
        )

        self.post_json('/domains/', {
            'name': 'test.com',
        })

    @override_settings(USE_DOMENATOR_ENABLED_FOR_DOMAINS=True)
    @responses.activate
    def test_delete_domain(self):
        org_id = self.organization['id']
        responses.add(
            responses.GET,
            f'https://domenator-test.yandex.net/api/private/domains/?org_id={org_id}&owned=True',
            json=[],
            status=200,
        )
        responses.add(
            responses.GET,
            f'https://domenator-test.yandex.net/api/private/domains/?org_id={org_id}&master=True',
            json=[],
            status=200,
        )
        domain = 'test.com'
        responses.add(
            responses.DELETE,
            f'https://domenator-test.yandex.net/api/domains/{domain}',
            json={},
            status=200,
        )

        self.delete_json(f'/domains/{domain}/', expected_code=200)

    @override_settings(USE_DOMENATOR_ENABLED_FOR_DOMAINS=True)
    @responses.activate
    def test_get_domains(self):
        org_id = self.organization['id']
        responses.add(
            responses.GET,
            f'https://domenator-test.yandex.net/api/private/domains/?org_id={org_id}&owned=True',
            json=[],
            status=200,
        )
        responses.add(
            responses.GET,
            f'https://domenator-test.yandex.net/api/private/domains/?org_id={org_id}&master=True',
            json=[],
            status=200,
        )

        fields = [
            'name', 'master', 'owned', 'mx', 'tech',
            'delegated', 'can_users_change_password', 'pop_enabled',
            'imap_enabled', 'postmaster_uid', 'org_id', 'country',
        ]

        fields_str = ','.join(fields)
        responses.add(
            responses.GET,
            f'https://domenator-test.yandex.net/api/domains/?org_ids={org_id}&fields={fields_str}',
            json=[{
                'org_id': org_id,
                'name': 'domain.com',
                'owned': True,
                'master': True,
                'tech': False,
                'mx': False,
                'delegated': False,
            }],
            status=200,
        )

        domain = 'test.com'
        DomainModel(self.main_connection).create(domain, org_id, owned=True)
        response = self.get_json('/v6/domains/', query_string={
            'fields': ','.join(fields)
        })

        assert response == [{
            'org_id': org_id,
            'name': 'domain.com',
            'owned': True,
            'master': True,
            'tech': False,
            'mx': False,
            'delegated': False,
            'can_users_change_password': True,
            'pop_enabled': False,
            'imap_enabled': False,
            'postmaster_uid': 0,
            'country': 'ru',
        }]

    @override_settings(USE_DOMENATOR_ENABLED_FOR_DOMAINS=True)
    @responses.activate
    def test_get_domains_from_two_organizations(self):
        org_id = self.organization['id']
        responses.add(
            responses.GET,
            f'https://domenator-test.yandex.net/api/private/domains/?org_id={org_id}&owned=True',
            json=[],
            status=200,
        )
        responses.add(
            responses.GET,
            f'https://domenator-test.yandex.net/api/private/domains/?org_id={org_id}&master=True',
            json=[],
            status=200,
        )

        admin_uid = self.user['id']

        org2 = self.create_organization(admin_uid=admin_uid)
        org2_id = org2['id']

        domain = 'org-2-domain.com'
        DomainModel(self.main_connection).delete(
            filter_data={
                'org_id': org2_id,
            },
            force_remove_all=True,
        )
        DomainModel(self.main_connection).create(domain, org2_id, owned=True)

        fields = [
            'name', 'master', 'owned', 'mx', 'tech',
            'delegated', 'can_users_change_password', 'pop_enabled',
            'imap_enabled', 'postmaster_uid', 'org_id', 'country',
        ]
        fields_str = ','.join(fields)

        responses.add(
            responses.GET,
            f'https://test.gendarme.mail.yandex.net/domain/status?name={domain}',
            json={
                "response": {
                    "last_added": "2020-07-06T14:12:23.198350+00:00",
                    "mx": {"match": True, "value": ""},
                    "last_check": "2020-08-28T14:48:06.745230+00:00",
                    "ns": {"match": False, "value": "ns1.expired.reg.ru"},
                    "dkim": [{"match": False, "value": "", "selector": "mail"}],
                    "spf": {"match": False, "value": ""},
                },
                "status": "ok",
            },
            status=200,
        )
        responses.add(
            responses.GET,
            f'https://domenator-test.yandex.net/api/domains/?org_ids={org_id}&fields={fields_str}',
            json=[{
                'org_id': org_id,
                'name': 'domain.com',
                'owned': True,
                'master': True,
                'tech': False,
                'mx': False,
                'delegated': False,
            }],
            status=200,
        )

        response = self.get_json(
            '/v6/domains/',
            headers=get_auth_headers(as_uid=self.user['id']),
            query_string={
                'fields': ','.join(fields)
            }
        )

        assert {'org_id': org_id, 'name': 'domain.com', 'owned': True, 'master': True, 'tech': False, 'mx': False,
                'delegated': False, 'can_users_change_password': True, 'pop_enabled': False, 'imap_enabled': False,
                'postmaster_uid': 0, 'country': 'ru'} in response

        assert {'delegated': False, 'master': False, 'mx': True, 'name': 'org-2-domain.com', 'org_id': org2_id,
                 'owned': True, 'can_users_change_password': True, 'pop_enabled': False, 'imap_enabled': False,
                 'postmaster_uid': 0, 'country': 'ru', 'tech': False} in response


class TestDomenatorPrivateMethods(TestCase):

    def setUp(self):
        super().setUp()
        tvm.tickets['domenator'] = 'tvm-ticket-domenator'
        setup_domenator_client(app)

    @responses.activate
    def test_private_get_domains(self):
        domain_data = {
            'org_id': 1,
            'name': 'test.com',
            'master': True,
            'owned': True,
            'display': True,
        }
        responses.add(
            responses.GET,
            'https://domenator-test.yandex.net/api/private/domains/?org_id=1&name=test.com',
            json=[domain_data],
            status=200,
        )

        response = app.domenator.private_get_domains(
            org_id=1,
            name='test.com',
        )

        assert [domain_data] == response

    @responses.activate
    def test_private_patch_domains(self):
        responses.add(
            responses.PATCH,
            'https://domenator-test.yandex.net/api/private/domains/?org_id=1&name=test.com',
            json={},
            status=200,
        )

        app.domenator.private_patch_domains(
            org_id=1,
            name='test.com',
            data={
                'master': False,
                'owned': False,
            },
        )
