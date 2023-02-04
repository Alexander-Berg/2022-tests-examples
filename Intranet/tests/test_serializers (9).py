from builtins import object, range, str
from copy import deepcopy
from datetime import datetime, timedelta

import pytest
from mock import MagicMock, call

from django.contrib.auth import get_user_model
from django.utils import timezone

from rest_framework.exceptions import ValidationError
from rest_framework.serializers import ModelSerializer

from kelvin.courses.models import Course, CourseLessonLink
from kelvin.lesson_assignments.models import LessonAssignment
from kelvin.lessons.models import Lesson, LessonProblemLink
from kelvin.lessons.tests.test_serializers import mocked_lesson  # Mock needed!
from kelvin.problems.markers import Marker
from kelvin.problems.models import Problem
from kelvin.results.models import CourseLessonSummary, LessonResult, LessonSummary
from kelvin.results.serializers import (
    CourseLessonResult, CourseLessonResultSerializer, CourseLessonSummaryInResultSerializer, LessonResultSerializer,
    LessonSummaryInResultSerializer,
)

User = get_user_model()


class TestLessonSummaryInResultSerializer(object):
    """
    Тест сериализатора сводки результатов в результатах занятия
    """

    def test_create(self, mocker):
        """
        Тест создания
        """
        mocked_summary = mocker.patch.object(LessonSummary, 'objects')
        mocked_summary.get_or_create.return_value = ('summary', True)
        mocked_summary.create.return_value = 'summary2'
        serializer = LessonSummaryInResultSerializer()

        # создание пользовательской сводки
        assert serializer.create({'lesson': 1, 'student': 2}) == 'summary'
        assert mocked_summary.mock_calls == [
            call.get_or_create(lesson=1, student=2)]

        # создание анонимной сводки результатов
        mocked_summary.reset_mock()
        assert serializer.create({'lesson': 1, 'student': None}) == 'summary2'
        assert mocked_summary.mock_calls == [
            call.create(lesson=1, student=None)]

    def test_update(self):
        """
        Тест обновления
        """
        summary = MagicMock()
        assert (LessonSummaryInResultSerializer().update(summary, 'data')
                is summary)


class TestCourseLessonSummaryInResultSerializer(object):
    """
    Тест сериализатора сводки результатов в результатах занятия в курсе
    """

    def test_create(self, mocker):
        """
        Тест создания
        """
        mocked_summary = mocker.patch.object(CourseLessonSummary, 'objects')
        mocked_summary.get_or_create.return_value = ('summary', True)
        mocked_summary.create.return_value = 'summary2'
        serializer = CourseLessonSummaryInResultSerializer()

        # создание пользовательской сводки
        assert serializer.create({'clesson': 1, 'student': 2}) == 'summary'
        assert mocked_summary.mock_calls == [
            call.get_or_create(clesson=1, student=2)]

        # создание анонимной сводки результатов
        mocked_summary.reset_mock()
        assert serializer.create({'clesson': 1, 'student': None}) == 'summary2'
        assert mocked_summary.mock_calls == [
            call.create(clesson=1, student=None)]

    def test_update(self):
        """
        Тест обновления
        """
        summary = MagicMock()
        assert (CourseLessonSummaryInResultSerializer().update(summary, 'data')
                is summary)


