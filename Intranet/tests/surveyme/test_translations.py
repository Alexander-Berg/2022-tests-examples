# -*- coding: utf-8 -*-
import json
import responses

from django.conf import settings
from django.test import TestCase

from events.accounts.helpers import YandexClient
from events.surveyme.factories import (
    SurveyFactory,
    SurveyQuestionFactory,
    SurveyQuestionChoiceFactory,
    SurveyQuestionMatrixTitleFactory,
)
from events.surveyme.models import (
    AnswerType,
    Survey,
    SurveyAgreement,
    SurveyQuestion,
    SurveyQuestionChoice,
    SurveyQuestionMatrixTitle,
    SurveyText,
)
from events.surveyme_integration.factories import (
    ServiceSurveyHookSubscriptionFactory,
    SurveyHookFactory,
    SurveyVariableFactory,
)
from events.surveyme_integration.services.email.context_processors import EmailBodyField


class TestSurveyTranslation(TestCase):  # {{{
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        super().setUp()
        self.profile = self.client.login_yandex(is_superuser=True)
        self.default_ru_name = 'some_name'
        self.survey = SurveyFactory(name=self.default_ru_name, user=self.profile)

    def test_change_name_with_russian_correct(self):  # {{{
        self.assertIsNone(self.survey.translations)

        url = '/admin/api/v2/surveys/{survey_id}/'.format(survey_id=self.survey.pk)
        headers = {'HTTP_ACCEPT_LANGUAGE': 'ru'}
        data = {'name': 'test name', }
        response = self.client.patch(url, data, **headers)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['name'], 'test name')

        self.survey.refresh_from_db()

        self.assertEqual(self.survey.name, 'test name')
        self.assertEqual(
            self.survey.translations,
            {'name': {'ru': 'test name'}}
        )
    # }}}

    def test_get_response_on_passed_language(self):  # {{{
        translations = {
            'name': {
                'ru': self.default_ru_name,
                'en': 'smth',
                'fr': 'hi'
            }
        }
        self.survey.translations = translations
        self.survey.save()
        url = '/admin/api/v2/surveys/{survey_id}/'.format(survey_id=self.survey.pk)

        for lang, result in translations['name'].items():
            headers = {'HTTP_ACCEPT_LANGUAGE': lang}

            response = self.client.get(url, **headers)
            self.assertEqual(response.status_code, 200)
            self.assertEqual(response.data['name'], result)
    # }}}

    def test_default_fallback_on_russian_language(self):  # {{{
        translations = {
            'name': {
                'ru': self.default_ru_name,
            }
        }
        self.survey.translations = translations
        self.survey.save()
        url = '/admin/api/v2/surveys/{survey_id}/'.format(survey_id=self.survey.pk)

        headers = {'HTTP_ACCEPT_LANGUAGE': 'en'}

        response = self.client.get(url, **headers)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['name'], self.default_ru_name)
    # }}}

    def test_get_correct_without_lang(self):  # {{{
        translations = {
            'name': {
                'ru': self.default_ru_name,
                'en': 'eng translation'
            }
        }
        self.survey.translations = translations
        self.survey.save()
        url = '/admin/api/v2/surveys/{survey_id}/'.format(survey_id=self.survey.pk)

        response = self.client.get(url)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['name'], self.default_ru_name)
    # }}}

    def test_get_default_without_translation(self):  # {{{
        translations = {
            'name': {
                'ru': self.default_ru_name,
            }
        }
        self.survey.translations = translations
        self.survey.save()
        url = '/admin/api/v2/surveys/{survey_id}/'.format(survey_id=self.survey.pk)

        headers = {'HTTP_ACCEPT_LANGUAGE': 'fi'}

        response = self.client.get(url, **headers)

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['name'], self.default_ru_name)
    # }}}

    def test_fallback_on_correct_language(self):  # {{{
        translations = {
            'name': {
                'ru': self.default_ru_name,
                'en': 'smth'
            }
        }
        self.survey.translations = translations
        self.survey.save()
        url = '/admin/api/v2/surveys/{survey_id}/'.format(survey_id=self.survey.pk)

        headers = {'HTTP_ACCEPT_LANGUAGE': 'de'}

        response = self.client.get(url, **headers)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['name'], 'smth')
    # }}}

    def test_override_existing_default_correct(self):  # {{{
        translations = {
            'name': {
                'ru': self.default_ru_name,
                'en': 'smth',
                'another': 'test',
                'fr': 'hi'
            }
        }
        self.survey.translations = translations
        self.survey.save()

        url = '/admin/api/v2/surveys/{survey_id}/'.format(survey_id=self.survey.pk)
        headers = {'HTTP_ACCEPT_LANGUAGE': 'ru'}
        data = {'name': 'test name', }
        response = self.client.patch(url, data, **headers)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['name'], 'test name')

        self.survey.refresh_from_db()

        self.assertEqual(self.survey.name, 'test name')
        translations['name']['ru'] = 'test name'
        self.assertEqual(
            self.survey.translations,
            translations,
        )
    # }}}

    def test_add_to_translations_on_create_success(self):  # {{{
        url = '/admin/api/v2/surveys/'
        headers = {'HTTP_ACCEPT_LANGUAGE': 'ru'}
        data = {'name': 'test name', }
        response = self.client.post(url, data, **headers)
        self.assertEqual(response.status_code, 201)
        survey = Survey.objects.get(pk=response.data['id'])
        self.assertEqual(survey.name, 'test name')
        self.assertEqual(
            survey.translations,
            {'name': {'ru': 'test name'}}
        )
    # }}}

    def test_add_to_translations_on_create_not_default_lang_success(self):  # {{{
        url = '/admin/api/v2/surveys/'
        headers = {'HTTP_ACCEPT_LANGUAGE': 'en'}
        data = {'name': 'test name', }
        response = self.client.post(url, data, **headers)
        self.assertEqual(response.status_code, 201)
        survey = Survey.objects.get(pk=response.data['id'])
        self.assertEqual(survey.name, 'test name')
        self.assertEqual(
            survey.translations,
            {'name': {'en': 'test name'}}
        )
    # }}}

    def test_doesnt_override_default_language_field(self):  # {{{
        self.assertIsNone(self.survey.translations)

        url = '/admin/api/v2/surveys/{survey_id}/'.format(survey_id=self.survey.pk)
        headers = {'HTTP_ACCEPT_LANGUAGE': 'en'}
        data = {'name': 'test name', }
        response = self.client.patch(url, data, **headers)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['name'], 'test name')
        self.survey.refresh_from_db()
        self.assertEqual(self.survey.name, self.default_ru_name)
        self.assertEqual(
            self.survey.translations,
            {'name': {'en': 'test name'}}
        )
    # }}}

    def test_doesnt_override_existing_default_correct(self):  # {{{
        translations = {
            'name': {
                'ru': self.default_ru_name,
                'en': 'smth',
                'another': 'test',
                'fr': 'hi'
            }
        }
        self.survey.translations = translations
        self.survey.save()

        url = '/admin/api/v2/surveys/{survey_id}/'.format(survey_id=self.survey.pk)
        headers = {'HTTP_ACCEPT_LANGUAGE': 'en'}
        data = {'name': 'test name', }
        response = self.client.patch(url, data, **headers)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['name'], 'test name')

        self.survey.refresh_from_db()

        self.assertEqual(self.survey.name, self.default_ru_name)
        translations['name']['en'] = 'test name'
        self.assertEqual(
            self.survey.translations,
            translations,
        )
    # }}}
# }}}


