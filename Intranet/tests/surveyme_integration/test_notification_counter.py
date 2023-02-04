# -*- coding: utf-8 -*-
import responses

from unittest.mock import patch, Mock
from django.test import TestCase

from events.accounts.helpers import YandexClient
from events.surveyme.factories import (
    SurveyFactory,
    SurveyQuestionFactory,
)
from events.surveyme.models import AnswerType
from events.surveyme_integration.factories import (
    ServiceSurveyHookSubscriptionFactory,
    SurveyHookFactory,
)
from events.surveyme_integration.services.http.services import HTTPService


def mock_response():
    return {
        'status': 'success',
        'response': {},
    }


class TestHookSubscriptionNotificationCounter(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory(is_published_external=True, is_allow_multiple_answers=True)
        self.question = SurveyQuestionFactory(
            label='short',
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            param_is_required=False,
        )
        self.survey_hook = SurveyHookFactory(survey=self.survey)
        self.subscription = ServiceSurveyHookSubscriptionFactory(
            survey_hook=self.survey_hook,
            service_type_action_id=4,  # rpc post
            http_url='http://yandex.ru/test_url/',
        )
        self.data = {
            self.question.get_form_field_name(): 'Short',
        }
        self.hide_notifications_url = '/admin/api/v2/surveys/%s/hide-notifications/' % self.survey.pk

    @responses.activate
    def post_data(self):
        responses.add(responses.POST, 'http://yandex.ru/test_url/', json={}, status=499)
        response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk, self.data)
        self.assertEqual(response.status_code, 200)
        return response

    def test_errors_counter_should_be_increased(self):
        self.post_data()
        self.assertEqual(self.subscription.hooksubscriptionnotification_set.count(), 1)
        self.assertEqual(self.subscription.hooksubscriptionnotificationcounter.errors_count, 0)

        self.subscription.hooksubscriptionnotificationcounter.refresh_from_db()
        self.assertEqual(self.subscription.hooksubscriptionnotificationcounter.errors_count, 1)

        with patch.object(HTTPService, 'post', Mock(return_value=mock_response())):
            self.post_data()
        self.assertEqual(self.subscription.hooksubscriptionnotification_set.count(), 2)

        self.subscription.hooksubscriptionnotificationcounter.refresh_from_db()
        self.assertEqual(self.subscription.hooksubscriptionnotificationcounter.errors_count, 1)

        self.post_data()
        self.assertEqual(self.subscription.hooksubscriptionnotification_set.count(), 3)
        self.assertEqual(self.subscription.hooksubscriptionnotificationcounter.errors_count, 1)

        self.subscription.hooksubscriptionnotificationcounter.refresh_from_db()
        self.assertEqual(self.subscription.hooksubscriptionnotificationcounter.errors_count, 2)

        self.subscription.hooksubscriptionnotificationcounter.refresh_from_db()
        self.assertEqual(self.subscription.hooksubscriptionnotificationcounter.errors_count, 2)

    def test_errors_counter_shouldnt_be_increased(self):
        self.subscription.hooksubscriptionnotificationcounter.refresh_from_db()
        self.assertEqual(self.subscription.hooksubscriptionnotificationcounter.errors_count, 0)

        self.subscription.hooksubscriptionnotificationcounter.refresh_from_db()
        self.assertEqual(self.subscription.hooksubscriptionnotificationcounter.errors_count, 0)

    def test_errors_counter_should_be_reset_with_survey_id(self):
        response = self.post_data()
        response = self.post_data()

        self.subscription.hooksubscriptionnotificationcounter.refresh_from_db()
        self.assertEqual(self.subscription.hooksubscriptionnotificationcounter.errors_count, 2)

        response = self.client.post(self.hide_notifications_url)
        self.assertEqual(response.status_code, 200)
        self.subscription.hooksubscriptionnotificationcounter.refresh_from_db()
        self.assertEqual(self.subscription.hooksubscriptionnotificationcounter.errors_count, 0)

    def test_errors_counter_shouldnt_be_reset_with_wrong_subscription_id(self):
        response = self.post_data()
        response = self.post_data()

        self.subscription.hooksubscriptionnotificationcounter.refresh_from_db()
        self.assertEqual(self.subscription.hooksubscriptionnotificationcounter.errors_count, 2)

        response = self.client.post(self.hide_notifications_url, data={'subscription': 9999})
        self.assertEqual(response.status_code, 200)
        self.subscription.hooksubscriptionnotificationcounter.refresh_from_db()
        self.assertEqual(self.subscription.hooksubscriptionnotificationcounter.errors_count, 2)

    def test_errors_counter_should_be_reset_with_subscription_id(self):
        response = self.post_data()
        response = self.post_data()

        self.subscription.hooksubscriptionnotificationcounter.refresh_from_db()
        self.assertEqual(self.subscription.hooksubscriptionnotificationcounter.errors_count, 2)

        response = self.client.post(self.hide_notifications_url, data={'subscription': self.subscription.pk})
        self.assertEqual(response.status_code, 200)
        self.subscription.hooksubscriptionnotificationcounter.refresh_from_db()
        self.assertEqual(self.subscription.hooksubscriptionnotificationcounter.errors_count, 0)

    def test_errors_counter_should_be_increased_after_reset(self):
        response = self.post_data()
        response = self.post_data()

        self.subscription.hooksubscriptionnotificationcounter.refresh_from_db()
        self.assertEqual(self.subscription.hooksubscriptionnotificationcounter.errors_count, 2)

        response = self.client.post(self.hide_notifications_url, data={'subscription': self.subscription.pk})
        self.assertEqual(response.status_code, 200)
        self.subscription.hooksubscriptionnotificationcounter.refresh_from_db()
        self.assertEqual(self.subscription.hooksubscriptionnotificationcounter.errors_count, 0)

        response = self.post_data()

        self.subscription.hooksubscriptionnotificationcounter.refresh_from_db()
        self.assertEqual(self.subscription.hooksubscriptionnotificationcounter.errors_count, 1)

    def test_errors_counter_should_be_reset_with_notification_id(self):
        response = self.post_data()

        self.subscription.hooksubscriptionnotificationcounter.refresh_from_db()
        self.assertEqual(self.subscription.hooksubscriptionnotificationcounter.errors_count, 1)

        notification = self.subscription.hooksubscriptionnotification_set.first()
        url = '/admin/api/v2/notifications/%s/' % notification.pk

        response = self.client.patch(url, data={'is_visible': False, 'status': 'canceled'})
        self.assertEqual(response.status_code, 200)
        self.subscription.hooksubscriptionnotificationcounter.refresh_from_db()
        self.assertEqual(self.subscription.hooksubscriptionnotificationcounter.errors_count, 0)
        notification.refresh_from_db()
        self.assertEqual(notification.is_visible, False)
        self.assertNotEqual(notification.status, 'canceled')

        response = self.client.patch(url, data={'is_visible': True, 'status': 'canceled'})
        self.assertEqual(response.status_code, 200)
        self.subscription.hooksubscriptionnotificationcounter.refresh_from_db()
        self.assertEqual(self.subscription.hooksubscriptionnotificationcounter.errors_count, 1)
        notification.refresh_from_db()
        self.assertEqual(notification.is_visible, True)
        self.assertNotEqual(notification.status, 'canceled')

    def test_errors_counter_shouldnt_be_reset_with_wrong_notification_id(self):
        url = '/admin/api/v2/notifications/9999/'
        response = self.client.patch(url, data={'is_visible': False})
        self.assertEqual(response.status_code, 404)
