# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime
import pprint
import time

import pytest
import requests as req
from hamcrest import equal_to

import balance.balance_db as db
import btestlib.reporter as reporter
from balance import balance_api as api
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.data import defaults
from btestlib.data import simpleapi_defaults

# s = xmlrpclib.ServerProxy('http://greed-ts1f.yandex.ru:8018/simple/xmlrpc', allow_none=1, use_datetime=1)
PRICE = 100
COMMISSION_PCT = 0.01
CONTRACT_START_DT = datetime.datetime.now() - datetime.timedelta(days=31)
ORDER_TEXT = str(datetime.datetime.today().strftime("%d%m%Y%H%M%S"))

SHIP_TOKEN = 'dostavka_pay_af2643478f5d38263ad745125cd46c00'
SHIP_SERVICE_PRODUCT = '101'

###########################

service_token = SHIP_TOKEN
service_product_id = SHIP_SERVICE_PRODUCT


def Print(obj):
    print pprint.pformat(obj).decode('unicode_escape')


def check_basket(trust_payment_id):
    result = api.simple().server.BalanceSimple.CheckBasket(service_token,
                                                           {'trust_payment_id': trust_payment_id,
                                                            'user_ip': '127.0.0.1'})
    return result


def create_refund(purchase_token, trust_payment_id, order_id, sum):
    Refund = api.simple().server.BalanceSimple.CreateRefund(service_token,
                                                            {'purchase_token': purchase_token, 'user_ip': '127.0.0.1',
                                                             'reason_desc': 'test1',
                                                             'trust_payment_id': trust_payment_id, 'orders': [
                                                                {'service_order_id': order_id, 'delta_amount': sum}]})
    return Refund


def do_refund(purchase_token, trust_refund_id):
    status = api.simple().server.BalanceSimple.DoRefund(service_token,
                                                        {'purchase_token': purchase_token, 'user_ip': '127.0.0.1',
                                                         'trust_refund_id': trust_refund_id})
    return status


def method_tickets():
    client_id = steps.CommonSteps.log(api.simple().server.BalanceSimple.CreatePartner)(service_token,
                                                                                       {'name': 'Py_test',
                                                                                        'operator_uid': defaults.PASSPORT_UID})

    if client_id['status'] <> 'success':
        raise Exception(
            "CreatePartner: failed")
    client_id = int(client_id['partner_id'])
    # client_id =9401140
    service_product_id = 'Taxi_test_' + str(client_id)
    print 'service_product_id: %s' % service_product_id
    product = steps.CommonSteps.log(api.simple().server.BalanceSimple.CreateServiceProduct)(service_token,
                                                                                            {
                                                                                                'prices': simpleapi_defaults.product_prices(),
                                                                                                'service_product_id': service_product_id,
                                                                                                'name': simpleapi_defaults.PRODUCT_NAME,
                                                                                                'partner_id': client_id
                                                                                            })
    if product['status'] <> 'success':
        raise Exception(
            "CreateServiceProduct: failed")
    person_id = steps.PersonSteps.create(client_id, 'ur', {'kpp': '234567890'})
    contract_id = steps.ContractSteps.create_contract('no_agency', {'CLIENT_ID': client_id, 'PERSON_ID': person_id,
                                                                    'DT': CONTRACT_START_DT, 'FIRM': 1
        , 'SERVICES': [101, 120]})[0]
    print contract_id
    db.balance().execute(
        'update (select * from T_CONTRACT_COLLATERAL c join T_CONTRACT_ATTRIBUTES a on c.id = a.COLLATERAL_ID where c.CONTRACT2_ID = :contract_id and a.code = \'MINIMAL_PAYMENT_COMMISSION\') set value_num = 1',
        {'contract_id': contract_id})
    Order = api.simple().CreateOrderOrSubscription(service_token,
                                                                          {'uid': '167324860', 'user_ip': '127.0.0.1',
                                                                           'region_id': 225,
                                                                           'service_product_id': service_product_id,
                                                                           'service_order_id': ORDER_TEXT})
    print Order
    order_id = Order['service_order_id']
    print 'order_id: %s' % order_id

    Basket = api.simple().server.BalanceSimple.CreateBasket(service_token,
                                                                                   {'user_ip': '95.108.174.132',
                                                                                    'currency': 'RUB', 'orders': [
                                                                                       {'service_order_id': ORDER_TEXT,
                                                                                        'price': PRICE}],
                                                                                    'paymethod_id': 'cash-2237685'})
    print Basket

    purchase_token = Basket['purchase_token']
    print 'purchase_token: %s' % purchase_token
    trust_payment_id = Basket['trust_payment_id']
    print 'trust_payment_id: %s' % trust_payment_id

    basket = api.simple().server.BalanceSimple.PayBasket(service_token, {'purchase_token': purchase_token,
                                                                         'trust_payment_id': trust_payment_id,
                                                                         'user_ip': '127.0.0.1',
                                                                         'orders': [
                                                                             {'delta_amount': PRICE, 'price': PRICE,
                                                                              'service_order_id': ORDER_TEXT}]})
    print '333'
    # print basket
    # if service_token == TAXIFEE_TOKEN:
    # print service_token, trust_payment_id, order_id, PRICE
    #     q = api.simple().server.BalanceSimple.UpdateBasket(service_token, {'trust_payment_id': trust_payment_id, 'orders': [{'service_order_id': order_id,
    #                                                                                                     'amount': PRICE, 'action':'clear'}]})
    #     print q



    r2 = req.post('https://tmongo1f.yandex.ru/web/start_payment',
                  data={'card_number': '5555555555554444', 'payment_method': 'new_card',
                        'purchase_token': purchase_token, 'expiration_month': '04', 'expiration_year': '2017',
                        'cardholder': 'TEST', 'cvn': '874'}, verify=False)
    print r2.text
    print '%s?purchase_token=%s'
    print '5555 5555 5555 4444; Any date; CVN: 123; Any name; email'
    print 'check_basket:'
    print (check_basket(str(trust_payment_id)))

    result = []
    while len(result) == 0:
        result = db.balance().execute(
            'select id from t_payment where id = (select id from t_ccard_bound_payment where trust_payment_id = :trust_payment_id)',
            {'trust_payment_id': trust_payment_id})
        time.sleep(2)
        print 'waiting for payment creation'
    payment_id = result[0]['id']
    print 'payment_id=%s' % payment_id

    ##post-authorisation for taxi
    # if service_token == TAXIFEE_TOKEN:
    #     print service_token, trust_payment_id, order_id, PRICE
    #     q= api.simple().server.BalanceSimple.UpdateBasket(service_token, {'trust_payment_id': trust_payment_id, 'orders': [{'service_order_id': order_id,
    #                                                                                                     'amount': PRICE, 'action':'clear'}]})
    #
    #     print q
    #
    # if service_token == EVENTS_TOKEN:
    #     print service_token, trust_payment_id, order_id, PRICE
    #     q= api.simple().server.BalanceSimple.UpdateBasket(service_token, {'trust_payment_id': trust_payment_id, 'orders': [{'service_order_id': order_id,
    #                                                                                                     'amount': PRICE, 'action':'clear'}]})
    #     print q



    postauth_dt = None
    while postauth_dt is None:
        postauth_dt = \
            db.balance().execute(
                'select postauth_dt from t_ccard_bound_payment where trust_payment_id = :trust_payment_id',
                {'trust_payment_id': trust_payment_id})[0]['postauth_dt']
        time.sleep(2)
    print 'postauth_dt=%s' % postauth_dt

    return trust_payment_id


