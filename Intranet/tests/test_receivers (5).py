from decimal import Decimal
from unittest.mock import patch

import faker

from django.test import TransactionTestCase

from lms.courses.tests.factories import (
    CourseFactory, CourseGroupFactory, CourseModuleFactory, CourseStudentFactory, LinkedCourseFactory,
    ModuleTypeFactory, StudentCourseProgressFactory, StudentModuleProgressFactory,
)
from lms.courses.tests.mixins import CourseModuleCacheMixin

from ..models import Course, CourseModule, CourseStudent

fake = faker.Faker()


class StudentModuleProgressPostSaveHandlerTestCase(TransactionTestCase):
    def test_student_module_progress_post_save(self):
        with patch('lms.courses.receivers.update_course_progress_task') as mock:
            course = CourseFactory()
            student = CourseStudentFactory.create_batch(5, course=course)[-1]
            module_type = ModuleTypeFactory(app_label="dummies", model="dummy")
            module = CourseModuleFactory(course=course, module_type=module_type)

            progress = StudentModuleProgressFactory(course=course, student=student, module=module)
            mock.assert_called_once_with(student_id=student.id)

            mock.reset_mock()
            progress.score_scaled = Decimal("33.33")
            progress.save()
            mock.assert_not_called()

            progress.score = 30
            progress.save()
            mock.assert_called_once_with(student_id=student.id)


class StudentCourseProgressPostSaveHandlerTestCase(TransactionTestCase):
    def setUp(self):
        self.threshold = fake.pyint(min_value=2, max_value=100)
        self.course = CourseFactory(completion_threshold=self.threshold)
        self.student = CourseStudentFactory(course=self.course, status=CourseStudent.StatusChoices.ACTIVE)

    def assert_passed(self):
        self.assertEqual(self.student.status, CourseStudent.StatusChoices.ACTIVE)
        self.assertTrue(self.student.is_passed)

    def assert_not_passed(self):
        self.assertEqual(self.student.status, CourseStudent.StatusChoices.ACTIVE)
        self.assertFalse(self.student.is_passed)

    def test_created_completion_threshold_has_been_overcome(self):
        with patch('lms.courses.receivers.start_course_passed_trigger_actions_task.delay') as mock:
            StudentCourseProgressFactory(
                course=self.course,
                student=self.student,
                score=fake.pyint(min_value=self.threshold, max_value=100)
            )
            mock.assert_called_once_with(course_id=self.course.id, student_id=self.student.id)
        self.assert_passed()

    def test_updated_completion_threshold_has_been_overcome(self):
        progress = StudentCourseProgressFactory(
            course=self.course,
            student=self.student,
            score=fake.pyint(min_value=0, max_value=self.threshold - 1)
        )
        progress.score = fake.pyint(min_value=self.threshold, max_value=100)
        with patch('lms.courses.receivers.start_course_passed_trigger_actions_task.delay') as mock:
            progress.save()
            mock.assert_called_once_with(course_id=self.course.id, student_id=self.student.id)
        self.assert_passed()

    def test_created_completion_threshold_has_not_been_overcome(self):
        StudentCourseProgressFactory(
            course=self.course,
            student=self.student,
            score=fake.pyint(min_value=0, max_value=self.threshold - 1)
        )
        self.assert_not_passed()

    def test_updated_completion_threshold_has_not_been_overcome(self):
        score = fake.pyint(min_value=0, max_value=self.threshold - 2)
        progress = StudentCourseProgressFactory(
            course=self.course,
            student=self.student,
            score=score
        )
        progress.score = fake.pyint(min_value=score + 1, max_value=self.threshold - 1)
        progress.save()
        self.assert_not_passed()


