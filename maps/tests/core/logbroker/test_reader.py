import asyncio
from unittest.mock import Mock

import kikimr.public.sdk.python.persqueue.grpc_pq_streaming_api as pqlib
import pytest
from ydb.public.api.protos.draft import persqueue_error_codes_pb2
from ydb.public.api.protos.draft import persqueue_common_pb2
from kikimr.yndx.api.protos import persqueue_pb2

from maps_adv.adv_store.v2.lib.core.logbroker import LogbrokerError, TopicReader
from maps_adv.adv_store.v2.tests import coro_mock

pytestmark = [pytest.mark.asyncio]


init_response = persqueue_pb2.ReadResponse(init=persqueue_pb2.ReadResponse.Init())
error_response = persqueue_pb2.ReadResponse(
    error=persqueue_common_pb2.Error(
        code=persqueue_error_codes_pb2.OVERLOAD, description="description"
    )
)
error_message = pqlib.ConsumerMessage(
    message_type=pqlib.ConsumerMessageType.MSG_ERROR,
    message_body=error_response,
    frontend=None,
)


def create_data_message(messages, cookie):
    data = persqueue_pb2.ReadResponse.Data(
        message_batch=[
            persqueue_pb2.ReadResponse.Data.MessageBatch(
                topic="topic",
                partition=0,
                message=[
                    persqueue_pb2.ReadResponse.Data.Message(data=message, offset=0)
                    for message in messages
                ],
            )
        ],
        cookie=cookie,
    )
    return pqlib.ConsumerMessage(
        message_type=pqlib.ConsumerMessageType.MSG_DATA,
        message_body=persqueue_pb2.ReadResponse(data=data),
        frontend=None,
    )


def create_commit_message(cookies):
    return pqlib.ConsumerMessage(
        message_type=pqlib.ConsumerMessageType.MSG_COMMIT,
        message_body=persqueue_pb2.ReadResponse(
            commit=persqueue_pb2.ReadResponse.Commit(cookie=cookies)
        ),
        frontend=None,
    )


@pytest.fixture
def consumer_mock(mocker):
    mocker.patch("asyncio.wrap_future", side_effect=lambda fut: fut)

    class MockPQStreamingConsumer:
        start = coro_mock()
        stop = coro_mock()
        next_event = coro_mock()
        commit = Mock()
        reads_done = Mock()

    return MockPQStreamingConsumer()


async def test_start_raises_on_start_timeout(consumer_mock, mocker):
    mocker.patch(
        "maps_adv.adv_store.v2.lib.core.logbroker.TopicReader.START_TIMEOUT", 0
    )
    consumer_mock.start.coro.side_effect = lambda: asyncio.sleep(1)
    reader = TopicReader(consumer_mock)

    with pytest.raises(LogbrokerError) as exc_info:
        await reader.start()

    assert exc_info.value.args == ("Consumer start timeout",)


async def test_start_raises_on_start_failure(consumer_mock):
    consumer_mock.start.coro.return_value = error_response
    reader = TopicReader(consumer_mock)

    with pytest.raises(LogbrokerError) as exc_info:
        await reader.start()

    assert exc_info.value.args == (
        f"Consumer failed to start with error {error_response}",
    )


async def test_start_raises_on_second_call(consumer_mock):
    consumer_mock.start.coro.return_value = init_response
    reader = TopicReader(consumer_mock)

    await reader.start()
    with pytest.raises(RuntimeError) as exc_info:
        await reader.start()

    assert exc_info.value.args == ("Attempt to call start() twice",)


async def test_start_starts_consumer(consumer_mock):
    consumer_mock.start.coro.return_value = init_response
    reader = TopicReader(consumer_mock)

    await reader.start()

    assert consumer_mock.start.coro.called


async def test_stop_raises_without_start(consumer_mock):
    consumer_mock.start.coro.return_value = init_response
    reader = TopicReader(consumer_mock)

    with pytest.raises(RuntimeError) as exc_info:
        await reader.stop()

    assert exc_info.value.args == ("Attempt to call stop() before start",)


async def test_stop_raises_on_stop_timeout(consumer_mock, mocker):
    mocker.patch("maps_adv.adv_store.v2.lib.core.logbroker.TopicReader.STOP_TIMEOUT", 0)
    consumer_mock.start.coro.return_value = init_response
    consumer_mock.stop.coro.side_effect = lambda: asyncio.sleep(1)
    reader = TopicReader(consumer_mock)
    await reader.start()

    with pytest.raises(LogbrokerError) as exc_info:
        await reader.stop()

    assert exc_info.value.args == ("Consumer stop timeout",)


async def test_stop_calls_stop(consumer_mock):
    consumer_mock.start.coro.return_value = init_response
    reader = TopicReader(consumer_mock)
    await reader.start()

    await reader.stop()

    assert consumer_mock.stop.coro.called


async def test_read_batch_raise_before_start(consumer_mock):
    consumer_mock.start.coro.return_value = init_response
    reader = TopicReader(consumer_mock)

    with pytest.raises(RuntimeError) as exc_info:
        [msg async for msg in reader.read_batch(1)]

    assert exc_info.value.args == ("Attempt to use object before start or after stop",)


async def test_commit_raise_before_start(consumer_mock):
    consumer_mock.start.coro.return_value = init_response
    reader = TopicReader(consumer_mock)

    with pytest.raises(RuntimeError) as exc_info:
        reader.commit()

    assert exc_info.value.args == ("Attempt to use object before start or after stop",)


