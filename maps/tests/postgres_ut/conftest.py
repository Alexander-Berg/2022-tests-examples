import pytest
from typing import Iterator

from maps.b2bgeo.ya_courier.analytics_backend.test_lib.local_pg import create_local_pg


@pytest.fixture
def pg_instance() -> Iterator[dict[str, str]]:
    with create_local_pg() as config:
        yield config
