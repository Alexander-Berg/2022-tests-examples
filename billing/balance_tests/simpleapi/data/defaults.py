# coding=utf-8
import datetime
from decimal import Decimal
from time import time
import btestlib.utils as utils

from simpleapi.data import uids_pool as uids

__author__ = 'fellow'

default_waiting_time = 60

partner_info = {'name': 'Vasya Pupkin',
                'email': 'vasya@pupkin.com',
                'operator_uid': 112986776}

admin = uids.mimino['trust_mimino_user_1']

user_ip = '::ffff:86.57.167.10'
back_url = None
return_path = 'http://yandex.ru'
email = 'test@test.ru'
phone = '+79999999999'
product_name = 'Super Product'

product_prices = [{'region_id': 225, 'dt': 1347521693, 'price': '10', 'currency': 'RUB'},
                  {'region_id': 84, 'dt': 1327521693, 'price': '10', 'currency': 'USD'},
                  {'region_id': 168, 'dt': 1327521693, 'price': '10', 'currency': 'AMD'},
                  {'region_id': 983, 'dt': 1327521693, 'price': '10', 'currency': 'USD'},
                  {'region_id': 159, 'dt': 1568021052, 'price': '10', 'currency': 'KZT'}
                  ]
product_prices_introductory = [{'region_id': 225, 'dt': 1347521693, 'price': '5', 'currency': 'RUB'},
                               {'region_id': 84, 'dt': 1327521693, 'price': '5', 'currency': 'USD'},
                               {'region_id': 168, 'dt': 1327521693, 'price': '5', 'currency': 'AMD'},
                               {'region_id': 983, 'dt': 1327521693, 'price': '5', 'currency': 'USD'},
                               {'region_id': 159, 'dt': 1568021052, 'price': '5', 'currency': 'KZT'}
                               ]

product_price = 10

taxi_pass_params = {
    'taxi_user_phone_id': '0test1taxi2user3phone4id5',
    'taxi_device_id': '0test1taxi2device3id4',
    'taxi_car_number': '0test1taxi2car3number4',
    'taxi_performer_id': '0test1taxi2performer3id4',
    'taxi_driver_license': '0test1taxi2driver3license4',
    'taxi_card_id': '0test1taxi2card3id4'
}

tinkoff_pass_params = {
    "submerchantIdRbs": 63067
}


rbs_ru_processing_id = 50105
yam_processing_id = 50015


class Status(object):
    base_success = 'base_success'
    force_3ds = 'force_3ds'
    not_enough_funds_RC51 = 'RC=51, reason=Not enough funds'
    do_not_honor_RC05 = 'RC=05, reason=Do not honor'
    error_RC06 = 'RC=06, reason=Error'
    invalid_transaction_RC12 = 'RC=12, reason=Invalid transaction'
    restricted_card_RC36 = 'RC=36, reason=Restricted card'
    transaction_not_permitted_RC57 = 'RC=57, reason=Transaction not permitted to card'
    transaction_not_permitted_RC58 = 'RC=58, reason=Transaction not permitted to card'
    restricted_card_RC62 = 'RC=62, reason=Restricted card'
    fraud_error = 'FRAUD_ERROR'

    afs_challenge = 'afs_challenge'
    afs_deny = 'afs_deny'
    afs_whitelist = 'afs_wl_in_tags'

    not_enough_funds = 'not_enough_funds'
    authorization_reject = 'authorization_reject'
    technical_error = 'technical_error'
    transaction_not_permitted = 'transaction_not_permitted'
    limit_exceeded = 'limit_exceeded'
    fail_3ds = 'fail_3ds'
    unknown_error = 'unknown_error'


class Trigger(object):
    def __init__(self, card_number=None, cvn=None, amount=None, penny=None):
        self.card_number = card_number
        self.cvn = cvn
        self.amount = amount
        self.penny = penny


