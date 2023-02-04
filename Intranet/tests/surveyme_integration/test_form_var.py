# -*- coding: utf-8 -*-
from django.test import TestCase, override_settings

from events.accounts.factories import UserFactory
from events.accounts.factories import OrganizationToGroupFactory
from events.data_sources.factories import TableRowFactory
from events.surveyme.factories import (
    ProfileSurveyAnswerFactory,
    SurveyQuestionFactory,
    SurveyQuestionChoiceFactory,
    SurveyFactory,
)
from events.surveyme.models import AnswerType
from events.surveyme_integration.variables.form import (
    FormAnswerIdVariable,
    FormIdVariable,
    FormNameVariable,
    FormQuestionAnswerVariable,
    FormQuestionsAnswersVariable,
    FormQuestionAnswerChoicesIdsVariable,
    FormQuestionAnswerChoiceSlugVariable,
    FormValidationVariable,
    FormAuthorEmailVariable,
)


class TestFormAnswerIdVariable(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.answer = ProfileSurveyAnswerFactory()

    def test_should_return_answer_id(self):
        var = FormAnswerIdVariable(answer=self.answer)
        self.assertEqual(var.get_value(), self.answer.id)


class TestFormIdVariable(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.answer = ProfileSurveyAnswerFactory()

    def test_should_return_form_id(self):
        var = FormIdVariable(answer=self.answer)
        self.assertEqual(var.get_value(), self.answer.survey.id)


class TestFormNameVariable(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.answer = ProfileSurveyAnswerFactory()

    def test_should_return_form_name(self):
        self.answer.survey.name = 'my form'
        var = FormNameVariable(answer=self.answer)
        self.assertEqual(var.get_value(), 'my form')


class TestFormQuestionAnswerVariable(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.answer = ProfileSurveyAnswerFactory()
        self.survey = self.answer.survey

    def test_answer_for_choices_question(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_choices')
        )
        # create choices
        choices = [
            SurveyQuestionChoiceFactory(
                label=i,
                survey_question=question
            )
            for i in ['one', 'two', 'three']
        ]
        # create question answer
        self.answer.data = {
            'data': [
                {
                    'question': {
                        'id': question.pk,
                    },
                    'value': [
                        {'key': str(choices[0].pk), 'text': choices[0].label},
                        {'key': str(choices[1].pk), 'text': choices[1].label},
                    ],
                },
            ],
        }
        self.answer.save()
        var = FormQuestionAnswerVariable(answer=self.answer, question=question.id)
        self.assertEqual(var.get_value(), 'one, two')

    def test_answer_for_short_text(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text')
        )
        # create question answer
        self.answer.data = {
            'data': [
                {
                    'question': {
                        'id': question.pk,
                    },
                    'value': 'hello world',
                },
            ],
        }
        self.answer.save()
        var = FormQuestionAnswerVariable(answer=self.answer, question=question.id)
        self.assertEqual(var.get_value(), 'hello world')


class TestFormQuestionAnswerVariable__group_question(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()
        self.group_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_group')
        )
        self.short_group_text = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            group=self.group_question
        )
        self.another_short_group_text = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            group=self.group_question
        )
        self.long_group_text = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_long_text'),
            group=self.group_question
        )

        self.profile_survey_answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            data={
                'data': [
                    {
                        'question': {
                            'id': self.group_question.pk,
                        },
                        'value': [
                            [
                                {
                                    'question': {
                                        'id': self.short_group_text.pk,
                                    },
                                    'value': 'smth short',
                                },
                            ],
                            [
                                {
                                    'question': {
                                        'id': self.short_group_text.pk,
                                    },
                                    'value': 'smth short another',
                                },
                                {
                                    'question': {
                                        'id': self.long_group_text.pk,
                                    },
                                    'value': 'long text',
                                },
                            ],
                        ],
                    },
                ],
            },
        )

    def test_group_question(self):
        var = FormQuestionAnswerVariable(
            answer=self.profile_survey_answer,
            question=self.group_question.id,
            only_with_value=True,
        )
        self.assertEqual(
            var.get_value(),
            '{group}\n{short} - smth short\n\n{group}\n{short} - smth short another\n{long} - long text'.format(
                group=self.group_question.label, short=self.short_group_text.label,
                long=self.long_group_text.label,
            ),
        )

    def test_group_question_not_created_already(self):
        var = FormQuestionAnswerVariable(
            answer=self.profile_survey_answer,
            question=self.group_question.id,
        )
        self.assertEqual(
            var.get_value(),
            '{group}\n{short} - smth short\n\n{group}\n{short} - smth short another\n{long} - long text'.format(
                group=self.group_question.label, short=self.short_group_text.label,
                long=self.long_group_text.label,
            ),
        )

    def test_group_question_with_no_answer(self):
        self.profile_survey_answer.data = {
            'data': [
                {
                    'question': {
                        'id': self.group_question.pk,
                    },
                    'value': [
                    ],
                },
            ],
        }
        self.profile_survey_answer.save()
        var = FormQuestionAnswerVariable(
            answer=self.profile_survey_answer,
            question=self.group_question.id,
        )
        self.assertEqual(var.get_value(), '')

    def test_group_question_with_no_answer_not_created_already(self):
        self.profile_survey_answer.data = {
            'data': [
            ],
        }
        self.profile_survey_answer.save()
        var = FormQuestionAnswerVariable(
            answer=self.profile_survey_answer,
            question=self.group_question.id,
        )
        self.assertEqual(var.get_value(), '')

    def test_question_in_group_with_multiple_answer(self):
        var = FormQuestionAnswerVariable(
            answer=self.profile_survey_answer,
            question=self.short_group_text.id,
        )
        self.assertEqual(var.get_value(), 'smth short, smth short another')

    def test_question_in_group_with_single_answer(self):
        var = FormQuestionAnswerVariable(
            answer=self.profile_survey_answer,
            question=self.long_group_text.id,
        )
        self.assertEqual(var.get_value(), 'long text')

    def test_question_in_group_with_no_answer(self):
        var = FormQuestionAnswerVariable(
            answer=self.profile_survey_answer,
            question=self.another_short_group_text.id,
        )
        self.assertEqual(var.get_value(), '')


