# coding: utf-8

__author__ = 'igogor'

from datetime import datetime
from decimal import Decimal

from balance.balance_objects import Product
from btestlib import utils
from btestlib.constants import Users, Nds, Currencies, Managers, Firms

PASSPORT_UID = Users.YB_ADM.uid

AUTO_PREFIX = '[MT]: '

PARTNER_COMMISSION_PCT = Decimal('3.55')


class Client(object):
    AGGREGATOR_PARTNER_TYPE = 2

    @staticmethod
    def default_params():
        return {
            # 'CLIENT_ID': None,  # int
            # 'CLIENT_TYPE_ID': None,  # int constants.ClientTypes.PHYS.id
            'NAME': 'balance_test {}'.format(datetime.now()),
            'EMAIL': 'client@in-fo.ru',
            'PHONE': '911',
            'FAX': '912',
            'URL': 'http://client.info/',
            'CITY': u'Батт',
            # 'IS_AGENCY': None,  # 0 или 1
            # 'AGENCY_ID': None,  # int
            # 'REGION_ID': None,  # int constants.Regions.RUSSIA.id
            # 'SERVICE_ID': None,  # int constants.Services.DIRECT.id
            # 'CURRENCY': None,  # int constants.Currencies.RUB.iso_code
            # 'MIGRATE_TO_CURRENCY': None,  # datetime
            # 'CURRENCY_CONVERT_TYPE': None,  # 'COPY' или 'MODIFY'
            # 'ONLY_MANUAL_NAME_UPDATE': None  # bool
        }


class ContractDefaults(object):
    MANAGER_CODE = Managers.PERANIDZE.code
    MANAGER_BO_CODE = Managers.FETISOVA.code
    MANAGER_RSYA_CODE = Managers.NIGAI.code


class Order(object):
    PRODUCT = Product(7, 1475, 'Bucks', 'Money')
    ORDER_QTY = 118
    CAMPAIGN_QTY = 99.9
    BASE_DT = datetime.now()
    MANAGER_UID = '244916211'
    PH_PAYSYS_ID = 1001
    UR_PAYSYS_ID = 1003

    @staticmethod
    def default_params():
        return {
            # 'ClientID': None,
            'ProductID': Order.PRODUCT.id,
            'ServiceID': Order.PRODUCT.service_id,
            # 'ServiceOrderID': None,
            # 'AgencyID': None,
            # 'ManagerUID': None,
            # 'RegionID': None,
            # 'discard_agency_discount': None,
            'Text': 'Py_Test order',  # Should NOT be changed
            'GroupServiceOrderID': -1,
            'GroupWithoutTransfer': 1
        }

    @staticmethod
    def default_orders_list(service_order_id, service_id=PRODUCT.service_id, qty=ORDER_QTY, dt=BASE_DT):
        return [{
            'ServiceID': service_id,
            'ServiceOrderID': service_order_id,
            'Qty': qty,
            'BeginDT': dt
        }]


def act():
    return {}


def tag():
    return {
        'TagName': 'CreatedByScript'
    }


def extprops():
    return {
        'attrname': None,
        'classname': None,
        'id': None,
        'key': None,
        'object_id': None,
        'passport_id': PASSPORT_UID,
        'update_dt': None,
        'value_clob': None,
        'value_dt': None,
        'value_num': None,
        'value_str': None
    }


def api_params():
    return {
        'service_code': 'ProductID',
        'product_id': 'ProductID',
        'agency_id': 'AgencyID',
        'service_order_id': 'ServiceOrderID',
        'client_id': 'ClientID',
        'service_id': 'ServiceID',
        'qty_old': 'QtyOld',
        'qty_new': 'QtyNew',
        'all_qty': 'AllQty',
        'qty_delta': 'QtyDelta'
    }


class Distribution(object):
    PLACE_INTERNAL_TYPE = 100
    PLACE_TYPE = 8
    DEFAULT_REVSHARE_SHOWS = 100
    DEFAULT_REVSHARE_CLICKS = 4
    DEFAULT_REVSHARE_CLICKSA = 3


