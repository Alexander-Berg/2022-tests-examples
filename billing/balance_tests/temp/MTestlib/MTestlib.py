# -*- coding: utf-8 -*-

import calendar
import datetime
import math
import pickle
import pprint
import re
import time
import urlparse
from collections import defaultdict

import common_data
import proxy_provider


def log(f):
    def cut(s):
        CUT_LIMIT = 1500
        return s[:CUT_LIMIT - 3] + u'...' if len(s) > CUT_LIMIT else s

    def smart_repr(obj):
        return pprint.pformat(obj).decode('unicode_escape')

    def new_logging_func(*args, **kwargs):
        argformat = []
        import xmlrpclib
        if isinstance(f, xmlrpclib._Method):
            formatparams = [f._Method__name]
        else:
            formatparams = [f.func_name]
        if args:
            argformat.append(u'%s')
            formatparams.append(', '.join([smart_repr(arg) for arg in args]))
        if kwargs:
            argformat.append(u'**kwargs = %s')
            formatparams.append(smart_repr(kwargs))
        format = u'Вызов: %s(' + u', '.join(argformat) + ')'
        # print cut(format%tuple(formatparams))
        result = f(*args, **kwargs)
        # print cut(u'Ответ: %s'%smart_repr(result))
        # print
        return result

    return new_logging_func


def Print(obj):
    print pprint.pformat(obj).decode('unicode_escape')


host = 'greed-tm1f'
# host = 'greed-dev1e'
# host = 'greed-ts1f'
# host = 'greed-load2e'
##host = 'greed-pt1f'
# host = 'greed-pt1g'
# host = ('greed-dev4f', 'ashvedunov')

rpc = proxy_provider.GetServiceProxy(host, 0)
test_rpc = proxy_provider.GetServiceProxy(host, 1)
http_api = proxy_provider.GetServiceProxy(host, 2)

# For data format convertation
sql_date_format = "%d.%m.%Y %H:%M:%S"
log_align = 30
passport_uid = 16571028

# For integration with Java
f = lambda: defaultdict(f)
objects = defaultdict(dict)


def create_client(client_params):
    # Default params
    client_params_defaults = {
        'EMAIL': 'client@in-fo.ru',
        'FAX': '5408410',
        'IS_AGENCY': 0,
        'NAME': u'Test',
        'PHONE': '911',
        'URL': 'www.qwerty.ru'
        ##        'IS_NON_RESIDENT': 0,
        ##        'FULLNAME': None,
        ##        'CURRENCY_PAYMENT': None
    }
    params = client_params_defaults.copy()
    # Received params update defaults
    if client_params:
        params.update(client_params)

    code, status, client_id = rpc.Balance.CreateClient(passport_uid, params)

    print ('{0:<' + str(log_align) + '} | {1}').format(
        '%s: %d' % ('Agency_id' if params['IS_AGENCY'] == 1 else 'Client_id', client_id), client_params)

    if not objects['clients'].has_key(client_id):
        objects['clients'][client_id] = dict()
    objects['clients'][client_id]['passport_uid'] = passport_uid
    objects['clients'][client_id].update(params)

    return client_id


def link_client_uid(client_id, uid):
    id_uid = test_rpc.ExecuteSQL('balance', 'select PASSPORT_ID from T_ACCOUNT where LOGIN = :uid', {'uid': uid})[0]['passport_id']
    test_rpc.ExecuteSQL('balance', 'update (select * from T_ACCOUNT where PASSPORT_ID = :id_uid ) set client_id = :client_id',
                        {'id_uid': id_uid, 'client_id': client_id})

    if not objects['links'].has_key(client_id):
        objects['links'][client_id] = []
    data = dict()
    data['uid'] = uid
    objects['links'][client_id].append(data)

    print 'Link: %s -> %s' % (uid, client_id)


def get_next_service_order_id(service_id):
    if service_id == 7:
        seq_name = 'S_TEST_SERVICE_ORDER_ID_7'
    elif service_id == 116:
        seq_name = 'S_TEST_SERVICE_ORDER_ID_116'
    else:
        seq_name = 'S_TEST_SERVICE_ORDER_ID'

    service_order_id = test_rpc.ExecuteSQL('balance', 'select ' + seq_name + '.nextval from dual')[0]['nextval']
    ##    service_order_id = test_rpc.ExecuteSQL('balance', 'select :sequence'+'.nextval from dual', {'sequence': seq_name})
    ##    service_order_id = test_rpc.ExecuteSQL('balance', 'select :sequence from dual', {'sequence': '{0}.nextval'.format(seq_name)})[0]['nextval']
    print ("{0:<" + str(log_align) + "} | {1}").format('Service_order_id: %d' % (service_order_id),
                                                       "{'ServiceID': %s}" % (service_id))
    return service_order_id


