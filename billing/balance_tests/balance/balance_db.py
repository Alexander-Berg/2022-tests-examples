# coding: utf-8

import warnings
import collections
import json
from enum import Enum
from itertools import chain
import btestlib.reporter as reporter
import btestlib.utils as utils
from balance import balance_api
from btestlib import environments, config

__author__ = 'igogor'


def rec_dd():
    return collections.defaultdict(rec_dd)


def ddict2dict(d):
    for k, v in sorted(d.items(), key=lambda x: x[0]):
        if isinstance(v, dict):
            d[k] = ddict2dict(v)
        if isinstance(v, list):
            d[k] = sorted(v)
    return dict(d)


def ddict2tuple(val):
    if isinstance(val, dict):
        res = []
        for k, v in sorted(val.items(), key=lambda x: x[0]):
            res.append(k)
            res.append(ddict2tuple(v))
        return tuple(res)
    elif isinstance(val, list):
        return [ddict2tuple(v) for v in sorted(val)]
    return val


class Diff(object):

    def __init__(self, objects_params):
        self.rows = []
        for classname, object_ids in objects_params.items():
            for object_id in object_ids:
                self.rows.append(DiffRow(classname, object_id))

    def get_diff(self, to_tuple=False):
        result = rec_dd()
        for row in self.rows:
            row_diff = row.get_diff()
            if row_diff:
                result[row.classname][row.object_id] = row_diff
        return ddict2tuple(result) if to_tuple else ddict2dict(result)


class DiffRow(object):

    def __init__(self, classname, object_id):
        self.object_id = object_id
        self.classname = classname
        self.data_before = self.get_data()

    def get_data(self):
        method = globals()['get_%s_by_id' % self.classname.lower()]
        return method(object_id=self.object_id)[0]

    def get_diff(self):
        result = {}
        data_after = self.get_data()
        for key in set(chain(self.data_before, data_after)):
            value_before = self.data_before.get(key, None)
            value_after = data_after.get(key, None)
            if value_before != value_after:
                result[key] = [value_before, value_after]
        return result


# todo-architect базовый класс надо перенести в utils
class BaseDB(object):
    def __init__(self, dbname):
        self.dbname = dbname

    def execute(self, query, named_params=None, single_row=False, fail_empty=False, descr=u'', host_code=None):
        with reporter.step(u"Выполняем запрос к базе {}: {} ".format(self.dbname, utils.decode_obj(descr)),
                           log_=False):
            if not host_code:
                result = balance_api.test_balance().ExecuteSQL(self.dbname, query, named_params)
            else:
                host_test_balance = utils.XmlRpc.ReportingServerProxy(
                    url=environments.BalanceEnvironment.test_env(host_code).test_balance_url,
                    namespace='TestBalance',
                    transport=environments.balance_env().transport)
                result = host_test_balance.ExecuteSQL(self.dbname, query, named_params)

            # когда запрос возвращает только статус обрабатывать количество строк бессмысленно
            if not isinstance(result, list):
                return result

            if fail_empty and len(result) == 0:
                raise utils.TestsError(u"Query should return results, but is empty:\n{}"
                                       .format(utils.Presenter.sql_query(query, named_params)))
            if single_row:
                if len(result) == 1:
                    return result[0]
                elif len(result) == 0:
                    return {}
                else:
                    raise utils.TestsError(u"Query should contain single row, but returns {} rows\n{}"
                                           .format(len(result), utils.Presenter.sql_query(query, named_params)))
            return result

    def insert(self, table, params, named_params=None):
        # if not isinstance(params_ordered_dict, collections.OrderedDict):
        #     raise TypeError('Use collections.OrderedDict in balance_db.Balance().insert()')
        warnings.warn("deprecated - use full queries instead", DeprecationWarning)
        query = 'insert into {table} ({names}) values ({values})'.format(
            table=table,
            names=', '.join(params.keys()),
            values=', '.join([str(value) for value in params.values()]))

        self.execute(query, named_params)

    def sequence_nextval(self, seq_name):
        return self.execute('select {}.nextval from dual'.format(seq_name),
                            single_row=True,
                            descr='Получаем {}.nextal'.format(seq_name))['nextval']

    # mviews - одна или несколько матвью через запятую
    def refresh_mview(self, mviews, fast=False):
        with reporter.step(u'Обновляем матвью {}'.format(mviews)):
            refresh_method = 'F' if fast else 'C'
            query = "BEGIN dbms_mview.refresh('{mviews}', '{refresh_method}'); END;".format(
                mviews=mviews, refresh_method=refresh_method)
            self.execute(query)


