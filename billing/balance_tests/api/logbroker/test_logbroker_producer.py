from concurrent.futures import Future

import pytest
from kikimr.public.sdk.python.persqueue.errors import (
    ActorNotReadyException,
    ActorTerminatedException,
    SessionClosedException,
    SessionFailureResult,
)
from mock import mock

from balance.api.logbroker import LogbrokerProducer
from balance.exc import MESSAGE_WRITE_ERROR, START_ERROR, START_UNEXPECTED_RESULT


def _mock_pqlib_producer(lb):
    lb.init()
    pqlib_producer = lb.api.create_producer.return_value
    start_future = pqlib_producer.start.return_value
    start_result = start_future.result.return_value
    start_result.init.max_seq_no = 123
    return pqlib_producer, start_future, start_result


def test_logbroker_producer_create_and_stop(logbroker_auth_pqlib_mocks):
    lb = logbroker_auth_pqlib_mocks
    # create an instance
    lb_a = lb.Logbroker.get_instance("a")
    # create a producer
    pqlib_producer, start_future, start_result = _mock_pqlib_producer(lb_a)
    producer = LogbrokerProducer(lb_a, "test_topic")
    assert producer.logbroker == lb_a
    assert producer.topic == "test_topic"
    assert producer.producer is None
    assert producer.last_id is None
    with pytest.raises(ValueError):
        producer.next_seq_no()
    producer.stop()
    pqlib_producer.stop.assert_not_called()


def test_logbroker_producer_init_ok_and_stop(logbroker_auth_pqlib_mocks):
    lb = logbroker_auth_pqlib_mocks
    # create an instance
    lb_a = lb.Logbroker.get_instance("a")
    # create a producer
    pqlib_producer, start_future, start_result = _mock_pqlib_producer(lb_a)
    producer = LogbrokerProducer(lb_a, "test_topic")
    # initialize
    producer.init()
    lb.pqlib.ProducerConfigurator.assert_called_once()
    assert lb.pqlib.ProducerConfigurator.call_args.args[0] == "test_topic"
    lb_a.api.create_producer.assert_called_once_with(
        lb.pqlib.ProducerConfigurator.return_value, lb_a.credentials_provider
    )
    assert producer.producer is not None
    assert producer.producer == pqlib_producer
    producer.producer.start.assert_called_once()
    start_future.result.assert_called_once_with(timeout=producer.logbroker.timeout)
    start_result.HasField.assert_called_once_with("init")
    assert producer.last_id == start_result.init.max_seq_no
    assert producer.next_seq_no() == start_result.init.max_seq_no + 1
    # stop
    producer.stop()
    assert producer.producer is None
    pqlib_producer.stop.assert_called_once()


def test_logbroker_producer_init_errors(logbroker_auth_pqlib_mocks):
    lb = logbroker_auth_pqlib_mocks
    # create an instance
    lb_a = lb.Logbroker.get_instance("a")
    # create a producer
    pqlib_producer, start_future, _ = _mock_pqlib_producer(lb_a)
    producer = LogbrokerProducer(lb_a, "test_topic")
    # initialize with SessionFailureResult
    start_future.result.return_value = SessionFailureResult("test_failure_reason")
    with pytest.raises(START_ERROR):
        producer.init()
    assert producer.producer is None
    assert producer.last_id is None
    # initialize with HasField("init") returning False
    start_future.result.reset_mock(return_value=True)
    start_result = start_future.result.return_value
    start_result.HasField.return_value = False
    with pytest.raises(START_UNEXPECTED_RESULT):
        producer.init()
    assert producer.producer is None
    assert producer.last_id is None


def test_logbroker_producer_write_ok(logbroker_auth_pqlib_mocks):
    lb = logbroker_auth_pqlib_mocks
    # create an instance
    lb_a = lb.Logbroker.get_instance("a")
    # create a producer
    pqlib_producer, start_future, start_result = _mock_pqlib_producer(lb_a)
    start_seq_no = start_result.init.max_seq_no
    producer = LogbrokerProducer(lb_a, "test_topic")
    # write the message
    producer.write("test_message")
    assert producer.last_id == start_seq_no + 1
    assert producer.producer == pqlib_producer
    producer.producer.write.assert_called_once_with(producer.last_id, "test_message")


@pytest.mark.parametrize(
    "error_class,expected",
    [
        (ActorTerminatedException, True),
        (SessionClosedException, True),
        (ActorNotReadyException, True),
        (Exception, False),
    ],
)
def test_logbroker_producer_write_errors(
    error_class, expected, logbroker_auth_pqlib_mocks
):
    lb = logbroker_auth_pqlib_mocks
    # create an instance
    lb_a = lb.Logbroker.get_instance("a")
    # create a producer
    pqlib_producer, start_future, start_result = _mock_pqlib_producer(lb_a)
    start_seq_no = start_result.init.max_seq_no
    producer = LogbrokerProducer(lb_a, "test_topic")
    # write the message
    pqlib_producer.write.side_effect = error_class
    if expected:
        with pytest.raises(START_ERROR):
            producer.write("test_message", 3)
            assert producer.last_id == start_seq_no + 3
            assert len(pqlib_producer.stop.mock_calls) == 3
            assert producer.producer is None
    else:
        with pytest.raises(error_class):
            producer.write("test_message", 3)
            assert producer.last_id == start_seq_no + 1
            pqlib_producer.stop.assert_not_called()
            assert producer.producer == pqlib_producer


def _pqlib_write(_seq_no, data):
    f = Future()
    exc = data.get("exception")
    if exc is not None:
        f.set_exception(exc)
        return f
    cancelled = data.get("cancelled")
    if cancelled:
        f.cancel()
        return f
    res = data.get("result")
    if res is not None:
        f.set_result(res)
        return f
    return f


@pytest.mark.parametrize(
    "messages,expected_states",
    [
        ([], []),
        ([{}, {}, {}], [MESSAGE_WRITE_ERROR, MESSAGE_WRITE_ERROR, MESSAGE_WRITE_ERROR]),
        (
            [
                {},
                {"exception": Exception()},
                {"cancelled": True},
                {"result": mock.MagicMock()},
                {"result": mock.MagicMock(HasField=mock.MagicMock(return_value=False))},
            ],
            [
                MESSAGE_WRITE_ERROR,
                MESSAGE_WRITE_ERROR,
                MESSAGE_WRITE_ERROR,
                None,
                MESSAGE_WRITE_ERROR,
            ],
        ),
    ],
)
def test_logbroker_producer_sync_write_batch_ok(
    messages, expected_states, logbroker_auth_pqlib_mocks
):
    lb = logbroker_auth_pqlib_mocks
    # create an instance
    lb_a = lb.Logbroker.get_instance("a")
    # create a producer
    pqlib_producer, start_future, start_result = _mock_pqlib_producer(lb_a)
    pqlib_producer.write = mock.MagicMock(wraps=_pqlib_write)
    producer = LogbrokerProducer(lb_a, "test_topic")
    # write messages
    res = producer.sync_write_batch(messages)
    assert len(res) == len(messages)
    for exc, expected in zip(res, expected_states):
        if expected is None:
            assert exc is None
        else:
            assert isinstance(exc, expected)
