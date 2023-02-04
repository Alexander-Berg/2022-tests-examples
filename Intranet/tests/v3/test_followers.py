# -*- coding: utf-8 -*-
from django.test import TestCase

from events.accounts.helpers import YandexClient
from events.followme.models import ContentFollower
from events.surveyme.factories import SurveyFactory
from events.v3.types import UnsubscribeStatusType


class TestFollowers(TestCase):
    client_class = YandexClient

    def setUp(self) -> None:
        self.user = self.client.login_yandex()
        self.survey = SurveyFactory()
        self.user.follow(self.survey)

    def test_unsubscribe(self):
        self.assertEqual(len(self.survey.followers), 1)
        secret_code = ContentFollower.objects.get(
            type='user',
            object_id=self.survey.id,
            user=self.user,
        ).secret_code
        response = self.client.post(f'/v3/followers/unsubscribe/{secret_code}/')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['survey_name'], self.survey.name)
        self.assertEqual(response.json()['status'], UnsubscribeStatusType.unsubscribed)
        self.assertEqual(len(self.survey.followers), 0)

        response = self.client.post(f'/v3/followers/unsubscribe/{secret_code}/')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['status'], UnsubscribeStatusType.not_subscribed)
        self.assertEqual(len(self.survey.followers), 0)

    def test_bad_secret_code(self):
        self.assertEqual(len(self.survey.followers), 1)
        secret_code = 'abcde'
        response = self.client.post(f'/v3/followers/unsubscribe/{secret_code}/')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['status'], UnsubscribeStatusType.not_subscribed)
        self.assertEqual(len(self.survey.followers), 1)
