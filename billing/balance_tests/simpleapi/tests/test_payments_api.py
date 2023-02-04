# coding=utf-8
import datetime
import string

import pytest
from hamcrest import contains
from hamcrest import less_than_or_equal_to, greater_than_or_equal_to, has_length, equal_to, is_in, not_, \
    contains_inanyorder

import btestlib.reporter as reporter
from btestlib.constants import Services
from btestlib.utils import CheckMode, check_mode
from btestlib.utils import Date
from simpleapi.common.oauth import Auth
from simpleapi.common.payment_methods import TrustWebPage, LinkedCard
from simpleapi.common.payment_methods import Via
from simpleapi.common.utils import DataObject
from simpleapi.common.utils import simple_random as random
from simpleapi.data import defaults, cards_pool
from simpleapi.data import features, stories
from simpleapi.data import marks
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import get_card, CVN
from simpleapi.matchers.deep_equals import deep_equals_to, deep_contains
from simpleapi.steps import check_steps as check
from simpleapi.steps import expected_steps as expected
from simpleapi.steps import payments_api_steps as payments_api
from simpleapi.steps import trust_steps as trust
from simpleapi.steps import passport_steps as passport

__author__ = 'slppls'


class Data(object):
    test_data = [
        DataObject(service=Services.TICKETS,
                   paymethod=TrustWebPage(Via.card(get_card())),
                   user_type=uids.Types.anonymous),
        # (Services.REALTYPAY, TrustWebPage(Via.card(get_card())), uids.Types.random_from_all),
        DataObject(service=Services.TAXI,
                   paymethod=LinkedCard(card=get_card(),
                                        list_payment_methods_callback=payments_api.PaymentMethods.get),
                   user_type=uids.Types.random_from_all),
        DataObject(service=Services.TAXI,
                   paymethod=LinkedCard(card=get_card(),
                                        list_payment_methods_callback=payments_api.PaymentMethods.get),
                   user_type=uids.Types.random_from_phonishes)
    ]
    test_payment_link_data = [
        marks.web_in_browser(DataObject(service=Services.TICKETS,
                                        paymethod=TrustWebPage(Via.card(get_card()), in_browser=True),
                                        user_type=uids.Types.anonymous)),
        marks.web_in_browser(DataObject(service=Services.REALTYPAY,
                                        paymethod=TrustWebPage(Via.card(get_card()), in_browser=True),
                                        user_type=uids.Types.random_from_all)),
        marks.web_in_browser(DataObject(service=Services.YDF,
                                        paymethod=TrustWebPage(Via.card(card=get_card()), in_browser=True),
                                        user_type=uids.Types.random_from_all)),
    ]
    test_status = [
        DataObject(service=Services.TICKETS,
                   paymethod=TrustWebPage(Via.card(card=get_card())),
                   user_type=uids.Types.random_from_all)
    ]
    test_3ds_status = [
        marks.web_in_browser(DataObject(service=Services.TICKETS,
                                        paymethod=TrustWebPage(Via.card(card=get_card(cvn=CVN.force_3ds)),
                                                               in_browser=True),
                                        user_type=uids.Types.random_from_all))
    ]
    labels = [
        'Some_label',
        '1_SoMe_2_label_3_'
    ]
    negative_labels = [
        ' -!@$%^*()Word',
        # TODO: sunshineguy: открыть, когда баг: TRUST-5650 исправят UPD справили, ждем когда выкатится на тест.
        # 'Word -!@$%^*() Word',
        # 'Word-!@$%^*() '
    ]

    class Promocodes(object):
        data_promoseries = [
            # DataObject(descr='Mandatory pack of params').new(promoseries_params=defaults.Promoseries.mandatory_params),
            DataObject(descr='Full pack of params').new(promoseries_params=defaults.Promoseries.base_params),
            DataObject(descr='Multiple available services').new(
                promoseries_params=defaults.Promoseries.custom_params({'services': [Services.TICKETS,
                                                                                    Services.EVENTS_TICKETS,
                                                                                    Services.EVENTS_TICKETS_NEW]})),
            DataObject(descr='Partial only = 1').new(
                promoseries_params=defaults.Promoseries.custom_params({'partial_only': 1})),
            DataObject(descr='Full payment only = 1').new(
                promoseries_params=defaults.Promoseries.custom_params({'full_payment_only': 1})),
            DataObject(descr='Not unique').new(
                promoseries_params=defaults.Promoseries.custom_params({'usage_limit': 5,
                                                                       'partial_only': 1})),
            DataObject(descr='With promocodes limit').new(
                promoseries_params=defaults.Promoseries.custom_params({'limit': 5})),
        ]

        data_promocodes = [
            DataObject(descr='Full pack of params').new(
                promoseries_params=defaults.Promoseries.base_params,
                promocode_params=defaults.Promocode.base_params),
            DataObject(descr='All params are from promocode').new(
                promoseries_params=defaults.Promoseries.mandatory_params,
                promocode_params=defaults.Promocode.base_params),
            DataObject(descr='All params are from promoseries').new(
                promoseries_params=defaults.Promoseries.base_params,
                promocode_params=defaults.Promocode.mandatory_params),
            DataObject(descr='External promocode').new(
                promoseries_params=defaults.Promoseries.base_params,
                promocode_params=defaults.Promocode.custom_params(
                    {'code': u''.join(random.choice(string.ascii_uppercase + string.digits) for _ in range(10)),
                     'quantity': 1})),
        ]
        data_promoseries_limits = [
            DataObject(descr='Promocodes quantity more then promosseries limit').new(promoseries_limit=10,
                                                                                     promocodes_quantity=20),
            DataObject(descr='Promocodes quantity more then 100').new(promoseries_limit='',
                                                                      promocodes_quantity=101),
            DataObject(descr='Promocodes quantity more then 100 and promoseries limit').new(promoseries_limit=10,
                                                                                            promocodes_quantity=101)
        ]


