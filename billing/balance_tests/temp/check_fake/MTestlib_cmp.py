# -*- coding: utf-8 -*-

import pprint
import datetime
from datetime import date
import urlparse
import time

import proxy_provider


def Print(obj):
    print pprint.pformat(obj).decode('unicode_escape')


##host = 'greed-ts1f'
##host = 'greed-load2e'

host = 'greed-ts1f'
tm = proxy_provider.GetServiceProxy(host, 0)
test = proxy_provider.GetServiceProxy(host, 1)


def proxy():
    tm = proxy_provider.GetServiceProxy(host, 0)
    test = proxy_provider.GetServiceProxy(host, 1)
    return tm, test


## Для приведения формата даты
sql_date_format = "%d.%m.%Y %H:%M:%S"


##def set_service_proxy (proxy, test_flag):
##    global test
##    global tm
##    if test_flag: test = proxy
##    else: tm = proxy

def create_client(client_params):
    tm, test = proxy()
    ## Параметры "по-умолчанию"
    client_params_defaults = {
        'EMAIL': 'client@in-fo.ru',
        'FAX': '5408410',
        'IS_AGENCY': 0,
        'NAME': u'Test',
        'PHONE': '911',
        'URL': 'www.qwerty.ru'
    }
    params = client_params_defaults.copy()
    ## Если при вызове переданы набор параметров,
    ## он переопределяет соответствующие значения "по-умолчанию"
    if client_params:
        params.update(client_params)

    code, status, client_id = tm.Balance.CreateClient(16571028, params)
    if params['IS_AGENCY'] == 1:
        print 'Agency_id: %d' % (client_id)
    else:
        print 'Client_id: %d' % (client_id)
    return client_id


def link_client_uid(client_id, uid):
    tm, test = proxy()
    id_uid = test.ExecuteSQL('balance', 'select PASSPORT_ID from T_ACCOUNT where LOGIN = :uid', {'uid': uid})[0]['passport_id']
    test.ExecuteSQL('balance', 'update (select * from T_ACCOUNT where PASSPORT_ID = :id_uid ) set client_id = :client_id',
                    {'id_uid': id_uid, 'client_id': client_id})


##    print '%s linked to %s' % (uid, client_id)

def get_next_service_order_id(service_id):
    tm, test = proxy()
    if service_id == 7:
        seq_name = 'S_TEST_SERVICE_ORDER_ID_7'
    elif service_id == 116:
        seq_name = 'S_TEST_SERVICE_ORDER_ID_116'
    else:
        seq_name = 'S_TEST_SERVICE_ORDER_ID'

    service_order_id = test.ExecuteSQL('balance', 'select ' + seq_name + '.nextval from dual')[0]['nextval']
    ##    print 'Service_order_id: %d' % (service_order_id)
    return service_order_id


def create_or_update_order(client_id, product_id, service_id, service_order_id, params=None, agency_id=None,
                           manager_uid=None):
    tm, test = proxy()
    main_params = {
        'ClientID': client_id,
        'ProductID': product_id,
        'ServiceID': service_id,
        'ServiceOrderID': service_order_id
    }

    order_params_defaults = {
        'TEXT': 'Py_Test order',
        'GroupServiceOrderID': -1,
        'GroupWithoutTransfer': 1
    }
    ## Дополняем словарь необязательными параметрами, если они переданы
    if agency_id: main_params.update({'AgencyID': agency_id})
    if manager_uid: main_params.update({'ManagerUID': manager_uid})
    ## Последовательно применяем параметры:
    ## order_params <- по-умолчанию <- переданные <- переданные обязательные
    order_params = {}
    order_params.update(order_params_defaults)
    order_params.update(params)
    order_params.update(main_params)

    tm.Balance.CreateOrUpdateOrdersBatch(16571028, [order_params])
    order_id = test.ExecuteSQL('balance', 'select max(id) from T_ORDER where CLIENT_ID = :client_id', {'client_id': client_id})[0][
        'MAX(ID)']
    ##    print 'Order: %d' % order_id
    return order_id


