# -*- coding: utf-8 -*-

__author__ = 'torvald'

import datetime
import time
from decimal import Decimal as D
from decimal import ROUND_HALF_UP

import pytest

import btestlib.reporter as reporter
from balance import balance_api as api
from balance import balance_db as db
from balance import balance_steps as steps
from balance.balance_objects import Product
from balance.features import Features
from btestlib import matchers as mtch
from btestlib import utils as utils

pytestmark = [pytest.mark.priority('mid'),
              reporter.feature(Features.MULTICURRENCY, Features.COMMON),
              pytest.mark.tickets('BALANCE-23016, BALANCE-23528'),
              pytest.mark.docpath('https://wiki.yandex-team.ru/balance/docs/process/multicurrency'),
              pytest.mark.docs(u'--group', u'Автотесты по мультивалютности в BYN')
              ]

D_UP = lambda x: D(x).quantize(D('.01'), rounding=ROUND_HALF_UP)

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))

DIRECT_SERVICE_ID = 7
BYN_PRODUCT = Product(7, 507529, 'Money', None)
FISH_PRODUCT = Product(7, 1475, 'Bucks', 'Money')

RUB_PRICE_WITH_NDS = D('30')
RU_NDS = D('1.18')

QTY = D('118')
BASE_DT = datetime.datetime.now()

BEL_REGION_ID = 149

CC_NON_RES_7_RUB = 1075
BANK_NON_RES_1_BYN = 1100
BANK_NON_RES_1_RUB = 1014
BANK_UR = 1003

BY_DISCOUNT_POLICY = 16

RUB = 810
BYN = 933

BYN_CURRENCY_CODE = 'BYN'
# BYN_RATE = D(api.medium().server.GetCurrencyRate(BYN_CURRENCY_CODE, NOW)[2]['rate'])

CALC_RUB_SUM = lambda acted: D_UP(acted * RUB_PRICE_WITH_NDS / RU_NDS)
CALC_BYN_SUM = lambda consumed, acted, rate: D_UP(
    D_UP(consumed * RUB_PRICE_WITH_NDS / RU_NDS / rate) / consumed * acted)
CALC_BYN_TO_RUB_ACT_SUM = lambda consumed, acted, rate: D_UP(D_UP(consumed * rate) / consumed * acted)

manager_uid = '244916211'


def create_invoice_with_act(orders, client_id, agency_id, person_id, paysys_id, contract_id=None,
                            credit=0, overdraft=0):
    order_owner = client_id
    invoice_owner = agency_id or client_id
    orders_list = []

    # Create invoice with all requested orders
    for product, qty, completions in orders:
        service_order_id = steps.OrderSteps.next_id(product.service_id)
        order_id = steps.OrderSteps.create(order_owner, service_order_id, service_id=product.service_id,
                                           product_id=product.id, params={'AgencyID': agency_id})
        orders_list.append({'ServiceID': product.service_id, 'ServiceOrderID': service_order_id,
                            'OrderID': order_id, 'Qty': qty, 'BeginDT': BASE_DT})
    request_id = steps.RequestSteps.create(invoice_owner, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=credit,
                                                 contract_id=contract_id, overdraft=overdraft)

    # Pay for prepayment invoice
    if not (credit or overdraft):
        steps.InvoiceSteps.pay(invoice_id)
    # Invoice with SW CreditCard invoice won't by automatically turned on, even after full payment. Turn it on manually
    if paysys_id == CC_NON_RES_7_RUB:
        steps.InvoiceSteps.turn_on(invoice_id)

    # # 'order' structure doesn't contain 'ServiceOrderID', use it from orders_list
    # for num, (product, qty, completions) in enumerate(orders):
    #     steps.CampaignsSteps.do_campaigns(product.service_id, orders_list[num]['ServiceOrderID'],
    #                                       {product.shipment_type: completions}, do_stop=0, campaigns_dt=BASE_DT)

    # 'order' structure doesn't contain 'ServiceOrderID', use it from orders_list
    for num, (product, qty, completions) in enumerate(orders):
        steps.CampaignsSteps.update_campaigns(product.service_id, orders_list[num]['ServiceOrderID'],
                                              {product.shipment_type: completions}, do_stop=0, campaigns_dt=BASE_DT)
        steps.CommonSteps.export('PROCESS_COMPLETION', 'Order', orders_list[num]['OrderID'])

    steps.ActsSteps.generate(invoice_owner, force=1, date=BASE_DT)

    # Return orders to user 'OrderID' and 'ServiceOrderID' futher
    return orders_list