def take_card_id(payment_methods):
    return [card.get('id') for card in payment_methods]


@reporter.feature(features.General.PaymentsAPI)
class TestPaymentsApi(object):
    @pytest.fixture
    def user_without_linked_cards(self):
        user = uids.get_random_of(uids.mutable)
        with reporter.step(u'Выбираем случайного пользователя для теста и отвязываем от него все привязанне карты'):
            reporter.logger().debug("Choose user: %s" % user)
            trust.process_unbinding(user=user)
            reporter.logger().debug("Unbind all cards of user %s before test" % user)
            cards, _ = trust.process_binding(user=user, cards=cards_pool.get_card())
            passport.auth_via_page(user=user)
            session_id = passport.get_current_session_id()
            return user, cards, session_id

    @reporter.story(stories.CardsOperations.CardsBinding)
    def test_unbind_card(self, user_without_linked_cards):
        user, cards, session_id = user_without_linked_cards
        with reporter.step(u'Удаляем карту у пользователя через REST API:'):
            payments_api.PaymentMethods.unbind_card(Services.DISK, user, cards[0], session_id)
            payment_methods = payments_api.PaymentMethods.get(Services.DISK, user)
            check.check_that(cards[0], not_(contains_inanyorder(take_card_id(payment_methods))),
                             step=u'Проверяем что карта удалилась',
                             error=u'Карта не удалилась')

    @reporter.story(stories.General.Payment)
    @pytest.mark.parametrize('test_data', Data.test_data, ids=DataObject.ids)
    def test_base_payment_cycle(self, test_data):
        service, paymethod, user_type = test_data.service, test_data.paymethod, test_data.user_type
        user = uids.get_random_of_type(user_type)
        paymethod.init(service, user)
        product_id = payments_api.Products.create_for_service(service, user)
        basket = payments_api.Payments.create(service, user=user, paymethod_id=paymethod.id,
                                              product_id=product_id)

        basket = payments_api.Payments.start(service, user, basket['purchase_token'])
        trust.pay_by(paymethod, service, user=user, payment_url=basket.get('payment_url'))

        payments_api.Wait.until_payment_done(service, user, basket['purchase_token'])

    @reporter.story(stories.General.PaymentLink)
    @pytest.mark.skipif(True, reason="Doesn't work now")
    @pytest.mark.parametrize('test_data', Data.test_payment_link_data, ids=DataObject.ids)
    def test_pay_by_payment_link(self, test_data):
        service, paymethod, user_type = test_data.service, test_data.paymethod, test_data.user_type
        user = uids.get_random_of_type(user_type)
        paymethod.init(service, user)
        product_id = payments_api.Products.create_for_service(service, user)
        _, link_id = payments_api.PaymentLinks.create(service, user=user, paymethod_id=paymethod.id,
                                                      product_id=product_id)
        resp = payments_api.PaymentLinks.pay(service, user, link_id)

        trust.pay_by(paymethod, service, user=user, payment_url=payments_api.Form.payment_link(link_id))
        payments_api.Wait.until_payment_done(service, user, resp['purchase_token'])

    @reporter.story(stories.General.PaymentStatus)
    @pytest.mark.parametrize('test_data', Data.test_status, ids=DataObject.ids)
    def test_status_base_payment_cycle(self, test_data):
        service, paymethod, user_type = test_data.service, test_data.paymethod, test_data.user_type
        user = uids.get_random_of_type(user_type)
        paymethod.init(service, user)
        product_id = payments_api.Products.create_for_service(service, user)
        # not started - check
        basket = payments_api.Payments.create(service, user=user, paymethod_id=paymethod.id,
                                              product_id=product_id)

        purchase_token = basket['purchase_token']
        basket = payments_api.Payments.get(service, user, purchase_token)
        orders = payments_api.Form.orders_for_refund(basket)

        payments_api.Wait.for_payment_status(service, user, purchase_token, defaults.PaymentApi.Status.not_started)
        # started - check
        basket = payments_api.Payments.start(service, user, purchase_token)
        payments_api.Wait.for_payment_status(service, user, purchase_token, defaults.PaymentApi.Status.started)
        # authorized - check
        trust.pay_by(paymethod, service, user=user, payment_url=basket.get('payment_url'))
        payments_api.Wait.for_payment_status(service, user, purchase_token, defaults.PaymentApi.Status.authorized)
        # cleared - check
        payments_api.Payments.clear(service, user, purchase_token)
        payments_api.Wait.for_payment_status(service, user, purchase_token, defaults.PaymentApi.Status.cleared)
        # refunded - check
        refund = payments_api.Refunds.create(service, user, purchase_token, orders)
        payments_api.Refunds.start(service, user, refund['trust_refund_id'])
        payments_api.Wait.for_payment_status(service, user, purchase_token, defaults.PaymentApi.Status.refunded)

    @reporter.story(stories.General.PaymentStatus)
    @pytest.mark.parametrize('test_data', Data.test_status, ids=DataObject.ids)
    def test_status_canceled(self, test_data):
        service, paymethod, user_type = test_data.service, test_data.paymethod, test_data.user_type
        user = uids.get_random_of_type(user_type)
        basket = payments_api.Payments.process(service, user=user, paymethod=paymethod, need_clearing=False)

        # canceled - check
        payments_api.Payments.unhold(service, user, basket['purchase_token'])
        payments_api.Wait.for_payment_status(service, user, basket['purchase_token'],
                                             defaults.PaymentApi.Status.canceled)

    @reporter.story(stories.General.PaymentStatus)
    @pytest.mark.parametrize('test_data', Data.test_status, ids=DataObject.ids)
    def test_status_not_authorized(self, test_data):
        service, user_type = test_data.service, test_data.user_type
        user = uids.get_random_of_type(user_type)
        paymethod = TrustWebPage(Via.card(get_card(cvn=CVN.not_enough_funds_RC51)))
        paymethod.init(service, user)
        product_id = payments_api.Products.create_for_service(service, user)
        # not_authorized - check
        basket = payments_api.Payments.create(service, user=user, paymethod_id=paymethod.id,
                                              product_id=product_id)
        purchase_token = basket['purchase_token']
        basket = payments_api.Payments.start(service, user, purchase_token)
        trust.pay_by(paymethod, service, user=user, payment_url=basket.get('payment_url'))
        payments_api.Wait.for_payment_status(service, user, purchase_token, defaults.PaymentApi.Status.not_authorized)

    @pytest.mark.skip(reason='Status started_3ds still not realized in trust')
    @reporter.story(stories.General.PaymentStatus)
    @pytest.mark.parametrize('test_data', Data.test_3ds_status, ids=DataObject.ids)
    def test_status_3ds_started(self, test_data):
        service, paymethod, user_type = test_data.service, test_data.paymethod, test_data.user_type
        user = uids.get_random_of_type(user_type)
        paymethod.init(service, user)
        product_id = payments_api.Products.create_for_service(service, user)
        # started_3ds - check
        basket = payments_api.Payments.create(service, user=user, paymethod_id=paymethod.id,
                                              product_id=product_id)
        purchase_token = basket['purchase_token']
        basket = payments_api.Payments.start(service, user, purchase_token)
        trust.pay_by(paymethod, service, user=user, payment_url=basket.get('payment_url'),
                     success_3ds_payment=True, break_3ds=True)
        payments_api.Wait.for_payment_status(service, user, purchase_token, defaults.PaymentApi.Status.started_3ds)

    @reporter.story(stories.General.PaymentStatus)
    @pytest.mark.parametrize('test_data', Data.test_3ds_status, ids=DataObject.ids)
    def test_status_base_3ds_payment_cycle(self, test_data):
        service, paymethod, user_type = test_data.service, test_data.paymethod, test_data.user_type
        user = uids.get_random_of_type(user_type)
        paymethod.init(service, user)
        product_id = payments_api.Products.create_for_service(service, user)
        basket = payments_api.Payments.create(service, user=user, paymethod_id=paymethod.id,
                                              product_id=product_id)

        purchase_token = basket['purchase_token']
        basket = payments_api.Payments.get(service, user, purchase_token)
        orders = payments_api.Form.orders_for_refund(basket)
        basket = payments_api.Payments.start(service, user, purchase_token)
        trust.pay_by(paymethod, service, user=user, payment_url=basket.get('payment_url'),
                     success_3ds_payment=True)
        # authorized - check
        payments_api.Wait.for_payment_status(service, user, purchase_token, defaults.PaymentApi.Status.authorized)
        # cleared - check
        payments_api.Payments.clear(service, user, purchase_token)
        payments_api.Wait.for_payment_status(service, user, purchase_token, defaults.PaymentApi.Status.cleared)
        # refunded - check
        refund = payments_api.Refunds.create(service, user, purchase_token, orders)
        payments_api.Refunds.start(service, user, refund['trust_refund_id'])
        payments_api.Wait.for_payment_status(service, user, purchase_token, defaults.PaymentApi.Status.refunded)

    @reporter.story(stories.General.PaymentStatus)
    @pytest.mark.parametrize('test_data', Data.test_3ds_status, ids=DataObject.ids)
    def test_status_3ds_not_authorized(self, test_data):
        service, paymethod, user_type = test_data.service, test_data.paymethod, test_data.user_type
        user = uids.get_random_of_type(user_type)
        paymethod.init(service, user)
        product_id = payments_api.Products.create_for_service(service, user)
        # not_authorized - check
        basket = payments_api.Payments.create(service, user=user, paymethod_id=paymethod.id,
                                              product_id=product_id)
        purchase_token = basket['purchase_token']
        basket = payments_api.Payments.start(service, user, purchase_token)
        trust.pay_by(paymethod, service, user=user, payment_url=basket.get('payment_url'),
                     success_3ds_payment=False)
        payments_api.Wait.for_payment_status(service, user, purchase_token, defaults.PaymentApi.Status.not_authorized)

    @reporter.story(stories.Methods.Call)
    @pytest.mark.parametrize('service', [Services.TICKETS,
                                         Services.EVENTS_TICKETS,
                                         Services.EVENTS_TICKETS_NEW,
                                         Services.TAXI,
                                         Services.MUSIC,
                                         Services.BUSES,
                                         Services.DOSTAVKA,
                                         Services.UFS,
                                         Services.MARKETPLACE], ids=DataObject.ids_service)
    @pytest.mark.parametrize('limit', [5, ], ids=DataObject.ids_custom(label='limit'))
    # todo fellow добавить limit=None когда пофиксят TRUST-3150
    def test_updated_payments_base_call(self, service, limit):
        from_ = Date.get_timestamp(Date.shift_date(datetime.datetime.now(), hours=-5))
        resp = payments_api.UpdatedPayments.get(service, from_=from_, limit=limit)
        check.check_that(len(resp), less_than_or_equal_to(limit or payments_api.UpdatedPayments.DEFAULT_LIMIT),
                         step=u'Проверяем что количество корзин в ответе не превосходит значение limit',
                         error=u'В ответе содержится больше корзин чем передано в параметре limit')
        with reporter.step(u'Проверяем что ответ содержит только те корзины, '
                           u'у которых update_ts позже переданного параметра from'):
            for basket in resp:
                check.check_that(float(basket['update_ts']), greater_than_or_equal_to(from_),
                                 error=u'Корзина {} попала в ответ метода, '
                                       u'хотя ее update_ts раньше указанного в вызове'.format(
                                     basket.get('purchase_token')))


