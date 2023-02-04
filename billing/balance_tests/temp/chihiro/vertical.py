# coding=utf-8

import datetime
import decimal

from balance import balance_api as api
from balance import balance_db as db
from balance import balance_steps as steps
from check import check_utils
from check import retrying

QTY = decimal.Decimal("55.7")
SLEEP = 20
CLIENT_LINK = 'chihiro-test-0'
LAST_DAY_OF_PREVIOUS_MONTH = datetime.datetime.now().replace(year=2007) + datetime.timedelta(minutes=10)
check_list = ['bua']

def get_max_cmp_dt(type):
    table = 'cmp.' + str(type) + '_cmp'
    query_1 = 'select id from ' + table + ' where dt = ( select max(dt) as dt from ' + table + ')'
    new_id = db.balance().execute(query_1)[0]['id'] if db.balance().execute(query_1) else None
    query = 'select finish_dt from ' + table + ' where id = ' + str(new_id)
    db.balance().execute(query)
    return db.balance().execute(query)[0]['finish_dt'] if new_id else None

@retrying.retry(stop_max_attempt_number=3, wait_exponential_multiplier=1 * 1000)
def start_check(type, object, raw_cmd_args):
    api.test_balance().DCSRunCheck({'code': type, 'objects': object, 'raw_cmd_args': raw_cmd_args})

def creator_data_all():
    type = 'bua'
    cmp_time = {}
    order_id = [str(test_2_()), str(test_2())]
    cmp_time[type] = get_max_cmp_dt(type)
    object = ','.join(order_id)
    raw_cmd_args = '--completions-dt {0} --acts-dt {1}'.format(str(datetime.datetime.now().strftime('%Y-%m-%d')), str(datetime.datetime.now().strftime('%Y-%m-%d')))
    start_check(type, object, raw_cmd_args)
    # query = 'select external_id from bo.T_INVOICE where id= :inv_id'
    # query_params = {'inv_id': inv_id}
    # eid = db.balance().execute(query, query_params)[0]['external_id']
    cmp_id = check_utils.get_cmp_id_list(cmp_time, check_list)
    table = 'cmp.{0}_cmp_data'.format(type)
    for order_ids in order_id:
        query = 'select state from ' + table + ' where order_id= :order_id and cmp_id= :cmp_id'
        query_params = {'order_id': order_ids, 'cmp_id': cmp_id[type]}
        state = db.balance().execute(query, query_params)
        if not state:
            state = 0
        else:
            state = state[0]['state']
        print(state)

def update_date(order_id, new_date):
    query = 'update (select * from bo.t_order where id = :order_id) set dt = :new_date'
    query_params = {'order_id': order_id, 'new_date': new_date}
    db.balance().execute(query, query_params)

def update_date_invoice(inv_id, new_date):
    query = 'update (select * from bo.t_invoice where id = :inv_id) set dt = :new_date'
    query_params = {'inv_id': inv_id, 'new_date': new_date}
    db.balance().execute(query, query_params)

def get_order_id(service_order_id, service_id):
    query = 'select id from t_order where SERVICE_ORDER_ID= :service_order_id and SERVICE_ID = :service_id'
    query_params = {'service_order_id': service_order_id, 'service_id': service_id}
    order_id = db.balance().execute(query, query_params)[0]['id']
    return order_id

def test_10():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')
    steps.ClientSteps.link(client_id, CLIENT_LINK)

    contract_id = None

    service_id = 7
    product_id = 1475

    service_order_id = steps.OrderSteps.next_id(service_id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=service_id, product_id=product_id)
    # order_id = get_order_id(service_order_id, service_id)
    new_date = datetime.datetime.now().replace(day=1) - datetime.timedelta(days=1)
    update_date(order_id, new_date)
    date = datetime.datetime.now().replace(day=1) - datetime.timedelta(days=1)


    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY,
         'BeginDT': date}]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params=dict(InvoiceDesireDT=date))

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, 1003,
                                                 credit=0,
                                                 contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id, payment_dt=date)

    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 12, 'Money': 0}, 0,
                                      date)
    steps.ActsSteps.generate(client_id, force=1, date=date)
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 10, 'Money': 0}, 0,
                                      date)
    act_id = db.get_acts_by_client(client_id)[0]['id']
    print(act_id)
    return order_id

