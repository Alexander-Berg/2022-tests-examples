import mock
import pytest
from freezegun import freeze_time

from django.conf import settings
from django.contrib.auth.models import Group
from django.core import mail
from django.test import override_settings
from django.utils import timezone
from django_idm_api.signals import role_added, role_removed

from intranet.crt.core.models import Certificate, CertificateType
from intranet.crt.constants import CERT_TYPE, CERT_STATUS, CA_NAME
from __tests__.utils.common import (
    create_user,
    create_group,
    create_permission,
)

pytestmark = pytest.mark.django_db


def create_cert(assessor, status=CERT_STATUS.ISSUED):
    certificate = Certificate(
        type=CertificateType.objects.get(name=CERT_TYPE.ASSESSOR),
        status=status,
        ca_name=CA_NAME.TEST_CA,
        common_name='test',
        email=assessor.email,
        user=assessor,
        requester=assessor,
        request='test',
        certificate='testcerttext',
        end_date=timezone.now() + timezone.timedelta(2*settings.CRT_ASSESSOR_EXPIRING_DAYS),
        requested_by_csr=True,
    )
    certificate.save()
    return certificate


def test_add_irrelevant_role(crt_robot):
    having_assessor_cert_perm = create_permission('can_have_assessor_certificate', 'core', 'certificate')
    assessor_group = create_group('assessors', permissions=[having_assessor_cert_perm])
    assessor = create_user('assessor')

    assert assessor.certificates.count() == 0

    with override_settings(INTERNAL_CA=CA_NAME.TEST_CA):
        role_added.send(None, login='assessor', group=None, role={'role': 'group-0'}, fields=None)
        assert len(mail.outbox) == 0

    assert assessor.certificates.count() == 0


def test_issue_cert(crt_robot):
    having_assessor_cert_perm = create_permission('can_have_assessor_certificate', 'core', 'certificate')
    assessor_group = create_group('assessors', permissions=[having_assessor_cert_perm])
    assessor = create_user('assessor')

    assert assessor.certificates.count() == 0

    group_id = Group.objects.get(name='assessors').pk

    with override_settings(INTERNAL_CA=CA_NAME.TEST_CA):
        role_added.send(None,
                        login='assessor',
                        group=None,
                        role={'role': 'group-%d' % group_id},
                        fields=None,)
        assert len(mail.outbox) == 1

    assert assessor.certificates.count() == 1
    cert = assessor.certificates.get()
    assert cert.type.name == CERT_TYPE.ASSESSOR
    assert cert.status == CERT_STATUS.ISSUED


def test_add_role_with_expiring_cert(crt_robot):
    having_assessor_cert_perm = create_permission('can_have_assessor_certificate', 'core', 'certificate')
    assessor_group = create_group('assessors', permissions=[having_assessor_cert_perm])
    assessor = create_user('assessor', groups=[assessor_group])

    cert = create_cert(assessor)
    assert assessor.certificates.count() == 1
    cert = assessor.certificates.get()
    assert cert.type.name == CERT_TYPE.ASSESSOR
    assert cert.status == CERT_STATUS.ISSUED

    now = timezone.now()
    cert.end_date = now + timezone.timedelta(days=settings.CRT_OLD_CERT_EXPIRATION_DAYS + 2)
    cert.save()

    group_id = Group.objects.get(name='assessors').pk

    with override_settings(INTERNAL_CA=CA_NAME.TEST_CA):
        with freeze_time(now):
            role_added.send(None,
                            login='assessor',
                            group=None,
                            role={'role': 'group-%d' % group_id},
                            fields=None,)
            assert len(mail.outbox) == 1

    assert assessor.certificates.count() == 2
    certs = assessor.certificates.order_by('pk')
    assert certs[0].type.name == CERT_TYPE.ASSESSOR
    assert certs[0].status == CERT_STATUS.ISSUED
    assert certs[0].end_date == now + timezone.timedelta(days=settings.CRT_OLD_CERT_EXPIRATION_DAYS)
    assert certs[1].type.name == CERT_TYPE.ASSESSOR
    assert certs[1].status == CERT_STATUS.ISSUED


@pytest.mark.parametrize('cert_status', [CERT_STATUS.ISSUED, CERT_STATUS.REQUESTED])
@pytest.mark.parametrize('expiration_days',
                         [(settings.CRT_ASSESSOR_EXPIRING_DAYS+1,),
                          (settings.CRT_ASSESSOR_EXPIRING_DAYS+1, settings.CRT_ASSESSOR_EXPIRING_DAYS+2),
                          (1, 1)])
def test_add_role_with_active_certs(crt_robot, expiration_days, cert_status):
    having_assessor_cert_perm = create_permission('can_have_assessor_certificate', 'core', 'certificate')
    assessor_group = create_group('assessors', permissions=[having_assessor_cert_perm])
    assessor = create_user('assessor', groups=[assessor_group])

    now = timezone.now()
    for day in expiration_days:
        end = now + timezone.timedelta(day)
        cert = create_cert(assessor, cert_status)
        cert.end_date = end
        cert.save()

    assert assessor.certificates.count() == len(expiration_days)

    group_id = Group.objects.get(name='assessors').pk

    with override_settings(INTERNAL_CA=CA_NAME.TEST_CA):
        role_added.send(None,
                        login='assessor',
                        group=None,
                        role={'role': 'group-%d' % group_id},
                        fields=None, )
        assert len(mail.outbox) == 0

    assert assessor.certificates.count() == len(expiration_days)


def test_revoke_cert(crt_robot):
    having_assessor_cert_perm = create_permission('can_have_assessor_certificate', 'core', 'certificate')
    assessor_group = create_group('assessors', permissions=[having_assessor_cert_perm])
    assessor = create_user('assessor', groups=[assessor_group])

    create_cert(assessor)
    assert assessor.certificates.count() == 1
    cert = assessor.certificates.get()
    assert cert.type.name == CERT_TYPE.ASSESSOR
    assert cert.status == CERT_STATUS.ISSUED

    group_id = Group.objects.get(name='assessors').pk

    with override_settings(INTERNAL_CA=CA_NAME.TEST_CA):
        role_removed.send(
            None,
            login='assessor',
            group=None,
            role={'role': 'group-%d' % group_id},
            data=None,
            is_fired=False,
            is_deleted=False,
        )

    assert assessor.certificates.count() == 1
    cert = assessor.certificates.get()
    assert cert.type.name == CERT_TYPE.ASSESSOR
    assert cert.status == CERT_STATUS.ISSUED
    assert cert.revoke_at is not None


def test_remove_irrelevant_role(crt_robot):
    having_assessor_cert_perm = create_permission('can_have_assessor_certificate', 'core', 'certificate')
    assessor_group = create_group('assessors', permissions=[having_assessor_cert_perm])
    assessor = create_user('assessor', groups=[assessor_group])

    create_cert(assessor)
    assert assessor.certificates.count() == 1
    cert = assessor.certificates.get()
    assert cert.type.name == CERT_TYPE.ASSESSOR
    assert cert.status == CERT_STATUS.ISSUED

    role_removed.send(
        None,
        login='assessor',
        group=None,
        role={'role': 'group-0'},
        data=None,
        is_fired=False,
        is_deleted=False,
    )

    assert assessor.certificates.count() == 1
    cert = assessor.certificates.get()
    assert cert.type.name == CERT_TYPE.ASSESSOR
    assert cert.status == CERT_STATUS.ISSUED
    assert cert.revoke_at is None
