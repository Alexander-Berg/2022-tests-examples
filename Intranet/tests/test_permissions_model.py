import random
from builtins import object

import pytest

from kelvin.accounts.models import User
from kelvin.courses.models import Course, CoursePermission
from kelvin.subjects.models import Subject

from .lib import create_random_course


class TestCoursePermission(object):
    """
    Тесты для проверки методов модели CoursePermission
    """
    @pytest.mark.django_db
    def test_get_actions_dict_no_roles(self, simple_user, simple_course):
        course_permission = CoursePermission(
            course=simple_course,
            user=simple_user,
            permission=CoursePermission.NONE
        )
        course_permission.save()

        actions_dict = course_permission.get_actions_dict()

        assert actions_dict == {'edit': 0, 'roles': 0, 'stats': 0, 'view': 0, 'review': 0}

    @pytest.mark.django_db
    def test_get_actions_dict_content_manager_only(self, simple_user, simple_course):
        course_permission = CoursePermission(
            course=simple_course,
            user=simple_user,
            permission=CoursePermission.CONTENT_MANAGER
        )
        course_permission.save()

        actions_dict = course_permission.get_actions_dict()

        assert actions_dict == {'edit': 1, 'roles': 0, 'stats': 1, 'view': 1, 'review': 1}

    @pytest.mark.django_db
    def test_get_actions_dict_analyst_only(self, simple_user, simple_course):
        course_permission = CoursePermission(
            course=simple_course,
            user=simple_user,
            permission=CoursePermission.ANALYST
        )
        course_permission.save()

        actions_dict = course_permission.get_actions_dict()

        assert actions_dict == {'edit': 0, 'roles': 0, 'stats': 1, 'view': 1, 'review': 0}

    @pytest.mark.django_db
    def test_get_actions_dict_owner_only(self, simple_user, simple_course):
        course_permission = CoursePermission(
            course=simple_course,
            user=simple_user,
            permission=CoursePermission.OWNER
        )
        course_permission.save()

        actions_dict = course_permission.get_actions_dict()

        assert actions_dict == {'edit': 1, 'roles': 1, 'stats': 1, 'view': 1, 'review': 1}

    @pytest.mark.django_db
    def test_get_actions_dict_superuser(self, simple_user, simple_course):
        simple_user.is_superuser = True
        simple_user.save()

        course_permission = CoursePermission(
            course=simple_course,
            user=simple_user,
            permission=CoursePermission.NONE
        )
        course_permission.save()

        actions_dict = course_permission.get_actions_dict()

        assert actions_dict == {'edit': 1, 'roles': 1, 'stats': 1, 'view': 1, 'review': 1}

    @pytest.mark.django_db
    def test_get_actions_dict_reviewer(self, simple_user, simple_course):
        simple_user.is_superuser = False
        simple_user.save()

        course_permission = CoursePermission(
            course=simple_course,
            user=simple_user,
            permission=CoursePermission.REVIEWER
        )
        course_permission.save()

        actions_dict = course_permission.get_actions_dict()

        assert actions_dict == {'edit': 0, 'roles': 0, 'stats': 0, 'view': 1, 'review': 1}

    @pytest.mark.django_db
    def test_get_actions_dict_reviewer_plus_analyst(self, simple_user, simple_course):
        simple_user.is_superuser = False
        simple_user.save()

        course_permission = CoursePermission(
            course=simple_course,
            user=simple_user,
            permission=CoursePermission.REVIEWER | CoursePermission.ANALYST
        )
        course_permission.save()

        actions_dict = course_permission.get_actions_dict()

        assert actions_dict == {'edit': 0, 'roles': 0, 'stats': 1, 'view': 1, 'review': 1}

    @pytest.mark.django_db
    def test_get_actions_dict_analyst_plus_content_manager(self, simple_user, simple_course):
        course_permission = CoursePermission(
            course=simple_course,
            user=simple_user,
            permission=CoursePermission.ANALYST | CoursePermission.CONTENT_MANAGER
        )
        course_permission.save()

        actions_dict = course_permission.get_actions_dict()

        assert actions_dict == {'edit': 1, 'roles': 0, 'stats': 1, 'view': 1, 'review': 1}

    @pytest.mark.django_db
    def test_owner_course_permission_auto_create(self, simple_user):
        course = create_random_course()
        user = course.owner
        course_permission = CoursePermission.objects.get(user=user, course=course)
        assert(course_permission.permission & CoursePermission.OWNER)

    @pytest.mark.django_db
    def test_user_is_curator_counting(self, simple_user):
        assert simple_user.is_curator is False

        first_course = create_random_course()
        second_course = create_random_course()

        first_course_permission = CoursePermission(
            user=simple_user,
            course=first_course,
            permission=CoursePermission.CURATOR,
        )
        first_course_permission.save()

        simple_user.refresh_from_db()
        assert simple_user.is_curator

        second_course_permission = CoursePermission(
            user=simple_user,
            course=second_course,
            permission=CoursePermission.CURATOR | CoursePermission.CONTENT_MANAGER,
        )
        second_course_permission.save()

        first_course_permission.delete()

        simple_user.refresh_from_db()
        assert simple_user.is_curator

        second_course_permission.delete()

        simple_user.refresh_from_db()
        assert simple_user.is_curator is False
