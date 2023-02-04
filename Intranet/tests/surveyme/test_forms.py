# -*- coding: utf-8 -*-
import json
import responses
import os

from dateutil import parser
from django.conf import settings
from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import TestCase
from django.utils import timezone
from django.utils.datastructures import MultiValueDict
from django.utils.encoding import force_str
from django.utils.translation import override, activate
from unittest.mock import patch, Mock

from events.accounts.helpers import YandexClient
from events.accounts.factories import UserFactory
from events.captcha.fields import CaptchaField
from events.common_app.utils import get_query_dict
from events.media.factories import ImageFactory
from events.surveyme.factories import (
    SurveyFactory,
    SurveyQuestionFactory,
    SurveyQuestionShowConditionNodeItemFactory,
    SurveyQuestionChoiceFactory,
    SurveyQuestionShowConditionNodeFactory,
    ProfileSurveyAnswerFactory,
)
from events.surveyme.forms import (
    SurveyForm,
    GroupWidget,
)
from events.surveyme.models import (
    AnswerType,
    SurveyQuestionShowConditionNode,
    SurveyAgreement,
    SurveyTicket,
    ProfileSurveyAnswer,
)
from events.conditions.factories import ContentTypeAttributeFactory
from events.common_storages.factories import ProxyStorageModelFactory
from events.common_storages.proxy_storages import ProxyStorage
from events.balance.factories import TicketFactory
from events.balance import forms as balance_forms
from events.common_app import widgets as common_app_widgets
from events.surveyme.exceptions import MaxSurveyAnswersException
from events.geobase_contrib.factories import CountryFactory
from events.arc_compat import read_asset


def get_answer_data(response):
    survey_answer = ProfileSurveyAnswer.objects.get(pk=response.data['answer_id'])
    return survey_answer.as_dict()


class TestSurveyFormBehavior_additional_fields(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()
        SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_choices')
        )

    def test_default(self):
        fields = SurveyForm(survey=self.survey).fields
        self.assertEqual(list(fields.keys()), ['answer_choices_1', 'captcha'])

    def test_should_be_without_additional_fields_if_asked_do_not_show_them(self):
        fields = SurveyForm(survey=self.survey, is_with_additional_fields=False).fields
        self.assertEqual(list(fields.keys()), ['answer_choices_1'])

    def test_should_add_captcha_field_by_default(self):
        survey = SurveyFactory()
        field = SurveyForm(survey=survey, is_with_captcha=True).fields.get('captcha')
        self.assertEqual(type(field), CaptchaField)
        self.assertTrue(field.required)

    def test_should_not_add_captcha_field_if_its_aked_not_to_do_it(self):
        survey = SurveyFactory()
        field = SurveyForm(survey=survey, is_with_captcha=False).fields.get('captcha')
        self.assertIsNone(field)


class TestSurveyFormBehavior_make_all_fields_not_required(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()
        SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_choices')
        )

    def test_should_be_with_required_questions_by_default(self):
        fields = SurveyForm(survey=self.survey, is_with_additional_fields=False).fields
        self.assertTrue(all([i.required for i in list(fields.values())]))

    def test_should_be_with_all_questions_not_required_if_asked(self):
        fields = SurveyForm(survey=self.survey, make_all_fields_not_required=True, is_with_additional_fields=False).fields
        self.assertEqual({i.required for i in list(fields.values())}, {False})


