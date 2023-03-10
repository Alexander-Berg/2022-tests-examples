# coding=utf-8
import datetime

import pytest
from hamcrest import is_, equal_to, is_in

import btestlib.reporter as reporter
from btestlib.constants import Services
from btestlib.utils import CheckMode, check_mode
from simpleapi.common import logger
from simpleapi.common.payment_methods import LinkedCard, Compensation, \
    ApplePay, Subsidy, GooglePay
from simpleapi.common.utils import DataObject
from simpleapi.data import defaults
from simpleapi.data import features, stories
from simpleapi.data import marks
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import get_card, RBS, Payture, Ecommpay, Sberbank
from simpleapi.matchers.deep_equals import deep_equals_to, deep_contains
from simpleapi.steps import check_steps as check
from simpleapi.steps import db_steps
from simpleapi.steps import expected_steps as expected
from simpleapi.steps import simple_steps as simple
from simpleapi.steps import simple_steps_bo as simple_bo
from simpleapi.steps import trust_steps as trust

__author__ = 'fellow'

"""
https://wiki.yandex-team.ru/balance/tz/taxibycard/
"""

log = logger.get_logger()

service = Services.TAXI
PAYMENTS_COUNT = 2  # count of payment in multipayment case


class Data(object):
    test_data_processing = [
        DataObject(paymethod=LinkedCard(card=get_card()),
                   country_data=defaults.CountryData.Russia,
                   user_type=uids.Types.random_from_test_passport),
        marks.payture_processing(
            DataObject(paymethod=LinkedCard(card=Payture.Success.Without3DS.card_second),
                       country_data=defaults.CountryData.Armenia,
                       user_type=uids.Types.random_from_test_passport)),
        marks.rbs_processing(
            DataObject(paymethod=LinkedCard(card=RBS.Success.Without3DS.card_visa),
                       country_data=defaults.CountryData.Ukraine,
                       user_type=uids.Types.random_from_test_passport)),
        marks.rbs_processing(
            DataObject(paymethod=LinkedCard(card=RBS.Success.Without3DS.card_visa),
                       country_data=defaults.CountryData.Kazakhstan,
                       user_type=uids.Types.random_from_test_passport)),
        marks.sberbank_processing(
            DataObject(paymethod=LinkedCard(card=Sberbank.Success.Without3DS.card_visa),
                       country_data=defaults.CountryData.Russia,
                       user_type=uids.Types.random_for_sberbank)),
        # todo fellow: ?????????? ???? ??????-?????????? ???? ????????????????, ?????????????????????? ????????
        # marks.ym_new_h2h_processing(
        #     DataObject(paymethod=LinkedCard(card=get_card()),
        #                country_data=defaults.CountryData.Russia,
        #                user_type=uids.Types.random_for_new_yamoney_api)),
    ]
    test_data_processing_new_user = [
        DataObject(paymethod=LinkedCard(card=get_card()),
                   country_data=defaults.CountryData.Russia,
                   user_type=uids.Types.random_autoremove),
    ]
    test_data_processing_pass_cvn = [
        # yamoney section
        marks.ym_h2h_processing(
            DataObject(paymethod=LinkedCard(card=get_card()),
                       country_data=defaults.CountryData.Russia,
                       user_type=uids.Types.random_from_test_passport).new(
                expected_terminals=[
                    defaults.Terminal.YaMoney.RUB,
                    defaults.Terminal.YaMoney.RUB_emu,
                    defaults.Terminal.YaMoney.RUB_emu_afs])),
        # todo fellow: ?????????? ???? ??????-?????????? ???? ????????????????, ?????????????????????? ????????
        # marks.ym_new_h2h_processing(
        #     DataObject(paymethod=LinkedCard(card=get_card()),
        #                country_data=defaults.CountryData.Russia,
        #                user_type=uids.Types.random_for_new_yamoney_api).new(
        #         expected_terminals=[
        #             defaults.Terminal.YaMoney.RUB,
        #             defaults.Terminal.YaMoney.RUB_emu])),
        # payture section
        marks.payture_processing(
            DataObject(paymethod=LinkedCard(card=Payture.Success.Without3DS.card_second),
                       country_data=defaults.CountryData.Armenia,
                       user_type=uids.Types.random_from_test_passport).new(
                expected_terminals=[defaults.Terminal.Payture.AMD, defaults.Terminal.Payture.AMD_emu])),
        # rbs section
        marks.rbs_processing(
            DataObject(paymethod=LinkedCard(card=RBS.Success.Without3DS.card_visa),
                       country_data=defaults.CountryData.Ukraine,
                       user_type=uids.Types.random_from_test_passport).new(
                expected_terminals=[defaults.Terminal.RBS.UAH, defaults.Terminal.RBS.UAH_emu])),
        marks.rbs_processing(
            DataObject(paymethod=LinkedCard(card=RBS.Success.Without3DS.card_visa),
                       country_data=defaults.CountryData.Kazakhstan,
                       user_type=uids.Types.random_from_test_passport).new(
                expected_terminals=[defaults.Terminal.RBS.KZT, defaults.Terminal.RBS.KZT_emu])),
        # ecommpay section
        marks.ecommpay_processing(
            DataObject(paymethod=LinkedCard(card=Ecommpay.Success.Without3DS.card_mastercard),
                       country_data=defaults.CountryData.Germany,
                       user_type=uids.Types.random_from_test_passport).new(
                expected_terminals=[defaults.Terminal.Ecommpay.EUR, defaults.Terminal.Ecommpay.EUR_emu])),
        marks.ecommpay_processing(
            DataObject(paymethod=LinkedCard(card=Ecommpay.Success.Without3DS.card_mastercard),
                       country_data=defaults.CountryData.Georgia,
                       user_type=uids.Types.random_from_test_passport).new(
                expected_terminals=[defaults.Terminal.Ecommpay.GEL, defaults.Terminal.Ecommpay.GEL_emu])),
        marks.ecommpay_processing(
            DataObject(paymethod=LinkedCard(card=Ecommpay.Success.Without3DS.card_mastercard),
                       country_data=defaults.CountryData.Moldova,
                       user_type=uids.Types.random_from_test_passport).new(
                expected_terminals=[defaults.Terminal.Ecommpay.MDL, defaults.Terminal.Ecommpay.MDL_emu])),
        marks.ecommpay_processing(
            DataObject(paymethod=LinkedCard(card=Ecommpay.Success.Without3DS.card_mastercard),
                       country_data=defaults.CountryData.Latvia,
                       user_type=uids.Types.random_from_test_passport).new(
                expected_terminals=[defaults.Terminal.Ecommpay.EUR, defaults.Terminal.Ecommpay.EUR_emu])),
        marks.ecommpay_processing(
            DataObject(paymethod=LinkedCard(card=Ecommpay.Success.Without3DS.card_mastercard),
                       country_data=defaults.CountryData.Kyrgyzstan,
                       user_type=uids.Types.random_from_test_passport).new(
                expected_terminals=[defaults.Terminal.Ecommpay.KGS, defaults.Terminal.Ecommpay.KGS_emu])),
        marks.ecommpay_processing(
            DataObject(paymethod=LinkedCard(card=Ecommpay.Success.Without3DS.card_mastercard),
                       country_data=defaults.CountryData.Estonia,
                       user_type=uids.Types.random_from_test_passport).new(
                expected_terminals=[defaults.Terminal.Ecommpay.EUR, defaults.Terminal.Ecommpay.EUR_emu])),
        # sberbank section
        marks.sberbank_processing(
            DataObject(paymethod=LinkedCard(card=Sberbank.Success.Without3DS.card_visa),
                       country_data=defaults.CountryData.Russia,
                       user_type=uids.Types.random_for_sberbank).new(
                expected_terminals=[defaults.Terminal.Sberbank.RUB, defaults.Terminal.Sberbank.RUB_emu]))
    ]
    test_data_change_uid = [
        DataObject(paymethod=LinkedCard(card=get_card()),
                   country_data=defaults.CountryData.Russia,
                   user_type=uids.Types.random_from_test_passport),
    ]
    test_data_compensation = [
        DataObject(paymethod=Compensation(),
                   country_data=defaults.CountryData.Russia,
                   user_type=uids.Types.random_from_test_passport),
        DataObject(paymethod=Compensation(),
                   country_data=defaults.CountryData.Armenia,
                   user_type=uids.Types.random_from_test_passport),
        DataObject(paymethod=Compensation(),
                   country_data=defaults.CountryData.Kazakhstan,
                   user_type=uids.Types.random_from_test_passport),
        DataObject(paymethod=Compensation(),
                   country_data=defaults.CountryData.Ukraine,
                   user_type=uids.Types.random_from_test_passport),
        DataObject(paymethod=Compensation(),
                   country_data=defaults.CountryData.Serbia,
                   user_type=uids.Types.random_from_test_passport),
    ]
    test_data_subsidy = [
        DataObject(paymethod=Subsidy(),
                   country_data=defaults.CountryData.Kazakhstan,
                   user_type=uids.Types.random_from_test_passport),
        DataObject(paymethod=Subsidy(),
                   country_data=defaults.CountryData.Ukraine,
                   user_type=uids.Types.random_from_test_passport),
        DataObject(paymethod=Subsidy(),
                   country_data=defaults.CountryData.Armenia,
                   user_type=uids.Types.random_from_test_passport),
        DataObject(paymethod=Subsidy(),
                   country_data=defaults.CountryData.Georgia,
                   user_type=uids.Types.random_from_test_passport),
        DataObject(paymethod=Subsidy(),
                   country_data=defaults.CountryData.Germany,
                   user_type=uids.Types.random_from_test_passport),
        DataObject(paymethod=Subsidy(),
                   country_data=defaults.CountryData.Serbia,
                   user_type=uids.Types.random_from_test_passport)
    ]
    test_routing = [
        DataObject(paymethod=LinkedCard(card=RBS.Success.Without3DS.card_visa),
                   orders_structure=defaults.Order.structure_rub_one_order,
                   user=uids.routing['rbs']).new(
            processing=defaults.rbs_ru_processing_id),
    ]
    test_data_compensation_refund = [
        marks.ym_h2h_processing(
            DataObject(paymethod=Compensation(),
                       country_data=defaults.CountryData.Russia,
                       user_type=uids.Types.random_from_test_passport).new(
                card_paymethod=LinkedCard(card=get_card()))),
        marks.payture_processing(
            DataObject(paymethod=Compensation(),
                       country_data=defaults.CountryData.Armenia,
                       user_type=uids.Types.random_from_test_passport).new(
                card_paymethod=LinkedCard(card=Payture.Success.Without3DS.card_second))),
        marks.rbs_processing(
            DataObject(paymethod=Compensation(),
                       country_data=defaults.CountryData.Ukraine,
                       user_type=uids.Types.random_from_test_passport).new(
                card_paymethod=LinkedCard(card=RBS.Success.Without3DS.card_visa))),
        marks.rbs_processing(
            DataObject(paymethod=Compensation(),
                       country_data=defaults.CountryData.Kazakhstan,
                       user_type=uids.Types.random_from_test_passport).new(
                card_paymethod=LinkedCard(card=RBS.Success.Without3DS.card_visa))),
    ]
    test_data_pass_params = [
        DataObject(paymethod=LinkedCard(card=get_card()),
                   country_data=defaults.CountryData.Russia,
                   user_type=uids.Types.random_from_test_passport).new(
            pass_params=defaults.taxi_pass_params)
    ]
    test_data_apple_and_google_pay = [
        marks.apple_pay(
            DataObject(paymethod=ApplePay(bind_token=True),
                       country_data=defaults.CountryData.Russia,
                       user_type=uids.Types.random_from_test_passport)),
        marks.google_pay(
            DataObject(paymethod=GooglePay(),
                       country_data=defaults.CountryData.Russia,
                       user_type=uids.Types.random_from_test_passport)),
    ]
    test_data_google_pay_with_minus = [
        marks.google_pay(
            DataObject(paymethod=GooglePay(need_order_tag_minus=True),
                       user_type=uids.Types.random_from_test_passport,
                       country_data=defaults.CountryData.Russia)),
    ]
    orders_structure = [
        defaults.Order.structure_rub_one_order,
        defaults.Order.structure_rub_two_orders,
    ]
    orders_structure_partial_refunds = [
        ({'currency': 'RUB', 'price': 9.},),
        ({'currency': 'RUB', 'price': 9.},
         {'currency': 'RUB', 'price': 9.})
    ]


