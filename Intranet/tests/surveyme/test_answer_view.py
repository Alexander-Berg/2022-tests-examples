# -*- coding: utf-8 -*-
from django.test import TestCase
from guardian.shortcuts import assign_perm, remove_perm
from events.accounts.helpers import YandexClient
from events.common_storages.utils import get_mds_url
from events.surveyme.factories import (
    SurveyFactory,
    SurveyQuestionFactory,
    SurveyQuestionChoiceFactory,
    ProfileSurveyAnswerFactory,
)
from events.surveyme.models import AnswerType


class TestAnswerResultsViewAdmin(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.survey = SurveyFactory()

    def test_should_return_short_text_question_data(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            label='Short text',
        )
        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            data={
                'data': [{
                    'question': question.get_answer_info(),
                    'value': 'Some text',
                }],
            }
        )

        self.client.login_yandex(is_superuser=True)
        response = self.client.get('/admin/api/v2/answers/%s/' % answer.pk)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data['data']), 1)

        answer_data = response.data['data'][0]
        self.assertEqual(answer_data['value'], 'Some text')
        self.assertEqual(answer_data['question']['label'], 'Short text')
        self.assertFalse(answer_data['question']['is_deleted'])
        self.assertFalse(answer_data['question']['is_hidden'])

    def test_should_return_short_text_question_data_by_secret_code(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            label='Short text',
        )
        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            data={
                'data': [{
                    'question': question.get_answer_info(),
                    'value': 'Some text',
                }],
            }
        )

        self.client.login_yandex(is_superuser=True)
        response = self.client.get('/admin/api/v2/answers/%s/' % answer.secret_code)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data['data']), 1)
        self.assertEqual(response.data['id'], answer.pk)

        answer_data = response.data['data'][0]
        self.assertEqual(answer_data['value'], 'Some text')
        self.assertEqual(answer_data['question']['label'], 'Short text')
        self.assertFalse(answer_data['question']['is_deleted'])

    def test_should_return_deleted_short_text_question_data(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            label='Short text',
            is_deleted=True,
            param_is_hidden=True,
        )
        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            data={
                'data': [{
                    'question': question.get_answer_info(),
                    'value': 'Some text',
                }],
            }
        )

        self.client.login_yandex(is_superuser=True)
        response = self.client.get('/admin/api/v2/answers/%s/' % answer.pk)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data['data']), 1)

        answer_data = response.data['data'][0]
        self.assertEqual(answer_data['value'], 'Some text')
        self.assertEqual(answer_data['question']['label'], 'Short text')
        self.assertTrue(answer_data['question']['is_deleted'])
        self.assertTrue(answer_data['question']['is_hidden'])

    def test_should_return_files_question_data(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_files'),
            label='Files',
        )
        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            data={
                'data': [{
                    'question': question.get_answer_info(),
                    'value': [{
                        'path': '123/456789_readme.txt',
                        'name': 'readme.txt',
                        'size': 321,
                    }],
                }],
            }
        )

        self.client.login_yandex(is_superuser=True)
        response = self.client.get('/admin/api/v2/answers/%s/' % answer.pk)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data['data']), 1)

        answer_data = response.data['data'][0]
        self.assertEqual(len(answer_data['value']), 1)
        self.assertDictEqual(answer_data['value'][0], {
            'path': get_mds_url('123/456789_readme.txt'),
            'name': 'readme.txt',
            'size': 321,
        })
        self.assertEqual(answer_data['question']['label'], 'Files')
        self.assertFalse(answer_data['question']['is_deleted'])
        self.assertFalse(answer_data['question']['is_hidden'])

    def test_should_return_group_question_data(self):
        group_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_group'),
            label='Group',
        )
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            label='Short text',
            group=group_question,
        )
        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            data={
                'data': [{
                    'question': group_question.get_answer_info(),
                    'value': [
                        [{
                            'question': question.get_answer_info(),
                            'value': 'text 1',
                        }],
                        [{
                            'question': question.get_answer_info(),
                            'value': 'text 2',
                        }],
                    ],
                }],
            }
        )

        self.client.login_yandex(is_superuser=True)
        response = self.client.get('/admin/api/v2/answers/%s/' % answer.pk)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data['data']), 1)

        answer_data = response.data['data'][0]
        self.assertEqual(answer_data['question']['label'], 'Group')
        self.assertFalse(answer_data['question']['is_deleted'])
        self.assertFalse(answer_data['question']['is_hidden'])

        self.assertEqual(len(answer_data['value']), 2)

        fieldset = answer_data['value'][0]
        self.assertEqual(len(fieldset), 1)
        self.assertEqual(fieldset[0]['question']['label'], 'Short text')
        self.assertFalse(fieldset[0]['question']['is_deleted'])
        self.assertFalse(fieldset[0]['question']['is_hidden'])
        self.assertEqual(fieldset[0]['value'], 'text 1')

        fieldset = answer_data['value'][1]
        self.assertEqual(len(fieldset), 1)
        self.assertEqual(fieldset[0]['question']['label'], 'Short text')
        self.assertFalse(fieldset[0]['question']['is_deleted'])
        self.assertFalse(fieldset[0]['question']['is_hidden'])
        self.assertEqual(fieldset[0]['value'], 'text 2')

    def test_should_return_group_question_data_with_deleted_sub_questions(self):
        group_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_group'),
            label='Group',
        )
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            label='Short text',
            group=group_question,
            is_deleted=True,
            param_is_hidden=True,
        )
        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            data={
                'data': [{
                    'question': group_question.get_answer_info(),
                    'value': [
                        [{
                            'question': question.get_answer_info(),
                            'value': 'text 1',
                        }],
                        [{
                            'question': question.get_answer_info(),
                            'value': 'text 2',
                        }],
                    ],
                }],
            }
        )

        self.client.login_yandex(is_superuser=True)
        response = self.client.get('/admin/api/v2/answers/%s/' % answer.pk)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data['data']), 1)

        answer_data = response.data['data'][0]
        self.assertEqual(answer_data['question']['label'], 'Group')
        self.assertFalse(answer_data['question']['is_deleted'])
        self.assertFalse(answer_data['question']['is_hidden'])

        self.assertEqual(len(answer_data['value']), 2)

        fieldset = answer_data['value'][0]
        self.assertEqual(len(fieldset), 1)
        self.assertEqual(fieldset[0]['question']['label'], 'Short text')
        self.assertTrue(fieldset[0]['question']['is_deleted'])
        self.assertTrue(fieldset[0]['question']['is_hidden'])
        self.assertEqual(fieldset[0]['value'], 'text 1')

        fieldset = answer_data['value'][1]
        self.assertEqual(len(fieldset), 1)
        self.assertEqual(fieldset[0]['question']['label'], 'Short text')
        self.assertTrue(fieldset[0]['question']['is_deleted'])
        self.assertTrue(fieldset[0]['question']['is_hidden'])
        self.assertEqual(fieldset[0]['value'], 'text 2')

    def test_should_return_deleted_group_question_data(self):
        group_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_group'),
            label='Group',
        )
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            label='Short text',
            group=group_question,
        )
        group_question.is_deleted = True
        group_question.param_is_hidden = True
        group_question.save()
        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            data={
                'data': [{
                    'question': group_question.get_answer_info(),
                    'value': [
                        [{
                            'question': question.get_answer_info(),
                            'value': 'text 1',
                        }],
                        [{
                            'question': question.get_answer_info(),
                            'value': 'text 2',
                        }],
                    ],
                }],
            }
        )

        self.client.login_yandex(is_superuser=True)
        response = self.client.get('/admin/api/v2/answers/%s/' % answer.pk)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data['data']), 1)

        answer_data = response.data['data'][0]
        self.assertEqual(answer_data['question']['label'], 'Group')
        self.assertTrue(answer_data['question']['is_deleted'])
        self.assertTrue(answer_data['question']['is_hidden'])

        self.assertEqual(len(answer_data['value']), 2)

        fieldset = answer_data['value'][0]
        self.assertEqual(len(fieldset), 1)
        self.assertEqual(fieldset[0]['question']['label'], 'Short text')
        self.assertFalse(fieldset[0]['question']['is_deleted'])
        self.assertFalse(fieldset[0]['question']['is_hidden'])
        self.assertEqual(fieldset[0]['value'], 'text 1')

        fieldset = answer_data['value'][1]
        self.assertEqual(len(fieldset), 1)
        self.assertEqual(fieldset[0]['question']['label'], 'Short text')
        self.assertFalse(fieldset[0]['question']['is_deleted'])
        self.assertFalse(fieldset[0]['question']['is_hidden'])
        self.assertEqual(fieldset[0]['value'], 'text 2')

    def test_should_return_short_text_question_data_for_valid_user(self):
        profile = self.client.login_yandex()
        assign_perm('change_survey', profile, self.survey)
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            label='Short text',
        )
        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            data={
                'data': [{
                    'question': question.get_answer_info(),
                    'value': 'Some text',
                }],
            }
        )

        response = self.client.get('/admin/api/v2/answers/%s/' % answer.pk)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data['data']), 1)

        answer_data = response.data['data'][0]
        self.assertEqual(answer_data['value'], 'Some text')
        self.assertEqual(answer_data['question']['label'], 'Short text')
        self.assertFalse(answer_data['question']['is_deleted'])
        self.assertFalse(answer_data['question']['is_hidden'])

    def test_shouldnt_return_short_text_question_data_for_invalid_user(self):
        profile = self.client.login_yandex()
        remove_perm('change_survey', profile, self.survey)
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            label='Short text',
        )
        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            data={
                'data': [{
                    'question': question.get_answer_info(),
                    'value': 'Some text',
                }],
            }
        )

        response = self.client.get('/admin/api/v2/answers/%s/' % answer.pk)
        self.assertEqual(response.status_code, 403)

    def test_should_return_data_with_quiz_params_for_answer_choices(self):
        self.client.login_yandex(is_superuser=True)
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
        )
        choices = [
            SurveyQuestionChoiceFactory(survey_question=question, label='one'),
            SurveyQuestionChoiceFactory(survey_question=question, label='two'),
            SurveyQuestionChoiceFactory(survey_question=question, label='three'),
        ]
        question.param_quiz = {
            'enabled': True,
            'answers': [{
                'value': choices[0].label,
                'scores': 5,
                'correct': True,
            }, {
                'value': choices[1].label,
                'scores': 10,
                'correct': True,
            }, {
                'value': choices[2].label,
                'scores': 0,
                'correct': False,
            }],
        }
        question.save()
        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            data={
                'data': [{
                    'question': question.get_answer_info(),
                    'scores': 0,
                    'value': [{
                        'key': str(choices[1].pk),
                        'text': choices[1].label,
                    }],
                }],
            }
        )

        response = self.client.get('/admin/api/v2/answers/%s/' % answer.pk)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data['data']), 1)
        answer = response.data['data'][0]
        self.assertListEqual(answer['value'], [{
            'key': str(choices[1].pk),
            'text': choices[1].label,
        }])
        self.assertListEqual(answer['question']['choices'], [{
            'key': str(choices[0].pk),
            'slug': str(choices[0].pk),
            'text': choices[0].label,
            'correct': True,
        }, {
            'key': str(choices[1].pk),
            'slug': str(choices[1].pk),
            'text': choices[1].label,
            'correct': True,
        }, {
            'key': str(choices[2].pk),
            'slug': str(choices[2].pk),
            'text': choices[2].label,
            'correct': False,
        }])

    def test_should_return_data_with_quiz_params_for_answer_short_text(self):
        self.client.login_yandex(is_superuser=True)
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            param_quiz={
                'enabled': True,
                'answers': [{
                    'value': '42',
                    'scores': 5,
                    'correct': True,
                }],
            },
        )
        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            data={
                'data': [{
                    'question': question.get_answer_info(),
                    'scores': 0,
                    'value': '12',
                }],
            }
        )

        response = self.client.get('/admin/api/v2/answers/%s/' % answer.pk)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data['data']), 1)
        answer = response.data['data'][0]
        self.assertEqual(answer['value'], '12')
        self.assertEqual(answer['question']['correct_value'], '42')


