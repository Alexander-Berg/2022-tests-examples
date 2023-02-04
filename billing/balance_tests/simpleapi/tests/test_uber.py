# coding: utf-8
import pytest
from hamcrest import has_key, has_entries, not_, is_in

import btestlib.reporter as reporter
from btestlib.constants import Services
from btestlib.utils import CheckMode, check_mode
from simpleapi.common.payment_methods import UberForwardingCard, UberRoamingCard, Compensation
from simpleapi.common.utils import DataObject
from simpleapi.data import defaults
from simpleapi.data import features, stories, marks
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import get_card, RBS
from simpleapi.matchers.deep_equals import deep_contains
from simpleapi.steps import card_forwarding_api_steps as cards_steps
from simpleapi.steps import check_steps as check
from simpleapi.steps import db_steps
from simpleapi.steps import expected_steps as expected
from simpleapi.steps import simple_steps as simple
from simpleapi.steps import trust_steps as trust
from simpleapi.steps import uber_steps as uber

__author__ = 'fellow'

pytestmark = marks.uber

user_type = uids.Types.uber
PAYMENTS_COUNT = 3  # count of payment in multipayment case


class Data(object):
    test_data_binding = [
        DataObject(region_id='RU', card=get_card()).new(expected_region_id='225'),
        DataObject(region_id='BY', card=get_card()).new(expected_region_id='149'),
        DataObject(region_id='KZ', card=RBS.Success.Without3DS.card_visa).new(expected_region_id='159'),
        DataObject(region_id='AZ', card=get_card()).new(expected_region_id='167'),
    ]
    test_data_forwarding = [
        DataObject(paymethod=UberForwardingCard(card=get_card()), country_data=defaults.CountryData.Russia),
        DataObject(paymethod=Compensation(), country_data=defaults.CountryData.Russia),
    ]
    test_data_roaming_card = [
        DataObject(paymethod=UberRoamingCard(), country_data=defaults.CountryData.Russia),
        DataObject(paymethod=UberRoamingCard(), country_data=defaults.CountryData.Belarus),
        DataObject(paymethod=UberRoamingCard(), country_data=defaults.CountryData.Kazakhstan),
        DataObject(paymethod=UberRoamingCard(), country_data=defaults.CountryData.Azerbaijan),
    ]
    test_data_roaming_compensation = [
        DataObject(paymethod=Compensation(), country_data=defaults.CountryData.Russia),
        DataObject(paymethod=Compensation(), country_data=defaults.CountryData.Kazakhstan),
    ]
    test_data_roaming_failed = [
        DataObject(paymethod=UberRoamingCard(is_valid=False), country_data=defaults.CountryData.Russia),
        DataObject(paymethod=UberRoamingCard(is_valid=False), country_data=defaults.CountryData.Belarus),
        DataObject(paymethod=UberRoamingCard(is_valid=False), country_data=defaults.CountryData.Kazakhstan),
        DataObject(paymethod=UberRoamingCard(is_valid=False), country_data=defaults.CountryData.Azerbaijan),
    ]
    orders_structure = [
        defaults.Order.structure_rub_one_order,
        # DataObject(orders_structure=defaults.Order.structure_rub_two_orders),
    ]


