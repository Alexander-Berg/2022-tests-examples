from builtins import str, object
import pytest

from django.core.urlresolvers import reverse
from django.contrib.auth import get_user_model

from kelvin.courses.models import Course, CourseLessonLink, CourseStudent
from kelvin.lesson_assignments.models import LessonAssignment
from kelvin.lessons.models import Lesson, LessonProblemLink
from kelvin.problems.models import Problem
from kelvin.results.models import CourseLessonResult, CourseLessonSummary
from kelvin.subjects.models import Theme

User = get_user_model()


@pytest.fixture
def assignment_models(subject_model):
    """Модели для тестов назначения"""
    theme = Theme.objects.create(id=1, name='theme', code='thm',
                                 subject=subject_model)
    teacher = User.objects.create(email='1@1.w', is_teacher=True, username='1')
    student1 = User.objects.create(email='2@1.w', is_teacher=False,
                                   username='student1')
    student2 = User.objects.create(email='3@1.w', is_teacher=False,
                                   username='student2')
    course = Course.objects.create(name=u'Новый спец курс',
                                   subject=subject_model, owner=teacher, id=1)
    CourseStudent.objects.create(course=course, student=student1)
    CourseStudent.objects.create(course=course, student=student2)
    lesson1 = Lesson.objects.create(id=1, owner=teacher, theme=theme,
                                    name=u'Занятие 1')
    lesson2 = Lesson.objects.create(id=2, owner=teacher, theme=theme,
                                    name=u'Занятие 2')
    problem1 = Problem.objects.create(markup={}, owner=teacher,
                                      subject=subject_model)
    problem2 = Problem.objects.create(markup={}, owner=teacher,
                                      subject=subject_model)
    problem_link1 = LessonProblemLink.objects.create(
        lesson=lesson1, problem=problem1, order=1)
    problem_link2 = LessonProblemLink.objects.create(
        lesson=lesson1, problem=problem2, order=2)
    problem_link3 = LessonProblemLink.objects.create(
        lesson=lesson1, problem=None, order=3)
    link1 = CourseLessonLink.objects.create(
        course=course, lesson=lesson1, order=1)
    link2 = CourseLessonLink.objects.create(
        course=course, lesson=lesson2, order=2)
    course.refresh_from_db()
    return (teacher, student1, student2, link1, link2,
            problem_link1, problem_link2, problem_link3)


