import json

import pytest
from kikimr.public.sdk.python.persqueue.grpc_pq_streaming_api import WriterCodec
from mock import mock


def _mock_message(message, codec):
    m = mock.MagicMock()
    m.data = (
        json.dumps(message, ensure_ascii=False, encoding="utf8")
        if not isinstance(message, (str, unicode, bytes))
        else message
    )
    if codec == WriterCodec.GZIP:
        from gzip import GzipFile
        from StringIO import StringIO

        buf = StringIO()
        with GzipFile(fileobj=buf, mode="wb") as out:
            if isinstance(m.data, bytes):
                out.write(m.data)
            else:
                out.write(m.data.encode("utf8"))
        m.data = buf.getvalue()
    m.meta.codec = codec.value
    return m


class TimeoutError(Exception):
    """Mock of the TimeoutError"""


def _get_consumer_read_mock(get_consumer, messages_per_topic, raise_on_read, codec):
    def _read(**_kwargs):
        _, topic = get_consumer.call_args.args
        if isinstance(messages_per_topic, dict):
            messages = messages_per_topic.get(topic)
        else:
            messages = messages_per_topic
        if messages:
            return [_mock_message(m, codec) for m in messages], topic
        raise raise_on_read

    return _read


def _mock_logbroker(
    messages=None,
    raise_on_read=TimeoutError,
    raise_on_write=None,
    patch_path="balance.api.logbroker.Logbroker",
    codec=WriterCodec.RAW,
):
    LB = mock.MagicMock()
    lb = LB.get_instance.return_value
    consumer = lb.get_consumer.return_value
    consumer.read = mock.MagicMock(
        wraps=_get_consumer_read_mock(lb.get_consumer, messages, raise_on_read, codec)
    )
    producer = lb.get_producer.return_value
    if raise_on_write:
        producer.write.side_effect = raise_on_write
        producer.sync_write_batch.side_effect = raise_on_write
    return mock.patch(patch_path, LB)


@pytest.fixture()
def mock_logbroker():
    return _mock_logbroker
