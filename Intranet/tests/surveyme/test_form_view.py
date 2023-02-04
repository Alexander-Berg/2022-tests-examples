# -*- coding: utf-8 -*-
import responses

from copy import deepcopy
from django.test import TestCase, override_settings
from unittest.mock import patch, Mock

from events.accounts.helpers import YandexClient
from events.accounts.factories import UserFactory
from events.captcha.captcha import Captcha
from events.surveyme.factories import (
    SurveyFactory,
    SurveyQuestionFactory,
    SurveyQuestionChoiceFactory,
    SurveyQuestionMatrixTitleFactory,
)
from events.surveyme.models import (
    AnswerType,
    ProfileSurveyAnswer,
    SurveyTicket,
    Survey,
)


class TestSurveyFormViewMixin:
    client_class = YandexClient

    def setUp(self):
        self.survey = SurveyFactory(
            is_allow_answer_editing=True,
            is_published_external=True,
        )
        self.survey_question = SurveyQuestionFactory(
            survey=self.survey,
            label='Name',
            answer_type=AnswerType.objects.get(slug='answer_name'),
        )
        self.email_question = SurveyQuestionFactory(
            survey=self.survey,
            label='Email',
            answer_type=AnswerType.objects.get(slug='answer_non_profile_email'),
        )
        self.user = UserFactory(email='akhmetov@yandex-team.ru')
        self.data = {
            self.email_question.param_slug: 'akhmetov@yandex-team.ru',
            self.survey_question.param_slug: 'Alexander',
            'is_agree_with_hr': 'on',
            'is_agree_with_events': 'on',
        }


