from freezegun import freeze_time
import pytest
import os
import mock

from django.core import mail
from django.core.management import call_command
from django.utils import timezone
from django_abc_data.models import AbcService

from intranet.crt.users.models import CrtGroup
from __tests__.utils.common import (
    create_user, create_certificate, create_certificate_type, assert_contains,
    create_approve_request,
    create_host, MockStartrekResponse, mock_startrek
)
from intranet.crt.constants import CERT_TYPE, CA_NAME, ABC_ADMINISTRATOR_SCOPE, ABC_CERTIFICATE_MANAGER_SCOPE, CERT_STATUS

pytestmark = pytest.mark.django_db


def test_expiring_host_certs_notify_responsibles(settings, crt_robot):
    user = create_user('testuser')
    resp_user = create_user('responsible')
    type_host = create_certificate_type(CERT_TYPE.HOST)
    days = [0, 1, 4, 7, 10, 14]
    certs = []
    hosts = []
    service = AbcService.objects.create(external_id=100, created_at=timezone.now(), modified_at=timezone.now())
    CrtGroup.objects.create(abc_service=service, external_id=10, is_deleted=False, role_scope=ABC_ADMINISTRATOR_SCOPE).users.add(resp_user)

    with freeze_time(timezone.now()):
        for i, day in enumerate(days):
            cert = create_certificate(user, type_host, serial_number=str(i), days=day, abc_service=service)
            host = create_host('host-{}.yandex.ru'.format(i))
            cert.hosts.add(host)
            certs.append(cert)
            hosts.append(host)

    call_command('expiring_host_certs_notify')

    assert len(mail.outbox) == 2
    assert mail.outbox[0].cc == []
    assert mail.outbox[0].subject == 'Срок действия сертификата testuser@ld.yandex.ru (TestCA) подходит к концу (responsible@yandex-team.ru)'

    card_url = '{}/certificates/{}?serial_number={}'.format(settings.CRT_URL, certs[3].id, certs[3].serial_number)
    assert_contains(mail.outbox[0].body, [
        'Срок действия сертификата <a href="{}">3</a> (TestCA) с Common Name testuser@ld.yandex.ru истекает через 7 дней ({}).'
        .format(card_url, certs[3].end_date.astimezone().strftime('%d.%m.%Y')),
        '<p>Сертификат действителен для доменов:</p>\nhost-3.yandex.ru',
        '<p>Мы не нашли сертификатов, выписанных на домены:</p>\nhost-3.yandex.ru',
    ])

    card_url = '{}/certificates/{}?serial_number={}'.format(settings.CRT_URL, certs[5].id, certs[5].serial_number)
    assert_contains(mail.outbox[1].body, [
        'Срок действия сертификата <a href="{}">5</a> (TestCA) с Common Name testuser@ld.yandex.ru истекает через 14 дней ({}).'
                    .format(card_url, certs[5].end_date.astimezone().strftime('%d.%m.%Y')),
        '<p>Сертификат действителен для доменов:</p>\nhost-5.yandex.ru',
        '<p>Мы не нашли сертификатов, выписанных на домены:</p>\nhost-5.yandex.ru',
    ])

    mail.outbox = []

    settings.CRT_TEST_NOTIFICATIONS = False
    call_command('expiring_host_certs_notify')

    assert mail.outbox[0].cc == [settings.CRT_CERT_EXPIRATION_NOTIFICATIONS_CC]


