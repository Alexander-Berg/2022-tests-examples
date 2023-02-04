# coding=utf-8

import pytest
from hamcrest import is_, equal_to, has_entry, has_entries

import btestlib.reporter as reporter
import simpleapi.data.defaults as defaults
from btestlib.constants import Services
from btestlib.utils import CheckMode, check_mode
from simpleapi.common.oauth import Auth
from simpleapi.common.payment_methods import LinkedCard
from simpleapi.common.utils import DataObject
from simpleapi.data import cards_pool as cards
from simpleapi.data import features, stories, marks
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import FRAUD_CARD
from simpleapi.matchers.deep_equals import deep_contains
from simpleapi.steps import check_steps as check
from simpleapi.steps import expected_steps as expected
from simpleapi.steps import mongo_steps as mongo
from simpleapi.steps import simple_steps as simple
from simpleapi.steps import trust_steps as trust

__author__ = 'slppls'


def ids_service_enabled_pay_methods(val):
    return 'Service={}'.format(val[0])


def ids_limit(val):
    return "with limit" if val else "without limit"


def ids_end_dt(val):
    return "with end_dt" if val else "without end_dt"


def ids_limit_qty(val):
    return "limit {},".format(val.limit) + " qty {}".format(val.quantity)


class Data(object):
    test_services = [[Services.TICKETS],
                     [Services.TICKETS,
                      Services.EVENTS_TICKETS,
                      Services.EVENTS_TICKETS_NEW]]
    test_limit = [None,
                  5]
    test_end_dt = [None,
                   simple.prepare_dt_with_shift(3)]
    test_limit_pack = [
        DataObject().new(limit=10, quantity=20),  # promocode quantity more then series limit
        DataObject().new(limit='', quantity=101),  # promocode quantity more then 100
        DataObject().new(limit=10, quantity=101)]  # promocode quantity more then 100 and series limit
    test_promo_status = [defaults.Promocode.Status.active,
                         defaults.Promocode.Status.expired,
                         defaults.Promocode.Status.not_started]
    developer_payload_data = [
        DataObject(paymethod=LinkedCard(card=cards.get_card()),
                   user_type=uids.Types.random_from_all, service=Services.MARKETPLACE).new(
                   developer_payload='{some_developer_payload: ""1!@#$%^&*()>/|\<,{}\'}'),
    ]
    cards = [
        cards.get_card(),
    ]

    regions = [
        '225',
        # '84'
    ]

    users = [
        # uids.Types.random_from_mutable,
        uids.Types.random_from_phonishes,
        # uids.Types.random_autoremove,
    ]
    test_data_mapping = [
        DataObject(country_data=defaults.CountryData.Russia).new(expected_sum='1.00'),
        DataObject(country_data=defaults.CountryData.CountryWithoutCurrency).new(expected_sum='1.00'),
        DataObject(country_data=defaults.CountryData.Armenia).new(expected_sum='20.00'),
        DataObject(country_data=defaults.CountryData.Georgia).new(expected_sum='0.20'),
        DataObject(country_data=defaults.CountryData.Kazakhstan).new(expected_sum='10.00'),
        DataObject(country_data=defaults.CountryData.Moldova).new(expected_sum='1.00'),
        DataObject(country_data=defaults.CountryData.Latvia).new(expected_sum='0.10'),
    ]


