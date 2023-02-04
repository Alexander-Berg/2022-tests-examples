# coding: utf-8
__author__ = 'a-vasin'

from collections import namedtuple
from decimal import Decimal

from enum import Enum

RETAIL_EXPENSES_DEVELOPER_INCOME_AMOUNT = 5000
DEVELOPER_EXPENSES_RETAIL_INCOME_AMOUNT = 4000


class DistributionSubtype(Enum):
    FIXED = 0
    REVSHARE = 1
    ADDAPTER = 2


# a-vasin: для некоторых значий page_id выбраны одни из множества возможных
# так как на данный момент кажется, что не играет роли какая именно страница выбрана
_DistributionType = namedtuple('_DistributionType',
                               'payment_type, page_id, place_id, result_page_id, default_amount, default_price, '
                               'units, partner_units, source_id, type_id, description, subtype, has_tail, '
                               'service_id, units_type_rate, contract_price_id')


class DistributionType(Enum):
    INSTALLS = _DistributionType(payment_type=1, page_id=10001, place_id=None, result_page_id=10001,
                                 default_amount=4000, default_price=Decimal('0.50'), units='shows',
                                 partner_units='shows', source_id=None, type_id=5, description=u'Дистрибуция.Установка',
                                 subtype=DistributionSubtype.FIXED, has_tail=False, service_id=None,
                                 units_type_rate=1, contract_price_id=None)

    SEARCHES = _DistributionType(payment_type=4, page_id=100005, place_id=None, result_page_id=100005,
                                 default_amount=5000, default_price=Decimal('2.50'), units='hits', partner_units='hits',
                                 source_id=None, type_id=5, description=u'Дистрибуция.Поиски',
                                 subtype=DistributionSubtype.FIXED,
                                 has_tail=True, service_id=None, units_type_rate=1000, contract_price_id=None)

    ACTIVATIONS = _DistributionType(payment_type=7, page_id=3010, place_id=None, result_page_id=3010,
                                    default_amount=7000, default_price=Decimal('4.50'), units='shows',
                                    partner_units='shows', source_id=None, type_id=5,
                                    description=u'Дистрибуция.Активации', subtype=DistributionSubtype.FIXED,
                                    has_tail=False, service_id=None, units_type_rate=1, contract_price_id=None)

    ADDAPPTER2_RETAIL = _DistributionType(
        payment_type=None, page_id=20001, place_id=None, result_page_id=20001, default_amount=8000,
        default_price=Decimal('4.50'), units='count', partner_units='shows', source_id=None, type_id=5,
        description=u'Установки Адаптер. Ритейл', subtype=DistributionSubtype.FIXED, has_tail=False, service_id=None,
        units_type_rate=1, contract_price_id=None)

    TAXI_NEW_PASSENGER = _DistributionType(
        payment_type=None, page_id=13001, place_id=None, result_page_id=13001, default_amount=2000,
        default_price=Decimal('9.99'), units='quantity', partner_units='shows', source_id=None, type_id=5,
        description=u'Виджет Такси. Первая поездка.', subtype=DistributionSubtype.FIXED, has_tail=False, service_id=None,
        units_type_rate=1, contract_price_id=None)

    # Revenue share
    DIRECT = _DistributionType(payment_type=3, page_id=542, place_id=63, result_page_id=10000,
                               default_amount=5000000, default_price=Decimal('5.50'), units='bucks',
                               partner_units='bucks', source_id=11, type_id=4,
                               description=u'Дистрибуция.Разделение доходов Директ',
                               subtype=DistributionSubtype.REVSHARE,
                               has_tail=True, service_id=7, units_type_rate=1000000, contract_price_id=1)

    MARKET_CPC = _DistributionType(payment_type=8, page_id=10003, place_id=10003, result_page_id=10003,
                                   default_amount=3000000, default_price=Decimal('7.50'), units='bucks',
                                   partner_units='bucks', source_id=13, type_id=4,
                                   description=u'Дистрибуция.Разделение доходов Маркет CPC',
                                   subtype=DistributionSubtype.REVSHARE,
                                   has_tail=True, service_id=11, units_type_rate=1000000, contract_price_id=3)

    MARKET_CPA = _DistributionType(payment_type=13, page_id=10004, place_id=10004, result_page_id=10004,
                                   default_amount=2000000, default_price=Decimal('12.50'), units='bucks',
                                   partner_units='bucks', source_id=14, type_id=4,
                                   description=u'Дистрибуция.Разделение доходов Маркет CPA',
                                   subtype=DistributionSubtype.REVSHARE,
                                   has_tail=True, service_id=11, units_type_rate=1000000, contract_price_id=4)

    TAXI_LUCKY_RIDE = _DistributionType(payment_type=18, page_id=13002, place_id=13002, result_page_id=13002,
                                        default_amount=6000000, default_price=Decimal('4.38'), units='bucks',
                                        partner_units='bucks', source_id=17, type_id=4,
                                        description=u'Дистрибуция. Успешная поездка Такси',
                                        subtype=DistributionSubtype.REVSHARE,
                                        has_tail=True, service_id=111, units_type_rate=1000000, contract_price_id=9)

    VIDEO_HOSTING = _DistributionType(payment_type=19, page_id=13003, place_id=13003, result_page_id=13003,
                                      default_amount=7000000, default_price=Decimal('3.42'), units='bucks',
                                      partner_units='bucks', source_id=18, type_id=4,
                                      description=u'Видеореклама на площадке',
                                      subtype=DistributionSubtype.REVSHARE,
                                      has_tail=True, service_id=None, units_type_rate=1000000, contract_price_id=10)

    PARTNER_DISCOVERY = _DistributionType(payment_type=20, page_id=10100, place_id=10100, result_page_id=10100,
                                          default_amount=Decimal('6868.68'), default_price=Decimal('100'), units='money',
                                          partner_units='amount', source_id=19, type_id=6,
                                          description=u'Партнерские Discovery',
                                          subtype=DistributionSubtype.REVSHARE,
                                          has_tail=True, service_id=None, units_type_rate=1, contract_price_id=8)

    def __init__(self, payment_type, page_id, place_id, result_page_id, default_amount, default_price,
                 units, partner_units, source_id, type_id, description, subtype, has_tail, service_id,
                 units_type_rate, contract_price_id):
        self.payment_type = payment_type
        self.page_id = page_id
        self.place_id = place_id
        self.result_page_id = result_page_id
        self.default_amount = default_amount
        self.default_price = default_price
        self.units = units
        self.partner_units = partner_units
        self.source_id = source_id
        self.type_id = type_id
        self.description = description
        self.subtype = subtype
        self.has_tail = has_tail
        self.service_id = service_id
        self.units_type_rate = units_type_rate
        self.contract_price_id = contract_price_id

