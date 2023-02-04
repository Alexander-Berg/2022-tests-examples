import contextlib

import mock
import os
import pytest
from google.protobuf import text_format
from infra.ya_salt.proto import ya_salt_pb2

from infra.ya_salt.lib import statusutil
from infra.ya_salt.lib.output import status


@contextlib.contextmanager
def override_tz():
    os.environ['TZ'] = 'Europe/Moscow'
    yield
    os.putenv('TZ', '')


TEST_STATUS = """
apt {
  last_update_ok {
    status: "True"
    message: "OK"
    transition_time {
      seconds: 1541003244
      nanos: 583663000
    }
  }
  next_update_run_time {
    seconds: 1541513714
  }
  last_update_duration {
    seconds: 13
  }
}

yasm {
  agent_package {
    version: "2.221-20181102"
    last_update_ok {
      status: "True"
      message: "OK"
      transition_time {
        seconds: 1540994205
        nanos: 764016000
      }
    }
  }
  helper_package {
    version: "1.0-2"
    last_update_ok {
      status: "True"
      message: "OK"
      transition_time {
        seconds: 1540994205
        nanos: 732089000
      }
    }
  }
  revision_timestamp: 1542280576
}
salt {
  revision: "d704d1e50415a1527135b0418f46e329e5c00f57"
  revision_timestamp {
    seconds: 1547126358
  }
  started {
    seconds: 1547195578
    nanos: 140316000
  }
  last_run_duration {
    seconds: 16
    nanos: 44000000
  }
}
salt_components {
  salt {
    state_results {
        id: "/db/bsconfig/configactive"
        ok: true
        started {
          seconds: 1547195592
        }
        duration: 1.80599999428
        comment: "Directory /db/bsconfig/configactive is in the correct state"
      }
      state_results {
        id: "/place/porto_layers"
        ok: false
        started {
          seconds: 1547195592
        }
        duration: 1.60199999809
        comment: "Directory /place/porto_layers is in the correct state"
      }
  }
}
"""

TEST_OUT = """ --- Salt
   ---
   ID: /db/bsconfig/configactive
   Started: 2019-01-11 11:33:12
   Duration: 0.002s
   OK: True
   Comment: Directory /db/bsconfig/configactive is in the correct state
   ---
   ID: /place/porto_layers
   Started: 2019-01-11 11:33:12
   Duration: 0.002s
   OK: False
   Comment: Directory /place/porto_layers is in the correct state

   Count-Ok: 1
   Count-Fail: 1
   Count-Changed: 0
   Rev: d704d1e50415a1527135b0418f46e329e5c00f57
   Rev-Timestamp: 2019-01-10 16:19:18 (ts: 1547126358)
   Duration: 16.044s
   Compile-Ok: False
   Compile-Ok-Message: -
   Exec-Ok: False
   Exec-Ok-Message: -

   Initial-Setup-Passed: False
   Initial-Setup-Passed-Message: -
 --- Apt status
   Last-Update-Ok: True
   Last-Transition-Time: 2018-10-31 19:27:24
   Last-Update-Message: "OK"
   Last-Update-Duration: 13s.
   Next-Update-Time: 2018-11-06 17:15:14
 --- Kernel status
   Current-Version: unknown
   Need-Reboot: False
   Last-Transition-Time: -
   Need-Reboot-Message: ""
   Boot-Version: unknown
   Boot-Version-Error: -
   Boot-Version-Changed: -
 --- Yasm-Agent-Package
   Current-Version: 2.221-20181102
   Last-Update-Ok: True
   Last-Transition-Time: 2018-10-31 16:56:45
   Last-Update-Message: "OK"
 --- Yasm-Agent-Service:
   Current-Version: unknown
   Last-Check-Ok: False
   Last-Transition-Time: -
   Last-Check-Message: ""
"""

TEST_OUT_SHORT = """ --- Salt
  ID: /place/porto_layers
   Started: 2019-01-11 11:33:12
   Duration: 0.002s
   Comment: Directory /place/porto_layers is in the correct state

   Count-Ok: 1
   Count-Fail: 1
   Count-Changed: 0
   Rev: d704d1e50415a1527135b0418f46e329e5c00f57
   Rev-Timestamp: 2019-01-10 16:19:18 (ts: 1547126358)
   Duration: 16.044s
   Compile-Ok: False
   Compile-Ok-Message: -
   Exec-Ok: False
   Exec-Ok-Message: -

   Initial-Setup-Passed: False
   Initial-Setup-Passed-Message: -
 --- Apt status
   Last-Update-Ok: True
   Last-Transition-Time: 2018-10-31 19:27:24
   Last-Update-Message: "OK"
   Last-Update-Duration: 13s.
   Next-Update-Time: 2018-11-06 17:15:14
 --- Kernel status
   Current-Version: unknown
   Need-Reboot: False
   Last-Transition-Time: -
   Need-Reboot-Message: ""
   Boot-Version: unknown
   Boot-Version-Error: -
   Boot-Version-Changed: -
 --- Yasm-Agent-Package
   Current-Version: 2.221-20181102
   Last-Update-Ok: True
   Last-Transition-Time: 2018-10-31 16:56:45
   Last-Update-Message: "OK"
 --- Yasm-Agent-Service:
   Current-Version: unknown
   Last-Check-Ok: False
   Last-Transition-Time: -
   Last-Check-Message: ""
"""


