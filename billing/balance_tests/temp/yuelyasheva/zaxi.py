# -*- coding: utf-8 -*-

import datetime
from decimal import Decimal as D

import pytest

from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils
from btestlib.constants import Services, Currencies, Firms, Managers
from btestlib.data import defaults, person_defaults

# coding: utf-8
__author__ = 'torvald'

from dateutil.relativedelta import relativedelta
import random
import time
import json

from temp.igogor.balance_objects import Contexts
from btestlib.constants import Services, TransactionType, Nds, SpendablePaymentType, PaymentType, OEBSOperationType, Export
from simpleapi.common.payment_methods import VirtualDeposit, VirtualDepositPayout, VirtualRefuel
from simpleapi.data.uids_pool import User
from btestlib import secrets
from balance.balance_steps import simple_api_steps

delta = datetime.timedelta

FIRST_MONTH = utils.Date.first_day_of_month() - relativedelta(months=2)
SECOND_MONTH = FIRST_MONTH + relativedelta(months=1)
NOW = datetime.datetime.now()
ORDER_DT = utils.Date.moscow_offset_dt() - relativedelta(days=1)

MANAGER = Managers.SOME_MANAGER


def create_fake_deposit_payment(context, client_id_taxopark, person_id_taxopark, taxi_contract_id, dt, invoice):
    deposit_row = {'amount': '7000.3',
                   'amount_fee': None,
                   'client_id': 104614837,
                   'contract_id': 866098,
                   'id': '1000000000277260',
                   'internal': None,
                   'invoice_eid': u'ЛСТ-1261502129-1',
                   'partner_id': 104930779,
                   'payment_id': 1033355239,
                   'payment_type': 'deposit',
                   'paysys_type_cc': 'fuel_hold',
                   'person_id': 8861058,
                   'product_id': 509406,
                   'service_id': 124,
                   'service_product_id': None,
                   'transaction_type': 'refund',
                   'yandex_reward': None,
                   'yandex_reward_wo_nds': None}

    simple_api_steps.SimpleApi.create_fake_tpt_row(context, client_id_taxopark,
                                                   person_id_taxopark, taxi_contract_id, dt=dt,
                                                   transaction_type=TransactionType.REFUND,
                                                   amount=D('1000.01'),
                                                   payment_type='deposit',
                                                   tpt_paysys_type_cc='fuel_hold',
                                                   client_id=client_id_taxopark,
                                                   invoice_eid=invoice['external_id'],
                                                   product_id=509406,
                                                   service_id=124)

def create_deposit_payment(context, client_id_taxopark, person_id_taxopark, taxi_contract_id, dt, invoice):
    pass
    # TAXI: virtual::deposit
    # service_product_id = steps.SimpleApi.create_service_product(Services.ZAXI, client_id_taxopark)
    #
    # service_order_id, trust_payment_id, purchase_token, payment_id = \
    #     steps.SimpleApi.create_trust_payment(Services.ZAXI, service_product_id,
    #                                          paymethod=VirtualDeposit(),
    #                                          currency=context.payment_currency,
    #                                          order_dt=ORDER_DT,
    #                                          price=D('7000.30'),
    #                                          developer_payload=json.dumps({'client_id': client_id_taxopark})
    #                                          )
    #
    # steps.CommonPartnerSteps.export_payment(payment_id)

    # Export
    # time.sleep(1)
    # thirdpaty_transactions = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)
    # for transaction in thirdpaty_transactions:
    #     steps.CommonSteps.export(Export.Type.OEBS, Export.Classname.TRANSACTION, transaction['id'])

