# -*- coding: utf-8 -*-

__author__ = 'aikawa'

import datetime

from temp.MTestlib import MTestlib as mtl

rpc = mtl.rpc
test_rpc = mtl.test_rpc

after = datetime.datetime(2015, 6, 24, 11, 0, 0)
disc_dt = datetime.datetime(2015, 5, 24, 11, 0, 0)

begin_dt = after
act_dt = after
campaigns_dt = after

manager_id = None
manager_uid = None


# person_type = 'ur'  # ЮЛ резидент РФ
# paysys_id = 1003    # Банк для юридических лиц
# service_id = 7      # Директ
# product_id = 1475   # Рекламная кампания

person_type = 'ur_autoru'  # ЮЛ резидент РФ авто_ру
paysys_id = 1091  # Банк для юридических лиц авто_ру
service_id = 99  # Авто_ру
product_id = 504568  # какой-то продукт авто_ру в днях

qty = 100

agency_id = mtl.create_client({'IS_AGENCY': 1, 'NAME': u'LastMan'})
client_id = mtl.create_client({'IS_AGENCY': 0, 'NAME': u'LastMan'})
person_id = None or mtl.create_person(agency_id, person_type, {'phone': '234'})
mtl.link_client_uid(client_id, 'clientuid32')

service_order_id = mtl.get_next_service_order_id(service_id)
order_id = mtl.create_or_update_order(client_id, product_id, service_id, service_order_id,
                                      {'TEXT': 'Py_Test order'}, agency_id=agency_id, manager_uid=manager_uid)
orders_list = [
    {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt}
]

request_id = mtl.create_request(agency_id, orders_list, disc_dt)

contract_id = mtl.create_contract2('auto_ru',
                                   {'client_id': agency_id, 'person_id': person_id, 'dt': '2015-01-01T00:00:00',
                                    'FINISH_DT': '2016-07-30T00:00:00', 'is_signed': '2015-01-01T00:00:00'})

invoice_id = mtl.create_invoice(request_id, person_id, paysys_id, credit=0, contract_id=contract_id, overdraft=0,
                                endbuyer_id=None)



mtl.OEBS_payment(invoice_id)

mtl.do_campaigns(service_id, service_order_id, {'Days': 50}, 0, campaigns_dt)
mtl.create_act(invoice_id, act_dt)
