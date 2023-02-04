# coding: utf-8

import pytest
from hamcrest import is_not, is_, equal_to, greater_than
from decimal import Decimal as D

import btestlib.reporter as reporter
from btestlib.constants import Services
from btestlib.utils import CheckMode, check_mode
from simpleapi.common.payment_methods import TrustWebPage, LinkedCard
from simpleapi.common.payment_methods import Via
from simpleapi.common.utils import DataObject
from simpleapi.matchers.deep_equals import deep_contains
from simpleapi.data import defaults
from simpleapi.data import features, stories
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import get_card, get_masked_number
from simpleapi.steps import check_steps as check
from simpleapi.steps import payments_api_steps as payments_api
from simpleapi.steps import simple_steps as simple
from simpleapi.steps import expected_steps as expected
from simpleapi.steps import mongo_steps as mongo

__author__ = 'fellow'

service = Services.DISK


class Data(object):
    test_disk_data = [
        DataObject(paymethod=TrustWebPage(Via.card(get_card()), in_browser=True), region_id=225, currency='RUB',
                   user_type=uids.Types.random_from_all),
        DataObject(paymethod=TrustWebPage(Via.card(get_card()), in_browser=True), region_id=84, currency='USD',
                   user_type=uids.Types.random_from_all),
        DataObject(paymethod=TrustWebPage(Via.card(get_card()), in_browser=True), region_id=983, currency='USD',
                   user_type=uids.Types.random_from_all),
        DataObject(paymethod=TrustWebPage(Via.card(get_card()), in_browser=True), region_id=225, currency='RUB',
                   user_type=uids.Types.random_autoremove),
    ]
    test_update_paymethod_data = [
        DataObject(comment=u'Update to new payment method',
                   user_type=uids.Types.random_from_all).new(
            paymethod_old=LinkedCard(card=get_card()),
            paymethod_new=LinkedCard(card=get_card())),
        DataObject(comment=u'Update to same payment method',
                   user_type=uids.Types.random_from_all).new(
            paymethod_old=LinkedCard(card=get_card()),
            paymethod_new=None),  # None means the same payment method as paymethod_old
    ]
    test_update_wrong_card_data = [
        DataObject(descr='Expired card').new(data_to_update={'expiration_month': '01',
                                                             'expiration_year': '2017'},
                                             expected_error=expected.SubscriptionBasket.update_to_invalid_paymethod()),
        DataObject(descr='Blocked card').new(data_to_update={'blocking_reason': 'Some test reason'},
                                             expected_error=expected.SubscriptionBasket.update_to_blocked_card())
    ]
    test_subs_type_data = [
        DataObject(descr=u'regular subscription').new(product=defaults.RestSubscription.NORMAL.copy()),
        DataObject(descr=u'trial subscription').new(product=defaults.RestSubscription.TRIAL.copy()),
    ]


