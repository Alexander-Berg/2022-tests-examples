__author__ = 'aikawa'

import datetime
from decimal import Decimal

from temp.igogor.balance_objects import Contexts
from btestlib.constants import Firms, Nds
from balance import balance_steps as steps

DIRECT_YANDEX_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1, precision=Decimal('0.000001'),
                                                               nds=Nds.YANDEX_RESIDENT, nds_included=True)
DT_18_NDS = datetime.datetime(2018, 11, 1)
DT_20_NDS = datetime.datetime(2019, 1, 1)

SERVICE_ID = 7
PRODUCT_ID = 1475
PAYSYS_ID = 1001

# TODO: no check_that


def test_act_amount_is_amount_nds():
    client_id = steps.ClientSteps.create()

    person_id = steps.PersonSteps.create(client_id, 'ur')

    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID,
                                       service_id=SERVICE_ID)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': DT_18_NDS}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=DT_18_NDS))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=1003,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id, payment_dt=DT_18_NDS)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 50}, 0, DT_18_NDS)
    act_id = steps.ActsSteps.generate(client_id, force=1, date=DT_18_NDS)[0]

    # steps.CommonSteps.export('OEBS', 'Act', act_id)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 100}, 0, DT_20_NDS)
    act_id = steps.ActsSteps.generate(client_id, force=1, date=DT_20_NDS)[0]
    # steps.CommonSteps.export('OEBS', 'Act', act_id)


def test_act_amount_is_a1mount_nds():
    client_id = steps.ClientSteps.create()

    person_id = steps.PersonSteps.create(client_id, 'ph')

    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID,
                                       service_id=SERVICE_ID)

    service_order_id2 = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id2 = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id2, product_id=PRODUCT_ID,
                                        service_id=SERVICE_ID)

    service_order_id3 = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id3 = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id3, product_id=PRODUCT_ID,
                                        service_id=SERVICE_ID)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': DT_18_NDS},
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id2, 'Qty': 100, 'BeginDT': DT_18_NDS}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=DT_18_NDS))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id, payment_dt=DT_18_NDS)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 50}, 0, DT_18_NDS)
    act_id = steps.ActsSteps.generate(client_id, force=1, date=DT_18_NDS)[0]
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 20}, 0, DT_18_NDS)

    steps.OrderSteps.transfer([{'order_id': order_id, 'qty_old': 100, 'qty_new': 20, 'all_qty': 0}],
                              [{'order_id': order_id3, 'qty_delta': 1}])
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id3, {'Bucks': 80}, 0, DT_18_NDS)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id2, {'Bucks': 100}, 0, DT_20_NDS)
    act_id = steps.ActsSteps.generate(client_id, force=1, date=DT_20_NDS)[0]


def test_pay_after_nds_change():
    client_id = steps.ClientSteps.create()

    person_id = steps.PersonSteps.create(client_id, 'ph')

    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID,
                                       service_id=SERVICE_ID)

    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': DT_18_NDS}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=DT_18_NDS))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id, payment_dt=DT_20_NDS)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 100}, 0, DT_18_NDS)
    act_id = steps.ActsSteps.generate(client_id, force=1, date=DT_20_NDS)[0]
