# -*- coding: cp1251 -*-
import xmlrpclib
import pprint
import webbrowser
import os
import subprocess
import datetime


def Print(obj):
    print pprint.pformat(obj).decode('unicode_escape')

TM = "http://greed-tm1f.yandex.ru:8002/simpleapi/xmlrpc"
TS = "http://greed-tm1f.yandex.ru:8002/xmlrpc"
TEST = 'http://xmlrpc.balance.greed-tm1f.yandex.ru:30702/xmlrpc'

XMLRPC_URL = TM
XMLRPC_URL_1 = TEST

proxy = xmlrpclib.ServerProxy(XMLRPC_URL, allow_none=1, use_datetime=1)
proxy1 = xmlrpclib.ServerProxy(XMLRPC_URL_1, allow_none=1, use_datetime=1)

amount = 800
commission = 2,28
is_anonim = 0
com_cat = 350
uid = '3000338162'
##4000160841   clientuid125
##3000338162	trusttestusr4 (яд)


def test_client():

    if  is_anonim == 1:
        COS = proxy.BalanceSimple.CreateOrderOrSubscription('tickets_f4ac4122ee48c213eec816f4d7944ea6',
                                                  {'user_ip': '127.0.0.1','region_id': 225,'service_product_id': '1408970943560',
                                                   'commission_category':com_cat})
        purchase_token = COS['purchase_token']
        service_order_id = COS['service_order_id']

        CB = proxy.BalanceSimple.CreateBasket('tickets_f4ac4122ee48c213eec816f4d7944ea6', {'purchase_token': purchase_token,'user_ip': '127.0.0.1',
        'currency': 'RUB', 'orders': [{'service_order_id': service_order_id, 'price': amount}]})
        trust_payment_id = CB['trust_payment_id']

        PB = proxy.BalanceSimple.PayBasket('tickets_f4ac4122ee48c213eec816f4d7944ea6', {'purchase_token': purchase_token,
        'trust_payment_id': trust_payment_id,'back_url': 'http://balance-dev.yandex.ru','user_ip': '127.0.0.1',
        'paymethod_id': 'trust_web_page','return_path': 'http://ya.ru'})


        print 'trust_payment_id=%s' % trust_payment_id
        print 'service_order_id=%s' % service_order_id
        print 'purchase_token=%s' % purchase_token
        print 'https://tmongo1f.yandex.ru/web/payment?purchase_token=%s' % purchase_token

    else:

        COS = proxy.BalanceSimple.CreateOrderOrSubscription('tickets_f4ac4122ee48c213eec816f4d7944ea6',
                                                  {'uid':uid,'user_ip': '127.0.0.1','region_id': 225,'service_product_id': '1408970943560','commission_category':com_cat})
        service_order_id = COS['service_order_id']

        CB = proxy.BalanceSimple.CreateBasket('tickets_f4ac4122ee48c213eec816f4d7944ea6', {'uid':uid,'user_ip': '127.0.0.1',
        'currency': 'RUB', 'orders': [{'service_order_id': service_order_id, 'price': amount}]})
        trust_payment_id = CB['trust_payment_id']

        PB = proxy.BalanceSimple.PayBasket('tickets_f4ac4122ee48c213eec816f4d7944ea6', {'uid':uid,
        'trust_payment_id': trust_payment_id,'back_url': 'http://balance-dev.yandex.ru','user_ip': '127.0.0.1',
        'paymethod_id': 'trust_web_page','return_path': 'http://ya.ru'})

        print 'trust_payment_id=%s' % trust_payment_id
        print 'service_order_id=%s' % service_order_id
        print 'https://tmongo1f.yandex.ru/web/payment?trust_payment_id=%s' % trust_payment_id

test_client()
