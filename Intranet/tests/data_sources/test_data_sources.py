# -*- coding: utf-8 -*-
import json
from unittest.mock import Mock

from django.test import TestCase
from django.core.exceptions import ValidationError
from django.utils.translation import ugettext_lazy as _

from events.data_sources.forms import AnswerMatrixDataSourceField
from events.data_sources.models import DataSourceItem, University
from events.surveyme.factories import SurveyQuestionMatrixTitleFactory, SurveyQuestionFactory


class TestAnswerMatrixDataSourceField(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.matrix_titles = [
            SurveyQuestionMatrixTitleFactory(type='row', label='first row'),
            SurveyQuestionMatrixTitleFactory(type='column', label='first column'),
        ]
        self.field = AnswerMatrixDataSourceField(
            data_source='survey_question_matrix_choice',
            allow_multiple_choice=False,
        )

    def test_to_python_with_multiple_choice(self):
        self.field.allow_multiple_choice = True
        response = self.field.to_python(['1_2', '2_3', '3'])
        expected = {
            '1_2': None,
            '2_3': None,
            '3': None,
        }
        self.assertEqual(response, expected)

    def test_to_python_should_return_first_item_if_multiple_choice_is_not_allowed(self):
        self.field.allow_multiple_choice = False
        response = self.field.to_python(['1_2', '2_3', '3'])
        expected = {'3': None, '2_3': None, '1_2': None}
        self.assertEqual(response, expected)

    def test_to_python_should_return_empty_dict_if_value_is_none(self):
        self.assertEqual(self.field.to_python(None), {})

    def test_to_python_should_return_empty_dict_if_value_is_list_with_nones(self):
        self.assertEqual(self.field.to_python([None]), {})
        self.assertEqual(self.field.to_python([None, '']), {})

    def test_clean_should_create_and_return_data_source_items(self):
        row_to_column = '%s_%s' % (self.matrix_titles[0].pk, self.matrix_titles[1].pk)
        response = self.field.clean([row_to_column])
        self.assertEqual(len(response), 1)
        self.assertEqual(type(response[0]), DataSourceItem)
        self.assertEqual(response[0].identity, row_to_column)
        self.assertEqual(response[0].get_text(), '"%s": %s' % (self.matrix_titles[0].label, self.matrix_titles[1].label))

    def test_clean_should_not_create_data_source_items_if_they_already_exists(self):
        row_to_column = '%s_%s' % (self.matrix_titles[0].pk, self.matrix_titles[1].pk)
        existing_item = DataSourceItem.objects.get_and_update(
            data_source='survey_question_matrix_choice',
            identity=row_to_column,
            text='change me',
        )
        response = self.field.clean([row_to_column])
        self.assertEqual(response[0], existing_item)
        self.assertEqual(DataSourceItem.objects.count(), 1)

    def test_clean_should_always_update_data_source_item_even_it_already_exists(self):
        row_to_column = '%s_%s' % (self.matrix_titles[0].pk, self.matrix_titles[1].pk)
        DataSourceItem.objects.get_and_update(
            data_source='survey_question_matrix_choice',
            identity=row_to_column,
            text='change me',
        )
        response = self.field.clean([row_to_column])
        self.assertEqual(response[0].get_text(), '"%s": %s' % (self.matrix_titles[0].label, self.matrix_titles[1].label))

    def test_clean_should_raise_validation_error_if_value_not_in_data_source(self):
        try:
            self.field.clean(['400_400'])
        except ValidationError as e:
            expected_message = 'Введите правильное значение.'
            self.assertEqual(e.messages[0], expected_message)
        else:
            self.fail('There is no row_to_column == 400_400 matrix titles with. Validation error should be raised')

    def test_should_apply_filters_while_validating(self):
        self.field.allow_multiple_choice = True
        question = SurveyQuestionFactory()
        self.matrix_titles[0].survey_question = question
        self.matrix_titles[1].survey_question = question
        self.matrix_titles[0].save()
        self.matrix_titles[1].save()
        second_row = SurveyQuestionMatrixTitleFactory(type='row', label='second row')
        row_to_column = '%s_%s' % (self.matrix_titles[0].pk, self.matrix_titles[1].pk)

        self.field.filters = {
            'question': question.pk
        }

        # with valid data
        response = list(self.field.clean([row_to_column]))
        expected = [
            DataSourceItem.objects.get_and_update(
                data_source='survey_question_matrix_choice',
                identity=row_to_column,
                text=self.matrix_titles[1].label,
            ),
        ]
        self.assertListEqual(response, expected)

        # with invalid data
        second_row_to_column = '%s_%s' % (second_row.pk, self.matrix_titles[1].pk)
        try:
            self.field.clean([
                row_to_column,
                second_row_to_column,  # has different question
            ])
        except ValidationError:
            pass
        else:
            self.fail('Should raise validation error because "second_row_to_column" event has different question')

    def test_should_raise_validation_error_if_fields_is_required_and_no_value_provided(self):
        try:
            self.field.clean([])
        except ValidationError as e:
            expected_message = _('This field is required.')
            self.assertEqual(e.messages[0], expected_message)
        else:
            self.fail('Should raise validation error if no value provided and field is required')

    def test_should_raise_validation_error_if_some_value_is_not_valid_row_to_id(self):
        try:
            self.field.clean(['1'])
        except ValidationError as e:
            expected_message = 'Введите правильное значение.'
            self.assertEqual(e.messages[0], expected_message)
        else:
            self.fail('Should raise validation error if some value is not valid row to id')

    def test_should_not_iterate_over_queryset_if_field_is_not_required_and_no_value_provided(self):
        self.field.required = False
        self.field.get_filtered_data_source_data = Mock(return_value=[])
        self.assertEqual(self.field.clean([]), [])
        self.assertFalse(self.field.get_filtered_data_source_data.called)


class TestDataSourceView(TestCase):
    def test_success(self):
        University.objects.create(name='Московский государственный университет')
        response = self.client.get('/v1/data-source/university/?http_accept_language=ru&suggest=универ')
        data = json.loads(response.content.decode(response.charset))
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(data['results']), 1)
        self.assertIn('Московский государственный университет', data['results'][0]['text'])

    def test_validationerror_on_unicode_error(self):
        response = self.client.get('/v1/data-source/university/?http_accept_language=ru&suggest=%D2%9A%D0%B0')
        self.assertEqual(response.status_code, 400)

    def test_incorrect_input_should_return_empty_list(self):
        response = self.client.get(u'/v1/data-source/address/?everybodybecoolthisis=crasher')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['count'], 0)
