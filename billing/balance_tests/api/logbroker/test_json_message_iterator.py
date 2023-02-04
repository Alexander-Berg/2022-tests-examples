# coding=utf-8
import pytest
from kikimr.public.sdk.python.persqueue.grpc_pq_streaming_api import WriterCodec


@pytest.mark.parametrize(
    "messages,result",
    [
        (None, []),
        ([], []),
        (
            [
                None,
                [],
                "Zv2\u0442\u0435\u0441\u0442ct3",
                u"цу4щк2шмт23ы",
                b"2q3o7r\u0442\u0435\u0441\u0442yw4aiu34",
                {"a": 1},
                {"b": [1, 2, 3], "c": True, "e": None},
            ],
            [
                (None, None),
                ([], None),
                (None, ValueError),
                (None, ValueError),
                (None, ValueError),
                ({"a": 1}, None),
                ({"b": [1, 2, 3], "c": True, "e": None}, None),
            ],
        ),
    ],
)
@pytest.mark.parametrize(
    "codec,codec_supported",
    [
        (WriterCodec.RAW, True),
        (WriterCodec.GZIP, True),
        (WriterCodec.LZOP, False),
    ],
)
def test_json_message_iterator(
    messages, result, codec, codec_supported, mock_logbroker
):
    with mock_logbroker(messages, codec=codec) as lb_mock:
        import balance.api.logbroker as lb

        reload(lb)

        consumer = lb_mock.get_instance().get_consumer("test_consumer", "test_topic")
        iterator = lb.JsonMessageIterator(consumer)
        # read the first time
        json_messages = list(iterator)
        assert len(json_messages) == len(result) == len(iterator)
        if messages:
            # check how commit alone works
            assert iterator.commit() is True
            assert len(iterator) == 0
            assert iterator.commit() is False
            assert iterator.commit_if_processed(len(json_messages)) is False
            # read the second time (the mock should always return the same read results)
            json_messages = list(iterator)
            assert len(json_messages) == len(result) == len(iterator)
            # check how the should_commit and commit_if_processed work
            assert iterator.should_commit(len(json_messages)) is True
            assert iterator.should_commit(len(json_messages) - 1) is False
            assert iterator.commit_if_processed(len(json_messages) - 1) is False
            assert iterator.commit_if_processed(len(json_messages)) is True
            assert len(iterator) == 0
            assert iterator.commit_if_processed(len(json_messages)) is False
            assert iterator.commit() is False
        else:
            # if no messages were read, nothing can be committed
            assert iterator.should_commit(len(json_messages)) is False
            assert iterator.commit_if_processed(len(json_messages)) is False
            assert iterator.commit() is False
        # check the results
        if messages:
            for (res, _orig, exc), (expected_res, expected_exc) in zip(
                json_messages, result
            ):
                if codec_supported:
                    assert res == expected_res
                    if expected_exc is None:
                        assert exc is None
                    else:
                        assert isinstance(exc, expected_exc)
                else:
                    assert res is None
                    assert isinstance(exc, ValueError)
