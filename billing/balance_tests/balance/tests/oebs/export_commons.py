# -*- coding: utf-8 -*-


import datetime
from collections import Iterable

from dateutil.relativedelta import relativedelta

import btestlib.reporter as reporter
from balance import balance_db as db
from balance import balance_steps as steps
from balance.distribution.distribution_types import DistributionType
from btestlib import utils as utils
from btestlib.constants import Firms, ContractPaymentType, Services
from btestlib.data.defaults import ContractDefaults
from btestlib.data.person_defaults import InnType

TODAY = utils.Date.nullify_time_of_date(datetime.datetime.now())
TODAY_ISO = utils.Date.date_to_iso_format(TODAY)
WEEK_AFTER_ISO = utils.Date.date_to_iso_format(TODAY + datetime.timedelta(days=7))


class Locators(object):
    def __init__(self, balance, oebs, balance_merge_function=None):
        """
        :param balance: функция-локатор для получения значения из баланса
        :param oebs: функция-локатор для получения значения из оебс
        :param balance_merge_function: функция для объединения значений из баланса в одно значение
        """
        self.balance = balance
        self.oebs = oebs
        self.balance_merge_function = balance_merge_function


def get_value_by_locator(data, locator):
    try:
        value = locator(data)
    except KeyError as e:
        value = u"Неправильный локатор или невалидный реквизит: {}".format(e.message)

    # при сравнении считаем что None = '-' = ' ' = ''
    if isinstance(value, (str, unicode)):
        value = value.strip()

    return '' if value in [None, '-'] else value


# todo-blubimov если во всех местах заиспользовать схему AttrsByType (как в договорах),
# то можно убрать добавление имени Enum в имя атрибута
def attrs_list_to_dict(attrs_list):
    attrs_dict = {}
    for attrs in attrs_list:
        # attrs может быть Enum'ом или одним из его элементов
        if isinstance(attrs, Iterable):
            attrs_dict.update({str(attr): attr.value for attr in attrs})
        else:
            attrs_dict[str(attrs)] = attrs.value
    return utils.keys_to_lowercase(attrs_dict)


# считываем значения атрибутов из наборов данных баланса и оебс
# по сути тоже самое что и read_attr_values_list(attrs_dict, [balance_data], [oebs_data])
def read_attr_values(attrs_list, balance_data, oebs_data):
    balance_values = {}
    oebs_values = {}

    for attr_name, locators in attrs_list_to_dict(attrs_list).items():
        balance_values[attr_name] = get_value_by_locator(balance_data, locators.balance)
        oebs_values[attr_name] = get_value_by_locator(oebs_data, locators.oebs)

    return balance_values, oebs_values


# считываем значения атрибутов из списков с наборами данных баланса и оебс
def read_attr_values_list(attrs_list, balance_data_list, oebs_data_list):
    balance_values_list = []
    oebs_values_list = []

    attrs_dict = attrs_list_to_dict(attrs_list)

    # т.к. списки баланса и оебс могут быть разной длины - считываем их поочереди
    for balance_data in balance_data_list:
        balance_values = {attr_name: get_value_by_locator(balance_data, locators.balance)
                          for attr_name, locators in attrs_dict.items()}
        balance_values_list.append(balance_values)

    for oebs_data in oebs_data_list:
        oebs_values = {attr_name: get_value_by_locator(oebs_data, locators.oebs)
                       for attr_name, locators in attrs_dict.items()}
        oebs_values_list.append(oebs_values)

    return balance_values_list, oebs_values_list


def get_order_eid(service_id, service_order_id):
    return '{}-{}'.format(service_id, service_order_id)


def create_contract(client_id, person_id, contract_type, contract_params=None):
    if person_id is None:
        person_id = steps.PersonSteps.create(client_id, 'ur')
    contract_params_default = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'PAYMENT_TYPE': ContractPaymentType.PREPAY,
        'SERVICES': [Services.DIRECT.id],
        'DT': TODAY_ISO,
        'FINISH_DT': WEEK_AFTER_ISO,
        'IS_SIGNED': TODAY_ISO,
    }
    if contract_params is not None:
        contract_params_default.update(contract_params)
    if contract_type != 'spendable_corp_clients':
        return steps.ContractSteps.create_contract_new(contract_type, contract_params_default, prevent_oebs_export=True)
    else:
        return steps.ContractSteps.create_contract(contract_type, contract_params_default, prevent_oebs_export=True)


