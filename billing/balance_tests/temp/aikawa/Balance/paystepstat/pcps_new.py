# -*- coding: utf-8 -*-
import datetime
import json
import pprint
from collections import defaultdict
from decimal import Decimal as D

import pytest

from balance import balance_db as db
from balance import balance_steps as steps
from btestlib.constants import Paysyses, PersonTypes, Services, Products
from simpleapi.matchers import deep_equals as de
from simpleapi.steps import check_steps as checks
from temp.igogor.balance_objects import Contexts

dt = datetime.datetime.now()

QTY = 100

ALL_PAY_POLICIES = [100, 102, 110, 111, 112, 210, 310, 410, 510, 600, 610, 700, 701, 702, 710, 810, 1010, 1200, 1210, 1300, 1310, 1400,
                    1410, 1600, 1610, 1810, 2310, 2510, 2610, 2710, 11100, 11101, 11102, 11103, 11110]

DIRECT_BANK_UR_RUB = Contexts.DIRECT_FISH_RUB_CONTEXT.new(paysys=Paysyses.BANK_UR_RUB)
MARKET_BANK_UR_RUB = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.MARKET)
GEO_BANK_UR_RUB = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.GEO, product=Products.GEO)
OFD_BANK_UR_RUB = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.OFD, product=Products.OFD_BUCKS)
# TOURS_BANK_UR_RUB = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.TOURS, product=Products.TOURS)
MEDIANA_BANK_UR_RUB = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.MEDIANA, product=Products.MEDIANA)
VENDORS_BANK_UR_RUB = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.VENDORS, product=Products.VENDOR)
AUTO_BANK_UR_RUB = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.UBER)
MEDIA_BANK_UR_RUB = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.MEDIA_70, product=Products.MEDIA)


def get_pp_by_service(client_id, service_id):
    client_category = db.get_client_by_id(client_id)[0]['is_agency']
    # определяем платежные политики по сервису
    query = '''SELECT DISTINCT vcsd.pay_policy_id
               FROM v_country_service_data vcsd
               JOIN v_country_service_data used ON used.region_id = vcsd.region_id
               WHERE vcsd.service_id = :service_id_1 AND used.explicit = 1 AND
              vcsd.agency = :agency_1 AND used.agency = 0'''
    query_params = {'service_id_1': service_id, 'agency_1': client_category}
    pay_policies = db.balance().execute(query, query_params)
    return [pay_policy['pay_policy_id'] for pay_policy in pay_policies]


def get_pp_by_service_and_region(client_id, service_id, region_id):
    client_category = db.get_client_by_id(client_id)[0]['is_agency']
    # определяем платежные политики по сервису и региону
    query = '''SELECT DISTINCT v_country_service_data.pay_policy_id
                FROM t_pay_policy_firm
                  JOIN v_country_service_data ON t_pay_policy_firm.pay_policy_id = v_country_service_data.pay_policy_id
                WHERE v_country_service_data.service_id IN (:service_id_1) AND v_country_service_data.region_id = :region_id_1 AND
                      v_country_service_data.agency = 0'''
    query_params = {'service_id_1': service_id, 'region_id_1': region_id}
    pay_policies = db.balance().execute(query, query_params)
    return [pay_policy['pay_policy_id'] for pay_policy in pay_policies]


def get_pp_by_service_and_used_pp(client_id, service_id, used_pp):
    client_category = db.get_client_by_id(client_id)[0]['is_agency']
    # определяем платежные политики по сервису
    query = '''with a as (
               SELECT DISTINCT
                v_country_service_data.pay_policy_id,
                      CASE WHEN (used.pay_policy_id IN ({0}))
                        THEN 1
                      ELSE 0 END                            AS anon_1
                    FROM v_country_service_data
                      JOIN v_country_service_data used ON used.region_id = v_country_service_data.region_id
                      LEFT OUTER JOIN t_pay_policy_firm t_pay_policy_firm_1 ON t_pay_policy_firm_1.pay_policy_id
                                                                               = v_country_service_data.pay_policy_id
                      LEFT OUTER JOIN t_country t_country_1 ON t_country_1.region_id = v_country_service_data.region_id
                    WHERE
                      v_country_service_data.service_id = :service_id_1 AND used.explicit = 1 AND v_country_service_data.agency = :client_category
                      AND used.agency = 0)
                    select * from a where anon_1 = (select max(anon_1) from a)'''
    query_format = ', '.join([str(pp) for pp in used_pp])
    query_params = {'service_id_1': service_id, 'client_category': client_category}
    pay_policies = db.balance().execute(query.format(query_format), query_params)
    return [pay_policy['pay_policy_id'] for pay_policy in pay_policies]


