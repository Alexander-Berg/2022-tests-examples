import mock

from infra.ya_salt.lib import subprocutil
from infra.ya_salt.lib import package_manager


def test_get_env_has_hostman():
    env, err = package_manager.Dpkg.get_env()
    assert err is None
    assert env['HOSTMAN'] == '1'


def test_parse_dpkg_query_out():
    # Check invalid cases
    s, err = package_manager.parse_dpkg_query_out('\n')
    assert s is None
    assert err
    # Check valid case
    s, err = package_manager.parse_dpkg_query_out('pkg\t20181003\tinstall ok installed\n')
    assert err is None
    assert s.name == 'pkg'
    assert s.version == '20181003'
    assert s.installed is True


def test_get_package_status():
    # Check that we failed to query package status
    m = mock.Mock()
    m.return_value = ('', '', subprocutil.Status(False, 'failed to execute'))
    p = package_manager.Dpkg(check_out=m)
    yasmagent = 'yandex-yasmagent'
    p_status, err = p.get_package_status(yasmagent)
    assert p_status is None
    assert err is not None
    assert m.call_count == 1
    expected_cmd = p.GET_QUERY[:]
    expected_cmd.append(yasmagent)
    assert m.call_args[0] == (expected_cmd,), m.call_args[0]
    assert m.call_args[1] > 10  # Timeout
    # Check that package is not installed
    m.reset_mock()
    m.return_value = (None,  # stdout
                      'dpkg-query: no packages found matching yandex-yasmagent1\n',  # stderr
                      subprocutil.Status(False, 'exited with status 1'),  # status
                      )
    p = package_manager.Dpkg(check_out=m)
    p_status, err = p.get_package_status(yasmagent)
    assert err is None
    assert p_status is not None
    assert p_status.name == yasmagent
    assert p_status.version == 'unknown'
    assert p_status.installed is False
    # Check good status
    m.reset_mock()
    m.return_value = ('yandex-yasmagent\t2.213-20181018\tinstall ok installed\n', '', subprocutil.Status(ok=True))
    p = package_manager.Dpkg(check_out=m)
    p_status, err = p.get_package_status(yasmagent)
    assert err is None
    assert p_status is not None
    assert p_status.name == yasmagent
    assert p_status.version == '2.213-20181018'
    assert p_status.installed is True


def test_install():
    # Failed case
    m = mock.Mock()
    m.return_value = ('', 'some error', subprocutil.Status(False, 'exited with code 100'))
    p = package_manager.Dpkg(check_out=m)
    p_status, err = p.install('pkg', '2.0-1')
    assert p.metrics().install_failures == 1
    assert p.metrics().installed_ok == 0
    assert p_status is None
    assert err is not None
    # Good case
    m.reset_mock()
    m.return_value = ('', '', subprocutil.Status(ok=True))
    p = package_manager.Dpkg(check_out=m)
    p_status, err = p.install('pkg', '2.0-1')
    assert err is None
    assert p_status.name == 'pkg'
    assert p_status.version == '2.0-1'
    assert p_status.installed is True
    assert p.metrics().install_failures == 0
    assert p.metrics().installed_ok == 1
    env, _ = p.get_env()
    m.assert_called_once_with(['/usr/bin/apt-get',
                               'install',
                               '-q', '-y',
                               '-o', 'DPkg::Options::=--force-confold',
                               '-o', 'DPkg::Options::=--force-confdef',
                               '--force-yes',
                               '--no-install-recommends',
                               'pkg=2.0-1'],
                              env=env,
                              timeout=p.INSTALL_TIMEOUT_SECONDS)


def test_purge():
    # Failed case
    m = mock.Mock()
    m.return_value = ('', 'some error', subprocutil.Status(False, 'exited with code 100'))
    p = package_manager.Dpkg(check_out=m)
    err = p.purge('pkg')
    assert p.metrics().purge_failures == 1
    assert err is not None
    # Good case
    m.reset_mock()
    m.return_value = ('', '', subprocutil.Status(ok=True))
    p = package_manager.Dpkg(check_out=m)
    err = p.purge('pkg')
    assert err is None
    assert p.metrics().purged_ok == 1
    env, _ = p.get_env()
    m.assert_called_once_with(['/usr/bin/apt-get',
                               'purge',
                               '-q', '-y',
                               'pkg'],
                              env=env,
                              timeout=p.PURGE_TIMEOUT_SECONDS)


