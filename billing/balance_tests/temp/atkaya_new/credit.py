# -*- coding: utf-8 -*-

from datetime import datetime
from decimal import Decimal

from balance import balance_steps as steps
from btestlib import utils
from btestlib.constants import Firms, ContractCommissionType
from temp.igogor.balance_objects import Contexts

NOW = utils.Date.nullify_time_of_date(datetime.now())
DIRECT_YANDEX = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                                     contract_type=ContractCommissionType.NO_AGENCY)


def test_credit():
    context = DIRECT_YANDEX
    params = {'CREDIT_LIMIT_SINGLE': Decimal(123456)}
    qty = 100
    dt = datetime(2021, 1, 1)
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_general_contract_by_context(context,
                                                                                                  postpay=True,
                                                                                                  start_dt=dt,
                                                                                                  additional_params=params)
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id=client_id, product_id=context.product.id, service_id=context.service.id,
                            service_order_id=service_order_id)
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params={'InvoiceDesireDT': dt})
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                 paysys_id=context.paysys.id,
                                                 credit=1, contract_id=contract_id)
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id, {context.product.type.code: qty}, 0)
    act_id = steps.ActsSteps.generate(client_id, force=1)[0]

    payment_term_dt = datetime(2021, 4, 21)
    steps.ActsSteps.set_payment_term_dt(act_id, payment_term_dt)

    steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
