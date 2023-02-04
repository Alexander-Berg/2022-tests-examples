# -*- coding: utf-8 -*-
from collections import defaultdict
from decimal import Decimal
import datetime
from enum import Enum
from hamcrest import has_length

import btestlib.reporter as reporter
from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils as utils
from btestlib.constants import Paysyses, PersonTypes, Firms, ContractCommissionType, Export
from btestlib.data.defaults import Date
from btestlib.data.person_defaults import InnType
from btestlib.matchers import equal_to_casted_dict, contains_dicts_equal_to
from export_commons import Locators, get_oebs_client_party_id, get_oebs_person_cust_account_id, \
    get_oebs_person_cust_account_role_id, create_contract_postpay, get_order_eid, get_oebs_inventory_item_id, \
    read_attr_values, read_attr_values_list, attrs_list_to_dict
from temp.igogor.balance_objects import Context
from temp.igogor.balance_objects import Contexts
from test_oebs_export_invoices import get_oebs_invoice_data, get_oebs_orders_data_list
import btestlib.config as balance_config

NOW = Date.NOW()
MONTH_AGO = NOW - datetime.timedelta(days=30)

class ACT_ATTRS(object):
    class ACT(Enum):
        currency = \
            Locators(balance=lambda b: b['t_invoice.iso_currency'],
                     oebs=lambda o: o['ra_customer_trx_all.invoice_currency_code'])

        client_party_id = \
            Locators(balance=lambda b: get_oebs_client_party_id(b['t_invoice.firm_id'], b['t_act.client_id']),
                     oebs=lambda o: o['ra_customer_trx_all.attribute9'])

        bill_to_customer_id = Locators(
            balance=lambda b: get_oebs_person_cust_account_id(b['t_invoice.firm_id'], b['t_invoice.person_id']),
            oebs=lambda o: o['ra_customer_trx_all.bill_to_customer_id'])

        # счет-фактура
        factura = \
            Locators(balance=lambda b: b['t_act.factura'],
                     oebs=lambda o: o['ra_customer_trx_all.doc_sequence_value'])

        # баланс выгружает дату строкой в attribute2, в trx_date видимо кладется тоже самое только в формате даты
        dt = \
            Locators(balance=lambda b: utils.Date.nullify_time_of_date(b['t_act.dt']),
                     oebs=lambda o: o['ra_customer_trx_all.trx_date'])

        invoice = \
            Locators(balance=lambda b: b['t_invoice.external_id'],
                     oebs=lambda o: o['ra_customer_trx_all.interface_header_attribute1'])

    class ACT_PAYMENT_TERM_DT(Enum):
        # баланс выгружает в поле term_id, а в term_due_date видимо лежит уже вычисленная из него дата
        payment_term_dt = Locators(balance=lambda b: utils.Date.nullify_time_of_date(b['t_act.dt']),
                                   oebs=lambda o: o['ra_customer_trx_all.term_due_date'])

    class ACT_POSTPAY_PAYMENT_TERM_DT(Enum):
        # баланс выгружает в поле term_id, а в term_due_date видимо лежит уже вычисленная из него дата
        payment_term_dt = \
            Locators(balance=lambda b: utils.Date.nullify_time_of_date(b['t_act.payment_term_dt']),
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
        ship_to_customer = \
            Locators(balance=lambda b: '',
                     oebs=lambda o: o['ra_customer_trx_all.ship_to_customer_id'])

    # тип акта в оебс

    # резидент с НДС и нерезидент с НДС (регион=Россия)
    class ACT_TYPE_RU_WITH_NDS(Enum):
        act_type = Locators(balance=lambda b: u'Акт вып. работ',
                            oebs=lambda o: o['ra_cust_trx_types_all.name'])

    # резидент с НДС и нерезидент с НДС (регион=Россия), но S
    class ACT_TYPE_RU_WITH_NDS_S(Enum):
        act_type = Locators(balance=lambda b: u'Акт вып. работ_S',
                            oebs=lambda o: o['ra_cust_trx_types_all.name'])

    # резидент без НДС (регион=Россия)
    class ACT_TYPE_RU_WO_NDS(Enum):
        act_type = Locators(balance=lambda b: u'Акт без НДС (осв.)',
                            oebs=lambda o: o['ra_cust_trx_types_all.name'])

    # резидент без НДС (регион=Россия), но S
    class ACT_TYPE_RU_WO_NDS_S(Enum):
        act_type = Locators(balance=lambda b: u'Акт без НДС (осв.)_S',
                            oebs=lambda o: o['ra_cust_trx_types_all.name'])

    # нерезидент rub без НДС (регион=Россия)
    class ACT_TYPE_RU_YT_RUB(Enum):
        act_type = Locators(balance=lambda b: u'Акт без НДС',
                            oebs=lambda o: o['ra_cust_trx_types_all.name'])

    # нерезидент rub без НДС (регион=Россия), но S
    class ACT_TYPE_RU_YT_RUB_S(Enum):
        act_type = Locators(balance=lambda b: u'Акт без НДС_S',
                            oebs=lambda o: o['ra_cust_trx_types_all.name'])

    # нерезидент usd (регион=Россия)
    class ACT_TYPE_RU_YT_USD(Enum):
        act_type = Locators(balance=lambda b: u'Акт валютный',
                            oebs=lambda o: o['ra_cust_trx_types_all.name'])

    # нерезидент eur,byr,byn,kzt (регион=Россия)
    class ACT_TYPE_RU_YT(Enum):
        act_type = Locators(balance=lambda b: u'Акт валютный {}'.format(b['t_invoice.iso_currency']),
                            oebs=lambda o: o['ra_cust_trx_types_all.name'])

    # регион=Украина
    class ACT_TYPE_UA(Enum):
        act_type = Locators(balance=lambda b: u'Акт вып. работ Укр',
                            oebs=lambda o: o['ra_cust_trx_types_all.name'])

    # все остальные регионы
    class ACT_TYPE_INVOICE(Enum):
        act_type = Locators(balance=lambda b: u'Invoice',
                            oebs=lambda o: o['ra_cust_trx_types_all.name'])

    # для INC, AG и Turkey баланс передает Invoice, но в ОЕБС есть доп. логика по установке типа
    class ACT_TYPE_INVOICE_S(Enum):
        act_type = Locators(balance=lambda b: u'Invoice_S',
                            oebs=lambda o: o['ra_cust_trx_types_all.name'])

    class ACT_TYPE_INVOICE_EUR_S(Enum):
        act_type = Locators(balance=lambda b: u'Invoice EUR_S',
                            oebs=lambda o: o['ra_cust_trx_types_all.name'])


class ACT_ROW_ATTRS(object):
    class ACT_ROW(Enum):
        # количество товара в строке
        quantity = \
            Locators(balance=lambda b: Decimal(str(b['t_act_trans.act_qty'])) / Decimal(str(b['t_consume.type_rate'])),
                     balance_merge_function=lambda values: sum(values),
                     oebs=lambda o: Decimal(str(o['ra_customer_trx_lines_all.line.quantity_invoiced'])))

        # налог в строке (при склейке строк возвращаем ндс первой строки, т.к. во всех строках акта он одинаковый)
        tax_rate = \
            Locators(balance=lambda b: b['t_invoice.nds_pct'],
                     balance_merge_function=lambda values: values[0],
                     oebs=lambda o: o['ra_customer_trx_lines_all.tax.tax_rate'])

        # продукт в строке (согласно правилам группировки в группе всегда одинаковый продукт)
        product_inventory_item_id = \
            Locators(balance=lambda b: get_oebs_inventory_item_id(b['t_order.service_code']),
                     balance_merge_function=lambda values: values[0],
                     oebs=lambda o: o['ra_customer_trx_lines_all.line.inventory_item_id'])

        sum_wo_nds = \
            Locators(balance=lambda b: Decimal(str(b['t_act_trans.amount'])) -
                                       Decimal(str(b['t_act_trans.amount_nds'])),
                     balance_merge_function=lambda values: sum(values),
                     oebs=lambda o: Decimal(str(o['ra_customer_trx_lines_all.line.extended_amount'])))

        nds_amount = \
            Locators(balance=lambda b: Decimal(str(b['t_act_trans.amount_nds'])),
                     balance_merge_function=lambda values: sum(values),
                     oebs=lambda o: Decimal(str(o['ra_customer_trx_lines_all.tax.extended_amount'])))

    # Тип документов - Обычный
    class PRINT_DOCS_COMMON(Enum):
        # название заказа
        row_order_description = \
            Locators(balance=lambda b: '',
                     balance_merge_function=lambda values: values[0],
                     oebs=lambda o: o['ra_customer_trx_lines_all.line.description'])

        # номер заказа
        row_order_eid = \
            Locators(balance=lambda b: '{}-'.format(b['t_order.service_id']),
                     balance_merge_function=lambda values: values[0],
                     oebs=lambda o: o['ra_customer_trx_lines_all.line.attribute12'])

    # Тип документов - Подробный
    # всегда выгружаются построчно, поэтому balance_merge_function здесь не нужна
    class PRINT_DOCS_DETAILED(Enum):
        # название заказа
        row_order_description = \
            Locators(balance=lambda b: b['t_order.text'],
                     oebs=lambda o: o['ra_customer_trx_lines_all.line.description'])

        # номер заказа
        row_order_eid = \
            Locators(balance=lambda b: get_order_eid(b['t_order.service_id'], b['t_order.service_order_id']),
                     oebs=lambda o: o['ra_customer_trx_lines_all.line.attribute12'])


def get_balance_data(act_id):
    balance_act_data = {}

    # t_act
    query = "SELECT * FROM t_act WHERE id = :act_id"
    result = db.balance().execute(query, {'act_id': act_id}, single_row=True)
    balance_act_data.update(utils.add_key_prefix(result, 't_act.'))

    # t_invoice
    query = "SELECT * FROM t_invoice WHERE id = :invoice_id"
    result = db.balance().execute(query, {'invoice_id': balance_act_data['t_act.invoice_id']}, single_row=True)
    balance_act_data.update(utils.add_key_prefix(result, 't_invoice.'))

    # данные по строкам акта
    balance_act_rows_data_list = []

    # t_act_trans
    query = "SELECT * FROM t_act_trans WHERE act_id = :act_id"
    result = db.balance().execute(query, {'act_id': act_id})
    for act_tran in result:
        balance_act_rows_data_list.append(utils.add_key_prefix(act_tran, 't_act_trans.'))

    for act_rows_data in balance_act_rows_data_list:
        # t_consume
        query = "SELECT * FROM t_consume WHERE id = :consume_id"
        result = db.balance().execute(query, {'consume_id': act_rows_data['t_act_trans.consume_id']}, single_row=True)
        act_rows_data.update(utils.add_key_prefix(result, 't_consume.'))

        # t_order
        query = "SELECT * FROM t_order WHERE id = :order_id"
        result = db.balance().execute(query, {'order_id': act_rows_data['t_consume.parent_order_id']}, single_row=True)
        act_rows_data.update(utils.add_key_prefix(result, 't_order.'))
        act_rows_data.update(balance_act_data)

    return balance_act_data, balance_act_rows_data_list


def get_oebs_data(balance_act_data):
    act_eid = balance_act_data['t_act.external_id']
    firm_id = balance_act_data['t_invoice.firm_id']

    oebs_act_data = get_oebs_invoice_data(act_eid, firm_id)

    # данные по строкам акта
    customer_trx_id = oebs_act_data['ra_customer_trx_all.customer_trx_id']
    oebs_act_rows_data_list = get_oebs_orders_data_list(customer_trx_id, firm_id)

    return oebs_act_data, oebs_act_rows_data_list


def check_attrs(act_id, act_attrs_list, act_rows_attrs_list, merge_balance_act_rows=False):
    with reporter.step(u'Считываем данные из баланса'):
        balance_act_data, balance_act_rows_data_list = get_balance_data(act_id)
    with reporter.step(u'Считываем данные из ОЕБС'):
        oebs_act_data, oebs_act_rows_data_list = get_oebs_data(balance_act_data)

    balance_act_values, oebs_act_values = \
        read_attr_values(act_attrs_list, balance_act_data, oebs_act_data)

    utils.check_that(oebs_act_values, equal_to_casted_dict(balance_act_values),
                     step=u'Проверяем корректность общих данных акта в ОЕБС')

    balance_act_rows_values_list, oebs_act_rows_values_list = \
        read_attr_values_list(act_rows_attrs_list, balance_act_rows_data_list, oebs_act_rows_data_list)

    if merge_balance_act_rows:
        balance_act_rows_values_list = [merge_balance_rows(act_rows_attrs_list, balance_act_rows_values_list)]

    utils.check_that(oebs_act_rows_values_list, contains_dicts_equal_to(balance_act_rows_values_list),
                     step=u'Проверяем корректность данных строчек акта в ОЕБС')
    utils.check_that(oebs_act_rows_values_list, has_length(len(balance_act_rows_values_list)),
                     step=u'Проверяем, что количество строк акта одинаково')


def merge_balance_rows(attrs_list, balance_act_rows_values_list):
    # модифицируем список словарей в словарь со списком значений по каждому атрибуту
    attr_to_values = defaultdict(list)
    for values_dict in balance_act_rows_values_list:
        for attr, value in values_dict.items():
            attr_to_values[attr].append(value)
    # объединяем список значений атрибута в одно значение
    locators_dict = attrs_list_to_dict(attrs_list)
    return {attr: locators_dict[attr].balance_merge_function(values) for attr, values in attr_to_values.items()}


def generate_act(context, client_id, orders_list, campaigns_dt=None):
    for order in orders_list:
        steps.CampaignsSteps.do_campaigns(context.service.id, order['ServiceOrderID'],
                                          {context.product.type.code: order['Qty']},
                                          campaigns_dt=campaigns_dt or NOW)
    # todo-blubimov здесь имеет значение force 1 или 0 ?
    act_id = steps.ActsSteps.generate(client_id, force=1,
                                      date=campaigns_dt or NOW,
                                      prevent_oebs_export=True)[0]
    return act_id


def create_act_on_prepay_invoice(context, client_id=None, qty_list=None):
    with reporter.step(u'Выставляем акт по предоплатному счету'):
        client_id = client_id or steps.ClientSteps.create(prevent_oebs_export=True)
        person_id = steps.PersonSteps.create(client_id, context.person_type.code, inn_type=InnType.RANDOM)

        qty_list_default = [30, 15.78]
        qty_list = qty_list or qty_list_default

        campaigns_list = [{'client_id': client_id,
                           'service_id': context.service.id,
                           'product_id': context.product.id,
                           'qty': qty}
                          for qty in qty_list]

        invoice_id, _, _, orders_list = steps.InvoiceSteps.create_force_invoice(client_id=client_id,
                                                                                person_id=person_id,
                                                                                campaigns_list=campaigns_list,
                                                                                paysys_id=context.paysys.id,
                                                                                invoice_dt=MONTH_AGO,
                                                                                prevent_oebs_export=True)

        steps.InvoiceSteps.turn_on(invoice_id)
        act_id = generate_act(context, client_id, orders_list, campaigns_dt=MONTH_AGO)

    if balance_config.ENABLE_SINGLE_ACCOUNT:
        single_account = db.get_invoice_by_charge_note_id(invoice_id)
        if single_account:
            invoice_id = single_account[0]['id']

    return client_id, person_id, invoice_id, act_id


def create_act_on_prepay_speechkit_invoice(context):
    with reporter.step(u'Выставляем акт по предоплатному счету'):
        client_id = steps.ClientSteps.create(prevent_oebs_export=True)
        person_id = steps.PersonSteps.create(client_id, context.person_type.code, inn_type=InnType.RANDOM)

        orders_list = [
            {'product_id': context.product.id, 'qty': 7000000},
            {'product_id': context.product.id, 'qty': 1578342},
        ]
        request_id = steps.RequestSteps.create_from_shop(client_id=client_id, firm_id=context.paysys.firm.id,
                                                         orders_list=orders_list)
        invoice_id = steps.InvoiceSteps.create_prepay_http(request_id=request_id, person_id=person_id,
                                                           paysys_id=context.paysys.id, prevent_oebs_export=True)

        # todo-blubimov нужно узнать почему само не откручивается, по идее т.к. это магазин - должно
        orders_data = db.balance().execute('SELECT o.service_id, o.service_order_id, io.quantity '
                                           'FROM t_invoice_order io, t_order o '
                                           'WHERE io.invoice_id = :invoice_id AND o.id = io.order_id',
                                           {'invoice_id': invoice_id}, fail_empty=True)
        for order in orders_data:
            steps.CampaignsSteps.do_campaigns(order['service_id'], order['service_order_id'],
                                              {context.product.type.code: order['quantity']})

        act_id = steps.ActsSteps.generate(client_id, force=1, prevent_oebs_export=True)[0]

        return client_id, person_id, invoice_id, act_id


def create_act_on_overdraft_invoice(context):
    with reporter.step(u'Выставляем акт по овердрафтному счету'):
        client_id = steps.ClientSteps.create(prevent_oebs_export=True)
        steps.OverdraftSteps.set_force_overdraft(client_id, context.service.id, 1000, context.paysys.firm.id)
        person_id = steps.PersonSteps.create(client_id, context.person_type.code, inn_type=InnType.RANDOM)
        campaigns_list = [
            {'client_id': client_id, 'service_id': context.service.id, 'product_id': context.product.id, 'qty': 17},
            {'client_id': client_id, 'service_id': context.service.id, 'product_id': context.product.id, 'qty': 11.55},
        ]
        invoice_id, _, _, orders_list = steps.InvoiceSteps.create_force_invoice(client_id=client_id,
                                                                                person_id=person_id,
                                                                                campaigns_list=campaigns_list,
                                                                                paysys_id=context.paysys.id,
                                                                                overdraft=1,
                                                                                prevent_oebs_export=True)

        act_id = generate_act(context, client_id, orders_list)

    return client_id, person_id, invoice_id, act_id


def create_act_on_postpay_invoice(context):
    with reporter.step(u'Выставляем акт по постоплатному счету'):
        client_id = steps.ClientSteps.create(prevent_oebs_export=True)
        person_id = steps.PersonSteps.create(client_id, context.person_type.code, inn_type=InnType.RANDOM)

        contract_id, _ = create_contract_postpay(client_id, person_id, context)

        campaigns_list = [
            {'client_id': client_id, 'service_id': context.service.id, 'product_id': context.product.id, 'qty': 100},
            {'client_id': client_id, 'service_id': context.service.id, 'product_id': context.product.id, 'qty': 19.99},
        ]
        fictive_invoice_id, _, _, orders_list = steps.InvoiceSteps.create_force_invoice(client_id=client_id,
                                                                                        person_id=person_id,
                                                                                        campaigns_list=campaigns_list,
                                                                                        paysys_id=context.paysys.id,
                                                                                        credit=1,
                                                                                        contract_id=contract_id)

        act_id = generate_act(context, client_id, orders_list)
        repayment_invoice_id = db.balance().execute('SELECT invoice_id FROM t_act WHERE id = :id', {'id': act_id},
                                                    single_row=True)['invoice_id']
        steps.ExportSteps.prevent_auto_export(repayment_invoice_id, Export.Classname.INVOICE)

    return client_id, person_id, contract_id, repayment_invoice_id, act_id


def create_act_on_personal_invoice(context):
    with reporter.step(u'Выставляем акт по лицевому счету'):
        client_id = steps.ClientSteps.create(prevent_oebs_export=True)
        person_id = steps.PersonSteps.create(client_id, context.person_type.code, inn_type=InnType.RANDOM)

        contract_id, _ = create_contract_postpay(client_id, person_id, context)

        campaigns_list = [
            {'client_id': client_id, 'service_id': context.service.id, 'product_id': context.product.id, 'qty': 101},
            {'client_id': client_id, 'service_id': context.service.id, 'product_id': context.product.id, 'qty': 19.99},
        ]
        invoice_id, _, _, orders_list = steps.InvoiceSteps.create_force_invoice(client_id=client_id,
                                                                                person_id=person_id,
                                                                                campaigns_list=campaigns_list,
                                                                                paysys_id=context.paysys.id,
                                                                                credit=1,
                                                                                contract_id=contract_id,
                                                                                prevent_oebs_export=True)

        act_id = generate_act(context, client_id, orders_list)

    return client_id, person_id, contract_id, invoice_id, act_id


# todo-blubimov какие контексты нас интересуют?
# todo-blubimov здесь и в выгрузке счетов контексты дублируются. нужнжо что-то с этим сделать
class PrepayContext(utils.ConstantsContainer):
    constant_type = Context

    BANK_UR_RUB = Contexts.DIRECT_FISH_RUB_CONTEXT.new(paysys=Paysyses.BANK_UR_RUB)
    BANK_UR_RUB_WO_NDS = Contexts.SPEECHKIT_CONTEXT.new(paysys=Paysyses.BANK_UR_RUB)
    BANK_PH_RUB = Contexts.DIRECT_FISH_RUB_CONTEXT.new(paysys=Paysyses.BANK_PH_RUB, person_type=PersonTypes.PH)
    BANK_YT_RUB = Contexts.DIRECT_FISH_YT_RUB_CONTEXT.new(paysys=Paysyses.BANK_YT_RUB)
    BANK_YT_RUB_WITH_NDS = Contexts.ADFOX_CONTEXT.new(paysys=Paysyses.BANK_YT_RUB_WITH_NDS, person_type=PersonTypes.YT)
    BANK_YT_USD = Contexts.DIRECT_FISH_YT_RUB_CONTEXT.new(paysys=Paysyses.BANK_YT_USD)
    BANK_YT_EUR = Contexts.DIRECT_FISH_YT_RUB_CONTEXT.new(paysys=Paysyses.BANK_YT_EUR)
    BANK_YT_BYN = Contexts.DIRECT_FISH_YT_RUB_CONTEXT.new(paysys=Paysyses.BANK_YT_BYN)
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

    BANK_SW_UR_EUR_SAG = Contexts.DIRECT_FISH_SW_EUR_CONTEXT.new(paysys=Paysyses.BANK_SW_UR_EUR_SAG)
    BANK_SW_YT_EUR_SAG = Contexts.DIRECT_FISH_SW_EUR_CONTEXT.new(person_type=PersonTypes.SW_YT,
                                                                 paysys=Paysyses.BANK_SW_YT_EUR_SAG)


# todo-blubimov здесь и в выгрузке счетов контексты дублируются. нужнжо что-то с этим сделать
class PostpayContext(utils.ConstantsContainer):
    constant_type = Context

    BANK_UR_RUB = PrepayContext.BANK_UR_RUB.new(contract_type=ContractCommissionType.OPT_CLIENT)
    BANK_US_UR_USD = PrepayContext.BANK_US_UR_USD.new(contract_type=ContractCommissionType.USA_OPT_CLIENT)
    BANK_YT_RUB = PrepayContext.BANK_YT_RUB.new(contract_type=ContractCommissionType.OPT_CLIENT)
    BANK_YT_RUB_WITH_NDS = PrepayContext.BANK_YT_RUB_WITH_NDS.new(contract_type=ContractCommissionType.OPT_CLIENT)
    BANK_YT_USD = PrepayContext.BANK_YT_USD.new(contract_type=ContractCommissionType.OPT_CLIENT)
    BANK_YT_EUR = PrepayContext.BANK_YT_EUR.new(contract_type=ContractCommissionType.OPT_CLIENT)
