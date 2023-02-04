from django.core.exceptions import ValidationError
from django.test import TestCase

from lms.courses.tests.factories import CourseFactory, CourseFileFactory, CourseStudentFactory

from ..models import ScormResourceStudentAttempt, ScormStudentAttempt
from ..services import create_new_attempt
from .factories import ScormFactory, ScormFileFactory, ScormResourceFactory


class NewtAttemptServiceTestCase(TestCase):
    def setUp(self):
        self.course = CourseFactory()
        self.student = CourseStudentFactory(course=self.course)
        self.scorm = ScormFactory(course=self.course, max_attempts=2)
        self.course_file = CourseFileFactory(course=self.course)
        ScormFileFactory(scorm=self.scorm, course_file=self.course_file)
        self.resources = ScormResourceFactory.create_batch(2, scorm_file=self.scorm.current_file)

    def test_max_attempts(self):
        with self.assertNumQueries(9):
            create_new_attempt(student=self.student, scorm=self.scorm)

        with self.assertNumQueries(10):
            create_new_attempt(student=self.student, scorm=self.scorm)

        with self.assertNumQueries(6):
            with self.assertRaises(ValidationError):
                create_new_attempt(student=self.student, scorm=self.scorm)

    def test_new_attempts(self):
        self.assertEqual(ScormStudentAttempt.objects.filter(
            student=self.student,
            scorm_file=self.scorm.current_file
        ).count(), 0)

        with self.assertNumQueries(9):
            create_new_attempt(student=self.student, scorm=self.scorm)

        attempt = ScormStudentAttempt.objects.filter(student=self.student, scorm_file=self.scorm.current_file).first()
        self.assertEqual(attempt.current_attempt, 1)

        for i, resource in enumerate(self.resources):
            self.assertEqual(
                ScormResourceStudentAttempt.objects
                .filter(student=attempt.student, scorm_resource=resource, current_attempt=attempt.current_attempt).
                count(),
                1,
            )

        with self.assertNumQueries(10):
            create_new_attempt(student=self.student, scorm=self.scorm)

        attempt.refresh_from_db()
        self.assertEqual(attempt.current_attempt, 2)

        for i, resource in enumerate(self.resources):
            self.assertEqual(
                ScormResourceStudentAttempt.objects
                .filter(student=attempt.student, scorm_resource=resource, current_attempt=attempt.current_attempt)
                .count(),
                1,
            )
