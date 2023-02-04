import mock
import time
import pytest

from infra.qyp.vmagent.src import qemu_ctl


def test_qemu_mon(tmpdir):
    socket_file = tmpdir.join('mon.sock')
    qemu_mon = qemu_ctl.QemuMon(str(socket_file))

    # case: socket file does not exists
    with pytest.raises(qemu_ctl.QemuMonError) as error:
        qemu_mon('test', 1)
    assert 'Socket timeout' in error.value.message

    socket_file.write('1')
    with mock.patch('socket.socket') as create_socket_mock:
        socket_mock = mock.Mock()
        create_socket_mock.return_value = socket_mock

        # case: call qemu_mon with any command
        cmd = 'any'
        qemu_mon(cmd)
        socket_mock.connect.assert_called_with(str(socket_file))
        socket_mock.recv.assert_called_with(1024)
        socket_mock.sendall.assert_called_with(cmd + '\n')
        socket_mock.recv.assert_called_with(1024)
        socket_mock.close.assert_called()
        assert socket_mock.settimeout.call_args[0] > 0

        # case: qemu_mon power_off
        socket_mock.reset_mock()
        socket_mock.connect.reset_mock()
        socket_mock.close.reset_mock()
        qemu_mon.power_off()
        socket_mock.sendall.assert_called_with('quit' + '\n')

        # case: qemu_mon system_powerdown
        socket_mock.reset_mock()
        socket_mock.connect.reset_mock()
        socket_mock.close.reset_mock()
        qemu_mon.system_powerdown()
        socket_mock.sendall.assert_called_with('system_powerdown' + '\n')

        # case: qemu_mon system_reset
        socket_mock.reset_mock()
        socket_mock.connect.reset_mock()
        socket_mock.close.reset_mock()
        qemu_mon.system_reset()
        socket_mock.sendall.assert_called_with('system_reset' + '\n')

        # case: qemu_mon set_vnc_password
        socket_mock.reset_mock()
        socket_mock.connect.reset_mock()
        socket_mock.close.reset_mock()
        qemu_mon.set_vnc_password('test')
        socket_mock.sendall.assert_called_with('set_password vnc test' + '\n')

        # case: call qemu_mon with timeout error
        socket_mock.reset_mock()
        socket_mock.connect.side_effect = lambda *args, **kwargs: time.sleep(1)
        socket_mock.close.side_effect = lambda *args, **kwargs: 1 / 0
        with pytest.raises(qemu_ctl.QemuMonError) as error:
            qemu_mon(cmd, timeout=1)
        assert error.value.message == 'Socket timeout: integer division or modulo by zero'


def test_qemu_ctl_get_qemu_container():
    porto_connection_mock = mock.Mock()
    _qemu_ctl = qemu_ctl.QemuCtl(porto_connection_mock, None)
    _qemu_ctl._qemu_mon = mock.Mock()
    qemu_container_mock = mock.Mock()

    # case: _get_qemu_container found new_one=False
    porto_connection_mock.Find.return_value = qemu_container_mock
    qemu_container = _qemu_ctl._get_qemu_container(new_one=False)
    assert qemu_container is qemu_container_mock
    porto_connection_mock.Find.assert_called_with(qemu_ctl.QemuCtl.QEMU_CONTAINER_NAME)

    # case: _get_qemu_container found new_one=True
    porto_connection_mock.Find.return_value = qemu_container_mock
    qemu_container_mock_2 = mock.Mock()
    porto_connection_mock.Create.return_value = qemu_container_mock_2
    qemu_container = _qemu_ctl._get_qemu_container(new_one=True)
    assert qemu_container is qemu_container_mock_2
    qemu_container_mock.Destroy.assert_called_with()
    porto_connection_mock.Find.assert_called_with(qemu_ctl.QemuCtl.QEMU_CONTAINER_NAME)

    # case: _get_qemu_container not found new_one=False
    porto_connection_mock.Find.return_value = None
    qemu_container = _qemu_ctl._get_qemu_container(new_one=False)
    assert qemu_container is None

    # case: _get_qemu_container not found new_one=True
    porto_connection_mock.Find.side_effect = qemu_ctl.PortoContainerNotExists()
    porto_connection_mock.Create.return_value = qemu_container_mock
    qemu_container = _qemu_ctl._get_qemu_container(new_one=True)
    assert qemu_container is qemu_container_mock
    porto_connection_mock.Create.assert_called_with(qemu_ctl.QemuCtl.QEMU_CONTAINER_NAME)


