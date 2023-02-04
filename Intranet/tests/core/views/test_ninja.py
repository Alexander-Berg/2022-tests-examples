import datetime
import pytest
import mock
from constance.test import override_config
from freezegun import freeze_time
from rest_framework import status

from django.utils import timezone
from django.utils.encoding import force_text

from intranet.crt.constants import CERT_TYPE, CA_NAME, CERT_STATUS
from intranet.crt.core.models import Certificate
from __tests__.utils.common import create_certificate
from intranet.crt.users.models import CrtUser
from intranet.crt.utils.ssl import parse_pfx, PemCertificate

pytestmark = pytest.mark.django_db


def request_ninja(crt_client, requester, os_family, is_exchange=False, **request_data):
    crt_client.login(requester)
    data = {
        'user': requester,
    }
    data.update(request_data)

    with mock.patch('intranet.crt.core.views.get_os_family') as get_os_family:
        get_os_family.return_value = os_family
        with mock.patch('intranet.crt.core.views.is_exchange_user') as is_exchange_user:
            is_exchange_user.return_value = is_exchange
            return crt_client.post('/ninja/', data=data)


def ninja_certs_count(requester, user):
    return Certificate.objects.filter(
        requester__username=requester, user__username=user,
        type__name__in=(CERT_TYPE.NINJA, CERT_TYPE.NINJA_EXCHANGE)
    ).count()


@pytest.mark.parametrize('cert_type_name', (CERT_TYPE.NINJA, CERT_TYPE.NINJA_EXCHANGE))
def test_ninja_request(crt_client, users, settings, cert_type_name, normal_user_ld_cert):
    settings.INTERNAL_CA = CA_NAME.TEST_CA
    normal_user_name = 'normal_user'
    device_platform = 'android'
    is_exchange = cert_type_name == CERT_TYPE.NINJA_EXCHANGE
    cn_suffix = 'ld.yandex.ru' if is_exchange else 'pda-ld.yandex.ru'

    response = request_ninja(crt_client, normal_user_name, device_platform, is_exchange)

    assert response.status_code == status.HTTP_201_CREATED
    assert response['content-type'] == 'application/x-pkcs12'

    pfx = response.content
    pem_cert, _ = parse_pfx(pfx, settings.CRT_NINJA_PFX_PASSWORD)
    serial_number = PemCertificate(pem_cert).serial_number

    cert = Certificate.objects.filter(serial_number=serial_number).first()
    assert cert is not None
    assert cert.type.name == cert_type_name
    assert cert.device_platform == device_platform
    assert cert.common_name == '{}@{}'.format(normal_user_name, cn_suffix)


def test_ninja_exchange_ios_request(crt_client, users, settings, normal_user_ld_cert):
    settings.INTERNAL_CA = CA_NAME.TEST_CA
    normal_user_name = 'normal_user'
    device_platform = 'ios'
    is_exchange = True

    response = request_ninja(crt_client, normal_user_name, device_platform, is_exchange)

    assert response.status_code == status.HTTP_201_CREATED
    assert response['content-type'] == 'text/plain'


def test_ninja_double_request(crt_client, users, certificate_types, settings, normal_user_ld_cert):
    settings.INTERNAL_CA = CA_NAME.TEST_CA
    normal_user_name = 'normal_user'
    device_platform = 'android'
    is_exchange = False
    cert_type = certificate_types[CERT_TYPE.NINJA]
    common_name = '{}@pda-ld.yandex.ru'.format(normal_user_name)
    added = timezone.now() - datetime.timedelta(minutes=settings.CRT_NINJA_ISSUE_THRESHOLD - 1)

    create_certificate(users[normal_user_name], cert_type, common_name=common_name, added=added)

    response = request_ninja(crt_client, normal_user_name, device_platform, is_exchange)

    assert response.status_code == status.HTTP_400_BAD_REQUEST