triggers = {
    Status.base_success: Trigger(None, '850', 10.00, 0.1),
    Status.force_3ds: Trigger('340905162506603', '123', 1093.00, 0.93),
    Status.not_enough_funds_RC51: Trigger('374976879730769', '009', 1099.00, 0.99),
    Status.do_not_honor_RC05: Trigger('349237549948055', '208', 1090.00, 0.90),
    Status.error_RC06: Trigger('376962667180910', '218', 1091.00, 0.91),
    Status.invalid_transaction_RC12: Trigger('344324871962337', '228', 1092.00, 0.92),
    Status.restricted_card_RC36: Trigger('371086760185504', '238', 1097.00, 0.97),
    Status.transaction_not_permitted_RC57: Trigger('347134900428615', '248', 1094.00, 0.94),
    Status.transaction_not_permitted_RC58: Trigger('348227425051315', '258', 1095.00, 0.95),
    Status.restricted_card_RC62: Trigger('371207485651337', '268', 1096.00, 0.96),
    Status.fraud_error: Trigger(None, None, 5001, None),

    Status.afs_challenge: Trigger(amount=10.13),
    Status.afs_deny: Trigger(amount=10.19),
    Status.afs_whitelist: Trigger(amount=10.18)
}


class ServiceProduct(object):
    app = {
        'prices': product_prices,
        'name': product_name,
        'type_': 'app',
    }

    class Subscription(object):
        TRIAL = {
            'name': 'Subscription trial product',
            'prices': product_prices,
            'type_': 'subs',
            'subs_period': '60S',
            'subs_trial_period': '60S',
            'active_until_dt': datetime.datetime.now() + datetime.timedelta(minutes=40),
            'single_purchase': 1,
        }
        NORMAL = {
            'name': 'Subscription normal product',
            'prices': product_prices,
            'type_': 'subs',
            'subs_period': '60S',
            'active_until_dt': datetime.datetime.now() + datetime.timedelta(minutes=40)
        }
        NORMAL_SINGLE_PURCHASED = {
            'name': 'Subscription normal product',
            'prices': product_prices,
            'type_': 'subs',
            'subs_period': '60S',
            'active_until_dt': datetime.datetime.now() + datetime.timedelta(minutes=40),
            'single_purchase': 1
        }
        INTRODUCTORY = {
            'name': 'Subscription introductory product',
            'prices': product_prices,
            'type_': 'subs',
            'subs_period': '60S',
            'subs_introductory_period': '60S',
            'subs_introductory_period_prices': product_prices_introductory,
            'active_until_dt': datetime.datetime.now() + datetime.timedelta(minutes=40),
            'single_purchase': 1,
        }


class Person(object):
    PH = {'email': 'testapi@yandex.ru',
          'fname': 'test',
          'mname': 'simple',
          'lname': 'api',
          'kpp': '123456789',
          'person_id': 0,
          'phone': '+73214567345',
          'type': 'ph'
          }
    UA = {'fname': 'test',
          'mname': 'simple',
          'lname': 'api',
          'phone': '+380 44 808571',
          'fax': '+380 44 572745',
          'email': 'testapi@yandex.ru',
          'inn': '245781126558',
          'type': 'pu',
          }


class Client(object):
    DEFAULT = {
        'name': 'Vasya Pupkin',
        'email': 'vasya@pupkin.com',
        'phone': '+79214567323',
        'fax': '+79214567323',
        'url': 'test.url',
        'city': 'St.Petersburg'
    }


class TemplateTag(object):
    desktop = 'desktop/form'
    mobile = 'mobile/form'
    smarttv = 'smarttv/form'


class Music(object):
    INAPP_TMPL = {"product_id": "io.drewnoff.subscriptions.%s",
                  "title": "Simple InApp",
                  "author": "DrewNoff",
                  "autorenewable": True,
                  "period": 10,
                  "description": "Test InApp"}

    class SubsPlan(object):
        PROLONG = [{'value': 'Trial$'},
                   {'value': 'Paid$'},
                   {'value': 'Paid$'},
                   ]
        LAPSE = [{'value': 'Trial$'},
                 {'value': 'Paid$'},
                 {'idle': 1.4},
                 {'value': 'Paid$'},
                 # {'value': 'Paid$'},
                 ]

    USER = {'_id': 'f6c5626e-23c6-46d0-a0c0-2c83269fc254'}

    product_id = 503355


