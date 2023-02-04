# coding: utf-8

import pytest
from hamcrest import is_not

import btestlib.reporter as reporter
from btestlib.constants import Services
from btestlib.utils import CheckMode, check_mode
from simpleapi.common.payment_methods import LinkedCard, TrustWebPage, Via
from simpleapi.common.utils import DataObject
from simpleapi.data import defaults
from simpleapi.data import features, stories
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import get_card, get_masked_number
from simpleapi.steps import check_steps as check
from simpleapi.steps import payments_api_steps as payments_api, simple_steps

__author__ = 'sunshineguy'

service = Services.AFISHA_MOVIEPASS


class Data(object):
    test_data = [
        DataObject(paymethod=TrustWebPage(Via.card(get_card()), in_browser=True),
                   user_type=uids.Types.random_from_all).new(single_purchase=True),
        DataObject(paymethod=TrustWebPage(Via.card(get_card()), in_browser=True),
                   user_type=uids.Types.random_from_all).new(single_purchase=False),
        DataObject(paymethod=TrustWebPage(Via.linked_card(get_card())),
                   user_type=uids.Types.random_from_all).new(single_purchase=False),
        DataObject(
            paymethod=TrustWebPage(Via.card(get_card()), in_browser=True, template_tag=defaults.TemplateTag.mobile),
            user_type=uids.Types.random_from_all).new(single_purchase=False),
    ]


def ids_data(val):
    return 'paymethod={}-single_purchase={}'.format(val.paymethod.title,
                                                    val.single_purchase)


@reporter.feature(features.Service.Afisha)
@reporter.story(stories.General.Payment)
class TestAfishaMoviePass(object):
    @pytest.mark.parametrize('test_data', Data.test_data, ids=ids_data)
    def test_normal_subscription_continuation(self, test_data):
        paymethod, user_type = test_data.paymethod, test_data.user_type
        user = uids.get_random_of_type(user_type)
        paymethod.init(service, user)
        with payments_api.Subscriptions.create_normal(service, user,
                                                      single_purchase=test_data.single_purchase) as subs:
            orders = [{"order_id": subs['order_id'], "currency": 'RUR', 'region_id': '225', "qty": 1}]
            with check_mode(CheckMode.FAILED):
                payments_api.Payments.process(service, paymethod, user=user, orders=orders)
                payments_api.Wait.until_subscription_continuation(service, user, subs['order_id'])

    def test_link_card_after_subscribe(self):
        user = uids.get_random_of(uids.all_)
        with reporter.step('Оплачиваем подписку и проверяем что карта оплаты привызалась к пользователю'):
            card = get_card()
            paymethod, region_id, currency = (TrustWebPage(Via.card(card)), 225, 'RUB')
            with payments_api.Subscriptions.create_normal(service, user, region_id) as subs:
                orders = [{"order_id": subs['order_id'], "currency": currency, 'region_id': region_id, "qty": 1}]
                with check_mode(CheckMode.FAILED):
                    payments_api.Payments.process(service, paymethod, user=user, orders=orders, currency=currency)
                linked_card = simple_steps.find_card_by_masked_number(service=service, user=user,
                                                                      number=get_masked_number(
                                                                             card['card_number']),
                                                                      list_payment_methods_callback=payments_api.PaymentMethods.get)
                check.check_that(linked_card, is_not(None),
                                 step=u'Проверяем что у пользователя появилась привязаннвя карта',
                                 error=u'У пользователя не появилось привязанной карты')
        with reporter.step('Оплачиваем привязанной картой новый заказ'):
            with payments_api.Subscriptions.create_normal(service, user, region_id) as subs:
                paymethod, region_id, currency = (LinkedCard(card=card), 225, 'RUB')
                orders = [{"order_id": subs['order_id'], "currency": currency, 'region_id': region_id, "qty": 1}]
                payments_api.Payments.process(service, paymethod, user=user, orders=orders, currency=currency)

