# coding: utf-8

from __future__ import unicode_literals

import uuid

from cryptography import x509
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from django.utils import timezone
from django.utils.encoding import force_text

from intranet.crt.utils.ssl import get_x509_custom_extensions, PemCertificateRequest

TEST_PRIVATE_KEY_SIZE = 1024
TEST_PRIVATE_KEY_EXPONENT = 65537


def generate_private_key():
    return (
        rsa.generate_private_key(
            public_exponent=TEST_PRIVATE_KEY_EXPONENT,
            key_size=TEST_PRIVATE_KEY_SIZE,
            backend=default_backend(),
        )
        .private_bytes(
            encoding=serialization.Encoding.PEM,
            format=serialization.PrivateFormat.PKCS8,
            encryption_algorithm=serialization.NoEncryption(),
        )
    )


def create_pem_certificate(request, private_key_data=None, not_before=timezone.now()):
    if private_key_data is None:
        private_key_data = generate_private_key()

    private_key = serialization.load_pem_private_key(
        data=private_key_data,
        password=None,
        backend=default_backend()
    )

    request = PemCertificateRequest(request)

    serial_number = int(uuid.uuid4())
    request_subject = request.x509_object.subject
    dns_names = [x509.DNSName(force_text(san.encode('idna'))) for san in request.sans]
    alt_names = x509.SubjectAlternativeName(dns_names)
    basic_constraints = x509.BasicConstraints(ca=True, path_length=0)
    now = timezone.now()
    not_after = now + timezone.timedelta(days=365)

    cert = (
        x509.CertificateBuilder()
            .subject_name(request_subject)
            .issuer_name(request_subject)
            .public_key(request.x509_object.public_key())
            .serial_number(serial_number)
            .not_valid_before(not_before)
            .not_valid_after(not_after)
            .add_extension(basic_constraints, critical=False)
            .add_extension(alt_names, critical=False)
    )
    csr_extensions = get_x509_custom_extensions(request.x509_object)
    for csr_extension in csr_extensions.values():
        cert = cert.add_extension(csr_extension, critical=False)
    cert = cert.sign(private_key, hashes.SHA256(), default_backend())
    return cert.public_bytes(encoding=serialization.Encoding.PEM)
