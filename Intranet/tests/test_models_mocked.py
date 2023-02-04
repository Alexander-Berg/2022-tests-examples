import json
from builtins import object, zip

import pytest
from mock import MagicMock, call

from django.utils import timezone

from kelvin.courses.models import CourseLessonLink
from kelvin.lesson_assignments.models import LessonAssignment
from kelvin.lessons.models import LessonProblemLink
from kelvin.problems.answers import Answer
from kelvin.problems.models import Problem
from kelvin.result_stats.models import (
    CourseLessonStat, CourseStudent, ProblemAnswerStat, ProblemStat, StudentCourseStat,
)
from kelvin.results.models import CourseLessonResult, CourseLessonSummary, LessonResult


class TestCourseLessonStat(object):
    """
    Тесты групповой статистики по занятию
    """
    def test_calculate(self, mocker):
        """
        Тест подсчета статистики
        """
        # нет группы
        stat = CourseLessonStat(
            id=1111,
            # FIXME `url='""'`
            clesson=CourseLessonLink(
                id=2222,
                lesson_id=3333,
                url='""',
                course_id=11,
                date_assignment=timezone.now(),
            )
        )
        mocked_course_student = mocker.patch.object(CourseStudent, 'objects')
        mocked_course_student.filter.return_value.values_list.return_value = []

        # нет учеников в курсе
        assert stat.calculate() is None, (
            u'Неправильный процент выполнения')
        assert mocked_course_student.mock_calls == [
            call.filter(course=11),
            call.filter().values_list('student_id', flat=True),
        ]

        # есть пользователи, но нет результатов
        mocked_course_student.reset_mock()
        mocked_course_student.filter.return_value.values_list.return_value = [
            1, 2]
        mocked_result_objects = mocker.patch.object(
            CourseLessonResult, 'objects')
        (mocked_result_objects.filter.return_value.select_related.return_value
         .order_by.return_value.distinct.return_value) = []

        assert stat.calculate() == (0, 0, 0, 0, 0, 0), (
            u'Неправильный процент выполнения')
        assert mocked_course_student.mock_calls == [
            call.filter(course=11),
            call.filter().values_list('student_id', flat=True),
        ]
        assert mocked_result_objects.mock_calls == [
            call.filter(summary__clesson=2222, summary__student_id__in=[1, 2],
                        date_created__gte=stat.clesson.date_assignment,),
            call.filter().select_related('summary'),
            call.filter().select_related().order_by('summary__student_id',
                                                    '-date_updated'),
            call.filter().select_related().order_by().distinct(
                'summary__student_id'),
        ], u'Неправильный запрос результатов учеников'

        # есть группа и есть результаты
        mocked_result_objects.reset_mock()
        mocked_course_student.reset_mock()
        mocked_course_student.filter.return_value.values_list.return_value = [
            1, 2, 3, 4]
        results = [
            CourseLessonResult(summary=CourseLessonSummary(student_id=1),
                               points=1, max_points=2),
            CourseLessonResult(summary=CourseLessonSummary(student_id=2),
                               points=3, max_points=8),
            CourseLessonResult(summary=CourseLessonSummary(student_id=3),
                               points=5, max_points=10),
            CourseLessonResult(summary=CourseLessonSummary(student_id=4),
                               points=7, max_points=8),
        ]
        # Мокаем `get_summary` у результатов
        # Мокаем `get_correct_count` у результатов
        for result, count in zip(results, [0, 2, 0, 3]):
            mocked_count = mocker.patch.object(result, 'get_correct_count')
            mocked_count.return_value = count
        # Мокаем `get_incorrect_count` у результатов
        for result, count in zip(results, [2, 0, 2, 0]):
            mocked_count = mocker.patch.object(result, 'get_incorrect_count')
            mocked_count.return_value = count
            # `get_summary` может возвращать что угодно,
            # т.к. его результат используется в замоканных методах
            mocked_summary = mocker.patch.object(result, 'get_summary')

        (mocked_result_objects.filter.return_value.select_related.return_value
         .order_by.return_value.distinct.return_value) = results
        mocked_get_summarizable_lessonproblemlinks = mocker.patch.object(
            CourseLessonResult, 'get_summarizable_lessonproblemlinks')
        mocked_get_summarizable_lessonproblemlinks.return_value = [
            LessonProblemLink(id=5),
            LessonProblemLink(id=6),
            LessonProblemLink(id=7),
        ]
        mocked_assignment_objects = mocker.patch.object(
            LessonAssignment, 'objects')
        mocked_assignment_objects.filter.return_value = [
            LessonAssignment(student_id=2, problems=[5, 6]),
            LessonAssignment(student_id=3, problems=[]),
        ]

        assert stat.calculate() == (63, 50, 4, 3, 4, 7)
        assert mocked_course_student.mock_calls == [
            call.filter(course=11),
            call.filter().values_list('student_id', flat=True),
        ]
        assert mocked_result_objects.mock_calls == [
            call.filter(
                summary__clesson=2222,
                summary__student_id__in=[1, 2, 3, 4],
                date_created__gte=stat.clesson.date_assignment,
            ),
            call.filter().select_related('summary'),
            call.filter().select_related('summary').order_by(
                'summary__student_id', '-date_updated'),
            call.filter().select_related('summary').order_by().distinct(
                'summary__student_id'),
        ], u'Неправильный запрос результатов учеников'
        assert mocked_get_summarizable_lessonproblemlinks.mock_calls == [
            call(3333),
        ], u'Неправильный запрос ссылок на задачи в занятии'
        assert mocked_assignment_objects.mock_calls == [
            call.filter(clesson=2222, student_id__in=[1, 2, 3, 4]),
        ], u'Неправильный запрос назначений студентов'
        result_calls = [result.get_correct_count.called for result in results]
        assert all(result_calls), (
            u'Нужно получить число корректных ответов у всех результатов')


