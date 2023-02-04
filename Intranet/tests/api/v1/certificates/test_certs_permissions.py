import mock
import pytest

from django.contrib.auth.models import Permission
from django.http import HttpRequest
from django_abc_data.models import AbcService
from django.utils import timezone

from intranet.crt.api.v1.certificates.permissions import CertificateActionsPermission, IsOwner
from intranet.crt.constants import CERT_TYPE, CERT_DETAIL_ACTION, ABC_CERTIFICATE_MANAGER_SCOPE
from __tests__.utils.common import create_certificate, create_certificate_type
from intranet.crt.users.models import CrtUser


pytestmark = pytest.mark.django_db


def check_permissions(method, cert, user, result, action=None, post=None):
    request = HttpRequest()
    request.user = user
    request.method = method
    if request.method == 'POST':
        request.data = {
            'action': action,
        }
        if post:
            request.data.update(post)

    assert CertificateActionsPermission().has_object_permission(request, None, cert) == result


def test_hold_unhold_revoke_update_by_owner(users, certificate_types):
    user = users['normal_user']

    cert = create_certificate(user, certificate_types[CERT_TYPE.HOST])

    for action in (CERT_DETAIL_ACTION.HOLD, CERT_DETAIL_ACTION.UNHOLD):
        check_permissions('POST', cert, user, False, action=action)

    for post in [{}, {'abc_service': 1}]:
        check_permissions('POST', cert, user, True, action=CERT_DETAIL_ACTION.UPDATE, post=post)

    for post in [{'manual_tags': [1, 2, 3]}, {'abc_service': 1, 'manual_tags': [1, 2, 3]}]:
        # Владельцы не могут обновлять теги на сертификате
        check_permissions('POST', cert, user, False, action=CERT_DETAIL_ACTION.UPDATE, post=post)

    check_permissions('DELETE', cert, user, True)


def test_hold_unhold_revoke_by_permission_bearer(users, certificate_types):
    user = users['revoke_all_user']  # has can_revoke_any_certificate permission
    requester = users['normal_user']

    cert = create_certificate(requester, certificate_types[CERT_TYPE.HOST])

    for action in (CERT_DETAIL_ACTION.HOLD, CERT_DETAIL_ACTION.UNHOLD):
        check_permissions('POST', cert, user, True, action=action)

    check_permissions('DELETE', cert, user, True)


def test_update_by_permission_bearer(users, certificate_types):
    user = users['helpdesk_user']
    requester = users['normal_user']

    cert = create_certificate(requester, certificate_types[CERT_TYPE.HOST])

    service_perm = Permission.objects.get(codename='can_change_abc_service_for_any_certificate')
    user.user_permissions.add(service_perm)

    # Обновлять сервис можно, а теги нет
    for post in [{}, {'abc_service': 1}]:
        check_permissions('POST', cert, user, True, action=CERT_DETAIL_ACTION.UPDATE, post=post)
    for post in [{'manual_tags': [1, 2, 3]}, {'abc_service': 1, 'manual_tags': [1, 2, 3]}]:
        check_permissions('POST', cert, user, False, action=CERT_DETAIL_ACTION.UPDATE, post=post)

    # Свапнем пермишны
    tags_perm = Permission.objects.get(codename='can_edit_certificate_tags')
    user.user_permissions.remove(service_perm)
    user.user_permissions.add(tags_perm)
    user = CrtUser.objects.get(pk=user.pk)  # сбрасываем кеш пермишнов

    # Теперь наоборот – теги можно, а сервис нельзя
    for post in [{}, {'manual_tags': [1, 2, 3]}]:
        check_permissions('POST', cert, user, True, action=CERT_DETAIL_ACTION.UPDATE, post=post)
    for post in [{'abc_service': 1}, {'abc_service': 1, 'manual_tags': [1, 2, 3]}]:
        check_permissions('POST', cert, user, False, action=CERT_DETAIL_ACTION.UPDATE, post=post)

