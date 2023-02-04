# coding: utf-8

"""
Script for testing card bindings.
"""

import random
import string

import pytest
from hamcrest import is_in, not_, is_, equal_to, not_none, none, \
    contains_string, has_entries, contains
import btestlib.reporter as reporter
from btestlib import matchers
from btestlib.constants import Services
from btestlib.utils import CheckMode, check_mode, Web, Pytest
from simpleapi.common import logger
from simpleapi.common.oauth import Auth
from simpleapi.common.payment_methods import TrustWebPage, Via, LinkedCard
from simpleapi.common.utils import DataObject
from simpleapi.data import cards_pool
from simpleapi.data import defaults
from simpleapi.data import features, stories
from simpleapi.data import marks
from simpleapi.data import uids_pool as uids
from simpleapi.steps import check_steps as check
from simpleapi.steps import mongo_steps as mongo
from simpleapi.steps import simple_steps as simple
from simpleapi.steps import trust_steps as trust
from simpleapi.steps import web_payment_steps as web

log = logger.get_logger()


def ids_port(val):
    if 18013 == val:
        return 'wo_proxy'
    return 'proxy'


def ids_processing_card(val):
    return 'status={}'.format(val.card.get('descr'))


def ids_ip(val):
    return "{}_ip".format(val.country_data['name'])


def ids_country(val):
    return val['name']