def create_fake_deposit_payout(context, client_id_taxopark, person_id_taxopark, taxi_contract_id, dt, invoice):

    deposit_payout = {'amount': '1000.01',
              'amount_fee': None,
              'client_id': 104614837,
              'contract_id': 866098,
              'id': '1000000000277320',
              'internal': None,
              'invoice_eid': u'ЛСТ-1261502129-1',
              'partner_id': 104930779,
              'payment_id': 1033355247,
              'payment_type': 'deposit_payout',
              'paysys_type_cc': 'fuel_hold_payment',
              'person_id': 8861058,
              'product_id': 509406,
              'service_id': 124,
              'service_product_id': None,
              'transaction_type': 'payment',
              'yandex_reward': '1000.01',
              'yandex_reward_wo_nds': '847.4661'}

    simple_api_steps.SimpleApi.create_fake_tpt_row(context, client_id_taxopark,
                                                   person_id_taxopark, taxi_contract_id, dt=dt,
                                                   transaction_type=TransactionType.PAYMENT,
                                                   amount=D('300.03'),
                                                   payment_type='deposit_payout',
                                                   paysys_type_cc='fuel_hold_payment',
                                                   client_id=client_id_taxopark,
                                                   invoice_eid=invoice['external_id'],
                                                   product_id=509406,
                                                   service_id=124,
                                                   yandex_reward=D('300.03'),
                                                   yandex_reward_wo_nds=D('113.13')
                                                   )

def create_deposit_payout(context, client_id_taxopark, person_id_taxopark, taxi_contract_id, dt, invoice):
    pass
    # TAXI: virtual::deposit_payout
    # service_product_id = steps.SimpleApi.create_service_product(Services.ZAXI, client_id_taxopark)
    #
    # service_order_id, trust_payment_id, purchase_token, payment_id = \
    #     steps.SimpleApi.create_trust_payment(Services.ZAXI, service_product_id,
    #                                          paymethod=VirtualDepositPayout(),
    #                                          currency=context.payment_currency,
    #                                          order_dt=ORDER_DT,
    #                                          price=D('1000.01'),
    #                                          )
    #
    # steps.CommonPartnerSteps.export_payment(payment_id)

    # Export
    # time.sleep(1)
    # thirdpaty_transactions = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)
    # for transaction in thirdpaty_transactions:
    #     steps.CommonSteps.export(Export.Type.OEBS, Export.Classname.TRANSACTION, transaction['id'])

