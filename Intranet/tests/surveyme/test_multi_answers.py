# -*- coding: utf-8 -*-
import json

from django.test import TestCase, override_settings

from events.accounts.helpers import YandexClient
from events.surveyme.factories import (
    SurveyFactory,
    SurveyQuestionFactory,
)
from events.surveyme.models import (
    AnswerType,
    ProfileSurveyAnswer,
    Survey,
)


def get_answered_questions(answer_id, question, **kwargs):
    survey_answer = ProfileSurveyAnswer.objects.get(pk=answer_id)
    answer = survey_answer.as_dict()
    return answer.get(question.pk, {}).get('value')


def get_profiles(survey):
    return (
        ProfileSurveyAnswer.objects.
        filter(survey=survey).distinct().
        values_list('user__pk', flat=True)
    )


class TestAnonymousUserMultiAnswers(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory(
            is_published_external=True,
            is_allow_multiple_answers=True,
        )
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
        )
        self.url = '/v1/surveys/{survey}/form/'.format(survey=self.survey.pk)

    def test_submit_form(self):
        # проверяем, что для незалогиненного пользователя
        # каждый раз создается новый профиль с ответом
        data = {
            self.question.param_slug: 'test one',
        }
        response = self.client.post(self.url, data)
        self.assertEqual(200, response.status_code)

        first_answer_id = response.data['answer_id']
        answered_questions = get_answered_questions(first_answer_id, self.question)
        self.assertIsNotNone(answered_questions)
        self.assertEqual('test one', answered_questions)
        self.assertEqual(1, len(get_profiles(self.survey)))

        data = {
            self.question.param_slug: 'test two',
        }
        response = self.client.post(self.url, data)
        self.assertEqual(200, response.status_code)

        second_answer_id = response.data['answer_id']
        answered_questions = get_answered_questions(second_answer_id, self.question)
        self.assertIsNotNone(answered_questions)
        self.assertEqual('test two', answered_questions)
        self.assertEqual(2, len(get_profiles(self.survey)))
        self.assertNotEqual(first_answer_id, second_answer_id)

        data = {
            self.question.param_slug: 'test three',
        }
        response = self.client.post(self.url, data)
        self.assertEqual(200, response.status_code)

        third_answer_id = response.data['answer_id']
        answered_questions = get_answered_questions(third_answer_id, self.question)
        self.assertIsNotNone(answered_questions)
        self.assertEqual('test three', answered_questions)
        self.assertEqual(3, len(get_profiles(self.survey)))
        self.assertNotEqual(second_answer_id, third_answer_id)