class Partner(object):
    COMPLETION_TYPE_DSP = 'DSP'
    COMPLETION_TYPE_DIRECT = 'Direct'
    BUCKS = 100
    PARTNER_REWARD_DSP = Decimal('400.33')
    PLACE_ID = 222
    DSP_CHARGE = Decimal('8.895')
    DSP_BLOCK_ID = 2
    DSP_ID = 1
    DSP_HITS = 111
    DSP_SHOWS = 24
    DSP_TOTAL_RESPONSE_COUNT = 87
    DSP_TOTAL_BID_SUM = Decimal('45.5')
    DSP_DEAL_ID = 12345
    DSP_YANDEX_PRICE = Decimal('2.4')
    DSP_FAKE_PRICE = Decimal('2.8')
    PARTNER_STAT_ID = 12345
    PLACE_TYPE = 3
    DSP_AGGREGATION_PCT = 5
    PAGE_ID_SEARCH_FORMS = 2040
    PAGE_ID_STRIPES = 1194
    PAGE_ID_POPUPS = 1195
    PARTNER_PCT = Decimal('43')
    AGGREGATOR_PCT = Decimal('27')


def taxi_corp():
    return {
        'PAYMENT_TYPE_CORP': 'corporate',
        'ORDER_SUM': 3000,
        'LOGIN_TO_LINK': 'clientuid36',
        'UID_TO_LINK': 167627334,
        'DEFAULT_CURRENCY': 'RUR',
        'DEFAULT_PARTNER_CURRENCY': 'RUR',
        'DEFAULT_COMMISSION_CURRENCY': 'RUR',
        'DEFAULT_ISO_CURRENCY': 'RUB',
        'DEFAULT_ISO_PARTNER_CURRENCY': 'RUB',
        'DEFAULT_ISO_COMMISSION_CURRENCY': 'RUB',
        'PAYMENT_TYPE': 'cash',
        'PAYMENT_TYPE_COMPENSATION': 'compensation',
        'PAYSYS_TYPE_CC': 'yandex',
        'SERVICE_ID': 135,
        'OEBS_ORG_ID': 64552,
        'TRANSACTION_TYPE_FOR_PAYMENT': 'payment',
        'TRANSACTION_TYPE_FOR_REFUND': 'refund',
        'EMPTY_UID': 313834851,
        'PAGE_ID': 10600,
        'FIRM_ID': 13,
        'PAYSYS_ID': 1301003,
        'INVOICE_TYPE': 'personal_account',
        'DEFAULT_NDS_PCT': Nds.DEFAULT,
        'ACT_TYPE': 'generic',
    }


class TaxiPayment(object):
    TRANSACTION_TYPE_PAYMENT = 'payment'
    TRANSACTION_TYPE_REFUND = 'refund'

    PAYMENT_TYPE_COMPENSATION = 'compensation'
    PAYMENT_TYPE_CASH = 'cash'
    PAYMENT_TYPE_CARD = 'card'
    PAYMENT_TYPE_CORPORATE = 'corporate'
    PAYMENT_TYPE_DEFAULT = PAYMENT_TYPE_CARD

    PAYSYS_TYPE_CC_DEFAULT = 'yamoney'
    PAYSYS_TYPE_CC = 'wallet1'
    PAYSYS_TYPE_CC_COMPENSATION = 'yandex'
    PAYSYS_TYPE_CC_COMPENSATION_UKRAINE = 'yataxi'

    SERVICE_ID = 124
    OEBS_ORG_ID = 64552
    OEBS_ORG_ID_TAXIBV = 64621
    OEBS_ORG_ID_UKRAINE = 89471
    OEBS_ORG_ID_ARMENIA = 100110
    OEBS_ORG_ID_KAZAKHSTAN = 94969
    TRANSACTION_TYPE_FOR_PAYMENT = 'payment'
    TRANSACTION_TYPE_FOR_REFUND = 'refund'
    INVOICE_TYPE_PREPAYMENT = 'prepayment'
    INVOICE_TYPE_ACCOUNT = 'personal_account'
    ACT_TYPE = 'generic'
    FIRM_TAXI = 13
    FIRM_TAXIBV = 22
    FIRM_UKRAINE = 23
    PAYSYS_ID_USD = 2201041
    PAYSYS_ID_EUR = 2201039
    PAYSYS_ID_RUR = 1301003
    PAYSYS_ID_PROMO = 1301094
    DEFAULT_NDS_PCT = Nds.DEFAULT
    DEFAULT_NDS_PCT_TAXIBV = 0
    DEFAULT_AMOUNT_FOR_TOTAL_SUM = 8766.7
    DEFAULT_AMOUNT = 222
    DEFAULT_CURRENCY = 'RUR'
    DEFAULT_ISO_CURRENCY = 'RUB'
    DEFAULT_NDS = Nds.DEFAULT
    DEFAULT_COMMISSION_PCT = Decimal('0.01')
    DEFAULT_ORDER_AMOUNT = Decimal('1000.32')
    CURRENCY_UKRAINE = 'UAH'


