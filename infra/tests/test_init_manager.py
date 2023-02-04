import mock

from infra.ya_salt.lib import init_manager
from infra.ya_salt.lib import subprocutil
from infra.ya_salt.proto import ya_salt_pb2


def test_is_running_systemd():
    sd = init_manager.Systemd
    test_cases = [
        (
            mock.Mock(return_value=('', '', subprocutil.Status(False, message=''))),
            ya_salt_pb2.Condition(status='Unknown', message=''),
        ),
        (
            mock.Mock(return_value=('SubState=running', '', subprocutil.Status(True, message=''))),
            ya_salt_pb2.Condition(status='True', message='running'),
        ),
        (
            mock.Mock(return_value=('SubState=dead', '', subprocutil.Status(True, message=''))),
            ya_salt_pb2.Condition(status='False', message='dead'),
        ), (
            mock.Mock(return_value=('SubState=lovingstarwars', '', subprocutil.Status(True, message=''))),
            ya_salt_pb2.Condition(status='Unknown',
                                  message='lovingstarwars'),
        ),
    ]
    for check_out_mock, expected in test_cases:
        got = sd(check_output=check_out_mock).get_running('sshd')
        check_out_mock.assert_called_once_with([sd.SYSTEMCTL, 'show', 'sshd.service',
                                                '-p', 'SubState'],
                                               timeout=30)
        assert got.status == expected.status
        assert got.message == expected.message


def test_start_systemd():
    sd = init_manager.Systemd
    test_cases = [
        (
            mock.Mock(return_value=('failure', '', subprocutil.Status(False, message='ERR'))),
            False,
        ),
        (
            mock.Mock(return_value=('OK', '', subprocutil.Status(True, message=''))),
            True,
        )
    ]
    for check_out_mock, ok in test_cases:
        err = sd(check_output=check_out_mock).start('sshd')
        check_out_mock.assert_called_once_with([sd.SYSTEMCTL, 'start', 'sshd.service'],
                                               timeout=30)
        if ok:
            assert err is None
        else:
            assert err


def test_stop_systemd():
    sd = init_manager.Systemd
    test_cases = [
        (
            mock.Mock(return_value=('failure', '', subprocutil.Status(False, message='ERR'))),
            False,
        ),
        (
            mock.Mock(return_value=('OK', '', subprocutil.Status(True, message=''))),
            True,
        )
    ]
    for check_out_mock, ok in test_cases:
        err = sd(check_output=check_out_mock).stop('sshd')
        check_out_mock.assert_called_once_with([sd.SYSTEMCTL, 'stop', 'sshd.service'],
                                               timeout=30)
        if ok:
            assert err is None
        else:
            assert err


def test_restart_systemd():
    sd = init_manager.Systemd
    test_cases = [
        (
            mock.Mock(return_value=('failure', '', subprocutil.Status(False, message='ERR'))),
            False,
        ),
        (
            mock.Mock(return_value=('OK', '', subprocutil.Status(True, message=''))),
            True,
        )
    ]
    for check_out_mock, ok in test_cases:
        err = sd(check_output=check_out_mock).restart('sshd')
        check_out_mock.assert_called_once_with([sd.SYSTEMCTL, 'restart', 'sshd.service'],
                                               timeout=30)
        if ok:
            assert err is None
        else:
            assert err


def test_reload_config_systemd():
    sd = init_manager.Systemd
    test_cases = [
        (
            mock.Mock(return_value=('failure', '', subprocutil.Status(False, message='ERR'))),
            False,
        ),
        (
            mock.Mock(return_value=('OK', '', subprocutil.Status(True, message=''))),
            True,
        )
    ]
    for check_out_mock, ok in test_cases:
        err = sd(check_output=check_out_mock).reload_config()
        check_out_mock.assert_called_once_with([sd.SYSTEMCTL, 'daemon-reload'],
                                               timeout=30)
        if ok:
            assert err is None
        else:
            assert err


def test_is_shutting_down_systemd():
    sd = init_manager.Systemd
    test_cases = [
        (
            mock.Mock(return_value=('degraded', '', subprocutil.Status(False, message='ERR'))),
            False,
        ),
        (
            mock.Mock(return_value=('stopping', '', subprocutil.Status(False, message='exit code 1'))),
            True,
        )
    ]
    for check_out_mock, outcome in test_cases:
        down = sd(check_output=check_out_mock).is_shutting_down()
        check_out_mock.assert_called_once_with([sd.SYSTEMCTL, 'is-system-running'], timeout=30)
        assert down is outcome