class TestProblemStat(object):
    """
    Тесты статистики по задаче
    """
    def test_calculate(self, mocker):
        """
        Тест подсчета статистики по задаче
        """
        # Без задачи работать нельзя
        stat = ProblemStat()
        with pytest.raises(AssertionError) as excinfo:
            stat.calculate()

        msg = excinfo.value.args[0] if excinfo.value.args else ""
        assert msg == 'Невозможно посчитать статистику без задачи', \
            u'Неправильное сообщение об ошибке'

        # Задача с двумя обрабатываемыми маркерами и одним необрабатываемым,
        # есть данные по ответам
        problem = Problem(
            id=1,
            markup={
                'answers': {
                    '1': {
                        '1': ['correctinput'],
                        '2': [0, 3],
                    },
                    '2': {
                        '1': [1, 2],
                        '2': [3, 4],
                    },
                },
                'layout': [
                    {
                        'content': {
                            'type': 'inline',
                            'options': {
                                'inputs': {
                                    '1': {
                                        'type': 'field',
                                        'group': 1,
                                    },
                                    '2': {
                                        'type': 'choice',
                                        'group': 2,
                                    },
                                },
                            },
                            'id': 1,
                        },
                        'kind': 'marker',
                    },
                    {
                        'content': {
                            'type': 'matching',
                            'id': 2,
                        },
                        'kind': 'marker',
                    }
                ],
            },
        )
        stat = ProblemStat(problem=problem)

        # Мокаем получение ответов
        mocked_lessonproblemlink = mocker.patch.object(LessonProblemLink,
                                                       'objects')
        mocked_lessonproblemlink.filter.return_value = [
            LessonProblemLink(id=11, lesson_id=111),
            LessonProblemLink(id=22, lesson_id=222),
            LessonProblemLink(id=33, lesson_id=333),
        ]
        mocked_clessonresult = mocker.patch.object(CourseLessonResult,
                                                   'objects')
        mocked_clessonresult.filter.return_value.values_list.return_value = [
            {
                '11': [
                    {
                        'mistakes': 1,
                        'markers': {
                            '1': {
                                'mistakes': 1,
                                'user_answer': {
                                    '1': 'correctinput',
                                    '2': [1],
                                },
                                'answer_status': {
                                    '1': True,
                                    '2': False,
                                },
                            },
                        },
                    },
                    {
                        'mistakes': 0,
                        'markers': {
                            '1': {
                                'mistakes': 0,
                                'user_answer': {
                                    '1': 'correctinput',
                                    '2': [0],
                                },
                                'answer_status': {
                                    '1': True,
                                    '2': True,
                                },
                            },
                            '2': {
                                'mistakes': 0,
                                'user_answer': 'whatever',
                            },
                        },
                    },
                ],
                '12': [
                    {
                        'mistakes': 0,
                        'markers': {
                            '1': {
                                'mistakes': 0,
                                'user_answer': {
                                    '1': 'otherinput',
                                },
                                'answer_status': {
                                    '1': True,
                                },
                            },
                        },
                    },
                ],
                '33': [
                    {
                        'mistakes': 1,
                        'markers': {
                            '1': {
                                'mistakes': 2,
                                'user_answer': {
                                    '1': 'incorrectinput',
                                    '2': [2, 3],
                                },
                                'answer_status': {
                                    '1': False,
                                    '2': False,
                                },
                            },
                        },
                    },
                    {
                        'mistakes': 0,
                        'markers': {
                            '1': {
                                'mistakes': 0,
                                'user_answer': {
                                    '2': [0],
                                },
                                'answer_status': {
                                    '1': True,
                                    '2': True,
                                },
                            },
                            '2': {
                                'mistakes': 0,
                                'user_answer': 'whatever',
                            },
                        },
                    },
                ],
            },
            {
                '22': [
                    {
                        'mistakes': 1,
                        'markers': {
                            '1': {
                                'mistakes': 2,
                                'user_answer': {
                                    '1': 'incorrectinpuu',
                                    '2': [2, 3],
                                },
                                'answer_status': {
                                    '1': False,
                                    '2': False,
                                },
                            },
                        },
                    },
                    {
                        'mistakes': 1,
                        'markers': {
                            '1': {
                                'mistakes': 0,
                                'user_answer': {
                                    '1': 'CorrectInput',
                                    '2': [0],
                                },
                                'answer_status': {
                                    '1': True,
                                    '2': True,
                                },
                            },
                            '2': {
                                'mistakes': 1,
                                'user_answer': 'whatever2',
                            },
                        },
                    },
                    {
                        'mistakes': 0,
                        'markers': {
                            '1': {
                                'mistakes': 0,
                                'user_answer': {
                                    '1': 'correctinput',
                                    '2': {'1': 2, '2': [3, 4, 5]},
                                },
                                'answer_status': {
                                    '1': True,
                                    '2': True,
                                },
                            },
                            '2': {
                                'mistakes': 0,
                                'user_answer': 'whatever',
                            },
                        },
                    },
                ],
            },
        ]
        mocked_lessonresult = mocker.patch.object(LessonResult, 'objects')
        mocked_lessonresult.filter.return_value.values_list.return_value = [
            {
                '33': [
                    {
                        'mistakes': 1,
                        'markers': {
                            '1': {
                                'mistakes': 2,
                                'user_answer': {
                                    '1': 'incorrectinput2',
                                },
                                'answer_status': {
                                    '1': False,
                                    '2': False,
                                },
                            },
                            '2': {
                                'mistakes': 0,
                                'user_answer': 'whatever2',
                            },
                        },
                    },
                    {
                        'mistakes': 1,
                        'markers': {
                            '1': {
                                'mistakes': 1,
                                'user_answer': {},
                                'answer_status': {
                                    '1': False,
                                    '2': True,
                                },
                            },
                            '2': {
                                'mistakes': 0,
                                'user_answer': 'whatever',
                            },
                        },
                    },
                    {
                        'mistakes': 0,
                        'markers': {
                            '1': {
                                'mistakes': 0,
                                'user_answer': {
                                    '1': 'correctinput',
                                    '2': {'2': [3, 4, 5], '1': 2},
                                },
                                'answer_status': {
                                    '1': True,
                                    '2': True,
                                },
                            },
                            '2': {
                                'mistakes': 0,
                                'user_answer': 'whatever',
                            },
                        },
                    },
                ],
            },
        ]

        assert stat.calculate() == (
            4,
            6,
            40,
            [
                ['1-1', {
                    'correct_number': 6,
                    'correct_answers': [
                        ('correctinput', 4),
                        ('CorrectInput', 1),
                    ],
                    'correct_without_answer': 1,
                    'incorrect_number': 4,
                    'incorrect_answers': [
                        ('incorrectinput', 1),
                        ('incorrectinput2', 1),
                        ('incorrectinpuu', 1),
                    ],
                    'incorrect_without_answer': 1,
                    'correct_percent': 60,
                }],
                ['1-2', {
                    'correct_number': 6,
                    'correct_answers': [
                        ([0], 3),
                        ({'1': 2, '2': [3, 4, 5]}, 2),
                    ],
                    'correct_without_answer': 1,
                    'incorrect_number': 4,
                    'incorrect_answers': [
                        ([2, 3], 2),
                        ([1], 1),
                    ],
                    'incorrect_without_answer': 1,
                    'correct_percent': 60,
                }],
            ],
        ), u'Неправильная статистика по задаче'
        assert mocked_lessonproblemlink.mock_calls == [
            call.filter(problem_id=1),
        ], u'Нужно получить список связей занятие-задача этой задачи'
        assert mocked_lessonresult.mock_calls == [
            call.filter(summary__lesson__in=[111, 222, 333]),
            call.filter().values_list('answers', flat=True),
        ], u'Нужно получить ответы "вне курса" на все занятия, где есть задача'
        assert mocked_clessonresult.mock_calls == [
            call.filter(summary__clesson__lesson__in=[111, 222, 333]),
            call.filter().values_list('answers', flat=True),
        ], u'Нужно получить ответы "в курсе" на все занятия, где есть задача'


