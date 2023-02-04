from __future__ import print_function

import sys  # noqa
import time
import json

from tempfile import NamedTemporaryFile

import pytest
import yatest

from google.protobuf.text_format import Parse
from yatest.common.network import PortManager
from http_server import HttpServer, HttpHandler

from solomon.agent.protos.loader_config_pb2 import TConfigLoaderConfig


SERVICE_CONF = """Project: "solomon"
Service: "test"

PullInterval: "1s"

Modules: [
    { HttpPull : {
        Url: "http://127.0.0.1:%(port)s/"
    }}
]
"""

METRICS = {
    # TODO: there should be "timeseries" instead of "value" when RATE and
    # HIST_RATE processing will be fixed
    "sensors": [
        {
            "value": 5877799000,
            "labels": {
                "path": "/System/UpTime"
            },
            "kind": "RATE"
        },
        {
            "value": 8468336640,
            "labels": {
                "path": "/System/UserTime"
            },
            "kind": "RATE"
            }
    ]
}


class MyHandler(HttpHandler):
    def do_GET(self):
        msg = json.dumps(METRICS)

        self.send_response(200)
        self.send_header('content-type', 'application/json')
        self.end_headers()
        self.wfile.write(msg)


class TestHttpPull(object):
    CONF_OVERRIDES = {}

    @pytest.fixture(autouse=True)
    def solomon_pull_conf(self, request):
        loader_conf = """FileLoader {
                    UpdateInterval: "1h"
                    ConfigsFiles: [
                        "%(file)s"
                    ]
                }"""

        with PortManager() as net, NamedTemporaryFile(dir=yatest.common.output_path()) as service_config:
            port = net.get_port()

            service_config.write(SERVICE_CONF % {'port': port})
            service_config.flush()

            srv = HttpServer(port, MyHandler)
            loader = Parse(loader_conf % ({'file': service_config.name}), TConfigLoaderConfig())
            TestHttpPull.CONF_OVERRIDES = {"ConfigLoader": loader}

            try:
                srv.start()
                yield loader
            finally:
                srv.stop()

    def test_solomon_pull(self, agent):
        time.sleep(2)

        data = agent.read(params={'project': 'solomon', 'service': 'test'})

        assert data == METRICS
