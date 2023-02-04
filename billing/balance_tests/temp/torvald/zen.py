# -*- coding: utf-8 -*-

import datetime
from decimal import Decimal as D

from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils
from btestlib.constants import Services, Currencies, Firms, Managers
from btestlib.data import defaults, person_defaults

# coding: utf-8
__author__ = 'chihiro'
import balalayka.balalayka_mongo as mongo
from bson.dbref import DBRef
import random

delta = datetime.timedelta


def insert_payment_service(price, dt, client_id, currency_id=643, service_id=5, firm=None, acc_type='wallet'):
    # wallet - ЯД
    # price в копейках int
    # в проде платежи в ЯД только в рублях, т.е. currency_id=643, но у Толоки может буть указана волюта USD TOLOKA-7862
    payment_data = mongo.balalayka().payment_service.find_one(
        {
            "service_id": service_id,
            "t_acc_type": acc_type,
            "status": 7
        })
    date = dt.strftime("%Y-%m-%dT%H:%M:%S.000Z")
    payment_data.update(
        {
            "doc_number": random.randint(100000, 999999),
            "summ": price,
            "currency_id": currency_id,
            "firm": DBRef("firm", firm or payment_data['firm'].id),
            "service_id": service_id,
            "metadata": "{\"client_id\":" + str(client_id) + "}",  # сюда номер плательщика
            "dt": datetime.datetime.strptime(date, "%Y-%m-%dT%H:%M:%S.000Z"),  # дата должна быть ISODate
            "update_dt": datetime.datetime.strptime(date, "%Y-%m-%dT%H:%M:%S.000Z")
        })
    if acc_type == 'wallet':
        payment_data['payment_system_answer']['processedDT'] = date.replace('.000Z', ".575+03:00")
    else:
        payment_data["payment_system_answer"]["time_processed"] = date
    payment_data.pop('_id')
    mongo.balalayka().payment_service.insert(payment_data)


MANAGER = Managers.SOME_MANAGER