class TestTranslationsSurvey(TestCase):  # {{{
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex(is_superuser=True)

    def test_create_survey_ru(self):  # {{{
        data = {
            'name': 'name_ru',
        }
        response = self.client.post('/admin/api/v2/surveys/', data=data, HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 201)

        survey_pk = response.data['id']
        survey = Survey.objects.get(pk=survey_pk)
        self.assertEqual(survey.language, 'ru')
        self.assertEqual(survey.name, 'name_ru')
        self.assertEqual(survey.translations['name']['ru'], 'name_ru')
    # }}}

    def test_create_survey_en(self):  # {{{
        data = {
            'name': 'name_en',
        }
        response = self.client.post('/admin/api/v2/surveys/', data=data, HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 201)

        survey_pk = response.data['id']
        survey = Survey.objects.get(pk=survey_pk)
        self.assertEqual(survey.language, 'en')
        self.assertEqual(survey.name, 'name_en')
        self.assertEqual(survey.translations['name']['en'], 'name_en')
    # }}}

    def test_patch_query_survey_ru(self):  # {{{
        survey = SurveyFactory(
            name='name_ru',
            language='ru',
            translations={
                'name': {
                    'ru': 'name_ru',
                    'en': 'name_en',
                },
            },
        )
        data = {
            'name': 'another_name_ru',
        }
        response = self.client.patch('/admin/api/v2/surveys/%s/' % survey.pk, data=data, HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 200)

        survey = Survey.objects.get(pk=survey.pk)
        self.assertEqual(survey.name, 'another_name_ru')
        self.assertEqual(len(survey.translations['name']), 2)
        self.assertEqual(survey.translations['name']['ru'], 'another_name_ru')
        self.assertEqual(survey.translations['name']['en'], 'name_en')
    # }}}

    def test_patch_query_survey_en(self):  # {{{
        survey = SurveyFactory(
            name='name_en',
            language='en',
            translations={
                'name': {
                    'ru': 'name_ru',
                    'en': 'name_en',
                },
            },
        )
        data = {
            'name': 'another_name_en',
        }
        response = self.client.patch('/admin/api/v2/surveys/%s/' % survey.pk, data=data, HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 200)

        survey = Survey.objects.get(pk=survey.pk)
        self.assertEqual(survey.name, 'another_name_en')
        self.assertEqual(len(survey.translations['name']), 2)
        self.assertEqual(survey.translations['name']['ru'], 'name_ru')
        self.assertEqual(survey.translations['name']['en'], 'another_name_en')
    # }}}

    def test_patch_query_survey_fi(self):  # {{{
        survey = SurveyFactory(
            name='name_en',
            language='en',
            translations={
                'name': {
                    'ru': 'name_ru',
                    'en': 'name_en',
                },
            },
        )
        data = {
            'name': 'name_fi',
        }
        response = self.client.patch('/admin/api/v2/surveys/%s/' % survey.pk, data=data, HTTP_ACCEPT_LANGUAGE='fi')
        self.assertEqual(response.status_code, 200)

        survey = Survey.objects.get(pk=survey.pk)
        self.assertEqual(survey.name, 'name_en')
        self.assertEqual(len(survey.translations['name']), 3)
        self.assertEqual(survey.translations['name']['ru'], 'name_ru')
        self.assertEqual(survey.translations['name']['en'], 'name_en')
        self.assertEqual(survey.translations['name']['fi'], 'name_fi')
    # }}}

    def test_admin_query_survey_ru(self):  # {{{
        survey = SurveyFactory(
            name='name_ru',
            language='ru',
            translations={
                'name': {
                    'ru': 'name_ru',
                    'en': 'name_en',
                },
            },
        )

        response = self.client.get('/admin/api/v2/surveys/%s/' % survey.pk, HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['name'], 'name_ru')

        response = self.client.get('/admin/api/v2/surveys/%s/' % survey.pk, HTTP_ACCEPT_LANGUAGE='kk')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['name'], 'name_ru')

        response = self.client.get('/admin/api/v2/surveys/%s/' % survey.pk, HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['name'], 'name_en')

        response = self.client.get('/admin/api/v2/surveys/%s/' % survey.pk, HTTP_ACCEPT_LANGUAGE='fi')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['name'], 'name_en')
    # }}}

    def test_admin_query_survey_en(self):  # {{{
        survey = SurveyFactory(
            name='name_en',
            language='en',
            translations={
                'name': {
                    'ru': 'name_ru',
                    'en': 'name_en',
                },
            },
        )

        response = self.client.get('/admin/api/v2/surveys/%s/' % survey.pk, HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['name'], 'name_ru')

        response = self.client.get('/admin/api/v2/surveys/%s/' % survey.pk, HTTP_ACCEPT_LANGUAGE='kk')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['name'], 'name_ru')

        response = self.client.get('/admin/api/v2/surveys/%s/' % survey.pk, HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['name'], 'name_en')

        response = self.client.get('/admin/api/v2/surveys/%s/' % survey.pk, HTTP_ACCEPT_LANGUAGE='fi')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['name'], 'name_en')
    # }}}

    def test_front_query_survey_ru(self):  # {{{
        survey = SurveyFactory(
            name='name_ru',
            language='ru',
            translations={
                'name': {
                    'ru': 'name_ru',
                    'en': 'name_en',
                },
            },
            is_published_external=True,
        )

        response = self.client.get('/v1/surveys/%s/' % survey.pk, HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['name'], 'name_ru')

        response = self.client.get('/v1/surveys/%s/' % survey.pk, HTTP_ACCEPT_LANGUAGE='kk')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['name'], 'name_ru')

        response = self.client.get('/v1/surveys/%s/' % survey.pk, HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['name'], 'name_en')

        response = self.client.get('/v1/surveys/%s/' % survey.pk, HTTP_ACCEPT_LANGUAGE='fi')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['name'], 'name_en')
    # }}}

    def test_front_query_survey_en(self):  # {{{
        survey = SurveyFactory(
            name='name_en',
            language='en',
            translations={
                'name': {
                    'ru': 'name_ru',
                    'en': 'name_en',
                },
            },
            is_published_external=True,
        )

        response = self.client.get('/v1/surveys/%s/' % survey.pk, HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['name'], 'name_ru')

        response = self.client.get('/v1/surveys/%s/' % survey.pk, HTTP_ACCEPT_LANGUAGE='kk')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['name'], 'name_ru')

        response = self.client.get('/v1/surveys/%s/' % survey.pk, HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['name'], 'name_en')

        response = self.client.get('/v1/surveys/%s/' % survey.pk, HTTP_ACCEPT_LANGUAGE='fi')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['name'], 'name_en')
    # }}}
# }}}


class TestTranslationsSurveyText(TestCase):  # {{{
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex(is_superuser=True)

    def test_create_texts_ru(self):  # {{{
        response = self.client.post('/admin/api/v2/surveys/', HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 201)

        survey_pk = response.data['id']
        text = SurveyText.objects.get(survey=survey_pk, slug='submit_button')
        self.assertEqual(text.value, 'Отправить')
        self.assertEqual(text.translations['value']['ru'], 'Отправить')
    # }}}

    def test_create_texts_en(self):  # {{{
        response = self.client.post('/admin/api/v2/surveys/', HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 201)

        survey_pk = response.data['id']
        text = SurveyText.objects.get(survey=survey_pk, slug='submit_button')
        self.assertEqual(text.value, 'Submit')
        self.assertEqual(text.translations['value']['en'], 'Submit')
    # }}}

    def test_admin_query_texts_ru(self):  # {{{
        response = self.client.post('/admin/api/v2/surveys/', HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 201)

        survey_pk = response.data['id']
        response = self.client.get('/admin/api/v2/survey-texts/?survey=%s' % survey_pk, HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 200)
        texts = {
            text['slug']: text
            for text in response.data['results']
        }
        self.assertEqual(texts['submit_button']['value'], 'Отправить')
    # }}}

    def test_admin_query_texts_en(self):  # {{{
        response = self.client.post('/admin/api/v2/surveys/', HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 201)

        survey_pk = response.data['id']
        response = self.client.get('/admin/api/v2/survey-texts/?survey=%s' % survey_pk, HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 200)
        texts = {
            text['slug']: text
            for text in response.data['results']
        }
        self.assertEqual(texts['submit_button']['value'], 'Submit')
    # }}}

    def test_admin_query_texts_kk(self):  # {{{
        response = self.client.post('/admin/api/v2/surveys/', HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 201)

        survey_pk = response.data['id']
        response = self.client.get('/admin/api/v2/survey-texts/?survey=%s' % survey_pk, HTTP_ACCEPT_LANGUAGE='kk')
        self.assertEqual(response.status_code, 200)
        texts = {
            text['slug']: text
            for text in response.data['results']
        }
        self.assertEqual(texts['submit_button']['value'], 'Отправить')
    # }}}

    def test_admin_query_texts_fi(self):  # {{{
        response = self.client.post('/admin/api/v2/surveys/', HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 201)

        survey_pk = response.data['id']
        response = self.client.get('/admin/api/v2/survey-texts/?survey=%s' % survey_pk, HTTP_ACCEPT_LANGUAGE='fi')
        self.assertEqual(response.status_code, 200)
        texts = {
            text['slug']: text
            for text in response.data['results']
        }
        self.assertEqual(texts['submit_button']['value'], 'Submit')
    # }}}

    def test_front_query_texts_ru(self):  # {{{
        response = self.client.post('/admin/api/v2/surveys/', HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 201)

        survey_pk = response.data['id']
        response = self.client.get('/v1/surveys/%s/' % survey_pk, HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 200)
        texts = response.data['texts']
        self.assertEqual(texts['submit_button'], 'Отправить')
    # }}}

    def test_front_query_texts_kk(self):  # {{{
        response = self.client.post('/admin/api/v2/surveys/', HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 201)

        survey_pk = response.data['id']
        response = self.client.get('/v1/surveys/%s/' % survey_pk, HTTP_ACCEPT_LANGUAGE='kk')
        self.assertEqual(response.status_code, 200)
        texts = response.data['texts']
        self.assertEqual(texts['submit_button'], 'Отправить')
    # }}}

    def test_front_query_texts_en(self):  # {{{
        response = self.client.post('/admin/api/v2/surveys/', HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 201)

        survey_pk = response.data['id']
        response = self.client.get('/v1/surveys/%s/' % survey_pk, HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 200)
        texts = response.data['texts']
        self.assertEqual(texts['submit_button'], 'Submit')
    # }}}

    def test_front_query_texts_fi(self):  # {{{
        response = self.client.post('/admin/api/v2/surveys/', HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 201)

        survey_pk = response.data['id']
        response = self.client.get('/v1/surveys/%s/' % survey_pk, HTTP_ACCEPT_LANGUAGE='fi')
        self.assertEqual(response.status_code, 200)
        texts = response.data['texts']
        self.assertEqual(texts['submit_button'], 'Submit')
    # }}}
# }}}


