# -*- coding: utf-8 -*-
import datetime

from django.test import TestCase, override_settings
from django.core.exceptions import ValidationError

from events.conditions.factories import ContentTypeAttributeFactory
from events.surveyme.factories import (
    SurveyFactory,
    SurveyQuestionFactory,
    SurveyQuestionChoiceFactory,
    SurveyQuestionMatrixTitleFactory,
    SurveyQuestionShowConditionNodeFactory,
    SurveyQuestionShowConditionNodeItemFactory,
)
from events.surveyme.models import (
    AnswerType,
    SurveyQuestionChoice,
    SurveyQuestionMatrixTitle,
)
from events.accounts.helpers import YandexClient


class TestSurveyQuestion(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def test_cannot_create_more_than_one_profile_question_with_same_type_for_same_survey(self):
        for answer_type in AnswerType.objects.filter(kind='profile'):
            # создадим первый вопрос
            question = SurveyQuestionFactory(
                answer_type=answer_type,
            )

            # проверим, что можем спокойно его сохраняться второй раз question
            question.save()

            # попытаемся создать второй вопрос
            try:
                question = SurveyQuestionFactory(
                    survey=question.survey,
                    answer_type=answer_type,
                )
                is_raised = False
            except ValidationError:
                is_raised = True

            msg = ('для одного и тогоже опроса должна быть возможность создать только 1 вопрос с answer_type.slug={0}, '
                   'т.к. он является вопросом к полю профиля'.format(answer_type.slug))
            self.assertTrue(is_raised, msg=msg)

    def test_validate_group_correctly(self):
        question = SurveyQuestionFactory()
        with self.assertRaises(ValidationError):
            question.group = question
            question.save()
        another_question = SurveyQuestionFactory()

        with self.assertRaises(ValidationError):
            question.group = another_question
            question.save()

        group_answer_type = AnswerType.objects.get(slug='answer_group')
        group_question = SurveyQuestionFactory(answer_type=group_answer_type)

        question.group = group_question
        question.save()

        question.refresh_from_db()
        self.assertEqual(question.group_id, group_question.id)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_business_group_field_type(self):
        survey = SurveyFactory()
        group_answer_type = AnswerType.objects.get(slug='answer_group')
        group_question = SurveyQuestionFactory(survey=survey, answer_type=group_answer_type)
        question = SurveyQuestionFactory(survey=survey, group=group_question)

        self.client.login_yandex(is_superuser=True)
        response = self.client.get(f'/admin/api/v2/survey-questions/{question.pk}/')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['survey_id'], survey.pk)
        self.assertEqual(response.data['group_id'], group_question.pk)