def migrate_to_currency(client_id, service_id, currency, region_id, dt=None, convert_type='COPY'):
    # 'MIGRATE_TO_CURRENCY' should be in future, so now()+2sec
    if not dt:
        dt = datetime.datetime.now() + dt_delta(seconds=2)
    steps.ClientSteps.create({'CLIENT_ID': client_id, 'REGION_ID': region_id, 'CURRENCY': currency,
                              'SERVICE_ID': service_id,
                              'MIGRATE_TO_CURRENCY': dt,
                              'CURRENCY_CONVERT_TYPE': convert_type})
    # Wait for the 3 seconds, 'MIGRATE_TO_CURRENCY' now in the past.
    time.sleep(3)
    steps.CommonSteps.export('MIGRATE_TO_CURRENCY', 'Client', client_id)


# ---------------------------------------------------------------------------------------------------------------------

@pytest.mark.parametrize('region_id', [None, BEL_REGION_ID])
def test_new_direct_client_by_CC_to_SW_in_RUB(region_id):
    QTY = D('118')
    ACT_QTY = D('99.9')

    client_id = steps.ClientSteps.create({'REGION_ID': region_id})
    person_id = steps.PersonSteps.create(client_id, 'by_ytph')

    orders = [(FISH_PRODUCT, QTY, ACT_QTY)]
    create_invoice_with_act(orders, client_id=client_id, agency_id=None, person_id=person_id,
                            paysys_id=CC_NON_RES_7_RUB, contract_id=None)

    actual = db.get_acts_by_client(client_id)
    expected = [{'amount': CALC_RUB_SUM(ACT_QTY)}]  # 30 \ 1.18 * 99.9 = 2539.83
    utils.check_that(actual, mtch.contains_dicts_with_entries(expected))


# ---------------------------------------------------------------------------------------------------------------------

@pytest.mark.parametrize('scenario', [(BANK_NON_RES_1_RUB, lambda rate: CALC_RUB_SUM(D('99.9'))),
                                      (BANK_NON_RES_1_BYN, lambda rate: CALC_BYN_SUM(D('118'), D('99.9'), rate))
                                      ],
                         ids=['FISH_yt_RUB', 'FISH_yt_BYN']
                         )
def test_new_direct_client_by_BANK_to_OOO(scenario):
    QTY = D('118')
    ACT_QTY = D('99.9')

    paysys_id, calculate_expected_amount = scenario

    client_id = steps.ClientSteps.create({'REGION_ID': BEL_REGION_ID})
    person_id = steps.PersonSteps.create(client_id, 'yt')

    orders = [(FISH_PRODUCT, QTY, ACT_QTY)]
    create_invoice_with_act(orders, client_id=client_id, agency_id=None, person_id=person_id,
                            paysys_id=paysys_id, contract_id=None)

    actual = db.get_acts_by_client(client_id)

    byn_currency_rate = D(api.medium().server.GetCurrencyRate(BYN_CURRENCY_CODE, NOW)[2]['rate'])
    expected = [{'amount': calculate_expected_amount(byn_currency_rate)}]
    utils.check_that(actual, mtch.contains_dicts_with_entries(expected))


# ---------------------------------------------------------------------------------------------------------------------

@pytest.mark.parametrize('contract_info', [
    (BYN, 'no_agency', 0, BANK_NON_RES_1_BYN, lambda rate: CALC_BYN_SUM(D('118'), D('99.9'), rate)),
    (BYN, 'no_agency_post', 1, BANK_NON_RES_1_BYN, lambda rate: CALC_BYN_SUM(D('118'), D('99.9'), rate)),
    (RUB, 'no_agency', 0, BANK_NON_RES_1_RUB, lambda rate: CALC_RUB_SUM(D('99.9'))),
    (RUB, 'no_agency_post', 1, BANK_NON_RES_1_RUB, lambda rate: CALC_RUB_SUM(D('99.9')))
],
                         ids=['FISH_no_agency_BYN', 'FISH_no_agency_credit_BYN', 'FISH_no_agency_RUB',
                              'FISH_no_agency_credit_RUB']
                         )
