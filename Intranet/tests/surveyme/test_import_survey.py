# -*- coding: utf-8 -*-
import datetime

from collections import defaultdict
from django.test import TestCase, override_settings

from events.accounts.factories import UserFactory
from events.accounts.models import Organization
from events.conditions.factories import ContentTypeAttributeFactory
from events.surveyme.factories import SurveyStyleTemplateFactory
from events.surveyme.helpers import JsonFixtureMixin
from events.surveyme.models import (
    SurveyQuestion,
    SurveyQuestionChoice,
    SurveyQuestionMatrixTitle,
    SurveyQuestionShowConditionNode,
    SurveyQuestionShowConditionNodeItem,
    SurveySubmitConditionNode,
    SurveySubmitConditionNodeItem,
)
from events.surveyme.survey_importer import SurveyImporter
from events.surveyme_integration.models import (
    IntegrationFileTemplate,
    JSONRPCSubscriptionData,
    JSONRPCSubscriptionParam,
    ServiceSurveyHookSubscription,
    StartrekSubscriptionData,
    SubscriptionAttachment,
    SubscriptionHeader,
    SurveyHook,
    SurveyHookCondition,
    SurveyHookConditionNode,
    SurveyVariable,
    WikiSubscriptionData,
)


class TestSurveyImporter(TestCase, JsonFixtureMixin):
    fixtures = ['initial_data.json']

    def setUp(self):
        ContentTypeAttributeFactory(pk=2, lookup_field='answer_choices')
        ContentTypeAttributeFactory(pk=12, lookup_field='answer_long_text')
        self.user = UserFactory()
        self.org, _ = Organization.objects.get_or_create(dir_id=123)

    def test_creating_survey(self):
        json_string = self.get_json_from_file('test_creating_survey.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id='123')

        self.assertEqual(survey.user.pk, self.user.pk)
        self.assertEqual(survey.org, self.org)
        self.assertEqual(survey.name, 'Пустая форма')
        self.assertFalse(survey.need_auth_to_answer)
        self.assertFalse(survey.is_only_for_iframe)
        self.assertFalse(survey.is_allow_answer_editing)
        self.assertTrue(survey.is_allow_multiple_answers)
        self.assertFalse(survey.is_allow_answer_versioning)
        self.assertEqual(survey.metrika_counter_code, '')
        self.assertEqual(survey.captcha_display_mode, 'auto')
        self.assertIsNone(survey.maximum_answers_count)
        self.assertFalse(survey.auto_control_publication_status)
        self.assertIsNone(survey.validator_url)
        self.assertIsNone(survey.styles_template)
        self.assertEqual(survey.type, 'form')
        self.assertIsNone(survey.date_published)
        self.assertIsNone(survey.date_unpublished)
        self.assertFalse(survey.is_published_external)
        self.assertFalse(survey.is_public)
        self.assertIsNone(survey.slug)

    def test_creating_texts(self):
        json_string = self.get_json_from_file('test_creating_texts.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id='123')

        texts = {
            text['slug']: text
            for text in survey.texts.values('slug', 'value', 'max_length', 'null')
        }
        self.assertEqual(len(texts), 8)
        self.assertEqual(
            texts['invitation_to_change'], {
                'max_length': 30,
                'null': False,
                'slug': 'invitation_to_change',
                'value': 'Изменить ответ на опрос'
            }
        )
        self.assertEqual(
            texts['invitation_to_submit'], {
                'max_length': 30,
                'null': False,
                'slug': 'invitation_to_submit',
                'value': 'Ответить на опрос'
            }
        )
        self.assertEqual(
            texts['save_changes_button'], {
                'max_length': 30,
                'null': False,
                'slug': 'save_changes_button',
                'value': 'Сохранить изменения'
            }
        )
        self.assertEqual(
            texts['submit_button'], {
                'max_length': 30,
                'null': False,
                'slug': 'submit_button',
                'value': 'Отправить'
            }
        )
        self.assertEqual(
            texts['successful_change'], {
                'max_length': 750,
                'null': True,
                'slug': 'successful_change',
                'value': ''
            }
        )
        self.assertEqual(
            texts['successful_change_title'], {
                'max_length': 90,
                'null': False,
                'slug': 'successful_change_title',
                'value': 'Ваш ответ на опрос был изменен'
            }
        )
        self.assertEqual(
            texts['successful_submission'], {
                'max_length': 750,
                'null': True,
                'slug': 'successful_submission',
                'value': ''
            }
        )
        self.assertEqual(
            texts['successful_submission_title'], {
                'max_length': 90,
                'null': False,
                'slug': 'successful_submission_title',
                'value': 'Спасибо за ответ на опрос'
            }
        )

    def test_creating_questions(self):
        json_string = self.get_json_from_file('test_creating_questions.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id='123')
        params = (
            'answer_type_id',
            'validator_type_id',
            'position',
            'page',
            'initial',
            'param_is_required',
            'param_is_allow_multiple_choice',
            'param_is_allow_other',
            'param_max',
            'param_min',
            'param_is_section_header',
            'param_max_file_size',
            'param_price',
            'param_variables',
            'param_widget',
            'param_is_hidden',
            'param_slug',
            'param_hint_type_id',
            'param_data_source',
            'param_data_source_params',
            'param_hint_data_source',
            'param_hint_data_source_params',
            'param_is_random_choices_position',
            'param_modify_choices',
            'param_max_files_count',
            'param_is_disabled_init_item',
            'param_date_field_type',
            'param_date_field_min',
            'param_date_field_max',
            'param_suggest_choices',
            'param_help_text',
            'param_quiz',
            'label',
        )
        questions = {
            question['param_slug']: question
            for question in survey.surveyquestion_set.values(*params)
        }
        self.assertEqual(survey.questions_count, 17)
        self.assertEqual(
            questions['answer_boolean_64359'], {
                'answer_type_id': 33,
                'initial': None,
                'page': 1,
                'param_data_source': 'survey_question_choice',
                'param_data_source_params': None,
                'param_date_field_max': None,
                'param_date_field_min': None,
                'param_date_field_type': 'date',
                'param_help_text': '',
                'param_hint_data_source': None,
                'param_hint_data_source_params': None,
                'param_hint_type_id': None,
                'param_is_allow_multiple_choice': False,
                'param_is_allow_other': False,
                'param_is_disabled_init_item': True,
                'param_is_hidden': False,
                'param_is_random_choices_position': False,
                'param_is_required': False,
                'param_is_section_header': False,
                'param_max': 1,
                'param_max_file_size': 20,
                'param_max_files_count': 20,
                'param_min': 0,
                'param_modify_choices': 'natural',
                'param_price': None,
                'param_slug': 'answer_boolean_64359',
                'param_suggest_choices': False,
                'param_variables': None,
                'param_widget': 'list',
                'param_quiz': None,
                'position': 7,
                'validator_type_id': None,
                'label': 'Да/Нет',
            }
        )

        question_data = questions['answer_boolean_64369']
        filters = question_data.pop('param_data_source_params')
        expected_filters = {
            "filter": {"name": "question"},
            "type": "specified_value",
            "value": survey.surveyquestion_set.get(param_slug='answer_boolean_64369').pk
        }
        json_filters = filters['filters']
        self.assertEqual(len(json_filters), 1)
        self.assertEqual(
            json_filters[0],
            expected_filters,
        )
        self.assertEqual(
            questions['answer_boolean_64369'], {
                'answer_type_id': 33,
                'initial': None,
                'page': 2,
                'param_data_source': 'survey_question_choice',
                'param_date_field_max': None,
                'param_date_field_min': None,
                'param_date_field_type': 'date',
                'param_help_text': '',
                'param_hint_data_source': None,
                'param_hint_data_source_params': None,
                'param_hint_type_id': None,
                'param_is_allow_multiple_choice': False,
                'param_is_allow_other': False,
                'param_is_disabled_init_item': True,
                'param_is_hidden': False,
                'param_is_random_choices_position': False,
                'param_is_required': False,
                'param_is_section_header': False,
                'param_max': 1,
                'param_max_file_size': 20,
                'param_max_files_count': 20,
                'param_min': 0,
                'param_modify_choices': 'natural',
                'param_price': None,
                'param_slug': 'answer_boolean_64369',
                'param_suggest_choices': False,
                'param_variables': None,
                'param_widget': 'list',
                'param_quiz': None,
                'position': 2,
                'validator_type_id': None,
                'label': 'Да/Нет из группы',
            }
        )
        self.assertEqual(
            questions['answer_date_64365'], {
                'answer_type_id': 39,
                'initial': None,
                'page': 1,
                'param_data_source': 'survey_question_choice',
                'param_data_source_params': None,
                'param_date_field_max': None,
                'param_date_field_min': None,
                'param_date_field_type': 'date',
                'param_help_text': '',
                'param_hint_data_source': None,
                'param_hint_data_source_params': None,
                'param_hint_type_id': None,
                'param_is_allow_multiple_choice': False,
                'param_is_allow_other': False,
                'param_is_disabled_init_item': True,
                'param_is_hidden': False,
                'param_is_random_choices_position': False,
                'param_is_required': False,
                'param_is_section_header': False,
                'param_max': 1,
                'param_max_file_size': 20,
                'param_max_files_count': 20,
                'param_min': 0,
                'param_modify_choices': 'natural',
                'param_price': None,
                'param_slug': 'answer_date_64365',
                'param_suggest_choices': False,
                'param_variables': None,
                'param_widget': 'list',
                'param_quiz': None,
                'position': 13,
                'validator_type_id': None,
                'label': 'Дата',
            }
        )

        question_data = questions['answer_date_64366']
        filters = question_data.pop('param_data_source_params')
        expected_filters = {
            "filter": {"name": "question"},
            "type": "specified_value",
            "value": survey.surveyquestion_set.get(param_slug='answer_date_64366').pk
        }
        json_filters = filters['filters']
        self.assertEqual(len(json_filters), 1)
        self.assertEqual(
            json_filters[0],
            expected_filters,
        )
        self.assertEqual(
            questions['answer_date_64366'], {
                'answer_type_id': 39,
                'initial': None,
                'page': 1,
                'param_data_source': 'survey_question_choice',
                'param_date_field_max': datetime.date(2019, 2, 28),
                'param_date_field_min': datetime.date(2019, 2, 1),
                'param_date_field_type': 'daterange',
                'param_help_text': '',
                'param_hint_data_source': None,
                'param_hint_data_source_params': None,
                'param_hint_type_id': None,
                'param_is_allow_multiple_choice': False,
                'param_is_allow_other': False,
                'param_is_disabled_init_item': True,
                'param_is_hidden': False,
                'param_is_random_choices_position': False,
                'param_is_required': False,
                'param_is_section_header': False,
                'param_max': 1,
                'param_max_file_size': 20,
                'param_max_files_count': 20,
                'param_min': 0,
                'param_modify_choices': 'natural',
                'param_price': None,
                'param_slug': 'answer_date_64366',
                'param_suggest_choices': False,
                'param_variables': None,
                'param_widget': 'list',
                'param_quiz': None,
                'position': 14,
                'validator_type_id': None,
                'label': 'Дата диапазон',
            }
        )
        self.assertEqual(
            questions['answer_files_64360'], {
                'answer_type_id': 34,
                'initial': None,
                'page': 1,
                'param_data_source': 'survey_question_choice',
                'param_data_source_params': None,
                'param_date_field_max': None,
                'param_date_field_min': None,
                'param_date_field_type': 'date',
                'param_help_text': '',
                'param_hint_data_source': None,
                'param_hint_data_source_params': None,
                'param_hint_type_id': None,
                'param_is_allow_multiple_choice': False,
                'param_is_allow_other': False,
                'param_is_disabled_init_item': True,
                'param_is_hidden': False,
                'param_is_random_choices_position': False,
                'param_is_required': False,
                'param_is_section_header': False,
                'param_max': 1,
                'param_max_file_size': 20,
                'param_max_files_count': 20,
                'param_min': 0,
                'param_modify_choices': 'natural',
                'param_price': None,
                'param_slug': 'answer_files_64360',
                'param_suggest_choices': False,
                'param_variables': None,
                'param_widget': 'list',
                'param_quiz': None,
                'position': 8,
                'validator_type_id': None,
                'label': 'Файлы',
            }
        )

        question_data = questions['answer_group_64367']
        filters = question_data.pop('param_data_source_params')
        expected_filters = {
            "filter": {"name": "question"},
            "type": "specified_value",
            "value": survey.surveyquestion_set.get(param_slug='answer_group_64367').pk
        }
        json_filters = filters['filters']
        self.assertEqual(len(json_filters), 1)
        self.assertEqual(
            json_filters[0],
            expected_filters,
        )
        self.assertEqual(
            questions['answer_group_64367'], {
                'answer_type_id': 1040,
                'initial': None,
                'page': 2,
                'param_data_source': 'survey_question_choice',
                'param_date_field_max': None,
                'param_date_field_min': None,
                'param_date_field_type': 'date',
                'param_help_text': '',
                'param_hint_data_source': None,
                'param_hint_data_source_params': None,
                'param_hint_type_id': None,
                'param_is_allow_multiple_choice': False,
                'param_is_allow_other': False,
                'param_is_disabled_init_item': True,
                'param_is_hidden': False,
                'param_is_random_choices_position': False,
                'param_is_required': False,
                'param_is_section_header': False,
                'param_max': 1,
                'param_max_file_size': 20,
                'param_max_files_count': 20,
                'param_min': 0,
                'param_modify_choices': 'natural',
                'param_price': None,
                'param_slug': 'answer_group_64367',
                'param_suggest_choices': False,
                'param_variables': None,
                'param_widget': 'list',
                'param_quiz': None,
                'position': 1,
                'validator_type_id': None,
                'label': 'Группа вопросов',
            }
        )
        self.assertEqual(
            questions['answer_long_text_64355'], {
                'answer_type_id': 2,
                'initial': None,
                'page': 1,
                'param_data_source': 'survey_question_choice',
                'param_data_source_params': None,
                'param_date_field_max': None,
                'param_date_field_min': None,
                'param_date_field_type': 'date',
                'param_help_text': '',
                'param_hint_data_source': None,
                'param_hint_data_source_params': None,
                'param_hint_type_id': None,
                'param_is_allow_multiple_choice': False,
                'param_is_allow_other': False,
                'param_is_disabled_init_item': True,
                'param_is_hidden': False,
                'param_is_random_choices_position': False,
                'param_is_required': False,
                'param_is_section_header': False,
                'param_max': None,
                'param_max_file_size': 20,
                'param_max_files_count': 20,
                'param_min': 0,
                'param_modify_choices': 'natural',
                'param_price': None,
                'param_slug': 'answer_long_text_64355',
                'param_suggest_choices': False,
                'param_variables': None,
                'param_widget': 'list',
                'param_quiz': None,
                'position': 3,
                'validator_type_id': None,
                'label': 'Длинный ответ',
            }
        )

        question_data = questions['answer_name_64363']
        filters = question_data.pop('param_data_source_params')
        expected_filters = {
            "filter": {"name": "question"},
            "type": "specified_value",
            "value": survey.surveyquestion_set.get(param_slug='answer_name_64363').pk
        }
        json_filters = filters['filters']
        self.assertEqual(len(json_filters), 1)
        self.assertEqual(
            json_filters[0],
            expected_filters,
        )
        self.assertEqual(
            questions['answer_name_64363'], {
                'answer_type_id': 37,
                'initial': None,
                'page': 1,
                'param_data_source': 'survey_question_choice',
                'param_date_field_max': None,
                'param_date_field_min': None,
                'param_date_field_type': 'date',
                'param_help_text': '',
                'param_hint_data_source': None,
                'param_hint_data_source_params': None,
                'param_hint_type_id': None,
                'param_is_allow_multiple_choice': False,
                'param_is_allow_other': False,
                'param_is_disabled_init_item': True,
                'param_is_hidden': False,
                'param_is_random_choices_position': False,
                'param_is_required': False,
                'param_is_section_header': False,
                'param_max': 1,
                'param_max_file_size': 20,
                'param_max_files_count': 20,
                'param_min': 0,
                'param_modify_choices': 'natural',
                'param_price': None,
                'param_slug': 'answer_name_64363',
                'param_suggest_choices': False,
                'param_variables': None,
                'param_widget': 'list',
                'param_quiz': None,
                'position': 11,
                'validator_type_id': None,
                'label': 'Имя',
            }
        )
        self.assertEqual(
            questions['answer_non_profile_email_64358'], {
                'answer_type_id': 32,
                'initial': None,
                'page': 1,
                'param_data_source': 'survey_question_choice',
                'param_data_source_params': None,
                'param_date_field_max': None,
                'param_date_field_min': None,
                'param_date_field_type': 'date',
                'param_help_text': '',
                'param_hint_data_source': 'user_email_list',
                'param_hint_data_source_params': None,
                'param_hint_type_id': None,
                'param_is_allow_multiple_choice': False,
                'param_is_allow_other': False,
                'param_is_disabled_init_item': True,
                'param_is_hidden': False,
                'param_is_random_choices_position': False,
                'param_is_required': False,
                'param_is_section_header': False,
                'param_max': 1,
                'param_max_file_size': 20,
                'param_max_files_count': 20,
                'param_min': 0,
                'param_modify_choices': 'natural',
                'param_price': None,
                'param_slug': 'answer_non_profile_email_64358',
                'param_suggest_choices': False,
                'param_variables': None,
                'param_widget': 'list',
                'param_quiz': None,
                'position': 6,
                'validator_type_id': None,
                'label': 'Email',
            }
        )

        question_data = questions['answer_number_64357']
        filters = question_data.pop('param_data_source_params')
        expected_filters = {
            "filter": {"name": "question"},
            "type": "specified_value",
            "value": survey.surveyquestion_set.get(param_slug='answer_number_64357').pk
        }
        json_filters = filters['filters']
        self.assertEqual(len(json_filters), 1)
        self.assertEqual(
            json_filters[0],
            expected_filters,
        )
        self.assertEqual(
            questions['answer_number_64357'], {
                'answer_type_id': 31,
                'initial': None,
                'page': 1,
                'param_data_source': 'survey_question_choice',
                'param_date_field_max': None,
                'param_date_field_min': None,
                'param_date_field_type': 'date',
                'param_help_text': '',
                'param_hint_data_source': None,
                'param_hint_data_source_params': None,
                'param_hint_type_id': None,
                'param_is_allow_multiple_choice': False,
                'param_is_allow_other': False,
                'param_is_disabled_init_item': True,
                'param_is_hidden': False,
                'param_is_random_choices_position': False,
                'param_is_required': False,
                'param_is_section_header': False,
                'param_max': 1000,
                'param_max_file_size': 20,
                'param_max_files_count': 20,
                'param_min': 0,
                'param_modify_choices': 'natural',
                'param_price': None,
                'param_slug': 'answer_number_64357',
                'param_suggest_choices': False,
                'param_variables': None,
                'param_widget': 'list',
                'param_quiz': None,
                'position': 5,
                'validator_type_id': None,
                'label': 'Поле ввода цифр',
            }
        )
        self.assertEqual(
            questions['answer_phone_64364'], {
                'answer_type_id': 38,
                'initial': None,
                'page': 1,
                'param_data_source': 'survey_question_choice',
                'param_data_source_params': None,
                'param_date_field_max': None,
                'param_date_field_min': None,
                'param_date_field_type': 'date',
                'param_help_text': '',
                'param_hint_data_source': None,
                'param_hint_data_source_params': None,
                'param_hint_type_id': None,
                'param_is_allow_multiple_choice': False,
                'param_is_allow_other': False,
                'param_is_disabled_init_item': True,
                'param_is_hidden': False,
                'param_is_random_choices_position': False,
                'param_is_required': False,
                'param_is_section_header': False,
                'param_max': 1,
                'param_max_file_size': 20,
                'param_max_files_count': 20,
                'param_min': 0,
                'param_modify_choices': 'natural',
                'param_price': None,
                'param_slug': 'answer_phone_64364',
                'param_suggest_choices': False,
                'param_variables': None,
                'param_widget': 'list',
                'param_quiz': None,
                'position': 12,
                'validator_type_id': None,
                'label': 'Телефон',
            }
        )

        question_data = questions['answer_short_text_64354']
        question_data.pop('param_data_source_params')
        self.assertEqual(
            questions['answer_short_text_64354'], {
                'answer_type_id': 1,
                'initial': None,
                'page': 1,
                'param_data_source': 'survey_question_choice',
                'param_date_field_max': None,
                'param_date_field_min': None,
                'param_date_field_type': 'date',
                'param_help_text': '',
                'param_hint_data_source': None,
                'param_hint_data_source_params': None,
                'param_hint_type_id': None,
                'param_is_allow_multiple_choice': False,
                'param_is_allow_other': False,
                'param_is_disabled_init_item': True,
                'param_is_hidden': False,
                'param_is_random_choices_position': False,
                'param_is_required': True,
                'param_is_section_header': False,
                'param_max': 100,
                'param_max_file_size': 20,
                'param_max_files_count': 20,
                'param_min': 1,
                'param_modify_choices': 'natural',
                'param_price': None,
                'param_slug': 'answer_short_text_64354',
                'param_suggest_choices': False,
                'param_variables': None,
                'param_widget': 'list',
                'param_quiz': None,
                'position': 1,
                'validator_type_id': None,
                'label': 'Короткий ответ',
            }
        )

        question_data = questions['answer_short_text_64368']
        question_data.pop('param_data_source_params')
        self.assertEqual(
            questions['answer_short_text_64368'], {
                'answer_type_id': 1,
                'initial': None,
                'page': 2,
                'param_data_source': 'survey_question_choice',
                'param_date_field_max': None,
                'param_date_field_min': None,
                'param_date_field_type': 'date',
                'param_help_text': '',
                'param_hint_data_source': None,
                'param_hint_data_source_params': None,
                'param_hint_type_id': None,
                'param_is_allow_multiple_choice': False,
                'param_is_allow_other': False,
                'param_is_disabled_init_item': True,
                'param_is_hidden': False,
                'param_is_random_choices_position': False,
                'param_is_required': False,
                'param_is_section_header': False,
                'param_max': None,
                'param_max_file_size': 20,
                'param_max_files_count': 20,
                'param_min': 0,
                'param_modify_choices': 'natural',
                'param_price': None,
                'param_slug': 'answer_short_text_64368',
                'param_suggest_choices': False,
                'param_variables': None,
                'param_widget': 'list',
                'param_quiz': None,
                'position': 1,
                'validator_type_id': None,
                'label': 'Короткий ответ из группы',
            }
        )
        self.assertEqual(
            questions['answer_statement_64356'], {
                'answer_type_id': 28,
                'initial': None,
                'page': 1,
                'param_data_source': 'survey_question_choice',
                'param_data_source_params': None,
                'param_date_field_max': None,
                'param_date_field_min': None,
                'param_date_field_type': 'date',
                'param_help_text': '',
                'param_hint_data_source': None,
                'param_hint_data_source_params': None,
                'param_hint_type_id': None,
                'param_is_allow_multiple_choice': False,
                'param_is_allow_other': False,
                'param_is_disabled_init_item': True,
                'param_is_hidden': False,
                'param_is_random_choices_position': False,
                'param_is_required': True,
                'param_is_section_header': False,
                'param_max': 1,
                'param_max_file_size': 20,
                'param_max_files_count': 20,
                'param_min': 1,
                'param_modify_choices': 'natural',
                'param_price': None,
                'param_slug': 'answer_statement_64356',
                'param_suggest_choices': False,
                'param_variables': None,
                'param_widget': 'list',
                'param_quiz': None,
                'position': 4,
                'validator_type_id': None,
                'label': 'Сообщение',
            }
        )
        self.assertEqual(
            questions['answer_surname_64362'], {
                'answer_type_id': 36,
                'initial': None,
                'page': 1,
                'param_data_source': 'survey_question_choice',
                'param_data_source_params': None,
                'param_date_field_max': None,
                'param_date_field_min': None,
                'param_date_field_type': 'date',
                'param_help_text': '',
                'param_hint_data_source': None,
                'param_hint_data_source_params': None,
                'param_hint_type_id': None,
                'param_is_allow_multiple_choice': False,
                'param_is_allow_other': False,
                'param_is_disabled_init_item': True,
                'param_is_hidden': False,
                'param_is_random_choices_position': False,
                'param_is_required': False,
                'param_is_section_header': False,
                'param_max': 1,
                'param_max_file_size': 20,
                'param_max_files_count': 20,
                'param_min': 0,
                'param_modify_choices': 'natural',
                'param_price': None,
                'param_slug': 'answer_surname_64362',
                'param_suggest_choices': False,
                'param_variables': None,
                'param_widget': 'list',
                'param_quiz': None,
                'position': 10,
                'validator_type_id': None,
                'label': 'Фамилия',
            }
        )
        self.assertEqual(
            questions['answer_url_64361'], {
                'answer_type_id': 35,
                'initial': None,
                'page': 1,
                'param_data_source': 'survey_question_choice',
                'param_data_source_params': None,
                'param_date_field_max': None,
                'param_date_field_min': None,
                'param_date_field_type': 'date',
                'param_help_text': '',
                'param_hint_data_source': None,
                'param_hint_data_source_params': None,
                'param_hint_type_id': None,
                'param_is_allow_multiple_choice': False,
                'param_is_allow_other': False,
                'param_is_disabled_init_item': True,
                'param_is_hidden': False,
                'param_is_random_choices_position': False,
                'param_is_required': False,
                'param_is_section_header': False,
                'param_max': 1,
                'param_max_file_size': 20,
                'param_max_files_count': 20,
                'param_min': 0,
                'param_modify_choices': 'natural',
                'param_price': None,
                'param_slug': 'answer_url_64361',
                'param_suggest_choices': False,
                'param_variables': None,
                'param_widget': 'list',
                'param_quiz': None,
                'position': 9,
                'validator_type_id': None,
                'label': 'URL',
            }
        )

    def test_creating_choices(self):
        json_string = self.get_json_from_file('test_creating_choices.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id='123')

        params = (
            'survey_question_id',
            'position',
            'slug',
            'is_hidden',
            'label_image',
            'label',
        )

        choices_ex = defaultdict(dict)
        for choice in \
                SurveyQuestionChoice.objects.filter(survey_question__survey=survey).select_related('survey_question'):
            choices_ex[choice.survey_question.param_slug][choice.slug] = choice

        choice_objects = SurveyQuestionChoice.objects.filter(survey_question__survey=survey)
        choices = {
            choice['slug']: choice
            for choice in choice_objects.values(*params)
        }
        self.assertEqual(len(choice_objects), 9)
        self.assertEqual(
            choices['149981'], {
                'is_hidden': False,
                'label': 'Установка или настройка ПО',
                'label_image': None,
                'position': 1,
                'slug': '149981',
                'survey_question_id': choice_objects.get(slug='149981').survey_question_id
            }
        )
        self.assertEqual(
            choices['149982'], {
                'is_hidden': False,
                'label': 'Заказ оборудования',
                'label_image': None,
                'position': 2,
                'slug': '149982',
                'survey_question_id': choice_objects.get(slug='149982').survey_question_id
            }
        )
        self.assertEqual(
            choices['149983'], {
                'is_hidden': False,
                'label': 'Настройка оборудования',
                'label_image': None,
                'position': 3,
                'slug': '149983',
                'survey_question_id': choice_objects.get(slug='149983').survey_question_id
            }
        )
        self.assertEqual(
            choices['149984'], {
                'is_hidden': False,
                'label': 'Обслуживание и ремонт оборудования',
                'label_image': None,
                'position': 4,
                'slug': '149984',
                'survey_question_id': choice_objects.get(slug='149984').survey_question_id
            }
        )
        self.assertEqual(
            choices['149985'], {
                'is_hidden': False,
                'label': 'Другое',
                'label_image': None,
                'position': 5,
                'slug': '149985',
                'survey_question_id': choice_objects.get(slug='149985').survey_question_id
            }
        )
        self.assertEqual(
            choices['150050'], {
                'is_hidden': False,
                'label': 'Блокирующий — невозможно продолжать работу',
                'label_image': None,
                'position': 1,
                'slug': '150050',
                'survey_question_id': choice_objects.get(slug='150050').survey_question_id
            }
        )
        self.assertEqual(
            choices['150051'], {
                'is_hidden': False,
                'label': 'Критичный — сильно затрудняет работу',
                'label_image': None,
                'position': 2,
                'slug': '150051',
                'survey_question_id': choice_objects.get(slug='150051').survey_question_id
            }
        )
        self.assertEqual(
            choices['150052'], {
                'is_hidden': False,
                'label': 'Средний — неприятно, но можно потерпеть',
                'label_image': None,
                'position': 3,
                'slug': '150052',
                'survey_question_id': choice_objects.get(slug='150052').survey_question_id
            }
        )
        self.assertEqual(
            choices['150053'], {
                'is_hidden': False,
                'label': 'Минимальный — хорошо бы сделать, но не срочно',
                'label_image': None,
                'position': 4,
                'slug': '150053',
                'survey_question_id': choice_objects.get(slug='150053').survey_question_id
            }
        )

    def test_creating_matrixes(self):
        json_string = self.get_json_from_file('test_creating_matrixes.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id='123')

        params = (
            'survey_question_id',
            'position',
            'type',
            'label',
        )
        matrix_objects = SurveyQuestionMatrixTitle.objects.filter(survey_question__survey=survey)
        matrixes = {
            matrix['label']: matrix
            for matrix in matrix_objects.values(*params)
        }
        self.assertEqual(len(matrix_objects), 20)
        self.assertEqual(
            matrixes['1'], {
                'label': '1',
                'position': 1,
                'survey_question_id': matrix_objects.get(label='1').survey_question_id,
                'type': 'column',
            }
        )
        self.assertEqual(
            matrixes['2'], {
                'label': '2',
                'position': 2,
                'survey_question_id': matrix_objects.get(label='2').survey_question_id,
                'type': 'column',
            }
        )
        self.assertEqual(
            matrixes['3'], {
                'label': '3',
                'position': 3,
                'survey_question_id': matrix_objects.get(label='3').survey_question_id,
                'type': 'column',
            }
        )
        self.assertEqual(
            matrixes['4'], {
                'label': '4',
                'position': 4,
                'survey_question_id': matrix_objects.get(label='4').survey_question_id,
                'type': 'column',
            }
        )
        self.assertEqual(
            matrixes['5'], {
                'label': '5',
                'position': 5,
                'survey_question_id': matrix_objects.get(label='5').survey_question_id,
                'type': 'column',
            }
        )
        self.assertEqual(
            matrixes['6'], {
                'label': '6',
                'position': 6,
                'survey_question_id': matrix_objects.get(label='6').survey_question_id,
                'type': 'column',
            }
        )
        self.assertEqual(
            matrixes['7'], {
                'label': '7',
                'position': 7,
                'survey_question_id': matrix_objects.get(label='7').survey_question_id,
                'type': 'column',
            }
        )
        self.assertEqual(
            matrixes['8'], {
                'label': '8',
                'position': 8,
                'survey_question_id': matrix_objects.get(label='8').survey_question_id,
                'type': 'column',
            }
        )
        self.assertEqual(
            matrixes['9'], {
                'label': '9',
                'position': 9,
                'survey_question_id': matrix_objects.get(label='9').survey_question_id,
                'type': 'column',
            }
        )
        self.assertEqual(
            matrixes['10'], {
                'label': '10',
                'position': 10,
                'survey_question_id': matrix_objects.get(label='10').survey_question_id,
                'type': 'column',
            }
        )
        self.assertEqual(
            matrixes['Вторая строка'], {
                'label': 'Вторая строка',
                'position': 2,
                'survey_question_id': matrix_objects.get(label='Вторая строка').survey_question_id,
                'type': 'row',
            }
        )
        self.assertEqual(
            matrixes['Второй столбец'], {
                'label': 'Второй столбец',
                'position': 2,
                'survey_question_id': matrix_objects.get(label='Второй столбец').survey_question_id,
                'type': 'column',
            }
        )
        self.assertEqual(
            matrixes['Оцените по десятибальной шкале'], {
                'label': 'Оцените по десятибальной шкале',
                'position': 1,
                'survey_question_id': matrix_objects.get(label='Оцените по десятибальной шкале').survey_question_id,
                'type': 'row',
            }
        )
        self.assertEqual(
            matrixes['Первая строка'], {
                'label': 'Первая строка',
                'position': 1,
                'survey_question_id': matrix_objects.get(label='Первая строка').survey_question_id,
                'type': 'row',
            }
        )
        self.assertEqual(
            matrixes['Первый столбец'], {
                'label': 'Первый столбец',
                'position': 1,
                'survey_question_id': matrix_objects.get(label='Первый столбец').survey_question_id,
                'type': 'column',
            }
        )
        self.assertEqual(
            matrixes['Пятый столбец'], {
                'label': 'Пятый столбец',
                'position': 5,
                'survey_question_id': matrix_objects.get(label='Пятый столбец').survey_question_id,
                'type': 'column',
            }
        )
        self.assertEqual(
            matrixes['Третий столбец'], {
                'label': 'Третий столбец',
                'position': 3,
                'survey_question_id': matrix_objects.get(label='Третий столбец').survey_question_id,
                'type': 'column',
            }
        )
        self.assertEqual(
            matrixes['Третья строка'], {
                'label': 'Третья строка',
                'position': 3,
                'survey_question_id': matrix_objects.get(label='Третья строка').survey_question_id,
                'type': 'row',
            }
        )
        self.assertEqual(
            matrixes['Четвертая строка'], {
                'label': 'Четвертая строка',
                'position': 4,
                'survey_question_id': matrix_objects.get(label='Четвертая строка').survey_question_id,
                'type': 'row',
            }
        )
        self.assertEqual(
            matrixes['Четвертый столбец'], {
                'label': 'Четвертый столбец',
                'position': 4,
                'survey_question_id': matrix_objects.get(label='Четвертый столбец').survey_question_id,
                'type': 'column',
            }
        )

    def test_creating_show_conditions_node_items(self):
        json_string = self.get_json_from_file('test_creating_show_conditions_nodes.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id='123')

        show_conditions_node_objects = \
            SurveyQuestionShowConditionNode.objects.filter(survey_question__survey=survey)
        self.assertEqual(len(show_conditions_node_objects), 4)

        params = (
            'operator',
            'position',
            'condition',
            'content_type_attribute',
            'survey_question_choice',
        )
        show_conditions_node_items_objects = \
            SurveyQuestionShowConditionNodeItem.objects.filter(survey_question__survey=survey)

        show_conditions_node_items = [
            show_conditions_node_item
            for show_conditions_node_item in show_conditions_node_items_objects.values(*params).order_by('pk')
        ]
        self.assertEqual(len(show_conditions_node_items), 10)
        self.assertEqual(
            show_conditions_node_items, [
                {
                    'condition': 'eq',
                    'content_type_attribute': 2,
                    'operator': 'and',
                    'position': 1,
                    'survey_question_choice': None
                },
                {
                    'condition': 'eq',
                    'content_type_attribute': 2,
                    'operator': 'and',
                    'position': 1,
                    'survey_question_choice': None
                },
                {
                    'condition': 'eq',
                    'content_type_attribute': 2,
                    'operator': 'or',
                    'position': 2,
                    'survey_question_choice': None
                },
                {
                    'condition': 'eq',
                    'content_type_attribute': 2,
                    'operator': 'or',
                    'position': 3,
                    'survey_question_choice': None
                },
                {
                    'condition': 'eq',
                    'content_type_attribute': 2,
                    'operator': 'and',
                    'position': 1,
                    'survey_question_choice': None
                },
                {
                    'condition': 'eq',
                    'content_type_attribute': 2,
                    'operator': 'or',
                    'position': 2,
                    'survey_question_choice': None
                },
                {
                    'condition': 'eq',
                    'content_type_attribute': 2,
                    'operator': 'or',
                    'position': 3,
                    'survey_question_choice': None
                },
                {
                    'condition': 'eq',
                    'content_type_attribute': 2,
                    'operator': 'and',
                    'position': 1,
                    'survey_question_choice': None
                },
                {
                    'condition': 'eq',
                    'content_type_attribute': 2,
                    'operator': 'or',
                    'position': 2,
                    'survey_question_choice': None
                },
                {
                    'condition': 'eq',
                    'content_type_attribute': 2,
                    'operator': 'or',
                    'position': 3,
                    'survey_question_choice': None
                }
            ]
        )

    def test_creating_hooks(self):
        json_string = self.get_json_from_file('test_creating_hooks.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id='123')

        params = (
            'is_active',
            'position',
            'survey_id',
            'triggers',
        )
        hooks_objects = SurveyHook.objects.filter(survey=survey)
        hooks = {
            hook['is_active']: hook
            for hook in hooks_objects.values(*params).order_by('pk')
        }
        self.assertEqual(len(SurveyHook.objects.filter(survey=survey)), 2)
        self.assertEqual(
            hooks[True], {
                'is_active': True,
                'position': 1,
                'survey_id': survey.pk,
                'triggers': 1,
            },
        )
        self.assertEqual(
            hooks[False], {
                'is_active': False,
                'position': 1,
                'survey_id': survey.pk,
                'triggers': 2,
            },
        )

    def test_creating_subscriptions(self):
        json_string = self.get_json_from_file('test_creating_hooks.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id='123')

        params = (
            'service_type_action',
            'is_synchronous',
            'is_active',
            'context_language',
            'title',
            'body',
            'email_to_address',
            'email_from_address',
            'email_spam_check',
            'http_url',
            'http_method',
            'http_format_name',
            'is_all_questions',
        )
        subscriptions = {
            subscription['title']: subscription
            for subscription in ServiceSurveyHookSubscription.objects.filter(survey_hook__survey=survey).values(*params)
        }
        self.assertEqual(len(subscriptions), 7)
        self.assertEqual(
            subscriptions['one'], {
                'body': '\n-----\nКоманда Яндекс.Форм\n-----\n',
                'context_language': 'ru',
                'email_from_address': 'devnull@yandex-team.ru',
                'email_spam_check': True,
                'email_to_address': '',
                'http_format_name': 'json',
                'http_method': 'get',
                'http_url': '',
                'is_active': True,
                'is_all_questions': False,
                'is_synchronous': False,
                'service_type_action': 3,
                'title': 'one'
            }
        )
        self.assertEqual(
            subscriptions['two'], {
                'body': '',
                'context_language': 'ru',
                'email_from_address': 'test@example.org',
                'email_spam_check': True,
                'email_to_address': '',
                'http_format_name': 'json',
                'http_method': 'get',
                'http_url': '',
                'is_active': True,
                'is_all_questions': False,
                'is_synchronous': True,
                'service_type_action': 3,
                'title': 'two'
            }
        )
        self.assertEqual(
            subscriptions['three'], {
                'body': '',
                'context_language': 'ru',
                'email_from_address': '',
                'email_spam_check': True,
                'email_to_address': '',
                'http_format_name': 'json',
                'http_method': 'get',
                'http_url': 'yandex.ru/create_answer',
                'is_active': True,
                'is_all_questions': False,
                'is_synchronous': False,
                'service_type_action': 6,
                'title': 'three'
            }
        )
        self.assertEqual(
            subscriptions['four'], {
                'body': '',
                'context_language': 'ru',
                'email_from_address': '',
                'email_spam_check': True,
                'email_to_address': '',
                'http_format_name': 'json',
                'http_method': 'get',
                'http_url': '',
                'is_active': True,
                'is_all_questions': False,
                'is_synchronous': False,
                'service_type_action': 8,
                'title': 'four'
            }
        )
        self.assertEqual(
            subscriptions['five'], {
                'body': '\n-----\nКоманда Яндекс.Форм\n-----\n',
                'context_language': 'ru',
                'email_from_address': 'devnull@yandex-team.ru',
                'email_spam_check': True,
                'email_to_address': '',
                'http_format_name': 'json',
                'http_method': 'get',
                'http_url': '',
                'is_active': True,
                'is_all_questions': False,
                'is_synchronous': False,
                'service_type_action': 3,
                'title': 'five'
            }
        )
        self.assertEqual(
            subscriptions['six'], {
                'body': '',
                'context_language': 'ru',
                'email_from_address': '',
                'email_spam_check': True,
                'email_to_address': '',
                'http_format_name': 'json',
                'http_method': 'get',
                'http_url': 'yandex.ru/create_answer',
                'is_active': True,
                'is_all_questions': False,
                'is_synchronous': False,
                'service_type_action': 6,
                'title': 'six'
            }
        )
        self.assertEqual(
            subscriptions['seven'], {
                'body': 'test test test',
                'context_language': 'ru',
                'email_from_address': '',
                'email_spam_check': True,
                'email_to_address': '',
                'http_format_name': 'json',
                'http_method': 'get',
                'http_url': '',
                'is_active': True,
                'is_all_questions': False,
                'is_synchronous': False,
                'service_type_action': 8,
                'title': 'seven'
            }
        )

    def test_creating_questions_in_subscription(self):
        json_string = self.get_json_from_file('test_creating_hooks.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id='123')

        params = (
            'title',
            'questions',
        )
        subscriptions = {
            subscription['questions']: subscription
            for subscription in ServiceSurveyHookSubscription.objects.filter(survey_hook__survey=survey).values(*params)
        }
        self.assertEqual(len(subscriptions), 3)
        self.assertEqual(
            subscriptions[survey.surveyquestion_set.get(param_slug='answer_long_text_64388').pk], {
                'questions': survey.surveyquestion_set.get(param_slug='answer_long_text_64388').pk,
                'title': 'one',
            }
        )
        self.assertEqual(
            subscriptions[survey.surveyquestion_set.get(param_slug='answer_short_text_64387').pk], {
                'questions': survey.surveyquestion_set.get(param_slug='answer_short_text_64387').pk,
                'title': 'one',
            }
        )

    def test_creating_json_rpc_integrations(self):
        json_string = self.get_json_from_file('test_creating_hooks.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id='123')

        params = (
            'params',
            'method',
            'subscription',
        )
        json_rpcs = {
            json_rpc['method']: json_rpc
            for json_rpc in (
                JSONRPCSubscriptionData.objects.filter(subscription__survey_hook__survey=survey)
                .values(*params)
            )
        }
        self.assertEqual(len(json_rpcs), 7)
        self.assertEqual(
            json_rpcs['get_one'], {
                'method': 'get_one',
                'params': None,
                'subscription':
                ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='one').pk
            }
        )
        self.assertEqual(
            json_rpcs['get_two'], {
                'method': 'get_two',
                'params': None,
                'subscription':
                ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='two').pk
            }
        )
        self.assertEqual(
            json_rpcs['get_three'], {
                'method': 'get_three',
                'params': None,
                'subscription':
                ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='three').pk
            }
        )
        self.assertEqual(
            json_rpcs['get_four'], {
                'method': 'get_four',
                'params': None,
                'subscription':
                ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='four').pk
            }
        )
        self.assertEqual(
            json_rpcs['get_five'], {
                'method': 'get_five',
                'params': None,
                'subscription':
                ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='five').pk
            }
        )
        self.assertEqual(
            json_rpcs['get_six'], {
                'method': 'get_six',
                'params': None,
                'subscription':
                ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='six').pk
            }
        )
        self.assertEqual(
            json_rpcs['get_seven'], {
                'method': 'get_seven',
                'params': None,
                'subscription':
                ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='seven').pk
            }
        )

    def test_creating_json_rpc_params(self):
        json_string = self.get_json_from_file('test_creating_integrations_params.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id='123')

        params = (
            'name',
            'value',
            'add_only_with_value',
        )
        json_rpc_params = {
            json_rpc_param['value']: json_rpc_param
            for json_rpc_param
            in JSONRPCSubscriptionParam.objects.filter(subscription__subscription__survey_hook__survey=survey).
            values(*params)
        }
        self.assertEqual(len(json_rpc_params), 2)
        self.assertEqual(
            json_rpc_params['one'], {
                'add_only_with_value': False,
                'name': 'text1',
                'value': 'one'
            }
        )
        self.assertEqual(
            json_rpc_params['two'], {
                'add_only_with_value': True,
                'name': 'text2',
                'value': 'two'
            }
        )

    def test_creating_startrek_integrations(self):
        json_string = self.get_json_from_file('test_creating_hooks.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id='123')

        params = (
            'tags',
            'followers',
            'components',
            'fields',
            'queue',
            'parent',
            'author',
            'assignee',
            'type',
            'project',
            'priority',
            'subscription',
        )
        startreks = {
            startrek['author']: startrek
            for startrek
            in StartrekSubscriptionData.objects.filter(subscription__survey_hook__survey=survey).values(*params)
        }
        self.assertEqual(len(startreks), 7)
        self.assertEqual(
            startreks['one'], {
                'assignee': '',
                'author': 'one',
                'components': None,
                'fields': [],
                'followers': [],
                'parent': '',
                'priority': None,
                'project': None,
                'queue': '',
                'subscription': ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='one').pk,
                'tags': [],
                'type': None
            },
        )
        self.assertEqual(
            startreks['two'], {
                'assignee': '',
                'author': 'two',
                'components': None,
                'fields': [],
                'followers': None,
                'parent': '',
                'priority': None,
                'project': None,
                'queue': '',
                'subscription': ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='two').pk,
                'tags': None,
                'type': None
            },
        )
        self.assertEqual(
            startreks['three'], {
                'assignee': '',
                'author': 'three',
                'components': None,
                'fields': [],
                'followers': None,
                'parent': '',
                'priority': None,
                'project': None,
                'queue': '',
                'subscription': ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='three').pk,
                'tags': None,
                'type': None
            },
        )
        self.assertEqual(
            startreks['four'], {
                'assignee': '',
                'author': 'four',
                'components': None,
                'fields': [{
                    'key': {
                        'type': 'array/string',
                        'slug': 'testfunctionalTags',
                        'name': 'Функциональность тегов',
                    },
                    'value': '123',
                    'add_only_with_value': False,
                }],
                'followers': [],
                'parent': 'TEST-123',
                'priority': 3,
                'project': None,
                'queue': 'TEST',
                'subscription': ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='four').pk,
                'tags': [],
                'type': 69
            },
        )
        self.assertEqual(
            startreks['five'], {
                'assignee': '',
                'author': 'five',
                'components': None,
                'fields': [],
                'followers': [],
                'parent': '',
                'priority': None,
                'project': None,
                'queue': '',
                'subscription': ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='five').pk,
                'tags': [],
                'type': None
            },
        )
        self.assertEqual(
            startreks['six'], {
                'assignee': '',
                'author': 'six',
                'components': None,
                'fields': [],
                'followers': None,
                'parent': '',
                'priority': None,
                'project': None,
                'queue': '',
                'subscription': ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='six').pk,
                'tags': None,
                'type': None
            },
        )
        self.assertEqual(
            startreks['seven'], {
                'assignee': '',
                'author': 'seven',
                'components': None,
                'fields': [],
                'followers': [],
                'parent': '',
                'priority': 3,
                'project': None,
                'queue': 'TEST',
                'subscription': ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='seven').pk,
                'tags': [],
                'type': 2
            },
        )

    def test_creating_startrek_fields(self):
        json_string = self.get_json_from_file('test_creating_integrations_params.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id='123')

        fields = []
        for it in (
            StartrekSubscriptionData.objects.filter(subscription__survey_hook__survey=survey)
            .values_list('fields', flat=True)
        ):
            fields.extend(it)

        startrek_fields = {
            field.get('value'): field
            for field in fields
        }

        self.assertEqual(len(startrek_fields), 2)

        field_data = startrek_fields['one']
        key = field_data.pop('key')

        self.assertDictEqual(key, {
            'type': 'array/string',
            'slug': 'tags',
            'name': 'Теги',
        })
        self.assertDictEqual(field_data, {
            'add_only_with_value': False,
            'value': 'one'
        })

        field_data = startrek_fields['two']
        key = field_data.pop('key')

        self.assertDictEqual(key, {
            'type': 'array/service',
            'slug': 'abcService',
            'name': 'ABC сервис',
        })
        self.assertDictEqual(field_data, {
            'add_only_with_value': True,
            'value': 'two'
        })

    def test_creating_wiki_integrations(self):
        json_string = self.get_json_from_file('test_creating_hooks.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id='123')

        params = (
            'supertag',
            'text',
            'subscription',
        )
        wikis = {
            wiki['supertag']: wiki
            for wiki
            in WikiSubscriptionData.objects.filter(subscription__survey_hook__survey=survey).values(*params)
        }
        self.assertEqual(len(wikis), 7)
        self.assertEqual(
            wikis['one'], {
                'supertag': 'one',
                'text': '',
                'subscription': ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='one').pk,
            },
        )
        self.assertEqual(
            wikis['two'], {
                'supertag': 'two',
                'text': '',
                'subscription': ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='two').pk,
            },
        )
        self.assertEqual(
            wikis['three'], {
                'supertag': 'three',
                'text': '',
                'subscription': ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='three').pk,
            },
        )
        self.assertEqual(
            wikis['four'], {
                'supertag': 'four',
                'text': '',
                'subscription': ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='four').pk,
            },
        )
        self.assertEqual(
            wikis['five'], {
                'supertag': 'five',
                'text': '',
                'subscription': ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='five').pk,
            },
        )
        self.assertEqual(
            wikis['six'], {
                'supertag': 'six',
                'text': '',
                'subscription': ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='six').pk,
            },
        )
        self.assertEqual(
            wikis['seven'], {
                'supertag': 'seven',
                'text': '',
                'subscription': ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='seven').pk,
            },
        )

    def test_creating_variables(self):
        json_string = self.get_json_from_file('test_creating_variables.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id='123')

        params = (
            'var',
            'format_name',
            'hook_subscription_id',
        )
        variables = [
            variable
            for variable in (
                SurveyVariable.objects.filter(hook_subscription__survey_hook__survey=survey)
                .values(*params).order_by('pk')
            )
        ]
        self.assertEqual(len(variables), 19)

        variables_args = [
            variable['arguments']
            for variable in (
                SurveyVariable.objects.filter(hook_subscription__survey_hook__survey=survey)
                .values('arguments').order_by('pk')
            )
        ]
        questions_var = []
        for variables_arg in variables_args:
            questions_var.append(variables_arg.pop('questions', None))
        questions_var = [questions for questions in questions_var if questions]
        self.assertEqual(len(questions_var), 0)

        self.assertEqual(
            {frozenset(argument.items()) for argument in variables_args},
            {frozenset({}),
             frozenset({'is_all_questions': True, 'only_with_value': False, 'show_filenames': False}.items()),
             frozenset({}),
             frozenset({}),
             frozenset({'show_filenames': False, 'question': survey.surveyquestion_set.get(param_slug='answer_long_text_64388').pk}.items()),
             frozenset({'show_filenames': False, 'question': survey.surveyquestion_set.get(param_slug='answer_long_text_64388').pk}.items()),
             frozenset({}),
             frozenset({}),
             frozenset({}),
             frozenset({'show_filenames': True, 'question': survey.surveyquestion_set.get(param_slug='answer_short_text_71387').pk}.items()),
             frozenset({}),
             frozenset({}),
             frozenset({}),
             frozenset({}),
             frozenset({'show_filenames': False, 'question': survey.surveyquestion_set.get(param_slug='answer_long_text_64388').pk}.items()),
             frozenset({}),
             frozenset({'is_all_questions': True, 'only_with_value': False, 'show_filenames': False}.items()),
             frozenset({'show_filenames': False, 'question': survey.surveyquestion_set.get(param_slug='answer_long_text_64388').pk}.items()),
             frozenset({}),
             }
        )

        self.assertEqual(
            {frozenset(var.items()) for var in variables},
            {frozenset(var.items()) for var in [
                {
                    'format_name': None,
                    'hook_subscription_id':
                        ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='one').pk,
                    'var': 'user.email'
                },
                {
                    'format_name': 'txt',
                    'hook_subscription_id':
                        ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='one').pk,
                    'var': 'form.questions_answers'
                },
                {
                    'format_name': None,
                    'hook_subscription_id':
                        ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='one').pk,
                    'var': 'user.login'
                },
                {
                    'format_name': None,
                    'hook_subscription_id':
                        ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='one').pk,
                    'var': 'form.question_answer'
                },
                {
                    'format_name': None,
                    'hook_subscription_id':
                        ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='one').pk,
                    'var': 'form.question_answer'
                },
                {
                    'format_name': None,
                    'hook_subscription_id':
                        ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='two').pk,
                    'var': 'form.id'
                },
                {
                    'format_name': None,
                    'hook_subscription_id':
                        ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='two').pk,
                    'var': 'browser.osversion'
                },
                {
                    'format_name': None,
                    'hook_subscription_id':
                        ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='two').pk,
                    'var': 'user.email'
                },
                {
                    'format_name': None,
                    'hook_subscription_id':
                        ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='two').pk,
                    'var': 'form.question_answer'
                },
                {
                    'format_name': None,
                    'hook_subscription_id':
                        ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='four').pk,
                    'var': 'user.name'
                },
                {
                    'format_name': None,
                    'hook_subscription_id':
                        ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='four').pk,
                    'var': 'user.login'
                },
                {
                    'format_name': None,
                    'hook_subscription_id':
                        ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='four').pk,
                    'var': 'user.login'
                },
                {
                    'format_name': None,
                    'hook_subscription_id':
                        ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='five').pk,
                    'var': 'user.login'
                },
                {
                    'format_name': None,
                    'hook_subscription_id':
                        ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='five').pk,
                    'var': 'form.question_answer'
                },
                {
                    'format_name': None,
                    'hook_subscription_id':
                        ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='five').pk,
                    'var': 'user.email'
                },
                {
                    'format_name': 'txt',
                    'hook_subscription_id':
                        ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='five').pk,
                    'var': 'form.questions_answers'
                },
                {
                    'format_name': None,
                    'hook_subscription_id':
                        ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='five').pk,
                    'var': 'form.question_answer'
                },
                {
                    'format_name': None,
                    'hook_subscription_id':
                        ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='seven').pk,
                    'var': 'user.login'
                },
                {
                    'format_name': None,
                    'hook_subscription_id':
                        ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey, title='seven').pk,
                    'var': 'user.name'
                },
            ]}
        )

    def test_converting_variables(self):
        json_string = self.get_json_from_file('test_creating_variables.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id='123')

        variables_new_set = {
            variable.variable_id
            for variable in SurveyVariable.objects.filter(hook_subscription__survey_hook__survey=survey)
        }
        variables_old_set = {
            '5c8a2635e5c7873cc6549d32',
            '5c8a2635e5c7873cc6549d33',
            '5c8a2635e5c7873cc6549d34',
            '5c8a2635e5c7873cc6549d35',
            '5c8a2635e5c7873cc6549d36',
            '5c8a275c4fecd7223f000000',
            '5c8a275c4fecd7223f000001',
            '5c8a275c4fecd7223f000002',
            '5c8a275c4fecd7223f000003',
            '5c8a275c4fecd7223f000004',
            '5c8a28e84fecd7223f000005',
            '5c8a28ec4fecd7223f000006',
            '5c8a28f54fecd7223f000007',
            '5c8a290c4fecd7223f000008',
            '5c8a295c4fecd7223f000009',
            '5c8a29614fecd7223f00000a',
            '5c8a29704fecd7223f00000b',
            '5c8a2a0e4fecd7223f00000c',
            '5c8a2a114fecd7223f00000d'}
        self.assertFalse(variables_old_set & variables_new_set)
        self.assertEqual(len(variables_new_set), 19)

    def test_creating_headers(self):
        json_string = self.get_json_from_file('test_creating_variables.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id='123')

        params = (
            'name',
            'add_only_with_value',
        )
        headers = [
            header
            for header in SubscriptionHeader.objects.filter(subscription__survey_hook__survey=survey)
            .values(*params).order_by('pk')
        ]
        self.assertEqual(len(headers), 4)
        self.assertEqual(
            headers, [
                {
                    'add_only_with_value': False,
                    'name': 'x-user-name'
                },
                {
                    'add_only_with_value': False,
                    'name': 'x-orgs'
                },
                {
                    'add_only_with_value': False,
                    'name': 'x-user-name'
                },
                {
                    'add_only_with_value': False,
                    'name': 'x-orgs'
                }
            ]
        )

    def test_creating_hook_condition_nodes_items(self):
        json_string = self.get_json_from_file('test_creating_hook_conditions_nodes.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id='123')

        nodes_objects = SurveyHookConditionNode.objects.filter(hook__survey=survey)
        self.assertEqual(len(nodes_objects), 1)

        params = (
            'operator',
            'position',
            'condition',
            'content_type_attribute',
            'value',
            'survey_question',
        )
        items_objects = SurveyHookCondition.objects.filter(condition_node__hook__survey=survey)
        items = [
            hook_conditions_node_item
            for hook_conditions_node_item
            in items_objects.values(*params).order_by('pk')
        ]
        self.assertEqual(len(items), 2)
        self.assertEqual(
            items, [
                {
                    'condition': 'eq',
                    'content_type_attribute': 12,
                    'operator': 'and',
                    'position': 1,
                    'survey_question': survey.surveyquestion_set.get(param_slug='answer_short_text_67077').pk,
                    'value': 'привет'
                },
                {
                    'condition': 'eq',
                    'content_type_attribute': 2,
                    'operator': 'or',
                    'position': 2,
                    'survey_question': survey.surveyquestion_set.get(param_slug='answer_choices_67079').pk,
                    'value': str(SurveyQuestionChoice.objects.get(slug='138170').pk)
                }
            ]
        )

    def test_creating_submit_condition_nodes_items(self):
        json_string = self.get_json_from_file('test_creating_submit_conditions_nodes.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id='123')

        nodes_objects = SurveySubmitConditionNode.objects.filter(survey=survey)
        self.assertEqual(len(nodes_objects), 3)

        params = (
            'operator',
            'position',
            'condition',
            'content_type_attribute',
            'value',
            'survey_question',
            'survey_question_choice',
        )
        items_objects = SurveySubmitConditionNodeItem.objects.filter(survey_submit_condition_node__survey=survey)
        items = [
            hook_conditions_node_item
            for hook_conditions_node_item
            in items_objects.values(*params).order_by('pk')
        ]
        self.assertEqual(len(items), 3)
        self.assertEqual(
            items, [
                {
                    'condition': u'eq',
                    'content_type_attribute': 12,
                    'operator': u'and',
                    'position': 1,
                    'survey_question': survey.surveyquestion_set.get(param_slug='answer_short_text_67082').pk,
                    'survey_question_choice': None,
                    'value': u'one'
                },
                {
                    'condition': u'eq',
                    'content_type_attribute': 12,
                    'operator': u'and',
                    'position': 1,
                    'survey_question': survey.surveyquestion_set.get(param_slug='answer_short_text_67083').pk,
                    'survey_question_choice': None,
                    'value': u'two'
                },
                {
                    'condition': u'eq',
                    'content_type_attribute': 2,
                    'operator': u'and',
                    'position': 1,
                    'survey_question': survey.surveyquestion_set.get(param_slug='answer_choices_67084').pk,
                    'survey_question_choice': None,
                    'value': str(SurveyQuestionChoice.objects.get(slug='138177').pk)
                }
            ]
        )

    def test_creating_images(self):
        json_string = self.get_json_from_file('test_creating_images.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id='123')

        self.assertEqual(
            survey.surveyquestion_set.get(param_slug='answer_short_text_67080').label_image.name,
            u'teamwork.jpg'
        )
        self.assertEqual(
            survey.surveyquestion_set.get(param_slug='answer_choices_67081').label_image.name,
            u'frute.jpg'
        )
        self.assertEqual(
            survey.surveyquestion_set.get(param_slug='answer_short_text_67080').label_image.image.name,
            u'69466/3530dd7989a2191248f5f18a93d044de'
        )
        self.assertEqual(
            survey.surveyquestion_set.get(param_slug='answer_choices_67081').label_image.image.name,
            u'69076/ef49f8fff0a635bf2ed3b337d6875f9b'
        )

        self.assertEqual(
            SurveyQuestionChoice.objects.get(slug='138172').label_image.name,
            u'apples.jpg'
        )
        self.assertEqual(
            SurveyQuestionChoice.objects.get(slug='138173').label_image.name,
            u'pears.jpg'
        )
        self.assertEqual(
            SurveyQuestionChoice.objects.get(slug='138174').label_image.name,
            u'grapes.jpg'
        )
        self.assertEqual(
            SurveyQuestionChoice.objects.get(slug='138172').label_image.image.name,
            u'69076/2a189b4ec105f535fc6d0761f3a1cad9'
        )
        self.assertEqual(
            SurveyQuestionChoice.objects.get(slug='138173').label_image.image.name,
            u'69076/f11086f26ba8be5fabf01b61e630ecd4'
        )
        self.assertEqual(
            SurveyQuestionChoice.objects.get(slug='138174').label_image.image.name,
            u'69466/a033d81d93ff591e4a7a47b54509cb53'
        )

    def test_creating_attachments(self):
        json_string = self.get_json_from_file('test_creating_attachments.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id='123')

        attachment = SubscriptionAttachment.objects.get(subscription__survey_hook__survey=survey)

        self.assertEqual(
            attachment.file.name,
            u'/404/93ba1029ec43d21db5983d611b72e1ef_admin/teamwork.jpg'
        )
        self.assertEqual(
            attachment.get_file_internal_url(),
            u'https://forms.yandex-team.ru/files?path=%2F404%2F93ba1029ec43d21db5983d611b72e1ef_admin%2Fteamwork.jpg'
        )

    def test_creating_attachment_templates(self):
        json_string = self.get_json_from_file('test_creating_attachments.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id='123')

        templates_objects = IntegrationFileTemplate.objects.filter(survey=survey)

        params = (
            'name',
            'template',
            'type',
            'slug',
            'subscriptions',
        )
        templates = templates_objects.values(*params)

        self.assertEqual(
            templates[0], {
                'subscriptions': ServiceSurveyHookSubscription.objects.get(survey_hook__survey=survey).pk,
                'type': u'pdf',
                'name': u'test pdf template',
                'template': u'# subject\n\ntext1 ****\nfile1 __',
                'slug': u'template_1034',
            }
        )

    def test_should_copy_correct_group_id(self):
        json_string = self.get_json_from_file('test_should_copy_correct_group_id.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id=None)

        questions = {
            question[0]: {
                'pk': question[1],
                'group_id': question[2],
            }
            for question in survey.surveyquestion_set.values_list('param_slug', 'pk', 'group_id')
        }
        group_question = questions['answer_group_15437']
        children = [
            questions['answer_short_text_15458'],
            questions['answer_choices_15456'],
        ]
        self.assertIsNone(group_question['group_id'])
        self.assertEqual(group_question['pk'], children[0]['group_id'])
        self.assertEqual(group_question['pk'], children[1]['group_id'])

    def test_should_correct_import_varables(self):
        json_string = self.get_json_from_file('test_should_correct_import_varables.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id=None)

        variables = list(
            SurveyVariable.objects.all()
            .filter(hook_subscription__survey_hook__survey_id=survey.pk)
        )
        self.assertEqual(len(variables), 4)

        question = SurveyQuestion.objects.get(survey_id=survey.pk, label='field1')

        var_all_questions, var_not_all_questions, var_valid_question, var_invalid_question = None, None, None, None
        for variable in variables:
            if variable.var == 'form.questions_answers':
                if variable.arguments['is_all_questions']:
                    var_all_questions = variable
                else:
                    var_not_all_questions = variable
            elif variable.var == 'form.question_answer':
                if variable.arguments['question'] is not None:
                    var_valid_question = variable
                else:
                    var_invalid_question = variable

        self.assertIsNotNone(var_all_questions)
        self.assertEqual(var_all_questions.arguments['questions'], [])

        self.assertIsNotNone(var_not_all_questions)
        self.assertEqual(var_not_all_questions.arguments['questions'], [question.pk])

        self.assertIsNotNone(var_valid_question)
        self.assertEqual(var_valid_question.arguments['question'], question.pk)

        self.assertIsNotNone(var_invalid_question)
        self.assertIsNone(var_invalid_question.arguments['question'])

    def test_should_link_default_style_template(self):
        SurveyStyleTemplateFactory(
            pk=3333,
            name='mint',
            type='default',
            styles={
                'display': 'none',
            },
        )
        json_string = self.get_json_from_file('test_should_link_default_style_template.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id=None)

        self.assertEqual(survey.styles_template.pk, 3333)
        self.assertEqual(survey.styles_template.name, 'mint')
        self.assertEqual(survey.styles_template.type, 'default')
        self.assertEqual(survey.styles_template.styles, {
            'display': 'none',
        })

    def test_should_create_custom_style_template(self):
        json_string = self.get_json_from_file('test_should_create_custom_style_template.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id=None)

        self.assertNotEqual(survey.styles_template.pk, 1015)
        self.assertEqual(survey.styles_template.name, 'taiga')
        self.assertEqual(survey.styles_template.type, 'custom')
        self.assertEqual(
            survey.styles_template.styles, {
                'question_text_color': '#f9f9f9',
                'bg_gradient': 'linear-gradient(to bottom, rgba(0, 0, 0, 0.5), rgba(0, 0, 0, 0.5))',
                'answer_text_color': '#f9f9f9',
                'bg_url': '${imagePath}/taiga.jpg',
                'body_padding': '15px',
                'controls_color': '#ffd218',
            }
        )

    @override_settings(IS_BUSINESS_SITE=True)
    def test_shouldnt_create_published_form_in_b2b(self):
        json_string = self.get_json_from_file('test_should_create_published_form_in_b2b.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id=None)

        self.assertFalse(survey.is_published_external)
        self.assertIsNone(survey.date_published)

    @override_settings(IS_BUSINESS_SITE=False)
    def test_should_create_published_form_in_int(self):
        json_string = self.get_json_from_file('test_should_create_unpublished_form_in_int.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id=None)

        self.assertFalse(survey.is_published_external)
        self.assertIsNone(survey.date_published)

    def test_should_create_counters(self):
        json_string = self.get_json_from_file('test_should_create_counters.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id=None)

        self.assertIsNotNone(survey.answercount)

    def test_translations_field(self):
        json_string = self.get_json_from_file('test_translations_field.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id=None)

        self.assertIsNotNone(survey.translations)
        self.assertDictEqual(
            survey.translations, {
                'name': {
                    'ru': 'Тест поля translations',
                },
            }
        )

        texts = {
            text.slug: text.translations
            for text in survey.texts.all()
        }
        self.assertEqual(len(texts), 8)
        for translations in texts.values():
            self.assertIsNotNone(translations)
        self.assertDictEqual(texts, {
            'successful_change': {
                'value': {
                    'ru': '',
                },
            },
            'invitation_to_change': {
                'value': {
                    'ru': 'Изменить ответ на опрос',
                },
            },
            'successful_change_title': {
                'value':  {
                    'ru': 'Ваш ответ на опрос был изменен',
                },
            },
            'successful_submission_title': {
                'value': {
                    'ru': 'Спасибо за ответ на опрос',
                },
            },
            'save_changes_button': {
                'value': {
                    'ru': 'Сохранить изменения',
                },
            },
            'successful_submission': {
                'value': {
                    'ru': '',
                },
            },
            'invitation_to_submit': {
                'value': {
                    'ru': 'Ответить на опрос',
                },
            },
            'submit_button': {
                'value': {
                    'ru': 'Отправить',
                },
            },
        })

        questions = {
            question.param_slug: question.translations
            for question in survey.surveyquestion_set.all()
        }
        self.assertEqual(len(questions), 3)
        for translations in questions.values():
            self.assertIsNotNone(translations)
        self.assertDictEqual(questions, {
            'text': {
                'label': {
                    'ru': 'Короткий ответ',
                },
                'param_help_text': {
                    'ru': '',
                },
            },
            'list': {
                'label': {
                    'ru': 'Список',
                },
                'param_help_text': {
                    'ru': '',
                },
            },
            'matrix': {
                'label': {
                    'ru': 'Матрица',
                },
                'param_help_text': {
                    'ru': '',
                },
            },
        })

        choices = {
            choice.slug: choice.translations
            for question in survey.surveyquestion_set.all()
            for choice in question.surveyquestionchoice_set.all()
        }
        self.assertEqual(len(choices), 3)
        for translations in choices.values():
            self.assertIsNotNone(translations)
        self.assertDictEqual(choices, {
            'xone': {
                'label': {
                    'ru': 'один',
                },
            },
            'xtwo': {
                'label': {
                    'ru': 'два',
                },
            },
            'xthree': {
                'label': {
                    'ru': 'три',
                },
            },
        })

        titles = {
            '%s,%s' % (title.type, title.position): title.translations
            for question in survey.surveyquestion_set.all()
            for title in question.surveyquestionmatrixtitle_set.all()
        }
        self.assertEqual(len(titles), 7)
        for translations in titles.values():
            self.assertIsNotNone(translations)
        self.assertDictEqual(titles, {
            'row,1': {
                'label': {
                    'ru': 'Строка1',
                },
            },
            'row,2': {
                'label': {
                    'ru': 'Строка2',
                },
            },
            'column,1': {
                'label': {
                    'ru': '1',
                },
            },
            'column,2': {
                'label': {
                    'ru': '2',
                },
            },
            'column,3': {
                'label': {
                    'ru': '3',
                },
            },
            'column,4': {
                'label': {
                    'ru': '4',
                },
            },
            'column,5': {
                'label': {
                    'ru': '5',
                },
            },
        })

    def test_should_contain_redirect_field(self):
        json_string = self.get_json_from_file('test_should_contain_redirect_field.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id=None)

        extra = survey.extra or {}
        redirect = extra.get('redirect')
        self.assertIsNotNone(redirect)
        self.assertTrue(redirect['enabled'])
        self.assertEqual(redirect['url'], 'https://yandex.ru/')

    def test_import_email_from_title(self):
        json_string = self.get_json_from_file('test_import_email_from_title.json')

        importer = SurveyImporter.from_string(json_string)

        old_subscription = importer.data['survey']['hooks'][0]['subscriptions'][0]
        old_variable = old_subscription['email_from_title'].strip('{}')
        old_ref_question = old_subscription['variables'][old_variable]['arguments']['question']

        survey = importer.import_survey(self.user.pk, dir_id=None)

        new_subscription = survey.hooks.first().subscriptions.first()
        new_variable = new_subscription.email_from_title.strip('{}')
        new_ref_question = new_subscription.variables_map[new_variable].arguments['question']

        self.assertNotEqual(new_variable, old_variable)
        self.assertNotEqual(new_ref_question, old_ref_question)

    @override_settings(APP_TYPE='forms_ext')
    def test_should_set_email_spam_check_for_ext(self):
        json_string = self.get_json_from_file('test_should_set_email_spam_check.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id=None)
        subscription = survey.hooks.first().subscriptions.first()
        self.assertTrue(subscription.email_spam_check)

    @override_settings(APP_TYPE='forms_int')
    def test_shouldnt_set_email_spam_check_for_int(self):
        json_string = self.get_json_from_file('test_should_set_email_spam_check.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id=None)
        subscription = survey.hooks.first().subscriptions.first()
        self.assertFalse(subscription.email_spam_check)

    def test_should_import_form_with_empty_logic_on_choices(self):
        json_string = self.get_json_from_file('test_should_import_form_with_empty_logic_on_choices.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id=None)

        question_on_logic = survey.surveyquestion_set.first()
        question_with_logic = survey.surveyquestion_set.last()

        nodes = list(question_with_logic.show_condition_nodes.all())
        self.assertEqual(len(nodes), 1)
        self.assertEqual(nodes[0].survey_question.pk, question_with_logic.pk)

        conditions = list(nodes[0].items.all())
        self.assertEqual(len(conditions), 1)
        self.assertEqual(conditions[0].value, '')
        self.assertEqual(conditions[0].survey_question.pk, question_on_logic.pk)

    def test_should_convert_profile_questions(self):
        json_string = self.get_json_from_file('test_should_convert_profile_questions.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id=None)

        questions = list(survey.surveyquestion_set.all())
        self.assertEqual(len(questions), 2)
        self.assertEqual(questions[0].answer_type_id, 1)
        self.assertEqual(questions[0].param_slug, 'answer_short_text_%s' % questions[0].pk)
        self.assertEqual(questions[1].answer_type_id, 1)
        self.assertEqual(questions[1].param_slug, 'answer_short_text_%s' % questions[1].pk)

    def test_should_convert_variables_in_json_rpc(self):
        json_string = self.get_json_from_file('test_should_convert_variables_in_json_rpc.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id=None)

        hook = survey.hooks.first()
        subscription = hook.subscriptions.first()
        variables = subscription.variables_map

        # сравниваем с исходными данными из фикстуры
        self.assertNotEqual(subscription.http_url, '{5f6dabf19462cda5ca87ad28}')
        self.assertNotEqual(subscription.json_rpc.method, '{5f6dabf79462cda5ca87ad29}')

        # сравниваем реальные данные
        self.assertIn(subscription.http_url[1:-1], variables)
        self.assertIn(subscription.json_rpc.method[1:-1], variables)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_create_patched_email_from_address_biz(self):
        json_string = self.get_json_from_file('test_should_create_patched_email_from_address.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id=None)

        hook = survey.hooks.first()
        self.assertIsNotNone(hook)
        subscription = hook.subscriptions.first()
        self.assertIsNotNone(subscription)
        self.assertEqual(subscription.email_from_address, f'{survey.pk}@forms-mailer.yaconnect.com')

    def test_should_create_group_question_with_param_not_required(self):
        json_string = self.get_json_from_file('test_should_create_group_question_with_param_not_required.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id=None)

        questions = list(survey.surveyquestion_set.all())
        self.assertEqual(len(questions), 2)
        self.assertEqual(questions[0].answer_type.slug, 'answer_files')
        self.assertTrue(questions[0].param_is_required)
        self.assertEqual(questions[1].answer_type.slug, 'answer_group')
        self.assertFalse(questions[1].param_is_required)

    def test_should_create_stats_param(self):
        json_string = self.get_json_from_file('test_should_create_stats_param.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id=None)

        extra = survey.extra or {}
        stats = extra.get('stats')
        self.assertDictEqual(stats, {
            'enabled': True,
        })

    def test_should_create_survey_with_invalid_submit_conditions(self):
        json_string = self.get_json_from_file('test_should_create_survey_with_invalid_submit_conditions.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id=None)

        questions = survey.surveyquestion_set.all()
        self.assertEqual(len(questions), 1)

        choices = questions[0].surveyquestionchoice_set.all()
        self.assertEqual(len(choices), 2)

        nodes = survey.submit_condition_nodes.all()
        self.assertEqual(len(nodes), 1)

        items = nodes[0].items.all()
        self.assertEqual(len(items), 2)
        self.assertEqual(items[0].value, 'invalid condition value')
        self.assertEqual(items[1].value, str(choices[1].pk))

    def test_should_create_survey_with_invalid_subscription_conditions(self):
        json_string = self.get_json_from_file('test_should_create_survey_with_invalid_subscription_conditions.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id=None)

        questions = survey.surveyquestion_set.all()
        self.assertEqual(len(questions), 1)

        choices = questions[0].surveyquestionchoice_set.all()
        self.assertEqual(len(choices), 3)

        hooks = survey.hooks.all()
        self.assertEqual(len(hooks), 1)

        nodes = hooks[0].condition_nodes.all()
        self.assertEqual(len(nodes), 1)

        items = nodes[0].items.all()
        self.assertEqual(len(items), 2)
        self.assertEqual(items[0].value, '')
        self.assertEqual(items[1].value, str(choices[1].pk))

    def test_should_create_survey_with_follow_result_in_subscription(self):
        json_string = self.get_json_from_file('test_should_create_survey_with_follow_result_in_subscription.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id=None)

        hooks = survey.hooks.all()
        self.assertEqual(len(hooks), 1)

        subscriptions = hooks[0].subscriptions.all()
        self.assertEqual(len(subscriptions), 1)

        self.assertEqual(subscriptions[0].follow_result, True)

    def test_should_preserve_survey_hook_name(self):
        json_string = self.get_json_from_file('test_should_preserve_survey_hook_name.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id=None)

        hooks = survey.hooks.all()
        self.assertEqual(len(hooks), 1)
        self.assertEqual(hooks[0].name, 'Default Survey Hook')

    def test_subscription_in_isolate_mode(self):
        json_string = self.get_json_from_file('test_subscription_in_isolate_mode.json')

        importer = SurveyImporter.from_string(json_string)
        survey = importer.import_survey(self.user.pk, dir_id=None)

        hooks = survey.hooks.all()
        self.assertEqual(len(hooks), 1)

        subscriptions = hooks[0].subscriptions.all()
        self.assertEqual(len(subscriptions), 1)
        self.assertEqual(subscriptions[0].service_type_action_id, 4)
        with self.assertRaises(JSONRPCSubscriptionData.DoesNotExist):
            subscriptions[0].json_rpc
        with self.assertRaises(StartrekSubscriptionData.DoesNotExist):
            subscriptions[0].startrek
        with self.assertRaises(WikiSubscriptionData.DoesNotExist):
            subscriptions[0].wiki