def test_9():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')
    steps.ClientSteps.link(client_id, CLIENT_LINK)

    contract_id = None

    service_id = 7
    product_id = 1475

    service_order_id = steps.OrderSteps.next_id(service_id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=service_id, product_id=product_id)
    # order_id = get_order_id(service_order_id, service_id)
    new_date = datetime.datetime.now().replace(day=1) - datetime.timedelta(days=1)
    update_date(order_id, new_date)
    date = datetime.datetime.now().replace(day=1) - datetime.timedelta(days=1)


    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY,
         'BeginDT': date}]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params=dict(InvoiceDesireDT=date))

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, 1003,
                                                 credit=0,
                                                 contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id, payment_dt=date)

    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 12, 'Money': 0}, 0,
                                      date)
    steps.ActsSteps.generate(client_id, force=1, date=date)
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 17, 'Money': 0}, 0,
                                      date)
    act_id = db.get_acts_by_client(client_id)[0]['id']
    print(act_id)
    return order_id

# todo использовать другой метод из библиотеки
def test_4():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')
    steps.ClientSteps.link(client_id, CLIENT_LINK)

    contract_id = None

    service_id = 7
    product_id = 1475

    service_order_id = steps.OrderSteps.next_id(service_id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=service_id, product_id=product_id)
    # order_id = get_order_id(service_order_id, service_id)
    new_date = datetime.datetime.now().replace(year=2007)
    update_date(order_id, new_date)

    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY,
         'BeginDT': LAST_DAY_OF_PREVIOUS_MONTH}]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params=dict(InvoiceDesireDT=LAST_DAY_OF_PREVIOUS_MONTH))

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, 1003,
                                                 credit=0,
                                                 contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id, payment_dt=LAST_DAY_OF_PREVIOUS_MONTH)

    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 12, 'Money': 0}, 0,
                                      LAST_DAY_OF_PREVIOUS_MONTH)
    steps.ActsSteps.generate(client_id, force=1, date=LAST_DAY_OF_PREVIOUS_MONTH)
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 17, 'Money': 0}, 0,
                                      LAST_DAY_OF_PREVIOUS_MONTH)
    act_id = db.get_acts_by_client(client_id)[0]['id']
    print(act_id)
    return order_id

def test_5():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')
    steps.ClientSteps.link(client_id, CLIENT_LINK)
    date = datetime.datetime.now().replace(day=1) - datetime.timedelta(days=2)

    contract_id = None

    service_id = 7
    product_id = 1475

    service_order_id = steps.OrderSteps.next_id(service_id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=service_id, product_id=product_id)
    order_id = get_order_id(service_order_id, service_id)
    update_date(order_id, date)

    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY,
         'BeginDT': LAST_DAY_OF_PREVIOUS_MONTH}]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params=dict(InvoiceDesireDT=date))

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, 1003,
                                                 credit=0,
                                                 contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id, payment_dt=date)

    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 12, 'Money': 0}, 0,
                                      date)
    steps.ActsSteps.generate(client_id, force=1, date=date)
    date = datetime.datetime.now().replace(day=2)
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 17, 'Money': 0}, 0,
                                      date)

    act_id = db.get_acts_by_client(client_id)[0]['id']
    print(act_id)

