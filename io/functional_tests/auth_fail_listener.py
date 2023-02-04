import threading
import yatest.common as yc
from .connector import QuasarConnector
from enum import Enum
import logging
import pytest

logger = logging.getLogger(__name__)


class Behavior(Enum):
    IGNORE = 0
    FAIL = 1
    RESTART = 2
    SKIP = 3


class AuthFailListener(object):
    def __init__(self, config, behavior=Behavior.FAIL):
        self.behavior = behavior
        host = yc.get_param("target_host", "localhost")
        self.connector = QuasarConnector(host, config.json["syncd"]["port"], "syncd", False)
        self.thread = threading.Thread(target=self._listen)
        self.running = True
        self.fail = False
        self.thread.start()

    def _listen(self):
        self.connector.start()
        while self.running:
            if self.connector.wait_for_message(lambda m: m.HasField("auth_failed")) is not None:
                logger.info("Recieved auth fail message")
                self.fail = True
                return

    def _fail(self):
        logger.info("Failing test with behavoir: {}".format(self.behavior))
        if self.behavior == Behavior.FAIL:
            pytest.fail("Test was failed due known backend bug: https://st.yandex-team.ru/QUASARINFRA-446")
        elif self.behavior == Behavior.RESTART:
            logger.error(
                "Test was failed due known backend bug: https://st.yandex-team.ru/QUASARINFRA-446"
                "\nRestarting suite..."
            )
            raise yc.RestartTestException()
        elif self.behavior == Behavior.SKIP:
            pytest.skip("Test was failed due known backend bug: https://st.yandex-team.ru/QUASARINFRA-446")
        elif self.behavior == Behavior.IGNORE:
            return
        else:
            raise RuntimeError("Dont know what to do whith Behavoir: {}".format(self.behavior))

    def stop(self):
        self.running = False
        self.connector.close()
        self.thread.join()
        if self.fail:
            self._fail()
