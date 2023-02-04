from decimal import Decimal

from faker import Faker

from django.core.exceptions import ValidationError
from django.test import TestCase, override_settings

from lms.courses.models import StudentModuleProgress
from lms.courses.tests.factories import (
    CourseFactory, CourseFileFactory, CourseStudentFactory, StudentModuleProgressFactory,
)

from ..models import ScormFile, ScormResourceStudentAttempt, ScormStudentAttempt
from .factories import ScormFactory, ScormFileFactory, ScormResourceFactory, ScormStudentAttemptFactory

fake = Faker()


class ScormModelTestCase(TestCase):
    def test_create_first_file_with_ready(self):
        scorm = ScormFactory()
        scorm_file = ScormFileFactory(scorm=scorm, course_file=CourseFileFactory(course=scorm.course),
                                      scorm_status=ScormFile.SCORM_MODULE_STATUS_READY)

        self.assertEqual(scorm.current_file, scorm_file)

    def test_create_first_file_with_error(self):
        scorm = ScormFactory()
        ScormFileFactory(scorm=scorm, course_file=CourseFileFactory(course=scorm.course),
                         scorm_status=ScormFile.SCORM_MODULE_STATUS_ERROR)

        self.assertIsNone(scorm.current_file)

    def test_create_second_file_with_ready(self):
        scorm = ScormFactory()
        scorm_file = ScormFileFactory(scorm=scorm, course_file=CourseFileFactory(course=scorm.course),
                                      scorm_status=ScormFile.SCORM_MODULE_STATUS_READY)

        self.assertEqual(scorm.current_file, scorm_file)

        new_scorm_file = ScormFileFactory(scorm=scorm, course_file=CourseFileFactory(course=scorm.course),
                                          scorm_status=ScormFile.SCORM_MODULE_STATUS_READY)

        self.assertEqual(scorm.current_file, new_scorm_file)

    def test_create_second_file_with_error(self):
        scorm = ScormFactory()
        scorm_file = ScormFileFactory(scorm=scorm, course_file=CourseFileFactory(course=scorm.course),
                                      scorm_status=ScormFile.SCORM_MODULE_STATUS_READY)

        self.assertEqual(scorm.current_file, scorm_file)

        ScormFileFactory(scorm=scorm, course_file=CourseFileFactory(course=scorm.course),
                         scorm_status=ScormFile.SCORM_MODULE_STATUS_ERROR)

        self.assertEqual(scorm.current_file, scorm_file)

    def test_delete(self):
        scorm = ScormFactory()
        attempt = ScormStudentAttemptFactory(scorm=scorm)
        progress = StudentModuleProgressFactory(module=scorm, student=attempt.student, course=scorm.course)

        with self.assertRaises(ValidationError):
            scorm.delete()

        attempt.delete()
        progress.delete()
        scorm.delete()

    @override_settings(SCORM_DEFAULT_MAX_ATTEMPTS=27)
    def test_default_max_attempts(self):
        scorm = ScormFactory(max_attempts=11)
        self.assertEqual(scorm.max_attempts, 11)

    @override_settings(SCORM_DEFAULT_MAX_ATTEMPTS=27)
    def test_update_max_attempts(self):
        scorm = ScormFactory()
        scorm.max_attempts = 13
        scorm.save()
        scorm.refresh_from_db()
        self.assertEqual(scorm.max_attempts, 13)

    @override_settings(SCORM_DEFAULT_MAX_ATTEMPTS=-20)
    def test_default_max_attempts_negative(self):
        scorm = ScormFactory(max_attempts=11)
        self.assertEqual(scorm.max_attempts, 11)

    @override_settings(SCORM_DEFAULT_MAX_ATTEMPTS=0)
    def test_default_max_attempts_zero(self):
        scorm = ScormFactory()
        self.assertEqual(scorm.max_attempts, 0)

    def test_default_max_attempts_not_set(self):
        scorm = ScormFactory(max_attempts=72)
        self.assertEqual(scorm.max_attempts, 72)

    def test_default_max_attempts_not_set_default(self):
        scorm = ScormFactory()
        self.assertEqual(scorm.max_attempts, 1)


