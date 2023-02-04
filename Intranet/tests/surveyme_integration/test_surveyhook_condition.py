# -*- coding: utf-8 -*-
from django.test import TestCase

from unittest.mock import patch, Mock
from events.surveyme.models import AnswerType
from events.conditions.factories import ContentTypeAttributeFactory
from events.conditions.models import ConditionItemBase
from events.surveyme.factories import (
    ProfileSurveyAnswerFactory,
    SurveyFactory,
    SurveyQuestionFactory,
    SurveyQuestionChoiceFactory,
)
from events.accounts.factories import UserFactory
from events.conditions.models import ContentTypeAttribute
from events.surveyme_integration.factories import (
    SurveyHookConditionFactory,
    SurveyHookFactory,
    SurveyHookConditionNodeFactory,
)
from events.surveyme_integration.models import SurveyHookCondition


class TestSurveyHookCondition__is_true(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.profile_survey_answer = ProfileSurveyAnswerFactory(
            user=UserFactory(),
            survey=SurveyFactory(),
        )
        self.source_request_content_type_attribute = ContentTypeAttributeFactory(
            title='Язык пользователя',
            lookup_field='accept-language',
            attr='source_request'
        )
        self.hook = SurveyHookFactory()
        self.node = SurveyHookConditionNodeFactory(hook=self.hook)
        self.condition = SurveyHookConditionFactory(
            condition_node=self.node,
            content_type_attribute=self.source_request_content_type_attribute,
            value='ru',
            operator='and',
            condition='eq',
        )

    def test_is_true_for_non_source_request_content_type_attribute_should_call_super_is_true_method(self):
        not_source_request_content_type_attribute = ContentTypeAttributeFactory(
            title='Ответ на вопрос',
            attr='pk'
        )
        condition = SurveyHookConditionFactory(
            node=SurveyHookFactory(),
            content_type_attribute=not_source_request_content_type_attribute,
        )
        with patch.object(ConditionItemBase, 'is_true', Mock()) as mocked_is_true_method:
            self.assertEqual(mocked_is_true_method.call_count, 0)
            condition.is_true(profile_survey_answer=self.profile_survey_answer)
            msg = 'Если ContentTypeAttribute.attr != source_request, то должен вызываться super-метод is_true'
            self.assertEqual(mocked_is_true_method.call_count, 1, msg=msg)

    def test_condition_should_be_false_if_source_request_is_empty(self):
        with patch.object(ConditionItemBase, 'is_true', Mock()) as mocked_is_true_method:
            self.profile_survey_answer.source_request = None
            self.profile_survey_answer.save()
            self.assertEqual(mocked_is_true_method.call_count, 0)
            is_true = self.condition.is_true(profile_survey_answer=self.profile_survey_answer)
            self.assertEqual(mocked_is_true_method.call_count, 0)
            msg = 'Если source_request ProfileSurveyAnswer пуст, то условия должно быть False'
            self.assertFalse(is_true, msg=msg)

    def test_condition_should_be_false_if_lookup_field_is_not_accept_language(self):
        with patch.object(ConditionItemBase, 'is_true', Mock()) as mocked_is_true_method:
            self.profile_survey_answer.source_request = {'headers': {'accept-language': 'ru'}}
            self.profile_survey_answer.save()
            self.assertTrue(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))
            self.condition.content_type_attribute.lookup_field = 'not-accept-language'
            self.condition.content_type_attribute.save()
            msg = (
                'Если lookup_field != accept-language, даже если content_type == ProfileSurveyAnswer '
                'и attr == source_request, is_true должен вернуть False '
                '(т.к. пока нет других таких условий из source_request)'
            )
            self.assertFalse(self.condition.is_true(profile_survey_answer=self.profile_survey_answer), msg=msg)
            self.assertEqual(mocked_is_true_method.call_count, 0)

    def test_condition_should_be_true_if_accept_language_eq(self):
        self.condition.content_type_attribute.lookup_field = 'accept-language'
        self.condition.content_type_attribute.save()
        with patch.object(ConditionItemBase, 'is_true', Mock()) as mocked_is_true_method:
            experiments = [
                {'condition': 'eq', 'accept-language': 'ru', 'headers': {}, 'result': False},
                {'condition': 'eq', 'accept-language': 'ru', 'headers': {'accept-language': ''}, 'result': False},
                {'condition': 'eq', 'accept-language': 'ru', 'headers': {'accept-language': 'ru'}, 'result': True},
                {'condition': 'eq', 'accept-language': 'ru', 'headers': {'accept-language': 'ru-RU'}, 'result': True},
                {'condition': 'eq', 'accept-language': 'ru', 'headers': {'accept-language': 'rU-RU'}, 'result': True},
                {'condition': 'neq', 'accept-language': 'ru', 'headers': {'accept-language': 'ru'}, 'result': False},
                {'condition': 'neq', 'accept-language': 'ru', 'headers': {'accept-language': 'en'}, 'result': True},
                {'condition': 'eq', 'accept-language': 'ru', 'headers': {'accept-language': 'en,ru'}, 'result': False},
                {'condition': 'eq', 'accept-language': 'ru', 'headers': {'accept-language': 'en;q=0.5,ru'}, 'result': True},
                {'condition': 'eq', 'accept-language': 'ru', 'headers': {'accept-language': 'ru;q=0.4,en;q=0.5'}, 'result': False},
            ]
            for experiment in experiments:
                self.condition.condition = experiment['condition']
                self.condition.value = experiment['accept-language']
                self.condition.save()
                self.profile_survey_answer.source_request = {'headers': experiment['headers']}
                self.profile_survey_answer.save()
                msg = 'Condition: {condition}, accept-language: {accept_language}, headers: {headers}'.format(
                    condition=experiment['condition'],
                    headers=experiment['headers'],
                    accept_language=experiment['accept-language'],
                )
                self.assertEqual(
                    self.condition.is_true(profile_survey_answer=self.profile_survey_answer),
                    experiment['result'],
                    msg=msg,
                )
                self.assertEqual(mocked_is_true_method.call_count, 0)

    def test_condition_should_get_accept_language_from_query_params(self):
        self.condition.content_type_attribute.lookup_field = 'accept-language'
        self.condition.content_type_attribute.save()
        with patch.object(ConditionItemBase, 'is_true', Mock()) as mocked_is_true_method:
            experiments = [
                {
                    'condition': 'eq',
                    'accept-language': 'ru',
                    'source_request': {'headers': {'accept-language': 'ru'}, 'query_params': {'lang': 'en'}},
                    'result': False,
                },
                {
                    'condition': 'neq',
                    'accept-language': 'ru',
                    'source_request': {'headers': {'accept-language': 'en'}, 'query_params': {'lang': 'ru'}},
                    'result': False,
                },
                {
                    'condition': 'eq',
                    'accept-language': 'ru',
                    'source_request': {'headers': {'accept-language': 'ru'}, 'query_params': {'lang': ['en', 'ru']}},
                    'result': False,
                },
                {
                    'condition': 'eq',
                    'accept-language': 'ru',
                    'source_request': {'headers': {'accept-language': 'en'}, 'query_params': {'lang': 'ru'}},
                    'result': True,
                },
                {
                    'condition': 'eq',
                    'accept-language': 'ru',
                    'source_request': {'headers': {'accept-language': 'ru'}, 'query_params': {'lang': 'ru'}},
                    'result': True,
                },
                {
                    'condition': 'eq',
                    'accept-language': 'ru',
                    'source_request': {'headers': {'accept-language': 'en'}, 'query_params': {'lang': ['ru', 'en']}},
                    'result': True,
                },
            ]
            for experiment in experiments:
                self.condition.condition = experiment['condition']
                self.condition.value = experiment['accept-language']
                self.condition.save()
                self.profile_survey_answer.source_request = experiment['source_request']
                self.profile_survey_answer.save()
                msg = 'Condition: {condition}, source_request: {source_request}, accept_language: {accept_language}'.format(
                    condition=experiment['condition'],
                    source_request=experiment['source_request'],
                    accept_language=experiment['accept-language'],
                )
                self.assertEqual(
                    self.condition.is_true(profile_survey_answer=self.profile_survey_answer),
                    experiment['result'],
                    msg=msg,
                )
                self.assertEqual(mocked_is_true_method.call_count, 0)

    def test_condition_should_be_false_if_parent_origin_is_none(self):
        with patch.object(ConditionItemBase, 'is_true', Mock()) as mocked_is_true_method:
            for parent_origin in [None, '', 'remove']:
                self.profile_survey_answer.source_request = {'parent_origin': parent_origin}
                if parent_origin == 'remove':
                    self.profile_survey_answer.source_request = {}
                self.profile_survey_answer.save()
                self.condition.content_type_attribute.lookup_field = 'parent_origin'
                self.condition.content_type_attribute.save()
                self.condition.value = 'ru'
                self.condition.save()
                msg = (
                    'Если parent_origin == parent_origin, но в source_request его нет или он пуст - нужно вернуть False'
                )
                self.assertFalse(self.condition.is_true(profile_survey_answer=self.profile_survey_answer), msg=msg)
                self.assertEqual(mocked_is_true_method.call_count, 0)

    def test_condition_for_parent_origin(self):
        self.condition.content_type_attribute.lookup_field = 'parent_origin'
        self.condition.content_type_attribute.save()
        with patch.object(ConditionItemBase, 'is_true', Mock()) as mocked_is_true_method:
            experiments = [
                {'condition': 'eq', 'parent_origin': 'ru', 'source_request': {'parent_origin': 'www.yandex.ru'}, 'result': True},
                {'condition': 'eq', 'parent_origin': 'ru', 'source_request': {'parent_origin': ['www.yandex.ru', 'www.yandex.com']}, 'result': True},
                {'condition': 'eq', 'parent_origin': 'ru', 'source_request': {'parent_origin': 'https://www.yandex.ru'}, 'result': True},
                {'condition': 'eq', 'parent_origin': 'ru', 'source_request': {'parent_origin': 'http://www.yandex.ru'}, 'result': True},
                {'condition': 'eq', 'parent_origin': 'ru', 'source_request': {'parent_origin': 'www.yandex.ru:80/my_path'}, 'result': True},
                {'condition': 'eq', 'parent_origin': 'ru', 'source_request': {'parent_origin': 'http://www.yandex.ru:80/my_path'}, 'result': True},
                {'condition': 'eq', 'parent_origin': 'ru', 'source_request': {'parent_origin': 'www.yandex.com'}, 'result': False},
                {'condition': 'eq', 'parent_origin': 'ru', 'source_request': {'parent_origin': ['www.yandex.com']}, 'result': False},
                {'condition': 'eq', 'parent_origin': 'ru', 'source_request': {'parent_origin': []}, 'result': False},
                {'condition': 'neq', 'parent_origin': 'ru', 'source_request': {'parent_origin': 'www.yandex.com'}, 'result': True},
                {'condition': 'neq', 'parent_origin': 'com', 'source_request': {'parent_origin': 'www.yandex.com'}, 'result': False},
                {'condition': 'eq', 'parent_origin': 'ru', 'source_request': {}, 'result': False},
                {'condition': 'eq', 'parent_origin': 'ru', 'source_request': {'parent_origin': None}, 'result': False},
                {'condition': 'eq', 'parent_origin': 'ru', 'source_request': {'parent_origin': ''}, 'result': False},
                {'condition': 'eq', 'parent_origin': 'ru', 'source_request': {'parent_origin': 'yandex.yandex'}, 'result': False},
            ]
            for experiment in experiments:
                self.condition.condition = experiment['condition']
                self.condition.value = experiment['parent_origin']
                self.condition.save()
                self.profile_survey_answer.source_request = experiment['source_request']
                self.profile_survey_answer.save()
                msg = 'Condition: {condition}, parent_origin: {parent_origin}, source_request: {source_request}'.format(
                    condition=experiment['condition'],
                    source_request=experiment['source_request'],
                    parent_origin=experiment['parent_origin'],
                )
                self.assertEqual(
                    self.condition.is_true(profile_survey_answer=self.profile_survey_answer),
                    experiment['result'],
                    msg=msg,
                )
                self.assertEqual(mocked_is_true_method.call_count, 0)

    def test_condition_should_be_true_if_one_node_is_true(self):
        ContentTypeAttribute.objects.all().delete()
        hook = SurveyHookFactory()
        node = SurveyHookConditionNodeFactory(hook=hook)
        source_request_content_type_attribute = ContentTypeAttributeFactory(
            title='Язык пользователя',
            lookup_field='accept-language',
            attr='source_request'
        )
        first_condition = SurveyHookConditionFactory(
            condition_node=node,
            content_type_attribute=source_request_content_type_attribute,
            value='ru',
            operator='and',
            condition='eq',
        )

        second_node = SurveyHookConditionNodeFactory(hook=hook)
        second_source_request_content_type_attribute = ContentTypeAttributeFactory(
            title='Parent origin',
            lookup_field='parent_origin',
            attr='source_request',
        )
        second_condition = SurveyHookConditionFactory(
            condition_node=second_node,
            content_type_attribute=second_source_request_content_type_attribute,
            value='ru',
            operator='and',
            condition='eq',
        )

        experiments = [
            {
                'source_request': {'parent_origin': 'www.yandex.ru', 'headers': {'accept-language': 'ru'}},
                'first_result': True,
                'second_result': True,
                'result': True,
            },
            {
                'source_request': {'parent_origin': 'www.yandex.ru', 'headers': {'accept-language': 'en'}},
                'first_result': False,
                'second_result': True,
                'result': True,
            },
            {
                'source_request': {'parent_origin': 'www.yandex.com', 'headers': {'accept-language': 'ru'}},
                'first_result': True,
                'second_result': False,
                'result': True,
            },
            {
                'source_request': {'parent_origin': 'www.yandex.com', 'headers': {'accept-language': 'en'}},
                'first_result': False,
                'second_result': False,
                'result': False,
            },
        ]
        for experiment in experiments:
            self.profile_survey_answer.source_request = experiment['source_request']
            self.profile_survey_answer.save()
            self.assertEqual(first_condition.is_true(profile_survey_answer=self.profile_survey_answer), experiment['first_result'])
            self.assertEqual(second_condition.is_true(profile_survey_answer=self.profile_survey_answer), experiment['second_result'])
            self.assertEqual(hook.is_true(profile_survey_answer=self.profile_survey_answer), experiment['result'])

    def test_condition_should_be_true_if_no_conditions(self):
        ContentTypeAttribute.objects.all().delete()
        hook = SurveyHookFactory()
        self.assertTrue(hook.is_true(profile_survey_answer=self.profile_survey_answer))


