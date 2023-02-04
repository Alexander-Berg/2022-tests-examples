import datetime

import hamcrest
import pytest

import balance.balance_db as db
import btestlib.utils as utils
from balance import balance_steps as steps
from btestlib.constants import Products, Paysyses, PersonTypes
from temp.igogor.balance_objects import Contexts

DT = datetime.datetime.now()
HALF_YEAR_AFTER_NOW_ISO = utils.Date.date_to_iso_format(DT + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = utils.Date.date_to_iso_format(DT - datetime.timedelta(days=180))
YEAR_BEFORE_NOW_ISO = utils.Date.date_to_iso_format(DT - datetime.timedelta(days=365))

dt = datetime.datetime.now()

DIRECT_TUNING_FISH_RUB = Contexts.DIRECT_TUNING_FISH_RUB_CONTEXT.new()
DIRECT_TUNING_FISH_RUB_PRODUCT_2 = Contexts.DIRECT_TUNING_FISH_RUB_CONTEXT.new(product=Products.DIRECT_TUNING_2)
DIRECT_TUNING_FISH_RUB_PRODUCT_3 = Contexts.DIRECT_TUNING_FISH_RUB_CONTEXT.new(product=Products.DIRECT_TUNING_3)
DIRECT_TUNING_FISH_RUB_CARD = Contexts.DIRECT_TUNING_FISH_RUB_CONTEXT.new(paysys=Paysyses.CC_UR_RUB)
DIRECT_TUNING_FISH_RUB_PH = Contexts.DIRECT_TUNING_FISH_RUB_CONTEXT.new(person_type=PersonTypes.PH,
                                                                        paysys=Paysyses.BANK_PH_RUB)
DIRECT_TUNING_FISH_RUB_PH_CARD = Contexts.DIRECT_TUNING_FISH_RUB_CONTEXT.new(person_type=PersonTypes.PH,
                                                                             paysys=Paysyses.CC_PH_RUB)
DIRECT_TUNING_FISH_RUB_PH_YD = Contexts.DIRECT_TUNING_FISH_RUB_CONTEXT.new(person_type=PersonTypes.PH,
                                                                           paysys=Paysyses.YM_PH_RUB)


@pytest.mark.parametrize('context, params', [(DIRECT_TUNING_FISH_RUB, {'expected': {'effective_sum': 1500}}),
                                             (DIRECT_TUNING_FISH_RUB,
                                              {'expected': {'effective_sum': 1500}, 'client_params': {'IS_AGENCY': 1}}),
                                             (DIRECT_TUNING_FISH_RUB_PRODUCT_2, {'expected': {'effective_sum': 2500}}),
                                             (DIRECT_TUNING_FISH_RUB_PRODUCT_3, {'expected': {'effective_sum': 5000}}),
                                             (DIRECT_TUNING_FISH_RUB_CARD, {'expected': {'effective_sum': 1500}}),
                                             (DIRECT_TUNING_FISH_RUB_PH, {'expected': {'effective_sum': 1500}}),
                                             (DIRECT_TUNING_FISH_RUB_PH_CARD, {'expected': {'effective_sum': 1500}}),
                                             (DIRECT_TUNING_FISH_RUB_PH_YD, {'expected': {'effective_sum': 1500}})])
def test_direct_tuning_direct_client(context, params):
    client_id = steps.ClientSteps.create(params.get('client_params', {}))
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    order_id = steps.OrderSteps.create(client_id=client_id, product_id=context.product.id,
                                       service_id=context.service.id,
                                       service_order_id=service_order_id, params={'AgencyID': None})
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 0.5, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)

    contract_id = None

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                 paysys_id=context.paysys.id,
                                                 credit=0, contract_id=contract_id, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.turn_on(invoice_id)

    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id, {context.product.type.code: 1}, 0, dt)

    act_id = steps.ActsSteps.generate(client_id, force=1, date=dt)[0]

    steps.ExportSteps.export_oebs(client_id=client_id, person_id=person_id, act_id=act_id, invoice_id=invoice_id)

    invoice = db.get_invoice_by_id(invoice_id)[0]
    utils.check_that(invoice['firm_id'], hamcrest.equal_to(1))
    utils.check_that(invoice['iso_currency'], hamcrest.equal_to('RUB'))
    utils.check_that(invoice['consume_sum'], hamcrest.equal_to(params['expected']['effective_sum']))
