# coding=utf-8
import pytest
from hamcrest import has_entries

import btestlib.reporter as reporter
from btestlib.constants import Services
from btestlib.utils import CheckMode, check_mode, Pytest
from simpleapi.common import logger
from simpleapi.common.payment_methods import TrustWebPage, Via
from simpleapi.common.utils import DataObject
from simpleapi.data import defaults
from simpleapi.data import features, stories
from simpleapi.data import marks
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import CVN, get_card, RBS, Sberbank
from simpleapi.steps import check_steps as check
from simpleapi.steps import expected_steps as expected
from simpleapi.steps import simple_steps as simple

__author__ = 'slppls'

log = logger.get_logger()


class Data(object):
    yam_services = {
        Services.MUSIC,
        Services.TICKETS,
        Services.UFS,
        Services.SERVICE_FOR_PROCESSINGS_TESTING,
    }
    rbs_services = [
        Services.TICKETS
    ]
    yam_paymethods = [
        marks.web_in_browser(
            DataObject(paymethod=TrustWebPage(Via.card(get_card(cvn=CVN.force_3ds)), in_browser=True),
                       user_type=uids.Types.random_from_all)),
        marks.web_in_browser(
            DataObject(paymethod=TrustWebPage(Via.linked_card(get_card(cvn=CVN.force_3ds)), in_browser=True),
                       user_type=uids.Types.random_from_all)),
    ]
    rbs_success_paymethods = [
        marks.web_in_browser(
            DataObject(paymethod=TrustWebPage(Via.card(RBS.Success.With3DS.card_mastecard), in_browser=True),
                       user_type=uids.Types.random_for_rbs)),
    ]
    sberbank_success_paymethods = [
        pytest.mark.skipif(True, reason="SberBank does not work")(
            marks.web_in_browser(
                DataObject(paymethod=TrustWebPage(Via.card(Sberbank.Success.With3DS.card_discover), in_browser=True),
                           user_type=uids.Types.random_for_sberbank))),
    ]
    rbs_failed_paymethods = [
        marks.web_in_browser(
            DataObject(paymethod=TrustWebPage(Via.card(RBS.Failed.PaRes.card_visa), in_browser=True),
                       user_type=uids.Types.random_for_rbs)),
        marks.web_in_browser(
            DataObject(paymethod=TrustWebPage(Via.card(RBS.Failed.PaRes.card_mastercard), in_browser=True),
                       user_type=uids.Types.random_for_rbs)),
    ]
    sberbank_failed_paymethods = [
        marks.web_in_browser(
            DataObject(paymethod=TrustWebPage(Via.card(Sberbank.Failed.PaRes.card_visa), in_browser=True),
                       user_type=uids.Types.random_for_sberbank)),
        marks.web_in_browser(
            DataObject(paymethod=TrustWebPage(Via.card(Sberbank.Failed.PaRes.card_mastercard), in_browser=True),
                       user_type=uids.Types.random_for_sberbank)),
    ]
    orders_structure = [
        defaults.Order.structure_rub_one_order,
        defaults.Order.structure_rub_two_orders,
    ]


@reporter.feature(features.General.With3DS)
class Test3DS(object):
    @reporter.story(stories.General.Success)
    @pytest.mark.parametrize(*Pytest.combine_set(Pytest.ParamsSet(names='test_data',
                                                                  values=[
                                                                      Data.yam_paymethods,
                                                                      Data.rbs_success_paymethods,
                                                                      Data.sberbank_success_paymethods
                                                                  ]),
                                                 Pytest.ParamsSet(names='service',
                                                                  values=[
                                                                      Data.yam_services,
                                                                      Data.rbs_services,
                                                                      Data.rbs_services
                                                                  ])),
                             ids=lambda test_data, service: '{}-{}'.format(DataObject.ids_service(service),
                                                                           DataObject.ids(test_data)))
    @pytest.mark.parametrize('orders_structure', Data.orders_structure,
                             ids=lambda orders_structure: DataObject.ids_orders(orders_structure))
    def test_3ds_success(self, test_data, service, orders_structure):
        paymethod, user_type = test_data.paymethod, test_data.user_type
        user = uids.get_random_of_type(user_type)
        with check_mode(CheckMode.FAILED):
            basket = simple.process_payment(service, user, orders_structure=orders_structure, paymethod=paymethod,
                                            success_3ds_payment=True, need_postauthorize=True)
            simple.process_refund(service, trust_payment_id=basket['trust_payment_id'], basket=basket, user=user)

    @reporter.story(stories.General.Failed)
    @pytest.mark.parametrize('service', Data.yam_services, ids=DataObject.ids_service)
    @pytest.mark.parametrize('test_data', Data.yam_paymethods, ids=DataObject.ids)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure,
                             ids=lambda orders_structure: DataObject.ids_orders(orders_structure))
    def test_yam_3ds_failed(self, test_data, service, orders_structure):
        paymethod, user_type = test_data.paymethod, test_data.user_type
        user = uids.get_random_of_type(user_type)
        with check_mode(CheckMode.IGNORED):
            basket = simple.process_payment(service, user, orders_structure=orders_structure, paymethod=paymethod,
                                            success_3ds_payment=False, should_failed=True)
        check.check_that(basket, has_entries(expected.With3DS.ym_fail_3DS()),
                         step=u'Проверяем, что оплата зафейлилась по причине неверного кода 3DS',
                         error=u'Некорректное сообщение об ошибке')

    @reporter.story(stories.General.Failed)
    @pytest.mark.parametrize('service', Data.rbs_services, ids=DataObject.ids_service)
    @pytest.mark.parametrize('test_data', Data.rbs_failed_paymethods, ids=DataObject.ids)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure,
                             ids=lambda orders_structure: DataObject.ids_orders(orders_structure))
    def test_rbs_3ds_failed(self, test_data, service, orders_structure):
        paymethod, user_type = test_data.paymethod, test_data.user_type
        user = uids.get_random_of_type(user_type)
        with check_mode(CheckMode.IGNORED):
            basket = simple.process_payment(service, user, orders_structure=orders_structure, paymethod=paymethod,
                                            success_3ds_payment=False, should_failed=True)
        check.check_that(basket, has_entries(expected.With3DS.rbs_fail_3DS()),
                         step=u'Проверяем, что оплата зафейлилась по причине неверного кода 3DS',
                         error=u'Некорректное сообщение об ошибке')

    @reporter.story(stories.General.Failed)
    @pytest.mark.skipif(True, reason="SberBank does not work")
    @pytest.mark.parametrize('service', Data.rbs_services, ids=DataObject.ids_service)
    @pytest.mark.parametrize('test_data', Data.sberbank_failed_paymethods, ids=DataObject.ids)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure,
                             ids=lambda orders_structure: DataObject.ids_orders(orders_structure))
    def test_sberbank_3ds_failed(self, test_data, service, orders_structure):
        paymethod, user_type = test_data.paymethod, test_data.user_type
        user = uids.get_random_of_type(user_type)
        with check_mode(CheckMode.IGNORED):
            basket = simple.process_payment(service, user, orders_structure=orders_structure, paymethod=paymethod,
                                            success_3ds_payment=False, should_failed=True)
        check.check_that(basket, has_entries(expected.With3DS.ym_fail_3DS()),
                         step=u'Проверяем, что оплата зафейлилась по причине неверного кода 3DS',
                         error=u'Некорректное сообщение об ошибке')


if __name__ == '__main__':
    pytest.main()