def test_new_direct_client_by_contract(contract_info):
    QTY = D('118')
    ACT_QTY = D('99.9')

    client_id = steps.ClientSteps.create({'REGION_ID': BEL_REGION_ID})
    person_id = steps.PersonSteps.create(client_id, 'yt')

    contract_currency, contract_type, credit, paysys_id, calculate_expected_amount = contract_info
    contract_id, _ = steps.ContractSteps.create_contract(contract_type, {'CLIENT_ID': client_id, 'PERSON_ID': person_id,
                                                                         'DT': HALF_YEAR_BEFORE_NOW_ISO,
                                                                         'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                                                                         'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                                                                         'SERVICES': [7],
                                                                         'DEAL_PASSPORT': HALF_YEAR_BEFORE_NOW_ISO,
                                                                         'CURRENCY': contract_currency,
                                                                         'CREDIT_LIMIT_SINGLE': 500000,
                                                                         # 'CREDIT_LIMIT_IN_CONTRACT_CURRENCY': 1
                                                                         })

    orders = [(FISH_PRODUCT, QTY, ACT_QTY)]
    create_invoice_with_act(orders, client_id=client_id, agency_id=None, person_id=person_id,
                            paysys_id=paysys_id, contract_id=contract_id, credit=credit)

    actual = db.get_acts_by_client(client_id)

    byn_currency_rate = D(api.medium().server.GetCurrencyRate(BYN_CURRENCY_CODE, NOW)[2]['rate'])
    expected = [{'amount': calculate_expected_amount(byn_currency_rate)}]
    utils.check_that(actual, mtch.contains_dicts_with_entries(expected))


# ---------------------------------------------------------------------------------------------------------------------

@pytest.mark.parametrize('paychoice', [(BYN_PRODUCT, BANK_NON_RES_1_BYN, 'yt', lambda rate: D('99.9')),
                                       # (BYN_PRODUCT, CC_NON_RES_7_RUB,   'by_ytph', lambda rate: D_UP(D('99.9') * rate))
                                       (BYN_PRODUCT, CC_NON_RES_7_RUB, 'by_ytph',
                                        lambda rate: CALC_BYN_TO_RUB_ACT_SUM(D('118'), D('99.9'), rate))
                                       ],
                         ids=['BYN_product_yt', 'BYN_product_byytph']
                         )
def test_new_multicurrency_client(paychoice):
    QTY = D('118')
    ACT_QTY = D('99.9')

    client_id = steps.ClientSteps.create()
    migrate_to_currency(client_id, DIRECT_SERVICE_ID, BYN_CURRENCY_CODE, BEL_REGION_ID)

    product, paysys_id, person_category, calculate_expected_amount = paychoice

    person_id = steps.PersonSteps.create(client_id, person_category)
    orders = [(product, QTY, ACT_QTY)]
    create_invoice_with_act(orders, client_id=client_id, agency_id=None, person_id=person_id,
                            paysys_id=paysys_id, contract_id=None)

    actual = db.get_acts_by_client(client_id)
    byn_currency_rate = D(api.medium().server.GetCurrencyRate(BYN_CURRENCY_CODE, NOW)[2]['rate'])
    expected = [{'amount': calculate_expected_amount(byn_currency_rate)}]
    utils.check_that(actual, mtch.contains_dicts_with_entries(expected))


# ---------------------------------------------------------------------------------------------------------------------

@pytest.mark.parametrize('contract_info', [('no_agency', 0),
                                           ('no_agency_post', 1)],
                         ids=['BYN_product', 'BYN_product_credit']
                         )
