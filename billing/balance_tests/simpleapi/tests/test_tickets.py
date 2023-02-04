# coding=utf-8
from decimal import Decimal

import pytest
from hamcrest import is_, none, equal_to

import btestlib.reporter as reporter
import btestlib.utils as utils
from btestlib.constants import Services
from btestlib.utils import CheckMode, check_mode, Pytest
from simpleapi import common
from simpleapi.common import logger
from simpleapi.common.payment_methods import TrustWebPage, Via, CompensationDiscount, ApplePay
from simpleapi.common.utils import DataObject
from simpleapi.data import defaults
from simpleapi.data import features, stories
from simpleapi.data import marks
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import get_card, get_masked_number, Sberbank, CardBrand
from simpleapi.matchers.deep_equals import deep_contains
from simpleapi.steps import check_steps as check
from simpleapi.steps import expected_steps as expected
from simpleapi.steps import simple_steps as simple
from simpleapi.steps import trust_steps as trust

__author__ = 'fellow'

log = logger.get_logger()

user_with_ym = uids.Type(pool=uids.secret, name='test_wo_proxy_old')
user_with_linked_card = uids.Type(pool=uids.secret, name='test_proxy_new')

"""
https://st.yandex-team.ru/TRUST-1552
"""


class Data(object):
    services = [
        Services.TICKETS,
        Services.EVENTS_TICKETS,
        Services.EVENTS_TICKETS_NEW,
    ]
    services_discount_200 = [
        Services.EVENTS_TICKETS,
        Services.EVENTS_TICKETS_NEW,
    ]
    services_discount_210 = [
        Services.TICKETS,
        Services.EVENTS_TICKETS,
        Services.EVENTS_TICKETS_NEW,
    ]
    services_discount_220 = [
        Services.TICKETS,
        Services.EVENTS_TICKETS,
        Services.EVENTS_TICKETS_NEW,
    ]
    services_discount_bin_card = [
        Services.EVENTS_TICKETS,
        Services.EVENTS_TICKETS_NEW,
    ]
    test_data_tickets = [
        marks.ym_h2h_processing(marks.web_in_browser(
            DataObject(paymethod=TrustWebPage(Via.card(get_card()), in_browser=True),
                       user_type=uids.Types.random_from_all))),
        marks.ym_h2h_processing(marks.web_in_browser(
            DataObject(paymethod=TrustWebPage(Via.linked_card(get_card(), from_linked_phonish=True), in_browser=True),
                       user_type=uids.Types.random_with_linked_phonishes))),
        marks.ym_h2h_processing(marks.web_in_browser(
            DataObject(paymethod=TrustWebPage(Via.linked_card(get_card()), in_browser=True),
                       user_type=uids.Types.random_from_all))),
        pytest.mark.yamoney(DataObject(paymethod=TrustWebPage(Via.yandex_money()), user_type=user_with_ym)),
        # МК отключили по TRUST-3320n
        # DataObject(paymethod=TrustWebPage()), user_type=uids.Types.random_with_phone),
        marks.simple_internal_logic(
            DataObject(paymethod=CompensationDiscount(), user_type=uids.Types.random_from_all)),
        marks.ym_h2h_processing(
            DataObject(paymethod=TrustWebPage(Via.card(get_card()), in_browser=True),
                       user_type=uids.Types.anonymous)),
        # todo fellow: Новый ЯД все-равно не работает, задизейблил пока
        # marks.ym_new_h2h_processing(marks.web_in_browser(
        #     DataObject(paymethod=TrustWebPage(Via.card(get_card()), in_browser=True),
        #                user_type=uids.Types.random_for_new_yamoney_api))),
        # marks.ym_new_h2h_processing(marks.web_in_browser(
        #     DataObject(paymethod=TrustWebPage(Via.linked_card(get_card()), in_browser=True),
        #                user_type=uids.Types.random_for_new_yamoney_api))),
    ]
    test_data_event_tickets = [
        marks.ym_h2h_processing(marks.web_in_browser(
            DataObject(paymethod=TrustWebPage(Via.card(get_card()), in_browser=True),
                       user_type=uids.Types.random_from_all))),
        marks.ym_h2h_processing(marks.web_in_browser(
            DataObject(paymethod=TrustWebPage(Via.linked_card(get_card()), in_browser=True),
                       user_type=uids.Types.random_from_all))),
        pytest.mark.yamoney(DataObject(paymethod=TrustWebPage(Via.yandex_money()), user_type=user_with_ym)),
        # для Билетов на мероприятия способ оплаты телефоном недоступен
        # DataObject(paymethod=TrustWebPage(Via.phone()), user_type=uids.Types.random_with_phone),
        marks.simple_internal_logic(DataObject(paymethod=CompensationDiscount(), user_type=uids.Types.random_from_all)),
        marks.ym_h2h_processing(
            DataObject(paymethod=TrustWebPage(Via.card(get_card()), in_browser=True), user_type=uids.Types.anonymous))
    ]
    test_data_event_tickets_new = [
        marks.ym_h2h_processing(marks.web_in_browser(
            DataObject(paymethod=TrustWebPage(Via.card(get_card()), in_browser=True),
                       user_type=uids.Types.random_from_all))),
        marks.ym_h2h_processing(marks.web_in_browser(
            DataObject(paymethod=TrustWebPage(Via.linked_card(get_card()), in_browser=True),
                       user_type=uids.Types.random_from_all))),
        marks.simple_internal_logic(DataObject(paymethod=CompensationDiscount(), user_type=uids.Types.random_from_all)),
        marks.ym_h2h_processing(
            DataObject(paymethod=TrustWebPage(Via.card(get_card())), user_type=uids.Type(anonymous=True)))
    ]
    test_data_save_card = [
        DataObject(paymethod=TrustWebPage(Via.card(get_card(), save_card=True), in_browser=True),
                   user_type=uids.Types.random_from_all),
    ]
    test_data_discount = [
        marks.ym_h2h_processing(marks.web_in_browser(
            DataObject(paymethod=TrustWebPage(Via.card(get_card()), in_browser=True),
                       user_type=uids.Types.random_from_all))),
        marks.ym_h2h_processing(marks.web_in_browser(
            DataObject(paymethod=TrustWebPage(Via.linked_card(get_card()), in_browser=True),
                       user_type=uids.Types.random_from_all))),
        marks.ym_h2h_processing(marks.web_in_browser(
            DataObject(paymethod=TrustWebPage(Via.linked_card(get_card(), from_linked_phonish=True),
                                              in_browser=True),
                       user_type=uids.Types.random_with_linked_phonishes))),
    ]
    test_data_discount_not_available = [
        # todo добавить кейс с maestro
        marks.ym_h2h_processing(DataObject(paymethod=TrustWebPage(Via.linked_card(get_card(brand=CardBrand.Visa))),
                                           user_type=uids.Types.random_from_all)),
    ]
    orders_structure = [
        defaults.Order.structure_rub_one_order,
        defaults.Order.structure_rub_two_orders,
    ]
    orders_structure_bin_discount = [
        (
        {'currency': 'RUB', 'fiscal_nds': defaults.Fiscal.NDS.nds_none, 'fiscal_title': defaults.Fiscal.fiscal_title},),
        ({'currency': 'RUB', 'fiscal_nds': defaults.Fiscal.NDS.nds_none, 'fiscal_title': defaults.Fiscal.fiscal_title},
         {'currency': 'RUB', 'fiscal_nds': defaults.Fiscal.NDS.nds_none, 'fiscal_title': defaults.Fiscal.fiscal_title})
    ]
    test_data_apple_token = [
        marks.apple_pay(DataObject(paymethod=ApplePay(), user_type=uids.Types.random_from_all)),
    ]
    test_data_no_save_card = [
        marks.ym_h2h_processing(DataObject(paymethod=TrustWebPage(Via.card(get_card()), in_browser=True),
                                           user_type=uids.Types.random_from_all)),
    ]
    test_data_two_discounts = [
        marks.ym_h2h_processing(DataObject(paymethod=TrustWebPage(Via.card(get_card()), in_browser=True),
                                           user_type=uids.Types.random_from_all).new(
            discount_1=defaults.Discounts.id220,
            discount_2=defaults.Discounts.id100))
    ]
    test_data_negative_bin_discount = [
        DataObject(descr='10_pct_for_Mastercard',
                   paymethod=TrustWebPage(Via.card(Sberbank.Success.Without3DS.card_visa),
                                          in_browser=True),
                   user_type=uids.Types.random_from_all).new(
            discount=defaults.DiscountsForBin.id101)
        # TODO: sunshineguy: Добавить МИР и т.д.
    ]
    test_data_bin_discounts = [
        DataObject(descr='10_pct_for_Mastercard',
                   paymethod=TrustWebPage(Via.card(get_card()), in_browser=True),
                   user_type=uids.Types.random_from_all).new(
            discount=defaults.DiscountsForBin.id101)

    ]


