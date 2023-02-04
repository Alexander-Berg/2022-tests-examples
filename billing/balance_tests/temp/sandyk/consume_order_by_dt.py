# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

import balance.balance_db as db
from balance import balance_steps as steps

DT = datetime.datetime.now() - datetime.timedelta(days=1)
PERSON_TYPE = 'ph'
PAYSYS_ID = 1001
SERVICE_ID = 7
NON_CURRENCY_PRODUCT_ID = 1475
OVERDRAFT_LIMIT = 120
MAIN_DT = datetime.datetime.now()
QTY = 30
dt = datetime.datetime.now() - datetime.timedelta(days=15)
dt2 = datetime.datetime.now() - datetime.timedelta(days=10)


# @pytest.mark.slow
# @reporter.feature(AllureFeatures.OVERDRAFT)
# @pytest.mark.tickets('BALANCE-22004')
def test_fair_overdraft_mv_client():

    # ########## 1 откат ##########
    # ########## https://balance-admin.greed-load2e.yandex.ru/order.xml?service_cc=PPC&service_order_id=23563360
    # client_id = steps.ClientSteps.create()
    # person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    # service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    # order_id = steps.OrderSteps.create(client_id, service_order_id, NON_CURRENCY_PRODUCT_ID, SERVICE_ID)
    # orders_list = [
    #     {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': dt}
    # ]
    # request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
    #                                        additional_params=dict(InvoiceDesireDT=dt))
    # invoices = []
    # bucks = 30
    # for inv in range(3):
    #     invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
    #                                                  credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    #     invoices.append(invoice_id)
    #     steps.InvoiceSteps.pay(invoice_id, None, None)
    #     steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': bucks}, 0, campaigns_dt=dt)
    #     if inv == 1:
    #         bucks += 10
    #     else:
    #         bucks += 30
    #
    # db.balance().execute("update (select * from T_CONSUME   where invoice_id = :invoice_id) set dt =:dt",
    #           {'invoice_id': invoices[0], 'dt': dt2})
    # db.balance().execute("update (select * from T_CONSUME   where invoice_id = :invoice_id) set dt =:dt",
    #           {'invoice_id': invoices[1], 'dt': dt})
    #
    # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 10}, 0, campaigns_dt=dt)


    ########## 2 перекрутка ########## что с ней мб не так?
    ##########
    # client_id = steps.ClientSteps.create()
    # person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    # service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    # order_id = steps.OrderSteps.create(client_id, service_order_id, NON_CURRENCY_PRODUCT_ID, SERVICE_ID)
    # orders_list = [
    #     {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': dt}
    # ]
    # request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
    #                                        additional_params=dict(InvoiceDesireDT=dt))
    # invoices = []
    # bucks = 30
    # for inv in range(3):
    #     invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
    #                                                  credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    #     invoices.append(invoice_id)
    #     steps.InvoiceSteps.pay(invoice_id, None, None)
    #     steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': bucks}, 0, campaigns_dt=dt)
    #     if inv == 1:
    #         bucks += 10
    #     else:
    #         bucks += 30
    #
    # db.balance().execute("update (select * from T_CONSUME   where invoice_id = :invoice_id) set dt =:dt",
    #           {'invoice_id': invoices[0], 'dt': dt2})
    # db.balance().execute("update (select * from T_CONSUME   where invoice_id = :invoice_id) set dt =:dt",
    #           {'invoice_id': invoices[1], 'dt': dt})
    #
    # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 100}, 0, campaigns_dt=dt)

#####
    # client_id = 10978646
    # person_id = 4192018
    # service_order_id = 23563380
    #
    # # order_id = steps.OrderSteps.create(client_id, service_order_id, NON_CURRENCY_PRODUCT_ID, SERVICE_ID)
    # orders_list = [
    #     {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 7, 'BeginDT': dt}
    # ]
    # request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
    #                                        additional_params=dict(InvoiceDesireDT=dt))
    # invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
    #                                                  credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    # steps.InvoiceSteps.pay(invoice_id, None, None)

