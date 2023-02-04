from guardian.shortcuts import assign_perm

from django.contrib.contenttypes.models import ContentType
from django.test import TestCase

from lms.users.tests.factories import GroupFactory, PermissionFactory, PermissionPresetFactory, UserFactory

from ..models import Course
from .factories import CourseFactory, CourseTeamFactory


class CourseModelPermissionTestCase(TestCase):
    def setUp(self):
        self.course_content_type = ContentType.objects.get_for_model(Course)
        self.course = CourseFactory()
        self.preset = PermissionPresetFactory()
        self.team = CourseTeamFactory(permission_preset=self.preset)
        self.permissions = PermissionFactory.create_batch(4, content_type=self.course_content_type)
        self.preset.permissions.add(*self.permissions)
        self.user = UserFactory()
        self.user.groups.add(self.team)

    def test_add_team(self):
        assign_perm(self.permissions[0], self.team, self.course)
        with self.assertNumQueries(5):
            self.course.teams.add(self.team)

        for permission in self.permissions:
            self.assertTrue(self.user.has_perm(permission.codename, self.course))

    def test_remove_team(self):
        self.course.teams.add(self.team)
        preset2 = PermissionPresetFactory()
        permissions2 = PermissionFactory.create_batch(3, content_type=self.course_content_type)
        preset2.permissions.add(*permissions2)
        team2 = CourseTeamFactory(permission_preset=preset2)
        self.course.teams.add(team2)
        self.user.groups.add(team2)

        with self.assertNumQueries(2):
            self.course.teams.remove(self.team)

        for permission in self.permissions:
            self.assertFalse(self.user.has_perm(permission.codename, self.course))

        for permission in permissions2:
            self.assertTrue(self.user.has_perm(permission.codename, self.course))

    def test_remove_team_but_in_other_team(self):
        team2 = CourseTeamFactory(permission_preset=self.preset)
        self.course.teams.add(self.team, team2)
        self.user.groups.add(team2)

        with self.assertNumQueries(2):
            self.course.teams.remove(self.team)

        for permission in self.permissions:
            self.assertTrue(self.user.has_perm(permission.codename, self.course))

    def test_remove_team_but_in_other_preset(self):
        preset2 = PermissionPresetFactory()
        preset2.permissions.add(*self.permissions[:2])
        team2 = CourseTeamFactory(permission_preset=preset2)
        self.course.teams.add(self.team, team2)
        self.user.groups.add(team2)

        with self.assertNumQueries(2):
            self.course.teams.remove(self.team)

        for permission in self.permissions[:2]:
            self.assertTrue(self.user.has_perm(permission.codename, self.course))
        for permission in self.permissions[3:]:
            self.assertFalse(self.user.has_perm(permission.codename, self.course))

    def test_clear_teams(self):
        preset2 = PermissionPresetFactory()
        preset2.permissions.add(*self.permissions[:2])
        team2 = CourseTeamFactory(permission_preset=preset2)
        self.course.teams.add(self.team, team2)
        other_group = GroupFactory()
        other_permission = PermissionFactory(content_type=self.course_content_type)
        assign_perm(other_permission, other_group, self.course)
        self.user.groups.add(team2, other_group)

        with self.assertNumQueries(3):
            self.course.teams.clear()

        for permission in self.permissions:
            self.assertFalse(self.user.has_perm(permission.codename, self.course))

        self.assertTrue(self.user.has_perm(other_permission.codename, self.course))
