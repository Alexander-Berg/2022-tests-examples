from django.test import TransactionTestCase

from lms.assignments.tests.factories import AssignmentFactory, AssignmentStudentResultFactory
from lms.courses.models import StudentModuleProgress
from lms.courses.tests.factories import CourseFactory
from lms.courses.tests.mixins import CourseModuleCacheMixin


class AssignmentStudentResultPostSaveHandlerTestCase(TransactionTestCase):
    def setUp(self):
        self.course = CourseFactory()
        self.assignment = AssignmentFactory(course=self.course)

    def test_new_assignment_student_result_completed(self):
        result = AssignmentStudentResultFactory(assignment=self.assignment, is_completed=True)
        module_progress = StudentModuleProgress.objects.get(module=self.assignment, student=result.student)

        self.assertEqual(module_progress.score, 100)

    def test_new_assignment_student_result_not_completed(self):
        result = AssignmentStudentResultFactory(assignment=self.assignment, is_completed=False)
        module_progress = StudentModuleProgress.objects.get(module=self.assignment, student=result.student)

        self.assertEqual(module_progress.score, 0)

    def test_update_existing_assignment_student_result_not_completed(self):
        result = AssignmentStudentResultFactory(assignment=self.assignment, is_completed=True)
        module_progress = StudentModuleProgress.objects.get(module=self.assignment, student=result.student)

        result.is_completed = False
        result.save()
        module_progress.refresh_from_db()

        self.assertEqual(module_progress.score, 0)

    def test_update_existing_assignment_student_result_completed(self):
        result = AssignmentStudentResultFactory(assignment=self.assignment, is_completed=False)
        module_progress = StudentModuleProgress.objects.get(module=self.assignment, student=result.student)

        result.is_completed = True
        result.save()
        module_progress.refresh_from_db()

        self.assertEqual(module_progress.score, 100)


class AssignmentPostSaveHandlerTestCase(CourseModuleCacheMixin, TransactionTestCase):
    def setUp(self):
        self.course = CourseFactory()
        self.assignment = AssignmentFactory(course=self.course, weight=1)

    def test_cache_refresh_update_weight(self):
        self.assignment.weight = 0
        self.assert_save_module(module=self.assignment, course_id=self.course.id)

    def test_cache_refresh_update_is_active(self):
        self.assignment.is_active = False
        self.assert_save_module(module=self.assignment, course_id=self.course.id)

    def test_cache_refresh_update_other(self):
        self.assignment.name = f'{self.assignment.name} updated'
        self.assert_save_module(module=self.assignment, call_count=0)

    def test_cache_refresh_create_zero_weight(self):
        assignment = AssignmentFactory.build(course=self.course, weight=0)
        self.assert_save_module(module=assignment, course_id=self.course.id)

    def test_cache_refresh_create_not_active(self):
        assignment = AssignmentFactory.build(course=self.course, is_active=False)
        self.assert_save_module(module=assignment, course_id=self.course.id)

    def test_cache_refresh_create(self):
        assignment = AssignmentFactory.build(course=self.course, weight=1, is_active=True)
        self.assert_save_module(module=assignment, course_id=self.course.id)


class AssignmentPostDeleteHandlerTestCase(CourseModuleCacheMixin, TransactionTestCase):
    def setUp(self):
        self.course = CourseFactory()

    def test_cache_refresh_delete_zero_weight(self):
        assignment = AssignmentFactory(course=self.course, weight=0)
        self.assert_delete_module(module=assignment, course_id=self.course.id)

    def test_cache_refresh_delete_not_active(self):
        assignment = AssignmentFactory(course=self.course, is_active=False)
        self.assert_delete_module(module=assignment, course_id=self.course.id)

    def test_cache_refresh_delete(self):
        assignment = AssignmentFactory(course=self.course, weight=1, is_active=True)
        self.assert_delete_module(module=assignment, course_id=self.course.id)
