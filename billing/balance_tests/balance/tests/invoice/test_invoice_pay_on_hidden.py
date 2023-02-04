# coding: utf-8

import datetime
import pytest

from balance import balance_db as db
from balance import balance_steps as steps
from temp.igogor.balance_objects import Contexts
from btestlib.constants import Paysyses

DIRECT_CONTEXT = Contexts.DIRECT_FISH_RUB_CONTEXT

NOW = datetime.datetime.now()


@pytest.mark.parametrize('paysys', [Paysyses.BANK_UR_RUB, Paysyses.CC_UR_RUB])
@pytest.mark.parametrize('context', [DIRECT_CONTEXT])
def test_overpaid(context, paysys):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')
    service_order_id = steps.OrderSteps.next_id(context.service.id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                            product_id=context.product.id)

    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 100,
         'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params={'InvoiceDesireDT': NOW})

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id,
                                                 credit=0, overdraft=0, contract_id=None)

    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id,
                                      campaigns_params={'Bucks': 50}, do_stop=0,
                                      campaigns_dt=NOW)
    db.balance().execute('''update t_invoice set hidden = 2 where id =:invoice_id''', {'invoice_id': invoice_id})
    if paysys.instant:
        steps.InvoiceSteps.turn_on(invoice_id)
    else:
        steps.InvoiceSteps.pay(invoice_id)
