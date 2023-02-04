# coding=utf-8
import string

from btestlib import reporter
from btestlib import utils
from btestlib.constants import Services
from simpleapi.common import logger
from simpleapi.common.oauth import Auth
from simpleapi.common.utils import simple_random as random
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import get_masked_number, get_card, CVN
from simpleapi.data.defaults import TemplateTag
from simpleapi.steps import card_forwarding_api_steps as uber_card_steps
from simpleapi.steps import masterpass_steps as mp_steps
from simpleapi.steps import simple_steps as simple
from simpleapi.steps import trust_steps as trust
from simpleapi.steps import uber_steps as uber
from simpleapi.steps import web_payment_steps as web_steps
from simpleapi.steps import webapi_payment_steps as webapi_steps

__author__ = 'fellow'

log = logger.get_logger()

BASE_IN_BROWSER_PARAM = False


class TYPE(object):
    CARD = 'card'
    YAMONEY = 'yandex_money'
    YAMONEY_WEB_PAGE = 'yandex_money_web_page'
    PHONE = 'phone'
    NO_PAYMETHOD = 'paymethod_is_not_defined'
    COUPON = 'coupon'
    SUBSIDY = 'subsidy'
    BRANDING_SUBSIDY = 'branding_subsidy'
    GUARANTEE_FEE = 'guarantee_fee'
    TRIP_BONUS = 'trip_bonus'
    PERSONNEL_BONUS = 'personnel_bonus'
    DISCOUNT_TAXI = 'discount_taxi'
    SUPPORT_COUPON = 'support_coupon'
    BOOKING_SUBSIDY = 'booking_subsidy'
    CASHRUNNER = 'virtual::cashrunner'
    DRYCLEAN = 'virtual::dryclean'
    COMPENSATION = 'compensation'
    COMPOSITE = 'composite'
    COMPENSATION_DISCOUNT = 'compensation_discount'
    CASH = 'cash'
    PURCHASE_TOKEN = 'purchase_token'
    BONUS_ACCOUNT = 'bonus_account'
    PROMOCODE = 'promocode'
    APPLE_TOKEN = 'apple_token'
    GOOGLE_PAY_TOKEN = 'google_pay_token'
    YAM_3DS = 'yandex_money_3ds'
    PAYSTEP_CARD = 'paystep_card'
    PAYSTEP_LINKED_CARD = 'paystep_linked_card'
    PAYSTEP_PAYPAL = 'paystep_paypal'
    PAYSTEP_WEBMONEY = 'paystep_webmoney'
    VIRTUAL_DEPOSIT = 'virtual::deposit'
    VIRTUAL_DEPOSIT_PAYOUT = 'virtual::deposit_payout'
    VIRTUAL_REFUEL = 'virtual::refuel'
    VIRTUAL_PROMOCODE = 'virtual::new_promocode'
    CERTIFICATE_PROMOCODE = 'virtual::certificate_promocode'
    MARKETING_PROMOCODE = 'virtual::marketing_promocode'
    AFISHA_CERTIFICATE = 'afisha_certificate'
    AFISHA_FAKE_REFUND = 'afisha_fake_refund'
    FAKE_REFUND_CERTIFICATE = 'virtual::fake_refund_cert'
    MARKET_CREDIT = 'sberbank_credit'
    FISCAL_MUSIC = 'fiscal::music'
    CHARITY_FUND = 'charity_fund'
    YANDEX_ACCOUNT_TOPUP = 'yandex_account_topup'
    YANDEX_ACCOUNT_WITHDRAW = 'yandex_account_withdraw'
    REWARD_ACCOUNT_WITHDRAW = 'reward_account_withdraw'
    VIRTUAL_KINOPOISK_SUBS_DISCOUNT = 'virtual::kinopoisk_subs_discounts'
    VIRTUAL_KINOPOISK_CARD_DISCOUNT = 'virtual::kinopoisk_card_discounts'
    VIRTUAL_BNPL = 'wrapper::bnpl'
    CREDIT_CESSION = 'credit::cession'


def get_web_steps(in_browser):
    return web_steps if in_browser else webapi_steps


class BasePaymethod(object):
    def __init__(self):
        self._id = None
        self.title = None
        self.type = None
        self.payment_mode = None

    def pay(self, *args, **kwargs):
        pass

    def init(self, *args, **kwargs):
        pass

    @property
    def id(self):
        if self._id is None:
            raise PaymentMethodNeedToBeInitialized('Payment method {} need to be initialized '
                                                   'via constructor or init method'.format(self))
        return self._id

    @id.setter
    def id(self, value):
        self._id = value

    def __str__(self):
        return "'{}'".format(self.title)

    @property
    def common_type(self):
        # 'cash-39989801' -> 'cash'
        if self.type:
            return self.type.split('-')[0]
        return self.type


class ConstantBasePaymethod(BasePaymethod):
    type = None

    def __init__(self, *args, **kwargs):
        super(ConstantBasePaymethod, self).__init__()
        self._id = self.title = self.type = self._get_type()

    def _get_type(self):
        return self.__class__.type


