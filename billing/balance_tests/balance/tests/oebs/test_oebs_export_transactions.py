# coding: utf-8
__author__ = 'a-vasin'

import json

from datetime import datetime
from decimal import Decimal

import pytest
from dateutil.relativedelta import relativedelta
from enum import Enum
from hamcrest import empty, has_length

import balance.balance_db as db
import balance.balance_steps as steps
import btestlib.reporter as reporter
from balance.features import Features
from btestlib import utils
from btestlib.constants import TransactionType, Firms, Services, Export, FoodProductType
from btestlib.data.person_defaults import InnType
from btestlib.matchers import equal_to_casted_dict
from export_commons import Locators, read_attr_values
from btestlib.data.partner_contexts import FOOD_RESTAURANT_CONTEXT

try:
    from typing import List, Dict, Any
except ImportError:
    pass

pytestmark = [reporter.feature(Features.OEBS, Features.PAYMENT, Features.REFUND, Features.CORRECTION),
              pytest.mark.docpath('https://wiki.yandex-team.ru/balance/docs/process/oebs'),
              pytest.mark.ticket('BALANCE-25180')
              ]


class ATTRS(object):
    class TRANSACTION(Enum):
        external_id = \
            Locators(balance=lambda b: b['t_contract2.external_id'],
                     oebs=lambda o: o['xxap_agent_reward_data.k_alias'])

        id = \
            Locators(balance=lambda b: b['t_thirdparty.id'],
                     oebs=lambda o: o['xxap_agent_reward_data.billing_line_id'])

        actual_dt = \
            Locators(balance=lambda b: utils.Date.nullify_time_of_date(datetime.now()),
                     oebs=lambda o: o['xxap_agent_reward_data.actual_date'])

        initial_dt = \
            Locators(balance=lambda b: utils.Date.nullify_time_of_date(b['t_thirdparty.dt']),
                     oebs=lambda o: o['xxap_agent_reward_data.initial_date'])

        transaction_type = \
            Locators(balance=lambda b: 'REFUND' if b['t_thirdparty.transaction_type'] == TransactionType.REFUND.name
            else 'PAYMENT',
                     oebs=lambda o: o['xxap_agent_reward_data.transaction_type'])

        amount = \
            Locators(balance=lambda b: get_money_value(b['t_thirdparty.amount']),
                     oebs=lambda o: o['xxap_agent_reward_data.row_amount'])

        transaction_curr_code = \
            Locators(balance=lambda b: b['t_thirdparty.iso_currency'],
                     oebs=lambda o: o['xxap_agent_reward_data.payment_curr_code'])

        yandex_curr_code = \
            Locators(balance=lambda b: b['t_thirdparty.iso_currency'],
                     oebs=lambda o: o['xxap_agent_reward_data.yandex_curr_code'])

        yandex_reward = \
            Locators(balance=lambda b: get_money_value(b['t_thirdparty.yandex_reward']),
                     oebs=lambda o: o['xxap_agent_reward_data.yandex_reward_amt'])

        org_id = \
            Locators(balance=lambda b: b['t_thirdparty.oebs_org_id'],
                     oebs=lambda o: o['xxap_agent_reward_data.org_id'])

        paysys_reward = \
            Locators(balance=lambda b: get_money_value(b['t_thirdparty.row_paysys_commission_sum']),
                     oebs=lambda o: o['xxap_agent_reward_data.paysys_reward_amt'])


TRANSACTIONS_TABLE = 'T_THIRDPARTY_TRANSACTIONS'
CORRECTIONS_TABLE = 'T_THIRDPARTY_CORRECTIONS'

TABLE_TO_QUEUE = {
    TRANSACTIONS_TABLE: Export.Classname.TRANSACTION,
    CORRECTIONS_TABLE: Export.Classname.CORRECTION
}


def test_payment(switch_to_pg):
    contract_id, _, _, payment_id = create_payment(service_fee=0)
    check_attrs(contract_id, payment_id, TRANSACTIONS_TABLE)


# Больше не модифицируем технического партнёра в БД.
# Потому можем теперь просто разбирать платежи на настоящий технический договор
# @pytest.mark.no_parallel('events_tickets_new', write=False)
def test_internal_payment(switch_to_pg):
    contract_id = get_tech_contract()
    _, _, _, payment_id = create_payment(service_fee=1)

    transaction_id = export_and_get_thirdparty_data(contract_id, payment_id, TRANSACTIONS_TABLE)['id']
    oebs_row = get_oebs_transaction(transaction_id)
    utils.check_that(oebs_row, empty(), u'Проверяем, что транзакция не уехала в OEBS')


