import pytest
from pytest_django.fixtures import _django_db_fixture_helper
# from pytest_django.django_compat import getfixturevalue


@pytest.fixture(scope='class')
def db_class(request, django_db_setup, django_db_blocker):
    """
    Аналог фикстуры `db` только для scope="class"
    Используется для фикстур со scope="class"
    """
    if ('transactional_db' in request.funcargnames
            or 'live_server' in request.funcargnames):
        request.getfixturevalue(request, 'transactional_db_class')
    else:
        _django_db_fixture_helper(False, request, django_db_blocker)


@pytest.fixture(scope='class')
def transactional_db_class(request, django_db_setup, django_db_blocker):
    """
    Аналог фикстуры `transactional_db` только для scope="class"
    Используется для фикстур со scope="class"
    """
    _django_db_fixture_helper(True, request, django_db_blocker)
