# -*- coding: utf-8 -*-
from collections import namedtuple
from operator import attrgetter

from django.test import TestCase
from django.forms import widgets

from events.conditions.factories import ContentTypeAttributeFactory
from events.surveyme.utils import (
    FormConditionEvaluator,
    sync_questions,
    order_questions,
)
from events.common_app.utils import get_query_dict
from events.data_sources.forms import DataSourceField


class TestFormConditionEvaluator(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.content_type_attribute = ContentTypeAttributeFactory(
            title='test',
            attr='answer_short_text',
        )

    def assertEvaluation(self, form_data, field_names, fields_by_name, conditions, expected, msg=None):
        response = FormConditionEvaluator(conditions).evaluate(
            get_query_dict(form_data),
            field_names,
            fields_by_name,
        )
        self.assertEqual(response, expected, msg=msg)

    def test_simple(self):
        self.assertEvaluation(
            form_data={
                'field_1': ['1', '2'],
                'field_2': ['2'],
                'field_3': ['3']
            },
            field_names=[
                'field_1',
                'field_2',
                'field_3'
            ],
            fields_by_name={
                'field_1': DataSourceField(widget=widgets.CheckboxSelectMultiple()),
                'field_2': DataSourceField(widget=widgets.CheckboxSelectMultiple()),
                'field_3': DataSourceField(widget=widgets.CheckboxSelectMultiple()),
            },
            conditions={
                'field_2': [
                    [
                        {
                            'operator': 'and',
                            'field': 'field_1',
                            'condition': 'eq',
                            'field_value': '1',
                            'additional_info': {
                                'content_type_attribute': self.content_type_attribute,
                            }
                        }
                    ]
                ],
                'field_3': [
                    [
                        {
                            'operator': 'and',
                            'field': 'field_2',
                            'condition': 'eq',
                            'field_value': '2',
                            'additional_info': {
                                'content_type_attribute': self.content_type_attribute,
                            }
                        }
                    ]
                ]
            },
            expected={
                'field_2': True,
                'field_3': True
            }
        )

    def test_without_field_name_in_list(self):
        self.assertEvaluation(
            form_data={
                'field_1': ['1', '2'],
                'field_2': ['2'],
                'field_3': ['3']
            },
            field_names=[
                'field_1',
                'field_2',
            ],
            fields_by_name={
                'field_1': DataSourceField(widget=widgets.CheckboxSelectMultiple()),
                'field_2': DataSourceField(widget=widgets.CheckboxSelectMultiple()),
            },
            conditions={
                'field_3': [
                    [
                        {
                            'operator': 'and',
                            'field': 'field_1',
                            'condition': 'eq',
                            'field_value': '1',
                            'additional_info': {
                                'content_type_attribute': self.content_type_attribute,
                            }
                        },
                        {
                            'operator': 'and',
                            'field': 'field_2',
                            'condition': 'eq',
                            'field_value': '3',
                            'additional_info': {
                                'content_type_attribute': self.content_type_attribute,
                            }
                        }
                    ]
                ]
            },
            expected={}
        )

    def test_with_not_existing_field_name(self):
        self.assertEvaluation(
            form_data={
                'field_1': ['1', '2'],
                'field_2': ['2'],
                'field_3': ['3']
            },
            field_names=[
                'not_existing_field_name',
                'field_1',
                'field_2',
                'field_3'
            ],
            fields_by_name={
                'field_1': DataSourceField(widget=widgets.CheckboxSelectMultiple()),
                'field_2': DataSourceField(widget=widgets.CheckboxSelectMultiple()),
                'field_3': DataSourceField(widget=widgets.CheckboxSelectMultiple()),
            },
            conditions={
                'field_3': [
                    [
                        {
                            'operator': 'and',
                            'field': 'field_1',
                            'condition': 'eq',
                            'field_value': '1',
                            'additional_info': {
                                'content_type_attribute': self.content_type_attribute,
                            }
                        }
                    ]
                ]
            },
            expected={
                'field_3': True
            }
        )

    def test_should_be_false_because_of_second_and(self):
        self.assertEvaluation(
            form_data={
                'field_1': ['1', '2'],
                'field_2': ['2'],
                'field_3': ['3']
            },
            field_names=[
                'field_1',
                'field_2',
                'field_3'
            ],
            fields_by_name={
                'field_1': DataSourceField(widget=widgets.CheckboxSelectMultiple()),
                'field_2': DataSourceField(widget=widgets.CheckboxSelectMultiple()),
                'field_3': DataSourceField(widget=widgets.CheckboxSelectMultiple()),
            },
            conditions={
                'field_3': [
                    [
                        {
                            'operator': 'and',
                            'field': 'field_1',
                            'condition': 'eq',
                            'field_value': '1',
                            'additional_info': {
                                'content_type_attribute': self.content_type_attribute,
                            }
                        },
                        {
                            'operator': 'and',
                            'field': 'field_2',
                            'condition': 'eq',
                            'field_value': '3',
                            'additional_info': {
                                'content_type_attribute': self.content_type_attribute,
                            }
                        }
                    ]
                ]
            },
            expected={
                'field_3': False
            }
        )

    def test_should_be_true_because_of_second_or(self):
        self.assertEvaluation(
            form_data={
                'field_1': ['1', '2'],
                'field_2': ['2'],
                'field_3': ['3']
            },
            field_names=[
                'field_1',
                'field_2',
                'field_3'
            ],
            fields_by_name={
                'field_1': DataSourceField(widget=widgets.CheckboxSelectMultiple()),
                'field_2': DataSourceField(widget=widgets.CheckboxSelectMultiple()),
                'field_3': DataSourceField(widget=widgets.CheckboxSelectMultiple()),
            },
            conditions={
                'field_3': [
                    [
                        {
                            'operator': 'and',
                            'field': 'field_1',
                            'condition': 'eq',
                            'field_value': '100',
                            'additional_info': {
                                'content_type_attribute': self.content_type_attribute,
                            },
                        },
                        {
                            'operator': 'or',
                            'field': 'field_2',
                            'condition': 'eq',
                            'field_value': '2',
                            'additional_info': {
                                'content_type_attribute': self.content_type_attribute,
                            },
                        }
                    ]
                ]
            },
            expected={
                'field_3': True
            }
        )

    def test_should_be_true_when_used_same_field(self):
        self.assertEvaluation(
            form_data={
                'field_1': ['1', '2'],
                'field_2': ['2'],
                'field_3': ['3']
            },
            field_names=[
                'field_1',
                'field_2',
                'field_3'
            ],
            fields_by_name={
                'field_1': DataSourceField(widget=widgets.CheckboxSelectMultiple()),
                'field_2': DataSourceField(widget=widgets.CheckboxSelectMultiple()),
                'field_3': DataSourceField(widget=widgets.CheckboxSelectMultiple()),
            },
            conditions={
                'field_3': [
                    [
                        {
                            'operator': 'and',
                            'field': 'field_1',
                            'condition': 'eq',
                            'field_value': '1',
                            'additional_info': {
                                'content_type_attribute': self.content_type_attribute,
                            },
                        },
                        {
                            'operator': 'and',
                            'field': 'field_1',
                            'condition': 'eq',
                            'field_value': '2',
                            'additional_info': {
                                'content_type_attribute': self.content_type_attribute,
                            },
                        },
                        {
                            'operator': 'and',
                            'field': 'field_2',
                            'condition': 'eq',
                            'field_value': '2',
                            'additional_info': {
                                'content_type_attribute': self.content_type_attribute,
                            },
                        },
                    ]
                ]
            },
            expected={
                'field_3': True
            }
        )

    def test_field_3_should_be_false_because_field_3_is_false(self):
        self.assertEvaluation(
            form_data={
                'field_1': ['1', '2'],
                'field_2': ['2'],
                'field_3': ['3']
            },
            field_names=[
                'field_1',
                'field_2',
                'field_3'
            ],
            fields_by_name={
                'field_1': DataSourceField(widget=widgets.CheckboxSelectMultiple()),
                'field_2': DataSourceField(widget=widgets.CheckboxSelectMultiple()),
                'field_3': DataSourceField(widget=widgets.CheckboxSelectMultiple()),
            },
            conditions={
                'field_2': [
                    [
                        {
                            'operator': 'and',
                            'field': 'field_1',
                            'condition': 'eq',
                            'field_value': '100',
                            'additional_info': {
                                'content_type_attribute': self.content_type_attribute,
                            }
                        },
                    ]
                ],
                'field_3': [
                    [
                        {
                            'operator': 'and',
                            'field': 'field_2',
                            'condition': 'eq',
                            'field_value': '2',
                            'additional_info': {
                                'content_type_attribute': self.content_type_attribute,
                            }
                        },
                    ]
                ]
            },
            expected={
                'field_2': False,
                'field_3': False,
            }
        )

    def test_field_3_should_be_true_because_field_1_has_value_1_and_2(self):
        self.assertEvaluation(
            form_data={
                'field_1': ['1', '2'],
                'field_2': ['2'],
                'field_3': ['3']
            },
            field_names=[
                'field_1',
                'field_2',
                'field_3'
            ],
            fields_by_name={
                'field_1': DataSourceField(widget=widgets.CheckboxSelectMultiple()),
                'field_2': DataSourceField(widget=widgets.CheckboxSelectMultiple()),
                'field_3': DataSourceField(widget=widgets.CheckboxSelectMultiple()),
            },
            conditions={
                'field_2': [
                    [
                        {
                            'operator': 'and',
                            'field': 'field_1',
                            'condition': 'eq',
                            'field_value': '100',
                            'additional_info': {
                                'content_type_attribute': self.content_type_attribute,
                            },
                        },
                    ]
                ],
                'field_3': [
                    [
                        {
                            'operator': 'and',
                            'field': 'field_2',
                            'condition': 'eq',
                            'field_value': '2',
                            'additional_info': {
                                'content_type_attribute': self.content_type_attribute,
                            },
                        },
                    ],
                    [
                        {
                            'operator': 'and',
                            'field': 'field_1',
                            'condition': 'eq',
                            'field_value': '1',
                            'additional_info': {
                                'content_type_attribute': self.content_type_attribute,
                            },
                        },
                        {
                            'operator': 'and',
                            'field': 'field_1',
                            'condition': 'eq',
                            'field_value': '2',
                            'additional_info': {
                                'content_type_attribute': self.content_type_attribute,
                            },
                        },
                    ]
                ]
            },
            expected={
                'field_2': False,
                'field_3': True,
            }
        )


class MockQuestion(object):
    def __init__(self, page, position, pk, group_id=None):
        self.page, self.position, self.pk = page, position, pk
        self.group_id = group_id

    def __eq__(self, other):
        return (
            self.page == other.page
            and self.position == other.position
            and self.pk == other.pk
            and self.group_id == other.group_id
        )

    def __repr__(self):
        return '(%s, %s, %s, %s)' % (self.page, self.position, self.pk, self.group_id)


class TestSyncQuestions(TestCase):
    sort_key = attrgetter('pk')

    def test_shouldnt_change_data(self):
        questions = [
            MockQuestion(1, 1, 12345),
            MockQuestion(1, 2, 12347),
            MockQuestion(2, 1, 12348),
            MockQuestion(2, 2, 12349),
            MockQuestion(3, 1, 12346),
            MockQuestion(3, 1, 12350, 12346),
            MockQuestion(3, 2, 12351, 12346),
        ]
        expected = []
        result = list(sync_questions(questions))
        self.assertEqual(result, expected)

    def test_shouldnt_change_data_and_should_return_full_list(self):
        questions = [
            MockQuestion(1, 1, 12345),
            MockQuestion(1, 2, 12347),
            MockQuestion(2, 1, 12348),
            MockQuestion(2, 2, 12349),
            MockQuestion(3, 1, 12346),
            MockQuestion(3, 1, 12350, 12346),
            MockQuestion(3, 2, 12351, 12346),
        ]
        expected = questions
        result = list(sorted(sync_questions(questions, only_changed=False), key=self.sort_key))
        expected = list(sorted(expected, key=self.sort_key))
        self.assertEqual(result, expected)

    def test_should_remove_gap_in_positions(self):
        questions = [
            MockQuestion(1, 1, 12345),
            MockQuestion(1, 3, 12347),
            MockQuestion(2, 1, 12348),
            MockQuestion(2, 4, 12349),
            MockQuestion(3, 2, 12346),
            MockQuestion(3, 1, 12350, 12346),
            MockQuestion(3, 1, 12351, 12346),
        ]
        expected = [
            MockQuestion(1, 2, 12347),
            MockQuestion(2, 2, 12349),
            MockQuestion(3, 1, 12346),
            MockQuestion(3, 2, 12351, 12346),
        ]
        result = list(sorted(sync_questions(questions), key=self.sort_key))
        expected = list(sorted(expected, key=self.sort_key))
        self.assertEqual(result, expected)

    def test_should_remove_gap_in_pages(self):
        questions = [
            MockQuestion(1, 1, 12345),
            MockQuestion(1, 2, 12347),
            MockQuestion(1, 3, 12348),
            MockQuestion(3, 1, 12346),
            MockQuestion(3, 2, 12350),
            MockQuestion(4, 1, 12351),
            MockQuestion(4, 2, 12349),
        ]
        expected = [
            MockQuestion(2, 1, 12346),
            MockQuestion(2, 2, 12350),
            MockQuestion(3, 1, 12351),
            MockQuestion(3, 2, 12349),
        ]
        result = list(sorted(sync_questions(questions), key=self.sort_key))
        expected = list(sorted(expected, key=self.sort_key))
        self.assertEqual(result, expected)

    def test_should_remove_gap_in_pages_from_the_beginning(self):
        questions = [
            MockQuestion(2, 1, 12348),
            MockQuestion(2, 2, 12349),
            MockQuestion(2, 3, 12347),
            MockQuestion(3, 1, 12346),
            MockQuestion(3, 1, 12350, 12346),
            MockQuestion(3, 2, 12351, 12346),
            MockQuestion(3, 2, 12345),
        ]
        expected = [
            MockQuestion(1, 1, 12348),
            MockQuestion(1, 2, 12349),
            MockQuestion(1, 3, 12347),
            MockQuestion(2, 1, 12346),
            MockQuestion(2, 1, 12350, 12346),
            MockQuestion(2, 2, 12351, 12346),
            MockQuestion(2, 2, 12345),
        ]
        result = list(sorted(sync_questions(questions), key=self.sort_key))
        expected = list(sorted(expected, key=self.sort_key))
        self.assertEqual(result, expected)

    def test_should_recalc_positions(self):
        questions = [
            MockQuestion(1, 1, 12345),
            MockQuestion(1, 1, 12347),
            MockQuestion(2, 1, 12348),
            MockQuestion(2, 1, 12349),
            MockQuestion(3, 1, 12346),
            MockQuestion(3, 1, 12350, 12346),
            MockQuestion(3, 1, 12351, 12346),
        ]
        expected = [
            MockQuestion(1, 2, 12347),
            MockQuestion(2, 2, 12349),
            MockQuestion(3, 2, 12351, 12346),
        ]
        result = list(sorted(sync_questions(questions), key=self.sort_key))
        expected = list(sorted(expected, key=self.sort_key))
        self.assertEqual(result, expected)

    def test_shouldnt_fail_on_empty_list(self):
        questions = []
        expected = []
        result = list(sync_questions(questions))
        self.assertEqual(result, expected)


class TestOrderQuestions(TestCase):
    Question = namedtuple('Question', ('label', 'pk', 'page', 'position', 'group_id'))

    def create_question(self, **kwargs):
        return self.Question(**kwargs)

    def test_order_questions_one_page_without_grous(self):
        questions = [
            self.create_question(label='1.1', pk=67659, page=1, position=1, group_id=None),
            self.create_question(label='1.5', pk=67665, page=1, position=6, group_id=None),
            self.create_question(label='1.3', pk=67661, page=1, position=3, group_id=None),
            self.create_question(label='1.4', pk=67662, page=1, position=4, group_id=None),
            self.create_question(label='1.2', pk=67660, page=1, position=2, group_id=None),
        ]
        result_pks = [
            q.pk
            for q in order_questions(questions)
        ]
        expected_pks = [
            67659, 67660, 67661, 67662, 67665,
        ]
        self.assertListEqual(result_pks, expected_pks)

    def test_order_questions_one_page_with_groups(self):
        questions = [
            self.create_question(label='1.1', pk=67659, page=1, position=1, group_id=None),
            self.create_question(label='1.5.1', pk=67664, page=1, position=1, group_id=67663),
            self.create_question(label='1.7.1', pk=67667, page=1, position=1, group_id=67666),
            self.create_question(label='1.2', pk=67660, page=1, position=2, group_id=None),
            self.create_question(label='1.6', pk=67665, page=1, position=6, group_id=None),
            self.create_question(label='1.7', pk=67666, page=1, position=7, group_id=None),
            self.create_question(label='1.3', pk=67661, page=1, position=3, group_id=None),
            self.create_question(label='1.4', pk=67662, page=1, position=4, group_id=None),
            self.create_question(label='1.5', pk=67663, page=1, position=5, group_id=None),
        ]
        result_pks = [
            q.pk
            for q in order_questions(questions)
        ]
        expected_pks = [
            67659, 67660, 67661, 67662, 67663,
            67664, 67665, 67666, 67667,
        ]
        self.assertListEqual(result_pks, expected_pks)

    def test_order_questions_two_pages_with_groups(self):
        questions = [
            self.create_question(label='1.1', pk=67659, page=1, position=1, group_id=None),
            self.create_question(label='1.5.1', pk=67664, page=1, position=1, group_id=67663),
            self.create_question(label='2.1', pk=67668, page=2, position=1, group_id=None),
            self.create_question(label='2.3.1', pk=67671, page=2, position=1, group_id=67670),
            self.create_question(label='7.1', pk=67667, page=1, position=1, group_id=67666),
            self.create_question(label='1.2', pk=67660, page=1, position=2, group_id=None),
            self.create_question(label='2.3.2', pk=67672, page=2, position=2, group_id=67670),
            self.create_question(label='2.2', pk=67669, page=2, position=2, group_id=None),
            self.create_question(label='1.3', pk=67661, page=1, position=3, group_id=None),
            self.create_question(label='2.3', pk=67670, page=2, position=3, group_id=None),
            self.create_question(label='1.4', pk=67662, page=1, position=4, group_id=None),
            self.create_question(label='2.4', pk=67673, page=2, position=4, group_id=None),
            self.create_question(label='1.5', pk=67663, page=1, position=5, group_id=None),
            self.create_question(label='1.6', pk=67665, page=1, position=6, group_id=None),
            self.create_question(label='1.7', pk=67666, page=1, position=7, group_id=None),
        ]
        result_pks = [
            q.pk
            for q in order_questions(questions)
        ]
        expected_pks = [
            67659, 67660, 67661, 67662, 67663,
            67664, 67665, 67666, 67667, 67668,
            67669, 67670, 67671, 67672, 67673,
        ]
        self.assertListEqual(result_pks, expected_pks)