class Taxi(object):
    CASH_SERVICE_ID = 111
    CARD_SERVICE_ID = 128

    UBER_CARD_SERVICE_ID = 125
    UBER_ROUMING_SERVICE_ID = 605

    PAYMENTS_SERVICE_ID = 124

    order_commission_cash = Decimal('60.9')
    order_commission_prepaid = Decimal('40.9')
    order_commission_card = Decimal('50.1')
    order_commission_corp = Decimal('70.1')

    childchair_card = Decimal('1.1')
    childchair_cash = Decimal('23.1')
    childchair_corp = Decimal('14.7')

    marketplace_advert_call_cash = Decimal('7.9')

    driver_workshift = Decimal('55.9')

    commission_correction_cash = Decimal('-18.4')
    commission_correction_card = Decimal('-3.7')

    hiring_with_car_cash = Decimal('3.8')
    hiring_with_car_card = Decimal('5.6')

    promocode_sum = Decimal('50')
    subsidy_sum = Decimal('33.99')

    # a-vasin: это всё должно быть в контекстах, но переписывать я это морально не готов, сорян
    CURRENCY_TO_PRODUCT = {
        Currencies.RUB: {'cash': 503352, 'card': 505142, 'correction': 503409, 'payment': 504691,
                         'childchair_cash': 508625, 'childchair_card': 508867, 'hiring_cash': 509001,
                         'hiring_card': 509005, 'payment_uber': 60509004, 'payment_uber_roaming': 60509005,
                         'marketplace_advert_call_cash': 509287},
        Currencies.USD: {'cash': 507859, 'card': 507860, 'correction': 508161, 'payment': 507858},
        Currencies.EUR: {'cash': 507862, 'card': 507863, 'correction': 508162, 'payment': 507861},
        Currencies.BYN: {'cash': 509674, 'card': 509673, 'correction': None, 'payment': 509688},
        Currencies.ILS: {'cash': 509825, 'card': 509826, 'correction': None, 'payment': 509827},
        Currencies.KZT: {'cash': 510017, 'card': 510018, 'correction': 510017, 'payment': 508436},
        Currencies.RON: {'cash': 510770, 'card': 510787, 'payment': 510786}
    }

    FIRM_CURRENCY_TO_PAYSYS = {
        Firms.TAXI_13.id: {
            Currencies.RUB: {'payment': 1301003, 'promo': 1301094}
        },
        Firms.TAXI_BV_22.id: {
            Currencies.USD: {'payment': 2201041, 'promo': 2201116},
            Currencies.EUR: {'payment': 2201039, 'promo': 2201119},
            Currencies.NOK: {'payment': 1212, 'promo': None},
        },
        Firms.UBER_115.id: {
            Currencies.USD: {'payment': 11501041, 'promo': 13701116},
            Currencies.BYN: {'payment': 1145, 'promo': 11501146}
        },
        Firms.MLU_EUROPE_125.id: {
            Currencies.USD: {'payment': 12501041, 'promo': None},
            Currencies.EUR: {'payment': 12501039, 'promo': None},
            Currencies.SEK: {'payment': 1147123, 'promo': None},
        },
        Firms.TAXI_UA_23.id: {
            Currencies.UAH: {'payment': 2301017, 'promo': None}
        },
        Firms.MLU_AFRICA_126.id: {
            Currencies.USD: {'payment': 12601041, 'promo': None}
        },
        Firms.TAXI_CORP_KZT_31.id: {
            Currencies.KZT: {'payment': 3101020, 'promo': None},
        },
        Firms.YANDEX_GO_SRL_127.id: {
            Currencies.RON: {'payment': 1150, 'promo': None},
        },
    }


# все возможные нац.версии
class NatVer(utils.ConstantsContainer):
    constant_type = str
    RU = 'ru'
    COM = 'com'
    TR = 'tr'
    KZ = 'kz'
    UA = 'ua'


