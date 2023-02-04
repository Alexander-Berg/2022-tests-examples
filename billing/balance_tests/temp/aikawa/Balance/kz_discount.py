# -*- coding: utf-8 -*-
__author__ = 'torvald'

import copy
import datetime
from decimal import Decimal as D

import pytest
from hamcrest import equal_to

import btestlib.reporter as reporter
from balance import balance_db as db
from balance import balance_steps as steps
from balance.balance_objects import Product
from balance.features import Features
from btestlib import utils
from btestlib.constants import Firms, Services
from btestlib.matchers import has_entries_casted

CLIENT_NUM_LIMIT = 5
BASE_DT = datetime.datetime(2016, 2, 12)
DISC_DT = datetime.datetime(2016, 1, 5)
to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta
NOW = datetime.datetime.now()
TWO_YEARS_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=720))
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
DT = datetime.datetime.now()
HALF_YEAR_AFTER_NOW_ISO = utils.Date.date_to_iso_format(DT + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = utils.Date.date_to_iso_format(DT - datetime.timedelta(days=180))
YEAR_BEFORE_NOW_ISO = utils.Date.date_to_iso_format(DT - datetime.timedelta(days=365))
TWO_YEARS_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=720))

DEFAULT_QTY = 100
DIRECT_KZ_PRICE = D('105')
MULTICURRENCY_KZ_PRICE = D('1')

KZU_PAYSYS_ID = 1020
KZP_PAYSYS_ID = 1021
SUB_PAYSYS_ID = 2501020

ORANGE_ID = 1020474
ORANGE_PERSON_ID = 2822846
ORANGE_CONTRACT_ID = 237598
ORANGE_MARKET_CONTRACT_ID = 246234
GET_ORANGE_CONTRACT = lambda service: ORANGE_MARKET_CONTRACT_ID if service == MARKET else ORANGE_CONTRACT_ID

KZ_DISCOUNT_POLICY_ID = D('18')
FIXED_ORANGE_DISCOUNT = D('18')
FIXED_DISCOUNT_FROM_ORANGE_MARKET_CONTRACT = D('18')
FIXED_DISCOUNT_FROM_OTHER_AGENCY_MARKET_CONTRACT = D('16')

ZERO_DISCOUNT_VALUE = D('0')
GET_ACCUMULATE_DISCOUNT = lambda x, y: D('100') - (D('100') - x) * (D('100') - y) / D('100')

DIRECT = Services.DIRECT.id
MARKET = Services.MARKET.id
GEO = Services.GEO.id
BAYAN = Services.MEDIA_BANNERS.id
MEDIA = Services.MEDIA_70.id
MEDIASELLING = Services.BAYAN.id

DIRECT_PRODUCT = Product(DIRECT, 1475, 'Bucks', 'Money')  #
DIRECT_KZT_PRODUCT = Product(DIRECT, 503166, 'Money')  #
MARKET_PRODUCT = Product(MARKET, 2136, 'Bucks')  #
GEO_SNG_PRODUCT = Product(GEO, 502952, 'Days')  # 6500
GEO_PRODUCT = Product(GEO, 10000022, 'Days')  # 6500 \ 30 (type_rate)
MEDIA_PRODUCT_1 = Product(BAYAN, 2584, 'Shows')  #
MEDIA_PRODUCT_31 = Product(MEDIA, 100000000, 'Money')  # 503341 валютный, Price = 1
MEDIA_PRODUCT = Product(MEDIA, 502941, 'Shows')  # price = 150000
MEDIA_KZ_PRODUCT = Product(MEDIA, 503261, 'Shows')  # media_discount = 13, unit_id = 799 (1000shows) 105 KZT
MEDIA_KZ_AUCTION_PRODUCT = Product(MEDIA, 506964, 'Money')  # media_discount = 27, price = 1, unit_id = 852

CONTRACT_DEFAULT_PARAMS = {'DT': TWO_YEARS_BEFORE_NOW_ISO,
                           'FINISH_DT': TWO_YEARS_AFTER_NOW_ISO,
                           'IS_SIGNED': TWO_YEARS_BEFORE_NOW_ISO,
                           'DEAL_PASSPORT': TWO_YEARS_BEFORE_NOW_ISO,
                           'DISCOUNT_POLICY_TYPE': KZ_DISCOUNT_POLICY_ID,
                           'PERSONAL_ACCOUNT': 1,
                           'LIFT_CREDIT_ON_PAYMENT': 1,
                           'PERSONAL_ACCOUNT_FICTIVE': 1,
                           'CURRENCY': 398,
                           'BANK_DETAILS_ID': 320,
                           }


