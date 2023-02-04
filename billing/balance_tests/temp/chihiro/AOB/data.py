# coding=utf-8
import datetime
import time

import balance.balance_db as db
from actbase import input
from balance import balance_steps as steps

SERVICE_ID_DIRECT = 7
PRODUCT_ID_DIRECT = 1475

SERVICE_ID_MARKET = 11
PRODUCT_ID_MARKET = 2136

PAYSYS_ID = 1050
QTY = 55.7

BASE_DT=datetime.datetime.now().replace(day=1)-datetime.timedelta(days=1)

# def get_date():
#     date = datetime.datetime.now()
#     month = date.month
#     if (month == 3):
#         BASE_DT = datetime.datetime(date.year, 2, 28)
#     else:
#         if (month == 1):
#             BASE_DT = datetime.datetime(date.year, 12, 31)
#         else:
#             if ((month % 2 == 1) and (month < 8)):
#                 BASE_DT = datetime.datetime(date.year, month - 1, 30)
#             if (month % 2 == 0 and month < 8):
#                 BASE_DT = datetime.datetime(date.year, month - 1, 31)
#             if (month == 8):
#                 BASE_DT = datetime.datetime(date.year, month - 1, 31)
#             else:
#                 if (month % 2 == 1 and month > 7):
#                     BASE_DT = datetime.datetime(date.year, month - 1, 31)
#                 if (month % 2 == 0 and month > 7):
#                     BASE_DT = datetime.datetime(date.year, month - 1, 30)
#     return BASE_DT

def get_cmp_id(compare_type):
    table = 'cmp.' + str(compare_type[compare_type.find('aob'):]) + '_cmp'
    query = 'select id from ' + table + ' where dt = ( select max(dt) as dt from ' + table + ')'
    old_id = db.balance().execute(query)[0]['id']
    new_id = old_id
    while True:

        if old_id == new_id:
            new_id = db.balance().execute(query)[0]['id']
            time.sleep(3)
            print u'в процессе...'
        if old_id <> new_id:
            query_2 = 'select finish_dt from ' + table + ' where id = ' + str(new_id)
            finish_dt = db.balance().execute(query_2)[0]['finish_dt']
            while True:
                if finish_dt is None:
                    time.sleep(3)
                    finish_dt = db.balance().execute(query_2)[0]['finish_dt']
                else:
                    print u'Сверка отработала, cmp_id = ' + str(new_id)
                    return (new_id)
                    break

def pycron_command(cmp_name, code_name, persons=None):
    if persons <> None:
        command = 'update (select * from bo.t_pycron_descr where name  =\'' + str(
            cmp_name) + '\') set command = \'/usr/lib/pymodules/python2.7/dcs/bin/' + str(
            code_name) + ' --user 1120000000017493 --objects ' + str(persons) + '\''
    elif persons == None:
        command = 'update (select * from bo.t_pycron_descr where name  =\'' + str(
            cmp_name) + '\') set command = \'/usr/lib/pymodules/python2.7/dcs/bin/' + str(
            code_name) + ' --nomnclose --user 1120000000017493 ' + '\''
    db.balance().execute(str(command))

def oebs_export(export_type, object_id):
    db.balance().execute(
        'update (select  * from t_export where  CLASSNAME = :export_type and   type=\'OEBS\' and OBJECT_ID = :object_id ) set priority = -1',
        {'export_type': export_type, 'object_id': object_id})
    state = 0
    print str(export_type) + ' export begin:' + str(datetime.datetime.now())
    while True:
        if state == 0:
            state = db.balance().execute(
                'select  state from t_export where  CLASSNAME = :export_type and   type=\'OEBS\' and OBJECT_ID = :object_id ',
                {'export_type': export_type, 'object_id': object_id})[0]['state']
            print('...(3)');
            time.sleep(3)
        else:
            print str(export_type) + ' export   end:' + str(datetime.datetime.now())
            break


def start_cmp(cmp_name, host):
    cmp_id = db.balance().execute('select id from T_PYCRON_LOCK where   name  = :cmp_name', {'cmp_name': cmp_name})[0][
        'id']
    db.balance().execute('update (select * from T_PYCRON_SCHEDULE where name = :cmp_name) set host = :host',
              {'cmp_name': cmp_name, 'host': host})
    db.balance().execute('update (select * from T_PYCRON_STATE where id = :cmp_id ) set host = :host, started  = null',
                         {'cmp_id': cmp_id, 'host': host})
    print u'Сверка запущена'
    print u'в процессе...'