def test_ninja_quota_exceeded(crt_client, users, settings, certificate_types, normal_user_ld_cert):
    """Если сертификатов уже слишком много, то нужно сначала отозвать как минимум несколько имеющихся"""
    settings.INTERNAL_CA = CA_NAME.TEST_CA
    normal_user_name = 'normal_user'
    device_platform = 'android'
    is_exchange = False
    cert_type = certificate_types[CERT_TYPE.NINJA]
    common_name = '{}@pda-ld.yandex.ru'.format(normal_user_name)
    added = timezone.now() - datetime.timedelta(minutes=30)

    # Куча "мусорных" сертификатов, которые в квоте не учитываются
    for i in range(20):
        cert = create_certificate(users[normal_user_name], cert_type, common_name=common_name, added=added)
        cert.status = CERT_STATUS.EXPIRED
        cert.save()
    for i in range(20):
        cert = create_certificate(users[normal_user_name], cert_type, common_name=common_name, added=added)
        cert.status = CERT_STATUS.ERROR
        cert.save()
    for i in range(20):
        cert = create_certificate(users[normal_user_name], cert_type, common_name=common_name, added=added)
        cert.status = CERT_STATUS.REVOKED
        cert.save()

    certificates = [
        create_certificate(users[normal_user_name], cert_type, common_name=common_name, added=added)
        for i
        in range(6)
    ]
    certificate_fields = ['{}__certificate_{}'.format(normal_user_name, cert.pk) for cert in certificates]
    old_certs_number = Certificate.objects.count()

    response = request_ninja(crt_client, normal_user_name, device_platform, is_exchange)
    assert response.status_code == status.HTTP_200_OK  # в случае успеха - 201. Нужно отозвать хотя бы 2 сертификата
    assert Certificate.objects.count() == old_certs_number

    response = request_ninja(
        crt_client, normal_user_name, device_platform, is_exchange,
        **{certificate_fields[0]: True}
    )
    assert response.status_code == status.HTTP_200_OK  # хотим отозвать один, а нужно два
    assert Certificate.objects.count() == old_certs_number

    response = request_ninja(
        crt_client, normal_user_name, device_platform, is_exchange,
        **{certificate_fields[0]: True, certificate_fields[1]: True}
    )
    assert response.status_code == status.HTTP_201_CREATED  # отозвали два, теперь всё должно получиться
    assert Certificate.objects.count() == old_certs_number + 1  # сертификат создался
    certificates[0].refresh_from_db()
    certificates[1].refresh_from_db()
    # Два указанных сертификата тем временем должны отозваться
    assert certificates[0].status == CERT_STATUS.REVOKED
    assert certificates[1].status == CERT_STATUS.REVOKED
    # А другие – нет
    for i in range(2, 6):
        assert certificates[i].status == CERT_STATUS.ISSUED


def test_individual_ninja_quota(crt_client, users, settings, certificate_types, normal_user_ld_cert):
    """Если сертификатов уже слишком много, то нужно сначала отозвать как минимум несколько имеющихся"""
    settings.INTERNAL_CA = CA_NAME.TEST_CA
    normal_user_name = 'normal_user'
    device_platform = 'android'
    is_exchange = False
    cert_type = certificate_types[CERT_TYPE.NINJA]
    common_name = '{}@pda-ld.yandex.ru'.format(normal_user_name)
    added = timezone.now() - datetime.timedelta(minutes=30)

    for i in range(6):
        create_certificate(users[normal_user_name], cert_type, common_name=common_name, added=added)

    old_certs_number = Certificate.objects.filter(type=cert_type).count()
    assert old_certs_number == 6

    response = request_ninja(crt_client, normal_user_name, device_platform, is_exchange)
    assert response.status_code == status.HTTP_200_OK  # в случае успеха - 201. Нужно отозвать хотя бы 2 сертификата
    assert Certificate.objects.filter(type=cert_type).count() == old_certs_number

    CrtUser.objects.filter(username=normal_user_name).update(ninja_certs_quota=3)

    response = request_ninja(crt_client, normal_user_name, device_platform, is_exchange)
    assert response.status_code == status.HTTP_200_OK  # теперь вообще надо отозвать 4 сертификата
    assert Certificate.objects.filter(type=cert_type).count() == old_certs_number

    CrtUser.objects.filter(username=normal_user_name).update(ninja_certs_quota=7)

    response = request_ninja(crt_client, normal_user_name, device_platform, is_exchange)
    assert response.status_code == status.HTTP_201_CREATED  # с повышенной квотой сертификат выдался
    assert Certificate.objects.filter(type=cert_type).count() == old_certs_number + 1


