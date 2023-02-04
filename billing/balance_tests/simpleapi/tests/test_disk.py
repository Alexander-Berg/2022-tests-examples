# coding: utf-8

import pytest
from hamcrest import is_not, is_, none

import btestlib.reporter as reporter
from btestlib.constants import Services
from simpleapi.common import logger
from simpleapi.common.oauth import Auth
from simpleapi.common.payment_methods import TrustWebPage, Via, LinkedCard, YandexMoney, YandexMoneyWebPage
from simpleapi.common.utils import DataObject, current_scheme_is
from simpleapi.data import defaults
from simpleapi.data import features, stories
from simpleapi.data import marks
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import get_masked_number, get_card
from simpleapi.steps import check_steps as check
from simpleapi.steps import simple_steps as simple
from simpleapi.steps import simple_steps_bo as simple_bo
from simpleapi.steps import trust_steps as trust

__author__ = 'fellow'

log = logger.get_logger()

service = Services.DISK


class Data(object):
    test_data = [
        pytest.mark.skipif(True, reason="No mobile payments for Disk")
        ((LinkedCard(card=get_card()), 225, 'RUB', uids.Types.random_from_all)),
        pytest.mark.skipif(True, reason="No mobile payments for Disk")
        ((LinkedCard(card=get_card()), 84, 'USD', uids.Types.random_from_all)),
        marks.web_in_browser(
            (TrustWebPage(Via.card(get_card()), in_browser=True), 225, 'RUB', uids.Types.random_from_all)),
        marks.web_in_browser(
            (TrustWebPage(Via.card(get_card()), in_browser=True), 84, 'USD', uids.Types.random_from_all)),
        marks.web_in_browser(
            (TrustWebPage(Via.card(get_card()), in_browser=True), 983, 'USD', uids.Types.random_from_all)),
        marks.web_in_browser(
            (TrustWebPage(Via.LinkedCard(get_card()), in_browser=True), 225, 'RUB', uids.Types.random_from_all)),
        marks.web_in_browser(
            (TrustWebPage(Via.LinkedCard(get_card()), in_browser=True), 84, 'USD', uids.Types.random_from_all)),
        marks.web_in_browser(
            (TrustWebPage(Via.LinkedCard(get_card()), in_browser=True), 983, 'USD', uids.Types.random_from_all)),
        pytest.mark.yamoney((YandexMoney(), 225, 'RUB', uids.Type(pool=uids.secret, name='test_wo_proxy_old'))),
        pytest.mark.yamoney((YandexMoneyWebPage(), 225, 'RUB', uids.Type(pool=uids.secret, name='test_wo_proxy_old'))),
    ]

    test_data_no_save_card = [
        marks.ym_h2h_processing(DataObject(paymethod=TrustWebPage(Via.card(get_card()), in_browser=True), region_id=225,
                                           user_type=uids.Types.random_from_all)),
    ]


