import asyncio
from unittest.mock import Mock

import pytest

from maps_adv.adv_store.v2.lib.core.logbroker import LogbrokerClient, LogbrokerError
from maps_adv.adv_store.v2.tests import coro_mock

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def api_mock(mocker):
    mocker.patch("asyncio.wrap_future", side_effect=lambda fut: fut)

    class MockPQStreamingAPI:
        start = coro_mock()
        stop = Mock()
        create_consumer = Mock()
        create_producer = Mock()

    api = MockPQStreamingAPI()
    mocker.patch(
        "kikimr.public.sdk.python.persqueue.grpc_pq_streaming_api.PQStreamingAPI",
        return_value=api,
    )

    return api


async def test_start_raises_on_start_timeout(api_mock, mocker):
    mocker.patch(
        "maps_adv.adv_store.v2.lib.core.logbroker.LogbrokerClient.START_TIMEOUT", 0
    )
    api_mock.start.coro.side_effect = lambda: asyncio.sleep(1)
    client = LogbrokerClient("host", 10, "token", "source_id")

    with pytest.raises(LogbrokerError) as exc_info:
        await client.start()

    assert exc_info.value.args == ("Api start timeout",)


async def test_start_raises_on_start_failure(api_mock):
    api_mock.start.coro.return_value = False
    client = LogbrokerClient("host", 10, "token", "source_id")

    with pytest.raises(LogbrokerError) as exc_info:
        await client.start()

    assert exc_info.value.args == ("Api start failed",)


async def test_start_raises_on_second_call(api_mock):
    api_mock.start.coro.return_value = True
    client = LogbrokerClient("host", 10, "token", "source_id")

    await client.start()
    with pytest.raises(RuntimeError) as exc_info:
        await client.start()

    assert exc_info.value.args == ("Attempt to call start() twice",)


async def test_start_starts_api(api_mock):
    api_mock.start.coro.return_value = True
    client = LogbrokerClient("host", 10, "token", "source_id")

    await client.start()

    assert api_mock.start.coro.called


async def test_stop_raises_without_start(api_mock):
    api_mock.start.coro.return_value = True
    client = LogbrokerClient("host", 10, "token", "source_id")

    with pytest.raises(RuntimeError) as exc_info:
        await client.stop()

    assert exc_info.value.args == ("Attempt to call stop() before start",)


async def test_stop_calls_stop(api_mock):
    api_mock.start.coro.return_value = True
    client = LogbrokerClient("host", 10, "token", "source_id")
    await client.start()

    await client.stop()

    assert api_mock.stop.called


@pytest.mark.parametrize(
    "method",
    [
        lambda client: client.create_reader("topic", "consumer"),
        lambda client: client.create_writer("topic"),
    ],
)
async def test_methods_raise_before_start(method, api_mock):
    api_mock.start.coro.return_value = True
    client = LogbrokerClient("host", 10, "token", "source_id")

    with pytest.raises(RuntimeError) as exc_info:
        method(client)

    assert exc_info.value.args == ("Attempt to use object before start or after stop",)


@pytest.mark.parametrize(
    "method",
    [
        lambda client: client.create_reader("topic", "consumer"),
        lambda client: client.create_writer("topic"),
    ],
)
async def test_methods_raise_after_stop(method, api_mock):
    api_mock.start.coro.return_value = True
    client = LogbrokerClient("host", 10, "token", "source_id")
    await client.start()
    await client.stop()

    with pytest.raises(RuntimeError) as exc_info:
        method(client)

    assert exc_info.value.args == ("Attempt to use object before start or after stop",)


async def test_create_reader_creates_consumer(api_mock, mocker):
    api_mock.start.coro.return_value = True

    async with LogbrokerClient("host", 10, "token", "source_id") as client:
        client.create_reader("topic", "consumer")

    assert api_mock.create_consumer.called


async def test_create_writer_creates_producer(api_mock, mocker):
    api_mock.start.coro.return_value = True

    async with LogbrokerClient("host", 10, "token", "source_id") as client:
        client.create_writer("topic")

    assert api_mock.create_consumer.calledd