class Direct(object):
    product_id = 503162
    product_id_uah = 503165


class Marketplace(object):
    shop_params_test = {
        'card': {'ym_shop_id': '139335',
                 'ym_shop_article_id': '697224'},
        'yandex_money': {'ym_shop_id': '13629',
                         'ym_shop_article_id': '136291',
                         'ym_scid': '5581'}
    }
    shop_params_emulator = {
        'card': {'ym_shop_id': '66666',
                 'ym_shop_article_id': '66666',
                 'processing_cc': 'yamoney_h2h_emulator'},
    }


class Subscriptions(object):
    class State(object):
        """
        0 - идет триальный период, способ оплаты не указан;
        1 - идет триальный период, способ оплаты указан;
        2 - триальный период закончился, неоплачена;
        3 - оплачена;
        4 - остановлена
        5 - остановлена, и у продукта есть subs_introductory_period
        6 - идет introductory_period, оплачена
        7 - introductory_period закончился, неоплачена
        """
        TRIAL_PERIOD_NO_PAYMETHOD = 0
        TRIAL_PERIOD_WITH_PAYMETHOD = 1
        NOT_PAID = 2
        PAID = 3
        FINISHED = 4
        STOPPED_WITH_INTRODUCTORY_PERIOD = 5
        PAID_INTRODUCTORY_PERIOD_IN_PROGRESS = 6
        NOT_PAID_INTRODUCTORY_PERIOD_FINISHED = 7


class Promocode(object):
    class Status(object):
        active = 'active'
        expired = 'expired'
        not_started = 'not_started'

    name = 'autotest_series'
    promocode_amount_part = 5
    promocode_amount_full = 15
    promocode_amount_big = 5000
    series_amount = 100
    quantity = 1

    base_params = {
        # 'code': 'promocode code',
        'amount': 100,
        'begin_ts': time() - 60,
        'end_ts': time() + 20 * 60,
        'quantity': 5,
    }
    mandatory_params = {

    }

    @staticmethod
    def custom_params(to_update, params=None):
        # здесь лучше использовать mandatory_params, но пока непонятно что туда входит
        if not params:
            params = Promocode.base_params
        params = Promocode.base_params.copy()
        params.update(to_update)
        return params

    @staticmethod
    def params_with_promoseries_params(promocode_params, promoseries_params):
        promoseries_params_copy = promoseries_params.copy()
        promoseries_params_copy.update(promocode_params)

        return promoseries_params_copy


class Promoseries(object):
    mandatory_params = {
        'name': 'autotest_series',
        'amount': 10,
    }

    base_params = {
        'name': u'autotest_series',
        'description': u'test description',
        'begin_ts': time(),
        'amount': 10,
        'end_ts': time() + 20 * 60,
    }

    @staticmethod
    def custom_params(to_update):
        # здесь лучше использовать mandatory_params, но пока непонятно что туда входит
        params = Promoseries.base_params.copy()
        params.update(to_update)
        return params


