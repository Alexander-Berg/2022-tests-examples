import pytest

from .lib import create_random_course, create_random_user


@pytest.fixture()
def simple_user():
    return create_random_user()


@pytest.fixture()
def simple_course():
    return create_random_course()
