# coding=utf-8

import json
import re
import time

import pytest
from hamcrest import empty, is_not, has_entries, contains_string, not_

import btestlib.reporter as reporter
from btestlib import environments
from btestlib.constants import Services
from btestlib.utils import CheckMode, check_mode
from simpleapi.common.payment_methods import LinkedCard
from simpleapi.common.utils import DataObject
from simpleapi.data import defaults
from simpleapi.data import features, stories
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import get_card, CVN
from simpleapi.steps import balance_test_steps as balance_test
from simpleapi.steps import check_steps as check
from simpleapi.steps import db_steps as db
from simpleapi.steps import expected_steps as expected
from simpleapi.steps import simple_steps as simple

__author__ = 'fellow'

service = Services.TAXI

"""
Тесты на взаимодействие с антифрод-системой
https://st.yandex-team.ru/PAYFRAUD-22
В качестве АФС выступает https://test.cybertonica.com (yandex/test-team/Testteam11)
"""


class Data(object):
    payment_data_afs = [
        DataObject(paymethod=LinkedCard(card=get_card()),
                   user_type=uids.Types.random_afs)
            .new(country_data=defaults.CountryData.Russia),
    ]
    payment_data_no_afs = [
        DataObject(paymethod=LinkedCard(card=get_card()),
                   user_type=uids.Types.random_from_test_passport)
            .new(country_data=defaults.CountryData.Russia),
    ]

    whitelist_data = [
        DataObject(
            descr='Service has whitelist terminal, afs rote to whitelist',
            service=Services.TAXI,
            paymethod=LinkedCard(card=get_card()),
            user_type=uids.Types.random_afs,
            orders_structure=defaults.Order.gen_single_order_structure(defaults.Status.afs_whitelist))
            .new(country_data=defaults.CountryData.Russia,
                 should_whitelist=True),
        DataObject(
            descr="Service hasn't whitelist terminal, afs rote to whitelist",
            service=Services.REALTYPAY,
            paymethod=LinkedCard(card=get_card()),
            user_type=uids.Types.random_afs,
            orders_structure=defaults.Order.gen_single_order_structure(defaults.Status.afs_whitelist))
            .new(country_data=defaults.CountryData.Russia,
                 should_whitelist=False),
        DataObject(
            descr='Service has whitelist terminal, afs rote to non-whitelist',
            service=Services.TAXI,
            paymethod=LinkedCard(card=get_card()),
            user_type=uids.Types.random_afs,
            orders_structure=defaults.Order.gen_single_order_structure(defaults.Status.base_success))
            .new(country_data=defaults.CountryData.Russia,
                 should_whitelist=False),

    ]

    orders_data_afs_deny = [
        DataObject(orders_structure=defaults.Order.gen_single_order_structure(defaults.Status.afs_deny)),
    ]
    orders_data_afs_allow = [
        DataObject(orders_structure=defaults.Order.gen_single_order_structure(defaults.Status.afs_challenge)),
        DataObject(orders_structure=defaults.Order.gen_single_order_structure(defaults.Status.base_success)),
    ]

    error_data = [
        DataObject().new(cvn=CVN.not_enough_funds_RC51, expected_error={"status": "FAILED",
                                                                        "comment": "RC=51, reason=Not enough funds",
                                                                        "code": "not_enough_funds"}),
        DataObject().new(cvn=CVN.do_not_honor_RC05, expected_error={"status": "FAILED",
                                                                    "comment": "RC=05, reason=Do not honor",
                                                                    "code": "authorization_reject"}),
    ]


