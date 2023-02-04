import pytest

from django.utils.translation import override

from staff.lib.testing import StaffFactory
from staff.person.tests.factories import ResponsibleForRobotFactory


@pytest.mark.django_db()
def test_get_responsible_for_robot():
    from staff.person_profile.views.responsible_for_robot_view import get_responsible_for_robot

    robot = StaffFactory(login='robot', is_robot=True)
    another_robot = StaffFactory(login='another_robot', is_robot=True)
    owner = StaffFactory(
        login='login_owner',
        first_name='Владелец',
        last_name='Владельцев',
    )
    user = StaffFactory(
        login='login_user',
        first_name='Пользователь',
        last_name='Пользователев',
    )
    alien = StaffFactory(login='alien')

    ResponsibleForRobotFactory(responsible=owner, robot=robot, role='owner')
    ResponsibleForRobotFactory(responsible=user, robot=robot, role='user')
    ResponsibleForRobotFactory(responsible=alien, robot=another_robot, role='owner')

    with override('ru'):
        result = get_responsible_for_robot('robot')

    assert result == {
        'owner': [{'first_name': 'Владелец', 'last_name': 'Владельцев', 'login': 'login_owner'}],
        'user': [{'first_name': 'Пользователь', 'last_name': 'Пользователев', 'login': 'login_user'}],
    }