def create_fake_fuel_fact(context, client_id_taxopark, client_id_fuel_station, person_id_taxopark, person_id_fuel_station, taxi_contract_id, zaxi_contract_id, zaxi_spendable_contract_id, dt, LSD, LSZ):
    a = {'amount': '3000.56',
         'amount_fee': None,
         'client_id': 104930779,
         'contract_id': 866099,
         'id': '1000000000277399',
         'internal': None,
         'invoice_eid': u'ЛСЗ-1261502131-1',
         'partner_id': 104930763,
         'payment_id': 1033355252,
         'payment_type': 'refuel',
         'paysys_type_cc': 'fuel_fact',
         'person_id': 8861058,
         'product_id': None,
         'service_id': 636,
         'service_product_id': None,
         'transaction_type': 'payment',
         'yandex_reward': '3000.56',
         'yandex_reward_wo_nds': '2542.84746'}

    simple_api_steps.SimpleApi.create_fake_tpt_row(Contexts.ZAXI_RU_CONTEXT, client_id_fuel_station,
                                                   person_id_fuel_station, zaxi_contract_id, dt=NOW,
                                                   transaction_type=TransactionType.PAYMENT,
                                                   amount=D('200.02'),
                                                   payment_type='refuel',
                                                   paysys_type_cc='fuel_fact',
                                                   client_id=client_id_taxopark,
                                                   invoice_eid=LSZ['external_id'],
                                                   product_id=None,
                                                   service_id=636,
                                                   yandex_reward=D('200.02'),
                                                   yandex_reward_wo_nds=D('113.13'))

    b = {'amount': '3000.56',
         'amount_fee': None,
         'client_id': 104930779,
         'contract_id': 866098,
         'id': '1000000000277453',
         'internal': 1,
         'invoice_eid': u'ЛСТ-1261502129-1',
         'partner_id': 104930763,
         'payment_id': 1033355252,
         'payment_type': 'refuel',
         'paysys_type_cc': 'fuel_fact',
         'person_id': 8861058,
         'product_id': 509406,
         'service_id': 124,
         'service_product_id': None,
         'transaction_type': 'payment',
         'yandex_reward': '3000.56',
         'yandex_reward_wo_nds': '2542.84746'}

    simple_api_steps.SimpleApi.create_fake_tpt_row(Contexts.ZAXI_RU_CONTEXT, client_id_fuel_station,
                                                   person_id_fuel_station, taxi_contract_id, dt=NOW,
                                                   transaction_type=TransactionType.PAYMENT,
                                                   amount=D('200.02'),
                                                   payment_type='refuel',
                                                   paysys_type_cc='fuel_fact',
                                                   client_id=client_id_taxopark,
                                                   invoice_eid=LSD['external_id'],
                                                   product_id=509406,
                                                   service_id=124,
                                                   yandex_reward=D('200.02'),
                                                   yandex_reward_wo_nds=D('113.13'),
                                                   internal=1
                                                   )

    c = {'amount': '3000.56',
         'amount_fee': None,
         'client_id': 104930779,
         'contract_id': 866100,
         'id': '1000000000277454',
         'internal': None,
         'invoice_eid': None,
         'partner_id': 104930763,
         'payment_id': 1033355252,
         'payment_type': 'refuel',
         'paysys_type_cc': 'yataxi',
         'person_id': 8861061,
         'product_id': None,
         'service_id': 637,
         'service_product_id': None,
         'transaction_type': 'payment',
         'yandex_reward': None,
         'yandex_reward_wo_nds': None}

    simple_api_steps.SimpleApi.create_fake_tpt_row(Contexts.ZAXI_RU_CONTEXT, client_id_fuel_station,
                                                   person_id_fuel_station, zaxi_spendable_contract_id, dt=NOW,
                                                   transaction_type=TransactionType.PAYMENT,
                                                   amount=D('200.02'),
                                                   payment_type='refuel',
                                                   paysys_type_cc='yataxi',
                                                   client_id=client_id_taxopark,
                                                   invoice_eid=None,
                                                   product_id=None,
                                                   service_id=637)

def create_fuel_fact(context, client_id_taxopark, client_id_fuel_station, person_id_taxopark,
                              person_id_fuel_station, taxi_contract_id, zaxi_contract_id, zaxi_spendable_contract_id,
                              dt, LSD, LSZ):
    pass
    # TAXI: virtual::refuel
    # service_product_id = steps.SimpleApi.create_service_product(Services.ZAXI, client_id_fuel_station)
    #
    # service_order_id, trust_payment_id, purchase_token, payment_id = \
    #     steps.SimpleApi.create_trust_payment(Services.ZAXI, service_product_id,
    #                                          paymethod=VirtualRefuel(),
    #                                          currency=context.payment_currency,
    #                                          order_dt=ORDER_DT, user=user,
    #                                          price=D('3000.56'))
    #
    # steps.CommonPartnerSteps.export_payment(payment_id)

    # Export
    # time.sleep(1)
    # thirdpaty_transactions = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)
    # for transaction in thirdpaty_transactions:
    #     steps.CommonSteps.export(Export.Type.OEBS, Export.Classname.TRANSACTION, transaction['id'])


def test_zaxi():
    context = Contexts.TAXI_RU_CONTEXT

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

# закончившийся
#     taxi_contract_id, _ = create_general_taxi_contract(context, client_id, person_id, start_dt=FIRST_MONTH)
# # аннулирован
#     taxi_contract_id, _ = create_general_taxi_contract(context, client_id, person_id, start_dt=FIRST_MONTH+relativedelta(days=5))
# # оферта деактивирован
#     taxi_contract_id, _ = create_general_taxi_contract(context, client_id, person_id, start_dt=FIRST_MONTH+relativedelta(days=5))
# # подписанный
    taxi_contract_id, _ = create_general_taxi_contract(context, client_id, person_id, start_dt=FIRST_MONTH+relativedelta(days=5))
