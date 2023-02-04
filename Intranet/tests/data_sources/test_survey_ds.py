# -*- coding: utf-8 -*-
import itertools

from django.test import TestCase
from django.utils.encoding import force_str
from events.data_sources.sources import (
    SurveyQuestionChoiceDataSource,
    SurveyQuestionMatrixChoiceDataSource,
)
from events.surveyme.factories import (
    SurveyQuestionChoiceFactory,
    SurveyQuestionFactory,
    SurveyQuestionMatrixTitleFactory,
)


class TestSurveyQuestionChoiceDataSource(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.choices = {
            'one': SurveyQuestionChoiceFactory(label='one', is_hidden=False),
            'two': SurveyQuestionChoiceFactory(label='two', is_hidden=False),
        }
        [c.save() for c in self.choices.values()]  # чтобы сработали сигналы установки slug

    def test_should_filter_by_question(self):
        self.choices['one'].survey_question = SurveyQuestionFactory()
        self.choices['one'].save()
        response = SurveyQuestionChoiceDataSource().get_filtered_queryset(filter_data={
            'question': str(self.choices['one'].survey_question.id)
        })
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0], self.choices['one'])

    def test_should_filter_by_ids(self):
        response = SurveyQuestionChoiceDataSource().get_filtered_queryset(filter_data={
            'id': [self.choices['one'].id]
        })
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0], self.choices['one'])

    def test_should_filter_by_text(self):
        response = SurveyQuestionChoiceDataSource().get_filtered_queryset(filter_data={
            'text': self.choices['one'].label
        })
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0], self.choices['one'])

    def test_serializer(self):
        response = SurveyQuestionChoiceDataSource.serializer_class(
            SurveyQuestionChoiceDataSource().get_filtered_queryset(), many=True
        ).data
        expected = [
            {
                'id': force_str(choice.id),
                'slug': force_str(choice.slug),
                'text': force_str(choice.label),
                'label_image': None,
            } for choice in self.choices.values()
        ]
        self.assertEqual(
            {frozenset(value.items()) for value in response},
            {frozenset(value.items()) for value in expected},
        )

    def test_serializer_with_hidden_choices(self):
        list(self.choices.values())[0].is_hidden = True
        list(self.choices.values())[0].save()
        response = SurveyQuestionChoiceDataSource.serializer_class(
            SurveyQuestionChoiceDataSource().get_filtered_queryset(), many=True
        ).data
        expected = [
            {
                'id': force_str(choice.id), 'label_image': None,
                'text': force_str(choice.label), 'slug': force_str(choice.slug),
            }
            for choice in self.choices.values() if not choice.is_hidden
        ]
        self.assertEqual(
            {frozenset(value.items()) for value in response},
            {frozenset(value.items()) for value in expected},
        )


class TestSurveyQuestionMatrixChoiceDataSource(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.columns = [
            SurveyQuestionMatrixTitleFactory(
                type='column', label='first_column', position=1,
            ),
            SurveyQuestionMatrixTitleFactory(
                type='column', label='second_column', position=2,
            ),
        ]
        self.rows = [
            SurveyQuestionMatrixTitleFactory(
                type='row', label='first_row', position=1,
            ),
            SurveyQuestionMatrixTitleFactory(
                type='row', label='second_row', position=2,
            ),
        ]

    def test_should_filter_by_question(self):
        question = SurveyQuestionFactory()
        self.columns[0].survey_question = question
        self.columns[0].save()
        self.rows[1].survey_question = question
        self.rows[1].save()
        response = SurveyQuestionMatrixChoiceDataSource().get_filtered_queryset(filter_data={
            'question': str(question.id)
        })
        self.assertEqual(len(response), 2)
        self.assertEqual(set(response), {self.columns[0], self.rows[1]})

    def test_should_filter_by_ids(self):
        response = SurveyQuestionMatrixChoiceDataSource().get_filtered_queryset(filter_data={
            'id': [self.columns[0].id]
        })
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0], self.columns[0])

    def test_should_filter_by_text(self):
        response = SurveyQuestionMatrixChoiceDataSource().get_filtered_queryset(filter_data={
            'text': self.columns[0].label
        })
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0], self.columns[0])

    def test_serializer(self):
        response = SurveyQuestionMatrixChoiceDataSource.serializer_class(
            SurveyQuestionMatrixChoiceDataSource().get_filtered_queryset(), many=True
        ).data
        expected = [
            {
                'id': force_str(choice.id),
                'text': force_str(choice.label),
                'type': force_str(choice.type),
                'position': choice.position,
            } for choice in itertools.chain(self.columns, self.rows)
        ]
        self.assertEqual(
            {frozenset(value.items()) for value in response},
            {frozenset(value.items()) for value in expected},
        )
