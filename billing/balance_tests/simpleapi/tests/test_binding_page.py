# coding=utf-8

import datetime
import time

import pytest
from hamcrest import (not_, is_in)

import btestlib.reporter as reporter
from btestlib.constants import Services
from btestlib.utils import CheckMode, check_mode
from simpleapi.common import logger
from simpleapi.common.utils import DataObject
from simpleapi.common.utils import current_scheme_is
from simpleapi.data import defaults
from simpleapi.data import features, stories
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import get_card
from simpleapi.matchers.deep_equals import deep_equals_to
from simpleapi.steps import check_steps as check
from simpleapi.steps import expected_steps as expected
from simpleapi.steps import mongo_steps as mongo
from simpleapi.steps import passport_steps as passport
from simpleapi.steps import simple_steps as simple
from simpleapi.steps import web_steps as web

__author__ = 'fellow'

log = logger.get_logger()

# Вот такая кривота.
# Суть в том, что функционлаьность одни и та же, но для Директа она работает в схеме BO, а для Паспорта - в BS
# Выхода два: либо дублировать код, либо делать что-то подобное тому что ниже
if current_scheme_is('BO'):
    service = Services.DIRECT
    feature = features.Service.Direct
    web_step = web.Direct
else:
    service = Services.PASSPORT
    feature = features.Service.Passport
    web_step = web.Passport