class TestSurveyHookCondition__is_true_for_answer_short_text(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.profile_survey_answer = ProfileSurveyAnswerFactory(
            user=UserFactory(uid=None),
            survey=SurveyFactory(),
        )
        self.content_type_attribute = ContentTypeAttributeFactory(
            title='Ответ на вопрос "Короткий ответ"',
            attr='answer_short_text',
            allowed_conditions=['eq', 'neq'],
        )
        self.survey = SurveyFactory(is_published_external=True)
        answer_short_text = AnswerType.objects.get(slug='answer_short_text')
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=answer_short_text
        )

        self.hook = SurveyHookFactory(survey=self.survey)
        self.node = SurveyHookConditionNodeFactory(hook=self.hook)
        self.exc_answer_value = 'ping'
        self.condition = SurveyHookConditionFactory(
            condition_node=self.node,
            content_type_attribute=self.content_type_attribute,
            value=self.exc_answer_value,
            operator='and',
            condition='eq',
            survey_question=self.question,
        )

    def test_condition_eq_should_be_false(self):
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': 'pong',
            }],
        }
        self.profile_survey_answer.save()
        self.assertFalse(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_neq_should_be_true(self):
        self.condition.condition = 'neq'
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': 'pong',
            }],
        }
        self.profile_survey_answer.save()
        self.assertTrue(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_eq_should_be_false_if_value_is_empty(self):
        self.condition.value = ''
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': 'pong',
            }],
        }
        self.profile_survey_answer.save()
        self.assertFalse(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_neq_should_be_true_if_value_is_empty(self):
        self.condition.condition = 'neq'
        self.condition.value = ''
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': 'pong',
            }],
        }
        self.profile_survey_answer.save()
        self.assertTrue(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_eq_should_be_true(self):
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': self.exc_answer_value,
            }],
        }
        self.profile_survey_answer.save()
        self.assertTrue(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_neq_should_be_false(self):
        self.condition.condition = 'neq'
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': self.exc_answer_value,
            }],
        }
        self.profile_survey_answer.save()
        self.assertFalse(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_eq_should_be_true_if_both_values_are_empty(self):
        self.condition.value = ''
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': '',
            }],
        }
        self.profile_survey_answer.save()
        self.assertTrue(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_neq_should_be_false_if_both_values_are_empty(self):
        self.condition.condition = 'neq'
        self.condition.value = ''
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': '',
            }],
        }
        self.profile_survey_answer.save()
        self.assertFalse(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))