class TestLessonResultSerializer(object):
    """
    Тесты сериализатора попыток прохождения занятия
    """
    validate_data = (
        (
            {
                'completed': True,
            },
            {
                'completed': True,
            },
        ),
        (
            {
                'answers': {
                    '200': {
                        '1': None,
                    },
                    '201': {
                        '1': {'user_answer': None},
                    },
                }
            },
            {
                'answers': {
                    '200': [{
                        'markers': {
                            '1': {
                                'answer_status': Marker.SKIPPED,
                                'mistakes': 1,
                                'max_mistakes': 1,
                            },
                        },
                        'theory': None,
                        'custom_answer': None,
                        'mistakes': 1,
                        'max_mistakes': 1,
                        'spent_time': None,
                        'completed': True,
                        'points': None,  # EDU-274
                        'checked_points': None,
                        'comment': '',
                        'answered': False,
                    }],
                    '201': [{
                        'markers': {
                            '1': {
                                'user_answer': None,
                                'answer_status': Marker.SKIPPED,
                                'mistakes': 2,
                                'max_mistakes': 3,
                            },
                            '2': {
                                'answer_status': Marker.SKIPPED,
                                'mistakes': 1,
                                'max_mistakes': 1,
                            },
                        },
                        'theory': None,
                        'custom_answer': None,
                        'mistakes': 3,
                        'max_mistakes': 4,
                        'spent_time': None,
                        'completed': True,
                        'points': None,  # EDU-274
                        'checked_points': None,
                        'comment': '',
                        'answered': False,
                    }],
                },
            },
        ),
        (
            {
                'answers': {
                    '200': {},
                    '201': {},
                },
            },
            {
                'answers': {
                    '200': [{
                        'markers': {
                            '1': {
                                'answer_status': Marker.SKIPPED,
                                'mistakes': 1,
                                'max_mistakes': 1,
                            },
                        },
                        'theory': None,
                        'custom_answer': None,
                        'mistakes': 1,
                        'max_mistakes': 1,
                        'spent_time': None,
                        'completed': True,
                        'points': None,  # EDU-274
                        'checked_points': None,
                        'comment': '',
                        'answered': False,
                    }],
                    '201': [{
                        'markers': {
                            '1': {
                                'answer_status': Marker.SKIPPED,
                                'mistakes': 2,
                                'max_mistakes': 3,
                            },
                            '2': {
                                'answer_status': Marker.SKIPPED,
                                'mistakes': 1,
                                'max_mistakes': 1,
                            },
                        },
                        'theory': None,
                        'custom_answer': None,
                        'mistakes': 3,
                        'max_mistakes': 4,
                        'spent_time': None,
                        'completed': True,
                        'points': None,  # EDU-274
                        'checked_points': None,
                        'comment': '',
                        'answered': False,
                    }],
                },
            },
        ),
        (
            {
                'answers': {
                    '200': {
                        '1': {'user_answer': {'1': '4'}},
                    },
                    '201': {
                        '1': {'user_answer': [0]},
                    },
                }
            },
            {
                'answers': {
                    '200': [{
                        'markers': {
                            '1': {
                                'user_answer': {'1': '4'},
                                'answer_status': {
                                    '1': True,
                                },
                                'mistakes': 0,
                                'max_mistakes': 1,
                            },
                        },
                        'theory': None,
                        'custom_answer': None,
                        'mistakes': 0,
                        'max_mistakes': 1,
                        'spent_time': None,
                        'completed': True,
                        'points': None,  # EDU-274
                        'checked_points': None,
                        'comment': '',
                        'answered': False,
                    }],
                    '201': [{
                        'markers': {
                            '1': {
                                'user_answer': [0],
                                'answer_status': [Marker.INCORRECT],
                                'mistakes': 3,
                                'max_mistakes': 3,
                            },
                            '2': {
                                'answer_status': Marker.SKIPPED,
                                'mistakes': 1,
                                'max_mistakes': 1,
                            },
                        },
                        'theory': None,
                        'custom_answer': None,
                        'mistakes': 4,
                        'max_mistakes': 4,
                        'spent_time': None,
                        'completed': True,
                        'points': None,  # EDU-274
                        'checked_points': None,
                        'comment': '',
                        'answered': False,
                    }],
                },
            },
        ),
        (
            {
                'answers': {
                    '200': [
                        {
                            '1': {'user_answer': {'1': '2'}}
                        },
                        {
                            '1': {'user_answer': {'1': '4.0'}},
                        },
                    ],
                    '201': [{
                        '1': {'user_answer': [1, 2]},
                        '2': {'user_answer': {'1': 'qwe'}},
                    }],
                    '203': {
                        '1': {'user_answer': 'qwe'},
                    },
                },
            },
            {
                'answers': {
                    '200': [
                        {
                            'markers': {
                                '1': {
                                    'user_answer': {'1': '2'},
                                    'answer_status': {'1': False},
                                    'mistakes': 1,
                                    'max_mistakes': 1,
                                },
                            },
                            'theory': None,
                            'custom_answer': None,
                            'mistakes': 1,
                            'max_mistakes': 1,
                            'spent_time': None,
                            'completed': True,
                            'points': None,  # EDU-274
                            'checked_points': None,
                            'comment': '',
                            'answered': False,
                        },
                        {
                            'markers': {
                                '1': {
                                    'user_answer': {'1': '4.0'},
                                    'answer_status': {'1': True},
                                    'mistakes': 0,
                                    'max_mistakes': 1,
                                },
                            },
                            'theory': None,
                            'custom_answer': None,
                            'mistakes': 0,
                            'max_mistakes': 1,
                            'spent_time': None,
                            'completed': True,
                            'points': None,  # EDU-274
                            'checked_points': None,
                            'comment': '',
                            'answered': False,
                        },
                    ],
                    '201': [{
                        'markers': {
                            '1': {
                                'user_answer': [1, 2],
                                'answer_status': [Marker.CORRECT,
                                                  Marker.CORRECT],
                                'mistakes': 0,
                                'max_mistakes': 3,
                            },
                            '2': {
                                'user_answer': {'1': 'qwe'},
                                'answer_status': {'1': True},
                                'mistakes': 0,
                                'max_mistakes': 1,
                            },
                        },
                        'theory': None,
                        'custom_answer': None,
                        'mistakes': 0,
                        'max_mistakes': 4,
                        'spent_time': None,
                        'completed': True,
                        'points': None,  # EDU-274
                        'checked_points': None,
                        'comment': '',
                        'answered': False,
                    }],
                },
            },
        ),
    )

    @pytest.mark.parametrize('attrs,expected', validate_data)
    def test_validate(self, mocker, attrs, expected, mocked_lesson):
        """
        Тест изменения и проверки ответов пользователя
        """
        # Мокаем проверку существования незавершенного результата на False
        mocked_objects = mocker.patch.object(LessonResult, 'objects')
        mocked_objects.filter.return_value.exists.return_value = False
        mocked_get_lesson = mocker.patch.object(LessonResultSerializer,
                                                '_get_lesson')
        mocked_get_lesson.return_value = mocked_lesson
        student = User(id=1, username='student1')
        attrs['summary'] = {
            'lesson': mocked_lesson,
            'student': student,
        }
        expected['summary'] = {
            'lesson': mocked_lesson,
            'student': student,
        }

        serializer = LessonResultSerializer()
        assert serializer.validate(attrs) == expected, (
            u'Неправильно проверены ответы')
        if attrs.get('completed'):
            assert mocked_objects.mock_calls == [], (
                u'Не должно быть проверки существования незавершенной попытки')
        else:
            assert mocked_objects.mock_calls == [
                call.filter(summary__lesson=attrs['summary']['lesson'],
                            completed=False,
                            summary__student=student),
                call.filter().exists(),
            ], u'Должна быть проверка существования незаверешнной попытки'

    def test_validate_fail(self, mocker, mocked_lesson):
        """
        Тесты ошибок при валидации
        """
        # нельзя изменять законченную попытку
        lesson_result = LessonResult(id=1, completed=True, answers={})
        serializer = LessonResultSerializer(instance=lesson_result)
        with pytest.raises(ValidationError) as excinfo:
            serializer.validate({'answers': 'data'})
        assert excinfo.value.detail == ['can\'t modify completed result'], (
            u'Неправильное сообщение об оибке')

        # если есть незавершенная попытка, она должна быть последней
        lesson_result = MagicMock()
        lesson_result.summary.lesson = mocked_lesson
        lesson_result.completed = False
        serializer = LessonResultSerializer(instance=lesson_result)
        wrong_answers = {
            '201': [
                {
                    'markers': {'user_answer': '1'},
                    'completed': False,
                },
                {
                    'markers': {'user_answer': '11'},
                    'completed': True,
                },
            ],
        }
        with pytest.raises(ValidationError) as excinfo:
            serializer.validate({'answers': wrong_answers})
        assert (excinfo.value.detail ==
                [u'uncompleted attempt can be only last']), (
            u'Неправильное сообщение об ошибке')

        # Нельзя создать незавершенную попытку, если такая уже есть
        mocked_objects = mocker.patch.object(LessonResult, 'objects')
        mocked_objects.filter.return_value.exists.return_value = True
        mocked_get_lesson = mocker.patch.object(LessonResultSerializer,
                                                '_get_lesson')
        mocked_get_lesson.return_value = mocked_lesson
        student = User(id=1, username='student1')
        data = {
            'summary': {
                'student': student,
                'lesson': mocked_lesson,
            },
            'completed': False,
        }
        serializer.instance = None

        with pytest.raises(ValidationError) as excinfo:
            serializer.validate(data)
        assert (excinfo.value.detail ==
                [u'there can be only one incomplete attempt']), (
            u'Неправильное сообщение об ошибке')
        assert mocked_objects.mock_calls == [
            call.filter(summary__lesson=mocked_lesson,
                        completed=False,
                        summary__student=student),
            call.filter().exists(),
        ], u'Должна быть проверка существования незаверешнной попытки'

    def test_validate_answers(self):
        """
        Проверка валидации поля ответов
        """
        problem1 = Problem(id=1, markup={'markup1': 'data'})
        problem2 = Problem(id=2, markup={'markup2': 'data'})
        problem3 = Problem(id=3, markup={'markup3': 'data'})
        problem4 = Problem(id=4, markup={'markup4': 'data'})
        problem5 = Problem(id=5, markup={'markup5': 'data'})
        link1 = LessonProblemLink(id=10, problem=problem1, options={
            'max_attempts': 5, 'show_tips': True})
        link2 = LessonProblemLink(id=11, problem=problem2, options={
            'max_attempts': 5, 'show_tips': True})
        link3 = LessonProblemLink(id=12, problem=problem3, options={
            'max_attempts': 5, 'show_tips': True})
        link4 = LessonProblemLink(id=13, problem=problem4, options={
            'max_attempts': 5, 'show_tips': True})
        link5 = LessonProblemLink(id=14, problem=problem5, options={
            'max_attempts': 5, 'show_tips': True})
        problem_links_dict = {
            str(link.id): link
            for link in [link1, link2, link3, link4, link5]
        }

        # в `validated_data` нет поля ответов
        validated_data = {'some': 'data'}
        serializer = LessonResultSerializer()
        assert (serializer._validate_answers(
            validated_data, problem_links_dict) is validated_data)

        # попытка переответить на вопрос, где уже есть правильный ответ
        validated_data = {
            'answers': {
                '10': [{'mistakes': 1}, {'mistakes': 2}, {'mistakes': 1}],
                '11': [{'mistakes': 1}, {'mistakes': 0}, {'mistakes': 1}],
                '12': [{'mistakes': 2}, {'mistakes': 0}, {'mistakes': 0}],
                '13': [{'mistakes': 2}, {'mistakes': 1}, {'mistakes': 1}],
            }
        }
        with pytest.raises(ValidationError) as excinfo:
            serializer._validate_answers(validated_data, problem_links_dict)
        assert excinfo.value.detail == [
            'should be no answer after correct one in problems '
            '[\'11\', \'12\']'
        ]

        # превышен лимит попыток на вопросы
        validated_data = {
            'answers': {
                '10': [{'mistakes': 1} for _ in range(6)],
                '11': [{'mistakes': 1} for _ in range(5)],
                '12': [{'mistakes': 1} for _ in range(6)],
            }
        }
        with pytest.raises(ValidationError) as excinfo:
            serializer._validate_answers(validated_data, problem_links_dict)
        assert excinfo.value.detail == [
            'attempt limit exceeded in problems [\'10\', \'12\']']

        # полное обновление попыток
        validated_data = {
            'any': 'data',
            'answers': {
                '11': [
                    {
                        'markers': {'user_answer': 'one'},
                        'mistakes': 1,
                    },
                    {
                        'markers': {'user_answer': 'two'},
                        'mistakes': 1,
                    },
                ],
            }
        }
        serializer = LessonResultSerializer()
        assert serializer._validate_answers(
            validated_data, problem_links_dict) == {
            'any': 'data',
            'answers': {
                '11': [
                    {
                        'markers': {'user_answer': 'one'},
                        'mistakes': 1,
                    },
                    {
                        'markers': {'user_answer': 'two'},
                        'mistakes': 1,
                    },
                ],
            }
        }

        # частичное обновление попыток
        lesson_result = MagicMock()
        lesson_result.answers = {
            '10': [
                {
                    'markers': {'user_answer': 'old'},
                    'completed': True,
                    'mistakes': 1,
                },
            ],
            '11': [
                {
                    'markers': {'user_answer': 'old 2'},
                    'completed': True,
                    'mistakes': 1,
                },
                {
                    'markers': {'user_answer': 'old too'},
                    'completed': False,
                    'mistakes': 1,
                },
            ],
            '13': [
                {
                    'markers': {'user_answer': 'also old'},
                    'completed': True,
                    'mistakes': 1,
                },
            ],
        }

        validated_data = {
            'answers': {
                '10': [
                    {
                        'markers': {'user_answer': '1'},
                        'completed': True,
                        'mistakes': 1,
                    },
                ],
                '11': [
                    {
                        'markers': {'user_answer': '22'},
                        'completed': True,
                        'mistakes': 1,
                    },
                ],
                '12': [
                    {
                        'markers': {'user_answer': '3'},
                        'completed': True,
                        'mistakes': 1,
                    },
                    {
                        'markers': {'user_answer': '33'},
                        'completed': True,
                        'mistakes': 1,
                    },
                ],
            },
            'completed': True,
        }
        initial_data = deepcopy(validated_data)
        # на первый и второй вопросы приходят не списки, а словари
        initial_data['answers']['10'] = initial_data['answers']['10'][0]
        initial_data['answers']['11'] = initial_data['answers']['11'][0]

        serializer = LessonResultSerializer(
            instance=lesson_result, partial=True, data=initial_data)

        expected_answers = {
            # должны добавить пришедшую попытку
            '10': [
                {
                    'markers': {'user_answer': 'old'},
                    'completed': True,
                    'mistakes': 1,
                },
                {
                    'markers': {'user_answer': '1'},
                    'completed': True,
                    'mistakes': 1,
                },
            ],
            # должны перезаписать незавершенную попытку
            '11': [
                {
                    'markers': {'user_answer': 'old 2'},
                    'completed': True,
                    'mistakes': 1,
                },
                {
                    'markers': {'user_answer': '22'},
                    'completed': True,
                    'mistakes': 1,
                },
            ],
            # должны дописать все попытки
            '12': [
                {
                    'markers': {'user_answer': '3'},
                    'completed': True,
                    'mistakes': 1,
                },
                {
                    'markers': {'user_answer': '33'},
                    'completed': True,
                    'mistakes': 1,
                },
            ],
            # должны оставить существующую попытку
            '13': [
                {
                    'markers': {'user_answer': 'also old'},
                    'completed': True,
                    'mistakes': 1,
                },
            ],
            # ответа на вопрос `14` не должно быть
        }

        expected_validated_data = {
            'answers': expected_answers,
            'completed': True,
        }
        assert serializer._validate_answers(
            validated_data, problem_links_dict) == expected_validated_data

    def test_create(self, mocker, mocked_lesson):
        """
        Тест создания модели
        """
        mocked_lesson.get_points.return_value = 50
        mocked_lesson.get_max_points.return_value = 101
        mocked_get_lesson = mocker.patch.object(LessonResultSerializer,
                                                '_get_lesson')
        mocked_get_lesson.return_value = mocked_lesson
        mocked_create = mocker.patch.object(
            ModelSerializer, 'create')
        validated_data = {
            'any': 'value',
            'summary': {'lesson': mocked_lesson},
        }
        expected = {
            'summary': {'lesson': mocked_lesson},
            'any': 'value',
            'points': 50,
            'max_points': 101,
        }
        serializer = LessonResultSerializer()
        serializer.create(validated_data)
        assert mocked_create.mock_calls == [call(expected)]

    def test_update(self, mocked_lesson, mocker):
        """
        Тесты обновления модели
        """
        lesson_result = MagicMock()
        lesson_result.summary.lesson = mocked_lesson
        mocked_lesson.get_points.return_value = 50
        mocked_lesson.get_max_points.return_value = 100
        lesson_result.completed = False
        serializer = LessonResultSerializer(instance=lesson_result)
        mocked_update = mocker.patch.object(ModelSerializer, 'update')
        mocked_update.return_value = 'mocked update'

        # не пересчитываем баллы
        validated_data = {}
        assert serializer.update(
            lesson_result, validated_data) == 'mocked update'
        assert lesson_result.mock_calls == []
        assert mocked_update.mock_calls == [call(lesson_result, {})]

        # есть ответы в данных
        mocked_update.reset_mock()
        serializer = LessonResultSerializer(instance=lesson_result)

        validated_data = {'answers': {}}
        assert serializer.update(
            lesson_result, validated_data) == 'mocked update'
        assert lesson_result.mock_calls == [
            call.summary.lesson.get_points({}),
            call.summary.lesson.get_max_points(),
        ]
        assert mocked_update.mock_calls == [
            call(lesson_result, {'max_points': 100, 'points': 50, 'answers': {}})]

        # не перезатираем spent_time
        mocked_update.reset_mock()
        serializer = LessonResultSerializer(instance=lesson_result)
        validated_data = {'answers': {'1': [{'spent_time': None}]}}
        lesson_result.answers = {'1': [{'spent_time': 10}]}
        assert serializer.update(
            lesson_result, validated_data) == 'mocked update'
        assert mocked_update.mock_calls == [
            call(lesson_result, {
                'max_points': 100, 'points': 50, 'answers': {'1': [{'spent_time': 10}]}
            })
        ]

    def test_to_representation(self, mocker):
        """
        Проверяет обработку `hide_answers` в контексте и поля `summary`
        """
        # версия `v2` с `hide_answers`
        data = {
            'id': 1,
            'points': 2,
            'answers': {},
            'summary': {'student': 3, 'lesson': 4},
        }
        mocked_super = mocker.patch.object(ModelSerializer,
                                           'to_representation')
        mocked_super.return_value = data
        expected = {
            'id': 1,
            'student': 3,
            'lesson': 4,
            'answers': {},
        }
        context = {'request': MagicMock(version='v2'), 'hide_answers': '1'}
        serializer = LessonResultSerializer(context=context)
        result = MagicMock()
        assert serializer.to_representation(result) == expected
        assert mocked_super.mock_calls == [call(result)]

        # версия `v2` без `hide_answers`
        mocked_super.reset_mock()
        data = {
            'id': 1,
            'points': 2,
            'answers': {},
            'summary': {'student': 3, 'lesson': 4},
        }
        mocked_super.return_value = data
        mocked_super.return_value = data
        expected = {
            'id': 1,
            'points': 2,
            'student': 3,
            'lesson': 4,
            'answers': {},
        }
        context = {'request': MagicMock(version='v2')}
        serializer = LessonResultSerializer(context=context)
        result = MagicMock()
        assert serializer.to_representation(result) == expected
        assert mocked_super.mock_calls == [call(result)]

    get_lesson_data = (
        (
            {},
            MagicMock(summary=MagicMock(lesson=1)),
            1,
        ),
        (
            {
                'summary': {'lesson': 2},
            },
            MagicMock(summary=MagicMock(lesson=1)),
            2,
        ),
        (
            {
                'summary': LessonSummary(lesson=Lesson(id=3)),
            },
            MagicMock(summary=MagicMock(lesson=1)),
            3,
        ),
    )

    @pytest.mark.parametrize('attrs,instance,expected', get_lesson_data)
    def test_get_lesson(self, attrs, instance, expected, mocker):
        """
        Тест метода получения занятия при валидации
        """
        mocked_lessons = mocker.patch.object(Lesson, 'objects')
        mocked_lessons.get.return_value = 'lesson'
        serializer = LessonResultSerializer(instance=instance)
        if 'summary' not in attrs:
            assert serializer._get_lesson(attrs) == expected
            assert mocked_lessons.mock_calls == []
        elif isinstance(attrs.get('summary'), LessonSummary):
            assert serializer._get_lesson(attrs).id == expected
            assert mocked_lessons.mock_calls == []
        else:
            assert serializer._get_lesson(attrs) == 'lesson'
            assert mocked_lessons.mock_calls == [call.get(id=2)]

    def test_to_internal_value(self, mocker):
        """
        Проверяет, что создаем поле `summary`
        """
        mocked_super = mocker.patch.object(ModelSerializer,
                                           'to_internal_value')
        mocked_super.return_value = 'data'

        # версия `v2`
        context = {'request': MagicMock(version='v2')}
        serializer = LessonResultSerializer(context=context)
        assert serializer.to_internal_value({'lesson': 1}) == 'data'
        assert mocked_super.mock_calls == [call({'summary': {'lesson': 1}})]