def create_contract_postpay(client_id, person_id, context):
    return create_contract(client_id, person_id, context.contract_type, {
        'CURRENCY': context.currency.num_code,
        'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
        'SERVICES': [context.service.id],
        'DEAL_PASSPORT': TODAY_ISO,
    })


def create_contract_rsya(client_id, person_id, contract_template, contract_params=None):
    contract_params_default = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'MANAGER_CODE': ContractDefaults.MANAGER_RSYA_CODE,
        'DT': TODAY_ISO,
        'END_DT': WEEK_AFTER_ISO,
        'IS_SIGNED': TODAY_ISO,
    }

    if contract_params is not None:
        contract_params_default.update(contract_params)

    return steps.ContractSteps.create_contract(contract_template, contract_params_default, prevent_oebs_export=True)


# ------------------------ методы считывающие данные из вспомогательных таблиц ------------------------

# кэш данных считанных из вспомогательных таблиц
data_cache = {}


# декоратор для сохранения данных в кэш
def cacheable(db_name, table_name, field_name):
    def decorator(func):
        def wrapper(*args, **kwargs):
            # формируем ключ вида 'balance.t_firm_export.1.oebs_org_id'
            arg_values = list(args)
            arg_values.extend(['{}_{}'.format(k, kwargs[k]) for k in sorted(kwargs.keys())])
            args_str = '.'.join(map(str, arg_values))
            cache_key = '{db}.{table}.{where_conditions}.{field}'.format(db=db_name,
                                                                         table=table_name,
                                                                         where_conditions=args_str,
                                                                         field=field_name)
            if cache_key not in data_cache:
                data_cache[cache_key] = func(*args, **kwargs)
            return data_cache[cache_key]

        return wrapper

    return decorator


@cacheable('balance', 't_tax_policy_pct', 'nds_pct')
def get_balance_tax_policy_nds_pct(tax_polict_pct_id):
    query = "SELECT nds_pct FROM t_tax_policy_pct WHERE id = :id"
    result = db.balance().execute(query, {'id': tax_polict_pct_id}, single_row=True)
    return result['nds_pct']


@cacheable('balance', 't_firm_export', 'oebs_org_id')
def get_balance_firm_oebs_org_id(firm_id):
    query = "SELECT oebs_org_id FROM t_firm_export WHERE firm_id = :firm_id AND export_type = 'OEBS'"
    result = db.balance().execute(query, {'firm_id': firm_id}, single_row=True)
    return result['oebs_org_id']


@cacheable('balance', 't_currency', 'iso_code')
def get_balance_currency_iso_code(num_code=0, iso_num_code=0):
    if iso_num_code:
        query = "SELECT iso_code  FROM t_currency WHERE iso_num_code = :iso_num_code"
        return db.balance().execute(query, {'iso_num_code': iso_num_code}, single_row=True)['iso_code']
    else:
        return steps.CurrencySteps.get_iso_code_by_num_code(num_code)


@cacheable('oebs', 'mtl_system_items_b', 'inventory_item_id')
def get_oebs_inventory_item_id(product_id):
    # продукты глобальны, поэтому считываем их из первой фирмы
    query = u"SELECT inventory_item_id FROM apps.mtl_system_items_b " \
            u"WHERE attribute13 = :product_id AND organization_id = 101 AND attribute_category = 'AR.-'"
    result = db.oebs().execute_oebs(Firms.YANDEX_1.id, query, {'product_id': product_id}, single_row=True)
    return result['inventory_item_id']


@cacheable('oebs', 'hz_parties', 'party_id')
def get_oebs_client_party_id(firm_id, client_id):
    query = "SELECT party_id FROM apps.hz_parties WHERE orig_system_reference = :object_id"
    result = db.oebs().execute_oebs(firm_id, query, {'object_id': 'C{}'.format(client_id)}, single_row=True)
    return result['party_id']


@cacheable('oebs', 'hz_parties', 'party_id')
def get_oebs_party_id(firm_id, object_id):
    query = "SELECT party_id FROM apps.hz_parties WHERE orig_system_reference = :object_id"
    result = db.oebs().execute_oebs(firm_id, query, {'object_id': object_id}, single_row=True)
    return result['party_id']


@cacheable('oebs', 'hz_parties', 'party_id')
def get_oebs_party_id_new(firm_id, object_id):
    query = "SELECT party_id FROM apps.hz_parties WHERE orig_system_reference = :object_id"
    result = db.oebs().execute_oebs(firm_id, query, {'object_id': object_id}, single_row=True)
    return result and result['party_id']


