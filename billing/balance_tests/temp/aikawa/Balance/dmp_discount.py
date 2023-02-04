# -*- coding: utf-8 -*-
# import datetime

#
# PRODUCT_LIST = [
# 508277,
# 508278,
# 508279,
# 508280,
# 508276,
# 508275,
# 508282,
# 508283,
# 508284,
# 508285,
# 508281]
# for product_id in PRODUCT_LIST:
#     steps.CommonSteps.export('OEBS', 'Product', product_id)


__author__ = 'atkaya'

import datetime
from decimal import Decimal as D

import pytest

import btestlib.reporter as reporter
from balance import balance_db as db
from balance import balance_steps as steps
from balance.balance_objects import Product
from balance.features import Features
from btestlib import utils as utils
from btestlib.matchers import has_entries_casted

BASE_DT = datetime.datetime.now()
to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

DMP_PRODUCT = 508318

DIRECT = 7
MARKET = 11
MEDIA = 70

DIRECT_PRODUCT = Product(DIRECT, 1475, 'Bucks', 'Money')
MARKET_PRODUCT = Product(MARKET, 2136, 'Bucks')
MEDIA_PRODUCT_31 = Product(MEDIA, 100000000, 'Money')  # 503341 валютный, Price = 1
MEDIA_PRODUCT_1 = Product(MEDIA, 503123, 'Shows')

VI_MINSK_AGENCY_ID = 1137155
PAYSYS_ID = 1014

SUM_WITH_DISCOUNT = D('2237.29')
CUSTOM_CREDIT_LIMIT = 600000000


def prepare_budget_by_payments(budget_owner, person_id, contract_id, paysys_id, budget_list):
    for product, completions, dt in budget_list:
        dt_1st = dt.replace(day=1)
        tmp_client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
        campaigns_list = [
            {'client_id': tmp_client_id, 'service_id': product.service_id, 'product_id': product.id, 'qty': completions,
             'begin_dt': dt_1st},
            {'client_id': tmp_client_id, 'service_id': 70, 'product_id': DMP_PRODUCT, 'qty': 100000000,
             'begin_dt': dt_1st}
        ]
        invoice_id, _, _, orders_list = steps.InvoiceSteps.create_force_invoice(client_id=tmp_client_id,
                                                                                person_id=person_id,
                                                                                campaigns_list=campaigns_list,
                                                                                paysys_id=paysys_id,
                                                                                invoice_dt=dt_1st,
                                                                                agency_id=budget_owner,
                                                                                # credit=p.get('is_fpa_budget', 0),
                                                                                credit=0,
                                                                                contract_id=contract_id,
                                                                                overdraft=0,
                                                                                manager_uid=None)
        steps.InvoiceSteps.pay(invoice_id)
        steps.CampaignsSteps.do_campaigns(product.service_id, orders_list[0]['ServiceOrderID'],
                                          {product.shipment_type: completions}, 0, dt_1st)


def prepare_budget_by_acts(budget_owner, person_id, contract_id, paysys_id, budget_list):
    prepare_budget_by_payments(budget_owner, person_id, contract_id, paysys_id, budget_list)
    for product, completions, dt in budget_list:
        steps.ActsSteps.generate(budget_owner, 1, dt)


