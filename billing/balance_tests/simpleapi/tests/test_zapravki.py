# coding=utf-8
import pytest
from hamcrest import equal_to

import btestlib.reporter as reporter
from btestlib.constants import Services
from btestlib.utils import CheckMode, check_mode
from simpleapi.common.payment_methods import LinkedCard
from simpleapi.common.utils import DataObject
from simpleapi.data import defaults
from simpleapi.data import features, stories
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import get_card
from simpleapi.steps import check_steps as check
from simpleapi.steps import payments_api_steps as payments_api

__author__ = 'sunshineguy'

service = Services.GAS_STATIONS


class Data(object):
    default_qty = 10
    test_data = [
        DataObject(paymethod=LinkedCard(card=get_card()),
                   user_type=uids.Types.random_from_test_passport)
    ]
    orders_structure = [
        ({'currency': 'RUB', 'price': defaults.Order.price, 'fiscal_nds': defaults.Fiscal.NDS.nds_none,
         'fiscal_title': defaults.Fiscal.fiscal_title, 'qty': default_qty},)
    ]


@reporter.feature(features.Service.Zapravki)
class TestZapravki(object):
    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('test_data', Data.test_data, ids=DataObject.ids)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure, ids=DataObject.ids_orders)
    def test_base_payment_cycle(self, test_data, orders_structure):
        user = uids.get_random_of_type(test_data.user_type)
        with check_mode(CheckMode.FAILED):
            basket = payments_api.Payments.process(service, paymethod=test_data.paymethod, user=user,
                                                   orders_structure=orders_structure,
                                                   need_clearing=True)
            payments_api.Refunds.process(service, user, basket['purchase_token'])
        check.check_that(float(basket['orders'][0]['current_qty']),
                         equal_to(float(orders_structure[0]['qty'])),
                         step=u'Проверяем, что количество указано верно',
                         error=u'Количество указано неверно!')
        check.check_that(float(basket['amount']),
                         equal_to(orders_structure[0]['qty'] * orders_structure[0]['price']),
                         step=u'Проверяем, что цена рассчитана верно',
                         error=u'Цена рассчитана неверно!')
