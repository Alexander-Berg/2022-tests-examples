# -*- coding: utf-8 -*-
import json
import random
import yenv

from datetime import datetime, date, timedelta
from unittest.mock import patch, ANY
from guardian.shortcuts import assign_perm
from django.utils.timezone import make_aware, now
from django.utils.encoding import force_str
from django.conf import settings
from django.test import TestCase, override_settings
from django.contrib.contenttypes.models import ContentType

from events.accounts.factories import UserFactory, OrganizationToGroupFactory
from events.accounts.helpers import YandexClient
from events.accounts.models import User
from events.common_storages.utils import get_mds_url
from events.surveyme.factories import (
    ProfileSurveyAnswerFactory,
    SurveyFactory,
    SurveyQuestionFactory,
    SurveyQuestionChoiceFactory,
    SurveySubmitConditionNodeItemFactory,
    SurveySubmitConditionNodeFactory,
    SurveyQuestionShowConditionNodeFactory,
    SurveyQuestionShowConditionNodeItemFactory,
    SurveyQuestionMatrixTitleFactory,
    SurveyGroupFactory,
    SurveyStyleTemplateFactory,
)

from events.conditions.factories import ContentTypeAttributeFactory
from events.countme.factories import QuestionCountFactory
from events.media.factories import ImageFactory
from events.surveyme.dataclasses import SurveyQuiz
from events.surveyme.export_answers_v2 import MdsUploader
from events.surveyme.models import (
    AnswerType,
    SurveyQuestion,
    SurveySubmitConditionNode,
    ProfileSurveyAnswer,
    Survey,
    SurveyGroup,
    SurveyStyleTemplate,
    SurveyQuestionChoice,
    SurveyQuestionMatrixTitle,
    ValidatorType,
)
from events.surveyme_integration.factories import (
    HookSubscriptionNotificationFactory,
    ServiceSurveyHookSubscriptionFactory,
    SurveyHookFactory,
)
from events.surveyme_integration.models import SurveyVariable
from events.surveyme.api_admin.v2.serializers import AnswerParamsSerializer
from events.tanker.factories import TankerKeysetFactory
from events.tanker import utils as tanker_utils
from events.history.models import HistoryRawEntry