@override_settings(IS_ENABLE_CAPTCHA=True)
class TestSurveyFormView__captcha(TestSurveyFormViewMixin, TestCase):
    fixtures = ['initial_data.json']

    def register_uri(self):
        responses.add(
            responses.GET,
            'http://api.captcha.yandex.net/generate',
            body='''<?xml version="1.0"?>
<number url='https://ext.captcha.yandex.net/image?key=12345'>12345</number>
            ''',
            content_type='text/xml',
        )

    def do_get_request(self, ip=None):
        ip = ip or '127.0.0.1'
        return self.client.get(
            f'/v1/surveys/{self.survey.pk}/form/',
            REMOTE_ADDR=ip,
            HTTP_X_FORWARDED_FOR=ip,
        )

    def do_post_request(self, ip=None, update_data=None):
        ip = ip or '127.0.0.1'
        data = deepcopy(self.data)
        if update_data:
            data.update(update_data)
        return self.client.post(
            f'/v1/surveys/{self.survey.pk}/form/',
            data=data,
            format='json',
            REMOTE_ADDR=ip,
            HTTP_X_FORWARDED_FOR=ip,
        )

    @responses.activate
    def test_should_not_use_captcha_by_default_on_get_request(self):
        self.register_uri()
        response = self.do_get_request(ip='10.1.0.1')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(responses.calls), 0)
        self.assertTrue('captcha' not in response.data['fields'])

    @responses.activate
    def test_should_use_captcha_by_default_if_it_is_enabled_in_survey(self):
        self.register_uri()
        self.survey.captcha_display_mode = 'auto'
        self.survey.save()

        response = self.do_get_request(ip='10.1.1.1')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(responses.calls), 0)
        self.assertTrue('captcha' not in response.data['fields'])

        self.survey.captcha_display_mode = 'always'
        self.survey.save()

        response = self.do_get_request(ip='10.1.1.1')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(responses.calls), 1)
        self.assertTrue('captcha' in response.data['fields'])

    @responses.activate
    def test_should_use_captcha_by_default_if_it_is_enabled_in_survey_not_created(self):
        self.register_uri()
        self.client.login_yandex(uid='11591999')

        self.survey.captcha_display_mode = 'auto'
        self.survey.save()

        response = self.do_get_request(ip='10.1.2.1')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(responses.calls), 0)
        self.assertTrue('captcha' not in response.data['fields'])

        self.survey.captcha_display_mode = 'always'
        self.survey.save()

        response = self.do_get_request(ip='10.1.2.1')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(responses.calls), 1)
        self.assertTrue('captcha' in response.data['fields'])

    @responses.activate
    def test_should_not_use_captcha_on_first_post_request(self):
        self.register_uri()
        response = self.do_post_request(ip='10.1.3.1')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(responses.calls), 0)

    @responses.activate
    def test_should_show_captcha_for_same_ip_address__if_it_successfully_posted_3_times_in_a_minute(self):
        self.register_uri()
        for i in range(8):
            response = self.do_post_request(ip='10.1.4.1')
            self.assertEqual(response.status_code, 200)
        response = self.do_post_request(ip='10.1.4.1')
        self.assertEqual(response.status_code, 400)
        self.assertTrue('captcha' in response.data['fields'])

    @responses.activate
    def test_should_show_captcha_for_same_user__if_it_successfully_posted_3_times_in_a_minute(self):
        self.register_uri()
        self.client.login_yandex()
        for i in range(8):
            response = self.do_post_request(ip=f'10.1.5.{i}')  # do not look at different ips
            self.assertEqual(response.status_code, 200)
        response = self.do_post_request(ip='10.1.5.7')
        self.assertEqual(response.status_code, 400)
        self.assertTrue('captcha' in response.data['fields'])

    @responses.activate
    def test_should_show_captcha_for_same_ip_address__not_for_other_ip_address(self):
        self.register_uri()
        for i in range(3):
            response = self.do_post_request(ip='10.1.6.1')
            self.assertEqual(response.status_code, 200)
        response = self.do_post_request(ip='10.1.6.2')
        self.assertEqual(response.status_code, 200)

    @responses.activate
    def test_should_show_captcha_if_survey_captcha_display_mode_equals_always(self):
        self.register_uri()
        self.survey.captcha_display_mode = 'auto'
        self.survey.save()
        response = self.do_post_request(ip='10.1.7.1')
        msg = 'Если режим показа капчи == `auto`, а запросов еще не было - капчу не нужно показывать'
        self.assertEqual(response.status_code, 200, msg=msg)

        self.survey.captcha_display_mode = 'always'
        self.survey.save()
        response = self.do_post_request(ip='10.1.7.1')
        msg = 'Если режим показа капчи == `always` - капчу нужно показывать всегда'
        self.assertEqual(response.status_code, 400, msg=msg)
        self.assertTrue('captcha' in response.data['fields'])

    @responses.activate
    @patch.object(Captcha, 'generate', Mock(return_value=('http://img.yandex.ru/code/', 'code')))
    @patch.object(Captcha, 'check', Mock(return_value=(True, 200)))
    def test_should_not_use_captcha_after_submission_form_with_captcha_till_the_next_3_fast_posts(self):
        self.register_uri()
        for i in range(8):
            response = self.do_post_request(ip='10.1.8.1')
            self.assertEqual(response.status_code, 200)
        response = self.do_post_request(ip='10.1.8.1')
        self.assertEqual(response.status_code, 400)
        self.assertTrue('captcha' in response.data['fields'])

        response = self.do_post_request(  # BANG!
            ip='10.1.8.1',
            update_data={
                'captcha_0': 'any',
                'captcha_1': 'any',
            },
        )
        self.assertEqual(response.status_code, 200)

        for i in range(8):
            response = self.do_post_request(ip='10.1.8.1')
            self.assertEqual(response.status_code, 200)
        response = self.do_post_request(ip='10.1.8.1')
        self.assertEqual(response.status_code, 400)
        self.assertTrue('captcha' in response.data['fields'])

    @responses.activate
    def test_should_not_show_captcha_if_posts_are_invalid(self):
        self.register_uri()
        for i in range(5):
            response = self.do_post_request(
                ip='10.1.10.1',
                update_data={
                    self.email_question.param_slug: None,
                },
            )
            self.assertEqual(response.status_code, 400)
            self.assertFalse('captcha' in response.data['fields'])