#####

  ########## 3 переносы в ЕС ##########
    #### https://balance-admin.greed-load2e.yandex.ru/order.xml?service_order_id=23563386&service_cc=PPC&ncrnd=79781
    #### https://balance-admin.greed-load2e.yandex.ru/order.xml?service_order_id=23563385&service_cc=PPC&ncrnd=7246
    ########## (прислать открутку на дочерний, запустить перенос)
    # client_id = steps.ClientSteps.create()
    # person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    # service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    # service_order_id1 = steps.OrderSteps.next_id(SERVICE_ID)
    # order_id = steps.OrderSteps.create(client_id, service_order_id, NON_CURRENCY_PRODUCT_ID, SERVICE_ID)
    # order_id1 = steps.OrderSteps.create(client_id, service_order_id1, NON_CURRENCY_PRODUCT_ID, SERVICE_ID)
    # orders_list = [
    #     {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': dt}
    #     ,{'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id1, 'Qty': QTY, 'BeginDT': dt}
    # ]
    # request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
    #                                        additional_params=dict(InvoiceDesireDT=dt))
    # invoices = []
    # bucks = 30
    # for inv in range(3):
    #     print inv
    #     invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
    #                                                  credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    #     invoices.append(invoice_id)
    #     steps.InvoiceSteps.pay(invoice_id, None, None)
    #     steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': bucks}, 0, campaigns_dt=dt)
    #     steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id1, {'Bucks': bucks}, 0, campaigns_dt=dt)
    #
    #     if inv == 1:
    #         bucks += 10
    #     else:
    #         bucks += 30
    #
    #
    # db.balance().execute("update (select * from T_CONSUME   where invoice_id = :invoice_id) set dt =:dt",
    #           {'invoice_id': invoices[0], 'dt': dt2})
    # db.balance().execute("update (select * from T_CONSUME   where invoice_id = :invoice_id) set dt =:dt",
    #           {'invoice_id': invoices[1], 'dt': dt})
    #
    # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 10}, 0, campaigns_dt=dt)
    # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id1, {'Bucks': 10}, 0, campaigns_dt=dt)
    #
    # steps.OrderSteps.merge(order_id,[order_id1])

    # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id , {'Bucks': 40}, 0, campaigns_dt=dt)
    # steps.OrderSteps.ua_enqueue([client_id])


    ########## 4  акт по предоплате (3 разных счета) ##########
    ####
    ####
    ##########

    # client_id = steps.ClientSteps.create()
    # person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    # service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    # order_id = steps.OrderSteps.create(client_id, service_order_id, NON_CURRENCY_PRODUCT_ID, SERVICE_ID)
    # orders_list = [
    #     {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': dt}
    # ]
    # request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
    #                                        additional_params=dict(InvoiceDesireDT=dt))
    # invoices = []
    # bucks = 30
    # for inv in range(3):
    #     invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
    #                                                  credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    #     invoices.append(invoice_id)
    #     steps.InvoiceSteps.pay(invoice_id, None, None)
    #     steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': bucks}, 0, campaigns_dt=dt)
    #     if inv == 1:
    #         bucks += 10
    #     else:
    #         bucks += 30
    #
    # db.balance().execute("update (select * from T_CONSUME   where invoice_id = :invoice_id) set dt =:dt",
    #           {'invoice_id': invoices[0], 'dt': dt2})
    # db.balance().execute("update (select * from T_CONSUME   where invoice_id = :invoice_id) set dt =:dt",
    #           {'invoice_id': invoices[1], 'dt': dt})
    # for inv in invoices:
    #     steps.ActsSteps.create(inv)

    ########## 5 акт по предоплате (один счет, 2 заявки) ##########
    ####https://balance-admin.greed-load2e.yandex.ru/order.xml?service_order_id=23563406&service_cc=PPC&ncrnd=7246
    ####
    ##########
    # client_id = steps.ClientSteps.create()
    # person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    #
    # service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    # order_id = steps.OrderSteps.create(client_id, service_order_id, NON_CURRENCY_PRODUCT_ID, SERVICE_ID)
    # orders_list = [
    #     {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': dt}
    # ]
    #
    # request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))
    # invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
    #                                              credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    # steps.InvoiceSteps.pay(invoice_id, None, None)
    # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 10}, 0, campaigns_dt = dt)
    #
    # db.balance().execute("update (select * from T_order  where id = :order_id) set MANAGER_CODE =20453",
    #               {'order_id': order_id})
    #
    #
    # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 15}, 0, campaigns_dt = dt)
    # cons_id = db.balance().execute('select max(id) as id from T_CONSUME where invoice_id = :invoice_id',
    #           {'invoice_id': invoice_id, 'dt': dt2})[0]['id']
    # db.balance().execute("update (select * from T_CONSUME   where id = :cons_id) set dt =:dt",
    #           {'cons_id': cons_id, 'dt': dt2})

    ## update (select * from T_CONSUME where PARENT_ORDER_ID = 32665557 and COMPLETION_QTY = 10 ) set current_sum = 750, current_qty = 25;
    ## update (select * from T_CONSUME where PARENT_ORDER_ID = 32665557 and COMPLETION_QTY =5 ) set current_sum = 150, current_qty = 5;
    # steps.ActsSteps.create(52607308)


