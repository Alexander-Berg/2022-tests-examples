# coding=utf-8

import pytest
from hamcrest import equal_to, is_, none, not_, has_entries, has_key, has_items, greater_than
from datetime import datetime
from decimal import Decimal as D

import btestlib.reporter as reporter
from btestlib.constants import Services
from btestlib.utils import CheckMode, check_mode, Date
from simpleapi.common import utils
from simpleapi.common.payment_methods import TrustWebPage, Via, LinkedCard
from simpleapi.common.utils import DataObject
from simpleapi.data import defaults
from simpleapi.data import features, stories
from simpleapi.data import marks
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import get_card, CVN
from simpleapi.matchers.deep_equals import deep_equals_to
from simpleapi.steps import check_steps as check
from simpleapi.steps import expected_steps as expected
from simpleapi.steps import mongo_steps as mongo
from simpleapi.steps import simple_steps as simple
from simpleapi.steps import simple_steps_bo as simple_bo
from simpleapi.steps import trust_steps as trust

__author__ = 'fellow'

service = Services.MUSIC


class Data(object):
    test_data = [
        DataObject(paymethod=LinkedCard(card=get_card()), user_type=uids.Types.random_from_all).new(payload=None),
        marks.web_in_browser(DataObject(paymethod=TrustWebPage(Via.linked_card(get_card()), in_browser=True),
                                        user_type=uids.Types.random_from_all).new(
            payload='{"show-bound-cards": true}')),
        marks.web_in_browser(DataObject(paymethod=TrustWebPage(Via.linked_card(get_card(), from_linked_phonish=True),
                                                               in_browser=True),
                                        user_type=uids.Types.random_with_linked_phonishes).new(
            payload='{"show-bound-cards": true}')),
        marks.web_in_browser(DataObject(paymethod=TrustWebPage(Via.card(get_card()), in_browser=True),
                                        user_type=uids.Types.random_from_all).new(payload=None)),
        pytest.mark.yamoney(DataObject(paymethod=TrustWebPage(Via.yandex_money()),
                                       user_type=uids.Type(pool=uids.secret, name='test_wo_proxy_old')).new(
            payload=None)),
        marks.web_in_browser(DataObject(paymethod=TrustWebPage(Via.card(get_card()), in_browser=True),
                                        user_type=uids.Types.random_autoremove).new(payload=None)),
    ]

    test_data_change_service_product = [
        DataObject(comment=u'same payment method',
                   paymethod=LinkedCard(card=get_card()),
                   user_type=uids.Types.random_from_all).new(
            payload=None,
            second_service_product_type=defaults.ServiceProduct.Subscription.NORMAL_SINGLE_PURCHASED.copy(),
            paymethod_new=None),
        DataObject(comment=u'new payment method',
                   paymethod=TrustWebPage(Via.linked_card(get_card()), in_browser=True),
                   user_type=uids.Types.random_from_all).new(
            payload=None,
            second_service_product_type=defaults.ServiceProduct.Subscription.NORMAL_SINGLE_PURCHASED.copy(),
            paymethod_new=TrustWebPage(Via.linked_card(get_card()), in_browser=True)),
        DataObject(comment=u'same payment method',
                   paymethod=TrustWebPage(Via.linked_card(get_card(), from_linked_phonish=True),
                                          in_browser=True),
                   user_type=uids.Types.random_with_linked_phonishes).new(
            payload='{"show-bound-cards": true}',
            second_service_product_type=defaults.ServiceProduct.Subscription.NORMAL_SINGLE_PURCHASED.copy(),
            paymethod_new=None),
        DataObject(comment=u'update to subs with introductory period',
                   paymethod=LinkedCard(card=get_card()),
                   user_type=uids.Types.random_from_all).new(
            payload=None,
            second_service_product_type=defaults.ServiceProduct.Subscription.INTRODUCTORY.copy(),
            paymethod_new=None),
        DataObject(comment=u'update to subs with trial period',
                   paymethod=LinkedCard(card=get_card()),
                   user_type=uids.Types.random_from_all).new(
            payload=None,
            second_service_product_type=defaults.ServiceProduct.Subscription.TRIAL.copy(),
            paymethod_new=None),
    ]

    test_data_short = [
        DataObject(paymethod=LinkedCard(card=get_card()),
                   user_type=uids.Types.random_from_all).new(
            payload=None,
            second_service_product_type=defaults.ServiceProduct.Subscription.NORMAL_SINGLE_PURCHASED.copy(),
            paymethod_new=None),
    ]

    test_data_next_charge = [
        DataObject(comment=u'user have only one linked card',
                   paymethod=LinkedCard(card=get_card()),
                   user_type=uids.Types.random_from_all).new(
            link_another_card=False,
            product_type=defaults.ServiceProduct.Subscription.NORMAL.copy()),
        DataObject(comment=u'user with yamoney wallet',
                   paymethod=LinkedCard(card=get_card()),
                   user_type=uids.Types.random_yamoney_wallet).new(
            link_another_card=False,
            product_type=defaults.ServiceProduct.Subscription.NORMAL.copy()),
        DataObject(comment=u'user have two linked cards',
                   paymethod=LinkedCard(card=get_card()),
                   user_type=uids.Types.random_from_all).new(
            link_another_card=True,
            product_type=defaults.ServiceProduct.Subscription.NORMAL.copy()),
        DataObject(comment=u'trial subscription',
                   paymethod=LinkedCard(card=get_card()),
                   user_type=uids.Types.random_from_all).new(
            link_another_card=False,
            product_type=defaults.ServiceProduct.Subscription.TRIAL.copy()),
        DataObject(comment=u'introductory subscription',
                   paymethod=LinkedCard(card=get_card()),
                   user_type=uids.Types.random_from_all).new(
            link_another_card=False,
            product_type=defaults.ServiceProduct.Subscription.INTRODUCTORY.copy()),
    ]
    orders_structure = [
        defaults.Order.structure_rub_one_order,
        defaults.Order.structure_rub_two_orders,
    ]

    test_data_no_save_card = [
        marks.ym_h2h_processing(DataObject(paymethod=TrustWebPage(Via.card(get_card()), in_browser=True),
                                           user_type=uids.Types.random_from_all)),
    ]
    subscription_type = [
        defaults.ServiceProduct.Subscription.TRIAL.copy(),
        defaults.ServiceProduct.Subscription.INTRODUCTORY.copy(),
    ]
    subscription_type_full = [
        defaults.ServiceProduct.Subscription.NORMAL.copy(),
        defaults.ServiceProduct.Subscription.TRIAL.copy(),
        defaults.ServiceProduct.Subscription.INTRODUCTORY.copy(),
    ]


