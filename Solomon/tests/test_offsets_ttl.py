from __future__ import print_function

import sys  # noqa
import json
from time import sleep


import pytest
import yatest
import requests

from google.protobuf.text_format import Parse
from yatest.common.network import PortManager

from solomon.agent.protos.modules_config_pb2 import TAgentModuleConfig


# Variables for pytest fixtures
CONF_PATH = yatest.common.test_source_path('data/test_offsets_ttl.conf')

HTTP_PUSH_CONF = """
    HttpPush {
        BindAddress: "127.0.0.1"
        BindPort: %(port)s
        Name: "httpPush"

        Handlers {
            Project: "solomon"
            Service: "test"

            Endpoint: "/"
        }
    }
"""


def make_metrics(value):
    return {
        "sensors": [
            {
                "value": value,
                "labels": {
                    "metric": "chunk_offset"
                },
                "kind": "GAUGE"
            }
        ]
    }


AGENT_SERVER_PORT = None


class TestOffsetsTTL(object):
    CONF_OVERRIDES = {}

    FETCHER1 = 'Fetcher_1'
    FETCHER2 = 'Fetcher_2'
    FETCHER3 = 'Fetcher_3'

    @pytest.fixture(autouse=True)
    def solomon_agent_conf(self, request):
        with PortManager() as net:
            global AGENT_SERVER_PORT
            AGENT_SERVER_PORT = net.get_port()

            modules_conf = Parse(HTTP_PUSH_CONF % {'port': AGENT_SERVER_PORT}, TAgentModuleConfig())

            TestOffsetsTTL.CONF_OVERRIDES = {
                "Modules": [modules_conf]
            }

    def send_metrics(self, metrics):
        endpoint = 'http://127.0.0.1:' + str(AGENT_SERVER_PORT) + '/'
        requests.post(endpoint, data=json.dumps(metrics), headers={'Content-Type': 'application/json'})

    def read_data(self, agent, fetcher_id, seqno):
        params = {'project': 'solomon', 'service': 'test'}
        headers = {'X-Solomon-FetcherId': fetcher_id, 'X-Solomon-SequenceNumber': str(seqno)}

        return agent.read_raw(params=params, headers=headers)

    def determine_offset(self, data):
        metric = data['sensors'][0]
        if 'timeseries' in metric:
            return metric['timeseries'][0]['value']
        else:
            return metric['value']

    def test_offsets_ttl(self, agent):
        for i in range(1, 11):
            self.send_metrics(make_metrics(i))

        # Register new consumers
        fetcher1_data, _ = self.read_data(agent, self.FETCHER1, 0)
        fetcher2_data, _ = self.read_data(agent, self.FETCHER2, 0)

        assert self.determine_offset(fetcher1_data) == 1
        assert self.determine_offset(fetcher2_data) == 1

        fetcher2_offset = 3

        # Test a soft TTL. Split the request into two to prevent offset removal for Fetcher_2
        self.read_data(agent, self.FETCHER2, fetcher2_offset)
        sleep(8)
        self.read_data(agent, self.FETCHER2, fetcher2_offset)
        sleep(7)

        # watch interval
        sleep(5)

        # Right now Fetcher_1's offset should be 2

        # Add a new Fetcher id with a zero offset to prevent data removal on read requests
        self.read_data(agent, self.FETCHER3, 0)

        fetcher1_data, _ = self.read_data(agent, self.FETCHER1, 2)
        fetcher2_data, _ = self.read_data(agent, self.FETCHER2, fetcher2_offset)

        assert self.determine_offset(fetcher1_data) == self.determine_offset(fetcher2_data), "offset value has shifted"
        assert fetcher2_offset == self.determine_offset(fetcher2_data), "offset is the same as was committed"

        # Test a hard TTL
        fetcher1_offset = 6
        self.read_data(agent, self.FETCHER1, fetcher1_offset)

        fetcher2_offset = 9
        self.read_data(agent, self.FETCHER2, fetcher2_offset)
        self.read_data(agent, self.FETCHER3, 1)
        sleep(8)
        self.read_data(agent, self.FETCHER2, fetcher2_offset)
        self.read_data(agent, self.FETCHER3, 1)
        sleep(7)
        self.read_data(agent, self.FETCHER2, fetcher2_offset)
        self.read_data(agent, self.FETCHER3, 1)
        sleep(8)
        self.read_data(agent, self.FETCHER2, fetcher2_offset)
        self.read_data(agent, self.FETCHER3, 1)
        sleep(7)

        # watch interval
        sleep(5)

        # Info about Fetcher_1 should already be erased because of a hard TTL

        fetcher1_data, _ = self.read_data(agent, self.FETCHER1, 2)
        fetcher2_data, _ = self.read_data(agent, self.FETCHER2, fetcher2_offset)

        assert 1 == self.determine_offset(fetcher1_data), ("all chunks were returned despite of the committed"
                                                           + " and passed offset values")
        assert fetcher2_offset == self.determine_offset(fetcher2_data), "Fetcher_2's offset has not changed"
