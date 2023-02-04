from builtins import str, object
import pytest

from django.core.urlresolvers import reverse
from django.utils import timezone

from kelvin.courses.models import Course, CourseLessonLink
from kelvin.lessons.models import Lesson, LessonProblemLink
from kelvin.results.models import (
    CourseLessonResult, LessonResult, LessonSummary,
)

from ..results.views.v2.test_course_lesson_result_view_set import user_answers


@pytest.mark.skip()
@pytest.mark.django_db
class TestProblemAdmin(object):
    """
    Тест админки задач
    """
    def test_recalculate_view(self, lesson_models, some_owner, theme_model,
                              subject_model, user_answers, student, student2,
                              jclient):
        """
        Тест пересчета результатов по задаче
        """
        # состав курса и занятий:
        # course:
        #   - clesson, lesson:
        #       - link1, problem1
        #       - link2, problem2
        #   - clesson2, lesson2
        #       - link3, problem2
        #   - clesson3, lesson3
        #       - link4, problem1
        #
        # изменяемая задача problem2
        lesson, problem1, problem2, link1, link2 = lesson_models
        lesson2 = Lesson.objects.create(
            owner=some_owner,
            theme=theme_model,
            name=u'Урок 2',
        )
        link3 = LessonProblemLink.objects.create(
            lesson=lesson2, problem=problem2, order=1,
        )
        lesson3 = Lesson.objects.create(
            owner=some_owner,
            theme=theme_model,
            name=u'Урок 3',
        )
        link4 = LessonProblemLink.objects.create(
            lesson=lesson3, problem=problem1, order=1,
            options={'show_tips': True, 'max_attempts': 5},
        )
        course = Course.objects.create(
            name='Some course',
            owner=some_owner,
            subject=subject_model,
        )
        now = timezone.now()
        clesson = CourseLessonLink.objects.create(
            course=course,
            lesson=lesson,
            order=1,
            accessible_to_teacher=now,
            date_assignment=now,
        )
        clesson2 = CourseLessonLink.objects.create(
            course=course,
            lesson=lesson2,
            order=3,
            accessible_to_teacher=now,
            date_assignment=now,
        )
        clesson3 = CourseLessonLink.objects.create(
            course=course,
            lesson=lesson3,
            order=2,
            accessible_to_teacher=now,
            date_assignment=now,
        )

        # проверяем, что без попыток всё работает
        recalculate_url = reverse('admin:problems_problem_recalculate',
                                  args=(problem2.id,))
        jclient.login(is_superuser=True)
        response = jclient.get(recalculate_url)
        assert response.status_code == 302, u'Должны редиректить'

        assert response.get('location') == (
            '/admin/problems/problem/{0}/change/'
            .format(problem2.id)
        ), u'Неправильный редирект'

        assert link3.options['max_attempts'] == 1000, (
            u'По умолчанию 1000 попыток'
        )

        # создаем попытки
        # 2 попытки на курсозанятие с задачей для изменения, 1 - без задачи,
        # result1 - clesson, student
        # result2 - clesson3, student
        # result3 - clesson2, student2
        # по одной попытке на занятие с задачей и без
        # result4 - lesson, student
        # result5 - lesson3, student
        answers, checked_answers = user_answers
        create_url = reverse('v2:course_lesson_result-list')
        jclient.login(user=student)
        create_data = {
            'answers': answers,
            'clesson': clesson.id,
        }
        assert jclient.post(create_url, create_data).status_code == 201
        create_data = {
            'answers': {
                link4.id: answers[link1.id],
            },
            'clesson': clesson3.id,
        }
        assert jclient.post(create_url, create_data).status_code == 201
        jclient.login(user=student2)
        create_data = {
            'answers': {
                link3.id: answers[link2.id],
            },
            'clesson': clesson2.id,
        }
        assert jclient.post(create_url, create_data).status_code == 201

        result1 = CourseLessonResult.objects.get(
            summary__student=student,
            summary__clesson=clesson,
        )
        result2 = CourseLessonResult.objects.get(
            summary__student=student2,
            summary__clesson=clesson2,
        )
        result3 = CourseLessonResult.objects.get(
            summary__student=student,
            summary__clesson=clesson3,
        )

        result4 = LessonResult.objects.create(
            answers=result1.answers,
            points=0,
            max_points=1000,
            summary=LessonSummary.objects.create(
                student=student,
                lesson=lesson,
            ),
        )
        result5 = LessonResult.objects.create(
            answers=result3.answers,
            points=0,
            max_points=2000,
            summary=LessonSummary.objects.create(
                student=student,
                lesson=lesson3,
            ),
        )

        # проверяем некоторые поля результатов и меняем разметку задачи
        assert result1.points == 1
        assert result1.answers[str(link1.id)][-1]['mistakes'] == 0
        assert result1.answers[str(link2.id)][-1]['mistakes'] == 2
        assert result2.points == 0
        assert result2.answers[str(link3.id)][-1]['mistakes'] == 2
        assert result3.points == 1
        assert result3.answers[str(link4.id)][-1]['mistakes'] == 0
        problem2.markup['answers']['1'] = [0, 1]
        problem2.save()

        # пересчитываем результаты
        jclient.login(is_superuser=True)
        response = jclient.get(recalculate_url)
        assert response.status_code == 302, u'Должны редиректить'
        assert (
            response.get('location') ==
            '/admin/problems/problem/{0}/change/'.format(problem2.id)
        )

        # проверить, что результат пересчитался
        result1.refresh_from_db()
        result2.refresh_from_db()
        result3.refresh_from_db()
        result4.refresh_from_db()
        result5.refresh_from_db()
        assert result1.points == 2
        assert result1.max_points == 2
        assert result1.answers[str(link1.id)][-1]['mistakes'] == 0
        assert result1.answers[str(link2.id)][-1]['mistakes'] == 0
        assert result2.points == 1
        assert result2.max_points == 1
        assert result2.answers[str(link3.id)][-1]['mistakes'] == 0
        assert result3.points == 1
        assert result3.max_points == 1
        assert result3.answers[str(link4.id)][-1]['mistakes'] == 0
        assert result4.points == 2
        assert result4.max_points == 2
        assert result4.answers[str(link1.id)][-1]['mistakes'] == 0
        assert result4.answers[str(link2.id)][-1]['mistakes'] == 0
        assert result5.points == 0
        assert result5.max_points == 2000
        assert result5.answers[str(link4.id)][-1]['mistakes'] == 0