# Сюда дописываем запросы, которые надо выполнять много раз
class BalanceBO(BaseDB):
    def __init__(self, dbname='balance'):
        super(BalanceBO, self).__init__(dbname=dbname)

    def get_order_id(self, service_id, service_order_id):
        result = self.execute('SELECT id FROM t_order WHERE service_id = {} AND service_order_id = {}'
                              .format(service_id, service_order_id),
                              single_row=True)
        return result['id']

    def insert_extprop(self, object_id, classname, attrname, passport_uid, id='s_extprops.nextval', key='null',
                       value_str='null', value_num='null', value_dt='null', value_clob='EMPTY_CLOB()',
                       update_dt='sysdate', update_dt_yt='null'):
        self.insert(table='T_EXTPROPS',
                    params={'ID': id,
                            'OBJECT_ID': object_id,
                            'CLASSNAME': classname,
                            'ATTRNAME': attrname,
                            'KEY': key,
                            'VALUE_STR': value_str,
                            'VALUE_NUM': value_num,
                            'VALUE_DT': value_dt,
                            'VALUE_CLOB': value_clob,
                            'UPDATE_DT': update_dt,
                            'PASSPORT_ID': passport_uid,
                            'UPDATE_DT_YT': update_dt_yt})

    def find_max_id(self, table_name, column_name='id'):
        result = self.execute('''SELECT MAX({0}) from {1}'''.format(column_name, table_name))
        return result[0]['MAX({0})'.format(column_name.upper())]

    def get_overdraft_limit(self, client_id, service_id):
        query = "SELECT overdraft_limit FROM t_client_overdraft WHERE client_id = :client_id AND service_id = :service_id"
        query_params = {'client_id': client_id, 'service_id': service_id}
        result = self.execute(query, query_params)
        return result[0]['overdraft_limit'] if len(result) > 0 else None

    def get_overdraft_limit_by_firm(self, client_id, service_id, firm_id):
        query = "SELECT overdraft_limit FROM t_client_overdraft WHERE client_id = :client_id AND service_id = :service_id AND firm_id = :firm_id"
        query_params = {'client_id': client_id, 'service_id': service_id, 'firm_id': firm_id}
        result = self.execute(query, query_params)
        return result[0]['overdraft_limit'] if len(result) > 0 else None

    def get_extprops_by_object_id(self, classname, object_id):
        query = "SELECT * FROM t_extprops WHERE classname = :classname AND object_id = :object_id"
        query_params = {'classname': classname, 'object_id': object_id}
        return balance().execute(query, query_params)

    def get_unpaid_y_invoices(self, client_id):
        query = "SELECT * FROM t_invoice WHERE type = 'y_invoice' AND receipt_sum < effective_sum AND client_id = :client_id"
        query_params = {'client_id': client_id}
        return balance().execute(query, query_params)

    def clear_act_creation_filter_config_value(self):
        query = "UPDATE bo.t_config SET value_json = '[]' WHERE item = 'ACT_CREATION_FILTER'"
        return balance().execute(query, {})


class Meta(BaseDB):
    def __init__(self):
        super(Meta, self).__init__(dbname='meta')


class BalanceBS(BalanceBO):
    """Отнаследовался от BalanceBO но надо учитывать, что некоторые таблицы в схемах различаются как их наличием,
    так и набором полей. Некоторые запросы валидные для BS на BO работать не будут и наоборот"""

    def __init__(self, dbname='bs'):
        super(BalanceBO, self).__init__(dbname=dbname)


@utils.cached
def balance():
    return BalanceBO()


# ждем BALANCE-28915
# @utils.cached
# def balance_meta():
#     return BalanceBO(dbname='balance_meta')


@utils.cached
def meta():
    return Meta()


@utils.cached
def balance_bs_ora():
    return BalanceBS(dbname='bs')


@utils.cached
def balance_bs_ora_dev():
    return BalanceBS(dbname='bs_dev')


@utils.cached
def balance_bo_ora_dev():
    return BalanceBS(dbname='bo_dev')


@utils.cached
def balance_bs_pg():
    return BalanceBS(dbname='bs_ng_single_host')


def pcidss_mysql():
    return BaseDB(dbname='pcidss_dev')


def balance_bs():
    return BalanceBS(dbname=environments.SimpleapiEnvironment.DB_NAME)


class OebsUser(Enum):
    YARU_ACCOUNTANT = u'YARU Пользователь бухгалтерии. Поступления на банковские счета.'
    YAUA_ACCOUNTANT = u'YAUA Бухгалтер ДДС'
    YAUA_DEBTOR_SETTINGS = u'YAUA Настройка Дебиторы'
    AUHO_ACCOUNTANT = u'AUHO Пользователь бухгалтерии. Поступления на банковские счета.'
    DEBTOR_DISPATCHER = u'YARU Настройка Дебиторы'
    YETA_ACCOUNTANT = u'YETA Бухгалтер ДДС'

    def __init__(self, login):
        self.login = login


class OebsConcurrentStatus(Enum):
    CORRECT = 'C'
    ERROR = 'E'
    WARNING = 'G'

    def __init__(self, label):
        self.label = label


