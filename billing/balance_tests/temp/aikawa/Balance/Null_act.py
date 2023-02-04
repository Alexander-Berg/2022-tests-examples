import datetime

import hamcrest
import pytest

from balance import balance_db as db
from balance import balance_steps as steps
from btestlib.constants import Firms

SERVICE_ID = 7
PRODUCT_ID = 1475
FIRM_ID = Firms.YANDEX_1.id
PAYSYS_ID = 1003
TODAY = datetime.datetime.now()
import btestlib.utils as utils


def test_internal_acts():
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})

    person_id = steps.PersonSteps.create(client_id, 'ur')

    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                       service_order_id=service_order_id, params={'AgencyID': None})
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': TODAY}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params={'InvoiceDesireDT': TODAY})

    invoice_id1, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                  credit=0, contract_id=None, overdraft=0)
    steps.InvoiceSteps.pay(invoice_id1)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 0.00001}, 0, TODAY)

    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                       service_order_id=service_order_id, params={'AgencyID': None})
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': TODAY}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params={'InvoiceDesireDT': TODAY})

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0)
    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 0.5}, 0, TODAY)

    steps.ActsSteps.enqueue([client_id], force=1, date=TODAY)
    sql_query = "SELECT input FROM t_export WHERE type = 'MONTH_PROC' AND classname = 'Client' AND object_id = {0}".format(
        client_id)
    export_input = steps.CommonSteps.get_pickled_value(sql_query, 'input')
    export_input['invoices'] = [invoice_id1]
    #
    # db.balance().execute(
    # "UPDATE t_export SET input = :input WHERE state = 0 AND type = 'MONTH_PROC' AND classname = 'Client' AND object_id = :client_id",
    # {'input': mtl.set_input_value(export_input), 'client_id': client_id})

    steps.CommonSteps.export('MONTH_PROC', 'Client', client_id, input_=export_input)
    # act_id = steps.ActsSteps.generate(client_id, force=1, date=TODAY)[0]
    # input_ ={'enq_operation_id': '276524314', 'force': '1', 'invoices': '[69977872]'}

    print db.get_acts_by_client(client_id)


def test_internal_acts2():
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    person_id = steps.PersonSteps.create(client_id, 'ur')
    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                       service_order_id=service_order_id, params={'AgencyID': None})
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': TODAY}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params={'InvoiceDesireDT': TODAY})

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0)
    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 0.5}, 0, TODAY)

    steps.ActsSteps.enqueue([client_id], force=1, date=TODAY)
    steps.CommonSteps.export('MONTH_PROC', 'Client', client_id)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 0.50001}, 0, TODAY)
    steps.ActsSteps.enqueue([client_id], force=1, date=TODAY)
    sql_query = "SELECT input FROM t_export WHERE type = 'MONTH_PROC' AND classname = 'Client' AND object_id = {0}".format(
        client_id)
    export_input = steps.CommonSteps.get_pickled_value(sql_query, 'input')
    print export_input
    steps.CommonSteps.export('MONTH_PROC', 'Client', client_id)
    # sql_query = "SELECT input FROM t_export WHERE type = 'MONTH_PROC' AND classname = 'Client' AND object_id = {0}".format(
    #     client_id)
    # export_input = steps.CommonSteps.get_pickled_value(sql_query, 'input')
    # export_input['invoices'] = [invoice_id1]
    # #
    # # db.balance().execute(
    # # "UPDATE t_export SET input = :input WHERE state = 0 AND type = 'MONTH_PROC' AND classname = 'Client' AND object_id = :client_id",
    # # {'input': mtl.set_input_value(export_input), 'client_id': client_id})
    #
    # steps.CommonSteps.export('MONTH_PROC', 'Client', client_id, input_=export_input)
    # # act_id = steps.ActsSteps.generate(client_id, force=1, date=TODAY)[0]
    # # input_ ={'enq_operation_id': '276524314', 'force': '1', 'invoices': '[69977872]'}

    print db.get_acts_by_client(client_id)


def generate_data_test_transfer_acted_is_true():
    return [
        {'first_campaign': 100, 'second_campaign': 0, 'qty_old': 100, 'qty_new': 0},
        # {'first_campaign': 100, 'second_campaign': 50, 'qty_old': 100, 'qty_new': 50},
        # {'first_campaign': 80, 'second_campaign': 50, 'qty_old': 100, 'qty_new': 50},
    ]


@pytest.mark.parametrize('params',
                         generate_data_test_transfer_acted_is_true()

                         )
def test_transfer_acted(params):
    PERSON_TYPE = 'ph'
    PAYSYS_ID = 1001
    SERVICE_ID = 7
    PRODUCT_ID = 1475
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id, service_order_id, PRODUCT_ID, SERVICE_ID)

    service_order_id_2 = steps.OrderSteps.next_id(SERVICE_ID)
    order_id_2 = steps.OrderSteps.create(client_id, service_order_id_2, PRODUCT_ID, SERVICE_ID)

    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': TODAY},
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id_2, 'Qty': 80, 'BeginDT': TODAY}
    ]

    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=TODAY))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    invoice = db.get_invoice_by_id(invoice_id)[0]
    transfer_acted_value = invoice['transfer_acted']

    utils.check_that(transfer_acted_value, hamcrest.equal_to(1))

    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': params['first_campaign']}, 0, TODAY)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id_2, {'Bucks': 60}, 0, TODAY)
    steps.ActsSteps.generate(client_id, force=1, date=TODAY)

    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': params['second_campaign']}, 0, TODAY)

    service_order_id_3 = steps.OrderSteps.next_id(SERVICE_ID)

    order_id_3 = steps.OrderSteps.create(client_id, service_order_id_3, PRODUCT_ID, SERVICE_ID)
    steps.OrderSteps.transfer(
        [{'order_id': order_id, 'qty_old': params['qty_old'], 'qty_new': params['qty_new'], 'all_qty': 0}],
        [{'order_id': order_id_3, 'qty_delta': 1}]
    )
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id_3, {'Bucks': 99}, 0, TODAY)
    print steps.ActsSteps.enqueue([client_id], force=1, date=TODAY)
    steps.ExportSteps.get_export_data(client_id, 'Client', 'MONTH_PROC')
    steps.CommonSteps.export('MONTH_PROC', 'Client', client_id)
    # steps.ActsSteps.generate(client_id, force=1, date=TODAY)
