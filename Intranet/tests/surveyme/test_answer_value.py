# -*- coding: utf-8 -*-
from django.test import TestCase
from django.utils.translation import ugettext as _

from events.surveyme.answer_value import (
    find_question_data,
    get_value_for_question,
    get_value_for_questions,
)
from events.surveyme.models import AnswerType
from events.surveyme.factories import (
    ProfileSurveyAnswerFactory,
    SurveyFactory,
    SurveyQuestionFactory,
)


class TestBooleanValue(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.question = SurveyQuestionFactory(
            label='Label',
            answer_type=AnswerType.objects.get(slug='answer_boolean'),
        )
        self.answer = ProfileSurveyAnswerFactory(
            survey=self.question.survey,
        )

    def test_should_return_yes(self):
        self.answer.data = {
            'data': [
                {
                    'value': True,
                    'question': self.question.get_answer_info(),
                },
            ],
        }
        self.answer.save()

        expected = _('Yes')
        self.assertEqual(get_value_for_question(self.answer, self.question.pk), expected)

        expected = [(self.question.get_label(), _('Yes'))]
        self.assertEqual(get_value_for_questions(self.answer, only_with_value=True), expected)

        expected = [(self.question.get_label(), _('Yes'))]
        self.assertEqual(get_value_for_questions(self.answer, only_with_value=False), expected)

    def test_should_return_no(self):
        self.answer.data = {
            'data': [
                {
                    'value': False,
                    'question': self.question.get_answer_info(),
                },
            ],
        }
        self.answer.save()

        expected = _('No')
        self.assertEqual(get_value_for_question(self.answer, self.question.pk), expected)

        expected = [(self.question.get_label(), _('No'))]
        self.assertEqual(get_value_for_questions(self.answer, only_with_value=True), expected)

        expected = [(self.question.get_label(), _('No'))]
        self.assertEqual(get_value_for_questions(self.answer, only_with_value=False), expected)

    def test_should_return_empty_string(self):
        self.answer.data = {
            'data': [],
        }
        self.answer.save()

        expected = ''
        self.assertEqual(get_value_for_question(self.answer, self.question.pk), expected)

        expected = []
        self.assertEqual(get_value_for_questions(self.answer, only_with_value=True), expected)

        expected = [(self.question.get_label(), '')]
        self.assertEqual(get_value_for_questions(self.answer, only_with_value=False), expected)


class TestGroupValue(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()
        self.group_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_group'),
            param_is_required=False,
        )
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            group=self.group_question,
            param_is_required=False,
        )
        self.answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
        )

    def test_should_return_text_value(self):
        self.answer.data = {
            'data': [
                {
                    'value': [
                        [{
                            'value': 'text1',
                            'question': self.question.get_answer_info(),
                        }],
                    ],
                    'question': self.group_question.get_answer_info(),
                },
            ],
        }
        self.answer.save()

        expected = f'{self.group_question.label}\n{self.question.label} - text1'
        self.assertEqual(get_value_for_question(self.answer, self.group_question.pk), expected)

        expected = [(self.group_question.label, f'{self.group_question.label}\n{self.question.label} - text1')]
        self.assertListEqual(get_value_for_questions(self.answer, only_with_value=True), expected)

        expected = [(self.group_question.label, f'{self.group_question.label}\n{self.question.label} - text1')]
        self.assertListEqual(get_value_for_questions(self.answer, only_with_value=False), expected)

        expected = 'text1'
        self.assertEqual(get_value_for_question(self.answer, self.question.pk), expected)

        expected = {
            f'{self.question.param_slug}__0': 'text1',
        }
        self.assertDictEqual(get_value_for_question(self.answer, self.question.pk, unfold_group=True), expected)

    def test_shouldnt_return_text_value(self):
        self.answer.data = {
            'data': [
                {},
            ],
        }
        self.answer.save()

        expected = ''
        self.assertEqual(get_value_for_question(self.answer, self.group_question.pk), expected)

        expected = []
        self.assertListEqual(get_value_for_questions(self.answer, only_with_value=True), expected)

        expected = [(self.group_question.label, '')]
        self.assertEqual(get_value_for_questions(self.answer, only_with_value=False), expected)

        expected = ''
        self.assertEqual(get_value_for_question(self.answer, self.question.pk), expected)

        expected = {}
        self.assertDictEqual(get_value_for_question(self.answer, self.question.pk, unfold_group=True), expected)

    def test_should_return_correct_value_for_empty_group(self):
        self.answer.data = {
            'data': [
                {
                    'value': [[None]],
                    'question': self.group_question.get_answer_info(),
                },
            ],
        }
        self.answer.save()

        self.assertEqual(get_value_for_question(self.answer, self.group_question.pk), '')

        self.assertEqual(get_value_for_questions(self.answer, only_with_value=True), [])

        expected = [(self.group_question.label, f'{self.group_question.label}\n{self.question.label} - ')]
        self.assertListEqual(get_value_for_questions(self.answer, only_with_value=False), expected)

        self.assertEqual(get_value_for_question(self.answer, self.question.pk), '')

        self.assertDictEqual(get_value_for_question(self.answer, self.question.pk, unfold_group=True), {})


