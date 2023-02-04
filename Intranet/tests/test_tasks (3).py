from datetime import timedelta

import pytest
from mock import call

from django.utils import timezone

from kelvin.courses.models import Course, CourseLessonLink
from kelvin.lessons.models import Lesson, LessonProblemLink
from kelvin.problems.models import Problem
from kelvin.result_stats.tasks import daily_recalculate_problemstat
from kelvin.results.models import CourseLessonResult, CourseLessonSummary, LessonResult, LessonSummary


@pytest.mark.django_db
def test_daily_recalculate_problemstat(mocker, teacher, subject_model,
                                       student, student2):
    """
    Тест правильности нахождения задач для пересчета статистики
    """
    two_days_before = timezone.now() - timedelta(days=2)
    problem1 = Problem.objects.create(
        owner=teacher,
        subject=subject_model,
        markup={},
    )
    problem2 = Problem.objects.create(
        owner=teacher,
        subject=subject_model,
        markup={},
    )
    problem3 = Problem.objects.create(
        owner=teacher,
        subject=subject_model,
        markup={},
    )
    problem4 = Problem.objects.create(
        owner=teacher,
        subject=subject_model,
        markup={},
    )

    # первое занятие состоит из problem1, problem2
    lesson1 = Lesson.objects.create(
        owner=teacher,
    )
    LessonProblemLink.objects.create(
        lesson=lesson1,
        problem=problem1,
        order=1,
    )
    LessonProblemLink.objects.create(
        lesson=lesson1,
        problem=problem2,
        order=2,
    )

    # второе занятие состоит из problem2, problem3
    lesson2 = Lesson.objects.create(
        owner=teacher,
    )
    LessonProblemLink.objects.create(
        lesson=lesson2,
        problem=problem2,
        order=1,
    )
    LessonProblemLink.objects.create(
        lesson=lesson2,
        problem=problem3,
        order=2,
    )

    # третье занятие состоит из problem3, problem4
    lesson3 = Lesson.objects.create(
        owner=teacher,
    )
    LessonProblemLink.objects.create(
        lesson=lesson3,
        problem=problem3,
        order=1,
    )
    LessonProblemLink.objects.create(
        lesson=lesson3,
        problem=problem4,
        order=2,
    )

    # курс состоит из занятий lesson1, lesson2
    course = Course.objects.create(
        owner=teacher,
        subject=subject_model,
    )
    clesson1 = CourseLessonLink.objects.create(
        course=course,
        lesson=lesson1,
    )
    clesson2 = CourseLessonLink.objects.create(
        course=course,
        lesson=lesson2,
    )

    # старый результат на курсозанятие clesson1
    summary1 = CourseLessonSummary.objects.create(
        clesson=clesson1,
        student=student,
    )
    result1 = CourseLessonResult.objects.create(
        summary=summary1,
        max_points=20,
    )
    CourseLessonResult.objects.filter(id=result1.id).update(
        date_updated=two_days_before,)

    # новый результат на курсозанятие clesson2
    summary2 = CourseLessonSummary.objects.create(
        clesson=clesson2,
        student=student2,
    )
    CourseLessonResult.objects.create(
        summary=summary2,
        max_points=20,
    )

    # новый результат на занятие вне курса lesson3
    summary3 = LessonSummary.objects.create(
        lesson=lesson3,
        student=student,
    )
    LessonResult.objects.create(
        summary=summary3,
        max_points=20,
    )

    mocked_task = mocker.patch(
        'kelvin.result_stats.tasks.recalculate_problemstat')
    daily_recalculate_problemstat()

    # порядок вызовов произвольный
    assert len(mocked_task.mock_calls) == 3
    for mock_call in mocked_task.mock_calls:
        assert mock_call in [
            call.delay(problem2.id),
            call.delay(problem3.id),
            call.delay(problem4.id),
        ]