def test_refund(switch_to_pg):
    contract_id, service_order_id, trust_payment_id, payment_id = create_payment(service_fee=0, exportable=False)
    create_refund(service_order_id, trust_payment_id)
    check_attrs(contract_id, payment_id, TRANSACTIONS_TABLE)


# Больше не модифицируем технического партнёра в БД.
# Потому можем теперь просто разбирать платежи на настоящий технический договор
# @pytest.mark.no_parallel('events_tickets_new', write=False)
def test_internal_refund(switch_to_pg):
    contract_id = get_tech_contract()
    _, service_order_id, trust_payment_id, payment_id = create_payment(service_fee=1, exportable=False)
    create_refund(service_order_id, trust_payment_id)

    transaction_id = export_and_get_thirdparty_data(contract_id, payment_id, TRANSACTIONS_TABLE)['id']
    oebs_row = get_oebs_transaction(transaction_id)
    utils.check_that(oebs_row, empty(), u'Проверяем, что транзакция не уехала в OEBS')


def test_correction_insertion(switch_to_pg):
    correction_id = get_new_correction_id()
    insert_new_correction(correction_id)
    export_rows = get_correction_export_rows(correction_id)
    utils.check_that(export_rows, has_length(1), u'Проверяем, что появилась запись в T_EXPORT')


# ------------------------------------------------
# Utils
def get_new_correction_id():
    with reporter.step(u"Получаем ID для вставки транзакции"):
        query = "SELECT s_request_order_id.nextval nextval FROM dual"
        return db.balance().execute(query)[0]['nextval']


def insert_new_correction(correction_id):
    with reporter.step(u"Вставляем корректировку с ID: {}".format(correction_id)):
        query = "INSERT INTO T_THIRDPARTY_CORRECTIONS(ID, AUTO, STARTRACK_ID, CONTRACT_ID, TRANSACTION_TYPE, SERVICE_ID) VALUES (:correction_id, 0, 'PAYSUP-208406', 183916, 'payment', 124)"
        params = {'correction_id': correction_id}
        db.balance().execute(query, params)


def get_correction_export_rows(correction_id):
    with reporter.step(u"Получаем строки из T_EXPORT для корректировки: {}".format(correction_id)):
        query = "SELECT * FROM T_EXPORT WHERE TYPE='OEBS' AND CLASSNAME='ThirdPartyCorrection' AND OBJECT_ID=:object_id"
        params = {'object_id': correction_id}
        return db.balance().execute(query, params)


@steps.ConfigSteps.temporary_changer('NETTING_CONFIG', 'value_json')
def change_netting_config(config_json):  # type: (str) -> str
    config_list = json.loads(config_json)  # type: List[Dict[str, Any]]
    for section in config_list:
        if section['service_id'] == 629:
            del section['stop_netting_after_date']

    return json.dumps(config_list)


def create_correction():
    context = FOOD_RESTAURANT_CONTEXT
    completion_dt = utils.Date.nullify_time_of_date(datetime.now()) - relativedelta(days=2)
    netting_dt = completion_dt + relativedelta(days=1)
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context, is_offer=1,
        additional_params={'start_dt': completion_dt})
    steps.ExportSteps.export_oebs(client_id=client_id, person_id=person_id, contract_id=contract_id)

    steps.PartnerSteps.create_fake_product_completion(
        completion_dt.replace(hour=23, minute=59, second=59),
        client_id=client_id,
        service_id=context.commission_service.id, service_order_id=0,
        commission_sum=100,
        type=FoodProductType.GOODS
    )

    with change_netting_config():
        # запускаем взаимозачет
        steps.TaxiSteps.process_netting(contract_id, netting_dt, forced=True)
    return contract_id


def get_tech_contract():
    client_id, _, contract_id = steps.CommonPartnerSteps.get_tech_ids(Services.EVENTS_TICKETS_NEW)

    # NOTE: считаем, что реальный технический клиент, договор и его доп.соглашения выгружены.
    # steps.ExportSteps.export_oebs(client_id=client_id, contract_id=contract_id)
    return contract_id


