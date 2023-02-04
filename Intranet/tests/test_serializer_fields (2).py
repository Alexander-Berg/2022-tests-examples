from builtins import object

from mock import MagicMock, call

from kelvin.common.serializer_fields import NestedForeignKeyField
from kelvin.lessons.models import LessonProblemLink
from kelvin.lessons.serializer_fields import AnswersField, ExpandableLessonField
from kelvin.problems.answers import Answer
from kelvin.problems.markers import Marker


class TestExpandableLessonField(object):
    """
    Тесты сериализатора задач в занятии
    """
    def test_to_representation(self, mocker):
        """
        Проверка сериализации
        """
        mocked_to_representation = mocker.patch.object(
            NestedForeignKeyField, 'to_representation')
        mocked_short_serializer = mocker.patch(
            'kelvin.lessons.serializer_fields.LessonShortSerializer'
        )

        # сериализация без разворачивания занятий
        field = ExpandableLessonField()
        field.to_representation(123)
        assert mocked_to_representation.mock_calls == [], (
            u'Метод не должен вызываться')
        assert mocked_short_serializer.mock_calls == [
            call(), call().to_representation(123)], (
            u'Должен быть вызван сериализатор')

        # сериализация с разворачиванием занятий
        mocked_short_serializer.reset_mock()
        mocked_to_representation.reset_mock()
        field = ExpandableLessonField()
        field._context = {'expand_lessons': True}
        field.to_representation(123)

        assert mocked_to_representation.mock_calls == [call(123)], (
            u'Должен быть вызван метод')
        assert mocked_short_serializer.mock_calls == [], (
            u'Сериализатор не должен вызываться')


class TestAnswersField(object):
    """
    Тест поля ответов
    """
    def test_to_representation(self, mocker):
        answers = {
            '10': [
                {
                    'markers': {
                        '1': {
                            'mistakes': 0,
                            'max_mistakes': 1,
                        },
                    },
                    'theory': {},
                    'mistakes': 1,
                    'max_mistakes': 2,
                    'spent_time': 45,
                    'completed': True,
                },
            ],
            '11': {
                'unknown': 'answer format',
            },
        }

        mocked_problems = mocker.patch.object(LessonProblemLink, 'objects')
        mocked_problems.filter.return_value.select_related.return_value = [
            MagicMock(id=10, problem=MagicMock(max_points=10)),
            MagicMock(id=11, problem=MagicMock(max_points=10)),
        ]

        field = AnswersField()
        expected = {
            '10': [
                {
                    'markers': {
                        '1': {
                            'status': Marker.CORRECT,
                            'mistakes': 0,
                            'max_mistakes': 1,
                        },
                    },
                    'theory': {},
                    'custom_answer': None,
                    'status': Answer.INCORRECT,
                    'spent_time': 45,
                    'completed': True,
                    'points': None,  # EDU-274
                    'comment': '',
                    'answered': False,
                },
            ],
        }
        assert field.to_representation(answers) == expected
