# -*- coding: utf-8 -*-
import responses

from django.test import TestCase

from events.abc.client import AbcClient, ROLE_TVM_MANAGER, ROLE_FORM_MANAGER


class TestAbcClient(TestCase):
    @responses.activate
    def test_get_services_by_tvm_client_success(self):
        responses.add(
            responses.GET,
            'https://abc-back.test.yandex-team.ru/api/v4/resources/consumers/',
            json={
                'results': [
                    {'service': {'id': 123}},
                    {'service': {'id': 234}},
                ],
            },
        )
        client = AbcClient()
        services = list(client.get_services_by_tvm_client('12345'))
        self.assertEqual(services, [123, 234])
        self.assertEqual(responses.calls[0].request.params['resource__external_id'], '12345')

    @responses.activate
    def test_get_services_by_tvm_client_empty_list(self):
        responses.add(
            responses.GET,
            'https://abc-back.test.yandex-team.ru/api/v4/resources/consumers/',
            json={'results': []},
        )
        client = AbcClient()
        services = list(client.get_services_by_tvm_client('12345'))
        self.assertEqual(services, [])
        self.assertEqual(responses.calls[0].request.params['resource__external_id'], '12345')

    @responses.activate
    def test_get_services_by_tvm_client_error(self):
        responses.add(
            responses.GET,
            'https://abc-back.test.yandex-team.ru/api/v4/resources/consumers/',
            status=404,
        )
        client = AbcClient()
        services = list(client.get_services_by_tvm_client('12345'))
        self.assertEqual(services, [])
        self.assertEqual(responses.calls[0].request.params['resource__external_id'], '12345')

    @responses.activate
    def test_has_roles_in_service_by_login_success(self):
        responses.add(
            responses.GET,
            'https://abc-back.test.yandex-team.ru/api/v4/services/members/',
            json={
                'results': [{'id': 123}],
            },
        )
        client = AbcClient()
        has_role = client.has_roles_in_service(12345, [ROLE_TVM_MANAGER], login='myname')
        self.assertTrue(has_role)
        self.assertEqual(responses.calls[0].request.params['role__code__in'], ROLE_TVM_MANAGER)
        self.assertEqual(responses.calls[0].request.params['person__login'], 'myname')
        self.assertTrue('person__uid' not in responses.calls[0].request.params)

    @responses.activate
    def test_has_roles_in_service_by_uid_success(self):
        responses.add(
            responses.GET,
            'https://abc-back.test.yandex-team.ru/api/v4/services/members/',
            json={
                'results': [{'id': 123}],
            },
        )
        client = AbcClient()
        has_role = client.has_roles_in_service(12345, [ROLE_TVM_MANAGER], uid='120001')
        self.assertTrue(has_role)
        self.assertEqual(responses.calls[0].request.params['role__code__in'], ROLE_TVM_MANAGER)
        self.assertEqual(responses.calls[0].request.params['person__uid'], '120001')
        self.assertTrue('person__login' not in responses.calls[0].request.params)

    @responses.activate
    def test_has_roles_in_service_by_login_empty_list(self):
        responses.add(
            responses.GET,
            'https://abc-back.test.yandex-team.ru/api/v4/services/members/',
            json={
                'results': [],
            },
        )
        client = AbcClient()
        has_role = client.has_roles_in_service(12345, [ROLE_TVM_MANAGER], login='myname')
        self.assertFalse(has_role)
        self.assertEqual(responses.calls[0].request.params['role__code__in'], ROLE_TVM_MANAGER)
        self.assertEqual(responses.calls[0].request.params['person__login'], 'myname')
        self.assertTrue('person__uid' not in responses.calls[0].request.params)

    @responses.activate
    def test_has_roles_in_service_by_login_client_error(self):
        responses.add(
            responses.GET,
            'https://abc-back.test.yandex-team.ru/api/v4/services/members/',
            status=404,
        )
        client = AbcClient()
        has_role = client.has_roles_in_service(12345, [ROLE_TVM_MANAGER], login='myname')
        self.assertFalse(has_role)
        self.assertEqual(responses.calls[0].request.params['role__code__in'], ROLE_TVM_MANAGER)
        self.assertEqual(responses.calls[0].request.params['person__login'], 'myname')
        self.assertTrue('person__uid' not in responses.calls[0].request.params)

    def test_has_roles_in_service_fails_without_login_or_uid(self):
        client = AbcClient()
        with self.assertRaises(AssertionError):
            client.has_roles_in_service(12345, [ROLE_TVM_MANAGER])

    @responses.activate
    def test_has_two_roles_in_service_success(self):
        responses.add(
            responses.GET,
            'https://abc-back.test.yandex-team.ru/api/v4/services/members/',
            json={
                'results': [{'id': 123}],
            },
        )
        client = AbcClient()
        has_role = client.has_roles_in_service(12345, [ROLE_TVM_MANAGER, ROLE_FORM_MANAGER], login='myname')
        self.assertTrue(has_role)
        self.assertEqual(responses.calls[0].request.params['role__code__in'], f'{ROLE_TVM_MANAGER},{ROLE_FORM_MANAGER}')
        self.assertEqual(responses.calls[0].request.params['person__login'], 'myname')
        self.assertTrue('person__uid' not in responses.calls[0].request.params)
