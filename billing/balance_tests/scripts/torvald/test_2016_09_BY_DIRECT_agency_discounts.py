# -*- coding: utf-8 -*-

__author__ = 'atkaya'

import datetime

import pytest
from hamcrest import equal_to

import btestlib.reporter as reporter
from balance import balance_api as api
from balance import balance_db as db
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils as utils

DIRECT_SERVICE_ID = 7
VI_MINSK_AGENCY_ID = 1137155
RUB_PAYSYS_ID = 1014
BYN_PAYSYS_ID = 1100

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))


def prepare_budget(acts, agency_id, person_id, contract_id, paysys_id):
    for service_id, product_id, completions, dt in acts:
        dt_1st = dt.replace(day=1)
        tmp_client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
        campaigns_list = [
            {'client_id': tmp_client_id, 'service_id': service_id, 'product_id': product_id, 'qty': completions,
             'begin_dt': dt_1st}
        ]
        invoice_id, _, _, orders_list = steps.InvoiceSteps.create_force_invoice(tmp_client_id,
                                                                                person_id,
                                                                                campaigns_list,
                                                                                paysys_id,
                                                                                dt_1st,
                                                                                agency_id=agency_id,
                                                                                credit=0,
                                                                                contract_id=contract_id,
                                                                                overdraft=0,
                                                                                manager_uid=None)
        steps.InvoiceSteps.pay(invoice_id)
        steps.CampaignsSteps.do_campaigns(service_id, orders_list[0]['ServiceOrderID'],
                                          {'Bucks': completions, 'Money': 0}, 0, dt_1st)
        steps.ActsSteps.generate(agency_id, 1, dt)


# Мультивалютность

