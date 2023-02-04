from builtins import str, range
import pytest
from copy import deepcopy

from django.core.urlresolvers import reverse

from kelvin.courses.models import CourseLessonLink
from kelvin.result_stats.models import StudentCourseStat
from integration_tests.fixtures.courses import make_course_available_for_student


@pytest.mark.django_db
def test_course_journal_calculation(jclient, student_in_course_with_lesson):
    """
    Проверяет наличие подсчета количества (не)правильных задач в статистике
    учеников по курсу (журнала курса):
    1) ученик курса посылает результат на занятие
    2) учитель запрашивает журнал курса, там должны быть правильные данные

    В проверенной контрольной работе не бывает пропущенных задач, есть только
    правильно решенные и все остальные - неправильно решенные.
    """
    course = student_in_course_with_lesson['course']
    clesson = student_in_course_with_lesson['clesson']
    problem_links = student_in_course_with_lesson['problem_links']
    student = student_in_course_with_lesson['student']
    teacher = course.owner
    is_control_work = clesson.mode == CourseLessonLink.CONTROL_WORK_MODE
    create_result_url = reverse('v2:course_lesson_result-list')

    # неправильный ответ и только одна попытка на первый вопрос,
    # такой ответ считается как неправильный
    answers = {
        str(problem_links[0].id): [
            {
                'markers': {
                    '1': {
                        'user_answer': {
                            '1': '3',
                        },
                    },
                },
                'answered': True,
            },
        ]
    }
    create_data = {
        'answers': answers,
        'clesson': clesson.id,
        'spent_time': 100500,
        'completed': False,
    }
    jclient.login(user=student)
    response = jclient.post(create_result_url, create_data)
    assert response.status_code == 201
    answer = response.json()
    result_id = answer['id']
    assert (answer['answers'][str(problem_links[0].id)][0]['markers']['1']
            ['mistakes']) == 1
    assert StudentCourseStat.objects.filter(
        student=student, course=course).exists() is True

    # проверяем журнал курса
    jclient.login(user=teacher)
    journal_url = reverse('v2:course-journal', args=(course.id,))
    make_course_available_for_student(course, teacher)
    response = jclient.get(journal_url)
    assert response.status_code == 200
    assert response.json()['data'] == {
        'students': {
            str(student.id): {
                'lessons': {
                    str(clesson.id): {'max_points': 2, 'points': 0, 'progress': 50},
                },
                'points': 0,
                'problems_correct': 0,
                'problems_incorrect': 2 if is_control_work else 1,
                'problems_skipped': 0 if is_control_work else 1,
                'total_efficiency': 0,
                'student_id': student.id,
                'staff_group': {'link': 'https://staff.yandex-team.ru/departments/?q=', 'name': ''},
            },
        },
    }

    # выполняем максимум неправильных попыток на первую задачу
    wrong_answer = answers[str(problem_links[0].id)][0]
    answers = {
        str(problem_links[0].id): [deepcopy(wrong_answer) for __ in range(
            problem_links[0].options['max_attempts'])]
    }
    update_data = {
        'answers': answers,
    }
    update_result_url = reverse('v2:course_lesson_result-detail',
                                args=(result_id,))
    jclient.login(user=student)
    response = jclient.patch(update_result_url, update_data)
    assert response.status_code == 200

    # проверяем журнал курса
    jclient.login(user=teacher)
    journal_url = reverse('v2:course-journal', args=(course.id,))
    response = jclient.get(journal_url)
    assert response.status_code == 200
    assert response.json()['data'] == {
        'students': {
            str(student.id): {
                'lessons': {
                    str(clesson.id): {'max_points': 2, 'points': 0, 'progress': 50},
                },
                'points': 0,
                'problems_correct': 0,
                'problems_incorrect': 2 if is_control_work else 1,
                'problems_skipped': 0 if is_control_work else 1,
                'total_efficiency': 0,
                'student_id': student.id,
                'staff_group': {'link': 'https://staff.yandex-team.ru/departments/?q=', 'name': ''},
            },
        },
    }

    # выполняем правильно вторую задачу
    answers = {
        str(problem_links[1].id): [
            {
                'markers': {
                    '1': {
                        'user_answer': [1, 2],
                    },
                },
                'answered': True,
            },
        ],
    }
    update_data = {
        'answers': answers,
    }
    jclient.login(user=student)
    response = jclient.patch(update_result_url, update_data)
    assert response.status_code == 200

    # проверяем журнал курса
    jclient.login(user=teacher)
    journal_url = reverse('v2:course-journal', args=(course.id,))
    response = jclient.get(journal_url)
    assert response.status_code == 200
    assert response.json()['data'] == {
        'students': {
            str(student.id): {
                'lessons': {
                    str(clesson.id): {'max_points': 2, 'points': 1, 'progress': 100},
                },
                'points': 1,
                'problems_correct': 1,
                'problems_incorrect': 1,
                'problems_skipped': 0,
                'total_efficiency': 50,
                'student_id': student.id,
                'staff_group': {'link': 'https://staff.yandex-team.ru/departments/?q=', 'name': ''},
            },
        },
    }