########## 6 переносы и акты в ЕС  ##########
    ## главный https://balance-admin.greed-load2e.yandex.ru/order.xml?service_order_id=23563431&service_cc=PPC
    ## дочерний https://balance-admin.greed-load2e.yandex.ru/order.xml?service_order_id=23563432&service_cc=PPC
    ######## (прислать открутку на дочерний, запустить перенос)
    # client_id = steps.ClientSteps.create()
    # person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    # service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    # service_order_id1 = steps.OrderSteps.next_id(SERVICE_ID)
    # order_id = steps.OrderSteps.create(client_id, service_order_id, NON_CURRENCY_PRODUCT_ID, SERVICE_ID)
    # order_id1 = steps.OrderSteps.create(client_id, service_order_id1, NON_CURRENCY_PRODUCT_ID, SERVICE_ID)
    # orders_list = [
    #     {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': dt}
    #     ,{'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id1, 'Qty': QTY, 'BeginDT': dt}
    # ]
    # request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
    #                                        additional_params=dict(InvoiceDesireDT=dt))
    # invoices = []
    # bucks = 30
    # for inv in range(3):
    #     print inv
    #     invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
    #                                                  credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    #     invoices.append(invoice_id)
    #     steps.InvoiceSteps.pay(invoice_id, None, None)
    #     steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': bucks}, 0, campaigns_dt=dt)
    #     steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id1, {'Bucks': bucks}, 0, campaigns_dt=dt)
    #
    #     if inv == 1:
    #         bucks += 10
    #     else:
    #         bucks += 30
    #
    #
    # db.balance().execute("update (select * from T_CONSUME   where invoice_id = :invoice_id) set dt =:dt",
    #           {'invoice_id': invoices[0], 'dt': dt2})
    # db.balance().execute("update (select * from T_CONSUME   where invoice_id = :invoice_id) set dt =:dt",
    #           {'invoice_id': invoices[1], 'dt': dt})
    #
    # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 10}, 0, campaigns_dt=dt)
    # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id1, {'Bucks': 10}, 0, campaigns_dt=dt)
    #
    # steps.OrderSteps.merge(order_id,[order_id1])
    #
    # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id1 , {'Bucks': 40}, 0, campaigns_dt=dt)
    # steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)
    # steps.ActsSteps.generate(client_id)