class TestSurveyFormBehavior_question_fields(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()
        self.survey.agreements.set(SurveyAgreement.objects.filter(slug__in=['events', 'hr']))

    def test_should_add_fields_from_questions(self):
        expected_field_names = []
        # create questions of all answer types
        for answer_type_slug in ['answer_short_text', 'answer_long_text', 'answer_short_text']:
            question = SurveyQuestionFactory(
                survey=self.survey,
                answer_type=AnswerType.objects.get(slug=answer_type_slug)
            )
            expected_field_names.append(question.get_form_field_name())
        # without additional fields
        fields = SurveyForm(survey=self.survey, is_with_additional_fields=False).fields
        self.assertEqual(list(fields.keys()), expected_field_names)

        # with additional fields
        fields = SurveyForm(survey=self.survey, is_with_additional_fields=True).fields
        expected_field_names.extend(['is_agree_with_events', 'is_agree_with_hr', 'captcha'])
        self.assertEqual(list(fields.keys()), expected_field_names)


class TestSurveyFormBehavior_agreement_fields(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()
        self.agreement = SurveyAgreement.objects.create(slug='test_agreement')
        self.survey.agreements.clear()
        self.survey.agreements.add(self.agreement)

    def test_should_add_agreements_fields(self):
        expected_field_names = []
        fields = SurveyForm(survey=self.survey, is_with_additional_fields=False).fields
        self.assertEqual(list(fields.keys()), expected_field_names)

        # with additional fields
        fields = SurveyForm(survey=self.survey, is_with_additional_fields=True).fields
        expected_field_names.extend([
            'is_agree_with_%s' % self.agreement.slug,
            'captcha'
        ])
        self.assertEqual(list(fields.keys()), expected_field_names)


class TestSurveyFormBehavior_field_conditions(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()
        answer_choices_type = AnswerType.objects.get(slug='answer_choices')
        answer_date = AnswerType.objects.get(slug='answer_date')
        self.questions = [
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=answer_choices_type,
                param_is_allow_multiple_choice=True,
                param_is_required=True,
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=answer_choices_type,
                param_is_allow_multiple_choice=True,
                param_is_required=True,
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=answer_choices_type,
                param_is_allow_multiple_choice=True,
                param_is_required=True,
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=answer_date,
                param_date_field_type='daterange',
                param_is_required=True,
            ),
        ]
        # create question choices for first question
        SurveyQuestionChoiceFactory(survey_question=self.questions[0], label='1')
        SurveyQuestionChoiceFactory(survey_question=self.questions[0], label='2')

        # create question choices for second question
        SurveyQuestionChoiceFactory(survey_question=self.questions[1], label='3')
        SurveyQuestionChoiceFactory(survey_question=self.questions[1], label='4')

        # create question choices for third question
        SurveyQuestionChoiceFactory(survey_question=self.questions[2], label='5')
        SurveyQuestionChoiceFactory(survey_question=self.questions[2], label='6')

        # create condition node for two last questions
        self.node_1 = SurveyQuestionShowConditionNodeFactory(survey_question=self.questions[1])
        self.node_2 = SurveyQuestionShowConditionNodeFactory(survey_question=self.questions[2])

        self.content_type_attribute = ContentTypeAttributeFactory(
            title='test',
            attr='answer_short_text',
        )

        # create conditions for second question
        SurveyQuestionShowConditionNodeItemFactory(
            position=1,
            survey_question_show_condition_node=self.node_1,
            operator='and',
            condition='eq',
            content_type_attribute=self.content_type_attribute,
            survey_question=self.questions[0],
            value=self.questions[0].surveyquestionchoice_set.all()[0],
        )
        SurveyQuestionShowConditionNodeItemFactory(
            position=2,
            survey_question_show_condition_node=self.node_1,
            operator='and',
            condition='eq',
            content_type_attribute=self.content_type_attribute,
            survey_question=self.questions[0],
            value=self.questions[0].surveyquestionchoice_set.all()[1],
        )

        # create conditions for third question
        SurveyQuestionShowConditionNodeItemFactory(
            position=1,
            survey_question_show_condition_node=self.node_2,
            operator='and',
            condition='eq',
            content_type_attribute=self.content_type_attribute,
            survey_question=self.questions[1],
            value=self.questions[1].surveyquestionchoice_set.all()[0],
        )
        SurveyQuestionShowConditionNodeItemFactory(
            position=2,
            survey_question_show_condition_node=self.node_2,
            operator='and',
            condition='neq',
            content_type_attribute=self.content_type_attribute,
            survey_question=self.questions[1],
            value=self.questions[1].surveyquestionchoice_set.all()[1],
        )

        date_start_content_type_attribute = ContentTypeAttributeFactory(
            title='test datetime',
            attr='answer_date.date_start',
        )
        date_end_content_type_attribute = ContentTypeAttributeFactory(
            title='test datetime',
            attr='answer_date.date_end',
        )

        self.fourth_question_exp_date = '2015-01-01'
        SurveyQuestionShowConditionNodeItemFactory(
            position=3,
            survey_question_show_condition_node=self.node_2,
            operator='and',
            condition='eq',
            content_type_attribute=date_start_content_type_attribute,
            survey_question=self.questions[3],
            value=self.fourth_question_exp_date,
        )
        SurveyQuestionShowConditionNodeItemFactory(
            position=4,
            survey_question_show_condition_node=self.node_2,
            operator='or',
            condition='gt',
            content_type_attribute=date_end_content_type_attribute,
            survey_question=self.questions[3],
            value=self.fourth_question_exp_date,
        )

    def test_should_add_conditions_info_to_field_other_data(self):
        fields = SurveyForm(survey=self.survey).fields
        self.assertFalse('show_conditions' in fields[self.questions[0].get_form_field_name()].other_data)

        expected = [
            [
                {
                    'operator': 'and',
                    'condition': 'eq',
                    'field': force_str(self.questions[0].get_form_field_name()),
                    'field_value': force_str(self.questions[0].surveyquestionchoice_set.all()[0].id)
                },
                {
                    'operator': 'and',
                    'condition': 'eq',
                    'field': force_str(self.questions[0].get_form_field_name()),
                    'field_value': force_str(self.questions[0].surveyquestionchoice_set.all()[1].id)
                },
            ]
        ]
        self.assertTrue('show_conditions' in fields[self.questions[1].get_form_field_name()].other_data)
        self.assertEqual(fields[self.questions[1].get_form_field_name()].other_data['show_conditions'], expected)

        expected = [
            [
                {
                    'operator': 'and',
                    'condition': 'eq',
                    'field': force_str(self.questions[1].get_form_field_name()),
                    'field_value': force_str(self.questions[1].surveyquestionchoice_set.all()[0].id)
                },
                {
                    'operator': 'and',
                    'condition': 'neq',
                    'field': force_str(self.questions[1].get_form_field_name()),
                    'field_value': force_str(self.questions[1].surveyquestionchoice_set.all()[1].id)
                },
                {
                    'condition': 'eq',
                    'field': force_str(self.questions[3].get_form_field_name()),
                    'field_value': self.fourth_question_exp_date,
                    'operator': 'and',
                    'tag_index': 0,
                },
                {
                    'condition': 'gt',
                    'field': force_str(self.questions[3].get_form_field_name()),
                    'field_value': self.fourth_question_exp_date,
                    'operator': 'or',
                    'tag_index': 1,
                },
            ]
        ]
        self.assertTrue('show_conditions' in fields[self.questions[2].get_form_field_name()].other_data)
        self.assertEqual(fields[self.questions[2].get_form_field_name()].other_data['show_conditions'], expected)

    def test_validation_without_conditions(self):
        SurveyQuestionShowConditionNode.objects.all().delete()
        form = SurveyForm(survey=self.survey, data={}, is_with_additional_fields=False)
        with override('en'):
            form.is_valid()

            expected = {
                self.questions[0].get_form_field_name(): ['This field is required.'],
                self.questions[1].get_form_field_name(): ['This field is required.'],
                self.questions[2].get_form_field_name(): ['This field is required.'],
                self.questions[3].get_form_field_name(): ['This field is required.'],
            }
            msg = 'поля без conditions должны уважать стандартную процедуру валидации на required'
            self.assertEqual(dict(form.errors), expected, msg=msg)

    def test_validation_without_condition_and_field_is_shown_but_without_data(self):
        SurveyQuestionShowConditionNode.objects.all().delete()
        form = SurveyForm(survey=self.survey, data={}, is_with_additional_fields=False)
        self.assertFalse(form.is_valid())

        msg = 'поля без conditions должны уважать стандартную процедуру валидации на required'
        self.assertTrue(self.questions[0].get_form_field_name() in form.errors, msg=msg)
        self.assertTrue(self.questions[1].get_form_field_name() in form.errors, msg=msg)
        self.assertTrue(self.questions[2].get_form_field_name() in form.errors, msg=msg)
        self.assertTrue(self.questions[3].get_form_field_name() in form.errors, msg=msg)

    def test_validation_with_condition_and_field_is_shown_and_with_data(self):
        data = get_query_dict({
            self.questions[0].get_form_field_name(): [
                self.questions[0].surveyquestionchoice_set.all()[0],
                self.questions[0].surveyquestionchoice_set.all()[1]
            ],
            self.questions[1].get_form_field_name(): [
                self.questions[1].surveyquestionchoice_set.all()[0],
            ],
            '%s_0' % self.questions[3].get_form_field_name(): ['2014-01-01'],
            '%s_1' % self.questions[3].get_form_field_name(): ['2014-01-02'],
        })
        form = SurveyForm(survey=self.survey, data=data, is_with_additional_fields=False)
        self.assertTrue(form.is_valid())

        msg = 'Т.к. поле показано по условию и ему задано значение, то валидация на required должна проходить'
        self.assertFalse(self.questions[1].get_form_field_name() in form.errors, msg=msg)
        self.assertFalse(self.questions[3].get_form_field_name() in form.errors, msg=msg)

    def test_validation_with_condition_and_field_is_shown_but_without_data(self):
        data = get_query_dict({
            self.questions[0].get_form_field_name(): [
                self.questions[0].surveyquestionchoice_set.all()[0],
                self.questions[0].surveyquestionchoice_set.all()[1]
            ]
        })
        form = SurveyForm(survey=self.survey, data=data, is_with_additional_fields=False)
        self.assertFalse(form.is_valid())

        msg = 'Т.к. поле показано по условию, но ему не задано значение, то валидация на required не должна проходить'
        self.assertTrue(self.questions[1].get_form_field_name() in form.errors, msg=msg)

    def test_validation_with_condition_and_field_is_shown_but_without_data_and_answer_date_condition(self):
        data = get_query_dict({
            self.questions[0].get_form_field_name(): [
                self.questions[0].surveyquestionchoice_set.all()[0],
                self.questions[0].surveyquestionchoice_set.all()[1]
            ],
            '%s_0' % self.questions[3].get_form_field_name(): ['2014-01-01'],
            '%s_1' % self.questions[3].get_form_field_name(): ['2016-01-02'],
        })
        form = SurveyForm(survey=self.survey, data=data, is_with_additional_fields=False)
        self.assertFalse(form.is_valid())

        msg = 'Т.к. поле показано по условию, но ему не задано значение, то валидация на required не должна проходить'
        self.assertTrue(self.questions[1].get_form_field_name() in form.errors, msg=msg)
        self.assertTrue(self.questions[2].get_form_field_name() in form.errors, msg=msg)

    def test_validation_with_condition_and_field_is_not_shown_and_without_data(self):
        data = get_query_dict({
            self.questions[0].get_form_field_name(): [
                self.questions[0].surveyquestionchoice_set.all()[0]
            ],
            '%s_0' % self.questions[3].get_form_field_name(): ['2014-01-01'],
            '%s_1' % self.questions[3].get_form_field_name(): ['2014-01-02'],
        })
        form = SurveyForm(survey=self.survey, data=data, is_with_additional_fields=False)
        msg = ('Т.к. поле не показано по условию, то даже при непереданном значении валидация '
               'на required не должна срабатывать. Так же и другое поле, которое зависит от текущего по condition, '
               'не должно быть показано')
        self.assertTrue(form.is_valid(), msg=msg)


class TestSurveyFormBehavior_save_fields_with_conditions(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()
        answer_choices_type = AnswerType.objects.get(slug='answer_choices')
        self.answer_param_name_type = AnswerType.objects.get(slug='answer_short_text')
        answer_date = AnswerType.objects.get(slug='answer_date')
        self.questions = [
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=answer_choices_type,
                param_is_allow_multiple_choice=True,
                param_is_required=False,
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=answer_choices_type,
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=self.answer_param_name_type,
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=answer_date,
                param_date_field_type='daterange',
                param_is_required=False,
            ),
        ]
        # create question choices for first question
        SurveyQuestionChoiceFactory(survey_question=self.questions[0], label='1')
        SurveyQuestionChoiceFactory(survey_question=self.questions[0], label='2')

        # create question choices for second question
        SurveyQuestionChoiceFactory(survey_question=self.questions[1], label='3')
        SurveyQuestionChoiceFactory(survey_question=self.questions[1], label='4')

        # create condition node for two last questions
        self.node_1 = SurveyQuestionShowConditionNodeFactory(survey_question=self.questions[1])
        self.node_2 = SurveyQuestionShowConditionNodeFactory(survey_question=self.questions[2])

        self.content_type_attribute = ContentTypeAttributeFactory(
            title='test',
            attr='answer_short_text',
        )

        # create conditions for second question
        SurveyQuestionShowConditionNodeItemFactory(
            position=1,
            survey_question_show_condition_node=self.node_1,
            operator='and',
            condition='eq',
            content_type_attribute=self.content_type_attribute,
            survey_question=self.questions[0],
            value=self.questions[0].surveyquestionchoice_set.all()[0],
        )

        # create conditions for third question
        SurveyQuestionShowConditionNodeItemFactory(
            position=1,
            survey_question_show_condition_node=self.node_2,
            operator='and',
            condition='eq',
            content_type_attribute=self.content_type_attribute,
            survey_question=self.questions[0],
            value=self.questions[0].surveyquestionchoice_set.all()[1],
        )

        date_start_content_type_attribute = ContentTypeAttributeFactory(
            title='test datetime',
            attr='answer_date.date_start',
        )
        date_end_content_type_attribute = ContentTypeAttributeFactory(
            title='test datetime',
            attr='answer_date.date_end',
        )

        self.exp_date = '2015-01-01'
        SurveyQuestionShowConditionNodeItemFactory(
            position=2,
            survey_question_show_condition_node=self.node_2,
            operator='or',
            condition='eq',
            content_type_attribute=date_start_content_type_attribute,
            survey_question=self.questions[3],
            value=self.exp_date,
        )
        SurveyQuestionShowConditionNodeItemFactory(
            position=3,
            survey_question_show_condition_node=self.node_2,
            operator='or',
            condition='gt',
            content_type_attribute=date_end_content_type_attribute,
            survey_question=self.questions[3],
            value=self.exp_date,
        )

        self.user = UserFactory(uid=None)

    def get_param_answers_and_answer_choices(self, profile_survey_answer):
        results = {}
        answer = profile_survey_answer.as_dict()
        for param_slug, answer_question in answer.items():
            question_id = answer_question.get('question', {}).get('id')
            slug = answer_question.get('question', {}).get('answer_type', {}).get('slug')
            if slug == 'answer_choices':
                results[question_id] = [
                    item.get('key')
                    for item in answer_question.get('value')
                ]
            else:
                results[question_id] = answer_question.get('value')
        return results

    def test_save_with_not_shown_fields_by_date_range_condition(self):
        data = get_query_dict({
            # задаем такой диапазон дат, чтобы не показать третий вопрос
            '%s_0' % self.questions[3].get_form_field_name(): ['2014-01-01'],
            '%s_1' % self.questions[3].get_form_field_name(): ['2014-01-02'],
        })
        form = SurveyForm(
            survey=self.survey,
            instance=self.user,
            data=data,
            is_with_additional_fields=False
        )
        is_valid = form.is_valid()
        self.assertTrue(is_valid, msg=form.errors)

        response = form.save()
        profile_survey_answer = response['profile_survey_answer']

        results = self.get_param_answers_and_answer_choices(profile_survey_answer)
        msg = ('У профиля имя не задано. т.к. '
               'поле имени профиля не показано по условию, то это значение не должно сохраниться в сущность профиля')
        self.assertFalse(self.questions[2].pk in results, msg)

    def test_save_with_shown_fields_by_date_range_condition(self):
        data = get_query_dict({
            # задаем такой диапазон дат, чтобы показать третий вопрос
            '%s_0' % self.questions[3].get_form_field_name(): ['2014-01-01'],
            '%s_1' % self.questions[3].get_form_field_name(): ['2017-01-02'],
            self.questions[2].get_form_field_name(): ['Alexander'],
        })
        form = SurveyForm(
            survey=self.survey,
            instance=self.user,
            data=data,
            is_with_additional_fields=False
        )
        is_valid = form.is_valid()
        self.assertTrue(is_valid, msg=form.errors)

        response = form.save()
        profile_survey_answer = response['profile_survey_answer']

        results = self.get_param_answers_and_answer_choices(profile_survey_answer)
        self.assertEqual(results[self.questions[2].pk], 'Alexander')

    def test_save_with_all_shown_fields(self):
        data = get_query_dict({
            # задаем первому вопросу такие значения, чтобы показались оба следующих поля
            self.questions[0].get_form_field_name(): [
                self.questions[0].surveyquestionchoice_set.all()[0],
                self.questions[0].surveyquestionchoice_set.all()[1]
            ],
            self.questions[1].get_form_field_name(): [
                self.questions[1].surveyquestionchoice_set.all()[0]
            ],
            self.questions[2].get_form_field_name(): [
                'Gena'
            ],
        })
        form = SurveyForm(
            survey=self.survey,
            instance=self.user,
            data=data,
            is_with_additional_fields=False
        )
        is_valid = form.is_valid()
        self.assertTrue(is_valid, msg=form.errors)

        response = form.save()
        profile_survey_answer = response['profile_survey_answer']

        results = self.get_param_answers_and_answer_choices(profile_survey_answer)
        choices = self.questions[0].surveyquestionchoice_set.values_list('pk', flat=True)
        self.assertListEqual(
            results[self.questions[0].pk],
            [str(choices[0]), str(choices[1])],
        )
        choices = self.questions[1].surveyquestionchoice_set.values_list('pk', flat=True)
        self.assertListEqual(
            results[self.questions[1].pk],
            [str(choices[0])],
        )
        self.assertEqual(results[self.questions[2].pk], 'Gena')

    def test_save_with_profile_field_not_shown__and_with_name_data__and_profile_without_name(self):
        data = get_query_dict({
            # задаем первому вопросу такие значения, чтобы показались только второе (generic) поле
            self.questions[0].get_form_field_name(): [
                self.questions[0].surveyquestionchoice_set.all()[0]
            ],
            self.questions[1].get_form_field_name(): [
                self.questions[1].surveyquestionchoice_set.all()[0]
            ],
            self.questions[2].get_form_field_name(): [
                'Gena'
            ],
        })
        form = SurveyForm(
            survey=self.survey,
            instance=self.user,
            data=data,
            is_with_additional_fields=False
        )
        self.assertTrue(form.is_valid())

        response = form.save()
        profile_survey_answer = response['profile_survey_answer']

        results = self.get_param_answers_and_answer_choices(profile_survey_answer)

        choices = self.questions[0].surveyquestionchoice_set.values_list('pk', flat=True)
        self.assertListEqual(
            results[self.questions[0].pk],
            [str(choices[0])],
        )
        choices = self.questions[1].surveyquestionchoice_set.values_list('pk', flat=True)
        self.assertListEqual(
            results[self.questions[1].pk],
            [str(choices[0])],
        )
        msg = ('У профиля имя не задано. Имя задано в форме. Но т.к. '
               'поле имени профиля не показано по условию, то это значение не должно сохраниться в сущность профиля')
        self.assertFalse(self.questions[2].pk in results, msg)

    def test_save_with_profile_field_not_shown__and_with_name_data__and_profile_without_name_with_daterange_condition(self):
        data = get_query_dict({
            self.questions[2].get_form_field_name(): ['Alexander'],
            # задаем такой диапазон дат, чтобы не показать третий вопрос
            '%s_0' % self.questions[3].get_form_field_name(): ['2010-01-01'],
            '%s_1' % self.questions[3].get_form_field_name(): ['2010-01-02'],
        })
        form = SurveyForm(
            survey=self.survey,
            instance=self.user,
            data=data,
            is_with_additional_fields=False
        )
        is_valid = form.is_valid()
        self.assertTrue(is_valid, msg=form.errors)

        response = form.save()
        profile_survey_answer = response['profile_survey_answer']

        results = self.get_param_answers_and_answer_choices(profile_survey_answer)
        self.assertFalse(self.questions[2].pk in results)

    def test_save_with_profile_field_not_shown__and_without_name_data__and_profile_with_name(self):
        data = get_query_dict({
            # задаем первому вопросу такие значения, чтобы показались только второе (generic) поле
            self.questions[0].get_form_field_name(): [
                self.questions[0].surveyquestionchoice_set.all()[0]
            ],
            self.questions[1].get_form_field_name(): [
                self.questions[1].surveyquestionchoice_set.all()[0]
            ],
        })
        form = SurveyForm(
            survey=self.survey,
            instance=self.user,
            data=data,
            is_with_additional_fields=False
        )
        self.assertTrue(form.is_valid())

        response = form.save()
        profile_survey_answer = response['profile_survey_answer']

        results = self.get_param_answers_and_answer_choices(profile_survey_answer)
        choices = self.questions[0].surveyquestionchoice_set.values_list('pk', flat=True)
        self.assertListEqual(
            results[self.questions[0].pk],
            [str(choices[0])],
        )
        choices = self.questions[1].surveyquestionchoice_set.values_list('pk', flat=True)
        self.assertListEqual(
            results[self.questions[1].pk],
            [str(choices[0])],
        )
        self.assertFalse(self.questions[2].pk in results)

    def test_save_with_generic_field_not_shown__and_with_choice_data__and_without_existing_answer(self):
        data = get_query_dict({
            # задаем первому вопросу такое значение, чтобы не показывалось второе generic поле
            self.questions[0].get_form_field_name(): [
                self.questions[0].surveyquestionchoice_set.all()[1]
            ],
            self.questions[1].get_form_field_name(): [
                self.questions[1].surveyquestionchoice_set.all()[0]
            ],
            self.questions[2].get_form_field_name(): [
                'Gena'
            ],
        })
        form = SurveyForm(
            survey=self.survey,
            instance=self.user,
            data=data,
            is_with_additional_fields=False
        )
        self.assertTrue(form.is_valid())

        response = form.save()
        profile_survey_answer = response['profile_survey_answer']

        results = self.get_param_answers_and_answer_choices(profile_survey_answer)
        choices = self.questions[0].surveyquestionchoice_set.values_list('pk', flat=True)
        self.assertListEqual(
            results[self.questions[0].pk],
            [str(choices[1])],
        )
        self.assertEqual(results[self.questions[2].pk], 'Gena')
        self.assertFalse(self.questions[1].pk in results)

    def test_save_with_generic_field_not_shown__and_without_choice_data__and_with_existing_answer(self):
        # создадим существующий ответ на второй вопрос
        choice = self.questions[1].surveyquestionchoice_set.all()[0]
        ProfileSurveyAnswerFactory(
            survey=self.survey,
            data={
                'data': [{
                    'question': self.questions[1].get_answer_info(),
                    'value': [{
                        'key': str(choice.pk),
                        'slug': choice.slug,
                        'text': choice.label,
                    }],
                }],
            },
        )

        data = get_query_dict({
            # задаем первому вопросу такое значение, чтобы не показывалось второе generic поле
            self.questions[0].get_form_field_name(): [
                self.questions[0].surveyquestionchoice_set.all()[1]
            ],
            self.questions[2].get_form_field_name(): [
                'Gena'
            ],
        })
        form = SurveyForm(
            survey=self.survey,
            instance=self.user,
            data=data,
            is_with_additional_fields=False
        )
        self.assertTrue(form.is_valid())

        response = form.save()
        profile_survey_answer = response['profile_survey_answer']

        results = self.get_param_answers_and_answer_choices(profile_survey_answer)
        choices = self.questions[0].surveyquestionchoice_set.values_list('pk', flat=True)
        self.assertListEqual(
            results[self.questions[0].pk],
            [str(choices[1])],
        )
        self.assertIsNone(results.get(self.questions[1].pk))

    def test_save_with_generic_field_not_shown__and_with_empty_choice_data__and_with_existing_answer(self):
        # создадим существующий ответ на второй вопрос
        choice = self.questions[1].surveyquestionchoice_set.all()[0]
        ProfileSurveyAnswerFactory(
            survey=self.survey,
            data={
                'data': [{
                    'question': self.questions[1].get_answer_info(),
                    'value': [{
                        'key': str(choice.pk),
                        'slug': choice.slug,
                        'text': choice.label,
                    }],
                }],
            },
        )

        data = get_query_dict({
            # задаем первому вопросу такое значение, чтобы не показывалось второе generic поле
            self.questions[0].get_form_field_name(): [
                self.questions[0].surveyquestionchoice_set.all()[1]
            ],
            self.questions[1].get_form_field_name(): [],
            self.questions[2].get_form_field_name(): [
                'Gena'
            ],
        })
        form = SurveyForm(
            survey=self.survey,
            instance=self.user,
            data=data,
            is_with_additional_fields=False
        )
        self.assertTrue(form.is_valid())

        response = form.save()
        profile_survey_answer = response['profile_survey_answer']

        results = self.get_param_answers_and_answer_choices(profile_survey_answer)
        choices = self.questions[0].surveyquestionchoice_set.values_list('pk', flat=True)
        self.assertListEqual(
            results[self.questions[0].pk],
            [str(choices[1])],
        )
        self.assertIsNone(results.get(self.questions[1].pk))

    def test_save_with_generic_field_not_shown__and_with_choice_data__and_with_existing_answer(self):
        # создадим существующий ответ на второй вопрос
        choice = self.questions[1].surveyquestionchoice_set.all()[0]
        ProfileSurveyAnswerFactory(
            survey=self.survey,
            data={
                'data': [{
                    'question': self.questions[1].get_answer_info(),
                    'value': [{
                        'key': str(choice.pk),
                        'slug': choice.slug,
                        'text': choice.label,
                    }],
                }],
            },
        )

        data = get_query_dict({
            # задаем первому вопросу такое значение, чтобы не показывалось второе generic поле
            self.questions[0].get_form_field_name(): [
                self.questions[0].surveyquestionchoice_set.all()[1]
            ],
            self.questions[1].get_form_field_name(): [
                self.questions[1].surveyquestionchoice_set.all()[1]
            ],
            self.questions[2].get_form_field_name(): [
                'Gena'
            ],
        })
        form = SurveyForm(
            survey=self.survey,
            instance=self.user,
            data=data,
            is_with_additional_fields=False
        )
        self.assertTrue(form.is_valid())

        response = form.save()
        profile_survey_answer = response['profile_survey_answer']

        results = self.get_param_answers_and_answer_choices(profile_survey_answer)
        choices = self.questions[0].surveyquestionchoice_set.values_list('pk', flat=True)
        self.assertListEqual(
            results[self.questions[0].pk],
            [str(choices[1])],
        )
        self.assertIsNone(results.get(self.questions[1].pk))

    def test_save_with_generic_field_not_shown__and_with_invalid_choice_data__and_with_existing_answer(self):
        # создадим существующий ответ на второй вопрос
        choice = self.questions[1].surveyquestionchoice_set.all()[0]
        ProfileSurveyAnswerFactory(
            survey=self.survey,
            data={
                'data': [{
                    'question': self.questions[1].get_answer_info(),
                    'value': [{
                        'key': str(choice.pk),
                        'slug': choice.slug,
                        'text': choice.label,
                    }],
                }],
            },
        )

        data = get_query_dict({
            # задаем первому вопросу такое значение, чтобы не показывалось второе generic поле
            self.questions[0].get_form_field_name(): [
                self.questions[0].surveyquestionchoice_set.all()[1]
            ],
            self.questions[1].get_form_field_name(): [
                'hello'  # задаем невалидное значение
            ],
            self.questions[2].get_form_field_name(): [
                'Gena'
            ],
        })
        form = SurveyForm(
            survey=self.survey,
            instance=self.user,
            data=data,
            is_with_additional_fields=False
        )
        self.assertTrue(form.is_valid())

        response = form.save()
        profile_survey_answer = response['profile_survey_answer']

        results = self.get_param_answers_and_answer_choices(profile_survey_answer)
        choices = self.questions[0].surveyquestionchoice_set.values_list('pk', flat=True)
        self.assertListEqual(
            results[self.questions[0].pk],
            [str(choices[1])],
        )
        self.assertEqual(results[self.questions[2].pk], 'Gena')
        self.assertIsNone(results.get(self.questions[1].pk))


patched_balance_client = Mock()
patched_balance_client.get_service_product = Mock(return_value={})


@patch('events.balance.models.balance_utils.get_balance_client_for_currency', Mock(return_value=patched_balance_client))
class TestSurveyFormBehavior_count_of_ticket_fields(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        activate('ru')
        self.survey = SurveyFactory()
        self.ticket = TicketFactory(survey=self.survey, price=1000)
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_count_of_tickets')
        )

    def test_default(self):
        fields = SurveyForm(survey=self.survey).fields
        self.assertTrue('answer_count_of_tickets_%s' % self.question.pk in list(fields.keys()))

    def test_should_be_invalid_if_requested_gt_qty_than_param_max(self):
        data = get_query_dict({self.question.get_form_field_name(): [10]})
        self.question.param_max = 8
        self.question.save()
        form = SurveyForm(survey=self.survey, data=data, is_with_additional_fields=False)

        msg = 'Т.к. передано значение больше, чем param_max - валидация не должна проходить'
        self.assertFalse(form.is_valid(), msg=msg)
        self.assertTrue(self.question.get_form_field_name() in form.errors, msg=msg)
        exp_error_message = ['Убедитесь, что это значение меньше либо равно 8.']
        self.assertEqual(form.errors[self.question.get_form_field_name()], exp_error_message)

    def test_should_be_invalid_if_requested_lt_qty_than_param_min(self):
        data = get_query_dict({self.question.get_form_field_name(): [2]})
        self.question.param_max = 10
        self.question.param_min = 5
        self.question.save()
        form = SurveyForm(survey=self.survey, data=data, is_with_additional_fields=False)

        msg = 'Т.к. передано значение меньше, чем param_min - валидация не должна проходить'
        self.assertFalse(form.is_valid(), msg=msg)
        self.assertTrue(self.question.get_form_field_name() in form.errors, msg=msg)
        exp_error_message = ['Убедитесь, что это значение больше либо равно 5.']
        self.assertEqual(form.errors[self.question.get_form_field_name()], exp_error_message)

    def test_should_be_valid(self):
        data = get_query_dict({self.question.get_form_field_name(): [7]})
        self.question.param_max = 10
        self.question.param_min = 5
        self.question.save()
        form = SurveyForm(survey=self.survey, data=data, is_with_additional_fields=False)

        msg = 'Т.к. передано param_min < значение < param_max, чем param_min - валидация должна проходить'
        self.assertTrue(form.is_valid(), msg=msg)

    def test_answer_count_question_widget_params(self):
        self.question.param_price = 5000
        self.question.param_max = 2
        self.question.param_min = 1
        self.question.save()
        data = get_query_dict({self.question.get_form_field_name(): [1]})
        with patch.object(self.survey, 'get_tickets_status', Mock(return_value={'available_quantity': 5})):
            form = SurveyForm(survey=self.survey, data=data, is_with_additional_fields=False)
        self.assertTrue(isinstance(form.fields[self.question.get_form_field_name()].widget, balance_forms.TicketsQuantityInput))
        exp_attrs = {
            'price': 5000,
            'max': 2,
            'min': 1,
            'currency': 'RUB',
        }
        msg = 'В параметры виджета должны записаться цена, макс. и мин. количество'
        self.assertEqual(form.fields[self.question.get_form_field_name()].widget.attrs, exp_attrs, msg=msg)

    def test_ticket_params_should_be_used_as_widget_params_if_question_params_is_not_setted(self):
        self.ticket.price = 9000
        with patch('events.balance.signals.balance_utils.get_balance_client_for_currency', Mock(return_value=patched_balance_client)):
            self.ticket.save()
        data = get_query_dict({self.question.get_form_field_name(): [1]})
        form = SurveyForm(survey=self.survey, data=data, is_with_additional_fields=False)
        self.assertTrue(isinstance(form.fields[self.question.get_form_field_name()].widget, balance_forms.TicketsQuantityInput))
        exp_attrs = {
            'price': 9000,
            'max': 2147483647,
            'min': 1,
            'currency': 'RUB',
        }
        self.assertEqual(form.fields[self.question.get_form_field_name()].widget.attrs, exp_attrs)

    def test_available_quantity_should_be_used_as_param_max_if_it_is_lt_than_param_max(self):
        self.question.param_max = 5
        self.question.param_min = 1
        self.question.save()
        data = get_query_dict({self.question.get_form_field_name(): [1]})
        with patch.object(self.survey, 'get_tickets_status', Mock(return_value={'available_quantity': 3})):
            form = SurveyForm(survey=self.survey, data=data, is_with_additional_fields=False)
        exp_attrs = {
            'price': 1000,
            'max': 3,
            'min': 1,
            'currency': 'RUB',
        }
        msg = 'Если оставшихся билетов меньше, чем param_max - в max должно вернуться оставшееся число'
        self.assertEqual(form.fields[self.question.get_form_field_name()].widget.attrs, exp_attrs, msg=msg)


class TestSurveyFormBehavior_answer_number(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        activate('ru')
        self.survey = SurveyFactory()
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_number')
        )

    def test_default(self):
        fields = SurveyForm(survey=self.survey).fields
        self.assertTrue('answer_number_%s' % self.question.pk in list(fields.keys()))

    def test_should_be_invalid_if_requested_gt_qty_than_param_max(self):
        data = get_query_dict({self.question.get_form_field_name(): [10]})
        self.question.param_max = 8
        self.question.save()
        form = SurveyForm(survey=self.survey, data=data, is_with_additional_fields=False)

        msg = 'Т.к. передано значение больше, чем param_max - валидация не должна проходить'
        self.assertFalse(form.is_valid(), msg=msg)
        self.assertTrue(self.question.get_form_field_name() in form.errors, msg=msg)
        exp_error_message = ['Убедитесь, что это значение меньше либо равно 8.']
        self.assertEqual(form.errors[self.question.get_form_field_name()], exp_error_message)

    def test_should_be_invalid_if_requested_gt_qty_than_allowed_max(self):
        data = get_query_dict({self.question.get_form_field_name(): [settings.MAX_POSTGRESQL_INT_VALUE + 1]})
        self.question.param_max = None
        self.question.save()
        form = SurveyForm(survey=self.survey, data=data, is_with_additional_fields=False)

        msg = 'Т.к. передано значение больше, чем param_max - валидация не должна проходить'
        self.assertFalse(form.is_valid(), msg=msg)
        self.assertTrue(self.question.get_form_field_name() in form.errors, msg=msg)
        exp_error_message = ['Убедитесь, что это значение меньше либо равно {}.'.format(settings.MAX_POSTGRESQL_INT_VALUE)]
        self.assertEqual(form.errors[self.question.get_form_field_name()], exp_error_message)

    def test_should_be_invalid_if_requested_lt_qty_than_param_min(self):
        data = get_query_dict({self.question.get_form_field_name(): [2]})
        self.question.param_max = 10
        self.question.param_min = 5
        self.question.save()
        form = SurveyForm(survey=self.survey, data=data, is_with_additional_fields=False)

        msg = 'Т.к. передано значение меньше, чем param_min - валидация не должна проходить'
        self.assertFalse(form.is_valid(), msg=msg)
        self.assertTrue(self.question.get_form_field_name() in form.errors, msg=msg)
        exp_error_message = ['Убедитесь, что это значение больше либо равно 5.']
        self.assertEqual(form.errors[self.question.get_form_field_name()], exp_error_message)

    def test_should_be_valid(self):
        data = get_query_dict({self.question.get_form_field_name(): [7]})
        self.question.param_max = 10
        self.question.param_min = 5
        self.question.save()
        form = SurveyForm(survey=self.survey, data=data, is_with_additional_fields=False)

        msg = 'Т.к. передано param_min < значение < param_max, чем param_min - валидация должна проходить'
        self.assertTrue(form.is_valid(), msg=msg)

    def test_answer_number_widget_params(self):
        self.question.param_max = 2
        self.question.param_min = 1
        self.question.save()
        data = get_query_dict({self.question.get_form_field_name(): [1]})
        with patch.object(self.survey, 'get_tickets_status', Mock(return_value={'available_quantity': 5})):
            form = SurveyForm(survey=self.survey, data=data, is_with_additional_fields=False)
        self.assertTrue(isinstance(form.fields[self.question.get_form_field_name()].widget, common_app_widgets.NumberInput))
        exp_attrs = {
            'max': 2,
            'min': 1,
        }
        msg = 'В параметры виджета должны записаться макс. и мин. количество'
        self.assertEqual(form.fields[self.question.get_form_field_name()].widget.attrs, exp_attrs, msg=msg)


class TestSurveyFormBehavior_hidden_fields(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()
        answer_choices_type = AnswerType.objects.get(slug='answer_choices')
        answer_param_name_type = AnswerType.objects.get(slug='answer_short_text')
        answer_param_subscribed_email_type = AnswerType.objects.get(slug='answer_short_text')
        answer_answer_long_text_type = AnswerType.objects.get(slug='answer_long_text')

        self.questions = [
            SurveyQuestionFactory(
                survey=self.survey,
                param_is_hidden=False,
                answer_type=answer_choices_type
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                param_is_hidden=False,
                answer_type=answer_param_name_type
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                param_is_hidden=False,
                answer_type=answer_param_subscribed_email_type
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                param_is_hidden=False,
                answer_type=answer_answer_long_text_type
            ),
        ]
        # create question choices for first question
        SurveyQuestionChoiceFactory(survey_question=self.questions[0], label='1')
        SurveyQuestionChoiceFactory(survey_question=self.questions[0], label='2')

    def test_by_default_all_questions_should_not_be_hidden(self):
        form = SurveyForm(survey=self.survey, data={}, files={}, is_with_additional_fields=False)
        msg = 'Поле не должно быть скрытым'
        for field in form.fields:
            self.assertFalse(form.fields[field].widget.is_hidden, msg=msg)

    def test_all_questions_should_be_hidden(self):
        for question in self.questions:
            question.param_is_hidden = True
            question.save()
        form = SurveyForm(survey=self.survey, data={}, files={}, is_with_additional_fields=False)
        msg = 'Поле должно быть скрытым'
        for field in form.fields:
            self.assertTrue(form.fields[field].widget.is_hidden, msg=msg)

    def test_with_one_hidden_field(self):
        self.questions[0].param_is_hidden = True
        self.questions[0].save()
        form = SurveyForm(survey=self.survey, data={}, files={}, is_with_additional_fields=False)
        for question in self.questions:
            self.assertEqual(form.fields[question.get_form_field_name()].widget.is_hidden, question.param_is_hidden)


class TestSurveyForm__choices_group_fields(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory(is_allow_answer_editing=True)
        self.group_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_group')
        )
        self.country_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='country',
            group=self.group_question,
        )
        self.city_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='city',
            group=self.group_question,
            param_data_source_params={
                'filters': [{
                    'filter': {
                        'data_source': 'country',
                        'name': 'country',
                    },
                    'field': self.country_question.id,
                    'type': 'field_value',
                }]
            }
        )

        self.user = UserFactory(uid=None)

    def test_should_add_group_identifier_for_data_source_filters(self):

        form = SurveyForm(
            instance=self.user,
            survey=self.survey,
            is_with_additional_fields=False
        )
        form_as_dict_fields = form.as_dict()['fields']
        city_question_form = form_as_dict_fields['{}__0'.format(self.city_question.get_form_field_name())]
        self.assertEqual(
            city_question_form['data_source']['filters'][0]['field'],
            '{}__0'.format(self.country_question.get_form_field_name()),
        )

    def test_should_add_identifier_for_couple_added_groups(self):
        data = get_query_dict(
            {'{}__0'.format(self.city_question.get_form_field_name()): ['1'],
             '{}__1'.format(self.city_question.get_form_field_name()): ['2342'],
             }
        )
        form = SurveyForm(
            instance=self.user,
            data=data,
            survey=self.survey,
            is_with_additional_fields=False
        )
        form_as_dict_fields = form.as_dict()['fields']
        city_question_form = form_as_dict_fields['{}__1'.format(self.city_question.get_form_field_name())]
        self.assertEqual(
            city_question_form['data_source']['filters'][0]['field'],
            '{}__1'.format(self.country_question.get_form_field_name()),
        )
        city_question_form = form_as_dict_fields['{}__0'.format(self.city_question.get_form_field_name())]
        self.assertEqual(
            city_question_form['data_source']['filters'][0]['field'],
            '{}__0'.format(self.country_question.get_form_field_name()),
        )

    def test_should_not_add_identifier_if_question_not_in_group(self):
        self.country_question.group_id = None
        self.country_question.save()

        form = SurveyForm(
            instance=self.user,
            survey=self.survey,
            is_with_additional_fields=False
        )
        form_as_dict_fields = form.as_dict()['fields']
        city_question_form = form_as_dict_fields['{}__0'.format(self.city_question.get_form_field_name())]
        self.assertEqual(
            city_question_form['data_source']['filters'][0]['field'],
            self.country_question.get_form_field_name(),
        )


class TestSurveyForm__choices_field_filtering_behaviour(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_is_allow_multiple_choice=True,
            param_data_source='survey_question_choice'
        )
        self.question.param_data_source_params = {
            'filters': [
                {
                    'type': 'specified_value',
                    'filter': {
                        'name': 'question'
                    },
                    'value': str(self.question.id)
                }
            ]
        }
        self.question.save()

        self.choices = {
            'one': SurveyQuestionChoiceFactory(label='one', survey_question=self.question),
            'two': SurveyQuestionChoiceFactory(label='two', survey_question=self.question),
            'three': SurveyQuestionChoiceFactory(label='three', survey_question=self.question),
        }
        [c.save() for c in self.choices.values()]  # для установки slug по сигналу
        self.choice_from_other_question = SurveyQuestionChoiceFactory(label='other')

    def test_should_apply_specified_filters_for_not_paginated_data_sources(self):
        form = SurveyForm(survey=self.survey, is_with_additional_fields=False)
        response = form.as_dict()['fields'].get(self.question.get_form_field_name())
        self.assertEqual(len(response['data_source'].get('items')), 3)
        expected = [
            {
                'text': i.label, 'id': str(i.id),
                'slug': i.slug, 'label_image': None,
            }
            for i in self.choices.values()
        ]
        self.assertEqual(
            {frozenset(value.items()) for value in response['data_source'].get('items')},
            {frozenset(value.items()) for value in expected},
        )

    def test_should_filter_for_multiple_items(self):
        self.countries = {
            'russia': CountryFactory(
                name='Россия',
                full_name='Россия, Евразия',
                translations={
                    'name': {
                        'ru': 'Россия',
                        'en': 'Russia',
                    },
                    'full_name': {
                        'ru': 'Россия, Евразия',
                        'en': 'Russia, Eurasia',
                    },
                },
            ),
            'usa': CountryFactory(
                name='США',
                full_name='США, Северная Америка',
                translations={
                    'name': {
                        'ru': 'США',
                        'en': 'USA',
                    },
                    'full_name': {
                        'ru': 'США, Северная Америка',
                        'en': 'USA, North America',
                    },
                },
            ),
            'china': CountryFactory(
                name='Китай',
                full_name='Китай, Азия',
                translations={
                    'name': {
                        'ru': 'Китай',
                        'en': 'China',
                    },
                    'full_name': {
                        'ru': 'Китай, Азия',
                        'en': 'China, Asia',
                    },
                },
            ),
        }

        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_is_allow_multiple_choice=True,
            param_data_source='country'
        )
        self.question.param_data_source_params = {
            'filters': [
                {
                    'type': 'specified_value',
                    'filter': {
                        'name': 'id'
                    },
                    'value': str(country.id)
                }
                for country in self.countries.values()
            ]
        }
        self.question.save()

        form = SurveyForm(survey=self.survey, is_with_additional_fields=False)
        field = form.fields[self.question.get_form_field_name()]
        self.assertEqual(
            field.filters,
            {'id': [str(country.id) for country in self.countries.values()]}
        )
        cleaned_data = field.clean([str(self.countries['usa'].id)])
        self.assertEqual(len(cleaned_data), 1)
        self.assertEqual(cleaned_data[0].get_text(), self.countries['usa'].full_name)

    def test_should_apply_specified_filters_for_not_paginated_data_sources__and_raise_error_if_value_not_in_set(self):
        # test with all good choices
        data = get_query_dict({
            self.question.get_form_field_name(): [i.id for i in self.choices.values()]
        })
        form = SurveyForm(survey=self.survey, data=data, is_with_additional_fields=False)
        self.assertTrue(form.is_valid())

        # test with one choice from another question
        data = get_query_dict({
            self.question.get_form_field_name(): [
                i.id for i in
                list(self.choices.values()) + [self.choice_from_other_question]
            ]
        })
        form = SurveyForm(survey=self.survey, data=data, is_with_additional_fields=False)
        self.assertFalse(form.is_valid())


