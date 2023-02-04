# coding: utf-8

import re
import time

import pytest
from hamcrest import is_not, none, equal_to

import btestlib.reporter as reporter
from btestlib import environments
from btestlib.constants import Services
from simpleapi.common.oauth import Auth
from simpleapi.common.payment_methods import TrustWebPage, Via
from simpleapi.common.utils import DataObject
from simpleapi.data import defaults
from simpleapi.data import features, stories
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import get_card, get_masked_number, get_card_with_separator
from simpleapi.steps import balance_test_steps as balance_test
from simpleapi.steps import check_steps as check
from simpleapi.steps import simple_steps as simple
from simpleapi.steps import simple_steps_bo as simple_bo
from simpleapi.steps import trust_steps as trust


class Data(object):
    cards = [
        get_card(),
        get_card_with_separator(get_card(), ' '),
        get_card_with_separator(get_card(), '\t'),
    ]
    regions = [
        '225',
        '84'
    ]


@reporter.feature(features.General.CardsOperations)
@reporter.story(stories.CardsOperations.CardsMasking)
class TestMaskingPan(object):
    @staticmethod
    def check_bind_card_masking(card_number, token):
        problem_character = card_number[4]
        if not problem_character.isdigit():
            card_number = ''.join(card_number.split(problem_character))
        masked_card_number = get_masked_number(card_number)
        path = '/var/remote-log/{}/yb/yb-trust-paysys.log'.format(environments.simpleapi_env().trust_log_url)
        time_str = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(time.time() - 2))
        regexp = '.+TrustLogic.bind_card+'

        resp = balance_test.find_in_log(path, time_str, regexp)
        binding_log = ''
        for line in resp['lines']:
            if token in line:
                binding_log = line
                break
        check.check_that(binding_log, is_not(none()),
                         step=u'Проверяем что в логе есть строка с привязкой карты',
                         error=u'Лог не содержит строки с привязкой карты')

        pattern_pan = re.compile(r"card_number_masked': u'(.{14}?)'")
        pan_check = pattern_pan.findall(binding_log)[0]
        assert pan_check == masked_card_number, 'We have {}, but waited {}'.format(pan_check, masked_card_number)

    @staticmethod
    def check_start_payment_masking(card_number, trust_payment_id):
        masked_card_number = get_masked_number(card_number)
        path = '/var/remote-log/{}/yb/yb-trust-paysys.log'.format(environments.simpleapi_env().trust_log_url)
        time_str = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(time.time() - 40))  # it work faster then ever
        regexp = '.+method: start_payment, args:+'

        resp = balance_test.find_in_log(path, time_str, regexp)
        binding_log = ''
        for line in resp['lines']:
            if trust_payment_id in line:
                binding_log = line
                break
        check.check_that(binding_log, is_not(none()),
                         step=u'Проверяем что в логе есть строка с привязкой карты',
                         error=u'Лог не содержит строки с привязкой карты')
        pattern_pan = re.compile(r"card_number_masked': '(.{14}?)'")
        pan_check = pattern_pan.findall(binding_log)[0]
        check.check_that(pan_check, equal_to(masked_card_number),
                         step=u'Проверяем что номер карты замаскировался правильно',
                         error=u'Полученный замаскированный номер карты {}, а ожидалось {}'.format(pan_check,
                                                                                                   masked_card_number))

    @pytest.mark.parametrize('card', Data.cards, ids=DataObject.ids_card)
    @pytest.mark.parametrize('region_id', Data.regions, ids=DataObject.ids_region_id)
    def test_bind_card_masking(self, card, region_id):
        user = uids.get_random_of(uids.mutable)
        token = trust.get_auth_token(Auth.get_auth(user), user)['access_token']
        trust.process_unbinding(user)
        trust.bind_card(token, card, region_id=region_id)
        self.check_bind_card_masking(card['card_number'], token)

    def test_start_payment_masking(self):
        user = uids.get_random_of(uids.all_)
        card = get_card()
        service_product_id = \
            simple.create_service_product_for_service(Services.DISK,
                                                      product_type=defaults.ServiceProduct.Subscription.NORMAL)
        service_order_id = simple_bo.get_service_order_id(Services.DISK)
        paymethod, region_id, currency = (TrustWebPage(Via.card(card)), 225, 'RUB')

        resp = simple_bo.create_and_pay_order(Services.DISK, user, service_product_id=service_product_id,
                                              service_order_id=service_order_id, paymethod=paymethod,
                                              region_id=region_id, currency=currency)
        self.check_start_payment_masking(card['card_number'], resp['trust_payment_id'])