class TestStudentCourseStat(object):
    """
    Тесты статистики ученика по курсу
    """

    def test_calculate(self, mocker):
        """
        Тест полного пересчета
        """
        mocked_clesson_manager = mocker.patch.object(CourseLessonLink,
                                                     'objects')
        mocked_assignment_manager = mocker.patch.object(LessonAssignment,
                                                        'objects')
        mocked_result_manager = mocker.patch.object(CourseLessonResult,
                                                    'objects')

        stat = StudentCourseStat(student_id=1, course_id=11)

        # Мокаем также вспомогательный метод `_get_lesson_result_data`
        mocked_get_lesson_result_data = mocker.patch.object(
            stat, '_get_lesson_result_data')
        mocked_set_zeroes = mocker.patch.object(stat, '_set_zeroes')

        # Курс без занятий
        (mocked_clesson_manager.filter.return_value.values_list
         .return_value) = []

        stat.calculate()

        assert mocked_clesson_manager.mock_calls == [
            call.filter(course_id=11),
            call.filter().values_list('id', flat=True)
        ], u'Должен быть запрос id занятий курса'
        assert mocked_set_zeroes.mock_calls == [call()], (
            u'Нужно выставить нулевые значения')
        assert mocked_result_manager.mock_calls == [], (
            u'Не должно быть запроса результатов')
        assert mocked_assignment_manager.mock_calls == [], (
            u'Не должно быть запроса назначений')
        assert mocked_get_lesson_result_data.mock_calls == [], (
            u'Не должно быть получения сводки из результата занятия')

        mocked_clesson_manager.reset_mock()
        mocked_set_zeroes.reset_mock()

        # Курс с занятиями, результатов ученика нет
        (mocked_clesson_manager.filter.return_value.values_list
         .return_value) = [111, 222, 333]
        (mocked_result_manager.filter.return_value.select_related
         .return_value) = []

        stat.calculate()

        assert mocked_clesson_manager.mock_calls == [
            call.filter(course_id=11),
            call.filter().values_list('id', flat=True),
        ], u'Должен быть запрос id занятий курса'
        assert mocked_result_manager.mock_calls == [
            call.filter(
                summary__student_id=1,
                summary__clesson__in=[111, 222, 333]
            ),
            call.filter().select_related('summary', 'summary__clesson'),
        ], u'Должен быть запрос результатов ученика по всем занятиям'
        assert mocked_set_zeroes.mock_calls == [call()], (
            u'Нужно выставить нулевые значения')
        assert mocked_assignment_manager.mock_calls == [], (
            u'Не должно быть запроса назначений')
        assert mocked_get_lesson_result_data.mock_calls == [], (
            u'Не должно быть получения сводки из результата занятия')

        mocked_clesson_manager.reset_mock()
        mocked_result_manager.reset_mock()
        mocked_set_zeroes.reset_mock()

        # Есть занятия, есть результаты
        (mocked_clesson_manager.filter.return_value.values_list
         .return_value) = [111, 222, 333, 444]
        (mocked_result_manager.filter.return_value.select_related
         .return_value) = [
            CourseLessonResult(
                id=101,
                summary=CourseLessonSummary(student_id=1, clesson_id=111),
                answers={},
            ),
            CourseLessonResult(
                id=202,
                summary=CourseLessonSummary(student_id=1, clesson_id=222),
                answers={},
            ),
            CourseLessonResult(
                id=404,
                summary=CourseLessonSummary(student_id=1, clesson_id=444),
                answers={},
            ),
        ]
        mocked_assignment_manager.filter.return_value = [
            LessonAssignment(clesson_id=111, problems=[]),
            LessonAssignment(clesson_id=222, problems=[1111, 2222, 3333]),
            LessonAssignment(clesson_id=333, problems=[4444, 5555]),
        ]
        mocked_get_lesson_result_data.return_value = {
            'points': 1,
            'max_points': 2,
            'problems_correct': 3,
            'problems_incorrect': 4,
            'problems_skipped': 5,
            'efficiency': 100,
        }

        stat.calculate()

        assert stat.points == 3, u'Неправильное количество очков'
        assert stat.problems_correct == 9, (
            u'Неправильное количество верно решенных задач')
        assert stat.problems_incorrect == 12, (
            u'Неправильное количество неверно решенных задач')
        assert stat.problems_skipped == 15, (
            u'Неправильное количество пропущенных задач')
        assert stat.total_efficiency == 50, (
            u'Неправильная общая эффективность')
        assert stat.clesson_data == {
            '111': {
                'points': 1,
                'max_points': 2,
                'problems_correct': 3,
                'problems_incorrect': 4,
                'problems_skipped': 5,
                'efficiency': 100,
            },
            '222': {
                'points': 1,
                'max_points': 2,
                'problems_correct': 3,
                'problems_incorrect': 4,
                'problems_skipped': 5,
                'efficiency': 100,
            },
            '444': {
                'points': 1,
                'max_points': 2,
                'problems_correct': 3,
                'problems_incorrect': 4,
                'problems_skipped': 5,
                'efficiency': 100,
            }
        }, u'Неправильные данные по занятиям'

        assert mocked_clesson_manager.mock_calls == [
            call.filter(course_id=11),
            call.filter().values_list('id', flat=True),
        ], u'Должен быть запрос id занятий курса'
        assert mocked_result_manager.mock_calls == [
            call.filter(
                summary__student_id=1,
                summary__clesson__in=[111, 222, 333, 444]
            ),
            call.filter().select_related('summary', 'summary__clesson'),
        ], u'Должен быть запрос результатов ученика по всем занятиям'
        assert mocked_assignment_manager.mock_calls == [
            call.filter(student_id=1, clesson__in=[111, 222, 333, 444])
        ], u'Должен быть запрос назначений по всем занятиям'
        assert mocked_get_lesson_result_data.mock_calls == [
            call(
                CourseLessonResult(id=101, answers={}),
                [],
            ),
            call(
                CourseLessonResult(id=202, answers={}),
                [1111, 2222, 3333],
            ),
            call(
                CourseLessonResult(id=404, answers={}),
                None,
            ),
        ], u'Нужно получить сводку по всем результатам ученика за этот курс'

    def test_recalculate_by_result(self, mocker):
        """
        Тест пересчета статистики по одному результату
        """
        mocked_get_problems = mocker.patch.object(LessonAssignment,
                                                  'get_student_problems')
        stat = StudentCourseStat(
            student_id=1,
            course_id=11,
            clesson_data={
                '111': {
                    'points': 1,
                    'max_points': 2,
                    'problems_correct': 3,
                    'problems_incorrect': 4,
                    'problems_skipped': 5,
                    'efficiency': 100,
                },
                '222': {
                    'points': 1,
                    'max_points': 2,
                    'problems_correct': 3,
                    'problems_incorrect': 4,
                    'problems_skipped': 5,
                    'efficiency': 100,
                },
            }
        )

        # Стандартный случай
        mocked_get_problems.return_value = [1111, 2222, 3333]
        result = CourseLessonResult(
            id=101,
            summary=CourseLessonSummary(student_id=1, clesson_id=222),
            answers={},
        )

        mocked_get_lesson_result_data = mocker.patch.object(
            stat, '_get_lesson_result_data')
        mocked_get_lesson_result_data.return_value = {
            'points': 2,
            'max_points': 3,
            'problems_correct': 4,
            'problems_incorrect': 5,
            'problems_skipped': 6,
            'efficiency': 100,
        }
        expected_clesson_data = {
            '111': {
                'points': 1,
                'max_points': 2,
                'problems_correct': 3,
                'problems_incorrect': 4,
                'problems_skipped': 5,
                'efficiency': 100,
            },
            '222': {
                'points': 2,
                'max_points': 3,
                'problems_correct': 4,
                'problems_incorrect': 5,
                'problems_skipped': 6,
                'efficiency': 100,
            },
        }
        stat.recalculate_by_result(result)

        assert stat.points == 3, u'Неправильное количество очков'
        assert stat.problems_correct == 7, (
            u'Неправильное количество верно решенных задач')
        assert stat.problems_incorrect == 9, (
            u'Неправильное количество неверно решенных задач')
        assert stat.problems_skipped == 11, (
            u'Неправильное количество пропущенных задач')
        assert stat.total_efficiency == 60, (
            u'Неправильная общая эффективность')
        assert stat.clesson_data == expected_clesson_data, (
            u'Неправильные данные по занятиям')

        assert mocked_get_problems.mock_calls == [
            call(1, 222),
        ], u'Нужно получить назначение'
        assert mocked_get_lesson_result_data.mock_calls == [
            call(
                CourseLessonResult(id=101, answers={}),
                [1111, 2222, 3333],
            ),
        ], u'Нужно получить сводку по результату'

    def test_get_lesson_result_data(self, mocker):
        """
        Тест вспомогательного метода получения сводки по результату
        """
        mocked_get_summarizable_lessonproblemlinks = mocker.patch.object(
            CourseLessonResult, 'get_summarizable_lessonproblemlinks')
        mocked_get_all_lessonproblemlinks_count = mocker.patch.object(
            CourseLessonResult, 'get_all_lessonproblemlinks_count')
        mocked_get_summary = mocker.patch.object(CourseLessonResult,
                                                 'get_summary')
        mocked_get_count = mocker.patch.object(CourseLessonResult,
                                               'get_count')
        # FIXME `url='""'`
        clesson = CourseLessonLink(id=222, url='""', lesson_id=19)
        result = CourseLessonResult(
            id=101,
            summary=CourseLessonSummary(
                student_id=1,
                clesson=clesson,
            ),
            answers={},
            points=10,
            max_points=100,
        )
        mocked_get_summary.return_value = {'summary': 'yes'}
        mocked_get_count.return_value = 1
        mocked_get_summarizable_lessonproblemlinks.return_value = 'lessonproblemlinks'
        mocked_get_all_lessonproblemlinks_count.return_value = 10

        # Когда назначения нет (назначены все задачи)
        data = StudentCourseStat._get_lesson_result_data(result, None)
        assert data == {
            'points': 10,
            'max_points': 100,
            'efficiency': 10,
            'problems_correct': 1,
            'problems_incorrect': 1,
            'problems_skipped': 16,
            'progress': 0,
        }, u'Неправильная сводка'
        assert mocked_get_summarizable_lessonproblemlinks.mock_calls == [
            call.filter(19, None),
        ], u'Нужно получить все задачи занятия'
        assert mocked_get_summary.mock_calls == [
            call('lessonproblemlinks', result, lesson_scenario=clesson,
                 force_show_results=True),
        ], u'Нужно получить первичную сводку по результату'
        assert mocked_get_count.mock_calls == [
            call(
                Answer.SUMMARY_CORRECT,
                'lessonproblemlinks',
                {'summary': 'yes'}
            ),
            call(
                Answer.SUMMARY_INCORRECT,
                'lessonproblemlinks',
                {'summary': 'yes'}
            ),
        ], u'Нужно посчитать правильные, неправильные ответы'

        # Назначение есть
        mocked_get_summarizable_lessonproblemlinks.reset_mock()
        mocked_get_summary.reset_mock()
        mocked_get_count.reset_mock()

        data = StudentCourseStat._get_lesson_result_data(result, [111, 222])
        assert data == {
            'points': 10,
            'max_points': 100,
            'efficiency': 10,
            'problems_correct': 1,
            'problems_incorrect': 1,
            'problems_skipped': 16,
            'progress': 0,
        }, u'Неправильная сводка'
        assert mocked_get_summarizable_lessonproblemlinks.mock_calls == [
            call.filter(19, [111, 222]),
        ], u'Нужно получить задачи из назначенного списка'
        assert mocked_get_summary.mock_calls == [
            call('lessonproblemlinks', result, lesson_scenario=clesson,
                 force_show_results=True),
        ], u'Нужно получить первичную сводку по результату'
        assert mocked_get_count.mock_calls == [
            call(
                Answer.SUMMARY_CORRECT,
                'lessonproblemlinks',
                {'summary': 'yes'}
            ),
            call(
                Answer.SUMMARY_INCORRECT,
                'lessonproblemlinks',
                {'summary': 'yes'}
            ),
        ], u'Нужно посчитать правильные, неправильные ответы'

    def test_set_zeroes(self, mocker):
        """
        Тест вспомогательного метода выставления нулевых значений
        """
        stat = StudentCourseStat(
            student_id=1,
            course_id=11,
            points=1,
            problems_correct=2,
            problems_incorrect=3,
            problems_skipped=4,
            total_efficiency=100,
            clesson_data={'some': 'data'},
        )
        stat._set_zeroes()
        assert stat.points == 0
        assert stat.problems_correct == 0
        assert stat.problems_incorrect == 0
        assert stat.problems_skipped == 0
        assert stat.total_efficiency == 0
        assert stat.clesson_data == {}