class TestFormQuestionAnswersVariable(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.answer = ProfileSurveyAnswerFactory()
        self.survey = self.answer.survey
        self.questions = []

        # choices
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            label="Your age"
        )
        # create choices
        self.choices = [
            SurveyQuestionChoiceFactory(
                label=i,
                survey_question=question
            )
            for i in ['one', 'two', 'three']
        ]
        self.questions.append(question)

        # short question
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            label='How are you?'
        )
        self.questions.append(question)

        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_name'),
            label="Your name?"
        )
        self.questions.append(question)

        # create question answer
        self.answer.data = {
            'data': [
                {
                    'question': {
                        'id': self.questions[0].pk,
                    },
                    'value': [
                        {'key': str(self.choices[0].pk), 'text': self.choices[0].label},
                        {'key': str(self.choices[1].pk), 'text': self.choices[1].label},
                    ],
                },
                {
                    'question': {
                        'id': self.questions[1].pk,
                    },
                    'value': "I'm good",
                },
                {
                    'question': {
                        'id': self.questions[2].pk,
                    },
                    'value': 'Myname',
                },
            ],
        }
        self.answer.save()

    def test_should_return_all_questions_if__is_all_questions(self):
        expected = ('Your age:\n'
                    'one, two\n'
                    '\n'
                    'How are you?:\n'
                    "I'm good\n"
                    '\n'
                    'Your name?:\n'
                    'Myname')
        var = FormQuestionsAnswersVariable(answer=self.answer, is_all_questions=True)
        self.assertEqual(var.get_value(), expected)
        self.assertEqual(var.get_value(format_name='txt'), expected, msg='should use txt formatting by default')

        # startrek formatting
        var = FormQuestionsAnswersVariable(answer=self.answer, is_all_questions=True)
        expected = ('**Your age**\n'
                    '%%\n'
                    'one, two\n'
                    '%%\n'
                    '\n'
                    '**How are you?**\n'
                    '%%\n'
                    "I'm good\n"
                    '%%\n'
                    '\n'
                    '**Your name?**\n'
                    '%%\n'
                    'Myname\n'
                    '%%\n')
        self.assertEqual(var.get_value(format_name='startrek'), expected)

    def test_should_return_only_some_questions_if_not_all(self):
        expected = ('Your age:\n'
                    'one, two\n'
                    '\n'
                    'How are you?:\n'
                    "I'm good")
        var = FormQuestionsAnswersVariable(
            answer=self.answer,
            questions=[self.questions[0].id, self.questions[1].id],
            is_all_questions=False,
            only_with_value=True,
        )
        self.assertEqual(var.get_value(format_name='txt'), expected, msg='should use txt formatting by default')

        # startrek formatting
        expected = ('**Your age**\n'
                    '%%\n'
                    'one, two\n'
                    '%%\n'
                    '\n'
                    '**How are you?**\n'
                    '%%\n'
                    "I'm good\n"
                    '%%\n')
        self.assertEqual(var.get_value(format_name='startrek'), expected)

    def test_should_return_only_answers_for_questions_with_value_if_argument_is_true(self):
        # with values
        expected = ('Your age:\n'
                    'one, two\n'
                    '\n'
                    'How are you?:\n'
                    "I'm good")
        var = FormQuestionsAnswersVariable(
            answer=self.answer,
            questions=[self.questions[0].id, self.questions[1].id],
            only_with_value=True
        )
        self.assertEqual(var.get_value(), expected)

        # with value is empty
        self.answer.data = {
            'data': [
                {
                    'question': {
                        'id': self.questions[0].pk,
                    },
                    'value': [
                        {'key': str(self.choices[0].pk), 'text': self.choices[0].label},
                        {'key': str(self.choices[1].pk), 'text': self.choices[1].label},
                    ],
                },
                {
                    'question': {
                        'id': self.questions[1].pk,
                    },
                    'value': '',
                },
            ],
        }
        self.answer.save()
        expected = ('Your age:\n'
                    'one, two')
        var = FormQuestionsAnswersVariable(
            answer=self.answer,
            questions=[self.questions[0].id, self.questions[1].id],
            only_with_value=True
        )
        self.assertEqual(var.get_value(), expected)

        # with no answer to question
        self.answer.data = {
            'data': [
                {
                    'question': {
                        'id': self.questions[0].pk,
                    },
                    'value': [
                        {'key': str(self.choices[0].pk), 'text': self.choices[0].label},
                        {'key': str(self.choices[1].pk), 'text': self.choices[1].label},
                    ],
                },
            ],
        }
        self.answer.save()
        expected = ('Your age:\n'
                    'one, two')
        var = FormQuestionsAnswersVariable(
            answer=self.answer,
            questions=[self.questions[0].id, self.questions[1].id],
            only_with_value=True
        )
        self.assertEqual(var.get_value(), expected)