AVIA_PRODUCT_IDS = {
    NatVer.RU: {Currencies.RUB: 508952, Currencies.EUR: 508957},
    NatVer.COM: {Currencies.RUB: 508969, Currencies.EUR: 508972},
    NatVer.TR: {Currencies.RUB: 508974, Currencies.EUR: 508977},
    NatVer.KZ: {Currencies.RUB: 508964, Currencies.EUR: 508967},
    NatVer.UA: {Currencies.RUB: 508959, Currencies.EUR: 508962},
}

class TaxiNewPromo(object):
    CASH_SERVICE_ID = 111
    CARD_SERVICE_ID = 128

    UBER_CARD_SERVICE_ID = 125
    UBER_ROUMING_SERVICE_ID = 605

    PAYMENTS_SERVICE_ID = 124

    order_commission_cash = Decimal('160.9')
    order_commission_card = Decimal('150.1')
    order_commission_corp = Decimal('170.1')

    childchair_card = Decimal('61.1')
    childchair_cash = Decimal('123.1')
    childchair_corp = Decimal('114.7')

    driver_workshift = Decimal('55.9')
    driver_workshift_cash = Decimal('55.9')
    driver_workshift_card = Decimal('3.2')

    commission_correction_cash = Decimal('-18.4')
    commission_correction_card = Decimal('-3.7')

    marketplace_advert_call_cash = Decimal('7.9')

    hiring_with_car_cash = Decimal('63.8')
    hiring_with_car_card = Decimal('75.6')

    cargo_cash = Decimal('92.11')
    cargo_card = Decimal('61.42')

    cargo_driver_workshift_cash = Decimal('81.2')
    cargo_driver_workshift_card = Decimal('87.7')

    cargo_hiring_with_car_cash = Decimal('66.6')
    cargo_hiring_with_car_card = Decimal('83.2')

    delivery_cash = Decimal('94.1')
    delivery_card = Decimal('99.32')

    delivery_driver_workshift_cash = Decimal('99.4')
    delivery_driver_workshift_card = Decimal('77.5')

    delivery_hiring_with_car_cash = Decimal('81.6')
    delivery_hiring_with_car_card = Decimal('102.2')

    promocode_sum = Decimal('50')
    subsidy_sum = Decimal('33.98')

    CURRENCY_TO_PRODUCT = {
        Currencies.RUB: {'cash': 503352, 'card': 505142, 'correction': 503409, 'payment': 504691,
                         'childchair_cash': 508625, 'childchair_card': 508867, 'hiring_cash': 509001,
                         'hiring_card': 509005, 'payment_uber': 60509004, 'payment_uber_roaming': 60509005,
                         'marketplace_advert_call_cash': 509287},
        Currencies.USD: {'cash': 507859, 'card': 507860, 'correction': 508161, 'payment': 507858},
        Currencies.EUR: {'cash': 507862, 'card': 507863, 'correction': 508162, 'payment': 507861},
        Currencies.KZT: {'cash': 510017, 'card': 510018, 'correction': 510017, 'payment': 508436},
        Currencies.RON: {'cash': 510770, 'card': 510787, 'payment': 510786}
    }

    FIRM_CURRENCY_TO_PAYSYS = {
        Firms.TAXI_13.id: {
            Currencies.RUB: {'payment': 1301003, 'promo': 1301094}
        },
        Firms.TAXI_BV_22.id: {
            Currencies.USD: {'payment': 2201041, 'promo': 2201116},
            Currencies.EUR: {'payment': 2201039, 'promo': 2201119},
            Currencies.NOK: {'payment': 1212, 'promo': None},
        },
        Firms.UBER_115.id: {
            Currencies.USD: {'payment': 11501041, 'promo': 13701116},
            Currencies.BYN: {'payment': 1145, 'promo': 11501146},
        },
        Firms.UBER_1088.id: {
            Currencies.USD: {'payment': 108801041, 'promo': 111001116},
            Currencies.BYN: {'payment': 108801145, 'promo': 108801146},
        },
        Firms.TAXI_UA_23.id: {
            Currencies.UAH: {'payment': 2301017, 'promo': None}
        },
        Firms.YANDEX_GO_ISRAEL_35.id: {
            Currencies.ILS: {'payment': 1200, 'promo': None}
        },
        Firms.YANGO_ISRAEL_1090.id: {
            Currencies.ILS: {'payment': 109001200, 'promo': None}
        },
        Firms.MLU_EUROPE_125.id: {
            Currencies.USD: {'payment': 12501041, 'promo': None},
            Currencies.EUR: {'payment': 12501039, 'promo': None},
            Currencies.RON: {'payment': 1147, 'promo': None},
            Currencies.SEK: {'payment': 1147123, 'promo': None},
        },
        Firms.MLU_AFRICA_126.id: {
            Currencies.USD: {'payment': 12601041, 'promo': None}
        },
        Firms.TAXI_CORP_KZT_31.id: {
            Currencies.KZT: {'payment': 3101020, 'promo': None},
        },
        Firms.YANDEX_GO_SRL_127.id: {
            Currencies.RON: {'payment': 1150, 'promo': None},
        },
        Firms.TAXI_CORP_ARM_122.id: {
            Currencies.USD: {'payment': 12202401, 'promo': None},
            Currencies.EUR: {'payment': 12202402, 'promo': None},
            Currencies.AMD: {'payment': 12201122, 'promo': None},
            Currencies.BYN: {'payment': 2409, 'promo': None},
            Currencies.NOK: {'payment': 2408, 'promo': None},
        },
    }