def create_request(client_id, orders_list, overdraft, invoice_dt=None):
    tm, test = proxy()
    ##req = tm.Balance.CreateRequest(16571028, client_id, [{'Qty': qty, 'ServiceID': service_id,
    ##                                        'ServiceOrderID': service_order_id, 'BeginDT':requestDate}],{})
    req = tm.Balance.CreateRequest(16571028, client_id, orders_list, {'Overdraft': overdraft})
    request_id = \
        test.ExecuteSQL('balance', 'select max(id) from T_REQUEST where CLIENT_ID = :client_id', {'client_id': client_id})[0][
            'MAX(ID)']
    if invoice_dt <> None:
        test.ExecuteSQL('balance', 
            'update (select * from T_REQUEST where ID = :request_id ) set invoice_dt = to_date(:invoice_dt,\'DD.MM.YYYY HH24:MI:SS\')',
            {'request_id': request_id, 'invoice_dt': invoice_dt.strftime(sql_date_format)})
    ##    print 'Request: %s' % (req[3])
    return request_id


def create_invoice(request_id, person_id, paysys_id, credit, overdraft=None, contract_id=None):
    tm, test = proxy()
    ##    if contract_id <> None:
    invoice_id = tm.Balance.CreateInvoice(16571028,
                                          {'PaysysID': paysys_id, 'PersonID': person_id, 'RequestID': request_id,
                                           'Credit': credit, 'Overdraft': overdraft, 'ContractID': contract_id})
    ##    if contract_id == None:
    ##        invoice_id = tm.Balance.CreateInvoice(16571028,{'PaysysID': paysys_id, 'PersonID': person_id, 'RequestID': request_id, 'Credit':credit})
    print 'Invoice: https://balance-admin.greed-tm1f.yandex.ru/invoice.xml?invoice_id=' + str(invoice_id)
    query = "select external_id, total_sum, consume_sum from t_invoice where id = :invoice_id"
    external_id = test.ExecuteSQL('balance', query, {'invoice_id': invoice_id})[0]['external_id']
    return invoice_id


