# coding=utf-8

from datetime import timedelta

import pytest
from hamcrest import is_in, is_, is_not, equal_to, has_key, not_

import btestlib.reporter as reporter
from btestlib.constants import Services
from simpleapi.common.oauth import Auth
from simpleapi.common.payment_methods import LinkedCard
from simpleapi.common.utils import DataObject
from simpleapi.data import defaults
from simpleapi.data import features, stories
from simpleapi.data import marks
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import get_card, CardBrand, Ecommpay, RBS, Payture
from simpleapi.matchers.deep_equals import deep_equals_to, deep_contains
from simpleapi.steps import bindings_v2_steps as bindings_v2
from simpleapi.steps import check_steps as check
from simpleapi.steps import expected_steps as expected
from simpleapi.steps import mongo_steps as mongo
from simpleapi.steps import simple_steps as simple
from simpleapi.steps import trust_steps as trust
from simpleapi.steps import web_payment_steps as web

__author__ = 'slppls'

'''
https://st.yandex-team.ru/TRUST-3820 - основная задачка
https://st.yandex-team.ru/TRUST-5086 - задача со всеми доработками
'''
pytestmark = marks.simple_internal_logic

service = Services.TAXI


class Data(object):
    auto_data = [
        DataObject(card=get_card(brand=CardBrand.emu_MasterCard), user_type=uids.Types.random_from_test_passport,
                   descr=defaults.BindingMethods.standard2),
        DataObject(card=get_card(brand=CardBrand.emu_MasterCard), user_type=uids.Types.random_from_test_passport,
                   descr=defaults.BindingMethods.random_amt),
        DataObject(card=get_card(brand=CardBrand.new_api_3ds), user_type=uids.Types.random_from_test_passport,
                   descr=defaults.BindingMethods.standard2_3ds),
    ]
    auto_data_new_user = [
        DataObject(card=get_card(brand=CardBrand.emu_MasterCard), user_type=uids.Types.random_autoremove,
                   descr=defaults.BindingMethods.standard2),
    ]
    base_data = [
        DataObject(card=get_card(brand=CardBrand.emu_MasterCard), user_type=uids.Types.random_from_test_passport,
                   descr=defaults.BindingMethods.standard2).new(method=defaults.BindingMethods.standard2),
        pytest.mark.skipif(True, reason='standart1 doesn`t work')(
            DataObject(card=get_card(brand=CardBrand.emu_MasterCard), user_type=uids.Types.random_from_test_passport,
                       descr=defaults.BindingMethods.standard1).new(method=defaults.BindingMethods.standard1)),
        DataObject(card=get_card(brand=CardBrand.emu_MasterCard), user_type=uids.Types.random_from_test_passport,
                   descr=defaults.BindingMethods.standard2_3ds).new(method=defaults.BindingMethods.standard2_3ds),
        DataObject(card=get_card(brand=CardBrand.emu_MasterCard), user_type=uids.Types.random_from_test_passport,
                   descr=defaults.BindingMethods.random_amt).new(method=defaults.BindingMethods.random_amt),
    ]
    test_data_no_3ds_terminal = [
        DataObject(card=Ecommpay.Success.Without3DS.card_mastercard,
                   region_id=defaults.CountryData.Georgia['region_id'],
                   user_type=uids.Types.random_from_test_passport,
                   descr='Switch from standard2_3ds to standard2').new(method=defaults.BindingMethods.standard2_3ds)
    ]
    test_data_region_id = [
        DataObject(card=get_card(brand=CardBrand.emu_MasterCard),
                   user_type=uids.Types.random_from_test_passport,
                   region_id=defaults.CountryData.Russia['region_id'],
                   currency=defaults.CountryData.Russia['currency']).new(
            second_random_amt=True),
        DataObject(card=RBS.Success.Without3DS.card_visa,
                   user_type=uids.Types.random_from_test_passport,
                   region_id=defaults.CountryData.Kazakhstan['region_id'],
                   currency=defaults.CountryData.Kazakhstan['currency']).new(
            second_random_amt=False),
        DataObject(card=Payture.Success.Without3DS.card_second,
                   user_type=uids.Types.random_from_test_passport,
                   region_id=defaults.CountryData.Armenia['region_id'],
                   currency=defaults.CountryData.Armenia['currency']).new(
            second_random_amt=False),
        DataObject(card=get_card(brand=CardBrand.emu_MasterCard),
                   user_type=uids.Types.random_from_test_passport,
                   region_id=None,
                   currency='RUB').new(
            second_random_amt=True),
        DataObject(card=get_card(brand=CardBrand.emu_MasterCard),
                   user_type=uids.Types.random_from_test_passport,
                   region_id='21536',
                   currency='RUB').new(
            second_random_amt=True)
    ]
    test_data_region_id_methods = [
        DataObject(descr=defaults.BindingMethods.standard2).new(method=defaults.BindingMethods.standard2),
        pytest.mark.skipif(True, reason="standart1 doesn`t work")(
            DataObject(descr=defaults.BindingMethods.standard1).new(method=defaults.BindingMethods.standard1)),
        pytest.mark.skipif(True, reason="3ds_doesn`t work on 168 and 159 region. "
                                        "Task: https://st.yandex-team.ru/TRUST-5851")(
            DataObject(descr=defaults.BindingMethods.standard2_3ds).new(method=defaults.BindingMethods.standard2_3ds)),
        DataObject(descr=defaults.BindingMethods.random_amt).new(method=defaults.BindingMethods.random_amt),
    ]
    system = [
        bindings_v2.MobileApi,
        bindings_v2.ServerApi,
    ]


