from constance.test import override_config
import mock
import pytest

from django.conf import settings
from django.core.management import call_command
from django.test.utils import mail
from django.utils import timezone
from django_abc_data.models import AbcService

from intranet.crt.core.models import PrivateKey
from intranet.crt.constants import CERT_TYPE
from __tests__.utils.common import MockYavClient, create_certificate, assert_contains, create_user
from __tests__.utils.ssl import generate_private_key
from intranet.crt.users.models import CrtGroup
from intranet.crt.constants import ABC_CERTIFICATE_MANAGER_SCOPE, ABC_ADMINISTRATOR_SCOPE

pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('scope', [ABC_CERTIFICATE_MANAGER_SCOPE, ABC_ADMINISTRATOR_SCOPE])
def test_send_notify_to_abc_scope(users, abc_services, certificate_types, crt_robot, scope):
    abc_service = AbcService.objects.get(external_id=2)
    abc_service_with_scope = AbcService.objects.get(external_id=3)
    group = CrtGroup.objects.create(
        type='servicerole',
        external_id=100,
        name='Test',
        url='Test',
        role_scope=scope,
        is_deleted=False,
        abc_service=abc_service_with_scope,
    )
    group.users.add(users['finn'])
    group.users.add(users['jake'])
    group.users.add(users['normal_user'])

    # Сертификат с валидным secret_id, abc сервис без скоупов, письмо уйдет certificate.user
    certificate1 = create_certificate(
        users['normal_user'],
        certificate_types[CERT_TYPE.HOST],
        hosts=['test1@yandex.ru'],
        serial_number='1',
        abc_service=abc_service,
    )
    certificate1.private_key = PrivateKey.objects.create(data=generate_private_key())
    certificate1.yav_secret_id = 'other_valid_secret_uuid'
    certificate1.save(update_fields=['yav_secret_id', 'private_key'])

    # Сертификат с невалидным secret_id, abc сервис без скоупов, письмо уйдет certificate.user
    certificate2 = create_certificate(
        users['normal_user'],
        certificate_types[CERT_TYPE.HOST],
        hosts=['test2@yandex.ru'],
        serial_number='2',
        abc_service=abc_service,
    )
    certificate2.private_key = PrivateKey.objects.create(data=generate_private_key())
    certificate2.yav_secret_id = 'unvalid_secret_uuid'
    certificate2.save(update_fields=['yav_secret_id', 'private_key'])

    # Сертификат с невалидным secret_id, заказан по csr, письмо никому не отправляем
    certificate3 = create_certificate(
        users['normal_user'],
        certificate_types[CERT_TYPE.HOST],
        hosts=['test3@yandex.ru'],
        serial_number='3',
        abc_service=abc_service,
        request='request',
    )
    certificate3.yav_secret_id = 'unvalid_secret_uuid'
    certificate3.save(update_fields=['yav_secret_id'])

    # Закажим не хостовый сертификат, письмо никому не отправится
    certificate4 = create_certificate(
        users['normal_user'],
        certificate_types[CERT_TYPE.PC],
        serial_number='4',
    )
    certificate4.yav_secret_id = 'valid_secret_uuid'
    certificate4.save(update_fields=['yav_secret_id'])

    # Сертификат с невалидным secret_id, abc сервис без скоупов, письмо уйдет certificate.user, finn, jake
    certificate5 = create_certificate(
        users['normal_user'],
        certificate_types[CERT_TYPE.HOST],
        hosts=['test5@yandex.ru'],
        serial_number='5',
        abc_service=abc_service_with_scope,
    )
    certificate5.private_key = PrivateKey.objects.create(data=generate_private_key())
    certificate5.yav_secret_id = 'unvalid_secret_uuid'
    certificate5.save(update_fields=['yav_secret_id', 'private_key'])

    # Сертификат с begin_date через 5 дней, секрет не пишем, письмо никому не отправим
    certificate6 = create_certificate(
        users['normal_user'],
        certificate_types[CERT_TYPE.HOST],
        hosts=['test6@yandex.ru'],
        serial_number='6',
        abc_service=abc_service_with_scope,
    )
    certificate6.private_key = PrivateKey.objects.create(data=generate_private_key())
    certificate6.yav_secret_id = 'unvalid_secret_uuid'
    certificate6.begin_date = timezone.now() + timezone.timedelta(days=5)
    certificate6.save(update_fields=['yav_secret_id', 'private_key', 'begin_date'])

    with mock.patch('intranet.crt.core.models.get_yav_client') as get_client:
        get_client.return_value = MockYavClient()
        call_command('save_private_keys_to_yav')

    assert len(mail.outbox) == 4
    mail1, mail2, mail3_1, mail3_2 = mail.outbox

    assert mail1.subject == 'Сертификат готов ({})'.format(users['normal_user'].email)
    assert_contains(
        mail1.body,
        [
            'Сертификат и приватный ключ загружены',
            'href="https://yav.yandex-team.ru/secret/other_valid_secret_uuid/"',
            'secret_id = other_valid_secret_uuid, version_id = 2'
        ],
    )

    assert mail2.subject == 'Сертификат готов ({})'.format(users['normal_user'].email)
    assert_contains(
        mail2.body,
        [
            'Сертификат и приватный ключ загружены',
            'href="https://yav.yandex-team.ru/secret/valid_secret_uuid/"',
            'secret_id = valid_secret_uuid, version_id = 1'
        ],
    )

    assert mail3_1.subject == 'Сертификат готов ({})'.format(users['normal_user'].email)
    assert_contains(
        mail3_1.body,
        [
            'Сертификат и приватный ключ загружены',
            'href="https://yav.yandex-team.ru/secret/valid_secret_uuid/"',
            'secret_id = valid_secret_uuid, version_id = 1'
        ],
    )

    assert mail3_2.subject == 'Сертификат готов ({}, {})'.format(users['finn'].email, users['jake'].email)
    assert_contains(
        mail3_2.body,
        [
            'Вы получили это письмо, поскольку входите в скоуп "Управление сертификатами" или "Администрирование" ABC сервиса "{}"'
            .format(abc_service_with_scope.name),
            'Сертификат, запрошенный пользователем {}'.format(users['normal_user'].username),
            'Сертификат и приватный ключ загружены',
            'href="https://yav.yandex-team.ru/secret/valid_secret_uuid/"',
            'secret_id = valid_secret_uuid, version_id = 1',
        ],
    )


