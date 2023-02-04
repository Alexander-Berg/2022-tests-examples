# -*- coding: utf-8 -*-

import datetime

import pytest
from hamcrest import equal_to

import btestlib.reporter as reporter
from balance import balance_api as api
from balance import balance_db as db
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils as utils

PAYSYS_ID = 1014

BASE_DT = datetime.datetime.now()
to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta


# DISC_DT = BASE_DT - datetime.timedelta(days=30)




@reporter.feature(Features.DISCOUNT)
@pytest.mark.parametrize('scenario', [
    # 1: 1 месяц
    {'acts': [[7, 1475, 787, datetime.datetime(2016, 6, 10, 0, 0, 0)]
        , [7, 1475, 787, datetime.datetime(2016, 6, 10, 0, 0, 0)]
        , [7, 1475, 787, datetime.datetime(2016, 6, 10, 0, 0, 0)]
        , [7, 1475, 787, datetime.datetime(2016, 6, 10, 0, 0, 0)]
        , [7, 1475, 787, datetime.datetime(2016, 6, 10, 0, 0, 0)]],
     'req_dt': datetime.datetime(2016, 7, 11, 0, 0, 0),
     'contract_start_dt': '2016-05-30T00:00:00',
     'discount_type': 16,
     'expected_discount_pct': 12}
])
def test_CIS_2016_agency_discount_new_contract(scenario):
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    invoice_owner = agency_id
    person_id = steps.PersonSteps.create(invoice_owner, 'yt', {})
    contract_id, _ = steps.ContractSteps.create_contract('opt_agency',
                                                         {'CLIENT_ID': agency_id, 'PERSON_ID': person_id,
                                                          'DT': scenario['contract_start_dt'],
                                                          'FINISH_DT': to_iso(BASE_DT + dt_delta(days=180)),
                                                          'FIRM': 1,
                                                          # 'CREDIT_LIMIT_SINGLE': 600000000,
                                                          'DISCOUNT_POLICY_TYPE': scenario['discount_type']
                                                          })
    for service_id, product_id, completions, dt in scenario['acts']:
        dt_1st = dt.replace(day=1)
        tmp_client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
        campaigns_list = [
            {'client_id': tmp_client_id, 'service_id': service_id, 'product_id': product_id, 'qty': completions,
             'begin_dt': dt_1st}
        ]
        invoice_id, external_id, total_sum, orders_list = steps.InvoiceSteps.create_force_invoice(tmp_client_id,
                                                                                                  person_id,
                                                                                                  campaigns_list,
                                                                                                  PAYSYS_ID,
                                                                                                  dt_1st,
                                                                                                  agency_id=agency_id,
                                                                                                  # TODO: for fpa
                                                                                                  credit=scenario[
                                                                                                      'is_fpa_budget'] if 'is_fpa_budget' in scenario else 0,
                                                                                                  contract_id=contract_id,
                                                                                                  overdraft=0,
                                                                                                  manager_uid=None)
        steps.InvoiceSteps.pay(invoice_id)
        steps.CampaignsSteps.do_campaigns(service_id, orders_list[0]['ServiceOrderID'],
                                          {'Bucks': completions, 'Money': 0}, 0, dt_1st)
        steps.ActsSteps.generate(invoice_owner, 1, dt)

    steps.CommonSteps.log(api.medium().server.Balance.GetClientDiscountsAll)(
        {'ClientID': agency_id, 'DT': scenario['req_dt']})
    # TODO: check

    # ---------- Request ----------
    service_order_id = steps.OrderSteps.next_id(service_id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=service_id, product_id=product_id,
                                       params={'AgencyID': agency_id})
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': scenario['req_dt']}
    ]
    # request_id = steps.RequestSteps.create(invoice_owner, orders_list, additional_params={'invoice_dt': scenario['req_dt']})
    request_id = steps.RequestSteps.create(invoice_owner, orders_list,
                                           additional_params={'InvoiceDesireDT': scenario['req_dt']})
    invoice_id, external_id, total_sum = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID,
                                                                   credit=scenario[
                                                                       'is_credit'] if 'is_credit' in scenario else 0,
                                                                   contract_id=contract_id, overdraft=0,
                                                                   endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    consume = db.get_consumes_by_invoice(invoice_id)[0]
    utils.check_that(consume['static_discount_pct'], equal_to(scenario['expected_discount_pct']))
