# coding: utf-8
import json
from functools import partial

import pytest
from mock.mock import call


@pytest.fixture()
def mock_app(mock_logbroker_app_base):
    return partial(
        mock_logbroker_app_base,
        "cluster_tools.logbroker_app_base",
        "LogbrokerAppBase",
    )


def test_logbroker_app_base_cfg(mock_app):
    config = {"a": 1, "b": 2}
    with mock_app(config) as lb_app:
        assert lb_app.config == config


@pytest.mark.parametrize("batch_size", [None, 1234, 0, -1])
def test_logbroker_app_base_get_message_iter(batch_size, mock_app):
    config = {"dev": {"batch_size": batch_size}}
    with mock_app(config, [{"test": "message"}]) as lb_app:
        iterator = lb_app.get_message_iter("test_consumer", "test_topic")
        lb_app.lb.get_consumer.assert_called_once()
        call_args = lb_app.lb.get_consumer.call_args.args
        assert "test_consumer" in call_args[0]
        assert "test_topic" == call_args[1]
        if batch_size is None or batch_size <= 0:
            assert not isinstance(iterator._consumer.max_count, int)
        else:
            assert iterator._consumer.max_count == batch_size


@pytest.mark.parametrize("messages", [[], [{"a": 1}, {"b": 2}]])
def test_logbroker_app_base_write_messages(messages, mock_app):
    with mock_app() as lb_app:
        lb_app.write_messages(messages, "test_topic")
        assert lb_app.lb.get_producer.mock_calls == [
            call("test_topic"),
            call().sync_write_batch([json.dumps(m) for m in messages]),
        ]