@reporter.feature(Features.DISCOUNT)
# @pytest.mark.parametrize('target_currency', ['RUB', 'BYN'])
@pytest.mark.parametrize('target_currency', ['RUB'])
@pytest.mark.parametrize('scenario', [
    # 1: 1 месяц
    {'RUB_contract_start_dt': '2016-07-01T00:00:00',
     'RUB_acts': [(7, 1475, 2000, datetime.datetime(2016, 8, 30, 0, 0, 0)),
                  (7, 1475, 2000, datetime.datetime(2016, 8, 30, 0, 0, 0)),
                  (7, 1475, 2000, datetime.datetime(2016, 8, 30, 0, 0, 0)),
                  (7, 1475, 2000, datetime.datetime(2016, 8, 30, 0, 0, 0)),
                  (7, 1475, 2000, datetime.datetime(2016, 8, 30, 0, 0, 0))
                  ],
     'discount_policy_type': 16,
     'target_invoice': (7, 1475, datetime.datetime(2016, 9, 11, 0, 0, 0)),
     # 'target_currency': 'BYN',
     'expected_discount_pct': 12},

    # 1: 1 месяц
    {'RUB_contract_start_dt': '2016-07-01T00:00:00',
     'RUB_acts': [(7, 1475, 2000, datetime.datetime(2016, 7, 30, 0, 0, 0)),
                  (7, 1475, 2000, datetime.datetime(2016, 7, 30, 0, 0, 0)),
                  (7, 1475, 2000, datetime.datetime(2016, 7, 30, 0, 0, 0)),
                  (7, 1475, 2000, datetime.datetime(2016, 7, 30, 0, 0, 0)),
                  (7, 1475, 2000, datetime.datetime(2016, 7, 30, 0, 0, 0))
                  ],
     'BYN_contract_start_dt': '2016-08-01T00:00:00',
     'BYN_acts': [(7, 1475, 2000, datetime.datetime(2016, 8, 30, 0, 0, 0)),
                  (7, 1475, 2000, datetime.datetime(2016, 8, 30, 0, 0, 0)),
                  (7, 1475, 2000, datetime.datetime(2016, 8, 30, 0, 0, 0)),
                  (7, 1475, 2000, datetime.datetime(2016, 8, 30, 0, 0, 0)),
                  (7, 1475, 2000, datetime.datetime(2016, 8, 30, 0, 0, 0))
                  ],
     'discount_policy_type': 16,
     'target_invoice': (7, 1475, datetime.datetime(2016, 9, 11, 0, 0, 0)),
     # 'target_currency': 'RUB',
     'expected_discount_pct': 12},
])
def test_BEL_2016_agency_direct_discount_new_contract(scenario, target_currency):
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    invoice_owner = agency_id
    person_id = steps.PersonSteps.create(invoice_owner, 'yt', {})

    if 'RUB_contract_start_dt' in scenario:
        rub_contract_id, _ = steps.ContractSteps.create_contract('opt_agency',
                                                                 {'client_id': invoice_owner, 'CLIENT_ID': person_id,
                                                                  'DT': scenario['RUB_contract_start_dt'],
                                                                  'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                                                                  'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                                                                  'SERVICES': [DIRECT_SERVICE_ID],
                                                                  'DISCOUNT_POLICY_TYPE': scenario[
                                                                      'discount_policy_type'],
                                                                  })
    if 'RUB_acts' in scenario:
        prepare_budget(acts=scenario['RUB_acts'], agency_id=invoice_owner, person_id=person_id,
                       contract_id=rub_contract_id, paysys_id=RUB_PAYSYS_ID)

    if 'BYN_contract_start_dt' in scenario:
        byn_contract_id, _ = steps.ContractSteps.create_contract('opt_agency',
                                                                 {'CLIENT_ID': invoice_owner, 'PERSON_ID': person_id,
                                                                  'DT': scenario['BYN_contract_start_dt'],
                                                                  'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                                                                  'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                                                                  'SERVICES': [7],
                                                                  'CURRENCY': 933,
                                                                  'DISCOUNT_POLICY_TYPE': 16,
                                                                  'LINK_CONTRACT_ID': rub_contract_id if rub_contract_id else None,
                                                                  })
    if 'BYN_acts' in scenario:
        prepare_budget(acts=scenario['BYN_acts'], agency_id=invoice_owner, person_id=person_id,
                       contract_id=byn_contract_id, paysys_id=BYN_PAYSYS_ID)

    # ---------- EstimateDiscount -----------
    # result = steps.CommonSteps.log(api.medium().server.Balance.EstimateDiscount)(
    #            {'ClientID': agency_id, 'PaysysID': PAYSYS_ID, 'ContractID': contract_id},
    #            [{'ProductID': product_id, 'ClientID': client_id, 'Qty': 100, 'ID': 1, 'BeginDT': scenario['req_dt'],
    #             'RegionID': None,'discard_agency_discount': 0}
    #            ])
    # assert result['AgencyDiscountPct'] == str(scenario['expected_discount_pct'])

    contract_id = rub_contract_id if 'target_currency' == 'RUB' else byn_contract_id
    paysys_id = RUB_PAYSYS_ID if 'target_currency' == 'RUB' else BYN_PAYSYS_ID
    service_id, product_id, request_dt = scenario['target_invoice']

    # ---------- GetClientDiscountsAll -----------
    steps.CommonSteps.log(api.medium().server.Balance.GetClientDiscountsAll)({'ClientID': agency_id,
                                                                              'DT': request_dt})
    # TODO: check

    # ---------- Request ----------
    service_order_id = steps.OrderSteps.next_id(DIRECT_SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=service_id, product_id=product_id,
                                       params={'AgencyID': agency_id})
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': request_dt}
    ]
    # request_id = steps.RequestSteps.create(invoice_owner, orders_list, additional_params={'invoice_dt': scenario['req_dt']})
    request_id = steps.RequestSteps.create(invoice_owner, orders_list,
                                           additional_params={'InvoiceDesireDT': request_dt})
    invoice_id, external_id, total_sum = steps.InvoiceSteps.create(request_id, person_id, paysys_id,
                                                                   credit=scenario[
                                                                       'is_credit'] if 'is_credit' in scenario else 0,
                                                                   contract_id=contract_id, overdraft=0,
                                                                   endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    consume = db.get_consumes_by_invoice(invoice_id)[0]
    utils.check_that(consume['static_discount_pct'], equal_to(scenario['expected_discount_pct']))
