# -*- coding: utf-8 -*-

import pprint
import time

import MTestlib_cmp as MTestlib
import proxy_provider


def Print(obj):
    print pprint.pformat(obj).decode('unicode_escape')


host = 'greed-ts1f'
tm = proxy_provider.GetServiceProxy(host, 0)
test = proxy_provider.GetServiceProxy(host, 1)


##Клиент
uid = 'clientuid34'
is_agency = False

##service_id = 7;
##product_id = 1475;

qty = 2000
is_credit = 0
overdraft = 0

sql_date_format = "%d.%m.%Y %H:%M:%S"


######### Создание клиента, плательщика, заказа, договора, счета
##def com(paysys_id, person_type, dt, contract_type = None):
##
##    client_id = MTestlib.create_client({'IS_AGENCY': 0})
##    person_id = MTestlib.create_person(client_id, person_type)
##
##    service_order_id = MTestlib.get_next_service_order_id(service_id)
##    order_id = MTestlib.create_or_update_order (client_id, product_id, service_id, service_order_id,  {'TEXT':'Py_Test order'})
##    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': dt}]
##    request_id = MTestlib.create_request (client_id, orders_list, overdraft, dt)
##
##    if contract_type <> None:
##        contract_id = MTestlib.create_contract(client_id, person_id, service_id, contract_type)
##        invoice_id = MTestlib.create_invoice (request_id, person_id, paysys_id, is_credit, overdraft, contract_id)
##    if contract_type == None:
##        contract_id = 'No contract'
##        invoice_id = MTestlib.create_invoice (request_id, person_id, paysys_id, is_credit, overdraft)
##    MTestlib.OEBS_payment(invoice_id, dt, None)
##
##    return client_id, person_id, service_order_id, order_id, contract_id, invoice_id


########### Счет не выгружен
def com_1(paysys_id, person_type, dt, service_id, product_id, contract_type=None):
    tm, test = MTestlib.proxy()

    client_id = MTestlib.create_client({'IS_AGENCY': 0})
    person_id = MTestlib.create_person(client_id, person_type)

    service_order_id = MTestlib.get_next_service_order_id(service_id)
    order_id = MTestlib.create_or_update_order(client_id, product_id, service_id, service_order_id,
                                               {'TEXT': 'Py_Test order'})
    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': dt}]
    request_id = MTestlib.create_request(client_id, orders_list, overdraft, dt)

    if contract_type <> None:
        contract_id = MTestlib.create_contract(client_id, person_id, service_id, contract_type)
        invoice_id = MTestlib.create_invoice(request_id, person_id, paysys_id, is_credit, overdraft, contract_id)
    if contract_type == None:
        contract_id = 'No contract'
        invoice_id = MTestlib.create_invoice(request_id, person_id, paysys_id, is_credit, overdraft)

    test.ExecuteSQL('balance', 
        'update (select  * from t_export where  CLASSNAME = \'Invoice\' and type =\'OEBS\' and OBJECT_ID = :invoice_id ) set state = 1',
        {'invoice_id': invoice_id})
    MTestlib.OEBS_payment(invoice_id, dt, None)

    return client_id, person_id, service_order_id, order_id, contract_id, invoice_id


########### Счет выгружен
def com_2(paysys_id, person_type, dt, service_id, product_id, contract_type=None):
    tm, test = MTestlib.proxy()
    ##    tm = proxy_provider.GetServiceProxy(host, 0)
    ##    test = proxy_provider.GetServiceProxy(host, 1)
    ##    MTestlib.set_service_proxy(tm, 0)
    ##    MTestlib.set_service_proxy(test, 1)

    client_id = MTestlib.create_client({'IS_AGENCY': 0})
    MTestlib.oebs_export('Client', client_id)
    person_id = MTestlib.create_person(client_id, person_type)
    MTestlib.oebs_export('Person', person_id)

    service_order_id = MTestlib.get_next_service_order_id(service_id)
    order_id = MTestlib.create_or_update_order(client_id, product_id, service_id, service_order_id,
                                               {'TEXT': 'Py_Test order'})
    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': dt}]
    request_id = MTestlib.create_request(client_id, orders_list, overdraft, dt)

    if contract_type <> None:
        contract_id = MTestlib.create_contract(client_id, person_id, service_id, contract_type)
        MTestlib.oebs_export('Contract', contract_id)
        invoice_id = MTestlib.create_invoice(request_id, person_id, paysys_id, is_credit, overdraft, contract_id)

    if contract_type == None:
        contract_id = 'No contract'
        invoice_id = MTestlib.create_invoice(request_id, person_id, paysys_id, is_credit, overdraft)

    MTestlib.oebs_export('Invoice', invoice_id)
    MTestlib.OEBS_payment(invoice_id, dt, None)

    return client_id, person_id, service_order_id, order_id, contract_id, invoice_id


