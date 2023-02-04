# coding=utf-8
import datetime
import decimal

import pytest
from hamcrest import equal_to

from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils

# Значения меток по-умолчанию
pytestmark = [pytest.mark.priority('mid')  # Параметризированная метка
    , pytest.mark.slow  # Непараметризированная метка
              ]
slow = pytest.mark.slow  # Для краткости можно и так

SERVICE_ID_DIRECT = 7
PRODUCT_ID_DIRECT = 503162

SERVICE_ID_MARKET = 11
PRODUCT_ID_MARKET = 2136

PAYSYS_ID = 1003
QTY = 55.7
BASE_DT = datetime.datetime.now()


# передаем сюда клиента, ид сервиса и заказа, список и количество заказов, которое хотим добавить к нему(n)
@pytest.fixture
def append_orders_list(service_id, client_id, n=1, orders_list=[]):
    for i in range(n):
        product_id = PRODUCT_ID_DIRECT
        service_order_id = steps.OrderSteps.next_id(service_id)
        steps.OrderSteps.create(client_id, service_order_id, service_id=service_id, product_id=product_id)
        list1 = {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}
        orders_list.append(list1)
    print (orders_list)
    return orders_list


@slow
def test_1():
    client_id = None or steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')
    steps.ClientSteps.link(client_id, 'chihiro-test-0')

    service_id = SERVICE_ID_DIRECT

    contract_id = None

    orders_list = append_orders_list(service_id, client_id)

    request_id = steps.RequestSteps.create(client_id, orders_list)

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id)
    campaigns_params = {'Bucks': 0, 'Money': 23.41}
    steps.CampaignsSteps.do_campaigns(service_id, orders_list[0]['ServiceOrderID'], campaigns_params, 0, BASE_DT)
    steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)
    sumact1 = decimal.Decimal(str(campaigns_params.get('Bucks'))) + decimal.Decimal(str(campaigns_params.get('Money')))

    # sumact2 = "select amount from t_act where invoice_id = :invoice_id"
    # query_params = {'invoice_id': invoice_id}
    # result = db.balance().execute(sumact2, query_params)
    # print(result)
    # assert sumact1 == decimal.Decimal(str(result[0]['amount']))
    act = db.get_acts_by_client(client_id)[0]
    utils.check_that(act['amount'], equal_to('23.41'))


@slow
def test_2():
    client_id = None or steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')
    steps.ClientSteps.link(client_id, 'chihiro-test-0')

    service_id = SERVICE_ID_DIRECT

    contract_id = None

    orders_list = append_orders_list(service_id, client_id)

    request_id = steps.RequestSteps.create(client_id, orders_list)

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id)
    sum1 = 0
    campaigns_params = {'Bucks': 0, 'Money': 23.41}
    steps.CampaignsSteps.do_campaigns(service_id, orders_list[1]['ServiceOrderID'], campaigns_params, 0, BASE_DT)
    steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)
    sum1 = sum1 + decimal.Decimal(str(campaigns_params.get('Bucks'))) + decimal.Decimal(
        str(campaigns_params.get('Money')))

    campaigns_params = {'Bucks': 0, 'Money': 20}
    steps.CampaignsSteps.do_campaigns(service_id, orders_list[1]['ServiceOrderID'], campaigns_params, 0, BASE_DT)
    steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)
    sum1 = decimal.Decimal(str(campaigns_params.get('Bucks'))) + decimal.Decimal(str(campaigns_params.get('Money')))

    sumact2 = "select amount from t_act where invoice_id = :invoice_id"
    query_params = {'invoice_id': invoice_id}
    result = db.balance().execute(sumact2, query_params)
    assert sum1 == decimal.Decimal(str(result[0]['amount']))


def test_3():
    client_id = None or steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')
    steps.ClientSteps.link(client_id, 'chihiro-test-0')

    service_id = SERVICE_ID_DIRECT

    contract_id = None

    orders_list = append_orders_list(service_id, client_id, 3)

    request_id = steps.RequestSteps.create(client_id, orders_list)

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id)
    campaigns_params = {'Bucks': 0, 'Money': 23.41}
    steps.CampaignsSteps.do_campaigns(service_id, orders_list[1]['ServiceOrderID'], campaigns_params, 0, BASE_DT)
    steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)
    sumact1 = decimal.Decimal(str(campaigns_params.get('Bucks'))) + decimal.Decimal(str(campaigns_params.get('Money')))
    sumact2 = "select amount from t_act where invoice_id = :invoice_id"
    query_params = {'invoice_id': invoice_id}
    result = db.balance().execute(sumact2, query_params)
    print(result)
    assert sumact1 == decimal.Decimal(str(result[0]['amount']))


def test_4():
    client_id = None or steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')
    steps.ClientSteps.link(client_id, 'chihiro-test-0')

    service_id = SERVICE_ID_DIRECT

    contract_id = None
    product_id = PRODUCT_ID_DIRECT
    service_order_id = steps.OrderSteps.next_id(service_id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=service_id, product_id=product_id)
    orders_list1 = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}
    ]

    orders_list = append_orders_list(service_id, client_id, 1, orders_list1)

    request_id = steps.RequestSteps.create(client_id, orders_list)

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id)
    campaigns_params = {'Bucks': 0, 'Money': 23.41}
    steps.CampaignsSteps.do_campaigns(service_id, orders_list[1]['ServiceOrderID'], campaigns_params, 0, BASE_DT)
    steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)
    sumact1 = decimal.Decimal(str(campaigns_params.get('Bucks'))) + decimal.Decimal(str(campaigns_params.get('Money')))
    sumact2 = "select amount from t_act where invoice_id = :invoice_id"
    query_params = {'invoice_id': invoice_id}
    result = db.balance().execute(sumact2, query_params)
    print(result)
    assert sumact1 == decimal.Decimal(str(result[0]['amount']))


if __name__ == "__main__":
    # pytest.main()
    test_2()
    # test_4()
