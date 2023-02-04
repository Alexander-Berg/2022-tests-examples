from guardian.shortcuts import assign_perm

from django.contrib.contenttypes.models import ContentType
from django.test import TestCase

from lms.staff.tests.factories import StaffCityFactory, StaffOfficeFactory, UserWithStaffProfileFactory
from lms.users.tests.factories import GroupFactory, PermissionFactory, UserFactory

from ..models import Course, CourseVisibility
from .factories import CourseFactory, CourseVisibilityFactory


class ActiveCoursesQuerysetTestCase(TestCase):
    def setUp(self) -> None:
        self.inactive_course = CourseFactory()
        self.active_course = CourseFactory(is_active=True)
        self.archived_course = CourseFactory(is_archive=True, is_active=True)

        self.user = UserFactory()
        self.group = GroupFactory()
        self.user.groups.add(self.group)
        self.user_course = CourseFactory(author=self.user)
        self.course_with_user_permission = CourseFactory()
        self.course_with_group_permission = CourseFactory()
        assign_perm("courses.view_course", self.user, self.course_with_user_permission)
        assign_perm(
            "courses.view_course", self.group, self.course_with_group_permission
        )

        self.another_user = UserFactory()
        self.another_group = GroupFactory()
        self.another_user.groups.add(self.another_group)
        self.another_user_course = CourseFactory(author=self.another_user)
        self.course_with_another_user_permission = CourseFactory()
        self.course_with_another_group_permission = CourseFactory()
        assign_perm(
            "courses.view_course",
            self.another_user,
            self.course_with_another_user_permission,
        )
        assign_perm(
            "courses.view_course",
            self.another_group,
            self.course_with_another_group_permission,
        )

    def build_expected(self, courses):
        return sorted(courses, key=lambda x: x.name)

    def test_not_preview(self):
        result = Course.objects.active(user=self.user, preview=False)

        expected = self.build_expected([self.active_course])

        self.assertEqual(list(result), expected)

    def test_preview(self):
        result = Course.objects.active(user=self.user, preview=True)
        expected = self.build_expected(
            [
                self.active_course,
                self.user_course,
                self.course_with_user_permission,
                self.course_with_group_permission,
            ]
        )

        self.assertEqual(list(result), expected)

    def test_preview_with_view_permission(self):
        assign_perm("courses.view_course", self.user)

        result = Course.objects.active(user=self.user, preview=True)
        expected = self.build_expected(
            [
                self.inactive_course,
                self.active_course,
                self.user_course,
                self.course_with_user_permission,
                self.course_with_group_permission,
                self.another_user_course,
                self.course_with_another_user_permission,
                self.course_with_another_group_permission,
            ]
        )

        self.assertEqual(list(result), expected)


class PermittedCoursesQuerysetTestCase(TestCase):
    def test_permitted_by_permissions(self):
        course_content_type = ContentType.objects.get_for_model(Course)
        user = UserFactory()
        group = GroupFactory()
        user.groups.add(group)
        course, other_course = CourseFactory.create_batch(2, is_active=True)
        user_permission1, user_permission2 = PermissionFactory.create_batch(2, content_type=course_content_type)
        group_permission1, group_permission2 = PermissionFactory.create_batch(2, content_type=course_content_type)
        common_permission1, common_permission2 = PermissionFactory.create_batch(2, content_type=course_content_type)
        assign_perm(user_permission1, user, course)
        assign_perm(common_permission1, user, course)
        assign_perm(group_permission1, group, course)
        assign_perm(common_permission1, group, course)

        for permission in [user_permission1, group_permission1, common_permission1]:
            result = Course.objects.permitted(permission=permission, user=user)
            self.assertListEqual(list(result), [course])

        for permission in [user_permission2, group_permission2, common_permission2]:
            result = Course.objects.permitted(permission=permission, user=user)
            self.assertListEqual(list(result), [])

    def test_permitted_by_superuser(self):
        course_content_type = ContentType.objects.get_for_model(Course)
        user = UserFactory(is_superuser=True)
        course = CourseFactory(is_active=True)
        permission = PermissionFactory(content_type=course_content_type)

        result = Course.objects.permitted(permission=permission, user=user)
        self.assertListEqual(list(result), [course])

    def test_permitted_by_author(self):
        course_content_type = ContentType.objects.get_for_model(Course)
        user = UserFactory(is_superuser=True)
        course = CourseFactory(author=user)
        permission = PermissionFactory(content_type=course_content_type)

        result = Course.objects.permitted(permission=permission, user=user)
        self.assertListEqual(list(result), [course])


class UnavailableCoursesQuerysetTestCase(TestCase):
    def setUp(self):
        self.author = UserFactory(is_staff=True)
        self.courses_with_no_visibility_rules = CourseFactory.create_batch(4, is_active=True)
        self.inactive_courses = CourseFactory.create_batch(2, is_active=False)
        self.city = StaffCityFactory()
        self.office = StaffOfficeFactory(city=self.city)
        self.user = UserWithStaffProfileFactory(staffprofile__office=self.office)
        self.available_courses = CourseFactory.create_batch(3, is_active=True)
        [
            CourseVisibilityFactory(course=course, rules={"eq": ["staff_city", self.city.id]})
            for course in self.available_courses
        ]
        self.unavailable_courses = CourseFactory.create_batch(5, is_active=True)
        [
            CourseVisibilityFactory(course=course, rules={"ne": ["staff_city", self.city.id]})
            for course in self.unavailable_courses
        ]

    def test_list_unavailable_courses(self):
        unavailable_courses = CourseVisibility.objects.unavailable_for(self.user)
        expected = {course.id for course in self.unavailable_courses}
        self.assertEqual(unavailable_courses, expected)
