import pytest
from mock import patch

from staff.person_profile.controllers.gpg_key import gpg_fingerprint, GPGKeyNoFingerprintError


def test_gpg_fingerprint_unicode():
    with patch('subprocess.Popen.communicate') as comm_mock:
        comm_mock.return_value = ('тест'.encode('utf-8'), '')
        with pytest.raises(GPGKeyNoFingerprintError):
            gpg_fingerprint('не ключ')


def test_gpg_fingerprint_not_a_key():
    with pytest.raises(GPGKeyNoFingerprintError):
        gpg_fingerprint('не ключ')