def refund(order_id, purchase_token, trust_payment_id):
    refund = create_refund(purchase_token, trust_payment_id, order_id, 5)
    print  refund
    print refund['trust_refund_id']
    do_refund(purchase_token, refund['trust_refund_id'])


@pytest.mark.slow
@pytest.mark.priority('low')
@reporter.feature(Features.TRUST, Features.PAYMENT)
@pytest.mark.tickets('BALANCE-21203')
def test_3rdparty_trans_tickets():
    trust_payment_id = method_tickets()
    print trust_payment_id
    payment_id = steps.SimpleApi.create_register([trust_payment_id], 0)
    db.balance().execute(
        'update (select * from t_export where object_id = :payment_id and type = \'THIRDPARTY_TRANS\') set priority=-1',
        {'payment_id': payment_id})
    state = 0
    print 'export is in progress'
    while state == 0:
        state = \
            db.balance().execute(
                'select state from t_export where object_id = :payment_id and type = \'THIRDPARTY_TRANS\'',
                {'payment_id': payment_id})[0]['state']
        time.sleep(2)
    else:
        print 'DONE! select * from t_thirdparty_transactions where payment_id  =%s' % payment_id
    result = \
        db.balance().execute('select count(*) as result from t_thirdparty_transactions where payment_id  =:payment_id',
                             {'payment_id': payment_id})[0]['result']
    utils.check_that(1, result, 'Проверяем, появился ли платеж')
    paysys_partner_id = db.balance().execute('select  paysys_partner_id from V_PAYMENT_TRUST where id =:payment_id',
                                             {'payment_id': payment_id})[0]['paysys_partner_id']
    expected_template =  {'paysys_partner_id': paysys_partner_id,
                         'yandex_reward': 1}
    payment_data = db.balance().execute(
        'select PAYSYS_PARTNER_ID, YANDEX_REWARD from t_thirdparty_transactions where payment_id =:payment_id',
        {'payment_id': payment_id})[0]
    utils.check_that(payment_data, equal_to(expected_template), 'Сравниваем данные')


if __name__ == "__main__":
    test_3rdparty_trans_tickets()
    pytest.main("test_3rdparty_trans_tickets.py -v")

    # print create_refund('9de99b400306bf6da2ef0182b8f41811', '56c19443795be217054c9dd1', '15022016120246', 10)
    # print do_refund('9de99b400306bf6da2ef0182b8f41811', '56c19ed8795be26db64c9a82')
    # reg(['56c19ed8795be26db64c9a82'],1)