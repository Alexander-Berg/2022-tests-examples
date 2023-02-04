import pytest

from porto.api import Connection


@pytest.fixture(scope='session')
def portoconn():
    return Connection()