def mocked_open_function(self, path):
    file_path = os.path.join(os.path.dirname(os.path.normpath(__file__)), path)
    file = read_asset(file_path)
    return SimpleUploadedFile(file.name, file.read())


@patch.object(ProxyStorage, 'open', mocked_open_function)
@patch.object(ProxyStorage, 'delete', Mock())
class TestSurveyFormBehaviour_save_multiple_files_in_answer_files(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory(is_allow_answer_editing=True)

        self.files = [
            {'path': 'yandex_ru.png', 'sha256': 'yandex_ru_sha256', 'original_name': 'yandex_ru.png', 'file_size': 9247},
            {'path': 'yandex.png', 'sha256': 'yandex_sha256', 'original_name': 'yandex.png', 'file_size': 7029},
            {'path': 'two_mb.jpg', 'sha256': 'two_mb_sha256', 'original_name': 'two_mb.jpg', 'file_size': 2128532},
            {'path': 'one_mb.jpg', 'sha256': 'one_mb_sha256', 'original_name': 'one_mb.jpg', 'file_size': 1024*1024 + 1},
        ]
        for file_metadata in self.files:
            ProxyStorageModelFactory(survey_id=self.survey.pk, **file_metadata)
        self.answer_files_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_files'),
            param_is_required=False,
            param_max_file_size=10,
            param_max_files_count=10,
        )
        self.short_text_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            param_is_required=False
        )
        self.user = UserFactory()

    def get_form(self, data=None):
        with override(language='ru'):
            return SurveyForm(
                survey=self.survey,
                instance=self.user,
                data=data,
                files=None,
                is_with_additional_fields=False
            )

    def save_form_with_file(self, upload_file_id):
        return self.save_form_with_many_files([upload_file_id])

    def save_form_with_many_files(self, upload_file_ids):
        data = MultiValueDict({
            self.answer_files_question.get_form_field_name(): upload_file_ids
        })
        form = self.get_form(data=data)
        self.assertTrue(form.is_valid())
        response = form.save()
        return response

    def test_should_be_valid_with_file(self):
        data = MultiValueDict({
            self.answer_files_question.get_form_field_name(): [self.files[0]['sha256']]
        })
        form = SurveyForm(
            survey=self.survey,
            instance=self.user,
            data=data,
            is_with_additional_fields=False
        )
        self.assertTrue(form.is_valid())

    def test_save(self):
        response = self.save_form_with_file(self.files[0]['sha256'])
        profile_survey_answer = response['profile_survey_answer']
        answer = profile_survey_answer.as_dict()
        question_answers = answer.get(self.answer_files_question.pk)
        self.assertIsNotNone(question_answers)
        self.assertListEqual([item.get('path') for item in question_answers.get('value')], [self.files[0]['path']])

    def test_resave(self):
        response = self.save_form_with_file(self.files[0]['sha256'])
        profile_survey_answer = response['profile_survey_answer']
        answer = profile_survey_answer.as_dict()
        question_answers = answer.get(self.answer_files_question.pk)
        self.assertIsNotNone(question_answers)
        self.assertListEqual([item.get('path') for item in question_answers.get('value')], [self.files[0]['path']])

        # resave with another file
        response = self.save_form_with_file(self.files[1]['sha256'])
        profile_survey_answer = response['profile_survey_answer']
        answer = profile_survey_answer.as_dict()
        question_answers = answer.get(self.answer_files_question.pk)
        self.assertIsNotNone(question_answers)
        self.assertListEqual([item.get('path') for item in question_answers.get('value')], [self.files[1]['path']])

    def test_save_form_with_existing_file_without_file_data(self):
        self.save_form_with_file(self.files[0]['sha256'])
        data = MultiValueDict({
            self.short_text_question.get_form_field_name(): ['answer']
        })

        form = self.get_form(data=data)
        self.assertTrue(form.is_valid())
        response = form.save()

        profile_survey_answer = response['profile_survey_answer']
        answer = profile_survey_answer.as_dict()
        question_answers = answer.get(self.answer_files_question.pk)
        self.assertIsNone(question_answers)

    def test_max_file_size__exceed(self):
        self.answer_files_question.param_max_file_size = 1
        self.answer_files_question.save()
        data = MultiValueDict({self.answer_files_question.get_form_field_name(): [self.files[3]['sha256']]})
        form = self.get_form(data=data)
        msg = (
            'на поле файла стоит ограничение в 1 мегабайт. Если пытаются грузить файл > 1 мегабайта, '
            'нужно показывать ошибку'
        )
        self.assertFalse(form.is_valid(), msg=msg)
        expected_error = 'Превышен максимальный размер файлов'
        self.assertEqual(form.errors[self.answer_files_question.get_form_field_name()][0], expected_error)

    def test_max_file_size__not_exceed(self):
        self.answer_files_question.param_max_file_size = 3
        self.answer_files_question.save()
        data = MultiValueDict({self.answer_files_question.get_form_field_name(): [self.files[2]['sha256']]})
        form = self.get_form(data=data)
        msg = (
            'на поле файла стоит ограничение в 3 мегабайта. Если пытаются грузить файл в 2 мегабайта, '
            'не нужно показывать ошибку'
        )
        self.assertTrue(form.is_valid(), msg=msg)

    def test_other_data_max_file_size(self):
        self.answer_files_question.param_max_file_size = 10
        self.answer_files_question.save()
        form = self.get_form()
        self.assertTrue(hasattr(form.fields['answer_files_1'], 'other_data'))
        msg = (
            'в other_data файлового поля должно записаться значение max_file_size '
            'с максимальным допустимым размером файлов'
        )
        self.assertEqual(form.fields[self.answer_files_question.get_form_field_name()].other_data.get('max_file_size'), 10, msg=msg)

    def test_max_files_count__exceed(self):
        self.answer_files_question.param_max_file_size = 10
        self.answer_files_question.param_max_files_count = 1
        self.answer_files_question.save()
        data = MultiValueDict({self.answer_files_question.get_form_field_name(): [
            self.files[2]['sha256'],
            self.files[0]['sha256'],
            self.files[1]['sha256'],
        ]})
        form = self.get_form(data=data)
        msg = (
            'Указано максимальное количество файлов - 1 '
            'Если пытаются грузить больше чем 1 файл - нужно показывать ошибку'
        )
        self.assertFalse(form.is_valid(), msg=msg)
        expected_error = 'Превышено максимальное количество файлов'
        self.assertEqual(form.errors[self.answer_files_question.param_slug][0], expected_error)

    def test_max_files_count__equals(self):
        self.answer_files_question.param_max_file_size = 10
        self.answer_files_question.param_max_files_count = 3
        self.answer_files_question.save()
        data = MultiValueDict({self.answer_files_question.get_form_field_name(): [
            self.files[2]['sha256'],
            self.files[0]['sha256'],
            self.files[1]['sha256'],
        ]})
        form = self.get_form(data=data)
        msg = (
            'Указано максимальное количество файлов - 3 '
            'Если пытаются грузить 3 файла или меньше - не нужно показывать ошибку'
        )
        self.assertTrue(form.is_valid(), msg=msg)

    def test_max_files_count__not_exceed(self):
        self.answer_files_question.param_max_file_size = 10
        self.answer_files_question.param_max_files_count = 3
        self.answer_files_question.save()
        data = MultiValueDict({self.answer_files_question.get_form_field_name(): [
            self.files[2]['sha256'],
            self.files[0]['sha256'],
        ]})
        form = self.get_form(data=data)
        msg = (
            'Указано максимальное количество файлов - 3 '
            'Если пытаются грузить 3 файла или меньше - не нужно показывать ошибку'
        )
        self.assertTrue(form.is_valid(), msg=msg)

    def test_other_data_max_files_count(self):
        self.answer_files_question.param_max_file_size = 10
        self.answer_files_question.param_max_files_count = 5
        self.answer_files_question.save()
        form = self.get_form()
        self.assertTrue(hasattr(form.fields[self.answer_files_question.get_form_field_name()], 'other_data'))
        msg = (
            'в other_data файлового поля должно записаться значение max_files_count '
            'с максимальным допустимым количеством файлов'
        )
        self.assertEqual(form.fields[self.answer_files_question.get_form_field_name()].other_data.get('max_files_count'), 5, msg=msg)

    def test_should_link_correct_file(self):
        surveys = [
            SurveyFactory(), SurveyFactory(),
        ]
        answer_files = AnswerType.objects.get(slug='answer_files')
        questions = [
            SurveyQuestionFactory(survey=surveys[0], answer_type=answer_files),
            SurveyQuestionFactory(survey=surveys[1], answer_type=answer_files),
        ]
        sha256 = '123456'
        meta = [
            ProxyStorageModelFactory(path='/101/readme.txt', sha256=sha256, file_size=10, survey=surveys[0]),
            ProxyStorageModelFactory(path='/102/readme.txt', sha256=sha256, file_size=10, survey=surveys[1]),
            ProxyStorageModelFactory(path='/103/readme.txt', sha256=sha256, file_size=10, survey=surveys[1]),
        ]

        data = {questions[0].param_slug: sha256}
        response = self.client.post(f'/v1/surveys/{surveys[0].pk}/form/', data)
        self.assertEqual(response.status_code, 200)
        answer = get_answer_data(response)
        self.assertEqual(answer[questions[0].pk]['value'][0]['path'], meta[0].path)

        data = {questions[1].param_slug: sha256}
        response = self.client.post(f'/v1/surveys/{surveys[1].pk}/form/', data)
        self.assertEqual(response.status_code, 200)
        answer = get_answer_data(response)
        self.assertEqual(answer[questions[1].pk]['value'][0]['path'], meta[2].path)


