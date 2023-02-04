import pytest
from django.db.models import ObjectDoesNotExist

from dwh.core.models.user import ROLE_SUPPORT


def test_basic(init_user):

    user = init_user()
    assert not user.is_staff
    assert not user.is_support
    assert not user.is_robot
    assert not user.has_perm('a', None)

    user.roles[ROLE_SUPPORT] = {}
    assert user.is_support
    assert user.has_perm('a', None)

    with pytest.raises(ObjectDoesNotExist):
        user.get_robot()

    robot = init_user(robot=True)
    assert robot == user.get_robot()
