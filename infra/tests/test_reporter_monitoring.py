import json

import mock
import pytest

from google.protobuf import json_format

from infra.ya_salt.proto import ya_salt_pb2
from infra.ya_salt.lib import package_manager
from infra.ya_salt.lib.reporters import monitoring
from infra.ya_salt.lib import pbutil


def test_extract_status_signals():
    f = monitoring.extract_status_signals
    st = ya_salt_pb2.HostmanStatus()
    # Test zero value
    assert f(st) == [
        (monitoring.METRIC_APT_UPDATE_OK, 0),
        (monitoring.METRIC_APT_UPDATE_DURATION, 0),
        (monitoring.METRIC_SERVER_INFO_OK, 0),
        (monitoring.METRIC_YASM_UPDATE_OK, 0),
    ]
    st.server_info.last_update_ok.status = 'True'
    assert f(st) == [
        (monitoring.METRIC_APT_UPDATE_OK, 0),
        (monitoring.METRIC_APT_UPDATE_DURATION, 0),
        (monitoring.METRIC_SERVER_INFO_OK, 1),
        (monitoring.METRIC_YASM_UPDATE_OK, 0),
    ]


def test_extract_salt_rev_status():
    spec = ya_salt_pb2.SaltSpec()
    status = ya_salt_pb2.SaltStatus()
    # Test not ok
    spec.revision = '73839dfa'
    status.revision = ''
    sig, v = monitoring.extract_salt_rev_status(spec, status)
    assert sig == monitoring.METRIC_SALT_REVISION_OK
    assert v == 0
    # Test ok
    status.revision = spec.revision
    sig, v = monitoring.extract_salt_rev_status(spec, status)
    assert sig == monitoring.METRIC_SALT_REVISION_OK
    assert v == 1


IS_MARKET_MODULE_CASES = [
    ({"started": "2019-06-25T07:39:56Z",
      "duration": 21.266000747680664,
      "ok": True,
      "id": "etc-hosts",
      "comment": "File /etc/hosts is in the correct state"
      }, False),
    ({"started": "2019-06-25T07:40:07Z",
      "duration": 0.8119999766349792,
      "ok": False,
      "id": "market.config.rfsd.db",
      "comment": "Directory /var/remote-log/db is in the correct state"
      }, False),
    ({"comment": "Command \"/usr/bin/newaliases\" run",
      "ok": True,
      "started": "2019-06-25T07:40:05Z",
      "duration": 330.0249938964844,
      "changes": "{\"pid\": 343379, \"retcode\": 0, \"stderr\": \"\", \"stdout\": \"\"}",
      "id": "market.config.aliases"
      }, True),
    ({"comment": "Module function mount.set_fstab executed",
      "ok": True,
      "started": "2019-06-25T07:40:07Z",
      "duration": 37.40800094604492,
      "changes": "{\"ret\": \"change\"}",
      "id": "market.config.rfsd.var.log.yandex"
      }, True),
]


@pytest.mark.parametrize("result,expected", IS_MARKET_MODULE_CASES)
def test_is_market_module_run(result, expected):
    f = monitoring.is_market_module_run
    m = ya_salt_pb2.StateRunResult()
    json_format.ParseDict(result, m)
    assert f(m) is expected, result


def test_extract_hostctl_signals():
    f = monitoring.extract_hostctl_signals
    hst = ya_salt_pb2.HostmanStatus()
    assert f(hst) == [(monitoring.METRIC_HOSTCTL_EXECUTE_TIME, 0)]
    hst.hostctl.execution_time.seconds = 3
    assert f(hst) == [(monitoring.METRIC_HOSTCTL_EXECUTE_TIME, 3)]