@pytest.mark.django_db
class TestLessonAssignmentViewSet(object):
    """
    Тесты методов работы с назначениями уроков
    """
    def test_list_route_get(self, jclient, assignment_models):
        """
        Тест получения матрицы назначения
        """
        get_url = reverse('v2:lesson_assignment-get')
        (teacher, student1, student2, link1, link2,
         problem_link1, problem_link2, problem_link3) = assignment_models
        LessonAssignment.objects.create(
            clesson=link1, student=student1, problems=[problem_link2.id,
                                                       problem_link3.id],
        )
        LessonAssignment.objects.create(
            clesson=link2, student=student2, problems=[problem_link2.id],
        )

        expected = {
            str(student1.id): [problem_link2.id, problem_link3.id],
            str(student2.id): [problem_link1.id, problem_link2.id,
                               problem_link3.id],
        }
        jclient.login(user=teacher)
        response = jclient.get(get_url, {'clesson': link1.id})
        assert response.json() == expected, u'Неправильный ответ'

    def test_list_route_save(self, jclient, assignment_models):
        """
        Тест сохранения матрицы назначения
        """
        save_url = reverse('v2:lesson_assignment-save')
        (teacher, student1, student2, link1, link2,
         problem_link1, problem_link2, problem_link3) = assignment_models
        LessonAssignment.objects.create(
            clesson=link2, student=student2, problems=[problem_link2.id],
        )
        old_course_date_updated = link1.course.date_updated
        old_clesson_date_updated = link1.date_updated
        jclient.login(user=teacher)

        # Создание назначения второму ученику для первого курсозанятия и
        # удаление первому, т.к. ему назначено все
        save_data = {
            str(student1.id): [problem_link1.id, problem_link2.id,
                               problem_link3.id],
            str(student2.id): [problem_link2.id],
        }
        response = jclient.post(
            save_url + '?clesson={0}'.format(link1.id),
            save_data,
        )
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert LessonAssignment.objects.all().count() == 2, (
            u'Должен создаться один объект')
        assert response.json() == {'reload': False}, (
            u'Для редактируемого урока не нужно уведомление о перезагрузке')
        assignment = LessonAssignment.objects.filter(clesson=link1.id)[0]
        assert assignment.clesson_id == link1.id
        assert assignment.student_id == student2.id
        assert assignment.problems == [problem_link2.id]
        link1.refresh_from_db()
        link1.course.refresh_from_db()
        assert link1.date_updated > old_clesson_date_updated, (
            u'Должна обновиться версия занятия')
        assert link1.course.date_updated > old_course_date_updated, (
            u'Должна обновиться версия курса')

        # Обоим назначается все, поэтому назначения удаляются
        save_data = {
            str(student1.id): [problem_link1.id, problem_link2.id,
                               problem_link3.id],
            str(student2.id): [problem_link1.id, problem_link2.id,
                               problem_link3.id],
        }
        response = jclient.post(
            save_url + '?clesson={0}'.format(link1.id),
            save_data,
        )
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.json() == {'reload': False}, (
            u'Для редактируемого урока не нужно уведомление о перезагрузке')
        assert LessonAssignment.objects.all().count() == 1, (
            u'Назначения clesson1 должны удалиться, т.к. назначено все')

        # Назначения двум ученикам для занятия, которое нельзя редактировать
        link1.lesson_editable = False
        link1.save()
        save_data = {
            str(student1.id): [problem_link1.id, problem_link2.id],
            str(student2.id): [problem_link2.id, problem_link3.id],
        }
        response = jclient.post(
            save_url + '?clesson={0}'.format(link1.id),
            save_data,
        )
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.json() == {'reload': True}, (
            u'При создании копии урока нужно уведомить о ее перезагрузке')
        assert LessonAssignment.objects.filter(clesson=link1).count() == 2, (
            u'Должны быть созданы два назначения')

        # Курсозанятие должно стать редактируемым и ссылаться на новое занятие
        old_lesson_id = link1.lesson_id
        link1.refresh_from_db()
        assert old_lesson_id != link1.lesson_id, (
            u'Должна быть создана копия занятия')
        assert link1.lesson_editable == True, (
            u'Курсозанятие должно стать редактируемым')

        # Проверяем, что назначения содержат новые ссылки на задачи, а по
        # ссылкам находятся нужные задачи
        student1_assignment = LessonAssignment.get_student_problems(student1,
                                                                    link1)
        student2_assignment = LessonAssignment.get_student_problems(student2,
                                                                    link1)
        assert student1_assignment != [problem_link1.id, problem_link2.id], (
            u'Назначение первого ученика должно содержать новые ссылки на '
            u'задачи')
        assert student2_assignment != [problem_link2.id, problem_link3.id], (
            u'Назначение второго ученика должно содержать новые ссылки на '
            u'задачи')

        student1_problems = [
            problem_link.problem_id for problem_link in
            LessonProblemLink.objects.filter(id__in=student1_assignment)
        ]
        student2_problems = [
            problem_link.problem_id for problem_link in
            LessonProblemLink.objects.filter(id__in=student2_assignment)
        ]
        assert (student1_problems ==
                [problem_link1.problem_id, problem_link2.problem_id]), (
            u'Назначение первого ученика ссылается на неправильные задачи')
        assert (student2_problems ==
                [problem_link2.problem_id, problem_link3.problem_id]), (
            u'Назначение второго ученика ссылается на неправильные задачи')

    def test_empty_lesson_assignments(self, jclient, assignment_models):
        """
        Тест получения назначений без задач в занятии и без назначений
        """
        get_url = reverse('v2:lesson_assignment-get')
        (teacher, student1, student2, link1, link2,
         problem_link1, problem_link2, problem_link3) = assignment_models
        expected = {
            str(student1.id): [],
            str(student2.id): [],
        }
        jclient.login(user=teacher)
        response = jclient.get(get_url, {'clesson': link2.id})
        assert response.json() == expected, u'Неправильный ответ'

    def test_list_route_save_with_results(self, jclient, assignment_models):
        """
        Тест обновления максимума баллов при обновлении назначения
        """
        save_url = reverse('v2:lesson_assignment-save')
        (teacher, student1, student2, link1, link2,
         problem_link1, problem_link2, problem_link3) = assignment_models
        LessonAssignment.objects.create(
            clesson=link2, student=student2, problems=[problem_link2.id],
        )
        save_data = {
            str(student1.id): [problem_link1.id, problem_link2.id,
                               problem_link3.id],
            str(student2.id): [problem_link2.id],
        }
        summary1 = CourseLessonSummary.objects.create(
            clesson=link1,
            student=student1,
        )
        summary2 = CourseLessonSummary.objects.create(
            clesson=link1,
            student=student2,
        )
        result1 = CourseLessonResult.objects.create(
            summary=summary1,
            max_points=30,
        )
        result2 = CourseLessonResult.objects.create(
            summary=summary2,
            max_points=20,
        )
        jclient.login(user=teacher)
        response = jclient.post(
            save_url + '?clesson={0}'.format(link1.id),
            save_data,
        )
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert LessonAssignment.objects.all().count() == 2, (
            u'Должен создаться один объект')
        result1.refresh_from_db()
        result2.refresh_from_db()
        assert result1.max_points == 2, (
            u'Максимум баллов первого ученика должен пересчитаться')
        assert result2.max_points == 1, (
            u'Максимум баллов второго ученика должен остаться прежним')
