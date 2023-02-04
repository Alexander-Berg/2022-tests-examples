# -*- coding: utf-8 -*-
from django.test import TestCase

from events.accounts.factories import UserFactory
from events.surveyme.factories import ProfileSurveyAnswerFactory
from events.surveyme_integration.variables.kinopoisk import (
    KinopoiskProfileTxtRenderer,
    KinopoiskEmailVariable,
    KinopoiskIsBetaUserVariable,
    KinopoiskUserIdVariable,
    KinopoiskMigrationStatusVariable,
)
from events.common_app.kinopoisk.mock_client import KINOPOISK_DATA


class TestKinopoiskProfileTxtRenderer(TestCase):
    def test_must_render_data_with_new_lines(self):
        renderer = KinopoiskProfileTxtRenderer()
        data = [{'title': 'test', 'value': 'data'}, {'title': 'test2', 'value': 'data2'}]
        rendered_text = renderer.render(data)
        exp_text = 'test: data\ntest2: data2'
        self.assertEqual(rendered_text, exp_text)

    def test_render_with_empty_data(self):
        renderer = KinopoiskProfileTxtRenderer()
        data = []
        rendered_text = renderer.render(data)
        exp_text = ''
        self.assertEqual(rendered_text, exp_text)


class KinopoiskVariableTestBase(object):
    what_to_get = None
    variable = None

    def setUp(self):
        user = UserFactory()
        self.answer = ProfileSurveyAnswerFactory(user=user)
        self.variable = self.variable(answer=self.answer)

    def test_for_anonymous_user_must_return_none(self):
        user = UserFactory(uid=None)
        self.answer.user = user
        self.assertTrue(self.answer.user.is_anonymous)
        self.assertEqual(self.variable.get_value(), None)

    def test_auth_user_must_return_full_kinopoisk_profile(self):
        KINOPOISK_DATA.profile_response = {
            'betaUser': True,
            'email': 'email@yandex-team.ru',
            'migrationStatus': 'SKIPPED',
            'userId': 1,
        }

        exp_value = KINOPOISK_DATA.profile_response.get(self.what_to_get)
        self.assertEqual(self.variable.get_value(), exp_value)


class TestKinopoiskEmailVariable(KinopoiskVariableTestBase, TestCase):
    what_to_get = 'email'
    variable = KinopoiskEmailVariable


class TestKinopoiskIsBetaUserVariable(KinopoiskVariableTestBase, TestCase):
    what_to_get = 'betaUser'
    variable = KinopoiskIsBetaUserVariable


class TestKinopoiskUserIdVariable(KinopoiskVariableTestBase, TestCase):
    what_to_get = 'userId'
    variable = KinopoiskUserIdVariable


class TestKinopoiskMigrationStatusVariable(KinopoiskVariableTestBase, TestCase):
    what_to_get = 'migrationStatus'
    variable = KinopoiskMigrationStatusVariable
