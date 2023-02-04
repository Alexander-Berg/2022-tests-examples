import pytest

from django.contrib.auth.models import Permission

from intranet.crt.constants import CA_NAME, CERT_TYPE
from intranet.crt.core.models import Certificate, Host
from intranet.crt.utils.ssl import PemCertificate


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_sdc_certificate_contains_sans(crt_client, users, path):
    helpdesk_user = users['helpdesk_user']
    permission = Permission.objects.get(codename='can_issue_sdc_certificates')
    helpdesk_user.user_permissions.add(permission)

    common_name = 'test.rover.sdc.yandex.net'
    request_data = {
        'type': CERT_TYPE.SDC,
        'ca_name': CA_NAME.TEST_CA,
        'common_name': common_name,
    }
    crt_client.login(helpdesk_user.username)
    response = crt_client.json.post(path, data=request_data)

    assert response.status_code == 201
    cert = Certificate.objects.get()

    pem_obj = PemCertificate(cert.certificate)
    assert pem_obj.common_name == common_name
    assert pem_obj.sans == [common_name]
    assert not Host.objects.exists()
