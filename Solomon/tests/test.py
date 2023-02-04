#!/usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import print_function

import json
import pytest
import requests
import sys  # noqa
import time

from collections import defaultdict

from solomon.agent.protos.loader_config_pb2 import TConfigLoaderConfig
from google.protobuf.text_format import Parse

import yatest.common
import misc


CONF_PATH = yatest.common.test_source_path('data/test.conf')


def wait_for_agent_data(intervalSec):
    def decorator(fn):
        def wrapper(agent, *args, **kwargs):
            agent.ensure_has_data(intervalSeconds=intervalSec)

            return fn(agent, *args, **kwargs)
        return wrapper
    return decorator


@wait_for_agent_data(5)
def test_agent_find_works(agent):
    data = agent.find(params={'project': 'solomon', 'service': 'test'})
    assert data['sensors'][0]['kind'] == 'COUNTER'
    assert data['sensors'][0]['labels']['sensor'] == 'call_count'


@wait_for_agent_data(5)
def test_agent_read_works(agent):
    data = agent.read(params={'project': 'solomon', 'service': 'test'})
    assert data['sensors'][0]['kind'] == 'COUNTER'
    assert data['sensors'][0]['labels']['sensor'] == 'call_count'


@wait_for_agent_data(5)
def test_agent_read_no_seqno(agent):
    data, headers = agent.read_raw(params={'project': 'solomon', 'service': 'test'})

    assert len(data['sensors']) == 1
    assert int(headers['x-solomon-nextsequencenumber']) == 2

    # we don't expect our offset to be committed unless sequence number is specified
    data = agent.read(params={'project': 'solomon', 'service': 'test'})

    assert len(data['sensors']) == 1
    assert int(headers['x-solomon-nextsequencenumber']) == 2


@wait_for_agent_data(5)
def test_agent_read_sequential(agent):
    data, headers = agent.read_raw(params={'project': 'solomon', 'service': 'test'},
                                   headers={'x-solomon-sequencenumber': '0'})

    assert len(data['sensors']) == 1
    next_seqno = headers['x-solomon-nextsequencenumber']
    assert int(next_seqno) == 2
    assert headers['x-solomon-hasmore'] in ('0', 'false', 'f')

    data = agent.read(params={'project': 'solomon', 'service': 'test'},
                      headers={'x-solomon-sequencenumber': next_seqno})

    assert 'sensors' not in data


@wait_for_agent_data(5)
def test_agent_read_after_restart(agent):
    data = agent.read(params={'project': 'solomon', 'service': 'test'},
                      headers={'x-solomon-sequencenumber': '100', 'X-Solomon-FetcherId': 'bar'})

    assert len(data['sensors']) == 1


@wait_for_agent_data(5)
def agent_get_all_ts(agent, seqno, metric_to_ts):
    data, headers = agent.read_raw(
        params={'project': 'solomon', 'service': 'test'},
        headers={'X-Solomon-SequenceNumber': str(seqno)}
    )

    seqno = headers['x-solomon-nextsequencenumber']

    assert 'sensors' in data
    metrics = data['sensors']

    for metric in metrics:
        assert ('ts' in metric or 'timeseries' in metric)

        labels = metric['labels']
        metric_id = '&'.join('{}={}'.format(key, labels[key]) for key in sorted(labels))

        if 'ts' in metric:
            assert metric['ts'] not in metric_to_ts[metric_id]

            metric_to_ts[metric_id].add(metric['ts'])

        if 'timeseries' in metric:
            for point in metric['timeseries']:
                assert point['ts'] not in metric_to_ts[metric_id]

                metric_to_ts[metric_id].add(point['ts'])

    return seqno


def test_agent_read_different_ts_for_different_seqno(agent):
    prev_metrics = defaultdict(set)
    curr_metrics = defaultdict(set)

    seqno = 0
    tries = 2

    seqno = agent_get_all_ts(agent, seqno, prev_metrics)

    for i in xrange(tries):
        time.sleep(11)
        seqno = agent_get_all_ts(agent, seqno, curr_metrics)

        for metric_id in curr_metrics:
            prev_ts = prev_metrics[metric_id]
            curr_ts = curr_metrics[metric_id]

            assert len(prev_ts & curr_ts) == 0

            prev_metrics[metric_id] = prev_ts | curr_ts

        curr_metrics = defaultdict(set)


