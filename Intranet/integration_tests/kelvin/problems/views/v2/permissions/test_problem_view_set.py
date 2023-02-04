from builtins import object
import pytest
from copy import deepcopy

from django.core.urlresolvers import reverse

from kelvin.problems.models import Problem


@pytest.mark.django_db
class TestProblemViewSet(object):
    """Тесты доступа к задачам"""

    def test_list(self, jclient, student, teacher, content_manager):
        """Тест доступа к списку задач"""
        list_url = reverse('v2:problem-list')

        # неавторизованный пользователь может смотреть список
        response = jclient.get(list_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # все роли могут смотреть список
        for user in (student, teacher, content_manager):
            jclient.login(user=user)
            response = jclient.get(list_url)
            assert response.status_code == 200, (
                u'Неправильный статус ответа, ответ: {0}'.format(response.content))

    detail_data = (
        # просмотр чужих задач
        (
            'some_owner',
            Problem.VISIBILITY_PRIVATE,
            'unauthorized',
            404,
        ),
        (
            'some_owner',
            Problem.VISIBILITY_PRIVATE,
            'student',
            404,
        ),
        (
            'some_owner',
            Problem.VISIBILITY_PRIVATE,
            'teacher',
            404,
        ),
        (
            'some_owner',
            Problem.VISIBILITY_PRIVATE,
            'content_manager',
            200,
        ),
        (
            'some_owner',
            Problem.VISIBILITY_PUBLIC,
            'unauthorized',
            200,
        ),
        (
            'some_owner',
            Problem.VISIBILITY_PUBLIC,
            'student',
            200,
        ),
        (
            'some_owner',
            Problem.VISIBILITY_PUBLIC,
            'teacher',
            200,
        ),
        (
            'some_owner',
            Problem.VISIBILITY_PUBLIC,
            'content_manager',
            200,
        ),
        # просмотр своих задач
        (
            'teacher',
            Problem.VISIBILITY_PRIVATE,
            'teacher',
            200,
        ),
        (
            'content_manager',
            Problem.VISIBILITY_PRIVATE,
            'content_manager',
            200,
        ),
        (
            'teacher',
            Problem.VISIBILITY_PUBLIC,
            'teacher',
            200,
        ),
        (
            'content_manager',
            Problem.VISIBILITY_PUBLIC,
            'content_manager',
            200,
        ),
    )

    @pytest.mark.parametrize('owner_name,visibility,role_name,expected_code',
                             detail_data)
    def test_detail(self, jclient, owner_name, visibility, role_name,
                    expected_code, subject_model, student, teacher,
                    content_manager, some_owner):
        """Тест прав получения одной задачи"""
        users = {
            'some_owner': some_owner,
            'student': student,
            'teacher': teacher,
            'content_manager': content_manager,
        }
        user = users.get(role_name)
        problem = Problem.objects.create(
            markup={},
            subject=subject_model,
            owner=users[owner_name],
            visibility=visibility,
        )
        if user:
            jclient.login(user=user)
        detail_url = reverse('v2:problem-detail', args=(problem.id,))

        response = jclient.get(detail_url)
        assert response.status_code == expected_code, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

    def test_detail_include_comment(self, jclient, student, teacher,
                                    content_manager, problem1):
        """
        Проверяем, поле `cm_comment` из разметки задачи доступно только
        контент-менеджерам
        """
        detail_url = reverse('v2:problem-detail', args=(problem1.id,))
        markup = problem1.markup

        jclient.login(user=content_manager)
        response = jclient.get(detail_url)

        assert response.json()['markup'] == markup

        # ученик и учитель не должны видеть комментарий
        markup.pop('cm_comment')
        for user in [student, teacher]:
            jclient.login(user=user)
            response = jclient.get(detail_url)

            assert response.json()['markup'] == markup

    def test_create(self, jclient, student, teacher, content_manager):
        """Тест доступа к созданию задачи"""
        problem_data = {
            'markup': {
                'layout': [{
                    'kind': 'text',
                    'content': {
                        'text': u'Текст задачи',
                        'options': {},
                    },
                }],
                'answers': {},
                'checks': {},
            },
        }
        create_url = reverse('v2:problem-list')

        # неавторизованный пользователь не может создавать задачу
        response = jclient.post(create_url, problem_data)
        assert response.status_code == 401, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # ученик не может создать задачу
        jclient.login(user=student)
        response = jclient.post(create_url, problem_data)
        assert response.status_code == 403, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # учитель может создать задачу
        jclient.login(user=teacher)
        response = jclient.post(create_url, problem_data)
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # контент-менеджер может создавать задачу
        jclient.login(user=content_manager)
        response = jclient.post(create_url, problem_data)
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

    update_data = (
        # обновление чужих задач
        (
            'some_owner',
            Problem.VISIBILITY_PRIVATE,
            'unauthorized',
            401,
        ),
        (
            'some_owner',
            Problem.VISIBILITY_PRIVATE,
            'student',
            403,
        ),
        (
            'some_owner',
            Problem.VISIBILITY_PRIVATE,
            'teacher',
            404,  # не проверяет права на объект, т.к. учитель не видит задачу
        ),
        (
            'some_owner',
            Problem.VISIBILITY_PRIVATE,
            'content_manager',
            200,
        ),
        (
            'some_owner',
            Problem.VISIBILITY_PUBLIC,
            'unauthorized',
            401,
        ),
        (
            'some_owner',
            Problem.VISIBILITY_PUBLIC,
            'student',
            403,
        ),
        (
            'some_owner',
            Problem.VISIBILITY_PUBLIC,
            'teacher',
            403,
        ),
        (
            'some_owner',
            Problem.VISIBILITY_PUBLIC,
            'content_manager',
            200,
        ),
        # обновление своих задач
        (
            'teacher',
            Problem.VISIBILITY_PRIVATE,
            'teacher',
            200,
        ),
        (
            'content_manager',
            Problem.VISIBILITY_PRIVATE,
            'content_manager',
            200,
        ),
        (
            'teacher',
            Problem.VISIBILITY_PUBLIC,
            'teacher',
            200,
        ),
        (
            'content_manager',
            Problem.VISIBILITY_PUBLIC,
            'content_manager',
            200,
        ),
    )

    @pytest.mark.parametrize('owner_name,visibility,role_name,expected_code',
                             update_data)
    def test_update(self, jclient, owner_name, visibility, role_name,
                    expected_code, subject_model, student, teacher,
                    content_manager, some_owner):
        """Тест прав обновления задачи"""
        users = {
            'some_owner': some_owner,
            'student': student,
            'teacher': teacher,
            'content_manager': content_manager,
        }
        user = users.get(role_name)
        problem = Problem.objects.create(
            markup={},
            subject=subject_model,
            owner=users[owner_name],
            visibility=visibility,
        )
        problem_data = {
            'markup': {
                'layout': [{
                    'kind': 'text',
                    'content': {
                        'text': u'Текст задачи',
                        'options': {},
                    },
                }],
                'answers': {},
                'checks': {},
            },
        }
        if user:
            jclient.login(user=user)
        update_url = reverse('v2:problem-detail', args=(problem.id,))

        response = jclient.put(update_url, problem_data)
        assert response.status_code == expected_code, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        response = jclient.patch(update_url, problem_data)
        assert response.status_code == expected_code, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

    delete_data = (
        # удаление чужих задач
        (
            'some_owner',
            Problem.VISIBILITY_PRIVATE,
            'unauthorized',
            401,
        ),
        (
            'some_owner',
            Problem.VISIBILITY_PRIVATE,
            'student',
            403,
        ),
        (
            'some_owner',
            Problem.VISIBILITY_PRIVATE,
            'teacher',
            404,  # не проверяет права на объект, т.к. учитель не видит задачу
        ),
        (
            'some_owner',
            Problem.VISIBILITY_PRIVATE,
            'content_manager',
            204,
        ),
        (
            'some_owner',
            Problem.VISIBILITY_PUBLIC,
            'unauthorized',
            401,
        ),
        (
            'some_owner',
            Problem.VISIBILITY_PUBLIC,
            'student',
            403,
        ),
        (
            'some_owner',
            Problem.VISIBILITY_PUBLIC,
            'teacher',
            403,
        ),
        (
            'some_owner',
            Problem.VISIBILITY_PUBLIC,
            'content_manager',
            204,
        ),
        # обновление своих задач
        (
            'teacher',
            Problem.VISIBILITY_PRIVATE,
            'teacher',
            204,
        ),
        (
            'content_manager',
            Problem.VISIBILITY_PRIVATE,
            'content_manager',
            204,
        ),
        (
            'teacher',
            Problem.VISIBILITY_PUBLIC,
            'teacher',
            204,
        ),
        (
            'content_manager',
            Problem.VISIBILITY_PUBLIC,
            'content_manager',
            204,
        ),
    )

    @pytest.mark.parametrize('owner_name,visibility,role_name,expected_code',
                             delete_data)
    def test_delete(self, jclient, owner_name, visibility, role_name,
                    expected_code, subject_model, student, teacher,
                    content_manager, some_owner):
        """Тест прав удаления задачи"""
        users = {
            'some_owner': some_owner,
            'student': student,
            'teacher': teacher,
            'content_manager': content_manager,
        }
        user = users.get(role_name)
        problem = Problem.objects.create(
            markup={},
            subject=subject_model,
            owner=users[owner_name],
            visibility=visibility,
        )
        if user:
            jclient.login(user=user)
        update_url = reverse('v2:problem-detail', args=(problem.id,))

        response = jclient.delete(update_url)
        assert response.status_code == expected_code, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
