# coding: utf-8
import datetime
from decimal import Decimal

import pytest
from hamcrest import is_, equal_to, has_entries

import btestlib.reporter as reporter
from btestlib.constants import Services
from btestlib.utils import CheckMode, check_mode, Date
from simpleapi.common import logger
from simpleapi.common.payment_methods import TrustWebPage, Via, ApplePay
from simpleapi.common.utils import DataObject
from simpleapi.data import defaults
from simpleapi.data import features, stories
from simpleapi.data import marks
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import get_card, CVN, Sberbank, CardBrand
from simpleapi.matchers.deep_equals import deep_equals_to
from simpleapi.steps import check_steps as check
from simpleapi.steps import db_steps
from simpleapi.steps import expected_steps as expected
from simpleapi.steps import simple_steps as simple
from simpleapi.steps import trust_steps as trust
from simpleapi.steps import webapi_steps as webapi

__author__ = 'slppls'

log = logger.get_logger()

pytestmark = marks.ym_h2h_processing

"""
https://st.yandex-team.ru/TRUST-1887
"""


def ids_status(val):
    return 'status={}'.format(val.status)


def ids_many_promo_statuses(val):
    return 'diff_statuses' if val[0] != val[1] else 'same_statuses'


def ids_promo_params(val):
    return 'full_payment={}, partial={}, paymethod={} '.format(val.full_payment_only,
                                                                         val.partial_only,
                                                                         val.paymethod.title)

class Data(object):
    services = [
        Services.TICKETS,
        Services.EVENTS_TICKETS,
        # Services.EVENTS_TICKETS_NEW,
    ]
    services_for_discount = [
        Services.EVENTS_TICKETS,
        Services.EVENTS_TICKETS_NEW,
    ]
    promocode_paymethods = [
        DataObject(paymethod=TrustWebPage(Via.Promocode())).new(
            amount=defaults.Promocode.promocode_amount_full),
        DataObject(paymethod=TrustWebPage(Via.card(get_card()))).new(
            amount=defaults.Promocode.promocode_amount_part),
        DataObject(paymethod=TrustWebPage(Via.linked_card(get_card()))).new(
            amount=defaults.Promocode.promocode_amount_part),
    ]
    multi_promocode_paymethods = [
        TrustWebPage(Via.card(get_card())),
        TrustWebPage(Via.linked_card(get_card())),
    ]
    statuses = [
        DataObject().new(status=defaults.Promocode.Status.active),
        DataObject().new(status=defaults.Promocode.Status.expired),
        DataObject().new(status=defaults.Promocode.Status.not_started)
    ]
    test_date_failed_case = [
        DataObject().new(status=defaults.Promocode.Status.expired,
                         waiting_error=expected.Promocode.expired(),
                         date_param='end_dt'),
        DataObject().new(status=defaults.Promocode.Status.not_started,
                         waiting_error=expected.Promocode.not_started(),
                         date_param='begin_dt')
    ]
    success_params_combination = [
        DataObject(paymethod=TrustWebPage(Via.Promocode())).new(
                   full_payment_only=0,
                   partial_only=0,
                   amount=defaults.Promocode.promocode_amount_full),
        DataObject(paymethod=TrustWebPage(Via.Promocode())).new(
                   full_payment_only=1,
                   partial_only=0,
                   amount=defaults.Promocode.promocode_amount_full),
        DataObject(paymethod=TrustWebPage(Via.card(get_card()))).new(
                   full_payment_only=0,
                   partial_only=0,
                   amount=defaults.Promocode.promocode_amount_part),
        DataObject(paymethod=TrustWebPage(Via.card(get_card()))).new(
                   full_payment_only=0,
                   partial_only=1,
                   amount=defaults.Promocode.promocode_amount_part),
    ]
    failed_params_combination = [
        DataObject(paymethod=TrustWebPage(Via.Promocode()),
                   error=expected.Promocode.amount_too_big()).new(
                   full_payment_only=0,
                   partial_only=1,
                   amount=defaults.Promocode.promocode_amount_full),
        DataObject(paymethod=TrustWebPage(Via.Promocode()),
                   error=expected.Promocode.amount_too_big()).new(
                   full_payment_only=1,
                   partial_only=1,
                   amount=defaults.Promocode.promocode_amount_full),
        DataObject(paymethod=TrustWebPage(Via.card(get_card())),
                   error=expected.Promocode.amount_too_small()).new(
                   full_payment_only=1,
                   partial_only=0,
                   amount=defaults.Promocode.promocode_amount_part),
        DataObject(paymethod=TrustWebPage(Via.card(get_card())),
                   error=expected.Promocode.amount_too_small()).new(
                   full_payment_only=1,
                   partial_only=1,
                   amount=defaults.Promocode.promocode_amount_part)

    ]
    # Используется только в закоменченном тесте, оставлю под комментарием, вдруг, пригодится
    # status_combination = [
    #    DataObject.new(promo_status_1=defaults.Promocode.Status.expired,
    #                   promo_status_2=defaults.Promocode.Status.active),
    #    DataObject.new(promo_status_1=defaults.Promocode.Status.active,
    #                   promo_status_2=defaults.Promocode.Status.active)
    # ]
    test_data_apple_token = [
        marks.apple_pay(DataObject(paymethod=ApplePay(), user_type=uids.Types.random_from_all)),
    ]
    orders_structure = [
        defaults.Order.structure_rub_one_order,
        # defaults.Order.structure_rub_two_orders, # https://st.yandex-team.ru/TRUST-4091 want to fix that
    ]
    orders_structure_bin_discount = [
        (
        {'currency': 'RUB', 'fiscal_nds': defaults.Fiscal.NDS.nds_none, 'fiscal_title': defaults.Fiscal.fiscal_title},),
        # ({'currency': 'RUB',  'fiscal_nds': defaults.Fiscal.NDS.nds_none,
        # 'fiscal_title': defaults.Fiscal.fiscal_title},
        #  {'currency': 'RUB',  'fiscal_nds': defaults.Fiscal.NDS.nds_none,
        # 'fiscal_title': defaults.Fiscal.fiscal_title})
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
                   user_type=uids.Types.random_from_all).new(
                   discount=defaults.DiscountsForBin.id101)

    ]


