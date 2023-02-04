# -*- coding: utf-8 -*-
import responses

from datetime import date, timedelta
from django.core.exceptions import ValidationError
from django.conf import settings
from django.test import TestCase

from events.media.factories import ImageFactory
from events.surveyme.factories import (
    SurveyQuestionFactory,
    SurveyQuestionChoiceFactory,
    SurveyQuestionMatrixTitleFactory,
)
from events.surveyme.models import AnswerType
from events.surveyme.preview import get_preview_class_type


class TestBaseField(TestCase):
    fixtures = ['initial_data.json']
    answer_type = None
    maxDiff = None

    def setUp(self):
        super().setUp()
        answer_type = AnswerType.objects.get(slug=self.answer_type)
        self.question = SurveyQuestionFactory(
            answer_type=answer_type,
            param_is_required=False,
            param_is_hidden=False,
        )
        self.preview_field_class = get_preview_class_type(self.answer_type)

    def register_uri(self):
        responses.add(
            responses.GET,
            'http://api.captcha.yandex.net/generate',
            body='<number url="https://ext.captcha.yandex.net/image?key=123">123</number>',
            content_type='text/xml',
        )

    @responses.activate
    def get_expected_data(self):
        self.register_uri()
        form = self.question.survey.get_form()
        form_data = form.as_dict()
        field_data = form_data['fields'][self.question.param_slug]
        del field_data['error_messages']
        # remove dirty state data
        if 'tags' in field_data and len(field_data['tags']) and field_data['tags'][0].get('attrs'):
            field_data['tags'][0]['attrs'].pop('value', None)
        return field_data

    def remove_fields(self, data, fields):
        if isinstance(data, dict):
            for field in fields:
                data.pop(field, None)
            for item in list(data.values()):
                self.remove_fields(item, fields)
        if isinstance(data, list):
            for item in data:
                self.remove_fields(item, fields)


class TestAnswerShortTextField(TestBaseField):
    answer_type = 'answer_short_text'

    def test_default_data(self):
        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        self.assertDictEqual(expected_data, preview_data)

    def test_customize_data(self):
        self.question.label = 'Короткий текст'
        self.question.help_text = 'Подсказка'
        self.question.param_is_required = True
        self.question.param_is_hidden = True
        self.question.initial = 'testme'
        self.question.save()
        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        expected_data['tags'][0]['attrs']['value'] = 'testme'
        self.assertDictEqual(expected_data, preview_data)

    def test_hints(self):
        self.question.param_hint_data_source = 'university'
        self.question.save()
        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        self.assertDictEqual(expected_data, preview_data)


class TestAnswerLongTextField(TestBaseField):
    answer_type = 'answer_long_text'

    def test_default_data(self):
        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        self.assertDictEqual(expected_data, preview_data)

    def test_customize_data(self):
        self.question.label = 'Длинный текст'
        self.question.help_text = 'Подсказка'
        self.question.param_is_required = True
        self.question.param_is_hidden = True
        self.question.param_min = 10
        self.question.param_max = 50
        self.question.save()
        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        self.assertDictEqual(expected_data, preview_data)


class TestAnswerNumberField(TestBaseField):
    answer_type = 'answer_number'

    def test_default_data(self):
        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        self.assertDictEqual(expected_data, preview_data)

    def test_customize_data(self):
        self.question.label = 'Число'
        self.question.help_text = 'Подсказка'
        self.question.param_is_required = True
        self.question.param_is_hidden = True
        self.question.param_min = 10
        self.question.param_max = 50
        self.question.initial = 123
        self.question.save()
        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        expected_data['tags'][0]['attrs']['value'] = '123'
        self.assertDictEqual(expected_data, preview_data)


class TestAnswerUrlField(TestBaseField):
    answer_type = 'answer_url'

    def test_default_data(self):
        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        self.assertDictEqual(expected_data, preview_data)

    def test_customize_data(self):
        self.question.label = 'Ссылка'
        self.question.help_text = 'Подсказка'
        self.question.param_is_required = True
        self.question.param_is_hidden = True
        self.question.save()
        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        self.assertDictEqual(expected_data, preview_data)


class TestAnswerEmailField(TestBaseField):
    answer_type = 'answer_non_profile_email'

    def test_default_data(self):
        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        self.assertDictEqual(expected_data, preview_data)

    def test_customize_data(self):
        self.question.label = 'Почта'
        self.question.help_text = 'Подсказка'
        self.question.param_is_required = True
        self.question.param_is_hidden = True
        self.question.save()
        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        self.assertDictEqual(expected_data, preview_data)


