#-*- coding: utf-8 -*-

import datetime
import pprint
import sys
import time

from MTestlib import MTestlib as mtl


def Print(obj):
    print pprint.pformat(obj).decode('unicode_escape')

rpc = mtl.rpc
test_rpc = mtl.test_rpc
auto_prefix = '[MT]: '

uid = 'clientuid33'

service_id = 7; product_id = 1475 #503162
##service_id = 11; product_id = 2136
##service_id = 70; product_id = 503273
##service_id = 77; product_id = 2584
# service_id = 99; product_id = 504697 #504596
##service_id = 5; product_id = 85 #503162
##service_id = 48; product_id = 503369
qty        = 100
paysys_id    = 1003
manager_uid = None #'96446401', '27116496'

def data_generator (paysys_id, person_type, dt = datetime.datetime.now(), orders = ['7-1475'], multi = None, mode = 'paypreview'):

    begin_dt     = dt;  request_dt   = dt; invoice_dt   = dt
    payment_dt   = dt ; campaigns_dt = dt; act_dt       = dt
    migrate_dt   = dt

    client_id = None or mtl.create_client({'IS_AGENCY': 0, 'NAME': u'Petrov3'})
    agency_id = None or mtl.create_client({'IS_AGENCY': 1, 'name': u'Иванов Иван Иванович'}) ##mass
    order_owner   = client_id
    invoice_owner = client_id
    if order_owner == invoice_owner: agency_id = None
    mtl.link_client_uid(invoice_owner, 'clientuid32')
    person_id = None or mtl.create_person(invoice_owner, person_type, {'phone':'234'})

    contract_id = None
    ##contract_id2 = mtl.create_contract2('opt_prem',{'client_id': agency_id, 'person_id': person_id, 'FINISH_DT': '2015-04-22T00:00:00', 'SERVICES': [70], 'is_signed': '2015-01-01T00:00:00'})
    # ---------- For multicurrency cases: ------------
    if multi:
        if multi == 'RUB':
            mtl.create_client({'CLIENT_ID': client_id, 'REGION_ID': '225', 'CURRENCY': 'RUB', 'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(seconds=5), 'SERVICE_ID': 7, 'CURRENCY_CONVERT_TYPE': 'MODIFY'})
        elif multi == 'UAH':
            mtl.create_client({'CLIENT_ID': client_id, 'REGION_ID': '187', 'CURRENCY': 'UAH', 'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(seconds=5), 'SERVICE_ID': 7, 'CURRENCY_CONVERT_TYPE': 'MODIFY'})
        test_rpc.ExecuteSQL('balance', "update T_EXPORT set priority = -1 where type = 'MIGRATE_TO_CURRENCY' and object_id = :client_id" , {'client_id': client_id })
        while 1==1:
            state = test_rpc.ExecuteSQL('balance', "select state from T_EXPORT where type = 'MIGRATE_TO_CURRENCY' and object_id = :client_id" , {'client_id': client_id })[0]['state']
            print(state)
            time.sleep(3)
            if state == 1: break

    # ---------- Process an 'orders' = ['99-111', '7-1475', '7-1475'] structure ------------
    orders_list = []
    for item in orders:
        service_id = item.split('-')[0]
        product_id = item.split('-')[1]
        service_order_id = mtl.get_next_service_order_id(service_id)
        order_id = mtl.create_or_update_order (order_owner, product_id, service_id, service_order_id,
            {'TEXT':'Py_Test order'}, agency_id = agency_id, manager_uid = manager_uid)
        orders_list.append({'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt})
    # ---------- Generate a target url ------------
    request_id = mtl.create_request (invoice_owner, orders_list, request_dt)
    if mode == 'paypreview':
        print ("{0}https://balance.{1}.yandex.ru/paypreview.xml?person_id={2}&request_id={3}&paysys_id={4}&contract_id={5}&coupon=&mode=ci".format(
            auto_prefix, mtl.host, person_id, request_id, paysys_id,
            contract_id if contract_id else '') + '&mt-login=yb-adm&mt-password=get_secret(*UsersPwd.YANDEX_TEAM_REG_CQR5_PWD)')
    elif mode == 'print_form':
        invoice_id = mtl.create_invoice (request_id, person_id, paysys_id, credit = 0, contract_id = contract_id, overdraft = 0, endbuyer_id = None)
        print ("{0}https://balance-admin.{1}.yandex.ru/invoice-publish.xml?ft=html&object_id={2}".format(
            auto_prefix, mtl.host,
            invoice_id) + '&mt-login=yb-adm&mt-password=get_secret(*UsersPwd.YANDEX_TEAM_REG_CQR5_PWD)')
    elif mode == 'invoice':
        invoice_id = mtl.create_invoice (request_id, person_id, paysys_id, credit = 0, contract_id = contract_id, overdraft = 0, endbuyer_id = None)
        print ("{0}https://balance-admin.{1}.yandex.ru/invoice.xml?invoice_id={2}".format(
            auto_prefix, mtl.host,
            invoice_id) + '&mt-login=yb-adm&mt-password=get_secret(*UsersPwd.YANDEX_TEAM_REG_CQR5_PWD)')

if __name__ == '__main__':
    #------------------------------------------------------------------------------
    # Parametrization for new ofertas:
    # scenario = [paysys_is, <person_type>, invoice_dt]
    # parameters: -paysys_id 1003 -person_type 'ur' -invoice_dt 01-05-2015 00:00:00
    # dt format: "%d.%m.%Y %H:%M:%S"

    #<debug
    argv = sys.argv
    argv.append('-paysys_id'); argv.append(1003)
    argv.append('-person_type'); argv.append('sw_yt')
    argv.append('-invoice_dt'); argv.append(datetime.datetime(2015,6,3,11,0,0))
##    argv.append('-orders'); argv.append(['98-505057', '98-505057'])
    argv.append('-orders'); argv.append(['7-1475'])
##    argv.append('-multi'); argv.append('RUB')
    argv.append('-mode'); argv.append('paypreview')
    #debug>
    #------------------------------------------------------------------------------

    params = sys.argv[1:]
    params_it = iter(params)
    items = zip(*[params_it]*2)
    params_dict = dict(items)

    data_generator (params_dict['-paysys_id'],
                    params_dict['-person_type'],
                    params_dict['-invoice_dt'] if params_dict.has_key('-invoice_dt') else datetime.datetime.now(),
                    params_dict['-orders'] if params_dict.has_key('-orders') else ['7-1475'],
                    params_dict['-multi'] if params_dict.has_key('-multi') else None,
                    params_dict['-mode'] if params_dict.has_key('-mode') else 'paypreview')
    pass