def test_qemu_ctl_check():
    porto_connection_mock = mock.Mock()
    qemu_mon_mock = mock.Mock()
    get_qemu_container_mock = mock.Mock()
    qemu_container_mock = mock.Mock()
    _qemu_ctl = qemu_ctl.QemuCtl(porto_connection_mock, None)
    _qemu_ctl._qemu_mon = qemu_mon_mock
    _qemu_ctl._get_qemu_container = get_qemu_container_mock

    # case: success check
    qemu_container_mock.reset_mock()
    get_qemu_container_mock.return_value = qemu_container_mock
    qemu_container_mock.Wait.return_value = []
    _qemu_ctl.check()

    # case: qemu container not found
    get_qemu_container_mock.return_value = None
    with pytest.raises(_qemu_ctl.QemuContainerDoesNotExists):
        _qemu_ctl.check()
    get_qemu_container_mock.assert_called_with(new_one=False)

    # case: qemu container found, and exit with code 0
    qemu_container_mock.reset_mock()
    get_qemu_container_mock.return_value = qemu_container_mock
    qemu_container_mock.Wait.return_value = ['qemu']
    qemu_container_mock.GetProperty.return_value = '0'
    with pytest.raises(_qemu_ctl.QemuContainerStopped):
        _qemu_ctl.check()
    qemu_container_mock.GetProperty.assert_called_with('exit_code')
    qemu_container_mock.Destroy.assert_called_with()

    # case: qemu container found, and exit with code > 0
    qemu_container_mock.reset_mock()
    get_qemu_container_mock.return_value = qemu_container_mock
    qemu_container_mock.Wait.return_value = ['qemu']
    qemu_container_mock.GetProperty.return_value = '10'
    with pytest.raises(_qemu_ctl.QemuContainerCrashed):
        _qemu_ctl.check()
    qemu_container_mock.GetProperty.assert_called_with('stderr')

    # case: qemu container found, and dropped unexpectedly
    qemu_container_mock.reset_mock()
    get_qemu_container_mock.return_value = qemu_container_mock
    qemu_container_mock.Wait.return_value = ['qemu']
    qemu_container_mock.GetProperty.side_effect = qemu_ctl.PortoContainerNotExists()
    with pytest.raises(_qemu_ctl.QemuContainerCrashed) as error:
        _qemu_ctl.check()
    assert error.value.message == 'Qemu container dropped unexpectedly'

    # case: qemu container found, and porto socket error
    qemu_container_mock.reset_mock()
    get_qemu_container_mock.return_value = qemu_container_mock
    qemu_container_mock.Wait.return_value = ['qemu']
    qemu_container_mock.GetProperty.side_effect = qemu_ctl.PortoSocketError()
    with pytest.raises(_qemu_ctl.QemuContainerCrashed) as error:
        _qemu_ctl.check()
    assert error.value.message == "Porto socket error"


def test_qemu_ctl_start():
    porto_connection_mock = mock.Mock()
    qemu_mon_mock = mock.Mock()
    get_qemu_container_mock = mock.Mock()
    qemu_container_mock = mock.Mock()
    _qemu_ctl = qemu_ctl.QemuCtl(porto_connection_mock, None)
    _qemu_ctl._qemu_mon = qemu_mon_mock
    _qemu_ctl._get_qemu_container = get_qemu_container_mock
    start_args = 'qemu_launcher.sh', '/', 'password'

    # case: container already exists
    get_qemu_container_mock.return_value = qemu_container_mock
    with pytest.raises(_qemu_ctl.QemuContainerAlreadyExists):
        _qemu_ctl.start(*start_args)
    get_qemu_container_mock.assert_called_with(new_one=False)

    # case: success start
    get_qemu_container_mock.reset_mock()
    get_qemu_container_mock.return_value = None
    get_qemu_container_mock.side_effect = [None, qemu_container_mock]
    _qemu_ctl.start(*start_args)
    assert qemu_container_mock.SetProperty.call_count == 3
    qemu_container_mock.SetProperty.assert_has_calls([
        mock.call("stdin_path", "/dev/null"),
        mock.call("command", "/bin/bash -x qemu_launcher.sh"),
        mock.call("cwd", start_args[1])
    ], any_order=True)
    qemu_mon_mock.set_vnc_password.assert_called_with(start_args[2])

    assert get_qemu_container_mock.call_count == 2
    get_qemu_container_mock.assert_has_calls([mock.call(new_one=False), mock.call(new_one=True)])
    qemu_container_mock.Start.assert_called_with()

    # case: any Exception with container
    get_qemu_container_mock.reset_mock()
    get_qemu_container_mock.side_effect = [None, Exception('test')]
    with pytest.raises(_qemu_ctl.QemuContainerCrashed):
        _qemu_ctl.start(*start_args)

    # case: any Exception with qemu_mon
    get_qemu_container_mock.reset_mock()
    get_qemu_container_mock.side_effect = [None, qemu_container_mock]
    qemu_mon_mock.set_vnc_password.side_effect = qemu_ctl.QemuMonError()
    qemu_container_mock.GetProperty.return_value = 'any text'
    with pytest.raises(_qemu_ctl.QemuContainerCrashed):
        _qemu_ctl.start(*start_args)

    qemu_container_mock.GetProperty.assert_called_with('stderr')