class TestSurveyBehavior_submit_conditions(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory(is_published_external=True)
        answer_choices_type = AnswerType.objects.get(slug='answer_choices')
        answer_date = AnswerType.objects.get(slug='answer_date')
        self.questions = [
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=answer_choices_type
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=answer_choices_type
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=answer_choices_type
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

        # create question choices for third question
        SurveyQuestionChoiceFactory(survey_question=self.questions[2], label='5')
        SurveyQuestionChoiceFactory(survey_question=self.questions[2], label='6')

        # create condition node for two last questions
        self.node_1 = SurveySubmitConditionNodeFactory(survey=self.survey)
        self.node_2 = SurveySubmitConditionNodeFactory(survey=self.survey)

        self.content_type_attribute = ContentTypeAttributeFactory(
            title='test',
            attr='answer_short_text',
        )

        # create conditions for first question
        SurveySubmitConditionNodeItemFactory(
            position=1,
            survey_submit_condition_node=self.node_1,
            operator='and',
            condition='eq',
            survey_question=self.questions[0],
            content_type_attribute_id=self.content_type_attribute.pk,
            value=self.questions[0].surveyquestionchoice_set.all()[0],
        )
        SurveySubmitConditionNodeItemFactory(
            position=2,
            survey_submit_condition_node=self.node_1,
            operator='and',
            condition='eq',
            survey_question=self.questions[0],
            content_type_attribute_id=self.content_type_attribute.pk,
            value=self.questions[0].surveyquestionchoice_set.all()[1],
        )

        # create conditions for second question
        SurveySubmitConditionNodeItemFactory(
            position=1,
            survey_submit_condition_node=self.node_2,
            operator='and',
            condition='eq',
            survey_question=self.questions[1],
            content_type_attribute_id=self.content_type_attribute.pk,
            value=self.questions[1].surveyquestionchoice_set.all()[0],
        )
        SurveySubmitConditionNodeItemFactory(
            position=2,
            survey_submit_condition_node=self.node_2,
            operator='and',
            condition='neq',
            survey_question=self.questions[0],
            content_type_attribute_id=self.content_type_attribute.pk,
            value=self.questions[0].surveyquestionchoice_set.all()[0],
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
        SurveySubmitConditionNodeItemFactory(
            position=3,
            survey_submit_condition_node=self.node_2,
            operator='or',
            condition='eq',
            content_type_attribute=date_start_content_type_attribute,
            survey_question=self.questions[3],
            value=self.exp_date,
        )
        SurveySubmitConditionNodeItemFactory(
            position=4,
            survey_submit_condition_node=self.node_2,
            operator='or',
            condition='gt',
            content_type_attribute=date_end_content_type_attribute,
            survey_question=self.questions[3],
            value=self.exp_date,
        )

    def test_should_add_conditions_info_to_survey_detail_info(self):
        response = self.client.get('/v1/surveys/{id}/'.format(id=self.survey.id)).data
        expected = [
            [
                {
                    'condition': 'eq',
                    'field': force_str(self.questions[0].get_form_field_name()),
                    'field_value': force_str(self.questions[0].surveyquestionchoice_set.all()[0].id),
                    'operator': 'and',
                },
                {
                    'condition': 'eq',
                    'field': force_str(self.questions[0].get_form_field_name()),
                    'field_value': force_str(self.questions[0].surveyquestionchoice_set.all()[1].id),
                    'operator': 'and',
                },
            ],
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
                    'field': force_str(self.questions[0].get_form_field_name()),
                    'field_value': force_str(self.questions[0].surveyquestionchoice_set.all()[0].id)
                },
                {
                    'operator': 'or',
                    'condition': 'eq',
                    'field': force_str(self.questions[3].get_form_field_name()),
                    'field_value': self.exp_date,
                    'tag_index': 0,
                },
                {
                    'operator': 'or',
                    'condition': 'gt',
                    'field': force_str(self.questions[3].get_form_field_name()),
                    'field_value': self.exp_date,
                    'tag_index': 1,
                },
            ]
        ]
        self.assertTrue('allow_post_conditions' in response)
        self.assertEqual(response['allow_post_conditions'], expected)

    def test_submit_without_conditions(self):
        SurveySubmitConditionNode.objects.all().delete()
        response = self.client.get('/v1/surveys/{id}/'.format(id=self.survey.id)).data
        msg = 'Если нет условий - поле allow_post_conditions должно быть пустым'
        self.assertEqual(response['allow_post_conditions'], [], msg=msg)
        for labels_list in [['1', '3', '5', '2015-01-01', '2015-01-02'], ['2', '4', '6', '2015-01-01', '2015-01-02']]:
            data = {
                self.questions[0].get_form_field_name(): labels_list[0],
                self.questions[1].get_form_field_name(): labels_list[1],
                self.questions[2].get_form_field_name(): labels_list[2],
                '%s_0' % self.questions[3].get_form_field_name(): labels_list[3],
                '%s_1' % self.questions[3].get_form_field_name(): labels_list[4],
            }
            response = self.client.post('/v1/surveys/{id}/form/'.format(id=self.survey.id), data)
            self.assertEqual(response.status_code, 200)

    def test_should_allow_post_if_one_of_nodes_are_true(self):
        data = {
            self.questions[0].get_form_field_name(): self.questions[0].surveyquestionchoice_set.all()[1],
            self.questions[1].get_form_field_name(): self.questions[1].surveyquestionchoice_set.all()[0],
            self.questions[2].get_form_field_name(): self.questions[2].surveyquestionchoice_set.all()[0],
        }
        response = self.client.post('/v1/surveys/{id}/form/'.format(id=self.survey.id), data)
        msg = 'Форма должна быть отправлена успешно, если условия сабмита соблюдены'
        self.assertEqual(response.status_code, 200, msg=msg)

    def test_should_allow_post_if_one_of_nodes_are_true_test_daterange_start(self):
        data = {
            self.questions[0].get_form_field_name(): self.questions[0].surveyquestionchoice_set.all()[0],
            self.questions[1].get_form_field_name(): self.questions[1].surveyquestionchoice_set.all()[0],
            self.questions[2].get_form_field_name(): self.questions[2].surveyquestionchoice_set.all()[0],
            '%s_0' % self.questions[3].get_form_field_name(): self.exp_date,
            '%s_1' % self.questions[3].get_form_field_name(): '2100-01-01',
        }
        response = self.client.post('/v1/surveys/{id}/form/'.format(id=self.survey.id), data)
        msg = 'Форма должна быть отправлена успешно, если условия сабмита соблюдены'
        self.assertEqual(response.status_code, 200, msg=msg)

    def test_should_allow_post_if_one_of_nodes_are_true_test_daterange_end(self):
        data = {
            self.questions[0].get_form_field_name(): self.questions[0].surveyquestionchoice_set.all()[0].pk,
            self.questions[1].get_form_field_name(): self.questions[1].surveyquestionchoice_set.all()[0].pk,
            self.questions[2].get_form_field_name(): self.questions[2].surveyquestionchoice_set.all()[0].pk,
            '%s_0' % self.questions[3].get_form_field_name(): '1900-01-01',
            '%s_1' % self.questions[3].get_form_field_name(): '2100-01-01',
        }
        response = self.client.post('/v1/surveys/{id}/form/'.format(id=self.survey.id), data)
        msg = 'Форма должна быть отправлена успешно, если условия сабмита соблюдены'
        self.assertEqual(response.status_code, 200, msg=msg)

    def test_should_not_allow_post_if_all_nodes_are_false(self):
        data = {
            self.questions[0].get_form_field_name(): self.questions[0].surveyquestionchoice_set.all()[0],
            self.questions[1].get_form_field_name(): self.questions[1].surveyquestionchoice_set.all()[0],
            self.questions[2].get_form_field_name(): self.questions[2].surveyquestionchoice_set.all()[0],
        }
        response = self.client.post('/v1/surveys/{id}/form/'.format(id=self.survey.id), data)
        msg = 'Если условия сабмита вернули False - нужно отдать код 400'
        self.assertEqual(response.status_code, 400, msg=msg)


class TestSurveyBehavior_submit_conditions_for_boolean_field(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory(is_published_external=True)
        answer_boolean_type = AnswerType.objects.get(slug='answer_boolean')
        self.questions = [
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=answer_boolean_type,
                param_is_required=False,
            ),
        ]

        self.node_1 = SurveySubmitConditionNodeFactory(survey=self.survey)

        self.content_type_attribute = ContentTypeAttributeFactory(
            title='test',
            attr='answer_boolean',
        )

        SurveySubmitConditionNodeItemFactory(
            position=1,
            survey_submit_condition_node=self.node_1,
            operator='and',
            condition='eq',
            survey_question=self.questions[0],
            content_type_attribute_id=self.content_type_attribute.pk,
            value=True,
        )

    def test_should_add_conditions_info_to_survey_detail_info(self):
        response = self.client.get('/v1/surveys/{id}/'.format(id=self.survey.id)).data
        expected = [
            [
                {
                    'condition': 'eq',
                    'field': force_str(self.questions[0].get_form_field_name()),
                    'field_value': 'True',
                    'operator': 'and',
                },
            ],
        ]
        self.assertTrue('allow_post_conditions' in response)
        self.assertEqual(response['allow_post_conditions'], expected)

    def test_submit_without_conditions(self):
        SurveySubmitConditionNode.objects.all().delete()
        response = self.client.get('/v1/surveys/{id}/'.format(id=self.survey.id)).data
        msg = 'Если нет условий - поле allow_post_conditions должно быть пустым'
        self.assertEqual(response['allow_post_conditions'], [], msg=msg)

        data = {
            self.questions[0].get_form_field_name(): False,
        }
        response = self.client.post('/v1/surveys/{id}/form/'.format(id=self.survey.id), data)
        self.assertEqual(response.status_code, 200)

        data = {
            self.questions[0].get_form_field_name(): True,
        }
        response = self.client.post('/v1/surveys/{id}/form/'.format(id=self.survey.id), data)
        self.assertEqual(response.status_code, 200)

    def test_submit_with_conditions(self):
        data = {
            self.questions[0].get_form_field_name(): False,
        }
        response = self.client.post('/v1/surveys/{id}/form/'.format(id=self.survey.id), data)
        msg = 'Если по условиям сабмита форма не должна быть отправлена - надо вернуть 400'
        self.assertEqual(response.status_code, 400, msg=msg)

        data = {
            self.questions[0].get_form_field_name(): True,
        }
        response = self.client.post('/v1/surveys/{id}/form/'.format(id=self.survey.id), data)
        self.assertEqual(response.status_code, 200)


class TestSurveyBehavior_submit_conditions_for_data_sources(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.survey = SurveyFactory(is_published_external=True)
        answer_choices_type = AnswerType.objects.get(slug='answer_choices')
        self.questions = [
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=answer_choices_type
            ),
        ]

        self.choice_1 = SurveyQuestionChoiceFactory(survey_question=self.questions[0], label='1')
        self.choice_2 = SurveyQuestionChoiceFactory(survey_question=self.questions[0], label='2')

        self.node_1 = SurveySubmitConditionNodeFactory(survey=self.survey)

        self.content_type_attribute = ContentTypeAttributeFactory(
            title='test',
            attr='answer_choices',
        )

        # create conditions for first question
        SurveySubmitConditionNodeItemFactory(
            position=1,
            survey_submit_condition_node=self.node_1,
            operator='and',
            condition='eq',
            survey_question=self.questions[0],
            content_type_attribute_id=self.content_type_attribute.pk,
            value=self.choice_1.pk,
        )

    def test_should_add_conditions_info_to_survey_detail_info(self):
        response = self.client.get('/v1/surveys/{id}/'.format(id=self.survey.id)).data
        expected = [
            [
                {
                    'condition': 'eq',
                    'field': force_str(self.questions[0].get_form_field_name()),
                    'field_value': force_str(self.choice_1.pk),
                    'operator': 'and',
                },
            ],
        ]
        self.assertTrue('allow_post_conditions' in response)
        self.assertEqual(response['allow_post_conditions'], expected)

    def test_submit_without_conditions(self):
        SurveySubmitConditionNode.objects.all().delete()
        response = self.client.get('/v1/surveys/{id}/'.format(id=self.survey.id)).data
        msg = 'Если нет условий - поле allow_post_conditions должно быть пустым'
        self.assertEqual(response['allow_post_conditions'], [], msg=msg)

        data = {
            self.questions[0].get_form_field_name(): self.choice_2.pk,
        }
        response = self.client.post('/v1/surveys/{id}/form/'.format(id=self.survey.id), data)
        self.assertEqual(response.status_code, 200)

    def test_submit_with_conditions(self):
        data = {
            self.questions[0].get_form_field_name(): self.choice_2.pk,
        }
        response = self.client.post('/v1/surveys/{id}/form/'.format(id=self.survey.id), data)
        msg = 'Если по условиям сабмита форма не должна быть отправлена - надо вернуть 400'
        self.assertEqual(response.status_code, 400, msg=msg)

        data = {
            self.questions[0].get_form_field_name(): self.choice_1.pk,
        }
        response = self.client.post('/v1/surveys/{id}/form/'.format(id=self.survey.id), data)
        self.assertEqual(response.status_code, 200)


class TestSurveyBehavior_submit_conditions_for_answer_short_text(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.survey = SurveyFactory(is_published_external=True)
        answer_short_text = AnswerType.objects.get(slug='answer_short_text')
        self.questions = [
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=answer_short_text,
                param_is_required=False,
            ),
        ]

        self.node_1 = SurveySubmitConditionNodeFactory(survey=self.survey)

        self.content_type_attribute = ContentTypeAttributeFactory(
            title='test content type attribute',
            attr='answer_short_text',
        )
        self.exc_answer_value = 'ping'

        # create conditions for first question
        SurveySubmitConditionNodeItemFactory(
            position=1,
            survey_submit_condition_node=self.node_1,
            operator='and',
            condition='eq',
            survey_question=self.questions[0],
            content_type_attribute_id=self.content_type_attribute.pk,
            value=self.exc_answer_value,
        )

    def test_should_add_conditions_info_to_survey_detail_info(self):
        response = self.client.get('/v1/surveys/{id}/'.format(id=self.survey.id)).data
        expected = [
            [
                {
                    'condition': 'eq',
                    'field': force_str(self.questions[0].get_form_field_name()),
                    'field_value': force_str(self.exc_answer_value),
                    'operator': 'and',
                },
            ],
        ]
        msg = 'Если заданы условия показа кнопки отправки - они должны быть в данных формы'
        self.assertTrue('allow_post_conditions' in response, msg=msg)
        self.assertEqual(response['allow_post_conditions'], expected, msg=msg)

    def test_submit_without_conditions(self):
        data = {
            self.questions[0].get_form_field_name(): 'pong',
        }
        response = self.client.post('/v1/surveys/{id}/form/'.format(id=self.survey.id), data)
        self.assertEqual(response.status_code, 400)

        SurveySubmitConditionNode.objects.all().delete()
        response = self.client.get('/v1/surveys/{id}/'.format(id=self.survey.id)).data
        msg = 'Если нет условий - поле allow_post_conditions должно быть пустым'
        self.assertEqual(response['allow_post_conditions'], [], msg=msg)

        response = self.client.post('/v1/surveys/{id}/form/'.format(id=self.survey.id), data)
        self.assertEqual(response.status_code, 200)

    def test_submit_with_conditions(self):
        data = {
            self.questions[0].get_form_field_name(): 'pong',
        }
        response = self.client.post('/v1/surveys/{id}/form/'.format(id=self.survey.id), data)
        msg = 'Если по условиям сабмита форма не должна быть отправлена - надо вернуть 400'
        self.assertEqual(response.status_code, 400, msg=msg)

        data = {
            self.questions[0].get_form_field_name(): self.exc_answer_value,
        }
        response = self.client.post('/v1/surveys/{id}/form/'.format(id=self.survey.id), data)
        msg = 'Если данные ответа удовлетворяют условиям сабмита - нужно вернуть 200'
        self.assertEqual(response.status_code, 200, msg=msg)


class TestSurveyQuestionCopy(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory()
        answer_short_text = AnswerType.objects.get(slug='answer_short_text')
        answer_choices_type = AnswerType.objects.get(slug='answer_choices')
        self.questions = [
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=answer_choices_type,
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=answer_short_text,
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=answer_short_text,
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=answer_choices_type,
                param_data_source='survey_question_matrix_choice',
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=answer_choices_type,
            ),
        ]

        self.questions[0].param_data_source = 'survey_question_choice'
        self.questions[0].param_data_source_params = {
            'filters': [{
                'filter': {'name': 'question'},
                'type': 'specified_value',
                'value': self.questions[0].pk,
            }]
        }
        self.questions[0].save()
        self.choices = [
            SurveyQuestionChoiceFactory(
                survey_question=self.questions[0],
                label='one',
            ),
            SurveyQuestionChoiceFactory(
                survey_question=self.questions[0],
                label='two',
            ),
        ]

        self.matrix_titles = [
            SurveyQuestionMatrixTitleFactory(
                survey_question=self.questions[3],
                type='row',
                label='row1',
            ),
            SurveyQuestionMatrixTitleFactory(
                survey_question=self.questions[3],
                type='row',
                label='row2',
            ),
            SurveyQuestionMatrixTitleFactory(
                survey_question=self.questions[3],
                type='column',
                label='column1',
            ),
            SurveyQuestionMatrixTitleFactory(
                survey_question=self.questions[3],
                type='column',
                label='column2',
            ),
        ]

        self.condition_node = SurveyQuestionShowConditionNodeFactory(survey_question=self.questions[2])
        self.content_type_attribute = ContentTypeAttributeFactory(
            title='test',
            attr='answer_short_text',
        )
        self.conditions = [
            SurveyQuestionShowConditionNodeItemFactory(
                position=1,
                survey_question_show_condition_node=self.condition_node,
                operator='and',
                condition='eq',
                content_type_attribute=self.content_type_attribute,
                survey_question=self.questions[0],
                value=self.questions[0].surveyquestionchoice_set.all()[0],
            ),
            SurveyQuestionShowConditionNodeItemFactory(
                position=2,
                survey_question_show_condition_node=self.condition_node,
                operator='and',
                condition='eq',
                content_type_attribute=self.content_type_attribute,
                survey_question=self.questions[0],
                value=self.questions[0].surveyquestionchoice_set.all()[1],
            ),
        ]

        self.questions[4].param_data_source = 'city'
        self.questions[4].param_data_source_params = {
            'filters': [{
                'filter': {
                    'data_source': 'country',
                    'name': 'country',
                },
                'type': 'specified_value',
                'value': '23',
            }]
        }
        self.questions[4].save()

        self.url = '/admin/api/v2/survey-questions/%s/copy/'

    def test_copy_first_question__field_with_choices(self):
        old_question = self.questions[0]
        response = self.client.post(self.url % old_question.pk)
        self.assertEqual(201, response.status_code)
        question = SurveyQuestion.objects.get(id=response.data['id'])

        self.assertEqual(old_question.survey.pk, question.survey_id)
        self.assertEqual(old_question.answer_type.pk, question.answer_type_id)
        self.assertNotEqual(old_question.param_slug, question.param_slug)
        self.assertIn(old_question.label, question.label)
        self.assertEqual(len(self.questions) + 1, question.position)

        ids = set(c.id for c in SurveyQuestionChoice.objects.filter(survey_question_id=question.pk))
        self.assertNotIn(self.choices[0].pk, ids)
        self.assertNotIn(self.choices[1].pk, ids)

        labels = set(c.label for c in SurveyQuestionChoice.objects.filter(survey_question_id=question.pk))
        self.assertIn(self.choices[0].label, labels)
        self.assertIn(self.choices[1].label, labels)

        self.assertIsNotNone(old_question.param_data_source_params)
        question = SurveyQuestion.objects.get(pk=question.id)
        self.assertIsNotNone(question.param_data_source_params)
        self.assertNotEqual(old_question.param_data_source_params, question.param_data_source_params)

    def test_copy_second_question__short_text(self):
        old_question = self.questions[1]
        response = self.client.post(self.url % old_question.pk)
        self.assertEqual(201, response.status_code)
        question = SurveyQuestion.objects.get(id=response.data['id'])

        self.assertEqual(old_question.survey.pk, question.survey_id)
        self.assertEqual(old_question.answer_type.pk, question.answer_type_id)
        self.assertNotEqual(old_question.param_slug, question.param_slug)
        self.assertIn(old_question.label, question.label)
        self.assertEqual(len(self.questions) + 1, question.position)

    def test_copy_third_question__short_text_with_conditions(self):
        old_question = self.questions[2]
        response = self.client.post(self.url % old_question.pk)
        self.assertEqual(201, response.status_code)
        question = SurveyQuestion.objects.get(id=response.data['id'])

        self.assertEqual(old_question.survey.pk, question.survey_id)
        self.assertEqual(old_question.answer_type.pk, question.answer_type_id)
        self.assertNotEqual(old_question.param_slug, question.param_slug)
        self.assertIn(old_question.label, question.label)
        self.assertEqual(len(self.questions) + 1, question.position)

        new_question = SurveyQuestion.objects.get(pk=question.id)

        self.assertEqual(old_question.show_condition_nodes.count(), new_question.show_condition_nodes.count())
        old_node = old_question.show_condition_nodes.first()
        new_node = new_question.show_condition_nodes.first()

        self.assertNotEqual(old_node.pk, new_node.pk)

        self.assertEqual(old_node.items.count(), new_node.items.count())
        old_items = list(old_node.items.all())
        new_items = list(new_node.items.all())

        self.assertNotEqual(old_items[0].pk, new_items[0].pk)
        self.assertEqual(old_items[0].operator, new_items[0].operator)
        self.assertEqual(old_items[0].condition, new_items[0].condition)
        self.assertEqual(old_items[0].value, new_items[0].value)

        self.assertNotEqual(old_items[1].pk, new_items[1].pk)
        self.assertEqual(old_items[1].operator, new_items[1].operator)
        self.assertEqual(old_items[1].condition, new_items[1].condition)
        self.assertEqual(old_items[1].value, new_items[1].value)

    def test_copy_fourth_question__field_with_matrix_titles(self):
        old_question = self.questions[3]
        response = self.client.post(self.url % old_question.pk)
        self.assertEqual(201, response.status_code)
        question = SurveyQuestion.objects.get(id=response.data['id'])
        self.assertEqual(old_question.survey.pk, question.survey_id)
        self.assertEqual(old_question.answer_type.pk, question.answer_type_id)
        self.assertNotEqual(old_question.param_slug, question.param_slug)
        self.assertIn(old_question.label, question.label)
        self.assertEqual(len(self.questions) + 1, question.position)

        ids = set(c.id for c in SurveyQuestionMatrixTitle.objects.filter(survey_question_id=question.pk))
        self.assertNotIn(self.matrix_titles[0].pk, ids)
        self.assertNotIn(self.matrix_titles[1].pk, ids)
        self.assertNotIn(self.matrix_titles[2].pk, ids)
        self.assertNotIn(self.matrix_titles[3].pk, ids)

        labels = set(c.label for c in SurveyQuestionMatrixTitle.objects.filter(survey_question_id=question.pk))
        self.assertIn(self.matrix_titles[0].label, labels)
        self.assertIn(self.matrix_titles[1].label, labels)
        self.assertIn(self.matrix_titles[2].label, labels)
        self.assertIn(self.matrix_titles[3].label, labels)

    def test_copy_fifth_question__field_with_complex_filter(self):
        old_question = self.questions[4]
        response = self.client.post(self.url % old_question.pk)
        self.assertEqual(201, response.status_code)
        question = SurveyQuestion.objects.get(id=response.data['id'])
        self.assertEqual(old_question.survey.pk, question.survey_id)
        self.assertEqual(old_question.answer_type.pk, question.answer_type_id)
        self.assertNotEqual(old_question.param_slug, question.param_slug)

        self.assertIsNotNone(old_question.param_data_source_params)
        self.assertIsNotNone(question.param_data_source_params)
        self.assertEqual(old_question.param_data_source_params, question.param_data_source_params)


class TestSurveyQuestionLangDetect(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory(is_published_external=True)
        self.answer_short_text = AnswerType.objects.get(slug='answer_short_text')
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=self.answer_short_text,
            label='label_ru',
            translations={
                'label': {
                    'ru': 'label_ru',
                    'en': 'label_en',
                },
            }
        )
        self.url = '/admin/api/v2/survey-questions/%s/' % self.question.pk
        self.form_url = '/v1/surveys/%s/form/' % self.survey.pk

    def create_org(self, user):
        o2g = OrganizationToGroupFactory()
        o2g.group.user_set.add(user)
        return o2g.org

    @override_settings(
        IS_BUSINESS_SITE=True,
    )
    def test_patch_question_for_business_always_should_be_russian(self):
        label_text = 'Some label text'
        data = {
            'label': label_text,
        }
        self.create_org(self.user)
        response = self.client.patch(self.url, data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['label'], label_text)

        response = self.client.patch(self.url, data=data, HTTP_ACCEPT_LANGUAGE='en', format='json')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['label'], label_text)

    def test_patch_question_for_none_business_should_use_default_method(self):
        label_text = 'Some label text'
        data = {
            'label': label_text,
        }
        response = self.client.patch(self.url, data=data, HTTP_ACCEPT_LANGUAGE='en', format='json')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['label'], label_text)

    @override_settings(
        IS_BUSINESS_SITE=True,
    )
    def test_get_form_for_business_always_should_be_russian(self):
        org = self.create_org(self.user)
        self.survey.org = org
        self.survey.save()

        response = self.client.get(self.form_url, HTTP_X_ORGS=str(org.dir_id))
        self.assertEqual(response.status_code, 200)
        question = response.data['fields'][self.question.param_slug]
        self.assertEqual(question['label'], 'label_ru')

        response = self.client.get(self.form_url, HTTP_ACCEPT_LANGUAGE='en', HTTP_X_ORGS=str(org.dir_id))
        self.assertEqual(response.status_code, 200)
        question = response.data['fields'][self.question.param_slug]
        self.assertEqual(question['label'], 'label_en')

    def test_get_form_for_none_business_should_use_default_method(self):
        response = self.client.get(self.form_url)
        self.assertEqual(response.status_code, 200)
        question = response.data['fields'][self.question.param_slug]
        self.assertEqual(question['label'], 'label_ru')

        response = self.client.get(self.form_url, HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(response.status_code, 200)
        question = response.data['fields'][self.question.param_slug]
        self.assertEqual(question['label'], 'label_en')


class TestSurveyQuestionsHasLogic(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory()
        self.questions = [
            SurveyQuestionFactory(survey=self.survey),
            SurveyQuestionFactory(survey=self.survey),
        ]
        self.question_on_logic = self.questions[0]
        self.question_with_logic = self.questions[1]
        self.url = '/admin/api/v2/survey-questions/%s/' % self.question_with_logic.pk

    def test_has_logic_equals_true_if_has_conditions(self):
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

        response = self.client.get(self.url)
        self.assertEqual(response.status_code, 200)
        question_result = response.data
        self.assertTrue(question_result['param_has_logic'])

    def test_has_logic_equals_false_if_question_on_logic_deleted(self):
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

        response = self.client.delete('/admin/api/v2/survey-questions/%s/' % self.question_on_logic.pk)
        self.assertEqual(response.status_code, 204)

        response = self.client.get(self.url)
        self.assertEqual(response.status_code, 200)
        question_result = response.data
        self.assertFalse(question_result['param_has_logic'])

    def test_has_logic_equals_false__if_has_not_conditions(self):
        response = self.client.get(self.url)
        self.assertEqual(response.status_code, 200)
        question_result = response.data
        self.assertFalse(question_result['param_has_logic'])


class TestSurveyQuestionUpdateChoices(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_superuser=True)
        self.group = SurveyGroupFactory()
        self.another_group = SurveyGroupFactory()
        self.survey = SurveyFactory()
        self.another_survey = SurveyFactory()
        self.survey_ct = ContentType.objects.get_for_model(Survey)
        self.group_ct = ContentType.objects.get_for_model(SurveyGroup)
        self.answer_choices = AnswerType.objects.get(slug='answer_choices')
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=self.answer_choices,
        )
        self.url = '/admin/api/v2/survey-questions/%s/' % self.question.pk
        self.label_image = ImageFactory()

    def test_update_choices(self):
        data = {
            'choices': [
                {
                    'label': 'Foo',
                    'position': 1
                },
                {
                    'label': 'Bar',
                    'position': 2
                },
                {
                    'label': 'Baz',
                    'position': 3
                }
            ]
        }

        response = self.client.patch(self.url, data=data, format='json')
        self.assertEqual(response.status_code, 200)
        result = response.data

        expected = [it.get('label') for it in data.get('choices', [])]
        received = [it.get('label') for it in result.get('choices', [])]
        self.assertEqual(received, expected)

        self.survey = SurveyFactory(name='Форма')
        self.another_survey = SurveyFactory(name='Форма another')
        self.another_group = SurveyGroupFactory()
        self.group_ct = ContentType.objects.get_for_model(self.group)
        self.survey_ct = ContentType.objects.get_for_model(self.survey)

    def test_should_create_choice_with_image(self):
        data = {
            'choices': [
                {
                    'label': 'Foo',
                    'position': 1,
                    'label_image': self.label_image.id,
                },

            ]
        }

        response = self.client.patch(self.url, data=data, format='json')
        self.assertEqual(response.status_code, 200)
        result = response.data
        self.assertEqual(len(result['choices']), 1)
        choice = result['choices'][0]
        self.assertEqual(choice['label_image']['id'], self.label_image.id)

    def test_should_raise_if_image_not_exists(self):
        data = {
            'choices': [
                {
                    'label': 'Foo',
                    'position': 1,
                    'label_image': self.label_image.id + 1,
                },

            ]
        }

        response = self.client.patch(self.url, data=data, format='json')
        self.assertEqual(response.status_code, 400)
        self.assertEqual(SurveyQuestionChoice.objects.filter(survey_question=self.question).count(), 0)

    def test_should_remove_image_from_choice(self):
        choice_obj = SurveyQuestionChoiceFactory(
            survey_question=self.question, label='Some choice', label_image=self.label_image
        )
        self.assertEqual(choice_obj.label_image_id, self.label_image.id)
        response = self.client.get(self.url)
        self.assertEqual(response.status_code, 200)
        result = response.data
        self.assertEqual(result['choices'][0]['label_image']['name'], self.label_image.name)

        data = {
            'choices': [
                {
                    'id': choice_obj.id,
                    'label_image': None,
                },

            ]
        }

        response = self.client.patch(self.url, data=data, format='json')
        self.assertEqual(response.status_code, 200)
        result = response.data
        self.assertEqual(len(result['choices']), 1)
        choice = result['choices'][0]
        self.assertIsNone(choice['label_image'])
        choice_obj.refresh_from_db()
        self.assertIsNone(choice_obj.label_image)

    def test_should_add_survey_to_group(self):
        self.assertIsNone(self.survey.group)
        date_updated = self.survey.date_updated
        response = self.client.post(
            '/admin/api/v2/survey-groups/%s/change-surveys/' % self.group.pk,
            data={'surveys': [self.survey.id], 'action': 'add'},
            format='json',
        )
        survey = Survey.objects.get(pk=self.survey.id)
        self.assertEqual(survey.group, self.group)
        self.assertEqual(response.status_code, 200)
        self.assertGreater(survey.date_updated, date_updated)

    def test_should_fail_without_surveys(self):
        response = self.client.post(
            '/admin/api/v2/survey-groups/%s/change-surveys/' % self.group.pk,
            data={'action': 'add'},
            format='json',
        )
        self.assertEqual(response.json(), {"detail": "Передавайте форму и мероприятие в запросе"})
        self.assertEqual(response.status_code, 400)

    def test_should_fail_without_action(self):
        response = self.client.post(
            '/admin/api/v2/survey-groups/%s/change-surveys/' % self.group.pk,
            data={'surveys': [self.survey.id]},
            format='json',
        )
        self.assertEqual(response.json(), {"detail": "Передавайте форму и мероприятие в запросе"})
        self.assertEqual(response.status_code, 400)

    def test_should_fail_with_wrong_action(self):
        response = self.client.post(
            '/admin/api/v2/survey-groups/%s/change-surveys/' % self.group.pk,
            data={'surveys': [self.survey.id], 'action': 'smth'},
            format='json',
        )
        self.assertEqual(response.json(), {"action": "Unknown action: smth"})
        self.assertEqual(response.status_code, 400)

    def test_should_add_many_surveys(self):
        self.assertIsNone(self.survey.group)
        self.assertIsNone(self.another_survey.group)
        date_updated = [self.survey.date_updated, self.another_survey.date_updated]
        response = self.client.post(
            '/admin/api/v2/survey-groups/%s/change-surveys/' % self.group.pk,
            data={
                'surveys': [self.survey.id, self.another_survey.id],
                'action': 'add'
            },
            format='json',
        )
        self.assertEqual(response.status_code, 200)
        survey = Survey.objects.get(pk=self.survey.id)
        self.assertEqual(survey.group, self.group)
        self.assertGreater(survey.date_updated, date_updated[0])
        another_survey = Survey.objects.get(pk=self.another_survey.id)
        self.assertEqual(another_survey.group, self.group)
        self.assertGreater(another_survey.date_updated, date_updated[1])

    def test_should_change_surveys_group(self):
        self.survey.group = self.another_group
        self.survey.save()
        date_updated = self.survey.date_updated
        response = self.client.post(
            '/admin/api/v2/survey-groups/%s/change-surveys/' % self.group.pk,
            data={'surveys': [self.survey.id], 'action': 'add'},
            format='json',
        )
        self.assertEqual(response.status_code, 200)
        survey = Survey.objects.get(pk=self.survey.id)
        self.assertEqual(survey.group, self.group)
        self.assertGreater(survey.date_updated, date_updated)

    def test_should_create_history(self):
        response = self.client.post(
            '/admin/api/v2/survey-groups/%s/change-surveys/' % self.group.pk,
            data={'surveys': [self.survey.id], 'action': 'add'},
            format='json',
        )
        self.assertEqual(response.status_code, 200)

        history_qs = (
            HistoryRawEntry.objects.all()
            .filter(
                content_type=ContentType.objects.get_for_model(Survey),
                object_id=self.survey.pk,
            )
        )
        self.assertEqual(history_qs.count(), 1)

    def test_should_create_history_with_existing_group(self):
        self.survey.group = self.another_group
        self.survey.save()

        response = self.client.post(
            '/admin/api/v2/survey-groups/%s/change-surveys/' % self.group.pk,
            data={'surveys': [self.survey.id], 'action': 'add'},
            format='json',
        )
        self.assertEqual(response.status_code, 200)

        history_qs = (
            HistoryRawEntry.objects.all()
            .filter(
                content_type=ContentType.objects.get_for_model(Survey),
                object_id=self.survey.pk,
            )
        )
        self.assertEqual(history_qs.count(), 1)


class TestSurveyStyleTemplate(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory()
        self.url = '/admin/api/v2/surveys/%s/change-style/' % self.survey.id
        self.template_id = 2
        self.styles_template = SurveyStyleTemplate.objects.create(
            pk=self.template_id
        )

    def test_should_add_styles_template(self):
        self.assertIsNone(self.survey.styles_template)
        response = self.client.post(
            self.url,
            data={'template_id': self.template_id},
            format='json',
        )
        self.assertEqual(response.status_code, 200)
        self.survey.refresh_from_db()
        self.assertEqual(self.survey.styles_template, self.styles_template)

    def test_shouldnt_add_styles_template_with_wrong_id(self):
        self.assertIsNone(self.survey.styles_template)
        response = self.client.post(
            self.url,
            data={'template_id': 9999},
            format='json',
        )
        self.assertEqual(response.status_code, 400)
        self.assertEqual(
            response.content.decode(), '{"detail":"Шаблона с указанным кодом не существует"}',
        )
        self.survey.refresh_from_db()
        self.assertIsNone(self.survey.styles_template)

    def test_shouldnt_add_styles_template_without_id(self):
        self.assertIsNone(self.survey.styles_template)
        response = self.client.post(
            self.url,
            data={},
            format='json',
        )
        self.assertEqual(response.status_code, 400)
        self.assertEqual(
            response.content.decode(), '{"detail":"В запросе отсутствует код шаблона"}',
        )
        self.survey.refresh_from_db()
        self.assertIsNone(self.survey.styles_template)

    def test_should_change_styles_template(self):
        self.survey.styles_template = self.styles_template
        self.survey.save()
        template_id = 3
        another_styles_template = SurveyStyleTemplate.objects.create(
            pk=template_id
        )
        response = self.client.post(
            self.url,
            data={'template_id': template_id},
            format='json',
        )
        self.assertEqual(response.status_code, 200)
        self.survey.refresh_from_db()
        self.assertEqual(self.survey.styles_template, another_styles_template)

    def test_should_remove_styles_template(self):
        self.survey.styles_template = self.styles_template
        self.survey.save()

        response = self.client.post(
            self.url,
            data={'template_id': None},
            format='json',
        )
        self.assertEqual(response.status_code, 200)
        self.survey.refresh_from_db()
        self.assertIsNone(self.survey.styles_template)

    def test_should_not_fail_if_no_styles_template(self):
        response = self.client.post(
            self.url,
            data={'template_id': None},
            format='json',
        )
        self.assertEqual(response.status_code, 200)
        self.survey.refresh_from_db()
        self.assertIsNone(self.survey.styles_template)


class TestSurveyQuestionWithChoices(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory()
        self.url = '/admin/api/v2/survey-questions/'
        self.detail_url = '/admin/api/v2/survey-questions/%s/'
        self.answer_choices = AnswerType.objects.get(slug='answer_choices')
        self.answer_short_text = AnswerType.objects.get(slug='answer_short_text')

    def test_create_choices_with_question(self):
        data = {
            'answer_type_id': self.answer_choices.pk,
            'label': 'Choices',
            'survey_id': self.survey.pk,
            'choices': [{
                'label': 'One'
            }, {
                'label': 'Two'
            }]
        }
        response = self.client.post(self.url, data, format='json')
        self.assertEqual(response.status_code, 201)

        choices = response.data['choices']
        self.assertEqual(len(choices), 2)

        self.assertEqual(choices[0]['label'], 'One')
        self.assertEqual(choices[1]['label'], 'Two')

    def test_update_question_with_choices(self):
        question = SurveyQuestionFactory(survey=self.survey, answer_type=self.answer_choices, label='Choices')
        SurveyQuestionChoiceFactory(survey_question=question, label='One')
        SurveyQuestionChoiceFactory(survey_question=question, label='Two')

        SurveyQuestionFactory(survey=self.survey, answer_type=self.answer_short_text, label='ShortText')

        data = {
            'position': 2,
        }
        response = self.client.patch(self.detail_url % question.pk, data, format='json')
        self.assertEqual(response.status_code, 200)

        self.assertEqual(response.data['position'], 2)

        choices = response.data['choices']
        self.assertEqual(len(choices), 2)

        self.assertEqual(choices[0]['label'], 'One')
        self.assertEqual(choices[1]['label'], 'Two')


class TestSurveyQuestionWithMatrixTitles(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory()
        self.url = '/admin/api/v2/survey-questions/'
        self.detail_url = '/admin/api/v2/survey-questions/%s/'
        self.answer_choices = AnswerType.objects.get(slug='answer_choices')
        self.answer_short_text = AnswerType.objects.get(slug='answer_short_text')

    def test_create_matrix_titles_with_question(self):
        data = {
            'answer_type_id': self.answer_choices.pk,
            'label': 'Titles',
            'survey_id': self.survey.pk,
            'param_data_source': 'survey_question_matrix_choice',
            'matrix_titles': [{
                'label': 'Row',
                'type': 'row'
            }, {
                'label': 'One',
                'type': 'column'
            }, {
                'label': 'Two',
                'type': 'column'
            }, {
                'label': 'Three',
                'type': 'column'
            }]
        }
        response = self.client.post(self.url, data, format='json')
        self.assertEqual(response.status_code, 201)

        matrix_titles = response.data['matrix_titles']
        self.assertEqual(len(matrix_titles), 4)

        self.assertEqual(matrix_titles[0]['label'], 'Row')
        self.assertEqual(matrix_titles[0]['type'], 'row')

        self.assertEqual(matrix_titles[1]['label'], 'One')
        self.assertEqual(matrix_titles[1]['type'], 'column')

        self.assertEqual(matrix_titles[2]['label'], 'Two')
        self.assertEqual(matrix_titles[2]['type'], 'column')

        self.assertEqual(matrix_titles[3]['label'], 'Three')
        self.assertEqual(matrix_titles[3]['type'], 'column')

    def test_update_question_with_choices(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=self.answer_choices,
            label='Titles',
            param_data_source='survey_question_matrix_choice',
        )
        SurveyQuestionMatrixTitleFactory(survey_question=question, type='row', label='Row')
        SurveyQuestionMatrixTitleFactory(survey_question=question, type='column', label='One')
        SurveyQuestionMatrixTitleFactory(survey_question=question, type='column', label='Two')
        SurveyQuestionMatrixTitleFactory(survey_question=question, type='column', label='Three')

        SurveyQuestionFactory(survey=self.survey, answer_type=self.answer_short_text, label='ShortText')

        data = {
            'position': 2,
        }
        response = self.client.patch(self.detail_url % question.pk, data, format='json')
        self.assertEqual(response.status_code, 200)

        self.assertEqual(response.data['position'], 2)

        matrix_titles = response.data['matrix_titles']
        self.assertEqual(len(matrix_titles), 4)

        self.assertEqual(matrix_titles[0]['label'], 'Row')
        self.assertEqual(matrix_titles[0]['type'], 'row')

        self.assertEqual(matrix_titles[1]['label'], 'One')
        self.assertEqual(matrix_titles[1]['type'], 'column')

        self.assertEqual(matrix_titles[2]['label'], 'Two')
        self.assertEqual(matrix_titles[2]['type'], 'column')

        self.assertEqual(matrix_titles[3]['label'], 'Three')
        self.assertEqual(matrix_titles[3]['type'], 'column')


class TestSurveyCreateWithOrgsHeader(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_superuser=True)
        self.url = '/admin/api/v2/surveys/'
        self.data = {'name': 'some_survey'}

    def test_create_without_org(self):
        response = self.client.post(self.url, data=self.data, HTTP_X_ORGS='')
        self.assertEqual(response.status_code, 201)
        self.assertIsNone(response.json()['org_id'])
        survey = Survey.objects.first()
        self.assertIsNone(survey.org)

    def test_create_without_header(self):
        response = self.client.post(self.url, data=self.data)
        self.assertEqual(response.status_code, 201)
        self.assertIsNone(response.json()['org_id'])
        survey = Survey.objects.first()
        self.assertIsNone(survey.org)

    def test_create_with_org(self):
        response = self.client.post(self.url, data=self.data, HTTP_X_ORGS='123')
        self.assertEqual(response.status_code, 201)
        self.assertIsNone(response.json()['org_id'])
        survey = Survey.objects.first()
        self.assertIsNone(survey.org)


@override_settings(IS_BUSINESS_SITE=True)
class TestSurveyAccessWithOrgsHeader(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = self.client.login_yandex()
        o2g = OrganizationToGroupFactory()
        self.org = o2g.org
        self.dir_id = self.org.dir_id
        self.orgs_data = ['732']
        self.user.groups.add(o2g.group)

        self.survey = SurveyFactory(user=self.user, is_published_external=True)
        assign_perm('surveyme.change_survey', self.user, self.survey)
        self.url = '/admin/api/v2/surveys/'
        self.admin_url = f'/admin/api/v2/surveys/{self.survey.pk}/'
        self.front_url = f'/v1/surveys/{self.survey.pk}/'

    def test_access_admin_without_org(self):
        response = self.client.get(self.admin_url, HTTP_X_ORGS='')
        self.assertEqual(response.status_code, 200)

        self.survey.org = self.org
        self.survey.save()

        response = self.client.get(self.admin_url, HTTP_X_ORGS='')
        self.assertEqual(response.status_code, 404)

        self.survey.user = self.user
        self.survey.save()

        response = self.client.get(self.admin_url, HTTP_X_ORGS='')
        self.assertEqual(response.status_code, 404)

    def test_access_admin_from_author_without_org(self):
        self.survey.user = self.user
        self.survey.save()

        response = self.client.get(self.admin_url, HTTP_X_ORGS='')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['id'], self.survey.id)

    def test_access_admin_with_different_org(self):
        response = self.client.get(self.admin_url, HTTP_X_ORGS='234')
        self.assertEqual(response.status_code, 200)

        self.survey.org = self.org
        self.survey.save()

        response = self.client.get(self.admin_url, HTTP_X_ORGS='234')
        self.assertEqual(response.status_code, 404)

        self.survey.user = self.user
        self.survey.save()

        response = self.client.get(self.admin_url, HTTP_X_ORGS='234')
        self.assertEqual(response.status_code, 404)

    def test_access_admin_with_same_org(self):
        response = self.client.get(self.admin_url, HTTP_X_ORGS=self.dir_id)
        self.assertEqual(response.status_code, 200)

        self.survey.org = self.org
        self.survey.save()

        response = self.client.get(self.admin_url, HTTP_X_ORGS=self.dir_id)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['id'], self.survey.id)

    def test_access_admin_with_author_and_with_same_org(self):
        self.survey.user = self.user
        self.survey.org = self.org
        self.survey.save()

        response = self.client.get(self.admin_url, HTTP_X_ORGS=self.dir_id)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['id'], self.survey.id)

    def test_access_admin_with_author_and_with_another_org(self):
        self.survey.user = self.user
        self.survey.save()

        response = self.client.get(self.admin_url, HTTP_X_ORGS='234')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['id'], self.survey.id)

    def test_access_front_without_org(self):
        response = self.client.get(self.front_url, HTTP_X_ORGS='')
        self.assertEqual(response.status_code, 200)

        self.survey.org = self.org
        self.survey.save()

        response = self.client.get(self.front_url, HTTP_X_ORGS='')
        self.assertEqual(response.status_code, 404)

        self.survey.user = self.user
        self.survey.save()

        response = self.client.get(self.front_url, HTTP_X_ORGS='')
        self.assertEqual(response.status_code, 404)

        self.survey.is_public = True
        self.survey.save()
        response = self.client.get(self.front_url, HTTP_X_ORGS='')
        self.assertEqual(response.status_code, 200)

        self.survey.is_public = False
        self.survey.org = None
        self.survey.save()

        response = self.client.get(self.front_url, HTTP_X_ORGS='')
        self.assertEqual(response.status_code, 200)

    def test_access_front_without_org_preview(self):
        response = self.client.get(self.front_url, {'preview': '1'}, HTTP_X_ORGS='')
        self.assertEqual(response.status_code, 200)

        self.survey.org = self.org
        self.survey.save()

        response = self.client.get(self.front_url, {'preview': '1'}, HTTP_X_ORGS='')
        self.assertEqual(response.status_code, 404)

        self.survey.is_public = True
        self.survey.save()
        response = self.client.get(self.front_url, {'preview': '1'}, HTTP_X_ORGS='')
        self.assertEqual(response.status_code, 404)

        self.survey.user = self.user
        self.survey.save()

        response = self.client.get(self.front_url, {'preview': '1'}, HTTP_X_ORGS='')
        self.assertEqual(response.status_code, 404)

        self.survey.org = None
        self.survey.save()

        response = self.client.get(self.front_url, {'preview': '1'}, HTTP_X_ORGS='')
        self.assertEqual(response.status_code, 200)

    def test_access_front_without_org_public(self):
        self.survey.is_public = True
        self.survey.save()

        response = self.client.get(self.front_url, HTTP_X_ORGS='')
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.content.decode(response.charset))
        self.assertEqual(data['id'], self.survey.id)

    def test_access_front_with_org_public(self):
        self.survey.is_public = True
        self.survey.save()

        response = self.client.get(self.front_url, HTTP_X_ORGS='234')
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.content.decode(response.charset))
        self.assertEqual(data['id'], self.survey.id)

    def test_access_front_with_wrong_org(self):
        self.survey.org = self.org
        self.survey.save()

        response = self.client.get(self.front_url, HTTP_X_ORGS='234')
        self.assertEqual(response.status_code, 404)

    def test_access_front_with_correct_org(self):
        self.survey.org = self.org
        self.survey.save()

        response = self.client.get(self.front_url, HTTP_X_ORGS=self.dir_id)
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.content.decode(response.charset))
        self.assertEqual(data['id'], self.survey.id)