class Oebs(BaseDB):
    def __init__(self):
        super(Oebs, self).__init__(dbname='oebs_qa')

    # По идее для запросов в ОЕБС нужно всегда использовать TestBalance.ExecuteOEBS,
    # т.к. в нем делается правильная инициализация фирмы. Но сейчас он работает только для select'ов
    # Это должны исправить в BALANCE-24726 и тогда этот метод можно будет переименовать в execute
    def execute_oebs(self, firm_id, query, named_params=None, single_row=False, fail_empty=False, descr=u''):
        named_params = named_params or {}  # todo-blubimov можно убрать после BALANCE-24726
        with reporter.step(u"Выполняем запрос к базе {} в фирму {}: {} ".format(self.dbname, firm_id,
                                                                                utils.decode_obj(descr)),
                           log_=False):
            result = balance_api.test_balance().ExecuteOEBS(firm_id, query, named_params)

            # когда запрос возвращает только статус обрабатывать количество строк бессмысленно
            if not isinstance(result, list):
                return result

            # todo-blubimov можно убрать после BALANCE-24726
            for i, v in enumerate(result):
                result[i] = utils.keys_to_lowercase(v)

            if fail_empty and len(result) == 0:
                raise utils.TestsError(u"Query should return results, but is empty:\n{}"
                                       .format(utils.Presenter.sql_query(query, named_params)))
            if single_row:
                if len(result) == 1:
                    return result[0]
                elif len(result) == 0:
                    return {}
                else:
                    raise utils.TestsError(u"Query should contain single row, but returns {} rows\n{}"
                                           .format(len(result), utils.Presenter.sql_query(query, named_params)))
            return result

    def get_account_id(self, account_number):
        return self.execute(descr="Получаем оебс id номера счета",
                            query="""SELECT bank_account_id
                                           FROM apps.ce_bank_accounts cba
                                           WHERE cba.bank_account_num = :account
                                           AND end_date IS NULL""",
                            named_params={'account': account_number}, single_row=True)['bank_account_id']

    def get_bank_id(self, bank_name):
        return self.execute(descr="Получаем оебс id банка",
                            query="""SELECT branch_party_id
                                           FROM apps.ce_bank_branches_v bbv
                                           WHERE bbv.bank_branch_name = :bank_name""",
                            named_params={'bank_name': bank_name}, single_row=True)['branch_party_id']


@utils.cached
def oebs():
    return Oebs()


# todo-architect эти функции должны быть распределены по соответствующим классам
# ----------------------------------------------------------------------------------------------------------------------
# TODO: support None => is null values
def get_custom_data(table, fields, identifiers, classname, object_id, ordering='id desc', and_statement=''):
    if classname in identifiers:
        query = 'select {0} from {1} where {2} in (:object_id) {3} order by {4}'.format(','.join(fields)
                                                                                        , table
                                                                                        , identifiers[classname]
                                                                                        , and_statement
                                                                                        , ordering
                                                                                        )
        return balance().execute(query, {'object_id': object_id})
    else:
        # TODO: add correct exception handling
        raise Exception


# TODO: support None => is null values
# TODO: support 'in' operation
def oracle_select(table, fields, filter_params, ordering='id desc'):
    warnings.warn("deprecated - use full queries instead", DeprecationWarning)
    query_string = 'SELECT {0} FROM {1} WHERE {2} ORDER BY {3}'
    filter_string = ' and '.join(["{0} = '{1}'".format(*item) for item in filter_params.items()])
    query = query_string.format(','.join(fields),
                                table,
                                filter_string,
                                ordering)
    return balance().execute(query)


# TODO: support None => is null values
# TODO: support 'in' operation
def oracle_update(table, update_params, filter_params):
    warnings.warn("deprecated - use full queries instead", DeprecationWarning)
    update_str = ','.join(['{0} = {1}'.format(*item) for item in update_params.items()])
    update_str = update_str.replace('None', 'null')
    filter_str = ' and '.join(['{0} = {1}'.format(*item) for item in filter_params.items()])
    filter_str = filter_str.replace('= None', 'is null')
    query = 'update {0} set {1} where {2}'.format(table, update_str, filter_str)
    return balance().execute(query)


# TODO: support None => is null values
# TODO: support 'in' operation
def oracle_delete(table, filter_params):
    warnings.warn("deprecated - use full queries instead", DeprecationWarning)
    filter_str = ' and '.join(['{0} = {1}'.format(*item) for item in filter_params.items()])
    filter_str = filter_str.replace('= None', 'is null')
    query = 'delete from {0} where {2}'.format(table, filter_str)
    return balance().execute(query)


# TODO: support None => is null values
def oracle_insert(table, params):
    warnings.warn("deprecated - use full queries instead", DeprecationWarning)
    query_string = 'INSERT INTO {0} ({1}) VALUES ({2})'
    query = query_string.format(table,
                                ','.join(params.keys()),
                                ','.join(['\'{0}\''.format(str(value)) for value in params.values()])
                                )
    return balance().execute(query)


# ----------------------------------------------------------------------------------------------------------------------
def get_requests(classname, object_id):
    fields = ['id',
              'promo_code_id'
              ]
    table = 't_request'
    identifiers = {'Id': 'id'}
    ordering = 'id desc'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering)


def get_request_by_id(object_id):
    return get_requests('Id', str(object_id))


# ----------------------------------------------------------------------------------------------------------------------


def get_consumes(classname, object_id):
    fields = ['id'
        , 'parent_order_id'
        , 'consume_qty'
        , 'consume_sum'
        , 'current_qty'
        , 'current_sum'
        , 'completion_qty'
        , 'completion_sum'
        , 'passport_id'
        , 'static_discount_pct'
        , 'invoice_id'
        , 'price'
        , 'operation_id'
        , 'discount_pct'
        , 'act_sum'
        , 'act_qty'
        , 'archive'
        , 'manager_code'
        , 'tax_policy_pct_id'
              ]
    table = 't_consume'
    identifiers = {'Invoice': 'invoice_id', 'Order': 'parent_order_id', 'Id': 'id'}
    ordering = 'id'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering)