class TestCourseLessonResultSerializer(object):
    """
    Тесты сериализатора попыток прохождения занятия в курсе
    """
    get_lesson_data = (
        (
            {},
            MagicMock(summary=MagicMock(clesson=1)),
            1,
        ),
        (
            {
                'summary': {'clesson': 2},
            },
            MagicMock(summary=MagicMock(clesson=1)),
            2,
        ),
        (
            {
                'summary': CourseLessonSummary(
                    clesson=CourseLessonLink(id=3, url='""')),
            },
            MagicMock(summary=MagicMock(clesson=1)),
            3,
        ),
    )

    @pytest.mark.parametrize('attrs,instance,expected', get_lesson_data)
    def test_get_clesson(self, attrs, instance, expected, mocker):
        """
        Тест метода получения курсозанятия при валидации
        """
        mocked_clessons = mocker.patch.object(CourseLessonLink, 'objects')
        mocked_clessons.get.return_value = 'clesson'
        serializer = CourseLessonResultSerializer(instance=instance)
        if 'summary' not in attrs:
            assert serializer._get_clesson(attrs) == expected
            assert mocked_clessons.mock_calls == []
        elif isinstance(attrs.get('summary'), CourseLessonSummary):
            assert serializer._get_clesson(attrs).id == expected
            assert mocked_clessons.mock_calls == []
        else:
            assert serializer._get_clesson(attrs) == 'clesson'
            assert mocked_clessons.mock_calls == [call.get(id=2)]

    def test_get_lesson(self, mocker):
        """
        Тест метода получения занятия
        """
        mocked = mocker.patch.object(CourseLessonResultSerializer,
                                     '_get_clesson')
        mocked.return_value = MagicMock(lesson='lesson')
        serializer = CourseLessonResultSerializer()
        assert serializer._get_lesson('data') == 'lesson'
        assert mocked.mock_calls == [call('data')], (
            u'Должны вернуть занятие курсозанятия')

    def test_get_assigned_problem_ids(self, mocker):
        """
        Тест получения назначенных задач
        """
        mocked_get_student_problems = mocker.patch.object(
            LessonAssignment, 'get_student_problems')
        mocked_get_student_problems.return_value = 'problem ids'
        mocked_get_clesson = mocker.patch.object(
            CourseLessonResultSerializer, '_get_clesson')
        mocked_get_clesson.return_value = 'clesson'
        user = MagicMock()
        user.is_authenticated = True
        serializer = CourseLessonResultSerializer(
            context={'request': MagicMock(user=user)},
        )
        assert serializer._get_assigned_problem_ids('data') == 'problem ids'
        assert mocked_get_student_problems.mock_calls == [
            call(user, 'clesson')]
        assert mocked_get_clesson.mock_calls == [call('data')]

        # повторное обращение не вызывает внешние методы
        mocked_get_clesson.reset_mock()
        mocked_get_student_problems.reset_mock()
        assert serializer._get_assigned_problem_ids('data') == 'problem ids'
        assert mocked_get_student_problems.mock_calls == []
        assert mocked_get_clesson.mock_calls == []

        # неавторизованный пользователь
        user.is_authenticated = False
        serializer = CourseLessonResultSerializer(
            context={'request': MagicMock(user=user)},
        )

        assert serializer._get_assigned_problem_ids('data') is None
        assert mocked_get_student_problems.mock_calls == []
        assert mocked_get_clesson.mock_calls == []

    def test_validate(self, mocker):
        """
        Тест валидации попыток на занятие в курсе
        """
        student = User(id=1, username='user1')
        course = Course(allow_anonymous=False)
        mocked_objects = mocker.patch.object(CourseLessonResult, 'objects')
        mocked_super_validate = mocker.patch.object(LessonResultSerializer,
                                                    'validate')
        mocked_super_validate.return_value = {
            'answers': {
                '1': 'one',
                '2': 'two',
                '3': 'three',
            }
        }
        mocked_get_assigned_problem_ids = mocker.patch.object(
            CourseLessonResultSerializer, '_get_assigned_problem_ids')
        mocked_get_assigned_problem_ids.return_value = [2, 3, 4]

        # неавторизованный пользователь не может создать попытку в обчыном
        # курсе
        request = MagicMock()
        serializer = CourseLessonResultSerializer(
            context={'request': request},
        )
        attrs = {
            'work_out': False,
            'summary': {
                'clesson': CourseLessonLink(course=course),
            },
        }
        with pytest.raises(ValidationError) as excinfo:
            serializer.validate(attrs)
        assert excinfo.value.detail == ['authenticate to create result'], (
            u'Неправильное сообщение об ошибке')

        # Пока проверку существования незавершенной попытки пометим False
        mocked_objects.filter.return_value.exists.return_value = False

        # нельзя изменить `work_out` у внеурочной попытки
        attrs = {
            'work_out': False,
        }
        serializer = CourseLessonResultSerializer(
            context={'request': request},
            instance=MagicMock(work_out=True, summary=MagicMock(
                clesson=CourseLessonLink()
            )),
        )
        with pytest.raises(ValidationError) as excinfo:
            serializer.validate(attrs)
        assert excinfo.value.detail == [
            'can not modify to non-workout attempt'], (
            u'Неправильное сообщение об ошибке')

        # случай, когда не нужна проверка на число попыток при создании
        attrs = {
            'work_out': True,
            'summary': {
                'clesson': CourseLessonLink(
                    max_attempts_in_group=2,
                    date_assignment=timezone.now(),
                ),
                'student': student,
            },
        }
        request = MagicMock()
        serializer = CourseLessonResultSerializer(
            context={'request': request})
        expected = {
            'answers': {
                '2': 'two',
                '3': 'three',
            }
        }
        assert serializer.validate(attrs) == expected
        assert mocked_objects.mock_calls == [
            call.filter(summary__clesson=attrs['summary']['clesson'],
                        completed=False,
                        summary__student=student,
                        work_out=True),
            call.filter().exists(),
        ], (u'Должна быть проверка существования другого '
            u'незавершенного результата')
        assert mocked_super_validate.mock_calls == [call(attrs)], (
            u'Должен быть вызван родительский метод')
        assert mocked_get_assigned_problem_ids.mock_calls == [
            call(expected)], (u'Метод должен быть вызван с тем же объектов, '
                              u'что возвращается')

        # случай, когда не нужна проверка на число попыток при обновлении
        mocked_super_validate.reset_mock()
        mocked_get_assigned_problem_ids.reset_mock()
        mocked_objects.reset_mock()
        attrs = {}
        serializer = CourseLessonResultSerializer(
            context={'request': request},
            instance=MagicMock(summary=MagicMock(clesson=CourseLessonLink())),
        )
        assert serializer.validate(attrs) == expected  # из предыдущего случая
        assert mocked_objects.mock_calls == [], u'Не должно быть запроса'
        assert mocked_super_validate.mock_calls == [call(attrs)], (
            u'Должен быть вызван родительский метод')
        assert mocked_get_assigned_problem_ids.mock_calls == [
            call(expected)], (u'Метод должен быть вызван с тем же объектов, '
                              u'что возвращается')

        # случай, когда проверяем число объектов
        mocked_super_validate.reset_mock()
        mocked_get_assigned_problem_ids.reset_mock()
        mocked_objects.reset_mock()
        mocked_objects.filter.return_value.count.return_value = 1
        attrs = {
            'work_out': False,
            'summary': {
                'clesson': CourseLessonLink(
                    max_attempts_in_group=2,
                    date_completed=None,
                    date_assignment=timezone.now(),
                ),
                'student': student,
            },
        }
        serializer = CourseLessonResultSerializer(context={'request': request})
        assert serializer.validate(attrs) == expected
        assert mocked_objects.mock_calls == [
            call.filter(summary__clesson=attrs['summary']['clesson'],
                        completed=False,
                        summary__student=student,
                        work_out=False),
            call.filter().exists(),
            call.filter(
                summary__clesson=attrs['summary']['clesson'],
                summary__student=student,
                work_out=False
            ),
            call.filter().count(),
        ], (u'Должны быть проверка существования другого незавершенного '
            u'занятия и запрос на количество попыток')
        assert mocked_super_validate.mock_calls == [call(attrs)], (
            u'Должен быть вызван родительский метод')
        assert mocked_get_assigned_problem_ids.mock_calls == [
            call(expected)], (u'Метод должен быть вызван с тем же объектов, '
                              u'что возвращается')

        # случай превышения числа попыток
        mocked_super_validate.reset_mock()
        mocked_get_assigned_problem_ids.reset_mock()
        mocked_objects.reset_mock()
        mocked_objects.filter.return_value.count.return_value = 2
        attrs = {
            'work_out': False,
            'summary': {
                'clesson': CourseLessonLink(max_attempts_in_group=2,
                                            course=course,
                                            date_assignment=timezone.now(),
                                            date_completed=None),
                'student': MagicMock(),
            },
        }
        serializer = CourseLessonResultSerializer(context={'request': request})
        with pytest.raises(ValidationError) as excinfo:
            serializer.validate(attrs)
        assert excinfo.value.detail == ['student reaches attempt limit'], (
            u'Неправильное сообщение об ошибке')
        assert mocked_get_assigned_problem_ids.mock_calls == []

        # случай завершенного занятия
        mocked_super_validate.reset_mock()
        mocked_get_assigned_problem_ids.reset_mock()
        mocked_objects.reset_mock()
        mocked_objects.filter.return_value.count.return_value = 2
        attrs = {
            'work_out': False,
            'summary': {
                'clesson': CourseLessonLink(max_attempts_in_group=2,
                                            course=course,
                                            date_assignment=timezone.now(),
                                            date_completed=timezone.now()),
                'student': MagicMock(),
            },
        }
        serializer = CourseLessonResultSerializer(context={'request': request})
        with pytest.raises(ValidationError) as excinfo:
            serializer.validate(attrs)
        assert excinfo.value.detail == ['clesson is completed'], (
            u'Неправильное сообщение об ошибке')
        assert mocked_get_assigned_problem_ids.mock_calls == []

        # случай существования незавершенного результата
        mocked_super_validate.reset_mock()
        mocked_get_assigned_problem_ids.reset_mock()
        mocked_objects.reset_mock()
        mocked_objects.filter.return_value.exists.return_value = True
        serializer = CourseLessonResultSerializer(
            context={'request': request})
        attrs = {
            'completed': False,
            'work_out': True,
            'summary': {
                'clesson': CourseLessonLink(
                    max_attempts_in_group=2,
                    date_assignment=timezone.now(),
                    date_completed=None,
                ),
                'student': student,
            },
        }
        with pytest.raises(ValidationError) as excinfo:
            serializer.validate(attrs)
        assert excinfo.value.detail == [
            u'there can be only one incomplete attempt'], (
            u'Неправильное сообщение об ошибке')
        assert mocked_objects.mock_calls == [
            call.filter(summary__clesson=attrs['summary']['clesson'],
                        completed=False,
                        summary__student=student,
                        work_out=True),
            call.filter().exists(),
        ], (u'Должна быть проверка существования другого '
            u'незавершенного результата')

        # случай превышения лимита времени в контрольной
        mocked_get_clesson = mocker.patch.object(CourseLessonResultSerializer, '_get_clesson')
        mocked_timezone = mocker.patch('django.utils.timezone')
        clesson = CourseLessonLink(
            finish_date=datetime(year=1970, month=1, day=1, hour=2, minute=50, second=1, tzinfo=timezone.utc),
            mode=CourseLessonLink.CONTROL_WORK_MODE,
            duration=45,
            date_assignment=timezone.now(),
        )
        mocker.patch.object(
            type(clesson), 'can_accept_results',
            new_callable=mocker.PropertyMock, return_value=True
        )
        mocked_get_clesson.return_value = clesson
        mocked_timezone.now.return_value = datetime(year=1970, month=1, day=1, hour=2,
                                           minute=50, second=1, tzinfo=timezone.utc)

        mocked_objects.filter.return_value.exists.return_value = False

        # Случай без инстанса
        serializer = CourseLessonResultSerializer(
            context={'request': request}
        )

        with pytest.raises(ValidationError) as excinfo:
            serializer.validate({
                'summary': {
                    'student': MagicMock(),
                },
            })
        assert excinfo.value.detail == ['time limit exceeded'], (
            u'Неправильное сообщение об ошибке')

        # Случай с инстансом
        mocked_instance = MagicMock(
            # date_created=datetime(year=1970, month=1, day=1, hour=2, minute=10, second=1),
            summary=MagicMock(clesson=CourseLessonLink())
        )
        mocked_instance.quiz_time_limit.return_value=datetime(
            year=1970, month=1, day=1, hour=2, minute=10, second=1, tzinfo=timezone.utc)

        serializer = CourseLessonResultSerializer(
            context={'request': request},
            instance=mocked_instance
        )

        with pytest.raises(ValidationError) as excinfo:
            serializer.validate({})
        assert excinfo.value.detail == ['time limit exceeded'], (
            u'Неправильное сообщение об ошибке')

        # случай превышения лимита времени в диагностике
        mocked_get_clesson.reset_mock()
        mocked_timezone.reset_mock()

        clesson = CourseLessonLink(
            finish_date=datetime(year=1970, month=1, day=1, hour=2, minute=50,
                                 second=1, tzinfo=timezone.utc),
            mode=CourseLessonLink.DIAGNOSTICS_MODE,
            duration=45,
            date_assignment=timezone.now(),
        )
        mocked_get_clesson.return_value = clesson
        serializer = CourseLessonResultSerializer(context={'request': request})

        with pytest.raises(ValidationError) as excinfo:
            serializer.validate({
                'summary': {
                    'student': MagicMock(),
                },
            })
        assert excinfo.value.detail == ['time limit exceeded'], (
            u'Неправильное сообщение об ошибке')

        # Занятие назначено на будущее
        mocked_get_clesson.reset_mock()
        mocked_timezone.reset_mock()

        serializer = CourseLessonResultSerializer(
            context={'request': request},
        )
        clesson = CourseLessonLink(
            date_assignment=timezone.now() + timedelta(days=1),
        )
        user = MagicMock(is_teacher=False, is_content_manager=False)
        request.user.is_teacher = False
        mocked_get_clesson.return_value = clesson

        with pytest.raises(ValidationError) as excinfo:
            serializer.validate({'summary': {'student': user}})
        assert excinfo.value.detail == [
            'can not create result for future clesson'], (
            u'Неправильное сообщение об ошибке')

    @pytest.mark.parametrize(
        'clesson_mode,affected',
        [
            (CourseLessonLink.CONTROL_WORK_MODE, True),
            (CourseLessonLink.DIAGNOSTICS_MODE, True),
            (CourseLessonLink.TRAINING_MODE, False),
            (CourseLessonLink.WEBINAR_MODE, False),
        ]
    )
    def test_validate_not_enough_time(self, clesson_mode, affected, mocker):
        """
        Не принимаем результат, если ученик только начинает
        проходить контрольную или диагностику и до конца осталось меньше
        времени, чем продолжительность контрольной.
        """
        request = MagicMock()

        now = timezone.now()
        # до окончания контрольной 30 минут, а сама контрольная – 45
        finish_date = now - timedelta(minutes=30)

        clesson = CourseLessonLink(
            finish_date=finish_date,
            mode=clesson_mode,
            duration=45,
            date_assignment=now,
        )

        mocker.patch.object(
            CourseLessonResultSerializer, '_get_clesson',
            return_value=clesson,
        )

        serializer = CourseLessonResultSerializer(context={'request': request})
        error_message = CourseLessonResultSerializer.default_error_messages['not enough time']  # noqa
        validation_data = {
            'summary': {
                'student': MagicMock(),
            },
        }

        if affected:
            with pytest.raises(ValidationError) as excinfo:
                serializer.validate(validation_data)

            assert excinfo.value.detail == [error_message], (
                u'Неправильное сообщение об ошибке')
        else:
            try:
                serializer._validate_if_results_accepted(clesson)
            except ValidationError:
                pytest.fail(
                    u'Валидация должна проходить для типа занятия '
                    u'{}'.format(clesson.get_mode_display())
                )

        # на patch запросы валидация не происходит
        serializer = CourseLessonResultSerializer(
            context={'request': request}, partial=True,
        )

        try:
            serializer._validate_if_results_accepted(clesson)
        except ValidationError:
            pytest.fail(u'Валидация должна проходить при '
                        u'обновлении результатов')

    def test_create(self, mocker):
        """
        Тест создания результата
        """
        mocked_super_create = mocker.patch.object(ModelSerializer, 'create')
        mocked_super_create.return_value = 'result'
        mocked_get_lesson = mocker.patch.object(
            CourseLessonResultSerializer, '_get_lesson')
        mocked_get_assigned = mocker.patch.object(
            CourseLessonResultSerializer, '_get_assigned_problem_ids')
        mocked_get_assigned.return_value = 'any ids'
        lesson = MagicMock()
        lesson.get_points.return_value = 1
        lesson.get_max_points.return_value = 2
        mocked_get_lesson.return_value = lesson
        request = MagicMock(user=3)

        serializer = CourseLessonResultSerializer(context={'request': request})
        assert serializer.create({'answers': 'some answers'}) == 'result'
        assert mocked_get_assigned.mock_calls == [call(
            {'max_points': 2, 'points': 1, 'answers': 'some answers'})]
        assert mocked_super_create.mock_calls == [call(
            {'max_points': 2, 'points': 1, 'answers': 'some answers'})]
        assert lesson.mock_calls == [
            call.get_max_points('any ids'),
            call.get_points('some answers'),
        ]
        assert mocked_get_lesson.mock_calls == [
            call({'max_points': 2, 'points': 1,
                  'answers': 'some answers'}),
        ]

    def test_to_representation(self, mocker):
        """
        Проверяет обработку `hide_answers` в контексте и поля `summary`
        """
        # версия `v2` с `hide_answers`
        data = {
            'id': 1,
            'points': 2,
            'answers': {},
            'summary': {'student': 3, 'clesson': 4},
        }
        mocked_super = mocker.patch.object(ModelSerializer,
                                           'to_representation')
        mocked_super.return_value = data
        expected = {
            'id': 1,
            'student': 3,
            'clesson': 4,
            'answers': {},
        }
        context = {'request': MagicMock(version='v2'), 'hide_answers': '1'}
        serializer = CourseLessonResultSerializer(context=context)
        result = MagicMock()
        clesson_results = mocker.patch.object(CourseLessonResult, 'objects')
        clesson_results.get.return_value = MagicMock(
            summary=MagicMock(clesson=MagicMock(mode=0)))
        assert serializer.to_representation(result) == expected
        assert mocked_super.mock_calls == [call(result)]

        # версия `v2` без `hide_answers`
        mocked_super.reset_mock()
        data = {
            'id': 1,
            'points': 2,
            'answers': {},
            'summary': {'student': 3, 'clesson': 4},
        }
        mocked_super.return_value = data
        mocked_super.return_value = data
        expected = {
            'id': 1,
            'points': 2,
            'student': 3,
            'clesson': 4,
            'answers': {},
        }
        context = {'request': MagicMock(version='v2')}
        serializer = CourseLessonResultSerializer(context=context)
        result = MagicMock()
        assert serializer.to_representation(result) == expected
        assert mocked_super.mock_calls == [call(result)]

    def test_to_internal_value(self, mocker):
        """
        Проверяет, что создаем поле `summary`
        """
        mocked_super = mocker.patch.object(ModelSerializer,
                                           'to_internal_value')
        mocked_super.return_value = 'data'

        # версия `v2`
        context = {'request': MagicMock(version='v2')}
        serializer = CourseLessonResultSerializer(context=context)
        assert serializer.to_internal_value({'clesson': 1}) == 'data'
        assert mocked_super.mock_calls == [call({'summary': {'clesson': 1}})]
