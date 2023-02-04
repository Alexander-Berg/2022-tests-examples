from guardian.shortcuts import assign_perm

from django.conf import settings
from django.contrib.contenttypes.models import ContentType

from rest_framework import status
from rest_framework.test import APITestCase

from lms.core.tests.mixins import UrlNameMixin
from lms.users.tests.factories import GroupFactory, PermissionFactory, UserFactory

from ..models import Course
from .factories import CourseFactory


class CoursePermissionTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'api:my-course-permissions'

    def test_url(self):
        self.assertURLNameEqual('my/courses/{}/permissions/', args=(1,), base_url=settings.API_BASE_URL)

    def test_list(self):
        user = UserFactory(is_active=True)
        group = GroupFactory()
        course = CourseFactory()
        user.groups.add(group)
        course_content_type = ContentType.objects.get_for_model(Course)
        user_permissions = PermissionFactory.create_batch(3, content_type=course_content_type)
        group_permissions = PermissionFactory.create_batch(3, content_type=course_content_type)
        common_permissions = PermissionFactory.create_batch(3, content_type=course_content_type)
        for permission in user_permissions + common_permissions:
            assign_perm(permission, user, course)
        for permission in group_permissions + common_permissions:
            assign_perm(permission, group, course)

        self.client.force_login(user=user)
        with self.assertNumQueries(5):
            response = self.client.get(self.get_url(course.id), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        data = response.data
        expected = {perm.codename for perm in set(user_permissions + group_permissions + common_permissions)}
        results = set(data)
        self.assertSetEqual(expected, results)