#     # taxi_contract_id, _ = create_general_taxi_contract(context, client_id, person_id, start_dt=FIRST_MONTH+relativedelta(days=5))

    zaxi_contract_id, _ = create_zaxi_contract(context, client_id, person_id, SECOND_MONTH, taxi_contract_id)

    # zaxi_spendable_contract_id, _ = create_spandable_zaxi_contract(context, client_id, person_id, context.nds,
    #                                                                start_dt=FIRST_MONTH)
    pass


def create_general_taxi_contract(context, client_id, person_id, start_dt):
    contract_id, external_id = steps.ContractSteps.create_contract('taxi_postpay', {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'DT': start_dt,
        'IS_SIGNED': None,
        'PARTNER_COMMISSION_PCT2': context.commission_pct,
        'SERVICES': [service.id for service in context.services],
        'FIRM': context.firm.id,
        'CURRENCY': context.currency.num_code,
        'COUNTRY': context.region.id
    })
    return contract_id, external_id


def create_zaxi_contract(context, client_id, person_id, start_dt, taxi_contract_id):
    contract_id, external_id = steps.ContractSteps.create_contract('taxi_postpay', {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'DT': start_dt,
        'IS_SIGNED': start_dt.isoformat(),
        'PARTNER_COMMISSION_PCT2': context.commission_pct,
        'SERVICES': [Services.ZAXI.id],
        'FIRM': Firms.GAS_STATIONS_124.id,
        'LINK_CONTRACT_ID': taxi_contract_id,
        'CURRENCY': context.currency.num_code,
        'COUNTRY': context.region.id
    })
    return contract_id, external_id


def create_spandable_zaxi_contract(context, client_id, spendable_person_id, nds, start_dt, payment_type=SpendablePaymentType.MONTHLY,
                                   remove_params=None):

    spendable_contract_id, spendable_external_id = steps.ContractSteps.create_contract('spendable_taxi_donate', {
        'CLIENT_ID': client_id,
        'PERSON_ID': spendable_person_id,
        'MANAGER_CODE': Managers.SOME_MANAGER.code,
        'FIRM': context.firm.id,
        'DT': start_dt,
        'IS_SIGNED': start_dt.isoformat(),
        'NDS': str(Nds.get(nds)),
        'PAYMENT_TYPE': payment_type,
        # 'LINK_CONTRACT_ID': contract_id,
        'CURRENCY': context.currency.iso_num_code,
        'SERVICES': [Services.ZAXI_SPENDABLE.id],
    }, remove_params=remove_params)

    return spendable_contract_id, spendable_external_id


def create_client_and_contract(context, nds, start_dt=FIRST_MONTH, payment_type=SpendablePaymentType.MONTHLY,
                               remove_params=None):
    client_id, contract_id = create_general_client_and_contract(context, start_dt)

    spendable_person_id = steps.PersonSteps.create(client_id, context.person_type.code, {'is-partner': '1'})
    spendable_contract_id, _ = steps.ContractSteps.create_contract('spendable_taxi_donate', {
        'CLIENT_ID': client_id,
        'PERSON_ID': spendable_person_id,
        'FIRM': context.firm.id,
        'DT': start_dt,
        'IS_SIGNED': start_dt.isoformat(),
        'NDS': str(Nds.get(nds)),
        'PAYMENT_TYPE': payment_type,
        'LINK_CONTRACT_ID': contract_id,
        'CURRENCY': context.currency.iso_num_code,
        'SERVICES': [Services.ZAXI.id],
    }, remove_params=remove_params)

    return client_id, spendable_person_id, spendable_contract_id


