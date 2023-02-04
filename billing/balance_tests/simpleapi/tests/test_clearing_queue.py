# coding=utf-8
import pytest

import btestlib.reporter as reporter
from btestlib.constants import Services
from simpleapi.common.payment_methods import LinkedCard, TrustWebPage, Via, UberForwardingCard, UberRoamingCard, \
    ApplePay, GooglePay
from simpleapi.common.utils import DataObject
from simpleapi.data import defaults
from simpleapi.data import features
from simpleapi.data import marks
from simpleapi.data import stories
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import get_card, Ecommpay, RBS, Payture, Sberbank
from simpleapi.data.defaults import CountryData
from simpleapi.matchers.deep_equals import deep_contains
from simpleapi.steps import check_steps as check
from simpleapi.steps import expected_steps as expected
from simpleapi.steps import simple_steps as simple

__author__ = 'fellow'


class Data(object):
    data = [
        marks.ym_h2h_processing(
            DataObject(service=Services.MARKETPLACE,
                       paymethod=TrustWebPage(Via.card(get_card())),
                       user_type=uids.Types.random_from_all,
                       **CountryData.get_clear_data(CountryData.Russia)).new(pass_cvn=False)),
        marks.ym_h2h_processing(
            DataObject(service=Services.MARKETPLACE,
                       paymethod=TrustWebPage(Via.linked_card(get_card(), from_linked_phonish=True)),
                       user_type=uids.Types.random_with_linked_phonishes,
                       **CountryData.get_clear_data(CountryData.Russia)).new(pass_cvn=False)),
        marks.ym_h2h_processing(
            DataObject(service=Services.MARKETPLACE,
                       paymethod=TrustWebPage(Via.linked_card(card=get_card())),
                       user_type=uids.Types.random_from_all,
                       **CountryData.get_clear_data(CountryData.Russia)).new(pass_cvn=False)),
        marks.ym_h2h_processing(
            DataObject(service=Services.MARKETPLACE,
                       paymethod=LinkedCard(card=get_card()),
                       user_type=uids.Types.random_from_all,
                       **CountryData.get_clear_data(CountryData.Russia)).new(pass_cvn=False)),
        marks.ym_h2h_processing(
            DataObject(service=Services.BLUE_MARKET_PAYMENTS,
                       paymethod=TrustWebPage(Via.card(get_card())),
                       user_type=uids.Types.random_from_all,
                       **CountryData.get_clear_data(CountryData.Russia)).new(pass_cvn=False)),
        marks.ym_h2h_processing(
            DataObject(service=Services.TAXI,
                       paymethod=LinkedCard(card=get_card()),
                       user_type=uids.Types.random_from_all,
                       **CountryData.get_clear_data(CountryData.Russia)).new(pass_cvn=False)),
        marks.ym_h2h_processing(
            marks.apple_pay(
                DataObject(service=Services.TAXI,
                           paymethod=ApplePay(bind_token=True),
                           user_type=uids.Types.random_from_all,
                           **CountryData.get_clear_data(CountryData.Russia)).new(pass_cvn=False))),
        marks.payture_processing(
            DataObject(service=Services.TAXI,
                       paymethod=LinkedCard(card=Payture.Success.Without3DS.card_second),
                       user_type=uids.Types.random_from_all,
                       **CountryData.get_clear_data(CountryData.Armenia)).new(pass_cvn=False)),
        marks.payture_processing(
            marks.google_pay(
                DataObject(service=Services.TAXI,
                           paymethod=GooglePay(),
                           user_type=uids.Types.random_from_all,
                           **CountryData.get_clear_data(CountryData.Russia)).new(pass_cvn=False))),
        marks.rbs_processing(
            DataObject(service=Services.TAXI,
                       paymethod=LinkedCard(card=RBS.Success.Without3DS.card_visa),
                       user_type=uids.Types.random_from_all,
                       **CountryData.get_clear_data(CountryData.Kazakhstan)).new(pass_cvn=False)),
        marks.ecommpay_processing(
            DataObject(service=Services.TAXI,
                       paymethod=LinkedCard(card=Ecommpay.Success.Without3DS.card_mastercard),
                       user_type=uids.Types.random_from_all,
                       **CountryData.get_clear_data(CountryData.Germany)).new(pass_cvn=True)),
        marks.sberbank_processing(
            DataObject(service=Services.TAXI,
                       paymethod=LinkedCard(card=Sberbank.Success.Without3DS.card_visa),
                       user_type=uids.Types.random_for_sberbank,
                       **CountryData.get_clear_data(CountryData.Russia)).new(pass_cvn=False)),
        marks.sberbank_processing(
            DataObject(service=Services.UBER,
                       paymethod=UberForwardingCard(card=get_card()),
                       user_type=uids.Types.uber,
                       **CountryData.get_clear_data(CountryData.Russia)).new(pass_cvn=False)),
        marks.uber(
            DataObject(service=Services.UBER_ROAMING,
                       paymethod=UberRoamingCard(),
                       user_type=uids.Types.uber,
                       **CountryData.get_clear_data(CountryData.Russia)).new(pass_cvn=False)),
        marks.no_parallel(
            marks.uber(
                DataObject(service=Services.UBER,
                           paymethod=UberForwardingCard(card=get_card()),
                           user_type=uids.Types.uber,
                           **CountryData.get_clear_data(CountryData.Russia)).new(pass_cvn=False))),

    ]
    orders_data = [
        DataObject(orders_structure=defaults.Order.structure_rub_one_order).new(clearing_plan=[{'action': 'clear'}, ]),
        DataObject(orders_structure=defaults.Order.structure_rub_two_orders).new(
            clearing_plan=[{'action': 'clear'}, {'action': 'cancel'}], )

    ]

    orders_data_reversal = [
        DataObject(orders_structure=defaults.Order.structure_rub_one_order).new(clearing_plan=[{'action': 'cancel'}, ]),
        DataObject(orders_structure=defaults.Order.structure_rub_two_orders).new(
            clearing_plan=[{'action': 'cancel'}, {'action': 'cancel'}], )
    ]