def get_consumes_by_invoice(object_id):
    return get_consumes('Invoice', object_id)


def get_consumes_by_order(object_id):
    return get_consumes('Order', object_id)


def get_consume_by_id(object_id):
    return get_consumes('Id', object_id)


# ----------------------------------------------------------------------------------------------------------------------

def get_acts(classname, object_id):
    fields = ['id',
              'dt',
              'invoice_id',
              'amount',
              'amount_nds',
              'external_id',
              'operation_id',
              'type',
              'paid_amount',
              'payment_term_dt',
              'currency_rate',
              'act_sum']
    table = 't_act_internal'
    identifiers = {'Id': 'id', 'Invoice': 'invoice_id', 'Client': 'client_id'}
    ordering = 'id desc'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering)


def get_act_by_id(object_id):
    return get_acts('Id', object_id)


def get_acts_by_client(object_id):
    return get_acts('Client', object_id)


def get_acts_by_invoice(object_id):
    return get_acts('Invoice', object_id)


def get_acts_by_fpa_invoice(object_id):
    # Get consumes list from fictive_personal_account invoice
    consume_list = get_consumes_by_invoice(object_id)
    act_trans_list = list()
    # TODO: doesn't support case of several acts in same consume
    # For each consume get act_trans (if exists)
    for consume in consume_list:
        result = get_act_trans_by_consume(consume['id'])
        if result:
            act_trans_list.append(result[0])
    acts_list = list()
    # TODO: doesn't support case of several rows in one act
    # Get acts by act_id
    for act_trans in act_trans_list:
        acts_list.extend(get_act_by_id(act_trans['act_id']))
    return acts_list


# ----------------------------------------------------------------------------------------------------------------------

def get_act_trans(classname, object_id):
    fields = ['id',
              'act_id',
              'amount',
              'amount_nds',
              'netting',
              'act_qty',
              'commission_type',
              'consume_id']
    table = 't_act_trans'
    identifiers = {'Act': 'act_id', 'Consume': 'consume_id', 'Id': 'id'}
    ordering = 'id desc'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering)


def get_act_trans_by_id(object_id):
    return get_act_trans('Id', object_id)


def get_act_trans_by_act(object_id):
    return get_act_trans('Act', object_id)


def get_act_trans_by_consume(object_id):
    return get_act_trans('Consume', object_id)


# ----------------------------------------------------------------------------------------------------------------------

def get_partner_acts(classname, object_id):
    fields = ['partner_contract_id',
              'place_id',
              'page_id',
              'description',
              'dt',
              'partner_reward_wo_nds']
    table = 't_partner_act_data'
    identifiers = {'Contract': 'partner_contract_id'}
    ordering = 'page_id'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering)


def get_acts_by_contract(object_id):
    return get_partner_acts('Contract', object_id)


# ----------------------------------------------------------------------------------------------------------------------

def get_requests(classname, object_id):
    fields = ['id',
              'dt',
              'invoice_dt',
              'client_id',
              'promo_code_id']
    table = 't_request'
    identifiers = {'Client': 'client_id', 'Id': 'id'}
    ordering = 'id desc'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering)


def get_requests_by_client(object_id):
    return get_requests('Client', object_id)


# Doesn't work with object_list
def get_request_by_invoice(object_id):
    request_id = get_invoices('Id', object_id)[0]['request_id']
    return get_requests('Id', request_id)


# ----------------------------------------------------------------------------------------------------------------------

def get_invoices(classname, object_id):
    fields = ['id',
              'dt',
              'request_id',
              'client_id',
              'paysys_id',
              'firm_id',
              'type',
              'credit',
              'promo_code_id',
              'consume_sum',
              'currency',
              'nds_pct',
              'transfer_acted',
              'total_sum',
              'person_id',
              'offer_type_id',
              'overdraft',
              'payment_term_dt',
              'payment_term_id',
              'receipt_sum',
              'total_act_sum',
              'effective_sum',
              'receipt_sum_1c',
              'extern',
              'postpay',
              'turn_on_dt',
              'iso_currency',
              'external_id',
              'contract_id']
    table = 't_invoice'
    identifiers = {'Client': 'client_id', 'Id': 'id', 'ExternalId': 'external_id', 'Contract': 'contract_id'}
    ordering = 'id desc'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering)


def get_invoice_by_id(object_id):
    return get_invoices('Id', object_id)


def get_invoice_by_eid(object_id):
    return get_invoices('ExternalId', object_id)


def get_invoice_by_charge_note_id(object_id):
    query = 'select repayment_invoice_id from BO.T_INVOICE_REPAYMENT where invoice_id = :item'
    repayment_invoice_id = balance().execute(query, {'item': object_id})
    if repayment_invoice_id:
        object_id = repayment_invoice_id[0]['repayment_invoice_id']
    return get_invoices('Id', object_id)


def get_y_invoices_by_fpa_invoice(object_id):
    acts_list = get_acts_by_fpa_invoice(object_id)
    y_invoice_list = list()
    for act in acts_list:
        y_invoice_list.extend(get_invoice_by_id(act['invoice_id']))
    return y_invoice_list


def get_invoices_by_contract_id(object_id):
    return get_invoices('Contract', object_id)


