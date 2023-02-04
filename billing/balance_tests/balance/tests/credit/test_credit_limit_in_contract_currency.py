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
from btestlib.constants import Currencies, Services, Regions

pytestmark = [pytest.mark.priority('mid'),
              reporter.feature(Features.MULTICURRENCY, Features.COMMON),
              pytest.mark.tickets('BALANCE-23016, BALANCE-23528'),
              pytest.mark.docpath('https://wiki.yandex-team.ru/balance/docs/process/multicurrency'),
              pytest.mark.docs(u'--group', u'Автотесты по мультивалютности в BYN')
              ]

NON_RESIDENT_QUERY = "Update t_client set FULLNAME = u'UL Nonres', CURRENCY_PAYMENT = 'USD', ISO_CURRENCY_PAYMENT = 'USD', IS_NON_RESIDENT = 1 where ID = :client_id"

D_UP = lambda x: D(x).quantize(D('.01'), rounding=ROUND_HALF_UP)

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))

DIRECT_SERVICE_ID = 7
BYN_PRODUCT = Product(Services.DIRECT.id, 507529, 'Money', None)
FISH_PRODUCT = Product(Services.DIRECT.id, 1475, 'Bucks', 'Money')

RUB_PRICE_WITH_NDS = D('30')
RU_NDS = D('1.18')

QTY = D('118')
BASE_DT = datetime.datetime.now()

BEL_REGION_ID = 149

KZU_PAYSYS_ID = 1020
KZP_PAYSYS_ID = 1021
SUB_PAYSYS_ID = 1060

CC_NON_RES_7_RUB = 1075
BANK_NON_RES_1_BYN = 1100
BANK_NON_RES_1_RUB = 1014
BANK_UR = 1003
BANK_NON_RES_WITH_AGENCY_USD = 1026

BANK_USD_SW_UR_PAYSYS_ID = 1044
BANK_USD_SW_YT_PAYSYS_ID = 1047

BY_DISCOUNT_POLICY = 16
KZ_DISCOUNT_POLICY = 18

# DIRECT_PRODUCT = Product(7, 1475, 'Bucks', 'Money')
# DIRECT_KZT_PRODUCT = Product(7, 503166, 'Money')
# MARKET_PRODUCT = Product(11, 2136, 'Bucks')
# MEDIA_PRODUCT_31 = Product(70, 100000000, 'Money')  # 503341 валютный, Price = 1
# MEDIA_PRODUCT_1 = Product(70, 503123, 'Shows')
# GEO_PRODUCT = Product(37, 502952, 'Days')