@reporter.feature(features.General.NewBindingApi)
@reporter.story(stories.CardsOperations.CardsBinding)
class TestBindings(object):
    @staticmethod
    def check_card_bounded(card_id, user, service, system):
        with reporter.step(u'Проверяем что карта {} привязаны к пользователю {}'.format(card_id, user)):
            paymethods = system.get(user, service)['bindings']
            card_ids = [paymethod['id'] for paymethod in paymethods]
            check.check_that(card_id, is_in(card_ids),
                             step=u'Проверяем, что карта присутствует в списке методов оплат',
                             error=u'Карта отсутствует в списке методов оплат')

    @pytest.mark.parametrize('test_data', Data.auto_data + Data.auto_data_new_user,
                             ids=DataObject.ids)
    @pytest.mark.parametrize('system', Data.system, ids=DataObject.ids)
    def test_auto_binding_cycle(self, test_data, system):
        """
        Привязка с верификацией типа auto - траст сам определяет метод.
        В тестинге метод определяется по бину карты.
        """
        user = uids.get_random_of_type(test_data.user_type)
        card = test_data.card
        binding_id, _ = system.process_binding(user, service, card)
        self.check_card_bounded(binding_id, user, service, system)
        bindings_v2.MobileApi.process_unbinding(user, service, binding_id)

    @pytest.mark.parametrize('test_data', Data.base_data, ids=DataObject.ids)
    @pytest.mark.parametrize('system', Data.system, ids=DataObject.ids)
    def test_binding_without_auto(self, test_data, system):
        """
        Первая верификация всегда должна быть с типом auto.
        Иначе выдаем ошибку.
        """
        user = uids.get_random_of_type(test_data.user_type)
        card, method = test_data.card, test_data.method
        token = trust.get_auth_token(Auth.get_auth(user, service), user)['access_token']

        binding_id = bindings_v2.MobileApi.bind_card(token, service, card)['binding']['id']
        resp = system.verify(token, user, service, binding_id, method)
        check.check_that(resp, deep_equals_to(expected.NewBindingApi.invalid_verification_type()),
                         step=u'Проверяем корректность ошибки в статусе',
                         error=u'Некорректный статус!')

    @pytest.mark.parametrize('test_data', Data.base_data, ids=DataObject.ids)
    @pytest.mark.parametrize('system', Data.system, ids=DataObject.ids)
    def test_base_binding_cycle(self, test_data, system):
        """
        Привязка с двойной верификацией. Сначала auto, затем любой method.
        p.s. method != method из auto, см. кейс ниже
        """
        user = uids.get_random_of_type(test_data.user_type)
        card, method = test_data.card, test_data.method
        binding_id, _ = system.process_binding(user, service, card, method=method)
        self.check_card_bounded(binding_id, user, service, system)
        bindings_v2.MobileApi.process_unbinding(user, service, binding_id)

    @pytest.mark.parametrize('test_data', Data.base_data, ids=DataObject.ids)
    @pytest.mark.parametrize('system', Data.system, ids=DataObject.ids)
    def test_same_verify_cache(self, test_data, system):
        """
        Верификация кэшируется по method.
        После успешной верификации через method повторная верификация на этот же method
        должна вернуть результат первой верификации. Время - 2.5 минуты.
        """
        user = uids.get_random_of_type(test_data.user_type)
        card, method = test_data.card, test_data.method
        token = trust.get_auth_token(Auth.get_auth(user, service), user)['access_token']

        binding_id, verification_id = system.process_binding(user, service, card, method=method)
        resp = system.verify(token, user, service, binding_id, method)
        check.check_that(resp['verification']['id'], is_(equal_to(verification_id)),
                         step=u'Проверяем, что вернулась та же самая верификация',
                         error=u'Создалась новая верификация!')
        check.check_that(resp['verification']['status'], is_(equal_to(defaults.BindingStatus.success)),
                         step=u'Проверяем статус верификации',
                         error=u'Некорректный статус верификации!')
        bindings_v2.MobileApi.process_unbinding(user, service, binding_id)

    @pytest.mark.parametrize('test_data', Data.base_data, ids=DataObject.ids)
    @pytest.mark.parametrize('system', Data.system, ids=DataObject.ids)
    def test_same_verify_after_time_cache(self, test_data, system):
        """
        Таймаут в 2.5 минуты можно искуственно убрать, проставив в монге start_ts в прошлое.
        """
        user = uids.get_random_of_type(test_data.user_type)
        card, method = test_data.card, test_data.method
        token = trust.get_auth_token(Auth.get_auth(user, service), user)['access_token']
        binding_id, verification_id = system.process_binding(user, service, card, method=method)
        start_dt = mongo.Payment.get_start_dt_by_purchase_token(verification_id)
        start_dt = start_dt - timedelta(minutes=5)
        mongo.Payment.update_data(purchase_token=verification_id, data_to_update={'start_dt': start_dt})
        last_verify_id = system.process_verify(token, user, service, binding_id, method=method)
        check.check_that(last_verify_id, is_not(equal_to(verification_id)),
                         step=u'Проверяем, что проведены две разные верификации',
                         error=u'Верификации равны!')
        bindings_v2.MobileApi.process_unbinding(user, service, binding_id)

    @pytest.mark.parametrize('system', Data.system, ids=DataObject.ids)
    def test_random_amt_same_sum(self, system):
        """
        При вводе одинаковой неверной суммы в guess_amount траст не
        должен уменьшать счетчик random_amount_tries_left.
        Пример: повторная отправка при подвисании интернета у пользователя.
        """
        expected_tries_left = 2
        user = uids.get_random_of_type(uids.Types.random_from_test_passport)
        card = get_card(brand=CardBrand.emu_MasterCard)
        token = trust.get_auth_token(Auth.get_auth(user, service), user)['access_token']

        binding_id = bindings_v2.MobileApi.bind_card(token, service, card)['binding']['id']
        verification_id = system.verify(token, user, service, binding_id,
                                        defaults.BindingMethods.auto)['verification']['id']
        resp = bindings_v2.Wait.until_verification_done(system, token, user, service, binding_id, verification_id,
                                                        status=defaults.BindingStatus.amount_expected)
        amount = float(resp['authorize_amount']) + 0.01
        for _ in range(3):
            answer = bindings_v2.MobileApi.guess_amount(token, binding_id, verification_id, amount)
            check.check_that(answer['verification']['random_amount_tries_left'], is_(equal_to(expected_tries_left)),
                             step=u'Проверяем, что количество попыток не изменилось',
                             error=u'Количество попыток изменилось!')

    @pytest.mark.parametrize('system', Data.system, ids=DataObject.ids)
    def test_failed_random_amt(self, system):
        """
        Неуспешная привязка для random_amt в случае трех неправильных кодов
        """
        user = uids.get_random_of_type(uids.Types.random_from_test_passport)

        card = get_card(brand=CardBrand.emu_MasterCard)
        token = trust.get_auth_token(Auth.get_auth(user, service), user)['access_token']
        binding_id = bindings_v2.MobileApi.bind_card(token, service, card)['binding']['id']

        verification_id = system.verify(token, user, service, binding_id,
                                        defaults.BindingMethods.auto)['verification']['id']

        resp = bindings_v2.Wait. \
            until_verification_done(system, token, user, service, binding_id, verification_id,
                                    status=defaults.BindingStatus.amount_expected)

        for tries in [2, 1, 0]:
            amount = float(resp['authorize_amount']) - 0.01 * (tries + 1)
            answer = bindings_v2.MobileApi.guess_amount(token, binding_id, verification_id, amount)
            check.check_that(answer['verification']['random_amount_tries_left'], is_(equal_to(tries)),
                             step=u'Проверяем, что количество попыток снизилось',
                             error=u'Количество попыток не изменилось!')
        check.check_that(answer['verification']['status'], is_(equal_to(defaults.BindingStatus.failure)),
                         step=u'Проверяем, что привязка зафейлилась',
                         error=u'Привязка не зафейлилась!')

    @pytest.mark.parametrize('test_data', Data.test_data_no_3ds_terminal, ids=DataObject.ids)
    @pytest.mark.parametrize('system', Data.system, ids=DataObject.ids)
    def test_switching_method_without_3ds_terminal(self, test_data, system):
        """
        Проверяем, что если для региона нет терминала с 3ds то берется
        обычный терминал.
        """
        user = uids.get_random_of_type(test_data.user_type)
        card, method, region_id = test_data.card, test_data.method, test_data.region_id
        # todo: sunshineguy: после доработки задачи TRUST-5852 выпилить флаг wait_3ds из всех ручек!
        binding_id, _ = system.process_binding(user, service, card, method=method, wait_3ds=False)
        self.check_card_bounded(binding_id, user, service, system)
        token = trust.get_auth_token(Auth.get_auth(user, service), user)['access_token']
        verify = system.verify(token, user, service, binding_id, method)
        # todo: sunshineguy: после доработки задачи TRUST-5852  выпилить эту проверку и раскоментить следующую
        check.check_that(verify['verification'], not_(has_key(defaults.BindingStatus.failure)),
                         step=u'Проверяем, что из-за отсутствия 3ds терминала метод верификации изменился на standart2',
                         error=u'Несмотря на отсуствие 3ds терминала, метод верефикации остался преждним!')
        # check.check_that(verify['verification']['method'], is_(equal_to('standard2_3ds')),
        #                  step=u'Проверяем, что метод верификации изменился',
        #                  error=u'Метод верификации не изменился!')
        bindings_v2.MobileApi.process_unbinding(user, service, binding_id)

    @pytest.mark.parametrize('data_methods', Data.test_data_region_id_methods, ids=DataObject.ids)
    @pytest.mark.parametrize('test_data', Data.test_data_region_id, ids=DataObject.ids_region_id)
    @pytest.mark.parametrize('system', Data.system, ids=DataObject.ids)
    def test_base_binding_cycle_with_region_id(self, data_methods, test_data, system):
        """
            Проверяем, что если передан region_id то он прокинется в verify
             и валюта региона отобразится в verify
        """
        user = uids.get_random_of_type(test_data.user_type)
        card, second_random_amt = test_data.card, test_data.second_random_amt
        method = data_methods.method
        binding_id, _ = system.process_binding(user, service, card, method=method, region_id=test_data.region_id,
                                               second_random_amt=second_random_amt)
        self.check_card_bounded(binding_id, user, service, system)
        token = trust.get_auth_token(Auth.get_auth(user, service), user)['access_token']
        currency = system.verify(token, user, service, binding_id, method, )['verification']['authorize_currency']
        check.check_that(currency, is_(equal_to(test_data.currency)),
                         step=u'Валюта проставилась в соответствии с переданным region_id',
                         error=u'Неправильная валюта!')
        bindings_v2.MobileApi.process_unbinding(user, service, binding_id)