def test_qemu_ctl_reset():
    porto_connection_mock = mock.Mock()
    qemu_mon_mock = mock.Mock()
    get_qemu_container_mock = mock.Mock()
    qemu_container_mock = mock.Mock()
    _qemu_ctl = qemu_ctl.QemuCtl(porto_connection_mock, None)
    _qemu_ctl._qemu_mon = qemu_mon_mock
    _qemu_ctl._get_qemu_container = get_qemu_container_mock

    # case: container does not exists
    get_qemu_container_mock.return_value = None
    with pytest.raises(_qemu_ctl.QemuContainerDoesNotExists):
        _qemu_ctl.reset()
    get_qemu_container_mock.assert_called_with(new_one=False)

    # case: container exists
    get_qemu_container_mock.return_value = qemu_container_mock
    _qemu_ctl.reset()
    qemu_mon_mock.system_reset.assert_called_with()


def test_qemu_ctl_try_graceful_stop():
    porto_connection_mock = mock.Mock()
    qemu_mon_mock = mock.Mock()
    get_qemu_container_mock = mock.Mock()
    qemu_container_mock = mock.Mock()
    _qemu_ctl = qemu_ctl.QemuCtl(porto_connection_mock, None)
    _qemu_ctl._qemu_mon = qemu_mon_mock
    _qemu_ctl._get_qemu_container = get_qemu_container_mock

    # case: container does not exists
    get_qemu_container_mock.return_value = None
    with pytest.raises(_qemu_ctl.QemuContainerDoesNotExists):
        list(_qemu_ctl.try_graceful_stop())
    get_qemu_container_mock.assert_called_with(new_one=False)

    # case: success stop
    get_qemu_container_mock.return_value = qemu_container_mock
    qemu_container_mock.Wait.return_value = ['qemu']
    list(_qemu_ctl.try_graceful_stop())
    qemu_mon_mock.system_powerdown.assert_called_with(timeout=10)

    # case: failed with timeout
    get_qemu_container_mock.return_value = qemu_container_mock
    qemu_mon_mock.system_powerdown.side_effect = lambda *args, **kwargs: time.sleep(1)
    qemu_container_mock.Wait.side_effect = [[], [], [], []]
    yield_calls = []
    with pytest.raises(_qemu_ctl.QemuGracefulStopTimeout):
        for left_seconds in _qemu_ctl.try_graceful_stop(timeout=2):
            yield_calls.append(left_seconds)
    assert yield_calls == [1, 0]

    # case: porto container does not exists
    get_qemu_container_mock.return_value = qemu_container_mock
    qemu_container_mock.Wait.side_effect = qemu_ctl.PortoContainerNotExists()
    list(_qemu_ctl.try_graceful_stop())
    qemu_mon_mock.system_powerdown.assert_called_with(timeout=10)


def test_qemu_ctl_power_off():
    porto_connection_mock = mock.Mock()
    qemu_mon_mock = mock.Mock()
    get_qemu_container_mock = mock.Mock()
    qemu_container_mock = mock.Mock()
    _qemu_ctl = qemu_ctl.QemuCtl(porto_connection_mock, None)
    _qemu_ctl._qemu_mon = qemu_mon_mock
    _qemu_ctl._get_qemu_container = get_qemu_container_mock

    # case: container does not exists
    get_qemu_container_mock.return_value = None
    with pytest.raises(_qemu_ctl.QemuContainerDoesNotExists):
        _qemu_ctl.power_off()
    get_qemu_container_mock.assert_called_with(new_one=False)

    # case: success
    get_qemu_container_mock.return_value = qemu_container_mock
    qemu_container_mock.Wait.return_value = ['qemu']
    qemu_container_mock.GetProperty.return_value = '0'
    _qemu_ctl.power_off()
    qemu_container_mock.GetProperty.assert_called_with('exit_code')

    # case: qemu stop with exit code > 0
    get_qemu_container_mock.return_value = qemu_container_mock
    qemu_container_mock.Wait.return_value = ['qemu']
    qemu_container_mock.GetProperty.return_value = '5'
    with pytest.raises(_qemu_ctl.QemuContainerCrashed) as error:
        _qemu_ctl.power_off()
    assert error.value.message == 'Qemu exited with code: 5'

    # case: qemu_mon raise error
    get_qemu_container_mock.return_value = qemu_container_mock
    qemu_mon_mock.side_effect = qemu_ctl.QemuMonError()
    qemu_container_mock.GetProperty.return_value = '10'
    with pytest.raises(_qemu_ctl.QemuContainerCrashed) as error:
        _qemu_ctl.power_off()
    assert error.value.message == 'Qemu exited with code: 10'