# лицевой счет
def test_():
    SERVICE_ID_DIRECT = 7
    PRODUCT_ID_DIRECT = 1475
    PAYSYS_ID = 1028
    QTY = 50
    BASE_DT = datetime.datetime.now()
    NEW_CAMPAIGNS_DT = datetime.datetime.now() + datetime.timedelta(days=1)
    client_id = None or steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'usu')
    steps.ClientSteps.link(client_id, 'clientuid32')

    service_id = SERVICE_ID_DIRECT
    product_id = PRODUCT_ID_DIRECT

    service_order_id = steps.OrderSteps.next_id(service_id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=service_id, product_id=product_id)
    service_order_id_2 = steps.OrderSteps.next_id(service_id)
    steps.OrderSteps.create(client_id, service_order_id_2, service_id=service_id, product_id=product_id)
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT},
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id_2, 'Qty': QTY, 'BeginDT': BASE_DT}
    ]
    contract_type = 'usa_comm_post'
    request_id = steps.RequestSteps.create(client_id, orders_list)
    contract_id, _ = steps.ContractSteps.create_contract(contract_type, {'PERSON_ID': person_id, 'CLIENT_ID': client_id,
                                                                         'SERVICES': [service_id],
                                                                         'FINISH_DT': NEW_CAMPAIGNS_DT,
                                                                         'REPAYMENT_ON_CONSUME': 0,
                                                                         'PERSONAL_ACCOUNT': 1})
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=1, contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)

    # db.balance().execute('update bo.t_invoice set TRANSFER_ACTED = 1 where id = :id', {'id': invoice_id})
    campaigns_qty = 20

    # steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': campaigns_qty, 'Money': 0}, 0, BASE_DT)
    # steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)
    campaigns_delta = 2
    # campaigns_qty = campaigns_qty+campaigns_delta
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': campaigns_qty, 'Money': 0}, 0, BASE_DT)
    campaigns_qty = campaigns_qty+campaigns_delta

    # steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)
    campaigns_qty_2 = 30
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id_2, {'Bucks': campaigns_qty_2, 'Money': 0}, 0, BASE_DT)
    steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)
    # campaigns_delta_2 = 11
    campaigns_qty_2 = campaigns_qty_2 - campaigns_delta
    # campaigns_qty = campaigns_qty + 5
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': campaigns_qty, 'Money': 0}, 0, BASE_DT)
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id_2, {'Bucks': campaigns_qty_2, 'Money': 0}, 0, BASE_DT)

def test_7():
    BASE_DT = datetime.datetime.now()
    NEW_CAMPAIGNS_DT = datetime.datetime.now() + datetime.timedelta(days=1)
    client_id = None or steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')
    steps.ClientSteps.link(client_id, 'clientuid32')

    service_id = 7
    product_id = 1475

    service_order_id = steps.OrderSteps.next_id(service_id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=service_id, product_id=product_id)
    # steps.ClientSteps.migrate_to_currency(client_id, 'MODIFY')
    service_order_id_2 = steps.OrderSteps.next_id(service_id)
    steps.OrderSteps.create(client_id, service_order_id_2, service_id=service_id, product_id=product_id)
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT},
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id_2, 'Qty': QTY, 'BeginDT': BASE_DT}
    ]
    contract_type = 'opt_agency_prem_post'
    sub_order_ids = [get_order_id(service_order_id,service_id), get_order_id(service_order_id_2, service_id)]
    service_order_id_3 = steps.OrderSteps.next_id(service_id)
    parent_order = steps.OrderSteps.create(client_id, service_order_id_3, service_id=service_id, product_id=product_id)
    # steps.OrderSteps.merge(parent_order, sub_order_ids)
    # steps.OrderSteps.ua_enqueue([client_id])
    request_id = steps.RequestSteps.create(client_id, orders_list)

    # contract_id, _ = steps.ContractSteps.create(contract_type, {'person_id': person_id, 'client_id': client_id,
    #                                                             'SERVICES': [service_id], 'FINISH_DT': NEW_CAMPAIGNS_DT,
    #                                                             'REPAYMENT_ON_CONSUME': 0, 'PERSONAL_ACCOUNT': 1})
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, 1003, credit=0, contract_id=None,
                                                 overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id, payment_dt=BASE_DT)
    steps.OrderSteps.merge(parent_order, sub_order_ids)

    # db.balance().execute('update bo.t_invoice set TRANSFER_ACTED = 1 where id = :id', {'id': invoice_id})
    campaigns_qty = 20

    # steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': campaigns_qty, 'Money': 0}, 0, BASE_DT)
    # steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)
    campaigns_delta = 2
    # campaigns_qty = campaigns_qty+campaigns_delta
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': campaigns_qty, 'Money': 0}, 0, BASE_DT)
    campaigns_qty = campaigns_qty+campaigns_delta

    # steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)
    campaigns_qty_2 = 30
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id_2, {'Bucks': campaigns_qty_2, 'Money': 0}, 0, BASE_DT)
    steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)
    # steps.ClientSteps.migrate_to_currency(client_id, 'MODIFY')
    # campaigns_delta_2 = 11
    campaigns_qty_2 = campaigns_qty_2 - campaigns_delta
    # campaigns_qty = campaigns_qty + 5
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': campaigns_qty, 'Money': 0}, 0, BASE_DT)
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id_2, {'Bucks': campaigns_qty_2, 'Money': 0}, 0, BASE_DT)

