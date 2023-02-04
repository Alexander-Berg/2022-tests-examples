# -*- coding: utf-8 -*-
import responses

from django.test import TestCase
from unittest.mock import patch

from events.surveyme.factories import ProfileSurveyAnswerFactory
from events.surveyme_integration.variables import SessionRefererVariable


class TestSessionRefererVariable(TestCase):
    should_get_first_element_from_result = False
    variable_class = SessionRefererVariable
    json_rpc_method = 'referer'
    success_value = "https://some.another_referer.com/"

    def setUp(self):
        self.answer = ProfileSurveyAnswerFactory()
        self.var = self.variable_class(answer=self.answer)
        self.answer.source_request = {'feedback_sid': '1120000000009214'}

    @responses.activate
    def test_should_return_none_if_result_is_blank(self):
        responses.add(
            responses.GET,
            'https://support-api.locdoc-test.yandex.net/api/v1/visits',
            json=[],
        )
        self.assertEqual(self.var.get_value(), None)

    @responses.activate
    def test_should_return_result_if_result(self):
        responses.add(
            responses.GET,
            'https://support-api.locdoc-test.yandex.net/api/v1/visits',
            json=[{
                'yandexuid': '1120000000009214',
                'referer': 'https://some.another_referer.com/',
                'created_at': '2018-01-09T09:49:42.541812Z',
            }],
        )
        self.assertEqual(self.var.get_value(), self.success_value)

    def test_should_send_right_session_id_and_method_name(self):
        with patch('events.surveyme_integration.variables.session.SupportAPIClient.get') as mock_instance:
            self.var.get_value()  # BANG!
            mock_instance.assert_called_once_with(
                what=self.json_rpc_method,
                params={'yandexuid': '1120000000009214'}
            )