class TestAnswerResultsViewFront(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()

    def test_should_return_short_text_question_data_for_secret_code(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            label='Short text',
        )
        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            data={
                'data': [{
                    'question': question.get_answer_info(),
                    'value': 'Some text',
                }],
            }
        )

        response = self.client.get('/v1/answers/%s/' % answer.secret_code)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['survey_id'], self.survey.pk)
        self.assertEqual(response.data['survey_name'], self.survey.name)
        self.assertEqual(len(response.data['data']), 1)

        answer_data = response.data['data'][0]
        self.assertEqual(answer_data['value'], 'Some text')
        self.assertEqual(answer_data['question']['label'], 'Short text')
        self.assertFalse(answer_data['question']['is_deleted'])
        self.assertFalse(answer_data['question']['is_hidden'])

    def test_shouldnt_return_short_text_question_data_for_answer_id(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            label='Short text',
        )
        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            data={
                'data': [{
                    'question': question.get_answer_info(),
                    'value': 'Some text',
                }],
            }
        )

        response = self.client.get('/v1/answers/%s/' % answer.pk)
        self.assertEqual(response.status_code, 404)

    def test_should_return_files_question_data(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_files'),
            label='Files',
        )
        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            data={
                'data': [{
                    'question': question.get_answer_info(),
                    'value': [{
                        'path': '123/456789_readme.txt',
                        'name': 'readme.txt',
                        'size': 321,
                    }],
                }],
            }
        )

        response = self.client.get('/v1/answers/%s/' % answer.secret_code)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['survey_id'], self.survey.pk)
        self.assertEqual(response.data['survey_name'], self.survey.name)
        self.assertEqual(len(response.data['data']), 1)

        answer_data = response.data['data'][0]
        self.assertEqual(len(answer_data['value']), 1)
        self.assertDictEqual(answer_data['value'][0], {
            'path': get_mds_url('123/456789_readme.txt'),
            'name': 'readme.txt',
            'size': 321,
        })
        self.assertEqual(answer_data['question']['label'], 'Files')
        self.assertFalse(answer_data['question']['is_deleted'])
        self.assertFalse(answer_data['question']['is_hidden'])

    def test_should_return_data_with_quiz_params_for_answer_choices(self):
        self.survey.extra = {
            'quiz': {
                'show_correct': True,
            },
        }
        self.survey.save()
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
        )
        choices = [
            SurveyQuestionChoiceFactory(survey_question=question, label='one'),
            SurveyQuestionChoiceFactory(survey_question=question, label='two'),
            SurveyQuestionChoiceFactory(survey_question=question, label='three'),
        ]
        question.param_quiz = {
            'enabled': True,
            'answers': [{
                'value': choices[0].label,
                'scores': 5,
                'correct': True,
            }, {
                'value': choices[1].label,
                'scores': 10,
                'correct': True,
            }, {
                'value': choices[2].label,
                'scores': 0,
                'correct': False,
            }],
        }
        question.save()
        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            data={
                'data': [{
                    'question': question.get_answer_info(),
                    'scores': 0,
                    'value': [{
                        'key': str(choices[1].pk),
                        'text': choices[1].label,
                    }],
                }],
            }
        )

        response = self.client.get('/v1/answers/%s/' % answer.secret_code)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data['data']), 1)
        answer = response.data['data'][0]
        self.assertListEqual(answer['value'], [{
            'key': str(choices[1].pk),
            'text': choices[1].label,
        }])
        self.assertListEqual(answer['question']['choices'], [{
            'key': str(choices[0].pk),
            'slug': str(choices[0].pk),
            'text': choices[0].label,
            'correct': True,
        }, {
            'key': str(choices[1].pk),
            'slug': str(choices[1].pk),
            'text': choices[1].label,
            'correct': True,
        }, {
            'key': str(choices[2].pk),
            'slug': str(choices[2].pk),
            'text': choices[2].label,
            'correct': False,
        }])

    def test_should_return_data_without_quiz_params_for_answer_choices(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
        )
        choices = [
            SurveyQuestionChoiceFactory(survey_question=question, label='one'),
            SurveyQuestionChoiceFactory(survey_question=question, label='two'),
            SurveyQuestionChoiceFactory(survey_question=question, label='three'),
        ]
        question.param_quiz = {
            'enabled': True,
            'answers': [{
                'value': choices[0].label,
                'scores': 5,
                'correct': True,
            }, {
                'value': choices[1].label,
                'scores': 10,
                'correct': True,
            }, {
                'value': choices[2].label,
                'scores': 0,
                'correct': False,
            }],
        }
        question.save()
        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            data={
                'data': [{
                    'question': question.get_answer_info(),
                    'scores': 0,
                    'value': [{
                        'key': str(choices[1].pk),
                        'text': choices[1].label,
                    }],
                }],
            }
        )

        response = self.client.get('/v1/answers/%s/' % answer.secret_code)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data['data']), 1)
        answer = response.data['data'][0]
        self.assertListEqual(answer['value'], [{
            'key': str(choices[1].pk),
            'text': choices[1].label,
        }])
        self.assertTrue('choices' not in answer['question'])

    def test_should_return_data_with_quiz_params_for_answer_short_text(self):
        self.survey.extra = {
            'quiz': {
                'show_correct': True,
            },
        }
        self.survey.save()
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            param_quiz={
                'enabled': True,
                'answers': [{
                    'value': '42',
                    'scores': 5,
                    'correct': True,
                }],
            },
        )
        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            data={
                'data': [{
                    'question': question.get_answer_info(),
                    'scores': 0,
                    'value': '12',
                }],
            }
        )

        response = self.client.get('/v1/answers/%s/' % answer.secret_code)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data['data']), 1)
        answer = response.data['data'][0]
        self.assertEqual(answer['value'], '12')
        self.assertEqual(answer['question']['correct_value'], '42')

    def test_should_return_data_without_quiz_params_for_answer_short_text(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            param_quiz={
                'enabled': True,
                'answers': [{
                    'value': '42',
                    'scores': 5,
                    'correct': True,
                }],
            },
        )
        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            data={
                'data': [{
                    'question': question.get_answer_info(),
                    'scores': 0,
                    'value': '12',
                }],
            }
        )

        response = self.client.get('/v1/answers/%s/' % answer.secret_code)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data['data']), 1)
        answer = response.data['data'][0]
        self.assertEqual(answer['value'], '12')
        self.assertTrue('correct_value' not in answer['question'])