class TestFindQuestionData(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.question = SurveyQuestionFactory(
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
        )
        self.group_question = SurveyQuestionFactory(
            answer_type=AnswerType.objects.get(slug='answer_group'),
        )
        self.child_question = SurveyQuestionFactory(
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            group_id=self.group_question.pk,
        )
        self.answer = ProfileSurveyAnswerFactory(
            survey=self.question.survey,
        )

    def test_should_return_regular_question(self):
        answer_data = [{
            'value': 'testit',
            'question': self.question.get_answer_info(),
        }]
        self.assertListEqual(
            list(find_question_data(answer_data, self.question.pk)), [{
                'value': 'testit',
                'question': self.question.get_answer_info(),
            }],
        )

    def test_should_return_grouped_question(self):
        answer_data = [{
            'value': [[
                {
                    'value': 'item one',
                    'question': self.child_question.get_answer_info(),
                },
            ],
            [
                {
                    'value': 'item two',
                    'question': self.child_question.get_answer_info(),
                },
            ]],
            'question': self.group_question.get_answer_info(),
        }]
        self.assertListEqual(
            list(find_question_data(answer_data, self.child_question.pk)), [{
                'value': 'item one',
                'question': self.child_question.get_answer_info(),
            }, {
                'value': 'item two',
                'question': self.child_question.get_answer_info(),
            }],
        )

    def test_shouldnt_return_regular_question(self):
        answer_data = [{
            'value': [],
            'question': self.group_question.get_answer_info(),
        }]
        self.assertListEqual(list(find_question_data(answer_data, self.question.pk)), [])

    def test_shouldnt_return_grouped_question(self):
        answer_data = [{
            'value': 'testit',
            'question': self.question.get_answer_info(),
        }]
        self.assertListEqual(list(find_question_data(answer_data, self.child_question.pk)), [])

    def test_shouldnt_fail_and_return_regular_question(self):
        answer_data = [{
            'value': 'testit',
            'question': self.question.get_answer_info(),
        }, {
            'value': [[
                {
                    'value': 'item one',
                    'question': self.child_question.get_answer_info(),
                },
            ],
            None,
            [
                {
                    'value': 'item two',
                    'question': self.child_question.get_answer_info(),
                },
            ]],
            'question': self.group_question.get_answer_info(),
        }]
        self.assertListEqual(
            list(find_question_data(answer_data, self.question.pk)), [{
                'value': 'testit',
                'question': self.question.get_answer_info(),
            }],
        )

    def test_shouldnt_fail_and_return_grouped_question(self):
        answer_data = [{
            'value': [[
                {
                    'value': 'item one',
                    'question': self.child_question.get_answer_info(),
                },
            ],
            None,
            [
                {
                    'value': 'item two',
                    'question': self.child_question.get_answer_info(),
                },
            ]],
            'question': self.group_question.get_answer_info(),
        }]
        self.assertListEqual(
            list(find_question_data(answer_data, self.child_question.pk)), [{
                'value': 'item one',
                'question': self.child_question.get_answer_info(),
            }, {
                'value': 'item two',
                'question': self.child_question.get_answer_info(),
            }],
        )
