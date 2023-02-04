# coding=utf-8

import balance.balance_db as db
import pytest
import datetime
from balance import balance_steps as steps
from balance import balance_api as api
from btestlib.data import defaults
from balance import balance_db as db
from temp.igogor.balance_objects import Contexts

NOW = datetime.datetime.now()
DIRECT = Contexts.DIRECT_FISH_RUB_CONTEXT.new()
MARKET = Contexts.MARKET_RUB_CONTEXT.new()
QTY = 100


@pytest.mark.parametrize('context, context_2', [(DIRECT, MARKET)])
def test_1(context, context_2):
    passport_id = db.get_passport_by_login('aikawa-test-1')[0]['passport_id']
    db.balance().execute('''UPDATE (SELECT * FROM t_passport WHERE PASSPORT_ID = :passport_id) SET CLIENT_ID = NULL''',
                         {'passport_id': passport_id})

    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    client_id_square = steps.ClientSteps.create()
    client_id_round = steps.ClientSteps.create()
    person_id_square = steps.PersonSteps.create(client_id_square, context.person_type.code)
    person_id_round = steps.PersonSteps.create(client_id_round, context.person_type.code)
    limited_repr_id = steps.ClientSteps.create()
    # limited_repr_id = 57589980
    api.test_balance().CreateUserClientAssociation(defaults.PASSPORT_UID, limited_repr_id, passport_id,
                                                   [client_id_square])

    person_id = steps.PersonSteps.create(agency_id, context.person_type.code)

    service_order_id1 = steps.OrderSteps.next_id(context.service.id)
    order_id1 = steps.OrderSteps.create(client_id_square, service_order_id1, service_id=context.service.id,
                                        product_id=context.product.id, params={'AgencyID': agency_id})
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id1, 'Qty': QTY, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(agency_id, orders_list, additional_params=dict(InvoiceDesireDT=NOW))
    invoice_id1, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=0,
                                                  contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id1)
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id1, {'Bucks': QTY}, 0, NOW)
    act_id = steps.ActsSteps.generate(agency_id, force=1, date=NOW)[0]

    service_order_id2 = steps.OrderSteps.next_id(context.service.id)
    order_id2 = steps.OrderSteps.create(client_id_round, service_order_id2, service_id=context.service.id,
                                        product_id=context.product.id, params={'AgencyID': agency_id})
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id2, 'Qty': QTY, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(agency_id, orders_list, additional_params=dict(InvoiceDesireDT=NOW))
    invoice_id2, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=0,
                                                  contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id2)
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id2, {'Bucks': QTY}, 0, NOW)
    act_id = steps.ActsSteps.generate(agency_id, force=1, date=NOW)[0]

    person_id_repr = steps.PersonSteps.create(limited_repr_id, context_2.person_type.code)

    service_order_id3 = steps.OrderSteps.next_id(context_2.service.id)
    order_id3 = steps.OrderSteps.create(limited_repr_id, service_order_id3, service_id=context_2.service.id,
                                        product_id=context_2.product.id)
    orders_list = [{'ServiceID': context_2.service.id, 'ServiceOrderID': service_order_id3, 'Qty': QTY, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(limited_repr_id, orders_list, additional_params=dict(InvoiceDesireDT=NOW))
    invoice_id3, _, _ = steps.InvoiceSteps.create(request_id, person_id_repr, context_2.paysys.id, credit=0,
                                                  contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id3)
    steps.CampaignsSteps.do_campaigns(context_2.service.id, service_order_id3, {'Bucks': QTY}, 0, NOW)
    act_id = steps.ActsSteps.generate(limited_repr_id, force=1, date=NOW)[0]

    service_order_id4 = steps.OrderSteps.next_id(context.service.id)
    order_id4 = steps.OrderSteps.create(limited_repr_id, service_order_id4, service_id=context.service.id,
                                        product_id=context.product.id)
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id4, 'Qty': QTY, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(limited_repr_id, orders_list, additional_params=dict(InvoiceDesireDT=NOW))
    invoice_id4, _, _ = steps.InvoiceSteps.create(request_id, person_id_repr, context.paysys.id, credit=0,
                                                  contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id4)
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id4, {'Bucks': QTY}, 0, NOW)
    act_id = steps.ActsSteps.generate(limited_repr_id, force=1, date=NOW)[0]

    service_order_id5 = steps.OrderSteps.next_id(context.service.id)
    order_id5 = steps.OrderSteps.create(client_id_round, service_order_id5, service_id=context.service.id,
                                        product_id=context.product.id)
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id5, 'Qty': QTY, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id_round, orders_list, additional_params=dict(InvoiceDesireDT=NOW))
    invoice_id5, _, _ = steps.InvoiceSteps.create(request_id, person_id_round, context.paysys.id, credit=0,
                                                  contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id5)
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id5, {'Bucks': QTY}, 0, NOW)
    act_id = steps.ActsSteps.generate(client_id_round, force=1, date=NOW)[0]

    service_order_id6 = steps.OrderSteps.next_id(context_2.service.id)
    order_id6 = steps.OrderSteps.create(client_id_round, service_order_id6, service_id=context_2.service.id,
                                        product_id=context_2.product.id)
    orders_list = [{'ServiceID': context_2.service.id, 'ServiceOrderID': service_order_id6, 'Qty': QTY, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id_round, orders_list, additional_params=dict(InvoiceDesireDT=NOW))
    invoice_id6, _, _ = steps.InvoiceSteps.create(request_id, person_id_round, context_2.paysys.id, credit=0,
                                                  contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id6)
    steps.CampaignsSteps.do_campaigns(context_2.service.id, service_order_id6, {'Bucks': QTY}, 0, NOW)
    act_id = steps.ActsSteps.generate(client_id_round, force=1, date=NOW)[0]

    service_order_id7 = steps.OrderSteps.next_id(context.service.id)
    order_id7 = steps.OrderSteps.create(client_id_square, service_order_id7, service_id=context.service.id,
                                        product_id=context.product.id)
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id7, 'Qty': QTY, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id_square, orders_list, additional_params=dict(InvoiceDesireDT=NOW))
    invoice_id7, _, _ = steps.InvoiceSteps.create(request_id, person_id_square, context.paysys.id, credit=0,
                                                  contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id7)
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id7, {'Bucks': QTY}, 0, NOW)
    act_id = steps.ActsSteps.generate(client_id_square, force=1, date=NOW)[0]

    print '''
    Агенство: {0}
    Квадратный клиент: {1}
    Круглый клиент: {2}
    Представитель: {3}, 'aikawa-test-10', Квадратный клиент
    Агентский счет с заказом квадратного клиента:
    https://admin-feature-direct-limited-roles.greed-branch.paysys.yandex.ru/invoice.xml?invoice_id={4}
    service_order_id - {11}
    Агентский счет с заказом круглого клиента:
    https://admin-feature-direct-limited-roles.greed-branch.paysys.yandex.ru/invoice.xml?invoice_id={5}
    service_order_id - {12}
    Счет на маркет представителя:
    https://admin-feature-direct-limited-roles.greed-branch.paysys.yandex.ru/invoice.xml?invoice_id={6}
    service_order_id - {13}
    Счет на Директ представителя:
    https://admin-feature-direct-limited-roles.greed-branch.paysys.yandex.ru/invoice.xml?invoice_id={7}
    service_order_id - {14}
    Счет на Директ круглого клиента:
    https://admin-feature-direct-limited-roles.greed-branch.paysys.yandex.ru/invoice.xml?invoice_id={8}
    service_order_id - {15}
    Счет на Маркет круглого клиента:
    https://admin-feature-direct-limited-roles.greed-branch.paysys.yandex.ru/invoice.xml?invoice_id={9}
    service_order_id - {16}
    Счет на Директ квадратного клиента:
    https://admin-feature-direct-limited-roles.greed-branch.paysys.yandex.ru/invoice.xml?invoice_id={10}
    service_order_id - {17}
    '''.format(agency_id, client_id_square, client_id_round, limited_repr_id, invoice_id1, invoice_id2, invoice_id3,
               invoice_id4, invoice_id5, invoice_id6, invoice_id7, service_order_id1, service_order_id2,
               service_order_id3, service_order_id4, service_order_id5, service_order_id6, service_order_id7)