class InBrowserType(object):
    DEFAULT = 'DEFAULT'  # тесты идут в зависимости от того, какой параметр передан в in_browser
    FULL_WEB = 'FULL_WEB'  # вне зависимости от переданного в in_browser параметра тесты идут через веб
    # FULL_API = 'FULL_API' # вне зависимости от переданного в in_browser параметра тесты идут через апи
    # full_api не реализуемо сейчас, поскольку есть тесты на 3ds и некоторые промокоды, в которых нельзя не идти в веб


def current_in_browser_type():
    import os

    in_browser_type = os.environ.get('IN_BROWSER_TYPE', InBrowserType.DEFAULT)
    return in_browser_type


class PaymentMethodNeedToBeInitialized(Exception):
    pass


class TrustWebPage(BasePaymethod):
    def __init__(self, via, in_browser=BASE_IN_BROWSER_PARAM, template_tag=TemplateTag.desktop):
        def get_in_browser_by_env():
            cur_in_browser = current_in_browser_type()
            if cur_in_browser == InBrowserType.DEFAULT:
                return in_browser
            if cur_in_browser == InBrowserType.FULL_WEB:
                return True
                # if cur_in_browser == InBrowserType.FULL_API:  # it is for future updates. may be
                #     return False

        super(TrustWebPage, self).__init__()
        self.via = via
        self.id = 'trust_web_page'
        self.in_browser = get_in_browser_by_env()
        self.template_tag = template_tag
        self.title = self.id + '_' + self.via.title + '_' + ('in_web' if self.in_browser else 'by_api') + \
                     '_' + 'via_{}'.format(template_tag)
        self.type = via.type
        self.region_id = None
        self.user = None

    @property
    def via_id(self):
        return self.via.id

    def pay(self, *args, **kwargs):
        capabilities = None
        # YA Money iframe doesn't work in firefox
        if isinstance(self.via, Via.YandexMoney):
            capabilities = utils.Web.CustomCapabilities.CHROME()

        with reporter.step(u'Совершаем оплату через страницу trust. '
                           u'Выбранный способ оплаты {}'.format(self.title)):
            if self.in_browser:
                with utils.Web.DriverProvider(capabilities=capabilities) as driver:
                    self.via.pay(driver=driver, template_tag=self.template_tag, in_browser=self.in_browser, *args,
                                 **kwargs)
            else:
                self.via.pay(template_tag=self.template_tag, in_browser=self.in_browser, *args, **kwargs)

    def init(self, service, user, region_id=225, *args, **kwargs):
        with reporter.step(u'Инициализируем способ оплаты {} если это требуется'.format(self.title)):
            self.region_id = region_id
            self.user = user
            self.via.init(service, user)


class YandexMoneyWebPage(ConstantBasePaymethod):
    type = TYPE.YAMONEY_WEB_PAGE

    def pay(self, service, *args, **kwargs):
        with reporter.step(u'Совершаем оплату через прямой переход в интерфейс ЯД'), \
             utils.Web.DriverProvider() as driver:
            web_steps.get_paymethods(service).\
                pay_by_yandex_money_web_page(driver=driver,
                                             *args, **kwargs)


class LinkedCard(BasePaymethod):
    def __init__(self, _id=None, service=None, user=None, card=None,
                 region_id=None, user_ip=None, payment_mode=None, from_linked_phonish=False,
                 list_payment_methods_callback=simple.list_payment_methods):
        super(LinkedCard, self).__init__()
        self._id = _id
        self.card = card
        self.type = TYPE.CARD
        self.title = 'mobile_linked_card' if not from_linked_phonish else 'mobile_linked_card_from_linked_phonish'
        self.list_payment_methods_callback = list_payment_methods_callback
        self.payment_mode = payment_mode
        self.from_linked_phonish = from_linked_phonish
        self.binding_trust_payment_id = None

        if service and user and card and (user.is_mutable()
                                          or user.kinopoisk_user_id or (user in uids.passport_users_for_kp)):
            self._id = simple.find_card_by_masked_number(service=service, user=user,
                                                         number=get_masked_number(self.card['card_number']),
                                                         list_payment_methods_callback=self.list_payment_methods_callback)
            if not self._id:
                trust.process_binding(user if not self.from_linked_phonish else user.linked_users[0],
                                      cards=card, region_id=region_id, user_ip=user_ip)
                self._id = choose_paymethod(TYPE.CARD, service, user,
                                            list_payment_methods_callback=self.list_payment_methods_callback)
        elif service and user:
            self._id = choose_paymethod(TYPE.CARD, service, user,
                                        list_payment_methods_callback=self.list_payment_methods_callback)

    def init(self, service, user, region_id=225, user_ip=None, *args, **kwargs):
        with reporter.step(u'Инициализируем способ оплаты {}'.format(self.title)):
            if self.card is not None and user.is_mutable():
                self._id = simple.find_card_by_masked_number(service=service, user=user,
                                                             number=get_masked_number(self.card['card_number']),
                                                             list_payment_methods_callback=self.list_payment_methods_callback)
                if not self._id:
                    # так сделано, поскольку мы не можем привязывать 3дсную карту, но можем ей платить
                    card_ = self.card.copy()
                    card_['cvn'] = 850 if (self.card['cvn'] == CVN.force_3ds and self.card['cardholder'] == 'TEST TEST') \
                                          or kwargs.get('bind_with_success_cvn', False) is True \
                        else self.card['cvn']
                    _, trust_payment_ids = trust.process_binding(
                        user if not self.from_linked_phonish else user.linked_users[0],
                        service=service, cards=card_, region_id=region_id, user_ip=user_ip)
                    self.binding_trust_payment_id = trust_payment_ids[0]
                    self._id = simple.find_card_by_masked_number(service=service, user=user,
                                                                 number=get_masked_number(self.card['card_number']),
                                                                 list_payment_methods_callback=self.list_payment_methods_callback)
            else:
                self._id = choose_paymethod(TYPE.CARD, service, user,
                                            list_payment_methods_callback=self.list_payment_methods_callback)

    def pay(self, user=None, *args, **kwargs):
        if kwargs.get('cvn'):
            token = trust.get_auth_token(Auth.get_auth(user), user)['access_token']
            trust.supply_payment_data(token,
                                      cvn=kwargs.get('cvn'),
                                      purchase_token=kwargs.get('purchase_token'),
                                      trust_payment_id=kwargs.get('trust_payment_id'))