@reporter.feature(features.Methods.CheckCard)
class TestXMLRPCCheckCard(object):
    pytestmark = marks.ym_h2h_processing
    '''
    https://st.yandex-team.ru/TRUST-1537
    '''

    @reporter.story(stories.Methods.Call)
    @pytest.mark.parametrize('card', Data.cards, ids=DataObject.ids_card)
    @pytest.mark.parametrize('region_id', Data.regions, ids=DataObject.ids_region_id)
    @pytest.mark.parametrize('user_type', Data.users, ids=DataObject.ids_user_type)
    def test_basic_logic(self, card, region_id, user_type):
        user = uids.get_random_of_type(user_type)
        token = trust.get_auth_token(Auth.get_auth(user), user)['access_token']
        trust.process_unbinding(user=user)
        bind_resp = trust.bind_card(token, card, region_id=region_id)
        card_id = bind_resp['payment_method'][5::]  # card-x****, but need only x****
        with check_mode(CheckMode.FAILED):
            resp = simple.check_card(Services.STORE, user.uid, card_id, region_id=region_id)
        mongo.Refund.wait_until_done(resp['trust_payment_id'])

    @pytest.fixture()
    def link_card_and_block_it(self, request):
        def blocked_card(card, user, region_id, reason):
            with reporter.step('Привязываем карту и блокируем ee, blocking_reason={}'.format(reason)):
                token = trust.get_auth_token(Auth.get_auth(user), user)['access_token']
                trust.process_unbinding(user)
                resp = trust.bind_card(token, card, region_id=region_id)
                mongo.Card.update_data(resp['trust_payment_id'],
                                       data_to_update={'blocking_reason': reason})

            def fin():
                with reporter.step('Убираем у карты blocking_reason в конце теста'):
                    mongo.Card.update_data(resp['trust_payment_id'],
                                           data_to_update={'blocking_reason': None},
                                           wait=False)

            request.addfinalizer(fin)
            return resp['payment_method'][5::]  # card-x****, but need only x****

        return blocked_card

    @reporter.story(stories.Methods.SpecialRules)
    @pytest.mark.parametrize('card', Data.cards, ids=DataObject.ids_card)
    @pytest.mark.parametrize('region_id', Data.regions, ids=DataObject.ids_region_id)
    def test_blocking_reason(self, card, region_id, link_card_and_block_it):
        user = uids.get_random_of(uids.mutable)
        reason = 'Some test reason'
        card_id = link_card_and_block_it(card, user, region_id, reason)
        with check_mode(CheckMode.IGNORED):
            resp = simple.check_card(Services.STORE, user.uid, card_id, region_id=region_id)
        check.check_that(resp, has_entries({'status': 'error',
                                            'status_code': 'authorization_reject',
                                            'status_desc': reason}))

    @reporter.story(stories.Methods.Call)
    @pytest.mark.parametrize('test_data', Data.test_data_mapping, ids=DataObject.ids_region_id)
    def test_currency_mapping_check_card(self, test_data):
        user = uids.get_random_of(uids.mutable)
        trust.process_unbinding(user=user)
        card = cards.get_card()
        country, expected_sum = test_data.country_data, test_data.expected_sum
        reference_mapping_value = {
            'currency': country['currency'],
            'sum': expected_sum
        }
        token = trust.get_auth_token(Auth.get_auth(user), user)['access_token']
        # card-x****, but need only x****
        card_id = trust.bind_card(token, card, region_id='225')['payment_method'][5::]
        # Если привязка карты отвалится по какой-либо причине, вероятнее всего мы все равно сможем посмотреть
        # валюту и сумму платежа в MongoDB.
        # Это не самое надежное решение :(
        with check_mode(CheckMode.IGNORED):
            resp = simple.check_card(Services.TAXI, user.uid, card_id, region_id=country['region_id'])
        trust_payment_id = resp['trust_payment_id']
        payment_info = mongo.Payment.find_by(trust_payment_id)
        check.check_that(reference_mapping_value, has_entries({'currency': payment_info['currency'],
                                                               'sum': str(payment_info['sum'])}),
                         step=u'Проверяем, что валюта и сумма списания выбраны верно',
                         error=u'Валюта и сумма списания выбраны неверно')