class TestSurveyFormView__maximum_answers_behavior(TestSurveyFormViewMixin, TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        super().setUp()
        self.survey = SurveyFactory(is_published_external=True)
        self.survey_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_name'),
        )
        self.data = {
            self.survey_question.param_slug: 'Alexander',
        }

    def test_must_close_survey_if_all_tickets_acquired(self):
        SurveyTicket.objects.all().delete()
        self.survey.maximum_answers_count = 1
        self.survey.save()

        response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/', self.data)
        self.assertEqual(response.status_code, 200)

        fresh_survey = Survey.objects.get(pk=self.survey.pk)
        msg = 'Так как закончились свободные билеты, регистрация должна быть закрыта'
        self.assertFalse(fresh_survey.is_published_external, msg=msg)

        response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/', self.data)
        self.assertEqual(response.status_code, 403, msg=msg)

    def test_must_not_increment_answers_if_profiles_survey_answer_is_edited(self):
        self.survey.maximum_answers_count = 2
        self.survey.is_allow_answer_editing = True
        self.survey.save()

        self.client.login_yandex()
        response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/', self.data)
        self.assertEqual(response.status_code, 200)

        response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/', self.data)
        msg = 'Регистрация должна быть открыта'
        self.assertEqual(response.status_code, 200, msg=msg)
        fresh_survey = Survey.objects.get(pk=self.survey.pk)
        self.assertTrue(fresh_survey.is_published_external, msg=msg)


@patch.object(Captcha, 'generate', Mock(return_value=('http://img.yandex.ru/code/', 'code')))
@patch.object(Captcha, 'check', Mock(return_value=(True, 200)))
class TestSurveyFormViewBehaviour__answer_matrix(TestSurveyFormViewMixin, TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        super().setUp()
        self.survey = SurveyFactory(is_published_external=True)
        self.matrix_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_widget='matrix',
            param_data_source='survey_question_matrix_choice',
        )
        self.columns = [
            SurveyQuestionMatrixTitleFactory(
                type='column', label='first_column', position=1,
                survey_question=self.matrix_question,
            ),
            SurveyQuestionMatrixTitleFactory(
                type='column', label='second_column', position=2,
                survey_question=self.matrix_question,
            ),
        ]
        self.rows = [
            SurveyQuestionMatrixTitleFactory(
                type='row', label='first_row', position=1,
                survey_question=self.matrix_question,
            ),
            SurveyQuestionMatrixTitleFactory(
                type='row', label='second_row', position=2,
                survey_question=self.matrix_question,
            ),
        ]

    def test_must_set_widget_param_and_rows_with_columns(self):
        response = self.client.get(f'/v1/surveys/{self.survey.pk}/form/')
        field = response.data['fields'][self.matrix_question.get_form_field_name()]

        self.assertEqual(field['widget'], 'matrix', msg='Неправильный тип виджета')

        exp_data_source_items = [
            {'text': self.columns[0].label, 'type': 'column', 'id': str(self.columns[0].pk), 'position': self.columns[0].position},
            {'text': self.rows[0].label, 'type': 'row', 'id': str(self.rows[0].pk), 'position': self.rows[0].position},
            {'text': self.columns[1].label, 'type': 'column', 'id': str(self.columns[1].pk), 'position': self.columns[1].position},
            {'text': self.rows[1].label, 'type': 'row', 'id': str(self.rows[1].pk), 'position': self.rows[1].position},
        ]
        self.assertEqual(field['data_source']['items'], exp_data_source_items)

    def test_param_is_required_behaviour(self):
        self.matrix_question.param_is_required = True
        self.matrix_question.save()
        empty_data = {
            self.matrix_question.get_form_field_name(): '',
            'captcha_0': 'any',
            'captcha_1': 'any',
        }
        response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/', empty_data)

        msg = 'Если поле обязательно, а значение не передано - нужно вернуть 400 и ошибку'
        self.assertEqual(response.status_code, 400, msg=msg)
        self.assertEqual(
            ['Необходимое поле'],
            response.data['fields'][self.matrix_question.get_form_field_name()]['errors'],
            msg=msg,
        )

        self.matrix_question.param_is_required = False
        self.matrix_question.save()
        response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/', empty_data)

        msg = 'Если поле не обязательно, а значение не передано - нужно вернуть 200'
        self.assertEqual(response.status_code, 200, msg=msg)

    def test_submit_data(self):
        experiments = [
            {'value': '12', 'status': 400},
            {'value': '34', 'status': 400},
            {'value': '', 'status': 400},
            {'value': 'invalid', 'status': 400},
            {'value': '%s_%s' % (self.rows[0].pk, self.columns[0].pk), 'status': 200},
            {'value': '%s_%s' % (self.rows[0].pk, self.columns[1].pk), 'status': 200},
            {'value': '%s_%s' % (self.rows[1].pk, self.columns[0].pk), 'status': 200},
            {'value': '%s_%s' % (self.rows[1].pk, self.columns[1].pk), 'status': 200},
        ]

        for experiment in experiments:
            data = {
                self.matrix_question.get_form_field_name(): experiment['value'],
                'captcha_0': 'any',
                'captcha_1': 'any',
            }

            response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/', data)
            self.assertEqual(response.status_code, experiment['status'])

            if experiment['status'] == 200:
                answer = ProfileSurveyAnswer.objects.get(pk=response.data['answer_id']).as_dict()
                msg = 'В ответ было записано неправильное значение'
                question_answer = answer.get(self.matrix_question.pk)
                self.assertIsNotNone(question_answer)
                question_value = question_answer.get('value')
                result = '%s_%s' % (
                    question_value[0].get('row', {}).get('key'),
                    question_value[0].get('col', {}).get('key'),
                )
                self.assertEqual(result, experiment['value'], msg=msg)

    def test_check_number_of_queries__get_form(self):
        url = f'/v1/surveys/{self.survey.pk}/form/'
        with self.assertNumQueries(8):
            response = self.client.get(url)
        self.assertEqual(response.status_code, 200)

    def test_check_number_of_queries__post_form(self):
        url = f'/v1/surveys/{self.survey.pk}/form/'
        field_name = self.matrix_question.param_slug
        field_value = '%s_%s' % (self.rows[0].pk, self.columns[1].pk)
        data = {
            field_name: field_value,
        }
        with self.assertNumQueries(20):
            response = self.client.post(url, data)
        self.assertEqual(response.status_code, 200)


