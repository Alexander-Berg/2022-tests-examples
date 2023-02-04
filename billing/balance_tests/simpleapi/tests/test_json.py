# coding=utf-8
import pytest
from hamcrest import is_, equal_to, has_items, is_not, has_key, has_entries
import datetime
import btestlib.reporter as reporter
import btestlib.utils as utils
import simpleapi.data.defaults as defaults
from btestlib.constants import Services
from btestlib.utils import CheckMode, check_mode
from simpleapi.common.oauth import Auth
from simpleapi.common.payment_methods import TrustWebPage, Via, get_common_paymethod_for_service, LinkedCard, \
    ApplePay, GooglePay
from simpleapi.common.utils import DataObject
from simpleapi.data import features, stories
from simpleapi.data import marks
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import get_card, CVN, get_card_with_separator
from simpleapi.steps import check_steps as check
from simpleapi.steps import db_steps
from simpleapi.steps import mongo_steps as mongo
from simpleapi.steps import simple_steps as simple
from simpleapi.steps import trust_steps as trust
from simpleapi.steps import expected_steps as expected
from simpleapi.steps import payments_api_steps as payments_api
__author__ = 'slppls'


class Data(object):
    ym_cards = [
        get_card(),
        get_card_with_separator(get_card(), ' '),
        get_card_with_separator(get_card(), '\t'),
    ]
    w1_cards = [
        # Bank hate microtransaction, waiting until agreed
        # cards_pool.get_prepared_random_card(),
        # cards_pool.get_card_with_separator(cards_pool.get_prepared_random_card(), ' '),
        # cards_pool.get_card_with_separator(cards_pool.get_prepared_random_card(), '\t')
    ]

    ym_currencies = [
        marks.ym_h2h_processing(None),
        marks.ym_h2h_processing('RUB'),
        pytest.mark.skipif(True, reason="No USD for taxi lol")(marks.ym_h2h_processing('USD'))
    ]
    w1_currencies = [
        'UAH',
        'AMD',
        'KZT',
        'GEL'
    ]

    data = [
        marks.apple_pay(DataObject(region_id=None, paymethod=ApplePay(bind_token=True)).new(should_be_in_lpm=True)),
        marks.apple_pay(DataObject(region_id=159, paymethod=ApplePay(bind_token=True)).new(should_be_in_lpm=False)),
        # marks.apple_pay(DataObject(region_id=21534, paymethod=ApplePay(bind_token=True)).new(should_be_in_lpm=True))
    ]

    google_pay_failure_bindings = [
        DataObject(paymethod=GooglePay(error=defaults.GooglePayErrors.ACCESS_DENIED),
                   user_type=uids.Types.random_from_test_passport,
                   error=expected.BindGooglePayToken.access_denied()),
        DataObject(paymethod=GooglePay(error=defaults.GooglePayErrors.AMOUNT_EXCEED),
                   user_type=uids.Types.random_from_test_passport,
                   error=expected.BindGooglePayToken.amount_exceeded()),
        DataObject(paymethod=GooglePay(error=defaults.GooglePayErrors.CARD_NOT_FOUND),
                   user_type=uids.Types.random_from_test_passport,
                   error=expected.BindGooglePayToken.card_not_found()),
        DataObject(paymethod=GooglePay(error=defaults.GooglePayErrors.FRAUD_ERROR_BIN_LIMIT),
                   user_type=uids.Types.random_from_test_passport,
                   error=expected.BindGooglePayToken.fraud_error_bin_limit()),
        DataObject(paymethod=GooglePay(error=defaults.GooglePayErrors.ISSUER_CARD_FAIL),
                   user_type=uids.Types.random_from_test_passport,
                   error=expected.BindGooglePayToken.issuer_card_fail()),
        DataObject(paymethod=GooglePay(error=defaults.GooglePayErrors.FRAUD_ERROR_CRITICAL_CARD),
                   user_type=uids.Types.random_from_test_passport,
                   error=expected.BindGooglePayToken.fraud_error_critical_card()),
        DataObject(paymethod=GooglePay(error=defaults.GooglePayErrors.PROCESSING_ACCESS_DENIED),
                   user_type=uids.Types.random_from_test_passport,
                   error=expected.BindGooglePayToken.processing_access_denied()),
        DataObject(paymethod=GooglePay(error=defaults.GooglePayErrors.PROCESSING_ERROR),
                   user_type=uids.Types.random_from_test_passport,
                   error=expected.BindGooglePayToken.processing_error()),
        DataObject(paymethod=GooglePay(error=defaults.GooglePayErrors.PROCESSING_TIME_OUT),
                   user_type=uids.Types.random_from_test_passport,
                   error=expected.BindGooglePayToken.processing_time_out()),
    ]
    developer_payload_data = [
        DataObject(paymethod=LinkedCard(card=get_card()),
                   user_type=uids.Types.random_from_all).new(
                   developer_payload='{some_developer_payload: ""1!@#$%^&*()>/|\<,{}\'}'),
    ]


