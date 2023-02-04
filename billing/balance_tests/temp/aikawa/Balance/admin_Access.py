# -*- coding: utf-8 -*-
__author__ = 'aikawa'
# casper.test.begin('pre/auth#Passport works', 1,1 function suite(test) {    casper.start('https://passport.yandex.ru/auth', function () {        this.fill('form', {            'login': '{login}',            'passwd': '{pass}'        }, true);    });    casper.then(function () {        this.wait(2000, function () {            test.assertSelectorHasText('h1', 'Персональные данные');        });    });    casper.run(function () {        test.done();    });});

import datetime

from balance import balance_steps as steps

PERSON_TYPE = 'ur'
PAYSYS_ID = 1003
SERVICE_ID = 7
PRODUCT_ID = 1475
MSR = 'Bucks'

dt = datetime.datetime.now()
previous_day_dt = (dt - datetime.timedelta(days=1)).replace(hour=0, minute=0, second= 0, microsecond=0)
print previous_day_dt



client_id = steps.ClientSteps.create()
person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID, service_order_id=service_order_id)
orders_list = [{'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt}]
request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
invoice_id,_,_= steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID, credit=0, contract_id=None, overdraft=0,endbuyer_id=None)
steps.InvoiceSteps.pay(invoice_id)
print steps.InvoiceSteps.change_dt_ai(invoice_id, previous_day_dt)
print steps.InvoiceSteps.make_rollback_ai(invoice_id, order_id=order_id)
