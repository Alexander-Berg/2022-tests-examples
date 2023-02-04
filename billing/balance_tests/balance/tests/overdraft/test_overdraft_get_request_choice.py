# -*- coding: utf-8 -*-
import datetime
import hamcrest
import pytest

from balance import balance_steps as steps
from balance import balance_api as api
from btestlib import utils, reporter
from dateutil.relativedelta import relativedelta
from btestlib.constants import Products, Paysyses, Services, Firms, Users, PersonTypes
from balance.features import Features

pytestmark = [reporter.feature(Features.OVERDRAFT, Features.MULTICURRENCY),
              pytest.mark.tickets('BALANCE-29469')]


@pytest.mark.parametrize('multicurrency', [True, False])
@pytest.mark.parametrize('days_after', [0, 5, 13, 40])
@pytest.mark.parametrize('client_limit', [900, 150])
def test_overdraft_get_request_choices(multicurrency, days_after, client_limit):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)

    service_id = Services.DIRECT.id
    product_id = Products.DIRECT_FISH.id
    paysys_id = Paysyses.BANK_UR_RUB.id
    qty = 300
    dt = datetime.datetime.now() - relativedelta(days=days_after)
    last_day_of_previous_month = datetime.datetime.now().replace(day=1) - datetime.timedelta(days=1)

    if multicurrency:
        with reporter.step(u'Переходим на мультивалютность миграцией'):
            steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='MODIFY',
                                                  dt=last_day_of_previous_month - relativedelta(days=2))
        with reporter.step(u'Подключаем овердрафт'):
            steps.OverdraftSteps.set_force_overdraft(client_id, service_id, client_limit, Firms.YANDEX_1.id,
                                                     currency='RUB')
    else:
        with reporter.step(u'Подключаем овердрафт'):
            client_limit /= 30
            qty /= 30
            steps.OverdraftSteps.set_force_overdraft(client_id, service_id, client_limit, Firms.YANDEX_1.id)

    service_order_id = steps.OrderSteps.next_id(service_id=service_id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=service_id,
                                       product_id=product_id, params={'AgencyID': None})
    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=dt))

    overdraft_params = api.medium().GetRequestChoices({'OperatorUid': Users.YB_ADM.uid,
                                                       'RequestID': request_id,
                                                       'PersonID': person_id})['overdrafts']

    utils.check_that(overdraft_params.get('is_present'), hamcrest.equal_to(True),
                     step=u"Проверяем ответ ручки GetRequestChoices: наличие овердрафта")
    utils.check_that(overdraft_params.get('is_available'), hamcrest.equal_to(True if client_limit > qty else False),
                     step=u"Проверяем ответ ручки GetRequestChoices: доступность овердрафта")
    utils.check_that(overdraft_params.get('currency'), hamcrest.equal_to('RUR'),
                     step=u"Проверяем ответ ручки GetRequestChoices: поле currency")
    utils.check_that(overdraft_params.get('iso_currency'), hamcrest.equal_to('RUB'),
                     step=u"Проверяем ответ ручки GetRequestChoices: поле iso_currency")
    utils.check_that(overdraft_params.get('overdraft_iso_currency'),
                     hamcrest.equal_to('RUB' if multicurrency else None),
                     step=u"Проверяем ответ ручки GetRequestChoices: поле overdraft_iso_currency")
    utils.check_that(overdraft_params.get('available_sum'),
                     hamcrest.equal_to(str(client_limit if multicurrency else client_limit * 30)),
                     step=u"Проверяем ответ ручки GetRequestChoices: доступная сумма в деньгах")
    utils.check_that(overdraft_params.get('available_sum_ue'),
                     hamcrest.equal_to(str(client_limit)),
                     step=u"Проверяем ответ ручки GetRequestChoices: доступная сумма в фишках (для валютных в деньгах)")
    utils.check_that(overdraft_params.get('nearly_expired_sum'), hamcrest.equal_to('0'),
                     step=u"Проверяем ответ ручки GetRequestChoices: сумма, которую нужно оплатить в течение 5 дней")
    utils.check_that(overdraft_params.get('nearly_expired_sum_ue'), hamcrest.equal_to('0'),
                     step=u"Проверяем ответ ручки GetRequestChoices: сумма, которую нужно оплатить в течение 5 дней"
                          u"в фишках (для валютных в деньгах)")
    utils.check_that(overdraft_params.get('expired_sum'), hamcrest.equal_to('0'),
                     step=u"Проверяем ответ ручки GetRequestChoices: сумма, оплата которой просрочена")
    utils.check_that(overdraft_params.get('expired_sum_ue'), hamcrest.equal_to('0'),
                     step=u"Проверяем ответ ручки GetRequestChoices: сумма, оплата которой просрочена "
                          u"в фишках (для валютных в деньгах)")
    utils.check_that(overdraft_params.get('spent_sum'), hamcrest.equal_to('0'),
                     step=u"Проверяем ответ ручки GetRequestChoices: все зачисления в деньгах")
    utils.check_that(overdraft_params.get('spent_sum_ue'), hamcrest.equal_to('0'),
                     step=u"Проверяем ответ ручки GetRequestChoices: все зачисления в фишках (для валютных в деньгах)")
    utils.check_that(overdraft_params.get('min_days_to_live'), hamcrest.equal_to(None),
                     step=u"Проверяем ответ ручки GetRequestChoices: количество дней до срока оплаты")

    try:
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=0, contract_id=None,
                                                     overdraft=1, endbuyer_id=None)
    except Exception, exc:
        utils.check_that(steps.CommonSteps.get_exception_code(exc), hamcrest.equal_to('NOT_ENOUGH_OVERDRAFT_LIMIT'))
        if qty <= client_limit:
            raise utils.TestsError(u"Запрещаем овердрафт при наличии необходимого лимита")
    else:
        service_order_id_2 = steps.OrderSteps.next_id(service_id=service_id)
        order_id_2 = steps.OrderSteps.create(client_id, service_order_id_2, service_id=service_id,
                                             product_id=product_id, params={'AgencyID': None})
        orders_list_2 = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id_2, 'Qty': qty}]
        request_id = steps.RequestSteps.create(client_id, orders_list_2, additional_params=dict(InvoiceDesireDT=dt))
        overdraft_params = api.medium().GetRequestChoices({'OperatorUid': Users.YB_ADM.uid,
                                                           'RequestID': request_id,
                                                           'PersonID': person_id})['overdrafts']

        utils.check_that(overdraft_params.get('is_present'), hamcrest.equal_to(True),
                         step=u"Проверяем ответ ручки GetRequestChoices: наличие овердрафта")
        utils.check_that(overdraft_params.get('is_available'),
                         hamcrest.equal_to(True if client_limit > qty and days_after < 15 else False),
                         step=u"Проверяем ответ ручки GetRequestChoices: доступность овердрафта")
        utils.check_that(overdraft_params.get('currency'), hamcrest.equal_to('RUR'),
                         step=u"Проверяем ответ ручки GetRequestChoices: поле currency")
        utils.check_that(overdraft_params.get('iso_currency'), hamcrest.equal_to('RUB'),
                         step=u"Проверяем ответ ручки GetRequestChoices: поле iso_currency")
        utils.check_that(overdraft_params.get('overdraft_iso_currency'),
                         hamcrest.equal_to('RUB' if multicurrency else None),
                         step=u"Проверяем ответ ручки GetRequestChoices: поле overdraft_iso_currency")
        utils.check_that(overdraft_params.get('available_sum'),
                         hamcrest.equal_to(str((client_limit - qty) if multicurrency else (client_limit - qty) * 30)),
                         step=u"Проверяем ответ ручки GetRequestChoices: доступная сумма в деньгах")
        utils.check_that(overdraft_params.get('available_sum_ue'),
                         hamcrest.equal_to(str(client_limit - qty)),
                         step=u"Проверяем ответ ручки GetRequestChoices: доступная сумма "
                              u"в фишках (для валютных в деньгах)")
        # utils.check_that(overdraft_params.get('nearly_expired_sum'),
        #                  hamcrest.equal_to(str((qty if multicurrency else qty * 30) if days_after == 13 else 0)),
        #                  step=u"Проверяем ответ ручки GetRequestChoices: "
        #                       u"сумма, которую нужно оплатить в течение 5 дней")
        # utils.check_that(overdraft_params.get('nearly_expired_sum_ue'),
        #                  hamcrest.equal_to(str(qty if days_after == 13 else 0)),
        #                  step=u"Проверяем ответ ручки GetRequestChoices: сумма, которую нужно оплатить в течение "
        #                       u"5 дней в фишках (для валютных в деньгах)")
        # utils.check_that(overdraft_params.get('expired_sum'),
        #                  hamcrest.equal_to(str((qty if multicurrency else qty * 30) if days_after >= 20 else 0)),
        #                  step=u"Проверяем ответ ручки GetRequestChoices: сумма, оплата которой просрочена")
        # utils.check_that(overdraft_params.get('expired_sum_ue'),
        #                  hamcrest.equal_to(str(qty if days_after >= 20 else 0)),
        #                  step=u"Проверяем ответ ручки GetRequestChoices: сумма, оплата которой просрочена "
        #                       u"в фишках (для валютных в деньгах)")
        utils.check_that(overdraft_params.get('spent_sum'),
                         hamcrest.equal_to(str(qty if multicurrency else qty * 30)),
                         step=u"Проверяем ответ ручки GetRequestChoices: все зачисления в деньгах")
        utils.check_that(overdraft_params.get('spent_sum_ue'), hamcrest.equal_to(str(qty)),
                         step=u"Проверяем ответ ручки GetRequestChoices: все зачисления "
                              u"в фишках (для валютных в деньгах)")
        # utils.check_that(overdraft_params.get('min_days_to_live'),
        #                  hamcrest.equal_to(15 - days_after if 10 < days_after < 15 else None),
        #                  step=u"Проверяем ответ ручки GetRequestChoices: количество дней до срока оплаты")
        # todo: Для корректной работы проверки нужно добавить учет праздников


