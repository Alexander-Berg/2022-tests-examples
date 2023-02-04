# -*- coding: utf-8 -*-

from decimal import Decimal
import datetime
import pytest
from enum import Enum
from hamcrest import has_length
import os
import json

import btestlib.reporter as reporter
from balance import balance_db as db
from balance import balance_steps as steps
from balance import snout_steps
from balance.tests.conftest import get_free_user
from balance.features import Features
from btestlib import utils as utils
from btestlib.constants import Paysyses, PersonTypes, Firms, ContractCommissionType, Products
from btestlib.data.person_defaults import InnType
from btestlib.matchers import equal_to_casted_dict, contains_dicts_equal_to
from export_commons import Locators, get_order_eid, get_balance_tax_policy_nds_pct, get_oebs_client_party_id, \
    get_oebs_person_cust_account_id, get_oebs_person_cust_account_role_id, get_oebs_inventory_item_id, \
    read_attr_values, read_attr_values_list, create_contract_postpay, get_balance_firm_oebs_org_id
from temp.igogor.balance_objects import Context, Contexts
import btestlib.config as balance_config

pytestmark = [reporter.feature(Features.OEBS, Features.INVOICE),
              pytest.mark.docpath('https://wiki.yandex-team.ru/balance/docs/process/oebs')]

try:
    import balance_contracts
    from balance_contracts.oebs.invoice import replace_mask
    from balance_contracts.contract_utils import utils as contract_utils
    from balance_contracts.contract_utils import deep_equals
    json_contracts_repo_path = os.path.dirname(os.path.abspath(balance_contracts.__file__))
except ImportError as err:
    json_contracts_repo_path = ''

JSON_OEBS_PATH = '/oebs/invoice/'

'''
Посмотреть в коде какие атрибуты, как и куда выгружаются можно тут balance/processors/oebs/__init__.py
'''


class INV_ATTRS(object):
    class INVOICE(Enum):
        currency = Locators(
            balance=lambda b: b['t_invoice.iso_currency'],
            oebs=lambda o: o['ra_customer_trx_all.invoice_currency_code'])

        client_party_id = Locators(
            balance=lambda b: get_oebs_client_party_id(b['t_invoice.firm_id'], b['t_invoice.client_id']),
            oebs=lambda o: o['ra_customer_trx_all.attribute9'])

        bill_to_customer_id = Locators(
            balance=lambda b: get_oebs_person_cust_account_id(b['t_invoice.firm_id'], b['t_invoice.person_id']),
            oebs=lambda o: o['ra_customer_trx_all.bill_to_customer_id'])

        # установлено опытным путем. это правильно?
        # в этом поле в оебс сумма хранится с запятой вместо точки
        sum = Locators(
            balance=lambda b: Decimal(str(b['t_invoice.total_sum'])),
            oebs=lambda o: Decimal(str(o['ra_customer_trx_all.global_attribute2'].replace(',', '.'))))

        # баланс выгружает дату строкой в attribute2, в trx_date видимо кладется тоже самое только в формате даты
        dt = Locators(
            balance=lambda b: utils.Date.nullify_time_of_date(b['t_invoice.dt']),
            oebs=lambda o: o['ra_customer_trx_all.trx_date'])

        # баланс выгружает в поле term_id, а в term_due_date видимо лежит уже вычисленная из него дата
        payment_term_dt = Locators(
            balance=lambda b: utils.Date.nullify_time_of_date(b['t_invoice.payment_term_dt'] or b['t_invoice.dt']),
            oebs=lambda o: o['ra_customer_trx_all.term_due_date'])

    class BILL_TO_CONTACT_PH(Enum):
        # проверяем, что значение пустое
        bill_to_contact = \
            Locators(balance=lambda b: '',
                     oebs=lambda o: o['ra_customer_trx_all.bill_to_contact_id'])

    class BILL_TO_CONTACT_NON_PH(Enum):
        bill_to_contact = Locators(
            balance=lambda b: get_oebs_person_cust_account_role_id(b['t_invoice.firm_id'], b['t_invoice.person_id']),
            oebs=lambda o: o['ra_customer_trx_all.bill_to_contact_id'])

    # заполняется у ЮЛ с регионом Россия и Украина (и Турция?)
    class SHIP_TO_CUSTOMER(Enum):
        ship_to_customer = Locators(
            balance=lambda b: get_oebs_person_cust_account_id(b['t_invoice.firm_id'], b['t_invoice.person_id']),
            oebs=lambda o: o['ra_customer_trx_all.ship_to_customer_id'])

    class SHIP_TO_CUSTOMER_EMPTY(Enum):
        # проверяем, что значение пустое
        ship_to_customer = Locators(balance=lambda b: '',
                                    oebs=lambda o: o['ra_customer_trx_all.ship_to_customer_id'])

    # Наименование счета в оебс

    class INVOICE_TYPE_RU_PREPAY(Enum):
        invoice_type = Locators(balance=lambda b: u'Счет на оплату',
                                oebs=lambda o: o['ra_cust_trx_types_all.name'])

    class INVOICE_TYPE_RU_PERSONAL(Enum):
        invoice_type = Locators(balance=lambda b: u'Лицевой счет',
                                oebs=lambda o: o['ra_cust_trx_types_all.name'])

    class INVOICE_TYPE_UA(Enum):
        invoice_type = Locators(balance=lambda b: u'Счет на оплату Укр',
                                oebs=lambda o: o['ra_cust_trx_types_all.name'])

    # us, sw_personal
    class INVOICE_TYPE_ACCOUNT(Enum):
        invoice_type = Locators(balance=lambda b: u'Account',
                                oebs=lambda o: o['ra_cust_trx_types_all.name'])

    # sw_prepay
    class INVOICE_TYPE_ACCOUNT_BILL(Enum):
        invoice_type = Locators(balance=lambda b: u'Account_bill',
                                oebs=lambda o: o['ra_cust_trx_types_all.name'])

    class ELS_NUMBER(Enum):
        els_number = Locators(balance=lambda b: str(b['t_client.single_account_number']),
                              oebs=lambda o: o['ra_customer_trx_all.global_attribute22'])


