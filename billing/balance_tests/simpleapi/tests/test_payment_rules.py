# coding=utf-8
import pytest

import btestlib.reporter as reporter
from btestlib.constants import Services
from simpleapi.common.utils import DataObject, find_dict_in_list
from simpleapi.data import defaults
from simpleapi.data import features, stories
from simpleapi.data import uids_pool as uids
from simpleapi.matchers.deep_equals import deep_contains
from simpleapi.steps import check_steps as check
from simpleapi.steps import expected_steps as expected
from simpleapi.steps import simple_steps as simple

__author__ = 'fellow'


class Data(object):
    default_qty = 10

    general_data = [
        # для нового маркета и сервисов с trust_price=1 должно учитываться qty, передаваемое в заказ
        DataObject(service=Services.NEW_MARKET).new(expected_qty=default_qty),
        DataObject(service=Services.YAC).new(expected_qty=default_qty),
        # для остальных сервисов qty не учитывается
        DataObject(service=Services.MARKETPLACE).new(expected_qty=0),
        DataObject(service=Services.TICKETS).new(expected_qty=0),
        DataObject(service=Services.TAXI).new(expected_qty=0)
    ]

    orders_structure = [
        ({'currency': 'RUB', 'price': defaults.Order.price, 'qty': default_qty},),
        ({'currency': 'RUB', 'price': defaults.Order.price, 'qty': default_qty},
         {'currency': 'RUB', 'price': defaults.Order.price, 'qty': default_qty}),
    ]


@reporter.feature(features.Rules.Payment)
class TestQtyRules(object):
    @reporter.story(stories.Rules.Qty)
    @pytest.mark.parametrize('general_data', Data.general_data, ids=DataObject.ids)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure, ids=DataObject.ids_orders)
    def test_qty_rules(self, general_data, orders_structure):
        """
        https://st.yandex-team.ru/TRUST-3164
        """
        service, expected_qty = general_data.service, general_data.expected_qty
        user = uids.get_random_of(uids.all_)

        orders = simple.form_orders_for_create(service, user, orders_structure=orders_structure)
        basket = simple.process_payment(service, user=user, orders=orders, need_postauthorize=True)

        for basket_order in basket['orders']:
            expected_order = find_dict_in_list(orders, service_order_id=basket_order['service_order_id'])
            check.check_that(basket_order,
                             deep_contains(expected.RegularOrder.order_with_qty(order=expected_order,
                                                                                expected_qty=expected_qty)),
                             step=u'Проверяем что в заказе присутствует корректное qty',
                             error=u'Некорректное значение qty в заказе')


if __name__ == '__main__':
    pytest.main()