class TestSurveyHookCondition__is_true_for_answer_date(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.profile_survey_answer = ProfileSurveyAnswerFactory(
            user=UserFactory(uid=None),
            survey=SurveyFactory(),
        )
        self.content_type_attribute = ContentTypeAttributeFactory(
            title='Ответ на вопрос "Дата"',
            attr='answer_date.date_start',
            allowed_conditions=['eq', 'neq', 'gt', 'lt'],
        )
        self.survey = SurveyFactory(is_published_external=True)
        answer_date = AnswerType.objects.get(slug='answer_date')
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=answer_date
        )

        self.hook = SurveyHookFactory(survey=self.survey)
        self.node = SurveyHookConditionNodeFactory(hook=self.hook)
        self.exc_answer_value = '2020-06-17'
        self.condition = SurveyHookConditionFactory(
            condition_node=self.node,
            content_type_attribute=self.content_type_attribute,
            value=self.exc_answer_value,
            operator='and',
            condition='eq',
            survey_question=self.question,
        )

    def test_condition_eq_should_be_false_if_doesnt_equal(self):
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': '2020-06-01',
            }],
        }
        self.profile_survey_answer.save()
        self.assertFalse(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_neq_should_be_true_if_doesnt_equal(self):
        self.condition.condition = 'neq'
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': '2020-06-01',
            }],
        }
        self.profile_survey_answer.save()
        self.assertTrue(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_lt_should_be_true_if_less(self):
        self.condition.condition = 'lt'
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': '2020-06-01',
            }],
        }
        self.profile_survey_answer.save()
        self.assertTrue(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_gt_should_be_false_if_less(self):
        self.condition.condition = 'gt'
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': '2020-06-01',
            }],
        }
        self.profile_survey_answer.save()
        self.assertFalse(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_eq_should_be_false_if_value_is_empty(self):
        self.condition.value = ''
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': '2020-06-01',
            }],
        }
        self.profile_survey_answer.save()
        self.assertFalse(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_neq_should_be_true_if_value_is_empty(self):
        self.condition.condition = 'neq'
        self.condition.value = ''
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': '2020-06-01',
            }],
        }
        self.profile_survey_answer.save()
        self.assertTrue(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_lt_should_be_false_if_value_is_empty(self):
        self.condition.condition = 'lt'
        self.condition.value = ''
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': '2020-06-01',
            }],
        }
        self.profile_survey_answer.save()
        self.assertFalse(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_gt_should_be_false_if_value_is_empty(self):
        self.condition.condition = 'gt'
        self.condition.value = ''
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': '2020-06-01',
            }],
        }
        self.profile_survey_answer.save()
        self.assertFalse(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_eq_should_be_true_if_equals(self):
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': self.exc_answer_value,
            }],
        }
        self.profile_survey_answer.save()
        self.assertTrue(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_neq_should_be_false_if_equals(self):
        self.condition.condition = 'neq'
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': self.exc_answer_value,
            }],
        }
        self.profile_survey_answer.save()
        self.assertFalse(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_eq_should_be_true_if_both_values_are_empty(self):
        self.condition.value = ''
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': None,
            }],
        }
        self.profile_survey_answer.save()
        self.assertTrue(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_neq_should_be_false_if_both_values_are_empty(self):
        self.condition.condition = 'neq'
        self.condition.value = ''
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': None,
            }],
        }
        self.profile_survey_answer.save()
        self.assertFalse(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_lt_should_be_false_if_both_values_are_empty(self):
        self.condition.condition = 'lt'
        self.condition.value = ''
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': None,
            }],
        }
        self.profile_survey_answer.save()
        self.assertFalse(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_gt_should_be_false_if_both_values_are_empty(self):
        self.condition.condition = 'gt'
        self.condition.value = ''
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': None,
            }],
        }
        self.profile_survey_answer.save()
        self.assertFalse(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_eq_should_be_false_if_date_range_not_empty(self):
        self.condition.condition = 'eq'
        self.condition.value = ''
        self.condition.save()
        self.question.param_date_field_type = 'daterange'
        self.question.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': {
                    'begin': '2021-09-01',
                    'end': '2021-09-30',
                },
            }],
        }
        self.profile_survey_answer.save()
        self.assertFalse(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_eq_should_be_true_if_date_range_not_empty(self):
        self.condition.condition = 'neq'
        self.condition.value = ''
        self.condition.save()
        self.question.param_date_field_type = 'daterange'
        self.question.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': {
                    'begin': '2021-09-01',
                    'end': '2021-09-30',
                },
            }],
        }
        self.profile_survey_answer.save()
        self.assertTrue(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))


