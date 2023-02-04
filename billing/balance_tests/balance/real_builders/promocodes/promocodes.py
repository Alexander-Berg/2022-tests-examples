# -*- coding: utf-8 -*-

from balance import balance_steps as steps
import datetime
from jsonrpc import dispatcher
import btestlib.utils as utils
from btestlib.constants import PersonTypes, Services, Paysyses, Products, ContractPaymentType, \
    Currencies, ContractCreditType, ContractCommissionType, Firms


#
# @staticmethod
# def create_new(calc_class_name=None, end_dt=None, promocodes=None, start_dt=None, calc_params=None,
#                minimal_amounts=None, firm_id=None, is_global_unique=True, valid_until_paid=0,
#                service_ids=None, new_clients_only=1, need_unique_urls=0, reservation_days=0,
#                skip_reservation_check=False):
#     promocodes = promocodes or []
#     minimal_amounts = minimal_amounts or {}
#     params = {'CalcClassName': calc_class_name,
#               'EndDt': end_dt,
#               'Promocodes': [{'code': promocode} for promocode in promocodes],
#               'StartDt': start_dt,
#               'CalcParams': calc_params,
#               'MinimalAmounts': minimal_amounts,
#               'FirmId': firm_id,
#               'IsGlobalUnique': is_global_unique,
#               'ValidUntilPaid': valid_until_paid,
#               'ServiceIds': service_ids,
#               'NewClientsOnly': new_clients_only,
#               'NeedUniqueUrls': need_unique_urls,
#               'ReservationDays': reservation_days,
#               'SkipReservationCheck': skip_reservation_check,
#               }
#     if service_ids is None:
#         del params['ServiceIds']
#     api.medium().ImportPromoCodes([params])
#     response = []
#     for promocode in promocodes:
#         promocode_id = db.get_promocode_by_code(promocode)[0]['id']
#         response.append({'id': promocode_id, 'code': promocode})
#     return response


# промокод с фиксированной скидкой, количество
@dispatcher.add_method
def test_fix_discount_amount_promo():
    promocode = 'TEST' + utils.generate_alfanumeric_string(12).upper()
    steps.PromocodeSteps.create_new(start_dt=datetime.datetime(2020,12,31), end_dt=datetime.datetime(2025,12,31),
                                    promocodes=[promocode], service_ids=[Services.DIRECT.id], firm_id=Firms.YANDEX_1.id,
                                    calc_params={"adjust_quantity": True, "apply_on_create": False, "discount_pct": 10},
                                    calc_class_name="FixedDiscountPromoCodeGroup", valid_until_paid=False,
                                    need_unique_urls=True, is_global_unique=True)
    return promocode


# промокод с фиксированной скидкой, сумма
@dispatcher.add_method
def test_fix_discount_sum_promo():
    promocode = 'TEST' + utils.generate_alfanumeric_string(12).upper()
    steps.PromocodeSteps.create_new(start_dt=datetime.datetime(2020,12,31), end_dt=datetime.datetime(2025,12,31),
                                    promocodes=[promocode], service_ids=[Services.DIRECT.id], firm_id=Firms.YANDEX_1.id,
                                    calc_params={"adjust_quantity": False, "apply_on_create": True, "discount_pct": 10},
                                    calc_class_name="FixedDiscountPromoCodeGroup", valid_until_paid=False,
                                    need_unique_urls=True, is_global_unique=True)
    return promocode


# промокод с бонусом в количестве, увеличение количества в строках счета
@dispatcher.add_method
def test_qty_bonus_increase_qty_invoice_promo():
    promocode = 'TEST' + utils.generate_alfanumeric_string(12).upper()
    steps.PromocodeSteps.create_new(start_dt=datetime.datetime(2020,12,31), end_dt=datetime.datetime(2025,12,31),
                                    promocodes=[promocode], service_ids=[Services.DIRECT.id], firm_id=Firms.YANDEX_1.id,
                                    calc_params={"adjust_quantity": True, "apply_on_create": True,
                                                 "product_bonuses": {str(Products.DIRECT_FISH.id): 5}},
                                    calc_class_name="FixedQtyBonusPromoCodeGroup", valid_until_paid=False,
                                    need_unique_urls=True, is_global_unique=True)
    return promocode


# промокод с бонусом в количестве, уменьшение суммы в строках счета
@dispatcher.add_method
def test_qty_bonus_decrease_sum_invoice_promo():
    promocode = 'TEST' + utils.generate_alfanumeric_string(12).upper()
    steps.PromocodeSteps.create_new(start_dt=datetime.datetime(2020,12,31), end_dt=datetime.datetime(2025,12,31),
                                    promocodes=[promocode], service_ids=[Services.DIRECT.id], firm_id=Firms.YANDEX_1.id,
                                    calc_params={"adjust_quantity": False, "apply_on_create": True,
                                                 "product_bonuses": {str(Products.DIRECT_FISH.id): 5}},
                                    calc_class_name="FixedQtyBonusPromoCodeGroup", valid_until_paid=False,
                                    need_unique_urls=True, is_global_unique=True)
    return promocode


