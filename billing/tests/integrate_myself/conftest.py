import kikimr.public.sdk.python.persqueue.grpc_pq_streaming_api as pqlib
import os
import pytest
from kikimr.public.sdk.python.persqueue.errors import SessionFailureResult
from yatest.common import source_path


@pytest.fixture(scope="module")
def init_api():
    logbroker_port = os.environ['LOGBROKER_PORT']
    logbroker_host = "localhost"

    api = pqlib.PQStreamingAPI(logbroker_host, logbroker_port)
    api_start_future = api.start()
    _ = api_start_future.result(timeout=10)

    yield api

    api.stop()


@pytest.fixture
def init_producer(init_api):
    topic = "accounts-events"
    source_id = "PqLibSampleProducer"
    configurator = pqlib.ProducerConfigurator(topic, source_id)

    producer = init_api.create_producer(configurator)

    start_future = producer.start()  # Also available with producer.start_future()
    # Wait for producer to start.
    start_result = start_future.result(timeout=10)

    # Will be used to store latest written SeqNo. See Logbroker basics for more info.
    max_seq_no = None

    # Result of start should be verified. An error could occur.
    if not isinstance(start_result, SessionFailureResult):
        if start_result.HasField("init"):
            max_seq_no = start_result.init.max_seq_no
        else:
            raise RuntimeError("Unexpected producer start result from server: {}.".format(start_result))

    yield {
        "producer": producer,
        "max_seq_no": max_seq_no
    }

    producer.stop()


@pytest.fixture
def consumer(init_api):
    topic = "accruals/new-accrual-common"
    consumer = "shared/testreader"
    configurator = pqlib.ConsumerConfigurator(topic, consumer)

    consumer = init_api.create_consumer(configurator)

    start_future = consumer.start()
    start_result = start_future.result(timeout=10)
    if not isinstance(start_result, SessionFailureResult):
        if not start_result.HasField("init"):
            raise RuntimeError("Bad consumer start result from server: {}.".format(start_result))
    else:
        raise RuntimeError("Error occurred on start of consumer: {}.".format(start_result))

    yield consumer

    consumer.stop()


@pytest.fixture
def message():
    with open(
        source_path() +
        '/billing/hot/accrualer/internal/accrualer/gotest/agency_accruals/test_bnpl/input_cashless.json',
        'r', encoding='utf-8'
    ) as f:
        return f.read()