def test_purge_set():
    # Failed case
    m = mock.Mock()
    m.return_value = ('', 'some error', subprocutil.Status(False, 'exited with code 100'))
    p = package_manager.Dpkg(check_out=m)
    err = p.purge_set(['pkg1', 'pkg2'])
    assert p.metrics().purge_failures == 1
    assert err is not None
    env, _ = p.get_env()
    m.assert_called_once_with(['/usr/bin/apt-get', 'purge', '-q', '-y', 'pkg1', 'pkg2'],
                              env=env,
                              timeout=p.PURGE_TIMEOUT_SECONDS)
    # Good case
    m.reset_mock()
    m.return_value = ('', '', subprocutil.Status(ok=True))
    p = package_manager.Dpkg(check_out=m)
    err = p.purge_set(['pkg1', 'pkg2'])
    assert err is None
    assert p.metrics().purged_ok == 1
    m.assert_called_once_with(['/usr/bin/apt-get', 'purge', '-q', '-y', 'pkg1', 'pkg2'],
                              env=env,
                              timeout=p.PURGE_TIMEOUT_SECONDS)


def test_install_set():
    # Failed case
    m = mock.Mock()
    m.return_value = ('', 'some error', subprocutil.Status(False, 'exited with code 100'))
    p = package_manager.Dpkg(check_out=m)
    err = p.install_set([('pkg', '2.0-1'), ('pkg-2', '2.0-2')])
    assert err is not None
    assert p.metrics().install_failures == 2
    assert p.metrics().installed_ok == 0
    env, _ = p.get_env()
    m.assert_called_once_with(['/usr/bin/apt-get',
                               'install',
                               '-q', '-y',
                               '-o', 'DPkg::Options::=--force-confold',
                               '-o', 'DPkg::Options::=--force-confdef',
                               '--force-yes',
                               '--no-install-recommends',
                               'pkg=2.0-1', 'pkg-2=2.0-2'],
                              env=env,
                              timeout=p.SET_TIMEOUT_SECONDS)
    # Good case
    m.reset_mock()
    m.return_value = ('', '', subprocutil.Status(ok=True))
    p = package_manager.Dpkg(check_out=m)
    err = p.install_set([('pkg', '2.0-1'), ('pkg-2', '2.0-2')])
    assert err is None
    assert p.metrics().install_failures == 0
    assert p.metrics().installed_ok == 2
    m.assert_called_once_with(['/usr/bin/apt-get',
                               'install',
                               '-q', '-y',
                               '-o', 'DPkg::Options::=--force-confold',
                               '-o', 'DPkg::Options::=--force-confdef',
                               '--force-yes',
                               '--no-install-recommends',
                               'pkg=2.0-1', 'pkg-2=2.0-2'],
                              env=env,
                              timeout=p.SET_TIMEOUT_SECONDS)


def test_update():
    # Failed case (dpkg fails)
    m = mock.Mock()
    m.return_value = ('', 'some error', subprocutil.Status(False, 'exited with code 100'))
    p = package_manager.Dpkg(check_out=m)
    err = p.update()
    assert err is not None
    env, err = p.get_env()
    assert err is None
    m.assert_called_once_with(p.APT_GET_UPDATE,
                              env=env,
                              timeout=p.APT_GET_TIMEOUT)
    # OK case
    m.reset_mock()
    m.return_value = ('', '', subprocutil.Status(True))
    p = package_manager.Dpkg(check_out=m)
    err = p.update()
    assert err is None
    m.assert_called_once_with(p.APT_GET_UPDATE,
                              env=env,
                              timeout=p.APT_GET_TIMEOUT)


def test_list():
    # Failed case (dpkg fails)
    m = mock.Mock()
    m.return_value = ('', 'some error', subprocutil.Status(False, 'exited with code 100'))
    p = package_manager.Dpkg(check_out=m)
    l, err = p.list()
    assert err is not None
    # Failed case (failed to parse output)
    m.return_value = ('yandex-yasmagent\t2.213-20181018\tinstall ok installed\t1\t\n', '', subprocutil.Status(ok=True))
    p = package_manager.Dpkg(check_out=m)
    l, err = p.list()
    assert err is not None
    # Good case
    m.return_value = ('yandex-yasmagent\t2.213-20181018\tinstall ok installed\n'
                      'xinetd\t1:2.3.14-7ubuntu4\tinstall ok installed\n', '', subprocutil.Status(ok=True))
    p = package_manager.Dpkg(check_out=m)
    l, err = p.list()
    assert err is None
    assert len(l) == 2
    assert l[0].name == 'yandex-yasmagent'
    assert l[0].version == '2.213-20181018'
    assert l[0].installed is True
    assert l[1].name == 'xinetd'
    assert l[1].version == '1:2.3.14-7ubuntu4'
    assert l[1].installed is True