# Турция
# Создание клиента, заказа предоплатного счета
# сумма актов совпадает
# положительный тест
def test_0():
    # BASE_DT = get_date()
    client_id = None or steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'tru')
    steps.ClientSteps.link(client_id, 'chihiro-test-0')

    contract_id = None

    service_id = SERVICE_ID_DIRECT
    product_id = PRODUCT_ID_DIRECT

    service_order_id = steps.OrderSteps.next_id(service_id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=service_id, product_id=product_id)

    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}]
    request_id = steps.RequestSteps.create(client_id, orders_list, invoice_dt=BASE_DT)

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id, payment_dt=BASE_DT)

    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 23, 'Money': 0}, 0, BASE_DT)
    steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)
    act_id = db.balance().execute("select ID from t_act where client_id=:client_id", {'client_id': client_id})
    act_id1 = act_id[0]['id']
    print ('Act_id: ', act_id1)
    oebs_export('Act', act_id1)
    # start_cmp('dcs_aob_tr', 'greed-tm1f')
    # steps.CommonSteps.wait_for(
    #     'select state as val from t_export where object_id = :act_id and type  = \'OEBS\' and classname = \'Act\'',
    #     {'act_id': act_id}, 1)
    input(act_id1)

# акт не выгружен в oebs
def test_1():
    # BASE_DT = get_date()
    print ('Date: ', BASE_DT)
    client_id = None or steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'tru')
    steps.ClientSteps.link(client_id, 'chihiro-test-0')

    contract_id = None

    service_id = SERVICE_ID_DIRECT
    product_id = PRODUCT_ID_DIRECT

    service_order_id = steps.OrderSteps.next_id(service_id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=service_id, product_id=product_id)

    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}]
    request_id = steps.RequestSteps.create(client_id, orders_list, invoice_dt=BASE_DT)

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id, payment_dt=BASE_DT)

    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 21, 'Money': 0}, 0, BASE_DT)
    steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)
    act_id = db.balance().execute("select ID from t_act where client_id=:client_id", {'client_id': client_id})
    act_id1 = act_id[0]['id']
    print ('Act_id: ', act_id1)
    db.balance().execute(
        'update (select  * from t_export where  CLASSNAME = :export_type and   type=\'OEBS\' and OBJECT_ID = :object_id ) set export_dt=NULL, next_export=NULL',
        {'export_type': 'Act', 'object_id': act_id1})
    # start_cmp('dcs_aob_tr', 'greed-tm1f')
    # steps.CommonSteps.wait_for(
    #     'select state as val from t_export where object_id = :act_id and type  = \'OEBS\' and classname = \'Act\'',
    #     {'act_id': act_id}, 1)
    input(act_id1)


# акт удален в биллинге
def test_2():
    # BASE_DT = get_date()
    print ('Date: ', BASE_DT)
    client_id = None or steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'tru')
    steps.ClientSteps.link(client_id, 'chihiro-test-0')

    contract_id = None

    service_id = SERVICE_ID_DIRECT
    product_id = PRODUCT_ID_DIRECT

    service_order_id = steps.OrderSteps.next_id(service_id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=service_id, product_id=product_id)

    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}]
    request_id = steps.RequestSteps.create(client_id, orders_list, invoice_dt=BASE_DT)

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id, payment_dt=BASE_DT)

    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 21, 'Money': 0}, 0, BASE_DT)
    steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)
    act_id = db.balance().execute("select ID from t_act where client_id=:client_id", {'client_id': client_id})
    act_id1 = act_id[0]['id']
    print ('Act_id: ', act_id1)
    oebs_export('Act', act_id1)
    # db.balance().execute('delete from t_act where  id = :id', {'id': act_id1})
    db.balance().execute('update (select  * from t_act where  id = :id) set hidden=4', {'id': act_id1})
    # start_cmp('dcs_aob_tr', 'greed-tm1f')
    # steps.CommonSteps.wait_for(
    #     'select state as val from t_export where object_id = :act_id and type  = \'OEBS\' and classname = \'Act\'',
    #     {'act_id': act_id}, 1)
    input(act_id1)


