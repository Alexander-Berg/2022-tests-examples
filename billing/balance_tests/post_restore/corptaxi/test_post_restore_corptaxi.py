# -*- coding: utf-8 -*-

import pytest

import balance.balance_api as api
from balance import balance_steps as steps

from post_restore.conftest import CORP_TAXI_DATA
from btestlib.data.partner_contexts import CORP_TAXI_RU_CONTEXT_GENERAL, \
    CORP_TAXI_ISRAEL_CONTEXT_GENERAL, CORP_TAXI_BY_CONTEXT_GENERAL, \
    CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP, CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED, \
    CORP_TAXI_KZ_CONTEXT_GENERAL_DECOUP, CORP_TAXI_KZ_CONTEXT_GENERAL_MIGRATED, CORP_TAXI_KZ_CONTEXT_GENERAL, \
    CORP_TAXI_ISRAEL_CONTEXT_GENERAL_DECOUP, CORP_TAXI_ISRAEL_CONTEXT_GENERAL_MIGRATED, \
    CORP_TAXI_BY_CONTEXT_GENERAL_DECOUP, CORP_TAXI_BY_CONTEXT_GENERAL_MIGRATED,FOOD_CORP_CONTEXT, DRIVE_B2B_CONTEXT, \
    UNIFIED_CORP_CONTRACT_CONTEXT

import pickle
from btestlib import reporter

from btestlib.constants import *
from btestlib.data.defaults import *
from btestlib.matchers import contains_dicts_equal_to

CLIENT_NAME = u'клиент корпоративного такси в балансе'
S3_PREFIX = 'CORP_TAXI_RESTORE_DUMP_TEST'
S3_PREFIX_DEBUG = 'CORP_TAXI_RESTORE_DUMP_DEBUG'

DATA_TO_RESTORE = CORP_TAXI_DATA

def load_value(storage, key_prefix, build_number=None, additional_info_required=False):
    key = utils.make_build_unique_key(key_prefix, build_number, additional_info_required=additional_info_required)
    def helper():
        if storage.is_present(key):
            with reporter.reporting(level=reporter.Level.NOTHING):
                return pickle.loads(storage.get_string_value(key))
        return None

    return utils.try_to_execute(helper, description="load {}".format(key))


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
        else:
            context = UNIFIED_CORP_CONTRACT_CONTEXT.new(contract_services=services)

    return context


@pytest.mark.parametrize('item', DATA_TO_RESTORE, ids=lambda item: item['user_info'][0]['login'])
def test_restore_corptaxi_contracts(item):
    # на всякий случай прокидываем логин в баланс
    steps.api.medium().GetPassportByLogin(0, item['user_info'][0]['login'])
    query = 'select client_id from bo.t_passport where login=:login'
    linked_client = db.balance().execute(query, {'login': item['user_info'][0]['login']})
    # print item['user_info'][0]['login']
    # проверяем, есть ли привязанные клиенты к логину. если нет, создаем нового
    if not linked_client[0]['client_id']:
        client_id = steps.ClientSteps.create({'NAME': CLIENT_NAME})
        steps.ClientSteps.link(client_id, item['user_info'][0]['login'])
    else:
        linked_client_name = steps.ClientSteps.get_client_name(linked_client[0]['client_id'])
        if linked_client_name == CLIENT_NAME:
            client_id = linked_client[0]['client_id']
        # если есть привязанный клиент и у него другое имя, значит это какой-то клиент с прода
        # нафиг его, делаем нового с нужным именем
        else:
            client_id = steps.ClientSteps.create({'NAME': CLIENT_NAME})
            steps.ClientSteps.link(client_id, item['user_info'][0]['login'])
    # идем по всем договорам
    for contract in item['contracts']:
        context = match_context(contract['firm_id'], contract['services'])
        persons = steps.ClientSteps.get_client_persons(client_id)
        person_id = None
        contract_id = None
        # если у клиента есть плательщики подходящего типа, оставляем их
        for person in persons:
            if person['TYPE'] == context.person_type.code:
                person_id = person['ID']
        if not person_id:
            person_id = steps.PersonSteps.create(client_id, context.person_type.code)
        contracts = api.medium().GetClientContracts({'ClientID': client_id})
        # идем по существующим договорам клиента. если нет с нужными сервисами, создаем заново
        for existing_contract in contracts:
            existing_params = {'payment_type': existing_contract['PAYMENT_TYPE'],
                               'services': existing_contract['SERVICES'],
                               'firm_id': existing_contract['FIRM_ID']}
            if existing_params == contract:
                contract_id = existing_contract['ID']
        if not contract_id:
            contract_id = create_contract(contract, context, client_id, person_id)
    created_contracts = []
    created_contracts_raw = api.medium().GetClientContracts({'ClientID': client_id})
    for contract in created_contracts_raw:
        created_contracts.append({'payment_type': contract['PAYMENT_TYPE'],
                                 'services': contract['SERVICES'],
                                 'firm_id': contract['FIRM_ID']})
    # проверяем, что у клиента создались все договоры
    utils.check_that(item['contracts'], contains_dicts_equal_to(created_contracts),
                     'Сравниваем созданные договоры с прикопанными')