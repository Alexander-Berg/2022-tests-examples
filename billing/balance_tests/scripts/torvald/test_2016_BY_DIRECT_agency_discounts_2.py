# -*- coding: utf-8 -*-

__author__ = 'atkaya'

import datetime

import pytest
from hamcrest import equal_to

import btestlib.reporter as reporter
from balance import balance_api as api
from balance import balance_db as db
from balance import balance_steps as steps
from balance.balance_objects import Product
from balance.features import Features
from btestlib import utils as utils

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))

BY_DISCOUNT_POLICY = 16
OTHER_DISCOUNT_POLICY = 1

DIRECT = 7
MEDIA = 70

DIRECT_PRODUCT = Product(7, 1475, 'Bucks', 'Money')
MEDIA_PRODUCT_1 = Product(70, 503123, 'Shows')

RUB = 810
BYN = 933

PAYSYS_MAP = {RUB: 1014, BYN: 1100}


# 5 клиентов:
# 	RUB
# 	BYN
# 	RUB+BYN (1+3)
# 	RUB+BYN (1+4)
# 	RUB+BYN (4+1)
# 70%:
# 	RUB
# 	BYN
# 	RUB+BYN
# 	69.49%
# 	69.5%
# 	нет Директа
# 3000:
# 	RUB давно, BYN новый, все акты в BYN
# 	BYN давно, RUB новый, все акты в RUB
# 	RUB+BYN > 3000 (соответствие к 100000 руб по курсу)
# 	RUB+BYN < 3000 (соответствие к 100000 руб по курсу)
#
# Двонаправленная связь
#
# Бюджет за 12 месяцев (акты ранее)
# Шкала
# НЕ действует старая шкала
#
#
# (!) связка в обратную сторону


# def prepare_budget(acts, agency_id, person_id, contract_id, paysys_id, credit):
def prepare_budget(**kwargs):
    acts = kwargs['acts']
    agency_id = kwargs['agency_id']
    person_id = kwargs['person_id']
    contract_id = kwargs['contract_id']
    paysys_id = kwargs['paysys_id']
    credit = kwargs['credit']
    for product, completions, dt in acts:
        # Создаём отдельного субклиента для каждого счёта:
        tmp_client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
        # Счёт выставляем на сумму, отличаюущуюся от желаемой суммы акта:
        qty_for_invoice = completions * 2

        # Выставляем счёт на указанную сумму, оплачиваем его, откручиваем и актим
        campaigns_list = [
            {'client_id': tmp_client_id, 'service_id': product.service_id, 'product_id': product.id,
             'qty': qty_for_invoice,
             'begin_dt': dt}
        ]
        invoice_id, _, _, orders_list = steps.InvoiceSteps.create_force_invoice(tmp_client_id,
                                                                                person_id,
                                                                                campaigns_list,
                                                                                paysys_id,
                                                                                dt,
                                                                                agency_id=agency_id,
                                                                                credit=0,
                                                                                contract_id=contract_id,
                                                                                overdraft=0,
                                                                                manager_uid=None)
        steps.InvoiceSteps.pay(invoice_id)
        steps.CampaignsSteps.do_campaigns(product.service_id, orders_list[0]['ServiceOrderID'],
                                          {product.shipment_type: completions}, 0, dt)
        steps.ActsSteps.generate(agency_id, 1, dt)


# Мультивалютность