def test_expiring_host_certs_notify_requesters(settings, crt_robot):
    user = create_user('testuser')
    type_host = create_certificate_type(CERT_TYPE.HOST)
    days = [0, 1, 4, 7, 10, 14]
    certs = []
    hosts = []
    with freeze_time(timezone.now()):
        for i, day in enumerate(days):
            cert = create_certificate(user, type_host, serial_number=str(i), days=day)
            host = create_host('host-{}.yandex.ru'.format(i))
            cert.hosts.add(host)
            certs.append(cert)
            hosts.append(host)

    call_command('expiring_host_certs_notify')

    assert len(mail.outbox) == 2
    assert mail.outbox[0].cc == []
    assert mail.outbox[0].subject == 'Срок действия сертификата testuser@ld.yandex.ru (TestCA) подходит к концу (testuser@yandex-team.ru)'

    card_url = '{}/certificates/{}?serial_number={}'.format(settings.CRT_URL, certs[3].id, certs[3].serial_number)
    assert_contains(mail.outbox[0].body, [
        'Срок действия сертификата <a href="{}">3</a> (TestCA) с Common Name testuser@ld.yandex.ru истекает через 7 дней ({}).'
        .format(card_url, certs[3].end_date.astimezone().strftime('%d.%m.%Y')),
        '<p>Сертификат действителен для доменов:</p>\nhost-3.yandex.ru',
        '<p>Мы не нашли сертификатов, выписанных на домены:</p>\nhost-3.yandex.ru',
    ])

    card_url = '{}/certificates/{}?serial_number={}'.format(settings.CRT_URL, certs[5].id, certs[5].serial_number)
    assert_contains(mail.outbox[1].body, [
        'Срок действия сертификата <a href="{}">5</a> (TestCA) с Common Name testuser@ld.yandex.ru истекает через 14 дней ({}).'
                    .format(card_url, certs[5].end_date.astimezone().strftime('%d.%m.%Y')),
        '<p>Сертификат действителен для доменов:</p>\nhost-5.yandex.ru',
        '<p>Мы не нашли сертификатов, выписанных на домены:</p>\nhost-5.yandex.ru',
    ])

    mail.outbox = []

    settings.CRT_TEST_NOTIFICATIONS = False
    call_command('expiring_host_certs_notify')

    assert mail.outbox[0].cc == [settings.CRT_CERT_EXPIRATION_NOTIFICATIONS_CC]


def test_expiring_host_certs_notify_with_nearby_days(settings, crt_robot):
    user = create_user('testuser')
    type_host = create_certificate_type(CERT_TYPE.HOST)
    days = [0, 1, 4, 7, 10, 14]
    certs = []
    hosts = []
    with freeze_time(timezone.now()):
        for i, day in enumerate(days):
            cert = create_certificate(user, type_host, serial_number=str(i), days=day)
            host = create_host('host-{}.yandex.ru'.format(i))
            cert.hosts.add(host)
            certs.append(cert)
            hosts.append(host)

    call_command('expiring_host_certs_notify', '--nearby-days')

    assert len(mail.outbox) == 2
    assert mail.outbox[0].subject == 'Срок действия сертификата testuser@ld.yandex.ru (TestCA) подходит к концу (testuser@yandex-team.ru)'
    assert mail.outbox[1].subject == 'Срок действия сертификата testuser@ld.yandex.ru (TestCA) подходит к концу (testuser@yandex-team.ru)'