class Data(object):
    cards = [
        cards_pool.get_card(),
        cards_pool.get_card_with_separator(cards_pool.get_card(), ' '),
        cards_pool.get_card_with_separator(cards_pool.get_card(), '\t'),
    ]
    regions = [
        '225',
        '84'
    ]
    labels = [
        'Some label',
        # pytest.mark.skipif(True, reason="Some trouble with cyrillic in teamcity")
        # (u'Какая-то метка'),
        'Some label 1!@#$%^&*()>/|\<,{}'
    ]
    services_unavailable_multiple = [
        Services.STORE,
        # Services.DISK,
        # Services.PARKOVKI,
    ]

    test_data_with_ip = [
        DataObject(country_data=defaults.CountryData.Armenia,
                   card=cards_pool.RBS.Success.Without3DS.card_visa),
        DataObject(country_data=defaults.CountryData.Georgia,
                   card=cards_pool.Ecommpay.Success.Without3DS.card_mastercard),
        DataObject(country_data=defaults.CountryData.Kazakhstan,
                   card=cards_pool.RBS.Success.Without3DS.card_visa),
        DataObject(country_data=defaults.CountryData.Russia,
                   card=cards_pool.get_card()),
        DataObject(country_data=defaults.CountryData.Other,
                   card=cards_pool.get_card())
    ]
    test_bind_yamoney_data = [
        DataObject(card=cards_pool.get_card(cvn=cards_pool.CVN.not_enough_funds_RC51)).new(
            status=defaults.Status.not_enough_funds),
        DataObject(card=cards_pool.get_card(cvn=cards_pool.CVN.do_not_honor_RC05)).new(
            status=defaults.Status.authorization_reject),
        DataObject(card=cards_pool.get_card(cvn=cards_pool.CVN.error_RC06)).new(
            status=defaults.Status.technical_error),
        DataObject(card=cards_pool.get_card(cvn=cards_pool.CVN.invalid_transaction_RC12)).new(
            status=defaults.Status.technical_error),
        DataObject(card=cards_pool.get_card(cvn=cards_pool.CVN.restricted_card_RC36)).new(
            status=defaults.Status.authorization_reject),
        DataObject(card=cards_pool.get_card(cvn=cards_pool.CVN.transaction_not_permitted_RC57)).new(
            status=defaults.Status.transaction_not_permitted),
        DataObject(card=cards_pool.get_card(cvn=cards_pool.CVN.transaction_not_permitted_RC58)).new(
            status=defaults.Status.transaction_not_permitted),
        DataObject(card=cards_pool.get_card(cvn=cards_pool.CVN.restricted_card_RC62)).new(
            status=defaults.Status.authorization_reject)
    ]
    test_bind_rbs_data = [
        DataObject(card=cards_pool.RBS.Success.Without3DS.card_visa).new(
            status=defaults.RBS.BindStatusDesc.success),
        DataObject(card=cards_pool.RBS.Success.With3DS.card_discover).new(
            status=defaults.RBS.BindStatusDesc.success),
        DataObject(card=cards_pool.RBS.Failed.BlockedByLimit.card).new(
            status=defaults.Status.limit_exceeded),
        DataObject(card=cards_pool.RBS.Failed.CardLimitations.card).new(
            status=defaults.Status.limit_exceeded),
        DataObject(card=cards_pool.RBS.Failed.CommError.card).new(
            status=defaults.Status.fail_3ds),
        DataObject(card=cards_pool.RBS.Failed.LimitExceed.card).new(
            status=defaults.Status.limit_exceeded),
        DataObject(card=cards_pool.RBS.Failed.MsgFormat.card).new(
            status=defaults.Status.unknown_error),
        DataObject(card=cards_pool.RBS.Failed.NetworkRefused.card).new(
            status=defaults.Status.authorization_reject),
        DataObject(card=cards_pool.RBS.Failed.NoFunds.card).new(
            status=defaults.RBS.BindStatusDesc.code116)
    ]
    test_bind_sberbank_data = [
        # У Сбера те же карты, что и у РБС и те же коды ошибок
        # за исключением нескольких карт
        DataObject(card=cards_pool.Sberbank.Success.Without3DS.card_visa).new(
            status=defaults.RBS.BindStatusDesc.success),
        DataObject(card=cards_pool.Sberbank.Success.With3DS.card_discover).new(
            status=defaults.RBS.BindStatusDesc.success),
        DataObject(card=cards_pool.Sberbank.Failed.BlockedByLimit.card).new(
            status=defaults.Status.limit_exceeded),
        DataObject(card=cards_pool.Sberbank.Failed.CardLimitations.card).new(
            status=defaults.Status.limit_exceeded),
        DataObject(card=cards_pool.Sberbank.Failed.CommError.card).new(
            status=defaults.Status.fail_3ds),
        DataObject(card=cards_pool.Sberbank.Failed.LimitExceed.card).new(
            status=defaults.Status.limit_exceeded),
        DataObject(card=cards_pool.Sberbank.Failed.MsgFormat.card).new(
            status=defaults.Status.unknown_error),
        DataObject(card=cards_pool.Sberbank.Failed.NetworkRefused.card).new(
            status=defaults.Status.authorization_reject),
        DataObject(card=cards_pool.Sberbank.Failed.NotEnoughMoney.card).new(
            status=defaults.Status.not_enough_funds),
        # (cards_pool.Sberbank.Failed.ResponseTimeout.card, defaults.RBS.BindStatusDesc.code5), # номер у нас фейковый
        DataObject(card=cards_pool.Sberbank.Failed.CannotSendRequest.card).new(
            status=defaults.Status.authorization_reject), ####
        DataObject(card=cards_pool.Sberbank.Success.SpasiboTrue.card).new(
            status=defaults.RBS.BindStatusDesc.success),
        DataObject(card=cards_pool.Sberbank.Success.SpasiboDisabled.card).new(
            status=defaults.RBS.BindStatusDesc.success),
        # (cards_pool.Sberbank.Failed.ActivityFraud.card, defaults.RBS.BindStatusDesc.success),
    ]
    test_bind_payture_data = [
        DataObject(card=cards_pool.Payture.Success.Without3DS.card_second).new(
            status=defaults.Payture.BindStatusDesc.success),
        DataObject(card=cards_pool.Payture.Failed.BlackListed.card).new(
            status=defaults.Payture.BindStatusDesc.black_listed),
        DataObject(card=cards_pool.Payture.Failed.ExpiredCard.card).new(
            status=defaults.Payture.BindStatusDesc.expired_card),
        DataObject(card=cards_pool.Payture.Failed.NoFunds.card).new(
            status=defaults.Payture.BindStatusDesc.no_funds),
        DataObject(card=cards_pool.Payture.Failed.WrongCard.card).new(
            status=defaults.Payture.BindStatusDesc.issuer_card_fail),
        DataObject(card=cards_pool.Payture.Failed.Issuer.card_fail).new(
            status=defaults.Payture.BindStatusDesc.issuer_card_fail),
        DataObject(card=cards_pool.Payture.Failed.Issuer.blocked_card).new(
            status=defaults.Payture.BindStatusDesc.issuer_blocked_card),
        # (cards_pool.Payture.Failed.TimeOut.blocking, defaults.Payture.BindStatusDesc.timeout),
        DataObject(card=cards_pool.Payture.Failed.ProcessingError.blocking).new(
            status=defaults.Payture.BindStatusDesc.processing_error),
    ]
    test_bind_ecommpay_data = [
        DataObject(card=cards_pool.Ecommpay.Success.Without3DS.card_mastercard).new(
            status=defaults.Ecommpay.BindStatusDesc.success),
    ]
    test_yamoney_countries = [
        defaults.CountryData.Russia,
    ]
    test_rbs_countries = [
        defaults.CountryData.Ukraine,
        defaults.CountryData.Kazakhstan
    ]
    test_ecommpay_countries = [
        defaults.CountryData.Germany,
        defaults.CountryData.Moldova,
        defaults.CountryData.Georgia,
        defaults.CountryData.Kyrgyzstan,
        defaults.CountryData.Latvia,
    ]

    card_1 = cards_pool.get_card()

    test_user_with_one_linked = [
        # список сценариев есть здесь https://wiki.yandex-team.ru/users/nesterova-av/cases3698/
        DataObject(user_type=uids.Types.random_with_linked_phonishes)
            .new(main_user_cards=(cards_pool.get_card(),),
                 first_linked_user_cards=tuple(),
                 second_linked_user_cards=tuple(),
                 descr=u'card_1 is linked to UID_1, UID_2 has no linked cards'),
        DataObject(user_type=uids.Types.random_with_linked_phonishes)
            .new(main_user_cards=tuple(),
                 first_linked_user_cards=(cards_pool.get_card(),),
                 second_linked_user_cards=tuple(),
                 descr=u'UID_1 has no linked cards, card_1 is linked to UID_2'),
        DataObject(user_type=uids.Types.random_with_linked_phonishes)
            .new(main_user_cards=(cards_pool.get_card(), card_1),
                 first_linked_user_cards=(cards_pool.get_card(), card_1),
                 second_linked_user_cards=tuple(),
                 descr=u'card_1 and card_2 is linked to UID_1, card_1 and card_3 is linked to UID_2'),
        DataObject(user_type=uids.Types.random_with_linked_phonishes)
            .new(main_user_cards=(cards_pool.get_card(), cards_pool.get_card()),
                 first_linked_user_cards=(cards_pool.get_card(), cards_pool.get_card()),
                 second_linked_user_cards=tuple(),
                 descr=u'card_1 and card_2 is linked to UID_1, card_3 and card_4 is linked to UID_2'),
    ]
    test_data_mapping = [
        DataObject(country_data=defaults.CountryData.Russia).new(expected_sum='2.00'),
        DataObject(country_data=defaults.CountryData.CountryWithoutCurrency).new(expected_sum='2.00'),
        DataObject(country_data=defaults.CountryData.Armenia).new(expected_sum='40.00'),
        DataObject(country_data=defaults.CountryData.Georgia).new(expected_sum='0.40'),
        DataObject(country_data=defaults.CountryData.Kazakhstan).new(expected_sum='20.00'),
        DataObject(country_data=defaults.CountryData.Moldova).new(expected_sum='2.00'),
        DataObject(country_data=defaults.CountryData.Latvia).new(expected_sum='0.20'),
    ]
    test_data_expiration_data = [
        DataObject(descr='update_year').new(date_params={'expiration_year': '2022'}),
        DataObject(descr='update_month').new(date_params={'expiration_month': '09'}),
        DataObject(descr='update_month_and_year').new(date_params={'expiration_year': '2022',
                                                                   'expiration_month': '09' }),
    ]