def ids_card(val):
    return val.get('descr')


def ids_currency(val):
    if val is None:
        return 'No currency'
    return val


def check_is_paymethod_in_lpm(resp, paymethod_id, should_be_in_lpm):
    check.check_that(resp, has_key(paymethod_id) if should_be_in_lpm else is_not(has_key(paymethod_id)),
                     step=u'Проверяем что для выбранного региона правильно отображаются способы оплаты',
                     error=u'Способы оплаты для выбранного региона отображаются неправильно!')


@reporter.feature(features.Methods.ListPaymentMethods)
class TestListPaymentMethods(object):
    pytestmark = marks.simple_internal_logic
    """
    https://st.yandex-team.ru/TRUST-1575
    """

    @reporter.story(stories.Methods.Call)
    def test_basic_logic(self):
        user = uids.get_random_of(uids.test_passport)
        token = trust.get_auth_token(Auth.get_auth(user), user)['access_token']
        with check_mode(CheckMode.FAILED):
            trust.list_payment_methods(token)

    @reporter.story(stories.Methods.Call)
    @pytest.mark.parametrize('data', Data.data, ids=DataObject.ids)
    def test_lpm_region_id(self, data):
        user = uids.get_random_of(uids.test_passport)
        paymethod, should_be_in_lpm, region_id = data.paymethod, data.should_be_in_lpm, data.region_id
        paymethod.init(service=Services.STORE, user=user)
        token = trust.get_auth_token(Auth.get_auth(user), user)['access_token']
        with check_mode(CheckMode.FAILED):
            resp = trust.list_payment_methods(token=token, region_id=region_id, show_all=True)['payment_methods']
            check_is_paymethod_in_lpm(resp, paymethod.id, should_be_in_lpm)


