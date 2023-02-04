from builtins import str, object
import pytest

from django.core.urlresolvers import reverse
from django.conf import settings

from kelvin.common.utils_for_tests import extract_error_message
from kelvin.courses.models import Course, CourseStudent
from kelvin.subjects.models import Subject, Theme


GET_ME_RESPONSE_DEFAULT_SETTINGS = {
    'use_staff_affiliation': False,
    'language': settings.STAFF_READER_DEFAULT_USER_LANGUAGE,
}


@pytest.mark.django_db
class TestUserViewSet(object):
    """
    Тесты `UserViewSet`
    """
    @pytest.mark.parametrize('version', ['v2'])
    def test_get_users(self, jclient, version, student):
        """
        Тесты на получение списка пользователей
        """
        users_url = reverse('{0}:user-list'.format(version))
        response = jclient.get(users_url)
        assert response.status_code == 401, (
            u'Для получения списка пользователей должна требоваться'
            u'авторизация'
        )

        jclient.login(user=student)
        response = jclient.get(users_url)
        assert response.status_code == 405, (
            u'Нет доступа к списку пользователей')

    # @pytest.mark.xfail
    # TODO Не видел что бы этот тест падал, может быть стоит его разблокировать
    @pytest.mark.parametrize('version', ['v2'])
    def test_add_child(self, jclient, student, student2, parent, version):
        """
        Тест добавления ребенка в профиль родителя по коду
        """
        code = student2.parent_code
        add_child_url = reverse('{0}:user-add-child'.format(version))
        assert parent.parent_profile.children.count() == 0, (
            u'На начало тестов у родителя нет детей')

        # обычный пользователь не имеет доступ к методу
        jclient.login(user=student)
        response = jclient.post(add_child_url, {'code': code})
        assert response.status_code == 403, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert parent.parent_profile.children.count() == 0, (
            u'У родителя нет детей')

        # родитель может добавить ребенка по коду
        jclient.login(user=parent)
        response = jclient.post(add_child_url, {'code': code})
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert parent.parent_profile.children.count() == 1, (
            u'У родителя должен появиться ребенок')
        assert parent.parent_profile.children.all()[0].id == student2.id

        # неправильный код дает 404
        response = jclient.post(add_child_url, {'code': 'ASD'})
        assert response.status_code == 404, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert parent.parent_profile.children.count() == 1, (
            u'У родителя должно быть 2 ребенка')

        # повторное добавление ничего не делает
        response = jclient.post(add_child_url, {'code': code})
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert parent.parent_profile.children.count() == 1, (
            u'У родителя должно быть 2 ребенка')

    @pytest.mark.xfail
    def test_child(self, jclient, student, student2):
        """
        Проверка получения информации о ребенке, которая доступна всем
        авторизованным пользователям
        """
        child_url = reverse('v2:user-child')

        # правильный запрос
        jclient.login(user=student2)
        response = jclient.get(child_url, {'code': student.parent_code})
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.json() == {
            'last_name': student.last_name,
            'first_name': student.first_name,
            'avatar': str(student.avatar) or None,
        }

        # отсутствующий код
        response = jclient.get(child_url, {'code': '1'})
        assert response.status_code == 404, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # нет кода в запросе
        response = jclient.get(child_url, {'some_code': student.parent_code})
        assert response.status_code == 400, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.json() == ['code parameter is required']

    @pytest.mark.django_db
    @pytest.mark.parametrize('version', ['v2'])
    def test_me(self, jclient, student, student2, parent, teacher,
                version):
        """
        Тестирование метода me
        """
        # для родителя
        parent.parent_profile.children.add(student)
        parent.parent_profile.children.add(student2)
        subject = Subject.objects.create()
        theme = Theme.objects.create(name='theme', code='thm',
                                     subject=subject)

        course1 = Course.objects.create(
            name=u'Новый спец курс1',
            subject=subject,
            owner=teacher,
            color='#abcabc',
        )
        course2 = Course.objects.create(
            name=u'Новый спец курс2',
            subject=subject,
            owner=teacher,
            color='#abcabc',
        )
        CourseStudent.objects.create(course=course1, student=student2)
        CourseStudent.objects.create(course=course1, student=student)
        CourseStudent.objects.create(course=course2, student=student)

        jclient.login(user=parent)
        me_url = reverse('{0}:user-me'.format(version))
        response = jclient.get(me_url)
        assert response.status_code == 200, response.content
        content = response.json()
        assert 'children' in content
        assert content['children'] == [
            {
                'id': student.id,
                'first_name': student.first_name,
                'last_name': student.last_name,
                'courses': [
                    {
                        'id': course1.id,
                        'name': u'Новый спец курс1',
                    },
                    {
                        'id': course2.id,
                        'name': u'Новый спец курс2',
                    },
                ],
                'avatar': student.avatar,
                'username': student.username,
            },
            {
                'id': student2.id,
                'first_name': student2.first_name,
                'last_name': student2.last_name,
                'courses': [
                    {
                        'id': course1.id,
                        'name': u'Новый спец курс1',
                    },
                ],
                'avatar': student2.avatar,
                'username': student2.username,
            },
        ]

        # запрос учителя
        jclient.login(user=teacher)
        me_url = reverse('{0}:user-me'.format(version))
        response = jclient.get(me_url)
        assert response.status_code == 200, response.content
        answer = response.json()
        assert answer.pop('id')
        assert answer.pop('date_joined')
        assert answer.pop('roles')
        assert answer == {
            'phone_number': u'',
            'first_name': None,
            'gender': 3,
            'last_name': None,
            'middle_name': None,
            'is_tos_accepted': False,
            'is_parent': False,
            'is_content_manager': False,
            'is_teacher': True,
            'is_superuser': False,
            'email': u'ivan.ivanovich@yandex.ru',
            'username': u'Иван Иваныч',
            'is_staff': False,
            'avatar': '',
            'display_name': '',
            'teacher_profile': {
                'region': u'Москва',
                'school': u'№1',
                'subject': None,
            },
            'yauser': None,
            'settings': GET_ME_RESPONSE_DEFAULT_SETTINGS,
            'experiment_flags': {},
            'projects': [],
        }

        # Запрос ученика. Выставим ему предсказуемый код
        student.parent_code = 'parentcode12345'
        student.save()
        jclient.logout()
        jclient.login(user=student)

        response = jclient.get(me_url)
        assert response.status_code == 200, response.content
        answer = response.json()
        assert answer.pop('id')
        assert answer.pop('date_joined')
        assert answer.pop('roles')
        assert answer == {
            'phone_number': u'',
            'first_name': None,
            'gender': 3,
            'last_name': None,
            'middle_name': None,
            'is_tos_accepted': False,
            'is_parent': False,
            'is_content_manager': False,
            'is_teacher': False,
            'is_superuser': False,
            'email': u'ivanofff@yandex.ru',
            'username': u'Петя Иванов',
            'is_staff': False,
            'avatar': '',
            'display_name': '',
            'parent_code': 'parentcode12345',
            'yauser': None,
            'settings': GET_ME_RESPONSE_DEFAULT_SETTINGS,
            'experiment_flags': {},
            'projects': [],
        }

    def test_accept_tos(self, jclient, student, student2):
        accept_tos_url = reverse('v2:user-accept-tos')

        # неавторизованный юзер
        response = jclient.post(accept_tos_url)
        assert response.status_code == 401

        # обычный юзер с непринятым TOS
        jclient.login(user=student)
        response = jclient.post(accept_tos_url)
        assert response.status_code == 200, response.content

        # выставили именно у этого юзера
        student.refresh_from_db()
        student2.refresh_from_db()
        assert student.is_tos_accepted is True
        assert student2.is_tos_accepted is False

        # повторный запрос о принятии тем же пользователем
        response = jclient.post(accept_tos_url)
        assert response.status_code == 400
        assert (
            extract_error_message(response.json()) ==
            'TOS is already accepted'
        )

        # согласия не изменились
        student.refresh_from_db()
        student2.refresh_from_db()
        assert student.is_tos_accepted is True
        assert student2.is_tos_accepted is False

        # другой юзер всё ещё может проставить согласие
        jclient.login(user=student2)
        response = jclient.post(accept_tos_url)
        assert response.status_code == 200, response.content

        # выставилось ок
        student.refresh_from_db()
        student2.refresh_from_db()
        assert student.is_tos_accepted is True
        assert student2.is_tos_accepted is True

        # второй раз тоже нельзя
        response = jclient.post(accept_tos_url)
        assert response.status_code == 400
        assert (
            extract_error_message(response.json()) ==
            'TOS is already accepted'
        )

    def test_update_teacher_data(self, jclient, teacher, subject_model):
        """
        Проверяет обновление данных учителя
        """
        detail_url = reverse('v2:user-detail', args=(teacher.id,))

        # получаем данные
        jclient.login(user=teacher)
        response = jclient.get(detail_url)
        assert response.status_code == 200, response.content
        answer = response.json()
        assert answer.pop('date_joined')
        assert answer.pop('roles')
        assert answer == {
            'avatar': '',
            'email': u'ivan.ivanovich@yandex.ru',
            'first_name': None,
            'gender': 3,
            'id': teacher.id,
            'is_tos_accepted': False,
            'is_content_manager': False,
            'is_parent': False,
            'is_staff': False,
            'is_teacher': True,
            'is_superuser': False,
            'last_name': None,
            'middle_name': None,
            'phone_number': u'',
            'display_name': '',
            'teacher_profile': {
                'region': u'Москва',
                'school': u'№1',
                'subject': None,
            },
            'username': u'Иван Иваныч',
            'yauser': None,
            'settings': GET_ME_RESPONSE_DEFAULT_SETTINGS,
            'experiment_flags': {},
            'projects': [],
        }

        # изменяем данные PUT-запросом
        # TODO тест обновления аватарки
        new_data = {
            'avatar': 'https://something.jpg',
            'first_name': u'Петр',
            'gender': 1,
            'last_name': u'Петров',
            'middle_name': u'Петрович',
            'phone_number': u'777 77 77',
            'teacher_profile': {
                'region': u'Петербург',
                'school': u'№100',
                'subject': subject_model.id,
            },
        }
        response = jclient.put(detail_url, new_data)
        assert response.status_code == 200, response.content
        answer = response.json()
        assert answer.pop('date_joined')
        assert answer.pop('roles')
        assert answer == {
            'avatar': 'https://something.jpg',
            'email': u'ivan.ivanovich@yandex.ru',
            'first_name': u'Петр',
            'gender': 1,
            'id': teacher.id,
            'is_tos_accepted': False,
            'is_content_manager': False,
            'is_parent': False,
            'is_staff': False,
            'is_teacher': True,
            'is_superuser': False,
            'last_name': u'Петров',
            'middle_name': u'Петрович',
            'phone_number': u'777 77 77',
            'display_name': '',
            'teacher_profile': {
                'region': u'Петербург',
                'school': u'№100',
                'subject': subject_model.id,
            },
            'username': u'Иван Иваныч',
            'yauser': None,
            'settings': GET_ME_RESPONSE_DEFAULT_SETTINGS,
            'experiment_flags': {},
            'projects': [],
        }

        # изменяем данные PATCH-запросом
        new_data = {
            'phone_number': u'77 77',
            'teacher_profile': {
                'school': u'№23',
                'subject': None,
            },
        }
        response = jclient.patch(detail_url, new_data)
        assert response.status_code == 200, response.content
        answer = response.json()
        assert answer.pop('date_joined')
        assert answer.pop('roles')
        assert answer == {
            'avatar': 'https://something.jpg',
            'email': u'ivan.ivanovich@yandex.ru',
            'first_name': u'Петр',
            'gender': 1,
            'id': teacher.id,
            'is_tos_accepted': False,
            'is_content_manager': False,
            'is_parent': False,
            'is_staff': False,
            'is_teacher': True,
            'is_superuser': False,
            'last_name': u'Петров',
            'middle_name': u'Петрович',
            'phone_number': u'77 77',
            'display_name': '',
            'teacher_profile': {
                'region': u'Петербург',
                'school': u'№23',
                'subject': None,
            },
            'username': u'Иван Иваныч',
            'yauser': None,
            'settings': GET_ME_RESPONSE_DEFAULT_SETTINGS,
            'experiment_flags': {},
            'projects': [],
        }

        # пробуем изменить данные, которые только на чтение
        new_data = {
            'is_staff': True,
        }
        response = jclient.patch(detail_url, new_data)
        assert response.status_code == 200, response.content
        assert response.json()['is_staff'] is False

        # изменяем данные не json PATCH-запросом
        new_data = {
            'phone_number': u'77',
            'teacher_profile': {
                'school': '№33',
            }
        }
        response = jclient.patch(detail_url, new_data)
        assert response.status_code == 200, response.content
        answer = response.json()
        assert answer.pop('date_joined')
        assert answer.pop('roles')
        assert answer == {
            'avatar': 'https://something.jpg',
            'email': u'ivan.ivanovich@yandex.ru',
            'first_name': u'Петр',
            'gender': 1,
            'id': teacher.id,
            'is_tos_accepted': False,
            'is_content_manager': False,
            'is_parent': False,
            'is_staff': False,
            'is_teacher': True,
            'is_superuser': False,
            'last_name': u'Петров',
            'middle_name': u'Петрович',
            'phone_number': u'77',
            'display_name': '',
            'teacher_profile': {
                'region': u'Петербург',
                'school': u'№33',
                'subject': None,
            },
            'username': u'Иван Иваныч',
            'yauser': None,
            'settings': GET_ME_RESPONSE_DEFAULT_SETTINGS,
            'experiment_flags': {},
            'projects': [],
        }

    def test_update_student_data(self, jclient, student, student2):
        """
        Проверяет обновление данных ученика
        """
        detail_url = reverse('v2:user-detail', args=(student.id,))

        # получаем данные
        jclient.login(user=student)
        response = jclient.get(detail_url)
        assert response.status_code == 200, response.content
        answer = response.json()
        assert answer.pop('date_joined')
        assert answer.pop('roles')
        assert answer == {
            'avatar': '',
            'email': 'ivanofff@yandex.ru',
            'first_name': None,
            'gender': 3,
            'id': student.id,
            'is_tos_accepted': False,
            'is_content_manager': False,
            'is_parent': False,
            'is_staff': False,
            'is_teacher': False,
            'is_superuser': False,
            'last_name': None,
            'middle_name': None,
            'phone_number': u'',
            'display_name': '',
            'username': u'Петя Иванов',
            'parent_code': student.parent_code,
            'teacher_profile': None,
            'yauser': None,
            'settings': GET_ME_RESPONSE_DEFAULT_SETTINGS,
            'experiment_flags': {},
            'projects': []
        }

        # изменяем данные PATCH-запросом
        new_data = {
            'phone_number': u'77 77',
            'first_name': u'Петров',
        }
        response = jclient.patch(detail_url, new_data)
        assert response.status_code == 200, response.content
        answer = response.json()
        assert answer.pop('date_joined')
        assert answer.pop('roles')
        assert answer == {
            'avatar': '',
            'email': 'ivanofff@yandex.ru',
            'first_name': u'Петров',
            'gender': 3,
            'id': student.id,
            'is_tos_accepted': False,
            'is_content_manager': False,
            'is_parent': False,
            'is_staff': False,
            'is_teacher': False,
            'is_superuser': False,
            'last_name': None,
            'middle_name': None,
            'phone_number': u'77 77',
            'display_name': '',
            'username': u'Петя Иванов',
            'parent_code': student.parent_code,
            'teacher_profile': None,
            'yauser': None,
            'settings': GET_ME_RESPONSE_DEFAULT_SETTINGS,
            'experiment_flags': {},
            'projects': [],
        }

        # изменяем данные не json PATCH-запросом
        new_data = {
            'phone_number': u'77 77',
            'first_name': u'Петров',
        }
        response = jclient.patch(detail_url, new_data)
        assert response.status_code == 200, response.content
        answer = response.json()
        assert answer.pop('date_joined')

        # нельзя менять данные другого пользователя
        new_data = {
            'first_name': 'hacked',
        }
        jclient.login(user=student2)
        response = jclient.patch(detail_url, new_data)
        assert response.status_code == 403

    def test_get_user_detail_permission(self, jclient, student, teacher,
                                        content_manager,
                                        course_with_teacher_and_users):
        """
        Проверяет корректность прав на доступ к detail
        """
        detail_url = reverse('v2:user-detail', args=(student.id,))

        # Студент может видеть сам себя
        jclient.login(user=student)
        response = jclient.get(detail_url)
        assert response.status_code == 200, response.content

        # Произвольный преподаватель не увидит
        jclient.login(user=teacher)
        response = jclient.get(detail_url)
        assert response.status_code == 403, response.content

        # А теперь добавим ученика в курс к этому учителю
        fixture_data = course_with_teacher_and_users
        detail_url = reverse('v2:user-detail',
                             args=(fixture_data['students'][0].id,))
        jclient.login(user=fixture_data['teacher'])
        response = jclient.get(detail_url)
        assert response.status_code == 200, response.content

        # Контент менеджер может видеть всех
        jclient.login(user=content_manager)
        response = jclient.get(detail_url)
        assert response.status_code == 200, response.content
