# -*- coding: utf-8 -*-
from datetime import timedelta, date
from decimal import Decimal
from django.test import TestCase
from django.utils import timezone
from freezegun import freeze_time

from events.countme.utils import get_counters, get_stat_info
from events.surveyme.models import AnswerType
from events.surveyme.factories import (
    SurveyFactory,
    SurveyQuestionFactory,
    ProfileSurveyAnswerFactory,
)


class TestGetStatInfo(TestCase):  # {{{
    def test_should_return_correct_data_for_base_question_type(self):
        data = {
            'data': [{
                'value': 'https://yandex.ru/',
                'question': {
                    'id': 1,
                    'answer_type': {
                        'slug': 'answer_url',
                    },
                },
            }],
        }
        stat_info = list(get_stat_info(data))
        self.assertEqual(len(stat_info), 1)
        self.assertEqual(stat_info[0], ('answer_url', 1, '', None))

    def test_should_return_correct_data_for_boolean(self):
        data = {
            'data': [{
                'value': True,
                'question': {
                    'id': 2,
                    'answer_type': {
                        'slug': 'answer_boolean',
                    },
                },
            }],
        }
        stat_info = list(get_stat_info(data))
        self.assertEqual(len(stat_info), 1)
        self.assertEqual(stat_info[0], ('answer_boolean', 2, '1', None))

    def test_should_return_correct_data_for_short_text(self):
        data = {
            'data': [{
                'value': 'textme',
                'question': {
                    'id': 3,
                    'answer_type': {
                        'slug': 'answer_short_text',
                    },
                },
            }],
        }
        stat_info = list(get_stat_info(data))
        self.assertEqual(len(stat_info), 1)
        self.assertEqual(stat_info[0], ('answer_short_text', 3, '', None))

    def test_should_return_correct_data_for_group(self):
        data = {
            'data': [{
                'value': [
                    [
                        {
                            'value': 'first',
                            'question': {
                                'id': 5,
                                'answer_type': {
                                    'slug': 'answer_short_text',
                                },
                            },
                        },
                    ],
                    [
                        {
                            'value': 'second',
                            'question': {
                                'id': 5,
                                'answer_type': {
                                    'slug': 'answer_short_text',
                                },
                            },
                        },
                    ],
                ],
                'question': {
                    'id': 4,
                    'answer_type': {
                        'slug': 'answer_group',
                    },
                },
            }],
        }
        stat_info = list(get_stat_info(data))
        self.assertEqual(len(stat_info), 3)
        self.assertEqual(stat_info[0], ('answer_group', 4, None, 2))
        self.assertEqual(stat_info[1], ('answer_short_text', 5, '', None))
        self.assertEqual(stat_info[2], ('answer_short_text', 5, '', None))

    def test_should_return_correct_data_for_choices(self):
        data = {
            'data': [{
                'value': [
                    {
                        'key': '1001', 'slug': None, 'text': 'first',
                    },
                    {
                        'key': '1003', 'slug': None, 'text': 'third',
                    },
                ],
                'question': {
                    'id': 6,
                    'answer_type': {
                        'slug': 'answer_choices',
                    },
                    'options': {
                        'data_source': 'survey_question_choice',
                    },
                },
            }],
        }
        stat_info = list(get_stat_info(data))
        self.assertEqual(len(stat_info), 1)
        self.assertEqual(stat_info[0], ('answer_choices', 6, ['1001', '1003'], None))

    def test_should_return_correct_data_for_data_sources(self):
        data = {
            'data': [{
                'value': [
                    {
                        'key': '1004', 'slug': 'forms', 'text': 'Forms',
                    },
                    {
                        'key': '1005', 'slug': 'tools', 'text': 'Tools',
                    },
                ],
                'question': {
                    'id': 7,
                    'answer_type': {
                        'slug': 'answer_choices',
                    },
                    'options': {
                        'data_source': 'abc_service',
                    },
                },
            }],
        }
        stat_info = list(get_stat_info(data))
        self.assertEqual(len(stat_info), 1)
        self.assertEqual(stat_info[0], ('answer_choices', 7, ['forms', 'tools'], None))

    def test_should_return_correct_data_for_matrix(self):
        data = {
            'data': [{
                'value': [
                    {
                        'row': {'key': '101', 'text': 'Row1'},
                        'col': {'key': '202', 'text': '2'},
                    },
                    {
                        'row': {'key': '102', 'text': 'Row2'},
                        'col': {'key': '201', 'text': '1'},
                    },
                    {
                        'row': {'key': '103', 'text': 'Row3'},
                        'col': {'key': '203', 'text': '3'},
                    },
                ],
                'question': {
                    'id': 8,
                    'answer_type': {
                        'slug': 'answer_choices',
                    },
                    'options': {
                        'data_source': 'survey_question_matrix_choice',
                    },
                },
            }],
        }
        stat_info = list(get_stat_info(data))
        self.assertEqual(len(stat_info), 1)
        self.assertEqual(stat_info[0], ('answer_choices', 8, ['101:202', '102:201', '103:203'], None))

    def test_should_return_correct_data_for_short_text_with_scores(self):
        data = {
            'data': [{
                'value': 'fourtytwo',
                'scores': 2.0,
                'question': {
                    'id': 9,
                    'answer_type': {
                        'slug': 'answer_short_text',
                    },
                },
            }],
        }
        stat_info = list(get_stat_info(data))
        self.assertEqual(len(stat_info), 1)
        self.assertEqual(stat_info[0], ('answer_short_text', 9, 'fourtytwo', 2.0))

    def test_should_return_correct_data_for_choices_with_scores(self):
        data = {
            'data': [{
                'value': [
                    {
                        'key': '1011', 'slug': None, 'text': 'mars',
                    },
                    {
                        'key': '1013', 'slug': None, 'text': 'venus',
                    },
                ],
                'scores': 3.0,
                'question': {
                    'id': 10,
                    'answer_type': {
                        'slug': 'answer_choices',
                    },
                    'options': {
                        'data_source': 'survey_question_choice',
                    },
                },
            }],
        }
        stat_info = list(get_stat_info(data))
        self.assertEqual(len(stat_info), 1)
        self.assertEqual(stat_info[0], ('answer_choices', 10, ['1011', '1013'], 3.0))