class TestAnswerNameField(TestBaseField):
    answer_type = 'answer_name'

    def test_default_data(self):
        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        self.assertDictEqual(expected_data, preview_data)

    @responses.activate
    def test_customize_data(self):
        self.register_uri()
        self.question.label = 'Имя'
        self.question.help_text = 'Подсказка'
        self.question.param_is_required = True
        self.question.param_is_hidden = True
        self.question.save()
        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        self.assertDictEqual(expected_data, preview_data)


class TestAnswerSurnameField(TestBaseField):
    answer_type = 'answer_surname'

    def test_default_data(self):
        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        self.assertDictEqual(expected_data, preview_data)

    def test_customize_data(self):
        self.question.label = 'Фамилия'
        self.question.help_text = 'Подсказка'
        self.question.param_is_required = True
        self.question.param_is_hidden = True
        self.question.save()
        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        self.assertDictEqual(expected_data, preview_data)


class TestAnswerPhoneField(TestBaseField):
    answer_type = 'answer_phone'

    def test_default_data(self):
        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        self.assertDictEqual(expected_data, preview_data)

    def test_customize_data(self):
        self.question.label = 'Телефон'
        self.question.help_text = 'Подсказка'
        self.question.param_is_required = True
        self.question.param_is_hidden = True
        self.question.save()
        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        self.assertDictEqual(expected_data, preview_data)


class TestAnswerStatementField(TestBaseField):
    answer_type = 'answer_statement'

    def test_default_data(self):
        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        self.assertDictEqual(expected_data, preview_data)

    def test_customize_data(self):
        self.question.label = 'Комментарий'
        self.question.help_text = 'Подсказка'
        self.question.param_is_required = True
        self.question.param_is_hidden = True
        self.question.save()
        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        self.assertDictEqual(expected_data, preview_data)


class TestAnswerBooleanField(TestBaseField):
    answer_type = 'answer_boolean'

    def test_default_data(self):
        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        self.assertDictEqual(expected_data, preview_data)

    def test_customize_data(self):
        self.question.label = 'Да/Нет'
        self.question.help_text = 'Подсказка'
        self.question.param_is_required = True
        self.question.param_is_hidden = True
        self.question.save()
        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        preview_data['tags'][0].pop('label')
        preview_data['tags'][0]['attrs'].pop('type')
        expected_data['tags'][0]['attrs'].pop('type')
        self.assertDictEqual(expected_data, preview_data)


class TestAnswerFilesField(TestBaseField):
    answer_type = 'answer_files'

    def test_default_data(self):
        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        self.assertDictEqual(expected_data, preview_data)

    def test_customize_data(self):
        self.question.label = 'Файлы'
        self.question.help_text = 'Подсказка'
        self.question.param_is_required = True
        self.question.param_is_hidden = True
        self.question.save()
        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        self.assertDictEqual(expected_data, preview_data)


class TestAnswerDateField(TestBaseField):
    answer_type = 'answer_date'

    def test_default_data(self):
        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        self.assertDictEqual(expected_data, preview_data)

    def test_customize_data(self):
        self.question.label = 'Дата'
        self.question.help_text = 'Подсказка'
        self.question.param_is_required = True
        self.question.param_is_hidden = True
        self.question.param_date_field_min = (date.today() - timedelta(days=7)).isoformat()
        self.question.param_date_field_max = (date.today() + timedelta(days=7)).isoformat()
        self.question.save()
        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        self.assertDictEqual(expected_data, preview_data)


class TestAnswerDateRangeField(TestBaseField):
    answer_type = 'answer_date'

    def setUp(self):
        super().setUp()
        self.question.param_date_field_type = 'daterange'
        self.question.save()

    def test_default_data(self):
        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        self.assertDictEqual(expected_data, preview_data)

    def test_customize_data(self):
        self.question.label = 'Диапазон дат'
        self.question.help_text = 'Подсказка'
        self.question.param_is_required = True
        self.question.param_is_hidden = True
        self.question.param_date_field_min = (date.today() - timedelta(days=7)).isoformat()
        self.question.param_date_field_max = (date.today() + timedelta(days=7)).isoformat()
        self.question.save()
        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        self.assertDictEqual(expected_data, preview_data)