class ORDER_ATTRS(object):
    class ORDER(Enum):
        # количество товара в заказе
        quantity = \
            Locators(balance=lambda b: Decimal(str(b['t_invoice_order.quantity'])) /
                                       Decimal(str(b['t_invoice_order.type_rate'])),
                     oebs=lambda o: Decimal(str(o['ra_customer_trx_lines_all.line.quantity_invoiced'])))

        # налог в заказе
        tax_rate = \
            Locators(balance=lambda b: get_balance_tax_policy_nds_pct(b['t_invoice_order.tax_policy_pct_id']),
                     oebs=lambda o: o['ra_customer_trx_lines_all.tax.tax_rate'])

        # продукт в заказе
        product_inventory_item_id = \
            Locators(balance=lambda b: get_oebs_inventory_item_id(b['t_order.service_code']),
                     oebs=lambda o: o['ra_customer_trx_lines_all.line.inventory_item_id'])

        # название заказа
        description = \
            Locators(balance=lambda b: b['t_order.text'],
                     oebs=lambda o: o['ra_customer_trx_lines_all.line.description'])

        # номер заказа
        eid = \
            Locators(balance=lambda b: get_order_eid(b['t_order.service_id'], b['t_order.service_order_id']),
                     oebs=lambda o: o['ra_customer_trx_lines_all.line.attribute12'])

        sum_wo_nds = \
            Locators(balance=lambda b: Decimal(str(b['t_invoice_order.amount'])) -
                                       Decimal(str(b['t_invoice_order.amount_nds'])),
                     oebs=lambda o: Decimal(str(o['ra_customer_trx_lines_all.line.extended_amount'])))

        nds_amount = \
            Locators(balance=lambda b: Decimal(str(b['t_invoice_order.amount_nds'])),
                     oebs=lambda o: Decimal(str(o['ra_customer_trx_lines_all.tax.extended_amount'])))

    class ORDER_PERSONAL(Enum):
        # название заказа
        description = \
            Locators(balance=lambda b: u'фиктивная строка',
                     oebs=lambda o: o['ra_customer_trx_lines_all.line.description'])

        # номер заказа
        eid = \
            Locators(balance=lambda b: '',
                     oebs=lambda o: o['ra_customer_trx_lines_all.line.attribute12'])


def get_balance_data(invoice_id):
    balance_invoice_data = {}

    # t_invoice
    query = "SELECT * FROM t_invoice WHERE id = :invoice_id"
    result = db.balance().execute(query, {'invoice_id': invoice_id}, single_row=True)
    balance_invoice_data.update(utils.add_key_prefix(result, 't_invoice.'))

    # t_client
    query = "SELECT single_account_number FROM t_client WHERE id = :client_id"
    result = db.balance().execute(query, {'client_id': balance_invoice_data['t_invoice.client_id']}, single_row=True)
    balance_invoice_data.update(utils.add_key_prefix(result, 't_client.'))

    # данные по заказам счета
    balance_orders_data_list = []

    if balance_invoice_data['t_invoice.type'] == 'personal_account':
        # лицевые выгружаются без заказов! заказы грузятся с актом
        balance_orders_data_list.append({})
    else:
        # t_invoice_order
        query = "SELECT * FROM t_invoice_order WHERE invoice_id = :invoice_id"
        result = db.balance().execute(query, {'invoice_id': invoice_id})
        for invoice_order in result:
            balance_orders_data_list.append(utils.add_key_prefix(invoice_order, 't_invoice_order.'))

        for order_data in balance_orders_data_list:
            # t_order
            query = "SELECT * FROM t_order WHERE id = :order_id"
            result = db.balance().execute(query, {'order_id': order_data['t_invoice_order.order_id']}, single_row=True)
            order_data.update(utils.add_key_prefix(result, 't_order.'))

    return balance_invoice_data, balance_orders_data_list


def get_oebs_data(balance_invoice_data):
    invoice_eid = balance_invoice_data['t_invoice.external_id']
    firm_id = balance_invoice_data['t_invoice.firm_id']

    oebs_invoice_data = get_oebs_invoice_data(invoice_eid, firm_id)

    # данные по заказам счета
    customer_trx_id = oebs_invoice_data['ra_customer_trx_all.customer_trx_id']
    oebs_orders_data_list = get_oebs_orders_data_list(customer_trx_id, firm_id)

    return oebs_invoice_data, oebs_orders_data_list