@reporter.feature(features.General.NewBindingApi)
@reporter.story(stories.General.Payment)
class TestPayment(object):
    @pytest.mark.parametrize('test_data', Data.auto_data, ids=DataObject.ids)
    @pytest.mark.parametrize('system', Data.system, ids=DataObject.ids)
    def test_auto_binding_wait_for_cvn(self, test_data, system):
        """
        Надо убедиться, что привязанной картой можно платить
        в кейсе с ожиданием cvn
        """
        user = uids.get_random_of_type(test_data.user_type)
        card = test_data.card
        _, _ = system.process_binding(user, service, card)
        paymethod = LinkedCard(card=card)
        simple.process_payment(service, user, paymethod=paymethod,
                               pass_cvn=True, need_postauthorize=True)

    @pytest.mark.parametrize('test_data', Data.auto_data, ids=DataObject.ids)
    @pytest.mark.parametrize('system', Data.system, ids=DataObject.ids)
    def test_auto_binding_base_payment_cycle(self, test_data, system):
        """
        Надо убедиться, что привязанной картой можно платить
        в кейсе с обычной оплатой
        """
        user = uids.get_random_of_type(test_data.user_type)
        card = test_data.card
        _, _ = system.process_binding(user, service, card)
        paymethod = LinkedCard(card=card)
        simple.process_payment(service, user, paymethod=paymethod,
                               need_postauthorize=True)


