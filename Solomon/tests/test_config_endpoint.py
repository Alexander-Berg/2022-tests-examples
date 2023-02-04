from __future__ import print_function

import sys  # noqa
from time import sleep

import yatest


CONF_PATH = yatest.common.test_source_path('data/test_config_endpoint.conf')


class TestHttpConfigEndpoint(object):
    CONF_OVERRIDES = {}

    def test_solomon_pull(self, agent):
        sleep(1)  # to let ManagementServer start
        config_as_is = agent.management_request('/config/plain', None)

        with open(CONF_PATH, 'r') as CONF:
            config = ''
            lines = []
            for line in config_as_is.split('\n'):
                if 'BindPort' not in line:
                    lines.append(line)

            config = '\n'.join(lines)

            assert config == CONF.read()
