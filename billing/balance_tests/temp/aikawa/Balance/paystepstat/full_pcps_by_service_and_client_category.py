# -*- coding: utf-8 -*-
import datetime
from collections import defaultdict
from decimal import Decimal as D

import json
import pytest
import pprint

from balance import balance_db as db
from balance import balance_steps as steps
from btestlib.constants import Paysyses, PersonTypes, Services, Products, ContractCommissionType, Firms, ContractPaymentType, Regions
from temp.igogor.balance_objects import Contexts
from simpleapi.matchers import deep_equals as de
from simpleapi.steps import check_steps as checks
from btestlib import utils as utils

TODAY = utils.Date.nullify_time_of_date(datetime.datetime.now())
TODAY_ISO = utils.Date.date_to_iso_format(TODAY)
WEEK_AFTER_ISO = utils.Date.date_to_iso_format(TODAY + datetime.timedelta(days=7))

dt = datetime.datetime.now()

QTY = 100


def get_pp_by_service(client_category, service_id):
    # определяем платежные политики по сервису
    query = '''SELECT DISTINCT vcsd.pay_policy_id
               FROM v_country_service_data vcsd
               JOIN v_country_service_data used ON used.region_id = vcsd.region_id
               WHERE vcsd.service_id = :service_id_1 AND used.explicit = 1 AND
              vcsd.agency = :agency_1 AND used.agency = 0'''
    query_params = {'service_id_1': service_id, 'agency_1': client_category}
    pay_policies = db.balance().execute(query, query_params)
    return [pay_policy['pay_policy_id'] for pay_policy in pay_policies]


def get_used_pp_by_person_category(person_category):
    query = '''SELECT DISTINCT ppf.pay_policy_id
               FROM t_pay_policy_firm ppf
               WHERE :person_category = ppf.category'''
    query_params = {'person_category': person_category}
    used_pay_policies = db.balance().execute(query, query_params)
    return [pay_policy['pay_policy_id'] for pay_policy in used_pay_policies]


def get_category_firm_pp_by_pp(pay_policy_ids):
    category_firm_pp_list = []
    query = '''SELECT * FROM T_PAY_POLICY_FIRM ppf
                    WHERE ppf.pay_policy_id in ({0})'''.format(
        ', '.join([str(pay_policy_id) for pay_policy_id in pay_policy_ids if pay_policy_id is not None]))
    pay_policies_firms = db.balance().execute(query)
    for pay_policy_firms in pay_policies_firms:
        category_firm_pp_list.append({'firm_id': pay_policy_firms['firm_id'],
                                      'category': pay_policy_firms['category'],
                                      'pay_policy_id': pay_policy_firms['pay_policy_id']})
    return category_firm_pp_list


def get_currencies_pp_by_pp(pay_policy_ids):
    result = defaultdict(list)
    query = '''SELECT DISTINCT ppf.PAY_POLICY_ID, fc.currency FROM T_PAY_POLICY_FIRM ppf
        LEFT JOIN T_FIRM_CURRENCY fc on fc.pay_policy_id = ppf.pay_policy_id
        WHERE ppf.pay_policy_id in ({0})'''.format(', '.join([str(pay_policy_id) for pay_policy_id in pay_policy_ids if pay_policy_id is not None]))
    pay_policy_currencies = db.balance().execute(query)
    for pay_policy_currency in pay_policy_currencies:
        if pay_policy_currency['currency'] is not None:
            result[pay_policy_currency['pay_policy_id']].append(pay_policy_currency['currency'])
    return result


def add_currency_to_category_firm_pp(category_firm_pp_list, currencies_pp_list):
    category_firm_set = set()
    currency_category_firm_set = set()
    for category_firm_pp in category_firm_pp_list:
        if not currencies_pp_list[category_firm_pp['pay_policy_id']]:
            category_firm_set.add((category_firm_pp['category'], category_firm_pp['firm_id']))
    for category_firm_pp in category_firm_pp_list:
        if currencies_pp_list[category_firm_pp['pay_policy_id']]:
            for currency in currencies_pp_list[category_firm_pp['pay_policy_id']]:
                dict_c_f_wo_curr = (category_firm_pp['category'], category_firm_pp['firm_id'])
                if dict_c_f_wo_curr not in category_firm_set:
                    currency_category_firm_set.add((currency, category_firm_pp['category'], category_firm_pp['firm_id']))
    return [{'currency': currency, 'category': category, 'firm_id': firm_id} for currency, category, firm_id in currency_category_firm_set], \
           [{'category': category, 'firm_id': firm_id} for category, firm_id in category_firm_set]


