import collections
import time

import mock
from juggler import bundles

from infra.cauth.agent.linux.juggler.bundle.checks import cauth_userd


def test_get_pidfile_from_config():
    conf = {'daemonConfig': {'pidFile': 'mock'}}
    assert cauth_userd.get_pidfile_from_config(conf) == "mock"
    assert cauth_userd.get_pidfile_from_config({}) == cauth_userd.DEFAULT_CONFIG['daemonConfig']['pidFile']
    assert cauth_userd.get_pidfile_from_config({'daemonConfig': {}}) == cauth_userd.DEFAULT_CONFIG['daemonConfig'][
        'pidFile']


def test_get_socket_path_from_config():
    conf = {'daemonConfig': {'socketPath': 'mock'}}
    assert cauth_userd.get_socket_path_from_config(conf) == "mock"
    assert cauth_userd.get_socket_path_from_config({}) == cauth_userd.DEFAULT_CONFIG['daemonConfig']['socketPath']
    assert cauth_userd.get_pidfile_from_config({'daemonConfig': {}}) == cauth_userd.DEFAULT_CONFIG['daemonConfig'][
        'pidFile']


def test_get_repo_path_from_config():
    conf = {'repoConfig': {'path': 'mock'}}
    assert cauth_userd.get_repo_path_from_config(conf) == "mock"
    assert cauth_userd.get_repo_path_from_config({}) == cauth_userd.DEFAULT_CONFIG['repoConfig']['path']
    assert cauth_userd.get_pidfile_from_config({'repoConfig': {}}) == cauth_userd.DEFAULT_CONFIG['daemonConfig'][
        'pidFile']


def test_get_pid():
    pid, err = cauth_userd.get_pid("123", None)
    assert pid is None
    assert err is not None
    pid, err = cauth_userd.get_pid("/mock", mock.mock_open(read_data="123\n"))
    assert err is None
    assert pid == "123"


def test_pid_running():
    m = mock.Mock(side_effect=Exception)
    ok, err = cauth_userd.pid_running("123", m)
    assert ok is None
    assert err is not None
    m = mock.Mock(return_value=True)
    ok, err = cauth_userd.pid_running("123", m)
    assert ok is True
    assert err is None


def test_group_exists():
    ms = mock.Mock()
    ms.__enter__ = mock.Mock()
    ms.__exit__ = mock.Mock()
    mrsp = mock.Mock()
    mrsp.status_code = 200
    ms.__enter__.return_value.get = mock.Mock(return_value=mrsp)
    m = mock.Mock(return_value=ms)
    ok, err = cauth_userd.group_exists("/run/cauth.sock", "dpt_yandex", session=m)
    ms.__enter__.return_value.get.assert_called_with('http+unix://%2Frun%2Fcauth.sock/nss/v1/group/dpt_yandex')
    assert ok is True
    assert err is None

    mrsp.status_code = 404
    ok, err = cauth_userd.group_exists("/run/cauth.sock", "dpt_yandex", session=m)
    assert ok is False
    assert err is None

    ms.__enter__.return_value.get = mock.Mock(side_effect=Exception)
    ok, err = cauth_userd.group_exists("/run/cauth.sock", "dpt_yandex", session=m)
    assert ok is False
    assert err is not None


StatResult = collections.namedtuple("StatResult", "st_mtime")


def test_get_target_timestamp():
    now = int(time.time())
    readlink = mock.Mock(return_value="/target")
    isdir = mock.Mock(return_value=True)
    stat = mock.Mock(return_value=StatResult(now))
    ts, err = cauth_userd.get_target_timestamp("/current", isdir, readlink, stat)
    assert ts == now
    assert err is None
    readlink.assert_called_with("/current")
    isdir.assert_called_with("/target")
    stat.assert_called_with("/target")


def test_check_userd_running_ok():
    isdir = mock.Mock(return_value=True)
    pid = mock.mock_open(read_data="123")
    s, err = cauth_userd.check_userd_running({}, pid, isdir)
    assert s == bundles.Status.OK
    assert err is None
    pid.assert_called_with(cauth_userd.DEFAULT_CONFIG['daemonConfig']['pidFile'])
    isdir.assert_called_with("/proc/123")