@override_settings(IS_ENABLE_CAPTCHA=False)
class TestLoggedUserMultiAnswers(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex()
        self.survey = SurveyFactory(is_published_external=True)
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
        )
        self.url = '/v1/surveys/{survey}/form/'.format(survey=self.survey.pk)

    def test_submit_form_should_not_allow_multiple_answers(self):
        # проверяем, что ответ можно сохранить только один раз
        self.survey.is_allow_multiple_answers = False
        self.survey.is_allow_answer_editing = False
        self.survey.save()
        self.assertFalse(self.survey.is_allow_multiple_answers)
        self.assertFalse(self.survey.is_allow_answer_editing)

        data = {
            self.question.param_slug: 'test one',
        }
        response = self.client.post(self.url, data)
        self.assertEqual(200, response.status_code)

        answered_questions = get_answered_questions(response.data['answer_id'], self.question)
        self.assertIsNotNone(answered_questions)
        self.assertEqual('test one', answered_questions)

        data = {
            self.question.param_slug: 'test two',
        }
        response = self.client.post(self.url, data)
        self.assertEqual(403, response.status_code)

    def test_submit_form_should_add_new_answer(self):
        self.survey.is_allow_multiple_answers = True
        self.survey.is_allow_answer_editing = True
        self.survey.is_allow_answer_versioning = True
        self.survey.save()
        self.assertTrue(self.survey.is_allow_multiple_answers)
        self.assertTrue(self.survey.is_allow_answer_editing)
        self.assertTrue(self.survey.is_allow_answer_versioning)

        self.another_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_date'),
        )

        data = {
            self.question.param_slug: 'test one',
            self.another_question.param_slug: ['2018-10-08'],
        }
        response = self.client.post(self.url, data)
        self.assertEqual(200, response.status_code)

        first_answer_id = response.data['answer_id']
        answered_questions = get_answered_questions(first_answer_id, self.question)
        self.assertIsNotNone(answered_questions)
        self.assertEqual('test one', answered_questions)

        answered_questions = get_answered_questions(first_answer_id, self.another_question)
        self.assertIsNotNone(answered_questions)
        self.assertEqual('2018-10-08', answered_questions)

        response = self.client.get(self.url)
        self.assertEqual(200, response.status_code)
        data = response.data
        self.assertEqual(
            data['fields'][self.question.param_slug]['tags'][0]['attrs']['value'],
            'test one',
        )
        self.assertEqual(
            data['fields'][self.another_question.param_slug]['tags'][0]['attrs']['value'],
            '2018-10-08',
        )

        data = {
            self.question.param_slug: 'test two',
            self.another_question.param_slug: ['2018-10-10'],
        }
        response = self.client.post(self.url, data)
        self.assertEqual(200, response.status_code)

        second_answer_id = response.data['answer_id']
        answered_questions = get_answered_questions(second_answer_id, self.question)
        self.assertIsNotNone(answered_questions)
        self.assertEqual('test two', answered_questions)

        answered_questions = get_answered_questions(second_answer_id, self.another_question)
        self.assertIsNotNone(answered_questions)
        self.assertEqual('2018-10-10', answered_questions)

        self.assertNotEqual(first_answer_id, second_answer_id)

    def test_submit_form_should_allow_answer_editing(self):
        # проверяем, что можно отредактировать сохраненный ответ
        self.survey.is_allow_multiple_answers = False
        self.survey.is_allow_answer_editing = True
        self.survey.save()
        self.assertFalse(self.survey.is_allow_multiple_answers)
        self.assertTrue(self.survey.is_allow_answer_editing)

        data = {
            self.question.param_slug: 'test one',
        }
        response = self.client.post(self.url, data)
        self.assertEqual(200, response.status_code)

        answered_questions = get_answered_questions(response.data['answer_id'], self.question)
        self.assertIsNotNone(answered_questions)
        self.assertEqual('test one', answered_questions)

        data = {
            self.question.param_slug: 'test two',
        }
        response = self.client.post(self.url, data)
        self.assertEqual(200, response.status_code)

        answered_questions = get_answered_questions(response.data['answer_id'], self.question)
        self.assertIsNotNone(answered_questions)
        self.assertEqual('test two', answered_questions)

    def test_submit_form_should_allow_multiple_answers(self):
        # проверяем, что отметы можно сабмитить несколько раз
        self.survey.is_allow_multiple_answers = True
        self.survey.is_allow_answer_editing = False
        self.survey.save()
        self.assertTrue(self.survey.is_allow_multiple_answers)
        self.assertFalse(self.survey.is_allow_answer_editing)

        data = {
            self.question.param_slug: 'test one',
        }
        response = self.client.post(self.url, data)
        self.assertEqual(200, response.status_code)

        first_answer_id = response.data['answer_id']
        answered_questions = get_answered_questions(first_answer_id, self.question)
        self.assertIsNotNone(answered_questions)
        self.assertEqual('test one', answered_questions)

        data = {
            self.question.param_slug: 'test two',
        }
        response = self.client.post(self.url, data)
        self.assertEqual(200, response.status_code)

        second_answer_id = response.data['answer_id']
        answered_questions = get_answered_questions(second_answer_id, self.question)
        self.assertIsNotNone(answered_questions)
        self.assertEqual('test two', answered_questions)

        self.assertNotEqual(first_answer_id, second_answer_id)

    def test_submit_form_should_allow_multiple_answers_complex_scenario(self):
        # включаем поддержку множественных ответов и отключаем редактирование
        self.survey.is_allow_multiple_answers = True
        self.survey.is_allow_answer_editing = False
        self.survey.save()
        self.assertTrue(self.survey.is_allow_multiple_answers)
        self.assertFalse(self.survey.is_allow_answer_editing)

        # делаем сабмит и проверяем, что он успешно записался
        data = {
            self.question.param_slug: 'test one',
        }
        response = self.client.post(self.url, data)
        self.assertEqual(200, response.status_code)

        first_answer_id = response.data['answer_id']
        answered_questions = get_answered_questions(first_answer_id, self.question)
        self.assertIsNotNone(answered_questions)
        self.assertEqual('test one', answered_questions)

        # делаем еще один сабмит и проверяем, что у нас сохранено два ответа
        data = {
            self.question.param_slug: 'test two',
        }
        response = self.client.post(self.url, data)
        self.assertEqual(200, response.status_code)

        second_answer_id = response.data['answer_id']
        answered_questions = get_answered_questions(second_answer_id, self.question)
        self.assertIsNotNone(answered_questions)
        self.assertEqual('test two', answered_questions)
        self.assertNotEqual(first_answer_id, second_answer_id)

        # отключаем множественные ответы и проверяем, что сабмит ответа запрещен
        self.survey.is_allow_multiple_answers = False
        self.survey.save()
        self.assertFalse(self.survey.is_allow_multiple_answers)

        data = {
            self.question.param_slug: 'test three',
        }
        response = self.client.post(self.url, data)
        self.assertEqual(403, response.status_code)

        # включаем редактирование ответов и проверяем, что последний
        # сохраненный ответ перезаписался с новыми данными
        self.survey.is_allow_answer_editing = True
        self.survey.save()
        self.assertTrue(self.survey.is_allow_answer_editing)

        data = {
            self.question.param_slug: 'test four',
        }
        response = self.client.post(self.url, data)
        self.assertEqual(200, response.status_code)

        third_answer_id = response.data['answer_id']
        answered_questions = get_answered_questions(third_answer_id, self.question)
        self.assertIsNotNone(answered_questions)
        self.assertEqual('test four', answered_questions)
        self.assertEqual(second_answer_id, third_answer_id)

        # включаем множественные ответы и проверяем, что ответ добавился
        self.survey.is_allow_multiple_answers = True
        self.survey.save()
        self.assertTrue(self.survey.is_allow_multiple_answers)

        data = {
            self.question.param_slug: 'test five',
        }
        response = self.client.post(self.url, data)
        self.assertEqual(200, response.status_code)

        fourth_answer_id = response.data['answer_id']
        answered_questions = get_answered_questions(fourth_answer_id, self.question)
        self.assertIsNotNone(answered_questions)
        self.assertEqual('test five', answered_questions)
        self.assertNotEqual(third_answer_id, fourth_answer_id)


