# coding=utf-8
from copy import deepcopy

import pytest
from hamcrest import equal_to, is_, is_in, not_

import btestlib.reporter as reporter
from btestlib.constants import Services
from btestlib.utils import CheckMode, check_mode, not_none
from simpleapi.common.payment_methods import TrustWebPage, PurchaseToken, LinkedCard, Via
from simpleapi.common.utils import DataObject
from simpleapi.data import defaults, features, stories, marks
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import get_card, get_masked_number
from simpleapi.steps import kinopoisk_plus_api_steps as kp_steps
from simpleapi.steps import payments_api_steps as payments_api
from simpleapi.steps import trust_steps as trust
from simpleapi.steps import check_steps as check
from simpleapi.steps import mongo_steps as mongo

__author__ = 'fellow'

service = Services.KINOPOISK_PLUS


class Data(object):
    test_data_user_type = [uids.anonymous,
                           uids.Types.random_from_all]
    test_data_paymethod = [
        TrustWebPage(Via.card(get_card()), in_browser=True,
                     template_tag=defaults.TemplateTag.smarttv),
        TrustWebPage(Via.card(get_card()), in_browser=True,
                     template_tag=defaults.TemplateTag.mobile),
        TrustWebPage(Via.card(get_card()), in_browser=True,
                     template_tag=defaults.TemplateTag.desktop),
    ]

    orders_structure = [
        [{'region_id': 225, 'currency': 'RUB', 'price': 10},
         {'region_id': 225, 'currency': 'RUB', 'price': 20.5},
         ],
        None
    ]


@reporter.feature(features.Service.KinopoiskPlus)
class TestKinopoiskPlus(object):
    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('user_type', Data.test_data_user_type, ids=DataObject.ids_user_type)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure, ids=DataObject.ids_orders)
    @pytest.mark.parametrize('paymethod', Data.test_data_paymethod, ids=DataObject.ids_paymethod)
    def test_base_payment_cycle(self, user_type, paymethod, orders_structure):
        user = uids.get_random_of_type(user_type)

        with check_mode(CheckMode.FAILED):
            basket = payments_api.Payments.process(service, paymethod=paymethod, user=user,
                                                   orders_structure=orders_structure,
                                                   need_clearing=True)

            payments_api.Refunds.process(service, user, basket['purchase_token'])

    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('length', [16, 17, 18, 19], ids=DataObject.ids_card_length)
    def test_card_serial_numbers(self, length):
        paymethod = TrustWebPage(Via.card(get_card(length=length)), in_browser=True,
                                 template_tag=defaults.TemplateTag.smarttv)
        user = uids.anonymous

        with check_mode(CheckMode.FAILED):
            payments_api.Payments.process(service, paymethod=paymethod, user=user,
                                          need_clearing=True)

    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('user_type', Data.test_data_user_type, ids=DataObject.ids_user_type)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure, ids=DataObject.ids_orders)
    @pytest.mark.parametrize('paymethod', Data.test_data_paymethod, ids=DataObject.ids_paymethod)
    def test_pay_by_token_and_refund(self, user_type, paymethod, orders_structure):
        user = uids.get_random_of_type(user_type)

        with check_mode(CheckMode.FAILED):
            basket = payments_api.Payments.process(service, paymethod=paymethod, user=user,
                                                   orders_structure=orders_structure,
                                                   keep_token=True)

            paymethod_pt = PurchaseToken(purchase_token=basket['purchase_token'])

            basket = payments_api.Payments.process(service, paymethod=paymethod_pt, user=user,
                                                   need_clearing=True)

            payments_api.Refunds.process(service, user, basket['purchase_token'])

    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('user_type', Data.test_data_user_type, ids=DataObject.ids_user_type)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure, ids=DataObject.ids_orders)
    @pytest.mark.parametrize('paymethod', Data.test_data_paymethod, ids=DataObject.ids_paymethod)
    def test_pay_by_token_after_refund(self, user_type, paymethod, orders_structure):
        user = uids.get_random_of_type(user_type)

        with check_mode(CheckMode.FAILED):
            basket = payments_api.Payments.process(service, paymethod=paymethod, user=user,
                                                   orders_structure=orders_structure, keep_token=True,
                                                   need_clearing=True)

            payments_api.Refunds.process(service, user, basket['purchase_token'])

            paymethod_pt = PurchaseToken(purchase_token=basket['purchase_token'])

            payments_api.Payments.process(service, paymethod=paymethod_pt, user=user,
                                          need_clearing=True)

    @reporter.story(stories.CardsOperations.CardsBinding)
    @pytest.mark.parametrize('paymethod', deepcopy(Data.test_data_paymethod), ids=DataObject.ids_paymethod)
    def test_bind_scond_card(self, paymethod):
        """
            Проверяем, что вторая карта привязывается через Web.
            Задача на автоматизацию: TESTTRUST-16
            Баг: TRUST-5882
        """
        user = uids.get_random_of_type(uids.Types.random_from_all)
        trust.process_unbinding(user, service=service)
        trust.process_binding(user, get_card(), service)
        masked_number = get_masked_number(paymethod.via.card['card_number'])
        paymethod.via.save_card = True
        with check_mode(CheckMode.FAILED):
            payments_api.Payments.process(service, paymethod=paymethod, user=user, need_clearing=True)
        check.check_that(payments_api.find_card_by_masked_number(service, user, masked_number),
                         not_none,
                         step=u'Проверяем, что вторая карта привязалась к пользователю',
                         error=u'Вторая карта не привязалась к пользователю!')