def merge_order(parent_order, sub_orders, group_without_transfer=True, passport_uid=passport_uid):
    parent_service_order_id = \
        test_rpc.ExecuteSQL('balance', 'select service_order_id from t_order where id =:id', {'id': parent_order})[0][
            'service_order_id']
    request_params = []
    for sub_order in sub_orders:
        sub_service_order_id = test_rpc.ExecuteSQL('balance', 
            'select service_id, service_order_id, service_code, client_id, agency_id from t_order where id =:id',
            {'id': sub_order})[0]
        sub_service_order_id['GroupServiceOrderID'] = parent_service_order_id
        sub_service_order_id['GroupWithoutTransfer'] = group_without_transfer
        sub_service_order_id['ProductID'] = sub_service_order_id.pop('service_code')
        sub_service_order_id['AgencyID'] = sub_service_order_id.pop('agency_id')
        sub_service_order_id['ServiceOrderID'] = sub_service_order_id.pop('service_order_id')
        sub_service_order_id['ClientID'] = sub_service_order_id.pop('client_id')
        sub_service_order_id['ServiceID'] = sub_service_order_id.pop('service_id')
        request_params.append(sub_service_order_id)
    rpc.Balance2.CreateOrUpdateOrdersBatch(passport_uid, request_params)
    if group_without_transfer:
        print ('GroupOrderID:{0}, sub_orders: {1} (not transferred)').format(parent_order, sub_orders)
    else:
        test_rpc.UATransferQueue([sub_service_order_id['ClientID']])
        sql = "select state as val from T_EXPORT where type = 'UA_TRANSFER' and object_id = :client_id"
        sql_params = {'client_id': sub_service_order_id['ClientID']}
        wait_for(sql, sql_params, value=1)
        print ('GroupOrderID:{0}, sub_orders: {1} (transferred)').format(parent_order, sub_orders)
    return parent_service_order_id


def create_or_update_order(client_id, product_id, service_id, service_order_id, params=None, agency_id=None,
                           manager_uid=None, region_id=None, discard_agency_discount=None):
    # Параметры "по-умолчанию":
    order_params_defaults = {
        'TEXT': 'Py_Test order',
        'GroupServiceOrderID': -1,
        'GroupWithoutTransfer': 1,
    }
    # Собираем обязательные переданные пользователем параметры в словарь:
    main_params = {
        'ClientID': client_id,
        'ProductID': product_id,
        'ServiceID': service_id,
        'ServiceOrderID': service_order_id
    }
    ## Дополняем словарь необязательными параметрами, если они переданы
    if agency_id: main_params.update({'AgencyID': agency_id})
    if manager_uid: main_params.update({'ManagerUID': manager_uid})
    if region_id: main_params.update({'RegionID': region_id})
    if discard_agency_discount: main_params.update({'discard_agency_discount': discard_agency_discount})
    ## Последовательно применяем параметры:
    ## order_params <- по-умолчанию <- переданные <- переданные обязательные
    order_params = {}
    order_params.update(order_params_defaults)
    order_params.update(params)
    order_params.update(main_params)

    answer = rpc.Balance.CreateOrUpdateOrdersBatch(passport_uid, [order_params])
    if answer[0][0] == 0:
        order_id = \
            test_rpc.ExecuteSQL('balance', 'select max(id) from T_ORDER where CLIENT_ID = :client_id', {'client_id': client_id})[
                0][
                'MAX(ID)']
        order_url = "https://balance-admin.greed-tm1f.yandex.ru/order.xml?order_id={0}".format(order_id)
        print  ('{0:<' + str(log_align) + '} | {1}, {2}, {3}').format('Order_id: %d' % (order_id), order_url,
                                                                      main_params, params)

    else:
        raise Exception(answer[0][1])

    if not objects['orders'].has_key(order_id):
        objects['orders'][order_id] = dict()
    objects['orders'][order_id]['passport_uid'] = passport_uid
    objects['orders'][order_id].update(order_params)

    return order_id


def create_request(client_id, orders_list, invoice_dt=None, additional_params={}):
    ##req = rpc.Balance.CreateRequest(passport_uid, client_id, [{'Qty': qty, 'ServiceID': service_id,
    ##                                        'ServiceOrderID': service_order_id, 'BeginDT':requestDate}],{})

    req = rpc.Balance.CreateRequest(passport_uid, client_id, orders_list, additional_params)
    request_id = \
        test_rpc.ExecuteSQL('balance', 'select max(id) from T_REQUEST where CLIENT_ID = :client_id', {'client_id': client_id})[0][
            'MAX(ID)']
    if invoice_dt:
        test_rpc.ExecuteSQL('balance', 
            'update (select * from T_REQUEST where ID = :request_id ) set INVOICE_DT = to_date(:invoice_dt,\'DD.MM.YYYY HH24:MI:SS\')',
            {'request_id': request_id, 'invoice_dt': invoice_dt.strftime(sql_date_format)})
    print ('{0:<' + str(log_align) + '} | {1}, {2}, {3}, {4}').format("Request_id: %s" % (request_id), req[3],
                                                                      "{'Owner': %s}" % client_id, orders_list,
                                                                      additional_params)
    req_url = 'http://balance.%s.yandex.ru/paypreview.xml?request_id=%s' % (host, str(request_id))
    print ('{0:<' + str(log_align) + '} | CI {1}').format("Request_id: %s" % (request_id), req_url)
    objects['requests'][request_id] = dict()
    objects['requests'][request_id]['passport_uid'] = passport_uid
    objects['requests'][request_id]['client_id'] = client_id
    objects['requests'][request_id]['orders_list'] = orders_list
    objects['requests'][request_id].update(additional_params)

    return request_id