'''
    Агенство: 60552984
    Квадратный клиент: 60552985
    Круглый клиент: 60552986
    Представитель: 60552987, 'aikawa-test-10', Квадратный клиент
    Агентский счет с заказом квадратного клиента:
    https://admin-feature-direct-limited-roles.greed-branch.paysys.yandex.ru/invoice.xml?invoice_id=70189501
    service_order_id - 96563100
    Агентский счет с заказом круглого клиента:
    https://admin-feature-direct-limited-roles.greed-branch.paysys.yandex.ru/invoice.xml?invoice_id=70189502
    service_order_id - 96563101
    Счет на маркет представителя:
    https://admin-feature-direct-limited-roles.greed-branch.paysys.yandex.ru/invoice.xml?invoice_id=70189503
    service_order_id - 22745014
    Счет на Директ представителя:
    https://admin-feature-direct-limited-roles.greed-branch.paysys.yandex.ru/invoice.xml?invoice_id=70189504
    service_order_id - 96563102
    Счет на Директ круглого клиента:
    https://admin-feature-direct-limited-roles.greed-branch.paysys.yandex.ru/invoice.xml?invoice_id=70189506
    service_order_id - 96563103
    Счет на Маркет круглого клиента:
    https://admin-feature-direct-limited-roles.greed-branch.paysys.yandex.ru/invoice.xml?invoice_id=70189507
    service_order_id - 22745015
    Счет на Директ квадратного клиента:
    https://admin-feature-direct-limited-roles.greed-branch.paysys.yandex.ru/invoice.xml?invoice_id=70189508
    service_order_id - 96563104

'''


