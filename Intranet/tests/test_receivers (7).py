from django.test import TransactionTestCase

from lms.courses.tests.factories import CourseFactory
from lms.courses.tests.mixins import CourseModuleCacheMixin
from lms.scorm.tests.factories import CourseFileFactory, ScormFactory, ScormFileFactory


class ScormPostSaveHandlerTestCase(CourseModuleCacheMixin, TransactionTestCase):
    def setUp(self):
        self.course = CourseFactory()
        self.scorm = ScormFactory(course=self.course, weight=1)
        course_file = CourseFileFactory(course=self.course)
        ScormFileFactory(scorm=self.scorm, course_file=course_file)
        self.scorm.is_active = True
        self.scorm.save()

    def test_cache_refresh_update_weight(self):
        self.scorm.weight = 0
        self.assert_save_module(module=self.scorm, course_id=self.course.id)

    def test_cache_refresh_update_is_active(self):
        self.scorm.is_active = False
        self.assert_save_module(module=self.scorm, course_id=self.course.id)

    def test_cache_refresh_update_other(self):
        self.scorm.name = f'{self.scorm.name} updated'
        self.assert_save_module(module=self.scorm, call_count=0)

    def test_cache_refresh_create_zero_weight(self):
        scorm = ScormFactory.build(course=self.course, weight=0)
        self.assert_save_module(module=scorm, course_id=self.course.id)

    def test_cache_refresh_create(self):
        scorm = ScormFactory.build(course=self.course, is_active=False)
        self.assert_save_module(module=scorm, course_id=self.course.id)


class ScormPostDeleteHandlerTestCase(CourseModuleCacheMixin, TransactionTestCase):
    def setUp(self):
        self.course = CourseFactory()

    def test_cache_refresh_delete_zero_weight(self):
        scorm = ScormFactory(course=self.course, weight=0)
        self.assert_delete_module(module=scorm, course_id=self.course.id)

    def test_cache_refresh_delete_not_active(self):
        scorm = ScormFactory(course=self.course, is_active=False)
        self.assert_delete_module(module=scorm, course_id=self.course.id)

    def test_cache_refresh_delete(self):
        scorm = ScormFactory(course=self.course, weight=1)
        course_file = CourseFileFactory(course=self.course)
        ScormFileFactory(scorm=scorm, course_file=course_file)
        scorm.is_active = True
        scorm.save()
        self.assert_delete_module(module=scorm, course_id=self.course.id)
