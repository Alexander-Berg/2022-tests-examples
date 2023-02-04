# -*- coding: utf-8 -*-
from django.test import TestCase

from events.conditions.factories import ContentTypeAttributeFactory
from events.surveyme.factories import (
    SurveyQuestionFactory,
    SurveyFactory,
    SurveyQuestionChoiceFactory,
)
from events.surveyme.models import AnswerType
from events.accounts.helpers import YandexClient


class TestSurveyQuestionShowConditionNodeViewSet_save_with_items(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.post_url = '/admin/api/v2/survey-question-show-condition-nodes/save-with-items/'

        self.survey = SurveyFactory()
        answer_choices = AnswerType.objects.get(slug='answer_choices')
        self.cta = ContentTypeAttributeFactory(lookup_field='answer_choices')

        self.survey_questions = [
            SurveyQuestionFactory(answer_type=answer_choices, survey=self.survey, position=1),
            SurveyQuestionFactory(answer_type=answer_choices, survey=self.survey, position=2),
            SurveyQuestionFactory(answer_type=answer_choices, survey=self.survey, position=3),
        ]

        # create 3 choices for every question
        for question in self.survey_questions:
            for i in range(3):
                SurveyQuestionChoiceFactory(survey_question=question)

        self.client.login_yandex(is_superuser=True)

    def post_data(self, data):
        return self.client.post(self.post_url, data=data, format='json')

    def test_should_create_node_with_items(self):
        data = {
            'survey_question': self.survey_questions[2].id,
            'nodes': [
                {
                    'survey_question': self.survey_questions[2].id,
                    'items': [
                        {
                            'condition': 'eq',
                            'operator': 'and',
                            'survey_question': self.survey_questions[0].id,
                            'content_type_attribute': self.cta.pk,
                            'value': self.survey_questions[0].surveyquestionchoice_set.all()[0].id
                        }
                    ]
                }
            ]
        }
        response = self.post_data(data)
        self.assertEqual(response.data, {"save_nodes_with_ids": [1], "saved_related_objects": {'items': [1]}})

        msg = 'должна быть создана одна нода show condition'
        self.assertEqual(self.survey_questions[2].show_condition_nodes.count(), 1, msg=msg)

        items = self.survey_questions[2].show_condition_nodes.all()[0].items.all()
        self.assertEqual(len(items), 1, 'у show condition node должен быть создан 1 item')

        self.assertEqual(items[0].condition, 'eq')
        self.assertEqual(items[0].operator, 'and')
        self.assertEqual(items[0].survey_question, self.survey_questions[0])
        self.assertEqual(items[0].value, str(self.survey_questions[0].surveyquestionchoice_set.all()[0].id))

    def test_should_validate_group(self):
        group_question = SurveyQuestionFactory(
            answer_type=AnswerType.objects.get(slug='answer_group'),
            survey=self.survey
        )
        self.survey_questions[0].group_id = group_question.id
        self.survey_questions[1].group_id = group_question.id
        self.survey_questions[0].save()
        self.survey_questions[1].save()

        another_group = SurveyQuestionFactory(
            answer_type=AnswerType.objects.get(slug='answer_group'),
            survey=self.survey
        )
        self.survey_questions[2].group_id = another_group.id
        self.survey_questions[2].save()

        data = {
            'survey_question': self.survey_questions[2].id,
            'nodes': [
                {
                    'survey_question': self.survey_questions[2].id,
                    'items': [
                        {
                            'condition': 'eq',
                            'operator': 'and',
                            'survey_question': group_question.id,
                            'content_type_attribute': self.cta.pk,
                            'value': 'test',
                        }
                    ]
                }
            ]
        }
        response = self.post_data(data)
        self.assertEqual(response.status_code, 400)
        self.assertEqual(
            str(response.data['error_message'][0]),
            'Нельзя использовать вопросы из серии в логике показа'
        )

        data = {
            'survey_question': self.survey_questions[2].id,
            'nodes': [
                {
                    'survey_question': self.survey_questions[2].id,
                    'items': [
                        {
                            'condition': 'eq',
                            'operator': 'and',
                            'survey_question': self.survey_questions[0].id,
                            'content_type_attribute': self.cta.pk,
                            'value': self.survey_questions[0].surveyquestionchoice_set.all()[0].id
                        }
                    ]
                }
            ]
        }
        response = self.post_data(data)
        self.assertEqual(response.status_code, 400)
        self.assertEqual(
            str(response.data['error_message'][0]),
            'Нельзя использовать серию вопросов из другой серии в настройке логики показа'
        )

        data = {
            'survey_question': self.survey_questions[1].id,
            'nodes': [
                {
                    'survey_question': self.survey_questions[1].id,
                    'items': [
                        {
                            'condition': 'eq',
                            'operator': 'and',
                            'survey_question': self.survey_questions[0].id,
                            'content_type_attribute': self.cta.pk,
                            'value': self.survey_questions[0].surveyquestionchoice_set.all()[0].id
                        }
                    ]
                }
            ]
        }
        response = self.post_data(data)
        self.assertEqual(response.data, {"save_nodes_with_ids": [1], "saved_related_objects": {'items': [1]}})

    def test_should_set_node_position_attribute_as_position_in_json(self):
        data = {
            'survey_question': self.survey_questions[2].id,
            'nodes': [
                {
                    'id': 100,
                    'survey_question': self.survey_questions[2].id,
                    'items': [
                        {
                            'condition': 'eq',
                            'operator': 'and',
                            'survey_question': self.survey_questions[0].id,
                            'content_type_attribute': self.cta.pk,
                            'value': self.survey_questions[0].surveyquestionchoice_set.all()[0].id
                        }
                    ]
                },
                {
                    'id': 1,
                    'survey_question': self.survey_questions[2].id,
                    'items': [
                        {
                            'condition': 'eq',
                            'operator': 'and',
                            'survey_question': self.survey_questions[0].id,
                            'content_type_attribute': self.cta.pk,
                            'value': self.survey_questions[0].surveyquestionchoice_set.all()[1].id
                        }
                    ]
                }
            ]
        }
        self.post_data(data)
        self.assertEqual(self.survey_questions[2].show_condition_nodes.count(), 2)
        self.assertEqual(self.survey_questions[2].show_condition_nodes.get(id=100).position, 1)
        self.assertEqual(self.survey_questions[2].show_condition_nodes.get(id=1).position, 2)

    def test_should_delete_node_if_it_is_no_more_in_list(self):
        data = {
            'survey_question': self.survey_questions[2].id,
            'nodes': [
                {
                    'survey_question': self.survey_questions[2].id,
                    'items': [
                        {
                            'condition': 'eq',
                            'operator': 'and',
                            'survey_question': self.survey_questions[0].id,
                            'content_type_attribute': self.cta.pk,
                            'value': self.survey_questions[0].surveyquestionchoice_set.all()[0].id
                        }
                    ]
                },
                {
                    'survey_question': self.survey_questions[2].id,
                    'items': [
                        {
                            'condition': 'eq',
                            'operator': 'and',
                            'survey_question': self.survey_questions[0].id,
                            'content_type_attribute': self.cta.pk,
                            'value': self.survey_questions[0].surveyquestionchoice_set.all()[1].id
                        }
                    ]
                }
            ]
        }
        self.post_data(data)
        self.assertEqual(self.survey_questions[2].show_condition_nodes.count(), 2)

        data['nodes'] = [data['nodes'][0]]
        self.post_data(data)
        msg = 'должны удаляться все condition node, не переданные для сохранения'
        self.assertEqual(self.survey_questions[2].show_condition_nodes.count(), 1, msg)

    def test_should_set_node_item_position_attribute_as_position_in_json(self):
        data = {
            'survey_question': self.survey_questions[2].id,
            'nodes': [
                {
                    'survey_question': self.survey_questions[2].id,
                    'items': [
                        {
                            'id': 100,
                            'condition': 'eq',
                            'operator': 'and',
                            'survey_question': self.survey_questions[0].id,
                            'content_type_attribute': self.cta.pk,
                            'value': self.survey_questions[0].surveyquestionchoice_set.all()[0].id
                        },
                        {
                            'id': 1,
                            'condition': 'eq',
                            'operator': 'and',
                            'survey_question': self.survey_questions[1].id,
                            'content_type_attribute': self.cta.pk,
                            'value': self.survey_questions[1].surveyquestionchoice_set.all()[0].id
                        }
                    ]
                }
            ]
        }
        self.post_data(data)
        items = self.survey_questions[2].show_condition_nodes.all()[0].items.all()
        self.assertEqual(len(items), 2)
        self.assertEqual(items.get(id=100).position, 1)
        self.assertEqual(items.get(id=1).position, 2)

    def test_should_delete_node_item_if_it_is_no_more_in_list(self):
        data = {
            'survey_question': self.survey_questions[2].id,
            'nodes': [
                {
                    'survey_question': self.survey_questions[2].id,
                    'items': [
                        {
                            'condition': 'eq',
                            'operator': 'and',
                            'survey_question': self.survey_questions[0].id,
                            'content_type_attribute': self.cta.pk,
                            'value': self.survey_questions[0].surveyquestionchoice_set.all()[0].id
                        },
                        {
                            'condition': 'eq',
                            'operator': 'and',
                            'survey_question': self.survey_questions[1].id,
                            'content_type_attribute': self.cta.pk,
                            'value': self.survey_questions[1].surveyquestionchoice_set.all()[0].id
                        }
                    ]
                }
            ]
        }
        self.post_data(data)
        items = self.survey_questions[2].show_condition_nodes.all()[0].items.all()
        self.assertEqual(len(items), 2)

        data['nodes'][0]['items'] = [data['nodes'][0]['items'][0]]
        self.post_data(data)
        msg = 'должны удаляться все condition node items, не переданные для сохранения'
        items = self.survey_questions[2].show_condition_nodes.all()[0].items.all()
        self.assertEqual(len(items), 1, msg)

    def test_should_delete_all_nodes___if_nodes_is_empty_list(self):
        data = {
            'survey_question': self.survey_questions[2].id,
            'nodes': [
                {
                    'survey_question': self.survey_questions[2].id,
                    'items': [
                        {
                            'condition': 'eq',
                            'operator': 'and',
                            'survey_question': self.survey_questions[0].id,
                            'content_type_attribute': self.cta.pk,
                            'value': self.survey_questions[0].surveyquestionchoice_set.all()[0].id
                        }
                    ]
                }
            ]
        }
        self.post_data(data)
        nodes = self.survey_questions[2].show_condition_nodes.all()
        self.assertEqual(len(nodes), 1)

        data['nodes'] = []
        self.post_data(data)
        msg = 'должны удаляться все nodes, если передан пустой список'
        self.assertEqual(len(self.survey_questions[2].show_condition_nodes.all()), 0, msg)

    def test_should_delete_all_node_items__if_items_is_empty_list(self):
        data = {
            'survey_question': self.survey_questions[2].id,
            'nodes': [
                {
                    'survey_question': self.survey_questions[2].id,
                    'items': [
                        {
                            'condition': 'eq',
                            'operator': 'and',
                            'survey_question': self.survey_questions[0].id,
                            'content_type_attribute': self.cta.pk,
                            'value': self.survey_questions[0].surveyquestionchoice_set.all()[0].id
                        },
                        {
                            'condition': 'eq',
                            'operator': 'and',
                            'survey_question': self.survey_questions[1].id,
                            'content_type_attribute': self.cta.pk,
                            'value': self.survey_questions[1].surveyquestionchoice_set.all()[0].id
                        }
                    ]
                }
            ]
        }
        self.post_data(data)
        items = self.survey_questions[2].show_condition_nodes.all()[0].items.all()
        self.assertEqual(len(items), 2)

        data['nodes'][0]['items'] = []
        self.post_data(data)
        msg = 'должны удаляться все condition node items, если передан пустой список'
        items = self.survey_questions[2].show_condition_nodes.all()[0].items.all()
        self.assertEqual(len(items), 0, msg)

    def test_should_force_first_item_with_and_operator(self):
        data = {
            'survey_question': self.survey_questions[2].id,
            'nodes': [
                {
                    'survey_question': self.survey_questions[2].id,
                    'items': [
                        {
                            'condition': 'eq',
                            'operator': 'or',
                            'survey_question': self.survey_questions[0].id,
                            'content_type_attribute': self.cta.pk,
                            'value': self.survey_questions[0].surveyquestionchoice_set.all()[0].id
                        },
                        {
                            'condition': 'eq',
                            'operator': 'or',
                            'survey_question': self.survey_questions[1].id,
                            'content_type_attribute': self.cta.pk,
                            'value': self.survey_questions[1].surveyquestionchoice_set.all()[0].id
                        }
                    ]
                }
            ]
        }
        self.post_data(data)
        items = self.survey_questions[2].show_condition_nodes.all()[0].items.all()
        expected = ['and', 'or']
        msg = 'operator первого item должен форситься в значение and'
        self.assertEqual([i.operator for i in items], expected, msg)
