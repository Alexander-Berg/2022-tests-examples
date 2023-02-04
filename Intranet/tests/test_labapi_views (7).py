from guardian.shortcuts import assign_perm

from django.conf import settings

from rest_framework import serializers, status
from rest_framework.test import APITestCase

from lms.core.tests.mixins import GenericRequestMixin, UrlNameMixin
from lms.courses.models import Course
from lms.courses.tests.factories import CourseFactory, CourseStudentFactory, StudentModuleProgressFactory
from lms.moduletypes.models import ModuleType
from lms.users.tests.factories import LabUserFactory

from ..models import VideoResource
from .factories import VideoResourceFactory


class LabVideoResourceViewSetCreateTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:videoresource-create'

    def setUp(self) -> None:
        # fill the cache
        ModuleType.objects.get_for_model(VideoResource)
        self.user = LabUserFactory(is_staff=True)
        self.course = CourseFactory()

    def test_url(self):
        self.assertURLNameEqual('videoresource_modules/', args=(), base_url=settings.LABAPI_BASE_URL)

    def test_create(self):
        assign_perm('courses.change_course', self.user, self.course)
        self.client.force_login(user=self.user)

        url = self.get_url()

        def make_expected(response):
            obj: VideoResource = VideoResource.objects.get(pk=response.data['id'])
            return {
                'id': obj.id,
                'block_id': None,
                'name': obj.name,
                'description': obj.description,
                'url': obj.url,
                'is_active': obj.is_active,
                'estimated_time': obj.estimated_time,
                'created': serializers.DateTimeField().to_representation(obj.created),
                'modified': serializers.DateTimeField().to_representation(obj.modified),
            }

        new_obj: VideoResource = VideoResourceFactory.build(course=self.course)
        payload = {
            'is_active': new_obj.is_active,
            'name': new_obj.name,
            'url': new_obj.url,
            'description': new_obj.description,
            'course_id': self.course.id,
        }
        self.create_request(url, data=payload, expected=make_expected, num_queries=13)


class LabVideoResourceViewSetDetailTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:videoresource-detail'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.course: Course = CourseFactory()
        self.resource: VideoResource = VideoResourceFactory(course=self.course)

    def test_url(self):
        self.assertURLNameEqual(
            'videoresource_modules/{}/',
            args=(self.resource.id,),
            base_url=settings.LABAPI_BASE_URL,
        )

    def build_expected(self, obj):
        return {
            'id': obj.id,
            'block_id': None,
            'name': obj.name,
            'description': obj.description,
            'url': obj.url,
            'is_active': obj.is_active,
            'estimated_time': obj.estimated_time,
            'created': serializers.DateTimeField().to_representation(obj.created),
            'modified': serializers.DateTimeField().to_representation(obj.modified),
        }

    def test_detail(self):
        assign_perm('courses.view_course', self.user, self.course)
        self.client.force_login(user=self.user)
        expected = self.build_expected(self.resource)
        url = self.get_url(self.resource.id)
        self.detail_request(url, expected=expected, num_queries=8)

    def test_update(self):
        new_obj: VideoResource = VideoResourceFactory.build(course=self.course)
        payload = {
            'name': new_obj.name,
            'description': new_obj.description,
            'url': new_obj.url,
        }

        def make_expected(response):
            obj = VideoResource.objects.get(pk=response.data['id'])
            return {
                'id': obj.id,
                'block_id': None,
                'name': obj.name,
                'description': obj.description,
                'url': obj.url,
                'is_active': obj.is_active,
                'estimated_time': obj.estimated_time,
                'created': serializers.DateTimeField().to_representation(obj.created),
                'modified': serializers.DateTimeField().to_representation(obj.modified),
            }

        assign_perm('courses.change_course', self.user, self.course)
        self.client.force_login(user=self.user)
        url = self.get_url(self.resource.id)
        self.update_request(url=url, data=payload, expected=make_expected, num_queries=13)

    def test_delete(self):
        assign_perm('courses.change_course', self.user, self.course)
        self.client.force_login(user=self.user)
        url = self.get_url(self.resource.id)
        self.delete_request(url=url, num_queries=12)

    def test_delete_with_progress(self):
        student = CourseStudentFactory(course=self.course)
        StudentModuleProgressFactory(course=self.course, student=student, module=self.resource)

        assign_perm('courses.change_course', self.user, self.course)
        self.client.force_login(user=self.user)
        url = self.get_url(self.resource.id)

        with self.assertNumQueries(11):
            response = self.client.delete(url, format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assert_errors(response.data, ['protected'])
