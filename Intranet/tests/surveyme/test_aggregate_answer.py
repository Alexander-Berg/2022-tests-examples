# -*- coding: utf-8 -*-
from datetime import date
from django.test import TestCase
from events.accounts.factories import UserFactory
from events.common_storages.factories import ProxyStorageModelFactory
from events.data_sources.models import DataSourceItem
from events.surveyme.factories import (
    ProfileSurveyAnswerFactory,
    SurveyFactory,
    SurveyQuestionFactory,
    SurveyQuestionChoiceFactory,
    SurveyQuestionMatrixTitleFactory,
)
from events.surveyme.models import AnswerType, ProfileSurveyAnswer, ValidatorType
from events.media.factories import ImageFactory
from events.surveyme.aggregate_answer import (
    get_answer_data,
    get_scores_data,
    get_quiz_data,
    make_decimal,
)


class TestAnswerData(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.user = UserFactory()
        self.files = [
            ProxyStorageModelFactory(
                path='/101/1234567890_file1.txt',
                original_name='file1.txt',
                file_size=112233,
            ),
            ProxyStorageModelFactory(
                path='/102/0987654321_file2.txt',
                original_name='file2.txt',
                file_size=332211,
            ),
        ]
        self.survey = SurveyFactory()
        self.questions = [
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=AnswerType.objects.get(slug='answer_short_text'),
                param_is_required=False,
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=AnswerType.objects.get(slug='answer_long_text'),
                param_is_required=False,
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=AnswerType.objects.get(slug='answer_number'),
                param_is_required=False,
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=AnswerType.objects.get(slug='answer_non_profile_email'),
                param_is_required=False,
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=AnswerType.objects.get(slug='answer_boolean'),
                param_is_required=False,
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=AnswerType.objects.get(slug='answer_url'),
                param_is_required=False,
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=AnswerType.objects.get(slug='answer_phone'),
                param_is_required=False,
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=AnswerType.objects.get(slug='answer_name'),
                param_is_required=False,
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=AnswerType.objects.get(slug='answer_surname'),
                param_is_required=False,
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=AnswerType.objects.get(slug='answer_date'),
                param_is_required=False,
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=AnswerType.objects.get(slug='answer_files'),
                param_is_required=False,
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=AnswerType.objects.get(slug='answer_choices'),
                param_is_required=False,
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=AnswerType.objects.get(slug='answer_group'),
                param_is_required=False,
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=AnswerType.objects.get(slug='answer_payment'),
                param_is_required=False,
                param_payment={
                    'account_id': '12345678',
                    'is_fixed': True,
                },
            ),
        ]
        self.choices = [
            SurveyQuestionChoiceFactory(survey_question=self.questions[11], label='One', slug='x-one'),
            SurveyQuestionChoiceFactory(survey_question=self.questions[11], label='Two', slug='x-two'),
            SurveyQuestionChoiceFactory(survey_question=self.questions[11], label='Three'),
        ]
        self.columns = [
            SurveyQuestionMatrixTitleFactory(survey_question=self.questions[11], label='Column1', type='column'),
            SurveyQuestionMatrixTitleFactory(survey_question=self.questions[11], label='Column2', type='column'),
        ]
        self.rows = [
            SurveyQuestionMatrixTitleFactory(survey_question=self.questions[11], label='Row1', type='row'),
            SurveyQuestionMatrixTitleFactory(survey_question=self.questions[11], label='Row2', type='row'),
        ]
        self.children = [
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=AnswerType.objects.get(slug='answer_short_text'),
                param_is_required=False,
                group_id=self.questions[12].pk,
                position=1,
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=AnswerType.objects.get(slug='answer_boolean'),
                param_is_required=False,
                group_id=self.questions[12].pk,
                position=2,
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                answer_type=AnswerType.objects.get(slug='answer_choices'),
                param_is_required=False,
                group_id=self.questions[12].pk,
                position=3,
            ),
        ]
        self.children_choices = [
            SurveyQuestionChoiceFactory(survey_question=self.children[2], label='Mon'),
            SurveyQuestionChoiceFactory(survey_question=self.children[2], label='Tue'),
            SurveyQuestionChoiceFactory(survey_question=self.children[2], label='Wed'),
        ]
        self.answer = ProfileSurveyAnswerFactory(survey=self.survey)

    def test_should_use_uid_and_cloud_uid(self):
        self.user.uid = '123'
        self.user.cloud_uid = 'abcd'
        self.user.save()

        self.answer.user = self.user
        self.answer.save()

        response = get_answer_data(self.survey, self.answer, {})

        self.assertEqual(response['id'], self.answer.pk)
        self.assertEqual(response['uid'], self.user.uid)
        self.assertEqual(response['cloud_uid'], self.user.cloud_uid)

    def test_should_use_only_cloud_uid(self):
        self.user.uid = None
        self.user.cloud_uid = 'abcd'
        self.user.save()

        self.answer.user = self.user
        self.answer.save()

        response = get_answer_data(self.survey, self.answer, {})

        self.assertEqual(response['id'], self.answer.pk)
        self.assertIsNone(response['uid'])
        self.assertEqual(response['cloud_uid'], self.user.cloud_uid)

    def test_should_use_only_uid(self):
        self.user.uid = '123'
        self.user.cloud_uid = None
        self.user.save()

        self.answer.user = self.user
        self.answer.save()

        response = get_answer_data(self.survey, self.answer, {})

        self.assertEqual(response['id'], self.answer.pk)
        self.assertEqual(response['uid'], self.user.uid)
        self.assertFalse('cloud_uid' in response)

    def test_answer_short_text(self):
        question = self.questions[0]

        cleaned_data = {
            question.param_slug: 'hello world',
        }
        response = get_answer_data(self.survey, self.answer, cleaned_data)

        self.assertEqual(response['id'], self.answer.pk)
        self.assertIsNone(response['uid'])
        self.assertEqual(response['created'], self.answer.date_created.strftime('%Y-%m-%dT%H:%M:%SZ'))

        survey_data = response['survey']
        self.assertEqual(survey_data['id'], str(self.survey.pk))
        self.assertEqual(survey_data['slug'], self.survey.slug)

        self.assertIn('data', response)
        data = response['data']
        self.assertTrue(len(data) > 0)
        answer_0 = data[0]

        self.assertIn('question', answer_0)
        question_data = answer_0['question']
        self.assertEqual(question_data['id'], question.pk)
        self.assertEqual(question_data['slug'], question.param_slug)

        self.assertIn('answer_type', question_data)
        answer_type_data = question_data['answer_type']
        self.assertEqual(answer_type_data['id'], question.answer_type.pk)
        self.assertEqual(answer_type_data['slug'], 'answer_short_text')

        self.assertIn('value', answer_0)
        value = answer_0['value']
        self.assertEqual(value, 'hello world')

    def test_answer_short_text_extra_data(self):
        self.answer.user = self.user
        self.answer.save()

        self.survey.slug = 'test-form'
        self.survey.save()

        cleaned_data = {}
        response = get_answer_data(self.survey, self.answer, cleaned_data)

        self.assertEqual(response['uid'], str(self.user.uid))

        survey_data = response['survey']
        self.assertEqual(survey_data['id'], str(self.survey.pk))
        self.assertEqual(survey_data['slug'], 'test-form')

    def test_answer_short_text_options(self):
        question = self.questions[0]
        question.param_is_required = True
        question.save()

        cleaned_data = {
            question.param_slug: 'hello world',
        }
        response = get_answer_data(self.survey, self.answer, cleaned_data)

        self.assertIn('data', response)
        data = response['data']
        self.assertTrue(len(data) > 0)
        answer_0 = data[0]

        self.assertIn('question', answer_0)
        question_data = answer_0['question']

        self.assertIn('options', question_data)
        options = question_data['options']
        self.assertEqual(options['required'], True)

    def test_answer_long_text(self):
        question = self.questions[1]

        cleaned_data = {
            question.param_slug: 'hello\nworld',
        }
        response = get_answer_data(self.survey, self.answer, cleaned_data)
        self.assertIn('data', response)
        data = response['data']
        self.assertTrue(len(data) > 0)
        answer_0 = data[0]

        question_data = answer_0['question']
        answer_type_data = question_data['answer_type']
        self.assertEqual(answer_type_data['slug'], 'answer_long_text')

        self.assertIn('value', answer_0)
        value = answer_0['value']
        self.assertEqual(value, 'hello\nworld')

    def test_answer_number(self):
        question = self.questions[2]

        cleaned_data = {
            question.param_slug: 42,
        }
        response = get_answer_data(self.survey, self.answer, cleaned_data)
        self.assertIn('data', response)
        data = response['data']
        self.assertTrue(len(data) > 0)
        answer_0 = data[0]

        question_data = answer_0['question']
        answer_type_data = question_data['answer_type']
        self.assertEqual(answer_type_data['slug'], 'answer_number')

        self.assertIn('value', answer_0)
        value = answer_0['value']
        self.assertEqual(value, 42)

    def test_answer_non_profile_email(self):
        question = self.questions[3]

        cleaned_data = {
            question.param_slug: 'wow@yandex.ru',
        }
        response = get_answer_data(self.survey, self.answer, cleaned_data)
        self.assertIn('data', response)
        data = response['data']
        self.assertTrue(len(data) > 0)
        answer_0 = data[0]

        question_data = answer_0['question']
        answer_type_data = question_data['answer_type']
        self.assertEqual(answer_type_data['slug'], 'answer_non_profile_email')

        self.assertIn('value', answer_0)
        value = answer_0['value']
        self.assertEqual(value, 'wow@yandex.ru')

    def test_answer_boolean(self):
        question = self.questions[4]

        cleaned_data = {
            question.param_slug: True,
        }
        response = get_answer_data(self.survey, self.answer, cleaned_data)
        self.assertIn('data', response)
        data = response['data']
        self.assertTrue(len(data) > 0)
        answer_0 = data[0]

        question_data = answer_0['question']
        answer_type_data = question_data['answer_type']
        self.assertEqual(answer_type_data['slug'], 'answer_boolean')

        self.assertIn('value', answer_0)
        value = answer_0['value']
        self.assertEqual(value, True)

    def test_answer_url(self):
        question = self.questions[5]

        cleaned_data = {
            question.param_slug: 'https://yandex.ru',
        }
        response = get_answer_data(self.survey, self.answer, cleaned_data)
        self.assertIn('data', response)
        data = response['data']
        self.assertTrue(len(data) > 0)
        answer_0 = data[0]

        question_data = answer_0['question']
        answer_type_data = question_data['answer_type']
        self.assertEqual(answer_type_data['slug'], 'answer_url')

        self.assertIn('value', answer_0)
        value = answer_0['value']
        self.assertEqual(value, 'https://yandex.ru')

    def test_answer_phone(self):
        question = self.questions[6]

        cleaned_data = {
            question.param_slug: '+7 800 789 12 34',
        }
        response = get_answer_data(self.survey, self.answer, cleaned_data)
        self.assertIn('data', response)
        data = response['data']
        self.assertTrue(len(data) > 0)
        answer_0 = data[0]

        question_data = answer_0['question']
        answer_type_data = question_data['answer_type']
        self.assertEqual(answer_type_data['slug'], 'answer_phone')

        self.assertIn('value', answer_0)
        value = answer_0['value']
        self.assertEqual(value, '+7 800 789 12 34')

    def test_answer_name(self):
        question = self.questions[7]

        cleaned_data = {
            question.param_slug: 'Your name',
        }
        response = get_answer_data(self.survey, self.answer, cleaned_data)
        self.assertIn('data', response)
        data = response['data']
        self.assertTrue(len(data) > 0)
        answer_0 = data[0]

        question_data = answer_0['question']
        answer_type_data = question_data['answer_type']
        self.assertEqual(answer_type_data['slug'], 'answer_name')

        self.assertIn('value', answer_0)
        value = answer_0['value']
        self.assertEqual(value, 'Your name')

    def test_answer_surname(self):
        question = self.questions[8]

        cleaned_data = {
            question.param_slug: 'Your surname',
        }
        response = get_answer_data(self.survey, self.answer, cleaned_data)
        self.assertIn('data', response)
        data = response['data']
        self.assertTrue(len(data) > 0)
        answer_0 = data[0]

        question_data = answer_0['question']
        answer_type_data = question_data['answer_type']
        self.assertEqual(answer_type_data['slug'], 'answer_surname')

        self.assertIn('value', answer_0)
        value = answer_0['value']
        self.assertEqual(value, 'Your surname')

    def test_answer_date(self):
        question = self.questions[9]

        cleaned_data = {
            question.param_slug: dict(date_start=date(2019, 4, 19)),
        }
        response = get_answer_data(self.survey, self.answer, cleaned_data)
        self.assertIn('data', response)
        data = response['data']
        self.assertTrue(len(data) > 0)
        answer_0 = data[0]

        question_data = answer_0['question']
        answer_type_data = question_data['answer_type']
        self.assertEqual(answer_type_data['slug'], 'answer_date')
        self.assertIn('options', question_data)
        options_data = question_data['options']
        self.assertFalse(options_data['date_range'])

        self.assertIn('value', answer_0)
        value = answer_0['value']
        self.assertEqual(value, '2019-04-19')

    def test_answer_date_range(self):
        question = self.questions[9]
        question.param_date_field_type = 'daterange'
        question.save()

        cleaned_data = {
            question.param_slug: dict(
                date_start=date(2019, 4, 1),
                date_end=date(2019, 4, 30),
            ),
        }
        response = get_answer_data(self.survey, self.answer, cleaned_data)
        self.assertIn('data', response)
        data = response['data']
        self.assertTrue(len(data) > 0)
        answer_0 = data[0]

        question_data = answer_0['question']
        answer_type_data = question_data['answer_type']
        self.assertEqual(answer_type_data['slug'], 'answer_date')
        self.assertIn('options', question_data)
        options_data = question_data['options']
        self.assertTrue(options_data['date_range'])

        self.assertIn('value', answer_0)
        value = answer_0['value']
        self.assertEqual(value['begin'], '2019-04-01')
        self.assertEqual(value['end'], '2019-04-30')

    def test_answer_date_range_half_open_with_start(self):
        question = self.questions[9]
        question.param_date_field_type = 'daterange'
        question.save()

        cleaned_data = {
            question.param_slug: dict(date_start=date(2019, 4, 1)),
        }
        response = get_answer_data(self.survey, self.answer, cleaned_data)
        self.assertIn('data', response)
        data = response['data']
        self.assertTrue(len(data) > 0)
        answer_0 = data[0]

        question_data = answer_0['question']
        answer_type_data = question_data['answer_type']
        self.assertEqual(answer_type_data['slug'], 'answer_date')
        self.assertIn('options', question_data)
        options_data = question_data['options']
        self.assertTrue(options_data['date_range'])

        self.assertIn('value', answer_0)
        value = answer_0['value']
        self.assertEqual(value['begin'], '2019-04-01')
        self.assertIsNone(value['end'])

    def test_answer_date_range_half_open_with_end(self):
        question = self.questions[9]
        question.param_date_field_type = 'daterange'
        question.save()

        cleaned_data = {
            question.param_slug: dict(date_end=date(2019, 4, 30)),
        }
        response = get_answer_data(self.survey, self.answer, cleaned_data)
        self.assertIn('data', response)
        data = response['data']
        self.assertTrue(len(data) > 0)
        answer_0 = data[0]

        question_data = answer_0['question']
        answer_type_data = question_data['answer_type']
        self.assertEqual(answer_type_data['slug'], 'answer_date')
        self.assertIn('options', question_data)
        options_data = question_data['options']
        self.assertTrue(options_data['date_range'])

        self.assertIn('value', answer_0)
        value = answer_0['value']
        self.assertIsNone(value['begin'])
        self.assertEqual(value['end'], '2019-04-30')

    def test_answer_files(self):
        question = self.questions[10]

        cleaned_data = {
            question.param_slug: [
                '/101/1234567890_file1.txt',
                '/102/0987654321_file2.txt',
            ],
        }
        response = get_answer_data(self.survey, self.answer, cleaned_data)
        self.assertIn('data', response)
        data = response['data']
        self.assertTrue(len(data) > 0)
        answer_0 = data[0]

        question_data = answer_0['question']
        answer_type_data = question_data['answer_type']
        self.assertEqual(answer_type_data['slug'], 'answer_files')

        self.assertIn('value', answer_0)
        value = answer_0['value']
        self.assertEqual(len(value), 2)
        self.assertEqual(value[0], {
            'path': '/101/1234567890_file1.txt',
            'name': 'file1.txt',
            'size': 112233,
            'namespace': 'forms',
        })
        self.assertEqual(value[1], {
            'path': '/102/0987654321_file2.txt',
            'name': 'file2.txt',
            'size': 332211,
            'namespace': 'forms',
        })

    def test_answer_choices(self):
        question = self.questions[11]
        question.param_data_source = 'survey_question_choice'
        question.param_is_allow_multiple_choice = True
        question.save()

        cleaned_data = {
            question.param_slug: [
                DataSourceItem(identity=str(self.choices[0].pk)),
                DataSourceItem(identity=str(self.choices[2].pk)),
            ],
        }
        response = get_answer_data(self.survey, self.answer, cleaned_data)
        self.assertIn('data', response)
        data = response['data']
        self.assertTrue(len(data) > 0)
        answer_0 = data[0]

        question_data = answer_0['question']
        answer_type_data = question_data['answer_type']
        self.assertEqual(answer_type_data['slug'], 'answer_choices')

        self.assertIn('options', question_data)
        options_data = question_data['options']
        self.assertTrue(options_data['multiple'])
        self.assertEqual(options_data['data_source'], 'survey_question_choice')
        self.assertIn('ordering', options_data)

        self.assertIn('value', answer_0)
        value = answer_0['value']
        self.assertEqual(len(value), 2)
        self.assertEqual(value[0], {
            'key': str(self.choices[0].pk),
            'slug': 'x-one',
            'text': 'One',
        })
        self.assertEqual(value[1], {
            'key': str(self.choices[2].pk),
            'slug': str(self.choices[2].pk),
            'text': 'Three',
        })

    def test_answer_data_source(self):
        question = self.questions[11]
        question.param_data_source = 'music_genre'
        question.save()

        cleaned_data = {
            question.param_slug: [
                DataSourceItem.objects.get_and_update(
                    data_source='music_genre',
                    identity='conjazz',
                    text='Contemporary Jazz',
                ),
            ],
        }
        response = get_answer_data(self.survey, self.answer, cleaned_data)
        self.assertIn('data', response)
        data = response['data']
        self.assertTrue(len(data) > 0)
        answer_0 = data[0]

        question_data = answer_0['question']
        answer_type_data = question_data['answer_type']
        self.assertEqual(answer_type_data['slug'], 'answer_choices')

        self.assertIn('options', question_data)
        options_data = question_data['options']
        self.assertFalse(options_data['multiple'])
        self.assertEqual(options_data['data_source'], 'music_genre')
        self.assertIn('ordering', options_data)

        self.assertIn('value', answer_0)
        value = answer_0['value']
        self.assertEqual(len(value), 1)
        self.assertEqual(value[0], {
            'key': 'conjazz',
            'text': 'Contemporary Jazz',
        })

    def test_answer_matrix_choice(self):
        question = self.questions[11]
        question.param_data_source = 'survey_question_matrix_choice'
        question.save()

        cleaned_data = {
            question.param_slug: [
                DataSourceItem.objects.get_and_update(
                    data_source='survey_question_matrix_choice',
                    identity='%s_%s' % (self.rows[0].pk, self.columns[0].pk),
                    text='"%s": %s' % (self.rows[0].label, self.columns[0].label),
                ),
                DataSourceItem.objects.get_and_update(
                    data_source='survey_question_matrix_choice',
                    identity='%s_%s' % (self.rows[1].pk, self.columns[1].pk),
                    text='"%s": %s' % (self.rows[1].label, self.columns[1].label),
                ),
            ],
        }
        response = get_answer_data(self.survey, self.answer, cleaned_data)
        self.assertIn('data', response)
        data = response['data']
        self.assertTrue(len(data) > 0)
        answer_0 = data[0]

        question_data = answer_0['question']
        answer_type_data = question_data['answer_type']
        self.assertEqual(answer_type_data['slug'], 'answer_choices')

        self.assertIn('options', question_data)
        options_data = question_data['options']
        self.assertFalse(options_data['multiple'])
        self.assertEqual(options_data['data_source'], 'survey_question_matrix_choice')
        self.assertIn('ordering', options_data)

        self.assertIn('value', answer_0)
        value = answer_0['value']
        self.assertEqual(len(value), 2)
        self.assertEqual(value[0]['row'], {
            'key': str(self.rows[0].pk),
            'text': 'Row1',
        })
        self.assertEqual(value[0]['col'], {
            'key': str(self.columns[0].pk),
            'text': 'Column1',
        })
        self.assertEqual(value[1]['row'], {
            'key': str(self.rows[1].pk),
            'text': 'Row2',
        })
        self.assertEqual(value[1]['col'], {
            'key': str(self.columns[1].pk),
            'text': 'Column2',
        })

    def test_answer_group_with_empty_fieldset(self):
        for child in self.children[1:]:
            child.is_deleted = True
            child.save()

        cleaned_data = {
            '%s__%s' % (self.children[0].param_slug, 0): 'Item1',
            '%s__%s' % (self.children[0].param_slug, 2): 'Item3',
        }

        response = get_answer_data(self.survey, self.answer, cleaned_data)
        self.assertIn('data', response)
        data = response['data']
        self.assertTrue(len(data) > 0)
        answer_0 = data[0]

        question_data = answer_0['question']
        answer_type_data = question_data['answer_type']
        self.assertEqual(answer_type_data['slug'], 'answer_group')

        self.assertIn('value', answer_0)
        value = answer_0['value']
        self.assertEqual(len(value), 2)

        item1 = value[0]
        self.assertEqual(len(item1), 1)
        answer_text = item1[0]
        self.assertEqual(answer_text['question']['answer_type']['slug'], 'answer_short_text')
        self.assertEqual(answer_text['value'], 'Item1')

        item2 = value[1]
        self.assertEqual(len(item2), 1)
        answer_text = item2[0]
        self.assertEqual(answer_text['question']['answer_type']['slug'], 'answer_short_text')
        self.assertEqual(answer_text['value'], 'Item3')

    def test_answer_group(self):
        cleaned_data = {
            '%s__%s' % (self.children[0].param_slug, 0): 'Item1',
            '%s__%s' % (self.children[1].param_slug, 0): True,

            '%s__%s' % (self.children[0].param_slug, 1): 'Item2',
            '%s__%s' % (self.children[1].param_slug, 1): False,
            '%s__%s' % (self.children[2].param_slug, 1): [
                DataSourceItem(identity=str(self.children_choices[1].pk)),
            ],

            '%s__%s' % (self.children[1].param_slug, 2): True,
            '%s__%s' % (self.children[2].param_slug, 2): [
                DataSourceItem(identity=str(self.children_choices[2].pk)),
            ],
        }

        response = get_answer_data(self.survey, self.answer, cleaned_data)
        self.assertIn('data', response)
        data = response['data']
        self.assertTrue(len(data) > 0)
        answer_0 = data[0]

        question_data = answer_0['question']
        answer_type_data = question_data['answer_type']
        self.assertEqual(answer_type_data['slug'], 'answer_group')

        self.assertIn('value', answer_0)
        value = answer_0['value']
        self.assertEqual(len(value), 3)

        item1 = value[0]
        self.assertEqual(len(item1), 2)
        answer_text = item1[0]
        self.assertEqual(answer_text['question']['answer_type']['slug'], 'answer_short_text')
        self.assertEqual(answer_text['value'], 'Item1')

        answer_boolean = item1[1]
        self.assertEqual(answer_boolean['question']['answer_type']['slug'], 'answer_boolean')
        self.assertTrue(answer_boolean['value'])

        item2 = value[1]
        self.assertEqual(len(item2), 3)
        answer_text = item2[0]
        self.assertEqual(answer_text['question']['answer_type']['slug'], 'answer_short_text')
        self.assertEqual(answer_text['value'], 'Item2')

        answer_boolean = item2[1]
        self.assertEqual(answer_boolean['question']['answer_type']['slug'], 'answer_boolean')
        self.assertFalse(answer_boolean['value'])

        answer_choices = item2[2]
        self.assertEqual(answer_choices['question']['answer_type']['slug'], 'answer_choices')
        self.assertEqual(answer_choices['value'], [{
            'key': str(self.children_choices[1].pk),
            'slug': str(self.children_choices[1].pk),
            'text': 'Tue',
        }])

        item3 = value[2]
        self.assertEqual(len(item3), 2)
        answer_boolean = item3[0]
        self.assertEqual(answer_boolean['question']['answer_type']['slug'], 'answer_boolean')
        self.assertTrue(answer_boolean['value'])

        answer_choices = item3[1]
        self.assertEqual(answer_choices['question']['answer_type']['slug'], 'answer_choices')
        self.assertEqual(answer_choices['value'], [{
            'key': str(self.children_choices[2].pk),
            'slug': str(self.children_choices[2].pk),
            'text': 'Wed',
        }])

    def test_all_answer_types(self):
        cleaned_data = {
            self.questions[0].param_slug: 'hello world',
            self.questions[1].param_slug: 'hello\nworld',
            self.questions[2].param_slug: 42,
            self.questions[3].param_slug: 'wow@yandex.ru',
            self.questions[4].param_slug: True,
            self.questions[5].param_slug: 'https://yandex.ru',
            self.questions[6].param_slug: '+7 800 789 12 34',
            self.questions[7].param_slug: 'Your name',
            self.questions[8].param_slug: 'Your surname',
            self.questions[9].param_slug: dict(date_start=date(2019, 4, 19)),
            self.questions[10].param_slug: [
                '/101/1234567890_file1.txt',
                '/102/0987654321_file2.txt',
            ],
            self.questions[11].param_slug: [
                DataSourceItem(identity=str(self.choices[1].pk)),
            ],
            '%s__%s' % (self.children[0].param_slug, 0): 'Item1',
            '%s__%s' % (self.children[1].param_slug, 0): True,
            '%s__%s' % (self.children[0].param_slug, 1): 'Item2',
            '%s__%s' % (self.children[1].param_slug, 1): False,
            '%s__%s' % (self.children[2].param_slug, 1): [
                DataSourceItem(identity=str(self.children_choices[1].pk)),
            ],
            '%s__%s' % (self.children[1].param_slug, 2): True,
            '%s__%s' % (self.children[2].param_slug, 2): [
                DataSourceItem(identity=str(self.children_choices[2].pk)),
            ],
        }

        response = get_answer_data(self.survey, self.answer, cleaned_data)
        self.assertIn('data', response)
        data = response['data']
        self.assertEqual(len(data), 13)

        self.assertEqual(data[0]['question']['answer_type']['slug'], 'answer_short_text')
        self.assertEqual(data[0]['value'], 'hello world')

        self.assertEqual(data[1]['question']['answer_type']['slug'], 'answer_long_text')
        self.assertEqual(data[1]['value'], 'hello\nworld')

        self.assertEqual(data[2]['question']['answer_type']['slug'], 'answer_number')
        self.assertEqual(data[2]['value'], 42)

        self.assertEqual(data[3]['question']['answer_type']['slug'], 'answer_non_profile_email')
        self.assertEqual(data[3]['value'], 'wow@yandex.ru')

        self.assertEqual(data[4]['question']['answer_type']['slug'], 'answer_boolean')
        self.assertEqual(data[4]['value'], True)

        self.assertEqual(data[5]['question']['answer_type']['slug'], 'answer_url')
        self.assertEqual(data[5]['value'], 'https://yandex.ru')

        self.assertEqual(data[6]['question']['answer_type']['slug'], 'answer_phone')
        self.assertEqual(data[6]['value'], '+7 800 789 12 34')

        self.assertEqual(data[7]['question']['answer_type']['slug'], 'answer_name')
        self.assertEqual(data[7]['value'], 'Your name')

        self.assertEqual(data[8]['question']['answer_type']['slug'], 'answer_surname')
        self.assertEqual(data[8]['value'], 'Your surname')

        self.assertEqual(data[9]['question']['answer_type']['slug'], 'answer_date')
        self.assertEqual(data[9]['value'], '2019-04-19')

        self.assertEqual(data[10]['question']['answer_type']['slug'], 'answer_files')
        self.assertEqual(data[10]['value'], [{
            'path': '/101/1234567890_file1.txt',
            'name': 'file1.txt',
            'size': 112233,
            'namespace': 'forms',
        },
        {
            'path': '/102/0987654321_file2.txt',
            'name': 'file2.txt',
            'size': 332211,
            'namespace': 'forms',
        }])

        self.assertEqual(data[11]['question']['answer_type']['slug'], 'answer_choices')
        self.assertEqual(data[11]['value'], [{
            'key': str(self.choices[1].pk),
            'slug': 'x-two',
            'text': 'Two',
        }])

        self.assertEqual(data[12]['question']['answer_type']['slug'], 'answer_group')

        self.assertEqual(data[12]['value'][0][0]['question']['answer_type']['slug'], 'answer_short_text')
        self.assertEqual(data[12]['value'][0][0]['value'], 'Item1')

        self.assertEqual(data[12]['value'][0][1]['question']['answer_type']['slug'], 'answer_boolean')
        self.assertEqual(data[12]['value'][0][1]['value'], True)

        self.assertEqual(data[12]['value'][1][0]['question']['answer_type']['slug'], 'answer_short_text')
        self.assertEqual(data[12]['value'][1][0]['value'], 'Item2')

        self.assertEqual(data[12]['value'][1][1]['question']['answer_type']['slug'], 'answer_boolean')
        self.assertEqual(data[12]['value'][1][1]['value'], False)

        self.assertEqual(data[12]['value'][1][2]['question']['answer_type']['slug'], 'answer_choices')
        self.assertEqual(data[12]['value'][1][2]['value'], [{
            'key': str(self.children_choices[1].pk),
            'slug': str(self.children_choices[1].pk),
            'text': 'Tue',
        }])

        self.assertEqual(data[12]['value'][2][0]['question']['answer_type']['slug'], 'answer_boolean')
        self.assertEqual(data[12]['value'][2][0]['value'], True)

        self.assertEqual(data[12]['value'][2][1]['question']['answer_type']['slug'], 'answer_choices')
        self.assertEqual(data[12]['value'][2][1]['value'], [{
            'key': str(self.children_choices[2].pk),
            'slug': str(self.children_choices[2].pk),
            'text': 'Wed',
        }])

    def test_answer_payment(self):
        question = self.questions[13]

        cleaned_data = {
            question.param_slug: {
                'amount': 170,
                'payment_method': 'AC',
            },
        }
        response = get_answer_data(self.survey, self.answer, cleaned_data)
        self.assertIn('data', response)
        data = response['data']
        self.assertTrue(len(data) > 0)
        answer_0 = data[0]

        question_data = answer_0['question']
        self.assertEqual(question_data['options']['account_id'], '12345678')
        answer_type_data = question_data['answer_type']
        self.assertEqual(answer_type_data['slug'], 'answer_payment')

        self.assertIn('value', answer_0)
        value = answer_0['value']
        self.assertEqual(value, {
            'amount': 170,
            'payment_method': 'AC',
        })

    def test_quiz_scores(self):
        first_question = self.questions[0]
        first_question.param_quiz = {
            'enabled': True,
            'answers': [
                {'correct': True, 'scores': 1, 'value': 'Hello'},
                {'correct': True, 'scores': 2, 'value': 'World'},
            ],
        }
        first_question.save()

        second_question = self.questions[11]
        second_question.param_quiz = {
            'enabled': True,
            'required': True,
            'answers': [
                {'correct': True, 'scores': 1, 'value': 'One'},
                {'correct': True, 'scores': 3, 'value': 'Three'},
            ],
        }
        second_question.param_is_allow_multiple_choice = True
        second_question.save()

        image = ImageFactory()
        self.survey.extra = {
            'quiz': {
                'calc_method': 'range',
                'items': [
                    {'title': 'title1', 'description': 'description1'},
                    {'title': 'title2', 'description': 'description2', 'image_id': image.pk},
                ],
            }
        }
        self.survey.save()

        cleaned_data = {
            first_question.param_slug: 'Hello',
            second_question.param_slug: [
                DataSourceItem(identity=str(self.choices[0].pk)),
                DataSourceItem(identity=str(self.choices[2].pk)),
            ],
        }
        response = get_answer_data(self.survey, self.answer, cleaned_data)
        self.assertIn('data', response)
        data = response['data']
        self.assertEqual(len(data), 2)
        answer_0 = data[0]
        self.assertEqual(answer_0['scores'], 1)
        answer_1 = data[1]
        self.assertEqual(answer_1['scores'], 4)

        self.assertIn('quiz', response)
        quiz = response['quiz']
        self.assertEqual(quiz['scores'], 5)
        self.assertEqual(quiz['total_scores'], 6)
        self.assertEqual(quiz['title'], 'title2')
        self.assertEqual(quiz['description'], 'description2')
        self.assertEqual(quiz['image_path'], image.image.name)