def create_payment(service_fee, exportable=True):
    with reporter.step(u'Создаем договор для клиента-партнера'):
        # создаем клиента-партнера
        partner_id = steps.SimpleApi.create_partner(Services.EVENTS_TICKETS_NEW)
        service_product_id = steps.SimpleApi.create_service_product(Services.EVENTS_TICKETS_NEW, partner_id,
                                                                    service_fee=service_fee)

        person_id = steps.PersonSteps.create(partner_id, 'ur', inn_type=InnType.RANDOM)

        # создаем договор для клиента-партнера
        contract_id, _ = steps.ContractSteps.create_contract('events_tickets2', {
            'CLIENT_ID': partner_id,
            'PERSON_ID': person_id,
            'FIRM': Firms.MEDIASERVICES_121.id
        })

    service_order_id, trust_payment_id, _, payment_id = \
        steps.SimpleApi.create_trust_payment(Services.EVENTS_TICKETS_NEW, service_product_id)

    steps.ExportSteps.export_oebs(client_id=partner_id, person_id=person_id, contract_id=contract_id)

    if exportable:
        steps.CommonPartnerSteps.export_payment(payment_id)

    return contract_id, service_order_id, trust_payment_id, payment_id


def create_refund(service_order_id, trust_payment_id):
    _, refund_id = steps.SimpleApi.create_refund(Services.EVENTS_TICKETS_NEW, service_order_id, trust_payment_id)

    steps.CommonPartnerSteps.export_payment(refund_id)


def get_balance_transaction_data(contract_id, payment_id, table_name):
    # t_contract2
    query = "SELECT * FROM t_contract2 WHERE id = :contract_id"
    transaction_data = db.balance().execute(query, {'contract_id': contract_id}, single_row=True)
    transaction_data = utils.add_key_prefix(transaction_data, 't_contract2.')

    # t_thirdparty_transactions / t_thirdparty_corrections
    t_thirdparty = export_and_get_thirdparty_data(contract_id, payment_id, table_name)
    if t_thirdparty['transaction_type'] == TransactionType.REFUND.name:
        t_thirdparty['amount'] = -Decimal(t_thirdparty['amount'])
    transaction_data.update(utils.add_key_prefix(t_thirdparty, 't_thirdparty.'))

    return transaction_data


def get_oebs_transaction_data(balance_data):
    transaction_id = balance_data['t_thirdparty.id']

    # xxap_agent_reward_data
    transaction_data = get_oebs_transaction(transaction_id)
    transaction_data = utils.add_key_prefix(transaction_data, 'xxap_agent_reward_data.')

    return transaction_data


def export_and_get_thirdparty_data(contract_id, payment_id, table_name):

    if payment_id:
        query = "SELECT * FROM {} WHERE payment_id = :payment_id".format(table_name)
        thirdparty_data = db.balance().execute(query, {'payment_id': payment_id})[0]

    else:
        query = "SELECT * FROM {} WHERE contract_id = :contract_id".format(table_name)
        thirdparty_data = db.balance().execute(query, {'contract_id': contract_id})
        thirdparty_data = thirdparty_data[0]

    # # t_thirdparty_transactions / t_thirdparty_corrections
    # query = "SELECT * FROM {} WHERE contract_id = :contract_id".format(table_name)
    # thirdparty_data = db.balance().execute(query, {'contract_id': contract_id})
    #
    # if payment_id:
    #     thirdparty_data = [row for row in thirdparty_data if row['payment_id'] == payment_id]
    # thirdparty_data = thirdparty_data[0]

    # blubimov лучше выгружать через ExportSteps.export_oebs
    steps.CommonSteps.export(Export.Type.OEBS, TABLE_TO_QUEUE[table_name], thirdparty_data['id'])
    return thirdparty_data


def get_oebs_transaction(transaction_id):
    query = "SELECT * FROM xxap_agent_reward_data WHERE billing_line_id=:transaction_id"
    return db.oebs().execute_oebs(Firms.YANDEX_1.id, query, {'transaction_id': transaction_id}, single_row=True)


def check_attrs(contract_id, payment_id, table_name):
    with reporter.step(u'Считываем данные из баланса'):
        balance_data = get_balance_transaction_data(contract_id, payment_id, table_name)
    with reporter.step(u'Считываем данные из ОЕБС'):
        oebs_data = get_oebs_transaction_data(balance_data)

    balance_values, oebs_values = read_attr_values(ATTRS.TRANSACTION, balance_data, oebs_data)

    utils.check_that(oebs_values, equal_to_casted_dict(balance_values),
                     step=u'Проверяем корректность общих данных счета в ОЕБС')


def get_money_value(value):
    if value is None:
        return 0
    return value