def create_invoice(request_id, person_id, paysys_id, credit=0, contract_id=None, overdraft=0, endbuyer_id=None):
    hash_dict = {'PaysysID': paysys_id, 'PersonID': person_id, 'RequestID': request_id}
    if contract_id:
        hash_dict['Credit'] = credit
        hash_dict['ContractID'] = contract_id
    if overdraft == 1:
        hash_dict['Overdraft'] = 1
    invoice_id = log(rpc.Balance.CreateInvoice)(passport_uid, hash_dict)
    if endbuyer_id:
        test_rpc.ExecuteSQL('balance', 
            'Insert into T_EXTPROPS (ID,OBJECT_ID,CLASSNAME,ATTRNAME,KEY,VALUE_STR,VALUE_NUM,VALUE_DT,VALUE_CLOB,UPDATE_DT,PASSPORT_ID) values (s_extprops.nextval,:invoice_id,\'Invoice\',\'endbuyer_id\',null,null,:endbuyer_id,null, EMPTY_CLOB(),sysdate,16571028)',
            {'invoice_id': invoice_id, 'endbuyer_id': endbuyer_id})
        test_rpc.ExecuteSQL('balance', 'commit')
    query = "select external_id, total_sum, consume_sum from t_invoice where id = :invoice_id"
    external_id = test_rpc.ExecuteSQL('balance', query, {'invoice_id': invoice_id})[0]['external_id']
    total_sum = test_rpc.ExecuteSQL('balance', query, {'invoice_id': invoice_id})[0]['total_sum']
    invoice_url = 'https://balance-admin.%s.yandex.ru/invoice.xml?invoice_id=%s' % (host, str(invoice_id))
    print ('{0:<' + str(log_align) + '} | {1}, {2}, {3}').format("Invoice_id: %s" % (str(invoice_id)), invoice_url,
                                                                 hash_dict, "{'EndbuyerID': %s}" % str(endbuyer_id))
    ##    print 'Invoice: https://balance-admin.greed-tm1f.yandex.ru/invoice.xml?invoice_id=' + str(invoice_id)

    objects['invoices'][invoice_id] = dict()
    objects['invoices'][invoice_id]['passport_uid'] = passport_uid
    objects['invoices'][invoice_id].update(hash_dict)
    objects['invoices'][invoice_id]['external_id'] = external_id
    objects['invoices'][invoice_id]['total_sum'] = total_sum
    objects['invoices'][invoice_id]['invoice_url'] = invoice_url

    return invoice_id


def create_force_invoice(client_id, person_id, campaigns_list, paysys_id,
                         invoice_dt=None, agency_id=None, credit=0, contract_id=None,
                         overdraft=0, manager_uid=None, endbuyer_id=None):
    order_owner = client_id
    invoice_owner = client_id
    if agency_id:
        invoice_owner = agency_id
    service_order_id = []
    order_id = []
    orders_list = []
    print '------ Orders list: ------'
    ## Проходим цикл столько раз, сколько словарей-заказов передано в campaigns_list
    for i in [x for x in xrange(len(campaigns_list))]:
        ## Сперва создаём заказы
        service_order_id.append(get_next_service_order_id(campaigns_list[i]['service_id']))
        order_id.append(
            create_or_update_order(campaigns_list[i]['client_id'] if 'client_id' in campaigns_list[i] else order_owner,
                                   product_id=campaigns_list[i]['product_id'],
                                   service_id=campaigns_list[i]['service_id'],
                                   service_order_id=service_order_id[i],
                                   params={'TEXT': 'Py_order_for_full_invoice'},
                                   agency_id=agency_id,
                                   manager_uid=manager_uid
                                   )
        )
        ## Затем добавляем их в список для реквеста
        orders_list.append({'ServiceID': campaigns_list[i]['service_id'],
                            'ServiceOrderID': service_order_id[i],
                            'Qty': campaigns_list[i]['qty'],
                            'BeginDT': campaigns_list[i]['begin_dt']}
                           )
    request_id = create_request(invoice_owner, orders_list, invoice_dt)
    ##    if contract_id:
    ##        invoice_id = create_invoice (request_id, person_id, paysys_id, credit=credit,
    ##                     contract_id=contract_id, overdraft=overdraft, endbuyer_id=endbuyer_id)
    ##    else: invoice_id = create_invoice (request_id, person_id, paysys_id)
    invoice_id = create_invoice(request_id, person_id, paysys_id, credit=credit,
                                contract_id=contract_id, overdraft=overdraft, endbuyer_id=endbuyer_id)
    ## Добавляем в order_list продукт, чтобы вернуть из метода полную информацию о результате выполнения
    for i in [x for x in xrange(len(campaigns_list))]:
        orders_list[i].update({'ProductID': campaigns_list[i]['product_id']})
    ##    print ('{0:<'+str(log_align)+'} | ...').format('create_force_invoice done')
    return invoice_id, orders_list