def test_expiring_host_certs_notify_with_overlapping_hosts(settings, crt_robot):
    user = create_user('testuser')
    type_host = create_certificate_type(CERT_TYPE.HOST)

    days = [7, 7, 7, 14, 14, 14, 50]
    with freeze_time(timezone.now()):
        certs = [create_certificate(user, type_host, serial_number=str(i), days=day) for (i, day) in enumerate(days)]
    hosts = [create_host('host-0.yandex.ru'), create_host('host-1.yandex.ru'), create_host('host-2.yandex.ru')]

    # Уведомления об одном хосте
    certs[0].hosts.add(hosts[0])
    certs[3].hosts.add(hosts[2])

    # Уведомления всё ещё об одном хосте
    certs[1].hosts.add(hosts[0])
    certs[1].hosts.add(hosts[1])
    certs[4].hosts.add(hosts[1])
    certs[4].hosts.add(hosts[2])

    # Уведомление о замене, но не о перевыпуске
    certs[2].hosts.add(hosts[1])
    # Нет уведомлений
    certs[5].hosts.add(hosts[1])

    # Сертификат, перекрывающий некоторые более старые
    certs[6].hosts.add(hosts[1])

    call_command('expiring_host_certs_notify')

    assert len(mail.outbox) == 4
    assert mail.outbox[0].subject == 'Срок действия сертификата testuser@ld.yandex.ru (TestCA) подходит к концу (testuser@yandex-team.ru)'

    card_url = '{}/certificates/{}?serial_number={}'.format(settings.CRT_URL, certs[0].id, certs[0].serial_number)
    assert_contains(mail.outbox[0].body, [
        'Срок действия сертификата <a href="{}">0</a> (TestCA) с Common Name testuser@ld.yandex.ru истекает через 7 дней ({}).'
                    .format(card_url, certs[0].end_date.astimezone().strftime('%d.%m.%Y')),
        '<p>Сертификат действителен для доменов:</p>\nhost-0.yandex.ru',
        '<p>Мы не нашли сертификатов, выписанных на домены:</p>\nhost-0.yandex.ru',
    ])
    assert mail.outbox[1].subject == 'Срок действия сертификата testuser@ld.yandex.ru (TestCA) подходит к концу (testuser@yandex-team.ru)'
    card_url = '{}/certificates/{}?serial_number={}'.format(settings.CRT_URL, certs[1].id, certs[1].serial_number)
    assert_contains(mail.outbox[1].body, [
        'Срок действия сертификата <a href="{}">1</a> (TestCA) с Common Name testuser@ld.yandex.ru истекает через 7 дней ({}).'
                    .format(card_url, certs[1].end_date.astimezone().strftime('%d.%m.%Y')),
        '<p>Сертификат действителен для доменов:</p>\nhost-0.yandex.ru\nhost-1.yandex.ru',
        '<p>Мы не нашли сертификатов, выписанных на домены:</p>\nhost-0.yandex.ru',
    ])

    assert mail.outbox[2].subject == 'Срок действия сертификата testuser@ld.yandex.ru (TestCA) подходит к концу (testuser@yandex-team.ru)'
    card_url = '{}/certificates/{}?serial_number={}'.format(settings.CRT_URL, certs[3].id, certs[3].serial_number)
    assert_contains(mail.outbox[2].body, [
        'Срок действия сертификата <a href="{}">3</a> (TestCA) с Common Name testuser@ld.yandex.ru истекает через 14 дней ({}).'
                    .format(card_url, certs[3].end_date.astimezone().strftime('%d.%m.%Y')),
        '<p>Сертификат действителен для доменов:</p>\nhost-2.yandex.ru',
        '<p>Мы не нашли сертификатов, выписанных на домены:</p>\nhost-2.yandex.ru',
    ])

    assert mail.outbox[3].subject == 'Срок действия сертификата testuser@ld.yandex.ru (TestCA) подходит к концу (testuser@yandex-team.ru)'
    card_url = '{}/certificates/{}?serial_number={}'.format(settings.CRT_URL, certs[4].id, certs[4].serial_number)
    assert_contains(mail.outbox[3].body, [
        'Срок действия сертификата <a href="{}">4</a> (TestCA) с Common Name testuser@ld.yandex.ru истекает через 14 дней ({}).'
                    .format(card_url, certs[4].end_date.astimezone().strftime('%d.%m.%Y')),
        '<p>Сертификат действителен для доменов:</p>\nhost-1.yandex.ru\nhost-2.yandex.ru',
        '<p>Мы не нашли сертификатов, выписанных на домены:</p>\nhost-2.yandex.ru',
    ])


