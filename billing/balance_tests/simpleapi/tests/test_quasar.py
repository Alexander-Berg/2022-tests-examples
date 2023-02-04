# coding=utf-8
import pytest
from hamcrest import is_not

import btestlib.reporter as reporter
from btestlib.constants import Services
from btestlib.utils import CheckMode, check_mode
from simpleapi.common.payment_methods import LinkedCard, Card
from simpleapi.common.utils import DataObject
from simpleapi.data import features, stories, defaults
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import get_card, get_masked_number
from simpleapi.matchers.deep_equals import deep_equals_ignore_order_to
from simpleapi.steps import check_steps as check
from simpleapi.steps import payments_api_steps as payments_api, simple_steps

__author__ = 'slppls'

service = Services.QUASAR


class Data(object):
    test_data = [
        (DataObject(paymethod=LinkedCard(card=get_card(),
                                         list_payment_methods_callback=payments_api.PaymentMethods.get),
                    user_type=uids.Types.random_from_test_passport)),
        (DataObject(paymethod=LinkedCard(card=get_card(), from_linked_phonish=True,
                                         list_payment_methods_callback=payments_api.PaymentMethods.get),
                    user_type=uids.Types.random_with_linked_phonishes)),
    ]
    orders_structure = [
        [{'region_id': 225, 'currency': 'RUB', 'price': 10},
         {'region_id': 225, 'currency': 'RUB', 'price': 20.5},
         ],
        None
    ]
    test_data_subscription = [
        (DataObject(paymethod=LinkedCard(card=get_card(),
                                         list_payment_methods_callback=payments_api.PaymentMethods.get),
                    region_id=225, currency='RUB',
                    user_type=uids.Types.random_from_all))]
    # TRUST может принимать любую строку, но передавая на число
    # мы можем убить партнерку так как ожидается число.
    # расчет комиссии исходя из документации: 2% - 200 1.25% - 125 etc
    commission_category = [
        [500, 100],
        [500],
        []
    ]


@reporter.feature(features.Service.Quasar)
class TestQuasar(object):
    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('test_data', Data.test_data, ids=DataObject.ids)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure, ids=DataObject.ids_orders)
    def test_base_payment_cycle_with_refund(self, test_data, orders_structure):
        paymethod, user_type = test_data.paymethod, test_data.user_type
        user = uids.get_random_of_type(user_type)

        with check_mode(CheckMode.FAILED):
            basket = payments_api.Payments.process(service, paymethod=paymethod, user=user,
                                                   orders_structure=orders_structure, need_clearing=True)

            payments_api.Refunds.process(service, user, basket['purchase_token'])

    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('test_data', Data.test_data, ids=DataObject.ids)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure, ids=DataObject.ids_orders)
    def test_base_payment_cycle_with_unhold(self, test_data, orders_structure):
        paymethod, user_type = test_data.paymethod, test_data.user_type
        user = uids.get_random_of_type(user_type)

        with check_mode(CheckMode.FAILED):
            basket = payments_api.Payments.process(service, paymethod=paymethod, user=user,
                                                   orders_structure=orders_structure)
            payments_api.Payments.unhold(service, user, basket['purchase_token'])
            payments_api.Wait.until_payment_cancelled(service, user, basket['purchase_token'])

    @pytest.mark.parametrize('test_data', Data.test_data_subscription, ids=DataObject.ids)
    def test_normal_subscription_continuation(self, test_data):
        paymethod, user_type, currency, region_id = \
            test_data.paymethod, test_data.user_type, test_data.currency, test_data.region_id
        user = uids.get_random_of_type(user_type)
        paymethod.init(service, user)
        with payments_api.Subscriptions.create_normal(service, user, region_id) as subs:
            orders = [{"order_id": subs['order_id'], "currency": currency, 'region_id': region_id, "qty": 1}]
            with check_mode(CheckMode.FAILED):
                payments_api.Payments.process(service, paymethod, user=user, orders=orders,
                                              region_id=region_id)
                payments_api.Wait.until_subscription_continuation(service, user, subs['order_id'])

    def test_link_card_after_subscribe(self):
        user = uids.get_random_of(uids.all_)
        with reporter.step('Оплачиваем подписку и проверяем что карта оплаты привызалась к пользователю'):
            card = get_card()
            paymethod, region_id, currency = (Card(card), 225, 'RUB')
            with payments_api.Subscriptions.create_normal(service, user, region_id) as subs:
                orders = [{"order_id": subs['order_id'], "currency": currency, 'region_id': region_id, "qty": 1}]
                with check_mode(CheckMode.FAILED):
                    payments_api.Payments.process(service, paymethod, user=user, orders=orders, currency=currency)
                linked_card = simple_steps.find_card_by_masked_number(service=service, user=user,
                                                                      number=get_masked_number(
                                                                          card['card_number']),
                                                                      list_payment_methods_callback=payments_api.PaymentMethods.get)
                check.check_that(linked_card, is_not(None),
                                 step=u'Проверяем что у пользователя появилась привязаннвя карта',
                                 error=u'У пользователя не появилось привязанной карты')
        with reporter.step('Оплачиваем привязанной картой новый заказ'):
            with payments_api.Subscriptions.create_normal(service, user, region_id) as subs:
                orders[0]['order_id'] = subs['order_id']
                payments_api.Payments.process(service, paymethod, user=user, orders=orders, currency=currency)

    @pytest.mark.parametrize('test_data', Data.test_data, ids=DataObject.ids)
    @pytest.mark.parametrize('commission_category', Data.commission_category,
                             ids=DataObject.ids)
    def test_commission_category_to_orders(self, commission_category,
                                           test_data):
        paymethod, user_type = test_data.paymethod, test_data.user_type
        orders_structure = defaults.Order.structure_rub_two_orders
        user = uids.get_random_of_type(user_type)
        with check_mode(CheckMode.FAILED):
            orders = payments_api.Payments.process(service,
                                                   paymethod=paymethod,
                                                   user=user,
                                                   orders_structure=orders_structure,
                                                   need_clearing=True,
                                                   commission_category_list=commission_category)['orders']
        return_commission_category = list()
        for order in orders:
            if order.get('commission_category'):
                return_commission_category.append(
                    int(order['commission_category']))
        check.check_that(return_commission_category,
                         deep_equals_ignore_order_to(commission_category),
                         step=u'Проверяем, что поле commission_category'
                              u'проставленно корректно и оно валидное',
                         error=u'ALARM! Поле commission_category проставленно'
                               u' некорректно и оно не валидно')


if __name__ == '__main__':
    pytest.main()
