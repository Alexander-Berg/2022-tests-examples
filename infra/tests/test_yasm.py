import mock

from infra.ya_salt.lib.components import yasm
from infra.ya_salt.lib import package_manager
from infra.ya_salt.proto import ya_salt_pb2


def test_install_agent_package():
    version = '2.222-20181113'
    # Package status query failed
    status = ya_salt_pb2.PackageStatus()
    pkg_man = mock.Mock()
    pkg_man.get_package_status.return_value = (None, 'DPKG FAILED')
    y = yasm.Yasm('testing', pkg_man, None)
    assert not y.install_agent_package('2.222-20181113', status)
    assert status.last_update_ok.status == 'False'
    assert status.version == 'unknown'
    pkg_man.get_package_status.assert_called_once_with(yasm.Yasm.AGENT_PACKAGE)
    # Versions match
    status.Clear()
    pkg_man.reset_mock()
    ps = package_manager.PackageStatus(yasm.Yasm.AGENT_PACKAGE, version, True)
    pkg_man.get_package_status.return_value = (ps, None)
    y = yasm.Yasm('testing', pkg_man, None)
    assert not y.install_agent_package(version, status)
    assert status.last_update_ok.status == 'True'
    assert status.version == version
    pkg_man.get_package_status.assert_called_once_with(yasm.Yasm.AGENT_PACKAGE)
    # No version specified
    status.Clear()
    pkg_man.reset_mock()
    assert not y.install_agent_package('', status)
    assert status.last_update_ok.status == 'False'
    assert status.version == version
    # Orly check failed
    status.Clear()
    pkg_man.reset_mock()
    ps = package_manager.PackageStatus(yasm.Yasm.AGENT_PACKAGE, '1.1', True)
    pkg_man.get_package_status.return_value = (ps, None)
    orly = mock.Mock()
    orly.start_operation.return_value = 'START FAILED'
    y = yasm.Yasm('testing', pkg_man, orly)
    assert not y.install_agent_package(version, status)
    assert status.last_update_ok.status == 'False'
    assert status.version == '1.1'
    pkg_man.get_package_status.assert_called_once_with(yasm.Yasm.AGENT_PACKAGE)
    # Install failed
    status.Clear()
    pkg_man.reset_mock()
    pkg_man.install.return_value = (None, 'install failed')
    orly = mock.Mock()
    orly.start_operation.return_value = None
    y = yasm.Yasm('testing', pkg_man, orly)
    assert not y.install_agent_package(version, status)
    assert status.last_update_ok.status == 'False'
    assert status.version == '1.1'
    pkg_man.install.assert_called_once_with(yasm.Yasm.AGENT_PACKAGE, version)
    # Everything went fine
    pkg_man.reset_mock()
    orly.reset_mock()
    pkg_man.install.return_value = (package_manager.PackageStatus(yasm.Yasm.AGENT_PACKAGE,
                                                                  version, True),
                                    None)
    y = yasm.Yasm('testing', pkg_man, orly)
    assert y.install_agent_package(version, status)
    assert status.last_update_ok.status == 'True'
    assert status.version == version
    pkg_man.install.assert_called_once_with(yasm.Yasm.AGENT_PACKAGE, version)


def test_run_agent_service():
    # Exception failure
    m = mock.Mock(side_effect=Exception('HTTP FAILED'))
    status = ya_salt_pb2.ServiceStatus()
    y = yasm.Yasm('testing', None, None)
    y.run_agent_service(status, m)
    m.assert_called_once_with(yasm.Yasm.AGENT_VERSION_URL, timeout=5)
    assert status.version == ''
    assert status.last_check_ok.status == 'False'
    assert status.last_check_ok.message
    # Response status failure
    resp = mock.Mock()
    resp.ok = False
    m = mock.Mock(return_value=resp)
    status.Clear()
    y = yasm.Yasm('testing', None, None)
    y.run_agent_service(status, m)
    m.assert_called_once_with(yasm.Yasm.AGENT_VERSION_URL, timeout=5)
    assert status.version == ''
    assert status.last_check_ok.status == 'False'
    assert status.last_check_ok.message
    # Response ok
    m.reset_mock()
    resp.reset_mock()
    resp.ok = True
    resp.content = '2.228-20181115'
    status.Clear()
    y = yasm.Yasm('testing', None, None)
    y.run_agent_service(status, m)
    m.assert_called_once_with(yasm.Yasm.AGENT_VERSION_URL, timeout=5)
    assert status.version == resp.content
    assert status.last_check_ok.status == 'True'
    assert status.last_check_ok.message == 'OK'


def test_run_helper_package():
    # TODO: implement
    pass