def test_format_status():
    m = ya_salt_pb2.HostmanStatus()
    text_format.Parse(TEST_STATUS, m)
    # We always format time in moscow TZ,
    # make sure we test in this TZ
    with override_tz():
        assert str(status.pformat_run(m, nocolor=True)) == TEST_OUT
        assert str(status.pformat_run(m, nocolor=True, short=True)) == TEST_OUT_SHORT


def test_get_executed_states_count():
    hm_status = ya_salt_pb2.HostmanStatus()
    hm_status.salt_components.add().salt.state_results.add()
    assert statusutil.get_executed_states_count(hm_status.salt_components) == 1
    hm_status.salt_components.add().salt.state_results.add()
    assert statusutil.get_executed_states_count(hm_status.salt_components) == 2
    hm_status.salt_components.add().salt.state_results.add()
    assert statusutil.get_executed_states_count(hm_status.salt_components) == 3


@pytest.fixture
def status_with_components():
    hm_status = ya_salt_pb2.HostmanStatus()
    c = hm_status.salt_components.add()
    c.name = 'c1'
    c.selector = 'deploy.c1'
    c.salt.state_results.add().ok = True
    c = hm_status.salt_components.add()
    c.name = 'c2'
    c.selector = 'deploy.c2'
    sr = c.salt.state_results.add()
    sr.ok = True
    sr.changes = 'wind of changes'
    c = hm_status.salt_components.add()
    c.name = 'c3'
    c.selector = 'deploy.c3'
    sr = c.salt.state_results.add()
    sr.ok = False
    sr.changes = 'wind of changes'
    return hm_status


def test_get_executed_states_results_by_component(status_with_components):
    ok, ch, fail = statusutil.get_executed_states_results_by_component(status_with_components.salt_components)
    assert len(ok) == 3
    assert ok['c1'] == 1
    assert ok['c2'] == 1
    assert ok['c3'] == 0

    assert len(ch) == 3
    assert ch['c1'] == 0
    assert ch['c2'] == 1
    assert ch['c3'] == 1

    assert len(fail) == 3
    assert fail['c1'] == 0
    assert fail['c2'] == 0
    assert fail['c3'] == 1


def test_has_failed_executed_states():
    hm_status = ya_salt_pb2.HostmanStatus()
    hm_status.salt_components.add().salt.state_results.add().ok = True
    hm_status.salt_components.add().salt.state_results.add().ok = False
    hm_status.salt_components.add().salt.state_results.add().ok = True
    assert statusutil.has_failed_executed_states(hm_status.salt_components) is True
    del hm_status.salt_components[:]
    assert statusutil.has_failed_executed_states(hm_status.salt_components) is False
    hm_status.salt_components.add().salt.state_results.add().ok = True
    assert statusutil.has_failed_executed_states(hm_status.salt_components) is False


def test_get_executed_states_results(status_with_components):
    filter_ok = mock.Mock(return_value=True)
    filter_ch = mock.Mock(return_value=True)
    filter_fail = mock.Mock(return_value=True)
    ok, ch, fail = statusutil.get_executed_states_results(status_with_components.salt_components,
                                                          ok_filter=filter_ok,
                                                          changed_filter=filter_ch,
                                                          fail_filter=filter_fail)
    assert ok == 2
    assert ch == 2
    assert fail == 1
    assert filter_ok.call_count == 2
    assert filter_ch.call_count == 2
    assert filter_fail.call_count == 1
    filter_ok = mock.Mock(return_value=False)
    filter_ch = mock.Mock(return_value=False)
    filter_fail = mock.Mock(return_value=False)
    ok, ch, fail = statusutil.get_executed_states_results(status_with_components.salt_components,
                                                          ok_filter=filter_ok,
                                                          changed_filter=filter_ch,
                                                          fail_filter=filter_fail)
    assert ok == 0
    assert ch == 0
    assert fail == 0


def test_enumerate_executed_states(status_with_components):
    results_cb = mock.Mock()
    statusutil.enumerate_executed_states(status_with_components.salt_components, results_cb)
    assert results_cb.call_count == 3
    results_cb.assert_has_calls([
        mock.call(status_with_components.salt_components[0].salt.state_results[0]),
        mock.call(status_with_components.salt_components[1].salt.state_results[0]),
        mock.call(status_with_components.salt_components[2].salt.state_results[0]),
    ])


def test_enumerate_executed_components(status_with_components):

    components_cb = mock.Mock()
    statusutil.enumerate_executed_components(status_with_components.salt_components, components_cb)
    assert components_cb.call_count == 3
    components_cb.assert_has_calls([
        mock.call(status_with_components.salt_components[0]),
        mock.call(status_with_components.salt_components[1]),
        mock.call(status_with_components.salt_components[2]),
    ])
