# coding=utf-8
import pytest

from btestlib import reporter
from btestlib.constants import Services
from btestlib.utils import check_mode, CheckMode
from simpleapi.common.payment_methods import TrustWebPage, Via
from simpleapi.common.utils import DataObject
from simpleapi.data import marks
import simpleapi.data.cards_pool as cards_pool
import simpleapi.steps.payments_api_steps as payments_api
import simpleapi.data.uids_pool as uids
from simpleapi.data import defaults
from simpleapi.data import features, stories

__author__ = 'sunshineguy'

service = Services.MESSENGER


class Data(object):
    test_data = [
        marks.web_in_browser(
            DataObject(paymethod=TrustWebPage(Via.linked_card(cards_pool.Tinkoff.Valid.card_mastercard,
                                                              list_payment_methods_callback=payments_api.PaymentMethods.get),
                                              in_browser=True),
                       user_type=uids.Types.random_from_test_passport)),
    ]
    orders_structure = [
        defaults.Order.structure_rub_one_order,
        defaults.Order.structure_rub_two_orders,
    ]


@reporter.feature(features.Service.Messenger)
class TestMessenger(object):
    @marks.tinkoff_processing
    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('test_data', Data.test_data, ids=DataObject.ids)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure, ids=DataObject.ids)
    def test_base_payment_cycle(self, test_data, orders_structure):
        paymethod, user_type = test_data.paymethod, test_data.user_type
        user = uids.get_random_of_type(user_type)

        with check_mode(CheckMode.FAILED):
            purchase_token = payments_api.Payments.process(service, paymethod, user,
                                                           need_clearing=True, orders_structure=orders_structure,
                                                           pass_params=defaults.tinkoff_pass_params)['purchase_token']
            payments_api.Refunds.process(service, user, purchase_token)


if __name__ == '__main__':
    pytest.main()