class TestAnswerChoicesListField(TestBaseField):
    answer_type = 'answer_choices'

    def setUp(self):
        super().setUp()
        self.question.param_data_source = 'survey_question_choice'
        self.question.save()
        SurveyQuestionChoiceFactory(survey_question=self.question, label='1')
        SurveyQuestionChoiceFactory(survey_question=self.question, label='2')
        SurveyQuestionChoiceFactory(survey_question=self.question, label='3')
        SurveyQuestionChoiceFactory(survey_question=self.question, label='4')
        SurveyQuestionChoiceFactory(survey_question=self.question, label='5')

    def test_default_data(self):
        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        self.assertDictEqual(expected_data, preview_data)

    def test_new_answer_data(self):
        self.question.surveyquestionchoice_set.all().delete()
        new_data = {
            "label": "Choices",
            "param_help_text": "",
            "param_is_required": False,
            "choices": [
                {"label": "Choice 1", "position": 1},
                {"label": "Choice 2", "position": 2},
            ],
            "param_modify_choices": "natural"
        }
        preview_field = self.preview_field_class(self.question, **new_data)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        self.assertNotEqual(expected_data, preview_data)
        expected_items = [
            {"text": "Choice 1", "position": 1, "label_image": None},
            {"text": "Choice 2", "position": 2, "label_image": None},
        ]
        self.assertEqual(preview_data['data_source']['items'], expected_items)
        self.assertEqual(preview_data['label'], new_data['label'])

        SurveyQuestionChoiceFactory(survey_question=self.question, label='new choice 1')
        SurveyQuestionChoiceFactory(survey_question=self.question, label='new choice 2')

        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        self.assertDictEqual(expected_data, preview_data)

    def test_customize_data(self):
        self.question.label = 'Список'
        self.question.help_text = 'Подсказка'
        self.question.param_is_required = True
        self.question.param_is_hidden = True
        self.question.param_suggest_choices = True
        self.question.param_is_allow_multiple_choice = True
        self.question.param_is_disabled_init_item = True
        self.question.save()
        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        self.assertDictEqual(expected_data, preview_data)


class TestAnswerChoicesDataSourceField(TestBaseField):
    answer_type = 'answer_choices'

    def setUp(self):
        super().setUp()
        self.question.param_data_source = 'city'
        self.question.param_data_source_params = {
            'filters': [{
                'filter': {
                    'data_source': 'country',
                    'name': 'country',
                },
                'type': 'specified_value',
                'value': '23',
            }]
        }
        self.question.save()

    def test_default_data(self):
        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        self.assertDictEqual(expected_data, preview_data)

    def test_customize_data(self):
        self.question.label = 'Список'
        self.question.help_text = 'Подсказка'
        self.question.param_is_required = True
        self.question.param_is_hidden = True
        self.question.param_suggest_choices = True
        self.question.param_is_allow_multiple_choice = True
        self.question.param_is_disabled_init_item = True
        self.question.save()
        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        self.assertDictEqual(expected_data, preview_data)


class TestAnswerMatrixChoicesField(TestBaseField):
    answer_type = 'answer_choices'

    def setUp(self):
        super().setUp()
        self.question.param_data_source = 'survey_question_matrix_choice'
        self.question.save()
        SurveyQuestionMatrixTitleFactory(survey_question=self.question, type='row', label='Выбери значение')
        SurveyQuestionMatrixTitleFactory(survey_question=self.question, type='column', label='1')
        SurveyQuestionMatrixTitleFactory(survey_question=self.question, type='column', label='2')
        SurveyQuestionMatrixTitleFactory(survey_question=self.question, type='column', label='3')
        SurveyQuestionMatrixTitleFactory(survey_question=self.question, type='column', label='4')
        SurveyQuestionMatrixTitleFactory(survey_question=self.question, type='column', label='5')

    def test_new_answer_data(self):
        self.question.surveyquestionchoice_set.all().delete()
        new_data = {
            "matrix_titles": [
                {"label": "Choice 1", "position": 1, 'type': 'column'},
                {"label": "Choice 2", "position": 2, 'type': 'column'},
                {"label": "Make a choice", "position": 1, 'type': 'row'},
            ],
        }
        preview_field = self.preview_field_class(self.question, **new_data)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        self.assertNotEqual(expected_data, preview_data)
        expected_items = [
            {"text": "Choice 1", "position": 1, 'type': 'column', 'label_image': None},
            {"text": "Choice 2", "position": 2, 'type': 'column', 'label_image': None},
            {"text": "Make a choice", "position": 1, 'type': 'row', 'label_image': None},
        ]
        self.assertEqual(preview_data['data_source']['items'], expected_items)

        SurveyQuestionMatrixTitleFactory(survey_question=self.question, type='row', label='Выбери значение')
        SurveyQuestionMatrixTitleFactory(survey_question=self.question, type='column', label='1')
        SurveyQuestionMatrixTitleFactory(survey_question=self.question, type='column', label='2')

        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        self.assertDictEqual(expected_data, preview_data)

    def test_default_data(self):
        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        self.assertDictEqual(expected_data, preview_data)
        self.assertEqual(len(expected_data['data_source']['items']), 6)

    def test_customize_data(self):
        self.question.label = 'Матрица ответов'
        self.question.help_text = 'Подсказка'
        self.question.param_is_required = True
        self.question.param_is_hidden = True
        self.question.save()
        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        self.assertDictEqual(expected_data, preview_data)
        self.assertEqual(len(expected_data['data_source']['items']), 6)


