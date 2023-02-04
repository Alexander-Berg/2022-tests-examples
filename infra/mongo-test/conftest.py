import pytest

from . mock import Database


@pytest.fixture
def db():
    """pytest fixture for database mocking."""

    db = Database()
    yield db.connection
    db.close()
