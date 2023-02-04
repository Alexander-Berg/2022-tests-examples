# coding=utf-8
import pytest
from hamcrest import has_items, equal_to, is_in

import btestlib.reporter as reporter
from btestlib.constants import Services
from btestlib.utils import CheckMode, check_mode, Web
from simpleapi.common.payment_methods import TrustWebPage, Via
from simpleapi.data import defaults
from simpleapi.data import features, stories
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import get_card
from simpleapi.matchers.deep_equals import deep_contains
from simpleapi.steps import check_steps as check
from simpleapi.steps import expected_steps as expected
from simpleapi.steps import masterpass_steps as masterpass
from simpleapi.steps import simple_steps as simple
from simpleapi.steps import trust_steps as trust
from simpleapi.steps import web_payment_steps as web

__author__ = 'slppls'

service = Services.TICKETS

"""
https://st.yandex-team.ru/TRUST-3766
"""


def ids_user(val):
    user = val['user']
    if user is uids.anonymous:
        return 'anonymous_user'
    elif user.has_phones:
        return 'user_with_phone-' + user.uid
    else:
        return 'user_without_phone-' + user.uid


class Data(object):
    masterpass_discount = [
        defaults.Discounts.id300,
    ]
    test_card_views = [
        [get_card(), ],
        [get_card() for _ in range(3)],
    ]


@reporter.feature(features.Service.Tickets)
@reporter.story(stories.General.BindingCard)
class TestBinding(object):
    def test_card_not_in_masterpass_and_not_in_trust(self):
        # Карта не привязана в мастерпассе, карта не привязана в трасте - должна привязаться и туда, и туда
        user = uids.get_random_of_type(uids.Types.random_with_phone)
        phone = user.phones[0]
        paymethod = TrustWebPage(Via.card(get_card(), save_card=True), in_browser=True)

        orders = simple.form_orders_for_create(service, user)
        with check_mode(CheckMode.FAILED):
            basket = simple.process_payment(service=service, user=user, paymethod=paymethod, orders=orders,
                                            user_phone=str(phone))

        check.check_that(basket['bound_to'], has_items(defaults.BoundTo.masterpass, defaults.BoundTo.trust),
                         step=u'Проверяем, что карта привязана и в трасте, и в мастерпассе',
                         error=u'Карта привязана некорректно!')
        masterpass.check_card_in_masterpass_cards(phone, paymethod.via.card)

    def test_card_not_in_masterpass_but_in_trust(self):
        # карта не привязана в мастерпассе, но привязана в трасте - должна привязаться в мастерпасс
        user = uids.get_random_of_type(uids.Types.random_with_phone)
        phone = user.phones[0]
        paymethod = TrustWebPage(Via.linked_card(get_card()), in_browser=True)

        orders = simple.form_orders_for_create(service, user)
        with check_mode(CheckMode.FAILED):
            basket = simple.process_payment(service=service, user=user, paymethod=paymethod, orders=orders,
                                            user_phone=str(phone))

        check.check_that(basket['bound_to'], has_items(defaults.BoundTo.masterpass, defaults.BoundTo.trust),
                         step=u'Проверяем, что карта привязана и в трасте, и в мастерпассе',
                         error=u'Карта привязана некорректно!')
        masterpass.check_card_in_masterpass_cards(phone, paymethod.via.card)

    def test_card_in_masterpass_but_not_in_trust(self):
        # карта привязана в мастерпассе, но не привязана в трасте - должна привязаться в траст
        user = uids.get_random_of_type(uids.Types.random_with_phone)
        phone = user.phones[0]
        paymethod = TrustWebPage(Via.linked_card(get_card(), masterpass_bind_to=phone,
                                                 bind_only_in_masterpass=True),
                                 in_browser=True)

        orders = simple.form_orders_for_create(service, user)
        with check_mode(CheckMode.FAILED):
            basket = simple.process_payment(service=service, user=user, paymethod=paymethod, orders=orders,
                                            user_phone=str(phone))

        check.check_that(basket['bound_to'], has_items(defaults.BoundTo.trust, defaults.BoundTo.masterpass),
                         step=u'Проверяем, что карта привязана и в трасте, и в мастерпассе',
                         error=u'Карта привязана некорректно!')

    def test_card_in_masterpass_and_in_trust(self):
        # карта привязана в мастерпассе, карта привязана в трасте - просто успешная оплата (:
        user = uids.get_random_of_type(uids.Types.random_with_phone)
        phone = user.phones[0]
        paymethod = TrustWebPage(Via.linked_card(get_card(), masterpass_bind_to=phone),
                                 in_browser=True)

        orders = simple.form_orders_for_create(service, user)
        with check_mode(CheckMode.FAILED):
            basket = simple.process_payment(service=service, user=user, paymethod=paymethod, orders=orders,
                                            user_phone=str(phone))

        check.check_that(basket['bound_to'], has_items(defaults.BoundTo.trust, defaults.BoundTo.masterpass),
                         step=u'Проверяем, что карта привязана и в трасте, и в мастерпассе',
                         error=u'Карта привязана некорректно!')

    def test_unbind_card_on_payment_page(self):
        user = uids.get_random_of_type(uids.Types.random_with_phone)
        card = get_card()
        phone = user.phones[0]
        paymethod = TrustWebPage(Via.linked_card(card))

        masterpass.process_binding_to_masterpass(phone, card)
        card_id = simple.get_masterpass_card_from_list_payment_methods(service=service, user=user, phone=phone,
                                                                       masterpass_fingerprint_seed=user.uid, card=card)

        payment_form = simple.process_to_payment_form(service, user=user,
                                                      paymethod=paymethod, init_paymethod=False, user_phone=phone)

        with Web.DriverProvider() as driver:
            web.paymethods_by_services.get(service).unbind_linked_card(card_id=card_id,
                                                                       payment_form=payment_form, driver=driver)
        masterpass.check_card_not_in_masterpass_cards(phone, paymethod.via.card)

    @pytest.mark.parametrize('card_list', Data.test_card_views,
                             ids=lambda card_list: 'card count in trust = {}'.format(len(card_list)))
    def test_list_payment_methods(self, card_list):
        user = uids.get_random_of_type(uids.Types.random_with_phone)
        phone = user.phones[0]

        trust.unbind_all_cards_of(user, service)
        trust_cards, _ = trust.process_binding(user=user, cards=card_list, multiple=1)

        mp_card = get_card()
        masterpass.process_binding_to_masterpass(phone, mp_card)
        mp_card_id = simple.get_masterpass_card_from_list_payment_methods(service=service, user=user, phone=phone,
                                                                          masterpass_fingerprint_seed=user.uid,
                                                                          card=mp_card)
        masterpass.check_card_in_masterpass_cards(phone, mp_card)
        trust_cards.append(mp_card_id)

        paymethods = simple.list_payment_methods(service=service, user=user, phone=phone,
                                                 masterpass_fingerprint_seed=user.uid)[0]['payment_methods']
        for card_id in trust_cards:
            check.check_that(card_id, is_in(paymethods),
                             step=u'Проверяем что в list_payment_methods существует карта {}'.format(card_id),
                             error=u'Карта {} отсутствует!'.format(card_id))