@override_settings(IS_BUSINESS_SITE=True)
class TestSurveySetOrg(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.dir_id = '123'
        self.orgs_data = ['732']
        with patch.object(User, '_get_organizations', return_value=self.orgs_data):
            self.user = self.client.login_yandex(is_superuser=True)
        self.url = '/admin/api/v2/surveys/'
        self.o2g = OrganizationToGroupFactory()
        self.org = self.o2g.org
        self.survey = SurveyFactory(user=self.user)
        self.admin_url = f'/admin/api/v2/surveys/{self.survey.pk}/'

    def test_set_org_from_request(self):
        data = {'org_id': '333'}
        response = self.client.patch(self.admin_url, data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.survey.refresh_from_db()
        self.assertEqual(self.survey.org.dir_id, '333')

    def test_set_org_from_request_if_already_set(self):
        self.survey.org = self.org
        self.survey.save()
        data = {'org_id': '333'}
        response = self.client.patch(self.admin_url, data=data, HTTP_X_ORGS=self.dir_id, format='json')
        self.assertEqual(response.status_code, 400)
        self.survey.refresh_from_db()
        self.assertEqual(self.survey.org, self.org)

    def test_remove_org_from_request_if_already_set(self):
        self.survey.org = self.org
        self.survey.save()
        data = {'org_id': None}
        response = self.client.patch(self.admin_url, data=data, HTTP_X_ORGS=self.dir_id, format='json')
        self.assertEqual(response.status_code, 400)
        self.survey.refresh_from_db()
        self.assertEqual(self.survey.org, self.org)

    def test_remove_org_from_request_if_not_set(self):
        data = {'org_id': None}
        self.assertIsNone(self.survey.org)
        response = self.client.patch(self.admin_url, data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.survey.refresh_from_db()
        self.assertIsNone(self.survey.org)

    def test_set_org_from_request_when_create(self):
        data = {'org_id': '333', 'name': 'smth'}
        response = self.client.post(self.url, data=data)
        self.assertEqual(response.status_code, 201)
        survey = Survey.objects.get(pk=response.json()['id'])
        self.assertEqual(survey.org.dir_id, '333')

    def test_set_org_from_data_even_with_header_when_create(self):
        data = {'org_id': '333', 'name': 'smth'}
        response = self.client.post(self.url, data=data, HTTP_X_ORGS=str('555'))
        self.assertEqual(response.status_code, 201)
        survey = Survey.objects.get(pk=response.json()['id'])
        self.assertEqual(survey.org.dir_id, '333')

    def test_set_org_from_header_when_create(self):
        data = {'name': 'smth'}
        response = self.client.post(self.url, data=data, HTTP_X_ORGS=str('555'))
        self.assertEqual(response.status_code, 201)
        survey = Survey.objects.get(pk=response.json()['id'])
        self.assertIsNone(survey.org)

    def test_not_set_org_from_header_when_create(self):
        data = {'name': 'smth', 'org_id': None}
        response = self.client.post(self.url, data=data, HTTP_X_ORGS=str('555'), format='json')
        self.assertEqual(response.status_code, 201)
        survey = Survey.objects.get(pk=response.json()['id'])
        self.assertIsNone(survey.org_id)


class TestSurveyBehavior_prepopulate_form(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory(is_published_external=True)
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
        )

    def test_submit_without_param(self):
        self.assertEqual(ProfileSurveyAnswer.objects.filter(survey=self.survey).count(), 0)
        data = {self.question.get_form_field_name(): 'test'}
        response = self.client.post('/v1/surveys/{id}/form/'.format(id=self.survey.id), data)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(ProfileSurveyAnswer.objects.filter(survey=self.survey).count(), 1)

    def test_submit_with_param(self):
        self.assertEqual(ProfileSurveyAnswer.objects.filter(survey=self.survey).count(), 0)
        data = {self.question.get_form_field_name(): 'test'}
        response = self.client.post(
            '/v1/surveys/{id}/form/?dry_run=True'.format(id=self.survey.id),
            data,
        )
        self.assertEqual(response.status_code, 200)
        self.assertEqual(ProfileSurveyAnswer.objects.filter(survey=self.survey).count(), 0)

        self.assertEqual(len(response.data['fields'][self.question.get_form_field_name()]['errors']), 0)

    def test_submit_with_param_return_with_validation(self):
        self.assertEqual(ProfileSurveyAnswer.objects.filter(survey=self.survey).count(), 0)

        self.question.validator_type = ValidatorType.objects.get(slug='inn')
        self.question.save()
        data = {self.question.get_form_field_name(): 'test'}
        response = self.client.post(
            '/v1/surveys/{id}/form/?dry_run=True'.format(id=self.survey.id),
            data,
        )
        self.assertEqual(response.status_code, 400)
        self.assertEqual(ProfileSurveyAnswer.objects.filter(survey=self.survey).count(), 0)

        self.assertEqual(len(response.data['fields'][self.question.get_form_field_name()]['errors']), 1)


class TestSurveyBehavior_get_form_with_style_template(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.styles_template = SurveyStyleTemplateFactory(
            styles={
                'display': 'none',
            },
        )
        self.survey = SurveyFactory(
            is_published_external=True,
            styles_template=self.styles_template,
        )
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
        )

    def test_get_form_data(self):
        response = self.client.get('/v1/surveys/%s/' % self.survey.pk)
        self.assertEqual(response.status_code, 200)

        data = json.loads(response.content.decode(response.charset))
        self.assertIn('styles_template', data)

        styles_template_data = data['styles_template']
        self.assertEqual(styles_template_data['id'], self.styles_template.pk)
        self.assertEqual(styles_template_data['type'], self.styles_template.type)
        self.assertEqual(styles_template_data['name'], self.styles_template.name)
        self.assertEqual(styles_template_data['styles'], self.styles_template.styles)


class TestSurveyStyleTemplateAdmin(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_superuser=True)
        self.styles_templates = [
            SurveyStyleTemplateFactory(
                type='default',
                styles={
                    'display': 'none',
                },
            ),
            SurveyStyleTemplateFactory(
                type='custom',
                styles={
                    'display': 'always',
                },
            ),
        ]

    def test_should_return_styles_template_list(self):
        response = self.client.get('/admin/api/v2/survey-style-templates/')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['count'], 2)

    def test_should_return_styles_template_filtered_list(self):
        response = self.client.get('/admin/api/v2/survey-style-templates/?type=default')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['count'], 1)
        self.assertEqual(response.data['results'][0]['id'], self.styles_templates[0].pk)

    def test_shouldnt_return_styles_template_filtered_list(self):
        response = self.client.get('/admin/api/v2/survey-style-templates/?type=custom')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['count'], 1)
        self.assertEqual(response.data['results'][0]['id'], self.styles_templates[1].pk)

    def test_should_return_specific_styles_template(self):
        response = self.client.get('/admin/api/v2/survey-style-templates/%s/' % self.styles_templates[0].pk)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['id'], self.styles_templates[0].pk)

    def test_should_create_new_styles_template(self):
        data = {
            'name': 'mint',
            'type': 'default',
            'styles': {
                'align': 'left',
            },
        }
        response = self.client.post('/admin/api/v2/survey-style-templates/', data=data, format='json')
        self.assertEqual(response.status_code, 201)
        self.assertEqual(response.data['name'], 'mint')
        self.assertEqual(response.data['type'], 'default')
        self.assertEqual(response.data['styles'], {
            'align': 'left',
        })

    def test_should_patch_existing_styles_template(self):
        data = {
            'styles': {
                'align': 'left',
            },
        }
        response = self.client.patch(
            '/admin/api/v2/survey-style-templates/%s/' % self.styles_templates[0].pk,
            data=data,
            format='json',
        )
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['styles'], {
            'align': 'left',
        })

        response = self.client.patch(
            '/admin/api/v2/survey-style-templates/%s/' % self.styles_templates[1].pk,
            data=data,
            format='json',
        )
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['styles'], {
            'align': 'left',
        })

    def test_should_patch_survey_with_styles_template(self):
        styles = [
            SurveyStyleTemplateFactory(name='Style1', type='default'),
            SurveyStyleTemplateFactory(name='Style2', type='custom'),
        ]
        survey = SurveyFactory()
        data = {
            'styles_template_id': styles[0].pk,
        }
        response = self.client.patch('/admin/api/v2/surveys/%s/' % survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['styles_template_id'], styles[0].pk)
        self.assertEqual(response.data['styles_template']['id'], styles[0].pk)
        self.assertEqual(response.data['styles_template']['template_id'], styles[0].pk)
        self.assertEqual(response.data['styles_template']['name'], styles[0].name)
        self.assertEqual(response.data['styles_template']['type'], styles[0].type)

        data = {
            'styles_template_id': styles[1].pk,
        }
        response = self.client.patch('/admin/api/v2/surveys/%s/' % survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['styles_template_id'], styles[1].pk)
        self.assertEqual(response.data['styles_template']['id'], styles[1].pk)
        self.assertEqual(response.data['styles_template']['template_id'], styles[1].pk)
        self.assertEqual(response.data['styles_template']['name'], styles[1].name)
        self.assertEqual(response.data['styles_template']['type'], styles[1].type)

        data = {
            'styles_template_id': None,
        }
        response = self.client.patch('/admin/api/v2/surveys/%s/' % survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['styles_template_id'], None)
        self.assertEqual(response.data['styles_template'], None)

        data = {
            'styles_template_id': random.randint(1000, 9999),
        }
        response = self.client.patch('/admin/api/v2/surveys/%s/' % survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 400)
        self.assertIn('styles_template_id', response.data)