@reporter.feature(features.General.Promocodes)
@reporter.story(stories.General.Promocodes)
class TestPromocodes(object):
    @staticmethod
    def check_promocode_payment_for_composite(basket, promo_amount):
        with reporter.step('Проверяем композитность платежа и его промокодную часть'):
            composite_payment_id = \
                db_steps.ng().get_composite_payment(basket['purchase_token'])
            # если оплата картой с промокодом, то метод оплаты - карта, создается композитный платеж, из двух строчек
            # в первой платеж картой, во второй - промокодом
            if basket['payment_method'] == 'direct_card':
                amount, orig_amount = db_steps.ng().get_amounts_from_payment(basket['purchase_token'],
                                                                             composite_payment_id=composite_payment_id)
                check.check_that(Decimal(amount), is_(equal_to(Decimal(promo_amount))))
                check.check_that(Decimal(orig_amount), is_(equal_to(Decimal(promo_amount))))
            # если оплата только промокодом, то композитный платёж более не создается
            elif basket['payment_method'] == 'new_promocode':
                check.check_that(composite_payment_id, is_(None))
                amount, orig_amount = db_steps.ng().get_amounts_from_payment(basket['purchase_token'])
                check.check_that(Decimal(amount), is_(equal_to(Decimal(orig_amount))))

    @pytest.mark.parametrize('statuses', Data.statuses, ids=ids_status)
    @pytest.mark.parametrize('paymethod', Data.multi_promocode_paymethods, ids=DataObject.ids_paymethod)
    @pytest.mark.parametrize('service', Data.services, ids=DataObject.ids_service)
    def test_pay_promocode_status(self, service, paymethod, statuses):
        user = uids.get_random_of(uids.all_)
        status = statuses.status
        promocode_id = simple.process_promocode_creating(service, promo_status=status)
        orders = simple.form_orders_for_create(service, user)
        with check_mode(CheckMode.FAILED):
            basket = simple.process_payment(service, user, orders=orders, paymethod=paymethod,
                                            promocode_id=promocode_id)
            resp = simple.check_basket(service, user=user, purchase_token=basket['purchase_token'])
            simple.check_promocode_in_payment(service, promocode_id, status, resp)

    @pytest.mark.parametrize('payment_params', Data.promocode_paymethods, ids=DataObject.ids_paymethod)
    @pytest.mark.parametrize('service', Data.services, ids=DataObject.ids_service)
    def test_pay_double_by_one_promocode(self, service, payment_params):
        user = uids.get_random_of(uids.all_)
        paymethod, amount = payment_params.paymethod, payment_params.amount
        promocode_id = simple.process_promocode_creating(service, promo_amount=amount)
        orders = simple.form_orders_for_create(service, user)
        with check_mode(CheckMode.FAILED):
            simple.process_payment(service, user, orders=orders, paymethod=paymethod,
                                   promocode_id=promocode_id)
            basket = simple.create_basket(service, user=user,
                                          orders=orders,
                                          paymethod_id=paymethod.id,
                                          promocode_id=promocode_id)
            simple.pay_basket(service, user=user,
                              trust_payment_id=basket['trust_payment_id']).get('payment_form')
        with check_mode(CheckMode.IGNORED):
            preview = webapi.preview_payment(basket['purchase_token'], promocode_id=promocode_id)
            check.check_that(preview['promocode']['status_code'], is_(equal_to('applied')))

    @pytest.mark.parametrize('payment_params',
                             Data.success_params_combination,
                             ids=ids_promo_params)
    @pytest.mark.parametrize('service', Data.services,
                             ids=DataObject.ids_service)
    def test_base_payment_cycle(self, service, payment_params):
        user = uids.get_random_of(uids.all_)
        paymethod, amount, full_payment_only, partial_only = \
            payment_params.paymethod, payment_params.amount, \
            payment_params.full_payment_only, payment_params.partial_only
        promocode_id = simple.process_promocode_creating(service,
                                                         promo_amount=amount,
                                                         full_payment_only=full_payment_only,
                                                         partial_only=partial_only)
        with check_mode(CheckMode.FAILED):
            basket = simple.process_payment(service, user,
                                            paymethod=paymethod,
                                            promocode_id=promocode_id)
            basket = simple.check_basket(service, user=user,
                                         purchase_token=basket['purchase_token'],
                                         with_promocodes=True)
        self.check_promocode_payment_for_composite(basket, amount)
        basket_promocode_id = basket['promocodes']['applied_promocode_id']
        check.check_that(promocode_id, equal_to(basket_promocode_id),
                         step=u'Проверяем что применился корректный промокод',
                         error=u'Применился некорректный промокод')

    @pytest.mark.parametrize('payment_params',
                             Data.success_params_combination,
                             ids=ids_promo_params)
    @pytest.mark.parametrize('service', Data.services_for_discount,
                             ids=DataObject.ids_service)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure_bin_discount,
                             ids=DataObject.ids_orders)
    @pytest.mark.parametrize('price', defaults.DiscountsForBin.prices_for_10_pct_300_max,
                             ids=DataObject.ids_custom('price_after_promo'))
    @pytest.mark.parametrize('test_discount', Data.test_data_bin_discounts,
                             ids=DataObject.ids)
    def test_base_payment_cycle_with_discount(self, service, payment_params,
                                              orders_structure, price, test_discount):
        """
        Скидка работает следующим образом:
        Скидка составляет n% от суммы заказа, но она не может привышать k руб
        Пример для n = 10 % и k = 300р
        Примеры: суммы: 1000, 3000, 5000
                 скидки: 100, 300,  300
        При частичной оплате промокодом - прибавляем к цене величину промокода
        для валидности тестовых данных.
        При полной оплате промокодом - скидка не должна примениться.
        """
        user_type, discount = test_discount.user_type, \
                              test_discount.discount
        user = uids.get_random_of_type(user_type)
        paymethod, full_payment_only, partial_only = \
            payment_params.paymethod, payment_params.full_payment_only, \
            payment_params.partial_only
        if payment_params.amount == defaults.Promocode.promocode_amount_part:
            amount = payment_params.amount
            price_after_promo = price
            price += amount
            applied_discount = price_after_promo * discount['pct'] / 100
            paid_amount = price_after_promo - (applied_discount if applied_discount < discount['max_price']
                                               else discount['max_price'])
        else:
            amount = defaults.Promocode.promocode_amount_big
            paid_amount = price
        for order in orders_structure:
            order['price'] = price / len(orders_structure)
        promocode_id = simple.process_promocode_creating(service,
                                                         promo_amount=amount,
                                                         full_payment_only=full_payment_only,
                                                         partial_only=partial_only)
        with check_mode(CheckMode.FAILED):
            basket = simple.process_payment(service, user, orders_structure=orders_structure,
                                            paymethod=paymethod,
                                            promocode_id=promocode_id,
                                            discounts=[discount['id']])
            basket = simple.check_basket(service, user=user,
                                         purchase_token=basket['purchase_token'],
                                         with_promocodes=True)
        TestPromocodes.check_promocode_payment_for_composite(basket, amount)
        basket_promocode_id = basket['promocodes']['applied_promocode_id']
        check.check_that(promocode_id, equal_to(basket_promocode_id),
                         step=u'Проверяем, что применился корректный промокод',
                         error=u'Применился некорректный промокод')
        check.check_that(Decimal(basket['paid_amount']),
                         equal_to(paid_amount),
                         step=u'Проверяем, что итоговая цена рассчитана правильно',
                         error=u'Итоговая цена рассчитана неверно!')

    @pytest.mark.parametrize('service', Data.services_for_discount,
                             ids=DataObject.ids_service)
    @pytest.mark.parametrize('test_discount', Data.test_data_negative_bin_discount,
                             ids=DataObject.ids)
    def test_not_discount_for_other_bin_card(self, service, test_discount):
        paymethod, user_type, discount = test_discount.paymethod, \
                                         test_discount.user_type, \
                                         test_discount.discount
        user = uids.get_random_of_type(user_type)
        orders = simple.form_orders_for_create(service, user)
        with check_mode(CheckMode.FAILED):
            basket = simple.process_payment(service, user, orders=orders,
                                            paymethod=paymethod,
                                            discounts=[discount['id']])
            basket = simple.check_basket(service, user=user,
                                         purchase_token=basket[
                                             'purchase_token'],
                                         with_promocodes=True)
        check.check_that(float(basket['paid_amount']),
                         equal_to(orders[0]['price']),
                         step=u'Проверяем, что скидка не применилась',
                         error=u'Скидка применилась!')

    @pytest.mark.parametrize('payment_params', Data.promocode_paymethods, ids=DataObject.ids_paymethod)
    @pytest.mark.parametrize('service', Data.services, ids=DataObject.ids_service)
    def test_base_payment_refund(self, service, payment_params):
        user = uids.get_random_of(uids.all_)
        paymethod, amount = payment_params.paymethod, payment_params.amount
        promocode_id = simple.process_promocode_creating(service, promo_amount=amount)

        orders = simple.form_orders_for_create(service, user)
        with check_mode(CheckMode.FAILED):
            basket = simple.process_payment(service, user, orders=orders, paymethod=paymethod,
                                            promocode_id=promocode_id)

        basket = simple.check_basket(service, user=user, purchase_token=basket['purchase_token'],
                                     with_promocodes=True)

        orders_for_update = simple.form_orders_for_update(orders)
        simple.update_basket(service, orders=orders_for_update, user=user,
                             trust_payment_id=basket['trust_payment_id'])
        simple.wait_until_real_postauth(service, user=user,
                                        trust_payment_id=basket['trust_payment_id'])

        composite_payments = simple.split_basket_to_composite_payments(service, user=user,
                                                                       purchase_token=basket['purchase_token'])
        with check_mode(CheckMode.FAILED):
            for composite_payment in composite_payments:
                simple.process_refund(service, user=user,
                                      trust_payment_id=composite_payment['trust_payment_id'],
                                      basket=composite_payment)

    @pytest.mark.parametrize('service', Data.services, ids=DataObject.ids_service)
    def test_pay_promocode_after_failure_payment(self, service):
        """
        Тест по мотивам https://st.yandex-team.ru/TRUST-2978
        Проводится платеж с промокодом и он фейлится по какой-то причине
        В таком случае в повторном платеже той же картой промокод тоже должен быть доступен
        """
        user = uids.get_random_of(uids.all_)
        promo_amount = defaults.Promocode.promocode_amount_part
        paymethod = TrustWebPage(Via.card(get_card(cvn=CVN.not_enough_funds_RC51)))
        promocode_id = simple.process_promocode_creating(service, promo_amount=promo_amount)

        orders = simple.form_orders_for_create(service, user)
        with reporter.step(u'Проводим неуспешный платеж с промокодом'), check_mode(CheckMode.IGNORED):
            simple.process_payment(service, user, orders=orders, paymethod=paymethod,
                                   promocode_id=promocode_id, should_failed=True)

        with reporter.step(u'Проводим успешный платеж, проверяем что промокод не протух'), check_mode(
                CheckMode.FAILED):
            paymethod.via.card.update({'cvn': CVN.base_success})
            basket = simple.process_payment(service, user, orders=orders, paymethod=paymethod,
                                            promocode_id=promocode_id, init_paymethod=False)

            basket = simple.check_basket(service, user=user, purchase_token=basket['purchase_token'],
                                         with_promocodes=True)
            self.check_promocode_payment_for_composite(basket, promo_amount)
            check.check_that(promocode_id, equal_to(basket['promocodes']['applied_promocode_id']),
                             step=u'Проверяем что применился корректный промокод',
                             error=u'Применился некорректный промокод')

    @pytest.mark.parametrize('service', Data.services, ids=DataObject.ids_service)
    def test_pay_promocode_after_refund(self, service):
        """
        Тест по мотивам https://st.yandex-team.ru/TRUST-2978
        Проводится платеж с промокодом, затем промокод рефаандится
        В таком случае промокод должен быть по прожнему доступен
        """
        user = uids.get_random_of(uids.all_)
        promo_amount = defaults.Promocode.promocode_amount_part
        paymethod = TrustWebPage(Via.card(get_card()))
        promocode_id = simple.process_promocode_creating(service, promo_amount=promo_amount)

        orders = simple.form_orders_for_create(service, user)
        with reporter.step(u'Проводим платеж с промокодом и рефандим промокодную часть'):
            basket = simple.process_payment(service, user, orders=orders, paymethod=paymethod,
                                            promocode_id=promocode_id, need_postauthorize=True)

            composite_payments = simple.split_basket_to_composite_payments(service, user=user,
                                                                           purchase_token=basket['purchase_token'])
            for composite_payment in composite_payments:
                if composite_payment['payment_method'] == 'new_promocode':
                    simple.process_refund(service, user=user,
                                          trust_payment_id=composite_payment['trust_payment_id'],
                                          basket=composite_payment)

        with reporter.step(u'Проводим успешный платеж, '
                           u'проверяем что промокод успешно применился'), check_mode(CheckMode.FAILED):
            paymethod.via.card.update({'cvn': CVN.base_success})
            basket = simple.process_payment(service, user, orders=orders, paymethod=paymethod,
                                            promocode_id=promocode_id, init_paymethod=False)

            basket = simple.check_basket(service, user=user, purchase_token=basket['purchase_token'],
                                         with_promocodes=True)
            self.check_promocode_payment_for_composite(basket, promo_amount)
            check.check_that(promocode_id, equal_to(basket['promocodes']['applied_promocode_id']),
                             step=u'Проверяем что применился корректный промокод',
                             error=u'Применился некорректный промокод')

    # slppls: билетных промокодов в вебе больше нет. Код оставлю, вдруг в таком виде они появятся у другого сервиса
    # @pytest.mark.parametrize('statuses', Data.status_combination, ids=ids_many_promo_statuses)
    # @pytest.mark.parametrize('payment_params', Data.promocode_paymethods, ids=ids_paymethods)
    # @pytest.mark.parametrize('service', Data.services, ids=ids_service)
    # def test_many_promocodes_in_one_payment(self, service, payment_params, statuses):
    #     user = uids.get_random_of(uids.all_)
    #     paymethod, amount = payment_params
    #     useless_series, useless_promo, useless_id = simple.process_promocode_creating(service,
    #                                                                                   promo_status=statuses[0],
    #                                                                                   promo_amount=amount)
    #     success_series, success_promo, success_id = simple.process_promocode_creating(service,
    #                                                                                   promo_status=statuses[1],
    #                                                                                   promo_amount=amount)
    #     orders = simple.form_orders_for_create(service, user)
    #     with check_mode(CheckMode.FAILED):
    #         basket = simple.process_payment(service, user, orders=orders, paymethod=paymethod,
    #                                         accept_promo=[str(useless_series), str(success_series)],
    #                                         promocode=success_promo, intermediate_promos=[useless_promo])
    #     basket = simple.check_basket(service, user=user, purchase_token=basket['purchase_token'],
    #                                  with_promocodes=True)
    #     check.check_iterable_contains(basket['promocodes']['attempted_promocode_ids'], [useless_id, success_id])
    #     check.check_that(basket['promocodes']['applied_promocode_id'], equal_to(success_id),
    #                      step=u'Проверяем, что применился ожидаемый промокод',
    #                      error=u'Применился некорректный промокод')

    @pytest.mark.parametrize('payment_params', Data.failed_params_combination, ids=ids_promo_params)
    @pytest.mark.parametrize('service', Data.services, ids=DataObject.ids_service)
    def test_amount_failed(self, service, payment_params):
        user = uids.get_random_of(uids.all_)
        paymethod, amount, full_payment_only, partial_only, error = \
            payment_params.paymethod, payment_params.amount, \
            payment_params.full_payment_only, payment_params.partial_only, \
            payment_params.error
        promocode_id = simple.process_promocode_creating(service, promo_amount=amount,
                                                         full_payment_only=full_payment_only,
                                                         partial_only=partial_only)
        paymethod.init(service, user)
        orders = simple.form_orders_for_create(service, user)
        with check_mode(CheckMode.FAILED):
            basket = simple.create_basket(service, user=user, orders=orders, paymethod_id=paymethod.id,
                                          promocode_id=promocode_id)
            simple.pay_basket(service, user=user,
                              trust_payment_id=basket['trust_payment_id'])
        with check_mode(CheckMode.IGNORED):
            resp = webapi.preview_payment(basket['purchase_token'], promocode_id=promocode_id)['promocode']
            check.check_that(resp, has_entries(error),
                             step=u'Проверяем, что в ответе ошибка про разницу в amount',
                             error=u'Некорректный ответ preview_payment')

    def test_service_failed(self):
        user = uids.get_random_of(uids.all_)
        # https://st.yandex-team.ru/TRUST-2125
        # создаем корзину для одного промокодного сервиса, а промосерию для другого, в результате ждём ошибку
        first_service = Services.TICKETS
        second_service = Services.EVENTS_TICKETS
        paymethod = TrustWebPage(Via.Promocode())
        promocode_id = simple.process_promocode_creating(second_service)
        paymethod.init(first_service, user)

        orders = simple.form_orders_for_create(first_service, user)
        with check_mode(CheckMode.FAILED):
            basket = simple.create_basket(first_service, user=user, orders=orders, paymethod_id=paymethod.id,
                                          promocode_id=promocode_id)
            simple.pay_basket(first_service, user=user,
                              trust_payment_id=basket['trust_payment_id'])
            preview = webapi.preview_payment(basket['purchase_token'], promocode_id=promocode_id)
            check.check_that(preview['promocode'], deep_equals_to(expected.Promocode.wrong_service()),
                             step=u'Проверяем, что в ответе ошибка про некорректный сервис',
                             error=u'Некорректный ответ preview_payment')

    @pytest.mark.parametrize('test_params', Data.test_date_failed_case, ids=ids_status)
    @pytest.mark.parametrize('service', Data.services, ids=DataObject.ids_service)
    def test_date_failed(self, service, test_params):
        user = uids.get_random_of(uids.all_)
        status, waiting_error, date_param = \
            test_params.status, test_params.waiting_error, test_params.date_param
        paymethod = TrustWebPage(Via.Promocode())
        promocode_id = simple.process_promocode_creating(service, promo_status=status)
        paymethod.init(service, user)
        orders = simple.form_orders_for_create(service, user)
        with check_mode(CheckMode.FAILED):
            basket = simple.create_basket(service, user=user, orders=orders, paymethod_id=paymethod.id,
                                          promocode_id=promocode_id)
            simple.pay_basket(service, user=user,
                              trust_payment_id=basket['trust_payment_id'])
            preview = webapi.preview_payment(basket['purchase_token'], promocode_id=promocode_id)['promocode']
            check.check_that(preview, has_entries(waiting_error),
                             step=u'Проверяем, что в ответе ошибка про некорректный промокод по дате',
                             error=u'Некорректный ответ preview_payment')
            check.check_iterable_contains(preview, [date_param])

    @pytest.mark.parametrize('paymethod', Data.multi_promocode_paymethods, ids=DataObject.ids_paymethod)
    @pytest.mark.parametrize('service', Data.services, ids=DataObject.ids_service)
    def test_discount_with_promo(self, service, paymethod):
        user = uids.get_random_of(uids.all_)
        promocode_id = simple.process_promocode_creating(service)
        orders = simple.form_orders_for_create(service, user)
        with check_mode(CheckMode.FAILED):
            basket = simple.process_payment(service, user, orders=orders, paymethod=paymethod,
                                            promocode_id=promocode_id, discounts=[defaults.Discounts.id100['id'], ])
        basket = simple.check_basket(service, user=user, purchase_token=basket['purchase_token'],
                                     with_promocodes=True)
        simple.check_promocode_in_payment(service, promocode_id, 'active', basket)
        basket_promocode_id = basket['promocodes']['applied_promocode_id']
        check.check_that(promocode_id, equal_to(basket_promocode_id),
                         step=u'Проверяем что применился корректный промокод',
                         error=u'Применился некорректный промокод')
        # TODO: возможно стоит сделать это чуть более расширенным
        check.check_iterable_contains(basket, must_contains=['discount_details', 'discounts'])

    @staticmethod
    def check_basket_update_dt(service, user, trust_payment_id, step, error, previous_update_dt):
        check.check_that(simple.check_basket(service, user=user, trust_payment_id=trust_payment_id),
                         has_entries(
                             expected.RegularBasket.basket_with_update_dt(previous_update_dt=previous_update_dt)),
                         step=step, error=error)

    @staticmethod
    def get_date_second_ago():
        return Date.shift_date(datetime.datetime.now(), seconds=-1)

    @reporter.story(stories.General.Rules)
    @pytest.mark.parametrize('paymethod', Data.multi_promocode_paymethods, ids=DataObject.ids_paymethod)
    @pytest.mark.parametrize('service', Data.services, ids=DataObject.ids_service)
    def test_update_dt_for_composite_payment(self, paymethod, service):
        user = uids.get_random_of(uids.all_)
        paymethod.init(service, user)
        promocode_id = simple.process_promocode_creating(service)
        orders = simple.form_orders_for_create(service, user)
        with reporter.step(u'Создаем корзину и проверяем что в ней проставился update_dt'):
            previous_update_dt = self.get_date_second_ago()
            basket = simple.create_basket(service, user=user,
                                          orders=orders,
                                          paymethod_id=paymethod.id,
                                          discounts=[defaults.Discounts.id100['id'], ],
                                          promocode_id=promocode_id)
            self.check_basket_update_dt(service, user=user, trust_payment_id=basket['trust_payment_id'],
                                        step=u'Проверяем что после создания корзины в ней проставился update_dt',
                                        error=u'В корзине не проставился update_dt после создания',
                                        previous_update_dt=previous_update_dt)
        with reporter.step(u'Оплачиваем корзину и проверяем что в ней изменился update_dt'):
            previous_update_dt = self.get_date_second_ago()
            payment_form = simple.pay_basket(service, user=user,
                                             trust_payment_id=basket['trust_payment_id']).get('payment_form')
            trust.pay_by(paymethod, service, user=user, payment_form=payment_form,
                         purchase_token=basket['purchase_token'])
            simple.wait_until_payment_done(service, user=user,
                                           purchase_token=basket['purchase_token'])
            self.check_basket_update_dt(service, user=user, trust_payment_id=basket['trust_payment_id'],
                                        step=u'Проверяем что после оплаты корзины в ней изменился update_dt',
                                        error=u'В корзине не изменился update_dt после ее оплаты',
                                        previous_update_dt=previous_update_dt)
        with reporter.step(u'Поставторизуем корзину и проверяем что в ней изменился update_dt'):
            previous_update_dt = self.get_date_second_ago()
            simple.process_postauthorize(service, user=user,
                                         trust_payment_id=basket['trust_payment_id'], orders=orders)
            self.check_basket_update_dt(service, user=user, trust_payment_id=basket['trust_payment_id'],
                                        step=u'Проверяем что после поставторизации корзины в ней изменился update_dt',
                                        error=u'В корзине не изменился update_dt после ее поставторизации',
                                        previous_update_dt=previous_update_dt)
        with reporter.step(u'Рефандим композитные платежи и проверяем что в них изменился update_dt'):
            composite_payments = simple.split_basket_to_composite_payments(service, user=user,
                                                                           purchase_token=basket['purchase_token'])
            with check_mode(CheckMode.FAILED):
                previous_update_dt = self.get_date_second_ago()
                for composite_payment in composite_payments:
                    simple.process_refund(service, user=user,
                                          trust_payment_id=composite_payment['trust_payment_id'],
                                          basket=composite_payment)
                    self.check_basket_update_dt(service, user=user, trust_payment_id=basket['trust_payment_id'],
                                                step=u'Проверяем что после возврата по композитному платежа в нем '
                                                     u'изменился update_dt',
                                                error=u'В композитном платежа не изменился update_dt после возврата',
                                                previous_update_dt=previous_update_dt)
                    self.check_basket_update_dt(service, user=user, trust_payment_id=basket['trust_payment_id'],
                                                step=u'Проверяем что после возврата по композитному платежу '
                                                     u'в родительском платеже изменился update_dt',
                                                error=u'В родительском платеже не изменился update_dt после возврата '
                                                      u'по композитному платежу',
                                                previous_update_dt=previous_update_dt)

    @pytest.mark.parametrize('test_data', Data.test_data_apple_token, ids=DataObject.ids)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure, ids=DataObject.ids_orders)
    @pytest.mark.parametrize('service', Data.services, ids=DataObject.ids_service)
    def test_apple_token(self, service, test_data, orders_structure):
        paymethod, user_type = test_data.paymethod, test_data.user_type
        user = uids.get_random_of_type(user_type)
        orders = simple.form_orders_for_create(service, user, orders_structure)
        promocode_id = simple.process_promocode_creating(service)
        with check_mode(CheckMode.FAILED):
            basket = simple.process_payment(service, user, orders=orders, paymethod=paymethod,
                                            need_postauthorize=True, promocode_id=promocode_id)
            simple.check_promocode_in_payment(service, promocode_id, 'active', basket)
            simple.wait_until_real_postauth(service, user=user,
                                            trust_payment_id=basket['trust_payment_id'])
            simple.process_refund(service, trust_payment_id=basket['trust_payment_id'], basket=basket, user=user)

    @pytest.mark.parametrize('test_data', Data.test_data_apple_token, ids=DataObject.ids)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure, ids=DataObject.ids_orders)
    @pytest.mark.parametrize('service', Data.services, ids=DataObject.ids_service)
    def test_apple_token_with_discount(self, service, test_data, orders_structure):
        paymethod, user_type = test_data.paymethod, test_data.user_type
        user = uids.get_random_of_type(user_type)
        orders = simple.form_orders_for_create(service, user, orders_structure)
        promocode_id = simple.process_promocode_creating(service)
        with check_mode(CheckMode.FAILED):
            basket = simple.process_payment(service, user, orders=orders, paymethod=paymethod, need_postauthorize=True,
                                            promocode_id=promocode_id, discounts=[defaults.Discounts.id100['id'], ])
            simple.check_promocode_in_payment(service, promocode_id, 'active', basket)
            simple.wait_until_real_postauth(service, user=user,
                                            trust_payment_id=basket['trust_payment_id'])
            check.check_iterable_contains(basket, must_contains=['discount_details', 'discounts'])