def test_creating_tickets_in_fire_queue(settings, crt_robot):
    """
        Проверка что тикеты в очереди FIRE создаются для сертификатов,
        выписанных CertumProductionCA, GlobalSignProductionCA
    """
    type_host = create_certificate_type(CERT_TYPE.HOST)
    days = [7, 7, 10, 14]
    ca_names = [CA_NAME.CERTUM_TEST_CA, CA_NAME.GLOBALSIGN_TEST_CA,
                CA_NAME.RC_INTERNAL_CA, CA_NAME.GLOBALSIGN_TEST_CA]

    with freeze_time(timezone.now()):
        certs = []

        for i, (day, ca_name) in enumerate(zip(days, ca_names)):
            certs.append(create_certificate(crt_robot, type_host, serial_number=str(i), days=day, ca_name=ca_name))

    hosts = [create_host('host-0.yandex.ru'), create_host('host-1.yandex.ru'),
             create_host('host-2.yandex.ru'), create_host('host-3.yandex.ru')]

    certs[0].hosts.add(hosts[0])
    certs[0].hosts.add(hosts[1])
    certs[0].hosts.add(hosts[2])
    certs[0].hosts.add(hosts[3])

    certs[1].hosts.add(hosts[0])
    certs[1].hosts.add(hosts[1])

    certs[2].hosts.add(hosts[2])
    certs[3].hosts.add(hosts[3])

    initial_count = MockStartrekResponse.sequence

    with mock.patch('intranet.crt.utils.startrek.get_component_id') as mocked_func:
        mocked_func.return_value = '1'
        with mock_startrek():
            call_command('expiring_host_certs_notify')

    expected_tickets = [
        (0, CA_NAME.CERTUM_TEST_CA),
        (1, CA_NAME.GLOBALSIGN_TEST_CA),
        (3, CA_NAME.GLOBALSIGN_TEST_CA),
    ]

    assert MockStartrekResponse.sequence - initial_count == len(expected_tickets)
    for response, ticket in zip(MockStartrekResponse.instances[initial_count-1:], expected_tickets):
        assert response.queue == settings.CRT_STARTREK_FIRE_QUEUE
        assert response.summary == 'Срок действия сертификата {} ({}) подходит к концу'.format(
                                        ticket[0],
                                        ticket[1],
                                    )


