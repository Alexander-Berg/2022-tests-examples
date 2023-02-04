from unittest.mock import patch

from lms.core.tests.mixins import GenericRequestMixin, UrlNameMixin
from lms.courses.models import Course
from lms.courses.tests.factories import CourseFactory


class CourseListChoiceFilterMixin(UrlNameMixin, GenericRequestMixin):
    URL_NAME = 'api:course-list'

    def get_choices(self):
        return Course._meta.get_field(self.field_name).choices

    def get_filter_name(self):
        return self.filter_name or self.field_name

    @staticmethod
    def create_courses_by_field(field_name, batch=9):
        field = Course._meta.get_field(field_name)
        courses = {}
        for value, _ in field.choices:
            params = {
                'is_active': True,
                field_name: value,
            }
            courses[value] = CourseFactory.create_batch(batch, **params)
        return courses

    def assert_course_filter(self, filter_name, courses, num_queries=10):
        for value, items in courses.items():
            url = self.get_url() + f"?{filter_name}={value}"
            expected = self.build_expected(items)
            self.list_request(url, num_queries=num_queries, expected=expected, only_ids=True)

    def build_expected(self, courses):
        return [{"id": course.id} for course in sorted(courses, key=lambda course: course.name)]


class CourseModuleCacheMixin:
    """
    Миксин для тестирования пересчёта кэша весов модулей.
    Позволяет не дублировать логику мокирования и проверок в тестах для классов, наследующих CourseModule
    """

    CACHED_METHOD = 'lms.courses.services.get_course_module_weights'

    def _assert_mock_called(self, mock, course_id=None, call_count=1, check_params=True):
        self.assertEqual(
            mock.call_count, call_count,
            'Ожидалось, что \'{}\' будет вызван {} раз. Был вызван {} раз.'.format(
                self.CACHED_METHOD, call_count, mock.call_count
            )
        )
        if call_count > 0 and check_params:
            mock.assert_called_with(course_id=course_id, _refresh=True)

    def assert_save_module(self, module, course_id=None, call_count=1, check_params=True):
        with patch(self.CACHED_METHOD) as mock:
            module.save()
            self._assert_mock_called(mock, course_id=course_id, call_count=call_count, check_params=check_params)

    def assert_delete_module(self, module, course_id=None, call_count=1, check_params=True):
        with patch(self.CACHED_METHOD) as mock:
            module.delete()
            self._assert_mock_called(mock, course_id=course_id, call_count=call_count, check_params=check_params)
