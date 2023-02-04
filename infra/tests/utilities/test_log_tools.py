"""Test our logging tools and utilities."""
import logging

import pytest

from walle.util import log_tools


class TestRateLimiter:
    class MockHandler(logging.Handler):
        def __init__(self):
            self.emitted_messages = []
            super(TestRateLimiter.MockHandler, self).__init__()

        def emit(self, record):
            self.emitted_messages.append(record)

    @pytest.fixture()
    def mock_timestamp(self, mp):
        return mp.function(log_tools.time.time, return_value=60)

    def _mk_logger(self, filter_obj=None):
        mock_handler = self.MockHandler()
        if filter_obj:
            mock_handler.addFilter(filter_obj)

        # root logger
        logger = logging.getLogger('')
        logger.addHandler(mock_handler)
        logger.setLevel(logging.NOTSET)

        return logger, mock_handler

    def test_not_filtering(self, mock_timestamp):
        logger, mock_handler = self._mk_logger()

        # log same message twice
        logger.info("I am a %s message", "first")
        logger.info("I am a %s message", "second")

        # no filter, logs twice
        assert 2 == len(mock_handler.emitted_messages)

    def test_filters_same_messages(self, mock_timestamp):
        logger, mock_handler = self._mk_logger(log_tools.RateLimitFilter())

        # log same message twice
        logger.info("I am a %s message", "first")
        logger.info("I am a %s message", "second")

        assert 1 == len(mock_handler.emitted_messages)

    def test_rate_limit_small_interval_limited(self, mock_timestamp):
        logger, mock_handler = self._mk_logger(log_tools.RateLimitFilter(interval=10))

        # log same message twice
        mock_timestamp.return_value = 70
        logger.info("I am a %s message", "first")
        mock_timestamp.return_value = 80
        logger.info("I am a %s message", "second")

        # logged within interval, rate limited
        assert 1 == len(mock_handler.emitted_messages)

    def test_rate_limit_big_interval_not_limited(self, mock_timestamp):
        logger, mock_handler = self._mk_logger(log_tools.RateLimitFilter(interval=10))

        # log same message twice
        mock_timestamp.return_value = 70
        logger.info("I am a %s message", "first")
        mock_timestamp.return_value = 81
        logger.info("I am a %s message", "second")

        # logged with big interval, not rate limited
        assert 2 == len(mock_handler.emitted_messages)
