# -*- coding: utf-8 -*-

import datetime
from decimal import Decimal as D

from balance import balance_steps as steps
from btestlib import utils
from balance import balance_db as db
from temp.igogor.balance_objects import Contexts
from btestlib.constants import Firms, ContractCommissionType
from .. import common_defaults

NOW = datetime.datetime.now()
ORDER_DT = NOW
INVOICE_DT = NOW
COMPLETIONS_DT = NOW

CONTEXT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                               contract_type=ContractCommissionType.NO_AGENCY,
                                               person_params=common_defaults.FIXED_UR_PARAMS)
QTY = D('250')
COMPLETIONS = D('99.99')


def create_base_request(client_id=None, agency_id=None, person_id=None, contract_id=None, need_agency=0, qty=QTY,
                        contract_params=None, fictive_scheme=False, context=CONTEXT, client_name=None,
                        no_request=False, no_manager=False, order_params=None, with_person=True, order_amount=1):
    service_id = context.service.id
    # Создаём клиента
    client_params = {'NAME': client_name} if client_name else {'NAME': common_defaults.CLIENT_NAME}
    client_id = client_id or steps.ClientSteps.create(params=client_params)
    agency_id = agency_id if not need_agency else steps.ClientSteps.create(params={'IS_AGENCY': 1,
                                                                                   'NAME': common_defaults.AGENCY_NAME})

    # Далее в скрипте будут фигурировать "владелец счёта": агентство или клиент и "владелей заказа": всегда клиент:
    order_owner = client_id
    invoice_owner = agency_id or client_id

    # Создаём плательщика
    person_params = context.person_params
    if with_person:
        person_id = person_id or steps.PersonSteps.create(invoice_owner, context.person_type.code, person_params)

    # Создаём договор:
    contract_id = contract_id
    if contract_params:
        contract_params.update({'CLIENT_ID': client_id,
                                'PERSON_ID': person_id})
        contract_id, contract_external_id = steps.ContractSteps.create_contract_new(context.contract_type, contract_params)
    if fictive_scheme:
        steps.ContractSteps.force_convert_to_fictive_credit_scheme(contract_id)

    orders_list = []
    for i in range(order_amount):
        service_order_id = steps.OrderSteps.next_id(context.service.id)
        manager = context.manager.uid if not no_manager else None
        if order_params:
            order_params.update({'AgencyID': agency_id, 'ManagerUID': manager})
        else:
            order_params = {'AgencyID': agency_id, 'ManagerUID': manager}
        order_id = int(steps.OrderSteps.create(order_owner, service_order_id, service_id=context.service.id,
                                           product_id=context.product.id,
                                           params=order_params))
        order_info = {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': ORDER_DT}
        orders_list.append(order_info)
    request_id = steps.RequestSteps.create(invoice_owner, orders_list,
                                           additional_params=dict(InvoiceDesireDT=INVOICE_DT)) \
        if not no_request else None

    return client_id, agency_id, person_id, contract_id, service_id, service_order_id, order_id, order_info, request_id


def create_base_invoice(request_id, person_id, contract_id=None, overdraft=0, credit=0, context=CONTEXT, turn_on=False,
                        need_campaigns=False, service_order_id=None):
    # Выставляем счёт
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=credit,
                                                 contract_id=contract_id, overdraft=overdraft, endbuyer_id=None)
    if turn_on:
        steps.InvoiceSteps.turn_on(invoice_id)
    if need_campaigns:
        steps.CampaignsSteps.do_campaigns(context.product.service.id, service_order_id,
                                          {'Bucks': COMPLETIONS, 'Money': 0}, 0, COMPLETIONS_DT)
    return invoice_id

def create_base_contract(contract_params, is_agency=0, context=CONTEXT):
    client_id = steps.ClientSteps.create({'NAME': common_defaults.CLIENT_NAME, 'IS_AGENCY': is_agency})
    person_id = steps.PersonSteps.create(client_id, context.person_type.code, params=context.person_params)
    contract_params.update({'CLIENT_ID': client_id, 'PERSON_ID': person_id})
    contract_id, contract_eid = steps.ContractSteps.create_contract_new(context.contract_type, contract_params)
    return client_id, person_id, contract_id, contract_eid
