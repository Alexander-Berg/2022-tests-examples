from __future__ import print_function

import time
import sys  # noqa

import yatest.common


CONF_PATH = yatest.common.test_source_path('data/loader_test.conf')


def test_python_loader_module_unload(agent):
    data = agent.management_request_json('/modules/json', None)

    assert len(data) == 2
    assert {d['Name'] for d in data} == {'test.TestPullModule-foo_42', 'test.TestPullModule-foo_43'}

    time.sleep(4)

    data = agent.management_request_json('/modules/json', None)

    assert len(data) == 1
    assert data[0]['Name'] == 'test.TestPullModule-foo_42'
    assert data[0]['SchedulerState'].lower() in ('executed', 'scheduled')