@reporter.feature(features.General.CardsOperations)
@reporter.story(stories.CardsOperations.CardsBinding)
class TestBindUnbind(object):
    @staticmethod
    def check_all_cards_bounded(user, cards_, service=Services.STORE):
        with reporter.step(u'Проверяем что все карты {} привязаны к пользователю {}'.format(cards_, user)):
            _, paymethods = simple.list_payment_methods(service, user)

            for card in cards_:
                check.check_that(card, is_in(paymethods))

    @staticmethod
    def check_no_cards_bounded(user, cards_, service=Services.STORE):
        with reporter.step(u'Проверяем что ни одна из карт {} не привязана к пользователю {}'.format(cards_, user)):
            _, paymethods = simple.list_payment_methods(service, user)

            for card in cards_:
                check.check_that(card, not_(is_in(paymethods)))

    @staticmethod
    def check_only_last_card_bounded(user, cards_, service=Services.STORE):
        with reporter.step(
                u'Проверяем что из карт {} только последняя привязана к пользователю {}'.format(cards_, user)):
            _, paymethods = simple.list_payment_methods(service, user)

            for card in cards_[:-1]:
                check.check_that(card, not_(is_in(paymethods)))

            check.check_that(cards_[-1], is_in(paymethods))

    @pytest.fixture
    def user_without_linked_cards(self):
        with reporter.step(u'Выбираем случайного пользователя для теста и отвязываем от него все привязанне карты'):
            user = uids.get_random_of(uids.mutable)
            log.debug("Choose user: %s" % user)

            trust.process_unbinding(user=user)
            log.debug("Unbind all cards of user %s before test" % user)

            return user

    @marks.ym_h2h_processing
    @pytest.mark.parametrize('card', Data.cards, ids=DataObject.ids_card)
    @pytest.mark.parametrize('region_id', Data.regions, ids=DataObject.ids_region_id)
    def test_bind_card(self, user_without_linked_cards, card, region_id):
        user = user_without_linked_cards
        linked_cards, trust_payment_id = trust.process_binding(user=user, cards=card, region_id=region_id)
        self.check_all_cards_bounded(user=user, cards_=linked_cards)
        mongo.Refund.wait_until_done(trust_payment_id[0])
        trust.process_unbinding(user=user)

    @marks.ym_h2h_processing
    def test_bind_card_for_new_user(self):
        user = uids.get_random_of(uids.autoremove)
        linked_cards, trust_payment_id = trust.process_binding(user=user, cards=cards_pool.get_card())
        self.check_all_cards_bounded(user=user, cards_=linked_cards)
        mongo.Refund.wait_until_done(trust_payment_id[0])
        trust.process_unbinding(user=user)

    @marks.ym_h2h_processing
    @pytest.mark.parametrize('card', Data.cards, ids=DataObject.ids_card)
    @pytest.mark.parametrize('region_id', Data.regions, ids=DataObject.ids_region_id)
    def test_unbind_card_short_id(self, user_without_linked_cards, card, region_id):
        """
        Проверяем, что если отвязываем карту по сокращенному id - она отвязывается
        полный id: card-x9999
        сокращенный id: x9999
        """
        user = user_without_linked_cards
        linked_cards, _ = trust.process_binding(user=user, cards=card, region_id=region_id)
        self.check_all_cards_bounded(user=user, cards_=linked_cards)
        for linked_card in linked_cards:
            trust.process_unbinding(user=user, card=linked_card.replace('card-', ''))
        self.check_no_cards_bounded(user=user, cards_=linked_cards)

    @marks.ym_h2h_processing
    @pytest.mark.parametrize('card', Data.cards, ids=DataObject.ids_card)
    @pytest.mark.parametrize('region_id', Data.regions, ids=DataObject.ids_region_id)
    def test_existing_binding(self, user_without_linked_cards, card, region_id):
        user = user_without_linked_cards
        trust.process_binding(user=user, cards=card)
        linked_cards, _ = trust.process_binding(user=user, cards=card, region_id=region_id)
        self.check_all_cards_bounded(user=user, cards_=linked_cards)
        trust.process_unbinding(user=user)

    @pytest.fixture
    def test_user_without_linked_cards(self):
        with reporter.step(u'Выбираем случайного пользователя для теста и отвязываем от него все привязанне карты'):
            user = uids.get_random_of(uids.test_passport)
            log.debug("Choose user: %s" % user)

            trust.process_unbinding(user=user)
            log.debug("Unbind all cards of user %s before test" % user)

            return user

    @pytest.mark.parametrize('card', Data.cards, ids=DataObject.ids_card)
    @pytest.mark.parametrize(*Pytest.combine_set(Pytest.ParamsSet(names='region_id',
                                                                  values=[['225'], ['84']]),
                                                 Pytest.ParamsSet(names='service',
                                                                  values=[
                                                                      [Services.PHONE, Services.TAXI],
                                                                      [Services.TAXI, ],
                                                                  ])),
                             ids=lambda region_id, service: '{} {}'.format(
                                 DataObject.ids_service(service), DataObject.ids_region_id(region_id)))
    def test_multiple_bind_available(self, test_user_without_linked_cards, card, region_id, service):
        """
        Для скоупов Стор, Диск, Парковки должна оставаться только последняя привязанная карта
        Для остальных скоупов - все карты
        """
        user = test_user_without_linked_cards
        linked_cards, _ = trust.process_binding(user=user, cards=(card,
                                                                  cards_pool.get_card(brand=cards_pool.CardBrand.Visa)),
                                                service=service, region_id=region_id)
        self.check_all_cards_bounded(user=user, service=service, cards_=linked_cards)
        trust.process_unbinding(user=user, service=service)

    @pytest.mark.parametrize('card', Data.cards, ids=DataObject.ids_card)
    @pytest.mark.parametrize('service', Data.services_unavailable_multiple, ids=DataObject.ids_service)
    def test_multiple_bind_unavailable(self, test_user_without_linked_cards, card, service):
        """
        Для скоупов Стор, Диск, Парковки должна оставаться только последняя привязанная карта
        Для остальных скоупов - все карты
        """
        user = test_user_without_linked_cards
        linked_cards, _ = trust.process_binding(user=user, cards=(card,
                                                                  cards_pool.get_card(brand=cards_pool.CardBrand.Visa)),
                                                service=service)
        self.check_only_last_card_bounded(user=user, service=service, cards_=linked_cards)
        trust.process_unbinding(user=user, service=service)

    @pytest.fixture
    def prepare_data(self, request):
        def prapared_data(uids_pool=uids.test_passport.copy()):
            user = uids.get_random_of(uids_pool)
            token = trust.get_auth_token(Auth.get_auth(user, Services.TAXI), user)['access_token']

            def fin():
                """
                Отвязываем все карты клиента в конце теста
                """
                trust.process_unbinding(user)

            request.addfinalizer(fin)

            return user, token

        return prapared_data

    @pytest.mark.parametrize('with_ip_data', Data.test_data_with_ip, ids=ids_ip)
    def test_bind_with_user_ip(self, with_ip_data, prepare_data):
        # https://st.yandex-team.ru/TRUST-1813
        # В проде ip определяется автоматически, в тесте такой возможности нет, поэтому для такси
        # в методе bind_card было введено поле user_ip. На основе него метод list_payment_methods
        # должен выдавать валюту и регион, соответствующий стране этого ip
        user, token = prepare_data()
        country, card = with_ip_data.country_data, with_ip_data.card
        user_ip, expected_currency, expected_region_id = country['user_ip'], country['currency'], country['region_id']

        with check_mode(CheckMode.FAILED):
            resp_bc = trust.bind_card(token, card, user_ip=user_ip, region_id=expected_region_id)

        paymethod = resp_bc['payment_method']
        paymethod_info = trust.list_payment_methods(token, region_id=expected_region_id)['payment_methods'][paymethod]
        check.check_that(expected_currency, is_(equal_to(paymethod_info['currency'])),
                         step=u'Проверяем корректность валюты', error=u'Некорректная валюта')
        check.check_that(expected_region_id, is_(equal_to(paymethod_info['region_id'])),
                         step=u'Проверяем корректность региона', error=u'Некорректный регион')

    def test_after_error_queue_doesnot_go_down(self, prepare_data):
        user, token = prepare_data()
        card = cards_pool.get_card(cvn=cards_pool.CVN.force_3ds)

        with check_mode(CheckMode.IGNORED):
            resp = trust.bind_card(token, card)
        mongo_desc, mongo_status = mongo.Payment.get_error_params_from_payment(resp['trust_payment_id'])
        check.check_that(resp['status_code'], is_(equal_to(mongo_status)),
                         step=u'Проверяем корректность статуса в MongoDB', error=u'Некорректный статус')
        check.check_that(resp['status_desc'], is_(equal_to(mongo_desc)),
                         step=u'Проверяем корректность ошибки в MongoDB', error=u'Некорректная ошибка')

    def test_rebind_expired_card(self, user_without_linked_cards):
        """
        По следам TRUST-1525
        Суть следующая: если у пользователя протухла карта,
        то он может спокойно перепривязать перевыпущенную карту с тем же номером
        """
        user = user_without_linked_cards
        card = cards_pool.get_card()
        region_id = 225

        linked_cards, trust_payment_id = trust.process_binding(user=user, cards=card, region_id=region_id)
        self.check_all_cards_bounded(user=user, cards_=linked_cards)
        with reporter.step(u'Меняем карту в MongoDB так чтобы она стала просроченной'):
            mongo.Card.update_data(trust_payment_id[0], data_to_update={'expiration_month': '01',
                                                                        'expiration_year': '2017'})
        self.check_no_cards_bounded(user=user, cards_=linked_cards)
        linked_cards, _ = trust.process_binding(user=user, cards=card, region_id=region_id)
        self.check_all_cards_bounded(user=user, cards_=linked_cards)

    def test_blocking_reason_after_rebind_card(self, user_without_linked_cards):
        """
        По следам TRUST-3081
        Суть следующая: если у пользователя была заблокирована карта,
        то после перепривязки перевыпущенной карты с тем же номером у новой не должно быть blocking_reason
        """
        user = user_without_linked_cards
        card = cards_pool.get_card()
        region_id = 225

        linked_cards, trust_payment_id = trust.process_binding(user=user, cards=card, region_id=region_id)
        self.check_all_cards_bounded(user=user, cards_=linked_cards)
        with reporter.step(u'Меняем карту в MongoDB так чтобы она стала просроченной и заблокированной'):
            mongo.Card.update_data(trust_payment_id[0], data_to_update={'blocking_reason': 'Some test reason',
                                                                        'expiration_month': '01',
                                                                        'expiration_year': '2017'
                                                                        })
            mongo.PaymentMethodsCache.clean_lpm_cache_for_user(user)
        self.check_no_cards_bounded(user=user, cards_=linked_cards)
        trust.process_binding(user=user, cards=card, region_id=region_id)
        rebinded_card = simple.find_card_by_masked_number(service=Services.STORE, user=user,
                                                          number=cards_pool.get_masked_number(card['card_number']))
        check.check_that('blocking_reason', not_(is_in(rebinded_card)),
                         step=u'Проверяем что после перепривязки у карты стерся blocking_reason',
                         error=u'У карты после перепривязки не стерся blocking_reason')

    @marks.ym_h2h_processing
    @pytest.mark.parametrize('test_data', Data.test_bind_yamoney_data, ids=ids_processing_card)
    @pytest.mark.parametrize('country', Data.test_yamoney_countries, ids=ids_country)
    def test_yamoney_bind_card(self, test_data, prepare_data, country):
        user, token = prepare_data()
        card, expected_status_desc = test_data.card, test_data.status
        with check_mode(CheckMode.IGNORED):
            resp = trust.bind_card(token, card, user_ip=country['user_ip'], region_id=country['region_id'])
        check.check_that(resp['status_desc'], contains_string(expected_status_desc),
                         step=u'Проверяем корректность ответа', error=u'Некорретный ответ метода bind_card для rbs')

    @marks.rbs_processing
    @pytest.mark.parametrize('test_data', Data.test_bind_rbs_data, ids=ids_processing_card)
    @pytest.mark.parametrize('country', Data.test_rbs_countries, ids=ids_country)
    def test_rbs_bind_card(self, test_data, prepare_data, country):
        user, token = prepare_data()
        card, expected_status_desc = test_data.card, test_data.status
        with check_mode(CheckMode.IGNORED):
            resp = trust.bind_card(token, card, user_ip=country['user_ip'], region_id=country['region_id'])
        if expected_status_desc is defaults.RBS.BindStatusDesc.success:
            mongo.Refund.wait_until_done(resp['trust_payment_id'])
        check.check_that(resp['status_desc'], contains_string(expected_status_desc),
                         step=u'Проверяем корректность ответа', error=u'Некорретный ответ метода bind_card для rbs')

    @marks.payture_processing
    @pytest.mark.parametrize('test_data', Data.test_bind_payture_data, ids=ids_processing_card)
    def test_payture_bind_card(self, test_data, prepare_data):
        user, token = prepare_data()
        card, expected_status_desc = test_data.card, test_data.status
        country = defaults.CountryData.Armenia
        with check_mode(CheckMode.IGNORED):
            resp = trust.bind_card(token, card, user_ip=country['user_ip'], region_id=country['region_id'])
        if expected_status_desc is defaults.Payture.BindStatusDesc.success:
            mongo.Refund.wait_until_done(resp['trust_payment_id'])
        check.check_that(resp['status_desc'], is_(equal_to(expected_status_desc)),
                         step=u'Проверяем корректность ответа', error=u'Некорретный ответ метода bind_card для payture')

    @marks.ecommpay_processing
    @pytest.mark.parametrize('test_data', Data.test_bind_ecommpay_data, ids=ids_processing_card)
    @pytest.mark.parametrize('country', Data.test_ecommpay_countries, ids=ids_country)
    def test_ecommpay_bind_card(self, test_data, prepare_data, country):
        user, token = prepare_data()
        card, expected_status_desc = test_data.card, test_data.status
        with check_mode(CheckMode.IGNORED):
            resp = trust.bind_card(token, card, user_ip=country['user_ip'], region_id=country['region_id'])
        mongo.Refund.wait_until_done(resp['trust_payment_id'])
        check.check_that(resp['status_desc'], is_(equal_to(expected_status_desc)),
                         step=u'Проверяем корректность ответа',
                         error=u'Некорретный ответ метода bind_card для ecommpay')

    @marks.sberbank_processing
    @pytest.mark.parametrize('test_data', Data.test_bind_sberbank_data, ids=ids_processing_card)
    def test_sberbank_bind_card(self, test_data, prepare_data):
        user, token = prepare_data(uids_pool=uids.sberbank)
        card, expected_status_desc = test_data.card, test_data.status
        country = defaults.CountryData.Russia
        with check_mode(CheckMode.IGNORED):
            resp = trust.bind_card(token, card, user_ip=country['user_ip'], region_id=country['region_id'])
        if expected_status_desc is defaults.RBS.BindStatusDesc.success:
            mongo.Refund.wait_until_done(resp['trust_payment_id'])
        check.check_that(resp['status_desc'], contains_string(expected_status_desc),
                         step=u'Проверяем корректность ответа', error=u'Некорретный ответ метода bind_card для Сбера')

    def test_exchange_ym_token(self, prepare_data):
        """
            Ручка exchange_ym_token в трасте дергается в том случае, если по какой-то причине в монге у карты нет
            карточного токена. Траст идет с ЯДовским токеном в ЯД и получает на основе него карточный токен, а затем
            платит им. Отсюда этот кейс:
            - привязать карту и удалить у нее card_token
            - совершить какую-нибудь простенькую оплату
            - card_token снова должен появиться в монго
        """
        user, token = prepare_data()
        card = cards_pool.get_card()

        trust_payment_id = trust.bind_card(token, card)['trust_payment_id']

        mongo.Card.unset_token(trust_payment_id)
        _, card_token = mongo.Card.get_ym_and_card_tokens(trust_payment_id)
        check.check_that(card_token, none(),
                         step=u'Проверяем отсутствие в MongoDB карточного токена',
                         error=u'Карточный токен присутствует!')
        simple.process_payment(Services.TAXI, user, paymethod=LinkedCard(card=card),
                               orders_structure=defaults.Order.structure_rub_one_order)
        ym_token, card_token = mongo.Card.get_ym_and_card_tokens(trust_payment_id)
        check.check_that(ym_token, not_none(),
                         step=u'Проверяем наличие в MongoDB токена ЯД',
                         error=u'Токен ЯД отсутствует!')
        check.check_that(card_token, not_none(),
                         step=u'Проверяем наличие в MongoDB карточного токена',
                         error=u'Карточный токен отсутствует!')

    def test_delete_card_token_and_rebind_card(self, prepare_data):
        """
        По следам TRUST-5754
        Суть: Необходимо убедиться, что card-token сохраняется, даже
        если карту уже привязывали раньше.
        """
        user, token = prepare_data()
        card = cards_pool.get_card()
        bind_info = trust.bind_card(token, card)
        trust_payment_id = bind_info['trust_payment_id']
        card_id = bind_info['payment_method']
        mongo.Card.unset_token(trust_payment_id)
        _, card_token = mongo.Card.get_ym_and_card_tokens(trust_payment_id)
        check.check_that(card_token, is_(none()),
                         step=u'Проверяем отсутствие в MongoDB карточного токена',
                         error=u'Карточный токен присутствует!')
        trust.unbind_card(token, card_id)
        trust.bind_card(token, card)
        _, card_token = mongo.Card.get_ym_and_card_tokens(trust_payment_id)
        check.check_that(card_token, not_none(),
                         step=u'Проверяем наличие в MongoDB карточного токена',
                         error=u'Карточный токен отсутствует!')

    @marks.ym_h2h_processing
    @pytest.mark.parametrize('test_data', Data.test_data_expiration_data, ids=DataObject.ids)
    @pytest.mark.parametrize('card', Data.cards, ids=DataObject.ids_card)
    def test_change_expiration_dt_from_future_to_past(self, user_without_linked_cards, card, test_data):
        """
        Тест проверяет, что срок действия карты можно поменять в прошлое.
        1. Привязываем карту
        2. Меняем срок дейсвтия карты на дату бОльшую чем была при привязке (в Монге)
        3. Превязываем карту еще раз с изначальной датой -> Срок дейсвтия карты должен измениться.
        """
        user = user_without_linked_cards
        date_params = test_data.date_params
        linked_cards, trust_payment_id = trust.process_binding(user=user, cards=card)
        mongo.Card.update_data(trust_payment_id[0], date_params, wait=False)
        _, paymethods = simple.list_payment_methods(Services.STORE, user)
        payment_methods = paymethods[linked_cards[0]]
        check.check_that(payment_methods, has_entries(date_params),
                         step=u'Проверяем, что срок дейсвтия карты изменился',
                         error=u'Срок действия карты не изменился!')
        trust.process_binding(user=user, cards=card)
        _, paymethods = simple.list_payment_methods(Services.STORE, user)
        payment_methods = paymethods[linked_cards[0]]
        check.check_that(payment_methods, has_entries({'expiration_month': card['expiration_month'],
                                                       'expiration_year': card['expiration_year']}),
                         step=u'Проверяем что после повторной привязки карты, срок дейсвтия карты изменился!',
                         error=u'Срок дейсвтия карты не изменился!')
        trust.process_unbinding(user=user)

    @pytest.mark.parametrize('test_data', Data.test_data_mapping, ids=DataObject.ids_region_id)
    def test_currency_maping_bind_card(self, user_without_linked_cards, test_data):
        card = cards_pool.get_card()
        user = user_without_linked_cards
        country, expected_sum = test_data.country_data, test_data.expected_sum
        reference_mapping_value = {
            'currency': country['currency'],
            'sum': expected_sum
        }
        token = trust.get_auth_token(Auth.get_auth(user, Services.TAXI), user)['access_token']
        # Если привязка карты отвалится по какой-либо причине, вероятнее всего мы все равно сможем посмотреть
        # валюту и сумму платежа в MongoDB.
        # Это не самое надежное решение :(
        with check_mode(CheckMode.IGNORED):
            resp = trust.bind_card(token, card, region_id=country['region_id'])
        trust_payment_id = resp['trust_payment_id']
        payment_info = mongo.Payment.find_by(trust_payment_id)
        check.check_that(reference_mapping_value, has_entries({'currency': payment_info['currency'],
                                                              'sum': str(payment_info['sum'])}),
                         step=u'Проверяем, что валюта и сумма списания выбраны верно',
                         error=u'Валюта и сумма списания выбраны неверно')
        trust.process_unbinding(user=user)


