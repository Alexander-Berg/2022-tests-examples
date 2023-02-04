from builtins import str, object
import datetime
from copy import deepcopy

import pytest
from django.conf import settings
from django.core.urlresolvers import reverse
from django.utils import timezone

from kelvin.common.utils_for_tests import (
    assert_has_error_message,
    extract_error_message,
)

from kelvin.common.utils import dt_to_microseconds
from kelvin.courses.models import Course, CourseLessonLink
from kelvin.lesson_assignments.models import LessonAssignment
from kelvin.lessons.models import LessonScenario
from kelvin.problems.answers import Answer
from kelvin.problems.markers import Marker
from kelvin.results.models import CourseLessonResult, CourseLessonSummary


@pytest.fixture
def course_models(lesson_models, subject_model, teacher):
    """
    Модель курса и связанные модели
    """
    lesson, problem1, problem2, link1, link2 = lesson_models
    course = Course.objects.create(
        name=u'Тестовый курс',
        subject=subject_model,
        owner=teacher,
    )
    clesson = CourseLessonLink.objects.create(
        course=course,
        lesson=lesson,
        order=1,
        date_assignment=timezone.now(),
    )
    return course, clesson


@pytest.fixture
def course_models_assignment(course_models, student):
    course, clesson = course_models
    problem_links_ids = [link.id for link in
                         clesson.lesson.lessonproblemlink_set.all()]
    LessonAssignment.objects.create(
        clesson=clesson, student=student, problems=problem_links_ids[:1],
    )
    return course, clesson


@pytest.fixture
def user_answers(lesson_models):
    lesson, problem1, problem2, link1, link2 = lesson_models
    answers = {
        link1.id: [
            {
                'markers': {
                    '1': {
                        'user_answer': {'1': '2'},
                    },
                },
                'answered': False,
            },
            {
                'markers': {
                    '1': {
                        'user_answer': {'1': '4.0'},
                    },
                },
                'answered': True,
            },
        ],
        link2.id: {
            '1': {
                'user_answer': [0, 1],
            },
        }
    }
    checked_answers = {
        str(link1.id): [
            {
                'markers': {
                    '1': {
                        'answer_status': {'1': False},
                        'user_answer': {'1': '2'},
                        'status': Marker.INCORRECT,
                        'mistakes': 1,
                        'max_mistakes': 1,
                    },
                },
                'theory': None,
                'custom_answer': None,
                'status': Answer.INCORRECT,
                'completed': True,
                'spent_time': None,
                'points': None,
                'comment': '',
                'answered': False,
            },
            {
                'markers': {
                    '1': {
                        'answer_status': {'1': True},
                        'user_answer': {'1': '4.0'},
                        'status': Marker.CORRECT,
                        'mistakes': 0,
                        'max_mistakes': 1,
                    },
                },
                'theory': None,
                'custom_answer': None,
                'status': Answer.CORRECT,
                'completed': True,
                'spent_time': None,
                'points': 1,
                'comment': '',
                'answered': True,
            },
        ],
        str(link2.id): [{
            'markers': {
                '1': {
                    'answer_status': [0, 1],
                    'user_answer': [0, 1],
                    'status': Marker.INCORRECT,
                    'mistakes': 2,
                    'max_mistakes': 3,
                },
            },
            'theory': None,
            'custom_answer': None,
            'status': Answer.INCORRECT,
            'completed': True,
            'spent_time': None,
            'points': 0,
            'comment': '',
            'answered': False,
        }],
    }
    return answers, checked_answers


@pytest.fixture
def user_answers2(lesson_models):
    lesson, problem1, problem2, link1, link2 = lesson_models
    answers = {
        link1.id: [
            {
                'markers': {
                    '1': {
                        'user_answer': {'1': '4.0'},
                    },
                },
            },
        ],
        link2.id: [{}],
    }
    checked_answers = {
        str(link1.id): [
            {
                'markers': {
                    '1': {
                        'answer_status': {'1': True},
                        'user_answer': {'1': '4.0'},
                        'status': Marker.CORRECT,
                        'mistakes': 0,
                        'max_mistakes': 1,
                    },
                },
                'theory': None,
                'custom_answer': None,
                'status': Answer.CORRECT,
                'completed': True,
                'spent_time': None,
                'points': 1,
                'comment': '',
                'answered': False,
            },
        ],
        str(link2.id): [{
            'markers': {
                '1': {
                    'answer_status': -1,
                    'status': Marker.INCORRECT,
                    'mistakes': 2,
                    'max_mistakes': 3,
                },
            },
            'theory': None,
            'custom_answer': None,
            'status': Answer.INCORRECT,
            'completed': True,
            'spent_time': None,
            'points': 0,
            'comment': '',
            'answered': False,
        }],
    }
    return answers, checked_answers


