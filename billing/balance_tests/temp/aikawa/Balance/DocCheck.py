import datetime

from balance import balance_db as db
from balance import balance_steps as steps

dt = datetime.datetime.now()

# steps.InvoiceSteps.pay_fair(48445266, payment_sum=None, payment_dt=None)
# current_year = datetime.date.today().year
# current_month = datetime.date.today().month
# _, last_day = calendar.monthrange(current_year, current_month)
# previous_month_not_the_last_day = (dt.replace(day=1) - datetime.timedelta(days=2))
# current_month_the_last_day = dt.replace(day=last_day, hour=0, minute=0, second=0, microsecond=0)
# print current_month_the_last_day
# previous_month_the_last_day = (dt.replace(day=1) - datetime.timedelta(days=1))
# print previous_month_the_last_day
#
# working_host = cfg.working_host.host
# print working_host

#
#
PERSON_TYPE = 'ur'
PAYSYS_ID = 1003
SERVICE_ID = 7
PRODUCT_ID = 1475
MSR = 'Bucks'
contract_type = 'no_agency_post'

def check_1():
    PERSON_TYPE = 'ur'
    PAYSYS_ID = 1003

    MSR = 'Bucks'
    contract_type = 'no_agency_post'
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, 'aikawa-test-0')
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

    db.balance().execute('''
    UPDATE (
    select DELIVERY_TYPE from T_PERSON WHERE id = :person_id) set DELIVERY_TYPE = 3''', {'person_id':person_id})

    SERVICE_ID = 11
    PRODUCT_ID = 2136

    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                       service_order_id=service_order_id)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))

    contract_id, _ = steps.ContractSteps.create_contract(contract_type, {'CLIENT_ID': client_id, 'PERSON_ID': person_id,
                                                                         'DT': '2015-04-30T00:00:00',
                                                                         'FINISH_DT': '2016-08-30T00:00:00',
                                                                         'IS_SIGNED': '2015-01-01T00:00:00',
                                                                         'SERVICES': [11],
                                                                         'FIRM': 111})

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=contract_id, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 200}, 0, dt)

    print steps.ActsSteps.generate(client_id, force=1, date=dt)

    SERVICE_ID = 7
    PRODUCT_ID = 1475

    for x in range(3):
        service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
        order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                           service_order_id=service_order_id)
        orders_list = [
            {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt}
        ]
        request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))

        contract_id, _ = steps.ContractSteps.create_contract(contract_type,
                                                             {'CLIENT_ID': client_id, 'PERSON_ID': person_id,
                                                              'DT': '2015-04-30T00:00:00',
                                                              'FINISH_DT': '2016-08-30T00:00:00',
                                                              'IS_SIGNED': '2015-01-01T00:00:00',
                                                              'SERVICES': [7],
                                                              'FIRM': 1})

        invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                     credit=0, contract_id=contract_id, overdraft=0, endbuyer_id=None)
        steps.InvoiceSteps.pay(invoice_id)

        steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 200}, 0, dt)

        print steps.ActsSteps.generate(client_id, force=1, date=dt)


check_1()


# steps.ClientSteps.link(1913313, 'aikawa-test-0')

def check_2():
    PERSON_TYPE = 'ur'
    PAYSYS_ID = 1003
    SERVICE_ID = 7
    PRODUCT_ID = 1475
    MSR = 'Bucks'
    contract_type = 'no_agency_post'
    client_id = steps.ClientSteps.create()
    # steps.ClientSteps.link(client_id, 'clientuid45')
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

    # db.balance().execute('''
    # UPDATE (
    # select DELIVERY_TYPE from T_PERSON WHERE id = :person_id) set DELIVERY_TYPE = 0''', {'person_id':person_id})
    #
    # service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    # order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
    #                                    service_order_id=service_order_id)
    # orders_list = [
    #     {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt}
    # ]
    # request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))
    #
    #
    # invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
    #                                              credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    # steps.InvoiceSteps.pay(invoice_id)
    #
    # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 200}, 0, dt)
    #
    # print steps.ActsSteps.generate(client_id, force=1, date=dt)
    #
    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                       service_order_id=service_order_id)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))

    contract_id, _ = steps.ContractSteps.create_contract(contract_type, {'CLIENT_ID': client_id, 'PERSON_ID': person_id,
                                                                         'DT': '2015-04-30T00:00:00',
                                                                         'FINISH_DT': '2016-08-30T00:00:00',
                                                                         'IS_SIGNED': '2015-01-01T00:00:00',
                                                                         'SERVICES': [7],
                                                                         'FIRM': 1})

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=1, contract_id=contract_id, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 200}, 0, dt)

    print steps.ActsSteps.generate(client_id, force=1, date=dt)

# check_2()


def check_3():
    PERSON_TYPE = 'ur'
    PAYSYS_ID = 1003
    SERVICE_ID = 7
    PRODUCT_ID = 1475
    MSR = 'Bucks'
    contract_type = 'no_agency_post'
    client_id = steps.ClientSteps.create()
    agency_id = steps.ClientSteps.create_agency()
    steps.ClientSteps.link(agency_id, 'aikawa-test-0')
    steps.ClientSteps.link(client_id, 'aikawa-test-10')
    person_id = steps.PersonSteps.create(agency_id, PERSON_TYPE)

    db.balance().execute('''
    UPDATE (
    select DELIVERY_TYPE from T_PERSON WHERE id = :person_id) set DELIVERY_TYPE = 1''', {'person_id':person_id})

    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                       service_order_id=service_order_id, params={'AgencyID': agency_id})
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=agency_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))


    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 200}, 0, dt)

    print steps.ActsSteps.generate(agency_id, force=1, date=dt)

# check_3()

# steps.ClientSteps.link(14188028, 'clientuid45')
