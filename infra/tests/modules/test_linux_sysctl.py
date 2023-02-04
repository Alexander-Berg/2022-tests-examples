# Import Python libs
from __future__ import absolute_import

import errno

import mock
# Import Salt Libs
import pytest

from salt.modules import linux_sysctl
from salt.modules import systemd
from salt.exceptions import CommandExecutionError

# Globals
linux_sysctl.__salt__ = {}
linux_sysctl.__context__ = {}
systemd.__context__ = {}


def test_get():
    """
    Tests the return of get function
    """
    with mock.patch('__builtin__.open', mock.mock_open(read_data='1')) as f:
        assert linux_sysctl.get('net.ipv4.ip_forward') == '1'
    f.assert_called_once_with('/proc/sys/net/ipv4/ip_forward')


def test_assign_proc_sys_failed():
    """
    Tests if we fail to assign unknown sysctl
    """
    with pytest.raises(CommandExecutionError):
        linux_sysctl.assign('net.ipv4.ip_forward_me_please', '1')


def test_assign_cmd_failed():
    """
    Tests if we fail to assign with invalid argument.
    """
    m = mock.mock_open()
    m.return_value.write.side_effect = EnvironmentError(errno.EINVAL, 'Invalid value')
    with mock.patch('__builtin__.open', m) as f:
        with pytest.raises(CommandExecutionError):
            linux_sysctl.assign('net.ipv4.ip_forward', 'backward')
    f.assert_called_once_with('/proc/sys/net/ipv4/ip_forward', 'w')


def test_assign_success():
    """
    Tests the return of successful assign function.
    """
    ret = {'net.ipv4.ip_forward': '1'}
    m = mock.mock_open()
    with mock.patch('__builtin__.open', m) as f:
        actual = linux_sysctl.assign('net.ipv4.ip_forward', '1')
    assert actual == ret
    f.assert_called_once_with('/proc/sys/net/ipv4/ip_forward', 'w')
