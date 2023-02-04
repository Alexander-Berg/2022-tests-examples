import datetime
from builtins import object

from mock import MagicMock, call

from django.contrib.auth import get_user_model
from django.db.utils import IntegrityError

from rest_framework.response import Response

from kelvin.results.models import CourseLessonResult
from kelvin.results.views import CourseLessonResultViewSet

User = get_user_model()


class TestCLessonResultViewSet(object):
    """
    Тесты апи результатов прохождения занятия в курсе
    """

    def test_versions(self, mocker):
        """
        Проверка, что метод `versions` возвращает даты изменения результата
        """
        mocked_objects = mocker.patch.object(CourseLessonResult, 'objects')
        mocked_objects.filter.return_value.\
            order_by.return_value.\
            values_list.return_value = [
                (1, datetime.datetime(2015, 6, 11)),
                (2, datetime.datetime(2015, 6, 12)),
            ]
        expected_result = {
            1: 1433980800,
            2: 1434067200,
        }

        request = MagicMock()
        user = User(id=1)
        request.user = user
        request.query_params.get.return_value = 1

        viewset = CourseLessonResultViewSet()
        viewset.request = request

        response = viewset.versions(request)
        assert isinstance(response, Response), u'Неправильный тип ответа'
        assert mocked_objects.mock_calls == [
            call.filter(summary__clesson__course=1, summary__student=user),
            call.filter().order_by('date_updated'),
            call.filter().order_by().values_list(
                'summary__clesson_id', 'date_updated'),
        ]
        assert response.data == expected_result

    def test_take(self, mocker):
        """
        Проверяет, что `take` проставляет пользователя из запроса в попытку
        """
        result = MagicMock()
        request = MagicMock()
        mocked_get_object = mocker.patch.object(CourseLessonResultViewSet,
                                                'get_object')
        mocked_get_object.return_value = result
        mocked_get_serializer = mocker.patch.object(CourseLessonResultViewSet,
                                                    'get_serializer')
        mocked_get_serializer.return_value.to_representation.return_value = (
            'data')
        viewset = CourseLessonResultViewSet()
        viewset.request = request
        response = viewset.take(None, None)
        assert isinstance(response, Response)
        assert response.data == 'data'
        assert result.summary.student == request.user
        assert result.summary.method_calls == [call.save()]
        assert mocked_get_object.mock_calls == [call(),]
        assert mocked_get_serializer.mock_calls == [
            call(), call().to_representation(result)]

        # у пользователя уже есть попытка
        mocked_get_object.reset_mock()
        mocked_get_serializer.reset_mock()
        result.summary.save.side_effect = IntegrityError
        response = viewset.take(None, None)
        assert isinstance(response, Response)
        assert response.data is None
        assert response.status_code == 400
        assert mocked_get_object.mock_calls == [call()]
        assert mocked_get_serializer.mock_calls == []