@reporter.feature(features.Methods.SupplyPaymentData)
class TestSupplyPaymentData(object):
    """
    https://st.yandex-team.ru/TRUST-988
    https://st.yandex-team.ru/TRUST-1614
    """

    @pytest.fixture
    def create_and_pay_basket(self):
        def create_and_pay_basket_for_service(service):
            with reporter.step(u'Создаем и оплачиваем корзину'):
                user = uids.get_random_of(uids.all_)
                card = get_card()
                paymethod = LinkedCard(card=card)
                paymethod.init(service, user)
                order = simple.create_service_product_and_order(service=service,
                                                                user=user)['service_order_id']
                basket = simple.create_basket(service, user=user, paymethod_id=paymethod.id,
                                              orders=[{'service_order_id': order, 'price': defaults.product_price}],
                                              wait_for_cvn=1)

                simple.pay_basket(service, user=user, purchase_token=basket['purchase_token'])

                return user, basket, paymethod.id, card

        return create_and_pay_basket_for_service

    @marks.ym_h2h_processing
    @reporter.story(stories.Methods.Call)
    def test_with_purchase_token(self, create_and_pay_basket):
        service = Services.MARKETPLACE
        user, basket, paymethod_id, _ = create_and_pay_basket(service)

        token = trust.get_auth_token(Auth.get_auth(user, service=service), user)['access_token']
        with check_mode(CheckMode.FAILED):
            trust.supply_payment_data(token, cvn=CVN.base_success, purchase_token=basket['purchase_token'],
                                      payment_method=paymethod_id)

        with reporter.step(u'Проверяем, что cvn пробросился и платеж проходит'):
            simple.wait_until_payment_done(service=service, user=user,
                                           purchase_token=basket['purchase_token'])

    @marks.ym_h2h_processing
    @reporter.story(stories.Methods.Call)
    def test_with_trust_payment_id(self, create_and_pay_basket):
        service = Services.MARKETPLACE
        user, basket, paymethod_id, _ = create_and_pay_basket(service)

        token = trust.get_auth_token(Auth.get_auth(user, service=service), user)['access_token']
        with check_mode(CheckMode.FAILED):
            trust.supply_payment_data(token, cvn=CVN.base_success, trust_payment_id=basket['trust_payment_id'],
                                      payment_method=paymethod_id)

        with reporter.step(u'Проверяем, что cvn пробросился и платеж проходит'):
            simple.wait_until_payment_done(service=service, user=user,
                                           purchase_token=basket['purchase_token'])

    @marks.ym_h2h_processing
    @pytest.fixture
    def create_and_half_pay_basket(self):
        def create_and_half_pay_basket_for_service(service):
            with reporter.step(u'Создаем корзину и вызываем для нее PayBasket (без непосредственной оплаты)'):
                user = uids.get_random_of(uids.all_)
                order = simple.create_service_product_and_order(service=service,
                                                                user=user)['service_order_id']

                paymethod = get_common_paymethod_for_service(service)
                paymethod.init(service, user)

                basket = simple.create_basket(service, user=user, paymethod_id=paymethod.id,
                                              orders=[{'service_order_id': order, 'price': defaults.product_price}])

                simple.pay_basket(service, user=user, purchase_token=basket['purchase_token'])

                return user, basket

        return create_and_half_pay_basket_for_service

    @marks.ym_h2h_processing
    @reporter.story(stories.Methods.Call)
    def test_with_card_data(self, create_and_half_pay_basket):
        """
        https://st.yandex-team.ru/TRUST-1614
        Для Маркетплейса добавлена возможность проброса полных карточных данных методом supply_payment_data
        """
        service = Services.MARKETPLACE
        user, basket = create_and_half_pay_basket(service)

        card = get_card()

        token = trust.get_auth_token(Auth.get_auth(user, service=service), user)['access_token']
        with check_mode(CheckMode.FAILED):
            trust.supply_payment_data(token, purchase_token=basket['purchase_token'],
                                      payment_method='new_card', cvn=card.get('cvn'),
                                      card_number=card.get('card_number'), cardholder=card.get('cardholder'),
                                      expiration_year=card.get('expiration_year'),
                                      expiration_month=card.get('expiration_month'))

        with reporter.step(u'Проверяем, что после проброса карточных данных платеж проходит'):
            simple.wait_until_payment_done(service=service, user=user,
                                           purchase_token=basket['purchase_token'])

    @marks.ym_h2h_processing
    @reporter.story(stories.Methods.Call)
    def test_with_card_id(self, create_and_half_pay_basket):
        """
        https://st.yandex-team.ru/TRUST-1614
        Для Маркетплейса добавлена возможноть проброса card_id привязанной карты методом supply_payment_data
        """
        service = Services.MARKETPLACE
        user, basket = create_and_half_pay_basket(service)

        card = get_card()

        linked_cards, _ = trust.process_binding(user=user, cards=card)

        token = trust.get_auth_token(Auth.get_auth(user, service), user)['access_token']
        with check_mode(CheckMode.FAILED):
            trust.supply_payment_data(token, purchase_token=basket['purchase_token'],
                                      payment_method=linked_cards[0])

        with reporter.step(u'Проверяем, что после проброса card_id платеж проходит'):
            simple.wait_until_payment_done(service=service, user=user,
                                           purchase_token=basket['purchase_token'])

    @reporter.story(stories.Methods.Call)
    @pytest.mark.skipif(True, reason="YaMoney work with all markets like a shit")
    def test_with_3ds_and_only_cvn(self, create_and_pay_basket):
        service = Services.TICKETS
        user, basket, paymethod_id, card = create_and_pay_basket(service)

        token = trust.get_auth_token(Auth.get_auth(user, service=service), user)['access_token']
        card['cvn'] = CVN.force_3ds
        with check_mode(CheckMode.FAILED):
            trust.supply_payment_data(token, cvn=card['cvn'], purchase_token=basket['purchase_token'],
                                      payment_method=paymethod_id)
            trust.wait_until_3ds_url_added(basket['purchase_token'])
            redirect_3ds_url = trust.check_payment(basket['purchase_token'])['redirect_3ds_url']

        paymethod = TrustWebPage(Via.Pay3ds(card))
        trust.pay_by(paymethod, service, payment_url=redirect_3ds_url)

        with reporter.step(u'Проверяем, что cvn пробросился и платеж проходит'):
            simple.wait_until_payment_done(service=service, user=user,
                                           purchase_token=basket['purchase_token'])

    @reporter.story(stories.Methods.Call)
    @pytest.mark.skipif(True, reason="YaMoney work with all markets like a shit")
    def test_with_3ds_and_card_data(self, create_and_half_pay_basket):
        service = Services.MARKETPLACE
        user, basket = create_and_half_pay_basket(service)

        card = get_card(cvn=CVN.force_3ds)

        token = trust.get_auth_token(Auth.get_auth(user, service=service), user)['access_token']
        with check_mode(CheckMode.FAILED):
            trust.supply_payment_data(token, purchase_token=basket['purchase_token'],
                                      payment_method='new_card', cvn=card.get('cvn'),
                                      card_number=card.get('card_number'), cardholder=card.get('cardholder'),
                                      expiration_year=card.get('expiration_year'),
                                      expiration_month=card.get('expiration_month'))
            trust.wait_until_3ds_url_added(basket['purchase_token'])
            redirect_3ds_url = trust.check_payment(basket['purchase_token'])['redirect_3ds_url']

        paymethod = TrustWebPage(Via.Pay3ds(card), in_browser=True)
        trust.pay_by(paymethod, service, payment_url=redirect_3ds_url)

        with reporter.step(u'Проверяем, что после проброса карточных данных платеж проходит'):
            simple.wait_until_payment_done(service=service, user=user,
                                           purchase_token=basket['purchase_token'])