def test_8():
    BASE_DT = datetime.datetime.now()
    NEW_CAMPAIGNS_DT = datetime.datetime.now() + datetime.timedelta(days=1)
    client_id = None or steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')
    steps.ClientSteps.link(client_id, 'clientuid32')

    service_id = 7
    product_id = 503162

    service_order_id = steps.OrderSteps.next_id(service_id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=service_id, product_id=1475)
    service_order_id_2 = steps.OrderSteps.next_id(service_id)
    steps.ClientSteps.migrate_to_currency(client_id, 'MODIFY')
    steps.OrderSteps.create(client_id, service_order_id_2, service_id=service_id, product_id=product_id)
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT},
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id_2, 'Qty': 90, 'BeginDT': BASE_DT}
    ]
    sub_order_ids = [get_order_id(service_order_id,service_id), get_order_id(service_order_id_2, service_id)]
    service_order_id_3 = steps.OrderSteps.next_id(service_id)
    parent_order = steps.OrderSteps.create(client_id, service_order_id_3, service_id=service_id, product_id=product_id)
    request_id = steps.RequestSteps.create(client_id, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, 1003, credit=0, contract_id=None,
                                                 overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id, payment_dt=BASE_DT)
    steps.OrderSteps.merge(parent_order, sub_order_ids)
    campaigns_qty = 20
    campaigns_delta = 2
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': campaigns_qty, 'Money': 0}, 0, BASE_DT)
    campaigns_qty = campaigns_qty+campaigns_delta
    campaigns_qty_2 = 90
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id_2, {'Bucks': 0, 'Money': campaigns_qty_2}, 0, BASE_DT)
    steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)
    campaigns_qty_2 = campaigns_qty_2 - 60
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': campaigns_qty, 'Money': 0}, 0, BASE_DT)
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id_2, {'Bucks': 0, 'Money': campaigns_qty_2}, 0, BASE_DT)


def test_1():
    BASE_DT = datetime.datetime.now() - datetime.timedelta(days=1)
    NEW_CAMPAIGNS_DT = datetime.datetime.now() + datetime.timedelta(days=1)
    client_id = None or steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')
    steps.ClientSteps.link(client_id, 'clientuid32')

    service_id = 7
    product_id = 1475

    service_order_id = steps.OrderSteps.next_id(service_id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=service_id, product_id=1475)
    api.test_balance().PayOrderCC({
        'order_id': order_id,
        'paysys_code': 'ce',
        'qty': 100
        # (или str(decimal.Decimal('100.333333')))
    })
    invoice_ce_id = \
        db.balance().execute('select id from bo.t_invoice where client_id = :client_id and paysys_id = 1006',
                             {'client_id': client_id})[0]['id']
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 50, 'Money': 0}, 0, BASE_DT)
    steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)
    api.test_balance().PayOrderCC({
        'order_id': order_id,
        'paysys_code': 'co',
        'qty': 50
        # (или str(decimal.Decimal('100.333333')))
    })
    invoice_co_id = \
        db.balance().execute('select id from bo.t_invoice where client_id = :client_id and paysys_id = 1007',
                             {'client_id': client_id})[0]['id']
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 60, 'Money': 0}, 0, BASE_DT)
    return order_id