@reporter.feature(features.Service.Uber)
class TestScenario3(object):
    service = Services.UBER
    story_prefix = 'Scenario3 '

    @pytest.fixture
    def user_without_linked_cards(self, request):
        def get_user(user_type, payment_method_id, card=None, region_id=None):
            with reporter.step(u'Выбираем случайного пользователя для теста и отвязываем от него все привязанне карты'):
                user = uids.get_random_of_type(user_type)

                cards_steps.Binding.unbind_all_cards_of(user, self.service)

            if card:
                with reporter.step(u'Привязываем пользователю карту {} перед началом теста'.format(payment_method_id)):
                    cards_steps.Binding.create(card=card, user=user,
                                               payment_method_id=payment_method_id, region_name=region_id)

            def fin():
                with reporter.step(u'Отвязываем карту {} в конце теста, '
                                   u'если она не была отвязана'.format(payment_method_id)), \
                     check_mode(CheckMode.IGNORED):
                    cards_steps.Binding.unbind(user=user, payment_method_id=payment_method_id)

            request.addfinalizer(fin)

            return user

        return get_user

    @reporter.story(story_prefix + stories.CardsOperations.CardsBinding)
    @pytest.mark.parametrize('test_data', Data.test_data_binding, ids=DataObject.ids)
    def test_bind_unbind(self, test_data, user_without_linked_cards):
        region_id, card, expected_region_id = test_data.region_id, test_data.card, \
                                           test_data.expected_region_id
        payment_method_id = cards_steps.gen_uber_payment_method_id()
        user = user_without_linked_cards(user_type, payment_method_id)
        uber_oauth_token = uber.Authorization.get_token_for(user)

        _, linked_payment_methods = simple.list_payment_methods(service=self.service, user=user,
                                                                uber_oauth_token=uber_oauth_token)
        with check_mode(CheckMode.FAILED):
            cards_steps.Binding.create(card=card, user=user, payment_method_id=payment_method_id, region_name=region_id)

        _, linked_payment_methods = simple.list_payment_methods(service=self.service, user=user,
                                                                uber_oauth_token=uber_oauth_token)

        check.check_that(linked_payment_methods,
                         has_key(cards_steps.format_payment_method_id_to_card_id(payment_method_id)),
                         step=u'Проверяем что привязанная карта появилась в ответе ListPaymentMethods',
                         error=u'Привязанная карта отсутствует в ответе ListPaymentMethods')
        check.check_that(linked_payment_methods.get(cards_steps.format_payment_method_id_to_card_id(payment_method_id)),
                         deep_contains(expected.Binding.binded_card(card, region=expected_region_id)),
                         step=u'Проверяем параметры привязанной карты',
                         error=u'Параметры привязанной карты некорректны')

        with check_mode(CheckMode.FAILED):
            cards_steps.Binding.unbind(payment_method_id=payment_method_id, user=user)

    @pytest.mark.skipif(True, reason="Doesn't work now")
    @reporter.story(story_prefix + stories.CardsOperations.CardsUpdating)
    @pytest.mark.parametrize('test_data', Data.test_data_binding, ids=DataObject.ids)
    def test_update_card(self, test_data, user_without_linked_cards):
        region_id, card, expected_region_id = test_data.region_id, test_data.card, \
                                           test_data.expected_region_id
        payment_method_id = cards_steps.gen_uber_payment_method_id()
        user = user_without_linked_cards(user_type, payment_method_id, card, region_id)
        uber_oauth_token = uber.Authorization.get_token_for(user)

        data_to_update = {'expiration_month': '02',
                          'expiration_year': '2022'}

        with check_mode(CheckMode.FAILED):
            cards_steps.Binding.update(user=user, payment_method_id=payment_method_id, **data_to_update)

        _, linked_payment_methods = simple.list_payment_methods(service=self.service, user=user,
                                                                uber_oauth_token=uber_oauth_token)

        check.check_that(linked_payment_methods,
                         has_key(cards_steps.format_payment_method_id_to_card_id(payment_method_id)),
                         step=u'Проверяем что привязанная карта появилась в ответе ListPaymentMethods',
                         error=u'Привязанная карта отсутствует в ответе ListPaymentMethods')

        check.check_that(linked_payment_methods.get(cards_steps.format_payment_method_id_to_card_id(payment_method_id)),
                         deep_contains(expected.Binding.binded_card(card, region=expected_region_id,
                                                                    **data_to_update)),
                         step=u'Проверяем параметры привязанной карты',
                         error=u'Параметры привязанной карты некорректны')

    @reporter.story(story_prefix + stories.CardsOperations.CardsBinding)
    @pytest.mark.parametrize('test_data', Data.test_data_binding, ids=DataObject.ids)
    def test_list_payment_methods_with_uber_token(self, test_data, user_without_linked_cards):
        region_id, card = test_data.region_id, test_data.card
        payment_method_id = cards_steps.gen_uber_payment_method_id()
        user = user_without_linked_cards(user_type, payment_method_id)
        uber_oauth_token = uber.Authorization.get_token_for(user)

        with check_mode(CheckMode.FAILED):
            cards_steps.Binding.create(card=card, user=user, payment_method_id=payment_method_id, region_name=region_id)

        _, linked_payment_methods = simple.list_payment_methods(service=self.service, user=user,
                                                                uber_oauth_token=uber_oauth_token)

        check.check_that(linked_payment_methods,
                         has_key(cards_steps.format_payment_method_id_to_card_id(payment_method_id)),
                         step=u'Проверяем что привязанная карта появилась в ответе ListPaymentMethods',
                         error=u'Привязанная карта отсутствует в ответе ListPaymentMethods')

        with check_mode(CheckMode.FAILED):
            cards_steps.Binding.unbind(payment_method_id=payment_method_id, user=user)

    @reporter.story(story_prefix + stories.CardsOperations.CardsBinding)
    @pytest.mark.parametrize('test_data', Data.test_data_binding, ids=DataObject.ids)
    def test_list_payment_methods_without_uber_token(self, test_data, user_without_linked_cards):
        region_id, card = test_data.region_id, test_data.card
        payment_method_id = cards_steps.gen_uber_payment_method_id()
        user = user_without_linked_cards(user_type, payment_method_id)

        with check_mode(CheckMode.FAILED):
            cards_steps.Binding.create(card=card, user=user, payment_method_id=payment_method_id,
                                       region_name=region_id)

        _, linked_payment_methods = simple.list_payment_methods(service=self.service, user=user)

        check.check_that(linked_payment_methods,
                         not_(has_key(cards_steps.format_payment_method_id_to_card_id(payment_method_id))),
                         step=u'Проверяем что привязанная карта не появилась в ответе ListPaymentMethods',
                         error=u'Привязанная карта присутствует в ответе ListPaymentMethods')

        with check_mode(CheckMode.FAILED):
            cards_steps.Binding.unbind(payment_method_id=payment_method_id, user=user)

    @reporter.story(story_prefix + stories.General.Payment)
    @pytest.mark.parametrize('test_data', Data.test_data_forwarding, ids=DataObject.ids)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure, ids=DataObject.ids)
    def test_base_multipayment_cycle_with_refund(self, test_data, orders_structure):
        paymethod, country = test_data.paymethod, test_data.country_data
        user = uids.get_random_of_type(user_type)
        uber_oauth_token = uber.Authorization.get_token_for(user)
        user_ip, currency, region_id = country['user_ip'], country['currency'], country['region_id']
        paymethod.init(self.service, user, uber_oauth_token=uber_oauth_token)
        orders = simple.form_orders_for_create(self.service, user, orders_structure)
        payments = []
        for _ in range(PAYMENTS_COUNT):
            with check_mode(CheckMode.FAILED):
                basket = simple.create_basket(self.service, user=user,
                                              orders=orders,
                                              paymethod_id=paymethod.id, currency=currency)
                payment_form = simple.pay_basket(self.service, user=user,
                                                 trust_payment_id=basket['trust_payment_id']).get('payment_form')
                trust.pay_by(paymethod, self.service, user=user, payment_form=payment_form,
                             purchase_token=basket['purchase_token'])

                simple.wait_until_payment_done(self.service, user=user,
                                               purchase_token=basket['purchase_token'])

                payments.append(basket['trust_payment_id'])

        # клирим все платежи
        for payment in payments:
            orders_for_update = simple.form_orders_for_update(orders)
            simple.update_basket(self.service, orders=orders_for_update, user=user,
                                 trust_payment_id=payment)
            simple.wait_until_real_postauth(self.service, user=user,
                                            trust_payment_id=payment)

        # а затем рефандим все платежи
        for payment in payments:
            basket = simple.check_basket(self.service, user=user, trust_payment_id=payment)
            simple.process_refund(self.service, trust_payment_id=basket['trust_payment_id'],
                                  basket=basket, user=user)

        for payment in payments:
            simple.check_basket(self.service, user=user, trust_payment_id=payment)