def test_write_to_yav(users, abc_services, certificate_types, crt_robot):
    abc_service = AbcService.objects.get(external_id=2)
    certificate = create_certificate(
        users['normal_user'],
        certificate_types[CERT_TYPE.HOST],
        hosts=['test@yandex.ru'],
        serial_number='1',
        abc_service=abc_service,
    )
    certificate.private_key = PrivateKey.objects.create(data=generate_private_key())
    certificate.yav_secret_id = 'unvalid_secret_uuid'
    certificate.save(update_fields=['yav_secret_id', 'private_key'])
    client = MockYavClient()
    with mock.patch('intranet.crt.core.models.get_yav_client') as get_client:
        get_client.return_value = client
        call_command('save_private_keys_to_yav')

    parts = [certificate.certificate]
    ca_chain_filename = certificate.controller.ca_cls.get_chain_path(is_ecc=certificate.is_ecc)
    with open(ca_chain_filename) as chain_file:
        parts.append(chain_file.read())
    parts = [_f for _f in parts if _f]
    pem_cert = '\n'.join(parts)

    required_data = [
        {'key': '%s_private_key' % certificate.serial_number, 'value': certificate.priv_key},
        {'key': '%s_certificate' % certificate.serial_number, 'value': pem_cert},
        {'key': '%s_key_cert' % certificate.serial_number, 'value': certificate.priv_key + pem_cert},
    ]

    sorting_key = lambda x: x['key']
    assert sorted(client.kwargs[0]['value'], key=sorting_key) == sorted(required_data, key=sorting_key)


