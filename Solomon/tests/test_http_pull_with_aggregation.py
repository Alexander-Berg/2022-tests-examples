from __future__ import print_function

import sys  # noqa
import time
import json

from tempfile import NamedTemporaryFile
from copy import deepcopy

import pytest
import yatest

from google.protobuf.text_format import Parse
from yatest.common.network import PortManager
from http_server import HttpServer, HttpHandler

from solomon.agent.protos.storage_config_pb2 import TStorageConfig
from solomon.agent.protos.loader_config_pb2 import TConfigLoaderConfig

import misc


PULL_INTERVAL_SECONDS = 3
AGGREGATION_INTERVAL_SECONDS = 5

SERVICE_CONF = """Project: "solomon"
Service: "%(service)s"

PullInterval: "%(interval)ss"

Modules: [
    { HttpPull : {
        Url: "http://127.0.0.1:%(port)s/"
    }}
]
"""

METRICS = {
    "sensors": [
        {
            "value": 4.5,
            "labels": {
                "name": "gauge_with_ts"
            },
            "kind": "GAUGE"
        },
        {
            "value": 4.5,
            "labels": {
                "name": "gauge_without_ts"
            },
            "kind": "GAUGE"
        },
        {
            "value": 23,
            "labels": {
                "name": "igauge_with_ts"
            },
            "kind": "IGAUGE"
        },
        {
            "value": 23,
            "labels": {
                "name": "igauge_without_ts"
            },
            "kind": "IGAUGE"
        },
        {
            "labels": {
                "name": "hist_with_ts"
            },
            "kind": "HIST",
            "hist": {
                "bounds": [1, 5, 8],
                "buckets": [2, 3, 4],
                "inf": 0
            }
        },
        {
            "labels": {
                "name": "hist_without_ts"
            },
            "kind": "HIST",
            "hist": {
                "bounds": [1, 5, 8],
                "buckets": [2, 3, 4],
                "inf": 0
            }
        },
        # Note! These metrics will not be processed at all. See https://ya.cc/5UGsp for more info
        {
            "value": 1,
            "labels": {
                "name": "counter_with_ts"
            },
            "kind": "COUNTER"
        },
        {
            "value": 2,
            "labels": {
                "name": "counter_without_ts"
            },
            "kind": "COUNTER"
        },
        {
            "value": 3,
            "labels": {
                "name": "rate_without_ts"
            },
            "kind": "RATE"
        },
    ]
}


def create_expected(cnt):
    timeseries_first_coeff = (cnt + 1) + (cnt + 2)
    timeseries_second_coeff = (cnt + 3) + (cnt + 4)

    return {
        "sensors": [
            {
                "labels": {
                    "name": "gauge_with_ts"
                },
                "kind": "GAUGE",
                "timeseries": [
                    {
                        "value": timeseries_first_coeff * METRICS['sensors'][0]['value']
                    },
                    {
                        "value": timeseries_second_coeff * METRICS['sensors'][0]['value']
                    }
                ]
            },
            {
                "labels": {
                    "name": "gauge_without_ts"
                },
                "kind": "GAUGE",
                "timeseries": [
                    {
                        "value": timeseries_first_coeff * METRICS['sensors'][1]['value']
                    },
                    {
                        "value": timeseries_second_coeff * METRICS['sensors'][1]['value']
                    }
                ]
            },
            {
                "labels": {
                    "name": "igauge_with_ts"
                },
                "kind": "IGAUGE",
                "timeseries": [
                    {
                        "value": timeseries_first_coeff * METRICS['sensors'][2]['value']
                    },
                    {
                        "value": timeseries_second_coeff * METRICS['sensors'][2]['value']
                    }
                ]
            },
            {
                "labels": {
                    "name": "igauge_without_ts"
                },
                "kind": "IGAUGE",
                "timeseries": [
                    {
                        "value": timeseries_first_coeff * METRICS['sensors'][3]['value']
                    },
                    {
                        "value": timeseries_second_coeff * METRICS['sensors'][3]['value']
                    }
                ]
            },
            {
                "labels": {
                    "name": "hist_with_ts"
                },
                "kind": "HIST",
                "timeseries": [
                    {
                        "hist": {
                            "bounds": [1, 5, 8],
                            "buckets": [x * timeseries_first_coeff for x in METRICS['sensors'][4]['hist']['buckets']],
                            "inf": 0
                        }
                    },
                    {
                        "hist": {
                            "bounds": [1, 5, 8],
                            "buckets": [x * timeseries_second_coeff for x in METRICS['sensors'][4]['hist']['buckets']],
                            "inf": 0
                        }
                    }
                ]
            },
            {
                "labels": {
                    "name": "hist_without_ts"
                },
                "kind": "HIST",
                "timeseries": [
                    {
                        "hist": {
                            "bounds": [1, 5, 8],
                            "buckets": [x * timeseries_first_coeff for x in METRICS['sensors'][5]['hist']['buckets']],
                            "inf": 0
                        }
                    },
                    {
                        "hist": {
                            "bounds": [1, 5, 8],
                            "buckets": [x * timeseries_second_coeff for x in METRICS['sensors'][5]['hist']['buckets']],
                            "inf": 0
                        }
                    }
                ]
            },
            # TODO: Uncomment after the fix. See https://ya.cc/5UGsp for more info
            # {
            #     "labels": {
            #         "name": "counter_with_ts"
            #     },
            #     "kind": "COUNTER",
            #     "timeseries": [
            #         {
            #             "value": timeseries_first_coeff * METRICS['sensors'][0]['value']
            #         },
            #         {
            #             "value": timeseries_second_coeff * METRICS['sensors'][0]['value']
            #         },
            #     ]
            # },
            # {
            #     "labels": {
            #         "name": "counter_without_ts"
            #     },
            #     "kind": "COUNTER",
            #     "timeseries": [
            #         {
            #             "value": timeseries_first_coeff * METRICS['sensors'][1]['value']
            #         },
            #         {
            #             "value": timeseries_second_coeff * METRICS['sensors'][1]['value']
            #         }
            #     ]
            # },
            # # TODO: there should be "timeseries" instead of "value" when RATE and
            # #  HIST_RATE processing will be fixed
            # {
            #     # it's a RATE without a ts, therefore only the last value is stored
            #     "value": timeseries_second_coeff * METRICS['sensors'][2]['value'],
            #     "labels": {
            #         "name": "rate_without_ts"
            #     },
            #     "kind": "RATE"
            # },
        ]
    }

