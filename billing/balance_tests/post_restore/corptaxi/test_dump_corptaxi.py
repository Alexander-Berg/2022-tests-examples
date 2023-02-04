# -*- coding: utf-8 -*-

import balance.balance_api as api
import pickle
from btestlib import reporter
from btestlib.constants import *
from btestlib.data.defaults import *

CLIENT_NAME = u'клиент корпоративного такси в балансе'
S3_PREFIX = 'CORP_TAXI_RESTORE_DUMP_TEST'
S3_PREFIX_DEBUG = 'CORP_TAXI_RESTORE_DUMP_DEBUG'


# TODO унести сохранение и загрузку данных из s3 в utils
def save_value(storage, key_prefix, value, additional_info_required=True):
        key = utils.make_build_unique_key(key_prefix, additional_info_required=additional_info_required)

        reporter.log("Saving data to key: {}\nData: {}".format(key, utils.Presenter.pretty(value)))

        with reporter.reporting(level=reporter.Level.NOTHING):
            utils.try_to_execute(
                lambda: storage.set_string_value(key, pickle.dumps(value)),
                description="save {}".format(key)
            )


def get_data_by_client_name(client_name=CLIENT_NAME):
    query = 'select id as client_id from bo.t_client where name=:client_name'
    data = db.balance().execute(query, {'client_name': client_name})
    clients_with_logins = []
    special_cases = []
    for item in data:
        query = 'select login, passport_id from bo.t_passport where client_id=:client_id'
        user_info = db.balance().execute(query, {'client_id': item['client_id']})
        special_case = False
        if user_info:
            contracts_info = api.medium().GetClientContracts({'ClientID': item['client_id']})
            client_contracts = []
            if len(contracts_info) > 1: special_case = True
            for contract in contracts_info:
                client_contracts.append({'payment_type': contract['PAYMENT_TYPE'],
                                         'services': contract['SERVICES'],
                                         'firm_id': contract['FIRM_ID']})
                if contract['FIRM_ID'] not in [Firms.TAXI_CORP_KZT_31.id, Firms.BELGO_CORP_128.id,
                                               Firms.YANDEX_GO_ISRAEL_35.id, Firms.TAXI_13.id]:
                    special_case = True
                for service in contract['SERVICES']:
                    if service not in [Services.TAXI_CORP_CLIENTS.id, Services.TAXI_CORP.id,
                                       Services.FOOD_CORP.id, Services.DRIVE_B2B.id]:
                        special_case = True
            if special_case:
                special_cases.append({'client_id': item['client_id'],
                                        'user_info': user_info,
                                        'contracts': client_contracts})
            clients_with_logins.append({'client_id': item['client_id'],
                                        'user_info': user_info,
                                        'contracts': client_contracts})
    print 'Special cases'
    print special_cases
    print clients_with_logins
    return clients_with_logins


def test_dump_data():
    data = get_data_by_client_name(CLIENT_NAME)
    save_value(utils.s3storage(), S3_PREFIX_DEBUG, data, additional_info_required=False)