@pytest.mark.priority('mid')
@reporter.feature(Features.DISCOUNT)
@pytest.mark.tickets('BALANCE-')
@pytest.mark.docpath('https://wiki.yandex-team.ru/balance/docs/process/discount')
@pytest.mark.docs(u'--group', u'Автотесты: Беларусь, агентские, Директ')
@pytest.mark.parametrize('p', [
    # 1: 1 месяц
    pytest.mark.smoke(
        utils.aDict({'acts': [(DIRECT_PRODUCT, 787, datetime.datetime(2016, 8, 30, 0, 0, 0)),
                              (DIRECT_PRODUCT, 787, datetime.datetime(2016, 8, 30, 0, 0, 0)),
                              (DIRECT_PRODUCT, 787, datetime.datetime(2016, 8, 30, 0, 0, 0)),
                              (DIRECT_PRODUCT, 787, datetime.datetime(2016, 8, 30, 0, 0, 0)),
                              (DIRECT_PRODUCT, 787, datetime.datetime(2016, 8, 30, 0, 0, 0))
                              ],
                     'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2016, 9, 11, 0, 0, 0)),
                     'contract_start_dt': datetime.datetime(2016, 8, 1, 0, 0, 0),
                     'discount_type': 16,
                     'expected_discount_pct': 12})),

    # 2: 2 месяца
    utils.aDict({'acts': [(DIRECT_PRODUCT, 1487, datetime.datetime(2016, 1, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 1487, datetime.datetime(2016, 1, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 1487, datetime.datetime(2016, 1, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 1487, datetime.datetime(2016, 1, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 1487, datetime.datetime(2016, 1, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 87, datetime.datetime(2016, 2, 29, 0, 0, 0)),
                          (DIRECT_PRODUCT, 87, datetime.datetime(2016, 2, 29, 0, 0, 0)),
                          (DIRECT_PRODUCT, 87, datetime.datetime(2016, 2, 29, 0, 0, 0)),
                          (DIRECT_PRODUCT, 87, datetime.datetime(2016, 2, 29, 0, 0, 0)),
                          (DIRECT_PRODUCT, 87, datetime.datetime(2016, 2, 29, 0, 0, 0))
                          ],
                 'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2016, 3, 10, 0, 0, 0)),
                 'contract_start_dt': datetime.datetime(2016, 1, 1, 0, 0, 0),
                 'discount_type': 16,
                 'expected_discount_pct': 12}),

    # 3: 3 месяца
    utils.aDict({'acts': [(DIRECT_PRODUCT, 1487, datetime.datetime(2016, 1, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 1487, datetime.datetime(2016, 1, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 1487, datetime.datetime(2016, 1, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 1487, datetime.datetime(2016, 1, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 1487, datetime.datetime(2016, 1, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 787, datetime.datetime(2016, 2, 29, 0, 0, 0)),
                          (DIRECT_PRODUCT, 787, datetime.datetime(2016, 2, 29, 0, 0, 0)),
                          (DIRECT_PRODUCT, 787, datetime.datetime(2016, 2, 29, 0, 0, 0)),
                          (DIRECT_PRODUCT, 787, datetime.datetime(2016, 2, 29, 0, 0, 0)),
                          (DIRECT_PRODUCT, 787, datetime.datetime(2016, 2, 29, 0, 0, 0)),
                          (DIRECT_PRODUCT, 98, datetime.datetime(2016, 3, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 98, datetime.datetime(2016, 3, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 98, datetime.datetime(2016, 3, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 98, datetime.datetime(2016, 3, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 97, datetime.datetime(2016, 3, 31, 0, 0, 0))
                          ],
                 'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2016, 4, 10, 0, 0, 0)),
                 'contract_start_dt': datetime.datetime(2016, 1, 1, 0, 0, 0),
                 'discount_type': 16,
                 'expected_discount_pct': 12}),

    # 4: 4 месяца, есть скидка, бюджет по предыдущим 3ем месяцам > 100 000
    utils.aDict({'acts': [(DIRECT_PRODUCT, 10000, datetime.datetime(2016, 1, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 10000, datetime.datetime(2016, 1, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 10000, datetime.datetime(2016, 1, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 10000, datetime.datetime(2016, 1, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 10000, datetime.datetime(2016, 1, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 2220, datetime.datetime(2016, 2, 29, 0, 0, 0)),
                          (DIRECT_PRODUCT, 2232, datetime.datetime(2016, 2, 29, 0, 0, 0)),
                          (DIRECT_PRODUCT, 2232, datetime.datetime(2016, 2, 29, 0, 0, 0)),
                          (DIRECT_PRODUCT, 2232, datetime.datetime(2016, 2, 29, 0, 0, 0)),
                          (DIRECT_PRODUCT, 2232, datetime.datetime(2016, 2, 29, 0, 0, 0)),
                          (DIRECT_PRODUCT, 50, datetime.datetime(2016, 3, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 50, datetime.datetime(2016, 3, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 50, datetime.datetime(2016, 3, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 50, datetime.datetime(2016, 3, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 50, datetime.datetime(2016, 3, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 100, datetime.datetime(2016, 4, 30, 0, 0, 0)),
                          (DIRECT_PRODUCT, 100, datetime.datetime(2016, 4, 30, 0, 0, 0)),
                          (DIRECT_PRODUCT, 100, datetime.datetime(2016, 4, 30, 0, 0, 0)),
                          (DIRECT_PRODUCT, 100, datetime.datetime(2016, 4, 30, 0, 0, 0)),
                          (DIRECT_PRODUCT, 100, datetime.datetime(2016, 4, 30, 0, 0, 0))
                          ],
                 'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2016, 5, 10, 0, 0, 0)),
                 'contract_start_dt': datetime.datetime(2016, 1, 1, 0, 0, 0),
                 'discount_type': 16,
                 'expected_discount_pct': 13}),

    # 5: 4 месяца, нет скидки, бюджет по предыдущим 3ем месяцам < 100 000
    utils.aDict({'acts': [(DIRECT_PRODUCT, 10000, datetime.datetime(2016, 2, 29, 0, 0, 0)),
                          (DIRECT_PRODUCT, 10000, datetime.datetime(2016, 2, 29, 0, 0, 0)),
                          (DIRECT_PRODUCT, 10000, datetime.datetime(2016, 2, 29, 0, 0, 0)),
                          (DIRECT_PRODUCT, 10000, datetime.datetime(2016, 2, 29, 0, 0, 0)),
                          (DIRECT_PRODUCT, 10000, datetime.datetime(2016, 2, 29, 0, 0, 0)),
                          (DIRECT_PRODUCT, 2219, datetime.datetime(2016, 3, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 2232, datetime.datetime(2016, 3, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 2232, datetime.datetime(2016, 3, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 2232, datetime.datetime(2016, 3, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 2232, datetime.datetime(2016, 3, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 50, datetime.datetime(2016, 4, 30, 0, 0, 0)),
                          (DIRECT_PRODUCT, 50, datetime.datetime(2016, 4, 30, 0, 0, 0)),
                          (DIRECT_PRODUCT, 50, datetime.datetime(2016, 4, 30, 0, 0, 0)),
                          (DIRECT_PRODUCT, 50, datetime.datetime(2016, 4, 30, 0, 0, 0)),
                          (DIRECT_PRODUCT, 50, datetime.datetime(2016, 4, 30, 0, 0, 0)),
                          (DIRECT_PRODUCT, 100, datetime.datetime(2016, 5, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 100, datetime.datetime(2016, 5, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 100, datetime.datetime(2016, 5, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 100, datetime.datetime(2016, 5, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 1, datetime.datetime(2016, 5, 31, 0, 0, 0))
                          ],
                 'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2016, 6, 10, 0, 0, 0)),
                 'contract_start_dt': datetime.datetime(2016, 1, 1, 0, 0, 0),
                 'discount_type': 16,
                 'expected_discount_pct': 0}),

    # 6: 4 месяца, нет скидки, клиентов в предыдущем 4
    utils.aDict({'acts': [(DIRECT_PRODUCT, 10000, datetime.datetime(2016, 1, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 10000, datetime.datetime(2016, 1, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 10000, datetime.datetime(2016, 1, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 10000, datetime.datetime(2016, 1, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 10000, datetime.datetime(2016, 1, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 10000, datetime.datetime(2016, 2, 29, 0, 0, 0)),
                          (DIRECT_PRODUCT, 10000, datetime.datetime(2016, 2, 29, 0, 0, 0)),
                          (DIRECT_PRODUCT, 10000, datetime.datetime(2016, 2, 29, 0, 0, 0)),
                          (DIRECT_PRODUCT, 10000, datetime.datetime(2016, 2, 29, 0, 0, 0)),
                          (DIRECT_PRODUCT, 10000, datetime.datetime(2016, 2, 29, 0, 0, 0)),
                          (DIRECT_PRODUCT, 10000, datetime.datetime(2016, 3, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 10000, datetime.datetime(2016, 3, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 10000, datetime.datetime(2016, 3, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 10000, datetime.datetime(2016, 3, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 10000, datetime.datetime(2016, 3, 31, 0, 0, 0)),
                          (MEDIA_PRODUCT_1, 100000, datetime.datetime(2016, 4, 30, 0, 0, 0)),
                          (DIRECT_PRODUCT, 10000, datetime.datetime(2016, 4, 30, 0, 0, 0)),
                          (DIRECT_PRODUCT, 10000, datetime.datetime(2016, 4, 30, 0, 0, 0)),
                          (DIRECT_PRODUCT, 10000, datetime.datetime(2016, 4, 30, 0, 0, 0)),
                          (DIRECT_PRODUCT, 10000, datetime.datetime(2016, 4, 30, 0, 0, 0))
                          ],
                 'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2016, 5, 10, 0, 0, 0)),
                 'contract_start_dt': datetime.datetime(2016, 1, 1, 0, 0, 0),
                 'discount_type': 16,
                 'expected_discount_pct': 0}),

    # 7: 4 месяца, есть скидка, за предыдущий месяц 5 клиентов, раньше по одному
    utils.aDict({'acts': [(DIRECT_PRODUCT, 50000, datetime.datetime(2016, 1, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 50000, datetime.datetime(2016, 2, 29, 0, 0, 0)),
                          (DIRECT_PRODUCT, 50000, datetime.datetime(2016, 3, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 100, datetime.datetime(2016, 4, 30, 0, 0, 0)),
                          (DIRECT_PRODUCT, 100, datetime.datetime(2016, 4, 30, 0, 0, 0)),
                          (DIRECT_PRODUCT, 100, datetime.datetime(2016, 4, 30, 0, 0, 0)),
                          (DIRECT_PRODUCT, 100, datetime.datetime(2016, 4, 30, 0, 0, 0)),
                          (DIRECT_PRODUCT, 100, datetime.datetime(2016, 4, 30, 0, 0, 0))
                          ],
                 'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2016, 5, 10, 0, 0, 0)),
                 'contract_start_dt': datetime.datetime(2016, 1, 1, 0, 0, 0),
                 'discount_type': 16,
                 'expected_discount_pct': 14}),

    # 8: 1 месяц, счет до 10ого числа, скидки нет
    utils.aDict({'acts': [(DIRECT_PRODUCT, 787, datetime.datetime(2016, 1, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 787, datetime.datetime(2016, 1, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 787, datetime.datetime(2016, 1, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 787, datetime.datetime(2016, 1, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 787, datetime.datetime(2016, 1, 31, 0, 0, 0))
                          ],
                 'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2016, 2, 9, 0, 0, 0)),
                 'contract_start_dt': datetime.datetime(2016, 1, 1, 0, 0, 0),
                 'discount_type': 16,
                 'expected_discount_pct': 0}),

    # 9: 4 месяца, нет скидки, у клиента > 70%
    utils.aDict({'acts': [(DIRECT_PRODUCT, 5000, datetime.datetime(2016, 1, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 50000, datetime.datetime(2016, 2, 29, 0, 0, 0)),
                          (DIRECT_PRODUCT, 20000, datetime.datetime(2016, 3, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 20000, datetime.datetime(2016, 3, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 20000, datetime.datetime(2016, 3, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 20000, datetime.datetime(2016, 3, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 20000, datetime.datetime(2016, 3, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 150, datetime.datetime(2016, 4, 30, 0, 0, 0)),
                          (DIRECT_PRODUCT, 54, datetime.datetime(2016, 4, 30, 0, 0, 0)),
                          (DIRECT_PRODUCT, 50, datetime.datetime(2016, 4, 30, 0, 0, 0)),
                          (DIRECT_PRODUCT, 50, datetime.datetime(2016, 4, 30, 0, 0, 0)),
                          (DIRECT_PRODUCT, 696, datetime.datetime(2016, 4, 30, 0, 0, 0))
                          ],
                 'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2016, 5, 10, 0, 0, 0)),
                 'contract_start_dt': datetime.datetime(2016, 1, 1, 0, 0, 0),
                 'discount_type': 16,
                 'expected_discount_pct': 0}),

    # 10: договор действует с 15 года, расчет только за 12 месяцев, только директ (бюджет на 10.02.2015 = 2 512 754,21)
    utils.aDict({'acts': [(DIRECT_PRODUCT, 50000, datetime.datetime(2015, 1, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 30000, datetime.datetime(2015, 2, 28, 0, 0, 0)),
                          (DIRECT_PRODUCT, 80000, datetime.datetime(2015, 12, 31, 0, 0, 0)),
                          (MEDIA_PRODUCT_1, 8000000, datetime.datetime(2016, 1, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 787, datetime.datetime(2016, 1, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 787, datetime.datetime(2016, 1, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 787, datetime.datetime(2016, 1, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 787, datetime.datetime(2016, 1, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 787, datetime.datetime(2016, 1, 31, 0, 0, 0))
                          ],
                 'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2016, 2, 10, 0, 0, 0)),
                 'contract_start_dt': datetime.datetime(2015, 1, 1, 0, 0, 0),
                 'discount_type': 16,
                 'expected_discount_pct': 14}),

    # 11: договор действует с 15 года, до 10 января 2016 скидка есть, по новым условиям
    utils.aDict({'acts': [(DIRECT_PRODUCT, 28000, datetime.datetime(2015, 11, 30, 0, 0, 0)),
                          (DIRECT_PRODUCT, 28000, datetime.datetime(2015, 11, 30, 0, 0, 0)),
                          (DIRECT_PRODUCT, 28000, datetime.datetime(2015, 11, 30, 0, 0, 0)),
                          (DIRECT_PRODUCT, 28000, datetime.datetime(2015, 11, 30, 0, 0, 0)),
                          (DIRECT_PRODUCT, 28000, datetime.datetime(2015, 11, 30, 0, 0, 0))
                          ],
                 'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2016, 1, 9, 0, 0, 0)),
                 'contract_start_dt': datetime.datetime(2015, 1, 1, 0, 0, 0),
                 'discount_type': 16,
                 'expected_discount_pct': 14}),

    # 12: самая большая скидка
    utils.aDict({'acts': [(DIRECT_PRODUCT, 236000, datetime.datetime(2016, 1, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 236000, datetime.datetime(2016, 1, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 236000, datetime.datetime(2016, 1, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 236000, datetime.datetime(2016, 1, 31, 0, 0, 0)),
                          (DIRECT_PRODUCT, 236000, datetime.datetime(2016, 1, 31, 0, 0, 0))
                          ],
                 'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2016, 2, 10, 0, 0, 0)),
                 'contract_start_dt': datetime.datetime(2016, 1, 1, 0, 0, 0),
                 'discount_type': 16,
                 'expected_discount_pct': 19}),

    # 13: тип скидки беларусь 2012
    utils.aDict({'acts': [(DIRECT_PRODUCT, 787, datetime.datetime(2016, 4, 30, 0, 0, 0)),
                          (DIRECT_PRODUCT, 787, datetime.datetime(2016, 4, 30, 0, 0, 0)),
                          (DIRECT_PRODUCT, 787, datetime.datetime(2016, 4, 30, 0, 0, 0)),
                          (DIRECT_PRODUCT, 787, datetime.datetime(2016, 4, 30, 0, 0, 0)),
                          (DIRECT_PRODUCT, 787, datetime.datetime(2016, 4, 30, 0, 0, 0))
                          ],
                 'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2016, 5, 10, 0, 0, 0)),
                 'contract_start_dt': datetime.datetime(2016, 4, 1, 0, 0, 0),
                 'discount_type': 14,
                 'expected_discount_pct': 12}),

    # TODO: + кредит
])
def test_BY_2016_agency_direct_discount(p):
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    person_id = steps.PersonSteps.create(agency_id, 'yt', {})
    contract_id, _ = steps.ContractSteps.create_contract('agency_belarus',
                                                         {'CLIENT_ID': agency_id, 'PERSON_ID': person_id,
                                                          'DT': to_iso(p.contract_start_dt),
                                                          'FINISH_DT': to_iso(BASE_DT + dt_delta(days=180)),
                                                          'SERVICES': [DIRECT, MEDIA],
                                                          'CREDIT_LIMIT_SINGLE': 600000000,
                                                          'DISCOUNT_POLICY_TYPE': p.discount_type
                                                          })

    if p.get('is_fpa_budget', 0):
        steps.ContractSteps.create_collateral(1033,
                                              {'CONTRACT2_ID': contract_id,
                                               'DT': to_iso(p.contract_start_dt),
                                               'FINISH_DT': to_iso(BASE_DT + dt_delta(days=180)),
                                               'IS_SIGNED': to_iso(p.contract_start_dt),
                                               'CREDIT_LIMIT_SINGLE': 600000000,
                                               })

    # Создаём бюджет
    prepare_budget_by_acts(agency_id, person_id, contract_id, PAYSYS_ID, p.acts)
    # -----------------------------------------------------------------------------------------------------------------

    # Серия проверок для целевого счёта
    product, qty, request_dt = p.target_invoice
    credit = p.get('is_credit', 0)

    # Проверяем ответ метода EstimateDiscount
    result = steps.DiscountSteps.estimate_discount(
        {'ClientID': agency_id, 'PaysysID': PAYSYS_ID, 'ContractID': contract_id},
        [{'ProductID': product.id, 'ClientID': client_id, 'Qty': qty, 'ID': 1,
          'BeginDT': request_dt, 'RegionID': None, 'discard_agency_discount': 0}])
    # utils.check_that(D(result['AgencyDiscountPct']), equal_to(p.expected_discount_pct))

    # Метод ответ метода GetClientDiscountsAll
    result = steps.DiscountSteps.get_client_discounts_all({'ClientID': agency_id, 'DT': request_dt})
    # TODO: добавить проверку

    # ---------- Request ----------
    # service_order_id = steps.OrderSteps.next_id(product.service_id)
    # order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=product.service_id, product_id=product.id,
    #                                    params={'AgencyID': agency_id})
    # orders_list = [
    #     {'ServiceID': product.service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': request_dt}
    # ]
    # # request_id = steps.RequestSteps.create(invoice_owner, orders_list, additional_params={'invoice_dt': scenario['req_dt']})
    # request_id = steps.RequestSteps.create(agency_id, orders_list, additional_params={'InvoiceDesireDT': request_dt})
    # invoice_id, external_id, total_sum = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID,
    #                                                                credit=scenario['is_credit'] if 'is_credit' in scenario else 0,
    #                                                                contract_id=contract_id, overdraft=0,
    #                                                                endbuyer_id=None)
    # steps.InvoiceSteps.pay(invoice_id)
    #
    # consume = db.get_consumes_by_invoice(invoice_id)[0]
    # utils.check_that(consume['static_discount_pct'], equal_to(scenario['expected_discount_pct']))

    # Выставляем и оплачиваем целевой счёт
    campaigns_list = [{'service_id': product.service_id, 'product_id': product.id, 'qty': qty, 'begin_dt': request_dt},
                      {'service_id': 70, 'product_id': DMP_PRODUCT, 'qty': qty, 'begin_dt': request_dt}]
    invoice_id, _, _, orders_list = steps.InvoiceSteps.create_force_invoice(client_id=client_id,
                                                                            person_id=person_id,
                                                                            campaigns_list=campaigns_list,
                                                                            paysys_id=PAYSYS_ID,
                                                                            invoice_dt=request_dt,
                                                                            agency_id=agency_id,
                                                                            credit=credit,
                                                                            contract_id=contract_id,
                                                                            overdraft=0,
                                                                            )
    if not credit:
        steps.InvoiceSteps.pay(invoice_id)

    # Проверка скидки в целевом счёте
    for order in orders_list:
        order_info = db.get_order_by_service_id_and_service_order_id(order['ServiceID'], order['ServiceOrderID'])[0]
        service_code = order_info['service_code']
        order_id = order_info['id']
        consumes = db.get_consumes_by_order(order_id)
        if service_code != DMP_PRODUCT:

            utils.check_that(consumes[0],
                             has_entries_casted({'static_discount_pct': p.expected_discount_pct}),
                             step=u'Проверяем сумму и скидку в заявке')
        else:
            utils.check_that(consumes[0],
                             has_entries_casted({'static_discount_pct': 0}),
                             step=u'Проверяем сумму и скидку в заявке')


if __name__ == '__main__':
    pass
    pytest.main('-v test_2017_BY_agency_DIRECT.py::test_BY_2016_agency_direct_discount[p4] -n4')