@reporter.feature(features.Service.Disk)
@pytest.mark.skipif(not current_scheme_is('BO'), reason="Only BO scheme")
class TestDisk(object):
    @pytest.fixture()
    def service_product_and_service_order_ids(self):
        service_product_id = simple.create_service_product_for_service(service)
        service_order_id = simple_bo.get_service_order_id(service)

        return service_product_id, service_order_id

    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('test_data', Data.test_data,
                             ids=lambda x: 'paymethod={}, region_id={}, currency={}'.format(x[0].title, x[1], x[2]))
    def test_base_payment_cycle(self, service_product_and_service_order_ids,
                                test_data):
        service_product_id, service_order_id = service_product_and_service_order_ids
        paymethod, region_id, currency, user_type = test_data
        user = uids.get_random_of_type(user_type)

        paymethod.init(service=service, user=user, region_id=region_id)

        simple_bo.create_order(service, service_order_id,
                               service_product_id, region_id=region_id, user=user)
        token = trust.get_auth_token(Auth.get_auth(user), user)['access_token']
        pay_order_resp = simple_bo.pay_order(service, service_order_id, paymethod,
                                             token=token, currency=currency)
        trust.pay_by(paymethod, service, user=user,
                     payment_form=pay_order_resp.get('payment_form'),
                     purchase_token=pay_order_resp.get('purchase_token'),
                     region_id=region_id)
        simple_bo.wait_until_payment_done(service, service_order_id, user=user)

        simple_bo.refund_order(service, service_order_id=service_order_id, user=user)

    @pytest.fixture()
    def subscription_service_product_and_service_order_ids(self):
        with reporter.step(u'Создаем подписочный продукт без триального периода'):
            service_product_id = \
                simple.create_service_product_for_service(service,
                                                          product_type=defaults.ServiceProduct.Subscription.NORMAL)
            service_order_id = simple_bo.get_service_order_id(service)

            return service_product_id, service_order_id

    @reporter.story(stories.General.Subscription)
    def test_link_card_after_subscribe(self, subscription_service_product_and_service_order_ids,
                                       service_product_and_service_order_ids):
        user = uids.get_random_of(uids.all_)
        with reporter.step(u'Оплачиваем подписку и проверяем что карта оплаты привызалась к пользователю'):
            card = get_card()
            service_product_id, service_order_id = subscription_service_product_and_service_order_ids
            paymethod, region_id, currency = (TrustWebPage(Via.card(card)), 225, 'RUB')

            simple_bo.create_and_pay_order(service, user, service_product_id=service_product_id,
                                           service_order_id=service_order_id, paymethod=paymethod,
                                           region_id=region_id, currency=currency)

            linked_card = simple.find_card_by_masked_number(service, user=user,
                                                            number=get_masked_number(card['card_number']))

            check.check_that(linked_card, is_not(None),
                             step=u'Проверяем что у пользователя появилась привязаннвя карта',
                             error=u'У пользователя не появилось привязанной карты')

        with reporter.step(u'Оплачиваем привязанной картой новый заказ'):
            service_product_id, service_order_id = service_product_and_service_order_ids
            paymethod, region_id, currency = (LinkedCard(card=card), 225, 'RUB')

            simple_bo.create_and_pay_order(service, user, service_product_id=service_product_id,
                                           service_order_id=service_order_id, paymethod=paymethod,
                                           region_id=region_id, currency=currency)

    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('test_data', Data.test_data_no_save_card, ids=DataObject.ids)
    def test_pay_and_doesnt_save_card(self, test_data, service_product_and_service_order_ids):
        with reporter.step(u'Оплачиваем корзину и НЕ сохраняем карту при этом'):
            paymethod, user_type, region_id = test_data.paymethod, test_data.user_type, test_data.region_id
            service_product_id, service_order_id = service_product_and_service_order_ids

            user = uids.get_random_of_type(user_type)
            simple_bo.create_order(service, service_order_id,
                                   service_product_id, region_id=225, user=user)
            token = trust.get_auth_token(Auth.get_auth(user), user)['access_token']
            pay_order_resp = simple_bo.pay_order(service, service_order_id, paymethod,
                                                 token=token)
            trust.pay_by(paymethod, service, user=user,
                         payment_form=pay_order_resp.get('payment_form'),
                         purchase_token=pay_order_resp.get('purchase_token'))
            simple_bo.wait_until_payment_done(service, service_order_id, user=user)

            simple_bo.refund_order(service, service_order_id=service_order_id, user=user)

        check.check_that(simple.find_card_by_masked_number(service=service, user=user,
                                                           number=get_masked_number(
                                                               paymethod.via.card['card_number'])),
                         is_(none()),
                         step=u'Проверяем что после оплаты карта не привязалась к пользователю',
                         error=u'После оплаты со снятым чекбоксом привязки карты '
                               u'карта все равно привязалась к пользователю')


if __name__ == '__main__':
    pytest.main()