class TestSurveyHookCondition__for_deleted_questions(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.profile_survey_answer = ProfileSurveyAnswerFactory(
            user=UserFactory(uid=None),
            survey=SurveyFactory(),
        )
        self.content_type_attribute = ContentTypeAttributeFactory(
            title='Ответ на вопрос "Короткий ответ"',
            attr='answer_short_text'
        )
        self.survey = SurveyFactory(is_published_external=True)
        answer_short_text = AnswerType.objects.get(slug='answer_short_text')
        self.short_text_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=answer_short_text
        )

        self.hook = SurveyHookFactory(survey=self.survey)
        self.node = SurveyHookConditionNodeFactory(hook=self.hook)

        self.exc_answer_value = 'ping'
        self.first_condition = SurveyHookConditionFactory(
            condition_node=self.node,
            content_type_attribute=self.content_type_attribute,
            value=self.exc_answer_value,
            operator='and',
            condition='eq',
            survey_question=self.short_text_question,
        )

        answer_long_text = AnswerType.objects.get(slug='answer_long_text')
        self.long_text_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=answer_long_text
        )
        self.content_type_attribute = ContentTypeAttributeFactory(
            title='Ответ на вопрос "Длинный ответ"',
            attr='answer_long_text'
        )
        self.long_text_exc_answer_value = 'smth'
        self.second_condition = SurveyHookConditionFactory(
            condition_node=self.node,
            content_type_attribute=self.content_type_attribute,
            value=self.long_text_exc_answer_value,
            operator='and',
            condition='eq',
            survey_question=self.long_text_question,
        )

    def test_should_filter_correct_after_delete(self):
        self.assertEqual(SurveyHookCondition.objects.count(), 2)
        self.long_text_question.is_deleted = True
        self.long_text_question.save()
        self.assertEqual(SurveyHookCondition.objects.count(), 1)

    def test_node_should_filter_deleted_questions(self):
        self.assertEqual(self.node.items.count(), 2)
        self.assertEqual(self.hook.condition_nodes.count(), 1)

        self.long_text_question.is_deleted = True
        self.long_text_question.save()
        self.assertEqual(self.node.items.count(), 1)
        self.assertEqual(self.hook.condition_nodes.count(), 1)
        condition = self.node.items.first()
        self.assertEqual(condition.survey_question, self.short_text_question)

        self.short_text_question.is_deleted = True
        self.short_text_question.save()
        self.assertEqual(self.node.items.count(), 0)
        self.assertEqual(self.hook.condition_nodes.count(), 0)

    def test_condition_should_be_true_after_delete(self):
        self.long_text_question.is_deleted = True
        self.long_text_question.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.short_text_question.get_answer_info(),
                'value': self.exc_answer_value,
            }],
        }
        self.profile_survey_answer.save()
        self.assertTrue(self.hook.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_should_be_false_before_delete_with_wrong_answer(self):
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.short_text_question.get_answer_info(),
                'value': self.exc_answer_value,
            }],
        }
        self.profile_survey_answer.save()
        self.assertFalse(self.hook.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_should_be_true_before_delete_with_correct_answer(self):
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.short_text_question.get_answer_info(),
                'value': self.exc_answer_value,
            }, {
                'question': self.long_text_question.get_answer_info(),
                'value': self.long_text_exc_answer_value,
            }],
        }
        self.profile_survey_answer.save()
        self.assertTrue(self.hook.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_should_be_true_after_delete_all(self):
        self.long_text_question.is_deleted = True
        self.long_text_question.save()
        self.short_text_question.is_deleted = True
        self.short_text_question.save()

        answer_number = AnswerType.objects.get(slug='answer_number')
        self.number_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=answer_number
        )

        self.profile_survey_answer.data = {
            'data': [{
                'question': self.number_question.get_answer_info(),
                'value': 2,
            }],
        }
        self.profile_survey_answer.save()
        self.assertTrue(self.hook.is_true(profile_survey_answer=self.profile_survey_answer))