class TestCreatePublishedForm(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_superuser=True)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_create_published_form_in_b2b(self):
        data = {
            'name': 'Test form',
        }
        response = self.client.post('/admin/api/v2/surveys/', data=data, format='json')
        self.assertEqual(response.status_code, 201)

        self.assertTrue(response.data['is_published_external'])
        self.assertIsNotNone(response.data['date_published'])

    @override_settings(IS_BUSINESS_SITE=False)
    def test_should_create_unpublished_form_in_int(self):
        data = {
            'name': 'Test form',
        }
        response = self.client.post('/admin/api/v2/surveys/', data=data, format='json')
        self.assertEqual(response.status_code, 201)

        self.assertFalse(response.data['is_published_external'])
        self.assertIsNone(response.data['date_published'])


class TestSurveyBehavior_redirect(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.survey = SurveyFactory()
        self.user = self.client.login_yandex(is_superuser=True)

    def test_redirect_should_be_none(self):
        response = self.client.get('/admin/api/v2/surveys/%s/' % self.survey.pk)
        self.assertEqual(response.status_code, 200)
        self.assertIsNone(response.data['redirect'])

        response = self.client.get('/admin/api/v2/surveys/%s/?detailed=0' % self.survey.pk)
        self.assertEqual(response.status_code, 200)
        self.assertIsNone(response.data['redirect'])

        response = self.client.get('/v1/surveys/%s/' % self.survey.pk)
        self.assertEqual(response.status_code, 200)
        self.assertIsNone(response.data['redirect'])

    def test_redirect_shouldnt_be_none(self):
        data = {
            'id': self.survey.pk,
            'redirect': {
                'enabled': False,
            },
        }
        response = self.client.patch('/admin/api/v2/surveys/%s/' % self.survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertIsNotNone(response.data['redirect'])
        self.assertFalse(response.data['redirect']['enabled'])

        response = self.client.get('/admin/api/v2/surveys/%s/' % self.survey.pk)
        self.assertEqual(response.status_code, 200)
        self.assertIsNotNone(response.data['redirect'])
        self.assertFalse(response.data['redirect']['enabled'])

    @override_settings(IS_BUSINESS_SITE=False)
    def test_redirect_should_be_valid(self):
        data = {
            'id': self.survey.pk,
            'redirect': {
                'enabled': True,
                'url': 'https://yandex.ru/',
                'auto_redirect': True,
                'with_delay': True,
                'timeout': 5000,
            },
        }
        response = self.client.patch('/admin/api/v2/surveys/%s/' % self.survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertIsNotNone(response.data['redirect'])
        self.assertDictEqual(response.data['redirect'], {
            'enabled': True,
            'url': 'https://yandex.ru/',
            'auto_redirect': True,
            'with_delay': True,
            'timeout': 5000,
            'keep_iframe': False,
        })

        response = self.client.get('/admin/api/v2/surveys/%s/' % self.survey.pk)
        self.assertEqual(response.status_code, 200)
        self.assertIsNotNone(response.data['redirect'])
        self.assertDictEqual(response.data['redirect'], {
            'enabled': True,
            'url': 'https://yandex.ru/',
            'auto_redirect': True,
            'with_delay': True,
            'timeout': 5000,
            'keep_iframe': False,
        })

        response = self.client.get('/v1/surveys/%s/' % self.survey.pk)
        self.assertEqual(response.status_code, 200)
        self.assertIsNotNone(response.data['redirect'])
        self.assertDictEqual(response.data['redirect'], {
            'enabled': True,
            'url': 'https://yandex.ru/',
            'auto_redirect': True,
            'with_delay': True,
            'timeout': 5000,
            'keep_iframe': False,
        })

    @override_settings(IS_BUSINESS_SITE=False)
    def test_redirect_should_be_valid_with_keep_iframe(self):
        data = {
            'id': self.survey.pk,
            'redirect': {
                'enabled': True,
                'url': 'https://yandex.ru/',
                'auto_redirect': True,
                'with_delay': True,
                'timeout': 5000,
                'keep_iframe': True,
            },
        }
        response = self.client.patch('/admin/api/v2/surveys/%s/' % self.survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertIsNotNone(response.data['redirect'])
        self.assertDictEqual(response.data['redirect'], {
            'enabled': True,
            'url': 'https://yandex.ru/',
            'auto_redirect': True,
            'with_delay': True,
            'timeout': 5000,
            'keep_iframe': True,
        })

        response = self.client.get('/admin/api/v2/surveys/%s/' % self.survey.pk)
        self.assertEqual(response.status_code, 200)
        self.assertIsNotNone(response.data['redirect'])
        self.assertDictEqual(response.data['redirect'], {
            'enabled': True,
            'url': 'https://yandex.ru/',
            'auto_redirect': True,
            'with_delay': True,
            'timeout': 5000,
            'keep_iframe': True,
        })

        response = self.client.get('/v1/surveys/%s/' % self.survey.pk)
        self.assertEqual(response.status_code, 200)
        self.assertIsNotNone(response.data['redirect'])
        self.assertDictEqual(response.data['redirect'], {
            'enabled': True,
            'url': 'https://yandex.ru/',
            'auto_redirect': True,
            'with_delay': True,
            'timeout': 5000,
            'keep_iframe': True,
        })

    @override_settings(IS_BUSINESS_SITE=False)
    def test_redirect_shouldnt_be_valid(self):
        data = {
            'id': self.survey.pk,
            'redirect': {
                'enabled': True,
                'url': '',
                'auto_redirect': True,
                'with_delay': True,
            },
        }
        response = self.client.patch('/admin/api/v2/surveys/%s/' % self.survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 400)
        self.assertIn('url', response.data['redirect'])
        self.assertIn('timeout', response.data['redirect'])


class TestSurveyBehavior_footer(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.survey = SurveyFactory()
        self.user = self.client.login_yandex(is_superuser=True)

    def test_footer_should_be_none(self):
        response = self.client.get('/admin/api/v2/surveys/%s/' % self.survey.pk)
        self.assertEqual(response.status_code, 200)
        self.assertIsNone(response.data['footer'])

        response = self.client.get('/admin/api/v2/surveys/%s/?detailed=0' % self.survey.pk)
        self.assertEqual(response.status_code, 200)
        self.assertIsNone(response.data['footer'])

        response = self.client.get('/v1/surveys/%s/' % self.survey.pk)
        self.assertEqual(response.status_code, 200)
        self.assertIsNone(response.data['footer'])

    def test_footer_shouldnt_be_none(self):
        data = {
            'id': self.survey.pk,
            'footer': {
                'enabled': False,
            },
        }
        response = self.client.patch('/admin/api/v2/surveys/%s/' % self.survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertIsNotNone(response.data['footer'])
        self.assertFalse(response.data['footer']['enabled'])

        response = self.client.get('/v1/surveys/%s/' % self.survey.pk)
        self.assertEqual(response.status_code, 200)
        self.assertIsNotNone(response.data['footer'])
        self.assertFalse(response.data['footer']['enabled'])

    @override_settings(IS_BUSINESS_SITE=True)
    def test_footer_always_enabled_on_b2b(self):
        data = {
            'id': self.survey.pk,
            'footer': {
                'enabled': False,
            },
        }
        response = self.client.patch('/admin/api/v2/surveys/%s/' % self.survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertIsNotNone(response.data['footer'])
        self.assertTrue(response.data['footer']['enabled'])

    @override_settings(IS_BUSINESS_SITE=False)
    def test_footer_if_not_b2b(self):
        data = {
            'id': self.survey.pk,
            'footer': {
                'enabled': False,
            },
        }
        response = self.client.patch('/admin/api/v2/surveys/%s/' % self.survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertIsNotNone(response.data['footer'])
        self.assertFalse(response.data['footer']['enabled'])

        data['footer']['enabled'] = True
        response = self.client.patch('/admin/api/v2/surveys/%s/' % self.survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertTrue(response.data['footer']['enabled'])

        response = self.client.get('/v1/surveys/%s/' % self.survey.pk)
        self.assertEqual(response.status_code, 200)
        self.assertTrue(response.data['footer']['enabled'])


class TestSurveyBehavior_quiz(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.survey = SurveyFactory()
        self.user = self.client.login_yandex(is_superuser=True)
        self.image = ImageFactory()

    def test_get_quiz_data(self):
        self.survey.extra = {
            'quiz': {
                'show_results': True,
                'calc_method': 'range',
                'items': [
                    {
                        'title': 'title1',
                        'description': 'description1',
                        'image_id': self.image.pk,
                    },
                    {
                        'title': 'title2',
                        'description': 'description2',
                    },
                ],
            }
        }
        self.survey.save()

        response = self.client.get('/admin/api/v2/survey-quiz/%s/' % self.survey.pk)
        self.assertEqual(response.status_code, 200)

        quiz = SurveyQuiz(response.data)
        self.assertTrue(quiz.show_results)
        self.assertEqual(quiz.calc_method, 'range')
        self.assertEqual(len(quiz.items), 2)
        self.assertEqual(quiz.items[0].title, 'title1')
        self.assertEqual(quiz.items[0].description, 'description1')
        self.assertEqual(quiz.items[0].image_id, self.image.pk)
        self.assertEqual(quiz.items[1].title, 'title2')
        self.assertEqual(quiz.items[1].description, 'description2')
        self.assertIsNone(quiz.items[1].image_id)

    def test_patch_correct_quiz_data_with_range_calc_method(self):
        data = {
            'show_results': True,
            'calc_method': 'range',
            'items': [
                {
                    'title': 'title1',
                    'description': 'description1',
                    'image_id': self.image.pk,
                },
                {
                    'title': 'title2',
                    'description': '',
                },
                {
                    'title': 'title3',
                },
            ],
        }
        response = self.client.patch('/admin/api/v2/survey-quiz/%s/' % self.survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)

        self.survey.refresh_from_db()
        self.assertIsNotNone(self.survey.extra)
        self.assertIsNotNone(self.survey.extra.get('quiz'))

        quiz = SurveyQuiz(self.survey.extra.get('quiz'))
        self.assertTrue(quiz.show_results)
        self.assertEqual(quiz.calc_method, 'range')
        self.assertEqual(len(quiz.items), 3)
        self.assertEqual(quiz.items[0].title, 'title1')
        self.assertEqual(quiz.items[0].description, 'description1')
        self.assertEqual(quiz.items[0].image_id, self.image.pk)
        self.assertEqual(quiz.items[1].title, 'title2')
        self.assertEqual(quiz.items[1].description, '')
        self.assertIsNone(quiz.items[1].image_id)
        self.assertEqual(quiz.items[2].title, 'title3')
        self.assertIsNone(quiz.items[2].description)
        self.assertIsNone(quiz.items[2].image_id)

    def test_patch_correct_quiz_data_with_scores_calc_method(self):
        SurveyQuestionFactory(
            survey=self.survey,
            param_quiz={
                'enabled': True,
                'answers': [
                    {'correct': True, 'value': 'testme', 'scores': 6},
                ],
            },
        )

        data = {
            'show_results': True,
            'calc_method': 'scores',
            'pass_scores': 3,
            'items': [
                {
                    'title': 'title1',
                    'description': 'description1',
                    'image_id': self.image.pk,
                },
                {
                    'title': 'title2',
                    'description': 'description2',
                },
            ],
        }
        response = self.client.patch('/admin/api/v2/survey-quiz/%s/' % self.survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)

        self.survey.refresh_from_db()
        self.assertIsNotNone(self.survey.extra)
        self.assertIsNotNone(self.survey.extra.get('quiz'))

        quiz = SurveyQuiz(self.survey.extra.get('quiz'))
        self.assertTrue(quiz.show_results)
        self.assertEqual(quiz.calc_method, 'scores')
        self.assertEqual(quiz.pass_scores, 3)
        self.assertEqual(len(quiz.items), 2)
        self.assertEqual(quiz.items[0].title, 'title1')
        self.assertEqual(quiz.items[0].description, 'description1')
        self.assertEqual(quiz.items[0].image_id, self.image.pk)
        self.assertEqual(quiz.items[1].title, 'title2')
        self.assertEqual(quiz.items[1].description, 'description2')
        self.assertIsNone(quiz.items[1].image_id)

    def test_should_throw_validation_error_when_items_is_none(self):
        data = {
            'show_results': True,
            'calc_method': 'range',
        }
        response = self.client.patch('/admin/api/v2/survey-quiz/%s/' % self.survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 400)
        self.assertIn('items', response.data)

    def test_should_throw_validation_error_when_items_is_empty(self):
        data = {
            'show_results': True,
            'calc_method': 'range',
            'items': [],
        }
        response = self.client.patch('/admin/api/v2/survey-quiz/%s/' % self.survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 400)
        self.assertIn('items', response.data)

    def test_should_throw_validation_error_when_items_count_is_less_then_expected(self):
        data = {
            'show_results': True,
            'calc_method': 'range',
            'items': [
                {
                    'title': 'title1',
                    'description': 'description1',
                    'image_id': self.image.pk,
                },
            ],
        }
        response = self.client.patch('/admin/api/v2/survey-quiz/%s/' % self.survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 400)
        self.assertIn('items', response.data)

    def test_should_throw_validation_error_when_items_count_is_greather_then_expected(self):
        data = {
            'show_results': True,
            'calc_method': 'scores',
            'items': [
                {
                    'title': 'title1',
                    'description': 'description1',
                },
                {
                    'title': 'title2',
                    'description': 'description2',
                },
                {
                    'title': 'title3',
                    'description': 'description3',
                },
            ],
        }
        response = self.client.patch('/admin/api/v2/survey-quiz/%s/' % self.survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 400)
        self.assertIn('items', response.data)

    def test_should_throw_validation_error_when_pass_scores_greather_then_total_scores(self):
        data = {
            'show_results': True,
            'calc_method': 'scores',
            'pass_scores': 12,
            'items': [
                {
                    'title': 'title1',
                    'description': 'description1',
                },
                {
                    'title': 'title2',
                    'description': 'description2',
                },
            ],
        }
        response = self.client.patch('/admin/api/v2/survey-quiz/%s/' % self.survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 400)
        self.assertIn('pass_scores', response.data)


class TestSurveyBehavior_submit_language(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory(is_published_external=True)
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
        )

    def test_should_return_russian_language(self):
        data = {
            self.question.param_slug: 'testit',
        }
        params = {
            'HTTP_ACCEPT_LANGUAGE': 'ru',
            'format': 'json',
        }
        response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk, data=data, **params)
        self.assertEqual(response.status_code, 200)

        answer_id = response.data['answer_id']
        answer = ProfileSurveyAnswer.objects.get(pk=answer_id)
        self.assertEqual(answer.source_request.get('lang'), 'ru')
        self.assertEqual(answer.get_answer_language(), 'ru')

    def test_should_return_english_language(self):
        data = {
            self.question.param_slug: 'testit',
        }
        params = {
            'HTTP_ACCEPT_LANGUAGE': 'en',
            'format': 'json',
        }
        response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk, data=data, **params)
        self.assertEqual(response.status_code, 200)

        answer_id = response.data['answer_id']
        answer = ProfileSurveyAnswer.objects.get(pk=answer_id)
        self.assertEqual(answer.source_request.get('lang'), 'en')
        self.assertEqual(answer.get_answer_language(), 'en')


class TestSurveyBehavior_group_like_question_slug(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory(is_published_external=True)
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            param_slug='answer_short_text__0',
        )

    def test_should_submit_form(self):
        data = {
            self.question.param_slug: 'testme',
        }
        response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertIsNotNone(response.data['answer_id'])


class TestSurveyYtUrl(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory(save_logs_for_statbox=True)

    @override_settings(APP_TYPE='forms_int')
    def test_should_return_correct_yt_url_for_forms_int(self):
        response = self.client.get('/admin/api/v2/surveys/%s/' % self.survey.pk)
        self.assertEqual(response.status_code, 200)
        part = 'forms_int/%s/%s' % (yenv.type, self.survey.pk)
        self.assertTrue(response.data['yt_url'].endswith(part))
        self.assertTrue(response.data['export_answers_to_yt'])

    @override_settings(APP_TYPE='forms_ext')
    def test_should_return_correct_yt_url_for_forms_ext(self):
        response = self.client.get('/admin/api/v2/surveys/%s/' % self.survey.pk)
        self.assertEqual(response.status_code, 200)
        part = 'forms_ext/%s/%s' % (yenv.type, self.survey.pk)
        self.assertTrue(response.data['yt_url'].endswith(part))
        self.assertTrue(response.data['export_answers_to_yt'])

    @override_settings(APP_TYPE='forms_ext_admin')
    def test_should_return_correct_yt_url_for_forms_ext_admin(self):
        response = self.client.get('/admin/api/v2/surveys/%s/' % self.survey.pk)
        self.assertEqual(response.status_code, 200)
        part = 'forms_ext/%s/%s' % (yenv.type, self.survey.pk)
        self.assertTrue(response.data['yt_url'].endswith(part))
        self.assertTrue(response.data['export_answers_to_yt'])

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_return_correct_yt_url_for_forms_biz(self):
        response = self.client.get('/admin/api/v2/surveys/%s/' % self.survey.pk)
        self.assertEqual(response.status_code, 200)
        self.assertIsNone(response.data['yt_url'])
        self.assertTrue(response.data['export_answers_to_yt'])


class TestMockProfileForAnonymous(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.survey = SurveyFactory(is_published_external=True, is_public=True)
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
        )
        self.data = {
            self.question.param_slug: 'testit',
        }

    def test_answer_survey_success(self):
        response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk, data=self.data)
        assert response.status_code == 200
        answer = ProfileSurveyAnswer.objects.get(survey_id=self.survey.pk)
        user = answer.user
        assert user.pk == settings.MOCK_PROFILE_ID

    def test_answer_survey_without_multiple_answers_success(self):
        self.survey.is_allow_multiple_answers = False
        self.survey.save()
        for _ in range(3):
            response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk, data=self.data)
            assert response.status_code == 200

        answers = ProfileSurveyAnswer.objects.filter(survey_id=self.survey.pk)
        assert len(answers) == 3
        assert set(answer.user.pk for answer in answers) == {settings.MOCK_PROFILE_ID}

    def test_answer_survey_with_answer_editing_success(self):
        self.survey.is_allow_answer_editing = True
        self.survey.save()
        for answer_count in range(3):
            data = {
                self.question.param_slug: 'testit_{}'.format(answer_count),
            }
            response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk, data=data)
            assert response.status_code == 200

        answers = ProfileSurveyAnswer.objects.filter(survey_id=self.survey.pk)
        self.assertEqual(len(answers), 3)
        results = {
            answer.as_dict()[self.question.pk].get('value')
            for answer in answers
        }
        expected = {
            f'testit_{i}' for i in range(3)
        }
        self.assertSetEqual(results, expected)

    def test_answer_survey_with_login_only_success(self):
        self.survey.need_auth_to_answer = True
        self.survey.save()
        response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk, data=self.data)
        assert response.status_code == 401

    def test_survey_no_access_for_anonymous(self):
        response = self.client.get('/admin/api/v2/surveys/%s/' % self.survey.pk)
        assert response.status_code == 401

    def test_survey_no_access_for_anonymous_list(self):
        response = self.client.get('/admin/api/v2/surveys/')
        assert response.status_code == 401


class TestSurveyGroupListView(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory()
        self.group = SurveyGroupFactory(user=self.user)
        self.client.login_yandex(is_superuser=True)

    def test_should_get_profile_data_from_bb(self):
        with patch('events.surveyme.api_admin.v2.serializers.JsonBlackbox.userinfo') as mock_userinfo:
            response = self.client.get('/admin/api/v2/survey-groups/')
        self.assertEqual(response.status_code, 200)
        mock_userinfo.assert_called_once_with(uid=str(self.user.uid), dbfields=ANY, userip=ANY)


class TestSurveySubmitWithComplexConditions(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory(is_published_external=True)
        answer_choices_type = AnswerType.objects.get(slug='answer_choices')
        answer_group = AnswerType.objects.get(slug='answer_group')
        answer_short_text = AnswerType.objects.get(slug='answer_short_text')
        self.yesno_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=answer_choices_type,
            param_slug='yesno',
        )
        self.group_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=answer_group,
            param_is_required=False,
        )
        self.version_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=answer_choices_type,
            param_slug='version',
            group_id=self.group_question.pk,
            position=1,
        )
        self.osversion_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=answer_short_text,
            param_slug='osversion',
            group_id=self.group_question.pk,
            position=2,
        )
        self.yesno_choices = [
            SurveyQuestionChoiceFactory(survey_question=self.yesno_question, label='Yes'),
            SurveyQuestionChoiceFactory(survey_question=self.yesno_question, label='No'),
        ]
        self.version_choices = [
            SurveyQuestionChoiceFactory(survey_question=self.version_question, label='Version1'),
            SurveyQuestionChoiceFactory(survey_question=self.version_question, label='Version2'),
            SurveyQuestionChoiceFactory(survey_question=self.version_question, label='Other'),
        ]
        self.content_type_attribute = ContentTypeAttributeFactory(
            title='test',
            attr='answer_choices',
            lookup_field='identity',
        )

        group_node = SurveyQuestionShowConditionNodeFactory(survey_question=self.group_question)
        SurveyQuestionShowConditionNodeItemFactory(
            position=1,
            survey_question_show_condition_node=group_node,
            operator='and',
            condition='eq',
            content_type_attribute=self.content_type_attribute,
            survey_question=self.yesno_question,
            value=str(self.yesno_choices[0].pk),
        )

        version_node = SurveyQuestionShowConditionNodeFactory(survey_question=self.version_question)
        SurveyQuestionShowConditionNodeItemFactory(
            position=1,
            survey_question_show_condition_node=version_node,
            operator='and',
            condition='eq',
            content_type_attribute=self.content_type_attribute,
            survey_question=self.yesno_question,
            value=str(self.yesno_choices[0].pk),
        )

        osversion_node = SurveyQuestionShowConditionNodeFactory(survey_question=self.osversion_question)
        SurveyQuestionShowConditionNodeItemFactory(
            position=1,
            survey_question_show_condition_node=osversion_node,
            operator='and',
            condition='eq',
            content_type_attribute=self.content_type_attribute,
            survey_question=self.yesno_question,
            value=str(self.yesno_choices[0].pk),
        )
        SurveyQuestionShowConditionNodeItemFactory(
            position=1,
            survey_question_show_condition_node=osversion_node,
            operator='and',
            condition='eq',
            content_type_attribute=self.content_type_attribute,
            survey_question=self.version_question,
            value=str(self.version_choices[-1].pk),
        )

    def test_should_submit_form_with_complex_conditions(self):
        data = {
            'yesno': self.yesno_choices[0].pk,
            'version__0': self.version_choices[0].pk,
            'version__1': self.version_choices[1].pk,
        }
        response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/', data=data)
        self.assertEqual(response.status_code, 200)


class TestSurveyNotificationsView(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory()
        self.notifications = [
            HookSubscriptionNotificationFactory(survey=self.survey, status='error', is_visible=True),
            HookSubscriptionNotificationFactory(survey=self.survey, status='error', is_visible=False),
            HookSubscriptionNotificationFactory(survey=self.survey, status='success', is_visible=True),
        ]

    def test_should_return_paginated_notifications_response(self):
        response = self.client.get(f'/admin/api/v2/surveys/{self.survey.pk}/notifications/')
        self.assertEqual(response.status_code, 200)

        results = response.data['results']
        self.assertEqual(len(results), 1)
        self.assertEqual(results[0]['id'], self.notifications[0].pk)
        self.assertTrue('context' not in results[0])
        self.assertTrue('response' not in results[0])
        self.assertTrue('error' not in results[0])
        self.assertTrue('error_message' in results[0])

    def test_should_execute_exact_number_of_queries(self):
        with self.assertNumQueries(10):
            response = self.client.get(f'/admin/api/v2/surveys/{self.survey.pk}/notifications/')
            self.assertEqual(response.status_code, 200)


class TestSurveyQuestionChoiceActions(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory(is_published_external=True)
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
        )
        self.choice = SurveyQuestionChoiceFactory(
            survey_question=self.question,
            label='one',
            slug='x-one',
        )

    def test_should_create_new_question_choices(self):
        data = {
            'choices': [
                {
                    'id': self.choice.pk,
                    'slug': self.choice.slug,
                    'label': self.choice.label,
                },
                {
                    'label': 'two',
                    'slug': 'x-two',
                },
                {
                    'label': 'three',
                },
            ],
        }
        response = self.client.patch(f'/admin/api/v2/survey-questions/{self.question.pk}/', data=data, format='json')
        self.assertEqual(response.status_code, 200)

        choices = {
            choice.label: choice
            for choice in self.question.surveyquestionchoice_set.all()
        }
        self.assertEqual(len(choices), 3)

        self.assertIn('one', choices)
        self.assertEqual(choices['one'].pk, self.choice.pk)
        self.assertEqual(choices['one'].slug, self.choice.slug)

        self.assertIn('two', choices)
        self.assertEqual(choices['two'].slug, 'x-two')

        self.assertIn('three', choices)
        self.assertEqual(choices['three'].slug, str(choices['three'].pk))

    def test_should_modify_existing_choices(self):
        data = {
            'choices': [
                {
                    'id': self.choice.pk,
                    'slug': 'y-one',
                    'label': 'another one',
                },
            ],
        }
        response = self.client.patch(f'/admin/api/v2/survey-questions/{self.question.pk}/', data=data, format='json')
        self.assertEqual(response.status_code, 200)

        choices = {
            choice.pk: choice
            for choice in self.question.surveyquestionchoice_set.all()
        }
        self.assertEqual(len(choices), 1)

        self.assertIn(self.choice.pk, choices)
        choice = choices[self.choice.pk]
        self.assertEqual(choice.pk, self.choice.pk)
        self.assertEqual(choice.slug, 'y-one')
        self.assertEqual(choice.label, 'another one')

    def test_should_delete_existing_choices(self):
        data = {
            'choices': [],
        }
        response = self.client.patch(f'/admin/api/v2/survey-questions/{self.question.pk}/', data=data, format='json')
        self.assertEqual(response.status_code, 200)

        choices = {
            choice.label: choice
            for choice in self.question.surveyquestionchoice_set.all()
        }
        self.assertEqual(len(choices), 0)

        choices = {
            choice.label: choice
            for choice in SurveyQuestionChoice.with_deleted_objects.filter(survey_question=self.question)
        }
        self.assertEqual(len(choices), 1)

        self.assertIn('one', choices)
        self.assertEqual(choices['one'].pk, self.choice.pk)
        self.assertEqual(choices['one'].slug, str(self.choice.pk))
        self.assertEqual(choices['one'].label, self.choice.label)


class TestRebuildCounters(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory()

    def test_should_execute_rebuild_counters_for_stats(self):
        ProfileSurveyAnswerFactory(survey=self.survey)

        with patch('events.countme.utils.rebuild_counters') as mock_rebuild:
            response = self.client.get(f'/admin/api/v2/surveys/{self.survey.pk}/stats/')

        self.assertEqual(response.status_code, 200)
        mock_rebuild.assert_called_once_with(self.survey.pk)

    def test_should_execute_rebuild_counters_for_stats_detail(self):
        ProfileSurveyAnswerFactory(survey=self.survey)

        with patch('events.countme.utils.rebuild_counters') as mock_rebuild:
            response = self.client.get(f'/admin/api/v2/surveys/{self.survey.pk}/stats-detail/')

        self.assertEqual(response.status_code, 200)
        mock_rebuild.assert_called_once_with(self.survey.pk)

    def test_shouldnt_execute_rebuild_counters_for_stats(self):
        with patch('events.countme.utils.rebuild_counters') as mock_rebuild:
            response = self.client.get(f'/admin/api/v2/surveys/{self.survey.pk}/stats/')

        self.assertEqual(response.status_code, 200)
        mock_rebuild.assert_not_called()

    def test_shouldnt_execute_rebuild_counters_for_stats_detail(self):
        with patch('events.countme.utils.rebuild_counters') as mock_rebuild:
            response = self.client.get(f'/admin/api/v2/surveys/{self.survey.pk}/stats-detail/')

        self.assertEqual(response.status_code, 200)
        mock_rebuild.assert_not_called()


class TestStatsView(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory()
        self.survey.answercount.count = 1
        self.survey.answercount.save()
        answer_short_text = AnswerType.objects.get(slug='answer_short_text')
        for i in range(105):
            question = SurveyQuestionFactory(survey=self.survey, answer_type=answer_short_text)
            QuestionCountFactory(survey=self.survey, question=question, composite_key=f'question{i}', count=1)

    def test_should_return_paginated_response_with_default_page_size(self):
        url = f'/admin/api/v2/surveys/{self.survey.pk}/stats-detail/'
        response = self.client.get(f'{url}?survey={self.survey.pk}')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data['questions']), 100)
        self.assertEqual(response.data['links']['next-url'], f'{url}?survey={self.survey.pk}&page=2&page_size=100')

        response = self.client.get(response.data['links']['next-url'])
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data['questions']), 5)
        self.assertEqual(response.data['links']['next-url'], None)

    def test_should_return_paginated_response_with_custom_page_size(self):
        url = f'/admin/api/v2/surveys/{self.survey.pk}/stats-detail/'
        response = self.client.get(f'{url}?survey={self.survey.pk}&page_size=50')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data['questions']), 50)
        self.assertEqual(response.data['links']['next-url'], f'{url}?survey={self.survey.pk}&page_size=50&page=2')

        response = self.client.get(response.data['links']['next-url'])
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data['questions']), 50)
        self.assertEqual(response.data['links']['next-url'], f'{url}?survey={self.survey.pk}&page_size=50&page=3')

        response = self.client.get(response.data['links']['next-url'])
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data['questions']), 5)
        self.assertEqual(response.data['links']['next-url'], None)

    def test_should_return_paginated_response_with_large_page_size(self):
        url = f'/admin/api/v2/surveys/{self.survey.pk}/stats-detail/'
        response = self.client.get(f'{url}?survey={self.survey.pk}&page_size=1000')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data['questions']), 105)
        self.assertEqual(response.data['links']['next-url'], None)


