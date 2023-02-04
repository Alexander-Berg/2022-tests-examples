# -*- coding: utf-8 -*-
import hashlib
import hmac

from django.conf import settings
from unittest.mock import patch, Mock
from django.contrib.contenttypes.models import ContentType
from django.test import TestCase, override_settings
from django.db.utils import IntegrityError

from events.accounts.models import User
from events.accounts.helpers import YandexClient
from events.conditions.models import ContentTypeAttribute
from events.conditions.factories import ContentTypeAttributeFactory
from events.surveyme.factories import (
    SurveyFactory,
    SurveyQuestionChoiceFactory,
    SurveyQuestionFactory,
    SurveySubmitConditionNodeItemFactory,
    SurveySubmitConditionNodeFactory,
)
from events.surveyme.models import SurveyText, Survey, AnswerType


class TestSurveyBehaviour__should_create_texts_on_creation(TestCase):
    fixtures = ['initial_data.json']

    def test_should_create_text(self):
        texts = {
            'submit_button': {
                'value': {
                    'ru': 'Отправить',
                    'en': 'Submit',
                    'tr': 'göndermek',
                }
            }
        }
        with patch.object(SurveyText, 'get_default_texts_for_survey_type', Mock(return_value=texts)):
            survey = SurveyFactory()
            msg = 'должен был создаться один текст при создании сущности опроса'
            self.assertEqual(survey.texts.count(), 1, msg=msg)
            created_text = survey.texts.all()[0]
            self.assertEqual(created_text.slug, 'submit_button')
            self.assertEqual(created_text.value, 'Отправить')
            self.assertEqual(created_text.null, False)
            self.assertEqual(created_text.max_length, None)

    def test_with_only_ru_value(self):
        texts = {
            'submit_button': {
                'value': {
                    'ru': 'Отправить',
                }
            }
        }
        with patch.object(SurveyText, 'get_default_texts_for_survey_type', Mock(return_value=texts)):
            survey = SurveyFactory()
            created_text = survey.texts.all()[0]
            self.assertEqual(created_text.value, 'Отправить')

    def test_without_value(self):
        texts = {
            'submit_button': {}
        }
        with patch.object(SurveyText, 'get_default_texts_for_survey_type', Mock(return_value=texts)):
            survey = SurveyFactory()
            created_text = survey.texts.all()[0]
            self.assertEqual(created_text.value, '')

    def test_with_null(self):
        texts = {
            'submit_button': {
                'null': True
            }
        }
        with patch.object(SurveyText, 'get_default_texts_for_survey_type', Mock(return_value=texts)):
            survey = SurveyFactory()
            created_text = survey.texts.all()[0]
            self.assertEqual(created_text.null, True)

    def test_with_max_length(self):
        texts = {
            'submit_button': {
                'max_length': 100
            }
        }
        with patch.object(SurveyText, 'get_default_texts_for_survey_type', Mock(return_value=texts)):
            survey = SurveyFactory()
            created_text = survey.texts.all()[0]
            self.assertEqual(created_text.max_length, 100)

    def test_should_create_all_texts_for_items(self):
        texts = {
            'submit_button': {
                'value': {
                    'ru': 'Отправить',
                    'en': 'Submit',
                    'tr': 'göndermek',
                }
            },
            'save_changes_button': {
                'value': {
                    'ru': 'Сохранить изменения',
                    'en': 'Save changes',
                    'tr': 'değişiklikleri kaydetmek',
                }
            }
        }
        with patch.object(SurveyText, 'get_default_texts_for_survey_type', Mock(return_value=texts)):
            survey = SurveyFactory()
            msg = 'должено было создаться два текста при создании сущности опроса'
            self.assertEqual(survey.texts.count(), 2, msg=msg)
            for key, item in list(texts.items()):
                created_text = SurveyText.objects.get(slug=key)
                self.assertEqual(created_text.slug, key)
                self.assertEqual(created_text.value, item['value']['ru'])
                self.assertEqual(created_text.null, False)
                self.assertEqual(created_text.max_length, None)

    def test_should_not_try_to_create_texts_if_saving_existing_survey_instance(self):
        texts = {
            'submit_button': {
                'value': {
                    'ru': 'Отправить',
                    'en': 'Submit',
                    'tr': 'göndermek',
                }
            }
        }
        with patch.object(SurveyText, 'get_default_texts_for_survey_type', Mock(return_value=texts)):
            survey = SurveyFactory()
            survey.save()  # BANG!
            self.assertEqual(survey.texts.count(), 1)

            class ShouldNotCallMe(Exception):
                pass

            with patch.object(SurveyText.objects, 'get_or_create', Mock(side_effect=ShouldNotCallMe)):
                try:
                    survey.save()
                except ShouldNotCallMe:
                    self.fail('Если инстанс survey сохраняется, а не создается, то не должна выполняться попытка '
                              'создания текстов для опроса')

    def test_thread_safety__for_not_unique_index(self):
        texts = {
            'submit_button': {
                'value': {
                    'ru': 'Отправить',
                    'en': 'Submit',
                    'tr': 'göndermek',
                }
            }
        }
        with patch.object(SurveyText, 'get_default_texts_for_survey_type', Mock(return_value=texts)):
            with patch.object(SurveyText.objects, 'get_or_create', Mock(side_effect=IntegrityError)):
                try:
                    SurveyFactory()
                except IntegrityError:
                    pass
                else:
                    self.fail('Если произошла ошибка БД при создании текста, но эта ошибка не проверки уникальности, '
                              'эту ошибку нельзя подавлять')

    def test_thread_safety__unique_index(self):
        texts = {
            'submit_button': {
                'value': {
                    'ru': 'Отправить',
                    'en': 'Submit',
                    'tr': 'göndermek',
                }
            }
        }
        exc = IntegrityError(
            1062,
            "Duplicate entry '1-hello' for key 'surveyme_surveytext_survey_id_7654a1549b1373ab_uniq'"
        )
        with patch.object(SurveyText, 'get_default_texts_for_survey_type', Mock(return_value=texts)):
            with patch.object(SurveyText.objects, 'get_or_create', Mock(side_effect=exc)):
                try:
                    SurveyFactory()
                except IntegrityError:
                    self.fail('Если произошла ошибка проверки уникальности при создании текста, '
                              'то ее нужно подавить. Это thread safety')