@reporter.feature(features.Service.Disk)
@reporter.story(stories.General.Subscription)
class TestDiskGeneral(object):
    @pytest.mark.parametrize('test_data', Data.test_disk_data, ids=DataObject.ids)
    def test_normal_subscription_continuation(self, test_data):
        paymethod, region_id, currency, user_type = \
            test_data.paymethod, test_data.region_id, test_data.currency, test_data.user_type
        user = uids.get_random_of_type(user_type)
        paymethod.init(service, user)
        with payments_api.Subscriptions.create_normal(service, user, region_id) as subs:
            orders = [{"order_id": subs['order_id'], "currency": currency, 'region_id': region_id, "qty": 1}]
            with check_mode(CheckMode.FAILED):
                payments_api.Payments.process(service, paymethod, user=user, orders=orders, currency=currency,
                                              region_id=region_id)
                payments_api.Wait.until_subscription_continuation(service, user, subs['order_id'])

    @pytest.mark.parametrize('test_data', Data.test_disk_data, ids=DataObject.ids)
    def test_trial_subscription_with_paymethod_continuation(self, test_data):
        """
        Подписка с триальным периодом и с заданным способом оплаты дожна продлеваться
        """
        paymethod, region_id, currency, user_type = \
            test_data.paymethod, test_data.region_id, test_data.currency, test_data.user_type
        user = uids.get_random_of_type(user_type)
        with payments_api.Subscriptions.create_trial(service, user, region_id) as subs:
            orders = [{"order_id": subs['order_id'], "currency": currency, 'region_id': region_id, "qty": 1}]
            with check_mode(CheckMode.FAILED):
                payments_api.Payments.process(service, paymethod, user=user, orders=orders, currency=currency,
                                              region_id=region_id)
                payments_api.Wait.until_trial_subscription_continuation(service, user, subs['order_id'])
                payments_api.Wait.until_subscription_continuation_one_more_time(service, user, subs['order_id'])

    @pytest.mark.skipif(True, reason="See TRUST-4702")
    @pytest.mark.parametrize('test_data', Data.test_disk_data, ids=DataObject.ids)
    def test_trial_subscription_without_paymethod_fail_continuation(self, test_data):
        """
        Подписка с триальным периодом и без заданного способа оплаты дожна останавливаться
        """
        paymethod, region_id, currency, user_type = \
            test_data.paymethod, test_data.region_id, test_data.currency, test_data.user_type
        user = uids.get_random_of_type(user_type)

        with payments_api.Subscriptions.create_trial(service, user, region_id) as subs:
            orders = [{"order_id": subs['order_id'], "currency": currency, 'region_id': region_id, "qty": 1}]
            with check_mode(CheckMode.FAILED):
                payments_api.Payments.process(service, paymethod, user=user, orders=orders, currency=currency,
                                              region_id=region_id)
                payments_api.Wait.until_subscription_finished(service, user, subs['order_id'])


