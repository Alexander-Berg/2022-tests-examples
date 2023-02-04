from builtins import str
from datetime import timedelta

import pytest

from django.core.urlresolvers import reverse
from django.utils import timezone

from kelvin.common.utils_for_tests import extract_error_message
from kelvin.courses.models import Course, CourseLessonLink
from kelvin.lessons.models import Lesson, LessonProblemLink
from kelvin.problems.answers import Answer
from kelvin.problems.markers import Marker
from kelvin.problems.models import Problem
from kelvin.results.models import CourseLessonResult
from kelvin.subjects.models import Subject, Theme
from kelvin.common.error_responses import ErrorsComposer


@pytest.mark.django_db
def test_story(some_owner, teacher, student, jclient):
    """
    Отправка результатов прохождения ученика
    """
    subject = Subject.objects.create(
        slug='math',
        name=u'Высшая математика',
    )
    theme = Theme.objects.create(
        name=u'Обыкновенные дифференциальные уравнения',
        code=u'1112',
        subject=subject,
    )
    problem1 = Problem.objects.create(
        markup={
            'layout': [
                {
                    'content': {
                        'text': u'{marker:1}',
                        'options': {
                            'style': 'normal'
                        }
                    },
                    'kind': 'text'
                },
                {
                    'content': {
                        'type': 'select',
                        'id': 1,
                        'options': {}
                    },
                    # FIXME Uncomented data is depricated and correct variant
                    # is commented till someone recheck all test data.
                    # "content": {
                    #     "type": "choice",
                    #     "options": {
                    #         "type_display": "vertical",
                    #         "choices": [
                    #             "понедельник",
                    #             "вторник",
                    #             "среда",
                    #             "четверг",
                    #             "пятница",
                    #             "суббота",
                    #             "воскресенье"
                    #         ],
                    #         "flavor": "checkbox"
                    #     },
                    #     "id": 1
                    # },
                    'kind': 'marker'
                }
            ],
            'checks': {},
            'answers': {
                '1': [2]
            }
        },
        owner=some_owner,
        subject=subject,
    )
    problem2 = Problem.objects.create(
        markup={
            'layout': [
                {
                    'content': {
                        'text': u'{marker:1}',
                        'options': {
                            'style': 'normal'
                        }
                    },
                    'kind': 'text'
                },
                {
                    'content': {
                        'type': 'select',
                        'id': 1,
                        'options': {}
                    },
                    'kind': 'marker'
                },
                {
                    'content': {
                        'type': 'select',
                        'id': 2,
                        'options': {}
                    },
                    'kind': 'marker'
                }
            ],
            'checks': {},
            'answers': {
                '1': None,
                '2': None
            }
        },
        owner=some_owner,
        subject=subject,
    )
    lesson = Lesson.objects.create(
        owner=some_owner, theme=theme, name=u'Урок')
    link1 = LessonProblemLink.objects.create(
        lesson=lesson, problem=problem1, order=1,
        options={'max_attempts': 3, 'show_tips': True},
    )
    link2 = LessonProblemLink.objects.create(
        lesson=lesson, problem=problem2, order=2,
        options={'max_attempts': 3, 'show_tips': True},
    )
    course = Course.objects.create(
        name=u'Тестовый курс',
        subject=subject,
        owner=teacher,
    )
    clesson = CourseLessonLink.objects.create(
        course=course,
        lesson=lesson,
        order=1,
    )
    post_url = reverse('v2:course_lesson_result-list')

    # надо быть залогиненым, чтобы отправлять результаты
    empty_result = {
        'clesson': clesson.id,
        'completed': False,
        'student': student.id,
    }
    response = jclient.post(post_url, empty_result)
    assert response.status_code == 400, (
        u'Неправильный статус ответа, ответ: {0}'.format(response.content))

    assert response.json() == ErrorsComposer.compose_response_body(
        source='non_field_errors',
        message='authenticate to create result',
        code='course_not_for_anonymous',
    )

    # нельзя создать попытку учеником на неназначенное занятие
    jclient.login(user=student)
    response = jclient.post(post_url, empty_result)
    assert response.status_code == 400, (
        u'Неправильный статус ответа, ответ: {0}'.format(response.content))
    assert response.json() == ErrorsComposer.compose_response_body(
        source='non_field_errors',
        message='can not create result for future clesson',
        code='future_date_assignment',
    )

    # нельзя создать попытку учеником на будущее занятие
    clesson.date_assignment = timezone.now() + timedelta(days=1)
    clesson.save()
    jclient.login(user=student)
    response = jclient.post(post_url, empty_result)
    assert response.status_code == 400, (
        u'Неправильный статус ответа, ответ: {0}'.format(response.content))

    assert response.json() == ErrorsComposer.compose_response_body(
        source='non_field_errors',
        message='can not create result for future clesson',
        code='future_date_assignment',
    )

    # создаем пустую попытку на занятие
    clesson.date_assignment = timezone.now()
    clesson.save()
    response = jclient.post(post_url, empty_result)
    assert response.status_code == 201, (
        u'Неправильный статус ответа, ответ: {0}'.format(response.content))
    answer = response.json()
    result_id = answer.pop('id')
    assert answer.pop('date_created')
    assert answer.pop('date_updated')
    expected = {
        'answers': {},
        'clesson': clesson.id,
        'student': student.id,
        'student_viewed_problems': {},
        'max_points': 2,
        'points': 0,
        'spent_time': None,
        'work_out': False,
        'completed': False,
        'viewed': False
    }
    assert answer == expected

    # адрес для патч-запросов
    patch_url = reverse('v2:course_lesson_result-detail', args=(result_id,))

    # пишем, что ученик затратил уже какое-то время на занятие
    response = jclient.patch(patch_url, {'spent_time': 101})
    assert response.status_code == 200, (
        u'Неправильный статус ответа, ответ: {0}'.format(response.content))
    answer = response.json()
    assert answer.pop('date_created')
    assert answer.pop('date_updated')
    expected = {
        'id': result_id,
        'answers': {},
        'clesson': clesson.id,
        'student': student.id,
        'points': 0,
        'student_viewed_problems': {},
        'max_points': 2,
        'spent_time': 101,
        'work_out': False,
        'completed': False,
        'viewed': False,
    }
    assert answer == expected

    # записываем незавершенную попытку на первый вопрос
    patch_data = {
        'answers': {
            str(link1.id): {
                'completed': False,
                'spent_time': 20,
                'markers': {
                    '1': {
                        'mistakes': 1,
                        'max_mistakes': 1,
                    },
                    # '1': [1],
                    # FIXME Uncomented data is depricated and correct variant
                    # is commented till someone recheck all test data.
                },
                'answered': False,
            },
        },
    }
    response = jclient.patch(patch_url, patch_data)
    assert response.status_code == 200, (
        u'Неправильный статус ответа, ответ: {0}'.format(response.content))
    answer = response.json()
    assert answer.pop('date_created')
    assert answer.pop('date_updated')
    expected = {
        'id': result_id,
        'answers': {
            str(link1.id): [
                {
                    'completed': False,
                    'markers': {
                        '1': {
                            'status': Marker.INCORRECT,
                            'mistakes': 1,
                            'max_mistakes': 1,
                        },
                    },
                    'theory': None,
                    'custom_answer': None,
                    'spent_time': 20,
                    'status': Answer.INCORRECT,
                    'points': 0,
                    'comment': '',
                    'answered': False,
                },
            ],
        },
        'clesson': clesson.id,
        'student': student.id,
        'points': 0,
        'student_viewed_problems': {},
        'max_points': 2,
        'spent_time': 101,
        'work_out': False,
        'completed': False,
        'viewed': False,
    }
    assert answer == expected

    # завершаем попытку на вопрос, не посылая `completed`
    patch_data = {
        'answers': {
            str(link1.id): {
                'spent_time': 40,
                'markers': {
                    '1': {
                        'mistakes': 1,
                        'max_mistakes': 1,
                    },
                },
                'answered': False,
            },
        },
    }
    response = jclient.patch(patch_url, patch_data)
    assert response.status_code == 200, (
        u'Неправильный статус ответа, ответ: {0}'.format(response.content))
    answer = response.json()
    assert answer.pop('date_created')
    assert answer.pop('date_updated')
    expected = {
        'id': result_id,
        'answers': {
            str(link1.id): [
                {
                    'completed': True,
                    'markers': {
                        '1': {
                            'status': Marker.INCORRECT,
                            'mistakes': 1,
                            'max_mistakes': 1,
                        },
                    },
                    'custom_answer': None,
                    'spent_time': 40,
                    'status': Answer.INCORRECT,
                    'points': 0,
                    'comment': '',
                    'answered': False,
                    'theory': None,
                },
            ]
        },
        'clesson': clesson.id,
        'student': student.id,
        'points': 0,
        'student_viewed_problems': {},
        'max_points': 2,
        'spent_time': 101,
        'work_out': False,
        'completed': False,
        'viewed': False,
    }
    assert answer == expected

    # посылаем вторую попытку на вопрос
    patch_data = {
        'answers': {
            str(link1.id): {
                'spent_time': 30,
                'markers': {
                    '1': {
                        'mistakes': 0,
                        'max_mistakes': 1,
                    },
                },
                'answered': True,
            },
        },
    }
    response = jclient.patch(patch_url, patch_data)
    assert response.status_code == 200, (
        u'Неправильный статус ответа, ответ: {0}'.format(response.content))
    answer = response.json()
    assert answer.pop('date_created')
    assert answer.pop('date_updated')
    expected = {
        'id': result_id,
        'answers': {
            str(link1.id): [
                {
                    'completed': True,
                    'markers': {
                        '1': {
                            'status': Marker.INCORRECT,
                            'mistakes': 1,
                            'max_mistakes': 1,
                        },
                    },
                    'theory': None,
                    'custom_answer': None,
                    'spent_time': 40,
                    'status': Answer.INCORRECT,
                    'points': 0,
                    'comment': '',
                    'answered': False,
                },
                {
                    'completed': True,
                    'markers': {
                        '1': {
                            'status': Marker.CORRECT,
                            'mistakes': 0,
                            'max_mistakes': 1,
                        },
                    },
                    'theory': None,
                    'custom_answer': None,
                    'spent_time': 30,
                    'status': Answer.CORRECT,
                    'points': 1,
                    'comment': '',
                    'answered': True,
                },
            ]
        },
        'clesson': clesson.id,
        'student': student.id,
        'points': 1,
        'student_viewed_problems': {},
        'max_points': 2,
        'spent_time': 101,
        'work_out': False,
        'completed': False,
        'viewed': False,
    }
    assert answer == expected

    # посылаем ответ на второй вопрос
    patch_data = {
        'answers': {
            str(link2.id): {
                'spent_time': 50,
                'markers': {
                    '1': {
                        'mistakes': 0,
                        'max_mistakes': 1,
                    },
                },
                'answered': True,
            },
        },
    }
    response = jclient.patch(patch_url, patch_data)
    assert response.status_code == 200, (
        u'Неправильный статус ответа, ответ: {0}'.format(response.content))
    answer = response.json()
    assert answer.pop('date_created')
    assert answer.pop('date_updated')
    expected = {
        'id': result_id,
        'answers': {
            str(link1.id): [
                {
                    'completed': True,
                    'markers': {
                        '1': {
                            'status': Marker.INCORRECT,
                            'mistakes': 1,
                            'max_mistakes': 1,
                        },
                    },
                    'theory': None,
                    'custom_answer': None,
                    'spent_time': 40,
                    'status': Answer.INCORRECT,
                    'points': 0,
                    'comment': '',
                    'answered': False,
                },
                {
                    'completed': True,
                    'markers': {
                        '1': {
                            'status': Marker.CORRECT,
                            'mistakes': 0,
                            'max_mistakes': 1,
                        },
                    },
                    'theory': None,
                    'custom_answer': None,
                    'spent_time': 30,
                    'status': Answer.CORRECT,
                    'points': 1,
                    'comment': '',
                    'answered': True,
                },
            ],
            str(link2.id): [
                {
                    'completed': True,
                    'markers': {
                        '1': {
                            'status': Marker.CORRECT,
                            'mistakes': 0,
                            'max_mistakes': 1,
                        },
                        '2': {
                            'status': Marker.INCORRECT,
                            'answer_status': -1,
                            'mistakes': 1,
                            'max_mistakes': 1,
                        }
                    },
                    'theory': None,
                    'custom_answer': None,
                    'spent_time': 50,
                    'status': Answer.INCORRECT,
                    'points': 0,
                    'comment': '',
                    'answered': True,
                },
            ],
        },
        'clesson': clesson.id,
        'student': student.id,
        'points': 1,
        'student_viewed_problems': {},
        'max_points': 2,
        'spent_time': 101,
        'work_out': False,
        'completed': False,
        'viewed': False,
    }
    assert answer == expected

    # можем переписать попытки на первый вопрос целиком
    patch_data = {
        'answers': {
            str(link1.id): [
                {
                    'spent_time': 20,
                    'markers': {
                        '1': {
                            'mistakes': 1,
                            'max_mistakes': 1,
                        },
                    },
                    'answered': False,
                },
                {
                    'spent_time': 30,
                    'markers': {
                        '1': {
                            'mistakes': 1,
                            'max_mistakes': 1,
                        },
                    },
                    'answered': False,
                },
                {
                    'spent_time': 40,
                    'markers': {
                        '1': {
                            'mistakes': 1,
                            'max_mistakes': 1,
                        },
                    },
                    'answered': True,
                },
            ],
        },
        # проверяем, что эти поля игнорируются
        'points': -1,
        'max_points': -1,
    }
    response = jclient.patch(patch_url, patch_data)
    assert response.status_code == 200, (
        u'Неправильный статус ответа, ответ: {0}'.format(response.content))
    answer = response.json()
    assert answer.pop('date_created')
    assert answer.pop('date_updated')
    expected = {
        'id': result_id,
        'answers': {
            str(link1.id): [
                {
                    'completed': True,
                    'markers': {
                        '1': {
                            'status': Marker.INCORRECT,
                            'mistakes': 1,
                            'max_mistakes': 1,
                        },
                    },
                    'theory': None,
                    'custom_answer': None,
                    'spent_time': 20,
                    'status': Answer.INCORRECT,
                    'comment': '',
                    # EDU-274 если присылают несколько попыток, то баллы
                    # проставляем только в последней попытке
                    'points': None,
                    'answered': False,
                },
                {
                    'completed': True,
                    'markers': {
                        '1': {
                            'status': Marker.INCORRECT,
                            'mistakes': 1,
                            'max_mistakes': 1,
                        },
                    },
                    'theory': None,
                    'custom_answer': None,
                    'spent_time': 30,
                    'status': Answer.INCORRECT,
                    'points': None,
                    'comment': '',
                    'answered': False,
                },
                {
                    'completed': True,
                    'markers': {
                        '1': {
                            'status': Marker.INCORRECT,
                            'mistakes': 1,
                            'max_mistakes': 1,
                        },
                    },
                    'theory': None,
                    'custom_answer': None,
                    'spent_time': 40,
                    'status': Answer.INCORRECT,
                    'points': 0,
                    'comment': '',
                    'answered': True,
                },
            ],
            str(link2.id): [
                {
                    'completed': True,
                    'markers': {
                        '1': {
                            'status': Marker.CORRECT,
                            'mistakes': 0,
                            'max_mistakes': 1,
                        },
                        '2': {
                            'status': Marker.INCORRECT,
                            'answer_status': -1,
                            'mistakes': 1,
                            'max_mistakes': 1,
                        }
                    },
                    'theory': None,
                    'custom_answer': None,
                    'spent_time': 50,
                    'status': Answer.INCORRECT,
                    'points': 0,
                    'comment': '',
                    'answered': True,
                },
            ],
        },
        'clesson': clesson.id,
        'student': student.id,
        'points': 0,
        'student_viewed_problems': {},
        'max_points': 2,
        'spent_time': 101,
        'work_out': False,
        'completed': False,
        'viewed': False,
    }
    assert answer == expected

    # нельзя создать еще одну попытку на первый вопрос
    patch_data = {
        'answers': {
            str(link1.id): {
                'spent_time': 50,
                'markers': {
                    '1': {
                        'mistakes': 0,
                        'max_mistakes': 1,
                    },
                },
            },
        },
    }
    response = jclient.patch(patch_url, patch_data)
    assert response.status_code == 400, (
        u'Неправильный статус ответа, ответ: {0}'.format(response.content))
    assert (
        extract_error_message(response.json()) ==
        u'attempt limit exceeded in problems [\'{0}\']'.format(link1.id)
    )

    # завершаем результат
    patch_data = {
        'completed': True,
    }
    response = jclient.patch(patch_url, patch_data)
    assert response.status_code == 200, (
        u'Неправильный статус ответа, ответ: {0}'.format(response.content))
    answer = response.json()
    assert answer.pop('date_created')
    assert answer.pop('date_updated')
    # сравниваем с предыдущим `expected`, но уже завершенным
    expected['completed'] = True
    assert answer == expected
    assert CourseLessonResult.objects.get(id=result_id).completed

    # нельзя отправить результат в завершенную попытку
    patch_data = {
        'answers': {
            str(link2.id): {
                'spent_time': 50,
                'markers': {
                    '1': {
                        'mistakes': 0,
                        'max_mistakes': 1,
                    },
                    '2': {
                        'mistakes': 0,
                        'max_mistakes': 1,
                    },
                },
            },
        },
    }
    response = jclient.patch(patch_url, patch_data)
    assert response.status_code == 400, (
        u'Неправильный статус ответа, ответ: {0}'.format(response.content))
    assert (
        extract_error_message(response.json()) ==
        'can\'t modify completed result'
    )

    # нельзя создать "урочную" попытку после завершения занятия
    clesson.date_completed = timezone.now()
    clesson.save()
    response = jclient.post(post_url, empty_result)
    assert response.status_code == 400, (
        u'Неправильный статус ответа, ответ: {0}'.format(response.content))
    assert (
        extract_error_message(response.json()) == u'clesson is completed'
    )

    # но можно создавать "внеурочные" попытки
    empty_result['work_out'] = True
    response = jclient.post(post_url, empty_result)
    assert response.status_code == 201, (
        u'Неправильный статус ответа, ответ: {0}'.format(response.content))
    answer = response.json()
    assert answer.pop('id')
    assert answer.pop('date_created')
    assert answer.pop('date_updated')
    expected = {
        'answers': {},
        'clesson': clesson.id,
        'student': student.id,
        'points': 0,
        'student_viewed_problems': {},
        'max_points': 2,
        'spent_time': None,
        'work_out': True,
        'completed': False,
        'viewed': False,
    }
    assert answer == expected
