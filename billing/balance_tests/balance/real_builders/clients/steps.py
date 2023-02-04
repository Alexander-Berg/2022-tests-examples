# -*- coding: utf-8 -*-

import datetime
from decimal import Decimal as D


from balance import balance_steps as steps
from btestlib import utils
from temp.igogor.balance_objects import Contexts
from btestlib.constants import Firms, ContractCommissionType, PersonTypes, Currencies, NdsNew, PaymentMethods, Regions
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
                                               person_params=common_defaults.FIXED_UR_PARAMS,
                                               payment_method=PaymentMethods.BANK.cc,
                                               currency=Currencies.RUB,
                                               nds=NdsNew.DEFAULT.pct_on_dt(NOW),
                                               region=Regions.RU)
QTY = D('250')
COMPLETIONS = D('99.99')


def create_base_request(client_id=None, agency_id=None, person_id=None, contract_id=None, need_agency=0, qty=QTY,
                        contract_params=None, contract_type=None, fictive_scheme=False, context=CONTEXT,
                        client_name=None):
    service_id = context.service.id
    # Создаём клиента
    client_params = {'NAME': client_name} if client_name else {'NAME': common_defaults.CLIENT_NAME}
    client_id = client_id or steps.ClientSteps.create(params=client_params)
    agency_id = agency_id if not need_agency else steps.ClientSteps.create(params={'IS_AGENCY': 1})

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

    service_order_id = steps.OrderSteps.next_id(context.service.id)
    steps.OrderSteps.create(order_owner, service_order_id, service_id=context.service.id, product_id=context.product.id,
                            params={'AgencyID': agency_id, 'ManagerUID': context.manager.uid})
    order_id = {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': ORDER_DT}
    request_id = steps.RequestSteps.create(invoice_owner, [order_id],
                                           additional_params=dict(InvoiceDesireDT=INVOICE_DT))

    return client_id, agency_id, person_id, contract_id, service_id, service_order_id, order_id, request_id


def create_client_and_person(is_agency=False, client_name=common_defaults.CLIENT_NAME, context=CONTEXT):
    client_id = steps.ClientSteps.create(params={'IS_AGENCY': is_agency, 'NAME': client_name})
    person_id = steps.PersonSteps.create(client_id, context.person_type.code,
                                         params=context.person_params)
    return client_id, person_id


def set_autooverdraft(client_id, person_id, overdraft_limit=1000, autooverdraft_limit=100, context=CONTEXT):
    steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='COPY',
                                          dt=NOW - utils.relativedelta(days=5),
                                          currency=context.currency.iso_code, region_id=context.region.id)

    steps.OverdraftSteps.set_force_overdraft(client_id, context.service.id, overdraft_limit, context.firm.id,
                                             currency=context.currency.iso_code,
                                             limit_wo_tax=int(D(overdraft_limit) / (1 + D(context.nds) / 100)))

    steps.OverdraftSteps.set_overdraft_params(person_id=person_id, client_limit=autooverdraft_limit,
                                              iso_currency=context.currency.iso_code,
                                              payment_method=context.payment_method)

