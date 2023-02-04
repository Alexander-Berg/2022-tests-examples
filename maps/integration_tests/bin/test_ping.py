import logging
import unittest

from .common import (
    PIPEDRIVE_GATE_URLS,
    pipedrive_gate_get,
)


class PingTest(unittest.TestCase):
    def test_ping(self):
        logging.info('Started test_ping()')

        for gate_url in PIPEDRIVE_GATE_URLS:
            logging.info(f'Pinging {gate_url}')
            pipedrive_gate_get(f'{gate_url}/ping')

        logging.info('Finished test_ping()')