async def test_finish_reading_raise_before_start(consumer_mock):
    consumer_mock.start.coro.return_value = init_response
    reader = TopicReader(consumer_mock)

    with pytest.raises(RuntimeError) as exc_info:
        await reader.finish_reading()

    assert exc_info.value.args == ("Attempt to use object before start or after stop",)


async def test_read_batch_raise_after_stop(consumer_mock):
    consumer_mock.start.coro.return_value = init_response
    reader = TopicReader(consumer_mock)
    await reader.start()
    await reader.stop()

    with pytest.raises(RuntimeError) as exc_info:
        [msg async for msg in reader.read_batch(1)]

    assert exc_info.value.args == ("Attempt to use object before start or after stop",)


async def test_commit_raise_after_stop(consumer_mock):
    consumer_mock.start.coro.return_value = init_response
    reader = TopicReader(consumer_mock)
    await reader.start()
    await reader.stop()

    with pytest.raises(RuntimeError) as exc_info:
        reader.commit()

    assert exc_info.value.args == ("Attempt to use object before start or after stop",)


async def test_finish_reading_raise_after_stop(consumer_mock):
    consumer_mock.start.coro.return_value = init_response
    reader = TopicReader(consumer_mock)
    await reader.start()
    await reader.stop()

    with pytest.raises(RuntimeError) as exc_info:
        await reader.finish_reading()

    assert exc_info.value.args == ("Attempt to use object before start or after stop",)


async def test_read_batch_returns_nothing_read_timeout(consumer_mock, mocker):
    consumer_mock.start.coro.return_value = init_response
    consumer_mock.next_event.coro.side_effect = lambda: asyncio.sleep(1)

    async with TopicReader(consumer_mock) as reader:
        assert [msg async for msg in reader.read_batch(1, read_timeout=0)] == []


async def test_read_batch_raises_on_error(consumer_mock):
    consumer_mock.start.coro.return_value = init_response
    consumer_mock.next_event.coro.return_value = error_message

    async with TopicReader(consumer_mock) as reader:
        with pytest.raises(LogbrokerError) as exc_info:
            [msg async for msg in reader.read_batch(1)]

    assert exc_info.value.args == (f"Read failed with error {error_response}",)


async def test_read_batch_calls_next_event(consumer_mock):
    consumer_mock.start.coro.return_value = init_response
    consumer_mock.next_event.coro.return_value = create_data_message([b"msg"], 1)

    async with TopicReader(consumer_mock) as reader:
        [msg async for msg in reader.read_batch(1)]

    assert consumer_mock.next_event.called


async def test_read_batch_returns_data(consumer_mock):
    consumer_mock.start.coro.return_value = init_response
    consumer_mock.next_event.coro.side_effect = [
        create_data_message([b"msg1"], 1),
        create_data_message([b"msg2"], 2),
    ]

    async with TopicReader(consumer_mock) as reader:
        messages = [msg async for msg in reader.read_batch(2)]

    assert messages == [b"msg1", b"msg2"]


async def test_read_batch_does_not_call_next_event_after_threshold(consumer_mock):
    consumer_mock.start.coro.return_value = init_response
    consumer_mock.next_event.coro.return_value = create_data_message(
        [b"msg", b"msg"], 1
    )

    async with TopicReader(consumer_mock) as reader:
        [msg async for msg in reader.read_batch(3)]

    assert consumer_mock.next_event.call_count == 2


async def test_commit_commits_all_uncommited_cookies(consumer_mock):
    consumer_mock.start.coro.return_value = init_response
    consumer_mock.next_event.coro.side_effect = [
        create_data_message([b"msg1"], 1),
        create_data_message([b"msg2"], 2),
    ]

    async with TopicReader(consumer_mock) as reader:
        [msg async for msg in reader.read_batch(2)]
        reader.commit()

    consumer_mock.commit.assert_called_with([1, 2])


async def test_commit_does_not_commit_twice(consumer_mock):
    consumer_mock.start.coro.return_value = init_response
    consumer_mock.next_event.coro.side_effect = [
        create_data_message([b"msg1"], 1),
        create_data_message([b"msg2"], 2),
    ]

    async with TopicReader(consumer_mock) as reader:
        [msg async for msg in reader.read_batch(2)]
        reader.commit()
        reader.commit()

    assert consumer_mock.commit.call_count == 1


async def test_finish_reading_calls_reads_done(consumer_mock):
    consumer_mock.start.coro.return_value = init_response
    consumer_mock.next_event.coro.return_value = create_data_message([b"msg"], 1)

    async with TopicReader(consumer_mock) as reader:
        await reader.finish_reading()

    assert consumer_mock.reads_done.called


async def test_finish_reading_waits_for_ack(consumer_mock):
    consumer_mock.start.coro.return_value = init_response
    consumer_mock.next_event.coro.side_effect = [
        create_data_message([b"msg"], 1),
        create_data_message([b"msg"], 2),
        create_data_message([b"msg"], 3),
        create_data_message([b"msg"], 4),
        create_commit_message([1]),
        create_commit_message([2, 3]),
    ]

    async with TopicReader(consumer_mock) as reader:
        [msg async for msg in reader.read_batch(3)]
        reader.commit()
        await reader.finish_reading()

    assert consumer_mock.next_event.call_count == 6
