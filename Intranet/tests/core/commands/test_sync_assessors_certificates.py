import mock
import pytest

from django.core.management import call_command
from django.core import mail
from django.test import override_settings

from intranet.crt.constants import CERT_STATUS
from intranet.crt.core.models import AssessorCertificate
from __tests__.utils.common import (
    create_user,
    create_group,
    create_permission,
)
from __tests__.core.test_assessor_certs import create_cert

pytestmark = pytest.mark.django_db


def test_sync(crt_robot):
    having_assessor_cert_perm = create_permission('can_have_assessor_certificate',
                                                  'core',
                                                  'certificate')
    assessor_group = create_group('assessors', permissions=[having_assessor_cert_perm])

    assessor1 = create_user('assessor1')
    assessor2 = create_user('assessor2')
    assessor3 = create_user('assessor3', groups=[assessor_group])
    assessor4 = create_user('assessor4', groups=[assessor_group])

    create_cert(assessor1)
    create_cert(assessor3)

    def local_revoke(self):
        return self.revoke()

    with override_settings(INTERNAL_CA='TestCA'):
        with mock.patch('intranet.crt.core.ca.test.TestCA.revoke') as revoke_mocker:
            call_command('sync_assessor_certs')
            assert len(mail.outbox) == 1
            assert revoke_mocker.call_count == 0

    assert AssessorCertificate.objects.count() == 3
    assert set(AssessorCertificate.objects
               .filter(revoke_at=None)
               .values_list('user__username', flat=True)) == {'assessor3', 'assessor4'}
    assert set(AssessorCertificate.objects
               .exclude(revoke_at=None)
               .values_list('user__username', flat=True)) == {'assessor1'}