@pytest.mark.parametrize('context, context_2', [(DIRECT, MARKET)])
def test_2_agency(context, context_2):
    passport_id = db.get_passport_by_login('aikawa-test-1')[0]['passport_id']
    db.balance().execute('''UPDATE (SELECT * FROM t_passport WHERE PASSPORT_ID = :passport_id) SET CLIENT_ID = NULL''',
                         {'passport_id': passport_id})

    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    agency_id2 = steps.ClientSteps.create({'IS_AGENCY': 1})

    client_id_square = steps.ClientSteps.create()
    client_id_round = steps.ClientSteps.create()
    person_id_square = steps.PersonSteps.create(client_id_square, context.person_type.code)
    person_id_round = steps.PersonSteps.create(client_id_round, context.person_type.code)
    limited_repr_id = steps.ClientSteps.create()
    # limited_repr_id = 57589980
    # api.test_balance().CreateUserClientAssociation(defaults.PASSPORT_UID, limited_repr_id, passport_id,
    #                                                [client_id_square])

    person_id_agency = steps.PersonSteps.create(agency_id, context.person_type.code)
    person_id_agency_2 = steps.PersonSteps.create(agency_id2, context.person_type.code)

    service_order_id1 = steps.OrderSteps.next_id(context.service.id)
    order_id1 = steps.OrderSteps.create(client_id_square, service_order_id1, service_id=context.service.id,
                                        product_id=context.product.id, params={'AgencyID': agency_id})
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id1, 'Qty': QTY, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(agency_id, orders_list, additional_params=dict(InvoiceDesireDT=NOW))
    invoice_id1, _, _ = steps.InvoiceSteps.create(request_id, person_id_agency, context.paysys.id, credit=0,
                                                  contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id1)

    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id1, {'Bucks': QTY}, 0, NOW)
    act_id = steps.ActsSteps.generate(agency_id, force=1, date=NOW)[0]

    service_order_id2 = steps.OrderSteps.next_id(context.service.id)
    order_id2 = steps.OrderSteps.create(client_id_square, service_order_id2, service_id=context.service.id,
                                        product_id=context.product.id, params={'AgencyID': agency_id2})
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id2, 'Qty': QTY, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(agency_id, orders_list, additional_params=dict(InvoiceDesireDT=NOW))
    invoice_id2, _, _ = steps.InvoiceSteps.create(request_id, person_id_agency_2, context.paysys.id, credit=0,
                                                  contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id2)


