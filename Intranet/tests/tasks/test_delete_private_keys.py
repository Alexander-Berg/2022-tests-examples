import pytest
from constance.test import override_config

from django.core.management import call_command
from waffle.testutils import override_switch

from intranet.crt.core.models import PrivateKey, Certificate
from intranet.crt.constants import CERT_TYPE, TASK_TYPE
from __tests__.utils.common import create_certificate
from __tests__.utils.ssl import generate_private_key

pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('delete_keys', [True, False])
def test_delete_private_keys_from_yav(users, certificate_types, delete_keys):
    create_certificate(
        users['finn'],
        certificate_types[CERT_TYPE.HOST],
        uploaded_to_yav=True,
        private_key=PrivateKey.objects.create(data=generate_private_key()),
    )
    create_certificate(
        users['normal_user'],
        certificate_types[CERT_TYPE.HOST],
        uploaded_to_yav=False,
        private_key=PrivateKey.objects.create(data=generate_private_key()),
    )
    create_certificate(
        users['normal_user'],
        certificate_types[CERT_TYPE.HOST],
        uploaded_to_yav=True,
        private_key=PrivateKey.objects.create(data=generate_private_key()),
    )

    with override_config(DELETE_WHITELIST=['finn']):
        with override_switch(TASK_TYPE.DELETE_UPLOADED_PRIVATE_KEYS, active=delete_keys):
            call_command(TASK_TYPE.DELETE_UPLOADED_PRIVATE_KEYS)

    assert (Certificate.objects.filter(private_key__isnull=True).count() ==
            (1 if delete_keys else 0))
