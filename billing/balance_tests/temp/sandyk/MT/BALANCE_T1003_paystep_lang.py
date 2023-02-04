# -*- coding: utf-8 -*-

import datetime

from balance import balance_steps as steps

SERVICE_ID = 11
PRODUCT_ID = 506525
QTY = 1000
BASE_DT = datetime.datetime.now()

def test_method():

    client_id = steps.ClientSteps.create()
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    steps.OrderSteps.create(client_id, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID)
    orders_list = [
    {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}]
    request_id = steps.RequestSteps.create(client_id, orders_list)

    print request_id
    return '[MT]: http://balance.greed-tm1f.yandex.ru/paypreview.xml?request_id=%s&mt-login=yb-adm&mt-password=get_secret(*UsersPwd.YANDEX_TEAM_REG_CQR5_PWD)&LANG=en' % request_id
if __name__ == "__main__":
    print test_method()


