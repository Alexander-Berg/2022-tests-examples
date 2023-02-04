# coding: utf-8

import datetime

import pytest
from dateutil.parser import parse

import btestlib.reporter as reporter
from btestlib.constants import Services
from simpleapi.common.payment_methods import Phone, LinkedCard, NoPaymethod, TrustWebPage, Via, YandexMoney
from simpleapi.common.utils import parse_subs_period, DataObject, current_scheme_is
from simpleapi.data import defaults
from simpleapi.data import features, stories
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import get_card
from simpleapi.matchers.date_matchers import later_on_timedelta_than_
from simpleapi.matchers.deep_equals import deep_equals_to
from simpleapi.steps import check_steps as check
from simpleapi.steps import expected_steps as expected
from simpleapi.steps import simple_steps as simple
from simpleapi.steps import simple_steps_bo as simple_bo

__author__ = 'fellow'
"""
Модуль тестирования подписок

Подписки обновляются скриптом https://github.yandex-team.ru/Billing/yb-balance-paysys/blob/RELEASE-2_105/paysys/subscriptions.py
Скрипт запускается через pycron: SELECT * FROM T_PYCRON_SCHEDULE WHERE name = 'subscriptions';
N.B. надо, чтобы стоял хост greed-tm1f, "кажется там есть механизм, который на ts не дает пускать скрипты" (c) @buyvich
Логи находятся в /var/log/yb/subscriptions.log

Посмотреть список заказов на продление (попадающих в обработчик) можно запросом

SELECT *
FROM t_order o
  JOIN t_service_product p ON o.SERVICE_PRODUCT_ID = p.ID
WHERE
  o.SUBS_UNTIL_DT > o.DT AND o.SUBS_UNTIL_DT <= sysdate AND o.SUBS_UNTIL_DT > sysdate - 2 AND o.FINISH_DT IS NULL
  AND
  o.SERVICE_ID != 117 AND p.ACTIVE_UNTIL_DT > sysdate
ORDER BY o.dt DESC;

Чтобы очистить очередь, можно проставить у этих заказов finish_dt

"""


class Data(object):
    test_data = [
        DataObject(service=Services.STORE,
                   paymethod=LinkedCard(card=get_card()),
                   region_id=225,
                   currency='RUB',
                   user_type=uids.Types.random_from_all),
        DataObject(service=Services.STORE,
                   paymethod=LinkedCard(card=get_card()),
                   region_id=84,
                   currency='USD',
                   user_type=uids.Types.random_from_all),
        # (Services.DISK, TrustWebPage(Via.card(get_card())), 225, 'RUB', uids.Types.random_from_all),
        # (Services.DISK, TrustWebPage(Via.card(get_card())), 84, 'USD', uids.Types.random_from_all),
        pytest.mark.yamoney(
            DataObject(service=Services.DISK,
                       paymethod=YandexMoney(),
                       region_id=225,
                       currency='RUB',
                       user_type=uids.Type(pool=uids.secret,
                                           name='test_wo_proxy_old'))),
        # DataObject(service=Services.STORE,
        #            paymethod=Phone(),
        #            region_id=225,
        #            currency='RUB',
        #            user_type=uids.Types.random_with_phone),
    ]
    test_data_trial = [
        # DataObject(service=Services.STORE,
        #            paymethod=Phone(),
        #            region_id=225,
        #            currency='USD',
        #            user_type=uids.Types.random_with_phone),
        # (Services.DISK, Card(card=get_card()), 225, 'RUB', uids.Types.random_from_all),
    ]
    test_data_trial_wo_paymethod = [
        DataObject(service=Services.STORE,
                   paymethod=NoPaymethod(),
                   region_id=225,
                   currency='RUB',
                   user_type=uids.Types.random_from_all),
        # (Services.DISK, NoPaymethod(), 225, 'RUB', uids.Types.random_from_all),
    ]
    test_data_other = [
        DataObject(service=Services.STORE,
                   paymethod=LinkedCard(card=get_card()),
                   region_id=225,
                   currency='RUB',
                   user_type=uids.Types.random_from_all),
        pytest.mark.skipif(True, reason="No mobile payments for Disk")
        # ((Services.DISK, LinkedCard(card=get_card()), 225, 'RUB', uids.Types.random_from_all)),
        # (Services.DISK, TrustWebPage(Via.card(get_card())), 225, 'RUB', uids.Types.random_from_all),
    ]
    only_store = [
        DataObject(service=Services.STORE,
                   paymethod=LinkedCard(card=get_card()),
                   region_id=225,
                   currency='RUB',
                   user_type=uids.Types.random_from_all),
    ]

    only_disk = [
        pytest.mark.skipif(True, reason="No mobile payments for Disk")
        (DataObject(service=Services.DISK,
                    paymethod=LinkedCard(card=get_card()),
                    region_id=225,
                    currency='RUB',
                    user_type=uids.Types.random_from_all)),
        DataObject(service=Services.DISK,
                   paymethod=TrustWebPage(Via.card(get_card())),
                   region_id=225,
                   currency='RUB',
                   user_type=uids.Types.random_from_all),
    ]