def get_oebs_invoice_data(balance_object_eid, firm_id):
    oebs_data = {}
    oebs_org_id = get_balance_firm_oebs_org_id(firm_id)

    # ra_customer_trx_all
    query = u"select * from apps.ra_customer_trx_all where trx_number = '{balance_eid}' and org_id = {org_id}".format(
        balance_eid=balance_object_eid, org_id=oebs_org_id)
    result = db.oebs().execute_oebs(firm_id, query, single_row=True)
    oebs_data.update(utils.add_key_prefix(result, 'ra_customer_trx_all.'))

    # ra_cust_trx_types_all
    query = u"SELECT * FROM apps.ra_cust_trx_types_all WHERE cust_trx_type_id = :oebs_type_id AND org_id = :org_id"
    result = db.oebs().execute_oebs(firm_id, query,
                                    {'oebs_type_id': oebs_data['ra_customer_trx_all.cust_trx_type_id'],
                                     'org_id': oebs_org_id}, single_row=True)
    oebs_data.update(utils.add_key_prefix(result, 'ra_cust_trx_types_all.'))

    return oebs_data


def get_oebs_orders_data_list(customer_trx_id, firm_id):
    oebs_order_data_list = []

    # ra_customer_trx_lines_all.line
    line_type = 'LINE'
    query = "SELECT * FROM apps.ra_customer_trx_lines_all WHERE customer_trx_id = :trx_id AND line_type = :line_type"
    result = db.oebs().execute_oebs(firm_id, query, {'trx_id': customer_trx_id, 'line_type': line_type})
    for result_line in result:
        oebs_order_data_list.append(utils.add_key_prefix(result_line,
                                                         'ra_customer_trx_lines_all.{}.'.format(line_type.lower())))

    for order_data in oebs_order_data_list:
        # ra_customer_trx_lines_all.tax
        line_type = 'TAX'
        query = "SELECT * FROM apps.ra_customer_trx_lines_all " \
                "WHERE customer_trx_id = :trx_id AND line_type = :line_type AND link_to_cust_trx_line_id = :line_id"
        result = db.oebs().execute_oebs(firm_id, query,
                                        {'trx_id': customer_trx_id,
                                         'line_type': line_type,
                                         'line_id': order_data['ra_customer_trx_lines_all.line.customer_trx_line_id']},
                                        single_row=True)
        order_data.update(utils.add_key_prefix(result, 'ra_customer_trx_lines_all.{}.'.format(line_type.lower())))

    return oebs_order_data_list


# todo-blubimov методы, которые можно унифицировать для использования во всех тестах на выгрузку в оебс
# ====================================================================================================================

def check_attrs(invoice_id, invoice_attrs_list, order_attrs_list=ORDER_ATTRS.ORDER):
    with reporter.step(u'Считываем данные из баланса'):
        balance_invoice_data, balance_orders_data_list = get_balance_data(invoice_id)
    with reporter.step(u'Считываем данные из ОЕБС'):
        oebs_invoice_data, oebs_orders_data_list = get_oebs_data(balance_invoice_data)

    balance_invoice_values, oebs_invoice_values = \
        read_attr_values(invoice_attrs_list, balance_invoice_data, oebs_invoice_data)

    utils.check_that(oebs_invoice_values, equal_to_casted_dict(balance_invoice_values),
                     step=u'Проверяем корректность общих данных счета в ОЕБС')

    balance_orders_values_list, oebs_orders_values_list = \
        read_attr_values_list(order_attrs_list, balance_orders_data_list, oebs_orders_data_list)

    utils.check_that(oebs_orders_values_list, contains_dicts_equal_to(balance_orders_values_list),
                     step=u'Проверяем корректность данных по заказам в ОЕБС')
    utils.check_that(oebs_orders_values_list, has_length(len(balance_orders_values_list)),
                     step=u'Проверяем, что количество заказов одинаково')


# ====================================================================================================================

# def create_exported_prepay_invoice(context):
#     with reporter.step(u'Выставляем предоплатный счет'):
#         client_id = steps.ClientSteps.create(prevent_oebs_export=True)
#         person_id = steps.PersonSteps.create(client_id, context.person_type.code, inn_type=InnType.RANDOM)
#         campaigns_list = [
#             {'client_id': client_id, 'service_id': context.service.id, 'product_id': context.product.id, 'qty': 100},
#             {'client_id': client_id, 'service_id': context.service.id, 'product_id': context.product.id, 'qty': 12.33}
#         ]
#         invoice_id, _, _, _ = steps.InvoiceSteps.create_force_invoice(client_id=client_id,
#                                                                       person_id=person_id,
#                                                                       campaigns_list=campaigns_list,
#                                                                       paysys_id=context.paysys.id,
#                                                                       prevent_oebs_export=True)
#
#     if balance_config.ENABLE_SINGLE_ACCOUNT:
#         invoice_id = db.get_invoice_by_charge_note_id(invoice_id)[0]['id']
#
#     steps.ExportSteps.export_oebs(client_id=client_id,
#                                   person_id=person_id,
#                                   invoice_id=invoice_id)
#
#     return invoice_id


