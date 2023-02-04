# -*- coding: utf-8 -*-
from datetime import datetime


from bson.objectid import ObjectId
from django.test import TestCase
from django.utils.translation import ugettext as _
from unittest.mock import patch, Mock

from events.accounts.factories import UserFactory
from events.surveyme.factories import ProfileSurveyAnswerFactory
from events.surveyme_integration.factories import SurveyVariableFactory
from events.surveyme_integration.helpers import IntegrationTestMixin
from events.surveyme_integration.utils import (
    FormatError,
    decode_long_email,
    encode_long_email,
    encode_string,
    get_email_with_name,
    render_variable,
    render_variables,
    round_datetime_five_minutes_down,
)
from events.surveyme_integration.variables import FormQuestionAnswerVariable


class Test_render_variables(IntegrationTestMixin, TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        super().setUp()

        self.user = UserFactory()
        self.answer = ProfileSurveyAnswerFactory(
            user=self.user,
            survey=self.survey_hook.survey,
        )

    def test_render_variables_force_render(self):
        self.subscription.surveyvariable_set.clear()
        with self.assertRaises(FormatError):
            render_variables('{123abc}', self.subscription, self.answer, None, 1, force_render=False)

        result = render_variables('{123abc}', self.subscription, self.answer, None, 1, force_render=True)
        self.assertEqual(result, '{123abc}')

    def test_render_variables(self):
        result = render_variables(
            'Test {%s}' % self.variables_ids['form_name'],
            self.subscription,
            self.answer,
            None,
            1
        )
        self.assertEqual(result, 'Test %s' % self.survey_hook.survey.name)

    def test_render_variables_with_answer(self):
        variables = [
            SurveyVariableFactory(
                variable_id='57554dc3c67f552562000012',
                hook_subscription=self.subscription,
                var=FormQuestionAnswerVariable.name,
                arguments={'question': self.questions['some_text'].id},
            ),
            SurveyVariableFactory(
                variable_id='57554dc3c67f552562000022',
                hook_subscription=self.subscription,
                var=FormQuestionAnswerVariable.name,
                arguments={'question': self.questions['career'].id},
            ),
        ]
        self.answer.data = {
            'data': [
                {
                    'question': {
                        'id': self.questions['some_text'].pk,
                    },
                    'value': 'some text',
                },
                {
                    'question': {
                        'id': self.questions['career'].pk,
                    },
                    'value': [
                        {
                            'key': str(self.career_choices['working'].pk),
                            'text': self.career_choices['working'].label,
                        },
                    ],
                },
            ],
        }
        self.answer.save()

        result = render_variables(
            'some_text={%s}, career={%s}' % (variables[0].variable_id, variables[1].variable_id),
            self.subscription,
            self.answer,
            None,
            1
        )
        self.assertEqual(result, 'some_text=some text, career=working')

    def test_render_variables_with_empty_answer(self):
        variables = [
            SurveyVariableFactory(
                variable_id='57554dc3c67f552562000012',
                hook_subscription=self.subscription,
                var=FormQuestionAnswerVariable.name,
                arguments={'question': self.questions['some_text'].id},
            ),
            SurveyVariableFactory(
                variable_id='57554dc3c67f552562000022',
                hook_subscription=self.subscription,
                var=FormQuestionAnswerVariable.name,
                arguments={'question': self.questions['career'].id},
            ),
        ]

        result = render_variables(
            'some_text={%s}, career={%s}' % (variables[0].variable_id, variables[1].variable_id),
            self.subscription,
            self.answer,
            None,
            1
        )
        self.assertEqual(result, 'some_text=, career=')

    def test_render_variables_with_tags(self):
        result = render_variables(
            '{%% if 1 %%}\n{%(var)s}{{%(var)s}}{42}\n{%% endif %%}\n{%% debug %%}' % {
                'var': self.variables_ids['form_name'],
            },
            self.subscription, self.answer, None, 1, force_render=True
        )
        self.assertEqual(result, '{%% if 1 %%}\n%(var)s{%(var)s}{42}\n{%% endif %%}\n{%% debug %%}' % {
            'var': self.survey_hook.survey.name,
        })

    def test_render_bulk_variables(self):
        from events.surveyme_integration.variables import variables_by_name
        from events.surveyme_integration.variables.staff import StaffMetaUserVariable
        variables_by_name[StaffMetaUserVariable.name] = StaffMetaUserVariable
        with patch('events.surveyme_integration.variables.staff.get_staff_response') as mock_staff:
            mock_staff.return_value = {self.user.username: {'login': 'zomb-prj-124', 'personal': {'birthday': '2013-05-20'}}}
            result = render_variables(
                'Login: {%s} BirthDay: {%s}' % (self.variables_ids['staff_login'],
                                                self.variables_ids['staff_birthday']
                                                ),
                self.subscription,
                self.answer,
                None,
                1
            )
        self.assertEqual(result, 'Login: %s BirthDay: %s' % (self.variables_result_data['staff_login'],
                                                             self.variables_result_data['staff_birthday'],
                                                             ))

    def test_should_return_data_not_found_string_for_invalid_variable(self):
        var = SurveyVariableFactory(
            variable_id=str(ObjectId()),
            hook_subscription=self.subscription,
            var='invalid variable name'
        )
        value = f'_{{{var.variable_id}}}_'
        expected = '_' + _('Данные не найдены') + '_'
        self.assertEqual(render_variables(value, self.subscription, self.answer, None, 1), expected)

    def test_shouldnt_change_source_string_for_invalid_variable(self):
        variable_id = str(ObjectId())
        value = f'_{{{variable_id}}}_'
        expected = value
        self.assertEqual(render_variables(value, self.subscription, self.answer, None, 1, force_render=True), expected)

    def test_should_return_empty_string_if_exception_invoked(self):
        var = SurveyVariableFactory(
            variable_id=str(ObjectId()),
            hook_subscription=self.subscription,
            var=FormQuestionAnswerVariable.name,
            arguments={'question': self.questions['some_text'].id},
        )
        with patch.object(FormQuestionAnswerVariable, 'get_value', Mock(side_effect=ValueError('incorrect variable'))):
            self.assertEqual(render_variable(var, self.answer, None, 1), '')
            FormQuestionAnswerVariable.get_value.assert_called_once()

    def test_should_return_empty_string_if_question_was_none(self):
        var = SurveyVariableFactory(
            variable_id=str(ObjectId()),
            hook_subscription=self.subscription,
            var=FormQuestionAnswerVariable.name,
            arguments={'question': None},
        )
        with patch('events.surveyme_integration.variables.form.get_value_for_question') as mocked:
            self.assertEqual(render_variable(var, self.answer, None, 1, False), '')
        mocked.assert_not_called()


class TestEncodeLongEmail(TestCase):
    def test_get_email_with_name(self):
        self.assertEqual(get_email_with_name('"test" <a@c.com>'), ('a@c.com', 'test'))
        self.assertEqual(get_email_with_name('"тест" <a@c.com>'), ('a@c.com', 'тест'))
        self.assertEqual(get_email_with_name('"test"<a@c.com>'), ('a@c.com', 'test'))
        self.assertEqual(get_email_with_name('"тест"<a@c.com>'), ('a@c.com', 'тест'))
        self.assertEqual(get_email_with_name('test<a@c.com>'), ('a@c.com', 'test'))
        self.assertEqual(get_email_with_name('тест<a@c.com>'), ('a@c.com', 'тест'))
        self.assertEqual(get_email_with_name('test<a@c.com> b@c.com'), ('a@c.com', 'test'))
        self.assertEqual(get_email_with_name('тест<a@c.com> b@c.com'), ('a@c.com', 'тест'))
        self.assertEqual(get_email_with_name('a@c.com'), ('a@c.com', ''))

    def test_encode_long_email(self):
        self.assertEqual(decode_long_email(encode_long_email('"test"<a@c.com>')), ('a@c.com', '"test"'))
        self.assertEqual(decode_long_email(encode_long_email('"тест"<a@c.com>')), ('a@c.com', '"тест"'))
        self.assertEqual(decode_long_email(encode_long_email('a@c.com')), ('a@c.com', ''))

        with patch('base64.b64encode') as mock_b64encode:
            encode_long_email('"test" <a@c.com>')
        mock_b64encode.assert_called_once_with(b'"test"')

        with patch('base64.b64encode') as mock_b64encode:
            encode_long_email('"тест" <a@c.com>')
        mock_b64encode.assert_called_once_with('"тест"'.encode())

        with patch('base64.b64encode') as mock_b64encode:
            encode_long_email('a@c.com')
        mock_b64encode.assert_not_called()


class TestEncodeString(TestCase):
    def test_should_return_encoded_string(self):
        self.assertEqual(encode_string('Yandex.Forms'), '=?utf-8?B?WWFuZGV4LkZvcm1z?=')
        self.assertEqual(encode_string('Яндекс.Формы'), '=?utf-8?B?0K/QvdC00LXQutGBLtCk0L7RgNC80Ys=?=')

    def test_shouldnt_return_encoded_string(self):
        self.assertEqual(encode_string(''), '')
        self.assertIsNone(encode_string(None))


class TestRoundDownTime(TestCase):
    def test_round_down_time(self):
        datetime_fields = ['year', 'month', 'day', 'hour']
        datetime_obj = datetime(year=2022, month=5, day=20, hour=5, minute=13, second=5)
        new_datetime_obj = round_datetime_five_minutes_down(datetime_obj)
        for datetime_field in datetime_fields:
            self.assertEqual(getattr(datetime_obj, datetime_field), getattr(new_datetime_obj, datetime_field))
        self.assertEqual(new_datetime_obj.minute, 10)
        self.assertEqual(new_datetime_obj.second, 0)

        datetime_obj = datetime(year=2022, month=5, day=20, hour=5, minute=10, second=5)
        new_datetime_obj = round_datetime_five_minutes_down(datetime_obj)
        for datetime_field in datetime_fields:
            self.assertEqual(getattr(datetime_obj, datetime_field), getattr(new_datetime_obj, datetime_field))
        self.assertEqual(new_datetime_obj.minute, 10)
        self.assertEqual(new_datetime_obj.second, 0)

        datetime_obj = datetime(year=2022, month=5, day=20, hour=5, minute=53, second=5)
        new_datetime_obj = round_datetime_five_minutes_down(datetime_obj)
        for datetime_field in datetime_fields:
            self.assertEqual(getattr(datetime_obj, datetime_field), getattr(new_datetime_obj, datetime_field))
        self.assertEqual(new_datetime_obj.minute, 50)
        self.assertEqual(new_datetime_obj.second, 0)