class TestLabelImage(TestBaseField):
    answer_type = 'answer_short_text'

    def setUp(self):
        super().setUp()
        self.label_image = ImageFactory()
        self.sizes = set(
            '%sx%s' % (w or '', h or '')
            for (w, h) in settings.IMAGE_SIZES
        )
        self.question.label_image = self.label_image
        self.question.save()

    def test_default_data(self):
        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()

        expected_data = self.get_expected_data()

        self.assertDictEqual(expected_data, preview_data)
        label_image = preview_data['label_image']
        self.assertIsNotNone(label_image)
        self.assertSetEqual(self.sizes, set(label_image['links'].keys()))
        for url in list(label_image['links'].values()):
            self.assertIn(str(self.label_image.image), url)

    def test_set_another_image(self):
        another_label_image = ImageFactory()

        new_data = {'label_image': another_label_image.pk}
        preview_field = self.preview_field_class(self.question, **new_data)
        preview_data = preview_field.as_dict()

        self.question.label_image = another_label_image
        self.question.save()
        expected_data = self.get_expected_data()

        self.assertDictEqual(expected_data, preview_data)
        label_image = preview_data['label_image']
        self.assertIsNotNone(label_image)
        self.assertSetEqual(self.sizes, set(label_image['links'].keys()))
        for url in list(label_image['links'].values()):
            self.assertIn(str(another_label_image.image), url)

    def test_drop_image(self):
        new_data = {'label_image': None}
        preview_field = self.preview_field_class(self.question, **new_data)
        preview_data = preview_field.as_dict()

        self.question.label_image = None
        self.question.save()
        expected_data = self.get_expected_data()

        self.assertDictEqual(expected_data, preview_data)
        label_image = preview_data['label_image']
        self.assertIsNone(label_image)

    def test_validation_error(self):
        new_data = {'label_image': 'not a number'}
        preview_field = self.preview_field_class(self.question, **new_data)
        with self.assertRaises(ValidationError) as exc:
            preview_field.as_dict()
            self.assertIn('label_image', exc.message_dict)