# def create_exported_overdraft_invoice(context):
#     QTY = 10
#     with reporter.step(u'Выставляем овердрафтный счет'):
#         client_id = steps.ClientSteps.create(prevent_oebs_export=True)
#         steps.OverdraftSteps.set_force_overdraft(client_id, context.service.id, QTY * 1000, context.paysys.firm.id)
#         person_id = steps.PersonSteps.create(client_id, context.person_type.code, inn_type=InnType.RANDOM)
#         campaigns_list = [
#             {'client_id': client_id, 'service_id': context.service.id, 'product_id': context.product.id, 'qty': QTY}
#         ]
#         invoice_id, _, _, _ = steps.InvoiceSteps.create_force_invoice(client_id=client_id,
#                                                                       person_id=person_id,
#                                                                       campaigns_list=campaigns_list,
#                                                                       paysys_id=context.paysys.id,
#                                                                       overdraft=1,
#                                                                       prevent_oebs_export=True)
#
#     steps.ExportSteps.export_oebs(client_id=client_id,
#                                   person_id=person_id,
#                                   invoice_id=invoice_id)
#
#     return invoice_id


# def create_exported_postpay_invoice(context, force_convert_to_fictive_credit_scheme=False):
#     with reporter.step(u'Выставляем постоплатный счет'):
#         client_id = steps.ClientSteps.create(prevent_oebs_export=True)
#         person_id = steps.PersonSteps.create(client_id, context.person_type.code, inn_type=InnType.RANDOM)
#
#         contract_id, _ = create_contract_postpay(client_id, person_id, context)
#
#         if force_convert_to_fictive_credit_scheme:
#             steps.ContractSteps.force_convert_to_fictive_credit_scheme(contract_id)
#
#         campaigns_list = [
#             {'client_id': client_id, 'service_id': context.service.id, 'product_id': context.product.id, 'qty': 100}
#         ]
#         fictive_invoice_id, _, _, _ = steps.InvoiceSteps.create_force_invoice(client_id=client_id,
#                                                                               person_id=person_id,
#                                                                               campaigns_list=campaigns_list,
#                                                                               paysys_id=context.paysys.id,
#                                                                               credit=1,
#                                                                               contract_id=contract_id)
#         repayment_invoice_id = steps.InvoiceSteps.make_repayment_invoice(fictive_invoice_id,
#                                                                          prevent_oebs_export=True)[0]
#
#     steps.ExportSteps.export_oebs(client_id=client_id,
#                                   person_id=person_id,
#                                   contract_id=contract_id,
#                                   invoice_id=repayment_invoice_id)
#
#     return repayment_invoice_id


# def create_exported_personal_invoice(context):
#     with reporter.step(u'Выставляем лицевой счет'):
#         client_id = steps.ClientSteps.create(prevent_oebs_export=True)
#         person_id = steps.PersonSteps.create(client_id, context.person_type.code, inn_type=InnType.RANDOM)
#
#         contract_id, _ = create_contract_postpay(client_id, person_id, context)
#
#         campaigns_list = [
#             {'client_id': client_id, 'service_id': context.service.id, 'product_id': context.product.id, 'qty': 100}
#         ]
#         invoice_id, _, _, _ = steps.InvoiceSteps.create_force_invoice(client_id=client_id,
#                                                                       person_id=person_id,
#                                                                       campaigns_list=campaigns_list,
#                                                                       paysys_id=context.paysys.id,
#                                                                       credit=1,
#                                                                       contract_id=contract_id,
#                                                                       prevent_oebs_export=True)
#
#     steps.ExportSteps.export_oebs(client_id=client_id,
#                                   person_id=person_id,
#                                   contract_id=contract_id,
#                                   invoice_id=invoice_id)
#
#     return invoice_id


# def create_exported_single_personal_account(user, context):
#     client_id = steps.ClientSteps.create({'REGION_ID': 225}, single_account_activated=True, enable_single_account=True)
#     steps.ClientSteps.link(client_id, user.login)
#     person_id = steps.PersonSteps.create(client_id, 'ur')
#
#     paysys_id = context.paysys.id
#     session, token = snout_steps.CartSteps.get_session_and_token(client_id)
#
#     service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
#
#     order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
#                                        product_id=Products.DIRECT_FISH.id, params={'AgencyID': None})
#
#     add_res = snout_steps.CartSteps.post_item_add(session, context.service.id, service_order_id, 1.000003, None, token)
#     item_ids = [i['id'] for i in add_res.json()['data']['items']]
#
#     snout_steps.CartSteps.post_create_request(session, _csrf=token, item_ids=item_ids)
#
#     query = 'select id from T_REQUEST where client_id = :item'
#     request_id = db.balance().execute(query, {'item': client_id})[0]['id']
#
#     invoice_id, external_id, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=0,
#                                                            contract_id=None, overdraft=0, endbuyer_id=None)
#     els_number = steps.ElsSteps.get_els_number_from_client(client_id)
#     invoice_id, _ = steps.ElsSteps.get_ls_by_person_id_and_els_number(person_id, els_number)
#
#     steps.InvoiceSteps.pay(invoice_id)
#     steps.ExportSteps.export_oebs(client_id=client_id,
#                                   person_id=person_id,
#                                   invoice_id=invoice_id)
#
#     return invoice_id


def check_json_contract(invoice_id, person_id, json_file):
    try:
        db.balance().execute(
            """update t_person_firm set oebs_export_dt = sysdate where person_id = :person_id""",
            {'person_id': person_id})
    except Exception:
        pass

    steps.ExportSteps.init_oebs_api_export('Invoice', invoice_id)
    actual_json_data = steps.ExportSteps.get_json_data('Invoice', invoice_id)

    steps.ExportSteps.log_json_contract_actions(json_contracts_repo_path,
                                                JSON_OEBS_PATH,
                                                json_file,
                                                balance_config.FIX_CURRENT_JSON_CONTRACT)

    contract_utils.process_json_contract(json_contracts_repo_path,
                                         JSON_OEBS_PATH,
                                         json_file,
                                         actual_json_data,
                                         replace_mask,
                                         balance_config.FIX_CURRENT_JSON_CONTRACT)