RUB = Currencies.RUB.num_code
BYN = Currencies.BYN.num_code
KZT = Currencies.KZT.num_code
USD = Currencies.USD.num_code

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

    # 'order' structure doesn't contain 'ServiceOrderID', use it from orders_list
    for num, (product, qty, completions) in enumerate(orders):
        steps.CampaignsSteps.do_campaigns(product.service_id, orders_list[num]['ServiceOrderID'],
                                          {product.shipment_type: completions}, do_stop=0, campaigns_dt=BASE_DT)

    steps.ActsSteps.generate(invoice_owner, force=1, date=BASE_DT)

    # Return orders to user 'OrderID' and 'ServiceOrderID' futher
    return orders_list, invoice_id


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


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('scenario', [
    #
    pytest.mark.smoke({'description': 'BYN_contract_limit_in_contract_currency',
                       'agency_actions': [],
                       'contracts':
                           [{'person_type': 'yt',
                             'subclient_actions': [
                                 lambda id: migrate_to_currency(id, DIRECT_SERVICE_ID, 'BYN', Regions.BY.id)],
                             'contract_info': {'type': 'opt_agency_post',
                                               'contract_params': {'SERVICES': [Services.DIRECT.id],
                                                                   # 'NON_RESIDENT_CLIENTS': 0,
                                                                   'BANK_DETAILS_ID': 3,
                                                                   'DEAL_PASSPORT': HALF_YEAR_BEFORE_NOW_ISO,
                                                                   'CURRENCY': Currencies.BYN.num_code,
                                                                   'DISCOUNT_POLICY_TYPE': BY_DISCOUNT_POLICY,
                                                                   'PERSONAL_ACCOUNT': 0,
                                                                   'LIFT_CREDIT_ON_PAYMENT': 0,
                                                                   'PERSONAL_ACCOUNT_FICTIVE': 0,
                                                                   'CREDIT_LIMIT_SINGLE': 6000,
                                                                   'CREDIT_LIMIT_IN_CONTRACT_CURRENCY': 1
                                                                   },
                                               'credit': 1},
                             'invoice': [(BYN_PRODUCT, D('118'), D('99.9'))],
                             'paysys_id': BANK_NON_RES_1_BYN
                             }
                            ],
                       'currency_for_rate': Currencies.BYN.iso_code,
                       'expected_spent_limit': lambda rate: [{'ISO_CURRENCY': Currencies.BYN.iso_code,
                                                              'LIMIT_SPENT': D_UP(D('118')),
                                                              }]
                       }),
    # тот же тест, но с переходом на старую кредитную схему апдейтом в базе
    {'description': 'BYN_contract_limit_in_contract_currency_force_fictive_credit_scheme',
     'force_fictive_credit_scheme': True,
     'agency_actions': [],
     'contracts':
         [{'person_type': 'yt',
           'subclient_actions': [lambda id: migrate_to_currency(id, DIRECT_SERVICE_ID, 'BYN', Regions.BY.id)],
           'contract_info': {'type': 'opt_agency_post',
                             'contract_params': {'SERVICES': [Services.DIRECT.id],
                                                 # 'NON_RESIDENT_CLIENTS': 0,
                                                 'BANK_DETAILS_ID': 3,
                                                 'DEAL_PASSPORT': HALF_YEAR_BEFORE_NOW_ISO,
                                                 'CURRENCY': Currencies.BYN.num_code,
                                                 'DISCOUNT_POLICY_TYPE': BY_DISCOUNT_POLICY,
                                                 'PERSONAL_ACCOUNT': 0,
                                                 'LIFT_CREDIT_ON_PAYMENT': 0,
                                                 'PERSONAL_ACCOUNT_FICTIVE': 0,
                                                 'CREDIT_LIMIT_SINGLE': 6000,
                                                 'CREDIT_LIMIT_IN_CONTRACT_CURRENCY': 1
                                                 },
                             'credit': 1},
           'invoice': [(BYN_PRODUCT, D('118'), D('99.9'))],
           'paysys_id': BANK_NON_RES_1_BYN
           }
          ],
     'currency_for_rate': Currencies.BYN.iso_code,
     'expected_spent_limit': lambda rate: [{'ISO_CURRENCY': Currencies.BYN.iso_code,
                                            'LIMIT_SPENT': D_UP(D('118')),
                                            }]
     },
    #
    pytest.mark.smoke({'description': 'BYN_contract_limit_in_RUB',
                       'agency_actions': [],
                       'contracts':
                           [{'person_type': 'yt',
                             'subclient_actions': [
                                 lambda id: migrate_to_currency(id, DIRECT_SERVICE_ID, 'BYN', Regions.BY.id)],
                             'contract_info': {'type': 'opt_agency_post',
                                               'contract_params': {'SERVICES': [Services.DIRECT.id],
                                                                   # 'NON_RESIDENT_CLIENTS': 0,
                                                                   'BANK_DETAILS_ID': 3,
                                                                   'DEAL_PASSPORT': HALF_YEAR_BEFORE_NOW_ISO,
                                                                   'CURRENCY': Currencies.BYN.num_code,
                                                                   'DISCOUNT_POLICY_TYPE': BY_DISCOUNT_POLICY,
                                                                   'PERSONAL_ACCOUNT': 0,
                                                                   'LIFT_CREDIT_ON_PAYMENT': 0,
                                                                   'PERSONAL_ACCOUNT_FICTIVE': 0,
                                                                   'CREDIT_LIMIT_SINGLE': 6000,
                                                                   'CREDIT_LIMIT_IN_CONTRACT_CURRENCY': 0
                                                                   },
                                               'credit': 1},
                             'invoice': [(BYN_PRODUCT, D('118'), D('99.9'))],
                             'paysys_id': BANK_NON_RES_1_BYN
                             }
                            ],
                       'currency_for_rate': Currencies.BYN.iso_code,
                       'expected_spent_limit': lambda rate: [{'ISO_CURRENCY': Currencies.RUB.iso_code,
                                                              'LIMIT_SPENT': D_UP(D('118') * rate)
                                                              }]
                       }),
    # тот же тест, но с переходом на старую кредитную схему апдейтом в базе
    {'description': 'BYN_contract_limit_in_RUB_force_fictive_credit_scheme',
     'force_fictive_credit_scheme': True,
     'agency_actions': [],
     'contracts':
         [{'person_type': 'yt',
           'subclient_actions': [
               lambda id: migrate_to_currency(id, DIRECT_SERVICE_ID, 'BYN', Regions.BY.id)],
           'contract_info': {'type': 'opt_agency_post',
                             'contract_params': {'SERVICES': [Services.DIRECT.id],
                                                 # 'NON_RESIDENT_CLIENTS': 0,
                                                 'BANK_DETAILS_ID': 3,
                                                 'DEAL_PASSPORT': HALF_YEAR_BEFORE_NOW_ISO,
                                                 'CURRENCY': Currencies.BYN.num_code,
                                                 'DISCOUNT_POLICY_TYPE': BY_DISCOUNT_POLICY,
                                                 'PERSONAL_ACCOUNT': 0,
                                                 'LIFT_CREDIT_ON_PAYMENT': 0,
                                                 'PERSONAL_ACCOUNT_FICTIVE': 0,
                                                 'CREDIT_LIMIT_SINGLE': 6000,
                                                 'CREDIT_LIMIT_IN_CONTRACT_CURRENCY': 0
                                                 },
                             'credit': 1},
           'invoice': [(BYN_PRODUCT, D('118'), D('99.9'))],
           'paysys_id': BANK_NON_RES_1_BYN
           }
          ],
     'currency_for_rate': Currencies.BYN.iso_code,
     'expected_spent_limit': lambda rate: [{'ISO_CURRENCY': Currencies.RUB.iso_code,
                                            'LIMIT_SPENT': D_UP(D('118') * rate)
                                            }]
     },

    #
    {'description': 'KZT_contract_limit_in_contract_currency',
     'agency_actions': [],
     'contracts':
         [{'person_type': 'yt_kzu',
           'subclient_actions': [lambda id: migrate_to_currency(id, DIRECT_SERVICE_ID, 'KZT', Regions.KZ.id)],
           'contract_info': {'type': 'opt_agency_post_kz',
                             'contract_params': {'SERVICES': [Services.DIRECT.id],
                                                 'DISCOUNT_POLICY_TYPE': KZ_DISCOUNT_POLICY,
                                                 'PERSONAL_ACCOUNT': 1,
                                                 'LIFT_CREDIT_ON_PAYMENT': 1,
                                                 'PERSONAL_ACCOUNT_FICTIVE': 1,
                                                 'CURRENCY': Currencies.KZT.num_code,
                                                 'PAYMENT_TERM': 13,
                                                 'BANK_DETAILS_ID': 320,
                                                 'DEAL_PASSPORT': HALF_YEAR_BEFORE_NOW_ISO,
                                                 'CREDIT_LIMIT_SINGLE': 6000,
                                                 'CREDIT_LIMIT_IN_CONTRACT_CURRENCY': 1
                                                 },
                             'credit': 1},
           'invoice': [(FISH_PRODUCT, D('18'), D('9.9'))],
           'paysys_id': SUB_PAYSYS_ID
           }
          ],
     'currency_for_rate': Currencies.KZT.iso_code,
     'expected_spent_limit': lambda rate: [{'ISO_CURRENCY': Currencies.KZT.iso_code,
                                            'LIMIT_SPENT': D_UP(D('1890')),
                                            # Договор "по сроку" - делаем пересчёт
                                            'LIMIT_TOTAL': D_UP(D('6000') * (D('13') + D('30')) / D('30')),
                                            }]
     },
    #
    {'description': 'KZT_contract_limit_in_RUB',
     'agency_actions': [],
     'contracts':
         [{'person_type': 'yt_kzu',
           'subclient_actions': [lambda id: migrate_to_currency(id, DIRECT_SERVICE_ID, 'KZT', Regions.KZ.id)],
           'contract_info': {'type': 'opt_agency_post_kz',
                             'contract_params': {'SERVICES': [Services.DIRECT.id],
                                                 'DISCOUNT_POLICY_TYPE': KZ_DISCOUNT_POLICY,
                                                 'CURRENCY': Currencies.KZT.num_code,
                                                 'PAYMENT_TERM': 13,
                                                 'BANK_DETAILS_ID': 320,
                                                 'DEAL_PASSPORT': HALF_YEAR_BEFORE_NOW_ISO,
                                                 'PERSONAL_ACCOUNT': 1,
                                                 'LIFT_CREDIT_ON_PAYMENT': 1,
                                                 'PERSONAL_ACCOUNT_FICTIVE': 1,
                                                 'CREDIT_LIMIT_SINGLE': 6000,
                                                 'CREDIT_LIMIT_IN_CONTRACT_CURRENCY': 0
                                                 },
                             'credit': 1},
           'invoice': [(FISH_PRODUCT, D('18'), D('9.9'))],
           'paysys_id': SUB_PAYSYS_ID
           }
          ],
     'currency_for_rate': Currencies.KZT.iso_code,
     'expected_spent_limit': lambda rate: [{'ISO_CURRENCY': Currencies.RUB.iso_code,
                                            # Пересчёт идёт через фиксированный курс:
                                            # select * from t_currency_rate_v2 where rate_src_id = 1111 and cc = 'KZT';
                                            # 0,195567144719687092568448500651890482399
                                            # 1890 / 360.76698 = 0.190882
                                            'LIMIT_SPENT': D_UP(D('1890') * rate),
                                            # Договор "по сроку" - делаем пересчёт
                                            'LIMIT_TOTAL': D_UP(D('6000') * (D('13') + D('30')) / D('30')),
                                            }]
     },

    # {'description': 'RUB_contract_with_non_residents_in_USD',
    #  'agency_actions': [],
    #  'contracts':
    #      [{'person_type': 'ur',
    #        'subclient_actions': [lambda client_id: db.BalanceBO().execute(query=NON_RESIDENT_QUERY,
    #                                                                       named_params={'client_id': client_id})
    #                              ],
    #        'contract_info': {'type': 'comm_post',
    #                          'contract_params': {'SERVICES': [7],
    #                                                # 'NON_RESIDENT_CLIENTS': 0,
    #                                              'BANK_DETAILS_ID': 3,
    #                                              # 'DEAL_PASSPORT': HALF_YEAR_BEFORE_NOW_ISO,
    #                                              'CURRENCY': RUB,
    #                                              # 'DISCOUNT_POLICY_TYPE': BY_DISCOUNT_POLICY,
    #                                              'PERSONAL_ACCOUNT': 0,
    #                                              'LIFT_CREDIT_ON_PAYMENT': 0,
    #                                              'PERSONAL_ACCOUNT_FICTIVE': 0,
    #                                              'CREDIT_LIMIT_SINGLE': 6000,
    #                                              'CREDIT_LIMIT_IN_CONTRACT_CURRENCY': 0,
    #                                              'NON_RESIDENT_CLIENTS': 1,
    #                                              },
    #                          'credit': 1},
    #        'invoice': [(FISH_PRODUCT, D('118'), D('99.9'))],
    #        'paysys_id': BANK_NON_RES_WITH_AGENCY_USD
    #        }
    #      ],
    #  'expected_invoice_params': {'currency': 'RUR'},
    #  'currency_for_rate': Currencies.RUB.iso_code,
    #  'expected_spent_limit': lambda rate: [{'ISO_CURRENCY': Currencies.RUB.iso_code,
    #                            'LIMIT_SPENT': D('118') * rate,
    #                           }]
    # },
    #
    # {'description': 'RUB_contract_with_non_residents_in_USD',
    #  'agency_actions': [],
    #  'contracts':
    #      [{'person_type': 'ur',
    #        'subclient_actions': [lambda client_id: db.BalanceBO().execute(query=NON_RESIDENT_QUERY,
    #                                                                       named_params={'client_id': client_id})
    #                              ],
    #        'contract_info': {'type': 'comm_post',
    #                          'contract_params': {'SERVICES': [7],
    #                                                # 'NON_RESIDENT_CLIENTS': 0,
    #                                              'BANK_DETAILS_ID': 3,
    #                                              # 'DEAL_PASSPORT': HALF_YEAR_BEFORE_NOW_ISO,
    #                                              'CURRENCY': RUB,
    #                                              # 'DISCOUNT_POLICY_TYPE': BY_DISCOUNT_POLICY,
    #                                              'PERSONAL_ACCOUNT': 0,
    #                                              'LIFT_CREDIT_ON_PAYMENT': 0,
    #                                              'PERSONAL_ACCOUNT_FICTIVE': 0,
    #                                              'CREDIT_LIMIT_SINGLE': 5000,
    #                                              'CREDIT_LIMIT_IN_CONTRACT_CURRENCY': 1,
    #                                              'NON_RESIDENT_CLIENTS': 1,
    #                                              },
    #                          'credit': 1},
    #        'invoice': [(FISH_PRODUCT, D('118'), D('99.9'))],
    #        'paysys_id': BANK_NON_RES_WITH_AGENCY_USD
    #        }
    #      ],
    #  'expected_invoice_params': {'currency': 'RUR'},
    #  'currency_for_rate': Currencies.RUB.iso_code,
    #  'expected_spent_limit': lambda rate: [{'ISO_CURRENCY': Currencies.RUB.iso_code,
    #                            'LIMIT_SPENT': D('118') * rate,
    #                           }]
    # },
    #
    # {'description': 'BYN_contract_limit_in_contract_currency',
    #  'agency_actions': [],
    #  'contracts':
    #      [{'person_type': 'sw_yt',
    #        'subclient_actions': [],
    #        'contract_info': {'type': 'shv_agent_post',
    #                          'contract_params': {'SERVICES': [7],
    #                                                # 'NON_RESIDENT_CLIENTS': 0,
    #                                              # 'BANK_DETAILS_ID': 3,
    #                                              # 'DEAL_PASSPORT': HALF_YEAR_BEFORE_NOW_ISO,
    #                                              # 'CURRENCY': USD,
    #                                              # 'PERSONAL_ACCOUNT': 1,
    #                                              # 'LIFT_CREDIT_ON_PAYMENT': 0,
    #                                              # 'PERSONAL_ACCOUNT_FICTIVE': 0,
    #                                              'CREDIT_LIMIT_SINGLE': 6000,
    #                                              'CREDIT_LIMIT_IN_CONTRACT_CURRENCY': 1
    #                                              },
    #                          'credit': 1},
    #        'invoice': [(FISH_PRODUCT, D('118'), D('99.9'))],
    #        'paysys_id': BANK_USD_SW_YT_PAYSYS_ID
    #        }
    #      ],
    #  'currency_for_rate': Currencies.BYN.iso_code,
    #  'expected_spent_limit': [{'ISO_CURRENCY': Currencies.BYN.iso_code,
    #                            'LIMIT_SPENT': D('118'),
    #                           }]
    # },
    #
    # {'description': 'BYN_contract_limit_in_contract_currency',
    #  'agency_actions': [],
    #  'contracts':
    #      [{'person_type': 'sw_yt',
    #        'subclient_actions': [],
    #        'contract_info': {'type': 'shv_agent_post',
    #                          'contract_params': {'SERVICES': [7],
    #                                                # 'NON_RESIDENT_CLIENTS': 0,
    #                                              # 'BANK_DETAILS_ID': 3,
    #                                              # 'DEAL_PASSPORT': HALF_YEAR_BEFORE_NOW_ISO,
    #                                              # 'CURRENCY': USD,
    #                                              # 'PERSONAL_ACCOUNT': 1,
    #                                              # 'LIFT_CREDIT_ON_PAYMENT': 0,
    #                                              # 'PERSONAL_ACCOUNT_FICTIVE': 0,
    #                                              'CREDIT_LIMIT_SINGLE': 6000,
    #                                              'CREDIT_LIMIT_IN_CONTRACT_CURRENCY': 0
    #                                              },
    #                          'credit': 1},
    #        'invoice': [(FISH_PRODUCT, D('118'), D('99.9'))],
    #        'paysys_id': BANK_USD_SW_YT_PAYSYS_ID
    #        }
    #      ],
    #  'currency_for_rate': Currencies.BYN.iso_code,
    #  'expected_spent_limit': [{'ISO_CURRENCY': Currencies.BYN.iso_code,
    #                            'LIMIT_SPENT': D('118'),
    #                           }]
    # },
],
                         ids=lambda x: x['description']
                         )
