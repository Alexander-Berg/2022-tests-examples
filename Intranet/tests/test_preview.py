from guardian.shortcuts import assign_perm

from django.contrib.auth.models import Permission

from rest_framework import status
from rest_framework.test import APITestCase

from lms.core.tests.mixins import UrlNameMixin
from lms.users.tests.factories import GroupFactory, PermissionPresetFactory, UserFactory
from lms.utils.tests.test_decorators import parameterized_expand_doc

from .factories import CourseFactory, CourseTeamFactory

COURSE_AVAILABILITY_MAP = [
    (False, False, False, False, False, False, False, 3, False),
    (False, False, False, True, False, False, False, 3, False),
    (False, False, True, False, False, False, False, 5, True),
    (False, False, True, True, False, False, False, 5, True),
    (False, True, False, False, False, False, False, 5, False),
    (False, True, False, True, False, False, False, 7, True),
    (False, True, True, False, False, False, False, 7, True),
    (False, True, True, True, False, False, False, 7, True),
    (True, False, False, False, False, False, False, 3, False),
    (True, False, False, True, False, False, False, 3, False),
    (True, False, True, False, False, False, False, 5, True),
    (True, False, True, True, False, False, False, 5, True),
    (True, True, False, False, False, False, False, 5, True),
    (True, True, False, True, False, False, False, 5, True),
    (True, True, True, False, False, False, False, 5, True),
    (True, True, True, True, False, False, False, 5, True),
    # preview by permissions
    (False, True, False, False, True, False, False, 7, True),
    (False, True, False, False, False, True, False, 7, True),
    (False, True, False, False, False, False, True, 7, True),
    # no preview by permissions
    (False, False, False, False, True, False, False, 3, False),
    (False, False, False, False, False, True, False, 3, False),
    (False, False, False, False, False, False, True, 3, False),
]


class CourseDetailPreviewTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'api:course-detail'

    @parameterized_expand_doc(COURSE_AVAILABILITY_MAP)
    def test_detail(
        self,
        is_superuser: bool,
        is_preview: bool,
        is_course_active: bool,
        user_is_author: bool,
        user_has_permission: bool,
        user_has_group_permission: bool,
        user_in_course_team: bool,
        num_queries: int,
        has_result: bool,
    ):
        user = UserFactory(is_superuser=is_superuser)
        author = user if user_is_author else UserFactory()
        course = CourseFactory(author=author, is_active=is_course_active)
        url = self.get_url(course.id)
        if user_has_permission:
            assign_perm('courses.view_course', user, course)
        if user_has_group_permission:
            group = GroupFactory()
            group.user_set.add(user)
            assign_perm('courses.view_course', group, course)
        if user_in_course_team:
            preset = PermissionPresetFactory()
            permission = Permission.objects.get(codename='view_course')
            preset.permissions.add(permission)
            course_team = CourseTeamFactory(permission_preset=preset)
            course_team.add_user(user.id)
            course.teams.add(course_team)
            self.assertTrue(user.has_perm('courses.view_course', course))
        if is_preview:
            url += '?preview=true'

        self.client.force_login(user=user)
        with self.assertNumQueries(num_queries):
            response = self.client.get(url, format='json')

        expected_status = status.HTTP_200_OK if has_result else status.HTTP_404_NOT_FOUND
        self.assertEqual(response.status_code, expected_status)


class CourseDetailPreviewDuplicateTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'api:course-detail'

    def test_detail(self):
        user = UserFactory()
        course = CourseFactory(author=user, is_active=True)

        assign_perm('courses.view_course', user, course)
        assign_perm('courses.change_course', user, course)

        group = GroupFactory()
        group.user_set.add(user)
        assign_perm('courses.view_course', group, course)
        assign_perm('courses.change_course', group, course)

        preset = PermissionPresetFactory()
        permission1 = Permission.objects.get(codename='view_course')
        permission2 = Permission.objects.get(codename='change_course')
        preset.permissions.add(permission1, permission2)
        course_team = CourseTeamFactory(permission_preset=preset)
        course_team.add_user(user.id)
        course.teams.add(course_team)

        url = self.get_url(course.id) + '?preview=true'

        self.client.force_login(user=user)
        with self.assertNumQueries(7):
            response = self.client.get(url, format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