class CountryData(object):
    @staticmethod
    def get_clear_data(data):
        return {k: v for k, v in data.iteritems() if k in ('user_ip', 'currency', 'region_id')}

    Ukraine = {
        'name': 'Ukraine',
        'user_ip': '62.16.31.254',
        'currency': 'UAH',
        'region_id': '187'
    }
    Armenia = {
        'name': 'Armenia',
        'user_ip': '185.8.2.153',
        'currency': 'AMD',
        'region_id': '168'
    }
    Georgia = {
        'name': 'Georgia',
        'user_ip': '194.60.250.210',
        'currency': 'GEL',
        'region_id': '169'
    }
    Kazakhstan = {
        'name': 'Kazakhstan',
        'user_ip': '193.193.238.84',
        'currency': 'KZT',
        'region_id': '159'
    }
    Russia = {
        'name': 'Russia',
        'user_ip': '77.88.55.50',
        'currency': 'RUB',
        'region_id': '225'
    }
    Germany = {
        'name': 'Germany',
        'user_ip': '89.204.130.100',
        'currency': 'EUR',
        'region_id': None
    }
    USA = {
        'name': 'USA',
        'user_ip': '97.77.104.22',
        'currency': 'USD',
        'region_id': '84'
    }
    Moldova = {
        'name': 'Moldova',
        'user_ip': '212.0.192.0',
        'currency': 'MDL',
        'region_id': '208'
    }
    Other = {  # Algeria
        'name': 'Other',
        'user_ip': '41.210.64.0',
        'currency': 'RUB',
        'region_id': '225'
    }
    Belarus = {
        'name': 'Belarus',
        'user_ip': '77.88.55.50',
        'currency': 'BYN',
        'region_id': '149',
    }
    Azerbaijan = {
        'name': 'Azerbaijan',
        'user_ip': '77.88.55.50',
        'currency': 'AZN',
        'region_id': '167',
    }
    Latvia = {
        'name': 'Latvia',
        'user_ip': '83.99.128.0',
        'currency': 'EUR',
        'region_id': '206'
    }
    Estonia = {
        'name': 'Estonia',
        'user_ip': '83.99.128.0',
        'currency': 'EUR',
        'region_id': '179'
    }
    Kyrgyzstan = {
        'name': 'Kyrgyzstan',
        'user_ip': '31.192.248.0',
        'currency': 'KGS',
        'region_id': '207'
    }
    Uzbekistan = {
        'name': 'Uzbekistan',
        'user_ip': '91.196.76.23',
        'currency': 'UZS',
        'region_id': '171'
    }
    Serbia = {
        'name': 'Serbia',
        'user_ip': '46.19.224.192',
        'currency': 'RSD',
        'region_id': '180'
    }
    CountryWithoutCurrency = {
        'name': 'Niue',
        'user_ip': '199.91.160.22',
        'currency': 'RUB',
        'region_id': '98542'
    }


class Terminal(object):
    class YaMoney(object):
        RUB = 90200069
        RUB_emu = 96013103
        RUB_emu_afs = 96013105

    class Payture(object):
        AMD = 95426002
        AMD_emu = 96026101

    class RBS(object):
        KZT = 95023010
        UAH = 95023006
        UZS = None

        KZT_emu = 96024102
        UAH_emu = 96023102
        UZS_emu = 96022110

    class Ecommpay(object):
        EUR = 95422001
        GEL = 95422002
        MDL = 95422003
        KGS = 95422004

        EUR_emu = 96022101
        GEL_emu = 96022102
        MDL_emu = 96022103
        KGS_emu = None  # have no now, but will be

    class Sberbank(object):
        RUB = 95713001
        RUB_emu = None  # have no now, but will be


class RBS(object):
    class BindStatusDesc(object):
        # Подробные комментарии в доке здесь https://st.yandex-team.ru/TRUST-2247
        success = 'card bound ok'
        code20010 = 'Code [-20010] - [payer_limit_exceeded]'
        code902 = 'Code [902] - [card_limitations]'
        code151017 = 'Code [151017] - [issuer_declined_3ds_auth]'
        code123 = 'Code [123] - [transaction_number_limit]'
        code913 = 'Code [913] - [declined_by_issuer]'
        code5 = 'Code [5] - [declined_by_issuer]'
        code116 = 'Code [116] - [not_enough_money]'


class Sberbank(object):
    class BindStatusDesc(object):
        code116 = 'ActionCode [116] - [not_enough_money]'
        code151018 = 'ActionCode [151018] - [processing_timeout]'


class Payture(object):
    class BindStatusDesc(object):
        # Подробные комментарии в доке здесь https://st.yandex-team.ru/TRUST-2925
        success = 'card bound ok'
        black_listed = "Code [ISSUER_BLOCKED_CARD]"
        expired_card = 'invalid_expiration_date'  # response from trust
        no_funds = "Code [AMOUNT_EXCEED]"
        issuer_card_fail = "Code [ISSUER_CARD_FAIL]"
        issuer_blocked_card = "Code [ISSUER_BLOCKED_CARD]"
        timeout = 'Operation timed out after 10020 milliseconds with 0 bytes received'  # response from trust
        processing_error = "Code [PROCESSING_ERROR]"