class TestSurveyFormBehavior_get_fresh_locked_survey_ticket(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()
        self.form = SurveyForm(survey=self.survey, is_with_additional_fields=False)

    def test_must_return_none_if_there_is_no_tickets(self):
        SurveyTicket.objects.all().delete()

        msg = 'Если нет свободных билетов - нужно вернуть None'
        self.assertIsNone(self.form._get_fresh_locked_survey_ticket(), msg=msg)

        SurveyTicket.objects.create(survey=self.survey, acquired=True)
        self.assertIsNone(self.form._get_fresh_locked_survey_ticket(), msg=msg)

    def test_must_return_ticket_if_free_ticket_exists(self):
        SurveyTicket.objects.all().delete()
        ticket = SurveyTicket.objects.create(survey=self.survey)
        locked_ticket = self.form._get_fresh_locked_survey_ticket()

        msg = 'Если есть свободный билет - нужно его вернуть'
        self.assertIsNotNone(locked_ticket, msg=msg)
        self.assertEqual(locked_ticket.pk, ticket.pk, msg=msg)

        msg = 'Вновь заблокированному билету должен установиться статус acquired=True'
        self.assertTrue(locked_ticket.acquired, msg=msg)


class TestSurveyFormBehavior_increment_answers_count(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory(is_published_external=True)
        self.form = SurveyForm(survey=self.survey, is_with_additional_fields=False)

    def test_must_not_do_anything_if_max_answers_count_is_none(self):
        SurveyTicket.objects.all().delete()

        self.survey.maximum_answers_count = None
        self.survey.save()

        self.form._get_fresh_locked_survey_ticket = Mock()
        self.form._increment_answers_count()

        msg = 'Если maximum_answers_count is None, то не нужно блокировать билет'
        self.assertEqual(self.form._get_fresh_locked_survey_ticket.call_count, 0, msg=msg)

    def test_must_lock_ticket_if_max_answers_count_is_not_none(self):
        SurveyTicket.objects.all().delete()

        self.survey.maximum_answers_count = 1
        self.survey.save()

        self.form._get_fresh_locked_survey_ticket = Mock()
        self.form._increment_answers_count()

        msg = 'Если maximum_answers_count is not None, то нужно блокировать билет'
        self.assertEqual(self.form._get_fresh_locked_survey_ticket.call_count, 1, msg=msg)

    def test_must_unpublish_survey_if_tickets_ended(self):
        self.survey.maximum_answers_count = 1
        self.survey.save()
        self.form._get_fresh_locked_survey_ticket = Mock(return_value=None)

        self.assertTrue(self.survey.is_published_external)

        try:
            self.form._increment_answers_count()
            raised = False
        except MaxSurveyAnswersException:
            raised = True

        msg = (
            'Если не удалось заблокировать билет, то нужно разопубликовать форму '
            'и вызвать исключение MaxSurveyAnswersException'
        )
        self.assertTrue(raised, msg=msg)
        self.assertEqual(self.form._get_fresh_locked_survey_ticket.call_count, 1, msg=msg)
        self.assertFalse(self.survey.is_published_external, msg=msg)

    def test_must_create_new_survey_ticket_if_maximum_answers_is_not_set(self):
        SurveyTicket.objects.all().delete()

        self.survey.maximum_answers_count = None
        self.survey.save()

        self.assertEqual(SurveyTicket.objects.count(), 0)
        self.form._get_fresh_locked_survey_ticket = Mock()
        self.form._increment_answers_count()

        msg = 'Если maximum_answers_count is None, то нужно создать новый заблокированный билет'
        self.assertEqual(SurveyTicket.objects.count(), 1, msg=msg)
        self.assertTrue(SurveyTicket.objects.all()[0].acquired, msg=msg)


class TestSurveyFormBehavior_validate_max_and_min_date_answer_params_for_simple_date(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory(is_published_external=True)
        self.answer_date_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_date'),
        )
        self.form = SurveyForm(survey=self.survey, is_with_additional_fields=False)

    def get_form(self, data=None):
        return SurveyForm(
            survey=self.survey,
            data=data,
            files=None,
            is_with_additional_fields=False
        )

    def test_save_form_with_simple_date_question_without_max_and_min_params(self):
        self.answer_date_question.param_date_field_min = None
        self.answer_date_question.param_date_field_max = None
        self.answer_date_question.save()

        data = {self.answer_date_question.get_form_field_name(): '2015-01-01'}
        form = self.get_form(data=data)
        self.assertTrue(form.is_valid())
        self.assertEqual(len(form.errors), 0)

    def test_save_form_with_simple_date_question_with_min_param(self):
        self.answer_date_question.param_date_field_min = None
        self.answer_date_question.param_date_field_max = None
        self.answer_date_question.save()

        experiments = [
            {'answer_date': '2015-01-01', 'param_date_field_min': '2014-12-10', 'must_be_valid': True},
            {'answer_date': '2015-01-01', 'param_date_field_min': '2015-01-01', 'must_be_valid': True},
            {'answer_date': '2015-01-01', 'param_date_field_min': '2015-01-10', 'must_be_valid': False},
        ]

        for i, experiment in enumerate(experiments):
            if experiment['param_date_field_min']:
                param_date_field_min = timezone.make_aware(parser.parse(experiment['param_date_field_min']), timezone.utc)
            else:
                param_date_field_min = None
            self.answer_date_question.param_date_field_min = param_date_field_min
            self.answer_date_question.save()
            data = {self.answer_date_question.get_form_field_name(): experiment['answer_date']}

            form = self.get_form(data=data)

            msg = '[%s] Если минимальная дата %s, а отправлена %s, то form.is_valid() должен возвращать %s' % (
                i,
                experiment['param_date_field_min'],
                experiment['answer_date'],
                experiment['must_be_valid'],
            )
            self.assertEqual(form.is_valid(), experiment['must_be_valid'], msg=msg)

    def test_save_form_with_simple_date_question_with_max_param(self):
        self.answer_date_question.param_date_field_min = None
        self.answer_date_question.param_date_field_max = None
        self.answer_date_question.save()

        experiments = [
            {'answer_date': '2015-01-01', 'param_date_field_max': '2014-12-10', 'must_be_valid': False},
            {'answer_date': '2015-01-01', 'param_date_field_max': '2015-01-01', 'must_be_valid': True},
            {'answer_date': '2015-01-01', 'param_date_field_max': '2016-01-10', 'must_be_valid': True},
        ]

        for i, experiment in enumerate(experiments):
            if experiment['param_date_field_max']:
                param_date_field_max = timezone.make_aware(parser.parse(experiment['param_date_field_max']), timezone.utc)
            else:
                param_date_field_max = None
            self.answer_date_question.param_date_field_max = param_date_field_max
            self.answer_date_question.save()
            data = {self.answer_date_question.get_form_field_name(): experiment['answer_date']}

            form = self.get_form(data=data)

            msg = '[%s] Если максимальная дата %s, а отправлена %s, то form.is_valid() должен возвращать %s' % (
                i,
                experiment['param_date_field_max'],
                experiment['answer_date'],
                experiment['must_be_valid'],
            )
            self.assertEqual(form.is_valid(), experiment['must_be_valid'], msg=msg)

    def test_save_form_with_simple_date_question_with_max_and_min_params(self):
        self.answer_date_question.param_date_field_min = None
        self.answer_date_question.param_date_field_max = None
        self.answer_date_question.save()

        experiments = [
            {'answer_date': '2015-01-01', 'date_min': '2014-01-01', 'date_max': '2014-12-10', 'must_be_valid': False},
            {'answer_date': '2015-01-01', 'date_min': '2014-01-01', 'date_max': '2015-01-01', 'must_be_valid': True},
            {'answer_date': '2014-01-01', 'date_min': '2014-01-01', 'date_max': '2015-01-10', 'must_be_valid': True},
            {'answer_date': '2020-01-01', 'date_min': '2014-01-01', 'date_max': '2015-01-10', 'must_be_valid': False},
        ]

        for i, experiment in enumerate(experiments):
            if experiment['date_min']:
                date_min = timezone.make_aware(parser.parse(experiment['date_min']), timezone.utc)
            else:
                date_min = None
            if experiment['date_max']:
                date_max = timezone.make_aware(parser.parse(experiment['date_max']), timezone.utc)
            else:
                date_max = None
            self.answer_date_question.param_date_field_max = date_max
            self.answer_date_question.param_date_field_min = date_min
            self.answer_date_question.save()

            data = {self.answer_date_question.get_form_field_name(): experiment['answer_date']}

            form = self.get_form(data=data)

            msg = '[%s] Если минимальная дата %s, максимальная %s, а отправлена %s, то form.is_valid() должен возвращать %s' % (
                i,
                experiment['date_min'],
                experiment['date_max'],
                experiment['answer_date'],
                experiment['must_be_valid'],
            )
            self.assertEqual(form.is_valid(), experiment['must_be_valid'], msg=msg)


class TestSurveyFormBehavior_validate_max_and_min_date_answer_params_for_date_range(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory(is_published_external=True)
        self.answer_date_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_date'),
            param_date_field_type='daterange',
        )
        self.form = SurveyForm(survey=self.survey, is_with_additional_fields=False)

    def get_form(self, data=None):
        return SurveyForm(
            survey=self.survey,
            data=data,
            files=None,
            is_with_additional_fields=False
        )

    def _get_data(self, answer_date_1, answer_date_2):
        return {
            '%s_0' % self.answer_date_question.get_form_field_name(): answer_date_1,
            '%s_1' % self.answer_date_question.get_form_field_name(): answer_date_2,
        }

    def test_save_form_without_max_and_min_params(self):
        self.answer_date_question.param_date_field_min = None
        self.answer_date_question.param_date_field_max = None
        self.answer_date_question.save()

        data = self._get_data('2015-01-01', '2015-01-02')
        form = self.get_form(data=data)

        msg = (
            'Если нет минимальной и максимальной дат, вторая дата '
            'диапазона больше первой, то форма должна быть валидной'
        )
        self.assertTrue(form.is_valid(), msg=msg)

    def test_save_form_is_second_param_lt_first(self):
        self.answer_date_question.param_date_field_min = None
        self.answer_date_question.param_date_field_max = None
        self.answer_date_question.save()

        data = self._get_data('2020-01-01', '2015-01-01')
        form = self.get_form(data=data)

        msg = 'Если вторая дата диапазона меньше первой, то форма не должна быть валидной'
        self.assertFalse(form.is_valid(), msg=msg)

    def test_save_form_is_second_param_eq_first(self):
        self.answer_date_question.param_date_field_min = None
        self.answer_date_question.param_date_field_max = None
        self.answer_date_question.save()

        data = self._get_data('2010-01-01', '2010-01-01')
        form = self.get_form(data=data)

        msg = 'Если вторая дата диапазона равна первой, то форма должна быть валидной'
        self.assertTrue(form.is_valid(), msg=msg)

    def test_save_form_with_min_param_only(self):
        self.answer_date_question.param_date_field_min = None
        self.answer_date_question.param_date_field_max = None
        self.answer_date_question.save()

        experiments = [
            {'answer_1': '2015-01-01', 'answer_2': '2015-01-02', 'param_date_field_min': '2014-12-10', 'must_be_valid': True},
            {'answer_1': '2015-01-01', 'answer_2': '2015-01-02', 'param_date_field_min': '2020-01-01', 'must_be_valid': False},
            {'answer_1': '2015-01-01', 'answer_2': '2015-01-02', 'param_date_field_min': '2015-01-01', 'must_be_valid': True},
        ]

        for i, experiment in enumerate(experiments):
            if experiment['param_date_field_min']:
                param_date_field_min = timezone.make_aware(parser.parse(experiment['param_date_field_min']), timezone.utc)
            else:
                param_date_field_min = None
            self.answer_date_question.param_date_field_min = param_date_field_min
            self.answer_date_question.save()

            data = self._get_data(experiment['answer_1'], experiment['answer_2'])
            form = self.get_form(data=data)

            msg = '[%s] Если минимальная дата %s, а отправлена %s - %s, то form.is_valid() должен возвращать %s' % (
                i,
                experiment['param_date_field_min'],
                experiment['answer_1'],
                experiment['answer_2'],
                experiment['must_be_valid'],
            )
            self.assertEqual(form.is_valid(), experiment['must_be_valid'], msg=msg)

    def test_save_form_with_max_param_only(self):
        self.answer_date_question.param_date_field_min = None
        self.answer_date_question.param_date_field_max = None
        self.answer_date_question.save()

        experiments = [
            {'answer_1': '2015-01-01', 'answer_2': '2015-01-02', 'param_date_field_max': '2014-12-10', 'must_be_valid': False},
            {'answer_1': '2015-01-01', 'answer_2': '2015-01-02', 'param_date_field_max': '2020-01-01', 'must_be_valid': True},
            {'answer_1': '2015-01-01', 'answer_2': '2015-01-02', 'param_date_field_max': '2015-01-02', 'must_be_valid': True},
        ]

        for i, experiment in enumerate(experiments):
            if experiment['param_date_field_max']:
                param_date_field_max = timezone.make_aware(parser.parse(experiment['param_date_field_max']), timezone.utc)
            else:
                param_date_field_max = None
            self.answer_date_question.param_date_field_max = param_date_field_max
            self.answer_date_question.save()

            data = self._get_data(experiment['answer_1'], experiment['answer_2'])
            form = self.get_form(data=data)

            msg = '[%s] Если максимальная дата %s, а отправлена %s - %s, то form.is_valid() должен возвращать %s' % (
                i,
                experiment['param_date_field_max'],
                experiment['answer_1'],
                experiment['answer_2'],
                experiment['must_be_valid'],
            )
            self.assertEqual(form.is_valid(), experiment['must_be_valid'], msg=msg)

    def test_save_form_with_min_and_max_params(self):
        self.answer_date_question.param_date_field_min = None
        self.answer_date_question.param_date_field_max = None
        self.answer_date_question.save()

        experiments = [
            {
                'answer_1': '2015-01-01',
                'answer_2': '2015-01-02',
                'param_date_field_min': '2014-12-10',
                'param_date_field_max': '2014-12-10',
                'must_be_valid': False,
            },
            {
                'answer_1': '2015-01-01',
                'answer_2': '2015-01-02',
                'param_date_field_min': '2015-01-01',
                'param_date_field_max': '2015-01-02',
                'must_be_valid': True,
            },
            {
                'answer_1': '2015-01-01',
                'answer_2': '2015-01-02',
                'param_date_field_min': '2015-01-01',
                'param_date_field_max': '2015-10-02',
                'must_be_valid': True,
            },
            {
                'answer_1': '2015-01-01',
                'answer_2': '2015-01-02',
                'param_date_field_min': '2014-01-01',
                'param_date_field_max': '2017-10-02',
                'must_be_valid': True,
            },
            {
                'answer_1': '2015-01-01',
                'answer_2': '2015-01-02',
                'param_date_field_min': '2015-01-02',
                'param_date_field_max': '2017-10-02',
                'must_be_valid': False,
            },
            {
                'answer_1': '2015-01-01',
                'answer_2': '2016-01-02',
                'param_date_field_min': '2015-01-02',
                'param_date_field_max': '2017-10-02',
                'must_be_valid': False,
            },
        ]

        for i, experiment in enumerate(experiments):
            if experiment['param_date_field_min']:
                param_date_field_min = timezone.make_aware(parser.parse(experiment['param_date_field_min']), timezone.utc)
            else:
                param_date_field_max = None
            if experiment['param_date_field_max']:
                param_date_field_max = timezone.make_aware(parser.parse(experiment['param_date_field_max']), timezone.utc)
            else:
                param_date_field_max = None

            self.answer_date_question.param_date_field_min = param_date_field_min
            self.answer_date_question.param_date_field_max = param_date_field_max
            self.answer_date_question.save()

            data = self._get_data(experiment['answer_1'], experiment['answer_2'])
            form = self.get_form(data=data)

            msg = '[%s] Если диапазон дат [%s %s], а отправлена %s - %s, то form.is_valid() должен возвращать %s' % (
                i,
                experiment['param_date_field_min'],
                experiment['param_date_field_max'],
                experiment['answer_1'],
                experiment['answer_2'],
                experiment['must_be_valid'],
            )
            self.assertEqual(form.is_valid(), experiment['must_be_valid'], msg=msg)


class TestSurveyFormBehavior_add_date_field_info(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory(is_published_external=True)
        self.answer_date_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_date'),
            param_date_field_type='date',
            param_slug='answer_date_1',
        )
        self.form = SurveyForm(survey=self.survey, is_with_additional_fields=False)

    def get_form(self, data=None):
        return SurveyForm(
            survey=self.survey,
            data=data,
            files=None,
            is_with_additional_fields=False
        )

    def test_save_form_with_min_param_only(self):
        self.answer_date_question.param_date_field_min = None
        self.answer_date_question.param_date_field_max = None
        self.answer_date_question.save()

        experiments = [
            {'min_param': None, 'max_param': None, 'exp_allowed_range': {}},
            {'min_param': '2015-01-01', 'max_param': None, 'exp_allowed_range': {'from': '2015-01-01'}},
            {'min_param': None, 'max_param': '2015-01-01', 'exp_allowed_range': {'to': '2015-01-01'}},
            {'min_param': '2015-01-01', 'max_param': '2015-01-02', 'exp_allowed_range': {'from': '2015-01-01', 'to': '2015-01-02'}},
        ]

        for i, experiment in enumerate(experiments):
            self.answer_date_question.param_date_field_min = experiment['min_param']
            self.answer_date_question.param_date_field_max = experiment['max_param']
            self.answer_date_question.save()

            form = self.get_form()
            allowed_range = form.fields['answer_date_1'].other_data.get('allowed_range', {})
            msg = '[%s] Неверное значение other_data.allowed_range' % i
            self.assertEqual(allowed_range, experiment['exp_allowed_range'], msg=msg)


class TestSurveyFormBehavior_not_initialized_answer_choices(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()
        answer_choices_type = AnswerType.objects.get(slug='answer_choices')
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=answer_choices_type,
            param_is_allow_multiple_choice=False,
            param_is_required=True,
        )

    def test_without_initial_data(self):
        SurveyForm(survey=self.survey)

    def test_with_initial_data(self):
        self.question.initial = [None]
        self.question.save()
        SurveyForm(survey=self.survey)


class TestSurveyFormBehaviour_form_with_pages(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()

        answer_short_text_type = AnswerType.objects.get(slug='answer_short_text')
        answer_choices_type = AnswerType.objects.get(slug='answer_choices')
        self.questions = [
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=answer_choices_type,
                page=1,
                position=1,
                param_slug='1',
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=answer_short_text_type,
                page=1,
                position=2,
                param_slug='2',
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=answer_choices_type,
                page=2,
                position=1,
                param_slug='3',
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=answer_short_text_type,
                page=2,
                position=2,
                param_slug='4',
            ),
        ]
        # create question choices for first question
        SurveyQuestionChoiceFactory(survey_question=self.questions[0], label='1')
        SurveyQuestionChoiceFactory(survey_question=self.questions[0], label='2')

        # create question choices for third question
        SurveyQuestionChoiceFactory(survey_question=self.questions[2], label='3')
        SurveyQuestionChoiceFactory(survey_question=self.questions[2], label='4')

        self.user = UserFactory(uid=None)

    def get_form(self, is_with_captcha=False):
        return SurveyForm(
            survey=self.survey,
            instance=self.user,
            data=None,
            files=None,
            is_with_additional_fields=True,
            is_with_captcha=is_with_captcha
        )

    def assert_has_field_name(self, form_dict):
        for field_name, field in list(form_dict['fields'].items()):
            self.assertIn('name', field)
            self.assertEqual(field_name, field['name'])

    def test_check_question_fields(self):
        form = self.get_form()
        form_dict = form.as_dict()
        self.assert_has_field_name(form_dict)

        pages = [field.get('page') for field in list(form_dict['fields'].values())]
        expected = [1, 1, 2, 2]
        self.assertEqual(pages, expected)

        positions = [field.get('position') for field in list(form_dict['fields'].values())]
        expected = [1, 2, 1, 2]
        self.assertEqual(positions, expected)

    @responses.activate
    def test_check_captcha(self):
        responses.add(
            responses.GET,
            'http://api.captcha.yandex.net/generate',
            body='<number url="https://ext.captcha.yandex.net/image?key=123">123</number>',
            content_type='text/xml',
        )
        form = self.get_form(is_with_captcha=True)
        form_dict = form.as_dict()
        self.assert_has_field_name(form_dict)

        slugs = [slug for slug in list(form_dict['fields'].keys())]
        pages = [field.get('page') for field in list(form_dict['fields'].values())]
        expected = [1, 1, 2, 2, 2]

        self.assertIn('captcha', slugs)
        self.assertEqual(pages, expected)

        positions = [field.get('position') for field in list(form_dict['fields'].values())]
        expected = [1, 2, 1, 2, 3]
        self.assertEqual(positions, expected)

    def test_check_agreements(self):
        self.survey.agreements.set(SurveyAgreement.objects.filter(slug__in=['events', 'hr']))
        form = self.get_form()
        form_dict = form.as_dict()
        self.assert_has_field_name(form_dict)

        slugs = [slug for slug in list(form_dict['fields'].keys())]
        pages = [field.get('page') for field in list(form_dict['fields'].values())]
        expected = [1, 1, 2, 2, 2, 2]

        self.assertIn('is_agree_with_events', slugs)
        self.assertIn('is_agree_with_hr', slugs)
        self.assertEqual(pages, expected)

        positions = [field.get('position') for field in list(form_dict['fields'].values())]
        expected = [1, 2, 1, 2, 3, 4]
        self.assertEqual(positions, expected)


class TestSurveyFormBehavior_access_by_slug(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.survey = SurveyFactory(is_published_external=True)
        SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text')
        )

    def test_slug_without_dashes(self):
        self.survey.slug = 'my_1st_test_form'
        self.survey.save()

        response = self.client.get('/v1/surveys/%s/' % self.survey.slug)
        self.assertEqual(response.status_code, 200)

        response = self.client.get('/v1/surveys/%s/form/' % self.survey.slug)
        self.assertEqual(response.status_code, 200)

    def test_slug_with_dashes(self):
        self.survey.slug = 'my-1st-test-form'
        self.survey.save()

        response = self.client.get('/v1/surveys/%s/' % self.survey.slug)
        self.assertEqual(response.status_code, 200)

        response = self.client.get('/v1/surveys/%s/form/' % self.survey.slug)
        self.assertEqual(response.status_code, 200)

    def test_slug_with_dashes_not_created_user(self):
        self.client.login_yandex(uid='11591999')
        self.survey.slug = 'my-1st-test-form'
        self.survey.save()

        response = self.client.get('/v1/surveys/%s/' % self.survey.slug)
        self.assertEqual(response.status_code, 200)

        response = self.client.get('/v1/surveys/%s/form/' % self.survey.slug)
        self.assertEqual(response.status_code, 200)


class TestSurveyFormBehavior__not_rewrite_model(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = self.client.login_yandex()
        self.survey = SurveyFactory(is_published_external=True)
        answer_choices = AnswerType.objects.get(slug='answer_short_text')
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=answer_choices,
            param_slug='uid',
        )

    def test_success_save(self):
        data = {
            self.question.param_slug: 'some_uid',
        }
        response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk, data)
        self.assertEqual(response.status_code, 200)
        answer = get_answer_data(response)

        question_value = answer.get(self.question.pk, {}).get('value')
        self.assertIsNotNone(question_value)
        self.assertEqual(question_value, 'some_uid')

    def test_success_not_rewrite(self):
        data = {
            self.question.param_slug: 'some_uid',
        }
        form = self.survey.get_form(
            data=data,
            files={},
            instance=self.user,
            request={},
            is_with_captcha=False,
        )
        self.assertTrue(form.is_valid())


class TestSurveyFormBehavior__catch_valuerror(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.survey = SurveyFactory(is_published_external=True)
        answer_choices = AnswerType.objects.get(slug='answer_choices')
        self.question = SurveyQuestionFactory(survey=self.survey, answer_type=answer_choices)
        self.choices = [
            SurveyQuestionChoiceFactory(survey_question=self.question, label='choice_1'),
            SurveyQuestionChoiceFactory(survey_question=self.question, label='choice_2'),
        ]

    def test_success(self):
        data = {
            self.question.param_slug: self.choices[0].pk,
        }
        response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk, data)
        self.assertEqual(response.status_code, 200)

    def test_validation_error(self):
        data = {
            self.question.param_slug: '1<fake data>',
        }
        response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk, data)
        self.assertEqual(response.status_code, 400)


