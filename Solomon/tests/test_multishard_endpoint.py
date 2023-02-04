from __future__ import print_function

import json

import pytest
import yatest

from google.protobuf.text_format import Parse
from yatest.common.network import PortManager

from solomon.agent.protos.modules_config_pb2 import TAgentModuleConfig
from solomon.agent.protos.http_server_config_pb2 import THttpServerConfig

from misc import send_metrics, are_metrics_equal
from multishard_decode import decode_multishard_data, ShardKey


# Variables for pytest fixtures
CONF_PATH = yatest.common.test_source_path('data/empty_config.conf')

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


def read_multishard_data(agent, continuation_token=''):
    shards_data = agent.read_all(continuation_token=continuation_token, response_format='JSON')
    new_continuation_token, shards_data = decode_multishard_data(shards_data)

    return new_continuation_token, shards_data


class TestMultiShardEndpoint(object):
    HTTP_PUSH_CONF = """
        HttpPush {
            BindAddress: "127.0.0.1"
            BindPort: %(port)s
            Name: "httpPush"

            Handlers {
                Project: "solomon"
                Service: "shard_1"

                Endpoint: "/1"
            }

            Handlers {
                Project: "solomon"
                Service: "shard_2"

                Endpoint: "/2"
            }

            Handlers {
                Project: "solomon"
                Service: "shard_3"

                Endpoint: "/3"
            }
        }
    """
    CONF_OVERRIDES = {}
    AGENT_SERVER_PORT = 0

    @pytest.fixture(autouse=True)
    def solomon_pull_conf(self, request):
        with PortManager() as net:
            self.AGENT_SERVER_PORT = net.get_port()
            self._url = 'http://127.0.0.1:' + str(self.AGENT_SERVER_PORT)

            modules_conf = Parse(self.HTTP_PUSH_CONF % {'port': self.AGENT_SERVER_PORT}, TAgentModuleConfig())

            TestMultiShardEndpoint.CONF_OVERRIDES = {
                "Modules": [modules_conf],
            }

    # Data inside Agent in this test is as follows:
    # | offset: 0 | offset: 1 | offset: 2 | offset: 3 |
    # | value: 0  | value: 1  | value: 2  | value: 3  |

    def init_data(self):
        self.metrics = [
            create_metric(i) for i in range(0, 4)
        ]

    def send_initial_data(self):
        for shard_i in range(1, 4):
            send_metrics(self._url + '/' + str(shard_i), self.metrics[0])

    # Read all data and register new consumers with a zero offset
    def read_initial_data(self, agent):
        return read_multishard_data(agent, continuation_token='')

    # Checks that:
    # 1. read data is the same as written
    # 2. Agent correctly responds with a continuation token
    def check_initial_data(self, agent):
        continuation_token, initial_data = self.read_initial_data(agent)

        for service in ['shard_1', 'shard_2', 'shard_3']:
            shard_key = ShardKey(project='solomon', service=service, cluster='')

            assert shard_key in initial_data
            shard_data = initial_data[shard_key]

            assert are_metrics_equal(json.loads(shard_data[0]), self.metrics[0])

        return continuation_token

    def send_rest_of_data(self):
        for shard_i in range(1, 4):
            for j in range(1, shard_i + 1):
                send_metrics(self._url + '/' + str(shard_i), self.metrics[j])

    def read_rest_of_data(self, agent, continuation_token):
        return read_multishard_data(agent, continuation_token)

    # Checks that:
    # 1. Agent responds with data according to a passed continuation token
    def check_rest_of_data(self, agent, continuation_token):
        new_continuation_token, rest_of_data = self.read_rest_of_data(agent, continuation_token)

        for shard_i in range(1, 4):
            service = 'shard_' + str(shard_i)
            shard_key = ShardKey(project='solomon', service=service, cluster='')

            expected_metric = {
                "kind": KIND,
                "labels": LABELS,
            }

            if shard_i == 1:
                expected_metric['value'] = 1
            else:
                expected_metric['timeseries'] = [
                    # [1, 2], [1, 2, 3]
                    {'value': j} for j in range(1, shard_i + 1)
                ]

            expected_obj = {
                "sensors": [
                    expected_metric
                ]
            }

            shard_data = rest_of_data[shard_key]
            assert are_metrics_equal(json.loads(shard_data[0]), expected_obj)

    def test_multishard_endpoint(self, agent):
        self.init_data()

        self.send_initial_data()
        continuation_token = self.check_initial_data(agent)
        self.send_rest_of_data()
        self.check_rest_of_data(agent, continuation_token)


