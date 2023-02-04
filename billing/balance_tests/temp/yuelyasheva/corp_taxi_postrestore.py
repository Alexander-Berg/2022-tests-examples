# -*- coding: utf-8 -*-
import json
from datetime import datetime
from decimal import Decimal
from itertools import chain

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import empty, equal_to

import balance.balance_db as db
import balance.balance_api as api
from balance import balance_steps as steps
from balance.balance_steps.new_taxi_steps import TaxiSteps
from btestlib import utils
from btestlib.constants import Services, Firms, Managers, Currencies, PaymentType, Products, PaysysType, BlueMarketOrderType
from btestlib.data.partner_contexts import CORP_TAXI_RU_CONTEXT_GENERAL, CORP_TAXI_KZ_CONTEXT_GENERAL, \
    CORP_TAXI_ISRAEL_CONTEXT_GENERAL, CORP_TAXI_BY_CONTEXT_GENERAL, \
    CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP, CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED, \
    CORP_TAXI_KZ_CONTEXT_GENERAL_DECOUP, CORP_TAXI_KZ_CONTEXT_GENERAL_MIGRATED, CORP_TAXI_KZ_CONTEXT_GENERAL, \
    CORP_TAXI_ISRAEL_CONTEXT_GENERAL_DECOUP, CORP_TAXI_ISRAEL_CONTEXT_GENERAL_MIGRATED, \
    CORP_TAXI_BY_CONTEXT_GENERAL_DECOUP, CORP_TAXI_BY_CONTEXT_GENERAL_MIGRATED,FOOD_CORP_CONTEXT, DRIVE_B2B_CONTEXT, \
    UNIFIED_CORP_CONTRACT_CONTEXT, UNIFIED_CORP_CONTRACT_SERVICES

import pickle
import copy
from btestlib import reporter, secrets

from balance.balance_objects import Context
from btestlib.constants import *
from btestlib.data.defaults import *

CLIENT_NAME = u'клиент корпоративного такси в балансе'
# CLIENT_NAME = u'клиент корпов в балансе тест'
# CLIENT_NAME = 'клиент корпов боевые данные'
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


def load_value(storage, key_prefix, build_number=None, additional_info_required=False):
    key = utils.make_build_unique_key(key_prefix, build_number, additional_info_required=additional_info_required)

    def helper():
        if storage.is_present(key):
            with reporter.reporting(level=reporter.Level.NOTHING):
                return pickle.loads(storage.get_string_value(key))
        return None

    return utils.try_to_execute(helper, description="load {}".format(key))



def try_name_select():
    name = 'ООО "Клиент"'
    query = 'select id as client_id from bo.t_client where name=:client_name'
    data = db.balance().execute(query, {'client_name': name})


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


def dump_data():
    data = get_data_by_client_name(CLIENT_NAME)
    save_value(utils.s3storage(), S3_PREFIX_DEBUG, data, additional_info_required=False)


def restore_data():
    data = load_value(utils.s3storage(), S3_PREFIX_DEBUG)
    print data
    restored_data = []
    for item in data:
        steps.api.medium().GetPassportByLogin(0, item['user_info'][0]['login'])
        query = 'select client_id from bo.t_passport where login=:login'
        linked_client = db.balance().execute(query, {'login': item['user_info'][0]['login']})
        if not linked_client[0]['client_id']:
            client_id = steps.ClientSteps.create({'NAME': CLIENT_NAME})
            steps.ClientSteps.link(client_id, item['user_info'][0]['login'])
        else:
            linked_client_name = steps.ClientSteps.get_client_name(linked_client[0]['client_id'])
            if linked_client_name == CLIENT_NAME:
                client_id = linked_client[0]['client_id']
            else:
                client_id = steps.ClientSteps.create({'NAME': CLIENT_NAME})
                steps.ClientSteps.link(client_id, item['user_info'][0]['login'])
        for contract in item['contracts']:
                context = match_context(contract['firm_id'], contract['services'])
                persons = steps.ClientSteps.get_client_persons(client_id)
                person_id = None
                contract_id = None
                for person in persons:
                    if person['TYPE'] == context.person_type.code:
                        person_id = person['ID']
                if not person_id:
                    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
                contracts = api.medium().GetClientContracts({'ClientID': client_id})
                for existing_contract in contracts:
                    existing_params = {'payment_type': existing_contract['PAYMENT_TYPE'],
                                       'services': existing_contract['SERVICES'],
                                       'firm_id': existing_contract['FIRM_ID']}
                    if existing_params == contract:
                        contract_id = existing_contract['ID']
                if not contract_id:
                    contract_id = create_contract(contract, context, client_id, person_id)
                restored_data.append({'client_id': client_id, 'contract_id': contract_id,
                                      'login': item['user_info'][0]['login']})

    reporter.log(u"Восстановленные данные")
    for item in restored_data:
        reporter.log(u"login: {0} \tclient_id: {1} \tcontract_id: {2}".format(item['login'],
                                                                              item['client_id'],
                                                                              item['contract_id']))


