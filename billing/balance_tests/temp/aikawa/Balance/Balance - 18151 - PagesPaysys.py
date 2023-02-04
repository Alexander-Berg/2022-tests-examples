# -*- coding: utf-8 -*-

__author__ = 'aikawa'

import datetime

from temp.MTestlib import MTestlib as mtl

rpc = mtl.rpc
test_rpc = mtl.test_rpc

after = datetime.datetime(2015, 7, 01, 11, 0, 0)
disc_dt = datetime.datetime(2015, 7, 01, 11, 0, 0)

begin_dt = after
act_dt = after
campaigns_dt = after

agency_id = None
manager_id = None
manager_uid = None
contract_id = None

# # -------- firm_id = 1 -------
# person_type = 'ph'  # ЮЛ резидент РФ
# paysys_id = 1000    # Яндекс.Деньги
# service_id = 7      # Директ
# product_id = 1475   # Рекламная кампания
# msr = 'Bucks'

# # -------- firm_id = 2 -------
person_type = 'pu'  # ЮЛ резидент UA
# paysys_id = 1032    # Яндекс.Деньги

# paysys_id = 1031    # Яндекс.Деньги

paysys_id = 1018  # Яндекс.Деньги

service_id = 7  # Директ
product_id = 1475  # Рекламная кампания
msr = 'Bucks'




# # -------- firm_id = 8 -------
# person_type = 'tru'  # ЮЛ резидент TR
# paysys_id = 1055    # Банк для юридических лиц в франках
#
# person_type = 'trp'  # ЮЛ резидент TR
# paysys_id = 1056    # Банк для юридических лиц в франках
# service_id = 7      # Директ
# product_id = 1475   # Рекламная кампания
# msr = 'Bucks'



qty = 100

client_id = mtl.create_client({'IS_AGENCY': 0, 'NAME': u'LastMan'})
person_id = None or mtl.create_person(client_id, person_type, {'phone': '234'})
mtl.link_client_uid(client_id, 'aikawa-test-0')
# mtl.link_client_uid(client_id, 'torvald-test-0')
service_order_id = mtl.get_next_service_order_id(service_id)
order_id = mtl.create_or_update_order(client_id, product_id, service_id, service_order_id,
                                      {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)
orders_list = [
    {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt}
]
request_id = mtl.create_request(client_id, orders_list, disc_dt)
invoice_id = mtl.create_invoice(request_id, person_id, paysys_id, credit=0, contract_id=contract_id, overdraft=0,
                                endbuyer_id=None)
mtl.OEBS_payment(invoice_id)

mtl.do_campaigns(service_id, service_order_id, {msr: 86}, 0, campaigns_dt)  # синхронно
# rpc.Balance.UpdateCampaigns([{"ServiceID": 7, "ServiceOrderID": service_order_id, "dt": campaigns_dt, "stop": 0, "Bucks": 70, "Money": 0}]) #асинхронно

mtl.create_act(invoice_id, act_dt)