class TestScoresData(TestCase):
    fixtures = ['initial_data.json']

    def test_should_return_text_question_scores(self):
        question = SurveyQuestionFactory(
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            param_quiz={
                'enabled': True,
                'answers': [
                    {'correct': True, 'scores': 1, 'value': 'one'},
                    {'correct': True, 'scores': 2, 'value': 'two'},
                ],
            },
        )
        scores_data = get_scores_data(question, 'one')
        self.assertEqual(scores_data, 1)

        scores_data = get_scores_data(question, 'two')
        self.assertEqual(scores_data, 2)

        scores_data = get_scores_data(question, 'three')
        self.assertEqual(scores_data, 0)

    def test_shouldnt_return_scores_for_disabled_param_quiz(self):
        question = SurveyQuestionFactory(
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            param_quiz={
                'enabled': False,
                'answers': [
                    {'correct': True, 'scores': 1, 'value': 'one'},
                    {'correct': True, 'scores': 2, 'value': 'two'},
                ],
            },
        )
        scores_data = get_scores_data(question, 'one')
        self.assertIsNone(scores_data)

    def test_shouldnt_return_scores_for_empty_param_quiz(self):
        question = SurveyQuestionFactory(
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
        )
        scores_data = get_scores_data(question, 'one')
        self.assertIsNone(scores_data)

    def test_should_return_radio_buttons_question_scores(self):
        question = SurveyQuestionFactory(
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_quiz={
                'enabled': True,
                'answers': [
                    {'correct': True, 'scores': 1, 'value': 'one'},
                    {'correct': True, 'scores': 2, 'value': 'two'},
                ],
            },
        )
        SurveyQuestionChoiceFactory(survey_question=question, label='one')
        SurveyQuestionChoiceFactory(survey_question=question, label='two')
        SurveyQuestionChoiceFactory(survey_question=question, label='three')

        scores_data = get_scores_data(question, [{'text': 'one'}])
        self.assertEqual(scores_data, 1)

        scores_data = get_scores_data(question, [{'text': 'two'}])
        self.assertEqual(scores_data, 2)

        scores_data = get_scores_data(question, [])
        self.assertEqual(scores_data, 0)

    def test_should_return_check_buttons_question_scores(self):
        question = SurveyQuestionFactory(
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_is_allow_multiple_choice=True,
            param_quiz={
                'enabled': True,
                'required': True,
                'answers': [
                    {'correct': True, 'scores': 1, 'value': 'one'},
                    {'correct': True, 'scores': 2, 'value': 'two'},
                ],
            },
        )
        SurveyQuestionChoiceFactory(survey_question=question, label='one')
        SurveyQuestionChoiceFactory(survey_question=question, label='two')
        SurveyQuestionChoiceFactory(survey_question=question, label='three')

        scores_data = get_scores_data(question, [{'text': 'one'}])
        self.assertEqual(scores_data, 0)

        scores_data = get_scores_data(question, [{'text': 'two'}])
        self.assertEqual(scores_data, 0)

        scores_data = get_scores_data(question, [{'text': 'one'}, {'text': 'two'}])
        self.assertEqual(scores_data, 3)

        scores_data = get_scores_data(question, [{'text': 'one'}, {'text': 'two'}, {'text': 'three'}])
        self.assertEqual(scores_data, 0)

        scores_data = get_scores_data(question, [])
        self.assertEqual(scores_data, 0)

    def test_should_return_radio_buttons_question_scores_with_wrong_required(self):
        question = SurveyQuestionFactory(
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_is_allow_multiple_choice=False,
            param_quiz={
                'enabled': True,
                'required': True,
                'answers': [
                    {'correct': True, 'scores': 1, 'value': 'one'},
                    {'correct': True, 'scores': 2, 'value': 'two'},
                ],
            },
        )
        SurveyQuestionChoiceFactory(survey_question=question, label='one')
        SurveyQuestionChoiceFactory(survey_question=question, label='two')
        SurveyQuestionChoiceFactory(survey_question=question, label='three')

        scores_data = get_scores_data(question, [{'text': 'one'}])
        self.assertEqual(scores_data, 1.0)

        scores_data = get_scores_data(question, [{'text': 'two'}])
        self.assertEqual(scores_data, 2.0)

        scores_data = get_scores_data(question, [{'text': 'three'}])
        self.assertEqual(scores_data, 0.0)

        scores_data = get_scores_data(question, [])
        self.assertEqual(scores_data, 0.0)

    def test_make_decimal(self):
        question = SurveyQuestionFactory()

        self.assertEqual(make_decimal(question, 'one'), 'one')
        self.assertEqual(make_decimal(question, '1.0'), '1.0')
        self.assertEqual(make_decimal(question, '1,0'), '1,0')

        question.validator_type = ValidatorType.objects.get(slug='decimal')
        question.save()

        self.assertEqual(make_decimal(question, 'one'), 'one')
        self.assertEqual(make_decimal(question, '1.0'), '1.0')
        self.assertEqual(make_decimal(question, '1,0'), '1.0')

    def test_should_return_decimal_question_scores(self):
        question = SurveyQuestionFactory(
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            param_quiz={
                'enabled': True,
                'answers': [
                    {'correct': True, 'scores': 1, 'value': '1.23'},
                    {'correct': True, 'scores': 2, 'value': '2.34'},
                ],
            },
            validator_type=ValidatorType.objects.get(slug='decimal'),
        )
        scores_data = get_scores_data(question, '1.23')
        self.assertEqual(scores_data, 1)

        scores_data = get_scores_data(question, '1,23')
        self.assertEqual(scores_data, 1)

        scores_data = get_scores_data(question, '2.34')
        self.assertEqual(scores_data, 2)

        scores_data = get_scores_data(question, '2,34')
        self.assertEqual(scores_data, 2)

        scores_data = get_scores_data(question, '3')
        self.assertEqual(scores_data, 0)