def get_pp_by_used_pp_by_service(client_id, service_id, used_pp_by_service):
    client_category = db.get_client_by_id(client_id)[0]['is_agency']
    # определяем платежные политики по сервису
    query = '''with a as (
               SELECT DISTINCT
                v_country_service_data.pay_policy_id,
                      CASE WHEN ((used.service_id, used.pay_policy_id) IN {0})
                        THEN 1
                      ELSE 0 END                            AS anon_1
                    FROM v_country_service_data
                      JOIN v_country_service_data used ON used.region_id = v_country_service_data.region_id
                      LEFT OUTER JOIN t_pay_policy_firm t_pay_policy_firm_1 ON t_pay_policy_firm_1.pay_policy_id
                                                                               = v_country_service_data.pay_policy_id
                      LEFT OUTER JOIN t_country t_country_1 ON t_country_1.region_id = v_country_service_data.region_id
                    WHERE
                      v_country_service_data.service_id = :service_id_1 AND v_country_service_data.agency = :client_category
                      AND used.agency = 0)
                    select * from a where anon_1 = (select max(anon_1) from a)'''
    params_len = len(used_pp_by_service) * len(used_pp_by_service[0])
    params_list = [':param_{}'.format(x) for x in range(1, params_len + 1)]
    query_format = '( ' + ', '.join(
        ['(' + ', '.join(params_list[i:i + 2]) + ')' for i in xrange(0, params_len, 2)]) + ')'
    params_value_list = []
    for pp in used_pp_by_service:
        params_value_list.append(pp[0])
        params_value_list.append(pp[1])
    query_params = {'param_{0}'.format(index + 1): params_value_list[index] for index in range(params_len)}
    query_params['service_id_1'] = service_id
    query_params['client_category'] = client_category
    pay_policies = db.balance().execute(query.format(query_format), query_params)
    return [pay_policy['pay_policy_id'] for pay_policy in pay_policies]