def create_force_invoice(client_id, person_id, campaigns_list, paysys_id, invoice_dt=None, agency_id=None, credit=0,
                         contract_id=0, manager_uid=None):
    tm, test = proxy()
    ## Владелец заказа: клиент
    order_owner = client_id
    ## Владелец счёта: агентство, если передано
    if agency_id:
        invoice_owner = agency_id
    else:
        invoice_owner = client_id

    service_order_id = []
    order_id = []
    orders_list = []
    ##    print '------ Orders list: ------'
    ## Проходим цикл столько раз, сколько словарей-заказов передано в campaigns_list
    for i in [x for x in xrange(len(campaigns_list))]:
        ## Сперва создаём заказы
        service_order_id.append(get_next_service_order_id(campaigns_list[i]['service_id']))
        order_id.append(
            create_or_update_order(order_owner,
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
    if credit == 1:
        invoice_id = create_invoice(request_id, person_id, paysys_id, credit=1, contract_id=contract_id)
    else:
        invoice_id = create_invoice(request_id, person_id, paysys_id)
    ## Добавляем в order_list продукт, чтобы вернуть из метода полную информацию о результате выполнения
    for i in [x for x in xrange(len(campaigns_list))]:
        orders_list[i].update({'ProductID': campaigns_list[i]['product_id']})
    ##    print orders_list
    return invoice_id, orders_list


def OEBS_payment(invoice_id, payment_date=None, payment_sum=None):
    tm, test = proxy()
    query = "select external_id, total_sum, consume_sum from t_invoice where id = :invoice_id"
    external_id = test.ExecuteSQL('balance', query, {'invoice_id': invoice_id})[0]['external_id']
    total_sum = test.ExecuteSQL('balance', query, {'invoice_id': invoice_id})[0]['total_sum']
    if payment_sum:
        p_sum = payment_sum
    else:
        p_sum = total_sum
    if payment_date:
        p_date = payment_date
    else:
        p_date = datetime.datetime.today()
    test.ExecuteSQL('balance', 
        'insert into t_correction_payment (dt, doc_date, sum, memo, invoice_eid) values (to_date(:paymentDate,' +
        '\'DD.MM.YYYY HH24:MI:SS\'),to_date(:paymentDate,\'DD.MM.YYYY HH24:MI:SS\'),:total_sum,\'Testing\',:external_id)',
        {'paymentDate': p_date.strftime(sql_date_format), 'total_sum': p_sum, 'external_id': external_id})
    test.TestBalance.OEBSPayment(invoice_id)
    ##    print 'Invoice %s (%d) received payment %d' % (external_id, invoice_id, p_sum)
    return 1


def do_campaigns(service_id, service_order_id, campaigns_params, do_stop=0, campaigns_dt=None):
    tm, test = proxy()
    campaigns_defaults = dict.fromkeys(['Bucks', 'Shows', 'Clicks', 'Units', 'Days', 'Money'], 0)
    params = campaigns_defaults.copy()
    params.update(campaigns_params)
    params.update({'service_id': service_id, 'service_order_id': service_order_id, 'do_stop': do_stop})
    if campaigns_dt:
        test.TestBalance.OldCampaigns(params, campaigns_dt)
    else:
        test.TestBalance.Campaigns(params)
    ##    print '%d-%d: completed %d Bucks, %d Money' % (service_id, service_order_id, params['Bucks'], params['Money'])
    return 1


def create_act(invoice_id, act_dt=None):
    tm, test = proxy()
    if act_dt:
        act_id = test.TestBalance.OldAct(invoice_id, act_dt)
    else:
        act_id = test.TestBalance.Act(invoice_id)


##    print 'Act_id: %d' % act_id[0]

def generate_acts(client_id, force, date):
    tm, test = proxy()
    test.TestBalance.ActAccounter(client_id, force, date)


##    print 'Acts generated'


def some_days_ago(date, number):
    tm, test = proxy()
    delta = datetime.timedelta(days=number)
    dt = date - delta
    return (dt)


def some_month_ago(number):
    tm, test = proxy()
    (year, month) = divmod(date.today().month, 12)
    day = date.today().day
    if month == 0:
        month = 12
        year = year - 1
    if (day == 31) or (day == 30) or (day == 29):
        day = 28
    dt = datetime.datetime(date.today().year + year, month - number, day)
    return dt


def get_overdraft(is_money, service_order_id, invoice_id, service_id, client_id, limit):
    tm, test = proxy()
    money_first = 0
    money_q = 0
    bucks_first = 0
    bucks_q = 0
    limit_new = (limit * 12) + 10

    if is_money == 1:
        money_first = 10

    if is_money == 0:
        bucks_first = 10

    test.TestBalance.OldCampaigns({'Money': money_first, 'Bucks': bucks_first, 'stop': '0', 'service_id': service_id,
                                   'service_order_id': service_order_id}, some_month_ago(6))
    test.TestBalance.OldAct(invoice_id, some_month_ago(6))

    for i in [4, 3, 2, 1]:
        if is_money == 1:
            money_q = limit_new / i

        if is_money == 0:
            bucks_q = limit_new / i
        test.TestBalance.OldCampaigns({'Money': money_q, 'Bucks': bucks_q, 'stop': '0', 'service_id': service_id,
                                       'service_order_id': service_order_id}, some_month_ago(i))
        test.TestBalance.OldAct(invoice_id, some_month_ago(i))

    test.TestBalance.CalculateOverdraft([client_id])


##    print 'get_overdraft complete'

def merge_clients(master_client_id, slave_client_id):
    tm, test = proxy()
    test.TestBalance.MergeClients(16571028, master_client_id, slave_client_id)


##    print 'Clients %d <- %d merged' % (master_client_id, slave_client_id)

def turn_on(invoice_id, sum=None):
    tm, test = proxy()
    if sum:
        test.TestBalance.TurnOn({'invoice_id': invoice_id, 'sum': sum})
    ##        print 'Invoice %d is turned on %d' % (invoice_id, sum)
    else:
        test.TestBalance.TurnOn({'invoice_id': invoice_id})


##        print 'Invoice %d is turned on full sum' % invoice_id

def hide_act(act_id):
    tm, test = proxy()
    test.TestBalance.HideAct(act_id)


##    print 'Act %d hidden' % act_id

def unhide_act(act_id):
    proxy()
    test.TestBalance.UnideAct(act_id)


##    print 'Act %d unhidden' % act_id
##------------------------------------------------------------------------------
def create_person(client_id, type_, params=None):
    tm, test = proxy()
    main_params = {
        'client_id': client_id,
        'type': type_
    }

    defaults_by_type = {
        'ur': {'name': 'PyTest Org',  ## Юр.лицо РФ
               'phone': '+7 905 1234567',
               'email': 'test-balance-notify@yandex-team.ru',
               'postcode': '123456',
               'postaddress': 'Python street, 42',
               'inn': '7719246912',
               'longname': 'PyTest Organization ООО',
               'legaladdress': 'Python street, 42',
               'person_id': 0},
        'ur2': {'name': 'PyTest Org',
                'phone': '+7 812 9999999',
                ##'fax': '+7 812 9999999',
                'email': 'pytestorg@yandex.ru',
                ##'representative': 'Hf', ## Контактное лицо

                'address': 'Python street, 42',
                ##'authority-doc-details': 'WY',
                ##'authority-doc-type': u'\u041f\u043e\u043b\u043e\u0436\u0435\u043d\u0438\u0435 \u043e \u0444\u0438\u043b\u0438\u0430\u043b\u0435',
                ##'bik': '044030723',
                ##'delivery-city': 'KZN',
                ##'delivery-type': '4',
                'account': '40702810574605466942',
                'inn': '7719246912',
                ##'invalid-address': '1',
                ##'kpp': '7243',
                'legaladdress': 'Python street, 42',
                'live-signature': '1',
                'longname': 'PyTest Organization ООО',
                'person_id': 0,
                'postaddress': 'Python street, 42',
                'postcode': '191025'
                ##'s_signer-position-name': 'President',
                ##'signer-person-gender': 'W',
                ##'signer-person-name': 'Signer aZzy',
                ##'type': 'ur'
                ##'vip': '1'
                },
        'ph': {'phone': '+7 905 1234567',  ## Физ.лицо РФ
               'email': 'testagpi2@yandex.ru',
               'person_id': 0},
        'pu': {'phone': '+7 905 1234567',  ## Юр.лицо РФ
               'email': 'testagpi2@yandex.ru',
               'inn': '245781126558',
               'person_id': 0},
        'trp': {'email': 'testagpi2@yandex.ru',  ## Физ-лицо Турция
                'phone': '+7 905 1234567',
                'postcode': '12345',
                'postaddress': 'Tur addr',
                'person_id': 0},
        'usp': {'lname': 'Py_John',
                'fname': 'Py_Doy',
                'email': 'usp@email.com',
                'phone': '+1 650 5173548',
                'postaddress': 'Py_Street 5',
                'city': 'py_city',
                'postcode': '98411',
                ##'file': '/testDocs.txt',
                'us-state': 'CA',
                'verified-docs': '1',
                'person_id': 0},
        'usu': {'name': 'Py_John',
                'phone': '+1 650 5173548',
                'email': 'usp@email.com',
                'postaddress': 'Py_Street 5',
                'city': 'py_city',
                'us-state': 'CA',
                'postcode': '98411',
                ##'file': '/testDocs.txt',
                'verified-docs': '1',
                'person_id': 0}
    }

    defaults = {'fname': 'Test1',
                'lname': 'Test2',
                'mname': 'Test3'}

    person_params = {}
    person_params.update(defaults)
    person_params.update(defaults_by_type[type_])
    if params <> None:
        person_params.update(params)
    person_params.update(main_params)

    person_id = tm.Balance.CreatePerson(16571028, person_params)
    print 'Person: %d' % (person_id)
    return person_id


def create_contract(client_id, person_id, service_id, contract_type=None):
    tm, test = proxy()
    ######    Коммерческий не агентский (предоплата)
    if contract_type == 'comm':
        ttt = urlparse.parse_qsl('external-id=&num=&commission=0&print-form-type=0&brand-type=70&client-id=' + str(
            client_id) + '&person-id=' + str(
            person_id) + '&account-type=0&bank-details-id=21&manager-code=20453&manager-bo-code=&dt=2013-03-05T00%3A00%3A00&finish-dt=2018-12-29T00%3A00%3A00&payment-type=2&unilateral=1&services=1&services-' + str(
            service_id) + '=' + str(
            service_id) + '&memo=&atypical-conditions-checkpassed=1&calc-termination=&attorney-agency-id=&commission-charge-type=1&commission-payback-type=2&commission-payback-pct=&commission-type=&supercommission-bonus=1&partner-commission-type=1&partner-commission-pct=&named-client-declared-sum=&commission-declared-sum=&supercommission=0&partner-commission-sum=&linked-contracts=1&limitlinked-contracts=&partner-min-commission-sum=&advance-payment-sum=&discard-nds=0&discount-policy-type=3&discount-fixed=12&declared-sum=&fixed-discount-pct=&belarus-budget-price=&ukr-budget=&budget-discount-pct=&kz-budget=&federal-annual-program-budget=&federal-budget=&belarus-budget=&federal-declared-budget=&kzt-budget=&year-product-discount=&year-planning-discount=&year-planning-discount-custom=&use-ua-cons-discount-checkpassed=1&consolidated-discount=&use-consolidated-discount-checkpassed=1&regional-budget=&use-regional-cons-discount-checkpassed=1&pda-budget=&contract-discount=&retro-discount=&discount-pct=&discount-findt=&credit-type=0&payment-term=&payment-term-max=&calc-defermant=0&personal-account-checkpassed=1&lift-credit-on-payment-checkpassed=1&auto-credit-checkpassed=1&personal-account-fictive-checkpassed=1&repayment-on-consume-checkpassed=1&credit-currency-limit=810&limitcredit-currency-limit=&credit-limit=17&limitcredit-limit=&turnover-forecast=17&limitturnover-forecast=&credit-limit-single=&partner-credit-checkpassed=1&discount-commission=&pp-1137-checkpassed=1&non-resident-clients-checkpassed=1&new-commissioner-report-checkpassed=1&commission-categories=%5B%5D&client-limits=%5B%5D&loyal-clients=%5B%5D&brand-clients=%5B%5D&discard-media-discount-checkpassed=1&is-booked-checkpassed=1&is-faxed-checkpassed=1&is-signed=&is-signed-checkpassed=1&is-signed-date=30+%D1%81%D0%B5%D0%BD+2014+%D0%B3.&is-signed-dt=2014-09-30T00%3A00%3A00&deal-passport-checkpassed=1&sent-dt-checkpassed=1&is-suspended-checkpassed=1&button-submit=%D0%A1%D0%BE%D1%85%D1%80%D0%B0%D0%BD%D0%B8%D1%82%D1%8C&collateral-form=&id=',
                                 True)
    ######   США: оптовый клиентский (предоплата)
    if contract_type == 'usa':
        ttt = urlparse.parse_qsl('external-id=&num=&commission=18&print-form-type=0&brand-type=70&client-id=' + str(
            client_id) + '&person-id=' +
                                 str(
                                     person_id) + '&account-type=0&manager-code=20453&manager-bo-code=&dt=2013-02-06T00%3A00%3A00&finish-dt=2018-12-30T00%3A00%3A00&' +
                                 'payment-type=2&unilateral=0&services=1&services-' + str(service_id) + '=' + str(
            service_id) + '&memo=&atypical-conditions-checkpassed=1&calc-termination=&attorney-agency-id=&' +
                                 'commission-charge-type=1&commission-payback-type=2&commission-payback-pct=&commission-type=&supercommission-bonus=1&' +
                                 'partner-commission-type=1&partner-commission-pct=&named-client-declared-sum=&commission-declared-sum=&supercommission=0&' +
                                 'partner-commission-sum=&linked-contracts=1&limitlinked-contracts=&partner-min-commission-sum=&advance-payment-sum=&discard-nds=0&' +
                                 'discount-policy-type=0&discount-fixed=12&declared-sum=&fixed-discount-pct=&belarus-budget-price=&budget-discount-pct=&' +
                                 'federal-declared-budget=&kz-budget=&federal-budget=&belarus-budget=&kzt-budget=&ukr-budget=&federal-annual-program-budget=&' +
                                 'year-product-discount=&year-planning-discount=&year-planning-discount-custom=&use-ua-cons-discount-checkpassed=1&consolidated-discount=&' +
                                 'use-consolidated-discount-checkpassed=1&regional-budget=&use-regional-cons-discount-checkpassed=1&pda-budget=&retro-discount=&' +
                                 'contract-discount=&discount-pct=&discount-findt=&credit-type=0&payment-term=&payment-term-max=&calc-defermant=0&' +
                                 'personal-account-checkpassed=1&lift-credit-on-payment-checkpassed=1&auto-credit-checkpassed=1&personal-account-fictive-checkpassed=1&' +
                                 'repayment-on-consume-checkpassed=1&credit-currency-limit=810&limitcredit-currency-limit=&credit-limit=17&limitcredit-limit=&' +
                                 'turnover-forecast=17&limitturnover-forecast=&credit-limit-single=&partner-credit-checkpassed=1&discount-commission=&' +
                                 'pp-1137-checkpassed=1&non-resident-clients-checkpassed=1&new-commissioner-report-checkpassed=1&commission-categories=%5B%5D&' +
                                 'client-limits=%5B%5D&loyal-clients=%5B%5D&brand-clients=%5B%5D&discard-media-discount-checkpassed=1&is-booked-checkpassed=1&' +
                                 'is-faxed-checkpassed=1&is-signed=&is-signed-checkpassed=1&is-signed-date=14+%D0%BE%D0%BA%D1%82+2014+%D0%B3.&' +
                                 'is-signed-dt=2014-10-14T00%3A00%3A00&deal-passport-checkpassed=1&sent-dt-checkpassed=1&is-suspended-checkpassed=1&' +
                                 'is-cancelled-checkpassed=1&button-submit=%D0%A1%D0%BE%D1%85%D1%80%D0%B0%D0%BD%D0%B8%D1%82%D1%8C&collateral-form=&id=',
                                 True)
    ######   Турция: оптовый агентский (предоплата)
    if contract_type == 'tr':
        ttt = urlparse.parse_qsl('external-id=&num=&commission=25&print-form-type=0&brand-type=70&client-id=' + str(
            client_id) + '&person-id=' + str(
            person_id) + '&account-type=0&manager-code=20453&manager-bo-code=&dt=2013-09-18T00%3A00%3A00&finish-dt=2019-12-25T00%3A00%3A00&payment-type=2&unilateral=0&services=1&services-' + str(
            service_id) + '=' + str(
            service_id) + '&memo=&atypical-conditions-checkpassed=1&calc-termination=&attorney-agency-id=&commission-charge-type=1&commission-payback-type=2&commission-payback-pct=&commission-type=&supercommission-bonus=1&partner-commission-type=1&partner-commission-pct=&named-client-declared-sum=&commission-declared-sum=&supercommission=0&partner-commission-sum=&linked-contracts=1&limitlinked-contracts=&partner-min-commission-sum=&advance-payment-sum=&discard-nds=0&discount-policy-type=0&discount-fixed=12&declared-sum=&fixed-discount-pct=&belarus-budget-price=&ukr-budget=&budget-discount-pct=&federal-declared-budget=&kz-budget=&federal-annual-program-budget=&federal-budget=&kzt-budget=&belarus-budget=&year-product-discount=&year-planning-discount=&year-planning-discount-custom=&use-ua-cons-discount-checkpassed=1&consolidated-discount=&use-consolidated-discount-checkpassed=1&regional-budget=&use-regional-cons-discount-checkpassed=1&pda-budget=&contract-discount=&retro-discount=&discount-pct=&discount-findt=&credit-type=0&payment-term=&payment-term-max=&calc-defermant=0&personal-account-checkpassed=1&lift-credit-on-payment-checkpassed=1&auto-credit-checkpassed=1&personal-account-fictive-checkpassed=1&repayment-on-consume-checkpassed=1&credit-currency-limit=810&limitcredit-currency-limit=&credit-limit=17&limitcredit-limit=&turnover-forecast=17&limitturnover-forecast=&credit-limit-single=&partner-credit-checkpassed=1&discount-commission=&pp-1137-checkpassed=1&non-resident-clients-checkpassed=1&new-commissioner-report-checkpassed=1&service-min-cost=&test-period-duration=&commission-categories=%5B%5D&client-limits=%5B%5D&brand-clients=%5B%5D&loyal-clients=%5B%5D&discard-media-discount-checkpassed=1&is-booked-checkpassed=1&is-faxed-checkpassed=1&is-signed=&is-signed-checkpassed=1&is-signed-date=19+%D0%B4%D0%B5%D0%BA+2014+%D0%B3.&is-signed-dt=2014-12-19T00%3A00%3A00&deal-passport-checkpassed=1&sent-dt-checkpassed=1&is-suspended-checkpassed=1&is-cancelled-checkpassed=1&button-submit=%D0%A1%D0%BE%D1%85%D1%80%D0%B0%D0%BD%D0%B8%D1%82%D1%8C&collateral-form=&id=',
                                 True)
    ######     Комиссионный агентский
    if contract_type == 'commiss':
        ttt = urlparse.parse_qsl('external-id=&num=&commission=1&print-form-type=0&brand-type=70&client-id=' + str(
            client_id) + '&person-id=' + str(
            person_id) + '&account-type=0&bank-details-id=21&manager-code=20453&manager-bo-code=&dt=2014-02-12T00%3A00%3A00&finish-dt=2016-12-28T00%3A00%3A00&payment-type=2&unilateral=1&services=1&services-7=7&memo=&atypical-conditions-checkpassed=1&calc-termination=&attorney-agency-id=&commission-charge-type=1&commission-payback-type=2&commission-payback-pct=&commission-type=47&supercommission-bonus=1&partner-commission-type=1&partner-commission-pct=&named-client-declared-sum=&commission-declared-sum=&supercommission=0&partner-commission-sum=&linked-contracts=1&limitlinked-contracts=&partner-min-commission-sum=&advance-payment-sum=&discard-nds=0&discount-policy-type=0&discount-fixed=12&declared-sum=&fixed-discount-pct=&belarus-budget-price=&ukr-budget=&budget-discount-pct=&federal-declared-budget=&kz-budget=&federal-annual-program-budget=&federal-budget=&kzt-budget=&belarus-budget=&year-product-discount=&year-planning-discount=&year-planning-discount-custom=&use-ua-cons-discount-checkpassed=1&consolidated-discount=&use-consolidated-discount-checkpassed=1&regional-budget=&use-regional-cons-discount-checkpassed=1&pda-budget=&contract-discount=&retro-discount=&discount-pct=&discount-findt=&credit-type=0&payment-term=&payment-term-max=&calc-defermant=0&personal-account-checkpassed=1&lift-credit-on-payment-checkpassed=1&auto-credit-checkpassed=1&personal-account-fictive-checkpassed=1&repayment-on-consume-checkpassed=1&credit-currency-limit=810&limitcredit-currency-limit=&credit-limit=17&limitcredit-limit=&turnover-forecast=17&limitturnover-forecast=&credit-limit-single=&partner-credit-checkpassed=1&discount-commission=&pp-1137-checkpassed=1&non-resident-clients-checkpassed=1&new-commissioner-report-checkpassed=1&service-min-cost=&test-period-duration=&commission-categories=%5B%5D&client-limits=%5B%5D&brand-clients=%5B%5D&loyal-clients=%5B%5D&discard-media-discount-checkpassed=1&is-booked-checkpassed=1&is-faxed-checkpassed=1&is-signed=&is-signed-checkpassed=1&is-signed-date=23+%D0%B4%D0%B5%D0%BA+2014+%D0%B3.&is-signed-dt=2014-12-23T00%3A00%3A00&deal-passport-checkpassed=1&sent-dt-checkpassed=1&is-suspended-checkpassed=1&is-cancelled-checkpassed=1&button-submit=%D0%A1%D0%BE%D1%85%D1%80%D0%B0%D0%BD%D0%B8%D1%82%D1%8C&collateral-form=&id=',
                                 True)
    con = tm.Balance.CreateContract('16571028', {k: v.decode('utf-8') for k, v in ttt})
    ##    print 'Contract_id: %s (external_id: %s)' % (con['ID'], con['EXTERNAL_ID'])
    return con['ID']


##### не актуально
def create_invoice_in_oferdraft(uid, service_id, service_order_id, qty, paysys_id, overdraft):
    tm, test = proxy()
    invoice = tm.Balance.CreateFastInvoice(
        {'login': uid, 'service_id': service_id, 'service_order_id': service_order_id, 'qty': qty,
         'paysys_id': paysys_id, 'overdraft': overdraft})['invoice_id']
    ##    print 'Invoice: https://balance-admin.greed-tm1f.yandex.ru/invoice.xml?invoice_id=' + str(invoice)
    query = "select external_id, total_sum, consume_sum from t_invoice where id = :invoice_id"
    external_id = test.ExecuteSQL('balance', query, {'invoice_id': invoice})[0]['external_id']
    return invoice


def oebs_export(export_type, object_id):
    tm, test = proxy()
    test.ExecuteSQL('balance', 
        'update (select  * from t_export where  CLASSNAME = :export_type and   type=\'OEBS\' and OBJECT_ID = :object_id ) set priority = -1',
        {'export_type': export_type, 'object_id': object_id})
    state = 0
    print str(export_type) + ' export begin:' + str(datetime.datetime.now())
    while True:
        if state == 0:
            state = test.ExecuteSQL('balance', 
                'select  state from t_export where  CLASSNAME = :export_type and   type=\'OEBS\' and OBJECT_ID = :object_id ',
                {'export_type': export_type, 'object_id': object_id})[0]['state']
            ##            print state
            time.sleep(3)
        else:
            print str(export_type) + ' export   end:' + str(datetime.datetime.now())
            break
