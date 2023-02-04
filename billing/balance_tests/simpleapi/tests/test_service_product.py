# coding=utf-8
import pytest

import btestlib.reporter as reporter
from btestlib.constants import Services
from btestlib.utils import CheckMode, check_mode
from simpleapi.common import logger
from simpleapi.common.utils import DataObject
from simpleapi.data import defaults
from simpleapi.data import features, stories
from simpleapi.data import marks
from simpleapi.data import uids_pool as uids
from simpleapi.matchers.deep_equals import deep_equals_to, deep_contains
from simpleapi.steps import check_steps as check
from simpleapi.steps import db_steps
from simpleapi.steps import expected_steps as expected
from simpleapi.steps import payments_api_steps as payments
from simpleapi.steps import simple_steps as simple

pytestmark = marks.simple_internal_logic

'''
https://st.yandex-team.ru/TRUST-775
'''


class PartnerException(Exception):
    pass


@reporter.feature(features.General.ServiceProduct)
class TestServiceProductsXMLRPC(object):
    pytestmark = marks.xmlrpc_api

    @reporter.story(stories.ServiceProduct.GeneralRules)
    @pytest.mark.parametrize("service", [Services.TICKETS,
                                         Services.TAXI,
                                         Services.DOSTAVKA],
                             ids=DataObject.ids_service)
    def test_partner_id_cannot_be_changed(self, service):
        service_product_id = simple.get_service_product_id()
        _, first_partner = simple.create_partner(service)
        _, second_partner = simple.create_partner(service)

        with check_mode(CheckMode.FAILED):
            simple.create_service_product(service,
                                          service_product_id,
                                          partner_id=first_partner)
        with check_mode(CheckMode.IGNORED):
            resp = simple.create_service_product(service,
                                                 service_product_id,
                                                 partner_id=second_partner)
            check.check_that(resp,
                             deep_contains(expected.ServiceProduct.partner_id_cannot_be_changed()),
                             step=u'Проверяем что при создании сервисного продукта для сервиса {} '
                                  u'partner_id не может быть изменен'.format(service),
                             error=u'При попытке изменить partner_id для сервиса {} '
                                   u'не вернулась ошибка'.format(service))

    @reporter.story(stories.ServiceProduct.GeneralRules)
    @pytest.mark.parametrize("service", [Services.MARKETPLACE,
                                         Services.NEW_MARKET],
                             ids=DataObject.ids_service)
    def test_partner_id_cannot_be_changed_if_payment_exists(self, service):
        """TRUST-3283"""
        user = uids.get_random_of(uids.all_)
        _, partner = simple.create_partner(service)

        with check_mode(CheckMode.FAILED):
            service_product_id = simple.create_service_product_for_service(service)

            orders = simple.form_orders_for_create(service, user, service_product_id=service_product_id)
            simple.process_payment(service, user, orders=orders, need_postauthorize=True)
        with check_mode(CheckMode.IGNORED):
            resp = simple.create_service_product(service,
                                                 service_product_id,
                                                 partner_id=partner)
            check.check_that(resp,
                             deep_equals_to(expected.ServiceProduct.service_product_already_has_payments()),
                             step=u'Проверяем что для сервиса {} если уже существую платежи по сервисному продукту, '
                                  u'то partner_id не может быть изменен'.format(service),
                             error=u'При попытке изменить partner_id в продукте, у которого уже есть платежи'
                                   u' для сервиса {} не вернулась ошибка'.format(service))

    @reporter.story(stories.ServiceProduct.GeneralRules)
    @pytest.mark.parametrize("service", [
        # marks.store(Services.STORE),
        # Services.PARKOVKI,
        Services.MARKETPLACE,
        Services.NEW_MARKET
    ],
                             ids=DataObject.ids_service)
    def test_partner_id_can_be_changed(self, service):
        service_product = simple.get_service_product_id()
        _, first_partner = simple.create_partner(service)
        _, second_partner = simple.create_partner(service)

        with check_mode(CheckMode.FAILED):
            simple.create_service_product(service,
                                          service_product,
                                          partner_id=first_partner)
            simple.create_service_product(service,
                                          service_product,
                                          partner_id=second_partner)

    @marks.not_for_green_line
    @reporter.story(stories.ServiceProduct.GeneralRules)
    @pytest.mark.parametrize("service", [Services.STORE, ],
                             ids=DataObject.ids_service)
    def test_partner_id_can_be_changed_if_payment_exists(self, service):
        """TRUST-3283"""
        service_product = simple.get_service_product_id()
        user = uids.get_random_of(uids.all_)
        _, first_partner = simple.create_partner(service)
        _, second_partner = simple.create_partner(service)

        simple.create_service_product(service,
                                      service_product,
                                      partner_id=first_partner)
        orders = simple.form_orders_for_create(service, user, service_product_id=service_product)
        simple.process_payment(service, user, orders=orders)

        with check_mode(CheckMode.FAILED):
            simple.create_service_product(service,
                                          service_product,
                                          partner_id=second_partner)

    @reporter.story(stories.ServiceProduct.CreationRules)
    @pytest.mark.parametrize("service", [Services.SHAD,
                                         Services.DISK,
                                         Services.YAC,
                                         Services.REALTYPAY],
                             ids=DataObject.ids_service)
    def test_products_cannot_be_with_partner(self, service):
        service_product_1st = simple.get_service_product_id()
        service_product_2nd = str(int(service_product_1st) + 1)

        with check_mode(CheckMode.IGNORED):
            resp = simple.create_service_product(service,
                                                 service_product_1st,
                                                 partner_id='1')
            check.check_that(resp,
                             deep_contains(expected.ServiceProduct.partner_for_the_product_is_forbidden()),
                             step=u'Проверяем что при создании сервисного продукта для сервиса {} '
                                  u'нельзя передавать partner_id'.format(service),
                             error=u'При попытке создать продукт с partner_id для сервиса {} '
                                   u'не вернулась ошибка'.format(service))

        with check_mode(CheckMode.FAILED):
            simple.create_service_product(service, service_product_2nd)

    @reporter.story(stories.ServiceProduct.CreationRules)
    @pytest.mark.parametrize("service", [
        # marks.store(Services.STORE),
        # Services.PARKOVKI,
        Services.TICKETS,
        Services.MARKETPLACE,
        Services.TAXI,
        Services.DOSTAVKA],
                             ids=DataObject.ids_service)
    def test_products_cannot_be_without_partner(self, service):
        service_product_1st = simple.get_service_product_id()
        service_product_2nd = str(int(service_product_1st) + 1)

        _, partner = simple.create_partner(service)

        with check_mode(CheckMode.FAILED):
            simple.create_service_product(service,
                                          service_product_1st,
                                          partner_id=partner)

        with check_mode(CheckMode.IGNORED):
            resp = simple.create_service_product(service, service_product_2nd)
            check.check_that(resp,
                             deep_contains(expected.ServiceProduct.product_requires_partner()),
                             step=u'Проверяем что при создании сервисного продукта для сервиса {} '
                                  u'обязательно нужно передавать partner_id'.format(service),
                             error=u'При попытке создать продукт без partner_id для сервиса {} '
                                   u'не вернулась ошибка'.format(service))

    @reporter.story(stories.ServiceProduct.ExportBsBo)
    @pytest.mark.parametrize("service", [Services.DISK,
                                         Services.SHAD,
                                         Services.YAC,
                                         Services.REALTYPAY],
                             ids=DataObject.ids_service)
    def test_load_product_without_partner(self, service):
        service_product_id = simple.get_service_product_id()
        with check_mode(CheckMode.FAILED):
            simple.create_service_product(service, service_product_id)

        bs_product = db_steps.bs().get_product_by_external_id(service_product_id, service)
        bo_product = db_steps.bo().get_product_by_external_id(service_product_id, service)

        assert bs_product, 'Error: product has not been created in BS scheme'
        assert bo_product, 'Error: product has not been created in BO scheme'

        check.check_iterable_contains(bs_product,
                                      must_contains=['external_id', 'name', ])
        check.check_dicts_equals(bs_product, bo_product)

    @reporter.story(stories.ServiceProduct.ExportBsBo)
    @pytest.mark.parametrize("service", [
        # marks.store(Services.STORE),
        # Services.PARKOVKI,
        Services.TICKETS,
        Services.MARKETPLACE,
        Services.TAXI,
        Services.DOSTAVKA],
                             ids=DataObject.ids_service)
    def test_load_product_with_partner(self, service):
        service_product_id = simple.get_service_product_id()
        _, partner = simple.create_partner(service)
        with check_mode(CheckMode.FAILED):
            simple.create_service_product(service, service_product_id, partner)

        bs_product = db_steps.bs_or_ng_by_service(service).get_product_by_external_id(service_product_id, service)
        bo_product = db_steps.bo().get_product_by_external_id(service_product_id, service)

        assert bs_product, 'Error: product has not been created in BS scheme'
        assert bo_product, 'Error: product has not been created in BO scheme'

        check.check_iterable_contains(bs_product,
                                      must_contains=['external_id', 'name', 'partner_id', ])
        check.check_dicts_equals(bs_product, bo_product)

    @reporter.story(stories.ServiceProduct.IntroductorySubsRules)
    @pytest.mark.parametrize("service", [Services.MUSIC], ids=DataObject.ids_service)
    def test_product_with_introductory_period_should_be_single_purchased(self, service):
        product_type = defaults.ServiceProduct.Subscription.INTRODUCTORY.copy()
        product_type.update({'single_purchase': 0})

        with check_mode(CheckMode.IGNORED):
            resp = simple.create_service_product(service, **product_type)
            check.check_that(resp,
                             deep_equals_to(expected.ServiceProduct.introductory_period_should_be_single_purchased()),
                             step=u'Проверяем что при создании introductory-продукта с single_purchase=0 '
                                  u'возвращается ошибка',
                             error=u'При создании introductory-продукта с single_purchase=0 не вернулась ошибка')

    @reporter.story(stories.ServiceProduct.IntroductorySubsRules)
    @pytest.mark.parametrize("service", [Services.MUSIC], ids=DataObject.ids_service)
    def test_product_with_introductory_period_without_price(self, service):
        """
        При создании продукта должны либо присутствовать оба поля
        subs_introductory_period и subs_introductory_period_prices, или оба отсутствовать.
        https://wiki.yandex-team.ru/users/goris/billing/introductoryperiod/
        """
        product_type = defaults.ServiceProduct.Subscription.INTRODUCTORY.copy()
        product_type.pop('subs_introductory_period_prices')

        with check_mode(CheckMode.IGNORED):
            resp = simple.create_service_product(service, **product_type)
            check.check_that(resp,
                             deep_equals_to(expected.ServiceProduct.missing_subs_introductory_period_prices()),
                             step=u'Проверяем что если передан subs_introductory_period, '
                                  u'но не передан subs_introductory_period_prices, то возвращается ошибка',
                             error=u'При создании продукта с переданным subs_introductory_period, '
                                   u'но не переданным subs_introductory_period_prices не вернулась ошибка')

    @reporter.story(stories.ServiceProduct.IntroductorySubsRules)
    @pytest.mark.parametrize("service", [Services.MUSIC], ids=DataObject.ids_service)
    def test_product_with_introductory_period_price_without_period(self, service):
        """
        При создании продукта должны либо присутствовать оба поля
        subs_introductory_period и subs_introductory_period_prices, или оба отсутствовать.
        https://wiki.yandex-team.ru/users/goris/billing/introductoryperiod/
        """
        product_type = defaults.ServiceProduct.Subscription.INTRODUCTORY.copy()
        product_type.pop('subs_introductory_period')

        with check_mode(CheckMode.IGNORED):
            resp = simple.create_service_product(service, **product_type)
            check.check_that(resp,
                             deep_equals_to(expected.ServiceProduct.missing_subs_introductory_period()),
                             step=u'Проверяем что если передан subs_introductory_period_prices, '
                                  u'но не передан subs_introductory_period, то возвращается ошибка',
                             error=u'При создании продукта с переданным subs_introductory_period_prices, '
                                   u'но не переданным subs_introductory_period не вернулась ошибка')

    @reporter.story(stories.ServiceProduct.IntroductorySubsRules)
    @pytest.mark.parametrize("service", [Services.MUSIC], ids=DataObject.ids_service)
    def test_introductory_period_prices_should_have_the_same_structure_as_prices(self, service):
        """
        subs_introductory_period_prices в таком же формате, что и prices.
        Причем для каждого объекта из prices нужно соответствующее значение из subs_introductory_period_prices.
        https://wiki.yandex-team.ru/users/goris/billing/introductoryperiod/
        """
        prices = [{'region_id': 225, 'dt': 1347521693, 'price': '10', 'currency': 'RUB'},
                  {'region_id': 84, 'dt': 1327521693, 'price': '10', 'currency': 'USD'}, ]
        prices_introductory = [{'region_id': 225, 'dt': 1347521693, 'price': '5', 'currency': 'RUB'}, ]

        product_type = defaults.ServiceProduct.Subscription.INTRODUCTORY.copy()
        product_type.update({'prices': prices,
                             'subs_introductory_period_prices': prices_introductory})

        with check_mode(CheckMode.IGNORED):
            resp = simple.create_service_product(service, **product_type)
            check.check_that(resp,
                             deep_equals_to(
                                 expected.ServiceProduct.introductory_period_prices_inconsistent_with_prices()),
                             step=u'Проверяем что для каждого объекта из prices '
                                  u'нужно соответствующее значение из subs_introductory_period_prices',
                             error=u'Структуры prices и subs_introductory_period_prices имеют разное содержимое, '
                                   u'но ошибка не вернулась')

    @reporter.story(stories.Subscriptions.AggregatedCharging)
    @pytest.mark.parametrize("service", [
        Services.DISK,
        Services.MUSIC
    ],
                             ids=DataObject.ids_service)
    def test_product_with_aggregated_charging_should_be_subs(self, service):
        with check_mode(CheckMode.IGNORED):
            resp = simple.create_service_product(service, aggregated_charging=1)
        check.check_that(resp,
                         deep_contains(expected.ServiceProduct.aggregated_charging_only_for_subs()),
                         step=u'Проверяем что при попытке создать НЕ подписочный продукт '
                              u'с aggregated_charging возвращается говорящая ошибка',
                         error=u'При попытке создать НЕ подписочный продукт с aggregated_charging '
                               u'вернулась некорректная ошибка или вовсе не было ошибки')


