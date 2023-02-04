from django.conf import settings

from rest_framework import status
from rest_framework.test import APITestCase

from lms.core.tests.mixins import GenericRequestMixin, UrlNameMixin
from lms.courses.models import CourseStudent, StudentModuleProgress
from lms.courses.tests.factories import CourseFactory, CourseStudentFactory, StudentModuleProgressFactory
from lms.users.tests.factories import UserFactory

from .factories import LinkResourceFactory, TextResourceFactory, VideoResourceFactory


class VideoResourceViewSetDetailTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:videoresource-detail'

    def setUp(self) -> None:
        self.course = CourseFactory()
        self.module = VideoResourceFactory(course=self.course)

    def build_expected(self, module):
        return {
            'id': module.id,
            'name': module.name,
            'description': module.description,
            'url': module.url,
            'estimated_time': module.estimated_time,
        }

    def test_url(self):
        self.assertURLNameEqual('videoresource_modules/{}/', args=(self.module.id,), base_url=settings.API_BASE_URL)

    def test_no_auth(self):
        self.detail_request(url=self.get_url(self.module.id), status_code=status.HTTP_403_FORBIDDEN, num_queries=0)

    def test_is_not_student(self):
        self.client.force_login(user=UserFactory())
        self.detail_request(url=self.get_url(self.module.id), status_code=status.HTTP_403_FORBIDDEN, num_queries=5)

    def test_detail(self):
        user = UserFactory()
        CourseStudentFactory(course=self.module.course, user=user)
        self.client.force_login(user=user)
        self.detail_request(url=self.get_url(self.module.id), expected=self.build_expected(self.module), num_queries=6)


class LinkResourceDetailTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:linkresource-detail'

    def setUp(self) -> None:
        self.course = CourseFactory()
        self.module = LinkResourceFactory(course=self.course)

    def build_expected(self, module):
        return {
            'id': module.id,
            'name': module.name,
            'description': module.description,
            'url': module.url,
            'estimated_time': module.estimated_time,
        }

    def test_url(self):
        self.assertURLNameEqual('linkresource_modules/{}/', args=(self.module.id,), base_url=settings.API_BASE_URL)

    def test_no_auth(self):
        self.detail_request(url=self.get_url(self.module.id), status_code=status.HTTP_403_FORBIDDEN, num_queries=0)

    def test_is_not_student(self):
        self.client.force_login(user=UserFactory())
        self.detail_request(url=self.get_url(self.module.id), status_code=status.HTTP_403_FORBIDDEN, num_queries=5)

    def test_available_for_active_or_completed_students(self):
        for student_status in [CourseStudent.StatusChoices.ACTIVE, CourseStudent.StatusChoices.COMPLETED]:
            student = CourseStudentFactory(course=self.course, status=student_status)
            self.client.force_login(user=student.user)
            self.detail_request(
                url=self.get_url(self.module.id),
                num_queries=6,
            )

    def test_not_available_for_expelled_student(self):
        student = CourseStudentFactory(course=self.course, status=CourseStudent.StatusChoices.EXPELLED)
        self.client.force_login(user=student.user)
        self.detail_request(
            url=self.get_url(self.module.id),
            num_queries=5,
            status_code=status.HTTP_403_FORBIDDEN,
        )

    def test_not_found(self):
        self.client.force_login(user=UserFactory())
        self.detail_request(url=self.get_url(self.module.id + 1), status_code=status.HTTP_404_NOT_FOUND, num_queries=3)

    def test_detail(self):
        user = UserFactory()
        CourseStudentFactory(course=self.module.course, user=user)
        self.client.force_login(user=user)
        self.detail_request(url=self.get_url(self.module.id), expected=self.build_expected(self.module), num_queries=6)


class TextResourceDetailTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:textresource-detail'

    def setUp(self) -> None:
        self.course = CourseFactory()
        self.module = TextResourceFactory(course=self.course)

    def build_expected(self, module):
        return {
            'id': module.id,
            'name': module.name,
            'description': module.description,
            'content': module.content,
            'estimated_time': module.estimated_time,
        }

    def test_url(self):
        self.assertURLNameEqual('textresource_modules/{}/', args=(self.module.id,), base_url=settings.API_BASE_URL)

    def test_no_auth(self):
        self.detail_request(url=self.get_url(self.module.id), status_code=status.HTTP_403_FORBIDDEN, num_queries=0)

    def test_is_not_student(self):
        self.client.force_login(user=UserFactory())
        self.detail_request(url=self.get_url(self.module.id), status_code=status.HTTP_403_FORBIDDEN, num_queries=5)

    def test_available_for_active_or_completed_students(self):
        for student_status in [CourseStudent.StatusChoices.ACTIVE, CourseStudent.StatusChoices.COMPLETED]:
            student = CourseStudentFactory(course=self.course, status=student_status)
            self.client.force_login(user=student.user)
            self.detail_request(
                url=self.get_url(self.module.id),
                num_queries=6,
            )

    def test_not_available_for_expelled_student(self):
        student = CourseStudentFactory(course=self.course, status=CourseStudent.StatusChoices.EXPELLED)
        self.client.force_login(user=student.user)
        self.detail_request(
            url=self.get_url(self.module.id),
            num_queries=5,
            status_code=status.HTTP_403_FORBIDDEN,
        )

    def test_not_found(self):
        self.client.force_login(user=UserFactory())
        self.detail_request(url=self.get_url(self.module.id + 1), status_code=status.HTTP_404_NOT_FOUND, num_queries=3)

    def test_detail(self):
        user = UserFactory()
        CourseStudentFactory(course=self.module.course, user=user)
        self.client.force_login(user=user)
        self.detail_request(url=self.get_url(self.module.id), expected=self.build_expected(self.module), num_queries=6)


class TextResourceCompleteTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:my-course-module-complete'

    def setUp(self):
        self.course = CourseFactory(is_active=True)
        self.module = TextResourceFactory(course=self.course)
        self.user = UserFactory()

    def test_url(self):
        self.assertURLNameEqual(
            'my/course_modules/{pk}/complete/', kwargs={'pk': 1}, base_url=settings.API_BASE_URL
        )

    def test_non_auth(self):
        with self.assertNumQueries(0):
            response = self.client.put(self.get_url(self.module.id), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_not_active_student(self):
        CourseStudentFactory(user=self.user, course=self.course, status=CourseStudent.StatusChoices.COMPLETED)
        self.client.force_login(user=self.user)

        with self.assertNumQueries(7):
            response = self.client.put(self.get_url(self.module.id), format='json')
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_complete_module(self):
        student = CourseStudentFactory(user=self.user, course=self.course)
        self.client.force_login(user=self.user)

        with self.assertNumQueries(15):
            response = self.client.put(self.get_url(self.module.id), format='json')
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        progress = StudentModuleProgress.objects.get(student=student, module_id=self.module.id)
        self.assertEqual(progress.score, 100)

    def test_complete_module_existing_progress(self):
        student = CourseStudentFactory(user=self.user, course=self.course)
        progress = StudentModuleProgressFactory(course=self.course, module=self.module, student=student, score=27)
        self.client.force_login(user=self.user)

        with self.assertNumQueries(17):
            response = self.client.put(self.get_url(self.module.id), format='json')
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        progress.refresh_from_db()
        self.assertEqual(progress.score, 100)