def test_2_():
   # BASE_DT = datetime.datetime.now().replace(day=1) - datetime.timedelta(days=1)
    NEW_CAMPAIGNS_DT = datetime.datetime.now() + datetime.timedelta(days=1)
    client_id = None or steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')
    steps.ClientSteps.link(client_id, 'clientuid32')

    service_id = 7
    contract_id = None
    # product_id = PRODUCT_ID_DIRECT

    service_order_id = steps.OrderSteps.next_id(service_id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=service_id, product_id=1475)
    new_date = datetime.datetime.now().replace(day=1) - datetime.timedelta(days=1)
    update_date(order_id, new_date)
    date = datetime.datetime.now().replace(day=1) - datetime.timedelta(days=1)


    # orders_list = [
    #     {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY,
    #      'BeginDT': date}]
    # request_id = steps.RequestSteps.create(client_id, orders_list,
    #                                        additional_params=dict(InvoiceDesireDT=date))
    #
    # invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, 1003,
    #                                              credit=0,
    #                                              contract_id=contract_id,
    #                                              overdraft=0, endbuyer_id=None)
    #
    # steps.InvoiceSteps.pay(invoice_id, payment_dt=date)
    #
    # steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 12, 'Money': 0}, 0,
    #                                   date)
    # steps.ActsSteps.generate(client_id, force=1, date=date)
    api.test_balance().PayOrderCC({
        'order_id': order_id,
        'paysys_code': 'ce',
        'qty': 100
        # (или str(decimal.Decimal('100.333333')))
    })


invoice_ce_id = db.balance().execute('select id from bo.t_invoice where client_id = :client_id and paysys_id = 1006',
                                     {'client_id': client_id})[0]['id']
db.balance().execute(
    'update (select * from bo.t_invoice where client_id = :client_id and paysys_id = 1006) set dt = :date',
    {'client_id': client_id, 'date': new_date})
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 50, 'Money': 0}, 0, new_date)
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY,
         'BeginDT': date}]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params=dict(InvoiceDesireDT=date))

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, 1003,
                                                 credit=0,
                                                 contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id, payment_dt=date)

    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 112, 'Money': 0}, 0,
                                      date)
    steps.ActsSteps.generate(client_id, force=1, date=date)
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 116, 'Money': 0}, 0,
                                      date)
    # steps.OrderSteps.create(client_id, service_order_id, service_id=service_id, product_id=product_id)
    # orders_list = [
    #     {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}
    # ]
    # request_id = steps.RequestSteps.create(client_id, orders_list)
    # invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, 1003, credit=0, contract_id=None,
    #                                              overdraft=0, endbuyer_id=None)
    # steps.InvoiceSteps.pay(invoice_id, payment_dt=BASE_DT)
    # steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 130, 'Money': 0}, 0, BASE_DT)
    # steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)
    return order_id