class TaxiNetting(object):
    TRANSACTION_TYPE = 'refund'
    CURRENCY_RUR = 'RUR'
    ISO_CURRENCY_RUR = 'RUB'
    PAYSYS_TYPE_CC = 'yandex'
    AUTO = 1
    AMOUNT_FEE = 0
    YANDEX_REWARD = 0
    YANDEX_REWARD_WO_NDS = 0
    INTERNAL = None
    OEBS_ORG_ID = 64552
    SERVICE_ID = 124


class EventsTickets(object):
    DEFAULT_CURRENCY = 'RUR'
    DEFAULT_PARTNER_CURRENCY = 'RUR'
    DEFAULT_COMMISSION_CURRENCY = 'RUR'
    DEFAULT_ISO_CURRENCY = 'RUB'
    DEFAULT_ISO_PARTNER_CURRENCY = 'RUB'
    DEFAULT_ISO_COMMISSION_CURRENCY = 'RUB'
    PAYMENT_TYPE = 'card'
    PAYMENT_TYPE_CARD_WEB = 'direct_card'
    PAYMENT_TYPE_COMPENSATION = 'compensation'
    PAYMENT_TYPE_COMPENSATION_DISCOUNT = 'compensation_discount'
    PAYMENT_TYPE_FOR_PROMOCODE = 'new_promocode'
    PAYSYS_TYPE_CC_YAMONEY = 'yamoney'
    PAYSYS_TYPE_CC_YANDEX = 'yandex'
    SERVICE_ID = 126
    OEBS_ORG_ID = 121
    FIRM_ID = 1
    TRANSACTION_TYPE_FOR_PAYMENT = 'payment'
    TRANSACTION_TYPE_FOR_REFUND = 'refund'
    PAYSYS_ID = 1003
    INVOICE_TYPE = 'personal_account'
    DEFAULT_NDS_PCT = Nds.DEFAULT
    ACT_TYPE = 'generic'


def multiship():
    return {
        'PRODUCT_WITH_DISCOUNT': 504970,
        'DEFAULT_PRODUCT': 505060
    }


class ADFox(object):
    PRODUCT_ADFOX_MOBILE_MAIN = 505173
    PRODUCT_ADFOX_MOBILE_DEFAULT = 504402
    PRODUCT_ADFOX_UNIT_PRODUCT = 505176
    PRODUCT_ADFOX_DMP = 10000024
    PRODUCT_ADFOX_SITES = 505170
    # PRODUCT_ADFOX_DMP = 508333
    DEFAULT_REQUESTS = Decimal('999000000')
    DEFAULT_REQUESTS_WO_ORDER = Decimal('1000')
    DEFAULT_MAIN_PRICE = Decimal('0.63')
    DEFAULT_SHOWS = Decimal('380000000')
    DEFAULT_SHOWS_FOR_VIP = Decimal('20000')
    DEFAULT_SHOWS_FOR_VIP2 = Decimal('30000')
    DEFAULT_SHOWS_FOR_DMP = Decimal('20000')
    DEFAULT_REQUESTS_FOR_DMP = Decimal('1000')
    DEFAULT_PRICE_FOR_DMP_1 = Decimal('20')
    # DEFAULT_PRICE_FOR_DMP_2 = Decimal('20')
    DEFAULT_DEF_PRICE = Decimal('0.4')
    DEFAULT_REQUESTS_FOR_UNIT9 = Decimal('64000000')
    DEFAULT_UNIT9_PRICE = Decimal('24500')