@reporter.feature(features.General.ClearingQueue)
class TestClearingQueue(object):
    @reporter.story(stories.Clearing.Clear)
    @pytest.mark.parametrize('orders_data', Data.orders_data, ids=DataObject.ids)
    @pytest.mark.parametrize('data', Data.data, ids=DataObject.ids)
    def test_clearing(self, data, orders_data):
        service, paymethod, user_type = data.service, data.paymethod, data.user_type
        user_ip, currency, region_id, pass_cvn = data.user_ip, data.currency, data.region_id, data.pass_cvn
        orders_structure, clearing_plan = orders_data.orders_structure, orders_data.clearing_plan
        user = uids.get_random_of_type(user_type)

        group_orders = True if isinstance(paymethod, (ApplePay, GooglePay)) else False

        orders = simple.form_orders_for_create(service, user, orders_structure, group_orders=group_orders)
        basket = simple.process_payment(service, user=user, orders=orders, paymethod=paymethod,
                                        currency=currency, region_id=region_id, user_ip=user_ip,
                                        pass_cvn=pass_cvn)

        orders_for_update = simple.form_orders_for_update(orders, clearing_plan)

        basket = simple.process_postauthorize(service, user=user,
                                              trust_payment_id=basket['trust_payment_id'],
                                              orders_for_update=orders_for_update)

        refunds = simple.form_refunds(orders, clearing_plan)
        check.check_that(basket, deep_contains(expected.RegularBasket.postauthorized(paymethod, orders,
                                                                                     refunds, currency=currency)),
                         step=u'Проверяем что поставторизованная корзина содержит все необходимые поля',
                         error=u'Корзина после поставторизации содержит некорректные поля')

    @reporter.story(stories.Clearing.Reversal)
    @pytest.mark.parametrize('orders_data', Data.orders_data_reversal, ids=DataObject.ids)
    @pytest.mark.parametrize('data', Data.data, ids=DataObject.ids)
    def test_reversal(self, data, orders_data):
        service, paymethod, user_type = data.service, data.paymethod, data.user_type
        user_ip, currency, region_id, pass_cvn = data.user_ip, data.currency, data.region_id, data.pass_cvn
        orders_structure, clearing_plan = orders_data.orders_structure, orders_data.clearing_plan
        user = uids.get_random_of_type(user_type)

        group_orders = True if isinstance(paymethod, (ApplePay, GooglePay)) else False

        orders = simple.form_orders_for_create(service, user, orders_structure, group_orders=group_orders)
        basket = simple.process_payment(service, user=user, orders=orders, paymethod=paymethod,
                                        currency=currency, region_id=region_id, user_ip=user_ip,
                                        pass_cvn=pass_cvn)

        orders_for_update = simple.form_orders_for_update(orders, clearing_plan)
        basket = simple.process_postauthorize(service, user=user,
                                              trust_payment_id=basket['trust_payment_id'],
                                              orders_for_update=orders_for_update)

        refunds = simple.form_refunds(orders, clearing_plan)
        check.check_that(basket, deep_contains(expected.RegularBasket.reversaled(orders, refunds)),
                         step=u'Проверяем что корзина поле реверсала содержит все необходимые поля',
                         error=u'Корзина после реверсала содержит некорректные поля')


if __name__ == '__main__':
    pytest.main()