def get_paysyses_by_category_firm_currency(category_firm_currency_list):
    if category_firm_currency_list:
        query = '''SELECT t_paysys.id,
                    t_paysys.cc,
                    pc.region_id,
                    pc.resident,
                    t_paysys.currency,
                    t_paysys.first_limit
                    FROM t_paysys
                    LEFT OUTER JOIN t_currency t_currency_1 ON t_paysys.currency = t_currency_1.char_code
                    LEFT OUTER JOIN t_person_category pc ON pc.category = t_paysys.category
                    WHERE t_paysys.extern = 1 AND (t_paysys.currency, t_paysys.category, t_paysys.firm_id) IN
                                  {0}'''
        params_len = len(category_firm_currency_list) * len(category_firm_currency_list[0])
        params_list = [':param_{}'.format(x) for x in range(1, params_len + 1)]
        query_format = '( ' + ', '.join(
            ['(' + ', '.join(params_list[i:i + 3]) + ')' for i in xrange(0, params_len, 3)]) + ')'
        # список словарей в список
        params_value_list = [[line['currency'], line['category'], line['firm_id']] for line in category_firm_currency_list]
        params_value_list = [item for sublist in params_value_list for item in sublist]
        query_params = {'param_{0}'.format(index + 1): params_value_list[index] for index in range(params_len)}
        paysyses_by_currency_firm_category = db.balance().execute(query.format(query_format), query_params)
        return set([(paysys['id'], paysys['cc'], paysys['region_id'], paysys['resident'], paysys['currency'],
                     paysys['first_limit']) for paysys in paysyses_by_currency_firm_category])
    else:
        return set()


def get_paysyses_by_category_firm(category_firm_list):
    if category_firm_list:
        query = '''SELECT t_paysys.id, t_paysys.cc, pc.region_id, pc.resident, t_paysys.currency, t_paysys.first_limit
                    FROM t_paysys
                    LEFT OUTER JOIN t_currency t_currency_1 ON t_paysys.currency = t_currency_1.char_code
                    LEFT OUTER JOIN t_person_category pc ON pc.category = t_paysys.category
                    WHERE t_paysys.extern = 1 AND (t_paysys.category, t_paysys.firm_id) IN
                                  {0}'''

        params_len = len(category_firm_list) * len(category_firm_list[0])
        params_list = [':param_{}'.format(x) for x in range(1, params_len + 1)]
        query_format = '( ' + ', '.join(
            ['(' + ', '.join(params_list[i:i + 2]) + ')' for i in xrange(0, params_len, 2)]) + ')'
        params_value_list = [[line['category'], line['firm_id']] for line in category_firm_list]
        params_value_list = [item for sublist in params_value_list for item in sublist]
        query_params = {'param_{0}'.format(index + 1): params_value_list[index] for index in range(params_len)}
        paysyses_by_firm_category = db.balance().execute(query.format(query_format), query_params)
        return set([(paysys['id'], paysys['cc'], paysys['region_id'], paysys['resident'], paysys['currency'],
                     paysys['first_limit']) for paysys in paysyses_by_firm_category])
    else:
        return set()


def get_pay_policies(client_category, service_id):
    pay_policy_ids = get_pp_by_service(client_category, service_id)
    return pay_policy_ids if pay_policy_ids!=[None] else None


def get_params_to_select_paysyses(pay_policy_ids=None):
    # pay_policy_ids = get_pay_policies(client_id, context, params, persons, with_orders)
    # получаем категорию плательщика и фирму из T_PAY_POLICY_FIRM
    category_firm_pp_list = get_category_firm_pp_by_pp(pay_policy_ids)
    # получаем валюту фирмы платежной политики из T_FIRM_CURRENCY
    currencies_pp_list = get_currencies_pp_by_pp(pay_policy_ids)
    # дописываем валюту к категории и фирме, если есть (category_firm_currency_list), или не дописываем валюту (category_firm_list)
    category_firm_currency_list, category_firm_list = add_currency_to_category_firm_pp(category_firm_pp_list,
                                                                                       currencies_pp_list)
    return category_firm_currency_list, category_firm_list


def get_paysyses(category_firm_currency_list, category_firm_list):
    paysyses_list = get_paysyses_by_category_firm_currency(category_firm_currency_list)
    paysyses_list_ = get_paysyses_by_category_firm(category_firm_list)
    paysyses_list.update(paysyses_list_)
    return paysyses_list