class UberForwardingCard(BasePaymethod):
    def __init__(self, _id=None, service=None, user=None, card=None, payment_mode=None, pass_cvn=False,
                 list_payment_methods_callback=simple.list_payment_methods, unbind_before=True):
        super(UberForwardingCard, self).__init__()
        self._id = _id
        self.card = card
        self.type = TYPE.CARD
        self.title = 'uber_forwarding_card'
        self.list_payment_methods_callback = list_payment_methods_callback
        self.payment_mode = payment_mode
        self.pass_cvn = pass_cvn
        self.unbind_before = unbind_before

        if service and user and card and user.is_mutable():
            self._id = simple.find_card_by_masked_number(service=service, user=user,
                                                         number=get_masked_number(self.card['card_number']),
                                                         list_payment_methods_callback=self.list_payment_methods_callback)
            if not self._id:
                uber_card_steps.Binding.unbind_all_cards_of(user, service=service)
                uber_card_steps.Binding.create(card=self.card, user=user, pass_cvn=self.pass_cvn)
                self._id = choose_paymethod(TYPE.CARD, service, user,
                                            list_payment_methods_callback=self.list_payment_methods_callback)
        elif service and user:
            self._id = choose_paymethod(TYPE.CARD, service, user,
                                        list_payment_methods_callback=self.list_payment_methods_callback)

    def init(self, service, user, *args, **kwargs):
        with reporter.step(u'Инициализируем способ оплаты {}'.format(self.title)):
            uber_card_steps.Binding.unbind_all_cards_of(user, service=service)
            self._id = uber_card_steps.Binding.create(card=self.card, user=user, pass_cvn=self.pass_cvn)['paymethod_id']


class UberRoamingCard(BasePaymethod):
    def __init__(self, _id=None, service=None, user=None, card=None,
                 list_payment_methods_callback=simple.list_payment_methods,
                 is_valid=True, uber_oauth_token=None):
        super(UberRoamingCard, self).__init__()
        self._id = _id
        self.type = TYPE.CARD
        self.title = 'uber_roaming_card'
        self.list_payment_methods_callback = list_payment_methods_callback
        self.is_valid = is_valid
        self.uber_oauth_token = uber_oauth_token

        if service and user:
            self.uber_oauth_token = uber_oauth_token or uber.Authorization.get_token_for(user)

            if is_valid:
                self._id = simple.find_card_by(service, user, id='valid', proto='uber', type='card',
                                               uber_oauth_token=self.uber_oauth_token)
            else:
                self._id = simple.find_card_by(service, user, id='invalid', proto='uber', type='card',
                                               uber_oauth_token=self.uber_oauth_token)

    def init(self, service, user, *args, **kwargs):
        with reporter.step(u'Инициализируем способ оплаты {}'.format(self.title)):
            if service and user:
                if not self.uber_oauth_token:
                    self.uber_oauth_token = kwargs.get('uber_oauth_token') or \
                                            uber.Authorization.get_token_for(user)
                params = {'id': 'e:uber:valid' if self.is_valid else 'e:uber:invalid',
                          'proto': 'uber',
                          'type': 'card'}
                self._id = simple.find_card_by(service, user, uber_oauth_token=self.uber_oauth_token, **params)

    def pay(self, user=None, *args, **kwargs):
        pass


class Card(BasePaymethod):
    def __init__(self, card=None, payment_mode=None):
        super(Card, self).__init__()
        self.card = card
        self.type = TYPE.CARD
        self.title = 'mobile_card'
        self._id = ''
        self.payment_mode = payment_mode

    def init(self, *args, **kwargs):
        pass

    def pay(self, service=None, user=None, *args, **kwargs):
        token = trust.get_auth_token(Auth.get_auth(user, service=service), user)['access_token'] \
            if user != uids.anonymous else None
        trust.supply_payment_data(token=token,
                                  purchase_token=kwargs.get('purchase_token'),
                                  trust_payment_id=kwargs.get('trust_payment_id'),
                                  payment_method='new_card',
                                  cvn=self.card.get('cvn'),
                                  card_number=self.card.get('card_number'),
                                  cardholder=self.card.get('cardholder'),
                                  expiration_year=self.card.get('expiration_year'),
                                  expiration_month=self.card.get('expiration_month'))


