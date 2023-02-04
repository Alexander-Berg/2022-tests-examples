# coding: utf-8

import pytest
from hamcrest import has_entries

import btestlib.reporter as reporter
from btestlib import environments
from btestlib.constants import Services
from btestlib.utils import CheckMode, check_mode
from simpleapi.common import logger
from simpleapi.common.oauth import Auth
from simpleapi.common.payment_methods import Phone, YandexMoney, LinkedCard, BonusAccount
from simpleapi.common.utils import current_scheme_is
from simpleapi.data import defaults
from simpleapi.data import features, stories
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import get_card
from simpleapi.steps import check_steps as check
from simpleapi.steps import mongo_steps as mongo
from simpleapi.steps import simple_steps as simple
from simpleapi.steps import simple_steps_bo as simple_bo
from simpleapi.steps import trust_steps as trust

__author__ = 'fellow'

log = logger.get_logger()

service = Services.STORE


class Data(object):
    test_data = [
        (LinkedCard(card=get_card()), 225, 'RUB', uids.Types.random_from_all),
        (LinkedCard(card=get_card()), 84, 'USD', uids.Types.random_from_all),
        # (TrustWebPage(Via.card(REAL_CARD)), 225, 'RUB'), # Стор отказался от оплат через страницу
        pytest.mark.yamoney((YandexMoney(), 225, 'RUB', uids.secret['test_wo_proxy_old'])),
        # (Phone(), 225, 'RUB', uids.Types.random_with_phone),
    ]

    test_data_bonus = [
        (LinkedCard(card=get_card()), 225, 'RUB', uids.Types.random_from_all),
        # (Card(card=get_card()), 84, 'USD', uids.get_random_of(uids.mimino)),
        pytest.mark.yamoney((YandexMoney(), 225, 'RUB', uids.secret['test_wo_proxy_old'])),
        # (Phone(), 225, 'RUB', uids.Types.random_with_phone),
    ]

    test_data_blocking_reason = [
        (LinkedCard(card=get_card()), 225, 'RUB', uids.Types.random_from_all),
        (LinkedCard(card=get_card()), 84, 'USD', uids.Types.random_from_all),
    ]


