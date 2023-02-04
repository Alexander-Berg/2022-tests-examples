# coding: utf-8
from btestlib import secrets

__author__ = 'atkaya'

from collections import namedtuple
from decimal import Decimal
from hamcrest import anything

from enum import Enum

import btestlib.utils as utils
from btestlib.constants import Services, Firms, Currencies, PaymentType, PaysysType, Regions
from simpleapi.data.defaults import user_ip
from simpleapi.data.uids_pool import User, anonymous
from balance.balance_objects import TrustPaymentData

DEFAULT_PRICE = Decimal('5000.01')
DEFAULT_PRICE_FOR_TESTS_WO_TRUST = Decimal('2099.2')

DEFAULT_USER = User(4003098409, 'testsimpleapi', secrets.get_secret(*secrets.UsersPwd.CLIENTUID_PWD))
USER_FAKE_TAXI = User(4003982351, 'testsimpleapitaxi', secrets.get_secret(*secrets.UsersPwd.CLIENTUID_PWD))
USER_NEW_API = User(496242349, "trustfortestingnewapi", secrets.get_secret(*secrets.UsersPwd.CLIENTUID_PWD))
USER_ANONYMOUS = anonymous

DEFAULT_USER_IP = user_ip
DEFAULT_REGION_ID = Regions.RU.id
DEFAULT_CURRENCY = Regions.RU.currency
REFUND_REASON = 'test1'

COMMISSION_PCT = Decimal('0.01')

DEFAULT_FEE = Decimal('123.09')
# DEFAULT_COMMISSION_CATEGORY - это процент * 100
DEFAULT_COMMISSION_CATEGORY = Decimal('1')  # =0.01%
DEFAULT_PROMOCODE_AMOUNT = 42  # a-vasin: только целые числа

DEFAULT_OFFSET = 4

UFS_PAYMENT_FEE = Decimal('60')
UFS_REFUND_FEE = Decimal('30')

PAYMENT_TYPE_TO_PAYSYS_FOR_FAKE_PAYMENTS = {
    PaymentType.CARD: PaysysType.MONEY,
    PaymentType.COMPENSATION: PaysysType.YANDEX,
    PaymentType.COMPENSATION_DISCOUNT: PaysysType.YANDEX,
    PaymentType.NEW_PROMOCODE: PaysysType.YANDEX
}

STATIC_EMULATOR_CARD = {
    'card_number': '5100004992728634',
    'cvn': '874',
    'expiration_month': '04',
    'expiration_year': '2022',
    'cardholder': 'TEST TEST',
    'descr': 'emulator_card',
    'type': 'MasterCard'
}

SPASIBO_EMULATOR_CARD = {
    'cardholder': 'TEST TEST',
    'cvn': '126',
    'expiration_month': '05',
    'expiration_year': '2020',
    'descr': 'emulator_card',
    'type': 'MasterCard',
    'card_number': '5469380041179762'
}

_ThirdPartyData = namedtuple('_ThirdPartyData',
                             'service, firm, currency, contract_currency, commission_pct, payment_type, paysys_type')


class ThirdPartyData(Enum):
    TAXI_DONATE = _ThirdPartyData(service=Services.TAXI_DONATE, firm=Firms.TAXI_13, currency=Currencies.RUB,
                                  contract_currency=Currencies.RUB, commission_pct=None,
                                  payment_type=PaymentType.SUBSIDY, paysys_type=PaysysType.TAXI)

    TAXI = _ThirdPartyData(service=Services.TAXI, firm=Firms.TAXI_13, currency=Currencies.RUB,
                           contract_currency=Currencies.RUB, commission_pct=Decimal('0.01'),
                           payment_type=PaymentType.CARD, paysys_type=PaysysType.MONEY)

    TAXI_BV = _ThirdPartyData(service=Services.TAXI, firm=Firms.TAXI_BV_22, currency=Currencies.KZT,
                              contract_currency=Currencies.USD, commission_pct=Decimal('0.01'),
                              payment_type=PaymentType.CARD, paysys_type=PaysysType.WALLET)

    TAXI_KZ = _ThirdPartyData(service=Services.TAXI, firm=Firms.TAXI_KAZ_24, currency=Currencies.KZT,
                              contract_currency=Currencies.KZT, commission_pct=Decimal('0.01'),
                              payment_type=PaymentType.CARD, paysys_type=PaysysType.ALFA)

    TAXI_ARMENIA = _ThirdPartyData(service=Services.TAXI, firm=Firms.TAXI_AM_26, currency=Currencies.AMD,
                                   contract_currency=Currencies.AMD, commission_pct=Decimal('0.01'),
                                   payment_type=PaymentType.CARD, paysys_type=PaysysType.ALFA)

    TAXI_UBER_BY = _ThirdPartyData(service=Services.TAXI, firm=Firms.UBER_115, currency=Currencies.BYN,
                                   contract_currency=Currencies.USD, commission_pct=Decimal('0.01'),
                                   payment_type=PaymentType.CARD, paysys_type=PaysysType.WALLET)

    TAXI_UBER_AZ = _ThirdPartyData(service=Services.TAXI, firm=Firms.UBER_115, currency=Currencies.AZN,
                                   contract_currency=Currencies.USD, commission_pct=Decimal('0.01'),
                                   payment_type=PaymentType.CARD, paysys_type=PaysysType.WALLET)

    MUSIC = _ThirdPartyData(service=Services.MUSIC, firm=Firms.MEDIASERVICES_121, currency=Currencies.RUB,
                            contract_currency=Currencies.RUB, commission_pct=None,
                            payment_type=PaymentType.CARD, paysys_type=PaysysType.MONEY)

    ZAXI = _ThirdPartyData(service=Services.ZAXI, firm=Firms.TAXI_13, currency=Currencies.RUB,
                           contract_currency=Currencies.RUB, commission_pct=None,
                           payment_type=PaymentType.CARD, paysys_type=PaysysType.MONEY)

    BLUE_MARKET_SUBSIDY = _ThirdPartyData(service=Services.BLUE_MARKET_SUBSIDY, firm=Firms.MARKET_111, currency=Currencies.RUB,
                                          contract_currency=Currencies.RUB, commission_pct=Decimal(0),
                                          payment_type=PaymentType.CARD, paysys_type=PaysysType.MONEY)

    def __init__(self, service, firm, currency, contract_currency, commission_pct, payment_type, paysys_type):
        self.service = service
        self.firm = firm
        self.currency = currency
        self.contract_currency = contract_currency
        self.commission_pct = commission_pct
        self.payment_type = payment_type
        self.paysys_type = paysys_type