# todo-blubimov здесь и в export_commons_acts контексты дублируются. нужнжо что-то с этим сделать
class PrepayContext(utils.ConstantsContainer):
    constant_type = Context

    BANK_UR_RUB = Contexts.DIRECT_FISH_RUB_CONTEXT.new(paysys=Paysyses.BANK_UR_RUB)
    BANK_PH_RUB = Contexts.DIRECT_FISH_RUB_CONTEXT.new(paysys=Paysyses.BANK_PH_RUB, person_type=PersonTypes.PH)

    BANK_YT_RUB = Contexts.DIRECT_FISH_YT_RUB_CONTEXT.new(paysys=Paysyses.BANK_YT_RUB)
    # todo-blubimov нужно использовать paysys=1060 или 11101060 ?
    # todo-blubimov должны выгружаться в фирму 1 или 111 ? (в t_person_category фирма 1)
    BANK_YTUR_KZT = Contexts.MARKET_RUB_CONTEXT.new(person_type=PersonTypes.YT_KZU, paysys=Paysyses.BANK_YTUR_KZT)
    BANK_YTPH_KZT = Contexts.MARKET_RUB_CONTEXT.new(person_type=PersonTypes.YT_KZP, paysys=Paysyses.BANK_YTPH_KZT)
    BANK_UA_UR_UAH = Contexts.DIRECT_FISH_UAH_CONTEXT.new(paysys=Paysyses.BANK_UA_UR_UAH)
    BANK_UA_PH_UAH = Contexts.DIRECT_FISH_UAH_CONTEXT.new(person_type=PersonTypes.PU,
                                                          paysys=Paysyses.BANK_UA_PH_UAH)
    BANK_US_UR_USD = Contexts.DIRECT_FISH_USD_CONTEXT.new(paysys=Paysyses.BANK_US_UR_USD)
    BANK_US_PH_USD = Contexts.DIRECT_FISH_USD_CONTEXT.new(person_type=PersonTypes.USP,
                                                          paysys=Paysyses.BANK_US_PH_USD)
    BANK_SW_UR_EUR = Contexts.DIRECT_FISH_SW_EUR_CONTEXT.new(paysys=Paysyses.BANK_SW_UR_EUR)
    BANK_SW_PH_EUR = Contexts.DIRECT_FISH_SW_EUR_CONTEXT.new(person_type=PersonTypes.SW_PH,
                                                             paysys=Paysyses.BANK_SW_PH_EUR)
    BANK_SW_YT_EUR = Contexts.DIRECT_FISH_SW_EUR_CONTEXT.new(person_type=PersonTypes.SW_YT,
                                                             paysys=Paysyses.BANK_SW_YT_EUR)
    BANK_SW_YTPH_EUR = Contexts.DIRECT_FISH_SW_EUR_CONTEXT.new(person_type=PersonTypes.SW_YTPH,
                                                               paysys=Paysyses.BANK_SW_YTPH_EUR)
    CC_BY_YTPH_RUB = Contexts.DIRECT_FISH_SW_EUR_CONTEXT.new(person_type=PersonTypes.BY_YTPH,
                                                             paysys=Paysyses.CC_BY_YTPH_RUB)
    BANK_TR_UR_TRY = Contexts.DIRECT_FISH_TRY_CONTEXT.new(paysys=Paysyses.BANK_TR_UR_TRY)
    BANK_TR_PH_TRY = Contexts.DIRECT_FISH_TRY_CONTEXT.new(person_type=PersonTypes.TRP,
                                                          paysys=Paysyses.BANK_TR_PH_TRY)
    BANK_PH_RUB_VERTICAL = Contexts.DIRECT_FISH_RUB_CONTEXT.new(
        paysys=Paysyses.BANK_PH_RUB.with_firm(Firms.VERTICAL_12),
        person_type=PersonTypes.PH)
    KZ_UR = Contexts.DIRECT_FISH_RUB_CONTEXT.new(person_type=PersonTypes.KZU, paysys=Paysyses.BANK_KZ_UR_TG)