def test_dont_write_to_yav(users, abc_services, certificate_types, crt_robot):
    abc_service = AbcService.objects.get(external_id=2)
    users['normal_user'].username = 'robot-mdbcert'
    users['normal_user'].save(update_fields=['username'])
    certificate = create_certificate(
        users['normal_user'],
        certificate_types[CERT_TYPE.HOST],
        hosts=['test@yandex.ru'],
        serial_number='1',
        abc_service=abc_service,
        uploaded_to_yav=None
    )
    certificate.private_key = PrivateKey.objects.create(data=generate_private_key())
    certificate.yav_secret_id = 'unvalid_secret_uuid'
    certificate.save(update_fields=['yav_secret_id', 'private_key'])
    client = MockYavClient()
    with mock.patch('intranet.crt.core.models.get_yav_client') as get_client:
        get_client.return_value = client
        call_command('save_private_keys_to_yav')

    assert client.args == []
    assert client.kwargs == []


def test_check_do_not_share_access_to_secret_with_cert(abc_services, certificate_types, crt_robot):
    abc_service = AbcService.objects.get(external_id=2)
    qloud_robot = create_user(settings.CRT_CERT_REQUESTERS_DO_NOT_SHARE_ACCESS[0])
    certificate = create_certificate(
        qloud_robot,
        certificate_types[CERT_TYPE.HOST],
        hosts=['test@yandex.ru'],
        serial_number='1',
        abc_service=abc_service,
    )
    certificate.private_key = PrivateKey.objects.create(data=generate_private_key())
    certificate.save(update_fields=['private_key'])
    client = MockYavClient()
    with mock.patch('intranet.crt.core.models.get_yav_client') as get_client:
        get_client.return_value = client
        call_command('save_private_keys_to_yav')

    assert len(mail.outbox) == 0
    assert client.roles_to_secrets == {
        'valid_secret_uuid': {('OWNER', qloud_robot.username,), ('APPENDER', crt_robot.username)}
    }


@override_config(MARKET_ABC_SERVICE_SLUG='MARKETITO')
@override_config(MARKET_ZOMBIES='zomb-user, zomb-user2')
@pytest.mark.parametrize('cert_type', (CERT_TYPE.ZOMBIE, CERT_TYPE.NINJA))
@pytest.mark.parametrize('cert_user', ('zomb-user', 'normal_user'))
@pytest.mark.parametrize('service_exists', (True, False))
def test_market_zombies_certs_is_being_written_to_yav(
    crt_robot, users, certificate_types, service_exists, cert_type, cert_user
):
    if service_exists:
        AbcService.objects.create(
            slug='MARKETITO', external_id=42, created_at=timezone.now(), modified_at=timezone.now(),
        )
    certificate = create_certificate(
        users[cert_user],
        certificate_types[cert_type],
        serial_number='1',
    )
    certificate.private_key = PrivateKey.objects.create(data=generate_private_key())
    certificate.save(update_fields=['private_key'])

    client = MockYavClient()
    with mock.patch('intranet.crt.core.models.get_yav_client') as get_client:
        get_client.return_value = client
        call_command('save_private_keys_to_yav')

    if not (
        cert_type == CERT_TYPE.ZOMBIE and cert_user == 'zomb-user'
    ):
        assert client.kwargs == []
        assert client.roles_to_secrets == {}
        return

    parts = [certificate.certificate]
    ca_chain_filename = certificate.controller.ca_cls.get_chain_path(is_ecc=certificate.is_ecc)
    with open(ca_chain_filename) as chain_file:
        parts.append(chain_file.read())
    parts = [_f for _f in parts if _f]
    pem_cert = '\n'.join(parts)

    required_data = [
        {'key': '%s_private_key' % certificate.serial_number, 'value': certificate.priv_key},
        {'key': '%s_certificate' % certificate.serial_number, 'value': pem_cert},
        {'key': '%s_key_cert' % certificate.serial_number, 'value': certificate.priv_key + pem_cert},
    ]

    sorting_key = lambda x: x['key']
    assert sorted(client.kwargs[0]['value'], key=sorting_key) == sorted(required_data, key=sorting_key)

    expected_roles = {('APPENDER', 'robot-crt'), ('OWNER', 'zomb-user')}
    if service_exists:
        expected_roles.add(('OWNER', (42, 'cert')))
    assert client.roles_to_secrets['valid_secret_uuid'] == expected_roles
