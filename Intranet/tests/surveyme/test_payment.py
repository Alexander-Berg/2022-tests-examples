# -*- coding: utf-8 -*-
import urllib.parse
from django.conf import settings
from django.test import TestCase
from events.surveyme.factories import (
    SurveyFactory,
    SurveyQuestionFactory,
)
from events.surveyme.models import AnswerType
from events.accounts.helpers import YandexClient


class TestPaymentField(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.survey = SurveyFactory(is_published_external=True)
        self.text_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            label='Короткий текст',
            translations={
                'label': {
                    'ru': 'Короткий текст',
                    'en': 'Short text',
                },
            },
        )
        self.payment_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_payment'),
            label='Оплата',
            translations={
                'label': {
                    'ru': 'Оплата',
                    'en': 'Payment',
                },
            },
            param_payment={
                'account_id': '12345',
                'is_fixed': False,
            },
            param_is_required=False,
            initial='170',
        )

    def test_should_return_form_with_payment_field(self):
        response = self.client.get('/v1/surveys/%s/form/' % self.survey.pk)
        self.assertEqual(response.status_code, 200)
        fields = response.data['fields']

        self.assertIn(self.payment_question.param_slug, fields)
        payment_field = fields[self.payment_question.param_slug]
        self.assertEqual(payment_field['widget'], 'PaymentWidget')

        self.assertIn('other_data', payment_field)
        other_data = payment_field['other_data']

        self.assertEqual(other_data['min'], settings.YOOMONEY_MIN_AMOUNT)
        self.assertEqual(other_data['max'], settings.YOOMONEY_MAX_AMOUNT)
        self.assertEqual(other_data['is_fixed'], False)

        self.assertIn('tags', payment_field)
        tags = payment_field['tags']

        self.assertEqual(len(tags), 1)
        self.assertIn('attrs', tags[0])
        attrs = tags[0]['attrs']

        self.assertIn('value', attrs)
        self.assertEqual(attrs['value'], '170')

    def test_should_return_form_with_payment_field_not_created(self):
        self.client.login_yandex(uid='11591999')
        response = self.client.get('/v1/surveys/%s/form/' % self.survey.pk)
        self.assertEqual(response.status_code, 200)
        fields = response.data['fields']

        self.assertIn(self.payment_question.param_slug, fields)
        payment_field = fields[self.payment_question.param_slug]
        self.assertEqual(payment_field['widget'], 'PaymentWidget')

        self.assertIn('other_data', payment_field)
        other_data = payment_field['other_data']

        self.assertEqual(other_data['min'], settings.YOOMONEY_MIN_AMOUNT)
        self.assertEqual(other_data['max'], settings.YOOMONEY_MAX_AMOUNT)
        self.assertEqual(other_data['is_fixed'], False)

        self.assertIn('tags', payment_field)
        tags = payment_field['tags']

        self.assertEqual(len(tags), 1)
        self.assertIn('attrs', tags[0])
        attrs = tags[0]['attrs']

        self.assertIn('value', attrs)
        self.assertEqual(attrs['value'], '170')

    def test_should_set_payment_url(self):
        data = {
            self.text_question.param_slug: 'testme',
            self.payment_question.param_slug: '340 AC',
        }
        response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk, data=data)

        self.assertEqual(response.status_code, 200)
        answer_id = response.data['answer_id']

        self.assertIn('payment_url', response.data)
        payment_url = response.data['payment_url']
        self.assertIsNotNone(payment_url)

        parsed_url = urllib.parse.urlparse(payment_url)
        self.assertEqual(parsed_url.netloc, 'yoomoney.ru')
        self.assertEqual(parsed_url.path, '/quickpay/confirm.xml')

        parsed_query = urllib.parse.parse_qs(parsed_url.query)
        self.assertEqual(parsed_query['label'], [str(answer_id)])
        self.assertEqual(parsed_query['paymentType'], ['AC'])
        self.assertEqual(parsed_query['quickpay-form'], ['shop'])
        self.assertEqual(parsed_query['sum'], ['340'])
        self.assertEqual(parsed_query['receiver'], ['12345'])

    def test_empty_account_id_should_set_payment_url(self):
        self.payment_question.param_payment = {
            'account_id': None,
            'is_fixed': False,
        }
        self.payment_question.save()
        data = {
            self.text_question.param_slug: 'testme',
            self.payment_question.param_slug: '340 AC',
        }
        response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk, data=data)

        self.assertEqual(response.status_code, 200)
        answer_id = response.data['answer_id']

        self.assertIn('payment_url', response.data)
        payment_url = response.data['payment_url']
        self.assertIsNotNone(payment_url)

        parsed_url = urllib.parse.urlparse(payment_url)
        self.assertEqual(parsed_url.netloc, 'yoomoney.ru')
        self.assertEqual(parsed_url.path, '/quickpay/confirm.xml')

        parsed_query = urllib.parse.parse_qs(parsed_url.query, keep_blank_values=True)
        self.assertEqual(parsed_query['label'], [str(answer_id)])
        self.assertEqual(parsed_query['paymentType'], ['AC'])
        self.assertEqual(parsed_query['quickpay-form'], ['shop'])
        self.assertEqual(parsed_query['sum'], ['340'])
        self.assertEqual(parsed_query['receiver'], [''])

    def test_without_amount_shouldnt_set_payment_url(self):
        data = {
            self.text_question.param_slug: 'testme',
        }
        response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk, data=data)
        self.assertEqual(response.status_code, 200)

        self.assertIn('payment_url', response.data)
        payment_url = response.data['payment_url']
        self.assertIsNone(payment_url)

    def test_zero_amount_shouldnt_set_payment_url(self):
        data = {
            self.text_question.param_slug: 'testme',
            self.payment_question.param_slug: '0 AC',
        }
        response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk, data=data)
        self.assertEqual(response.status_code, 200)

        self.assertIn('payment_url', response.data)
        payment_url = response.data['payment_url']
        self.assertIsNone(payment_url)

    def test_should_return_error_value_too_small(self):
        data = {
            self.text_question.param_slug: 'testme',
            self.payment_question.param_slug: '1 AC',
        }
        response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk, data=data)
        self.assertEqual(response.status_code, 400)

    def test_should_return_error_value_too_large(self):
        data = {
            self.text_question.param_slug: 'testme',
            self.payment_question.param_slug: '20000 AC',
        }
        response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk, data=data)
        self.assertEqual(response.status_code, 400)

    def test_should_return_error_incorrect_format(self):
        data = {
            self.text_question.param_slug: 'testme',
            self.payment_question.param_slug: 'invalid',
        }
        response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk, data=data)
        self.assertEqual(response.status_code, 400)
