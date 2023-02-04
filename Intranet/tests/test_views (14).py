import unittest

from .utils import get_entity_data, get_meta
from django.test.client import Client
from django.core.urlresolvers import reverse
from mock import patch


class DocTestCase(unittest.TestCase):
    def setUp(self):
        self.client = Client()
        self.auth_patcher = patch(
            'staff_api.middleware.AuthMiddleware.process_request',
            return_value=None,
        )
        self.auth_patcher.start()

    def tearDown(self):
        self.auth_patcher.stop()

    def test_index(self):
        response = self.client.get(reverse('static_api:index'))

        self.assertEqual(response.status_code, 200)

    def test_person(self):
        self._test_entity('persons')

    def test_group(self):
        self._test_entity('groups')

    def test_groupmembership(self):
        self._test_entity('groupmembership')

    def test_organization(self):
        self._test_entity('organizations')

    def test_office(self):
        self._test_entity('offices')

    def test_room(self):
        self._test_entity('rooms')

    def test_table(self):
        self._test_entity('tables')

    def test_departmentstaff(self):
        self._test_entity('departmentstaff')

    def _test_entity(self, resource_plural):
        response = self.client.get(
            reverse(
                'static_api:resource',
                args=[resource_plural],
            ),
            {'_doc': 1}
        )

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response['Content-Type'], 'text/html; charset=utf-8')


class AdminTestCase(unittest.TestCase):
    def setUp(self):
        self.client = Client()

    @patch('static_api.helpers.get_meta', new=get_meta)
    @patch('static_api.helpers.get_entity_data', new=get_entity_data)
    def test_index(self):
        response = self.client.get(reverse('static_api:admin-index'))

        self.assertEqual(response.status_code, 200)
