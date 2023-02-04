# -*- coding: utf-8 -*-
import json
import os
import responses

from django.conf import settings
from django.test import TestCase
from unittest.mock import patch

from events.surveyme.models import AnswerType
from events.accounts.factories import UserFactory, OrganizationFactory
from events.common_app.directory import DirectoryClient
from events.surveyme.factories import (
    ProfileSurveyAnswerFactory,
    SurveyQuestionFactory,
)
from events.surveyme_integration.variables import (
    DirectoryIsAdminVariable,
    DirectoryIsUserVariable,
    DirectoryOrgIdVariable,
    DirectorySubscriptionPlanVariable,
    DirectoryVIPVariable,
    DirectoryStaffMetaUserVariable,
    DirectoryStaffMetaQuestionVariable,
)
from events.arc_compat import read_asset


class Cassette:
    def __init__(self, *args, **kwargs):
        self.args = args
        self.kwargs = kwargs


class DirectoryVariableTestCase(TestCase):
    fixtures = ['initial_data.json']
    variable_class = None

    def setUp(self):
        self.user = UserFactory()
        self.answer = ProfileSurveyAnswerFactory(user=self.user)

    @responses.activate
    def get_result(self, cassettes=None, params=None):
        for cassette in cassettes or []:
            responses.add(*cassette.args, **cassette.kwargs)

        params = params or {}
        params['answer'] = self.answer

        var = self.variable_class(**params)
        return var.get_value()


class TestDirectoryIsAdminVariable(DirectoryVariableTestCase):
    variable_class = DirectoryIsAdminVariable

    def test_should_return_true_for_directory_admin(self):
        cassettes = [
            Cassette(
                responses.GET,
                'https://api-integration-qa.directory.ws.yandex.net/v6/organizations/',
                json={'links': {}, 'result': [{'id': 732}]},
            ),
            Cassette(
                responses.GET,
                f'https://api-integration-qa.directory.ws.yandex.net/v6/users/{self.user.uid}/',
                json={'id': self.user.uid, 'is_admin': '1'},
            ),
        ]
        self.assertEqual(self.get_result(cassettes=cassettes), '1')

    def test_should_return_false_for_yandex_team_user(self):
        cassettes = [
            Cassette(
                responses.GET,
                'https://api-integration-qa.directory.ws.yandex.net/v6/organizations/',
                json={'code': 'authentication-error'},
                status=401,
            ),
        ]
        self.assertEqual(self.get_result(cassettes=cassettes), '0')

    def test_should_return_false_for_not_directory_user(self):
        cassettes = [
            Cassette(
                responses.GET,
                'https://api-integration-qa.directory.ws.yandex.net/v6/organizations/',
                json={'links': {}, 'result': []},
            ),
        ]
        self.assertEqual(self.get_result(cassettes=cassettes), '0')

    def test_should_return_false_for_anonymous_user(self):
        self.user.uid = None
        self.user.save()

        cassettes = [
            Cassette(
                responses.GET,
                'https://api-integration-qa.directory.ws.yandex.net/v6/organizations/',
                json={'code': 'authentication-error'},
                status=401,
            ),
        ]
        self.assertEqual(self.get_result(cassettes=cassettes), '0')


class TestDirectoryIsUserVariable(DirectoryVariableTestCase):
    variable_class = DirectoryIsUserVariable

    def test_should_return_true_for_directory_admin(self):
        cassettes = [
            Cassette(
                responses.GET,
                'https://api-integration-qa.directory.ws.yandex.net/v6/organizations/',
                json={'links': {}, 'result': [{'id': 732}]},
            ),
        ]
        self.assertEqual(self.get_result(cassettes=cassettes), '1')

    def test_should_return_false_for_yandex_team_user(self):
        cassettes = [
            Cassette(
                responses.GET,
                'https://api-integration-qa.directory.ws.yandex.net/v6/organizations/',
                json={'code': 'authentication-error'},
                status=401,
            ),
        ]
        self.assertEqual(self.get_result(cassettes=cassettes), '0')

    def test_should_return_false_for_not_directory_user(self):
        cassettes = [
            Cassette(
                responses.GET,
                'https://api-integration-qa.directory.ws.yandex.net/v6/organizations/',
                json={'links': {}, 'result': []},
            ),
        ]
        self.assertEqual(self.get_result(cassettes=cassettes), '0')

    def test_should_return_false_for_anonymous_user(self):
        self.user.uid = None
        self.user.save()

        cassettes = [
            Cassette(
                responses.GET,
                'https://api-integration-qa.directory.ws.yandex.net/v6/organizations/',
                json={'code': 'authentication-error'},
                status=401,
            ),
        ]
        self.assertEqual(self.get_result(cassettes=cassettes), '0')


