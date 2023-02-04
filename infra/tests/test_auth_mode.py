from ya.skynet.services.portoshell import proxy
proxy.register_proxies()  # noqa

from ya.skynet.services.portoshell.slots.auth_mode import get_allowed_cert_kind, ssh_keys_allowed
from infra.skylib.certificates import CertKind


def test_get_allowed_cert_kind():
    assert get_allowed_cert_kind() == CertKind.Any
    assert get_allowed_cert_kind(None) == CertKind.Any
    assert get_allowed_cert_kind(None, None) == CertKind.Any

    assert get_allowed_cert_kind('unknown') == CertKind.Any
    assert get_allowed_cert_kind('unknown', 'all') == CertKind.Any
    assert get_allowed_cert_kind('unknown', 'secure') == CertKind.Secure

    assert get_allowed_cert_kind('insecure_and_secure') == CertKind.Insecure | CertKind.Secure
    assert get_allowed_cert_kind('insecure_and_secure', 'secure') == CertKind.Secure
    assert get_allowed_cert_kind('insecure_and_secure', 'unknown') == CertKind.Insecure | CertKind.Secure


def test_ssh_keys_allowed():
    assert ssh_keys_allowed()
    assert ssh_keys_allowed(None)
    assert ssh_keys_allowed(None, None)

    assert ssh_keys_allowed('unknown')
    assert ssh_keys_allowed('unknown', 'all')
    assert not ssh_keys_allowed('unknown', 'secure')

    assert not ssh_keys_allowed('insecure_and_secure')
    assert not ssh_keys_allowed('insecure_and_secure', 'secure')
    assert not ssh_keys_allowed('insecure_and_secure', 'unknown')