def test_overdraft_invoice_before_and_after_migrate():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)

    service_id = Services.DIRECT.id
    product_id = Products.DIRECT_FISH.id
    paysys_id = Paysyses.BANK_UR_RUB.id

    now = datetime.datetime.now()
    dt_invoice_before_migrate = datetime.datetime.now() - relativedelta(days=3)
    dt_migrate = datetime.datetime.now() - relativedelta(days=2)

    with reporter.step(u'Подключаем овердрафт'):
        steps.OverdraftSteps.set_force_overdraft(client_id, service_id, 1000, Firms.YANDEX_1.id)

    service_order_id = steps.OrderSteps.next_id(service_id=service_id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=service_id,
                                       product_id=product_id, params={'AgencyID': None})
    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 300}]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params=dict(InvoiceDesireDT=dt_invoice_before_migrate))
    overdraft_params = steps.RequestSteps.get_request_choices(request_id, person_id=person_id)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=0, contract_id=None,
                                                 overdraft=1, endbuyer_id=None)

    with reporter.step(u'Переходим на мультивалютность миграцией'):
        steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='MODIFY',
                                              dt=dt_migrate)

    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 300}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=now))
    overdraft_params = steps.RequestSteps.get_request_choices(request_id, person_id=person_id)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=0, contract_id=None,
                                                 overdraft=1, endbuyer_id=None)

    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 300}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=now))

    overdraft_params = steps.RequestSteps.get_request_choices(request_id, person_id=person_id)
