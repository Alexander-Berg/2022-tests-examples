import pytest

from pay.sprint.sprint.tests.common_conftest import *  # noqa
from pay.sprint.sprint.tests.db import *  # noqa


@pytest.fixture
def db_engine(mocked_db_engine):
    return mocked_db_engine
