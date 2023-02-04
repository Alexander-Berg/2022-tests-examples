from infra.dist.cacus.lib import common
import mock
import pytest


def test_gpg_sign():
    lock_mgr = mock.Mock()
    lock_mgr.__exit__ = mock.Mock(return_value=None)
    lock_mgr.__enter__ = mock.Mock()
    lock = mock.Mock(return_value=lock_mgr)
    process = mock.Mock()
    popen = mock.Mock(return_value=process)
    process.communicate = mock.Mock(return_value=('BEGIN PGP', 'error'))
    with pytest.raises(common.GPGSignError):
        common.gpg_sign('mock', 'closedsource@good.inc', popen_class=popen, lock=lock)

    process.communicate = mock.Mock(return_value=('NO PGP', 'ok'))
    with pytest.raises(common.GPGSignError):
        common.gpg_sign('mock', 'closedsource@good.inc', popen_class=popen, lock=lock)

    process.communicate = mock.Mock(return_value=('BEGIN PGP', 'ok'))
    assert common.gpg_sign('mock', 'closedsource@good.inc', popen_class=popen, lock=lock) == 'BEGIN PGP'


def test_gpg_sign_in_place():
    lock_mgr = mock.Mock()
    lock_mgr.__exit__ = mock.Mock(return_value=None)
    lock_mgr.__enter__ = mock.Mock()
    lock = mock.Mock(return_value=lock_mgr)
    process = mock.Mock()
    popen = mock.Mock(return_value=process)
    process.communicate = mock.Mock(return_value=('BEGIN PGP', 'error'))
    with pytest.raises(common.GPGSignError):
        common.gpg_sign_in_place('mock', 'closedsource@good.inc', popen_class=popen, lock=lock)

    process.communicate = mock.Mock(return_value=('NO PGP', 'ok'))
    with pytest.raises(common.GPGSignError):
        common.gpg_sign_in_place('mock', 'closedsource@good.inc', popen_class=popen, lock=lock)

    process.communicate = mock.Mock(return_value=('BEGIN PGP', 'ok'))
    assert common.gpg_sign_in_place('mock', 'closedsource@good.inc', popen_class=popen, lock=lock) == 'BEGIN PGP'
