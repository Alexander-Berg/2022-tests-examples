# coding=utf-8
import pytest

import btestlib.reporter as reporter
from btestlib.constants import Services
from btestlib.utils import CheckMode, check_mode
from simpleapi.common import logger
from simpleapi.common.payment_methods import TrustWebPage, Via
from simpleapi.common.utils import DataObject
from simpleapi.data import defaults
from simpleapi.data import features, stories
from simpleapi.data import marks
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import get_card
from simpleapi.steps import simple_steps as simple
from simpleapi.steps import trust_steps as trust

__author__ = 'fellow'

log = logger.get_logger()

service = Services.REALTYPAY

class Data(object):
    test_data = [
        marks.ym_h2h_processing(
            marks.web_in_browser(DataObject(paymethod=TrustWebPage(Via.card(get_card()), in_browser=True),
                                            user_type=uids.Types.random_from_all))),
        pytest.mark.yamoney(DataObject(paymethod=TrustWebPage(Via.yandex_money()),
                                       user_type=uids.Type(pool=uids.secret, name='test_wo_proxy_old'))),
        # https://st.yandex-team.ru/TRUST-2852
        # DataObject(paymethod=TrustWebPage(Via.phone()), user_type=uids.Types.random_with_phone),
    ]
    orders_structure = [
        defaults.Order.structure_rub_one_order,
        defaults.Order.structure_rub_two_orders,
    ]


@reporter.feature(features.Service.Realty)
@pytest.mark.skipif(True, reason="Realty betrayed us!")
class TestRealty(object):
    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('test_data', Data.test_data, ids=DataObject.ids)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure, ids=DataObject.ids_orders)
    def test_base_payment_cycle(self, test_data, orders_structure):
        paymethod, user_type = test_data.paymethod, test_data.user_type
        user = uids.get_random_of_type(user_type)
        paymethod.init(service, user)
        orders = simple.form_orders_for_create(service, user, orders_structure)

        with check_mode(CheckMode.FAILED):
            basket = simple.create_basket(service, user=user,
                                          orders=orders,
                                          paymethod_id=paymethod.id)
            basket = simple.pay_basket(service, user=user,
                                       trust_payment_id=basket['trust_payment_id'])
            trust.pay_by(paymethod, service, user=user,
                         payment_form=basket['payment_form'])

            simple.wait_until_payment_done(service, user=user,
                                           trust_payment_id=basket['trust_payment_id'])

            basket = simple.check_basket(service, user=user, trust_payment_id=basket['trust_payment_id'])
            orders_for_update = simple.form_orders_for_update(orders)
            simple.update_basket(service, orders=orders_for_update, user=user,
                                 trust_payment_id=basket['trust_payment_id'])
            simple.wait_until_real_postauth(service, user=user,
                                            trust_payment_id=basket['trust_payment_id'])
            simple.process_refund(service, trust_payment_id=basket['trust_payment_id'], basket=basket, user=user)

    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('length', [16, 17, 18, 19], ids=DataObject.ids_card_length)
    def test_card_serial_numbers(self, length):
        paymethod = TrustWebPage(Via.card(get_card(length=length)))
        user = uids.get_random_of(uids.mimino)

        orders = simple.form_orders_for_create(service, user, Data.orders_structure[0])
        with check_mode(CheckMode.FAILED):
            simple.process_payment(service=service, user=user, paymethod=paymethod, orders=orders)


if __name__ == '__main__':
    pytest.main()
