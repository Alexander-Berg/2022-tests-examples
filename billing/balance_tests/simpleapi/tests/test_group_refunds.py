# coding=utf-8
import pytest
from hamcrest import equal_to

import btestlib.reporter as reporter
from btestlib.constants import Services
from simpleapi.common import logger
from simpleapi.common.payment_methods import LinkedCard
from simpleapi.common.payment_methods import TrustWebPage, Via
from simpleapi.common.utils import DataObject
from simpleapi.data import features, stories
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import get_card
from simpleapi.steps import check_steps as check
from simpleapi.steps import db_steps
from simpleapi.steps import simple_steps as simple

__author__ = 'slppls'

log = logger.get_logger()

"""
https://st.yandex-team.ru/TRUST-1655
Кейсы, при которых срабатывает групповой рефанд:
1) заказ оплачен с карты из 1 строчки. Сначала ресайз - появляется рефанд, затем отменяем оставшуюся часть.
В результате появляется второй рефанд. Затем они группируются в один групповой.

2) заказ оплачен кошельком ЯД на несколько строк. Отменяется одна строка заказа. Создается рефанд. Отменяется другая.
Создается рефанд. Затем клирим оставшуюся часть. В результате имеется групповой рефанд на сумму первых двух отмен.

Возможно в скором времени даже одиночные рефанды будут группироваться в групповой рефанд, но пока такового функционала
нет. Потенциальное вероятное TODO.
"""


class Data(object):
    test_data = [
        DataObject(service=Services.TAXI,
                   paymethod=LinkedCard(card=get_card()),
                   user_type=uids.Types.random_from_all,
                   orders_structure=({'currency': 'RUB', 'price': 1000},)
                   ).new(clearing_plan=[{'amount': '700'},
                                        {'action': 'cancel'}],
                         expected_refund=[{'amount': 300, 'type': 'REFUND'},
                                          {'amount': 700, 'type': 'REFUND'},
                                          {'amount': 1000, 'type': 'REFUND_GROUP'}]),
        pytest.mark.yamoney(DataObject(service=Services.TICKETS,
                                       paymethod=TrustWebPage(Via.yandex_money()),
                                       user_type=uids.Type(pool=uids.secret, name='test_wo_proxy_old'),
                                       orders_structure=({'currency': 'RUB', 'price': 300},
                                                         {'currency': 'RUB', 'price': 300},
                                                         {'currency': 'RUB', 'price': 400})
                                       ).new(clearing_plan=[{'action': 'cancel'},
                                                            {'action': 'cancel'},
                                                            {'action': 'clear'}],
                                             expected_refund=[{'amount': 300, 'type': 'REFUND'},
                                                              {'amount': 300, 'type': 'REFUND'},
                                                              {'amount': 600, 'type': 'REFUND_GROUP'}]))
    ]


@reporter.feature(features.General.GroupRefund)
@reporter.story(stories.General.Refund)
class TestGroupRefunds(object):
    @pytest.mark.parametrize('test_data', Data.test_data, ids=DataObject.ids)
    def test_group_refunds(self, test_data):
        service, paymethod, user_type = test_data.service, test_data.paymethod, test_data.user_type
        user = uids.get_random_of_type(user_type)
        orders_structure, update_action, expected_refund = \
            test_data.orders_structure, test_data.clearing_plan, test_data.expected_refund
        orders = simple.form_orders_for_create(service, user, orders_structure)

        trust_payment_id = simple.process_payment(service, user=user,
                                                  orders=orders, paymethod=paymethod)['trust_payment_id']

        orders_for_update = simple.form_orders_for_update(orders, update_action)
        for order in orders_for_update:
            simple.update_basket(service, orders=[order], user=user,
                                 trust_payment_id=trust_payment_id)

        actual_refund = db_steps.bs().get_info_for_group_refund(trust_payment_id)
        check.check_that(actual_refund, equal_to(expected_refund),
                         step=u'Проверяем что корректно создался групповой возврат',
                         error=u'Некорректно создался групповой возврат')


if __name__ == '__main__':
    pytest.main()
