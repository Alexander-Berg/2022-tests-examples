from unittest.mock import patch

from guardian.shortcuts import assign_perm

from django.conf import settings
from django.contrib.auth import get_user_model

from rest_framework import serializers, status
from rest_framework.test import APITestCase

from lms.core.tests.mixins import GenericRequestMixin, UrlNameMixin
from lms.courses.models import CourseFile
from lms.courses.tests.factories import CourseBlockFactory, CourseFactory, CourseFileFactory
from lms.moduletypes.models import ModuleType
from lms.users.tests.factories import LabUserFactory

from ..models import Scorm, ScormFile
from .factories import ScormFactory, ScormFileFactory

User = get_user_model()


class ScormLabViewSetCreateTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:scorm-create'

    def setUp(self):
        # fill the cache
        ModuleType.objects.get_for_model(Scorm)
        self.user = LabUserFactory(is_staff=True)
        self.course = CourseFactory()

    def test_url(self):
        self.assertURLNameEqual('scorm_modules/', args=(), base_url=settings.LABAPI_BASE_URL)

    def test_create_without_file(self):
        assign_perm('courses.change_course', self.user, self.course)
        self.client.force_login(user=self.user)

        url = self.get_url()

        def make_expected(response):
            scorm = Scorm.objects.get(pk=response.data['id'])
            return {
                'id': response.data['id'],
                'block_id': None,
                'course_id': scorm.course_id,
                'name': scorm.name,
                'description': scorm.description,
                'is_active': scorm.is_active,
                'max_attempts': scorm.max_attempts,
                'current_file': None,
                'estimated_time': scorm.estimated_time,
                'created': response.data['created'],
                'modified': response.data['modified'],
            }

        new_scorm = ScormFactory.build(course=self.course)
        data = {
            'is_active': new_scorm.is_active,
            'name': new_scorm.name,
            'description': new_scorm.description,
            'course_id': self.course.id,
            'max_attempts': new_scorm.max_attempts,
        }
        self.create_request(url=url, data=data, expected=make_expected, num_queries=16)

    def test_create(self):
        assign_perm('courses.change_course', self.user, self.course)
        self.client.force_login(user=self.user)

        def make_expected(response):
            scorm = Scorm.objects.get(pk=response.data['id'])
            return {
                'id': response.data['id'],
                'block_id': None,
                'course_id': response.data['course_id'],
                'name': scorm.name,
                'description': scorm.description,
                'is_active': scorm.is_active,
                'max_attempts': scorm.max_attempts,
                'current_file': None,
                'estimated_time': scorm.estimated_time,
                'created': response.data['created'],
                'modified': response.data['modified'],
            }

        url = self.get_url()
        course_file = CourseFileFactory(course=self.course)

        new_scorm = ScormFactory.build(course=self.course)

        data = {
            'is_active': new_scorm.is_active,
            'name': new_scorm.name,
            'description': new_scorm.description,
            'course_id': self.course.id,
            'course_file_id': course_file.id,
            'max_attempts': new_scorm.max_attempts,
        }

        self.create_request(url=url, data=data, expected=make_expected, num_queries=22)

    def test_create_with_block(self):
        assign_perm('courses.change_course', self.user, self.course)
        self.client.force_login(user=self.user)
        block = CourseBlockFactory(course=self.course)

        def make_expected(response):
            scorm = Scorm.objects.get(pk=response.data['id'])
            return {
                'id': response.data['id'],
                'block_id': block.id,
                'course_id': response.data['course_id'],
                'name': scorm.name,
                'description': scorm.description,
                'is_active': scorm.is_active,
                'max_attempts': scorm.max_attempts,
                'current_file': None,
                'estimated_time': scorm.estimated_time,
                'created': response.data['created'],
                'modified': response.data['modified'],
            }

        url = self.get_url()
        course_file = CourseFileFactory(course=self.course)

        new_scorm = ScormFactory.build(course=self.course)

        data = {
            'is_active': new_scorm.is_active,
            'name': new_scorm.name,
            'description': new_scorm.description,
            'course_id': self.course.id,
            'course_file_id': course_file.id,
            'max_attempts': new_scorm.max_attempts,
            'block_id': block.id,
        }

        self.create_request(url=url, data=data, expected=make_expected, num_queries=26)

    def test_fail_create(self):
        scorm = ScormFactory(course=self.course)
        data = {
            'name': scorm.name,
            'description': scorm.description,
            'max_attempts': scorm.max_attempts,
            'course_id': self.course.id,
            'is_active': True,
        }

        assign_perm('courses.change_course', self.user, scorm.course)
        self.client.force_login(user=self.user)
        url = self.get_url()
        self.create_request(url=url, data=data, status_code=status.HTTP_400_BAD_REQUEST, num_queries=13)


class ScormLabViewSetDetailTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:scorm-detail'

    def setUp(self):
        self.user = LabUserFactory(is_staff=True)
        self.course = CourseFactory()
        self.scorm = ScormFactory(course=self.course)
        self.course_file = CourseFileFactory(course=self.course)
        self.scorm_file = ScormFileFactory(scorm=self.scorm, course_file=self.course_file)

    def test_url(self):
        self.assertURLNameEqual('scorm_modules/{}/', args=(self.scorm.id,), base_url=settings.LABAPI_BASE_URL)

    def build_expected(self, scorm):
        return {
            'id': self.scorm.id,
            'block_id': None,
            'course_id': self.course.id,
            'name': self.scorm.name,
            'description': self.scorm.description,
            'is_active': self.scorm.is_active,
            'max_attempts': self.scorm.max_attempts,
            'current_file': {
                'id': self.scorm_file.id,
                'filename': self.course_file.filename,
                'public_url': self.scorm_file.public_url,
                'scorm_status': self.scorm_file.scorm_status,
                'comment': self.scorm_file.comment,
                'error_messages': self.scorm_file.error_messages,
                'manifest': self.scorm_file.manifest,
                'resources': [{
                    'id': resource.id,
                    'resource_id': resource.resource_id,
                    'href': resource.href,
                    'created': serializers.DateTimeField().to_representation(resource.created),
                    'modified': serializers.DateTimeField().to_representation(resource.modified),
                } for resource in self.scorm_file.resources.all()],
                'created': serializers.DateTimeField().to_representation(self.scorm_file.created),
            },
            'estimated_time': self.scorm.estimated_time,
            'created': serializers.DateTimeField().to_representation(self.scorm.created),
            'modified': serializers.DateTimeField().to_representation(self.scorm.modified),
        }

    def test_detail(self):
        assign_perm('courses.view_course', self.user, self.scorm.course)
        self.client.force_login(user=self.user)
        expected = self.build_expected(self.scorm)
        url = self.get_url(self.scorm.id)
        self.detail_request(url=url, expected=expected, num_queries=9)

    def test_update(self):
        new_scorm = ScormFactory.build(course=self.course)

        data = {
            'name': new_scorm.name,
            'description': new_scorm.description,
            'max_attempts': new_scorm.max_attempts,
        }

        def make_expected(response):
            return {
                'id': self.scorm.id,
                'block_id': None,
                'course_id': self.scorm.course_id,
                'name': new_scorm.name,
                'description': new_scorm.description,
                'is_active': self.scorm.is_active,
                'max_attempts': self.scorm.max_attempts,
                'current_file': {
                    'id': self.scorm_file.id,
                    'filename': self.course_file.filename,
                    'public_url': self.scorm_file.public_url,
                    'scorm_status': self.scorm_file.scorm_status,
                    'comment': self.scorm_file.comment,
                    'error_messages': self.scorm_file.error_messages,
                    'manifest': self.scorm_file.manifest,
                    'resources': [{
                        'id': resource.id,
                        'resource_id': resource.resource_id,
                        'href': resource.href,
                        'created': serializers.DateTimeField().to_representation(resource.created),
                        'modified': serializers.DateTimeField().to_representation(resource.modified),
                    } for resource in self.scorm_file.resources.all()],
                    'created': serializers.DateTimeField().to_representation(self.scorm_file.created),
                },
                'estimated_time': self.scorm.estimated_time,
                'created': serializers.DateTimeField().to_representation(self.scorm.created),
                'modified': response.data['modified'],
            }

        assign_perm('courses.change_course', self.user, self.scorm.course)
        self.client.force_login(user=self.user)
        url = self.get_url(self.scorm.id)
        self.update_request(url=url, data=data, expected=make_expected, num_queries=18)

    def test_add_block(self):
        block = CourseBlockFactory(course=self.course)

        data = {
            'block_id': block.id,
        }

        def make_expected(response):
            return {
                'id': self.scorm.id,
                'block_id': block.id,
                'course_id': self.scorm.course_id,
                'name': self.scorm.name,
                'description': self.scorm.description,
                'is_active': self.scorm.is_active,
                'max_attempts': self.scorm.max_attempts,
                'current_file': {
                    'id': self.scorm_file.id,
                    'filename': self.course_file.filename,
                    'public_url': self.scorm_file.public_url,
                    'scorm_status': self.scorm_file.scorm_status,
                    'comment': self.scorm_file.comment,
                    'error_messages': self.scorm_file.error_messages,
                    'manifest': self.scorm_file.manifest,
                    'resources': [{
                        'id': resource.id,
                        'resource_id': resource.resource_id,
                        'href': resource.href,
                        'created': serializers.DateTimeField().to_representation(resource.created),
                        'modified': serializers.DateTimeField().to_representation(resource.modified),
                    } for resource in self.scorm_file.resources.all()],
                    'created': serializers.DateTimeField().to_representation(self.scorm_file.created),
                },
                'estimated_time': self.scorm.estimated_time,
                'created': serializers.DateTimeField().to_representation(self.scorm.created),
                'modified': response.data['modified'],
            }

        assign_perm('courses.change_course', self.user, self.scorm.course)
        self.client.force_login(user=self.user)
        url = self.get_url(self.scorm.id)
        self.partial_update_request(url=url, data=data, expected=make_expected, num_queries=22)

    def test_remove_block(self):
        self.scorm.block = CourseBlockFactory(course=self.course)

        data = {
            'block_id': None,
        }

        def make_expected(response):
            return {
                'id': self.scorm.id,
                'block_id': None,
                'course_id': self.scorm.course_id,
                'name': self.scorm.name,
                'description': self.scorm.description,
                'is_active': self.scorm.is_active,
                'max_attempts': self.scorm.max_attempts,
                'current_file': {
                    'id': self.scorm_file.id,
                    'filename': self.course_file.filename,
                    'public_url': self.scorm_file.public_url,
                    'scorm_status': self.scorm_file.scorm_status,
                    'comment': self.scorm_file.comment,
                    'error_messages': self.scorm_file.error_messages,
                    'manifest': self.scorm_file.manifest,
                    'resources': [{
                        'id': resource.id,
                        'resource_id': resource.resource_id,
                        'href': resource.href,
                        'created': serializers.DateTimeField().to_representation(resource.created),
                        'modified': serializers.DateTimeField().to_representation(resource.modified),
                    } for resource in self.scorm_file.resources.all()],
                    'created': serializers.DateTimeField().to_representation(self.scorm_file.created),
                },
                'estimated_time': self.scorm.estimated_time,
                'created': serializers.DateTimeField().to_representation(self.scorm.created),
                'modified': response.data['modified'],
            }

        assign_perm('courses.change_course', self.user, self.scorm.course)
        self.client.force_login(user=self.user)
        url = self.get_url(self.scorm.id)
        self.partial_update_request(url=url, data=data, expected=make_expected, num_queries=18)

    def test_fail_update(self):
        scorm = ScormFactory(course=self.course)
        course_file = CourseFileFactory(course=self.course)
        ScormFileFactory(scorm=scorm, course_file=course_file,
                         scorm_status=ScormFile.SCORM_MODULE_STATUS_PENDING)
        data = {
            'name': scorm.name,
            'description': scorm.description,
            'max_attempts': scorm.max_attempts,
            'is_active': True,
        }

        assign_perm('courses.change_course', self.user, scorm.course)
        self.client.force_login(user=self.user)
        url = self.get_url(scorm.id)
        self.update_request(url=url, data=data, status_code=status.HTTP_400_BAD_REQUEST, num_queries=10)

    def test_delete(self):
        assign_perm('courses.change_course', self.user, self.scorm.course)
        self.client.force_login(user=self.user)
        url = self.get_url(self.scorm.id)
        self.delete_request(url=url, num_queries=20)


class ScormFileLabViewSetListTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:scorm-file-list'

    def setUp(self):
        self.user = LabUserFactory(is_staff=True)
        self.course = CourseFactory()
        self.scorm = ScormFactory(course=self.course)

    def test_url(self):
        self.assertURLNameEqual('scorm_modules/{}/files/', args=(self.scorm.id,), base_url=settings.LABAPI_BASE_URL)

    def build_expected(self, scorm_files):
        return [
            {
                'id': scorm_file.id,
                'filename': scorm_file.course_file.filename,
                'public_url': scorm_file.public_url,
                'scorm_status': scorm_file.scorm_status,
                'comment': scorm_file.comment,
                'error_messages': scorm_file.error_messages,
                'manifest': scorm_file.manifest,
                'resources': [{
                    'id': resource.id,
                    'resource_id': resource.resource_id,
                    'href': resource.href,
                    'created': serializers.DateTimeField().to_representation(resource.created),
                    'modified': serializers.DateTimeField().to_representation(resource.modified),
                } for resource in scorm_file.resources.all()],
                'created': serializers.DateTimeField().to_representation(scorm_file.created),
            } for scorm_file in sorted(scorm_files, key=lambda file: file.id)
        ]

    def test_list(self):
        assign_perm('courses.view_course', self.user, self.scorm.course)
        self.client.force_login(user=self.user)

        course_files = CourseFileFactory.create_batch(10, course=self.course)
        scorm_files = [
            ScormFileFactory(scorm=self.scorm, course_file=course_file)
            for course_file in course_files
        ]
        expected = self.build_expected(scorm_files)
        url = self.get_url(self.scorm.id)

        self.list_request(url=url, expected=expected, num_queries=11)


class CourseFileLabViewSetCreateTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:course-file-create'

    def setUp(self):
        self.user = LabUserFactory(is_staff=True)
        self.course = CourseFactory()

    def test_url(self):
        self.assertURLNameEqual('course_files/', base_url=settings.LABAPI_BASE_URL)

    def test_permission_denied(self):
        self.client.force_login(user=self.user)
        data = {
            'filename': 'scorm_file.zip',
            'course_id': self.course.id
        }
        with self.assertNumQueries(8):
            response = self.client.post(self.get_url(), data=data)
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_create(self):
        assign_perm('courses.change_course', self.user, self.course)
        self.client.force_login(user=self.user)

        data = {
            'filename': 'scorm_file.zip',
            'course_id': self.course.id
        }

        def mock_signed_url(path):
            return f"https://file.storage/bucket/{path}"

        def mock_upload_destination(filename):
            return f"files/a/b/hash/{filename}"

        def make_expected(response):
            course_file = CourseFile.objects.get(pk=response.data['course_file_id'])
            url = mock_upload_destination(course_file.filename)
            self.assertEqual(course_file.file, url)
            return {
                'course_file_id': response.data['course_file_id'],
                'url': mock_signed_url(url)
            }

        with patch('lms.courses.serializers.course_file.s3_boto_client.generate_presigned_url', mock_signed_url),\
             patch('lms.courses.models.files_upload_destination', mock_upload_destination):
            self.create_request(url=self.get_url(), data=data, expected=make_expected, num_queries=10)