@pytest.mark.parametrize('context, context_2', [(DIRECT, MARKET)])
def test_parent_order(context, context_2):
    passport_id = db.get_passport_by_login('aikawa-test-1')[0]['passport_id']
    db.balance().execute(
        '''UPDATE (SELECT * FROM t_passport WHERE PASSPORT_ID = :passport_id) SET CLIENT_ID = NULL''',
        {'passport_id': passport_id})

    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})

    client_id_square = steps.ClientSteps.create()
    client_id_round = steps.ClientSteps.create()
    person_id_square = steps.PersonSteps.create(client_id_square, context.person_type.code)
    person_id_round = steps.PersonSteps.create(client_id_round, context.person_type.code)
    limited_repr_id = steps.ClientSteps.create()
    api.test_balance().CreateUserClientAssociation(defaults.PASSPORT_UID, limited_repr_id, passport_id,
                                                   [client_id_square])

    person_id_agency = steps.PersonSteps.create(agency_id, context.person_type.code)

    service_order_id1 = steps.OrderSteps.next_id(context.service.id)
    order_id1 = steps.OrderSteps.create(client_id_square, service_order_id1, service_id=context.service.id,
                                        product_id=context.product.id, params={'AgencyID': agency_id})
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id1, 'Qty': QTY, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(agency_id, orders_list, additional_params=dict(InvoiceDesireDT=NOW))
    invoice_id1, _, _ = steps.InvoiceSteps.create(request_id, person_id_agency, context.paysys.id, credit=0,
                                                  contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id1)

    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id1, {'Bucks': QTY}, 0, NOW)
    act_id = steps.ActsSteps.generate(agency_id, force=1, date=NOW)[0]

    service_order_id2 = steps.OrderSteps.next_id(context.service.id)
    order_id2 = steps.OrderSteps.create(agency_id, service_order_id2, service_id=context.service.id,
                                        product_id=context.product.id, params={'AgencyID': None})
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id2, 'Qty': QTY, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(agency_id, orders_list, additional_params=dict(InvoiceDesireDT=NOW))
    invoice_id2, _, _ = steps.InvoiceSteps.create(request_id, person_id_agency, context.paysys.id, credit=0,
                                                  contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id2)

    steps.OrderSteps.merge(order_id2, [order_id1])
    #
    # '''
    #     Агенство: 60552984
    #     Квадратный клиент: 60552985
    #     Круглый клиент: 60552986
    #     Представитель: 60552987, 'aikawa-test-10', Квадратный клиент
    #     Агентский счет с заказом квадратного клиента:
    #     https://admin-feature-direct-limited-roles.greed-branch.paysys.yandex.ru/invoice.xml?invoice_id=70189501
    #     service_order_id - 96563100
    #     Агентский счет с заказом круглого клиента:
    #     https://admin-feature-direct-limited-roles.greed-branch.paysys.yandex.ru/invoice.xml?invoice_id=70189502
    #     service_order_id - 96563101
    #     Счет на маркет представителя:
    #     https://admin-feature-direct-limited-roles.greed-branch.paysys.yandex.ru/invoice.xml?invoice_id=70189503
    #     service_order_id - 22745014
    #     Счет на Директ представителя:
    #     https://admin-feature-direct-limited-roles.greed-branch.paysys.yandex.ru/invoice.xml?invoice_id=70189504
    #     service_order_id - 96563102
    #     Счет на Директ круглого клиента:
    #     https://admin-feature-direct-limited-roles.greed-branch.paysys.yandex.ru/invoice.xml?invoice_id=70189506
    #     service_order_id - 96563103
    #     Счет на Маркет круглого клиента:
    #     https://admin-feature-direct-limited-roles.greed-branch.paysys.yandex.ru/invoice.xml?invoice_id=70189507
    #     service_order_id - 22745015
    #     Счет на Директ квадратного клиента:
    #     https://admin-feature-direct-limited-roles.greed-branch.paysys.yandex.ru/invoice.xml?invoice_id=70189508
    #     service_order_id - 96563104
    #
    # '''