def create_simple_invoice(client_id, context, hide_person=True):
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    campaigns_list = [
        {'client_id': client_id, 'service_id': context.service.id, 'product_id': context.product.id, 'qty': 100}
    ]
    invoice_id, _, _, _ = steps.InvoiceSteps.create_force_invoice(client_id=client_id,
                                                                  person_id=person_id,
                                                                  campaigns_list=campaigns_list,
                                                                  paysys_id=context.paysys.id)
    steps.InvoiceSteps.pay(invoice_id)

    if hide_person:
        steps.PersonSteps.hide_person(person_id)
    return invoice_id


def format_collateral_attrs(collateral_attrs):
    result = defaultdict()
    for attr in collateral_attrs:
        if attr['code'] in ['FIRM', 'CURRENCY']:
            result[attr['code'].lower()] = attr['value_num']
    return result


def format_paysys(paysyses, excluded_paysyses=None):
    def rec_dd():
        return defaultdict(rec_dd)

    excluded_paysyses_list = []
    if excluded_paysyses:
        for reason, excluded_paysys in excluded_paysyses.iteritems():
            for paysys in excluded_paysys:
                excluded_paysyses_list.append(paysys)
    if paysyses:
        query = '''SELECT id, category, currency, firm_id
                    FROM t_paysys
                    WHERE t_paysys.id IN ({0}) order by id'''
        query_format = ', '.join([str(paysys) for paysys in paysyses])
        paysyses_by_ids = db.balance().execute(query.format(query_format))
        result = rec_dd()
        for paysys in paysyses_by_ids:
            if not result[paysys['firm_id']][paysys['currency']][paysys['category']]:
                result[paysys['firm_id']][paysys['currency']][paysys['category']] = []
                if paysys['id'] not in excluded_paysyses_list and len(str(paysys['id'])) < 15:
                    result[paysys['firm_id']][paysys['currency']][paysys['category']].append(paysys['id'])
            else:
                if paysys['id'] not in excluded_paysyses_list and len(str(paysys['id'])) < 15:
                    result[paysys['firm_id']][paysys['currency']][paysys['category']].append(paysys['id'])
        return result
    else:
        return {}


def create_category_firm_currency_list(params):
    result = []
    for firm, currencies in params.iteritems():
        for currency, categories in currencies.iteritems():
            for category in categories:
                result.append({'category': category, 'currency': currency, 'firm_id': firm})
    return result


def create_contract(client_id, contract_params):
    person_id = steps.PersonSteps.create(client_id, contract_params['person'].code)
    contract_params_default = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'PAYMENT_TYPE': ContractPaymentType.PREPAY,
        'SERVICES': [Services.DIRECT.id],
        'DT': TODAY_ISO,
        'FINISH_DT': WEEK_AFTER_ISO,
        'IS_SIGNED': TODAY_ISO,
    }
    contract_params_default.update(contract_params['contract_params'])
    return steps.ContractSteps.create_contract_new(contract_params['contract_type'], contract_params_default, prevent_oebs_export=True)


DIRECT = Contexts.DIRECT_FISH_RUB_CONTEXT.new()
MARKET = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.MARKET)
MEDIASELLING = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.BAYAN, product=Products.MEDIA)
YT_CONTEXT = Contexts.DIRECT_FISH_YT_RUB_CONTEXT.new(person_type=PersonTypes.YT)
MEDIANA = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.MEDIANA, product=Products.MEDIANA)

BEL_TO_27_BYN = {'27': {'BYN': {'byp': [2701102],
                                'byu': [1125, 2701101]}}}

services = db.balance().execute('''select * from t_service order by id''')
services_ids = [service['id'] for service in services]


@pytest.mark.parametrize('params', [
    services_ids
    # [35]
])
def test_get_full_pcps(params):
    def rec_dd():
        return defaultdict(rec_dd)

    result = rec_dd()
    for service_id in params:
        for client_category in [0, 1]:
            pay_policies = get_pay_policies(client_category, service_id)
            if pay_policies:
                pprint.pprint(service_id)
                pprint.pprint(client_category)
                print pay_policies
                category_firm_currency_list, category_firm_list = get_params_to_select_paysyses(pay_policies)
                # получаем способы оплаты по платежной политике

                paysyses_list = get_paysyses(category_firm_currency_list, category_firm_list)
                paysyses_ids = set(paysys[0] for paysys in paysyses_list)
                pcps = json.loads(json.dumps(format_paysys(paysyses_ids)))

                result[service_id][client_category] = pcps
    print json.loads(json.dumps(result))