class TrustPaymentCases(object):
    # пока кейсы для компенсаций добавлены только для рублей (на стороне баланса нет необходимости проверять для разных валют,
    # на данный момент обработка всех компенсаций одинакова)
    TAXI_RU_124 = TrustPaymentData().new(
        name='Taxi (124) Russia',
        service=Services.TAXI,
        currency=Currencies.RUB,
        region_id=Regions.RU,
        commission_category=DEFAULT_COMMISSION_CATEGORY,
        dt_offset=DEFAULT_OFFSET,
        amount=DEFAULT_PRICE_FOR_TESTS_WO_TRUST,
        price=DEFAULT_PRICE_FOR_TESTS_WO_TRUST,
        payment_method='card-x4250'
    )
    TAXI_RU_124_COMPENSATION = TAXI_RU_124.new(
        payment_method=PaymentType.COMPENSATION
    )
    TAXI_RU_125 = TAXI_RU_124.new(
        name='Taxi (125) Russia',
        service=Services.UBER
    )
    TAXI_RU_125_COMPENSATION = TAXI_RU_125.new(
        payment_method=PaymentType.COMPENSATION
    )
    TAXI_RU_605 = TAXI_RU_124.new(
        name='Taxi (605) Russia',
        service=Services.UBER_ROAMING
    )
    TAXI_RU_605_COMPENSATION = TAXI_RU_605.new(
        payment_method=PaymentType.COMPENSATION
    )
    TAXI_USD_124 = TAXI_RU_124.new(
        name='Taxi (124) USD',
        currency=Currencies.USD,
        region_id=Regions.GEO
    )
    TAXI_USD_125 = TAXI_USD_124.new(
        name='Taxi (125) USD',
        service=Services.UBER
    )
    TAXI_USD_605 = TAXI_USD_124.new(
        name='Taxi (605) USD',
        service=Services.UBER_ROAMING
    )
    TAXI_EUR_124 = TAXI_RU_124.new(
        name='Taxi (124) EUR',
        currency=Currencies.EUR,
        region_id=Regions.LAT
    )
    TAXI_KZT_124 = TAXI_RU_124.new(
        name='Taxi (124) KZT',
        currency=Currencies.KZT,
        region_id=Regions.KZ
    )
    TAXI_KZT_125 = TAXI_KZT_124.new(
        name='Taxi (125) KZT',
        service=Services.UBER
    )
    TAXI_KZT_605 = TAXI_KZT_124.new(
        name='Taxi (605) KZT',
        service=Services.UBER_ROAMING
    )
    TAXI_AMD_124 = TAXI_RU_124.new(
        name='Taxi (124) AMD',
        currency=Currencies.AMD,
        region_id=Regions.ARM
    )
    TAXI_BYN_124 = TAXI_RU_124.new(
        name='Taxi (124) BYN',
        currency=Currencies.BYN,
        region_id=Regions.BY
    )
    TAXI_BYN_125 = TAXI_BYN_124.new(
        name='Taxi (125) BYN',
        service=Services.UBER
    )
    TAXI_BYN_605 = TAXI_BYN_124.new(
        name='Taxi (605) BYN',
        service=Services.UBER_ROAMING
    )
    TAXI_AZN_124 = TAXI_RU_124.new(
        name='Taxi (124) AZN',
        service=Services.TAXI,
        currency=Currencies.AZN,
        region_id=Regions.AZ
    )
    TAXI_AZN_125 = TAXI_RU_124.new(
        name='Taxi (125) AZN',
        service=Services.UBER,
        currency=Currencies.AZN,
        region_id=Regions.AZ
    )
    TAXI_AZN_605 = TAXI_AZN_125.new(
        name='Taxi (605) AZN',
        service=Services.UBER_ROAMING,
    )
    TAXI_ILS_124 = TAXI_RU_124.new(
        name='Taxi (124) ILS',
        currency=Currencies.ILS,
        region_id=Regions.ISR
    )
    TAXI_GHS_124 = TAXI_RU_124.new(
        name='Taxi (124) GHS',
        currency=Currencies.GHS,
        region_id=Regions.GHA
    )
    TAXI_BOB_124 = TAXI_RU_124.new(
        name='Taxi (124) BOB',
        currency=Currencies.BOB,
        region_id=Regions.BOL
    )
    TAXI_RON_124 = TAXI_RU_124.new(
        name='Taxi (124) RON',
        currency=Currencies.RON,
        region_id=Regions.RO
    )
    TAXI_RON_124_COMPENSATION = TAXI_RON_124.new(
        name='Taxi (124) RON Compensation',
        payment_method=PaymentType.COMPENSATION,
    )
    TAXI_GHS_125 = TAXI_RU_125.new(
        name='Taxi (125) GHS',
        currency=Currencies.GHS,
        region_id=Regions.GHA
    )
    TAXI_GHS_605 = TAXI_RU_605.new(
        name='Taxi (605) GHS',
        currency=Currencies.GHS,
        region_id=Regions.GHA
    )
    TAXI_ZAR_124 = TAXI_RU_124.new(
        name='Taxi (124) ZAR',
        currency=Currencies.ZAR,
        region_id=Regions.ZA,
    )
    TAXI_NOR_124 = TAXI_RU_124.new(
        name='Taxi (124) NOR',
        currency=Currencies.NOK,
        region_id=Regions.NOR,
    )
    TAXI_SWE_124 = TAXI_RU_124.new(
        name='Taxi (124) SWE',
        currency=Currencies.SEK,
        region_id=Regions.SWE,
    )