@cacheable('oebs', 'hz_party_sites', 'party_site_id')
def get_oebs_party_site_id(firm_id, object_id):
    query = "SELECT party_site_id FROM apps.hz_party_sites WHERE orig_system_reference =:object_id"
    result = db.oebs().execute_oebs(firm_id, query, {'object_id': object_id}, single_row=True)
    return result['party_site_id']


@cacheable('oebs', 'hz_party_sites', 'party_site_id')
def get_oebs_party_site_id_new(firm_id, object_id):
    query = "SELECT party_site_id FROM apps.hz_party_sites WHERE orig_system_reference =:object_id"
    result = db.oebs().execute_oebs(firm_id, query, {'object_id': object_id}, single_row=True)
    return result and result['party_site_id']


@cacheable('oebs', 'hz_locations', 'location_id')
def get_oebs_location_id(firm_id, object_id):
    query = "SELECT location_id FROM apps.hz_locations WHERE orig_system_reference = :object_id"
    result = db.oebs().execute_oebs(firm_id, query, {'object_id': object_id}, single_row=True)
    return result['location_id']


@cacheable('oebs', 'hz_party_site_use', 'party_site_use_id')
def get_oebs_party_site_use_wo_site_use_type(firm_id, party_site_id):
    query = "SELECT party_site_use_id FROM apps.hz_party_site_uses WHERE party_site_id = :party_site_id"
    result = db.oebs().execute_oebs(firm_id, query, {'party_site_id': party_site_id},
                                    single_row=True)
    return result['party_site_use_id']


@cacheable('oebs', 'hz_party_site_use', 'party_site_use_id')
def get_oebs_party_site_use(firm_id, site_use_type, party_site_id):
    query = "SELECT party_site_use_id FROM apps.hz_party_site_uses WHERE site_use_type = :site_use_type AND party_site_id = :party_site_id"
    result = db.oebs().execute_oebs(firm_id, query, {'site_use_type': site_use_type, 'party_site_id': party_site_id},
                                    single_row=True)
    return result['party_site_use_id']


@cacheable('oebs', 'hz_cust_accounts', 'cust_account_id')
def get_oebs_person_cust_account_id(firm_id, person_id):
    query = "SELECT cust_account_id FROM apps.hz_cust_accounts WHERE orig_system_reference = :object_id"
    result = db.oebs().execute_oebs(firm_id, query, {'object_id': 'P{}'.format(person_id)}, single_row=True)
    return result['cust_account_id']


@cacheable('oebs', 'hz_cust_acct_sites', 'cust_acct_site_id')
def get_oebs_cust_acct_site_id(firm_id, person_id):
    query = "SELECT cust_acct_site_id FROM apps.hz_cust_acct_sites WHERE orig_system_reference = :object_id"
    result = db.oebs().execute_oebs(firm_id, query, {'object_id': 'P{}'.format(person_id)}, single_row=True)
    return result['cust_acct_site_id']


@cacheable('oebs', 'hz_cust_account_roles', 'cust_account_role_id')
def get_oebs_person_cust_account_role_id(firm_id, person_id):
    query = "SELECT cust_account_role_id FROM apps.hz_cust_account_roles WHERE orig_system_reference = :object_id"
    result = db.oebs().execute_oebs(firm_id, query, {'object_id': 'P{}_C'.format(person_id)}, single_row=True)
    return result['cust_account_role_id']


@cacheable('oebs', 'per_people_x', 'person_id')
def get_oebs_manager_person_id(firm_id, manager_code):
    query = "SELECT person_id FROM apps.per_people_x WHERE attribute29 = :manager_code"
    result = db.oebs().execute_oebs(firm_id, query, {'manager_code': manager_code}, single_row=True)
    return result['person_id']


# -------------------------------------------------------------------------------------------------------

def get_invoice_id_by_contract(contract_id):
    with reporter.step(u'Получаем счет по договору: {}'.format(contract_id)):
        query = "SELECT id FROM T_INVOICE WHERE CONTRACT_ID=:contract_id"
        params = {'contract_id': contract_id}
        return db.balance().execute(query, params)[0]['id']


def get_act_id_by_invoice(invoice_id):
    with reporter.step(u'Получаем акт по счету: {}'.format(invoice_id)):
        query = "SELECT id FROM T_ACT WHERE INVOICE_ID=:invoice_id"
        params = {'invoice_id': invoice_id}
        return db.balance().execute(query, params)[0]['id']