@reporter.feature(features.General.PaymentsAPI)
@reporter.story(stories.General.Promocodes)
class TestPromocodes(object):
    pytestmark = marks.simple_internal_logic

    service = Services.TICKETS

    @pytest.mark.parametrize('test_data', Data.Promocodes.data_promoseries, ids=DataObject.ids)
    def test_create_promoseries(self, test_data):
        with check_mode(CheckMode.FAILED):
            promoseries = payments_api.Promoseries.create(service=self.service, **test_data.promoseries_params)

        check.check_that(promoseries['series'],
                         deep_contains(expected.Promoseries.created(service=self.service,
                                                                    **test_data.promoseries_params)),
                         step=u'Проверяем корректность созданной промосерии',
                         error=u'Промосерия была создана с некорректными параметрами')

    def test_create_not_unique_promoseries_without_partial_only(self):
        with check_mode(CheckMode.IGNORED):
            resp = payments_api.Promoseries.create(service=self.service,
                                                   **defaults.Promoseries.custom_params({'partial_only': 0,
                                                                                         'usage_limit': 5}))
            check.check_that(resp, deep_contains(expected.Promocode.usage_limit_without_partial_only()),
                             step=u'Проверяем что при попытке создать неуникальный промокод с partial_only=0 '
                                  u'возвращается ошибка',
                             error=u'При попытке создать неуникальный промокод с partial_only=0 '
                                   u'не возвращается ошибка или текст ошибки некорректен')

    def test_get_promoseries_status(self):
        promoseries = payments_api.Promoseries.create(service=self.service, **defaults.Promoseries.base_params)

        with check_mode(CheckMode.FAILED):
            payments_api.Promoseries.get_status(service=self.service, series_id=promoseries['series']['id'])

    @pytest.mark.parametrize('test_data', Data.Promocodes.data_promocodes, ids=DataObject.ids)
    def test_create_promocode(self, test_data):
        promoseries = payments_api.Promoseries.create(service=self.service, **test_data.promoseries_params)
        series_id = promoseries['series']['id']

        with check_mode(CheckMode.FAILED):
            resp = payments_api.Promocodes.create(service=self.service, series_id=series_id,
                                                  **test_data.promocode_params)

        if not test_data.promocode_params.get('code'):
            check.check_that(resp['promocodes'],
                             deep_equals_to([has_length(test_data.promocode_params.get('code_length') or 10)
                                             for _ in range(test_data.promocode_params.get('quantity') or 10)]),
                             step=u'Проверяем что создано корректное число промокодов с корректной длинной промокода',
                             error=u'Создано некорректное число промокодов или длинна промокодов некорректна')
        else:
            check.check_that(resp['promocodes'][0], equal_to(test_data.promocode_params['code']),
                             step=u'Проверяем что создался внешний промокод',
                             error=u'Некорректное создался внешний промокод')

        promocode_result_params = defaults.Promocode.params_with_promoseries_params(test_data.promocode_params,
                                                                                    test_data.promoseries_params)

        reporter.log('RESULT: \n {}'.format(reporter.pformat(promocode_result_params)))

        for promo in resp['promocodes']:
            promo_info = payments_api.Promocodes.get_by_text(service=self.service, text=promo)['result']
            check.check_that(promo_info,
                             deep_contains(expected.Promocode.created(service=self.service, series_id=series_id,
                                                                      **promocode_result_params)),
                             step=u'Проверяем корректность созданного промокода {}'.format(promo),
                             error=u'Промокод {} был создан с некорректными параметрами'.format(promo))

    @pytest.mark.parametrize('test_data', Data.Promocodes.data_promocodes, ids=DataObject.ids)
    def test_create_external_promocode_with_qty_more_than_one(self, test_data):
        promoseries = payments_api.Promoseries.create(service=self.service, **test_data.promoseries_params)

        with check_mode(CheckMode.IGNORED):
            resp = payments_api.Promocodes.create(service=self.service, series_id=promoseries['series']['id'],
                                                  **defaults.Promocode.custom_params({'code': 'promocode code',
                                                                                      'quantity': 5}))

        check.check_that(resp, deep_equals_to(expected.Promocode.external_promocode_with_qty_more_than_one()),
                         step=u'Проверяем что при попытке создать внешний промокод с quantity>1 '
                              u'возвращается ошибка',
                         error=u'При попытке создать внешний промокод с quantity>0 '
                               u'не возвращается ошибка или текст ошибки некорректен')

    def test_promocode_must_be_unique(self):
        promoseries = payments_api.Promoseries.create(service=self.service, **defaults.Promoseries.base_params)

        code = u''.join(random.choice(string.ascii_uppercase + string.digits) for _ in range(10))
        series_id = promoseries['series']['id']

        with reporter.step(u'Дважды создаем промокод с одним и тем же code. '
                           u'Проверяем что на втором вызове происходит ошибка'):
            resp = payments_api.Promocodes.create(service=self.service, series_id=series_id,
                                                  **defaults.Promocode.custom_params({'code': code,
                                                                                      'quantity': 1}))
            promocode = resp['promocodes'][0]

            with check_mode(CheckMode.IGNORED):
                resp = payments_api.Promocodes.create(service=self.service, series_id=series_id,
                                                      **defaults.Promocode.custom_params({'code': code,
                                                                                          'quantity': 1}))

            check.check_that(resp, deep_contains(expected.Promocode.is_not_unique(promocode)),
                             step=u'Проверяем что при попытке создать второй промокод с тем же code возращается ошибка',
                             error=u'Удалось создать два промокода с одинаковым code')

    @pytest.mark.parametrize('test_data', Data.Promocodes.data_promoseries_limits, ids=DataObject.ids)
    def test_promoseries_limit_overflow(self, test_data):
        # Количество промокодов не должно превышать предел промокодов в промосерии
        # Промокодов одновременно нельзя создать более 100
        promoseries_limit, promocodes_quantity = test_data.promoseries_limit, test_data.promocodes_quantity
        promoseries = payments_api.Promoseries.create(service=self.service,
                                                      **defaults.Promoseries.custom_params(
                                                          {'limit': promoseries_limit}))
        with check_mode(CheckMode.IGNORED):
            resp = payments_api.Promocodes.create(service=self.service, series_id=promoseries['series']['id'],
                                                  **defaults.Promocode.custom_params(
                                                      {'quantity': promocodes_quantity}))

        check.check_that(resp, deep_contains(expected.Promocode.quantity_and_limit_error()),
                         step=u'Проверяем что при попытке создать промокодов более чем позволено в серии '
                              u'либо более 100 возникает ошибка',
                         error=u'При попытке создать промокодов более чем позволено в серии '
                               u'либо долее 100 не возникла ошибка или текст ошибки некорректен')

    def test_get_promocode_by_promoseries(self):
        promoseries = payments_api.Promoseries.create(service=self.service, **defaults.Promoseries.base_params)
        payments_api.Promocodes.create(service=self.service, series_id=promoseries['series']['id'])

        with check_mode(CheckMode.FAILED):
            payments_api.Promocodes.get_by_series(service=self.service,
                                                  series_id=promoseries['series']['id'],
                                                  page=1)

    def test_get_promocode_by_id(self):
        # todo: довольно странный кейс, откуда в проде люди получают promocode_id?
        promoseries = payments_api.Promoseries.create(service=self.service, **defaults.Promoseries.base_params)
        promocodes = payments_api.Promocodes.create(service=self.service, series_id=promoseries['series']['id'])

        promocode_id = payments_api.Promocodes.get_by_text(service=self.service,
                                                           text=promocodes['promocodes'][0])['result']['promocode_id']

        with check_mode(CheckMode.FAILED):
            payments_api.Promocodes.get_by_id(service=self.service, id=promocode_id)

    def test_get_promocode_by_text(self):
        promoseries = payments_api.Promoseries.create(service=self.service, **defaults.Promoseries.base_params)
        promocodes = payments_api.Promocodes.create(service=self.service, series_id=promoseries['series']['id'])

        with check_mode(CheckMode.FAILED):
            payments_api.Promocodes.get_by_text(service=self.service, text=promocodes['promocodes'][0])