class TestSurveySubmitWithHiddenRequiredField(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory(is_published_external=True)
        self.yesno_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_slug='yesno',
            param_is_required=False,
        )
        self.text_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            param_slug='text',
            param_is_required=True,
        )
        self.yesno_choices = [
            SurveyQuestionChoiceFactory(survey_question=self.yesno_question, label='Yes'),
            SurveyQuestionChoiceFactory(survey_question=self.yesno_question, label='No'),
        ]
        self.content_type_attribute = ContentTypeAttributeFactory(
            attr='answer_choices',
            lookup_field='identity',
        )

        text_node = SurveyQuestionShowConditionNodeFactory(survey_question=self.text_question)
        SurveyQuestionShowConditionNodeItemFactory(
            position=1,
            survey_question_show_condition_node=text_node,
            operator='and',
            condition='neq',
            content_type_attribute=self.content_type_attribute,
            survey_question=self.yesno_question,
            value='',
        )

    def test_should_submit_empty_form(self):
        data = {}
        response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/', data=data, format='json')
        self.assertEqual(response.status_code, 200)

    def test_should_submit_empty_form_with_empty_text_field(self):
        data = {
            'text': '',
        }
        response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/', data=data, format='json')
        self.assertEqual(response.status_code, 200)

    def test_should_submit_empty_form_with_multiple_choices_and_empty_text_field(self):
        self.yesno_question.param_is_allow_multiple_choice = True
        self.yesno_question.save()
        data = {
            'text': '',
        }
        response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/', data=data, format='json')
        self.assertEqual(response.status_code, 200)

    def test_should_submit_form(self):
        data = {
            'yesno': self.yesno_choices[0].pk,
            'text': 'testme',
        }
        response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/', data=data, format='json')
        self.assertEqual(response.status_code, 200)

    def test_shouldnt_submit_form_without_text_field(self):
        data = {
            'yesno': self.yesno_choices[0].pk,
        }
        response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/', data=data, format='json')
        self.assertEqual(response.status_code, 400)
        self.assertEqual(len(response.data['fields']['yesno']['errors']), 0)
        self.assertEqual(len(response.data['fields']['text']['errors']), 1)

    def test_shouldnt_submit_form_with_empty_text_field(self):
        data = {
            'yesno': self.yesno_choices[0].pk,
            'text': '',
        }
        response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/', data=data, format='json')
        self.assertEqual(response.status_code, 400)
        self.assertEqual(len(response.data['fields']['yesno']['errors']), 0)
        self.assertEqual(len(response.data['fields']['text']['errors']), 1)


