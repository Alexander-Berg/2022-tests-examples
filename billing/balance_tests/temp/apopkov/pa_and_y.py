# -*- coding: utf-8 -*-
import datetime
from balance import balance_steps as steps
import pytest
from btestlib.constants import Paysyses, PersonTypes, Services, Products, ContractCommissionType, Firms, \
    ContractPaymentType, Currencies, Regions
from dateutil.relativedelta import relativedelta
from btestlib import utils
import balance.balance_api as api
from balance import balance_db as db


def test_pa_invoice():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PersonTypes.SW_UR.code)

    dt = datetime.datetime.now()

    contract_id, _ = steps.ContractSteps.create_contract_new(ContractCommissionType.SW_OPT_AGENCY,
                                                             {
                                                                 'CLIENT_ID': client_id,
                                                                 'PERSON_ID': person_id,
                                                                 'SERVICES': [Services.SHOP.id, Services.DIRECT.id],
                                                                 'FIRM': Firms.EUROPE_AG_7.id,
                                                                 'CURRENCY': Currencies.EUR.num_code,
                                                                 'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                                                                 'DT': dt - relativedelta(months=15),
                                                                 'FINISH_DT': dt + relativedelta(months=12),
                                                                 'IS_SIGNED': utils.Date.to_iso(utils.Date.nullify_time_of_date(dt - relativedelta(months=15))),
                                                                 'REPAYMENT_ON_CONSUME': 1,
                                                                 'PERSONAL_ACCOUNT': 1,
                                                                 'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': 0
                                                             })

    service_id = Services.DIRECT.id
    product_id = Products.DIRECT_FISH.id
    now = datetime.datetime.now()
    paysys_id = Paysyses.BANK_SW_UR_EUR.id

    service_order_id = steps.OrderSteps.next_id(service_id=service_id)  # внешний ID заказа

    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=service_id,
                                       product_id=product_id, params={'AgencyID': None})

    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': now}]

    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=now))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=1,
                                                 contract_id=contract_id, overdraft=0, endbuyer_id=None)

    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 50}, 0, now)

    act_id = steps.ActsSteps.generate(client_id, force=1, date=now)[0]


def test_y_invoice():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)

    dt = datetime.datetime.now()

    contract_id, _ = steps.ContractSteps.create_contract_new(ContractCommissionType.OPT_AGENCY,
                                                             {
                                                                 'CLIENT_ID': client_id,
                                                                 'PERSON_ID': person_id,
                                                                 'SERVICES': [Services.SHOP.id, Services.DIRECT.id],
                                                                 'FIRM': Firms.YANDEX_1.id,
                                                                 'CURRENCY': Currencies.RUB.num_code,
                                                                 'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                                                                 'DT': dt - relativedelta(months=15),
                                                                 'FINISH_DT': dt + relativedelta(months=12),
                                                                 'IS_SIGNED': utils.Date.to_iso(utils.Date.nullify_time_of_date(dt - relativedelta(months=15))),
                                                                 'REPAYMENT_ON_CONSUME': 1,
                                                                 'PERSONAL_ACCOUNT': 1,
                                                                 'PERSONAL_ACCOUNT_FICTIVE': 1
                                                             })

    service_id = Services.DIRECT.id
    product_id = Products.DIRECT_FISH.id
    now = datetime.datetime.now()
    paysys_id = Paysyses.BANK_UR_RUB.id

    service_order_id = steps.OrderSteps.next_id(service_id=service_id)  # внешний ID заказа

    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=service_id,
                                       product_id=product_id, params={'AgencyID': None})

    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': now}]

    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=now))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=1,
                                                 contract_id=contract_id, overdraft=0, endbuyer_id=None)

    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 50}, 0, now)

    act_id = steps.ActsSteps.generate(client_id, force=1, date=now)[0]


