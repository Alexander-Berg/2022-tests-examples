# -*- coding: utf-8 -*-

import datetime as d

import hamcrest
import pytest
from dateutil.relativedelta import relativedelta

from balance import balance_db as db
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils, reporter
from btestlib.constants import ContractCommissionType, ContractPaymentType, Currencies, Services, Managers
from btestlib.data import partner_contexts as pc
from btestlib.data.defaults import Date

pytestmark = [reporter.feature(Features.EMAIL), pytest.mark.tickets('BALANCE-28449,BALANCE-29036')]

manager_email = 'nugzar@yandex-team.ru'  # Текущий email менеджера

dt = d.datetime.now() - relativedelta(months=2)

# таблица рассылок с условиями на вики https://wiki.yandex-team.ru/Balance/Уведомления-по-договорам/
@pytest.mark.parametrize('context, email_list',
                         [
                             (pc.ADFOX_RU_CONTEXT, [manager_email, "docs-project@yandex-team.ru"]),
                             (pc.BUSES_RU_CONTEXT, [manager_email, "b2b-bus@yandex-team.ru"]),
                             (pc.TICKETS_118_CONTEXT, [manager_email, 'docs-media@yandex-team.ru', 'dpasik@yandex-team.ru',
                               'kate-parf@yandex-team.ru']),
                             (pc.EVENTS_TICKETS_CONTEXT, [manager_email, 'docs-media@yandex-team.ru', 'dpasik@yandex-team.ru',
                               'kate-parf@yandex-team.ru']),
                             (pc.TELEMEDICINE_CONTEXT, [manager_email, 'mikhail@yandex-team.ru', 'emilab@yandex-team.ru']),
                             (pc.CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED, [manager_email, 'docs_alarm@yandex-team.ru']),
                             (pc.CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP, [manager_email, 'docs_alarm@yandex-team.ru']),
                             (pc.TAXI_RU_CONTEXT, [manager_email, 'taxi-netoriginala@yandex-team.ru']),
                             (pc.CLOUD_RU_CONTEXT, [manager_email, 'docs-project@yandex-team.ru', 'cloud_ops@yandex-team.ru']),
                             # (pc.GAS_STATION_RU_CONTEXT, [manager_email, 'docs-project@yandex-team.ru']),
                             (pc.STATION_PAYMENTS_CONTEXT, [manager_email, 'docs-project@yandex-team.ru'])
                          ], ids=lambda context, email_list: context.name)
def test_email_message_recipients_no_docs_received_partner(context, email_list):
    contract_id = get_partner_contract_id(context)

    steps.CommonSteps.export('CONTRACT_NOTIFY', 'Contract', contract_id)

    recipient_list = get_recipient_list(contract_id)

    utils.check_that(set(recipient_list), hamcrest.equal_to(set(email_list)),
                     step=u'Проверяем список адресатов рассылки')


@pytest.mark.parametrize('services, email_list', [
    ([Services.TAXI_BREND], [manager_email, 'forleasing@yandex-team.ru']),
    # ([Services.TOURS], [manager_email, 'partner-travel@yandex-team.ru']),
], ids=lambda services, email_list: ' '.join(str(service) for service in services))
def test_email_message_recipients_no_docs_received(services, email_list):
    contract_id = get_contract_id(services)

    steps.CommonSteps.export('CONTRACT_NOTIFY', 'Contract', contract_id)

    recipient_list = get_recipient_list(contract_id)

    utils.check_that(set(recipient_list), hamcrest.equal_to(set(email_list)),
                     step=u'Проверяем список адресатов рассылки')


def get_partner_contract_id(context):
    params = {'start_dt': dt, 'manager_uid': Managers.PERANIDZE.uid}
    if context == pc.ZAXI_RU_CONTEXT:
        _, _, taxi_contract_id, _ = steps.ContractSteps.create_partner_contract(pc.TAXI_RU_CONTEXT)
        params.update({'link_contract_id': taxi_contract_id})
    _, _, contract_id, _ = steps.ContractSteps.create_partner_contract(context, is_postpay=0, unsigned=True,
                                                                       additional_params=params)
    return contract_id


def get_contract_id(services):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')

    contract_params = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'PAYMENT_TYPE': ContractPaymentType.PREPAY,
        'SERVICES': [service.id for service in services],
        'DT': utils.Date.to_iso(utils.Date.nullify_time_of_date(dt)),
        'FINISH_DT': Date.HALF_YEAR_AFTER_TODAY_ISO,
        'CURRENCY': Currencies.RUB.num_code
    }
    contract_comission = ContractCommissionType.COMMISS
    contract_id, _ = steps.ContractSteps.create_contract_new(contract_comission, contract_params)
    return contract_id


def get_recipient_list(contract_id):
    with reporter.step(u'Получаем список адресатов рассылки'):
        query = "SELECT RECEPIENT_ADDRESS FROM T_MESSAGE WHERE OBJECT_ID =:item AND DT >= trunc(SYSDATE)"
        recipient_list = [recipient['recepient_address'] for recipient in
                          db.balance().execute(query, {'item': contract_id})]
    return recipient_list
