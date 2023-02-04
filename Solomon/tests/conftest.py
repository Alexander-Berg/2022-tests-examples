from __future__ import print_function

import json
import os
import socket
import sys  # noqa
import tempfile
import time
from fractions import gcd

import pytest
import requests

import yatest.common

from yatest.common.network import PortManager

from solomon.agent.protos.agent_config_pb2 import TAgentConfig
from solomon.libs.multi_shard.proto.multi_shard_pb2 import TMultiShardRequest
from google.protobuf.text_format import MessageToString, Parse


class Agent(object):
    def __init__(self, port, mgmt_port):
        self._port = port
        self._management_port = mgmt_port
        self._url = 'http://127.0.0.1:' + str(port)
        self._headers = {'X-Solomon-FetcherId': 'foo'}

    def _do_request_raw(self, url, params, headers, method='GET'):
        headers_full = self._headers.copy()
        if headers is not None:
            headers_full.update(**headers)

        response = requests.request(method, url, params=params, headers=headers_full)
        response.raise_for_status()

        return response

    def _do_request_raw_json(self, url, params, headers, method='GET'):
        response = self._do_request_raw(url, params, headers, method)
        return json.loads(response.text), response.headers

    def request_endpoint(self, endpoint, params, headers):
        response = self._do_request_raw(self._url + endpoint, params=None, headers=None)
        return response.text

    def read_raw(self, params, headers=None):
        return self._do_request_raw_json(self._url + '/storage/read', params, headers)

    def read_all(self, continuation_token='', response_format='JSON'):
        params = {
            'format': response_format,
        }

        headers = {
            'Content-Type': 'application/octet-stream',
            'X-Solomon-FetcherId': 'localFetcher',
        }

        request = TMultiShardRequest()
        request.ContinuationToken = continuation_token
        payload = request.SerializeToString()

        response = requests.request('POST', self._url + '/storage/readAll', params=params, headers=headers, data=payload)
        response.raise_for_status()

        return response.content

    def read(self, params, headers=None):
        j, _ = self.read_raw(params, headers)
        return j

    def find_raw(self, params, headers=None):
        return self._do_request_raw_json(self._url + '/storage/find', params, headers)

    def find(self, params, headers=None):
        j, _ = self.find_raw(params, headers)
        return j

    def management_request(self, path, params, headers=None):
        response = self._do_request_raw('http://127.0.0.1:' + str(self._management_port) + path, params, headers)
        return response.text

    def management_request_json(self, path, params, headers=None):
        data, _ = self._do_request_raw_json('http://127.0.0.1:' + str(self._management_port) + path, params, headers)
        return data

    @property
    def ready(self):
        try:
            socket.create_connection(('127.0.0.1', self._port))
        except socket.error:
            return False

        return True

    def wait_till_pull_is_performed(self, intervalSeconds=15):
        toSleep = intervalSeconds - (int(time.time()) % intervalSeconds)
        toSleep += 1
        time.sleep(toSleep)

    def ensure_has_data(self, max_tries=10, intervalSeconds=15):
        self.wait_till_pull_is_performed(intervalSeconds=intervalSeconds)

        tries = 0

        while tries < max_tries:
            data = self.find(params={'project': 'solomon', 'service': 'test'})
            if 'sensors' in data:
                return

            tries += 1
            time.sleep(0.1)

        raise RuntimeError('failed to initialize agent')


def make_config(source, dest, ports, overrides):
    config = Parse(open(source).read(), TAgentConfig())

    for attr_name, value in overrides.items():
        attr = getattr(config, attr_name)

        # for repeated fields like Modules
        if hasattr(attr, 'extend'):
            del attr[:]
            attr.extend(value)
        else:
            attr.CopyFrom(value)

    if config.HasField('HttpServer'):
        config.HttpServer.BindPort = ports['storage']

    if config.HasField('ManagementServer'):
        config.ManagementServer.BindPort = ports['mgmt']

    dest.write(MessageToString(config))
    dest.flush()

    return config


@pytest.fixture
def agent(request):
    BIN_PATH = yatest.common.binary_path('solomon/agent/bin/solomon-agent')

    OUT_FILE_PATH = yatest.common.output_path('captured.out')
    ERR_FILE_PATH = yatest.common.output_path('captured.err')
    CONF_PATH = yatest.common.test_source_path('data/test.conf')
    ENV = os.environ.copy()

    conf_path = getattr(request.module, "CONF_PATH", CONF_PATH)

    # a dict {"SectionName": TSectionProto}
    conf_overrides = getattr(request.cls, "CONF_OVERRIDES", {})

    assert conf_path is not None

    with PortManager() as net, \
        tempfile.NamedTemporaryFile(dir=yatest.common.output_path()) as config_file, \
            open(OUT_FILE_PATH, 'w') as capture_out, open(ERR_FILE_PATH, 'w') as capture_err:

        ports = {'storage': net.get_port(), 'mgmt': net.get_port()}

        config_obj = make_config(conf_path, config_file, ports, conf_overrides)

        call_args = [BIN_PATH, '--config', config_file.name]

        is_in_aggregation_mode = config_obj.HasField('Storage') and config_obj.Storage.HasField('AggregationOptions')

        if is_in_aggregation_mode:
            # Align aggregation and pulling time grids
            pull_interval = getattr(request.module, 'PULL_INTERVAL_SECONDS')
            aggr_interval = getattr(request.module, 'AGGREGATION_INTERVAL_SECONDS')

            lcm = (pull_interval * aggr_interval) / gcd(pull_interval, aggr_interval)
            toSleep = lcm - int(time.time()) % lcm

            if toSleep < 1:
                toSleep += lcm
            # First wait for a pulling process to start
            toSleep -= 1

            time.sleep(toSleep)

        p = yatest.common.execute(call_args,
                                  wait=False,
                                  check_exit_code=True, env=ENV,
                                  cwd=yatest.common.test_source_path(),
                                  stdout=capture_out, stderr=capture_err)

        try:
            agent = Agent(ports['storage'], ports['mgmt'])

            if config_obj.HasField('HttpServer'):  # in a pull mode (push otherwise SOLOMON-3636)
                while not agent.ready:
                    time.sleep(0.1)
                    if not p.running:
                        raise RuntimeError('Agent died with code ' + str(p.exit_code))

            yield agent
        finally:
            if p.running:
                p.terminate()
