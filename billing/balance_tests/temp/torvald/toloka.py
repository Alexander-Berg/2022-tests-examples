# -*- coding: utf-8 -*-

import datetime
from decimal import Decimal as D

from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils
from btestlib.constants import Services, Currencies, Firms, Managers
from btestlib.data import person_defaults

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
            'manager_uid': '1120000000047228', #MANAGER.uid,
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