@pytest.mark.priority('mid')
@reporter.feature(Features.DISCOUNT)
@pytest.mark.tickets('BALANCE-')
@pytest.mark.docpath('https://wiki.yandex-team.ru/balance/docs/process/discount')
@pytest.mark.docs(u'--group', u'Автотесты: Казахстан, агентские, Директ, Маркет, Справочник')
@pytest.mark.parametrize('p',
                         [
                             # 0.Scale (799 999.99 (0%)), act in -1 month
                             utils.aDict({'budget': [(DIRECT_PRODUCT, 7619, datetime.datetime(2016, 12, 20))],
                                          'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2017, 1, 11)),
                                          'expected_discount_pct': D('0'),
                                          'expected_consume_sum': D('10500')}),

                             # 1.Scale (800 000 (10%)), act in -1 month
                             # 800_000.0 / 6_500.0 * 30.0 / 0.5 (50% скидки) = 7384.615384
                             pytest.mark.smoke(
                                 utils.aDict({'budget': [(DIRECT_PRODUCT, 7384.615384, datetime.datetime(2016, 12, 20))],
                                              'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2017, 1, 11)),
                                              'expected_discount_pct': D('10'),
                                              'expected_consume_sum': D('9450')})  # 10500 * 0.9 = 9450
                             ),

                             # 2.Scale (1 299 999.99 (10% + 20% за количество заказанного по GEO)), act in -2 month
                             # 1299999.99 / 6500 * 30 / 0.6 (40% скидки) = 9999.999923
                             utils.aDict({'budget': [(DIRECT_PRODUCT, 9999.999923, datetime.datetime(2016, 11, 20))],
                                          'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2017, 1, 11)),
                                          'expected_discount_pct': D('10'),
                                          'expected_accumulate_pct': D('28'),
                                          # Для поддержки суммы скидок в целевом счёте
                                          'expected_consume_sum': D('15600')}),

                             # 3.Scale (1 300 000 (11% + 30% за количество заказанного по GEO)), act in -3 month
                             # 1300000 / 350 * 1000 / 0.80 (20% скидка) = 4642857.142857
                             # TODO: https://st.yandex-team.ru/BALANCE-24409
                             utils.aDict({'budget': [(DIRECT_PRODUCT, 4642858, datetime.datetime(2016, 10, 20))],
                                          'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2017, 1, 11)),
                                          'expected_discount_pct': D('11'),
                                          'expected_accumulate_pct': D('37.7'),
                                          # Для поддержки суммы скидок в целевом счёте
                                          'expected_consume_sum': D('13498.33')}),

                             # 4.Scale (1 999 999.99 (11%)), act in -3 month
                             # 1999999.99 / 2400 * 1000 = 833334
                             utils.aDict({'budget': [(DIRECT_PRODUCT, 833333, datetime.datetime(2016, 10, 20))],
                                          'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2017, 1, 11)),
                                          'expected_discount_pct': D('11'),
                                          'expected_consume_sum': D('31.15')}),

                             # 5.Scale (2_000_000 (12%)), act in -3 month
                             #    1999999.99 / 2400 * 1000 = 833334
                             utils.aDict(
                                 {'budget': [(DIRECT_PRODUCT, 2000000, datetime.datetime(2016, 10, 20))],
                                  'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2017, 1, 11)),
                                  'expected_discount_pct': D('12'),
                                  'expected_consume_sum': D('30.8')}),

                             # 6.Scale (800 000 (10%)), act in -3 month (start of month - more than 3 month from target invoice dt
                             #    800_000.0 / 6_500.0 * 30.0 / 0.5 (50% скидки) = 7384.615384
                             utils.aDict({'budget': [(DIRECT_PRODUCT, 7384.615384, datetime.datetime(2016, 10, 1))],
                                          'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2017, 1, 10)),
                                          'expected_discount_pct': D('10'),
                                          'expected_consume_sum': D('9450')}),  # 10500 * 0.9 = 9450

                             # 7.Scale (13%)), several rows in budget
                             utils.aDict(
                                 {'budget': [(DIRECT_PRODUCT, 761.904666, datetime.datetime(2016, 12, 20)),  # _ 799 999
                                             (DIRECT_PRODUCT, 9999.999923, datetime.datetime(2016, 12, 20)),
                                             # 1 299 999.99
                                             (DIRECT_PRODUCT, 4642858, datetime.datetime(2016, 11, 20)),  # 1 300 000
                                             (DIRECT_PRODUCT, 2000000, datetime.datetime(2016, 10, 20)),
                                             # 2 000 000
                                             ],  # 5 399 998.99
                                  'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2017, 1, 11)),
                                  'expected_discount_pct': D('13'),
                                  'expected_consume_sum': D('9135')}),

                             # 8.Scale (1 999 999.99 (11%)), act in -3 month
                             # 1999999.99 / 2400 * 1000 = 833334
                             utils.aDict({'budget': [(DIRECT_PRODUCT, 833333, datetime.datetime(2016, 10, 20))],
                                          'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2017, 1, 11)),
                                          'expected_discount_pct': D('11'),
                                          'expected_consume_sum': D('213.6')}),
                             # 100 * 2400 \ 1000 * (1 - 0.11) = 213.6

                             # NEGATIVE ---------------------------------------------------------------------------------------------

                             # 9.Scale (800 000 (10%)), act before 3 previous months
                             # 800_000.0 / 6_500.0 * 30.0 / 0.5 (50% скидки) = 7384.615384
                             utils.aDict({'budget': [(DIRECT_PRODUCT, 7619, datetime.datetime(2016, 9, 30))],
                                          'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2017, 1, 11)),
                                          'expected_discount_pct': D('0'),
                                          'expected_consume_sum': D('10500')}),

                             # # For EstimateDiscount response testing
                             #
                             # # 9.Scale (800 000 (10%)), before discount re-calculation (10th of month)
                             # # 800_000.0 / 6_500.0 * 30.0 / 0.5 (50% скидки) = 7384.615384
                             # utils.aDict({'budget': [(GEO_PRODUCT, 7384.615384, datetime.datetime(2016, 12, 20))],
                             #              'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2017, 1, 1)),
                             #              'expected_discount_pct': D('0'),
                             #              'expected_consume_sum': D('10500')}
                             # ),
                             # # 9.Scale (800 000 (10%)), before discount re-calculation (10th of month)
                             # # 800_000.0 / 6_500.0 * 30.0 / 0.5 (50% скидки) = 7384.615384
                             # utils.aDict({'budget': [(GEO_PRODUCT, 7384.615384, datetime.datetime(2016, 12, 20))],
                             #              'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2017, 1, 9)),
                             #              'expected_discount_pct': D('0'),
                             #              'expected_consume_sum': D('10500')}
                             # ),
                             # # 9.Scale (800 000 (10%)), before discount re-calculation (10th of month)
                             # # 800_000.0 / 6_500.0 * 30.0 / 0.5 (50% скидки) = 7384.615384
                             # utils.aDict({'budget': [(GEO_PRODUCT, 7384.615384, datetime.datetime(2016, 9, 20))],
                             #              'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2017, 1, 9)),
                             #              'expected_discount_pct': D('10'),
                             #              'expected_consume_sum': D('9450')}
                             # ),
                             # # 9.Scale (800 000 (10%)), before discount re-calculation (10th of month)
                             # # 800_000.0 / 6_500.0 * 30.0 / 0.5 (50% скидки) = 7384.615384
                             # utils.aDict({'budget': [(GEO_PRODUCT, 7384.615384, datetime.datetime(2016, 12, 20))],
                             #              'target_invoice': (DIRECT_PRODUCT, 100, datetime.datetime(2017, 1, 10)),
                             #              'expected_discount_pct': D('10'),
                             #              'expected_consume_sum': D('9450')}
                             # ),
                         ]
                         )