class DSP(object):
    PRODUCT_ID = 503364
    ADDITIONAL_PRODUCT_ID = 504076
    PRODUCT_ID_SW = 503367
    ADDITIONAL_PRODUCT_ID_SW = 504319
    PRODUCT_ID_US = 503365
    ADDITIONAL_PRODUCT_ID_US = 504317


SERVICE_ID_TO_CONTRACT_TYPE_SPENDABLE_MAP = {119: 89, 135: 81, 137: 87, 210: 85, 204: 45, 601: 48, 134: 35, 609: 91}

# List of the typical products
service_product_list = '''
    #--- 5 ---
    service_id = 5;  product_id = 85     # ???
    #--- Директ ---
    service_id = 7;  product_id = 1475   #
    service_id = 7;  product_id = 503162 # RUB
    service_id = 7;  product_id = 503163 # USD
    service_id = 7;  product_id = 503164 # EUR
    service_id = 7;  product_id = 503165 # UAH
    service_id = 7;  product_id = 503166 # KZT
    service_id = 7;  product_id = 503353 # CHF
    service_id = 7;  product_id = 503354 # TRY
    #--- Маркет ---
    service_id = 11; product_id = 2136   #
    service_id = 11; product_id = 503790 # CPA
    #--- Справочник ---
    service_id = 37; product_id = 502918 # comm_type = 12
    service_id = 37; product_id = 502950 # comm_type = 15
    #--- Технологии Яндекса ---
    service_id = 45; product_id = 503870 #
    #--- Метрика ---
    service_id = 48; product_id = 503369 #
    #--- Баннерокрутилка ---
    service_id = 67; product_id = 2306   #
    service_id = 67; product_id = 2600   #
    #--- Медиаселлинг ---
    service_id = 70; product_id = 504033
    service_id = 70; product_id = 503306 # TRP; full_render = 1
    service_id = 70; product_id = 503307 # TRP; full_render = 1
    service_id = 70; product_id = 503286 # TRP; full_render = 1 0
    service_id = 70; product_id = 503305 # full_render = 0
    service_id = 70; product_id = 503384 # EUR
    service_id = 70; product_id = 503387 # EUR
    service_id = 70; product_id = 503389 # CHF
    service_id = 70; product_id = 503386 # CHF
    service_id = 70; product_id = 503385 # USD
    service_id = 70; product_id = 503388 # USD
    service_id = 70; product_id = 503139 # RUB
    service_id = 70; product_id = 503785 # UAH 1
    service_id = 70; product_id = 503243 # UAH
    service_id = 70; product_id = 503240 # media_discount 8
    service_id = 70; product_id = 503283 # media_discount 1
    service_id = 70; product_id = 502956 # media_discount 2
    service_id = 70; product_id = 503376 # media_discount 3
    service_id = 70; product_id = 503802 # media_discount 4
    service_id = 70; product_id = 503258 # media_discount 13
    service_id = 70; product_id = 503242 # UAH 18
    service_id = 70; product_id = 502410 # UAH 8
    #--- 77 ---
    service_id = 77; product_id = 2584   #
    #--- 80 ---
    service_id = 80; product_id = 503247 #
    #--- Недвижимость ---
    service_id = 81; product_id = 503932 #
    #--- Авто.ру ---
    service_id = 99; product_id = 504697 # Шины и Диски
    service_id = 99; product_id = 504596 # Авто
    #--- 114 ---
    service_id = 114; product_id = 502981 #
    '''


class Date(object):
    NOW = datetime.now

    TODAY = utils.Date.nullify_time_of_date(NOW())
    TODAY_ISO = utils.Date.to_iso(TODAY)

    HALF_YEAR_AFTER_TODAY = utils.Date.shift_date(TODAY, months=6)
    HALF_YEAR_AFTER_TODAY_ISO = utils.Date.to_iso(HALF_YEAR_AFTER_TODAY)

    YEAR_AFTER_TODAY = utils.Date.shift_date(TODAY, years=1)
    YEAR_AFTER_TODAY_ISO = utils.Date.to_iso(YEAR_AFTER_TODAY)


if __name__ == '__main__':
    pass
