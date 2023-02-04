import asyncio

import pytest
from yt.yson.yson_types import YsonList

from maps_adv.common.helpers import AsyncIterator, coro_mock
from maps_adv.common.yt_utils.tests.shared_mock import (
    SharedCallableMock,
    SharedCallableMockManager,
    SharedCallableMockProxy,
)


class YqlRequestMockProxy(SharedCallableMockProxy):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)

        self._exposed_props_get = self._exposed_props_get + ("run", "get_results")
        self._exposed_props_set = self._exposed_props_set + ("run", "get_results")


class YqlRequestResultsMockProxy(SharedCallableMockProxy):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)

        self._exposed_props_get = self._exposed_props_get + ("status", "text", "table")
        self._exposed_props_set = self._exposed_props_set + ("status", "text", "table")


class YqlResultTableMockProxy(SharedCallableMockProxy):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)

        self._exposed_props_get = self._exposed_props_get + ("get_iterator",)
        self._exposed_props_set = self._exposed_props_set + ("get_iterator",)


SharedCallableMockManager.register(
    "YqlRequestMock", SharedCallableMock, YqlRequestMockProxy
)
SharedCallableMockManager.register(
    "YqlRequestResultsMock", SharedCallableMock, YqlRequestResultsMockProxy
)
SharedCallableMockManager.register(
    "YqlResultTableMock", SharedCallableMock, YqlResultTableMockProxy
)


@pytest.fixture
def mp_mock_manager():
    with SharedCallableMockManager() as manager:
        yield manager


@pytest.fixture
def dm():
    class DM:
        iter_some_data = AsyncIterator([])
        write_some_data = coro_mock()

    async def data_consumer(generator):
        async for _ in generator:
            await asyncio.sleep(0.1)

    DM.write_some_data.side_effect = data_consumer

    return DM()


@pytest.fixture(autouse=True)
def mock_yt(mocker, mp_mock_manager):
    methods = (
        "remove",
        "create",
        "exists",
        "list",
        "read_table",
        "write_table",
        "set_attribute",
        "run_remote_copy",
        "run_sort",
        "Transaction",
    )
    mocks = {
        method: mocker.patch(
            f"yt.wrapper.YtClient.{method}", mp_mock_manager.SharedCallableMock()
        )
        for method in methods
    }
    mocks["list"].return_value = YsonList()
    mocks["read_table"].return_value = []

    return mocks


@pytest.fixture
def yt_transaction_mock(mock_yt, mp_mock_manager):
    mock = mp_mock_manager.SharedCallableMock()
    mock_yt["Transaction"].return_value = mock

    return mock


@pytest.fixture(autouse=True)
def mock_yql(mocker, mp_mock_manager):
    table_get_iterator = mp_mock_manager.SharedCallableMock()
    table_get_iterator.return_value = []

    results_table = mp_mock_manager.YqlResultTableMock()
    results_table.get_iterator = table_get_iterator

    request_results = mp_mock_manager.YqlRequestResultsMock()
    request_results.status = mp_mock_manager.SharedCallableMock()
    request_results.text = mp_mock_manager.SharedCallableMock()
    request_results.table = results_table

    request = mp_mock_manager.YqlRequestMock()
    request.run = mp_mock_manager.SharedCallableMock()
    request.get_results = request_results

    query = mp_mock_manager.SharedCallableMock(return_value=request)

    return {
        "query": mocker.patch("yql.api.v1.client.YqlClient.query", query),
        "request_run": request.run,
        "request_get_results": request_results,
        "table_get_iterator": table_get_iterator,
    }