@reporter.feature(features.Service.Uber)
class TestScenario5(object):
    service = Services.UBER_ROAMING
    story_prefix = 'Scenario5 '

    def check_label_present(self, card_id, user, label):
        with reporter.step(
                u'Проверяем что у карты {} присутствует метка {}'.format(card_id, label)):
            uber_oauth_token = uber.Authorization.get_token_for(user)
            _, paymethods = simple.list_payment_methods(self.service, user, uber_oauth_token=uber_oauth_token)
            check.check_that(label, is_in(paymethods.get(card_id).get('service_labels', list())),
                             error=u'У карты {} отсутвтует метка {}'.format(card_id, label))

    def check_label_absent(self, card_id, user, label):
        with reporter.step(
                u'Проверяем что у карты {} отсутствует метка {}'.format(card_id, label)):
            uber_oauth_token = uber.Authorization.get_token_for(user)
            _, paymethods = simple.list_payment_methods(self.service, user, uber_oauth_token=uber_oauth_token)
            check.check_that(label, not_(is_in(paymethods.get(card_id).get('service_labels', list()))),
                             error=u'У карты {} присуттсвует метка {} хотя ее отвязали'.format(card_id, label))

    @reporter.story(story_prefix + stories.CardsOperations.CardsLabels)
    def test_set_card_label(self):
        user = uids.get_random_of_type(user_type)
        card_id = 'card-e:uber:valid'
        label = 'some_test_label'

        simple.set_card_label(service=self.service, user=user, card=card_id,
                              label=label, action=None)

        self.check_label_present(card_id=card_id, user=user, label=label)

        simple.set_card_label(service=self.service, user=user, card=card_id,
                              label=label, action='delete')

        self.check_label_absent(card_id=card_id, user=user, label=label)

    @reporter.story(story_prefix + stories.General.Payment)
    def test_payment_refund(self):
        """
        Refund у Uber_Roaming невозможен сразу после проведения платежа.
        Платеж клирится в течении суток после проведения платежа. Поэтому мы берем старый платеж из базы и
        рефандим его.
        !! После переналивки, если тест не будет работать, прогнать его несколько раз, для того, что бы в базе
        появились платежи для рефанда!
        """
        paymethod = UberRoamingCard()
        country = defaults.CountryData.Russia
        user = uids.get_random_of_type(user_type)
        currency = country['currency']
        # Просто проводим платеж на будущее
        with check_mode(CheckMode.FAILED):
            simple.process_payment(self.service, user, paymethod=paymethod,
                                   currency=currency)
        # Проводим рефанд платежа
        payment_info = db_steps.bs().get_tpi_for_uber_refund(uids.get_all_uids_from_pools(user_type.pool))
        if not payment_info:
            raise Exception('Отсуствуют платежи по которым можно сделать возврат')

        user_uid = payment_info['passport_id']
        user = uids.search_user_in_pools(user_type.pool, user_uid)
        trust_payment_id = payment_info['trust_payment_id']
        basket = simple.check_basket(self.service, trust_payment_id=trust_payment_id, user=user)
        simple.process_refund(self.service, trust_payment_id=trust_payment_id, user=user, basket=basket)

    @reporter.story(story_prefix + stories.General.Payment)
    @pytest.mark.parametrize('test_data', Data.test_data_roaming_card, ids=DataObject.ids)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure, ids=DataObject.ids)
    def test_base_multipayment_cycle_with_reversal(self, test_data, orders_structure):
        paymethod, country = test_data.paymethod, test_data.country_data
        user = uids.get_random_of_type(user_type)
        currency = country['currency']
        orders = simple.form_orders_for_create(self.service, user, orders_structure)
        with check_mode(CheckMode.FAILED):
            payment = simple.process_payment(self.service, user, paymethod=paymethod, orders=orders,
                                             currency=currency)['trust_payment_id']
        # запрашиваем реверсал
            orders_for_update = simple.form_orders_for_update(orders, default_action='cancel')
            simple.update_basket(self.service, orders=orders_for_update, user=user,
                                 trust_payment_id=payment)
            simple.wait_until_real_postauth(self.service, user=user,
                                            trust_payment_id=payment)

    @reporter.story(story_prefix + stories.General.Payment)
    @pytest.mark.parametrize('test_data', Data.test_data_roaming_compensation, ids=DataObject.ids)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure, ids=DataObject.ids)
    def test_base_multipayment_cycle_compensation(self, test_data, orders_structure):
        paymethod, country = test_data.paymethod, test_data.country_data
        user = uids.get_random_of_type(user_type)
        currency = country['currency']
        for _ in range(PAYMENTS_COUNT):
            with check_mode(CheckMode.FAILED):
                simple.process_payment(self.service, user, orders_structure=orders_structure,
                                       paymethod=paymethod, currency=currency)

    @reporter.story(story_prefix + stories.General.Payment)
    @pytest.mark.parametrize('test_data', Data.test_data_roaming_failed, ids=DataObject.ids)
    @pytest.mark.parametrize('orders_structure', Data.orders_structure, ids=DataObject.ids)
    def test_failed_payment(self, test_data, orders_structure):
        paymethod, country = test_data.paymethod, test_data.country_data
        user = uids.get_random_of_type(user_type)
        user_ip, currency, region_id = country['user_ip'], country['currency'], country['region_id']
        orders = simple.form_orders_for_create(self.service, user, orders_structure)
        paymethod.init(self.service, user)
        with check_mode(CheckMode.FAILED):
            basket = simple.create_basket(self.service, user=user,
                                          orders=orders,
                                          paymethod_id=paymethod.id, currency=currency,
                                          uber_oauth_token=paymethod.uber_oauth_token)
            payment_form = simple.pay_basket(self.service, user=user,
                                             trust_payment_id=basket['trust_payment_id']).get('payment_form')
            trust.pay_by(paymethod, self.service, user=user, payment_form=payment_form,
                         purchase_token=basket['purchase_token'])

        with check_mode(CheckMode.IGNORED):
            basket = simple.wait_until_payment_failed(self.service, user=user,
                                                      purchase_token=basket['purchase_token'])

        check.check_that(basket, has_entries({'status': 'cancelled',
                                              'status_code': 'invalid_processing_request',
                                              'status_desc': 'Bad Request'}),
                         step=u'Проверяем код ошибки после попытки оплаты',
                         error=u'Ожидался неуспешный платеж, а прошел успешный. Либо текст ошибки не верен')


if __name__ == '__main__':
    pytest.main()