def test_KZ_other_agency_discount(p):
    # Создём агентство, плательщика, договор и субклиента для целевого счёта
    client_id = steps.ClientSteps.create({'REGION_ID': None, 'IS_AGENCY': 0})
    agency_id = steps.ClientSteps.create({'REGION_ID': None, 'IS_AGENCY': 1})
    person_id = steps.PersonSteps.create(agency_id, 'kzu')

    contract_params = copy.deepcopy(CONTRACT_DEFAULT_PARAMS)
    contract_params.update({'CLIENT_ID': agency_id,
                            'PERSON_ID': person_id,
                            'SERVICES': [DIRECT],
                            'FIRM': Firms.KZ_25.id,
                            'DT': YEAR_BEFORE_NOW_ISO,
                            'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                            'IS_SIGNED': YEAR_BEFORE_NOW_ISO,
                            'PAYMENT_TYPE': 2
                            })
    contract_id, _ = steps.ContractSteps.create_contract_new('KZ_OPT_AGENCY', contract_params)

    # Создаём бюджет
    steps.DiscountSteps.prepare_budget_by_acts(agency_id, person_id, contract_id, SUB_PAYSYS_ID, p.budget)

    # -----------------------------------------------------------------------------------------------------------------

    # Серия проверок для целевого счёта
    product, qty, target_invoice_dt = p.target_invoice
    credit = 0  # p.get('is_credit', 0)

    # # Проверяем ответ метода EstimateDiscount
    result = steps.DiscountSteps.estimate_discount(
        {'ClientID': agency_id,
         'PaysysID': SUB_PAYSYS_ID,
         'ContractID': contract_id},
        [
            {'ProductID': product.id,
             'ClientID': client_id,
             'Qty': qty,
             'ID': 1,
             'BeginDT': target_invoice_dt,
             'RegionID': None,
             'discard_agency_discount': 0}
        ])
    # utils.check_that(D(result['AgencyDiscountPct']), equal_to(p.expected_discount_pct))

    # Метод ответ метода GetClientDiscountsAll
    result = steps.DiscountSteps.get_client_discounts_all({'ClientID': agency_id,
                                                           'DT': target_invoice_dt})

    # [{'Budget': '960000',
    #   'ContractExternalID': '209602/16',
    #   'Currency': 'KZT',
    #   'Discount': '10',
    #   'DiscountIDs': [7, 12, 15, 1, 13, 27],
    #   'DiscountName': 'KzOrange_fixed_agency_discount',
    #   'DiscountType': None,
    #   'NextBudget': '1300000',
    #   'NextDiscount': '11'}]

    # TODO: добавить проверку

    # Выставляем и оплачиваем целевой счёт
    campaigns_list = [
        {'service_id': product.service_id, 'product_id': product.id, 'qty': qty, 'begin_dt': target_invoice_dt}]
    invoice_id, _, _, _ = steps.InvoiceSteps.create_force_invoice(client_id=client_id,
                                                                  person_id=person_id,
                                                                  campaigns_list=campaigns_list,
                                                                  paysys_id=SUB_PAYSYS_ID,
                                                                  invoice_dt=target_invoice_dt,
                                                                  agency_id=agency_id,
                                                                  credit=credit,
                                                                  contract_id=contract_id,
                                                                  overdraft=0,
                                                                  )
    if not credit:
        steps.InvoiceSteps.pay(invoice_id)

    # Для Справочника и Справочника СНГ выдаётся дополнительная скидка от количества заказанных услуг
    # Она НЕ проявляет себя в проверка методов, но появляется при выставлении целевого счёта. Для поддержки этого:
    expected_discount_pct = p.get('expected_accumulate_pct', p.expected_discount_pct)

    # Проверка скидки в целевом счёте
    utils.check_that(db.get_consumes_by_invoice(invoice_id)[0],
                     has_entries_casted({'consume_sum': p.expected_consume_sum,
                                         'static_discount_pct': expected_discount_pct}),
                     step=u'Проверяем сумму и скидку в заявке')

