from builtins import str, object
import pytest
from django.contrib.auth import get_user_model
from django.core.urlresolvers import reverse

from integration_tests.kelvin.sapi.utils import (
    assert_sirius_pagination, assert_contains_value, obj_factory, get_response
)
from kelvin.result_stats.models import StudentCourseStat


User = get_user_model()


@pytest.mark.skip()
@pytest.mark.django_db
class TestSiriusStudentCourseStat(object):
    """
    Тест API выгрузки результатов пройденных курсов
    """

    def test_student_course_stat(self, jclient, course1, course2, sirius_users):
        """
        Тест выгрузки результатов

        Проверки:
        - ответ содержит данные пагинации
        - ответ содержит данные по определенному логину
        - ответ содержит корректное количество записей
        - ответ на несуществующий курс пуст
        """
        teacher_user, student_user = sirius_users

        stat_student1 = obj_factory(StudentCourseStat, dict(
            student=student_user,
            course=course1,
        ))
        stat_student2 = obj_factory(StudentCourseStat, dict(
            student=obj_factory(User, dict(
                username='some_random_user',
                email='abcdefg@mail.test'
            )),
            course=course1,
        ))
        stat_student3 = obj_factory(StudentCourseStat, dict(
            student=student_user,
            course=course2,
        ))

        staff_user = obj_factory(User, dict(
            username='staff_user',
            email='staff@mail.test',
            is_staff=True,
        ))

        jclient.login(user=staff_user)

        # существующий курс
        url = reverse('v2:sirius-student-course-stat-all-students',
                      args=(course1.pk, ))

        response, response_json = get_response(jclient, url)

        assert_sirius_pagination(response_json)

        assert_contains_value(response_json['results'],
                              student_user.username,
                              key='username')

        # несуществующий курс
        url = reverse('v2:sirius-student-course-stat-all-students',
                      args=(667,))

        response, response_json = get_response(jclient, url)

        assert_sirius_pagination(response_json)

        assert len(response_json['results']) == 0

    def test_test_student_course_stat_by_username(self, jclient, sirius_users,
                                                  course1, course2):
        """
        Тест выгрузки результатов пользователя по списку курсов

        Проверки:
        - результат содержит информацию о пагинации
        - результат содержит id курсов
        """
        teacher_user, student_user = sirius_users

        jclient.login(user=student_user)

        url = reverse('v2:sirius-student-course-stat-user-results',
                      args=(student_user.username,))

        stat_student1 = obj_factory(StudentCourseStat, dict(
            student=student_user,
            course=course1,
        ))
        stat_student2 = obj_factory(StudentCourseStat, dict(
            student=student_user,
            course=course2,
        ))

        course_ids = ','.join([str(course1.pk), str(course2.pk)])
        url += '?course_ids=' + course_ids

        response, response_json = get_response(jclient, url)

        assert_sirius_pagination(response_json)

        assert_contains_value(response_json['results'],
                              value=course1.pk,
                              key='course_id')
        assert_contains_value(response_json['results'],
                              value=course2.pk,
                              key='course_id')
