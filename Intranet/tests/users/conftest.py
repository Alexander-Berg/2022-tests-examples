import pytest


@pytest.fixture
def dummy_yauser(request):
    class DummyYauser:
        uid = '1'
        login = 'smosker'
        first_name = 'first_name'
        last_name = 'last_name'
    try:
        request.cls.dummy_yauser = DummyYauser
    except AttributeError:
        return DummyYauser


@pytest.fixture
def dummy_yauser2(request):
    class DummyYauser:
        uid = '2'
        login = 'smosker_1'
        first_name = 'first_name'
        last_name = 'last_name'
    try:
        request.cls.dummy_yauser2 = DummyYauser
    except AttributeError:
        return DummyYauser
