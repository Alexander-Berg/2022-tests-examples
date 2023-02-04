import mock

from infra.ya_salt.lib.components import apt
from infra.ya_salt.proto import ya_salt_pb2


def test_run_fix():
    pkg_man = mock.Mock()
    pkg_man.repair_half_configured.return_value = None
    # Case: OK
    listdir = mock.Mock()
    listdir.return_value = ['tmp.i']
    a = apt.Apt(pkg_man)
    assert a._run_fix(listdir) is None
    listdir.assert_called_once_with(apt.Apt.UPDATES_DIR)
    # Case: listdir failed
    listdir.reset_mock()
    listdir.side_effect = EnvironmentError(2, '')
    assert a._run_fix(listdir) is not None
    listdir.assert_called_once_with(apt.Apt.UPDATES_DIR)
    # Case: tmp.i file found
    listdir.reset_mock()
    listdir.side_effect = None
    listdir.return_value = ['001', 'tmp.i', '002']
    assert a._run_fix(listdir) is None
    listdir.assert_called_once_with(apt.Apt.UPDATES_DIR)
    pkg_man.repair_half_configured.assert_called_once_with()


def test_apt_update():
    pkg_man = mock.Mock()
    pkg_man.update.return_value = 'non existing binary'
    # Test exec failure
    a = apt.Apt(pkg_man)
    m = ya_salt_pb2.Condition()
    a._run_update(m)
    assert m.status == 'False'
    assert m.message == 'non existing binary'
    pkg_man.update.assert_called_once_with()
    # Test good result
    pkg_man.reset_mock()
    pkg_man.update.return_value = None
    m = ya_salt_pb2.Condition()
    a._run_update(m)
    assert m.status == 'True'
    assert m.message == 'OK'
    pkg_man.update.assert_called_once_with()


def test_calc_next_update():
    for _ in xrange(10):
        next_time = apt.Apt.calc_next_update(time_func=lambda: 1000)
        min_time = 1000 + apt.Apt.PERIOD_SECONDS
        assert min_time <= next_time <= min_time + apt.Apt.JITTER_SECONDS