class TestVirtualShardsInPull(object):
    HTTP_SERVER_CONF = """
        Shards {
            Project: "solomon"
            Service: "test_multi"
            PreserveOriginal: true

            ShardKeyOverride {
                Project: "{{cloud_id}}"
                Cluster: "{{cluster_id}}"
                Service: "{{folder_id}}"
            }
        }
    """
    HTTP_PUSH_CONF = """
        HttpPush {
            BindAddress: "127.0.0.1"
            BindPort: %(port)s
            Name: "httpPush"

            Handlers {
                Project: "solomon"
                Service: "test_multi"

                Endpoint: "/multi"
            }
        }
    """
    CONF_OVERRIDES = {}
    AGENT_SERVER_PORT = 0

    @pytest.fixture(autouse=True)
    def solomon_pull_conf(self, request):
        with PortManager() as net:
            self.AGENT_SERVER_PORT = net.get_port()
            self._url = 'http://127.0.0.1:' + str(self.AGENT_SERVER_PORT)

            http_server_conf = Parse(self.HTTP_SERVER_CONF, THttpServerConfig())
            modules_conf = Parse(self.HTTP_PUSH_CONF % {'port': self.AGENT_SERVER_PORT}, TAgentModuleConfig())

            TestVirtualShardsInPull.CONF_OVERRIDES = {
                "HttpServer": http_server_conf,
                "Modules": [modules_conf],
            }

    def init_data(self):
        self.metrics = {
            "sensors": [
                {
                    "value": 1,
                    "kind": "COUNTER",
                    "labels": {
                        "key": "value",
                        "cloud_id": "1",
                        "folder_id": "1",
                        "cluster_id": "1",
                    }
                },
                {
                    "value": 2,
                    "kind": "COUNTER",
                    "labels": {
                        "key": "value",
                        "cloud_id": "2",
                        "folder_id": "2",
                        "cluster_id": "2",
                    }
                },
                {
                    "value": 3,
                    "kind": "COUNTER",
                    "labels": {
                        "key": "value",
                        "cloud_id": "3",
                        "folder_id": "3",
                        "cluster_id": "3",
                    }
                },
                # this metric should be skipped, because cloud_id and folder_id labels are not present
                {
                    "value": 4,
                    "kind": "COUNTER",
                    "labels": {
                        "key": "value",
                        "cluster_id": "new_cluster_value"
                    }
                }
            ]
        }

        self.expected_metrics = []
        for i in range(1, 5):
            self.expected_metrics.append({'sensors': [
                {
                    "value": i,
                    "kind": "COUNTER",
                    "labels": {
                        "key": "value",
                    }
                }
            ]})

    def send_data(self):
        send_metrics(self._url + '/multi', self.metrics)

    def read_data(self, agent):
        _, data = read_multishard_data(agent)

        # one metric was skipped
        assert len(data) == 4, 'wrong resulting number of shards'

        for i in range(1, 4):
            shard_key = ShardKey(project=str(i), service=str(i), cluster=str(i))
            assert shard_key in data, 'no shard with a key: ' + str(shard_key)
            assert are_metrics_equal(json.loads(data[shard_key][0]), self.expected_metrics[i - 1])

        # original source shard
        shard_key = ShardKey(project='solomon', service='test_multi', cluster='')
        assert shard_key in data
        assert are_metrics_equal(json.loads(data[shard_key][0]), self.metrics)

    def test_virtual_shards(self, agent):
        self.init_data()
        self.send_data()
        self.read_data(agent)
