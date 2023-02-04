import mock
import pytest

from django.contrib.auth.models import Permission
from django.http import HttpRequest

from intranet.crt.api.v2.certificates.permissions import (CanViewCertificate,
                                                 CanRevokeCertificate,
                                                 CanUpdateCertificateData,
                                                 CanDownloadCertificate,
                                                 )
from intranet.crt.constants import CERT_TYPE, CERT_STATUS, CA_NAME
from intranet.crt.core.models import Certificate, CertificateType
from __tests__.utils.common import create_certificate, create_certificate_type
from intranet.crt.users.models import CrtUser

pytestmark = pytest.mark.django_db


def create_cert(requester, cert_type=CERT_TYPE.HOST):
    return Certificate.objects.create(
        user=requester,
        requester=requester,
        type=CertificateType.objects.get(name=cert_type),
        status=CERT_STATUS.ISSUED,
        ca_name=CA_NAME.INTERNAL_TEST_CA,
        requested_by_csr=False

    )


def check_permissions(method, cert, user, has=(), does_not_have=(), post=None):
    request = HttpRequest()
    request.user = user
    request.method = method
    if post:
        request.POST = post

    for perm in has:
        assert perm().has_object_permission(request, None, cert)
    for perm in does_not_have:
        assert not perm().has_object_permission(request, None, cert)


def test_permissions_by_owner(users):
    user = users['normal_user']

    cert = create_cert(user)

    for method in ('GET', 'POST', 'PATCH'):
        check_permissions(
            method, cert, user,
            has=[CanViewCertificate, CanDownloadCertificate, CanRevokeCertificate, CanUpdateCertificateData],
            post={'abc_service': 1},
        )

    # При редактировании тегов GET отрабатывает, потому что он всегда отрабатывает
    for post in [{'manual_tags': [1, 2, 3]}, {'abc_service': 1, 'manual_tags': [1, 2, 3]}]:
        check_permissions(
            'GET', cert, user,
            has=[CanUpdateCertificateData],
            post=post,
        )

    # Но POST и PATCH не работают, так как редактировать теги (кроме админов) даже владельцы сертификатов не могут
    for method in ('POST', 'PATCH'):
        for post in [{'manual_tags': [1, 2, 3]}, {'abc_service': 1, 'manual_tags': [1, 2, 3]}]:
            check_permissions(
                method, cert, user,
                does_not_have=[CanUpdateCertificateData],
                post=post,
            )


def test_viewer_permission(users):
    user = users['normal_user']
    perm = Permission.objects.get(codename='can_view_any_certificate')
    user.user_permissions.add(perm)

    requester = users['helpdesk_user']

    cert = create_cert(requester)

    check_permissions(
        'GET', cert, user,
        has=[CanViewCertificate, CanUpdateCertificateData],
        does_not_have=[CanDownloadCertificate, CanRevokeCertificate],
        post={'abc_service': 1},
    )
    for method in ('POST', 'PATCH'):
        check_permissions(
            method, cert, user,
            has=[CanViewCertificate],
            does_not_have=[CanDownloadCertificate, CanRevokeCertificate, CanUpdateCertificateData],
            post={'abc_service': 1},
        )


def test_download_permission(users):
    user = users['normal_user']
    perm = Permission.objects.get(codename='can_download_any_certificate')
    user.user_permissions.add(perm)

    requester = users['helpdesk_user']

    cert = create_cert(requester)

    check_permissions(
        'GET', cert, user,
        has=[CanDownloadCertificate, CanUpdateCertificateData],
        does_not_have=[CanViewCertificate, CanRevokeCertificate],
        post={'abc_service': 1},
    )
    for method in ('POST', 'PATCH'):
        check_permissions(
            method, cert, user,
            has=[CanDownloadCertificate],
            does_not_have=[CanViewCertificate, CanRevokeCertificate, CanUpdateCertificateData],
            post={'abc_service': 1},
        )


def test_hold_unhold_revoke_by_permission_bearer(users):
    user = users['helpdesk_user']  # has can_revoke_users_certificates permission
    requester = users['normal_user']

    cert = create_cert(requester, cert_type=CERT_TYPE.LINUX_PC)

    check_permissions(
        'GET', cert, user,
        has=[CanRevokeCertificate, CanUpdateCertificateData],
        does_not_have=[CanViewCertificate, CanDownloadCertificate],
    )
    for method in ('POST', 'PATCH'):
        check_permissions(
            method, cert, user,
            has=[CanRevokeCertificate],
            does_not_have=[CanViewCertificate, CanDownloadCertificate, CanUpdateCertificateData],
            post={'abc_service': 1},
        )