class TestCreateForm(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex(is_superuser=True)

    def test_create_simple_form(self):
        # обычная форма должна создаться с установленным свойством is_allow_multiple_answers
        data = {
            'name': 'testform',
        }
        headers = {
            'content_type': 'application/json',
        }
        response = self.client.post('/admin/api/v2/surveys/', json.dumps(data), **headers)
        self.assertEqual(201, response.status_code)
        survey = Survey.objects.get(pk=response.json()['id'])
        self.assertTrue(survey.is_allow_multiple_answers)

        # для обычной формы можно сбрасывать флаг is_allow_multiple_answers
        data = {
            'is_allow_multiple_answers': False,
        }
        response = self.client.patch('/admin/api/v2/surveys/%s/' % survey.pk, json.dumps(data), **headers)
        self.assertEqual(200, response.status_code)
        survey = Survey.objects.get(pk=survey.pk)
        self.assertFalse(survey.is_allow_multiple_answers)

        # для обычной формы можно устанавливать флаг is_allow_multiple_answers
        data = {
            'is_allow_multiple_answers': True,
        }
        response = self.client.patch('/admin/api/v2/surveys/%s/' % survey.pk, json.dumps(data), **headers)
        self.assertEqual(200, response.status_code)
        survey = Survey.objects.get(pk=survey.pk)
        self.assertTrue(survey.is_allow_multiple_answers)
