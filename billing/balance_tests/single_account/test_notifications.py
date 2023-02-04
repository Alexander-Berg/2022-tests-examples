# coding=utf-8

import pytest

from butils.decimal_unit import DecimalUnit
from balance.providers.personal_acc_manager import PersonalAccountManager
from balance.actions import single_account
from mailer.balance_mailer import MessageData
from balance.processors import process_payments
from balance.actions.single_account.notifications import construct_spa_services_list

from tests.object_builder import PersonBuilder, ClientBuilder, OebsCashPaymentFactBuilder, ServiceBuilder


pytestmark = [pytest.mark.single_account]


def get_personal_account_for_person(session, client, person):
    single_account.prepare.process_client(client)
    personal_account = PersonalAccountManager(session).\
        for_person(person).\
        for_single_account(client.single_account_number).\
        get(auto_create=False)
    return personal_account


@pytest.mark.email
def test_create_free_funds_message(app, session):
    client = ClientBuilder(with_single_account=True).build(session).obj
    person = PersonBuilder(
        client=client, name='Sponge Bob',
        email='s.bob@nickelodeon.com'
    ).build(session).obj
    personal_account = get_personal_account_for_person(session, client, person)

    # Создаем поступление на ЛС
    amount = DecimalUnit('12345.67', personal_account.currency)
    OebsCashPaymentFactBuilder(amount=amount, operation_type='INSERT', invoice=personal_account).build(session)
    process_payments.handle_invoice(personal_account)

    message = single_account.notifications.create_free_funds_message(session, personal_account, amount)

    assert message.object_id == personal_account.id

    # MessageData.rcpt в тестовых окружениях заменяется на значение из конфига,
    # чтобы письма не отправлялись реальным пользователям,
    # поэтому передачу email можем проверить только до EmailMessage.
    assert message.recepient_address == person.email

    message_data = MessageData.from_message_mapper(message, app.cfg)

    assert message_data.rcpt_name == person.name
    assert message_data.subject == u'На ваш Единый лицевой счёт поступили средства'
    # Если тело письма описано в HTML, оно передается во вложении
    body = message_data.attach_list[0].data
    assert u'12 345,67 руб.' in body


@pytest.mark.email
def test_create_free_funds_message_with_empty_person_email(session):
    client = ClientBuilder(with_single_account=True, email='s.bob@nickelodeon.com').build(session).obj
    person = PersonBuilder(client=client, name='Sponge Bob', email=None).build(session).obj
    personal_account = get_personal_account_for_person(session, client, person)
    amount = DecimalUnit(100, personal_account.currency)
    message = single_account.notifications.create_free_funds_message(session, personal_account, amount)
    assert message.recepient_address == client.email


@pytest.mark.email
def test_create_free_funds_message_with_empty_client_email(session):
    client = ClientBuilder(with_single_account=True, email=None).build(session).obj
    person = PersonBuilder(client=client, name='Sponge Bob', email=None).build(session).obj
    personal_account = get_personal_account_for_person(session, client, person)
    amount = DecimalUnit(100, personal_account.currency)
    message = single_account.notifications.create_free_funds_message(session, personal_account, amount)
    expected_email = session.config.get('SPA_OVERPAYMENT_NOTIFICATION_FALLBACK_EMAIL')
    assert message.recepient_address == expected_email


class TestConstructSPAServicesList(object):
    @pytest.mark.parametrize(
        ['config_value', 'expected_result'],
        [([], ''),
         (False, ''),
         (True, '')]
    )
    def test(self, session, config_value, expected_result):
        session.config.__dict__['SINGLE_ACCOUNT_ENABLED_SERVICES'] = config_value
        assert construct_spa_services_list(session) == expected_result

    def test_not_empty_list(self, session):
        s1 = ServiceBuilder(display_name=u'Сервис 1').build(session).obj
        s2 = ServiceBuilder(display_name=u'Сервис 2').build(session).obj
        session.config.__dict__['SINGLE_ACCOUNT_ENABLED_SERVICES'] = [s1.id, s2.id]
        assert construct_spa_services_list(session) in (u'(Сервис 1, Сервис 2)', u'(Сервис 2, Сервис 1)')
