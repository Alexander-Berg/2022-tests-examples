import datetime

import pytest
from freezegun import freeze_time

from django.core.management import call_command
from django.utils import timezone

from intranet.crt.constants import CERT_TYPE, CERT_STATUS
from intranet.crt.core.models import Certificate
from intranet.crt.exceptions import CrtTimestampError
from intranet.crt.tasks.mark_expired_certs import MarkExpiredCertsTask
from intranet.crt.tasks.revoke.queued_certs import RevokeQueuedCertsTask
from __tests__.utils.common import create_certificate

pytestmark = pytest.mark.django_db

now_time = timezone.now()


@pytest.fixture
def certificates(users, crt_robot, certificate_types):
    type_pc = certificate_types[CERT_TYPE.PC]

    revoke_at = now_time - datetime.timedelta(seconds=1)
    create_certificate(crt_robot, type_pc, revoke_at=revoke_at, serial_number='1')

    revoke_at = now_time + datetime.timedelta(seconds=1)
    create_certificate(crt_robot, type_pc, revoke_at=revoke_at, serial_number='2')

    create_certificate(crt_robot, type_pc, serial_number='3')

    dismissed_user = users['dismissed-user']
    create_certificate(dismissed_user, type_pc, serial_number='4')

    end_date = now_time - datetime.timedelta(seconds=1)
    create_certificate(crt_robot, type_pc, status=CERT_STATUS.HOLD, end_date=end_date, serial_number='5')
    create_certificate(crt_robot, type_pc, status=CERT_STATUS.REVOKED, end_date=end_date, serial_number='6')
    create_certificate(crt_robot, type_pc, status=CERT_STATUS.ISSUED, end_date=end_date, serial_number='7')


def test_queued_revoke(certificates):
    with freeze_time(now_time):
        call_command('revoke_queued_certs')

    assert Certificate.objects.filter(status=CERT_STATUS.HOLD).count() == 2
    assert Certificate.objects.get(serial_number='1').status == CERT_STATUS.HOLD


def test_queued_revoke_too_mach(settings, certificates):
    settings.CRT_REVOKED_COUNT_THRESHOLD = 0

    with freeze_time(now_time):
        with pytest.raises(CrtTimestampError):
            RevokeQueuedCertsTask().run()

    with freeze_time(now_time):
        call_command('revoke_queued_certs', '--force')

    assert Certificate.objects.filter(status=CERT_STATUS.HOLD).count() == 2
    assert Certificate.objects.get(serial_number='1').status == CERT_STATUS.HOLD


def test_revoke_certs_for_dismissed_users(certificates):
    with freeze_time(now_time):
        call_command('revoke_dismissed_users_certs')

    assert Certificate.objects.get(serial_number='4').revoke_at == now_time


def test_mark_expired(certificates):
    with freeze_time(now_time):
        MarkExpiredCertsTask().run()

    expired_serials = set(
        Certificate.objects
        .filter(status=CERT_STATUS.EXPIRED)
        .values_list('serial_number', flat=True)
    )

    assert expired_serials == {'5', '6', '7'}