@reporter.feature(features.Methods.CheckCard)
class TestCheckCard(object):
    """
    https://st.yandex-team.ru/TRUST-1537
    """

    @pytest.fixture
    def user_with_linked_card(self):
        def user_and_linked_card_in_currency(card, service=Services.STORE):
            with reporter.step(u'Выбираем случайного пользователя для теста и отвязываем от него все привязанне карты'):
                user = uids.get_random_of(uids.test_passport)
                trust.process_unbinding(user=user, service=service)

            with reporter.step(u'Привязываем пользователю новую карту'):
                linked_cards, _ = trust.process_binding(user=user, cards=card, service=service)

            return user, linked_cards

        return user_and_linked_card_in_currency

    @reporter.story(stories.Methods.Call)
    @pytest.mark.skipif(True, reason="Trust decided ot comment out the code with trust.CheckCard")
    @pytest.mark.parametrize(*utils.Pytest.combine_set(
        utils.Pytest.ParamsSet(names='card',
                               values=[Data.ym_cards,
                                       Data.w1_cards
                                       ]),
        utils.Pytest.ParamsSet(names='currency',
                               values=[Data.ym_currencies,
                                       Data.w1_currencies
                                       ])),
                             ids=lambda card, currency: '{}-{}'.format(card['descr'], currency))
    def test_basic_logic(self, card, currency, user_with_linked_card):
        service = Services.TAXI
        user, linked_card = user_with_linked_card(card, service=service)

        token = trust.get_auth_token(Auth.get_auth(user, service=service), user)['access_token']
        card_id = linked_card[0][5::]  # card-x****, but need only x****
        with check_mode(CheckMode.FAILED):
            trust_payment_id = trust.check_card(token, card_id, currency=currency)['trust_payment_id']
        mongo.Refund.wait_until_done(trust_payment_id)
        trust.process_unbinding(user=user, service=service)