@reporter.feature(feature)
class TestBindingPage(object):
    def undind_all_cards(self, user):
        passport.auth_via_page(user=user)
        session_id = passport.get_current_session_id()
        _, paymethods = simple.list_payment_methods(service, user)
        for paymethod_id, paymethod_info in paymethods.iteritems():
            if paymethod_info['type'] == 'card':
                simple.unbind_card(service, session_id,
                                   defaults.user_ip,
                                   paymethod_id)

    @pytest.fixture
    def binding(self, request):
        user = uids.get_random_of(uids.all_)
        self.undind_all_cards(user)
        card = get_card()
        card_id = simple.process_binding(service, user, card, web_step)

        def fin():
            """
            Отвязываем все карты клиента в конце теста
            """
            self.undind_all_cards(user)

        request.addfinalizer(fin)

        return user, card, card_id

    @reporter.story(stories.General.BindingCard)
    @pytest.mark.parametrize('card_length', [16, 17, 18, 19], ids=DataObject.ids_card_length)
    def test_binding_unbinding_logged_in(self, card_length):
        """
        Для проведения привязки через pcidss нужна либо кука session_id (залогинены под кем-то)
        Либо yandexuid (пришли из яндекса)
        """
        user = uids.get_random_of(uids.all_)
        start_dt = datetime.datetime.now() - datetime.timedelta(minutes=1)
        with check_mode(CheckMode.FAILED):
            purchase_token = simple.create_binding(service, user)['purchase_token']
            resp = simple.do_binding(service, purchase_token)
            check.check_that(resp, deep_equals_to(expected.Binding.in_progress()),
                             step=u'Проверяем корректность статуса привязки в состоянии in_progress')

            resp = simple.check_binding(service, purchase_token)
            check.check_that(resp, deep_equals_to(expected.Binding.initialized(start_dt)),
                             step=u'Проверяем корректность статуса привязки в состоянии initialized')

            passport.auth_via_page(user=user)
            web_step.bind_card(get_card(length=card_length), purchase_token)
            simple.wait_until_binding_done(service, purchase_token)

            resp_binding_done = simple.check_binding(service, purchase_token)
            check.check_that(resp_binding_done, deep_equals_to(expected.Binding.done(start_dt)),
                             step=u'Проверяем корректность статуса привязки в состоянии done')
            mongo.Refund.wait_until_done(resp['trust_payment_id'])

            card_id = resp_binding_done['payment_method_id']
            _, paymethods = simple.list_payment_methods(service, user)
            check.check_that(card_id, is_in(paymethods.keys()),
                             step=u'Проверяем что карта отображается в списке доступных способов оплаты')

            session_id = passport.get_current_session_id()
            simple.unbind_card(service, session_id, defaults.user_ip, card_id)

            _, paymethods = simple.list_payment_methods(service, user)
            check.check_that(card_id, not_(is_in(paymethods.keys())),
                             step=u'Проверяем что карта не отображается в списке доступных способов оплаты')

    @reporter.story(stories.General.BindingCard)
    def test_binding_unbinding_not_logged_in(self):
        """
        Для проведения привязки через pcidss нужна либо кука session_id (залогинены под кем-то)
        Либо yandexuid (пришли из яндекса)
        """
        user = uids.get_random_of(uids.all_)
        start_dt = datetime.datetime.now() - datetime.timedelta(minutes=1)

        with check_mode(CheckMode.FAILED):
            purchase_token = simple.create_binding(service, user)['purchase_token']
            resp = simple.do_binding(service, purchase_token)
            check.check_that(resp, deep_equals_to(expected.Binding.in_progress()),
                             step=u'Проверяем корректность статуса привязки в состоянии in_progress')

            resp = simple.check_binding(service, purchase_token)
            check.check_that(resp, deep_equals_to(expected.Binding.initialized(start_dt)),
                             step=u'Проверяем корректность статуса привязки в состоянии initialized')

            web_step.bind_card(get_card(), purchase_token, from_yandex=True)
            simple.wait_until_binding_done(service, purchase_token)

            resp_binding_done = simple.check_binding(service, purchase_token)
            check.check_that(resp_binding_done, deep_equals_to(expected.Binding.done(start_dt)),
                             step=u'Проверяем корректность статуса привязки в состоянии done')
            mongo.Refund.wait_until_done(resp['trust_payment_id'])

            card_id = resp_binding_done['payment_method_id']
            _, paymethods = simple.list_payment_methods(service, user)
            check.check_that(card_id, is_in(paymethods.keys()),
                             step=u'Проверяем что карта отображается в списке доступных способов оплаты')

            passport.auth_via_page(user=user)
            session_id = passport.get_current_session_id()
            simple.unbind_card(service, session_id, defaults.user_ip, card_id)

    @reporter.story(stories.General.BindingCard)
    def test_unbind_not_linked_card(self):
        """
        При попытке отвязать несуществующую карту не должно происходить ошибки
        """
        user = uids.get_random_of(uids.all_)
        passport.auth_via_page(user=user)
        session_id = passport.get_current_session_id()
        with check_mode(CheckMode.FAILED):
            simple.unbind_card(service, session_id, defaults.user_ip, 'card-x1111')

    @reporter.story(stories.General.BindingCard)
    def test_binding_existed(self, binding):
        """
        Если пытаемся привязать уже существующую карту,
        не должно быть ошибки
        """
        user, card, card_id = binding

        with check_mode(CheckMode.FAILED):
            purchase_token = simple.create_binding(service, user)['purchase_token']
            simple.do_binding(service, purchase_token)

            web_step.bind_card(card, purchase_token)
            simple.wait_until_binding_done(service, purchase_token)
            simple.check_binding(service, purchase_token)

    @reporter.story(stories.General.BindingCard)
    def test_binding_second_card(self, binding):
        """
        Можно привязать более одной карты
        """
        user, card_1, card_id_1 = binding
        with check_mode(CheckMode.FAILED):
            purchase_token = simple.create_binding(service, user)['purchase_token']
            simple.do_binding(service, purchase_token)

            web_step.bind_card(get_card(), purchase_token)
            simple.wait_until_binding_done(service, purchase_token)
            card_id_2 = simple.check_binding(service, purchase_token)['payment_method_id']

            _, paymethods = simple.list_payment_methods(service, user)
            check.check_that(card_id_1, is_in(paymethods.keys()))
            check.check_that(card_id_2, is_in(paymethods.keys()))

    @reporter.story(stories.General.BindingCard)
    @pytest.mark.skipif(True, reason='Too long timeout')
    def test_binding_timed_out(self):
        """
        Если после do_binding не осуществить приязку через веб,
        привязка таймаутится
        """
        user = uids.get_random_of(uids.all_)
        with check_mode(CheckMode.FAILED):
            purchase_token = simple.create_binding(service, user)['purchase_token']
            simple.do_binding(service, purchase_token)

        time.sleep(2 * 60)

        with check_mode(CheckMode.IGNORED):
            resp_timed_out = simple.check_binding(service, purchase_token)
            expected_resp_timed_out = expected.Binding.timed_out()
            check.check_that(resp_timed_out, deep_equals_to(expected_resp_timed_out))

    @reporter.story(stories.General.BindingCard)
    def test_max_5_active_bindings(self):
        """
        Чтобы нас не спамили, стоит ограничение в максимум 5 активных привязок (TRUST-1378)
        """
        user = uids.get_random_of(uids.all_)
        self.undind_all_cards(user)

        for _ in range(5):
            purchase_token = simple.create_binding(service, user)['purchase_token']
            simple.do_binding(service, purchase_token)

        with check_mode(CheckMode.IGNORED):
            resp = simple.create_binding(service, user)

        check.check_that(resp, deep_equals_to(expected.Binding.too_many_active_bindings(user=user)))


if __name__ == '__main__':
    pytest.main()
