# -*- coding: utf-8 -*-

import pprint
import datetime
from sys import argv

from temp.MTestlib import MTestlib as mtl


def Print(obj):
    print pprint.pformat(obj).decode('unicode_escape')


rpc = mtl.rpc
test_rpc = mtl.test_rpc

uid = 'clientuid33'
manager_uid = None  ##'96446401'

service_id = 7;
product_id = 1475  ##503162
##service_id = 11; product_id = 2136
##service_id = 70; product_id = 502761
qty = 100
paysys_id = 1018

dt = datetime.datetime(2015, 5, 15, 11, 0, 0)
disc_dt = datetime.datetime(2015, 6, 10, 11, 0, 0)

begin_dt = dt
request_dt = dt
invoice_dt = dt
payment_dt = dt
campaigns_dt = dt
act_dt = dt
migrate_dt = dt

# ------------------------------------------------------------------------------
# В структуре ниже описаны 4 клиента
scenario = [[7, 1475, 500, 1]
    , [7, 1475, 500, 1]
    , [7, 1475, 500, 1]
    , [7, 1475, 500, 1]
            ]
##scenario = [[11,2136,500,1]
##          , [11,2136,500,1]
##          , [11,2136,500,1]
##          , [11,2136,500,1]
##          ]
# debug
argv.append('-test-case')
argv.append(5)

if argv[1] and argv[1] == '-test-case': flag = argv[2]
# test: control
if flag == 0: scenario.append([11, 2136, 500, 1]);
# test: 4 clients in period
if flag == 1:
    pass
# test: 20000.00 UAH in period
elif flag == 2:
    scenario.append([7, 1475, 222.222000, 1]);
# test: 19999.99 UAH in period
elif flag == 3:
    scenario.append([7, 1475, 222.221111, 1]);
# test: >69.50% per single client by Direct in period
elif flag == 4:
    scenario.append([7, 1475, 4558, 1])
# test: <69.50% per single client by Direct in period
elif flag == 5:
    scenario.append([7, 1475, 4557, 1])
# test:
##elif flag == 6: scenario.append([114,503070,500,1])
# ------------------------------------------------------------------------------

client_id = None or mtl.create_client({'IS_AGENCY': 0, 'NAME': u'Petrov3'})
##    mtl.create_client({'CLIENT_ID': client_id, 'REGION_ID': '225', 'CURRENCY': 'RUB', 'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(seconds=5), 'SERVICE_ID': 7, 'CURRENCY_CONVERT_TYPE': 'MODIFY'})
agency_id = None or mtl.create_client({'IS_AGENCY': 1, 'name': u'Иванов Иван Иванович'})  ##mass
order_owner = client_id
invoice_owner = agency_id
if order_owner == invoice_owner: agency_id = None
mtl.link_client_uid(invoice_owner, 'clientuid32')
person_id = None or mtl.create_person(invoice_owner, 'pu', {'phone': '234'})
contract_id = mtl.create_contract2('ukr_opt_ag_prem',
                                   {'client_id': agency_id, 'person_id': person_id, 'dt': '2015-04-01T00:00:00',
                                    'FINISH_DT': '2015-10-22T00:00:00', 'SERVICES': [114]})
##    contract_id = None

# ---------- Turnover in last period ----------
for service_id, product_id, completions, part in scenario:
    tmp_client_id = None or mtl.create_client({'IS_AGENCY': 0, 'NAME': u'Petrov3'})
    campaigns_list = [
        {'client_id': tmp_client_id, 'service_id': service_id, 'product_id': product_id, 'qty': completions,
         'begin_dt': begin_dt}
        ##        , {'client_id': client_id2, 'service_id': service_id, 'product_id': product_id, 'qty': qty, 'begin_dt': begin_dt}
    ]
    invoice_id, orders_list = mtl.create_force_invoice(tmp_client_id, person_id, campaigns_list, paysys_id,
                                                       invoice_dt, agency_id=agency_id, credit=0,
                                                       contract_id=contract_id, overdraft=0, manager_uid=manager_uid)

    mtl.OEBS_payment(invoice_id, None, None)
    mtl.do_campaigns(service_id, orders_list[0]['ServiceOrderID'], {'Bucks': completions * part, 'Money': 0}, 0,
                     campaigns_dt)

    mtl.test_rpc.ExecuteSQL("update t_deferpay set issue_dt = date'2015-03-15' where invoice_id = %d" % invoice_id);
    mtl.test_rpc.ExecuteSQL("commit");

mtl.act_accounter(invoice_owner, 1, dt)

# ---------- Discount ----------
mtl.log(rpc.Balance.EstimateDiscount)({'ClientID': invoice_owner, 'PaysysID': paysys_id, 'ContractID': contract_id}, [
    {'ProductID': product_id, 'ClientID': order_owner, 'Qty': qty, 'ID': 1, 'BeginDT': disc_dt, 'RegionID': None,
     'discard_agency_discount': 0}])
##print "rpc.Balance.EstimateDiscount({'ClientID': %d, 'PaysysID': %d, 'ContractID': %d}, [{'ProductID': %d, 'ClientID': %d, 'Qty': %d, 'ID': 1, 'BeginDT': %s, 'RegionID': 1, 'discard_agency_discount': 0}])" % (agency_id, paysys_id, contract_id, product_id,client_id,qty,'datetime.datetime(2015,4,21,0,0,0)')

# ---------- Request ----------
service_order_id = mtl.get_next_service_order_id(service_id)
order_id = mtl.create_or_update_order(order_owner, product_id, service_id, service_order_id,
                                      {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)
orders_list = [
    {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': disc_dt}
]
request_id = mtl.create_request(invoice_owner, orders_list, disc_dt)
invoice_id = mtl.create_invoice(request_id, person_id, paysys_id, credit=1, contract_id=contract_id, overdraft=0,
                                endbuyer_id=None)