class TestTranslationsSurveyAgreement(TestCase):  # {{{
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex(is_superuser=True)

    def test_query_agreements_ru(self):  # {{{
        response = self.client.get('/admin/api/v2/survey-agreements/', HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 200)
        agreements = {
            agreement['slug']: agreement['text']
            for agreement in response.data['results']
        }
        self.assertIn('ЯНДЕКС', agreements['events'])
    # }}}

    def test_query_agreements_kk(self):  # {{{
        response = self.client.get('/admin/api/v2/survey-agreements/', HTTP_ACCEPT_LANGUAGE='kk')
        self.assertEqual(response.status_code, 200)
        agreements = {
            agreement['slug']: agreement['text']
            for agreement in response.data['results']
        }
        self.assertIn('ЯНДЕКС', agreements['events'])
    # }}}

    def test_query_agreements_en(self):  # {{{
        response = self.client.get('/admin/api/v2/survey-agreements/', HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 200)
        agreements = {
            agreement['slug']: agreement['text']
            for agreement in response.data['results']
        }
        self.assertIn('Yandex', agreements['events'])
    # }}}

    def test_query_agreements_fi(self):  # {{{
        response = self.client.get('/admin/api/v2/survey-agreements/', HTTP_ACCEPT_LANGUAGE='fi')
        self.assertEqual(response.status_code, 200)
        agreements = {
            agreement['slug']: agreement['text']
            for agreement in response.data['results']
        }
        self.assertIn('Yandex', agreements['events'])
    # }}}

    def test_front_agreements_ru(self):  # {{{
        survey = SurveyFactory(
            is_published_external=True,
        )
        survey.agreements.set([
            SurveyAgreement.objects.get(slug='events'),
        ])
        SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
        )
        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 200)
        self.assertIn('ЯНДЕКС', response.data['fields']['is_agree_with_events']['label'])
    # }}}

    def test_front_agreements_kk(self):  # {{{
        survey = SurveyFactory(
            is_published_external=True,
        )
        survey.agreements.set([
            SurveyAgreement.objects.get(slug='events'),
        ])
        SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
        )
        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='kk')
        self.assertEqual(response.status_code, 200)
        self.assertIn('ЯНДЕКС', response.data['fields']['is_agree_with_events']['label'])
    # }}}

    def test_front_agreements_en(self):  # {{{
        survey = SurveyFactory(
            is_published_external=True,
        )
        survey.agreements.set([
            SurveyAgreement.objects.get(slug='events'),
        ])
        SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
        )
        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 200)
        self.assertIn('Yandex', response.data['fields']['is_agree_with_events']['label'])
    # }}}

    def test_front_agreements_fi(self):  # {{{
        survey = SurveyFactory(
            is_published_external=True,
        )
        survey.agreements.set([
            SurveyAgreement.objects.get(slug='events'),
        ])
        SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
        )
        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='fi')
        self.assertEqual(response.status_code, 200)
        self.assertIn('Yandex', response.data['fields']['is_agree_with_events']['label'])
    # }}}
# }}}


class TestTranslationsSurveyQuestion(TestCase):  # {{{
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex(is_superuser=True)

    def test_create_question_ru(self):  # {{{
        survey = SurveyFactory(language='ru')
        answer_short_text = AnswerType.objects.get(slug='answer_short_text')
        data = {
            'label': 'label_ru',
            'answer_type_id': answer_short_text.pk,
            'survey_id': survey.pk,
        }
        response = self.client.post('/admin/api/v2/survey-questions/', data=data, HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 201)

        question = SurveyQuestion.objects.get(pk=response.data['id'])
        self.assertEqual(question.label, 'label_ru')
        self.assertEqual(question.translations['label']['ru'], 'label_ru')
        self.assertNotIn('en', question.translations['label'])

        another_data = {
            'label': 'another_label_en',
            'answer_type_id': answer_short_text.pk,
            'survey_id': survey.pk,
        }
        response = self.client.post('/admin/api/v2/survey-questions/', data=another_data, HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 201)

        another_question = SurveyQuestion.objects.get(pk=response.data['id'])
        self.assertEqual(another_question.label, 'another_label_en')
        self.assertEqual(another_question.translations['label']['en'], 'another_label_en')
        self.assertNotIn('ru', another_question.translations['label'])
    # }}}

    def test_create_question_en(self):  # {{{
        survey = SurveyFactory(language='en')
        answer_short_text = AnswerType.objects.get(slug='answer_short_text')
        data = {
            'label': 'label_en',
            'answer_type_id': answer_short_text.pk,
            'survey_id': survey.pk,
        }
        response = self.client.post('/admin/api/v2/survey-questions/', data=data, HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 201)

        question = SurveyQuestion.objects.get(pk=response.data['id'])
        self.assertEqual(question.label, 'label_en')
        self.assertEqual(question.translations['label']['en'], 'label_en')
        self.assertNotIn('ru', question.translations['label'])

        another_data = {
            'label': 'another_label_ru',
            'answer_type_id': answer_short_text.pk,
            'survey_id': survey.pk,
        }
        response = self.client.post('/admin/api/v2/survey-questions/', data=another_data, HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 201)

        another_question = SurveyQuestion.objects.get(pk=response.data['id'])
        self.assertEqual(another_question.label, 'another_label_ru')
        self.assertEqual(another_question.translations['label']['ru'], 'another_label_ru')
        self.assertNotIn('en', another_question.translations['label'])
    # }}}

    def test_patch_question_ru(self):  # {{{
        survey = SurveyFactory(language='ru')
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            label='label_ru',
            translations={
                'label': {
                    'ru': 'label_ru',
                },
            },
        )
        data_ru = {
            'id': question.pk,
            'label': 'changed_label_ru',
        }
        response = self.client.patch('/admin/api/v2/survey-questions/%s/' % question.pk, data=data_ru, HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 200)

        question_ru = SurveyQuestion.objects.get(pk=response.data['id'])
        self.assertEqual(question_ru.label, 'changed_label_ru')
        self.assertEqual(question_ru.translations['label']['ru'], 'changed_label_ru')
        self.assertNotIn('en', question_ru.translations['label'])

        data_en = {
            'id': question.pk,
            'label': 'changed_label_en',
        }
        response = self.client.patch('/admin/api/v2/survey-questions/%s/' % question.pk, data=data_en, HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 200)

        question_en = SurveyQuestion.objects.get(pk=response.data['id'])
        self.assertEqual(question_en.label, 'changed_label_ru')
        self.assertEqual(question_en.translations['label']['ru'], 'changed_label_ru')
        self.assertEqual(question_en.translations['label']['en'], 'changed_label_en')
    # }}}

    def test_patch_question_en(self):  # {{{
        survey = SurveyFactory(language='en')
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            label='label_ru',
            translations={
                'label': {
                    'en': 'label_en',
                },
            },
        )
        data_en = {
            'id': question.pk,
            'label': 'changed_label_en',
        }
        response = self.client.patch('/admin/api/v2/survey-questions/%s/' % question.pk, data=data_en, HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 200)

        question_en = SurveyQuestion.objects.get(pk=response.data['id'])
        self.assertEqual(question_en.label, 'changed_label_en')
        self.assertEqual(question_en.translations['label']['en'], 'changed_label_en')
        self.assertNotIn('ru', question_en.translations['label'])

        data_ru = {
            'id': question.pk,
            'label': 'changed_label_ru',
        }
        response = self.client.patch('/admin/api/v2/survey-questions/%s/' % question.pk, data=data_ru, HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 200)

        question_ru = SurveyQuestion.objects.get(pk=response.data['id'])
        self.assertEqual(question_ru.label, 'changed_label_en')
        self.assertEqual(question_ru.translations['label']['en'], 'changed_label_en')
        self.assertEqual(question_ru.translations['label']['ru'], 'changed_label_ru')
    # }}}

    def test_admin_query_question_ru(self):  # {{{
        survey = SurveyFactory(language='ru')
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            label='label_ru',
            translations={
                'label': {
                    'ru': 'label_ru',
                },
            },
        )
        response = self.client.get('/admin/api/v2/survey-questions/%s/' % question.pk, HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['label'], 'label_ru')

        response = self.client.get('/admin/api/v2/survey-questions/%s/' % question.pk, HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['label'], 'label_ru')

        question.translations['label']['en'] = 'label_en'
        question.save()

        response = self.client.get('/admin/api/v2/survey-questions/%s/' % question.pk, HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['label'], 'label_en')
    # }}}

    def test_admin_query_question_en(self):  # {{{
        survey = SurveyFactory(language='en')
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            label='label_en',
            translations={
                'label': {
                    'en': 'label_en',
                },
            },
        )
        response = self.client.get('/admin/api/v2/survey-questions/%s/' % question.pk, HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['label'], 'label_en')

        response = self.client.get('/admin/api/v2/survey-questions/%s/' % question.pk, HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['label'], 'label_en')

        question.translations['label']['ru'] = 'label_ru'
        question.save()

        response = self.client.get('/admin/api/v2/survey-questions/%s/' % question.pk, HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['label'], 'label_ru')
    # }}}

    def test_front_query_question_ru(self):  # {{{
        survey = SurveyFactory(language='ru', is_published_external=True)
        questions = [
            SurveyQuestionFactory(
                survey=survey,
                answer_type=AnswerType.objects.get(slug='answer_short_text'),
                param_slug='text',
                label='label_ru',
                translations={
                    'label': {
                        'ru': 'label_ru',
                    },
                },
            ),
            SurveyQuestionFactory(
                survey=survey,
                answer_type=AnswerType.objects.get(slug='answer_boolean'),
                param_slug='boolean',
                label='',
                translations={
                    'label': {
                        'en': 'boolean_en',
                    },
                },
            ),
        ]
        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['fields']['text']['label'], 'label_ru')

        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='kk')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['fields']['text']['label'], 'label_ru')

        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['fields']['text']['label'], 'label_ru')

        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='fi')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['fields']['text']['label'], 'label_ru')

        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['fields']['boolean']['label'], 'boolean')

        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='kk')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['fields']['boolean']['label'], 'boolean')

        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['fields']['boolean']['label'], 'boolean_en')

        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='fi')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['fields']['boolean']['label'], 'boolean_en')

        questions[0].translations['label']['en'] = 'label_en'
        questions[0].save()

        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['fields']['text']['label'], 'label_ru')

        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='kk')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['fields']['text']['label'], 'label_ru')

        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['fields']['text']['label'], 'label_en')

        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='fi')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['fields']['text']['label'], 'label_en')
    # }}}

    def test_front_query_question_en(self):  # {{{
        survey = SurveyFactory(language='en', is_published_external=True)
        questions = [
            SurveyQuestionFactory(
                survey=survey,
                answer_type=AnswerType.objects.get(slug='answer_short_text'),
                param_slug='text',
                label='label_en',
                translations={
                    'label': {
                        'en': 'label_en',
                    },
                },
            ),
            SurveyQuestionFactory(
                survey=survey,
                answer_type=AnswerType.objects.get(slug='answer_boolean'),
                param_slug='boolean',
                label='',
                translations={
                    'label': {
                        'ru': 'boolean_ru',
                    },
                },
            ),
        ]
        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['fields']['text']['label'], 'label_en')

        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='fi')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['fields']['text']['label'], 'label_en')

        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['fields']['text']['label'], 'label_en')

        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='kk')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['fields']['text']['label'], 'label_en')

        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['fields']['boolean']['label'], 'boolean')

        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='fi')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['fields']['boolean']['label'], 'boolean')

        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['fields']['boolean']['label'], 'boolean_ru')

        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='kk')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['fields']['boolean']['label'], 'boolean_ru')

        questions[0].translations['label']['ru'] = 'label_ru'
        questions[0].save()

        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['fields']['text']['label'], 'label_en')

        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='fi')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['fields']['text']['label'], 'label_en')

        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['fields']['text']['label'], 'label_ru')

        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='kk')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['fields']['text']['label'], 'label_ru')
    # }}}