class TestPatchSurveyVariableWithCorrectForeignKey(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_superuser=True)
        self.some_subscription = ServiceSurveyHookSubscriptionFactory()
        self.survey = SurveyFactory()
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
        )

    def test_should_create_subscription(self):
        data = {
            'hooks': [{
                'subscriptions': [{
                    'service_type_action': 3,
                    'title': '{123}',
                    'body': 'testme',
                    'email_to_address': '{456}',
                    'variables': {
                        '123': {
                            '_id': '123',
                            'hook_subscription_id': self.some_subscription.pk,
                            'var': 'form.question_answer',
                            'arguments': {
                                'question': self.question.pk,
                            },
                        },
                        '456': {
                            '_id': '456',
                            'hook_subscription_id': self.some_subscription.pk,
                            'var': 'form.author_email',
                            'arguments': {},
                        },
                    },
                    'json_rpc': {},
                    'startrek': {},
                    'wiki': {},
                }],
            }],
        }
        response = self.client.patch(f'/admin/api/v2/surveys/{self.survey.pk}/', data=data, format='json')
        self.assertEqual(response.status_code, 200)

        hook = self.survey.hooks.first()
        self.assertIsNotNone(hook)
        self.assertNotEqual(hook.pk, self.some_subscription.survey_hook_id)

        subscription = hook.subscriptions.first()
        self.assertIsNotNone(subscription)
        self.assertNotEqual(subscription.pk, self.some_subscription.pk)
        self.assertEqual(subscription.title, '{123}')
        self.assertEqual(subscription.body, 'testme')
        self.assertEqual(subscription.email_to_address, '{456}')

        variables = list(SurveyVariable.objects.filter(pk__in=['123', '456']))
        self.assertEqual(len(variables), 2)
        self.assertEqual(variables[0].hook_subscription_id, subscription.pk)
        self.assertIn(variables[0].variable_id, ('123', '456'))
        self.assertEqual(variables[1].hook_subscription_id, subscription.pk)
        self.assertIn(variables[1].variable_id, ('123', '456'))

    def test_should_create_subscription_with_nullable_fields(self):
        data = {
            'hooks': [{
                'subscriptions': [{
                    'service_type_action': 3,
                    'title': 'subject',
                    'body': 'testme',
                    'email_to_address': 'user@company.com',
                    'variables': None,
                    'json_rpc': None,
                    'startrek': None,
                    'wiki': None,
                }],
            }],
        }
        response = self.client.patch(f'/admin/api/v2/surveys/{self.survey.pk}/', data=data, format='json')
        self.assertEqual(response.status_code, 200)

        hook = self.survey.hooks.first()
        self.assertIsNotNone(hook)
        self.assertNotEqual(hook.pk, self.some_subscription.survey_hook_id)

        subscription = hook.subscriptions.first()
        self.assertIsNotNone(subscription)
        self.assertNotEqual(subscription.pk, self.some_subscription.pk)
        self.assertEqual(subscription.title, 'subject')
        self.assertEqual(subscription.body, 'testme')
        self.assertEqual(subscription.email_to_address, 'user@company.com')

        self.assertIsNotNone(subscription.wiki)
        self.assertIsNotNone(subscription.startrek)
        self.assertIsNotNone(subscription.json_rpc)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_create_subscription_with_patched_email_from_address_biz(self):
        data = {
            'hooks': [{
                'subscriptions': [{
                    'service_type_action': 3,
                    'title': 'subject',
                    'body': 'testme',
                    'email_to_address': 'user@company.com',
                    'email_from_address': 'admin@company.com',
                    'variables': {},
                    'json_rpc': {},
                    'startrek': {},
                    'wiki': {},
                }],
            }],
        }
        response = self.client.patch(f'/admin/api/v2/surveys/{self.survey.pk}/', data=data, format='json')
        self.assertEqual(response.status_code, 200)

        hook = self.survey.hooks.first()
        self.assertIsNotNone(hook)
        self.assertNotEqual(hook.pk, self.some_subscription.survey_hook_id)

        subscription = hook.subscriptions.first()
        self.assertIsNotNone(subscription)
        self.assertNotEqual(subscription.pk, self.some_subscription.pk)
        self.assertEqual(subscription.email_from_address, f'{self.survey.pk}@forms-mailer.yaconnect.com')

    def test_should_modify_subscription(self):
        hook = SurveyHookFactory(survey=self.survey)  # create subscription hook
        subscription = ServiceSurveyHookSubscriptionFactory(
            survey_hook=hook,
            service_type_action_id=3,
            email_to_address='admin@company.com',
        )

        data = {
            'hooks': [{
                'id': hook.pk,
                'subscriptions': [{
                    'id': subscription.pk,
                    'service_type_action': 3,
                    'date_created': subscription.date_created,
                    'title': '{123}',
                    'body': 'testme',
                    'email_to_address': '{456}',
                    'variables': {
                        '123': {
                            '_id': '123',
                            'hook_subscription_id': self.some_subscription.pk,
                            'var': 'form.question_answer',
                            'arguments': {
                                'question': self.question.pk,
                            },
                        },
                        '456': {
                            '_id': '456',
                            'hook_subscription_id': self.some_subscription.pk,
                            'var': 'form.author_email',
                            'arguments': {},
                        },
                    },
                    'json_rpc': {},
                    'startrek': {},
                    'wiki': {},
                }],
            }],
        }
        response = self.client.patch(f'/admin/api/v2/surveys/{self.survey.pk}/', data=data, format='json')
        self.assertEqual(response.status_code, 200)

        subscription.refresh_from_db()
        self.assertIsNotNone(subscription)
        self.assertEqual(subscription.title, '{123}')
        self.assertEqual(subscription.body, 'testme')
        self.assertEqual(subscription.email_to_address, '{456}')

        variables = list(SurveyVariable.objects.filter(pk__in=['123', '456']))
        self.assertEqual(len(variables), 2)
        self.assertEqual(variables[0].hook_subscription_id, subscription.pk)
        self.assertIn(variables[0].variable_id, ('123', '456'))
        self.assertEqual(variables[1].hook_subscription_id, subscription.pk)
        self.assertIn(variables[1].variable_id, ('123', '456'))