class Ecommpay(object):
    class BindStatusDesc(object):
        success = 'card bound ok'


class PaymentMode(object):
    WEB_PAYMENT = 'web_payment'
    API_PAYMENT = 'api_payment'
    EXTERNAL_WEB_PAYMENT = 'external_web_payment'
    INVOICING = 'invoicing'


class PaymentApi(object):
    class Product(object):
        app = {'product_type': 'app',
               'name': 'TestTestAppProduct'}

        prices = [{'region_id': 225, 'start_ts': time(), 'price': '10', 'currency': 'RUB'},
                  {'region_id': 84, 'start_ts': time(), 'price': '10', 'currency': 'USD'},
                  {'region_id': 168, 'start_ts': time(), 'price': '10', 'currency': 'AMD'},
                  {'region_id': 983, 'start_ts': time(), 'price': '10', 'currency': 'USD'},
                  ]

    partner_name = 'RestTestPartner'
    partner_email = 'RestTest@mail.ru'
    partner_city = 'RestTestCity'
    product_name = 'RestTestProduct'

    class Binding(object):
        class Status(object):
            success = 'paid ok'

    class Status(object):
        not_started = 'not_started'
        started = 'started'
        authorized = 'authorized'
        cleared = 'cleared'
        refunded = 'refunded'
        canceled = 'canceled'
        started_3ds = '3ds_started'
        not_authorized = 'not_authorized'


class RestSubscription(object):
    NORMAL = {
        'name': 'Subscription normal rest product',
        'prices': PaymentApi.Product.prices,
        'product_type': 'subs',
        'subs_period': '60S',
        'active_until_ts': time() + 60 * 40
    }
    TRIAL = {
        'name': 'Subscription trial rest product',
        'prices': PaymentApi.Product.prices,
        'product_type': 'subs',
        'subs_period': '60S',
        'subs_trial_period': '60S',
        'active_until_ts': time() + 60 * 40,
        'single_purchase': 1,
    }


class PCIDSS(object):
    class State(object):
        normal = 'NORMAL'
        generate_kek = 'GENERATE_KEK'
        confirm_kek_parts = 'CONFIRM_KEK_PARTS'
        reencrypt_deks = 'REENCRYPT_DEKS'
        switch_kek = 'SWITCH_KEK'
        recalculate_hmacs = 'RECALCULATE_HMACS'
        cleanup = 'CLEANUP'

    class Path(object):
        scheduler_path = '/var/remote-log/pci-dev1{}.paysys.yandex.net/cp_scheduler/card_pyproxy_scheduler.log'
        servant_restart_path = '/var/remote-log/pci-dev1{}.paysys.yandex.net/{}'
        confpatch_conf_path = '/var/remote-log/pci-dev1{}.paysys.yandex.net/conf-card_pyproxy_common/key_settings.cfg.xml'

    class Port(object):
        keykeeper_python = '14011'
        keykeeper_c = '14021'
        confpatch = '14201'
        tokenizer = '14303'
        keyapi = '14301'

    servant_pool = ['e', 'f', 'h']
    base_servant = 'f'


class Discounts(utils.ConstantsContainer):
    constant_type = dict
    id100 = {'id': '100', 'pct': Decimal('10')}
    id200 = {'id': '200', 'pct': Decimal('10')}
    id210 = {'id': '210', 'pct': Decimal('50')}
    id220 = {'id': '220', 'pct': Decimal('30')}
    id300 = {'id': '300', 'pct': Decimal('10')}
    id123 = {'id': '123', 'pct': Decimal('10')}


class DiscountsForBin(object):
    id101 = {
        'id': '101',
        'pct': Decimal('10'),
        'max_price': 300,
    }
    prices_for_10_pct_300_max = [
        Decimal('2000.20'),
        Decimal('3000.00'),
        Decimal('4000.40'),
    ]