# }}}


class TestTranslationsSurveyQuestionChoice(TestCase):  # {{{
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex(is_superuser=True)

    def test_create_question_choice_ru(self):  # {{{
        survey = SurveyFactory(language='ru')
        answer_choices = AnswerType.objects.get(slug='answer_choices')
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=answer_choices,
        )
        data = {
            'choices': [
                {
                    'slug': 'one',
                    'label': 'one_ru',
                },
                {
                    'slug': 'two',
                    'label': 'two_ru',
                },
            ],
        }
        response = self.client.patch(
            '/admin/api/v2/survey-questions/%s/' % question.pk,
            data=data,
            format='json',
            HTTP_ACCEPT_LANGUAGE='ru',
        )
        self.assertEqual(response.status_code, 200)

        choices = {
            choice.slug: choice
            for choice in SurveyQuestionChoice.objects.filter(survey_question=question)
        }
        self.assertEqual(choices['one'].label, 'one_ru')
        self.assertEqual(choices['one'].translations['label']['ru'], 'one_ru')
        self.assertNotIn('en', choices['one'].translations['label'])
        self.assertEqual(choices['two'].label, 'two_ru')
        self.assertEqual(choices['two'].translations['label']['ru'], 'two_ru')
        self.assertNotIn('en', choices['two'].translations['label'])

        another_question = SurveyQuestionFactory(
            survey=survey,
            answer_type=answer_choices,
        )
        another_data = {
            'choices': [
                {
                    'slug': 'one',
                    'label': 'first_en',
                },
                {
                    'slug': 'two',
                    'label': 'second_en',
                },
            ],
        }
        response = self.client.patch(
            '/admin/api/v2/survey-questions/%s/' % another_question.pk,
            data=another_data,
            format='json',
            HTTP_ACCEPT_LANGUAGE='en',
        )
        self.assertEqual(response.status_code, 200)

        another_choices = {
            choice.slug: choice
            for choice in SurveyQuestionChoice.objects.filter(survey_question=another_question)
        }
        self.assertEqual(another_choices['one'].label, '')
        self.assertEqual(another_choices['one'].translations['label']['en'], 'first_en')
        self.assertNotIn('ru', another_choices['one'].translations['label'])
        self.assertEqual(another_choices['two'].label, '')
        self.assertEqual(another_choices['two'].translations['label']['en'], 'second_en')
        self.assertNotIn('ru', another_choices['two'].translations['label'])
    # }}}

    def test_create_question_choice_en(self):  # {{{
        survey = SurveyFactory(language='en')
        answer_choices = AnswerType.objects.get(slug='answer_choices')
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=answer_choices,
        )
        data = {
            'choices': [
                {
                    'slug': 'one',
                    'label': 'one_en',
                },
                {
                    'slug': 'two',
                    'label': 'two_en',
                },
            ],
        }
        response = self.client.patch(
            '/admin/api/v2/survey-questions/%s/' % question.pk,
            data=data,
            format='json',
            HTTP_ACCEPT_LANGUAGE='en',
        )
        self.assertEqual(response.status_code, 200)

        choices = {
            choice.slug: choice
            for choice in SurveyQuestionChoice.objects.filter(survey_question=question)
        }
        self.assertEqual(choices['one'].label, 'one_en')
        self.assertEqual(choices['one'].translations['label']['en'], 'one_en')
        self.assertNotIn('ru', choices['one'].translations['label'])
        self.assertEqual(choices['two'].label, 'two_en')
        self.assertEqual(choices['two'].translations['label']['en'], 'two_en')
        self.assertNotIn('ru', choices['two'].translations['label'])

        another_question = SurveyQuestionFactory(
            survey=survey,
            answer_type=answer_choices,
        )
        another_data = {
            'choices': [
                {
                    'slug': 'one',
                    'label': 'first_ru',
                },
                {
                    'slug': 'two',
                    'label': 'second_ru',
                },
            ],
        }
        response = self.client.patch(
            '/admin/api/v2/survey-questions/%s/' % another_question.pk,
            data=another_data,
            format='json',
            HTTP_ACCEPT_LANGUAGE='ru',
        )
        self.assertEqual(response.status_code, 200)

        another_choices = {
            choice.slug: choice
            for choice in SurveyQuestionChoice.objects.filter(survey_question=another_question)
        }
        self.assertEqual(another_choices['one'].label, '')
        self.assertEqual(another_choices['one'].translations['label']['ru'], 'first_ru')
        self.assertNotIn('en', another_choices['one'].translations['label'])
        self.assertEqual(another_choices['two'].label, '')
        self.assertEqual(another_choices['two'].translations['label']['ru'], 'second_ru')
        self.assertNotIn('en', another_choices['two'].translations['label'])
    # }}}

    def test_patch_question_choice_ru(self):  # {{{
        survey = SurveyFactory(language='ru')
        answer_choices = AnswerType.objects.get(slug='answer_choices')
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=answer_choices,
        )
        choices = [
            SurveyQuestionChoiceFactory(
                survey_question=question,
                slug='one',
                label='one_ru',
            ),
            SurveyQuestionChoiceFactory(
                survey_question=question,
                slug='two',
                label='two_ru',
            ),
        ]
        data_ru = {
            'choices': [
                {
                    'id': choices[0].pk,
                    'label': 'changed_one_ru',
                },
                {
                    'id': choices[1].pk,
                    'label': 'changed_two_ru',
                },
            ],
        }
        response = self.client.patch(
            '/admin/api/v2/survey-questions/%s/' % question.pk,
            data=data_ru,
            format='json',
            HTTP_ACCEPT_LANGUAGE='ru',
        )
        self.assertEqual(response.status_code, 200)
        changed_choices = {
            choice.slug: choice
            for choice in SurveyQuestionChoice.objects.filter(survey_question=question)
        }
        self.assertEqual(changed_choices['one'].label, 'changed_one_ru')
        self.assertEqual(changed_choices['one'].translations['label']['ru'], 'changed_one_ru')
        self.assertNotIn('en', changed_choices['one'].translations['label'])
        self.assertEqual(changed_choices['two'].label, 'changed_two_ru')
        self.assertEqual(changed_choices['two'].translations['label']['ru'], 'changed_two_ru')
        self.assertNotIn('en', changed_choices['one'].translations['label'])

        data_en = {
            'choices': [
                {
                    'id': choices[0].pk,
                    'label': 'one_en',
                },
                {
                    'id': choices[1].pk,
                    'label': 'two_en',
                },
            ],
        }
        response = self.client.patch(
            '/admin/api/v2/survey-questions/%s/' % question.pk,
            data=data_en,
            format='json',
            HTTP_ACCEPT_LANGUAGE='en',
        )
        self.assertEqual(response.status_code, 200)
        another_choices = {
            choice.slug: choice
            for choice in SurveyQuestionChoice.objects.filter(survey_question=question)
        }
        self.assertEqual(another_choices['one'].label, 'changed_one_ru')
        self.assertEqual(another_choices['one'].translations['label']['ru'], 'changed_one_ru')
        self.assertEqual(another_choices['one'].translations['label']['en'], 'one_en')
        self.assertEqual(another_choices['two'].label, 'changed_two_ru')
        self.assertEqual(another_choices['two'].translations['label']['ru'], 'changed_two_ru')
        self.assertEqual(another_choices['two'].translations['label']['en'], 'two_en')
    # }}}

    def test_patch_question_choice_en(self):  # {{{
        survey = SurveyFactory(language='en')
        answer_choices = AnswerType.objects.get(slug='answer_choices')
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=answer_choices,
        )
        choices = [
            SurveyQuestionChoiceFactory(
                survey_question=question,
                slug='one',
                label='one_en',
            ),
            SurveyQuestionChoiceFactory(
                survey_question=question,
                slug='two',
                label='two_en',
            ),
        ]
        data_en = {
            'choices': [
                {
                    'id': choices[0].pk,
                    'label': 'changed_one_en',
                },
                {
                    'id': choices[1].pk,
                    'label': 'changed_two_en',
                },
            ],
        }
        response = self.client.patch(
            '/admin/api/v2/survey-questions/%s/' % question.pk,
            data=data_en,
            format='json',
            HTTP_ACCEPT_LANGUAGE='en',
        )
        self.assertEqual(response.status_code, 200)
        changed_choices = {
            choice.slug: choice
            for choice in SurveyQuestionChoice.objects.filter(survey_question=question)
        }
        self.assertEqual(changed_choices['one'].label, 'changed_one_en')
        self.assertEqual(changed_choices['one'].translations['label']['en'], 'changed_one_en')
        self.assertNotIn('ru', changed_choices['one'].translations['label'])
        self.assertEqual(changed_choices['two'].label, 'changed_two_en')
        self.assertEqual(changed_choices['two'].translations['label']['en'], 'changed_two_en')
        self.assertNotIn('ru', changed_choices['two'].translations['label'])

        data_ru = {
            'choices': [
                {
                    'id': choices[0].pk,
                    'label': 'one_ru',
                },
                {
                    'id': choices[1].pk,
                    'label': 'two_ru',
                },
            ],
        }
        response = self.client.patch(
            '/admin/api/v2/survey-questions/%s/' % question.pk,
            data=data_ru,
            format='json',
            HTTP_ACCEPT_LANGUAGE='ru',
        )
        self.assertEqual(response.status_code, 200)
        another_choices = {
            choice.slug: choice
            for choice in SurveyQuestionChoice.objects.filter(survey_question=question)
        }
        self.assertEqual(another_choices['one'].label, 'changed_one_en')
        self.assertEqual(another_choices['one'].translations['label']['en'], 'changed_one_en')
        self.assertEqual(another_choices['one'].translations['label']['ru'], 'one_ru')
        self.assertEqual(another_choices['two'].translations['label']['en'], 'changed_two_en')
        self.assertEqual(another_choices['two'].translations['label']['ru'], 'two_ru')
    # }}}

    def test_admin_query_survey_choice_ru(self):  # {{{
        survey = SurveyFactory(language='ru')
        answer_choices = AnswerType.objects.get(slug='answer_choices')
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=answer_choices,
        )
        choices = [
            SurveyQuestionChoiceFactory(
                survey_question=question,
                slug='one',
                label='one_ru',
                translations={
                    'label': {
                        'ru': 'one_ru',
                        'en': 'one_en',
                    },
                },
            ),
            SurveyQuestionChoiceFactory(
                survey_question=question,
                slug='two',
                label='two_ru',
                translations={
                    'label': {
                        'ru': 'two_ru',
                    },
                },
            ),
        ]
        response = self.client.get('/admin/api/v2/survey-questions/%s/' % question.pk, HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 200)

        choices = {
            choice['slug']: choice['label']
            for choice in response.data['choices']
        }
        self.assertEqual(choices['one'], 'one_ru')
        self.assertEqual(choices['two'], 'two_ru')

        response = self.client.get('/admin/api/v2/survey-questions/%s/' % question.pk, HTTP_ACCEPT_LANGUAGE='kk')
        self.assertEqual(response.status_code, 200)

        choices = {
            choice['slug']: choice['label']
            for choice in response.data['choices']
        }
        self.assertEqual(choices['one'], 'one_ru')
        self.assertEqual(choices['two'], 'two_ru')

        response = self.client.get('/admin/api/v2/survey-questions/%s/' % question.pk, HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 200)

        choices = {
            choice['slug']: choice['label']
            for choice in response.data['choices']
        }
        self.assertEqual(choices['one'], 'one_en')
        self.assertEqual(choices['two'], 'two_ru')

        response = self.client.get('/admin/api/v2/survey-questions/%s/' % question.pk, HTTP_ACCEPT_LANGUAGE='fi')
        self.assertEqual(response.status_code, 200)

        choices = {
            choice['slug']: choice['label']
            for choice in response.data['choices']
        }
        self.assertEqual(choices['one'], 'one_en')
        self.assertEqual(choices['two'], 'two_ru')
    # }}}

    def test_admin_query_survey_choice_en(self):  # {{{
        survey = SurveyFactory(language='en')
        answer_choices = AnswerType.objects.get(slug='answer_choices')
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=answer_choices,
        )
        choices = [
            SurveyQuestionChoiceFactory(
                survey_question=question,
                slug='one',
                label='one_en',
                translations={
                    'label': {
                        'en': 'one_en',
                        'ru': 'one_ru',
                    },
                },
            ),
            SurveyQuestionChoiceFactory(
                survey_question=question,
                slug='two',
                label='two_en',
                translations={
                    'label': {
                        'en': 'two_en',
                    },
                },
            ),
        ]
        response = self.client.get('/admin/api/v2/survey-questions/%s/' % question.pk, HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 200)

        choices = {
            choice['slug']: choice['label']
            for choice in response.data['choices']
        }
        self.assertEqual(choices['one'], 'one_en')
        self.assertEqual(choices['two'], 'two_en')

        response = self.client.get('/admin/api/v2/survey-questions/%s/' % question.pk, HTTP_ACCEPT_LANGUAGE='fi')
        self.assertEqual(response.status_code, 200)

        choices = {
            choice['slug']: choice['label']
            for choice in response.data['choices']
        }
        self.assertEqual(choices['one'], 'one_en')
        self.assertEqual(choices['two'], 'two_en')

        response = self.client.get('/admin/api/v2/survey-questions/%s/' % question.pk, HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 200)

        choices = {
            choice['slug']: choice['label']
            for choice in response.data['choices']
        }
        self.assertEqual(choices['one'], 'one_ru')
        self.assertEqual(choices['two'], 'two_en')

        response = self.client.get('/admin/api/v2/survey-questions/%s/' % question.pk, HTTP_ACCEPT_LANGUAGE='kk')
        self.assertEqual(response.status_code, 200)

        choices = {
            choice['slug']: choice['label']
            for choice in response.data['choices']
        }
        self.assertEqual(choices['one'], 'one_ru')
        self.assertEqual(choices['two'], 'two_en')

    def test_front_query_survey_choice_ru(self):
        survey = SurveyFactory(language='ru', is_published_external=True)
        answer_choices = AnswerType.objects.get(slug='answer_choices')
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=answer_choices,
            param_slug='choices',
        )
        choices = [
            SurveyQuestionChoiceFactory(
                survey_question=question,
                slug='one',
                label='one_ru',
                translations={
                    'label': {
                        'ru': 'one_ru',
                        'en': 'one_en',
                    },
                },
            ),
            SurveyQuestionChoiceFactory(
                survey_question=question,
                slug='two',
                label='two_ru',
                translations={
                    'label': {
                        'ru': 'two_ru',
                    },
                },
            ),
        ]
        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 200)
        choices = {
            choice['slug']: choice['text']
            for choice in response.data['fields']['choices']['data_source']['items']
        }
        self.assertEqual(choices['one'], 'one_ru')
        self.assertEqual(choices['two'], 'two_ru')

        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='kk')
        self.assertEqual(response.status_code, 200)
        choices = {
            choice['slug']: choice['text']
            for choice in response.data['fields']['choices']['data_source']['items']
        }
        self.assertEqual(choices['one'], 'one_ru')
        self.assertEqual(choices['two'], 'two_ru')

        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 200)
        choices = {
            choice['slug']: choice['text']
            for choice in response.data['fields']['choices']['data_source']['items']
        }
        self.assertEqual(choices['one'], 'one_en')
        self.assertEqual(choices['two'], 'two_ru')

        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='fi')
        self.assertEqual(response.status_code, 200)
        choices = {
            choice['slug']: choice['text']
            for choice in response.data['fields']['choices']['data_source']['items']
        }
        self.assertEqual(choices['one'], 'one_en')
        self.assertEqual(choices['two'], 'two_ru')
    # }}}

    def test_front_query_survey_choice_en(self):  # {{{
        survey = SurveyFactory(language='en', is_published_external=True)
        answer_choices = AnswerType.objects.get(slug='answer_choices')
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=answer_choices,
            param_slug='choices',
        )
        choices = [
            SurveyQuestionChoiceFactory(
                survey_question=question,
                slug='one',
                label='one_en',
                translations={
                    'label': {
                        'en': 'one_en',
                        'ru': 'one_ru',
                    },
                },
            ),
            SurveyQuestionChoiceFactory(
                survey_question=question,
                slug='two',
                label='two_en',
                translations={
                    'label': {
                        'en': 'two_en',
                    },
                },
            ),
        ]
        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 200)
        choices = {
            choice['slug']: choice['text']
            for choice in response.data['fields']['choices']['data_source']['items']
        }
        self.assertEqual(choices['one'], 'one_en')
        self.assertEqual(choices['two'], 'two_en')

        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='fi')
        self.assertEqual(response.status_code, 200)
        choices = {
            choice['slug']: choice['text']
            for choice in response.data['fields']['choices']['data_source']['items']
        }
        self.assertEqual(choices['one'], 'one_en')
        self.assertEqual(choices['two'], 'two_en')

        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 200)
        choices = {
            choice['slug']: choice['text']
            for choice in response.data['fields']['choices']['data_source']['items']
        }
        self.assertEqual(choices['one'], 'one_ru')
        self.assertEqual(choices['two'], 'two_en')

        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='kk')
        self.assertEqual(response.status_code, 200)
        choices = {
            choice['slug']: choice['text']
            for choice in response.data['fields']['choices']['data_source']['items']
        }
        self.assertEqual(choices['one'], 'one_ru')
        self.assertEqual(choices['two'], 'two_en')
    # }}}
