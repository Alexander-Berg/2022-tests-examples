# coding=utf-8
__author__ = 'atkaya'
import datetime
from dateutil.relativedelta import relativedelta

from balance import balance_steps as steps
from btestlib.constants import PersonTypes, Services, Paysyses, Products, User, ContractPaymentType, \
    Currencies, ContractCreditType, ContractCommissionType, Firms
import balance.balance_db as db
from btestlib.data.defaults import Date

LOGIN = User(946894961, 'yndx-balance-assessor-user')

# не менять количество и свойства создаваемых заказов! они явно прописаны в кейсах
# кейсы можно искать по ключу "КИ"

def test_create_objects_for_asessors():
    client_id = steps.ClientSteps.create()

    steps.UserSteps.link_user_and_client(LOGIN, client_id)

    person_id_1 = steps.PersonSteps.create(client_id, PersonTypes.UR.code)
    person_id_2 = steps.PersonSteps.create(client_id, PersonTypes.PH.code)
    dt = datetime.datetime.now()

    # оплаченный и заакченный заказ по директу
    service_order_id_direct = steps.OrderSteps.next_id(service_id=Services.DIRECT.id)
    order_id_direct = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id_direct,
                                       product_id=Products.DIRECT_FISH.id,
                                       service_id=Services.DIRECT.id,
                            params={'Text': u'Тестовый оплаченный и заакченный заказ Директ'})
    orders_list = [
        {'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_direct, 'Qty': 100, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=dt))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id_1,
                                                 paysys_id=Paysyses.BANK_UR_RUB.id,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)
    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_direct, {Products.DIRECT_FISH.type.code: 100}, 0, dt)
    steps.ActsSteps.generate(client_id, force=1, date=dt)


    # заказ с родительским
    service_order_id_direct = steps.OrderSteps.next_id(service_id=Services.DIRECT.id)
    order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id_direct,
                                       product_id=Products.DIRECT_FISH.id,
                                       service_id=Services.DIRECT.id,
                            params={'Text': u'Тестовый заказ Директ с родительским'})
    orders_list = [
        {'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_direct, 'Qty': 100, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=dt))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id_1,
                                                 paysys_id=Paysyses.BANK_UR_RUB.id,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    db.balance().execute("update t_order set group_order_id = :parent_id where id = :id",
                         {'parent_id': order_id_direct, 'id': order_id})


    # неоплаченный заказ по директу с датой -5 дней от текущей даты
    dt_1 = datetime.datetime.now() - relativedelta(days=5)
    service_order_id_direct = steps.OrderSteps.next_id(service_id=Services.DIRECT.id)
    order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id_direct,
                                       product_id=Products.DIRECT_FISH.id,
                                       service_id=Services.DIRECT.id,
                            params={'Text': u'Тестовый неоплаченный заказ на Директ'})
    db.balance().execute("update t_order set dt = :dt where id = :id", {'dt': dt_1, 'id': order_id})
    orders_list = [
        {'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_direct, 'Qty': 100, 'BeginDT': dt_1}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=dt_1))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id_1,
                                                 paysys_id=Paysyses.BANK_UR_RUB.id,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)

    # оплаченный заказ по директу без откруток
    service_order_id_direct = steps.OrderSteps.next_id(service_id=Services.DIRECT.id)
    steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id_direct,
                                       product_id=Products.DIRECT_FISH.id,
                                       service_id=Services.DIRECT.id,
                            params={'Text': u'Тестовый оплаченный заказ на Директ без откруток'})
    orders_list = [
        {'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_direct, 'Qty': 500, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=dt))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id_1,
                                                 paysys_id=Paysyses.BANK_UR_RUB.id,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)


    # оплаченный заказ по директу, откручен не полностью, без акта
    service_order_id_direct = steps.OrderSteps.next_id(service_id=Services.DIRECT.id)
    steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id_direct,
                                       product_id=Products.DIRECT_FISH.id,
                                       service_id=Services.DIRECT.id,
                            params={'Text': u'Тестовый оплаченный заказ на Директ частично открученный'})
    orders_list = [
        {'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_direct, 'Qty': 500, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=dt))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id_1,
                                                 paysys_id=Paysyses.BANK_UR_RUB.id,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)
    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_direct, {Products.DIRECT_FISH.type.code: 250}, 0, dt)


    # заказ по Директу с несколькими оплатами
    service_order_id_direct = steps.OrderSteps.next_id(service_id=Services.DIRECT.id)
    steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id_direct,
                                       product_id=Products.DIRECT_FISH.id,
                                       service_id=Services.DIRECT.id,
                            params={'Text': u'Тестовый заказ Директ с несколькими оплатами'})
    orders_list = [
        {'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_direct, 'Qty': 500, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=dt))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id_1,
                                                 paysys_id=Paysyses.BANK_UR_RUB.id,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=dt))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id_1,
                                                 paysys_id=Paysyses.BANK_UR_RUB.id,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)

    # оплаченные и заакченные заказы по маркету
    for num in xrange(1, 5):
        service_order_id_direct = steps.OrderSteps.next_id(service_id=Services.MARKET.id)
        steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id_direct,
                                           product_id=Products.MARKET.id,
                                           service_id=Services.MARKET.id,
                                params={'Text': u'Тестовый заказ Маркет номер'+str(num)})
        orders_list = [
            {'ServiceID': Services.MARKET.id, 'ServiceOrderID': service_order_id_direct, 'Qty': 100*num, 'BeginDT': dt}
        ]
        request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                               additional_params=dict(InvoiceDesireDT=dt))
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id_1,
                                                     paysys_id=Paysyses.BANK_UR_RUB.id,
                                                     credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
        steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)
        steps.CampaignsSteps.do_campaigns(Services.MARKET.id, service_order_id_direct, {Products.MARKET.type.code: 100*num}, 0, dt)
        steps.ActsSteps.generate(client_id, force=1, date=dt)

    # заказ на маркет с договором и постоплатным счетом
    contract_params = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id_1,
        'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
        'FIRM': Firms.MARKET_111.id,
        'SERVICES': [Services.MARKET.id],
        'DT': Date.TODAY_ISO,
        'FINISH_DT': Date.HALF_YEAR_AFTER_TODAY_ISO,
        'IS_SIGNED': Date.TODAY_ISO,
        'CURRENCY': Currencies.RUB.num_code,
        'CREDIT_TYPE': ContractCreditType.BY_TERM_AND_SUM,
        'CREDIT_LIMIT_SINGLE': 1000000,
        'PERSONAL_ACCOUNT': 1
    }
    contract_id, _ = steps.ContractSteps.create_contract_new(ContractCommissionType.OPT_CLIENT, contract_params)
    service_order_id_direct = steps.OrderSteps.next_id(service_id=Services.MARKET.id)
    steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id_direct,
                                       product_id=Products.MARKET.id,
                                       service_id=Services.MARKET.id,
                            params={'Text': u'Тестовый заказ Маркет с договором'})
    orders_list = [
        {'ServiceID': Services.MARKET.id, 'ServiceOrderID': service_order_id_direct, 'Qty': 100, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=dt))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id_1,
                                                 paysys_id=Paysyses.BANK_UR_RUB.id,
                                                 credit=1, contract_id=contract_id, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)
    steps.CampaignsSteps.do_campaigns(Services.MARKET.id, service_order_id_direct, {Products.MARKET.type.code: 100}, 0, dt)
    steps.ActsSteps.generate(client_id, force=1, date=dt)

    # оплаченные и заакченные заказы по медийке
    for num in xrange(1, 2):
        service_order_id_direct = steps.OrderSteps.next_id(service_id=Services.MEDIA_70.id)
        steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id_direct,
                                           product_id=Products.MEDIA_2.id,
                                           service_id=Services.MEDIA_70.id,
                                params={'Text': u'Тестовый заказ Медийка номер'+str(num)})
        orders_list = [
            {'ServiceID': Services.MEDIA_70.id, 'ServiceOrderID': service_order_id_direct, 'Qty': 100*num, 'BeginDT': dt}
        ]
        request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                               additional_params=dict(InvoiceDesireDT=dt))
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id_2,
                                                     paysys_id=Paysyses.BANK_PH_RUB.id,
                                                     credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
        steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)
        steps.CampaignsSteps.do_campaigns(Services.MEDIA_70.id, service_order_id_direct, {Products.MEDIA_2.type.code: 100*num}, 0, dt)
        steps.ActsSteps.generate(client_id, force=1, date=dt)