DEFAULT_PAYMENT_JSON_TEMPLATE = [{"fiscal_nds": anything(),
                                    "fiscal_inn": anything(),
                                    "fiscal_title": anything(),
                                    "price": anything(),
                                    "id": anything(),
                                    "amount": anything(),
                                    "source_id": anything(),
                                    "cancel_dt": anything(),
                                    "order": {
                                        "region_id": anything(),
                                        "contract_id": anything(),
                                        "update_dt": anything(),
                                        "text": anything(),
                                        "price": anything(),
                                        "service_order_id_number": anything(),
                                        "start_dt_utc": anything(),
                                        "clid": anything(),
                                        "service_order_id": anything(),
                                        "service_product_id": anything(),
                                        "start_dt_offset": anything(),
                                        "service_id": anything(),
                                        "dt": anything(),
                                        "passport_id": anything(),
                                        "commission_category": anything()},
                                    "quantity": anything()}]

def create_default_orders(service_order_id_list=None, price_list=None, fiscal_nds_list=None, qty_list=None):
    if service_order_id_list is None:
        service_order_id_list = [None]

    if price_list is None:
        price_list = [DEFAULT_PRICE] * len(service_order_id_list)

    if fiscal_nds_list is None:
        fiscal_nds_list = [None] * len(service_order_id_list)

    if qty_list is None:
        qty_list = [None] * len(service_order_id_list)

    return [utils.remove_empty({'service_order_id': service_order_id,
                                'price': price,
                                'action': 'clear',
                                'fiscal_nds': fiscal_nds.name if fiscal_nds else None,
                                'fiscal_title': 'test_fiscal_title' if fiscal_nds else None,
                                'qty': qty})
            for service_order_id, price, fiscal_nds, qty in
            zip(service_order_id_list, price_list, fiscal_nds_list, qty_list)
            ]


def create_default_refund_orders(service_order_id_list=None, delta_amount_list=None):
    if service_order_id_list is None:
        service_order_id_list = [None]

    if delta_amount_list is None:
        delta_amount_list = [DEFAULT_PRICE] * len(service_order_id_list)

    return [
        utils.remove_empty({'service_order_id': service_order_id,
                            'delta_amount': delta_amount})
        for service_order_id, delta_amount in zip(service_order_id_list, delta_amount_list)
    ]


def create_update_orders(service_order_id_list, amount_list=None, action_list=None):
    if amount_list is None:
        amount_list = [None] * len(service_order_id_list)

    if action_list is None:
        action_list = [None] * len(service_order_id_list)

    return [utils.remove_empty({'service_order_id': service_order_id,
                                'amount': amount,
                                'action': action})
            for service_order_id, amount, action in zip(service_order_id_list, amount_list, action_list)
            ]
