import pytest

from intranet.crt.core.controllers.certificates import ImdmCertificateController
from intranet.crt.core.models import Certificate

pytestmark = pytest.mark.django_db


def test_duplicate_certificates(certificates):
    dup_certs = ImdmCertificateController.duplicate_certificates()

    assert {cert.serial_number for cert in list(dup_certs.keys())} == {'3', '7'}

    cert_3 = Certificate.objects.get(serial_number='3')
    assert [cert.serial_number for cert in dup_certs[cert_3]] == ['1', '2']

    cert_7 = Certificate.objects.get(serial_number='7')
    assert [cert.serial_number for cert in dup_certs[cert_7]] == ['5']