class TestScoreResultsViewFront(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()

    def test_should_return_mostly_null_response(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            label='Short text',
        )
        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            data={
                'data': [{
                    'question': question.get_answer_info(),
                    'value': 'Some text',
                }],
            }
        )

        response = self.client.get('/v1/scores/%s/' % answer.secret_code)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['survey_id'], self.survey.pk)
        self.assertFalse(response.data['show_results'])
        self.assertFalse(response.data['show_correct'])
        self.assertIsNone(response.data['scores'])
        self.assertIsNone(response.data['total_scores'])
        self.assertIsNone(response.data['title'])
        self.assertIsNone(response.data['description'])
        self.assertIsNone(response.data['image'])

    def test_should_return_scores_response(self):
        self.survey.extra = {
            'quiz': {
                'show_results': True,
                'show_correct': True,
            },
        }
        self.survey.save()
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            label='Short text',
        )
        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            data={
                'quiz': {
                    'title': 'Some title',
                    'description': 'Some description',
                    'scores': 10.0/3,
                    'total_scores': 100.0/3,
                    'image_path': None,
                },
                'data': [{
                    'question': question.get_answer_info(),
                    'value': 'Some text',
                }],
            }
        )

        response = self.client.get('/v1/scores/%s/' % answer.secret_code)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['survey_id'], self.survey.pk)
        self.assertTrue(response.data['show_results'])
        self.assertTrue(response.data['show_correct'])
        self.assertEqual(response.data['scores'], 3.33)
        self.assertEqual(response.data['total_scores'], 33.33)
        self.assertEqual(response.data['title'], 'Some title')
        self.assertEqual(response.data['description'], 'Some description')
        self.assertIsNone(response.data['image'])
