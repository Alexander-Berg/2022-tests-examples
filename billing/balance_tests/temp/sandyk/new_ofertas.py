# -*- coding: utf-8 -*-

import datetime

import balance.balance_steps as steps
from btestlib import local_config as conf

auto_prefix = '[MT]: '

SERVICE_ID = 7
PRODUCT_ID = 1475
QTY = 100
PAYSYS_ID = 1003
MULTI_DT = datetime.datetime.now() - datetime.timedelta(days=1)
DT = datetime.datetime.now()


def data_generator(paysys_id, person_type, dt=datetime.datetime.now(), orders=['7-1475'], multi=None,
                   mode='paypreview'):
    client_id = None or steps.ClientSteps.create({'IS_AGENCY': 0})
    person_id = None or steps.PersonSteps.create(client_id, person_type)
    region_id = {'RUB': 225,
                 'UAH': 187}
    if multi:
        steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='MODIFY',
                                              region_id=region_id[multi])

    # ---------- Process an 'orders' = ['99-111', '7-1475', '7-1475'] structure ------------
    orders_list = []
    for item in orders:
        service_id = int(item.split('-')[0])
        product_id = item.split('-')[1]
        service_order_id = steps.OrderSteps.next_id(service_id)
        order_id = steps.OrderSteps.create(client_id, service_order_id, product_id, service_id,
                                           {'TEXT': 'Py_Test order', 'AgencyID': None, 'ManagerUID': None})
        orders_list = [
            {'ServiceID': 7, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': dt}
        ]

    # ---------- Generate a target url ------------
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params={'InvoiceDesireDT': dt})
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    env = conf.ENVIRONMENT_SETTINGS['balance']
    page = {
        'paypreview': "{0}https://balance.greed-{1}.yandex.ru/paypreview.xml?person_id={2}&request_id={3}&paysys_id={4}&contract_id=&coupon=&mode=ci",
        'print_form': "{0}https://balance-admin.greed-{1}.yandex.ru/invoice-publish.xml?ft=html&object_id={5}",
        'invoice': "{0}https://balance.greed-{1}.yandex.ru/invoice.xml?invoice_id={5}"}
    print (page[mode].format(auto_prefix, env, person_id, request_id, paysys_id,
                             invoice_id) + '&mt-login=yb-adm&mt-password=get_secret(*UsersPwd.YANDEX_TEAM_REG_CQR5_PWD)')