@reporter.feature(features.Service.Taxi)
class TestTaxi(object):
    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('test_data', Data.test_data_processing, ids=DataObject.ids)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure, ids=DataObject.ids_orders)
    def test_base_multipayment_cycle_with_refund(self, test_data, orders_structure):
        paymethod, country, user_type = \
            test_data.paymethod, test_data.country_data, test_data.user_type
        user = uids.get_random_of_type(user_type)
        user_ip, currency, region_id = country['user_ip'], country['currency'], country['region_id']
        paymethod.init(service, user, region_id=region_id, user_ip=user_ip)
        orders = simple.form_orders_for_create(service, user, orders_structure)
        payments = []
        for _ in range(PAYMENTS_COUNT):
            with check_mode(CheckMode.FAILED):
                basket = simple.create_basket(service, user=user,
                                              orders=orders,
                                              paymethod_id=paymethod.id, currency=currency)
                payment_form = simple.pay_basket(service, user=user,
                                                 trust_payment_id=basket['trust_payment_id']).get('payment_form')
                trust.pay_by(paymethod, service, user=user, payment_form=payment_form,
                             purchase_token=basket['purchase_token'])
                simple.wait_until_payment_done(service, user=user,
                                               purchase_token=basket['purchase_token'])
                payments.append(basket['trust_payment_id'])
        # ???????????? ?????? ??????????????
        for payment in payments:
            orders_for_update = simple.form_orders_for_update(orders)
            simple.update_basket(service, orders=orders_for_update, user=user,
                                 trust_payment_id=payment)
            simple.wait_until_real_postauth(service, user=user,
                                            trust_payment_id=payment)
        # ?? ?????????? ???????????????? ?????? ??????????????
        for payment in payments:
            basket = simple.check_basket(service, user=user, trust_payment_id=payment)
            simple.process_refund(service, trust_payment_id=basket['trust_payment_id'], basket=basket, user=user)
        for payment in payments:
            simple.check_basket(service, user=user, trust_payment_id=payment)

    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('test_data', Data.test_data_processing +
                                          Data.test_data_processing_new_user,
                             ids=DataObject.ids)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure, ids=DataObject.ids_orders)
    def test_base_multipayment_cycle_with_reversal(self, test_data, orders_structure):
        paymethod, country, user_type = \
            test_data.paymethod, test_data.country_data, test_data.user_type
        user = uids.get_random_of_type(user_type)
        user_ip, currency, region_id = country['user_ip'], country['currency'], country['region_id']
        paymethod.init(service, user, region_id=region_id, user_ip=user_ip)
        orders = simple.form_orders_for_create(service, user, orders_structure)
        payments = []
        for _ in range(PAYMENTS_COUNT):
            with check_mode(CheckMode.FAILED):
                basket = simple.create_basket(service, user=user,
                                              orders=orders,
                                              paymethod_id=paymethod.id, currency=currency)
                payment_form = simple.pay_basket(service, user=user,
                                                 trust_payment_id=basket['trust_payment_id']).get('payment_form')
                trust.pay_by(paymethod, service, user=user, payment_form=payment_form,
                             purchase_token=basket['purchase_token'])
                simple.wait_until_payment_done(service, user=user,
                                               purchase_token=basket['purchase_token'])
                payments.append(basket['trust_payment_id'])
        # ?????????????????????? ????????????????
        for payment in payments:
            orders_for_update = simple.form_orders_for_update(orders, default_action='cancel')
            simple.update_basket(service, orders=orders_for_update, user=user,
                                 trust_payment_id=payment)
        simple.wait_until_real_postauth(service, user=user,
                                        trust_payment_id=payment)
        basket_postauthorized = simple.check_basket(service, user=user,
                                                    trust_payment_id=payment)
        check.check_that(basket_postauthorized['status'], is_(equal_to('refund')))

    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('test_data', Data.test_data_change_uid, ids=DataObject.ids)
    def test_change_uid(self, test_data):
        paymethod, country, user_type = \
            test_data.paymethod, test_data.country_data, test_data.user_type
        user, user_new = uids.get_random_of_type(user_type, num=2)
        currency = country['currency']
        paymethod.init(service, user_new)
        with reporter.step(u'?????????????? ?????????? ?????? ???????????? ?????????????????????????? ({})'.format(user)):
            orders = simple.form_orders_for_create(service, user)
        payments = []
        with reporter.step(u'?????????????????? ???????????????????????? ?????? ???????????? ?????????????????????????? ({})'.format(user_new)):
            for _ in range(PAYMENTS_COUNT):
                with check_mode(CheckMode.FAILED):
                    basket = simple.create_basket(service, user=user_new,
                                                  orders=orders,
                                                  paymethod_id=paymethod.id, currency=currency)
                    payment_form = simple.pay_basket(service, user=user_new,
                                                     trust_payment_id=basket['trust_payment_id']).get('payment_form')
                    trust.pay_by(paymethod, service, user=user_new, payment_form=payment_form,
                                 purchase_token=basket['purchase_token'])
                    simple.wait_until_payment_done(service, user=user_new,
                                                   purchase_token=basket['purchase_token'])
                    payments.append(basket['trust_payment_id'])
        # ???????????? ?????? ??????????????
        for payment in payments:
            simple.process_postauthorize(service, user_new,
                                         trust_payment_id=payment, orders=orders)
        # ?? ?????????? ???????????????? ????
        for payment in payments:
            basket = simple.check_basket(service, user=user_new, trust_payment_id=payment)
            simple.process_refund(service, trust_payment_id=basket['trust_payment_id'], basket=basket, user=user_new)

    @reporter.story(stories.General.Compensation)
    @marks.simple_internal_logic
    @pytest.mark.parametrize('test_data', Data.test_data_compensation +
                             Data.test_data_subsidy,
                             ids=DataObject.ids)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure,
                             ids=DataObject.ids_orders)
    def test_compensation_and_subsidy_payment(self, test_data,
                                              orders_structure):
        paymethod, country, user_type = test_data.paymethod, \
                                        test_data.country_data, \
                                        test_data.user_type
        user = uids.get_random_of_type(user_type)
        user_ip, currency, region_id = \
            country['user_ip'], country['currency'], country['region_id']
        paymethod.init(service, user, region_id=region_id, user_ip=user_ip)
        orders = simple.form_orders_for_create(service, user, orders_structure)
        with check_mode(CheckMode.FAILED):
            simple.process_payment(service, user=user, orders=orders,
                                   paymethod=paymethod,
                                   currency=currency,
                                   user_ip=user_ip)

    @reporter.story(stories.General.Compensation)
    @pytest.mark.parametrize('test_data', Data.test_data_compensation_refund, ids=DataObject.ids)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure, ids=DataObject.ids_orders)
    def test_compensation_refund(self, test_data, orders_structure):
        """
        ???????? ?????????? ?????????????? ?????????????????????? ???????????????? ???????????????? ????????????, ???? ?????????????????????? ???????????????????? ??????????????
        """
        paymethod, country, user_type, card_paymethod = \
            test_data.paymethod, test_data.country_data, \
            test_data.user_type, test_data.card_paymethod
        user = uids.get_random_of_type(user_type)
        user_ip, currency, region_id = country['user_ip'], country['currency'], country['region_id']
        paymethod.init(service, user)
        orders = simple.form_orders_for_create(service, user, orders_structure)
        # ?????????????? ??????????????????????
        comp_trust_payment_id = \
            simple.process_payment(service, user, orders=orders,
                                   paymethod=paymethod, currency=currency)['trust_payment_id']
        # ???????????????? ???????????????? ????????????
        card_paymethod.init(service, user, region_id=region_id, user_ip=user_ip)
        simple.process_payment(service, user, orders=orders,
                               paymethod=card_paymethod,
                               currency=currency)
        # ?????????????????? ?????? ???? ?????????????????????? ???????????????? ??????????????
        check.check_trust_refunds(orders, comp_trust_payment_id)

    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('test_data', Data.test_data_pass_params, ids=DataObject.ids)
    def test_pass_params(self, test_data):
        """
        https://st.yandex-team.ru/TRUST-1318
        ???????????????? pass_params ?? CreateBasket
        ???????????????????????? ?????? ???????????????? ?????????? ???????????????????? ?????? ?????????????????? ???? ?????????? ?????????????? ?? ????????????????????
        (???????????????? ?????????????????? ?????? ?????????????????? ???????????????? ????????????????).
        ?????????????????? ???????????? ??????????: pass_params: {'taxi_phone': '...', 'taxi_duration': '...', ...},
        ???????????? ???????????? ???????????????? ?????????????????? ?? ???????????????????????? ??????????????????.
        """
        paymethod, country, user_type, pass_params = \
            test_data.paymethod, test_data.country_data, \
            test_data.user_type, test_data.pass_params
        user = uids.get_random_of_type(user_type)
        user_ip, currency, region_id = country['user_ip'], country['currency'], country['region_id']
        paymethod.init(service, user, region_id=region_id, user_ip=user_ip)
        orders = simple.form_orders_for_create(service, user)
        with check_mode(CheckMode.FAILED):
            basket = simple.process_payment(service, user=user, orders=orders,
                                            paymethod=paymethod, init_paymethod=False,
                                            currency=currency, pass_params=pass_params)
            check.check_log(user, basket['trust_payment_id'], pass_params)

    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('test_data', Data.test_data_processing_pass_cvn,
                             ids=DataObject.ids)
    def test_wait_for_cvn(self, test_data):
        """
        ???????? ?? CreateBasket ?????????????? wait_for_cvn=1, ???? ???????????? ???????????? ?????????????? ?????????????????? cvn'??
        (?????????????????????? ???????????? supply_payment_data)
        ?????????? ???????????????? ???????????????? ?????????? payment_timeout
        """
        paymethod, country, user_type = \
            test_data.paymethod, test_data.country_data, test_data.user_type
        user = uids.get_random_of_type(user_type)
        user_ip, currency, region_id = country['user_ip'], country['currency'], country['region_id']
        paymethod.init(service, user, region_id=region_id, user_ip=user_ip)
        orders = simple.form_orders_for_create(service, user)
        with check_mode(CheckMode.FAILED):
            simple.process_payment(service, user=user, orders=orders, paymethod=paymethod,
                                   pass_cvn=True, currency=currency, need_postauthorize=True)

    @reporter.story(stories.General.Refund)
    @pytest.mark.parametrize('test_data', Data.test_data_processing_pass_cvn, ids=DataObject.ids)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure_partial_refunds, ids=DataObject.ids_orders)
    def test_partial_refunds(self, test_data, orders_structure):
        """
        TRUST-3670
        """
        paymethod, country, user_type = \
            test_data.paymethod, test_data.country_data, test_data.user_type
        user = uids.get_random_of_type(user_type)
        user_ip, currency, region_id = country['user_ip'], country['currency'], country['region_id']
        orders = simple.form_orders_for_create(service, user, orders_structure)
        refund_parts = 3
        with check_mode(CheckMode.BROKEN):
            basket = simple.process_payment(service, user, orders=orders,
                                            paymethod=paymethod, currency=currency,
                                            user_ip=user_ip, region_id=region_id,
                                            pass_cvn=True, need_postauthorize=True)

        with check_mode(CheckMode.FAILED):
            refunds = list()
            orders_for_refund = [dict(service_order_id=order['service_order_id'],
                                      delta_amount=float(order['paid_amount']) / refund_parts)
                                 for order in basket['orders']]
            for _ in range(refund_parts):
                refunds.append(orders_for_refund)
                simple.process_refund(service, user=user,
                                      trust_payment_id=basket['trust_payment_id'],
                                      orders=orders_for_refund)
                basket = simple.check_basket(service, user=user, trust_payment_id=basket['trust_payment_id'])
                check.check_that(basket, deep_contains(expected.RegularBasket.with_refunds(orders,
                                                                                           refunds=refunds,
                                                                                           currency=currency)))

    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('test_data', Data.test_data_processing_pass_cvn, ids=DataObject.ids)
    def test_wait_for_cvn_expired(self, test_data):
        """
        ???????? ?? CreateBasket ?????????????? wait_for_cvn=1, ???? ???????????? ???????????? ?????????????? ?????????????????? cvn'??
        (?????????????????????? ???????????? supply_payment_data)
        ?????????? ???????????????? ???????????????? ?????????? payment_timeout
        ???? ???????????????????? ?????????????? payment_timeout ?????????????? ???????????? ????????????????????
        """
        paymethod, country, user_type = \
            test_data.paymethod, test_data.country_data, test_data.user_type
        user = uids.get_random_of_type(user_type)
        user_ip, currency, region_id = country['user_ip'], country['currency'], country['region_id']
        paymethod.init(service, user, region_id=region_id, user_ip=user_ip)
        orders = simple.form_orders_for_create(service, user)
        with check_mode(CheckMode.FAILED):
            basket = simple.create_basket(service, user=user, orders=orders,
                                          paymethod_id=paymethod.id,
                                          currency=currency, wait_for_cvn=1,
                                          payment_timeout=60)
            simple.pay_basket(service, user=user,
                              trust_payment_id=basket['trust_payment_id']).get('payment_form')
            # ???? ???????????????????? ?????????????? payment_timeout ?????????????? ???????????? ????????????????????
            with check_mode(CheckMode.IGNORED):
                simple.wait_until_payment_expired(service, user=user,
                                                  purchase_token=basket['purchase_token'])

    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('test_data', Data.test_data_apple_and_google_pay, ids=DataObject.ids)
    def test_apple_or_google_pay_multipayment_cycle(self, test_data):
        """
        ???????????? ???? ???????????????????? ?????????? ???????????????? ???????????? apple/google_token
        ?????? ?????????? ???????????????????? ?????????? ???????????????? ????????????:
        ?????????????? ?????????? bind_apple_token (bind_google_pay_token)
        ?? ???????????????? ?? CreateBasket.paymethod_id ???? ?????? ????????????????????????
        ?? ???????? ???????????? (?????? ???????????? ????????????) ???????? ?? ???? ?? ?????????????????? ???????????? ?????????????? payment_card_synonym
        ???????????????????????? ???????????????? user_token
        ?????? ?????????????????????? ?????????????? ?? ???? ???????????????? ?????? user_token

        ???????? ?? ?????????????? ?????????????????? ??????????, ?????? ???????????? ???????? ???? ?????????? ????????????
        ???????????? ???????????????????????? ?????????? _
        ??.??., ????????????????, '123' ?? '123_something' - ?????? ???????? ????????????
        """
        paymethod, country, user_type = \
            test_data.paymethod, test_data.country_data, test_data.user_type
        user = uids.get_random_of_type(user_type)
        currency = country['currency']
        paymethod.init(service, user)
        service_order_id = simple_bo.get_service_order_id(service)
        orders_structure = (
            {'currency': 'RUB', 'price': defaults.Order.price, 'service_order_id': service_order_id},
            {'currency': 'RUB', 'price': defaults.Order.price, 'service_order_id': str(service_order_id) + '_tips'})
        orders = simple.form_orders_for_create(service, user, orders_structure)
        payments = []
        for _ in range(PAYMENTS_COUNT):
            with check_mode(CheckMode.FAILED):
                basket = simple.create_basket(service, user=user,
                                              orders=orders,
                                              paymethod_id=paymethod.id,
                                              currency=currency)
                payment_form = simple.pay_basket(service, user=user,
                                                 trust_payment_id=basket['trust_payment_id']).get('payment_form')
                trust.pay_by(paymethod, service, user=user, payment_form=payment_form,
                             purchase_token=basket['purchase_token'])

                simple.wait_until_payment_done(service, user=user,
                                               purchase_token=basket['purchase_token'])
                payments.append(basket['trust_payment_id'])
        # ???????????? ?????? ??????????????
        for payment in payments:
            simple.process_postauthorize(service, user,
                                         trust_payment_id=payment, orders=orders)
        # ?? ?????????? ???????????????? ?????? ??????????????
        for payment in payments:
            basket = simple.check_basket(service, user=user, trust_payment_id=payment)
            simple.process_refund(service, trust_payment_id=basket['trust_payment_id'], basket=basket, user=user)

    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('test_data', Data.test_data_apple_and_google_pay, ids=DataObject.ids)
    def test_apple_or_google_pay_expected_prod_case(self, test_data):
        """
        ???????? ?????????????? ?????????? ?????????????????????? ?? ??????????

        1. ???????? ?????????? - ?????????????? ?? ??????????
        1. ?????????????? ?????????????????? ???????????????? ???? ???????????????? ?? ?????????? ???????????????? = ?????????? ???? ??_1
        2. ?????????????? ?????? ???????? ?????????????? ?? ?????????? ??????????????????: 1) - ?????????? ???? ??_1,
                                                       2) - ?????????????????? ?????????? (???????????? ???? ??????????????-????????????)
        """
        paymethod, country, user_type = \
            test_data.paymethod, test_data.country_data, test_data.user_type
        user = uids.get_random_of_type(user_type)
        currency = country['currency']
        paymethod.init(service, user)
        service_order_id = simple_bo.get_service_order_id(service)
        orders_structure = (
            {'currency': 'RUB', 'price': defaults.Order.price, 'service_order_id': service_order_id},
            {'currency': 'RUB', 'price': defaults.Order.price, 'service_order_id': str(service_order_id) + '_tips'})
        orders = simple.form_orders_for_create(service, user, orders_structure=orders_structure[0])
        with reporter.step(u'?????????????? ?????????????????????????? ???? ??????????????'), check_mode(CheckMode.FAILED):
            payments = []
            for _ in range(PAYMENTS_COUNT):
                basket = simple.create_basket(service, user=user,
                                              orders=(orders[0],),
                                              paymethod_id=paymethod.id)
                payment_form = simple.pay_basket(service, user=user,
                                                 trust_payment_id=basket['trust_payment_id']).get(
                    'payment_form')
                trust.pay_by(paymethod, service, user=user, payment_form=payment_form,
                             purchase_token=basket['purchase_token'])
                simple.wait_until_payment_done(service, user=user,
                                               purchase_token=basket['purchase_token'])
                payments.append(basket['trust_payment_id'])
        with reporter.step(u'?? ?????????????????? ?????????????? ?????? ??????????????: ???????? ?????????????? + ????????????'), check_mode(
                CheckMode.FAILED):
            basket = simple.create_basket(service, user=user,
                                          orders=orders,
                                          paymethod_id=paymethod.id)
            payment_form = simple.pay_basket(service, user=user,
                                             trust_payment_id=basket['trust_payment_id']).get(
                'payment_form')
            trust.pay_by(paymethod, service, user=user, payment_form=payment_form,
                         purchase_token=basket['purchase_token'])
            simple.wait_until_payment_done(service, user=user,
                                           purchase_token=basket['purchase_token'])
            payments.append(basket['trust_payment_id'])
        # ???????????? ?????? ??????????????
        for payment in payments:
            simple.process_postauthorize(service, user,
                                         trust_payment_id=payment, orders=orders)

    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('test_data', Data.test_data_apple_and_google_pay, ids=DataObject.ids)
    def test_apple_or_google_pay_not_group_orders(self, test_data):
        """
        ???????????? ?????? ??????????.
        ???????? ?? ?????????????? ?????????????????? ??????????, ?????? ???????????? ???????? ???? ?????????? ????????????
        ?????????? ???????????????????????? ?????????????????? ????????????
        """
        paymethod, user_type = test_data.paymethod, test_data.user_type
        user = uids.get_random_of_type(user_type)
        paymethod.init(service, user)
        orders = simple.form_orders_for_create(service, user,
                                               orders_structure=defaults.Order.structure_rub_two_orders)
        with check_mode(CheckMode.IGNORED):
            basket = simple.create_basket(service, user=user,
                                          orders=orders,
                                          paymethod_id=paymethod.id)
        check.check_that(basket, deep_equals_to(expected.RegularBasket.orders_not_in_same_group()))

    @pytest.mark.skipif(True, reason="Weird test, doesnt work")
    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('test_data', Data.test_data_apple_and_google_pay, ids=DataObject.ids)
    def test_apple_or_google_pay_order_have_another_tag(self, test_data):
        """
        ???????????? ?????? ??????????.
        ???????? ?????????????? ???????????????????????? ?????????? apple/google_token'????
        ?????????? ???????????????????????? ?????????????????? ????????????
        """
        paymethod, user_type = test_data.paymethod, test_data.user_type
        user = uids.get_random_of_type(user_type)
        orders = simple.form_orders_for_create(service, user)
        with reporter.step(u'?????????????????????? apple_token ?? ???????????????????? ???? ??????????????'):
            simple.process_payment(service, user, orders=orders, paymethod=paymethod)
        with reporter.step(u'?????????????????????? ?????????? apple_token ?? ???????????????? ???? ???????????????? ???? ???? ??????????????'), check_mode(
                CheckMode.IGNORED):
            paymethod.init(service, user)
            basket = simple.create_basket(service, user, orders=orders, paymethod_id=paymethod.id)
        check.check_that(basket, deep_equals_to(expected.RegularBasket.orer_has_another_tag()))

    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('test_data', Data.test_routing, ids=DataObject.ids_paymethod)
    def test_routing_by_uid(self, test_data):
        paymethod, orders_structure, user, processing = \
            test_data.paymethod, test_data.orders_structure, \
            test_data.user, test_data.processing
        paymethod.init(service, user)
        orders = simple.form_orders_for_create(service, user, orders_structure)
        with check_mode(CheckMode.FAILED):
            basket = simple.create_basket(service, user=user,
                                          orders=orders,
                                          paymethod_id=paymethod.id)
            payment_form = simple.pay_basket(service, user=user,
                                             trust_payment_id=basket['trust_payment_id']).get('payment_form')
            trust.pay_by(paymethod, service, user=user, payment_form=payment_form,
                         purchase_token=basket['purchase_token'])
            simple.wait_until_payment_done(service, user=user,
                                           purchase_token=basket['purchase_token'])
        processing_id = db_steps.bs().get_processing_from_payment(basket['purchase_token'])
        check.check_that(processing_id, is_(equal_to(processing)),
                         step=u'?????????????????? ???????????????????????? ???????????????????? ??????????????????????',
                         error=u'?????? ???????????? ???????????????????????? ????????????????????')

    @staticmethod
    def get_previous_update_dt_from_basket(basket):
        return datetime.datetime.fromtimestamp(float(basket['update_ts_msec']) / 1000)

    @reporter.story(stories.General.Rules)
    @pytest.mark.parametrize('test_data', Data.test_data_change_uid, ids=DataObject.ids)
    def test_update_dt_base_cycle(self, test_data):
        paymethod, user_type = test_data.paymethod, test_data.user_type
        user = uids.get_random_of_type(user_type)
        paymethod.init(service, user)
        with reporter.step(u'?????????????? ?????????????? ?? ?????????????????? ?????? ?? ?????? ?????????????????????? update_dt'):
            previous_update_dt = datetime.datetime.now().replace(microsecond=0)
            orders = simple.form_orders_for_create(service, user)
            basket = simple.create_basket(service, user=user,
                                          orders=orders,
                                          paymethod_id=paymethod.id)
            basket = simple.wait_until_update_dt(service, user, previous_update_dt=previous_update_dt,
                                                 trust_payment_id=basket['trust_payment_id'])
        with reporter.step(u'???????????????????? ?????????????? ?? ?????????????????? ?????? ?? ?????? ?????????????????? update_dt'):
            previous_update_dt = self.get_previous_update_dt_from_basket(basket)
            payment_form = simple.pay_basket(service, user=user,
                                             trust_payment_id=basket['trust_payment_id']).get('payment_form')
            trust.pay_by(paymethod, service, user=user, payment_form=payment_form,
                         purchase_token=basket['purchase_token'])
            simple.wait_until_payment_done(service, user=user,
                                           purchase_token=basket['purchase_token'])
            basket = simple.wait_until_update_dt(service, user, previous_update_dt=previous_update_dt,
                                                 trust_payment_id=basket['trust_payment_id'])
        with reporter.step(u'???????????????????????????? ?????????????? ?? ?????????????????? ?????? ?? ?????? ?????????????????? update_dt'):
            previous_update_dt = self.get_previous_update_dt_from_basket(basket)
            simple.process_postauthorize(service, user=user,
                                         trust_payment_id=basket['trust_payment_id'], orders=orders)
            basket = simple.wait_until_update_dt(service, user, previous_update_dt=previous_update_dt,
                                                 trust_payment_id=basket['trust_payment_id'])
        with reporter.step(u'???????????????? ?????????????? ?? ?????????????????? ?????? ?? ?????? ?????????????????? update_dt'):
            previous_update_dt = self.get_previous_update_dt_from_basket(basket)
            basket = simple.check_basket(service, user=user, trust_payment_id=basket['trust_payment_id'])
            simple.process_refund(service, trust_payment_id=basket['trust_payment_id'], basket=basket, user=user)
            simple.wait_until_update_dt(service, user, previous_update_dt=previous_update_dt,
                                        trust_payment_id=basket['trust_payment_id'])

    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('test_data', Data.test_data_processing_pass_cvn, ids=DataObject.ids)
    def test_processing_terminal(self, test_data):
        paymethod, country, user_type, expected_terminals = \
            test_data.paymethod, test_data.country_data, \
            test_data.user_type, test_data.expected_terminals
        user = uids.get_random_of_type(user_type)
        user_ip, currency, region_id = country['user_ip'], country['currency'], country['region_id']
        paymethod.init(service, user, region_id=region_id, user_ip=user_ip)
        orders = simple.form_orders_for_create(service, user)
        with check_mode(CheckMode.IGNORED):
            basket = simple.process_payment(service, user=user, orders=orders, paymethod=paymethod,
                                            pass_cvn=True, currency=currency)
            real_terminal = db_steps.bs().get_terminal_from_payment(basket['trust_payment_id'])
        check.check_that(real_terminal, is_in(expected_terminals),
                         step=u'?????????????????? ???????????????????????? ??????????????????',
                         error=u'???????????????? ???????????????????????? ????????????????')

    @pytest.mark.parametrize('test_data', Data.test_data_google_pay_with_minus, ids=DataObject.ids)
    def test_pay_google_pay_with_minus_in_order_tag(self, test_data):
        paymethod, country, user_type = test_data.paymethod, \
                                        test_data.country_data, test_data.user_type
        user = uids.get_random_of_type(user_type)
        currency = country['currency']
        paymethod.init(service, user)
        service_order_id = simple_bo.get_service_order_id(service)
        orders_structure = (
            {'currency': 'RUB', 'price': defaults.Order.price, 'service_order_id': service_order_id},
            {'currency': 'RUB', 'price': defaults.Order.price, 'service_order_id': str(service_order_id) + '_tips'})
        orders = simple.form_orders_for_create(service, user, orders_structure)
        with check_mode(CheckMode.FAILED):
            basket = simple.process_payment(service, user, orders=orders, paymethod=paymethod,
                                            currency=currency, need_postauthorize=True)
            simple.process_refund(service, trust_payment_id=basket['trust_payment_id'],
                                  basket=basket, user=user)
            check.check_that(basket['payment_method'][15::],
                             is_(equal_to(paymethod.order_tag.replace('-', '_'))),
                             step=u'?????????????????? ???????????????????????? ???????? order_tag ?????????? ???????????? Google Pay token',
                             error=u'???????????????????????? order_tag ?? Google Pay token ?????????? ????????????')


if __name__ == '__main__':
    pytest.main()
