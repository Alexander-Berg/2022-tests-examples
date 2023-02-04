from builtins import object
import pytest
from django.core.urlresolvers import reverse
from rest_framework.status import (
    HTTP_200_OK,
    HTTP_400_BAD_REQUEST,
    HTTP_401_UNAUTHORIZED,
    HTTP_403_FORBIDDEN,
    HTTP_404_NOT_FOUND,
    HTTP_429_TOO_MANY_REQUESTS,
)

from kelvin.lesson_assignments.models import LessonAssignment


@pytest.mark.django_db
class TestCourseJoin(object):
    """
    Тесты API на добавление ученика (пользователя) в курс
    """

    @staticmethod
    def assert_response_code(response, code=HTTP_200_OK):
        assert response.status_code == code, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content)
        )

    @staticmethod
    def assert_courses_ids_equal_to_response_ids(expected_ids, response):
        assert set(course['id'] for course in response.json()) == expected_ids

    @staticmethod
    def assert_students_in_course(students, course):
        course_students = set(course.students.values_list('id', flat=True))
        assert course_students == set(student.id for student in students)

    @pytest.mark.parametrize(
        'request_body, response_code',
        [
            ({}, HTTP_400_BAD_REQUEST),
            ({'codes': []}, HTTP_400_BAD_REQUEST),
            ({'codes': ['a', 1]}, HTTP_404_NOT_FOUND),
            ({'child': 'abc', 'codes': [1]}, HTTP_400_BAD_REQUEST),
            ({'child': 999, 'codes': [1]}, HTTP_403_FORBIDDEN),
            ({'codes': [999]}, HTTP_404_NOT_FOUND),
        ]
    )
    def test_incorrect_join_data(self, courses_with_students, jclient,
                                 request_body, response_code, student):
        """
        Негативные тесты добавления ученика в курс
        """

        join_url = reverse('v2:course-join')
        jclient.login(user=student)

        response = jclient.post(join_url, request_body)
        assert response.status_code == response_code, u'Неверный код ответа'

    def test_join(self, courses_with_students, student, student2, jclient):
        """
        Проверяет различные способы добавления ученика в курс

        NOTE: пользователь student добавлен во все курсы в фикстуре
        NOTE: пользователь student2 добавлен в course2 в фикстуре
        """
        join_url = reverse('v2:course-join')
        course1, course2, course3 = courses_with_students

        jclient.login(user=student2)

        # запрос на добавление и отображение курсов
        expected_ids = {course1.id, course2.id}
        response = jclient.post(join_url, {'codes': [course1.code]})

        self.assert_response_code(response)
        self.assert_courses_ids_equal_to_response_ids(expected_ids, response)
        self.assert_students_in_course([student, student2], course1)

        # повторное добавление
        response = jclient.post(join_url, {'codes': [course1.code]})

        self.assert_response_code(response)
        self.assert_courses_ids_equal_to_response_ids(expected_ids, response)
        self.assert_students_in_course([student, student2], course1)

        # запрос на добавление в несколько курсов
        jclient.login(user=student2)

        expected_ids = {course1.id, course2.id, course3.id}
        response = jclient.post(
            join_url, {'codes': [course1.code, course2.code, course3.code]}
        )

        self.assert_response_code(response)
        self.assert_courses_ids_equal_to_response_ids(expected_ids, response)
        self.assert_students_in_course([student, student2], course1)
        self.assert_students_in_course([student, student2], course2)
        self.assert_students_in_course([student, student2], course3)

    def test_join_by_parent(self, courses_with_students, parent_for_student2,
                            student, student2, jclient):
        """
        Родитель добавляет ребенка в курс
        """
        join_url = reverse('v2:course-join')
        course1, course2, course3 = courses_with_students

        jclient.login(user=parent_for_student2)

        # ребенок уже добавлен в один из курсов
        expected_ids = {course2.id, course3.id}
        response = jclient.post(
            join_url,
            {'child': student2.id, 'codes': [course3.code]}
        )

        self.assert_response_code(response)
        self.assert_courses_ids_equal_to_response_ids(expected_ids, response)
        self.assert_students_in_course([student, student2], course3)

        # родитель добавляет не своего ребенка в курс
        response = jclient.post(
            join_url,
            {'child': student.id, 'codes': [course3.code]}
        )

        self.assert_response_code(response, HTTP_403_FORBIDDEN)

    def test_join_to_course_with_variations(self, student, jclient,
                                            course_with_lesson_variations):
        """
        Проверяет, что ученику проставляются назначения в курсе с вариативными
        занятиями
        """
        data = course_with_lesson_variations
        course = data['course']

        clessons = data['clessons']

        join_url = reverse('v2:course-join')

        assert LessonAssignment.objects.filter(
            clesson__course=course, student=student).count() == 0

        jclient.login(user=student)
        response = jclient.post(join_url, {'codes': [course.code]})

        self.assert_response_code(response)

        assert LessonAssignment.objects.filter(
            clesson__course=course, student=student).count() == 2

        # назначение на первое занятие
        assignment = LessonAssignment.objects.get(clesson=clessons[0],
                                                  student=student)

        problem_links = data['problem_links'][clessons[0].lesson.id]

        assert len(assignment.problems) == 4

        problems_set = set(assignment.problems)

        def assert_problem_link_in_set(*problem_link_indexes):
            """
            По переданным индесам находим problem_link и проверяем
            что он есть в problems_set'e.
            """
            result = False
            for pid in problem_link_indexes:
                result |= problem_links[pid].id in problems_set
            assert result, u'Ни одного problem_link не найдено в problem_set'

        assert_problem_link_in_set(2)
        assert_problem_link_in_set(5)
        assert_problem_link_in_set(0, 3, 6)
        assert_problem_link_in_set(1, 4, 7)

        # назначение на второе занятие
        assignment = LessonAssignment.objects.get(clesson=clessons[1],
                                                  student=student)
        problem_links = data['problem_links'][clessons[1].lesson.id]
        assert len(assignment.problems) == 1
        assigned_problem = assignment.problems[0]
        assert (
            problem_links[0].id == assigned_problem or
            problem_links[1].id == assigned_problem or
            problem_links[2].id == assigned_problem
        )

    def test_join_not_allowed_for_anonymous(self, jclient):
        """
        Анонимный пользователь не может добавить курс
        """
        join_url = reverse('v2:course-join')
        response = jclient.post(join_url, {'codes': ['CODE']})

        self.assert_response_code(response, HTTP_401_UNAUTHORIZED)

    @pytest.mark.parametrize(
        'setup_throttling',
        [
            ('course_join', '1/day'),
        ],
        indirect=True,
    )
    @pytest.mark.usefixtures('setup_throttling')
    def test_check_throttling_for_authenticated_user(self, student2, jclient,
                                                     courses_with_students):
        """
        Срабатывает ratelimit для пользователя
        """
        join_url = reverse('v2:course-join')
        course1 = courses_with_students[0]

        jclient.login(user=student2)

        response = jclient.post(join_url, {'codes': [course1.code]})
        self.assert_response_code(response)

        response = jclient.post(join_url, {'codes': [course1.code]})
        self.assert_response_code(response, HTTP_429_TOO_MANY_REQUESTS)

        assert 'Retry-After' in response