def get_pp_by_service_and_person(client_id, service_id, person_type):
    client_category = db.get_client_by_id(client_id)[0]['is_agency']
    person_region = \
        db.balance().execute('select * from t_person_category where category = :person_type',
                             {'person_type': person_type})[
            0]['region_id']
    # определяем платежные политики по сервису
    query = '''SELECT DISTINCT vcsd.pay_policy_id
               FROM v_country_service_data vcsd
               JOIN v_country_service_data used ON used.region_id = vcsd.region_id
               WHERE vcsd.service_id = :service_id_1 AND used.explicit = 1 AND
              vcsd.agency = :agency_1 AND used.agency = 0 AND vcsd.region_id = :person_region'''
    query_params = {'service_id_1': service_id, 'agency_1': client_category, 'person_region': person_region}
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
        ', '.join([str(pay_policy_id) for pay_policy_id in pay_policy_ids]))
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
        WHERE ppf.pay_policy_id in ({0})'''.format(', '.join([str(pay_policy_id) for pay_policy_id in pay_policy_ids]))
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


def get_paysyses_cc_by_service(service_id):
    query = '''SELECT DISTINCT t_paysys.cc FROM t_paysys
              JOIN t_paysys_service ON t_paysys.id = t_paysys_service.paysys_id
              WHERE t_paysys_service.service_id IN (:service_id)'''
    query_params = {'service_id': service_id}
    paysyses_cc_by_service = db.balance().execute(query, query_params)
    return set([paysys['cc'] for paysys in paysyses_cc_by_service])


def filter_paysys_by_cc(paysyses_list, paysyses_cc):
    filtered_out = []
    filter_result = set()
    for index, paysys in enumerate(paysyses_list):
        if paysys[1] in paysyses_cc:
            filter_result.add(paysys)
        else:
            if len(str(paysys[0])) < 15:
                filtered_out.append(paysys[0])
    print 'filtered_out', filtered_out
    return set(line for line in filter_result)


def filter_paysys_by_tax(paysyses_list, product_id):
    filter_result = set()
    for paysys in paysyses_list:
        query = '''SELECT t.id, t.tax_policy_id, t.nds_pct, t.nsp_pct
           FROM t_tax t
              JOIN t_currency c ON c.num_code = t.currency_id
              FULL JOIN T_TAX_POLICY tp ON t.tax_policy_id = tp.id
            WHERE t.product_id = :product_id AND t.hidden = 0 AND t.dt <= :dt AND
                  ((t.tax_policy_id IS NULL AND c.char_code = :currency) OR
                   (t.tax_policy_id IS NOT NULL AND tp.hidden = 0 AND tp.region_id = :region_id {0}))
           ORDER BY t.dt DESC'''
        if paysys[3] is not None:
            format_str = ' AND tp.resident = :resident'
        else:
            format_str = ' '
        query = query.format(format_str)
        query_params = {'product_id': product_id, 'currency': paysys[4], 'dt': dt, 'region_id': paysys[2],
                        'resident': paysys[3]}
        taxes = db.balance().execute(query, query_params)
        if taxes:
            filter_result.add(paysys)
    return filter_result

    # for tax in taxes:
    #     find_taxes = []
    #     if tax['tax_policy_id'] is not None:
    #         query = 'select * from T_TAX_POLICY_PCT tpp where tpp.tax_policy_id = :tax_policy_id and hidden = 0 and dt<= :dt and rownum = 1 order by dt desc'
    #         query_params = {'tax_policy_id': tax['tax_policy_id'], 'dt': dt}
    #         tpp = db.balance().execute(query, query_params)
    #         if tpp:
    #             find_taxes.append(tpp)
    #     else:
    #         query = '''SELECT tpp.id
    #                    FROM t_tax_policy_pct tpp
    #                       JOIN T_TAX_POLICY tp ON tpp.tax_policy_id = tp.id
    #                    WHERE tpp.nds_pct = :nds_pct AND tpp.nsp_pct = :nsp_pct AND tpp.dt <= :dt AND tpp.hidden = 0
    #                           AND tp.hidden = 0 AND rownum = 1 AND tp.region_id = :region_id AND tp.resident = :resident
    #                    ORDER BY tp.default_tax, tpp.dt DESC'''
    #         query_params = {'nds_pct': tax['nds_pct'] if paysys[3] else 0,
    #                         'nsp_pct': tax['nsp_pct'] if paysys[3] else 0,
    #                         'dt': dt,
    #                         'region_id': paysys[2],
    #                         'resident': paysys[3]}
    #         tpp = db.balance().execute(query, query_params)
    #         if tpp:
    #             find_taxes.append(tpp)
    # print find_taxes


def filter_paysys_by_offer(paysyses_list, service_id):
    SERVER_OFFER_MAP = {132: [11101033, 11101003, 1044, 1047],
                        81: [1201000, 1201001, 1201002, 1201003, 1201033]}
    filter_result = set()
    for paysys in paysyses_list:
        if paysys[0] not in SERVER_OFFER_MAP.get(service_id, []):
            filter_result.add(paysys)
    return filter_result


def filter_paysys_by_limit_value(paysyses_list, params):
    filter_result = set()
    for paysys in paysyses_list:
        if paysys[5] is not None:
            if params.get('invoice_approx_total_sum', False):
                if params['invoice_approx_total_sum'] < D(paysys[5]):
                    filter_result.add(paysys)
            else:
                filter_result.add(paysys)
        else:
            filter_result.add(paysys)
    return filter_result