@pytest.mark.parametrize('context, attrs, json_file', [
    # firm 1
    (PrepayContext.BANK_UR_RUB, [INV_ATTRS.INVOICE, INV_ATTRS.INVOICE_TYPE_RU_PREPAY,
                                 INV_ATTRS.BILL_TO_CONTACT_NON_PH, INV_ATTRS.SHIP_TO_CUSTOMER], 'prepayment_bank_ur_rub.json'),
    (PrepayContext.BANK_PH_RUB, [INV_ATTRS.INVOICE, INV_ATTRS.INVOICE_TYPE_RU_PREPAY,
                                 INV_ATTRS.BILL_TO_CONTACT_PH, INV_ATTRS.SHIP_TO_CUSTOMER_EMPTY], 'prepayment_bank_ph_rub.json'),
    (PrepayContext.BANK_YT_RUB, [INV_ATTRS.INVOICE, INV_ATTRS.INVOICE_TYPE_RU_PREPAY,
                                 INV_ATTRS.BILL_TO_CONTACT_NON_PH, INV_ATTRS.SHIP_TO_CUSTOMER_EMPTY], 'prepayment_bank_yt_rub.json'),
    (PrepayContext.BANK_YTUR_KZT, [INV_ATTRS.INVOICE, INV_ATTRS.INVOICE_TYPE_RU_PREPAY,
                                   INV_ATTRS.BILL_TO_CONTACT_NON_PH, INV_ATTRS.SHIP_TO_CUSTOMER_EMPTY], 'prepayment_bank_ytur_kzt.json'),
    (PrepayContext.BANK_YTPH_KZT, [INV_ATTRS.INVOICE, INV_ATTRS.INVOICE_TYPE_RU_PREPAY,
                                   INV_ATTRS.BILL_TO_CONTACT_NON_PH, INV_ATTRS.SHIP_TO_CUSTOMER_EMPTY], 'prepayment_bank_ytph_rub.json'),
    # firm 2
    # pytest.mark.skip(reason='UKRAINE WAS TURNED OFF')
    # (PrepayContext.BANK_UA_UR_UAH, [INV_ATTRS.INVOICE, INV_ATTRS.INVOICE_TYPE_UA,
    #                                 INV_ATTRS.BILL_TO_CONTACT_NON_PH, INV_ATTRS.SHIP_TO_CUSTOMER]),
    # (PrepayContext.BANK_UA_PH_UAH, [INV_ATTRS.INVOICE, INV_ATTRS.INVOICE_TYPE_UA,
    #                                 INV_ATTRS.BILL_TO_CONTACT_PH, INV_ATTRS.SHIP_TO_CUSTOMER_EMPTY]),
    # firm 4
    (PrepayContext.BANK_US_UR_USD, [INV_ATTRS.INVOICE, INV_ATTRS.INVOICE_TYPE_ACCOUNT,
                                    INV_ATTRS.BILL_TO_CONTACT_NON_PH, INV_ATTRS.SHIP_TO_CUSTOMER_EMPTY], 'prepayment_bank_us_ur_usd.json'),
    (PrepayContext.BANK_US_PH_USD, [INV_ATTRS.INVOICE, INV_ATTRS.INVOICE_TYPE_ACCOUNT,
                                    INV_ATTRS.BILL_TO_CONTACT_PH, INV_ATTRS.SHIP_TO_CUSTOMER_EMPTY], 'prepayment_bank_us_ph_usd.json'),
    # firm 7
    (PrepayContext.BANK_SW_UR_EUR, [INV_ATTRS.INVOICE, INV_ATTRS.INVOICE_TYPE_ACCOUNT_BILL,
                                    INV_ATTRS.BILL_TO_CONTACT_NON_PH, INV_ATTRS.SHIP_TO_CUSTOMER_EMPTY], 'prepayment_bank_sw_ur_eur.json'),
    (PrepayContext.BANK_SW_PH_EUR, [INV_ATTRS.INVOICE, INV_ATTRS.INVOICE_TYPE_ACCOUNT_BILL,
                                    INV_ATTRS.BILL_TO_CONTACT_PH, INV_ATTRS.SHIP_TO_CUSTOMER_EMPTY], 'prepayment_bank_sw_ph_eur.json'),
    (PrepayContext.BANK_SW_YT_EUR, [INV_ATTRS.INVOICE, INV_ATTRS.INVOICE_TYPE_ACCOUNT_BILL,
                                    INV_ATTRS.BILL_TO_CONTACT_NON_PH, INV_ATTRS.SHIP_TO_CUSTOMER_EMPTY], 'prepayment_bank_sw_yt_eur.json'),
    (PrepayContext.BANK_SW_YTPH_EUR, [INV_ATTRS.INVOICE, INV_ATTRS.INVOICE_TYPE_ACCOUNT_BILL,
                                      INV_ATTRS.BILL_TO_CONTACT_PH, INV_ATTRS.SHIP_TO_CUSTOMER_EMPTY], 'prepayment_bank_sw_ytph_eur.json'),
    (PrepayContext.CC_BY_YTPH_RUB, [INV_ATTRS.INVOICE, INV_ATTRS.INVOICE_TYPE_ACCOUNT_BILL,
                                    INV_ATTRS.BILL_TO_CONTACT_PH, INV_ATTRS.SHIP_TO_CUSTOMER_EMPTY], 'prepayment_cc_by_ytph_rub.json'),
    # firm 8 BALANCE-35586
    # (PrepayContext.BANK_TR_UR_TRY, [INV_ATTRS.INVOICE, INV_ATTRS.INVOICE_TYPE_ACCOUNT,
    #                                 INV_ATTRS.BILL_TO_CONTACT_NON_PH, INV_ATTRS.SHIP_TO_CUSTOMER_EMPTY], 'prepayment_bank_tr_ur_try.json'),
    # (PrepayContext.BANK_TR_PH_TRY, [INV_ATTRS.INVOICE, INV_ATTRS.INVOICE_TYPE_ACCOUNT,
    #                                 INV_ATTRS.BILL_TO_CONTACT_PH, INV_ATTRS.SHIP_TO_CUSTOMER_EMPTY], 'prepayment_bank_tr_ph_try.json'),
    # firm 12
    (PrepayContext.BANK_PH_RUB_VERTICAL, [INV_ATTRS.INVOICE, INV_ATTRS.INVOICE_TYPE_RU_PREPAY,
                                          INV_ATTRS.BILL_TO_CONTACT_PH, INV_ATTRS.SHIP_TO_CUSTOMER_EMPTY], 'prepayment_bank_ph_rub_vertical.json'),
    # firm 25
    (PrepayContext.KZ_UR, [INV_ATTRS.INVOICE, INV_ATTRS.INVOICE_TYPE_ACCOUNT_BILL,
                           INV_ATTRS.BILL_TO_CONTACT_NON_PH, INV_ATTRS.SHIP_TO_CUSTOMER_EMPTY], 'prepayment_bank_kz_ur_tg.json'),
], ids=lambda context, attrs, json_file: "{context_name}({paysys_id})".format(context_name=PrepayContext.name(context),
                                                                   paysys_id=context.paysys.id))
