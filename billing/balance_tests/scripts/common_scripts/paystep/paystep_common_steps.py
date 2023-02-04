# -*- coding: utf-8 -*-
from collections import defaultdict

from balance import balance_db as db


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

def get_available_paysys_cc(context):

    paysyses_from_t_paysys_service = db.balance().execute(
        '''SELECT cc FROM t_paysys WHERE id IN
          (SELECT paysys_id FROM t_paysys_service
           WHERE service_id = :service_id)''',
        {'service_id': context.service.id})
    cc_paysyses_from_t_paysys_service_set = set(paysys['cc'] for paysys in paysyses_from_t_paysys_service)
    return cc_paysyses_from_t_paysys_service_set

def format_paysys(paysyses):
    if paysyses:
        query = '''SELECT id, category, currency, firm_id
                    FROM t_paysys
                    WHERE t_paysys.id IN ({0}) ORDER BY id'''
        query_format = ', '.join([str(paysys) for paysys in paysyses])
        paysyses_by_ids = db.balance().execute(query.format(query_format))
        result = set()
        for paysys in paysyses_by_ids:
            result.add((str(paysys['firm_id']), paysys['currency'], paysys['category']))
        result_dict = [{'firm_id': fcpc[0], 'currency': fcpc[1], 'category': fcpc[2]} for
             fcpc in result]
        return result_dict
    else:
        return {}


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
        params_value_list = [[line['currency'], line['category'], line['firm_id']] for line in
                             category_firm_currency_list]
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


def get_paysyses(category_firm_currency_list, category_firm_list):
    paysyses_list = get_paysyses_by_category_firm_currency(category_firm_currency_list)
    paysyses_list_ = get_paysyses_by_category_firm(category_firm_list)
    paysyses_list.update(paysyses_list_)
    return paysyses_list


def get_currencies_pp_by_pp(pay_policy_ids):
    result = defaultdict(list)
    query = '''SELECT DISTINCT ppf.PAY_POLICY_ID, fc.currency FROM T_PAY_POLICY_FIRM ppf
        LEFT JOIN T_FIRM_CURRENCY fc on fc.pay_policy_id = ppf.pay_policy_id
        WHERE ppf.pay_policy_id in ({0})'''.format(
        ', '.join([str(pay_policy_id) for pay_policy_id in pay_policy_ids if pay_policy_id is not None]))
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
                    currency_category_firm_set.add(
                        (currency, category_firm_pp['category'], category_firm_pp['firm_id']))
    return [{'currency': currency, 'category': category, 'firm_id': firm_id} for currency, category, firm_id in
            currency_category_firm_set], \
           [{'category': category, 'firm_id': firm_id} for category, firm_id in category_firm_set]


def get_params_to_select_paysyses(pay_policy_ids=None):
    category_firm_pp_list = get_category_firm_pp_by_pp(pay_policy_ids)
    currencies_pp_list = get_currencies_pp_by_pp(pay_policy_ids)
    category_firm_currency_list, category_firm_list = add_currency_to_category_firm_pp(category_firm_pp_list,
                                                                                       currencies_pp_list)
    return category_firm_currency_list, category_firm_list


def get_pay_policies(client_category, service_id):
    query = '''SELECT DISTINCT vcsd.pay_policy_id
               FROM v_country_service_data vcsd
               JOIN v_country_service_data used ON used.region_id = vcsd.region_id
               WHERE vcsd.service_id = :service_id_1 AND used.explicit = 1 AND
              vcsd.agency = :agency_1 AND used.agency = 0'''
    query_params = {'service_id_1': service_id, 'agency_1': client_category}
    pay_policies = db.balance().execute(query, query_params)
    pay_policy_ids = [pay_policy['pay_policy_id'] for pay_policy in pay_policies]
    return pay_policy_ids if pay_policy_ids != [None] else None


def get_firm_currency_person_category(client_category, service_id):
    pay_policies = get_pay_policies(client_category, service_id)
    if pay_policies:
        category_firm_currency_list, category_firm_list = get_params_to_select_paysyses(pay_policies)
        paysyses_list = get_paysyses(category_firm_currency_list, category_firm_list)
        paysyses_ids = set(paysys[0] for paysys in paysyses_list)
        pcps = format_paysys(paysyses_ids)
        return pcps

def format_request_choices(request_choices):
    def rec_dd():
        return defaultdict(rec_dd)

    result = set()
    pcp_list = request_choices['pcp_list']
    paysyses_wo_contract_list = []
    for pcp in pcp_list:
        paysyses_wo_contract_list.extend([paysys['id'] for paysys in pcp['paysyses']])
    if paysyses_wo_contract_list:
        query = '''SELECT id, category, currency, firm_id
                            FROM t_paysys
                            WHERE t_paysys.id IN ({0}) ORDER BY id'''
        query_format = ', '.join([str(paysys) for paysys in paysyses_wo_contract_list])
        paysyses_by_ids = db.balance().execute(query.format(query_format))
        result = set()
        for paysys in paysyses_by_ids:
            result.add((str(paysys['firm_id']), paysys['currency'], paysys['category']))
    return result