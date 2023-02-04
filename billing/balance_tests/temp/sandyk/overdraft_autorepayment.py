# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

import pytest

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features

# DT = datetime.datetime.now() - datetime.timedelta(days=1)
PERSON_TYPE = 'ph'
PAYSYS_ID = 1001
SERVICE_ID = 7
NON_CURRENCY_PRODUCT_ID = 1475
OVERDRAFT_LIMIT = 120
MAIN_DT = datetime.datetime.now()
QTY = 15
dt = datetime.datetime.now() - datetime.timedelta(days=31)


@pytest.mark.slow
@reporter.feature(Features.OVERDRAFT)
@pytest.mark.tickets('BALANCE-22004')
def test_fair_overdraft_mv_client():
    # client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    # person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    # steps.ClientSteps.set_force_overdraft(client_id, SERVICE_ID, OVERDRAFT_LIMIT, firm_id=1, start_dt=MAIN_DT,
    #                                           currency=None)
    # date_now = datetime.datetime.now()
    # for i in range(2):
    #
    #
    #     service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    #     order_id = steps.OrderSteps.create(client_id, service_order_id, NON_CURRENCY_PRODUCT_ID, SERVICE_ID)
    #     orders_list = [
    #         {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 15, 'BeginDT': date_now}
    #     ]
    #     request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
    #                                            additional_params=dict(InvoiceDesireDT=date_now))
    #     invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
    #                                                  credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    #
    # contract_id,_ =  steps.ContractSteps.create('no_agency',{'client_id': client_id, 'person_id': person_id,
    #                                                'dt'       : '2016-05-01T00:00:00',
    #                                                # 'FINISH_DT': None,
    #                                                'is_signed': '2016-05-01T00:00:00'
    #                                                # 'is_signed': None,
    #                                                ,'SERVICES': [7]
    #                                                ,'FIRM': 1
    #                                                ,'SCALE':1
    #                                                })
    #
    # service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    # order_id = steps.OrderSteps.create(client_id, service_order_id, NON_CURRENCY_PRODUCT_ID, SERVICE_ID)
    # orders_list = [
    #         {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 15, 'BeginDT': date_now}
    #     ]
    # request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
    #                                            additional_params=dict(InvoiceDesireDT=date_now))
    # invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
    #                                                  credit=0, contract_id=contract_id, overdraft=0, endbuyer_id=None)
    #
    # invoices = []
    # for i in range(2):
    #     print '!!!'
    #     print i
    #     service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    #     order_id = steps.OrderSteps.create(client_id, service_order_id, NON_CURRENCY_PRODUCT_ID, SERVICE_ID)
    #     orders_list = [
    #         {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 20, 'BeginDT': dt}
    #     ]
    #     request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
    #                                            additional_params=dict(InvoiceDesireDT=date_now))
    #     invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
    #                                                  credit=0, contract_id=contract_id, overdraft=1, endbuyer_id=None)
    #     invoices.append(invoice_id)
    #
    #     steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 15}, 0, dt)
    #
    #
    # for inv in invoices:
    #     db.balance().execute("update (select * from t_invoice where id = :invoice_id) set payment_term_dt =:dt,dt =:dt",
    #               {'invoice_id': inv, 'dt': dt})
    #     db.balance().execute("update (select * from T_CONSUME   where invoice_id = :invoice_id) set dt =:dt",
    #               {'invoice_id': inv, 'dt': dt})
    #
    # steps.OverdraftSteps.overdraft_job(client_id)
    # steps.ActsSteps.generate(client_id, 1, dt)
    #
    # for inv in invoices:
    #     db.balance().execute("update (select * from t_act   where invoice_id = :invoice_id) set payment_term_dt =:dt,dt =:dt",
    #               {'invoice_id': inv, 'dt': dt})
    #
    # steps.OverdraftSteps.overdraft_job(client_id)


    # steps.InvoiceSteps.pay(52279921, None, None)
    steps.OverdraftSteps.overdraft_job(13615851)


if __name__ == "__main__":
    test_fair_overdraft_mv_client()