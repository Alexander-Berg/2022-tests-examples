# -*- coding: utf-8 -*-

import datetime

import btestlib.reporter as reporter
from balance import balance_steps as steps

DIRECT = 7
DIRECT_PRODUCT = 1475
MARKET = 11
MARKET_PRODUCT = 2136
MEDIASELLING = 70
MEDIASELLING_PRODUCT = 503306
PAYSYS_ID = 1003
QTY = 50
BASE_DT = datetime.datetime.now()

ORDERS_COUNT = 13


def data_generator(type_=None):
    client_id = None or steps.ClientSteps.create()
    # db.balance().execute('''Update t_client set FULLNAME = u'UL Nonres', CURRENCY_PAYMENT = 'USD', ISO_CURRENCY_PAYMENT = 'USD', IS_NON_RESIDENT = 1
    #                     where ID = :client_id ''',
    #         {'client_id':client_id})

    # Agency:
    agency_id = None or steps.ClientSteps.create({'IS_AGENCY': 1})
    order_owner = client_id
    invoice_owner = agency_id if agency_id is not None else client_id
    # Product:
    service_id = DIRECT
    product_id = DIRECT_PRODUCT

    person_id = None or steps.PersonSteps.create(invoice_owner, 'ur')

    if type_ == 'fictive':
        credit_flag = 1
        # Agency:
        #    no changes
        # Product:
        #    no changes

        contract_id, _ = steps.ContractSteps.create_contract('comm_post',
                                                             {'CLIENT_ID': invoice_owner, 'PERSON_ID': person_id,
                                                              'DT': '2015-04-30T00:00:00',
                                                              'FINISH_DT': '2016-06-30T00:00:00',
                                                              'IS_SIGNED': '2015-01-01T00:00:00',
                                                              'SERVICES': [service_id],
                                                              # 'COMMISSION_TYPE': 57,
                                                              'NON_RESIDENT_CLIENTS': 0,
                                                              # 'REPAYMENT_ON_CONSUME': 0,
                                                              # 'PERSONAL_ACCOUNT': 1,
                                                              # 'LIFT_CREDIT_ON_PAYMENT': 0,
                                                              # 'PERSONAL_ACCOUNT_FICTIVE': 0
                                                              })

    if type_ == 'fictive_personal_account':
        credit_flag = 1
        # Agency
        #    no changes
        # Product:
        #    no changes

        contract_id, _ = steps.ContractSteps.create_contract('opt_prem',
                                                             {'CLIENT_ID': invoice_owner, 'PERSON_ID': person_id,
                                                              'DT': '2015-04-30T00:00:00',
                                                              'FINISH_DT': '2016-06-30T00:00:00',
                                                              'IS_SIGNED': '2015-01-01T00:00:00',
                                                              'SERVICES': [service_id],
                                                              # 'COMMISSION_TYPE': 57,
                                                              # 'NON_RESIDENT_CLIENTS': 1,
                                                              # 'REPAYMENT_ON_CONSUME': 0,
                                                              # 'PERSONAL_ACCOUNT': 1,
                                                              # 'LIFT_CREDIT_ON_PAYMENT': 0,
                                                              # 'PERSONAL_ACCOUNT_FICTIVE': 0
                                                              })
        steps.ContractSteps.create_collateral(1033, {'CONTRACT2_ID': contract_id, 'DT': '2015-04-30T00:00:00',
                                                     'IS_SIGNED': '2015-01-01T00:00:00'})

    if type_ == 'personal_account':
        credit_flag = 1
        # Agency:
        agency_id = None
        order_owner = client_id
        invoice_owner = agency_id if agency_id is not None else client_id
        # Product:
        service_id = MARKET
        product_id = MARKET_PRODUCT

        contract_id, _ = steps.ContractSteps.create_contract('no_agency_post',
                                                             {'CLIENT_ID': invoice_owner, 'PERSON_ID': person_id,
                                                              'DT': '2015-04-30T00:00:00',
                                                              'FINISH_DT': '2016-06-30T00:00:00',
                                                              'IS_SIGNED': '2015-01-01T00:00:00',
                                                              'SERVICES': [service_id],
                                                              # 'COMMISSION_TYPE': 57,
                                                              'NON_RESIDENT_CLIENTS': 0,
                                                              # 'REPAYMENT_ON_CONSUME': 0,
                                                              'PERSONAL_ACCOUNT': 1,
                                                              # 'LIFT_CREDIT_ON_PAYMENT': 0,
                                                              # 'PERSONAL_ACCOUNT_FICTIVE': 0
                                                              })

    if type_ == 'prepayment_wo_contract':
        credit_flag = 0
        # Agency
        #    no changes
        # Product:
        #    no changes

        contract_id = None

    if type_ == 'prepayment_with_contract':
        credit_flag = 0
        # Agency:
        agency_id = None
        order_owner = client_id
        invoice_owner = agency_id if agency_id is not None else client_id
        # Product:
        #    no changes

        contract_id, _ = steps.ContractSteps.create_contract('no_agency_post',
                                                             {'CLIENT_ID': invoice_owner, 'PERSON_ID': person_id,
                                                              'DT': '2015-04-30T00:00:00',
                                                              'FINISH_DT': '2016-06-30T00:00:00',
                                                              'IS_SIGNED': '2015-01-01T00:00:00',
                                                              'SERVICES': [service_id],
                                                              # 'COMMISSION_TYPE': 57,
                                                              'NON_RESIDENT_CLIENTS': 0,
                                                              # 'REPAYMENT_ON_CONSUME': 0,
                                                              'PERSONAL_ACCOUNT': 1,
                                                              'LIFT_CREDIT_ON_PAYMENT': 0,
                                                              'PERSONAL_ACCOUNT_FICTIVE': 0
                                                              })

    if type_ == 'fpa_trp':
        credit_flag = 1
        # Agency:
        #    no changes
        # Product:
        service_id = MEDIASELLING
        product_id = MEDIASELLING_PRODUCT

        contract_id, _ = steps.ContractSteps.create_contract('opt_prem',
                                                             {'CLIENT_ID': invoice_owner, 'PERSON_ID': person_id,
                                                              'DT': '2015-04-30T00:00:00',
                                                              'FINISH_DT': '2016-06-30T00:00:00',
                                                              'IS_SIGNED': '2015-01-01T00:00:00',
                                                              'SERVICES': [service_id],
                                                              # 'COMMISSION_TYPE': 57,
                                                              # 'NON_RESIDENT_CLIENTS': 1,
                                                              # 'REPAYMENT_ON_CONSUME': 0,
                                                              # 'PERSONAL_ACCOUNT': 1,
                                                              # 'LIFT_CREDIT_ON_PAYMENT': 0,
                                                              # 'PERSONAL_ACCOUNT_FICTIVE': 0
                                                              })
        steps.ContractSteps.create_collateral(1033, {'CONTRACT2_ID': contract_id, 'DT': '2015-04-30T00:00:00',
                                                     'IS_SIGNED': '2015-01-01T00:00:00'})

    if type_ == 'several_acts_in_y_invoice':
        credit_flag = 1
        # Agency
        #    no changes
        # Product:
        #    no changes

        contract_id, _ = steps.ContractSteps.create_contract('opt_prem',
                                                             {'CLIENT_ID': invoice_owner, 'PERSON_ID': person_id,
                                                              'DT': '2015-04-30T00:00:00',
                                                              'FINISH_DT': '2016-06-30T00:00:00',
                                                              'IS_SIGNED': '2015-01-01T00:00:00',
                                                              'SERVICES': [7, 11],
                                                              # 'COMMISSION_TYPE': 57,
                                                              # 'NON_RESIDENT_CLIENTS': 1,
                                                              # 'REPAYMENT_ON_CONSUME': 0,
                                                              # 'PERSONAL_ACCOUNT': 1,
                                                              # 'LIFT_CREDIT_ON_PAYMENT': 0,
                                                              # 'PERSONAL_ACCOUNT_FICTIVE': 0
                                                              })
        steps.ContractSteps.create_collateral(1033, {'CONTRACT2_ID': contract_id, 'DT': '2015-04-30T00:00:00',
                                                     'IS_SIGNED': '2015-01-01T00:00:00'})

    service_order_ids = []
    order_ids = []
    orders_list = []
    for i in xrange(ORDERS_COUNT):
        reporter.log(('--------------------------{}-------------------------'.format(i)))
        service_order_ids.append(steps.OrderSteps.next_id(service_id))
        order_ids.append(
            steps.OrderSteps.create(order_owner, service_order_ids[i], service_id=service_id, product_id=product_id,
                                    params={'AgencyID': agency_id}))
        orders_list.append(
            {'ServiceID': service_id, 'ServiceOrderID': service_order_ids[i], 'Qty': QTY, 'BeginDT': BASE_DT})

    request_id = steps.RequestSteps.create(invoice_owner, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=credit_flag,
                                                 contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)
    if credit_flag == 0:
        steps.InvoiceSteps.pay(invoice_id)

    if type_ == 'several_acts_in_y_invoice':
        for i in xrange(3):
            service_id = MARKET
            product_id = MARKET_PRODUCT
            service_order_ids_market = []

            reporter.log(('--------------------------{}-------------------------'.format(i)))
            service_order_ids_market.append(steps.OrderSteps.next_id(service_id))
            order_ids.append(steps.OrderSteps.create(order_owner, service_order_ids_market[i], service_id=service_id,
                                                     product_id=product_id,
                                                     params={'AgencyID': agency_id}))
            orders_list.append({'ServiceID': service_id, 'ServiceOrderID': service_order_ids_market[i], 'Qty': QTY,
                                'BeginDT': BASE_DT})

            request_id = steps.RequestSteps.create(invoice_owner, orders_list)
            invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=credit_flag,
                                                         contract_id=contract_id,
                                                         overdraft=0, endbuyer_id=None)
        service_order_ids += service_order_ids_market

    for i in xrange(ORDERS_COUNT):
        if type_ == 'fpa_trp':
            steps.CampaignsSteps.do_campaigns(service_id, service_order_ids[i], {'Bucks': 0, 'Money': 49}, 0, BASE_DT)
        else:
             steps.CampaignsSteps.do_campaigns(service_id, service_order_ids[i], {'Bucks': 0.25 * i, 'Money': 0}, 0,
                                              BASE_DT)

    steps.ActsSteps.generate(invoice_owner, force=1, date=BASE_DT)


def test_fictive_personal_account_with_multiple_act_trans():
    data_generator(type_='fictive_personal_account')


def test_fictive_with_multiple_act_trans():
    data_generator(type_='fictive')


def test_personal_account_with_multiple_act_trans():
    data_generator(type_='personal_account')


def test_prepayment_wo_contract_with_multiple_act_trans():
    data_generator(type_='prepayment_wo_contract')


def test_prepayment_with_contract_with_multiple_act_trans():
    data_generator(type_='prepayment_with_contract')


def test_fpa_trp_with_multiple_act_trans():
    data_generator(type_='fpa_trp')


def test_several_acts_in_y_invoice_with_multiple_act_trans():
    data_generator(type_='several_acts_in_y_invoice')


if __name__ == "__main__":
    # test_fictive_personal_account_with_multiple_act_trans()
    # test_fictive_with_multiple_act_trans()
    # test_personal_account_with_multiple_act_trans()
    # test_prepayment_wo_contract_with_multiple_act_trans()
    # test_prepayment_with_contract_with_multiple_act_trans()
    # test_fpa_trp_with_multiple_act_trans()
    test_several_acts_in_y_invoice_with_multiple_act_trans()