@pytest.mark.parametrize(
    'value', ['-5', 'foo']
)
def test_bad_header_values(agent, value):
    with pytest.raises(requests.HTTPError):
        agent.read(params={'project': 'solomon', 'service': 'test'}, headers={'x-solomon-sequencenumber': value})


@wait_for_agent_data(5)
def test_management_server(agent):
    agent.read(params={'project': 'solomon', 'service': 'test'})

    data = agent.management_request_json('/counters/json', None)
    assert 'sensors' in data
    metric_names = {s['labels']['sensor'] for s in data['sensors']}

    # check that metrics from different components are written as expected
    assert 'execTimeMs' in metric_names
    assert 'http.storage.find.responseTimeMillis' in metric_names
    assert 'storage.pointCount' in metric_names
    assert 'process.memRssBytes' in metric_names


CONF_LOADER_TMPL = """
Python2Loader {
    UpdateInterval: "30s"
    FilePath: "data/config_loader.py"
    ModuleName: "solomon",
    ClassName: "%s"
}
"""


class TestModuleCancelling(object):
    @pytest.fixture(autouse=True)
    def setup(self, request):
        conf_loader = CONF_LOADER_TMPL % "TestCancellingConfigLoader"
        loader = Parse(conf_loader, TConfigLoaderConfig())
        TestModuleCancelling.CONF_OVERRIDES = {"ConfigLoader": loader}

    def test_module_cancelling(self, agent):
        agent.wait_till_pull_is_performed(5)

        modules_data = agent.management_request_json('/modules/json', None)

        assert len(modules_data) == 1
        assert modules_data[0]['Name'] == 'test.TestPullModuleCancelling'
        assert modules_data[0]['SchedulerState'].lower() == 'cancelled'

        module_metric_exists = False
        modules_metrics = agent.management_request_json('/counters/json', None)
        for metric in modules_metrics['sensors']:
            if metric['labels'].get('module', '') == 'test.TestPullModuleCancelling':
                module_metric_exists = True
        assert module_metric_exists is True

        time.sleep(5)

        modules_data = agent.management_request_json('/modules/json', None)
        assert len(modules_data) == 0

        modules_metrics = agent.management_request_json('/counters/json', None)
        for metric in modules_metrics['sensors']:
            assert metric['labels'].get('module', '') != 'test.TestPullModuleCancelling'


class TestPythonHistograms(object):
    EXPECTED = json.loads("""
{
  "sensors": [
    {
      "hist": {
        "buckets": [
          1,
          0,
          0
        ],
        "bounds": [
          10,
          50,
          100
        ],
        "inf": 0
      },
      "kind": "HIST",
      "labels": {
        "sensor": "hist_counter_explicit"
      }
    },
    {
      "hist": {
        "buckets": [
          0,
          0,
          0,
          0,
          0
        ],
        "bounds": [
          5,
          20,
          35,
          50,
          65
        ],
        "inf": 1
      },
      "kind": "HIST_RATE",
      "labels": {
        "sensor": "hist_rate_linear"
      }
    },
    {
      "hist": {
        "buckets": [
          0,
          0,
          0,
          0,
          0
        ],
        "bounds": [
          3,
          6,
          12,
          24,
          48
        ],
        "inf": 5
      },
      "kind": "HIST_RATE",
      "labels": {
        "sensor": "hist_rate_exponential"
      }
    }
  ]
}
    """)

    @pytest.fixture(autouse=True)
    def setup(self, request):
        conf_loader = CONF_LOADER_TMPL % "TestHistConfigLoader"
        loader = Parse(conf_loader, TConfigLoaderConfig())
        TestPythonHistograms.CONF_OVERRIDES = {"ConfigLoader": loader}

    def test_python_histograms(self, agent):
        agent.ensure_has_data(intervalSeconds=5)
        data = agent.read(params={'project': 'solomon', 'service': 'test'})

        misc.are_metrics_equal(data, TestPythonHistograms.EXPECTED)