def test_new_multicurrency_client_by_BYN_contract(contract_info):
    QTY = D('118')
    ACT_QTY = D('99.9')

    client_id = steps.ClientSteps.create()
    migrate_to_currency(client_id, DIRECT_SERVICE_ID, BYN_CURRENCY_CODE, BEL_REGION_ID)
    person_id = steps.PersonSteps.create(client_id, 'yt')

    contract_type, credit = contract_info
    contract_id, _ = steps.ContractSteps.create_contract(contract_type, {'CLIENT_ID': client_id, 'PERSON_ID': person_id,
                                                                         'DT': HALF_YEAR_BEFORE_NOW_ISO,
                                                                         'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                                                                         'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                                                                         'SERVICES': [7],
                                                                         'DEAL_PASSPORT': HALF_YEAR_BEFORE_NOW_ISO,
                                                                         'CURRENCY': BYN,
                                                                         'CREDIT_LIMIT_SINGLE': 500000,
                                                                         # 'CREDIT_LIMIT_IN_CONTRACT_CURRENCY': 1
                                                                         })

    orders = [(BYN_PRODUCT, QTY, ACT_QTY)]
    create_invoice_with_act(orders, client_id=client_id, agency_id=None, person_id=person_id,
                            paysys_id=BANK_NON_RES_1_BYN, contract_id=contract_id, credit=credit)

    actual = db.get_acts_by_client(client_id)
    expected = [{'amount': ACT_QTY}]
    utils.check_that(actual, mtch.contains_dicts_with_entries(expected))


# ---------------------------------------------------------------------------------------------------------------------

@pytest.mark.smoke
def test_existed_client_migration_byn_product_availability():
    QTY = D('118')
    ACT_QTY = D('99.9')

    client_id = steps.ClientSteps.create({'REGION_ID': BEL_REGION_ID})
    person_id = steps.PersonSteps.create(client_id, 'by_ytph')

    orders = [(FISH_PRODUCT, QTY, ACT_QTY)]
    create_invoice_with_act(orders, client_id=client_id, agency_id=None, person_id=person_id,
                            paysys_id=CC_NON_RES_7_RUB, contract_id=None)

    # Migration
    migrate_to_currency(client_id, DIRECT_SERVICE_ID, BYN_CURRENCY_CODE, BEL_REGION_ID)

    # Invoice with BYN-orders
    yt_person_id = steps.PersonSteps.create(client_id, 'yt')
    orders = [(BYN_PRODUCT, QTY, ACT_QTY)]
    create_invoice_with_act(orders, client_id=client_id, agency_id=None, person_id=yt_person_id,
                            paysys_id=BANK_NON_RES_1_BYN, contract_id=None)

    actual = db.get_acts_by_client(client_id)
    expected = [{'amount': CALC_RUB_SUM(ACT_QTY)},
                {'amount': ACT_QTY}]
    utils.check_that(actual, mtch.contains_dicts_with_entries(expected))


# ---------------------------------------------------------------------------------------------------------------------

def test_existed_RUB_client_migration_byn_product_availability():
    QTY = D('118')
    ACT_QTY = D('99.9')

    client_id = steps.ClientSteps.create()

    # Invoice with fish-orders and acts
    person_id = steps.PersonSteps.create(client_id, 'ur')
    orders = [(FISH_PRODUCT, QTY, ACT_QTY)]
    create_invoice_with_act(orders, client_id=client_id, agency_id=None, person_id=person_id,
                            paysys_id=BANK_UR, contract_id=None)

    # Hide UR person before migration
    steps.PersonSteps.hide_person(person_id)
    # Migration
    migrate_to_currency(client_id, DIRECT_SERVICE_ID, BYN_CURRENCY_CODE, BEL_REGION_ID)

    # Invoice with BYN-orders
    yt_person_id = steps.PersonSteps.create(client_id, 'yt')
    orders = [(BYN_PRODUCT, QTY, ACT_QTY)]
    create_invoice_with_act(orders, client_id=client_id, agency_id=None, person_id=yt_person_id,
                            paysys_id=BANK_NON_RES_1_BYN, contract_id=None)

    actual = db.get_acts_by_client(client_id)
    expected = [{'amount': ACT_QTY * RUB_PRICE_WITH_NDS},
                {'amount': ACT_QTY}]
    utils.check_that(actual, mtch.contains_dicts_with_entries(expected))


# ---------------------------------------------------------------------------------------------------------------------