def get_get_used_person_categories_by_invoice(client_id):
    result = defaultdict(list)
    query = '''SELECT
                  o.SERVICE_ID,
                  pc.category,
                  i.firm_id
                FROM t_person p
                  JOIN T_PERSON_CATEGORY pc ON p.TYPE = pc.CATEGORY
                  JOIN T_CLIENT cl ON cl.id = p.CLIENT_ID
                  LEFT JOIN t_invoice i ON i.PERSON_ID = p.id
                  LEFT JOIN t_paysys ps ON ps.id = i.PAYSYS_ID
                  LEFT JOIN t_consume c ON c.invoice_id = i.id
                  LEFT JOIN t_order o ON o.id = c.PARENT_ORDER_ID
                  LEFT JOIN T_SERVICE s ON s.id = o.SERVICE_ID
                WHERE cl.id = :client_id AND ps.certificate = 0
      AND nvl(s.client_only, 0) = 0 AND (p.hidden = 0 OR i.id IS NOT NULL)'''
    query_params = {'client_id': client_id}
    service_category_firm_sets = db.balance().execute(query, query_params)
    for s_c_f in service_category_firm_sets:
        result[s_c_f['service_id']].append((s_c_f['firm_id'], s_c_f['category']))
    return result


def get_used_pp_by_s_c_f(s_c_f):
    pay_policy_set_by_service = []
    for service, category_firm_sets in s_c_f.iteritems():
        for firm, category in category_firm_sets:
            used_pay_policy_ids = get_used_pp_by_person_category(category)
            for pp in used_pay_policy_ids:
                pay_policy_set_by_service.append((service, pp))
    return pay_policy_set_by_service


def get_pay_policies(client_id, context, params, persons, with_orders=False, client_region=None):
    if client_region is not None:
        pay_policy_ids = get_pp_by_service_and_region(client_id, context.service.id, client_region)
        return pay_policy_ids
    if with_orders and not persons:
        s_c_f = get_get_used_person_categories_by_invoice(client_id)
        used_pay_policy_ids_by_service = get_used_pp_by_s_c_f(s_c_f)
        pay_policy_ids = get_pp_by_used_pp_by_service(client_id, context.service.id, used_pay_policy_ids_by_service)
        return pay_policy_ids
    if persons:
        pay_policy_ids_res = []
        pay_policy_ids_non_res = []
        for person in persons:
            if person['is_person_resident']:
                pay_policy_ids_res = get_pp_by_service_and_person(client_id, context.service.id, person['type'].code)
            else:
                used_pay_policy_ids = get_used_pp_by_person_category(person['type'].code)
                pay_policy_ids_non_res = get_pp_by_service_and_used_pp(client_id, context.service.id, used_pay_policy_ids)
        return pay_policy_ids_res if pay_policy_ids_res else pay_policy_ids_non_res
    else:
        pay_policy_ids = get_pp_by_service(client_id, context.service.id)
        return pay_policy_ids


def filter_paysyses(paysyses_list, context, params):
    paysyses_cc = get_paysyses_cc_by_service(context.service.id)
    paysyses_filtered_by_cc = filter_paysys_by_cc(paysyses_list, paysyses_cc)
    paysyses_filtered_by_tax = filter_paysys_by_tax(paysyses_filtered_by_cc, context.product.id)
    paysyses_filtered_by_offer = filter_paysys_by_offer(paysyses_filtered_by_tax, context.service.id)
    paysyses_filtered_by_limit_value = filter_paysys_by_limit_value(paysyses_filtered_by_offer, params)
    return paysyses_filtered_by_limit_value


def get_params_to_select_paysyses(client_id, context, params, pay_policy_ids=None, persons=[], with_orders=False):
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


@pytest.mark.parametrize('context, params', [
    # (Contexts.DIRECT_FISH_RUB_CONTEXT.new(paysys=Paysyses.BANK_UR_RUB), {}),
    (Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.MARKET), {}),
    (Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.GEO, product=Products.GEO), {}),
    (Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.OFD, product=Products.OFD_BUCKS), {}),
    # (Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.TOURS, product=Products.TOURS), {}),
    (Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.MEDIANA, product=Products.MEDIANA), {}),
    (Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.VENDORS, product=Products.VENDOR), {}),
    (Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.UBER), {}),
    (Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.MEDIA_70, product=Products.MEDIA), {}),
    (Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.TOLOKA, product=Products.TOLOKA), {}),
    (Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.CATALOG1, product=Products.CATALOG1),
     {'invoice_approx_total_sum': 1711000}),
    (Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.CATALOG1, product=Products.CATALOG2),
     {'invoice_approx_total_sum': 590000}),
    (Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.MEDIA_BANNERS, product=Products.BAYAN), {}),
    (Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.BAYAN, product=Products.DIRECT_FISH), {}),
    (Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.REALTY, product=Products.REALTY), {}),
    (Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.RABOTA, product=Products.RABOTA), {}),
    (Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.AUTORU, product=Products.AUTORU), {}),
    (Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.BANKI, product=Products.DIRECT_FISH), {}),
    (Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.KUPIBILET, product=Products.KUPIBILET), {}),
    (Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.CLOUD_143, product=Products.DIRECT_FISH), {}),
    (Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.MEDIANA, product=Products.MEDIANA), {}),
    (Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT_TUNING, product=Products.MEDIANA), {}),
]
                         )