# }}}


class TestTranslationsSurveyQuestionMatrixTitle(TestCase):  # {{{
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex(is_superuser=True)

    def test_create_question_matrix_titles_ru(self):  # {{{
        survey = SurveyFactory(language='ru')
        answer_choices = AnswerType.objects.get(slug='answer_choices')
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=answer_choices,
        )
        data = {
            'matrix_titles': [
                {
                    'type': 'row',
                    'label': 'row_ru',
                },
                {
                    'type': 'column',
                    'label': 'column_ru',
                },
            ],
        }
        response = self.client.patch(
            '/admin/api/v2/survey-questions/%s/' % question.pk,
            data=data,
            format='json',
            HTTP_ACCEPT_LANGUAGE='ru',
        )
        self.assertEqual(response.status_code, 200)

        titles = {
            title.type: title
            for title in SurveyQuestionMatrixTitle.objects.filter(survey_question=question)
        }
        self.assertEqual(titles['row'].label, 'row_ru')
        self.assertEqual(titles['row'].translations['label']['ru'], 'row_ru')
        self.assertNotIn('en', titles['row'].translations['label'])
        self.assertEqual(titles['column'].label, 'column_ru')
        self.assertEqual(titles['column'].translations['label']['ru'], 'column_ru')
        self.assertNotIn('en', titles['column'].translations['label'])

        another_question = SurveyQuestionFactory(
            survey=survey,
            answer_type=answer_choices,
        )
        another_data = {
            'matrix_titles': [
                {
                    'type': 'row',
                    'label': 'row_en',
                },
                {
                    'type': 'column',
                    'label': 'column_en',
                },
            ],
        }
        response = self.client.patch(
            '/admin/api/v2/survey-questions/%s/' % another_question.pk,
            data=another_data,
            format='json',
            HTTP_ACCEPT_LANGUAGE='en',
        )
        self.assertEqual(response.status_code, 200)

        another_choices = {
            title.type: title
            for title in SurveyQuestionMatrixTitle.objects.filter(survey_question=another_question)
        }
        self.assertEqual(another_choices['row'].label, '')
        self.assertEqual(another_choices['row'].translations['label']['en'], 'row_en')
        self.assertNotIn('ru', another_choices['row'].translations['label'])
        self.assertEqual(another_choices['column'].label, '')
        self.assertEqual(another_choices['column'].translations['label']['en'], 'column_en')
        self.assertNotIn('ru', another_choices['column'].translations['label'])
    # }}}

    def test_create_question_matrix_titles_en(self):  # {{{
        survey = SurveyFactory(language='en')
        answer_choices = AnswerType.objects.get(slug='answer_choices')
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=answer_choices,
        )
        data = {
            'matrix_titles': [
                {
                    'type': 'row',
                    'label': 'row_en',
                },
                {
                    'type': 'column',
                    'label': 'column_en',
                },
            ],
        }
        response = self.client.patch(
            '/admin/api/v2/survey-questions/%s/' % question.pk,
            data=data,
            format='json',
            HTTP_ACCEPT_LANGUAGE='en',
        )
        self.assertEqual(response.status_code, 200)

        titles = {
            title.type: title
            for title in SurveyQuestionMatrixTitle.objects.filter(survey_question=question)
        }
        self.assertEqual(titles['row'].label, 'row_en')
        self.assertEqual(titles['row'].translations['label']['en'], 'row_en')
        self.assertNotIn('ru', titles['row'].translations['label'])
        self.assertEqual(titles['column'].label, 'column_en')
        self.assertEqual(titles['column'].translations['label']['en'], 'column_en')
        self.assertNotIn('ru', titles['column'].translations['label'])

        another_question = SurveyQuestionFactory(
            survey=survey,
            answer_type=answer_choices,
        )
        another_data = {
            'matrix_titles': [
                {
                    'type': 'row',
                    'label': 'row_ru',
                },
                {
                    'type': 'column',
                    'label': 'column_ru',
                },
            ],
        }
        response = self.client.patch(
            '/admin/api/v2/survey-questions/%s/' % another_question.pk,
            data=another_data,
            format='json',
            HTTP_ACCEPT_LANGUAGE='ru',
        )
        self.assertEqual(response.status_code, 200)

        another_choices = {
            title.type: title
            for title in SurveyQuestionMatrixTitle.objects.filter(survey_question=another_question)
        }
        self.assertEqual(another_choices['row'].label, '')
        self.assertEqual(another_choices['row'].translations['label']['ru'], 'row_ru')
        self.assertNotIn('en', another_choices['row'].translations['label'])
        self.assertEqual(another_choices['column'].label, '')
        self.assertEqual(another_choices['column'].translations['label']['ru'], 'column_ru')
        self.assertNotIn('en', another_choices['column'].translations['label'])
    # }}}

    def test_patch_question_matrix_title_ru(self):  # {{{
        survey = SurveyFactory(language='ru')
        answer_choices = AnswerType.objects.get(slug='answer_choices')
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=answer_choices,
        )
        titles = [
            SurveyQuestionMatrixTitleFactory(
                survey_question=question,
                type='row',
                label='row_ru',

            ),
            SurveyQuestionMatrixTitleFactory(
                survey_question=question,
                type='column',
                label='column_ru',
            ),
        ]
        data_ru = {
            'matrix_titles': [
                {
                    'id': titles[0].pk,
                    'label': 'changed_row_ru',
                },
                {
                    'id': titles[1].pk,
                    'label': 'changed_column_ru',
                },
            ],
        }
        response = self.client.patch(
            '/admin/api/v2/survey-questions/%s/' % question.pk,
            data=data_ru,
            format='json',
            HTTP_ACCEPT_LANGUAGE='ru',
        )
        self.assertEqual(response.status_code, 200)
        changed_titles = {
            title.type: title
            for title in SurveyQuestionMatrixTitle.objects.filter(survey_question=question)
        }
        self.assertEqual(changed_titles['row'].label, 'changed_row_ru')
        self.assertEqual(changed_titles['row'].translations['label']['ru'], 'changed_row_ru')
        self.assertNotIn('en', changed_titles['row'].translations['label'])
        self.assertEqual(changed_titles['column'].label, 'changed_column_ru')
        self.assertEqual(changed_titles['column'].translations['label']['ru'], 'changed_column_ru')
        self.assertNotIn('en', changed_titles['column'].translations['label'])

        data_en = {
            'matrix_titles': [
                {
                    'id': titles[0].pk,
                    'label': 'row_en',
                },
                {
                    'id': titles[1].pk,
                    'label': 'column_en',
                },
            ],
        }
        response = self.client.patch(
            '/admin/api/v2/survey-questions/%s/' % question.pk,
            data=data_en,
            format='json',
            HTTP_ACCEPT_LANGUAGE='en',
        )
        self.assertEqual(response.status_code, 200)
        another_titles = {
            title.type: title
            for title in SurveyQuestionMatrixTitle.objects.filter(survey_question=question)
        }
        self.assertEqual(another_titles['row'].label, 'changed_row_ru')
        self.assertEqual(another_titles['row'].translations['label']['ru'], 'changed_row_ru')
        self.assertEqual(another_titles['row'].translations['label']['en'], 'row_en')
        self.assertEqual(another_titles['column'].label, 'changed_column_ru')
        self.assertEqual(another_titles['column'].translations['label']['ru'], 'changed_column_ru')
        self.assertEqual(another_titles['column'].translations['label']['en'], 'column_en')
    # }}}

    def test_patch_question_matrix_title_en(self):  # {{{
        survey = SurveyFactory(language='en')
        answer_choices = AnswerType.objects.get(slug='answer_choices')
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=answer_choices,
        )
        titles = [
            SurveyQuestionMatrixTitleFactory(
                survey_question=question,
                type='row',
                label='row_en',

            ),
            SurveyQuestionMatrixTitleFactory(
                survey_question=question,
                type='column',
                label='column_en',
            ),
        ]
        data_en = {
            'matrix_titles': [
                {
                    'id': titles[0].pk,
                    'label': 'changed_row_en',
                },
                {
                    'id': titles[1].pk,
                    'label': 'changed_column_en',
                },
            ],
        }
        response = self.client.patch(
            '/admin/api/v2/survey-questions/%s/' % question.pk,
            data=data_en,
            format='json',
            HTTP_ACCEPT_LANGUAGE='en',
        )
        self.assertEqual(response.status_code, 200)
        changed_titles = {
            title.type: title
            for title in SurveyQuestionMatrixTitle.objects.filter(survey_question=question)
        }
        self.assertEqual(changed_titles['row'].label, 'changed_row_en')
        self.assertEqual(changed_titles['row'].translations['label']['en'], 'changed_row_en')
        self.assertNotIn('ru', changed_titles['row'].translations['label'])
        self.assertEqual(changed_titles['column'].label, 'changed_column_en')
        self.assertEqual(changed_titles['column'].translations['label']['en'], 'changed_column_en')
        self.assertNotIn('ru', changed_titles['column'].translations['label'])

        data_ru = {
            'matrix_titles': [
                {
                    'id': titles[0].pk,
                    'label': 'row_ru',
                },
                {
                    'id': titles[1].pk,
                    'label': 'column_ru',
                },
            ],
        }
        response = self.client.patch(
            '/admin/api/v2/survey-questions/%s/' % question.pk,
            data=data_ru,
            format='json',
            HTTP_ACCEPT_LANGUAGE='ru',
        )
        self.assertEqual(response.status_code, 200)
        another_titles = {
            title.type: title
            for title in SurveyQuestionMatrixTitle.objects.filter(survey_question=question)
        }
        self.assertEqual(another_titles['row'].label, 'changed_row_en')
        self.assertEqual(another_titles['row'].translations['label']['en'], 'changed_row_en')
        self.assertEqual(another_titles['row'].translations['label']['ru'], 'row_ru')
        self.assertEqual(another_titles['column'].label, 'changed_column_en')
        self.assertEqual(another_titles['column'].translations['label']['en'], 'changed_column_en')
        self.assertEqual(another_titles['column'].translations['label']['ru'], 'column_ru')
    # }}}

    def test_admin_query_survey_matrix_title_ru(self):  # {{{
        survey = SurveyFactory(language='ru')
        answer_choices = AnswerType.objects.get(slug='answer_choices')
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=answer_choices,
        )
        titles = [
            SurveyQuestionMatrixTitleFactory(
                survey_question=question,
                type='row',
                label='row_ru',
                translations={
                    'label': {
                        'ru': 'row_ru',
                        'en': 'row_en',
                    },
                },
            ),
            SurveyQuestionMatrixTitleFactory(
                survey_question=question,
                type='column',
                label='column_ru',
                translations={
                    'label': {
                        'ru': 'column_ru',
                    },
                },
            ),
        ]
        response = self.client.get('/admin/api/v2/survey-questions/%s/' % question.pk, HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 200)

        titles = {
            choice['type']: choice['label']
            for choice in response.data['matrix_titles']
        }
        self.assertEqual(titles['row'], 'row_ru')
        self.assertEqual(titles['column'], 'column_ru')

        response = self.client.get('/admin/api/v2/survey-questions/%s/' % question.pk, HTTP_ACCEPT_LANGUAGE='kk')
        self.assertEqual(response.status_code, 200)

        titles = {
            choice['type']: choice['label']
            for choice in response.data['matrix_titles']
        }
        self.assertEqual(titles['row'], 'row_ru')
        self.assertEqual(titles['column'], 'column_ru')

        response = self.client.get('/admin/api/v2/survey-questions/%s/' % question.pk, HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 200)

        titles = {
            choice['type']: choice['label']
            for choice in response.data['matrix_titles']
        }
        self.assertEqual(titles['row'], 'row_en')
        self.assertEqual(titles['column'], 'column_ru')

        response = self.client.get('/admin/api/v2/survey-questions/%s/' % question.pk, HTTP_ACCEPT_LANGUAGE='fi')
        self.assertEqual(response.status_code, 200)

        titles = {
            choice['type']: choice['label']
            for choice in response.data['matrix_titles']
        }
        self.assertEqual(titles['row'], 'row_en')
        self.assertEqual(titles['column'], 'column_ru')
    # }}}

    def test_admin_query_survey_matrix_title_en(self):  # {{{
        survey = SurveyFactory(language='en')
        answer_choices = AnswerType.objects.get(slug='answer_choices')
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=answer_choices,
        )
        titles = [
            SurveyQuestionMatrixTitleFactory(
                survey_question=question,
                type='row',
                label='row_en',
                translations={
                    'label': {
                        'en': 'row_en',
                        'ru': 'row_ru',
                    },
                },
            ),
            SurveyQuestionMatrixTitleFactory(
                survey_question=question,
                type='column',
                label='column_en',
                translations={
                    'label': {
                        'en': 'column_en',
                    },
                },
            ),
        ]
        response = self.client.get('/admin/api/v2/survey-questions/%s/' % question.pk, HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 200)

        titles = {
            title['type']: title['label']
            for title in response.data['matrix_titles']
        }
        self.assertEqual(titles['row'], 'row_en')
        self.assertEqual(titles['column'], 'column_en')

        response = self.client.get('/admin/api/v2/survey-questions/%s/' % question.pk, HTTP_ACCEPT_LANGUAGE='fi')
        self.assertEqual(response.status_code, 200)

        titles = {
            title['type']: title['label']
            for title in response.data['matrix_titles']
        }
        self.assertEqual(titles['row'], 'row_en')
        self.assertEqual(titles['column'], 'column_en')

        response = self.client.get('/admin/api/v2/survey-questions/%s/' % question.pk, HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 200)

        titles = {
            title['type']: title['label']
            for title in response.data['matrix_titles']
        }
        self.assertEqual(titles['row'], 'row_ru')
        self.assertEqual(titles['column'], 'column_en')

        response = self.client.get('/admin/api/v2/survey-questions/%s/' % question.pk, HTTP_ACCEPT_LANGUAGE='kk')
        self.assertEqual(response.status_code, 200)

        titles = {
            title['type']: title['label']
            for title in response.data['matrix_titles']
        }
        self.assertEqual(titles['row'], 'row_ru')
        self.assertEqual(titles['column'], 'column_en')
    # }}}

    def test_front_query_survey_matrix_title_ru(self):  # {{{
        survey = SurveyFactory(language='ru', is_published_external=True)
        answer_choices = AnswerType.objects.get(slug='answer_choices')
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=answer_choices,
            param_slug='matrix_titles',
            param_data_source='survey_question_matrix_choice',
        )
        titles = [
            SurveyQuestionMatrixTitleFactory(
                survey_question=question,
                type='row',
                label='row_ru',
                translations={
                    'label': {
                        'ru': 'row_ru',
                        'en': 'row_en',
                    },
                },
            ),
            SurveyQuestionMatrixTitleFactory(
                survey_question=question,
                type='column',
                label='column_ru',
                translations={
                    'label': {
                        'ru': 'column_ru',
                    },
                },
            ),
        ]
        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 200)
        titles = {
            title['type']: title['text']
            for title in response.data['fields']['matrix_titles']['data_source']['items']
        }
        self.assertEqual(titles['row'], 'row_ru')
        self.assertEqual(titles['column'], 'column_ru')

        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='kk')
        self.assertEqual(response.status_code, 200)
        titles = {
            title['type']: title['text']
            for title in response.data['fields']['matrix_titles']['data_source']['items']
        }
        self.assertEqual(titles['row'], 'row_ru')
        self.assertEqual(titles['column'], 'column_ru')

        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 200)
        titles = {
            title['type']: title['text']
            for title in response.data['fields']['matrix_titles']['data_source']['items']
        }
        self.assertEqual(titles['row'], 'row_en')
        self.assertEqual(titles['column'], 'column_ru')

        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='fi')
        self.assertEqual(response.status_code, 200)
        titles = {
            title['type']: title['text']
            for title in response.data['fields']['matrix_titles']['data_source']['items']
        }
        self.assertEqual(titles['row'], 'row_en')
        self.assertEqual(titles['column'], 'column_ru')
    # }}}

    def test_front_query_survey_matrix_title_en(self):  # {{{
        survey = SurveyFactory(language='en', is_published_external=True)
        answer_choices = AnswerType.objects.get(slug='answer_choices')
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=answer_choices,
            param_slug='matrix_titles',
            param_data_source='survey_question_matrix_choice',
        )
        titles = [
            SurveyQuestionMatrixTitleFactory(
                survey_question=question,
                type='row',
                label='row_en',
                translations={
                    'label': {
                        'en': 'row_en',
                        'ru': 'row_ru',
                    },
                },
            ),
            SurveyQuestionMatrixTitleFactory(
                survey_question=question,
                type='column',
                label='column_en',
                translations={
                    'label': {
                        'en': 'column_en',
                    },
                },
            ),
        ]
        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 200)
        titles = {
            title['type']: title['text']
            for title in response.data['fields']['matrix_titles']['data_source']['items']
        }
        self.assertEqual(titles['row'], 'row_en')
        self.assertEqual(titles['column'], 'column_en')

        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='fi')
        self.assertEqual(response.status_code, 200)
        titles = {
            title['type']: title['text']
            for title in response.data['fields']['matrix_titles']['data_source']['items']
        }
        self.assertEqual(titles['row'], 'row_en')
        self.assertEqual(titles['column'], 'column_en')

        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 200)
        titles = {
            title['type']: title['text']
            for title in response.data['fields']['matrix_titles']['data_source']['items']
        }
        self.assertEqual(titles['row'], 'row_ru')
        self.assertEqual(titles['column'], 'column_en')

        response = self.client.get('/v1/surveys/%s/form/' % survey.pk, HTTP_ACCEPT_LANGUAGE='kk')
        self.assertEqual(response.status_code, 200)
        titles = {
            title['type']: title['text']
            for title in response.data['fields']['matrix_titles']['data_source']['items']
        }
        self.assertEqual(titles['row'], 'row_ru')
        self.assertEqual(titles['column'], 'column_en')
    # }}}