@patch.object(Captcha, 'generate', Mock(return_value=('http://img.yandex.ru/code/', 'code')))
@patch.object(Captcha, 'check', Mock(return_value=(True, 200)))
class TestSurveyFormViewBehaviour__answer_choice(TestSurveyFormViewMixin, TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        super().setUp()
        self.survey = SurveyFactory(is_published_external=True)
        self.choice_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='survey_question_choice',
        )
        self.choices = [
            SurveyQuestionChoiceFactory(
                label='first',
                position=1,
                survey_question=self.choice_question,
            ),
            SurveyQuestionChoiceFactory(
                label='second',
                position=2,
                survey_question=self.choice_question,
            ),
        ]

    def test_check_number_of_queries__get_form(self):
        url = f'/v1/surveys/{self.survey.pk}/form/'
        with self.assertNumQueries(8):
            response = self.client.get(url)
        self.assertEqual(response.status_code, 200)

    def test_check_number_of_queries__post_form(self):
        url = f'/v1/surveys/{self.survey.pk}/form/'
        field_name = self.choice_question.param_slug
        field_value = str(self.choices[1].pk)
        data = {
            field_name: field_value,
        }
        with self.assertNumQueries(20):
            response = self.client.post(url, data)
        self.assertEqual(response.status_code, 200)


class TestSimpleDateField(TestSurveyFormViewMixin, TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        super().setUp()
        self.survey = SurveyFactory(is_published_external=True)
        self.survey_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_date'),
            param_date_field_type='date',
        )
        self.simple_date_data = {
            self.survey_question.get_form_field_name(): '2014-04-04',
            'is_agree_with_hr': 'on',
            'is_agree_with_events': 'on',
        }

    def test_simple_date_answer_1(self):
        self.survey_question.param_is_required = True
        self.survey_question.save()

        response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/', data=self.simple_date_data)
        self.assertEqual(response.status_code, 200)

        profile_survey_answer = ProfileSurveyAnswer.objects.get(pk=response.data['answer_id'])
        answer = profile_survey_answer.as_dict()
        question_answer = answer.get(self.survey_question.pk)
        self.assertIsNotNone(question_answer)
        question_value = question_answer.get('value')
        expected = self.simple_date_data[self.survey_question.get_form_field_name()]
        self.assertEqual(question_value, expected)

    def test_simple_date_answer_2(self):
        self.survey_question.param_is_required = False
        self.survey_question.save()

        response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/', data=self.simple_date_data)
        self.assertEqual(response.status_code, 200)

        profile_survey_answer = ProfileSurveyAnswer.objects.get(pk=response.data['answer_id'])
        answer = profile_survey_answer.as_dict()
        question_answer = answer.get(self.survey_question.pk)
        self.assertIsNotNone(question_answer)
        question_value = question_answer.get('value')
        expected = self.simple_date_data[self.survey_question.get_form_field_name()]
        self.assertEqual(question_value, expected)

    def test_simple_date_answer_3(self):
        empty_data = {self.survey_question.get_form_field_name(): ''}

        self.survey_question.param_is_required = True
        self.survey_question.save()

        response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/', data=empty_data)
        self.assertEqual(response.status_code, 400)

    def test_simple_date_answer_4(self):
        self.survey_question.param_is_required = True
        self.survey_question.save()

        response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/', data={})
        self.assertEqual(response.status_code, 400)


