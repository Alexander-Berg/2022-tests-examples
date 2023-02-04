# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import pprint
import time
import xmlrpclib

from selenium import webdriver

import balance.balance_db as db

PRICE = 100
COMMISSION_PCT = 0.01
SIMPLE = xmlrpclib.ServerProxy('http://greed-tm1f.yandex.ru:8018/simple/xmlrpc')

EVENTS_TOKEN = 'events_tickets_923c891a6334317f77c44d6733b69519'
EVENTS_SERVICE_PRODUCT = 'fee_1433926694867'

###########################

service_token = EVENTS_TOKEN
service_product_id = EVENTS_SERVICE_PRODUCT
service_product_id2 = 'product_1433926694651'

def Print(obj):
    print pprint.pformat(obj).decode('unicode_escape')


def check_basket(trust_payment_id):
    result = SIMPLE.BalanceSimple.CheckBasket(service_token,
                                              {'trust_payment_id': trust_payment_id, 'user_ip': '127.0.0.1'})
    return result


def create_refund(purchase_token, trust_payment_id, order_id, sum):
    Refund = SIMPLE.BalanceSimple.CreateRefund(service_token, {'purchase_token': purchase_token, 'user_ip': '127.0.0.1',
                                                               'reason_desc': 'test1',
                                                               'trust_payment_id': trust_payment_id, 'orders': [{'service_order_id': order_id, 'delta_amount': sum}]})
    return Refund


def do_refund(purchase_token, trust_refund_id):
    status = SIMPLE.BalanceSimple.DoRefund(service_token, {'purchase_token': purchase_token, 'user_ip': '127.0.0.1',
                                                           'trust_refund_id': trust_refund_id})
    return status


def reg(trust_payment_ids,is_refund):
    payment_list = []
    for item in trust_payment_ids:
        if is_refund == 0:
            #payment
            sql1 = 'select p.id, p.amount, vpt.trust_payment_id from t_payment p join v_payment_trust vpt on p.id = vpt.id and vpt.trust_payment_id in (:trust_payment_ids)'
        if is_refund == 1:
            #refund
            sql1 = 'select p.id, p.amount, vpt.trust_refund_id from t_payment p join v_payment_refund vpt on p.id = vpt.id and vpt.trust_refund_id in (:trust_payment_ids)'
        sql1_params = {'trust_payment_ids': item}
        a = db.balance().execute(sql1, sql1_params)
        print sql1, sql1_params
        payment_list.append(db.balance().execute(sql1, sql1_params)[0])
    register_amount = 0
    register_commission = 0
    for item in payment_list:
        register_amount += float(item['amount'])
        item['commission'] = round(float(item['amount']) * COMMISSION_PCT, 2)
        register_commission += item['commission']

    print('DONE: list')

    status = db.balance().execute(
        '''Insert into t_payment_register (ID,DT,PAYSYS_CODE,REGISTER_DT,AMOUNT,COMMISSION,FILE_NAME,INCOMING_MAIL_ID) values (s_payment_register_id.nextval,sysdate,'TRUST',trunc(sysdate),:register_amount,:commission,null,null)''',
        {'register_amount': register_amount, 'commission': register_commission})

    db.balance().execute('commit')
    print('DONE: register')

    register_id = \
        db.balance().execute('select * from (select * from t_payment_register order by dt desc) where rownum = 1')[0][
            'id']

    for payment in payment_list:
        status = db.balance().execute(
            "Insert into t_payment_register_line (ID,REGISTER_ID,PAYSYS_CODE,PAYMENT_DT,AMOUNT,COMMISSION) values (s_payment_register_line_id.nextval,:register_id,'TRUST',sysdate,:amount,:commission)",
            {'register_id': register_id, 'amount': payment['amount'], 'commission': payment['commission']})
    db.balance().execute('commit')
    print('DONE: lines')

    register_line_list = db.balance().execute(
        "select id, amount from t_payment_register_line where register_id = :register_id", {'register_id': register_id})
    print (register_line_list)

    for item in payment_list:
        for line in register_line_list:
            if item['amount'] == line['amount']:
                item['line_id'] = line['id']
                register_line_list.remove(line)
                break

    print payment_list

    for item in payment_list:
        db.balance().execute(
            "update t_payment set register_id = :register_id, register_line_id = :register_line_id where id = (select id from t_ccard_bound_payment where trust_payment_id = :trust_payment_id)",
            {'register_id': register_id, 'register_line_id': item['line_id'],
             'trust_payment_id': item['trust_payment_id']})
    db.balance().execute('commit')
    print('DONE: payments')

    print register_id
    return payment_list[0]['id']

