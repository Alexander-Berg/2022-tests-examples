from guardian.shortcuts import assign_perm

from django.contrib.auth.models import Permission

from rest_framework import status
from rest_framework.test import APITestCase

from lms.core.tests.mixins import UrlNameMixin
from lms.users.tests.factories import GroupFactory, LabUserFactory, PermissionPresetFactory

from .factories import CourseFactory, CourseTeamFactory


class LabCourseDetailPermissionTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:course-detail'

    def test_get(self):
        user = LabUserFactory()
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

        self.client.force_login(user=user)

        with self.assertNumQueries(7):
            response = self.client.get(self.get_url(course.id), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