class TestSurveyHookCondition__is_true_for_answer_boolean(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_boolean'),
        )
        self.profile_survey_answer = ProfileSurveyAnswerFactory(
            user=UserFactory(uid=None),
            survey=self.survey,
        )

        self.content_type_attribute = ContentTypeAttributeFactory(
            attr='answer_boolean',
            allowed_conditions=['eq', 'neq'],
        )
        self.hook = SurveyHookFactory(survey=self.survey)
        self.node = SurveyHookConditionNodeFactory(hook=self.hook)
        self.exc_answer_value = str(True)
        self.condition = SurveyHookConditionFactory(
            condition_node=self.node,
            content_type_attribute=self.content_type_attribute,
            value=self.exc_answer_value,
            operator='and',
            condition='eq',
            survey_question=self.question,
        )

    def test_condition_eq_should_be_false(self):
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': False,
            }],
        }
        self.profile_survey_answer.save()
        self.assertFalse(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_neq_should_be_true(self):
        self.condition.condition = 'neq'
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': False,
            }],
        }
        self.profile_survey_answer.save()
        self.assertTrue(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_eq_should_be_false_if_value_is_empty(self):
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': None,
            }],
        }
        self.profile_survey_answer.save()
        self.assertFalse(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_eq_should_be_true(self):
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': True,
            }],
        }
        self.profile_survey_answer.save()
        self.assertTrue(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_neq_should_be_false(self):
        self.condition.condition = 'neq'
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': True,
            }],
        }
        self.profile_survey_answer.save()
        self.assertFalse(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_eq_should_be_true_if_both_values_are_empty(self):
        self.condition.value = ''
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': None,
            }],
        }
        self.profile_survey_answer.save()
        self.assertTrue(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_neq_should_be_false_if_both_values_are_empty(self):
        self.condition.condition = 'neq'
        self.condition.value = ''
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': None,
            }],
        }
        self.profile_survey_answer.save()
        self.assertFalse(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))