@reporter.feature(features.General.NewBindingApi)
@reporter.story(stories.Notifications.BindingNotifies)
class TestNotifications(object):
    def test_binding_notification(self):
        """
        После сохранения карточных данных должна прийти нотификация
        Она общая для всех методов верификации
        """
        user = uids.get_random_of_type(uids.Types.random_from_test_passport)
        bindings_v2.ServerApi.settings(user, service)

        card = get_card(brand=CardBrand.emu_MasterCard)
        token = trust.get_auth_token(Auth.get_auth(user, service), user)['access_token']

        binding_id = bindings_v2.MobileApi.bind_card(token, service, card)['binding']['id']
        notify_resp = bindings_v2.Wait.until_binding_notify_done(user, binding_id)
        check.check_that(notify_resp['notification_info']['binding'],
                         deep_contains(expected.NewBindingApi.binding_info_notify(card)),
                         step=u'Проверяем нотификацию по карточным данным',
                         error=u'Нотификация по карточным данным некорректна!')

    @pytest.mark.parametrize('method', [defaults.BindingMethods.standard2,
                                        # standard1 doesn't work
                                        # defaults.BindingMethods.standard1
                                        ], ids=DataObject.ids)
    @pytest.mark.parametrize('system', Data.system, ids=DataObject.ids)
    def test_notification_standard_cycle(self, method, system):
        """
        Нотификации для цикла верификации с методом standard1/standard2
        verification_start -> authorization_result
        """
        user = uids.get_random_of_type(uids.Types.random_from_test_passport)
        bindings_v2.ServerApi.settings(user, service)

        card = get_card(brand=CardBrand.emu_MasterCard)
        token = trust.get_auth_token(Auth.get_auth(user, service), user)['access_token']
        binding_id = bindings_v2.MobileApi.bind_card(token, service, card)['binding']['id']

        system.process_verify(token, user, service, binding_id)
        verification_id = system.verify(token, user, service, binding_id, method)['verification']['id']
        notify_resp = bindings_v2.Wait. \
            until_verification_notify_done(user,
                                           defaults.BindingStatus.in_progress,
                                           defaults.BindingEvent.verify_start,
                                           verification_id)
        check.check_that(notify_resp['notification_info']['verification'],
                         deep_contains(expected.NewBindingApi.verify_start_notify(method)),
                         step=u'Проверяем нотификацию о начале верификации',
                         error=u'Нотификация о начале верификации некорректна!')

        bindings_v2.Wait.until_verification_done(system, token, user, service, binding_id, verification_id)
        notify_resp = bindings_v2.Wait. \
            until_verification_notify_done(user,
                                           defaults.BindingStatus.success,
                                           defaults.BindingEvent.authorize_result,
                                           verification_id)
        check.check_that(notify_resp['notification_info']['verification'],
                         deep_contains(expected.NewBindingApi.authorize_result_notify(method)),
                         step=u'Проверяем нотификацию о результатах верификации',
                         error=u'Нотификация о результатах некорректна!')

    @pytest.mark.parametrize('system', Data.system, ids=DataObject.ids)
    def test_notification_standard2_3ds_cycle(self, system):
        """
        Нотификации для цикла верификации с методом standard2_3ds
        verification_start -> 3ds_start -> 3ds_status_received -> authorization_result
        """
        user = uids.get_random_of_type(uids.Types.random_from_test_passport)
        bindings_v2.ServerApi.settings(user, service)

        card = get_card(brand=CardBrand.new_api_3ds)
        token = trust.get_auth_token(Auth.get_auth(user, service), user)['access_token']
        method = defaults.BindingMethods.standard2_3ds
        binding_id = bindings_v2.MobileApi.bind_card(token, service, card)['binding']['id']

        verification_id = system.verify(token, user, service, binding_id,
                                        defaults.BindingMethods.auto)['verification']['id']
        notify_resp = bindings_v2.Wait. \
            until_verification_notify_done(user,
                                           defaults.BindingStatus.in_progress,
                                           defaults.BindingEvent.verify_start,
                                           verification_id)
        check.check_that(notify_resp['notification_info']['verification'],
                         deep_contains(expected.NewBindingApi.verify_start_notify(method)),
                         step=u'Проверяем нотификацию о начале верификации',
                         error=u'Нотификация о начале верификации некорректна!')

        resp = bindings_v2.Wait. \
            until_verification_done(system, token, user, service, binding_id, verification_id,
                                    status=defaults.BindingStatus.required_3ds)
        notify_resp = bindings_v2.Wait. \
            until_verification_notify_done(user,
                                           defaults.BindingStatus.required_3ds,
                                           defaults.BindingEvent.start_3ds,
                                           verification_id)
        check.check_that(notify_resp['notification_info']['verification'],
                         deep_contains(expected.NewBindingApi.required_3ds_notify()),
                         step=u'Проверяем нотификацию об ожидании 3ds',
                         error=u'Нотификация об ожидании 3ds некорректна!')

        web.fill_emulator_3ds_page(resp['3ds_url'], defaults.Binding3dsCode.success)
        notify_resp = bindings_v2.Wait. \
            until_verification_notify_done(user,
                                           defaults.BindingStatus.status_3ds_received,
                                           defaults.BindingEvent.status_3ds_received,
                                           verification_id)
        check.check_that(notify_resp['notification_info']['verification'],
                         deep_contains(expected.NewBindingApi.status_3ds_received_notify()),
                         step=u'Проверяем нотификацию о вводе 3ds',
                         error=u'Нотификация о вводе 3ds некорректна!')

        bindings_v2.Wait. \
            until_verification_done(system, token, user, service, binding_id, verification_id)
        notify_resp = bindings_v2.Wait. \
            until_verification_notify_done(user,
                                           defaults.BindingStatus.success,
                                           defaults.BindingEvent.authorize_result,
                                           verification_id)
        check.check_that(notify_resp['notification_info']['verification'],
                         deep_contains(expected.NewBindingApi.authorize_result_notify(method)),
                         step=u'Проверяем нотификацию о результатах верификации',
                         error=u'Нотификация о результате верификации некорректна!')

    @pytest.mark.parametrize('system', Data.system, ids=DataObject.ids)
    def test_notification_random_amt_cycle(self, system):
        """
        Нотификации для цикла верификации с методом random_amt
        verification_start -> authorization_result -> confirmation_code_received
        """
        user = uids.get_random_of_type(uids.Types.random_from_test_passport)
        bindings_v2.ServerApi.settings(user, service)

        card = get_card(brand=CardBrand.emu_MasterCard)
        token = trust.get_auth_token(Auth.get_auth(user, service), user)['access_token']
        method = defaults.BindingMethods.random_amt
        binding_id = bindings_v2.MobileApi.bind_card(token, service, card)['binding']['id']

        verification_id = system.verify(token, user, service, binding_id,
                                        defaults.BindingMethods.auto)['verification']['id']
        notify_resp = bindings_v2.Wait. \
            until_verification_notify_done(user,
                                           defaults.BindingStatus.in_progress,
                                           defaults.BindingEvent.verify_start,
                                           verification_id)
        check.check_that(notify_resp['notification_info']['verification'],
                         deep_contains(expected.NewBindingApi.verify_start_notify(method)),
                         step=u'Проверяем нотификацию о начале верификации',
                         error=u'Нотификация о начале верификации некорректна!')

        resp = bindings_v2.Wait. \
            until_verification_done(system, token, user, service, binding_id, verification_id,
                                    status=defaults.BindingStatus.amount_expected)
        notify_resp = bindings_v2.Wait. \
            until_verification_notify_done(user,
                                           defaults.BindingStatus.amount_expected,
                                           defaults.BindingEvent.authorize_result,
                                           verification_id)
        check.check_that(notify_resp['notification_info']['verification'],
                         deep_contains(expected.NewBindingApi.random_amt_notify(method)),
                         step=u'Проверяем нотификацию о результатах верификации',
                         error=u'Нотификация о результате верификации некорректна!')

        amount = float(resp['authorize_amount'])
        bindings_v2.MobileApi.guess_amount(token, binding_id, verification_id, amount)

        notify_resp = bindings_v2.Wait. \
            until_verification_notify_done(user,
                                           defaults.BindingStatus.success,
                                           defaults.BindingEvent.confirmation_code_received,
                                           verification_id)
        check.check_that(notify_resp['notification_info']['verification'],
                         deep_contains(expected.NewBindingApi.random_amt_notify(method)),
                         step=u'Проверяем нотификацию о вводе кода подтверждения',
                         error=u'Нотификация о вводе кода подтверждения некорректна!')

    @pytest.mark.parametrize('system', Data.system, ids=DataObject.ids)
    def test_notification_failed_standard2_3ds(self, system):
        """
        Нотификация для неуспешной standard2_3ds верификации
        """
        user = uids.get_random_of_type(uids.Types.random_from_test_passport)
        bindings_v2.ServerApi.settings(user, service)

        card = get_card(brand=CardBrand.new_api_3ds)
        token = trust.get_auth_token(Auth.get_auth(user, service), user)['access_token']
        method = defaults.BindingMethods.standard2_3ds
        binding_id = bindings_v2.MobileApi.bind_card(token, service, card)['binding']['id']

        verification_id = system.verify(token, user, service, binding_id,
                                        defaults.BindingMethods.auto)['verification']['id']
        resp = bindings_v2.Wait. \
            until_verification_done(system, token, user, service, binding_id, verification_id,
                                    status=defaults.BindingStatus.required_3ds)

        web.fill_emulator_3ds_page(resp['3ds_url'], defaults.Binding3dsCode.wrong_3ds)
        notify_resp = bindings_v2.Wait. \
            until_verification_notify_done(user,
                                           defaults.BindingStatus.failure,
                                           defaults.BindingEvent.authorize_result,
                                           verification_id)
        check.check_that(notify_resp['notification_info']['verification'],
                         deep_contains(expected.NewBindingApi.failed_authorize_result_notify(method)),
                         step=u'Проверяем нотификацию о результатах верификации',
                         error=u'Нотификация о результате верификации некорректна!')

    @pytest.mark.parametrize('system', Data.system, ids=DataObject.ids)
    def test_notification_failed_random_amt(self, system):
        """
        Нотификация для неуспешной random_amt верификации
        """
        user = uids.get_random_of_type(uids.Types.random_from_test_passport)
        bindings_v2.ServerApi.settings(user, service)

        card = get_card(brand=CardBrand.emu_MasterCard)
        token = trust.get_auth_token(Auth.get_auth(user, service), user)['access_token']
        method = defaults.BindingMethods.random_amt
        binding_id = bindings_v2.MobileApi.bind_card(token, service, card)['binding']['id']

        verification_id = system.verify(token, user, service, binding_id,
                                        defaults.BindingMethods.auto)['verification']['id']

        resp = bindings_v2.Wait. \
            until_verification_done(system, token, user, service, binding_id, verification_id,
                                    status=defaults.BindingStatus.amount_expected)

        for num in range(3):
            amount = float(resp['authorize_amount']) - 0.01 * (num + 1)
            bindings_v2.MobileApi.guess_amount(token, binding_id, verification_id, amount)

        notify_resp = bindings_v2.Wait. \
            until_verification_notify_done(user,
                                           defaults.BindingStatus.failure,
                                           defaults.BindingEvent.confirmation_code_received,
                                           verification_id)
        check.check_that(notify_resp['notification_info']['verification'],
                         deep_contains(expected.NewBindingApi.random_amt_notify(method)),
                         step=u'Проверяем нотификацию о вводе кода подтверждения',
                         error=u'Нотификация о вводе кода подтверждения некорректна!')

    @pytest.mark.parametrize('test_data', Data.base_data, ids=DataObject.ids)
    @pytest.mark.parametrize('system', Data.system, ids=DataObject.ids)
    def test_same_verify_after_unbind_cache(self, test_data, system):
        """
            После отвязки карты в течение времени кеша новая верификация
            должна вернуть новый verification_id
        """
        user = uids.get_random_of_type(test_data.user_type)
        card, method = test_data.card, test_data.method
        binding_id, verification_id = system.process_binding(user, service, card, method=method)
        bindings_v2.MobileApi.process_unbinding(user, service, binding_id)
        binding_id_2, verification_id_2 = system.process_binding(user, service, card, method=method)
        check.check_that(verification_id_2, is_not(equal_to(verification_id)),
                         step=u'Проверяем, что вернулась новая верификация',
                         error=u'Вернулась старая верификация!')
