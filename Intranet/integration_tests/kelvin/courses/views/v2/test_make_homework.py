import re

import pytest

from django.core.urlresolvers import reverse
from django.contrib.auth import get_user_model
from django.utils import timezone

from kelvin.courses.models import Course, CourseLessonLink, CourseStudent
from kelvin.lesson_assignments.models import LessonAssignment
from kelvin.lessons.models import Lesson, LessonProblemLink
from kelvin.problems.models import Problem

PROBLEM_MARKUP = {
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
                'type': 'field',
                'id': 1,
                'options': {
                    'type_content': 'number'
                }
            },
            'kind': 'marker'
        }
    ],
    'checks': {},
    'answers': {
        '1': 4
    }
}

User = get_user_model()


@pytest.mark.django_db
def test_make_homework(jclient, teacher, some_owner, subject_model):
    """
    Тест создания домашнего задания
    """
    # создаем курс
    problem1 = Problem.objects.create(markup=PROBLEM_MARKUP, owner=some_owner,
                                      subject=subject_model)
    problem2 = Problem.objects.create(markup=PROBLEM_MARKUP, owner=some_owner,
                                      subject=subject_model)
    problem3 = Problem.objects.create(markup=PROBLEM_MARKUP, owner=some_owner,
                                      subject=subject_model)
    lesson = Lesson.objects.create(owner=some_owner, name=u'Занятие')
    link1 = LessonProblemLink.objects.create(
        lesson=lesson, problem=problem1, order=1,
        options={'max_attempts': 5, 'show_tips': True},
    )
    link2 = LessonProblemLink.objects.create(
        lesson=lesson, problem=problem2, order=2,
        options={'max_attempts': 5, 'show_tips': True},
    )
    link3 = LessonProblemLink.objects.create(
        lesson=lesson, problem=problem3, order=3,
        options={'max_attempts': 5, 'show_tips': True},
    )
    course = Course.objects.create(owner=teacher, name=u'Курс',
                                   subject=subject_model)
    now = timezone.now()
    clesson = CourseLessonLink.objects.create(
        course=course, lesson=lesson, order=1, accessible_to_teacher=now,
        date_assignment=now,
    )

    # добавляем ученикам курс
    student1 = User.objects.create(username=u'Ученик 1', email='student1@1.ru')
    student2 = User.objects.create(username=u'Ученик 2', email='student2@1.ru')
    student3 = User.objects.create(username=u'Ученик 3', email='student3@1.ru')
    CourseStudent.objects.create(course=course, student=student1)
    CourseStudent.objects.create(course=course, student=student2)
    CourseStudent.objects.create(course=course, student=student3)

    # записываем результаты
    post_result_url = reverse('v2:course_lesson_result-list')

    # первый ученик ответил правильно на второй вопрос
    jclient.login(user=student1)
    response = jclient.post(post_result_url, {
        'clesson': clesson.id,
        'answers': {
            link2.id: {
                'markers': {
                    '1': {'user_answer': 4},
                },
            },
        },
    })
    assert response.status_code == 201, (
        'Неправильный статус ответа, ответ: {0}'.format(response.content))

    # второй ученик ошибся в первом и ответил правильно на два последних
    jclient.login(user=student2)
    response = jclient.post(post_result_url, {
        'clesson': clesson.id,
        'answers': {
            link1.id: {
                'markers': {
                    '1': {'user_answer': 2},
                },
            },
            link2.id: {
                'markers': {
                    '1': {'user_answer': 4},
                },
            },
            link3.id: {
                'markers': {
                    '1': {'user_answer': 4},
                },
            },
        },
    })
    assert response.status_code == 201, (
        'Неправильный статус ответа, ответ: {0}'.format(response.content))

    # третий ученик решил все правильно
    jclient.login(user=student3)
    response = jclient.post(post_result_url, {
        'clesson': clesson.id,
        'answers': {
            link1.id: {
                'markers': {
                    '1': {'user_answer': 4},
                },
            },
            link2.id: {
                'markers': {
                    '1': {'user_answer': 4},
                },
            },
            link3.id: {
                'markers': {
                    '1': {'user_answer': 4},
                },
            },
        },
    })
    assert response.status_code == 201, (
        'Неправильный статус ответа, ответ: {0}'.format(response.content))

    # создаем домашнее задание
    make_homework_url = reverse('v2:course_lesson-make-homework',
                                args=(clesson.id,))
    jclient.login(user=teacher)

    response = jclient.post(make_homework_url)
    assert response.status_code == 201, (
        'Неправильный статус ответа, ответ: {0}'.format(response.content))
    answer = response.json()
    assert 'id' in answer, ' Должен быть идентификатор нового занятия'
    homework = CourseLessonLink.objects.get(id=answer['id'])
    assert homework.course_id == course.id
    assert homework.order == 1
    assert homework.accessible_to_teacher < timezone.now()
    assert homework.date_assignment is None
    assert homework.lesson.id != lesson.id
    assert homework.lesson.owner.id == some_owner.id
    assert re.match(r'Домашняя работа от \d\d\.\d\d\.\d\d\d\d', homework.lesson.name)
    homework_problems = list(homework.lesson.lessonproblemlink_set.values_list(
        'problem_id', flat=True))
    assert homework_problems == [problem1.id, problem3.id]

    # проверяем назначения
    assert LessonAssignment.objects.filter(clesson=homework).count() == 2

    # первому ученику должно быть назначены все задачи
    assert LessonAssignment.objects.filter(
        student=student1, clesson=homework).count() == 0

    # второму ученику - только первая задача, причем по ссылке из д.з.
    student2_assignment = LessonAssignment.objects.get(
        student=student2, clesson=homework).problems
    first_new_problem_link_id = LessonProblemLink.objects.filter(
        lesson_id=homework.lesson.id)[0].id
    assert student2_assignment == [first_new_problem_link_id], (
        'Назначение ссылается на неправильную задачу')

    # третьему ученику ничего не назначено
    assert LessonAssignment.objects.get(
        student=student3, clesson=homework).problems == []