class TestSurveyFormBehavior__answer_date(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory(is_published_external=True)
        self.required = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            param_is_required=True,
        )
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_date'),
        )

    def test_should_succeed(self):
        data = {
            self.required.param_slug: 'qwerty',
            self.question.param_slug: '2021-08-27',
        }
        response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/', data)
        self.assertEqual(response.status_code, 200)

    def test_should_fail_and_return_old_data(self):
        data = {
            self.question.param_slug: '2021-08-27',
        }
        response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/', data)
        self.assertEqual(response.status_code, 400)

        fields = response.data['fields']
        self.assertEqual(len(fields), 2)
        self.assertEqual(fields[self.question.param_slug]['tags'][0]['attrs']['value'], '2021-08-27')


class TestSurveyFormBehavior_post_with_conditions(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory(is_published_external=True)
        answer_number = AnswerType.objects.get(slug='answer_number')
        answer_short_text = AnswerType.objects.get(slug='answer_short_text')
        self.questions = [
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=answer_number,
                param_max=100,
                param_is_required=False,
                param_slug='cond',
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=answer_short_text,
                param_is_required=False,
                param_slug='lt50',
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=answer_short_text,
                param_is_required=False,
                param_slug='gt50',
            ),
        ]
        self.node_1 = SurveyQuestionShowConditionNodeFactory(survey_question=self.questions[1])
        self.node_2 = SurveyQuestionShowConditionNodeFactory(survey_question=self.questions[2])

        content_type_attribute = ContentTypeAttributeFactory(
            title='test',
            attr='answer_number',
            allowed_conditions=['lt', 'gt', 'eq', 'neq'],
        )

        SurveyQuestionShowConditionNodeItemFactory(
            position=1,
            survey_question_show_condition_node=self.node_1,
            operator='and',
            condition='lt',
            content_type_attribute=content_type_attribute,
            survey_question=self.questions[0],
            value=50,
        )
        SurveyQuestionShowConditionNodeItemFactory(
            position=1,
            survey_question_show_condition_node=self.node_2,
            operator='and',
            condition='gt',
            content_type_attribute=content_type_attribute,
            survey_question=self.questions[0],
            value=50,
        )


