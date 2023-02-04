from __future__ import print_function, unicode_literals

import sys  # noqa
import time
import socket

import pytest

from solomon.agent.protos.modules_config_pb2 import TAgentModuleConfig
from google.protobuf.text_format import Parse

from yatest.common.network import PortManager


class GraphiteClient(object):
    def __init__(self, host, port):
        self._address = (host, port)

    def send(self, data):
        if isinstance(data, list):
            data = '\n'.join(data)

        self._sock = socket.create_connection(self._address)
        self._send_data(data)

        time.sleep(0.5)

    def _send_data(self, data):
        self._sock.sendall(data)
        self._sock.close()


class GraphiteClientSlow(GraphiteClient):
    def _send_data(self, data):
        for c in data:
            self._sock.send(c)

        self._sock.close()


class GraphiteTest(object):
    CONF_OVERRIDES = {}

    @pytest.fixture
    def graphite_module_conf(self, request):
        with PortManager() as net:
            module_conf = request.cls.GRAPHITE_MODULE_CONF % {
                'port': net.get_port()}

            module = Parse(module_conf, TAgentModuleConfig())
            GraphiteTest.CONF_OVERRIDES = {"Modules": [module]}
            yield module

    @pytest.fixture(params=[GraphiteClient, GraphiteClientSlow])
    def graphite_agent(self, request, graphite_module_conf, agent):
        return agent, request.param(
            graphite_module_conf.GraphitePush.BindAddress,
            graphite_module_conf.GraphitePush.BindPort)


class TestGraphiteLoose(GraphiteTest):
    GRAPHITE_MODULE_CONF = """GraphitePush {
            BindAddress: "127.0.0.1"
            BindPort: %(port)s
            ThreadCount: 4
            Name: "myGraphite"

            Service: "test-service"
            Project: "test-project"

            MappingRules {
                Mode: AS_IS
            }
        }"""

    def test_graphite_send_one(self, graphite_agent):
        storage, graphite = graphite_agent

        graphite.send('test.metric 0 0')
        data = storage.read(
            params={'project': 'test-project', 'service': 'test-service'})
        assert len(data['sensors']) == 1
        metric = data['sensors'][0]
        assert metric['kind'] == 'GAUGE'
        assert metric['labels'] == {'sensor': 'test.metric'}

    def test_graphite_send_list(self, graphite_agent):
        storage, graphite = graphite_agent

        graphite.send(
            ['test.metric 0 0', 'test.metric 0 1', 'test.metric 0 2'])
        data = storage.read(
            params={'project': 'test-project', 'service': 'test-service'})
        assert len(data['sensors']) == 1
        metric = data['sensors'][0]
        assert len(metric['timeseries']) == 3

    def test_graphite_send_broken_format(self, graphite_agent):
        storage, graphite = graphite_agent

        graphite.send(
            ['test.metric,0,0', 'test.metric 0 1', 'test.metric 0 2'])

        data = storage.read(
            params={'project': 'test-project', 'service': 'test-service'})
        assert data == {}

    def test_large_request(self, graphite_agent):
        storage, graphite = graphite_agent

        data = ['foo.bar{0} {1} {2}'.format(x, x, x) for x in xrange(16000)]

        graphite.send(data)

        read = storage.read(
            params={'project': 'test-project', 'service': 'test-service'})

        read_names = {s['labels']['sensor'] for s in read['sensors']}
        expected_names = {s.split(' ')[0] for s in data}

        assert read_names == expected_names


