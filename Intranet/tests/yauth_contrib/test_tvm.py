# -*- coding: utf-8 -*-
import responses

from django.conf import settings
from django.test import TestCase, override_settings
from unittest.mock import patch, MagicMock

from events.common_app.helpers import override_cache_settings
from events.yauth_contrib.mechanisms.tvm2 import Mechanism

TEST_TVM2_CLIENT = 321


class MockServiceTicket:
    def __init__(self, src):
        self.src = src


class MockUserTicket:
    def __init__(self, default_uid):
        self.default_uid = default_uid


class MockTVM2:
    def __init__(self, src, default_uid=None):
        self.src = src
        self.default_uid = default_uid

    def parse_service_ticket(self, service_ticket):
        return MockServiceTicket(self.src)

    def parse_user_ticket(self, user_ticket):
        if self.default_uid:
            return MockUserTicket(self.default_uid)


class MockRequest:
    def __init__(self, orgs=None):
        self.orgs = orgs


@override_settings(
    IS_BUSINESS_SITE=True,
    TVM2_CLIENT_ID=TEST_TVM2_CLIENT,
    YAUTH_TVM2_SERVICE_CLIENTS=[TEST_TVM2_CLIENT],
)
@override_cache_settings()
class TestTVM2CloudUserAuth(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.backend = Mechanism()

    def register_org(self, cloud_uid, data=None, status=200):
        responses.add(
            responses.GET,
            'https://api-integration-qa.directory.ws.yandex.net/v6/organizations/',
            json=data or {},
            headers={'X-Cloud-UID': cloud_uid},
            status=status,
        )

    def register_cloud_uid(self, cloud_uid, data=None, status=200):
        responses.add(
            responses.GET,
            f'https://api-integration-qa.directory.ws.yandex.net/v6/users/cloud/{cloud_uid}/',
            json=data or {},
            headers={'X-Cloud-UID': cloud_uid},
            status=status,
        )

    @responses.activate
    def test_cloud_auth_success(self):
        cloud_uid = 'bfb0imi4jkldenpjpcsr'
        org_id = '104562'
        self.register_org(cloud_uid, data={
            'result': [org_id],
            'links': {},
        })
        self.register_cloud_uid(cloud_uid, data={
            'email': 'user1@foobar.yandex.ru',
            'nickname': 'user1',
            'name': {'first': 'John', 'last': 'Doe'},
        })

        with patch('events.yauth_contrib.mechanisms.tvm2.get_tvm2_client') as mock_get_tvm2_client:
            mock_get_tvm2_client.return_value = MockTVM2(TEST_TVM2_CLIENT)
            yauser = self.backend.apply(
                request=MockRequest([org_id]),
                service_ticket='smth',
                user_ip=None,
                uid='123',
                cloud_uid=cloud_uid,
                org_id=org_id,
            )
            self.assertEqual(yauser.uid, '123')
            self.assertEqual(yauser.cloud_uid, cloud_uid)
            user = yauser.get_user()

        self.assertFalse(yauser.is_anonymous)
        self.assertFalse(user.is_anonymous)

        self.assertEqual(yauser.authenticated_by.mechanism_name, 'tvm2')

        self.assertEqual(yauser.uid, '123')
        self.assertEqual(yauser.cloud_uid, cloud_uid)
        self.assertEqual(yauser.raw_service_ticket, 'smth')

        self.assertEqual(user.uid, '123')
        self.assertEqual(user.cloud_uid, cloud_uid)
        self.assertEqual(user.uid_type, 'cloud')
        self.assertTrue(user.username.startswith('gu_'))

    def test_cloud_auth_fail_wrong_ticket(self):
        cloud_uid = 'bfb0imi4jkldenpjpcsr'
        org_id = '104562'
        with patch('events.yauth_contrib.mechanisms.tvm2.get_tvm2_client') as patch_client:
            patch_client.return_value = MagicMock()
            patch_client.return_value.parse_service_ticket = MagicMock(return_value=None)
            yauser = self.backend.apply(
                request=MockRequest([org_id]),
                service_ticket='smth',
                user_ip=None,
                cloud_uid=cloud_uid,
                org_id=org_id,
            )
            user = yauser.get_user()

        self.assertTrue(yauser.is_anonymous)
        self.assertTrue(user.is_anonymous)

        self.assertEqual(yauser.authenticated_by.mechanism_name, 'tvm2')

        self.assertIsNone(yauser.uid)
        self.assertIsNone(user.uid)
        self.assertEqual(user.id, settings.MOCK_PROFILE_ID)

    @responses.activate
    def test_cloud_auth_fail_wrong_uid(self):
        cloud_uid = 'bfb0imi4jkldenpjpcsr9999'
        self.register_org(cloud_uid, status=200, data={
            'result': [],
            'links': {},
        })

        with patch('events.yauth_contrib.mechanisms.tvm2.get_tvm2_client'):
            yauser = self.backend.apply(
                request=MockRequest([]),
                service_ticket='smth',
                user_ip=None,
                cloud_uid=cloud_uid,
                org_id=None,
            )
            user = yauser.get_user()

        self.assertTrue(yauser.is_anonymous)
        self.assertTrue(user.is_anonymous)

        self.assertEqual(yauser.authenticated_by.mechanism_name, 'tvm2')
        self.assertIsNone(yauser.uid)

        self.assertIsNone(user.uid)
        self.assertEqual(user.id, settings.MOCK_PROFILE_ID)


@override_settings(
    TVM2_CLIENT_ID=TEST_TVM2_CLIENT,
    YAUTH_TVM2_SERVICE_CLIENTS=[TEST_TVM2_CLIENT],
)
@override_cache_settings()
class TestTVM2XUidAuth(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.backend = Mechanism()

    def register_mimino(self, body=None, status=200):
        responses.add(
            responses.GET,
            'http://blackbox-mimino.yandex.net/blackbox',
            body=body,
            status=status,
            content_type='text/xml',
        )

    def register_yateam(self, body=None, status=200):
        responses.add(
            responses.GET,
            'http://blackbox.yandex-team.ru/blackbox',
            body=body,
            status=status,
            content_type='text/xml',
        )

    @responses.activate
    @override_settings(IS_INTERNAL_SITE=False)
    def test_external_uid_auth_success(self):
        uid = '123'
        org_id = '104562'
        self.register_mimino(f'<doc><uid>{uid}</uid></doc>')
        with patch('events.yauth_contrib.mechanisms.tvm2.get_tvm2_client') as mock_get_tvm2_client:
            mock_get_tvm2_client.return_value = MockTVM2(TEST_TVM2_CLIENT)
            yauser = self.backend.apply(
                request=MockRequest([org_id]),
                service_ticket='smth',
                user_ip=None,
                uid=uid,
                org_id=org_id,
            )
            user = yauser.get_user()

        self.assertFalse(yauser.is_anonymous)
        self.assertFalse(user.is_anonymous)

        self.assertEqual(yauser.authenticated_by.mechanism_name, 'tvm2')

        self.assertEqual(yauser.uid, uid)
        self.assertIsNone(yauser.default_email)
        self.assertEqual(yauser.raw_service_ticket, 'smth')
        self.assertIsNone(yauser.blackbox_result)
        self.assertEqual(yauser.service_ticket.src, TEST_TVM2_CLIENT)

        self.assertEqual(user.uid, uid)
        self.assertEqual(user.email, '')
        self.assertEqual(user.uid_type, 'external')
        self.assertTrue(user.username.startswith('gu_'))

    @responses.activate
    @override_settings(IS_INTERNAL_SITE=True)
    def test_internal_uid_auth_success(self):
        uid = '123'
        org_id = '104562'
        self.register_yateam(f'<doc><uid>{uid}</uid></doc>')
        with patch('events.yauth_contrib.mechanisms.tvm2.get_tvm2_client') as mock_get_tvm2_client:
            mock_get_tvm2_client.return_value = MockTVM2(TEST_TVM2_CLIENT)
            yauser = self.backend.apply(
                request=MockRequest([org_id]),
                service_ticket='smth',
                user_ip=None,
                uid=uid,
                org_id=org_id,
            )
            user = yauser.get_user()

        self.assertFalse(yauser.is_anonymous)
        self.assertFalse(user.is_anonymous)

        self.assertEqual(yauser.authenticated_by.mechanism_name, 'tvm2')

        self.assertEqual(yauser.uid, uid)
        self.assertIsNone(yauser.default_email)
        self.assertEqual(yauser.raw_service_ticket, 'smth')
        self.assertIsNone(yauser.blackbox_result)
        self.assertEqual(yauser.service_ticket.src, TEST_TVM2_CLIENT)

        self.assertEqual(user.uid, uid)
        self.assertEqual(user.email, '')
        self.assertEqual(user.cloud_uid, None)
        self.assertTrue(user.username.startswith('gu_'))

    @responses.activate
    @override_settings(IS_INTERNAL_SITE=True)
    def test_should_return_anonymous_user_with_service_ticket(self):
        org_id = '104562'
        with patch('events.yauth_contrib.mechanisms.tvm2.get_tvm2_client') as mock_get_tvm2_client:
            mock_get_tvm2_client.return_value = MockTVM2(TEST_TVM2_CLIENT)
            yauser = self.backend.apply(
                request=MockRequest([org_id]),
                service_ticket='smth',
                user_ip=None,
                org_id=org_id,
            )
            user = yauser.get_user()

        self.assertTrue(yauser.is_anonymous)
        self.assertTrue(user.is_anonymous)
        self.assertEqual(yauser.service_ticket.src, TEST_TVM2_CLIENT)

    @responses.activate
    @override_settings(IS_INTERNAL_SITE=True)
    def test_should_return_anonymous_user_with_service_ticket_for_not_valid_service_client(self):
        uid = '123'
        service_client = 999
        org_id = '104562'
        with patch('events.yauth_contrib.mechanisms.tvm2.get_tvm2_client') as mock_get_tvm2_client:
            mock_get_tvm2_client.return_value = MockTVM2(service_client)
            yauser = self.backend.apply(
                request=MockRequest([org_id]),
                service_ticket='smth',
                user_ip=None,
                uid=uid,
                org_id=org_id,
            )
            user = yauser.get_user()

        self.assertTrue(yauser.is_anonymous)
        self.assertTrue(user.is_anonymous)
        self.assertEqual(yauser.service_ticket.src, service_client)

    @responses.activate
    @override_settings(IS_INTERNAL_SITE=True)
    def test_should_return_anonymous_user_without_service_ticket(self):
        org_id = '104562'
        with patch('events.yauth_contrib.mechanisms.tvm2.get_tvm2_client') as mock_get_tvm2_client:
            mock_get_tvm2_client.return_value = MockTVM2(None)
            yauser = self.backend.apply(
                request=MockRequest([org_id]),
                service_ticket=None,
                user_ip=None,
                org_id=org_id,
            )
            user = yauser.get_user()

        self.assertTrue(yauser.is_anonymous)
        self.assertTrue(user.is_anonymous)
        self.assertIsNone(yauser.service_ticket.src)