def test_y_invoice_toloka():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PersonTypes.PH.code)

    dt = datetime.datetime.now()

    contract_id, _ = steps.ContractSteps.create_contract_new(ContractCommissionType.NO_AGENCY,
                                                             {
                                                                 'CLIENT_ID': client_id,
                                                                 'PERSON_ID': person_id,
                                                                 'SERVICES': [Services.TOLOKA.id],
                                                                 'FIRM': Firms.JAMS_120.id,
                                                                 'CURRENCY': Currencies.RUB.num_code,
                                                                 'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                                                                 'DT': dt - relativedelta(months=15),
                                                                 'FINISH_DT': dt + relativedelta(months=12),
                                                                 'IS_SIGNED': utils.Date.to_iso(utils.Date.nullify_time_of_date(dt - relativedelta(months=15))),
                                                                 'REPAYMENT_ON_CONSUME': 1,
                                                                 'PERSONAL_ACCOUNT': 1,
                                                                 'PERSONAL_ACCOUNT_FICTIVE': 1,
                                                                 'PARTNER_CREDIT': 1,

                                                             })

    service_id = Services.TOLOKA.id
    product_id = Products.TOLOKA.id
    now = datetime.datetime.now()
    paysys_id = Paysyses.BANK_PH_RUB.id

    service_order_id = steps.OrderSteps.next_id(service_id=service_id)  # внешний ID заказа

    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=service_id,
                                       product_id=product_id, params={'AgencyID': None})

    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': now}]

    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=now))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=1,
                                                 contract_id=contract_id, overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id)

    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 50}, 0, now)

    act_id = steps.ActsSteps.generate(client_id, force=1, date=now)[0]

    api.test_balance().ExportObject('OEBS', 'Client', client_id, 0, None, None)
    api.test_balance().ExportObject('OEBS', 'Contract', contract_id, 0, None, None)

    y_invoice_id = [i['id'] for i in steps.InvoiceSteps.get_invoice_data_by_client_with_ids(client_id)
                    if i['id'] != invoice_id][0]
    api.test_balance().ExportObject('OEBS', 'Invoice', y_invoice_id, 0, None, None)
    # api.test_balance().ExportObject('OEBS', 'Invoice', invoice_id, 0, None, None)
    api.test_balance().ExportObject('OEBS', 'Act', act_id, 0, None, None)

    print 'Client: {}'.format(client_id)
    print 'Contract: {}'.format(contract_id)
    print 'Invoice: {}'.format(y_invoice_id)
    # print 'Invoice: {}'.format(invoice_id)
    print 'Act: {}'.format(act_id)


def test_y_invoice_toloka_16():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PersonTypes.SW_YT.code)

    dt = datetime.datetime.now()

    contract_id, _ = steps.ContractSteps.create_contract_new(ContractCommissionType.NO_AGENCY,
                                                             {
                                                                 'CLIENT_ID': client_id,
                                                                 'PERSON_ID': person_id,
                                                                 'SERVICES': [Services.TOLOKA.id],
                                                                 'FIRM': Firms.SERVICES_AG_16.id,
                                                                 'CURRENCY': Currencies.USD.num_code,
                                                                 'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                                                                 'DT': dt - relativedelta(months=15),
                                                                 'FINISH_DT': dt + relativedelta(months=12),
                                                                 'IS_SIGNED': utils.Date.to_iso(utils.Date.nullify_time_of_date(dt - relativedelta(months=15))),
                                                                 'REPAYMENT_ON_CONSUME': 1,
                                                                 'PERSONAL_ACCOUNT': 1,
                                                                 'PERSONAL_ACCOUNT_FICTIVE': 1,
                                                                 'PARTNER_CREDIT': 1,

                                                             })

    service_id = Services.TOLOKA.id
    product_id = Products.TOLOKA.id
    now = datetime.datetime.now()
    paysys_id = Paysyses.BANK_SW_YT_USD.id

    service_order_id = steps.OrderSteps.next_id(service_id=service_id)  # внешний ID заказа

    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=service_id,
                                       product_id=product_id, params={'AgencyID': None})

    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': now}]

    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=now))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=1,
                                                 contract_id=contract_id, overdraft=0, endbuyer_id=None)

    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 50}, 0, now)

    act_id = steps.ActsSteps.generate(client_id, force=1, date=now)[0]