@reporter.feature(features.Methods.CheckPayment)
@reporter.story(stories.Methods.Call)
class TestCheckPayment(object):
    """
    https://st.yandex-team.ru/TRUST-974
    """
    service = Services.TICKETS

    @pytest.mark.parametrize('test_data', Data.developer_payload_data, ids=DataObject.ids)
    def test_payment_with_developer_payload(self, test_data):
        paymethod, user_type, developer_payload = test_data.paymethod, \
                                                           test_data.user_type, \
                                                           test_data.developer_payload
        user = uids.get_random_of_type(user_type)
        paymethod.init(self.service, user)
        with check_mode(CheckMode.FAILED):
            orders = payments_api.Form.orders_for_payment(service=self.service, user=user,
                                                          orders_structure=defaults.Order.structure_rub_one_order)
            basket = payments_api.Payments.create(self.service, user=user, paymethod_id=paymethod.id, orders=orders,
                                                  developer_payload=developer_payload)
            start_payment_dp = payments_api.Payments.start(self.service, user,
                                                           basket['purchase_token']).get('developer_payload')
            check.check_that(start_payment_dp, is_(equal_to(developer_payload)),
                             step=u'Проверяем, что при инициализации оплаты отобразился верный developer_payload',
                             error=u'При инициализации оплаты поле developer_payload отображается некорректно!')
            trust.pay_by(paymethod, self.service, user=user,
                         payment_url=basket.get('payment_url'),
                         purchase_token=basket['purchase_token'])

            check_payment_dp = payments_api.Wait.until_payment_done(self.service, user,
                                                                    basket['purchase_token']).get('developer_payload')
            check.check_that(check_payment_dp, is_(equal_to(developer_payload)),
                             step=u'Проверяем, что при проверке карзины отобразился верный developer_payload',
                             error=u'При проверке карзины поле developer_payload отображается некорректно!')

    def test_fail_pay_basket_one_hour_after_create(self):
        """
            Тест кейс из задачи TESTTRUST-13
            Проверяем, что при попытке оплаты корзины, которая создана более часа назад, оплата не проходит.
            Траст должен вернуть говорящую ошибку.
        """
        delta_time = 2
        user = uids.get_random_of_type(uids.Types.random_from_all)
        paymethod = LinkedCard(card=get_card())
        paymethod.init(self.service, user)
        orders = payments_api.Form.orders_for_payment(service=self.service, user=user,
                                                      orders_structure=defaults.Order.structure_rub_one_order)
        purchase_token = payments_api.Payments.create(self.service, user=user,
                                                      paymethod_id=paymethod.id, orders=orders)['purchase_token']
        new_dt = (datetime.datetime.now() - datetime.timedelta(hours=delta_time)).strftime('%Y-%m-%d %H:%M:%S')
        db_steps.bs().update_payment_dt(new_dt, purchase_token)
        with check_mode(CheckMode.IGNORED):
            basket = payments_api.Payments.start(self.service, user, purchase_token)
        check.check_that(basket, has_entries(expected.BasketError().payment_timeout()),
                         step=u'Проверяем что при попытке оплатить корзину верунлась верная ошибка',
                         error=u'При попытке оплатить корзину вернулась неверная ошибка!')

    @marks.ym_h2h_processing
    def test_basic_logic(self):
        paymethod = TrustWebPage(Via.Card(get_card()))
        user = uids.get_random_of_type(uids.Types.random_from_all)

        orders = simple.form_orders_for_create(self.service, user)
        with check_mode(CheckMode.FAILED):
            basket = simple.process_payment(service=self.service, user=user, paymethod=paymethod, orders=orders)
            resp = trust.check_payment(basket['purchase_token'])

        check.check_that(resp.iterkeys(), has_items('amount', 'currency'),
                         step=u'Проверяем что ответ метода содержит необходимые поля',
                         error=u'Ответ метода CheckPayment не содержит необходимые поля')

    @marks.web_in_browser
    def test_basic_logic_with3ds(self):
        paymethod = TrustWebPage(Via.Card(get_card(cvn=CVN.force_3ds)), in_browser=True)
        user = uids.get_random_of_type(uids.Types.random_from_all)

        orders = simple.form_orders_for_create(self.service, user)
        with check_mode(CheckMode.FAILED):
            basket = simple.process_payment(service=self.service, user=user, paymethod=paymethod,
                                            orders=orders, success_3ds_payment=True)
            resp = trust.check_payment(basket['purchase_token'])

        check.check_that(resp.iterkeys(), has_items('amount', 'currency'),
                         step=u'Проверяем что ответ метода содержит необходимые поля',
                         error=u'Ответ метода CheckPayment не содержит необходимые поля')

    @marks.ym_h2h_processing
    def test_discount_with_promocode(self):
        user = uids.get_random_of_type(uids.Types.random_from_all)
        paymethod = TrustWebPage(Via.card(get_card()))
        promocode_id = simple.process_promocode_creating(self.service)

        orders = simple.form_orders_for_create(self.service, user)
        with check_mode(CheckMode.FAILED):
            basket = simple.process_payment(self.service, user, orders=orders, paymethod=paymethod,
                                            promocode_id=promocode_id,
                                            discounts=[defaults.Discounts.id100['id'], ])
            simple.check_basket(self.service, purchase_token=basket['purchase_token'])
            payment_status = trust.check_payment(basket['purchase_token'])
            promo_status = simple.get_promocode_status(self.service, promocode_id=promocode_id,
                                                       with_payments=True)['result']['payment_ids'][0]

        check.check_that(promo_status, is_(equal_to(basket['trust_payment_id'])),
                         step=u'Проверяем, что промокод применился именно в этом платеже',
                         error=u'Некорректно применился промокод')
        check.check_that(payment_status['discounts'][0], is_(equal_to(defaults.Discounts.id100['id'])),
                         step=u'Проверяем корректность ID скидки в CheckPayment',
                         error=u'ID скидки в CheckPayment некорректен')


