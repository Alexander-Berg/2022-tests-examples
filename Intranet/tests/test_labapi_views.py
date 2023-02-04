import faker
from guardian.shortcuts import assign_perm

from django.conf import settings

from rest_framework import serializers, status
from rest_framework.test import APITestCase

from lms.core.tests.mixins import GenericRequestMixin, UrlNameMixin
from lms.moduletypes.models import ModuleType
from lms.users.tests.factories import LabUserFactory

from ..models import Assignment
from .factories import AssignmentFactory, AssignmentStudentResultFactory, CourseFactory

fake = faker.Faker()


class AssignmentLabViewSetCreateTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:assignment-create'

    def setUp(self):
        ModuleType.objects.get_for_model(Assignment)
        self.user = LabUserFactory(is_staff=True)
        self.course = CourseFactory()

    def build_payload(self, assignment: Assignment):
        return {
            "course_id": self.course.id,
            "name": assignment.name,
            "description": assignment.description,
            "is_active": assignment.is_active,
            "estimated_time": assignment.estimated_time,
        }

    def test_url(self):
        self.assertURLNameEqual('assignment_modules/', args=(), base_url=settings.LABAPI_BASE_URL)

    def test_permission_denied(self):
        self.client.force_login(user=self.user)
        assignment = AssignmentFactory.build(course=self.course)
        self.create_request(
            self.get_url(),
            data=self.build_payload(assignment),
            status_code=status.HTTP_403_FORBIDDEN, num_queries=8
        )

    def test_create(self):
        assign_perm('courses.change_course', self.user, self.course)
        self.client.force_login(user=self.user)

        assignment = AssignmentFactory.build(course=self.course)

        def expected(response):
            return {
                'id': response.data['id'],
                'block_id': None,
                'name': assignment.name,
                'description': assignment.description,
                'is_active': assignment.is_active,
                'estimated_time': assignment.estimated_time,
                'created': serializers.DateTimeField().to_representation(response.data['created']),
                'modified': serializers.DateTimeField().to_representation(response.data['modified']),
            }

        self.create_request(self.get_url(), data=self.build_payload(assignment), expected=expected, num_queries=13)


class AssignmentLabViewSetDetailTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:assignment-detail'

    def setUp(self):
        ModuleType.objects.get_for_model(Assignment)
        self.user = LabUserFactory(is_staff=True)
        self.course = CourseFactory()
        self.assignment = AssignmentFactory(course=self.course)

    def build_payload(self, assignment: Assignment):
        return {
            "course_id": self.course.id,
            "name": assignment.name,
            "description": assignment.description,
            "is_active": assignment.is_active,
            "estimated_time": assignment.estimated_time,
        }

    def test_url(self):
        self.assertURLNameEqual(
            'assignment_modules/{}/',
            args=(self.assignment.id,),
            base_url=settings.LABAPI_BASE_URL
        )

    def test_permission_denied(self):
        self.client.force_login(user=self.user)
        self.detail_request(
            self.get_url(self.assignment.id),
            status_code=status.HTTP_403_FORBIDDEN,
            num_queries=9
        )

    def test_detail(self):
        assign_perm('courses.view_course', self.user, self.course)
        self.client.force_login(user=self.user)

        expected = {
            'id': self.assignment.id,
            'block_id': None,
            'name': self.assignment.name,
            'description': self.assignment.description,
            'is_active': self.assignment.is_active,
            'estimated_time': self.assignment.estimated_time,
            'created': serializers.DateTimeField().to_representation(self.assignment.created),
            'modified': serializers.DateTimeField().to_representation(self.assignment.modified),
        }

        self.detail_request(
            self.get_url(self.assignment.id),
            expected=expected,
            num_queries=9
        )

    def test_update(self):
        assign_perm('courses.change_course', self.user, self.course)
        self.client.force_login(user=self.user)

        new_assignment = AssignmentFactory.build()
        request_payload = self.build_payload(new_assignment)
        request_payload.pop('course_id')

        def expected(response):
            return {
                'id': self.assignment.id,
                'block_id': None,
                'name': new_assignment.name,
                'description': new_assignment.description,
                'is_active': new_assignment.is_active,
                'estimated_time': new_assignment.estimated_time,
                'created': serializers.DateTimeField().to_representation(self.assignment.created),
                'modified': serializers.DateTimeField().to_representation(response.data['modified']),
            }

        self.update_request(
            self.get_url(self.assignment.id),
            data=request_payload,
            expected=expected,
            num_queries=14
        )

    def test_delete_with_results(self):
        assign_perm('courses.change_course', self.user, self.course)
        self.client.force_login(user=self.user)

        AssignmentStudentResultFactory(assignment=self.assignment)

        self.delete_request(
            self.get_url(self.assignment.id),
            status_code=status.HTTP_400_BAD_REQUEST,
            num_queries=10
        )

    def test_delete(self):
        assign_perm('courses.change_course', self.user, self.course)
        self.client.force_login(user=self.user)

        self.delete_request(
            self.get_url(self.assignment.id),
            num_queries=15
        )