########### Акт не выгружен
def com_3(paysys_id, person_type, dt, service_id, product_id, contract_type=None):
    tm, test = MTestlib.proxy()

    client_id, person_id, service_order_id, order_id, contract_id, invoice_id = com_1(paysys_id, person_type, dt,
                                                                                      service_id, product_id,
                                                                                      contract_type)
    test.TestBalance.OldCampaigns(
        {'Bucks': 500, 'stop': '0', 'service_id': service_id, 'service_order_id': service_order_id}, dt)
    ##    MTestlib.do_campaigns(service_id,service_order_id,500, dt)
    test.TestBalance.OldAct(invoice_id, dt)
    ##    MTestlib.create_act(invoice_id, dt)

    act_id = test.ExecuteSQL('balance', 'select id from t_act where INVOICE_ID = :invoice_id', {'invoice_id': invoice_id})[0]['id']
    print 'Act: %d' % (act_id)
    test.ExecuteSQL('balance', 
        'update (select  * from t_export where  CLASSNAME = \'Act\' and OBJECT_ID = :act_id ) set state = 1',
        {'act_id': act_id})

    return client_id, person_id, order_id, contract_id, invoice_id, act_id, service_order_id


########### Акт  выгружен
def com_4(paysys_id, person_type, dt, service_id, product_id, contract_type=None):
    tm, test = MTestlib.proxy()

    client_id, person_id, service_order_id, order_id, contract_id, invoice_id = com_2(paysys_id, person_type, dt,
                                                                                      service_id, product_id,
                                                                                      contract_type)
    test.TestBalance.OldCampaigns(
        {'Bucks': 500, 'stop': '0', 'service_id': service_id, 'service_order_id': service_order_id}, dt)
    test.TestBalance.OldAct(invoice_id, dt)

    act_id = test.ExecuteSQL('balance', 'select id from t_act where INVOICE_ID = :invoice_id', {'invoice_id': invoice_id})[0]['id']
    print 'Act: %d' % (act_id)
    MTestlib.oebs_export('Act', act_id)

    return client_id, person_id, order_id, contract_id, invoice_id, act_id, service_order_id


def start_cmp(cmp_name, host):
    test.ExecuteSQL('balance', 'update (select * from T_PYCRON_LOCK where   name  = :cmp_name) set host = :host',
                    {'cmp_name': cmp_name, 'host': host})
    cmp_id = test.ExecuteSQL('balance', 'select id from T_PYCRON_LOCK where   name  = :cmp_name', {'cmp_name': cmp_name})[0]['id']
    test.ExecuteSQL('balance', 'update (select * from T_PYCRON_SCHEDULE where name = :cmp_name) set host = :host',
                    {'cmp_name': cmp_name, 'host': host})
    test.ExecuteSQL('balance', 'update (select * from T_PYCRON_STATE where id = :cmp_id ) set host = :host, started  = null',
                    {'cmp_id': cmp_id, 'host': host})
    print u'Сверка запущена'
    print u'в процессе...'


###### получаемм id  запуска сверки, когда она отработала
def get_cmp_id(compare_type):
    table = 'cmp.' + str(compare_type) + '_cmp'
    query = 'select id from ' + table + ' where dt = ( select max(dt) as dt from ' + table + ')'
    old_id = test.ExecuteSQL('balance', query)[0]['id']
    new_id = old_id
    while True:

        if old_id == new_id:
            new_id = test.ExecuteSQL('balance', query)[0]['id']
            time.sleep(3)
            print u'в процессе...'
        if old_id <> new_id:
            query_2 = 'select finish_dt from ' + table + ' where id = ' + str(new_id)
            finish_dt = test.ExecuteSQL('balance', query_2)[0]['finish_dt']
            while True:
                if finish_dt is None:
                    time.sleep(3)
                    finish_dt = test.ExecuteSQL('balance', query_2)[0]['finish_dt']
                else:
                    print u'Сверка отработала, cmp_id = ' + str(new_id)
                    return (new_id)
                    break


def check_cmp_type(cmp_type, host_cmp):
    types = {'iob', 'iob_us', 'aob', 'aob_us', 'aob_tr'}
    hosts = {'ts', 'tm'}
    if (cmp_type in types) and (host_cmp in hosts):
        check = 0
    else:
        check = 1
    return check


def pycron_command(cmp_name, code_name, persons=None):
    if persons <> None:
        command = 'update (select * from bo.t_pycron_descr where name  =\'' + str(
            cmp_name) + '\') set command = \'/usr/lib/pymodules/python2.7/dcs/bin/' + str(
            code_name) + ' --nomnclose --user 1120000000017493 --objects ' + str(persons) + '\''
    elif persons == None:
        command = 'update (select * from bo.t_pycron_descr where name  =\'' + str(
            cmp_name) + '\') set command = \'/usr/lib/pymodules/python2.7/dcs/bin/' + str(
            code_name) + ' --nomnclose --user 1120000000017493 ' + '\''
    test.ExecuteSQL('balance', str(command))


