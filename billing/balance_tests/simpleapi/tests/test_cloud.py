# coding=utf-8
import pytest
from hamcrest import is_, none

import btestlib.reporter as reporter
from btestlib.constants import Services
from simpleapi.common.payment_methods import TrustWebPage, Via
from simpleapi.common.utils import DataObject
from simpleapi.data import defaults
from simpleapi.data import features, stories
from simpleapi.data import marks
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import get_card, get_masked_number
from simpleapi.steps import check_steps as check
from simpleapi.steps import simple_steps as simple

__author__ = 'slppls'

service = Services.CLOUD_154


class Data(object):
    test_data = [
        marks.ym_h2h_processing(marks.web_in_browser(
            DataObject(paymethod=TrustWebPage(Via.card(get_card()), in_browser=True),
                       user_type=uids.Types.random_from_all))),
        marks.ym_h2h_processing(marks.web_in_browser(
            DataObject(paymethod=TrustWebPage(Via.linked_card(get_card()), in_browser=True),
                       user_type=uids.Types.random_from_all))),
    ]

    test_data_no_save_card = [
        marks.ym_h2h_processing(DataObject(paymethod=TrustWebPage(Via.card(get_card()), in_browser=True),
                                           user_type=uids.Types.random_from_all)),
    ]

    orders_structure = [
        defaults.Order.structure_rub_one_order,
        defaults.Order.structure_rub_two_orders,
    ]


@reporter.feature(features.Service.Cloud)
class TestCloud(object):
    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('test_data', Data.test_data, ids=DataObject.ids)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure, ids=DataObject.ids_orders)
    def test_basic_cycle(self, test_data, orders_structure):
        paymethod, user_type = test_data.paymethod, test_data.user_type
        user = uids.get_random_of_type(user_type)

        basket = simple.process_payment(service, user=user, orders_structure=orders_structure,
                                        paymethod=paymethod, need_postauthorize=True)
        simple.process_refund(service, trust_payment_id=basket['trust_payment_id'], basket=basket, user=user)

    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('test_data', Data.test_data_no_save_card, ids=DataObject.ids)
    def test_pay_and_doesnt_save_card(self, test_data):
        with reporter.step('Оплачиваем корзину и НЕ сохраняем карту при этом'):
            paymethod, user_type = test_data.paymethod, test_data.user_type
            user = uids.get_random_of_type(user_type)
            simple.process_payment(service, user=user, paymethod=paymethod, need_postauthorize=True)
        check.check_that(simple.find_card_by_masked_number(service=service, user=user,
                                                           number=get_masked_number(
                                                                paymethod.via.card['card_number'])),
                         is_(none()),
                         step=u'Проверяем что после оплаты карта не привязалась к пользователю',
                         error=u'После оплаты со снятым чекбоксом привязки карты '
                               u'карта все равно привязалась к пользователю')