class Fiscal(object):
    fiscal_title = 'test_fiscal_status'
    fiscal_partner_inn = '111111111111111'
    fiscal_partner_phone = '81111111111'

    firm_email = u'test@email'
    firm_url = u'http://test.url'

    class NDS(object):
        nds_none = 'nds_none'
        nds_0 = 'nds_0'
        nds_10 = 'nds_10'
        nds_10_110 = 'nds_10_110'
        nds_18 = 'nds_18'
        nds_18_118 = 'nds_18_118'
        nds_20 = 'nds_20'

    class TaxationType(object):
        OSN = 'OSN'
        USN_income = 'USN_income'
        USN_income_minus_charge = 'USN_income_minus_charge'
        ESN_calc_income = 'ESN_calc_income'
        ESN_agriculture = 'ESN_agriculture'
        patent = 'patent'


class Order(object):
    @staticmethod
    def gen_single_order_structure(status):
        return ({'currency': 'RUB', 'price': triggers[status].amount},)

    price = triggers[Status.base_success].amount + triggers[Status.base_success].penny
    fiscal_price = triggers[Status.base_success].amount * 10 + triggers[Status.base_success].penny
    qty = 30
    structure_rub_one_order = (
        {'currency': 'RUB', 'price': price, 'fiscal_nds': Fiscal.NDS.nds_none, 'fiscal_title': Fiscal.fiscal_title},)
    structure_rub_two_orders = (
        {'currency': 'RUB', 'price': price, 'fiscal_nds': Fiscal.NDS.nds_none, 'fiscal_title': Fiscal.fiscal_title},
        {'currency': 'RUB', 'price': price, 'fiscal_nds': Fiscal.NDS.nds_none, 'fiscal_title': Fiscal.fiscal_title})
    structure_rub_fiscal = (
        {'currency': 'RUB', 'price': fiscal_price, 'fiscal_nds': Fiscal.NDS.nds_none,
         'fiscal_title': Fiscal.fiscal_title},)

    order_created = 'order_created'
    order_purchased = 'order_purchased'


class PayMethodsInWeb(object):
    new_card = 'new_card'
    new_promocode = 'new_promocode'


class MasterPass(object):
    MerchantName = 'YandexAfishaTest'
    TransactionType = 'Purchase'
    OriginalOrderId = 'mp_test_1'
    DealDate = '2017-10-18 12:13:14'


class BoundTo(object):
    masterpass = 'masterpass'
    trust = 'trust'


class Processing_cc(object):
    yam_emu = 'yamoney_h2h_emulator'


class BindingMethods(object):
    auto = 'auto'
    standard1 = 'standard1'
    standard2 = 'standard2'
    random_amt = 'random_amt'
    standard2_3ds = 'standard2_3ds'


class BindingStatus(object):
    in_progress = 'in_progress'
    success = 'success'
    amount_expected = 'amount_expected'
    failure = 'failure'
    required_3ds = '3ds_required'
    status_3ds_received = '3ds_status_received'


class BindingEvent(object):
    card_data_received = 'card_data_received'
    verify_start = 'verification_start'
    authorize_result = 'authorization_result'
    confirmation_code_received = 'confirmation_code_received'
    start_3ds = '3ds_start'
    status_3ds_received = '3ds_status_received'


class Binding3dsCode(object):
    success = 200
    wrong_3ds = 400


class GooglePayErrors(object):
    # возможные коды ошибок здесь http://payture.com/api/api#error-codes_
    FRAUD_ERROR_BIN_LIMIT = 'FRAUD_ERROR_BIN_LIMIT'
    ACCESS_DENIED = 'ACCESS_DENIED'
    AMOUNT_EXCEED = 'AMOUNT_EXCEED'
    CARD_NOT_FOUND = 'CARD_NOT_FOUND'
    FRAUD_ERROR_CRITICAL_CARD = 'FRAUD_ERROR_CRITICAL_CARD'
    ISSUER_CARD_FAIL = 'ISSUER_CARD_FAIL'
    PROCESSING_ACCESS_DENIED = 'PROCESSING_ACCESS_DENIED'
    PROCESSING_ERROR = 'PROCESSING_ERROR'
    PROCESSING_TIME_OUT = 'PROCESSING_TIME_OUT'




