# coding: utf-8

import pytest
import re

from django.urls import reverse
from django.utils import timezone
from rest_framework import status

from intranet.crt.core.models import Certificate
from intranet.crt.constants import CA_NAME, CERT_TYPE, CERT_STATUS

from __tests__.utils.common import create_certificate

pytestmark = pytest.mark.django_db


def get_hung_in_requested_response(crt_client):
    return crt_client.json.get(reverse('hung-in-requested'))


def test_hung_certificates_respond_with_ok(crt_client):
    response = get_hung_in_requested_response(crt_client)
    assert response.status_code == status.HTTP_200_OK


def test_doesnt_report_sync_certs(crt_client, crt_robot, certificate_types):
    def create_sync_ca_certificate(status=CERT_STATUS.REQUESTED, **kwargs):
        cert = create_certificate(
            crt_robot,
            status=status,
            type=certificate_types[CERT_TYPE.PC],
            ca_name=CA_NAME.TEST_CA,
            **kwargs
        )
        assert not cert.is_async
        return cert

    not_monitored = [ # noqa: F841
        create_sync_ca_certificate(status=CERT_STATUS.ISSUED),
        create_sync_ca_certificate(status=CERT_STATUS.ERROR),
        create_sync_ca_certificate(status=CERT_STATUS.VALIDATION),
        create_sync_ca_certificate(),
        create_sync_ca_certificate(exclude_from_monitoring=True),
        create_sync_ca_certificate(added=timezone.now() - timezone.timedelta(hours=1)),
        create_sync_ca_certificate(
            exclude_from_monitoring=True,
            added=timezone.now() - timezone.timedelta(hours=1),
        ),
    ]

    response = get_hung_in_requested_response(crt_client)
    assert response.status_code == status.HTTP_200_OK


def test_reports_async_certs(crt_client, crt_robot, certificate_types):
    def create_async_ca_certificate(status=CERT_STATUS.REQUESTED, **kwargs):
        cert = create_certificate(
            crt_robot,
            status=status,
            type=certificate_types[CERT_TYPE.PC],
            ca_name=CA_NAME.APPROVABLE_TEST_CA,
            **kwargs
        )
        assert cert.is_async
        return cert

    not_monitored = [  # noqa: F841
        create_async_ca_certificate(status=CERT_STATUS.ISSUED),
        create_async_ca_certificate(status=CERT_STATUS.ERROR),
        create_async_ca_certificate(status=CERT_STATUS.VALIDATION),
        create_async_ca_certificate(),
        create_async_ca_certificate(exclude_from_monitoring=True),
        create_async_ca_certificate(
            added=timezone.now() - timezone.timedelta(hours=1),
            exclude_from_monitoring=True,
        ),
    ]

    cert_expired = create_async_ca_certificate(
        added=timezone.now() - timezone.timedelta(hours=1)
    )

    response = get_hung_in_requested_response(crt_client)
    assert response.status_code == status.HTTP_412_PRECONDITION_FAILED
    assert re.match(rb'Found 1 hung certificates in requested status. Some of them: \d+', response.content)

    Certificate.objects.filter(pk=cert_expired.pk).update(exclude_from_monitoring=True)
    response = get_hung_in_requested_response(crt_client)
    assert response.status_code == status.HTTP_200_OK