@marks.google_pay
@reporter.feature(features.Methods.BindGooglePayToken)
@reporter.story(stories.Methods.Call)
class TestGooglePay(object):
    def test_google_pay_auto_refund(self):
        """
        После авторизации должен происходить рефанд на сумму списания
        TESTTRUST-28
        """
        service = Services.TAXI
        paymethod = GooglePay()
        user = uids.get_random_of(uids.all_)

        token = trust.get_auth_token(Auth.get_auth(user, service), user)['access_token']
        resp = trust.bind_google_pay_token(token=token, google_pay_token=paymethod.token,
                                           order_tag=paymethod.order_tag)
        mongo.Refund.wait_until_done(trust_payment_id=resp['trust_payment_id'])

    @pytest.mark.parametrize('test_data', Data.google_pay_failure_bindings, ids=DataObject.ids_error)
    def test_failure_bind_google_pay_token(self, test_data):
        service = Services.TAXI
        paymethod, user_type, expected_error = \
            test_data.paymethod, test_data.user_type, test_data.error
        user = uids.get_random_of_type(user_type)

        with check_mode(CheckMode.IGNORED):
            token = trust.get_auth_token(Auth.get_auth(user, service), user)['access_token']
            resp = trust.bind_google_pay_token(token=token, google_pay_token=paymethod.token,
                                               order_tag=paymethod.order_tag)
        check.check_that(resp, has_entries(expected_error),
                         step=u'Проверяем код ошибки после попытки привязать токен Google Pay',
                         error=u'Некорректный статус после попытки привязать токен Google Pay')


if __name__ == '__main__':
    pytest.main()
