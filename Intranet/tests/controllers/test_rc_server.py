import pytest

from intranet.crt.constants import CERT_TYPE, CERT_STATUS
from intranet.crt.core.controllers.certificates import RcServerCertificateController
from intranet.crt.core.models import Certificate
from __tests__.utils.common import create_certificate

pytestmark = pytest.mark.django_db


@pytest.fixture
def certificates(users, crt_robot, certificate_types):
    type_rc_server = certificate_types[CERT_TYPE.RC_SERVER]

    create_certificate(crt_robot, type_rc_server, common_name='s1', serial_number='1')
    create_certificate(users['normal_user'], type_rc_server, common_name='s1', serial_number='2')
    create_certificate(crt_robot, type_rc_server, common_name='s1', serial_number='3')
    create_certificate(crt_robot, type_rc_server, common_name='s1', status=CERT_STATUS.REVOKED, serial_number='4')

    create_certificate(crt_robot, type_rc_server, common_name='s2', serial_number='5')
    create_certificate(crt_robot, type_rc_server, common_name='s2', status=CERT_STATUS.ERROR, serial_number='6')
    create_certificate(crt_robot, type_rc_server, common_name='s2', serial_number='7')
    create_certificate(crt_robot, certificate_types[CERT_TYPE.PC], common_name='s2')

    create_certificate(crt_robot, type_rc_server, common_name='s3')

    create_certificate(crt_robot, type_rc_server, common_name='s4', status=CERT_STATUS.REVOKED)
    create_certificate(crt_robot, type_rc_server, common_name='s5', status=CERT_STATUS.REVOKED)


def test_duplicate_certificates(certificates):
    dup_certs = RcServerCertificateController.duplicate_certificates()

    assert {cert.serial_number for cert in list(dup_certs.keys())} == {'3', '7'}

    cert_3 = Certificate.objects.get(serial_number='3')
    assert [cert.serial_number for cert in dup_certs[cert_3]] == ['1', '2']

    cert_7 = Certificate.objects.get(serial_number='7')
    assert [cert.serial_number for cert in dup_certs[cert_7]] == ['5']
