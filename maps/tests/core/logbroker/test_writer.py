import asyncio
from unittest import mock

import pytest
from ydb.public.api.protos.draft import persqueue_error_codes_pb2
from ydb.public.api.protos.draft import persqueue_common_pb2
from kikimr.yndx.api.protos import persqueue_pb2

from maps_adv.adv_store.v2.lib.core.logbroker import LogbrokerError, TopicWriter
from maps_adv.adv_store.v2.tests import coro_mock

pytestmark = [pytest.mark.asyncio]


error_response = persqueue_pb2.WriteResponse(
    error=persqueue_common_pb2.Error(
        code=persqueue_error_codes_pb2.OVERLOAD, description="description"
    )
)
init_response = persqueue_pb2.WriteResponse(
    init=persqueue_pb2.WriteResponse.Init(
        max_seq_no=10, session_id="session_id", partition=0, topic="topic"
    )
)
ack_response = persqueue_pb2.WriteResponse(
    ack=persqueue_pb2.WriteResponse.Ack(seq_no=0, offset=0, already_written=False)
)


@pytest.fixture
def producer_mock(mocker):
    mocker.patch("asyncio.wrap_future", side_effect=lambda fut: fut)

    class MockPQStreamingProducer:
        start = coro_mock()
        stop = coro_mock()
        write = coro_mock()

    return MockPQStreamingProducer()


async def test_start_raises_on_start_timeout(producer_mock, mocker):
    mocker.patch(
        "maps_adv.adv_store.v2.lib.core.logbroker.TopicWriter.START_TIMEOUT", 0
    )
    producer_mock.start.coro.side_effect = lambda: asyncio.sleep(1)
    writer = TopicWriter(producer_mock)

    with pytest.raises(LogbrokerError) as exc_info:
        await writer.start()

    assert exc_info.value.args == ("Producer start timeout",)


async def test_start_raises_on_start_failure(producer_mock):
    producer_mock.start.coro.return_value = error_response
    writer = TopicWriter(producer_mock)

    with pytest.raises(LogbrokerError) as exc_info:
        await writer.start()

    assert exc_info.value.args == (
        f"Producer failed to start with error {error_response}",
    )


async def test_start_raises_on_second_call(producer_mock):
    producer_mock.start.coro.return_value = init_response
    writer = TopicWriter(producer_mock)

    await writer.start()
    with pytest.raises(RuntimeError) as exc_info:
        await writer.start()

    assert exc_info.value.args == ("Attempt to call start() twice",)


async def test_start_starts_producer(producer_mock):
    producer_mock.start.coro.return_value = init_response
    writer = TopicWriter(producer_mock)

    await writer.start()

    assert producer_mock.start.coro.called


async def test_stop_raises_without_start(producer_mock):
    producer_mock.start.coro.return_value = init_response
    writer = TopicWriter(producer_mock)

    with pytest.raises(RuntimeError) as exc_info:
        await writer.stop()

    assert exc_info.value.args == ("Attempt to call stop() before start",)


async def test_stop_raises_on_stop_timeout(producer_mock, mocker):
    mocker.patch("maps_adv.adv_store.v2.lib.core.logbroker.TopicWriter.STOP_TIMEOUT", 0)
    producer_mock.start.coro.return_value = init_response
    producer_mock.stop.coro.side_effect = lambda: asyncio.sleep(1)
    writer = TopicWriter(producer_mock)
    await writer.start()

    with pytest.raises(LogbrokerError) as exc_info:
        await writer.stop()

    assert exc_info.value.args == ("Producer stop timeout",)


async def test_stop_calls_stop(producer_mock):
    producer_mock.start.coro.return_value = init_response
    writer = TopicWriter(producer_mock)
    await writer.start()

    await writer.stop()

    assert producer_mock.stop.coro.called


async def test_write_one_raises_before_start(producer_mock):
    producer_mock.start.coro.return_value = init_response
    writer = TopicWriter(producer_mock)

    with pytest.raises(RuntimeError) as exc_info:
        await writer.write_one(b"data")

    assert exc_info.value.args == ("Attempt to use object before start or after stop",)


async def test_write_one_raises_after_stop(producer_mock):
    producer_mock.start.coro.return_value = init_response
    writer = TopicWriter(producer_mock)
    await writer.start()
    await writer.stop()

    with pytest.raises(RuntimeError) as exc_info:
        await writer.write_one(b"data")

    assert exc_info.value.args == ("Attempt to use object before start or after stop",)


async def test_write_one_raises_on_write_timeout(producer_mock, mocker):
    mocker.patch(
        "maps_adv.adv_store.v2.lib.core.logbroker.TopicWriter.WRITE_TIMEOUT", 0
    )
    producer_mock.start.coro.return_value = init_response
    producer_mock.write.coro.side_effect = lambda: asyncio.sleep(1)

    async with TopicWriter(producer_mock) as writer:
        with pytest.raises(LogbrokerError) as exc_info:
            await writer.write_one(b"data")

    assert exc_info.value.args == ("Message write timeout",)


async def test_write_one_raises_on_write_error(producer_mock):
    producer_mock.start.coro.return_value = init_response
    producer_mock.write.coro.return_value = error_response

    async with TopicWriter(producer_mock) as writer:
        with pytest.raises(LogbrokerError) as exc_info:
            await writer.write_one(b"data")

    assert exc_info.value.args == (f"Message write failed with error {error_response}",)


async def test_write_one_calls_write(producer_mock):
    producer_mock.start.coro.return_value = init_response
    producer_mock.write.coro.return_value = ack_response

    async with TopicWriter(producer_mock) as writer:
        await writer.write_one(b"data")

    producer_mock.write.assert_called_with(11, b"data")


async def test_write_one_increases_seq_no(producer_mock):
    producer_mock.start.coro.return_value = init_response
    producer_mock.write.coro.return_value = ack_response

    async with TopicWriter(producer_mock) as writer:
        await writer.write_one(b"data")
        await writer.write_one(b"data")

    assert producer_mock.write.call_args_list == [
        mock.call(11, b"data"),
        mock.call(12, b"data"),
    ]