def get_invoices_by_client_id(object_id):
    return get_invoices('Client', object_id)


# ----------------------------------------------------------------------------------------------------------------------

def get_repayment_invoices(classname, object_id):
    fields = ['invoice_id',
              'repayment_invoice_id']

    table = 'T_INVOICE_REPAYMENT'
    identifiers = {'Invoice': 'invoice_id', 'Repayment': 'repayment_invoice_id'}
    ordering = 'invoice_id desc'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering)


def get_repayment_by_invoice(object_id):
    return get_repayment_invoices('Invoice', object_id)


def get_invoice_by_repayment(object_id):
    return get_repayment_invoices('Repayment', object_id)


# ----------------------------------------------------------------------------------------------------------------------

def get_receipts(classname, object_id):
    fields = ['invoice_id',
              'id',
              'receipt_sum']

    table = 'T_RECEIPT'
    identifiers = {'Invoice': 'invoice_id'}
    ordering = 'invoice_id desc'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering)


def get_receipt_by_invoice(object_id):
    return get_receipts('Invoice', object_id)


# ----------------------------------------------------------------------------------------------------------------------

def get_contracts(classname, object_id):
    fields = [
        'id',
        'client_id',
        'person_id',
        'external_id',
        'type',
    ]
    table = 't_contract2'
    identifiers = {'Client': 'client_id', 'Id': 'id', 'Person': 'person_id'}
    ordering = 'id desc'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering)


def get_contracts_by_client(object_id):
    return get_contracts('Client', object_id)


def get_contract_by_id(object_id):
    return get_contracts('Id', object_id)


# ----------------------------------------------------------------------------------------------------------------------

def get_contract_collaterals(classname, object_id):
    fields = [
        'id',
        'attribute_batch_id',
        'contract2_id',
        'num',
        'is_faxed',
        'is_signed',
        'is_cancelled',
        'dt'

    ]
    table = 't_contract_collateral'
    identifiers = {'Contract': 'contract2_id', 'Batch': 'attribute_batch_id'}
    ordering = 'attribute_batch_id'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering)


def sort_collaterals(collaterals):
    return sorted(collaterals, key=lambda x: (0 if x['num'] is None else 1, x['dt']))


def get_collaterals_by_contract(object_id):
    return sort_collaterals(get_contract_collaterals('Contract', object_id))


def get_collateral_by_batch_id(object_id):
    return sort_collaterals(get_contract_collaterals('Batch', object_id))


# ----------------------------------------------------------------------------------------------------------------------
def get_contract_attributes(classname, object_id):
    fields = [
        'id',
        'attribute_batch_id',
        'code',
        'value_num',
        'key_num',
        'value_str',
        'value_dt'
    ]
    table = 'T_CONTRACT_ATTRIBUTES'
    identifiers = {'Batch': 'attribute_batch_id'}
    ordering = 'id desc'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering)


def get_attributes_by_batch_id(object_id):
    return get_contract_attributes('Batch', object_id)


def get_attributes_by_attr_code(contract_id, attr_code, collateral_num=None):
    if not collateral_num:
        num_text = 'num is null'
    else:
        num_text = 'num = ' + "'" + str(collateral_num) + "'"
    attributes_data = balance().execute(
        "select * from t_contract_attributes \
                where collateral_id in \
                (select id from t_contract_collateral \
                where contract2_id = :contract_id and " + num_text + ") \
                and (code = :attr_code) \
                order by value_num",
        {'contract_id': contract_id, 'collateral_num': collateral_num if collateral_num else None,
         'attr_code': attr_code})
    return attributes_data[0]['value_clob'] or attributes_data[0]['value_dt'] or attributes_data[0]['value_num'] or attributes_data[0]['value_str']


# ----------------------------------------------------------------------------------------------------------------------

def get_persons(classname, object_id):
    fields = [
        'attribute_batch_id',
        'id',
        'client_id',
        'type',
        'inn',
        'kpp',
        'bik',
        'account',
        'invalid_address',
        'longname',
        'inn_doc_details',
        'dt',
        'fias_guid',
        'kladr_code',
        'legal_address_code',
        'legaladdress',
        'name'
    ]
    table = 'v_person'
    identifiers = {'Client': 'client_id', 'Id': 'id'}
    ordering = 'id desc'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering)


def get_persons_by_client(object_id):
    return get_persons('Client', object_id)


def get_person_by_id(object_id):
    return get_persons('Id', object_id)


# ----------------------------------------------------------------------------------------------------------------------
def get_clients(classname, object_id):
    fields = [
        'id',
        'region_id',
        'is_agency',
        'subregion_id',
        'manual_suspect',
        'overdraft_ban',
        'single_account_number'
    ]
    table = 't_client'
    identifiers = {'Id': 'id', 'Region': 'region_id'}
    ordering = 'id desc'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering)


def get_client_by_id(object_id):
    return get_clients('Id', object_id)


# ----------------------------------------------------------------------------------------------------------------------
def get_client_service_data_rows(classname, object_id):
    fields = [
        'class_id',
        'service_id',
        'iso_currency',
        'convert_type'
    ]
    table = 't_client_service_data'
    identifiers = {'Client': 'class_id'}
    ordering = 'class_id desc'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering)