class TestFormQuestionAnswersVariable__group_question(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()
        self.group_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_group')
        )
        self.short_group_text = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            group=self.group_question
        )
        self.another_short_group_text = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            group=self.group_question
        )
        self.long_group_text = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_long_text'),
            group=self.group_question
        )
        self.profile_survey_answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            data={
                'data': [
                    {
                        'question': {
                            'id': self.group_question.pk,
                        },
                        'value': [
                            [
                                {
                                    'question': {
                                        'id': self.short_group_text.pk,
                                    },
                                    'value': 'smth short',
                                },
                            ],
                            None,
                            [
                                {
                                    'question': {
                                        'id': self.short_group_text.pk,
                                    },
                                    'value': 'smth short another',
                                },
                                {
                                    'question': {
                                        'id': self.long_group_text.pk,
                                    },
                                    'value': 'long text',
                                },
                            ],
                        ],
                    },
                ],
            },
        )

    def test_for_all_questions(self):
        var = FormQuestionsAnswersVariable(
            answer=self.profile_survey_answer,
            is_all_questions=True,
            only_with_value=True,
        )
        expected = (
            '{question1}:'
            '\n{question1}'
            '\n{question2} - smth short'
            '\n\n{question1}'
            '\n{question2} - smth short another'
            '\n{question4} - long text').format(
                question1=self.group_question.label,
                question2=self.short_group_text.label,
                question4=self.long_group_text.label,
            )
        self.assertEqual(var.get_value(), expected)

    def test_for_all_questions_for_not_created_group(self):
        self.profile_survey_answer.data = {
            'data': [
            ],
        }
        self.profile_survey_answer.save()
        var = FormQuestionsAnswersVariable(
            answer=self.profile_survey_answer,
            only_with_value=False,
            is_all_questions=True
        )
        expected = (
            '{question1}:\nНет ответа').format(
                question1=self.group_question.label,
            )
        self.assertEqual(var.get_value(), expected)

    def test_for_group_question_without_answers(self):
        self.profile_survey_answer.data = {
            'data': [
                {
                    'question': {
                        'id': self.group_question.pk,
                    },
                    'value': [
                    ],
                },
            ],
        }
        self.profile_survey_answer.save()
        var = FormQuestionsAnswersVariable(
            answer=self.profile_survey_answer,
            questions=[self.group_question.id],
            only_with_value=False,
            is_all_questions=False
        )
        expected = (
            '{question1}:\nНет ответа').format(
                question1=self.group_question.label,
            )
        self.assertEqual(var.get_value(), expected)

    def test_for_group_question(self):
        var = FormQuestionsAnswersVariable(
            answer=self.profile_survey_answer,
            questions=[self.group_question.id],
            is_all_questions=False,
            only_with_value=True,
        )
        expected = (
            '{question1}:'
            '\n{question1}'
            '\n{question2} - smth short'
            '\n\n{question1}'
            '\n{question2} - smth short another'
            '\n{question4} - long text').format(
                question1=self.group_question.label,
                question2=self.short_group_text.label,
                question4=self.long_group_text.label,
            )
        self.assertEqual(var.get_value(), expected)


