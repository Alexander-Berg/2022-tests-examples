import pytest

from library.auth.key import RSAKey, ECDSAKey, Certificate
from infra.skylib.openssh_krl import (
    CertificatesSection,
    CertificatesSectionSerialList,
    CertificatesSectionSerialRange,
    CertificatesSectionSerialBitmap,
    CertificatesSectionKeyId,
    FingerprintSHA1Section,
    FingerprintSHA256Section,
    KRL,
)


def make_ca():
    return RSAKey.generate()


def make_key():
    return ECDSAKey.generate()


def make_cert_section(ca):
    return CertificatesSection(
        ca_key=ca.networkRepresentation(),
        reserved=b'',
        cert_sections=[
            CertificatesSectionSerialList(serials=[2, 12, 80, 506]),
            CertificatesSectionSerialRange(serial_min=100, serial_max=500),
            CertificatesSectionSerialBitmap(serial_offset=490, revoked_keys_bitmap=b'\xaa\xaa\xaa'),
            CertificatesSectionKeyId([b'revoked_key_id']),
        ],
    )


@pytest.fixture(scope='session')
def ca1():
    return make_ca()


@pytest.fixture(scope='session')
def ca2():
    return make_ca()


@pytest.fixture(scope='session')
def ca3():
    return make_ca()


@pytest.fixture(scope='function')
def revoked_key1():
    return make_key()


@pytest.fixture(scope='function')
def revoked_key2():
    return make_key()


@pytest.fixture(scope='function')
def valid_key():
    return make_key()


@pytest.fixture
def krl(ca1, ca2, revoked_key1, revoked_key2):
    return KRL(
        header=None,
        sections=[
            make_cert_section(ca1),
            make_cert_section(ca2),
            FingerprintSHA1Section([revoked_key1.fingerprint('sha1')]),
            FingerprintSHA256Section([revoked_key2.fingerprint('sha256')]),
        ],
        signature=None,
    )


@pytest.mark.parametrize('serial', list(range(10)))
def test_serial_list(serial):
    banned = [2, 12, 80, 506]
    key_id = b''
    section = CertificatesSectionSerialList(serials=banned)
    assert section.cert_valid(serial, key_id) == (serial not in banned)


@pytest.mark.parametrize('serial', list(range(10)))
def test_serial_range(serial):
    key_id = b''
    section = CertificatesSectionSerialRange(serial_min=3, serial_max=5)
    assert section.cert_valid(serial, key_id) == (serial < 3 or serial > 5)


@pytest.mark.parametrize('serial', list(range(30)))
def test_serial_bitmap(serial):
    key_id = b''
    # banned 1st of every 4 starting from 4 till 19: 1000100010001000
    section = CertificatesSectionSerialBitmap(serial_offset=4, revoked_keys_bitmap=b'\x88\x88')
    assert section.cert_valid(serial, key_id) == (serial < 4 or serial > 19 or serial % 4 != 0)


def test_key_id():
    section = CertificatesSectionKeyId([b'revoked', b'id1'])
    assert section.cert_valid(0, b'key_id1')
    assert not section.cert_valid(0, b'revoked')
    assert not section.cert_valid(0, b'id1')


@pytest.mark.parametrize('serial', list(range(600)))
@pytest.mark.parametrize('used_ca', ['ca1', 'ca2', 'ca3'])
@pytest.mark.parametrize('key_id', [b'id1', b'revoked_key_id'])
def test_krl_cert_validation(krl, ca1, ca2, ca3, serial, used_ca, key_id):
    ca = {
        'ca1': ca1,
        'ca2': ca2,
        'ca3': ca3,
    }[used_ca]

    cert = Certificate(
        public_key=make_key().publicKey(),
        serial=serial,
        cert_type=1,
        key_id=key_id,
        principals=[],
        valid_range=(0, 1 << 63),
        critical_options={},
        extensions={},
        signing_key=ca,
        blob=None,
    )

    should_be_invalid = (
        key_id == b'revoked_key_id'
        or serial in (2, 12, 80, 506)
        or (100 <= serial <= 500)
        or ((490 <= serial < 514) and serial % 2 == 0)
    ) and ca is not ca3

    assert (not should_be_invalid) == krl.cert_valid(cert)


@pytest.mark.parametrize('used_key', ['revoked_key1', 'revoked_key2', 'valid_key'])
def test_krl_key_validation(krl, ca3, revoked_key1, revoked_key2, valid_key, used_key):
    key = {
        'revoked_key1': revoked_key1,
        'revoked_key2': revoked_key1,
        'valid_key': valid_key,
    }[used_key]

    cert = Certificate(
        public_key=key.publicKey(),
        serial=9999,
        cert_type=1,
        key_id=b'',
        principals=[],
        valid_range=(0, 1 << 63),
        critical_options={},
        extensions={},
        signing_key=ca3,
        blob=None,
    )

    should_be_invalid = used_key != 'valid_key'

    assert (not should_be_invalid) == krl.cert_valid(cert)