def test_export_invoice_prepay(context, attrs, json_file):

    client_id = steps.ClientSteps.create(prevent_oebs_export=True)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code, inn_type=InnType.RANDOM)
    campaigns_list = [
        {'client_id': client_id, 'service_id': context.service.id, 'product_id': context.product.id, 'qty': 100},
        {'client_id': client_id, 'service_id': context.service.id, 'product_id': context.product.id, 'qty': 12.33}
    ]
    invoice_id, _, _, _ = steps.InvoiceSteps.create_force_invoice(client_id=client_id,
                                                                  person_id=person_id,
                                                                  campaigns_list=campaigns_list,
                                                                  paysys_id=context.paysys.id,
                                                                  prevent_oebs_export=True)


    if balance_config.ENABLE_SINGLE_ACCOUNT:
        invoice_id = db.get_invoice_by_charge_note_id(invoice_id)[0]['id']

    if balance_config.JSON_CONTRACT_OEBS and json_file:
        check_json_contract(invoice_id, person_id, json_file)

    else:
        steps.ExportSteps.export_oebs(client_id=client_id,
                                      person_id=person_id,
                                      invoice_id=invoice_id)
        check_attrs(invoice_id, attrs)


@pytest.mark.parametrize('context, attrs, json_file', [
    # firm 1
    (PrepayContext.BANK_UR_RUB,
     [INV_ATTRS.INVOICE, INV_ATTRS.INVOICE_TYPE_RU_PREPAY, INV_ATTRS.BILL_TO_CONTACT_NON_PH, INV_ATTRS.SHIP_TO_CUSTOMER],
     'overdraft_bank_ur_rub.json'),
], ids=lambda context, attrs, json_file: "{context_name}({paysys_id})".format(context_name=PrepayContext.name(context),
                                                                   paysys_id=context.paysys.id))
def test_export_invoice_overdraft(context, attrs, json_file):
    QTY = 10
    with reporter.step(u'Выставляем овердрафтный счет'):
        client_id = steps.ClientSteps.create(prevent_oebs_export=True)
        steps.OverdraftSteps.set_force_overdraft(client_id, context.service.id, QTY * 1000, context.paysys.firm.id)
        person_id = steps.PersonSteps.create(client_id, context.person_type.code, inn_type=InnType.RANDOM)
        campaigns_list = [
            {'client_id': client_id, 'service_id': context.service.id, 'product_id': context.product.id, 'qty': QTY}
        ]
        invoice_id, _, _, _ = steps.InvoiceSteps.create_force_invoice(client_id=client_id,
                                                                      person_id=person_id,
                                                                      campaigns_list=campaigns_list,
                                                                      paysys_id=context.paysys.id,
                                                                      overdraft=1,
                                                                      prevent_oebs_export=True)

    if balance_config.JSON_CONTRACT_OEBS and json_file:
        check_json_contract(invoice_id, person_id, json_file)

    else:
        steps.ExportSteps.export_oebs(client_id=client_id,
                                      person_id=person_id,
                                      invoice_id=invoice_id)

        check_attrs(invoice_id, attrs)


# todo-blubimov здесь и в export_commons_acts контексты дублируются. нужно что-то с этим сделать
class PostpayContext(utils.ConstantsContainer):
    constant_type = Context

    BANK_UR_RUB = PrepayContext.BANK_UR_RUB.new(contract_type=ContractCommissionType.OPT_CLIENT)
    BANK_US_UR_USD = PrepayContext.BANK_US_UR_USD.new(contract_type=ContractCommissionType.USA_OPT_CLIENT)


@pytest.mark.parametrize('context, attrs, json_file', [
    # firm 1
    (PostpayContext.BANK_UR_RUB, [INV_ATTRS.INVOICE, INV_ATTRS.INVOICE_TYPE_RU_PREPAY,
                                  INV_ATTRS.BILL_TO_CONTACT_NON_PH, INV_ATTRS.SHIP_TO_CUSTOMER],
     'postpay_bank_ur_rub.json'),
], ids=lambda context, attrs, json_file: "{context_name}({paysys_id})".format(context_name=PrepayContext.name(context),
                                                                   paysys_id=context.paysys.id))
