from django.core.exceptions import ValidationError
from django.test import TestCase

from lms.courses.tests.factories import CourseFactory

from .factories import AssignmentFactory, AssignmentStudentResultFactory, CourseStudentFactory


class AssignmentStudentResultModelTestCase(TestCase):
    def setUp(self) -> None:
        self.assignment = AssignmentFactory()
        self.student = CourseStudentFactory(course=self.assignment.course)

    def test_course_from_assignment(self):
        assignment_student_result = AssignmentStudentResultFactory(assignment=self.assignment, student=self.student)
        self.assertEqual(assignment_student_result.course, self.assignment.course)

    def test_student_different_course(self):
        another_course = CourseFactory()
        another_course_student = CourseStudentFactory(course=another_course)
        with self.assertRaises(ValidationError):
            AssignmentStudentResultFactory(assignment=self.assignment, student=another_course_student)