class YandexMoney(BasePaymethod):
    def __init__(self, _id=None, service=None, user=None):
        super(YandexMoney, self).__init__()
        if _id:
            self._id = _id
        elif service and user:
            self._id = choose_paymethod(TYPE.YAMONEY, service, user)
        self.type = self.title = TYPE.YAMONEY

    def init(self, service, user, *args, **kwargs):
        with reporter.step(u'Инициализируем способ оплаты {}'.format(self.title)):
            if not self._id:
                self._id = choose_paymethod(TYPE.YAMONEY, service, user)


class Phone(BasePaymethod):
    def __init__(self, _id=None, service=None, user=None):
        super(Phone, self).__init__()
        if _id:
            self._id = _id
        elif service and user:
            self._id = choose_paymethod(TYPE.PHONE, service, user)
        self.type = self.title = TYPE.PHONE

    def init(self, service, user, *args, **kwargs):
        with reporter.step(u'Инициализируем способ оплаты {}'.format(self.title)):
            if not self._id:
                self._id = choose_paymethod(TYPE.PHONE, service, user)


class Coupon(ConstantBasePaymethod):
    type = TYPE.COUPON


class Subsidy(ConstantBasePaymethod):
    type = TYPE.SUBSIDY


class BrandingSubsidy(ConstantBasePaymethod):
    type = TYPE.BRANDING_SUBSIDY


class GuaranteeFee(ConstantBasePaymethod):
    type = TYPE.GUARANTEE_FEE


class TripBonus(ConstantBasePaymethod):
    type = TYPE.TRIP_BONUS


class PersonnelBonus(ConstantBasePaymethod):
    type = TYPE.PERSONNEL_BONUS


class DiscountTaxi(ConstantBasePaymethod):
    type = TYPE.DISCOUNT_TAXI


class SupportCoupon(ConstantBasePaymethod):
    type = TYPE.SUPPORT_COUPON


class BookingSubsidy(ConstantBasePaymethod):
    type = TYPE.BOOKING_SUBSIDY


class Cashrunner(ConstantBasePaymethod):
    type = TYPE.CASHRUNNER


class Dryclean(ConstantBasePaymethod):
    type = TYPE.DRYCLEAN


class Compensation(ConstantBasePaymethod):
    type = TYPE.COMPENSATION


class MarketCredit(ConstantBasePaymethod):
    type = TYPE.MARKET_CREDIT


class VirtualBnpl(ConstantBasePaymethod):
    type = TYPE.VIRTUAL_BNPL

    @staticmethod
    def delete_virtual_prefix(string):
        prefix = 'wrapper::'
        if string.startswith(prefix):
            return string[len(prefix):]
        return string

    @property
    def common_type(self):
        result = super(VirtualBnpl, self).common_type
        return VirtualBnpl.delete_virtual_prefix(result)


class CreditCession(ConstantBasePaymethod):
    type = TYPE.CREDIT_CESSION


class CompensationDiscount(ConstantBasePaymethod):
    type = TYPE.COMPENSATION_DISCOUNT


class PertnerIdDashPaymethod(BasePaymethod):
    PAYMETHOD_DASH = None

    def __init__(self, partner_id=None):
        super(PertnerIdDashPaymethod, self).__init__()
        if partner_id is None:
            self._id = self.type = self.title = self.PAYMETHOD_DASH
        else:
            self._id = self.type = self.title = (self.PAYMETHOD_DASH + '-' + str(partner_id))


class Cash(PertnerIdDashPaymethod):
    PAYMETHOD_DASH = TYPE.CASH


class CharityFund(PertnerIdDashPaymethod):
    PAYMETHOD_DASH = TYPE.CHARITY_FUND


class PurchaseToken(ConstantBasePaymethod):
    type = TYPE.PURCHASE_TOKEN

    def __init__(self, purchase_token):
        super(PurchaseToken, self).__init__()
        self._id = 'purchase_token-' + purchase_token


class NoPaymethod(ConstantBasePaymethod):
    type = TYPE.NO_PAYMETHOD

    def __init__(self):
        super(NoPaymethod, self).__init__()
        self._id = ''


class BonusAccount(BasePaymethod):
    def __init__(self, _id=None, service=None, user=None):
        super(BonusAccount, self).__init__()
        if _id:
            self._id = _id
        elif service and user:
            self._id = choose_paymethod(TYPE.BONUS_ACCOUNT, service, user)
        self.type = self.title = TYPE.BONUS_ACCOUNT

    def init(self, service, user, *args, **kwargs):
        with reporter.step(u'Инициализируем способ оплаты {}'.format(self.title)):
            if not self._id:
                self._id = choose_paymethod(TYPE.BONUS_ACCOUNT, service, user)


def gen_order_tag(max_size=50, need_minus=False):
    order_tag_size = random.randint(2, max_size)
    if need_minus:
        return ''.join(('-' if i % 4 == 0 and not i == 0 else random.choice(string.ascii_uppercase + string.digits))
                       for i in range(order_tag_size))
    return ''.join(random.choice(string.ascii_uppercase + string.digits) for _ in range(order_tag_size))


