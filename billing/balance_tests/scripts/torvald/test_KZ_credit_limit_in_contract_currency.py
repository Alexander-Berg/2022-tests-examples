# -*- coding: utf-8 -*-

import datetime

import pytest
from hamcrest import has_entries

from balance import balance_api as api
from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils

DIRECT_KZ_PRICE = 105
MULTICURRENCY_KZ_PRICE = 1

SELF_PAYSYS_ID = 1021
SUB_PAYSYS_ID = 1060

ORANGE_ID = 1020474
ORANGE_PERSON_ID = 3862317
ORANGE_CONTRACT_ID = 278780

KZ_DISCOUNT_POLICY_ID = 18

FIXED_ORANGE_DISCOUNT = 18

SERVICE_ID = 7
PRODUCT_ID = 1475
PAYSYS_ID = 1003
QTY = 100
COMPLETION_QTY = 75.345
BASE_DT = datetime.datetime.now()

# 'scenario' structure:
# [service_id | product_id | qty | completion_part


@pytest.mark.parametrize ('scenario',
    [
    # 0.
                {'acts': [(7, 1475, 1000, datetime.datetime(2015,10,1))
                         ,(7, 1475, 1000, datetime.datetime(2015,11,1))
                         ,(7, 1475, 1000, datetime.datetime(2015,12,1))
                         ,(7, 1475, 1000, datetime.datetime(2016,1,1))],
                'limit': 10000}
    ]
)
def test_other_agency(scenario):
    client_id = None or steps.ClientSteps.create({'IS_AGENCY': 0, 'NAME': u'Petrov3'})
    agency_id = None or steps.ClientSteps.create({'IS_AGENCY': 1, 'name': u'Иванов Иван Иванович'})  ##mass
    # agency_id = 9099443
    order_owner = client_id
    invoice_owner = agency_id
    person_id = None or steps.PersonSteps.create(invoice_owner, 'yt_kzu', {'phone': '234'})
    # person_id = 3851793

    contract_id, contract_eid = steps.ContractSteps.create_contract('opt_agency_post_kz',
                                                                    {'CLIENT_ID': agency_id, 'PERSON_ID': person_id
                                                                        , 'DT': '2015-01-01T00:00:00'
                                                                        , 'FINISH_DT': '2017-12-31T00:00:00'
                                                                        , 'IS_SIGNED': '2015-01-01T00:00:00'
                                                                        , 'SERVICES': [7, 11, 37, 67, 70, 77]
                                                                        , 'DISCOUNT_POLICY_TYPE': KZ_DISCOUNT_POLICY_ID
                                                                        , 'PERSONAL_ACCOUNT': 1
                                                                        , 'LIFT_CREDIT_ON_PAYMENT': 1
                                                                        , 'PERSONAL_ACCOUNT_FICTIVE': 1
                                                                        , 'CURRENCY': 398
                                                                        , 'BANK_DETAILS_ID': 320
                                                                        , 'DEAL_PASSPORT': '2015-12-01T00:00:00'
                                                                        , 'CREDIT_LIMIT_IN_CONTRACT_CURRENCY': 1
                                                                        , 'CREDIT_LIMIT_SINGLE': 1000001
                                                                     }
                                                                    )
    # contract_id = 274798

    # ---------- Budget ----------
    for service_id, product_id, completions, act_dt in scenario['acts']:
        temp_client_id = None or steps.ClientSteps.create({'IS_AGENCY': 0, 'NAME': u'Petrov3'})

        service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
        steps.OrderSteps.create(temp_client_id, service_order_id, service_id=service_id, product_id=product_id,
                                params={'AgencyID': agency_id})
        service_order_id2 = steps.OrderSteps.next_id(SERVICE_ID)
        steps.OrderSteps.create(temp_client_id, service_order_id2, service_id=service_id, product_id=product_id,
                                params={'AgencyID': agency_id})
        orders_list = [
            {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': completions, 'BeginDT': act_dt}
            , {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id2, 'Qty': completions, 'BeginDT': act_dt}
        ]

        request_id = steps.RequestSteps.create(invoice_owner, orders_list, additional_params=dict(InvoiceDesireDT=act_dt))
        invoice_id, external_id, total_sum = steps.InvoiceSteps.create(request_id, person_id, SUB_PAYSYS_ID, credit=1,
                                                                       contract_id=contract_id, overdraft=0,
                                                                       endbuyer_id=None)
        steps.InvoiceSteps.pay(invoice_id, payment_dt = act_dt)

        if service_id in [7,11]:
            completion_params = {'Bucks': completions}
        elif service_id == 37:
            completion_params ={'Days': completions}

        steps.CampaignsSteps.do_campaigns(service_id, orders_list[0]['ServiceOrderID'],
                                      completion_params, 0, act_dt)
        steps.ActsSteps.generate(invoice_owner, force=1, date=act_dt)
        y_invoice_list = db.get_y_invoices_by_fpa_invoice(invoice_id)
        steps.InvoiceSteps.pay(y_invoice_list[0]['id'])
    pass






    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(order_owner, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                                       params={'AgencyID': agency_id})
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}
    ]
    request_id = steps.RequestSteps.create(invoice_owner, orders_list, additional_params=dict(InvoiceDesireDT=BASE_DT))
    invoice_id, external_id, total_sum = steps.InvoiceSteps.create(request_id, person_id, SUB_PAYSYS_ID, credit=1,
                                                                   contract_id=contract_id, overdraft=0,
                                                                   endbuyer_id=None)

    if SERVICE_ID in [7,11]:
        completion_params = {'Bucks': QTY}
    elif SERVICE_ID == 37:
        completion_params ={'Days': QTY}

    steps.CampaignsSteps.do_campaigns(SERVICE_ID, orders_list[0]['ServiceOrderID'],
                                      completion_params, 0, BASE_DT)
    steps.ActsSteps.generate(invoice_owner, date=BASE_DT)


    #-----------------------------------------------------------------------------------------------------------------

    service_id, product_id, completions, target_invoice_dt = scenario['target_invoice']

    # ---------- EstimateDiscount ----------
    result = steps.CommonSteps.log(api.medium().server.Balance.EstimateDiscount)(
                {'ClientID': invoice_owner, 'PaysysID': SUB_PAYSYS_ID, 'ContractID': contract_id},
                [{'ProductID': product_id, 'ClientID': order_owner, 'Qty': QTY, 'ID': 1, 'BeginDT': target_invoice_dt,
                 'RegionID': None,'discard_agency_discount': 0}
                ])
    # assert result['AgencyDiscountPct'] == str(scenario['expected_discount_pct'])

    # ---------- GetClientDiscountsAll -----------
    result = steps.CommonSteps.log(api.medium().server.Balance.GetClientDiscountsAll)({'ClientID': invoice_owner})
    # expected = [{'Currency': 'RUB',
    #              'NextDiscount': None,
    #              'Budget': '1200000',
    #              'Discount': '30',
    #              'NextBudget': None,
    #              'ContractExternalID': contract_eid,
    #              'DiscountName': 'kz_client',
    #              'DiscountType': 13,
    #              'DiscountIDs': [1, 13]}]
    # utils.check_that(expected, mtch.FullMatch(result))

    # ---------- Request ----------
    service_order_id = steps.OrderSteps.next_id(service_id)
    order_id = steps.OrderSteps.create(order_owner, service_order_id, service_id=service_id, product_id=product_id,
                                       params={'AgencyID': agency_id})
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': target_invoice_dt}
    ]
    request_id = steps.RequestSteps.create(invoice_owner, orders_list, additional_params=dict(InvoiceDesireDT=target_invoice_dt))
    invoice_id, external_id, total_sum = steps.InvoiceSteps.create(request_id, person_id, SUB_PAYSYS_ID, credit=1,
                                                                   contract_id=contract_id, overdraft=0,
                                                                   endbuyer_id=None)

    if service_id in [7,11]:
        completion_params = {'Bucks': completions}
    elif service_id == 37:
        completion_params ={'Days': completions}

    steps.CampaignsSteps.do_campaigns(service_id, orders_list[0]['ServiceOrderID'],
                                      completion_params, 0, target_invoice_dt)
    steps.ActsSteps.generate(invoice_owner, date=target_invoice_dt)

    consume = db.get_consumes_by_invoice(invoice_id)[0]
    utils.check_that(consume, has_entries({
        'consume_sum': scenario['expected_consume_sum'],
        'static_discount_pct': scenario['expected_discount_pct']
    }))


if __name__ == "__main__":
    # test_simple_client()
    pytest.main("-v test_2016_KZ_MEDIA_agency_discount.py")