# заказ под агентством на маркет непривязанного к представителю с ограниченным доступом клиента
@pytest.mark.parametrize('context, context_2', [(DIRECT, MARKET)])
def test_agency_market_round(context, context_2):
    passport_id = db.get_passport_by_login('aikawa-test-1')[0]['passport_id']
    db.balance().execute('''UPDATE (SELECT * FROM t_passport WHERE PASSPORT_ID = :passport_id) SET CLIENT_ID = NULL''',
                         {'passport_id': passport_id})

    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    client_id_square = steps.ClientSteps.create()
    client_id_round = steps.ClientSteps.create()
    person_id_square = steps.PersonSteps.create(client_id_square, context.person_type.code)
    person_id_round = steps.PersonSteps.create(client_id_round, context.person_type.code)
    limited_repr_id = steps.ClientSteps.create()
    # limited_repr_id = 57589980
    api.test_balance().CreateUserClientAssociation(defaults.PASSPORT_UID, None, passport_id,
                                                   [client_id_square])

    person_id = steps.PersonSteps.create(agency_id, context_2.person_type.code)

    service_order_id1 = steps.OrderSteps.next_id(context_2.service.id)
    order_id1 = steps.OrderSteps.create(client_id_round, service_order_id1, service_id=context_2.service.id,
                                        product_id=context_2.product.id, params={'AgencyID': agency_id})
    orders_list = [{'ServiceID': context_2.service.id, 'ServiceOrderID': service_order_id1, 'Qty': QTY, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(agency_id, orders_list, additional_params=dict(InvoiceDesireDT=NOW))
    invoice_id1, _, _ = steps.InvoiceSteps.create(request_id, person_id, context_2.paysys.id, credit=0,
                                                  contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id1)
    steps.CampaignsSteps.do_campaigns(context_2.service.id, service_order_id1, {'Bucks': QTY}, 0, NOW)
    act_id = steps.ActsSteps.generate(agency_id, force=1, date=NOW)[0]

    '''
        Агенство: 60552984
        Квадратный клиент: 60552985
        Круглый клиент: 60552986
        Представитель: 60552987, 'aikawa-test-10', Квадратный клиент
        Агентский счет с заказом квадратного клиента:
        https://admin-feature-direct-limited-roles.greed-branch.paysys.yandex.ru/invoice.xml?invoice_id=70189501
        service_order_id - 96563100
        Агентский счет с заказом круглого клиента:
        https://admin-feature-direct-limited-roles.greed-branch.paysys.yandex.ru/invoice.xml?invoice_id=70189502
        service_order_id - 96563101
        Счет на маркет представителя:
        https://admin-feature-direct-limited-roles.greed-branch.paysys.yandex.ru/invoice.xml?invoice_id=70189503
        service_order_id - 22745014
        Счет на Директ представителя:
        https://admin-feature-direct-limited-roles.greed-branch.paysys.yandex.ru/invoice.xml?invoice_id=70189504
        service_order_id - 96563102
        Счет на Директ круглого клиента:
        https://admin-feature-direct-limited-roles.greed-branch.paysys.yandex.ru/invoice.xml?invoice_id=70189506
        service_order_id - 96563103
        Счет на Маркет круглого клиента:
        https://admin-feature-direct-limited-roles.greed-branch.paysys.yandex.ru/invoice.xml?invoice_id=70189507
        service_order_id - 22745015
        Счет на Директ квадратного клиента:
        https://admin-feature-direct-limited-roles.greed-branch.paysys.yandex.ru/invoice.xml?invoice_id=70189508
        service_order_id - 96563104

    '''


@pytest.mark.parametrize('context, context_2', [(DIRECT, MARKET)])
def test_direct_client_order(context, context_2):
    passport_id = db.get_passport_by_login('aikawa-test-1')[0]['passport_id']
    db.balance().execute('''UPDATE (SELECT * FROM t_passport WHERE PASSPORT_ID = :passport_id) SET CLIENT_ID = NULL''',
                         {'passport_id': passport_id})

    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    client_id_square = steps.ClientSteps.create()

    limited_repr_id = steps.ClientSteps.create()
    api.test_balance().CreateUserClientAssociation(defaults.PASSPORT_UID, limited_repr_id, passport_id,
                                                   [client_id_square])

    person_id = steps.PersonSteps.create(agency_id, context.person_type.code)
    person_id_square = steps.PersonSteps.create(client_id_square, context.person_type.code)

    service_order_id1 = steps.OrderSteps.next_id(context.service.id)
    order_id1 = steps.OrderSteps.create(client_id_square, service_order_id1, service_id=context.service.id,
                                        product_id=context.product.id, params={'AgencyID': None})
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id1, 'Qty': QTY, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id_square, orders_list, additional_params=dict(InvoiceDesireDT=NOW))
    invoice_id1, _, _ = steps.InvoiceSteps.create(request_id, person_id_square, context.paysys.id, credit=0,
                                                  contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id1)
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id1, {'Bucks': QTY}, 0, NOW)
    act_id = steps.ActsSteps.generate(client_id_square, force=1, date=NOW)[0]