@reporter.feature(features.Service.Disk)
@reporter.story(stories.Subscriptions.SpecialRules)
class TestDiskSpecialRules(object):
    @pytest.mark.parametrize('test_data', Data.test_disk_data, ids=DataObject.ids)
    def test_short_lived_subscription(self, test_data):
        """
        Если истекает active_until_dt в подписочном продукте, то заказ дальше не продлевается
        """
        paymethod, region_id, currency, user_type = \
            test_data.paymethod, test_data.region_id, test_data.currency, test_data.user_type
        user = uids.get_random_of_type(user_type)
        paymethod.init(service, user)
        with payments_api.Subscriptions.create_short_lived(service, user, region_id) as subs:
            orders = [{"order_id": subs['order_id'], "currency": currency, 'region_id': region_id, "qty": 1}]
            with check_mode(CheckMode.FAILED):
                payments_api.Payments.process(service, paymethod, user=user, orders=orders, currency=currency,
                                              region_id=region_id)
                # вначале подписка продляется, до того как истек active_until_dt ...
                payments_api.Wait.until_subscription_continuation(service, user, subs['order_id'])
                # ... а затем останавливается
                payments_api.Wait.until_subscription_finished(service, user, subs['order_id'])

    @pytest.mark.parametrize('test_data', Data.test_disk_data, ids=DataObject.ids)
    def test_single_purchase_1_work(self, test_data):
        """
        Для некотрых сервисов (например Диск) признак single_purchase отключен
        Для Диска, например, может быть несколько подписок на 10GB
        """
        paymethod, region_id, currency, user_type = \
            test_data.paymethod, test_data.region_id, test_data.currency, test_data.user_type
        user = uids.get_random_of_type(user_type)
        with payments_api.Subscriptions.create_two_normal_orders(service, user, region_id) as dict_value:
            order_id_1, order_id_2 = dict_value['order_id_1'], dict_value['order_id_2']
            orders_1 = [{"order_id": order_id_1, "currency": currency, 'region_id': region_id, "qty": 1}]
            orders_2 = [{"order_id": order_id_2, "currency": currency, 'region_id': region_id, "qty": 1}]
            with check_mode(CheckMode.FAILED):
                payments_api.Payments.process(service, paymethod, user=user, orders=orders_1, currency=currency,
                                              region_id=region_id)
                payments_api.Payments.process(service, paymethod, user=user, orders=orders_2, currency=currency,
                                              region_id=region_id)
                payments_api.Wait.until_subscription_continuation(service, user, order_id_1)
                payments_api.Wait.until_subscription_continuation(service, user, order_id_2)

    def test_link_card_after_subscribe(self):
        user = uids.get_random_of(uids.all_)
        with reporter.step('Оплачиваем подписку и проверяем что карта оплаты привызалась к пользователю'):
            card = get_card()
            paymethod, region_id, currency = TrustWebPage(Via.card(card)), 225, 'RUB'
            with payments_api.Subscriptions.create_normal(service, user, region_id) as subs:
                orders = [{"order_id": subs['order_id'], "currency": currency, 'region_id': region_id, "qty": 1}]

                with check_mode(CheckMode.FAILED):
                    payments_api.Payments.process(service, paymethod, user=user, orders=orders, currency=currency)

                linked_card = simple.find_card_by_masked_number(service, user=user,
                                                                number=get_masked_number(card['card_number']),
                                                                list_payment_methods_callback=payments_api.PaymentMethods.get)
                check.check_that(linked_card, is_not(None),
                                 step=u'Проверяем что у пользователя появилась привязаннвя карта',
                                 error=u'У пользователя не появилось привязанной карты')
        with reporter.step('Оплачиваем привязанной картой новый заказ'):
            with payments_api.Subscriptions.create_normal(service, user, region_id) as subs:
                paymethod, region_id, currency = (LinkedCard(card=card), 225, 'RUB')
                orders = [{"order_id": subs['order_id'], "currency": currency, 'region_id': region_id, "qty": 1}]
                payments_api.Payments.process(service, paymethod, user=user, orders=orders, currency=currency)

    @pytest.mark.parametrize('test_data', Data.test_update_paymethod_data, ids=DataObject.ids)
    def test_update_next_charge_payment_method(self, test_data):
        """
        По мотивам: https://st.yandex-team.ru/TRUST-6123
        """
        user = uids.get_random_of_type(test_data.user_type)
        paymethod_old, paymethod_new = test_data.paymethod_old, test_data.paymethod_new

        with payments_api.Subscriptions.create_normal(service, user) as subs:
            orders = [{"order_id": subs['order_id'], "currency": 'RUB', 'region_id': 225, "qty": 1}]
            payments_api.Payments.process(service, paymethod_old, user=user, orders=orders)
            # todo: пусть будет пока что так, для скорости работы
            # вообще надо разобраться глобально когда нужно/не нужно ждать предварительного продления
            # payments_api.Wait.until_subscription_continuation(service, user, order_id)

            if paymethod_new:
                paymethod_new.init(service, user)
            else:
                paymethod_new = paymethod_old

            payments_api.Subscriptions.update(service, user, subs['order_id'], paymethod=paymethod_new)

            next_charge_paymethod = payments_api.Subscriptions.get(service, user,
                                                                   subs['order_id'])['next_charge_payment_method']
            check.check_that(next_charge_paymethod, is_(equal_to(paymethod_new.id)),
                             step=u'Проверяем что после изменения способа оплаты подписки '
                                  u'новый способ корректно отображается в next_charge_payment_method',
                             error=u'Некорректный способ оплаты в next_charge_payment_method '
                                   u'после изменения способа оплаты в подписке')

            payments_api.Wait.until_subscription_continuation_one_more_time(service, user, subs['order_id'])
            basket = payments_api.Subscriptions.get_last_payment(service, user, subs['order_id'])
            check.check_that(basket['paymethod_id'], is_(equal_to(paymethod_new.id)),
                             step=u'Проверяем что последний платеж по подписке '
                                  u'был проведен с новым способом оплаты',
                             error=u'Последний платеж по подписке был проведен с некорректным способом оплаты')

    def test_update_payment_method_to_nonexistent(self):
        """
        По мотивам: https://st.yandex-team.ru/TRUST-6124
        Кейс когда передаем несуществующий paymethod_id
        """
        user = uids.get_random_of_type(uids.Types.random_from_all)
        paymethod = LinkedCard(card=get_card())
        paymethod_invalid = LinkedCard(_id='card-x123456a1b2cd3e456ff7g89e')

        with payments_api.Subscriptions.create_normal(service, user) as subs:
            orders = [{"order_id": subs['order_id'], "currency": 'RUB', 'region_id': 225, "qty": 1}]
            payments_api.Payments.process(service, paymethod, user=user, orders=orders)
            # todo: пусть будет пока что так, для скорости работы
            # вообще надо разобраться глобально когда нужно/не нужно ждать предварительного продления
            # payments_api.Wait.until_subscription_continuation(service, user, order_id)

            with check_mode(CheckMode.IGNORED):
                resp = payments_api.Subscriptions.update(service, user, subs['order_id'], paymethod=paymethod_invalid)
                check.check_that(resp, deep_contains(expected.SubscriptionBasket.update_to_invalid_paymethod()),
                                 step=u'Проверяем что при попытке изменить способ оплаты в подписке на несуществующий '
                                      u'возвращается говорящая ошибка',
                                 error=u'Некорректная ошибка при попытке изменить способ оплаты в подписке '
                                       u'на несуществующий')

    @pytest.mark.parametrize('test_data', Data.test_update_wrong_card_data, ids=DataObject.ids)
    def test_update_payment_method_to_wrong(self, test_data):
        """
        По мотивам: https://st.yandex-team.ru/TRUST-6124
        Кейс когда передаем просроченную карту или заблокированную карту
        """
        user = uids.get_random_of_type(uids.Types.random_from_all)
        paymethod = LinkedCard(card=get_card())

        paymethod_invalid = LinkedCard(card=get_card())
        paymethod_invalid.init(service, user)
        mongo.Card.update_data(trust_payment_id=paymethod_invalid.binding_trust_payment_id,
                               data_to_update=test_data.data_to_update)

        with payments_api.Subscriptions.create_normal(service, user) as subs:
            orders = [{"order_id": subs['order_id'], "currency": 'RUB', 'region_id': 225, "qty": 1}]
            payments_api.Payments.process(service, paymethod, user=user, orders=orders)
            # todo: пусть будет пока что так, для скорости работы
            # вообще надо разобраться глобально когда нужно/не нужно ждать предварительного продления
            # payments_api.Wait.until_subscription_continuation(service, user, order_id)

            with check_mode(CheckMode.IGNORED):
                resp = payments_api.Subscriptions.update(service, user, subs['order_id'], paymethod=paymethod_invalid)
                check.check_that(resp, deep_contains(test_data.expected_error),
                                 step=u'Проверяем что при попытке изменить способ оплаты в подписке на некорректный '
                                      u'(просроченная или заблокированная карта) возвращается говорящая ошибка',
                                 error=u'Некорректная ошибка при попытке изменить способ оплаты в подписке '
                                       u'на некорректный (просроченная или заблокированная карта)')

    def test_update_on_trial_period(self):
        """
        По мотивам: https://st.yandex-team.ru/TRUST-6139
        """
        user = uids.get_random_of_type(uids.Types.random_from_all)

        paymethod = LinkedCard(card=get_card())
        paymethod_new = LinkedCard(card=get_card())
        paymethod_new.init(service, user)

        with payments_api.Subscriptions.create_trial(service, user) as subs:
            orders = [{"order_id": subs['order_id'], "currency": 'RUB', 'region_id': 225, "qty": 1}]
            payments_api.Payments.process(service, paymethod, user=user, orders=orders)

            payments_api.Subscriptions.update(service, user, subs['order_id'], paymethod=paymethod_new)

            with reporter.step(u'Ждем пока у подписки пройдет триальный период и период с обычным списанием'):
                payments_api.Wait.until_trial_subscription_continuation(service, user, subs['order_id'])
                payments_api.Wait.until_subscription_continuation_one_more_time(service, user, subs['order_id'])

            basket = payments_api.Subscriptions.get_last_payment(service, user, subs['order_id'])
            check.check_that(basket['paymethod_id'], is_(equal_to(paymethod_new.id)),
                             step=u'Проверяем что последний платеж по подписке '
                                  u'был проведен с новым способом оплаты',
                             error=u'Последний платеж по подписке был проведен с некорректным способом оплаты')