class TestDirectoryOrgIdVariable(DirectoryVariableTestCase):
    variable_class = DirectoryOrgIdVariable

    def test_should_return_org_id(self):
        cassettes = [
            Cassette(
                responses.GET,
                'https://api-integration-qa.directory.ws.yandex.net/v6/organizations/',
                json={'links': {}, 'result': [{'id': 732}]},
            ),
        ]
        self.assertEqual(self.get_result(cassettes=cassettes), '732')

    def test_should_return_none_for_yandex_team_user(self):
        cassettes = [
            Cassette(
                responses.GET,
                'https://api-integration-qa.directory.ws.yandex.net/v6/organizations/',
                json={'code': 'authentication-error'},
                status=401,
            ),
        ]
        self.assertIsNone(self.get_result(cassettes=cassettes))

    def test_should_return_none_for_not_directory_user(self):
        cassettes = [
            Cassette(
                responses.GET,
                'https://api-integration-qa.directory.ws.yandex.net/v6/organizations/',
                json={'links': {}, 'result': []},
            ),
        ]
        self.assertIsNone(self.get_result(cassettes=cassettes))

    def test_should_return_none_for_anonymous_user(self):
        self.user.uid = None
        self.user.save()

        cassettes = [
            Cassette(
                responses.GET,
                'https://api-integration-qa.directory.ws.yandex.net/v6/organizations/',
                json={'code': 'authentication-error'},
                status=401,
            ),
        ]
        self.assertIsNone(self.get_result(cassettes=cassettes))


class TestDirectorySubscriptionPlanVariable(DirectoryVariableTestCase):
    variable_class = DirectorySubscriptionPlanVariable

    def test_should_return_subscription_plan(self):
        cassettes = [
            Cassette(
                responses.GET,
                'https://api-integration-qa.directory.ws.yandex.net/v6/organizations/',
                json={'links': {}, 'result': [{'id': 732, 'subscription_plan': 'free'}]},
            ),
        ]
        self.assertEqual(self.get_result(cassettes=cassettes), 'free')

    def test_should_return_none_for_yandex_team_user(self):
        cassettes = [
            Cassette(
                responses.GET,
                'https://api-integration-qa.directory.ws.yandex.net/v6/organizations/',
                json={'code': 'authentication-error'},
                status=401,
            ),
        ]
        self.assertIsNone(self.get_result(cassettes=cassettes))

    def test_should_return_none_for_not_directory_user(self):
        cassettes = [
            Cassette(
                responses.GET,
                'https://api-integration-qa.directory.ws.yandex.net/v6/organizations/',
                json={'links': {}, 'result': []},
            ),
        ]
        self.assertIsNone(self.get_result(cassettes=cassettes))

    def test_should_return_none_for_anonymous_user(self):
        self.user.uid = None
        self.user.save()

        cassettes = [
            Cassette(
                responses.GET,
                'https://api-integration-qa.directory.ws.yandex.net/v6/organizations/',
                json={'code': 'authentication-error'},
                status=401,
            ),
        ]
        self.assertIsNone(self.get_result(cassettes=cassettes))