class TestSurveyFormBehavior_groups_questions(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory(is_published_external=True, is_allow_multiple_answers=True)
        self.group_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_group')
        )
        self.short_group_text = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            group=self.group_question
        )
        self.long_group_text = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_long_text'),
            group=self.group_question
        )
        self.short_no_group_text = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
        )
        self.data = {
            '{}__0'.format(self.short_group_text.param_slug): 'short group text one',
            '{}__1'.format(self.short_group_text.param_slug): 'short group text two',
            '{}__0'.format(self.long_group_text.param_slug): 'long group text one',
            '{}__1'.format(self.long_group_text.param_slug): 'long group text two',
            self.short_no_group_text.param_slug: 'short text no group',
        }

    def test_should_be_with_required_questions_by_default(self):
        fields = SurveyForm(survey=self.survey, data=self.data).fields
        self.assertNotIn(self.short_group_text.param_slug, list(fields.keys()))
        group_field = fields[self.group_question.param_slug]
        self.assertEqual(group_field.position, 1)
        self.assertEqual(group_field.widget.__class__, GroupWidget)

        short_group_text_field__0 = fields['{}__0'.format(self.short_group_text.param_slug)]
        self.assertEqual(short_group_text_field__0.group_id, self.group_question.id)
        self.assertEqual(short_group_text_field__0.group_slug, self.group_question.get_form_field_name())
        self.assertEqual(short_group_text_field__0.position, 2)
        self.assertEqual(short_group_text_field__0.question, self.short_group_text)
        self.assertEqual(short_group_text_field__0.label, self.short_group_text.label)

        short_group_text_field__1 = fields['{}__1'.format(self.short_group_text.param_slug)]
        self.assertEqual(short_group_text_field__1.group_id, self.group_question.id)
        self.assertEqual(short_group_text_field__1.group_slug, self.group_question.get_form_field_name())
        self.assertEqual(short_group_text_field__1.position, 2)
        self.assertEqual(short_group_text_field__1.question, self.short_group_text)
        self.assertEqual(short_group_text_field__1.label, self.short_group_text.label)

        long_group_text_field__0 = fields['{}__0'.format(self.long_group_text.param_slug)]
        self.assertEqual(long_group_text_field__0.group_id, self.group_question.id)
        self.assertEqual(long_group_text_field__0.group_slug, self.group_question.get_form_field_name())
        self.assertEqual(long_group_text_field__0.position, 3)
        self.assertEqual(long_group_text_field__0.question, self.long_group_text)
        self.assertEqual(long_group_text_field__0.label, self.long_group_text.label)

        long_group_text_field__1 = fields['{}__1'.format(self.long_group_text.param_slug)]
        self.assertEqual(long_group_text_field__1.group_id, self.group_question.id)
        self.assertEqual(long_group_text_field__1.group_slug, self.group_question.get_form_field_name())
        self.assertEqual(long_group_text_field__1.position, 3)
        self.assertEqual(long_group_text_field__1.question, self.long_group_text)
        self.assertEqual(long_group_text_field__1.label, self.long_group_text.label)

        short_no_group_text_field = fields[self.short_no_group_text.param_slug]
        self.assertIsNone(short_no_group_text_field.group_id)
        self.assertIsNone(short_no_group_text_field.group_slug)
        self.assertEqual(short_no_group_text_field.position, 4)
        self.assertEqual(short_no_group_text_field.question, self.short_no_group_text)
        self.assertEqual(short_no_group_text_field.label, self.short_no_group_text.label)

    def test_success(self):
        response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk, self.data)
        self.assertEqual(response.status_code, 200)
        answer = get_answer_data(response)

        self.assertEqual(len(answer), 2)
        group_values = answer.get(self.group_question.pk, {}).get('value')
        self.assertIsNotNone(group_values)
        self.assertEqual(len(group_values), 2)
        self.assertEqual(len(group_values[0]), 2)
        self.assertEqual(len(group_values[1]), 2)

    def test_return_all_groups_questions_if_not_pass_validation(self):
        self.data = {
            self.long_group_text.get_form_field_name('0'): 'long group text two',
            self.short_group_text.get_form_field_name('1'): 'smth',
            self.long_group_text.get_form_field_name('2'): 'long group text another',
            self.short_no_group_text.param_slug: 'short text no group',
        }
        self.long_group_text.param_min = 60
        self.long_group_text.param_is_required = False
        self.long_group_text.save()
        self.short_group_text.param_is_required = False
        self.short_group_text.save()

        response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk, self.data)
        self.assertEqual(response.status_code, 400)
        fields = list(response.data['fields'].keys())
        self.assertEqual(
            set(fields),
            {
                self.long_group_text.get_form_field_name('0'),
                self.long_group_text.get_form_field_name('1'),
                self.long_group_text.get_form_field_name('2'),
                self.short_group_text.get_form_field_name('0'),
                self.short_group_text.get_form_field_name('1'),
                self.short_group_text.get_form_field_name('2'),
                self.short_no_group_text.get_form_field_name(),
                self.group_question.get_form_field_name(),
            }
        )

    def test_show_conditions(self):
        self.node_1 = SurveyQuestionShowConditionNodeFactory(survey_question=self.long_group_text)

        self.content_type_attribute = ContentTypeAttributeFactory(
            title='test',
            attr='answer_short_text',
        )

        SurveyQuestionShowConditionNodeItemFactory(
            position=1,
            survey_question_show_condition_node=self.node_1,
            operator='and',
            condition='eq',
            content_type_attribute=self.content_type_attribute,
            survey_question=self.short_group_text,
            value='smth',
        )
        response = self.client.get('/v1/surveys/%s/form/' % self.survey.pk)
        show_conditions = response.data['fields'][self.long_group_text.get_form_field_name('0')]['other_data']['show_conditions']
        self.assertEqual(show_conditions[0][0]['field'], self.short_group_text.get_form_field_name('0'))

        self.short_group_text.group = None
        self.short_group_text.save()
        response = self.client.get('/v1/surveys/%s/form/' % self.survey.pk)
        show_conditions = response.data['fields'][self.long_group_text.get_form_field_name('0')]['other_data']['show_conditions']
        self.assertEqual(show_conditions[0][0]['field'], self.short_group_text.get_form_field_name())

        self.short_group_text.group = self.group_question
        self.long_group_text.group = None
        self.short_group_text.save()
        self.long_group_text.save()

        response = self.client.get('/v1/surveys/%s/form/' % self.survey.pk)
        show_conditions = response.data['fields'][self.long_group_text.get_form_field_name()]['other_data']['show_conditions']
        self.assertEqual(show_conditions[0][0]['field'], self.short_group_text.get_form_field_name('0'))

    def test_show_conditions_not_created(self):
        self.client.login_yandex()
        self.node_1 = SurveyQuestionShowConditionNodeFactory(survey_question=self.long_group_text)

        self.content_type_attribute = ContentTypeAttributeFactory(
            title='test',
            attr='answer_short_text',
        )

        SurveyQuestionShowConditionNodeItemFactory(
            position=1,
            survey_question_show_condition_node=self.node_1,
            operator='and',
            condition='eq',
            content_type_attribute=self.content_type_attribute,
            survey_question=self.short_group_text,
            value='smth',
        )
        response = self.client.get('/v1/surveys/%s/form/' % self.survey.pk)
        json_response = json.loads(response.content.decode('utf-8'))
        show_conditions = json_response['fields'][self.long_group_text.get_form_field_name('0')]['other_data']['show_conditions']
        self.assertEqual(show_conditions[0][0]['field'], self.short_group_text.get_form_field_name('0'))

        self.short_group_text.group = None
        self.short_group_text.save()
        response = self.client.get('/v1/surveys/%s/form/' % self.survey.pk)
        json_response = json.loads(response.content.decode('utf-8'))
        show_conditions = json_response['fields'][self.long_group_text.get_form_field_name('0')]['other_data']['show_conditions']
        self.assertEqual(show_conditions[0][0]['field'], self.short_group_text.get_form_field_name())

        self.short_group_text.group = self.group_question
        self.long_group_text.group = None
        self.short_group_text.save()
        self.long_group_text.save()

        response = self.client.get('/v1/surveys/%s/form/' % self.survey.pk)
        json_response = json.loads(response.content.decode('utf-8'))
        show_conditions = json_response['fields'][self.long_group_text.get_form_field_name()]['other_data']['show_conditions']
        self.assertEqual(show_conditions[0][0]['field'], self.short_group_text.get_form_field_name('0'))

    def test_should_not_check_field_in_group_if_group_not_displayed(self):
        self.node_1 = SurveyQuestionShowConditionNodeFactory(survey_question=self.group_question)
        self.long_group_text.param_is_required = True
        self.long_group_text.save()

        self.content_type_attribute = ContentTypeAttributeFactory(
            title='test',
            attr='answer_short_text',
        )

        SurveyQuestionShowConditionNodeItemFactory(
            position=1,
            survey_question_show_condition_node=self.node_1,
            operator='and',
            condition='eq',
            content_type_attribute=self.content_type_attribute,
            survey_question=self.short_no_group_text,
            value='smth',
        )

        data = {self.short_no_group_text.param_slug: 'short text no group', }
        response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk, data)
        self.assertEqual(response.status_code, 200)
        answer = get_answer_data(response)

        no_group_text_value = answer.get(self.short_no_group_text.pk, {}).get('value')
        self.assertIsNotNone(no_group_text_value)
        self.assertEqual(no_group_text_value, 'short text no group')
        old_response = response

        data = {self.short_no_group_text.param_slug: 'smth', }
        response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk, data)
        self.assertEqual(response.status_code, 400)
        answer = get_answer_data(old_response)

        no_group_text_value = answer.get(self.short_no_group_text.pk, {}).get('value')
        self.assertIsNotNone(no_group_text_value)
        self.assertEqual(no_group_text_value, 'short text no group')

    def test_should_work_correct_in_some_questions_not_displayed(self):
        self.node_1 = SurveyQuestionShowConditionNodeFactory(survey_question=self.long_group_text)
        self.long_group_text.param_is_required = True
        self.long_group_text.save()

        self.content_type_attribute = ContentTypeAttributeFactory(
            title='test',
            attr='answer_short_text',
        )

        SurveyQuestionShowConditionNodeItemFactory(
            position=1,
            survey_question_show_condition_node=self.node_1,
            operator='and',
            condition='eq',
            content_type_attribute=self.content_type_attribute,
            survey_question=self.short_group_text,
            value='smth',
        )

        data = {
            self.short_group_text.get_form_field_name('0'): 'smth',
            self.long_group_text.get_form_field_name('0'): 'long group text',

            self.short_group_text.get_form_field_name('2'): 'smth',
            self.long_group_text.get_form_field_name('2'): 'long group text another',

            self.short_group_text.get_form_field_name('1'): 'not smth',
            self.short_group_text.get_form_field_name('3'): 'hello smth',

            self.short_no_group_text.param_slug: 'short text no group',
        }

        response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk, data)
        self.assertEqual(response.status_code, 200)
        answer = get_answer_data(response)

        non_group_value = answer.get(self.short_no_group_text.pk, {}).get('value')
        self.assertIsNotNone(non_group_value)
        self.assertEqual(non_group_value, 'short text no group')

        group_values = answer.get(self.group_question.pk, {}).get('value')
        self.assertIsNotNone(group_values)
        self.assertEqual(len(group_values), 4)

        results = [
            field.get('value')
            for fieldset in group_values
            for field in fieldset
        ]
        expected = [
            'smth', 'long group text',
            'not smth',
            'smth', 'long group text another',
            'hello smth',
        ]
        self.assertListEqual(results, expected)

    def test_should_not_fail_if_no_data(self):
        data = {self.short_no_group_text.param_slug: 'short text no group', }
        response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk, data)
        self.assertEqual(response.status_code, 400)

    def test_should_work_correctly_without_required(self):
        self.short_group_text.param_is_required = False
        self.short_group_text.save()
        self.long_group_text.param_is_required = False
        self.long_group_text.save()

        data = {self.short_no_group_text.param_slug: 'short text no group', }
        response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk, data)
        self.assertEqual(response.status_code, 200)
        answer = get_answer_data(response)

        no_group_text_value = answer.get(self.short_no_group_text.pk, {}).get('value')
        self.assertIsNotNone(no_group_text_value)
        self.assertEqual(no_group_text_value, 'short text no group')

    def test_should_work_correctly_with_some_required(self):
        self.long_group_text.param_is_required = False
        self.long_group_text.save()

        data = {
            self.short_no_group_text.param_slug: 'short text no group',
        }
        response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk, data)
        self.assertEqual(response.status_code, 400)

        data = {
            self.short_no_group_text.param_slug: 'short text no group',
            '{}__0'.format(self.short_group_text.param_slug): 'short group text one',
        }
        response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk, data)
        self.assertEqual(response.status_code, 200)
        answer = get_answer_data(response)

        no_group_text_value = answer.get(self.short_no_group_text.pk, {}).get('value')
        self.assertIsNotNone(no_group_text_value)
        self.assertEqual(no_group_text_value, 'short text no group')

        group_values = answer.get(self.group_question.pk, {}).get('value')
        self.assertIsNotNone(group_values)
        self.assertEqual(len(group_values), 1)

        results = [
            field.get('value')
            for fieldset in group_values
            for field in fieldset
        ]
        expected = [
            'short group text one',
        ]
        self.assertListEqual(results, expected)

    def test_validation_error(self):
        phone_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_phone'),
            group=self.group_question
        )
        self.data['{}__0'.format(phone_question.param_slug)] = '89164567819'
        self.data['{}__1'.format(phone_question.param_slug)] = '123'

        response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk, self.data)
        self.assertEqual(response.status_code, 400)
        self.assertEqual(
            response.data['fields']['{}__1'.format(phone_question.param_slug)]['errors'],
            ['Введите телефонный номер в международном формате'],
        )
        self.assertEqual(
            response.data['fields']['{}__0'.format(phone_question.param_slug)]['errors'],
            [],
        )

    def test_editable_form(self):
        self.survey.is_allow_answer_editing = True
        self.survey.is_allow_multiple_answers = False
        self.survey.save()

        # базовый тест
        response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk, self.data)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(self.survey.profilesurveyanswer_set.count(), 1)
        self.survey.profilesurveyanswer_set.first()

        # меняем тесты ответов в группе
        data = {
            '{}__0'.format(self.short_group_text.param_slug): 'text1',
            '{}__1'.format(self.short_group_text.param_slug): 'text2',
            '{}__0'.format(self.long_group_text.param_slug): 'para1',
            '{}__1'.format(self.long_group_text.param_slug): 'para2',
            self.short_no_group_text.param_slug: 'short text no group',
        }
        response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk, data)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(self.survey.profilesurveyanswer_set.count(), 1)

        # увеличиваем количество ответов в группе
        data = {
            '{}__0'.format(self.short_group_text.param_slug): 'text1',
            '{}__1'.format(self.short_group_text.param_slug): 'text2',
            '{}__2'.format(self.short_group_text.param_slug): 'text3',
            '{}__0'.format(self.long_group_text.param_slug): 'para1',
            '{}__1'.format(self.long_group_text.param_slug): 'para2',
            '{}__2'.format(self.long_group_text.param_slug): 'para3',
            self.short_no_group_text.param_slug: 'short text no group',
        }
        response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk, data)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(self.survey.profilesurveyanswer_set.count(), 1)

        # уменьшаем количество ответов в группе
        data = {
            '{}__0'.format(self.short_group_text.param_slug): 'text1',
            '{}__0'.format(self.long_group_text.param_slug): 'para1',
            self.short_no_group_text.param_slug: 'short text no group',
        }
        response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk, data)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(self.survey.profilesurveyanswer_set.count(), 1)


