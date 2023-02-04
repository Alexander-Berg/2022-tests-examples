import json
import unittest

from faker import Faker
from mock import Mock

from saas.library.python.api_mixins import JsonAPI, JsonAPIException
from saas.library.python.api_mixins.tests.fake import Provider


fake = Faker()
fake.add_provider(Provider)


class TestJsonAPI(unittest.TestCase):
    def test_success_request(self):
        fake_response_data = {'status': 'ok'}
        fake_response = fake.get_response(json.dumps(fake_response_data).encode(), 200)

        api = JsonAPI(base_url='test')
        api._session = fake.get_session(fake_response)

        response_data = api._make_request('get', 'test')

        self.assertEqual(response_data, fake_response_data)

    def test_error_200_request(self):
        fake_response_data = {'status': 'error'}
        fake_response = fake.get_response(json.dumps(fake_response_data).encode(), 200)

        api = JsonAPI(base_url='test')
        api._session = fake.get_session(fake_response)
        api._is_error_response = Mock(return_value=True)

        with self.assertRaises(JsonAPIException) as context_manager:
            api._make_request('get', 'test')

        self.assertEqual(context_manager.exception.error, JsonAPIException.DEFAULT_ERROR)
        self.assertEqual(context_manager.exception.status_code, fake_response.status_code)

    def test_error_400_request(self):
        fake_response_data = {'status': 'error'}
        fake_response = fake.get_response(json.dumps(fake_response_data).encode(), 400)

        api = JsonAPI(base_url='test')
        api._session = fake.get_session(fake_response)

        with self.assertRaises(JsonAPIException) as context_manager:
            api._make_request('get', 'test')

        self.assertEqual(context_manager.exception.error, JsonAPIException.DEFAULT_ERROR)
        self.assertEqual(context_manager.exception.status_code, fake_response.status_code)

    def test_error_400_detailed_request(self):
        fake_response_data = {'status': 'error', 'error': 'test'}
        fake_response = fake.get_response(json.dumps(fake_response_data).encode(), 400)

        api = JsonAPI(base_url='test')
        api._session = fake.get_session(fake_response)
        api._get_error_from_response_data = Mock(return_value=fake_response_data['error'])

        with self.assertRaises(JsonAPIException) as context_manager:
            api._make_request('get', 'test')

        self.assertEqual(context_manager.exception.error, fake_response_data['error'])
        self.assertEqual(context_manager.exception.status_code, fake_response.status_code)