class TestDirectoryVIPVariable(DirectoryVariableTestCase):
    variable_class = DirectoryVIPVariable

    def setUp(self):
        super().setUp()
        self.question = SurveyQuestionFactory(
            survey=self.answer.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text')
        )

    def test_return_true_for_vip(self):
        self.answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': 'wikitest.yaconnect.com',
            }],
        }
        self.answer.save()

        cassettes = [
            Cassette(
                responses.GET,
                'https://api-integration-qa.directory.ws.yandex.net/v6/who-is/',
                json={'org_id': 732},
            ),
            Cassette(
                responses.GET,
                'https://api-integration-qa.directory.ws.yandex.net/v6/organizations/732/',
                json={'id': 732, 'vip': ['tracker', 'wiki']},
            ),
        ]
        params = {'answer': self.answer, 'question': self.question.id}
        self.assertTrue(self.get_result(cassettes=cassettes, params=params))

    def test_return_false_for_vip(self):
        self.answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': 'wikitest.yaconnect.com',
            }],
        }
        self.answer.save()

        cassettes = [
            Cassette(
                responses.GET,
                'https://api-integration-qa.directory.ws.yandex.net/v6/who-is/',
                json={'org_id': 732},
            ),
            Cassette(
                responses.GET,
                'https://api-integration-qa.directory.ws.yandex.net/v6/organizations/732/',
                json={'id': 732, 'vip': []},
            ),
        ]
        params = {'answer': self.answer, 'question': self.question.id}
        self.assertFalse(self.get_result(cassettes=cassettes, params=params))

    def test_return_undef_for_vip_on_error(self):
        self.answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': 'wikitest.yaconnect.com',
            }],
        }
        self.answer.save()

        cassettes = [
            Cassette(
                responses.GET,
                'https://api-integration-qa.directory.ws.yandex.net/v6/who-is/',
                json={'org_id': 732},
            ),
            Cassette(
                responses.GET,
                'https://api-integration-qa.directory.ws.yandex.net/v6/organizations/732/',
                json={'code': 'internal error'},
                status=500,
            ),
        ]
        params = {'answer': self.answer, 'question': self.question.id}
        self.assertEqual(self.get_result(cassettes=cassettes, params=params), 'Not defined')

    def test_return_undef_for_vip_on_invalid_entry(self):
        self.answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': 'helloworld',
            }],
        }
        self.answer.save()

        cassettes = [
            Cassette(
                responses.GET,
                'https://api-integration-qa.directory.ws.yandex.net/v6/who-is/',
                json={'org_id': 732},
            ),
            Cassette(
                responses.GET,
                'https://api-integration-qa.directory.ws.yandex.net/v6/organizations/732/',
                json={'code': 'not found'},
                status=404,
            ),
        ]
        params = {'answer': self.answer, 'question': self.question.id}
        self.assertEqual(self.get_result(cassettes=cassettes, params=params), 'Not defined')

    def test_return_undef_for_vip_on_blank(self):
        self.answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': '',
            }],
        }
        self.answer.save()

        cassettes = []
        params = {'answer': self.answer, 'question': self.question.id}
        self.assertEqual(self.get_result(cassettes=cassettes, params=params), 'Not defined')


