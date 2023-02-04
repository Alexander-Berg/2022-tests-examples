from builtins import str, range
import pytest
import random

from django.utils.timezone import now

from integration_tests.fixtures.courses import add_students_to_course

from kelvin.courses.journal import LessonJournal
from kelvin.courses.models import CourseLessonLink, CourseStudent, Course
from kelvin.lessons.models import Lesson
from kelvin.results.models import CourseLessonResult, CourseLessonSummary
from kelvin.result_stats.models import StudentCourseStat


@pytest.fixture
def statistic(course, lesson_models, students):
    """
    Фикстура, которая создает связку

                       Курс1
                      Занятие1
              задача1      задача2
    Ученик1     +1            +2
    Ученик2     --            -1
    Ученик3     -5            +5

    По каждой задаче максимум 5 попыток

    :param course           Просто пустой курс
    :param lesson_models    Содержит урок и связи с двумя задачами
    :param students         Содержит трех учеников курса
    """

    # Описание задач смотреть в фикстуре problem_models.
    lesson1, problem1, problem2, link1, link2 = lesson_models

    clesson = CourseLessonLink.objects.create(
        course=course,
        lesson=lesson1,
        date_assignment=now(),
    )

    add_students_to_course(course, students)

    # Создаем результат.
    # В таблице указан итоговый статус и количество попыток по задачам
    true_result = [
        [
            [LessonJournal.PROBLEM_CORRECT, 1],
            [LessonJournal.PROBLEM_CORRECT, 2],
        ],
        [
            [LessonJournal.PROBLEM_NO_INFO, 0],
            [LessonJournal.PROBLEM_INCORRECT, 1],
        ],
        [
            [LessonJournal.PROBLEM_INCORRECT_DONE, 5],
            [LessonJournal.PROBLEM_CORRECT, 5],
        ],
    ]

    CourseLessonResult.objects.create(
        summary=CourseLessonSummary.objects.create(
            clesson=clesson,
            student=students[0],
        ),
        completed=True,
        points=0,             # Это поле нас не интересует
        max_points=0,         # Это поле нас не интересует
        answers={
            str(link1.id): [
                {
                    'mistakes': 0,
                    'max_mistakes': 1,
                    'completed': True,
                    'spent_time': 20,
                    'points': 0,
                    'markers': {
                        '1': {
                            'mistakes': 0,
                            'max_mistakes': 1,
                        },
                    },
                },
            ],
            str(link2.id): [
                {
                    'mistakes': 1,
                    'max_mistakes': 1,
                    'completed': True,
                    'spent_time': 20,
                    'points': 0,
                    'markers': {
                        '1': {
                            'mistakes': 1,
                            'max_mistakes': 1,
                        },
                    },
                },
                {
                    'mistakes': 0,
                    'max_mistakes': 1,
                    'completed': True,
                    'spent_time': 20,
                    'points': 0,
                    'markers': {
                        '1': {
                            'mistakes': 0,
                            'max_mistakes': 1,
                        },
                    },
                },
            ],
        }
    )
    CourseLessonResult.objects.create(
        summary=CourseLessonSummary.objects.create(
            clesson=clesson,
            student=students[1],
        ),
        completed=True,
        points=0,             # Это поле нас не интересует
        max_points=0,         # Это поле нас не интересует
        answers={
            str(link2.id): [
                {
                    'mistakes': 1,
                    'max_mistakes': 1,
                    'completed': True,
                    'spent_time': 20,
                    'points': 0,
                    'markers': {
                        '1': {
                            'mistakes': 1,
                            'max_mistakes': 1,
                        },
                    },
                },
            ],
        }
    )
    CourseLessonResult.objects.create(
        summary=CourseLessonSummary.objects.create(
            clesson=clesson,
            student=students[2],
        ),
        completed=True,
        points=0,             # Это поле нас не интересует
        max_points=0,         # Это поле нас не интересует
        answers={
            str(link1.id): [
                {
                    'mistakes': 1,
                    'max_mistakes': 1,
                    'completed': True,
                    'spent_time': 20,
                    'points': 0,
                    'markers': {
                        '1': {
                            'mistakes': 1,
                            'max_mistakes': 1,
                        },
                    },
                } for _ in range(5)
            ],
            str(link2.id): [
                {
                    'mistakes': 1,
                    'max_mistakes': 1,
                    'completed': True,
                    'spent_time': 20,
                    'points': 0,
                    'markers': {
                        '1': {
                            'mistakes': 1,
                            'max_mistakes': 1,
                        },
                    },
                } for _ in range(4)
            ] + [{
                'mistakes': 0,
                'max_mistakes': 1,
                'completed': True,
                'spent_time': 20,
                'points': 0,
                'markers': {
                    '1': {
                        'mistakes': 0,
                        'max_mistakes': 1,
                    },
                },
            }],
        }
    )

    return course, clesson, students, true_result


@pytest.fixture
def course_models(some_owner, subject_model):
    """
    Модель связи курса и 3х занятий,
    у последнего занятия не установлена
    дата доступности учителю
    """
    course = Course.objects.create(
        name=u'new Course',
        subject=subject_model,
        owner=some_owner,
    )
    lesson1 = Lesson.objects.create(
        owner=some_owner,
        name=u'Lesson 1',
    )
    lesson2 = Lesson.objects.create(
        owner=some_owner,
        name=u'Lesson 2',
    )

    clesson1 = CourseLessonLink.objects.create(
        date_assignment=now(),
        course=course,
        lesson=lesson1,
    )
    clesson2 = CourseLessonLink.objects.create(
        date_assignment=now(),
        course=course,
        lesson=lesson2,
    )
    clesson3 = CourseLessonLink.objects.create(
        course=course,
        lesson=lesson2,
    )

    return course, [clesson1, clesson2, clesson3]


@pytest.fixture
def course_student_stats(course_models, students):
    """
    Фикстура, которая создает курс, 3 курсозанятия,
    3 ученика, назначенных на курс, и статистику
    каждого ученика по курсу с рандомной эффективностью
    по занятиям
    """
    stats = []
    course, clessons = course_models

    for student in students:
        CourseStudent.objects.create(
            course=course,
            student=student,
        )

        clesson_data = dict(
            (
                clesson.id,
                {
                    'efficiency': int(random.random() * 100),
                    'progress': int(random.random() * 100),
                    'problems_skipped': 1,
                    'max_points': 20,
                    'points': 5,
                    'problems_correct': 0,
                    'problems_incorrect': 1
                }
            )
            for clesson in clessons
        )

        stats.append(
            StudentCourseStat.objects.create(
                course=course,
                student=student,
                clesson_data=clesson_data,
            )
        )

    return course, clessons, students, stats