class TestProblemAnswerStat(object):
    """
    Тесты статистики по ответу на задание
    """
    CORRECT_ANSWER = {
        'markers': {
            '1': {
                'user_answer': [1, 2],
                'mistakes': 0,
                'max_mistakes': 1,
                'answer_status': 1,
            },
        },
        'mistakes': 0,
        'max_mistakes': 1,
        'completed': True,
        'spent_time': None,
        'points': 20,
        'max_points': 20,
    }
    CORRECT_MARKERS_ANSWERS = {
        '1': {
            'user_answer': [1, 2],
        },
    }

    INCORRECT_ANSWER1 = {
        'markers': {
            '1': {
                'user_answer': [3, 4],
                'mistakes': 1,
                'max_mistakes': 1,
                'answer_status': 0,
            },
        },
        'mistakes': 1,
        'max_mistakes': 1,
        'completed': True,
        'spent_time': None,
        'points': 0,
        'max_points': 20,
    }
    INCORRECT_MARKERS_ANSWERS1 = {
        '1': {
            'user_answer': [3, 4],
        },
    }
    INCORRECT_ANSWER2 = {
        'markers': {
            '1': {
                'user_answer': [1, 4],
                'mistakes': 1,
                'max_mistakes': 1,
                'answer_status': 0,
            },
        },
        'mistakes': 1,
        'max_mistakes': 1,
        'completed': True,
        'spent_time': None,
        'points': 0,
        'max_points': 20,
    }
    INCORRECT_MARKERS_ANSWERS2 = {
        '1': {
            'user_answer': [1, 4],
        },
    }

    def test_get_problem_answers(self, mocker):
        """
        Тест получения всех ответов на задачу
        """
        lessonproblemlinks = mocker.patch.object(LessonProblemLink, 'objects')
        lesson_results = mocker.patch.object(LessonResult, 'objects')
        clesson_results = mocker.patch.object(CourseLessonResult, 'objects')

        (lessonproblemlinks
         .filter.return_value
         .values.return_value) = [
            {
                'id': 1,
                'lesson_id': 11,
            },
            {
                'id': 2,
                'lesson_id': 12,
            },
        ]

        lesson_results.filter.return_value.values_list.return_value = [
            {
                '1': [self.CORRECT_ANSWER],
                '3': [{}],
            }
        ]
        (clesson_results
         .filter.return_value
         .values_list.return_value) = [
            {
                '2': [self.INCORRECT_ANSWER1],
            }
        ]

        answers = ProblemAnswerStat.get_problem_answers(42)
        expected_answers = [self.CORRECT_ANSWER, self.INCORRECT_ANSWER1]

        assert answers == expected_answers
        assert lessonproblemlinks.mock_calls == [
            call.filter(problem_id=42),
            call.filter().values('id', 'lesson_id'),
        ]
        assert lesson_results.mock_calls == [
            call.filter(summary__lesson__in=[11, 12]),
            call.filter().values_list('answers', flat=True),
        ]
        assert clesson_results.mock_calls == [
            call.filter(summary__clesson__lesson__in=[11, 12]),
            call.filter().values_list('answers', flat=True),
        ]

    def test_calculate_stats(self, mocker):
        """
        Тест подсчета статистики
        """
        mocker.patch('django.db.transaction.get_connection')
        get_answers = mocker.patch.object(
            ProblemAnswerStat, 'get_problem_answers')
        get_answers.return_value = [
            self.CORRECT_ANSWER,
            self.INCORRECT_ANSWER1,
        ]

        # мокаем уже существующую статистику
        existing_stat1 = MagicMock(
            count=0,
            percent=0,
            markers_answer=self.CORRECT_MARKERS_ANSWERS,
            is_correct=True,
        )
        existing_stat2 = MagicMock(
            count=0,
            percent=0,
            markers_answer=self.INCORRECT_MARKERS_ANSWERS2,
            is_correct=False,
        )
        # дублирует первую статистику
        existing_stat3 = MagicMock(markers_answer=self.CORRECT_MARKERS_ANSWERS)

        # мокаем создание нового объекта
        new_stat = MagicMock(
            count=0,
            percent=0,
            markers_answer=self.INCORRECT_MARKERS_ANSWERS1,
            is_correct=False,
        )
        answer_stat = mocker.patch('kelvin.result_stats.models.ProblemAnswerStat')
        answer_stat.return_value = new_stat

        answer_stat.objects.filter.return_value = [
            existing_stat1,
            existing_stat2,
            existing_stat3,
        ]

        ProblemAnswerStat.calculate_stats(42)

        assert existing_stat1.count == 1
        assert existing_stat1.percent == 50
        assert existing_stat1.markers_answer == self.CORRECT_MARKERS_ANSWERS
        assert existing_stat1.is_correct is True
        assert existing_stat1.mock_calls == [call.save()]

        # статистика для не существующего ответа
        # должна быть удалена
        assert existing_stat2.count == 0
        assert existing_stat2.percent == 0
        assert existing_stat2.markers_answer == self.INCORRECT_MARKERS_ANSWERS2
        assert existing_stat2.is_correct is False
        assert existing_stat2.mock_calls == [call.delete()]

        # дубликат должет быть удален
        assert existing_stat3.mock_calls == [call.delete()]

        assert new_stat.count == 1
        assert new_stat.percent == 50
        assert new_stat.markers_answer == self.INCORRECT_MARKERS_ANSWERS1
        assert new_stat.is_correct is False
        assert new_stat.mock_calls == [call.save()]

        assert answer_stat.mock_calls == [
            call.objects.filter(problem=42),
            call(
                count=0,
                markers_answer={'1': {'user_answer': [3, 4]}},
                problem_id=42,
            ),
        ]
        assert get_answers.mock_calls == [call(42)]

    def test_get_answer_stat(self):
        """
        Тест получения статистики для ответа
        """
        stat = ProblemAnswerStat(markers_answer=self.CORRECT_MARKERS_ANSWERS)
        stats_by_answer = {
            repr(self.CORRECT_MARKERS_ANSWERS): stat
        }

        # если уже есть статистика для ответа, нужно ее вернуть
        result = ProblemAnswerStat.get_answer_stat(
            42, stats_by_answer, self.CORRECT_ANSWER['markers'])

        assert result == stat
        assert stats_by_answer == {
            repr(self.CORRECT_MARKERS_ANSWERS): stat
        }

        # если статистики для ответа нет, нужно создать новую
        result = ProblemAnswerStat.get_answer_stat(
            42, stats_by_answer, self.INCORRECT_ANSWER1['markers'])

        assert result.problem_id == 42
        assert result.markers_answer == self.INCORRECT_MARKERS_ANSWERS1

        assert stats_by_answer[repr(self.CORRECT_MARKERS_ANSWERS)] == stat
        assert stats_by_answer[repr(self.INCORRECT_MARKERS_ANSWERS1)] == result

    def test_clear_answer_fields(self):
        """
        Тест на удаление лишних полей из объекта ответа
        """
        markers_answer = {
            '1': {
                'user_answer': [3, 4],
                'mistakes': 1,
                'max_mistakes': 1,
                'answer_status': 0,
            },
            '2': {
                'user_answer': None,
                'mistakes': 1,
                'max_mistakes': 1,
                'answer_status': 0,
            },
            '3': {
                'mistakes': 1,
                'max_mistakes': 1,
                'answer_status': 0,
            }
        }

        cleared = ProblemAnswerStat.clear_answer_fields(markers_answer)
        expected = {
            '1': {
                'user_answer': [3, 4],
            },
            '2': {
                'user_answer': None,
            },
            '3': {},
        }

        assert cleared == expected