class TestDirectoryStaffMetaUserVariable(DirectoryVariableTestCase):
    variable_class = DirectoryStaffMetaUserVariable

    def setUp(self):
        super().setUp()
        self.answer.user.uid = '1130000023386160'
        self.answer.survey.org = OrganizationFactory()
        self.question = SurveyQuestionFactory(
            survey=self.answer.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text')
        )
        self.answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': 'wikitest.yaconnect.com',
            }],
        }
        self.answer.save()
        self.user_1130000023386160 = self.load_fixture('user_1130000023386160.json')
        self.user_1130000023627135 = self.load_fixture('user_1130000023627135.json')
        self.department_1 = self.load_fixture('department_1.json')
        self.department_6 = self.load_fixture('department_6.json')

    def load_fixture(self, fixture_name):
        buf = read_asset(os.path.join(settings.FIXTURES_DIR, 'directory', fixture_name))
        return json.loads(buf)

    def test_birth_day(self):
        with patch.object(DirectoryClient, 'get_user', return_value=self.user_1130000023386160):
            var = self.variable_class(answer=self.answer)
            result = var.get_value(format_name='dir_staff.birth_day')
        self.assertEqual(result, '2001-01-01')

    def test_first_name(self):
        with patch.object(DirectoryClient, 'get_user', return_value=self.user_1130000023386160):
            var = self.variable_class(answer=self.answer)
            result = var.get_value(format_name='dir_staff.first_name')
        self.assertEqual(result, 'Wiki')

    def test_last_name(self):
        with patch.object(DirectoryClient, 'get_user', return_value=self.user_1130000023386160):
            var = self.variable_class(answer=self.answer)
            result = var.get_value(format_name='dir_staff.last_name')
        self.assertEqual(result, 'Test')

    def test_middle_name(self):
        with patch.object(DirectoryClient, 'get_user', return_value=self.user_1130000023386160):
            var = self.variable_class(answer=self.answer)
            result = var.get_value(format_name='dir_staff.middle_name')
        self.assertEqual(result, '')

    def test_phone(self):
        with patch.object(DirectoryClient, 'get_user', return_value=self.user_1130000023386160):
            var = self.variable_class(answer=self.answer)
            result = var.get_value(format_name='dir_staff.phone')
        self.assertEqual(result, '4959117813, 4951378911')

    def test_email(self):
        with patch.object(DirectoryClient, 'get_user', return_value=self.user_1130000023386160):
            var = self.variable_class(answer=self.answer)
            result = var.get_value(format_name='dir_staff.email')
        self.assertEqual(result, 'wiki@wikitest.yaconnect.com')

    def test_groups(self):
        with patch.object(DirectoryClient, 'get_user', return_value=self.user_1130000023386160):
            var = self.variable_class(answer=self.answer)
            result = var.get_value(format_name='dir_staff.groups')
        self.assertEqual(result, 'Команда КФ b2b, Отдел информационных исправлений')

    def test_department(self):
        with patch.object(DirectoryClient, 'get_user', return_value=self.user_1130000023386160):
            var = self.variable_class(answer=self.answer)
            result = var.get_value(format_name='dir_staff.department')
        self.assertEqual(result, 'Простой отдел №2')

    def test_manager(self):
        with (
            patch.object(DirectoryClient, 'get_user', return_value=self.user_1130000023386160),
            patch.object(DirectoryClient, 'get_department', return_value=self.department_6),
        ):
            var = self.variable_class(answer=self.answer)
            result = var.get_value(format_name='dir_staff.manager')
        self.assertEqual(result, 'Антон Чапоргин')

    def test_empty_manager(self):
        with (
            patch.object(DirectoryClient, 'get_user', return_value=self.user_1130000023627135),
            patch.object(DirectoryClient, 'get_department', return_value=self.department_1),
        ):
            var = self.variable_class(answer=self.answer)
            result = var.get_value(format_name='dir_staff.manager')
        self.assertEqual(result, '')


class TestDirectoryStaffMetaQuestionVariable(DirectoryVariableTestCase):
    variable_class = DirectoryStaffMetaQuestionVariable

    def setUp(self):
        super().setUp()
        self.answer.user.uid = '1130000023386160'
        self.answer.survey.org = OrganizationFactory()
        self.question = SurveyQuestionFactory(
            survey=self.answer.survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='dir_user',
        )
        self.answer.data = {
            'data': [
                {
                    'value': [{
                        'key': '1130000023627135',
                    }],
                    'question': {
                        'id': self.question.pk,
                        'options': {
                            'data_source': self.question.param_data_source,
                        },
                    },
                },
            ],
        }

        self.user_1130000023386160 = self.load_fixture('user_1130000023386160.json')
        self.user_1130000023627135 = self.load_fixture('user_1130000023627135.json')
        self.department_6 = self.load_fixture('department_6.json')

    def load_fixture(self, fixture_name):
        buf = read_asset(os.path.join(settings.FIXTURES_DIR, 'directory', fixture_name))
        return json.loads(buf)

    def test_birth_day(self):
        with patch.object(DirectoryClient, 'get_user', return_value=self.user_1130000023627135):
            var = self.variable_class(answer=self.answer, question=self.question.pk)
            result = var.get_value(format_name='dir_staff.birth_day')
        self.assertEqual(result, '2011-01-01')

    def test_first_name(self):
        with patch.object(DirectoryClient, 'get_user', return_value=self.user_1130000023627135):
            var = self.variable_class(answer=self.answer)
            result = var.get_value(format_name='dir_staff.first_name')
        self.assertEqual(result, 'Антон')

    def test_last_name(self):
        with patch.object(DirectoryClient, 'get_user', return_value=self.user_1130000023627135):
            var = self.variable_class(answer=self.answer)
            result = var.get_value(format_name='dir_staff.last_name')
        self.assertEqual(result, 'Чапоргин')