# }}}


class TestTranslationSubmitForm(TestCase):  # {{{
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):  # {{{
        self.profile = self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory(  # {{{
            language='ru',
            name='name_ru',
            translations={
                'name': {
                    'ru': 'name_ru',
                    'en': 'name_en',
                },
            },
            is_published_external=True,
            user=self.profile,
        )  # }}}
        self.questions = [  # {{{
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=AnswerType.objects.get(slug='answer_short_text'),
                param_is_required=False,
                param_slug='text',
                label='text_ru',
                translations={
                    'label': {
                        'ru': 'text_ru',
                        'en': 'text_en',
                    },
                },
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=AnswerType.objects.get(slug='answer_choices'),
                param_is_required=False,
                param_slug='choices',
                label='choices_ru',
                translations={
                    'label': {
                        'ru': 'choices_ru',
                        'en': 'choices_en',
                    },
                },
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=AnswerType.objects.get(slug='answer_choices'),
                param_is_required=False,
                param_data_source='survey_question_matrix_choice',
                param_slug='titles',
                label='titles_ru',
                translations={
                    'label': {
                        'ru': 'titles_ru',
                        'en': 'titles_en',
                    },
                },
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=AnswerType.objects.get(slug='answer_boolean'),
                param_is_required=False,
                param_slug='boolean',
                label='boolean_ru',
                translations={
                    'label': {
                        'ru': 'boolean_ru',
                        'en': 'boolean_en',
                    },
                },
            ),
        ]  # }}}
        self.choices = [  # {{{
            SurveyQuestionChoiceFactory(
                survey_question=self.questions[1],
                slug='one',
                label='one_ru',
                translations={
                    'label': {
                        'ru': 'one_ru',
                        'en': 'one_en',
                    }
                },
            ),
            SurveyQuestionChoiceFactory(
                survey_question=self.questions[1],
                slug='two',
                label='two_ru',
                translations={
                    'label': {
                        'ru': 'two_ru',
                        'en': 'two_en',
                    }
                },
            ),
        ]  # }}}
        self.titles = [  # {{{
            SurveyQuestionMatrixTitleFactory(
                survey_question=self.questions[2],
                type='row',
                label='row_ru',
                translations={
                    'label': {
                        'ru': 'row_ru',
                        'en': 'row_en',
                    }
                },
            ),
            SurveyQuestionMatrixTitleFactory(
                survey_question=self.questions[2],
                type='column',
                label='column1_ru',
                translations={
                    'label': {
                        'ru': 'column1_ru',
                        'en': 'column1_en',
                    }
                },
            ),
            SurveyQuestionMatrixTitleFactory(
                survey_question=self.questions[2],
                type='column',
                label='column2_ru',
                translations={
                    'label': {
                        'ru': 'column2_ru',
                        'en': 'column2_en',
                    }
                },
            ),
        ]  # }}}
        self.hook = SurveyHookFactory(survey=self.survey)
        self.subscription = ServiceSurveyHookSubscriptionFactory(
            survey_hook=self.hook,
            email_to_address='user@domain.com',
            email_from_address='devnull@domain.com',
            email_spam_check=False,
            context_language='from_request',
            title='title',
            body='body',
        )
        self.variables = [
            SurveyVariableFactory(
                hook_subscription=self.subscription,
                var='form.name',
            ),
            SurveyVariableFactory(
                hook_subscription=self.subscription,
                var='form.questions_answers',
                arguments={
                    'questions': [
                        self.questions[0].pk,
                        self.questions[1].pk,
                        self.questions[2].pk,
                        self.questions[3].pk,
                    ],
                }
            ),
        ]
        self.subscription.title = '{%s}' % self.variables[0].variable_id
        self.subscription.body = '{%s}' % self.variables[1].variable_id
        self.subscription.save()
    # }}}

    def register_uri(self, status=200, result=None):
        result = result or {
            'message_id': '<123@hostname>',
            'status': 'OK',
            'task_id': '1234',
        }
        body = {
            'result': result,
        }
        url = settings.SENDER_URL.format(
            account=settings.SENDER_ACCOUNT,
            campaign=settings.APP_TYPE,
        )
        responses.add(responses.POST, url, json=body, status=status)

    def assertRequestIsNotEmpty(self):
        self.assertEqual(len(responses.calls), 1)

    def assertRequestIsEmpty(self):
        self.assertEqual(len(responses.calls), 0)

    @responses.activate
    def test_submit_ru(self):  # {{{
        self.register_uri()
        data = {
            self.questions[0].param_slug: 'ru',
            self.questions[1].param_slug: '%s' % (self.choices[1].pk,),
            self.questions[2].param_slug: '%s_%s' % (self.titles[0].pk, self.titles[2].pk),
            self.questions[3].param_slug: 'on',
        }
        response = self.client.post(
            '/v1/surveys/%s/form/' % self.survey.pk,
            data=data,
            HTTP_ACCEPT_LANGUAGE='ru',
        )
        self.assertEqual(response.status_code, 200)
        response.data['answer_id']

        self.assertRequestIsNotEmpty()
        request = responses.calls[0].request
        request_json = json.loads(request.body.decode())

        self.assertDictEqual(request_json['args'], {
            'subject': 'name_ru',
            'body': (
                'text_ru:\n'
                'ru\n\n'
                'choices_ru:\n'
                'two_ru\n\n'
                'titles_ru:\n'
                '"row_ru": column2_ru\n\n'
                'boolean_ru:\n'
                'Да\n\n%s' %
                EmailBodyField.disclaimer
            ),
        })
    # }}}

    @responses.activate
    def test_submit_kk(self):  # {{{
        self.register_uri()
        data = {
            self.questions[0].param_slug: 'kk',
            self.questions[1].param_slug: '%s' % (self.choices[1].pk,),
            self.questions[2].param_slug: '%s_%s' % (self.titles[0].pk, self.titles[2].pk),
            self.questions[3].param_slug: '',
        }
        response = self.client.post(
            '/v1/surveys/%s/form/' % self.survey.pk,
            data=data,
            HTTP_ACCEPT_LANGUAGE='ru',
        )
        self.assertEqual(response.status_code, 200)
        response.data['answer_id']

        self.assertRequestIsNotEmpty()
        request = responses.calls[0].request
        request_json = json.loads(request.body.decode())

        self.assertDictEqual(request_json['args'], {
            'subject': 'name_ru',
            'body': (
                'text_ru:\n'
                'kk\n\n'
                'choices_ru:\n'
                'two_ru\n\n'
                'titles_ru:\n'
                '"row_ru": column2_ru\n\n'
                'boolean_ru:\n'
                'Нет\n\n%s' %
                EmailBodyField.disclaimer
            ),
        })
    # }}}

    @responses.activate
    def test_submit_en(self):  # {{{
        self.register_uri()
        data = {
            self.questions[0].param_slug: 'en',
            self.questions[1].param_slug: '%s' % (self.choices[1].pk,),
            self.questions[2].param_slug: '%s_%s' % (self.titles[0].pk, self.titles[2].pk),
            self.questions[3].param_slug: 'on',
        }
        response = self.client.post(
            '/v1/surveys/%s/form/' % self.survey.pk,
            data=data,
            HTTP_ACCEPT_LANGUAGE='en',
        )
        self.assertEqual(response.status_code, 200)
        response.data['answer_id']

        self.assertRequestIsNotEmpty()
        request = responses.calls[0].request
        request_json = json.loads(request.body.decode())

        self.assertDictEqual(request_json['args'], {
            'subject': 'name_en',
            'body': (
                'text_en:\n'
                'en\n\n'
                'choices_en:\n'
                'two_en\n\n'
                'titles_en:\n'
                '"row_en": column2_en\n\n'
                'boolean_en:\n'
                'Yes\n\n%s' %
                EmailBodyField.disclaimer
            ),
        })
    # }}}

    @responses.activate
    def test_submit_fi(self):  # {{{
        self.register_uri()
        data = {
            self.questions[0].param_slug: 'fi',
            self.questions[1].param_slug: '%s' % (self.choices[1].pk,),
            self.questions[2].param_slug: '%s_%s' % (self.titles[0].pk, self.titles[2].pk),
            self.questions[3].param_slug: '',
        }
        response = self.client.post(
            '/v1/surveys/%s/form/' % self.survey.pk,
            data=data,
            HTTP_ACCEPT_LANGUAGE='en',
        )
        self.assertEqual(response.status_code, 200)
        response.data['answer_id']

        self.assertRequestIsNotEmpty()
        request = responses.calls[0].request
        request_json = json.loads(request.body.decode())

        self.assertDictEqual(request_json['args'], {
            'subject': 'name_en',
            'body': (
                'text_en:\n'
                'fi\n\n'
                'choices_en:\n'
                'two_en\n\n'
                'titles_en:\n'
                '"row_en": column2_en\n\n'
                'boolean_en:\n'
                'No\n\n%s' %
                EmailBodyField.disclaimer
            ),
        })
    # }}}
# }}}
