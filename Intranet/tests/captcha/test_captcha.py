# -*- coding: utf-8 -*-
from unittest.mock import patch, Mock

from django.test import TestCase

from events.captcha.captcha import Captcha


class TestCaptcha(TestCase):
    def setUp(self):
        self.captcha = Captcha()

    def test_generate_captcha__with_http_url_in_response(self):
        xml = '<?xml version=\'1.0\'?><number url="http://captcha_url">captcha_key</number>'
        response = Mock()
        response.content = xml
        response.status_code = 200
        with patch('events.captcha.captcha.Captcha._send_request_to_captcha_server', Mock(return_value=response)):
            expected_result = ('https://captcha_url', 'captcha_key')
            self.assertEqual(self.captcha.generate(), expected_result)

    def test_generate_captcha__with_https_url_in_response(self):
        xml = '<?xml version=\'1.0\'?><number url="https://captcha_url">captcha_key</number>'
        response = Mock()
        response.content = xml
        response.status_code = 200
        with patch('events.captcha.captcha.Captcha._send_request_to_captcha_server', Mock(return_value=response)):
            expected_result = ('https://captcha_url', 'captcha_key')
            self.assertEqual(self.captcha.generate(), expected_result)

    def test_generate_captcha_with_non_valid_xml_response(self):
        xml = 'not_valid_xml'
        response = Mock()
        response.content = xml
        response.status_code = 200
        with patch('events.captcha.captcha.Captcha._send_request_to_captcha_server', Mock(return_value=response)):
            expected_result = (None, None)
            self.assertEqual(self.captcha.generate(), expected_result)

    def test_check_results(self):
        xml = '<?xml version=\'1.0\'?><image_check>ok</image_check>'
        response = Mock()
        response.content = xml
        response.status_code = 200
        with patch('events.captcha.captcha.Captcha._send_request_to_captcha_server', Mock(return_value=response)) as patched:
            expected_result = (True, None)
            self.assertEqual(self.captcha.check('value', 'key'), expected_result)
            self.assertEqual(
                patched.call_args,
                (('check', {'rep': 'value', 'type': 'lite', 'key': 'key'}),)
            )

    def test_check_results_with_another_captcha_type(self):
        xml = '<?xml version=\'1.0\'?><image_check>ok</image_check>'
        response = Mock()
        response.content = xml
        response.status_code = 200
        with patch('events.captcha.captcha.Captcha._send_request_to_captcha_server', Mock(return_value=response)) as patched:
            expected_result = (True, None)
            self.assertEqual(self.captcha.check('value', 'key', type='std'), expected_result)
            self.assertEqual(
                patched.call_args,
                (('check', {'rep': 'value', 'key': 'key', 'type': 'std'}),)
            )

    def test_check_results_with_failed_captcha_verification(self):
        xml = '<?xml version=\'1.0\'?><image_check>failed</image_check>'
        response = Mock()
        response.content = xml
        response.status_code = 200
        with patch('events.captcha.captcha.Captcha._send_request_to_captcha_server', Mock(return_value=response)) as patched:
            expected_result = (False, 400)
            self.assertEqual(self.captcha.check('value', 'key', type='std'), expected_result)
            self.assertEqual(
                patched.call_args,
                (('check', {'rep': 'value', 'key': 'key', 'type': 'std'}),)
            )

        xml = '<?xml version=\'1.0\'?><image_check error="not found">failed</image_check>'
        response.content = xml
        with patch('events.captcha.captcha.Captcha._send_request_to_captcha_server', Mock(return_value=response)) as patched:
            expected_result = (False, 400)
            self.assertEqual(self.captcha.check('value', 'key', type='std'), expected_result)
            self.assertEqual(
                patched.call_args,
                (('check', {'rep': 'value', 'key': 'key', 'type': 'std'}),)
            )

    def test_check_captcha_with_non_valid_xml_response(self):
        xml = 'not_valid_xml'
        response = Mock()
        response.content = xml
        response.status_code = 200
        with patch('events.captcha.captcha.Captcha._send_request_to_captcha_server', Mock(return_value=response)):
            expected_result = (False, 400)
            self.assertEqual(self.captcha.check('value', 'key', type='std'), expected_result)

    def test_check_captcha_with_non_200_response_code(self):
        xml = '<?xml version=\'1.0\'?><image_check error="not found">failed</image_check>'
        response = Mock()
        response.content = xml
        response.status_code = 500
        with patch('events.captcha.captcha.Captcha._send_request_to_captcha_server', Mock(return_value=response)):
            expected_result = (False, 500)
            self.assertEqual(self.captcha.check('value', 'key', type='std'), expected_result)

    def test_send_request_to_captcha_server_with_russian_letters(self):
        xml = '<?xml version=\'1.0\'?><image_check>ok</image_check>'
        response = Mock()
        response.content = xml
        response.status_code = 200
        with patch('events.common_app.utils.requests_session.get', Mock(return_value=response)):
            self.assertEqual(self.captcha.check('буквы', 'key', type='std'), (True, None))