@reporter.feature(features.Service.Tickets)
class TestPayments(object):
    @reporter.story(stories.General.Payment)
    def test_payment_cycle_with_refund(self):
        user = uids.get_random_of_type(uids.Types.random_with_phone)
        phone = user.phones[0]
        paymethod = TrustWebPage(Via.linked_card(get_card(), masterpass_bind_to=phone,
                                                 bind_only_in_masterpass=True), in_browser=True)

        orders = simple.form_orders_for_create(service, user)
        with check_mode(CheckMode.FAILED):
            basket = simple.process_payment(service=service, user=user, paymethod=paymethod, orders=orders,
                                            user_phone=str(phone), need_postauthorize=True)
            simple.wait_until_real_postauth(service, user=user,
                                            trust_payment_id=basket['trust_payment_id'])
            simple.process_refund(service, trust_payment_id=basket['trust_payment_id'], basket=basket, user=user)

    @reporter.story(stories.General.Payment)
    def test_payment_cycle_with_reversal(self):
        user = uids.get_random_of_type(uids.Types.random_with_phone)
        phone = user.phones[0]
        paymethod = TrustWebPage(Via.linked_card(get_card(), masterpass_bind_to=phone,
                                                 bind_only_in_masterpass=True), in_browser=True)

        orders = simple.form_orders_for_create(service, user)
        with check_mode(CheckMode.FAILED):
            basket = simple.process_payment(service=service, user=user, paymethod=paymethod, orders=orders,
                                            user_phone=str(phone))

            orders_for_update = simple.form_orders_for_update(orders, default_action='cancel')
            simple.process_postauthorize(service, user, basket['trust_payment_id'],
                                         orders_for_update=orders_for_update)

    @reporter.story(stories.General.Discount)
    @pytest.mark.parametrize('discount', Data.masterpass_discount,
                             ids=lambda discount: 'discount_id={}'.format(discount['id']))
    def test_discount(self, discount):
        user = uids.get_random_of_type(uids.Types.random_with_phone)
        phone = user.phones[0]
        paymethod = TrustWebPage(Via.linked_card(get_card(), masterpass_bind_to=phone,
                                                 bind_only_in_masterpass=True), in_browser=True)

        orders = simple.form_orders_for_create(service, user)

        with check_mode(CheckMode.FAILED):
            basket = simple.process_payment(service, user, orders=orders, paymethod=paymethod,
                                            discounts=[discount['id'], ], user_phone=phone)

        basket = simple.check_basket(service, user=user, purchase_token=basket['purchase_token'])
        check.check_that(basket, deep_contains(expected.RegularBasket.paid(paymethod, orders,
                                                                           discounts=[discount, ],
                                                                           with_email=True)))

    @reporter.story(stories.General.Promocodes)
    @pytest.mark.parametrize('discount', Data.masterpass_discount,
                             ids=lambda discount: 'discount_id={}'.format(discount['id']))
    def test_discount_with_promo(self, discount):
        user = uids.get_random_of_type(uids.Types.random_with_phone)
        phone = user.phones[0]
        paymethod = TrustWebPage(Via.linked_card(get_card(), masterpass_bind_to=phone,
                                                 bind_only_in_masterpass=True), in_browser=True)
        promocode_id = simple.process_promocode_creating(service)

        orders = simple.form_orders_for_create(service, user)
        with check_mode(CheckMode.FAILED):
            basket = simple.process_payment(service, user, orders=orders, paymethod=paymethod,
                                            promocode_id=promocode_id, discounts=[discount['id'], ], user_phone=phone)

        basket = simple.check_basket(service, user=user, purchase_token=basket['purchase_token'], with_promocodes=True)
        simple.check_promocode_in_payment(service, promocode_id, 'active', basket)
        basket_promocode_id = basket['promocodes']['applied_promocode_id']
        check.check_that(promocode_id, equal_to(basket_promocode_id),
                         step=u'Проверяем что применился корректный промокод',
                         error=u'Применился некорректный промокод')
        check.check_iterable_contains(basket, must_contains=['discount_details', 'discounts'])
