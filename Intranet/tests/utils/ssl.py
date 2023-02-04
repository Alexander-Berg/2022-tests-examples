import datetime

from cryptography import x509
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.x509.oid import NameOID

from django.utils import timezone
from django.utils.encoding import force_text

from intranet.crt.csr.config import (
    TpmSmartcard1CSubjectCsrConfig,
    TpmSmartcard1CForeignSubjectCsrConfig,
    TempPcCsrConfig,
)
from intranet.crt.utils.test_ca import TEST_PRIVATE_KEY_EXPONENT, TEST_PRIVATE_KEY_SIZE, generate_private_key
from intranet.crt.utils.ssl import PemCertificate, PemCertificateRequest


class CrlBuilder(object):
    backend = default_backend()
    private_key = rsa.generate_private_key(
        public_exponent=TEST_PRIVATE_KEY_EXPONENT,
        key_size=TEST_PRIVATE_KEY_SIZE,
        backend=backend,
    )

    def __init__(self):
        ca_name = x509.Name([x509.NameAttribute(x509.NameOID.COMMON_NAME, 'Test CA')])
        self.builder = (
            x509.CertificateRevocationListBuilder()
            .issuer_name(ca_name)
            .next_update(timezone.now() + datetime.timedelta(days=1))
        )

    def set_last_update(self, date):
        self.builder = self.builder.last_update(date)
        return self

    def add_cert(self, serial_number, revoke_date=None, hold=False):
        if revoke_date is None:
            revoke_date = timezone.now()

        reason_flag = x509.ReasonFlags.key_compromise
        if hold:
            reason_flag = x509.ReasonFlags.certificate_hold

        revoked_cert = (
            x509.RevokedCertificateBuilder()
            .serial_number(int(serial_number, 16))
            .revocation_date(revoke_date)
            .add_extension(x509.CRLReason(reason_flag), critical=False)
            .build(self.backend)
        )

        self.builder = self.builder.add_revoked_certificate(revoked_cert)

        return self

    def get_der_bytes(self):
        crl = self.builder.sign(
            private_key=self.private_key,
            algorithm=hashes.SHA256(),
            backend=self.backend,
        )

        return crl.public_bytes(serialization.Encoding.DER)


def get_modulus_from_pem(pem):
    try:
        return PemCertificate(pem).x509_object.public_key().public_numbers().n
    except ValueError:
        return PemCertificateRequest(pem).x509_object.public_key().public_numbers().n


class CsrBuilder(object):
    backend = default_backend()
    private_key = serialization.load_pem_private_key(
        data=generate_private_key(),
        password=None,
        backend=default_backend()
    )

    def __init__(self):
        self.subject = [
            x509.NameAttribute(NameOID.COUNTRY_NAME, "RU"),
            x509.NameAttribute(NameOID.LOCALITY_NAME, "Moscow"),
        ]
        self.builder = x509.CertificateSigningRequestBuilder()

    def add_common_name(self, value):
        self.subject.append(x509.NameAttribute(NameOID.COMMON_NAME, value))
        return self

    def add_email_address(self, value):
        self.subject.append(x509.NameAttribute(NameOID.EMAIL_ADDRESS, value))
        return self

    def add_unit_name(self, value):
        self.subject.append(x509.NameAttribute(NameOID.ORGANIZATIONAL_UNIT_NAME, value))
        return self

    def get_pem_csr(self):
        csr = self.builder.subject_name(x509.Name(self.subject))\
            .sign(
            self.private_key,
            hashes.SHA256(),
            self.backend,
        )
        return csr.public_bytes(encoding=serialization.Encoding.PEM)


def build_tpm_smartcard_1c_csr(common_name, foreign=False):
    return force_text(
        TpmSmartcard1CForeignSubjectCsrConfig(
            cn_1=common_name,
            ou_1='ForeignUsers',
            dc_1='ld',
            dc_2='yandex',
            dc_3='ru',
        ).get_csr()
    ) if foreign else force_text(
        TpmSmartcard1CSubjectCsrConfig(
            cn_1=common_name,
            cn_2='Users',
            dc_1='ld',
            dc_2='yandex',
            dc_3='ru',
        ).get_csr()
    )


def build_temp_pc_csr(common_name):
    return force_text(
        TempPcCsrConfig(common_name=common_name).get_csr()
    )