class TestAnswerParamsSerializer(TestCase):
    serializer_class = AnswerParamsSerializer

    def test_should_parse_date(self):
        data = {
            'started': '2020-10-01',
            'finished': '2020-10-30',
        }
        serializer = self.serializer_class(data=data)
        self.assertTrue(serializer.is_valid())

        started = serializer.validated_data['started']
        self.assertEqual(started, make_aware(datetime(2020, 10, 1)))

        finished = serializer.validated_data['finished']
        self.assertEqual(finished, make_aware(datetime(2020, 10, 30)))

    def test_should_parse_datetime(self):
        data = {
            'started': '2020-10-01T09:00:00',
            'finished': '2020-10-30T16:59:59',
        }
        serializer = self.serializer_class(data=data)
        self.assertTrue(serializer.is_valid())

        started = serializer.validated_data['started']
        self.assertEqual(started, make_aware(datetime(2020, 10, 1, 9, 0, 0)))

        finished = serializer.validated_data['finished']
        self.assertEqual(finished, make_aware(datetime(2020, 10, 30, 16, 59, 59)))

    def test_shouldnt_parse_invalid_data(self):
        data = {
            'started': '2020_10_01',
            'finished': '2020_10_30',
        }
        serializer = self.serializer_class(data=data)
        self.assertFalse(serializer.is_valid())

        self.assertIn('started', serializer.errors)
        self.assertIn('finished', serializer.errors)


class TestSurveyViewSet_answers(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory()
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
        )
        self.answers = [
            ProfileSurveyAnswerFactory(
                survey=self.survey,
                date_created=make_aware(datetime(2020, 10, 1, 12, 00)),
                data={
                    'data': [{
                        'question': self.question.get_answer_info(),
                        'value': '1',
                    }],
                },
            ),
            ProfileSurveyAnswerFactory(
                survey=self.survey,
                date_created=make_aware(datetime(2020, 10, 10, 12, 00)),
                data={
                    'data': [{
                        'question': self.question.get_answer_info(),
                        'value': '2',
                    }],
                },
            ),
            ProfileSurveyAnswerFactory(
                survey=self.survey,
                date_created=make_aware(datetime(2020, 10, 20, 12, 00)),
                data={
                    'data': [{
                        'question': self.question.get_answer_info(),
                        'value': '3',
                    }],
                },
            ),
        ]

    def test_should_filter_all_answers(self):
        response = self.client.get(f'/admin/api/v2/surveys/{self.survey.pk}/answers/')
        self.assertEqual(response.status_code, 200)

        self.assertEqual(response.data['count'], 3)
        self.assertEqual(len(response.data['results']), 1)
        answer_data = response.data['results'][0]
        self.assertEqual(answer_data['survey_id'], self.survey.pk)
        self.assertEqual(answer_data['survey_name'], self.survey.name)
        self.assertEqual(answer_data['id'], self.answers[-1].pk)
        self.assertEqual(answer_data['answer_key'], self.answers[-1].secret_code)

    def test_should_filter_answers_later_then(self):
        response = self.client.get(f'/admin/api/v2/surveys/{self.survey.pk}/answers/', {
            'started': '2020-10-15',
        })
        self.assertEqual(response.status_code, 200)

        self.assertEqual(response.data['count'], 1)
        self.assertEqual(len(response.data['results']), 1)
        answer_data = response.data['results'][0]
        self.assertEqual(answer_data['survey_id'], self.survey.pk)
        self.assertEqual(answer_data['survey_name'], self.survey.name)
        self.assertEqual(answer_data['id'], self.answers[-1].pk)
        self.assertEqual(answer_data['answer_key'], self.answers[-1].secret_code)

    def test_should_filter_answers_earlier_then(self):
        response = self.client.get(f'/admin/api/v2/surveys/{self.survey.pk}/answers/', {
            'finished': '2020-10-05',
        })
        self.assertEqual(response.status_code, 200)

        self.assertEqual(response.data['count'], 1)
        self.assertEqual(len(response.data['results']), 1)
        answer_data = response.data['results'][0]
        self.assertEqual(answer_data['survey_id'], self.survey.pk)
        self.assertEqual(answer_data['survey_name'], self.survey.name)
        self.assertEqual(answer_data['id'], self.answers[0].pk)
        self.assertEqual(answer_data['answer_key'], self.answers[0].secret_code)

    def test_should_filter_answers_in_range(self):
        response = self.client.get(f'/admin/api/v2/surveys/{self.survey.pk}/answers/', {
            'started': '2020-10-05',
            'finished': '2020-10-15',
        })
        self.assertEqual(response.status_code, 200)

        self.assertEqual(response.data['count'], 1)
        self.assertEqual(len(response.data['results']), 1)
        answer_data = response.data['results'][0]
        self.assertEqual(answer_data['survey_id'], self.survey.pk)
        self.assertEqual(answer_data['survey_name'], self.survey.name)
        self.assertEqual(answer_data['id'], self.answers[1].pk)
        self.assertEqual(answer_data['answer_key'], self.answers[1].secret_code)

    def test_shouldnt_filter_any_answers(self):
        response = self.client.get(f'/admin/api/v2/surveys/{self.survey.pk}/answers/', {
            'started': '2020-10-15',
            'finished': '2020-10-05',
        })
        self.assertEqual(response.status_code, 200)

        self.assertEqual(response.data['count'], 0)
        self.assertEqual(len(response.data['results']), 0)

    def test_should_throw_bad_request(self):
        response = self.client.get(f'/admin/api/v2/surveys/{self.survey.pk}/answers/', {
            'started': '2020_10_05',
            'finished': '2020_10_15',
        })
        self.assertEqual(response.status_code, 400)

        self.assertIn('started', response.data)
        self.assertIn('finished', response.data)