class TestSurveyBehaviour__default_agreements_on_creation(TestCase):
    fixtures = ['initial_data.json']

    def test_should_not_bind_agreements_for_created_simple_form_survey(self):
        survey = Survey.objects.create(name='test_survey')
        self.assertEqual(list(survey.agreements.all()), [])


class TestSurveyBehaviour__default_states_on_creation(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.content_type_attribute = ContentTypeAttribute.objects.create(
            content_type=ContentType.objects.get_for_model(User),
            attr='user.groups'
        )
        ContentTypeAttribute.objects.create(
            content_type=ContentType.objects.get_for_model(User),
            attr='user'
        )

    def test_should_not_create_any_recommendations_for_other_forms(self):
        survey = Survey.objects.create(name='test_survey')
        self.assertEqual(list(survey.surveystateconditionnode_set.all()), [])

    def test_should_not_create_recommendations_if_not_content_type_attr(self):
        self.content_type_attribute.delete()
        survey = Survey.objects.create(name='test_survey')
        self.assertEqual(list(survey.surveystateconditionnode_set.all()), [])


class TestSurveyBehaviour__default_questions_on_creation(TestCase):
    fixtures = ['initial_data.json']

    def test_should_not_create_questions_for_simple_form(self):
        survey = Survey.objects.create(name='test_survey')
        self.assertEqual(survey.surveyquestion_set.all().count(), 0)


class TestSurvey___get_submit_conditions(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()
        answer_choices_type = AnswerType.objects.get(slug='answer_choices')
        self.questions = [
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=answer_choices_type
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=answer_choices_type
            ),
        ]
        # create question choices for first question
        SurveyQuestionChoiceFactory(survey_question=self.questions[0], label='1')
        SurveyQuestionChoiceFactory(survey_question=self.questions[0], label='2')

        # create question choices for second question
        SurveyQuestionChoiceFactory(survey_question=self.questions[1], label='3')
        SurveyQuestionChoiceFactory(survey_question=self.questions[1], label='4')

    def test_get_submit_conditions_without_conditions_should_return_empty_list(self):
        msg = 'Если нет условий - get_submit_conditions должен вернуть пустой список'
        self.assertEqual(self.survey.get_submit_conditions(), [], msg=msg)

    def test_get_submit_conditions_should_return_list_with_conditions(self):
        node_1 = SurveySubmitConditionNodeFactory(survey=self.survey)
        node_2 = SurveySubmitConditionNodeFactory(survey=self.survey)
        content_type_attribute = ContentTypeAttributeFactory()
        SurveySubmitConditionNodeItemFactory(
            position=1,
            survey_submit_condition_node=node_1,
            operator='and',
            condition='eq',
            survey_question=self.questions[0],
            content_type_attribute=content_type_attribute,
            value=self.questions[0].surveyquestionchoice_set.all()[0],
        )
        SurveySubmitConditionNodeItemFactory(
            position=2,
            survey_submit_condition_node=node_1,
            operator='or',
            condition='neq',
            survey_question=self.questions[1],
            content_type_attribute=content_type_attribute,
            value=self.questions[0].surveyquestionchoice_set.all()[1],
        )
        SurveySubmitConditionNodeItemFactory(
            position=3,
            survey_submit_condition_node=node_2,
            operator='and',
            condition='eq',
            survey_question=self.questions[1],
            content_type_attribute=content_type_attribute,
            value=self.questions[0].surveyquestionchoice_set.all()[0],
        )

        exp_conditions = [
            [
                {
                    'condition': 'eq',
                    'field': self.questions[0].param_slug,
                    'field_value': '1',
                    'operator': 'and'
                },
                {
                    'condition': 'neq',
                    'field': self.questions[1].param_slug,
                    'field_value': '2',
                    'operator': 'or'
                }
            ],
            [
                {
                    'condition': 'eq',
                    'field': self.questions[1].param_slug,
                    'field_value': '1',
                    'operator': 'and'
                }
            ]
        ]

        msg = 'Если нет условий - get_submit_conditions должен вернуть список с условиями'
        self.assertListEqual(self.survey.get_submit_conditions(), exp_conditions, msg=msg)


class TestSurvey___hashed_id_field_existance(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.survey_wo_hash = SurveyFactory(pk=1)
        self.survey_w_hash = SurveyFactory(pk=100000)
        self.profile = self.client.login_yandex(is_superuser=True)

    @override_settings(APP_TYPE='forms_ext_admin', SURVEY_HASH_START_ID=10615)
    def test_existence_and_value_of_hashed_id_in_ext(self):
        # without hash
        response = self.client.get('/admin/api/v2/surveys/{id}/'.format(id=self.survey_wo_hash.pk))
        self.assertEqual(response.status_code, 200)
        self.assertIsNone(response.data['hashed_id'])

        # with hash
        response = self.client.get('/admin/api/v2/surveys/{id}/'.format(id=self.survey_w_hash.pk))
        self.assertEqual(response.status_code, 200)
        self.assertIsNotNone(response.data['hashed_id'])

        # add '?detailed=0'
        response = self.client.get('/admin/api/v2/surveys/{id}/?detailed=0'.format(id=self.survey_w_hash.pk))
        self.assertEqual(response.status_code, 200)
        self.assertIsNotNone(response.data['hashed_id'])

        # check hash value
        encoded_id = str(self.survey_w_hash.pk).encode()
        h = hmac.new(settings.SURVEY_HASH_SECRET.encode(), encoded_id, hashlib.sha1)
        self.assertEqual(response.data['hashed_id'], '{}.{}'.format(self.survey_w_hash.pk, h.hexdigest()))

    @override_settings(APP_TYPE='forms_biz', SURVEY_HASH_START_ID=10615)
    def test_hashed_id_is_null_in_noext(self):
        # without hash
        response = self.client.get('/admin/api/v2/surveys/{id}/'.format(id=self.survey_wo_hash.pk))
        self.assertEqual(response.status_code, 200)
        self.assertIsNone(response.data['hashed_id'])

        # still without hash
        response = self.client.get('/admin/api/v2/surveys/{id}/'.format(id=self.survey_w_hash.pk))
        self.assertEqual(response.status_code, 200)
        self.assertIsNone(response.data['hashed_id'])


class TestSurvey__total_scores(TestCase):
    fixtures = ['initial_data.json']

    def test_calc_total_scores(self):
        survey = SurveyFactory()
        SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
        )
        SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            param_quiz={
                'enabled': False,
                'answers': [
                    {
                        'correct': True,
                        'scores': 1,
                        'value': 'first',
                    },
                    {
                        'correct': True,
                        'scores': 2,
                        'value': 'second',
                    },
                ],
            },
        )
        SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_is_allow_multiple_choice=False,
            param_quiz={
                'enabled': True,
                'answers': [
                    {
                        'correct': True,
                        'scores': 3,
                        'value': 'three',
                    },
                    {
                        'correct': True,
                        'scores': 4,
                        'value': 'four',
                    },
                    {
                        'correct': False,
                        'scores': 10,
                        'value': 'incorrect',
                    },
                ],
            },
        )
        SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_is_allow_multiple_choice=True,
            param_quiz={
                'enabled': True,
                'answers': [
                    {
                        'correct': True,
                        'scores': 5,
                        'value': 'five',
                    },
                    {
                        'correct': True,
                        'scores': 6,
                        'value': 'six',
                    },
                ],
            },
        )
        SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            param_quiz={
                'enabled': True,
                'answers': [
                    {
                        'correct': True,
                        'scores': 1,
                        'value': 'one',
                    },
                    {
                        'correct': True,
                        'scores': 2,
                        'value': 'two',
                    },
                ],
            },
        )
        self.assertEqual(survey.total_scores, 4 + (5 + 6) + 2)
        self.assertEqual(survey.quiz_question_count, 3)

    def test_should_return_zero_if_three_is_not_correct_answers(self):
        survey = SurveyFactory()
        SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            param_quiz={
                'enabled': True,
                'answers': [
                    {
                        'correct': False,
                        'scores': 1,
                        'value': 'first',
                    },
                    {
                        'correct': False,
                        'scores': 2,
                        'value': 'second',
                    },
                ],
            },
        )
        SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_quiz={
                'enabled': True,
                'answers': [
                    {
                        'correct': False,
                        'scores': 3,
                        'value': 'three',
                    },
                    {
                        'correct': False,
                        'scores': 4,
                        'value': 'four',
                    },
                    {
                        'correct': False,
                        'scores': 10,
                        'value': 'incorrect',
                    },
                ],
            },
        )
        self.assertEqual(survey.total_scores, 0)
        self.assertEqual(survey.quiz_question_count, 2)
