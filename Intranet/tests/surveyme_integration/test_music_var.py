# -*- coding: utf-8 -*-

from django.test import TestCase
from unittest.mock import patch

from events.accounts.factories import UserFactory
from events.surveyme.factories import ProfileSurveyAnswerFactory
from events.surveyme_integration.variables.music import MusicStatusVariable


class TestMusicStatusVariable(TestCase):
    def setUp(self):
        user = UserFactory()
        self.answer = ProfileSurveyAnswerFactory(user=user)
        self.variable = MusicStatusVariable(answer=self.answer)

    def test_should_return_valid_status(self):
        self.assertEqual('not-mobile', self.variable.get_value())

    def test_for_anonymous_user_should_return_none(self):
        user = UserFactory(uid=None)
        self.answer.user = user
        self.assertTrue(self.answer.user.is_anonymous)
        self.assertEqual(None, self.variable.get_value())

    @patch('events.surveyme_integration.variables.music.MusicStatusClient')
    def test_exception_musnt_corrupt_get_value(self, MusicStatusClient):
        client = MusicStatusClient.return_value
        client.get_status.side_effect = ValueError()
        self.assertIsNone(self.variable.get_value())