def test_check_userd_running_crit():
    isdir = mock.Mock(return_value=False)
    pid = mock.mock_open(read_data="123")
    s, err = cauth_userd.check_userd_running({}, pid, isdir)
    assert s == bundles.Status.CRIT
    assert err is not None
    pid.assert_called_with(cauth_userd.DEFAULT_CONFIG['daemonConfig']['pidFile'])
    isdir.assert_called_with("/proc/123")


def test_check_userd_resolver():
    ms = mock.Mock()
    ms.__enter__ = mock.Mock()
    ms.__exit__ = mock.Mock()
    mrsp = mock.Mock()
    mrsp.status_code = 200
    ms.__enter__.return_value.get = mock.Mock(return_value=mrsp)
    m = mock.Mock(return_value=ms)
    s, err = cauth_userd.check_userd_resolver({}, session=m)
    assert s == bundles.Status.OK
    assert err is None

    mrsp.status_code = 404
    s, err = cauth_userd.check_userd_resolver({}, session=m)
    assert s == bundles.Status.CRIT
    assert err is not None

    ms.__enter__.return_value.get = mock.Mock(side_effect=Exception)
    s, err = cauth_userd.check_userd_resolver({}, session=m)
    assert s == bundles.Status.CRIT
    assert err is not None


def test_check_userd_current():
    now = int(time.time()) - 1
    readlink = mock.Mock(return_value="/target")
    isdir = mock.Mock(return_value=True)
    stat = mock.Mock(return_value=StatResult(now))
    s, err = cauth_userd.check_userd_current({}, readlink=readlink, isdir=isdir, stat=stat)
    assert s == bundles.Status.OK
    assert err is None

    now = time.time() - 1 - cauth_userd.CURRENT_AGE_THRESHOLD
    stat = mock.Mock(return_value=StatResult(now))
    s, err = cauth_userd.check_userd_current({}, readlink=readlink, isdir=isdir, stat=stat)
    assert s == bundles.Status.CRIT
    assert err is not None


def test_run_next():
    assert cauth_userd.run_next(bundles.Status.OK, None) is True
    assert cauth_userd.run_next(bundles.Status.CRIT, None) is False
    assert cauth_userd.run_next(bundles.Status.OK, 'err') is False
    assert cauth_userd.run_next(bundles.Status.CRIT, 'err') is False


def test_run_check_default_config(monkeypatch):
    exists = mock.Mock(return_value=False)
    running = mock.Mock(return_value=(bundles.Status.OK, None))
    resolver = mock.Mock(return_value=(bundles.Status.OK, None))
    current = mock.Mock(return_value=(bundles.Status.OK, None))
    monkeypatch.setattr(cauth_userd, 'check_userd_running', running)
    monkeypatch.setattr(cauth_userd, 'check_userd_resolver', resolver)
    monkeypatch.setattr(cauth_userd, 'check_userd_current', current)
    e = cauth_userd.run_check([], exists=exists)
    assert e.status == bundles.Status.OK
    exists.assert_called_with(cauth_userd.DEFAULT_USERD_CONF)
    running.assert_called()
    resolver.assert_called()
    current.assert_called()


def test_run_check_custom_config(monkeypatch):
    args = ("--userd-config-path", "/etc/config.yaml")
    read_config = mock.Mock(return_value=(cauth_userd.DEFAULT_CONFIG, None))
    exists = mock.Mock(return_value=None)
    running = mock.Mock(return_value=(bundles.Status.OK, None))
    resolver = mock.Mock(return_value=(bundles.Status.OK, None))
    current = mock.Mock(return_value=(bundles.Status.OK, None))
    monkeypatch.setattr(cauth_userd, 'read_config', read_config)
    monkeypatch.setattr(cauth_userd, 'check_userd_running', running)
    monkeypatch.setattr(cauth_userd, 'check_userd_resolver', resolver)
    monkeypatch.setattr(cauth_userd, 'check_userd_current', current)
    e = cauth_userd.run_check(args, exists=exists)
    assert e.status == bundles.Status.OK
    exists.assert_not_called()
    read_config.assert_called_with("/etc/config.yaml")
    running.assert_called()
    resolver.assert_called()
    current.assert_called()