def test_all_pp(context, params):
    client_id = steps.ClientSteps.create()
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id=client_id, product_id=context.product.id, service_id=context.service.id,
                            service_order_id=service_order_id)
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': dt}]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params={'InvoiceDesireDT': dt})
    request_choices = steps.RequestSteps.get_request_choices(request_id)
    paysyses_from_request = set(paysys['id'] for paysys in request_choices['paysys_list'])
    paysyses = get_paysyses(client_id, context, params)
    filtered_paysyses_list = filter_paysyses(paysyses, context, params)
    paysyses_ids = set(paysys[0] for paysys in filtered_paysyses_list)
    assert paysyses_from_request == paysyses_ids


def format_paysys_from_request(paysyses, excluded_paysyses=None):
    def rec_dd():
        return defaultdict(rec_dd)

    excluded_paysyses_list = []
    if excluded_paysyses:
        for reason, excluded_paysys in excluded_paysyses.iteritems():
            for paysys in excluded_paysys:
                excluded_paysyses_list.append(paysys)
    print 'excluded_paysyses_list', excluded_paysyses_list
    if paysyses:
        query = '''SELECT id, category, currency, firm_id
                    FROM t_paysys
                    WHERE t_paysys.id IN ({0}) order by id'''
        query_format = ', '.join([str(paysys) for paysys in paysyses])
        paysyses_by_ids = db.balance().execute(query.format(query_format))
        print paysyses_by_ids
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
    print result
    return result


DIRECT = Contexts.DIRECT_FISH_RUB_CONTEXT.new()
MARKET = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.MARKET)
MEDIASELLING = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.BAYAN, product=Products.MEDIA)
YT_CONTEXT = Contexts.DIRECT_FISH_YT_RUB_CONTEXT.new(person_type=PersonTypes.YT)
MEDIANA = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.MEDIANA, product=Products.MEDIANA)

BEL_TO_27_BYN = {'27': {'BYN': {'byp': [2701102],
                                'byu': [1125, 2701101]}}}




