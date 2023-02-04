# -*- coding: utf-8 -*-
from datetime import date

__author__ = 'sandyk'

import datetime

import allure
import pytest

import balance.balance_db as db
from balance import balance_steps as steps
from balance.features import Features
from decimal import Decimal
import balance.balance_api as api

DT = datetime.datetime.now()
PERSON_TYPE = 'kzu'
PAYSYS_ID =1002
SERVICE_ID =7
NON_CURRENCY_PRODUCT_ID =  1475
# 503166
# OVERDRAFT_LIMIT = 1000
MAIN_DT = datetime.datetime.now()
SHIPMENT_DT =MAIN_DT.replace(minute=0, hour=0, second=0, microsecond=0)
QTY = 250
START_DT = str(datetime.datetime.today().strftime("%Y-%m-%d")) + 'T00:00:00'

# 502962
@pytest.mark.slow
@allure.feature(Features.OVERDRAFT)
@pytest.mark.tickets('BALANCE-22004')
def test_fair_overdraft_mv_client():
    # client_id = steps.ClientSteps.create(params={'IS_AGENCY':0})
    # # # steps.ClientSteps.link(client_id,'clientuid45')
    # person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    # # # contract_id = None
    # contract_id = steps.ContractSteps.create_contract_new('commiss', {'CLIENT_ID': client_id,
    #                                                                              'PERSON_ID': person_id,
    #                                                                             'IS_FAXED': START_DT, 'DT': START_DT, 'IS_SIGNED': START_DT,
    #                                                                               'FIRM': 1, 'SERVICES':[SERVICE_ID],
    #                                                                         'PAYMENT_TYPE':2,
    #     'UNILATERAL':1,
    #  'CURRENCY':810})[0]

    ##даем честный овердрафт
    # steps.ClientSteps.set_overdraft(client_id, SERVICE_ID, OVERDRAFT_LIMIT, firm_id=1, start_dt=MAIN_DT,
    #                                             currency=None, invoice_currency=None)
    # db.balance().execute('update (select * from t_export where OBJECT_ID = :client_id and type = \'OVERDRAFT\') set priority=-1',
    #           {'client_id': client_id})
    # steps.CommonSteps.wait_for(
    #             'select state as val from t_export where OBJECT_ID = :client_id and type = \'OVERDRAFT\' and classname = \'Client\'',
    #             {'client_id': client_id}, 1, interval=2)
    # service_order_id = 1000
    # client_id = 39901727
    # # steps.ClientSteps.link(client_id, 'clientuid34')
    # person_id = 5705922
    # agency_id = 56064644
    client_id = 998108
    # person_id = 5735465
    # contract_id = 267340
    # service_order_id = 62146218

    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id, service_order_id, NON_CURRENCY_PRODUCT_ID, SERVICE_ID ,
                                       {'TEXT': 'Py_Test order', 'AgencyID': None, 'ManagerUID': None})

    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': DT},
     ]

    ##переводим на мультивалютность
    # steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='COPY', dt=DT,
    #                                       region_id=159,currency='KZT')

    ##пересчитываем овердрафт
    # api.test_balance().CalculateOverdraft([client_id])
    # steps.CommonSteps.export('OVERDRAFT', 'Client', client_id)
    # db.balance().execute('update (select * from t_export where OBJECT_ID = :client_id and type = \'OVERDRAFT\') set priority=-1',
    #           {'client_id': client_id})
    # steps.CommonSteps.wait_for(
    #             'select state as val from t_export where OBJECT_ID = :client_id and type = \'OVERDRAFT\' and classname = \'Client\'',
    #             {'client_id': client_id}, 1, interval=2)
    #
    # limit = db.balance().execute('select overdraft_limit from T_CLIENT_OVERDRAFT  where client_id = :client_id',
    #           {'client_id': client_id})[0]['overdraft_limit']
    #
    # assert str(limit) == str(OVERDRAFT_LIMIT*30), 'Please check overdraft for client %s'%client_id

    ##выставляем счет
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=DT))

    #
    # invoice_id = 64836364
    # service_order_id = '20000000383717'
    # client_id = 39940097
    # invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
    #                                              credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    # steps.InvoiceSteps.pay(invoice_id)

    # steps.CampaignsSteps.update_campaigns(SERVICE_ID, service_order_id,
    #                                       {'Bucks': Decimal('10.12345601')}, do_stop=0, campaigns_dt=MAIN_DT)

    # api.medium().DailyShipments([{'Clicks':Decimal('10.12345601'), 'ServiceID':SERVICE_ID, 'ServiceOrderID':service_order_id,
    # 'dt':SHIPMENT_DT}])
    # steps.CommonSteps.export('PROCESS_COMPLETION', 'Order', order_id)


    # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 50}, 0, campaigns_dt=MAIN_DT)
    # steps.ActsSteps.generate(client_id, 1, MAIN_DT)
    # result = db.balance().execute('select total_sum, overdraft from t_invoice where id = :invoice_id',
    #           {'invoice_id': invoice_id})
    # assert (result[0]['total_sum'] == str(QTY)) and (result[0]['overdraft'] == 1), 'Check invoice %s'%invoice_id


if __name__ == "__main__":
    test_fair_overdraft_mv_client()