def OEBS_payment(invoice_id, payment_sum=None, payment_date=None):
    query = "select external_id, total_sum, consume_sum from t_invoice where id = :invoice_id"
    external_id = log(test_rpc.ExecuteSQL)('balance', query, {'invoice_id': invoice_id})[0]['external_id']
    total_sum = test_rpc.ExecuteSQL('balance', query, {'invoice_id': invoice_id})[0]['total_sum']
    ## Если передана сумма, то оплата на неё, если нет, то на полную сумму счёта
    if payment_sum:
        p_sum = payment_sum
    else:
        p_sum = total_sum
    ## Если передана дата, то оплата этой датой, если нет, то sysdate
    if payment_date:
        p_date = payment_date
    else:
        p_date = datetime.datetime.today()
    log(test_rpc.ExecuteSQL)('balance', 
        'insert into t_correction_payment (dt, doc_date, sum, memo, invoice_eid) values (to_date(:paymentDate,' +
        '\'DD.MM.YYYY HH24:MI:SS\'),to_date(:paymentDate,\'DD.MM.YYYY HH24:MI:SS\'),:total_sum,\'Testing\',:external_id)',
        {'paymentDate': p_date.strftime(sql_date_format), 'total_sum': p_sum, 'external_id': external_id})
    test_rpc.TestBalance.OEBSPayment(invoice_id)
    # print  ('{0:<'+str(log_align)+'} | {1}').format('OEBS_payment: %s <- %s' % (str(invoice_id), str(p_sum)), "{'PaymentDT': %s}" % str(p_date))
    ##    print 'Invoice %s (%d) received payment %s' % (external_id, invoice_id, str(p_sum))

    if not objects['payments'].has_key(invoice_id):
        objects['payments'][invoice_id] = []
    data = dict()
    data['payment_sum'] = p_sum
    data['payment_date'] = p_date
    objects['payments'][invoice_id].append(data)

    return 1


def do_campaigns(service_id, service_order_id, campaigns_params, do_stop=0, campaigns_dt=None):
    ## Формируем словарик параметров "по-умолчанию" со значениями 0
    campaigns_defaults = dict.fromkeys(['Bucks', 'Shows', 'Clicks', 'Units', 'Days', 'Money'], 0)
    ## params <- параметры "по-умолчанию" <- переданные обязательные параметры
    params = campaigns_defaults.copy()
    params.update(campaigns_params)
    params.update({'service_id': service_id, 'service_order_id': service_order_id, 'do_stop': do_stop})
    ## Если передана дата, то открутки этой датой, если нет, то sysdate
    if campaigns_dt:
        test_rpc.TestBalance.OldCampaigns(params, campaigns_dt)
    else:
        test_rpc.TestBalance.Campaigns(params)
    print  ('{0:<' + str(log_align) + '} | {1}, {2}').format('do_campaigns: done', params,
                                                             "{'CampaignsDT': %s}" % str(campaigns_dt))
    ##    print '%d-%d: completed %d Bucks, %d Money' % (service_id, service_order_id, params['Bucks'], params['Money'])

    order_eid = '{0}-{1}'.format(service_id, service_order_id)
    if not objects['campaigns'].has_key(order_eid):
        objects['campaigns'][order_eid] = []
    data = dict()
    data.update(params)
    data['campaigns_dt'] = campaigns_dt
    objects['campaigns'][order_eid].append(data)

    return 1


def create_act(invoice_id, act_dt=None):
    if act_dt:
        act_id = test_rpc.TestBalance.OldAct(invoice_id, act_dt)
    else:
        act_id = test_rpc.TestBalance.Act(invoice_id)
    print  ('{0:<' + str(log_align) + '} | {1}, {2}').format('create_act: done', "{'InvoiceID': %s}" % str(invoice_id),
                                                             "{'ActDT': %s}" % str(act_dt))

    if not objects['acts'].has_key(invoice_id):
        objects['acts'][invoice_id] = []
    data = dict()
    data['act_id'] = act_id
    data['act_dt'] = act_dt
    objects['acts'][invoice_id].append(data)


##    print 'Act_id: %d' % act_id[0]

def act_accounter(client_id, force, date):
    ## В тестовом серванте метод ActAccounter вызывает внутренний метод act_accounter
    ## Правильно называть так, так как CUtAgava только частный случай генерации актов
    test_rpc.TestBalance.ActAccounter(client_id, force, date)
    print  ('{0:<' + str(log_align) + '} | {1}, {2}, {3}').format('act_accounter: done',
                                                                  "{'ClientID': %s}" % str(client_id),
                                                                  "{'Force': %s}" % str(force),
                                                                  "{'DT': %s}" % str(date))

    if not objects['actions'].has_key('act_accounter'):
        objects['actions']['act_accounter'] = []
    data = dict()
    data['client_id'] = client_id
    data['force'] = force
    data['date'] = date
    objects['actions']['act_accounter'].append(data)


def act_enqueuer(clients_list, dt, force):
    test_rpc.TestBalance.ActEnqueuer(clients_list, dt, force)
    return 1


##    print 'Acts generated'

def merge_clients(master_client_id, slave_client_id):
    test_rpc.TestBalance.MergeClients(passport_uid, master_client_id, slave_client_id)
    print 'Clients %d <- %d merged' % (master_client_id, slave_client_id)

    if not objects['merges'].has_key(master_client_id):
        objects['merges'][master_client_id] = []
    data = dict()
    data['slave_client_id'] = slave_client_id
    objects['merges'][master_client_id].append(data)


def turn_on(invoice_id, sum=None):
    if sum:
        test_rpc.TestBalance.TurnOn({'invoice_id': invoice_id, 'sum': sum})
        print 'Invoice %d is turned on %d' % (invoice_id, sum)
    else:
        test_rpc.TestBalance.TurnOn({'invoice_id': invoice_id})
        print 'Invoice %d is turned on full sum' % invoice_id

    if not objects['actions'].has_key('turn_on'):
        objects['actions']['turn_on'] = []
    data = dict()
    data['invoice_id'] = invoice_id
    data['sum'] = sum
    objects['actions']['turn_on'].append(data)