def test_agency_invoice(scenario):
    agency_id = None or steps.ClientSteps.create({'IS_AGENCY': 1})

    # Condition for ability to use 'None' value in parametrization
    [action(agency_id) for action in scenario['agency_actions']]

    # Create invoices for different subclients (with different contracts)
    for contract in scenario['contracts']:
        person_id = None or steps.PersonSteps.create(agency_id, contract['person_type'])
        client_id = None or steps.ClientSteps.create()

        # Condition for ability to use 'None' value in parametrization
        [action(client_id) for action in contract['subclient_actions']]

        # Prepare contract params
        contract_params = {'CLIENT_ID': agency_id,
                           'PERSON_ID': person_id,
                           'DT': HALF_YEAR_BEFORE_NOW_ISO,
                           'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                           'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                           }

        contract_params.update(contract['contract_info']['contract_params'])
        contract_id, contract_eid = steps.ContractSteps.create_contract(contract['contract_info']['type'],
                                                                        contract_params)

        if scenario.get('force_fictive_credit_scheme', False):
            steps.ContractSteps.force_convert_to_fictive_credit_scheme(contract_id)

        # steps.ContractSteps.create_collateral(1033, {'CONTRACT2_ID': contract_id,
        #                                              'DT' : HALF_YEAR_BEFORE_NOW_ISO,
        #                                              'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO})

        # Remove 'expected_amount' from 'invoices' struct to have correct structure,
        # which can be sent to 'create_invoice_with_act' method
        orders_list, invoice_id = create_invoice_with_act(contract['invoice'], client_id=client_id, agency_id=agency_id,
                                                          person_id=person_id, paysys_id=contract['paysys_id'],
                                                          contract_id=contract_id,
                                                          credit=contract['contract_info']['credit'])

        invoice = db.get_invoice_by_id(invoice_id)

        target_product = contract['invoice'][0][0]
        result = api.medium().GetClientCreditLimits(agency_id, target_product.id)
        limit_spent = result['LIMITS']

        # expected = [{'CONTRACT_EID': contract_eid,
        #              'CONTRACT_ID': contract_id,
        #              'LIMIT_TOTAL': '6000.00'}]
        # expected.update(scenario['expected_spent_limit'])

        # {'CURRENCY': 'RUR',
        #  'LIMITS': [{'CONTRACT_EID': '95873/16',
        #              'CONTRACT_ID': '310361',
        #              'ISO_CURRENCY': 'BYN',
        #              'LIMIT_SPENT': '118.0000',
        #              'LIMIT_TOTAL': '6000.00'}],
        #  'PRODUCT_PRICE': '1.0000000000'}

        currency_rate = D(api.medium().server.GetCurrencyRate(scenario['currency_for_rate'], NOW)[2]['rate'])
        expected_limit = scenario['expected_spent_limit'](currency_rate)

        utils.check_that(limit_spent, mtch.contains_dicts_with_entries(expected_limit))