class CourseStudentPostSaveHandlerTestCase(TransactionTestCase):
    def test_update_modules_progress_for_student_created(self):
        with patch('lms.courses.receivers.update_modules_progress_for_student_task.delay') as mock:
            student = CourseStudentFactory(status=CourseStudent.StatusChoices.ACTIVE)
            mock.assert_called_once_with(student_id=student.id)

    def test_update_modules_progress_for_student_updated(self):
        with patch('lms.courses.receivers.update_modules_progress_for_student_task.delay') as mock:
            student = CourseStudentFactory(status=CourseStudent.StatusChoices.ACTIVE)
            mock.reset_mock()
            student.status = CourseStudent.StatusChoices.EXPELLED
            student.save()
            mock.assert_not_called()

    def test_update_linked_modules_progress_for_student_created(self):
        with patch('lms.courses.receivers.update_linked_modules_progress_for_student_task.delay') as mock:
            CourseStudentFactory(is_passed=False)
            mock.assert_not_called()

            student = CourseStudentFactory(is_passed=True)
            mock.assert_called_once_with(user_id=student.user_id, course_id=student.course_id)

    def test_update_linked_modules_progress_for_student_updated(self):
        with patch('lms.courses.receivers.update_linked_modules_progress_for_student_task.delay') as mock:
            student = CourseStudentFactory(is_passed=False)
            new_group = CourseGroupFactory(course=student.course)
            mock.reset_mock()

            student.group = new_group
            student.save()
            mock.assert_not_called()

            student.is_passed = True
            student.save()
            mock.assert_called_once_with(user_id=student.user_id, course_id=student.course_id)


class LinkedCoursePostSaveHandlerTestCase(CourseModuleCacheMixin, TransactionTestCase):
    def setUp(self):
        self.course = CourseFactory(course_type=Course.TypeChoices.TRACK)
        self.other_course = CourseFactory()
        self.linked_course = LinkedCourseFactory(course=self.course, weight=1)

    def test_cache_refresh_update_weight(self):
        self.linked_course.weight = 0
        self.assert_save_module(module=self.linked_course, course_id=self.course.id)

    def test_cache_refresh_update_is_active(self):
        self.linked_course.is_active = False
        self.assert_save_module(module=self.linked_course, course_id=self.course.id)

    def test_cache_refresh_update_other(self):
        self.linked_course.name = f'{self.linked_course.name} updated'
        self.assert_save_module(module=self.linked_course, call_count=0)

    def test_cache_refresh_create_zero_weight(self):
        linked_course = LinkedCourseFactory.build(course=self.course, linked_course=self.other_course, weight=0)
        self.assert_save_module(module=linked_course, course_id=self.course.id)

    def test_cache_refresh_create_not_active(self):
        linked_course = LinkedCourseFactory.build(course=self.course, linked_course=self.other_course, is_active=False)
        self.assert_save_module(module=linked_course, course_id=self.course.id)

    def test_cache_refresh_create(self):
        linked_course = LinkedCourseFactory.build(
            course=self.course, linked_course=self.other_course, weight=1, is_active=True
        )
        self.assert_save_module(module=linked_course, course_id=self.course.id)

    def test_update_linked_course_module_progress(self):
        with patch('lms.courses.receivers.update_linked_course_module_progress_task.delay') as mock:
            linked_course = LinkedCourseFactory()
            mock.assert_called_once_with(module_id=linked_course.id)

            mock.reset_mock()

            linked_course.name = f"{linked_course.name}_updated"
            linked_course.save()
            mock.assert_not_called()


class LinkedCoursePostDeleteHandlerTestCase(CourseModuleCacheMixin, TransactionTestCase):
    def setUp(self):
        self.course = CourseFactory(course_type=Course.TypeChoices.TRACK)

    def test_cache_refresh_delete_zero_weight(self):
        linked_course = LinkedCourseFactory(course=self.course, weight=0)
        self.assert_delete_module(module=linked_course, course_id=self.course.id)

    def test_cache_refresh_delete_not_active(self):
        linked_course = LinkedCourseFactory(course=self.course, is_active=False)
        self.assert_delete_module(module=linked_course, course_id=self.course.id)

    def test_cache_refresh_delete(self):
        linked_course = LinkedCourseFactory(course=self.course, weight=1, is_active=True)
        self.assert_delete_module(module=linked_course, course_id=self.course.id)


class CourseModulePostSaveHandlerTestCase(CourseModuleCacheMixin, TransactionTestCase):
    def setUp(self):
        self.course = CourseFactory()
        module_type = ModuleTypeFactory(app_label="dummies", model="dummy")
        fake_module = CourseModuleFactory(course=self.course, module_type=module_type, weight=1)
        self.module = CourseModule.objects.get(id=fake_module.id)

    def test_cache_refresh_update_weight(self):
        self.module.weight = 0
        self.assert_save_module(module=self.module, course_id=self.course.id)

    def test_cache_refresh_update_is_active(self):
        self.module.is_active = False
        self.assert_save_module(module=self.module, course_id=self.course.id)

    def test_cache_refresh_update_other(self):
        self.module.name = f'{self.module.name} updated'
        self.assert_save_module(module=self.module, call_count=0)
