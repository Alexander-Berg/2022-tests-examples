# coding=utf-8
import pytest

import btestlib.reporter as reporter
from btestlib.constants import Services
from btestlib.utils import CheckMode, check_mode
from simpleapi.common.payment_methods import LinkedCard
from simpleapi.common.utils import DataObject
from simpleapi.data import features, stories, defaults
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import Sberbank
from simpleapi.steps import payments_api_steps as payments_api

__author__ = 'fellow'

service = Services.DRIVE

'''
Общался с @carabas
Во время поездки делают поминутный холд (условно каждые 10 минут холдят 100 рублей на карте)
Потом все это клирят через 2 дня
Рефанды делают через наш саппорт, либо возмещают бонусными баллами без похода к нам (этот способ у них в проритете)
Бывают еще разовые штрафные платежи (за штрафы гибдд, как я понял), но для нас это обычный платеж
В приложении обязательно надо привязать карту, поэтому платежи LinkedCard
У них всегда одна страчка в заказе, нам передают product_id и amount
Но он интересовался про возможность передавать несколько ордеров напрямую, поэтому добавил и этот сценарий тоже
'''
PAYMENTS_COUNT = 2  # Количество платежей в тесте на мультиплатежи


class Data(object):
    test_data = [
        (DataObject(paymethod=LinkedCard(card=Sberbank.Success.Without3DS.card_visa,
                                         list_payment_methods_callback=payments_api.PaymentMethods.get),
                    user_type=uids.Types.random_from_test_passport)),
    ]

    orders_structure = [
        [{'region_id': 225, 'currency': 'RUB', 'price': 10},
         {'region_id': 225, 'currency': 'RUB', 'price': 20.5},
         ],
        None
    ]
    orders_structure_multipayment = [
        defaults.Order.structure_rub_one_order,
        defaults.Order.structure_rub_two_orders,
    ]


@reporter.feature(features.Service.Carsharing)
class TestCarsharing(object):
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

    @staticmethod
    def base_start_multiply_cycle(user_type, orders_structure, paymethod):
        user = uids.get_random_of_type(user_type)
        paymethod.init(service, user)
        payments = []
        orders = payments_api.Form.orders_for_payment(service=service,
                                                      user=user,
                                                      orders_structure=orders_structure)
        for _ in range(PAYMENTS_COUNT):
            basket = payments_api.Payments.process(service, paymethod,
                                                   user, orders=orders)
            payments.append(basket['purchase_token'])
        return user, payments

    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('test_data', Data.test_data,
                             ids=DataObject.ids)
    @pytest.mark.parametrize('orders_structure',
                             Data.orders_structure_multipayment,
                             ids=DataObject.ids_orders)
    def test_base_multipayment_cycle_with_refund(self, test_data,
                                                 orders_structure):
        user, payments = \
            TestCarsharing.base_start_multiply_cycle(test_data.user_type,
                                                     orders_structure,
                                                     test_data.paymethod)
        # клирим все платежи
        for payment in payments:
            payments_api.Payments.clear(service=service, user=user,
                                        purchase_token=payment)
            payments_api.Wait.until_clearing_done(service, user,
                                                  purchase_token=payment)
        # а затем рефандим все платежи
        for payment in payments:
            payments_api.Refunds.process(service, user, payment)

    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('test_data', Data.test_data,
                             ids=DataObject.ids)
    @pytest.mark.parametrize('orders_structure',
                             Data.orders_structure_multipayment,
                             ids=DataObject.ids_orders)
    def test_base_multipayment_cycle_with_unhold(self, test_data,
                                                 orders_structure):
        user, payments = \
            TestCarsharing.base_start_multiply_cycle(test_data.user_type,
                                                     orders_structure,
                                                     test_data.paymethod)
        # делаем unhold всех платежей
        for payment in payments:
            payments_api.Payments.unhold(service, user, payment)
            payments_api.Wait.until_payment_cancelled(service, user, payment)


if __name__ == '__main__':
    pytest.main()