def check(compare_type, external_id, cmp_id, type):
    tm, test = MTestlib.proxy()
    t_bill = 'select count (*) as count from cmp.' + str(compare_type) + '_bill where Eid = ' + str(external_id)
    t_oebs = 'select count (*) as count from cmp.' + str(compare_type) + '_oebs where Eid = ' + str(external_id)
    t_cmp_data = 'select count (*) as count from cmp.' + str(compare_type) + '_cmp_data where Eid = ' + str(
        external_id) + ' and cmp_id = ' + str(cmp_id)
    in_bill = test.ExecuteSQL('balance', t_bill)[0]['count']
    in_oebs = test.ExecuteSQL('balance', t_oebs)[0]['count']
    in_cmp_data = test.ExecuteSQL('balance', t_cmp_data)[0]['count']
    passed = 0
    ##    print in_bill, in_oebs, in_cmp_data
    if type in [1, 2, 3, 4, 5] and ((in_bill > 1 or in_oebs > 1) or (in_bill == 0 and in_oebs == 0)):
        passed = 'FAILED'
    elif type == 0 and ((in_cmp_data <> 0) or (in_bill == 0 or in_oebs == 0)):
        passed = 'FAILED'
    else:
        passed = 'PASSED'
    if passed == 'PASSED':
        t_data = 'select state, sum_bill, sum_oebs, dt_bill,dt_oebs, pid_bill, pid_oebs from cmp.' + str(
            compare_type) + '_cmp_data where Eid = ' + str(external_id) + 'and cmp_id= ' + str(cmp_id)
        data = test.ExecuteSQL('balance', t_data)
        state = data[0]['state']
        sum_bill = data[0]['sum_bill']
        sum_oebs = data[0]['sum_oebs']
        dt_bill = data[0]['dt_bill']
        dt_oebs = data[0]['dt_oebs']
        pid_bill = data[0]['pid_bill']
        pid_oebs = data[0]['pid_oebs']
        ############## 1, 'Отсутствует в OEBS'
        if type == 1:
            if state == 1 and sum_oebs == None and sum_bill <> None and dt_bill <> None and dt_oebs == None and pid_bill == None and pid_oebs == None:
                passed = 'passed'
            else:
                passed = 'failed'
                ############## 2, 'Отсутствует в Биллинге'
        if type == 2:
            if state == 2 and sum_oebs <> None and sum_bill == None and dt_bill == None and dt_oebs <> None and pid_bill == None and pid_oebs == None:
                passed = 'passed'
            else:
                passed = 'failed'
                ############## 3, 'Расходятся суммы акта'
        if type == 3:
            if state == 3 and sum_oebs <> None and sum_bill <> None and sum_oebs <> sum_bill and dt_bill == None and dt_oebs == None and pid_bill == None and pid_oebs == None:
                passed = 'passed'
            else:
                passed = 'failed'
                ############## 4, 'Расходятся даты акта'
        if type == 4:
            if state == 4 and sum_oebs == None and sum_bill == None and dt_bill <> None and dt_oebs <> None and dt_bill <> dt_oebs and pid_bill == None and pid_oebs == None:
                passed = 'passed'
            else:
                passed = 'failed'
                ############## 5, 'Расходятся плательщики'
        if type == 5:
            if state == 5 and sum_oebs == None and sum_bill == None and dt_bill == None and dt_oebs == None and pid_bill <> None and pid_oebs <> None and pid_bill <> pid_oebs:
                passed = 'passed'
            else:
                passed = 'failed'

    return passed


def insert_into_t_act_check(act_id):
    tm, test = MTestlib.proxy()
    select = 'select external_id from t_act where id = ' + str(act_id)
    external_id = test.ExecuteSQL('balance', select)[0]['external_id']
    select_2 = 'INSERT INTO BO.XXXX_T_ACT_CHECK (trx_number) VALUES (' + str(external_id) + ')'
    test.ExecuteSQL('balance', select_2)


def base_check(compare_type, dt):
    dt_bill = 'select min(dt) as dt from cmp.' + str(compare_type) + '_bill'
    dt_oebs = 'select min(dt) as dt  from cmp.' + str(compare_type) + '_oebs'
    bill = test.ExecuteSQL('balance', dt_bill)[0]['dt']
    oebs = test.ExecuteSQL('balance', dt_oebs)[0]['dt']
    if bill >= dt and oebs >= dt:
        results = 'passed'
    else:
        results = 'failed'
    print results
    return results
