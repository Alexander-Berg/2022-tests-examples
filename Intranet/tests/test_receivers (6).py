from django.test import TransactionTestCase

from lms.courses.tests.factories import CourseFactory
from lms.courses.tests.mixins import CourseModuleCacheMixin

from .factories import LinkResourceFactory, TextResourceFactory, VideoResourceFactory


class LinkResourcePostSaveHandlerTestCase(CourseModuleCacheMixin, TransactionTestCase):
    def setUp(self):
        self.course = CourseFactory()
        self.link_resource = LinkResourceFactory(course=self.course, weight=1)

    def test_cache_refresh_update_weight(self):
        self.link_resource.weight = 0
        self.assert_save_module(module=self.link_resource, course_id=self.course.id)

    def test_cache_refresh_update_is_active(self):
        self.link_resource.is_active = False
        self.assert_save_module(module=self.link_resource, course_id=self.course.id)

    def test_cache_refresh_update_other(self):
        self.link_resource.name = f'{self.link_resource.name} updated'
        self.assert_save_module(module=self.link_resource, call_count=0)

    def test_cache_refresh_create_zero_weight(self):
        link_resource = LinkResourceFactory.build(course=self.course, weight=0)
        self.assert_save_module(module=link_resource, course_id=self.course.id)

    def test_cache_refresh_create_not_active(self):
        link_resource = LinkResourceFactory.build(course=self.course, is_active=False)
        self.assert_save_module(module=link_resource, course_id=self.course.id)

    def test_cache_refresh_create(self):
        link_resource = LinkResourceFactory.build(course=self.course, weight=1, is_active=True)
        self.assert_save_module(module=link_resource, course_id=self.course.id)


class LinkResourcePostDeleteHandlerTestCase(CourseModuleCacheMixin, TransactionTestCase):
    def setUp(self):
        self.course = CourseFactory()

    def test_cache_refresh_delete_zero_weight(self):
        link_resource = LinkResourceFactory(course=self.course, weight=0)
        self.assert_delete_module(module=link_resource, course_id=self.course.id)

    def test_cache_refresh_delete_not_active(self):
        link_resource = LinkResourceFactory(course=self.course, is_active=False)
        self.assert_delete_module(module=link_resource, course_id=self.course.id)

    def test_cache_refresh_delete(self):
        link_resource = LinkResourceFactory(course=self.course, weight=1, is_active=True)
        self.assert_delete_module(module=link_resource, course_id=self.course.id)


class TextResourcePostSaveHandlerTestCase(CourseModuleCacheMixin, TransactionTestCase):
    def setUp(self):
        self.course = CourseFactory()
        self.text_resource = TextResourceFactory(course=self.course, weight=1)

    def test_cache_refresh_update_weight(self):
        self.text_resource.weight = 0
        self.assert_save_module(module=self.text_resource, course_id=self.course.id)

    def test_cache_refresh_update_is_active(self):
        self.text_resource.is_active = False
        self.assert_save_module(module=self.text_resource, course_id=self.course.id)

    def test_cache_refresh_update_other(self):
        self.text_resource.name = f'{self.text_resource.name} updated'
        self.assert_save_module(module=self.text_resource, call_count=0)

    def test_cache_refresh_create_zero_weight(self):
        text_resource = TextResourceFactory.build(course=self.course, weight=0)
        self.assert_save_module(module=text_resource, course_id=self.course.id)

    def test_cache_refresh_create_not_active(self):
        text_resource = TextResourceFactory.build(course=self.course, is_active=False)
        self.assert_save_module(module=text_resource, course_id=self.course.id)

    def test_cache_refresh_create(self):
        text_resource = TextResourceFactory.build(course=self.course, weight=1, is_active=True)
        self.assert_save_module(module=text_resource, course_id=self.course.id)


class TextResourcePostDeleteHandlerTestCase(CourseModuleCacheMixin, TransactionTestCase):
    def setUp(self):
        self.course = CourseFactory()

    def test_cache_refresh_delete_zero_weight(self):
        text_resource = TextResourceFactory(course=self.course, weight=0)
        self.assert_delete_module(module=text_resource, course_id=self.course.id)

    def test_cache_refresh_delete_not_active(self):
        text_resource = TextResourceFactory(course=self.course, is_active=False)
        self.assert_delete_module(module=text_resource, course_id=self.course.id)

    def test_cache_refresh_delete(self):
        text_resource = TextResourceFactory(course=self.course, weight=1, is_active=True)
        self.assert_delete_module(module=text_resource, course_id=self.course.id)


class VideoResourcePostSaveHandlerTestCase(CourseModuleCacheMixin, TransactionTestCase):
    def setUp(self):
        self.course = CourseFactory()
        self.video_resource = VideoResourceFactory(course=self.course, weight=1)

    def test_cache_refresh_update_weight(self):
        self.video_resource.weight = 0
        self.assert_save_module(module=self.video_resource, course_id=self.course.id)

    def test_cache_refresh_update_is_active(self):
        self.video_resource.is_active = False
        self.assert_save_module(module=self.video_resource, course_id=self.course.id)

    def test_cache_refresh_update_other(self):
        self.video_resource.name = f'{self.video_resource.name} updated'
        self.assert_save_module(module=self.video_resource, call_count=0)

    def test_cache_refresh_create_zero_weight(self):
        video_resource = VideoResourceFactory.build(course=self.course, weight=0)
        self.assert_save_module(module=video_resource, course_id=self.course.id)

    def test_cache_refresh_create_not_active(self):
        video_resource = VideoResourceFactory.build(course=self.course, is_active=False)
        self.assert_save_module(module=video_resource, course_id=self.course.id)

    def test_cache_refresh_create(self):
        video_resource = VideoResourceFactory.build(course=self.course, weight=1, is_active=True)
        self.assert_save_module(module=video_resource, course_id=self.course.id)


class VideoResourcePostDeleteHandlerTestCase(CourseModuleCacheMixin, TransactionTestCase):
    def setUp(self):
        self.course = CourseFactory()

    def test_cache_refresh_delete_zero_weight(self):
        video_resource = VideoResourceFactory(course=self.course, weight=0)
        self.assert_delete_module(module=video_resource, course_id=self.course.id)

    def test_cache_refresh_delete_not_active(self):
        video_resource = VideoResourceFactory(course=self.course, is_active=False)
        self.assert_delete_module(module=video_resource, course_id=self.course.id)

    def test_cache_refresh_delete(self):
        video_resource = VideoResourceFactory(course=self.course, weight=1, is_active=True)
        self.assert_delete_module(module=video_resource, course_id=self.course.id)
