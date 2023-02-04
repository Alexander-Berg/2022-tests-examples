import contextlib

import pytest
import mock

from django.core.exceptions import ValidationError
from django.db import connection
from django.utils.encoding import force_text

from intranet.crt.constants import CERT_TYPE
from intranet.crt.core.models import PrivateKey, HostToApprove
from __tests__.utils.common import create_certificate
from __tests__.utils.ssl import generate_private_key
from intranet.crt.utils.ssl import PrivateKeyCryptographer

pytestmark = pytest.mark.django_db


def test_serial_number_isupper_check(crt_robot, certificate_types):
    with pytest.raises(AssertionError):
        create_certificate(crt_robot, certificate_types['pc'], serial_number='abc123')


def test_private_key_encryption(settings):
    private_key_pem = generate_private_key()
    PrivateKey.objects.create(data=private_key_pem)

    with connection.cursor() as cursor:
        cursor.execute('select data from {}'.format(PrivateKey._meta.db_table))
        encrypted_private_key = force_text(cursor.fetchone()[0])

    cryptographer = PrivateKeyCryptographer(settings.CRT_PRIVATE_KEY_PASSWORDS)
    assert cryptographer.decrypt(encrypted_private_key) == private_key_pem

    assert PrivateKey.objects.first().data == private_key_pem


@pytest.mark.parametrize('cert_type', CERT_TYPE.USERNAME_IN_COMMON_NAME_TYPES)
def test_get_subject_user(users, certificate_types, cert_type):
    normal_user = users['normal_user']
    another_user = users['another_user']
    zomb_user = users['zomb-user']
    common_names = {
        CERT_TYPE.COURTECY_VPN: 'normal_user@ld.yandex.ru',
        CERT_TYPE.ZOMBIE: 'zomb-user@ld.yandex.ru',
        CERT_TYPE.NINJA: 'normal_user@pda-ld.yandex.ru',
        CERT_TYPE.ASSESSOR: 'normal_user@assessors.yandex-team.ru',
        CERT_TYPE.VPN_TOKEN: 'normal_user@ld.yandex.ru',
        CERT_TYPE.VPN_1D: 'normal_user@ld.yandex.ru',
        CERT_TYPE.TPM_SMARTCARD_1C: 'normal_user@smartcard',
        CERT_TYPE.LINUX_PC: 'normal_user@ld.yandex.ru',
        CERT_TYPE.LINUX_TOKEN: 'normal_user@ld.yandex.ru',
        CERT_TYPE.PC: 'normal_user@ld.yandex.ru',
        CERT_TYPE.BANK_PC: 'normal_user@ld.yandex.ru',
        CERT_TYPE.MOBVPN: 'normal_user@ld.yandex.ru',
        CERT_TYPE.HYPERCUBE: 'normal_user@pda-ld.yandex.ru',
        CERT_TYPE.NINJA_EXCHANGE: 'normal_user@ld.yandex.ru',
        CERT_TYPE.IMDM: 'normal_user@ld.yandex.ru',
        CERT_TYPE.TEMP_PC: 'normal_user@pda-ld.yandex.ru',
    }
    cert = create_certificate(
        another_user, certificate_types[cert_type], common_name=common_names[cert_type]
    )
    if cert_type == CERT_TYPE.ZOMBIE:
        assert cert.get_subject_user() == zomb_user
    else:
        assert cert.get_subject_user() == normal_user


@contextlib.contextmanager
def _patch_managed_domain():
    with mock.patch('intranet.crt.core.models.get_name_servers', return_value={"ns1.yandex.ru", "ns2.yandex.ru"}):
        yield


@pytest.mark.parametrize('domain', ["0", "-", "sdffdsfsdfsds"])
def test_incorrect_host_to_approve_globalsign_domain(domain):
    with _patch_managed_domain(), pytest.raises(ValidationError):
        HostToApprove.objects.create(
            host="rocco66.test",
            name_servers="ns3.yandex.ru,ns4.yandex.ru",
            globalsign_domain_id=domain,
        ).full_clean()


@pytest.mark.parametrize('domain', ["", "DSMS20000137387"])
def test_correct_host_to_approve_globalsign_domain(domain):
    with _patch_managed_domain():
        HostToApprove.objects.create(
            host="rocco66.test",
            name_servers="ns3.yandex.ru,ns4.yandex.ru",
            globalsign_domain_id=domain,
        ).full_clean()