def hide_act(act_id):
    test_rpc.TestBalance.HideAct(act_id)
    print 'Act %d hidden' % act_id


def unhide_act(act_id):
    test_rpc.TestBalance.UnhideAct(act_id)
    print 'Act %d unhidden' % act_id


##------------------------------------------------------------------------------
def create_person(client_id, type_, params=None):
    ## Переданные обязательные параметры
    main_params = {
        'client_id': client_id,
        'type': type_
    }
    ## Обязательные параметры зависящие от переданного типа
    defaults_by_type = common_data.defaults_by_type
    ## Общие для всех типов параметры
    if not type_ in ['endbuyer_ph', 'endbuyer_ur']:
        defaults = {'fname': 'Test1',
                    'lname': 'Test2',
                    'mname': 'Test3'}
    ## person_params <- общие <- "по-умолчанию" для типа <- переданные <- переданные по-умолчанию
    person_params = {}
    if not type_ in ['endbuyer_ph', 'endbuyer_ur']:
        person_params.update(defaults)
    person_params.update(defaults_by_type[type_])
    if params <> None:
        person_params.update(params)
    person_params.update(main_params)

    person_id = rpc.Balance.CreatePerson(passport_uid, person_params)

    print ('{0:<' + str(log_align) + '} | {1}, {2}').format("Person_id: %s" % (str(person_id)), main_params, params)
    ##    print 'Person: %d' % (person_id)

    if not objects['persons'].has_key(person_id):
        objects['persons'][person_id] = dict()
    objects['persons'][person_id]['passport_uid'] = passport_uid
    objects['persons'][person_id].update(person_params)

    return person_id


def get_input_value(query):
    ##Waiting for the value like: select input fromt_export where type = 'UA_TRANSFER' and classname = 'Order' and object_id = <id>
    query = query.replace("'", "\'")
    query = query.replace("input", "UTL_ENCODE.BASE64_ENCODE(input) as key1")
    value = test_rpc.ExecuteSQL('balance', query)[0]['key1']
    return pickle.loads(value.decode('base64'))


def set_input_value(data):
    # запиклит передаваемое значение, и преобразует в однострочное выражение для insert/update
    pickled_data = pickle.dumps(data)
    pickled_list = pickled_data.split('\n')
    for index, row in enumerate(pickled_list):
        pickled_list[index] = "'" + row.replace("'", "''") + "'"
    result = ('UTL_RAW.CAST_TO_RAW(' + '||chr(10)||'.join(pickled_list) + ')')
    return result


def tmp(str):
    ## Вариант 1: получение списка вызовов из глобального пространства имён
    ## Вариант 2: примеры вызово в аннотациях (3.0 ?) - получение аннотаций для функций
    exmpl = {
        'CreateTransferMultiple': 'MTestLib.CreateTransferMultiple(16571028,[{"ServiceID":7, "ServiceOrderID":"291631412", "QtyOld":"30000.000000", "QtyNew":"0.000000"}],[{"ServiceID":7, "ServiceOrderID":"291631411", QtyDelta":"30000.000000"}])',
        'UpdateCampaigns': 'MTestLib.UpdateCampaigns([{"ServiceID": 7, "ServiceOrderID": 291631705, "dt": datetime.datetime.now(), "stop": 0, "Bucks": 14, "Money": 0}])',
        '': ''
    }
    for method in exmpl:
        if str.upper() in method.upper(): print (exmpl[method])


def get_force_overdraft(client_id, service_id, limit, firm_id=1, start_dt=datetime.datetime.now(), currency=None):
    ##    dt_str = start_dt.strftime(sql_date_format)  ##Переформатировать в строку
    sql = 'insert into t_client_overdraft (client_id, service_id, overdraft_limit, firm_id, start_dt, update_dt, currency) values (:client_id, :service_id, :limit, :firm_id, :start_dt, sysdate, :currency)'
    ##    sql_params = {'client_id': client_id, 'service_id': service_id, 'limit': limit, 'firm_id': firm_id, 'dt_str': 'to_date(\'{0}\',\'DD.MM.YYYY HH24:MI:SS\')'.format(dt_str), 'currency': currency}
    sql_params = {'client_id': client_id, 'service_id': service_id, 'limit': limit, 'firm_id': firm_id,
                  'start_dt': start_dt, 'currency': currency}
    test_rpc.ExecuteSQL('balance', sql, sql_params)
    test_rpc.ExecuteSQL('balance', 'commit')
    print ('Overdraft given to %d (service: %d, limit: %d, firm: %d, dt: %s, multicurrency: %s)' % (
        client_id, service_id, limit, firm_id, start_dt, currency))

    if not objects['actions'].has_key('get_force_overdraft'):
        objects['actions']['get_force_overdraft'] = []
    data = dict()
    data['client_id'] = client_id
    data['service_id'] = service_id
    data['limit'] = limit
    data['firm_id'] = firm_id
    data['start_dt'] = start_dt
    data['currency'] = currency
    objects['actions']['get_force_overdraft'].append(data)


def add_months_to_date(base_date, months):
    a_months = months
    if abs(a_months) > 11:
        s = a_months // abs(a_months)
        div, mod = divmod(a_months * s, 12)
        a_months = mod * s
        a_years = div * s
    else:
        a_years = 0

    year = base_date.year + a_years

    month = base_date.month + a_months
    if month > 12:
        year += 1
        month -= 12
    elif month < 1:
        year -= 1
        month += 12
    day = min(calendar.monthrange(year, month)[1],
              base_date.day)
    return datetime.datetime.combine(datetime.date(year, month, day), base_date.time())