class TestChoicesLabelImage(TestBaseField):
    answer_type = 'answer_choices'

    def setUp(self):
        super().setUp()
        self.label_image = ImageFactory()
        self.another_label_image = ImageFactory()
        self.sizes = set(
            '%sx%s' % (w or '', h or '')
            for (w, h) in settings.IMAGE_SIZES
        )
        self.question.param_data_source = 'survey_question_choice'
        self.question.save()

        self.choices = [
            SurveyQuestionChoiceFactory(
                survey_question=self.question,
                label='one',
                label_image=self.label_image,
            ),
            SurveyQuestionChoiceFactory(
                survey_question=self.question,
                label='two',
                label_image=self.another_label_image,
            ),
            SurveyQuestionChoiceFactory(
                survey_question=self.question,
                label='three',
            ),
        ]

    def test_default_data(self):
        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()

        expected_data = self.get_expected_data()

        self.assertDictEqual(expected_data, preview_data)
        data_source = preview_data['data_source']
        self.assertIsNotNone(data_source)
        items = data_source['items']
        self.assertIsNotNone(items)
        labels = [item['text'] for item in items]
        self.assertListEqual(['one', 'two', 'three'], labels)

        label_image = items[0]['label_image']
        self.assertIsNotNone(label_image)
        self.assertSetEqual(self.sizes, set(label_image['links'].keys()))
        for url in list(label_image['links'].values()):
            self.assertIn(str(self.label_image.image), url)

        label_image = items[1]['label_image']
        self.assertIsNotNone(label_image)
        self.assertSetEqual(self.sizes, set(label_image['links'].keys()))
        for url in list(label_image['links'].values()):
            self.assertIn(str(self.another_label_image.image), url)

        label_image = items[2]['label_image']
        self.assertIsNone(label_image)

    def test_set_another_choices(self):
        new_data = {
            'choices': [
                {'label': '1', 'label_image': self.another_label_image.pk},
                {'label': '2', 'label_image': self.label_image.pk},
                {'label': '3', 'label_image': None},
            ]
        }
        preview_field = self.preview_field_class(self.question, **new_data)
        preview_data = preview_field.as_dict()

        self.question.surveyquestionchoice_set.all().delete()
        SurveyQuestionChoiceFactory(
            survey_question=self.question,
            label='1',
            label_image=self.another_label_image,
        )
        SurveyQuestionChoiceFactory(
            survey_question=self.question,
            label='2',
            label_image=self.label_image,
        )
        SurveyQuestionChoiceFactory(
            survey_question=self.question,
            label='3',
        )
        expected_data = self.get_expected_data()
        self.remove_fields(expected_data['data_source'], ['id', 'slug', 'name', 'is_system'])

        self.assertDictEqual(expected_data, preview_data)
        data_source = preview_data['data_source']
        self.assertIsNotNone(data_source)
        items = data_source['items']
        self.assertIsNotNone(items)
        labels = [item['text'] for item in items]
        self.assertListEqual(['1', '2', '3'], labels)

        label_image = items[0]['label_image']
        self.assertIsNotNone(label_image)
        self.assertSetEqual(self.sizes, set(label_image['links'].keys()))
        for url in list(label_image['links'].values()):
            self.assertIn(str(self.another_label_image.image), url)

        label_image = items[1]['label_image']
        self.assertIsNotNone(label_image)
        self.assertSetEqual(self.sizes, set(label_image['links'].keys()))
        for url in list(label_image['links'].values()):
            self.assertIn(str(self.label_image.image), url)

        label_image = items[2]['label_image']
        self.assertIsNone(label_image)

    def test_drop_choices(self):
        new_data = {
            'choices': None,
        }
        preview_field = self.preview_field_class(self.question, **new_data)
        preview_data = preview_field.as_dict()

        self.question.surveyquestionchoice_set.all().delete()
        expected_data = self.get_expected_data()

        self.assertDictEqual(expected_data, preview_data)
        data_source = preview_data['data_source']
        self.assertIsNotNone(data_source)
        items = data_source['items']
        self.assertIsNotNone(items)
        self.assertListEqual(items, [])

    def test_validation_error(self):
        new_data = {
            'choices': [
                {'label': '1', 'label_image': 'not a number', },
            ]
        }
        preview_field = self.preview_field_class(self.question, **new_data)
        with self.assertRaises(ValidationError) as exc:
            preview_field.as_dict()
            self.assertIn('label_image', exc.message_dict)


class TestAnswerPaymentField(TestBaseField):
    answer_type = 'answer_payment'

    def test_default_data(self):
        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        self.assertDictEqual(expected_data, preview_data)

    def get_expected_data(self):
        expected = super().get_expected_data()
        param_payment = self.question.param_payment or {}
        expected['other_data'].update({
            'is_fixed': bool(param_payment.get('is_fixed')),
            'min': 2,
            'max': 15000,
        })
        del expected['tags'][0]['attrs']['type']
        return expected

    def test_customize_data(self):
        self.question.label = 'Оплата'
        self.question.help_text = 'Подсказка'
        self.question.param_is_required = True
        self.question.param_is_hidden = True
        self.question.param_payment = {
            'account_id': '1234567890',
            'is_fixed': True,
        }
        self.question.initial = '123'
        self.question.save()
        preview_field = self.preview_field_class(self.question)
        preview_data = preview_field.as_dict()
        expected_data = self.get_expected_data()
        expected_data['tags'][0]['attrs']['value'] = '123'
        self.assertDictEqual(expected_data, preview_data)
