# -*- coding: utf-8 -*-
from django.test import TestCase

from unittest.mock import patch, Mock

from events.surveyme.factories import SurveyFactory
from events.balance.models import Ticket


@patch('events.balance.balance_client.BalanceClient.create_service_product', Mock())
class Test__create_default_trust_texts_for_survey(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.exp_values = {
            'successful_payment': {'value': {'ru': 'Оплата завершена. Спасибо.', 'en': ''}},
            'payment_page_title': {'value': {'ru': 'Покупка билета', 'en': ''}},
        }

    def test__create_default_texts_for_simple_form_after_creating_ticket(self):
        mocked_client = Mock()
        survey = SurveyFactory()
        self.assertEqual(survey.texts.filter(slug__in=self.exp_values.keys()).count(), 0)
        with patch('events.balance.balance_client.BalanceClient', mocked_client):
            Ticket.objects.create(survey=survey, price=1000, name='ticket')
        texts = survey.texts.filter(slug__in=self.exp_values.keys())
        self.assertEqual(texts.count(), len(self.exp_values))
        for text in texts:
            self.assertEqual(text.value, self.exp_values[text.slug]['value']['ru'])
            self.assertEqual(text.translations['value']['ru'], self.exp_values[text.slug]['value']['ru'])
            self.assertEqual(text.translations['value']['en'], self.exp_values[text.slug]['value']['en'])
