from builtins import object

import pytest
from mock import MagicMock

from kelvin.courses.models import CoursePermission
from kelvin.courses.serializers.course_permission import CoursePermissionSerializer

from .lib import create_random_course


class TestCoursePermissionSerializer(object):
    """
    Тесты класса CoursePermissionSerializer
    """

    @pytest.mark.django_db
    def test_serializing_single_role(self, simple_user, simple_course):
        """
        Проверяем сохранение одной роли
        """
        for role in ['owner', 'content_manager', 'analyst', 'curator']:
            course_permission = CoursePermission(
                course=simple_course,
                user=simple_user,
                permission=0
            )

            setattr(course_permission, 'is_{}'.format(role), True)

            data = CoursePermissionSerializer(
                context={'request': MagicMock()}
            ).to_representation(course_permission)

            assert data['roles'] == [role]
