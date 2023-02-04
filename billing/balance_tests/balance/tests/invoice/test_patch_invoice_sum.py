# -*- coding: utf-8 -*-
import datetime

import hamcrest
import pytest
from decimal import Decimal
import btestlib.utils as utils
from balance import balance_steps as steps
from balance import balance_db as db
from btestlib.matchers import contains_dicts_with_entries
from temp.igogor.balance_objects import Contexts

DIRECT_FISH_RUB_CONTEXT = Contexts.DIRECT_FISH_RUB_CONTEXT.new()
NOW = datetime.datetime.now()
QTY = '766.6666666666666666666666667'
INVOICE_SUM = Decimal('10000')
ORDER_PRECISION = 6


@pytest.mark.parametrize('context', [
    DIRECT_FISH_RUB_CONTEXT])
def test_patch_invoice_sum(context):
    agency_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(agency_id, context.person_type.code)
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(agency_id, service_order_id, service_id=context.service.id,
                            product_id=context.product.id)
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY,
         'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(agency_id, orders_list,
                                           additional_params={'InvoiceDesireDT': NOW})
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                 paysys_id=context.paysys.id,
                                                 credit=0, contract_id=None, overdraft=0)
    invoice_rows = db.get_invoice_orders_by_invoice_id(invoice_id)
    utils.check_that(invoice_rows,
                     contains_dicts_with_entries([{'quantity': utils.dround(QTY, decimal_places=ORDER_PRECISION),
                                                   'initial_quantity': QTY}]))
    steps.InvoiceSteps.patch_sum_ai(invoice_id, invoice_sum=INVOICE_SUM)
    invoice_rows = db.get_invoice_orders_by_invoice_id(invoice_id)
    changed_rounded_qty = utils.dround(Decimal(INVOICE_SUM) / Decimal('30'), decimal_places=ORDER_PRECISION)
    utils.check_that(invoice_rows, contains_dicts_with_entries([{'quantity': changed_rounded_qty,
                                                                 'initial_quantity': changed_rounded_qty}]))