@pytest.fixture
def create_and_stop_subscription_finally(request):
    def create_subs_order(user, product=None, product_id=None, developer_payload=None,
                          subs_begin_ts=None, parent_service_order_id=None):
        orders = simple.form_orders_for_create(service, user, Data.orders_structure[0],
                                               service_product_id=product_id,
                                               service_product_type=product,
                                               developer_payload=developer_payload,
                                               subs_begin_ts=subs_begin_ts,
                                               parent_service_order_id=parent_service_order_id)

        def fin():
            for order in orders:
                simple_bo.stop_subscription(service, service_order_id=order['service_order_id'], user=user)

        request.addfinalizer(fin)

        return orders

    return create_subs_order


@reporter.feature(features.Service.Music)
@reporter.story(stories.General.Subscription)
@pytest.mark.parametrize('test_data', Data.test_data, ids=DataObject.ids)
class TestBaseMusicSubscriptions(object):
    def test_normal_subscription_continuation(self, test_data, create_and_stop_subscription_finally):
        product = defaults.ServiceProduct.Subscription.NORMAL.copy()
        paymethod, user_type, payload = test_data.paymethod, test_data.user_type, test_data.payload
        user = uids.get_random_of_type(user_type)
        orders = create_and_stop_subscription_finally(user, product, developer_payload=payload)
        basket = simple.process_payment(service, user=user, orders=orders, paymethod=paymethod)
        simple.wait_until_subscription_continuation(service, user=user, orders=orders,
                                                    trust_payment_id=basket['trust_payment_id'])

    def test_trial_subscription_continuation(self, test_data, create_and_stop_subscription_finally):
        product = defaults.ServiceProduct.Subscription.TRIAL.copy()
        paymethod, user_type, payload = test_data.paymethod, test_data.user_type, test_data.payload
        user = uids.get_random_of_type(user_type)
        orders = create_and_stop_subscription_finally(user, product, developer_payload=payload)
        basket = simple.process_payment(service, user=user, orders=orders, paymethod=paymethod)
        simple.wait_until_subscription_continuation(service, user=user, orders=orders,
                                                    trust_payment_id=basket['trust_payment_id'])
        # simple.wait_until_subs_state_do(service, user, orders, defaults.Subscriptions.State.PAID)
        basket = simple.check_basket(service, user=user, trust_payment_id=basket['trust_payment_id'])
        amount, orig_amount, paid_amount, orders = \
            basket['amount'], basket['orig_amount'], \
            basket['paid_amount'], basket['orders'][0]
        check.check_that(amount, is_(equal_to('0.00')),
                         step=u'Проверяем, что amount занулён',
                         error=u'Некорректный amount')
        check.check_that(orig_amount, is_(equal_to('0.00')),
                         step=u'Проверяем, что orig_amount занулён',
                         error=u'Некорректный orig_amount')
        check.check_that(paid_amount, is_(equal_to('0.00')),
                         step=u'Проверяем, что paid_amount занулён',
                         error=u'Некорректный paid_amount')
        check.check_that(basket.get('fiscal_status', None), none(),
                         step=u'Проверяем, что в триал не проставился фискал',
                         error=u'ALARM! В триале есть фискальные данные')
        check.check_that(orders, has_key('has_trial_period'),
                         step=u'Проверяем, что в orders есть ключ:'
                              u' "has_trial_period"',
                         error=u'ALARM! В orders отсутствует ключ:'
                               u' "has_trial_period"')

    @staticmethod
    def check_orders_amount(basket, *prices_list):
        from decimal import Decimal as D
        for order in basket['orders']:
            exp_amount = D(0)
            currency = order['current_amount'][0][0]
            for prices in prices_list:
                exp_amount += D(utils.find_dict_in_list(prices, currency=currency)['price'])
            check.check_that(D(order['current_amount'][0][1]), equal_to(exp_amount),
                             step=u'Проверяем, что применилась правильная цена подписки',
                             error=u'Применилась неправильная цена подписки')

    def test_introductory_subscription_continuation(self, test_data, create_and_stop_subscription_finally):
        product = defaults.ServiceProduct.Subscription.INTRODUCTORY.copy()
        paymethod, user_type, payload = test_data.paymethod, test_data.user_type, test_data.payload
        user = uids.get_random_of_type(user_type)
        orders = create_and_stop_subscription_finally(user, product, developer_payload=payload)
        with reporter.step(u'Оплачиваем подписку и ждем, пока закончится introductory-период'):
            basket = simple.process_payment(service, user=user, orders=orders, paymethod=paymethod)

            self.check_orders_amount(basket,
                                     product['subs_introductory_period_prices'])
            simple.wait_until_introductory_period_finished(service, user=user, orders=orders,
                                                           trust_payment_id=basket['trust_payment_id'])
        with reporter.step(u'После окончания introductory-периода ждем, что подписка продлевается в штатном режиме'):
            basket = simple.wait_until_subscription_continuation(service, user=user, orders=orders,
                                                                 trust_payment_id=basket['trust_payment_id'])
            self.check_orders_amount(basket,
                                     product['subs_introductory_period_prices'],
                                     product['prices'])
        check.check_that(basket['orders'][0], has_key('has_intro_period'),
                         step=u'Проверяем, что в orders есть ключ:'
                              u' "has_intro_period"',
                         error=u'ALARM! В orders отсутствует ключ:'
                               u' "has_intro_period"')

    @staticmethod
    def stop_subs_and_check_state(user, basket, order, state, step, error):
        simple_bo.stop_subscription(service, service_order_id=order['service_order_id'], user=user)
        basket = simple.check_basket(service, user=user, trust_payment_id=basket['trust_payment_id'])
        order_state = utils.find_dict_in_list(basket['orders'],
                                              service_order_id=order['service_order_id'])['subs_state']
        check.check_that(order_state, equal_to(state), step=step, error=error)

    def test_stop_normal_subscription(self, test_data, create_and_stop_subscription_finally):
        product = defaults.ServiceProduct.Subscription.NORMAL.copy()
        paymethod, user_type, payload = test_data.paymethod, test_data.user_type, test_data.payload
        user = uids.get_random_of_type(user_type)
        orders = create_and_stop_subscription_finally(user, product, developer_payload=payload)

        basket = simple.process_payment(service, user=user, orders=orders, paymethod=paymethod)

        simple.wait_until_subscription_continuation(service, user=user, orders=orders,
                                                    trust_payment_id=basket['trust_payment_id'])
        for order in orders:
            self.stop_subs_and_check_state(user, basket, order,
                                           state=defaults.Subscriptions.State.FINISHED,
                                           step=u'Проверяем, что подписка {} остановилась'.format(
                                               order['service_order_id']),
                                           error=u'Не остановилась подписка')

    def test_stop_subscription_before_introductory_period(self, test_data, create_and_stop_subscription_finally):
        product = defaults.ServiceProduct.Subscription.INTRODUCTORY.copy()
        paymethod, user_type, payload = test_data.paymethod, test_data.user_type, test_data.payload
        user = uids.get_random_of_type(user_type)
        paymethod.init(service, user)
        orders = create_and_stop_subscription_finally(user, product, developer_payload=payload)
        with reporter.step(u'Создаем introductory-подписку и сразу останавливаем ее'):
            basket = simple.create_basket(service, user=user,
                                          orders=orders,
                                          paymethod_id=paymethod.id)
            for order in orders:
                self.stop_subs_and_check_state(user, basket, order,
                                               state=defaults.Subscriptions.State.FINISHED,
                                               step=u'Проверяем, что подписка {} остановилась на introductory-периоде'.format(
                                                   order['service_order_id']),
                                               error=u'Подписка не остановилась или статус неверен')

    def test_stop_subscription_on_introductory_period(self, test_data, create_and_stop_subscription_finally):
        product = defaults.ServiceProduct.Subscription.INTRODUCTORY.copy()
        paymethod, user_type, payload = test_data.paymethod, test_data.user_type, test_data.payload
        user = uids.get_random_of_type(user_type)
        orders = create_and_stop_subscription_finally(user, product, developer_payload=payload)
        with reporter.step(u'Оплачиваем introductory-подписку и останавливаем ее '
                           u'до того как закончится ознакомительный период'):
            basket = simple.process_payment(service, user=user, orders=orders, paymethod=paymethod)
        for order in orders:
            self.stop_subs_and_check_state(user, basket, order,
                                           state=defaults.Subscriptions.State.STOPPED_WITH_INTRODUCTORY_PERIOD,
                                           step=u'Проверяем, что подписка {} остановилась на introductory-периоде'.format(
                                               order['service_order_id']),
                                           error=u'Подписка не остановилась или статус неверен')

    def test_stop_subscription_after_introductory_period(self, test_data, create_and_stop_subscription_finally):
        product = defaults.ServiceProduct.Subscription.INTRODUCTORY.copy()
        paymethod, user_type, payload = test_data.paymethod, test_data.user_type, test_data.payload
        user = uids.get_random_of_type(user_type)
        orders = create_and_stop_subscription_finally(user, product, developer_payload=payload)
        with reporter.step(u'Оплачиваем introductory-подписку и ждем, пока закончится ознакомительный период'):
            basket = simple.process_payment(service, user=user, orders=orders, paymethod=paymethod)

            simple.wait_until_introductory_period_finished(service, user=user, orders=orders,
                                                           trust_payment_id=basket['trust_payment_id'])
        with reporter.step(u'Останавливаем introductory-подписку после того как introductory-период закончился'):
            for order in orders:
                self.stop_subs_and_check_state(user, basket, order,
                                               state=defaults.Subscriptions.State.STOPPED_WITH_INTRODUCTORY_PERIOD,
                                               step=u'Проверяем, что подписка {} остановилась на introductory-периоде'.format(
                                                   order['service_order_id']),
                                               error=u'Подписка не остановилась или статус неверен')