def get_client_service_data_rows_by_client(object_id):
    return get_client_service_data_rows('Client', object_id)


# ----------------------------------------------------------------------------------------------------------------------
def get_products(classname, object_id):
    fields = [
        'id',
        'unit_id',
        'commission_type'
    ]
    table = 't_product'
    identifiers = {'Id': 'id'}
    ordering = 'id desc'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering)


def get_product_by_id(object_id):
    return get_products('Id', object_id)


# ----------------------------------------------------------------------------------------------------------------------
def get_prices(classname, object_id):
    fields = [
        'id',
        'product_id',
        'dt',
        'price',
        'tax',
        'iso_currency',
        'tax_policy_id',
        'tax_policy_pct_id'
    ]
    table = 't_price'
    identifiers = {'Product': 'product_id'}
    ordering = 'dt desc'
    and_statement = 'and hidden = 0'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering, and_statement)


def get_prices_by_product_id(object_id):
    return get_prices('Product', object_id)


# ----------------------------------------------------------------------------------------------------------------------

def get_payments(classname, object_id):
    fields = [
        'id',
        'invoice_id'
    ]
    table = 't_payment'
    identifiers = {'Id': 'id', 'Invoice': 'invoice_id'}
    ordering = 'id desc'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering)


def get_payments_by_invoice_id(object_id):
    return get_payments('Invoice', object_id)


# ----------------------------------------------------------------------------------------------------------------------

def get_units(classname, object_id):
    fields = [
        'id',
        'iso_currency',
        'precision'
    ]
    table = 't_product_unit'
    identifiers = {'Id': 'id'}
    ordering = 'id desc'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering)


def get_unit_by_id(object_id):
    return get_units('Id', object_id)


# ----------------------------------------------------------------------------------------------------------------------
def get_invoice_orders(classname, object_id):
    fields = [
        'id',
        'invoice_id',
        'quantity',
        'initial_quantity',
        'discount_pct',
        'amount_no_discount',
        'internal_price',
        'effective_sum',
        'amount'
    ]
    table = 't_invoice_order'
    identifiers = {'Invoice': 'invoice_id'}
    ordering = 'id'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering)


def get_invoice_orders_by_invoice_id(object_id):
    return get_invoice_orders('Invoice', object_id)


# ----------------------------------------------------------------------------------------------------------------------
def get_orders(classname, object_id):
    fields = [
        'id',
        'service_order_id',
        'service_id',
        'client_id',
        'agency_id',
        'main_order',
        'service_code',
        'product_currency',
        'product_iso_currency',
        'completion_fixed_qty',
        'group_order_id',
        'completion_qty',
        'child_ua_type'
    ]
    table = 't_order'
    identifiers = {'Id': 'id', 'ServiceOrderID': 'service_order_id', 'Client': 'client_id', 'Agency': 'agency_id',
                   'MainOrder': 'main_order'}
    ordering = 'id desc'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering)


def get_orders_from_invoice_order(classname, object_id):
    fields = [
        'invoice_id',
        'order_id',
        'quantity',
        'service_code',
        'child_ua_type'
    ]
    table = 't_invoice_order'
    identifiers = {'InvoiceId': 'invoice_id', 'OrderId': 'order_id'}
    ordering = 'order_id desc'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering)


def get_order_by_client(object_id):
    return get_orders('Client', object_id)


def get_order_by_id(object_id):
    return get_orders('Id', object_id)


def get_order_by_service_id_and_service_order_id(service_id, service_order_id):
    return balance().execute(
        '''SELECT * FROM t_order WHERE service_id = :service_id AND service_order_id = :service_order_id''',
        {'service_id': service_id, 'service_order_id': service_order_id})


def get_orders_by_invoice(object_id):
    return get_orders_from_invoice_order('InvoiceId', object_id)


# ----------------------------------------------------------------------------------------------------------------------

def get_messages(classname, object_id):
    fields = [
        'id',
        'object_id'
    ]
    table = 't_message'
    identifiers = {'ObjectId': 'object_id', 'Id': 'id'}
    ordering = 'id desc'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering)


def get_message_by_object_id(object_id):
    return get_messages('ObjectId', object_id)


# ----------------------------------------------------------------------------------------------------------------------
def get_promocodes(classname, object_id):
    fields = [
        'id',
        'code',
        'bonus1',
        'bonus2',
        'start_dt',
        'middle_dt',
        'end_dt',
        'reservation_days',
        'multicurrency_bonuses',
        'group_id'
    ]
    table = 't_promo_code'
    identifiers = {'Id': 'id', 'Code': 'code'}
    ordering = 'id desc'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering)


def get_promocodes2(classname, object_id):
    fields = [
        'id',
        'code',
        'group_id'
    ]
    table = 't_promo_code_2'
    identifiers = {'Id': 'id', 'Code': 'code'}
    ordering = 'id desc'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering)


def get_promocode_group(classname, object_id):
    fields = [
        'id',
        'start_dt',
        'end_dt',
        'reservation_days',
        'calc_params'
    ]
    table = 't_promo_code_group'
    identifiers = {'Id': 'id', 'Code': 'code'}
    ordering = 'id desc'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering)


def get_promocode_group_by_group_id(object_id):
    return get_promocode_group('Id', object_id)


