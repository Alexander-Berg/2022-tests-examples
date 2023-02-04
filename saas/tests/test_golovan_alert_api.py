import json
import unittest

from typing import Callable, List

from faker import Faker

from saas.library.python.api_mixins import JsonAPIException
from saas.library.python.saas_alerts.golovan import golovan_alert_api, AlertTemplate
from saas.library.python.saas_alerts.tests.fake import Provider


fake = Faker()
fake.add_provider(Provider)


class TestGolovanAlertAPI(unittest.TestCase):
    def test_success_request(self):
        fake_response_data = {'status': 'ok'}
        fake_response = fake.get_response(json.dumps(fake_response_data).encode(), 200)
        golovan_alert_api._session = fake.get_session(fake_response)

        response_data = golovan_alert_api._make_request('get', 'test')

        self.assertEqual(response_data, fake_response_data)

    def test_error_200_request(self):
        fake_response_data = {'status': 'error', 'error_code': 'test'}
        fake_response = fake.get_response(json.dumps(fake_response_data).encode(), 200)
        golovan_alert_api._session = fake.get_session(fake_response)

        with self.assertRaises(JsonAPIException) as context_manager:
            golovan_alert_api._make_request('get', 'test')

        self.assertEqual(context_manager.exception.error, fake_response_data['error_code'])
        self.assertEqual(context_manager.exception.status_code, fake_response.status_code)

    def test_error_400_request(self):
        fake_response_data = {'status': 'ok'}
        fake_response = fake.get_response(json.dumps(fake_response_data).encode(), 400)
        golovan_alert_api._session = fake.get_session(fake_response)

        with self.assertRaises(JsonAPIException) as context_manager:
            golovan_alert_api._make_request('get', 'test')

        self.assertEqual(context_manager.exception.error, JsonAPIException.DEFAULT_ERROR)
        self.assertEqual(context_manager.exception.status_code, fake_response.status_code)

    def test_template_methods(self):
        fake_response_data = {'status': 'ok'}
        fake_response = fake.get_response(json.dumps(fake_response_data).encode(), 200)
        golovan_alert_api._session = fake.get_session(fake_response)

        template = fake.get_alert_template()

        template_methods: List[Callable[[AlertTemplate], dict]] = [
            golovan_alert_api.create_or_update_template,
            golovan_alert_api.render_new_json_template,
            golovan_alert_api.apply_template
        ]
        for method in template_methods:
            response_data = method(template)
            self.assertEqual(fake_response_data, response_data)
