from unittest.mock import patch, MagicMock


FAKE_NOW = 1024


class MockRun():
    def __init__(self, returncode, out, err):
        self.returncode = returncode
        self.stdout = out
        self.stderr = err


@patch('time.time', MagicMock(return_value=FAKE_NOW))
@patch('infra.rtc.juggler.bundle.checks.portod_tasks.get_porto_pids', MagicMock(return_value=[]))
@patch('infra.rtc.juggler.bundle.checks.portod_tasks.path.exists', MagicMock(return_value=True))
@patch('infra.rtc.juggler.bundle.checks.portod_tasks.check_porto', MagicMock(return_value=(0, "Ok")))
def test_no_porto_on_host(manifest):
    data = manifest.execute('portod_tasks')
    expected = {
        'events': [
            {
                'description': '{"status": "OK", "timestamp": 1024, "reason": "Ok"}',
                'service': 'portod_tasks',
                'status': 'OK',
            }
        ]
    }
    assert expected == data


@patch('time.time', MagicMock(return_value=FAKE_NOW))
@patch('infra.rtc.juggler.bundle.checks.portod_tasks.get_porto_pids', MagicMock(return_value=[]))
@patch('infra.rtc.juggler.bundle.checks.portod_tasks.path.exists', MagicMock(return_value=False))
def test_no_portoctl_on_host(manifest):
    data = manifest.execute('portod_tasks')
    expected = {
        'events': [
            {
                'description': '{"status": "CRIT", "timestamp": 1024, "reason": "\'/usr/sbin/portoctl\' does not exist"}',
                'service': 'portod_tasks',
                'status': 'CRIT',
            }
        ]
    }
    assert expected == data


@patch('time.time', MagicMock(return_value=FAKE_NOW))
@patch('infra.rtc.juggler.bundle.checks.portod_tasks.get_porto_pids', MagicMock(return_value=[]))
@patch('infra.rtc.juggler.bundle.checks.portod_tasks.path.exists', MagicMock(return_value=True))
@patch('infra.rtc.juggler.bundle.checks.portod_tasks.check_porto', MagicMock(return_value=(2, "'/' container is not meta")))
def test_root_ct_error(manifest):
    data = manifest.execute('portod_tasks')
    expected = {
        'events': [
            {
                'description': '{"status": "CRIT", "timestamp": 1024, "reason": "\'/\' container is not meta"}',
                'service': 'portod_tasks',
                'status': 'CRIT',
            }
        ]
    }
    assert expected == data


@patch('time.time', MagicMock(return_value=FAKE_NOW))
@patch('infra.rtc.juggler.bundle.checks.portod_tasks.get_cgroups',
       MagicMock(return_value=['/sys/fs/cgroup/freezer/portod/cgroup.procs', '/sys/fs/cgroup/cpuacct/portod/tasks']))
@patch('infra.rtc.juggler.bundle.checks.portod_tasks.get_porto_pids', MagicMock(return_value=['12', '34']))
@patch('infra.rtc.juggler.bundle.checks.portod_tasks.get_porto_tasks', MagicMock(return_value=['1234', '4321']))
@patch('infra.rtc.juggler.bundle.checks.portod_tasks.path.exists', MagicMock(return_value=True))
@patch('infra.rtc.juggler.bundle.checks.portod_tasks.check_porto', MagicMock(return_value=(0, "Ok")))
def test_normal_porto_on_host(manifest):
    data = manifest.execute('portod_tasks')
    expected = {
        'events': [
            {
                'description': '{"status": "OK", "timestamp": 1024, "reason": "Ok"}',
                'service': 'portod_tasks',
                'status': 'OK',
            }
        ]
    }
    assert expected == data


@patch('time.time', MagicMock(return_value=FAKE_NOW))
@patch('infra.rtc.juggler.bundle.checks.portod_tasks.get_cgroups', MagicMock(return_value=['/sys/fs/cgroup/cpu/porto%app/cgroup.procs']))
@patch('infra.rtc.juggler.bundle.checks.portod_tasks.get_porto_pids', MagicMock(return_value=['12', '34']))
@patch('infra.rtc.juggler.bundle.checks.portod_tasks.get_porto_tasks', MagicMock(return_value=['1234']))
@patch('infra.rtc.juggler.bundle.checks.portod_tasks.path.exists', MagicMock(return_value=True))
@patch('infra.rtc.juggler.bundle.checks.portod_tasks.check_porto', MagicMock(return_value=(0, "Ok")))
def test_porto_task_in_child_cgroup(manifest):
    data = manifest.execute('portod_tasks')
    expected = {
        'events': [
            {
                'description': '{"status": "CRIT", "timestamp": 1024, "reason": "Incorrect cgroup for portod task 1234: /sys/fs/cgroup/cpu/porto%app/cgroup.procs"}',
                'service': 'portod_tasks',
                'status': 'CRIT',
            }
        ]
    }
    assert expected == data


@patch('time.time', MagicMock(return_value=FAKE_NOW))
@patch('infra.rtc.juggler.bundle.checks.portod_tasks.get_cgroups', MagicMock(return_value=['/sys/fs/cgroup/freezer/porto/app/cgroup.procs']))
@patch('infra.rtc.juggler.bundle.checks.portod_tasks.get_porto_pids', MagicMock(return_value=['12', '34']))
@patch('infra.rtc.juggler.bundle.checks.portod_tasks.get_porto_tasks', MagicMock(return_value=['1234']))
@patch('infra.rtc.juggler.bundle.checks.portod_tasks.path.exists', MagicMock(return_value=True))
@patch('infra.rtc.juggler.bundle.checks.portod_tasks.check_porto', MagicMock(return_value=(0, "Ok")))
def test_porto_task_in_container_cgroup(manifest):
    data = manifest.execute('portod_tasks')
    expected = {
        'events': [
            {
                'description': '{"status": "CRIT", "timestamp": 1024, "reason": "Incorrect cgroup for portod task 1234: /sys/fs/cgroup/freezer/porto/app/cgroup.procs"}',
                'service': 'portod_tasks',
                'status': 'CRIT',
            }
        ]
    }
    assert expected == data