@reporter.feature(Features.DISCOUNT)
@pytest.mark.parametrize('scenario', [
    # # 0: Регрессионная проверка RUB, 12%
    #     {'budget': [{'RUB_contract': {'contract_type': 'opt_agency_post',
    #                                   'contract_params': {'DT': '2016-08-11T00:00:00',
    #                                                       'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
    #                                                       'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
    #                                                       'DEAL_PASSPORT': '2016-08-11T00:00:00',
    #                                                       'SERVICES': [DIRECT, MEDIA],
    #                                                       'CURRENCY': RUB,
    #                                                       'DISCOUNT_POLICY_TYPE': BY_DISCOUNT_POLICY},
    #                                  'acts':   [(DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 5, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 10, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 15, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 25, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 30, 0, 0, 0))],
    #                                 },
    #                 },
    #                 {'BYN_contract': {'contract_type': 'opt_agency_post',
    #                                   'contract_params': {'DT': '2016-08-11T00:00:00',
    #                                                       'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
    #                                                       'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
    #                                                       'DEAL_PASSPORT': '2016-08-11T00:00:00',
    #                                                       'SERVICES': [DIRECT, MEDIA],
    #                                                       'CURRENCY': BYN,
    #                                                       'DISCOUNT_POLICY_TYPE': BY_DISCOUNT_POLICY,
    #                                                       # Для связывания используем первый созданный в этой валюте договор
    #                                                       'LINK_CONTRACT_ID': lambda con, curr: con[curr][0]},
    #                                  'acts':   [(DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 5, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 10, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 15, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 25, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 30, 0, 0, 0))],
    #                                 },
    #                 }],
    #      'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2016, 10, 10, 0, 0, 0)),
    #      'target_currency': RUB,
    #      'expected_discount_pct': 12},
    #
    # # RUB, 5 клиентов
    #     {'budget': [{'RUB_contract': {'contract_type': 'opt_agency_post',
    #                                   'contract_params': {'DT': '2016-08-11T00:00:00',
    #                                                       'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
    #                                                       'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
    #                                                       'DEAL_PASSPORT': '2016-08-11T00:00:00',
    #                                                       'SERVICES': [DIRECT, MEDIA],
    #                                                       'CURRENCY': RUB,
    #                                                       'DISCOUNT_POLICY_TYPE': BY_DISCOUNT_POLICY},
    #                                  'acts':   [(DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 5, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 10, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 15, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 25, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 30, 0, 0, 0))],
    #                                 },
    #                 }],
    #      'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2016, 10, 10, 0, 0, 0)),
    #      'target_currency': RUB,
    #      'expected_discount_pct': 12},
    #
    # # BYN, 5 клиента
    #     {'budget': [{'BYN_contract': {'contract_type': 'opt_agency_post',
    #                                   'contract_params': {'DT': '2016-08-11T00:00:00',
    #                                                       'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
    #                                                       'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
    #                                                       'DEAL_PASSPORT': '2016-08-11T00:00:00',
    #                                                       'SERVICES': [DIRECT, MEDIA],
    #                                                       'CURRENCY': BYN,
    #                                                       'DISCOUNT_POLICY_TYPE': BY_DISCOUNT_POLICY},
    #                                  'acts':   [(DIRECT_PRODUCT, 2000, datetime.datetime(2016, 9, 5, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 2000, datetime.datetime(2016, 9, 10, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 2000, datetime.datetime(2016, 9, 15, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 2000, datetime.datetime(2016, 9, 25, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 2000, datetime.datetime(2016, 9, 30, 0, 0, 0))],
    #                                 },
    #                 }],
    #      'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2016, 10, 10, 0, 0, 0)),
    #      'target_currency': BYN,
    #      'expected_discount_pct': 12},
    #
    # # RUB, 4 клиента
    #     {'budget': [{'RUB_contract': {'contract_type': 'opt_agency_post',
    #                                   'contract_params': {'DT': '2016-08-11T00:00:00',
    #                                                       'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
    #                                                       'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
    #                                                       'DEAL_PASSPORT': '2016-08-11T00:00:00',
    #                                                       'SERVICES': [DIRECT, MEDIA],
    #                                                       'CURRENCY': RUB,
    #                                                       'DISCOUNT_POLICY_TYPE': BY_DISCOUNT_POLICY},
    #                                  'acts':   [(DIRECT_PRODUCT, 2000, datetime.datetime(2016, 9, 5, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 2000, datetime.datetime(2016, 9, 10, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 2000, datetime.datetime(2016, 9, 15, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 2000, datetime.datetime(2016, 9, 25, 0, 0, 0))],
    #                                 },
    #                 }],
    #      'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2016, 10, 10, 0, 0, 0)),
    #      'target_currency': RUB,
    #      'expected_discount_pct': 0},

    # # BYN, 4 клиента
    #     {'budget': [{'BYN_contract': {'contract_type': 'opt_agency_post',
    #                                   'contract_params': {'DT': '2016-08-11T00:00:00',
    #                                                       'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
    #                                                       'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
    #                                                       'DEAL_PASSPORT': '2016-08-11T00:00:00',
    #                                                       'SERVICES': [DIRECT, MEDIA],
    #                                                       'CURRENCY': BYN,
    #                                                       'DISCOUNT_POLICY_TYPE': BY_DISCOUNT_POLICY},
    #                                  'acts':   [(DIRECT_PRODUCT, 2000, datetime.datetime(2016, 9, 5, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 2000, datetime.datetime(2016, 9, 10, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 2000, datetime.datetime(2016, 9, 15, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 2000, datetime.datetime(2016, 9, 25, 0, 0, 0))],
    #                                 },
    #                 }],
    #      'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2016, 10, 10, 0, 0, 0)),
    #      'target_currency': BYN,
    #      'expected_discount_pct': 0},






    # ---------------------------------------------------------------------------------------------------------------------
    # ---------------------------------------------------------------------------------------------------------------------
    # https://st.yandex-team.ru/BALANCE-23827
    # ---------------------------------------------------------------------------------------------------------------------
    # ---------------------------------------------------------------------------------------------------------------------




    # 0: RUB + BYN, 5 клиентов = 3 + 2, target = RUB
    {'budget': [{'RUB_contract': {'contract_type': 'opt_agency_post',
                                  'contract_params': {'DT': '2016-08-11T00:00:00',
                                                      'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                                                      'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                                                      'DEAL_PASSPORT': '2016-08-11T00:00:00',
                                                      'SERVICES': [DIRECT, MEDIA],
                                                      'CURRENCY': RUB,
                                                      'DISCOUNT_POLICY_TYPE': BY_DISCOUNT_POLICY},
                                  'acts': [(DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 5, 0, 0, 0)),
                                           (DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 10, 0, 0, 0)),
                                           (DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 15, 0, 0, 0))],
                                  },
                 },
                {'BYN_contract': {'contract_type': 'opt_agency_post',
                                  'contract_params': {'DT': '2016-08-11T00:00:00',
                                                      'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                                                      'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                                                      'DEAL_PASSPORT': '2016-08-11T00:00:00',
                                                      'SERVICES': [DIRECT, MEDIA],
                                                      'CURRENCY': BYN,
                                                      'DISCOUNT_POLICY_TYPE': BY_DISCOUNT_POLICY,
                                                      # Для связывания используем первый созданный в этой валюте договор
                                                      'LINK_CONTRACT_ID': lambda con, curr: con[curr][0]},
                                  'acts': [(DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 5, 0, 0, 0)),
                                           (DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 10, 0, 0, 0))],
                                  },
                 }],
     'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2016, 10, 10, 0, 0, 0)),
     'target_currency': RUB,
     'expected_discount_pct': 12},

    # 0: RUB + BYN, 5 клиентов = 3 + 2, target = BYN
    {'budget': [{'RUB_contract': {'contract_type': 'opt_agency_post',
                                  'contract_params': {'DT': '2016-08-11T00:00:00',
                                                      'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                                                      'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                                                      'DEAL_PASSPORT': '2016-08-11T00:00:00',
                                                      'SERVICES': [DIRECT, MEDIA],
                                                      'CURRENCY': RUB,
                                                      'DISCOUNT_POLICY_TYPE': BY_DISCOUNT_POLICY},
                                  'acts': [(DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 5, 0, 0, 0)),
                                           (DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 10, 0, 0, 0)),
                                           (DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 15, 0, 0, 0))],
                                  },
                 },
                {'BYN_contract': {'contract_type': 'opt_agency_post',
                                  'contract_params': {'DT': '2016-08-11T00:00:00',
                                                      'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                                                      'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                                                      'DEAL_PASSPORT': '2016-08-11T00:00:00',
                                                      'SERVICES': [DIRECT, MEDIA],
                                                      'CURRENCY': BYN,
                                                      'DISCOUNT_POLICY_TYPE': BY_DISCOUNT_POLICY,
                                                      # Для связывания используем первый созданный в этой валюте договор
                                                      'LINK_CONTRACT_ID': lambda con, curr: con[curr][0]},
                                  'acts': [(DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 5, 0, 0, 0)),
                                           (DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 10, 0, 0, 0))],
                                  },
                 }],
     'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2016, 10, 10, 0, 0, 0)),
     'target_currency': BYN,
     'expected_discount_pct': 12},

    # 0: RUB + BYN, 5 клиентов = 5 + 2, target = BYN
    {'budget': [{'RUB_contract': {'contract_type': 'opt_agency_post',
                                  'contract_params': {'DT': '2016-08-11T00:00:00',
                                                      'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                                                      'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                                                      'DEAL_PASSPORT': '2016-08-11T00:00:00',
                                                      'SERVICES': [DIRECT, MEDIA],
                                                      'CURRENCY': RUB,
                                                      'DISCOUNT_POLICY_TYPE': BY_DISCOUNT_POLICY},
                                  'acts': [(DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 5, 0, 0, 0)),
                                           (DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 10, 0, 0, 0)),
                                           (DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 15, 0, 0, 0)),
                                           (DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 20, 0, 0, 0)),
                                           (DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 25, 0, 0, 0))],
                                  },
                 },
                {'BYN_contract': {'contract_type': 'opt_agency_post',
                                  'contract_params': {'DT': '2016-08-11T00:00:00',
                                                      'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                                                      'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                                                      'DEAL_PASSPORT': '2016-08-11T00:00:00',
                                                      'SERVICES': [DIRECT, MEDIA],
                                                      'CURRENCY': BYN,
                                                      'DISCOUNT_POLICY_TYPE': BY_DISCOUNT_POLICY,
                                                      # Для связывания используем первый созданный в этой валюте договор
                                                      'LINK_CONTRACT_ID': lambda con, curr: con[curr][0]},
                                  'acts': [(DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 5, 0, 0, 0)),
                                           (DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 10, 0, 0, 0))],
                                  },
                 }],
     'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2016, 10, 10, 0, 0, 0)),
     'target_currency': BYN,
     'expected_discount_pct': 12},

    # 0: RUB + BYN, 5 клиентов = 2 + 5, target = RUB
    {'budget': [{'RUB_contract': {'contract_type': 'opt_agency_post',
                                  'contract_params': {'DT': '2016-08-11T00:00:00',
                                                      'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                                                      'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                                                      'DEAL_PASSPORT': '2016-08-11T00:00:00',
                                                      'SERVICES': [DIRECT, MEDIA],
                                                      'CURRENCY': RUB,
                                                      'DISCOUNT_POLICY_TYPE': BY_DISCOUNT_POLICY},
                                  'acts': [(DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 5, 0, 0, 0)),
                                           (DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 10, 0, 0, 0)), ],
                                  },
                 },
                {'BYN_contract': {'contract_type': 'opt_agency_post',
                                  'contract_params': {'DT': '2016-08-11T00:00:00',
                                                      'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                                                      'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                                                      'DEAL_PASSPORT': '2016-08-11T00:00:00',
                                                      'SERVICES': [DIRECT, MEDIA],
                                                      'CURRENCY': BYN,
                                                      'DISCOUNT_POLICY_TYPE': BY_DISCOUNT_POLICY,
                                                      # Для связывания используем первый созданный в этой валюте договор
                                                      'LINK_CONTRACT_ID': lambda con, curr: con[curr][0]},
                                  'acts': [(DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 5, 0, 0, 0)),
                                           (DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 10, 0, 0, 0)),
                                           (DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 15, 0, 0, 0)),
                                           (DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 20, 0, 0, 0)),
                                           (DIRECT_PRODUCT, 1400, datetime.datetime(2016, 9, 25, 0, 0, 0))],
                                  },
                 }],
     'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2016, 10, 10, 0, 0, 0)),
     'target_currency': RUB,
     'expected_discount_pct': 12},

    # # RUB, 69.49%
    #     {'budget': [{'RUB_contract': {'contract_type': 'opt_agency_post',
    #                                   'contract_params': {'DT': '2016-08-11T00:00:00',
    #                                                       'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
    #                                                       'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
    #                                                       'DEAL_PASSPORT': '2016-08-11T00:00:00',
    #                                                       'SERVICES': [DIRECT, MEDIA],
    #                                                       'CURRENCY': RUB,
    #                                                       'DISCOUNT_POLICY_TYPE': BY_DISCOUNT_POLICY},
    #                                  'acts':   [(DIRECT_PRODUCT, 5000, datetime.datetime(2016, 9, 5, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 500, datetime.datetime(2016, 9, 10, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 500, datetime.datetime(2016, 9, 15, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 500, datetime.datetime(2016, 9, 25, 0, 0, 0)),
    #                                             # (DIRECT_PRODUCT, 695.279896, datetime.datetime(2016, 9, 30, 0, 0, 0))],
    #                                             (DIRECT_PRODUCT, 695, datetime.datetime(2016, 9, 30, 0, 0, 0))],
    #                                 },
    #                 }],
    #      'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2016, 10, 10, 0, 0, 0)),
    #      'target_currency': RUB,
    #      'expected_discount_pct': 12},

    # BYN, 69.49%
    {'budget': [{'RUB_contract': {'contract_type': 'opt_agency_post',
                                  'contract_params': {'DT': '2016-08-11T00:00:00',
                                                      'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                                                      'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                                                      'DEAL_PASSPORT': '2016-08-11T00:00:00',
                                                      'SERVICES': [DIRECT, MEDIA],
                                                      'CURRENCY': BYN,
                                                      'DISCOUNT_POLICY_TYPE': BY_DISCOUNT_POLICY},
                                  'acts': [(DIRECT_PRODUCT, 5000, datetime.datetime(2016, 9, 5, 0, 0, 0)),
                                           (DIRECT_PRODUCT, 500, datetime.datetime(2016, 9, 10, 0, 0, 0)),
                                           (DIRECT_PRODUCT, 500, datetime.datetime(2016, 9, 15, 0, 0, 0)),
                                           (DIRECT_PRODUCT, 500, datetime.datetime(2016, 9, 25, 0, 0, 0)),
                                           (DIRECT_PRODUCT, 695, datetime.datetime(2016, 9, 30, 0, 0, 0))],
                                  },
                 }],
     'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2016, 10, 10, 0, 0, 0)),
     'target_currency': BYN,
     'expected_discount_pct': 12},

    # BYN, 69.50%
    {'budget': [{'RUB_contract': {'contract_type': 'opt_agency_post',
                                  'contract_params': {'DT': '2016-08-11T00:00:00',
                                                      'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                                                      'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                                                      'DEAL_PASSPORT': '2016-08-11T00:00:00',
                                                      'SERVICES': [DIRECT, MEDIA],
                                                      'CURRENCY': BYN,
                                                      'DISCOUNT_POLICY_TYPE': BY_DISCOUNT_POLICY},
                                  'acts': [(DIRECT_PRODUCT, 5000, datetime.datetime(2016, 9, 5, 0, 0, 0)),
                                           (DIRECT_PRODUCT, 500, datetime.datetime(2016, 9, 10, 0, 0, 0)),
                                           (DIRECT_PRODUCT, 500, datetime.datetime(2016, 9, 15, 0, 0, 0)),
                                           (DIRECT_PRODUCT, 500, datetime.datetime(2016, 9, 25, 0, 0, 0)),
                                           (DIRECT_PRODUCT, 694, datetime.datetime(2016, 9, 30, 0, 0, 0))],
                                  },
                 }],
     'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2016, 10, 10, 0, 0, 0)),
     'target_currency': BYN,
     'expected_discount_pct': 0},

    # # 0: Регрессионная проверка RUB, 12%
    #     {'budget': {'RUB_contract': {'contract_params': {'dt': '2016-06-11T00:00:00',
    #                                                      'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
    #                                                      'is_signed': HALF_YEAR_BEFORE_NOW_ISO,
    #                                                      'DEAL_PASSPORT': '2016-06-11T00:00:00',
    #                                                      'SERVICES': [DIRECT, MEDIA],
    #                                                      'CURRENCY': RUB,
    #                                                      'DISCOUNT_POLICY_TYPE': BY_DISCOUNT_POLICY},
    #                                  'acts':   [(DIRECT_PRODUCT, 1400, datetime.datetime(2016, 7, 5, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 1400, datetime.datetime(2016, 7, 10, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 1400, datetime.datetime(2016, 7, 15, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 1400, datetime.datetime(2016, 7, 25, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 1400, datetime.datetime(2016, 7, 30, 0, 0, 0))],
    #                                 },
    #                 },
    #      'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2016, 8, 10, 0, 0, 0)),
    #      'target_currency': RUB,
    #      'expected_discount_pct': 12},
    #
    # # 1: Регрессионная проверка, BYN, 13%
    #     {'budget': {'BYN_contract': {'contract_params': {'dt': '2016-06-11T00:00:00',
    #                                                      'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
    #                                                      'is_signed': HALF_YEAR_BEFORE_NOW_ISO,
    #                                                      'DEAL_PASSPORT': '2016-06-11T00:00:00',
    #                                                      'SERVICES': [DIRECT, MEDIA],
    #                                                      'CURRENCY': BYN,
    #                                                      'DISCOUNT_POLICY_TYPE': BY_DISCOUNT_POLICY,
    #                                                      'LINK_CONTRACT_ID': None},
    #                                  'acts':   [(DIRECT_PRODUCT, 20000, datetime.datetime(2016, 7, 5, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 10000, datetime.datetime(2016, 7, 10, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 5000, datetime.datetime(2016, 7, 15, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 2500, datetime.datetime(2016, 7, 25, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 3750, datetime.datetime(2016, 7, 30, 0, 0, 0))],
    #                                 },
    #                 },
    #      'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2016, 9, 10, 0, 0, 0)),
    #      'target_currency': BYN,
    #      'expected_discount_pct': 13},
    #
    # # 2: BYN, 14%, оборот за 2 месяца из 3-х
    #     {'budget': {'BYN_contract': {'contract_params': {'dt': '2015-04-11T00:00:00',
    #                                                      'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
    #                                                      'is_signed': HALF_YEAR_BEFORE_NOW_ISO,
    #                                                      'DEAL_PASSPORT': '2015-04-11T00:00:00',
    #                                                      'SERVICES': [DIRECT, MEDIA],
    #                                                      'CURRENCY': BYN,
    #                                                      'DISCOUNT_POLICY_TYPE': BY_DISCOUNT_POLICY,
    #                                                      'LINK_CONTRACT_ID': None},
    #                                  'acts':   [ # Акт ранее 3-х последних месяцев
    #                                             (DIRECT_PRODUCT, 125000, datetime.datetime(2015, 12, 12, 0, 0, 0)),
    #
    #                                             (DIRECT_PRODUCT, 7000, datetime.datetime(2016, 5, 25, 0, 0, 0)),
    #
    #                                             (DIRECT_PRODUCT, 1200, datetime.datetime(2016, 7, 5, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 2000, datetime.datetime(2016, 7, 10, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 1500, datetime.datetime(2016, 7, 15, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 1200, datetime.datetime(2016, 7, 25, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 1100, datetime.datetime(2016, 7, 30, 0, 0, 0))],
    #                                 },
    #                 },
    #      'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2016, 8, 10, 0, 0, 0)),
    #      'target_currency': BYN,
    #      'expected_discount_pct': 14},
    #
    # # 3: BYN, 15%, оборот за 3 месяца
    #     {'budget': {'BYN_contract': {'contract_params': {'dt': '2015-06-11T00:00:00',
    #                                                      'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
    #                                                      'is_signed': HALF_YEAR_BEFORE_NOW_ISO,
    #                                                      'DEAL_PASSPORT': '2016-07-11T00:00:00',
    #                                                      'SERVICES': [DIRECT, MEDIA],
    #                                                      'CURRENCY': BYN,
    #                                                      'DISCOUNT_POLICY_TYPE': BY_DISCOUNT_POLICY,
    #                                                      'LINK_CONTRACT_ID': None},
    #                                  'acts':   [(DIRECT_PRODUCT, 100000, datetime.datetime(2016, 7, 25, 0, 0, 0)),
    #
    #                                             (DIRECT_PRODUCT, 100000, datetime.datetime(2016, 8, 25, 0, 0, 0)),
    #
    #                                             (DIRECT_PRODUCT, 40000, datetime.datetime(2016, 9, 5, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 25000, datetime.datetime(2016, 9, 10, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 10000, datetime.datetime(2016, 9, 15, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 2500, datetime.datetime(2016, 9, 25, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 1000, datetime.datetime(2016, 9, 30, 0, 0, 0))],
    #                                 },
    #                 },
    #      'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2016, 10, 10, 0, 0, 0)),
    #      'target_currency': BYN,
    #      'expected_discount_pct': 15},
    #
    # # 4: BYN, 16%, договор ранее 3 месяцев, нет оборота в 1-ом из месяцев проверки
    #     {'budget': {'BYN_contract': {'contract_params': {'dt': '2015-06-11T00:00:00',
    #                                                      'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
    #                                                      'is_signed': HALF_YEAR_BEFORE_NOW_ISO,
    #                                                      'DEAL_PASSPORT': '2016-06-11T00:00:00',
    #                                                      'SERVICES': [DIRECT, MEDIA],
    #                                                      'CURRENCY': BYN,
    #                                                      'DISCOUNT_POLICY_TYPE': BY_DISCOUNT_POLICY,
    #                                                      'LINK_CONTRACT_ID': None},
    #                                  'acts':   [(DIRECT_PRODUCT, 400000, datetime.datetime(2016, 7, 25, 0, 0, 0)),
    #
    #                                             (DIRECT_PRODUCT, 50000, datetime.datetime(2016, 9, 5, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 35000, datetime.datetime(2016, 9, 10, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 20000, datetime.datetime(2016, 9, 15, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 5000, datetime.datetime(2016, 9, 25, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 12345, datetime.datetime(2016, 9, 30, 0, 0, 0))],
    #                                 },
    #                 },
    #      'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2016, 10, 10, 0, 0, 0)),
    #      'target_currency': BYN,
    #      'expected_discount_pct': 16},
    #
    # # 5: BYN, 17%, договор ранее 3 месяцев, оборот только в последнем
    #     {'budget': {'BYN_contract': {'contract_params': {'dt': '2015-06-11T00:00:00',
    #                                                      'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
    #                                                      'is_signed': HALF_YEAR_BEFORE_NOW_ISO,
    #                                                      'DEAL_PASSPORT': '2016-06-11T00:00:00',
    #                                                      'SERVICES': [DIRECT, MEDIA],
    #                                                      'CURRENCY': BYN,
    #                                                      'DISCOUNT_POLICY_TYPE': BY_DISCOUNT_POLICY,
    #                                                      'LINK_CONTRACT_ID': None},
    #                                  'acts':   [(DIRECT_PRODUCT, 500002, datetime.datetime(2016, 9, 5, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 500001, datetime.datetime(2016, 9, 10, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 500003, datetime.datetime(2016, 9, 15, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 500004, datetime.datetime(2016, 9, 25, 0, 0, 0)),
    #                                             (DIRECT_PRODUCT, 12345, datetime.datetime(2016, 9, 30, 0, 0, 0))],
    #                                 },
    #                 },
    #      'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2016, 10, 10, 0, 0, 0)),
    #      'target_currency': BYN,
    #      'expected_discount_pct': 17},
    #

])
def test_2016_SNG_DIRECT_MEDIA_agency_discount(scenario):
    # Создаём агентство, клиента и плательщика
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    invoice_owner = agency_id
    person_id = steps.PersonSteps.create(invoice_owner, 'yt', {})

    # Словарь для хранения договоров по валюте.
    # Target-счёт будет выставляться по ПЕРВОМУ из списка для валюты указанной в scenario['target_currency']
    contracts = {RUB: [], BYN: []}

    # Формируем бюджет по договорам и без них, обходя структуру budget
    for item in scenario['budget']:
        for contract_descr, budget_part in item.items():

            # Параметры бюджета "по-умолчанию". Будут переопределены в случае, если описаны параметры договора
            contract_id = None
            contract_currency = scenario['target_currency']

            # Если переданы параметры договора, создаём его и запоминаем его валюту
            if budget_part['contract_params']:
                budget_part['contract_params']
                params = {'CLIENT_ID': invoice_owner, 'PERSON_ID': person_id}
                params.update(budget_part['contract_params'])
                if 'LINK_CONTRACT_ID' in budget_part['contract_params']:
                    params.update(
                        {'LINK_CONTRACT_ID': budget_part['contract_params']['LINK_CONTRACT_ID'](contracts, RUB)})
                contract_id, _ = steps.ContractSteps.create_contract(budget_part['contract_type'], params)
                contract_currency = budget_part['contract_params']['CURRENCY']

            # Складываем номер договора в справочник договоров по валютам. Если договор не создавался - кладём None
            contracts[contract_currency].append(contract_id)

            # Если описана структура актов - создаём их.
            data = []
            if 'acts' in budget_part:
                for act in budget_part['acts']:
                    data.append({'acts': budget_part['acts'], 'agency_id': invoice_owner, 'person_id': person_id,
                                 'contract_id': contract_id, 'paysys_id': PAYSYS_MAP[contract_currency], 'credit': 0})
                    # prepare_budget(acts=budget_part['acts'], agency_id=invoice_owner, person_id=person_id,
                    #                contract_id=contract_id, paysys_id=PAYSYS_MAP[contract_currency], credit=0)

            from concurrent import futures
            executor = futures.ThreadPoolExecutor(max_workers=5)
            results = executor.map(prepare_budget, data)

    # Определение основных параметов целевого-счёта.
    # contract_id и paysys_id зависят от scenario['target_currency']
    # Из списка договоров по определённой валюте будет использован ПЕРВЫЙ
    product, qty, request_dt = scenario['target_invoice']
    service_id = product.service_id
    contract_id = contracts[scenario['target_currency']][0]
    paysys_id = PAYSYS_MAP[scenario['target_currency']]

    # ---------- GetClientDiscountsAll -----------
    steps.CommonSteps.log(api.medium().server.Balance.GetClientDiscountsAll)({'ClientID': agency_id,
                                                                              'DT': request_dt})
    # TODO: check

    # ---------- Target ----------
    service_order_id = steps.OrderSteps.next_id(service_id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=service_id, product_id=product.id,
                                       params={'AgencyID': agency_id})
    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': request_dt}]
    request_id = steps.RequestSteps.create(invoice_owner, orders_list,
                                           additional_params={'InvoiceDesireDT': request_dt})
    invoice_id, external_id, total_sum = steps.InvoiceSteps.create(request_id, person_id, paysys_id,
                                                                   credit=scenario[
                                                                       'is_credit'] if 'is_credit' in scenario else 0,
                                                                   contract_id=contract_id, overdraft=0,
                                                                   endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    # Проверяем скидку в заявке
    consume = db.get_consumes_by_invoice(invoice_id)[0]
    utils.check_that(consume['static_discount_pct'], equal_to(scenario['expected_discount_pct']))