def test_2():
    # BASE_DT = datetime.datetime.now().replace(day=1) - datetime.timedelta(days=1)
    NEW_CAMPAIGNS_DT = datetime.datetime.now() + datetime.timedelta(days=1)
    client_id = None or steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')
    steps.ClientSteps.link(client_id, 'clientuid32')

    service_id = 7
    contract_id = None
    # product_id = PRODUCT_ID_DIRECT

    service_order_id = steps.OrderSteps.next_id(service_id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=service_id, product_id=1475)
    new_date = datetime.datetime.now().replace(day=1) - datetime.timedelta(days=1)
    update_date(order_id, new_date)
    date = datetime.datetime.now().replace(day=1) - datetime.timedelta(days=1)


    # orders_list = [
    #     {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY,
    #      'BeginDT': date}]
    # request_id = steps.RequestSteps.create(client_id, orders_list,
    #                                        additional_params=dict(InvoiceDesireDT=date))
    #
    # invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, 1003,
    #                                              credit=0,
    #                                              contract_id=contract_id,
    #                                              overdraft=0, endbuyer_id=None)
    #
    # steps.InvoiceSteps.pay(invoice_id, payment_dt=date)
    #
    # steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 12, 'Money': 0}, 0,
    #                                   date)
    # steps.ActsSteps.generate(client_id, force=1, date=date)
    api.test_balance().PayOrderCC({
        'order_id': order_id,
        'paysys_code': 'ce',
        'qty': 100
        # (или str(decimal.Decimal('100.333333')))
    })
    invoice_ce_id = \
        db.balance().execute('select id from bo.t_invoice where client_id = :client_id and paysys_id = 1006',
                             {'client_id': client_id})[0]['id']
    db.balance().execute(
        'update (select * from bo.t_invoice where client_id = :client_id and paysys_id = 1006) set dt = :date',
        {'client_id': client_id, 'date': new_date})
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 50, 'Money': 0}, 0, new_date)
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY,
         'BeginDT': date}]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params=dict(InvoiceDesireDT=date))

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, 1003,
                                                 credit=0,
                                                 contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id, payment_dt=date)

    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 112, 'Money': 0}, 0,
                                      date)
    steps.ActsSteps.generate(client_id, force=1, date=date)
    # steps.OrderSteps.create(client_id, service_order_id, service_id=service_id, product_id=product_id)
    # orders_list = [
    #     {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}
    # ]
    # request_id = steps.RequestSteps.create(client_id, orders_list)
    # invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, 1003, credit=0, contract_id=None,
    #                                              overdraft=0, endbuyer_id=None)
    # steps.InvoiceSteps.pay(invoice_id, payment_dt=BASE_DT)
    # steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 130, 'Money': 0}, 0, BASE_DT)
    # steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)

    return order_id


def test_3():
    BASE_DT = datetime.datetime.now()
    NEW_CAMPAIGNS_DT = datetime.datetime.now() + datetime.timedelta(days=1)
    client_id = None or steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')
    steps.ClientSteps.link(client_id, 'clientuid32')

    service_id = 7
    # product_id = PRODUCT_ID_DIRECT

    service_order_id = steps.OrderSteps.next_id(service_id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=service_id, product_id=1475)
    new_date = datetime.datetime.now().replace(year=2007)
    update_date(order_id, new_date)
    api.test_balance().PayOrderCC({
        'order_id': order_id,
        'paysys_code': 'ce',
        'qty': 100
        # (или str(decimal.Decimal('100.333333')))
    })
    new_date = datetime.datetime.now().replace(year=2007) + datetime.timedelta(minutes=10)
    invoice_ce_id = \
        db.balance().execute('select id from bo.t_invoice where client_id = :client_id and paysys_id = 1006',
                             {'client_id': client_id})[0]['id']
    update_date_invoice(invoice_ce_id, new_date)
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 50, 'Money': 0}, 0, BASE_DT)
    # steps.OrderSteps.create(client_id, service_order_id, service_id=service_id, product_id=product_id)
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}
    ]
    request_id = steps.RequestSteps.create(client_id, orders_list)
    new_date = datetime.datetime.now().replace(year=2007) + datetime.timedelta(minutes=11)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, 1003, credit=0, contract_id=None,
                                                 overdraft=0, endbuyer_id=None)
    update_date_invoice(invoice_id, new_date)
    steps.InvoiceSteps.pay(invoice_id, payment_dt=new_date)
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 107, 'Money': 0}, 0, BASE_DT)
    steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}
    ]
    request_id = steps.RequestSteps.create(client_id, orders_list)
    # new_date = datetime.datetime.now().replace(year=2007) + datetime.timedelta(minutes=11)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, 1003, credit=0, contract_id=None,
                                                 overdraft=0, endbuyer_id=None)
    # update_date_invoice(invoice_id, new_date)
    steps.InvoiceSteps.pay(invoice_id, payment_dt=BASE_DT)
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 123, 'Money': 0}, 0, BASE_DT)
    # steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)