# @pytest.mark.priority('mid')
# @reporter.feature(Features.DISCOUNT)
# @pytest.mark.tickets('BALANCE-')
# @pytest.mark.docpath('https://wiki.yandex-team.ru/balance/docs/process/discount')
# @pytest.mark.docs(u'--group', u'Автотесты: Казахстан, агентские, Маркет, другие агентства')
# @pytest.mark.parametrize('p',
#                          [
#                              pytest.mark.xfail(reason='https://st.yandex-team.ru/BALANCE-24681')(
#                                  utils.aDict({'budget': [],
#                                               'target_invoice': (MARKET_PRODUCT, 100, datetime.datetime(2017, 1, 10)),
#                                               'expected_discount_pct': FIXED_DISCOUNT_FROM_OTHER_AGENCY_MARKET_CONTRACT,
#                                               'expected_consume_sum': D('8820')})  # 10500 * (1 - 0.16) = 8820
#                              ),
#                          ]
#                          )
# def test_KZ_other_agency_discount_MARKET(p, data_cache):
#     # Создём агентство, плательщика, договор и субклиента для целевого счёта
#     with utils.CachedData(data_cache, ['client_id', 'agency_id', 'person_id', 'contract_id'],
#                           force_invalidate=True) as c:
#         if not c: raise utils.SkipContextManagerBodyException()
#
#         client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
#         agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
#         person_id = steps.PersonSteps.create(agency_id, 'yt_kzu')
#
#         contract_params = copy.deepcopy(CONTRACT_DEFAULT_PARAMS)
#         contract_params.update({'CLIENT_ID': agency_id,
#                                 'PERSON_ID': person_id,
#                                 'SERVICES': [MARKET],
#                                 'FIRM': Firms.MARKET_111.id,
#                                 'CONTRACT_DISCOUNT': FIXED_DISCOUNT_FROM_OTHER_AGENCY_MARKET_CONTRACT})
#         contract_id, _ = steps.ContractSteps.create_contract('opt_agency_post_kz', contract_params
#                                                              # {'CLIENT_ID': agency_id,
#                                                              #  'PERSON_ID': person_id,
#                                                              #  'DT': HALF_YEAR_BEFORE_NOW_ISO,
#                                                              #  'FINISH_DT': TWO_YEARS_AFTER_NOW_ISO,
#                                                              #  'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
#                                                              #  'DEAL_PASSPORT': HALF_YEAR_BEFORE_NOW_ISO,
#                                                              #  'SERVICES': [MARKET],
#                                                              #  'DISCOUNT_POLICY_TYPE': KZ_DISCOUNT_POLICY_ID,
#                                                              #  'CONTRACT_DISCOUNT': FIXED_DISCOUNT_FROM_OTHER_AGENCY_MARKET_CONTRACT,
#                                                              #  'PERSONAL_ACCOUNT': 1,
#                                                              #  'LIFT_CREDIT_ON_PAYMENT': 1,
#                                                              #  'PERSONAL_ACCOUNT_FICTIVE': 1,
#                                                              #  'CURRENCY': 398,
#                                                              #  'BANK_DETAILS_ID': 320,
#                                                              #  }
#                                                              )
#
#         # Создаём бюджет
#         steps.DiscountSteps.prepare_budget_by_acts(agency_id, person_id, contract_id, SUB_PAYSYS_ID, p.budget)
#     # -----------------------------------------------------------------------------------------------------------------
#
#     # Серия проверок для целевого счёта
#     product, qty, target_invoice_dt = p.target_invoice
#     credit = 1  # p.get('is_credit', 0)
#
#     # # Проверяем ответ метода EstimateDiscount
#     result = steps.DiscountSteps.estimate_discount(
#         {'ClientID': agency_id,
#          'PaysysID': SUB_PAYSYS_ID,
#          'ContractID': contract_id},
#         [
#             {'ProductID': product.id,
#              'ClientID': client_id,
#              'Qty': qty,
#              'ID': 1,
#              'BeginDT': target_invoice_dt,
#              'RegionID': None,
#              'discard_agency_discount': 0}
#         ])
#     # utils.check_that(D(result['AgencyDiscountPct']), equal_to(p.expected_discount_pct))
#
#     # Метод ответ метода GetClientDiscountsAll
#     result = steps.DiscountSteps.get_client_discounts_all({'ClientID': agency_id,
#                                                            'DT': target_invoice_dt})
#
#     # [{'Budget': '960000',
#     #   'ContractExternalID': '209602/16',
#     #   'Currency': 'KZT',
#     #   'Discount': '10',
#     #   'DiscountIDs': [7, 12, 15, 1, 13, 27],
#     #   'DiscountName': 'KzOrange_fixed_agency_discount',
#     #   'DiscountType': None,
#     #   'NextBudget': '1300000',
#     #   'NextDiscount': '11'}]
#
#     # TODO: добавить проверку
#
#     # Выставляем и оплачиваем целевой счёт
#     campaigns_list = [
#         {'service_id': product.service_id, 'product_id': product.id, 'qty': qty, 'begin_dt': target_invoice_dt}]
#     invoice_id, _, _, _ = steps.InvoiceSteps.create_force_invoice(client_id=client_id,
#                                                                   person_id=person_id,
#                                                                   campaigns_list=campaigns_list,
#                                                                   paysys_id=SUB_PAYSYS_ID,
#                                                                   invoice_dt=target_invoice_dt,
#                                                                   agency_id=agency_id,
#                                                                   credit=credit,
#                                                                   contract_id=contract_id,
#                                                                   overdraft=0,
#                                                                   )
#     if not credit:
#         steps.InvoiceSteps.pay(invoice_id)
#
#     # Для Справочника и Справочника СНГ выдаётся дополнительная скидка от количества заказанных услуг
#     # Она НЕ проявляет себя в проверка методов, но появляется при выставлении целевого счёта. Для поддержки этого:
#     expected_discount_pct = p.get('expected_accumulate_pct', p.expected_discount_pct)
#
#     # Проверка скидки в целевом счёте
#     utils.check_that(db.get_consumes_by_invoice(invoice_id)[0],
#                      has_entries_casted({'consume_sum': p.expected_consume_sum,
#                                          'static_discount_pct': expected_discount_pct}),
#                      step=u'Проверяем сумму и скидку в заявке')
#
#
# if __name__ == "__main__":
#     # test_simple_client()
#     pytest.main("-v test_2016_KZ_7_11_37_agency_discount.py -k 'orange'")
