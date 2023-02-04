import asyncio
from unittest.mock import Mock

import pytest
from tvmauth.mock import TvmClientPatcher

from maps_adv.common.helpers import coro_mock
from maps_adv.geosmb.clients.logbroker.logbroker import (
    LogbrokerClient,
    LogbrokerError,
)

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


@pytest.fixture
def lb_client():
    with TvmClientPatcher():
        yield LogbrokerClient(
            host="host",
            port=10,
            default_source_id=b"source_id",
            tvm_destination="dst-alias",
            tvm_port=8888,
            tvm_self_alias="self",
        )


async def test_start_raises_on_start_timeout(api_mock, lb_client, mocker):
    mocker.patch(
        "maps_adv.geosmb.clients.logbroker.logbroker.LogbrokerClient.START_TIMEOUT",  # noqa
        0,
    )
    api_mock.start.coro.side_effect = lambda: asyncio.sleep(1)

    with pytest.raises(LogbrokerError) as exc_info:
        await lb_client.start()

    assert exc_info.value.args == ("Api start timeout",)


async def test_start_raises_on_start_failure(api_mock, lb_client):
    api_mock.start.coro.return_value = False

    with pytest.raises(LogbrokerError) as exc_info:
        await lb_client.start()

    assert exc_info.value.args == ("Api start failed",)


async def test_start_raises_on_second_call(api_mock, lb_client):
    api_mock.start.coro.return_value = True

    await lb_client.start()
    with pytest.raises(RuntimeError) as exc_info:
        await lb_client.start()

    assert exc_info.value.args == ("Attempt to call start() twice",)


async def test_start_starts_api(api_mock, lb_client):
    api_mock.start.coro.return_value = True

    await lb_client.start()

    assert api_mock.start.coro.called


async def test_stop_raises_without_start(api_mock, lb_client):
    api_mock.start.coro.return_value = True

    with pytest.raises(RuntimeError) as exc_info:
        await lb_client.stop()

    assert exc_info.value.args == ("Attempt to call stop() before start",)


async def test_stop_calls_stop(api_mock, lb_client):
    api_mock.start.coro.return_value = True

    await lb_client.start()
    await lb_client.stop()

    assert api_mock.stop.called


@pytest.mark.parametrize(
    "method",
    [
        lambda client: client.create_reader("topic", "consumer"),
        lambda client: client.create_writer("topic"),
    ],
)
async def test_methods_raise_before_start(method, api_mock, lb_client):
    api_mock.start.coro.return_value = True

    with pytest.raises(RuntimeError) as exc_info:
        method(lb_client)

    assert exc_info.value.args == ("Attempt to use object before start or after stop",)


@pytest.mark.parametrize(
    "method",
    [
        lambda client: client.create_reader("topic", "consumer"),
        lambda client: client.create_writer("topic"),
    ],
)
async def test_methods_raise_after_stop(method, api_mock, lb_client):
    api_mock.start.coro.return_value = True
    await lb_client.start()
    await lb_client.stop()

    with pytest.raises(RuntimeError) as exc_info:
        method(lb_client)

    assert exc_info.value.args == ("Attempt to use object before start or after stop",)


async def test_create_reader_creates_consumer(api_mock, lb_client):
    api_mock.start.coro.return_value = True

    async with lb_client as started_client:
        started_client.create_reader("topic", "consumer")

    assert api_mock.create_consumer.called


async def test_create_writer_creates_producer(api_mock, lb_client):
    api_mock.start.coro.return_value = True

    async with lb_client as started_client:
        started_client.create_writer("topic")

    assert api_mock.create_producer.called