class TestSurveyHookCondition__is_true_for_answer_choices(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
        )
        self.choices = [
            SurveyQuestionChoiceFactory(survey_question=self.question),
            SurveyQuestionChoiceFactory(survey_question=self.question),
            SurveyQuestionChoiceFactory(survey_question=self.question),
        ]
        self.profile_survey_answer = ProfileSurveyAnswerFactory(
            user=UserFactory(uid=None),
            survey=self.survey,
        )

        self.content_type_attribute = ContentTypeAttributeFactory(
            attr='answer_choices',
            allowed_conditions=['eq', 'neq'],
        )
        self.hook = SurveyHookFactory(survey=self.survey)
        self.node = SurveyHookConditionNodeFactory(hook=self.hook)
        self.exc_answer_value = str(self.choices[1].pk)
        self.condition = SurveyHookConditionFactory(
            condition_node=self.node,
            content_type_attribute=self.content_type_attribute,
            value=self.exc_answer_value,
            operator='and',
            condition='eq',
            survey_question=self.question,
        )

    def test_condition_eq_should_be_false(self):
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': [
                    {
                        'key': str(self.choices[0].pk),
                    },
                    {
                        'key': str(self.choices[2].pk),
                    },
                ],
            }],
        }
        self.profile_survey_answer.save()
        self.assertFalse(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_neq_should_be_true(self):
        self.condition.condition = 'neq'
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': [
                    {
                        'key': str(self.choices[0].pk),
                    },
                    {
                        'key': str(self.choices[2].pk),
                    },
                ],
            }],
        }
        self.profile_survey_answer.save()
        self.assertTrue(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_eq_should_be_false_if_value_is_empty(self):
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': None,
            }],
        }
        self.profile_survey_answer.save()
        self.assertFalse(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_eq_should_be_true(self):
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': [
                    {
                        'key': str(self.choices[0].pk),
                    },
                    {
                        'key': str(self.choices[1].pk),
                    },
                ],
            }],
        }
        self.profile_survey_answer.save()
        self.assertTrue(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_neq_should_be_false(self):
        self.condition.condition = 'neq'
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': [
                    {
                        'key': str(self.choices[0].pk),
                    },
                    {
                        'key': str(self.choices[1].pk),
                    },
                ],
            }],
        }
        self.profile_survey_answer.save()
        self.assertFalse(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_eq_should_be_true_if_both_values_are_empty(self):
        self.condition.value = ''
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': None,
            }],
        }
        self.profile_survey_answer.save()
        self.assertTrue(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_neq_should_be_false_if_both_values_are_empty(self):
        self.condition.condition = 'neq'
        self.condition.value = ''
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': None,
            }],
        }
        self.profile_survey_answer.save()
        self.assertFalse(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))