@reporter.feature(features.General.ServiceProduct)
class TestServiceProductsRest(object):
    pytestmark = marks.xmlrpc_api

    @reporter.story(stories.ServiceProduct.GeneralRules)
    @pytest.mark.parametrize("service", [Services.TICKETS,
                                         Services.TAXI,
                                         Services.DOSTAVKA],
                             ids=DataObject.ids_service)
    def test_partner_id_cannot_be_changed_via_create(self, service):
        user = uids.get_random_of(uids.all_)
        service_product_id = simple.get_service_product_id()
        first_partner = payments.Partners.create(service, user=user)['partner_id']
        second_partner = payments.Partners.create(service, user=user)['partner_id']

        with check_mode(CheckMode.FAILED):
            payments.Products.create_for_service(service=service, user=user,
                                                 product_id=service_product_id, partner_id=first_partner)
        with check_mode(CheckMode.IGNORED):
            resp, _ = payments.Products.create_for_service(service=service, user=user,
                                                           product_id=service_product_id,
                                                           partner_id=second_partner, extended_response=True)
            check.check_that(resp,
                             deep_contains(expected.ServiceProduct.partner_id_cannot_be_changed()),
                             step=u'Проверяем что при создании сервисного продукта для сервиса {} '
                                  u'partner_id не может быть изменен'.format(service),
                             error=u'При попытке изменить partner_id для сервиса {} '
                                   u'не вернулась ошибка'.format(service))

    @reporter.story(stories.ServiceProduct.GeneralRules)
    @pytest.mark.parametrize("service", [Services.TICKETS,
                                         Services.TAXI,
                                         Services.DOSTAVKA],
                             ids=DataObject.ids_service)
    def test_partner_id_cannot_be_changed_via_update(self, service):
        user = uids.get_random_of(uids.all_)
        service_product_id = simple.get_service_product_id()
        first_partner = payments.Partners.create(service, user=user)['partner_id']
        second_partner = payments.Partners.create(service, user=user)['partner_id']

        with check_mode(CheckMode.FAILED):
            _, product = payments.Products.create_for_service(service=service, user=user,
                                                              product_id=service_product_id,
                                                              partner_id=first_partner,
                                                              extended_response=True)
        with check_mode(CheckMode.IGNORED):
            resp, _ = payments.Products.update_for_service(service=service, user=user, name=product['name'],
                                                           product_id=service_product_id, partner_id=second_partner)
            check.check_that(resp,
                             deep_contains(expected.ServiceProduct.partner_id_cannot_be_changed()),
                             step=u'Проверяем что при создании сервисного продукта для сервиса {} '
                                  u'partner_id не может быть изменен'.format(service),
                             error=u'При попытке изменить partner_id для сервиса {} '
                                   u'не вернулась ошибка'.format(service))

    @reporter.story(stories.ServiceProduct.GeneralRules)
    @pytest.mark.parametrize("service", [
        # marks.store(Services.STORE),
        Services.MARKETPLACE,
        Services.NEW_MARKET
    ],
                             ids=DataObject.ids_service)
    def test_partner_id_can_be_changed_via_create(self, service):
        user = uids.get_random_of(uids.all_)
        service_product_id = simple.get_service_product_id()
        first_partner = payments.Partners.create(service, user=user)['partner_id']
        second_partner = payments.Partners.create(service, user=user)['partner_id']

        with check_mode(CheckMode.FAILED):
            payments.Products.create_for_service(service=service, user=user,
                                                 product_id=service_product_id, partner_id=first_partner)
            payments.Products.create_for_service(service=service, user=user,
                                                 product_id=service_product_id, partner_id=second_partner)

    @reporter.story(stories.ServiceProduct.GeneralRules)
    @pytest.mark.parametrize("service", [
        # marks.store(Services.STORE),
        Services.MARKETPLACE,
        Services.NEW_MARKET
    ],
                             ids=DataObject.ids_service)
    def test_partner_id_can_be_changed_via_update(self, service):
        user = uids.get_random_of(uids.all_)
        service_product_id = simple.get_service_product_id()
        first_partner = payments.Partners.create(service, user=user)['partner_id']
        second_partner = payments.Partners.create(service, user=user)['partner_id']

        with check_mode(CheckMode.FAILED):
            _, product = payments.Products.create_for_service(service=service, user=user,
                                                              product_id=service_product_id,
                                                              partner_id=first_partner,
                                                              extended_response=True)
            payments.Products.update_for_service(service=service, user=user, name=product['name'],
                                                 product_id=service_product_id, partner_id=second_partner)

    @reporter.story(stories.ServiceProduct.CreationRules)
    @pytest.mark.parametrize("service", [Services.SHAD,
                                         Services.DISK,
                                         Services.YAC,
                                         Services.REALTYPAY
                                         ],
                             ids=DataObject.ids_service)
    def test_products_cannot_be_with_partner(self, service):
        user = uids.get_random_of(uids.all_)
        service_product_1st = simple.get_service_product_id()
        service_product_2nd = str(int(service_product_1st) + 1)

        with check_mode(CheckMode.IGNORED):
            resp, _ = payments.Products.create_for_service(service=service, user=user,
                                                           product_id=service_product_1st, partner_id='1',
                                                           extended_response=True)
            check.check_that(resp,
                             deep_contains(expected.ServiceProduct.partner_for_the_product_is_forbidden()),
                             step=u'Проверяем что при создании сервисного продукта для сервиса {} '
                                  u'нельзя передавать partner_id'.format(service),
                             error=u'При попытке создать продукт с partner_id для сервиса {} '
                                   u'не вернулась ошибка'.format(service))

        with check_mode(CheckMode.FAILED):
            payments.Products.create_for_service(service=service, user=user,
                                                 product_id=service_product_2nd)

    @reporter.story(stories.ServiceProduct.CreationRules)
    @pytest.mark.parametrize("service", [
        # marks.store(Services.STORE),
        Services.TICKETS,
        Services.MARKETPLACE,
        Services.TAXI,
        Services.DOSTAVKA
    ],
                             ids=DataObject.ids_service)
    def test_products_cannot_be_without_partner(self, service):
        user = uids.get_random_of(uids.all_)
        service_product_1st = simple.get_service_product_id()
        service_product_2nd = str(int(service_product_1st) + 1)

        partner = payments.Partners.create(service, user=user)['partner_id']

        with check_mode(CheckMode.FAILED):
            payments.Products.create_for_service(service=service, user=user,
                                                 product_id=service_product_1st, partner_id=partner)

        with check_mode(CheckMode.IGNORED):
            resp, _ = payments.Products.create_for_service(service=service, user=user,
                                                           product_id=service_product_2nd, no_partner=True,
                                                           extended_response=True)
            check.check_that(resp,
                             deep_contains(expected.ServiceProduct.product_requires_partner()),
                             step=u'Проверяем что при создании сервисного продукта для сервиса {} '
                                  u'обязательно нужно передавать partner_id'.format(service),
                             error=u'При попытке создать продукт без partner_id для сервиса {} '
                                   u'не вернулась ошибка'.format(service))

    @reporter.story(stories.Subscriptions.AggregatedCharging)
    @pytest.mark.parametrize("service", [
        Services.DISK,
        Services.MUSIC
    ],
                             ids=DataObject.ids_service)
    def test_product_with_aggregated_charging_should_be_subs(self, service):
        user = uids.get_random_of(uids.all_)
        product_type = defaults.PaymentApi.Product.app.copy()
        product_type.update({'aggregated_charging': 1})
        with check_mode(CheckMode.IGNORED):
            resp, _ = payments.Products.create_for_service(service=service, user=user,
                                                           product_type=product_type,
                                                           extended_response=True)
        check.check_that(resp,
                         deep_contains(expected.ServiceProduct.aggregated_charging_only_for_subs()),
                         step=u'Проверяем что при попытке создать НЕ подписочный продукт '
                              u'с aggregated_charging возвращается говорящая ошибка',
                         error=u'При попытке создать НЕ подписочный продукт с aggregated_charging '
                               u'вернулась некорректная ошибка или вовсе не было ошибки')


if __name__ == '__main__':
    pytest.main()