def test_sending_emails_about_expiring_certs(settings, crt_robot):
    """
        Проверка что тикеты в очереди FIRE создаются для сертификатов,
        выписанных CertumProductionCA, GlobalSignProductionCA
    """
    type_host = create_certificate_type(CERT_TYPE.HOST)
    days = [4, 7, 11, 14, 21]
    ca_names = [CA_NAME.RC_INTERNAL_CA, CA_NAME.GLOBALSIGN_PRODUCTION_CA, CA_NAME.YC_INTERNAL_CA,
                CA_NAME.CERTUM_PRODUCTION_CA, CA_NAME.INTERNAL_TEST_CA]
    user = create_user('testuser')

    with freeze_time(timezone.now()):
        certs = []

        for i, (day, ca_name) in enumerate(zip(days, ca_names)):
            certs.append(create_certificate(user, type_host, serial_number=str(i), days=day, ca_name=ca_name))

    hosts = [create_host('host-0.yandex.ru'), create_host('host-1.yandex.ru'), create_host('host-2.yandex.ru')]

    hosts[0].certificates.add(certs[0])
    hosts[0].certificates.add(certs[1])
    hosts[0].certificates.add(certs[2])
    hosts[0].certificates.add(certs[3])
    hosts[0].certificates.add(certs[4])

    hosts[1].certificates.add(certs[2])
    hosts[1].certificates.add(certs[3])

    hosts[2].certificates.add(certs[1])
    hosts[2].certificates.add(certs[4])

    with mock.patch('intranet.crt.utils.startrek.get_component_id') as mocked_func:
        mocked_func.return_value = '1'
        with mock_startrek():
            call_command('expiring_host_certs_notify')

    assert len(mail.outbox) == 3

    assert mail.outbox[0].subject == 'Срок действия сертификата testuser@ld.yandex.ru (GlobalSignProductionCA) подходит к концу (testuser@yandex-team.ru)'
    card_url = '{}/certificates/{}?serial_number={}'.format(settings.CRT_URL, certs[1].id, certs[1].serial_number)
    assert_contains(mail.outbox[0].body, [
        'Срок действия сертификата <a href="{}">1</a> (GlobalSignProductionCA) с Common Name testuser@ld.yandex.ru истекает через 7 дней ({}).'
                    .format(card_url, certs[1].end_date.astimezone().strftime('%d.%m.%Y')),
        '<p>Сертификат действителен для доменов:</p>\nhost-0.yandex.ru\nhost-2.yandex.ru',
        '<p>Мы не нашли сертификатов, выписанных на домены:</p>\nhost-0.yandex.ru',
    ])

    assert mail.outbox[1].subject == 'Срок действия сертификата testuser@ld.yandex.ru (CertumProductionCA) подходит к концу (testuser@yandex-team.ru)'
    card_url = '{}/certificates/{}?serial_number={}'.format(settings.CRT_URL, certs[3].id, certs[3].serial_number)
    assert_contains(mail.outbox[1].body, [
        'Срок действия сертификата <a href="{}">3</a> (CertumProductionCA) с Common Name testuser@ld.yandex.ru истекает через 14 дней ({}).'
                    .format(card_url, certs[3].end_date.astimezone().strftime('%d.%m.%Y')),
        '<p>Сертификат действителен для доменов:</p>\nhost-0.yandex.ru\nhost-1.yandex.ru',
        '<p>Мы не нашли сертификатов, выписанных на домены:</p>\nhost-0.yandex.ru',
    ])

    assert mail.outbox[2].subject == 'Срок действия сертификата testuser@ld.yandex.ru (InternalTestCA) подходит к концу (testuser@yandex-team.ru)'
    card_url = '{}/certificates/{}?serial_number={}'.format(settings.CRT_URL, certs[4].id, certs[4].serial_number)
    assert_contains(mail.outbox[2].body, [
        'Срок действия сертификата <a href="{}">4</a> (InternalTestCA) с Common Name testuser@ld.yandex.ru истекает через 21 день ({}).'
                    .format(card_url, certs[4].end_date.astimezone().strftime('%d.%m.%Y')),
        '<p>Сертификат действителен для доменов:</p>\nhost-0.yandex.ru\nhost-2.yandex.ru',
        '<p>Мы не нашли сертификатов, выписанных на домены:</p>\nhost-0.yandex.ru\nhost-2.yandex.ru',
    ])


@pytest.fixture
def type_host():
    return create_certificate_type(CERT_TYPE.HOST)


def test_not_notify_about_already_requesting(type_host, settings, crt_robot):
    with freeze_time(timezone.now()):
        user = create_user('testuser')
        expiring_cert = create_certificate(
            user,
            type_host,
            serial_number='1',
            days=settings.CRT_CERT_EXPIRATION_NOTIFICATION_DAYS[-1],
            ca_name=CA_NAME.RC_INTERNAL_CA,
        )
        requested_cert = create_certificate(
            user,
            type_host,
            serial_number='2',
            days=365,
            ca_name=CA_NAME.RC_INTERNAL_CA,
            status=CERT_STATUS.NEED_APPROVE,
        )

    host = create_host('host-0.yandex.ru')
    host.certificates.add(expiring_cert)
    host.certificates.add(requested_cert)

    call_command('expiring_host_certs_notify')

    assert len(mail.outbox) == 0