class ApplePay(BasePaymethod):
    def __init__(self, bind_token=False, pass_to_supply_payment_data=False):
        super(ApplePay, self).__init__()
        self.bind_token = bind_token
        self.pass_to_supply_payment_data = pass_to_supply_payment_data
        self.type = TYPE.CARD
        self.title = TYPE.APPLE_TOKEN
        self._id = '' if self.pass_to_supply_payment_data else TYPE.APPLE_TOKEN
        #  apple_token живет год, получаем у таксистов TRUST-1936
        self.token = "{\"header\": {\"ephemeralPublicKey\": \"MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEHTrMunWmv+ENr4etPjZF3JBopTrfYkG8neQRFG26Xgn9xsRWdcUtK5bUAj72j7DfaRXDC52bmiwfb+kGu46A4A==\", \"transactionId\": \"391cb17779baa74e3c869c2bf2a67cc6af3edebb5f6260d1d3156de0d57e1f2c\", \"publicKeyHash\": \"LWOlXotuxMhZZd2ttf8eyRAX4Krbs9c8+h9TJeg85Nw=\"}, \"version\": \"EC_v1\", \"data\": \"mi6uKq5Xjcs29DQgh7zWrptYbWImpruHTQ9EMyc7xSKWFgYEtuWDrz/beX7yztDC0Agm5Rp7UbzaDVHuwjiZ3EF2m7/3xOMpMUrn0gdXS0YoeYCRvi/3bnfrgWj5BT6O7b4w8YMg49Ck3PBHWBKR9sOqhSymubtQNOeE0hLqdZTpcNqoC4Jft9iMCYDFWxS1jnv3JnVOxE0f34mSbzr33n3IiBeZKSgbK7fHf4LSw0T2D4Hgcyg/0GF5ZUXQIqI9uMc3PJUhas2LGFODTQvaDqr5TCXTZqOPUEOnQwBE2OvSbmBFd4nf565nBKzdL7LKOyLmJKh5VCyE4OZA6mWfXeOgNFwmSfkhrZ6HMN3KTFnigQ0RIvt+9J9LIxcPiu55KXNnVIydhhsfPA8/fjvP9t1E10mGb0dwk+rAQakALA==\", \"signature\": \"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\"}"

    def init(self, service, user, region_id=225, *args, **kwargs):
        with reporter.step(u'Инициализируем способ оплаты {}'.format(self.title)):
            if self.bind_token:
                token = trust.get_auth_token(Auth.get_auth(user, service), user)['access_token']
                self._id = trust.bind_apple_pay_token(token=token, apple_token=self.token,
                                                      order_tag=gen_order_tag(), region_id=region_id)['payment_method']

    def pay(self, service=None, user=None, purchase_token=None, payment_form=None, payment_url=None, cvn=None,
            trust_payment_id=None, success_3ds_payment=None, break_3ds=None, *args, **kwargs):
        if self.pass_to_supply_payment_data:
            auth_token = trust.get_auth_token(Auth.get_auth(user, service=service), user)['access_token'] \
                if user != uids.anonymous else None
            trust.supply_payment_data(token=auth_token,
                                      purchase_token=purchase_token,
                                      trust_payment_id=trust_payment_id,
                                      apple_token=self.token)