@reporter.feature(features.Methods.ListPaymentMethods)
class TestXMLRPCListPaymentMethods(object):
    pytestmark = marks.simple_internal_logic

    @pytest.mark.skipif(True, reason="hardcode card does not work, need meta.T_BINBASE_MARKUP")
    @reporter.story(stories.Methods.Call)
    def test_fraud_card(self):
        user = uids.get_random_of(uids.test_passport)
        token = trust.get_auth_token(Auth.get_auth(user), user)['access_token']
        card_id = trust.bind_card(token, FRAUD_CARD)['payment_method']
        with check_mode(CheckMode.FAILED):
            resp = simple.list_payment_methods(service=Services.STORE, user=user)[0]['payment_methods']
            check.check_that(resp[card_id], has_entry('possible_moneyless_card', 1),
                             step=u'Проверяем что для фродовой карты присутствует параметр possible_moneyless_card',
                             error=u'Для фродовой карты отсутствует параметр possible_moneyless_card')

            # тест не актуален после того как перешли на тестирование процессингов в отдельном сервисе
            # @reporter.story(stories.Methods.Call)
            # @pytest.mark.parametrize('test_data', Data.services_enabled_payment_methods, ids=ids_service_enabled_pay_methods)
            # def test_enabled_payment_methods(self, test_data):
            #     service, expected_enabled_payment_methods = test_data
            #     user = uids.get_random_of(uids.test_passport)
            #     check.check_that(simple.list_payment_methods(service=service, user=user)[0]['enabled_payment_methods'],
            #                      contains_dicts_equal_to(expected_enabled_payment_methods, same_length=False),
            #                      step=u'Проверяем что список enabled_payment_methods корректен',
            #                      error=u'Список enabled_payment_methods в ответе метода ListPaymentMethods некорректен')


@reporter.feature(features.Methods.CreatePromoseries)
class TestXMLRPCCreatePromoseries(object):
    pytestmark = marks.simple_internal_logic
    '''
    https://st.yandex-team.ru/TRUST-1947
    '''

    @pytest.mark.parametrize('services', Data.test_services, ids=DataObject.ids_service)
    @pytest.mark.parametrize('limit', Data.test_limit, ids=ids_limit)
    def test_basic_logic(self, services, limit):
        with check_mode(CheckMode.FAILED):
            # Проверка, что со всеми основными комбинациями параметров создается промосерия
            series_id = simple.create_promoseries(Services.TICKETS, name='autotest_series', amount=100,
                                                  begin_dt=simple.prepare_dt_with_shift(2),
                                                  services=services, limit=limit)['series']['id']
            simple.get_promoseries_status(Services.TICKETS, series_id)

    def test_not_unique_promo_without_partial_only(self):
        with check_mode(CheckMode.IGNORED):
            resp = simple.create_promoseries(Services.TICKETS, name='autotest_series', amount=100,
                                             begin_dt=simple.prepare_dt_with_shift(2), partial_only=0, usage_limit=5)
            check.check_that(resp, deep_contains(expected.Promocode.usage_limit_without_partial_only()))