CNT = {
    'cnt1': 0,
    'cnt2': 3,
}

EXPECTED1 = create_expected(CNT['cnt1'])
EXPECTED2 = create_expected(CNT['cnt2'])


# Since BaseHttpServer creates a handler instance for every request, its state should be external
def CreateHandlerWithACounter(counter_name):
    class MyHandler(HttpHandler):
        cnt_name = counter_name

        def do_GET(self):
            global CNT
            CNT[self.cnt_name] += 1
            now_ts = time.time()

            metrics_to_send = deepcopy(METRICS)
            metrics_to_send['sensors'][0]['ts'] = int(now_ts)
            metrics_to_send['sensors'][2]['ts'] = int(now_ts)
            metrics_to_send['sensors'][4]['ts'] = int(now_ts)

            metrics_to_send['sensors'][0]['value'] = METRICS['sensors'][0]['value'] * CNT[self.cnt_name]
            metrics_to_send['sensors'][1]['value'] = METRICS['sensors'][1]['value'] * CNT[self.cnt_name]
            metrics_to_send['sensors'][2]['value'] = METRICS['sensors'][2]['value'] * CNT[self.cnt_name]
            metrics_to_send['sensors'][3]['value'] = METRICS['sensors'][3]['value'] * CNT[self.cnt_name]
            metrics_to_send['sensors'][4]['hist']['buckets'] =\
                [x * CNT[self.cnt_name] for x in METRICS['sensors'][4]['hist']['buckets']]
            metrics_to_send['sensors'][5]['hist']['buckets'] =\
                [x * CNT[self.cnt_name] for x in METRICS['sensors'][5]['hist']['buckets']]

            msg = json.dumps(metrics_to_send)

            self.send_response(200)
            self.send_header('content-type', 'application/json')
            self.end_headers()
            self.wfile.write(msg)

    return MyHandler


class TestHttpPullAgg(object):
    CONF_OVERRIDES = {}

    @pytest.fixture(autouse=True)
    def solomon_pull_conf(self, request):
        storage_conf = """
            AggregationOptions {
                AggregationInterval: "%(interval)ss"
            }
        """
        loader_conf = """
            FileLoader {
                UpdateInterval: "1h"
                ConfigsFiles: [
                    "%(file1)s",
                    "%(file2)s"
                ]
            }
        """

        with PortManager() as net, \
             NamedTemporaryFile(dir=yatest.common.output_path()) as service_config1, \
             NamedTemporaryFile(dir=yatest.common.output_path()) as service_config2:
            port1 = net.get_port()
            port2 = net.get_port()

            service_config1.write(SERVICE_CONF % {'port': port1, 'interval': PULL_INTERVAL_SECONDS, 'service': 'test1'})
            service_config1.flush()
            service_config2.write(SERVICE_CONF % {'port': port2, 'interval': PULL_INTERVAL_SECONDS, 'service': 'test2'})
            service_config2.flush()

            srv1 = HttpServer(port1, CreateHandlerWithACounter('cnt1'))
            srv2 = HttpServer(port2, CreateHandlerWithACounter('cnt2'))

            storage = Parse(storage_conf % {'interval': AGGREGATION_INTERVAL_SECONDS}, TStorageConfig())
            loader = Parse(loader_conf % ({'file1': service_config1.name, 'file2': service_config2.name}),
                           TConfigLoaderConfig())
            TestHttpPullAgg.CONF_OVERRIDES = {"ConfigLoader": loader, "Storage": storage}

            try:
                srv1.start()
                srv2.start()
                yield loader
            finally:
                srv1.stop()
                srv2.stop()

    def test_solomon_pull_with_aggregation(self, agent):
        time.sleep(1)
        time.sleep(11)

        data1 = agent.read(params={'project': 'solomon', 'service': 'test1'})
        data2 = agent.read(params={'project': 'solomon', 'service': 'test2'})

        misc.remove_ts_from_data(data1)
        misc.remove_ts_from_data(data2)

        # Test that two shards can be processed simultaneously
        assert misc.are_metrics_equal(data1, EXPECTED1)
        assert misc.are_metrics_equal(data2, EXPECTED2)
