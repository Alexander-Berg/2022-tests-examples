from builtins import str, object
import pytest

from django.contrib.auth import get_user_model
from django.core.urlresolvers import reverse

from kelvin.lessons.models import Lesson, LessonProblemLink
from kelvin.results.models import LessonResult, LessonSummary
from kelvin.subjects.models import Theme, Subject

User = get_user_model()


@pytest.mark.xfail
@pytest.mark.django_db
class TestLessonResultViewSet(object):
    """
    Проверка ручек результатов прохождения занятия
    """
    def test_list(self, jclient, some_owner, student):
        """
        Проверка получения списка результатов
        """
        # Нужно залогиниться для просмотра
        jclient.login()
        list_url = reverse('v2:lesson_result-list')

        # пустой список
        response = jclient.get(list_url)
        assert response.status_code == 200, u'Неправильный статус ответа'
        assert response.json() == {
            'count': 0, 'next': None, 'previous': None, 'results': []}, (
            u'Должен быть пустой список')

        # создаем урок и результаты
        subject = Subject.objects.create(name=u'Предмет')
        theme = Theme.objects.create(id=1, subject=subject)
        lesson = Lesson.objects.create(owner=some_owner, theme=theme,
                                       name=u'Тема')
        summary = LessonSummary.objects.create(student=student, lesson=lesson)
        result1 = LessonResult(
            summary=summary,
            points=10,
            max_points=10,
            answers={
                '11': [
                    {
                        'markers': {
                            '1': {
                                'user_answer': 3,
                                'mistakes': 1,
                                'max_mistakes': 1,
                            },
                        },
                        'mistakes': 1,
                        'max_mistakes': 1,
                        'completed': True,
                        'points': 0,
                        'answered': False,
                    },
                    {
                        'markers': {
                            '2': {
                                'user_answer': 0,
                                'mistakes': 1,
                                'max_mistakes': 1,
                            },
                        },
                        'mistakes': 1,
                        'max_mistakes': 1,
                        'completed': True,
                        'points': 20,
                        'answered': True,
                    },
                ],
            },
        )
        result2 = LessonResult(
            summary=summary,
            points=3, max_points=10, answers={},
        )
        result1.save()
        result2.save()

        response = jclient.get(list_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        answer = response.json()
        assert len(answer['results']) == 2, u'Должно быть 2 результата'
        for result in answer['results']:
            assert 'id' in result, u'Должен быть идентификатор попытки'
            assert 'lesson' in result, u'Должен быть идентификатор занятия'
            assert result['lesson'] == lesson.id, (
                u'Неправильный идентификатор занятия')
            assert 'student' in result, (
                u'Должен быть ученик, прошедший занятие')
            assert result['student'] == student.id, (
                u'Неправильный идентификатор ученика')
            assert 'answers' in result, u'Должны быть ответы пользователя'
            assert 'points' in result, (
                u'Должно быть количество баллов за занятие')
            if result['id'] == result1.id:
                assert ((result['answers'], result['points']) ==
                        ({
                            '11': [
                                {
                                    'status': 0,
                                    'completed': True,
                                    'spent_time': None,
                                    'markers': {
                                        '1': {
                                            'status': 0,
                                            'user_answer': 3,
                                            'mistakes': 1,
                                            'max_mistakes': 1,
                                        },
                                    },
                                    'points': 0,
                                    'comment': '',
                                    'answered': False,
                                },
                                {
                                    'status': 0,
                                    'completed': True,
                                    'spent_time': None,
                                    'markers': {
                                        '2': {
                                            'status': 0,
                                            'user_answer': 0,
                                            'mistakes': 1,
                                            'max_mistakes': 1,
                                        },
                                    },
                                    'points': 20,
                                    'comment': '',
                                    'answered': True,
                                },
                            ],
                         }, 10)), (
                    u'Неправильные ответы и баллы в результате')
            elif result['id'] == result2.id:
                assert ((result['answers'], result['points']) == ({}, 3)), (
                    u'Неправильные ответы и баллы в результате')

    def test_list_filter(self, jclient, some_owner):
        """
        Проверка фильтрации по полям модели
        """
        lesson1 = Lesson.objects.create(owner=some_owner)
        lesson2 = Lesson.objects.create(owner=some_owner)
        user1 = User.objects.create(username='user1')
        user2 = User.objects.create(username='user2')
        summary_user1_lesson1 = LessonSummary.objects.create(
            student=user1, lesson=lesson1)
        summary_user2_lesson1 = LessonSummary.objects.create(
            student=user2, lesson=lesson1)
        summary_user2_lesson2 = LessonSummary.objects.create(
            student=user2, lesson=lesson2)
        LessonResult.objects.create(
            answers={}, points=1, max_points=1, completed=True,
            summary=summary_user1_lesson1,
        )
        LessonResult.objects.create(
            answers={},  points=1, max_points=1, completed=True,
            summary=summary_user2_lesson1,
        )
        LessonResult.objects.create(
            answers={}, points=1, max_points=1, completed=True,
            summary=summary_user2_lesson2,
        )
        LessonResult.objects.create(
            answers={}, points=1, max_points=1, completed=False,
            summary=summary_user2_lesson1,
        )
        get_url = reverse('v2:lesson_result-list')

        # Без авторизации список посмотреть не можем
        response = jclient.get(get_url, {'completed': True})
        assert response.status_code == 401, (
            u'Неправильный статус ответа, ответ {0}'.format(response.content))
        assert response.json() == {
            'detail': 'Authentication credentials were not provided.'}, (
            u'Неправильный ответ')

        # Логинимся
        jclient.login()
        response = jclient.get(get_url, {'completed': True})
        assert response.status_code == 200, (
            u'Неправильный код ответа: {0}'.format(response.json()))
        assert len(response.json()['results']) == 3

        response = jclient.get(
            get_url, {'completed': True, 'student': user1.id})
        assert response.status_code == 200, (
            u'Неправильный код ответа: {0}'.format(response.json()))
        assert len(response.json()['results']) == 1

        response = jclient.get(
            get_url, {'completed': True, 'lesson': lesson1.id})
        assert response.status_code == 200, (
            u'Неправильный код ответа: {0}'.format(response.json()))
        assert len(response.json()['results']) == 2

    def test_retrieve(self, jclient, some_owner, student):
        """
        Проверка получения одного результата
        """
        get_url = reverse('v2:lesson_result-detail', args=(1,))

        # Без авторизации никакой результат получить не можем
        response = jclient.get(get_url, {'completed': True})
        assert response.status_code == 401, (
            u'Неправильный статус ответа, ответ {0}'.format(response.content))
        assert response.json() == {
            'detail': 'Authentication credentials were not provided.'}, (
            u'Неправильный ответ')

        # Логинимся произвольным пользователем
        jclient.login(user=some_owner)

        # запрос, когда нет результата
        get_url = reverse('v2:lesson_result-detail', args=(1,))
        response = jclient.get(get_url)
        assert response.status_code == 404, u'Неправильный статус ответа'

        # создаем урок и результаты
        subject = Subject.objects.create(name=u'Предмет')
        theme = Theme.objects.create(id=1, subject=subject)
        lesson = Lesson.objects.create(owner=some_owner, theme=theme,
                                       name=u'Урок')
        summary = LessonSummary.objects.create(student=student, lesson=lesson)
        result1 = LessonResult(
            points=10, max_points=10, summary=summary,
            answers={'100': [{'1': 3}, {'2': 0}]},
        )
        result2 = LessonResult(
            summary=summary, points=3, max_points=10, answers={},
        )
        result1.save()
        result2.save()

        get_url = reverse('v2:lesson_result-detail', args=(result2.id,))

        # Чужой результат получить не можем
        response = jclient.get(get_url, {'completed': True})
        assert response.status_code == 403, (
            u'Неправильный статус ответа, ответ {0}'.format(response.content))
        assert response.json() == {
            'detail': 'You do not have permission to perform this action.'}, (
            u'Неправильный ответ')

        # Логинимся пользователем, которому принадлежит результат
        jclient.logout()
        jclient.login(user=student)
        response = jclient.get(get_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        expected = {
            'answers': {},
            'id': result2.id,
            'lesson': lesson.id,
            'points': 3,
            'max_points': 10,
            'spent_time': None,
            'student': student.id,
            'work_out': False,
            'completed': True,
        }
        answer = response.json()
        assert answer.pop('date_created'), u'Должна быть дата создания'
        assert answer.pop('date_updated'), u'Должна быть дата обновления'
        assert answer == expected, u'Неправильный результат'

    def test_create_invalid(self, jclient, some_owner):
        """
        Проверка создания результата из неправильных данных
        """
        create_url = reverse('v2:lesson_result-list')

        subject = Subject.objects.create(name=u'Предмет')
        theme = Theme.objects.create(id=1, subject=subject)
        lesson = Lesson.objects.create(owner=some_owner, theme=theme,
                                       name=u'Урок')
        create_data = {
            'lesson': lesson.id,
        }
        response = jclient.post(create_url, create_data)
        assert response.status_code == 401, (
            u'Надо быть авторизованным для создания')

        # авторизуемся и повторяем попытку
        jclient.login()

        # надо посылать список словарей
        create_data = {
            'lesson': lesson.id,
            'answers': [0, 1],
        }
        response = jclient.post(create_url, create_data)
        assert response.status_code == 400, u'Должен быть список словарей'
        assert (response.json() ==
                {'answers': [u'[0, 1] is not of type \'object\'']}), (
            u'Неправильное сообщение об ошбке')

    def test_create_valid(self, jclient, lesson_models):
        """
        Проверка создания результата из правильных данных
        """
        create_url = reverse('v2:lesson_result-list')
        jclient.login()

        # тестовые данные
        lesson, problem1, problem2, link1, link2 = lesson_models
        user_answers = {
            link1.id: [
                {
                    'markers': {
                        '1': {
                            'user_answer': {'1': '2'},
                            'answer_status': 100.
                        },
                    },
                    'mistakes': 100500,
                    'max_mistakes': 500000,
                    'answered': True,
                },
                {
                    '1': {
                        'user_answer': {'1': '4.0'},
                    },
                },
            ],
            link2.id: {
                '1': {'user_answer': [0, 1]},
            }
        }
        create_data = {
            'answers': user_answers,
            'lesson': lesson.id,
            'spent_time': 100500,
        }
        expected = {
            'answers': {
                str(link1.id): [
                    {
                        'markers': {
                            '1': {
                                'answer_status': 100,
                                'user_answer': {'1': '2'},
                                'status': 0,
                                # этот ответ не проверяется
                                # поэтому нет `mistakes`
                            },
                        },
                        'status': 0,
                        'completed': True,
                        'spent_time': None,
                        'points': None,
                        'comment': '',
                        'answered': True,
                    },
                    {
                        'markers': {
                            '1': {
                                'answer_status': {'1': True},
                                'user_answer': {'1': '4.0'},
                                'status': 1,
                                'mistakes': 0,
                                'max_mistakes': 1,
                            },
                        },
                        'status': 1,
                        'completed': True,
                        'spent_time': None,
                        'points': 16,
                        'comment': '',
                        'answered': False,
                    },
                ],
                str(link2.id): [{
                    'markers': {
                        '1': {
                            'answer_status': [0, 1],
                            'user_answer': [0, 1],
                            'status': 0,
                            'mistakes': 2,
                            'max_mistakes': 3,
                        },
                    },
                    'status': 0,
                    'completed': True,
                    'spent_time': None,
                    'points': 0,
                    'comment': '',
                    'answered': False,
                }],
            },
            'points': 16,
            'max_points': 40,
            'spent_time': 100500,
            'lesson': lesson.id,
            'student': jclient.user.id,
            'work_out': False,
            'completed': True,
        }

        # запрос на создание попытки
        response = jclient.post(create_url, create_data)
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        answer = response.json()
        assert answer.pop('date_created'), (
            u'У попытки должно быть время создания')
        assert answer.pop('date_updated'), (
            u'У попытки должно быть время изменения')
        result_id = answer.pop('id')
        assert result_id, u'У попытки должен быть идентификатор'
        assert answer == expected, u'Неправильный ответ'

        # проверяем, что в базе лежат поля ошибок
        result = LessonResult.objects.get(id=result_id)
        assert result.answers[str(link1.id)][0]['mistakes'] == 100500
        assert result.answers[str(link1.id)][0]['max_mistakes'] == 500000

    def test_only_one_incomplete(self, jclient, lesson_models,
                                 student, student2):
        """
        Проверка того, что нельзя создать больше одной незавершенной попытки
        одним учеником для одного занятия
        """
        # Готовим данные
        lesson, problem1, problem2, link1, link2 = lesson_models
        answers = {
            str(link1.id): {
                'markers': {
                    '1': {
                        'user_answer': {'1': '2'},
                    },
                },
                'completed': False,
            },
        }
        # Создаем незавершенную попытку
        jclient.login(user=student)
        create_url = reverse('v2:lesson_result-list')
        create_data = {
            'answers': answers,
            'lesson': lesson.id,
            'spent_time': 100500,
            'completed': False,
        }
        response = jclient.post(create_url, create_data)
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        attempt_id = response.json()['id']

        # Нельзя создать еще одну незавершенную попытку
        expected = {
            u'non_field_errors': [u'there can be only one incomplete attempt'],
        }
        response = jclient.post(create_url, create_data)
        assert response.status_code == 400, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.json() == expected, u'Неправильный ответ'

        # Создаем незавершенную попытку другим пользователем
        jclient.login(user=student2)
        response = jclient.post(create_url, create_data)
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # Изначальным пользователем создаем незавершенную попытку
        # на другое занятие
        jclient.logout()
        jclient.login(user=student)
        lesson2 = Lesson.objects.get(id=lesson.id)
        lesson2.id = None
        lesson2.save()
        lproblem = LessonProblemLink.objects.create(
            lesson=lesson2, problem=problem1, order=1, type=1,
            options={'max_attempts': 5, 'show_tips': True},
        )
        create_data2 = {
            'answers': answers,
            'lesson': lesson2.id,
            'spent_time': 100500,
            'completed': False,
        }
        response = jclient.post(create_url, create_data2)
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # Завершаем изначальную попытку
        update_url = reverse('v2:lesson_result-detail', args=(attempt_id,))
        response = jclient.patch(update_url, {'completed': True})
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # Создаем новую незавершенную попытку на это занятие
        response = jclient.post(create_url, create_data)
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

    def test_update_patch(self, jclient, lesson_result_models):
        """Тесты частичного обновления попытки"""
        lesson_result, lesson, link1, link2 = lesson_result_models
        update_url = reverse('v2:lesson_result-detail', args=(lesson_result.id,))
        update_data = {
            'answers': {
                str(link1.id): {
                    'markers': {
                        '1': {
                            'user_answer': {'1': '2'},
                        }
                    },
                    'completed': False,
                    'answered': True,
                },
            },
        }
        expected_answers = {
            str(link1.id): [
                {
                    'markers': {
                        '1': {
                            'answer_status': {'1': False},
                            'user_answer': {'1': '2'},
                            'status': 0,
                            'mistakes': 1,
                            'max_mistakes': 1,
                        },
                    },
                    'status': 0,
                    'completed': False,
                    'spent_time': None,
                    'points': 0,
                    'comment': '',
                    'answered': True,
                },
            ],
            str(link2.id): [
                {
                    'markers': {
                        '1': {
                            'answer_status': [0, 1],
                            'user_answer': [0, 1],
                            'status': 0,
                            'mistakes': 1,
                            'max_mistakes': 3,
                        },
                    },
                    'status': 0,
                    'completed': True,
                    'spent_time': None,
                    'points': 0,
                    'comment': '',
                    'answered': False,
                },
            ],
        }
        response = jclient.patch(update_url, update_data)
        assert response.status_code == 200, (
            u'Неправильный статус ответа: {0}'.format(response.json()))
        answer = response.json()
        assert answer['answers'] == expected_answers, u'Неправильные ответы'
        lesson_result.refresh_from_db()
        assert lesson_result.completed is False, u'Попытка сама не завершается'
        assert lesson_result.points == 0, u'Число баллов пересчитано'

    def test_update_put(self, jclient, lesson_result_models):
        """Тесты полного обновления попытки"""
        lesson_result, lesson, link1, link2 = lesson_result_models
        update_url = reverse('v2:lesson_result-detail', args=(lesson_result.id,))
        update_data = {
            'answers': {
                str(link1.id): {
                    'markers': {
                        '1': {
                            'user_answer': {'1': '2'},
                        },
                    },
                    'completed': False,
                },
            },
            'lesson': lesson.id,
        }
        expected_answers = {
            str(link1.id): [
                {
                    'markers': {
                        '1': {
                            'answer_status': {'1': False},
                            'user_answer': {'1': '2'},
                            'status': 0,
                            'mistakes': 1,
                            'max_mistakes': 1,
                        },
                    },
                    'status': 0,
                    'completed': False,
                    'spent_time': None,
                    'points': 0,
                    'comment': '',
                    'answered': False,
                },
            ],
        }
        response = jclient.put(update_url, update_data)
        assert response.status_code == 200, (
            u'Неправильный статус ответа: {0}'.format(response.json()))
        answer = response.json()
        assert answer['answers'] == expected_answers, u'Неправильные ответы'
        lesson_result.refresh_from_db()
        assert lesson_result.completed is False, u'Попытка сама не завершается'
        assert lesson_result.points == 0, u'Число баллов пересчитано'

    def test_updates_with_wrong_user(self, jclient, student,
                                     lesson_result_models):
        """
        Тесты обновлений результата не тем пользователем, которому он
        принадлежит
        """
        jclient.logout()
        jclient.login(user=User.objects.create(username='result_update_user'))
        lesson_result, lesson, link1, link2 = lesson_result_models
        update_data = {'something': 'something'}
        update_url = reverse('v2:lesson_result-detail', args=(lesson_result.id,))

        # Не можем обновить результат put-запросом
        response = jclient.put(update_url, update_data)
        assert response.status_code == 403, (
            u'Неправильный статус ответа, ответ {0}'.format(response.content))
        assert response.json() == {
            'detail': 'You do not have permission to perform this action.'}, (
            u'Неправильный ответ')

        # Не можем обновить результат patch-запросом
        response = jclient.patch(update_url, update_data)
        assert response.status_code == 403, (
            u'Неправильный статус ответа, ответ {0}'.format(response.content))
        assert response.json() == {
            'detail': 'You do not have permission to perform this action.'}, (
            u'Неправильный ответ')