def test_y_invoice_vzglyad():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PersonTypes.YT.code)

    dt = datetime.datetime.now()

    contract_id, _ = steps.ContractSteps.create_contract_new(ContractCommissionType.NO_AGENCY,
                                                             {
                                                                 'CLIENT_ID': client_id,
                                                                 'PERSON_ID': person_id,
                                                                 'SERVICES': [Services.VZGLYAD.id],
                                                                 'FIRM': Firms.YANDEX_1.id,
                                                                 'CURRENCY': Currencies.USD.num_code,
                                                                 'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                                                                 'DT': dt - relativedelta(months=15),
                                                                 'FINISH_DT': dt + relativedelta(months=12),
                                                                 'IS_SIGNED': utils.Date.to_iso(utils.Date.nullify_time_of_date(dt - relativedelta(months=15))),
                                                                 'REPAYMENT_ON_CONSUME': 1,
                                                                 'PERSONAL_ACCOUNT': 1,
                                                                 'PERSONAL_ACCOUNT_FICTIVE': 1,
                                                                 'DEAL_PASSPORT': utils.Date.to_iso(utils.Date.nullify_time_of_date(dt - relativedelta(months=15)))
                                                             })

    service_id = Services.VZGLYAD.id
    product_id = Products.VZGLYAD.id
    now = datetime.datetime.now()
    paysys_id = Paysyses.BANK_YT_USD.id

    service_order_id = steps.OrderSteps.next_id(service_id=service_id)  # внешний ID заказа

    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=service_id,
                                       product_id=product_id, params={'AgencyID': None})

    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': now}]

    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=now))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=1,
                                                 contract_id=contract_id, overdraft=0, endbuyer_id=None)

    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Units': 50}, 0, now)

    act_id = steps.ActsSteps.generate(client_id, force=1, date=now)[0]


def test_y_invoice_34_shad():
    client_id = steps.ClientSteps.create()
    # client_id = steps.ClientSteps.create({'REGION_ID': Regions.RU.id})
    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)

    dt = datetime.datetime.now()

    contract_id, _ = steps.ContractSteps.create_contract_new(ContractCommissionType.OPT_AGENCY,
                                                             {
                                                                 'CLIENT_ID': client_id,
                                                                 'PERSON_ID': person_id,
                                                                 'SERVICES': [Services.SHOP.id],
                                                                 'FIRM': Firms.SHAD_34.id,
                                                                 'CURRENCY': Currencies.RUB.num_code,
                                                                 'PAYMENT_TYPE': ContractPaymentType.PREPAY,
                                                                 'DT': dt - relativedelta(months=15),
                                                                 'FINISH_DT': dt + relativedelta(months=12),
                                                                 'IS_SIGNED': utils.Date.to_iso(utils.Date.nullify_time_of_date(dt - relativedelta(months=15))),
                                                                 # 'REPAYMENT_ON_CONSUME': 1,
                                                                 # 'PERSONAL_ACCOUNT': 1,
                                                                 # 'PERSONAL_ACCOUNT_FICTIVE': 1,
                                                                 'DEAL_PASSPORT': utils.Date.to_iso(utils.Date.nullify_time_of_date(dt - relativedelta(months=15)))
                                                             })

    api.test_balance().ExportObject('OEBS', 'Client', client_id, 0, None, None)
    api.test_balance().ExportObject('OEBS', 'Contract', contract_id, 0, None, None)

    query = 'select id from T_CONTRACT_COLLATERAL where contract2_id = :contract_id'
    contract_collateral = db.balance().execute(query, {'contract_id': contract_id})[0]['id']

    api.test_balance().ExportObject('OEBS', 'ContractCollateral', contract_collateral, 0, None, None)