def ids_card_bin(val):
    return 'bin_card={}-price={}'.format(val.card['card_number'][:6],
                                         val.price)


@reporter.feature(features.Service.Tickets)
class TestTickets(object):
    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize(*Pytest.combine_set(Pytest.ParamsSet(names='test_data',
                                                                  values=[
                                                                      Data.test_data_tickets,
                                                                      Data.test_data_event_tickets,
                                                                      Data.test_data_event_tickets_new
                                                                  ]),
                                                 Pytest.ParamsSet(names='service',
                                                                  values=[
                                                                      [Services.TICKETS, ],
                                                                      [Services.EVENTS_TICKETS],
                                                                      [Services.EVENTS_TICKETS_NEW]
                                                                  ])),
                             ids=lambda test_data, service: '{} {}'.format(
                                 DataObject.ids_service(service), DataObject.ids(test_data))
                             )
    @pytest.mark.parametrize('orders_structure', Data.orders_structure,
                             ids=lambda orders_structure: DataObject.ids_orders(orders_structure))
    def test_base_payment_cycle(self, test_data, service, orders_structure):
        paymethod, user_type = test_data.paymethod, test_data.user_type
        user = uids.get_random_of_type(user_type)
        orders = simple.form_orders_for_create(service, user, orders_structure)
        with check_mode(CheckMode.FAILED):
            basket = simple.process_payment(service=service, user=user, paymethod=paymethod, orders=orders,
                                            need_postauthorize=True)
            simple.wait_until_real_postauth(service, user=user,
                                            trust_payment_id=basket['trust_payment_id'])
            simple.process_refund(service, trust_payment_id=basket['trust_payment_id'], basket=basket, user=user)

    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('length', [16, 17, 18, 19], ids=DataObject.ids_card_length)
    @pytest.mark.parametrize('service', Data.services, ids=DataObject.ids_service)
    def test_card_serial_numbers(self, service, length):
        paymethod = TrustWebPage(Via.card(get_card(length=length)), in_browser=True)
        user = uids.get_random_of(uids.mimino)

        orders = simple.form_orders_for_create(service, user, Data.orders_structure[0])
        with check_mode(CheckMode.FAILED):
            simple.process_payment(service=service, user=user, paymethod=paymethod, orders=orders)

    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('test_data', Data.test_data_save_card, ids=DataObject.ids)
    @pytest.mark.parametrize('service', Data.services, ids=DataObject.ids_service)
    def test_pay_and_save_card(self, service, test_data):
        with reporter.step(u'Оплачиваем корзину и сохраняем карту'):
            paymethod, user_type = test_data.paymethod, test_data.user_type
            user = uids.get_random_of_type(user_type)

            orders = simple.form_orders_for_create(service, user)
            simple.process_payment(service=service, user=user, paymethod=paymethod, orders=orders)

            simple.list_payment_methods(service, user)

        with reporter.step(u'Оплачиваем новую корзину привязанной картой'), check_mode(CheckMode.FAILED):
            paymethod_linked = TrustWebPage(Via.linked_card(card=paymethod.via.card), in_browser=True)
            orders = simple.form_orders_for_create(service, user)
            basket = simple.create_basket(service, user=user,
                                          orders=orders,
                                          paymethod_id=paymethod_linked.id)
            payment_form = simple.pay_basket(service, user=user,
                                             trust_payment_id=basket['trust_payment_id']).get('payment_form')
            trust.pay_by(paymethod_linked, service, user=user, payment_form=payment_form,
                         purchase_token=basket['purchase_token'])

            simple.wait_until_payment_done(service, user=user,
                                           purchase_token=basket['purchase_token'])

    @reporter.story(stories.General.Discount)
    @pytest.mark.parametrize(*utils.Pytest.combine_set(
        utils.Pytest.ParamsSet(names='discount',
                               values=[
                                   [defaults.Discounts.id100, ],
                                   [defaults.Discounts.id200, ],
                                   [defaults.Discounts.id210, ],
                                   [defaults.Discounts.id220, ],
                               ]),
        utils.Pytest.ParamsSet(names='service',
                               values=[
                                   Data.services,
                                   Data.services_discount_200,
                                   Data.services_discount_210,
                                   Data.services_discount_220,
                               ])),
                             ids=lambda discount, service: '{}-{}'.format(discount['id'],
                                                                          DataObject.ids_service(service)))
    @pytest.mark.parametrize('test_data', Data.test_data_discount, ids=DataObject.ids)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure, ids=DataObject.ids_orders)
    def test_discount(self, service, discount, test_data, orders_structure):
        paymethod, user_type = test_data.paymethod, test_data.user_type
        user = uids.get_random_of_type(user_type)
        orders = simple.form_orders_for_create(service, user, orders_structure)

        with check_mode(CheckMode.FAILED):
            basket = simple.process_payment(service, user, orders=orders, paymethod=paymethod,
                                            discounts=[discount['id'], ])
        check.check_that(basket, deep_contains(expected.RegularBasket.paid(paymethod, orders,
                                                                           discounts=[discount, ],
                                                                           with_email=True)))
        orders_for_update = simple.form_orders_for_update(orders)
        simple.update_basket(service, orders=orders_for_update, user=user,
                             trust_payment_id=basket['trust_payment_id'])
        simple.wait_until_real_postauth(service, user=user,
                                        trust_payment_id=basket['trust_payment_id'])
        with check_mode(CheckMode.FAILED):
            simple.process_refund(service, trust_payment_id=basket['trust_payment_id'], basket=basket, user=user)

    @reporter.story(stories.General.Discount)
    @pytest.mark.parametrize('test_data', Data.test_data_discount_not_available, ids=DataObject.ids)
    @pytest.mark.parametrize('service', Data.services, ids=DataObject.ids_service)
    def test_discount_only_for_mastercard(self, service, test_data):
        paymethod, user_type = test_data.paymethod, test_data.user_type
        user = uids.get_random_of_type(user_type)
        orders = simple.form_orders_for_create(service, user)

        basket = simple.process_payment(service, user, orders=orders, paymethod=paymethod,
                                        discounts=[defaults.Discounts.id100['id'], ])

        check.check_that(basket, deep_contains(expected.RegularBasket.paid(paymethod, orders,
                                                                           discounts=None, with_email=True)))

    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('test_data', Data.test_data_apple_token, ids=DataObject.ids)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure, ids=DataObject.ids_orders)
    @pytest.mark.parametrize('service', Data.services, ids=DataObject.ids_service)
    def test_apple_token(self, service, test_data, orders_structure):
        paymethod, user_type = test_data.paymethod, test_data.user_type
        user = uids.get_random_of_type(user_type)
        orders = simple.form_orders_for_create(service, user, orders_structure)
        with check_mode(CheckMode.FAILED):
            basket = simple.process_payment(service, user, orders=orders, paymethod=paymethod, need_postauthorize=True)
            simple.wait_until_real_postauth(service, user=user,
                                            trust_payment_id=basket['trust_payment_id'])
            simple.process_refund(service, trust_payment_id=basket['trust_payment_id'], basket=basket, user=user)

    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('service', Data.services, ids=DataObject.ids)
    @pytest.mark.parametrize('test_data', Data.test_data_no_save_card, ids=ids_card_bin)
    def test_pay_and_doesnt_save_card(self, test_data, service):
        with reporter.step('Оплачиваем корзину и НЕ сохраняем карту при этом'):
            paymethod, user_type = test_data.paymethod, test_data.user_type
            user = uids.get_random_of_type(user_type)
            orders = simple.form_orders_for_create(service, user)
            simple.process_payment(service=service, user=user, paymethod=paymethod, orders=orders,
                                   need_postauthorize=True)
        check.check_that(simple.find_card_by_masked_number(service=service, user=user,
                                                           number=get_masked_number(
                                                               paymethod.via.card['card_number'])),
                         is_(none()),
                         step=u'Проверяем, что после оплаты карта не привязалась к пользователю',
                         error=u'Карта привязалась к пользователю даже '
                               u'после оплаты со снятым чекбоксом привязки карты')

    @reporter.story(stories.General.Discount)
    @pytest.mark.parametrize('service', Data.services_discount_bin_card,
                             ids=DataObject.ids_service)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure_bin_discount,
                             ids=DataObject.ids_orders)
    @pytest.mark.parametrize('price', defaults.DiscountsForBin.prices_for_10_pct_300_max,
                             ids=DataObject.ids_custom('price'))
    @pytest.mark.parametrize('test_discount', Data.test_data_bin_discounts,
                             ids=DataObject.ids)
    def test_discount_for_card_bin(self, price, service, orders_structure,
                                   test_discount):
        """
        Скидка работает следующим образом:
        Скидка составляет n% от суммы заказа, но она не может привышать k руб
        Пример для: n = 10 % и k = 300р
          суммы: 1000, 3000, 5000
          скидки: 100, 300,  300
        """
        paymethod, user_type, discount = test_discount.paymethod, \
                                         test_discount.user_type, \
                                         test_discount.discount
        user = uids.get_random_of_type(user_type)
        applied_discount = price * discount['pct'] / 100
        paid_amount = price - (applied_discount if applied_discount < discount['max_price']
                               else discount['max_price'])
        for order in orders_structure:
            order['price'] = price / len(orders_structure)
        amount = simple.process_payment(service=service, user=user,
                                        paymethod=paymethod, orders_structure=orders_structure,
                                        discounts=[discount['id']])['paid_amount']
        check.check_that(Decimal(amount), equal_to(paid_amount),
                         step=u'Проверяем, что итоговая цена рассчитана правильно',
                         error=u'Итоговая цена с учетом скидки '
                               u'рассчитана неправильно!')

    @reporter.story(stories.General.Discount)
    @pytest.mark.parametrize('service', Data.services_discount_bin_card,
                             ids=DataObject.ids_service)
    @pytest.mark.parametrize('test_discount', Data.test_data_negative_bin_discount,
                             ids=DataObject.ids)
    def test_not_discount_for_other_bin_card(self, service, test_discount):
        paymethod, user_type, discount = test_discount.paymethod, \
                                         test_discount.user_type, \
                                         test_discount.discount
        user = uids.get_random_of_type(user_type)
        orders = simple.form_orders_for_create(service, user)
        amount = simple.process_payment(service=service, user=user,
                                        paymethod=paymethod, orders=orders,
                                        discounts=[discount['id']])['paid_amount']
        check.check_that(float(amount), equal_to(orders[0]['price']),
                         step=u'Проверяем, что скидка не применилась',
                         error=u'Скидка применилась!')

    @reporter.story(stories.General.Discount)
    @pytest.mark.parametrize('test_data', Data.test_data_two_discounts,
                             ids=DataObject.ids)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure,
                             ids=DataObject.ids_orders)
    @pytest.mark.parametrize('service', Data.services_discount_bin_card,
                             ids=DataObject.ids_service)
    def test_two_discounts(self, orders_structure, test_data, service):
        """
            Тест на применение двух скидок:
            Если в параметр discounts переданно две или более скидок то они применяются последовательно т.е.
            Сперва применяется первая скидка, потом к получившейся цене применяется вторая скидка. Эту логику планируют
            изменить подробности в todo
        """
        # todo: sunshineguy: логику применения скидок собираются переделвывать -> TRUST-5674
        paymethod, user_type, discount_1, discount_2 = test_data.paymethod, test_data.user_type, \
                                                       test_data.discount_1, test_data.discount_2
        user = uids.get_random_of_type(user_type)
        total_price = 0
        for order in orders_structure:
            total_price += order['price']
        total_price = common.utils.apply_discount(Decimal(total_price), [discount_1['pct'], discount_2['pct']])
        amount = simple.process_payment(service=service, user=user,
                                        paymethod=paymethod, orders_structure=orders_structure,
                                        discounts=[discount_1['id'], discount_2['id']])['paid_amount']
        check.check_that(Decimal(amount), equal_to(total_price.quantize(Decimal('1.00'))),
                         step=u'Проверяем, что итоговая цена рассчитана правильно',
                         error=u'Итоговая цена с учетом нескольких скидок '
                               u'рассчитана неправильно!')


if __name__ == '__main__':
    pytest.main()