def test_export_invoice_postpay(context, attrs, json_file):
    with reporter.step(u'Выставляем постоплатный счет'):
        client_id = steps.ClientSteps.create(prevent_oebs_export=True)
        person_id = steps.PersonSteps.create(client_id, context.person_type.code, inn_type=InnType.RANDOM)

        contract_id, _ = create_contract_postpay(client_id, person_id, context)

        force_convert_to_fictive_credit_scheme = True
        if force_convert_to_fictive_credit_scheme:
            steps.ContractSteps.force_convert_to_fictive_credit_scheme(contract_id)

        campaigns_list = [
            {'client_id': client_id, 'service_id': context.service.id, 'product_id': context.product.id, 'qty': 100}
        ]
        fictive_invoice_id, _, _, _ = steps.InvoiceSteps.create_force_invoice(client_id=client_id,
                                                                              person_id=person_id,
                                                                              campaigns_list=campaigns_list,
                                                                              paysys_id=context.paysys.id,
                                                                              credit=1,
                                                                              contract_id=contract_id)
        repayment_invoice_id = steps.InvoiceSteps.make_repayment_invoice(fictive_invoice_id,
                                                                         prevent_oebs_export=True)[0]

    if balance_config.JSON_CONTRACT_OEBS and json_file:
        check_json_contract(repayment_invoice_id, person_id, json_file)

    else:
        steps.ExportSteps.export_oebs(client_id=client_id,
                                      person_id=person_id,
                                      contract_id=contract_id,
                                      invoice_id=repayment_invoice_id)

        check_attrs(repayment_invoice_id, attrs)


# лицевые выгружаются без заказов! заказы грузятся с актом
@pytest.mark.parametrize('context, attrs, json_file', [
    # firm 4
    (PostpayContext.BANK_US_UR_USD, [INV_ATTRS.INVOICE, INV_ATTRS.INVOICE_TYPE_ACCOUNT,
                                     INV_ATTRS.BILL_TO_CONTACT_NON_PH, INV_ATTRS.SHIP_TO_CUSTOMER_EMPTY],
     'personal_account_bank_us_usd.json'),
], ids=lambda context, attrs, json_file: "{context_name}({paysys_id})".format(context_name=PrepayContext.name(context),
                                                                   paysys_id=context.paysys.id))
def test_export_invoice_personal(context, attrs, json_file):
    with reporter.step(u'Выставляем лицевой счет'):
        client_id = steps.ClientSteps.create(prevent_oebs_export=True)
        person_id = steps.PersonSteps.create(client_id, context.person_type.code, inn_type=InnType.RANDOM)

        contract_id, _ = create_contract_postpay(client_id, person_id, context)

        campaigns_list = [
            {'client_id': client_id, 'service_id': context.service.id, 'product_id': context.product.id, 'qty': 100}
        ]
        invoice_id, _, _, _ = steps.InvoiceSteps.create_force_invoice(client_id=client_id,
                                                                      person_id=person_id,
                                                                      campaigns_list=campaigns_list,
                                                                      paysys_id=context.paysys.id,
                                                                      credit=1,
                                                                      contract_id=contract_id,
                                                                      prevent_oebs_export=True)

    if balance_config.JSON_CONTRACT_OEBS and json_file:
        check_json_contract(invoice_id, person_id, json_file)

    else:
        steps.ExportSteps.export_oebs(client_id=client_id,
                                      person_id=person_id,
                                      contract_id=contract_id,
                                      invoice_id=invoice_id)

        check_attrs(invoice_id, attrs, order_attrs_list=ORDER_ATTRS.ORDER_PERSONAL)


@pytest.mark.parametrize('context, attrs, json_file', [
    (PostpayContext.BANK_UR_RUB, [INV_ATTRS.INVOICE, INV_ATTRS.INVOICE_TYPE_RU_PERSONAL,
                                  INV_ATTRS.BILL_TO_CONTACT_NON_PH, INV_ATTRS.SHIP_TO_CUSTOMER,
                                  INV_ATTRS.ELS_NUMBER], 'els_bank_us_usd.json'),
], ids=lambda context, attrs, json_file: "{context_name}({paysys_id})".format(context_name=PrepayContext.name(context),
                                                                   paysys_id=context.paysys.id))
def test_export_invoice_els(context, attrs, json_file, get_free_user):
    user = get_free_user()
    client_id = steps.ClientSteps.create({'REGION_ID': 225}, single_account_activated=True, enable_single_account=True)
    steps.ClientSteps.link(client_id, user.login)
    person_id = steps.PersonSteps.create(client_id, 'ur')

    paysys_id = context.paysys.id
    session, token = snout_steps.CartSteps.get_session_and_token(client_id)

    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)

    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                       product_id=Products.DIRECT_FISH.id, params={'AgencyID': None})

    add_res = snout_steps.CartSteps.post_item_add(session, context.service.id, service_order_id, 1.000003, None, token)
    item_ids = [i['id'] for i in add_res.json()['data']['items']]

    snout_steps.CartSteps.post_create_request(session, _csrf=token, item_ids=item_ids)

    query = 'select id from T_REQUEST where client_id = :item'
    request_id = db.balance().execute(query, {'item': client_id})[0]['id']

    invoice_id, external_id, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=0,
                                                           contract_id=None, overdraft=0, endbuyer_id=None)
    els_number = steps.ElsSteps.get_els_number_from_client(client_id)
    invoice_id, _ = steps.ElsSteps.get_ls_by_person_id_and_els_number(person_id, els_number)

    steps.InvoiceSteps.pay(invoice_id)
    if balance_config.JSON_CONTRACT_OEBS and json_file:
        check_json_contract(invoice_id, person_id, json_file)

    else:
        steps.ExportSteps.export_oebs(client_id=client_id,
                                      person_id=person_id,
                                      invoice_id=invoice_id)
        check_attrs(invoice_id, attrs, order_attrs_list=ORDER_ATTRS.ORDER_PERSONAL)