def test_update_service_by_permission_bearer(users):
    requester = users['helpdesk_user']
    user = users['normal_user']

    perm = Permission.objects.get(codename='can_change_abc_service_for_any_certificate')
    user.user_permissions.add(perm)

    cert = create_cert(requester)

    for method in ('GET', 'POST', 'PATCH'):
        check_permissions(
            method, cert, user,
            has=[CanUpdateCertificateData],
            does_not_have=[CanViewCertificate, CanRevokeCertificate, CanDownloadCertificate],
            post={'abc_service': 1},
        )


def test_update_different_values(users):
    requester = users['helpdesk_user']
    user = users['normal_user']

    service_perm = Permission.objects.get(codename='can_change_abc_service_for_any_certificate')
    user.user_permissions.add(service_perm)

    cert = create_cert(requester)

    for post in [{}, {'abc_service': 1}, {'manual_tags': [1, 2, 3]}, {'abc_service': 1, 'manual_tags': [1, 2, 3]}]:
        # GET-запрос отрабатывает вообще при любых полях формы
        check_permissions(
            'GET', cert, user,
            has=[CanUpdateCertificateData],
            does_not_have=[CanViewCertificate, CanRevokeCertificate, CanDownloadCertificate],
            post=post,
        )

    for method in ('POST', 'PATCH'):
        # На изменение abc-сервиса есть пермишн
        for post in [{}, {'abc_service': 1}]:
            check_permissions(
                method, cert, user,
                has=[CanUpdateCertificateData],
                does_not_have=[],
                post=post,
            )
        # А на изменение тегов – нет
        for post in [{'manual_tags': [1, 2, 3]}, {'abc_service': 1, 'manual_tags': [1, 2, 3]}]:
            check_permissions(
                method, cert, user,
                has=[],
                does_not_have=[CanUpdateCertificateData],
                post=post,
            )

    # Старый пермишн отзовёв, а на теги – выдадим
    user.user_permissions.remove(service_perm)
    tag_perm = Permission.objects.get(codename='can_edit_certificate_tags')
    user.user_permissions.add(tag_perm)
    user = CrtUser.objects.get(pk=user.pk)  # сбрасываем кеш пермишнов

    for method in ('POST', 'PATCH'):
        for post in [{}, {'manual_tags': [1, 2, 3]}]:
            check_permissions(
                method, cert, user,
                has=[CanUpdateCertificateData],
                does_not_have=[],
                post=post,
            )
        for post in [{'abc_service': 1}, {'abc_service': 1, 'manual_tags': [1, 2, 3]}]:
            check_permissions(
                method, cert, user,
                has=[],
                does_not_have=[CanUpdateCertificateData],
                post=post,
            )


def test_all_permissions(users):
    user = users['revoke_all_user']  # has can_revoke_any_certificate permission
    perm = Permission.objects.get(codename='can_change_abc_service_for_any_certificate')
    user.user_permissions.add(perm)
    perm = Permission.objects.get(codename='can_view_any_certificate')
    user.user_permissions.add(perm)
    perm = Permission.objects.get(codename='can_download_any_certificate')
    user.user_permissions.add(perm)
    perm = Permission.objects.get(codename='can_edit_certificate_tags')
    user.user_permissions.add(perm)

    requester = users['normal_user']

    cert = create_cert(requester)

    for method in ('GET', 'POST', 'PATCH'):
        for post in [{}, {'abc_service': 1}, {'manual_tags': [1, 2, 3]}, {'abc_service': 1, 'manual_tags': [1, 2, 3]}]:
            check_permissions(
                method, cert, user,
                has=[CanViewCertificate, CanDownloadCertificate, CanRevokeCertificate, CanUpdateCertificateData],
                post=post,
            )

    user.user_permissions.all().delete()
    user.is_superuser = True
    user.save()
    for method in ('GET', 'POST', 'PATCH'):
        for post in [{}, {'abc_service': 1}, {'manual_tags': [1, 2, 3]}, {'abc_service': 1, 'manual_tags': [1, 2, 3]}]:
            check_permissions(
                method, cert, user,
                has=[CanViewCertificate, CanDownloadCertificate, CanRevokeCertificate, CanUpdateCertificateData],
                post=post,
            )


def test_can_revoke_users_certificates_permission(users):
    normal_user = users['normal_user']
    helpdesk_user = users['helpdesk_user']
    cert_types = [create_certificate_type(cert_type, True) for cert_type in CERT_TYPE.ALL_TYPES]
    certs = [create_certificate(normal_user, cert_type) for cert_type in cert_types]
    check_perm_kwargs = {
        'method': 'POST',
        'user': helpdesk_user,
        'has': set(),
        'does_not_have': set()
    }
    for cert in certs:
        check_perm_kwargs['cert'] = cert
        if cert.type.name in CERT_TYPE.USERS_TYPES:
            check_perm_kwargs['has'].add(CanRevokeCertificate)
            check_perm_kwargs['does_not_have'].clear()
        else:
            check_perm_kwargs['has'].clear()
            check_perm_kwargs['does_not_have'].add(CanRevokeCertificate)
        check_permissions(**check_perm_kwargs)