class TestFormQuestionAnswerChoicesIdsVariable(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.answer = ProfileSurveyAnswerFactory()
        self.survey = self.answer.survey

    def test_answer_for_choices_question(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_choices')
        )
        # create choices
        choices = []
        for i in ['one', 'two', 'three']:
            choices.append(SurveyQuestionChoiceFactory(
                label=i,
                survey_question=question
            ))
        self.answer.data = {
            'data': [{
                'question': question.get_answer_info(),
                'value': [
                    {
                        'key': str(i.pk),
                        'slug': i.slug,
                        'text': i.label,
                    }
                    for i in choices[:2]
                ],
            }],
        }
        self.answer.save()
        var = FormQuestionAnswerChoicesIdsVariable(answer=self.answer, question=question.id)
        self.assertEqual(var.get_value(), ','.join([str(c.pk) for c in choices[:2]]))

    def test_answer_for_non_choices_question(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text')
        )
        self.answer.data = {
            'data': [{
                'question': question.get_answer_info(),
                'value': 'hello world',
            }],
        }
        self.answer.save()
        var = FormQuestionAnswerChoicesIdsVariable(answer=self.answer, question=question.id)
        self.assertEqual(var.get_value(), '')

    def test_answer_for_group_with_choices_question(self):
        group_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_group'),
            param_data_source='survey_question_choice',
        )
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='survey_question_choice',
            group=group_question,
        )
        # create choices
        choices = []
        for i in ['one', 'two', 'three']:
            choices.append(SurveyQuestionChoiceFactory(
                label=i,
                survey_question=question,
                slug=i,
            ))
        self.answer.data = {
            'data': [{
                'question': group_question.get_answer_info(),
                'value': [
                    [
                        {
                            'question': question.get_answer_info(),
                            'value': [
                                {
                                    'key': str(choices[0].pk),
                                    'slug': choices[0].slug,
                                },
                                {
                                    'key': str(choices[2].pk),
                                    'slug': choices[2].slug,
                                },
                            ],
                        },
                    ],
                    [],
                    [
                        {
                            'question': question.get_answer_info(),
                            'value': [
                                {
                                    'key': str(choices[1].pk),
                                    'slug': choices[1].slug,
                                },
                                {
                                    'key': str(choices[2].pk),
                                    'slug': choices[2].slug,
                                },
                            ],
                        },
                    ],
                ],
            }],
        }
        self.answer.save()
        var = FormQuestionAnswerChoicesIdsVariable(answer=self.answer, question=question.id)
        self.assertEqual(var.get_value(), f'{choices[0].pk},{choices[2].pk},{choices[1].pk},{choices[2].pk}')

        self.answer.data = {
            'data': [{
                'question': group_question.get_answer_info(),
                'value': [],
            }],
        }
        self.answer.save()
        var = FormQuestionAnswerChoicesIdsVariable(answer=self.answer, question=question.id)
        self.assertEqual(var.get_value(), '')

        self.answer.data = {
            'data': [{
                'question': group_question.get_answer_info(),
                'value': None,
            }],
        }
        self.answer.save()
        var = FormQuestionAnswerChoicesIdsVariable(answer=self.answer, question=question.id)
        self.assertEqual(var.get_value(), '')

        var = FormQuestionAnswerChoicesIdsVariable(answer=self.answer, question=99999)
        self.assertEqual(var.get_value(), '')