if __name__ == '__main__':

    offers = [('ph', Firms.JAMS_120.id)]
    # offers = [('sw_ytph', Firms.SERVICES_AG_16.id)]

    start_dt = datetime.datetime(2018, 5, 10)
    # payments = [(D('250.13'), stat_dt, 'USD'),
    #             (D('251.13'), stat_dt, 'USD')]
    # charges = [(100, 200, 300000), (200, 300, 400000)]

    searches = [
        # search_id
        utils.aDict({'payments': [
            (D('101.13'), start_dt - delta(days=18), 'RUB', 'wallet'),
            (D('100.13'), start_dt, 'RUB', 'wallet'),
            (D('102.13'), start_dt + delta(days=1), 'RUB', 'wallet')
        ]
        })
    ]

    client_id = steps.ClientSteps.create()
    # client_id = 56510589

    contracts = {}
    persons = {}
    for person_type, firm_id in offers:
        person_id = steps.PersonSteps.create(client_id, person_type,
                                             params={'is-partner': '1',
                                                     'fname': 'person_{}'.format(
                                                         str(datetime.datetime.now().isoformat()))},
                                             inn_type=person_defaults.InnType.RANDOM)
        persons[person_type] = person_id

        contract_id, contract_eid = steps.ContractSteps.create_offer({
            'client_id': client_id,
            'person_id': person_id,
            'manager_uid': '1120000000047228',  # MANAGER.uid,
            'personal_account': 1,
            'currency': Currencies.RUB.char_code,
            'firm_id': firm_id,
            'services': [Services.TOLOKA.id],
            'payment_term': 10,
            'start_dt': datetime.datetime(2018, 1, 1),
            'nds': 18,
            'link_contract_id': contracts.get('ph', None)
        })

        contracts[person_type] = contract_id
    # contracts = {'ph': 498647, 'sw_ytph': 498648}

    for search in searches:

        # search_id = db.balance().execute("SELECT s_test_place_search_id.nextval search FROM dual")[0]['search']

        # steps.api.medium().CreateOrUpdatePlace(defaults.PASSPORT_UID,
        #                                        {'SearchID': search_id, 'ClientID': client_id, 'Type': 20,
        #                                         'URL': "pytest.com"})

        # Вставка данных по платежам напрямую:
        for price, dt, currency, acc_type in search.payments:
            # Фейковые оплаты из Балалайки:
            query = 'SELECT S_ZEN_TRANSACTIONS_TEST.nextval FROM dual'
            result = db.balance().execute(query)
            # max_transaction_id = result[0]['val']
            # transaction_id = max_transaction_id + 1 if max_transaction_id else 42
            transaction_id = result[0]['nextval']

            balalayka_stat_params = {'price': price,
                                     'dt': dt,
                                     'client_id': client_id,
                                     'transaction_id': transaction_id,
                                     'currency': currency,
                                     'acc_type': acc_type,
                                     'service_id': Services.TOLOKA.id}

            query = "insert into t_partner_balalayka_stat values(:price, :dt, :client_id, :transaction_id, :currency, :acc_type, :service_id)"
            db.balance().execute(query, balalayka_stat_params)

            query = "insert into t_export (classname, object_id, type) values ('BalalaykaPayment', :transaction_id, 'THIRDPARTY_TRANS')"
            db.balance().execute(query, balalayka_stat_params)

            # Реальные платежи из Балалайки:
            # insert_payment_service(price=int((price * D('100')).quantize(D('1'))),
            #                        dt=dt,
            #                        client_id=client_id,
            #                        currency_id=643,
            #                        service_id=2,
            #                        firm=11,
            #                        acc_type=acc_type)
            #
            # steps.api.test_balance().GetPartnerCompletions({'start_dt': dt,
            #                                                 'end_dt': datetime.datetime.now(),
            #                                                 'completion_source': 'toloka'})

            steps.CommonSteps.export('THIRDPARTY_TRANS', 'BalalaykaPayment', transaction_id)

        pass