def get_overdraft(client_id, service_id, limit, firm_id=1, start_dt=datetime.datetime.now(), currency=None,
                  invoice_currency=None):
    if firm_id == 1:
        paysys_id = 1003
        person_id = create_person(client_id, 'ur', {'phone': '234'})
        currency_product = 503162
        region_id = 225
    elif firm_id == 2:
        paysys_id = 1018
        person_id = create_person(client_id, 'pu', {'phone': '234'})
        currency_product = 503165
        region_id = 187

    if service_id == 7 and invoice_currency:
        product_id = currency_product
    elif service_id == 7:
        product_id = 1475
    elif service_id == 11:
        product_id = 2136
    qty = (limit * 12) + 10

    if currency:
        create_client({'CLIENT_ID': client_id, 'REGION_ID': region_id, 'CURRENCY': currency,
                       'MIGRATE_TO_CURRENCY': start_dt + datetime.timedelta(seconds=5), 'SERVICE_ID': service_id,
                       'CURRENCY_CONVERT_TYPE': 'COPY'})
        money = money_new = qty / 5
        bucks = bucks_new = 0
    else:
        bucks = bucks_new = qty / 5
        money = money_new = 0

    campaigns_list = [
        {'service_id': service_id, 'product_id': product_id, 'qty': qty, 'begin_dt': add_months_to_date(start_dt, -6)}
    ]
    invoice_id, orders_list = create_force_invoice(client_id, person_id, campaigns_list, paysys_id,
                                                   add_months_to_date(start_dt, -6))
    OEBS_payment(invoice_id, None, None)
    ##    (year, month) = divmod(datetime.datetime(2014,12,29).month, 12)
    test_rpc.TestBalance.OldCampaigns({'Money': money, 'Bucks': bucks, 'stop': '0', 'service_id': service_id,
                                       'service_order_id': orders_list[0]['ServiceOrderID']},
                                      add_months_to_date(start_dt, -6))
    test_rpc.TestBalance.OldAct(invoice_id, add_months_to_date(start_dt, -6))

    for i in [4, 3, 2, 1]:
        money_new += money
        bucks_new += bucks
        test_rpc.TestBalance.OldCampaigns(
            {'Money': money_new, 'Bucks': bucks_new, 'stop': '0', 'service_id': service_id,
             'service_order_id': orders_list[0]['ServiceOrderID']}, add_months_to_date(start_dt, -i))
        test_rpc.TestBalance.OldAct(invoice_id, add_months_to_date(start_dt, -i))

    test_rpc.TestBalance.CalculateOverdraft([client_id])
    print ('Fair оverdraft given to %d (service: %d, limit: %d, firm: %d, dt: %s, multicurrency: %s)' % (
        client_id, service_id, limit, firm_id, start_dt.strftime(sql_date_format), currency))

    if not objects['actions'].has_key('get_overdraft'):
        objects['actions']['get_force_overdraft'] = []
    data = dict()
    data['client_id'] = client_id
    data['service_id'] = service_id
    data['limit'] = limit
    data['firm_id'] = firm_id
    data['start_dt'] = start_dt
    data['currency'] = currency
    objects['actions']['get_force_overdraft'].append(data)


def get_direct_discount(client_id, dt, pct=None, budget=None, currency='null'):
    if pct:
        # sql = "select x+1 as x from bo.t_scale_points where scale_code = 'direct25' and end_dt and hidden = 0 and y = :y and nvl(currency, 'null') = :currency"
        # sql_params = {'y': pct, 'currency': currency or 'null'}
        sql = "select x+1 as x from bo.t_scale_points where scale_code = 'direct25' and end_dt is null and hidden = 0 and y = :y and currency is null"
        sql_params = {'y': pct}
        if budget:
            print "Both params specified. 'pct' value will override 'budget'"
        budgets_list = test_rpc.ExecuteSQL('balance', sql, sql_params)
        if len(budgets_list) != 1:
            raise Exception(u"MTestlib exception: 'Empty or multiple budget values'")
        else:
            budget = budgets_list[0]['x']
        budget = int(math.ceil((budget / 25.42)))
    # sql = "Insert into bo.t_client_direct_budget (ID,CLIENT_ID,END_DT,CLASSNAME,BUDGET,CURRENCY,UPDATE_DT) values (s_client_direct_budget_id.nextval,:client_id,:end_dt,'DirectDiscountCalculator',:budget,:currency,sysdate)"
    # sql_params = {'client_id': client_id, 'end_dt': dt or datetime.datetime.today().date().replace(day=1), 'budget': budget, 'currency': currency}
    sql = "Insert into bo.t_client_direct_budget (ID,CLIENT_ID,END_DT,CLASSNAME,BUDGET,CURRENCY,UPDATE_DT) values (s_client_direct_budget_id.nextval,:client_id,:end_dt,'DirectDiscountCalculator',:budget,null,sysdate)"
    sql_params = {'client_id': client_id, 'end_dt': dt or datetime.datetime.today().date().replace(day=1),
                  'budget': budget}
    test_rpc.ExecuteSQL('balance', sql, sql_params);
    print('SUCCESS')

    if not objects['actions'].has_key('get_direct_discount'):
        objects['actions']['get_direct_discount'] = []
    data = dict()
    data['client_id'] = client_id
    data['dt'] = dt
    data['pct'] = pct
    data['budget'] = budget
    data['currency'] = currency
    objects['actions']['get_direct_discount'].append(data)