def get_promocode_group_by_promocode_id(object_id):
    group_id = get_promocode_by_id(object_id=object_id)[0]['group_id']
    return get_promocode_group('Id', group_id)


def get_promocode_by_code(object_id):
    return get_promocodes('Code', object_id)


def get_promocode2_by_code(object_id):
    return get_promocodes2('Code', object_id)


def get_promocode_by_id(object_id):
    return get_promocodes('Id', object_id)


def get_promocode2_by_id(object_id):
    return get_promocodes2('Id', object_id)


def get_promocode2_by_id(object_id):
    return get_promocodes2('Id', object_id)


def get_promocode_by_invoice(object_id):
    promo_code_id = get_invoice_by_id(object_id)[0]['promo_code_id']
    return get_promocode_by_id(promo_code_id)


def get_promocodes_reservations(classname, object_id):
    fields = [
        'client_id',
        'promocode_id',
        'begin_dt'
    ]
    table = 't_promo_code_reservation'
    identifiers = {'Client_id': 'client_id', 'Promocode_id': 'promocode_id'}
    ordering = 'promocode_id desc'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering)


def get_promocodes_reservation_by_code(object_id):
    promocode_id = get_promocode_by_code(object_id)[0]['id']
    return get_promocodes_reservations('Promocode_id', promocode_id)


def get_promocodes_reservation_by_promocode_id(object_id):
    return get_promocodes_reservations('Promocode_id', object_id)


# ----------------------------------------------------------------------------------------------------------------------
def get_shipments(classname, object_dict):
    fields = [
        'service_order_id',
        'service_id',
        'money',
        'days',
        'units'
    ]
    table = 't_shipment'
    identifiers = {'ServiceOrderID': 'service_order_id'}
    ordering = 'dt desc'
    and_statement = 'and service_id = {0}'.format(object_dict['service_id'])
    return get_custom_data(table, fields, identifiers, classname, object_dict['service_order_id'], ordering,
                           and_statement)


def get_shipments_by_service_order_id(object_dict):
    return get_shipments('ServiceOrderID', object_dict)


# ----------------------------------------------------------------------------------------------------------------------
def get_apikeys_tariff(classname, object_id):
    fields = [
        'id',
        'cc',
    ]
    table = 't_tariff'
    identifiers = {'CC': 'cc'}
    ordering = 'id desc'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering)


def get_apikeys_tariff_by_cc(object_id):
    return get_apikeys_tariff('CC', object_id)


# ----------------------------------------------------------------------------------------------------------------------
def get_apikeys_tariff_group(classname, object_id):
    fields = [
        'id',
        'cc',
    ]
    table = 't_tariff_group'
    identifiers = {'ServiceID': 'service_id'}
    ordering = 'id desc'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering)


def get_apikeys_tariff_group_by_service(object_id):
    return get_apikeys_tariff_group('ServiceID', object_id)


def get_apikeys_tariff_groups():
    return BalanceBO().execute('SELECT id,cc,name FROM t_tariff_group  ORDER BY id DESC')


# ----------------------------------------------------------------------------------------------------------------------
def get_deferpays(classname, object_id):
    fields = [
        'id',
        'request_id',
        'orig_request_id',
        'effective_sum',
        'operation_id',
        'client_id',
        'paysys_id',
        'person_id',
        'invoice_id'
    ]
    table = 't_deferpay'
    identifiers = {'Id': 'id', 'Invoice': 'invoice_id'}
    ordering = 'id desc'
    deferpays = get_custom_data(table, fields, identifiers, classname, object_id, ordering)
    for deferpay in deferpays:
        deferpay['request_id'] = int(deferpay['request_id'])
        deferpay['orig_request_id'] = int(deferpay['orig_request_id'])
    return deferpays


def get_deferpays_by_invoice(object_id):
    return get_deferpays('Invoice', object_id)


# ----------------------------------------------------------------------------------------------------------------------
def get_operations(classname, object_id):
    fields = [
        'id',
        'type_id',
        'passport_id',
        'create_traceback_id',
        'insert_traceback_id',
        'invoice_id',
        'parent_operation_id'
    ]
    table = 't_operation'
    identifiers = {'Id': 'id', 'Invoice': 'invoice_id'}
    ordering = 'id desc'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering)


def get_operation_by_id(object_id):
    return get_operations('Id', object_id)


def get_operation_by_act(act_id):
    act = get_act_by_id(act_id)
    operation_id = act[0]['operation_id']
    return get_operation_by_id(operation_id)


# ----------------------------------------------------------------------------------------------------------------------

def get_reverses(classname, object_id):
    fields = [
        'id',
        'consume_id',
        'invoice_id',
        'operation_id'
    ]
    table = 't_reverse'
    identifiers = {'Id': 'id', 'Invoice': 'invoice_id'}
    ordering = 'id desc'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering)


def get_reverse_by_invoice(object_id):
    return get_reverses('Invoice', object_id)


# ----------------------------------------------------------------------------------------------------------------------

def get_service(classname, object_id):
    fields = [
        'id',
        'token',
        'contract_needed',
        'restrict_client'
    ]
    table = 'v_service'
    identifiers = {'Id': 'id'}
    ordering = 'id desc'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering)