def test_method_tickets():
    Order = SIMPLE.BalanceSimple.CreateOrderOrSubscription(service_token, {'uid': '167324860', 'user_ip': '127.0.0.1',
                                                                           'region_id': 225,
                                                                           'service_product_id': service_product_id})
    Order2 = SIMPLE.BalanceSimple.CreateOrderOrSubscription(service_token, {'uid': '167324860', 'user_ip': '127.0.0.1',
                                                                           'region_id': 225,
                                                                           'service_product_id': service_product_id2})

    order_id = Order['service_order_id']
    order_id2 = Order2['service_order_id']
    print 'order_id: %s' % order_id
    print 'order_id2: %s' % order_id2

    Basket = SIMPLE.BalanceSimple.CreateBasket(service_token, {'user_ip': '127.0.0.1', 'currency': 'RUB', 'orders': [
        {'service_order_id': order_id, 'price': PRICE},{'service_order_id': order_id2, 'price': 10}]})
    print Basket

    purchase_token = Basket['purchase_token']
    print 'purchase_token: %s' % purchase_token
    trust_payment_id = Basket['trust_payment_id']
    print 'trust_payment_id: %s' % trust_payment_id

    PaymentForm = SIMPLE.BalanceSimple.PayBasket(service_token, {'purchase_token': purchase_token,
                                                                 'trust_payment_id': trust_payment_id,
                                                                 'back_url': 'http://balance-dev.yandex.ru',
                                                                 'user_ip': '127.0.0.1',
                                                                 'paymethod_id': 'trust_web_page',
                                                                 'return_path': 'http://ya.ru', })
    #filling form, payment creation
    browser = webdriver.Firefox()
    url = '%s?purchase_token=%s' % (
    PaymentForm['payment_form']['_TARGET'], PaymentForm['payment_form']['purchase_token']) + '&external_service_id=118'
    browser.get(url)
    time.sleep(3)
    browser.find_element_by_name("cnumber0").send_keys("5555")
    browser.find_element_by_name("cnumber1").send_keys("5555")
    browser.find_element_by_name("cnumber2").send_keys("5555")
    browser.find_element_by_name("cnumber3").send_keys("4444")

    browser.find_element_by_name("expiration_month").send_keys("12")
    browser.find_element_by_name("expiration_year").send_keys("17")
    browser.find_element_by_name("cardholder").send_keys("test")
    browser.find_element_by_name("mailto").send_keys("test-balance-notify@yandex-team.ru")
    browser.find_element_by_name("cvn").send_keys("123")
    time.sleep(2)
    browser.find_element_by_xpath("//b[@class=\"action-button js-submit-button\"]").click()
    time.sleep(5)
    browser.close()
    time.sleep(2)
    print '%s?purchase_token=%s' % (
    PaymentForm['payment_form']['_TARGET'], PaymentForm['payment_form']['purchase_token'])
    print '5555 5555 5555 4444; Any date; CVN: 123; Any name; email'

    Print(check_basket(str(trust_payment_id)))

    result = []
    while len(result) == 0:
        result = db.balance().execute(
            'select id from t_payment where id = (select id from t_ccard_bound_payment where trust_payment_id = :trust_payment_id)',
            {'trust_payment_id': trust_payment_id})
        time.sleep(2)
        print 'waiting for payment creation'
    payment_id= result[0]['id']
    print 'payment_id=%s'%payment_id

    postauth_dt = None
    while postauth_dt is None:
        postauth_dt = \
            db.balance().execute(
                'select postauth_dt from t_ccard_bound_payment where trust_payment_id = :trust_payment_id',
                {'trust_payment_id': trust_payment_id})[0]['postauth_dt']
        time.sleep(2)
    print 'postauth_dt=%s'%postauth_dt

    ##post-authorisation for taxi
    if service_token == EVENTS_TOKEN:
        print '111'
        print service_token, trust_payment_id, order_id, PRICE
        q= SIMPLE.BalanceSimple.UpdateBasket(service_token, {'trust_payment_id': trust_payment_id, 'orders': [{'service_order_id': order_id,
                                                                                                        'amount': PRICE, 'action':'clear'},
                                                                                                              {'service_order_id': order_id2,
                                                                                                       'amount': 10, 'action':'clear'}]})
        print q

    return trust_payment_id

def refund (order_id,purchase_token,trust_payment_id):

    refund = create_refund(purchase_token, trust_payment_id, order_id, 60)
    print  refund
    print refund['trust_refund_id']
    do_refund(purchase_token,refund['trust_refund_id'])

def qwe():
    trust_payment_id = test_method_tickets()
    print trust_payment_id
    payment_id=reg([trust_payment_id],0);
    db.balance().execute(
        'update (select * from t_export where object_id = :payment_id and type = \'THIRDPARTY_TRANS\') set priority=-1',
        {'payment_id': payment_id})
    state =0
    print 'export is in progress'
    while state == 0 :
        state = \
            db.balance().execute(
                'select state from t_export where object_id = :payment_id and type = \'THIRDPARTY_TRANS\'',
                {'payment_id': payment_id})[0]['state']
        time.sleep(2)
    else: print 'DONE! select * from t_thirdparty_transactions where payment_id  =%s'%payment_id
    rows = \
        db.balance().execute('select count(*) as result from t_thirdparty_transactions where payment_id  =:payment_id',
                             {'payment_id': payment_id})[0]['result']
    reward = db.balance().execute(
        'select yandex_reward from t_thirdparty_transactions where payment_id  =:payment_id and amount_fee>0',
        {'payment_id': payment_id})
    assert rows == 2, 'Check the payment %s. It should be 2 rows in the table'%payment_id
    assert reward is None, 'Check yandex_reward for payment %s'%payment_id


qwe ()
