# coding=utf-8
import pytest

import btestlib.reporter as reporter
from btestlib.constants import Services
from btestlib.utils import CheckMode, check_mode
from simpleapi.common.payment_methods import TrustWebPage, Compensation
from simpleapi.common.payment_methods import Via
from simpleapi.common.utils import DataObject
from simpleapi.data import features, stories
from simpleapi.data import marks
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import get_card
from simpleapi.steps import payments_api_steps as payments_api
from simpleapi.data import defaults
__author__ = 'fellow'

service = Services.UFS


class Data(object):
    test_data = [
        marks.web_in_browser(
            DataObject(paymethod=TrustWebPage(Via.linked_card(get_card(),
                                                              list_payment_methods_callback=payments_api.PaymentMethods.get,
                                                              from_linked_phonish=True),
                                              in_browser=True),
                       user_type=uids.Types.random_with_linked_phonishes)),
        marks.web_in_browser(
            DataObject(paymethod=TrustWebPage(Via.linked_card(get_card(),
                                                              list_payment_methods_callback=payments_api.PaymentMethods.get),
                                              in_browser=True),
                       user_type=uids.Types.random_from_all)),
        marks.web_in_browser(
            DataObject(paymethod=TrustWebPage(Via.card(get_card()),
                                              in_browser=True),
                       user_type=uids.Types.random_from_all)),
        DataObject(
            paymethod=TrustWebPage(Via.card(get_card()), in_browser=True,
                                   template_tag=defaults.TemplateTag.mobile),
            user_type=uids.Types.random_from_all),
        DataObject(
            paymethod=TrustWebPage(Via.linked_card(get_card(),
                                                   list_payment_methods_callback=payments_api.PaymentMethods.get),
                                   in_browser=True,
                                   template_tag=defaults.TemplateTag.mobile),
            user_type=uids.Types.random_from_all),
        DataObject(paymethod=Compensation(),
                   user_type=uids.Types.random_from_all)
    ]

    orders_structure = [
        [{'region_id': 225, 'currency': 'RUB', 'price': 10},
         {'region_id': 225, 'currency': 'RUB', 'price': 20.5},
         ],
        None
    ]


@reporter.feature(features.Service.TicketToRide)
class TestTicketToRide(object):
    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('test_data', Data.test_data, ids=DataObject.ids)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure, ids=DataObject.ids_orders)
    def test_base_payment_cycle(self, test_data, orders_structure):
        paymethod, user_type = test_data.paymethod, test_data.user_type
        user = uids.get_random_of_type(user_type)

        with check_mode(CheckMode.FAILED):
            basket = payments_api.Payments.process(service, paymethod=paymethod, user=user,
                                                   orders_structure=orders_structure,
                                                   need_clearing=True)

            payments_api.Refunds.process(service, user, basket['purchase_token'])


if __name__ == '__main__':
    pytest.main()
