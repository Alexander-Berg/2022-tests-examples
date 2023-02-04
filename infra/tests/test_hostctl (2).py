import mock
import pytest
from infra.ya_salt.lib import constants
from infra.ya_salt.lib import hostctl
from infra.ya_salt.lib import subprocutil
from infra.ya_salt.proto import config_pb2
from infra.ya_salt.proto import ya_salt_pb2


def test_hostctl_manage():
    c = config_pb2.Config()
    c.hostctl_path = '/hostctl.mock'
    hctl = hostctl.Hostctl(c)
    check_output = mock.Mock(return_value=("", "", subprocutil.Status(True, 'msg')))
    err = hctl.manage('repo_path', None, check_output)
    assert err is None
    check_output.assert_called_once()


def test_hostctl_manage_inline():
    c = config_pb2.Config()
    c.hostctl_path = '/hostctl.mock'
    hctl = hostctl.Hostctl(c)
    check_output = mock.Mock(return_value=("", "", subprocutil.Status(True, 'msg')))
    err = hctl.manage_inline('mock', 'repo_path', 'contents', check_output)
    assert err is None
    args = [
        '/hostctl.mock',
        'manage',
        '--logfile', constants.LOG_FILE_HOSTCTL,
        '--repo', 'repo_path',
        '--no-orly',
        '--stdin',
    ]
    check_output.assert_called_once_with(args, timeout=hostctl.HOSTCTL_TIMEOUT, data='contents')


def test_run_hostctl_conditions():
    s = ya_salt_pb2.HostmanStatus()
    ctx = mock.Mock()
    ctx.done = mock.Mock(return_value=True)
    hctl = mock.Mock()
    err = hostctl.run_hostctl_manage(hctl, ctx, s, None)
    assert err is None
    ctx.done = mock.Mock(return_value=False)
    hctl.available = mock.Mock(return_value='n/a')
    err = hostctl.run_hostctl_manage(hctl, ctx, s, None)
    assert err == 'n/a'


def test_run_hostctl_ok():
    s = ya_salt_pb2.HostmanStatus()
    ctx = mock.Mock()
    ctx.done = mock.Mock(return_value=False)
    hctl = mock.Mock()
    hctl.available = mock.Mock(return_value=None)
    hctl.manage_or_report_called = mock.Mock(return_value=False)
    hctl.manage = mock.Mock(return_value=None)
    err = hostctl.run_hostctl_manage(hctl, ctx, s, None, None)
    assert err is None
    ctx.ok.assert_called_once()
    ctx.fail.assert_not_called()
    assert s.hostctl.ok.status == 'True'


def test_run_hostctl_fail():
    s = ya_salt_pb2.HostmanStatus()
    ctx = mock.Mock()
    ctx.done = mock.Mock(return_value=False)
    hctl = mock.Mock()
    hctl.available = mock.Mock(return_value=None)
    hctl.manage_or_report_called = mock.Mock(return_value=False)
    hctl.manage = mock.Mock(return_value='err')
    err = hostctl.run_hostctl_manage(hctl, ctx, s, None, None)
    assert err == 'err'
    ctx.ok.assert_not_called()
    ctx.fail.assert_called_once()
    assert s.hostctl.ok.status == 'False'
    assert s.hostctl.ok.message == 'err'


def test_hostctl_manage_target():
    c = config_pb2.Config()
    c.hostctl_path = '/hostctl.mock'
    hctl = hostctl.Hostctl(c)
    check_output = mock.Mock(return_value=("", "", subprocutil.Status(True, 'msg')))
    err = hctl.manage_target('repo_path', 'target', 'https://orly', check_output)
    assert err is None
    check_output.assert_called_once_with([
        '/hostctl.mock', 'manage',
        '--logfile', '/var/log/hostctl.log',
        '--repo', 'repo_path',
        '--target-units', 'target',
        '--orly-url', 'https://orly'
    ], timeout=1800)


def test_hostctl_manage_target_no_orly():
    c = config_pb2.Config()
    c.hostctl_path = '/hostctl.mock'
    hctl = hostctl.Hostctl(c)
    check_output = mock.Mock(return_value=("", "", subprocutil.Status(True, 'msg')))
    err = hctl.manage_target('repo_path', 'target', None, check_output)
    assert err is None
    check_output.assert_called_once_with([
        '/hostctl.mock', 'manage',
        '--logfile', '/var/log/hostctl.log',
        '--repo', 'repo_path',
        '--target-units', 'target',
        '--no-orly'
    ], timeout=1800)


@pytest.mark.parametrize('status', ('False', '', 'Unknown', 'blah'))
def test_orly_url_from_orly_initial_setup(status):
    s = ya_salt_pb2.HostmanStatus()
    s.initial_setup_passed.status = status
    o = mock.Mock()
    o.orly = mock.Mock()
    o.orly.url = 'https://mock/.../'
    assert hostctl.orly_url_from_orly(o, s.initial_setup_passed) is None


def test_orly_url_from_orly_not_initial_setup():
    s = ya_salt_pb2.HostmanStatus()
    s.initial_setup_passed.status = 'True'
    o = mock.Mock()
    o.orly = mock.Mock()
    o.orly.url = 'https://mock/.../'
    assert hostctl.orly_url_from_orly(o, s.initial_setup_passed) == 'https://mock/.../'
    o.orly.url = 'https://mock/.../rest/'
    assert hostctl.orly_url_from_orly(o, s.initial_setup_passed) == 'https://mock/.../'


def test_hostctl_report_ok():
    c = config_pb2.Config()
    c.hostctl_path = '/hostctl.mock'
    c.report_addrs.append('https://report/')
    hctl = hostctl.Hostctl(c)
    check_output = mock.Mock(return_value=("", "", subprocutil.Status(True, 'msg')))
    err = hctl.report(check_output)
    assert err is None
    check_output.assert_called_once_with(
        '/hostctl.mock report --logfile /var/log/hostctl.log --report-addr https://report/'.split(), timeout=1800)


def test_hostctl_report_fail():
    c = config_pb2.Config()
    c.hostctl_path = '/hostctl.mock'
    c.report_addrs.append('https://report/')
    hctl = hostctl.Hostctl(c)
    check_output = mock.Mock(return_value=("", "", subprocutil.Status(False, 'err')))
    err = hctl.report(check_output)
    assert err == 'err'
    check_output.assert_called_once_with(
        '/hostctl.mock report --logfile /var/log/hostctl.log --report-addr https://report/'.split(), timeout=1800)
