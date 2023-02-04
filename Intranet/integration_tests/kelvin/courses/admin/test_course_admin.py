from builtins import object

import pytest

from django.contrib.auth import get_user_model
from django.core.urlresolvers import reverse

from kelvin.courses.models import Course, CourseLessonLink, CourseStudent
from kelvin.lesson_assignments.models import LessonAssignment
from kelvin.lessons.models import Lesson, LessonProblemLink
from kelvin.problems.models import Problem

User = get_user_model()


@pytest.mark.skip()
@pytest.mark.django_db
class TestCourseAdmin(object):
    """
    Тесты админки курсов
    """
    @pytest.mark.parametrize('assignments,result', (
        # в занятии задачи сгруппированы так:
        # первая группа: 0, 3, 6
        # вторая группа: 1, 4, 7
        # без группы: 2, 5
        (
            [],
            'OK',
        ),
        (
            [
                [2, 0, 7, 5],
            ],
            'OK',
        ),
        (
            [
                [2, 5],
            ],
            '1 updated',
        ),
        (
            [
                None,
                [0, 2, 7, 5],
            ],
            '1 updated',
        ),
        (
            [
                [0, 1, 2, 3],
                [],
            ],
            '2 updated'
        ),
        (
            [
                None,
                [0, 1, 2, 3],
                [],
                [0, 2, 7, 5],
                None,
            ],
            '4 updated'
        ),
    ))
    def test_ensure_assignments_view(self, jclient, assignments, result,
                                     course_with_lesson_variations):
        """
        Проверка кнопки пересчета назначений по курсозанятию
        """
        data = course_with_lesson_variations
        course = data['course']
        clesson = data['clessons'][0]
        problem_links = data['problem_links'][clesson.lesson.id]

        for i, assigned_problems in enumerate(assignments):
            student = User.objects.create(username=u'Ученик {}'.format(i + 1))
            CourseStudent.objects.create(student=student, course=course)
            if assigned_problems is None:
                # ученик без назначения
                continue
            LessonAssignment.objects.create(
                clesson=clesson,
                student=student,
                problems=[problem_links[i].id for i in assigned_problems],
            )

        ensure_assignments_url = reverse(
            'admin:courses_course_ensure_assignments', args=(clesson.id,))
        jclient.login(is_superuser=True)
        response = jclient.get(ensure_assignments_url)
        assert response.status_code == 200
        assert response.content.decode() == result

        assert len(assignments) == LessonAssignment.objects.all().count()
        for assignment in LessonAssignment.objects.all():
            assert len(assignment.problems) == 4
            problems_set = set(assignment.problems)
            assert problem_links[2].id in problems_set
            assert problem_links[5].id in problems_set
            assert (
                problem_links[0].id in problems_set or
                problem_links[3].id in problems_set or
                problem_links[6].id in problems_set
            )
            assert (
                problem_links[1].id in problems_set or
                problem_links[4].id in problems_set or
                problem_links[7].id in problems_set
            )

    def test_course_convert_view(self, course_with_lesson_variations,
                                 empty_lesson, jclient):
        """
        Проверяет метод конвертации курса в вариативное занятие
        """
        course = course_with_lesson_variations['course']
        clessons = course_with_lesson_variations['clessons']
        convert_url = reverse('admin:courses_course_course_convert',
                              args=(course.id,))
        jclient.login(is_superuser=True)
        response = jclient.post(
            convert_url, {'target_lesson_id': empty_lesson.id})
        assert response.status_code == 302
        assert response.get('Location') == (
            '/admin/lessons/lesson/{0}/change/'
            .format(empty_lesson.id)
        )
        assert LessonProblemLink.objects.filter(
            lesson=empty_lesson, group=clessons[0].lesson.id).count() == 8
        assert LessonProblemLink.objects.filter(
            lesson=empty_lesson, group=clessons[1].lesson.id).count() == 3
        assert LessonProblemLink.objects.filter(
            lesson=empty_lesson, group=clessons[2].lesson.id).count() == 3
