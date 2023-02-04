from __future__ import print_function

import pytest
import yatest

from google.protobuf.text_format import Parse
from yatest.common.network import PortManager

from solomon.agent.protos.modules_config_pb2 import TAgentModuleConfig

from misc import send_metrics


# Variables for pytest fixtures
DO_NOT_WAIT_AGENT_DATA = True
CONF_PATH = yatest.common.test_source_path('data/empty_config.conf')

# Variables for tests
NEXT_SEQ_NO_HEADER = 'X-Solomon-NextSequenceNumber'

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

KIND = 'GAUGE'
LABELS = {
    "label_name": "label_value"
}


def create_metric(value):
    return {
        "sensors": [{
            "value": value,
            "labels": LABELS,
            "kind": KIND,
        }]
    }

AGENT_SERVER_PORT = None


class TestDataDeduplication(object):
    CONF_OVERRIDES = {}

    @pytest.fixture(autouse=True)
    def solomon_pull_conf(self, request):
        with PortManager() as net:
            global AGENT_SERVER_PORT
            AGENT_SERVER_PORT = net.get_port()

            self._url = 'http://127.0.0.1:' + str(AGENT_SERVER_PORT)

            modules_conf = Parse(HTTP_PUSH_CONF % {'port': AGENT_SERVER_PORT}, TAgentModuleConfig())

            TestDataDeduplication.CONF_OVERRIDES = {
                "Modules": [modules_conf],
            }

    def init_data(self):
        self.num_of_initial_chunks = 3
        self.num_of_rest_chunks = 5

        self.metrics = [
            create_metric(i) for i in range(0, self.num_of_initial_chunks + self.num_of_rest_chunks)
        ]

    def send_initial_data(self):
        for chunk_i in range(0, self.num_of_initial_chunks):
            send_metrics(self._url + '/', self.metrics[chunk_i])

    # Checks the fix: https://st.yandex-team.ru/SOLOMON-4733#5da46981a2b79e001eb4f595
    def check_agent_restart(self):
        # Right now there are only 3 chunks of data with offsets 1, 2 and 3. So the largest allowed seqNo value is 4.
        # If a fetcher comes with a bigger value (e.g. after an agent restarted), it should be handled correctly
        chunk_offset = 5

        # First of all, all chunks are present
        data, headers = self.agent.read_raw(params={'project': 'solomon', 'service': 'test'},
                                            headers={'X-Solomon-SequenceNumber': str(chunk_offset)})
        assert 'sensors' in data
        assert len(data['sensors']) == 1
        assert 'timeseries' in data['sensors'][0]
        tseries = data['sensors'][0]['timeseries']
        assert len(tseries) == self.num_of_initial_chunks

        for i in range(0, self.num_of_initial_chunks):
            assert tseries[i]['value'] == i

        # And NextSequenceNumber is exactly one past the latest chunk
        assert int(headers[NEXT_SEQ_NO_HEADER]) == 4
        prev_next_seqno = headers[NEXT_SEQ_NO_HEADER]

        # Now a fetcher adapts to an agent
        chunk_offset = headers[NEXT_SEQ_NO_HEADER]
        data, headers = self.agent.read_raw(params={'project': 'solomon', 'service': 'test'},
                                            headers={'X-Solomon-SequenceNumber': str(chunk_offset)})
        assert data == {}
        assert headers[NEXT_SEQ_NO_HEADER] == prev_next_seqno  # no new data were added

    def send_more_data(self):
        for chunk_i in range(self.num_of_initial_chunks, self.num_of_initial_chunks + self.num_of_rest_chunks):
            send_metrics(self._url + '/', self.metrics[chunk_i])

    def check_fetcher_restart(self):
        data, headers = self.agent.read_raw(params={'project': 'solomon', 'service': 'test'},
                                            headers={'X-Solomon-SequenceNumber': str(0)})
        # Fetcher has restarted, but it hasn't read new data yet, so now it should read it
        assert 'sensors' in data
        assert 'timeseries' in data['sensors'][0]
        tseries = data['sensors'][0]['timeseries']
        assert len(tseries) == self.num_of_rest_chunks

        for i in range(0, self.num_of_rest_chunks):
            assert tseries[i]['value'] == self.num_of_initial_chunks + i

        # And Agent returns correct seqNo
        assert int(headers[NEXT_SEQ_NO_HEADER]) == self.num_of_initial_chunks + self.num_of_rest_chunks + 1

        # Now, if Fetcher restarts again,
        data, headers = self.agent.read_raw(params={'project': 'solomon', 'service': 'test'},
                                            headers={'X-Solomon-SequenceNumber': str(0)})

        # it will not get the same data
        assert data == {}
        assert int(headers[NEXT_SEQ_NO_HEADER]) == self.num_of_initial_chunks + self.num_of_rest_chunks + 1

    def test_data_deduplication(self, agent):
        self.agent = agent
        self.init_data()

        self.send_initial_data()
        self.check_agent_restart()
        self.send_more_data()
        self.check_fetcher_restart()