@pytest.mark.parametrize('context, context_2', [(DIRECT, MARKET)])
def test_direct_client_order_old(context, context_2):
    passport_id = db.get_passport_by_login('aikawa-test-1')[0]['passport_id']
    db.balance().execute('''UPDATE (SELECT * FROM t_passport WHERE PASSPORT_ID = :passport_id) SET CLIENT_ID = NULL''',
                         {'passport_id': passport_id})

    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    client_id_square = steps.ClientSteps.create()

    api.test_balance().CreateUserClientAssociation(defaults.PASSPORT_UID, agency_id, passport_id)

    person_id = steps.PersonSteps.create(agency_id, context.person_type.code)
    person_id_square = steps.PersonSteps.create(client_id_square, context.person_type.code)

    service_order_id1 = steps.OrderSteps.next_id(context.service.id)
    order_id1 = steps.OrderSteps.create(client_id_square, service_order_id1, service_id=context.service.id,
                                        product_id=context.product.id, params={'AgencyID': None})
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id1, 'Qty': QTY, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id_square, orders_list, additional_params=dict(InvoiceDesireDT=NOW))
    invoice_id1, _, _ = steps.InvoiceSteps.create(request_id, person_id_square, context.paysys.id, credit=0,
                                                  contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id1)
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id1, {'Bucks': QTY}, 0, NOW)
    act_id = steps.ActsSteps.generate(client_id_square, force=1, date=NOW)[0]


@pytest.mark.parametrize('context', [DIRECT])
def test_link_completly_limited(context):
    passport_id = db.get_passport_by_login('aikawa-test-1')[0]['passport_id']
    db.balance().execute('''UPDATE (SELECT * FROM t_passport WHERE PASSPORT_ID = :passport_id) SET CLIENT_ID = NULL''',
                         {'passport_id': passport_id})

    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    client_id_square = steps.ClientSteps.create()

    api.test_balance().CreateUserClientAssociation(defaults.PASSPORT_UID, agency_id, passport_id)

    person_id = steps.PersonSteps.create(agency_id, context.person_type.code)
    person_id_square = steps.PersonSteps.create(client_id_square, context.person_type.code)

    service_order_id1 = steps.OrderSteps.next_id(context.service.id)
    order_id1 = steps.OrderSteps.create(client_id_square, service_order_id1, service_id=context.service.id,
                                        product_id=context.product.id, params={'AgencyID': None})
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id1, 'Qty': QTY, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id_square, orders_list, additional_params=dict(InvoiceDesireDT=NOW))
    invoice_id1, _, _ = steps.InvoiceSteps.create(request_id, person_id_square, context.paysys.id, credit=0,
                                                  contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id1)
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id1, {'Bucks': QTY}, 0, NOW)
    act_id = steps.ActsSteps.generate(client_id_square, force=1, date=NOW)[0]
