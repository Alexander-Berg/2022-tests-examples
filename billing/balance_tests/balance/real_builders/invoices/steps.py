# -*- coding: utf-8 -*-

import datetime
from decimal import Decimal as D

from balance import balance_steps as steps
from btestlib import utils
from temp.igogor.balance_objects import Contexts
from btestlib.constants import Firms, ContractCommissionType, PersonTypes
from .. import common_defaults

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
ORDER_DT = NOW
INVOICE_DT = NOW
COMPLETIONS_DT = NOW
ACT_DT = NOW

CONTEXT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                               contract_type=ContractCommissionType.NO_AGENCY,
                                               person_params=common_defaults.FIXED_UR_PARAMS)
QTY = D('250')
COMPLETIONS = D('99.99')


def create_base_request(qty=QTY, orders_amount=1, contract_params=None, contract_type=None, fictive_scheme=False,
                        context=CONTEXT, client_id=None, agency_id=None, person_id=None, with_endbuyer=None,
                        client_name=None, need_agency=False, contract_id=None):

    # Создаём клиента
    client_params = {'NAME': client_name} if client_name else {'NAME': common_defaults.CLIENT_NAME}
    client_id = client_id or steps.ClientSteps.create(params=client_params)
    agency_id = agency_id if not need_agency else steps.ClientSteps.create(params={'NAME': common_defaults.AGENCY_NAME,
                                                                                   'IS_AGENCY': 1})

    # Далее в скрипте будут фигурировать "владелец счёта": агентство или клиент и "владелей заказа": всегда клиент:
    order_owner = client_id
    invoice_owner = agency_id or client_id

    # Создаём плательщика
    person_params = context.person_params
    person_id = person_id or steps.PersonSteps.create(invoice_owner, context.person_type.code, person_params)

    # Создаём договор:
    contract_id = contract_id
    if contract_params and not contract_id:
        contract_params.update({'CLIENT_ID': agency_id if with_endbuyer else client_id,
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
            {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': ORDER_DT})
        service_order_id_list.append(service_order_id)

    # Создаём риквест
    request_id = steps.RequestSteps.create(invoice_owner, orders_list, additional_params=dict(InvoiceDesireDT=INVOICE_DT))

    endbuyer_id = steps.PersonSteps.create(invoice_owner, PersonTypes.ENDBUYER_UR.code,
                                           common_defaults.FIXED_ENDBUYER_UR_PARAMS) if with_endbuyer else None
    return client_id, person_id, orders_list, service_order_id_list, contract_id, request_id, endbuyer_id


def create_base_invoice(qty=QTY, overdraft=0, orders_amount=1, credit=0, contract_params=None, contract_type=None,
                        fictive_scheme=False, context=CONTEXT, client_id=None, agency_id=None, person_id=None,
                        with_endbuyer=None, client_name=None, need_agency=False, contract_id=None):
    client_id, person_id, orders_list, service_order_id_list, contract_id, request_id, endbuyer_id = \
        create_base_request(qty, orders_amount=orders_amount, contract_params=contract_params,
                            contract_type=contract_type, fictive_scheme=fictive_scheme, context=context,
                            client_id=client_id, person_id=person_id, with_endbuyer=with_endbuyer,
                            client_name=client_name, need_agency=need_agency, contract_id=contract_id)

    if overdraft:
        steps.ClientSteps.set_force_overdraft(client_id, context.service.id, 100000000, firm_id=context.firm.id)

    # Выставляем счёт
    invoice_id, external_id, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=credit,
                                                 contract_id=contract_id, overdraft=overdraft, endbuyer_id=endbuyer_id)

    return client_id, person_id, orders_list, service_order_id_list, contract_id, request_id, invoice_id, external_id