@reporter.feature(features.General.Promocodes)
@reporter.story(stories.General.NotUniquePromocodes)
class TestNotUniquePromocodes(object):
    @pytest.mark.parametrize('service', Data.services, ids=DataObject.ids_service)
    def test_payment_twice_different_card(self, service):
        """
        Оплата с неуникальным промокодом двумя разными картами - успешный кейс
        Оплата неуникальным промокодом возможна, если карты разные, и невозможна, если одна и та же
        """
        user = uids.get_random_of(uids.all_)
        promocode_id = simple.process_notunique_promocode_creating(service)
        orders = simple.form_orders_for_create(service, user)

        paymethods = list()
        paymethods.append(TrustWebPage(Via.card(get_card())))
        paymethods.append(TrustWebPage(Via.card(get_card())))

        for paymethod in paymethods:
            with check_mode(CheckMode.FAILED):
                simple.process_payment(service, user, orders=orders, paymethod=paymethod, promocode_id=promocode_id)

    @pytest.mark.parametrize('paymethod', Data.multi_promocode_paymethods, ids=DataObject.ids_paymethod)
    @pytest.mark.parametrize('service', Data.services, ids=DataObject.ids_service)
    def test_payment_twice_same_card(self, service, paymethod):
        """
        Оплата с неуникальным промокодом одной и той же картой - неуспешный кейс
        Оплата неуникальным промокодом возможна, если карты разные, и невозможна, если одна и та же
        """
        user = uids.get_random_of(uids.all_)
        promocode_id = simple.process_notunique_promocode_creating(service)
        orders = simple.form_orders_for_create(service, user)
        simple.process_payment(service, user, orders=orders, paymethod=paymethod,
                               promocode_id=promocode_id)
        with check_mode(CheckMode.IGNORED):
            basket = simple.process_payment(service, user, orders=orders, paymethod=paymethod, should_failed=True,
                                            promocode_id=promocode_id)
        check.check_that(basket, has_entries(expected.Promocode.already_used()),
                         step=u'Проверяем, что в композитном платеже указана ошибка о невозможности оплатить дважды',
                         error=u'Некорректный ответ в композитном платеже')

    @pytest.mark.parametrize('service', Data.services, ids=DataObject.ids_service)
    def test_payment_same_linked_card_and_card(self, service):
        """
        Оплата с неуникальным промокодом одной и той же картой, привязанной и нет - неуспешный кейс
        Оплата неуникальным промокодом возможна, если карты разные, и невозможна, если одна и та же
        """
        user = uids.get_random_of(uids.all_)
        promocode_id = simple.process_notunique_promocode_creating(service)
        card = get_card()
        paymethod_linked_card = TrustWebPage(Via.LinkedCard(card))
        paymethod_card = TrustWebPage(Via.card(card))
        orders = simple.form_orders_for_create(service, user)
        simple.process_payment(service, user, orders=orders, paymethod=paymethod_linked_card,
                               promocode_id=promocode_id)
        with check_mode(CheckMode.IGNORED):
            basket = simple.process_payment(service, user, orders=orders, paymethod=paymethod_card, should_failed=True,
                                            promocode_id=promocode_id)
        check.check_that(basket, has_entries(expected.Promocode.already_used()),
                         step=u'Проверяем, что в композитном платеже указана ошибка о невозможности оплатить дважды',
                         error=u'Некорректный ответ в композитном платеже')

    @pytest.mark.parametrize('service', Data.services, ids=DataObject.ids_service)
    def test_decrement_count_after_payment(self, service):
        # Тест, проверяющий уменьшение счётчика использования неуникальных промокодов с каждой оплатой
        # Также проверяется невозможность оплатить, когда счётчик на нуле
        user = uids.get_random_of(uids.all_)
        paymethods = list()
        paymethods.append(TrustWebPage(Via.card(get_card())))
        paymethods.append(TrustWebPage(Via.card(get_card())))
        promocode_limit = len(paymethods)
        promocode_id = simple.process_notunique_promocode_creating(service, usage_limit=promocode_limit)
        orders = simple.form_orders_for_create(service, user)
        usage_limit = simple.get_promocode_status(service, promocode_id=promocode_id)['result']['usage_limit']
        for paymethod in paymethods:
            with check_mode(CheckMode.FAILED):
                basket = simple.process_payment(service, user, orders=orders, paymethod=paymethod,
                                                promocode_id=promocode_id)
            usage_number = simple.get_promocode_status(service, promocode_id=promocode_id)['result']['usage_number']
        check.check_that(usage_limit, is_(usage_number),
                         step=u'Проверяем, что количество оплат сравнялось с лимитом',
                         error=u'Некорректный результат лимита и оплат')
        preview = webapi.preview_payment(basket['purchase_token'], promocode_id=promocode_id)['promocode']
        check.check_that(preview, deep_equals_to(expected.Promocode.applied()),
                         step=u'Проверяем, что в ответе ошибка о том, что промокоды кончились',
                         error=u'Некорректный ответ preview_payment')

    @pytest.mark.parametrize('test_data', Data.test_data_apple_token, ids=DataObject.ids)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure, ids=DataObject.ids_orders)
    @pytest.mark.parametrize('service', Data.services, ids=DataObject.ids_service)
    def test_apple_token(self, service, test_data, orders_structure):
        paymethod, user_type = test_data.paymethod, test_data.user_type
        user = uids.get_random_of_type(user_type)
        orders = simple.form_orders_for_create(service, user, orders_structure)
        promocode_id = simple.process_notunique_promocode_creating(service)
        with check_mode(CheckMode.FAILED):
            for i in range(2):
                basket = simple.process_payment(service, user, orders=orders, paymethod=paymethod,
                                                need_postauthorize=True, promocode_id=promocode_id)
                simple.wait_until_real_postauth(service, user=user,
                                                trust_payment_id=basket['trust_payment_id'])

    @pytest.mark.parametrize('test_data', Data.test_data_apple_token, ids=DataObject.ids)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure, ids=DataObject.ids_orders)
    @pytest.mark.parametrize('service', Data.services, ids=DataObject.ids_service)
    def test_apple_token_with_discount(self, service, test_data, orders_structure):
        paymethod, user_type = test_data.paymethod, test_data.user_type
        user = uids.get_random_of_type(user_type)
        orders = simple.form_orders_for_create(service, user, orders_structure)
        promocode_id = simple.process_notunique_promocode_creating(service)
        with check_mode(CheckMode.FAILED):
            for i in range(2):
                basket = simple.process_payment(service, user, orders=orders, paymethod=paymethod,
                                                need_postauthorize=True, promocode_id=promocode_id,
                                                discounts=[defaults.Discounts.id100['id'], ])
                simple.wait_until_real_postauth(service, user=user,
                                                trust_payment_id=basket['trust_payment_id'])
                check.check_iterable_contains(basket, must_contains=['discount_details', 'discounts'])


if __name__ == '__main__':
    pytest.main()