@reporter.feature(features.Service.Disk)
@reporter.story(stories.Subscriptions.AggregatedCharging)
class TestDiskAggregatedCharging(object):
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
        return product

    @pytest.mark.parametrize('test_data', Data.test_subs_type_data, ids=DataObject.ids)
    def test_aggregated_charging(self, test_data):
        paymethod = TrustWebPage(Via.card(get_card()))
        user = uids.get_random_of_type(uids.Types.random_from_all)
        product = self.modify_product(test_data.product, aggregated_charging=1)

        with payments_api.Subscriptions.create_by_product(service, user, product) as subs:
            orders = [{"order_id": subs['order_id'], "currency": 'RUB', 'region_id': 225, "qty": 1}]
            payments_api.Payments.process(service, paymethod, user=user, orders=orders)
            payments_api.Wait.until_subscription_continuation(service, user, subs['order_id'])

            last_payment = payments_api.Subscriptions.get_last_payment(service, user, subs['order_id'])
            check.check_that(D(last_payment['basket_rows'][0]['quantity']), is_(greater_than(D(1))),
                             step=u'Проверяем что подписка содержит более одного аггрегированного платежа',
                             error=u'Подписка не содержит аггрегированных платежей')

    @pytest.mark.parametrize('test_data', Data.test_subs_type_data, ids=DataObject.ids)
    def test_update_to_aggregated_charging(self, test_data):
        """
        Создается подписка без aggregated_charging
        Ждется обычное продление
        Апдейтится продукт: aggregated_charging=1
        Ждется еще одно продление и проверяется что последний платеж - аггрегирующий
        """
        paymethod = TrustWebPage(Via.card(get_card()))
        user = uids.get_random_of_type(uids.Types.random_from_all)
        product = self.modify_product(test_data.product)

        with payments_api.Subscriptions.create_by_product(service, user, product) as subs:
            orders = [{"order_id": subs['order_id'], "currency": 'RUB', 'region_id': 225, "qty": 1}]
            payments_api.Payments.process(service, paymethod, user=user, orders=orders)
            payments_api.Wait.until_subscription_continuation(service, user, subs['order_id'])

            last_payment = payments_api.Subscriptions.get_last_payment(service, user, subs['order_id'])
            check.check_that(D(last_payment['basket_rows'][0]['quantity']), is_(equal_to(D(1))),
                             step=u'Проверяем что у подписки нет аггрегирующих платежей',
                             error=u'У подписки присутствуют аггрегирующие платежи, '
                                   u'хотя сама подписка не аггрегирующая')

            product.update({'aggregated_charging': 1})
            payments_api.Products.update(service, user, product_id=subs.get('product_id'), **product)

            payments_api.Wait.until_subscription_continuation_one_more_time(service, user, subs['order_id'])
            last_payment = payments_api.Subscriptions.get_last_payment(service, user, subs['order_id'])
            check.check_that(D(last_payment['basket_rows'][0]['quantity']), is_(greater_than(D(1))),
                             step=u'Проверяем что подписка содержит более одного аггрегированного платежа',
                             error=u'Подписка не содержит аггрегированных платежей')

    @pytest.mark.parametrize('test_data', Data.test_subs_type_data, ids=DataObject.ids)
    def test_update_to_non_aggregated_charging(self, test_data):
        """
        Создается подписка с aggregated_charging=1
        Ждется продление
        Апдейтится продукт: aggregated_charging=0
        Ждется еще одно продление и проверяется что последний платеж - НЕ аггрегирующий
        """
        paymethod = TrustWebPage(Via.card(get_card()))
        user = uids.get_random_of_type(uids.Types.random_from_all)
        product = self.modify_product(test_data.product, aggregated_charging=1)

        with payments_api.Subscriptions.create_by_product(service, user, product) as subs:
            orders = [{"order_id": subs['order_id'], "currency": 'RUB', 'region_id': 225, "qty": 1}]
            payments_api.Payments.process(service, paymethod, user=user, orders=orders)
            payments_api.Wait.until_subscription_continuation(service, user, subs['order_id'])

            product.update({'aggregated_charging': 0})
            payments_api.Products.update(service, user, product_id=subs.get('product_id'), **product)

            payments_api.Wait.until_subscription_continuation_one_more_time(service, user, subs['order_id'])
            last_payment = payments_api.Subscriptions.get_last_payment(service, user, subs['order_id'])
            check.check_that(D(last_payment['basket_rows'][0]['quantity']), is_(equal_to(D(1))),
                             step=u'Проверяем что у подписки нет аггрегирующих платежей',
                             error=u'У подписки присутствуют аггрегирующие платежи, '
                                   u'хотя сама подписка не аггрегирующая')