if __name__ == '__main__':

    query = 'SELECT S_ZEN_TRANSACTIONS_TEST.nextval FROM dual'
    result = db.balance().execute(query)
    # max_transaction_id = result[0]['val']
    # transaction_id = max_transaction_id + 1 if max_transaction_id else 42
    transaction_id = result[0]['nextval']

    insert_payment_service(price=1234,
                           dt=datetime.datetime(2018, 6, 3),
                           client_id=82051157,
                           currency_id=643,
                           service_id=5,
                           firm=34)

    # insert_payment_service(price=1234,
    #                        dt=datetime.datetime(2018, 6, 4),
    #                        client_id=82051157,
    #                        currency_id=643,
    #                        service_id=2,
    #                        firm=11)  # u'ООО «Яндекс.Пробки»'
    #
    # insert_payment_service(price=1234,
    #                        dt=datetime.datetime(2018, 6, 4),
    #                        client_id=82051157,
    #                        currency_id=840,
    #                        service_id=2,
    #                        firm=16)  # u'Yandex Services AG'



    steps.api.test_balance().GetPartnerCompletions({'start_dt': datetime.datetime(2018, 6, 1),
                                                    'end_dt': datetime.datetime.now(),
                                                    'completion_source': 'zen'})

    # offers = [('ph', Firms.ZEN_28.id), ('sw_ytph', Firms.SERVICES_AG_16.id)]
    offers = [('sw_ytph', Firms.SERVICES_AG_16.id)]

    start_dt = datetime.datetime(2018, 4, 10)
    # payments = [(D('250.13'), stat_dt, 'USD'),
    #             (D('251.13'), stat_dt, 'USD')]
    # charges = [(100, 200, 300000), (200, 300, 400000)]

    searches = [
        # search_id
        utils.aDict({'payments': [
            (D('101.13'), start_dt - delta(days=5), 'RUB', 'wallet'),
            (D('100.13'), start_dt - delta(days=4), 'RUB', 'wallet'),
            (D('102.13'), start_dt - delta(days=3), 'RUB', 'wallet')
        ],
            'charges': [
                (start_dt - delta(days=1), 102, 103, 77.1),
                (start_dt, 105, 106, 88.8),
                # (start_dt + delta(days=1), 108, 109, 11.1),
                # (start_dt + delta(days=60), 111, 112, 1000013)
            ]
        }),
        #
        # utils.aDict({'payments': [
        #     (D('201.13'), start_dt, 'RUB', 'wallet'),
        #     (D('202.13'), start_dt + delta(days=30), 'RUB', 'wallet')
        #     ],
        #     'charges': [
        #         (start_dt - delta(days=180), 202, 203, 99.9),
        #         (start_dt, 205, 206, 100.1),
        #         (start_dt + delta(days=30), 208, 209, 7.99),
        #         # (start_dt + delta(days=60), 211, 212, 2000013)
        #     ]
        # }),
    ]

    # searches = [
    #     # search_id
    #     utils.aDict({
    #         'payments': [
    #             # RUB
    #             # (D('100.13'), start_dt, 'RUB'),
    #             # (D('101.13'), start_dt + delta(days=30), 'RUB'),
    #             (D('102.13'), start_dt + delta(days=60), 'RUB'),
    #             # USD
    #             (D('300.13'), start_dt, 'USD'),
    #             (D('301.13'), start_dt + delta(days=30), 'USD'),
    #             # (D('302.13'), start_dt + delta(days=60), 'USD')
    #         ],
    #         'charges': [(start_dt - delta(days=180), 102, 103, 1000004),
    #                     (start_dt, 105, 106, 1000007),
    #                     (start_dt + delta(days=30), 108, 109, 1000010),
    #                     (start_dt + delta(days=60), 111, 112, 1000013)]
    #     }),
    #     #
    #     utils.aDict({
    #         'payments': [
    #             # RUB
    #             (D('200.13'), start_dt, 'RUB'),
    #             # (D('201.13'), start_dt + delta(days=30), 'RUB'),
    #             (D('202.13'), start_dt + delta(days=60), 'RUB'),
    #             # USD
    #             # (D('400.13'), start_dt, 'USD'),
    #             (D('401.13'), start_dt + delta(days=30), 'USD'),
    #             # (D('402.13'), start_dt + delta(days=60), 'USD')
    #         ],
    #         'charges': [(start_dt - delta(days=180), 202, 203, 2000004),
    #                     (start_dt, 205, 206, 2000007),
    #                     (start_dt + delta(days=30), 208, 209, 2000010),
    #                     (start_dt + delta(days=60), 211, 212, 2000013)]
    #     }),
    # ]

    client_id = 81401179 or steps.ClientSteps.create()
    # client_id = 56510589

    # contracts = {}
    # persons = {}
    # for person_type, firm_id in offers:
    #     person_id = steps.PersonSteps.create(client_id, person_type,
    #                                          params={'is-partner': '1',
    #                                                  'fname': 'person_{}'.format(
    #                                                      str(datetime.datetime.now().isoformat()))},
    #                                          inn_type=person_defaults.InnType.RANDOM)
    #     persons[person_type] = person_id
    #
    #     contract_id, contract_eid = steps.ContractSteps.create_offer({
    #         'client_id': client_id,
    #         'person_id': person_id,
    #         'manager_uid': MANAGER.uid,
    #         'personal_account': 1,
    #         'currency': Currencies.RUB.char_code,
    #         'firm_id': firm_id,
    #         'services': [Services.ZEN.id],
    #         'payment_term': 10,
    #         'start_dt': datetime.datetime(2017, 8, 1),
    #         'nds': 18,
    #         'link_contract_id': contracts.get('ph', None)
    #     })
    #
    #     contracts[person_type] = contract_id
    contracts = {'ph': 562786, 'sw_ytph': 562787}

    for search in searches:

        search_id = db.balance().execute("SELECT s_test_place_search_id.nextval search FROM dual")[0]['search']

        steps.api.medium().CreateOrUpdatePlace(defaults.PASSPORT_UID,
                                               {'SearchID': search_id, 'ClientID': client_id, 'Type': 20,
                                                'URL': "pytest.com"})

        # Вставка данных по платежам напрямую:
        for price, dt, currency, acc_type in search.payments:
            # Фейковые оплаты из Балалайки:
            query = 'SELECT S_ZEN_TRANSACTIONS_TEST.nextval FROM dual'
            result = db.balance().execute(query)
            # max_transaction_id = result[0]['val']
            # transaction_id = max_transaction_id + 1 if max_transaction_id else 42
            transaction_id = result[0]['nextval']

            zen_stat_params = {'price': price,
                               'dt': dt,
                               'client_id': client_id,
                               'transaction_id': transaction_id,
                               'currency': currency,
                               'acc_type': acc_type,
                               'service_id': Services.ZEN.id}

            # query = "insert into t_partner_zen_stat values(:price, :dt, :client_id, :transaction_id, :currency, :acc_type)"
            # db.balance().execute(query, zen_stat_params)

            query = "insert into t_partner_balalayka_stat values(:price, :dt, :client_id, :transaction_id, :currency, :acc_type, :service_id)"
            db.balance().execute(query, zen_stat_params)

            query = "insert into t_export (classname, object_id, type) values ('BalalaykaPayment', :transaction_id, 'THIRDPARTY_TRANS')"
            db.balance().execute(query, zen_stat_params)

            # Реальные платежи из Балалайки:
            # insert_payment_service(price=int((price * D('100')).quantize(D('1'))),
            #                        dt=dt,
            #                        client_id=client_id,
            #                        currency_id=643,
            #                        service_id=5,
            #                        acc_type=acc_type)
            #
            # steps.api.test_balance().GetPartnerCompletions({'start_dt': dt,
            #                                                 'end_dt': datetime.datetime.now(),
            #                                                 'completion_source': 'zen'})

            steps.CommonSteps.export('THIRDPARTY_TRANS', 'BalalaykaPayment', transaction_id)

        # Вставка данных по начислениям

        for charge_dt, clicks, shows, bucks in search.charges:
            query = "INSERT INTO bo.T_PARTNER_DSP_STAT VALUES " \
                    "(:dt, 216651, 1, 1, 0, :amount, :amount, 0, 0, 0, 0, 0, 0, :search_id)"
            db.balance().execute(query,
                                 {'dt': charge_dt, 'search_id': search_id, 'amount': bucks})

    steps.CommonPartnerSteps.generate_partner_acts_fair(contracts['sw_ytph'], start_dt)

    # --------------------------------------------------------------------------------------------------

    searches = [
        # search_id
        utils.aDict({'payments': [
            (D('102.13'), start_dt + delta(days=1), 'RUB', 'wallet'),
            (D('102.13'), start_dt + delta(days=2), 'RUB', 'wallet')
        ],
            'charges': [
                (start_dt + delta(days=1), 102, 103, 77.1),
                (start_dt + delta(days=2), 108, 109, 11.1),
            ]
        }),
        #
        # utils.aDict({'payments': [
        #     (D('201.13'), start_dt, 'RUB', 'wallet'),
        #     (D('202.13'), start_dt + delta(days=30), 'RUB', 'wallet')
        #     ],
        #     'charges': [
        #         (start_dt - delta(days=180), 202, 203, 99.9),
        #         (start_dt, 205, 206, 100.1),
        #         (start_dt + delta(days=30), 208, 209, 7.99),
        #         # (start_dt + delta(days=60), 211, 212, 2000013)
        #     ]
        # }),
    ]

    for person_type, firm_id in offers:
        person_id = persons[person_type]

        contract_id, contract_eid = steps.ContractSteps.create_offer({
            'client_id': client_id,
            'person_id': person_id,
            'manager_uid': MANAGER.uid,
            'personal_account': 1,
            'currency': Currencies.RUB.char_code,
            'firm_id': firm_id,
            'services': [Services.ZEN.id],
            'payment_term': 10,
            'start_dt': datetime.datetime(2018, 1, 31),
            'nds': 18,
            'link_contract_id': contracts.get('ph', None)
        })

        contracts[person_type] = contract_id

    for search in searches:

        search_id = db.balance().execute("SELECT s_test_place_search_id.nextval search FROM dual")[0]['search']

        steps.api.medium().CreateOrUpdatePlace(defaults.PASSPORT_UID,
                                               {'SearchID': search_id, 'ClientID': client_id, 'Type': 20,
                                                'URL': "pytest.com"})

        # Вставка данных по платежам напрямую:
        for price, dt, currency, acc_type in search.payments:
            # Фейковые оплаты из Балалайки:
            query = 'select max(transaction_id) as val from t_partner_zen_stat'
            result = db.balance().execute(query)
            max_transaction_id = result[0]['val']
            transaction_id = max_transaction_id + 1 if max_transaction_id else 42

            zen_stat_params = {'price': price,
                               'dt': dt,
                               'client_id': client_id,
                               'transaction_id': transaction_id,
                               'currency': currency,
                               'acc_type': acc_type}

            query = "insert into t_partner_zen_stat values(:price, :dt, :client_id, :transaction_id, :currency, :acc_type)"
            db.balance().execute(query, zen_stat_params)

            query = "insert into t_export (classname, object_id, type) values ('ZenPayment', :transaction_id, 'THIRDPARTY_TRANS')"
            db.balance().execute(query, zen_stat_params)

            # Реальные платежи из Балалайки:
            # currency = 'RUR' if currency == 'RUB' else currency
            # insert_payment_service(str((price * D('100')).quantize(D('1'))), dt, client_id, currency, 5)
            #
            # steps.api.test_balance().GetPartnerCompletions({'start_dt': start_dt,
            #                                                 'end_dt': datetime.datetime.now(),
            #                                                 'completion_source': 'zen'})

            steps.CommonSteps.export('THIRDPARTY_TRANS', 'ZenPayment', transaction_id)

        # Вставка данных по начислениям

        for charge_dt, clicks, shows, bucks in search.charges:
            query = "INSERT INTO bo.T_PARTNER_DSP_STAT VALUES " \
                    "(:dt, 216651, 1, 1, 0, :amount, :amount, 0, 0, 0, 0, 0, 0, :search_id)"
            db.balance().execute(query,
                                 {'dt': charge_dt, 'search_id': search_id, 'amount': bucks})

    # Генерируем акты
    steps.CommonPartnerSteps.generate_partner_acts_fair(contracts['sw_ytph'], start_dt)
    steps.CommonPartnerSteps.generate_partner_acts_fair(contracts['sw_ytph'], start_dt + delta(days=30))
