from builtins import object

import pytest
from mock import MagicMock, call

from rest_framework.exceptions import ValidationError
from rest_framework.response import Response

from kelvin.courses.models import Course, CourseLessonLink
from kelvin.lesson_assignments.models import LessonAssignment
from kelvin.lesson_assignments.views import (
    LessonAssignmentViewSet, recalculate_courselessonstat, recalculate_studentcoursestat,
)
from kelvin.lessons.models import Lesson
from kelvin.results.models import CourseLessonResult, CourseLessonSummary


class TestLessonAssignmentViewSet(object):
    """
    Тесты методов работы с назначенисями занятия
    """
    def test_get(self, mocker):
        """
        Тест получения матрицы назначения
        """
        request = MagicMock()
        viewset = LessonAssignmentViewSet()
        mocked_get_queryset = mocker.patch.object(viewset, 'get_queryset')
        mocked_filter_queryset = mocker.patch.object(viewset,
                                                     'filter_queryset')
        assignment1 = LessonAssignment(problems=[1, 2, 3])
        assignment1.student_id = 1
        assignment2 = LessonAssignment(problems=[2, 3, 5])
        assignment2.student_id = 2
        mocked_get_queryset.return_value = 'queryset'
        mocked_filter_queryset.return_value = [assignment1, assignment2]
        mocked_ids = mocker.patch.object(viewset, 'get_problems_and_students')
        mocked_ids.return_value = ([1, 2, 3, 4], [1, 2, 3])
        expected = {
            1: [1, 2, 3],
            2: [2, 3, 5],
            3: [1, 2, 3, 4],
        }

        response = viewset.get(request)
        assert isinstance(response, Response), u'Неправильный класс ответа'
        assert response.status_code == 200, u'Неправильный код ответа'
        assert response.data == expected, u'Неправильный ответ'
        assert mocked_get_queryset.mock_calls == [call()]
        assert mocked_filter_queryset.mock_calls == [call('queryset')], (
            u'Должны отфильтровать данные')

    def test_save(self, mocker):
        """
        Тест сохранения матрицы назначения
        """
        mocked_atomic = mocker.patch(
            'kelvin.lesson_assignments.views.transaction')
        request = MagicMock()
        request.data = {
            '1': [1, 2, 3],
            '2': [2, 3, 10],
            '3': ['1', '2', '3', '4'],
            '4': [1, 2],
        }
        request.query_params.get.return_value = 1
        viewset = LessonAssignmentViewSet()
        viewset.request = request

        mocked_clesson = mocker.patch.object(CourseLessonLink, 'objects')
        mocked_course = mocker.patch.object(Course, 'objects')
        mocked_result = mocker.patch.object(CourseLessonResult, 'objects')
        mocked_result.filter.return_value.select_related.return_value = []
        mocked_now = mocker.patch('django.utils.timezone.now')
        mocked_now.return_value = '<now>'
        mocked_get_queryset = mocker.patch.object(viewset, 'get_queryset')
        mocked_get_queryset.return_value = 'queryset'
        mocked_filter_queryset = mocker.patch.object(viewset,
                                                     'filter_queryset')
        mocked_ids = mocker.patch.object(viewset, 'get_problems_and_students')
        mocked_ids.return_value = ([1, 2, 3, 4], [1, 2, 3])
        mocked_assignment = mocker.patch(
            'kelvin.lesson_assignments.views.LessonAssignment')
        mocked_student_stat = mocker.patch.object(
            recalculate_studentcoursestat, 'delay')
        mocked_clesson_stat = mocker.patch.object(
            recalculate_courselessonstat, 'delay')

        # Случай, когда урок редактируемый
        lesson = Lesson()
        clesson_object = CourseLessonLink(lesson=lesson, lesson_editable=True,
                                          course_id='<course_id>')
        clesson_object.id = 17
        mocked_clesson.select_related.return_value.get.return_value = (
            clesson_object)

        assignment = LessonAssignment()
        mocked_assignment.return_value = assignment

        response = viewset.save(MagicMock())
        assert isinstance(response, Response), u'Неправильный класс ответа'
        assert response.status_code == 200, u'Неправильный код ответа'
        assert response.data == {'reload': False}, (
            u'Для редактируемого урока не нужно уведомление о перезагрузке')
        assert mocked_assignment.mock_calls == [
            call(problems=[1, 2, 3]),
            call(problems=[2, 3]),
            call.objects.bulk_create([assignment, assignment]),
        ], u'Должны создать 2 назначения'
        assert mocked_get_queryset.mock_calls == [call()], (
            u'Должны вызвать метод')
        assert mocked_filter_queryset.mock_calls == [call('queryset'),
                                                     call().delete()], (
            u'Должны удалить все отфильтрованные назначения')
        assert mocked_clesson.mock_calls == [
            call.select_related('lesson'),
            call.select_related().get(id=1),
            call.filter(id=1),
            call.filter().update(date_updated='<now>'),
        ]
        assert mocked_course.mock_calls == [
            call.filter(id='<course_id>'),
            call.filter().update(date_updated='<now>'),
        ]
        assert mocked_result.mock_calls == [
            call.filter(summary__clesson=clesson_object,
                        summary__student_id__in={'1', '3', '2'}),
            call.filter().select_related('summary'),
        ]
        assert mocked_clesson_stat.mock_calls == []
        assert mocked_student_stat.mock_calls == []

        # Случай наличия результатов по занятию
        mocked_assignment.reset_mock()
        mocked_get_queryset.reset_mock()
        mocked_filter_queryset.reset_mock()
        mocked_clesson.reset_mock()
        mocked_course.reset_mock()
        mocked_result.reset_mock()
        result = CourseLessonResult(id=101, max_points=40, answers={})
        result.summary = CourseLessonSummary(student_id=2)
        mocked_get_max_points = mocker.patch.object(lesson, 'get_max_points')
        mocked_get_max_points.return_value = 50
        mocked_result.filter.return_value.select_related.return_value = [
            result]

        response = viewset.save(MagicMock())
        assert isinstance(response, Response), u'Неправильный класс ответа'
        assert response.status_code == 200, u'Неправильный код ответа'
        assert response.data == {'reload': False}, (
            u'Для редактируемого урока не нужно уведомление о перезагрузке')
        assert mocked_assignment.mock_calls == [
            call(problems=[1, 2, 3]),
            call(problems=[2, 3]),
            call.objects.bulk_create([assignment, assignment]),
        ], u'Должны создать 2 назначения'
        assert mocked_result.mock_calls == [
            call.filter(summary__clesson=clesson_object,
                        summary__student_id__in={'1', '3', '2'}),
            call.filter().select_related('summary'),
            call.filter(id=101),
            call.filter().update(max_points=50),
        ], u'Должны обновить результат из-за несовпадения максимума баллов'
        assert mocked_clesson_stat.mock_calls == [call(17)]
        assert mocked_student_stat.mock_calls == [call(2, '<course_id>')]

        # Случай, когда урок нередактируемый
        mocked_assignment.reset_mock()
        mocked_get_queryset.reset_mock()
        mocked_filter_queryset.reset_mock()
        mocked_clesson.reset_mock()
        mocked_course.reset_mock()
        mocked_clesson_stat.reset_mock()
        mocked_student_stat.reset_mock()
        mocked_result.reset_mock()
        mocked_result.filter.return_value.select_related.return_value = []
        clesson_object.lesson_editable = False
        mocked_lesson_copy = mocker.patch(
            'kelvin.lesson_assignments.views.copy_lesson'
        )
        mocked_clesson_save = mocker.patch.object(clesson_object, 'save')
        mocked_lesson_copy.return_value = {
            'new_lesson_id': 2222,
            'problems': {
                1: 5,
                2: 6,
                3: 7,
                4: 8,
            }
        }
        # В запросе также нужен пользователь
        request.user = MagicMock()

        response = viewset.save(MagicMock())
        assert isinstance(response, Response), u'Неправильный класс ответа'
        assert response.status_code == 200, u'Неправильный код ответа'
        assert response.data == {'reload': True}, (
            u'При создании копии урока нужно уведомить о ее перезагрузке')
        assert mocked_assignment.mock_calls == [
            call(problems=[5, 6, 7]),
            call(problems=[6, 7]),
            call.objects.bulk_create([assignment, assignment]),
        ], u'Должны создать 2 назначения'
        assert mocked_get_queryset.mock_calls == [call()], (
            u'Должны вызвать метод')
        assert mocked_filter_queryset.mock_calls == [call('queryset'),
                                                     call().delete()], (
            u'Должны удалить все отфильтрованные назначения')
        assert mocked_lesson_copy.mock_calls == [
            call(lesson, owner=request.user),
        ], u'Должно вызываться копирование занятия'

        assert clesson_object.lesson_editable, (
            u'Занятие должно стать редактируемым')
        assert clesson_object.lesson_id == 2222, (
            u'В clesson должна быть ссылка на копию занятия')
        assert mocked_clesson_save.mock_calls == [
            call()], u'Должно быть вызвано сохранение clesson'
        assert mocked_clesson.mock_calls == [
            call.select_related('lesson'),
            call.select_related().get(id=1),
        ]
        assert mocked_course.mock_calls == [], u'Не должно быть вызовов курса'
        assert mocked_result.mock_calls == [
            call.filter(summary__clesson=clesson_object,
                        summary__student_id__in={'1', '3', '2'}),
            call.filter().select_related('summary'),
        ]
        assert mocked_clesson_stat.mock_calls == []
        assert mocked_student_stat.mock_calls == []

        # Исключение - неправильный формат данных запроса
        request.data = [1, 2, 3]
        with pytest.raises(ValidationError) as excinfo:
            viewset.save(MagicMock())
        assert excinfo.value.detail == [
            'Assignment data should be a dictionary'], (
            u'Неправильное сообщение об ошибке')

        # Исключение - непреобразуемые в int значения в назначениях
        request.data = {
            '1': [1, 'str', 3],
        }
        with pytest.raises(ValidationError) as excinfo:
            viewset.save(MagicMock())
        assert excinfo.value.detail == [
            'Values for problems should be integers'], (
            u'Неправильное сообщение об ошибке')

        # Исключение - несуществующий clesson
        request.data = {
            '1': [1, 2, 3],
        }
        mocked_clesson.select_related.return_value.get.side_effect = (
            CourseLessonLink.DoesNotExist)
        with pytest.raises(ValidationError) as excinfo:
            viewset.save(MagicMock())
        assert excinfo.value.detail == ['Invalid clesson'], (
            u'Неправильное сообщение об ошибке')

    def test_get_problems_and_students(self, mocker):
        """
        Тест получения списка задач и учеников
        """
        mocked_clesson = mocker.patch.object(CourseLessonLink, 'objects')
        (mocked_clesson.filter.return_value.values_list.return_value
         .exclude.return_value.order_by.return_value) = [1, 2]
        (mocked_clesson.filter.return_value.exclude.return_value
         .values_list.return_value) = [3, 4]
        request = MagicMock()
        request.query_params.get.return_value = 1
        viewset = LessonAssignmentViewSet()
        viewset.request = request
        viewset.format_kwarg = None
        problem_ids, student_ids = viewset.get_problems_and_students()
        assert problem_ids == [1, 2], u'Должны вернуться значения из мока'
        assert student_ids == [3, 4], u'Должны вернуться значения из мока'

        # случай исключения
        request.query_params.get.return_value = 'qwerty'
        with pytest.raises(ValidationError) as excinfo:
            viewset.get_problems_and_students()
        assert excinfo.value.detail == [
            'use with integer argument `clesson`'], (
            u'Неправильное сообщение об ошибке')
