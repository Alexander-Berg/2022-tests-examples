import mock
import pytest

from rest_framework import status
from freezegun import freeze_time

from django.utils import timezone
from django.conf import settings

from intranet.crt.constants import CERT_STATUS
from intranet.crt.core.models import Certificate

pytestmark = pytest.mark.django_db


def test_request_view(crt_client, users):
    user = users['normal_user']

    crt_client.login(user.username)

    # Запросим истекающий сертификат
    with freeze_time(timezone.now() - timezone.timedelta(days=365-30)):
        with mock.patch('intranet.crt.core.views.old_get_inums_and_models') as moked:
            moked.return_value = [('ABC', 'ABC')]
            response = crt_client.post('/request/', data={'device': 'ABC', 'password': 'abc'})

    assert response.status_code == status.HTTP_302_FOUND

    cert = Certificate.objects.get()
    assert cert.status == CERT_STATUS.ISSUED
    assert cert.revoke_at is None

    # Перезапросим истекающий сертификат
    with freeze_time(timezone.now()):
        with mock.patch('intranet.crt.core.views.old_get_inums_and_models') as moked:
            moked.return_value = [('ABC', 'ABC')]
            crt_client.post('/request/', data={'device': 'ABC', 'password': 'abc'})

        cert.refresh_from_db()
        assert cert.revoke_at == timezone.now() + timezone.timedelta(
            hours=settings.CRT_LINUXPC_HOLD_ON_REISSUE_AFTER_HOURS
        )
