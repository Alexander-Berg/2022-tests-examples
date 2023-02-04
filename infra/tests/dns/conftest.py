import pytest

from infra.walle.server.tests.lib.util import TestCase


# TODO(rocco66): unified all that 'test' fixtures
@pytest.fixture
def test(request, monkeypatch_timestamp):
    return TestCase.create(request)


@pytest.fixture(autouse=True)
def database_for_tests(database):
    pass
