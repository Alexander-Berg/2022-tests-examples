from django.test import TransactionTestCase

from lms.courses.tests.factories import CourseFactory
from lms.courses.tests.mixins import CourseModuleCacheMixin

from .factories import ClassroomFactory


class ClassroomPostSaveHandlerTestCase(CourseModuleCacheMixin, TransactionTestCase):
    def setUp(self):
        self.course = CourseFactory()
        self.classroom = ClassroomFactory(course=self.course, weight=1)

    def test_cache_refresh_update_weight(self):
        self.classroom.weight = 0
        self.assert_save_module(module=self.classroom, course_id=self.course.id)

    def test_cache_refresh_update_is_active(self):
        self.classroom.is_active = False
        self.assert_save_module(module=self.classroom, course_id=self.course.id)

    def test_cache_refresh_update_other(self):
        self.classroom.name = f'{self.classroom.name} updated'
        self.assert_save_module(module=self.classroom, call_count=0)

    def test_cache_refresh_create_zero_weight(self):
        classroom = ClassroomFactory.build(course=self.course, weight=0)
        self.assert_save_module(module=classroom, course_id=self.course.id)

    def test_cache_refresh_create_not_active(self):
        classroom = ClassroomFactory.build(course=self.course, is_active=False)
        self.assert_save_module(module=classroom, course_id=self.course.id)

    def test_cache_refresh_create(self):
        classroom = ClassroomFactory.build(course=self.course, weight=1, is_active=True)
        self.assert_save_module(module=classroom, course_id=self.course.id)


class ClassroomPostDeleteHandlerTestCase(CourseModuleCacheMixin, TransactionTestCase):
    def setUp(self):
        self.course = CourseFactory()

    def test_cache_refresh_delete_zero_weight(self):
        classroom = ClassroomFactory(course=self.course, weight=0)
        self.assert_delete_module(module=classroom, course_id=self.course.id)

    def test_cache_refresh_delete_not_active(self):
        classroom = ClassroomFactory(course=self.course, is_active=False)
        self.assert_delete_module(module=classroom, course_id=self.course.id)

    def test_cache_refresh_delete(self):
        classroom = ClassroomFactory(course=self.course, weight=1, is_active=True)
        self.assert_delete_module(module=classroom, course_id=self.course.id)
