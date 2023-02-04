from builtins import object

import pytest
from mock import MagicMock, call

from django.core.exceptions import ValidationError as DjangoValidationError

from rest_framework import serializers

from kelvin.common.serializer_mixins import SerializerManyToManyMixin
from kelvin.lessons.models import Lesson, LessonProblemLink
from kelvin.lessons.serializers import (
    LessonInCLessonSerializer, LessonInCourseSerializer, LessonProblemLinkSerializer, LessonScenarioSerializer,
    LessonSerializer,
)
from kelvin.problems.models import Problem


@pytest.fixture
def mocked_lesson():
    """Имитация урока с задачами"""
    problem1 = Problem(
        id=20,
        markup={
            'layout': [
                {
                    'content': {
                        'text': u'{marker:1}',
                        'options': {
                            'style': 'normal'
                        }
                    },
                    'kind': 'text'
                },
                {
                    'content': {
                        'type': 'inline',
                        'id': 1,
                        'options': {
                            'text': u'Ответ: {input:1}',
                            'inputs': {
                                '1': {
                                    'type': 'field',
                                    'group': 1,
                                    'options': {
                                        'width': 3,
                                        'type_content': 'number',
                                    },
                                },
                            },
                        },
                    },
                    'kind': 'marker'
                }
            ],
            'checks': {
                '1': {
                    '1': {
                        'type': 'EQUAL',
                        'sources': [
                            {
                                'type': 'INPUT',
                                'source': 1,
                            },
                            {
                                'type': 'NUMBER',
                                'source': 4,
                            },
                        ],
                    },
                },
            },
            'answers': {
                '1': {
                    '1': '4',
                }
            }
        },
    )
    problem2 = Problem(
        id=21,
        markup={
            'layout': [
                {
                    'content': {
                        'text': u'{marker:1}',
                        'options': {
                            'style': 'normal'
                        }
                    },
                    'kind': 'text'
                },
                {
                    'content': {
                        'type': 'choice',
                        'id': 1,
                        'options': {
                            'type_content': 'number',
                            'choices': [
                                'Брежнев',
                                'Горбачев',
                                'Ленин'
                            ]
                        }
                    },
                    'kind': 'marker'
                },
                {
                    'content': {
                        'type': 'inline',
                        'id': 2,
                        'options': {
                            'text': u'Ответ: {input:1}',
                            'inputs': {
                                '1': {
                                    'type': 'field',
                                    'group': 1,
                                    'options': {
                                        'type_content': 'text',
                                    },
                                },
                            },
                        },
                    },
                    'kind': 'marker'
                }
            ],
            'checks': {
                '2': {
                    '1': {
                        'type': 'EQUAL',
                        'sources': [
                            {
                                'type': 'INPUT',
                                'source': 1,
                            },
                            {
                                'type': 'STRING',
                                'source': 'qwe',
                            },
                        ],
                    },
                },
            },
            'answers': {
                '1': [
                    1,
                    2
                ],
                '2': {
                    '1': 'qwe',
                },
            }
        },
    )
    lesson = MagicMock()
    lesson.id = 10
    lesson.problems.all.return_value = [problem1, problem2]
    link1 = LessonProblemLink(id=200, problem=problem1)
    link2 = LessonProblemLink(id=201, problem=problem2)
    (lesson.lessonproblemlink_set.all.return_value.select_related
     .return_value) = [link1, link2]
    lesson.lessonproblemlink_set.count.return_value = 2
    lesson.problem_links_dict = {
        '200': LessonProblemLink(problem=problem1),
        '201': LessonProblemLink(problem=problem2),
    }
    return lesson