def test_6():
    BASE_DT = datetime.datetime.now()
    NEW_CAMPAIGNS_DT = datetime.datetime.now() + datetime.timedelta(days=1)
    client_id = None or steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')
    steps.ClientSteps.link(client_id, 'clientuid32')

    service_id = 7
    product_id = 1475

    service_order_id = steps.OrderSteps.next_id(service_id)
    ord_id = steps.OrderSteps.create(client_id, service_order_id, service_id=service_id, product_id=product_id)
    new_date = datetime.datetime.now()- datetime.timedelta(days=2)
    update_date(ord_id, new_date)
    # steps.ClientSteps.migrate_to_currency(client_id, 'MODIFY')
    service_order_id_2 = steps.OrderSteps.next_id(service_id)
    ord_id_2 = steps.OrderSteps.create(client_id, service_order_id_2, service_id=service_id, product_id=product_id)
    update_date(ord_id_2, new_date)
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT},
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id_2, 'Qty': QTY, 'BeginDT': BASE_DT}
    ]
    contract_type = 'opt_agency_prem_post'
    sub_order_ids = [get_order_id(service_order_id,service_id), get_order_id(service_order_id_2, service_id)]
    service_order_id_3 = steps.OrderSteps.next_id(service_id)
    parent_order = steps.OrderSteps.create(client_id, service_order_id_3, service_id=service_id, product_id=product_id)
    update_date(parent_order, new_date)
    # steps.OrderSteps.merge(parent_order, sub_order_ids)
    # steps.OrderSteps.ua_enqueue([client_id])
    request_id = steps.RequestSteps.create(client_id, orders_list)

    # contract_id, _ = steps.ContractSteps.create(contract_type, {'person_id': person_id, 'client_id': client_id,
    #                                                             'SERVICES': [service_id], 'FINISH_DT': NEW_CAMPAIGNS_DT,
    #                                                             'REPAYMENT_ON_CONSUME': 0, 'PERSONAL_ACCOUNT': 1})
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, 1003, credit=0, contract_id=None,
                                                 overdraft=0, endbuyer_id=None)
    update_date_invoice(invoice_id, new_date)
    steps.InvoiceSteps.pay(invoice_id, payment_dt=BASE_DT)
    steps.OrderSteps.merge(parent_order, sub_order_ids)
    steps.OrderSteps.make_optimized(parent_order)

    # db.balance().execute('update bo.t_invoice set TRANSFER_ACTED = 1 where id = :id', {'id': invoice_id})
    campaigns_qty = 20

    # steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': campaigns_qty, 'Money': 0}, 0, BASE_DT)
    # steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)
    campaigns_delta = 2
    # campaigns_qty = campaigns_qty+campaigns_delta
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': campaigns_qty, 'Money': 0}, 0, BASE_DT- datetime.timedelta(days=1))
    campaigns_qty = campaigns_qty+campaigns_delta

    # steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)
    campaigns_qty_2 = 30
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id_2, {'Bucks': campaigns_qty_2, 'Money': 0}, 0, BASE_DT- datetime.timedelta(days=1))
    # steps.ActsSteps.generate(client_id, force=1, date=BASE_DT- datetime.timedelta(days=1))
    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)
    steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)
    # steps.ClientSteps.migrate_to_currency(client_id, 'MODIFY')
    # campaigns_delta_2 = 11
    campaigns_qty_2 = campaigns_qty_2 - campaigns_delta
    # campaigns_qty = campaigns_qty + 5
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': campaigns_qty, 'Money': 0}, 0, BASE_DT- datetime.timedelta(days=1))
    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id_2, {'Bucks': campaigns_qty_2, 'Money': 0}, 0, BASE_DT)


if __name__ == "__main__":
    creator_data_all()