def test_extract_salt_signals():
    f = monitoring.extract_salt_signals
    hst = ya_salt_pb2.HostmanStatus()
    st = hst.salt
    st.revision_timestamp.seconds = 123
    st.compilation_ok.status = 'False'
    st.execution_ok.status = 'False'
    assert f(hst) == [
        (monitoring.METRIC_SALT_EXECUTE_TIME, 0),
        (monitoring.METRIC_SALT_INIT_FAIL, 0),
        (monitoring.METRIC_SALT_COMPILE_FAIL, 1),
        (monitoring.METRIC_SALT_EXECUTE_FAIL, 1),
        (monitoring.METRIC_SALT_OK, 0),
        (monitoring.METRIC_SALT_FAIL, 0),
        (monitoring.METRIC_SALT_CHANGED, 0),
        (monitoring.METRIC_SALT_TIMESTAMP, 123),
        (monitoring.METRIC_SALT_COMPONENTS_INIT_FAIL, 0),
    ]
    st.last_run_duration.seconds = 11
    st.compilation_ok.status = 'True'
    st.execution_ok.status = 'True'
    sc = hst.salt_components.add()
    pbutil.true_cond(sc.initialized)
    r = sc.salt.state_results.add()
    r.id = ''
    r.ok = True
    r.changes = '{"some": "changes"}'
    r = sc.salt.state_results.add()
    r.id = 'yandex-diskmanager'
    r.ok = False
    r.changes = ''
    r = sc.salt.state_results.add()
    r.ok = True
    r.changes = ''
    assert f(hst) == [
        (monitoring.METRIC_SALT_EXECUTE_TIME, 11),
        (monitoring.METRIC_SALT_INIT_FAIL, 0),
        (monitoring.METRIC_SALT_COMPILE_FAIL, 0),
        (monitoring.METRIC_SALT_EXECUTE_FAIL, 0),
        (monitoring.METRIC_SALT_OK, 2),
        (monitoring.METRIC_SALT_FAIL, 1),
        (monitoring.METRIC_SALT_CHANGED, 1),
        (monitoring.METRIC_SALT_TIMESTAMP, 123),
        (monitoring.METRIC_SALT_COMPONENTS_INIT_FAIL, 0),
    ]
    # Check that we do not fail execution because of O'Rly
    del hst.salt_components[:]
    sc = hst.salt_components.add()
    pbutil.true_cond(sc.initialized)
    sc.applied.status = 'False'
    sc.applied.message = 'too many operations in progress'
    st.last_run_duration.seconds = 11
    st.revision_timestamp.seconds = 456
    st.compilation_ok.status = 'True'
    st.execution_ok.status = 'False'
    sc = hst.salt_components.add()
    pbutil.false_cond(sc.initialized, 'mock')
    sc = hst.salt_components.add()
    pbutil.false_cond(sc.initialized, 'mock')
    pbutil.false_cond(hst.salt.compilation_ok, 'mock')
    assert f(hst) == [
        (monitoring.METRIC_SALT_EXECUTE_TIME, 11),
        (monitoring.METRIC_SALT_INIT_FAIL, 0),
        (monitoring.METRIC_SALT_COMPILE_FAIL, 1),
        (monitoring.METRIC_SALT_EXECUTE_FAIL, 0),
        (monitoring.METRIC_SALT_OK, 0),
        (monitoring.METRIC_SALT_FAIL, 0),
        (monitoring.METRIC_SALT_CHANGED, 0),
        (monitoring.METRIC_SALT_TIMESTAMP, 456),
        (monitoring.METRIC_SALT_COMPONENTS_INIT_FAIL, 2),
    ]


def test_ready_and_need_reboot():
    status = ya_salt_pb2.KernelStatus(version='4.4')
    pbutil.set_condition(status.need_reboot, 'True')
    assert monitoring.ready_and_need_reboot(status)
    pbutil.set_condition(status.need_reboot, 'Unknown')
    assert not monitoring.ready_and_need_reboot(status)


def test_extract_gencfg_signal():
    spec = ya_salt_pb2.HostmanSpec()
    f = monitoring.extract_gencfg_signal
    rv = f(spec)
    assert len(rv) == 1
    assert rv[0] == (monitoring.METRIC_GENCFG_GROUPS_COUNT, 0)
    spec.gencfg_groups.extend(['ALL_RUNTIME', 'ALL_YP'])
    rv = f(spec)
    assert len(rv) == 1
    assert rv[0] == (monitoring.METRIC_GENCFG_GROUPS_COUNT, 2)