class TestLeesonInCLessonSerializer(object):
    """
    Тесты сериализатора занятия в курсозанятии
    """
    validate_data = (
        (
            {'any': 'data'},
            {'any': 'data'},
        ),
        (
            {
                'any': 'data',
                'lessonproblemlink_set': [
                    {
                        'one': 'problem',
                    },
                    {
                        'second': 'problem',
                    },
                ],
            },
            {
                'any': 'data',
                'lessonproblemlink_set': [
                    {
                        'one': 'problem',
                        'order': 1,
                    },
                    {
                        'second': 'problem',
                        'order': 2,
                    },
                ],
            },
        ),
    )

    @pytest.mark.parametrize('attrs,expected', validate_data)
    def test_validate(self, attrs, expected):
        """Тест валидации"""
        serializer = LessonInCLessonSerializer()
        assert serializer.validate(attrs) == expected


class TestLessonSerializer(object):
    """
    Тесты сериализатора занятия
    """
    def test_save(self, mocker):
        """Проверка сохранения"""
        mocked_atomic = mocker.patch(
            'kelvin.lessons.serializers.lesson.transaction'
        )
        mocked_save = mocker.patch.object(SerializerManyToManyMixin, 'save')
        mocked_save.return_value = 'lesson'

        # нет поля сценария в валидированных данных
        serializer = LessonSerializer()
        serializer._validated_data = {}
        assert serializer.save() == 'lesson', u'Возвращается урок'
        assert mocked_save.mock_calls == [call()], (
            u'Должно быть вызвано сохранение сериализатора урока')

        # есть поле сценария в даных
        mocked_save.reset_mock()
        mocked_scenario_save = mocker.patch.object(LessonScenarioSerializer,
                                                   'save')
        mocked_scenario_save.return_value = 'scenario'
        lesson = Lesson()
        lesson._primary_scenario = 'old scenario'
        mocked_save.return_value = lesson
        serializer = LessonSerializer()
        serializer._validated_data = {'primary_scenario': '1'}
        assert serializer.save() == lesson
        assert mocked_scenario_save.mock_calls == [call(lesson=lesson,
                                                        primary=True)], (
            u'Должно быть вызвано сохранение сценария')
        assert lesson.primary_scenario == 'scenario', (
            u'Должен быть новый сценарий')


class TestLessonInCourseSerializer(object):
    """
    Тесты сериализатора урока в рамках курса
    """
    def test_problems_count(self, mocked_lesson):
        representation = LessonInCourseSerializer().to_representation(
            mocked_lesson)

        assert representation['problems_count'] == len(
            mocked_lesson.problems.all())


class TestLessonProblemLinkSerializer(object):
    """
    Тесты сериализатора связи занятие-вопрос
    """
    def test_validate(self, mocker):
        """
        Тесты валидации связи
        """
        mocked_validate_json = mocker.patch(
            'kelvin.lessons.validators.LessonProblemLinkValidator.validate_options_json'
        )

        # Успешная валидация
        attrs = {
            'options': 1,
            'type': 2,
        }
        assert LessonProblemLinkSerializer().validate(attrs) == attrs
        assert mocked_validate_json.mock_calls == [call(1, 2)]

        # ошибка при валидации опций
        mocked_validate_json.reset_mock()
        mocked_validate_json.side_effect = DjangoValidationError('message')
        with pytest.raises(serializers.ValidationError) as excinfo:
            LessonProblemLinkSerializer().validate(attrs)
        assert excinfo.value.detail == {'options': ['message']}

        # Ошибка проверки соответствия типа содержимому
        mocked_validate_json.reset_mock()
        mocked_validate_json.side_effect = None
        attrs = {
            'options': 1,
            'type': 1,
            'theory': MagicMock(),
        }
        with pytest.raises(serializers.ValidationError) as excinfo:
            LessonProblemLinkSerializer().validate(attrs)
        assert excinfo.value.detail == [
            u'Link type does not match its content']
        assert mocked_validate_json.mock_calls == [call(1, 1)]

        mocked_validate_json.reset_mock()
        attrs = {
            'options': 1,
            'type': 3,
            'problem': MagicMock(),
        }
        with pytest.raises(serializers.ValidationError) as excinfo:
            LessonProblemLinkSerializer().validate(attrs)
        assert excinfo.value.detail == [
            u'Link type does not match its content']
        assert mocked_validate_json.mock_calls == [call(1, 3)]
