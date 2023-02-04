import pytest
from django.template.loader import render_to_string
from django.conf import settings

from intranet.crt.constants import CERT_TYPE
from intranet.crt.core.models import CertificateType, Host
from __tests__.utils.common import create_certificate
from intranet.crt.users.models import CrtUser

pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('cert_type', [CERT_TYPE.HOST, CERT_TYPE.CLIENT_SERVER])
def test_template_need_aprrove(users, cert_type):
    certificate = get_certificate_of_type(cert_type)
    if cert_type == CERT_TYPE.HOST:
        Host.objects.create(hostname='host1')
        Host.objects.create(hostname='host2')
    hosts = Host.objects.all()
    description = render_template(hosts, certificate)
    if cert_type == CERT_TYPE.HOST:
        assert 'host-сертификата' in description
        assert 'Для хостов:' in description
        assert 'host1' in description
        assert 'host2' in description
    else:
        assert 'client-server-сертификата' in description
        assert 'Для abcd' in description


def get_certificate_of_type(type_name):
    certificate_type = CertificateType.objects.get(name=type_name)
    user = CrtUser.objects.all()[0]
    return create_certificate(user, certificate_type, common_name='abcd')


def render_template(hosts, certificate):
    return render_to_string(
        'host-certificate-need-approve.txt',
        {
            'certificate': certificate,
            'hosts': hosts,
            'crt_api_base_url': settings.CRT_API_URL,
        }
    )