class TestSurveyFormBehavior_label_image(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()
        self.image = ImageFactory()
        self.sizes = set(
            '%sx%s' % (w or '', h or '')
            for (w, h) in settings.IMAGE_SIZES
        )

    def test_field_shouldnt_contain_label_image(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
        )
        fields = SurveyForm(survey=self.survey, is_with_additional_fields=False).fields
        label_image = fields[question.param_slug].label_image
        self.assertIsNone(label_image)

    def test_field_answer_short_text_should_contain_label_image(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            label_image=self.image,
        )
        fields = SurveyForm(survey=self.survey, is_with_additional_fields=False).fields
        label_image = fields[question.param_slug].label_image
        self.assertIsNotNone(label_image)
        self.assertSetEqual(set(label_image['links'].keys()), self.sizes)
        for url in list(label_image['links'].values()):
            self.assertIn(str(self.image.image), url)

    def test_field_answer_choices_should_contain_label_image(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            label_image=self.image,
        )
        fields = SurveyForm(survey=self.survey, is_with_additional_fields=False).fields
        label_image = fields[question.param_slug].label_image
        self.assertIsNotNone(label_image)
        self.assertSetEqual(set(label_image['links'].keys()), self.sizes)
        for url in list(label_image['links'].values()):
            self.assertIn(str(self.image.image), url)

    def test_answer_choices_should_not_contain_label_image(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
        )
        SurveyQuestionChoiceFactory(
            survey_question=question, label='smth',
        )
        fields = SurveyForm(survey=self.survey, is_with_additional_fields=False).get_fields_as_dicts()
        items = fields[question.param_slug]['data_source']['items']
        self.assertEqual(len(items), 1)
        label_image = items[0]['label_image']
        self.assertIsNone(label_image)

    def test_answer_choices_should_contain_label_image(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
        )
        SurveyQuestionChoiceFactory(
            survey_question=question, label='smth',
            label_image=self.image,
        )
        fields = SurveyForm(survey=self.survey, is_with_additional_fields=False).get_fields_as_dicts()
        items = fields[question.param_slug]['data_source']['items']
        self.assertEqual(len(items), 1)
        label_image = items[0]['label_image']
        self.assertIsNotNone(label_image)
        self.assertSetEqual(set(label_image['links'].keys()), self.sizes)
        for url in list(label_image['links'].values()):
            self.assertIn(str(self.image.image), url)


class TestSurveyFormBehavior_group_and_captcha(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory(is_published_external=True)
        self.answer_short_text = AnswerType.objects.get(slug='answer_short_text')
        SurveyQuestionFactory(survey=self.survey, answer_type=self.answer_short_text)
        SurveyQuestionFactory(survey=self.survey, answer_type=self.answer_short_text)
        SurveyQuestionFactory(survey=self.survey, answer_type=self.answer_short_text)

    def test_without_group__captcha_should_be_the_last_field(self):
        fields = SurveyForm(survey=self.survey, is_with_additional_fields=True).fields
        self.assertEqual(len(fields), 4)
        self.assertIn('captcha', fields)
        self.assertEqual(fields['captcha'].position, 4)

    def test_with_group__captcha_should_be_the_last_non_group_field(self):
        answer_group = AnswerType.objects.get(slug='answer_group')
        group = SurveyQuestionFactory(survey=self.survey, answer_type=answer_group)
        SurveyQuestionFactory(survey=self.survey, answer_type=self.answer_short_text, group=group)

        fields = SurveyForm(survey=self.survey, is_with_additional_fields=True).fields
        self.assertEqual(len(fields), 6)
        self.assertIn('captcha', fields)
        self.assertEqual(fields['captcha'].position, 5)


class TestSurveyFormBehavior_with_language_fallback(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.survey = SurveyFactory(is_published_external=True, language='ru')
        self.answer_short_text = AnswerType.objects.get(slug='answer_short_text')
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=self.answer_short_text,
            label='label_ru',
            translations={
                'label': {
                    'ru': 'label_ru',
                    'en': 'label_en',
                    'de': 'label_de',
                },
            },
        )
        self.form_url = '/v1/surveys/%s/form/' % self.survey.pk

    def get_data(self, response):
        return json.loads(response.content.decode(response.charset))

    def test_russian_without_fallback(self):
        response = self.client.get(self.form_url, HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 200)
        fields = self.get_data(response)['fields']
        field = fields[self.question.param_slug]
        self.assertEqual(field['label'], 'label_ru')

    def test_english_without_fallback(self):
        response = self.client.get(self.form_url, HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 200)
        fields = self.get_data(response)['fields']
        field = fields[self.question.param_slug]
        self.assertEqual(field['label'], 'label_en')

    def test_deutsch_without_fallback(self):
        response = self.client.get(self.form_url, HTTP_ACCEPT_LANGUAGE='de')
        self.assertEqual(response.status_code, 200)
        fields = self.get_data(response)['fields']
        field = fields[self.question.param_slug]
        self.assertEqual(field['label'], 'label_de')

    def test_deutsch_with_fallback(self):
        del self.question.translations['label']['de']
        self.question.save()

        response = self.client.get(self.form_url, HTTP_ACCEPT_LANGUAGE='de')
        self.assertEqual(response.status_code, 200)
        fields = self.get_data(response)['fields']
        field = fields[self.question.param_slug]
        self.assertEqual(field['label'], 'label_en')

    def test_english_with_fallback(self):
        del self.question.translations['label']['en']
        self.question.save()

        response = self.client.get(self.form_url, HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 200)
        fields = self.get_data(response)['fields']
        field = fields[self.question.param_slug]
        self.assertEqual(field['label'], 'label_ru')

    def test_russian_with_fallback(self):
        self.question.label = ''
        del self.question.translations['label']['ru']
        self.question.save()

        response = self.client.get(self.form_url, HTTP_ACCEPT_LANGUAGE='ru')
        self.assertEqual(response.status_code, 200)
        fields = self.get_data(response)['fields']
        field = fields[self.question.param_slug]
        self.assertEqual(field['label'], self.question.param_slug)


class TestSurveyForm__captcha(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = self.client.login_yandex()
        self.survey = SurveyFactory(
            is_published_external=True,
            captcha_type='ocr',
            captcha_display_mode='always',
        )
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            label='label',
        )

        self.url = f'/v1/surveys/{self.survey.pk}/form/'

    @responses.activate
    def test_show_ocr_captcha(self):
        responses.add(
            responses.GET,
            'http://api.captcha.yandex.net/generate',
            body='<number url="https://ext.captcha.yandex.net/image?key=123">123</number>',
            content_type='text/xml',
        )
        response = self.client.get(self.url)
        self.assertEqual(response.status_code, 200)
        captcha_field = response.data['fields']['captcha']
        self.assertEqual(captcha_field['other_data']['captcha_type'], 'ocr')
        self.assertEqual(captcha_field['tags'][0]['attrs']['value'], '123')

    @responses.activate
    def test_post_with_ocr_captcha_success(self):
        responses.add(
            responses.GET,
            'http://api.captcha.yandex.net/check',
            body='<image_check>ok</image_check>',
            content_type='text/xml',
        )
        data = {
            "captcha_0": '002928GOSlPzuAryFtDTW6zDh9AmQVQj',
            "captcha_1": 'like Butylene',
            self.question.param_slug: 'test',
        }
        response = self.client.post(self.url, data=data)
        self.assertEqual(response.status_code, 200)

    @responses.activate
    def test_post_with_ocr_captcha_fail(self):
        responses.add(
            responses.GET,
            'http://api.captcha.yandex.net/check',
            body='<image_check error="not found">failed</image_check>',
            content_type='text/xml',
        )
        data = {
            "captcha_0": '002CnzWZXqtgOl7l41gliBmTsLzr6Kvg',
            "captcha_1": 'like Butylene123',
            self.question.param_slug: 'test',
        }
        response = self.client.post(self.url, data=data)
        self.assertEqual(response.status_code, 400)


class TestSurveyFormBehavior_hidden_choices(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory(is_published_external=True)
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
        )
        self.choices = [
            SurveyQuestionChoiceFactory(survey_question=self.question, label='one'),
            SurveyQuestionChoiceFactory(survey_question=self.question, label='two', is_hidden=True),
            SurveyQuestionChoiceFactory(survey_question=self.question, label='three'),
        ]

    def test_should_skip_hidden_choices(self):
        response = self.client.get('/v1/surveys/%s/form/' % self.survey.pk)
        self.assertEqual(response.status_code, 200)

        fields = response.data['fields']
        data_source = fields[self.question.param_slug]['data_source']
        items = set(
            int(item['id'])
            for item in data_source['items']
        )
        self.assertEqual(items, set([self.choices[0].pk, self.choices[2].pk]))


class TestSurveyFormBehavior_answer_group(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory(is_published_external=True)
        self.group_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_group'),
        )
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            param_slug='short_text',
            param_min=2,
            param_max=5,
            group=self.group_question,
        )

    def test_should_return_correct_errors_list(self):
        data = {
            'short_text__0': '1',
            'short_text__1': '12',
            'short_text__2': '123456',
        }
        response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 400)
        fields = response.data['fields']
        self.assertEqual(len(fields['short_text__0']['errors']), 1)
        self.assertEqual(len(fields['short_text__1']['errors']), 0)
        self.assertEqual(len(fields['short_text__2']['errors']), 1)


