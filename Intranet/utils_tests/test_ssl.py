import pytest

from intranet.crt.exceptions import CrtError
from __tests__.utils.ssl import generate_private_key
from intranet.crt.utils.ssl import PrivateKeyCryptographer

pytestmark = pytest.mark.django_db


def test_private_key_cryptographer():
    private_key = generate_private_key()

    cryptographer = PrivateKeyCryptographer(['pass1'])
    encrypted_private_key = cryptographer.encrypt(private_key)
    assert encrypted_private_key.startswith(b'-----BEGIN ENCRYPTED PRIVATE KEY-----')

    cryptographer = PrivateKeyCryptographer(['pass2', 'pass1'])
    decrypted_private_key = cryptographer.decrypt(encrypted_private_key)
    assert decrypted_private_key == private_key

    encrypted_private_key = cryptographer.encrypt(private_key)
    cryptographer = PrivateKeyCryptographer(['pass2'])
    decrypted_private_key = cryptographer.decrypt(encrypted_private_key)
    assert decrypted_private_key == private_key

    cryptographer = PrivateKeyCryptographer(['pass1', 'pass3'])
    with pytest.raises(CrtError):
        cryptographer.decrypt(encrypted_private_key)
