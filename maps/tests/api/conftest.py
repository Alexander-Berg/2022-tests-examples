from datetime import datetime, timezone

import pytest


@pytest.fixture(autouse=True)
def yang_mock(yang):
    yang.create_task_suite.coro.return_value = {
        "id": "suite_id_1",
        "created_at": datetime.now(timezone.utc),
    }

    return yang


@pytest.fixture(autouse=True)
def common_geosearch_client_mocks(geosearch):
    geosearch.resolve_org.coro.return_value.name = "Название"
    geosearch.resolve_org.coro.return_value.categories_names = ["Кафе"]
    geosearch.resolve_org.coro.return_value.formatted_callable_phones = [
        "+7 (000) 000-00-01",
        "+7 (000) 000-00-02",
    ]
    geosearch.resolve_org.coro.return_value.open_hours = [(0, 604800)]


@pytest.fixture(autouse=True)
def geoproduct_mock(geoproduct):
    return geoproduct