@pytest.mark.parametrize('context, params, expected', [
    (DIRECT, {'client_region': 225}, {'expected_paysys_list': {'1': {'RUR': {'ph': [1000, 1001, 1002, 1052, 1103, 1124, 2100, 2200],
                                                                             'ur': [1003, 1009, 1033]}}},
                                      'excluded_paysyses': {'NOT_IN_PAYSYS_SERVICE': [1015, 1030, 1025, 1093, 1024, 1019, 2301]}}),

    (DIRECT, {'client_region': 149}, {'expected_paysys_list': BEL_TO_27_BYN,
                                      'excluded_paysyses': {'NOT_IN_PAYSYS_SERVICE': [1126]}}),

    (MEDIASELLING, {'client_region': 149}, {'expected_paysys_list': {},
                                            'excluded_paysyses': {'NOT_IN_PAYSYS_SERVICE': [1126]}}),

    (DIRECT, {'client_region': 159}, {'expected_paysys_list': {'25': {'KZT': {'kzp': [1121, 2501021],
                                                                              'kzu': [1120, 2501020]}}},
                                      'excluded_paysyses': {'NOT_IN_PAYSYS_SERVICE': [1126]}}),

    (MEDIANA,
     {'persons': [{'type': PersonTypes.UR, 'is_person_resident': True}]},
     {'expected_paysys_list': {'1': {'RUR': {'ph': [1000, 1001, 1002],
                                             'ur': [1003, 1033]}}},
      'excluded_paysyses': {'NOT_IN_PAYSYS_SERVICE': [1030, 2200, 1025, 1093, 1009, 2100, 1024, 1124, 1015, 1019, 2301, 1103, 1052]}}),

    (DIRECT, {'persons': [], 'invoices': [YT_CONTEXT]}, {'expected_paysys_list': {"1": {"RUR": {"yt": [1014]},
                                                                                        "USD": {"yt": [1013]},
                                                                                        "EUR": {"yt": [1023]}},
                                                                                  "7": {"RUR": {"by_ytph": [1075]}}},
                                                         'excluded_paysyses': {'NOT_IN_PAYSYS_SERVICE': [2201, 1062, 1049, 11069, 1057, 1078]}}),

    (DIRECT, {'persons': [{'type': PersonTypes.UR, 'is_person_resident': True}]}, {'expected_paysys_list': {"1": {"RUR": {"ph": [1000, 1001, 1002, 1052, 1103, 1124, 2100, 2200],
                                                                                                                          "ur": [1003, 1009, 1033]}}},
                                                                                   'excluded_paysyses': {'NOT_IN_PAYSYS_SERVICE': [1030, 1025, 1093, 1024, 1015, 1019, 2301]}}),
    (DIRECT, {'persons': [{'type': PersonTypes.USU, 'is_person_resident': True}]}, {'expected_paysys_list': {"4": {"USD": {"usu": [1028, 1065],
                                                                                                                           "usp": [1029, 1064]}}},
                                                                                    'excluded_paysyses': {'NOT_IN_PAYSYS_SERVICE': [1035, 1034]}}),
    (DIRECT, {'persons': [{'type': PersonTypes.SW_YT, 'is_person_resident': False}]}, {'expected_paysys_list': {"1": {"RUR": {"ph": [1000, 1001, 1002, 1052, 1103, 1124, 2100, 2200],
                                                                                                                              "yt": [1014],
                                                                                                                              "ur": [1003, 1009, 1033]},
                                                                                                                      "USD": {"yt": [1013]},
                                                                                                                      "EUR": {"yt": [1023]}},
                                                                                                                "4": {"USD": {"usu": [1028, 1065],
                                                                                                                              "usp": [1029, 1064]}},
                                                                                                                "7": {"RUR": {"by_ytph": [1075]},
                                                                                                                      "USD": {"sw_ytph": [1070, 1076, 1106, 701099],
                                                                                                                              "sw_yt": [1047, 1086, 1107]},
                                                                                                                      "CHF": {"sw_ytph": [1071, 1079, 1110],
                                                                                                                              "sw_yt": [1048, 1088, 1111]},
                                                                                                                      "EUR": {"sw_ytph": [1069, 1077, 1114],
                                                                                                                              "sw_yt": [1046, 1087, 1115]}},
                                                                                                                "8": {"TRY": {"trp": [1051, 1056], "tru": [1050, 1055]}},
                                                                                                                "25": {"KZT": {"kzp": [1121, 2501021], "kzu": [1120, 2501020]}},
                                                                                                                "27": {"BYN": {"byu": [1125, 2701101], "byp": [2701102]}}},

                                                                                       'excluded_paysyses': {'NOT_IN_PAYSYS_SERVICE': [2202, 1062, 1093, 2101, 1030, 1049, 1024, 1019, 1057, 2300,
                                                                                                                                       1063, 1126, 2203, 1025, 1074, 1015, 2301, 11069, 1078, 2201,
                                                                                                                                       1072, 1034, 1035, 1073]}}),
    (DIRECT, {'persons': [{'type': PersonTypes.YT, 'is_person_resident': False},
                          {'type': PersonTypes.KZP, 'is_person_resident': True}]}, {'expected_paysys_list': {"25": {"KZT": {"kzp": [1121, 2501021],
                                                                                                                            "kzu": [1120, 2501020]}}},
                                                                                    'excluded_paysyses': {'NOT_IN_PAYSYS_SERVICE': [2201, 1062, 1049, 11069, 1057, 1078]}}),
    (MARKET, {}, {'expected_paysys_list': {"7": {"RUR": {"by_ytph": [1075]},
                                                 "USD": {"sw_ph": [1067, 1080],
                                                         "sw_ur": [1044, 1083],
                                                         "sw_yt": [1047, 1086],
                                                         "sw_ytph": [1070, 1076]},
                                                 "CHF": {"sw_ph": [1068, 1082],
                                                         "sw_ur": [1045, 1085],
                                                         "sw_yt": [1048, 1088],
                                                         "sw_ytph": [1071, 1079]},
                                                 "EUR": {"sw_ph": [1066, 1081],
                                                         "sw_ur": [1043, 1084],
                                                         "sw_yt": [1046, 1087],
                                                         "sw_ytph": [1069, 1077]}},
                                           "4": {"USD": {"usu": [1028]}},
                                           "111": {"RUR": {"ph": [11101000, 11101001, 11101002, 11101052],
                                                           "yt": [11101014],
                                                           "ur": [11101003, 11101033]},
                                                   "BYN": {"yt": [11101100]},
                                                   "USD": {"yt": [11101013]},
                                                   "KZT": {"yt_kzp": [11101061],
                                                           "yt_kzu": [11101060]},
                                                   "EUR": {"yt": [11101023]}}},
                  'excluded_paysyses': {'NOT_IN_PAYSYS_SERVICE': [1051, 1113, 2202, 1056, 1105, 11101015, 1108, 1115, 2300,
                                                                  11101030, 1107, 1065, 11101025, 11101103, 1104,
                                                                  11101124, 11102200, 11111069, 1050, 11101009,
                                                                  1114, 1064, 11101019, 1063, 2203, 11101057,
                                                                  702205, 1111, 11102201, 1106, 11101078, 11101024,
                                                                  11101062, 1074, 2101, 1029, 1112, 1072, 11102301,
                                                                  1034, 1109, 11101093, 11101036, 11101049, 11102100,
                                                                  1055, 1035, 1073, 1110, 701099]}})
])
def test_pp_depend_on_client_region_1(context, params, expected):
    client_params = {'REGION_ID': params['client_region']} if params.get('client_region', False) else None
    client_id = steps.ClientSteps.create(client_params)
    for person in params.get('persons', []):
        steps.PersonSteps.create(client_id, person['type'].code)
    for invoice_context in params.get('invoices', []):
        create_simple_invoice(client_id, invoice_context, hide_person=False)
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id=client_id, product_id=context.product.id, service_id=context.service.id,
                            service_order_id=service_order_id)
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': dt}]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params={'InvoiceDesireDT': dt})
    request_choices = steps.RequestSteps.get_request_choices(request_id)

    # определяем платежную политику
    pay_policies = get_pay_policies(client_id, context, params, persons=params.get('persons', False), with_orders=params.get('invoices', False), client_region=params.get('client_region', None))
    # узнаем категорию плательщика/фирму/валюту и категорию/фирму
    category_firm_currency_list, category_firm_list = get_params_to_select_paysyses(client_id, context, params, pay_policies)
    print category_firm_currency_list
    print category_firm_list
    # получаем способы оплаты по платежной политике
    paysyses_list = get_paysyses(category_firm_currency_list, category_firm_list)
    # фильтруем способы оплаты
    filtered_paysyses_list = filter_paysyses(paysyses_list, context, params)

    # получаем способы оплаты из реквеста
    paysyses_from_request = set(paysys['id'] for paysys in request_choices['paysys_list'])
    # форматируем способы оплаты из реквеста
    formated_paysyses = format_paysys_from_request(paysyses_from_request)
    # формируем тройки фирма/валюта/категория из параметров
    category_firm_currency_list_from_params = create_category_firm_currency_list(expected['expected_paysys_list'])
    paysyses_list_from_params = get_paysyses(category_firm_currency_list_from_params, [])
    formated_paysyses_from_params = format_paysys_from_request(set(paysys[0] for paysys in paysyses_list_from_params), expected['excluded_paysyses'])
    pprint.pprint(json.loads(json.dumps(formated_paysyses_from_params)))
    paysyses_ids = set(paysys[0] for paysys in filtered_paysyses_list)
    assert paysyses_from_request == paysyses_ids
    checks.check_that(expected['expected_paysys_list'], de.deep_equals_to(json.loads(json.dumps(formated_paysyses))))
    checks.check_that(json.loads(json.dumps(formated_paysyses)), de.deep_equals_to(json.loads(json.dumps(formated_paysyses_from_params))))