def ids_bindings_and_blocked(val):
    return 'max cards {}, blocked {}'.format(val['bindings_count'], val['will_block'])


def ids_block_or_expired(val):
    return 'with blocked card' if val.get('blocking_reason') else 'with expired card'


@reporter.feature(features.Service.Music)
@reporter.story(stories.General.Subscription)
class TestSpecificMusicSubscriptions(object):
    @pytest.fixture
    def prepare_bindings(self):
        def unbind_cards_and_bind_some_new(user, bindings_count):
            trust.process_unbinding(user)
            cards = [get_card() for _ in range(bindings_count)]
            _, trust_payment_id = trust.process_binding(user, cards=cards, service=service, multiple=1)

            return cards, trust_payment_id

        return unbind_cards_and_bind_some_new

    def test_subs_continuation_by_same_card(self, create_and_stop_subscription_finally,
                                            prepare_bindings):
        product = defaults.ServiceProduct.Subscription.NORMAL.copy()
        user = uids.get_random_of_type(uids.Types.random_from_all)
        orders = create_and_stop_subscription_finally(user, product)

        cards, _ = prepare_bindings(user, 3)
        paymethod = LinkedCard(card=cards[0])

        paid_basket = simple.process_payment(service, user=user, orders=orders, paymethod=paymethod)
        subs_continuation_basket = simple.wait_until_subscription_continuation(service, user=user, orders=orders,
                                                                               trust_payment_id=paid_basket[
                                                                                   'trust_payment_id'])
        subs_basket = simple.check_basket(service, user=user,
                                          purchase_token=subs_continuation_basket['orders'][0]['payments'][1])

        with reporter.step(u'Проверяем, что подписка продлена той же картой, что и основная оплата'):
            check.check_that(paid_basket['payment_method'], is_(equal_to(subs_basket['payment_method'])),
                             step=u'Проверяем, что payment_method у подписки и продления идентичны',
                             error=u'Подписка продлена не той же картой, что и основная оплата!')
            check.check_that(paid_basket['user_account'], is_(subs_basket['user_account']),
                             step=u'Проверяем, что номер карты у подписки и продления идентичны',
                             error=u'У подписки и основной оплаты разный номер карты!')

    @pytest.mark.parametrize('test_data', [{'bindings_count': 2, 'will_block': 1},
                                           {'bindings_count': 3, 'will_block': 2}],
                             ids=ids_bindings_and_blocked)
    # TODO: slppls: вместо blocking_reason добавить недостаток средств через эмулятор. Как сейчас - не работает
    @pytest.mark.parametrize('date_to_up', [  # {'blocking_reason': 'Some test reason'},
        {'expiration_month': '01',
         'expiration_year': '2017'}],
                             ids=ids_block_or_expired)
    def test_subs_continuation_when_main_card_blocked(self, test_data, create_and_stop_subscription_finally,
                                                      prepare_bindings, date_to_up):
        product = defaults.ServiceProduct.Subscription.NORMAL.copy()
        user = uids.get_random_of_type(uids.Types.random_from_all)
        orders = create_and_stop_subscription_finally(user, product)

        cards, trust_payment_id = prepare_bindings(user, test_data['bindings_count'])
        paymethod = LinkedCard(card=cards[0])
        paid_basket = simple.process_payment(service, user=user, orders=orders, paymethod=paymethod)

        for num in range(test_data['will_block']):
            mongo.Card.update_data(trust_payment_id[num], data_to_update=date_to_up)
        subs_continuation_basket = simple.wait_until_subscription_continuation(service, user=user, orders=orders,
                                                                               trust_payment_id=paid_basket[
                                                                                   'trust_payment_id'])
        subs_basket = simple.check_basket(service, user=user,
                                          purchase_token=subs_continuation_basket['orders'][0]['payments'][1])

        with reporter.step(u'Проверяем, что подписка продлена не той же картой, что и основная оплата'):
            check.check_that(paid_basket['payment_method'], not_(subs_basket['payment_method']),
                             step=u'Проверяем, что payment_method у подписки и продления различаются',
                             error=u'Подписка продлена той же картой, что и основная оплата!')
            check.check_that(paid_basket['user_account'], not_(subs_basket['user_account']),
                             step=u'Проверяем, что номер карты у подписки и продления различаются',
                             error=u'У подписки и основной оплаты идентичный номер карты!')

    @pytest.fixture
    def block_card_and_unblock_finally(self, request):
        def blocked_card(trust_payment_id, reason):
            with reporter.step(u'Помечаем карту меткой blocking_reason'):
                mongo.Card.update_data(trust_payment_id, data_to_update={'blocking_reason': reason})

            def fin():
                with reporter.step(u'Разблокируем карту в конце теста'):
                    mongo.Card.update_data(trust_payment_id, data_to_update={'blocking_reason': None}, wait=False)

            request.addfinalizer(fin)

        return blocked_card

    def test_subs_stop_when_card_blocked(self, create_and_stop_subscription_finally, block_card_and_unblock_finally):
        product = defaults.ServiceProduct.Subscription.NORMAL.copy()
        paymethod = LinkedCard(card=get_card())
        user = uids.get_random_of_type(uids.Types.random_from_all)
        orders = create_and_stop_subscription_finally(user, product)

        reason = 'Some test reason'

        with reporter.step(u'Создаем и оплачиваем подписку, ждем ее продления'):
            basket = simple.process_payment(service, user=user, orders=orders, paymethod=paymethod)

        block_card_and_unblock_finally(trust_payment_id=basket['trust_payment_id'], reason=reason)

        resp = simple.wait_until_failed_subscription_continuation(service, user=user, orders=orders,
                                                                  trust_payment_id=basket['trust_payment_id'])
        last_purchase_token = simple.get_last_subs_purchase_token(resp)

        with check_mode(CheckMode.IGNORED), \
             reporter.step(u'Проверяем, что подписка с заблокированной картой останавливается'):
            simple.wait_until_subs_state_do(service, user, defaults.Subscriptions.State.NOT_PAID,
                                            purchase_token=last_purchase_token)
            resp = simple.check_basket(service, purchase_token=last_purchase_token)

        check.check_that(resp, has_entries(expected.BasketError.card_is_blocked(reason)),
                         step=u'Проверяем статус подписки после неудачной оплаты из-за заблокированной карты',
                         error=u'Некорректный статус корзины после неудачной оплаты из-за заблокированной карты')

    @pytest.mark.parametrize('product', [defaults.ServiceProduct.Subscription.NORMAL.copy(),
                                         defaults.ServiceProduct.Subscription.INTRODUCTORY.copy()],
                             ids=DataObject.ids_product_name)
    @pytest.mark.parametrize('test_data', Data.test_data, ids=DataObject.ids_paymethod)
    def test_visible_of_subs_date(self, test_data, product, create_and_stop_subscription_finally):
        list_of_params = ['begin_dt', 'begin_ts', 'begin_ts_msec',
                          'subs_until_dt', 'subs_until_ts', 'subs_until_ts_msec']

        paymethod, user_type, payload = test_data.paymethod, test_data.user_type, test_data.payload
        user = uids.get_random_of_type(user_type)
        orders = create_and_stop_subscription_finally(user, product, developer_payload=payload)
        paymethod.init(service, user)

        # до оплаты параметров быть не должно
        basket = simple.create_basket(service, user=user, orders=orders, paymethod_id=paymethod.id)
        check_basket_result = simple.check_basket(service, user=user, trust_payment_id=basket['trust_payment_id'])
        check.check_that(check_basket_result['orders'][0].keys(), not_(has_items(*list_of_params)),
                         step=u'Проверяем, что заказ в корзине до оплаты НЕ содержит поля {}'.format(list_of_params),
                         error=u'Заказ в корзине до оплаты содержит какие-то из полей {}'.format(list_of_params))

        pay_basket_result = simple.pay_basket(service, user=user, trust_payment_id=basket['trust_payment_id'])
        check.check_that(pay_basket_result['orders'][0].keys(), not_(has_items(*list_of_params)),
                         step=u'Проверяем, что заказ в ответе pay_basket до оплаты '
                              u'НЕ содержит поля {}'.format(list_of_params),
                         error=u'Заказ в ответе pay_basket до оплаты '
                               u'содержит какие-то из полей {}'.format(list_of_params))
        trust.pay_by(paymethod, service, user=user, payment_form=pay_basket_result.get('payment_form', None),
                     purchase_token=pay_basket_result['purchase_token'])

        # после оплаты параметры быть должны
        simple.wait_until_subscription_continuation(service, user=user, orders=orders,
                                                    trust_payment_id=basket['trust_payment_id'])
        check_basket_result = simple.check_basket(service, user=user, trust_payment_id=basket['trust_payment_id'])
        check.check_that(check_basket_result['orders'][0].keys(), has_items(*list_of_params),
                         step=u'Проверяем, что заказ в корзине после оплаты содержит поля {}'.format(list_of_params),
                         error=u'Заказ в корзине после оплаты НЕ содержит поля {}'.format(list_of_params))
        # останавливаем подписку, чтобы не было бесконечных продлений до рефанда. Плюс так делает сама музыка
        for order in orders:
            simple_bo.stop_subscription(service, service_order_id=order['service_order_id'], user=user)

        # после рефанда параметров быть не должно
        for purchase_token in check_basket_result['orders'][0]['payments']:
            basket_tmp = simple.check_basket(service, user=user, purchase_token=purchase_token)
            simple.process_refund(service, trust_payment_id=basket_tmp['trust_payment_id'], basket=check_basket_result)
        check_basket_result = simple.check_basket(service, user=user, trust_payment_id=basket['trust_payment_id'])
        check.check_that(check_basket_result['orders'][0].keys(), not_(has_items(*list_of_params)),
                         step=u'Проверяем, что заказ в корзине после рефанда НЕ содержит поля {}'.format(
                             list_of_params),
                         error=u'Заказ в корзине после рефанда содержит какие-то из полей {}'.format(list_of_params))

    @pytest.mark.parametrize('test_data', Data.test_data, ids=DataObject.ids_paymethod)
    def test_visible_of_subs_date_trial(self, test_data, create_and_stop_subscription_finally):
        list_of_params = ['begin_dt', 'begin_ts', 'begin_ts_msec',
                          'subs_until_dt', 'subs_until_ts', 'subs_until_ts_msec']

        product = defaults.ServiceProduct.Subscription.TRIAL.copy()
        paymethod, user_type, payload = test_data.paymethod, test_data.user_type, test_data.payload
        user = uids.get_random_of_type(user_type)
        orders = create_and_stop_subscription_finally(user, product, developer_payload=payload)
        paymethod.init(service, user)

        # до оплаты параметров быть не должно
        basket = simple.create_basket(service, user=user, orders=orders, paymethod_id=paymethod.id)
        check_basket_result = simple.check_basket(service, user=user, trust_payment_id=basket['trust_payment_id'])
        check.check_that(check_basket_result['orders'][0].keys(), not_(has_items(*list_of_params)),
                         step=u'Проверяем, что заказ в корзине до оплаты НЕ содержит поля {}'.format(list_of_params),
                         error=u'Заказ в корзине до оплаты содержит какие-то из полей {}'.format(list_of_params))

        pay_basket_result = simple.pay_basket(service, user=user, trust_payment_id=basket['trust_payment_id'])
        check.check_that(pay_basket_result['orders'][0].keys(), not_(has_items(*list_of_params)),
                         step=u'Проверяем, что заказ в ответе pay_basket до оплаты '
                              u'НЕ содержит поля {}'.format(list_of_params),
                         error=u'Заказ в ответе pay_basket до оплаты '
                               u'содержит какие-то из полей {}'.format(list_of_params))
        trust.pay_by(paymethod, service, user=user, payment_form=pay_basket_result.get('payment_form', None),
                     purchase_token=pay_basket_result['purchase_token'])

        # после оплаты параметры быть должны
        simple.wait_until_subscription_continuation(service, user=user, orders=orders,
                                                    trust_payment_id=basket['trust_payment_id'])
        check_basket_result = simple.check_basket(service, user=user, trust_payment_id=basket['trust_payment_id'])
        check.check_that(check_basket_result['orders'][0].keys(), has_items(*list_of_params),
                         step=u'Проверяем, что заказ в корзине после оплаты содержит поля {}'.format(list_of_params),
                         error=u'Заказ в корзине после оплаты НЕ содержит поля {}'.format(list_of_params))

    @pytest.mark.parametrize('test_data', Data.test_data, ids=DataObject.ids_paymethod)
    def test_single_purchase(self, test_data):
        # slppls: Данный кейс ранее использовался в подписочных продуктах, но затем логика работы с single_purchase
        # изменилась, и кейс в таком виде работает только для неподписочных продуктов.
        # Так как у музыки нет неподписочных продуктов - тест находится в специфических - на проде такого кейса нет.
        paymethod, user_type, payload = test_data.paymethod, test_data.user_type, test_data.payload
        user = uids.get_random_of_type(user_type)

        product = defaults.ServiceProduct.app.copy()
        product.update({'single_purchase': True})
        service_product_id = simple.create_service_product_for_service(service, product_type=product)

        orders_1 = simple.form_orders_for_create(service, user, Data.orders_structure[0],
                                                 service_product_id=service_product_id,
                                                 developer_payload=payload)
        orders_2 = simple.form_orders_for_create(service, user, Data.orders_structure[0],
                                                 service_product_id=service_product_id,
                                                 developer_payload=payload)

        with reporter.step(u'Создаем и оплачиваем корзину'):
            simple.process_payment(service, user=user, orders=orders_1, paymethod=paymethod)

        with reporter.step(u'Пытаемся создать вторую корзину на тот же продукт'), check_mode(CheckMode.IGNORED):
            basket = simple.create_basket(service, user=user, orders=orders_2, paymethod_id=paymethod.id)
            pay_basket_result = simple.pay_basket(service, user=user, purchase_token=basket['purchase_token'])

            check.check_that(pay_basket_result,
                             deep_equals_to(expected.RegularBasket.already_purchased(service_product_id)),
                             step=u'Проверяем, что возвращается говорящая ошибка о том, что корзина уже создана',
                             error=u'Не вернулась говорящая ошибка о том, что корзина уже создана')

    @pytest.fixture()
    def create_normal_subscription_product(self):
        def normal_subscription_product(service, single_purchase=None):
            with reporter.step(u'Создаем подписочный продукт без триального периода с single_purchase={}'
                                       .format(single_purchase)):
                product = defaults.ServiceProduct.Subscription.NORMAL.copy()
                if single_purchase is not None:
                    product.update({'single_purchase': single_purchase})
                service_product_id = simple.create_service_product_for_service(service, product_type=product)

            return service_product_id

        return normal_subscription_product

    def test_two_orders_with_single_purchase_have_one_order_id(self, create_normal_subscription_product):
        user = uids.get_random_of_type(uids.Types.random_from_all)
        service_product_id = create_normal_subscription_product(service, single_purchase=1)
        orders_1 = simple.create_service_product_and_order(service, user=user,
                                                           service_product_id=service_product_id)
        orders_2 = simple.create_service_product_and_order(service, user=user,
                                                           service_product_id=service_product_id)

        check.check_that(orders_1['service_order_id'], is_(equal_to(orders_2['service_order_id'])),
                         step=u'Проверяем, что у заказов одинаковые id',
                         error=u'У заказов разные id!')
        check.check_that(orders_2['status_code'], is_(equal_to(defaults.Order.order_purchased)),
                         step=u'Проверяем, что возвращается говорящий status_code о том, что заказ уже создан',
                         error=u'Не вернулась говорящий status_code!')

    @pytest.mark.parametrize('test_data', Data.test_data, ids=DataObject.ids_paymethod)
    def test_new_order_after_stop_subs_with_single_purchase(self, test_data, create_normal_subscription_product):
        paymethod, user_type, payload = test_data.paymethod, test_data.user_type, test_data.payload
        user = uids.get_random_of_type(user_type)
        service_product_id = create_normal_subscription_product(service, single_purchase=1)

        orders_1 = simple.create_service_product_and_order(service, user=user,
                                                           service_product_id=service_product_id,
                                                           developer_payload=payload)
        base_orders = [{'currency': 'RUB', 'service_order_id': orders_1['service_order_id']}]

        with reporter.step(u'Создаем подписку и останавливаем, не продляя'):
            simple.process_payment(service, user=user, orders=base_orders, paymethod=paymethod)
            simple_bo.stop_subscription(service, service_order_id=base_orders[0]['service_order_id'], user=user)
        orders_2 = simple.create_service_product_and_order(service, user=user,
                                                           service_product_id=service_product_id,
                                                           developer_payload=payload)
        check.check_that(orders_1['service_order_id'], equal_to(orders_2['service_order_id']),
                         step=u'Проверяем, что у заказов одинаковые id',
                         error=u'У заказов разные id!')
        check.check_that(orders_2['status_code'], is_(equal_to(defaults.Order.order_purchased)),
                         step=u'Проверяем, что возвращается говорящий status_code о том, что заказ уже создан',
                         error=u'Не вернулась говорящий status_code!')

    @pytest.mark.parametrize('test_data', Data.test_data,
                             ids=DataObject.ids_paymethod)
    def test_new_order_after_stop_subs_with_single_purchase_and_new_payment(self, test_data,
                                                                            create_normal_subscription_product):
        paymethod, user_type, payload = test_data.paymethod, test_data.user_type, test_data.payload
        user = uids.get_random_of_type(user_type)
        service_product_id = create_normal_subscription_product(service, single_purchase=1)
        orders_1 = simple.create_service_product_and_order(service, user=user,
                                                           service_product_id=service_product_id,
                                                           developer_payload=payload)
        base_orders = [{'currency': 'RUB', 'service_order_id': orders_1['service_order_id']}]
        with reporter.step(u'Создаем подписку и останавливаем, не продляя'):
            basket = simple.process_payment(service, user=user, orders=base_orders, paymethod=paymethod)
            simple_bo.stop_subscription(service, service_order_id=base_orders[0]['service_order_id'], user=user)
            simple.wait_until_time_goes_to(basket['orders'][0]['subs_until_dt'])
            orders_2 = simple.create_service_product_and_order(service,
                                                               user=user,
                                                               service_product_id=service_product_id,
                                                               developer_payload=payload)
            base_orders = [{'currency': 'RUB',
                            'service_order_id': orders_2['service_order_id']}]
            simple.process_payment(service, user=user, orders=base_orders,
                                   paymethod=paymethod)
        check.check_that(orders_1['service_order_id'], not_(equal_to(orders_2['service_order_id'])),
                         step=u'Проверяем, что у заказов разные id',
                         error=u'У заказов одинаковые id!')

    @pytest.mark.parametrize('test_data', Data.subscription_type,
                             ids=DataObject.ids_product_name)
    def test_two_basket_one_payment_id(self, test_data, create_and_stop_subscription_finally):
        product = test_data.copy()
        paymethod = TrustWebPage(Via.card(get_card()))
        user = uids.get_random_of_type(uids.Types.random_from_all)
        orders = create_and_stop_subscription_finally(user, product)
        first_basket = simple.create_basket(service, user=user, orders=orders,
                                            paymethod_id=paymethod.id)
        second_basket = simple.create_basket(service, user=user, orders=orders,
                                             paymethod_id=paymethod.id)
        check.check_that(first_basket['trust_payment_id'],
                         is_(equal_to(second_basket['trust_payment_id'])),
                         step=u'Проверяем, что trust_payment_id одинаковы',
                         error=u'ALARM! разные trust_payment_id')

    def test_payment_for_paid_subscription(self, create_and_stop_subscription_finally):
        product = defaults.ServiceProduct.Subscription.NORMAL.copy()
        product['single_purchase'] = 1
        paymethod = TrustWebPage(Via.card(get_card()))
        user = uids.get_random_of_type(uids.Types.random_from_all)
        orders = create_and_stop_subscription_finally(user, product)
        basket = simple.process_payment(service, user=user, orders=orders,
                                        paymethod=paymethod)
        service_product_id = basket['orders'][0]['service_product_id']
        service_order_id = basket['orders'][0]['service_order_id']
        service_order_id_2 = simple.form_orders_for_create(service, user,
                                                           Data.orders_structure[0],
                                                           service_product_type=product,
                                                           service_product_id=service_product_id) \
            [0]['service_order_id']
        check.check_that(service_order_id,
                         is_(equal_to(service_order_id_2)),
                         step=u'Проверяем, что service_order_id одинаковы',
                         error=u'разные service_order_id!')

    @pytest.mark.parametrize('test_data', Data.subscription_type,
                             ids=DataObject.ids_product_name)
    def test_trial_and_introductory_without_single_purchase(self, test_data):
        product = test_data.copy()
        with check_mode(CheckMode.IGNORED):
            status_code = simple.create_service_product(service,
                                                        active_until_dt=product['active_until_dt'],
                                                        name=product['name'],
                                                        subs_period=product['subs_period'],
                                                        subs_trial_period=product.get('subs_trial_period'),
                                                        subs_introductory_period=product.get(
                                                            'subs_introductory_period'),
                                                        subs_introductory_period_prices=product.get(
                                                            'subs_introductory_period_prices'),
                                                        type_=product['type_'])['status_code']
        check.check_that(status_code, equal_to('invalid_single_purchase'),
                         step=u'Проверяем, что вернулось верное сообщение об ошибке',
                         error=u'Вернулось неверное сообщение об ошибке')

    @pytest.mark.parametrize('test_data', Data.test_data_next_charge,
                             ids=DataObject.ids)
    def test_check_next_charge_payment_method_id(self, test_data, create_and_stop_subscription_finally):
        """
        Проверка прогнозирования способа оплаты для следующего подписочного платежа
        Кейсы 10 -- 14 из TESTTRUST-4
        """
        paymethod, user_type = test_data.paymethod, test_data.user_type
        user = uids.get_random_of_type(user_type)

        if test_data.link_another_card:
            trust.process_binding(user, service=service, multiple=True, cards=get_card())

        orders = create_and_stop_subscription_finally(user, test_data.product_type)

        basket = simple.process_payment(service, user=user, orders=orders, paymethod=paymethod)

        check.check_that(basket['orders'][0].get('next_charge_payment_method'), is_(equal_to(paymethod.id)),
                         step=u'Проверяем, что у заказа проставился корректный next_charge_payment_method',
                         error=u'У заказа проставился некорректный '
                               u'(или вовсе не проставился, ой мамочки!) next_charge_payment_method')

        basket = simple.wait_until_subscription_continuation(service, user=user, orders=orders,
                                                             trust_payment_id=basket['trust_payment_id'])

        last_payment = basket['orders'][0]['payments'][-1]

        check.check_that(simple.check_basket(service, user=user, purchase_token=last_payment)['payment_method'],
                         is_(equal_to(paymethod.id)),
                         step=u'Проверяем, что подписка в реальности '
                              u'была оплачена методом из next_charge_payment_method',
                         error=u'Подписка в реальности была оплачена не next_charge_payment_method!'
                         )


