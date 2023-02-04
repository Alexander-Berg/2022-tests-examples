# -*- coding: utf-8 -*-

import datetime
from decimal import Decimal as D

from balance import balance_steps as steps
from btestlib import utils
from temp.igogor.balance_objects import Contexts
from btestlib.constants import Firms, ContractCommissionType, TransactionType
from .. import common_defaults

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
ORDER_DT = NOW
INVOICE_DT = NOW
COMPLETIONS_DT = NOW
ACT_DT = NOW

_, _, contract_start_dt, act_dt_1, _, act_dt_2 = utils.Date.previous_three_months_start_end_dates()

CONTEXT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                               contract_type=ContractCommissionType.NO_AGENCY,
                                               person_params=common_defaults.FIXED_UR_PARAMS)
QTY = D('100')
COMPLETIONS = D('50')


def create_base_request(qty=QTY, orders_amount=1, contract_params=None, contract_type=None, fictive_scheme=False,
                        context=CONTEXT, client_id=None, person_id=None, contract_id=None, invoice_desired_dt=COMPLETIONS_DT):
    # Создаём клиента
    client_id = client_id or steps.ClientSteps.create(params={'NAME': common_defaults.CLIENT_NAME})
    agency_id = None

    # Далее в скрипте будут фигурировать "владелец счёта": агентство или клиент и "владелей заказа": всегда клиент:
    order_owner = client_id
    invoice_owner = agency_id or client_id

    # Создаём плательщика
    person_params = context.person_params
    person_id = person_id or steps.PersonSteps.create(invoice_owner, context.person_type.code, person_params)

    # Создаём договор:
    contract_id = contract_id
    if contract_params:
        contract_params.update({'CLIENT_ID': client_id,
                                'PERSON_ID': person_id})
        contract_id, contract_external_id = steps.ContractSteps.create_contract_new(contract_type, contract_params)
    if fictive_scheme:
        steps.ContractSteps.force_convert_to_fictive_credit_scheme(contract_id)

    # Создаём список заказов:
    service_order_id_list = []
    orders_list = []

    for _ in xrange(orders_amount):
        service_order_id = steps.OrderSteps.next_id(context.service.id)
        steps.OrderSteps.create(order_owner, service_order_id, service_id=context.service.id, product_id=context.product.id,
                                params={'AgencyID': agency_id, 'ManagerUID': context.manager.uid})
        orders_list.append(
            {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': invoice_desired_dt})
        service_order_id_list.append(service_order_id)

    # Создаём риквест
    request_id = steps.RequestSteps.create(invoice_owner, orders_list, additional_params=dict(InvoiceDesireDT=invoice_desired_dt))

    return client_id, person_id, orders_list, service_order_id_list, contract_id, request_id


def create_base_invoice(qty=QTY, overdraft=0, orders_amount=1, credit=0, contract_params=None, contract_type=None,
                        fictive_scheme=False, paid=True, context=CONTEXT,
                        client_id=None, person_id=None, contract_id=None, turn_on=False, invoice_desired_dt=COMPLETIONS_DT):
    client_id, person_id, orders_list, service_order_id_list, contract_id, request_id = \
        create_base_request(qty, orders_amount=orders_amount, contract_params=contract_params,
                            contract_type=contract_type, fictive_scheme=fictive_scheme, context=context,
                            client_id=client_id, person_id=person_id, contract_id=contract_id, invoice_desired_dt=invoice_desired_dt)

    if overdraft:
        steps.ClientSteps.set_force_overdraft(client_id, CONTEXT.service.id, 100000000)

    # Выставляем счёт
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=credit,
                                                 contract_id=contract_id, overdraft=overdraft, endbuyer_id=None)
    if paid:
        steps.InvoiceSteps.pay(invoice_id)
    if turn_on:
        steps.InvoiceSteps.turn_on(invoice_id)

    return client_id, person_id, orders_list, service_order_id_list, contract_id, request_id, invoice_id


def create_base_act(act_dt=ACT_DT, completions_dt=COMPLETIONS_DT,
                    completions=COMPLETIONS, context=CONTEXT,
                    client_id=None, person_id=None, contract_id=None,
                    paid_invoice=True, turn_on=False, act_num=0, contract_params=None, contract_type=None, qty=QTY,
                    overdraft=0, orders_amount=1, credit=0, invoice_desired_dt=COMPLETIONS_DT):
    client_id, person_id, orders_list, service_order_id_list, contract_id, request_id, invoice_id = \
        create_base_invoice(client_id=client_id, person_id=person_id, contract_id=contract_id, context=context,
                            paid=paid_invoice, turn_on=turn_on, contract_params=contract_params,
                            contract_type=contract_type, qty=qty, overdraft=overdraft, orders_amount=orders_amount,
                            credit=credit, invoice_desired_dt=invoice_desired_dt)
    # Отправляем НЕчестные открутки:
    steps.CampaignsSteps.do_campaigns(context.product.service.id, orders_list[0]['ServiceOrderID'],
                                      {context.product.type.code: completions}, 0, completions_dt)
    # Выставляем акт
    steps.ActsSteps.generate(client_id, force=1, date=act_dt)
    act_id = steps.ActsSteps.get_all_act_data(client_id)[act_num]['id']
    external_act_id = steps.ActsSteps.get_act_external_id(act_id)
    external_invoice_id = steps.InvoiceSteps.get_all_invoice_data_by_id(invoice_id)['external_id']

    return client_id, person_id, invoice_id, external_invoice_id, act_id, external_act_id, service_order_id_list


def create_partner_act(context, corp_client_id, corp_contract_id, dt, coef=D('1'), client_id=111, person_id=111,
                       contract_id=111):
    # добавляем открутки
    steps.SimpleApi.create_fake_tpt_data(context, client_id, person_id,
                                         contract_id, dt,
                                         [{'client_amount': D('111.32') * coef,
                                           'yandex_reward': D('111.32') * coef,
                                           'client_id': corp_client_id,
                                           'transaction_type': TransactionType.PAYMENT},
                                          {'client_amount': D('43.8') * coef,
                                           'yandex_reward': D('43.8') * coef,
                                           'client_id': corp_client_id,
                                           'transaction_type': TransactionType.REFUND}
                                          ])
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(corp_client_id, corp_contract_id, dt)
    act_id = steps.ActsSteps.get_all_act_data(corp_client_id, dt=dt)[0]['id']
    external_id = steps.ActsSteps.get_act_external_id(act_id)
    return act_id, external_id