# Use test_rpc.ExportObject('OEBS', 'Person', 4539724) instead
##def oebs_export (export_type, object_id):
##    test_rpc.ExecuteSQL('balance', 'update (select  * from t_export where  CLASSNAME = :export_type and   type=\'OEBS\' and OBJECT_ID = :object_id ) set priority = -1', {'export_type': export_type,'object_id': object_id })
##    state = 0
##    print str(export_type) + ' export begin:' + str(datetime.datetime.now())
##    while True:
##        if state == 0:
##            state = test_rpc.ExecuteSQL('balance', 'select  state from t_export where  CLASSNAME = :export_type and   type=\'OEBS\' and OBJECT_ID = :object_id ', {'export_type': export_type ,'object_id': object_id })[0]['state']
##            print('...(3)'); time.sleep(3)
##        else:
##            print str(export_type) + ' export   end:' + str(datetime.datetime.now())
##            break

def create_contract2(_type, params, mode='Contract'):
    if mode == 'Contract':
        items_by_type = common_data.contracts_by_type
        mapping_list = {
            'client_id': 'client-id'
            , 'person_id': 'person-id'
            , 'CURRENCY': 'credit-currency-limit'
            , 'dt': 'dt'
            , 'FINISH_DT': 'finish-dt'
            , 'SERVICES': 'services-'  ##this param should be
            , 'is_signed': 'is-signed,is-signed-date,is-signed-dt'  ## several values for single param
            , 'is_faxed': 'is-faxed,is-faxed-date,is-faxed-dt'
            , 'is_cancelled': 'is-cancelled,is-cancelled-date,is-cancelled-dt'
            , 'IS_SUSPENDED': 'is-suspended,is-suspended-date,is-suspended-dt'
            , 'SENT_DT': 'sent-dt,sent-dt-date,sent-dt-dt'
            , 'IS_BOOKED': 'is-booked'
            , 'DISTRIBUTION_TAG': 'distribution-tag'
            , 'COMMISSION_TYPE': 'commission-type'
            , 'NON_RESIDENT_CLIENTS': 'non-resident-clients'
            , 'REPAYMENT_ON_CONSUME': 'repayment-on-consume'
        }
    elif mode == 'Collateral':
        items_by_type = common_data.collaterals_by_type
        mapping_list = {
            'contract2_id': 'id'
            , 'dt': 'col-new-dt'
            , 'XXX': 'col-new-print-form-type'
            , 'is_signed': 'col-new-is-signed,col-new-is-signed-date,col-new-is-signed-dt'
            ## several values for single param
            , 'is_faxed': 'col-new-is-faxed,col-new-is-faxed-date,col-new-is-faxed-dt'
            # , 'is_cancelled' : 'is-cancelled,is-cancelled-date,is-cancelled-dt'
            # , 'IS_SUSPENDED' : 'is-suspended,is-suspended-date,is-suspended-dt'
            # , 'SENT_DT'      : 'sent-dt,sent-dt-date,sent-dt-dt'
            # , 'IS_BOOKED'    : 'is-booked'
        }
    if not items_by_type.has_key(_type):
        raise Exception('MTestlib exception: No such contract type')
    else:
        ignored_keys = []
        source = items_by_type[_type]
        ##Convert to list of the tuples ('param', 'value')
        source_tmp = urlparse.parse_qsl(source, True)
        ##Convert to dict
        source_dict = {key: value.decode('utf-8') for (key, value) in source_tmp}
        for key in params:
            try:
                if key == 'SERVICES':  ##special logic for services
                    # param_services = int(params[key])
                    for service_key in source_dict.keys():
                        if service_key.startswith(mapping_list[key]):
                            source_dict.pop(service_key)
                    for subkey in params[key]:
                        source_dict[mapping_list[key] + str(subkey)] = subkey
                elif key.upper() in ['IS_SIGNED', 'IS_FAXED', 'IS_CANCELLED', 'IS_SUSPENDED', 'SENT_DT',
                                     'IS_BOOKED']:  ##special logic for dates
                    keylist = mapping_list[key].split(',')
                    source_dict[keylist[0]] = ''  ## is-signed - flag
                    if len(keylist) > 1:
                        source_dict[keylist[2]] = params[key]  ## is-signed-dt - real date
                        mn = {1: 'янв', 2: 'фев', 3: 'мар', 4: 'апр', 5: 'май', 6: 'июн', 7: 'июл', 8: 'авг', 9: 'сен',
                              10: 'окт', 11: 'ноя', 12: 'дек'}
                        ## ...-date - date for display in format: 'DD MON YYYY г.'
                        source_dict[keylist[1]] = '{0} {1} {2} г.'.format(int(params[key][8:10]),
                                                                          mn[int(params[key][5:7])], params[key][:4])
                elif key in ('NON_RESIDENT_CLIENTS', 'REPAYMENT_ON_CONSUME'):
                    if mapping_list[key] in source_dict:
                        source_dict.pop(mapping_list[key])
                    if params[key] == 1:
                        source_dict[mapping_list[key]] = ''
                else:
                    source_dict[mapping_list[key]] = params[key] or ''  ##empty string for params with None value
            except KeyError:
                ignored_keys.append(key)
                ## print source_dict
                ##        print [(key,source_dict[key]) for key in sorted(source_dict.keys())]
        print('Next params were ignored: %s') % str(ignored_keys)

        contract = rpc.Balance.CreateContract(passport_uid, source_dict)

        print 'mode = {0} | Contract_id: {1} (external_id: {2})'.format(mode, contract['ID'], contract['EXTERNAL_ID'])

        if not objects['contracts'].has_key(contract['ID']):
            objects['contracts'][contract['ID']] = []
        data = dict()
        data['passport_uid'] = passport_uid
        data['source_dict'] = source_dict
        data['mode'] = mode
        data['external_id'] = contract['EXTERNAL_ID']
        objects['contracts'][contract['ID']].append(data)

        return contract['ID']


