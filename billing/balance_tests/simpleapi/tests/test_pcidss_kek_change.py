# coding=utf-8

import pytest
from hamcrest import equal_to, is_

import btestlib.reporter as reporter
from btestlib.constants import Services
from btestlib.utils import TestsError
from simpleapi.common.payment_methods import TrustWebPage, Via
from simpleapi.data import cards_pool
from simpleapi.data import defaults
from simpleapi.data import features
from simpleapi.data import uids_pool as uids
from simpleapi.steps import simple_steps as simple
from simpleapi.steps import trust_steps as trust
from simpleapi.steps.check_steps import check_that
from simpleapi.steps.pcidss_steps import KeyApi, ChangeKEK, get_kek_part_3

__author__ = 'slppls'


@pytest.fixture(scope='class')
def check_keyapi_ready():
    state = KeyApi.get_state_from_status(KeyApi.status())
    kek_part3 = get_kek_part_3(KeyApi().get_version_from_status(KeyApi().status()))
    # случай, когда упало где-то до перешифровки деков и применим cleanup
    if state == defaults.PCIDSS.State.generate_kek or state == defaults.PCIDSS.State.confirm_kek_parts:
        KeyApi.cleanup(kek_part3)
        KeyApi().wait_cleanup_done()
    state = KeyApi.get_state_from_status(KeyApi.status())
    # случай, когда упало где-то на перешифровки деков
    if state == defaults.PCIDSS.State.reencrypt_deks:
        # если текущая версия кека и целевая версия равны
        if KeyApi.get_target_version_from_status(KeyApi.status()) == KeyApi.get_version_from_status(KeyApi.status()):
            # значит упало на попытке reset_target_version, а значит надо менять кек со старой компонентой
            KeyApi.switch_kek(kek_part3)
        else:
            # иначе же меняем кек с новой компонентой
            kek_part3_new = KeyApi.get_component(kek_part3)
            KeyApi.switch_kek(kek_part3_new)
        KeyApi().wait_switch_kek_done()
    state = KeyApi.get_state_from_status(KeyApi.status())
    # случай, когда руками уже ничего не сделать и надо обращаться к @sage
    if state != defaults.PCIDSS.State.normal:
        raise TestsError('Cannot run test. Status is not {}.'.format(defaults.PCIDSS.State.normal))


@reporter.feature(features.PCIDSS.KeyApi)
@pytest.mark.usefixtures('check_keyapi_ready')
class TestKeyApi(object):
    def test_basic_cycle(self):
        ChangeKEK.process_kek_change()

    def test_bind_card_with_kek_change(self):
        # 0 bind card
        service = Services.TICKETS
        user = uids.get_random_of(uids.all_)
        card = cards_pool.get_card()
        trust.process_binding(user=user, cards=card)
        # 1 change kek
        ChangeKEK.process_kek_change()
        # 2 try to pay by early linked card
        with reporter.step(u'Совершаем платеж картой, сохраненной до смены КЕКа'):
            paymethod = TrustWebPage(Via.LinkedCard(card=card))
            orders = simple.form_orders_for_create(service, user)

            simple.process_payment(service, user=user, orders=orders,
                                   paymethod=paymethod)

    def test_bind_existing_card_after_kek_change(self):
        # 0 bind card
        service = Services.TAXI
        user = uids.get_random_of(uids.all_)
        card = cards_pool.get_card()
        masked_pan = cards_pool.get_masked_number(card['card_number'])
        trust.process_binding(user=user, cards=card, service=service)
        # 1 change kek
        ChangeKEK.process_kek_change()
        # 2 try to bind linked card one more time
        trust.process_binding(user=user, cards=card, service=service, multiple=1)
        with reporter.step(u'Проверяем что у пользователя привязана только одна карта {}'.format(card)):
            _, paymethods = simple.list_payment_methods(service, user)
            card_count = 0

            for card_id in paymethods:
                if paymethods[card_id]['number'] == masked_pan:
                    card_count += 1
            check_that(card_count, is_(equal_to(1)),
                       step=u'Проверяем, что карта привязана лишь единожды',
                       error=u'Карта привязана несколько раз!')

    def test_cleanup_before_reencrypt(self):
        _, old_kek_parts = ChangeKEK.process_data_prepare()
        ChangeKEK.process_generate_kek(old_kek_parts)
        new_kek_parts = ChangeKEK.process_get_components(old_kek_parts)
        ChangeKEK.process_confirm_components(new_kek_parts)
        ChangeKEK.process_cleanup(old_kek_parts)

    def test_reset_target_version(self):
        start_kek_version, old_kek_parts = ChangeKEK.process_data_prepare()
        ChangeKEK.process_generate_kek(old_kek_parts)
        new_kek_parts = ChangeKEK.process_get_components(old_kek_parts)
        ChangeKEK.process_confirm_components(new_kek_parts)
        ChangeKEK.process_reencrypt_deks(new_kek_parts)
        ChangeKEK.process_reset_target_version(old_kek_parts)
        ChangeKEK.process_switch_kek(old_kek_parts)

        current_kek_version = KeyApi.get_version_from_status(KeyApi.status())
        check_that(start_kek_version, is_(equal_to(current_kek_version)),
                   step=u'Проверяем, что версия КЕКа откачена до первоначальной',
                   error=u'Текущая версия КЕКа не эквивалента первоначальной')