########## 7 Новый ЛС (возврат в кредит)  - как по одному заказу полусить несколько заявок##########  - ок
    ## https://balance-admin.greed-load2e.yandex.ru/invoice.xml?invoice_id=52607330
    ## https://balance-admin.greed-tm1f.yandex.ru/invoice.xml?invoice_id=52095566
    ######## (вернуть 300 - снимается с самой старой (по дате) заявки)
    # agency_id =  steps.ClientSteps.create({'IS_AGENCY': 1})
    # client_id =  steps.ClientSteps.create({'IS_AGENCY': 0})
    # invoice_owner = agency_id
    # order_owner = client_id
    # person_id = steps.PersonSteps.create(invoice_owner, PERSON_TYPE)
    #
    # contract_id, _ = steps.ContractSteps.create('opt_prem_post',{'client_id': invoice_owner, 'person_id': person_id,
    #                                                # 'dt'       : '2015-04-30T00:00:00',
    #                                                # 'FINISH_DT': None,
    #                                                'is_signed': '2015-01-01T00:00:00'
    #                                                # 'is_signed': None,
    #                                                ,'SERVICES': [7]
    #                                                # 'FIRM': 1,
    #                                                ,'SCALE':1
    #                                                })
    #
    # service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    # service_order_id1 = steps.OrderSteps.next_id(SERVICE_ID)
    # order_id = steps.OrderSteps.create(order_owner, service_order_id, NON_CURRENCY_PRODUCT_ID, SERVICE_ID,
    #     {'TEXT':'Py_Test order','AgencyID' : invoice_owner, 'ManagerUID': None})
    # order_id1 = steps.OrderSteps.create(order_owner, service_order_id1, NON_CURRENCY_PRODUCT_ID, SERVICE_ID,
    #     {'TEXT':'Py_Test order','AgencyID' : invoice_owner, 'ManagerUID': None})
    # orders_list = [
    #     {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': dt}
    #     ,{'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id1, 'Qty': QTY, 'BeginDT': dt}
    # ]
    #
    # invoices = []
    # bucks = 30
    #
    # for inv in range(3):
    #     print inv
    #     request_id = steps.RequestSteps.create(client_id=invoice_owner, orders_list=orders_list,
    #                                        additional_params=dict(InvoiceDesireDT=dt))
    #     invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
    #                                                  credit=1, contract_id=contract_id, overdraft=0, endbuyer_id=None)
    #     invoices.append(invoice_id)
    #     steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': bucks}, 0, campaigns_dt=dt)
    #     steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id1, {'Bucks': bucks}, 0, campaigns_dt=dt)
    #
    #     if inv == 1:
    #         bucks += 10
    #     else:
    #         bucks += 30
    #
    # cons_id = db.balance().execute('select max(id) as id from T_CONSUME where invoice_id = :invoice_id',
    #           {'invoice_id': invoice_id})[0]['id']
    #
    # db.balance().execute("update (select * from T_CONSUME   where id = :cons_id) set dt =:dt",
    #           {'cons_id': cons_id, 'dt': dt2})




    # ########## 1 откат ##########
    # ########## https://balance-admin.greed-load2e.yandex.ru/order.xml?service_cc=PPC&service_order_id=23563360
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id, service_order_id, NON_CURRENCY_PRODUCT_ID, SERVICE_ID)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=dt))
    invoices = []
    bucks = 30
    for inv in range(3):
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                     credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
        invoices.append(invoice_id)
        steps.InvoiceSteps.pay(invoice_id, None, None)
        steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': bucks}, 0, campaigns_dt=dt)
        if inv == 1:
            bucks += 10
        else:
            bucks += 30

    db.balance().execute("update (select * from T_CONSUME   where invoice_id = :invoice_id) set dt =:dt",
                         {'invoice_id': invoices[0], 'dt': dt2})
    db.balance().execute("update (select * from T_CONSUME   where invoice_id = :invoice_id) set dt =:dt",
                         {'invoice_id': invoices[1], 'dt': dt})

    # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 10}, 0, campaigns_dt=dt)




if __name__ == "__main__":
    test_fair_overdraft_mv_client()



  #
  # client_id = steps.ClientSteps.create()
  #   person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
  #
  #   service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
  #   order_id = steps.OrderSteps.create(client_id, service_order_id, NON_CURRENCY_PRODUCT_ID, SERVICE_ID)
  #   orders_list = [
  #       {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': dt}
  #   ]
  #
  #   request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))
  #   invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
  #                                                credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
  #   steps.InvoiceSteps.pay(invoice_id, None, None)
  #   steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 10}, 0, campaigns_dt = dt)
  #
    #   db.balance().execute("update (select * from T_order  where id = :order_id) set MANAGER_CODE =20453",
  #                 {'order_id': order_id})
  #
  #
  #   steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 15}, 0, campaigns_dt = dt)
    #   cons_id = db.balance().execute('select max(id) as id from T_CONSUME where invoice_id = :invoice_id',
  #             {'invoice_id': invoice_id, 'dt': dt2})[0]['id']
    #   db.balance().execute("update (select * from T_CONSUME   where id = :cons_id) set dt =:dt",
  #             {'cons_id': cons_id, 'dt': dt2})