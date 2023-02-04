import pytest

from django.conf import settings
from django.core.management import call_command
from django.utils import timezone

from intranet.crt.core.models import PrivateKey, Certificate
from intranet.crt.constants import CERT_TYPE, TASK_TYPE, ACTION_TYPE
from __tests__.utils.common import create_certificate
from __tests__.utils.ssl import generate_private_key

pytestmark = pytest.mark.django_db


def test_remove_private_keys_from_yav(users, certificate_types):
    cert1 = create_certificate(
        users['normal_user'],
        certificate_types[CERT_TYPE.HOST],
        uploaded_to_yav=False,
        private_key=PrivateKey.objects.create(data=generate_private_key()),
    )
    cert2 = create_certificate(
        users['normal_user'],
        certificate_types[CERT_TYPE.HOST],
        uploaded_to_yav=True,
        private_key=PrivateKey.objects.create(data=generate_private_key()),
    )

    old_time = timezone.now() - timezone.timedelta(settings.CRT_DAYS_TO_REMOVE_PRIVATE_KEYS + 1)
    Certificate.objects.filter(pk=cert2.pk).update(issued=old_time)

    call_command(TASK_TYPE.REMOVE_PRIVATE_KEYS)

    cert1.refresh_from_db()
    cert2.refresh_from_db()

    assert cert1.private_key_id
    assert not cert2.private_key_id
    assert cert1.actions.filter(type=ACTION_TYPE.CERT_PRIVATE_KEY_DELETED).count() == 0
    assert cert2.actions.filter(type=ACTION_TYPE.CERT_PRIVATE_KEY_DELETED).count() == 1