@reporter.feature(features.Service.Music)
@reporter.story(stories.General.PhantomSubscription)
class TestPhantomSubscriptions(object):
    # https://st.yandex-team.ru/TESTTRUST-4

    @pytest.mark.parametrize('test_data', Data.test_data, ids=DataObject.ids_paymethod)
    def test_delayed_subscription_start(self, test_data, create_and_stop_subscription_finally):
        """
        Отложенный старт подписки
        Кейс 7 из TESTTRUST-4
        """
        product = defaults.ServiceProduct.Subscription.NORMAL_SINGLE_PURCHASED.copy()

        paymethod, user_type, payload = test_data.paymethod, test_data.user_type, test_data.payload
        user = uids.get_random_of_type(user_type)

        subs_begin_ts = Date.shift_date(datetime.now(), seconds=60 * 2)
        orders = create_and_stop_subscription_finally(user, product, developer_payload=payload,
                                                      subs_begin_ts=subs_begin_ts)

        basket = simple.process_payment(service, user=user, orders=orders, paymethod=paymethod)

        simple.wait_until_phantom_period_finished(service, user=user, orders=orders,
                                                  trust_payment_id=basket['trust_payment_id'],
                                                  subs_begin_ts=subs_begin_ts)
        simple.wait_until_subscription_continuation(service, user=user, orders=orders,
                                                    trust_payment_id=basket['trust_payment_id'])

    @pytest.mark.parametrize('test_data', Data.test_data_short, ids=DataObject.ids_paymethod)
    def test_stop_on_phantom_period(self, test_data, create_and_stop_subscription_finally):
        """
        Остановка во время действия фантомного периода
        Кейс 8 из TESTTRUST-4
        """
        product = defaults.ServiceProduct.Subscription.NORMAL_SINGLE_PURCHASED.copy()

        paymethod, user_type, payload = test_data.paymethod, test_data.user_type, test_data.payload
        user = uids.get_random_of_type(user_type)

        subs_begin_ts = Date.shift_date(datetime.now(), seconds=60 * 2)
        orders = create_and_stop_subscription_finally(user, product, developer_payload=payload,
                                                      subs_begin_ts=subs_begin_ts)

        basket = simple.process_payment(service, user=user, orders=orders, paymethod=paymethod)

        simple_bo.stop_subscription(service, service_order_id=orders[0]['service_order_id'], user=user)

        simple.wait_until_subs_state_do(service, user, defaults.Subscriptions.State.FINISHED,
                                        trust_payment_id=basket['trust_payment_id'])

    @pytest.mark.parametrize('test_data', Data.test_data_change_service_product,
                             ids=DataObject.ids)
    def test_update_subs_product(self, test_data, create_and_stop_subscription_finally):
        """
        Апдейт действующей подписки (изменение продукта у действующей подписки)
        Кейсы 2, 3, 4 из TESTTRUST-4
        """
        with reporter.step(u"Создаем работающую подписку"):
            product = defaults.ServiceProduct.Subscription.NORMAL_SINGLE_PURCHASED.copy()
            product.update({'subs_period': '180S'})

            paymethod, user_type, payload = test_data.paymethod, test_data.user_type, \
                                            test_data.payload,
            user = uids.get_random_of_type(user_type)

            orders = create_and_stop_subscription_finally(user, product, developer_payload=payload)

            basket = simple.process_payment(service, user=user, orders=orders, paymethod=paymethod)

            basket = simple.wait_until_subscription_continuation(service, user=user, orders=orders,
                                                                 trust_payment_id=basket['trust_payment_id'])
        with reporter.step(u"Меняем продукт у созданной подписки и смотрим, что она продлевается"):
            product_new = test_data.second_service_product_type
            paymethod_new = test_data.paymethod_new or paymethod
            orders_new = create_and_stop_subscription_finally(user, product_new, developer_payload=payload,
                                                              parent_service_order_id=orders[0]['service_order_id'])
            basket_new = simple.process_payment(service, user=user, orders=orders_new,
                                                paymethod=paymethod_new)

            simple.wait_until_phantom_period_finished(service, user=user, orders=orders_new,
                                                      trust_payment_id=basket_new['trust_payment_id'],
                                                      orders_previous=basket['orders'])
            simple.wait_until_subscription_continuation(service, user=user, orders=orders_new,
                                                        trust_payment_id=basket_new['trust_payment_id'])

        with reporter.step(u"Проверяем, что подписка со старым продуктом остановлена"):
            simple.wait_until_subs_state_do(service, user, defaults.Subscriptions.State.FINISHED,
                                            trust_payment_id=basket['trust_payment_id'])

    @pytest.mark.parametrize('test_data', Data.test_data_short, ids=DataObject.ids)
    def test_update_subs_product_when_main_has_stopped(self, test_data, create_and_stop_subscription_finally):
        """
        Апдейт остановленной подписки
        Кейс 9 из TESTTRUST-4
        """
        with reporter.step(u"Создаем работающую подписку, ждем ее продления, а затем останавливаем"):
            product = defaults.ServiceProduct.Subscription.NORMAL_SINGLE_PURCHASED.copy()
            product.update({'subs_period': '180S'})

            paymethod, user_type, payload = test_data.paymethod, test_data.user_type, \
                                            test_data.payload,
            user = uids.get_random_of_type(user_type)

            orders = create_and_stop_subscription_finally(user, product, developer_payload=payload)

            basket = simple.process_payment(service, user=user, orders=orders, paymethod=paymethod)

            basket = simple.wait_until_subscription_continuation(service, user=user, orders=orders,
                                                                 trust_payment_id=basket['trust_payment_id'])

            simple_bo.stop_subscription(service, service_order_id=orders[0]['service_order_id'], user=user)

            simple.wait_until_subs_state_do(service, user, defaults.Subscriptions.State.FINISHED,
                                            trust_payment_id=basket['trust_payment_id'])

        with reporter.step(u"Меняем продукт у созданной подписки и смотрим, что она продлевается"):
            product_new = test_data.second_service_product_type
            paymethod_new = test_data.paymethod_new or paymethod
            orders_new = create_and_stop_subscription_finally(user, product_new, developer_payload=payload,
                                                              parent_service_order_id=orders[0]['service_order_id'])
            basket_new = simple.process_payment(service, user=user, orders=orders_new,
                                                paymethod=paymethod_new)

            simple.wait_until_phantom_period_finished(service, user=user, orders=orders_new,
                                                      trust_payment_id=basket_new['trust_payment_id'],
                                                      orders_previous=basket['orders'])
            simple.wait_until_subscription_continuation(service, user=user, orders=orders_new,
                                                        trust_payment_id=basket_new['trust_payment_id'])

        with reporter.step(u"Проверяем, что подписка со старым продуктом остановлена"):
            simple.wait_until_subs_state_do(service, user, defaults.Subscriptions.State.FINISHED,
                                            trust_payment_id=basket['trust_payment_id'])

    @pytest.mark.parametrize('test_data', Data.test_data_short,
                             ids=DataObject.ids_paymethod)
    def test_update_subs_product_no_second_payment(self, test_data, create_and_stop_subscription_finally):
        """
        Попытка апдейта продукта без фактической оплаты после смены продукта.
        Апдейт не происходит
        Кейс 5 из TESTTRUST-4
        """
        with reporter.step(u"Создаем работающую подписку"):
            product = defaults.ServiceProduct.Subscription.NORMAL_SINGLE_PURCHASED.copy()
            product.update({'subs_period': '180S'})

            paymethod, user_type, payload = test_data.paymethod, test_data.user_type, test_data.payload
            user = uids.get_random_of_type(user_type)

            orders = create_and_stop_subscription_finally(user, product, developer_payload=payload)

            basket = simple.process_payment(service, user=user, orders=orders, paymethod=paymethod)

            basket = simple.wait_until_subscription_continuation(service, user=user, orders=orders,
                                                                 trust_payment_id=basket['trust_payment_id'])
        with reporter.step(u"Меняем продукт у созданной подписки, но не оплачивам ее во второй раз."):
            product_new = defaults.ServiceProduct.Subscription.NORMAL_SINGLE_PURCHASED.copy()
            create_and_stop_subscription_finally(user, product_new, developer_payload=payload,
                                                 parent_service_order_id=orders[0]['service_order_id'])

        with reporter.step(u"Проверяем, что подписка со старым продуктом по-прежнему работает"):
            simple.wait_until_subscription_continuation(service, user=user, orders=orders,
                                                        trust_payment_id=basket['trust_payment_id'],
                                                        subs_period_count=basket['orders'][0]['subs_period_count'])

    @pytest.mark.parametrize('test_data', Data.test_data_short, ids=DataObject.ids)
    def test_update_subs_product_second_payment_has_failed(self, test_data,
                                                           create_and_stop_subscription_finally):
        """
        Попытка апдейта продукта, оплата после смены продукта зафейлилась.
        Апдейт не происходит
        Кейс 6 из TESTTRUST-4
        """
        with reporter.step(u"Создаем работающую подписку"):
            product = defaults.ServiceProduct.Subscription.NORMAL_SINGLE_PURCHASED.copy()
            product.update({'subs_period': '180S'})

            paymethod, user_type, payload = test_data.paymethod, test_data.user_type, test_data.payload
            user = uids.get_random_of_type(user_type)
            trust.process_unbinding(user, service=Services.MUSIC)
            orders = create_and_stop_subscription_finally(user, product, developer_payload=payload)

            basket = simple.process_payment(service, user=user, orders=orders, paymethod=paymethod)

            basket = simple.wait_until_subscription_continuation(service, user=user, orders=orders,
                                                                 trust_payment_id=basket['trust_payment_id'])
        with reporter.step(u"Меняем продукт у созданной подписки и оплачиваем ее так, чтобы оплата зафейлилась"):
            card = get_card()
            card.update({'cvn': CVN.not_enough_funds_RC51})
            paymethod_new = TrustWebPage(Via.card(card=card))
            product_new = test_data.second_service_product_type
            orders_new = create_and_stop_subscription_finally(user, product_new, developer_payload=payload,
                                                              parent_service_order_id=orders[0]['service_order_id'])
            with check_mode(CheckMode.IGNORED):
                simple.process_payment(service, user=user, orders=orders_new,
                                       paymethod=paymethod_new, should_failed=True)
        with reporter.step(u"Проверяем, что подписка со старым продуктом по-прежнему работает"):
            simple.wait_until_subscription_continuation(service, user=user, orders=orders,
                                                        trust_payment_id=basket['trust_payment_id'],
                                                        subs_period_count=basket['orders'][0]['subs_period_count'])

    @pytest.mark.parametrize('test_data', Data.test_data_short,
                             ids=DataObject.ids_paymethod)
    def test_unstop_subscription(self, test_data, create_and_stop_subscription_finally):
        """
        Отмена отмены подписки
        Кейс 1 из TESTTRUST-4
        """
        with reporter.step(u"Создаем подписку и останавливаем ее"):
            product = defaults.ServiceProduct.Subscription.NORMAL_SINGLE_PURCHASED.copy()
            product.update({'subs_period': '180S'})
            product_id = simple.create_service_product_for_service(service, product,
                                                                   defaults.Fiscal.NDS.nds_none,
                                                                   defaults.Fiscal.fiscal_title)

            paymethod, user_type, payload = test_data.paymethod, test_data.user_type, test_data.payload
            user = uids.get_random_of_type(user_type)

            orders = create_and_stop_subscription_finally(user, product_id=product_id, developer_payload=payload)

            basket = simple.process_payment(service, user=user, orders=orders, paymethod=paymethod)

            simple_bo.stop_subscription(service, service_order_id=orders[0]['service_order_id'], user=user)

            simple.wait_until_subs_state_do(service, user, defaults.Subscriptions.State.FINISHED,
                                            trust_payment_id=basket['trust_payment_id'])

        with reporter.step(u"Меняем продукт у остановленной подписки и смотрим, что она вновь продлевается"):
            orders_new = create_and_stop_subscription_finally(user, product_id=product_id, developer_payload=payload,
                                                              parent_service_order_id=orders[0]['service_order_id'])
            basket_new = simple.process_payment(service, user=user, orders=orders_new,
                                                paymethod=paymethod, init_paymethod=False)

            simple.wait_until_phantom_period_finished(service, user=user, orders=orders_new,
                                                      trust_payment_id=basket_new['trust_payment_id'],
                                                      orders_previous=basket['orders'])
            simple.wait_until_subscription_continuation(service, user=user, orders=orders_new,
                                                        trust_payment_id=basket_new['trust_payment_id'])

        with reporter.step(u"Проверяем, что подписка со старым продуктом остановлена"):
            simple.wait_until_subs_state_do(service, user, defaults.Subscriptions.State.FINISHED,
                                            trust_payment_id=basket['trust_payment_id'])