@override_settings(IS_BUSINESS_SITE=True)
class TestFormQuestionAnswerDirUserVariable(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.o2g = OrganizationToGroupFactory()
        self.answer = ProfileSurveyAnswerFactory()
        self.survey = self.answer.survey
        self.survey.org = self.o2g.org
        self.survey.save()

    def test_answer_for_choices_question(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='dir_user',
        )
        # create question answer
        self.answer.data = {
            'data': [
                {
                    'question': {
                        'id': question.pk,
                    },
                    'value': [
                        {'key': 'ivanov', 'text': 'Ivan Ivanov (ivanov)'},
                    ],
                }
            ],
        }
        self.answer.save()
        var = FormQuestionAnswerVariable(answer=self.answer, question=question.id)
        self.assertEqual(var.get_value(), 'Ivan Ivanov (ivanov)')


class TestFormQuestionAnswerChoiceSlugVariable(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.answer = ProfileSurveyAnswerFactory()
        self.survey = self.answer.survey

    def test_answer_for_choices_question(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='survey_question_choice',
        )
        # create choices
        choices = []
        for i in ['one', 'two', 'three']:
            choices.append(SurveyQuestionChoiceFactory(
                label=i,
                survey_question=question,
                slug=i,
            ))
        self.answer.data = {
            'data': [{
                'question': question.get_answer_info(),
                'value': [
                    {
                        'key': str(i.pk),
                        'slug': i.slug,
                        'text': i.label,
                    }
                    for i in choices[:2]
                ],
            }],
        }
        self.answer.save()
        var = FormQuestionAnswerChoiceSlugVariable(answer=self.answer, question=question.id)
        self.assertEqual(var.get_value(), ','.join([str(c.slug) for c in choices[:2]]))

    def test_answer_for_data_source_question(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='test_data_source',
        )
        # create choices
        choices = []
        for i in ['one', 'two', 'three']:
            choices.append(SurveyQuestionChoiceFactory(
                label=i,
                survey_question=question,
                slug=i,
            ))
        self.answer.data = {
            'data': [{
                'question': question.get_answer_info(),
                'value': [
                    {
                        'key': str(i.pk),
                        'slug': i.slug,
                        'text': i.label,
                    }
                    for i in choices[:2]
                ],
            }],
        }
        self.answer.save()
        var = FormQuestionAnswerChoiceSlugVariable(answer=self.answer, question=question.id)
        self.assertEqual(var.get_value(), ','.join([str(c.slug) for c in choices[:2]]))

    def test_answer_for_non_choices_question(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text')
        )
        self.answer.data = {
            'data': [{
                'question': question.get_answer_info(),
                'value': 'hello world',
            }],
        }
        self.answer.save()
        var = FormQuestionAnswerChoicesIdsVariable(answer=self.answer, question=question.id)
        self.assertEqual(var.get_value(), '')

    def test_yt_table_source_data(self):
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='yt_table_source',
        )
        choices = [
            TableRowFactory(source_id='smth'),
            TableRowFactory(source_id='smth1'),
            TableRowFactory(source_id='smth2'),
        ]
        self.answer.data = {
            'data': [{
                'question': question.get_answer_info(),
                'value': [{
                    'key': str(choices[-1].pk),
                    'slug': choices[-1].source_id,
                    'text': choices[-1].text,
                }],
            }],
        }
        self.answer.save()
        var = FormQuestionAnswerChoiceSlugVariable(answer=self.answer, question=question.id)
        self.assertEqual(var.get_value(), choices[-1].source_id)

    def test_answer_for_group_with_choices_question(self):
        group_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_group'),
            param_data_source='survey_question_choice',
        )
        question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_data_source='survey_question_choice',
            group=group_question,
        )
        # create choices
        choices = []
        for i in ['one', 'two', 'three']:
            choices.append(SurveyQuestionChoiceFactory(
                label=i,
                survey_question=question,
                slug=i,
            ))
        self.answer.data = {
            'data': [{
                'question': group_question.get_answer_info(),
                'value': [
                    [
                        {
                            'question': question.get_answer_info(),
                            'value': [
                                {
                                    'key': str(choices[0].pk),
                                    'slug': choices[0].slug,
                                },
                                {
                                    'key': str(choices[2].pk),
                                    'slug': choices[2].slug,
                                },
                            ],
                        },
                    ],
                    [],
                    [
                        {
                            'question': question.get_answer_info(),
                            'value': [
                                {
                                    'key': str(choices[1].pk),
                                    'slug': choices[1].slug,
                                },
                                {
                                    'key': str(choices[2].pk),
                                    'slug': choices[2].slug,
                                },
                            ],
                        },
                    ],
                ],
            }],
        }
        self.answer.save()
        var = FormQuestionAnswerChoiceSlugVariable(answer=self.answer, question=question.id)
        self.assertEqual(var.get_value(), 'one,three,two,three')

        self.answer.data = {
            'data': [{
                'question': group_question.get_answer_info(),
                'value': [],
            }],
        }
        self.answer.save()
        var = FormQuestionAnswerChoiceSlugVariable(answer=self.answer, question=question.id)
        self.assertEqual(var.get_value(), '')

        self.answer.data = {
            'data': [{
                'question': group_question.get_answer_info(),
                'value': None,
            }],
        }
        self.answer.save()
        var = FormQuestionAnswerChoiceSlugVariable(answer=self.answer, question=question.id)
        self.assertEqual(var.get_value(), '')

        var = FormQuestionAnswerChoiceSlugVariable(answer=self.answer, question=99999)
        self.assertEqual(var.get_value(), '')


class TestFormValidationVariable(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.answer = ProfileSurveyAnswerFactory(source_request={'validation_status': 'error'})

    def test_should_return_validation_status_if_set(self):
        var = FormValidationVariable(answer=self.answer)
        self.assertEqual(var.get_value(), 'error')


class TestFormAuthorEmailVariable(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.user = UserFactory()
        self.survey = SurveyFactory(user=self.user)
        self.answer = ProfileSurveyAnswerFactory(survey=self.survey)

    @override_settings(IS_INTERNAL_SITE=False)
    def test_should_return_form_author_name(self):
        self.answer.survey.name = 'my form'
        var = FormAuthorEmailVariable(answer=self.answer)
        self.assertEqual(var.get_value(), self.user.email)