class TestSurveyHookCondition__is_true_for_answer_number(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_number'),
        )
        self.profile_survey_answer = ProfileSurveyAnswerFactory(
            user=UserFactory(uid=None),
            survey=self.survey,
        )

        self.content_type_attribute = ContentTypeAttributeFactory(
            attr='answer_number',
            allowed_conditions=['eq', 'neq', 'gt', 'lt'],
        )
        self.hook = SurveyHookFactory(survey=self.survey)
        self.node = SurveyHookConditionNodeFactory(hook=self.hook)
        self.exc_answer_value = '42'
        self.condition = SurveyHookConditionFactory(
            condition_node=self.node,
            content_type_attribute=self.content_type_attribute,
            value=self.exc_answer_value,
            operator='and',
            condition='eq',
            survey_question=self.question,
        )

    def test_condition_eq_should_be_false(self):
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': 12,
            }],
        }
        self.profile_survey_answer.save()
        self.assertFalse(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_lt_should_be_false(self):
        self.condition.condition = 'lt'
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': 72,
            }],
        }
        self.profile_survey_answer.save()
        self.assertFalse(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_gt_should_be_false(self):
        self.condition.condition = 'gt'
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': 12,
            }],
        }
        self.profile_survey_answer.save()
        self.assertFalse(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_neq_should_be_true(self):
        self.condition.condition = 'neq'
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': 12,
            }],
        }
        self.profile_survey_answer.save()
        self.assertTrue(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_eq_should_be_false_if_value_is_empty(self):
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': None,
            }],
        }
        self.profile_survey_answer.save()
        self.assertFalse(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_lt_should_be_false_if_value_is_empty(self):
        self.condition.condition = 'lt'
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': None,
            }],
        }
        self.profile_survey_answer.save()
        self.assertFalse(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_gt_should_be_false_if_value_is_empty(self):
        self.condition.condition = 'gt'
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': None,
            }],
        }
        self.profile_survey_answer.save()
        self.assertFalse(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_eq_should_be_true(self):
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': 42,
            }],
        }
        self.profile_survey_answer.save()
        self.assertTrue(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_lt_should_be_true(self):
        self.condition.condition = 'lt'
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': 12,
            }],
        }
        self.profile_survey_answer.save()
        self.assertTrue(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_gt_should_be_true(self):
        self.condition.condition = 'gt'
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': 72,
            }],
        }
        self.profile_survey_answer.save()
        self.assertTrue(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_neq_should_be_false(self):
        self.condition.condition = 'neq'
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': 42,
            }],
        }
        self.profile_survey_answer.save()
        self.assertFalse(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_eq_should_be_true_if_both_values_are_empty(self):
        self.condition.value = ''
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': None,
            }],
        }
        self.profile_survey_answer.save()
        self.assertTrue(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_neq_should_be_false_if_both_values_are_empty(self):
        self.condition.condition = 'neq'
        self.condition.value = ''
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': None,
            }],
        }
        self.profile_survey_answer.save()
        self.assertFalse(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_lt_should_be_false_if_both_values_are_empty(self):
        self.condition.condition = 'lt'
        self.condition.value = ''
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': None,
            }],
        }
        self.profile_survey_answer.save()
        self.assertFalse(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))

    def test_condition_gt_should_be_false_if_both_values_are_empty(self):
        self.condition.condition = 'gt'
        self.condition.value = ''
        self.condition.save()
        self.profile_survey_answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': None,
            }],
        }
        self.profile_survey_answer.save()
        self.assertFalse(self.condition.is_true(profile_survey_answer=self.profile_survey_answer))


