import io
import tarfile

import six

from awacs.lib import certs


def test_pack_certs():
    cert_id = 'cert1'
    public_key = b'public_key'
    private_key = b'private_key'
    expected_files = {
        './priv/{}.pem'.format(cert_id): private_key,
        './allCAs-{}.pem'.format(cert_id): public_key,
    }
    tls_keys = ('1st', '2nd', '3rd',)
    cert_tarball = certs.pack_certs(private_key=private_key, public_key=public_key, cert_id=cert_id)
    with tarfile.open(fileobj=io.BytesIO(cert_tarball), mode='r:gz') as t:
        for path, fl in six.iteritems(expected_files):
            try:
                actual_file = t.extractfile(path).read()
            except Exception as e:
                raise AssertionError(e)
            assert actual_file == fl
        for tls_key in tls_keys:
            try:
                actual_file = t.extractfile('./priv/{}.{}.key'.format(tls_key, cert_id)).read()
            except Exception as e:
                raise AssertionError(e)
            assert len(actual_file) == 48


def test_certs_compare():
    cert_id = 'cert1'
    public_key = b'public_key'
    private_key = b'private_key'
    tarball_1 = certs.pack_certs(private_key=private_key, public_key=public_key, cert_id=cert_id)

    is_changed, reason = certs.tarball_needs_update(cert_tarball=tarball_1,
                                                    private_key=private_key,
                                                    public_key=public_key,
                                                    cert_id=cert_id)
    assert not is_changed
    assert reason is None

    is_changed, reason = certs.tarball_needs_update(cert_tarball=tarball_1,
                                                    private_key=b'-',
                                                    public_key=public_key,
                                                    cert_id=cert_id)
    assert is_changed
    assert reason == '"./priv/cert1.pem" has changed'

    is_changed, reason = certs.tarball_needs_update(cert_tarball=tarball_1,
                                                    private_key=private_key,
                                                    public_key=b'-',
                                                    cert_id=cert_id)
    assert is_changed
    assert reason == '"./allCAs-cert1.pem" has changed'

    is_changed, reason = certs.tarball_needs_update(cert_tarball=tarball_1,
                                                    private_key=private_key,
                                                    public_key=public_key,
                                                    cert_id='-')
    assert is_changed
    assert reason == 'file "./priv/-.pem" not found'

    is_changed, reason = certs.tarball_needs_update(cert_tarball=None,
                                                    private_key=private_key,
                                                    public_key=public_key,
                                                    cert_id=cert_id)
    assert is_changed
    assert reason == 'added secrets.tgz'

    is_changed, reason = certs.tarball_needs_update(cert_tarball=b'abc',
                                                    private_key=private_key,
                                                    public_key=public_key,
                                                    cert_id=cert_id)
    assert is_changed
    assert reason == 'failed to read old secrets.tgz'