class TestSurveyFormBehavior_field_conditions_with_deleted(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory(is_published_external=True)
        answer_short_text = AnswerType.objects.get(slug='answer_short_text')
        self.questions = [
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=answer_short_text,
                param_is_required=True,
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=answer_short_text,
                param_is_required=True,
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=answer_short_text,
                param_is_required=True,
            ),
        ]

        # create condition node for two last questions
        self.node_1 = SurveyQuestionShowConditionNodeFactory(survey_question=self.questions[1])
        self.node_2 = SurveyQuestionShowConditionNodeFactory(survey_question=self.questions[2])

        self.content_type_attribute = ContentTypeAttributeFactory(
            title='test',
            attr='answer_short_text',
        )

        # create conditions for second question
        SurveyQuestionShowConditionNodeItemFactory(
            position=1,
            survey_question_show_condition_node=self.node_1,
            operator='and',
            condition='eq',
            content_type_attribute=self.content_type_attribute,
            survey_question=self.questions[0],
            value='1',
        )

        # create conditions for third question
        SurveyQuestionShowConditionNodeItemFactory(
            position=1,
            survey_question_show_condition_node=self.node_2,
            operator='and',
            condition='eq',
            content_type_attribute=self.content_type_attribute,
            survey_question=self.questions[0],
            value='1',
        )
        SurveyQuestionShowConditionNodeItemFactory(
            position=2,
            survey_question_show_condition_node=self.node_2,
            operator='or',
            condition='eq',
            content_type_attribute=self.content_type_attribute,
            survey_question=self.questions[1],
            value='2',
        )

    def test_should_submit_on_all_questions(self):
        data = {
            self.questions[0].param_slug: '1',
            self.questions[1].param_slug: '2',
            self.questions[2].param_slug: '3',
        }
        response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/', data, format='json')
        self.assertEqual(response.status_code, 200)

    def test_should_submit_on_first_question(self):
        data = {
            self.questions[0].param_slug: '100',
        }
        response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/', data, format='json')
        self.assertEqual(response.status_code, 200)

    def test_should_submit_on_second_question_with_deleted_first(self):
        self.questions[0].is_deleted = True
        self.questions[0].save()

        data = {
            self.questions[1].param_slug: '200',
        }
        response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/', data, format='json')
        self.assertEqual(response.status_code, 200)


class TestUrlInForm(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_url'),
        )

    def test_url_valid(self):
        data = get_query_dict({
            self.question.get_form_field_name(): ['https://yandex.ru'],
        })
        form = SurveyForm(survey=self.survey, data=data, is_with_additional_fields=False)
        self.assertTrue(form.is_valid())

    def test_url_valid_with_underline_in_the_middle(self):
        data = get_query_dict({
            self.question.get_form_field_name(): ['http://abv_1111.eee5555.ru/mconstr.html'],
        })
        form = SurveyForm(survey=self.survey, data=data, is_with_additional_fields=False)
        self.assertTrue(form.is_valid())

    def test_url_not_valid_with_underline_in_the_begin(self):
        data = get_query_dict({
            self.question.get_form_field_name(): ['http://_1111.eee5555.ru/mconstr.html'],
        })
        form = SurveyForm(survey=self.survey, data=data, is_with_additional_fields=False)
        self.assertFalse(form.is_valid())

    def test_url_not_valid_with_underline_at_the_end(self):
        data = get_query_dict({
            self.question.get_form_field_name(): ['http://1111_.eee5555.ru/mconstr.html'],
        })
        form = SurveyForm(survey=self.survey, data=data, is_with_additional_fields=False)
        self.assertFalse(form.is_valid())
