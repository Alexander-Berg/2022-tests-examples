# -*- coding: utf-8 -*-
import responses

from django.test import TestCase
from unittest.mock import patch
from yc_as_client.exceptions import UnauthenticatedException

from events.accounts.factories import UserFactory
from events.accounts.managers import UserManager
from events.common_app.helpers import MockRequest
from events.yauth_contrib.helpers import IamTokenTestCase
from events.yauth_contrib.mechanisms.iam_token import AccessClient, Mechanism


class TestIamTokenAuth(IamTokenTestCase):
    fixtures = ['initial_data.json']

    def test_should_return_user_info(self):
        self.client.set_token('abcd:123')

        user = UserFactory(uid=None, cloud_uid='abcd:123')

        with patch.object(UserManager, 'get_or_create_user') as mock_create_user:
            mock_create_user.return_value = user
            response = self.client.get('/admin/api/v2/users/me/')

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['cloud_uid'], 'abcd:123')
        mock_create_user.assert_called_once_with(None, 'abcd:123', '123')

    def test_shouldnt_return_user_info(self):
        with patch.object(UserManager, 'get_or_create_user') as mock_create_user:
            response = self.client.get('/admin/api/v2/users/me/')

        self.assertEqual(response.status_code, 401)
        mock_create_user.assert_not_called()


class MockOriginalException(Exception):
    def details(self):
        return str(self)


class TestMechanism(TestCase):
    def test__get_iam_token(self):
        m = Mechanism()

        request = MockRequest(META='HTTP_AUTHORIZATION=Bearer test-iam-token')
        self.assertEqual(m._get_iam_token(request), 'test-iam-token')

        request = MockRequest()
        self.assertIsNone(m._get_iam_token(request))

    def test_extract_params(self):
        m = Mechanism()

        request = MockRequest(META='HTTP_AUTHORIZATION=Bearer test-iam-token')
        self.assertDictEqual(m.extract_params(request), {
            'request': request,
            'iam_token': 'test-iam-token',
        })

        request = MockRequest()
        self.assertIsNone(m.extract_params(request))

    @responses.activate
    def test_should_return_yauser(self):
        responses.add(
            responses.GET,
            'https://api-integration-qa.directory.ws.yandex.net/v6/organizations/',
            json={
                'result': [{'id': 123}, {'id': 234}],
                'links': {},
            },
            status=200,
        )
        m = Mechanism()

        iam_token = 'test-iam-token'
        request = MockRequest(META=f'HTTP_AUTHORIZATION=Bearer {iam_token}')
        request.has_orgs = False
        with patch.object(AccessClient, 'get_cloud_uid') as mock_cloud_uid:
            mock_cloud_uid.return_value = 'abcd'
            yauser = m.apply(request, iam_token)

        self.assertTrue(isinstance(yauser, m.YandexUser))
        self.assertEqual(yauser.cloud_uid, 'abcd')
        self.assertListEqual(yauser.orgs, ['123', '234'])
        self.assertListEqual(request.orgs, ['123', '234'])
        mock_cloud_uid.assert_called_once_with(iam_token)
        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(responses.calls[0].request.headers['X-Cloud-UID'], 'abcd')

    @responses.activate
    def test_dont_check_organizations_if_has_orgs(self):
        responses.add(
            responses.GET,
            'https://api-integration-qa.directory.ws.yandex.net/v6/organizations/',
            json={
                'result': [{'id': 123}, {'id': 234}],
                'links': {},
            },
            status=200,
        )
        m = Mechanism()

        iam_token = 'test-iam-token'
        request = MockRequest(META=f'HTTP_AUTHORIZATION=Bearer {iam_token}')
        request.has_orgs = True
        request.orgs = ['12', '35']
        with patch.object(AccessClient, 'get_cloud_uid') as mock_cloud_uid:
            mock_cloud_uid.return_value = 'abcd'
            yauser = m.apply(request, iam_token)

        self.assertTrue(isinstance(yauser, m.YandexUser))
        self.assertEqual(yauser.cloud_uid, 'abcd')
        self.assertListEqual(yauser.orgs, ['12', '35'])
        self.assertListEqual(request.orgs, ['12', '35'])
        mock_cloud_uid.assert_called_once_with(iam_token)
        self.assertEqual(len(responses.calls), 0)

    @responses.activate
    def test_should_return_annonymous(self):
        m = Mechanism()

        iam_token = 'test-iam-token'
        request = MockRequest(META=f'HTTP_AUTHORIZATION=Bearer {iam_token}')
        with patch.object(AccessClient, 'get_cloud_uid') as mock_cloud_uid:
            mock_cloud_uid.side_effect = UnauthenticatedException(MockOriginalException())
            yauser = m.apply(request, iam_token)

        self.assertTrue(isinstance(yauser, m.AnonymousYandexUser))
        self.assertEqual(len(responses.calls), 0)
