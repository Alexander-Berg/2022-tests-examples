# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import os
import six
import unittest
import requests_mock
import yatest.common

from saas.library.python.racktables import ProjectNetworksApi
from saas.library.python.racktables.errors import UnknownMacro, ProjectNetworksApiError
from saas.library.python.racktables.network_macro import NetworkMacro


requests_mock.mock.case_sensitive = True


class TestProjectNetworks(unittest.TestCase):
    TEST_MACRO_NAME = '_SAAS_TEST_NETS_'

    def setUp(self):
        self.project_networks_api = ProjectNetworksApi()
        self.required_headers = {'Authorization': 'OAuth {}'.format(os.getenv('RACKTABLES_OAUTH_TOKEN'))}

    @requests_mock.Mocker()
    def test_list_macros(self, requests_mocker):
        list_macros_response = open(yatest.common.source_path('saas/library/python/racktables/tests/data/list_macros.json'), 'rb')
        requests_mocker.get(ProjectNetworksApi.BASE_URL, body=list_macros_response, request_headers=self.required_headers)

        result = self.project_networks_api.list_macros()
        list_macros_response.close()

        self.assertEqual(len(result), 22851)
        self.assertIsInstance(result[0], NetworkMacro)

        self.assertEqual(requests_mocker.call_count, 1)
        self.assertListEqual(requests_mocker.last_request.qs['op'], ['list_macros', ])

    @requests_mock.Mocker()
    def test_show_macro(self, requests_mocker):
        test_macro_name = '_SAAS_BASE_NETS_'
        show_macro_response = open(yatest.common.source_path('saas/library/python/racktables/tests/data/show_macro.json'), 'rb')
        requests_mocker.get(ProjectNetworksApi.BASE_URL, body=show_macro_response, request_headers=self.required_headers)

        result = self.project_networks_api.show_macro(test_macro_name)
        show_macro_response.close()

        self.assertListEqual(result.networks, [])
        self.assertEqual(result.parent, '_SAAS_BASE_NETS_')
        self.assertEqual(result.description, 'test network')
        self.assertEqual(result.secured, 0)
        self.assertEqual(result.internet, 0)
        self.assertEqual(result.can_create_network, 1)

        self.assertEqual(requests_mocker.call_count, 1)
        self.assertListEqual(requests_mocker.last_request.qs['op'], ['show_macro', ])
        self.assertListEqual(requests_mocker.last_request.qs['name'], [test_macro_name, ])

    @requests_mock.Mocker()
    def test_show_macro_raises_correct_exceptions(self, requests_mocker):
        show_macro_response = open(yatest.common.source_path('saas/library/python/racktables/tests/data/show_macro_not_exists.txt'), 'rb')
        requests_mocker.get(ProjectNetworksApi.BASE_URL, body=show_macro_response, request_headers=self.required_headers, status_code=500)

        with self.assertRaises(UnknownMacro):
            self.project_networks_api.show_macro(self.TEST_MACRO_NAME)

        requests_mocker.get(ProjectNetworksApi.BASE_URL, request_headers=self.required_headers, status_code=500)

        with self.assertRaises(ProjectNetworksApiError):
            self.project_networks_api.show_macro(self.TEST_MACRO_NAME)

    @requests_mock.Mocker()
    def test_create_macro(self, requests_mocker):
        test_macro = {
            'name': '_SAAS_TEST_NETS_',
            'owners': ['svc_saas_administration', 'salmin', 'coffeeman', 'i024', 'saku'],
            'owner_service': 'svc_saas_',
            'parent': '_SAAS_BASE_NETS_',
            'description': 'test network'
        }
        create_macro_response = open(yatest.common.source_path('saas/library/python/racktables/tests/data/create_macro.json'), 'rb')
        requests_mocker.post(ProjectNetworksApi.BASE_URL, body=create_macro_response, request_headers=self.required_headers)

        result = self.project_networks_api.create_macro(**test_macro)
        create_macro_response.close()

        self.assertEqual(result.name, test_macro['name'])
        self.assertListEqual(result.owners, sorted(test_macro['owners']))
        self.assertEqual(result.parent, test_macro['parent'])
        self.assertEqual(result.name, test_macro['name'])

        self.assertEqual(requests_mocker.call_count, 1)

        qs = {k: [v, ] for k, v in six.iteritems(test_macro)}
        qs['owners'] = ['svc_saas_administration,salmin,coffeeman,i024,saku', ]
        qs['op'] = ['create_macro', ]
        self.assertDictEqual(requests_mocker.last_request.qs, qs)

    @requests_mock.Mocker()
    def test_edit_macro_description(self, requests_mocker):
        test_macro_name = '_SAAS_TEST_NETS_'

        edit_macro_response = open(yatest.common.source_path('saas/library/python/racktables/tests/data/edit_macro.json'), 'rb')
        requests_mocker.post(ProjectNetworksApi.BASE_URL, body=edit_macro_response, request_headers=self.required_headers)

        test_macro_description = 'test network description'
        result = self.project_networks_api.edit_macro(test_macro_name, description=test_macro_description)

        self.assertEqual(result.description, test_macro_description)
        self.assertDictEqual(requests_mocker.last_request.qs, {'op': ['edit_macro', ], 'name': [test_macro_name, ], 'description': [test_macro_description, ]})

    @requests_mock.Mocker()
    def test_edit_macro_owners(self, requests_mocker):
        test_macro_name = '_SAAS_TEST_NETS_'

        edit_macro_response = open(yatest.common.source_path('saas/library/python/racktables/tests/data/edit_macro.json'), 'rb')
        requests_mocker.post(ProjectNetworksApi.BASE_URL, body=edit_macro_response, request_headers=self.required_headers)

        test_macro_owners = 'svc_saas_administration,salmin,coffeeman,i024,saku'
        result = self.project_networks_api.edit_macro(test_macro_name, owners=test_macro_owners)

        self.assertEqual(result.owners, sorted(test_macro_owners.split(',')))
        self.assertDictEqual(requests_mocker.last_request.qs, {'op': ['edit_macro', ], 'name': [test_macro_name, ], 'owners': [test_macro_owners, ]})

    @requests_mock.Mocker()
    def test_edit_macro_parent(self, requests_mocker):
        test_macro_name = '_SAAS_TEST_NETS_'

        edit_macro_response = open(yatest.common.source_path('saas/library/python/racktables/tests/data/edit_macro.json'), 'rb')
        requests_mocker.post(ProjectNetworksApi.BASE_URL, body=edit_macro_response, request_headers=self.required_headers)

        test_macro_parent = '_SAAS_BASE_NETS_'
        result = self.project_networks_api.edit_macro(test_macro_name, parent=test_macro_parent)

        self.assertEqual(result.parent, test_macro_parent)
        self.assertDictEqual(requests_mocker.last_request.qs, {'op': ['edit_macro', ], 'name': [test_macro_name, ], 'parent': [test_macro_parent, ]})

    @requests_mock.Mocker()
    def test_edit_macro_all_at_once(self, requests_mocker):
        test_macro_name = '_SAAS_TEST_NETS_'
        test_macro_description = 'test network description'
        test_macro_owners = 'svc_saas_administration,salmin,coffeeman,i024,saku'
        test_macro_owner_service = 'svc_refresh'
        test_macro_parent = '_SAAS_BASE_NETS_'

        edit_macro_response = open(yatest.common.source_path('saas/library/python/racktables/tests/data/edit_macro.json'), 'rb')
        requests_mocker.post(ProjectNetworksApi.BASE_URL, body=edit_macro_response, request_headers=self.required_headers)

        self.project_networks_api.edit_macro(test_macro_name, test_macro_owner_service, test_macro_owners, test_macro_parent, test_macro_description)

        self.assertDictEqual(
            requests_mocker.last_request.qs,
            {'op': ['edit_macro', ], 'name': [test_macro_name, ], 'owner_service': [test_macro_owner_service, ], 'owners': [test_macro_owners, ],
             'parent': [test_macro_parent, ], 'description': [test_macro_description, ]}
        )

        with self.assertRaises(ValueError):
            self.project_networks_api.edit_macro(test_macro_name)

        self.assertEqual(requests_mocker.call_count, 1)

    @requests_mock.Mocker()
    def test_delete_macro(self, requests_mocker):
        test_macro_name = '_SAAS_TEST_NETS_'
        delete_macro_response = open(yatest.common.source_path('saas/library/python/racktables/tests/data/delete_macro.json'), 'rb')
        requests_mocker.post(ProjectNetworksApi.BASE_URL, body=delete_macro_response, request_headers=self.required_headers)

        result = self.project_networks_api.delete_macro(test_macro_name)
        delete_macro_response.close()

        self.assertListEqual(result, [])

        self.assertEqual(requests_mocker.call_count, 1)
        self.assertDictEqual(requests_mocker.last_request.qs, {'op': ['delete_macro', ], 'name': [test_macro_name, ]})

    @requests_mock.Mocker()
    def test_create_network(self, requests_mocker):
        test_macro_name = '_SAAS_TEST_NETS_'
        create_network_response = open(yatest.common.source_path('saas/library/python/racktables/tests/data/create_network.json'), 'rb')
        requests_mocker.post(ProjectNetworksApi.BASE_URL, body=create_network_response, request_headers=self.required_headers)

        result = self.project_networks_api.create_network(test_macro_name)
        create_network_response.close()

        self.assertListEqual(result.networks, [{'id': '4e65', 'description': ''}])

        self.assertEqual(requests_mocker.call_count, 1)
        self.assertDictEqual(requests_mocker.last_request.qs, {'op': ['create_network', ], 'macro_name': [test_macro_name, ]})

    @requests_mock.Mocker()
    def test_delete_network(self, requests_mocker):
        test_macro_name = '_SAAS_TEST_NETS_'
        test_project_id = '4e65'

        delete_network_response = open(yatest.common.source_path('saas/library/python/racktables/tests/data/delete_network.json'), 'rb')
        requests_mocker.post(ProjectNetworksApi.BASE_URL, body=delete_network_response, request_headers=self.required_headers)

        result = self.project_networks_api.delete_network(test_project_id, test_macro_name)
        delete_network_response.close()

        self.assertListEqual(result.networks, [])

        self.assertEqual(requests_mocker.call_count, 1)
        self.assertDictEqual(requests_mocker.last_request.qs, {'op': ['delete_network', ], 'macro_name': [test_macro_name, ], 'project_id': [test_project_id, ]})

    @requests_mock.Mocker()
    def test_move_network(self, requests_mocker):
        test_macro_name = '_SAAS_TEST_NET_'
        test_project_id = '4e66'

        move_network_response = open(yatest.common.source_path('saas/library/python/racktables/tests/data/move_network.json'), 'rb')
        requests_mocker.post(ProjectNetworksApi.BASE_URL, body=move_network_response, request_headers=self.required_headers)

        result = self.project_networks_api.move_network(test_project_id, test_macro_name)
        move_network_response.close()

        self.assertListEqual(result.networks, [{"id": "4e66", "description": ""}, ])

        self.assertEqual(requests_mocker.call_count, 1)
        self.assertDictEqual(requests_mocker.last_request.qs, {'op': ['move_network', ], 'macro_name': [test_macro_name, ], 'project_id': [test_project_id, ]})
