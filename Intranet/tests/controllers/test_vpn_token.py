import pytest

from intranet.crt.constants import CERT_TYPE, CERT_STATUS
from intranet.crt.core.controllers.certificates import VpnTokenCertificateController
from intranet.crt.core.models import Certificate
from __tests__.utils.common import create_certificate

pytestmark = pytest.mark.django_db


@pytest.fixture
def certificates(users, crt_robot, certificate_types):
    type_vpn_token = certificate_types[CERT_TYPE.VPN_TOKEN]
    create_certificate(crt_robot, type_vpn_token, serial_number='1')
    create_certificate(crt_robot, type_vpn_token, serial_number='2')
    create_certificate(crt_robot, type_vpn_token, serial_number='3')

    normal_user = users['normal_user']
    create_certificate(normal_user, type_vpn_token, serial_number='4')
    create_certificate(normal_user, type_vpn_token, serial_number='5', status=CERT_STATUS.ERROR)
    create_certificate(normal_user, type_vpn_token, serial_number='6')

    create_certificate(users['helpdesk_user'], type_vpn_token)


def test_duplicate_certificates(certificates):
    dup_certs = VpnTokenCertificateController.duplicate_certificates()

    assert {cert.serial_number for cert in list(dup_certs.keys())} == {'3', '6'}

    cert_3 = Certificate.objects.get(serial_number='3')
    assert [cert.serial_number for cert in dup_certs[cert_3]] == ['1', '2']

    cert_6 = Certificate.objects.get(serial_number='6')
    assert [cert.serial_number for cert in dup_certs[cert_6]] == ['4']
