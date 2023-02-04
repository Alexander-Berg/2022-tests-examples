from builtins import object

import pytest
from mock import MagicMock

from kelvin.results.permissions import ObjectResultAuthor, ObjectResultForAnonymous


class TestObjectResultAuthor(object):
    """
    Тесты для класса `ObjectResultAuthor`
    """
    has_object_permission_cases = (
        ('GET', 1, 1, True),
        ('GET', 1, 2, False),
        ('PUT', 1, 1, True),
        ('PUT', 1, 2, False),
        ('PATCH', 1, 1, True),
        ('PATCH', 1, 2, False),
    )

    @pytest.mark.parametrize('method,student_id,request_user_id,expected',
                             has_object_permission_cases)
    def test_has_object_permission(self, method, student_id,
                                   request_user_id, expected):
        """
        Тесты получения прав на объект
        """
        request = MagicMock()
        request.user.id = request_user_id
        request.method = method
        result_object = MagicMock()
        result_object.summary.student_id = student_id
        view = MagicMock()

        assert ObjectResultAuthor().has_object_permission(
            request, view, result_object) == expected, (
            u'Получены неправильные права')


class TestObjectResultForAnonymous(object):
    """
    Тесты для класса `ObjectResultForAnonymous`
    """
    has_object_permission_cases = (
        (1, False, False),
        (1, True, False),
        (None, False, False),
        (None, True, True),
    )

    @pytest.mark.parametrize('student_id,allow_anonymous,expected',
                             has_object_permission_cases)
    def test_has_object_permission(self, student_id, allow_anonymous,
                                   expected):
        """
        Тесты получения прав на объект
        """
        request = MagicMock()
        result_object = MagicMock()
        result_object.summary.student_id = student_id
        result_object.summary.clesson.course.allow_anonymous = allow_anonymous
        view = MagicMock()

        assert ObjectResultForAnonymous().has_object_permission(
            request, view, result_object) == expected, (
            u'Получены неправильные права')