class ScormResourceModelTestCase(TestCase):
    def setUp(self) -> None:
        self.course = CourseFactory()
        self.scorm = ScormFactory(course=self.course, max_attempts=0)
        self.course_file = CourseFileFactory(course=self.scorm.course)
        ScormFileFactory(scorm=self.scorm, course_file=self.course_file)
        self.resource = ScormResourceFactory(scorm_file=self.scorm.current_file)
        self.student = CourseStudentFactory(course=self.course)
        self.scorm_attempt = ScormStudentAttemptFactory(
            scorm=self.scorm, student=self.student, scorm_file=self.scorm.current_file
        )
        self.resource_attempt = ScormResourceStudentAttempt.objects.get(
            student=self.student, scorm_resource=self.resource
        )

    def test_delete(self):
        with self.assertRaises(ValidationError):
            self.resource.delete()

        self.resource_attempt.delete()
        self.resource.delete()

    def test_update_score(self):
        scaled_score = Decimal('0.9949999')
        self.resource_attempt.data = {'cmi': {'score': {'scaled': str(scaled_score)}}}
        with self.assertNumQueries(17):
            self.resource_attempt.save()

        module_progress_list = list(StudentModuleProgress.objects.filter(student=self.student))
        self.assertEqual(len(module_progress_list), 1)
        module_progress = module_progress_list[0]
        self.assertEqual(module_progress.module_id, self.scorm.id)
        self.assertEqual(module_progress.score, 99)

    def test_update_negative_score(self):
        self.resource_attempt.data = {'cmi': {'score': {'scaled': '-0.1234567'}}}
        with self.assertNumQueries(17):
            self.resource_attempt.save()

        module_progress = StudentModuleProgress.objects.get(student=self.student, module=self.scorm)
        self.assertEqual(module_progress.score, 0)

    def test_update_very_big_score(self):
        self.resource_attempt.data = {'cmi': {'score': {'scaled': '1.1234567'}}}
        with self.assertNumQueries(17):
            self.resource_attempt.save()

        module_progress = StudentModuleProgress.objects.get(student=self.student, module=self.scorm)
        self.assertEqual(module_progress.score, 100)

    def test_update_score_round_up(self):
        self.resource_attempt.data = {'cmi': {'score': {'scaled': '0.7150000'}}}
        with self.assertNumQueries(17):
            self.resource_attempt.save()

        module_progress = StudentModuleProgress.objects.get(student=self.student, module=self.scorm)
        self.assertEqual(module_progress.score, 72)

    def test_update_score_with_nan_score_scaled_progress_not_created(self):
        self.resource_attempt.data = {'cmi': {'score': {'scaled': 'nan'}}}
        with self.assertNumQueries(8):
            self.resource_attempt.save()

        self.assertFalse(StudentModuleProgress.objects.filter(student=self.student, module=self.scorm).exists())

    def test_update_score_with_inf_score_scaled_not_created(self):
        self.resource_attempt.data = {'cmi': {'score': {'scaled': 'infinity'}}}
        with self.assertNumQueries(8):
            self.resource_attempt.save()

        self.assertFalse(StudentModuleProgress.objects.filter(student=self.student, module=self.scorm).exists())

    def test_update_score_with_nan_score_scaled_progress_not_updated(self):
        module_progress = StudentModuleProgressFactory(
            student=self.student, module=self.scorm, course=self.course, score=72
        )
        self.resource_attempt.data = {'cmi': {'score': {'scaled': 'nan'}}}
        with self.assertNumQueries(8):
            self.resource_attempt.save()

        module_progress.refresh_from_db()
        self.assertEqual(module_progress.score, 72)

    def test_update_multiple_times(self):
        self.resource_attempt.data = {'cmi': {'score': {'scaled': '0.1'}}}
        with self.assertNumQueries(17):
            self.resource_attempt.save()

        self.resource_attempt.data = {'cmi': {'score': {'scaled': '0.1', 'max': '1'}}}
        with self.assertNumQueries(7):
            self.resource_attempt.save()

        self.resource_attempt.data = {'cmi': {'score': {'scaled': '0.2', 'max': '1'}}}
        with self.assertNumQueries(14):
            self.resource_attempt.save()

    def test_update_score_completed_and_passed_status(self):
        self.resource_attempt.data = {'cmi': {
            'completion_status': 'completed',
            'success_status': 'passed',
            'score': {'scaled': '0.5'},
        }}
        with self.assertNumQueries(17):
            self.resource_attempt.save()

        module_progress = StudentModuleProgress.objects.get(student=self.student, module=self.scorm)
        self.assertEqual(module_progress.score, 100)

    def test_update_score_completed_and_not_passed_status(self):
        self.resource_attempt.data = {'cmi': {
            'completion_status': 'completed',
            'success_status': 'failed',
            'score': {'scaled': '0.5'},
        }}
        with self.assertNumQueries(17):
            self.resource_attempt.save()

        module_progress = StudentModuleProgress.objects.get(student=self.student, module=self.scorm)
        self.assertEqual(module_progress.score, 50)

    def test_update_score_not_completed_and_passed_status(self):
        self.resource_attempt.data = {'cmi': {
            'completion_status': 'incomplete',
            'success_status': 'passed',
            'score': {'scaled': '0.5'},
        }}
        with self.assertNumQueries(17):
            self.resource_attempt.save()

        module_progress = StudentModuleProgress.objects.get(student=self.student, module=self.scorm)
        self.assertEqual(module_progress.score, 50)

    def test_update_score_after_new_attempt_lower_score(self):
        progress = StudentModuleProgressFactory(
            student=self.student, module=self.scorm, course=self.scorm.course, score=33
        )
        self.scorm_attempt.save()
        self.scorm_attempt.refresh_from_db()
        attempt = ScormResourceStudentAttempt.objects.get(
            student=self.student, scorm_resource=self.resource, current_attempt=self.scorm_attempt.current_attempt
        )

        attempt.data = {'cmi': {'score': {'scaled': '0.2'}}}
        with self.assertNumQueries(11):
            attempt.save()

        progress.refresh_from_db()
        self.assertEqual(progress.score, 33)

    def test_update_score_after_new_attempt_higher_score(self):
        progress = StudentModuleProgressFactory(
            student=self.student, module=self.scorm, course=self.scorm.course, score=33
        )
        self.scorm_attempt.save()
        self.scorm_attempt.refresh_from_db()
        attempt = ScormResourceStudentAttempt.objects.get(
            student=self.student, scorm_resource=self.resource, current_attempt=self.scorm_attempt.current_attempt
        )

        attempt.data = {'cmi': {'score': {'scaled': '0.5'}}}
        with self.assertNumQueries(18):
            attempt.save()

        progress.refresh_from_db()
        self.assertEqual(progress.score, 50)


class ScormStudentAttemptModelTestCase(TestCase):
    def test_student_from_other_course(self):
        student = CourseStudentFactory()
        scorm = ScormFactory()
        attempt = ScormStudentAttempt(student=student, scorm=scorm)

        with self.assertRaises(ValidationError):
            attempt.save()

    def test_max_attempts(self):
        course = CourseFactory()
        student = CourseStudentFactory(course=course)
        scorm = ScormFactory(course=course)
        course_file = CourseFileFactory(course=course)
        ScormFileFactory(scorm=scorm, course_file=course_file)
        attempt = ScormStudentAttempt(student=student, scorm=scorm, scorm_file=scorm.current_file)
        attempt.save()
        attempt.current_attempt += 1

        with self.assertRaises(ValidationError):
            attempt.save()