def create_collateral2(_type, params):
    create_contract2(_type, params, 'Collateral')


def create_distr_client_person_tag():
    tag_id = test_rpc.ExecuteSQL('balance', "select max(id) from t_distribution_tag")[0]['MAX(ID)'] + 1
    print 'Tag_id: %d' % (tag_id)
    client_id = create_client({'IS_AGENCY': 0, 'NAME': u'Test AG'})
    person_id = create_person(client_id, 'ur', {'is-partner': '1'})
    rpc.Balance.CreateOrUpdateDistributionTag(16571028,
                                              {'TagID': tag_id, 'TagName': 'CreatedByScript', 'ClientID': client_id})
    return client_id, person_id, tag_id


def get_input_value(query, key='input'):
    ##Waiting for the value like: select input fromt_export where type = 'UA_TRANSFER' and classname = 'Order' and object_id = <id>
    query = query.replace("'", "\'")
    query = query.replace(key, "UTL_ENCODE.BASE64_ENCODE({0}) as key1".format(key))
    value = test_rpc.ExecuteSQL('balance', query)[0]['key1']
    return pickle.loads(value.decode('base64'))


def tmp(str):
    ## Вариант 1: получение списка вызовов из глобального пространства имён
    ## Вариант 2: примеры вызово в аннотациях (3.0 ?) - получение аннотаций для функций
    exmpl = {
        'CreateTransferMultiple': 'rpc.Balance.CreateTransferMultiple(16571028,[{"ServiceID":7, "ServiceOrderID":"291631412", "QtyOld":"30000.000000", "QtyNew":"0.000000"}],[{"ServiceID":7, "ServiceOrderID":"291631411", "QtyDelta":"30000.000000"}])',
        'UpdateCampaigns': 'rpc.Balance.UpdateCampaigns([{"ServiceID": 7, "ServiceOrderID": 291631705, "dt": datetime.datetime.now(), "stop": 0, "Bucks": 14, "Money": 0}])',
        '': '',
        'ServerProxy': 'rpc = xmlrpclib.ServerProxy("http://greed-tm1f.yandex.ru:8002/xmlrpc", allow_none=1, use_datetime=1)'
    }
    for method in exmpl:
        if str.upper() in method.upper(): print (exmpl[method])


def wait_for(sql, sql_params, value=1, interval=5, timeout=300):
    # sql = 'select value as val from bo.t_table where param = param_value'
    timer = 0
    while timer < timeout:
        time.sleep(interval)
        timer += interval
        cur_val = test_rpc.ExecuteSQL('balance', sql, sql_params)[0]['val']
        print('{0} sec: {1}'.format(interval, cur_val))
        if cur_val == value: return 'Waiting for {0} sec'.format(timer)
    raise Exception(
        "MTestlib exception: 'Timeout in {0} sec'\nWaiting for: {3}\nParams: {4}".format(timeout, sql, sql_params))


def create_operation(passport_uid):
    operation_id = rpc.Balance.CreateOperation(passport_uid)
    print (operation_id)

    if not objects['operations'].has_key(operation_id):
        objects['operations'][operation_id] = dict()
    objects['operations'][operation_id]['passport_uid'] = passport_uid

    return operation_id


def create_transfer_multiple(passport_uid, src_list, dst_list, output=1, operation_id=None):
    result = rpc.Balance.CreateTransferMultiple(passport_uid, src_list, dst_list, output, operation_id)
    print (result)

    if not objects['actions'].has_key('create_transfer_multiple'):
        objects['actions']['create_transfer_multiple'] = []
    data = dict()
    data['passport_uid'] = passport_uid
    data['src_list'] = src_list
    data['dst_list'] = dst_list
    data['output'] = output
    data['operation_id'] = operation_id
    objects['actions']['create_transfer_multiple'].append(data)

    return result


def dict_repr_formatter(obj):
    indent = ''
    flag = True
    pattern = re.compile('[{},]')
    obj_str = obj.__repr__()
    while flag:
        item = re.search(pattern, obj_str)
        if item and item.group() == '{':
            obj_str = obj_str.replace('{', '\n' + indent + '<', 1)
            indent += '\t'
        elif item and item.group() == '}':
            indent = indent[:-1]
            obj_str = obj_str.replace('}', '\n' + indent + '>', 1)
        elif item and item.group() == ',':
            obj_str = obj_str.replace(',', ';\n' + indent, 1)
        elif item is None:
            break
    obj_str = obj_str.replace('<', '{')
    obj_str = obj_str.replace('>', '}')
    obj_str = obj_str.replace(';', ',')
    ##    print(obj_str)
    Print(dict(obj))
