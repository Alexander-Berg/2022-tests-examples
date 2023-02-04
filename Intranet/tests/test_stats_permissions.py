from builtins import object, str

import pytest
from mock import MagicMock

from kelvin.courses.models import CoursePermission, CourseStudent
from kelvin.courses.permissions import CanViewCuratorStats, CanViewStudentStats

from .lib import create_random_course, create_random_user


class TestCanViewCuratorStats(object):
    @pytest.mark.django_db
    def test_superuser_access(self):
        superuser = create_random_user()
        superuser.is_superuser = True

        first_course = create_random_course()
        second_course = create_random_course()

        request = MagicMock()

        request.GET = {
            'course_ids': ','.join([
                str(first_course.id),
                str(second_course.id),
            ])
        }

        request.user = superuser
        view = MagicMock()

        assert CanViewCuratorStats().has_permission(request, view)

    only_one_course_permission_cases = (
        CoursePermission.CURATOR,
        CoursePermission.CONTENT_MANAGER,
        CoursePermission.OWNER,
        CoursePermission.NONE,
    )

    @pytest.mark.django_db
    @pytest.mark.parametrize('course_permission', only_one_course_permission_cases)
    def test_only_one_course_permission(self, course_permission):
        curator = create_random_user()

        first_course = create_random_course()
        second_course = create_random_course()

        CoursePermission(
            course=first_course,
            user=curator,
            permission=course_permission
        ).save()

        request = MagicMock()

        request.GET = {
            'course_ids': ','.join([
                str(first_course.id),
                str(second_course.id),
            ])
        }

        request.user = curator
        view = MagicMock()

        assert CanViewCuratorStats().has_permission(request, view) is False

    two_courses_permission_cases = (
        (
            CoursePermission.REVIEWER,
            CoursePermission.OWNER,
            False,
        ),
        (
            CoursePermission.NONE,
            CoursePermission.OWNER,
            False,
        ),
        (
            CoursePermission.CURATOR,
            CoursePermission.REVIEWER,
            False,
        ),
        (
            CoursePermission.OWNER,
            CoursePermission.REVIEWER,
            False,
        ),

        (
            CoursePermission.CURATOR,
            CoursePermission.CURATOR,
            True,
        ),
        (
            CoursePermission.CURATOR,
            CoursePermission.OWNER,
            True,
        ),
        (
            CoursePermission.CURATOR,
            CoursePermission.CONTENT_MANAGER,
            True,
        ),
        (
            CoursePermission.CURATOR,
            CoursePermission.ANALYST,
            True,
        ),
    )

    @pytest.mark.django_db
    @pytest.mark.parametrize('first_permission,second_permission,expected', two_courses_permission_cases)
    def test_two_courses_permission(self, first_permission, second_permission, expected):
        curator = create_random_user()

        first_course = create_random_course()
        second_course = create_random_course()

        CoursePermission(
            course=first_course,
            user=curator,
            permission=first_permission,
        ).save()

        CoursePermission(
            course=second_course,
            user=curator,
            permission=second_permission,
        ).save()

        request = MagicMock()

        request.GET = {
            'course_ids': ','.join([
                str(first_course.id),
                str(second_course.id),
            ])
        }

        request.user = curator
        view = MagicMock()

        assert CanViewCuratorStats().has_permission(request, view) is expected

    @pytest.mark.django_db
    def test_non_existing_course(self):
        curator = create_random_user()

        course = create_random_course()

        CoursePermission(
            course=course,
            user=curator,
            permission=CoursePermission.CURATOR,
        ).save()

        request = MagicMock()

        request.GET = {
            'course_ids': ','.join([
                str(course.id),
                str(course.id + 1),
            ])
        }

        request.user = curator
        view = MagicMock()

        assert CanViewCuratorStats().has_permission(request, view) is False

    @pytest.mark.django_db
    def test_no_course_ids(self):
        user = create_random_user()

        request = MagicMock()

        request.GET = {}

        request.user = user
        view = MagicMock()

        assert CanViewCuratorStats().has_permission(request, view)


class TestCanViewStudentStats(object):
    @pytest.mark.django_db
    def test_two_joined_courses(self):
        student = create_random_user()

        first_course = create_random_course()
        second_course = create_random_course()

        CourseStudent(student=student, course=first_course).save()
        CourseStudent(student=student, course=second_course).save()

        request = MagicMock()

        request.GET = {
            'course_ids': ','.join([
                str(first_course.id),
                str(second_course.id),
            ])
        }

        request.user = student
        view = MagicMock()

        assert CanViewStudentStats().has_permission(request, view)

    @pytest.mark.django_db
    def test_one_joined_course(self):
        student = create_random_user()

        first_course = create_random_course()
        second_course = create_random_course()

        CourseStudent(student=student, course=first_course).save()

        request = MagicMock()

        request.GET = {
            'course_ids': ','.join([
                str(first_course.id),
                str(second_course.id),
            ])
        }

        request.user = student
        view = MagicMock()

        assert CanViewStudentStats().has_permission(request, view) is False

    @pytest.mark.django_db
    def test_one_deleted_course(self):
        student = create_random_user()

        first_course = create_random_course()
        second_course = create_random_course()

        CourseStudent(student=student, course=first_course).save()
        CourseStudent(student=student, course=second_course, deleted=True).save()

        request = MagicMock()

        request.GET = {
            'course_ids': ','.join([
                str(first_course.id),
                str(second_course.id),
            ])
        }

        request.user = student
        view = MagicMock()

        assert CanViewStudentStats().has_permission(request, view)

    @pytest.mark.django_db
    def test_one_curator_course(self):
        student = create_random_user()

        first_course = create_random_course()
        second_course = create_random_course()

        CourseStudent(student=student, course=first_course).save()
        CoursePermission(user=student, course=second_course, permission=CoursePermission.CURATOR).save()

        request = MagicMock()

        request.GET = {
            'course_ids': ','.join([
                str(first_course.id),
                str(second_course.id),
            ])
        }

        request.user = student
        view = MagicMock()

        assert CanViewStudentStats().has_permission(request, view) is False

    @pytest.mark.django_db
    def test_non_existing_course(self):
        student = create_random_user()

        course = create_random_course()

        CourseStudent(student=student, course=course).save()

        request = MagicMock()

        request.GET = {
            'course_ids': ','.join([
                str(course.id),
                str(course.id + 1),
            ])
        }

        request.user = student
        view = MagicMock()

        assert CanViewStudentStats().has_permission(request, view) is False

    @pytest.mark.django_db
    def test_no_course_ids(self):
        user = create_random_user()

        request = MagicMock()

        request.GET = {}

        request.user = user
        view = MagicMock()

        assert CanViewCuratorStats().has_permission(request, view)