def test_check_salt():
    loads = json.loads
    f = monitoring.check_salt
    hm_status = ya_salt_pb2.HostmanStatus()
    status = hm_status.salt
    # Check init failed
    pbutil.set_condition(status.init_ok, 'False', 'INIT TEST')
    c = f(hm_status.salt, hm_status.salt_components)
    assert c.status == 'CRIT'
    assert loads(c.desc)['reason'] == 'Init failed: INIT TEST'
    # Check compilation failed
    pbutil.set_condition(status.init_ok, 'True')
    pbutil.set_condition(status.compilation_ok, 'False', 'COMPILATION TEST')
    c = f(hm_status.salt, hm_status.salt_components)
    assert c.status == 'CRIT'
    assert loads(c.desc)['reason'] == 'Compilation failed: COMPILATION TEST'
    # Check execution failed
    pbutil.set_condition(status.compilation_ok, 'True')
    pbutil.set_condition(status.execution_ok, 'False', 'COMPILATION TEST')
    c = f(hm_status.salt, hm_status.salt_components)
    assert c.status == 'CRIT'
    assert loads(c.desc)['reason'] == 'Execution failed: COMPILATION TEST'
    # Check failed states
    pbutil.set_condition(status.execution_ok, 'True')
    state = hm_status.salt_components.add().salt.state_results.add()
    state.ok = False
    state.id = "yandex-hw-watcher@"
    c = f(hm_status.salt, hm_status.salt_components)
    assert c.status == 'CRIT'
    assert loads(c.desc)['reason'] == 'Failed states: 1 [yandex-hw-watcher@]'


def test_extract_juggler_checks():
    """
    Simple smoke test to have at least something.
    """
    spec = ya_salt_pb2.HostmanSpec()
    status = ya_salt_pb2.HostmanStatus()
    # === All failed checks
    # Not ready kernel
    pbutil.set_condition(status.kernel.need_reboot, 'True', 'To 4.4.4.4')
    # Failed apt update
    pbutil.set_condition(status.apt.last_update_ok, 'False', 'Failed')
    # Failed hostman
    pbutil.set_condition(status.initial_setup_passed, 'False', 'something failed')
    # Failed salt states
    state = status.salt_components.add().salt.state_results.add()
    state.ok = False
    state.id = 'yandex-hw-watcher@'
    rv = monitoring.extract_juggler_checks(spec, status)
    assert len(rv) == 4
    for i in rv:
        assert i.status == 'CRIT', i.to_event('test')


def test_monitoring_extract_signals():
    spec = ya_salt_pb2.HostmanSpec()
    status = ya_salt_pb2.HostmanStatus()
    pkg_man = mock.Mock()
    pkg_man.metrics.return_value = package_manager.Metrics()
    m = monitoring.Monitoring(spec, None, pkg_man=pkg_man)
    signals = m.extract_signals(status)
    pkg_man.metrics.assert_called_once_with()
    assert isinstance(signals, list)
    for s in signals:
        assert type(s) is tuple
        assert len(s) == 2
        assert isinstance(s[0], basestring)
        assert isinstance(s[1], (int, float, long)), s


def test_extract_ctype_ok():
    s = ya_salt_pb2.HostmanSpec()
    s.walle_tags.extend(['mock', 'rtc.stage-experiment', 'mock'])
    m = monitoring.Monitoring(s, None, None)
    assert m.extract_ctype() == 'experiment'
    del s.walle_tags[:]
    s.walle_tags.extend(['mock', 'rtc.stage-production', 'mock'])
    assert m.extract_ctype() == 'production'
    del s.walle_tags[:]
    s.walle_tags.extend(['mock', 'rtc.stage-testing', 'mock'])
    assert m.extract_ctype() == 'testing'
    del s.walle_tags[:]
    s.walle_tags.extend(['mock', 'rtc.stage-unknown', 'mock'])
    assert m.extract_ctype() == 'unknown'


def test_extract_ctype_warn():
    s = ya_salt_pb2.HostmanSpec()
    s.env_type = 'prestable'
    s.walle_tags.extend(['mock', 'rtc.stageexperiment', 'mock'])
    m = monitoring.Monitoring(s, None, None)
    assert m.extract_ctype() == 'prestable'
    s.env_type = 'production'
    assert m.extract_ctype() == 'production'
