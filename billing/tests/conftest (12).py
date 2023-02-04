import os

import django
import pytest
from django.conf import settings
from ift.core.models import User

try:
    import library.python
    import pkgutil
    django.setup()  # Аркадийный pytest-django на этом моменте ещё не сконфигурировал Django.

    ARCADIA_RUN = True

except ImportError:
    ARCADIA_RUN = False

try:
    from envbox import get_environment
    environ = get_environment()

except ImportError:
    environ = os.environ


@pytest.fixture(autouse=True)
def db_access_on(django_db_reset_sequences,):
    """Используем бд в тестах без поимённого маркирования."""

@pytest.fixture
def init_user():
    """Создаёт объект пользователя и связанные с ним."""

    def init_user_(username=None, *, robot=False, **kwargs) -> User:

        username = username or settings.TEST_USERNAME

        if robot:
            username = settings.ROBOT_NAME

        user = User(
            username=username,
            **kwargs
        )
        pwd = 'testpassword'
        user.set_password(pwd)
        user.testpassword = pwd
        user.save()

        return user

    return init_user_
