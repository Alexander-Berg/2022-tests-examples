from builtins import object

import pytest
from django.contrib.auth import get_user_model
from django.core.urlresolvers import reverse
from integration_tests.kelvin.sapi.utils import (
    get_response, obj_factory,
    assert_contains_value,
    assert_not_contains_value,
)
from kelvin.courses.models import Course, CourseStudent, CourseLessonLink, AssignmentRule
from kelvin.subjects.models import Subject
from integration_tests.fixtures.courses import make_course_available_for_student


User = get_user_model()


@pytest.mark.skip()
@pytest.mark.django_db
class TestSiriusCourses(object):
    """
    Тест API курсов
    """

    @staticmethod
    def sequence_contains_id_key(sequence, id_value):
        """
        Ассерт на то что список словарей содержит значение в полях id
        """
        assert any(item.get('id') == id_value for item in sequence)

    @staticmethod
    def sequence_not_contains_id_key(sequence, id_value):
        """
        Ассерт на то что список словарей не содержит значение в полях id
        """
        assert not any(item.get('id') == id_value for item in sequence)

    def test_library(self, jclient, sirius_users, sirius_subjects,
                     course1, course2, course3_not_free, course_student1):
        """
        Тест библиотеки курсов.

        * ученику назначен 1 курс в фикстурах
        * в фикстурах добавлены 2 публичных предмета и один непубличный

        Проверки:
        - возвращается статус 200
        - ответ содержит секцию курсов не включая уже назначенные
        - ответ содержит секцию предметов не включая непубличные
        """
        teacher_user, student_user = sirius_users

        make_course_available_for_student(course1, student_user)
        make_course_available_for_student(course2, student_user)

        jclient.login(user=student_user)

        url = reverse('v2:sirius-courses-library')
        response = jclient.get(url)
        response_json = response.json()

        assert response.status_code == 200

        assert 'courses' in response_json
        courses_data = response_json['courses']
        assert len(courses_data) > 0

        self.sequence_not_contains_id_key(courses_data, course_student1.course.pk)
        self.sequence_contains_id_key(courses_data, course2.pk)
        self.sequence_not_contains_id_key(courses_data, course3_not_free.pk)

        assert 'subjects' in response_json
        subjects_data = response_json['subjects']
        assert len(subjects_data) > 0

        subject1, subject2, subject3 = sirius_subjects

        self.sequence_contains_id_key(subjects_data, subject1.pk)
        self.sequence_contains_id_key(subjects_data, subject2.pk)
        self.sequence_not_contains_id_key(subjects_data, subject3.pk)

    def test_feedback(self, jclient, sirius_users, course1):
        """
        Тест оценки курса.

        * первый пользователь оставляет фидбэк с оценкой POST /sirius-courses/<course_id>/feedback
        * проверяет, что в списке курсов GET /sirius-courses/ его оценка отображается верно
        * проевряет, что среднаяя оцекна по одному значению посчиталась верно
        * второй пользователь оставляет фидбэк с оценкой POST /sirius-courses/<course_id>/feedback
        * проверяет, что нельзя поставить оценку больше 5
        * проверяет, что в списке курсов GET /sirius-courses/ его оценка отображается верно
        * проевряет, что среднаяя оцекна по двум значениям посчиталась верно
        * второй пользователь меняет свой фидбэк с оценкой POST /sirius-courses/<course_id>/feedback
        * проверяет, что в списке курсов GET /sirius-courses/ его обновленная оценка отображается верно
        * проевряет, что среднаяя оцекна пересчиталась с учетом изменения его оценки
        """
        teacher_user, student_user = sirius_users

        jclient.login(user=student_user)

        feedback_url = reverse('v2:sirius-courses-feedback', args=(course1.pk,))
        list_url = reverse('v2:sirius-courses-list')

        response = jclient.post(feedback_url, {'score': 1, 'comment': 'bad course'})

        assert response.status_code == 403

        make_course_available_for_student(course1, student_user)
        make_course_available_for_student(course1, teacher_user)

        response = jclient.post(reverse('v2:sirius-courses-join'), {'codes': [course1.code]})

        assert response.status_code == 200

        response = jclient.post(feedback_url, {'score': 1, 'comment': 'bad course'})
        response_json = response.json()

        assert response.status_code == 200
        assert response_json.get('score') == 1
        assert response_json.get('comment') == 'bad course'

        response = jclient.get(list_url)
        response_json = response.json()
        response_course = response_json.get('results')[0]

        assert response_course.get('average_score') == '1.0'
        assert response_course.get('feedback').get('score') == 1
        assert response_course.get('feedback').get('comment') == 'bad course'
        assert response.status_code == 200

        jclient.login(user=teacher_user)

        response = jclient.post(reverse('v2:sirius-courses-join'), {'codes': [course1.code]})
        assert response.status_code == 200

        response = jclient.post(feedback_url, {'score': 6})
        assert response.status_code == 400

        response = jclient.post(feedback_url, {'score': 2})

        response_json = response.json()
        assert response_json.get('score') == 2
        assert response_json.get('comment') is None
        assert response.status_code == 200

        response = jclient.get(list_url)
        response_json = response.json()

        response_course = response_json.get('results')[0]

        assert response_course.get('average_score') == '1.5'
        assert response_course.get('feedback').get('score') == 2
        assert response_course.get('feedback').get('comment') == ''
        assert response.status_code == 200

        response = jclient.post(
            feedback_url,
            {'score': 4, 'comment': 'I changed my mind'}
        )
        response_json = response.json()
        assert response_json.get('score') == 4
        assert response_json.get('comment') == 'I changed my mind'
        assert response.status_code == 200

        response = jclient.get(list_url)
        response_json = response.json()
        response_course = response_json.get('results')[0]

        assert response_course.get('average_score') == '2.5'
        assert response_course.get('feedback').get('score') == 4
        assert response_course.get('feedback').get('comment') == 'I changed my mind'
        assert response.status_code == 200

    def test_assigned(self, jclient, sirius_users, course1, course2):
        """
        Тест назначенных курсов.

        * ученику назначен 1 курс в фикстурах

        Проверки:
        - возвращается код 200
        - до назначения курса результат пуст
        - результат содержит число курсов равное назначенному
        - результат содержит назначенный курс
        """
        # создаем нового пользователя
        student1 = obj_factory(User, dict(
            username='student_user1',
            email='student1@mail.test',
        ))

        jclient.login(user=student1)

        url = reverse('v2:sirius-courses-assigned')
        response, response_json = get_response(jclient, url)

        assert response.status_code == 200
        # у пользователя нет назначенных курсов
        assert len(response_json) == 0

        # назначаем пользователю курс
        obj_factory(CourseStudent, dict(
            course=course1,
            student=student1
        ))

        response, response_json = get_response(jclient, url)

        assert len(response_json) == 1
        # тот курс, который мы назначили
        assert_contains_value(response_json,
                              value=course1.pk,
                              key='id')

        # назначаем еще один курс
        obj_factory(CourseStudent, dict(
            course=course2,
            student=student1
        ))

        response, response_json = get_response(jclient, url)
        assert len(response_json) == 2
        assert_contains_value(response_json,
                              value=course2.pk,
                              key='id')

    def test_sirius_join(self, jclient, course1, course2):
        """
        Тест добавления курса

        Проверки:
        * Невалидный запрос возвращает ошибку
        * Корректный запрос возвращает 200
        * Корректный запрос содержит информацию о добавленных курсах
        """
        # создаем нового пользователя без курсов

        student1 = obj_factory(User, dict(
            username='student_user1',
            email='student1@mail.test',
        ))

        url = reverse('v2:sirius-courses-join')

        jclient.login(user=student1)

        # отправляет пустые данные
        response = jclient.post(url, {})
        assert response.status_code == 400

        make_course_available_for_student(course1, student1)
        make_course_available_for_student(course2, student1)

        # формируем запрос со списком курсов к которым хотим присоединиться
        request_data = {
            'codes': [
                course1.code,
                course2.code,
            ]
        }

        response = jclient.post(url, request_data)
        response_json = response.json()

        assert response.status_code == 200
        assert_contains_value(response_json,
                              value=course1.pk,
                              key='id')
        assert_contains_value(response_json,
                              value=course2.pk,
                              key='id')

    def test_sirius_retrieve(self, jclient, sirius_student_user, sirius_teacher_user, sirius_lesson1, sirius_lesson2):
        """
        Тест API информации по курсу

        Проверки:
        * код ответа 200
        * ответ содержит необходимые секции данных
        * ответ содержит информацию по входящим в курс занятиям
        """
        course = obj_factory(Course, dict(
            name=u'Some title',
            description=u'Описание курса',
            code='AABBCCXX',
            owner=sirius_teacher_user,
            subject=obj_factory(Subject, dict(
                name=u'Subj title',
                slug='subj-title',
            )),
        ))

        make_course_available_for_student(course, sirius_student_user)

        obj_factory(CourseLessonLink, [
            dict(
                lesson=sirius_lesson1,
                course=course,
                order=1,
            ),
            dict(
                lesson=sirius_lesson2,
                course=course,
                order=2,
            )
        ])

        obj_factory(CourseStudent, dict(
            course=course,
            student=sirius_student_user
        ))

        url = reverse('v2:sirius-courses-detail', args=(course.pk, ))
        jclient.login(user=sirius_student_user)
        response, response_json = get_response(jclient, url)

        assert response.status_code == 200

        assert 'clessons' in response_json
        assert 'points' in response_json
        assert 'info' in response_json

        assert_contains_value(response_json['clessons'],
                              value=sirius_lesson1.pk,
                              key='lesson_id')
        assert_contains_value(response_json['clessons'],
                              value=sirius_lesson2.pk,
                              key='lesson_id')

    def test_leave(self, jclient, sirius_student_user, course1):
        """
        Тест API отвязки курса

        Проверки:
        * непривязанный курс не может быть отвязан - 400
        * привязанный курс успешно отвязывается
        * курс действительно не содержит данного пользователя
        """
        jclient.login(user=sirius_student_user)

        url = reverse('v2:sirius-courses-leave', args=(course1.pk,))

        # отвязка непривязанного курса возвращает код 400
        response = jclient.post(url)
        assert response.status_code == 400

        # выполняем привязку курса
        obj_factory(CourseStudent, dict(
            course=course1,
            student=sirius_student_user
        ))

        # отвязка привязанного курса возвращает код 200
        response = jclient.post(url)
        assert response.status_code == 200

        # пользователь действительно больше не привязан к курсу
        assert course1.is_assigned(sirius_student_user.id) is False

    def test_assigned_to(self, jclient, course1, course2, sirius_lesson1,
                         sirius_lesson2):
        """
        Тест API получения назначенных пользователю курсов по username

        Проверки:
        * апи доступен только staff-юзерам
        * апи возвращает только курсы пользователя
        * апи содержит список курсозанятий
        """
        url_name = 'v2:sirius-courses-assigned-to'

        # интерфейс доступен только пользователям с правами staff
        student_user = obj_factory(User, dict(
            username='student_user0',
            email='student0@mail.test',
        ))
        url = reverse(url_name, args=(student_user.id,))

        jclient.login(user=student_user)
        response, response_json = get_response(jclient, url)
        assert response.status_code == 403
        jclient.logout()

        # создаем нового пользователя staff
        staff_user = obj_factory(User, dict(
            username='staff_user',
            email='staff_user@mail.test',
            is_staff=True
        ))

        jclient.login(user=staff_user)

        url = reverse(url_name, args=(student_user.id,))
        response, response_json = get_response(jclient, url)
        # у ученика еще нет курсов
        assert len(response_json) == 0

        # создаем курсозанятия
        clessons1 = obj_factory(CourseLessonLink, [
            dict(
                lesson=sirius_lesson1,
                course=course1,
                order=1,
                mode=CourseLessonLink.CONTROL_WORK_MODE
            ),
            dict(
                lesson=sirius_lesson2,
                course=course1,
                order=2,
                mode=CourseLessonLink.CONTROL_WORK_MODE
            )
        ])

        # выполняем привязку курса
        obj_factory(CourseStudent, dict(
            course=course1,
            student=student_user
        ))

        url = reverse(url_name, args=(student_user.id,))
        response, response_json = get_response(jclient, url)

        assert_contains_value(response_json, value=course1.pk, key='id')
        assert_not_contains_value(response_json, value=course2.pk, key='id')

        course1_info = response_json[0]
        assert 'clessons' in course1_info
        assert len(course1_info.get('clessons')) == len(clessons1)