class TestSurveyHookCondition__is_true_for_user_groups(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.profile_survey_answer = ProfileSurveyAnswerFactory(
            user=UserFactory(),
            survey=SurveyFactory(),
        )

    def test_should_skip_condition_item_with_attr_user_groups(self):
        content_type_attribute = ContentTypeAttributeFactory(
            lookup_field='pk',
            attr='user.groups'
        )
        hook = SurveyHookFactory()
        node = SurveyHookConditionNodeFactory(hook=hook)
        condition = SurveyHookConditionFactory(
            condition_node=node,
            content_type_attribute=content_type_attribute,
            value='19',  # some auth group id
            operator='and',
            condition='eq',
        )

        with patch.object(ConditionItemBase, 'is_true', Mock()) as mocked_is_true_method:
            is_true = condition.is_true(profile_survey_answer=self.profile_survey_answer)
            self.assertTrue(is_true)
            self.assertEqual(mocked_is_true_method.call_count, 0)

    def test_should_skip_condition_item_with_attr_groups(self):
        content_type_attribute = ContentTypeAttributeFactory(
            lookup_field='pk',
            attr='groups'
        )
        hook = SurveyHookFactory()
        node = SurveyHookConditionNodeFactory(hook=hook)
        condition = SurveyHookConditionFactory(
            condition_node=node,
            content_type_attribute=content_type_attribute,
            value='19',  # some auth group id
            operator='and',
            condition='eq',
        )

        with patch.object(ConditionItemBase, 'is_true', Mock()) as mocked_is_true_method:
            is_true = condition.is_true(profile_survey_answer=self.profile_survey_answer)
            self.assertTrue(is_true)
            self.assertEqual(mocked_is_true_method.call_count, 0)