class TestDateRangeField(TestSurveyFormViewMixin, TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        super().setUp()
        self.survey = SurveyFactory(is_published_external=True)
        self.survey_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_date'),
            param_date_field_type='daterange',
        )
        self.question_form_field_name = self.survey_question.get_form_field_name()
        self.date_range_data = {
            '%s_0' % self.question_form_field_name: '2014-04-04',
            '%s_1' % self.question_form_field_name: '2014-04-05',
            'is_agree_with_hr': 'on',
            'is_agree_with_events': 'on',
        }

    def test_date_range_answer_1(self):
        self.survey_question.param_is_required = True
        self.survey_question.save()

        response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/', data=self.date_range_data)
        self.assertEqual(response.status_code, 200)

    def test_date_range_answer_2(self):
        self.survey_question.param_is_required = False
        self.survey_question.save()

        response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/', data=self.date_range_data)
        self.assertEqual(response.status_code, 200)

    def test_date_range_answer_3(self):
        empty_data = {
            '%s_0' % self.question_form_field_name: '',
            '%s_1' % self.question_form_field_name: '',
        }

        self.survey_question.param_is_required = True
        self.survey_question.save()

        response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/', data=empty_data)
        self.assertEqual(response.status_code, 400)

    def test_date_range_answer_4(self):
        empty_first_data = {
            '%s_0' % self.question_form_field_name: '',
            '%s_1' % self.question_form_field_name: '2014-04-05',
        }

        self.survey_question.param_is_required = True
        self.survey_question.save()

        response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/', data=empty_first_data)
        self.assertEqual(response.status_code, 400)

    def test_date_range_answer_5(self):
        empty_second_data = {
            '%s_0' % self.question_form_field_name: '2014-04-04',
            '%s_1' % self.question_form_field_name: '',
        }

        self.survey_question.param_is_required = True
        self.survey_question.save()

        response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/', data=empty_second_data)
        self.assertEqual(response.status_code, 400)

    def test_date_range_answer_6(self):
        empty_data = {
            '%s_0' % self.question_form_field_name: '',
            '%s_1' % self.question_form_field_name: '',
        }

        self.survey_question.param_is_required = False
        self.survey_question.save()

        response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/', data=empty_data)
        self.assertEqual(response.status_code, 200)

    def test_date_range_answer_7(self):
        empty_first_data = {
            '%s_0' % self.question_form_field_name: '',
            '%s_1' % self.question_form_field_name: '2014-04-05',
        }

        self.survey_question.param_is_required = False
        self.survey_question.save()

        response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/', data=empty_first_data)
        self.assertEqual(response.status_code, 400)

    def test_date_range_answer_8(self):
        empty_second_data = {
            '%s_0' % self.question_form_field_name: '2014-04-04',
            '%s_1' % self.question_form_field_name: '',
        }

        self.survey_question.param_is_required = False
        self.survey_question.save()

        response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/', data=empty_second_data)
        self.assertEqual(response.status_code, 400)


class TestCreateSurveyWithCustomType(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_superuser=True)

    def test_should_create_survey_with_default_type(self):
        data = {}
        response = self.client.post('/admin/api/v2/surveys/', data=data, format='json')
        self.assertEqual(response.status_code, 201)
        self.assertEqual(response.data['type'], 'simple_form')

    def test_should_create_survey_with_custom_type(self):
        data = {
            'type': 'zen',
        }
        response = self.client.post('/admin/api/v2/surveys/', data=data, format='json')
        self.assertEqual(response.status_code, 201)
        self.assertEqual(response.data['type'], 'zen')
