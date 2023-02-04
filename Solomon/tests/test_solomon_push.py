from __future__ import print_function

import sys  # noqa
import time
import json
from urlparse import parse_qs
from collections import defaultdict

from tempfile import NamedTemporaryFile

import pytest
import yatest

from google.protobuf.text_format import Parse
from yatest.common.network import PortManager
from http_server import HttpServer, HttpHandler

from solomon.agent.protos.loader_config_pb2 import TConfigLoaderConfig
from solomon.agent.protos.push_config_pb2 import TPushConfig

from library.python.monlib.encoder import loads


CONF_PATH = yatest.common.test_source_path('data/test_push.conf')

SERVICE_CONF_TEMPLATE = """
    Project: "%(project)s"
    Service: "%(service)s"

    PullInterval: "1s"

    Modules: [
        {
            Python2: {
                FilePath: "data/config_loader.py"
                ModuleName: "test_module_for_push"
                ClassName: "TestPullModuleForPushMode"
                Params: [
                    {
                        key: "start_value"
                        value: "%(start_value)s"
                    }
                ]
            }
        }
    ]
"""

PUSH_CONF = """
    Hosts: [
        {
            Host: "127.0.0.1"
            Port: %(port)s
        }
    ],
    AllShards: true
    Cluster: "solomon_push_tests"
    PushInterval: "1s"
    RetryInterval: "5s"
    RetryTimes: 3
"""


POST_DATA = []
REQUEST_IDS = []
REQ_CNT = {
    3: 0,
    5: 0,
    7: 0,
}


class MyHandlerPOST(HttpHandler):
    def do_POST(self):
        qs = parse_qs(self.path)

        if 'Content-Type' not in self.headers:
            # because a failed assertion stays unnoticed
            raise Exception('No "Content-Type" header in the request')

        if self.headers['Content-Type'] != 'application/x-solomon-spack':
            raise Exception('Unknown metric Content-Type: %(content_type)s' % {
                'content_type': self.headers['Content-Type'],
            })

        length = int(self.headers["Content-Length"])
        request = self.rfile.read(length)
        data = loads(request)

        global POST_DATA
        global REQUEST_IDS
        global REQ_CNT

        data = json.loads(data)
        for metric in data['sensors']:
            values = []
            if 'value' in metric:
                values = [metric['value']]
            elif 'timeseries' in metric:
                values = [x['value'] for x in metric['timeseries']]
            else:
                raise Exception('Unknown metrics format')

            first_value = values[0]
            factor = None
            if first_value % 3 == 0:
                factor = 3
            if first_value % 5 == 0:
                factor = 5
            if first_value % 7 == 0:
                factor = 7

            for value in values:
                assert value % factor == 0

            POST_DATA.append({
                'project': qs['project'][0],
                'service': qs['service'][0],
                'request_id': qs['requestId'][0],
                'metric': metric,
            })
            REQUEST_IDS.append(qs['requestId'][0])

            REQ_CNT[factor] += 1

        less_frequent_factor = 3
        for factor in REQ_CNT.keys():
            if REQ_CNT[factor] < REQ_CNT[less_frequent_factor]:
                less_frequent_factor = factor

        if REQ_CNT[less_frequent_factor] > 2:
            self.send_response(200)
            self.send_header('content-type', 'application/text')
            self.end_headers()
            self.wfile.write('ok')
        else:
            self.send_response(500)
            self.send_header('content-type', 'application/text')
            self.end_headers()
            self.wfile.write('internal error')


class TestSolomonPush(object):
    CONF_OVERRIDES = {}

    @pytest.fixture(autouse=True)
    def solomon_push_conf(self, request):
        loader_conf = """
            FileLoader {
                UpdateInterval: "1h"
                ConfigsFiles: [
                    "%(file_1)s",
                    "%(file_2)s",
                    "%(file_3)s"
                ]
            }
        """

        with PortManager() as net, \
             NamedTemporaryFile(dir=yatest.common.output_path()) as service_config_1, \
             NamedTemporaryFile(dir=yatest.common.output_path()) as service_config_2, \
             NamedTemporaryFile(dir=yatest.common.output_path()) as service_config_3:

            port_POST = net.get_port()

            service_config_1.write(SERVICE_CONF_TEMPLATE % {
                'project': 'solomon_1',
                'service': 'test_push_1',
                'start_value': '3',
            })
            service_config_2.write(SERVICE_CONF_TEMPLATE % {
                'project': 'solomon_2',
                'service': 'test_push_2',
                'start_value': '5',
            })
            service_config_3.write(SERVICE_CONF_TEMPLATE % {
                'project': 'solomon_3',
                'service': 'test_push_3',
                'start_value': '7',
            })
            service_config_1.flush()
            service_config_2.flush()
            service_config_3.flush()

            POST_srv = HttpServer(port_POST, MyHandlerPOST)

            loader = Parse(
                loader_conf % ({
                    'file_1': service_config_1.name,
                    'file_2': service_config_2.name,
                    'file_3': service_config_3.name,
                }),
                TConfigLoaderConfig()
            )
            push_config = Parse(PUSH_CONF % {'port': port_POST}, TPushConfig())

            TestSolomonPush.CONF_OVERRIDES = {
                "ConfigLoader": loader,
                "Push": push_config,
            }

            try:
                POST_srv.start()
                yield loader
            finally:
                POST_srv.stop()

    def check_project_data(self, projects_data, project, service, factor):
        label_value = str(factor) + 'x'
        values_cnt = defaultdict(int)
        values_req_id = defaultdict(str)
        is_repeated = False

        assert len(projects_data) > 0, "empty projects data"

        for data in projects_data[project]:
            metric = data['metric']
            values = []
            if 'value' in metric:
                values = [metric['value']]
            elif 'timeseries' in metric:
                values = [x['value'] for x in metric['timeseries']]

            assert data['project'] == project
            assert data['service'] == service
            assert metric['labels']['sensor'] == label_value

            for metric_value in values:
                assert metric_value % factor == 0

                values_cnt[metric_value] += 1
                if values_cnt[metric_value] > 1:
                    is_repeated = True
                    assert values_req_id[metric_value] == data['request_id']
                else:
                    values_req_id[metric_value] = data['request_id']

        assert is_repeated is True

    def test_solomon_push(self, agent):
        # TODO: the whole test is probably too complicated - make it more understandable
        time.sleep(10)

        projects_data = defaultdict(list)

        for data in POST_DATA:
            projects_data[data['project']].append(data)

        self.check_project_data(projects_data, 'solomon_1', 'test_push_1', 3)
        self.check_project_data(projects_data, 'solomon_2', 'test_push_2', 5)
        self.check_project_data(projects_data, 'solomon_3', 'test_push_3', 7)