@reporter.feature(features.Service.Store)
@pytest.mark.skipif(not current_scheme_is('BO'), reason="Only BO scheme")
class TestStore(object):
    @pytest.fixture()
    def service_product_and_service_order_ids(self):
        service_product_id = simple.create_service_product_for_service(service)
        service_order_id = simple_bo.get_service_order_id(service)

        return service_product_id, service_order_id

    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('test_data', Data.test_data,
                             ids=lambda x: 'paymethod={}, region_id={}, currency={}'.format(x[0].title, x[1], x[2]))
    def test_base_payment_cycle(self, service_product_and_service_order_ids, test_data):
        service_product_id, service_order_id = service_product_and_service_order_ids
        paymethod, region_id, currency, user_type = test_data
        user = uids.get_random_of_type(user_type)

        paymethod.init(service=service, user=user)

        simple_bo.create_order(service, service_order_id,
                               service_product_id, region_id=region_id, user=user)
        token = trust.get_auth_token(Auth.get_auth(user), user)['access_token']
        pay_order_resp = simple_bo.pay_order(service, service_order_id, paymethod,
                                             token=token, currency=currency)
        trust.pay_by(paymethod, service, user=user,
                     payment_form=pay_order_resp.get('payment_form'),
                     purchase_token=pay_order_resp.get('purchase_token'))
        simple_bo.wait_until_payment_done(service, service_order_id, user=user)

        simple_bo.refund_order(service, service_order_id=service_order_id, user=user)

    @pytest.fixture()
    def service_product_with_bonus(self):
        with reporter.step(u'Создаем сервисный продукт с бонусом'):
            prices = defaults.product_prices
            prices.append({'price': 100, 'currency': 'YSTBN', 'dt': 1327521693, 'region_id': 225})
            product_bonus = {'bonuses': [{'region_id': 225, 'dt': 1327521693, 'bonus': 200, 'currency': 'YSTBN'}, ],
                             'prices': prices}
            service_product_id = simple.create_service_product_for_service(service, product_type=product_bonus)
            service_order_id = simple_bo.get_service_order_id(service)

            return service_product_id, service_order_id

    @pytest.mark.skipif('BS' in environments.simpleapi_env().name, reason="Bonuses doesn't work in bs scheme")
    @pytest.mark.parametrize('test_data', Data.test_data_bonus,
                             ids=lambda x: 'paymethod={}, region_id={}, currency={}'.format(x[0].title, x[1], x[2]))
    @reporter.story(stories.General.BonusPayment)
    def test_bonus_provides(self, service_product_with_bonus, test_data):
        service_product_id, service_order_id = service_product_with_bonus
        paymethod, region_id, currency, user_type = test_data
        user = uids.get_random_of_type(user_type)
        with reporter.step(u'Оплачиваем заказ и проверяем что после оплаты зачислились бонусы'):
            simple_bo.create_and_pay_order(service, user, service_product_id=service_product_id,
                                           service_order_id=service_order_id, paymethod=paymethod,
                                           region_id=region_id, currency=currency)

            simple_bo.wait_until_bonus_provides(service, service_order_id, user=user)

        with reporter.step(u'Создаем новый заказ и оплачиваем его бонусом'):
            service_product_id_bonus = simple.create_service_product_for_service(service)
            service_order_id_bonus = simple_bo.get_service_order_id(service)

            simple_bo.create_and_pay_order(service, user, service_product_id=service_product_id_bonus,
                                           service_order_id=service_order_id_bonus, paymethod=BonusAccount(),
                                           region_id=region_id, currency=currency)

            # todo: добавить кейсы для бонусов
            #   1) Бонус доступен только для Стора
            #   2) Не хватает средств для оплаты на бонусном счете
            #   3) https://st.yandex-team.ru/TRUST-567
            #   4) оплаты в долларах?

    @pytest.fixture()
    def make_card_blocked(self, request):
        def blocked_card(user, paymethod, region_id, currency, reason):
            with reporter.step(u'Оплачиваем заказ и блокируем карту, '
                               u'которой проводилась оплата: blocking_reason={}'.format(reason)):
                service_product_id = simple.create_service_product_for_service(service)
                service_order_id = simple_bo.get_service_order_id(service)
                resp = simple_bo.process_payment_order(service=service, user=user, paymethod=paymethod,
                                                       service_order_id=service_order_id,
                                                       service_product_id=service_product_id,
                                                       region_id=region_id, currency=currency)

                mongo.Card.update_data(resp['trust_payment_id'],
                                       data_to_update={'blocking_reason': reason})

            def fin():
                with reporter.step(u'Убираем у карты blocking_reason в конце теста'):
                    mongo.Card.update_data(resp['trust_payment_id'],
                                           data_to_update={'blocking_reason': None},
                                           wait=False)

            request.addfinalizer(fin)

        return blocked_card

    @reporter.story(stories.General.Rules)
    @pytest.mark.parametrize('test_data', Data.test_data_blocking_reason,
                             ids=lambda x: 'paymethod={}, region_id={}, currency={}'.format(x[0].title, x[1], x[2]))
    def test_blocking_reason(self, test_data, service_product_and_service_order_ids, make_card_blocked):
        service_product_id, service_order_id = service_product_and_service_order_ids
        paymethod, region_id, currency, user_type = test_data
        user = uids.get_random_of_type(user_type)
        reason = 'Some test reason'

        make_card_blocked(user=user, paymethod=paymethod,
                          region_id=region_id, currency=currency, reason=reason)

        with check_mode(CheckMode.IGNORED):
            resp = simple_bo.process_payment_order(service=service, user=user, paymethod=paymethod,
                                                   service_order_id=service_order_id,
                                                   service_product_id=service_product_id,
                                                   region_id=region_id, currency=currency,
                                                   should_failed=True)

        from simpleapi.steps import expected_steps as expected
        check.check_that(resp, has_entries(expected.BasketError.card_is_blocked(reason)),
                         step=u'Проверяем что оплата с заблокированной картой не прошла',
                         error=u'Прошла оплата с заблокированной картой или вернулась некорректная ошибка')


if __name__ == '__main__':
    pytest.main()