# Cases of agency invoice with contract creation. Agency and subclients can be multicurrency (BYN)
# Main case: old RUB contract for non-currency subclients, new BYN contract for new sub-clients (which use BYN-paysys,
# or just multicurrency)
# Parametrization:
# 'agency_multicurrency' - multicurrency params for agency
# 'contracts' - list of contract params with additional params for subclient and invoice for this contract
#            'subclient_multicurrency' - multicurrency params for agency
#            'contract_info': type, currency and credit sign for contract
#            'invoice': list of orders for invoice. TODO: doesn't support expected result for several invoice_orders
#                      'order': Product instance, qty for invoice, completion qty for act creation
#                      'expected_amount': expected act amount
#            'paysys': paysys for invoice
#
@pytest.mark.parametrize('scenario', [
    # Два независимых договора (RUB \ BYN)
    {'description': 'RUB_BYN_fish',
     'agency_multicurrency': None,
     'contracts':
         [{'subclient_multicurrency': None,
           'contract_info': {'type': 'opt_agency', 'currency': RUB, 'credit': 0},
           'invoice': [{'order': (FISH_PRODUCT, D('118'), D('99.9')),
                        'expected_amount': lambda rate: CALC_RUB_SUM(D('99.9'))}
                       ],
           'paysys': BANK_NON_RES_1_RUB
           },
          {'subclient_multicurrency': None,
           'contract_info': {'type': 'opt_agency', 'currency': BYN, 'credit': 0},
           'invoice': [{'order': (FISH_PRODUCT, D('118'), D('99.9')),
                        'expected_amount': lambda rate: CALC_BYN_SUM(D('118'), D('99.9'), rate)}
                       ],
           'paysys': BANK_NON_RES_1_BYN
           }
          ]
     },
    #
    {'description': 'RUB_BYN_with_multi_subclient',
     'agency_multicurrency': None,
     'contracts':
         [{'subclient_multicurrency': None,
           'contract_info': {'type': 'opt_agency', 'currency': RUB, 'credit': 0},
           'invoice': [{'order': (FISH_PRODUCT, D('118'), D('99.9')),
                        'expected_amount': lambda rate: CALC_RUB_SUM(D('99.9'))}
                       ],
           'paysys': BANK_NON_RES_1_RUB
           },
          {'subclient_multicurrency': (DIRECT_SERVICE_ID, 'BYN', 149),
           'contract_info': {'type': 'opt_agency', 'currency': BYN, 'credit': 0},
           'invoice': [{'order': (BYN_PRODUCT, D('118'), D('99.9')),
                        'expected_amount': lambda rate: D('99.9')}
                       ],
           'paysys': BANK_NON_RES_1_BYN
           }
          ]
     },
    #
    {'description': 'RUB_BYN_with_multi_agency_and_subclient',
     'agency_multicurrency': (DIRECT_SERVICE_ID, 'BYN', 149),
     'contracts':
         [{'subclient_multicurrency': None,
           'contract_info': {'type': 'opt_agency', 'currency': RUB, 'credit': 0},
           'invoice': [{'order': (FISH_PRODUCT, D('118'), D('99.9')),
                        'expected_amount': lambda rate: CALC_RUB_SUM(D('99.9'))}
                       ],
           'paysys': BANK_NON_RES_1_RUB
           },
          {'subclient_multicurrency': (DIRECT_SERVICE_ID, 'BYN', 149),
           'contract_info': {'type': 'opt_agency', 'currency': BYN, 'credit': 0},
           'invoice': [{'order': (BYN_PRODUCT, D('118'), D('99.9')),
                        'expected_amount': lambda rate: D('99.9')}
                       ],
           'paysys': BANK_NON_RES_1_BYN
           }
          ]
     },
    #
    {'description': 'RUB_BYN_with_multi_agency_and_subclient_credit',
     'agency_multicurrency': (DIRECT_SERVICE_ID, 'BYN', 149),
     'contracts':
         [{'subclient_multicurrency': None,
           'contract_info': {'type': 'opt_agency_post', 'currency': RUB, 'credit': 1},
           'invoice': [{'order': (FISH_PRODUCT, D('118'), D('99.9')),
                        'expected_amount': lambda rate: D('2539.83')}
                       ],
           'paysys': BANK_NON_RES_1_RUB
           },
          {'subclient_multicurrency': (DIRECT_SERVICE_ID, 'BYN', 149),
           'contract_info': {'type': 'opt_agency_post', 'currency': BYN, 'credit': 1},
           'invoice': [{'order': (BYN_PRODUCT, D('118'), D('99.9')),
                        'expected_amount': lambda rate: D('99.9')}
                       ],
           'paysys': BANK_NON_RES_1_BYN
           }
          ]
     },

    # # Нет договора в подходящей валюте!!
    # {'agency_multicurrency': None,
    #  'contracts':
    #      [{'subclient_multicurrency': None,
    #        'contract_info': {'type': 'agency_belarus', 'currency': RUB, 'credit': 0},
    #        'invoice': [{'order': (FISH_PRODUCT, D('118'), D('99.9')),
    #                      'expected_amount': lambda: D('2539.83')}
    #                     ],
    #        'paysys': BANK_NON_RES_1_RUB
    #        },
    #      ]
    # },
    #
    # # Фиктивный счёт в валюте лимита кредита
    # {'agency_multicurrency': None,
    #  'contracts':
    #     [
    #       {'subclient_multicurrency': None,
    #        'contract_info': {'type': 'opt_agency_post', 'currency': RUB, 'credit': 1},
    #        'invoice': [{'order': (FISH_PRODUCT, D('118'), D('99.9')),
    #                      'expected_amount': lambda: D('99.9')}
    #                     ],
    #        'paysys': BANK_NON_RES_1_BYN
    #        }
    #      ]
    # },
],
                         ids=lambda x: x['description']
                         )