class GooglePay(BasePaymethod):
    def __init__(self, error=None, need_order_tag_minus=False):
        super(GooglePay, self).__init__()
        self.type = TYPE.CARD
        self._id = self.title = TYPE.GOOGLE_PAY_TOKEN
        #  google_pay_token получаем в telegram канале: подключение GPay через Payture для Такси
        self.token = "eyJzaWduYXR1cmUiOiJNRVVDSUFreFNra29RZDA3SHZlY3gyemxCRmpSUmxWYkJMMFNKYmFzb2k4L1NtNk5BaUVBc3E0R1ZiRldkQS9WZzlrYVdmaDIwTzA1QktBMXptaTEvYXJDT3ZTSFFOMFx1MDAzZCIsInByb3RvY29sVmVyc2lvbiI6IkVDdjEiLCJzaWduZWRNZXNzYWdlIjoie1wiZW5jcnlwdGVkTWVzc2FnZVwiOlwibDQvMlRyL1BZZ0Ywa0dpbHpuRmtZTG1ENFFwc2ZkYkdodFZzK0xwUVNPaHhTZmdHVzd2aG9GYVFkbWl5RGVjSkpvT1pHUXNCS1JjanZUYnZ1QjRwSk5GUnNIUkdmQ2hiMXJJb01sdEVMdDZNNkUvSlRkUld5SVp4eFJZZDA4cHc3VUswc1duTmV2Y0NQdGttbVFobWl5ZFRxSTcvTm9OZFNGL0FyUUlkZDVBRHJEdHJoVEZVdW9NNnJoMnp3MXE2c1FXU3pXdmhaUkN1OUZTd25lbUNKVE5oYU1POW9UUnduVlJ5ZDhYejhsbk40MWdNaTZsdGljc0N6cDBmQUZqMklJNVNXcjBiOGsycjE5RVRCT3FKVjd3VE02bW9hOU1jVmpudzQzTzFnVDlsOUxTLzhIMHFmdk9lUTNJTURUaFYyZ3lDUW9Ud3JJRHhJMnF3dWg5OHpLcUhrZzFlR3hEc1IwbzRtWThYcHRJU0NaTERZS2tTYnNxbnRzUGZ2TEZWZEhrVklyTTVLaUJFSFhpRWdvUGtGSnpxaWFnUFd2R04vd21uM0lsR2FQeUE4Vm91YjlUSU5xMW1BNVhML1dpTk5XeVhoTDh6czAzSDc2NFRkak5VRkVKTE9CbFhxVWdkbWhMaURtWmZmMy9lUzhVQzJMRDFpSHUvbGNYcHkrdU8zbUNrQ2R1ZTg2N2lENlpRSytoRXFGUW9qaWo4WkxYMExUYlpjK25GVUV5T2N6TDE2TGtkU25TajlWcDRTbkowZmxOUWc0RGJ5c2F1MDQyOERwQ3JvQkVUYkFcXHUwMDNkXFx1MDAzZFwiLFwiZXBoZW1lcmFsUHVibGljS2V5XCI6XCJCTTZFWmJOSmlwMDk3eEJxRlB0NXMwc2orVVpxRm5jR0I4LzYwZ0RPczZvb2FId2YyU2w0UnFQb1lSenQ0WDBBcVQ2ZE84Y280bFpKem5TakI1YWxZTVlcXHUwMDNkXCIsXCJ0YWdcIjpcIlNmQzhyRGNSUWVZSy9PeWVvNytySER0bS8relV2Z2NEekMyNklBdnU0bklcXHUwMDNkXCJ9In0="
        if error:
            self.token += '_{}'.format(error)
        self.order_tag = gen_order_tag(need_minus=need_order_tag_minus)

    def init(self, service, user, region_id=225, *args, **kwargs):
        with reporter.step(u'Инициализируем способ оплаты {}'.format(self.title)):
            token = trust.get_auth_token(Auth.get_auth(user, service), user)['access_token']
            self._id = trust.bind_google_pay_token(token=token, google_pay_token=self.token,
                                                   order_tag=self.order_tag, region_id=region_id)['payment_method']

    def pay(self, service=None, user=None, purchase_token=None, payment_form=None, payment_url=None, cvn=None,
            trust_payment_id=None, success_3ds_payment=None, break_3ds=None, *args, **kwargs):
        pass