TEMPLATED_CONF = """GraphitePush {
        BindAddress: "127.0.0.1"
        BindPort: %(port)s
        ThreadCount: 4
        Name: "myGraphite"

        Service: "test-service"
        Project: "test-project"

        MappingRules {
            Mode: %(mode)s
            Metrics {
                Pattern: "*.foo.*"
                LabelMappings {
                    Label: "label1"
                    Template: "$1"
                }

                LabelMappings {
                    Label: "label2"
                    Template: "$2_with_string"
                }

                LabelMappings {
                    Label: "constLabel"
                    Template: "helloWorld"
                }
            }
            Blacklist {
                Pattern: "blacklisted.*.*.*"
                Pattern: "bar.*"
                Pattern: "geo.taxi.*.*.cpuload.*"
                Pattern: "geo.taxi.*.*.diskstat.*"
                Pattern: "geo.taxi.*.*.diskusage.*"
                Pattern: "geo.taxi.*.*.iostat.*"
                Pattern: "geo.taxi.*.*.la.*"
                Pattern: "geo.taxi.*.*.meminfo.*"
                Pattern: "geo.taxi.*.*.net_snmp.*"
                Pattern: "geo.taxi.*.*.netstat.*"
                Pattern: "geo.taxi.*.*.sockstat.*"
            }
        }
    }"""


class TestGraphiteStrict(GraphiteTest):
    GRAPHITE_MODULE_CONF = TEMPLATED_CONF % (
        {'mode': 'IGNORE', 'port': '%(port)s'})

    def test_graphite_unmatched_no_error(self, graphite_agent):
        storage, graphite = graphite_agent

        graphite.send(
            ['test.metric 0 0', 'test.metric 0 1', 'test.metric 0 2'])

        data = storage.read(
            params={'project': 'test-project', 'service': 'test-service'})
        assert data == {}

    def test_ignore_conversion(self, graphite_agent):
        storage, graphite = graphite_agent

        graphite.send(['prefix.foo.suffix 0 100', 'prefix.foo.suffix 0 101',
                       'prefix2.foo.suffix 1 102', 'foo.bar 2 200'])

        data = storage.read(
            params={'project': 'test-project', 'service': 'test-service'})
        assert len(data['sensors']) == 2

        expected_data = {'sensors': [
            {'kind': 'GAUGE', 'labels': {'label1': 'prefix', 'label2': 'suffix_with_string', 'constLabel': 'helloWorld'},
                'timeseries': [{'ts': 100, 'value': 0}, {'ts': 101, 'value': 0}]},
            {'kind': 'GAUGE', 'labels': {'label1': 'prefix2', 'label2': 'suffix_with_string', 'constLabel': 'helloWorld'},
                'ts': 102, 'value': 1}
        ]}

        assert data == expected_data


class TestGraphiteBlacklist(GraphiteTest):
    GRAPHITE_MODULE_CONF = TEMPLATED_CONF % (
        {'mode': 'AS_IS', 'port': '%(port)s'})

    def test_blacklist(self, graphite_agent):
        storage, graphite = graphite_agent

        graphite.send(['blacklisted.test.metric.foo 0 0', 'bar.metric 0 1', 'test.foo.metric 2 1', 'test.foo.metric 3 2',
                       'geo.taxi.taxi_unstable_freeswitch.freeswitch-sas-01_taxi_dev_yandex_net.cpuload.user 0 0',
                       'geo.taxi.taxi_unstable_freeswitch.freeswitch-sas-01_taxi_dev_yandex_net.cpuload.nice 100 500',
                       'geo.taxi.taxi_unstable_freeswitch.freeswitch-sas-01_taxi_dev_yandex_net.cpuload.system 200 200',
                       'geo.taxi.taxi_unstable_freeswitch.freeswitch-sas-01_taxi_dev_yandex_net.cpuload.idle 300 400',
                       'geo.taxi.taxi_unstable_freeswitch.freeswitch-sas-01_taxi_dev_yandex_net.cpuload.iowait 1 4',
                       'geo.taxi.taxi_unstable_freeswitch.freeswitch-sas-01_taxi_dev_yandex_net.cpuload.hard_irq 1 3',
                       'geo.taxi.taxi_unstable_freeswitch.freeswitch-sas-01_taxi_dev_yandex_net.cpuload.soft_irq 2 5'])

        data = storage.read(
            params={'project': 'test-project', 'service': 'test-service'})
        assert len(data['sensors']) == 1

        expected_data = {'sensors': [
            {'kind': 'GAUGE', 'labels': {'label1': 'test', 'label2': 'metric_with_string', 'constLabel': 'helloWorld'},
                'timeseries': [{'ts': 1, 'value': 2}, {'ts': 2, 'value': 3}]},
        ]}

        assert data == expected_data