def _get_random_label():
    return ''.join(random.choice(string.ascii_letters + string.digits) for _ in range(20))


@reporter.feature(features.General.CardsOperations)
@reporter.story(stories.CardsOperations.LinkedUsers)
class TestBindForLinkedUsers(object):
    @staticmethod
    def check_card_in_paymethods(paymethods, card):
        check.check_that(paymethods.values(),
                         matchers.contains_dicts_with_entries(
                             ({'number': cards_pool.get_masked_number(card['card_number']),
                               'type': 'card'},),
                             same_length=False),
                         step=u'Проверяем что карта {} есть в списке привязанных карт'.format(card['card_number']),
                         error=u'Карты {} нет в списке привязанных карт'.format(card['card_number']))

    @staticmethod
    def check_bounded_cards(user, service, main_user_expected_cards, *linked_users_expected_cards):

        def get_all_cards(*lists_of_cards):
            """объединяет списки карт в один список, при этом выкидывает дубликаты"""
            joined_list = list()
            for list_of_cards in lists_of_cards:
                joined_list.extend(list_of_cards)

            return [dict(t) for t in set([tuple(d.items()) for d in joined_list if d])]

        with reporter.step(u'Проверяем список привязанных карт основного пользователя {}'.format(user)):
            _, paymethods_of_main_user = simple.list_payment_methods(service, user)

            for card in get_all_cards(main_user_expected_cards, *linked_users_expected_cards):
                TestBindForLinkedUsers.check_card_in_paymethods(paymethods_of_main_user, card)

        with reporter.step(u'Проверяем список привязанных карт связанных фонишей'):
            for linked_user, his_expected_cards in zip(user.linked_users, linked_users_expected_cards):
                _, paymethods_of_linked_user = simple.list_payment_methods(service, linked_user)

                for card in his_expected_cards:
                    TestBindForLinkedUsers.check_card_in_paymethods(paymethods_of_linked_user, card)

    @staticmethod
    def undind_cards_of_main_user_and_all_linked_users(user):
        with reporter.step(u'Отвязываем от пользователя {} '
                           u'и связанных с ним пользователей все привязанные карты'.format(user)):
            trust.process_unbinding(user=user)
            # for linked_user in user.linked_users:
            #     trust.process_unbinding(user=linked_user)

    @pytest.mark.parametrize('test_data', Data.test_user_with_one_linked, ids=DataObject.ids)
    @pytest.mark.parametrize('region_id', Data.regions, ids=DataObject.ids_region_id)
    def test_user_with_one_linked(self, test_data, region_id):
        user_type, main_user_cards, first_linked_user_cards, second_linked_user_cards = test_data.user_type, \
                                                                                        test_data.main_user_cards, \
                                                                                        test_data.first_linked_user_cards, \
                                                                                        test_data.second_linked_user_cards
        user = uids.get_random_of_type(user_type)
        self.undind_cards_of_main_user_and_all_linked_users(user)

        for card in main_user_cards:
            trust.process_binding(user=user, cards=card, region_id=region_id, multiple=1)

        for linked_user, users_linked_cards in zip(user.linked_users,
                                                   (first_linked_user_cards, second_linked_user_cards)):
            for card in users_linked_cards:
                trust.process_binding(user=linked_user, cards=card, region_id=region_id, multiple=1)

        self.check_bounded_cards(user, Services.PASSPORT, main_user_cards, first_linked_user_cards,
                                 second_linked_user_cards)