# промокод с бонусом в количестве, увеличение количества в заявках
@dispatcher.add_method
def test_qty_bonus_increase_qty_consume_promo():
    promocode = 'TEST' + utils.generate_alfanumeric_string(12).upper()
    steps.PromocodeSteps.create_new(start_dt=datetime.datetime(2020,12,31), end_dt=datetime.datetime(2025,12,31),
                                    promocodes=[promocode], service_ids=[Services.DIRECT.id], firm_id=Firms.YANDEX_1.id,
                                    calc_params={"adjust_quantity": True, "apply_on_create": False,
                                                 "product_bonuses": {str(Products.DIRECT_FISH.id): 5}},
                                    calc_class_name="FixedQtyBonusPromoCodeGroup", valid_until_paid=False,
                                    need_unique_urls=True, is_global_unique=True)
    return promocode


# промокод с бонусом в деньгах, увеличение количества в строках счета
@dispatcher.add_method
def test_money_bonus_increase_qty_invoice_promo():
    promocode = 'TEST' + utils.generate_alfanumeric_string(12).upper()
    steps.PromocodeSteps.create_new(start_dt=datetime.datetime(2020,12,31), end_dt=datetime.datetime(2025,12,31),
                                    promocodes=[promocode], service_ids=[Services.DIRECT.id], firm_id=Firms.YANDEX_1.id,
                                    calc_params={"adjust_quantity": True, "apply_on_create": True,
                                                 "currency_bonuses": {Currencies.RUB.iso_code: 400}},
                                    calc_class_name="FixedSumBonusPromoCodeGroup", valid_until_paid=False,
                                    need_unique_urls=True, is_global_unique=True)
    return promocode


# промокод с бонусом в деньгах, уменьшение суммы в строках счета
@dispatcher.add_method
def test_money_bonus_decrease_sum_invoice_promo():
    promocode = 'TEST' + utils.generate_alfanumeric_string(12).upper()
    steps.PromocodeSteps.create_new(start_dt=datetime.datetime(2020,12,31), end_dt=datetime.datetime(2025,12,31),
                                    promocodes=[promocode], service_ids=[Services.DIRECT.id], firm_id=Firms.YANDEX_1.id,
                                    calc_params={"adjust_quantity": False, "apply_on_create": True,
                                                 "currency_bonuses": {Currencies.RUB.iso_code: 400}},
                                    calc_class_name="FixedSumBonusPromoCodeGroup", valid_until_paid=False,
                                    need_unique_urls=True, is_global_unique=True)
    return promocode


# промокод с бонусом в деньгах, увеличение количества в заявках
@dispatcher.add_method
def test_money_bonus_increase_qty_consume_promo():
    promocode = 'TEST' + utils.generate_alfanumeric_string(12).upper()
    steps.PromocodeSteps.create_new(start_dt=datetime.datetime(2020,12,31), end_dt=datetime.datetime(2025,12,31),
                                    promocodes=[promocode], service_ids=[Services.DIRECT.id], firm_id=Firms.YANDEX_1.id,
                                    calc_params={"adjust_quantity": True, "apply_on_create": False,
                                                 "currency_bonuses": {Currencies.RUB.iso_code: 400}},
                                    calc_class_name="FixedSumBonusPromoCodeGroup", valid_until_paid=False,
                                    need_unique_urls=True, is_global_unique=True)
    return promocode


# просроченный промокод
@dispatcher.add_method
def test_overdue_promo():
    promocode = 'TEST' + utils.generate_alfanumeric_string(12).upper()
    steps.PromocodeSteps.create_new(start_dt=datetime.datetime(2020,12,31), end_dt=datetime.datetime(2021,01,01),
                                    promocodes=[promocode], service_ids=[Services.DIRECT.id], firm_id=Firms.YANDEX_1.id,
                                    calc_params={"adjust_quantity": True, "apply_on_create": False, "discount_pct": 10},
                                    calc_class_name="FixedDiscountPromoCodeGroup", valid_until_paid=False,
                                    need_unique_urls=True, is_global_unique=True)
    return promocode

@dispatcher.add_method


# промокод на кинопоиск
def test_kinopoisk_promo():
    promocode = 'TEST' + utils.generate_alfanumeric_string(12).upper()
    steps.PromocodeSteps.create_new(start_dt=datetime.datetime(2020,12,31), end_dt=datetime.datetime(2025,12,31),
                                    promocodes=[promocode], service_ids=[Services.DIRECT.id], firm_id=Firms.KINOPOISK_9.id,
                                    calc_params={"adjust_quantity": True, "apply_on_create": False, "discount_pct": 10},
                                    calc_class_name="FixedDiscountPromoCodeGroup", valid_until_paid=False,
                                    need_unique_urls=True, is_global_unique=True)
    return promocode
