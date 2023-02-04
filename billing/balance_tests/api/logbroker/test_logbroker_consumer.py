import pytest
from kikimr.public.sdk.python.persqueue.errors import (
    SessionFailureResult,
)
from kikimr.public.sdk.python.persqueue.grpc_pq_streaming_api import ConsumerMessageType
from mock import mock

from balance.api.logbroker import LogbrokerConsumer
from balance.exc import MESSAGE_READ_ERROR, START_ERROR, START_UNEXPECTED_RESULT


def _mock_pqlib_consumer(lb):
    lb.init()
    pqlib_consumer = lb.api.create_consumer.return_value
    start_future = pqlib_consumer.start.return_value
    start_result = start_future.result.return_value
    return pqlib_consumer, start_future, start_result


def test_logbroker_consumer_create_and_stop(logbroker_auth_pqlib_mocks):
    lb = logbroker_auth_pqlib_mocks
    # create an instance
    lb_a = lb.Logbroker.get_instance("a")
    # create a consumer
    pqlib_consumer, start_future, start_result = _mock_pqlib_consumer(lb_a)
    consumer = LogbrokerConsumer(lb_a, "consumer_name", "test_topic", 123)
    assert consumer.logbroker == lb_a
    assert consumer.name == "consumer_name"
    assert consumer.topic == "test_topic"
    assert consumer.consumer is None
    assert consumer.max_count == 123
    consumer.stop()
    pqlib_consumer.stop.assert_not_called()


def test_logbroker_consumer_init_ok_and_stop(logbroker_auth_pqlib_mocks):
    lb = logbroker_auth_pqlib_mocks
    # create an instance
    lb_a = lb.Logbroker.get_instance("a")
    # create a consumer
    pqlib_consumer, start_future, start_result = _mock_pqlib_consumer(lb_a)
    consumer = LogbrokerConsumer(lb_a, "consumer_name", "test_topic", 123)
    # initialize
    consumer.init()
    lb.pqlib.ConsumerConfigurator.assert_called_with(
        "test_topic", "consumer_name", max_count=123
    )
    lb_a.api.create_consumer.assert_called_once_with(
        lb.pqlib.ConsumerConfigurator.return_value, lb_a.credentials_provider
    )
    assert consumer.consumer is not None
    assert consumer.consumer == pqlib_consumer
    consumer.consumer.start.assert_called_once()
    start_future.result.assert_called_once_with(timeout=1)
    start_result.HasField.assert_called_once_with("init")
    # stop
    consumer.stop()
    assert consumer.consumer is None
    pqlib_consumer.stop.assert_called_once()


def test_logbroker_consumer_init_errors(logbroker_auth_pqlib_mocks):
    lb = logbroker_auth_pqlib_mocks
    # create an instance
    lb_a = lb.Logbroker.get_instance("a")
    # create a consumer
    pqlib_consumer, start_future, _ = _mock_pqlib_consumer(lb_a)
    consumer = LogbrokerConsumer(lb_a, "consumer_name", "test_topic", 123)
    # initialize with SessionFailureResult
    start_future.result.return_value = SessionFailureResult("test_failure_reason")
    with pytest.raises(START_ERROR):
        consumer.init()
    assert consumer.consumer is None
    # initialize with HasField("init") returning False
    start_future.result.reset_mock(return_value=True)
    start_result = start_future.result.return_value
    start_result.HasField.return_value = False
    with pytest.raises(START_UNEXPECTED_RESULT):
        consumer.init()
    assert consumer.consumer is None


def _make_result_mock(result_type):
    return mock.MagicMock(type=result_type)


def _make_result_with_data_mock(batches, cookie):
    result = _make_result_mock(ConsumerMessageType.MSG_DATA)
    result.message.data.cookie = cookie
    result.message.data.message_batch = [
        mock.MagicMock(message=[mock.MagicMock(data=message) for message in batch])
        for batch in batches
    ]
    return result


def _make_multiple_results(results):
    def _result(*args, **kwargs):
        if results:
            return results.pop()

    return mock.MagicMock(wraps=_result)


@pytest.fixture()
def result_with_data():
    batches = [
        ["a", "b", "c"],
        ["d", "e", "f"],
        ["g", "h", "i"],
    ]
    return _make_result_with_data_mock(batches, "a_cookie")


def _test_consumer_read_common(consumer, next_event, results, num_messages):
    num_results = len(results)
    last_result = results[0]
    if consumer.consumer is not None:
        consumer.consumer.commit.reset_mock()
    next_event.result = _make_multiple_results(results)
    messages, cookie = consumer.read()
    assert (
        next_event.result.mock_calls
        == [mock.call(timeout=consumer.logbroker.timeout)] * num_results
    )
    consumer.consumer.commit.assert_called_once_with(last_result.message.data.cookie)
    assert cookie is None
    assert len(messages) == num_messages


def test_logbroker_consumer_read(result_with_data, logbroker_auth_pqlib_mocks):
    lb = logbroker_auth_pqlib_mocks
    # create an instance
    lb_a = lb.Logbroker.get_instance("a")
    # create a consumer
    pqlib_consumer, start_future, start_result = _mock_pqlib_consumer(lb_a)
    next_event = pqlib_consumer.next_event.return_value
    consumer = LogbrokerConsumer(lb_a, "consumer_name", "test_topic", 123)
    num_messages = sum(
        len(batch.message) for batch in result_with_data.message.data.message_batch
    )
    # read messages with DATA result
    # with auto commit
    _test_consumer_read_common(consumer, next_event, [result_with_data], num_messages)
    # the consumer is inited the first time
    lb_a.api.create_consumer.assert_called_once()
    assert consumer.consumer == pqlib_consumer
    # without auto commit
    consumer.consumer.commit.reset_mock()
    next_event.result = _make_multiple_results([result_with_data])
    messages, cookie = consumer.read(auto_commit=False)
    assert next_event.result.mock_calls == [
        mock.call(timeout=consumer.logbroker.timeout),
    ]
    assert cookie == result_with_data.message.data.cookie
    assert len(messages) == num_messages
    consumer.consumer.commit.assert_not_called()
    # read messages with LOCK result
    lock_result = _make_result_mock(ConsumerMessageType.MSG_LOCK)
    _test_consumer_read_common(
        consumer,
        next_event,
        [result_with_data, lock_result],
        num_messages,
    )
    lock_result.ready_to_read.assert_called_once()
    # read messages with COMMIT result
    _test_consumer_read_common(
        consumer,
        next_event,
        [result_with_data, _make_result_mock(ConsumerMessageType.MSG_COMMIT)],
        num_messages,
    )
    # read messages with RELEASE result
    _test_consumer_read_common(
        consumer,
        next_event,
        [result_with_data, _make_result_mock(ConsumerMessageType.MSG_RELEASE)],
        num_messages,
    )
    # read messages with ERROR result
    _test_consumer_read_common(
        consumer,
        next_event,
        [result_with_data, _make_result_mock(ConsumerMessageType.MSG_ERROR)],
        num_messages,
    )
    pqlib_consumer.stop.assert_called_once()
    assert lb_a.api.create_consumer.call_count == 2
    # unexpected result
    with pytest.raises(MESSAGE_READ_ERROR):
        _test_consumer_read_common(
            consumer,
            next_event,
            [
                _make_result_mock("unknown_type"),
                _make_result_mock(ConsumerMessageType.MSG_ERROR),
            ],
            num_messages,
        )