@pytest.mark.django_db
class TestCourseLessonResultViewSet(object):
    """
    Тесты методов работы с попытками на занятие в курсе
    """
    def test_create_with_assignment(self, jclient, course_models,
                                    user_answers, student):
        """
        Проверяет, что максимум баллов учитывает назначения и что ответы
        записываются только у назначенных задач
        """
        course, clesson = course_models
        answers, checked_answers = user_answers
        problem_links_ids = [link.id for link in
                             clesson.lesson.lessonproblemlink_set.all()]
        LessonAssignment.objects.create(
            clesson=clesson, student=student,
            problems=problem_links_ids[:1],
        )
        create_data = {
            'answers': answers,
            'clesson': clesson.id,
            'spent_time': 100500,
        }
        expected = {
            'answers': {
                str(problem_links_ids[0]): checked_answers.pop(
                    str(problem_links_ids[0])),
            },
            'points': 1,
            'max_points': 1,
            'student_viewed_problems': {},
            'spent_time': 100500,
            'clesson': clesson.id,
            'student': student.id,
            'work_out': False,
            'completed': True,
            'viewed': False,
        }
        create_url = reverse('v2:course_lesson_result-list')
        jclient.login(user=student)

        # создаем попытку
        response = jclient.post(create_url, create_data)
        answer = response.json()
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert answer.pop('date_created'), (
            u'У попытки должно быть время создания')
        assert answer.pop('date_updated'), (
            u'У попытки должно быть время изменения')
        assert answer.pop('id'), u'У попытки должен быть идентификатор'
        assert answer == expected, u'Неправильный ответ'

    # В задаче сгруппированы следующие ответы [[1, 2], [2, 3]]
    @pytest.mark.parametrize('answer,result', [
        (
            {
                '10': answer1,
                '20': answer2,
            },
            True,
        ) for answer1 in [1, 2] for answer2 in [2, 3]
    ] + [
        (
            {
                '10': answer2,
                '20': answer1,
            },
            True,
        ) for answer1 in [1, 2] for answer2 in [2, 3]
    ] + [(
        {
            '10': 1,
            '20': 1,
        },
        False,
    )])
    def test_create_with_input_union(self, jclient, subject_model, teacher,
                                     lesson_with_problem_with_union, student,
                                     answer, result):
        """
        Тест на корректную работу чека (IS_PERMUTATION_FROM), который
        используется для объединения инпутов
        """
        lesson, problem, link = lesson_with_problem_with_union
        course = Course.objects.create(
            name=u'Тестовый курс',
            subject=subject_model,
            owner=teacher,
        )
        clesson = CourseLessonLink.objects.create(
            course=course,
            lesson=lesson,
            order=1,
            date_assignment=timezone.now(),
        )

        problem_links_ids = [link.id for link in
                             clesson.lesson.lessonproblemlink_set.all()]
        LessonAssignment.objects.create(
            clesson=clesson, student=student,
            problems=problem_links_ids[:1],
        )
        create_data = {
            'answers': {
                link.id: [
                    {
                        'markers': {
                            '1': {
                                'user_answer': answer,
                            },
                        },
                    },
                ],
            },
            'clesson': clesson.id,
            'spent_time': 100500,
        }
        create_url = reverse('v2:course_lesson_result-list')
        jclient.login(user=student)

        # создаем попытку
        response = jclient.post(create_url, create_data)
        answer = response.json()
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        answers = answer['answers'][str(link.id)]
        assert answers[-1]['markers']['1']['answer_status']['100'] == result, (
            u'Неправильный ответ: ({})'.format(answers))

    def test_create_max_attempts(self, jclient, course_models, user_answers,
                                 student):
        """
        Проверяет, что можем создать не больше определенного числа попыток
        с параметром `work_out=False`
        """
        course, clesson = course_models

        # проставляем конкретные параметры сценария (даже если они дефолтные)
        clesson.max_attempts_in_group = 2
        clesson.mode = LessonScenario.TRAINING_MODE
        clesson.save()

        answers, checked_answers = user_answers
        create_data = {
            'answers': answers,
            'clesson': clesson.id,
            'spent_time': 100500,
        }
        expected = {
            'answers': checked_answers,
            'points': 1,
            'max_points': 2,
            'student_viewed_problems': {},
            'spent_time': 100500,
            'clesson': clesson.id,
            'student': student.id,
            'work_out': False,
            'completed': True,
            'viewed': False,
        }
        create_url = reverse('v2:course_lesson_result-list')
        jclient.login(user=student)

        # создаем первую попытку
        response = jclient.post(create_url, create_data)
        answer = response.json()
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert answer.pop('date_created'), (
            u'У попытки должно быть время создания')
        assert answer.pop('date_updated'), (
            u'У попытки должно быть время изменения')
        assert answer.pop('id'), u'У попытки должен быть идентификатор'
        assert answer == expected, u'Неправильный ответ'

        # создаем вторую попытку
        response = jclient.post(create_url, create_data)
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert CourseLessonResult.objects.count() == 2, (
            u'Должно быть 2 попытки')

        # нельзя создать третью попытку
        response = jclient.post(create_url, create_data)
        assert response.status_code == 400, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        assert_has_error_message(
            response.json(),
            source='non_field_errors',
            message=u'student reaches attempt limit',
        )

        assert CourseLessonResult.objects.count() == 2, (
            u'Должно быть 2 попытки')

        # но можно создать внеурочную попытку
        create_data = {
            'answers': answers,
            'clesson': clesson.id,
            'spent_time': 100,
            'work_out': True,
        }
        expected = {
            'answers': checked_answers,
            'points': 1,
            'max_points': 2,
            'student_viewed_problems': {},
            'spent_time': 100,
            'clesson': clesson.id,
            'student': student.id,
            'work_out': True,
            'completed': True,
            'viewed': False,
        }
        response = jclient.post(create_url, create_data)
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        answer = response.json()
        assert answer.pop('date_created'), (
            u'У попытки должно быть время создания')
        assert answer.pop('date_updated'), (
            u'У попытки должно быть время изменения')
        assert answer.pop('id'), u'У попытки должен быть идентификатор'
        assert answer == expected, u'Неправильный ответ'
        assert CourseLessonResult.objects.count() == 3, (
            u'Должно быть 3 попытки')

    def test_create_for_future_clesson(self, jclient, student, teacher,
                                       content_manager, course_models,
                                       user_answers):
        """
        Проверяет, что ученик не может создать попытку для занятия в будущем,
        а учитель и контент менеджер могут
        """
        course, clesson = course_models
        clesson.date_assignment = timezone.now() + datetime.timedelta(days=1)
        clesson.save()

        answers, checked_answers = user_answers
        create_data = {
            'answers': answers,
            'clesson': clesson.id,
            'spent_time': 100500,
        }
        create_url = reverse('v2:course_lesson_result-list')

        # ученик не может создать резульатт
        jclient.login(user=student)
        response = jclient.post(create_url, create_data)
        assert response.status_code == 400, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # учитель может создать результат
        jclient.login(user=teacher)
        expected = {
            'answers': checked_answers,
            'points': 1,
            'max_points': 2,
            'student_viewed_problems': {},
            'spent_time': 100500,
            'clesson': clesson.id,
            'student': teacher.id,
            'work_out': False,
            'completed': True,
            'viewed': False,
        }
        response = jclient.post(create_url, create_data)
        answer = response.json()
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert answer.pop('date_created'), (
            u'У попытки должно быть время создания')
        assert answer.pop('date_updated'), (
            u'У попытки должно быть время изменения')
        assert answer.pop('id'), u'У попытки должен быть идентификатор'
        assert answer == expected, u'Неправильный ответ'

        # контент-менеджер может создать результат
        jclient.login(user=content_manager)
        expected = {
            'answers': checked_answers,
            'points': 1,
            'max_points': 2,
            'student_viewed_problems': {},
            'spent_time': 100500,
            'clesson': clesson.id,
            'student': content_manager.id,
            'work_out': False,
            'completed': True,
            'viewed': False,
        }
        response = jclient.post(create_url, create_data)
        answer = response.json()
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert answer.pop('date_created'), (
            u'У попытки должно быть время создания')
        assert answer.pop('date_updated'), (
            u'У попытки должно быть время изменения')
        assert answer.pop('id'), u'У попытки должен быть идентификатор'
        assert answer == expected, u'Неправильный ответ'

    def test_only_one_incomplete(self, jclient, course_models, user_answers,
                                 student, student2):
        """
        Проверка того, что нельзя создать больше одной незавершенной попытки
        в классе либо вне класса одним учеником для одного курсозанятия
        """
        # Готовим данные
        course, clesson = course_models
        answers, checked_answers = user_answers
        clesson.max_attempts_in_group = 2
        clesson.mode = LessonScenario.TRAINING_MODE
        clesson.save()

        # Создаем незавершенную попытку
        jclient.login(user=student)
        create_url = reverse('v2:course_lesson_result-list')
        create_data = {
            'answers': answers,
            'clesson': clesson.id,
            'spent_time': 100500,
            'completed': False,
            'work_out': False,
        }
        response = jclient.post(create_url, create_data)
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        attempt_id = response.json()['id']

        # Нельзя создать еще одну незавершенную попытку
        expected = u'there can be only one incomplete attempt'

        response = jclient.post(create_url, create_data)
        assert response.status_code == 400, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert (
            extract_error_message(response.json()) == expected
        )

        # Обновить можно put- и patch-запросами
        update_url = reverse(
            'v2:course_lesson_result-detail', args=(attempt_id,)
        )
        response = jclient.put(update_url, create_data)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        response = jclient.patch(update_url, {'spent_time': 100501})
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # Создаем незавершенную попытку другим пользователем
        jclient.login(user=student2)
        response = jclient.post(create_url, create_data)
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # Изначальным пользователем создаем незавершенную попытку "в классе"
        # на другое курсозанятие
        jclient.login(user=student)
        clesson2 = CourseLessonLink.objects.create(
            course=course,
            lesson=clesson.lesson,
            order=2,
            date_assignment=timezone.now()
        )
        create_data2 = {
            'answers': answers,
            'clesson': clesson2.id,
            'spent_time': 100500,
            'completed': False,
            'work_out': False,
        }
        response = jclient.post(create_url, create_data2)
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # На него же можем создать незавершенную попытку "вне класса"
        create_data3 = {key: value for key, value in create_data2.items()}
        create_data3['work_out'] = True
        response = jclient.post(create_url, create_data3)
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # Завершаем изначальную попытку
        CourseLessonResult.objects.filter(id=attempt_id).update(completed=True)

        # Создаем новую незавершенную попытку на это курсозанятие
        response = jclient.post(create_url, create_data)
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

    def test_create_anonymous(self, jclient, course_models, user_answers):
        """
        Проверяет, что анонимный пользователь может создать результат в курсе
        для анонимоных пользователей
        """
        course, clesson = course_models
        create_data = {
            'answers': {},
            'clesson': clesson.id,
            'spent_time': 0,
        }
        expected = {
            'answers': {},
            'points': 0,
            'max_points': 2,
            'student_viewed_problems': {},
            'spent_time': 0,
            'clesson': clesson.id,
            'student': None,
            'work_out': False,
            'completed': True,
            'viewed': False,
        }
        create_url = reverse('v2:course_lesson_result-list')

        # создаем попытку в обычном курсе
        response = jclient.post(create_url, create_data)
        assert response.status_code == 400, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # создаем попытку в курсе для анонимных пользователей
        course.allow_anonymous = True
        course.save()
        response = jclient.post(create_url, create_data)
        answer = response.json()
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert answer.pop('date_created'), (
            u'У попытки должно быть время создания')
        assert answer.pop('date_updated'), (
            u'У попытки должно быть время изменения')
        first_id = answer.pop('id')
        assert first_id, u'У попытки должен быть идентификатор'
        assert answer == expected, u'Неправильный ответ'

        # анонимам можно создавать несколько незавершенных попыток
        create_data = {
            'answers': {},
            'clesson': clesson.id,
            'spent_time': 0,
            'completed': False,
        }
        response = jclient.post(create_url, create_data)
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        response = jclient.post(create_url, create_data)
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

    def test_update_full_attempt(self, jclient, course_models, user_answers,
                                 user_answers2, student):
        """
        Проверяет, что незавершенные попытки можно обновить полностью
        с помощью PUT-запроса
        """
        course, clesson = course_models

        answers, checked_answers = user_answers
        create_data = {
            'answers': answers,
            'clesson': clesson.id,
            'spent_time': 100500,
            'completed': False,
        }
        create_url = reverse('v2:course_lesson_result-list')
        jclient.login(user=student)

        # создаем незавершенную попытку
        response = jclient.post(create_url, create_data)
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        answer = response.json()
        attempt_id = answer.pop('id')

        # изменяем попытку
        answers, checked_answers = user_answers2
        update_url = reverse('v2:course_lesson_result-detail', args=(attempt_id,))
        update_data = {
            'answers': answers,
            'clesson': clesson.id,
            'spent_time': 100,
            'completed': True,
            'viewed': True,
            # только для чтения
            'points': 0,
            'max_points': 0,
        }
        expected = {
            'answers': checked_answers,
            'points': 1,
            'max_points': 2,
            'student_viewed_problems': {},
            'spent_time': 100,
            'clesson': clesson.id,
            'student': student.id,
            'work_out': False,
            'completed': True,
            'viewed': True,
        }
        response = jclient.put(update_url, update_data)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        answer = response.json()
        assert answer.pop('date_created'), (
            u'У попытки должно быть время создания')
        assert answer.pop('date_updated'), (
            u'У попытки должно быть время изменения')
        assert answer.pop('id') == attempt_id, (
            u'У попытки должен быть идентификатор')
        assert answer == expected, u'Неправильный ответ'
        assert CourseLessonResult.objects.count() == 1, (
            u'Должна быть одна попытка')

        # завершенную попытку нельзя изменить
        update_data = deepcopy(create_data)
        update_data.pop('completed')
        expected = u"can't modify completed result"
        response = jclient.put(update_url, update_data)
        assert response.status_code == 400, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert extract_error_message(response.json()) == expected, (
            u'Неправильный ответ'
        )

    def test_update_work_out(self, jclient, student, user_answers,
                             user_answers2, course_models):
        """
        Тест обновления внеурочного занятия
        """
        course, clesson = course_models
        clesson.max_attempts_in_group = 0
        clesson.save()

        answers, checked_answers = user_answers
        create_data = {
            'answers': answers,
            'clesson': clesson.id,
            'completed': False,
            'work_out': True,
        }
        create_url = reverse('v2:course_lesson_result-list')
        jclient.login(user=student)

        # создаем незавершенную попытку
        response = jclient.post(create_url, create_data)
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        answer = response.json()
        attempt_id = answer.pop('id')
        assert answer.pop('date_created'), (
            u'У попытки должно быть время создания')
        assert answer.pop('date_updated'), (
            u'У попытки должно быть время изменения')
        expected = {
            'answers': checked_answers,
            'points': 1,
            'max_points': 2,
            'student_viewed_problems': {},
            'spent_time': None,
            'clesson': clesson.id,
            'student': student.id,
            'work_out': True,
            'completed': False,
            'viewed': False,
        }
        assert answer == expected, u'Неправильный ответ'

        # изменяем попытку
        answers, checked_answers = user_answers2
        update_url = reverse('v2:course_lesson_result-detail', args=(attempt_id,))
        update_data = {
            'answers': answers,
            'clesson': clesson.id,
            'spent_time': 100,
            # только для чтения
            'points': 0,
            'max_points': 0,
        }
        expected = {
            'answers': checked_answers,
            'points': 1,
            'max_points': 2,
            'student_viewed_problems': {},
            'spent_time': 100,
            'clesson': clesson.id,
            'student': student.id,
            'work_out': True,
            'completed': False,
            'viewed': False,
        }
        response = jclient.patch(update_url, update_data)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        answer = response.json()
        assert answer.pop('date_created'), (
            u'У попытки должно быть время создания')
        assert answer.pop('date_updated'), (
            u'У попытки должно быть время изменения')
        assert answer.pop('id') == attempt_id, (
            u'У попытки должен быть идентификатор')
        assert answer == expected, u'Неправильный ответ'
        assert CourseLessonResult.objects.count() == 1, (
            u'Должна быть одна попытка')

        # нельзя сделать попытку "урочной"
        update_data = {
            'work_out': False,
        }
        response = jclient.patch(update_url, update_data)
        assert response.status_code == 400, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert (
            extract_error_message(response.json()) ==
            'can not modify to non-workout attempt'
        )

    def test_updates_with_wrong_user(self, jclient, student, teacher,
                                     course_models, user_answers):
        """
        Тесты обновлений результата не тем пользователем, которому он
        принадлежит
        """
        course, clesson = course_models
        answers, checked_answers = user_answers
        summary = CourseLessonSummary.objects.create(
            clesson=clesson,
            student=student,
        )
        result = CourseLessonResult.objects.create(
            summary=summary,
            answers=answers,
            spent_time=15,
            points=1050,
            max_points=100500,
        )
        # Логинимся другим пользователем
        jclient.login(user=teacher)
        update_url = reverse('v2:course_lesson_result-detail', args=(result.id,))
        update_data = {'some': 'thing'}

        # Не можем обновить результат put-запросом
        response = jclient.put(update_url, update_data)
        assert response.status_code == 403, (
            u'Неправильный статус ответа, ответ {0}'.format(response.content))

        # Не можем обновить результат patch-запросом
        response = jclient.patch(update_url, update_data)
        assert response.status_code == 403, (
            u'Неправильный статус ответа, ответ {0}'.format(response.content))

    def test_update_not_assigned_clesson(self, jclient, content_manager,
                                         teacher, course_models, user_answers):
        """
        Проверяет, что учитель и контент-менеджер могут проходить невыданное
        занятие
        """
        course, clesson = course_models
        clesson.date_assignment = None
        clesson.save()
        answers, checked_answers = user_answers
        for user in (content_manager, teacher):
            summary = CourseLessonSummary.objects.create(
                clesson=clesson,
                student=user,
            )
            result = CourseLessonResult.objects.create(
                summary=summary,
                answers=answers,
                spent_time=15,
                points=1050,
                max_points=100500,
                completed=False,
            )
            jclient.login(user=user)
            update_url = reverse('v2:course_lesson_result-detail',
                                 args=(result.id,))
            update_data = {'spent_time': 120}
            response = jclient.patch(update_url, update_data)
            assert response.status_code == 200, (
                u'Неправильный статус ответа, ответ {0}'
                .format(response.content)
            )
            assert response.json()['spent_time'] == 120

    def test_update_completed(self, jclient, course_models, user_answers,
                                 student):
        """
        Проверяет, что можно завершить попытку прохождения контрольной,
        обновив поле completed
        """
        course, clesson = course_models

        clesson.mode = CourseLessonLink.CONTROL_WORK_MODE
        clesson.finish_date = timezone.now() + datetime.timedelta(
            days=1)
        clesson.duration = 10
        clesson.save()
        problem_links_ids = clesson.lesson.lessonproblemlink_set.values_list(
            'id', flat=True)
        LessonAssignment.objects.create(
            clesson=clesson, student=student,
            problems=list(problem_links_ids[:1]),
        )

        answers, checked_answers = user_answers
        create_data = {
            'answers': answers,
            'clesson': clesson.id,
            'completed': False,
        }
        create_url = reverse('v2:course_lesson_result-list')
        jclient.login(user=student)

        # создаем незавершенную попытку
        response = jclient.post(create_url, create_data)
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        answer = response.json()
        attempt_id = answer.pop('id')

        # завершаем попытку
        update_url = reverse('v2:course_lesson_result-detail', args=(attempt_id,))
        update_data = {
            'completed': True,
        }

        response = jclient.patch(update_url, update_data)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        result = CourseLessonResult.objects.get(pk=attempt_id)
        assert result.completed is True, 'Попытка должна быть завершена'

    @pytest.mark.xfail
    def test_versions(self, jclient,
                      student, teacher, user_answers, course_models):
        """
        Тест получения версий результатов по курсу
        """
        course, clesson = course_models
        jclient.login(user=student)

        # запрос без параметров вернет ошибку
        versions_url = reverse('v2:course_lesson_result-versions')
        response = jclient.get(versions_url)
        assert response.status_code == 400

        response = jclient.get(versions_url, {'course': course.id})
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # пока нет результатов, ответ должен быть пустой
        assert response.json() == {}

        # добавляем попытку пользователя
        create_url = reverse('v2:course_lesson_result-list')
        create_data = {
            'answers': user_answers[0],
            'clesson': clesson.id,
            'completed': True,
            'work_out': True
        }
        response = jclient.post(create_url, create_data)
        assert response.status_code == 201

        # подменяем дату изменения, чтобы дальше ее проверить
        CourseLessonResult.objects.filter(id=response.json()['id']).update(
            date_updated=datetime.datetime(2014, 8, 25)
        )

        # ожидаемый ответ ручки
        expected_data = {str(clesson.id): 1408924800}

        # запрашиваем версии результатов
        response = jclient.get(versions_url, {'course': course.id})
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.json() == expected_data

        # создаем еще одну попытку для этого пользователя и подменяем ее дату
        # на более старую — не должно отразиться на результате
        response = jclient.post(create_url, create_data)
        assert response.status_code == 201
        CourseLessonResult.objects.filter(id=response.json()['id']).update(
            date_updated=datetime.datetime(2013, 8, 25)
        )

        # создаем попытку другого пользователя, она
        # не должна отразиться в результате
        jclient.login(user=teacher)
        response = jclient.post(create_url, create_data)
        assert response.status_code == 201

        # запрашиваем версии результатов еще раз
        jclient.login(user=student)
        response = jclient.get(versions_url, {'course': course.id})
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.json() == expected_data

    def test_update_with_date_updated_filter(
            self, jclient, course_models, user_answers, student):
        """
        Тест обновления попытки с фильтрацией по `date_updated`
        """
        course, clesson = course_models

        answers, checked_answers = user_answers
        create_data = {
            'clesson': clesson.id,
            'spent_time': 100500,
            'completed': False,
        }
        create_url = reverse('v2:course_lesson_result-list')
        jclient.login(user=student)

        # создаем незавершенную попытку
        response = jclient.post(create_url, create_data)
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        answer = response.json()
        date_updated = answer.get('date_updated')
        result_id = answer.get('id')

        # изменяем результат
        CourseLessonResult.objects.get(id=result_id).save()
        retrieve_url = reverse('v2:course_lesson_result-detail', args=(result_id,))

        # пытаемся изменить результат со старым `date_updated`
        response = jclient.patch(
            retrieve_url + '?date_updated={0}'.format(date_updated),
            {'answers': answers},
        )
        assert response.status_code == 404, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # получаем обновленную попытку и пытаемся сохранить с новым
        # `date_updated`
        result = jclient.get(retrieve_url).json()
        response = jclient.patch(
            retrieve_url + '?date_updated={0}'.format(result['date_updated']),
            {'answers': answers},
        )
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        answer = response.json()

        expected = {
            'answers': checked_answers,
            'points': 1,
            'max_points': 2,
            'student_viewed_problems': {},
            'spent_time': 100500,
            'clesson': clesson.id,
            'student': student.id,
            'work_out': False,
            'completed': False,
            'viewed': False,
        }
        assert answer.pop('date_created'), (
            u'У попытки должно быть время создания')
        assert answer.pop('date_updated'), (
            u'У попытки должно быть время изменения')
        assert answer.pop('id') == result['id'], (
            u'У попытки должен быть идентификатор')
        assert answer == expected, u'Неправильный ответ'

    def test_update_anonymous(self, jclient, course_models,
                                    user_answers, student):
        """
        Проверяет обновление результата анонимным пользователем
        """
        course, clesson = course_models
        course.allow_anonymous = True
        course.save()
        answers, checked_answers = user_answers
        create_url = reverse('v2:course_lesson_result-list')
        create_data = {
            'answers': {},
            'clesson': clesson.id,
            'spent_time': 10,
            'completed': False,
        }

        # создаем попытку
        response = jclient.post(create_url, create_data)
        answer = response.json()
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # обновляем попытку
        update_url = reverse('v2:course_lesson_result-detail',
                             args=(answer['id'],))
        update_data = {
            'answers': answers,
            'clesson': clesson.id,
            'spent_time': 100,
        }
        expected = {
            'answers': checked_answers,
            'points': 1,
            'max_points': 2,
            'student_viewed_problems': {},
            'spent_time': 100,
            'clesson': clesson.id,
            'student': None,
            'work_out': False,
            'completed': False,
            'viewed': False,
        }
        response = jclient.put(update_url, update_data)
        answer = response.json()
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert answer.pop('date_created'), (
            u'У попытки должно быть время создания')
        assert answer.pop('date_updated'), (
            u'У попытки должно быть время изменения')
        assert answer.pop('id'), u'У попытки должен быть идентификатор'
        assert answer == expected, u'Неправильный ответ'

        # проверяем, что анонимный пользователь не может обновить попытку
        # авторизованного ученика
        jclient.login(user=student)
        response = jclient.post(create_url, create_data)
        answer = response.json()
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        update_url = reverse('v2:course_lesson_result-detail',
                             args=(answer['id'],))
        jclient.logout()
        response = jclient.put(update_url, update_data)
        assert response.status_code == 401, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

    def test_update_anonymous_result_by_authenticated_user(
            self, jclient, course_models, student, student2):
        """
        Проверяет, что залогиненый пользователь может приписать себе любой
        анонимный результат, если у него нет еще результата
        """
        course, clesson = course_models
        course.allow_anonymous = True
        course.save()
        create_url = reverse('v2:course_lesson_result-list')
        create_data = {
            'answers': {},
            'clesson': clesson.id,
            'spent_time': 10,
            'completed': False,
        }

        # создаем попытку анонимным пользователем
        response = jclient.post(create_url, create_data)
        answer = response.json()
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # анонимный пользователь не может использовать метод
        take_url = reverse('v2:course_lesson_result-take',
                           args=(answer['id'],))
        update_data = ''
        response = jclient.post(take_url, update_data)
        assert response.status_code == 401, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # обновляем попытку авторизованным пользователем
        expected = {
            'answers': {},
            'points': 0,
            'max_points': 2,
            'student_viewed_problems': {},
            'spent_time': 10,
            'clesson': clesson.id,
            'student': student.id,
            'work_out': False,
            'completed': False,
            'viewed': False,
        }
        jclient.login(user=student)
        response = jclient.post(take_url, update_data)
        answer = response.json()
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert answer.pop('date_created'), (
            u'У попытки должно быть время создания')
        assert answer.pop('date_updated'), (
            u'У попытки должно быть время изменения')
        assert answer.pop('id'), u'У попытки должен быть идентификатор'
        assert answer == expected, u'Неправильный ответ'

        # ученик не может взять попытку другого ученика
        jclient.login(user=student2)
        response = jclient.post(take_url, update_data)
        assert response.status_code == 403, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # второй раз пользователь не может приписать другую попытку
        jclient.logout()
        response = jclient.post(create_url, create_data)
        answer = response.json()
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        jclient.login(user=student)
        take_url = reverse('v2:course_lesson_result-take',
                           args=(answer['id'],))
        response = jclient.post(take_url, '')
        assert response.status_code == 400, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

    def test_diagnostics_results(
            self, jclient, course_models, user_answers, student):
        """
        Тест создания попытки по диагностике и получения результата
        """
        # TODO EDU-1387
        # завершенная диагностика
        course, clesson = course_models
        clesson.mode = LessonScenario.DIAGNOSTICS_MODE
        clesson.finish_date = timezone.now()
        clesson.save()

        create_url = reverse('v2:course_lesson_result-list')
        create_data = {
            'answers': user_answers[0],
            'clesson': clesson.id,
            'completed': True,
            'work_out': True,
        }
        jclient.login(user=student)
        response = jclient.post(create_url, create_data)
        assert response.status_code == 400
        assert (
            extract_error_message(response.json()) == 'time limit exceeded'
        )

        # незавершенная диагностика
        clesson.finish_date = timezone.now() + datetime.timedelta(days=1)
        clesson.save()
        response = jclient.post(create_url, create_data)
        assert response.status_code == 201

        result_id = response.json()['id']
        retrieve_url = reverse('v2:course_lesson_result-detail',
                               args=(result_id,))
        expected = {
            'student': student.id,
            'work_out': True,
            'completed': True,
            'viewed': False,
            'clesson': clesson.id,
            'max_points': 2,
            'student_viewed_problems': {},
            'points': 1,
            'spent_time': None,
        }

        response = jclient.get(retrieve_url)
        assert response.status_code == 200
        answer = response.json()
        assert answer.pop('date_created'), (
            u'У попытки должно быть время создания')
        assert answer.pop('date_updated'), (
            u'У попытки должно быть время изменения')
        assert answer.pop('id') == result_id
        assert answer.pop('answers') == user_answers[1]
        assert answer == expected

    def test_list_permissions(self, jclient, student, parent, teacher,
                              content_manager):
        """
        Проверяет, что метод списка доступен только учителям, родителям и
        контент-менеджерам
        """
        list_url = reverse('v2:course_lesson_result-list')

        # анонимный пользователь не имеет доступ
        response = jclient.get(list_url)
        assert response.status_code == 401

        # ученик не имеет доступ
        jclient.login(user=student)
        response = jclient.get(list_url)
        assert response.status_code == 403

        # учитель, родитель, контент-менеджер имеют доступ
        for user in (teacher, content_manager, parent):
            jclient.login(user=user)
            response = jclient.get(list_url)
            assert response.status_code == 200

    def test_detail_permissions(self, jclient, course_models, student):
        """
        Проверяет, что ученик может смотреть свой результат
        """
        course, clesson = course_models
        create_url = reverse('v2:course_lesson_result-list')
        create_data = {
            'answers': {},
            'clesson': clesson.id,
            'spent_time': 0,
        }
        jclient.login(user=student)
        response = jclient.post(create_url, create_data)
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        detail_url = reverse('v2:course_lesson_result-detail',
                             args=(response.json()['id'],))
        response = jclient.get(detail_url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

    def test_detail_viewed(self, jclient, course_models, user_answers,
                           student, student2):
        """
        Проверяет, что можно выставить завершенной попытке флаг viewed
        """
        course, clesson = course_models

        # создаем идущую в данный момент контрольную
        clesson.mode = CourseLessonLink.CONTROL_WORK_MODE
        clesson.date_created = timezone.now() - datetime.timedelta(
            days=3)
        clesson.finish_date = timezone.now() + datetime.timedelta(
            days=1)
        clesson.duration = 10
        clesson.save()

        answers, checked_answers = user_answers

        # создаем завершенную в прошлом попытку
        summary = CourseLessonSummary.objects.create(
            clesson=clesson,
            student=student,
        )
        result = CourseLessonResult.objects.create(
            summary=summary,
            answers=answers,
            spent_time=15,
            points=1050,
            max_points=100500,
            completed=True,
            viewed=False,
            date_created=timezone.now() - datetime.timedelta(days=2),
        )

        update_url = reverse('v2:course_lesson_result-viewed',
                             args=(result.id,))

        # неавторизованный пользователь не может делать запрос
        response = jclient.post(update_url, {})
        assert response.status_code == 401, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # авторизованный пользователь
        jclient.login(user=student)

        response = jclient.post(update_url, {})
        assert response.status_code == 400, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # контрольная уже окончена
        clesson.finish_date = timezone.now() - datetime.timedelta(
            days=1)
        clesson.save()

        response = jclient.post(update_url, {})
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        result.refresh_from_db()
        assert result.viewed is True, (
            u'Результат должен быть помечен просмотренным')

        # повторный запрос успешно проходит
        response = jclient.post(update_url, {})
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        result.refresh_from_db()
        assert result.viewed is True, (
            u'Результат должен быть помечен просмотренным')

        # нельзя просмотреть чужую попытку
        jclient.login(user=student2)
        response = jclient.post(update_url, {})
        assert response.status_code == 400, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        # нельзя исправлять не в контрольной работе
        jclient.login(user=student)
        for mode in (CourseLessonLink.DIAGNOSTICS_MODE,
                     CourseLessonLink.WEBINAR_MODE,
                     CourseLessonLink.TRAINING_MODE):
            clesson.mode = mode
            clesson.save()
            response = jclient.post(update_url, {})
            assert response.status_code == 400, (
                u'Неправильный статус ответа, ответ: {0}'
                .format(response.content)
            )

    def test_custom_answer_simple_create(self, jclient, user_custom_answer,
                                         course_models_assignment, student):
        """
        Проверяем создание попытки с ручной проверкой
        + Проставилась дата создания
        + Проставился id ученика
        """

        course, clesson = course_models_assignment
        answers = user_custom_answer
        create_data = {
            'answers': answers,
            'clesson': clesson.id,
            'spent_time': 100500,
        }
        create_url = reverse('v2:course_lesson_result-list')
        jclient.login(user=student)

        # создаем попытку
        response = jclient.post(create_url, create_data)
        answer = response.json()
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert answer['points'] == 0, (
            u'Только задачи с флагом ручной проверки должны оцениваться через '
            u'ручную проверку')
        for link_id in list(answers.keys()):
            custom_answer = (
                answer['answers'][str(link_id)][-1]['custom_answer'])
            assert custom_answer[0].pop('date'), (
                u'У сообщения должно быть время создания')
            assert custom_answer == [{
                'type': 'solution',
                'message': u'Отправляю решение',
                'user': {
                    'id': student.id,
                }
            }], u'Неправильный ответ ручной проверки'

    def test_custom_answer_simple_patch(self, jclient, user_custom_answer,
                                        course_models_assignment, student):
        """
        Проверяем изменение попытки с ручной проверкой
        """
        course, clesson = course_models_assignment
        for link in clesson.lesson.lessonproblemlink_set.all():
            link.problem.custom_answer = True
            link.problem.save()
        answers = user_custom_answer
        create_data = {
            'answers': {},
            'clesson': clesson.id,
            'spent_time': 100500,
            'completed': False,
        }
        create_url = reverse('v2:course_lesson_result-list')
        jclient.login(user=student)

        # создаем незавершенную попытку
        response = jclient.post(create_url, create_data)
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        answer = response.json()
        attempt_id = answer.pop('id')

        # завершаем попытку
        update_url = reverse('v2:course_lesson_result-detail',
                             args=(attempt_id,))
        update_data = {
            'answers': answers,
        }
        response = jclient.patch(update_url, update_data)
        answer = response.json()
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        res_answers = answer['answers']
        assert res_answers[list(res_answers.keys())[0]][0]['points'] is None, (
            u'Только задачи с флагом ручной проверки должны оцениваться через '
            u'ручную проверку')
        assert answer['points'] == 0, u'Сумма баллов должна быть 0'
        for link_id in list(answers.keys()):
            custom_answer = (
                answer['answers'][str(link_id)][-1]['custom_answer'])
            assert custom_answer[0].pop('date'), (
                u'У сообщения должно быть время создания')
            assert custom_answer == [{
                'type': 'solution',
                'message': u'Отправляю решение',
                'user': {
                    'id': student.id,
                }
            }], u'Неправильный ответ ручной проверки'

        # нельзя ответить, не передав ручной ответ
        for link_id in list(answers.keys()):
            answers[link_id][0].pop('custom_answer')
        update_data = {
            'answers': answers,
        }
        response = jclient.patch(update_url, update_data)
        assert response.status_code == 400, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

    def test_custom_answer_statuses(self, jclient, user_custom_answer,
                                    course_models_assignment, student):
        """
        Проверяем правильный расчет статусов для задачи с ручной проверкой
        + Пользователь сделал посылку - статус UNCHECKED
        + Учитель поставил cтатус SUMMARY_INCORRECT
        + Пользователь сделал еще посылку - статус UNCHECKED
        + Учитель поставил статус SUMMARY_INCORRECT (EDUCATION-1374)
        """

        course, clesson = course_models_assignment
        answers = user_custom_answer
        create_data = {
            'answers': answers,
            'clesson': clesson.id,
            'spent_time': 100500,
            'completed': False,
        }
        create_url = reverse('v2:course_lesson_result-list')
        jclient.login(user=student)

        # создаем попытку
        response = jclient.post(create_url, create_data)
        answer = response.json()
        attempt_id = answer.pop('id')
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert len(answer['answers']) == 1, u'В данном занятии только 1 задача'
        current_answer = answer['answers'][list(answer['answers'].keys())[0]]
        assert len(current_answer) == 1, u'Пока только 1 попытка'
        assert len(current_answer[0]['custom_answer']) == 1, (
            u'Должно быть 1 сообщение')
        assert current_answer[0]['status'] == Answer.UNCHECKED, (
            u'Статус должен быть UNCHECKED')

        # учитель стваит 0 баллов
        update_url = reverse('v2:course_lesson_result-detail',
                             args=(attempt_id,))
        answers[list(answers.keys())[0]][0]['custom_answer'].append(
            {
                'type': 'check',
                'message': u'Ответ с 0',
                'points': 0,
                'status': Answer.SUMMARY_INCORRECT,
            },
        )
        update_data = {
            'answers': answers,
        }
        response = jclient.patch(update_url, update_data)
        answer = response.json()
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert len(answer['answers']) == 1, u'В данном занятии только 1 задача'
        current_answer = answer['answers'][list(answer['answers'].keys())[0]]
        assert len(current_answer) == 1, u'Пока только 1 попытка'
        assert len(current_answer[0]['custom_answer']) == 2, (
            u'Должно быть 2 сообщения')
        assert current_answer[0]['status'] == Answer.SUMMARY_INCORRECT, (
            u'Статус должен быть SUMMARY_INCORRECT')

        # ученик делает новую посылку
        update_url = reverse('v2:course_lesson_result-detail',
                             args=(attempt_id,))
        answers[list(answers.keys())[0]].append(
            {
                'markers': {},
                'custom_answer': [
                    {
                        'type': 'solution',
                        'message': u'Новая посылка',
                    },
                ],
            }
        )
        update_data = {
            'answers': answers,
        }
        response = jclient.patch(update_url, update_data)
        answer = response.json()
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert len(answer['answers']) == 1, u'В данном занятии только 1 задача'
        current_answer = answer['answers'][list(answer['answers'].keys())[0]]
        assert len(current_answer) == 2, u'Должно быть 2 попытки'
        assert len(current_answer[1]['custom_answer']) == 1, (
            u'Должно быть 1 сообщение во второй посылке')
        assert current_answer[1]['status'] == Answer.UNCHECKED, (
            u'Статус должен быть UNCHECKED')

        # учитель ставит 5 баллов
        update_url = reverse('v2:course_lesson_result-detail',
                             args=(attempt_id,))
        answers[list(answers.keys())[0]][1]['custom_answer'].append(
            {
                'type': 'check',
                'message': u'Ответ с 5',
                'points': 5,
                'status': Answer.SUMMARY_INCORRECT
            },
        )
        update_data = {
            'answers': answers,
        }
        response = jclient.patch(update_url, update_data)
        answer = response.json()
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert len(answer['answers']) == 1, u'В данном занятии только 1 задача'
        current_answer = answer['answers'][list(answer['answers'].keys())[0]]
        assert len(current_answer) == 2, u'Должно быть 2 попытки'
        assert len(current_answer[1]['custom_answer']) == 2, (
            u'Должно быть 2 сообщения во второй посылке')
        assert current_answer[1]['status'] == Answer.SUMMARY_INCORRECT, (
            u'Статус должен быть SUMMARY_INCORRECT')

    def test_custom_answer_post_with_empty_attempt(self, jclient,
                                                   lesson_models,
                                                   user_custom_answer,
                                                   course_models_assignment,
                                                   student):
        """
        Проверяем, что изменение попытки с ручной проверкой произойдет
        успешно, если  присутствует пустая попытка в результатах
        """
        lesson, problem1, problem2, link1, link2 = lesson_models
        course, clesson = course_models_assignment
        answers = user_custom_answer
        answers[link2.id] = [
            {
                'markers': None,
                'custom_answer': [],
                'spent_time': 100,
            },
        ]
        create_data = {
            'answers': answers,
            'clesson': clesson.id,
            'spent_time': 100500,
            'completed': False,
        }
        create_url = reverse('v2:course_lesson_result-list')
        jclient.login(user=student)

        # создаем попытку
        response = jclient.post(create_url, create_data)
        answer = response.json()
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

        attempt_id = answer.pop('id')

        update_url = reverse('v2:course_lesson_result-detail',
                             args=(attempt_id,))
        answers[list(answers.keys())[0]][0]['custom_answer'].append(
            {
                'type': 'check',
                'message': u'Ответ',
                'points': 10,
                'status': Answer.SUMMARY_INCORRECT,
            },
        )
        update_data = {
            'answers': answers,
        }
        response = jclient.patch(update_url, update_data)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

    def test_update_spent_time(self, jclient, user_answers,
                               course_models, student):
        """
        Проверка ручки изменения проведённого в задаче времени.
        """
        course, clesson = course_models

        # проставляем конкретные параметры сценария (даже если они дефолтные)
        clesson.max_attempts_in_group = 2
        clesson.mode = LessonScenario.TRAINING_MODE
        clesson.save()

        answers, _ = user_answers
        existing_link_id = list(answers.keys())[0]

        create_data = {
            'answers': {},
            'clesson': clesson.id,
            'spent_time': 100,
            'completed': False,
        }
        expected = {
            'points': 0,
            'max_points': 2,
            'student_viewed_problems': {},
            'spent_time': 100,
            'clesson': clesson.id,
            'student': student.id,
            'work_out': False,
            'completed': False,
            'viewed': False,
        }
        create_url = reverse('v2:course_lesson_result-list')
        jclient.login(user=student)

        # создаем первую попытку
        response = jclient.post(create_url, create_data)
        answer = response.json()
        result_id = answer.pop('id')

        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert answer.pop('date_created'), (
            u'У попытки должно быть время создания')
        assert answer.pop('date_updated'), (
            u'У попытки должно быть время изменения')
        assert answer.pop('answers') is not None, (
            u'В попытке должны быть оцененные ответы')
        assert result_id, u'У попытки должен быть идентификатор'
        assert answer == expected, u'Неправильный ответ'

        result = CourseLessonResult.objects.get(id=result_id)
        invalid_link_id = str(int(existing_link_id) * 100 + 1)

        assert result.spent_time == 100, (
            u'Неправильное значение времени, проведённого в занятии')

        update_spent_time_url = reverse(
            'v2:course_lesson_result-update-spent-time',
            args=(result_id,),
        )

        # валидный запрос. Идентификатор связи имеет тип int
        data = {
            'time_delta': 5,
            'link_id': existing_link_id,
        }

        # делаем запрос c неправильным date_updated
        response = jclient.post(
            update_spent_time_url + '?date_updated=100500', data)

        assert response.status_code == 404, (
            u'Должен вернуть 404 так как date_updated не совпадает: {0}'
            .format(response.content)
        )

        response = jclient.post(
            update_spent_time_url + '?date_updated={0}'
            .format(dt_to_microseconds(result.date_updated)),
            data
        )

        assert response.status_code == 200, (
            u'Не удалось обновить время: {0}'.format(response.content))

        expected = {
            'points': 0,
            'max_points': 2,
            'student_viewed_problems': {},
            'spent_time': 105,
            'answers': {
                str(existing_link_id): [
                    {
                        'status': Answer.UNCHECKED,
                        'comment': '',
                        'custom_answer': None,
                        'completed': False,
                        'markers': None,
                        'theory': None,
                        'answered': False,
                        'points': None,
                        'spent_time': 5,
                    },
                ],
            },
            'clesson': clesson.id,
            'student': student.id,
            'work_out': False,
            'completed': False,
            'viewed': False,
        }

        answer = response.json()

        assert answer.pop('date_created'), (
            u'У попытки должно быть время создания')
        assert answer.pop('date_updated'), (
            u'У попытки должно быть время изменения')
        assert answer.pop('id'), u'У попытки должен быть идентификатор'
        assert answer == expected, u'Неправильный ответ'

        result.refresh_from_db()
        assert result.spent_time == 105, (
            u'Неправильное значение времени, проведённого в занятии')
        assert result.answers[str(existing_link_id)][-1]['spent_time'] == 5, (
            u'Неправильное значение времени, проведённого в задаче')

        # валидный запрос, должна быть сумма с предыдущим в той же попытке.
        # Идентификатор связи тут и далее имеет тип str
        existing_link_id = str(existing_link_id)
        data = {
            'time_delta': 3,
            'link_id': existing_link_id,
        }

        # делаем запрос без date_updated
        # для обратной совместимости такой запрос должен проходить
        response = jclient.post(update_spent_time_url, data)

        assert response.status_code == 200, (
            u'Не удалось обновить время: {0}'.format(response.content))

        expected = {
            'points': 0,
            'max_points': 2,
            'student_viewed_problems': {},
            'spent_time': 108,
            'answers': {
                str(existing_link_id): [
                    {
                        'status': Answer.UNCHECKED,
                        'comment': '',
                        'custom_answer': None,
                        'completed': False,
                        'markers': None,
                        'theory': None,
                        'answered': False,
                        'points': None,
                        'spent_time': 8,
                    },
                ],
            },
            'clesson': clesson.id,
            'student': student.id,
            'work_out': False,
            'completed': False,
            'viewed': False,
        }

        answer = response.json()

        assert answer.pop('date_created'), (
            u'У попытки должно быть время создания')
        assert answer.pop('date_updated'), (
            u'У попытки должно быть время изменения')
        assert answer.pop('id'), u'У попытки должен быть идентификатор'
        assert answer == expected, u'Неправильный ответ'

        result.refresh_from_db()
        assert result.spent_time == 108, (
            u'Неправильное значение времени, проведённого в занятии')
        assert result.answers[str(existing_link_id)][-1]['spent_time'] == 8, (
            u'Неправильное значение времени, проведённого в задаче')

        # Даем ответ по предыдущей попытке.
        # Таймер все еще пишет в старую попытку. Ждем поддержку с фронта

        result.answers[str(existing_link_id)][0]['markers'] = {
            '1': {
                'user_answer': {
                    '1': '44',
                },
                'max_mistakes': 1,
                'mistakes': 1,
                'answer_status': {
                    '1': False,
                }
            }
        }
        result.save()

        data = {
            'time_delta': 2,
            'link_id': existing_link_id,
        }

        response = jclient.post(
            update_spent_time_url + '?date_updated={0}'
            .format(dt_to_microseconds(result.date_updated)),
            data
        )

        assert response.status_code == 200, (
            u'Не удалось обновить время: {0}'.format(response.content))

        expected['spent_time'] = 110
        expected.pop('answers')
        expected_last_answer = {
            'status': Answer.UNCHECKED,
            'comment': '',
            'custom_answer': None,
            'completed': False,
            'answered': False,
            'points': None,
            'spent_time': 10,
            'theory': None,
        }

        answer = response.json()

        assert answer.pop('date_created'), (
            u'У попытки должно быть время создания')
        assert answer.pop('date_updated'), (
            u'У попытки должно быть время изменения')
        assert answer.pop('id'), u'У попытки должен быть идентификатор'
        answers = answer.pop('answers')
        answers[str(existing_link_id)][-1].pop('markers')
        assert (answers[str(existing_link_id)][-1] ==
                expected_last_answer), u'Неправильный ответ'
        assert answer == expected, u'Неправильный ответ'

        result.refresh_from_db()
        assert result.spent_time == 110, (
            u'Неправильное значение времени, проведённого в занятии')
        assert len(result.answers[str(existing_link_id)]) == 1, (
            u'Неправильное количество попыток в задаче')
        assert result.answers[str(existing_link_id)][-1]['spent_time'] == 10, (
            u'Неправильное значение времени, проведённого в задаче')

        # отрицательная дельта
        data = {
            'time_delta': -5,
            'link_id': existing_link_id,
        }

        response = jclient.post(
            update_spent_time_url + '?date_updated={0}'
            .format(dt_to_microseconds(result.date_updated)),
            data
        )

        assert response.status_code == 400, (
            u'Удалось обновить время некорректным запросом')

        result.refresh_from_db()
        assert result.spent_time == 110, (
            u'Неправильное значение времени, проведённого в занятии')
        assert result.answers[str(existing_link_id)][-1]['spent_time'] == 10, (
            u'Неправильное значение времени, проведённого в задаче')

        response = jclient.post(
            update_spent_time_url + '?date_updated={0}'
            .format(dt_to_microseconds(result.date_updated)),
            data
        )

        # несуществующая связь
        data = {
            'time_delta': 5,
            'link_id': invalid_link_id,
        }

        response = jclient.post(
            update_spent_time_url + '?date_updated={0}'
            .format(dt_to_microseconds(result.date_updated)),
            data
        )

        assert response.status_code == 400, (
            u'Удалось обновить время некорректным запросом')

        result.refresh_from_db()
        assert result.spent_time == 110, (
            u'Неправильное значение времени, проведённого в занятии')
        assert result.answers[str(existing_link_id)][-1]['spent_time'] == 10, (
            u'Неправильное значение времени, проведённого в задаче')

        # неполные данные
        data = {
            'link_id': existing_link_id,
        }

        response = jclient.post(
            update_spent_time_url + '?date_updated={0}'
            .format(dt_to_microseconds(result.date_updated)),
            data
        )

        assert response.status_code == 400, (
            u'Удалось обновить время некорректным запросом')

        result.refresh_from_db()
        assert result.spent_time == 110, (
            u'Неправильное значение времени, проведённого в занятии')
        assert result.answers[str(existing_link_id)][-1]['spent_time'] == 10, (
            u'Неправильное значение времени, проведённого в задаче')

        # неполные данные
        data = {
            'time_delta': 8,
        }

        response = jclient.post(
            update_spent_time_url + '?date_updated={0}'
            .format(dt_to_microseconds(result.date_updated)),
            data
        )

        assert response.status_code == 400, (
            u'Удалось обновить время некорректным запросом')

        result.refresh_from_db()
        assert result.spent_time == 110, (
            u'Неправильное значение времени, проведённого в занятии')
        assert result.answers[str(existing_link_id)][-1]['spent_time'] == 10, (
            u'Неправильное значение времени, проведённого в задаче')

    def test_create_with_testing(self, jclient, lesson_models,
                                 subject_model, user_answers, teacher):
        """
        Проверяет, что попытки учителя после выдачи должны затереться
        """
        lesson, problem1, problem2, link1, link2 = lesson_models
        course = Course.objects.create(
            name=u'Тестовый курс',
            subject=subject_model,
            owner=teacher,
        )
        # создаем занятие которое будем тестировать до выдачи
        clesson = CourseLessonLink.objects.create(
            course=course,
            lesson=lesson,
            order=1,
            accessible_to_teacher=timezone.now(),
            date_assignment=None,
        )
        answers, checked_answers = user_answers
        problem_links_ids = [link.id for link in
                             clesson.lesson.lessonproblemlink_set.all()]
        LessonAssignment.objects.create(
            clesson=clesson, student=teacher,
            problems=problem_links_ids[:1],
        )
        create_data = {
            'answers': answers,
            'clesson': clesson.id,
            'spent_time': 100500,
        }
        expected = {
            'answers': {
                str(problem_links_ids[0]): checked_answers.pop(
                    str(problem_links_ids[0])),
            },
            'points': 1,
            'max_points': 1,
            'student_viewed_problems': {},
            'spent_time': 100500,
            'clesson': clesson.id,
            'student': teacher.id,
            'work_out': False,
            'completed': True,
            'viewed': False,
        }
        create_url = reverse('v2:course_lesson_result-list')
        jclient.login(user=teacher)
    
        # создаем попытку
        response = jclient.post(create_url, create_data)
        answer = response.json()
        assert response.status_code == 201, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert answer.pop('date_created'), (
            u'У попытки должно быть время создания')
        assert answer.pop('date_updated'), (
            u'У попытки должно быть время изменения')
    
        attempt_id = answer.get('id', None)
    
        assert answer.pop('id'), u'У попытки должен быть идентификатор'
        assert answer == expected, u'Неправильный ответ'
    
        # выдаем задачу
        update_url = reverse('v2:course_lesson-detail', args=(clesson.id,))
        update_data = {
            'clesson': {
                'date_assignment': timezone.now().strftime(
                    settings.REST_FRAMEWORK['DATETIME_FORMAT']
                )
            }
        }
        response = jclient.patch(update_url, update_data)
        answer = response.json()
    
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
    
        assert answer['clesson'], u'Неправилный ответ'
        assert answer['clesson']['date_assignment'] is not None, (
            u'Дата выдачи не должна быть None')
    
        # проверяем что попытка исчезла
        detail_url = reverse(
            'v2:course_lesson_result-detail', args=(attempt_id,)
        )
    
        response = jclient.get(detail_url)
    
        assert response.status_code == 404, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
