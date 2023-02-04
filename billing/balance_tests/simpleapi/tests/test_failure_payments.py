# coding=utf-8
import pytest
from hamcrest import has_entries

import btestlib.reporter as reporter
from btestlib.constants import Services
from btestlib.utils import CheckMode, check_mode
from simpleapi.common.payment_methods import TrustWebPage, Via, LinkedCard, ApplePay, GooglePay
from simpleapi.common.utils import current_scheme_is, DataObject
from simpleapi.data import features, stories
from simpleapi.data import marks
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import get_card, CVN, Ecommpay, Tinkoff
from simpleapi.data.defaults import Status, Order, CountryData, Fiscal, tinkoff_pass_params
from simpleapi.steps import check_steps as check
from simpleapi.steps import expected_steps as expected
from simpleapi.steps import mongo_steps as mongo
from simpleapi.steps import simple_steps as simple
import simpleapi.steps.payments_api_steps as payments_api
__author__ = 'fellow'


class Data(object):
    test_data = [
        pytest.mark.skipif(not current_scheme_is('BS'), reason="Only BS scheme")
        (DataObject(service=Services.TICKETS,
                    paymethod=TrustWebPage(Via.card(get_card(cvn=CVN.not_enough_funds_RC51))),
                    user_type=uids.Types.random_from_all,
                    error=expected.BasketError.not_enough_funds(Status.not_enough_funds_RC51))
         ),
        pytest.mark.skipif(not current_scheme_is('BS'), reason="Only BS scheme")
        (DataObject(service=Services.TICKETS,
                    paymethod=TrustWebPage(Via.card(get_card(cvn=CVN.do_not_honor_RC05))),
                    user_type=uids.Types.random_from_all,
                    error=expected.BasketError.authorization_reject(Status.do_not_honor_RC05))
         ),
        pytest.mark.skipif(not current_scheme_is('BS'), reason="Only BS scheme")
        (DataObject(service=Services.TICKETS,
                    paymethod=TrustWebPage(Via.card(get_card(cvn=CVN.error_RC06))),
                    user_type=uids.Types.random_from_all,
                    error=expected.BasketError.payment_gateway_technical_error(Status.error_RC06))
         ),
        pytest.mark.skipif(not current_scheme_is('BS'), reason="Only BS scheme")
        (DataObject(service=Services.REALTYPAY,
                    paymethod=TrustWebPage(Via.card(get_card(cvn=CVN.invalid_transaction_RC12))),
                    user_type=uids.Types.random_from_all,
                    error=expected.BasketError.payment_gateway_technical_error(Status.invalid_transaction_RC12))
         ),
        pytest.mark.skipif(not current_scheme_is('BS'), reason="Only BS scheme")
        (DataObject(service=Services.REALTYPAY,
                    paymethod=TrustWebPage(Via.card(get_card(cvn=CVN.restricted_card_RC36))),
                    user_type=uids.Types.random_from_all,
                    error=expected.BasketError.authorization_reject(Status.restricted_card_RC36))
         ),
        pytest.mark.skipif(not current_scheme_is('BS'), reason="Only BS scheme")
        (DataObject(service=Services.REALTYPAY,
                    paymethod=TrustWebPage(Via.card(get_card(cvn=CVN.transaction_not_permitted_RC57))),
                    user_type=uids.Types.random_from_all,
                    error=expected.BasketError.transaction_not_permitted(Status.transaction_not_permitted_RC57))
         ),
        pytest.mark.skipif(not current_scheme_is('BS'), reason="Only BS scheme")
        (DataObject(service=Services.TICKETS,
                    paymethod=TrustWebPage(Via.card(get_card(cvn=CVN.transaction_not_permitted_RC58))),
                    user_type=uids.Types.random_from_all,
                    error=expected.BasketError.authorization_reject(Status.transaction_not_permitted_RC58))
         ),
        pytest.mark.skipif(not current_scheme_is('BS'), reason="Only BS scheme")
        (DataObject(service=Services.TICKETS,
                    paymethod=TrustWebPage(Via.card(get_card(cvn=CVN.restricted_card_RC62))),
                    user_type=uids.Types.random_from_all,
                    error=expected.BasketError.authorization_reject(Status.restricted_card_RC62))
         ),

        # tests below are for BO scheme
        pytest.mark.skipif(not current_scheme_is('BO'), reason="Only BO scheme")
        (DataObject(service=Services.DISK,
                    paymethod=TrustWebPage(Via.card(get_card(cvn=CVN.error_RC06))),
                    user_type=uids.Types.random_from_all,
                    error=expected.BasketError.technical_error(Status.error_RC06))
         ),
        pytest.mark.skipif(not current_scheme_is('BO'), reason="Only BO scheme")
        (DataObject(service=Services.DISK,
                    paymethod=TrustWebPage(Via.card(get_card(cvn=CVN.invalid_transaction_RC12))),
                    user_type=uids.Types.random_from_all,
                    error=expected.BasketError.technical_error(Status.invalid_transaction_RC12))
         ),
        pytest.mark.skipif(not current_scheme_is('BO'), reason="Only BO scheme")
        (DataObject(service=Services.DISK,
                    paymethod=TrustWebPage(Via.card(get_card(cvn=CVN.restricted_card_RC36))),
                    user_type=uids.Types.random_from_all,
                    error=expected.BasketError.technical_error(Status.restricted_card_RC36))
         ),
    ]

    test_data_ecommpay = [
        pytest.mark.skipif(not current_scheme_is('BS'), reason="Only BS scheme")(
            marks.ecommpay_processing(
                (DataObject(service=Services.TAXI,
                            paymethod=LinkedCard(card=Ecommpay.ProcessingError.Without3DS.card_mastercard),
                            user_type=uids.Types.random_from_test_passport,
                            country_data=CountryData.Germany,
                            error=expected.BasketError.ecommpay_error())
                 ),
            ))
    ]

    test_data_blocking_reason = [
        DataObject(service=Services.TAXI,
                   paymethod=LinkedCard(card=get_card()),
                   user_type=uids.Types.random_from_all),
        DataObject(service=Services.TICKETS,
                   paymethod=TrustWebPage(Via.card(card=get_card())),
                   user_type=uids.Types.random_from_all),
        DataObject(service=Services.TICKETS,
                   paymethod=TrustWebPage(Via.linked_card(card=get_card())),
                   user_type=uids.Types.random_from_all),
    ]

    test_data_apple_or_google_token = [
        pytest.mark.skipif(not current_scheme_is('BS'), reason="Only BS scheme")
        (marks.apple_pay(DataObject(service=Services.TICKETS,
                                    paymethod=ApplePay(),
                                    user_type=uids.Types.random_from_all,
                                    orders_structure=Order.gen_single_order_structure(Status.not_enough_funds_RC51),
                                    error=expected.BasketError.not_enough_funds(Status.not_enough_funds_RC51))
                         )),
        pytest.mark.skipif(not current_scheme_is('BS'), reason="Only BS scheme")
        (marks.apple_pay(DataObject(service=Services.TICKETS,
                                    paymethod=ApplePay(),
                                    user_type=uids.Types.random_from_all,
                                    orders_structure=Order.gen_single_order_structure(Status.do_not_honor_RC05),
                                    error=expected.BasketError.authorization_reject(Status.do_not_honor_RC05))
                         )),
        pytest.mark.skipif(not current_scheme_is('BS'), reason="Only BS scheme")
        (marks.apple_pay(DataObject(service=Services.TICKETS,
                                    paymethod=ApplePay(),
                                    user_type=uids.Types.random_from_all,
                                    orders_structure=Order.gen_single_order_structure(Status.error_RC06),
                                    error=expected.BasketError.payment_gateway_technical_error(Status.error_RC06))
                         )),
        pytest.mark.skipif(not current_scheme_is('BS'), reason="Only BS scheme")
        (marks.apple_pay(DataObject(service=Services.TICKETS,
                                    paymethod=ApplePay(),
                                    user_type=uids.Types.random_from_all,
                                    orders_structure=Order.gen_single_order_structure(Status.invalid_transaction_RC12),
                                    error=expected.BasketError.payment_gateway_technical_error(
                                        Status.invalid_transaction_RC12))
                         )),
        pytest.mark.skipif(not current_scheme_is('BS'), reason="Only BS scheme")
        (marks.apple_pay(DataObject(service=Services.TAXI,
                                    paymethod=ApplePay(),
                                    user_type=uids.Types.random_from_all,
                                    orders_structure=Order.gen_single_order_structure(Status.restricted_card_RC36),
                                    error=expected.BasketError.authorization_reject(Status.restricted_card_RC36))
                         )),
        pytest.mark.skipif(not current_scheme_is('BS'), reason="Only BS scheme")
        (marks.apple_pay(DataObject(service=Services.TAXI,
                                    paymethod=ApplePay(),
                                    user_type=uids.Types.random_from_all,
                                    orders_structure=Order.gen_single_order_structure(
                                        Status.transaction_not_permitted_RC57),
                                    error=expected.BasketError.transaction_not_permitted(
                                        Status.transaction_not_permitted_RC57))
                         )),
        pytest.mark.skipif(not current_scheme_is('BS'), reason="Only BS scheme")
        (marks.apple_pay(DataObject(service=Services.TAXI,
                                    paymethod=ApplePay(),
                                    user_type=uids.Types.random_from_all,
                                    orders_structure=Order.gen_single_order_structure(
                                        Status.transaction_not_permitted_RC58),
                                    error=expected.BasketError.authorization_reject(
                                        Status.transaction_not_permitted_RC58))
                         )),
        pytest.mark.skipif(not current_scheme_is('BS'), reason="Only BS scheme")
        (marks.apple_pay(DataObject(service=Services.TAXI,
                                    paymethod=ApplePay(),
                                    user_type=uids.Types.random_from_all,
                                    orders_structure=Order.gen_single_order_structure(Status.restricted_card_RC62),
                                    error=expected.BasketError.authorization_reject(Status.restricted_card_RC62))
                         )),
        pytest.mark.skipif(not current_scheme_is('BS'), reason="Only BS scheme")
        (marks.google_pay(DataObject(service=Services.TAXI,
                                     paymethod=GooglePay(),
                                     user_type=uids.Types.random_from_all,
                                     orders_structure=Order.gen_single_order_structure(Status.fraud_error),
                                     error=expected.BasketError.fraud_error())
                          )),
    ]

    test_data_tinkoff_payment = [
        pytest.mark.skipif(not current_scheme_is('NG'), reason="Only NG scheme")(
            marks.tinkoff_processing(
                (DataObject(service=Services.MESSENGER,
                            paymethod=LinkedCard(card=Tinkoff.NotEnoughMoney.card_mastercard),
                            user_type=uids.Types.random_from_test_passport,
                            error=expected.RestBasketError.not_enough_money(),
                            descr='not_enough_money')
                 ),
            )),
        pytest.mark.skipif(not current_scheme_is('NG'), reason="Only NG scheme")(
            marks.tinkoff_processing(
                (DataObject(service=Services.MESSENGER,
                            paymethod=LinkedCard(card=Tinkoff.ExpiredCard.card_mastercard),
                            user_type=uids.Types.random_from_test_passport,
                            error=expected.RestBasketError.card_expired(),
                            descr='expired_card')
                 ),
            )),
        pytest.mark.skipif(not current_scheme_is('NG'), reason="Only NG scheme")(
            marks.tinkoff_processing(
                (DataObject(service=Services.MESSENGER,
                            paymethod=LinkedCard(card=Tinkoff.RBSInternalError.card_mastercard),
                            user_type=uids.Types.random_from_test_passport,
                            error=expected.RestBasketError.rbs_internal_error(),
                            descr='internal_error')
                 ),
            )),
        pytest.mark.skipif(not current_scheme_is('NG'), reason="Only NG scheme")(
            marks.tinkoff_processing(
                (DataObject(service=Services.MESSENGER,
                            paymethod=LinkedCard(card=Tinkoff.TimeoutInternalError.card_mastercard),
                            user_type=uids.Types.random_from_test_passport,
                            error=expected.RestBasketError.rbs_internal_error(),
                            descr='timeout_internal_error')
                 ),
            )),
        pytest.mark.skipif(not current_scheme_is('NG'), reason="Only NG scheme")(
            marks.tinkoff_processing(
                (DataObject(service=Services.MESSENGER,
                            paymethod=LinkedCard(card=Tinkoff.IncorrectCardNumber.card_mastercard),
                            user_type=uids.Types.random_from_test_passport,
                            error=expected.RestBasketError.incorrect_card_number(),
                            descr='incorrect_card_number')
                 ),
            )),
    ]
    test_data_tinkoff_cancel = [
        pytest.mark.skipif(not current_scheme_is('NG'), reason="Only NG scheme")(
            marks.tinkoff_processing(
                (DataObject(service=Services.MESSENGER,
                            paymethod=LinkedCard(card=Tinkoff.CancelError76.card_mastercard),
                            user_type=uids.Types.random_from_test_passport,
                            error=expected.RestBasketError.incorrect_card_number(),
                            descr='incorrect_card_number')
                 ),
            )),
    ]
    test_data_tinkoff_clear = [
        pytest.mark.skipif(not current_scheme_is('NG'), reason="Only NG scheme")(
            marks.tinkoff_processing(
                (DataObject(service=Services.MESSENGER,
                            paymethod=LinkedCard(card=Tinkoff.ClearingError05.card_mastercard),
                            user_type=uids.Types.random_from_test_passport,
                            error=expected.RestBasketError.incorrect_card_number(),
                            descr='incorrect_card_number')
                 ),
            )),
    ]