class TestQuizData(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.image = ImageFactory()
        self.survey = SurveyFactory(
            extra={
                'quiz': {
                    'calc_method': 'range',
                    'items': [
                        {'title': 'title1', 'description': 'description1', 'image_id': self.image.pk},
                        {'title': 'title2', 'description': 'description2'},
                    ],
                },
            },
        )
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            param_quiz={
                'enabled': True,
                'answers': [
                    {'scores': 1, 'value': 'one', 'correct': True},
                    {'scores': 2, 'value': 'two', 'correct': True},
                    {'scores': 4, 'value': 'four', 'correct': True},
                ],
            },
        )

    def test_quiz_data_with_first_range(self):
        expected = {
            'total_scores': 4,
            'scores': 0,
            'question_count': 1,
            'title': 'title1',
            'description': 'description1',
            'image_path': self.image.image.name,
        }
        self.assertEqual(get_quiz_data(self.survey, 0), expected)
        expected = {
            'total_scores': 4,
            'scores': 1,
            'question_count': 1,
            'title': 'title1',
            'description': 'description1',
            'image_path': self.image.image.name,
        }
        self.assertEqual(get_quiz_data(self.survey, 1), expected)

    def test_quiz_data_with_second_range(self):
        expected = {
            'total_scores': 4,
            'scores': 4,
            'question_count': 1,
            'title': 'title2',
            'description': 'description2',
            'image_path': None,
        }
        self.assertEqual(get_quiz_data(self.survey, 4), expected)

    def test_quiz_data_with_disabled_quiz_info(self):
        self.survey.extra = {
            'quiz':  {
                'enabled': False,
            }
        }
        self.survey.save()
        expected = {
            'total_scores': 4,
            'scores': 4,
            'question_count': 1,
            'title': None,
            'description': None,
            'image_path': None,
        }
        self.assertEqual(get_quiz_data(self.survey, 4), expected)

    def test_quiz_data_without_quiz_info(self):
        self.survey.extra = {
            'quiz': None,
        }
        self.survey.save()
        expected = {
            'total_scores': 4,
            'scores': 4,
            'question_count': 1,
            'title': None,
            'description': None,
            'image_path': None,
        }
        self.assertEqual(get_quiz_data(self.survey, 4), expected)

    def test_quiz_data_with_invalid_range(self):
        expected = {
            'total_scores': 4.0,
            'question_count': 1,
            'scores': 42,
            'title': None,
            'description': None,
            'image_path': None,
        }
        self.assertDictEqual(get_quiz_data(self.survey, 42), expected)

    def test_quiz_data_without_test_questions(self):
        self.question.param_quiz = None
        self.question.save()
        self.assertIsNone(get_quiz_data(self.survey, 0))

    def test_quiz_data_with_disabled_test_questions(self):
        param_quiz = self.question.param_quiz
        param_quiz['enabled'] = False
        self.question.param_quiz = param_quiz
        self.question.save()
        self.assertIsNone(get_quiz_data(self.survey, 0))


class TestQuizAttr(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory(is_published_external=True)
        self.question = SurveyQuestionFactory(
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            survey=self.survey,
        )

    def test_shouldnt_create_quiz_attr(self):
        data = {
            self.question.param_slug: 'testit',
        }
        response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/', data=data, format='json')

        self.assertEqual(response.status_code, 200)
        answer = ProfileSurveyAnswer.objects.get(pk=response.data['answer_id'])
        self.assertIsNotNone(answer.data)
        self.assertFalse('quiz' in answer.data)

    def test_should_create_quiz_attr(self):
        self.question.param_quiz = {
            'enabled': True,
        }
        self.question.save()
        data = {
            self.question.param_slug: 'testit',
        }
        response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/', data=data, format='json')

        self.assertEqual(response.status_code, 200)
        answer = ProfileSurveyAnswer.objects.get(pk=response.data['answer_id'])
        self.assertIsNotNone(answer.data)
        self.assertTrue('quiz' in answer.data)