class TestSurveyViewSet_answers__answer_files(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory()
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_files'),
        )
        self.answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            data={
                'data': [{
                    'question': self.question.get_answer_info(),
                    'value': [{
                        'path': '/123/readme.txt',
                    }],
                }],
            },
        )

    def test_should_return_path_with_default_tld(self):
        response = self.client.get(f'/admin/api/v2/surveys/{self.survey.pk}/answers/')
        self.assertEqual(response.status_code, 200)

        self.assertEqual(response.data['count'], 1)
        self.assertEqual(len(response.data['results']), 1)
        answer_data = response.data['results'][0]
        self.assertEqual(answer_data['id'], self.answer.pk)
        expected = get_mds_url('/123/readme.txt', tld=None)
        self.assertEqual(answer_data['data'][0]['value'][0]['path'], expected)

    def test_should_return_path_with_referenced_tld(self):
        response = self.client.get(
            f'/admin/api/v2/surveys/{self.survey.pk}/answers/',
            HTTP_REFERER='https://yandex.com',
        )
        self.assertEqual(response.status_code, 200)

        self.assertEqual(response.data['count'], 1)
        self.assertEqual(len(response.data['results']), 1)
        answer_data = response.data['results'][0]
        self.assertEqual(answer_data['id'], self.answer.pk)
        expected = get_mds_url('/123/readme.txt', tld='.com')
        self.assertEqual(answer_data['data'][0]['value'][0]['path'], expected)


class TestAnswerResultView__answer_files(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        # self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory()
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_files'),
        )
        self.answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            data={
                'data': [{
                    'question': self.question.get_answer_info(),
                    'value': [{
                        'path': '/123/readme.txt',
                    }],
                }],
            },
        )

    def test_should_return_path_with_default_tld(self):
        response = self.client.get(f'/v1/answers/{self.answer.secret_code}/')
        self.assertEqual(response.status_code, 200)

        answer_data = response.data
        self.assertEqual(answer_data['id'], self.answer.pk)
        expected = get_mds_url('/123/readme.txt', tld=None)
        self.assertEqual(answer_data['data'][0]['value'][0]['path'], expected)

    def test_should_return_path_with_referenced_tld(self):
        response = self.client.get(
            f'/v1/answers/{self.answer.secret_code}/',
            HTTP_REFERER='https://yandex.com',
        )
        self.assertEqual(response.status_code, 200)

        answer_data = response.data
        self.assertEqual(answer_data['id'], self.answer.pk)
        expected = get_mds_url('/123/readme.txt', tld='.com')
        self.assertEqual(answer_data['data'][0]['value'][0]['path'], expected)


class TestFrontSurveyViewSet_stats(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
        )

    def test_should_return_stats(self):
        self.survey.extra = {
            'stats':  {
                'enabled': True,
            },
        }
        self.survey.save()
        with patch('events.countme.stats.get_stats_info') as mock_get_stats_info:
            mock_get_stats_info.return_value = {}
            response = self.client.get(f'/v1/surveys/{self.survey.pk}/stats/')
        self.assertEqual(response.status_code, 200)
        mock_get_stats_info.assert_called_once()
        (survey, ) = mock_get_stats_info.call_args[0]
        self.assertEqual(survey.pk, self.survey.pk)

    def test_should_return_404_not_found(self):
        with patch('events.countme.stats.get_stats_info') as mock_get_stats_info:
            mock_get_stats_info.return_value = {}
            response = self.client.get(f'/v1/surveys/{self.survey.pk}/stats/')
        self.assertEqual(response.status_code, 404)
        mock_get_stats_info.assert_not_called()


class TestSurveyBehavior_teaser(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.survey = SurveyFactory()
        self.user = self.client.login_yandex(is_superuser=True)

    def test_teaser_should_be_none(self):
        response = self.client.get('/admin/api/v2/surveys/%s/' % self.survey.pk)
        self.assertEqual(response.status_code, 200)
        self.assertIsNone(response.data['teaser'])

        response = self.client.get('/admin/api/v2/surveys/%s/?detailed=0' % self.survey.pk)
        self.assertEqual(response.status_code, 200)
        self.assertIsNone(response.data['teaser'])

        response = self.client.get('/v1/surveys/%s/' % self.survey.pk)
        self.assertEqual(response.status_code, 200)
        self.assertIsNone(response.data['teaser'])

    def test_teaser_shouldnt_be_none(self):
        data = {
            'id': self.survey.pk,
            'teaser': {
                'enabled': False,
            },
        }
        response = self.client.patch('/admin/api/v2/surveys/%s/' % self.survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertIsNotNone(response.data['teaser'])
        self.assertFalse(response.data['teaser']['enabled'])

        response = self.client.get('/v1/surveys/%s/' % self.survey.pk)
        self.assertEqual(response.status_code, 200)
        self.assertIsNotNone(response.data['teaser'])
        self.assertFalse(response.data['teaser']['enabled'])

    @override_settings(IS_BUSINESS_SITE=True)
    def test_teaser_always_enabled_on_b2b(self):
        data = {
            'id': self.survey.pk,
            'teaser': {
                'enabled': False,
            },
        }
        response = self.client.patch('/admin/api/v2/surveys/%s/' % self.survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertIsNotNone(response.data['teaser'])
        self.assertTrue(response.data['teaser']['enabled'])

    @override_settings(IS_BUSINESS_SITE=False)
    def test_teaser_if_not_b2b(self):
        data = {
            'id': self.survey.pk,
            'teaser': {
                'enabled': False,
            },
        }
        response = self.client.patch('/admin/api/v2/surveys/%s/' % self.survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertIsNotNone(response.data['teaser'])
        self.assertFalse(response.data['teaser']['enabled'])

        data['teaser']['enabled'] = True
        response = self.client.patch('/admin/api/v2/surveys/%s/' % self.survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertTrue(response.data['teaser']['enabled'])

        response = self.client.get('/v1/surveys/%s/' % self.survey.pk)
        self.assertEqual(response.status_code, 200)
        self.assertTrue(response.data['teaser']['enabled'])


class TestSurveyQuestionViewSet_answer_date(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.survey = SurveyFactory()
        self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory()
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_date'),
        )

    def test_should_save_date_min_max_params(self):
        data = {
            'param_date_field_min': '2020-06-01',
            'param_date_field_max': '2020-06-30',
        }
        response = self.client.patch(f'/admin/api/v2/survey-questions/{self.question.pk}/', data=data, format='json')
        self.assertEqual(response.status_code, 200)

        self.question.refresh_from_db()
        self.assertEqual(self.question.param_date_field_min, date(2020, 6, 1))
        self.assertEqual(self.question.param_date_field_max, date(2020, 6, 30))

    def test_should_save_datetime_min_max_params(self):
        data = {
            'param_date_field_min': '2020-06-01T00:00:00Z',
            'param_date_field_max': '2020-06-30T00:00:00Z',
        }
        response = self.client.patch(f'/admin/api/v2/survey-questions/{self.question.pk}/', data=data, format='json')
        self.assertEqual(response.status_code, 200)

        self.question.refresh_from_db()
        self.assertEqual(self.question.param_date_field_min, date(2020, 6, 1))
        self.assertEqual(self.question.param_date_field_max, date(2020, 6, 30))

    def test_should_return_question_data(self):
        self.question.param_date_field_min = date(2020, 6, 1)
        self.question.param_date_field_max = date(2020, 6, 30)
        self.question.save()

        response = self.client.get(f'/admin/api/v2/survey-questions/{self.question.pk}/')
        self.assertEqual(response.status_code, 200)

        self.assertEqual(response.data['param_date_field_min'], '2020-06-01')
        self.assertEqual(response.data['param_date_field_max'], '2020-06-30')

    def test_should_return_questions_data(self):
        self.question.param_date_field_min = date(2020, 6, 1)
        self.question.param_date_field_max = date(2020, 6, 30)
        self.question.save()

        response = self.client.get(f'/admin/api/v2/survey-questions/?survey={self.survey.pk}')
        self.assertEqual(response.status_code, 200)

        self.assertEqual(len(response.data['results']), 1)
        result = response.data['results'][0]
        self.assertEqual(result['param_date_field_min'], '2020-06-01')
        self.assertEqual(result['param_date_field_max'], '2020-06-30')


class TestSurveyExportAnswers(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory()

    def test_should_set_date_exported__export(self):
        self.assertIsNone(self.survey.date_exported)

        with patch.object(MdsUploader, 'upload_report') as mock_upload_report:
            mock_upload_report.return_value = {
                'file_name': 'report.csv',
                'path': '/report.csv',
                'content_type': 'text/csv',
                'status_code': 200,
            }
            response = self.client.post(f'/admin/api/v2/surveys/{self.survey.pk}/export/')

        self.assertEqual(response.status_code, 202)
        self.survey.refresh_from_db()
        self.assertIsNotNone(self.survey.date_exported)
        mock_upload_report.assert_called_once()

    def test_should_change_date_exported__export(self):
        old_date_exported = now() - timedelta(days=1)
        self.survey.date_exported = old_date_exported
        self.survey.save()
        self.assertEqual(self.survey.date_exported, old_date_exported)

        with patch.object(MdsUploader, 'upload_report') as mock_upload_report:
            mock_upload_report.return_value = {
                'file_name': 'report.csv',
                'path': '/report.csv',
                'content_type': 'text/csv',
                'status_code': 200,
            }
            response = self.client.post(f'/admin/api/v2/surveys/{self.survey.pk}/export/')

        self.assertEqual(response.status_code, 202)
        self.survey.refresh_from_db()
        self.assertTrue(self.survey.date_exported > old_date_exported)
        mock_upload_report.assert_called_once()

    def test_should_set_date_exported__answers(self):
        self.assertIsNone(self.survey.date_exported)

        response = self.client.get(f'/admin/api/v2/surveys/{self.survey.pk}/answers/')

        self.assertEqual(response.status_code, 200)
        self.survey.refresh_from_db()
        self.assertIsNotNone(self.survey.date_exported)

    def test_should_change_date_exported__answers(self):
        old_date_exported = now() - timedelta(days=1)
        self.survey.date_exported = old_date_exported
        self.survey.save()
        self.assertEqual(self.survey.date_exported, old_date_exported)

        response = self.client.get(f'/admin/api/v2/surveys/{self.survey.pk}/answers/')

        self.assertEqual(response.status_code, 200)
        self.survey.refresh_from_db()
        self.assertTrue(self.survey.date_exported > old_date_exported)


class TestPostSurveyGroupView(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = UserFactory()
        self.client.set_cookie(self.user.uid)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_create_surveygroup_with_org_when_dir_id_is_string(self):
        o2g = OrganizationToGroupFactory()
        data = {'name': 'the group', 'org_id': o2g.org.dir_id}
        response = self.client.post(
            '/admin/api/v2/survey-groups/',
            data=data, format='json', HTTP_X_ORGS=o2g.org.dir_id,
        )
        self.assertEqual(response.status_code, 201)
        self.assertEqual(response.data['org_id'], o2g.org.dir_id)

        surveygroup = SurveyGroup.objects.get(pk=response.data['id'])
        self.assertEqual(surveygroup.org, o2g.org)
        self.assertTrue(self.user.has_perm('change_surveygroup', surveygroup))

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_create_surveygroup_with_org_when_dir_id_is_integer(self):
        o2g = OrganizationToGroupFactory()
        data = {'name': 'the group', 'org_id': int(o2g.org.dir_id)}
        response = self.client.post(
            '/admin/api/v2/survey-groups/',
            data=data, format='json', HTTP_X_ORGS=o2g.org.dir_id,
        )
        self.assertEqual(response.status_code, 201)
        self.assertEqual(response.data['org_id'], o2g.org.dir_id)

        surveygroup = SurveyGroup.objects.get(pk=response.data['id'])
        self.assertEqual(surveygroup.org, o2g.org)
        self.assertTrue(self.user.has_perm('change_surveygroup', surveygroup))


class TestChangeFollowType(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_superuser=True)

    def test_can_change_follow_type_from_none(self):
        survey = SurveyFactory(follow_type=None)
        response = self.client.patch(
            f'/admin/api/v2/surveys/{survey.id}/',
            data={'follow_type': '5m'}, format='json',
        )
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['follow_type'], '5m')

    def test_can_change_follow_type_to_none(self):
        survey = SurveyFactory(follow_type='5m')
        response = self.client.patch(
            f'/admin/api/v2/surveys/{survey.id}/',
            data={'follow_type': None}, format='json',
        )
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['follow_type'], None)

    def test_can_not_change_follow_type_to_not_existing(self):
        survey = SurveyFactory(follow_type='5m')
        response = self.client.patch(
            f'/admin/api/v2/surveys/{survey.id}/',
            data={'follow_type': '6m'}, format='json',
        )
        self.assertEqual(response.status_code, 400)


class TestNoSurveysListView(TestCase):
    client_class = YandexClient

    def test_no_surveys_list_view(self):
        self.client.login_yandex(is_superuser=True)
        response = self.client.get('/v1/surveys/')
        self.assertEqual(response.status_code, 404)


class TestSurveyLanguages(TestCase):
    client_class = YandexClient

    def setUp(self) -> None:
        self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory()

    def test_no_keyset(self):
        response = self.client.get(f'/admin/api/v2/surveys/{self.survey.id}/')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['languages'], None)

    def test_no_languages_in_keyset(self):
        ct = ContentType.objects.get_for_model(Survey)
        TankerKeysetFactory(name='keyset', content_type=ct, object_id=self.survey.pk)
        response = self.client.get(f'/admin/api/v2/surveys/{self.survey.id}/')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['languages'], None)

    def test_languages_in_keyset(self):
        ct = ContentType.objects.get_for_model(Survey)
        languages = {
            'ru': tanker_utils.STATUSES['APPROVED'],
            'en': tanker_utils.STATUSES['EXPIRED'],
            'de': tanker_utils.STATUSES['REQUIRES_TRANSLATION'],
        }
        TankerKeysetFactory(name='keyset', content_type=ct, object_id=self.survey.pk, languages=languages)
        response = self.client.get(f'/admin/api/v2/surveys/{self.survey.id}/')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['languages'], languages)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_return_none_for_business(self):
        response = self.client.get(f'/admin/api/v2/surveys/{self.survey.id}/')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['languages'], None)
