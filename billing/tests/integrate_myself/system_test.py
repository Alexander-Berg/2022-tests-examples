import json
import kikimr.public.sdk.python.persqueue.grpc_pq_streaming_api as pqlib
import logging
import pytest
import time
from yatest.common import source_path

logger = logging.getLogger(__name__)


def _process_single_batch(consumer_message):
    """
    This is a simple method extracting and returning all messages from single consumer batch.
    """
    ret = []
    for batch in consumer_message.data.message_batch:
        for message in batch.message:
            ret.append(message.data.decode('utf-8'))
    return ret


def test_accrualer_integration(init_producer, consumer, message):
    """
    Пишем в логброкер, ждём пока аккруалер из него прочитает, проверяем что аккруалер записал в другой топик
    """
    producer = init_producer["producer"]
    max_seq_no = max(init_producer["max_seq_no"], 1)
    response = producer.write(max_seq_no, message)
    write_result = response.result(timeout=10)

    if not write_result.HasField("ack"):
        raise RuntimeError("Message write failed with error {}".format(write_result))

    time.sleep(20)

    result = consumer.next_event().result(timeout=10)

    if not result.type == pqlib.ConsumerMessageType.MSG_DATA:
        pytest.fail("Expected result.type to be pqlib.ConsumerMessageType.MSG_DATA")

    extracted_messages = _process_single_batch(result.message)
    assert len(extracted_messages) == 1

    message = json.loads(extracted_messages[0])
    message.pop('message_dt')

    with open(
            source_path() +
            '/billing/hot/accrualer/internal/accrualer/gotest/agency_accruals/test_bnpl/result_cashless.json',
            'r', encoding='utf-8'
    ) as f:
        expected = json.loads(f.read())
        expected.pop('message_dt')
        expected.pop('billing_person_id')
        expected.pop('message_id')
        expected.pop('terminalid')

        message.pop('event')

        assert message == expected
