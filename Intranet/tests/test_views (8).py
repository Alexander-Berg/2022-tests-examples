from builtins import object

import pytest
from mock import MagicMock, call

from django.contrib.auth import get_user_model

from rest_framework.response import Response

from kelvin.lessons.models import Lesson
from kelvin.lessons.serializer_fields import AnswersField
from kelvin.lessons.serializers import LessonSerializer
from kelvin.lessons.views import LessonViewSet
from kelvin.results.models import LessonResult, LessonSummary
from kelvin.results.serializers import LessonResultSerializer

User = get_user_model()


class TestLessonViewSet(object):
    """
    Тесты апи занятий
    """
    def test_answer(self, mocker):
        """
        Проверка ответа на занятие
        """
        request = MagicMock()
        request.data = {}
        request.user = User()
        request.query_params = {}
        viewset = LessonViewSet()
        viewset.request = request
        viewset.format_kwarg = None
        mocked_is_valid = mocker.patch.object(
            LessonResultSerializer, 'is_valid')
        mocked_save = mocker.patch.object(LessonResultSerializer, 'save')
        summary = LessonSummary(lesson=Lesson(id=5), student=User(id=6))
        mocked_save.return_value = LessonResult(
            id=789, summary=summary, answers={'key': 'any'},
        )
        mocked_answers_to_representation = mocker.patch.object(
            AnswersField, 'to_representation')
        mocked_answers_to_representation.return_value = {'some': 'answers'}
        mocked_get_object = mocker.patch.object(viewset, 'get_object')
        lesson = Lesson(id=123)
        mocked_get_object.return_value = lesson
        mocked_get_serializer = mocker.patch.object(viewset, 'get_serializer')
        mocked_get_serializer.return_value.to_representation.return_value = (
            {'serialized': 'data'})

        response = viewset.answer(request, pk=12)

        assert isinstance(response, Response)
        assert response.status_code == 201

        # проверка сериализованного `LessonResult`
        assert 'attempt' in response.data
        assert response.data['attempt'].pop('date_created') is None
        assert response.data['attempt'].pop('date_updated') is None
        expected_attempt = {
            'answers': {'some': 'answers'},
            'id': 789,
            'lesson': 5,
            'points': None,
            'max_points': None,
            'spent_time': None,
            'student': 6,
            'work_out': False,
            'completed': True,
            'viewed': False,
        }
        expected_data = {
            'serialized': 'data',
            'attempt': expected_attempt,
        }
        assert response.data == expected_data
        assert mocked_is_valid.mock_calls == [call(raise_exception=True)]
        assert mocked_save.mock_calls == [call()]
        assert mocked_get_serializer.mock_calls == [
            call(), call().to_representation(lesson)]
        assert mocked_get_object.mock_calls == [call()]
        assert mocked_answers_to_representation.mock_calls == [
            call({'key': 'any'}),
        ]

    get_queryset_data = [
        (
            'GET',
            False,
            [
                call.all(),
                call.all().prefetch_related(
                    'lessonproblemlink_set',
                    'lessonproblemlink_set__problem',
                    'lessonproblemlink_set__problem__subject',
                    'methodology',
                ),
            ],
        ),
        (
            'GET',
            True,
            [
                call.all(),
                call.all().prefetch_related(
                    'lessonproblemlink_set',
                    'lessonproblemlink_set__problem',
                    'lessonproblemlink_set__problem__subject',
                    'methodology',
                ),
                call.all().prefetch_related().prefetch_related(
                    'lessonproblemlink_set__problem__resources',
                    'lessonproblemlink_set__problem__meta')
            ],
        ),
        (
            'POST',
            False,
            [
                call.all(),
            ],
        ),
        (
            'POST',
            True,
            [
                call.all(),
            ],
        ),
    ]

    @pytest.mark.parametrize('method,expand_problems,calls', get_queryset_data)
    def test_get_queryset(self, method, expand_problems, calls, mocker):
        """
        Тестирование метода `get_queryset`
        """
        mocked_objects = mocker.patch.object(Lesson, 'objects')
        request = MagicMock()
        request.method = method
        request.query_params = {}
        if expand_problems:
            request.query_params['expand_problems'] = True
        view = LessonViewSet()
        view.request = request
        view.get_queryset()
        assert mocked_objects.mock_calls == calls

    def test_get_serializer_context(self):
        """
        Проверяем, что в контекст добавляетмя параметр `expand_problems`
        """
        request = MagicMock()
        request.query_params.get.return_value = 1
        request.data = {}
        request.user = User()
        viewset = LessonViewSet()
        viewset.request = request
        viewset.format_kwarg = None
        context = viewset.get_serializer_context()
        assert set(context.keys()) == {'expand_problems', 'view', 'request',
                                       'format', 'hide_answers'}, (
            u'Неправильные ключи в контексте сериализатора')
        assert context['expand_problems'] == 1, u'Неправильный параметр'
        assert request.mock_calls == [
            call.query_params.get('expand_problems'),
            call.query_params.get('hide_answers')], (
            u'Параметр должен браться из запроса')

    def test_perform_create(self, mocker):
        """
        Проверка, что метод `perform_create` вызывается и проставляет
        владельца курса
        """
        serializer_save = mocker.patch.object(LessonSerializer, 'save')
        serializer_save.return_value = True
        serializer = LessonSerializer()

        request = MagicMock()
        user = User(id=1)
        request.user = user

        view = LessonViewSet()
        view.request = request
        view.perform_create(serializer)
        assert serializer_save.mock_calls == [
            call(owner=user)], u'Save was called with wrong parameters'