@reporter.feature(features.Methods.CreatePromocode)
class TestXMLRPCCreatePromocode(object):
    pytestmark = marks.simple_internal_logic
    '''
    https://st.yandex-team.ru/TRUST-1947
    '''
    service = Services.TICKETS

    @pytest.fixture()
    def use_base_promoseries(self):
        def create_promoseries(service):
            with reporter.step('Создаем базовую промосерию'):
                return simple.create_promoseries(service, name='autotest_series', amount=100,
                                                 begin_dt=simple.prepare_dt_with_shift(2))['series']['id']

        return create_promoseries

    def test_basic_logic(self, use_base_promoseries):
        series_id = use_base_promoseries(self.service)
        with check_mode(CheckMode.FAILED):
            simple.create_promocode(self.service, series_id, amount=5)

    def test_unique_promocode(self, use_base_promoseries):
        # Проверка на уникальность промокода. Проще всего делается, если код устанавливается самостоятельно
        # Заодно проверяется, работает ли установка текста промокода руками
        series_id = use_base_promoseries(self.service)
        promocode = 'iamtestpromocode{}'.format(series_id)
        with check_mode(CheckMode.FAILED):
            simple.create_promocode(self.service, series_id, amount=100, quantity=1,
                                    code=promocode)
        with check_mode(CheckMode.IGNORED):
            resp = simple.create_promocode(self.service, series_id, amount=100, quantity=1,
                                           code=promocode)
        check.check_that(resp, deep_contains(expected.Promocode.is_not_unique(promocode)))

    @pytest.mark.parametrize('test_end_dt', Data.test_end_dt, ids=ids_end_dt)
    def test_dt_from_promoseries_to_promocode(self, test_end_dt):
        # При передаче begin_dt и/или end_dt в промосерию они также должны проставляться в промокоды
        # Проверка всех вариантов такой передачи.
        begin_dt = simple.prepare_dt_with_shift(2)
        with check_mode(CheckMode.FAILED):
            series_id = simple.create_promoseries(self.service, name='autotest_series', amount=100,
                                                  begin_dt=begin_dt, end_dt=test_end_dt)['series']['id']
            promocode = simple.create_promocode(self.service, series_id, amount=100, quantity=1)['promocodes'][0]
            resp = simple.get_promocode_status(self.service, promocode)['result']
            expected_resp = {'begin_dt': begin_dt,
                             'end_dt': test_end_dt}

        check.check_dicts_equals(resp, expected_resp,
                                 compare_only=['begin_dt', 'end_dt'])

    @pytest.mark.parametrize('test_limit_pack', Data.test_limit_pack, ids=ids_limit_qty)
    def test_promoseries_quantity_less_promocodes(self, test_limit_pack):
        # Количество промокодов не должно превышать предел промокодов в промосерии
        # Промокодов одновременно нельзя создать более 100
        limit, quantity = test_limit_pack.limit, test_limit_pack.quantity
        with check_mode(CheckMode.FAILED):
            series_id = simple.create_promoseries(self.service, name='autotest_series', amount=100,
                                                  begin_dt=simple.prepare_dt_with_shift(2), limit=limit)['series']['id']
        with check_mode(CheckMode.IGNORED):
            resp = simple.create_promocode(self.service, series_id, amount=100, quantity=quantity)
        check.check_that(resp, deep_contains(expected.Promocode.quantity_and_limit_error()))

    @pytest.mark.parametrize('status', Data.test_promo_status)
    def test_promocode_status(self, status):
        _, promocode, _ = simple.process_promocode_creating(self.service, promo_status=status, extended_response=True)
        actual = simple.get_promocode_status(self.service, promocode)['result']['status']
        check.check_that(actual, is_(equal_to(status)))


@reporter.feature(features.Methods.CheckPayment)
class TestXMLRPCCheckPayment(object):
    @pytest.mark.parametrize('test_data', Data.developer_payload_data, ids=DataObject.ids)
    def test_payment_with_developer_payload(self, test_data):
        paymethod, user_type, service, developer_payload = test_data.paymethod, \
                                                           test_data.user_type, \
                                                           test_data.service, \
                                                           test_data.developer_payload
        user = uids.get_random_of_type(user_type)
        paymethod.init(service, user)
        with check_mode(CheckMode.FAILED):
            orders = simple.form_orders_for_create(service, user)
            purchase_token = simple.create_basket(service, user=user,
                                                  paymethod_id=paymethod.id,
                                                  orders=orders,
                                                  developer_payload=developer_payload)['purchase_token']
            payment_start = simple.pay_basket(service, user=user,
                                              purchase_token=purchase_token)
            check.check_that(payment_start.get('developer_payload'),
                             is_(equal_to(developer_payload)),
                             step=u'Проверяем, что при инициализации оплаты отобразился верный developer_payload',
                             error=u'При инициализации оплаты поле developer_payload отображается некорректно!')
            trust.pay_by(paymethod, service, user=user, payment_form=payment_start.get('payment_form'),
                         purchase_token=purchase_token)
            check_payment_dp = simple.wait_until_payment_done(service, user=user,
                                                              purchase_token=purchase_token).get('developer_payload')
        check.check_that(check_payment_dp, is_(equal_to(developer_payload)),
                         step=u'Проверяем, что при проверке карзины отобразился верный developer_payload',
                         error=u'При проверке корзины поле developer_payload отображается некорректно!')


if __name__ == '__main__':
    pytest.main()