@pytest.mark.parametrize('init_cert_count, status_code, final_cert_count', [
    (9, status.HTTP_201_CREATED, 10),
    (10, status.HTTP_200_OK, 10),
])
def test_ninja_zomb_quota(crt_client, users, settings, certificate_types, init_cert_count,
                          status_code, final_cert_count, zomb_pc_user_ld_cert):
    """Если сертификатов уже слишком много, то нужно сначала отозвать как минимум несколько имеющихся"""
    settings.INTERNAL_CA = CA_NAME.TEST_CA
    requester = 'zomb_pc_user'
    zombie = 'zomb-user'
    device_platform = 'android'
    is_exchange = False
    cert_type = certificate_types[CERT_TYPE.NINJA]
    common_name = f'{zombie}@pda-ld.yandex.ru'
    added = timezone.now() - datetime.timedelta(minutes=30)

    for i in range(init_cert_count):
        create_certificate(users[zombie], cert_type, common_name=common_name, added=added)
    assert init_cert_count == Certificate.objects.filter(type=cert_type).count()

    users[requester].robots.add(users[zombie])
    response = request_ninja(crt_client, requester, device_platform, is_exchange, user=zombie)
    assert response.status_code == status_code
    assert Certificate.objects.filter(type=cert_type).count() == final_cert_count


def test_ninja_double_request_for_zombies(crt_client, users, certificate_types,
                                          settings, normal_user_ld_cert):
    settings.INTERNAL_CA = CA_NAME.TEST_CA
    requester = 'normal_user'
    zombie = 'zomb-user'
    device_platform = 'android'
    is_exchange = False

    create_certificate(
        users[zombie],
        certificate_types[CERT_TYPE.PC],
        common_name='zomb-user@ld.yandex.ru'
    )

    response = request_ninja(crt_client, requester, device_platform, is_exchange)
    assert response.status_code == status.HTTP_201_CREATED
    assert ninja_certs_count(requester, requester) == 1

    response = request_ninja(crt_client, requester, device_platform, is_exchange)
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert ninja_certs_count(requester, requester) == 1

    with freeze_time(timezone.now() + timezone.timedelta(settings.CRT_NINJA_ISSUE_THRESHOLD + 1)):
        response = request_ninja(crt_client, requester, device_platform, is_exchange)
    assert response.status_code == status.HTTP_201_CREATED
    assert ninja_certs_count(requester, requester) == 2

    response = request_ninja(crt_client, requester, device_platform, is_exchange, **{'user': zombie})
    assert response.status_code == status.HTTP_200_OK
    assert 'You cannot request certificate for zomb-user' in force_text(response.content)
    assert ninja_certs_count(requester, zombie) == 0

    users[requester].robots.add(users[zombie])

    response = request_ninja(crt_client, requester, device_platform, is_exchange, **{'user': zombie})
    assert response.status_code == status.HTTP_201_CREATED
    assert ninja_certs_count(requester, zombie) == 1

    response = request_ninja(crt_client, requester, device_platform, is_exchange, **{'user': zombie})
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert ninja_certs_count(requester, zombie) == 1

    with freeze_time(timezone.now() + timezone.timedelta(settings.CRT_NINJA_ISSUE_THRESHOLD + 1)):
        response = request_ninja(crt_client, requester, device_platform, is_exchange, **{'user': zombie})
    assert response.status_code == status.HTTP_201_CREATED
    assert ninja_certs_count(requester, zombie) == 2