class Via(object):
    class Card(object):
        def __init__(self, card, save_card=False, unbind_before=True):
            self.card = card
            self.title = 'via_card'
            self.type = TYPE.CARD
            self.save_card = save_card
            self.id = 'direct_card'
            self.unbind_before = unbind_before

        def pay(self, service, template_tag, in_browser=BASE_IN_BROWSER_PARAM, *args, **kwargs):
            get_web_steps(in_browser=in_browser). \
                get_paymethods(service, template_tag). \
                pay_by_card(card=self.card,
                            save_card=self.save_card,
                            *args, **kwargs)

        def init(self, service, user, *args, **kwargs):
            if self.unbind_before:
                trust.unbind_all_cards_of(user, service=service)

    class LinkedCard(object):
        def __init__(self, card=None, card_id=None, unbind_before=True,
                     list_payment_methods_callback=simple.list_payment_methods,
                     bind_only_in_masterpass=False, masterpass_bind_to=None,
                     from_linked_phonish=False):
            self.card = card
            self.card_id = card_id
            self.title = 'via_linked_card' if not from_linked_phonish else 'via_linked_card_from_linked_phonish'
            self.type = TYPE.CARD
            self.id = None
            self.unbind_before = unbind_before
            self.list_payment_methods_callback = list_payment_methods_callback
            self.bind_only_in_masterpass = bind_only_in_masterpass
            self.masterpass_bind_to = masterpass_bind_to
            self.phone = masterpass_bind_to if masterpass_bind_to else None
            self.from_linked_phonish = from_linked_phonish

        def pay(self, service, template_tag, in_browser=BASE_IN_BROWSER_PARAM, *args, **kwargs):
            get_web_steps(in_browser=in_browser).\
                get_paymethods(service, template_tag).\
                pay_by_linked_card(
                card_id=self.card_id,
                card=self.card,
                *args, **kwargs)

        def init(self, service, user, region_id=None, user_ip=None, *args, **kwargs):
            if self.unbind_before:
                trust.unbind_all_cards_of(user, service)

            if self.masterpass_bind_to:
                mp_steps.process_binding_to_masterpass(self.masterpass_bind_to, self.card)
                self.id = self.card_id = \
                    simple.get_masterpass_card_from_list_payment_methods(service=service, user=user,
                                                                         phone=self.masterpass_bind_to,
                                                                         masterpass_fingerprint_seed=user.uid,
                                                                         card=self.card)
                if self.bind_only_in_masterpass:
                    return

            if self.card is not None:
                # так сделано, поскольку мы не можем привязывать 3дсную карту, но можем ей платить
                card_ = self.card.copy()
                card_['cvn'] = 850 if self.card['cvn'] == CVN.force_3ds and self.card['cardholder'] == 'TEST TEST' \
                    else self.card['cvn']
                trust.process_binding(user if not self.from_linked_phonish else user.linked_users[0],
                                      cards=card_, region_id=region_id, user_ip=user_ip)

                self.card_id = simple.find_card_by_masked_number(service, user,
                                                                 get_masked_number(self.card['card_number']),
                                                                 list_payment_methods_callback=self.list_payment_methods_callback)
            if self.card_id is None:
                self.card_id = choose_paymethod('card', service, user,
                                                list_payment_methods_callback=self.list_payment_methods_callback)

            self.id = self.card_id

    class YandexMoney(object):
        def __init__(self):
            self.title = 'via_yandex_money'
            self.type = TYPE.YAMONEY
            self.id = None

        def pay(self, service, *args, **kwargs):
            web_steps.get_paymethods(service).\
                pay_by_yandex_money(*args, **kwargs)

        def init(self, *args, **kwargs):
            pass

    class Phone(object):
        def __init__(self):
            self.title = 'via_phone'
            self.type = TYPE.PHONE
            self.phone = None
            self.id = None

        def pay(self, service, *args, **kwargs):
            web_steps.get_paymethods(service).\
                pay_by_phone(self.phone, *args, **kwargs)

        def init(self, service, user, *args, **kwargs):
            self.phone = user.phones[0]

    class Promocode(object):
        def __init__(self, unbind_before=True):
            self.title = 'via_promocode'
            self.type = TYPE.PROMOCODE
            self.unbind_before = unbind_before

        def pay(self, service, template_tag, in_browser=BASE_IN_BROWSER_PARAM, *args, **kwargs):
            get_web_steps(in_browser=in_browser).\
                get_paymethods(service, template_tag).\
                pay_by_only_promocode(*args, **kwargs)

        def init(self, service, user, *args, **kwargs):
            if self.unbind_before:
                trust.unbind_all_cards_of(user, service)

    class Pay3ds(object):
        def __init__(self, card=None):
            self.title = 'via_3ds'
            self.type = TYPE.YAM_3DS
            self.id = None
            self.card = card

        def pay(self, service, *args, **kwargs):
            web_steps.get_paymethods(service).\
                pay_by_3ds(self.card, *args, **kwargs)

        def init(self, *args, **kwargs):
            pass

    @classmethod
    def card(cls, card, save_card=False, unbind_before=True):
        return cls.Card(card, save_card=save_card, unbind_before=unbind_before)

    @classmethod
    def linked_card(cls, card=None, card_id=None, unbind_before=True,
                    list_payment_methods_callback=simple.list_payment_methods,
                    bind_only_in_masterpass=None,
                    masterpass_bind_to=None, from_linked_phonish=False):
        return cls.LinkedCard(card, card_id, unbind_before=unbind_before,
                              list_payment_methods_callback=list_payment_methods_callback,
                              bind_only_in_masterpass=bind_only_in_masterpass,
                              masterpass_bind_to=masterpass_bind_to,
                              from_linked_phonish=from_linked_phonish)

    @classmethod
    def yandex_money(cls):
        return cls.YandexMoney()

    @classmethod
    # slppls-TODO: this method and all of webphone need to rework
    def phone(cls):
        return cls.Phone()


class NoAvailablePayMethod(Exception):
    pass


def choose_paymethod(paymethod, service, user, region_id=225,
                     list_payment_methods_callback=simple.list_payment_methods):
    with reporter.step(u'Выбираем способ оплаты {} для пользователя {}'.format(paymethod, user)):
        _, available_methods = list_payment_methods_callback(service, user)

        chosen_paymethod = None
        for method_name, method_info in available_methods.items():
            if method_name == paymethod:
                chosen_paymethod = method_name
            elif method_name.startswith(paymethod) \
                    and (paymethod == 'card'
                         or str(region_id) == method_info['region_id']):
                chosen_paymethod = method_name
        if not chosen_paymethod:
            raise NoAvailablePayMethod(
                u'no available pay method by name {} and region {}, available methods: {}'.format
                (paymethod, region_id, available_methods))

        reporter.attach(u'Выбранный способ оплаты', chosen_paymethod)

    return chosen_paymethod


def get_common_paymethod_for_service(service):
    """
    Возвращает 'дефолтный' (наиболее популярный) способ оплаты для сервиса
    Можно посмотреть запросом:

    SELECT
    SERVICE_ID,
    PAYMENT_TYPE,
    count(*)
    FROM V_PAYMENT_TRUST
    WHERE DT < to_date('дата_последней_переналивки')
    GROUP BY SERVICE_ID, PAYMENT_TYPE
    ORDER BY SERVICE_ID;

    Не используем способы оплат типа phone или yandex_money,
    т.к. они требуют специального пользователя

    """
    if service in [Services.TICKETS, Services.MARKETPLACE, Services.DISK, Services.NEW_MARKET]:
        paymethod = TrustWebPage(Via.card(card=get_card()))
    else:
        paymethod = LinkedCard(card=get_card())

    reporter.attach(u'Примечание', u'Выбран способ оплаты {} для сервиса {}'.format(paymethod, service))

    return paymethod


