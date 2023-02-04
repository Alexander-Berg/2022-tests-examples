# -*- coding: utf-8 -*-
import responses

from django.conf import settings
from django.test import TestCase, override_settings

from events.accounts.helpers import YandexClient
from events.surveyme.factories import SurveyFactory
from events.surveyme.models import Survey
from events.surveyme.api_admin.v2.serializers import SurveyBanSerializer
from events.yauth_contrib.helpers import TVMAuthTestCase


class TestSurveyBanSerializer(TestCase):
    serializer_class = SurveyBanSerializer

    def setUp(self):
        self.survey = SurveyFactory(
            is_published_external=True,
            is_public=True,
            captcha_display_mode='auto',
        )

    def test_should_ban_survey(self):
        data = {
            'ban': True,
        }
        serializer = self.serializer_class(self.survey, data=data)
        self.assertTrue(serializer.is_valid())

        survey = serializer.save()
        self.assertTrue(survey.is_ban_detected)
        self.assertFalse(survey.is_published_external)

    def test_should_unban_survey(self):
        self.survey.is_ban_detected = True
        self.survey.is_published_external = False
        self.survey.save()

        data = {
            'ban': False,
        }
        serializer = self.serializer_class(self.survey, data=data)
        self.assertTrue(serializer.is_valid())

        survey = serializer.save()
        self.assertFalse(survey.is_ban_detected)
        self.assertFalse(survey.is_published_external)

    @responses.activate
    def test_shouldnt_turn_on_capture_intranet(self):
        Survey.set_spam_detected(self.survey.pk, True)
        response = self.client.get(f'/v1/surveys/{self.survey.pk}/form/')
        self.assertEqual(response.status_code, 200)

    @responses.activate
    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_turn_on_capture_business(self):
        responses.add(
            responses.GET,
            'http://api.captcha.yandex.net/generate',
            body='<number url="https://ext.captcha.yandex.net/image?key=123">123</number>',
            content_type='text/xml',
        )
        Survey.set_spam_detected(self.survey.pk, True)

        response = self.client.get(f'/v1/surveys/{self.survey.pk}/form/')
        self.assertEqual(response.status_code, 200)
        self.assertIn('captcha', response.data['fields'])

    def test_should_turn_off_capture(self):
        Survey.set_spam_detected(self.survey.pk, False)

        response = self.client.get(f'/v1/surveys/{self.survey.pk}/form/')
        self.assertEqual(response.status_code, 200)
        self.assertNotIn('captcha', response.data['fields'])


class TestSurveyBanViewSupport(TestCase):
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_staff=True)
        self.survey = SurveyFactory()

    def test_should_change_spam_ban_status(self):
        data = {
            'ban': True,
        }
        response = self.client.post(f'/admin/api/v2/survey-ban/{self.survey.pk}/', data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertTrue(response.data['ban'])

        self.survey.refresh_from_db()
        self.assertTrue(self.survey.is_ban_detected)

    def test_should_return_spam_ban_status(self):
        self.survey.is_ban_detected = True
        self.survey.save()

        response = self.client.get(f'/admin/api/v2/survey-ban/{self.survey.pk}/')
        self.assertEqual(response.status_code, 200)
        self.assertTrue(response.data['ban'])


class TestSurveyBanViewSuperUser(TestCase):
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory()

    def test_should_change_spam_ban_status(self):
        data = {
            'ban': True,
        }
        response = self.client.post(f'/admin/api/v2/survey-ban/{self.survey.pk}/', data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertTrue(response.data['ban'])

        self.survey.refresh_from_db()
        self.assertTrue(self.survey.is_ban_detected)

    def test_should_return_spam_ban_status(self):
        self.survey.is_ban_detected = True
        self.survey.save()

        response = self.client.get(f'/admin/api/v2/survey-ban/{self.survey.pk}/')
        self.assertEqual(response.status_code, 200)
        self.assertTrue(response.data['ban'])

    def test_shouldnt_copy_survey(self):
        self.survey.is_ban_detected = True
        self.survey.save()

        response = self.client.post(f'/admin/api/v2/surveys/{self.survey.pk}/copy/')
        self.assertEqual(response.status_code, 400)

    def test_shouldnt_change_publication_status(self):
        self.survey.is_ban_detected = True
        self.survey.save()

        data = {
            'is_published_external': True,
        }
        response = self.client.patch(f'/admin/api/v2/surveys/{self.survey.pk}/', data=data, format='json')
        self.assertEqual(response.status_code, 400)
        self.assertIn('is_published_external', response.data)

    def test_should_return_ban_status_for_survey(self):
        self.survey.is_ban_detected = True
        self.survey.save()

        response = self.client.get(f'/admin/api/v2/surveys/{self.survey.pk}/')
        self.assertEqual(response.status_code, 200)
        self.assertTrue(response.data['is_ban_detected'])


class TestSurveyBanView_tvm_service(TVMAuthTestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory(is_published_external=True)

    def test_should_change_ban_status(self):
        self.client.set_service_ticket(settings.ANTISPAM_TVM2_CLIENTS[0])
        data = {
            'ban': True,
        }
        response = self.client.post(f'/admin/api/v2/survey-ban/{self.survey.pk}/', data=data, format='json')
        self.assertEqual(response.status_code, 200)

        self.survey.refresh_from_db()
        self.assertTrue(self.survey.is_ban_detected)