def get_service_by_id(object_id):
    return get_service('Id', object_id)


# ----------------------------------------------------------------------------------------------------------------------

def get_payment(classname, object_id):
    fields = [
        'id',
        'dt',
        'invoice_id',
        'postauth_dt',
        'creator_uid',
        'paysys_code',
        'amount',
        'currency',
        'transaction_id',
        'purchase_token',
        'card_holder'
    ]
    table = 't_payment'
    identifiers = {'Id': 'id', 'Invoice': 'invoice_id', 'TrustPayment': 'trust_payment_id'}
    ordering = 'id desc'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering)


def get_payments_by_invoice(object_id):
    return get_payment('Invoice', object_id)


def get_payments_by_trust_payment_id(object_id):
    return get_payment('TrustPayment', object_id)


# ----------------------------------------------------------------------------------------------------------------------

def get_passport(classname, object_id):
    fields = [
        'passport_id',
        'client_id',
        'login'
    ]
    table = 't_passport'
    identifiers = {'Login': 'login', 'PassportId': 'passport_id'}
    ordering = 'passport_id desc'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering)


def get_passport_by_login(object_id):
    return get_passport('Login', object_id)


def get_passport_by_passport_id(object_id):
    return get_passport('PassportId', object_id)


# ----------------------------------------------------------------------------------------------------------------------

def get_role(classname, object_id):
    fields = [
        'passport_id',
        'role_id'
    ]
    table = 't_role_user'
    identifiers = {'Passport': 'passport_id'}
    ordering = 'passport_id desc'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering)


def get_role_by_passport(object_id):
    return get_role('Passport', object_id)


# ----------------------------------------------------------------------------------------------------------------------

def get_role_client_user(classname, object_id):
    fields = [
        'passport_id',
        'client_id',
        'role_id'
    ]
    table = 't_role_client_user'
    identifiers = {'Passport': 'passport_id'}
    ordering = 'passport_id desc'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering)


def get_role_client_user_by_passport(object_id):
    return get_role_client_user('Passport', object_id)


# ----------------------------------------------------------------------------------------------------------------------


def get_oebs_cash_payment_fact(classname, object_id):
    fields = [
        'receipt_number',
        'xxar_cash_fact_id',
    ]
    table = 't_oebs_cash_payment_fact'
    identifiers = {'ReceiptNumber': 'receipt_number'}
    ordering = 'xxar_cash_fact_id desc'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering)


def get_oebs_cash_payment_fact_by_receipt_number(object_id):
    return get_oebs_cash_payment_fact('ReceiptNumber', object_id)


# ----------------------------------------------------------------------------------------------------------------------


def get_invoice_refund(classname, object_id):
    fields = [
        'id',
        'invoice_id',
    ]
    table = 't_invoice_refund'
    identifiers = {'Invoice': 'invoice_id'}
    ordering = 'id desc'
    return get_custom_data(table, fields, identifiers, classname, object_id, ordering)


def get_invoice_refund_by_invoice_id(object_id):
    return get_invoice_refund('Invoice', object_id)


# ----------------------------------------------------------------------------------------------------------------------


def get_currency_rate(dt, cc, rate_src_id):
    reporter.log(cc)
    return balance().execute(
        'SELECT * FROM t_currency_rate_v2 WHERE cc = :cc AND rate_dt = :dt AND rate_src_id = :rate_src_id',
        {'cc': cc, 'dt': utils.Date.nullify_time_of_date(dt), 'rate_src_id': rate_src_id})


# ----------------------------------------------------------------------------------------------------------------------

def get_currency(id):
    return balance().execute(
        'SELECT * FROM t_currency WHERE num_code = :id',
        {'id': id})


def get_order_id(trust_payment_id):
    with reporter.step(u'Получаем id заказа для платежа: {}'.format(trust_payment_id)):
        query = "SELECT ro.PARENT_ORDER_ID order_id " \
                "FROM T_CCARD_BOUND_PAYMENT cc JOIN T_REQUEST_ORDER ro ON cc.REQUEST_ID=ro.REQUEST_ID " \
                "WHERE cc.TRUST_PAYMENT_ID=:trust_payment_id"
        params = {'trust_payment_id': trust_payment_id}
        return balance().execute(query, params)[0]['order_id']


def get_payment_rows(trust_payment_id):
    with reporter.step(u'Получаем payment_rows для платежа: {}'.format(trust_payment_id)):
        query = "SELECT PAYMENT_ROWS FROM BO.T_PAYMENT WHERE TRUST_PAYMENT_ID = :trust_payment_id"
        params = {'trust_payment_id': trust_payment_id}
        payment_rows = balance().execute(query, params)[0]['payment_rows']
        return json.loads(payment_rows)


def clear_client(user):
    query = "UPDATE T_ACCOUNT SET CLIENT_ID=NULL WHERE PASSPORT_ID=:uid"
    params = {'uid': user.id_}
    balance().execute(query, params)


# def get_apikeys_tariff_id(tariff):
#     query = 'select id from t_tariff where cc = :cc'
#     return balance().execute(query,{'cc': tariff})

if __name__ == "__main__":
    a = oracle_update('t_contract_collateral', {'dt': 12}, {'contract_id': 123435, 'num': None})
    pass
