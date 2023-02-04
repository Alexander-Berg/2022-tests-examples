# -*- coding: utf-8 -*-

import datetime

import pytest

import balance.balance_db as db
import btestlib.matchers as matchers
import btestlib.reporter as reporter
import btestlib.utils as utils
from balance import balance_steps as steps
from balance.features import Features

DT = datetime.datetime.now()
PERSON_TYPE = 'ph'
PAYSYS_ID = 1001
SERVICE_ID = 7
PRODUCT_ID = 1475
OVERDRAFT_LIMIT = 1000
FIRM_ID = 1


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('data',
                         [
                             {'is_invoice_paid': False,
                              'is_invoice_expired': True,
                              'is_manual_suspect': True,
                              'with_our_fault_bad_debt': False,
                              'another_act': False
                              },
                             {'is_invoice_paid': 300,
                              'is_invoice_expired': True,
                              'is_manual_suspect': False,
                              'with_our_fault_bad_debt': False,
                              'another_act': False
                              },
                             {'is_invoice_paid': False,
                              'is_invoice_expired': False,
                              'is_manual_suspect': False,
                              'with_our_fault_bad_debt': False,
                              'another_act': False
                              },
                             {'is_invoice_paid': False,
                              'is_invoice_expired': True,
                              'with_our_fault_bad_debt': {'our_fault': 1, 'hidden': 0},
                              'another_act': False,
                              # expected:
                              'is_manual_suspect': False,

                              },
                             {'is_invoice_paid': False,
                              'is_invoice_expired': True,
                              'is_manual_suspect': True,
                              'with_our_fault_bad_debt': {'our_fault': 0, 'hidden': 0},
                              'another_act': False
                              },
                             {'is_invoice_paid': False,
                              'is_invoice_expired': True,
                              'is_manual_suspect': True,
                              'with_our_fault_bad_debt': {'our_fault': 1, 'hidden': 1},
                              'another_act': False
                              },
                             {'is_invoice_paid': False,
                              'is_invoice_expired': True,
                              'is_manual_suspect': True,
                              'with_our_fault_bad_debt': {'our_fault': 1, 'hidden': 0},
                              'another_act': True
                              },
                         ]
                         )
def test_overdraft_invoice_manual_suspect_raising(data):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    steps.OverdraftSteps.set_force_overdraft(client_id, SERVICE_ID, OVERDRAFT_LIMIT, FIRM_ID)

    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    steps.OrderSteps.create(client_id, service_order_id, PRODUCT_ID, SERVICE_ID)

    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 10, 'BeginDT': DT}
    ]

    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=DT))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=1, endbuyer_id=None)

    if data['is_invoice_paid']:
        steps.InvoiceSteps.pay(invoice_id, payment_sum=data['is_invoice_paid'])

    if data['is_invoice_expired']:
        steps.OverdraftSteps.expire_overdraft_invoice(invoice_id, 1)

    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id,
                                      campaigns_params={'Bucks': 5}, do_stop=0, campaigns_dt=DT)

    act_id = steps.ActsSteps.generate(client_id, force=1, date=DT)[0]

    bd = data['with_our_fault_bad_debt']

    if bd:
        steps.BadDebtSteps.make_bad_debt(invoice_id, bd['our_fault'])
        if bd['hidden']:
            steps.BadDebtSteps.make_bad_debt_hidden(act_id)

    if data['another_act']:
        steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id,
                                          campaigns_params={'Bucks': 7}, do_stop=0, campaigns_dt=DT)

        steps.ActsSteps.generate(client_id, force=1, date=DT)

    steps.OverdraftSteps.run_manual_suspect(client_id)

    client = db.get_client_by_id(client_id)[0]
    reporter.log(client)
    if data['is_manual_suspect']:
        utils.check_that(client, matchers.has_entries({'manual_suspect': 1}))
    else:
        utils.check_that(client, matchers.has_entries({'manual_suspect': 0}))


def test_overdraft_invoice_manual_suspect_removal():
    client_id = steps.ClientSteps.create()
    steps.OverdraftSteps.make_client_manual_suspect(client_id)
    steps.OverdraftSteps.run_manual_suspect(client_id)
    client = db.get_client_by_id(client_id)[0]
    utils.check_that(client, matchers.has_entries({'manual_suspect': 0}))
