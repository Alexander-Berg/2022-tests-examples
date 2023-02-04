from __future__ import print_function

import sys  # noqa
import json

from tempfile import NamedTemporaryFile

import pytest
import yatest

from google.protobuf.text_format import Parse
from yatest.common.network import PortManager

from http_server import HttpServer, HttpHandler
from solomon.agent.protos.loader_config_pb2 import TConfigLoaderConfig

import misc


SERVICE_CONF = """Project: "solomon"
Service: "test"

PullInterval: "1s"

Modules: [
    { Unistat : {
        Url: "http://127.0.0.1:%(port)s/"
    }}
]
"""

RESPONSE = """[
    ["signal1_max", 10],
    ["signal2_hgram", [[0, 100], [50, 200], [200, 300]]],
    ["prj=some-project;signal3_summ", 3],
    ["signal4_summ", 5]
]"""


# TODO: there should be "timeseries" instead of "value" when RATE and
# HIST_RATE processing will be fixed
EXPECTED = json.loads("""{
  "sensors": [
    {
      "kind": "GAUGE",
      "labels": {
        "sensor": "signal1_max"
      },
      "timeseries": [
        {"value": 10},
        {"value": 10}
      ]
    },
    {
      "hist": {
        "buckets": [
          0,
          100,
          200
        ],
        "bounds": [
          0,
          50,
          200
        ],
        "inf": 300
      },
      "kind": "HIST_RATE",
      "labels": {
        "sensor": "signal2_hgram"
      }
    },
    {
      "kind": "RATE",
      "labels": {
        "sensor": "signal3_summ",
        "prj": "some-project"
      },
      "value": 3
    },
    {
      "kind": "RATE",
      "labels": {
        "sensor": "signal4_summ"
      },
      "value": 5
    }
  ]
}""")


class UnistatHandler(HttpHandler):
    def do_GET(self):
        self.send_response(200)
        self.send_header('content-type', 'application/json')
        self.end_headers()
        self.wfile.write(RESPONSE)


class TestUnistatPull(object):
    CONF_OVERRIDES = {}

    @pytest.fixture(autouse=True)
    def unistat_pull_conf(self, request):
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

            srv = HttpServer(port, UnistatHandler)
            loader = Parse(loader_conf % ({'file': service_config.name}), TConfigLoaderConfig())
            TestUnistatPull.CONF_OVERRIDES = {"ConfigLoader": loader}

            try:
                srv.start()
                yield loader
            finally:
                srv.stop()

    def test_unistat_pull(self, agent):
        agent.wait_till_pull_is_performed(1)

        data = agent.read(params={'project': 'solomon', 'service': 'test'})
        misc.remove_ts_from_data(data)

        assert data == EXPECTED
