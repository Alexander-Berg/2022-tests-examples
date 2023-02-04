from builtins import object
import pytest
from django.contrib.auth import get_user_model
from django.core.urlresolvers import reverse

from integration_tests.kelvin.sapi.utils import (
    assert_sirius_pagination, get_response, obj_factory, assert_contains_value
)
from kelvin.courses.models import CourseLessonLink, Course, CourseStudent
from kelvin.lessons.models import LessonProblemLink
from kelvin.problems.models import Problem
from kelvin.results.models import CourseLessonResult, CourseLessonSummary
from kelvin.subjects.models import Subject

User = get_user_model()


@pytest.mark.skip()
@pytest.mark.django_db
class TestSiriusCLesson(object):
    """
    Тесты API курсозанятий
    """
    
    def test_export_results(self, jclient, sirius_student_user,
                            sirius_staff_user, sirius_lesson1,
                            course1):
        """
        Тест выгрузки результатов пользователей по курсозанятию

        Проверки:
        * Пользователь с ролью ученика не видит результаты
        * Пользователь с ролью админа видит результаты
        * Результат содержит информацию по необходимым пользователям
        """
        clesson1 = obj_factory(CourseLessonLink, dict(
            lesson=sirius_lesson1,
            mode=CourseLessonLink.CONTROL_WORK_MODE,
            duration=45,
            course=course1,
            order=1,
        ))

        student_user2 = obj_factory(User, dict(
            username='student_user2',
            email='student2@mail.test',
        ))

        # создаем результаты
        clesson1_results = obj_factory(CourseLessonResult, [
            dict(
                summary=obj_factory(CourseLessonSummary, dict(
                    clesson=clesson1,
                    student=sirius_student_user,
                )),
                points=100,
                max_points=2000,
            ),
            dict(
                summary=obj_factory(CourseLessonSummary, dict(
                    clesson=clesson1,
                    student=student_user2,
                )),
                points=100,
                max_points=2000,
            )
        ])

        # with student
        jclient.login(user=sirius_student_user)

        url = reverse('v2:sirius-clesson-export-results', args=(clesson1.pk, ))

        response, response_json = get_response(jclient, url)
        assert response.status_code == 403

        jclient.logout()

        # with staff now
        jclient.login(user=sirius_staff_user)
        response, response_json = get_response(jclient, url)

        assert_sirius_pagination(response_json)
        assert len(response_json['results']) == len(clesson1_results)

        assert_contains_value(response_json['results'],
                              value=sirius_student_user.username,
                              key='username')

        assert_contains_value(response_json['results'],
                              value=student_user2.username,
                              key='username')

    def test_controlwork(self, jclient, sirius_student_user,
                         sirius_teacher_user, sirius_lesson1, sirius_lesson2):
        """
        Тест API получения списка занятий типа "контрольная работа"

        Проверки:
        * результат содержит занятия с режимом контрольной работы
        * результат содержит идентификаторы требуемых заданий
        """
        course = obj_factory(Course, dict(
            name=u'Some title',
            description=u'Описание курса',
            code='AABBCCXX',
            owner=sirius_teacher_user,
            subject=obj_factory(Subject, dict(
                name=u'Subj title',
                slug='subj-title',
            ))
        ))

        clessons = obj_factory(CourseLessonLink, [
            dict(
                lesson=sirius_lesson1,
                course=course,
                order=1,
                mode=CourseLessonLink.CONTROL_WORK_MODE
            ),
            dict(
                lesson=sirius_lesson2,
                course=course,
                order=2,
                mode=CourseLessonLink.CONTROL_WORK_MODE
            )
        ])

        obj_factory(CourseStudent, dict(
            course=course,
            student=sirius_student_user
        ))

        url = reverse('v2:sirius-clesson-controlwork')

        jclient.login(user=sirius_student_user)

        response, response_json = get_response(jclient, url)

        assert response.status_code == 200

        assert response_json[0]['mode'] == CourseLessonLink.CONTROL_WORK_MODE
        assert response_json[1]['mode'] == CourseLessonLink.CONTROL_WORK_MODE

        assert_contains_value(response_json,
                              value=clessons[0].pk,
                              key='id')

        assert_contains_value(response_json,
                              value=clessons[1].pk,
                              key='id')

    def test_problems(self, jclient, sirius_student_user, sirius_teacher_user,
                      sirius_lesson1, course1, sirius_subjects):
        """
        Тест API получения списка задач курсозанятия

        Проверки:
        * код ответа
        * ответ содержит id привязанных задач
        """
        lesson_problem_links = obj_factory(LessonProblemLink, [
            dict(
                lesson=sirius_lesson1,
                problem=obj_factory(Problem, dict(
                    markup='',
                    owner=sirius_teacher_user,
                    subject=sirius_subjects[0],
                )),
                order=1,
            ),
            dict(
                lesson=sirius_lesson1,
                problem=obj_factory(Problem, dict(
                    markup='',
                    owner=sirius_teacher_user,
                    subject=sirius_subjects[0],
                )),
                order=2,
            ),
            dict(
                lesson=sirius_lesson1,
                problem=obj_factory(Problem, dict(
                    markup='',
                    owner=sirius_teacher_user,
                    subject=sirius_subjects[0],
                )),
                order=3,
            ),
            dict(
                lesson=sirius_lesson1,
                problem=obj_factory(Problem, dict(
                    markup='',
                    owner=sirius_teacher_user,
                    subject=sirius_subjects[0],
                )),
                order=4,
            ),
        ])

        obj_factory(CourseStudent, dict(
            course=course1,
            student=sirius_student_user,
        ))

        clesson = obj_factory(CourseLessonLink, dict(
            lesson=sirius_lesson1,
            course=course1,
            order=1,
            mode=CourseLessonLink.CONTROL_WORK_MODE
        ))

        url = reverse('v2:sirius-clesson-problems', args=(clesson.pk, ))

        jclient.login(user=sirius_student_user)

        response, response_json = get_response(jclient, url)

        assert response.status_code == 200

        for lpl in lesson_problem_links:
            assert_contains_value(response_json,
                                  value=lpl.pk,
                                  key='id')
