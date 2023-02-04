# coding=utf-8
import pytest

import btestlib.reporter as reporter
from btestlib.constants import Services
from btestlib.utils import CheckMode, check_mode
from simpleapi.common import logger
from simpleapi.common.payment_methods import Coupon, Subsidy
from simpleapi.common.utils import DataObject
from simpleapi.data import defaults
from simpleapi.data import features, stories
from simpleapi.data import marks
from simpleapi.data import uids_pool as uids
from simpleapi.steps import simple_steps as simple

__author__ = 'fellow'
'''
Тесты на оплаты для сервиса taxi_donate
Оплаты проходят от Яндекса таксистам и таксопаркам

Что такое субсидии и с чем их едят: (https://wiki.yandex-team.ru/taxi/burning-subsidy/)
Решается проблема горящих заказов. Когда клиент запрашивает такси, но оно не находится,
так-как поблизости нет свободных машин.
Город разбивается на зоны.
В каждой зоне прогнозируется соотношение количества водителей на один заказ.
Исходя из полученного соотношения считается размер субсидии водителю за один час нахождения
в пределах сегмета с включённым роботом в пределах зоны
Информация о субсидии выводится в Яндекс.Таксометр для заманивания водителей в "непопулярную" зону


Купоны - https://st.yandex-team.ru/TAXIBACKEND-151

'''

log = logger.get_logger()

service = Services.TAXI_DONATE
user_type = uids.Types.random_from_all

pytestmark = marks.simple_internal_logic


class Data(object):
    paymethods = [
        Coupon(),
        Subsidy()
    ]

    orders_structure = [
        defaults.Order.structure_rub_one_order,
        defaults.Order.structure_rub_two_orders,
    ]


@reporter.feature(features.Service.TaxiDonate)
class TestTaxiDonate(object):
    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('paymethod', Data.paymethods, ids=DataObject.ids_paymethod)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure, ids=DataObject.ids_orders)
    def test_base_payment_cycle(self, paymethod, orders_structure):
        user = uids.get_random_of_type(user_type)
        orders = simple.form_orders_for_create(service, user, orders_structure)
        with check_mode(CheckMode.FAILED):
            basket = simple.create_basket(service, user=user,
                                          orders=orders,
                                          paymethod_id=paymethod.id)
            basket = simple.pay_basket(service, user=user,
                                       trust_payment_id=basket['trust_payment_id'])
            simple.wait_until_payment_done(service, user=user,
                                           trust_payment_id=basket['trust_payment_id'])

            basket = simple.check_basket(service, user=user, trust_payment_id=basket['trust_payment_id'])
            orders_for_update = simple.form_orders_for_update(orders)
            simple.update_basket(service, orders=orders_for_update, user=user,
                                 trust_payment_id=basket['trust_payment_id'])
            simple.process_refund(service, trust_payment_id=basket['trust_payment_id'], basket=basket, user=user)


if __name__ == '__main__':
    pytest.main()