@pytest.mark.parametrize('give_update_perm', [False, True])
def test_hold_unhold_revoke_without_permissions(users, give_update_perm, certificate_types):
    user = users['normal_user']
    requester = users['another_user']

    if give_update_perm:
        perm = Permission.objects.get(codename='can_change_abc_service_for_any_certificate')
        user.user_permissions.add(perm)

    cert = create_certificate(requester, certificate_types[CERT_TYPE.HOST])

    for action in (CERT_DETAIL_ACTION.HOLD, CERT_DETAIL_ACTION.UNHOLD):
        check_permissions('POST', cert, user, False, action)

    check_permissions('DELETE', cert, user, False)


@pytest.mark.parametrize('give_revoke_perm', [False, True])
def test_update_without_permissions(users, give_revoke_perm, certificate_types):
    user = users['normal_user']
    requester = users['another_user']

    if give_revoke_perm:
        perm = Permission.objects.get(codename='can_revoke_any_certificate')
        user.user_permissions.add(perm)

    cert = create_certificate(requester, certificate_types[CERT_TYPE.HOST])

    # Что-либо действительно менять прав нет
    for post in [{'manual_tags': [1, 2, 3]}, {'abc_service': 1}, {'abc_service': 1, 'manual_tags': [1, 2, 3]}]:
        check_permissions('POST', cert, user, False, CERT_DETAIL_ACTION.UPDATE, post=post)

    # Но при этом сделать update, который не меняет ничего, можно
    check_permissions('POST', cert, user, True, CERT_DETAIL_ACTION.UPDATE, post=None)


@pytest.mark.parametrize('type_', CERT_TYPE.active_types())
def test_revoke_cert_by_owner_of_the_cert(users, type_, certificate_types):
    user = users['normal_user']
    requester = users['another_user']
    type_ = certificate_types[type_]
    cert = create_certificate(requester=requester, user=user, type=type_)
    request = HttpRequest()
    request.user = user
    request.method = 'GET'

    assert IsOwner().has_object_permission(request, None, cert) is True


@pytest.mark.parametrize('type_', CERT_TYPE.active_types())
def test_revoke_cert_by_user_from_group(users, type_, certificate_types):
    user = users['normal_user']
    requester = users['another_user']
    service = AbcService.objects.create(external_id=100, created_at=timezone.now(), modified_at=timezone.now())
    type_ = certificate_types[type_]
    cert = create_certificate(requester=requester, user=requester, type=type_, abc_service=service)
    user.staff_groups.create(
        abc_service=service,
        external_id=200,
        is_deleted=False,
        url='777',
        role_scope=ABC_CERTIFICATE_MANAGER_SCOPE
    )
    check_permissions('DELETE', cert, user, True)


@pytest.mark.parametrize('type_', CERT_TYPE.active_types())
def test_revoke_cert_by_user_not_from_group(users, type_, certificate_types):
    user = users['normal_user']
    requester = users['another_user']
    service = AbcService.objects.create(external_id=100, created_at=timezone.now(), modified_at=timezone.now())
    type_ = certificate_types[type_]
    cert = create_certificate(requester=requester, user=requester, type=type_, abc_service=service)
    check_permissions('DELETE', cert, user, False)


def test_can_revoke_users_certificates_permission(users):
    normal_user = users['normal_user']
    helpdesk_user = users['helpdesk_user']
    cert_types = [create_certificate_type(cert_type, True) for cert_type in CERT_TYPE.ALL_TYPES]
    certs = [create_certificate(normal_user, cert_type) for cert_type in cert_types]
    for cert in certs:
        for action in (CERT_DETAIL_ACTION.HOLD, CERT_DETAIL_ACTION.UNHOLD, CERT_DETAIL_ACTION.REVOKE):
            check_permissions('POST', cert, helpdesk_user, cert.type.name in CERT_TYPE.USERS_TYPES, action)