@pytest.mark.skipif(not current_scheme_is('BO'), reason="Only BO scheme")
@reporter.feature(features.Service.Disk + ' and ' + features.Service.Store)
@reporter.story(stories.General.Subscription)
class TestSubscriptions(object):
    @pytest.fixture()
    def create_normal_subscription(self, request):
        def normal_subscription(service, user):
            with reporter.step(u'Создаем подписочный продукт '
                               u'без триального периода'):
                product = defaults.ServiceProduct.Subscription.NORMAL.copy()
                service_product_id = \
                    simple.create_service_product_for_service(service,
                                                              product_type=product)
                service_order_id = simple_bo.get_service_order_id(service)

            def fin():
                with reporter.step(u'Останавливаем подписку в конце теста'):
                    simple_bo.stop_subscription(service,
                                                service_order_id=service_order_id,
                                                user=user)

            request.addfinalizer(fin)

            return service_product_id, service_order_id

        return normal_subscription

    @pytest.mark.parametrize('test_data', Data.test_data, ids=DataObject.ids)
    def test_normal_subscription_continuation(self, create_normal_subscription,
                                              test_data):
        """
        Продление подписки без триального периода
        """
        service, paymethod, region_id, currency, user_type = \
            test_data.service, test_data.paymethod, test_data.region_id, \
            test_data.currency, test_data.user_type
        user = uids.get_random_of_type(user_type)
        service_product_id, service_order_id = \
            create_normal_subscription(service, user)

        resp = simple_bo.create_and_pay_order(service, user,
                                              service_order_id=service_order_id,
                                              service_product_id=service_product_id,
                                              paymethod=paymethod,
                                              region_id=region_id,
                                              currency=currency)

        timedelta = parse_subs_period(resp['subs_period'])
        check.check_that(parse(resp['subs_until_ts']),
                         later_on_timedelta_than_(parse(resp['begin_ts']), *timedelta),
                         step=u'Проверяем, что в момент начала подписки subs_until_ts = begin_ts + subs_period',
                         error=u'Некорректный subs_until_ts')
        simple_bo.wait_until_subscription_continuation(service, service_order_id, user=user)

    @pytest.fixture()
    def create_trial_subscription(self, request):
        def trial_subscription(service, user):
            with reporter.step(u'Создаем подписочный продукт c триальным периодом'):
                product = defaults.ServiceProduct.Subscription.TRIAL.copy()
                product.update({'active_until_dt': None})
                service_product_id = simple.create_service_product_for_service(service, product_type=product)
                service_order_id = simple_bo.get_service_order_id(service)

            def fin():
                with reporter.step(u'Останавливаем подписку в конце теста'):
                    simple_bo.stop_subscription(service, service_order_id=service_order_id, user=user)

            request.addfinalizer(fin)

            return service_product_id, service_order_id

        return trial_subscription

    @pytest.mark.parametrize('test_data', Data.test_data_trial, ids=DataObject.ids)
    def test_trial_subscription_with_paymethod_continuation(self, create_trial_subscription, test_data):
        """
        Подписка с триальным периодом и с заданным способом оплаты дожна продлеваться
        """
        service, paymethod, region_id, currency, user_type = \
            test_data.service, test_data.paymethod, test_data.region_id, test_data.currency, test_data.user_type
        user = uids.get_random_of_type(user_type)
        service_product_id, service_order_id = create_trial_subscription(service, user)

        simple_bo.create_and_pay_order(service, user, service_order_id=service_order_id,
                                       service_product_id=service_product_id, paymethod=paymethod,
                                       region_id=region_id, currency=currency)

        simple_bo.wait_until_trial_subscription_continuation(service, service_order_id, user=user)

    @pytest.mark.parametrize('test_data', Data.test_data_trial_wo_paymethod, ids=DataObject.ids)
    def test_trial_subscription_without_paymethod_fail_continuation(self, create_trial_subscription, test_data):
        """
        Подписка с триальным периодом и без заданного способа оплаты дожна останавливаться
        """
        service, paymethod, region_id, currency, user_type = \
            test_data.service, test_data.paymethod, test_data.region_id, test_data.currency, test_data.user_type
        user = uids.get_random_of_type(user_type)
        service_product_id, service_order_id = create_trial_subscription(service, user)

        simple_bo.create_and_pay_order(service, user, service_order_id=service_order_id,
                                       service_product_id=service_product_id, paymethod=paymethod,
                                       region_id=region_id, currency=currency)

        simple_bo.wait_until_subscription_finished(service, service_order_id, user=user)

    @pytest.fixture()
    def create_short_lived_subscription(self):
        def trial_subscription(service):
            with reporter.step(u'Создаем подписочный продукт c active_until_dt=sysdate+3_минуты'):
                product = defaults.ServiceProduct.Subscription.NORMAL.copy()
                product.update({'active_until_dt': datetime.datetime.now() + datetime.timedelta(minutes=3)})
                service_product_id = simple.create_service_product_for_service(service, product_type=product)
                service_order_id = simple_bo.get_service_order_id(service)

                return service_product_id, service_order_id

        return trial_subscription

    @pytest.mark.parametrize('test_data', Data.test_data_other, ids=DataObject.ids)
    def test_short_lived_subscription(self, create_short_lived_subscription, test_data):
        """
        Если истекает active_until_dt в подписочном продукте, то заказ дальше не продлевается
        """
        service, paymethod, region_id, currency, user_type = \
            test_data.service, test_data.paymethod, test_data.region_id, test_data.currency, test_data.user_type
        user = uids.get_random_of_type(user_type)
        service_product_id, service_order_id = create_short_lived_subscription(service)

        simple_bo.create_and_pay_order(service, user, service_order_id=service_order_id,
                                       service_product_id=service_product_id, paymethod=paymethod,
                                       region_id=region_id, currency=currency)
        # вначале подписка должна продлиться, до того как истек active_until_dt ...
        simple_bo.wait_until_subscription_continuation(service, service_order_id, user=user)
        # ... а затем остановиться
        simple_bo.wait_until_subscription_finished(service, service_order_id, user=user)

    @pytest.mark.parametrize('test_data', Data.test_data_other, ids=DataObject.ids)
    def test_already_purchase(self, create_normal_subscription, test_data):
        """
        Если у подписочного продукта стоит single_purchase=1 то пользователь не может иметь две активных подписки
        CreateOrder должен возвращать service_order_id активной подписки
        """
        service, paymethod, region_id, currency, user_type = \
            test_data.service, test_data.paymethod, test_data.region_id, test_data.currency, test_data.user_type
        user = uids.get_random_of_type(user_type)
        service_product_id, service_order_id = create_normal_subscription(service, user)

        simple_bo.create_order(service, service_order_id,
                               service_product_id, region_id=region_id, user=user)

        resp = simple_bo.create_order(service, service_order_id,
                                      service_product_id, region_id=region_id, user=user)

        check.check_that(resp, deep_equals_to(expected.SubscriptionOrder.already_purchased(service_order_id)))

    @pytest.fixture()
    def create_two_normal_subscription_orders(self, request):
        def normal_subscription(service, user, single_purchase=None):
            with reporter.step(u'Создаем подписочный продукт без триального периода с single_purchase='
                                       .format(single_purchase)):
                product = defaults.ServiceProduct.Subscription.NORMAL.copy()
                if single_purchase is not None:
                    product.update({'single_purchase': single_purchase})
                service_product_id = simple.create_service_product_for_service(service, product_type=product)
                service_order_id_1 = simple_bo.get_service_order_id(service)
                service_order_id_2 = simple_bo.get_service_order_id(service)

            def fin():
                with reporter.step(u'Останавливаем подписки в конце теста'):
                    simple_bo.stop_subscription(service, service_order_id=service_order_id_1, user=user)
                    if not (service == Services.STORE and (not single_purchase or single_purchase == 1)):
                        simple_bo.stop_subscription(service, service_order_id=service_order_id_2, user=user)

            request.addfinalizer(fin)

            return service_product_id, service_order_id_1, service_order_id_2

        return normal_subscription

    @pytest.mark.parametrize('test_data', Data.only_store, ids=DataObject.ids)
    def test_single_purchase_1_work(self, create_two_normal_subscription_orders, test_data):
        """
        Для некотрых сервисов (например Стор) включена следующая настройка:
        Если у подписочного продукта стоит single_purchase=1 то пользователь не может иметь две активных подписки
        Если уже есть оплаченная подписка, CreateOrder должен возвращать service_order_id активной подписки
        """
        service, paymethod, region_id, currency, user_type = \
            test_data.service, test_data.paymethod, test_data.region_id, test_data.currency, test_data.user_type
        user = uids.get_random_of_type(user_type)
        service_product_id, service_order_id_1, service_order_id_2 = \
            create_two_normal_subscription_orders(service, user)
        simple_bo.create_and_pay_order(service, user, service_order_id=service_order_id_1,
                                       service_product_id=service_product_id, paymethod=paymethod,
                                       region_id=region_id, currency=currency)
        resp = simple_bo.create_order(service, service_order_id_2,
                                      service_product_id, region_id=region_id, user=user)
        check.check_that(resp, deep_equals_to(expected.SubscriptionOrder.created(service_order_id_1)),
                         step=u'Проверяем что возвращается service_order_id активной подписки',
                         error=u'Вернулся service_order_id вновь созданной подписки')

    @pytest.mark.skipif(True, reason="Disk now only in postgres")
    @pytest.mark.parametrize('test_data', Data.only_disk, ids=DataObject.ids)
    def test_single_purchase_1_doesnt_work(self, create_two_normal_subscription_orders, test_data):
        """
        Для некотрых сервисов (например Диск) признак single_purchase отключен
        Для Диска, например, может быть несколько подписок на 10GB
        """
        service, paymethod, region_id, currency, user_type = \
            test_data.service, test_data.paymethod, test_data.region_id, test_data.currency, test_data.user_type
        user = uids.get_random_of_type(user_type)
        service_product_id, service_order_id_1, service_order_id_2 = \
            create_two_normal_subscription_orders(service, user)
        simple_bo.create_and_pay_order(service, user, service_order_id=service_order_id_1,
                                       service_product_id=service_product_id, paymethod=paymethod,
                                       region_id=region_id, currency=currency)
        simple_bo.create_and_pay_order(service, user, service_order_id=service_order_id_2,
                                       service_product_id=service_product_id, paymethod=paymethod,
                                       region_id=region_id, currency=currency)
        simple_bo.wait_until_subscription_continuation(service, service_order_id_1, user=user)
        simple_bo.wait_until_subscription_continuation(service, service_order_id_2, user=user)

    @pytest.mark.parametrize('test_data', Data.test_data_other, ids=DataObject.ids)
    def test_single_purchase_0(self, create_two_normal_subscription_orders, test_data):
        """
        Если у подписочного продукта стоит single_purchase=0,
        то у пользователя может быть более одной активной подписки
        """
        service, paymethod, region_id, currency, user_type = \
            test_data.service, test_data.paymethod, test_data.region_id, test_data.currency, test_data.user_type
        user = uids.get_random_of_type(user_type)
        service_product_id, service_order_id_1, service_order_id_2 = \
            create_two_normal_subscription_orders(service, user, single_purchase=0)

        simple_bo.create_and_pay_order(service, user, service_order_id=service_order_id_1,
                                       service_product_id=service_product_id, paymethod=paymethod,
                                       region_id=region_id, currency=currency)

        simple_bo.create_and_pay_order(service, user, service_order_id=service_order_id_2,
                                       service_product_id=service_product_id, paymethod=paymethod,
                                       region_id=region_id, currency=currency)

        simple_bo.wait_until_subscription_continuation(service, service_order_id_1, user=user)
        simple_bo.wait_until_subscription_continuation(service, service_order_id_2, user=user)

    @pytest.mark.parametrize('test_data', Data.only_store, ids=DataObject.ids)
    def test_stop_subscription_on_trial(self, create_trial_subscription, test_data):
        """
        Останавливаем подписку в момент действия триального периода
        """
        service, paymethod, region_id, currency, user_type = \
            test_data.service, test_data.paymethod, test_data.region_id, test_data.currency, test_data.user_type
        user = uids.get_random_of_type(user_type)
        service_product_id, service_order_id = create_trial_subscription(service, user)

        simple_bo.create_and_pay_order(service, user, service_order_id=service_order_id,
                                       service_product_id=service_product_id, paymethod=paymethod,
                                       region_id=region_id, currency=currency)

        simple_bo.stop_subscription(service, user=user, service_order_id=service_order_id)

        subs_state = simple_bo.check_order(service, user=user, service_order_id=service_order_id)['subs_state']

        check.check_that(subs_state, deep_equals_to(defaults.Subscriptions.State.FINISHED),
                         step=u'Проверяем что подписка на триальном периоде остановилась',
                         error=u'Не остановилась подписка на триальном периоде')

    @pytest.mark.parametrize('test_data', Data.only_store, ids=DataObject.ids)
    def test_stop_subscription_twice(self, create_normal_subscription, test_data):
        """
        Останавливаем подписку дважды
        """
        service, paymethod, region_id, currency, user_type = \
            test_data.service, test_data.paymethod, test_data.region_id, test_data.currency, test_data.user_type
        user = uids.get_random_of_type(user_type)
        service_product_id, service_order_id = create_normal_subscription(service, user)

        simple_bo.create_and_pay_order(service, user, service_order_id=service_order_id,
                                       service_product_id=service_product_id, paymethod=paymethod,
                                       region_id=region_id, currency=currency)

        with reporter.step(u'Останавливаем подписку'):
            simple_bo.stop_subscription(service, user=user, service_order_id=service_order_id)

            subs_state = simple_bo.check_order(service, user=user, service_order_id=service_order_id)['subs_state']

            check.check_that(subs_state, deep_equals_to(defaults.Subscriptions.State.FINISHED),
                             step=u'Проверяем что подписка остановилась',
                             error=u'Не остановилась подписка')

        with reporter.step(u'Останавливаем подписку во второй раз'):
            simple_bo.stop_subscription(service, user=user, service_order_id=service_order_id)

            subs_state = simple_bo.check_order(service, user=user, service_order_id=service_order_id)['subs_state']

            check.check_that(subs_state, deep_equals_to(defaults.Subscriptions.State.FINISHED),
                             step=u'Проверяем что для уже остановленной подписки корректно отработал StopSubscription',
                             error=u'Ошибка при остановке уже остановленной подписки')


if __name__ == '__main__':
    pytest.main()
