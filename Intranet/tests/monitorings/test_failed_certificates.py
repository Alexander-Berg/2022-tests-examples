# coding: utf-8

from __future__ import absolute_import, unicode_literals
import pytest

from django.urls import reverse
from django.utils import timezone
from django.utils.encoding import force_bytes
from rest_framework import status

from intranet.crt.core.models import ApproveRequest, Certificate
from intranet.crt.constants import CA_NAME, CERT_TYPE, CERT_STATUS

from __tests__.utils.common import create_certificate

pytestmark = pytest.mark.django_db


def get_failed_certificates_response(crt_client):
    return crt_client.json.get(reverse('failed-certificates'))


def test_failed_certificates_ok(crt_client):
    response = get_failed_certificates_response(crt_client)
    assert response.status_code == status.HTTP_200_OK


def test_failed_certificates(crt_client, crt_robot, certificate_types):
    def create_test_certificate(status, exclude_from_monitoring=False):
        return create_certificate(
            crt_robot,
            status=status,
            type=certificate_types[CERT_TYPE.PC],
            ca_name=CA_NAME.CERTUM_TEST_CA,
            exclude_from_monitoring=exclude_from_monitoring,
        )

    cert1 = create_test_certificate(status=CERT_STATUS.ISSUED)
    cert2 = create_test_certificate(status=CERT_STATUS.ERROR, exclude_from_monitoring=True)
    cert3 = create_test_certificate(status=CERT_STATUS.ERROR)

    cert4 = create_test_certificate(status=CERT_STATUS.VALIDATION)
    a = ApproveRequest.objects.create(certificate=cert4)
    ApproveRequest.objects.filter(pk=a.pk).update(update_date=timezone.now() - timezone.timedelta(days=1))

    cert5 = create_test_certificate(status=CERT_STATUS.VALIDATION)
    ApproveRequest.objects.create(certificate=cert5)

    response = get_failed_certificates_response(crt_client)
    assert response.status_code == status.HTTP_412_PRECONDITION_FAILED
    assert response.content == force_bytes(f'Failed certificates: {cert3.pk} | Not validated certificates: {cert4.pk}')

    Certificate.objects.filter(pk__in=(cert3.pk, cert4.pk)).update(exclude_from_monitoring=True)
    response = get_failed_certificates_response(crt_client)
    assert response.status_code == status.HTTP_200_OK