# }}}


@freeze_time('2020-06-19T00:02')
class TestGetCounters(TestCase):  # {{{
    def test_should_return_correct_counters_info_without_scores(self):  # {{{
        survey = SurveyFactory()
        ProfileSurveyAnswerFactory(  # {{{
            survey=survey,
            date_created=(timezone.now() - timedelta(minutes=5)),
            data={
                'data': [{
                    'value': 'https://yandex.ru/',
                    'question': {
                        'id': 1,
                        'answer_type': {
                            'slug': 'answer_url',
                        },
                    },
                }],
            },
        )  # }}}
        ProfileSurveyAnswerFactory(  # {{{
            survey=survey,
            date_created=timezone.now() - timedelta(minutes=3),
            data={
                'data': [{
                    'value': True,
                    'question': {
                        'id': 2,
                        'answer_type': {
                            'slug': 'answer_boolean',
                        },
                    },
                }],
            },
        )  # }}}
        ProfileSurveyAnswerFactory(  # {{{
            survey=survey,
            date_created=timezone.now() - timedelta(minutes=3),
            data={
                'data': [{
                    'value': 'textme',
                    'question': {
                        'id': 3,
                        'answer_type': {
                            'slug': 'answer_short_text',
                        },
                    },
                }],
            },
        )  # }}}
        ProfileSurveyAnswerFactory(  # {{{
            survey=survey,
            date_created=timezone.now() - timedelta(minutes=3),
            data={
                'data': [{
                    'value': [
                        [
                            {
                                'value': 'first',
                                'question': {
                                    'id': 5,
                                    'answer_type': {
                                        'slug': 'answer_short_text',
                                    },
                                },
                            },
                        ],
                        [
                            {
                                'value': 'second',
                                'question': {
                                    'id': 5,
                                    'answer_type': {
                                        'slug': 'answer_short_text',
                                    },
                                },
                            },
                        ],
                    ],
                    'question': {
                        'id': 4,
                        'answer_type': {
                            'slug': 'answer_group',
                        },
                    },
                }],
            },
        )  # }}}
        ProfileSurveyAnswerFactory(  # {{{
            survey=survey,
            date_created=timezone.now() - timedelta(minutes=3),
            data={
                'data': [{
                    'value': [
                        {
                            'key': '1001', 'slug': None, 'text': 'first',
                        },
                        {
                            'key': '1003', 'slug': None, 'text': 'third',
                        },
                    ],
                    'question': {
                        'id': 6,
                        'answer_type': {
                            'slug': 'answer_choices',
                        },
                        'options': {
                            'data_source': 'survey_question_choice',
                        },
                    },
                }],
            },
        )  # }}}
        ProfileSurveyAnswerFactory(  # {{{
            survey=survey,
            date_created=timezone.now() - timedelta(minutes=3),
            data={
                'data': [{
                    'value': [
                        {
                            'key': '1004', 'slug': 'forms', 'text': 'Forms',
                        },
                        {
                            'key': '1005', 'slug': 'tools', 'text': 'Tools',
                        },
                    ],
                    'question': {
                        'id': 7,
                        'answer_type': {
                            'slug': 'answer_choices',
                        },
                        'options': {
                            'data_source': 'abc_service',
                        },
                    },
                }],
            },
        )  # }}}
        ProfileSurveyAnswerFactory(  # {{{
            survey=survey,
            date_created=timezone.now() - timedelta(minutes=2),
            data={
                'data': [{
                    'value': [
                        {
                            'row': {'key': '101', 'text': 'Row1'},
                            'col': {'key': '202', 'text': '2'},
                        },
                        {
                            'row': {'key': '102', 'text': 'Row2'},
                            'col': {'key': '201', 'text': '1'},
                        },
                        {
                            'row': {'key': '103', 'text': 'Row3'},
                            'col': {'key': '203', 'text': '3'},
                        },
                    ],
                    'question': {
                        'id': 8,
                        'answer_type': {
                            'slug': 'answer_choices',
                        },
                        'options': {
                            'data_source': 'survey_question_matrix_choice',
                        },
                    },
                }],
            },
        )  # }}}

        started_at = timezone.now()
        last_modified = timezone.now() - timedelta(minutes=4)
        counters = get_counters(started_at, survey.pk, last_modified)

        self.assertEqual(counters.answer_count, 6)

        self.assertEqual(len(counters.answer_count_by_date), 2)
        self.assertEqual(counters.answer_count_by_date[date(2020, 6, 18)], 5)
        self.assertEqual(counters.answer_count_by_date[date(2020, 6, 19)], 1)

        self.assertEqual(len(counters.question_count), 10)
        self.assertEqual(counters.question_count[(2, '1')], 1)
        self.assertEqual(counters.question_count[(3, '')], 1)
        self.assertEqual(counters.question_count[(5, '')], 2)
        self.assertEqual(counters.question_count[(6, '1001')], 1)
        self.assertEqual(counters.question_count[(6, '1003')], 1)
        self.assertEqual(counters.question_count[(7, 'forms')], 1)
        self.assertEqual(counters.question_count[(7, 'tools')], 1)
        self.assertEqual(counters.question_count[(8, '101:202')], 1)
        self.assertEqual(counters.question_count[(8, '102:201')], 1)
        self.assertEqual(counters.question_count[(8, '103:203')], 1)

        self.assertEqual(len(counters.question_count_by_date), 10)
        self.assertEqual(counters.question_count_by_date[(2, '1', date(2020, 6, 18))], 1)
        self.assertEqual(counters.question_count_by_date[(3, '', date(2020, 6, 18))], 1)
        self.assertEqual(counters.question_count_by_date[(5, '', date(2020, 6, 18))], 2)
        self.assertEqual(counters.question_count_by_date[(6, '1001', date(2020, 6, 18))], 1)
        self.assertEqual(counters.question_count_by_date[(6, '1003', date(2020, 6, 18))], 1)
        self.assertEqual(counters.question_count_by_date[(7, 'forms', date(2020, 6, 18))], 1)
        self.assertEqual(counters.question_count_by_date[(7, 'tools', date(2020, 6, 18))], 1)
        self.assertEqual(counters.question_count_by_date[(8, '101:202', date(2020, 6, 19))], 1)
        self.assertEqual(counters.question_count_by_date[(8, '102:201', date(2020, 6, 19))], 1)
        self.assertEqual(counters.question_count_by_date[(8, '103:203', date(2020, 6, 19))], 1)

        self.assertEqual(counters.group_depth[4], 2)

        self.assertEqual(len(counters.answer_scores_count), 0)
        self.assertEqual(len(counters.answer_scores_count_by_date), 0)
        self.assertEqual(len(counters.question_scores_count), 0)
        self.assertEqual(len(counters.question_scores_count_by_date), 0)
    # }}}

    def test_should_return_correct_counters_info_with_scores(self):  # {{{
        survey = SurveyFactory()
        ProfileSurveyAnswerFactory(  # {{{
            survey=survey,
            date_created=(timezone.now() - timedelta(minutes=5)),
            data={
                'quiz': {
                    'scores': 3.0,
                },
                'data': [{
                    'value': 'demotext',
                    'scores': 3.0,
                    'question': {
                        'id': 1,
                        'answer_type': {
                            'slug': 'answer_short_text',
                        },
                    },
                }],
            },
        )  # }}}
        ProfileSurveyAnswerFactory(  # {{{
            survey=survey,
            date_created=(timezone.now() - timedelta(minutes=4)),
            data={
                'quiz': {
                    'scores': 2.0,
                },
                'data': [{
                    'value': 'fourtytwo',
                    'scores': 2.0,
                    'question': {
                        'id': 9,
                        'answer_type': {
                            'slug': 'answer_short_text',
                        },
                    },
                }],
            },
        )  # }}}
        ProfileSurveyAnswerFactory(  # {{{
            survey=survey,
            date_created=(timezone.now() - timedelta(minutes=3)),
            data={
                'quiz': {
                    'scores': 0.0,
                },
                'data': [{
                    'value': 'twentyone',
                    'scores': 0.0,
                    'question': {
                        'id': 9,
                        'answer_type': {
                            'slug': 'answer_short_text',
                        },
                    },
                }],
            },
        )  # }}}
        ProfileSurveyAnswerFactory(  # {{{
            survey=survey,
            date_created=(timezone.now() - timedelta(minutes=2)),
            data={
                'quiz': {
                    'scores': 3.0,
                },
                'data': [{
                    'value': [
                        {
                            'key': '1011', 'slug': None, 'text': 'mars',
                        },
                        {
                            'key': '1013', 'slug': None, 'text': 'venus',
                        },
                    ],
                    'scores': 3.0,
                    'question': {
                        'id': 10,
                        'answer_type': {
                            'slug': 'answer_choices',
                        },
                        'options': {
                            'data_source': 'survey_question_choice',
                        },
                    },
                }],
            },
        )  # }}}

        started_at = timezone.now()
        last_modified = timezone.now() - timedelta(minutes=4)
        counters = get_counters(started_at, survey.pk, last_modified)

        self.assertEqual(counters.answer_count, 3)

        self.assertEqual(len(counters.answer_count_by_date), 2)
        self.assertEqual(counters.answer_count_by_date[date(2020, 6, 18)], 2)
        self.assertEqual(counters.answer_count_by_date[date(2020, 6, 19)], 1)

        self.assertEqual(len(counters.question_count), 4)
        self.assertEqual(counters.question_count[(9, 'fourtytwo')], 1)
        self.assertEqual(counters.question_count[(9, '')], 1)
        self.assertEqual(counters.question_count[(10, '1011')], 1)
        self.assertEqual(counters.question_count[(10, '1013')], 1)

        self.assertEqual(len(counters.question_count_by_date), 4)
        self.assertEqual(counters.question_count_by_date[(9, 'fourtytwo', date(2020, 6, 18))], 1)
        self.assertEqual(counters.question_count_by_date[(9, '', date(2020, 6, 18))], 1)
        self.assertEqual(counters.question_count_by_date[(10, '1011', date(2020, 6, 19))], 1)
        self.assertEqual(counters.question_count_by_date[(10, '1013', date(2020, 6, 19))], 1)

        self.assertEqual(len(counters.answer_scores_count), 3)
        self.assertEqual(counters.answer_scores_count[Decimal('0.0')], 1)
        self.assertEqual(counters.answer_scores_count[Decimal('2.0')], 1)
        self.assertEqual(counters.answer_scores_count[Decimal('3.0')], 1)

        self.assertEqual(len(counters.answer_scores_count_by_date), 3)
        self.assertEqual(counters.answer_scores_count_by_date[(Decimal('0.0'), date(2020, 6, 18))], 1)
        self.assertEqual(counters.answer_scores_count_by_date[(Decimal('2.0'), date(2020, 6, 18))], 1)
        self.assertEqual(counters.answer_scores_count_by_date[(Decimal('3.0'), date(2020, 6, 19))], 1)

        self.assertEqual(len(counters.question_scores_count), 3)
        self.assertEqual(counters.question_scores_count[(9, Decimal('0.0'))], 1)
        self.assertEqual(counters.question_scores_count[(9, Decimal('2.0'))], 1)
        self.assertEqual(counters.question_scores_count[(10, Decimal('3.0'))], 1)

        self.assertEqual(len(counters.question_scores_count_by_date), 3)
        self.assertEqual(counters.question_scores_count_by_date[(9, Decimal('0.0'), date(2020, 6, 18))], 1)
        self.assertEqual(counters.question_scores_count_by_date[(9, Decimal('2.0'), date(2020, 6, 18))], 1)
        self.assertEqual(counters.question_scores_count_by_date[(10, Decimal('3.0'), date(2020, 6, 19))], 1)
    # }}}