# различная сумма актов
def test_3():
    # BASE_DT = get_date()
    print ('Date: ', BASE_DT)
    client_id = None or steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'tru')
    steps.ClientSteps.link(client_id, 'chihiro-test-0')

    contract_id = None

    service_id = SERVICE_ID_DIRECT
    product_id = PRODUCT_ID_DIRECT

    service_order_id = steps.OrderSteps.next_id(service_id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=service_id, product_id=product_id)

    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}]
    request_id = steps.RequestSteps.create(client_id, orders_list, invoice_dt=BASE_DT)

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id, payment_dt=BASE_DT)

    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 21, 'Money': 0}, 0, BASE_DT)
    steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)
    act_id = db.balance().execute("select ID from t_act where client_id=:client_id", {'client_id': client_id})
    act_id1 = act_id[0]['id']
    print ('Act_id: ', act_id1)
    oebs_export('Act', act_id1)
    db.balance().execute('update (select  * from t_act where  id = :id) set amount = 113', {'id': act_id1})
    # start_cmp('dcs_aob_tr', 'greed-tm1f')
    # steps.CommonSteps.wait_for(
    #     'select state as val from t_export where object_id = :act_id and type  = \'OEBS\' and classname = \'Act\'',
    #     {'act_id': act_id}, 1)
    input(act_id1)



# различная дата актов
def test_4():
    # BASE_DT = get_date()
    print ('Date: ', BASE_DT)
    client_id = None or steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'tru')
    steps.ClientSteps.link(client_id, 'chihiro-test-0')

    contract_id = None

    service_id = SERVICE_ID_DIRECT
    product_id = PRODUCT_ID_DIRECT

    service_order_id = steps.OrderSteps.next_id(service_id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=service_id, product_id=product_id)

    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}]
    request_id = steps.RequestSteps.create(client_id, orders_list, invoice_dt=BASE_DT)

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id, payment_dt=BASE_DT)

    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 21, 'Money': 0}, 0, BASE_DT)
    steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)
    act_id = db.balance().execute("select ID from t_act where client_id=:client_id", {'client_id': client_id})
    act_id1 = act_id[0]['id']
    print ('Act_id: ', act_id1)
    oebs_export('Act', act_id1)
    # date2=datetime.datetime(BASE_DT - datetime.timedelta(days=3))
    date2 = BASE_DT+datetime.timedelta(minutes=3)
    db.balance().execute('update (select  * from t_act where id = :id) set dt =:date2',
                         {'id': act_id1, 'date2': date2})
    # start_cmp('dcs_aob_tr', 'greed-tm1f')
    # steps.CommonSteps.wait_for(
    #     'select state as val from t_export where object_id = :act_id and type  = \'OEBS\' and classname = \'Act\'',
    #     {'act_id': act_id}, 1)
    input(act_id1)


# различные плательщики
def test_5():
    # BASE_DT = get_date()
    print ('Date: ', BASE_DT)
    client_id = None or steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'tru')
    person_id2 = steps.PersonSteps.create(client_id, 'tru')
    steps.ClientSteps.link(client_id, 'chihiro-test-0')

    contract_id = None

    service_id = SERVICE_ID_DIRECT
    product_id = PRODUCT_ID_DIRECT

    service_order_id = steps.OrderSteps.next_id(service_id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=service_id, product_id=product_id)

    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}]
    request_id = steps.RequestSteps.create(client_id, orders_list, invoice_dt=BASE_DT)

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id, payment_dt=BASE_DT)

    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 21, 'Money': 0}, 0, BASE_DT)
    steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)
    act_id = db.balance().execute("select ID from t_act where client_id=:client_id", {'client_id': client_id})
    act_id1 = act_id[0]['id']
    print ('Act_id: ', act_id1)
    oebs_export('Act', act_id1)
    db.balance().execute('update (select  * from t_invoice where  id = :invoice_id) set person_id = :person_id2',
                         {'invoice_id': invoice_id, 'person_id2': person_id2})
    # start_cmp('dcs_aob_tr', 'greed-tm1f')
    # steps.CommonSteps.wait_for(
    #     'select state as val from t_export where object_id = :act_id and type  = \'OEBS\' and classname = \'Act\'',
    #     {'act_id': act_id}, 1)
    input(act_id1)

def Prepare():
    test_0()
    test_1()
    test_2()
    test_3()
    test_4()
    test_5()
    pass

if __name__ == "__main__":
    test_1()
    # Prepare()
    # start_cmp('dcs_aob_tr', 'greed-tm1f')
    # get_date()