def test_agency_invoice(scenario):
    agency_id = None or steps.ClientSteps.create({'IS_AGENCY': 1})
    db.balance().execute("Update t_client set REGION_ID = :region_id where ID = :client_id", {'client_id': agency_id,
                                                                                              'region_id': 149})
    # Condition for ability to use 'None' value in parametrization
    if scenario['agency_multicurrency']:
        migrate_to_currency(agency_id, *scenario['agency_multicurrency'])
    person_id = None or steps.PersonSteps.create(agency_id, 'yt')

    # Create invoices for different subclients (with different contracts)
    for contract in scenario['contracts']:
        client_id = None or steps.ClientSteps.create()
        # Condition for ability to use 'None' value in parametrization
        if contract['subclient_multicurrency']:
            migrate_to_currency(client_id, *contract['subclient_multicurrency'])

        # Prepare contract params: add credit params if 'credit'==1
        contract_params = {'CLIENT_ID': agency_id,
                           'PERSON_ID': person_id,
                           'DT': HALF_YEAR_BEFORE_NOW_ISO,
                           'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                           'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                           'SERVICES': [7],
                           # 'NON_RESIDENT_CLIENTS': 0,
                           'BANK_DETAILS_ID': 3,
                           'DEAL_PASSPORT': HALF_YEAR_BEFORE_NOW_ISO,
                           'CURRENCY': contract['contract_info']['currency'],
                           'DISCOUNT_POLICY_TYPE': BY_DISCOUNT_POLICY,
                           }
        if contract['contract_info']['credit']:
            # contract_credit_params = {'PERSONAL_ACCOUNT': 1,
            #                           'LIFT_CREDIT_ON_PAYMENT': 1,
            #                           'PERSONAL_ACCOUNT_FICTIVE': 1,
            #                           'CREDIT_LIMIT_SINGLE': 500000000,
            #                           'CREDIT_LIMIT_IN_CONTRACT_CURRENCY': 1}
            contract_credit_params = {'PERSONAL_ACCOUNT': 0,
                                      'LIFT_CREDIT_ON_PAYMENT': 0,
                                      'PERSONAL_ACCOUNT_FICTIVE': 0,
                                      'CREDIT_LIMIT_SINGLE': 500000000,
                                      'CREDIT_LIMIT_IN_CONTRACT_CURRENCY': 1}
            contract_params.update(contract_credit_params)
        contract_id, _ = steps.ContractSteps.create_contract(contract['contract_info']['type'], contract_params)

        # Remove 'expected_amount' from 'invoices' struct to have correct structure,
        # which can be sent to 'create_invoice_with_act' method
        orders = [item['order'] for item in contract['invoice']]
        create_invoice_with_act(orders, client_id=client_id, agency_id=agency_id, person_id=person_id,
                                paysys_id=contract['paysys'], contract_id=contract_id,
                                credit=contract['contract_info']['credit'])

        actual = db.get_acts_by_client(agency_id)
        # Create expected list from 'expected_amount' values of 'invoices' structure
        byn_currency_rate = D(api.medium().server.GetCurrencyRate(BYN_CURRENCY_CODE, NOW)[2]['rate'])
        expected = [{'amount': item['expected_amount'](byn_currency_rate)} for item in contract['invoice']]
        utils.check_that(actual, mtch.contains_dicts_with_entries(expected, same_length=False))