@reporter.feature(features.Service.Music)
@reporter.story(stories.Subscriptions.AggregatedCharging)
class TestAggregatedCharging(object):
    @staticmethod
    def modify_product(product, aggregated_charging=None):
        # делаем продукт аггрегирующим если надо, а также уменьшаем интервал продления
        # чтобы за время между запусками продлятора накопилось какое-то количество
        # интервалов продления
        product = product.copy()
        product.update({'aggregated_charging': aggregated_charging,
                        'subs_period': '30S'})
        if 'subs_trial_period' in product:
            product.update({'subs_trial_period': '30S'})
        if 'subs_introductory_period' in product:
            product.update({'subs_introductory_period': '30S'})

        return product

    @pytest.mark.parametrize('product', Data.subscription_type_full, ids=DataObject.ids_product_name)
    def test_aggreagated_charging(self, product, create_and_stop_subscription_finally):
        paymethod = TrustWebPage(Via.linked_card(get_card()))
        product = self.modify_product(product, aggregated_charging=1)
        user = uids.get_random_of_type(uids.Types.random_from_all)

        orders = create_and_stop_subscription_finally(user, product)
        basket = simple.process_payment(service, user=user, orders=orders, paymethod=paymethod)
        simple.wait_until_subscription_continuation(service, user=user, orders=orders,
                                                    trust_payment_id=basket['trust_payment_id'])

        last_payment = simple.get_last_subs_payment(service, user, trust_payment_id=basket['trust_payment_id'])

        check.check_that(D(last_payment['basket_rows'][0]['quantity']), is_(greater_than(D(1))),
                         step=u'Проверяем что подписка содержит более одного аггрегированного платежа',
                         error=u'Подписка не содержит аггрегированных платежей')


if __name__ == '__main__':
    pytest.main()