@reporter.feature(features.General.FailurePayments)
@reporter.story(stories.General.Payment)
class TestFailurePayment(object):
    @pytest.mark.parametrize('test_data', Data.test_data, ids=DataObject.ids_error)
    def test_failure_payment(self, test_data):
        service, paymethod, user_type, expected_status = \
            test_data.service, test_data.paymethod, test_data.user_type, test_data.error
        user = uids.get_random_of_type(user_type)
        orders = simple.form_orders_for_create(service, user)

        with check_mode(CheckMode.IGNORED):
            basket = simple.process_payment(service, user, orders=orders, paymethod=paymethod, should_failed=True)
        check.check_that(basket, has_entries(expected_status),
                         step=u'Проверяем код ошибки после попытки оплаты',
                         error=u'Некорректный статус после оплаты')

    @pytest.mark.parametrize('test_data', Data.test_data_ecommpay, ids=DataObject.ids_error)
    def test_ecommpay_failure_payment(self, test_data):
        service, paymethod, country, user_type, expected_status = \
            test_data.service, test_data.paymethod, test_data.country_data, test_data.user_type, test_data.error
        user = uids.get_random_of_type(user_type)
        user_ip, currency, region_id = country['user_ip'], country['currency'], country['region_id']
        orders = simple.form_orders_for_create(service, user)

        with check_mode(CheckMode.IGNORED):
            basket = simple.process_payment(service, user, orders=orders, paymethod=paymethod, user_ip=user_ip,
                                            region_id=region_id, should_failed=True)
        check.check_that(basket, has_entries(expected_status),
                         step=u'Проверяем код ошибки после попытки оплаты',
                         error=u'Некорректный статус после оплаты')

    @pytest.fixture()
    def make_card_blocked(self, request):
        def blocked_card(service, user, paymethod, orders, reason):
            with reporter.step('Оплачиваем корзину и блокируем карту, '
                               'которой проводилась оплата: blocking_reason={}'.format(reason)):
                basket = simple.process_payment(service, user=user, orders=orders, paymethod=paymethod)

                mongo.Card.update_data(basket['trust_payment_id'],
                                       data_to_update={'blocking_reason': reason})

            def fin():
                with reporter.step('Убираем у карты blocking_reason в конце теста'):
                    mongo.Card.update_data(basket['trust_payment_id'],
                                           data_to_update={'blocking_reason': None},
                                           wait=False)

            request.addfinalizer(fin)

        return blocked_card

    @pytest.mark.skipif(not current_scheme_is('BS'), reason="Only BS scheme")
    @pytest.mark.parametrize('test_data', Data.test_data_blocking_reason, ids=DataObject.ids)
    def test_blocking_reason(self, test_data, make_card_blocked):
        service, paymethod, user_type = test_data.service, test_data.paymethod, test_data.user_type
        user = uids.get_random_of_type(user_type)
        reason = 'Some test reason'
        orders = simple.form_orders_for_create(service, user)

        make_card_blocked(service, user=user, paymethod=paymethod, orders=orders, reason=reason)

        with check_mode(CheckMode.IGNORED):
            basket = simple.process_payment(service, user=user, orders=orders, paymethod=paymethod, should_failed=True)

        check.check_that(basket, has_entries(expected.BasketError.card_is_blocked(reason)),
                         step=u'Проверяем что оплата заблокированной картой не прошла',
                         error=u'Оплата заблокированной картой прошла или ошибка некорректна')

    @pytest.mark.parametrize('test_data', Data.test_data_apple_or_google_token, ids=DataObject.ids_error)
    def test_apple_or_google_pay(self, test_data):
        service, paymethod, country, user_type, orders_structure, expected_status = \
            test_data.service, test_data.paymethod, test_data.country_data, test_data.user_type, test_data.orders_structure, test_data.error
        user = uids.get_random_of_type(user_type)
        orders = simple.form_orders_for_create(service, user, orders_structure)
        with check_mode(CheckMode.IGNORED):
            basket = simple.process_payment(service, user, orders=orders, paymethod=paymethod, should_failed=True)

        check.check_that(basket, has_entries(expected_status),
                         step=u'Проверяем код ошибки после попытки оплаты',
                         error=u'Некорректный статус после оплаты')

    @pytest.mark.parametrize('test_data', Data.test_data_tinkoff_payment, ids=DataObject.ids)
    def test_tinkoff_failure_payment(self, test_data):
        service, paymethod, user_type, expected_status = \
            test_data.service, test_data.paymethod, test_data.user_type, test_data.error
        user = uids.get_random_of_type(user_type)

        with check_mode(CheckMode.IGNORED):
            basket = payments_api.Payments.process(service, paymethod, user,
                                                   pass_params=tinkoff_pass_params, should_failed=True)
        check.check_that(basket, has_entries(expected_status),
                         step=u'Проверяем код ошибки после попытки оплаты',
                         error=u'Некорректный статус после оплаты')

    @pytest.mark.parametrize('test_data', Data.test_data_tinkoff_cancel, ids=DataObject.ids)
    def test_tinkoff_failure_cancel(self, test_data):
        service, paymethod, user_type, expected_status = \
            test_data.service, test_data.paymethod, test_data.user_type, test_data.error
        user = uids.get_random_of_type(user_type)

        with check_mode(CheckMode.IGNORED):
            purchase_token = payments_api.Payments.process(service, paymethod, user,
                                                           need_clearing=True,
                                                           pass_params=tinkoff_pass_params)['purchase_token']
            payments_api.Refunds.process(service, user, purchase_token)
            basket = payments_api.Payments.get(service, user, purchase_token)
        check.check_that(basket, has_entries(expected_status),
                         step=u'Проверяем код ошибки после попытки отмены',
                         error=u'Некорректный статус после отмены')

    @pytest.mark.parametrize('test_data', Data.test_data_tinkoff_clear, ids=DataObject.ids)
    def test_tinkoff_failure_clear(self, test_data):
        service, paymethod, user_type, expected_status = \
            test_data.service, test_data.paymethod, test_data.user_type, test_data.error
        user = uids.get_random_of_type(user_type)

        with check_mode(CheckMode.IGNORED):
            basket = payments_api.Payments.process(service, paymethod, user,
                                                   need_clearing=True,
                                                   pass_params=tinkoff_pass_params)['purchase_token']
        check.check_that(basket, has_entries(expected_status),
                         step=u'Проверяем код ошибки после попытки клиринга',
                         error=u'Некорректный статус после клиринга')


if __name__ == '__main__':
    pytest.main()
