import pytest

from pay.sprint.sprint.tests.common_conftest import *  # noqa
from pay.sprint.sprint.tests.db import *  # noqa


@pytest.fixture
def db_engine(raw_db_engine):
    return raw_db_engine
