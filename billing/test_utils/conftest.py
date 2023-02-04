import pytest

from billing.library.python.yql_utils.test_utils.utils import (
    create_yql_client,
)


__all__ = [
    'yql_client'
]


@pytest.fixture
def yql_client():
    return create_yql_client()