class TestSurveyQuestion__validate_param_min_and_param_max_values(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.question = SurveyQuestionFactory()

    def test_should_not_raise_exception_if_max_date_is_none(self):
        self.question.param_date_field_min = datetime.datetime.now()
        self.question.param_date_field_max = None
        try:
            self.question.clean_fields()
            raised = False
        except ValidationError:
            raised = True
        msg = 'Если максимальная дата не установлена, то не нужно вызывать исключение'
        self.assertFalse(raised, msg=msg)

    def test_should_not_raise_exception_if_min_date_is_none(self):
        self.question.param_date_field_min = None
        self.question.param_date_field_max = datetime.datetime.now()
        try:
            self.question.clean_fields()
            raised = False
        except ValidationError:
            raised = True
        msg = 'Если минимальная дата не установлена, то не нужно вызывать исключение'
        self.assertFalse(raised, msg=msg)

    def test_should_raise_exception_if_min_date_eq_max(self):
        self.question.param_date_field_min = datetime.datetime.now()
        self.question.param_date_field_max = datetime.datetime.now()
        try:
            self.question.clean_fields()
            raised = False
        except ValidationError:
            raised = True
        msg = 'Если минимальная дата равна максимальной, то нужно вызвать исключение'
        self.assertTrue(raised, msg=msg)

    def test_should_raise_exception_if_min_date_gt_max(self):
        self.question.param_date_field_min = datetime.datetime.now() + datetime.timedelta(days=1)
        self.question.param_date_field_max = datetime.datetime.now()
        try:
            self.question.clean_fields()
            raised = False
        except ValidationError:
            raised = True
        msg = 'Если минимальная дата больше максимальной, то нужно вызвать исключение'
        self.assertTrue(raised, msg=msg)

    def test_should_not_raise_exception_if_min_date_lt_max(self):
        self.question.param_date_field_min = datetime.datetime.now()
        self.question.param_date_field_max = datetime.datetime.now() + datetime.timedelta(days=1)
        try:
            self.question.clean_fields()
            raised = False
        except ValidationError:
            raised = True
        msg = 'Если минимальная дата меньше максимальной, то не нужно вызывать исключение'
        self.assertFalse(raised, msg=msg)


class TestSurveyQuestion__has_logic(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()
        self.questions = [
            SurveyQuestionFactory(survey=self.survey),
            SurveyQuestionFactory(survey=self.survey),
        ]
        self.question_on_logic = self.questions[0]
        self.question_with_logic = self.questions[1]

    def test_should_true_if_condition(self):
        node = SurveyQuestionShowConditionNodeFactory(survey_question=self.question_with_logic)
        content_type_attribute = ContentTypeAttributeFactory(
            title='test',
            attr='answer_short_text',
        )
        SurveyQuestionShowConditionNodeItemFactory(
            position=1,
            survey_question_show_condition_node=node,
            operator='and',
            condition='eq',
            content_type_attribute=content_type_attribute,
            survey_question=self.question_on_logic,
            value='1',
        )
        self.assertTrue(self.question_with_logic.param_has_logic)

    def test_should_false_if_not_condition(self):
        self.assertFalse(self.question_with_logic.param_has_logic)


class TestSurveyQuestionChoice(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory()

    def test_should_mark_question_choice_as_deleted(self):
        question = SurveyQuestionFactory(
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            survey=self.survey
        )

        red = SurveyQuestionChoiceFactory(survey_question=question, label='red')
        yellow = SurveyQuestionChoiceFactory(survey_question=question, label='yellow')
        green = SurveyQuestionChoiceFactory(survey_question=question, label='green', slug='x-green')
        blue = SurveyQuestionChoiceFactory(survey_question=question, label='blue')

        data = {
            'choices': [{
                'survey_question_id': question.pk,
                'id': red.pk,
                'slug': red.slug,
                'label': red.label,
            },
            {
                'survey_question_id': question.pk,
                'id': yellow.pk,
                'slug': yellow.slug,
                'label': yellow.label,
            },
            {
                'survey_question_id': question.pk,
                'id': blue.pk,
                'slug': blue.slug,
                'label': blue.label,
            }],
        }

        response = self.client.patch('/admin/api/v2/survey-questions/%s/' % question.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)

        self.assertTrue(SurveyQuestionChoice.with_deleted_objects.filter(pk=green.pk).exists())
        deleted_green = SurveyQuestionChoice.with_deleted_objects.get(pk=green.pk)
        self.assertEqual(deleted_green.slug, str(deleted_green.pk))
        self.assertFalse(SurveyQuestionChoice.objects.filter(pk=green.pk).exists())


class TestSurveyQuestionMatrixTitle(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory()

    def test_should_mark_matrix_title_as_deleted(self):
        question = SurveyQuestionFactory(
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            survey=self.survey
        )

        row1 = SurveyQuestionMatrixTitleFactory(type='row', label='row1', survey_question=question)
        row2 = SurveyQuestionMatrixTitleFactory(type='row', label='row2', survey_question=question)
        col1 = SurveyQuestionMatrixTitleFactory(type='column', label='1', survey_question=question)
        col2 = SurveyQuestionMatrixTitleFactory(type='column', label='2', survey_question=question)
        col3 = SurveyQuestionMatrixTitleFactory(type='column', label='3', survey_question=question)

        data = {
            'matrix_titles': [{
                'survey_question_id': question.pk,
                'id': row1.pk,
                'type': row1.type,
                'label': row1.label,
            },
            {
                'survey_question_id': question.pk,
                'id': col1.pk,
                'type': col1.type,
                'label': col1.label,
            },
            {
                'survey_question_id': question.pk,
                'id': col2.pk,
                'type': col2.type,
                'label': col2.label,
            }],
        }

        response = self.client.patch('/admin/api/v2/survey-questions/%s/' % question.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)

        self.assertTrue(SurveyQuestionMatrixTitle.with_deleted_objects.filter(pk=row2.pk).exists())
        self.assertTrue(SurveyQuestionMatrixTitle.with_deleted_objects.filter(pk=col3.pk).exists())
        self.assertFalse(SurveyQuestionMatrixTitle.objects.filter(pk=row2.pk).exists())
        self.assertFalse(SurveyQuestionMatrixTitle.objects.filter(pk=col3.pk).exists())
