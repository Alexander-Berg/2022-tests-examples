from django.conf import settings

from rest_framework import status
from rest_framework.test import APITestCase

from lms.core.tests.mixins import GenericRequestMixin, UrlNameMixin
from lms.moduletypes.models import ModuleType
from lms.users.tests.factories import UserFactory

from ..models import Assignment, CourseStudent
from .factories import AssignmentFactory, AssignmentStudentResultFactory, CourseFactory, CourseStudentFactory


class AssignmentDetailTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:assignment-detail'

    def setUp(self):
        ModuleType.objects.get_for_model(Assignment)
        self.user = UserFactory()
        self.course = CourseFactory()
        self.student = CourseStudentFactory(course=self.course, user=self.user)
        self.assignment = AssignmentFactory(course=self.course)

    def build_expected(self, assignment: Assignment):
        return {
            "id": assignment.id,
            "name": assignment.name,
            "description": assignment.description,
            "estimated_time": assignment.estimated_time,
        }

    def test_url(self):
        self.assertURLNameEqual('assignment_modules/{}/', args=(self.assignment.id,), base_url=settings.API_BASE_URL)

    def test_no_auth(self):
        self.detail_request(
            url=self.get_url(self.assignment.id),
            status_code=status.HTTP_403_FORBIDDEN,
            num_queries=0
        )

    def test_another_course(self):
        another_course = CourseFactory()
        another_student = CourseStudentFactory(course=another_course)
        self.client.force_login(user=another_student.user)
        self.create_request(
            url=self.get_url(self.assignment.id),
            status_code=status.HTTP_403_FORBIDDEN,
            num_queries=5
        )

    def test_not_exists(self):
        self.client.force_login(user=self.user)
        self.detail_request(
            url=self.get_url(self.assignment.id + 1),
            status_code=status.HTTP_404_NOT_FOUND,
            num_queries=3
        )

    def test_get_result(self):
        self.client.force_login(user=self.user)
        self.detail_request(
            url=self.get_url(self.assignment.id),
            expected=self.build_expected(self.assignment),
            num_queries=6
        )

    def test_available_for_active_or_completed_students(self):
        for student_status in [CourseStudent.StatusChoices.ACTIVE, CourseStudent.StatusChoices.COMPLETED]:
            student = CourseStudentFactory(course=self.course, status=student_status)
            self.client.force_login(user=student.user)
            self.detail_request(
                url=self.get_url(self.assignment.id),
                num_queries=6,
            )

    def test_not_available_for_expelled_student(self):
        student = CourseStudentFactory(course=self.course, status=CourseStudent.StatusChoices.EXPELLED)
        self.client.force_login(user=student.user)
        self.detail_request(
            url=self.get_url(self.assignment.id),
            num_queries=5,
            status_code=status.HTTP_403_FORBIDDEN,
        )


class AssignmentSetCompletedDeprecatedTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:my-assignment-complete'

    def setUp(self):
        ModuleType.objects.get_for_model(Assignment)
        self.user = UserFactory()
        self.course = CourseFactory()
        self.student = CourseStudentFactory(course=self.course, user=self.user)
        self.assignment = AssignmentFactory(course=self.course)

    def test_url(self):
        self.assertURLNameEqual(
            'my/assignment_modules/{}/complete/',
            args=(self.assignment.id,),
            base_url=settings.API_BASE_URL
        )

    def test_no_auth(self):
        self.detail_request(
            url=self.get_url(self.assignment.id),
            status_code=status.HTTP_403_FORBIDDEN,
            num_queries=0
        )

    def test_not_exists(self):
        self.client.force_login(user=self.user)
        self.detail_request(
            url=self.get_url(self.assignment.id + 1),
            status_code=status.HTTP_404_NOT_FOUND,
            num_queries=3
        )

    def test_set_completed_first_time(self):
        self.client.force_login(user=self.user)
        self.create_request(
            url=self.get_url(self.assignment.id),
            status_code=status.HTTP_204_NO_CONTENT,
            num_queries=26
        )
        result = self.assignment.results.first()
        self.assertIsNotNone(result)
        expected = AssignmentStudentResultFactory.build(
            id=result.id,
            assignment_id=self.assignment.id,
            student_id=self.student.id,
            is_completed=True,
        )
        self.assertEqual(result, expected)

    def test_set_completed_existing_result(self):
        result = AssignmentStudentResultFactory(
            assignment=self.assignment,
            student=self.student,
            is_completed=False,
        )
        self.client.force_login(user=self.user)
        self.create_request(
            url=self.get_url(self.assignment.id),
            status_code=status.HTTP_204_NO_CONTENT,
            num_queries=25
        )
        expected = AssignmentStudentResultFactory.build(
            id=result.id,
            assignment_id=self.assignment.id,
            student_id=self.student.id,
            is_completed=True,
        )
        self.assertEqual(result, expected)

    def test_another_course(self):
        another_course = CourseFactory()
        another_student = CourseStudentFactory(course=another_course)
        self.client.force_login(user=another_student.user)
        self.create_request(
            url=self.get_url(self.assignment.id),
            status_code=status.HTTP_403_FORBIDDEN,
            num_queries=4
        )

    def test_not_active_student(self):
        user = UserFactory()
        CourseStudentFactory(user=user, course=self.course, status=CourseStudent.StatusChoices.COMPLETED)
        self.client.force_login(user=user)

        self.create_request(
            url=self.get_url(self.assignment.id),
            status_code=status.HTTP_404_NOT_FOUND,
            num_queries=5
        )


class AssignmentSetCompletedTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:my-course-module-complete'

    def setUp(self):
        ModuleType.objects.get_for_model(Assignment)
        self.user = UserFactory()
        self.course = CourseFactory()
        self.student = CourseStudentFactory(course=self.course, user=self.user)
        self.assignment = AssignmentFactory(course=self.course)

    def test_url(self):
        self.assertURLNameEqual(
            'my/course_modules/{}/complete/',
            args=(self.assignment.id,),
            base_url=settings.API_BASE_URL
        )

    def test_no_auth(self):
        self.detail_request(
            url=self.get_url(self.assignment.id),
            status_code=status.HTTP_403_FORBIDDEN,
            num_queries=0
        )

    def test_not_exists(self):
        self.client.force_login(user=self.user)
        self.detail_request(
            url=self.get_url(self.assignment.id + 1),
            status_code=status.HTTP_404_NOT_FOUND,
            num_queries=3
        )

    def test_set_completed_first_time(self):
        self.client.force_login(user=self.user)
        self.update_request(
            url=self.get_url(self.assignment.id),
            data='',
            status_code=status.HTTP_204_NO_CONTENT,
            num_queries=28
        )
        result = self.assignment.results.first()
        self.assertIsNotNone(result)
        expected = AssignmentStudentResultFactory.build(
            id=result.id,
            assignment_id=self.assignment.id,
            student_id=self.student.id,
            is_completed=True,
        )
        self.assertEqual(result, expected)

    def test_set_completed_existing_result(self):
        result = AssignmentStudentResultFactory(
            assignment=self.assignment,
            student=self.student,
            is_completed=False,
        )
        self.client.force_login(user=self.user)
        self.update_request(
            url=self.get_url(self.assignment.id),
            data='',
            status_code=status.HTTP_204_NO_CONTENT,
            num_queries=27
        )
        expected = AssignmentStudentResultFactory.build(
            id=result.id,
            assignment_id=self.assignment.id,
            student_id=self.student.id,
            is_completed=True,
        )
        self.assertEqual(result, expected)

    def test_another_course(self):
        another_course = CourseFactory()
        another_student = CourseStudentFactory(course=another_course)
        self.client.force_login(user=another_student.user)
        self.update_request(
            url=self.get_url(self.assignment.id),
            data='',
            status_code=status.HTTP_403_FORBIDDEN,
            num_queries=6
        )

    def test_not_active_student(self):
        user = UserFactory()
        CourseStudentFactory(user=user, course=self.course, status=CourseStudent.StatusChoices.COMPLETED)
        self.client.force_login(user=user)

        self.update_request(
            url=self.get_url(self.assignment.id),
            data='',
            status_code=status.HTTP_404_NOT_FOUND,
            num_queries=7
        )


class AssignmentSetIncompleteTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:my-assignment-incomplete'

    def setUp(self):
        ModuleType.objects.get_for_model(Assignment)
        self.user = UserFactory()
        self.course = CourseFactory()
        self.student = CourseStudentFactory(course=self.course, user=self.user)
        self.assignment = AssignmentFactory(course=self.course)

    def test_url(self):
        self.assertURLNameEqual(
            'my/assignment_modules/{}/incomplete/',
            args=(self.assignment.id,),
            base_url=settings.API_BASE_URL
        )

    def test_no_auth(self):
        self.detail_request(
            url=self.get_url(self.assignment.id),
            status_code=status.HTTP_403_FORBIDDEN,
            num_queries=0
        )

    def test_not_exists(self):
        self.client.force_login(user=self.user)
        self.detail_request(
            url=self.get_url(self.assignment.id + 1),
            status_code=status.HTTP_404_NOT_FOUND,
            num_queries=3
        )

    def test_set_incomplete_first_time(self):
        self.client.force_login(user=self.user)
        self.create_request(
            url=self.get_url(self.assignment.id),
            status_code=status.HTTP_204_NO_CONTENT,
            num_queries=26
        )
        result = self.assignment.results.first()
        self.assertIsNotNone(result)
        expected = AssignmentStudentResultFactory.build(
            id=result.id,
            assignment_id=self.assignment.id,
            student_id=self.student.id,
            is_completed=False,
        )
        self.assertEqual(result, expected)

    def test_set_incomplete_existing_result(self):
        result = AssignmentStudentResultFactory(
            assignment=self.assignment,
            student=self.student,
            is_completed=True,
        )
        self.client.force_login(user=self.user)
        self.create_request(
            url=self.get_url(self.assignment.id),
            status_code=status.HTTP_204_NO_CONTENT,
            num_queries=25
        )
        expected = AssignmentStudentResultFactory.build(
            id=result.id,
            assignment_id=self.assignment.id,
            student_id=self.student.id,
            is_completed=False,
        )
        self.assertEqual(result, expected)

    def test_another_course(self):
        another_course = CourseFactory()
        another_student = CourseStudentFactory(course=another_course)
        self.client.force_login(user=another_student.user)
        self.create_request(
            url=self.get_url(self.assignment.id),
            status_code=status.HTTP_403_FORBIDDEN,
            num_queries=4
        )

    def test_not_active_student(self):
        user = UserFactory()
        CourseStudentFactory(user=user, course=self.course, status=CourseStudent.StatusChoices.COMPLETED)
        self.client.force_login(user=user)

        self.create_request(
            url=self.get_url(self.assignment.id),
            status_code=status.HTTP_404_NOT_FOUND,
            num_queries=5
        )


class AssignmentStudentResultDetailTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:my-assignment-detail'

    def setUp(self):
        ModuleType.objects.get_for_model(Assignment)
        self.user = UserFactory()
        self.course = CourseFactory()
        self.student = CourseStudentFactory(course=self.course, user=self.user)
        self.assignment = AssignmentFactory(course=self.course)

    def build_expected(self, is_completed):
        return {'is_completed': is_completed}

    def test_url(self):
        self.assertURLNameEqual(
            'my/assignment_modules/{}/',
            args=(self.assignment.id,),
            base_url=settings.API_BASE_URL
        )

    def test_no_auth(self):
        self.detail_request(
            url=self.get_url(self.assignment.id),
            status_code=status.HTTP_403_FORBIDDEN,
            num_queries=0
        )

    def test_not_exists(self):
        self.client.force_login(user=self.user)
        self.detail_request(
            url=self.get_url(self.assignment.id + 1),
            status_code=status.HTTP_404_NOT_FOUND,
            num_queries=3
        )

    def test_another_course(self):
        assignment = AssignmentFactory(course=self.course)
        another_course = CourseFactory()
        student = CourseStudentFactory(course=another_course)
        self.client.force_login(user=student.user)
        self.detail_request(
            url=self.get_url(assignment.id),
            status_code=status.HTTP_403_FORBIDDEN,
            num_queries=4
        )

    def test_no_student_results(self):
        self.client.force_login(user=self.student.user)
        self.detail_request(
            url=self.get_url(self.assignment.id),
            expected=self.build_expected(False),
            num_queries=6
        )

    def test_completed(self):
        AssignmentStudentResultFactory(
            assignment=self.assignment,
            student=self.student,
            is_completed=True,
        )
        self.client.force_login(user=self.user)
        self.detail_request(
            url=self.get_url(self.assignment.id),
            expected=self.build_expected(True),
            num_queries=6
        )

    def test_incomplete(self):
        AssignmentStudentResultFactory(
            assignment=self.assignment,
            student=self.student,
            is_completed=False,
        )
        self.client.force_login(user=self.user)
        self.detail_request(
            url=self.get_url(self.assignment.id),
            expected=self.build_expected(False),
            num_queries=6
        )

    def test_has_other_results(self):
        another_student = CourseStudentFactory(course=self.course)
        another_assignment = AssignmentFactory(course=self.course)
        AssignmentStudentResultFactory(
            assignment=another_assignment,
            student=self.student,
            is_completed=True,
        )
        AssignmentStudentResultFactory(
            assignment=another_assignment,
            student=another_student,
            is_completed=True,
        )
        AssignmentStudentResultFactory(
            assignment=self.assignment,
            student=another_student,
            is_completed=True,
        )
        self.client.force_login(user=self.user)
        self.detail_request(
            url=self.get_url(self.assignment.id),
            expected=self.build_expected(False),
            num_queries=6
        )

    def test_not_available_for_completed_student(self):
        student = CourseStudentFactory(course=self.course, status=CourseStudent.StatusChoices.COMPLETED)
        self.client.force_login(user=student.user)
        self.detail_request(
            url=self.get_url(self.assignment.id),
            num_queries=5,
            status_code=status.HTTP_404_NOT_FOUND,
        )

    def test_not_available_for_expelled_student(self):
        student = CourseStudentFactory(course=self.course, status=CourseStudent.StatusChoices.EXPELLED)
        self.client.force_login(user=student.user)
        self.detail_request(
            url=self.get_url(self.assignment.id),
            num_queries=4,
            status_code=status.HTTP_403_FORBIDDEN,
        )