class PaystepCard(ConstantBasePaymethod):
    type = TYPE.PAYSTEP_CARD

    def __init__(self, processing):
        super(PaystepCard, self).__init__()
        self.processing = processing

    def pay(self, service=None, *args, **kwargs):
        with reporter.step(u'Совершаем оплату через пейстеп >> картой'), \
             utils.Web.DriverProvider() as driver:
            web_steps.paymethods_by_professional_services.get(service).pay_by_card(driver=driver,
                                                                                   processing=self.processing,
                                                                                   service=service,
                                                                                   *args, **kwargs)


class PaystepLinkedCard(BasePaymethod):
    def __init__(self, processing, unbind_before=True):
        super(PaystepLinkedCard, self).__init__()
        self.title = self.type = TYPE.PAYSTEP_LINKED_CARD
        self.processing = processing
        self.unbind_before = unbind_before
        self.card_id = None

    def pay(self, service=None, *args, **kwargs):
        with reporter.step(u'Совершаем оплату через пейстеп >> привязанной картой'), \
             utils.Web.DriverProvider() as driver:
            web_steps.paymethods_by_professional_services.get(service).pay_by_linked_card(driver=driver,
                                                                                          processing=self.processing,
                                                                                          service=service,
                                                                                          card_id=self.id,
                                                                                          *args, **kwargs)

    def init(self, service, user, card, region_id=None, user_ip=None, *args, **kwargs):
        if self.unbind_before:
            trust.unbind_all_cards_of(user, service)

        # так сделано, поскольку мы не можем привязывать 3дсную карту, но можем ей платить
        card_ = card.copy()
        card_['cvn'] = 850 if card['cvn'] == CVN.force_3ds and card['cardholder'] == 'TEST TEST' \
            else card['cvn']
        linked_cards, _ = trust.process_binding(user, cards=card_, region_id=region_id, user_ip=user_ip)
        self.id = self.card_id = linked_cards[0]


class PaystepPayPal(ConstantBasePaymethod):
    type = TYPE.PAYSTEP_PAYPAL

    def __init__(self, processing=None):
        super(PaystepPayPal, self).__init__()
        self.processing = processing

    def pay(self, service=None, *args, **kwargs):
        with reporter.step(u'Совершаем оплату через пейстеп >> paypal'), \
             utils.Web.DriverProvider() as driver:
            web_steps.paymethods_by_professional_services.get(service).pay_by_paypal(driver=driver,
                                                                                     processing=self.processing,
                                                                                     *args, **kwargs)


class PaystepWebMoney(ConstantBasePaymethod):
    type = TYPE.PAYSTEP_WEBMONEY

    def __init__(self, processing=None):
        super(PaystepWebMoney, self).__init__()
        self.processing = processing

    def pay(self, service=None, *args, **kwargs):
        with reporter.step(u'Совершаем оплату через пейстеп >> webmoney'), \
             utils.Web.DriverProvider() as driver:
            web_steps.paymethods_by_professional_services.get(service).pay_by_webmoney(driver=driver,
                                                                                       processing=self.processing,
                                                                                       *args, **kwargs)


class VirtualDeposit(ConstantBasePaymethod):
    type = TYPE.VIRTUAL_DEPOSIT


class VirtualDepositPayout(ConstantBasePaymethod):
    type = TYPE.VIRTUAL_DEPOSIT_PAYOUT


class VirtualRefuel(ConstantBasePaymethod):
    type = TYPE.VIRTUAL_REFUEL


class VirtualPromocode(ConstantBasePaymethod):
    type = TYPE.VIRTUAL_PROMOCODE


class CertificatePromocode(ConstantBasePaymethod):
    type = TYPE.CERTIFICATE_PROMOCODE


class MarketingPromocode(ConstantBasePaymethod):
    type = TYPE.MARKETING_PROMOCODE


class AfishaFakeRefund(ConstantBasePaymethod):
    type = TYPE.AFISHA_FAKE_REFUND


class FakeRefundCertificate(ConstantBasePaymethod):
    type = TYPE.FAKE_REFUND_CERTIFICATE


class AfishaCertificate(ConstantBasePaymethod):
    type = TYPE.AFISHA_CERTIFICATE


class YandexAccountTopup(ConstantBasePaymethod):
    type = TYPE.YANDEX_ACCOUNT_TOPUP

    def __init__(self, id_):
        super(YandexAccountTopup, self).__init__()
        self._id = id_


class YandexAccountWithdraw(ConstantBasePaymethod):
    type = TYPE.YANDEX_ACCOUNT_WITHDRAW

    def __init__(self, id_):
        super(YandexAccountWithdraw, self).__init__()
        self._id = id_


class RewardAccountWithdraw(ConstantBasePaymethod):
    type = TYPE.REWARD_ACCOUNT_WITHDRAW

    def __init__(self, id_):
        super(RewardAccountWithdraw, self).__init__()
        self._id = id_


class FiscalMusic(ConstantBasePaymethod):
    type = TYPE.FISCAL_MUSIC


class Composite(ConstantBasePaymethod):
    type = TYPE.COMPOSITE
