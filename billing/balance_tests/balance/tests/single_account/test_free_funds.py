# coding=utf-8

import datetime
import pytest
from balance import balance_steps as steps
from balance import balance_db as db
from balance import balance_api as api
from temp.igogor.balance_objects import Contexts
from btestlib.constants import Services, Products, Paysyses
from btestlib import utils as utils

to_iso = utils.Date.date_to_iso_format
NOW = datetime.datetime.now()
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))

DIRECT = Contexts.DIRECT_MONEY_RUB_CONTEXT.new()
GEO = DIRECT.new(service=Services.GEO, product=Products.GEO)
MEDIA_70 = DIRECT.new(service=Services.MEDIA_70, product=Products.MEDIA)
METRICA = DIRECT.new(service=Services.METRICA, product=Products.METRICA)


def test_partly_turn_on():
    client_id = steps.ClientSteps.create({'REGION_ID': 225}, single_account_activated=False, enable_single_account=True)
    person_id = steps.PersonSteps.create(client_id, 'ur')
    diff = db.Diff({'client': [client_id], 'person': [person_id]})
    single_account_number = api.test_balance().SingleAccountProcessClient(client_id)
    print diff.get_diff()


@pytest.mark.parametrize('services', [
    # [DIRECT, GEO, MEDIA_70, METRICA],
    [DIRECT],
    # [MEDIA_70]
])
def test_free_funds(services):
    client_id = steps.ClientSteps.create({'REGION_ID': 225}, single_account_activated=True, enable_single_account=True)
    steps.ClientSteps.link(client_id, 'aikawa-test-10')
    steps.ClientSteps.set_force_overdraft(client_id, service_id=7, limit=100)
    person_id = steps.PersonSteps.create(client_id, 'ur')
    single_account_number = steps.ElsSteps.create_els(client_id)
    now = datetime.datetime.now()
    paysys_id = Paysyses.BANK_UR_RUB.id

    orders_list = []
    for context in services:
        service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)  # внешний ID заказа

        order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                           product_id=context.product.id, params={'AgencyID': None})
        orders_list.append({'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 1,
                            'BeginDT': NOW})
        # steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id,
        #                                   {context.product.type.code: 1}, 0, NOW)

    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params={'InvoiceDesireDT': NOW})

    #
    invoice_id, external_id, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=0,
                                                           contract_id=None, overdraft=0, endbuyer_id=None)
    payment_id = db.get_payments_by_invoice(invoice_id)[0]['id']
    personal_account_id, _ = steps.ElsSteps.get_ls_by_person_id_and_els_number(person_id=person_id,
                                                                               els_number=single_account_number)
    steps.InvoiceSteps.pay_fair(personal_account_id, payment_sum=350000, orig_id=payment_id)
    for order in orders_list:
        steps.CampaignsSteps.do_campaigns(order['ServiceID'], order['ServiceOrderID'],
                                          {'Money': 1}, 0, NOW)
    # import time
    # time.sleep(30)
    # steps.InvoiceSteps.pay_fair(personal_account_id, payment_sum=2000, orig_id=None)
    act_id = steps.ActsSteps.generate(client_id, force=1, date=datetime.datetime.now())[0]
    message_id = db.get_message_by_object_id(personal_account_id)[0]['id']
    print steps.MessageSteps.get_message_data(message_id)
    print steps.RequestSteps.get_request_choices(request_id, show_disabled_paysyses=False)
