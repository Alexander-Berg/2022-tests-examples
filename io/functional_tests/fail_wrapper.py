import pytest


class FailWrapper(object):
    def __init__(self, logger):
        self.logger = logger

    def assert_fail(self, condition, message):
        if not condition:
            self.fail(message)

    def fail(self, message):
        self.logger.log_fail(message)
        pytest.fail(message)