def create_contract(contract, context, client_id, person_id):
    is_postpay = 1 if contract['payment_type'] == 3 else 0
    _, _, contract_id, _ = steps.ContractSteps.create_partner_contract(context, is_postpay=is_postpay,
                                                                       client_id=client_id, person_id=person_id)
    return contract_id


def match_context(firm_id, services):
    context = None
    if firm_id == Firms.TAXI_CORP_KZT_31.id:
        if services == [Services.TAXI_CORP_CLIENTS.id]:
            context = CORP_TAXI_KZ_CONTEXT_GENERAL_DECOUP
        elif services == [Services.TAXI_CORP.id]:
            context = CORP_TAXI_KZ_CONTEXT_GENERAL
        elif sorted(services) == [Services.TAXI_CORP.id, Services.TAXI_CORP_CLIENTS.id]:
            context = CORP_TAXI_KZ_CONTEXT_GENERAL_MIGRATED
    elif firm_id == Firms.YANDEX_GO_ISRAEL_35.id:
        if services == [Services.TAXI_CORP_CLIENTS.id]:
            context = CORP_TAXI_ISRAEL_CONTEXT_GENERAL_DECOUP
        elif services == [Services.TAXI_CORP.id]:
            context = CORP_TAXI_ISRAEL_CONTEXT_GENERAL
        elif sorted(services) == [Services.TAXI_CORP.id, Services.TAXI_CORP_CLIENTS.id]:
            context = CORP_TAXI_ISRAEL_CONTEXT_GENERAL_MIGRATED
    elif firm_id == Firms.BELGO_CORP_128.id:
        if services == [Services.TAXI_CORP_CLIENTS.id]:
            context = CORP_TAXI_BY_CONTEXT_GENERAL_DECOUP
        elif services == [Services.TAXI_CORP.id]:
            context = CORP_TAXI_BY_CONTEXT_GENERAL
        elif sorted(services) == [Services.TAXI_CORP.id, Services.TAXI_CORP_CLIENTS.id]:
            context = CORP_TAXI_BY_CONTEXT_GENERAL_MIGRATED
    elif firm_id == Firms.TAXI_13.id:
        if services == [Services.DRIVE_B2B.id]:
            context = DRIVE_B2B_CONTEXT
        elif services == [Services.FOOD_CORP.id]:
            context = FOOD_CORP_CONTEXT
        elif services == [Services.TAXI_CORP_CLIENTS.id]:
            context = CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP
        elif services == [Services.TAXI_CORP.id]:
            context = CORP_TAXI_RU_CONTEXT_GENERAL
        elif sorted(services) == [Services.TAXI_CORP.id, Services.TAXI_CORP_CLIENTS.id]:
            context = CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED
        elif sorted(services) == sorted(UNIFIED_CORP_CONTRACT_SERVICES):
            context = UNIFIED_CORP_CONTRACT_CONTEXT

    return context


def create_test_data():
    contexts = [DRIVE_B2B_CONTEXT,
                CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED,
                FOOD_CORP_CONTEXT,
                CORP_TAXI_KZ_CONTEXT_GENERAL_MIGRATED,
                CORP_TAXI_ISRAEL_CONTEXT_GENERAL_MIGRATED,
                CORP_TAXI_BY_CONTEXT_GENERAL_DECOUP]
    logins = ['yb-corp-restore-test-1','yb-corp-restore-test-2','yb-corp-restore-test-3','yb-corp-restore-test-4',
              'yb-corp-restore-test-5','yb-corp-restore-test-6','yb-corp-restore-test-7','yb-corp-restore-test-8',
              'yb-corp-restore-test-9','yb-corp-restore-test-10']
    for i in range(len(contexts)):
        client_id = steps.ClientSteps.create({'NAME': CLIENT_NAME})
        api.medium().GetPassportByLogin(0, logins[i])
        steps.ClientSteps.link(client_id, logins[i])
        is_postpay = i % 2
        client_id, person_id, contract_id, _ = \
            steps.ContractSteps.create_partner_contract(contexts[i], client_id=client_id, is_postpay=is_postpay)


if __name__ == "__main__":
    # create_test_data()
    dump_data()

    # restore_data()