@reporter.feature(features.Service.KinopoiskPlus)
@reporter.story(stories.CardsOperations.LinkedUsers)
class TestKinopoiskLinkedUids(object):
    @staticmethod
    def check_all_cards_bounded(user, cards_, service=service):
        with reporter.step(u'Проверяем что все карты {} привязаны к пользователю {}'.format(cards_, user)):
            paymethods = payments_api.PaymentMethods.get(service, user)[0]['bound_payment_methods'].keys()
            if not isinstance(cards_, (list, tuple)):
                cards_ = (cards_,)
            for card in cards_:
                check.check_that(card, is_in(paymethods))

    @staticmethod
    def check_no_cards_bounded(user, cards_, service=service):
        with reporter.step(u'Проверяем что ни одна из карт {} не привязана к пользователю {}'.format(cards_, user)):
            paymethods = payments_api.PaymentMethods.get(service, user)[0]['bound_payment_methods'].keys()
            if not isinstance(cards_, (list, tuple)):
                cards_ = (cards_,)
            for card in cards_:
                check.check_that(card, not_(is_in(paymethods)))

    @marks.no_parallel('kinopoisk_plus')
    def test_kinopoisk_card_which_bind_before_link_account_visible_in_passport(self):
        """
            Кейс №1 из TESTTRUST-20
            Проверяем, что карта, привязанная в Кинопоиске до связи аккаунтов, отображается в Паспорте и не отображается
            после отвязки аккаунтов.
        """
        passport_user = uids.get_random_of(uids.passport_users_for_kp)
        kp_step = kp_steps.KinopoiskPlus(passport_user)
        card_id = kp_step.binding_card_for_kp()
        kp_step.process_linking_users()
        TestKinopoiskLinkedUids.check_all_cards_bounded(user=passport_user, cards_=card_id)
        kp_step.process_unlinking_users()
        TestKinopoiskLinkedUids.check_no_cards_bounded(user=passport_user, cards_=card_id)

    @marks.no_parallel('kinopoisk_plus')
    def test_kinopoisk_card_invisible_in_passport_after_unbind_in_kinopoisk(self):
        """
            Кейс №2 из TESTTRUST-20
            Проверяем, что карта, привязанная в Кинопоиске, удалилась из Паспорта после удаления карты в Кинопоиске.
        """
        passport_user = uids.get_random_of(uids.passport_users_for_kp)
        kp_step = kp_steps.KinopoiskPlus(passport_user)
        card_id = kp_step.binding_card_for_kp()
        kp_step.process_linking_users()
        kp_step.delete_card_for_kp(card_id)
        TestKinopoiskLinkedUids.check_no_cards_bounded(user=passport_user, cards_=card_id)

    @marks.no_parallel('kinopoisk_plus')
    def test_kinopoisk_card_invisible_in_passport_and_delete_in_kinopoisk_after_unbind_in_passport(self):
        """
            Кейс №3 из TESTTRUST-20
            Проверяем, что карта, привязанная в Кинопоиске, удалилась из Паспорта и Кинопоиска
            после удаления карты в Паспорте.
        """
        passport_user = uids.get_random_of(uids.passport_users_for_kp)
        kp_step = kp_steps.KinopoiskPlus(passport_user)
        card_id = kp_step.binding_card_for_kp()
        kp_step.process_linking_users()
        trust.process_unbinding(passport_user, card_id, service)
        TestKinopoiskLinkedUids.check_no_cards_bounded(user=kp_step.kp_user, cards_=card_id)
        TestKinopoiskLinkedUids.check_no_cards_bounded(user=passport_user, cards_=card_id)

    @marks.no_parallel('kinopoisk_plus')
    def test_passport_card_which_bind_before_link_account_visible_in_kinopoisk(self):
        """
            Кейс №4 из TESTTRUST-20
            Проверяем, что карта, привязанная в Паспорте до связи аккаунтов, отображается в Кинопоиске и не отображается
            после отвязки аккаунтов.
        """
        passport_user = uids.get_random_of(uids.passport_users_for_kp)
        kp_step = kp_steps.KinopoiskPlus(passport_user)
        linked_cards, _ = trust.process_binding(passport_user, get_card(), service)
        kp_step.process_linking_users()
        TestKinopoiskLinkedUids.check_all_cards_bounded(user=kp_step.kp_user, cards_=linked_cards)
        kp_step.process_unlinking_users()
        TestKinopoiskLinkedUids.check_no_cards_bounded(user=kp_step.kp_user, cards_=linked_cards)

    @marks.no_parallel('kinopoisk_plus')
    def test_passport_card_invisible_in_kinopoisk_after_unbind_in_passport(self):
        """
            Кейс №5 из TESTTRUST-20
            Проверяем, что карта, привязанная в Паспорте, удалилась из Кинопоиска после удаления карты в Паспорте.
        """
        passport_user = uids.get_random_of(uids.passport_users_for_kp)
        kp_step = kp_steps.KinopoiskPlus(passport_user)
        linked_cards, _ = trust.process_binding(passport_user, get_card(), service)
        kp_step.process_linking_users()
        trust.process_unbinding(passport_user, linked_cards[0], service)
        TestKinopoiskLinkedUids.check_no_cards_bounded(user=kp_step.kp_user, cards_=linked_cards)

    @marks.no_parallel('kinopoisk_plus')
    def test_kinopoisk_card_which_bind_after_link_account_visible_in_passport(self):
        """
            Кейс №6 из TESTTRUST-20
            Проверяем, что карта, привязанная в Кинопоиске после связи аккаунтов, отображается в Паспорте.
        """
        passport_user = uids.get_random_of(uids.passport_users_for_kp)
        kp_step = kp_steps.KinopoiskPlus(passport_user)
        kp_step.process_linking_users()
        card_id = kp_step.binding_card_for_kp()
        TestKinopoiskLinkedUids.check_all_cards_bounded(user=passport_user, cards_=card_id)

    @marks.no_parallel('kinopoisk_plus')
    def test_passport_card_which_bind_after_link_account_visible_in_kinopoisk(self):
        """
            Кейс №7 из TESTTRUST-20
            Проверяем, что карта, привязанная в Паспорте после связи аккаунтов, отображается в Кинопоиске.
        """
        passport_user = uids.get_random_of(uids.passport_users_for_kp)
        kp_step = kp_steps.KinopoiskPlus(passport_user)
        kp_step.process_linking_users()
        linked_cards, _ = trust.process_binding(passport_user, get_card(), service)
        TestKinopoiskLinkedUids.check_all_cards_bounded(user=kp_step.kp_user, cards_=linked_cards)
        kp_step.process_unlinking_users()
        TestKinopoiskLinkedUids.check_no_cards_bounded(user=kp_step.kp_user, cards_=linked_cards)

    @staticmethod
    def check_one_card_id_for_one_card(cards):
        """
            На вход передается список из card_id одной физической карты привязанной к разным пользователям.
            Проверяем, что все card_id одинаковы.
        """
        check.check_that(len(set(cards)), is_(equal_to(1)),
                         step=u'Проверяем, что одна и та же карта привязалась с одинаковым card_id',
                         error=u'Одна и та же карта привязалась с разным card_id {}'.format(cards))

    @staticmethod
    def check_duplicate_cards(users):
        """
            Метод проверяет, что одна и та же карта, привязанная к связанным аккаунтам, не дублируется в
            ответе list_payment_methods для этих аккаунтов.
            Если карта
        """
        for user in users:
            user_methods = payments_api.PaymentMethods.get(service, user)[0]['bound_payment_methods'].keys()
            check.check_that(len(set(user_methods)), is_(len(user_methods)),
                             step=u'Проверяем, что в ответе lpm для пользователя карта выдается только один раз',
                             error=u'В ответе lpm для полльзвателя {} одна и та же карта выдалась дважды'.format(user))

    @marks.no_parallel('kinopoisk_plus')
    def test_same_card_in_kinopoisk_and_passport_is_not_duplicate(self):
        """
            Кейс №8 из TESTTRUST-20
            Проверяем, что одна и та же физическая карта, привязанная к аккаунтам Кинопоиска и Паспорта не дублируется
            в list_payment_methods для этих аккаунтов.
        """
        passport_user = uids.get_random_of(uids.passport_users_for_kp)
        kp_step = kp_steps.KinopoiskPlus(passport_user)
        card = get_card()
        linked_cards_p, _ = trust.process_binding(passport_user, [card, get_card()], service)
        kp_first_card = kp_step.binding_card_for_kp(card)
        kp_second_card = kp_step.binding_card_for_kp()
        linked_cards_kp = [kp_first_card, kp_second_card]
        kp_step.process_linking_users()
        TestKinopoiskLinkedUids.check_one_card_id_for_one_card([linked_cards_p[0], kp_first_card])
        TestKinopoiskLinkedUids.check_duplicate_cards([kp_step.kp_user, passport_user])
        TestKinopoiskLinkedUids.check_all_cards_bounded(user=kp_step.kp_user,
                                                        cards_=(linked_cards_kp + [linked_cards_p[1]]))
        TestKinopoiskLinkedUids.check_all_cards_bounded(user=passport_user,
                                                        cards_=(linked_cards_p + [linked_cards_kp[1]]))
        kp_step.process_unlinking_users()
        TestKinopoiskLinkedUids.check_all_cards_bounded(user=kp_step.kp_user, cards_=linked_cards_kp)
        TestKinopoiskLinkedUids.check_all_cards_bounded(user=passport_user, cards_=linked_cards_p)

    @marks.no_parallel('kinopoisk_plus')
    def test_same_card_in_kinopoisk_and_passport_delete_on_both_accounts_after_unbind_in_passport(self):
        """
            Кейс №9 из TESTTRUST-20
            Проверяем, что одна и та же физическая карта, привязанная к аккаунтам Кинопоиска и Паспорта удаляется из
            обоих аккаунтов, при удалении карты в Паспорте.
        """
        passport_user = uids.get_random_of(uids.passport_users_for_kp)
        kp_step = kp_steps.KinopoiskPlus(passport_user)
        card = get_card()
        linked_cards, _ = trust.process_binding(passport_user, card, service)
        kp_card = kp_step.binding_card_for_kp(card)
        kp_step.process_linking_users()
        trust.process_unbinding(passport_user, kp_card, service=service)
        TestKinopoiskLinkedUids.check_no_cards_bounded(user=kp_step.kp_user, cards_=kp_card)
        TestKinopoiskLinkedUids.check_no_cards_bounded(user=passport_user, cards_=linked_cards[0])

    @staticmethod
    def check_deduplicate_card(card_id_1, card_id_2, user):
        paymethods = payments_api.PaymentMethods.get(service, user)[0]['bound_payment_methods'].keys()
        with reporter.step(u'Проверяем что карты {} и {} корректно дедуплицируются '
                           u'у пользователя {}'.format(card_id_1, card_id_2, user)):
            if paymethods.count(card_id_1) and paymethods.count(card_id_2) and card_id_1 != card_id_2:
                raise AssertionError('Карты не дедуплицировались')

    @marks.no_parallel('kinopoisk_plus')
    def test_same_card_in_kinopoisk_and_passport_with_different_hash_not_duplicate(self):
        """
            Кейс №10 из TESTTRUST-20
            Проверяем, что одна и та же физическая карта, привязанная к аккаунтам Кинопоиска и Паспорта, с разным
            хешом обоих аккаунтов, не дублируется в list_payment_methods для этих аккаунтов.
        """
        passport_user = uids.get_random_of(uids.passport_users_for_kp)
        kp_step = kp_steps.KinopoiskPlus(passport_user)
        card = get_card()
        kp_card, payment_id = kp_step.binding_card_for_kp(card, True)
        mongo.Card.update_binding_hash(payment_id)
        mongo.PaymentMethodsCache.clean_lpm_cache_for_user(kp_step.kp_user)
        linked_cards, _ = trust.process_binding(passport_user, card, service)
        kp_step.process_linking_users()
        TestKinopoiskLinkedUids.check_deduplicate_card(kp_card, linked_cards[0], kp_step.kp_user)
        TestKinopoiskLinkedUids.check_deduplicate_card(kp_card, linked_cards[0], passport_user)
        kp_step.process_unlinking_users()
        TestKinopoiskLinkedUids.check_all_cards_bounded(user=kp_step.kp_user, cards_=kp_card)
        TestKinopoiskLinkedUids.check_all_cards_bounded(user=passport_user, cards_=linked_cards)

    @marks.no_parallel('kinopoisk_plus')
    @pytest.mark.skipif(True, reason="Wait info about business logic from 'nesterova-av'")
    def test_buy_subscription_in_kinopoisk_and_try_unbind_kinopoisk_card_in_passport(self):
        """
            Кейс №11 из TESTTRUST-20
            Проверяем, что при активной подписке на аккаунте Кинопоиска при попытке удалить карту, с которой идет
            оплата подписки, через Пасспорт падает ошибка и карта остается привязанной.
        """
        card = get_card()
        passport_user = uids.get_random_of(uids.passport_users_for_kp)
        kp_step = kp_steps.KinopoiskPlus(passport_user)
        kp_card = kp_step.binding_card_for_kp(card)
        kp_step.process_linking_users()
        paymethod = LinkedCard(Via.card(card))
        with payments_api.Subscriptions.create_normal(service, kp_step.kp_user) as subs:
            orders = [{"order_id": subs['order_id'], "currency": 'RUB', 'region_id': '225', "qty": 1}]
            payments_api.Payments.process(service, paymethod, user=kp_step.kp_user, orders=orders, currency='RUB')
            payments_api.Wait.until_subscription_continuation(service, kp_step.kp_user, subs['order_id'])
            with check_mode(CheckMode.IGNORED):
                trust.process_unbinding(passport_user, kp_card, service=Services.KINOPOISK_PLUS)
                # Тут нужно будет сделать проверку ошибки
                TestKinopoiskLinkedUids.check_all_cards_bounded(user=kp_step.kp_user, cards_=kp_card)
                TestKinopoiskLinkedUids.check_all_cards_bounded(user=passport_user, cards_=kp_card)
            trust.process_unbinding(passport_user, kp_card, service=Services.KINOPOISK_PLUS)
            TestKinopoiskLinkedUids.check_no_cards_bounded(user=kp_step.kp_user, cards_=kp_card)
            TestKinopoiskLinkedUids.check_no_cards_bounded(user=passport_user, cards_=kp_card)

    @marks.no_parallel('kinopoisk_plus')
    def test_pay_on_passport_by_kinopoisk_card(self):
        """
            Кейс №12 из TESTTRUST-20
            Совершаем покупку Паспортным аккаунтом, оплачиваем картой, привязанной на Кинопоиске.
        """
        card = get_card()
        passport_user = uids.get_random_of(uids.passport_users_for_kp)
        kp_step = kp_steps.KinopoiskPlus(passport_user)
        kp_step.binding_card_for_kp()
        kp_step.process_linking_users()
        paymethod = LinkedCard(Via.card(card))
        with check_mode(CheckMode.FAILED):
            basket = payments_api.Payments.process(service, paymethod=paymethod, user=passport_user,
                                                   need_clearing=True)
            payments_api.Refunds.process(service, passport_user, basket['purchase_token'])

    @marks.no_parallel('kinopoisk_plus')
    def test_pay_on_kinopoisk_by_passport_card(self):
        """
            Кейс №13 из TESTTRUST-20
            Совершаем покупку Кинопоисковым аккаунтом, оплачиваем картой, привязанной на Паспорте.
        """
        card = get_card()
        passport_user = uids.get_random_of(uids.passport_users_for_kp)
        kp_step = kp_steps.KinopoiskPlus(passport_user)
        trust.process_binding(passport_user, card, service)
        kp_step.process_linking_users()
        paymethod = LinkedCard(Via.card(card))
        with check_mode(CheckMode.FAILED):
            basket = payments_api.Payments.process(service, paymethod=paymethod, user=kp_step.kp_user,
                                                   need_clearing=True)
            payments_api.Refunds.process(service, kp_step.kp_user, basket['purchase_token'])

    # Тесты на взаимодействие фонишей и связаных КП UID'ов
    @marks.no_parallel('kinopoisk_plus')
    def test_kinopoisk_card_which_bind_before_link_account_visible_in_phonish(self):
        """
            Кейс №14 из TESTTRUST-20
            Проверяем, что карта, привязанная в Кинопоиске до связи аккаунтов, отображается у Фониша и не отображается
            после отвязки аккаунтов.
        """
        passport_user = uids.get_random_of(uids.with_linked_phonishes_for_kp)
        kp_step = kp_steps.KinopoiskPlus(passport_user)
        card_id = kp_step.binding_card_for_kp()
        kp_step.process_linking_users()
        TestKinopoiskLinkedUids.check_no_cards_bounded(user=passport_user.linked_users[0], cards_=card_id)
        kp_step.process_unlinking_users()
        TestKinopoiskLinkedUids.check_no_cards_bounded(user=passport_user.linked_users[0], cards_=card_id)

    @marks.no_parallel('kinopoisk_plus')
    def test_phonish_card_which_bind_before_link_account_visible_in_kinopoisk(self):
        """
            Кейс №15 из TESTTRUST-20
            Проверяем, что карта, привязанная у Фониша до связи аккаунтов, отображается в Кинопоиске и не отображается
            после отвязки аккаунтов.
        """
        passport_user = uids.get_random_of(uids.with_linked_phonishes_for_kp)
        kp_step = kp_steps.KinopoiskPlus(passport_user)
        card_id = trust.bind_card(passport_user.linked_users[0].token, get_card())['payment_method']
        kp_step.process_linking_users()
        TestKinopoiskLinkedUids.check_all_cards_bounded(user=kp_step.kp_user, cards_=card_id)
        kp_step.process_unlinking_users()
        TestKinopoiskLinkedUids.check_no_cards_bounded(user=kp_step.kp_user, cards_=card_id)

    @marks.no_parallel('kinopoisk_plus')
    def test_phonish_card_invisible_in_kinopoisk_after_unbind_in_phonish(self):
        """
            Кейс №16 из TESTTRUST-20
            Проверяем, что карта, привязанная у Фониша, удалилась из Кинопоиска после удаления карты у Фониша.
        """
        passport_user = uids.get_random_of(uids.with_linked_phonishes_for_kp)
        kp_step = kp_steps.KinopoiskPlus(passport_user)
        card_id = trust.bind_card(passport_user.linked_users[0].token, get_card())['payment_method']
        kp_step.process_linking_users()
        trust.unbind_card(passport_user.linked_users[0].token, card_id[5::])
        TestKinopoiskLinkedUids.check_no_cards_bounded(user=kp_step.kp_user, cards_=card_id)

    @marks.no_parallel('kinopoisk_plus')
    def test_kinopoisk_card_which_bind_after_link_account_visible_in_phonish(self):
        """
            Кейс №17 из TESTTRUST-20
            Проверяем, что карта, привязанная к Кинопоиску после связи аккаунтов, отображается у Фониша.
        """
        passport_user = uids.get_random_of(uids.with_linked_phonishes_for_kp)
        kp_step = kp_steps.KinopoiskPlus(passport_user)
        kp_step.process_linking_users()
        card_id = kp_step.binding_card_for_kp()
        TestKinopoiskLinkedUids.check_no_cards_bounded(user=passport_user.linked_users[0], cards_=card_id)

    @marks.no_parallel('kinopoisk_plus')
    def test_phonish_card_which_bind_after_link_account_visible_in_kinopoisk(self):
        """
            Кейс №18 из TESTTRUST-20
            Проверяем, что карта, привязанная к Фонишу после связи аккаунтов, отображается в Кинопоиске.
        """
        passport_user = uids.get_random_of(uids.with_linked_phonishes_for_kp)
        kp_step = kp_steps.KinopoiskPlus(passport_user)
        kp_step.process_linking_users()
        card_id = trust.bind_card(passport_user.linked_users[0].token, get_card())['payment_method']
        TestKinopoiskLinkedUids.check_all_cards_bounded(user=kp_step.kp_user, cards_=card_id)

    @marks.no_parallel('kinopoisk_plus')
    def test_same_card_in_kinopoisk_and_phonish_is_not_duplicate(self):
        """
            Кейс №19 из TESTTRUST-20
            Проверяем, что одна и та же физическая карта, привязанная к аккаунтам Кинопоиска и Фониша не дублируется
            в list_payment_methods для этих аккаунтов.
        """
        card = get_card()
        passport_user = uids.get_random_of(uids.with_linked_phonishes_for_kp)
        kp_step = kp_steps.KinopoiskPlus(passport_user)
        card_id_kp = kp_step.binding_card_for_kp(card)
        card_id_phonish = trust.bind_card(passport_user.linked_users[0].token, card)['payment_method']
        kp_step.process_linking_users()
        TestKinopoiskLinkedUids.check_one_card_id_for_one_card([card_id_kp, card_id_phonish])
        TestKinopoiskLinkedUids.check_duplicate_cards([kp_step.kp_user, passport_user, passport_user.linked_users[0]])

    @marks.no_parallel('kinopoisk_plus')
    def test_same_card_in_kinopoisk_passport_and_phonish_is_not_duplicate(self):
        """
            Кейс №20 из TESTTRUST-20
            Проверяем, что одна и та же физическая карта, привязанная к аккаунтам Кинопоиска, Паспорта  и Фониша
            не дублируется в list_payment_methods для этих аккаунтов.
        """
        card = get_card()
        passport_user = uids.get_random_of(uids.with_linked_phonishes_for_kp)
        kp_step = kp_steps.KinopoiskPlus(passport_user)
        card_id_kp = kp_step.binding_card_for_kp(card)
        card_id_phonish = trust.bind_card(passport_user.linked_users[0].token, card)['payment_method']
        linked_cards, _ = trust.process_binding(passport_user, card, service=service)
        card_id_p = linked_cards[0]
        kp_step.process_linking_users()
        TestKinopoiskLinkedUids.check_one_card_id_for_one_card([card_id_kp, card_id_phonish, card_id_p])
        TestKinopoiskLinkedUids.check_duplicate_cards([kp_step.kp_user, passport_user, passport_user.linked_users[0]])

    @marks.no_parallel('kinopoisk_plus')
    def test_unbind_duplicate_kinopoisk_and_phonish_card_from_passport(self):
        """
            Кейс №21 из TESTTRUST-20
            Проверяем, что одна и та же физическая карта, привязанная к аккаунтам Кинопоиска и Фониша удаляется из
            обоих аккаунтов, при удалении карты в Паспорте.
        """
        card = get_card()
        passport_user = uids.get_random_of(uids.with_linked_phonishes_for_kp)
        kp_step = kp_steps.KinopoiskPlus(passport_user)
        card_id_kp = kp_step.binding_card_for_kp(card)
        card_id_phonish = trust.bind_card(passport_user.linked_users[0].token, card)['payment_method']
        kp_step.process_linking_users()
        trust.process_unbinding(passport_user, card_id_kp, service)
        TestKinopoiskLinkedUids.check_no_cards_bounded(user=kp_step.kp_user, cards_=card_id_kp)
        TestKinopoiskLinkedUids.check_no_cards_bounded(user=passport_user.linked_users[0], cards_=card_id_phonish)

    @marks.no_parallel('kinopoisk_plus')
    def test_unbind_duplicate_kinopoisk_and_phonish_card_from_phonish(self):
        """
            Кейс №22 из TESTTRUST-20
            Проверяем, что одна и та же физическая карта, привязанная к аккаунтам Кинопоиска и Фониша удаляется из
            обоих аккаунтов, при удалении карты у Фониша.
        """
        card = get_card()
        passport_user = uids.get_random_of(uids.with_linked_phonishes_for_kp)
        kp_step = kp_steps.KinopoiskPlus(passport_user)
        card_id_kp = kp_step.binding_card_for_kp(card)
        card_id_phonish = trust.bind_card(passport_user.linked_users[0].token, card)['payment_method']
        kp_step.process_linking_users()
        trust.process_unbinding(passport_user.linked_users[0], card_id_kp, service)
        TestKinopoiskLinkedUids.check_no_cards_bounded(user=passport_user.linked_users[0], cards_=card_id_phonish)
        TestKinopoiskLinkedUids.check_all_cards_bounded(user=kp_step.kp_user, cards_=card_id_kp)

    @marks.no_parallel('kinopoisk_plus')
    def test_unbind_duplicate_kinopoisk_passport_and_phonish_card_from_passport(self):
        """
            Кейс №23 из TESTTRUST-20
            Проверяем, что одна и та же физическая карта, привязанная к аккаунтам Кинопоиска, Паспорта и Фониша
            удаляется из всехо, при удалении карты в Паспорте.
        """
        card = get_card()
        passport_user = uids.get_random_of(uids.with_linked_phonishes_for_kp)
        kp_step = kp_steps.KinopoiskPlus(passport_user)
        card_id_kp = kp_step.binding_card_for_kp(card)
        card_id_phonish = trust.bind_card(passport_user.linked_users[0].token, card)['payment_method']
        linked_cards, _ = trust.process_binding(passport_user, card, service=service)
        card_id_p = linked_cards[0]
        kp_step.process_linking_users()
        trust.process_unbinding(passport_user, card_id_p, service)
        TestKinopoiskLinkedUids.check_no_cards_bounded(user=passport_user, cards_=card_id_p)
        TestKinopoiskLinkedUids.check_no_cards_bounded(user=kp_step.kp_user, cards_=card_id_kp)
        TestKinopoiskLinkedUids.check_no_cards_bounded(user=passport_user.linked_users[0], cards_=card_id_phonish)

    @marks.no_parallel('kinopoisk_plus')
    @pytest.mark.skipif(True, reason="Wait rework by 'al-kov'")
    def test_unbind_duplicate_kinopoisk_passport_and_phonish_card_from_phonish(self):
        """
            Кейс №24 из TESTTRUST-20
            Проверяем, что одна и та же физическая карта, привязанная к аккаунтам Кинопоиска, Паспорта и Фониша
            удаляется из всех аккаунтов, при удалении карты у Фониша.
        """
        card = get_card()
        passport_user = uids.get_random_of(uids.with_linked_phonishes_for_kp)
        kp_step = kp_steps.KinopoiskPlus(passport_user)
        card_id_kp = kp_step.binding_card_for_kp(card)
        card_id_phonish = trust.bind_card(passport_user.linked_users[0].token, card)['payment_method']
        linked_cards, _ = trust.process_binding(passport_user, card, service=service)
        card_id_p = linked_cards[0]
        kp_step.process_linking_users()
        trust.process_unbinding(passport_user.linked_users[0], card_id_kp, service)
        TestKinopoiskLinkedUids.check_no_cards_bounded(user=passport_user.linked_users[0], cards_=card_id_phonish)
        TestKinopoiskLinkedUids.check_no_cards_bounded(user=passport_user, cards_=card_id_p)
        TestKinopoiskLinkedUids.check_all_cards_bounded(user=kp_step.kp_user, cards_=card_id_kp)


if __name__ == '__main__':
    pytest.main()