def test_notify_about_need_approve_expiring(type_host, settings, crt_robot):
    with freeze_time(timezone.now()):
        user = create_user('testuser')
        expiring_cert = create_certificate(
            user,
            type_host,
            serial_number='1',
            days=settings.CRT_NOTIFY_ABOUT_NEED_APPROVE_BELOW_DAYS,
            ca_name=CA_NAME.RC_INTERNAL_CA,
        )
        requested_cert = create_certificate(
            user,
            type_host,
            serial_number='2',
            days=365,
            ca_name=CA_NAME.RC_INTERNAL_CA,
            status=CERT_STATUS.NEED_APPROVE,
        )

    hostname = 'host-0.yandex.ru'
    host = create_host(hostname)
    host.certificates.add(expiring_cert)
    host.certificates.add(requested_cert)
    ticket = 'SOME-1'
    create_approve_request(requested_cert, user, 'SOME-1')

    call_command('expiring_host_certs_notify')

    assert len(mail.outbox) == 1
    mail_body = mail.outbox[0].body
    expected_texts = (
        'По следуюшим тикетам требуется согласование:',
        CA_NAME.RC_INTERNAL_CA,
        ticket,
        hostname,
    )
    for text in expected_texts:
        assert text in mail_body
    assert 'Мы не нашли сертификатов, выписанных на домены:' not in mail_body


def test_notify_about_need_approve_expiring_and_just_expiring(type_host, settings, crt_robot):
    with freeze_time(timezone.now()):
        user = create_user('testuser')
        expiring_cert = create_certificate(
            user,
            type_host,
            serial_number='1',
            days=settings.CRT_NOTIFY_ABOUT_NEED_APPROVE_BELOW_DAYS,
            ca_name=CA_NAME.RC_INTERNAL_CA,
        )
        requested_cert = create_certificate(
            user,
            type_host,
            serial_number='2',
            ca_name=CA_NAME.RC_INTERNAL_CA,
            status=CERT_STATUS.NEED_APPROVE,
        )
    requested_cert.end_date = None
    requested_cert.save()

    hostname = 'host-0.yandex.ru'
    host = create_host(hostname)
    host.certificates.add(expiring_cert)
    host.certificates.add(requested_cert)
    hostname_expires = 'host-1.yandex.ru'
    create_host(hostname_expires).certificates.add(expiring_cert)
    ticket = 'SOME-1'
    create_approve_request(requested_cert, user, 'SOME-1')

    call_command('expiring_host_certs_notify')

    assert len(mail.outbox) == 1
    expected_texts = (
        'По следуюшим тикетам требуется согласование:',
        'Мы не нашли сертификатов, выписанных на домены:',
        CA_NAME.RC_INTERNAL_CA,
        ticket,
        hostname,
        hostname_expires,
    )
    mail_body = mail.outbox[0].body
    for text in expected_texts:
        assert text in mail_body


def test_one_responsible_get_one_notify(crt_robot):
    user = create_user('testuser')
    resp_user = create_user('responsible')
    type_host = create_certificate_type(CERT_TYPE.HOST)
    service = AbcService.objects.create(
        external_id=1, created_at=timezone.now(), modified_at=timezone.now()
    )

    CrtGroup.objects.create(
        external_id=1, is_deleted=False, abc_service=service, role_scope=ABC_ADMINISTRATOR_SCOPE
    ).users.add(resp_user)
    CrtGroup.objects.create(
        external_id=2, is_deleted=False, abc_service=service, role_scope=ABC_CERTIFICATE_MANAGER_SCOPE
    ).users.add(resp_user)

    cert = create_certificate(user, type_host, abc_service=service, days=21)
    host = create_host('py.test')
    cert.hosts.add(host)

    call_command('expiring_host_certs_notify')

    assert len(mail.outbox) == 1


@pytest.mark.parametrize('notify_on_expiration, notifications_sent', [
    (True, 1),
    (False, 0)
])
def test_expiring_host_certs_notify_on_expiration_flag(crt_robot, notify_on_expiration, notifications_sent):
    user = create_user('testuser')
    type_host = create_certificate_type(CERT_TYPE.HOST)
    service = AbcService.objects.create(
        external_id=1, created_at=timezone.now(), modified_at=timezone.now()
    )

    cert = create_certificate(user, type_host, abc_service=service, days=21, notify_on_expiration=notify_on_expiration)
    host = create_host('py.test')
    cert.hosts.add(host)

    call_command('expiring_host_certs_notify')
    assert notifications_sent == len(mail.outbox)