def test_ninja_double_request_after_revoking(crt_client, users, certificate_types,
                                             settings, normal_user_ld_cert):
    settings.INTERNAL_CA = CA_NAME.TEST_CA
    requester = 'normal_user'
    device_platform = 'android'
    is_exchange = False

    response = request_ninja(crt_client, requester, device_platform, is_exchange)
    assert response.status_code == status.HTTP_201_CREATED
    assert ninja_certs_count(requester, requester) == 1

    response = request_ninja(crt_client, requester, device_platform, is_exchange)
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert ninja_certs_count(requester, requester) == 1

    Certificate.objects.filter(
        type__name__in=(CERT_TYPE.NINJA, CERT_TYPE.NINJA_EXCHANGE)
    ).update(status=CERT_STATUS.REVOKED)

    response = request_ninja(crt_client, requester, device_platform, is_exchange)
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert ninja_certs_count(requester, requester) == 1

    with freeze_time(timezone.now() + timezone.timedelta(settings.CRT_NINJA_ISSUE_THRESHOLD + 1)):
        response = request_ninja(crt_client, requester, device_platform, is_exchange)

    assert response.status_code == status.HTTP_201_CREATED
    assert ninja_certs_count(requester, requester) == 2


@pytest.mark.parametrize('cert_user', ['normal_user', 'zomb-user'])
def test_ninja_active_domain_cert_required(crt_client, users, settings, certificate_types, cert_user):
    """PDAS выписывают только владельцы активных сертификатов @ld.yandex.ru для себя и своих зомби"""
    settings.INTERNAL_CA = CA_NAME.TEST_CA
    requester = users['normal_user']
    zombie = users['zomb-user']
    requester.robots.add(zombie)

    response = request_ninja(crt_client, requester.username, 'android', user=users[cert_user])
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response.content == (b'User normal_user has no active certificates '
                                b'to access Yandex network. Issuing prohibited.')

    cert = create_certificate(
        requester, certificate_types[CERT_TYPE.PC], common_name=f'{requester.username}@ld.yandex.ru'
    )
    response = request_ninja(crt_client, requester.username, 'android', user=users[cert_user])
    assert response.status_code == status.HTTP_201_CREATED

    cert.controller.revoke()

    with freeze_time(timezone.now() + timezone.timedelta(settings.CRT_NINJA_ISSUE_THRESHOLD + 1)):
        response = request_ninja(crt_client, requester.username, 'android', user=users[cert_user])
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response.content == (b'User normal_user has no active certificates '
                                b'to access Yandex network. Issuing prohibited.')


def test_ninja_requesting_for_robots(crt_client, users, settings, certificate_types):
    """Выписать PDAS на робота robot-* нельзя"""
    settings.INTERNAL_CA = CA_NAME.TEST_CA
    requester = users['normal_user']
    robot = users['robot-user']
    requester.robots.add(robot)

    response = request_ninja(crt_client, requester.username, 'android', user=robot)
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response.content == (b'Requesting certificates for robots is prohibited')


def test_ninja_request_with_pdas_whitelist(crt_client, users, settings, certificate_types):
    settings.INTERNAL_CA = CA_NAME.TEST_CA
    normal_user = users['normal_user']
    zombie = users['zomb-user']
    normal_user.robots.add(zombie)

    # Зомби без ld-серта не может выписать себе pdas
    assert not zombie.certificates.exists()
    response = request_ninja(crt_client, zombie.username, 'android', user=zombie)
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response.content == (b'User zomb-user has no active certificates '
                                b'to access Yandex network. Issuing prohibited.')

    # Зомби в PDAS_WHITELIST может выписать pdas без ld-серта
    with override_config(PDAS_WHITELIST='zomb-user'):
        response = request_ninja(crt_client, zombie.username, 'android', user=zombie)
    assert response.status_code == status.HTTP_201_CREATED

    # Владелец зомби без ld-серта не может выписать pdas, даже если зомби в PDAS_WHITELIST
    assert not normal_user.certificates.exists()
    with freeze_time(timezone.now() + timezone.timedelta(settings.CRT_NINJA_ISSUE_THRESHOLD + 1)):
        with override_config(PDAS_WHITELIST='zomb-user'):
            response = request_ninja(crt_client, normal_user.username, 'android', user=zombie)
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response.content == (b'User normal_user has no active certificates '
                                b'to access Yandex network. Issuing prohibited.')