# }}}


class TestCountersWithNullableGroup(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()
        self.group_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_group'),
            param_is_required=False,
        )
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            group=self.group_question,
            param_is_required=False,
        )

    def test_should_correct_recount_survey_1(self):
        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            data={
                'data': [{
                    'question': self.group_question.get_answer_info(),
                    'value': [
                        None,
                        [{
                            'question': self.question.get_answer_info(),
                            'value': '1',
                        }],
                        [{
                            'question': self.question.get_answer_info(),
                            'value': '2',
                        }],
                    ],
                }],
            }
        )
        counters = get_counters(answer.date_updated, self.survey.pk, self.survey.date_created)
        self.assertIsNotNone(counters)
        self.assertEqual(counters.answer_count, 1)
        self.assertEqual(len(counters.question_count), 1)
        key = (self.question.pk, '')
        self.assertEqual(counters.question_count[key], 2)

    def test_should_correct_recount_survey_2(self):
        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            data={
                'data': [{
                    'question': self.group_question.get_answer_info(),
                    'value': [
                        [{
                            'question': self.question.get_answer_info(),
                            'value': '1',
                        }],
                        None,
                        [{
                            'question': self.question.get_answer_info(),
                            'value': '2',
                        }],
                    ],
                }],
            }
        )
        counters = get_counters(answer.date_updated, self.survey.pk, self.survey.date_created)
        self.assertIsNotNone(counters)
        self.assertEqual(counters.answer_count, 1)
        self.assertEqual(len(counters.question_count), 1)
        key = (self.question.pk, '')
        self.assertEqual(counters.question_count[key], 2)

    def test_should_correct_recount_survey_3(self):
        answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            data={
                'data': [{
                    'question': self.group_question.get_answer_info(),
                    'value': [
                        [{
                            'question': self.question.get_answer_info(),
                            'value': '1',
                        }],
                        [{
                            'question': self.question.get_answer_info(),
                            'value': '2',
                        }],
                        None,
                    ],
                }],
            }
        )
        counters = get_counters(answer.date_updated, self.survey.pk, self.survey.date_created)
        self.assertIsNotNone(counters)
        self.assertEqual(counters.answer_count, 1)
        self.assertEqual(len(counters.question_count), 1)
        key = (self.question.pk, '')
        self.assertEqual(counters.question_count[key], 2)
