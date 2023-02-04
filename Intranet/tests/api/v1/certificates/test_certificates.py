import mock
import pytest

from django.http import HttpRequest
from django_abc_data.models import AbcService
from django.utils import timezone

from intranet.crt.tags.models import CertificateTagRelation
from intranet.crt.core.ca.exceptions import CaError, Ca429Error
from intranet.crt.constants import CERT_TYPE, ABC_CERTIFICATE_MANAGER_SCOPE, CA_NAME
from __tests__.utils.common import create_certificate, create_certificate_tag

pytestmark = pytest.mark.django_db


def test_queries_in_certificate_list(crt_client, users, certificate_types, django_assert_num_queries):
    normal_user = users['normal_user']
    crt_client.login(normal_user.username)

    tag1 = create_certificate_tag('tag1')
    tag2 = create_certificate_tag('tag2')
    tag3 = create_certificate_tag('tag3')

    cert1 = create_certificate(normal_user, certificate_types[CERT_TYPE.PC])

    crt_client.json.get('/api/certificate/')  # для подцепления кеша пермишнов

    EXPECTED_QUERIES = 9

    with django_assert_num_queries(EXPECTED_QUERIES):
        response = crt_client.json.get('/api/certificate/')
        assert response.status_code == 200

    cert2 = create_certificate(normal_user, certificate_types[CERT_TYPE.NINJA])

    with django_assert_num_queries(EXPECTED_QUERIES):
        response = crt_client.json.get('/api/certificate/')
        assert response.status_code == 200

    CertificateTagRelation.objects.create(certificate=cert1, tag=tag1, source='manual')

    with django_assert_num_queries(EXPECTED_QUERIES):
        response = crt_client.json.get('/api/certificate/')
        assert response.status_code == 200

    CertificateTagRelation.objects.create(certificate=cert1, tag=tag2, source='manual')

    with django_assert_num_queries(EXPECTED_QUERIES):
        response = crt_client.json.get('/api/certificate/')
        assert response.status_code == 200

    CertificateTagRelation.objects.create(certificate=cert2, tag=tag3, source='manual')

    with django_assert_num_queries(EXPECTED_QUERIES):
        response = crt_client.json.get('/api/certificate/')
        assert response.status_code == 200


@pytest.mark.parametrize(
    'ca_error,status',
    (
        (Ca429Error('too_many_requests', '10'), 429),
        (CaError('random_err'), 500),
    )
)
def test_revoke_ca_error(crt_client, users, certificate_types, ca_error, status):
    requester = users['another_user']
    service = AbcService.objects.create(external_id=100, created_at=timezone.now(), modified_at=timezone.now())
    requester.staff_groups.create(
        abc_service=service,
        external_id=200,
        is_deleted=False,
        url='777',
        role_scope=ABC_CERTIFICATE_MANAGER_SCOPE,
    )

    requester = users['normal_user']
    cert = create_certificate(
        requester=requester,
        user=requester,
        type=certificate_types[CERT_TYPE.HOST],
        ca_name=CA_NAME.INTERNAL_TEST_CA, abc_service=service
    )
    request = HttpRequest()
    request.user = requester
    request.method = 'DELETE'
    crt_client.login(requester.username)

    with mock.patch('intranet.crt.core.ca.internal.InternalCA.raise_non_200', mock.Mock(side_effect=ca_error)):
        with mock.patch('intranet.crt.core.ca.internal.InternalCaSession.request'):
            response = crt_client.json.delete('/api/certificate/{}/'.format(cert.pk))

    assert response.status_code == status
    if isinstance(ca_error, Ca429Error):
        assert response['Retry-After'] == ca_error.retry_after