@reporter.feature(features.General.AFS)
class TestAFS(object):
    @staticmethod
    def check_afs_params(trust_payment_id, expected_error):
        with reporter.step(u'Проверяем, были ли переданы параметры ошибки в АФС '
                           u'по платежу {}'.format(trust_payment_id)):
            path = '/var/remote-log/{}/yb/yb-trust-queue.log'.format(environments.simpleapi_env().trust_log_url)
            time_str = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(time.time() - 10))
            regexp = '.+sending body:.+\"extid\": \"{}\".+'.format(trust_payment_id)
            resp = balance_test.find_in_log(path, time_str, regexp)

            check.check_that(resp.get('lines'), is_not(empty()),
                             error=u'В результате поиска по логу ничего не найдено')

            params_str = resp.get('lines')[-1]
            params = dict()
            m = re.search('{.+}', params_str)
            if m:
                params = json.loads(m.group(0))

            check.check_that(params, has_entries(expected_error))

    @staticmethod
    def check_afs_stat(trust_payment_id, expected_stat):
        with reporter.step(u'Проверяем, была ли сохранена статистика АФС у нас в БД '
                           u'по платежу {}'.format(trust_payment_id)):
            actual_stat = db.bs().get_afs_stat(trust_payment_id)
            check.check_that(actual_stat, has_entries(expected_stat))

    @reporter.story(stories.AFS.General)
    @pytest.mark.parametrize('payment_data', Data.payment_data_afs, ids=DataObject.ids)
    @pytest.mark.parametrize('orders_data', Data.orders_data_afs_deny, ids=DataObject.ids)
    def test_base_antifraud_rules(self, payment_data, orders_data):
        """
        Пока что правил как таковых нет, просто получаем deny если сумма платежа **.19
        """
        paymethod, country, user_type = payment_data.paymethod, \
                                        payment_data.country_data, \
                                        payment_data.user_type

        user = uids.get_random_of_type(user_type)
        user_ip, currency, region_id = country['user_ip'], country['currency'], country['region_id']
        paymethod.init(service, user, region_id=region_id, user_ip=user_ip)

        with check_mode(CheckMode.IGNORED):
            resp = simple.process_payment(service, user, orders_structure=orders_data.orders_structure,
                                          paymethod=paymethod, should_failed=True, init_paymethod=False,
                                          region_id=region_id, user_ip=user_ip,
                                          currency=currency, pass_cvn=True)
        check.check_that(resp, has_entries(expected.BasketError.afs_blacklisted()),
                         step=u'Проверяем корректность статуса корзины после deny от АФС',
                         error=u'Некорректный статус корзины после deny от АФС')

    @reporter.story(stories.AFS.General)
    @pytest.mark.parametrize('payment_data', Data.payment_data_afs, ids=DataObject.ids)
    @pytest.mark.parametrize('orders_data', Data.orders_data_afs_allow, ids=DataObject.ids)
    def test_afs_allow(self, payment_data, orders_data):
        """
        Помимо deny от АФС могут прийти статусы allow (все хорошо) и
        challenge (афс затрудняется проверить платеж на фрод)
        В обоих случаях платеж у нас должен проходить
        """
        paymethod, country, user_type = payment_data.paymethod, \
                                        payment_data.country_data, \
                                        payment_data.user_type

        user = uids.get_random_of_type(user_type)
        user_ip, currency, region_id = country['user_ip'], country['currency'], country['region_id']

        with check_mode(CheckMode.FAILED):
            simple.process_payment(service, user, orders_structure=orders_data.orders_structure,
                                   paymethod=paymethod, region_id=region_id, user_ip=user_ip,
                                   currency=currency, pass_cvn=True)

    @reporter.story(stories.AFS.General)
    @pytest.mark.parametrize('payment_data', Data.payment_data_no_afs, ids=DataObject.ids)
    @pytest.mark.parametrize('orders_data', Data.orders_data_afs_deny, ids=DataObject.ids)
    def test_no_afs_terminal(self, payment_data, orders_data):
        """
        Если нет терминала с afs_check_method=sync, то в АФС не идем
        В тесте реализовано с помощью роутинга:
        идем с пользователем, которго НЕ роутит в АФС-терминал, и проверяем
        что сумма платежа **.19 не порождает ошибку blacklisted
        """
        paymethod, country, user_type = payment_data.paymethod, \
                                        payment_data.country_data, \
                                        payment_data.user_type

        user = uids.get_random_of_type(user_type)
        user_ip, currency, region_id = country['user_ip'], country['currency'], country['region_id']

        with check_mode(CheckMode.FAILED):
            simple.process_payment(service, user, orders_structure=orders_data.orders_structure,
                                   paymethod=paymethod, region_id=region_id, user_ip=user_ip,
                                   currency=currency, pass_cvn=True)

    @reporter.story(stories.AFS.SendingErrors)
    @pytest.mark.parametrize('payment_data', Data.payment_data_afs, ids=DataObject.ids)
    @pytest.mark.parametrize('error_data', Data.error_data, ids=DataObject.ids)
    def test_send_payment_status_to_afs(self, payment_data, error_data):
        paymethod, country, user_type = payment_data.paymethod, \
                                        payment_data.country_data, \
                                        payment_data.user_type
        cvn, expected_error = error_data.cvn, error_data.expected_error

        user = uids.get_random_of_type(user_type)
        user_ip, currency, region_id = country['user_ip'], country['currency'], country['region_id']
        paymethod.init(service, user, region_id=region_id, user_ip=user_ip,
                       bind_with_success_cvn=True)

        paymethod.card.update({'cvn': cvn})

        with check_mode(CheckMode.IGNORED):
            resp = simple.process_payment(service, user, paymethod=paymethod,
                                          should_failed=True, init_paymethod=False,
                                          region_id=region_id, user_ip=user_ip,
                                          currency=currency, pass_cvn=True)

        self.check_afs_params(trust_payment_id=resp['trust_payment_id'],
                              expected_error=expected_error)

    @staticmethod
    def check_is_terminal_whitelist_or_not(trust_payment_id, should_be_whitelist):
        terminal = db.bs().get_payment_terminal(trust_payment_id)
        if should_be_whitelist:
            check.check_that(terminal['description'], contains_string('whitelist'),
                             step=u'Проверяем что платеж пошел через whitelist-терминал',
                             error=u'Платеж пошел не через whitelist-терминал')
        else:
            check.check_that(terminal['description'], not_(contains_string('whitelist')),
                             step=u'Проверяем что платеж пошел НЕ через whitelist-терминал',
                             error=u'Платеж пошел через whitelist-терминал, а должен был пройти через обычный')

    @reporter.story(stories.AFS.Whitelist)
    @pytest.mark.parametrize('data', Data.whitelist_data, ids=DataObject.ids)
    def test_whitelist_terminal(self, data):
        """
        Тест на логику работы с whitelist-терминалами:
        При проведении платежа если есть терминал с afs_check_method='sync', то идем в АФС с этим терминалом.
        Если АФС вернул специальный тег (wl_amount), то платеж должен перенаправляться в whitelist-терминал
        если он есть у сервиса.
        Если такого терминала нет, то платеж должен проходить через ранее выбранный терминал
        """
        service, paymethod, country, user_type, \
        orders_structure, should_whitelist = data.service, \
                                             data.paymethod, \
                                             data.country_data, \
                                             data.user_type, \
                                             data.orders_structure, \
                                             data.should_whitelist

        user = uids.get_random_of_type(user_type)
        user_ip, currency, region_id = country['user_ip'], country['currency'], country['region_id']

        with check_mode(CheckMode.FAILED):
            resp = simple.process_payment(service, user, orders_structure=orders_structure,
                                          paymethod=paymethod, region_id=region_id, user_ip=user_ip,
                                          currency=currency, pass_cvn=True)

        self.check_is_terminal_whitelist_or_not(resp['trust_payment_id'], should_whitelist)


if __name__ == '__main__':
    pytest.main()