@reporter.feature(features.General.CardsOperations)
@reporter.story(stories.CardsOperations.CardsLabels)
class TestSetCardLabel(object):
    """
    Тесты на простановку сервисных меток
    https://beta.wiki.yandex-team.ru/balance/simple/servicelabels/
    https://st.yandex-team.ru/TRUST-804
    """

    @staticmethod
    def check_label_present(card, user, label, service=Services.STORE):
        with reporter.step(
                u'Проверяем что у карты {} присутствует метка {} в скоупе для сервиса {}'.format(card, label, service)):
            _, paymethods = simple.list_payment_methods(service, user)
            check.check_that(label, is_in(paymethods[card]['service_labels']))

    @staticmethod
    def check_label_absent(card, user, label, service=Services.STORE):
        with reporter.step(
                u'Проверяем что у карты {} отсутствует метка {} в скоупе для сервиса {}'.format(card, label, service)):
            _, paymethods = simple.list_payment_methods(service, user)
            if paymethods[card].get('service_labels'):
                check.check_that(label, not_(is_in(paymethods[card]['service_labels'])))

    @staticmethod
    def setting_label(user, card, label=None):
        if not label:
            label = _get_random_label()
        with check_mode(CheckMode.FAILED):
            simple.set_card_label(Services.STORE, user, card, label)
        TestSetCardLabel.check_label_present(card, user, label)
        # check label doesn't visible under other service
        TestSetCardLabel.check_label_absent(card, user, label, service=Services.DISK)
        return label

    @staticmethod
    def delete_label(user, card, label):
        with check_mode(CheckMode.FAILED):
            simple.set_card_label(Services.STORE, user, card, label, 'delete')
        TestSetCardLabel.check_label_absent(card, user, label)

    @pytest.fixture
    def user_and_linked_card(self, request):
        """
        Выбираем пользователя и привязываем ему карту
        """
        user = uids.get_random_of(uids.mutable)
        log.debug("Choose user: %s" % user)
        card = cards_pool.get_card()
        token = trust.get_auth_token(Auth.get_auth(user), user)['access_token']
        card_id = trust.bind_card(token, card)['payment_method']

        def fin():
            log.debug("Finally unbind card")
            trust.process_unbinding(user)

        request.addfinalizer(fin)

        return user, card_id, token, card

    @pytest.mark.parametrize('label', Data.labels, ids=DataObject.ids_custom('label'))
    def test_set_and_delete_label(self, user_and_linked_card, label):
        user, card_id, _, _ = user_and_linked_card
        self.setting_label(user, card_id, label)
        self.delete_label(user, card_id, label)

    def test_set_and_delete_label_new_user(self):
        user = uids.get_random_of(uids.autoremove)
        cards, _ = trust.process_binding(user=user, cards=cards_pool.get_card())
        label = 'Some_label'
        self.setting_label(user, cards[0], label)
        self.delete_label(user, cards[0], label)
        trust.process_unbinding(user=user)

    def test_delete_label_doesnt_exist(self, user_and_linked_card):
        user, card_id, _, _ = user_and_linked_card
        label = "This label doesn't exist"
        self.delete_label(user, card_id, label)

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
        log.debug("Choose user_1: %s, user_2: %s" % (user_1, user_2))

        token = trust.get_auth_token(Auth.get_auth(user_1), user_1)['access_token']
        card = trust.bind_card(token, cards_pool.REAL_CARD)['payment_method']

        def fin():
            log.debug("Finally unbind card")
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

    def test_user_does_not_have_card(self):
        user = uids.get_random_of(uids.test_passport)
        card_id = 'x11aaa1bbb2c3333ddd4444e'
        label = _get_random_label()
        with check_mode(CheckMode.IGNORED):
            resp = simple.set_card_label(Services.STORE, user, card_id, label)
            check.check_that([resp['status'], resp['status_desc']],
                             contains(u'error', u'User {} doesn`t have card ''{}'.format(user.uid, card_id)),
                             step=u'Проверяем, что при попытке поставить метку отвязаной карте получаем верную ошибку',
                             error=u'При попытке поставить метку отвязаной карте получили неверную ошибку')


@reporter.feature(features.General.CardsOperations)
@reporter.story(stories.CardsOperations.CardsBinding)
class TestUnbindCardOnPaymentPage(object):
    class Data(object):
        services_xmlrpc_api = [
            Services.TICKETS,
            Services.EVENTS_TICKETS,
            Services.EVENTS_TICKETS_NEW,
            Services.BUSES
        ]

    @pytest.mark.parametrize('service', Data.services_xmlrpc_api, ids=DataObject.ids_service)
    def test_unbind_card_on_payment_page(self, service):
        card = cards_pool.get_card()
        paymethod = TrustWebPage(Via.linked_card(card))
        user = uids.get_random_of_type(uids.Types.random_from_all)

        linked_cards, _ = trust.process_binding(user=user, cards=card, service=service)

        payment_form = simple.process_to_payment_form(service, user=user,
                                                      paymethod=paymethod, init_paymethod=False)

        with Web.DriverProvider() as driver:
            web.get_paymethods(service). \
                unbind_linked_card(card=card,
                                   card_id=linked_cards[0],
                                   payment_form=payment_form,
                                   driver=driver)

        TestBindUnbind.check_no_cards_bounded(user, linked_cards, service=service)


if __name__ == '__main__':
    pytest.main()