def _get_random_label():
    return ''.join(random.choice(string.ascii_letters + string.digits) for _ in range(20))


@reporter.feature(features.General.CardsOperations)
@reporter.story(stories.CardsOperations.CardsLabels)
class TestSetCardLabel(object):
    """
    Тесты на простановку сервисных меток
    https://beta.wiki.yandex-team.ru/balance/simple/servicelabels/
    https://st.yandex-team.ru/TRUST-804
    """

    @staticmethod
    def check_label_present(card, user, label, service=Services.DISK):
        with reporter.step(
                u'Проверяем что у карты {} присутствует метка {} в скоупе для сервиса {}'.format(card, label, service)):
            _, paymethods = payments_api.PaymentMethods.get(service, user)
            check.check_that(label, is_in(paymethods[card]['service_labels']))

    @staticmethod
    def check_label_absent(card, user, label, service=Services.STORE):
        with reporter.step(
                u'Проверяем что у карты {} отсутствует метка {} в скоупе для сервиса {}'.format(card, label, service)):
            _, paymethods = payments_api.PaymentMethods.get(service, user)
            if paymethods[card].get('service_labels'):
                check.check_that(label, not_(is_in(paymethods[card]['service_labels'])))

    @staticmethod
    def setting_label(user, card, label=None):
        if not label:
            label = _get_random_label()
        with check_mode(CheckMode.FAILED):
            payments_api.PaymentMethods.set_label(Services.DISK, user, card, label)
        TestSetCardLabel.check_label_present(card, user, label)
        # check label doesn't visible under other service
        TestSetCardLabel.check_label_absent(card, user, label, service=Services.AFISHA_MOVIEPASS)
        return label

    @staticmethod
    def delete_label(user, card, label):
        with check_mode(CheckMode.FAILED):
            payments_api.PaymentMethods.delete_label(Services.DISK, user, card, label)
        TestSetCardLabel.check_label_absent(card, user, label)

    @pytest.fixture
    def user_and_linked_card(self, request):
        """
        Выбираем пользователя и привязываем ему карту
        """
        user = uids.get_random_of(uids.mutable)
        reporter.logger().debug("Choose user: %s" % user)
        card = cards_pool.get_card()
        token = trust.get_auth_token(Auth.get_auth(user), user)['access_token']
        card_id = trust.bind_card(token, card)['payment_method']

        def fin():
            reporter.logger().debug("Finally unbind card")
            trust.process_unbinding(user)

        request.addfinalizer(fin)

        return user, card_id, token, card

    @pytest.mark.parametrize('label', Data.labels, ids=DataObject.ids_custom('label'))
    def test_set_and_delete_label(self, label, user_and_linked_card):
        user, card, _, _ = user_and_linked_card
        self.setting_label(user, card, label)
        self.delete_label(user, card, label)

    def test_delete_label_doesnt_exist(self, user_and_linked_card):
        user, card, _, _ = user_and_linked_card
        label = "This_label_does_not_exist"
        self.delete_label(user, card, label)

    def test_label_preserved_after_rebinding(self, user_and_linked_card):
        user, card_id, token, card = user_and_linked_card
        label = self.setting_label(user, card_id)
        # отвяжем и привяжем карту заново, метка должна сохраниться
        trust.unbind_card(token, card_id)
        card_id = trust.bind_card(token, card)['payment_method']
        TestSetCardLabel.check_label_present(card_id, user, label)
        # ... а после удаления - удалиться
        self.delete_label(user, card_id, label)

    @pytest.fixture
    def two_users_and_linked_card(self, request):
        """
        Выбираем двух пользователей
        К одному из них привязываем карту
        """
        user_1, user_2 = uids.get_random_of(uids.mutable, 2)
        reporter.logger().debug("Choose user_1: %s, user_2: %s" % (user_1, user_2))

        token = trust.get_auth_token(Auth.get_auth(user_1), user_1)['access_token']
        card = trust.bind_card(token, cards_pool.REAL_CARD)['payment_method']

        def fin():
            reporter.logger().debug("Finally unbind card")
            trust.process_unbinding(user_1)

        request.addfinalizer(fin)

        return user_1, user_2, card

    def test_label_preserved_for_other_user(self, two_users_and_linked_card):
        user_1, user_2, card = two_users_and_linked_card
        label = self.setting_label(user_1, card)

        # привяжем карту к другому пользователю
        token = trust.get_auth_token(Auth.get_auth(user_2), user_2)['access_token']
        card = trust.bind_card(token, cards_pool.REAL_CARD)['payment_method']
        # и проверим, что метка сохранилась...
        TestSetCardLabel.check_label_present(card, user_2, label)
        self.delete_label(user_2, card, label)

    @pytest.fixture
    def user_and_unbinded_card(self):
        """
        Привязываем к пользователю карту и сразу же отвязываем
        а затем в тесте пытаемся проставить метку удаленной карте
        """
        user = uids.get_random_of(uids.mutable)
        reporter.logger().debug("Choose user: %s" % user)

        token = trust.get_auth_token(Auth.get_auth(user), user)['access_token']
        card = trust.bind_card(token, cards_pool.REAL_CARD)['payment_method']
        trust.process_unbinding(user)

        return user, card

    def test_user_does_not_have_card(self, user_and_unbinded_card):
        user, card = user_and_unbinded_card
        label = _get_random_label()
        with check_mode(CheckMode.IGNORED):
            resp = payments_api.PaymentMethods.set_label(Services.DISK, user, card, label)
        check.check_that([resp['status'], resp['status_desc']],
                         contains(u'error', u'User {} doesn`t have card ''{}'.format(user.uid,
                                                                                     card.replace('card-', ''))),
                         step=u'Проверяем, что при попытке поставить метку отвязаной карте получаем верную ошибку',
                         error=u'При попытке поставить метку отвязаной карте получили неверную ошибку')

    @pytest.mark.parametrize('label', Data.negative_labels, ids=DataObject.ids_custom('label'))
    def test_set_banned_label(self, label, user_and_linked_card):
        user, card, _, _ = user_and_linked_card
        with check_mode(CheckMode.IGNORED):
            resp = payments_api.PaymentMethods.set_label(Services.DISK, user, card, label)
            check.check_that([resp['status'], resp['service_code']], contains(u'error', u'invalid_params'),
                             step=u'Проверяем, что при попытке поставить метку с запрещенными '
                                  u'символами получаем ошибку',
                             error=u'При попытке поставить метку с запрещеннными символами не получаем ошибку!')
        TestSetCardLabel.check_label_absent(card, user, label, Services.DISK)


if __name__ == '__main__':
    pytest.main()
