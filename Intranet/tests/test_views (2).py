from builtins import object

import pytest
from mock import MagicMock, call

from django.contrib.auth import get_user_model

from rest_framework.exceptions import ValidationError
from rest_framework.response import Response

from kelvin.accounts.views import UserViewSet
from kelvin.courses.models import Course, CourseStudent

User = get_user_model()


class TestUserViewSet(object):
    """
    Тесты `UserViewSet`
    """

    @pytest.mark.django_db
    def test_me(self, mocker):
        """
        Проверка получения профиля авторизованного пользователя
        """
        request = MagicMock()
        request.user = User()
        viewset = UserViewSet()
        viewset.request = request
        viewset.format_kwarg = None
        mocked_update_user_courses_task = mocker.patch('kelvin.accounts.views.set_update_user_courses_task', lambda *_, **__: None)
        mocked_get_serializer = mocker.patch.object(viewset, 'get_serializer')
        mocked_get_serializer.return_value.to_representation.return_value = {
            'teacher_profile': None,
        }

        # простой пользователь
        response = viewset.me(request)
        assert isinstance(response, Response), 'Неправильный тип ответа'
        assert mocked_get_serializer.mock_calls == [
            call(), call().to_representation(request.user)], (
            'Должен быть сериализован пользователь из запроса')

        # учитель
        mocked_get_serializer.reset_mock()
        mocked_get_serializer.return_value.to_representation.return_value = {
            'teacher_profile': None,
        }
        request.user = MagicMock(is_teacher=True, is_parent=False)
        response = viewset.me(request)
        assert isinstance(response, Response), 'Неправильный тип ответа'
        assert mocked_get_serializer.mock_calls == [
            call(), call().to_representation(request.user)], (
            'Должен быть сериализован пользователь из запроса')
        assert 'teacher_profile' in response.data

        # мероприятия учителя
        mocked_get_serializer.reset_mock()
        mocked_get_serializer.return_value.to_representation.return_value = {}
        request.user = MagicMock(is_teacher=True, is_parent=False)

        response = viewset.me(request)
        assert isinstance(response, Response), 'Неправильный тип ответа'
        assert mocked_get_serializer.mock_calls == [
            call(), call().to_representation(request.user)], (
            'Должен быть сериализован пользователь из запроса')

        # неподтвержденный учитель
        mocked_get_serializer.reset_mock()
        mocked_get_serializer.return_value.to_representation.return_value = {
            'teacher_profile': None,
        }
        request.user = MagicMock(is_teacher=False, is_parent=False)
        response = viewset.me(request)
        assert isinstance(response, Response), 'Неправильный тип ответа'
        assert mocked_get_serializer.mock_calls == [
            call(), call().to_representation(request.user)], (
            'Должен быть сериализован пользователь из запроса')
        assert 'teacher_profile' not in response.data

        # родитель
        mocked_get_serializer.reset_mock()
        mocked_get_serializer.return_value.to_representation.return_value = {
            'teacher_profile': None,
        }
        request.user = MagicMock(is_parent=True, is_teacher=False)
        (request.user.parent_profile.children.all.return_value
         .only.return_value) = [User(id=1), User(id=2)]
        mocked_course_student = mocker.patch.object(CourseStudent, 'objects')
        (mocked_course_student.filter.return_value.order_by.return_value
         .select_related.return_value) = (
            CourseStudent(course=Course(id=10, name='name'), student_id=1),
            CourseStudent(course=Course(id=20, name='name2'), student_id=2),
        )

        response = viewset.me(request)
        assert isinstance(response, Response), 'Неправильный тип ответа'
        assert mocked_get_serializer.mock_calls == [
            call(), call().to_representation(request.user)], (
            'Должен быть сериализован пользователь из запроса')
        assert response.data['children'] == [
            {
                'id': 1,
                'first_name': None,
                'last_name': None,
                'courses': [{'id': 10, 'name': 'name'}],
                'avatar': None,
                'username': '',
            },
            {
                'id': 2,
                'first_name': None,
                'last_name': None,
                'courses': [{'id': 20, 'name': 'name2'}],
                'avatar': None,
                'username': '',
            },
        ]
        assert mocked_course_student.mock_calls == [
            call.filter(student__in=[User(id=1), User(id=2)]),
            call.filter().order_by('course'),
            call.filter().order_by().select_related('course'),
        ]

    def test_accept_tos(self):
        """
        Проверяем ручку принятия пользовательского соглашения
        """
        request = MagicMock()
        user = MagicMock()

        request.user = user
        viewset = UserViewSet()
        viewset.request = request

        # пользователь с непринятым TOS
        user.is_tos_accepted = False
        response = viewset.accept_tos(request)
        assert isinstance(response, Response)
        assert response.status_code == 200
        assert user.mock_calls == [
            call.save(update_fields=['is_tos_accepted'])
        ]
        assert user.is_tos_accepted is True

        # пользователь с уже принятым TOS
        user.reset_mock()
        user.is_tos_accepted = True
        with pytest.raises(ValidationError) as excinfo:
            viewset.accept_tos(request)
            assert excinfo.value.detail
        assert user.mock_calls == []
        assert user.is_tos_accepted is True

    def test_add_child(self, mocker):
        """
        Тест ручки добавления ребенка
        """
        request = MagicMock()
        user = MagicMock()
        request.user = user
        request.data = {}
        mocked_user_objects = mocker.patch.object(User, 'objects')
        mocked_user_objects.get.return_value = 'user'
        viewset = UserViewSet()
        viewset.request = request
        viewset.format_kwarg = None
        mocked_viewset_me = mocker.patch.object(viewset, 'me')
        mocked_viewset_me.return_value = Response({})

        # неправильные данные для создания ребенка, необходим код
        request.data = {
            'middle_name': 'Отчество',
        }
        with pytest.raises(ValidationError) as excinfo:
            viewset.add_child(request)
        assert excinfo.value.detail == ['code was expected'], (
            'Надо указывать код в данных запроса')
        mocked_user_objects.reset_mock()

        # добавление ребенка по коду
        request.data = {'code': 'underage code'}
        response = viewset.add_child(request)
        assert isinstance(response, Response), 'Неправильный тип ответа'
        assert response.status_code == 200
        assert user.mock_calls == [
            call.parent_profile.children.add('user'),
        ]

        assert mocked_viewset_me.mock_calls == [
            call(request),
        ]

        assert mocked_user_objects.mock_calls == [
            call.get(parent_code='underage code')]

        # случаи исключений при неправильном указании кода
        user.reset_mock()
        mocked_user_objects.reset_mock()
        mocked_user_objects.get.side_effect = User.DoesNotExist
        response = viewset.add_child(request)
        assert isinstance(response, Response), 'Неправильный тип ответа'
        assert response.status_code == 404

        user.reset_mock()
        mocked_user_objects.reset_mock()
        mocked_user_objects.get.side_effect = ValueError
        with pytest.raises(ValidationError) as excinfo:
            viewset.add_child(request)
        assert excinfo.value.detail == ['wrong code value'], (
            'Надо указывать код в данных запроса')

    def test_child(self, mocker):
        """
        Тест ручки получения информации о ребенке
        """
        request = MagicMock()
        request.query_params = {'code': 'QWERTY'}
        mocked_user_objects = mocker.patch.object(User, 'objects')
        mocked_user_objects.get.return_value = User(
            first_name='Петя',
            last_name='Петров',
        )
        viewset = UserViewSet()
        viewset.request = request

        # нормальный запрос
        response = viewset.child(request)
        assert response.status_code == 200
        assert response.data == {
            'first_name': 'Петя',
            'last_name': 'Петров',
            'avatar': None,
        }
        assert mocked_user_objects.mock_calls == [
            call.get(parent_code='QWERTY'),
        ]

        # нет кода в запросе
        mocked_user_objects.reset_mock()
        request.query_params = {}
        with pytest.raises(ValidationError) as excinfo:
            viewset.child(request)
        assert excinfo.value.detail == ['code parameter is required'], (
            u'Надо указывать код в данных запроса')
        assert mocked_user_objects.mock_calls == []

        # не найден ребенок
        request.query_params = {'code': 'QWERTY'}
        mocked_user_objects.reset_mock()
        mocked_user_objects.get.side_effect = User.DoesNotExist
        response = viewset.child(request)
        assert response.status_code == 404
        assert mocked_user_objects.mock_calls == [
            call.get(parent_code='QWERTY'),
        ]
